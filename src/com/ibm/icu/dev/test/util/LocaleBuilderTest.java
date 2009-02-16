/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.InvalidLocaleException;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.LocaleBuilder;

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
        // "K": +1 = locale key / +2 = locale type
        // "E": +1 = extension letter / +2 = extension value
        // "P": +1 = private use
        // "X": indicates an exception must be thrown
        // "T": +1 = expected language tag
        String[][] TESTCASE = {
            {"L", "en", "R", "us", "T", "en-US"},
            {"L", "en", "R", "FR", "L", "fr", "T", "fr-FR"},
            {"L", "123", "X"},
            {"R", "us", "T", "und-US"},
            {"R", "usa", "X"},
            {"R", "123", "L", "en", "T", "en-123"},
            {"S", "LATN", "L", "DE", "T", "de-Latn"},
            {"S", "latin", "X"},
//            {"E", "z", "ExtZ", "L", "en", "T", "en-z-extz"},
            {"E", "z", "ExtZ", "L", "en", "T", "en"},
//            {"L", "fr", "R", "FR", "P", "Yoshito-ICU", "T", "fr-FR-x-yoshito-icu"},
            {"L", "fr", "R", "FR", "P", "Yoshito-ICU", "T", "fr-FR"},
//            {"L", "ja", "R", "jp", "K", "ca", "japanese", "T", "ja-JP-u-ca-japanese"},
            {"L", "ja", "R", "jp", "K", "ca", "japanese", "T", "ja-JP-x-ldml-k-ca-japanese"},
//            {"K", "co", "PHONEBK", "K", "ca", "greg", "L", "De", "T", "de-u-ca-greg-co-phonebk"},
            {"K", "co", "PHONEBK", "K", "ca", "greg", "L", "De", "T", "de-x-ldml-k-ca-greg-k-co-phonebk"},
        };

        for (int tidx = 0; tidx < TESTCASE.length; tidx++) {
            LocaleBuilder bld = new LocaleBuilder();
            int i = 0;
            String expected = null;
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
                        bld.setLocaleKeyword(key, type);
                    } else if (method.equals("E")) {
                        String key = TESTCASE[tidx][i++];
                        String value = TESTCASE[tidx][i++];
                        bld.setExtension(key.charAt(0), value);
                    } else if (method.equals("P")) {
                        bld.setPrivateUse(TESTCASE[tidx][i++]);
                    } else if (method.equals("X")) {
                        errln("FAIL: No excetion was thrown - test csae: "
                                + Utility.arrayToString(TESTCASE[tidx]));
                    } else if (method.equals("T")) {
                        expected = TESTCASE[tidx][i];
                        break;
                    }
                } catch (InvalidLocaleException e) {
                    if (TESTCASE[tidx][i].equals("X")) {
                        // This exception is expected
                        break;
                    } else {
                        errln("FAIL: InvalidLocaleException at offset " + i
                                + " in test case: " + Utility.arrayToString(TESTCASE[tidx]));
                    }
                }
            }
            if (expected != null) {
                ULocale loc = bld.get();
                String langtag = loc.toLanguageTag();
                if (!expected.equals(langtag)) {
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
        ULocale loc = new ULocale("th_TH@calendar=greg");
        LocaleBuilder bld = new LocaleBuilder();
        try {
            bld.setLocale(loc);
            ULocale loc1 = bld.get();
            if (!loc.equals(loc1)) {
                errln("FAIL: Locale loc1 " + loc1 + " was returned by the builder.  Expected " + loc);
            }
            bld.setLanguage("").setLocaleKeyword("calendar", "buddhist")
                .setLanguage("TH").setLocaleKeyword("calendar", "greg");
            ULocale loc2 = bld.get();
            if (!loc.equals(loc2)) {
                errln("FAIL: Locale loc2 " + loc2 + " was returned by the builder.  Expected " + loc);
            }            
        } catch (InvalidLocaleException e) {
            errln("FAIL: InvalidLocaleException: " + e.getMessage());
        }
    }
}
