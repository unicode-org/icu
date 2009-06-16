/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.util;

import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.util.TimeZone;

public class TimeZoneNameProviderICU extends java.util.spi.TimeZoneNameProvider {

    @Override
    public String getDisplayName(String ID, boolean daylight, int style, Locale locale) {
        TimeZone tz = TimeZone.getTimeZone(ID);
        Locale actualLocale = ICULocaleServiceProvider.canonicalize(locale);
        String disp = tz.getDisplayName(daylight, style, actualLocale);
        if (disp.length() == 0) {
            return null;
        }
        // This is ugly hack, but no simple solution to check if
        // the localized name was picked up.
        int numDigits = 0;
        for (int i = 0; i < disp.length(); i++) {
            char c = disp.charAt(i);
            if (UCharacter.isDigit(c)) {
                numDigits++;
            }
        }
        // If there are more than 3 numbers, this code assume GMT format was used.
        if (numDigits >= 3) {
            return null;
        }

        if (daylight) {
            // ICU uses standard name for daylight name when the zone does not use
            // daylight saving time.

            // This is yet another ugly hack to support the JDK's behavior
            String stdDisp = tz.getDisplayName(false, style, actualLocale);
            if (disp.equals(stdDisp)) {
                return null;
            }
        }
        return disp;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
