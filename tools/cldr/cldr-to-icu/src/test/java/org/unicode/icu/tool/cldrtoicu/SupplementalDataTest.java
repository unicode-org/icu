// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth8.assertThat;
import static org.unicode.cldr.api.CldrValue.parseValue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.unicode.cldr.api.CldrDataSupplier;
import org.unicode.cldr.api.CldrValue;
import org.unicode.cldr.tool.LikelySubtags;
import org.unicode.cldr.util.LanguageTagCanonicalizer;
import org.unicode.cldr.util.LocaleIDParser;
import org.unicode.cldr.util.SupplementalDataInfo;
import org.unicode.icu.tool.cldrtoicu.testing.FakeDataSupplier;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

/**
 * Unit tests for the supplemental data API. These tests either use fake data for unit testing, or
 * compare behaviour between this API and the equivalent CLDR utility tool for regression testing.
 */
@RunWith(JUnit4.class)
public class SupplementalDataTest {
    private static SupplementalData regressionData;
    private static LikelySubtags likelySubtags;

    @BeforeClass
    public static void loadRegressionData() {
        Path cldrRoot = Paths.get(System.getProperty("CLDR_DIR"));
        regressionData = SupplementalData.create(CldrDataSupplier.forCldrFilesIn(cldrRoot));
        likelySubtags = new LikelySubtags();
    }

    @Test
    public void testGetParent_explicit() {
        // Locales with an explicit (non truncation) parent (a.k.a "English is weird").
        SupplementalData fakeData = fakeSupplementalData(parentLocales("en_001", "en_AU", "en_GB"));

        assertThat(fakeData.getExplicitParentLocaleOf("en_GB")).hasValue("en_001");
        assertThat(fakeData.getExplicitParentLocaleOf("en_AU")).hasValue("en_001");
        assertThat(fakeData.getExplicitParentLocaleOf("en_US")).isEmpty();
        assertThat(fakeData.getExplicitParentLocaleOf("en")).isEmpty();

        assertThat(fakeData.getParent("en_GB")).isEqualTo("en_001");
        assertThat(fakeData.getParent("en_AU")).isEqualTo("en_001");
        assertThat(fakeData.getParent("en_001")).isEqualTo("en");
        assertThat(fakeData.getParent("en_US")).isEqualTo("en");
        assertThat(fakeData.getParent("en")).isEqualTo("root");

    }

    @Test
    public void testGetParent_likelyScript() {
        // To figure out default scripts we use likely subtags.
        SupplementalData fakeData = fakeSupplementalData(likelySubtag("zh", "zh_Hans_CN"));

        // When removing a non-default script, the parent become "root".
        assertThat(fakeData.getParent("zh_Hant")).isEqualTo("root");
        // "Hans" is recognized as the default script, so the parent is obtained via truncation.
        assertThat(fakeData.getParent("zh_Hans")).isEqualTo("zh");
    }

    @Test
    public void testMaximize() {
        SupplementalData fakeData = fakeSupplementalData(
            likelySubtag("en", "en_Latn_US"),
            likelySubtag("pt", "pt_Latn_BR"),
            likelySubtag("und", "en_Latn_US"));

        // You cannot maximize "root".
        assertThat(fakeData.maximize("root")).isEmpty();
        // Existing subtags preserved.
        assertThat(fakeData.maximize("en")).hasValue("en_Latn_US");
        assertThat(fakeData.maximize("en_GB")).hasValue("en_Latn_GB");
        assertThat(fakeData.maximize("en_VARIANT")).hasValue("en_Latn_US_VARIANT");
        // Some other similar examples.
        assertThat(fakeData.maximize("pt")).hasValue("pt_Latn_BR");
        assertThat(fakeData.maximize("pt_PT")).hasValue("pt_Latn_PT");
        assertThat(fakeData.maximize("und")).hasValue("en_Latn_US");
    }

    @Test
    public void testReplaceDeprecatedTags_iAmRoot() {
        SupplementalData fakeData = fakeSupplementalData();
        assertThat(fakeData.replaceDeprecatedTags("root")).isEqualTo("root");
    }

