// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.util;


import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.Test;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.locale.XCldrStub.Joiner;
import com.ibm.icu.impl.locale.XCldrStub.Splitter;
import com.ibm.icu.impl.locale.XLocaleDistance;
import com.ibm.icu.impl.locale.XLocaleDistance.DistanceOption;
import com.ibm.icu.impl.locale.XLocaleMatcher;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

/**
 * Test the XLocaleMatcher.
 *
 * @author markdavis
 */
public class XLocaleMatcherTest extends TestFmwk {
    private static final boolean REFORMAT = false; // set to true to get a reformatted data file listed

    private static final int REGION_DISTANCE = 4;

    private static final XLocaleDistance LANGUAGE_MATCHER_DATA = XLocaleDistance.getDefault();

    private XLocaleMatcher newXLocaleMatcher() {
        return new XLocaleMatcher("");
    }

    private XLocaleMatcher newXLocaleMatcher(LocalePriorityList build) {
        return new XLocaleMatcher(build);
    }

    private XLocaleMatcher newXLocaleMatcher(String string) {
        return new XLocaleMatcher(LocalePriorityList.add(string).build());
    }

    private XLocaleMatcher newXLocaleMatcher(LocalePriorityList string, int d) {
        return XLocaleMatcher.builder().setSupportedLocales(string).setThresholdDistance(d).build();
    }

    private XLocaleMatcher newXLocaleMatcher(LocalePriorityList string, int d, DistanceOption distanceOption) {
        return XLocaleMatcher
            .builder()
            .setSupportedLocales(string)
            .setThresholdDistance(d)
            .setDistanceOption(distanceOption)
            .build();
    }

    //    public void testParentLocales() {
    //        // find all the regions that have a closer relation because of an explicit parent
    //        Set<String> explicitParents = new HashSet<>(INFO.getExplicitParents());
    //        explicitParents.remove("root");
    //        Set<String> otherParents = new HashSet<>(INFO.getExplicitParents());
    //        for (String locale : explicitParents) {
    //            while (true) {
    //                locale = LocaleIDParser.getParent(locale);
    //                if (locale == null || locale.equals("root")) {
    //                    break;
    //                }
    //                otherParents.add(locale);
    //            }
    //        }
    //        otherParents.remove("root");
    //
    //        for (String locale : CONFIG.getCldrFactory().getAvailable()) {
    //            String parentId = LocaleIDParser.getParent(locale);
    //            String parentIdSimple = LocaleIDParser.getSimpleParent(locale);
    //            if (!explicitParents.contains(parentId) && !otherParents.contains(parentIdSimple)) {
    //                continue;
    //            }
    //            System.out.println(locale + "\t" + CONFIG.getEnglish().getName(locale) + "\t" + parentId + "\t" + parentIdSimple);
    //        }
    //    }


// TBD reenable with override data
//    public void testOverrideData() {
//        double threshold = 0.05;
//        XLocaleDistance XLocaleMatcherData = new XLocaleDistance()
//        .addDistance("br", "fr", 10, true)
//        .addDistance("es", "cy", 10, true);
//        logln(XLocaleMatcherData.toString());
//
//        final XLocaleMatcher matcher = newXLocaleMatcher(
//            LocalePriorityList
//            .add(ULocale.ENGLISH)
//            .add(ULocale.FRENCH)
//            .add(ULocale.UK)
//            .build(), XLocaleMatcherData, threshold);
//        logln(matcher.toString());
//
//        assertEquals(ULocale.FRENCH, matcher.getBestMatch(new ULocale("br")));
//        assertEquals(ULocale.ENGLISH, matcher.getBestMatch(new ULocale("es"))); // one
//        // way
//    }


    private void assertEquals(Object expected, Object string) {
        assertEquals("", expected, string);
    }

