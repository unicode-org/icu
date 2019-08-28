// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.unicode.cldr.api.CldrDraftStatus;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;

/**
 * The converter config intended to generate the standard ICU data files. This used to be something
 * that was configured by text files such as "icu-locale-deprecates.xml" and "icu-config.
 */
public final class IcuConverterConfig implements LdmlConverterConfig {

    private static final Optional<Path> DEFAULT_CLDR_DIR =
        Optional.ofNullable(System.getProperty("CLDR_DIR", null))
            .map(d -> Paths.get(d).toAbsolutePath());

    private static final Optional<Path> DEFAULT_ICU_DIR =
        Optional.ofNullable(System.getProperty("ICU_DIR", null))
            .map(d -> Paths.get(d).toAbsolutePath());

    /** The builder with which to specify configuration for the {@link LdmlConverter}. */
    public static final class Builder {
        private Path cldrDir = DEFAULT_CLDR_DIR.orElse(null);
        private Path outputDir =
            DEFAULT_ICU_DIR.map(d -> d.resolve("icu4c/source/data")).orElse(null);
        private Path specialsDir =
            DEFAULT_ICU_DIR.map(d -> d.resolve("icu4c/source/data/xml")).orElse(null);;
        private ImmutableSet<OutputType> outputTypes = OutputType.ALL;
        private CldrDraftStatus minimalDraftStatus = CldrDraftStatus.CONTRIBUTED;
        private boolean emitReport = false;

        /**
         * Sets the CLDR base directory from which to load all CLDR data. This is optional if the
         * {@code CLDR_DIR} environment variable is set, which will be used instead.
         */
        public Builder setCldrDir(Path cldrDir) {
            this.cldrDir = checkNotNull(cldrDir.toAbsolutePath());
            return this;
        }

        /**
         * Sets the output directory in which the ICU data directories and files will go. This is
         * optional if the {@code ICU_DIR} system property is set, which will be used to generate
         * the path instead (i.e. {@code "icu4c/source/data"} inside the ICU release directory).
         */
        public Builder setOutputDir(Path outputDir) {
            this.outputDir = checkNotNull(outputDir);
            return this;
        }

        /**
         * Sets the "specials" directory containing additional ICU specific data to be processed.
         * This is optional if the {@code ICU_DIR} system property is set, which will be used to
         * generate the path instead (i.e. {@code "icu4c/source/data/xml"} inside the ICU release
         * directory).
         */
        public Builder setSpecialsDir(Path specialsDir) {
            this.specialsDir = checkNotNull(specialsDir);
            return this;
        }

        /**
         * Sets the output types which will be converted. This is optional and defaults to {@link
         * OutputType#ALL}.
         */
        public Builder setOutputTypes(Iterable<OutputType> types) {
            this.outputTypes = ImmutableSet.copyOf(types);
            return this;
        }

        /**
         * Sets the minimum draft status for CLDR data to be converted (paths below this status are
         * ignored during conversion). This is optional and defaults to {@link
         * CldrDraftStatus#CONTRIBUTED}.
         */
        public Builder setMinimalDraftStatus(CldrDraftStatus minimalDraftStatus) {
            this.minimalDraftStatus = checkNotNull(minimalDraftStatus);
            return this;
        }

        public Builder setEmitReport(boolean emitReport) {
            this.emitReport = emitReport;
            return this;
        }

        /** Returns a converter config from the current builder state. */
        public LdmlConverterConfig build() {
            return new IcuConverterConfig(this);
        }
    }

    private final Path cldrDir;
    private final Path outputDir;
    private final Path specialsDir;
    private final ImmutableSet<OutputType> outputTypes;
    private final CldrDraftStatus minimalDraftStatus;
    private final boolean emitReport;