    @Test
    public void testReplaceDeprecatedTags_sameSubtags() {
        SupplementalData fakeData = fakeSupplementalData(likelySubtag("en", "en_Latn_US"));

        // Replacement does not minimize or maximize results (even though "Latn" is likely).
        assertThat(fakeData.replaceDeprecatedTags("en_Latn_GB")).isEqualTo("en_Latn_GB");
        assertThat(fakeData.replaceDeprecatedTags("en_GB")).isEqualTo("en_GB");
    }

    @Test
    public void testReplaceDeprecatedTags_subtagReplacement() {
        SupplementalData fakeData = fakeSupplementalData(
            languageAlias("cym", "cy"),
            scriptAlias("Qaai", "Zinh"),
            territoryAlias("YU", "RS"));

        // Region is deprecated
        assertThat(fakeData.replaceDeprecatedTags("en_YU")).isEqualTo("en_RS");
        // Script is deprecated
        assertThat(fakeData.replaceDeprecatedTags("ar_Qaai_IR")).isEqualTo("ar_Zinh_IR");
        // Language is deprecated
        assertThat(fakeData.replaceDeprecatedTags("cym_GB")).isEqualTo("cy_GB");
    }

    @Test
    public void testReplaceDeprecatedTags_complex() {
        SupplementalData fakeData = fakeSupplementalData(
            languageAlias("sh", "sr_Latn"),
            languageAlias("zh_TW", "zh_Hant_TW"),
            languageAlias("tzm_Latn_MA", "tzm_MA"),
            territoryAlias("YU", "RS"),
            likelySubtag("sr", "sr_Cyrl_RS"),
            likelySubtag("zh_Hant", "zh_Hant_TW"));

        // "sh" -> "sr_Latn", taking precedence over the fact that "sr" maximizes to "sr_Cyrl_RS".
        assertThat(fakeData.replaceDeprecatedTags("sh_YU")).isEqualTo("sr_Latn_RS");
        // Alias lookup can add tags however depending on the situation.
        assertThat(fakeData.replaceDeprecatedTags("zh_TW")).isEqualTo("zh_Hant_TW");
        // But it will NOT remove tags (even though the languageAlias table contains an entry from
        // "tzm_Latn_MA" to "tzm_MA").
        assertThat(fakeData.replaceDeprecatedTags("tzm_Latn_MA")).isEqualTo("tzm_Latn_MA");
    }

    @Test
    public void testGetDefaultCalendar() {
        SupplementalData fakeData = fakeSupplementalData(
            defaultCalendar("gregorian", "001"),
            defaultCalendar("persian", "AF"),
            likelySubtag("uz", "uz_Latn_UZ"),
            likelySubtag("uz_AF", "uz_Arab_AF"),
            likelySubtag("uz_Arab", "uz_Arab_AF"));
        assertThat(fakeData.getDefaultCalendar("root")).hasValue("gregorian");
        // Empty because "gregorian" is the default found in the parent locale.
        assertThat(fakeData.getDefaultCalendar("en_US")).isEmpty();
        assertThat(fakeData.getDefaultCalendar("uz")).isEmpty();
        assertThat(fakeData.getDefaultCalendar("uz_AF")).hasValue("persian");
        assertThat(fakeData.getDefaultCalendar("uz_Arab")).hasValue("persian");
        // Empty because "uz_Arab" defines the persian calendar.
        assertThat(fakeData.getDefaultCalendar("uz_Arab_AF")).isEmpty();
    }

    @Test
    public void testGetDefaultCalendar_secretHacks() {
        SupplementalData fakeData = fakeSupplementalData(
            defaultCalendar("gregorian", "001"),
            likelySubtag("ja", "ja_Jpan_JP"),
            likelySubtag("th", "th_Thai_TH"));
        // Empty because "gregorian" is the default found in the parent locale.
        assertThat(fakeData.getDefaultCalendar("ja_US")).isEmpty();
        assertThat(fakeData.getDefaultCalendar("ja")).isEmpty();

        // Traditional calendars for a region cannot be represented via the territory-only based
        // CLDR data calendar mapping, so they exist as hard coded "hacks" in SupplementalData.
        // They could be pulled out into the configuration API, but they should ideally just be
        // derived from CLDR data directly.
        assertThat(fakeData.getDefaultCalendar("ja_JP_TRADITIONAL")).hasValue("japanese");
        assertThat(fakeData.getDefaultCalendar("ja_TRADITIONAL")).hasValue("japanese");
        assertThat(fakeData.getDefaultCalendar("th_TH_TRADITIONAL")).hasValue("buddhist");
        assertThat(fakeData.getDefaultCalendar("th_TRADITIONAL")).hasValue("buddhist");
    }

