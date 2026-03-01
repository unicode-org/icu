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

    private static final boolean SUNJRE;
    private static final boolean IBMJRE;

    static {
        String javaVendor = System.getProperty("java.vendor");
        if (javaVendor != null) {
            if (javaVendor.indexOf("Sun") >= 0) {
                SUNJRE = true;
                IBMJRE = false;
            } else if (javaVendor.indexOf("IBM") >= 0) {
                SUNJRE = false;
                IBMJRE = true;
            } else {
                SUNJRE = false;
                IBMJRE = false;
            }
        } else {
            SUNJRE = false;
            IBMJRE = false;
        }
    }

    public static boolean isSUNJRE() {
        return SUNJRE;
    }
    public static boolean isIBMJRE() {
        return IBMJRE;
    }

    private static final Set<Locale> EXCLUDED_LOCALES = new HashSet<Locale>();
    static {
        EXCLUDED_LOCALES.add(Locale.ROOT);
        // de-GR is supported by Java 8, but not supported by CLDR / ICU
        EXCLUDED_LOCALES.add(new Locale("de", "GR"));
    }

    /*
     * Checks if the given locale is excluded from locale SPI test
     */
    public static boolean isExcluded(Locale loc) {
        if (EXCLUDED_LOCALES.contains(loc)) {
            return true;
        }
        return isProblematicIBMLocale(loc);
    }

    /*
     * Ticket#6368
     * 
     * The ICU4J locale spi test cases reports many errors on IBM Java 6. There are two kinds
     * of problems observed and both of them look like implementation problems in IBM Java 6.
     * 
     * - When a locale has variant field (for example, sr_RS_Cyrl, de_DE_PREEURO), adding ICU
     *   suffix in the variant field (for example, sr_RS_Cyrl_ICU, de_DE_PREEURO_ICU) has no effects.
     *   For these locales, IBM JRE 6 ignores installed Locale providers.
     *   
     * - For "sh" sublocales with "ICU" variant (for example, sh__ICU, sh_CS_ICU), IBM JRE 6 also
     *   ignores installed ICU locale providers. Probably, "sh" is internally mapped to "sr_RS_Cyrl"
     *   internally before locale look up.
     * 
     * For now, we exclude these problematic locales from locale spi test cases on IBM Java 6.
     */
    public static boolean isProblematicIBMLocale(Locale loc) {
        if (!isIBMJRE()) {
            return false;
        }
        if (loc.getLanguage().equals("sh")) {
            return true;
        }
        String variant = loc.getVariant();
        if (variant.startsWith("EURO") || variant.startsWith("PREEURO")
                || variant.startsWith("Cyrl") || variant.startsWith("Latn")) {
            return true;
        }
        return false;
    }
}
