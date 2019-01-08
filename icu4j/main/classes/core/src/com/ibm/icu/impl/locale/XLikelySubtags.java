// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.locale;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.ULocale;

public final class XLikelySubtags {
    private static final String PSEUDO_ACCENTS_PREFIX = "'";  // -XA, -PSACCENT
    private static final String PSEUDO_BIDI_PREFIX = "+";  // -XB, -PSBIDI
    private static final String PSEUDO_CRACKED_PREFIX = ",";  // -XC, -PSCRACK

    private static final boolean DEBUG_OUTPUT = false;

    // TODO: Load prebuilt data from a resource bundle
    // to avoid the dependency on the builder code.
    static final XLikelySubtags INSTANCE = new XLikelySubtags(LikelySubtagsBuilder.build());

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
    private final LSR[] lsrs;

    private XLikelySubtags(XLikelySubtags.Data data) {
        languageAliases = data.languageAliases;
        regionAliases = data.regionAliases;
        trie = data.trie;
        lsrs = data.lsrs;

        // Cache the result of looking up language="und" encoded as "*", and "und-Zzzz" ("**").
        BytesTrie.Result result = trie.next('*');
        assert result == BytesTrie.Result.INTERMEDIATE_VALUE;
        int value = trie.getValue();
        assert value == 0;
        trieUndState = trie.getState64();
        result = trie.next('*');
        assert result == BytesTrie.Result.INTERMEDIATE_VALUE;
        value = trie.getValue();
        assert value == 0;
        trieUndZzzzState = trie.getState64();
        result = trie.next('*');
        assert result.hasValue();
        defaultLsrIndex = trie.getValue();
        trie.reset();

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

    LSR makeMaximizedLsrFrom(ULocale locale) {
        String name = locale.getName();
        if (name.startsWith("@x=")) {
            // Private use language tag x-subtag-subtag...
            return new LSR(name, "", "");
        }

        // Handle pseudolocales like en-XA, ar-XB, fr-PSCRACK.
        // They should match only themselves,
        // not other locales with what looks like the same language and script subtags.
        String language = locale.getLanguage();
        String script = locale.getScript();
        String region = locale.getCountry();
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

        String variant = locale.getVariant();
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
        // script is ok
        region = getCanonical(regionAliases, region);
        return INSTANCE.maximize(language, script, region);
    }

    /**
     * Raw access to addLikelySubtags. Input must be in canonical format, eg "en", not "eng" or "EN".
     */
    private LSR maximize(String language, String script, String region) {
        int retainOldMask = 0;
        BytesTrie iter = new BytesTrie(trie);
        // language lookup
        if (language.equals("und")) {
            language = "";
        }
        long state;
        int value = trieNext(iter, language, false);
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
        // script lookup
        if (script.equals("Zzzz")) {
            script = "";
        }
        value = trieNext(iter, script, false);
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
                value = trieNext(iter, "", false);
                assert value == 0;
                state = iter.getState64();
            }
        }
        // region lookup
        if (region.equals("ZZ")) {
            region = "";
        }
        value = trieNext(iter, region, true);
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
                value = trieNext(iter, "", true);
                if (value < 0) {  // TODO: should never happen?! just assert value >= 0?
                    return null;
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

    private static final int trieNext(BytesTrie iter, String s, boolean finalSubtag) {
        BytesTrie.Result result;
        if (s.isEmpty()) {
            result = iter.next('*');
        } else {
            int end = s.length() - 1;
            for (int i = 0;; ++i) {
                result = iter.next(s.charAt(i));
                if (i < end) {
                    if (!result.hasNext()) {
                        return -1;
                    }
                } else {
                    // last character of this subtag
                    break;
                }
            }
        }
        if (!finalSubtag) {
            if (result == BytesTrie.Result.INTERMEDIATE_VALUE) {
                return 0;  // value should be 0, don't care
            }
        } else {
            if (result.hasValue()) {
                return iter.getValue();
            }
        }
        return -1;
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
        int value = trieNext(iter, result.language, false);
        assert value >= 0;
        value = trieNext(iter, "", false);
        assert value >= 0;
        value = trieNext(iter, "", true);
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
        Map<String, LSR> map = new LinkedHashMap<>();
        Set<String> prefixes = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (BytesTrie.Entry entry : trie) {
            sb.setLength(0);
            int length = entry.bytesLength();
            for (int i = 0; i < length;) {
                byte b = entry.byteAt(i++);
                sb.append((char) b);
                if (i < length && prefixes.contains(sb.toString())) {
                    sb.append('-');
                }
            }
            String s = sb.toString();
            if (entry.value == 0) {
                // intermediate match point
                prefixes.add(s);
            } else {
                map.put(s, lsrs[entry.value]);
            }
        }
        return map;
    }

    @Override
    public String toString() {
        return getTable().toString();
    }
}
