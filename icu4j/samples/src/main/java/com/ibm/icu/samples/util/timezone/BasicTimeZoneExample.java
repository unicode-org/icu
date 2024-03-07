// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2011, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.util.timezone;

import java.util.Date;

import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.BasicTimeZone;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.TimeZoneRule;
import com.ibm.icu.util.TimeZoneTransition;
import com.ibm.icu.util.ULocale;

/**
 * com.ibm.icu.util.BasicTimeZone Coding Examples
 */
public class BasicTimeZoneExample {
    public static void main(String... args) {
        nextTransitionExample();
        previousTransitionExample();
        timeZoneRulesExample();
        equivalentTransitionsExample();
    }

    public static void nextTransitionExample() {
        // ---getNextTransitionExample
        System.out.println("### Iterates time zone transitions in America/Los_Angeles starting 2005-01-01 and forward");

        // A TimeZone instance created by getTimeZone with TIMEZONE_ICU is always a BasicTimeZone
        BasicTimeZone btz = (BasicTimeZone)TimeZone.getTimeZone("America/Los_Angeles", TimeZone.TIMEZONE_ICU);

        // Date format for the wall time
        SimpleDateFormat wallTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", ULocale.US);
        wallTimeFmt.setTimeZone(btz);

        long start = 1104537600000L;    // 2005-01-01 0:00 UTC
        for (int i = 0; i < 5; i++) {   // Up to 5 transitions
            TimeZoneTransition trans = btz.getNextTransition(start, false /* not including start time */);

            // Display the transition time and offset information
            long transTime = trans.getTime();
            System.out.println(wallTimeFmt.format(new Date(transTime - 1)) + " -> " + wallTimeFmt.format(new Date(transTime)));
            System.out.println(" - Before (Offset/Save): " + trans.getFrom().getRawOffset() + "/" + trans.getFrom().getDSTSavings());
            System.out.println(" - After  (Offset/Save): " + trans.getTo().getRawOffset() + "/" + trans.getTo().getDSTSavings());

            // Update start time for next transition
            start = transTime;
        }
        // ---getNextTransitionExample
    }

    public static void previousTransitionExample() {
        // ---getPreviousTransitionExample
        System.out.println("### Iterates time zone transitions in America/Los_Angeles starting 2010-01-01 and backward");

        // A TimeZone instance created by getTimeZone with TIMEZONE_ICU is always a BasicTimeZone
        BasicTimeZone btz = (BasicTimeZone)TimeZone.getTimeZone("America/Los_Angeles", TimeZone.TIMEZONE_ICU);

        // Date format for the wall time
        SimpleDateFormat wallTimeFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", ULocale.US);
        wallTimeFmt.setTimeZone(btz);

        long start = 1262304000000L;    // 2010-01-01 0:00 UTC
        for (int i = 0; i < 5; i++) {   // Up to 5 transitions
            TimeZoneTransition trans = btz.getPreviousTransition(start, false /* not including start time */);

            // Display the transition time and offset information
            long transTime = trans.getTime();
            System.out.println(wallTimeFmt.format(new Date(transTime - 1)) + " -> " + wallTimeFmt.format(new Date(transTime)));
            System.out.println(" - Before (Offset/Save): " + trans.getFrom().getRawOffset() + "/" + trans.getFrom().getDSTSavings());
            System.out.println(" - After  (Offset/Save): " + trans.getTo().getRawOffset() + "/" + trans.getTo().getDSTSavings());

            // Update start time for next transition
            start = transTime;
        }
        // ---getPreviousTransitionExample
    }

    public static void timeZoneRulesExample() {
        // ---getTimeZoneRulesExample
        System.out.println("### Extracts time zone rules used by America/Los_Angeles since year 2005");

        // A TimeZone instance created by getTimeZone with TIMEZONE_ICU is always a BasicTimeZone
        BasicTimeZone btz = (BasicTimeZone)TimeZone.getTimeZone("America/Los_Angeles", TimeZone.TIMEZONE_ICU);
        long since = 1104537600000L;    // 2005-01-01 0:00 UTC
        TimeZoneRule[] rules = btz.getTimeZoneRules(since);
        System.out.println("Rule(initial): " + rules[0]);
        for (int i = 1; i < rules.length; i++) {
            System.out.println("Rule: " + rules[i]);
        }
        // ---getTimeZoneRulesExample
    }

    public static void equivalentTransitionsExample() {
        // ---hasEquivalentTransitionsExample
        System.out.println("### Compare America/New_York and America/Detroit since year 1970");

        // A TimeZone instance created by getTimeZone with TIMEZONE_ICU is always a BasicTimeZone
        BasicTimeZone tzNewYork = (BasicTimeZone)TimeZone.getTimeZone("America/New_York", TimeZone.TIMEZONE_ICU);
        BasicTimeZone tzDetroit = (BasicTimeZone)TimeZone.getTimeZone("America/Detroit", TimeZone.TIMEZONE_ICU);

        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("Etc/GMT"));

        // Compare these time zones every 10 years since year 1970 up to year 2009
        for (int startYear = 1970; startYear <= 2000; startYear += 10) {
            long start, end;

            cal.set(startYear, Calendar.JANUARY, 1, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            start = cal.getTimeInMillis();

            // Set the end time to the end of startYear + 9
            int endYear = startYear + 9;
            cal.set(endYear + 1, Calendar.JANUARY, 1, 0, 0, 0);
            end = cal.getTimeInMillis() - 1;

            // Check if these two zones have equivalent time zone transitions for the given time range
            boolean isEquivalent = tzNewYork.hasEquivalentTransitions(tzDetroit, start, end);
            System.out.println(startYear + "-" + endYear + ": " + isEquivalent);
        }
        // ---hasEquivalentTransitionsExample
    }
}
