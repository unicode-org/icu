// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.impl.locale;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.TreeMap;

import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.UResource;
import com.ibm.icu.util.BytesTrie;
import com.ibm.icu.util.Region;
import com.ibm.icu.util.ULocale;

public final class LikelySubtags {
    private static final String PSEUDO_ACCENTS_PREFIX = "'";  // -XA, -PSACCENT
    private static final String PSEUDO_BIDI_PREFIX = "+";  // -XB, -PSBIDI
    private static final String PSEUDO_CRACKED_PREFIX = ",";  // -XC, -PSCRACK

    public static final int SKIP_SCRIPT = 1;

    private static final boolean DEBUG_OUTPUT = LSR.DEBUG_OUTPUT;

    // VisibleForTesting
    public static final class Data {
        public final Map<String, String> languageAliases;
        public final Map<String, String> regionAliases;
        public final byte[] trie;
        public final LSR[] lsrs;

        public Data(Map<String, String> languageAliases, Map<String, String> regionAliases,
                byte[] trie, LSR[] lsrs) {
            this.languageAliases = languageAliases;
            this.regionAliases = regionAliases;
            this.trie = trie;
            this.lsrs = lsrs;
        }

        private static UResource.Value getValue(UResource.Table table,
                String key, UResource.Value value) {
            if (!table.findValue(key, value)) {
                throw new MissingResourceException(
                        "langInfo.res missing data", "", "likely/" + key);
            }
            return value;
        }

        // VisibleForTesting
        public static Data load() throws MissingResourceException {
            ICUResourceBundle langInfo = ICUResourceBundle.getBundleInstance(
                    ICUData.ICU_BASE_NAME, "langInfo",
                    ICUResourceBundle.ICU_DATA_CLASS_LOADER, ICUResourceBundle.OpenType.DIRECT);
            UResource.Value value = langInfo.getValueWithFallback("likely");
            UResource.Table likelyTable = value.getTable();

            Map<String, String> languageAliases;
            if (likelyTable.findValue("languageAliases", value)) {
                String[] pairs = value.getStringArray();
                languageAliases = new HashMap<>(pairs.length / 2);
                for (int i = 0; i < pairs.length; i += 2) {
                    languageAliases.put(pairs[i], pairs[i + 1]);
                }
            } else {
                languageAliases = Collections.emptyMap();
            }

            Map<String, String> regionAliases;
            if (likelyTable.findValue("regionAliases", value)) {
                String[] pairs = value.getStringArray();
                regionAliases = new HashMap<>(pairs.length / 2);
                for (int i = 0; i < pairs.length; i += 2) {
                    regionAliases.put(pairs[i], pairs[i + 1]);
                }
            } else {
                regionAliases = Collections.emptyMap();
            }

            ByteBuffer buffer = getValue(likelyTable, "trie", value).getBinary();
            byte[] trie = new byte[buffer.remaining()];
            buffer.get(trie);

            String[] m49 = getValue(likelyTable, "m49", value).getStringArray();
            LSR[] lsrs = LSR.decodeInts(getValue(likelyTable, "lsrnum", value).getIntVector(), m49);
            return new Data(languageAliases, regionAliases, trie, lsrs);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) { return true; }
            if (other == null || !getClass().equals(other.getClass())) { return false; }
            Data od = (Data)other;
            return
                    languageAliases.equals(od.languageAliases) &&
                    regionAliases.equals(od.regionAliases) &&
                    Arrays.equals(trie, od.trie) &&
                    Arrays.equals(lsrs, od.lsrs);
        }

