// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.CalType;
import com.ibm.icu.impl.EraRules;
import com.ibm.icu.impl.EraRules.RuleDate;
import com.ibm.icu.util.JapaneseCalendar;

/**
 * Tests for EraRules class
 */
@RunWith(JUnit4.class)
public class EraRulesTest extends TestFmwk {
    @Test
    public void TestAPIs() {
        for (CalType calType : CalType.values()) {
            String calId = calType.getId();
            if (calId.equals("iso8601") || calId.equals("unknown")) {
                continue;
            }
            EraRules rules = EraRules.getInstance(calType);
            if (rules == null) {
                errln("Era rules for " + calId + " is not available.");
            }
            int numEras = rules.getNumberOfEras();
            if (numEras <= 0) {
                errln("Number of era rules for " + calId + " is " + numEras);
            }
            RuleDate prevEnd = null;
            for (int i = 0; i < numEras; i++) {
                RuleDate start = rules.getStartDate(i);
                if (prevEnd != null) {
                    int cmp = prevEnd.compareTo(start);
                    if (cmp >= 0) {
                        errln("Start date of rule " + i + " for " + calId
                                + " is before the end date of previous rule");
                    }
                }
                prevEnd = start;
            }
        }
    }

    @Test
    public void TestJapanese() {
        EraRules rules = EraRules.getInstance(CalType.JAPANESE, true);
        // Rules should have an era after Heisei
        int numRules = rules.getNumberOfEras();
        if (numRules <= JapaneseCalendar.HEISEI) {
            errln("Era after Heisei is not available.");
        }
        RuleDate ruleDate = rules.getStartDate(JapaneseCalendar.HEISEI + 1);
        if (ruleDate.getYear() != 2019) {
            errln("Era after Heisei should start in 2019, but got " + ruleDate.getYear());
        }
    }
}
