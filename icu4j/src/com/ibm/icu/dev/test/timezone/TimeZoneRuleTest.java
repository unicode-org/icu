/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.timezone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.AnnualTimeZoneRule;
import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateTimeRule;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.InitialTimeZoneRule;
import com.ibm.icu.util.RuleBasedTimeZone;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneRule;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VTimeZone;

/**
 * Test cases for TimeZoneRule and RuleBasedTimeZone
 */
public class TimeZoneRuleTest extends TestFmwk {

    private static final int HOUR = 60 * 60 * 1000;

    public static void main(String[] args) throws Exception {
        new TimeZoneRuleTest().run(args);
    }

    /*
     * Compare SimpleTimeZone with equivalent RBTZ
     */
    public void TestSimpleRuleBasedTimeZone() {
        SimpleTimeZone stz = new SimpleTimeZone(-1*HOUR, "TestSTZ",
                Calendar.SEPTEMBER, -30, -Calendar.SATURDAY, 1*HOUR, SimpleTimeZone.WALL_TIME,
                Calendar.FEBRUARY, 2, Calendar.SUNDAY, 1*HOUR, SimpleTimeZone.WALL_TIME,
                1*HOUR);


        DateTimeRule dtr;
        AnnualTimeZoneRule atzr;
        final int STARTYEAR = 2000;

        InitialTimeZoneRule ir = new InitialTimeZoneRule(
                "RBTZ_Initial", // Initial time Name
                -1*HOUR,        // Raw offset
                1*HOUR);        // DST saving amount
        
        // Original rules
        RuleBasedTimeZone rbtz1 = new RuleBasedTimeZone("RBTZ1", ir);
        dtr = new DateTimeRule(Calendar.SEPTEMBER, 30, Calendar.SATURDAY, false,
                1*HOUR, DateTimeRule.WALL_TIME); // SUN<=30 in September, at 1AM wall time
        atzr = new AnnualTimeZoneRule("RBTZ_DST1",
                -1*HOUR /* rawOffset */, 1*HOUR /* dstSavings */, dtr,
                STARTYEAR, AnnualTimeZoneRule.MAX_YEAR);
        rbtz1.addTransitionRule(atzr);
        dtr = new DateTimeRule(Calendar.FEBRUARY, 2, Calendar.SUNDAY,
                1*HOUR, DateTimeRule.WALL_TIME); // 2nd Sunday in February, at 1AM wall time
        atzr = new AnnualTimeZoneRule("RBTZ_STD1",
                -1*HOUR /* rawOffset */, 0 /* dstSavings */, dtr,
                STARTYEAR, AnnualTimeZoneRule.MAX_YEAR);
        rbtz1.addTransitionRule(atzr);

        // Equivalent, but different date rule type
        RuleBasedTimeZone rbtz2 = new RuleBasedTimeZone("RBTZ2", ir);
        dtr = new DateTimeRule(Calendar.SEPTEMBER, -1, Calendar.SATURDAY,
                1*HOUR, DateTimeRule.WALL_TIME); // Last Sunday in September at 1AM wall time
        atzr = new AnnualTimeZoneRule("RBTZ_DST2", -1*HOUR, 1*HOUR, dtr, STARTYEAR, AnnualTimeZoneRule.MAX_YEAR);
        rbtz2.addTransitionRule(atzr);
        dtr = new DateTimeRule(Calendar.FEBRUARY, 8, Calendar.SUNDAY, true,
                1*HOUR, DateTimeRule.WALL_TIME); // SUN>=8 in February, at 1AM wall time
        atzr = new AnnualTimeZoneRule("RBTZ_STD2", -1*HOUR, 0, dtr, STARTYEAR, AnnualTimeZoneRule.MAX_YEAR);
        rbtz2.addTransitionRule(atzr);

        // Equivalent, but different time rule type
        RuleBasedTimeZone rbtz3 = new RuleBasedTimeZone("RBTZ3", ir);
        dtr = new DateTimeRule(Calendar.SEPTEMBER, 30, Calendar.SATURDAY, false,
                2*HOUR, DateTimeRule.UTC_TIME);
        atzr = new AnnualTimeZoneRule("RBTZ_DST3", -1*HOUR, 1*HOUR, dtr, STARTYEAR, AnnualTimeZoneRule.MAX_YEAR);
        rbtz3.addTransitionRule(atzr);
        dtr = new DateTimeRule(Calendar.FEBRUARY, 2, Calendar.SUNDAY,
                0*HOUR, DateTimeRule.STANDARD_TIME);
        atzr = new AnnualTimeZoneRule("RBTZ_STD3", -1*HOUR, 0, dtr, STARTYEAR, AnnualTimeZoneRule.MAX_YEAR);
        rbtz3.addTransitionRule(atzr);

        // Check equivalency for 10 years
        long start = getUTCMillis(STARTYEAR, Calendar.JANUARY, 1);
        long until = getUTCMillis(STARTYEAR + 10, Calendar.JANUARY, 1);

        if (!(stz.hasEquivalentTransitions(rbtz1, start, until))) {
            errln("FAIL: rbtz1 must be equivalent to the SimpleTimeZone in the time range.");
        }
        if (!(stz.hasEquivalentTransitions(rbtz2, start, until))) {
            errln("FAIL: rbtz2 must be equivalent to the SimpleTimeZone in the time range.");
        }
        if (!(stz.hasEquivalentTransitions(rbtz3, start, until))) {
            errln("FAIL: rbtz3 must be equivalent to the SimpleTimeZone in the time range.");
        }
    }

