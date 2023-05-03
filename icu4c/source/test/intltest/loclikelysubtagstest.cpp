// © 2023 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
#include "loclikelysubtagstest.h"

#include "loclikelysubtags.h"
#include "lsr.h"

XLikelySubtagsTest::XLikelySubtagsTest()
{
    UErrorCode status = U_ZERO_ERROR;
    service = XLikelySubtags::XLikelySubtags::getSingleton(status);
    U_ASSERT(U_SUCCESS(status));
}

XLikelySubtagsTest::~XLikelySubtagsTest()
{

}

void XLikelySubtagsTest::runIndexedTest( int32_t index, UBool exec, const char* &name, char* /*par*/ )
{
    TESTCASE_AUTO_BEGIN;
    TESTCASE_AUTO(TestBasic);
    TESTCASE_AUTO_END;
}

void XLikelySubtagsTest::assertLSR(UnicodeString msg, const LSR& expected, const LSR& actual) {
    assertEquals(msg + u".language", expected.language, actual.language);
    assertEquals(msg + u".script", expected.script, actual.script);
    assertEquals(msg + u".region", expected.region, actual.region);
}

void XLikelySubtagsTest::TestBasic() {
    UErrorCode status = U_ZERO_ERROR;
    // In https://unicode.org/reports/tr35/#Likely_Subtags
    // UTS35 mandate the lookup sequence as
    // Lookup. Look up each of the following in order, and stop on the first match:
    //  languageₛ_scriptₛ_regionₛ
    //  languageₛ_regionₛ
    //  languageₛ_scriptₛ
    //  languageₛ
    //  und_scriptₛ
    // If there is no match, either return
    //  the match for "und" (in APIs where a valid language tag is required).
    static const struct {
        const char* const locale;
        const char* const language;
        const char* const script;
        const char* const region;
    } test_data[] = {
        // Basic test
        // Test language alias work before matching
        // <languageAlias type="in" replacement="id" reason="deprecated"/> <!-- Indonesian -->
        // ...
        // <likelySubtag from="id" to="id_Latn_ID"/>
        { "in", "id", "Latn", "ID" },
        // Test territory alias work before matching
        // <territoryAlias type="BU" replacement="MM" reason="deprecated"/> <!-- Burma -->
        // ...
        // <likelySubtag from="und_MM" to="my_Mymr_MM"/>
        { "und_BU", "my", "Mymr", "MM" },
        // Test language and territory alias work before matching
        // <languageAlias type="in" replacement="id" reason="deprecated"/> <!-- Indonesian -->
        // <territoryAlias type="BU" replacement="MM" reason="deprecated"/> <!-- Burma -->
        // ...
        // <likelySubtag from="id" to="id_Latn_ID"/>
        { "in_BU", "id", "Latn", "MM" },
        // Test language and script alias work before matching
        { "in_Qaai", "id", "Zinh", "ID" },
        // Test script alias replacement not matching resulted in matching for
        // und.
        { "und_Qaai", "en", "Latn", "US" },
        // Test und with region match
        // <likelySubtag from="und_002" to="en_Latn_NG"/>
        { "und_002", "en", "Latn", "002" },
        // Test three letter territory alias replacement.
        // <territoryAlias type="ESH" replacement="EH" reason="overlong"/> <!-- Western Sahara -->
        // <likelySubtag from="und_EH" to="ar_Arab_EH"/>
        { "und_ESH", "ar", "Arab", "EH" },
        { "und_Zzzz_ESH", "ar", "Arab", "EH" },
        // Test three letter territory alias replacement and
        // language_region is looked before language_script
        // <territoryAlias type="ESH" replacement="EH" reason="overlong"/> <!-- Western Sahara -->
        // <likelySubtag from="und_EH" to="ar_Arab_EH"/>
        { "und_Thai_ESH", "ar", "Thai", "EH" },
        // Test matching language_script
        // <likelySubtag from="und_Adlm" to="ff_Adlm_GN"/>
        { "und_Adlm", "ff", "Adlm", "GN" },
        // Test matching language_script
        // <likelySubtag from="und_Adlm" to="ff_Adlm_GN"/>
        { "und_Adlm_AC", "ff", "Adlm", "AC" },
        // Test matching language_script
        // <territoryAlias type="ASC" replacement="AC" reason="overlong"/> <!-- Ascension Island -->
        // <likelySubtag from="und_Adlm" to="ff_Adlm_GN"/>
        { "und_Adlm_ASC", "ff", "Adlm", "AC" },
        // test language_region is looked before language_script
        // <likelySubtag from="und_TW" to="zh_Hant_TW"/>
        { "und_Adlm_TW", "zh", "Adlm", "TW" },
        // test language_region is looked before language_script
        // <territoryAlias type="TWN" replacement="TW" reason="overlong"/> <!-- Taiwan -->
        // <likelySubtag from="und_TW" to="zh_Hant_TW"/>
        { "und_Adlm_TWN", "zh", "Adlm", "TW" },
        // test language
        // <likelySubtag from="bwe" to="bwe_Mymr_MM" origin="sil1"/>	<!-- Bwe Karen ➡︎ Bwe Karen (Myanmar, Myanmar [Burma]) -->
        { "bwe", "bwe", "Mymr", "MM" },
        // test language but keep script.
        // <likelySubtag from="bwe" to="bwe_Mymr_MM" origin="sil1"/>	<!-- Bwe Karen ➡︎ Bwe Karen (Myanmar, Myanmar [Burma]) -->
        { "bwe_Thai", "bwe", "Thai", "MM" },
        // test language but keep territory.
        // <likelySubtag from="bwe" to="bwe_Mymr_MM" origin="sil1"/>	<!-- Bwe Karen ➡︎ Bwe Karen (Myanmar, Myanmar [Burma]) -->
        { "bwe_VN", "bwe", "Mymr", "VN" },
        // <territoryAlias type="VNM" replacement="VN" reason="overlong"/> <!-- Vietnam -->
        // test language but keep territory.
        // <territoryAlias type="VNM" replacement="VN" reason="overlong"/> <!-- Vietnam -->

        // <likelySubtag from="bwe" to="bwe_Mymr_MM" origin="sil1"/>	<!-- Bwe Karen ➡︎ Bwe Karen (Myanmar, Myanmar [Burma]) -->
        { "bwe_VNM", "bwe", "Mymr", "VN" },

        // test language but keep script and territory.
        // <likelySubtag from="bwe" to="bwe_Mymr_MM" origin="sil1"/>	<!-- Bwe Karen ➡︎ Bwe Karen (Myanmar, Myanmar [Burma]) -->
        { "bwe_Thai_VN", "bwe", "Thai", "VN" },
        // test language but keep script and territory.
        // <likelySubtag from="bwe" to="bwe_Mymr_MM" origin="sil1"/>	<!-- Bwe Karen ➡︎ Bwe Karen (Myanmar, Myanmar [Burma]) -->
        // <territoryAlias type="VNM" replacement="VN" reason="overlong"/> <!-- Vietnam -->
        { "bwe_Thai_VNM", "bwe", "Thai", "VN" },

        // Match
        // <likelySubtag from="und_ER" to="ti_Ethi_ER"/>
        { "und_Ethi_ER", "ti", "Ethi", "ER" },
        // Match
        // <likelySubtag from="und_HK" to="zh_Hant_HK"/>
        { "und_Latn_HK", "zh", "Hant", "HK" },
        // No matching for "art", Therefore return the match for "und"
        // <likelySubtag from="und" to="en_Latn_US"/>
        { "art", "en", "Latn", "US" },
        // No matching for "art", Therefore return the match for "und"
        // <likelySubtag from="und" to="en_Latn_US"/>
        { "art_ZZ", "en", "Latn", "US" },
        // No matching for "art", Therefore return the match for "und"
        // <likelySubtag from="und" to="en_Latn_US"/>
        { "art_CN", "en", "Latn", "US" },
        { "art_TW", "en", "Latn", "US" },
        { "art_HK", "en", "Latn", "US" },
        { "art_AQ", "en", "Latn", "US" },
        { "art_Zzzz_CN", "en", "Latn", "US" },
        { "art_Zzzz_TW", "en", "Latn", "US" },
        { "art_Zzzz_HK", "en", "Latn", "US" },
        { "art_Zzzz_AQ", "en", "Latn", "US" },
        // No matching for "art", Therefore return the match for "und"
        // <likelySubtag from="und" to="en_Latn_US"/>
        { "art_Latn", "en", "Latn", "US" },
        { "art_Latn_ZZ", "en", "Latn", "US" },
        // Match
        // <likelySubtag from="und_Hans" to="zh_Hans_CN"/>
        { "art_Hans", "art", "Hans", "CN" },
        { "art_Hans_ZZ", "art", "Hans", "CN" },
        // Match
        // <likelySubtag from="und_Hant" to="zh_Hant_TW"/>
        { "art_Hant", "art", "Hant", "TW" },
        { "art_Hant_ZZ", "art", "Hant", "TW" },
        // No matching for "art_Moon", Therefore return the match for "und"
        { "art_Moon", "en", "Latn", "US" },
        { "art_Moon_ZZ", "en", "Latn", "US" },
        // <likelySubtag from="und_150" to="ru_Cyrl_RU"/>
        { "und_150", "ru", "Cyrl", "150" },
    };
    for (const auto& test : test_data) {
        Locale l = Locale(test.locale);
        LSR actual = service->makeMaximizedLsrFrom(l, status);
        LSR expected(test.language, test.script, test.region, 0);
        assertLSR(UnicodeString(u"makeMaximizedLsrFrom(") + test.locale + ")", expected, actual);
    }
}