    @Test
    public void testGetParent_regression() {
        for (String id : TEST_LOCALE_IDS) {
            assertWithMessage("id=%s", id)
                .that(getIdChain(id, regressionData::getParent))
                .isEqualTo(getIdChain(id, LocaleIDParser::getParent));
        }
    }

    @Test
    public void testMaximize_regression() {
        for (String id : TEST_LOCALE_IDS) {
            assertWithMessage("id=%s", id)
                .that(regressionData.maximize(id).orElse(null))
                .isEqualTo(likelySubtags.maximize(id));
        }
    }

    @Test
    public void testReplaceDeprecatedTags_regression() {
        LanguageTagCanonicalizer ltc = new LanguageTagCanonicalizer();
        for (String id : TEST_LOCALE_IDS) {
            // Work around:
            // https://unicode-org.atlassian.net/projects/CLDR/issues/CLDR-13194
            try {
                ltc.transform(id);
            } catch (NullPointerException e) {
                // Occurs for sh_CS and sh_YU.
                continue;
            }
            // Need to maximize to work around:
            // https://unicode-org.atlassian.net/projects/CLDR/issues/CLDR-13196
            assertWithMessage("id=%s", id)
                .that(regressionData.maximize(regressionData.replaceDeprecatedTags(id)).orElse(null))
                .isEqualTo(likelySubtags.maximize(ltc.transform(id)));
        }
    }

    private static Iterable<String> getIdChain(String id, Function<String, String> fn) {
        List<String> chain = new ArrayList<>();
        while (!id.equals("root")) {
            chain.add(id);
            id = fn.apply(id);
        }
        chain.add(id);
        return chain;
    }