    /*
     * Test equivalency between OlsonTimeZone and custom RBTZ representing the
     * equivalent rules in a certain time range
     */
    public void TestHistoricalRuleBasedTimeZone() {
        // Compare to America/New_York with equivalent RBTZ
        TimeZone ny = TimeZone.getTimeZone("America/New_York");

        //RBTZ
        InitialTimeZoneRule ir = new InitialTimeZoneRule("EST", -5*HOUR, 0);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone("EST5EDT", ir);

        DateTimeRule dtr;
        AnnualTimeZoneRule tzr;

        // Standard time
        dtr = new DateTimeRule(Calendar.OCTOBER, -1, Calendar.SUNDAY,
                2*HOUR, DateTimeRule.WALL_TIME);    // Last Sunday in October, at 2AM wall time
        tzr = new AnnualTimeZoneRule("EST", -5*HOUR /* rawOffset */, 0 /* dstSavings */, dtr, 1967, 2006);
        rbtz.addTransitionRule(tzr);

        dtr = new DateTimeRule(Calendar.NOVEMBER, 1, Calendar.SUNDAY,
                true, 2*HOUR, DateTimeRule.WALL_TIME);  // SUN>=1 in November, at 2AM wall time
        tzr = new AnnualTimeZoneRule("EST", -5*HOUR, 0, dtr, 2007, AnnualTimeZoneRule.MAX_YEAR);
        rbtz.addTransitionRule(tzr);

        // Daylight saving time
        dtr = new DateTimeRule(Calendar.APRIL, -1, Calendar.SUNDAY,
                2*HOUR, DateTimeRule.WALL_TIME);    // Last Sunday in April, at 2AM wall time
        tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1967, 1973);
        rbtz.addTransitionRule(tzr);

