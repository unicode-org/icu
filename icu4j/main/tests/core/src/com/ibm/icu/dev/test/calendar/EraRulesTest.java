// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.CalType;
import com.ibm.icu.impl.EraRules;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

/**
 * Tests for EraRules class
 */
@RunWith(JUnit4.class)
public class EraRulesTest extends TestFmwk {
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
                errln("Number of era including tentative eras is fewer than one without tentative eras in calendar: "
                        + calId);
            }

            Calendar cal = Calendar.getInstance(TimeZone.GMT_ZONE, new ULocale("en"));
            int currentIdx = rules1.getCurrentEraIndex();
            int currentYear = cal.get(Calendar.YEAR);
            int idx = rules1.getEraIndex(currentYear, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DATE));
            if (idx != currentIdx) {
                errln("Current era index:" + currentIdx + " is different from era index of now:" + idx
                        + " in calendar:" + calId);
            }

            int eraStartYear = rules1.getStartYear(currentIdx);
            if (currentYear < eraStartYear) {
                errln("Current era's start year is after the current year in calendar:" + calId);
            }
        }
    }

    @Test
    public void testJapanese() {
        EraRules rules = EraRules.getInstance(CalType.JAPANESE, true);
        // Rules should have an era after Heisei
        int numRules = rules.getNumberOfEras();
        if (numRules <= JapaneseCalendar.HEISEI) {
            errln("Era after Heisei is not available.");
        }
        int postHeiseiStartYear = rules.getStartYear(JapaneseCalendar.HEISEI + 1);
        if (postHeiseiStartYear != 2019) {
            errln("Era after Heisei should start in 2019, but got " + postHeiseiStartYear);
        }
    }
}