    private static final ImmutableSet<String> TEST_LOCALE_IDS = ImmutableSet.of(
        "af", "af_NA", "af_ZA", "agq", "agq_CM", "ak", "ak_GH", "am", "am_ET", "ar", "ar_001",
        "ar_AE", "ar_BH", "ar_DJ", "ar_DZ", "ar_EG", "ar_EH", "ar_ER", "ar_IL", "ar_IQ", "ar_JO",
        "ar_KM", "ar_KW", "ar_LB", "ar_LY", "ar_MA", "ar_MR", "ar_OM", "ar_PS", "ar_QA", "ar_SA",
        "ar_SD", "ar_SO", "ar_SS", "ar_SY", "ar_TD", "ar_TN", "ar_YE", "ars", "as", "as_IN",
        "asa", "asa_TZ", "ast", "ast_ES", "az", "az_AZ", "az_Cyrl", "az_Cyrl_AZ", "az_Latn",
        "az_Latn_AZ", "bas", "bas_CM", "be", "be_BY", "bem", "bem_ZM", "bez", "bez_TZ", "bg",
        "bg_BG", "bm", "bm_ML", "bn", "bn_BD", "bn_IN", "bo", "bo_CN", "bo_IN", "br", "br_FR",
        "brx", "brx_IN", "bs", "bs_Cyrl", "bs_Cyrl_BA", "bs_Latn", "bs_Latn_BA", "bs_BA", "ca",
        "ca_AD", "ca_ES", "ca_FR", "ca_IT", "ccp", "ccp_BD", "ccp_IN", "ce", "ce_RU", "ceb",
        "ceb_PH", "cgg", "cgg_UG", "chr", "chr_US", "ckb", "ckb_IQ", "ckb_IR", "cs", "cs_CZ", "cy",
        "cy_GB", "da", "da_DK", "da_GL", "dav", "dav_KE", "de", "de_AT", "de_BE", "de_CH", "de_DE",
        "de_IT", "de_LI", "de_LU", "dje", "dje_NE", "dsb", "dsb_DE", "dua", "dua_CM", "dyo",
        "dyo_SN", "dz", "dz_BT", "ebu", "ebu_KE", "ee", "ee_GH", "ee_TG", "el", "el_CY", "el_GR",
        "en", "en_001", "en_150", "en_AE", "en_AG", "en_AI", "en_AS", "en_AT", "en_AU", "en_BB",
        "en_BE", "en_BI", "en_BM", "en_BS", "en_BW", "en_BZ", "en_CA", "en_CC", "en_CH", "en_CK",
        "en_CM", "en_CX", "en_CY", "en_DE", "en_DG", "en_DK", "en_DM", "en_ER", "en_FI", "en_FJ",
        "en_FK", "en_FM", "en_GB", "en_GD", "en_GG", "en_GH", "en_GI", "en_GM", "en_GU", "en_GY",
        "en_HK", "en_IE", "en_IL", "en_IM", "en_IN", "en_IO", "en_JE", "en_JM", "en_KE", "en_KI",
        "en_KN", "en_KY", "en_LC", "en_LR", "en_LS", "en_MG", "en_MH", "en_MO", "en_MP", "en_MS",
        "en_MT", "en_MU", "en_MW", "en_MY", "en_NA", "en_NF", "en_NG", "en_NL", "en_NR", "en_NU",
        "en_NZ", "en_PG", "en_PH", "en_PK", "en_PN", "en_PR", "en_PW", "en_RH", "en_RW", "en_SB",
        "en_SC", "en_SD", "en_SE", "en_SG", "en_SH", "en_SI", "en_SL", "en_SS", "en_SX", "en_SZ",
        "en_TC", "en_TK", "en_TO", "en_TT", "en_TV", "en_TZ", "en_UG", "en_UM", "en_US",
        "en_US_POSIX", "en_VC", "en_VG", "en_VI", "en_VU", "en_WS", "en_ZA", "en_ZM", "en_ZW", "eo",
        "eo_001", "es", "es_003", "es_419", "es_AR", "es_BO", "es_BR", "es_BZ", "es_CL", "es_CO",
        "es_CR", "es_CU", "es_DO", "es_EA", "es_EC", "es_ES", "es_GQ", "es_GT", "es_HN", "es_IC",
        "es_MX", "es_NI", "es_PA", "es_PE", "es_PH", "es_PR", "es_PY", "es_SV", "es_US", "es_UY",
        "es_VE", "et", "et_EE", "eu", "eu_ES", "ewo", "ewo_CM", "fa", "fa_AF", "fa_IR", "ff",
        "ff_CM", "ff_GN", "ff_Latn", "ff_Latn_BF", "ff_Latn_CM", "ff_Latn_GH", "ff_Latn_GM",
        "ff_Latn_GN", "ff_Latn_GW", "ff_Latn_LR", "ff_Latn_MR", "ff_Latn_NE", "ff_Latn_NG",
        "ff_Latn_SL", "ff_Latn_SN", "ff_MR", "ff_SN", "fi", "fi_FI", "fil", "fil_PH", "fo", "fo_DK",
        "fo_FO", "fr", "fr_BE", "fr_BF", "fr_BI", "fr_BJ", "fr_BL", "fr_CA", "fr_CD", "fr_CF",
        "fr_CG", "fr_CH", "fr_CI", "fr_CM", "fr_DJ", "fr_DZ", "fr_FR", "fr_GA", "fr_GF", "fr_GN",
        "fr_GP", "fr_GQ", "fr_HT", "fr_KM", "fr_LU", "fr_MA", "fr_MC", "fr_MF", "fr_MG", "fr_ML",
        "fr_MQ", "fr_MR", "fr_MU", "fr_NC", "fr_NE", "fr_PF", "fr_PM", "fr_RE", "fr_RW", "fr_SC",
        "fr_SN", "fr_SY", "fr_TD", "fr_TG", "fr_TN", "fr_VU", "fr_WF", "fr_YT", "fur", "fur_IT",
        "fy", "fy_NL", "ga", "ga_IE", "gd", "gd_GB", "gl", "gl_ES", "gsw", "gsw_CH", "gsw_FR",
        "gsw_LI", "gu", "gu_IN", "guz", "guz_KE", "gv", "gv_IM", "ha", "ha_GH", "ha_NE", "ha_NG",
        "haw", "haw_US", "he", "he_IL", "hi", "hi_IN", "hr", "hr_BA", "hr_HR", "hsb", "hsb_DE",
        "hu", "hu_HU", "hy", "hy_AM", "ia", "ia_001", "id", "id_ID", "ig", "ig_NG", "ii", "ii_CN",
        "in", "in_ID", "is", "is_IS", "it", "it_CH", "it_IT", "it_SM", "it_VA", "iw", "iw_IL", "ja",
        "ja_JP", "jgo", "jgo_CM", "jmc", "jmc_TZ", "jv", "jv_ID", "ka", "ka_GE", "kab", "kab_DZ",
        "kam", "kam_KE", "kde", "kde_TZ", "kea", "kea_CV", "khq", "khq_ML", "ki", "ki_KE", "kk",
        "kk_KZ", "kkj", "kkj_CM", "kl", "kl_GL", "kln", "kln_KE", "km", "km_KH", "kn", "kn_IN",
        "ko", "ko_KP", "ko_KR", "kok", "kok_IN", "ks", "ks_IN", "ksb", "ksb_TZ", "ksf", "ksf_CM",
        "ksh", "ksh_DE", "ku", "ku_TR", "kw", "kw_GB", "ky", "ky_KG", "lag", "lag_TZ", "lb",
        "lb_LU", "lg", "lg_UG", "lkt", "lkt_US", "ln", "ln_AO", "ln_CD", "ln_CF", "ln_CG", "lo",
        "lo_LA", "lrc", "lrc_IQ", "lrc_IR", "lt", "lt_LT", "lu", "lu_CD", "luo", "luo_KE", "luy",
        "luy_KE", "lv", "lv_LV", "mas", "mas_KE", "mas_TZ", "mer", "mer_KE", "mfe", "mfe_MU", "mg",
        "mg_MG", "mgh", "mgh_MZ", "mgo", "mgo_CM", "mi", "mi_NZ", "mk", "mk_MK", "ml", "ml_IN",
        "mn", "mn_MN", "mo", "mr", "mr_IN", "ms", "ms_BN", "ms_MY", "ms_SG", "mt", "mt_MT", "mua",
        "mua_CM", "my", "my_MM", "mzn", "mzn_IR", "naq", "naq_NA", "nb", "nb_NO", "nb_SJ", "nd",
        "nd_ZW", "nds", "nds_DE", "nds_NL", "ne", "ne_IN", "ne_NP", "nl", "nl_AW", "nl_BE", "nl_BQ",
        "nl_CW", "nl_NL", "nl_SR", "nl_SX", "nmg", "nmg_CM", "nn", "nn_NO", "nnh", "nnh_CM", "no",
        "no_NO", "nus", "nus_SS", "nyn", "nyn_UG", "om", "om_ET", "om_KE", "or", "or_IN", "os",
        "os_GE", "os_RU", "pa", "pa_Arab", "pa_Arab_PK", "pa_Guru", "pa_Guru_IN", "pa_IN", "pa_PK",
        "pl", "pl_PL", "ps", "ps_AF", "ps_PK", "pt", "pt_AO", "pt_BR", "pt_CH", "pt_CV", "pt_GQ",
        "pt_GW", "pt_LU", "pt_MO", "pt_MZ", "pt_PT", "pt_ST", "pt_TL", "qu", "qu_BO", "qu_EC",
        "qu_PE", "rm", "rm_CH", "rn", "rn_BI", "ro", "ro_MD", "ro_RO", "rof", "rof_TZ", "ru",
        "ru_BY", "ru_KG", "ru_KZ", "ru_MD", "ru_RU", "ru_UA", "rw", "rw_RW", "rwk", "rwk_TZ", "sah",
        "sah_RU", "saq", "saq_KE", "sbp", "sbp_TZ", "sd", "sd_PK", "se", "se_FI", "se_NO", "se_SE",
        "seh", "seh_MZ", "ses", "ses_ML", "sg", "sg_CF", "sh", "sh_BA", "sh_CS", "sh_YU", "shi",
        "shi_Latn", "shi_Latn_MA", "shi_Tfng", "shi_Tfng_MA", "shi_MA", "si", "si_LK", "sk",
        "sk_SK", "sl", "sl_SI", "smn", "smn_FI", "sn", "sn_ZW", "so", "so_DJ", "so_ET", "so_KE",
        "so_SO", "sq", "sq_AL", "sq_MK", "sq_XK", "sr", "sr_Cyrl", "sr_Cyrl_BA", "sr_Cyrl_ME",
        "sr_Cyrl_RS", "sr_Cyrl_CS", "sr_Cyrl_XK", "sr_Cyrl_YU", "sr_Latn", "sr_Latn_BA",
        "sr_Latn_ME", "sr_Latn_RS", "sr_Latn_CS", "sr_Latn_XK", "sr_Latn_YU", "sr_BA", "sr_ME",
        "sr_RS", "sr_CS", "sr_YU", "sv", "sv_AX", "sv_FI", "sv_SE", "sw", "sw_CD", "sw_KE", "sw_TZ",
        "sw_UG", "ta", "ta_IN", "ta_LK", "ta_MY", "ta_SG", "te", "te_IN", "teo", "teo_KE", "teo_UG",
        "tg", "tg_TJ", "th", "th_TH", "ti", "ti_ER", "ti_ET", "tk", "tk_TM", "tl", "tl_PH", "to",
        "to_TO", "tr", "tr_CY", "tr_TR", "tt", "tt_RU", "twq", "twq_NE", "tzm", "tzm_MA", "ug",
        "ug_CN", "uk", "uk_UA", "ur", "ur_IN", "ur_PK", "uz", "uz_AF", "uz_Arab", "uz_Arab_AF",
        "uz_Cyrl", "uz_Cyrl_UZ", "uz_Latn", "uz_Latn_UZ", "uz_UZ", "vai", "vai_Latn", "vai_Latn_LR",
        "vai_LR", "vai_Vaii", "vai_Vaii_LR", "vi", "vi_VN", "vun", "vun_TZ", "wae", "wae_CH", "wo",
        "wo_SN", "xh", "xh_ZA", "xog", "xog_UG", "yav", "yav_CM", "yi", "yi_001", "yo", "yo_BJ",
        "yo_NG", "yue", "yue_Hans", "yue_Hans_CN", "yue_Hant", "yue_Hant_HK", "zgh", "zgh_MA", "zh",
        "zh_Hans", "zh_Hans_CN", "zh_Hans_HK", "zh_Hans_MO", "zh_Hans_SG", "zh_Hant", "zh_Hant_HK",
        "zh_Hant_MO", "zh_Hant_TW", "zh_CN", "zh_HK", "zh_MO", "zh_SG", "zh_TW", "zu", "zu_ZA");

