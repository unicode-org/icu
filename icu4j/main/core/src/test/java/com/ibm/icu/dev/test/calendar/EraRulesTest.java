// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.calendar;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.CalType;
import com.ibm.icu.impl.EraRules;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for EraRules class */
@RunWith(JUnit4.class)
public class EraRulesTest extends CoreTestFmwk {
    @Test
    public void testAPIs() {
        for (CalType calType : CalType.values()) {
            String calId = calType.getId();
            if (calId.equals("iso8601") || calId.equals("unknown")) {
                continue;
            }
            EraRules rules1 = EraRules.getInstance(calType, false);
            if (rules1 == null) {
                errln("Era rules for " + calId + " is not available.");
            }

            EraRules rules2 = EraRules.getInstance(calType, true);
            if (rules2 == null) {
                errln("Era rules for " + calId + " (including tentative eras) is not available.");
            }
            int numEras1 = rules1.getNumberOfEras();
            if (numEras1 <= 0) {
                errln("Number of era rules for " + calId + " is " + numEras1);
            }
            int numEras2 = rules2.getNumberOfEras();
            if (numEras2 < numEras1) {
                errln(
                        "Number of eras including tentative eras is fewer than one without tentative eras in calendar: "
                                + calId);
            }

            Calendar cal = Calendar.getInstance(TimeZone.GMT_ZONE, new ULocale("en"));
            int currentEra = rules1.getCurrentEraCode();
            int currentYear = cal.get(Calendar.YEAR);
            int eraCode =
                    rules1.getEraCode(
                            currentYear, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE));
            if (eraCode != currentEra) {
                errln(
                        "Current era code:"
                                + currentEra
                                + " is different from era code of now:"
                                + eraCode
                                + " in calendar:"
                                + calId);
            }

            int eraStartYear = rules1.getStartYear(currentEra);
            if (currentYear < eraStartYear) {
                errln("Current era's start year is after the current year in calendar:" + calId);
            }
        }
    }

    @Test
    public void testJapanese() {
        EraRules rules = EraRules.getInstance(CalType.JAPANESE, true);
        // Rules should have an era after Heisei
        int maxEra = rules.getMaxEraCode();
        if (maxEra <= JapaneseCalendar.HEISEI) {
            errln("Era after Heisei is not available.");
        }
        int postHeiseiStartYear = rules.getStartYear(JapaneseCalendar.HEISEI + 1);
        if (postHeiseiStartYear != 2019) {
            errln("Era after Heisei should start in 2019, but got " + postHeiseiStartYear);
        }

        // Note: Current CLDR data uses 1868-10-23 as the start date of Meiji.
        // This is not really accurate, because it does not count Lunar-solar
        // calendar system used before Meiji 6. This might be changed in future.
        assertEquals("1868-10-23", JapaneseCalendar.MEIJI, rules.getEraCode(1868, 10, 23));
        assertEquals("1868-10-22", GregorianCalendar.AD, rules.getEraCode(1868, 10, 22));
        assertEquals("0001-01-01", GregorianCalendar.AD, rules.getEraCode(1, 1, 1));
        assertEquals("0000-12-31", GregorianCalendar.BC, rules.getEraCode(0, 12, 31));
        assertEquals("-1000-01-01", GregorianCalendar.BC, rules.getEraCode(-1000, 1, 1));

        final int[] expectedEraCodes = {0, 1, 232, 233, 234, 235, 236};
        int era = rules.getMinEraCode();
        assertEquals("Minimum era", expectedEraCodes[0], era);
        for (int i = 1; i < expectedEraCodes.length; i++) {
            era = rules.getNextEraCode(era);
            assertEquals("Era index " + i, expectedEraCodes[i], era);
        }
        // getNextEraCode returns maximum era when the input era is already
        // maximum era.
        era = rules.getNextEraCode(era);
        assertEquals("Max era", maxEra, era);

        // getNextEracode returns next available era code even the input
        // era data does not exist.
        era = rules.getNextEraCode(100);
        assertEquals("getNextEraCode(100)", 232, era);

        // getNextEracode returns the maximum era code if the input
        // era code is greater than the maximum.
        era = rules.getNextEraCode(maxEra + 1);
        assertEquals("getNextEraCode(maxEra+1)", maxEra, era);

        // getNextEracode returns the minimum era code if the input
        // era code is less than the minimum.
        int minEra = rules.getMinEraCode();
        era = rules.getNextEraCode(minEra - 1);
        assertEquals("getNextEraCode(minEra-1)", minEra, era);

        int numEras = rules.getNumberOfEras();
        assertEquals("Numbers of Eras", expectedEraCodes.length, numEras);
    }
}
