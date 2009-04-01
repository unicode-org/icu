/*
 ********************************************************************************
 * Copyright (C) 2007-2009, Google, International Business Machines Corporation *
 * and others. All Rights Reserved.                                             *
 ********************************************************************************
 */

package com.ibm.icu.dev.test.format;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;

public class TimeZoneFormatTest extends com.ibm.icu.dev.test.TestFmwk {

    public static void main(String[] args) throws Exception {
        new TimeZoneFormatTest().run(args);
    }

    private static final String[] PATTERNS = {"z", "zzzz", "Z", "ZZZZ", "v", "vvvv", "V", "VVVV"};

    /*
     * Test case for checking if a TimeZone is properly set in the result calendar
     * and if the result TimeZone has the expected behavior.
     */
    public void TestTimeZoneRoundTrip() {
        TimeZone unknownZone = new SimpleTimeZone(-31415, "Etc/Unknown");
        int badDstOffset = -1234;
        int badZoneOffset = -2345;

        int[][] testDateData = {
            {2007, 1, 15},
            {2007, 6, 15},
            {1990, 1, 15},
            {1990, 6, 15},
            {1960, 1, 15},
            {1960, 6, 15},
        };

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();

        // Set up rule equivalency test range
        long low, high;
        cal.set(1900, 0, 1);
        low = cal.getTimeInMillis();
        cal.set(2040, 0, 1);
        high = cal.getTimeInMillis();

        // Set up test dates
        Date[] DATES = new Date[testDateData.length];
        cal.clear();
        for (int i = 0; i < DATES.length; i++) {
            cal.set(testDateData[i][0], testDateData[i][1], testDateData[i][2]);
            DATES[i] = cal.getTime();
        }

        // Set up test locales
        ULocale[] LOCALES = null;
        if (getInclusion() > 5) {
            LOCALES = ULocale.getAvailableLocales();
        } else {
            LOCALES = new ULocale[] {new ULocale("en"), new ULocale("en_CA"), new ULocale("fr"), new ULocale("zh_Hant")};
        }

        String[] tzids = TimeZone.getAvailableIDs();
        int[] inOffsets = new int[2];
        int[] outOffsets = new int[2];

        // Run the roundtrip test
        for (int locidx = 0; locidx < LOCALES.length; locidx++) {
            for (int patidx = 0; patidx < PATTERNS.length; patidx++) {
                SimpleDateFormat sdf = new SimpleDateFormat(PATTERNS[patidx], LOCALES[locidx]);

                for (int tzidx = 0; tzidx < tzids.length; tzidx++) {
                    TimeZone tz = TimeZone.getTimeZone(tzids[tzidx]);

                    for (int datidx = 0; datidx < DATES.length; datidx++) {
                        // Format
                        sdf.setTimeZone(tz);
                        String tzstr = sdf.format(DATES[datidx]);

                        // Before parse, set unknown zone to SimpleDateFormat instance
                        // just for making sure that it does not depends on the time zone
                        // originally set.
                        sdf.setTimeZone(unknownZone);

                        // Parse
                        ParsePosition pos = new ParsePosition(0);
                        Calendar outcal = Calendar.getInstance(unknownZone);
                        outcal.set(Calendar.DST_OFFSET, badDstOffset);
                        outcal.set(Calendar.ZONE_OFFSET, badZoneOffset);

                        sdf.parse(tzstr, outcal, pos);

                        // Check the result
                        TimeZone outtz = outcal.getTimeZone();

                        tz.getOffset(DATES[datidx].getTime(), false, inOffsets);
                        outtz.getOffset(DATES[datidx].getTime(), false, outOffsets);

                        if (PATTERNS[patidx].equals("VVVV")) {
                            // Location: time zone rule must be preserved except
                            // zones not actually associated with a specific location.
                            // Time zones in this category do not have "/" in its ID.
                            String canonicalID = TimeZone.getCanonicalID(tzids[tzidx]);
                            if (canonicalID != null && !outtz.getID().equals(canonicalID)) {
                                // Canonical ID did not match - check the rules
                                boolean bFailure = false;
                                if ((tz instanceof BasicTimeZone) && (outtz instanceof BasicTimeZone)) {
                                    bFailure = !(canonicalID.indexOf('/') == -1)
                                                && !((BasicTimeZone)outtz).hasEquivalentTransitions(tz, low, high);
                                }
                                if (bFailure) {
                                    errln("Canonical round trip failed; tz=" + tzids[tzidx]
                                            + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                            + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                            + ", outtz=" + outtz.getID());
                                } else {
                                    logln("Canonical round trip failed (as expected); tz=" + tzids[tzidx]
                                            + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                            + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                            + ", outtz=" + outtz.getID());
                                }
                            }
                        } else {
                            // Check if localized GMT format or RFC format is used.
                            int numDigits = 0;
                            for (int n = 0; n < tzstr.length(); n++) {
                                if (UCharacter.isDigit(tzstr.charAt(n))) {
                                    numDigits++;
                                }
                            }

                            if (numDigits >= 3) {
                                // Localized GMT or RFC: total offset (raw + dst) must be preserved.
                                int inOffset = inOffsets[0] + inOffsets[1];
                                int outOffset = outOffsets[0] + outOffsets[1];
                                if (inOffset != outOffset) {
                                    errln("Offset round trip failed; tz=" + tzids[tzidx]
                                        + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                        + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                        + ", inOffset=" + inOffset + ", outOffset=" + outOffset);
                                }
                            } else {
                                // Specific or generic: raw offset must be preserved.
                                if (inOffsets[0] != outOffsets[0]) {
                                    errln("Raw offset round trip failed; tz=" + tzids[tzidx]
                                        + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                        + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                        + ", inRawOffset=" + inOffsets[0] + ", outRawOffset=" + outOffsets[0]);
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    /*
     * Test case of round trip time and text.  This test case detects every canonical TimeZone's
     * rule transition since 1900 until 2020, then check if time around each transition can
     * round trip as expected.
     */
    public void TestTimeRoundTrip() {

        boolean TEST_ALL = "true".equalsIgnoreCase(getProperty("TimeZoneRoundTripAll"));

        int startYear, endYear;

        if (TEST_ALL || getInclusion() > 5) {
            startYear = 1900;
        } else {
            startYear = 1990;
        }

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        endYear = cal.get(Calendar.YEAR) + 3;

        cal.set(startYear, Calendar.JANUARY, 1);
        final long START_TIME = cal.getTimeInMillis();

        cal.set(endYear, Calendar.JANUARY, 1);
        final long END_TIME = cal.getTimeInMillis();

        // Whether each pattern is ambiguous at DST->STD local time overlap
        final boolean[] AMBIGUOUS_DST_DECESSION = {false, false, false, false, true, true, false, true};
        // Whether each pattern is ambiguous at STD->STD/DST->DST local time overlap
        final boolean[] AMBIGUOUS_NEGATIVE_SHIFT = {true, true, false, false, true, true, true, true};

        final String BASEPATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

        ULocale[] LOCALES = null;
        boolean REALLY_VERBOSE = false;

        // timer for performance analysis
        long[] times = new long[PATTERNS.length];
        long timer;

        if (TEST_ALL) {
            // It may take about an hour for testing all locales
            LOCALES = ULocale.getAvailableLocales();
        } else if (getInclusion() > 5) {
            LOCALES = new ULocale[] {
                new ULocale("ar_EG"), new ULocale("bg_BG"), new ULocale("ca_ES"), new ULocale("da_DK"), new ULocale("de"),
                new ULocale("de_DE"), new ULocale("el_GR"), new ULocale("en"), new ULocale("en_AU"), new ULocale("en_CA"),
                new ULocale("en_US"), new ULocale("es"), new ULocale("es_ES"), new ULocale("es_MX"), new ULocale("fi_FI"),
                new ULocale("fr"), new ULocale("fr_CA"), new ULocale("fr_FR"), new ULocale("he_IL"), new ULocale("hu_HU"),
                new ULocale("it"), new ULocale("it_IT"), new ULocale("ja"), new ULocale("ja_JP"), new ULocale("ko"),
                new ULocale("ko_KR"), new ULocale("nb_NO"), new ULocale("nl_NL"), new ULocale("nn_NO"), new ULocale("pl_PL"),
                new ULocale("pt"), new ULocale("pt_BR"), new ULocale("pt_PT"), new ULocale("ru_RU"), new ULocale("sv_SE"),
                new ULocale("th_TH"), new ULocale("tr_TR"), new ULocale("zh"), new ULocale("zh_Hans"), new ULocale("zh_Hans_CN"),
                new ULocale("zh_Hant"), new ULocale("zh_Hant_HK"), new ULocale("zh_Hant_TW")
            };
        } else {
            LOCALES = new ULocale[] {
                new ULocale("en"),
            };
        }

        SimpleDateFormat sdfGMT = new SimpleDateFormat(BASEPATTERN);
        sdfGMT.setTimeZone(TimeZone.getTimeZone("Etc/GMT"));

        long testCounts = 0;
        long[] testTimes = new long[4];
        boolean[] expectedRoundTrip = new boolean[4];
        int testLen = 0;
        for (int locidx = 0; locidx < LOCALES.length; locidx++) {
            logln("Locale: " + LOCALES[locidx].toString());
            for (int patidx = 0; patidx < PATTERNS.length; patidx++) {
                logln("    pattern: " + PATTERNS[patidx]);
                String pattern = BASEPATTERN + " " + PATTERNS[patidx];
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, LOCALES[locidx]);

                String[] ids = TimeZone.getAvailableIDs();
                for (int zidx = 0; zidx < ids.length; zidx++) {
                    String id = TimeZone.getCanonicalID(ids[zidx]);
                    if (id == null || !id.equals(ids[zidx])) {
                        // Skip aliases
                        continue;
                    }
                    BasicTimeZone btz = (BasicTimeZone)TimeZone.getTimeZone(ids[zidx], TimeZone.TIMEZONE_ICU);
                    TimeZone tz = TimeZone.getTimeZone(ids[zidx]);
                    sdf.setTimeZone(tz);

                    long t = START_TIME;
                    TimeZoneTransition tzt = null;
                    boolean middle = true;
                    while (t < END_TIME) {
                        if (tzt == null) {
                            testTimes[0] = t;
                            expectedRoundTrip[0] = true;
                            testLen = 1;
                        } else {
                            int fromOffset = tzt.getFrom().getRawOffset() + tzt.getFrom().getDSTSavings();
                            int toOffset = tzt.getTo().getRawOffset() + tzt.getTo().getDSTSavings();
                            int delta = toOffset - fromOffset;
                            if (delta < 0) {
                                boolean isDstDecession = tzt.getFrom().getDSTSavings() > 0 && tzt.getTo().getDSTSavings() == 0;
                                testTimes[0] = t + delta - 1;
                                expectedRoundTrip[0] = true;
                                testTimes[1] = t + delta;
                                expectedRoundTrip[1] = isDstDecession ?
                                        !AMBIGUOUS_DST_DECESSION[patidx] : !AMBIGUOUS_NEGATIVE_SHIFT[patidx];
                                testTimes[2] = t - 1;
                                expectedRoundTrip[2] = isDstDecession ?
                                        !AMBIGUOUS_DST_DECESSION[patidx] : !AMBIGUOUS_NEGATIVE_SHIFT[patidx];
                                testTimes[3] = t;
                                expectedRoundTrip[3] = true;
                                testLen = 4;
                            } else {
                                testTimes[0] = t - 1;
                                expectedRoundTrip[0] = true;
                                testTimes[1] = t;
                                expectedRoundTrip[1] = true;
                                testLen = 2;
                            }
                        }
                        for (int testidx = 0; testidx < testLen; testidx++) {
                            testCounts++;
                            timer = System.currentTimeMillis();
                            String text = sdf.format(new Date(testTimes[testidx]));
                            try {
                                Date parsedDate = sdf.parse(text);
                                long restime = parsedDate.getTime();
                                if (restime != testTimes[testidx]) {
                                    StringBuffer msg = new StringBuffer();
                                    msg.append("Time round trip failed for ")
                                        .append("tzid=").append(ids[zidx])
                                        .append(", locale=").append(LOCALES[locidx])
                                        .append(", pattern=").append(PATTERNS[patidx])
                                        .append(", text=").append(text)
                                        .append(", gmt=").append(sdfGMT.format(new Date(testTimes[testidx])))
                                        .append(", time=").append(testTimes[testidx])
                                        .append(", restime=").append(restime)
                                        .append(", diff=").append(restime - testTimes[testidx]);
                                    if (expectedRoundTrip[testidx]) {
                                        errln("FAIL: " + msg.toString());
                                    } else if (REALLY_VERBOSE) {
                                        logln(msg.toString());
                                    }
                                }
                            } catch (ParseException pe) {
                                errln("FAIL: " + pe.getMessage());
                            }
                            times[patidx] += System.currentTimeMillis() - timer;
                        }
                        tzt = btz.getNextTransition(t, false);
                        if (tzt == null) {
                            break;
                        }
                        if (middle) {
                            // Test the date in the middle of two transitions.
                            t += (tzt.getTime() - t)/2;
                            middle = false;
                            tzt = null;
                        } else {
                            t = tzt.getTime();
                        }
                    }
                }
            }
        }

        long total = 0;
        logln("### Elapsed time by patterns ###");
        for (int i = 0; i < PATTERNS.length; i++) {
            logln(times[i] + "ms (" + PATTERNS[i] + ")");
            total += times[i];
        }
        logln("Total: " + total + "ms");
        logln("Iteration: " + testCounts);
    }
}