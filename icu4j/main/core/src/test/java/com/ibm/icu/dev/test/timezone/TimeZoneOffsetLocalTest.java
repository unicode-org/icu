// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2007-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.timezone;

import java.util.Date;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.AnnualTimeZoneRule;
import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.BasicTimeZone.LocalOption;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateTimeRule;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.InitialTimeZoneRule;
import com.ibm.icu.util.RuleBasedTimeZone;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;

/**
 * Testing getOffset APIs using local time
 */
@RunWith(JUnit4.class)
public class TimeZoneOffsetLocalTest extends CoreTestFmwk {
    /*
     * Testing getOffset APIs around rule transition by local standard/wall time.
     */
    @Test
    public void TestGetOffsetAroundTransition() {
        final int HOUR = 60*60*1000;
        final int MINUTE = 60*1000;

        int[][] DATES = {
            {2006, Calendar.APRIL, 2, 1, 30, 1*HOUR+30*MINUTE},
            {2006, Calendar.APRIL, 2, 2, 00, 2*HOUR},
            {2006, Calendar.APRIL, 2, 2, 30, 2*HOUR+30*MINUTE},
            {2006, Calendar.APRIL, 2, 3, 00, 3*HOUR},
            {2006, Calendar.APRIL, 2, 3, 30, 3*HOUR+30*MINUTE},
            {2006, Calendar.OCTOBER, 29, 0, 30, 0*HOUR+30*MINUTE},
            {2006, Calendar.OCTOBER, 29, 1, 00, 1*HOUR},
            {2006, Calendar.OCTOBER, 29, 1, 30, 1*HOUR+30*MINUTE},
            {2006, Calendar.OCTOBER, 29, 2, 00, 2*HOUR},
            {2006, Calendar.OCTOBER, 29, 2, 30, 2*HOUR+30*MINUTE},
        };

        // Expected offsets by getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds)
        int[] OFFSETS1 = {
            // April 2, 2006
            -8*HOUR,
            -7*HOUR,
            -7*HOUR,
            -7*HOUR,
            -7*HOUR,

            // October 29, 2006
            -7*HOUR,
            -8*HOUR,
            -8*HOUR,
            -8*HOUR,
            -8*HOUR,
        };


        // Expected offsets by getOffset(long time, boolean local, int[] offsets) with local = true
        // or getOffsetFromLocal(long time, LocalOption nonExistingTimeOpt, LocalOption duplicatedTimeOpt, int[] offsets)
        // with nonExistingTimeOpt = STANDARD_*/duplicatedTimeOpt = STANDARD_*
        int[][] OFFSETS2 = {
            // April 2, 2006
            {-8*HOUR, 0},
            {-8*HOUR, 0},
            {-8*HOUR, 0},
            {-8*HOUR, 1*HOUR},
            {-8*HOUR, 1*HOUR},

            // Oct 29, 2006
            {-8*HOUR, 1*HOUR},
            {-8*HOUR, 0},
            {-8*HOUR, 0},
            {-8*HOUR, 0},
            {-8*HOUR, 0},
        };

        // Expected offsets by getOffsetFromLocal(long time, LocalOption nonExistingTimeOpt, LocalOption duplicatedTimeOpt, int[] offsets)
        // with nonExistingTimeOpt = DAYLIGHT_*/duplicatedTimeOpt = DAYLIGHT_*
        int[][] OFFSETS3 = {
            // April 2, 2006
            {-8*HOUR, 0},
            {-8*HOUR, 1*HOUR},
            {-8*HOUR, 1*HOUR},
            {-8*HOUR, 1*HOUR},
            {-8*HOUR, 1*HOUR},

            // October 29, 2006
            {-8*HOUR, 1*HOUR},
            {-8*HOUR, 1*HOUR},
            {-8*HOUR, 1*HOUR},
            {-8*HOUR, 0},
            {-8*HOUR, 0},
        };

        int[] offsets = new int[2];

        TimeZone utc = TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(utc);
        cal.clear();

        // Set up TimeZone objects - OlsonTimeZone, SimpleTimeZone and RuleBasedTimeZone
        BasicTimeZone[] TESTZONES = new BasicTimeZone[3];

        TESTZONES[0] = (BasicTimeZone)TimeZone.getTimeZone("America/Los_Angeles", TimeZone.TIMEZONE_ICU);
        TESTZONES[1] = new SimpleTimeZone(-8*HOUR, "Simple Pacific Time",
                                            Calendar.APRIL, 1, Calendar.SUNDAY, 2*HOUR,
                                            Calendar.OCTOBER, -1, Calendar.SUNDAY, 2*HOUR);

        InitialTimeZoneRule ir = new InitialTimeZoneRule(
                "Pacific Standard Time", // Initial time Name
                -8*HOUR,        // Raw offset
                0*HOUR);        // DST saving amount

        RuleBasedTimeZone rbPT = new RuleBasedTimeZone("Rule based Pacific Time", ir);

        DateTimeRule dtr;
        AnnualTimeZoneRule atzr;
        final int STARTYEAR = 2000;

        dtr = new DateTimeRule(Calendar.APRIL, 1, Calendar.SUNDAY,
                2*HOUR, DateTimeRule.WALL_TIME); // 1st Sunday in April, at 2AM wall time
        atzr = new AnnualTimeZoneRule("Pacific Daylight Time",
                -8*HOUR /* rawOffset */, 1*HOUR /* dstSavings */, dtr,
                STARTYEAR, AnnualTimeZoneRule.MAX_YEAR);
        rbPT.addTransitionRule(atzr);

        dtr = new DateTimeRule(Calendar.OCTOBER, -1, Calendar.SUNDAY,
                2*HOUR, DateTimeRule.WALL_TIME); // last Sunday in October, at 2AM wall time
        atzr = new AnnualTimeZoneRule("Pacific Standard Time",
                -8*HOUR /* rawOffset */, 0 /* dstSavings */, dtr,
                STARTYEAR, AnnualTimeZoneRule.MAX_YEAR);
        rbPT.addTransitionRule(atzr);

        TESTZONES[2] = rbPT;

        // Calculate millis
        long [] MILLIS = new long[DATES.length];
        for (int i = 0; i < DATES.length; i++) {
            cal.clear();
            cal.set(DATES[i][0], DATES[i][1], DATES[i][2], DATES[i][3], DATES[i][4]);
            MILLIS[i] = cal.getTimeInMillis();
        }

        DateFormat df = DateFormat.getInstance();
        df.setTimeZone(utc);

        // Test getOffset(int era, int year, int month, int day, int dayOfWeek, int millis)
        for (int i = 0; i < TESTZONES.length; i++) {
            for (int d = 0; d < DATES.length; d++) {
                int offset = TESTZONES[i].getOffset(GregorianCalendar.AD, DATES[d][0], DATES[d][1], DATES[d][2],
                                                    Calendar.SUNDAY, DATES[d][5]);
                if (offset != OFFSETS1[d]) {
                    errln("Bad offset returned by " + TESTZONES[i].getID() + " at "
                            + df.format(new Date(MILLIS[d])) + "(standard) - Got: " + offset + " Expected: " + OFFSETS1[d]);
                }
            }
        }

        // Test getOffset(long time, boolean local, int[] offsets) with local=true
        for (int i = 0; i < TESTZONES.length; i++) {
            for (int m = 0; m < MILLIS.length; m++) {
                TESTZONES[i].getOffset(MILLIS[m], true, offsets);
                if (offsets[0] != OFFSETS2[m][0] || offsets[1] != OFFSETS2[m][1]) {
                    errln("Bad offset returned by " + TESTZONES[i].getID() + " at "
                            + df.format(new Date(MILLIS[m])) + "(wall) - Got: "
                            + offsets[0] + "/" + offsets[1]
                            + " Expected: " + OFFSETS2[m][0] + "/" + OFFSETS2[m][1]);
                }
            }
        }

        // Test getOffsetFromLocal(long time, LocalOption nonExistingTimeOpt, LocalOption duplicatedTimeOpt, int[] offsets)
        // with nonExistingTimeOpt = STANDARD_*/duplicatedTimeOpt = STANDARD_*
        for (int i = 0; i < TESTZONES.length; i++) {
            for (int m = 0; m < MILLIS.length; m++) {
                TESTZONES[i].getOffsetFromLocal(MILLIS[m], LocalOption.STANDARD_FORMER, LocalOption.STANDARD_LATTER, offsets);
                if (offsets[0] != OFFSETS2[m][0] || offsets[1] != OFFSETS2[m][1]) {
                    errln("Bad offset returned by " + TESTZONES[i].getID() + " at "
                            + df.format(new Date(MILLIS[m])) + "(wall/STANDARD_FORMER/STANDARD_LATTER) - Got: "
                            + offsets[0] + "/" + offsets[1]
                            + " Expected: " + OFFSETS2[m][0] + "/" + OFFSETS2[m][1]);
                }
            }
        }

        // Test getOffsetFromLocal(long time, LocalOption nonExistingTimeOpt, LocalOption duplicatedTimeOpt, int[] offsets)
        // with nonExistingTimeOpt = DAYLIGHT_*/duplicatedTimeOpt = DAYLIGHT_*
        for (int i = 0; i < TESTZONES.length; i++) {
            for (int m = 0; m < MILLIS.length; m++) {
                TESTZONES[i].getOffsetFromLocal(MILLIS[m], LocalOption.DAYLIGHT_LATTER, LocalOption.DAYLIGHT_FORMER, offsets);
                if (offsets[0] != OFFSETS3[m][0] || offsets[1] != OFFSETS3[m][1]) {
                    errln("Bad offset returned by " + TESTZONES[i].getID() + " at "
                            + df.format(new Date(MILLIS[m])) + "(wall/DAYLIGHT_LATTER/DAYLIGHT_FORMER) - Got: "
                            + offsets[0] + "/" + offsets[1]
                            + " Expected: " + OFFSETS3[m][0] + "/" + OFFSETS3[m][1]);
                }
            }
        }

        // Test getOffsetFromLocal(long time, LocalOption nonExistingTimeOpt, LocalOption duplicatedTimeOpt, int[] offsets)
        // with nonExistingTimeOpt = FORMER/duplicatedTimeOpt = LATTER
        for (int i = 0; i < TESTZONES.length; i++) {
            for (int m = 0; m < MILLIS.length; m++) {
                TESTZONES[i].getOffsetFromLocal(MILLIS[m], LocalOption.FORMER, LocalOption.LATTER, offsets);
                if (offsets[0] != OFFSETS2[m][0] || offsets[1] != OFFSETS2[m][1]) {
                    errln("Bad offset returned by " + TESTZONES[i].getID() + " at "
                            + df.format(new Date(MILLIS[m])) + "(wall/FORMER/LATTER) - Got: "
                            + offsets[0] + "/" + offsets[1]
                            + " Expected: " + OFFSETS2[m][0] + "/" + OFFSETS2[m][1]);
                }
            }
        }

        // Test getOffsetFromLocal(long time, LocalOption nonExistingTimeOpt, LocalOption duplicatedTimeOpt, int[] offsets)
        // with nonExistingTimeOpt = LATTER/duplicatedTimeOpt = FORMER
        for (int i = 0; i < TESTZONES.length; i++) {
            for (int m = 0; m < MILLIS.length; m++) {
                TESTZONES[i].getOffsetFromLocal(MILLIS[m], LocalOption.LATTER, LocalOption.FORMER, offsets);
                if (offsets[0] != OFFSETS3[m][0] || offsets[1] != OFFSETS3[m][1]) {
                    errln("Bad offset returned by " + TESTZONES[i].getID() + " at "
                            + df.format(new Date(MILLIS[m])) + "(wall/LATTER/FORMER) - Got: "
                            + offsets[0] + "/" + offsets[1]
                            + " Expected: " + OFFSETS3[m][0] + "/" + OFFSETS3[m][1]);
                }
            }
        }
    }
}
