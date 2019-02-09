// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.ULocale;

public final class XLikelySubtags {
    private static final String PSEUDO_ACCENTS_PREFIX = "'";  // -XA, -PSACCENT
    private static final String PSEUDO_BIDI_PREFIX = "+";  // -XB, -PSBIDI
    private static final String PSEUDO_CRACKED_PREFIX = ",";  // -XC, -PSCRACK

    static final int SKIP_SCRIPT = 1;

    private static final boolean DEBUG_OUTPUT = LSR.DEBUG_OUTPUT;

    // TODO: Load prebuilt data from a resource bundle
    // to avoid the dependency on the builder code.
    // VisibleForTesting
    public static final XLikelySubtags INSTANCE = new XLikelySubtags(LikelySubtagsBuilder.build());

    static final class Data {
        private final Map<String, String> languageAliases;
        private final Map<String, String> regionAliases;
        private final BytesTrie trie;
        private final LSR[] lsrs;

        Data(Map<String, String> languageAliases, Map<String, String> regionAliases,
                BytesTrie trie, LSR[] lsrs) {
            this.languageAliases = languageAliases;
            this.regionAliases = regionAliases;
            this.trie = trie;
            this.lsrs = lsrs;
        }
    }

    private final Map<String, String> languageAliases;
    private final Map<String, String> regionAliases;

    // The trie maps each lang+script+region (encoded in ASCII) to an index into lsrs.
    // There is also a trie value for each intermediate lang and lang+script.
    // '*' is used instead of "und", "Zzzz"/"" and "ZZ"/"".
    private final BytesTrie trie;
    private final long trieUndState;
    private final long trieUndZzzzState;
    private final int defaultLsrIndex;
    private final long[] trieFirstLetterStates = new long[26];
    private final LSR[] lsrs;

    private XLikelySubtags(XLikelySubtags.Data data) {
        languageAliases = data.languageAliases;
        regionAliases = data.regionAliases;
        trie = data.trie;
        lsrs = data.lsrs;

        // Cache the result of looking up language="und" encoded as "*", and "und-Zzzz" ("**").
        BytesTrie.Result result = trie.next('*');
        assert result.hasNext();
        trieUndState = trie.getState64();
        result = trie.next('*');
        assert result.hasNext();
        trieUndZzzzState = trie.getState64();
        result = trie.next('*');
        assert result.hasValue();
        defaultLsrIndex = trie.getValue();
        trie.reset();

        for (char c = 'a'; c <= 'z'; ++c) {
            result = trie.next(c);
            if (result == BytesTrie.Result.NO_VALUE) {
                trieFirstLetterStates[c - 'a'] = trie.getState64();
            }
            trie.reset();
        }

        if (DEBUG_OUTPUT) {
            System.out.println("*** likely subtags");
            for (Map.Entry<String, LSR> mapping : getTable().entrySet()) {
                System.out.println(mapping);
            }
        }
    }

    private static String getCanonical(Map<String, String> aliases, String alias) {
        String canonical = aliases.get(alias);
        return canonical == null ? alias : canonical;
    }

    // VisibleForTesting
    public LSR makeMaximizedLsrFrom(ULocale locale) {
        String name = locale.getName();
        if (name.startsWith("@x=")) {
            // Private use language tag x-subtag-subtag...
            return new LSR(name, "", "");
        }
        return makeMaximizedLsr(locale.getLanguage(), locale.getScript(), locale.getCountry(),
                locale.getVariant());
    }

    LSR makeMaximizedLsrFrom(Locale locale) {
        String tag = locale.toLanguageTag();
        if (tag.startsWith("x-")) {
            // Private use language tag x-subtag-subtag...
            return new LSR(tag, "", "");
        }
        return makeMaximizedLsr(locale.getLanguage(), locale.getScript(), locale.getCountry(),
                locale.getVariant());
    }

