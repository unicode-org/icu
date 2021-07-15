// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.ime.indic;

import java.awt.Image;
import java.awt.im.spi.InputMethod;
import java.awt.im.spi.InputMethodDescriptor;
import java.util.Locale;
import java.util.ResourceBundle;

public abstract class IndicIMDescriptor implements InputMethodDescriptor {
    private final Locale locale;
    private final String name;

    protected IndicIMDescriptor(Locale locale, String name) {
    this.locale = locale;
    this.name = name;
    }

    protected abstract IndicInputMethodImpl getImpl();

    public Locale[] getAvailableLocales() {
        return new Locale[] { locale };
    }

    public boolean hasDynamicLocaleList() {
        return false;
    }

    public synchronized String getInputMethodDisplayName(Locale inputLocale, Locale displayLanguage) {
    try {
        ResourceBundle rb = ResourceBundle.getBundle("com.ibm.icu.dev.tool.ime.indic.DisplayNames", 
                             displayLanguage);
        return rb.getString("DisplayName." + name);
    }
    catch (Throwable t) {
        return name;
    }
    }

    public Image getInputMethodIcon(Locale inputLocale) {
        return null;
    }

    public InputMethod createInputMethod() throws Exception {
        return new IndicInputMethod(locale, getImpl());
    }

    public String toString() {
    return name;
    }
}