    /**
     * If all the base languages are the same, then each sublocale matches
     * itself most closely
     */
    @Test
    public void testExactMatches() {
        String lastBase = "";
        TreeSet<ULocale> sorted = new TreeSet<ULocale>();
        for (ULocale loc : ULocale.getAvailableLocales()) {
            String language = loc.getLanguage();
            if (!lastBase.equals(language)) {
                check(sorted);
                sorted.clear();
                lastBase = language;
            }
            sorted.add(loc);
        }
        check(sorted);
    }

    private void check(Set<ULocale> sorted) {
        if (sorted.isEmpty()) {
            return;
        }
        check2(sorted);
        ULocale first = sorted.iterator().next();
        ULocale max = ULocale.addLikelySubtags(first);
        sorted.add(max);
        check2(sorted);
    }

    /**
     * @param sorted
     */
    private void check2(Set<ULocale> sorted) {
        // TODO Auto-generated method stub
        logln("Checking: " + sorted);
        XLocaleMatcher matcher = newXLocaleMatcher(
            LocalePriorityList.add(
                sorted.toArray(new ULocale[sorted.size()]))
            .build());
        for (ULocale loc : sorted) {
            String stringLoc = loc.toString();
            assertEquals(stringLoc, matcher.getBestMatch(stringLoc).toString());
        }
    }

    @Test
    public void testComputeDistance_monkeyTest() {
        String[] codes = ULocale.getISOCountries();
        Random random = new Random();
        XLocaleMatcher lm = newXLocaleMatcher();
        for (int i = 0; i < 1000; ++i) {
            String x = codes[random.nextInt(codes.length)];
            String y = codes[random.nextInt(codes.length)];
            double d = lm.distance(ULocale.forLanguageTag("xx-Xxxx-"+x), ULocale.forLanguageTag("xx-Xxxx-"+y));
            if (x.equals("ZZ") || y.equals("ZZ")) {
                assertEquals("dist(regionDistance," + x + ") = 0", REGION_DISTANCE, d);
            } else if (x.equals(y)) {
                assertEquals("dist(x,x) = 0", 0.0, d);
            } else {
                assertTrue("dist(" + x + "," + y + ") > 0", d > 0);
                assertTrue("dist(" + x + "," + y + ") ≤ " + REGION_DISTANCE, d <= REGION_DISTANCE);
            }
        }
    }


