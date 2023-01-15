// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Builder;

public class TestUtil {

    static final String ICU_VARIANT = "ICU4J";
    private static final String ICU_VARIANT_SUFFIX = "_ICU4J";

    public static Locale toICUExtendedLocale(Locale locale) {
        if (isICUExtendedLocale(locale)) {
            return locale;
        }

        String variant = locale.getVariant();
        variant = variant.length() == 0 ? ICU_VARIANT : variant + ICU_VARIANT_SUFFIX;

        // We once convert Locale to ULocale, then update variant
        // field. We could do this using Locale APIs, but have to
        // use a lot of reflections, because the test code should
        // also run on JRE 6.
        ULocale uloc = ULocale.forLocale(locale);
        if (uloc.getScript().length() == 0) {
            return new Locale(locale.getLanguage(), locale.getCountry(), variant);
        }

        // For preserving JDK Locale's script, we cannot use
        // the regular Locale constructor.
        ULocale modUloc = null;
        Builder locBld = new Builder();
        try {
            locBld.setLocale(uloc);
            locBld.setVariant(variant);
            modUloc = locBld.build();
            return modUloc.toLocale();
        } catch (Exception e) {
            // hmm, it should not happen
            throw new RuntimeException(e);
        }
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

    private static final Set<Locale> ICU_LOCALES = new HashSet<>();

    static {
        ULocale[] icuULocales = ULocale.getAvailableLocales();
        for (ULocale icuULoc : icuULocales) {
            Locale jdkLoc = icuULoc.toLocale();
            // Make sure nothing lost
            ULocale uloc = ULocale.forLocale(jdkLoc);
            if (icuULoc.equals(uloc)) {
                ICU_LOCALES.add(jdkLoc);
            }
        }
    }

    /*
     * Checks if the given locale is excluded from locale SPI test
     */
    public static boolean isExcluded(Locale loc) {
        if (Locale.ROOT.equals(loc)) {
            return true;
        }
        if (isICUExtendedLocale(loc)) {
            return false;
        }
        return !ICU_LOCALES.contains(loc);
    }
}
