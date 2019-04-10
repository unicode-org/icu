/*
 ********************************************************************************
 * Copyright (C) 2007-2013, Google, International Business Machines Corporation *
 * and others. All Rights Reserved.                                             *
 ********************************************************************************
 */

package com.ibm.icu.dev.test.format;

import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import com.ibm.icu.impl.ZoneMeta;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.TimeZoneFormat;
import com.ibm.icu.text.TimeZoneFormat.ParseOption;
import com.ibm.icu.text.TimeZoneFormat.Style;
import com.ibm.icu.text.TimeZoneFormat.TimeType;
import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Output;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZone.SystemTimeZoneType;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;

public class TimeZoneFormatTest extends com.ibm.icu.dev.test.TestFmwk {

	private static boolean JDKTZ = (TimeZone.getDefaultTimeZoneType() == TimeZone.TIMEZONE_JDK);
    private static final Pattern EXCL_TZ_PATTERN = Pattern.compile(".*/Riyadh8[7-9]");

    public static void main(String[] args) throws Exception {
        new TimeZoneFormatTest().run(args);
    }

    private static final String[] PATTERNS = {
        "z",
        "zzzz",
        "Z",        // equivalent to "xxxx"
        "ZZZZ",     // equivalent to "OOOO"
        "v",
        "vvvv",
        "O",
        "OOOO",
        "X",
        "XX",
        "XXX",
        "XXXX",
        "XXXXX",
        "x",
        "xx",
        "xxx",
        "xxxx",
        "xxxxx",
        "V",
        "VV",
        "VVV",
        "VVVV"
    };
    boolean REALLY_VERBOSE_LOG = false;

