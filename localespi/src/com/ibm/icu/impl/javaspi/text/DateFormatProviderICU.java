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

import com.ibm.icu.impl.javaspi.ICULocale;
import com.ibm.icu.impl.jdkadapter.SimpleDateFormatICU;

public class DateFormatProviderICU extends DateFormatProvider {

    @Override
    public DateFormat getDateInstance(int style, Locale locale) {
        com.ibm.icu.text.DateFormat icuDfmt = com.ibm.icu.text.DateFormat.getDateInstance(
                style, ICULocale.canonicalize(locale));
        if (!(icuDfmt instanceof com.ibm.icu.text.SimpleDateFormat)) {
            // icuDfmt must be always SimpleDateFormat
            return null;
        }
        return SimpleDateFormatICU.wrap((com.ibm.icu.text.SimpleDateFormat)icuDfmt);
    }

    @Override
    public DateFormat getDateTimeInstance(int dateStyle, int timeStyle, Locale locale) {
        com.ibm.icu.text.DateFormat icuDfmt = com.ibm.icu.text.DateFormat.getDateTimeInstance(
                dateStyle, timeStyle, ICULocale.canonicalize(locale));
        if (!(icuDfmt instanceof com.ibm.icu.text.SimpleDateFormat)) {
            // icuDfmt must be always SimpleDateFormat
            return null;
        }
        return SimpleDateFormatICU.wrap((com.ibm.icu.text.SimpleDateFormat)icuDfmt);
    }

    @Override
    public DateFormat getTimeInstance(int style, Locale locale) {
        com.ibm.icu.text.DateFormat icuDfmt = com.ibm.icu.text.DateFormat.getTimeInstance(
                style, ICULocale.canonicalize(locale));
        if (!(icuDfmt instanceof com.ibm.icu.text.SimpleDateFormat)) {
            // icuDfmt must be always SimpleDateFormat
            return null;
        }
        return SimpleDateFormatICU.wrap((com.ibm.icu.text.SimpleDateFormat)icuDfmt);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocale.getAvailableLocales();
    }
}
