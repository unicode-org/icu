/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

public class LocaleNameTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new LocaleNameTest().run(args);
    }

    public void TestLanguageNames() {
        Locale[] locales = Locale.getAvailableLocales();
        StringBuffer icuid = new StringBuffer();
        for (Locale inLocale : locales) {
            if (TestUtil.isProblematicIBMLocale(inLocale)) {
                logln("Skipped " + inLocale);
                continue;
            }

            ULocale inULocale = ULocale.forLocale(inLocale);
            Locale inLocaleICU = TestUtil.toICUExtendedLocale(inLocale);
            for (Locale forLocale : locales) {
                if (forLocale.getLanguage().length() == 0) {
                    continue;
                }
                icuid.setLength(0);
                icuid.append(forLocale.getLanguage());
                String country = forLocale.getCountry();
                String variant = forLocale.getVariant();
                if (country.length() != 0) {
                    icuid.append("_");
                    icuid.append(country);
                }
                if (variant.length() != 0) {
                    if (country.length() == 0) {
                        icuid.append("_");
                    }
                    icuid.append("_");
                    icuid.append(variant);
                }
                ULocale forULocale = new ULocale(icuid.toString());
                String icuname = ULocale.getDisplayLanguage(forULocale.getLanguage(), inULocale);
                if (icuname.equals(forULocale.getLanguage()) || icuname.length() == 0) {
                    continue;
                }

                String name = forLocale.getDisplayLanguage(inLocale);
                if (TestUtil.isICUExtendedLocale(inLocale)) {
                    // The name should be taken from ICU
                    if (!name.equals(icuname)) {
                        errln("FAIL: Language name by ICU is " + icuname + ", but got " + name
                                + " for locale " + forLocale + " in locale " + inLocale);
                    }
                } else {
                    if (!name.equals(icuname)) {
                        logln("INFO: Language name by JDK is " + name + ", but " + icuname + 
                              " by ICU, for locale " + forLocale + " in locale " + inLocale);
                    }
                    // Try explicit ICU locale (xx_yy_ICU)
                    name = forLocale.getDisplayLanguage(inLocaleICU);
                    if (!name.equals(icuname)) {
                        errln("FAIL: Language name by ICU is " + icuname + ", but got " + name
                              + " for locale " + forLocale + " in locale " + inLocaleICU);
                    }
                }
            }
        }
    }

    public void TestCountryNames() {
        Locale[] locales = Locale.getAvailableLocales();
        for (Locale inLocale : locales) {
            if (TestUtil.isProblematicIBMLocale(inLocale)) {
                logln("Skipped " + inLocale);
                continue;
            }

            ULocale inULocale = ULocale.forLocale(inLocale);
            Locale inLocaleICU = TestUtil.toICUExtendedLocale(inLocale);
            for (Locale forLocale : locales) {
                if (forLocale.getCountry().length() == 0) {
                    continue;
                }
                // ULocale#forLocale preserves country always
                ULocale forULocale = ULocale.forLocale(forLocale);
                String icuname = ULocale.getDisplayCountry(forULocale.getCountry(), inULocale);
                if (icuname.equals(forULocale.getCountry()) || icuname.length() == 0) {
                    continue;
                }

                String name = forLocale.getDisplayCountry(inLocale);
                if (TestUtil.isICUExtendedLocale(inLocale)) {
                    // The name should be taken from ICU
                    if (!name.equals(icuname)) {
                        errln("FAIL: Country name by ICU is " + icuname + ", but got " + name
                                + " for locale " + forLocale + " in locale " + inLocale);
                    }
                } else {
                    // The name might be taken from JDK
                    if (!name.equals(icuname)) {
                        logln("INFO: Country name by JDK is " + name + ", but " + icuname + 
                              " in ICU, for locale " + forLocale + " in locale " + inLocale);
                    }
                    // Try explicit ICU locale (xx_yy_ICU)
                    name = forLocale.getDisplayCountry(inLocaleICU);
                    if (!name.equals(icuname)) {
                        errln("FAIL: Country name by ICU is " + icuname + ", but got " + name
                              + " for locale " + forLocale + " in locale " + inLocaleICU);
                    }
                }
            }
        }
    }

    public void TestVariantNames() {
        Locale[] locales = Locale.getAvailableLocales();
        StringBuffer icuid = new StringBuffer();
        for (Locale inLocale : locales) {
            if (TestUtil.isProblematicIBMLocale(inLocale)) {
                logln("Skipped " + inLocale);
                continue;
            }

            ULocale inULocale = ULocale.forLocale(inLocale);
            Locale inLocaleICU = TestUtil.toICUExtendedLocale(inLocale);
            for (Locale forLocale : locales) {
                if (forLocale.getVariant().length() == 0) {
                    continue;
                }
                icuid.setLength(0);
                icuid.append(forLocale.getLanguage());
                String country = forLocale.getCountry();
                String variant = forLocale.getVariant();
                if (country.length() != 0) {
                    icuid.append("_");
                    icuid.append(country);
                }
                if (variant.length() != 0) {
                    if (country.length() == 0) {
                        icuid.append("_");
                    }
                    icuid.append("_");
                    icuid.append(variant);
                }
                ULocale forULocale = new ULocale(icuid.toString());
                String icuname = ULocale.getDisplayVariant(forULocale.getVariant(), inULocale);
                if (icuname.equals(forULocale.getVariant()) || icuname.length() == 0) {
                    continue;
                }

                String name = forLocale.getDisplayVariant(inLocale);
                if (TestUtil.isICUExtendedLocale(inLocale)) {
                    // The name should be taken from ICU
                    if (!name.equals(icuname)) {
                        errln("FAIL: Variant name by ICU is " + icuname + ", but got " + name
                                + " for locale " + forLocale + " in locale " + inLocale);
                    }
                } else {
                    if (!name.equals(icuname)) {
                        logln("INFO: Variant name by JDK is " + name + ", but " + icuname + 
                              " in ICU, for locale " + forLocale + " in locale " + inLocale);
                    }
                    // Try explicit ICU locale (xx_yy_ICU)
                    name = forLocale.getDisplayVariant(inLocaleICU);
                    if (!name.equals(icuname)) {
                        errln("FAIL: Variant name by ICU is " + icuname + ", but got " + name
                              + " for locale " + forLocale + " in locale " + inLocaleICU);
                    }
                }
            }
        }
    }
}