    /*
     * Test case for checking if a TimeZone is properly set in the result calendar
     * and if the result TimeZone has the expected behavior.
     */
    public void TestTimeZoneRoundTrip() {
        boolean TEST_ALL = "true".equalsIgnoreCase(getProperty("TimeZoneRoundTripAll"));

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
        if (TEST_ALL || getInclusion() > 5) {
            LOCALES = ULocale.getAvailableLocales();
        } else {
            LOCALES = new ULocale[] {new ULocale("en"), new ULocale("en_CA"), new ULocale("fr"), new ULocale("zh_Hant")};
        }

        String[] tzids;
        if (JDKTZ) {
            tzids = java.util.TimeZone.getAvailableIDs();
        } else {
            tzids = TimeZone.getAvailableIDs();
        }
        int[] inOffsets = new int[2];
        int[] outOffsets = new int[2];

        // Run the roundtrip test
        for (int locidx = 0; locidx < LOCALES.length; locidx++) {
            logln("Locale: " + LOCALES[locidx].toString());

            String localGMTString = TimeZoneFormat.getInstance(LOCALES[locidx]).formatOffsetLocalizedGMT(0);

            for (int patidx = 0; patidx < PATTERNS.length; patidx++) {
                logln("    pattern: " + PATTERNS[patidx]);
                SimpleDateFormat sdf = new SimpleDateFormat(PATTERNS[patidx], LOCALES[locidx]);

                for (int tzidx = 0; tzidx < tzids.length; tzidx++) {
                    if (EXCL_TZ_PATTERN.matcher(tzids[tzidx]).matches()) {
                        continue;
                    }
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

                        if (PATTERNS[patidx].equals("V")) {
                            // Short zone ID - should support roundtrip for canonical CLDR IDs
                            String canonicalID = TimeZone.getCanonicalID(tzids[tzidx]);
                            if (!outtz.getID().equals(canonicalID)) {
                                if (outtz.getID().equals("Etc/Unknown")) {
                                    // Note that some zones like Asia/Riyadh87 does not have
                                    // short zone ID and "unk" is used as the fallback
                                    if (REALLY_VERBOSE_LOG) {
                                        logln("Canonical round trip failed (probably as expected); tz=" + tzids[tzidx]
                                            + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                            + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                            + ", outtz=" + outtz.getID());
                                    }
                                } else {
                                    errln("Canonical round trip failed; tz=" + tzids[tzidx]
                                        + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                        + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                        + ", outtz=" + outtz.getID());
                                }
                            }
                        } else if (PATTERNS[patidx].equals("VV")) {
                            // Zone ID - full roundtrip support
                            if (!outtz.getID().equals(tzids[tzidx])) {
                                errln("Zone ID round trip failed; tz=" + tzids[tzidx]
                                        + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                        + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                        + ", outtz=" + outtz.getID());
                            }
                        } else if (PATTERNS[patidx].equals("VVV") || PATTERNS[patidx].equals("VVVV")) {
                            // Location: time zone rule must be preserved except
                            // zones not actually associated with a specific location.
                            String canonicalID = TimeZone.getCanonicalID(tzids[tzidx]);
                            if (canonicalID != null && !outtz.getID().equals(canonicalID)) {
                                // Canonical ID did not match - check the rules
                                boolean bFailure = false;
                                if ((tz instanceof BasicTimeZone) && (outtz instanceof BasicTimeZone)) {
                                    boolean hasNoLocation = TimeZone.getRegion(tzids[tzidx]).equals("001");
                                    bFailure = !hasNoLocation
                                                && !((BasicTimeZone)outtz).hasEquivalentTransitions(tz, low, high);
                                }
                                if (bFailure) {
                                    errln("Canonical round trip failed; tz=" + tzids[tzidx]
                                            + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                            + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                            + ", outtz=" + outtz.getID());
                                } else if (REALLY_VERBOSE_LOG) {
                                    logln("Canonical round trip failed (as expected); tz=" + tzids[tzidx]
                                            + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                            + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                            + ", outtz=" + outtz.getID());
                                }
                            }
                        } else {
                            boolean isOffsetFormat = (PATTERNS[patidx].charAt(0) == 'Z'
                                    || PATTERNS[patidx].charAt(0) == 'O'
                                    || PATTERNS[patidx].charAt(0) == 'X'
                                    || PATTERNS[patidx].charAt(0) == 'x');
                            boolean minutesOffset = false;
                            if (PATTERNS[patidx].charAt(0) == 'X' || PATTERNS[patidx].charAt(0) == 'x') {
                                minutesOffset = PATTERNS[patidx].length() <= 3;
                            }

                            if (!isOffsetFormat) {
                                // Check if localized GMT format is used as a fallback of name styles
                                int numDigits = 0;
                                for (int n = 0; n < tzstr.length(); n++) {
                                    if (UCharacter.isDigit(tzstr.charAt(n))) {
                                        numDigits++;
                                    }
                                }
                                isOffsetFormat = (numDigits > 0);
                            }

                            if (isOffsetFormat || tzstr.equals(localGMTString)) {
                                // Localized GMT or ISO: total offset (raw + dst) must be preserved.
                                int inOffset = inOffsets[0] + inOffsets[1];
                                int outOffset = outOffsets[0] + outOffsets[1];
                                int diff = outOffset - inOffset;
                                if (minutesOffset) {
                                    diff = (diff / 60000) * 60000;
                                }
                                if (diff != 0) {
                                    errln("Offset round trip failed; tz=" + tzids[tzidx]
                                        + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                        + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                        + ", inOffset=" + inOffset + ", outOffset=" + outOffset);
                                }
                            } else {
                                // Specific or generic: raw offset must be preserved.
                                if (inOffsets[0] != outOffsets[0]) {
                                    if (JDKTZ && tzids[tzidx].startsWith("SystemV/")) {
                                        // JDK uses rule SystemV for these zones while
                                        // ICU handles these zones as aliases of existing time zones
                                        if (REALLY_VERBOSE_LOG) {
                                            logln("Raw offset round trip failed; tz=" + tzids[tzidx]
                                                + ", locale=" + LOCALES[locidx] + ", pattern=" + PATTERNS[patidx]
                                                + ", time=" + DATES[datidx].getTime() + ", str=" + tzstr
                                                + ", inRawOffset=" + inOffsets[0] + ", outRawOffset=" + outOffsets[0]);
                                        }

                                    } else {
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

        // These patterns are ambiguous at DST->STD local time overlap
        List<String> AMBIGUOUS_DST_DECESSION = Arrays.asList("v", "vvvv", "V", "VV", "VVV", "VVVV");

        // These patterns are ambiguous at STD->STD/DST->DST local time overlap
        List<String> AMBIGUOUS_NEGATIVE_SHIFT = Arrays.asList("z", "zzzz", "v", "vvvv", "V", "VV", "VVV", "VVVV");

        // These patterns only support integer minutes offset
        List<String> MINUTES_OFFSET = Arrays.asList("X", "XX", "XXX", "x", "xx", "xxx");

        // Regex pattern used for filtering zone IDs without exemplar location
        final Pattern LOC_EXCLUSION_PATTERN = Pattern.compile("Etc/.*|SystemV/.*|.*/Riyadh8[7-9]");

        final String BASEPATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS";

        ULocale[] LOCALES = null;

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
                boolean minutesOffset = MINUTES_OFFSET.contains(PATTERNS[patidx]);

                Set<String> ids = null;
                if (JDKTZ) {
                	ids = new TreeSet<String>();
                	String[] jdkIDs = java.util.TimeZone.getAvailableIDs();
                	for (String jdkID : jdkIDs) {
                        if (EXCL_TZ_PATTERN.matcher(jdkID).matches()) {
                            continue;
                        }
                        String tmpID = TimeZone.getCanonicalID(jdkID);
                		if (tmpID != null) {
                			ids.add(tmpID);
                		}
                	}
                } else {
                	ids = TimeZone.getAvailableIDs(SystemTimeZoneType.CANONICAL, null, null);                	
                }

                for (String id : ids) {
                    if (PATTERNS[patidx].equals("V")) {
                        // Some zones do not have short ID assigned, such as Asia/Riyadh87.
                        // The time roundtrip will fail for such zones with pattern "V" (short zone ID).
                        // This is expected behavior.
                        String shortZoneID = ZoneMeta.getShortID(id);
                        if (shortZoneID == null) {
                            continue;
                        }
                    } else if (PATTERNS[patidx].equals("VVV")) {
                        // Some zones are not associated with any region, such as Etc/GMT+8.
                        // The time roundtrip will fail for such zones with pattern "VVV" (exemplar location).
                        // This is expected behavior.
                        if (id.indexOf('/') < 0 || LOC_EXCLUSION_PATTERN.matcher(id).matches()) {
                            continue;
                        }
                    }

                    if (id.equals("Pacific/Apia") && PATTERNS[patidx].equals("vvvv")) {
                        // known issue #11052 - Ambiguous zone name - Samoa Time
                        continue;
                    }

                    BasicTimeZone btz = (BasicTimeZone)TimeZone.getTimeZone(id, TimeZone.TIMEZONE_ICU);
                    TimeZone tz = TimeZone.getTimeZone(id);
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
                                        !AMBIGUOUS_DST_DECESSION.contains(PATTERNS[patidx]) :
                                        !AMBIGUOUS_NEGATIVE_SHIFT.contains(PATTERNS[patidx]);
                                testTimes[2] = t - 1;
                                expectedRoundTrip[2] = isDstDecession ?
                                        !AMBIGUOUS_DST_DECESSION.contains(PATTERNS[patidx]) :
                                        !AMBIGUOUS_NEGATIVE_SHIFT.contains(PATTERNS[patidx]);
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
                                long timeDiff = restime - testTimes[testidx];
                                boolean bTimeMatch = minutesOffset ?
                                        (timeDiff/60000)*60000 == 0 : timeDiff == 0;
                                if (!bTimeMatch) {
                                    StringBuffer msg = new StringBuffer();
                                    msg.append("Time round trip failed for ")
                                        .append("tzid=").append(id)
                                        .append(", locale=").append(LOCALES[locidx])
                                        .append(", pattern=").append(PATTERNS[patidx])
                                        .append(", text=").append(text)
                                        .append(", gmt=").append(sdfGMT.format(new Date(testTimes[testidx])))
                                        .append(", time=").append(testTimes[testidx])
                                        .append(", restime=").append(restime)
                                        .append(", diff=").append(timeDiff);
                                    if (expectedRoundTrip[testidx]
                                        && !isSpecialTimeRoundTripCase(LOCALES[locidx], id, PATTERNS[patidx], testTimes[testidx])) {
                                        errln("FAIL: " + msg.toString());
                                    } else if (REALLY_VERBOSE_LOG) {
                                        logln(msg.toString());
                                    }
                                }
                            } catch (ParseException pe) {
                                errln("FAIL: " + pe.getMessage() + " tzid=" + id + ", locale=" + LOCALES[locidx] +
                                        ", pattern=" + PATTERNS[patidx] + ", text=" + text);
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

    // Special exclusions in TestTimeZoneRoundTrip.
    // These special cases do not round trip time as designed.
    private boolean isSpecialTimeRoundTripCase(ULocale loc, String id, String pattern, long time) {
        final Object[][] EXCLUSIONS = {
            {null, "Asia/Chita", "zzzz", Long.valueOf(1414252800000L)},
            {null, "Asia/Chita", "vvvv", Long.valueOf(1414252800000L)},
            {null, "Asia/Srednekolymsk", "zzzz", Long.valueOf(1414241999999L)},
            {null, "Asia/Srednekolymsk", "vvvv", Long.valueOf(1414241999999L)},
        };
        boolean isExcluded = false;
        for (Object[] excl : EXCLUSIONS) {
            if (excl[0] == null || loc.equals((ULocale)excl[0])) {
                if (id.equals(excl[1])) {
                    if (excl[2] == null || pattern.equals((String)excl[2])) {
                        if (excl[3] == null || ((Long)excl[3]).compareTo(time) == 0) {
                            isExcluded = true;
                            break;
                        }
                    }
                }
            }
        }
        return isExcluded;
    }

    public void TestParse() {
        final Object[][] DATA = {
        //   text                   inpos       locale      style                       parseAll?   expected            outpos      time type
            {"Z",                   0,          "en_US",    Style.ISO_EXTENDED_FULL,    false,      "Etc/GMT",          1,          TimeType.UNKNOWN},
            {"Z",                   0,          "en_US",    Style.SPECIFIC_LONG,        false,      "Etc/GMT",          1,          TimeType.UNKNOWN},
            {"Zambia time",         0,          "en_US",    Style.ISO_EXTENDED_FULL,    true,       "Etc/GMT",          1,          TimeType.UNKNOWN},
            {"Zambia time",         0,          "en_US",    Style.GENERIC_LOCATION,     false,      "Africa/Lusaka",    11,         TimeType.UNKNOWN},
            {"Zambia time",         0,          "en_US",    Style.ISO_BASIC_LOCAL_FULL, true,       "Africa/Lusaka",    11,         TimeType.UNKNOWN},
            {"+00:00",              0,          "en_US",    Style.ISO_EXTENDED_FULL,    false,      "Etc/GMT",          6,          TimeType.UNKNOWN},
            {"-01:30:45",           0,          "en_US",    Style.ISO_EXTENDED_FULL,    false,      "GMT-01:30:45",     9,          TimeType.UNKNOWN},
            {"-7",                  0,          "en_US",    Style.ISO_BASIC_LOCAL_FULL, false,      "GMT-07:00",        2,          TimeType.UNKNOWN},
            {"-2222",               0,          "en_US",    Style.ISO_BASIC_LOCAL_FULL, false,      "GMT-22:22",        5,          TimeType.UNKNOWN},
            {"-3333",               0,          "en_US",    Style.ISO_BASIC_LOCAL_FULL, false,      "GMT-03:33",        4,          TimeType.UNKNOWN},
            {"XXX+01:30YYY",        3,          "en_US",    Style.LOCALIZED_GMT,        false,      "GMT+01:30",        9,          TimeType.UNKNOWN},
            {"GMT0",                0,          "en_US",    Style.SPECIFIC_SHORT,       false,      "Etc/GMT",          3,          TimeType.UNKNOWN},
            {"EST",                 0,          "en_US",    Style.SPECIFIC_SHORT,       false,      "America/New_York", 3,          TimeType.STANDARD},
            {"ESTx",                0,          "en_US",    Style.SPECIFIC_SHORT,       false,      "America/New_York", 3,          TimeType.STANDARD},
            {"EDTx",                0,          "en_US",    Style.SPECIFIC_SHORT,       false,      "America/New_York", 3,          TimeType.DAYLIGHT},
            {"EST",                 0,          "en_US",    Style.SPECIFIC_LONG,        false,      null,                 0,          TimeType.UNKNOWN},
            {"EST",                 0,          "en_US",    Style.SPECIFIC_LONG,        true,       "America/New_York", 3,          TimeType.STANDARD},
            {"EST",                 0,          "en_CA",    Style.SPECIFIC_SHORT,       false,      "America/Toronto",  3,          TimeType.STANDARD},
        };

        for (Object[] test : DATA) {
            String text = (String)test[0];
            int inPos = (Integer)test[1];
            ULocale loc = new ULocale((String)test[2]);
            Style style = (Style)test[3];
            EnumSet<ParseOption> options = (Boolean)test[4] ? EnumSet.of(ParseOption.ALL_STYLES) : null;
            String expID = (String)test[5];
            int expPos = (Integer)test[6];
            TimeType expType = (TimeType)test[7];

            TimeZoneFormat tzfmt = TimeZoneFormat.getInstance(loc);
            Output<TimeType> timeType = new Output<TimeType>(TimeType.UNKNOWN);
            ParsePosition pos = new ParsePosition(inPos);
            TimeZone tz = tzfmt.parse(style, text, pos, options, timeType);

            String errMsg = null;
            if (tz == null) {
                if (expID != null) {
                    errMsg = "Parse failure - expected: " + expID;
                }
            } else if (!tz.getID().equals(expID)) {
                errMsg = "Time zone ID: " + tz.getID() + " - expected: " + expID;
            } else if (pos.getIndex() != expPos) {
                errMsg = "Parsed pos: " + pos.getIndex() + " - expected: " + expPos;
            } else if (timeType.value != expType) {
                errMsg = "Time type: " + timeType + " - expected: " + expType;
            }

            if (errMsg != null) {
                errln("Fail: " + errMsg + " [text=" + text + ", pos=" + inPos + ", style=" + style + "]");
            }
        }
    }

    public void TestISOFormat() {
        final int[] OFFSET = {
            0,          // 0
            999,        // 0.999s
            -59999,     // -59.999s
            60000,      // 1m
            -77777,     // -1m 17.777s
            1800000,    // 30m
            -3600000,   // -1h
            36000000,   // 10h
            -37800000,  // -10h 30m
            -37845000,  // -10h 30m 45s
            108000000,  // 30h
        };
 
        final String[][] ISO_STR = {
            // 0
            {
                "Z", "Z", "Z", "Z", "Z",
                "+00", "+0000", "+00:00", "+0000", "+00:00",
                "+0000"
            },
            // 999
            {
                "Z", "Z", "Z", "Z", "Z",
                "+00", "+0000", "+00:00", "+0000", "+00:00",
                "+0000"
            },
            // -59999
            {
                "Z", "Z", "Z", "-000059", "-00:00:59",
                "+00", "+0000", "+00:00", "-000059", "-00:00:59",
                "-000059"
            },
            // 60000
            {
                "+0001", "+0001", "+00:01", "+0001", "+00:01",
                "+0001", "+0001", "+00:01", "+0001", "+00:01",
                "+0001"
            },
            // -77777
            {
                "-0001", "-0001", "-00:01", "-000117", "-00:01:17",
                "-0001", "-0001", "-00:01", "-000117", "-00:01:17",
                "-000117"
            },
            // 1800000
            {
                "+0030", "+0030", "+00:30", "+0030", "+00:30",
                "+0030", "+0030", "+00:30", "+0030", "+00:30",
                "+0030"
            },
            // -3600000
            {
                "-01", "-0100", "-01:00", "-0100", "-01:00",
                "-01", "-0100", "-01:00", "-0100", "-01:00",
                "-0100"
            },
            // 36000000
            {
                "+10", "+1000", "+10:00", "+1000", "+10:00",
                "+10", "+1000", "+10:00", "+1000", "+10:00",
                "+1000"
            },
            // -37800000
            {
                "-1030", "-1030", "-10:30", "-1030", "-10:30",
                "-1030", "-1030", "-10:30", "-1030", "-10:30",
                "-1030"
            },
            // -37845000
            {
                "-1030", "-1030", "-10:30", "-103045", "-10:30:45",
                "-1030", "-1030", "-10:30", "-103045", "-10:30:45",
                "-103045"
            },
            // 108000000
            {
                null, null, null, null, null,
                null, null, null, null, null,
                null
            }
        };

        final String[] PATTERN = {
            "X", "XX", "XXX", "XXXX", "XXXXX", "x", "xx", "xxx", "xxxx", "xxxxx",
            "Z", // equivalent to "xxxx"
        };

        final int[] MIN_OFFSET_UNIT = {
            60000, 60000, 60000, 1000, 1000, 60000, 60000, 60000, 1000, 1000,
            1000,
        };

        // Formatting
        SimpleDateFormat sdf = new SimpleDateFormat();
        Date d = new Date();

        for (int i = 0; i < OFFSET.length; i++) {
            SimpleTimeZone tz = new SimpleTimeZone(OFFSET[i], "Zone Offset:" + String.valueOf(OFFSET[i]) + "ms");
            sdf.setTimeZone(tz);
            for (int j = 0; j < PATTERN.length; j++) {
                sdf.applyPattern(PATTERN[j]);
                try {
                    String result = sdf.format(d);
                    if (!result.equals(ISO_STR[i][j])) {
                        errln("FAIL: pattern=" + PATTERN[j] + ", offset=" + OFFSET[i] + " -> "
                            + result + " (expected: " + ISO_STR[i][j] + ")");
                    }
                } catch (IllegalArgumentException e) {
                    if (ISO_STR[i][j] != null) {
                        errln("FAIL: IAE thrown for pattern=" + PATTERN[j] + ", offset=" + OFFSET[i]
                                + " (expected: " + ISO_STR[i][j] + ")");
                    }
                }
            }
        }

        // Parsing
        SimpleTimeZone bogusTZ = new SimpleTimeZone(-1, "Zone Offset: -1ms");
        for (int i = 0; i < ISO_STR.length; i++) {
            for (int j = 0; j < ISO_STR[i].length; j++) {
                if (ISO_STR[i][j] == null) {
                    continue;
                }
                ParsePosition pos = new ParsePosition(0);
                Calendar outcal = Calendar.getInstance(bogusTZ);
                sdf.applyPattern(PATTERN[j]);

                sdf.parse(ISO_STR[i][j], outcal, pos);

                if (pos.getIndex() != ISO_STR[i][j].length()) {
                    errln("FAIL: Failed to parse the entire input string: " + ISO_STR[i][j]);
                    continue;
                }

                TimeZone outtz = outcal.getTimeZone();
                int outOffset = outtz.getRawOffset();
                int adjustedOffset = OFFSET[i] / MIN_OFFSET_UNIT[j] * MIN_OFFSET_UNIT[j];

                if (outOffset != adjustedOffset) {
                    errln("FAIL: Incorrect offset:" + outOffset + "ms for input string: " + ISO_STR[i][j]
                            + " (expected:" + adjustedOffset + "ms)");
                }
            }
        }
    }
}