    private static CldrValue parentLocales(String parent, String... locales) {
        return supplementalData(
            "parentLocales/parentLocale[@parent=\"%s\"][@locales=\"%s\"]",
            parent, Joiner.on(' ').join(locales));
    }

    private static CldrValue defaultCalendar(String calendar, String... territories) {
        return supplementalData(
            "calendarPreferenceData/calendarPreference[@territories=\"%s\"][@ordering=\"%s\"]",
            Joiner.on(' ').join(territories), calendar);
    }

    private static CldrValue likelySubtag(String from, String to) {
        return supplementalData(
            "likelySubtags/likelySubtag[@from=\"%s\"][@to=\"%s\"]", from, to);
    }

    private static CldrValue languageAlias(String type, String replacement) {
        return supplementalData(
            "metadata/alias/languageAlias[@type=\"%s\"][@replacement=\"%s\"]", type, replacement);
    }

    private static CldrValue scriptAlias(String type, String replacement) {
        return supplementalData(
            "metadata/alias/scriptAlias[@type=\"%s\"][@replacement=\"%s\"]", type, replacement);
    }

    private static CldrValue territoryAlias(String type, String replacement) {
        return supplementalData(
            "metadata/alias/territoryAlias[@type=\"%s\"][@replacement=\"%s\"]", type, replacement);
    }

    private static CldrValue supplementalData(String path, Object... args) {
        return parseValue(String.format("//supplementalData/" + path, args), "");
    }

    private static SupplementalData fakeSupplementalData(CldrValue... values) {
        return SupplementalData.create(new FakeDataSupplier().addSupplementalData(values));
    }
}
