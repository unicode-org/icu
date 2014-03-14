/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.iuc;

import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.util.ULocale;

public class Sample13_Hello {
    public static void main(String... args) {
        String world = LocaleDisplayNames
                .getInstance(ULocale.getDefault()).regionDisplayName("001");
        System.out.println("Hello, " + world + "\u2603");
    }
}
