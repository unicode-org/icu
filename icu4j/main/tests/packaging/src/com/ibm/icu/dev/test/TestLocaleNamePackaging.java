// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2009-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import static com.ibm.icu.impl.LocaleDisplayNamesImpl.DataTableType.LANG;
import static com.ibm.icu.impl.LocaleDisplayNamesImpl.DataTableType.REGION;

import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.impl.LocaleDisplayNamesImpl;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.LocaleDisplayNames.DialectHandling;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class TestLocaleNamePackaging extends TestFmwk {
    public TestLocaleNamePackaging() {
    }

    public boolean validate() {
        logln("language data: " + LocaleDisplayNamesImpl.haveData(LANG));
        logln("  region data: " + LocaleDisplayNamesImpl.haveData(REGION));
        return true;
    }

    private static ULocale[] locales = {
        ULocale.ROOT, ULocale.US, new ULocale("es_ES"), ULocale.GERMANY,
        new ULocale("und_TH")
    };

    // Java Locales equivalent to above
    private static Locale[] javaLocales = {
        new Locale(""), Locale.US, new Locale("es", "ES"), Locale.GERMANY,
        new Locale("und", "TH")
    };

    @Test
    public void testRegionDisplayNames() {
        String[] expectedWithRegionData = {
            "",
            "US",
            "ES",
            "DE",
            "TH",
            "",
            "United States",
            "Spain",
            "Germany",
            "Thailand",
            "",
            "Estados Unidos",
            "Espa\u00f1a",
            "Alemania",
            "Tailandia",
            "",
            "Vereinigte Staaten",
            "Spanien",
            "Deutschland",
            "Thailand",
            "",
            "US",
            "ES",
            "DE",
            "TH",
        };
        String[] expectedWithoutRegionData = {
            "",
            "US",
            "ES",
            "DE",
            "TH",
        };
        String[] expected = LocaleDisplayNamesImpl.haveData(REGION) ?
            expectedWithRegionData : expectedWithoutRegionData;

        int n = 0;
        for (ULocale displayLocale : locales) {
            LocaleDisplayNames dn = LocaleDisplayNames.getInstance(displayLocale);
            for (ULocale targetLocale : locales) {
                String result = dn.regionDisplayName(targetLocale.getCountry());
                assertEquals(targetLocale + " in " + displayLocale, expected[n++], result);
                if (n == expected.length) {
                    n = 0;
                }
            }
        }

        // Same test with Java Locale
        n = 0;
        for (Locale displayJavaLocale : javaLocales) {
            LocaleDisplayNames dn = LocaleDisplayNames.getInstance(displayJavaLocale);
            for (Locale targetLocale : javaLocales) {
                String result = dn.regionDisplayName(targetLocale.getCountry());
                assertEquals(targetLocale + " in " + displayJavaLocale, expected[n++], result);
                if (n == expected.length) {
                    n = 0;
                }
            }
        }

    }

    @Test
    public void testLanguageDisplayNames() {
        String[] expectedWithLanguageData = {
            "",
            "en",
            "es",
            "de",
            "",
            "",
            "English",
            "Spanish",
            "German",
            "",
            "",
            "ingl\u00E9s",
            "espa\u00F1ol",
            "alem\u00E1n",
            "",
            "",
            "Englisch",
            "Spanisch",
            "Deutsch",
            "",
            "",
            "en",
            "es",
            "de",
            "",
        };
        String[] expectedWithoutLanguageData = {
            "",
            "en",
            "es",
            "de",
            "",
        };
        String[] expected = LocaleDisplayNamesImpl.haveData(LANG) ?
            expectedWithLanguageData : expectedWithoutLanguageData;

        int n = 0;
        for (ULocale displayLocale : locales) {
            LocaleDisplayNames dn = LocaleDisplayNames.getInstance(displayLocale);
            for (ULocale targetLocale : locales) {
                String result = dn.languageDisplayName(targetLocale.getLanguage());
                assertEquals(targetLocale + " in " + displayLocale, expected[n++], result);
                if (n == expected.length) {
                    n = 0;
                }
            }
        }

        // Same test with Java Locale
        n = 0;
        for (Locale displayJavaLocale : javaLocales) {
            LocaleDisplayNames dn = LocaleDisplayNames.getInstance(displayJavaLocale);
            for (Locale targetLocale : javaLocales) {
                // ICU-20273: ICU and Java handle "und" differently, skip those test cases.
                if (!"und".equals(targetLocale.getLanguage())) {
                    String result = dn.languageDisplayName(targetLocale.getLanguage());
                    assertEquals(targetLocale + " in " + displayJavaLocale, expected[n], result);
                }
                if (++n == expected.length) {
                    n = 0;
                }
            }
        }

    }

    // test a 'root' locale, with keywords
    @Test
    public void testLocaleDisplayNameWithKeywords() {
        String[] expectedWithLanguageData = {
            "und (collation=phonebook)",
            "Unknown language (Phonebook Sort Order)",
            "lengua desconocida (orden de list\u00EDn telef\u00F3nico)",
            "Unbekannte Sprache (Telefonbuch-Sortierung)",
            "und (collation=phonebook)",
        };
        String[] expectedWithoutLanguageData = {
            "und (collation=phonebook)",
        };
        String[] expected = LocaleDisplayNamesImpl.haveData(LANG) ?
            expectedWithLanguageData : expectedWithoutLanguageData;

        ULocale kl = new ULocale("@collation=phonebook");

        int n = 0;
        for (ULocale displayLocale : locales) {
            LocaleDisplayNames dn = LocaleDisplayNames.getInstance(displayLocale);
            String result = dn.localeDisplayName(kl);
            assertEquals(kl + " in " + displayLocale, expected[n++], result);
            if (n == expected.length) {
                n = 0;
            }
        }
    }

    @Test
    public void testLanguageDisplayNameDoesNotTranslateRoot() {
        // "root" is not a language code-- the fact that we have our data organized this
        // way is immaterial.  "root" remains untranslated whether we have data or not.
        LocaleDisplayNames dn = LocaleDisplayNames.getInstance(ULocale.US);
        assertEquals("root", "root", dn.languageDisplayName("root"));
    }

    @Test
    public void testLanguageDisplayNameDoesNotTranslateDialects() {
        // Dialect ids are also not language codes.
        LocaleDisplayNames dn = LocaleDisplayNames.getInstance(ULocale.US,
                                                               DialectHandling.DIALECT_NAMES);
        assertEquals("dialect", "en_GB", dn.languageDisplayName("en_GB"));

        String target = LocaleDisplayNamesImpl.haveData(LANG)
            ? "British English"
            : (LocaleDisplayNamesImpl.haveData(REGION)
               ? "en (United Kingdom)"
               : "en (GB)");
        assertEquals("dialect 2", target, dn.localeDisplayName("en_GB"));
    }

    @Test
    public void testLocaleKeywords() {
        LocaleDisplayNames dn = LocaleDisplayNames.getInstance(ULocale.US,
                DialectHandling.DIALECT_NAMES);
        String name = dn.localeDisplayName("de@collation=phonebook");
        String target = LocaleDisplayNamesImpl.haveData(LANG) ?
                "German (Phonebook Sort Order)" : "de (collation=phonebook)";
        assertEquals("collation", target, name);

    }
}