    @Test
    public void testPerf() {
        if (LANGUAGE_MATCHER_DATA == null) {
            return; // skip except when testing data
        }
        final ULocale desired = new ULocale("sv");

        final String shortList = "en, sv";
        final String longList = "af, am, ar, az, be, bg, bn, bs, ca, cs, cy, cy, da, de, el, en, en-GB, es, es-419, et, eu, fa, fi, fil, fr, ga, gl, gu, hi, hr, hu, hy, id, is, it, iw, ja, ka, kk, km, kn, ko, ky, lo, lt, lv, mk, ml, mn, mr, ms, my, ne, nl, no, pa, pl, pt, pt-PT, ro, ru, si, sk, sl, sq, sr, sr-Latn, sv, sw, ta, te, th, tr, uk, ur, uz, vi, zh-CN, zh-TW, zu";
        final String veryLongList = "af, af_NA, af_ZA, agq, agq_CM, ak, ak_GH, am, am_ET, ar, ar_001, ar_AE, ar_BH, ar_DJ, ar_DZ, ar_EG, ar_EH, ar_ER, ar_IL, ar_IQ, ar_JO, ar_KM, ar_KW, ar_LB, ar_LY, ar_MA, ar_MR, ar_OM, ar_PS, ar_QA, ar_SA, ar_SD, ar_SO, ar_SS, ar_SY, ar_TD, ar_TN, ar_YE, as, as_IN, asa, asa_TZ, ast, ast_ES, az, az_Cyrl, az_Cyrl_AZ, az_Latn, az_Latn_AZ, bas, bas_CM, be, be_BY, bem, bem_ZM, bez, bez_TZ, bg, bg_BG, bm, bm_ML, bn, bn_BD, bn_IN, bo, bo_CN, bo_IN, br, br_FR, brx, brx_IN, bs, bs_Cyrl, bs_Cyrl_BA, bs_Latn, bs_Latn_BA, ca, ca_AD, ca_ES, ca_ES_VALENCIA, ca_FR, ca_IT, ce, ce_RU, cgg, cgg_UG, chr, chr_US, ckb, ckb_IQ, ckb_IR, cs, cs_CZ, cu, cu_RU, cy, cy_GB, da, da_DK, da_GL, dav, dav_KE, de, de_AT, de_BE, de_CH, de_DE, de_LI, de_LU, dje, dje_NE, dsb, dsb_DE, dua, dua_CM, dyo, dyo_SN, dz, dz_BT, ebu, ebu_KE, ee, ee_GH, ee_TG, el, el_CY, el_GR, en, en_001, en_150, en_AG, en_AI, en_AS, en_AT, en_AU, en_BB, en_BE, en_BI, en_BM, en_BS, en_BW, en_BZ, en_CA, en_CC, en_CH, en_CK, en_CM, en_CX, en_CY, en_DE, en_DG, en_DK, en_DM, en_ER, en_FI, en_FJ, en_FK, en_FM, en_GB, en_GD, en_GG, en_GH, en_GI, en_GM, en_GU, en_GY, en_HK, en_IE, en_IL, en_IM, en_IN, en_IO, en_JE, en_JM, en_KE, en_KI, en_KN, en_KY, en_LC, en_LR, en_LS, en_MG, en_MH, en_MO, en_MP, en_MS, en_MT, en_MU, en_MW, en_MY, en_NA, en_NF, en_NG, en_NL, en_NR, en_NU, en_NZ, en_PG, en_PH, en_PK, en_PN, en_PR, en_PW, en_RW, en_SB, en_SC, en_SD, en_SE, en_SG, en_SH, en_SI, en_SL, en_SS, en_SX, en_SZ, en_TC, en_TK, en_TO, en_TT, en_TV, en_TZ, en_UG, en_UM, en_US, en_US_POSIX, en_VC, en_VG, en_VI, en_VU, en_WS, en_ZA, en_ZM, en_ZW, eo, eo_001, es, es_419, es_AR, es_BO, es_CL, es_CO, es_CR, es_CU, es_DO, es_EA, es_EC, es_ES, es_GQ, es_GT, es_HN, es_IC, es_MX, es_NI, es_PA, es_PE, es_PH, es_PR, es_PY, es_SV, es_US, es_UY, es_VE, et, et_EE, eu, eu_ES, ewo, ewo_CM, fa, fa_AF, fa_IR, ff, ff_CM, ff_GN, ff_MR, ff_SN, fi, fi_FI, fil, fil_PH, fo, fo_DK, fo_FO, fr, fr_BE, fr_BF, fr_BI, fr_BJ, fr_BL, fr_CA, fr_CD, fr_CF, fr_CG, fr_CH, fr_CI, fr_CM, fr_DJ, fr_DZ, fr_FR, fr_GA, fr_GF, fr_GN, fr_GP, fr_GQ, fr_HT, fr_KM, fr_LU, fr_MA, fr_MC, fr_MF, fr_MG, fr_ML, fr_MQ, fr_MR, fr_MU, fr_NC, fr_NE, fr_PF, fr_PM, fr_RE, fr_RW, fr_SC, fr_SN, fr_SY, fr_TD, fr_TG, fr_TN, fr_VU, fr_WF, fr_YT, fur, fur_IT, fy, fy_NL, ga, ga_IE, gd, gd_GB, gl, gl_ES, gsw, gsw_CH, gsw_FR, gsw_LI, gu, gu_IN, guz, guz_KE, gv, gv_IM, ha, ha_GH, ha_NE, ha_NG, haw, haw_US, he, he_IL, hi, hi_IN, hr, hr_BA, hr_HR, hsb, hsb_DE, hu, hu_HU, hy, hy_AM, id, id_ID, ig, ig_NG, ii, ii_CN, is, is_IS, it, it_CH, it_IT, it_SM, ja, ja_JP, jgo, jgo_CM, jmc, jmc_TZ, ka, ka_GE, kab, kab_DZ, kam, kam_KE, kde, kde_TZ, kea, kea_CV, khq, khq_ML, ki, ki_KE, kk, kk_KZ, kkj, kkj_CM, kl, kl_GL, kln, kln_KE, km, km_KH, kn, kn_IN, ko, ko_KP, ko_KR, kok, kok_IN, ks, ks_IN, ksb, ksb_TZ, ksf, ksf_CM, ksh, ksh_DE, kw, kw_GB, ky, ky_KG, lag, lag_TZ, lb, lb_LU, lg, lg_UG, lkt, lkt_US, ln, ln_AO, ln_CD, ln_CF, ln_CG, lo, lo_LA, lrc, lrc_IQ, lrc_IR, lt, lt_LT, lu, lu_CD, luo, luo_KE, luy, luy_KE, lv, lv_LV, mas, mas_KE, mas_TZ, mer, mer_KE, mfe, mfe_MU, mg, mg_MG, mgh, mgh_MZ, mgo, mgo_CM, mk, mk_MK, ml, ml_IN, mn, mn_MN, mr, mr_IN, ms, ms_BN, ms_MY, ms_SG, mt, mt_MT, mua, mua_CM, my, my_MM, mzn, mzn_IR, naq, naq_NA, nb, nb_NO, nb_SJ, nd, nd_ZW, ne, ne_IN, ne_NP, nl, nl_AW, nl_BE, nl_BQ, nl_CW, nl_NL, nl_SR, nl_SX, nmg, nmg_CM, nn, nn_NO, nnh, nnh_CM, nus, nus_SS, nyn, nyn_UG, om, om_ET, om_KE, or, or_IN, os, os_GE, os_RU, pa, pa_Arab, pa_Arab_PK, pa_Guru, pa_Guru_IN, pl, pl_PL, prg, prg_001, ps, ps_AF, pt, pt_AO, pt_BR, pt_CV, pt_GW, pt_MO, pt_MZ, pt_PT, pt_ST, pt_TL, qu, qu_BO, qu_EC, qu_PE, rm, rm_CH, rn, rn_BI, ro, ro_MD, ro_RO, rof, rof_TZ, root, ru, ru_BY, ru_KG, ru_KZ, ru_MD, ru_RU, ru_UA, rw, rw_RW, rwk, rwk_TZ, sah, sah_RU, saq, saq_KE, sbp, sbp_TZ, se, se_FI, se_NO, se_SE, seh, seh_MZ, ses, ses_ML, sg, sg_CF, shi, shi_Latn, shi_Latn_MA, shi_Tfng, shi_Tfng_MA, si, si_LK, sk, sk_SK, sl, sl_SI, smn, smn_FI, sn, sn_ZW, so, so_DJ, so_ET, so_KE, so_SO, sq, sq_AL, sq_MK, sq_XK, sr, sr_Cyrl, sr_Cyrl_BA, sr_Cyrl_ME, sr_Cyrl_RS, sr_Cyrl_XK, sr_Latn, sr_Latn_BA, sr_Latn_ME, sr_Latn_RS, sr_Latn_XK, sv, sv_AX, sv_FI, sv_SE, sw, sw_CD, sw_KE, sw_TZ, sw_UG, ta, ta_IN, ta_LK, ta_MY, ta_SG, te, te_IN, teo, teo_KE, teo_UG, th, th_TH, ti, ti_ER, ti_ET, tk, tk_TM, to, to_TO, tr, tr_CY, tr_TR, twq, twq_NE, tzm, tzm_MA, ug, ug_CN, uk, uk_UA, ur, ur_IN, ur_PK, uz, uz_Arab, uz_Arab_AF, uz_Cyrl, uz_Cyrl_UZ, uz_Latn, uz_Latn_UZ, vai, vai_Latn, vai_Latn_LR, vai_Vaii, vai_Vaii_LR, vi, vi_VN, vo, vo_001, vun, vun_TZ, wae, wae_CH, xog, xog_UG, yav, yav_CM, yi, yi_001, yo, yo_BJ, yo_NG, zgh, zgh_MA, zh, zh_Hans, zh_Hans_CN, zh_Hans_HK, zh_Hans_MO, zh_Hans_SG, zh_Hant, zh_Hant_HK, zh_Hant_MO, zh_Hant_TW, zu, zu_ZA";

        final XLocaleMatcher matcherShort = newXLocaleMatcher(shortList);
        final XLocaleMatcher matcherLong = newXLocaleMatcher(longList);
        final XLocaleMatcher matcherVeryLong = newXLocaleMatcher(veryLongList);

        final LocaleMatcher matcherShortOld = new LocaleMatcher(shortList);
        final LocaleMatcher matcherLongOld = new LocaleMatcher(longList);
        final LocaleMatcher matcherVeryLongOld = new LocaleMatcher(veryLongList);

        //XLocaleMatcher.DEBUG = true;
        ULocale expected = new ULocale("sv");
        assertEquals(expected, matcherShort.getBestMatch(desired));
        assertEquals(expected, matcherLong.getBestMatch(desired));
        assertEquals(expected, matcherVeryLong.getBestMatch(desired));
        //XLocaleMatcher.DEBUG = false;

        long timeShortNew=0;
        long timeMediumNew=0;
        long timeLongNew=0;

        for (int i = 0; i < 2; ++i) {
            int iterations = i == 0 ? 1000 : 1000000;
            boolean showMessage = i != 0;
            timeShortNew = timeXLocaleMatcher("Duration (few  supported):\t", desired, matcherShort, showMessage, iterations);
            timeMediumNew = timeXLocaleMatcher("Duration (med. supported):\t", desired, matcherLong, showMessage, iterations);
            timeLongNew = timeXLocaleMatcher("Duration (many supported):\t", desired, matcherVeryLong, showMessage, iterations);
        }

        long timeShortOld=0;
        long timeMediumOld=0;
        long timeLongOld=0;

        for (int i = 0; i < 2; ++i) {
            int iterations = i == 0 ? 1000 : 100000;
            boolean showMessage = i != 0;
            timeShortOld = timeLocaleMatcher("Old Duration (few  supported):\t", desired, matcherShortOld, showMessage, iterations);
            timeMediumOld = timeLocaleMatcher("Old Duration (med. supported):\t", desired, matcherLongOld, showMessage, iterations);
            timeLongOld = timeLocaleMatcher("Old Duration (many supported):\t", desired, matcherVeryLongOld, showMessage, iterations);
        }

        assertTrue("timeShortNew (=" + timeShortNew + ") < 25% of timeShortOld (=" + timeShortOld + ")", timeShortNew * 4 < timeShortOld);
        assertTrue("timeMediumNew (=" + timeMediumNew + ") < 25% of timeMediumOld (=" + timeMediumOld + ")", timeMediumNew * 4 < timeMediumOld);
        assertTrue("timeLongNew (=" + timeLongNew + ") < 25% of timeLongOld (=" + timeLongOld + ")", timeLongNew * 4 < timeLongOld);

    }

