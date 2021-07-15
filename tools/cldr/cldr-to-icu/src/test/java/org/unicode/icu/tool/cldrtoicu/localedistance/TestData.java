// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

import java.util.List;

import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;

import com.google.common.base.Ascii;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.impl.locale.LSR;
import com.ibm.icu.util.BytesTrie;

/**
 * Utilities for easily generating test data for the LocaleDistanceMapper tests.
 */
final class TestData {
    /**
     * Returns an LSR from a locale ID pattern (e.g. "und", "zh-Hant", "en-*-GB").
     * This is definitely not a general locale parser!
     */
    static LSR lsr(String s) {
        List<String> parts = Splitter.on('-').splitToList(s);
        checkArgument(parts.size() <= 3);
        return new LSR(
                parts.get(0),
                parts.size() > 1 ? parts.get(1) : "",
                parts.size() > 2 ? parts.get(2) : "",
                LSR.DONT_CARE_FLAGS);
    }

    enum AliasType { LANGUAGE, TERRITORY }

    enum AliasReason { DEPRECATED, OVERLONG, LEGACY, MACRO }

    /** Returns CLDR data for the given values. */
    static CldrData cldrData(CldrValue... values) {
        return CldrDataSupplier.forValues(asList(values));
    }

    /** Returns a CldrValue for a {@code <paradigmLocales>} element. */
    static CldrValue paradigms(String... values) {
        return supplemental(
                "languageMatching/languageMatches[@type=\"written_new\"]/"
                        + "paradigmLocales[@locales=\"%s\"]",
                String.join(" ", values));
    }

    /** Returns a CldrValue for a {@code <matchVariable>} element. */
    static CldrValue matchVariable(String id, String value) {
        return supplemental(
                "languageMatching/languageMatches[@type=\"written_new\"]/"
                        + "matchVariable[@id=\"%s\"][@value=\"%s\"]",
                id, value);
    }

    /** Returns a CldrValue for a {@code <languageMatch>} element. */
    static CldrValue languageMatch(
            String desired, String supported, int distance, boolean oneway, int sort) {
        return supplemental(
                "languageMatching/languageMatches[@type=\"written_new\"]/"
                        + "languageMatch[@_q=\"%d\"][@desired=\"%s\"][@supported=\"%s\"][@distance=\"%d\"]%s",
                sort, desired, supported, distance, oneway ? "[@oneway=\"true\"]" : "");
    }

    /** Returns a CldrValue for either a {@code <languageAlias>} or {@code <territoryAlias>} element. */
    static CldrValue alias(AliasType type, AliasReason reason, String value, String... replacement) {
        return supplemental(
                "metadata/alias/%sAlias[@type=\"%s\"][@replacement=\"%s\"][@reason=\"%s\"]",
                lower(type), value, String.join(" ", replacement), lower(reason));
    }

    /** Returns a CldrValue for either a {@code <likelySubtags>} element. */
    static CldrValue likelySubtag(String from, String to) {
        return supplemental(
                "likelySubtags/likelySubtag[@from=\"%s\"][@to=\"%s\"]",
                from, to);
    }

    /** Returns a CldrValue for a {@code <territoryContainment>} group element. */
    static CldrValue territoryGroup(String region, String... subregions) {
        return supplemental(
                "territoryContainment/group[@type=\"%s\"][@contains=\"%s\"]",
                region, String.join(" ", subregions));
    }

    /**
     * Returns a CldrValue for a {@code <territoryContainment>} group element where
     * {@code @status="group"}.
     */
    static CldrValue territoryGrouping(String region, String... subregions) {
        return supplemental(
                "territoryContainment/group[@type=\"%s\"][@contains=\"%s\"][@status=\"group\"]",
                region, String.join(" ", subregions));
    }

    /**
     * Returns a CldrValue for a {@code <territoryContainment>} group element where
     * {@code @status="deprecated"}.
     */
    static CldrValue deprecatedTerritory(String region, String... subregions) {
        return supplemental(
                "territoryContainment/group[@type=\"%s\"][@contains=\"%s\"][@status=\"deprecated\"]",
                region, String.join(" ", subregions));
    }

    /**
     * Returns a map from expanded Trie keys to mapped value. This is useful in allowing
     * tests to use human readable data when testing Tries.
     *
     * @param star a string representing the Trie wildcard in the output keys, which for
     *             readability differs between use cases (e.g. "*" for subtags and "*-*"
     *             for match rules).
     * @param fn a function to map the actual Trie value to a more readable value for
     *           testing.
     */
    static <T> ImmutableMap<String, T> getTrieTable(BytesTrie trie, String star, Function<Integer, T> fn) {
        // Mostly copied from LocaleDistance (since the necessary constructor is private).
        // Main change is the this no longer uses a TreeMap, since we want to test order.
        ImmutableMap.Builder<String, T> map = ImmutableMap.builder();
        StringBuilder sb = new StringBuilder();
        for (BytesTrie.Entry entry : trie) {
            sb.setLength(0);
            int length = entry.bytesLength();
            for (int i = 0; i < length; ++i) {
                byte b = entry.byteAt(i);
                if (b == '*') {
                    sb.append(star).append('-');
                } else if (b >= 0) {
                    sb.append((char) b);
                } else {  // end of subtag (high bit set)
                    sb.append((char) (b & 0x7f)).append('-');
                }
            }
            assert sb.length() > 0 && sb.charAt(sb.length() - 1) == '-';
            sb.setLength(sb.length() - 1);
            map.put(sb.toString(), fn.apply(entry.value));
        }
        return map.build();
    }

    private static CldrValue supplemental(String path, Object... args) {
        return CldrValue.parseValue(String.format("//supplementalData/" + path, args), "");
    }

    private static String lower(Enum<?> value) {
        return Ascii.toLowerCase(value.name());
    }

    private TestData() {}
}
