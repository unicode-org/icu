/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.javaspi.text;

import java.text.BreakIterator;
import java.text.spi.BreakIteratorProvider;
import java.util.Locale;

import com.ibm.icu.impl.javaspi.ICULocaleServiceProvider;
import com.ibm.icu.impl.jdkadapter.BreakIteratorICU;

public class BreakIteratorProviderICU extends BreakIteratorProvider {

    @Override
    public BreakIterator getCharacterInstance(Locale locale) {
        com.ibm.icu.text.BreakIterator icuBrkItr = com.ibm.icu.text.BreakIterator.getCharacterInstance(
                ICULocaleServiceProvider.canonicalize(locale));
        return BreakIteratorICU.wrap(icuBrkItr);
    }

    @Override
    public BreakIterator getLineInstance(Locale locale) {
        com.ibm.icu.text.BreakIterator icuBrkItr = com.ibm.icu.text.BreakIterator.getLineInstance(
                ICULocaleServiceProvider.canonicalize(locale));
        return BreakIteratorICU.wrap(icuBrkItr);
    }

    @Override
    public BreakIterator getSentenceInstance(Locale locale) {
        com.ibm.icu.text.BreakIterator icuBrkItr = com.ibm.icu.text.BreakIterator.getSentenceInstance(
                ICULocaleServiceProvider.canonicalize(locale));
        return BreakIteratorICU.wrap(icuBrkItr);
    }

    @Override
    public BreakIterator getWordInstance(Locale locale) {
        com.ibm.icu.text.BreakIterator icuBrkItr = com.ibm.icu.text.BreakIterator.getWordInstance(
                ICULocaleServiceProvider.canonicalize(locale));
        return BreakIteratorICU.wrap(icuBrkItr);
    }

    @Override
    public Locale[] getAvailableLocales() {
        return ICULocaleServiceProvider.getAvailableLocales();
    }

}
