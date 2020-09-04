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

import com.ibm.icu.util.UResourceBundle;

/**
 * @author srl
 *
 */
public class Sample30_ResHello {
    public static void main(String... args) {
        Locale locale = Locale.getDefault();
        UResourceBundle bundle = 
                UResourceBundle.getBundleInstance(
                        Sample30_ResHello.class.getPackage().getName().replace('.', '/')+"/data/reshello",
                        locale,
                        Sample30_ResHello.class.getClassLoader());
        System.out.println(bundle.getString("hello"));
    }
}