    private long timeXLocaleMatcher(String title, ULocale desired, XLocaleMatcher matcher,
        boolean showmessage, int iterations) {
        long start = System.nanoTime();
        for (int i = iterations; i > 0; --i) {
            matcher.getBestMatch(desired);
        }
        long delta = System.nanoTime() - start;
        if (showmessage) logln(title + (delta / iterations) + " nanos");
        return (delta / iterations);
    }

    private long timeLocaleMatcher(String title, ULocale desired, LocaleMatcher matcher,
        boolean showmessage, int iterations) {
        long start = System.nanoTime();
        for (int i = iterations; i > 0; --i) {
            matcher.getBestMatch(desired);
        }
        long delta = System.nanoTime() - start;
        if (showmessage) logln(title + (delta / iterations) + " nanos");
        return (delta / iterations);
    }

    @Test
    public void testDataDriven() throws IOException {
        DataDrivenTestHelper tfh = new MyTestFileHandler()
            .setFramework(this)
            .run(XLocaleMatcherTest.class, "data/localeMatcherTest.txt");
        if (REFORMAT) {
            System.out.println(tfh.appendLines(new StringBuilder()));
        }
    }

    private static final Splitter COMMA_SPACE = Splitter.on(Pattern.compile(",\\s*|\\s+")).trimResults();
    private static final Joiner JOIN_COMMA_SPACE = Joiner.on(", ");
    private static final UnicodeSet DIGITS = new UnicodeSet("[0-9]").freeze();