    private LSR makeMaximizedLsr(String language, String script, String region, String variant) {
        // Handle pseudolocales like en-XA, ar-XB, fr-PSCRACK.
        // They should match only themselves,
        // not other locales with what looks like the same language and script subtags.
        if (region.length() == 2 && region.charAt(0) == 'X') {
            switch (region.charAt(1)) {
            case 'A':
                return new LSR(PSEUDO_ACCENTS_PREFIX + language,
                        PSEUDO_ACCENTS_PREFIX + script, region);
            case 'B':
                return new LSR(PSEUDO_BIDI_PREFIX + language,
                        PSEUDO_BIDI_PREFIX + script, region);
            case 'C':
                return new LSR(PSEUDO_CRACKED_PREFIX + language,
                        PSEUDO_CRACKED_PREFIX + script, region);
            default:  // normal locale
                break;
            }
        }

        if (variant.startsWith("PS")) {
            switch (variant) {
            case "PSACCENT":
                return new LSR(PSEUDO_ACCENTS_PREFIX + language,
                        PSEUDO_ACCENTS_PREFIX + script, region.isEmpty() ? "XA" : region);
            case "PSBIDI":
                return new LSR(PSEUDO_BIDI_PREFIX + language,
                        PSEUDO_BIDI_PREFIX + script, region.isEmpty() ? "XB" : region);
            case "PSCRACK":
                return new LSR(PSEUDO_CRACKED_PREFIX + language,
                        PSEUDO_CRACKED_PREFIX + script, region.isEmpty() ? "XC" : region);
            default:  // normal locale
                break;
            }
        }

        language = getCanonical(languageAliases, language);
        // (We have no script mappings.)
        region = getCanonical(regionAliases, region);
        return INSTANCE.maximize(language, script, region);
    }

    /**
     * Raw access to addLikelySubtags. Input must be in canonical format, eg "en", not "eng" or "EN".
     */
    private LSR maximize(String language, String script, String region) {
        if (language.equals("und")) {
            language = "";
        }
        if (script.equals("Zzzz")) {
            script = "";
        }
        if (region.equals("ZZ")) {
            region = "";
        }
        if (!script.isEmpty() && !region.isEmpty() && !language.isEmpty()) {
            return new LSR(language, script, region);  // already maximized
        }

        int retainOldMask = 0;
        BytesTrie iter = new BytesTrie(trie);
        long state;
        int value;
        // Small optimization: Array lookup for first language letter.
        int c0;
        if (language.length() >= 2 && 0 <= (c0 = language.charAt(0) - 'a') && c0 <= 25 &&
                (state = trieFirstLetterStates[c0]) != 0) {
            value = trieNext(iter.resetToState64(state), language, 1);
        } else {
            value = trieNext(iter, language, 0);
        }
        if (value >= 0) {
            if (!language.isEmpty()) {
                retainOldMask |= 4;
            }
            state = iter.getState64();
        } else {
            retainOldMask |= 4;
            iter.resetToState64(trieUndState);  // "und" ("*")
            state = 0;
        }

        if (value > 0) {
            // Intermediate or final value from just language.
            if (value == SKIP_SCRIPT) {
                value = 0;
            }
            if (!script.isEmpty()) {
                retainOldMask |= 2;
            }
        } else {
            value = trieNext(iter, script, 0);
            if (value >= 0) {
                if (!script.isEmpty()) {
                    retainOldMask |= 2;
                }
                state = iter.getState64();
            } else {
                retainOldMask |= 2;
                if (state == 0) {
                    iter.resetToState64(trieUndZzzzState);  // "und-Zzzz" ("**")
                } else {
                    iter.resetToState64(state);
                    value = trieNext(iter, "", 0);
                    assert value >= 0;
                    state = iter.getState64();
                }
            }
        }

        if (value > 0) {
            // Final value from just language or language+script.
            if (!region.isEmpty()) {
                retainOldMask |= 1;
            }
        } else {
            value = trieNext(iter, region, 0);
            if (value >= 0) {
                if (!region.isEmpty()) {
                    retainOldMask |= 1;
                }
            } else {
                retainOldMask |= 1;
                if (state == 0) {
                    value = defaultLsrIndex;
                } else {
                    iter.resetToState64(state);
                    value = trieNext(iter, "", 0);
                    assert value > 0;
                }
            }
        }
        LSR result = lsrs[value];

        if (language.isEmpty()) {
            language = "und";
        }

        if (retainOldMask == 0) {
            return result;
        }
        if ((retainOldMask & 4) == 0) {
            language = result.language;
        }
        if ((retainOldMask & 2) == 0) {
            script = result.script;
        }
        if ((retainOldMask & 1) == 0) {
            region = result.region;
        }
        return new LSR(language, script, region);
    }