        dtr = new DateTimeRule(Calendar.JANUARY, 6,
                2*HOUR, DateTimeRule.WALL_TIME);    // January 6, at 2AM wall time
        tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1974, 1974);
        rbtz.addTransitionRule(tzr);
        
        dtr = new DateTimeRule(Calendar.FEBRUARY, 23,
                2*HOUR, DateTimeRule.WALL_TIME);    // February 23, at 2AM wall time
        tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1975, 1975);
        rbtz.addTransitionRule(tzr);

        dtr = new DateTimeRule(Calendar.APRIL, -1, Calendar.SUNDAY,
                2*HOUR, DateTimeRule.WALL_TIME);    // Last Sunday in April, at 2AM wall time
        tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1976, 1986);
        rbtz.addTransitionRule(tzr);

        dtr = new DateTimeRule(Calendar.APRIL, 1, Calendar.SUNDAY,
                true, 2*HOUR, DateTimeRule.WALL_TIME);  // SUN>=1 in April, at 2AM wall time
        tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 1987, 2006);
        rbtz.addTransitionRule(tzr);

        dtr = new DateTimeRule(Calendar.MARCH, 8, Calendar.SUNDAY,
                true, 2*HOUR, DateTimeRule.WALL_TIME);  // SUN>=8 in March, at 2AM wall time
        tzr = new AnnualTimeZoneRule("EDT", -5*HOUR, 1*HOUR, dtr, 2007, AnnualTimeZoneRule.MAX_YEAR);
        rbtz.addTransitionRule(tzr);

        // hasEquivalentTransitions
        long jan1_1950 = getUTCMillis(1950, Calendar.JANUARY, 1);
        long jan1_1967 = getUTCMillis(1971, Calendar.JANUARY, 1);
        long jan1_2010 = getUTCMillis(2010, Calendar.JANUARY, 1);        

        if (!(((BasicTimeZone)ny).hasEquivalentTransitions(rbtz, jan1_1967, jan1_2010))) {
            errln("FAIL: The RBTZ must be equivalent to America/New_York between 1967 and 2010");
        }
        if (((BasicTimeZone)ny).hasEquivalentTransitions(rbtz, jan1_1950, jan1_2010)) {
            errln("FAIL: The RBTZ must not be equivalent to America/New_York between 1950 and 2010");
        }

        // Same with above, but calling RBTZ#hasEquivalentTransitions against OlsonTimeZone
        if (!rbtz.hasEquivalentTransitions(ny, jan1_1967, jan1_2010)) {
            errln("FAIL: The RBTZ must be equivalent to America/New_York between 1967 and 2010");
        }
        if (rbtz.hasEquivalentTransitions(ny, jan1_1950, jan1_2010)) {
            errln("FAIL: The RBTZ must not be equivalent to America/New_York between 1950 and 2010");
        }

    
    }

    /*
     * Check if transitions returned by getNextTransition/getPreviousTransition
     * are actual time transitions.
     */
    public void TestOlsonTransition() {
        String[] zids = getTestZIDs();
        for (int i = 0; i < zids.length; i++) {
            TimeZone tz = TimeZone.getTimeZone(zids[i]);
            if (tz == null) {
                break;
            }
            int j = 0;
            while (true) {
                long[] timerange = getTestTimeRange(j++);
                if (timerange == null) {
                    break;
                }
                verifyTransitions(tz, timerange[0], timerange[1]);
            }
        }
    }

    /*
     * Check if an OlsonTimeZone and its equivalent RBTZ have the exact same
     * transitions.
     */
    public void TestRBTZTransition() {
        int[] STARTYEARS = {
            1950,
            1975,
            2000,
            2010
        };

        String[] zids = getTestZIDs();
        for (int i = 0; i < zids.length; i++) {
            TimeZone tz = TimeZone.getTimeZone(zids[i]);
            if (tz == null) {
                break;
            }
            for (int j = 0; j < STARTYEARS.length; j++) {
                long startTime = getUTCMillis(STARTYEARS[j], Calendar.JANUARY, 1);
                TimeZoneRule[] rules = ((BasicTimeZone)tz).getTimeZoneRules(startTime);
                RuleBasedTimeZone rbtz = new RuleBasedTimeZone(tz.getID() + "(RBTZ)",
                        (InitialTimeZoneRule)rules[0]);
                for (int k = 1; k < rules.length; k++) {
                    rbtz.addTransitionRule(rules[k]);
                }

                // Compare the original OlsonTimeZone with the RBTZ starting the startTime for 20 years
                long until = getUTCMillis(STARTYEARS[j] + 20, Calendar.JANUARY, 1);

                // Ascending
                compareTransitionsAscending(tz, rbtz, startTime, until, false);
                // Ascending/inclusive
                compareTransitionsAscending(tz, rbtz, startTime + 1, until, true);
                // Descending
                compareTransitionsDescending(tz, rbtz, startTime, until, false);
                // Descending/inclusive
                compareTransitionsDescending(tz, rbtz, startTime + 1, until, true);
            }
            
        }
    }

    /*
     * Test cases for HasTimeZoneRules#hasEquivalentTransitions
     */
    public void TestHasEquivalentTransitions() {
        // America/New_York and America/Indiana/Indianapolis are equivalent
        // since 2006
        TimeZone newyork = TimeZone.getTimeZone("America/New_York");
        TimeZone indianapolis = TimeZone.getTimeZone("America/Indiana/Indianapolis");
        TimeZone gmt_5 = TimeZone.getTimeZone("Etc/GMT+5");

        long jan1_1971 = getUTCMillis(1971, Calendar.JANUARY, 1);
        long jan1_2005 = getUTCMillis(2005, Calendar.JANUARY, 1);
        long jan1_2006 = getUTCMillis(2006, Calendar.JANUARY, 1);
        long jan1_2007 = getUTCMillis(2007, Calendar.JANUARY, 1);
        long jan1_2011 = getUTCMillis(2010, Calendar.JANUARY, 1);
        
        if (((BasicTimeZone)newyork).hasEquivalentTransitions(indianapolis, jan1_2005, jan1_2011)) {
            errln("FAIL: New_York is not equivalent to Indianapolis between 2005 and 2010");
        }
        if (!((BasicTimeZone)newyork).hasEquivalentTransitions(indianapolis, jan1_2006, jan1_2011)) {
            errln("FAIL: New_York is equivalent to Indianapolis between 2006 and 2010");
        }

        if (!((BasicTimeZone)indianapolis).hasEquivalentTransitions(gmt_5, jan1_1971, jan1_2006)) {
            errln("FAIL: Indianapolis is equivalent to GMT+5 between 1971 and 2005");
        }
        if (((BasicTimeZone)indianapolis).hasEquivalentTransitions(gmt_5, jan1_1971, jan1_2007)) {
            errln("FAIL: Indianapolis is not equivalent to GMT+5 between 1971 and 2006");
        }
    }

    /*
     * Write out time zone rules of OlsonTimeZone into VTIMEZONE format, create a new
     * VTimeZone from the VTIMEZONE data, then compare transitions
     */
    public void TestVTimeZoneRoundTrip() {
        long startTime = getUTCMillis(1850, Calendar.JANUARY, 1);
        long endTime = getUTCMillis(2050, Calendar.JANUARY, 1);

        String[] tzids = getTestZIDs();
        for (int i = 0; i < tzids.length; i++) {
            BasicTimeZone olsontz = (BasicTimeZone)TimeZone.getTimeZone(tzids[i]);
            VTimeZone vtz_org = VTimeZone.create(tzids[i]);
            VTimeZone vtz_new = null;
            try {
                // Write out VTIMEZONE
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(baos);
                vtz_org.write(writer);
                writer.close();
                byte[] vtzdata = baos.toByteArray();
                // Read VTIMEZONE
                ByteArrayInputStream bais = new ByteArrayInputStream(vtzdata);
                InputStreamReader reader = new InputStreamReader(bais);
                vtz_new = VTimeZone.create(reader);
                reader.close();

            } catch (IOException ioe) {
                errln("FAIL: IO error while writing/reading VTIMEZONE data");
            }
            // Check equivalency after the first transition.
            // The DST information before the first transition might be lost
            // because there is no good way to represent the initial time with
            // VTIMEZONE.
            if (vtz_new.getOffset(startTime) != olsontz.getOffset(startTime)) {
                errln("FAIL: VTimeZone for " + tzids[i]
                         + " is not equivalent to its OlsonTimeZone corresponding at " + startTime);
            }
            TimeZoneTransition tzt = olsontz.getNextTransition(startTime, false);
            if (tzt != null) {
                if (!vtz_new.hasEquivalentTransitions(olsontz, tzt.getTime(), endTime, true)) {
                    errln("FAIL: VTimeZone for " + tzids[i] + " is not equivalent to its OlsonTimeZone corresponding.");
                }
            }
        }
    }

    /*
     * Write out time zone rules of OlsonTimeZone after a cutover date into VTIMEZONE format,
     * create a new VTimeZone from the VTIMEZONE data, then compare transitions
     */
    public void TestVTimeZoneRoundTripPartial() {
        long[] cutoverTimes = new long[] {
            getUTCMillis(1900, Calendar.JANUARY, 1),
            getUTCMillis(1950, Calendar.JANUARY, 1),
            getUTCMillis(2020, Calendar.JANUARY, 1)
        };
        long endTime = getUTCMillis(2050, Calendar.JANUARY, 1);

        String[] tzids = getTestZIDs();
        for (int n = 0; n < cutoverTimes.length; n++) {
            long startTime = cutoverTimes[n];
            for (int i = 0; i < tzids.length; i++) {
                BasicTimeZone olsontz = (BasicTimeZone)TimeZone.getTimeZone(tzids[i]);
                VTimeZone vtz_org = VTimeZone.create(tzids[i]);
                VTimeZone vtz_new = null;
                try {
                    // Write out VTIMEZONE
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(baos);
                    vtz_org.write(writer, startTime);
                    writer.close();
                    byte[] vtzdata = baos.toByteArray();
                    // Read VTIMEZONE
                    ByteArrayInputStream bais = new ByteArrayInputStream(vtzdata);
                    InputStreamReader reader = new InputStreamReader(bais);
                    vtz_new = VTimeZone.create(reader);
                    reader.close();

                } catch (IOException ioe) {
                    errln("FAIL: IO error while writing/reading VTIMEZONE data");
                }
                // Check equivalency after the first transition.
                // The DST information before the first transition might be lost
                // because there is no good way to represent the initial time with
                // VTIMEZONE.
                if (vtz_new.getOffset(startTime) != olsontz.getOffset(startTime)) {
                    errln("FAIL: VTimeZone for " + tzids[i]
                             + " is not equivalent to its OlsonTimeZone corresponding at " + startTime);
                }
                TimeZoneTransition tzt = olsontz.getNextTransition(startTime, false);
                if (tzt != null) {
                    if (!vtz_new.hasEquivalentTransitions(olsontz, tzt.getTime(), endTime, true)) {
                        errln("FAIL: VTimeZone for " + tzids[i] + "(>=" + startTime + ") is not equivalent to its OlsonTimeZone corresponding.");
                    }
                }
            }            
        }
    }

    /*
     * Write out simple time zone rules from an OlsonTimeZone at various time into VTIMEZONE
     * format and create a new VTimeZone from the VTIMEZONE data, then make sure the raw offset
     * and DST savings are same in these two time zones.
     */
    public void TestVTimeZoneSimpleWrite() {
        long[] testTimes = new long[] {
                getUTCMillis(2006, Calendar.JANUARY, 1),
                getUTCMillis(2006, Calendar.MARCH, 15),
                getUTCMillis(2006, Calendar.MARCH, 31),
                getUTCMillis(2006, Calendar.APRIL, 5),
                getUTCMillis(2006, Calendar.OCTOBER, 25),
                getUTCMillis(2006, Calendar.NOVEMBER, 1),
                getUTCMillis(2006, Calendar.NOVEMBER, 5),
                getUTCMillis(2007, Calendar.JANUARY, 1)
        };

        String[] tzids = getTestZIDs();
        for (int n = 0; n < testTimes.length; n++) {
            long time = testTimes[n];

            int[] offsets1 = new int[2];
            int[] offsets2 = new int[2];

            for (int i = 0; i < tzids.length; i++) {
                VTimeZone vtz_org = VTimeZone.create(tzids[i]);
                VTimeZone vtz_new = null;
                try {
                    // Write out VTIMEZONE
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    OutputStreamWriter writer = new OutputStreamWriter(baos);
                    vtz_org.writeSimple(writer, time);
                    writer.close();
                    byte[] vtzdata = baos.toByteArray();
                    // Read VTIMEZONE
                    ByteArrayInputStream bais = new ByteArrayInputStream(vtzdata);
                    InputStreamReader reader = new InputStreamReader(bais);
                    vtz_new = VTimeZone.create(reader);
                    reader.close();
                } catch (IOException ioe) {
                    errln("FAIL: IO error while writing/reading VTIMEZONE data");
                }

                // Check equivalency
                vtz_org.getOffset(time, false, offsets1);
                vtz_new.getOffset(time, false, offsets2);
                if (offsets1[0] != offsets2[0] || offsets1[1] != offsets2[1]) {
                    errln("FAIL: VTimeZone writeSimple for " + tzids[i] + " at time " + time + " failed to the round trip.");
                }
            }            
        }
    }

    /*
     * Write out time zone rules of OlsonTimeZone into VTIMEZONE format with RFC2445 header TZURL and
     * LAST-MODIFIED, create a new VTimeZone from the VTIMEZONE data to see if the headers are preserved.
     */
    public void TestVTimeZoneHeaderProps() {
        String tzid = "America/Chicago";
        String tzurl = "http://source.icu-project.org";
        Date lastmod = new Date(getUTCMillis(2007, Calendar.JUNE, 1));

        VTimeZone vtz = VTimeZone.create(tzid);
        vtz.setTZURL(tzurl);
        vtz.setLastModified(lastmod);

        // Roundtrip conversion
        VTimeZone newvtz1 = null;
        try {
            // Write out VTIMEZONE
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos);
            vtz.write(writer);
            writer.close();
            byte[] vtzdata = baos.toByteArray();
            // Read VTIMEZONE
            ByteArrayInputStream bais = new ByteArrayInputStream(vtzdata);
            InputStreamReader reader = new InputStreamReader(bais);
            newvtz1 = VTimeZone.create(reader);
            reader.close();

            // Check if TZURL and LAST-MODIFIED headers are preserved
            if (!(tzurl.equals(newvtz1.getTZURL()))) {
                errln("FAIL: TZURL property is not preserved during the roundtrip conversion.  Before:"
                        + tzurl + "/After:" + newvtz1.getTZURL());
            }
            if (!(lastmod.equals(newvtz1.getLastModified()))) {
                errln("FAIL: LAST-MODIFIED property is not preserved during the roundtrip conversion.  Before:"
                        + lastmod.getTime() + "/After:" + newvtz1.getLastModified().getTime());
            }
        } catch (IOException ioe) {
            errln("FAIL: IO error while writing/reading VTIMEZONE data");
        }

        // Second roundtrip, with a cutover
        VTimeZone newvtz2 = null;
        try {
            // Set different tzurl
            String newtzurl = "http://www.ibm.com";
            newvtz1.setTZURL(newtzurl);
            // Write out VTIMEZONE
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStreamWriter writer = new OutputStreamWriter(baos);
            newvtz1.write(writer, getUTCMillis(2000, Calendar.JANUARY, 1));
            writer.close();
            byte[] vtzdata = baos.toByteArray();
            // Read VTIMEZONE
            ByteArrayInputStream bais = new ByteArrayInputStream(vtzdata);
            InputStreamReader reader = new InputStreamReader(bais);
            newvtz2 = VTimeZone.create(reader);
            reader.close();

            // Check if TZURL and LAST-MODIFIED headers are preserved
            if (!(newtzurl.equals(newvtz2.getTZURL()))) {
                errln("FAIL: TZURL property is not preserved during the second roundtrip conversion.  Before:"
                        + newtzurl + "/After:" + newvtz2.getTZURL());
            }
            if (!(lastmod.equals(newvtz2.getLastModified()))) {
                errln("FAIL: LAST-MODIFIED property is not preserved during the second roundtrip conversion.  Before:"
                        + lastmod.getTime() + "/After:" + newvtz2.getLastModified().getTime());
            }
        } catch (IOException ioe) {
            errln("FAIL: IO error while writing/reading VTIMEZONE data");
        }
        
    }

    /*
     * Extract simple rules from an OlsonTimeZone and make sure the rule format matches
     * the expected format.
     */
    public void TestGetSimpleRules() {
        long[] testTimes = new long[] {
                getUTCMillis(1970, Calendar.JANUARY, 1),
                getUTCMillis(2000, Calendar.MARCH, 31),
                getUTCMillis(2005, Calendar.JULY, 1),
                getUTCMillis(2010, Calendar.NOVEMBER, 1),
            };

        String[] tzids = getTestZIDs();
        for (int n = 0; n < testTimes.length; n++) {
            long time = testTimes[n];
            for (int i = 0; i < tzids.length; i++) {
                BasicTimeZone tz = (BasicTimeZone)TimeZone.getTimeZone(tzids[i]);
                TimeZoneRule[] rules = tz.getSimpleTimeZoneRulesNear(time);
                if (rules == null) {
                    errln("FAIL: Failed to extract simple rules for " + tzids[i] + " at " + time);
                } else {
                    if (rules.length == 1) {
                        if (!(rules[0] instanceof InitialTimeZoneRule)) {
                            errln("FAIL: Unexpected rule object type is returned for " + tzids[i] + " at " + time);
                        }
                    } else if (rules.length == 3) {
                        if (!(rules[0] instanceof InitialTimeZoneRule)
                                || !(rules[1] instanceof AnnualTimeZoneRule)
                                || !(rules[2] instanceof AnnualTimeZoneRule)) {
                            errln("FAIL: Unexpected rule object type is returned for " + tzids[i] + " at " + time);
                        }
                        for (int idx = 1; idx <= 2; idx++) {
                            DateTimeRule dtr = ((AnnualTimeZoneRule)rules[idx]).getRule();
                            if (dtr.getTimeRuleType() != DateTimeRule.WALL_TIME) {
                                errln("FAIL: WALL_TIME is not used as the time rule in the time zone rule(" + idx + ") for " + tzids[i] + " at " + time);
                            }
                            if (dtr.getDateRuleType() != DateTimeRule.DOW) {
                                errln("FAIL: DOW is not used as the date rule in the time zone rule(" + idx + ") for " + tzids[i] + " at " + time);
                            }
                        }
                    } else {
                        errln("FAIL: Unexpected number of rules returned for " + tzids[i] + " at " + time);
                    }
                }
            }
        }
    }
    
    /*
     * Check if a time shift really happens on each transition returned by getNextTransition or
     * getPreviousTransition in the specified time range
     */
    private void verifyTransitions(TimeZone tz, long start, long end) {
        BasicTimeZone icutz = (BasicTimeZone)tz;
        long time;
        int[] before = new int[2];
        int[] after = new int[2];
        TimeZoneTransition tzt0;

        // Ascending
        tzt0 = null;
        time = start;
        while(true) {
            TimeZoneTransition tzt = icutz.getNextTransition(time, false);

            if (tzt == null) {
                break;
            }
            time = tzt.getTime();
            if (time >= end) {
                break;
            }
            icutz.getOffset(time, false, after);
            icutz.getOffset(time - 1, false, before);

            if (after[0] == before[0] && after[1] == before[1]) {
                errln("FAIL: False transition returned by getNextTransition for " + icutz.getID() + " at " + time);
            }
            if (tzt0 != null &&
                    (tzt0.getTo().getRawOffset() != tzt.getFrom().getRawOffset()
                    || tzt0.getTo().getDSTSavings() != tzt.getFrom().getDSTSavings())) {
                errln("FAIL: TO rule of the previous transition does not match FROM rule of this transtion at "
                        + time + " for " + icutz.getID());                
            }
            tzt0 = tzt;
        }

        // Descending
        tzt0 = null;
        time = end;
        while(true) {
            TimeZoneTransition tzt = icutz.getPreviousTransition(time, false);
            if (tzt == null) {
                break;
            }
            time = tzt.getTime();
            if (time <= start) {
                break;
            }
            icutz.getOffset(time, false, after);
            icutz.getOffset(time - 1, false, before);

            if (after[0] == before[0] && after[1] == before[1]) {
                errln("FAIL: False transition returned by getPreviousTransition for " + icutz.getID() + " at " + time);
            }

            if (tzt0 != null &&
                    (tzt0.getFrom().getRawOffset() != tzt.getTo().getRawOffset()
                    || tzt0.getFrom().getDSTSavings() != tzt.getTo().getDSTSavings())) {
                errln("FAIL: TO rule of the next transition does not match FROM rule in this transtion at "
                        + time + " for " + icutz.getID());                
            }
            tzt0 = tzt;
        }
    }

    /*
     * Compare all time transitions in 2 time zones in the specified time range in ascending order
     */
    private void compareTransitionsAscending(TimeZone tz1, TimeZone tz2, long start, long end, boolean inclusive) {
        BasicTimeZone z1 = (BasicTimeZone)tz1;
        BasicTimeZone z2 = (BasicTimeZone)tz2;
        String zid1 = tz1.getID();
        String zid2 = tz2.getID();

        long time = start;
        while(true) {
            TimeZoneTransition tzt1 = z1.getNextTransition(time, inclusive);
            TimeZoneTransition tzt2 = z2.getNextTransition(time, inclusive);
            boolean inRange1 = false;
            boolean inRange2 = false;
            if (tzt1 != null) {
                if (tzt1.getTime() < end || (inclusive && tzt1.getTime() == end)) {
                    inRange1 = true;
                }
            }
            if (tzt2 != null) {
                if (tzt2.getTime() < end || (inclusive && tzt2.getTime() == end)) {
                    inRange2 = true;
                }
            }
            if (!inRange1 && !inRange2) {
                // No more transition in the range
                break;
            }
            if (!inRange1) {
                errln("FAIL: " + zid1 + " does not have any transitions after " + time + " before " + end);
                break;
            }
            if (!inRange2) {
                errln("FAIL: " + zid2 + " does not have any transitions after " + time + " before " + end);
                break;
            }
            if (tzt1.getTime() != tzt2.getTime()) {
                errln("FAIL: First transition after " + time + " "
                        + zid1 + "[" + tzt1.getTime() + "] "
                        + zid2 + "[" + tzt2.getTime() + "]");
                break;
            }
            time = tzt1.getTime();
            if (inclusive) {
                time++;
            }
        }
    }

    /*
     * Compare all time transitions in 2 time zones in the specified time range in descending order
     */
    private void compareTransitionsDescending(TimeZone tz1, TimeZone tz2, long start, long end, boolean inclusive) {
        BasicTimeZone z1 = (BasicTimeZone)tz1;
        BasicTimeZone z2 = (BasicTimeZone)tz2;
        String zid1 = tz1.getID();
        String zid2 = tz2.getID();
        long time = end;
        while(true) {
            TimeZoneTransition tzt1 = z1.getPreviousTransition(time, inclusive);
            TimeZoneTransition tzt2 = z2.getPreviousTransition(time, inclusive);
            boolean inRange1 = false;
            boolean inRange2 = false;
            if (tzt1 != null) {
                if (tzt1.getTime() > start || (inclusive && tzt1.getTime() == start)) {
                    inRange1 = true;
                }
            }
            if (tzt2 != null) {
                if (tzt2.getTime() > start || (inclusive && tzt2.getTime() == start)) {
                    inRange2 = true;
                }
            }
            if (!inRange1 && !inRange2) {
                // No more transition in the range
                break;
            }
            if (!inRange1) {
                errln("FAIL: " + zid1 + " does not have any transitions before " + time + " after " + start);
                break;
            }
            if (!inRange2) {
                errln("FAIL: " + zid2 + " does not have any transitions before " + time + " after " + start);
                break;
            }
            if (tzt1.getTime() != tzt2.getTime()) {
                errln("FAIL: Last transition before " + time + " "
                        + zid1 + "[" + tzt1.getTime() + "] "
                        + zid2 + "[" + tzt2.getTime() + "]");
                break;
            }
            time = tzt1.getTime();
            if (inclusive) {
                time--;
            }
        }
    }

    private static final String[] TESTZIDS = {
        "AGT",
        "America/New_York",
        "America/Los_Angeles",
        "America/Indiana/Indianapolis",
        "America/Havana",
        "Europe/Lisbon",
        "Europe/Paris",
        "Asia/Tokyo",
        "Asia/Sakhalin",
        "Africa/Cairo",
        "Africa/Windhoek",
        "Australia/Sydney",
        "Etc/GMT+8"
    };

    private String[] getTestZIDs() {
        if (isVerbose()) {
            return TimeZone.getAvailableIDs();
        }
        return TESTZIDS;
    }

    private static final int[][] TESTYEARS = {
        {1895, 1905}, // including int32 minimum second
        {1965, 1975}, // including the epoch
        {1995, 2015}  // practical year range
    };

    private long[] getTestTimeRange(int idx) {
        int loyear, hiyear;
        if (idx < TESTYEARS.length) {
            loyear = TESTYEARS[idx][0];
            hiyear = TESTYEARS[idx][1];
        } else if (idx == TESTYEARS.length && isVerbose()) {
            loyear = 1850;
            hiyear = 2050;
        } else {
            return null;
        }

        long[] times = new long[2];
        times[0] = getUTCMillis(loyear, Calendar.JANUARY, 1);
        times[1] = getUTCMillis(hiyear + 1, Calendar.JANUARY, 1);

        return times;
    }

    private GregorianCalendar utcCal;

    private long getUTCMillis(int year, int month, int dayOfMonth) {
        if (utcCal == null) {
            utcCal = new GregorianCalendar(TimeZone.getTimeZone("UTC"), ULocale.ROOT);
        }
        utcCal.clear();
        utcCal.set(year, month, dayOfMonth);
        return utcCal.getTimeInMillis();
    }
}