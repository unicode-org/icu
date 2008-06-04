/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.ibm.icu.impl.ICUResourceBundle;

public class TestUtil {

    private static final Locale[] JDK_LOCALES = Locale.getAvailableLocales();
    private static final Locale[] ICU_LOCALES = ICUResourceBundle.getAvailableLocales();
    private static final Locale[] ICU_ONLY_LOCALES;
    private static final Set<Locale> JDK_LOCALE_SET = new HashSet<Locale>();
    private static final Set<Locale> ICU_LOCALE_SET = new HashSet<Locale>();
    private static final Set<Locale> ICU_ONLY_LOCALE_SET = new HashSet<Locale>();

    private static final String ICU_VARIANT = "ICU";
    private static final String ICU_VARIANT_SUFFIX = "_ICU";

    static {
        for (Locale jdkloc : JDK_LOCALES) {
            JDK_LOCALE_SET.add(jdkloc);
        }

        for (Locale iculoc : ICU_LOCALES) {
            ICU_LOCALE_SET.add(iculoc);
            if (!JDK_LOCALE_SET.contains(iculoc)) {
                ICU_ONLY_LOCALE_SET.add(iculoc);
            }
        }
        ICU_ONLY_LOCALES = ICU_ONLY_LOCALE_SET.toArray(new Locale[0]);
    }

    public static boolean isICUOnly(Locale locale) {
        return ICU_ONLY_LOCALE_SET.contains(locale);
    }

    public static Locale[] getICUOnlyLocales() {
        return Arrays.copyOf(ICU_ONLY_LOCALES, ICU_ONLY_LOCALES.length);
    }

    public static Locale[] getJDKLocales() {
        return Arrays.copyOf(JDK_LOCALES, JDK_LOCALES.length);
    }

    public static Locale[] getICULocales() {
        return Arrays.copyOf(ICU_LOCALES, ICU_LOCALES.length);
    }

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
}