    class MyTestFileHandler extends DataDrivenTestHelper {

        Output<ULocale> bestDesired = new Output<ULocale>();
        DistanceOption distanceOption = DistanceOption.NORMAL;
        int threshold = -1;

        @Override
        public void handle(int lineNumber, boolean breakpoint, String commentBase, List<String> arguments) {
            List<String> supported = COMMA_SPACE.splitToList(arguments.get(0));
            final String supportedReformatted = JOIN_COMMA_SPACE.join(supported);
            LocalePriorityList supportedList = LocalePriorityList.add(supportedReformatted).build();

            Iterable<String> desired = COMMA_SPACE.split(arguments.get(1));
            final String desiredReformatted = JOIN_COMMA_SPACE.join(desired);
            LocalePriorityList desiredList = LocalePriorityList.add(desiredReformatted).build();

            String expected = arguments.get(2);
            String expectedLanguageTag = expected.equals("null") ? null : new ULocale(expected).toLanguageTag();

            String expectedUi = arguments.size() < 4 ? null : arguments.get(3);
            String expectedUiLanguageTag = expectedUi == null || expectedUi.equals("null") ? null
                : new ULocale(expectedUi).toLanguageTag();

            if (breakpoint) {
                breakpoint = false; // put debugger breakpoint here to break at @debug in test file
            }

            XLocaleMatcher matcher = threshold < 0 && distanceOption == DistanceOption.NORMAL
                ? newXLocaleMatcher(supportedList)
                : newXLocaleMatcher(supportedList, threshold, distanceOption);
            commentBase = "(" + lineNumber + ") " + commentBase;

            ULocale bestSupported;
            if (expectedUi != null) {
                bestSupported = matcher.getBestMatch(desiredList, bestDesired);
                ULocale bestUI = XLocaleMatcher.combine(bestSupported, bestDesired.value);
                assertEquals(commentBase + " (UI)", expectedUiLanguageTag, bestUI == null ? null : bestUI.toLanguageTag());
            } else {
                bestSupported = matcher.getBestMatch(desiredList);
            }
            String bestMatchLanguageTag = bestSupported == null ? null : bestSupported.toLanguageTag();
            assertEquals(commentBase, expectedLanguageTag, bestMatchLanguageTag);
        }

        @Override
        public void handleParams(String comment, List<String> arguments) {
            String switchItem = arguments.get(0);
            if (switchItem.equals("@DistanceOption")) {
                distanceOption = DistanceOption.valueOf(arguments.get(1));
            } else if (switchItem.equals("@Threshold")) {
                threshold = Integer.valueOf(arguments.get(1));
            } else {
                super.handleParams(comment, arguments);
            }
            return;
        }
    }
}
