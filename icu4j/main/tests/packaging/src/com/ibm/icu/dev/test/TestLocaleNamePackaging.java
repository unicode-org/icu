/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import com.ibm.icu.text.LocaleDisplayNames;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.impl.LocaleDisplayNamesImpl;
import static com.ibm.icu.impl.LocaleDisplayNamesImpl.DataTableType.*;

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
        ULocale.ROOT, ULocale.US, new ULocale("es_ES"), ULocale.GERMANY
    };

    // we expect to have data for these
    public void testRegionDisplayNames() {
        
        String[] expectedWithRegionData = {
            "",
            "US",
            "ES",
            "DE",
            "",
            "United States",
            "Spain",
            "Germany",
            "",
            "Estados Unidos",
            "Espa\u00f1a",
            "Alemania",
            "",
            "Vereinigte Staaten",
            "Spanien",
            "Deutschland",
        };
        String[] expectedWithoutRegionData = {
            "",
            "US",
            "ES",
            "DE",
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

    // we don't expect to have data for these
    public void testLanguageDisplayNames() {
        String[] expectedWithLanguageData = {
            "root",
            "en",
            "es",
            "de",
            "Root",
            "English",
            "Spanish",
            "German",
            "ra\u00EDz",
            "ingl\u00E9s",
            "espa\u00F1ol",
            "alem\u00E1n",
            "Root",
            "Englisch",
            "Spanisch",
            "Deutsch",
        };
        String[] expectedWithoutLanguageData = {
            "root", // TODO: fix this
            "en",
            "es",
            "de",
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
}
