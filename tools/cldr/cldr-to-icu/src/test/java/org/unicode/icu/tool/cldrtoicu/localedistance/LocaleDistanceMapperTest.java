// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.truth.Truth.assertThat;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasReason.DEPRECATED;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasReason.LEGACY;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasReason.MACRO;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasType.LANGUAGE;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasType.TERRITORY;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.alias;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.cldrData;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.deprecatedTerritory;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.languageMatch;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.likelySubtag;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.matchVariable;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.paradigms;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.territoryGroup;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.territoryGrouping;
import static org.unicode.icu.tool.cldrtoicu.testing.IcuDataSubjectFactory.assertThat;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.Test;
import org.unicode.cldr.api.CldrData;
import org.unicode.icu.tool.cldrtoicu.IcuData;
import org.unicode.icu.tool.cldrtoicu.RbPath;
import org.unicode.icu.tool.cldrtoicu.RbValue;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.ibm.icu.impl.locale.LSR;
import com.ibm.icu.util.BytesTrie;

/**
 * Higher level tests for {@link LocaleDistanceMapper} to demonstrate that CLDR values
 * are matched and processed, and the IcuData is written as expected.
 *
 * <p>Most of the separate parts which make up this mapper are already tested at a
 * lower level in the other tests in this package.
 */
