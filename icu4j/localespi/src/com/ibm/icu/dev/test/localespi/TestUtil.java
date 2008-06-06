/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.util.Locale;

public class TestUtil {

    private static final String ICU_VARIANT = "ICU";
    private static final String ICU_VARIANT_SUFFIX = "_ICU";

    public static Locale toICUExtendedLocale(Locale locale) {
        if (isICUExtendedLocale(locale)) {
            return locale;
        }
        String variant = locale.getVariant();
        variant = variant.length() == 0 ? ICU_VARIANT : variant + ICU_VARIANT_SUFFIX;
        return new Locale(locale.getLanguage(), locale.getCountry(), variant);
    }

    public static boolean isICUExtendedLocale(Locale locale) {
        String variant = locale.getVariant();
        if (variant.equals(ICU_VARIANT) || variant.endsWith(ICU_VARIANT_SUFFIX)) {
            return true;
        }
        return false;
    }

    public static boolean equals(Object o1, Object o2) {
        if (o1 == null && o2 == null) {
            return true;
        }
        if (o1 == null || o2 == null) {
            return false;
        }
        return o1.equals(o2);
    }
}
