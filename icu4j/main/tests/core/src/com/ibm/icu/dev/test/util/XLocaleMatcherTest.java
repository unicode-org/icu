// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.locale.LocaleDistance;
import com.ibm.icu.impl.locale.LocaleDistance.DistanceOption;
import com.ibm.icu.impl.locale.XCldrStub.FileUtilities;
import com.ibm.icu.impl.locale.XLocaleMatcher;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.ULocale;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * Test the XLocaleMatcher.
 *
 * @author markdavis
 */
@RunWith(JUnitParamsRunner.class)
public class XLocaleMatcherTest extends TestFmwk {
    private static final int REGION_DISTANCE = 4;

    private static final LocaleDistance LANGUAGE_MATCHER_DATA = LocaleDistance.INSTANCE;

    private XLocaleMatcher newXLocaleMatcher() {
        return new XLocaleMatcher("");
    }

    private XLocaleMatcher newXLocaleMatcher(LocalePriorityList build) {
        return new XLocaleMatcher(build);
    }

    private XLocaleMatcher newXLocaleMatcher(String string) {
        return new XLocaleMatcher(LocalePriorityList.add(string).build());
    }

    @SuppressWarnings("unused")
    private XLocaleMatcher newXLocaleMatcher(LocalePriorityList string, int d) {
        return XLocaleMatcher.builder().setSupportedLocales(string).setThresholdDistance(d).build();
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
        TreeSet<ULocale> sorted = new TreeSet<>();
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

    private static final class PerfCase {
        ULocale desired;
        ULocale expectedShort;
        ULocale expectedLong;
        ULocale expectedVeryLong;

        PerfCase(String des, String expShort, String expLong, String expVeryLong) {
            desired = new ULocale(des);
            expectedShort = new ULocale(expShort);
            expectedLong = new ULocale(expLong);
            expectedVeryLong = new ULocale(expVeryLong);
        }
    }

    private static final int WARM_UP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 20000;
    private static final int AVG_PCT_MEDIUM_NEW_OLD = 33;
    private static final int AVG_PCT_LONG_NEW_OLD = 80;

    @Test
    public void testPerf() {
        if (LANGUAGE_MATCHER_DATA == null) {
            return; // skip except when testing data
        }

        final String shortList = "en, sv";
        final String longList = "af, am, ar, az, be, bg, bn, bs, ca, cs, cy, cy, da, de, " +
                "el, en, en-GB, es, es-419, et, eu, fa, fi, fil, fr, ga, gl, gu, " +
                "hi, hr, hu, hy, id, is, it, iw, ja, ka, kk, km, kn, ko, ky, lo, lt, lv, " +
                "mk, ml, mn, mr, ms, my, ne, nl, no, pa, pl, pt, pt-PT, ro, ru, " +
                "si, sk, sl, sq, sr, sr-Latn, sv, sw, ta, te, th, tr, uk, ur, uz, vi, " +
                "zh-CN, zh-TW, zu";
        final String veryLongList = "af, af_NA, af_ZA, agq, agq_CM, ak, ak_GH, am, am_ET, " +
                "ar, ar_001, ar_AE, ar_BH, ar_DJ, ar_DZ, ar_EG, ar_EH, ar_ER, ar_IL, ar_IQ, " +
                "ar_JO, ar_KM, ar_KW, ar_LB, ar_LY, ar_MA, ar_MR, ar_OM, ar_PS, ar_QA, " +
                "ar_SA, ar_SD, ar_SO, ar_SS, ar_SY, ar_TD, ar_TN, ar_YE, as, as_IN, asa, asa_TZ, " +
                "ast, ast_ES, az, az_Cyrl, az_Cyrl_AZ, az_Latn, az_Latn_AZ, " +
                "bas, bas_CM, be, be_BY, bem, bem_ZM, bez, bez_TZ, bg, bg_BG, bm, bm_ML, " +
                "bn, bn_BD, bn_IN, bo, bo_CN, bo_IN, br, br_FR, brx, brx_IN, " +
                "bs, bs_Cyrl, bs_Cyrl_BA, bs_Latn, bs_Latn_BA, ca, ca_AD, ca_ES, ca_ES_VALENCIA, " +
                "ca_FR, ca_IT, ce, ce_RU, cgg, cgg_UG, chr, chr_US, ckb, ckb_IQ, ckb_IR, cs, cs_CZ, " +
                "cu, cu_RU, cy, cy_GB, da, da_DK, da_GL, dav, dav_KE, de, de_AT, de_BE, de_CH, " +
                "de_DE, de_LI, de_LU, dje, dje_NE, dsb, dsb_DE, dua, dua_CM, dyo, dyo_SN, dz, dz_BT, " +
                // removed en_001 to avoid exact match
                "ebu, ebu_KE, ee, ee_GH, ee_TG, el, el_CY, el_GR, en, en_150, " +
                "en_AG, en_AI, en_AS, en_AT, en_AU, en_BB, en_BE, en_BI, en_BM, en_BS, en_BW, " +
                "en_BZ, en_CA, en_CC, en_CH, en_CK, en_CM, en_CX, en_CY, en_DE, en_DG, en_DK, " +
                "en_DM, en_ER, en_FI, en_FJ, en_FK, en_FM, en_GB, en_GD, en_GG, en_GH, en_GI, " +
                "en_GM, en_GU, en_GY, en_HK, en_IE, en_IL, en_IM, en_IN, en_IO, en_JE, en_JM, " +
                "en_KE, en_KI, en_KN, en_KY, en_LC, en_LR, en_LS, en_MG, en_MH, en_MO, en_MP, " +
                "en_MS, en_MT, en_MU, en_MW, en_MY, en_NA, en_NF, en_NG, en_NL, en_NR, en_NU, " +
                "en_NZ, en_PG, en_PH, en_PK, en_PN, en_PR, en_PW, en_RW, en_SB, en_SC, en_SD, " +
                "en_SE, en_SG, en_SH, en_SI, en_SL, en_SS, en_SX, en_SZ, en_TC, en_TK, en_TO, " +
                "en_TT, en_TV, en_TZ, en_UG, en_UM, en_US, en_US_POSIX, en_VC, en_VG, en_VI, " +
                "en_VU, en_WS, en_ZA, en_ZM, en_ZW, eo, eo_001, es, es_419, es_AR, es_BO, es_CL, " +
                "es_CO, es_CR, es_CU, es_DO, es_EA, es_EC, es_ES, es_GQ, es_GT, es_HN, es_IC, " +
                "es_MX, es_NI, es_PA, es_PE, es_PH, es_PR, es_PY, es_SV, es_US, es_UY, es_VE, " +
                "et, et_EE, eu, eu_ES, ewo, ewo_CM, fa, fa_AF, fa_IR, ff, ff_CM, ff_GN, ff_MR, " +
                "ff_SN, fi, fi_FI, fil, fil_PH, fo, fo_DK, fo_FO, fr, fr_BE, fr_BF, fr_BI, fr_BJ, " +
                "fr_BL, fr_CA, fr_CD, fr_CF, fr_CG, fr_CH, fr_CI, fr_CM, fr_DJ, fr_DZ, " +
                "fr_FR, fr_GA, fr_GF, fr_GN, fr_GP, fr_GQ, fr_HT, fr_KM, fr_LU, fr_MA, fr_MC, " +
                "fr_MF, fr_MG, fr_ML, fr_MQ, fr_MR, fr_MU, fr_NC, fr_NE, fr_PF, fr_PM, fr_RE, " +
                "fr_RW, fr_SC, fr_SN, fr_SY, fr_TD, fr_TG, fr_TN, fr_VU, fr_WF, fr_YT, " +
                "fur, fur_IT, fy, fy_NL, ga, ga_IE, gd, gd_GB, gl, gl_ES, gsw, gsw_CH, gsw_FR, " +
                "gsw_LI, gu, gu_IN, guz, guz_KE, gv, gv_IM, ha, ha_GH, ha_NE, ha_NG, haw, haw_US, " +
                "he, he_IL, hi, hi_IN, hr, hr_BA, hr_HR, hsb, hsb_DE, hu, hu_HU, hy, hy_AM, " +
                "id, id_ID, ig, ig_NG, ii, ii_CN, is, is_IS, it, it_CH, it_IT, it_SM, ja, ja_JP, " +
                "jgo, jgo_CM, jmc, jmc_TZ, ka, ka_GE, kab, kab_DZ, kam, kam_KE, kde, kde_TZ, " +
                "kea, kea_CV, khq, khq_ML, ki, ki_KE, kk, kk_KZ, kkj, kkj_CM, kl, kl_GL, " +
                "kln, kln_KE, km, km_KH, kn, kn_IN, ko, ko_KP, ko_KR, kok, kok_IN, " +
                "ks, ks_IN, ksb, ksb_TZ, ksf, ksf_CM, ksh, ksh_DE, kw, kw_GB, ky, ky_KG, " +
                "lag, lag_TZ, lb, lb_LU, lg, lg_UG, lkt, lkt_US, ln, ln_AO, ln_CD, ln_CF, ln_CG, " +
                "lo, lo_LA, lrc, lrc_IQ, lrc_IR, lt, lt_LT, lu, lu_CD, luo, luo_KE, luy, luy_KE, " +
                "lv, lv_LV, mas, mas_KE, mas_TZ, mer, mer_KE, mfe, mfe_MU, mg, mg_MG, " +
                "mgh, mgh_MZ, mgo, mgo_CM, mk, mk_MK, ml, ml_IN, mn, mn_MN, mr, mr_IN, ms, ms_BN, " +
                "ms_MY, ms_SG, mt, mt_MT, mua, mua_CM, my, my_MM, mzn, mzn_IR, naq, naq_NA, " +
                "nb, nb_NO, nb_SJ, nd, nd_ZW, ne, ne_IN, ne_NP, nl, nl_AW, nl_BE, nl_BQ, nl_CW, " +
                "nl_NL, nl_SR, nl_SX, nmg, nmg_CM, nn, nn_NO, nnh, nnh_CM, nus, nus_SS, nyn, " +
                "nyn_UG, om, om_ET, om_KE, or, or_IN, os, os_GE, os_RU, pa, pa_Arab, pa_Arab_PK, " +
                "pa_Guru, pa_Guru_IN, pl, pl_PL, prg, prg_001, ps, ps_AF, pt, pt_AO, pt_BR, " +
                "pt_CV, pt_GW, pt_MO, pt_MZ, pt_PT, pt_ST, pt_TL, qu, qu_BO, qu_EC, qu_PE, rm, " +
                "rm_CH, rn, rn_BI, ro, ro_MD, ro_RO, rof, rof_TZ, root, ru, ru_BY, ru_KG, ru_KZ, " +
                "ru_MD, ru_RU, ru_UA, rw, rw_RW, rwk, rwk_TZ, sah, sah_RU, saq, saq_KE, sbp, " +
                "sbp_TZ, se, se_FI, se_NO, se_SE, seh, seh_MZ, ses, ses_ML, sg, sg_CF, shi, " +
                "shi_Latn, shi_Latn_MA, shi_Tfng, shi_Tfng_MA, si, si_LK, sk, sk_SK, sl, sl_SI, " +
                "smn, smn_FI, sn, sn_ZW, so, so_DJ, so_ET, so_KE, so_SO, sq, sq_AL, sq_MK, sq_XK, " +
                "sr, sr_Cyrl, sr_Cyrl_BA, sr_Cyrl_ME, sr_Cyrl_RS, sr_Cyrl_XK, sr_Latn, " +
                "sr_Latn_BA, sr_Latn_ME, sr_Latn_RS, sr_Latn_XK, sv, sv_AX, sv_FI, sv_SE, sw, " +
                "sw_CD, sw_KE, sw_TZ, sw_UG, ta, ta_IN, ta_LK, ta_MY, ta_SG, te, te_IN, teo, " +
                "teo_KE, teo_UG, th, th_TH, ti, ti_ER, ti_ET, tk, tk_TM, to, to_TO, tr, tr_CY, " +
                "tr_TR, twq, twq_NE, tzm, tzm_MA, ug, ug_CN, uk, uk_UA, ur, ur_IN, ur_PK, uz, " +
                "uz_Arab, uz_Arab_AF, uz_Cyrl, uz_Cyrl_UZ, uz_Latn, uz_Latn_UZ, vai, vai_Latn, " +
                "vai_Latn_LR, vai_Vaii, vai_Vaii_LR, vi, vi_VN, vo, vo_001, vun, vun_TZ, wae, " +
                "wae_CH, xog, xog_UG, yav, yav_CM, yi, yi_001, yo, yo_BJ, yo_NG, zgh, zgh_MA, " +
                "zh, zh_Hans, zh_Hans_CN, zh_Hans_HK, zh_Hans_MO, zh_Hans_SG, zh_Hant, " +
                "zh_Hant_HK, zh_Hant_MO, zh_Hant_TW, zu, zu_ZA";

        final XLocaleMatcher matcherShort = newXLocaleMatcher(shortList);
        final XLocaleMatcher matcherLong = newXLocaleMatcher(longList);
        final XLocaleMatcher matcherVeryLong = newXLocaleMatcher(veryLongList);

        final LocaleMatcher matcherShortOld = new LocaleMatcher(shortList);
        final LocaleMatcher matcherLongOld = new LocaleMatcher(longList);
        final LocaleMatcher matcherVeryLongOld = new LocaleMatcher(veryLongList);

        long timeShortNew=0;
        long timeMediumNew=0;
        long timeLongNew=0;

        long timeShortOld=0;
        long timeMediumOld=0;
        long timeLongOld=0;

        PerfCase[] pcs = new PerfCase[] {
                // Exact match in all matchers.
                new PerfCase("sv", "sv", "sv", "sv"),
                // Common locale, exact match only in very long list.
                new PerfCase("fr_CA", "en", "fr", "fr_CA"),
                // Unusual locale, no exact match.
                new PerfCase("de_CA", "en", "de", "de"),
                // World English maps to several region partitions.
                new PerfCase("en_001", "en", "en", "en"),
                // Ancient language with interesting subtags.
                new PerfCase("egy_Copt_CY", "en", "af", "af")
        };

        for (PerfCase pc : pcs) {
            final ULocale desired = pc.desired;

            assertEquals(pc.expectedShort, matcherShort.getBestMatch(desired));
            assertEquals(pc.expectedLong, matcherLong.getBestMatch(desired));
            assertEquals(pc.expectedVeryLong, matcherVeryLong.getBestMatch(desired));

            timeXLocaleMatcher(desired, matcherShort, WARM_UP_ITERATIONS);
            timeXLocaleMatcher(desired, matcherLong, WARM_UP_ITERATIONS);
            timeXLocaleMatcher(desired, matcherVeryLong, WARM_UP_ITERATIONS);
            long tns = timeXLocaleMatcher(desired, matcherShort, BENCHMARK_ITERATIONS);
            System.out.format("New Duration (few  supported):\t%s\t%d\tnanos\n", desired, tns);
            timeShortNew += tns;
            long tnl = timeXLocaleMatcher(desired, matcherLong, BENCHMARK_ITERATIONS);
            System.out.format("New Duration (med. supported):\t%s\t%d\tnanos\n", desired, tnl);
            timeMediumNew += tnl;
            long tnv = timeXLocaleMatcher(desired, matcherVeryLong, BENCHMARK_ITERATIONS);
            System.out.format("New Duration (many supported):\t%s\t%d\tnanos\n", desired, tnv);
            timeLongNew += tnv;

            timeLocaleMatcher(desired, matcherShortOld, WARM_UP_ITERATIONS);
            timeLocaleMatcher(desired, matcherLongOld, WARM_UP_ITERATIONS);
            timeLocaleMatcher(desired, matcherVeryLongOld, WARM_UP_ITERATIONS);
            long tos = timeLocaleMatcher(desired, matcherShortOld, BENCHMARK_ITERATIONS);
            System.out.format("Old Duration (few  supported):\t%s\t%d\tnanos  new/old=%d%%\n",
                    desired, tos, (100 * tns) / tos);
            timeShortOld += tos;
            long tol = timeLocaleMatcher(desired, matcherLongOld, BENCHMARK_ITERATIONS);
            System.out.format("Old Duration (med. supported):\t%s\t%d\tnanos  new/old=%d%%\n",
                    desired, tol, (100 * tnl) / tol);
            timeMediumOld += tol;
            long tov = timeLocaleMatcher(desired, matcherVeryLongOld, BENCHMARK_ITERATIONS);
            System.out.format("Old Duration (many supported):\t%s\t%d\tnanos  new/old=%d%%\n",
                    desired, tov, (100 * tnv) / tov);
            timeLongOld += tov;
        }

        assertTrue(
                String.format("timeShortNew=%d < %d%% of timeShortOld=%d",
                        timeShortNew, AVG_PCT_MEDIUM_NEW_OLD, timeShortOld),
                timeShortNew * 100 < timeShortOld * AVG_PCT_MEDIUM_NEW_OLD);
        assertTrue(
                String.format("timeMediumNew=%d < %d%% of timeMediumOld=%d",
                        timeMediumNew, AVG_PCT_MEDIUM_NEW_OLD, timeMediumOld),
                timeMediumNew * 100 < timeMediumOld * AVG_PCT_MEDIUM_NEW_OLD);
        assertTrue(
                String.format("timeLongNew=%d < %d%% of timeLongOld=%d",
                        timeLongNew, AVG_PCT_LONG_NEW_OLD, timeLongOld),
                timeLongNew * 100 < timeLongOld * AVG_PCT_LONG_NEW_OLD);
    }

    private long timeXLocaleMatcher(ULocale desired, XLocaleMatcher matcher, int iterations) {
        long start = System.nanoTime();
        for (int i = iterations; i > 0; --i) {
            matcher.getBestMatch(desired);
        }
        long delta = System.nanoTime() - start;
        return (delta / iterations);
    }

    private long timeLocaleMatcher(ULocale desired, LocaleMatcher matcher, int iterations) {
        long start = System.nanoTime();
        for (int i = iterations; i > 0; --i) {
            matcher.getBestMatch(desired);
        }
        long delta = System.nanoTime() - start;
        return (delta / iterations);
    }

    private static final class TestCase implements Cloneable {
        private static final String ENDL = System.getProperties().getProperty("line.separator");

        int lineNr = 0;

        String nameLine = "";
        String supportedLine = "";
        String defaultLine = "";
        String distanceLine = "";
        String thresholdLine = "";
        String matchLine = "";

        String supported = "";
        String def = "";
        String distance = "";
        String threshold = "";
        String desired = "";
        String expMatch = "";
        String expDesired = "";
        String expCombined = "";

        @Override
        public TestCase clone() throws CloneNotSupportedException {
            return (TestCase) super.clone();
        }

        void reset(String newNameLine) {
            nameLine = newNameLine;
            supportedLine = "";
            defaultLine = "";
            distanceLine = "";
            thresholdLine = "";

            supported = "";
            def = "";
            distance = "";
            threshold = "";
        }

        String toInputsKey() {
            return supported + '+' + def + '+' + distance + '+' + threshold + '+' + desired;
        }

        private static void appendLine(StringBuilder sb, String line) {
            if (!line.isEmpty()) {
                sb.append(ENDL).append(line);
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(nameLine);
            appendLine(sb, supportedLine);
            appendLine(sb, defaultLine);
            appendLine(sb, distanceLine);
            appendLine(sb, thresholdLine);
            sb.append(ENDL).append("line ").append(lineNr).append(':');
            appendLine(sb, matchLine);
            return sb.toString();
        }
    }

    private static String getSuffixAfterPrefix(String s, int limit, String prefix) {
        if (prefix.length() <= limit && s.startsWith(prefix)) {
            return s.substring(prefix.length(), limit);
        } else {
            return null;
        }
    }

    // UsedReflectively, not private to avoid unused-warning
    static List<TestCase> readTestCases() throws Exception {
        List<TestCase> tests = new ArrayList<>();
        Map<String, Integer> uniqueTests = new HashMap<>();
        TestCase test = new TestCase();
        String filename = "data/localeMatcherTest.txt";
        try (BufferedReader in = FileUtilities.openFile(XLocaleMatcherTest.class, filename)) {
            String line;
            while ((line = in.readLine()) != null) {
                ++test.lineNr;
                // Start of comment, or end of line, minus trailing spaces.
                int limit = line.indexOf('#');
                if (limit < 0) {
                    limit = line.length();
                }
                char c;
                while (limit > 0 && ((c = line.charAt(limit - 1)) == ' ' || c == '\t')) {
                    --limit;
                }
                if (limit == 0) {  // empty line
                    continue;
                }
                String suffix;
                if (line.startsWith("** test: ")) {
                    test.reset(line);
                } else if ((suffix = getSuffixAfterPrefix(line, limit, "@supported=")) != null) {
                    test.supportedLine = line;
                    test.supported = suffix;
                } else if ((suffix = getSuffixAfterPrefix(line, limit, "@default=")) != null) {
                    test.defaultLine = line;
                    test.def = suffix;
                } else if ((suffix = getSuffixAfterPrefix(line, limit, "@distance=")) != null) {
                    test.distanceLine = line;
                    test.distance = suffix;
                } else if ((suffix = getSuffixAfterPrefix(line, limit, "@threshold=")) != null) {
                    test.thresholdLine = line;
                    test.threshold = suffix;
                } else {
                    int matchSep = line.indexOf(">>");
                    // >> before an inline comment, and followed by more than white space.
                    if (0 <= matchSep && (matchSep + 2) < limit) {
                        test.matchLine = line;
                        test.desired = line.substring(0, matchSep).trim();
                        test.expDesired = test.expCombined = "";
                        int start = matchSep + 2;
                        int expLimit = line.indexOf('|', start);
                        if (expLimit < 0) {
                            test.expMatch = line.substring(start, limit).trim();
                        } else {
                            test.expMatch = line.substring(start, expLimit).trim();
                            start = expLimit + 1;
                            expLimit = line.indexOf('|', start);
                            if (expLimit < 0) {
                                test.expDesired = line.substring(start, limit).trim();
                            } else {
                                test.expDesired = line.substring(start, expLimit).trim();
                                test.expCombined = line.substring(expLimit + 1, limit).trim();
                            }
                        }
                        String inputs = test.toInputsKey();
                        Integer prevIndex = uniqueTests.get(inputs);
                        if (prevIndex == null) {
                            uniqueTests.put(inputs, tests.size());
                        } else {
                            System.out.println("Locale matcher test case on line " + test.lineNr
                                    + " is a duplicate of line " + tests.get(prevIndex).lineNr);
                        }
                        tests.add(test.clone());
                    } else {
                        throw new IllegalArgumentException("test data syntax error on line "
                                + test.lineNr + "\n" + line);
                    }
                }
            }
        }
        System.out.println("Number of duplicate locale matcher test cases: " + (tests.size() - uniqueTests.size()));
        return tests;
    }

    private static ULocale getULocaleOrNull(String s) {
        if (s.equals("null")) {
            return null;
        } else {
            return new ULocale(s);
        }
    }

    @Test
    @Parameters(method = "readTestCases")
    public void dataDriven(TestCase test) {
        XLocaleMatcher matcher;
        if (test.def.isEmpty() && test.distance.isEmpty() && test.threshold.isEmpty()) {
            matcher = new XLocaleMatcher(test.supported);
        } else {
            XLocaleMatcher.Builder builder = XLocaleMatcher.builder();
            builder.setSupportedLocales(test.supported);
            if (!test.def.isEmpty()) {
                builder.setDefaultLanguage(new ULocale(test.def));
            }
            if (!test.distance.isEmpty()) {
                DistanceOption distance;
                switch (test.distance) {
                case "normal":
                    distance = DistanceOption.REGION_FIRST;
                    break;
                case "script":
                    distance = DistanceOption.SCRIPT_FIRST;
                    break;
                default:
                    throw new IllegalArgumentException("unsupported distance value " + test.distance);
                }
                builder.setDistanceOption(distance);
            }
            if (!test.threshold.isEmpty()) {
                int threshold = Integer.valueOf(test.threshold);
                builder.setThresholdDistance(threshold);
            }
            matcher = builder.build();
        }

        ULocale expMatch = getULocaleOrNull(test.expMatch);
        if (test.expDesired.isEmpty() && test.expCombined.isEmpty()) {
            ULocale bestSupported = matcher.getBestMatch(test.desired);
            assertEquals("bestSupported", expMatch, bestSupported);
        } else {
            LocalePriorityList desired = LocalePriorityList.add(test.desired).build();
            Output<ULocale> bestDesired = new Output<>();
            ULocale bestSupported = matcher.getBestMatch(desired, bestDesired);
            assertEquals("bestSupported", expMatch, bestSupported);
            if (!test.expDesired.isEmpty()) {
                ULocale expDesired = getULocaleOrNull(test.expDesired);
                assertEquals("bestDesired", expDesired, bestDesired.value);
            }
            if (!test.expCombined.isEmpty()) {
                ULocale expCombined = getULocaleOrNull(test.expCombined);
                ULocale combined = XLocaleMatcher.combine(bestSupported, bestDesired.value);
                assertEquals("combined", expCombined, combined);
            }
        }
    }
}