    private IcuConverterConfig(Builder builder) {
        this.cldrDir = checkNotNull(builder.cldrDir,
            "must set a CLDR directory, or the CLDR_DIR system property");
        if (DEFAULT_CLDR_DIR.isPresent() && !this.cldrDir.equals(DEFAULT_CLDR_DIR.get())) {
            System.err.format(
                "Warning: Specified CLDR base directory does not appear to match the"
                    + " directory inferred by the 'CLDR_DIR' system property.\n"
                    + "Specified: %s\n"
                    + "Inferred: %s\n",
                this.cldrDir, DEFAULT_CLDR_DIR.get());
        }
        this.outputDir = checkNotNull(builder.outputDir);
        checkArgument(!Files.isRegularFile(outputDir),
            "specified output directory if not a directory: %s", outputDir);
        this.specialsDir = checkNotNull(builder.specialsDir,
            "must specify a 'specials' XML directory");
        checkArgument(Files.isDirectory(specialsDir),
            "specified specials directory does not exist: %s", specialsDir);
        this.outputTypes = builder.outputTypes;
        checkArgument(!this.outputTypes.isEmpty(),
            "must specify at least one output type to be generated (possible values are: %s)",
            Arrays.asList(OutputType.values()));
        this.minimalDraftStatus = builder.minimalDraftStatus;
        this.emitReport = builder.emitReport;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override public Path getCldrDirectory() {
        return cldrDir;
    }

    @Override public Path getOutputDir() {
        return outputDir;
    }

    @Override public Set<OutputType> getOutputTypes() {
        return outputTypes;
    }

    @Override public CldrDraftStatus getMinimumDraftStatus() {
        return minimalDraftStatus;
    }

    @Override public Path getSpecialsDir() {
        return specialsDir;
    }

    @Override public boolean emitReport() {
        return emitReport;
    }

    // Currently hard-coded "hacks" which could be encoded via the builder if wanted.

    @Override public Map<String, String> getForcedAliases(IcuLocaleDir dir) {
        switch (dir) {
        case COLL:
            return ImmutableMap.<String, String>builder()
                // It is not at all clear why this is being done (we expect "sr_Latn_ME" normally).
                // TODO: Find out and document this properly.
                .put("sr_ME", "sr_Cyrl_ME")

                // This appears to be a hack to avoid needing to copy and maintain the same "zh"
                // data for "yue". The files for "yue" in this directory should be empty otherwise.
                //
                // The maximized versions of "yue_Hans" is "yue_Hans_CN" (vs "zh_Hans_CN"), and for
                // "yue" it's "yue_Hant_HK" (vs "zh_Hant_HK"), so the aliases are effectively just
                // rewriting the base language.
                .put("yue_Hans", "zh_Hans")
                .put("yue", "zh_Hant")
                .build();
        case RBNF:
            // It is not at all clear why this is being done. It's certainly not exactly the same
            // as above, since (a) the alias is reversed (b) "zh_Hant" does exist, with different
            // data than "yue", so this alias is not just rewriting the base language.
            // TODO: Find out and document this properly.
            return ImmutableMap.of("zh_Hant_HK", "yue");
        default:
            return ImmutableMap.of();
        }
    }

    // This set of locale files in each directory denotes the supported/available locales for that
    // API. In most cases, it's the same set, but a few directories support only a subset of IDs.
    @Override public ImmutableSet<String> getTargetLocaleIds(IcuLocaleDir dir) {
        switch (dir) {
        case COLL:
            return COLL_LOCALE_IDS;
        case BRKITR:
            return BRKITR_LOCALE_IDS;
        case RBNF:
            return RBNF_LOCALE_IDS;
        default:
            return ICU_LOCALE_IDS;
        }
    }

    // The primary set of locale IDs to be generated. Other, directory specific, sets should be
    // subsets of this. Some of these ID are aliases, so XML files may not exist for all of them.
    //
    // This was further modified (in order to better match the set of generated ICU files) by:
    // * Removing "es_003" (which just seems to be ignored in current code)
    // * Adding:  "en_NH", "sr_XK", "yue_CN", "yue_HK" (deprecated locale IDs in the manual config)
    // * Adding: "no_NO_NY" (a not even structurally valid ID that exists for very legacy reasons)
    private static final ImmutableSet<String> ICU_LOCALE_IDS = ImmutableSet.of(
        "root",
        // A
        "af", "af_NA", "af_ZA", "agq", "agq_CM", "ak", "ak_GH", "am", "am_ET", "ar", "ar_001",
        "ar_AE", "ar_BH", "ar_DJ", "ar_DZ", "ar_EG", "ar_EH", "ar_ER", "ar_IL", "ar_IQ",
        "ar_JO", "ar_KM", "ar_KW", "ar_LB", "ar_LY", "ar_MA", "ar_MR", "ar_OM", "ar_PS",
        "ar_QA", "ar_SA", "ar_SD", "ar_SO", "ar_SS", "ar_SY", "ar_TD", "ar_TN", "ar_YE", "ars",
        "as", "as_IN", "asa", "asa_TZ", "ast", "ast_ES", "az", "az_AZ", "az_Cyrl", "az_Cyrl_AZ",
        "az_Latn", "az_Latn_AZ",
        // B
        "bas", "bas_CM", "be", "be_BY", "bem", "bem_ZM", "bez", "bez_TZ", "bg", "bg_BG", "bm",
        "bm_ML", "bn", "bn_BD", "bn_IN", "bo", "bo_CN", "bo_IN", "br", "br_FR", "brx", "brx_IN",
        "bs", "bs_Cyrl", "bs_Cyrl_BA", "bs_Latn", "bs_Latn_BA", "bs_BA",
        // C
        "ca", "ca_AD", "ca_ES", "ca_FR", "ca_IT", "ccp", "ccp_BD", "ccp_IN", "ce", "ce_RU",
        "ceb", "ceb_PH", "cgg", "cgg_UG", "chr", "chr_US", "ckb", "ckb_IQ", "ckb_IR", "cs",
        "cs_CZ", "cy", "cy_GB",
        // D
        "da", "da_DK", "da_GL", "dav", "dav_KE", "de", "de_AT", "de_BE", "de_CH", "de_DE",
        "de_IT", "de_LI", "de_LU", "dje", "dje_NE", "dsb", "dsb_DE", "dua", "dua_CM", "dyo",
        "dyo_SN", "dz", "dz_BT",
        // E
        "ebu", "ebu_KE", "ee", "ee_GH", "ee_TG", "el", "el_CY", "el_GR", "en", "en_001",
        "en_150", "en_AE", "en_AG", "en_AI", "en_AS", "en_AT", "en_AU", "en_BB", "en_BE",
        "en_BI", "en_BM", "en_BS", "en_BW", "en_BZ", "en_CA", "en_CC", "en_CH", "en_CK",
        "en_CM", "en_CX", "en_CY", "en_DE", "en_DG", "en_DK", "en_DM", "en_ER", "en_FI",
        "en_FJ", "en_FK", "en_FM", "en_GB", "en_GD", "en_GG", "en_GH", "en_GI", "en_GM",
        "en_GU", "en_GY", "en_HK", "en_IE", "en_IL", "en_IM", "en_IN", "en_IO", "en_JE",
        "en_JM", "en_KE", "en_KI", "en_KN", "en_KY", "en_LC", "en_LR", "en_LS", "en_MG",
        "en_MH", "en_MO", "en_MP", "en_MS", "en_MT", "en_MU", "en_MW", "en_MY", "en_NA",
        "en_NF", "en_NG", "en_NH", "en_NL", "en_NR", "en_NU", "en_NZ", "en_PG", "en_PH",
        "en_PK", "en_PN", "en_PR", "en_PW", "en_RH", "en_RW", "en_SB", "en_SC", "en_SD",
        "en_SE", "en_SG", "en_SH", "en_SI", "en_SL", "en_SS", "en_SX", "en_SZ", "en_TC",
        "en_TK", "en_TO", "en_TT", "en_TV", "en_TZ", "en_UG", "en_UM", "en_US", "en_US_POSIX",
        "en_VC", "en_VG", "en_VI", "en_VU", "en_WS", "en_ZA", "en_ZM", "en_ZW", "eo",
        "eo_001", "es", "es_419", "es_AR", "es_BO", "es_BR", "es_BZ", "es_CL", "es_CO",
        "es_CR", "es_CU", "es_DO", "es_EA", "es_EC", "es_ES", "es_GQ", "es_GT", "es_HN",
        "es_IC", "es_MX", "es_NI", "es_PA", "es_PE", "es_PH", "es_PR", "es_PY", "es_SV",
        "es_US", "es_UY", "es_VE", "et", "et_EE", "eu", "eu_ES", "ewo", "ewo_CM",
        // F
        "fa", "fa_AF", "fa_IR", "ff", "ff_CM", "ff_GN", "ff_Latn", "ff_Latn_BF", "ff_Latn_CM",
        "ff_Latn_GH", "ff_Latn_GM", "ff_Latn_GN", "ff_Latn_GW", "ff_Latn_LR", "ff_Latn_MR",
        "ff_Latn_NE", "ff_Latn_NG", "ff_Latn_SL", "ff_Latn_SN", "ff_MR", "ff_SN", "fi",
        "fi_FI", "fil", "fil_PH", "fo", "fo_DK", "fo_FO", "fr", "fr_BE", "fr_BF", "fr_BI",
        "fr_BJ", "fr_BL", "fr_CA", "fr_CD", "fr_CF", "fr_CG", "fr_CH", "fr_CI", "fr_CM",
        "fr_DJ", "fr_DZ", "fr_FR", "fr_GA", "fr_GF", "fr_GN", "fr_GP", "fr_GQ", "fr_HT",
        "fr_KM", "fr_LU", "fr_MA", "fr_MC", "fr_MF", "fr_MG", "fr_ML", "fr_MQ", "fr_MR",
        "fr_MU", "fr_NC", "fr_NE", "fr_PF", "fr_PM", "fr_RE", "fr_RW", "fr_SC", "fr_SN",
        "fr_SY", "fr_TD", "fr_TG", "fr_TN", "fr_VU", "fr_WF", "fr_YT", "fur", "fur_IT",
        "fy", "fy_NL",
        // G
        "ga", "ga_IE", "gd", "gd_GB", "gl", "gl_ES", "gsw", "gsw_CH", "gsw_FR", "gsw_LI",
        "gu", "gu_IN", "guz", "guz_KE", "gv", "gv_IM",
        // H
        "ha", "ha_GH", "ha_NE", "ha_NG", "haw", "haw_US", "he", "he_IL", "hi", "hi_IN",
        "hr", "hr_BA", "hr_HR", "hsb", "hsb_DE", "hu", "hu_HU", "hy", "hy_AM",
        // I
        "ia", "ia_001", "id", "id_ID", "ig", "ig_NG", "ii", "ii_CN", "in", "in_ID", "is",
        "is_IS", "it", "it_CH", "it_IT", "it_SM", "it_VA", "iw", "iw_IL",
        // J
        "ja", "ja_JP", "ja_JP_TRADITIONAL", "jgo", "jgo_CM", "jmc", "jmc_TZ", "jv", "jv_ID",
        // K
        "ka", "ka_GE", "kab", "kab_DZ", "kam", "kam_KE", "kde", "kde_TZ", "kea", "kea_CV",
        "khq", "khq_ML", "ki", "ki_KE", "kk", "kk_KZ", "kkj", "kkj_CM", "kl", "kl_GL", "kln",
        "kln_KE", "km", "km_KH", "kn", "kn_IN", "ko", "ko_KP", "ko_KR", "kok", "kok_IN",
        "ks", "ks_IN", "ksb", "ksb_TZ", "ksf", "ksf_CM", "ksh", "ksh_DE", "ku", "ku_TR",
        "kw", "kw_GB", "ky", "ky_KG",
        // L
        "lag", "lag_TZ", "lb", "lb_LU", "lg", "lg_UG", "lkt", "lkt_US", "ln", "ln_AO",
        "ln_CD", "ln_CF", "ln_CG", "lo", "lo_LA", "lrc", "lrc_IQ", "lrc_IR", "lt", "lt_LT",
        "lu", "lu_CD", "luo", "luo_KE", "luy", "luy_KE", "lv", "lv_LV",
        // M
        "mas", "mas_KE", "mas_TZ", "mer", "mer_KE", "mfe", "mfe_MU", "mg", "mg_MG", "mgh",
        "mgh_MZ", "mgo", "mgo_CM", "mi", "mi_NZ", "mk", "mk_MK", "ml", "ml_IN", "mn",
        "mn_MN", "mo", "mr", "mr_IN", "ms", "ms_BN", "ms_MY", "ms_SG", "mt", "mt_MT", "mua",
        "mua_CM", "my", "my_MM", "mzn", "mzn_IR",
        // N
        "naq", "naq_NA", "nb", "nb_NO", "nb_SJ", "nd", "nd_ZW", "nds", "nds_DE", "nds_NL",
        "ne", "ne_IN", "ne_NP", "nl", "nl_AW", "nl_BE", "nl_BQ", "nl_CW", "nl_NL", "nl_SR",
        "nl_SX", "nmg", "nmg_CM", "nn", "nn_NO", "nnh", "nnh_CM", "no", "no_NO", "no_NO_NY",
        "nus", "nus_SS", "nyn", "nyn_UG",
        // O
        "om", "om_ET", "om_KE", "or", "or_IN", "os", "os_GE", "os_RU",
        // P
        "pa", "pa_Arab", "pa_Arab_PK", "pa_Guru", "pa_Guru_IN", "pa_IN", "pa_PK", "pl",
        "pl_PL", "ps", "ps_AF", "ps_PK", "pt", "pt_AO", "pt_BR", "pt_CH", "pt_CV", "pt_GQ",
        "pt_GW", "pt_LU", "pt_MO", "pt_MZ", "pt_PT", "pt_ST", "pt_TL",
        // Q
        "qu", "qu_BO", "qu_EC", "qu_PE",
        // R
        "rm", "rm_CH", "rn", "rn_BI", "ro", "ro_MD", "ro_RO", "rof", "rof_TZ", "ru",
        "ru_BY", "ru_KG", "ru_KZ", "ru_MD", "ru_RU", "ru_UA", "rw", "rw_RW", "rwk", "rwk_TZ",
        // S
        "sah", "sah_RU", "saq", "saq_KE", "sbp", "sbp_TZ", "sd", "sd_PK", "se", "se_FI",
        "se_NO", "se_SE", "seh", "seh_MZ", "ses", "ses_ML", "sg", "sg_CF", "sh", "sh_BA",
        "sh_CS", "sh_YU", "shi", "shi_Latn", "shi_Latn_MA", "shi_Tfng", "shi_Tfng_MA",
        "shi_MA", "si", "si_LK", "sk", "sk_SK", "sl", "sl_SI", "smn", "smn_FI", "sn",
        "sn_ZW", "so", "so_DJ", "so_ET", "so_KE", "so_SO", "sq", "sq_AL", "sq_MK", "sq_XK",
        "sr", "sr_Cyrl", "sr_Cyrl_BA", "sr_Cyrl_ME", "sr_Cyrl_RS", "sr_Cyrl_CS", "sr_Cyrl_XK",
        "sr_Cyrl_YU", "sr_Latn", "sr_Latn_BA", "sr_Latn_ME", "sr_Latn_RS", "sr_Latn_CS",
        "sr_Latn_XK", "sr_Latn_YU", "sr_BA", "sr_ME", "sr_RS", "sr_CS", "sr_XK", "sr_YU",
        "sv", "sv_AX", "sv_FI", "sv_SE", "sw", "sw_CD", "sw_KE", "sw_TZ", "sw_UG",
        // T
        "ta", "ta_IN", "ta_LK", "ta_MY", "ta_SG", "te", "te_IN", "teo", "teo_KE", "teo_UG",
        "tg", "tg_TJ", "th", "th_TH", "th_TH_TRADITIONAL", "ti", "ti_ER", "ti_ET", "tk",
        "tk_TM", "tl", "tl_PH", "to", "to_TO", "tr", "tr_CY", "tr_TR", "tt", "tt_RU",
        "twq", "twq_NE", "tzm", "tzm_MA",
        // U
        "ug", "ug_CN", "uk", "uk_UA", "ur", "ur_IN", "ur_PK", "uz", "uz_AF", "uz_Arab",
        "uz_Arab_AF", "uz_Cyrl", "uz_Cyrl_UZ", "uz_Latn", "uz_Latn_UZ", "uz_UZ",
        // V
        "vai", "vai_Latn", "vai_Latn_LR", "vai_LR", "vai_Vaii", "vai_Vaii_LR", "vi",
        "vi_VN", "vun", "vun_TZ",
        // W
        "wae", "wae_CH", "wo", "wo_SN",
        // X
        "xh", "xh_ZA", "xog", "xog_UG",
        // Y
        "yav", "yav_CM", "yi", "yi_001", "yo", "yo_BJ", "yo_NG", "yue", "yue_CN", "yue_HK",
        "yue_Hans", "yue_Hans_CN", "yue_Hant", "yue_Hant_HK",
        // Z
        "zgh", "zgh_MA", "zh", "zh_Hans", "zh_Hans_CN", "zh_Hans_HK", "zh_Hans_MO",
        "zh_Hans_SG", "zh_Hant", "zh_Hant_HK", "zh_Hant_MO", "zh_Hant_TW", "zh_CN",
        "zh_HK", "zh_MO", "zh_SG", "zh_TW", "zu", "zu_ZA");

    private static final ImmutableSet<String> COLL_LOCALE_IDS = ImmutableSet.of(
        "root",
        // A-B
        "af", "am", "ars", "ar", "as", "az", "be", "bg", "bn", "bo", "bs_Cyrl", "bs",
        // C-F
        "ca", "ceb", "chr", "cs", "cy", "da", "de_AT", "de", "dsb", "dz", "ee", "el", "en",
        "en_US_POSIX", "en_US", "eo", "es", "et", "fa_AF", "fa", "fil", "fi", "fo", "fr_CA", "fr",
        // G-J
        "ga", "gl", "gu", "ha", "haw", "he", "hi", "hr", "hsb", "hu", "hy",
        "id_ID", "id", "ig", "in", "in_ID", "is", "it", "iw_IL", "iw", "ja",
        // K-P
        "ka", "kk", "kl", "km", "kn", "kok", "ko", "ku", "ky", "lb", "lkt", "ln", "lo", "lt", "lv",
        "mk", "ml", "mn", "mo", "mr", "ms", "mt", "my", "nb", "ne", "nl", "nn", "no_NO", "no",
        "om", "or", "pa_IN", "pa", "pa_Guru", "pl", "ps", "pt",
        // R-T
        "ro", "ru", "se", "sh_BA", "sh_CS", "sh", "sh_YU", "si", "sk", "sl", "smn", "sq",
        "sr_BA", "sr_Cyrl_ME", "sr_Latn", "sr_ME", "sr_RS", "sr", "sv", "sw",
        "ta", "te", "th", "tk", "to", "tr",
        // U-Z
        "ug", "uk", "ur", "uz", "vi", "wae", "wo", "xh", "yi", "yo", "yue_CN", "yue_Hans",
        "yue", "zh_CN", "zh_Hant", "zh_HK", "zh_MO", "zh_SG", "zh_TW", "zh", "zu");

    private static final ImmutableSet<String> BRKITR_LOCALE_IDS = ImmutableSet.of(
        "root", "de", "el", "en", "en_US_POSIX", "en_US", "es", "fr", "it", "ja", "pt", "ru",
        "zh_Hant", "zh");

    private static final ImmutableSet<String> RBNF_LOCALE_IDS = ImmutableSet.of(
        "root", "af", "ak", "am", "ars", "ar", "az", "be", "bg", "bs", "ca", "ccp", "chr", "cs",
        "cy", "da", "de_CH", "de", "ee", "el", "en_001", "en_IN", "en", "eo", "es_419", "es_DO",
        "es_GT", "es_HN", "es_MX", "es_NI", "es_PA", "es_PR", "es_SV", "es", "es_US", "et",
        "fa_AF", "fa", "ff", "fil", "fi", "fo", "fr_BE", "fr_CH", "fr", "ga", "he", "hi", "hr",
        "hu", "hy", "id", "in", "is", "it", "iw", "ja", "ka", "kl", "km", "ko", "ky", "lb",
        "lo", "lrc", "lt", "lv", "mk", "ms", "mt", "my", "nb", "nl", "nn", "no", "pl", "pt_PT",
        "pt", "qu", "ro", "ru", "se", "sh", "sk", "sl", "sq", "sr_Latn", "sr", "sv",
        "sw", "ta", "th", "tr", "uk", "vi", "yue_Hans", "yue", "zh_Hant_HK", "zh_Hant", "zh_HK",
        "zh_MO", "zh_TW", "zh");
}