    private static final int trieNext(BytesTrie iter, String s, int i) {
        BytesTrie.Result result;
        if (s.isEmpty()) {
            result = iter.next('*');
        } else {
            int end = s.length() - 1;
            for (;; ++i) {
                int c = s.charAt(i);
                if (i < end) {
                    if (!iter.next(c).hasNext()) {
                        return -1;
                    }
                } else {
                    // last character of this subtag
                    result = iter.next(c | 0x80);
                    break;
                }
            }
        }
        switch (result) {
        case NO_MATCH: return -1;
        case NO_VALUE: return 0;
        case INTERMEDIATE_VALUE:
            assert iter.getValue() == SKIP_SCRIPT;
            return SKIP_SCRIPT;
        case FINAL_VALUE: return iter.getValue();
        default: return -1;
        }
    }

    LSR minimizeSubtags(String languageIn, String scriptIn, String regionIn,
            ULocale.Minimize fieldToFavor) {
        LSR result = maximize(languageIn, scriptIn, regionIn);

        // We could try just a series of checks, like:
        // LSR result2 = addLikelySubtags(languageIn, "", "");
        // if result.equals(result2) return result2;
        // However, we can optimize 2 of the cases:
        //   (languageIn, "", "")
        //   (languageIn, "", regionIn)

        // value00 = lookup(result.language, "", "")
        BytesTrie iter = new BytesTrie(trie);
        int value = trieNext(iter, result.language, 0);
        assert value >= 0;
        if (value == 0) {
            value = trieNext(iter, "", 0);
            assert value >= 0;
            if (value == 0) {
                value = trieNext(iter, "", 0);
            }
        }
        assert value > 0;
        LSR value00 = lsrs[value];
        boolean favorRegionOk = false;
        if (result.script.equals(value00.script)) { //script is default
            if (result.region.equals(value00.region)) {
                return new LSR(result.language, "", "");
            } else if (fieldToFavor == ULocale.Minimize.FAVOR_REGION) {
                return new LSR(result.language, "", result.region);
            } else {
                favorRegionOk = true;
            }
        }

        // The last case is not as easy to optimize.
        // Maybe do later, but for now use the straightforward code.
        LSR result2 = maximize(languageIn, scriptIn, "");
        if (result2.equals(result)) {
            return new LSR(result.language, result.script, "");
        } else if (favorRegionOk) {
            return new LSR(result.language, "", result.region);
        }
        return result;
    }

    private Map<String, LSR> getTable() {
        Map<String, LSR> map = new TreeMap<>();
        StringBuilder sb = new StringBuilder();
        for (BytesTrie.Entry entry : trie) {
            sb.setLength(0);
            int length = entry.bytesLength();
            for (int i = 0; i < length;) {
                byte b = entry.byteAt(i++);
                if (b == '*') {
                    sb.append("*-");
                } else if (b >= 0) {
                    sb.append((char) b);
                } else {  // end of subtag
                    sb.append((char) (b & 0x7f)).append('-');
                }
            }
            assert sb.length() > 0 && sb.charAt(sb.length() - 1) == '-';
            sb.setLength(sb.length() - 1);
            map.put(sb.toString(), lsrs[entry.value]);
        }
        return map;
    }

    @Override
    public String toString() {
        return getTable().toString();
    }
}
