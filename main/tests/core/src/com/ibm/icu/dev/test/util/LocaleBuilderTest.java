/*
 *******************************************************************************
 * Copyright (C) 2009-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.Arrays;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.IllformedLocaleException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Builder;

/**
 * Test cases for ULocale.LocaleBuilder
 */
public class LocaleBuilderTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new LocaleBuilderTest().run(args);
    }

    public void TestLocaleBuilder() {
        // First String "st": strict (default) / "lv": lenient variant
        // "L": +1 = language
        // "S": +1 = script
        // "R": +1 = region
        // "V": +1 = variant
        // "K": +1 = LDML key / +2 = LDML type
        // "E": +1 = extension letter / +2 = extension value
        // "P": +1 = private use
        // "X": indicates an exception must be thrown
        // "T": +1 = expected language tag
        String[][] TESTCASE = {
            {"st", "L", "en", "R", "us", "T", "en-US", "en_US"},
            {"st", "L", "en", "R", "FR", "L", "fr", "T", "fr-FR", "fr_FR"},
            {"st", "L", "123", "X"},
            {"st", "R", "us", "T", "und-US", "_US"},
            {"st", "R", "usa", "X"},
            {"st", "R", "123", "L", "en", "T", "en-123", "en_123"},
            {"st", "S", "LATN", "L", "DE", "T", "de-Latn", "de_Latn"},
            {"st", "S", "latin", "X"},
            {"st", "L", "th", "R", "th", "K", "nu", "thai", "T", "th-TH-u-nu-thai", "th_TH@numbers=thai"},
            {"st", "E", "z", "ExtZ", "L", "en", "T", "en-z-extz", "en@z=extz"},
            {"st", "L", "fr", "R", "FR", "P", "Yoshito-ICU", "T", "fr-FR-x-yoshito-icu", "fr_FR@x=yoshito-icu"},
            {"st", "L", "ja", "R", "jp", "K", "ca", "japanese", "T", "ja-JP-u-ca-japanese", "ja_JP@calendar=japanese"},
            {"st", "K", "co", "PHONEBK", "K", "ca", "gregory", "L", "De", "T", "de-u-ca-gregory-co-phonebk", "de@calendar=gregorian;collation=phonebook"},
            {"st", "E", "o", "OPQR", "E", "a", "aBcD", "T", "und-a-abcd-o-opqr", "@a=abcd;o=opqr"},
            {"st", "E", "u", "nu-thai-ca-gregory", "L", "TH", "T", "th-u-ca-gregory-nu-thai", "th@calendar=gregorian;numbers=thai"},
            {"st", "L", "en", "K", "tz", "usnyc", "R", "US", "T", "en-US-u-tz-usnyc", "en_US@timezone=America/New_York"},
            {"st", "L", "de", "K", "co", "phonebk", "K", "ks", "level1", "K", "kk", "true", "T", "de-u-co-phonebk-kk-true-ks-level1", "de@collation=phonebook;colnormalization=yes;colstrength=primary"},
            {"lv", "L", "en", "R", "us", "V", "Windows_XP", "T", "en-US-windows-x-variant-xp", "en_US_WINDOWS_XP"},
        };

        Builder bld_st = new Builder();
        Builder bld_lv = new Builder(true);

        for (int tidx = 0; tidx < TESTCASE.length; tidx++) {
            int i = 0;
            String[] expected = null;

            Builder bld = bld_st;
            String bldType = TESTCASE[tidx][i++];

            if (bldType.equals("lv")) {
                bld = bld_lv;
            }

            bld.clear();

            while (true) {
                String method = TESTCASE[tidx][i++];
                try {
                    if (method.equals("L")) {
                        bld.setLanguage(TESTCASE[tidx][i++]);
                    } else if (method.equals("S")) {
                        bld.setScript(TESTCASE[tidx][i++]);
                    } else if (method.equals("R")) {
                        bld.setRegion(TESTCASE[tidx][i++]);
                    } else if (method.equals("V")) {
                        bld.setVariant(TESTCASE[tidx][i++]);
                    } else if (method.equals("K")) {
                        String key = TESTCASE[tidx][i++];
                        String type = TESTCASE[tidx][i++];
                        bld.setUnicodeLocaleKeyword(key, type);
                    } else if (method.equals("E")) {
                        String key = TESTCASE[tidx][i++];
                        String value = TESTCASE[tidx][i++];
                        bld.setExtension(key.charAt(0), value);
                    } else if (method.equals("P")) {
                        bld.setExtension(ULocale.PRIVATE_USE_EXTENSION, TESTCASE[tidx][i++]);
                    } else if (method.equals("X")) {
                        errln("FAIL: No excetion was thrown - test csae: "
                                + Arrays.toString(TESTCASE[tidx]));
                    } else if (method.equals("T")) {
                        expected = new String[2];
                        expected[0] = TESTCASE[tidx][i];
                        expected[1] = TESTCASE[tidx][i + 1];
                        break;
                    }
                } catch (IllformedLocaleException e) {
                    if (TESTCASE[tidx][i].equals("X")) {
                        // This exception is expected
                        break;
                    } else {
                        errln("FAIL: IllformedLocaleException at offset " + i
                                + " in test case: " + Arrays.toString(TESTCASE[tidx]));
                    }
                }
            }
            if (expected != null) {
                ULocale loc = bld.build();
                if (!expected[1].equals(loc.toString())) {
                    errln("FAIL: Wrong locale ID - " + loc + 
                            " for test case: " + Arrays.toString(TESTCASE[tidx]));
                }
                String langtag = loc.toLanguageTag();
                if (!expected[0].equals(langtag)) {
                    errln("FAIL: Wrong language tag - " + langtag + 
                            " for test case: " + Arrays.toString(TESTCASE[tidx]));
                }
                ULocale loc1 = ULocale.forLanguageTag(langtag);
                if (!loc.equals(loc1)) {
                    errln("FAIL: Language tag round trip failed for " + loc);
                }
            }
        }
    }

    public void TestSetLocale() {
        ULocale loc = new ULocale("th_TH@calendar=gregorian");
        Builder bld = new Builder();
        try {
            bld.setLocale(loc);
            ULocale loc1 = bld.build();
            if (!loc.equals(loc1)) {
                errln("FAIL: Locale loc1 " + loc1 + " was returned by the builder.  Expected " + loc);
            }
            bld.setLanguage("").setUnicodeLocaleKeyword("ca", "buddhist")
                .setLanguage("TH").setUnicodeLocaleKeyword("ca", "gregory");
            ULocale loc2 = bld.build();
            if (!loc.equals(loc2)) {
                errln("FAIL: Locale loc2 " + loc2 + " was returned by the builder.  Expected " + loc);
            }            
        } catch (IllformedLocaleException e) {
            errln("FAIL: IllformedLocaleException: " + e.getMessage());
        }
    }
}
