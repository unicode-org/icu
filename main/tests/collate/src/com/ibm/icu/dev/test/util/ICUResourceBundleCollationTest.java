/**
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.jar.JarEntry;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.Holiday;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceTypeMismatchException;


public final class ICUResourceBundleCollationTest extends TestFmwk {
    private static final ClassLoader testLoader = ICUResourceBundleCollationTest.class.getClassLoader();

    private static final String COLLATION_RESNAME = "collations";
    private static final String COLLATION_KEYWORD = "collation";
    private static final String DEFAULT_NAME = "default";
    private static final String STANDARD_NAME = "standard";

    public static void main(String args[]) throws Exception {
        new ICUResourceBundleCollationTest().run(args);
    }

    public void TestFunctionalEquivalent(){
       String[] collCases = {
       //  avail   locale                               equiv
           "f",     "de_US_CALIFORNIA",                 "de",
           "f",     "zh_TW@collation=stroke",           "zh@collation=stroke", /* alias of zh_Hant_TW */
           "t",     "zh_Hant_TW@collation=stroke",      "zh@collation=stroke",
           "f",     "de_CN@collation=pinyin",           "de",
           "t",     "zh@collation=pinyin",              "zh",
           "f",     "zh_CN@collation=pinyin",           "zh", /* alias of zh_Hans_CN */
           "t",     "zh_Hans_CN@collation=pinyin",      "zh",
           "f",     "zh_HK@collation=pinyin",           "zh", /* alias of zh_Hant_HK */
           "t",     "zh_Hant_HK@collation=pinyin",      "zh",
           "f",     "zh_HK@collation=stroke",           "zh@collation=stroke", /* alias of zh_Hant_HK */
           "t",     "zh_Hant_HK@collation=stroke",      "zh@collation=stroke",
           "f",     "zh_HK",                            "zh@collation=stroke", /* alias of zh_Hant_HK */
           "t",     "zh_Hant_HK",                       "zh@collation=stroke",
           "f",     "zh_MO",                            "zh@collation=stroke", /* alias of zh_Hant_MO */
           "t",     "zh_Hant_MO",                       "zh@collation=stroke",
           "f",     "zh_TW_STROKE",                     "zh@collation=stroke",
           "f",     "zh_TW_STROKE@collation=big5han",   "zh@collation=big5han",
           "f",     "de_CN@calendar=japanese",          "de",
           "t",     "de@calendar=japanese",             "de",
           "f",     "zh_TW@collation=big5han",          "zh@collation=big5han", /* alias of zh_Hant_TW */
           "t",     "zh_Hant_TW@collation=big5han",     "zh@collation=big5han",
           "f",     "zh_TW@collation=gb2312han",        "zh@collation=gb2312han", /* alias of zh_Hant_TW */
           "t",     "zh_Hant_TW@collation=gb2312han",   "zh@collation=gb2312han",
           "f",     "zh_CN@collation=big5han",          "zh@collation=big5han", /* alias of zh_Hans_CN */
           "t",     "zh_Hans_CN@collation=big5han",     "zh@collation=big5han",
           "f",     "zh_CN@collation=gb2312han",        "zh@collation=gb2312han", /* alias of zh_Hans_CN */
           "t",     "zh_Hans_CN@collation=gb2312han",   "zh@collation=gb2312han",
           "t",     "zh@collation=big5han",             "zh@collation=big5han",
           "t",     "zh@collation=gb2312han",           "zh@collation=gb2312han",
           "t",     "hi_IN@collation=direct",           "hi@collation=direct",
           "t",     "hi@collation=standard",            "hi",
           "t",     "hi@collation=direct",              "hi@collation=direct",
           "f",     "hi_AU@collation=direct;currency=CHF;calendar=buddhist",    "hi@collation=direct",
           "f",     "hi_AU@collation=standard;currency=CHF;calendar=buddhist",  "hi",
           "t",     "de_DE@collation=pinyin",           "de", /* bug 4582 tests */
           "f",     "de_DE_BONN@collation=pinyin",      "de",
           "t",     "nl",                               "root",
           "t",     "nl_NL",                            "root",
           "f",     "nl_NL_EEXT",                       "root",
           "t",     "nl@collation=stroke",              "root",
           "t",     "nl_NL@collation=stroke",           "root",
           "f",     "nl_NL_EEXT@collation=stroke",      "root",
       };

       logln("Testing functional equivalents for collation...");
       getFunctionalEquivalentTestCases(ICUResourceBundle.ICU_COLLATION_BASE_NAME,
                                        Collator.class.getClassLoader(),
               COLLATION_RESNAME, COLLATION_KEYWORD, true, collCases);
    }

    public void TestGetWithFallback(){
        /*
        UResourceBundle bundle =(UResourceBundle) UResourceBundle.getBundleInstance("com/ibm/icu/dev/data/testdata","te_IN");
        String key = bundle.getStringWithFallback("Keys/collation");
        if(!key.equals("COLLATION")){
            errln("Did not get the expected result from getStringWithFallback method.");
        }
        String type = bundle.getStringWithFallback("Types/collation/direct");
        if(!type.equals("DIRECT")){
            errln("Did not get the expected result form getStringWithFallback method.");
        }
        */
        ICUResourceBundle bundle = null;
        String key = null;
        try{
            bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_COLLATION_BASE_NAME,ULocale.canonicalize("de__PHONEBOOK"));

            if(!bundle.getULocale().equals("de")){
                errln("did not get the expected bundle");
            }
            key = bundle.getStringWithFallback("collations/collation/default");
            if(!key.equals("phonebook")){
                errln("Did not get the expected result from getStringWithFallback method.");
            }

        }catch(MissingResourceException ex){
            logln("got the expected exception");
        }


        bundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_COLLATION_BASE_NAME,"fr_FR");
        key = bundle.getStringWithFallback("collations/default");
        if(!key.equals("standard")){
            errln("Did not get the expected result from getStringWithFallback method.");
        }
    }

    public void TestKeywordValues(){
        String kwVals[];
        boolean foundStandard = false;
        int n;

        logln("Testing getting collation values:");
        kwVals = ICUResourceBundle.getKeywordValues(ICUResourceBundle.ICU_COLLATION_BASE_NAME,COLLATION_RESNAME);
        for(n=0;n<kwVals.length;n++) {
            logln(new Integer(n).toString() + ": " + kwVals[n]);
            if(DEFAULT_NAME.equals(kwVals[n])) {
                errln("getKeywordValues for collation returned 'default' in the list.");
            } else if(STANDARD_NAME.equals(kwVals[n])) {
                if(foundStandard == false) {
                    foundStandard = true;
                    logln("found 'standard'");
                } else {
                    errln("Error - 'standard' is in the keyword list twice!");
                }
            }
        }

        if(foundStandard == false) {
            errln("Error - 'standard' was not in the collation tree as a keyword.");
        } else {
            logln("'standard' was found as a collation keyword.");
        }
    }

    public void TestOpen(){
        UResourceBundle bundle = (UResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_COLLATION_BASE_NAME, "en_US_POSIX");
        if(bundle==null){
            errln("could not load the stream");
        }
    }

    private void getFunctionalEquivalentTestCases(String path, ClassLoader cl, String resName, String keyword,
            boolean truncate, String[] testCases) {
        //String F_STR = "f";
        String T_STR = "t";
        boolean isAvail[] = new boolean[1];

        logln("Testing functional equivalents...");
        for(int i = 0; i < testCases.length ;i+=3) {
            boolean expectAvail = T_STR.equals(testCases[i+0]);
            ULocale inLocale = new ULocale(testCases[i+1]);
            ULocale expectLocale = new ULocale(testCases[i+2]);

            logln(new Integer(i/3).toString() + ": " + new Boolean(expectAvail).toString() + "\t\t" +
                    inLocale.toString() + "\t\t" + expectLocale.toString());

            ULocale equivLocale = ICUResourceBundle.getFunctionalEquivalent(path, cl, resName, keyword, inLocale, isAvail, truncate);
            boolean gotAvail = isAvail[0];

            if((gotAvail != expectAvail) || !equivLocale.equals(expectLocale)) {
                errln(new Integer(i/3).toString() + ":  Error, expected  Equiv=" + new Boolean(expectAvail).toString() + "\t\t" +
                        inLocale.toString() + "\t\t--> " + expectLocale.toString() + ",  but got " + new Boolean(gotAvail).toString() + " " +
                        equivLocale.toString());
            }
        }
    }
}