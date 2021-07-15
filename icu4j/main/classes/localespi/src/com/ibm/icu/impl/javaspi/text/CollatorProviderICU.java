// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.text;

import java.text.Collator;
import java.text.spi.CollatorProvider;
import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.impl.jdkadapter.CollatorICU;

public class CollatorProviderICU extends CollatorProvider {

    @Override
    public Collator getInstance(Locale locale) {
        com.ibm.icu.text.Collator icuCollator = com.ibm.icu.text.Collator.getInstance(
                ICULocaleServiceProvider.toULocaleNoSpecialVariant(locale));
        return CollatorICU.wrap(icuCollator);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
