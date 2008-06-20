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
    private static final boolean IBMJRE;
    static {
        String javaVendor = System.getProperty("java.vendor");
        if (javaVendor != null && javaVendor.indexOf("IBM") >= 0) {
            IBMJRE = true;
        } else {
            IBMJRE = false;
        }
    }

    public static boolean isProblematicIBMLocale(Locale loc) {
        if (!IBMJRE) {
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
