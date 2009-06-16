/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.util.Iterator;
import java.util.Locale;

import com.ibm.icu.util.ULocale;

public class ULocaleTest extends ICUTestCase {
    private String sampleName;
    private String longULocaleName;
    private String longULocaleBasename;
    private String nonNormalizedName;
    private ULocale longULocale;
    private Locale sampleLocale;
        
    /**
     * @Override
     */
    protected void setUp() throws Exception {
        super.setUp();
                
        sampleName = "ll_CC_VVVV";
        longULocaleName = "ll_Ssss_CC_VVVV@collation=phonebook;key=value";
        longULocaleBasename = longULocaleName.substring(0, longULocaleName.indexOf('@'));
        nonNormalizedName = "LL_ssss_cc_VVVV@ Key = value ; Collation = phonebook ; ";
        longULocale = new ULocale(longULocaleName);
        sampleLocale = new ULocale(sampleName).toLocale();
    }
        
    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.hashCode()'
     */
    public void testHashCode() {
        ULocale obj = ULocale.GERMANY;
        ULocale eq = new ULocale("de_DE");
        ULocale neq = new ULocale("de_DE_FRENCH");
                
        ICUTestCase.testEHCS(obj, eq, neq);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.forLocale(Locale)'
     */
    public void testForLocale() {
        assertEquals(ULocale.GERMANY, ULocale.forLocale(Locale.GERMANY));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.ULocale(String)'
     */
    public void testULocaleString() {
        assertEquals(ULocale.GERMAN, new ULocale("de"));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.ULocale(String, String)'
     */
    public void testULocaleStringString() {
        assertEquals(ULocale.GERMANY, new ULocale("de", "DE"));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.ULocale(String, String, String)'
     */
    public void testULocaleStringStringString() {
        assertEquals(sampleLocale, new ULocale("ll", "cc", "VVVV").toLocale());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.createCanonical(String)'
     */
    public void testCreateCanonical() {
        ULocale result = ULocale.createCanonical("de__PHONEBOOK");
        assertEquals(new ULocale("de@collation=phonebook"), result);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.toLocale()'
     */
    public void testToLocale() {
        assertEquals(sampleLocale, new ULocale("ll", "cc", "VVVV").toLocale());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDefault()'
     */
    public void testGetDefault() {
        assertEquals(Locale.getDefault(), ULocale.getDefault().toLocale());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.setDefault(ULocale)'
     */
    public void testSetDefault() {
        Locale oldLocale = Locale.getDefault();
        ULocale oldULocale = ULocale.getDefault();
        try {
            ULocale.setDefault(longULocale);
            ICUTestCase.assertNotEqual(Locale.getDefault(), oldLocale);
            ICUTestCase.assertNotEqual(ULocale.getDefault(), oldULocale);
            assertEquals(longULocale, ULocale.getDefault());
            assertEquals(sampleLocale, Locale.getDefault());
        }
        finally {
            ULocale.setDefault(oldULocale);
            Locale.setDefault(oldLocale); // in case of some error
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.clone()'
     */
    public void testClone() {
        // see testHashcode
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.equals(Object)'
     */
    public void testEqualsObject() {
        // see testHashcode
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getAvailableLocales()'
     */
    public void testGetAvailableLocales() {
        ULocale[] ulocales = ULocale.getAvailableLocales();
        if (ICUTestCase.testingWrapper) {
            Locale[] locales = Locale.getAvailableLocales();
            for (int i = 0; i < ulocales.length; ++i) {
                assertEquals(ulocales[i].toLocale(), locales[i]);
            }
        }
        // else nothing to test except that the function returned.
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getISOCountries()'
     */
    public void testGetISOCountries() {
        String[] ucountries = ULocale.getISOCountries();
        assertNotNull(ucountries);
        if (ICUTestCase.testingWrapper) {
            // keep our own data for now
            // our data doesn't match java's so this test would fail
            // TODO: enable if we decide to use java's data
            // String[] countries = Locale.getISOCountries();
            // TestBoilerplate.assertArraysEqual(ucountries, countries);
        }
        // else nothing to test except that the function returned.
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getISOLanguages()'
     */
    public void testGetISOLanguages() {
        String[] ulanguages = ULocale.getISOLanguages();
        assertNotNull(ulanguages);
        if (ICUTestCase.testingWrapper) {
            // keep our own data for now
            // our data doesn't match java's so this test would fail
            // TODO: enable if we decide to use java's data
            // String[] languages = Locale.getISOLanguages();
            // TestBoilerplate.assertArraysEqual(ulanguages, languages);
        }
        // else nothing to test except that the function returned.
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getLanguage()'
     */
    public void testGetLanguage() {
        assertEquals("ll", longULocale.getLanguage());
        assertEquals("ll", longULocale.toLocale().getLanguage());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getLanguage(String)'
     */
    public void testGetLanguageString() {
        assertEquals("ll", ULocale.getLanguage(longULocale.getName()));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getScript()'
     */
    public void testGetScript() {
        assertEquals("Ssss", longULocale.getScript());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getScript(String)'
     */
    public void testGetScriptString() {
        assertEquals("Ssss", ULocale.getScript(longULocale.getName()));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getCountry()'
     */
    public void testGetCountry() {
        assertEquals("CC", longULocale.getCountry());
        assertEquals("CC", longULocale.toLocale().getCountry());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getCountry(String)'
     */
    public void testGetCountryString() {
        assertEquals("CC", ULocale.getCountry(longULocale.getName()));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getVariant()'
     */
    public void testGetVariant() {
        assertEquals("VVVV", longULocale.getVariant());
        assertEquals("VVVV", longULocale.toLocale().getVariant());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getVariant(String)'
     */
    public void testGetVariantString() {
        assertEquals("VVVV", ULocale.getVariant(longULocale.getName()));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getFallback(String)'
     */
    public void testGetFallbackString() {
        assertEquals(ULocale.GERMAN, ULocale.getFallback(ULocale.GERMANY.getName()));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getFallback()'
     */
    public void testGetFallback() {
        assertEquals(ULocale.GERMAN, ULocale.GERMANY.getFallback());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getBaseName()'
     */
    public void testGetBaseName() {
        assertEquals(longULocaleBasename, longULocale.getBaseName());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getBaseName(String)'
     */
    public void testGetBaseNameString() {
        assertEquals(longULocaleBasename, longULocale.getBaseName());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getName()'
     */
    public void testGetName() {
        assertEquals(longULocaleName, longULocale.getName());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getName(String)'
     */
    public void testGetNameString() {
        assertEquals(longULocaleName, ULocale.getName(nonNormalizedName));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.toString()'
     */
    public void testToString() {
        assertEquals(longULocaleName, longULocale.toString());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getKeywords()'
     */
    public void testGetKeywords() {
        Iterator iter = longULocale.getKeywords();
        assertEquals(iter.next(), "collation");
        assertEquals(iter.next(), "key");
        assertFalse(iter.hasNext());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getKeywords(String)'
     */
    public void testGetKeywordsString() {
        Iterator iter = ULocale.getKeywords(nonNormalizedName);
        assertEquals(iter.next(), "collation");
        assertEquals(iter.next(), "key");
        assertFalse(iter.hasNext());
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getKeywordValue(String)'
     */
    public void testGetKeywordValueString() {
        assertEquals("value", longULocale.getKeywordValue("key"));
        assertEquals("phonebook", longULocale.getKeywordValue("collation"));
        assertNull(longULocale.getKeywordValue("zzyzx"));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getKeywordValue(String, String)'
     */
    public void testGetKeywordValueStringString() {
        assertEquals("value", ULocale.getKeywordValue(longULocaleName, "key"));
        assertEquals("phonebook", ULocale.getKeywordValue(longULocaleName, "collation"));
        assertNull(ULocale.getKeywordValue(longULocaleName, "zzyzx"));

    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.canonicalize(String)'
     */
    public void testCanonicalize() {
        assertEquals("de@collation=phonebook", ULocale.canonicalize("de__PHONEBOOK"));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.setKeywordValue(String, String)'
     */
    public void testSetKeywordValueStringString() {
        ULocale munged = longULocale.setKeywordValue("key", "C#");
        assertEquals("C#", munged.getKeywordValue("key"));
        munged = munged.setKeywordValue("zzyzx", "grue");
        assertEquals("grue", munged.getKeywordValue("zzyzx"));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.setKeywordValue(String, String, String)'
     */
    public void testSetKeywordValueStringStringString() {
        String munged = ULocale.setKeywordValue(longULocaleName, "key", "C#");
        assertEquals("C#", ULocale.getKeywordValue(munged, "key"));
        munged = ULocale.setKeywordValue(munged, "zzyzx", "grue");
        assertEquals("grue", ULocale.getKeywordValue(munged, "zzyzx"));
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getISO3Language()'
     */
    public void testGetISO3Language() {
        String il = ULocale.GERMANY.getISO3Language();
        String jl = Locale.GERMANY.getISO3Language();
        assertEquals(il, jl);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getISO3Language(String)'
     */
    public void testGetISO3LanguageString() {
        String il = ULocale.getISO3Language(ULocale.GERMANY.getName());
        String jl = Locale.GERMANY.getISO3Language();
        assertEquals(il, jl);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getISO3Country()'
     */
    public void testGetISO3Country() {
        String ic = ULocale.GERMANY.getISO3Country();
        String jc = Locale.GERMANY.getISO3Country();
        assertEquals(ic, jc);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getISO3Country(String)'
     */
    public void testGetISO3CountryString() {
        String ic = ULocale.getISO3Country(ULocale.GERMANY.getName());
        String jc = Locale.GERMANY.getISO3Country();
        assertEquals(ic, jc);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayLanguage()'
     */
    public void testGetDisplayLanguage() {
        String idl = ULocale.GERMANY.getDisplayLanguage();
        String jdl = Locale.GERMANY.getDisplayLanguage();
        assertEquals(idl, jdl);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayLanguage(ULocale)'
     */
    public void testGetDisplayLanguageULocale() {
        String idl = ULocale.GERMANY.getDisplayLanguage(ULocale.GERMANY);
        String jdl = Locale.GERMANY.getDisplayLanguage(Locale.GERMANY);
        assertEquals(idl, jdl);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayLanguage(String, String)'
     */
    public void testGetDisplayLanguageStringString() {
        String idl = ULocale.getDisplayLanguage(ULocale.GERMANY.getName(), "de_DE");
        String jdl = Locale.GERMANY.getDisplayLanguage(Locale.GERMANY);
        assertEquals(idl, jdl);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayLanguage(String, ULocale)'
     */
    public void testGetDisplayLanguageStringULocale() {
        String idl = ULocale.getDisplayLanguage(ULocale.GERMANY.getName(), ULocale.GERMANY);
        String jdl = Locale.GERMANY.getDisplayLanguage(Locale.GERMANY);
        assertEquals(idl, jdl);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayScript()'
     */
    public void testGetDisplayScript() {
        String is = ULocale.TRADITIONAL_CHINESE.getDisplayScript();
        if (ICUTestCase.testingWrapper) {
            assertEquals("Hant", is);
        } else {
            assertEquals("Traditional Chinese", is);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayScript(ULocale)'
     */
    public void testGetDisplayScriptULocale() {
        String is = ULocale.TRADITIONAL_CHINESE.getDisplayScript(ULocale.GERMANY);
        if (ICUTestCase.testingWrapper) {
            assertEquals("Hant", is);
        } else {
            // TODO: look up expected value
            assertEquals("Hant", is);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayScript(String, String)'
     */
    public void testGetDisplayScriptStringString() {
        String is = ULocale.getDisplayScript("zh_Hant", "de_DE");
        if (ICUTestCase.testingWrapper) {
            assertEquals("Hant", is);
        } else {
            // TODO: look up expected value
            assertEquals("Hant", is);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayScript(String, ULocale)'
     */
    public void testGetDisplayScriptStringULocale() {
        String is = ULocale.getDisplayScript("zh_Hant", ULocale.GERMANY);
        if (ICUTestCase.testingWrapper) {
            assertEquals("Hant", is);
        } else {
            // TODO: look up expected value
            assertEquals("Hant", is);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayCountry()'
     */
    public void testGetDisplayCountry() {
        String idc = ULocale.GERMANY.getDisplayCountry();
        String jdc = Locale.GERMANY.getDisplayCountry();
        assertEquals(idc, jdc);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayCountry(ULocale)'
     */
    public void testGetDisplayCountryULocale() {
        String idc = ULocale.GERMANY.getDisplayCountry(ULocale.GERMANY);
        String jdc = Locale.GERMANY.getDisplayCountry(Locale.GERMANY);
        assertEquals(idc, jdc);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayCountry(String, String)'
     */
    public void testGetDisplayCountryStringString() {
        String idc = ULocale.getDisplayCountry("de_DE", "de_DE");
        String jdc = Locale.GERMANY.getDisplayCountry(Locale.GERMANY);
        assertEquals(idc, jdc);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayCountry(String, ULocale)'
     */
    public void testGetDisplayCountryStringULocale() {
        String idc = ULocale.getDisplayCountry("de_DE", ULocale.GERMANY);
        String jdc = Locale.GERMANY.getDisplayCountry(Locale.GERMANY);
        assertEquals(idc, jdc);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayVariant()'
     */
    public void testGetDisplayVariant() {
        String idv = new ULocale("de_DE_PHONEBOOK").getDisplayVariant();
        String jdv = new Locale("de", "DE", "PHONEBOOK").getDisplayVariant();
        assertEquals(jdv, idv);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayVariant(ULocale)'
     */
    public void testGetDisplayVariantULocale() {
        String idv = new ULocale("de_DE_PHONEBOOK").getDisplayVariant(ULocale.GERMANY);
        String jdv = new Locale("de", "DE", "PHONEBOOK").getDisplayVariant(Locale.GERMANY);
        assertEquals(jdv, idv);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayVariant(String, String)'
     */
    public void testGetDisplayVariantStringString() {
        String idv = ULocale.getDisplayVariant("de_DE_PHONEBOOK", "de_DE");
        String jdv = new Locale("de", "DE", "PHONEBOOK").getDisplayVariant(Locale.GERMANY);
        assertEquals(jdv, idv);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayVariant(String, ULocale)'
     */
    public void testGetDisplayVariantStringULocale() {
        String idv = ULocale.getDisplayVariant("de_DE_PHONEBOOK", ULocale.GERMANY);
        String jdv = new Locale("de", "DE", "PHONEBOOK").getDisplayVariant(Locale.GERMANY);
        assertEquals(jdv, idv);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayKeyword(String)'
     */
    public void testGetDisplayKeywordString() {
        String idk = ULocale.getDisplayKeyword("collation");
        assertEquals("collation", idk);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayKeyword(String, String)'
     */
    public void testGetDisplayKeywordStringString() {
        String idk = ULocale.getDisplayKeyword("collation", "de_DE");
        if (ICUTestCase.testingWrapper) {
            assertEquals("collation", idk);
        } else {
            // TODO: find real value
            assertEquals("collation", idk);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayKeyword(String, ULocale)'
     */
    public void testGetDisplayKeywordStringULocale() {
        String idk = ULocale.getDisplayKeyword("collation", ULocale.GERMANY);
        if (ICUTestCase.testingWrapper) {
            assertEquals("collation", idk);
        } else {
            // TODO: find real value
            assertEquals("collation", idk);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayKeywordValue(String)'
     */
    public void testGetDisplayKeywordValueString() {
        ULocale ul = new ULocale("de_DE@collation=phonebook");
        String idk = ul.getDisplayKeywordValue("collation");
        if (ICUTestCase.testingWrapper) {
            assertEquals("phonebook", idk);
        } else {
            // TODO: find real value
            assertEquals("phonebook", idk);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayKeywordValue(String, ULocale)'
     */
    public void testGetDisplayKeywordValueStringULocale() {
        ULocale ul = new ULocale("de_DE@collation=phonebook");
        String idk = ul.getDisplayKeywordValue("collation", ULocale.GERMANY);
        if (ICUTestCase.testingWrapper) {
            assertEquals("phonebook", idk);
        } else {
            // TODO: find real value
            assertEquals("phonebook", idk);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayKeywordValue(String, String, String)'
     */
    public void testGetDisplayKeywordValueStringStringString() {
        String idk = ULocale.getDisplayKeywordValue("de_DE@collation=phonebook", "collation", "de_DE");
        if (ICUTestCase.testingWrapper) {
            assertEquals("phonebook", idk);
        } else {
            // TODO: find real value
            assertEquals("phonebook", idk);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayKeywordValue(String, String, ULocale)'
     */
    public void testGetDisplayKeywordValueStringStringULocale() {
        String idk = ULocale.getDisplayKeywordValue("de_DE@collation=phonebook", "collation", ULocale.GERMANY);
        if (ICUTestCase.testingWrapper) {
            assertEquals("phonebook", idk);
        } else {
            // TODO: find real value
            assertEquals("phonebook", idk);
        }
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayName()'
     */
    public void testGetDisplayName() {
        String idn = ULocale.GERMANY.getDisplayName();
        String jdn = Locale.GERMANY.getDisplayName();
        assertEquals(idn, jdn);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayName(ULocale)'
     */
    public void testGetDisplayNameULocale() {
        String idn = ULocale.GERMANY.getDisplayName(ULocale.GERMANY);
        String jdn = Locale.GERMANY.getDisplayName(Locale.GERMANY);
        assertEquals(idn, jdn);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayName(String, String)'
     */
    public void testGetDisplayNameStringString() {
        String idn = ULocale.getDisplayName("de_DE", "de_DE");
        String jdn = Locale.GERMANY.getDisplayName(Locale.GERMANY);
        assertEquals(idn, jdn);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.getDisplayName(String, ULocale)'
     */
    public void testGetDisplayNameStringULocale() {
        String idn = ULocale.getDisplayName("de_DE", ULocale.GERMANY);
        String jdn = Locale.GERMANY.getDisplayName(Locale.GERMANY);
        assertEquals(idn, jdn);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.acceptLanguage(String, ULocale[], boolean[])'
     */
    public void testAcceptLanguageStringULocaleArrayBooleanArray() {
        boolean[] fallback = new boolean[1];
        ULocale[] locales = { 
            new ULocale("en_CA"), 
            new ULocale("es_US"), 
        };
        ULocale result = ULocale.acceptLanguage("en-US, en-GB, en-CA, es-US", locales, fallback);
        assertEquals(new ULocale("en_CA"), result);
        assertFalse(fallback[0]);
        result = ULocale.acceptLanguage("en-US, en-GB, es-US-NEWMEXICO", locales, fallback);
        assertEquals(new ULocale("es_US"), result);
        assertTrue(fallback[0]);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.acceptLanguage(ULocale[], ULocale[], boolean[])'
     */
    public void testAcceptLanguageULocaleArrayULocaleArrayBooleanArray() {
        boolean[] fallback = new boolean[1];
        ULocale[] locales = { 
            new ULocale("en_CA"), 
            new ULocale("es_US"), 
        };
        ULocale[] accept_locales = {
            new ULocale("en_US"),
            new ULocale("en_GB"),
            new ULocale("en_CA"),
            new ULocale("es_US"),
        };
        ULocale[] accept_locales2 = {
            new ULocale("en_US"),
            new ULocale("en_GB"),
            new ULocale("es_US_NEWMEXICO"),
        };
        ULocale result = ULocale.acceptLanguage(accept_locales, locales, fallback);
        assertEquals(new ULocale("en_CA"), result);
        assertFalse(fallback[0]);
        result = ULocale.acceptLanguage(accept_locales2, locales, fallback);
        assertEquals(new ULocale("es_US"), result);
        assertTrue(fallback[0]);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.acceptLanguage(String, boolean[])'
     */
    public void testAcceptLanguageStringBooleanArray() {
        boolean[] fallback = new boolean[1];
        ULocale result = ULocale.acceptLanguage("en-CA, en-GB, es-US", fallback);
        assertEquals(new ULocale("en_CA"), result);
        assertFalse(fallback[0]);
        result = ULocale.acceptLanguage("es-US-NEWMEXICO", fallback);
        assertNotNull(result); // actual result depends on jdk
        assertTrue(fallback[0]);
    }

    /*
     * Test method for 'com.ibm.icu.x.util.ULocale.acceptLanguage(ULocale[], boolean[])'
     */
    public void testAcceptLanguageULocaleArrayBooleanArray() {
        boolean[] fallback = new boolean[1];
        ULocale[] accept_locales = {
            new ULocale("en_CA"),
            new ULocale("en_GB"),
            new ULocale("es_US"),
        };
        ULocale[] accept_locales2 = {
            new ULocale("es_US_NEWMEXICO"),
        };
        ULocale result = ULocale.acceptLanguage(accept_locales, fallback);
        assertEquals(new ULocale("en_CA"), result);
        assertFalse(fallback[0]);
        result = ULocale.acceptLanguage(accept_locales2, fallback);
        assertNotNull(result); // actual result depends on jdk
        assertTrue(fallback[0]);
    }
}
