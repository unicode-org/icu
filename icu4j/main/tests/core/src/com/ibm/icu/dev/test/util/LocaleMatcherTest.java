// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 ******************************************************************************************
 * Copyright (C) 2009-2015, Google, Inc.; International Business Machines Corporation and *
 * others. All Rights Reserved.                                                           *
 ******************************************************************************************
 */

package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.tool.locale.LikelySubtagsBuilder;
import com.ibm.icu.impl.locale.XCldrStub.FileUtilities;
import com.ibm.icu.impl.locale.XLikelySubtags;
import com.ibm.icu.util.LocaleMatcher;
import com.ibm.icu.util.LocaleMatcher.FavorSubtag;
import com.ibm.icu.util.LocalePriorityList;
import com.ibm.icu.util.ULocale;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

/**
 * Test the LocaleMatcher.
 *
 * @author markdavis
 */
@RunWith(JUnitParamsRunner.class)
public class LocaleMatcherTest extends TestFmwk {
    private static final ULocale ZH_MO = new ULocale("zh_MO");
    private static final ULocale ZH_HK = new ULocale("zh_HK");

    private LocaleMatcher newLocaleMatcher(LocalePriorityList build) {
        return new LocaleMatcher(build);
    }

    private LocaleMatcher newLocaleMatcher(String string) {
        return new LocaleMatcher(LocalePriorityList.add(string).build());
    }

    @Test
    public void testParentLocales() {
        assertCloser("es_AR", "es_419", "es_ES");
        assertCloser("es_AR", "es_419", "es");

        assertCloser("es_AR", "es_MX", "es");
        assertCloser("es_AR", "es_MX", "es");

        assertCloser("en_AU", "en_GB", "en_US");
        assertCloser("en_AU", "en_GB", "en");

        assertCloser("en_AU", "en_NZ", "en_US");
        assertCloser("en_AU", "en_NZ", "en");

        assertCloser("pt_AO", "pt_PT", "pt_BR");
        assertCloser("pt_AO", "pt_PT", "pt");

        assertCloser("zh_HK", "zh_MO", "zh_TW");
        assertCloser("zh_HK", "zh_MO", "zh_CN");
        assertCloser("zh_HK", "zh_MO", "zh");
    }

    private void assertCloser(String a, String closer, String further) {
        LocaleMatcher matcher = newLocaleMatcher(further + ", " + closer);
        assertEquals("test " + a + " is closer to " + closer + " than to " + further, new ULocale(closer), matcher.getBestMatch(a));
        matcher = newLocaleMatcher(closer + ", " + further);
        assertEquals("test " + a + " is closer to " + closer + " than to " + further, new ULocale(closer), matcher.getBestMatch(a));
    }

