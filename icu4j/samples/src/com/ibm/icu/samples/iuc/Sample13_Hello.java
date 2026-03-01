// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.iuc;

import java.util.Locale;

import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.util.ULocale;

public class Sample13_Hello {
    public static void main(String... args) {
        Locale locale = Locale.getDefault();
        String world = LocaleDisplayNames
                .getInstance(ULocale.forLocale(locale))
                .regionDisplayName("001");
        System.out.println("Hello, " + world + "\u2603");
    }
}
