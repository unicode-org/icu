/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.util.Locale;
import java.util.TimeZone;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.ULocale;

public class TimeZoneNameTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new TimeZoneNameTest().run(args);
    }

    public void TestTimeZoneNames() {
        if (skipIfBeforeICU(4,0,0)) {
            // This test does not work well - likely caused by #6321
            return;
        }
        String[] tzids = TimeZone.getAvailableIDs();
        for (String tzid : tzids) {
            TimeZone tz = TimeZone.getTimeZone(tzid);
            com.ibm.icu.util.TimeZone tzIcu = com.ibm.icu.util.TimeZone.getTimeZone(tzid);
            for (Locale loc : Locale.getAvailableLocales()) {
                checkDisplayName(false, TimeZone.SHORT, tz, tzIcu, loc);
                checkDisplayName(true, TimeZone.SHORT, tz, tzIcu, loc);
                checkDisplayName(false, TimeZone.LONG, tz, tzIcu, loc);
                checkDisplayName(true, TimeZone.LONG, tz, tzIcu, loc);
            }
        }
    }

    private void checkDisplayName(boolean daylight, int style,
            TimeZone tz, com.ibm.icu.util.TimeZone icuTz, Locale loc) {
        ULocale uloc = ULocale.forLocale(loc);
        boolean shortStyle = (style == TimeZone.SHORT);
        String icuname = icuTz.getDisplayName(daylight,
                (shortStyle ? com.ibm.icu.util.TimeZone.SHORT : com.ibm.icu.util.TimeZone.LONG),
                uloc);

        int numDigits = 0;
        for (int i = 0; i < icuname.length(); i++) {
            if (UCharacter.isDigit(icuname.charAt(i))) {
                numDigits++;
            }
        }
        if (numDigits >= 3) {
            // ICU does not have the localized name
            return;
        }

        String name = tz.getDisplayName(daylight, style, loc);

        if (TestUtil.isICUExtendedLocale(loc)) {
            // The name should be taken from ICU
            if (!name.equals(icuname)) {
                errln("FAIL: TimeZone name by ICU is " + icuname + ", but got " + name
                        + " for time zone " + tz.getID() + " in locale " + loc
                        + " (daylight=" + daylight + ", style="
                        + (shortStyle ? "SHORT" : "LONG") + ")");
            }
        } else {
            if (!name.equals(icuname)) {
                logln("INFO: TimeZone name by ICU is " + icuname + ", but got " + name
                        + " for time zone " + tz.getID() + " in locale " + loc
                        + " (daylight=" + daylight + ", style="
                        + (shortStyle ? "SHORT" : "LONG") + ")");
            }
            // Try explicit ICU locale (xx_yy_ICU)
            Locale icuLoc = TestUtil.toICUExtendedLocale(loc);
            name = tz.getDisplayName(daylight, style, icuLoc);
            if (!name.equals(icuname)) {
                errln("FAIL: TimeZone name by ICU is " + icuname + ", but got " + name
                        + " for time zone " + tz.getID() + " in locale " + icuLoc
                        + " (daylight=" + daylight + ", style="
                        + (shortStyle ? "SHORT" : "LONG") + ")");
            }
        }
    }
}
