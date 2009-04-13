/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.text;

import java.text.DateFormat;
import java.text.spi.DateFormatProvider;
import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU;

public class DateFormatProviderICU extends DateFormatProvider {

    private static final int NONE = -1;

    @Override
    public DateFormat getDateInstance(int style, Locale locale) {
        return getInstance(style, NONE, locale);
    }

    @Override
    public DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale locale) {
        return getInstance(dateStyle, timeStyle, locale);
    }

    @Override
    public DateFormat getTimeInstance(int style, Locale locale) {
        return getInstance(NONE, style, locale);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

    private DateFormat getInstance(int dstyle, int tstyle, Locale locale) {
        com.ibm.icu.text.DateFormat icuDfmt;
        Locale actual = ICULocaleServiceProvider.canonicalize(locale);
        if (dstyle == NONE) {
            icuDfmt = com.ibm.icu.text.DateFormat.getTimeInstance(tstyle, actual);
        } else if (tstyle == NONE) {
            icuDfmt = com.ibm.icu.text.DateFormat.getDateInstance(dstyle, actual);
        } else {
            icuDfmt = com.ibm.icu.text.DateFormat.getDateTimeInstance(dstyle, tstyle, actual);
        }
        if (!(icuDfmt instanceof com.ibm.icu.text.SimpleDateFormat)) {
            // icuDfmt must be always SimpleDateFormat
            return null;
        }

        com.ibm.icu.text.DecimalFormatSymbols decfs = ICULocaleServiceProvider.getDecimalFormatSymbolsForLocale(actual);
        if (decfs != null) {
            com.ibm.icu.text.NumberFormat icuNfmt = icuDfmt.getNumberFormat();
            if (icuNfmt instanceof com.ibm.icu.text.DecimalFormat) {
                ((com.ibm.icu.text.DecimalFormat)icuNfmt).setDecimalFormatSymbols(decfs);
            } else if (icuNfmt instanceof com.ibm.icu.impl.DateNumberFormat) {
                ((com.ibm.icu.impl.DateNumberFormat)icuNfmt).setZeroDigit(decfs.getDigit());
            }
            icuDfmt.setNumberFormat(icuNfmt);
        }

        return SimpleDateFormatICU.wrap((com.ibm.icu.text.SimpleDateFormat)icuDfmt);
    }
}