        @Override
        public int hashCode() {  // unused; silence ErrorProne
            return 1;
        }
    }

    // VisibleForTesting
    public static final LikelySubtags INSTANCE = new LikelySubtags(Data.load());

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

    private LikelySubtags(LikelySubtags.Data data) {
        languageAliases = data.languageAliases;
        regionAliases = data.regionAliases;
        trie = new BytesTrie(data.trie, 0);
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

    /**
     * Implementation of LocaleMatcher.canonicalize(ULocale).
     */
    public ULocale canonicalize(ULocale locale) {
        String lang = locale.getLanguage();
        String lang2 = languageAliases.get(lang);
        String region = locale.getCountry();
        String region2 = regionAliases.get(region);
        if (lang2 != null || region2 != null) {
            return new ULocale(
                lang2 == null ? lang : lang2,
                locale.getScript(),
                region2 == null ? region : region2);
        }
        return locale;
    }

    private static String getCanonical(Map<String, String> aliases, String alias) {
        String canonical = aliases.get(alias);
        return canonical == null ? alias : canonical;
    }

    // VisibleForTesting
    public LSR makeMaximizedLsrFrom(ULocale locale, boolean returnInputIfUnmatch) {
        String name = locale.getName();  // Faster than .toLanguageTag().
        if (name.startsWith("@x=")) {
            String tag = locale.toLanguageTag();
            assert tag.startsWith("und-x-");
            // Private use language tag x-subtag-subtag... which CLDR changes to
            // und-x-subtag-subtag...
            return new LSR(tag, "", "", LSR.EXPLICIT_LSR);
        }
        LSR max = makeMaximizedLsr(locale.getLanguage(), locale.getScript(), locale.getCountry(),
                locale.getVariant(), returnInputIfUnmatch);
        if (max.language.isEmpty() && max.script.isEmpty() && max.region.isEmpty()) {
            return new LSR(locale.getLanguage(), locale.getScript(), locale.getCountry(), LSR.EXPLICIT_LSR);
        }
        return max;
    }

    public LSR makeMaximizedLsrFrom(Locale locale) {
        String tag = locale.toLanguageTag();
        if (tag.startsWith("x-") || tag.startsWith("und-x-")) {
            // Private use language tag x-subtag-subtag... which CLDR changes to
            // und-x-subtag-subtag...
            return new LSR(tag, "", "", LSR.EXPLICIT_LSR);
        }
        return makeMaximizedLsr(locale.getLanguage(), locale.getScript(), locale.getCountry(),
                locale.getVariant(), false);
    }

    private LSR makeMaximizedLsr(String language, String script, String region, String variant, boolean returnInputIfUnmatch) {
        // Handle pseudolocales like en-XA, ar-XB, fr-PSCRACK.
        // They should match only themselves,
        // not other locales with what looks like the same language and script subtags.
        if (!returnInputIfUnmatch) {
            if (region.length() == 2 && region.charAt(0) == 'X') {
                switch (region.charAt(1)) {
                case 'A':
                    return new LSR(PSEUDO_ACCENTS_PREFIX + language,
                            PSEUDO_ACCENTS_PREFIX + script, region, LSR.EXPLICIT_LSR);
                case 'B':
                    return new LSR(PSEUDO_BIDI_PREFIX + language,
                            PSEUDO_BIDI_PREFIX + script, region, LSR.EXPLICIT_LSR);
                case 'C':
                    return new LSR(PSEUDO_CRACKED_PREFIX + language,
                            PSEUDO_CRACKED_PREFIX + script, region, LSR.EXPLICIT_LSR);
                default:  // normal locale
                    break;
                }
            }

            if (variant.startsWith("PS")) {
                int lsrFlags = region.isEmpty() ?
                            LSR.EXPLICIT_LANGUAGE | LSR.EXPLICIT_SCRIPT : LSR.EXPLICIT_LSR;
                switch (variant) {
                case "PSACCENT":
                    return new LSR(PSEUDO_ACCENTS_PREFIX + language,
                            PSEUDO_ACCENTS_PREFIX + script,
                            region.isEmpty() ? "XA" : region, lsrFlags);
                case "PSBIDI":
                    return new LSR(PSEUDO_BIDI_PREFIX + language,
                            PSEUDO_BIDI_PREFIX + script,
                            region.isEmpty() ? "XB" : region, lsrFlags);
                case "PSCRACK":
                    return new LSR(PSEUDO_CRACKED_PREFIX + language,
                            PSEUDO_CRACKED_PREFIX + script,
                            region.isEmpty() ? "XC" : region, lsrFlags);
                default:  // normal locale
                    break;
                }
            }
        }

        language = getCanonical(languageAliases, language);
        // (We have no script mappings.)
        region = getCanonical(regionAliases, region);
        return maximize(language, script, region, returnInputIfUnmatch);
    }

    /**
     * Helper method to find out a region is a macroregion
     */
    private boolean isMacroregion(String region) {
        Region.RegionType type = Region.getInstance(region).getType();
        return type == Region.RegionType.WORLD ||
            type == Region.RegionType.CONTINENT ||
            type == Region.RegionType.SUBCONTINENT ;
    }

    /**
     * Raw access to addLikelySubtags. Input must be in canonical format, eg "en", not "eng" or "EN".
     */
    private LSR maximize(String language, String script, String region, boolean returnInputIfUnmatch) {
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
            return new LSR(language, script, region, LSR.EXPLICIT_LSR);  // already maximized
        }

        boolean retainLanguage = false;
        boolean retainScript = false;
        boolean retainRegion = false;
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
        boolean matchLanguage = (value >= 0);
        boolean matchScript = false;
        if (value >= 0) {
            retainLanguage = ! language.isEmpty();
            state = iter.getState64();
        } else {
            retainLanguage = true;
            iter.resetToState64(trieUndState);  // "und" ("*")
            state = 0;
        }

        if (value >= 0 && !script.isEmpty()) {
            matchScript = true;
        }
        if (value > 0) {
            // Intermediate or final value from just language.
            if (value == SKIP_SCRIPT) {
                value = 0;
            }
            retainScript = ! script.isEmpty();
        } else {
            value = trieNext(iter, script, 0);
            if (value >= 0) {
                retainScript = ! script.isEmpty();
                state = iter.getState64();
            } else {
                retainScript = true;
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

        boolean matchRegion = false;
        if (value > 0) {
            // Final value from just language or language+script.
            retainRegion = ! region.isEmpty();
        } else {
            value = trieNext(iter, region, 0);
            if (value >= 0) {
                if (!region.isEmpty() && !isMacroregion(region)) {
                    retainRegion = true;
                    matchRegion = true;
                }
            } else {
                retainRegion = true;
                if (state == 0) {
                    value = defaultLsrIndex;
                } else {
                    iter.resetToState64(state);
                    value = trieNext(iter, "", 0);
                    assert value != 0;
                    if (value < 0) {
                        retainLanguage = !language.isEmpty();
                        retainScript = !script.isEmpty();
                        retainRegion = !region.isEmpty();
                        // Fallback to und_$region =>
                        iter.resetToState64(trieUndState);  // "und" ("*")
                        value = trieNext(iter, "", 0);
                        assert value == 0;
                        long trieUndEmptyState = iter.getState64();
                        value = trieNext(iter, region, 0);
                        // Fallback to und =>
                        if (value < 0) {
                            iter.resetToState64(trieUndEmptyState);
                            value = trieNext(iter, "", 0);
                            assert value > 0;
                        }
                    }
                }
            }
        }

        if (returnInputIfUnmatch &&
            (!(matchLanguage || matchScript || (matchRegion && language.isEmpty())))) {
            return new LSR("", "", "", LSR.EXPLICIT_LSR);  // no matching.
        }
        if (language.isEmpty()) {
            language = "und";
        }

        if (! (retainLanguage || retainScript || retainRegion)) {
            assert value >= 0;
            assert lsrs[value].flags == LSR.IMPLICIT_LSR;
            return lsrs[value];
        }
        if (!retainLanguage) {
            assert value >= 0;
            language = lsrs[value].language;
        }
        if (!retainScript) {
            assert value >= 0;
            script = lsrs[value].script;
        }
        if (!retainRegion) {
            assert value >= 0;
            region = lsrs[value].region;
        }
        int retainMask = (retainLanguage ? 4 : 0) + (retainScript ? 2 : 0) + (retainRegion ? 1 : 0);
        // retainOldMask flags = LSR explicit-subtag flags
        return new LSR(language, script, region, retainMask);
    }

    /**
     * Tests whether lsr is "more likely" than other.
     * For example, fr-Latn-FR is more likely than fr-Latn-CH because
     * FR is the default region for fr-Latn.
     *
     * <p>The likelyInfo caches lookup information between calls.
     * The return value is an updated likelyInfo value,
     * with bit 0 set if lsr is "more likely".
     * The initial value of likelyInfo must be negative.
     */
    int compareLikely(LSR lsr, LSR other, int likelyInfo) {
        // If likelyInfo >= 0:
        // likelyInfo bit 1 is set if the previous comparison with lsr
        // was for equal language and script.
        // Otherwise the scripts differed.
        if (!lsr.language.equals(other.language)) {
            return 0xfffffffc;  // negative, lsr not better than other
        }
        if (!lsr.script.equals(other.script)) {
            int index;
            if (likelyInfo >= 0 && (likelyInfo & 2) == 0) {
                index = likelyInfo >> 2;
            } else {
                index = getLikelyIndex(lsr.language, "");
                likelyInfo = index << 2;
            }
            LSR likely = lsrs[index];
            if (lsr.script.equals(likely.script)) {
                return likelyInfo | 1;
            } else {
                return likelyInfo & ~1;
            }
        }
        if (!lsr.region.equals(other.region)) {
            int index;
            if (likelyInfo >= 0 && (likelyInfo & 2) != 0) {
                index = likelyInfo >> 2;
            } else {
                index = getLikelyIndex(lsr.language, lsr.region);
                likelyInfo = (index << 2) | 2;
            }
            LSR likely = lsrs[index];
            if (lsr.region.equals(likely.region)) {
                return likelyInfo | 1;
            } else {
                return likelyInfo & ~1;
            }
        }
        return likelyInfo & ~1;  // lsr not better than other
    }

    // Subset of maximize().
    private int getLikelyIndex(String language, String script) {
        if (language.equals("und")) {
            language = "";
        }
        if (script.equals("Zzzz")) {
            script = "";
        }

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
            state = iter.getState64();
        } else {
            iter.resetToState64(trieUndState);  // "und" ("*")
            state = 0;
        }

        if (value > 0) {
            // Intermediate or final value from just language.
            if (value == SKIP_SCRIPT) {
                value = 0;
            }
        } else {
            value = trieNext(iter, script, 0);
            if (value >= 0) {
                state = iter.getState64();
            } else {
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
        } else {
            value = trieNext(iter, "", 0);
            assert value > 0;
        }
        return value;
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

    public LSR minimizeSubtags(String languageIn, String scriptIn, String regionIn,
            ULocale.Minimize fieldToFavor) {
        LSR max = maximize(languageIn, scriptIn, regionIn, true);
        if (max.language.isEmpty() && max.region.isEmpty() && max.script.isEmpty()) {
            // Cannot match, return as is
            return new LSR(languageIn, scriptIn, regionIn, LSR.EXPLICIT_LSR);
        }
        LSR test = maximize(max.language, "", "", true);
        if (test.isEquivalentTo(max)) {
            return new LSR(max.language, "", "", LSR.DONT_CARE_FLAGS);
        }
        if (ULocale.Minimize.FAVOR_REGION == fieldToFavor) {
            test = maximize(max.language, "", max.region, true);
            if (test.isEquivalentTo(max)) {
                return new LSR(max.language, "", max.region, LSR.DONT_CARE_FLAGS);
            }
            test = maximize(max.language, max.script, "", true);
            if (test.isEquivalentTo(max)) {
                return new LSR(max.language, max.script, "", LSR.DONT_CARE_FLAGS);
            }
        } else {
            test = maximize(max.language, max.script, "", true);
            if (test.isEquivalentTo(max)) {
                return new LSR(max.language, max.script, "", LSR.DONT_CARE_FLAGS);
            }
            test = maximize(max.language, "", max.region, true);
            if (test.isEquivalentTo(max)) {
                return new LSR(max.language, "", max.region, LSR.DONT_CARE_FLAGS);
            }
        }
        return new LSR(max.language, max.script, max.region, LSR.DONT_CARE_FLAGS);
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
