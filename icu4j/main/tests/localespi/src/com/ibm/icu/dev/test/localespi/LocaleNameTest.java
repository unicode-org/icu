// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.localespi;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class LocaleNameTest extends TestFmwk {
    private static final Method GETDISPLAYSCRIPT_METHOD;

    static {
        Method mGetDisplayScript = null;
        try {
            mGetDisplayScript = Locale.class.getMethod("getDisplayScript", new Class[] {Locale.class});
        } catch (Exception e) {
            // fall through
        }
        GETDISPLAYSCRIPT_METHOD = mGetDisplayScript;
    }

    @Test
    public void TestLanguageNames() {
        Locale[] locales = Locale.getAvailableLocales();
        StringBuffer icuid = new StringBuffer();
        for (Locale inLocale : locales) {
            if (TestUtil.isExcluded(inLocale)) {
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

    @Test
    public void TestScriptNames() {
        if (GETDISPLAYSCRIPT_METHOD == null) {
            logln("INFO: Locale#getDisplayScript(Locale) is not available.");
            return;
        }

        Locale[] locales = Locale.getAvailableLocales();
        for (Locale inLocale : locales) {
            if (TestUtil.isExcluded(inLocale)) {
                logln("Skipped " + inLocale);
                continue;
            }

            ULocale inULocale = ULocale.forLocale(inLocale);
            Locale inLocaleICU = TestUtil.toICUExtendedLocale(inLocale);
            for (ULocale forULocale : ULocale.getAvailableLocales()) {
                if (forULocale.getScript().length() == 0) {
                    continue;
                }
                Locale forLocale = forULocale.toLocale();
                String icuname = forULocale.getDisplayScript(inULocale);
                if (icuname.equals(forULocale.getScript()) || icuname.length() == 0) {
                    continue;
                }

                String name = null;
                try {
                    name = (String)GETDISPLAYSCRIPT_METHOD.invoke(forLocale, new Object[] {inLocale});
                } catch (Exception e) {
                    errln("FAIL: JDK Locale#getDisplayScript(\"" + inLocale + "\") throws exception: " + e.getMessage());
                    continue;
                }

                if (TestUtil.isICUExtendedLocale(inLocale)) {
                    // The name should be taken from ICU
                    if (!name.equals(icuname)) {
                        errln("FAIL: Script name by ICU is " + icuname + ", but got " + name
                                + " for locale " + forLocale + " in locale " + inLocale);
                    }
                } else {
                    // The name might be taken from JDK
                    if (!name.equals(icuname)) {
                        logln("INFO: Script name by JDK is " + name + ", but " + icuname +
                                " in ICU, for locale " + forLocale + " in locale " + inLocale);
                    }
                    // Try explicit ICU locale (xx_yy_ICU)
                    try {
                        name = (String)GETDISPLAYSCRIPT_METHOD.invoke(forLocale, new Object[] {inLocaleICU});
                    } catch (Exception e) {
                        errln("FAIL: JDK Locale#getDisplayScript(\"" + inLocaleICU + "\") throws exception: " + e.getMessage());
                        continue;
                    }
                    if (!name.equals(icuname)) {
                        errln("FAIL: Script name by ICU is " + icuname + ", but got " + name
                                + " for locale " + forLocale + " in locale " + inLocaleICU);
                    }
                }
            }
        }
    }

    @Test
    public void TestCountryNames() {
        Locale[] locales = Locale.getAvailableLocales();
        for (Locale inLocale : locales) {
            if (TestUtil.isExcluded(inLocale)) {
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
                String icuname = forULocale.getDisplayCountry(inULocale);
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

    @Test
    public void TestVariantNames() {
        Set<Locale> locales = new HashSet<Locale>();
        for (Locale l : Locale.getAvailableLocales()) {
            locales.add(l);
        }
        // Add some locales with variant
        final Locale[] additionalLocales = {
            new Locale("fr", "FR", "1694acad"),
            new Locale("de", "DE", "1901"),
            new Locale("en", "US", "boont"),
            new Locale("el", "GR", "monoton"),
        };
        for (Locale l : additionalLocales) {
            locales.add(l);
        }

        for (Locale inLocale : locales) {
            if (TestUtil.isExcluded(inLocale)) {
                logln("Skipped " + inLocale);
                continue;
            }

            ULocale inULocale = ULocale.forLocale(inLocale);
            Locale inLocaleICU = TestUtil.toICUExtendedLocale(inLocale);
            for (Locale forLocale : locales) {
                String locVar = forLocale.getVariant();
                if (locVar.length() == 0) {
                    continue;
                }
                // Note: JDK resolves a display name for each variant subtag
                String[] locVarSubtags = locVar.split("_");

                for (String locSingleVar : locVarSubtags) {
                    if (locSingleVar.equals(TestUtil.ICU_VARIANT)
                            || locSingleVar.equals("Cyrl") || locSingleVar.equals("Latn")) { // IBM Java 6 has locales with 'variant' Cryl/Latn
                        continue;
                    }
                    Locale forLocaleSingleVar = new Locale(forLocale.getLanguage(), forLocale.getCountry(), locSingleVar);
                    ULocale forULocaleSingleVar = new ULocale("und_ZZ_" + locSingleVar);
                    String icuname = forULocaleSingleVar.getDisplayVariant(inULocale);
                    if (icuname.equals(locSingleVar) || icuname.length() == 0) {
                        continue;
                    }

                    String name = forLocaleSingleVar.getDisplayVariant(inLocale);
                    if (name.equalsIgnoreCase(locSingleVar)) {
                        // ICU does not have any localized display name.
                        // Note: ICU turns variant to upper case string, while Java does not.
                        continue;
                    }
                    if (TestUtil.isICUExtendedLocale(inLocale)) {
                        // The name should be taken from ICU
                        if (!name.equals(icuname)) {
                            errln("FAIL: Variant name by ICU is " + icuname + ", but got " + name
                                    + " for locale " + forLocaleSingleVar + " in locale " + inLocale);
                        }
                    } else {
                        if (!name.equals(icuname)) {
                            logln("INFO: Variant name by JDK is " + name + ", but " + icuname +
                                  " in ICU, for locale " + forLocaleSingleVar + " in locale " + inLocale);
                        }
                        // Try explicit ICU locale (xx_yy_ICU)
                        name = forLocaleSingleVar.getDisplayVariant(inLocaleICU);
                        if (!name.equals(icuname)) {
                            errln("FAIL: Variant name by ICU is " + icuname + ", but got " + name
                                  + " for locale " + forLocaleSingleVar + " in locale " + inLocaleICU);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void Test20639_DeprecatesISO3Language() {
        String[][] cases = new String[][]{
            {"nb", "nob"},
            {"no", "nor"}, // why not nob?
            {"he", "heb"},
            {"iw", "heb"},
            {"ro", "ron"},
            {"mo", "mol"},
        };
        for (String[] cas : cases) {
            ULocale loc = new ULocale(cas[0]);
            String actual = loc.getISO3Language();
            assertEquals(cas[0], cas[1], actual);
        }
    }
}