    @Test
    public void testChinese() {
        LocaleMatcher matcher = newLocaleMatcher("zh_CN, zh_TW, iw");
        ULocale taiwanChinese = new ULocale("zh_TW");
        ULocale chinaChinese = new ULocale("zh_CN");
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese, matcher.getBestMatch("zh_Hant_TW"));
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese, matcher.getBestMatch("zh_Hant"));
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese, matcher.getBestMatch("zh_TW"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese, matcher.getBestMatch("zh_Hans_CN"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese, matcher.getBestMatch("zh_CN"));
        assertEquals("zh_CN, zh_TW, iw;", chinaChinese, matcher.getBestMatch("zh"));
        assertEquals("zh_CN, zh_TW, iw;", taiwanChinese, matcher.getBestMatch("zh_Hant_HK"));
    }

    @Test
    public void testenGB() {
        final LocaleMatcher matcher = newLocaleMatcher("fr, en, en_GB, es_MX, es_419, es");
        assertEquals("en_GB", matcher.getBestMatch("en_NZ").toString());
        assertEquals("es", matcher.getBestMatch("es_ES").toString());
        assertEquals("es_419", matcher.getBestMatch("es_AR").toString());
        assertEquals("es_MX", matcher.getBestMatch("es_MX").toString());
    }

    @Test
    public void testFallbacks() {
        LocalePriorityList lpl = LocalePriorityList.add("en, hi").build();
        final LocaleMatcher matcher = newLocaleMatcher(lpl);
        assertEquals("hi", matcher.getBestMatch("sa").toString());
    }

    @Test
    public void testBasics() {
        LocaleMatcher matcher = newLocaleMatcher(
                LocalePriorityList.
                    add(ULocale.FRENCH).add(ULocale.UK).add(ULocale.ENGLISH).
                    build());
        logln(matcher.toString());

        assertEquals(ULocale.UK, matcher.getBestMatch(ULocale.UK));
        assertEquals(ULocale.ENGLISH, matcher.getBestMatch(ULocale.US));
        assertEquals(ULocale.FRENCH, matcher.getBestMatch(ULocale.FRANCE));
        assertEquals(ULocale.FRENCH, matcher.getBestMatch(ULocale.JAPAN));

        // API coverage
        List<Locale> locales = new ArrayList<>();
        locales.add(Locale.FRENCH);
        locales.add(Locale.UK);
        matcher = LocaleMatcher.builder().
                setSupportedLocales(locales).addSupportedLocale(Locale.ENGLISH).
                setDefaultLocale(Locale.GERMAN).build();
        assertEquals(ULocale.UK, matcher.getBestMatch(ULocale.UK));
        assertEquals(ULocale.ENGLISH, matcher.getBestMatch(ULocale.US));
        assertEquals(ULocale.FRENCH, matcher.getBestMatch(ULocale.FRANCE));
        assertEquals(ULocale.GERMAN, matcher.getBestMatch(ULocale.JAPAN));

        ULocale udesired = new ULocale("en_GB");  // distinct object from ULocale.UK
        LocaleMatcher.Result result = matcher.getBestMatchResult(udesired);
        assertTrue("exactly desired en-GB object", udesired == result.getDesiredULocale());
        assertEquals(Locale.UK, result.getDesiredLocale());
        assertEquals(0, result.getDesiredIndex());
        assertEquals(ULocale.UK, result.getSupportedULocale());
        assertEquals(Locale.UK, result.getSupportedLocale());
        assertEquals(1, result.getSupportedIndex());

        LocalePriorityList list = LocalePriorityList.add(ULocale.JAPAN, ULocale.US).build();
        result = matcher.getBestMatchResult(list);
        assertEquals(1, result.getDesiredIndex());
        assertEquals(Locale.US, result.getDesiredLocale());

        Locale desired = new Locale("en", "US");  // distinct object from Locale.US
        result = matcher.getBestLocaleResult(desired);
        assertEquals(ULocale.US, result.getDesiredULocale());
        assertTrue("exactly desired en-US object", desired == result.getDesiredLocale());
        assertEquals(0, result.getDesiredIndex());
        assertEquals(ULocale.ENGLISH, result.getSupportedULocale());
        assertEquals(Locale.ENGLISH, result.getSupportedLocale());
        assertEquals(2, result.getSupportedIndex());

        result = matcher.getBestMatchResult(ULocale.JAPAN);
        assertNull(result.getDesiredLocale());
        assertNull(result.getDesiredULocale());
        assertEquals(-1, result.getDesiredIndex());
        assertEquals(ULocale.GERMAN, result.getSupportedULocale());
        assertEquals(Locale.GERMAN, result.getSupportedLocale());
        assertEquals(-1, result.getSupportedIndex());
    }

    private static final String locString(ULocale loc) {
        return loc != null ? loc.getName() : "(null)";
    }

    @Test
    public void testSupportedDefault() {
        // The default locale is one of the supported locales.
        List<ULocale> locales = Arrays.asList(
                new ULocale("fr"), new ULocale("en_GB"), new ULocale("en"));
        LocaleMatcher matcher = LocaleMatcher.builder().
            setSupportedULocales(locales).
            setDefaultULocale(locales.get(1)).
            build();
        ULocale best = matcher.getBestMatch("en_GB");
        assertEquals("getBestMatch(en_GB)", "en_GB", locString(best));
        best = matcher.getBestMatch("en_US");
        assertEquals("getBestMatch(en_US)", "en", locString(best));
        best = matcher.getBestMatch("fr_FR");
        assertEquals("getBestMatch(fr_FR)", "fr", locString(best));
        best = matcher.getBestMatch("ja_JP");
        assertEquals("getBestMatch(ja_JP)", "en_GB", locString(best));
        LocaleMatcher.Result result = matcher.getBestMatchResult(new ULocale("ja_JP"));
        assertEquals("getBestMatchResult(ja_JP).supp",
                     "en_GB", locString(result.getSupportedULocale()));
        assertEquals("getBestMatchResult(ja_JP).suppIndex",
                     -1, result.getSupportedIndex());
    }

    @Test
    public void testUnsupportedDefault() {
        // The default locale does not match any of the supported locales.
        List<ULocale> locales = Arrays.asList(
                new ULocale("fr"), new ULocale("en_GB"), new ULocale("en"));
        LocaleMatcher matcher = LocaleMatcher.builder().
            setSupportedULocales(locales).
            setDefaultULocale(new ULocale("de")).
            build();
        ULocale best = matcher.getBestMatch("en_GB");
        assertEquals("getBestMatch(en_GB)", "en_GB", locString(best));
        best = matcher.getBestMatch("en_US");
        assertEquals("getBestMatch(en_US)", "en", locString(best));
        best = matcher.getBestMatch("fr_FR");
        assertEquals("getBestMatch(fr_FR)", "fr", locString(best));
        best = matcher.getBestMatch("ja_JP");
        assertEquals("getBestMatch(ja_JP)", "de", locString(best));
        LocaleMatcher.Result result = matcher.getBestMatchResult(new ULocale("ja_JP"));
        assertEquals("getBestMatchResult(ja_JP).supp",
                     "de", locString(result.getSupportedULocale()));
        assertEquals("getBestMatchResult(ja_JP).suppIndex",
                     -1, result.getSupportedIndex());
    }

    @Test
    public void testNoDefault() {
        // We want null instead of any default locale.
        List<ULocale> locales = Arrays.asList(
                new ULocale("fr"), new ULocale("en_GB"), new ULocale("en"));
        LocaleMatcher matcher = LocaleMatcher.builder().
            setSupportedULocales(locales).
            setNoDefaultLocale().
            build();
        ULocale best = matcher.getBestMatch("en_GB");
        assertEquals("getBestMatch(en_GB)", "en_GB", locString(best));
        best = matcher.getBestMatch("en_US");
        assertEquals("getBestMatch(en_US)", "en", locString(best));
        best = matcher.getBestMatch("fr_FR");
        assertEquals("getBestMatch(fr_FR)", "fr", locString(best));
        best = matcher.getBestMatch("ja_JP");
        assertEquals("getBestMatch(ja_JP)", "(null)", locString(best));
        LocaleMatcher.Result result = matcher.getBestMatchResult(new ULocale("ja_JP"));
        assertEquals("getBestMatchResult(ja_JP).supp",
                     "(null)", locString(result.getSupportedULocale()));
        assertEquals("getBestMatchResult(ja_JP).suppIndex",
                     -1, result.getSupportedIndex());
    }

    @Test
    public void testFallback() {
        // check that script fallbacks are handled right
        final LocaleMatcher matcher = newLocaleMatcher("zh_CN, zh_TW, iw");
        assertEquals(new ULocale("zh_TW"), matcher.getBestMatch("zh_Hant"));
        assertEquals(new ULocale("zh_CN"), matcher.getBestMatch("zh"));
        assertEquals(new ULocale("zh_CN"), matcher.getBestMatch("zh_Hans_CN"));
        assertEquals(new ULocale("zh_TW"), matcher.getBestMatch("zh_Hant_HK"));
        assertEquals(new ULocale("iw"), matcher.getBestMatch("iw_IT"));
    }

    @Test
    public void testSpecials() {
        // check that nearby languages are handled
        final LocaleMatcher matcher = newLocaleMatcher("en, fil, ro, nn");
        assertEquals(new ULocale("fil"), matcher.getBestMatch("tl"));
        assertEquals(new ULocale("ro"), matcher.getBestMatch("mo"));
        assertEquals(new ULocale("nn"), matcher.getBestMatch("nb"));
        // make sure default works
        assertEquals(new ULocale("en"), matcher.getBestMatch("ja"));
    }

    @Test
    public void testRegionalSpecials() {
        // verify that en_AU is closer to en_GB than to en (which is en_US)
        final LocaleMatcher matcher = newLocaleMatcher("en, en_GB, es, es_419");
        assertEquals("es_MX in {en, en_GB, es, es_419}", new ULocale("es_419"), matcher.getBestMatch("es_MX"));
        assertEquals("en_AU in {en, en_GB, es, es_419}", new ULocale("en_GB"), matcher.getBestMatch("en_AU"));
        assertEquals("es_ES in {en, en_GB, es, es_419}", new ULocale("es"), matcher.getBestMatch("es_ES"));
    }

    @Test
    public void testHK() {
        // HK and MO are closer to each other for Hant than to TW
        final LocaleMatcher matcher = newLocaleMatcher("zh, zh_TW, zh_MO");
        assertEquals("zh_HK in {zh, zh_TW, zh_MO}", ZH_MO, matcher.getBestMatch("zh_HK"));
        final LocaleMatcher matcher2 = newLocaleMatcher("zh, zh_TW, zh_HK");
        assertEquals("zh_MO in {zh, zh_TW, zh_HK}", ZH_HK, matcher2.getBestMatch("zh_MO"));
    }

    @Test
    public void TestLocaleMatcherCoverage() {
        // Add tests for better code coverage
        LocaleMatcher matcher = newLocaleMatcher(LocalePriorityList.add(null, 0).build());
        logln(matcher.toString());
    }

    private void assertEquals(Object expected, Object string) {
        assertEquals("", expected, string);
    }

    private void assertNull(Object bestMatch) {
        assertNull("", bestMatch);
    }

    @Test
    public void testEmpty() {
        final LocaleMatcher matcher = LocaleMatcher.builder().build();
        assertNull(matcher.getBestMatch(ULocale.FRENCH));
        LocaleMatcher.Result result = matcher.getBestMatchResult(ULocale.FRENCH);
        assertNull(result.getDesiredULocale());
        assertNull(result.getDesiredLocale());
        assertEquals(-1, result.getDesiredIndex());
        assertNull(result.getSupportedULocale());
        assertNull(result.getSupportedLocale());
        assertEquals(-1, result.getSupportedIndex());
    }

    static final ULocale ENGLISH_CANADA = new ULocale("en_CA");

    private static double match(ULocale a, ULocale b) {
        final LocaleMatcher matcher = new LocaleMatcher("");
        return matcher.match(a, null, b, null);
    }

    @Test
    public void testMatch_exact() {
        assertEquals(1.0, match(ENGLISH_CANADA, ENGLISH_CANADA));
    }

    @Test
    public void testMatch_none() {
        double match = match(new ULocale("ar_MK"), ENGLISH_CANADA);
        assertTrue("Actual >= 0: " + match, 0 <= match);
        assertTrue("Actual < 0.2 (~ language + script distance): " + match, 0.2 > match);
    }

    @Test
    public void testMatch_matchOnMaximized() {
        ULocale undTw = new ULocale("und_TW");
        ULocale zhHant = new ULocale("zh_Hant");
        double matchZh = match(undTw, new ULocale("zh"));
        double matchZhHant = match(undTw, zhHant);
        assertTrue("und_TW should be closer to zh_Hant (" + matchZhHant +
            ") than to zh (" + matchZh + ")",
            matchZh < matchZhHant);
        double matchEnHantTw = match(new ULocale("en_Hant_TW"), zhHant);
        assertTrue("zh_Hant should be closer to und_TW (" + matchZhHant +
            ") than to en_Hant_TW (" + matchEnHantTw + ")",
            matchEnHantTw < matchZhHant);
        assertTrue("zh should not match und_TW (" + matchZh +
            ") or en_Hant_TW (" + matchEnHantTw + ")",
            matchZh == 0.0 && matchEnHantTw == 0.0);
    }

    @Test
    public void testResolvedLocale() {
        LocaleMatcher matcher = LocaleMatcher.builder().
            addSupportedULocale(new ULocale("ar-EG")).
            build();
        ULocale desired = new ULocale("ar-SA-u-nu-latn");
        LocaleMatcher.Result result = matcher.getBestMatchResult(desired);
        assertEquals("best", "ar_EG", result.getSupportedLocale().toString());
        ULocale resolved = result.makeResolvedULocale();
        assertEquals("ar-EG + ar-SA-u-nu-latn = ar-SA-u-nu-latn",
                     "ar-SA-u-nu-latn",
                     resolved.toLanguageTag());
    }

    @Test
    public void testMatchLegacyCode() {
        final LocaleMatcher matcher = newLocaleMatcher("fr, i_klingon, en_Latn_US");
        assertEquals("en_Latn_US", matcher.getBestMatch("en_GB_oed").toString());
        // assertEquals("tlh", matcher.getBestMatch("i_klingon").toString());
    }

    @Test
    public void testGetBestMatchForList_exactMatch() {
        final LocaleMatcher matcher = newLocaleMatcher("fr, en_GB, ja, es_ES, es_MX");
        assertEquals("ja", matcher.getBestMatch("ja, de").toString());
    }

    @Test
    public void testGetBestMatchForList_simpleVariantMatch() {
        final LocaleMatcher matcher = newLocaleMatcher("fr, en_GB, ja, es_ES, es_MX");
        // Intentionally avoiding a perfect_match or two candidates for variant
        // matches.
        assertEquals("en_GB", matcher.getBestMatch("de, en_US").toString());
        // Fall back.
        assertEquals("fr", matcher.getBestMatch("de, zh").toString());
    }

    @Test
    public void testGetBestMatchForList_matchOnMaximized() {
        final LocaleMatcher matcher = newLocaleMatcher("en, ja");
        // final LocaleMatcher matcher =
        // newLocaleMatcher("fr, en, ja, es_ES, es_MX");
        // Check that if the preference is maximized already, it works as well.
        assertEquals("Match for ja_Jpan_JP (maximized already)",
            "ja", matcher.getBestMatch("ja_Jpan_JP, en-AU").toString());
        if (true)
            return;
        // ja_JP matches ja on likely subtags, and it's listed first, thus it
        // wins over
        // thus it wins over the second preference en_GB.
        assertEquals("Match for ja_JP, with likely region subtag",
            "ja", matcher.getBestMatch("ja_JP, en_US").toString());
        // Check that if the preference is maximized already, it works as well.
        assertEquals("Match for ja_Jpan_JP (maximized already)",
            "ja", matcher.getBestMatch("ja_Jpan_JP, en_US").toString());
    }

    @Test
    public void testGetBestMatchForList_noMatchOnMaximized() {
        // Regression test for http://b/5714572 .
        final LocaleMatcher matcher = newLocaleMatcher("en, de, fr, ja");
        // de maximizes to de_DE. Pick the exact match for the secondary
        // language instead.
        assertEquals("de", matcher.getBestMatch("de_CH, fr").toString());
    }

    @Test
    public void testBestMatchForTraditionalChinese() {
        // Scenario: An application that only supports Simplified Chinese (and
        // some other languages),
        // but does not support Traditional Chinese. zh_Hans_CN could be
        // replaced with zh_CN, zh, or
        // zh_Hans, it wouldn't make much of a difference.
        final LocaleMatcher matcher = newLocaleMatcher("fr, zh_Hans_CN, en_US");

        // The script distance (simplified vs. traditional Han) is now considered
        // no match, so we just get the first entry.
        assertEquals("fr", matcher.getBestMatch("zh_TW").toString());
        assertEquals("fr", matcher.getBestMatch("zh_Hant").toString());

        // For geo_political reasons, you might want to avoid a zh_Hant ->
        // zh_Hans match.
        // In this case, if zh_TW, zh_HK or a tag starting with zh_Hant is
        // requested, you can
        // change your call to getBestMatch to include a 2nd language
        // preference.
        // "en" is a better match since its distance to "en_US" is closer than
        // the distance
        // from "zh_TW" to "zh_CN" (script distance).
        assertEquals("en_US", matcher.getBestMatch("zh_TW, en").toString());
        assertEquals("en_US", matcher.getBestMatch("zh_Hant_CN, en").toString());
        assertEquals("zh_Hans_CN", matcher.getBestMatch("zh_Hans, en").toString());
    }

    @Test
    public void testUndefined() {
        // When the undefined language doesn't match anything in the list,
        // getBestMatch returns
        // the default, as usual.
        LocaleMatcher matcher = newLocaleMatcher("it,fr");
        assertEquals("it", matcher.getBestMatch("und").toString());

        // When it *does* occur in the list, BestMatch returns it, as expected.
        matcher = newLocaleMatcher("it,und");
        assertEquals("", matcher.getBestMatch("und").toString());

        // The unusual part:
        // max("und") = "en_Latn_US", and since matching is based on maximized
        // tags, the undefined
        // language would normally match English. But that would produce the
        // counterintuitive results
        // that getBestMatch("und", LocaleMatcher("it,en")) would be "en", and
        // getBestMatch("en", LocaleMatcher("it,und")) would be "und".
        //
        // To avoid that, we change the matcher's definitions of max
        // (AddLikelySubtagsWithDefaults)
        // so that max("und")="und". That produces the following, more desirable
        // results:
        matcher = newLocaleMatcher("it,en");
        assertEquals("it", matcher.getBestMatch("und").toString());
        matcher = newLocaleMatcher("it,und");
        assertEquals("it", matcher.getBestMatch("en").toString());
    }

    @Test
    public void testGetBestMatch_googlePseudoLocales() {
        // Google pseudo locales are primarily based on variant subtags.
        // (See below for the region code based fall back options.)
        final LocaleMatcher matcher = newLocaleMatcher(
            "fr, pt");
        assertEquals("fr", matcher.getBestMatch("de").toString());
        assertEquals("fr", matcher.getBestMatch("en_US").toString());
        assertEquals("fr", matcher.getBestMatch("en").toString());
        assertEquals("pt", matcher.getBestMatch("pt_BR").toString());
    }

    @Test
    public void testGetBestMatch_regionDistance() {
        LocaleMatcher matcher = newLocaleMatcher("es_AR, es");
        assertEquals("es_AR", matcher.getBestMatch("es_MX").toString());

        matcher = newLocaleMatcher("fr, en, en_GB");
        assertEquals("en_GB", matcher.getBestMatch("en_CA").toString());

        matcher = newLocaleMatcher("de_AT, de_DE, de_CH");
        assertEquals("de_DE", matcher.getBestMatch("de").toString());

        showDistance(matcher, "en", "en_CA");
        showDistance(matcher, "en_CA", "en");
        showDistance(matcher, "en_US", "en_CA");
        showDistance(matcher, "en_CA", "en_US");
        showDistance(matcher, "en_GB", "en_CA");
        showDistance(matcher, "en_CA", "en_GB");
        showDistance(matcher, "en", "en_UM");
        showDistance(matcher, "en_UM", "en");
    }

    private void showDistance(LocaleMatcher matcher, String desired, String supported) {
        ULocale desired2 = new ULocale(desired);
        ULocale supported2 = new ULocale(supported);
        double distance = matcher.match(desired2, ULocale.addLikelySubtags(desired2), supported2, ULocale.addLikelySubtags(supported2));
        logln(desired + " to " + supported + " :\t" + distance);
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

    private static final ULocale posix = new ULocale("en_US_POSIX");

    /**
     * @param sorted
     */
    private void check2(Set<ULocale> sorted) {
        logln("Checking: " + sorted);
        LocaleMatcher matcher = newLocaleMatcher(
            LocalePriorityList.add(
                sorted.toArray(new ULocale[sorted.size()]))
            .build());
        for (ULocale loc : sorted) {
            // The result may not be the exact same locale, but it must be equivalent.
            // Variants and extensions are ignored.
            if (loc.equals(posix)) { continue; }
            ULocale max = ULocale.addLikelySubtags(loc);
            ULocale best = matcher.getBestMatch(loc);
            ULocale maxBest = ULocale.addLikelySubtags(best);
            assertEquals(loc.toString(), max, maxBest);
        }
    }

    @Test
    public void testAsymmetry() {
        LocaleMatcher matcher;
        matcher = new LocaleMatcher("mul, nl");
        assertEquals("nl", matcher.getBestMatch("af").toString()); // af => nl

        matcher = new LocaleMatcher("mul, af");
        assertEquals("mul", matcher.getBestMatch("nl").toString()); // but nl !=> af
    }


    @Test
    public void testGetBestMatchForList_matchOnMaximized2() {
        final LocaleMatcher matcher = newLocaleMatcher("fr, en-GB, ja, es-ES, es-MX");
        // ja-JP matches ja on likely subtags, and it's listed first, thus it wins over
        // thus it wins over the second preference en-GB.
        assertEquals("Match for ja-JP, with likely region subtag",
            "ja", matcher.getBestMatch("ja-JP, en-GB").toString());
        // Check that if the preference is maximized already, it works as well.
        assertEquals("Match for ja-Jpan-JP (maximized already)",
            "ja", matcher.getBestMatch("ja-Jpan-JP, en-GB").toString());
    }

    @Test
    public void testGetBestMatchForList_closeEnoughMatchOnMaximized() {
        final LocaleMatcher matcher = newLocaleMatcher("en-GB, en, de, fr, ja");
        assertEquals("de", matcher.getBestMatch("de-CH, fr").toString());
        assertEquals("en", matcher.getBestMatch("en-US, ar, nl, de, ja").toString());
    }

    @Test
    public void testGetBestMatchForPortuguese() {
        final LocaleMatcher withPTExplicit = newLocaleMatcher("pt_PT, pt_BR, es, es_419");
        final LocaleMatcher withPTImplicit = newLocaleMatcher("pt_PT, pt, es, es_419");
        // Could happen because "pt_BR" is a tier_1 language and "pt_PT" is tier_2.

        final LocaleMatcher withoutPT = newLocaleMatcher("pt_BR, es, es_419");
        // European user who prefers Spanish over Brazilian Portuguese as a fallback.

        assertEquals("pt_PT", withPTExplicit.getBestMatch("pt_PT, es, pt").toString());
        assertEquals("pt_PT", withPTImplicit.getBestMatch("pt_PT, es, pt").toString());
        // The earlier pt_PT vs. pt_BR region mismatch is as good as the later es perfect match
        // because of the demotion per desired locale.
        assertEquals("pt_BR", withoutPT.getBestMatch("pt_PT, es, pt").toString());

        // Brazilian user who prefers South American Spanish over European Portuguese as a fallback.
        // The asymmetry between this case and above is because it's "pt_PT" that's missing between the
        // matchers as "pt_BR" is a much more common language.
        assertEquals("pt_BR", withPTExplicit.getBestMatch("pt, es_419, pt_PT").toString());
        assertEquals("pt", withPTImplicit.getBestMatch("pt, es_419, pt_PT").toString());
        assertEquals("pt_BR", withoutPT.getBestMatch("pt, es_419, pt_PT").toString());

        // Code that adds the user's country can get "pt_US" for a user's language.
        // That should fall back to "pt_BR".
        assertEquals("pt_BR", withPTExplicit.getBestMatch("pt_US, pt_PT").toString());
        assertEquals("pt", withPTImplicit.getBestMatch("pt_US, pt_PT").toString());
    }

    @Test
    public void testVariantWithScriptMatch() {
        final LocaleMatcher matcher = newLocaleMatcher("fr, en, sv");
        assertEquals("en", matcher.getBestMatch("en-GB").toString());
        assertEquals("en", matcher.getBestMatch("en-GB, sv").toString());
    }

    @Test
    public void testVariantWithScriptMatch2() {
        final LocaleMatcher matcher = newLocaleMatcher("en, sv");
        assertEquals("en", matcher.getBestMatch("en-GB, sv").toString());
    }

    @Test
    public void Test8288() {
        final LocaleMatcher matcher = newLocaleMatcher("it, en");
        assertEquals("it", matcher.getBestMatch("und").toString());
        assertEquals("en", matcher.getBestMatch("und, en").toString());
    }

    @Test
    public void testDemotion() {
        LocalePriorityList supported = LocalePriorityList.add("fr, de-CH, it").build();
        LocalePriorityList desired = LocalePriorityList.add("fr-CH, de-CH, it").build();
        LocaleMatcher noDemotion = LocaleMatcher.builder().
                setSupportedULocales(supported.getULocales()).
                setDemotionPerDesiredLocale(LocaleMatcher.Demotion.NONE).build();
        assertEquals("no demotion", new ULocale("de-CH"), noDemotion.getBestMatch(desired));

        LocaleMatcher regionDemotion = LocaleMatcher.builder().
                setSupportedULocales(supported.getULocales()).
                setDemotionPerDesiredLocale(LocaleMatcher.Demotion.REGION).build();
        assertEquals("region demotion", ULocale.FRENCH, regionDemotion.getBestMatch(desired));
    }

    @Test
    public void testDirection() {
        List<ULocale> desired = Arrays.asList(new ULocale("arz-EG"), new ULocale("nb-DK"));
        LocaleMatcher.Builder builder =
                LocaleMatcher.builder().setSupportedLocales("ar, nn");
        // arz is a close one-way match to ar, and the region matches.
        // (Egyptian Arabic vs. Arabic)
        LocaleMatcher withOneWay = builder.build();
        assertEquals("with one-way", "ar", withOneWay.getBestMatch(desired).toString());
        // nb is a less close two-way match to nn, and the regions differ.
        // (Norwegian Bokmal vs. Nynorsk)
        LocaleMatcher onlyTwoWay = builder.setDirection(LocaleMatcher.Direction.ONLY_TWO_WAY).build();
        assertEquals("only two-way", "nn", onlyTwoWay.getBestMatch(desired).toString());
    }

    @Test
    public void testMaxDistanceAndIsMatch() {
        LocaleMatcher.Builder builder = LocaleMatcher.builder();
        LocaleMatcher standard = builder.build();
        ULocale germanLux = new ULocale("de-LU");
        ULocale germanPhoenician = new ULocale("de-Phnx-AT");
        ULocale greek = new ULocale("el");
        assertTrue("standard de-LU / de", standard.isMatch(germanLux, ULocale.GERMAN));
        assertFalse("standard de-Phnx-AT / de", standard.isMatch(germanPhoenician, ULocale.GERMAN));

        // Allow a script difference to still match.
        LocaleMatcher loose = builder.setMaxDistance(germanPhoenician, ULocale.GERMAN).build();
        assertTrue("loose de-LU / de", loose.isMatch(germanLux, ULocale.GERMAN));
        assertTrue("loose de-Phnx-AT / de", loose.isMatch(germanPhoenician, ULocale.GERMAN));
        assertFalse("loose el / de", loose.isMatch(greek, ULocale.GERMAN));

        // Allow at most a regional difference.
        LocaleMatcher regional = builder.setMaxDistance(new Locale("de", "AT"), Locale.GERMAN).build();
        assertTrue("regional de-LU / de", regional.isMatch(new Locale("de", "LU"), Locale.GERMAN));
        assertFalse("regional da / no", regional.isMatch(new Locale("da"), new Locale("no")));
        assertFalse("regional zh-Hant / zh", regional.isMatch(Locale.CHINESE, Locale.TRADITIONAL_CHINESE));
    }

    @Test
    public void testCanonicalize() {
        LocaleMatcher matcher = LocaleMatcher.builder().build();
        assertEquals("bh --> bho", new ULocale("bho"), matcher.canonicalize(new ULocale("bh")));
        assertEquals("mo-200 --> ro-CZ", new ULocale("ro_CZ"),
                matcher.canonicalize(new ULocale("mo_200")));
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

    @Test
    public void testPerf() {
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

        final LocaleMatcher matcherShort = newLocaleMatcher(shortList);
        final LocaleMatcher matcherLong = newLocaleMatcher(longList);
        final LocaleMatcher matcherVeryLong = newLocaleMatcher(veryLongList);

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

            assertEquals(desired.toString(), pc.expectedShort, matcherShort.getBestMatch(desired));
            assertEquals(desired.toString(), pc.expectedLong, matcherLong.getBestMatch(desired));
            assertEquals(desired.toString(), pc.expectedVeryLong, matcherVeryLong.getBestMatch(desired));

            timeLocaleMatcher(desired, matcherShort, WARM_UP_ITERATIONS);
            timeLocaleMatcher(desired, matcherLong, WARM_UP_ITERATIONS);
            timeLocaleMatcher(desired, matcherVeryLong, WARM_UP_ITERATIONS);
            long tns = timeLocaleMatcher(desired, matcherShort, BENCHMARK_ITERATIONS);
            System.out.format("New Duration (few  supported):\t%s\t%d\tnanos\n", desired, tns);
            long tnl = timeLocaleMatcher(desired, matcherLong, BENCHMARK_ITERATIONS);
            System.out.format("New Duration (med. supported):\t%s\t%d\tnanos\n", desired, tnl);
            long tnv = timeLocaleMatcher(desired, matcherVeryLong, BENCHMARK_ITERATIONS);
            System.out.format("New Duration (many supported):\t%s\t%d\tnanos\n", desired, tnv);
        }

        maximizePerf();
    }

    private static long timeLocaleMatcher(ULocale desired, LocaleMatcher matcher, int iterations) {
        long start = System.nanoTime();
        for (int i = iterations; i > 0; --i) {
            matcher.getBestMatch(desired);
        }
        long delta = System.nanoTime() - start;
        return (delta / iterations);
    }

    private void maximizePerf() {
        final String tags = "af, am, ar, az, be, bg, bn, bs, ca, cs, cy, cy, da, de, " +
                "el, en, en-GB, es, es-419, et, eu, fa, fi, fil, fr, ga, gl, gu, " +
                "hi, hr, hu, hy, id, is, it, iw, ja, ka, kk, km, kn, ko, ky, lo, lt, lv, " +
                "mk, ml, mn, mr, ms, my, ne, nl, no, pa, pl, pt, pt-PT, ro, ru, " +
                "si, sk, sl, sq, sr, sr-Latn, sv, sw, ta, te, th, tr, uk, ur, uz, vi, " +
                "zh-CN, zh-TW, zu";
        LocalePriorityList list = LocalePriorityList.add(tags).build();
        int few = 1000;
        long t = timeMaximize(list, few);  // warm up
        t = timeMaximize(list, few);  // measure for scale
        long targetTime = 100000000L;  // 10^8 ns = 0.1s
        int iterations = (int)((targetTime * few) / t);
        t = timeMaximize(list, iterations);
        int length = 0;
        for (@SuppressWarnings("unused") ULocale locale : list) { ++length; }
        System.out.println("maximize: " + (t / iterations / length) + " ns/locale: " +
                t + " ns / " + iterations + " iterations / " + length + " locales");
    }

    // returns total ns not per iteration
    private  static long timeMaximize(Iterable<ULocale> list, int iterations) {
        long start = System.nanoTime();
        for (int i = iterations; i > 0; --i) {
            for (ULocale locale : list) {
                XLikelySubtags.INSTANCE.makeMaximizedLsrFrom(locale);
            }
        }
        return System.nanoTime() - start;
    }

    @Test
    public void testLikelySubtagsLoadedDataSameAsBuiltFromScratch() {
        XLikelySubtags.Data built = LikelySubtagsBuilder.build();
        XLikelySubtags.Data loaded = XLikelySubtags.Data.load();
        assertEquals("run LocaleDistanceBuilder and update ICU4C langInfo.txt", built, loaded);
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
        String favor = "";
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
            favor = "";
            threshold = "";
        }

        String toInputsKey() {
            return supported + '+' + def + '+' + favor + '+' + threshold + '+' + desired;
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
        try (BufferedReader in = FileUtilities.openFile(LocaleMatcherTest.class, filename)) {
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
                } else if ((suffix = getSuffixAfterPrefix(line, limit, "@favor=")) != null) {
                    test.distanceLine = line;
                    test.favor = suffix;
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

    private static Locale toLocale(ULocale ulocale) {
        return ulocale != null ? ulocale.toLocale() : null;
    }

    private static Iterable<Locale> localesFromULocales(Collection<ULocale> ulocales) {
        List<Locale> locales = new ArrayList<>(ulocales.size());
        for (ULocale ulocale : ulocales) {
            locales.add(ulocale.toLocale());
        }
        return locales;
    }

    @Test
    @Parameters(method = "readTestCases")
    public void dataDriven(TestCase test) {
        LocaleMatcher matcher;
        if (test.def.isEmpty() && test.favor.isEmpty() && test.threshold.isEmpty()) {
            matcher = new LocaleMatcher(test.supported);
        } else {
            LocaleMatcher.Builder builder = LocaleMatcher.builder();
            builder.setSupportedLocales(test.supported);
            if (!test.def.isEmpty()) {
                builder.setDefaultULocale(new ULocale(test.def));
            }
            if (!test.favor.isEmpty()) {
                FavorSubtag favor;
                switch (test.favor) {
                case "normal":
                    favor = FavorSubtag.LANGUAGE;
                    break;
                case "script":
                    favor = FavorSubtag.SCRIPT;
                    break;
                default:
                    throw new IllegalArgumentException("unsupported FavorSubtag value " + test.favor);
                }
                builder.setFavorSubtag(favor);
            }
            if (!test.threshold.isEmpty()) {
                int threshold = Integer.valueOf(test.threshold);
                builder.internalSetThresholdDistance(threshold);
            }
            matcher = builder.build();
        }

        ULocale expMatch = getULocaleOrNull(test.expMatch);
        if (test.expDesired.isEmpty() && test.expCombined.isEmpty()) {
            ULocale bestSupported = matcher.getBestMatch(test.desired);
            assertEquals("bestSupported ULocale from string", expMatch, bestSupported);
            LocalePriorityList desired = LocalePriorityList.add(test.desired).build();
            if (desired.getULocales().size() == 1) {
                ULocale desiredULocale = desired.iterator().next();
                bestSupported = matcher.getBestMatch(desiredULocale);
                assertEquals("bestSupported ULocale from ULocale", expMatch, bestSupported);
                Locale desiredLocale = desiredULocale.toLocale();
                Locale bestSupportedLocale = matcher.getBestLocale(desiredLocale);
                assertEquals("bestSupported Locale from Locale",
                        toLocale(expMatch), bestSupportedLocale);

                LocaleMatcher.Result result = matcher.getBestMatchResult(desiredULocale);
                assertEquals("result.getSupportedULocale from ULocale",
                        expMatch, result.getSupportedULocale());
                assertEquals("result.getSupportedLocale from ULocale",
                        toLocale(expMatch), result.getSupportedLocale());

                result = matcher.getBestLocaleResult(desiredLocale);
                assertEquals("result.getSupportedULocale from Locale",
                        expMatch, result.getSupportedULocale());
                assertEquals("result.getSupportedLocale from Locale",
                        toLocale(expMatch), result.getSupportedLocale());
            } else {
                bestSupported = matcher.getBestMatch(desired);
                assertEquals("bestSupported ULocale from ULocale iterator",
                        expMatch, bestSupported);
                Locale bestSupportedLocale = matcher.getBestLocale(
                        localesFromULocales(desired.getULocales()));
                assertEquals("bestSupported Locale from Locale iterator",
                        toLocale(expMatch), bestSupportedLocale);
            }
        } else {
            LocalePriorityList desired = LocalePriorityList.add(test.desired).build();
            LocaleMatcher.Result result = matcher.getBestMatchResult(desired);
            assertEquals("result.getSupportedULocale from ULocales",
                    expMatch, result.getSupportedULocale());
            assertEquals("result.getSupportedLocale from ULocales",
                    toLocale(expMatch), result.getSupportedLocale());
            if (!test.expDesired.isEmpty()) {
                ULocale expDesired = getULocaleOrNull(test.expDesired);
                assertEquals("result.getDesiredULocale from ULocales",
                        expDesired, result.getDesiredULocale());
                assertEquals("result.getDesiredLocale from ULocales",
                        toLocale(expDesired), result.getDesiredLocale());
            }
            if (!test.expCombined.isEmpty()) {
                ULocale expCombined = getULocaleOrNull(test.expCombined);
                assertEquals("combined ULocale from ULocales", expCombined, result.makeResolvedULocale());
                assertEquals("combined Locale from ULocales", toLocale(expCombined), result.makeResolvedLocale());
            }

            result = matcher.getBestLocaleResult(localesFromULocales(desired.getULocales()));
            assertEquals("result.getSupportedULocale from Locales",
                    expMatch, result.getSupportedULocale());
            assertEquals("result.getSupportedLocale from Locales",
                    toLocale(expMatch), result.getSupportedLocale());
            if (!test.expDesired.isEmpty()) {
                ULocale expDesired = getULocaleOrNull(test.expDesired);
                assertEquals("result.getDesiredULocale from Locales",
                        expDesired, result.getDesiredULocale());
                assertEquals("result.getDesiredLocale from Locales",
                        toLocale(expDesired), result.getDesiredLocale());
            }
            if (!test.expCombined.isEmpty()) {
                ULocale expCombined = getULocaleOrNull(test.expCombined);
                assertEquals("combined ULocale from Locales", expCombined, result.makeResolvedULocale());
                assertEquals("combined Locale from Locales", toLocale(expCombined), result.makeResolvedLocale());
            }
        }
    }
}
