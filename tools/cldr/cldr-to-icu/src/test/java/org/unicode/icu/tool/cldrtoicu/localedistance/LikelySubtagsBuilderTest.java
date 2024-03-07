// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu.localedistance;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasReason.DEPRECATED;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasReason.LEGACY;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasReason.OVERLONG;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasType.LANGUAGE;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.AliasType.TERRITORY;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.alias;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.likelySubtag;
import static org.unicode.icu.tool.cldrtoicu.localedistance.TestData.lsr;

import org.junit.Test;
import org.unicode.cldr.api.CldrData;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.impl.locale.LSR;
import com.ibm.icu.impl.locale.LikelySubtags;
import com.ibm.icu.util.BytesTrie;

public class LikelySubtagsBuilderTest {

    @Test
    public void testLanguageAliases() {
        LikelySubtags.Data subtags = LikelySubtagsBuilder.build(getTestData(
                // Minimum mapping (or else code complains).
                likelySubtag("und", "en_Latn_US"),

                alias(LANGUAGE, DEPRECATED, "in", "id"),
                alias(LANGUAGE, DEPRECATED, "mo", "ro"),
                // Overlong languages are ignored.
                alias(LANGUAGE, OVERLONG, "eng", "en"),
                // Non-simple languages with script, region or other extensions are ignored.
                alias(LANGUAGE, LEGACY, "zh_TW", "zh_Hant_TW"),
                alias(LANGUAGE, LEGACY, "i-default", "en-x-i-default")));

        assertThat(subtags.languageAliases).containsExactly("in", "id", "mo", "ro");
    }

    @Test
    public void testTerritoryAliases() {
        LikelySubtags.Data subtags = LikelySubtagsBuilder.build(getTestData(
                // Minimum mapping (or else code complains).
                likelySubtag("und", "en_Latn_US"),

                // When more than one replacement exists, take the first.
                alias(TERRITORY, DEPRECATED, "CS", "RS ME"),
                alias(TERRITORY, DEPRECATED, "UK", "GB"),
                // Overlong territories are ignored.
                alias(TERRITORY, OVERLONG, "eng", "en"),
                alias(TERRITORY, OVERLONG, "999", "ZZ")));

        assertThat(subtags.regionAliases).containsExactly("CS", "RS", "UK", "GB");
    }

    @Test
    public void testLikelySubtags() {
        LikelySubtags.Data subtags = LikelySubtagsBuilder.build(getTestData(
                likelySubtag("und", "en_Latn_US"),
                likelySubtag("en", "en_Latn_US"),
                likelySubtag("pt", "pt_Latn_BR"),
                likelySubtag("und_BR", "pt_Latn_BR"),
                likelySubtag("zh", "zh_Hans_CN"),
                likelySubtag("zh_TW", "zh_Hant_TW"),
                likelySubtag("zh_Hant", "zh_Hant_TW")));

        assertThat(subtags.lsrs).asList()
                .containsExactly(
                        // Special cases (these should never change).
                        lsr(""),
                        lsr("skip-script"),
                        // Locales mapped to by the likely subtags mappings (in order).
                        lsr("en-Latn-US"),
                        lsr("pt-Latn-BR"),
                        lsr("zh-Hans-CN"),
                        lsr("zh-Hant-TW"))
                .inOrder();

        // Order is by "subtag" (left-to-right) with lexicographical order of tags (other
        // than '*' which is always sorted first).
        // Results are mapped to their corresponding value in the LSRs list.
        assertThat(getTrieTable(subtags))
                .containsExactly(
                        "*-*-*", lsr("en-Latn-US"),
                        "*-*-BR", lsr("pt-Latn-BR"),
                        "*-Latn-*", lsr("en-Latn-US"),
                        "*-Latn-BR", lsr("pt-Latn-BR"),
                        "*-Latn-US", lsr("en-Latn-US"),
                        "en", lsr("en-Latn-US"),
                        "pt", lsr("pt-Latn-BR"),
                        "zh-*-*", lsr("zh-Hans-CN"),
                        "zh-*-TW", lsr("zh-Hant-TW"),
                        "zh-Hant", lsr("zh-Hant-TW"))
                .inOrder();
    }

    private static ImmutableMap<String, LSR> getTrieTable(LikelySubtags.Data subtags) {
        // We rebuild the Trie from the byte[] data.
        return TestData.getTrieTable(new BytesTrie(subtags.trie, 0), "*", i -> subtags.lsrs[i]);
    }

    private static CldrData getTestData(CldrValue... values) {
        return CldrDataSupplier.forValues(asList(values));
    }
}
