/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.util;

import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocale;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.TimeZone;

public class TimeZoneNameProvider extends java.util.spi.TimeZoneNameProvider {

    @Override
    public String getDisplayName(String ID, boolean daylight, int style, Locale locale) {
        TimeZone tz = TimeZone.getTimeZone(ID);
        String disp = tz.getDisplayName(daylight, style, ICULocale.canonicalize(locale));
        if (disp.length() == 0) {
            return null;
        }
        // This is ugly hack, but no simple solution to check if
        // the localized name was picked up.
        int numDigits = 0;
        for (int i = 0; i < disp.length(); i++) {
            char c = disp.charAt(i);
            if (UCharacter.isDefined(c)) {
                numDigits++;
            }
        }
        // If there are more than 3 numbers, this code assume GMT format was used.
        if (numDigits >= 3) {
            return null;
        }
        return disp;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocale.getAvailableLocales();
    }

}