public class LocaleDistanceMapperTest {
    @Test
    public void testEndToEnd() {
        // Language match elements are ordered, so need an incrementing sort index.
        int idx = 0;

        // A representative subset of CLDR data needed to generate the locale distance.
        // This focuses on two distinct cases:
        // 1: American vs non-American and British English
        //    This demonstrates the way that special case mappings are handled.
        // 2: Chinese, Simplified and Traditional
        //    This demonstrates languages with multiple scripts.
        CldrData testData = cldrData(
                paradigms("en", "en_GB", "es", "es_419"),
                matchVariable("$enUS", "PR+US+VI"),
                matchVariable("$cnsar", "HK+MO"),

                // The <languageMatch> element is marked "ORDERED" in the DTD, so
                // ordering of match rules can can affect output (when paths are
                // otherwise equal). DTD ordering will not re-order this data.
                languageMatch("yue", "zh", 10, true, ++idx),
                languageMatch("*", "*", 80, false, ++idx),

                languageMatch("zh_Hans", "zh_Hant", 15, true, ++idx),
                languageMatch("zh_Hant", "zh_Hans", 19, true, ++idx),
                languageMatch("zh_Latn", "zh_Hans", 20, true, ++idx),
                languageMatch("*_*", "*_*", 50, false, ++idx),

                languageMatch("en_*_$enUS", "en_*_$enUS", 4, false, ++idx),
                languageMatch("en_*_$!enUS", "en_*_GB", 3, false, ++idx),
                languageMatch("en_*_$!enUS", "en_*_$!enUS", 4, false, ++idx),
                languageMatch("en_*_*", "en_*_*", 5, false, ++idx),

                languageMatch("zh_Hant_$cnsar", "zh_Hant_$cnsar", 4, false, ++idx),
                languageMatch("zh_Hant_$!cnsar", "zh_Hant_$!cnsar", 4, false, ++idx),
                languageMatch("zh_Hant_*", "zh_Hant_*", 5, false, ++idx),
                languageMatch("*_*_*", "*_*_*", 4, false, ++idx),

                // NOTE: This is deliberately NOT in DTD order to demonstrate that the
                // mapper will reorder these (putting "und" last) which means that the
                // ICU data here is NOT affected by changes in the likely subtag order).
                likelySubtag("und", "en_Latn_US"),
                likelySubtag("und_HK", "zh_Hant_HK"),
                likelySubtag("und_MO", "zh_Hant_MO"),
                likelySubtag("und_TW", "zh_Hant_TW"),
                likelySubtag("und_030", "zh_Hans_CN"),
                likelySubtag("und_142", "zh_Hans_CN"),
                likelySubtag("und_CN", "zh_Hans_CN"),
                likelySubtag("und_Hans", "zh_Hans_CN"),
                likelySubtag("und_Hant", "zh_Hant_TW"),
                likelySubtag("zh", "zh_Hans_CN"),
                likelySubtag("zh_Hant", "zh_Hant_TW"),
                likelySubtag("zh_TW", "zh_Hant_TW"),

                // NOT in DTD order (to demonstrate order invariance later).
                alias(LANGUAGE, LEGACY, "zh_SG", "zh_Hans_SG"),
                alias(LANGUAGE, LEGACY, "zh_HK", "zh_Hant_HK"),
                alias(LANGUAGE, LEGACY, "zh_TW", "zh_Hant_TW"),
                alias(LANGUAGE, LEGACY, "zh_MO", "zh_Hant_MO"),
                alias(LANGUAGE, LEGACY, "zh_CN", "zh_Hans_CN"),
                alias(LANGUAGE, MACRO, "cmn", "zh"),

                // NOT in DTD order (to demonstrate order invariance later).
                alias(TERRITORY, DEPRECATED, "UK", "GB"),
                alias(TERRITORY, DEPRECATED, "AN", "CW", "SX", "BQ"),

                // Rather trimmed down containment hierarchy. It still retains macro
                // regions and grouping to demonstrate that these work as expected.
                territoryGroup("001", "019", "142", "150"),          // World
                territoryGrouping("001", "EU"),
                territoryGroup("019", "021", "419"),                 // Americas
                territoryGroup("142", "030", "035"),                 // Asia
                territoryGroup("150", "154", "155"),                 // Europe
                territoryGrouping("EU", "DE", "FR", "IE"),           // European Union (no CH or GB)
                territoryGroup("021", "CA", "PM", "US"),             // Northern America
                territoryGroup("419", "013", "029"),                 // Latin America and the Caribbean
                territoryGroup("030", "CN", "HK", "MO", "TW"),       // Eastern Asia
                territoryGroup("035", "PH", "SG", "TH", "VN"),       // South-Eastern Asia
                territoryGroup("154", "GB", "IE"),                   // Northern Europe
                territoryGroup("155", "CH", "DE", "FR"),             // Western Europe
                territoryGroup("013", "CR", "MX", "PA"),             // Central America
                territoryGroup("029", "BQ", "CW", "PR", "SX", "VI"), // Caribbean
                deprecatedTerritory("029", "AN"));                   // Antilles (=> BQ, CW, SX)

        IcuData icuData = LocaleDistanceMapper.process(testData);
        // Aliases come in (deprecated, replacement) pairs.
        assertThat(icuData).hasValuesFor("likely/languageAliases", "cmn", "zh");
        assertThat(icuData).hasValuesFor("likely/regionAliases", "AN", "CW", "UK", "GB");

        // LSR values come in (language, script, region) tuples. They are the mapped-to
        // values for the likely subtag mappings, ordered by the DTD order in which the
        // mapping keys were encountered.
        assertThat(icuData).hasValuesFor("likely/lsrs",
                "", "", "",
                "skip", "script", "",
                "zh", "Hans", "CN",
                "zh", "Hant", "TW",
                "en", "Latn", "US",
                "zh", "Hant", "HK",
                "zh", "Hant", "MO");

        // It's a bit easier to see how match keys are grouped against the partitions.
        ImmutableSetMultimap<Integer, String> likelyTrie =
                getTrieMap(icuData, "likely/trie:bin", "*").asMultimap().inverse();

        // Special values in the lookup table don't map from any locales directly.
        assertThat(likelyTrie).valuesForKey(0).isEmpty();
        assertThat(likelyTrie).valuesForKey(1).isEmpty();

        // Index 4: en-Latn-US (the general default and default for Latn).
        assertThat(likelyTrie).valuesForKey(4).containsExactly("*-Latn-*", "*-Latn-US", "*-*-*");

        // Index 2: zh-Hans-CN (default for zh, Hans and CN separately).
        assertThat(likelyTrie).valuesForKey(2).containsExactly(
                "*-*-030", "*-*-142",               // macro regions
                "*-*-CN", "*-Hans-*", "*-Hans-CN",  // unknown language match
                "cmn-*-*",                          // language alias
                "zh-*-*");                          // default for language

        // Index 2: zh-Hant-TW (default for zh if Hant or TW is given).
        assertThat(likelyTrie).valuesForKey(3).containsExactly(
                "*-*-TW", "*-Hant-*", "*-Hant-TW",  // unknown language match
                "cmn-*-TW", "cmn-Hant",             // language alias with specific script/region
                "zh-*-TW", "zh-Hant");              // default for script/region

        // Other zh languages (zh-Hant-HK, zh-Hant-MO) require an explicit region match.
        assertThat(likelyTrie).valuesForKey(5).containsExactly("*-*-HK", "*-Hant-HK");
        assertThat(likelyTrie).valuesForKey(6).containsExactly("*-*-MO", "*-Hant-MO");

        // Pairs of expanded paradigm locales (using LSR tuples) in declaration order.
        // This is just the list from the CLDR data with no processing.
        assertThat(icuData).hasValuesFor("match/paradigms",
                "en", "Latn", "US",
                "en", "Latn", "GB",
                "es", "Latn", "ES",
                "es", "Latn", "419");

        // See PartitionInfoTest for a description of the ordering of these strings.
        assertThat(icuData).hasValuesFor("match/partitions",
                ".", "0", "1", "2", "3", "0123", "03", "02", "01");

        ImmutableMap<String, Integer> matchTrie = getTrieMap(icuData, "match/trie:bin", "*-*");
        byte[] regionLookup = getBytes(icuData, "match/regionToPartitions:bin");
        ImmutableList<String> partitions =
                icuData.get(RbPath.parse("match/partitions")).get(0).getElements();

        // Test defaults have been trimmed.
        assertThat(matchTrie).doesNotContainKey("*-*");
        assertThat(matchTrie).doesNotContainKey("*-*-*-*");
        assertThat(matchTrie).doesNotContainKey("*-*-*-*-*-*");

        // Some zh specific tests.
        assertThat(matchTrie).containsEntry("yue-zh", 10);  // Encapsulated language
        assertThat(matchTrie).containsEntry("zh-zh-Hant-Hant-*-*", 5);

        // Special marker that means "en-en" matches don't use script information.
        // This is assumed in the distance tests below, so it's important to check.
        assertThat(matchTrie).containsEntry("en-en", 128);

        // British English is a slightly better match against non-American English.
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "CA", "GB", 3);
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "GB", "GB", 3);
        // "EU" works here because while it's a macro region, in this data it only
        // covers a single partition.
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "GB", "EU", 3);

        // Pairs of non-American or American English languages get a larger distance.
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "CA", "DE", 4);
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "US", "PR", 4);
        // Deprecated regions (AN) are still mapped to partitions and get real distances.
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "AN", "TW", 4);

        // Mixing American and non-American English gets the default "en-en-*-*" distance.
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "GB", "US", 5);
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "CA", "US", 5);
        assertEnDistanceForRegions(matchTrie, regionLookup, partitions, "US", "AN", 5);

        // Default distances for language, script and region, plus minimum region distance.
        // Minimum region distance is "en_*_$!enUS" -> "en_*_GB" (as seen above).
        assertThat(icuData).hasValuesFor("match/distances:intvector", "80", "50", "4", "3");
    }

    // Helper to make assertions about language distance a bit more readable.
    // PartitionInfoTest includes more low level tests for precise ordering etc.
    private static void assertEnDistanceForRegions(
            ImmutableMap<String, Integer> matchTrie,
            byte[] regionLookup,
            ImmutableList<String> paritions,
            String regionA, String regionB,
            int distance) {
        // Three step lookup for each region:
        // 1: Find LSR index from region string.
        // 2: Lookup partition group index from region lookup table.
        // 3: Lookup partition group string from partitions table.
        String partitionA = paritions.get(regionLookup[LSR.indexForRegion(regionA)]);
        String partitionB = paritions.get(regionLookup[LSR.indexForRegion(regionB)]);

        // For now only support cases where there's a single partition ID associated
        // with the region (this is all non-macro regions and *some* macro regions).
        checkArgument(partitionA.length() == 1 && partitionB.length() == 1,
                "multiple partitions unsupported in test: %s %s", regionA, regionB);

        // This is a depth 2 key because we know that "en" skips scripts. This will
        // not work the same for "zh" because that needs scripts information.
        String key = String.format("en-en-%s-%s", partitionA, partitionB);
        if (matchTrie.containsKey(key)) {
            assertThat(matchTrie).containsEntry(key, distance);
        } else {
            assertThat(matchTrie).containsEntry("en-en-*-*", distance);
        }
    }

    // Returns the mapping for a Trie from a ":bin" suffixed resource value.
    // "star" defines what the Trie wildcard should be expanded to (for readability).
    private static ImmutableMap<String, Integer> getTrieMap(IcuData icuData, String path, String star) {
        return TestData.getTrieTable(getTrie(icuData, path), star, i -> i);
    }

    // Reads a Trie from a ":bin" suffixed resource value.
    private static BytesTrie getTrie(IcuData icuData, String path) {
        return new BytesTrie(getBytes(icuData, path), 0);
    }

    // Reads a byte array from a ":bin" suffixed resource value.
    private static byte[] getBytes(IcuData icuData, String path) {
        RbPath rbPath = RbPath.parse(path);
        checkArgument(rbPath.isBinPath(), "only binary paths (:bin) should have binary data: %s", path);
        List<RbValue> rbValues = icuData.get(rbPath);
        checkArgument(rbValues != null, "missing value for: %s", rbPath);
        checkArgument(rbValues.size() == 1, "expect single RbValue: %s", rbValues);
        // Take a sequence of hex-strings, convert each to a byte[] and collect them.
        return rbValues.get(0).getElements().stream()
                .map(LocaleDistanceMapperTest::decodeHex)
                .collect(
                        ByteArrayOutputStream::new,
                        (out, b) -> out.write(b, 0, b.length),
                        (out, b) -> out.write(b.toByteArray(), 0, b.size()))
                .toByteArray();
    }

    // Hex chars to byte array (2 chars per byte, little endian).
    private static byte[] decodeHex(String s) {
        checkArgument(s.length() % 2 == 0, "binary hex strings must have an even length: %s", s);
        checkArgument(HEX.matchesAllOf(s), "invalid binary hex string: %s", s);
        byte[] bytes = new byte[s.length() / 2];
        for (int n = 0; n < bytes.length; n++) {
            bytes[n] = (byte) Integer.parseUnsignedInt(s.substring(2 * n, 2 * (n + 1)), 16);
        }
        return bytes;
    }

    private static final CharMatcher HEX = CharMatcher.anyOf("0123456789abcdefABCDEF");
}
