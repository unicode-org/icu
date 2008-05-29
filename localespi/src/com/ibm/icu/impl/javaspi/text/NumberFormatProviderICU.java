/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.text;

import java.text.NumberFormat;
import java.text.spi.NumberFormatProvider;
import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocale;
import com.ibm.icu.impl.jdkadapter.DecimalFormatICU;

public class NumberFormatProviderICU extends NumberFormatProvider {

    @Override
    public NumberFormat getCurrencyInstance(Locale locale) {
        com.ibm.icu.text.NumberFormat icuNfmt = com.ibm.icu.text.NumberFormat.getCurrencyInstance(
                ICULocale.canonicalize(locale));
        if (!(icuNfmt instanceof com.ibm.icu.text.DecimalFormat)) {
            // icuNfmt must be always DecimalFormat
            return null;
        }
        return DecimalFormatICU.wrap((com.ibm.icu.text.DecimalFormat)icuNfmt);
    }

    @Override
    public NumberFormat getIntegerInstance(Locale locale) {
        com.ibm.icu.text.NumberFormat icuNfmt = com.ibm.icu.text.NumberFormat.getIntegerInstance(
                ICULocale.canonicalize(locale));
        if (!(icuNfmt instanceof com.ibm.icu.text.DecimalFormat)) {
            // icuNfmt must be always DecimalFormat
            return null;
        }
        return DecimalFormatICU.wrap((com.ibm.icu.text.DecimalFormat)icuNfmt);
    }

    @Override
    public NumberFormat getNumberInstance(Locale locale) {
        com.ibm.icu.text.NumberFormat icuNfmt = com.ibm.icu.text.NumberFormat.getNumberInstance(
                ICULocale.canonicalize(locale));
        if (!(icuNfmt instanceof com.ibm.icu.text.DecimalFormat)) {
            // icuNfmt must be always DecimalFormat
            return null;
        }
        return DecimalFormatICU.wrap((com.ibm.icu.text.DecimalFormat)icuNfmt);
    }

    @Override
    public NumberFormat getPercentInstance(Locale locale) {
        com.ibm.icu.text.NumberFormat icuNfmt = com.ibm.icu.text.NumberFormat.getPercentInstance(
                ICULocale.canonicalize(locale));
        if (!(icuNfmt instanceof com.ibm.icu.text.DecimalFormat)) {
            // icuNfmt must be always DecimalFormat
            return null;
        }
        return DecimalFormatICU.wrap((com.ibm.icu.text.DecimalFormat)icuNfmt);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocale.getAvailableLocales();
    }

}
