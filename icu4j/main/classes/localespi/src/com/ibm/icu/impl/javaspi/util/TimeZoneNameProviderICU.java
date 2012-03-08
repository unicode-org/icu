/*
 *******************************************************************************
 * Copyright (C) 2008-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.util;

import java.util.Locale;
import java.util.TimeZone;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.text.TimeZoneNames;
import com.ibm.icu.text.TimeZoneNames.NameType;

public class TimeZoneNameProviderICU extends java.util.spi.TimeZoneNameProvider {

    @Override
    public String getDisplayName(String ID, boolean daylight, int style, Locale locale) {
        String dispName = null;
        boolean[] isSystemID = new boolean[1];
        String canonicalID = com.ibm.icu.util.TimeZone.getCanonicalID(ID, isSystemID);
        if (isSystemID[0]) {
            long date = System.currentTimeMillis();
            TimeZoneNames tznames = TimeZoneNames.getInstance(ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale));
            switch (style) {
            case TimeZone.LONG:
                dispName = daylight ?
                        tznames.getDisplayName(canonicalID, NameType.LONG_DAYLIGHT, date) :
                        tznames.getDisplayName(canonicalID, NameType.LONG_STANDARD, date);
                break;
            case TimeZone.SHORT:
                dispName = daylight ?
                        tznames.getDisplayName(canonicalID, NameType.SHORT_DAYLIGHT, date) :
                        tznames.getDisplayName(canonicalID, NameType.SHORT_STANDARD, date);
                break;
            }
        }
        return dispName;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
