/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
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
            {"L", "en", "R", "us", "T", "en-us", "en_US"},
            {"L", "en", "R", "FR", "L", "fr", "T", "fr-fr", "fr_FR"},
            {"L", "123", "X"},
            {"R", "us", "T", "und-us", "_US"},
            {"R", "usa", "X"},
            {"R", "123", "L", "en", "T", "en-123", "en_123"},
            {"S", "LATN", "L", "DE", "T", "de-latn", "de_Latn"},
            {"S", "latin", "X"},
            {"L", "th", "R", "th", "K", "nu", "thai", "T", "th-th-u-nu-thai", "th_TH@numbers=thai"},
            {"E", "z", "ExtZ", "L", "en", "T", "en-z-extz", "en@z=extz"},
            {"L", "fr", "R", "FR", "P", "Yoshito-ICU", "T", "fr-fr-x-yoshito-icu", "fr_FR@x=yoshito-icu"},
            {"L", "ja", "R", "jp", "K", "ca", "japanese", "T", "ja-jp-u-ca-japanese", "ja_JP@calendar=japanese"},
            {"K", "co", "PHONEBK", "K", "ca", "gregory", "L", "De", "T", "de-u-ca-gregory-co-phonebk", "de@calendar=gregorian;collation=phonebook"},
            {"E", "o", "OPQR", "E", "a", "aBcD", "T", "und-a-abcd-o-opqr", "@a=abcd;o=opqr"},
            {"E", "u", "nu-thai-ca-gregory", "L", "TH", "T", "th-u-ca-gregory-nu-thai", "th@calendar=gregorian;numbers=thai"},
            {"L", "en", "K", "tz", "usnyc", "R", "US", "T", "en-us-u-tz-usnyc", "en_US@timezone=america/new_york"},
            {"L", "de", "K", "co", "phonebk", "K", "ks", "level1", "K", "kk", "true", "T", "de-u-co-phonebk-kk-true-ks-level1", "de@collation=phonebook;colnormalization=yes;colstrength=primary"},
//          {"L", "en", "V", "foooo_barrr", "T", "en-foooo-barrr", "en__FOOOO_BARRR"},
        };

        Builder bld = new Builder();
        for (int tidx = 0; tidx < TESTCASE.length; tidx++) {
            bld.clear();
            int i = 0;
            String[] expected = null;
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
                        bld.setLDMLExtensionValue(key, type);
                    } else if (method.equals("E")) {
                        String key = TESTCASE[tidx][i++];
                        String value = TESTCASE[tidx][i++];
                        bld.setExtension(key.charAt(0), value);
                    } else if (method.equals("P")) {
                        bld.setExtension(ULocale.PRIVATE_USE_EXTENSION, TESTCASE[tidx][i++]);
                    } else if (method.equals("X")) {
                        errln("FAIL: No excetion was thrown - test csae: "
                                + Utility.arrayToString(TESTCASE[tidx]));
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
                                + " in test case: " + Utility.arrayToString(TESTCASE[tidx]));
                    }
                }
            }
            if (expected != null) {
                ULocale loc = bld.create();
                if (!expected[1].equals(loc.toString())) {
                    errln("FAIL: Wrong locale ID - " + loc + 
                            " for test case: " + Utility.arrayToString(TESTCASE[tidx]));
                }
                String langtag = loc.toLanguageTag();
                if (!expected[0].equals(langtag)) {
                    errln("FAIL: Wrong language tag - " + langtag + 
                            " for test case: " + Utility.arrayToString(TESTCASE[tidx]));
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
            ULocale loc1 = bld.create();
            if (!loc.equals(loc1)) {
                errln("FAIL: Locale loc1 " + loc1 + " was returned by the builder.  Expected " + loc);
            }
            bld.setLanguage("").setLDMLExtensionValue("ca", "buddhist")
                .setLanguage("TH").setLDMLExtensionValue("ca", "gregory");
            ULocale loc2 = bld.create();
            if (!loc.equals(loc2)) {
                errln("FAIL: Locale loc2 " + loc2 + " was returned by the builder.  Expected " + loc);
            }            
        } catch (IllformedLocaleException e) {
            errln("FAIL: IllformedLocaleException: " + e.getMessage());
        }
    }
}
