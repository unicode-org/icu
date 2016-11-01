/*
 *******************************************************************************
 * Copyright (C) 2013, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.samples.iuc;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/**
 * @author srl
 *
 */
public class Sample30_ResHello {
    public static void main(String... args) {
        ULocale locale = ULocale.getDefault();
        UResourceBundle bundle = 
                UResourceBundle.getBundleInstance(
                        Sample30_ResHello.class.getPackage().getName().replace('.', '/')+"/data/reshello",
                        locale,
                        Sample30_ResHello.class.getClassLoader());
        System.out.println(bundle.getString("hello"));
    }
}
