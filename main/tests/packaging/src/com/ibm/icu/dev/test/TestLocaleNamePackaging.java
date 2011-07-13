/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import static com.ibm.icu.impl.LocaleDisplayNamesImpl.DataTableType.LANG;
import static com.ibm.icu.impl.LocaleDisplayNamesImpl.DataTableType.REGION;

import com.ibm.icu.impl.LocaleDisplayNamesImpl;
import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.text.LocaleDisplayNames.DialectHandling;
import com.ibm.icu.util.ULocale;

public class TestLocaleNamePackaging extends TestFmwk {

    public static void main(String[] args) {
        new TestLocaleNamePackaging().run(args);
    }

    public TestLocaleNamePackaging() {
    }

    public boolean validate() {
        warnln("language data: " + LocaleDisplayNamesImpl.haveData(LANG));
        warnln("  region data: " + LocaleDisplayNamesImpl.haveData(REGION));
        return true;
    }

    private static ULocale[] locales = {
        ULocale.ROOT, ULocale.US, new ULocale("es_ES"), ULocale.GERMANY,
        new ULocale("und_TH")
    };

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
            "United States",
            "Spain",
            "Germany",
            "Thailand",
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
    }

    public void testLanguageDisplayNames() {
        String[] expectedWithLanguageData = {
            "",
            "en",
            "es",
            "de",
            "und",
            "",
            "English",
            "Spanish",
            "German",
            "Unknown or Invalid Language",
            "",
            "ingl\u00E9s",
            "espa\u00F1ol",
            "alem\u00E1n",
            "indeterminada",
            "",
            "Englisch",
            "Spanisch",
            "Deutsch",
            "Unbestimmte Sprache",
            "",
            "English",
            "Spanish",
            "German",
            "Unknown or Invalid Language",
        };
        String[] expectedWithoutLanguageData = {
            "",
            "en",
            "es",
            "de",
            "und"
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
    }

    // test a 'root' locale, with keywords
    public void testLocaleDisplayNameWithKeywords() {
        String[] expectedWithLanguageData = {
            "root (collation=phonebook)",
            "Root (collation=Phonebook Sort Order)",
            "ra\u00EDz (intercalaci\u00F3n=orden de list\u00EDn telef\u00F3nico)",
            "Root (Sortierung=Telefonbuch-Sortierregeln)",
            "Root (collation=Phonebook Sort Order)",
        };
        String[] expectedWithoutLanguageData = {
            "root (collation=phonebook)",
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

    public void testLanguageDisplayNameDoesNotTranslateRoot() {
        // "root" is not a language code-- the fact that we have our data organized this
        // way is immaterial.  "root" remains untranslated whether we have data or not.
        LocaleDisplayNames dn = LocaleDisplayNames.getInstance(ULocale.US);
        assertEquals("root", "root", dn.languageDisplayName("root"));
    }

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
}
