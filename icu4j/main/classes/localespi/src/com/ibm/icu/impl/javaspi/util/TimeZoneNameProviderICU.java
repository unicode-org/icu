// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
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

            {
                // Workaround for Java bug. Java needs all 4 names available at the same time.
                // 2012-10-09 yoshito
                String lstd = tznames.getDisplayName(canonicalID, NameType.LONG_STANDARD, date);
                String ldst = tznames.getDisplayName(canonicalID, NameType.LONG_DAYLIGHT, date);
                String sstd = tznames.getDisplayName(canonicalID, NameType.SHORT_STANDARD, date);
                String sdst = tznames.getDisplayName(canonicalID, NameType.SHORT_DAYLIGHT, date);

                if (lstd != null && ldst != null && sstd != null && sdst != null) {
                    switch (style) {
                    case TimeZone.LONG:
                        dispName = daylight ? ldst : lstd;
                        break;
                    case TimeZone.SHORT:
                        dispName = daylight ? sdst : sstd;
                        break;
                    }
                }
            }

//            {
//                switch (style) {
//                case TimeZone.LONG:
//                    dispName = daylight ?
//                            tznames.getDisplayName(canonicalID, NameType.LONG_DAYLIGHT, date) :
//                            tznames.getDisplayName(canonicalID, NameType.LONG_STANDARD, date);
//                    break;
//                case TimeZone.SHORT:
//                    dispName = daylight ?
//                            tznames.getDisplayName(canonicalID, NameType.SHORT_DAYLIGHT, date) :
//                            tznames.getDisplayName(canonicalID, NameType.SHORT_STANDARD, date);
//                    break;
//                }
//            }
        }
        return dispName;
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
