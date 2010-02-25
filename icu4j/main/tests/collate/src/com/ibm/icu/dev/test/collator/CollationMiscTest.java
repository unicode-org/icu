 /*
 *******************************************************************************
 * Copyright (C) 2002-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/** 
 * Port From:   ICU4C v2.1 : cintltest
 * Source File: $ICU4CRoot/source/test/cintltest/cmsccoll.c
 */
 
package com.ibm.icu.dev.test.collator;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.ImplicitCEGenerator;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.CollationElementIterator;
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.Normalizer;
import com.ibm.icu.text.RawCollationKey;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.CollationKey.BoundMode;

import java.util.Set;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;

public class CollationMiscTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new CollationMiscTest().run(args);
        // new CollationMiscTest().TestLocaleRuleBasedCollators(); 
    }
    
    //private static final int NORM_BUFFER_TEST_LEN_ = 32;
    private static final class Tester 
    {
        int u;
        String NFC;
        String NFD;
    }
    
    private static final boolean hasCollationElements(Locale locale)
    {
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_COLLATION_BASE_NAME,locale);
        if (rb != null) {
            try {
                String collkey = rb.getStringWithFallback("collations/default"); 
                ICUResourceBundle elements = rb.getWithFallback("collations/" + collkey);
                if (elements != null) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return false;
    }
    
    public void TestComposeDecompose() 
    {
        Tester t[] = new Tester[0x30000];
        t[0] = new Tester();
        logln("Testing UCA extensively\n");
        RuleBasedCollator coll;
        try {
            coll = (RuleBasedCollator)Collator.getInstance(Locale.ENGLISH);
        } 
        catch (Exception e) {
            warnln("Error opening collator\n");
            return;
        }
    
        int noCases = 0;
        for (int u = 0; u < 0x30000; u ++) {
            String comp = UTF16.valueOf(u);
            int len = comp.length();
            t[noCases].NFC = Normalizer.normalize(u, Normalizer.NFC);
            t[noCases].NFD = Normalizer.normalize(u, Normalizer.NFD);
    
            if (t[noCases].NFC.length() != t[noCases].NFD.length() 
                || (t[noCases].NFC.compareTo(t[noCases].NFD) != 0) 
                || (len != t[noCases].NFD.length())
                || (comp.compareTo(t[noCases].NFD) != 0)) {
                t[noCases].u = u;
                if (len != t[noCases].NFD.length() 
                    || (comp.compareTo(t[noCases].NFD) != 0)) {
                    t[noCases].NFC = comp;
                }
                noCases ++;
                t[noCases] = new Tester();
            } 
        }
    
        for (int u = 0; u < noCases; u ++) {
            if (!coll.equals(t[u].NFC, t[u].NFD)) {
                errln("Failure: codePoint \\u" + Integer.toHexString(t[u].u) 
                      + " fails TestComposeDecompose in the UCA");
                CollationTest.doTest(this, coll, t[u].NFC, t[u].NFD, 0);
            }
        }
    
        logln("Testing locales, number of cases = " + noCases);
        Locale loc[] = Collator.getAvailableLocales();
        for (int i = 0; i < loc.length; i ++) {
            if (hasCollationElements(loc[i])) {
                logln("Testing locale " + loc[i].getDisplayName());
                coll = (RuleBasedCollator)Collator.getInstance(loc[i]);
                coll.setStrength(Collator.IDENTICAL);
     
                for (int u = 0; u < noCases; u ++) {
                    if (!coll.equals(t[u].NFC, t[u].NFD)) {
                        errln("Failure: codePoint \\u" 
                              + Integer.toHexString(t[u].u)
                              + " fails TestComposeDecompose for locale "
                              + loc[i].getDisplayName());
                        // this tests for the iterators too
                        CollationTest.doTest(this, coll, t[u].NFC, t[u].NFD, 
                                             0);
                    }
                }
            }
        }
    }
    
    public void TestRuleOptions() {
        // values here are hardcoded and are correct for the current UCA when 
        // the UCA changes, one might be forced to change these values. 
        // (\\u02d0, \\U00010FFFC etc...) 
        String[] rules = {
            // cannot test this anymore, as [last primary ignorable] doesn't 
            // have a  code point associated to it anymore  
            // "&[before 3][last primary ignorable]<<<k",
            // - all befores here amount to zero       
            "&[before 3][first tertiary ignorable]<<<a",
            "&[before 3][last tertiary ignorable]<<<a",  
            "&[before 3][first secondary ignorable]<<<a",
            "&[before 3][last secondary ignorable]<<<a", 
            // 'normal' befores  
            "&[before 3][first primary ignorable]<<<c<<<b &[first primary ignorable]<a",
            // we don't have a code point that corresponds to the last primary 
            // ignorable 
            "&[before 3][last primary ignorable]<<<c<<<b &[last primary ignorable]<a",
            "&[before 3][first variable]<<<c<<<b &[first variable]<a",
            "&[last variable]<a &[before 3][last variable]<<<c<<<b ",
            "&[first regular]<a &[before 1][first regular]<b", 
            "&[before 1][last regular]<b &[last regular]<a",
            "&[before 1][first implicit]<b &[first implicit]<a",
            "&[before 1][last implicit]<b &[last implicit]<a", 
            "&[last variable]<z&[last primary ignorable]<x&[last secondary ignorable]<<y&[last tertiary ignorable]<<<w&[top]<u",
        };
        String[][] data = {
            // {"k", "\u20e3"},
            {"\\u0000", "a"}, // you cannot go before first tertiary ignorable 
            {"\\u0000", "a"}, // you cannot go before last tertiary ignorable 
            {"\\u0000", "a"}, // you cannot go before first secondary ignorable
            {"\\u0000", "a"}, // you cannot go before first secondary ignorable
            {"c", "b", "\\u0332", "a"},
            {"\\u0332", "\\u20e3", "c", "b", "a"},
            {"c", "b", "\\u0009", "a", "\\u000a"},
            {"c", "b", "\\uD834\\uDF71", "a", "\\u02d0"},
            {"b", "\\u02d0", "a", "\\u02d1"},
            // The character in the second ordering test string
            // has to match the character that has the [last regular] weight
            // which changes with each UCA version.
            // See the bottom of FractionalUCA.txt which says something like
            // [last regular [CE 27, 05, 05]] # U+1342E EGYPTIAN HIEROGLYPH AA032
            {"b", "\\U0001342E", "a", "\\u4e00"},
            {"b", "\\u4e00", "a", "\\u4e01"},
            {"b", "\\U0010FFFD", "a"},
            {"\ufffb",  "w", "y", "\u20e3", "x", "\u137c", "z", "u"},
        };
        
        for (int i = 0; i< rules.length; i++) {
            genericRulesStarter(rules[i], data[i]);
        }
    }
    
    void genericRulesStarter(String rules, String[] s) {
        genericRulesStarterWithResult(rules, s, -1);
    }
    
    void genericRulesStarterWithResult(String rules, String[] s, int result) {
        
        RuleBasedCollator coll = null;
        try {
            coll = new RuleBasedCollator(rules);
            // logln("Rules starter for " + rules);
            genericOrderingTestWithResult(coll, s, result);
        } catch (Exception e) {
            warnln("Unable to open collator with rules " + rules);
        }
    }
    
    void genericRulesStarterWithOptionsAndResult(String rules, String[] s, String[] atts, Object[] attVals, int result) {
        RuleBasedCollator coll = null;
        try {
            coll = new RuleBasedCollator(rules);
            genericOptionsSetter(coll, atts, attVals);
            genericOrderingTestWithResult(coll, s, result);
        } catch (Exception e) {
            warnln("Unable to open collator with rules " + rules);
        }
    }
    void genericOrderingTestWithResult(Collator coll, String[] s, int result) {
        String t1 = "";
        String t2 = "";
        
        for(int i = 0; i < s.length - 1; i++) {
            for(int j = i+1; j < s.length; j++) {
                t1 = Utility.unescape(s[i]);
                t2 = Utility.unescape(s[j]);
                // System.out.println(i + " " + j);
                CollationTest.doTest(this, (RuleBasedCollator)coll, t1, t2, 
                                     result);
            }
        }
    }

    void reportCResult(String source, String target, CollationKey sourceKey, CollationKey targetKey,
                       int compareResult, int keyResult, int incResult, int expectedResult ) {
        if (expectedResult < -1 || expectedResult > 1) {
            errln("***** invalid call to reportCResult ****");
            return;
        }
        boolean ok1 = (compareResult == expectedResult);
        boolean ok2 = (keyResult == expectedResult);
        boolean ok3 = (incResult == expectedResult);
        if (ok1 && ok2 && ok3 /* synwee to undo && !isVerbose()*/) {
            return;    
        } else {
            String msg1 = ok1? "Ok: compare(\"" : "FAIL: compare(\"";
            String msg2 = "\", \"";
            String msg3 = "\") returned ";
            String msg4 = "; expected ";
            String sExpect = new String("");
            String sResult = new String("");
            sResult = appendCompareResult(compareResult, sResult);
            sExpect = appendCompareResult(expectedResult, sExpect);
            if (ok1) {
                // logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
            }
            msg1 = ok2 ? "Ok: key(\"" : "FAIL: key(\"";
            msg2 = "\").compareTo(key(\"";
            msg3 = "\")) returned ";
            sResult = appendCompareResult(keyResult, sResult);
            if (ok2) {
                // logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
                msg1 = "  ";
                msg2 = " vs. ";
                errln(msg1 + prettify(sourceKey) + msg2 + prettify(targetKey));
            }
            msg1 = ok3 ? "Ok: incCompare(\"" : "FAIL: incCompare(\"";
            msg2 = "\", \"";
            msg3 = "\") returned ";
            sResult = appendCompareResult(incResult, sResult);
            if (ok3) {
                // logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
            }                
        }
    }
    
    String appendCompareResult(int result, String target) {
        if (result == -1) {  //LESS
            target += "LESS";
        } else if (result == 0) {  //EQUAL
            target += "EQUAL";
        } else if (result == 1) {  //GREATER
            target += "GREATER";
        } else {
            String huh = "?";
            target += huh + result;
        }
        return target;
    }
    
    String prettify(CollationKey sourceKey) {
        int i;
        byte[] bytes= sourceKey.toByteArray();
        String target = "[";
    
        for (i = 0; i < bytes.length; i++) {
            String numStr = Integer.toHexString(bytes[i]);
            if (numStr.length()>2) {
                target += numStr.substring(numStr.length()-2);
            }
            else {
                target += numStr;
            }
            target += " ";
        }
        target += "]";
        return target;
    }
    
    public void TestBeforePrefixFailure() {
        String[] rules = {
            "&g <<< a&[before 3]\uff41 <<< x",
            "&\u30A7=\u30A7=\u3047=\uff6a&\u30A8=\u30A8=\u3048=\uff74&[before 3]\u30a7<<<\u30a9",
            "&[before 3]\u30a7<<<\u30a9&\u30A7=\u30A7=\u3047=\uff6a&\u30A8=\u30A8=\u3048=\uff74",
        };
        String[][] data = {
            {"x", "\uff41"},
            {"\u30a9", "\u30a7"},
            {"\u30a9", "\u30a7"},
        };
        
        for(int i = 0; i< rules.length; i++) {
            genericRulesStarter(rules[i], data[i]);
        }
    }
    
    public void TestContractionClosure() {
        String[] rules = {
            "&b=\u00e4\u00e4",
            "&b=\u00C5",
        };
        String[][] data = {
            { "b", "\u00e4\u00e4", "a\u0308a\u0308", "\u00e4a\u0308", "a\u0308\u00e4" },
            { "b", "\u00C5", "A\u030A", "\u212B" },
        };
        
        for(int i = 0; i< rules.length; i++) {
            genericRulesStarterWithResult(rules[i], data[i], 0);
        }
    }
    
    public void TestPrefixCompose() {
        String rule1 = "&\u30a7<<<\u30ab|\u30fc=\u30ac|\u30fc";
        
        String string = rule1;
        try {
            RuleBasedCollator coll = new RuleBasedCollator(string);
            logln("rule:" + coll.getRules());
        } catch (Exception e) {
            warnln("Error open RuleBasedCollator rule = " + string);
        }
    }
    
    public void TestStrCollIdenticalPrefix() {
        String rule = "&\ud9b0\udc70=\ud9b0\udc71";
        String test[] = {
            "ab\ud9b0\udc70",
            "ab\ud9b0\udc71"
        };
        genericRulesStarterWithResult(rule, test, 0);
    }
    
    public void TestPrefix() {
        String[] rules = {
            "&z <<< z|a",
            "&z <<< z|   a", 
            "[strength I]&a=\ud900\udc25&z<<<\ud900\udc25|a", 
        };
        String[][] data = {
            {"zz", "za"},
            {"zz", "za"},
            {"aa", "az", "\ud900\udc25z", "\ud900\udc25a", "zz"},
        };
        
        for(int i = 0; i<rules.length; i++) {
            genericRulesStarter(rules[i], data[i]);
        }
    }
    
    public void TestNewJapanese() {
        
        String test1[] = {
            "\u30b7\u30e3\u30fc\u30ec",
            "\u30b7\u30e3\u30a4",
            "\u30b7\u30e4\u30a3",
            "\u30b7\u30e3\u30ec",
            "\u3061\u3087\u3053",
            "\u3061\u3088\u3053",
            "\u30c1\u30e7\u30b3\u30ec\u30fc\u30c8",
            "\u3066\u30fc\u305f",
            "\u30c6\u30fc\u30bf", 
            "\u30c6\u30a7\u30bf",
            "\u3066\u3048\u305f",
            "\u3067\u30fc\u305f", 
            "\u30c7\u30fc\u30bf",
            "\u30c7\u30a7\u30bf",
            "\u3067\u3048\u305f",
            "\u3066\u30fc\u305f\u30fc",
            "\u30c6\u30fc\u30bf\u30a1",
            "\u30c6\u30a7\u30bf\u30fc",
            "\u3066\u3047\u305f\u3041",
            "\u3066\u3048\u305f\u30fc",
            "\u3067\u30fc\u305f\u30fc",
            "\u30c7\u30fc\u30bf\u30a1",
            "\u3067\u30a7\u305f\u30a1",
            "\u30c7\u3047\u30bf\u3041",
            "\u30c7\u30a8\u30bf\u30a2",
            "\u3072\u3086",
            "\u3073\u3085\u3042",
            "\u3074\u3085\u3042",
            "\u3073\u3085\u3042\u30fc",
            "\u30d3\u30e5\u30a2\u30fc",
            "\u3074\u3085\u3042\u30fc",
            "\u30d4\u30e5\u30a2\u30fc",
            "\u30d2\u30e5\u30a6",
            "\u30d2\u30e6\u30a6",
            "\u30d4\u30e5\u30a6\u30a2",
            "\u3073\u3085\u30fc\u3042\u30fc", 
            "\u30d3\u30e5\u30fc\u30a2\u30fc",
            "\u30d3\u30e5\u30a6\u30a2\u30fc",
            "\u3072\u3085\u3093",
            "\u3074\u3085\u3093",
            "\u3075\u30fc\u308a",
            "\u30d5\u30fc\u30ea",
            "\u3075\u3045\u308a",
            "\u3075\u30a5\u308a",
            "\u3075\u30a5\u30ea",
            "\u30d5\u30a6\u30ea",
            "\u3076\u30fc\u308a",
            "\u30d6\u30fc\u30ea",
            "\u3076\u3045\u308a",
            "\u30d6\u30a5\u308a",
            "\u3077\u3046\u308a",
            "\u30d7\u30a6\u30ea",
            "\u3075\u30fc\u308a\u30fc",
            "\u30d5\u30a5\u30ea\u30fc",
            "\u3075\u30a5\u308a\u30a3",
            "\u30d5\u3045\u308a\u3043",
            "\u30d5\u30a6\u30ea\u30fc",
            "\u3075\u3046\u308a\u3043",
            "\u30d6\u30a6\u30ea\u30a4",
            "\u3077\u30fc\u308a\u30fc",
            "\u3077\u30a5\u308a\u30a4",
            "\u3077\u3046\u308a\u30fc",
            "\u30d7\u30a6\u30ea\u30a4",
            "\u30d5\u30fd",
            "\u3075\u309e",
            "\u3076\u309d",
            "\u3076\u3075",
            "\u3076\u30d5",
            "\u30d6\u3075",
            "\u30d6\u30d5",
            "\u3076\u309e",
            "\u3076\u3077",
            "\u30d6\u3077",
            "\u3077\u309d",
            "\u30d7\u30fd",
            "\u3077\u3075",
        };
        
        String test2[] = {
            "\u306f\u309d", // H\u309d 
            "\u30cf\u30fd", // K\u30fd 
            "\u306f\u306f", // HH 
            "\u306f\u30cf", // HK 
            "\u30cf\u30cf", // KK 
            "\u306f\u309e", // H\u309e 
            "\u30cf\u30fe", // K\u30fe 
            "\u306f\u3070", // HH\u309b 
            "\u30cf\u30d0", // KK\u309b 
            "\u306f\u3071", // HH\u309c 
            "\u30cf\u3071", // KH\u309c 
            "\u30cf\u30d1", // KK\u309c 
            "\u3070\u309d", // H\u309b\u309d 
            "\u30d0\u30fd", // K\u309b\u30fd 
            "\u3070\u306f", // H\u309bH 
            "\u30d0\u30cf", // K\u309bK 
            "\u3070\u309e", // H\u309b\u309e 
            "\u30d0\u30fe", // K\u309b\u30fe 
            "\u3070\u3070", // H\u309bH\u309b 
            "\u30d0\u3070", // K\u309bH\u309b 
            "\u30d0\u30d0", // K\u309bK\u309b 
            "\u3070\u3071", // H\u309bH\u309c 
            "\u30d0\u30d1", // K\u309bK\u309c 
            "\u3071\u309d", // H\u309c\u309d 
            "\u30d1\u30fd", // K\u309c\u30fd 
            "\u3071\u306f", // H\u309cH 
            "\u30d1\u30cf", // K\u309cK 
            "\u3071\u3070", // H\u309cH\u309b 
            "\u3071\u30d0", // H\u309cK\u309b 
            "\u30d1\u30d0", // K\u309cK\u309b
            "\u3071\u3071", // H\u309cH\u309c
            "\u30d1\u30d1", // K\u309cK\u309c
        };
        
        String[] att = { "strength", };
        Object[] val = { new Integer(Collator.QUATERNARY), };
        
        String[] attShifted = { "strength", "AlternateHandling"};
        Object valShifted[] = { new Integer(Collator.QUATERNARY), 
                                Boolean.TRUE };
       
        genericLocaleStarterWithOptions(Locale.JAPANESE, test1, att, val);
        genericLocaleStarterWithOptions(Locale.JAPANESE, test2, att, val);
        
        genericLocaleStarterWithOptions(Locale.JAPANESE, test1, attShifted,
                                        valShifted);
        genericLocaleStarterWithOptions(Locale.JAPANESE, test2, attShifted,
                                        valShifted);
    }
    
    void genericLocaleStarter(Locale locale, String s[]) {
        RuleBasedCollator coll = null;
        try {
            coll = (RuleBasedCollator)Collator.getInstance(locale);
            
        } catch (Exception e) {
            warnln("Unable to open collator for locale " + locale);
            return;
        }
        // logln("Locale starter for " + locale);
        genericOrderingTest(coll, s);
    }
    
    void genericLocaleStarterWithOptions(Locale locale, String[] s, String[] attrs, Object[] values) {
        genericLocaleStarterWithOptionsAndResult(locale, s, attrs, values, -1);
    }
    
    private void genericOptionsSetter(RuleBasedCollator coll, String[] attrs, Object[] values) {
        for(int i = 0; i < attrs.length; i++) {
            if (attrs[i].equals("strength")) {
                coll.setStrength(((Integer)values[i]).intValue());
            } 
            else if (attrs[i].equals("decomp")) {
                coll.setDecomposition(((Integer)values[i]).intValue());
            } 
            else if (attrs[i].equals("AlternateHandling")) {
                coll.setAlternateHandlingShifted(((Boolean)values[i]
                                                  ).booleanValue());
            }
            else if (attrs[i].equals("NumericCollation")) {
                coll.setNumericCollation(((Boolean)values[i]).booleanValue());
            }
            else if (attrs[i].equals("UpperFirst")) {
                coll.setUpperCaseFirst(((Boolean)values[i]).booleanValue());
            }
            else if (attrs[i].equals("LowerFirst")) {
                coll.setLowerCaseFirst(((Boolean)values[i]).booleanValue());
            }
            else if (attrs[i].equals("CaseLevel")) {
                coll.setCaseLevel(((Boolean)values[i]).booleanValue());
            }
        }        
    }
    
    void genericLocaleStarterWithOptionsAndResult(Locale locale, String[] s, String[] attrs, Object[] values, int result) {
        RuleBasedCollator coll = null;
        try {
            coll = (RuleBasedCollator)Collator.getInstance(locale);
        } catch (Exception e) {
            warnln("Unable to open collator for locale " + locale);
            return;
        }
        // logln("Locale starter for " +locale);
        
        // logln("Setting attributes");
        genericOptionsSetter(coll, attrs, values);
        
        genericOrderingTestWithResult(coll, s, result);
    }
    
    void genericOrderingTest(Collator coll, String[] s) {
        genericOrderingTestWithResult(coll, s, -1);
    }
    
    public void TestNonChars() {
        String test[] = {
            "\u0000",
            "\uFFFE", "\uFFFF",
            "\\U0001FFFE", "\\U0001FFFF",
            "\\U0002FFFE", "\\U0002FFFF",
            "\\U0003FFFE", "\\U0003FFFF",
            "\\U0004FFFE", "\\U0004FFFF",
            "\\U0005FFFE", "\\U0005FFFF",
            "\\U0006FFFE", "\\U0006FFFF",
            "\\U0007FFFE", "\\U0007FFFF",
            "\\U0008FFFE", "\\U0008FFFF",
            "\\U0009FFFE", "\\U0009FFFF",
            "\\U000AFFFE", "\\U000AFFFF",
            "\\U000BFFFE", "\\U000BFFFF",
            "\\U000CFFFE", "\\U000CFFFF",
            "\\U000DFFFE", "\\U000DFFFF",
            "\\U000EFFFE", "\\U000EFFFF",
            "\\U000FFFFE", "\\U000FFFFF",
            "\\U0010FFFE", "\\U0010FFFF"
        };
        Collator coll = null;
        try {
            coll = Collator.getInstance(new Locale("en", "US"));
        } catch (Exception e) {
            warnln("Unable to open collator");
            return;
        }
        // logln("Test non characters");
        
        genericOrderingTestWithResult(coll, test, 0);
    }
    
    public void TestExtremeCompression() {
        String[] test = new String[4];
        
        for(int i = 0; i<4; i++) {
            StringBuffer temp = new StringBuffer();
            for (int j = 0; j < 2047; j++) {
                temp.append('a');
            }
            temp.append((char)('a' + i));
            test[i] = temp.toString();
        }
        
        genericLocaleStarter(new Locale("en", "US"), test);
    }

    /**
     * Tests surrogate support.
     */
    public void TestSurrogates() {
        String test[] = {"z","\ud900\udc25", "\ud805\udc50", "\ud800\udc00y",  
                         "\ud800\udc00r", "\ud800\udc00f", "\ud800\udc00", 
                         "\ud800\udc00c", "\ud800\udc00b", "\ud800\udc00fa", 
                         "\ud800\udc00fb", "\ud800\udc00a", "c", "b"};

        String rule = "&z < \ud900\udc25 < \ud805\udc50 < \ud800\udc00y "
            + "< \ud800\udc00r < \ud800\udc00f << \ud800\udc00 "
            + "< \ud800\udc00fa << \ud800\udc00fb < \ud800\udc00a " 
            + "< c < b";
        genericRulesStarter(rule, test);
    }
    
    public void TestBocsuCoverage() {
        String test = "\u0041\u0441\u4441\\U00044441\u4441\u0441\u0041";
        Collator coll = Collator.getInstance();
        coll.setStrength(Collator.IDENTICAL);
        CollationKey key = coll.getCollationKey(test);
        logln("source:" + key.getSourceString());
    }
    
    public void TestCyrillicTailoring() {
        String test[] = {
            "\u0410b",
            "\u0410\u0306a",
            "\u04d0A"
        };
        genericLocaleStarter(new Locale("en", ""), test);
        genericRulesStarter("&\u0410 = \u0410", test);
        genericRulesStarter("&Z < \u0410", test);
        genericRulesStarter("&\u0410 = \u0410 < \u04d0", test);
        genericRulesStarter("&Z < \u0410 < \u04d0", test);
        genericRulesStarter("&\u0410 = \u0410 < \u0410\u0301", test);
        genericRulesStarter("&Z < \u0410 < \u0410\u0301", test);
    }

    public void TestSuppressContractions() {
        String testNoCont2[] = {
            "\u0410\u0302a",
            "\u0410\u0306b",
            "\u0410c"            
        };
        String testNoCont[] = {
            "a\u0410",            
            "A\u0410\u0306",
            "\uFF21\u0410\u0302"
        };
            
        genericRulesStarter("[suppressContractions [\u0400-\u047f]]", testNoCont);
        genericRulesStarter("[suppressContractions [\u0400-\u047f]]", testNoCont2);
    }
        
    public void TestCase() {
        String gRules = "\u0026\u0030\u003C\u0031\u002C\u2460\u003C\u0061\u002C\u0041";
        String[] testCase = {
            "1a", "1A", "\u2460a", "\u2460A"
        };
        int[][] caseTestResults = {
            { -1, -1, -1, 0, -1, -1, 0, 0, -1 },
            { 1, -1, -1, 0, -1, -1, 0, 0, 1 },
            { -1, -1, -1, 0, 1, -1, 0, 0, -1 },
            { 1, -1, 1, 0, -1, -1, 0, 0, 1 }
    
        };
        boolean[][] caseTestAttributes = {
            { false, false},
            { true, false},
            { false, true},
            { true, true}
        };
        
        int i,j,k;
        Collator  myCollation;
        try {
            myCollation = Collator.getInstance(new Locale("en", "US"));
        } catch (Exception e) {
            warnln("ERROR: in creation of rule based collator ");
            return;
        }
        // logln("Testing different case settings");
        myCollation.setStrength(Collator.TERTIARY);
    
        for(k = 0; k <4; k++) {
            if (caseTestAttributes[k][0] == true) {
                // upper case first
                ((RuleBasedCollator)myCollation).setUpperCaseFirst(true);
            }
            else {
                // upper case first
                ((RuleBasedCollator)myCollation).setLowerCaseFirst(true);   
            }
            ((RuleBasedCollator)myCollation).setCaseLevel(
                                                          caseTestAttributes[k][1]);
          
            // logln("Case first = " + caseTestAttributes[k][0] + ", Case level = " + caseTestAttributes[k][1]);
            for (i = 0; i < 3 ; i++) {
                for(j = i+1; j<4; j++) {
                    CollationTest.doTest(this, 
                                         (RuleBasedCollator)myCollation, 
                                         testCase[i], testCase[j], 
                                         caseTestResults[k][3*i+j-1]);
                }
            }
        }
        try {
            myCollation = new RuleBasedCollator(gRules);
        } catch (Exception e) {
            warnln("ERROR: in creation of rule based collator");
            return;
        }
        // logln("Testing different case settings with custom rules");
        myCollation.setStrength(Collator.TERTIARY);
    
        for(k = 0; k<4; k++) {
            if (caseTestAttributes[k][0] == true) {
                ((RuleBasedCollator)myCollation).setUpperCaseFirst(true);
            }
            else {
                ((RuleBasedCollator)myCollation).setUpperCaseFirst(false);
            }
            ((RuleBasedCollator)myCollation).setCaseLevel(
                                                          caseTestAttributes[k][1]);
            for (i = 0; i < 3 ; i++) {
                for(j = i+1; j<4; j++) {
                    CollationTest.doTest(this, 
                                         (RuleBasedCollator)myCollation, 
                                         testCase[i], testCase[j], 
                                         caseTestResults[k][3*i+j-1]);
                }
            }
        }

        {
            String[] lowerFirst = {
                "h",
                "H",
                "ch",
                "Ch",
                "CH",
                "cha",
                "chA",
                "Cha",
                "ChA",
                "CHa",
                "CHA",
                "i",
                "I"
            };
    
            String[] upperFirst = {
                "H",
                "h",
                "CH",
                "Ch",
                "ch",
                "CHA",
                "CHa",
                "ChA",
                "Cha",
                "chA",
                "cha",
                "I",
                "i"
            };
            // logln("mixed case test");
            // logln("lower first, case level off");
            genericRulesStarter("[casefirst lower]&H<ch<<<Ch<<<CH", lowerFirst); 
            // logln("upper first, case level off");
            genericRulesStarter("[casefirst upper]&H<ch<<<Ch<<<CH", upperFirst);
            // logln("lower first, case level on");
            genericRulesStarter("[casefirst lower][caselevel on]&H<ch<<<Ch<<<CH", lowerFirst);
            // logln("upper first, case level on");
            genericRulesStarter("[casefirst upper][caselevel on]&H<ch<<<Ch<<<CH", upperFirst);
        }
    }

    public void TestIncompleteCnt() {
        String[] cnt1 = {
            "AA",
            "AC",
            "AZ",
            "AQ",
            "AB",
            "ABZ",
            "ABQ",
            "Z",
            "ABC",
            "Q",
            "B"
        };
            
        String[] cnt2 = {
            "DA",
            "DAD",
            "DAZ",
            "MAR",
            "Z",
            "DAVIS",
            "MARK",
            "DAV",
            "DAVI"
        };
        RuleBasedCollator coll =  null;
        String temp = " & Z < ABC < Q < B";
        try {
            coll = new RuleBasedCollator(temp);
        } catch (Exception e) {
            warnln("fail to create RuleBasedCollator");
            return;
        }
        
        int size = cnt1.length;
        for(int i = 0; i < size-1; i++) {
            for(int j = i+1; j < size; j++) {
                String t1 = cnt1[i];
                String t2 = cnt1[j];
                CollationTest.doTest(this, coll, t1, t2, -1);
            }
        }
        
        temp = " & Z < DAVIS < MARK <DAV";
        try {
            coll = new RuleBasedCollator(temp);
        } catch (Exception e) {
            warnln("fail to create RuleBasedCollator");
            return;
        }
        
        size = cnt2.length;
        for(int i = 0; i < size-1; i++) {
            for(int j = i+1; j < size; j++) {
                String t1 = cnt2[i];
                String t2 = cnt2[j];
                CollationTest.doTest(this, coll, t1, t2, -1);
            }
        }
    }
        
    public void TestBlackBird() {
        String[] shifted = {
            "black bird",
            "black-bird",
            "blackbird",
            "black Bird",
            "black-Bird",
            "blackBird",
            "black birds",
            "black-birds",
            "blackbirds"
        };
        int[] shiftedTert = {
            0,
            0,
            0,
            -1,
            0,
            0,
            -1,
            0,
            0
        };
        String[] nonignorable = {
            "black bird",
            "black Bird",
            "black birds",
            "black-bird",
            "black-Bird",
            "black-birds",
            "blackbird",
            "blackBird",
            "blackbirds"
        };
        int i = 0, j = 0;
        int size = 0;
        Collator coll = Collator.getInstance(new Locale("en", "US"));
        //ucol_setAttribute(coll, UCOL_NORMALIZATION_MODE, UCOL_OFF, &status);
        //ucol_setAttribute(coll, UCOL_ALTERNATE_HANDLING, UCOL_NON_IGNORABLE, &status);
        ((RuleBasedCollator)coll).setAlternateHandlingShifted(false);
        size = nonignorable.length;
        for(i = 0; i < size-1; i++) {
            for(j = i+1; j < size; j++) {
                String t1 = nonignorable[i];
                String t2 = nonignorable[j];
                CollationTest.doTest(this, (RuleBasedCollator)coll, t1, t2, -1);
            }
        }
        ((RuleBasedCollator)coll).setAlternateHandlingShifted(true);
        coll.setStrength(Collator.QUATERNARY);
        size = shifted.length;
        for(i = 0; i < size-1; i++) {
            for(j = i+1; j < size; j++) {
                String t1 = shifted[i];
                String t2 = shifted[j];
                CollationTest.doTest(this, (RuleBasedCollator)coll, t1, t2, -1);
            }
        }
        coll.setStrength(Collator.TERTIARY);
        size = shifted.length;
        for(i = 1; i < size; i++) {
            String t1 = shifted[i-1];
            String t2 = shifted[i];
            CollationTest.doTest(this, (RuleBasedCollator)coll, t1, t2, 
                                 shiftedTert[i]);
        }
    }
    
    public void TestFunkyA() {
        String[] testSourceCases = {
            "\u0041\u0300\u0301",
            "\u0041\u0300\u0316",
            "\u0041\u0300",
            "\u00C0\u0301",
            // this would work with forced normalization 
            "\u00C0\u0316",
        };
        
        String[] testTargetCases = {
            "\u0041\u0301\u0300",
            "\u0041\u0316\u0300",
            "\u00C0",
            "\u0041\u0301\u0300",
            // this would work with forced normalization 
            "\u0041\u0316\u0300",
        };
        
        int[] results = {
            1,
            0,
            0,
            1,
            0
        };

        Collator  myCollation;
        try {
            myCollation = Collator.getInstance(new Locale("en", "US"));
        } catch (Exception e) {
            warnln("ERROR: in creation of rule based collator");
            return;
        }
        // logln("Testing some A letters, for some reason");
        myCollation.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        myCollation.setStrength(Collator.TERTIARY);
        for (int i = 0; i < 4 ; i++)
            {
                CollationTest.doTest(this, (RuleBasedCollator)myCollation, 
                                     testSourceCases[i], testTargetCases[i], 
                                     results[i]);
            }
    }
    
    public void TestChMove() {
        String[] chTest = {
            "c",
            "C",
            "ca", "cb", "cx", "cy", "CZ",
            "c\u030C", "C\u030C",
            "h",
            "H",
            "ha", "Ha", "harly", "hb", "HB", "hx", "HX", "hy", "HY",
            "ch", "cH", "Ch", "CH",
            "cha", "charly", "che", "chh", "chch", "chr",
            "i", "I", "iarly",
            "r", "R",
            "r\u030C", "R\u030C",
            "s",
            "S",
            "s\u030C", "S\u030C",
            "z", "Z",
            "z\u030C", "Z\u030C"
        };
        Collator coll = null;
        try {
            coll = Collator.getInstance(new Locale("cs", ""));
        } catch (Exception e) {
            warnln("Cannot create Collator");
            return;
        }
        int size = chTest.length;
        for(int i = 0; i < size-1; i++) {
            for(int j = i+1; j < size; j++) {
                String t1 = chTest[i];
                String t2 = chTest[j];
                CollationTest.doTest(this, (RuleBasedCollator)coll, t1, t2, -1);
            }
        }
    }
    
    public void TestImplicitTailoring() {
        String rules[] = { "&[before 1]\u4e00 < b < c &[before 1]\u4e00 < d < e",
                           "&\u4e00 < a <<< A < b <<< B",
                           "&[before 1]\u4e00 < \u4e01 < \u4e02",
                           "&[before 1]\u4e01 < \u4e02 < \u4e03",
        };
        String cases[][] = {
            { "d", "e", "b", "c", "\u4e00"}, 
            { "\u4e00", "a", "A", "b", "B", "\u4e01"},
            { "\u4e01", "\u4e02", "\u4e00"},
            { "\u4e02", "\u4e03", "\u4e01"},
        };
        
        int i = 0;
        
        for(i = 0; i < rules.length; i++) {
            genericRulesStarter(rules[i], cases[i]);
        }
        
    }

    public void TestFCDProblem() {
        String s1 = "\u0430\u0306\u0325";
        String s2 = "\u04D1\u0325";
        Collator coll = null;
        try {
            coll = Collator.getInstance();
        } catch (Exception e) {
            warnln("Can't create collator");
            return;
        }
        
        coll.setDecomposition(Collator.NO_DECOMPOSITION);
        CollationTest.doTest(this, (RuleBasedCollator)coll, s1, s2, 0);
        coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        CollationTest.doTest(this, (RuleBasedCollator)coll, s1, s2, 0);
    }
    
    public void TestEmptyRule() {
        String rulez = "";
        try {
            RuleBasedCollator coll = new RuleBasedCollator(rulez);
            logln("rule:" + coll.getRules());
        } catch (Exception e) {
            warnln(e.getMessage());
        }
    }
    
    /* superseded by TestBeforePinyin, since Chinese collation rules have changed */
    /*
    public void TestJ784() {
        String[] data = {
            "A", "\u0101", "\u00e1", "\u01ce", "\u00e0",
            "E", "\u0113", "\u00e9", "\u011b", "\u00e8",
            "I", "\u012b", "\u00ed", "\u01d0", "\u00ec",
            "O", "\u014d", "\u00f3", "\u01d2", "\u00f2",
            "U", "\u016b", "\u00fa", "\u01d4", "\u00f9",
            "\u00fc", "\u01d6", "\u01d8", "\u01da", "\u01dc"
        };
        genericLocaleStarter(new Locale("zh", ""), data);
    }
    */
    
    public void TestJ815() {
        String data[] = {
            "aa",
            "Aa",
            "ab",
            "Ab",
            "ad",
            "Ad",
            "ae",
            "Ae",
            "\u00e6",
            "\u00c6",
            "af",
            "Af",
            "b",
            "B"
        };
        genericLocaleStarter(new Locale("fr", ""), data);
        genericRulesStarter("[backwards 2]&A<<\u00e6/e<<<\u00c6/E", data);
    }
    
    public void TestJ3087()
    {
        String rule[] = {"&h<H&CH=\u0427",
                         "&CH=\u0427&h<H",
                         "&CH=\u0427"}; 
        RuleBasedCollator rbc = null;
        CollationElementIterator iter1;
        CollationElementIterator iter2;
        for (int i = 0; i < rule.length; i ++) {
            try {
                rbc = new RuleBasedCollator(rule[i]); 
            } catch (Exception e) {
                warnln(e.getMessage());
                return;
            }
            iter1 = rbc.getCollationElementIterator("CH"); 
            iter2 = rbc.getCollationElementIterator("\u0427");
            int ce1 = CollationElementIterator.IGNORABLE;
            int ce2 = CollationElementIterator.IGNORABLE;
            while (ce1 != CollationElementIterator.NULLORDER
                   && ce2 != CollationElementIterator.NULLORDER) {
                ce1 = iter1.next();
                ce2 = iter2.next();
                if (ce1 != ce2) {
                    errln("Error generating RuleBasedCollator with the rule "
                          + rule[i]);
                    errln("CH != \\u0427");
                }
            }
        }
    }
    
    public void DontTestJ831() { // Latvian does not use upper first
        String[] data = {
            "I",
            "i",
            "Y",
            "y"
        };
        genericLocaleStarter(new Locale("lv", ""), data);
    }
    
    public void TestBefore() {
        String data[] = {
            "\u0101", "\u00e1", "\u01ce", "\u00e0", "A",
            "\u0113", "\u00e9", "\u011b", "\u00e8", "E",
            "\u012b", "\u00ed", "\u01d0", "\u00ec", "I",
            "\u014d", "\u00f3", "\u01d2", "\u00f2", "O",
            "\u016b", "\u00fa", "\u01d4", "\u00f9", "U",
            "\u01d6", "\u01d8", "\u01da", "\u01dc", "\u00fc"
        };
        genericRulesStarter(
                            "&[before 1]a<\u0101<\u00e1<\u01ce<\u00e0"
                            + "&[before 1]e<\u0113<\u00e9<\u011b<\u00e8"
                            + "&[before 1]i<\u012b<\u00ed<\u01d0<\u00ec"
                            + "&[before 1]o<\u014d<\u00f3<\u01d2<\u00f2"
                            + "&[before 1]u<\u016b<\u00fa<\u01d4<\u00f9"
                            + "&u<\u01d6<\u01d8<\u01da<\u01dc<\u00fc", data);
    }

    public void TestRedundantRules() {
        String[] rules = {
            //"& a <<< b <<< c << d <<< e& [before 1] e <<< x",
            "& b <<< c <<< d << e <<< f& [before 3] f <<< x",
            "& a < b <<< c << d <<< e& [before 1] e <<< x",
            "& a < b < c < d& [before 1] c < m",
            "& a < b <<< c << d <<< e& [before 3] e <<< x",
            "& a < b <<< c << d <<< e& [before 2] e <<< x",
            "& a < b <<< c << d <<< e <<< f < g& [before 1] g < x",
            "& a <<< b << c < d& a < m",
            "&a<b<<b\u0301 &z<b",
            "&z<m<<<q<<<m",
            "&z<<<m<q<<<m",
            "& a < b < c < d& r < c",
            "& a < b < c < d& r < c",
            "& a < b < c < d& c < m",
            "& a < b < c < d& a < m"
        };
        
        String[] expectedRules = {
            //"&\u2089<<<x",
            "&\u0252<<<x",
            "& a <<< x < b <<< c << d <<< e",
            "& a < b < m < c < d",
            "& a < b <<< c << d <<< x <<< e",
            "& a < b <<< c <<< x << d <<< e",
            "& a < b <<< c << d <<< e <<< f < x < g",
            "& a <<< b << c < m < d",
            "&a<b\u0301 &z<b",
            "&z<q<<<m",
            "&z<q<<<m",
            "& a < b < d& r < c",
            "& a < b < d& r < c",
            "& a < b < c < m < d",
            "& a < m < b < c < d"
        };
        
        String[][] testdata = {
            //            {"\u2089", "x"},
            {"\u0252", "x"},
            {"a", "x", "b", "c", "d", "e"},
            {"a", "b", "m", "c", "d"},
            {"a", "b", "c", "d", "x", "e"},
            {"a", "b", "c", "x", "d", "e"},
            {"a", "b", "c", "d", "e", "f", "x", "g"},
            {"a", "b", "c", "m", "d"},
            {"a", "b\u0301", "z", "b"},
            {"z", "q", "m"},
            {"z", "q", "m"},
            {"a", "b", "d"},
            {"r", "c"},
            {"a", "b", "c", "m", "d"},
            {"a", "m", "b", "c", "d"}
        };
        
        String rlz = "";
        for(int i = 0; i<rules.length; i++) {
            logln("testing rule " + rules[i] + ", expected to be" + expectedRules[i]);
            try {
                rlz = rules[i];
                Collator credundant = new RuleBasedCollator(rlz);
                rlz = expectedRules[i];
                Collator cresulting = new RuleBasedCollator(rlz);
                logln(" credundant Rule:" + ((RuleBasedCollator)credundant).getRules());
                logln(" cresulting Rule:" + ((RuleBasedCollator)cresulting).getRules());
            } catch (Exception e) {
                warnln("Cannot create RuleBasedCollator");
            }
            //testAgainstUCA(cresulting, credundant, "expected", TRUE, &status);
            // logln("testing using data\n");
            genericRulesStarter(rules[i], testdata[i]);
        }
    }
    
    public void TestExpansionSyntax() {
        String[] rules = {
            "&AE <<< a << b <<< c &d <<< f",
            "&AE <<< a <<< b << c << d < e < f <<< g",
            "&AE <<< B <<< C / D <<< F"
        };
        
        String[] expectedRules = {
            "&A <<< a / E << b / E <<< c /E  &d <<< f",
            "&A <<< a / E <<< b / E << c / E << d / E < e < f <<< g",
            "&A <<< B / E <<< C / ED <<< F / E"
        };
        
        String[][] testdata = {
            {"AE", "a", "b", "c"},
            {"AE", "a", "b", "c", "d", "e", "f", "g"},
            {"AE", "B", "C"} // / ED <<< F / E"},
        };
        
        for(int i = 0; i<rules.length; i++) {
            // logln("testing rule " + rules[i] + ", expected to be " + expectedRules[i]);
            try {
                String rlz = rules[i];
                Collator credundant = new RuleBasedCollator(rlz);
                rlz = expectedRules[i];
                Collator cresulting = new RuleBasedCollator(rlz);
                logln(" credundant Rule:" + ((RuleBasedCollator)credundant).getRules());
                logln(" cresulting Rule:" + ((RuleBasedCollator)cresulting).getRules());
            } catch (Exception e) {
                warnln(e.getMessage());
            }
            // testAgainstUCA still doesn't handle expansions correctly, so this is not run 
            // as a hard error test, but only in information mode 
            //testAgainstUCA(cresulting, credundant, "expected", FALSE, &status);
            
            // logln("testing using data");
            genericRulesStarter(rules[i], testdata[i]);
        }
    }

    public void TestHangulTailoring() {
        String[] koreanData = {
            "\uac00", "\u4f3d", "\u4f73", "\u5047", "\u50f9", "\u52a0", "\u53ef", "\u5475",
            "\u54e5", "\u5609", "\u5ac1", "\u5bb6", "\u6687", "\u67b6", "\u67b7", "\u67ef",
            "\u6b4c", "\u73c2", "\u75c2", "\u7a3c", "\u82db", "\u8304", "\u8857", "\u8888",
            "\u8a36", "\u8cc8", "\u8dcf", "\u8efb", "\u8fe6", "\u99d5",
            "\u4EEE", "\u50A2", "\u5496", "\u54FF", "\u5777", "\u5B8A", "\u659D", "\u698E",
            "\u6A9F", "\u73C8", "\u7B33", "\u801E", "\u8238", "\u846D", "\u8B0C"
        };
        
        String rules =
            "&\uac00 <<< \u4f3d <<< \u4f73 <<< \u5047 <<< \u50f9 <<< \u52a0 <<< \u53ef <<< \u5475 "
            + "<<< \u54e5 <<< \u5609 <<< \u5ac1 <<< \u5bb6 <<< \u6687 <<< \u67b6 <<< \u67b7 <<< \u67ef "
            + "<<< \u6b4c <<< \u73c2 <<< \u75c2 <<< \u7a3c <<< \u82db <<< \u8304 <<< \u8857 <<< \u8888 "
            + "<<< \u8a36 <<< \u8cc8 <<< \u8dcf <<< \u8efb <<< \u8fe6 <<< \u99d5 "
            + "<<< \u4EEE <<< \u50A2 <<< \u5496 <<< \u54FF <<< \u5777 <<< \u5B8A <<< \u659D <<< \u698E "
            + "<<< \u6A9F <<< \u73C8 <<< \u7B33 <<< \u801E <<< \u8238 <<< \u846D <<< \u8B0C";
        
        String rlz = rules;
        
        Collator coll = null;
        try {
            coll = new RuleBasedCollator(rlz);
        } catch (Exception e) {
            warnln("Unable to open collator with rules" + rules);
            return;
        }
        // logln("Using start of korean rules\n");
        genericOrderingTest(coll, koreanData);
        // logln("Setting jamoSpecial to TRUE and testing once more\n");
        
        // can't set jamo in icu4j 
        // ((UCATableHeader *)coll->image)->jamoSpecial = TRUE; // don't try this at home
        // genericOrderingTest(coll, koreanData);
        
        // no such locale in icu4j
        // logln("Using ko__LOTUS locale\n");
        // genericLocaleStarter(new Locale("ko__LOTUS", ""), koreanData);
    }

    public void TestIncrementalNormalize() {
        Collator        coll = null;
        // logln("Test 1 ....");
        {
            /* Test 1.  Run very long unnormalized strings, to force overflow of*/
            /*          most buffers along the way.*/
            
            try {
                coll = Collator.getInstance(new Locale("en", "US"));
            } catch (Exception e) {
                warnln("Cannot get default instance!");
                return;
            }
            char baseA     =0x41;
            char ccMix[]   = {0x316, 0x321, 0x300};
            int          sLen;
            int          i;
            StringBuffer strA = new StringBuffer();
            StringBuffer strB = new StringBuffer();
            
            coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            
            for (sLen = 1000; sLen<1001; sLen++) {
                strA.delete(0, strA.length());
                strA.append(baseA);
                strB.delete(0, strB.length());
                strB.append(baseA);
                for (i=1; i< sLen; i++) {
                    strA.append(ccMix[i % 3]);
                    strB.insert(1, ccMix[i % 3]);
                }
                coll.setStrength(Collator.TERTIARY);   // Do test with default strength, which runs
                CollationTest.doTest(this, (RuleBasedCollator)coll, 
                                     strA.toString(), strB.toString(), 0);    //   optimized functions in the impl
                coll.setStrength(Collator.IDENTICAL);   // Do again with the slow, general impl.
                CollationTest.doTest(this, (RuleBasedCollator)coll, 
                                     strA.toString(), strB.toString(), 0);
            }
        }
        /*  Test 2:  Non-normal sequence in a string that extends to the last character*/
        /*         of the string.  Checks a couple of edge cases.*/
        // logln("Test 2 ....");    
        {
            String strA = "AA\u0300\u0316";
            String strB = "A\u00c0\u0316";
            coll.setStrength(Collator.TERTIARY);
            CollationTest.doTest(this, (RuleBasedCollator)coll, strA, strB, 0);
        }
        /*  Test 3:  Non-normal sequence is terminated by a surrogate pair.*/
        // logln("Test 3 ....");
        {
            String strA = "AA\u0300\u0316\uD800\uDC01";
            String strB = "A\u00c0\u0316\uD800\uDC00";
            coll.setStrength(Collator.TERTIARY);
            CollationTest.doTest(this, (RuleBasedCollator)coll, strA, strB, 1);
        }
        /*  Test 4:  Imbedded nulls do not terminate a string when length is specified.*/
        // logln("Test 4 ....");
        /*
         * not a valid test since string are null-terminated in java{
         char strA[] = {0x41, 0x00, 0x42};
         char strB[] = {0x41, 0x00, 0x00};
            
         int result = coll.compare(new String(strA), new String(strB));
         if (result != 1) {
         errln("ERROR 1 in test 4\n");
         }
            
         result = coll.compare(new String(strA, 0, 1), new String(strB, 0, 1));
         if (result != 0) {
         errln("ERROR 1 in test 4\n");
         }
            
         CollationKey sortKeyA = coll.getCollationKey(new String(strA));
         CollationKey sortKeyB = coll.getCollationKey(new String(strB));
            
         int r = sortKeyA.compareTo(sortKeyB);
         if (r <= 0) {
         errln("Error 4 in test 4\n");
         }
    
         coll.setStrength(Collator.IDENTICAL);
         sortKeyA = coll.getCollationKey(new String(strA));
         sortKeyB = coll.getCollationKey(new String(strB));
    
         r = sortKeyA.compareTo(sortKeyB);
         if (r <= 0) {
         errln("Error 7 in test 4\n");
         }
            
         coll.setStrength(Collator.TERTIARY);
         }
        */
        /*  Test 5:  Null characters in non-normal source strings.*/
        // logln("Test 5 ....");
        /*
         * not a valid test since string are null-terminated in java{
         {
         char strA[] = {0x41, 0x41, 0x300, 0x316, 0x00, 0x42,};
         char strB[] = {0x41, 0x41, 0x300, 0x316, 0x00, 0x00,};
           
    
         int result = coll.compare(new String(strA, 0, 6), new String(strB, 0, 6));
         if (result < 0) {
         errln("ERROR 1 in test 5\n");
         }
         result = coll.compare(new String(strA, 0, 4), new String(strB, 0, 4));
         if (result != 0) {
         errln("ERROR 2 in test 5\n");
         }
    
         CollationKey sortKeyA = coll.getCollationKey(new String(strA));
         CollationKey sortKeyB = coll.getCollationKey(new String(strB));
         int r = sortKeyA.compareTo(sortKeyB);
         if (r <= 0) {
         errln("Error 4 in test 5\n");
         }
    
         coll.setStrength(Collator.IDENTICAL);
            
         sortKeyA = coll.getCollationKey(new String(strA));
         sortKeyB = coll.getCollationKey(new String(strB));
         r = sortKeyA.compareTo(sortKeyB);
         if (r <= 0) {
         errln("Error 7 in test 5\n");
         }
            
         coll.setStrength(Collator.TERTIARY);
         }
        */
        /*  Test 6:  Null character as base of a non-normal combining sequence.*/
        // logln("Test 6 ....");
        /*
         * not a valid test since string are null-terminated in java{
         {
         char strA[] = {0x41, 0x0, 0x300, 0x316, 0x41, 0x302,};
         char strB[] = {0x41, 0x0, 0x302, 0x316, 0x41, 0x300,};
    
         int result = coll.compare(new String(strA, 0, 5), new String(strB, 0, 5));
         if (result != -1) {
         errln("Error 1 in test 6\n");
         }
         result = coll.compare(new String(strA, 0, 1), new String(strB, 0, 1));
         if (result != 0) {
         errln("Error 2 in test 6\n");
         }
         }
        */
    }
    
    public void TestContraction() {
        String[] testrules = {
            "&A = AB / B",
            "&A = A\\u0306/\\u0306",
            "&c = ch / h",
        };
        String[] testdata = {
            "AB", "AB", "A\u0306", "ch"
        };
        String[] testdata2 = {
            "\u0063\u0067",
            "\u0063\u0068",
            "\u0063\u006C",
        };
        String[] testrules3 = {
            "&z < xyz &xyzw << B",
            "&z < xyz &xyz << B / w",
            "&z < ch &achm << B",
            "&z < ch &a << B / chm",
            "&\ud800\udc00w << B",
            "&\ud800\udc00 << B / w",
            "&a\ud800\udc00m << B",
            "&a << B / \ud800\udc00m",
        };
    
        RuleBasedCollator  coll = null;
        for (int i = 0; i < testrules.length; i ++) {
            CollationElementIterator iter1 = null;
            int j = 0;
            // logln("Rule " + testrules[i] + " for testing\n");
            String rule = testrules[i];
            try {
                coll = new RuleBasedCollator(rule);
            } catch (Exception e) {
                warnln("Collator creation failed " + testrules[i]);
                return;
            }
            try {
                iter1 = coll.getCollationElementIterator(testdata[i]);
            } catch (Exception e) {
                errln("Collation iterator creation failed\n");
                return;
            }
            while (j < 2) {
                CollationElementIterator iter2;
                int ce;
                try {
                    iter2 = coll.getCollationElementIterator(String.valueOf(testdata[i].charAt(j)));
                
                }catch (Exception e) {
                    errln("Collation iterator creation failed\n");
                    return;
                }
                ce = iter2.next();
                while (ce != CollationElementIterator.NULLORDER) {
                    if (iter1.next() != ce) {
                        errln("Collation elements in contraction split does not match\n");
                        return;
                    }
                    ce = iter2.next();
                }
                j ++;
            }
            if (iter1.next() != CollationElementIterator.NULLORDER) {
                errln("Collation elements not exhausted\n");
                return;
            }
        }
        String rule = "& a < b < c < ch < d & c = ch / h";
        try {
            coll = new RuleBasedCollator(rule);
        } catch (Exception e) {
            errln("cannot create rulebased collator");
            return;
        }
        
        if (coll.compare(testdata2[0], testdata2[1]) != -1) {
            errln("Expected " + testdata2[0] + " < " + testdata2[1]);
            return;
        }
        if (coll.compare(testdata2[1], testdata2[2]) != -1) {
            errln("Expected " + testdata2[1] + " < " + testdata2[2]);
            return;
        }
        for (int i = 0; i < testrules3.length; i += 2) {
            RuleBasedCollator          coll1, coll2;
            CollationElementIterator iter1, iter2;
            char               ch = 0x0042;
            int            ce;
            rule = testrules3[i];
            try {
                coll1 = new RuleBasedCollator(rule);
            } catch (Exception e) {
                errln("Fail: cannot create rulebased collator, rule:" + rule);
                return;
            }
            rule = testrules3[i + 1];
            try {
                coll2 = new RuleBasedCollator(rule);
            } catch (Exception e) {
                errln("Collator creation failed " + testrules[i]);
                return;
            }
            try {
                iter1 = coll1.getCollationElementIterator(String.valueOf(ch));
                iter2 = coll2.getCollationElementIterator(String.valueOf(ch));
            } catch (Exception e) {
                errln("Collation iterator creation failed\n");
                return;
            }
            ce = iter1.next();
            
            while (ce != CollationElementIterator.NULLORDER) {
                if (ce != iter2.next()) {
                    errln("CEs does not match\n");
                    return;
                }
                ce = iter1.next();
            }
            if (iter2.next() != CollationElementIterator.NULLORDER) {
                errln("CEs not exhausted\n");
                return;
            }
        }
    }
    
    public void TestExpansion() {
        String[] testrules = {
            "&J << K / B & K << M",
            "&J << K / B << M"
        };
        String[] testdata = {
            "JA", "MA", "KA", "KC", "JC", "MC",
        };
        
        Collator  coll;
        for (int i = 0; i < testrules.length; i++) {
            // logln("Rule " + testrules[i] + " for testing\n");
            String rule = testrules[i];
            try {
                coll = new RuleBasedCollator(rule);
            } catch (Exception e) {
                warnln("Collator creation failed " + testrules[i]);
                return;
            }
            
            for (int j = 0; j < 5; j ++) {
                CollationTest.doTest(this, (RuleBasedCollator)coll, 
                                     testdata[j], testdata[j + 1], -1);
            }
        }
    }
    
    public void TestContractionEndCompare()
    {
        String rules = "&b=ch";
        String src = "bec";
        String tgt = "bech";
        Collator coll = null;
        try {
            coll = new RuleBasedCollator(rules);
        } catch (Exception e) {
            warnln("Collator creation failed " + rules);
            return;
        }
        CollationTest.doTest(this, (RuleBasedCollator)coll, src, tgt, 1);
    }
    
    public void TestLocaleRuleBasedCollators() {
        if (getInclusion() < 5) {
            // not serious enough to run this
            return;
        }
        Locale locale[] = Collator.getAvailableLocales();
        String prevrule = null;
        for (int i = 0; i < locale.length; i ++) {
            Locale l = locale[i];
            try {
                ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_COLLATION_BASE_NAME,l);
                String collkey = rb.getStringWithFallback("collations/default"); 
                ICUResourceBundle elements = rb.getWithFallback("collations/" + collkey);
                if (elements == null) {
                    continue;
                }
                String rule = null;
                /*
                  Object[][] colldata = (Object[][])elements;
                  // %%CollationBin
                  if (colldata[0][1] instanceof byte[]){
                  rule = (String)colldata[1][1];
                  }
                  else {
                  rule = (String)colldata[0][1];
                  }
                */
                rule = elements.getString("Sequence");   

                RuleBasedCollator col1 = 
                    (RuleBasedCollator)Collator.getInstance(l);
                if (!rule.equals(col1.getRules())) {
                    errln("Rules should be the same in the RuleBasedCollator and Locale");
                }
                if (rule != null && rule.length() > 0 
                    && !rule.equals(prevrule)) {
                    RuleBasedCollator col2 = new RuleBasedCollator(rule);
                    if (!col1.equals(col2)) {
                        errln("Error creating RuleBasedCollator from " +
                              "locale rules for " + l.toString());
                    }
                }
                prevrule = rule;
            } catch (Exception e) {
                warnln("Error retrieving resource bundle for testing: " + e.toString());
            }
        }
    }
    
    public void TestOptimize() {
        /* this is not really a test - just trying out 
         * whether copying of UCA contents will fail 
         * Cannot really test, since the functionality 
         * remains the same.
         */
        String rules[] = {
            "[optimize [\\uAC00-\\uD7FF]]"
        };
        String data[][] = {
            { "a", "b"}
        };
        int i = 0;
    
        for(i = 0; i<rules.length; i++) {
            genericRulesStarter(rules[i], data[i]);
        }
    }    
    
    public void TestIdenticalCompare() 
    {    
        try {
            RuleBasedCollator coll 
                = new RuleBasedCollator("& \uD800\uDC00 = \uD800\uDC01");
            String strA = "AA\u0300\u0316\uD800\uDC01";
            String strB = "A\u00c0\u0316\uD800\uDC00";
            coll.setStrength(Collator.IDENTICAL);
            CollationTest.doTest(this, coll, strA, strB, 1);
        } catch (Exception e) {
            warnln(e.getMessage());
        }
    }
    
    public void TestMergeSortKeys() 
    {
        String cases[] = {"abc", "abcd", "abcde"};
        String prefix = "foo";
        String suffix = "egg";
        CollationKey mergedPrefixKeys[] = new CollationKey[cases.length];
        CollationKey mergedSuffixKeys[] = new CollationKey[cases.length];
        
        Collator coll = Collator.getInstance(Locale.ENGLISH);
        genericLocaleStarter(Locale.ENGLISH, cases);
        
        int strength = Collator.PRIMARY;
        while (strength <= Collator.IDENTICAL) {
            coll.setStrength(strength);
            CollationKey prefixKey = coll.getCollationKey(prefix);
            CollationKey suffixKey = coll.getCollationKey(suffix);
            for (int i = 0; i < cases.length; i ++) {
                CollationKey key = coll.getCollationKey(cases[i]);
                mergedPrefixKeys[i] = prefixKey.merge(key);
                mergedSuffixKeys[i] = suffixKey.merge(key);
                if (mergedPrefixKeys[i].getSourceString() != null
                    || mergedSuffixKeys[i].getSourceString() != null) {
                    errln("Merged source string error: expected null");
                }
                if (i > 0) {
                    if (mergedPrefixKeys[i-1].compareTo(mergedPrefixKeys[i])
                        >= 0) {
                        errln("Error while comparing prefixed keys @ strength "
                              + strength);
                        errln(prettify(mergedPrefixKeys[i-1]));
                        errln(prettify(mergedPrefixKeys[i]));
                    }
                    if (mergedSuffixKeys[i-1].compareTo(mergedSuffixKeys[i]) 
                        >= 0) {
                        errln("Error while comparing suffixed keys @ strength "
                              + strength);
                        errln(prettify(mergedSuffixKeys[i-1]));
                        errln(prettify(mergedSuffixKeys[i]));
                    }
                }
            }
            if (strength == Collator.QUATERNARY) {
                strength = Collator.IDENTICAL;
            } 
            else {
                strength ++;
            }
        }       
    }
    
    public void TestVariableTop() 
    {
        // parseNextToken is not released as public so i create my own rules
        String rules = "& a < b < c < de < fg & hi = j";
        try {
            RuleBasedCollator coll = new RuleBasedCollator(rules);
            String tokens[] = {"a", "b", "c", "de", "fg", "hi", "j", "ab"};
            coll.setAlternateHandlingShifted(true);
            for (int i = 0; i < tokens.length; i ++) {
                int varTopOriginal = coll.getVariableTop();
                try {
                    int varTop = coll.setVariableTop(tokens[i]);
                    if (i > 4) {
                        errln("Token " + tokens[i] + " expected to fail");
                    }
                    if (varTop != coll.getVariableTop()) {
                        errln("Error setting and getting variable top");
                    }
                    CollationKey key1 = coll.getCollationKey(tokens[i]);
                    for (int j = 0; j < i; j ++) {
                        CollationKey key2 = coll.getCollationKey(tokens[j]);
                        if (key2.compareTo(key1) < 0) {
                            errln("Setting variable top shouldn't change the comparison sequence");
                        }
                        byte sortorder[] = key2.toByteArray();
                        if (sortorder.length > 0 
                            && (key2.toByteArray())[0] > 1) {
                            errln("Primary sort order should be 0");
                        }
                    }
                } catch (Exception e) {
                    CollationElementIterator iter 
                        = coll.getCollationElementIterator(tokens[i]);
                    /*int ce =*/ iter.next();
                    int ce2 = iter.next();
                    if (ce2 == CollationElementIterator.NULLORDER) {
                        errln("Token " + tokens[i] + " not expected to fail");
                    }
                    if (coll.getVariableTop() != varTopOriginal) {
                        errln("When exception is thrown variable top should "
                              + "not be changed");
                    }
                }
                coll.setVariableTop(varTopOriginal);
                if (varTopOriginal != coll.getVariableTop()) {
                    errln("Couldn't restore old variable top\n");
                }
            }
            
            // Testing calling with error set
            try {
                coll.setVariableTop("");
                errln("Empty string should throw an IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                logln("PASS: Empty string failed as expected");
            }
            try {
                coll.setVariableTop(null);
                errln("Null string should throw an IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                logln("PASS: null string failed as expected");
            }
        } catch (Exception e) {
            warnln("Error creating RuleBasedCollator");
        }
    }
    
    public void TestUCARules() 
    {
        try {
            // only root locale can have empty tailorings .. not English!
            RuleBasedCollator coll 
                = (RuleBasedCollator)Collator.getInstance(new Locale("","",""));
            String rule 
                = coll.getRules(false);
            if (!rule.equals("")) {
                errln("Empty rule string should have empty rules " + rule);
            }
            rule = coll.getRules(true);
            if (rule.equals("")) {
                errln("UCA rule string should not be empty");
            }
            coll = new RuleBasedCollator(rule);
        } catch (Exception e) {
            warnln(e.getMessage());
        }
    }
    
    /**
     * Jitterbug 2726
     */
    public void TestShifted()
    {
        RuleBasedCollator collator = (RuleBasedCollator) Collator.getInstance();
        collator.setStrength(Collator.PRIMARY);
        collator.setAlternateHandlingShifted(true);
        CollationTest.doTest(this, collator, " a", "a", 0); // works properly
        CollationTest.doTest(this, collator, "a", "a ", 0); // inconsistent results
    }
    
    /**
     * Test for CollationElementIterator previous and next for the whole set of
     * unicode characters with normalization on.
     */
    public void TestNumericCollation()
    {
        String basicTestStrings[] = {"hello1", "hello2", "hello123456"};
        String preZeroTestStrings[] = {"avery1",
                                       "avery01",
                                       "avery001",
                                       "avery0001"};
        String thirtyTwoBitNumericStrings[] = {"avery42949672960",
                                               "avery42949672961",
                                               "avery42949672962",
                                               "avery429496729610"};
    
        String supplementaryDigits[] = {"\uD835\uDFCE", // 0 
                                        "\uD835\uDFCF", // 1 
                                        "\uD835\uDFD0", // 2 
                                        "\uD835\uDFD1", // 3 
                                        "\uD835\uDFCF\uD835\uDFCE", // 10 
                                        "\uD835\uDFCF\uD835\uDFCF", // 11 
                                        "\uD835\uDFCF\uD835\uDFD0", // 12 
                                        "\uD835\uDFD0\uD835\uDFCE", // 20 
                                        "\uD835\uDFD0\uD835\uDFCF", // 21 
                                        "\uD835\uDFD0\uD835\uDFD0" // 22 
        };
    
        String foreignDigits[] = {"\u0661",
                                  "\u0662",
                                  "\u0663",
                                  "\u0661\u0660",
                                  "\u0661\u0662",
                                  "\u0661\u0663",
                                  "\u0662\u0660",
                                  "\u0662\u0662",
                                  "\u0662\u0663",
                                  "\u0663\u0660",
                                  "\u0663\u0662",
                                  "\u0663\u0663"
        };
    
        // Open our collator.
        RuleBasedCollator coll 
            = (RuleBasedCollator)Collator.getInstance(Locale.ENGLISH);
        String att[] = {"NumericCollation"};
        Boolean val[] = {Boolean.TRUE};
        genericLocaleStarterWithOptions(Locale.ENGLISH, basicTestStrings, att,
                                        val);
        genericLocaleStarterWithOptions(Locale.ENGLISH, 
                                        thirtyTwoBitNumericStrings, att, val);
        genericLocaleStarterWithOptions(Locale.ENGLISH, foreignDigits, att, 
                                        val);
        genericLocaleStarterWithOptions(Locale.ENGLISH, supplementaryDigits, 
                                        att, val);    
    
        // Setting up our collator to do digits.
        coll.setNumericCollation(true);
    
        // Testing that prepended zeroes still yield the correct collation 
        // behavior. 
        // We expect that every element in our strings array will be equal.
        for (int i = 0; i < preZeroTestStrings.length - 1; i ++) {
            for (int j = i + 1; j < preZeroTestStrings.length; j ++) {
                CollationTest.doTest(this, coll, preZeroTestStrings[i], 
                                     preZeroTestStrings[j],0);
            }
        }

        //cover setNumericCollationDefault, getNumericCollation
        assertTrue("The Numeric Collation setting is on", coll.getNumericCollation());
        coll.setNumericCollationDefault();
        logln("After set Numeric to default, the setting is: " + coll.getNumericCollation());
    }
        
    public void Test3249()
    {
        String rule = "&x < a &z < a";
        try {
            RuleBasedCollator coll = new RuleBasedCollator(rule);
            if(coll!=null){
                logln("Collator did not throw an exception");   
            }
        } catch (Exception e) {
            warnln("Error creating RuleBasedCollator with " + rule + " failed");
        }
    }
    
    public void TestTibetanConformance() 
    {  
        String test[] = {"\u0FB2\u0591\u0F71\u0061", "\u0FB2\u0F71\u0061"};
        try {
            Collator coll = Collator.getInstance();
            coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            if (coll.compare(test[0], test[1]) != 0) {
                errln("Tibetan comparison error");
            }
            CollationTest.doTest(this, (RuleBasedCollator)coll, 
                                 test[0], test[1], 0);
        } catch (Exception e) {
            warnln("Error creating UCA collator");
        }
    }
    
    public void TestJ3347()
    {
        try {
            Collator coll = Collator.getInstance(Locale.FRENCH);
            ((RuleBasedCollator)coll).setAlternateHandlingShifted(true);
            if (coll.compare("6", "!6") != 0) {
                errln("Jitterbug 3347 failed");
            }
        } catch (Exception e) {
            warnln("Error creating UCA collator");
        }
    }
    
    public void TestPinyinProblem()
    {
        String test[] = { "\u4E56\u4E56\u7761", "\u4E56\u5B69\u5B50" };
        genericLocaleStarter(new Locale("zh", "", "PINYIN"), test);     
    }
    
    static final long topByte = 0xFF000000L;
    static final long bottomByte = 0xFFL;
    static final long fourBytes = 0xFFFFFFFFL;
    
    static final int MAX_INPUT = 0x220001; // 2 * Unicode range + 2
    
    private void show(int i, ImplicitCEGenerator imp) {
        if (i >= 0 && i <= MAX_INPUT) {
            logln(Utility.hex(i) + "\t" + Utility.hex(imp.getImplicitFromRaw(i) & fourBytes));
        } 
    }

    private void throwError(String title, int cp, ImplicitCEGenerator imp) {
        throw new IllegalArgumentException(title + "\t" + Utility.hex(cp, 6) + "\t" + Utility.hex(imp.getImplicitFromRaw(cp) & fourBytes));
    }
    
    private void throwError(String title, long ce) {
        errln(title + "\t" + Utility.hex(ce & fourBytes));
    }
    
    public void TestImplicitGeneration()
    {
        logln("Start");
        try {
            ImplicitCEGenerator foo = new ImplicitCEGenerator(0xE0, 0xE4);
                
            //int x = foo.getRawImplicit(0xF810);
            foo.getRawFromImplicit(0xE20303E7);

            //int gap4 = foo.getGap4();
            //logln("Gap4: " + gap4); 
            //int gap3 = foo.getGap3();
            //int minTrail = foo.getMinTrail();
            //int maxTrail = foo.getMaxTrail();
            long last = 0;
            long current;
            for (int i = 0; i <= MAX_INPUT; ++i) {
                current = foo.getImplicitFromRaw(i) & fourBytes;
                        
                // check that it round-trips AND that all intervening ones are illegal
                int roundtrip = foo.getRawFromImplicit((int)current);
                if (roundtrip != i) {
                    throwError("No roundtrip", i, foo); 
                }
                if (last != 0) {
                    for (long j = last + 1; j < current; ++j) {
                        roundtrip = foo.getRawFromImplicit((int)j);
                        // raise an error if it *doesn't* find an error
                        if (roundtrip != -1) {
                            throwError("Fails to recognize illegal", j);
                        }
                    }
                }
                // now do other consistency checks
                long lastBottom = last & bottomByte;
                long currentBottom = current & bottomByte;
                long lastTop = last & topByte;
                long currentTop = current & topByte;
                        
                // do some consistency checks
                /*
                  long gap = current - last;               
                  if (currentBottom != 0) { // if we are a 4-byte
                  // gap has to be at least gap4
                  // and gap from minTrail, maxTrail has to be at least gap4
                  if (gap <= gap4) foo.throwError("Failed gap4 between", i);
                  if (currentBottom < minTrail + gap4) foo.throwError("Failed gap4 before", i);
                  if (currentBottom > maxTrail - gap4) foo.throwError("Failed gap4 after", i);
                  } else { // we are a three-byte
                  gap = gap >> 8; // move gap down for comparison.
                  long current3Bottom = (current >> 8) & bottomByte;
                  if (gap <= gap3) foo.throwError("Failed gap3 between ", i);
                  if (current3Bottom < minTrail + gap3) foo.throwError("Failed gap3 before", i);
                  if (current3Bottom > maxTrail - gap3) foo.throwError("Failed gap3 after", i);
                  }
                */
                // print out some values for spot-checking
                if (lastTop != currentTop || i == 0x10000 || i == 0x110000) {
                    show(i-3, foo);
                    show(i-2, foo);
                    show(i-1, foo);
                    if (i == 0) {
                        // do nothing
                    } else if (lastBottom == 0 && currentBottom != 0) {
                        logln("+ primary boundary, 4-byte CE's below");
                    } else if (lastTop != currentTop) {
                        logln("+ primary boundary");
                    }
                    show(i, foo);
                    show(i+1, foo);
                    show(i+2, foo);
                    logln("...");
                }
                last = current;
                if(foo.getCodePointFromRaw(foo.getRawFromCodePoint(i)) != i) {
                    errln("No raw <-> code point roundtrip for "+Utility.hex(i));
                }                       
            }
            show(MAX_INPUT-2, foo);
            show(MAX_INPUT-1, foo);
            show(MAX_INPUT, foo);
        } catch (Exception e) {
            e.printStackTrace();
            warnln(e.getMessage());
        } finally {
            logln("End");
        }
    }

    /* supercedes TestJ784 */
    public void TestBeforePinyin() {
        String rules = 
            "&[before 2]A << \u0101  <<< \u0100 << \u00E1 <<< \u00C1 << \u01CE <<< \u01CD << \u00E0 <<< \u00C0" +
            "&[before 2]e << \u0113 <<< \u0112 << \u00E9 <<< \u00C9 << \u011B <<< \u011A << \u00E8 <<< \u00C8" +
            "&[before 2] i << \u012B <<< \u012A << \u00ED <<< \u00CD << \u01D0 <<< \u01CF << \u00EC <<< \u00CC" +
            "&[before 2] o << \u014D <<< \u014C << \u00F3 <<< \u00D3 << \u01D2 <<< \u01D1 << \u00F2 <<< \u00D2" +
            "&[before 2]u << \u016B <<< \u016A << \u00FA <<< \u00DA << \u01D4 <<< \u01D3 << \u00F9 <<< \u00D9" +
            "&U << \u01D6 <<< \u01D5 << \u01D8 <<< \u01D7 << \u01DA <<< \u01D9 << \u01DC <<< \u01DB << \u00FC";
    
        String test[] = {
            "l\u0101",
            "la",
            "l\u0101n",
            "lan ",
            "l\u0113",
            "le",
            "l\u0113n",
            "len"
        };
    
        String test2[] = {
            "x\u0101",
            "x\u0100",
            "X\u0101",
            "X\u0100",
            "x\u00E1",
            "x\u00C1",
            "X\u00E1",
            "X\u00C1",
            "x\u01CE",
            "x\u01CD",
            "X\u01CE",
            "X\u01CD",
            "x\u00E0",
            "x\u00C0",
            "X\u00E0",
            "X\u00C0",
            "xa",
            "xA",
            "Xa",
            "XA",
            "x\u0101x",
            "x\u0100x",
            "x\u00E1x",
            "x\u00C1x",
            "x\u01CEx",
            "x\u01CDx",
            "x\u00E0x",
            "x\u00C0x",
            "xax",
            "xAx"
        };
        /* TODO: port builder fixes to before */
        genericRulesStarter(rules, test);
        genericLocaleStarter(new Locale("zh","",""), test);
        genericRulesStarter(rules, test2);
        genericLocaleStarter(new Locale("zh","",""), test2);
    }

    public void TestUpperFirstQuaternary()
    {
      String tests[] = { "B", "b", "Bb", "bB" };
      String[] att = { "strength", "UpperFirst" };
      Object attVals[] = { new Integer(Collator.QUATERNARY), Boolean.TRUE };
      genericLocaleStarterWithOptions(new Locale("root","",""), tests, att, attVals);
    }
    
    public void TestJ4960()
    {
        String tests[] = { "\\u00e2T", "aT" };
        String att[] = { "strength", "CaseLevel" };
        Object attVals[] = { new Integer(Collator.PRIMARY), Boolean.TRUE };
        String tests2[] = { "a", "A" };
        String rule = "&[first tertiary ignorable]=A=a";
        String att2[] = { "CaseLevel" };        
        Object attVals2[] = { Boolean.TRUE };
        // Test whether we correctly ignore primary ignorables on case level when
        // we have only primary & case level
        genericLocaleStarterWithOptionsAndResult(new Locale("root", ""), tests, att, attVals, 0);
        // Test whether ICU4J will make case level for sortkeys that have primary strength
        // and case level
        genericLocaleStarterWithOptions(new Locale("root", ""), tests2, att, attVals);
        // Test whether completely ignorable letters have case level info (they shouldn't)
        genericRulesStarterWithOptionsAndResult(rule, tests2, att2, attVals2, 0);        
    }
    
    public void TestJB5298(){
        ULocale[] locales = Collator.getAvailableULocales();
        logln("Number of collator locales returned : " + locales.length);
        // double-check keywords
        String[] keywords = Collator.getKeywords();
        if (keywords.length != 1 || !keywords[0].equals("collation")) {
            throw new IllegalArgumentException("internal collation error");
        }
    
        String[] values = Collator.getKeywordValues("collation");
        log("Collator.getKeywordValues returned: ");
        for(int i=0; i<values.length;i++){
            log(values[i]+", ");
        }
        logln("");
        logln("Number of collator values returned : " + values.length);
        
        Set foundValues = new TreeSet(Arrays.asList(values));
        
        for (int i = 0; i < locales.length; ++i) {
          for (int j = 0; j < values.length; ++j) {
            ULocale tryLocale = values[j].equals("standard") 
            ? locales[i] : new ULocale(locales[i] + "@collation=" + values[j]); 
            // only append if not standard
            ULocale canon = Collator.getFunctionalEquivalent("collation",tryLocale);
            if (!canon.equals(tryLocale)) {
                continue; // has a different 
            }else {// functional equivalent, so skip
                logln(tryLocale + " : "+canon+", ");
            }
            String can = canon.toString();
            int idx = can.indexOf("@collation=");
            String val = idx >= 0 ? can.substring(idx+11, can.length()) : "";
            if(val.length()>0 && !foundValues.contains(val)){
                errln("Unknown collation found "+ can);
            }
          }        
        }
        logln(" ");
    }

    public void
    TestJ5367()
    {
        String[] test = { "a", "y" };
        String rules = "&Ny << Y &[first secondary ignorable] <<< a";
        genericRulesStarter(rules, test);        
    }
    
    public void
    TestVI5913()
    {

        String rules[] = {
                "&a < \u00e2 <<< \u00c2",
                "&a < \u1FF3 ",  // OMEGA WITH YPOGEGRAMMENI
                "&s < \u0161 ",  // &s < s with caron
                "&z < a\u00EA",  // &z < a+e with circumflex
        };
        String cases[][] = {
            { "\u1EAC", "A\u0323\u0302", "\u1EA0\u0302", "\u00C2\u0323", }, 
            { "\u1FA2", "\u03C9\u0313\u0300\u0345", "\u1FF3\u0313\u0300", 
              "\u1F60\u0300\u0345", "\u1f62\u0345", "\u1FA0\u0300", },
            { "\u1E63\u030C", "s\u0323\u030C", "s\u030C\u0323"},
            { "a\u1EC7", //  a+ e with dot below and circumflex
              "a\u1EB9\u0302", // a + e with dot below + combining circumflex
              "a\u00EA\u0323", // a + e with circumflex + combining dot below
            }
        };
        
        
        for(int i = 0; i < rules.length; i++) {
            
            RuleBasedCollator coll = null;
            try {
                coll = new RuleBasedCollator(rules[i]);
            } catch (Exception e) {
                warnln("Unable to open collator with rules " + rules[i]);
            }

            logln("Test case["+i+"]:");
            CollationKey expectingKey = coll.getCollationKey(cases[i][0]);
            for (int j=1; j<cases[i].length; j++) {
                CollationKey key = coll.getCollationKey(cases[i][j]);
                if ( key.compareTo(expectingKey)!=0) {
                    errln("Error! Test case["+i+"]:"+"source:" + key.getSourceString());
                    errln("expecting:"+prettify(expectingKey)+ "got:"+  prettify(key));
                }
                logln("   Key:"+  prettify(key));
            }
        }   
        
        
        RuleBasedCollator vi_vi = null;
        try {
            vi_vi = (RuleBasedCollator)Collator.getInstance(
                                                      new Locale("vi", ""));
            logln("VI sort:");
            CollationKey expectingKey = vi_vi.getCollationKey(cases[0][0]);
            for (int j=1; j<cases[0].length; j++) {
                CollationKey key = vi_vi.getCollationKey(cases[0][j]);
                if ( key.compareTo(expectingKey)!=0) {
                    // TODO (claireho): change the logln to errln after vi.res is up-to-date.
                    // errln("source:" + key.getSourceString());
                    // errln("expecting:"+prettify(expectingKey)+ "got:"+  prettify(key));
                    logln("Error!! in Vietnese sort - source:" + key.getSourceString());
                    logln("expecting:"+prettify(expectingKey)+ "got:"+  prettify(key));
                }
                // logln("source:" + key.getSourceString());
                logln("   Key:"+  prettify(key));
            }
        } catch (Exception e) {
            warnln("Error creating Vietnese collator");
            return;
        }
        
    }
    
    
    public void Test6179()
    {
        String rules[] = {
                "&[last primary ignorable]<< a  &[first primary ignorable]<<b ",
                "&[last secondary ignorable]<<< a &[first secondary ignorable]<<<b",  
        };
        // defined in UCA5.1
        String firstPrimIgn = "\u0332";  
        String lastPrimIgn = "\uD800\uDDFD";
        String firstVariable = "\u0009";
        byte[] secIgnKey = {1,1,4,0};
        
        int i=0; 
        {
            
            RuleBasedCollator coll = null;
            try {
                coll = new RuleBasedCollator(rules[i]);
            } catch (Exception e) {
                warnln("Unable to open collator with rules " + rules[i]);
            }

            logln("Test rule["+i+"]"+rules[i]);
            
            CollationKey keyA = coll.getCollationKey("a");
            logln("Key for \"a\":"+  prettify(keyA));
            if (keyA.compareTo(coll.getCollationKey(lastPrimIgn))<=0) {
                CollationKey key = coll.getCollationKey(lastPrimIgn);
                logln("Collation key for 0xD800 0xDDFD: "+prettify(key));
                errln("Error! String \"a\" must be greater than \uD800\uDDFD -"+
                      "[Last Primary Ignorable]");
            }
            if (keyA.compareTo(coll.getCollationKey(firstVariable))>=0) {
                CollationKey key = coll.getCollationKey(firstVariable);
                logln("Collation key for 0x0009: "+prettify(key));
                errln("Error! String \"a\" must be less than 0x0009 - [First Variable]");
            }
            CollationKey keyB = coll.getCollationKey("b");
            logln("Key for \"b\":"+  prettify(keyB));
            if (keyB.compareTo(coll.getCollationKey(firstPrimIgn))<=0) {
                CollationKey key = coll.getCollationKey(firstPrimIgn);
                logln("Collation key for 0x0332: "+prettify(key));
                errln("Error! String \"b\" must be greater than 0x0332 -"+
                      "[First Primary Ignorable]");
            }
            if (keyB.compareTo(coll.getCollationKey(firstVariable))>=0) {
                CollationKey key = coll.getCollationKey(firstVariable);
                logln("Collation key for 0x0009: "+prettify(key));
                errln("Error! String \"b\" must be less than 0x0009 - [First Variable]");
            }
        }
        {
            i=1;   
            RuleBasedCollator coll = null;
            try {
                coll = new RuleBasedCollator(rules[i]);
            } catch (Exception e) {
                warnln("Unable to open collator with rules " + rules[i]);
            }

            logln("Test rule["+i+"]"+rules[i]);
            
            CollationKey keyA = coll.getCollationKey("a");
            logln("Key for \"a\":"+  prettify(keyA));
            byte[] keyAInBytes = keyA.toByteArray();
            for (int j=0; j<keyAInBytes.length && j<secIgnKey.length; j++) {
                if (keyAInBytes[j]!=secIgnKey[j]) {
                    if ((char)keyAInBytes[j]<=(char)secIgnKey[j]) {
                        logln("Error! String \"a\" must be greater than [Last Secondary Ignorable]");
                    }
                    break;
                }
            }
            if (keyA.compareTo(coll.getCollationKey(firstVariable))>=0) {
                errln("Error! String \"a\" must be less than 0x0009 - [First Variable]");
                CollationKey key = coll.getCollationKey(firstVariable);
                logln("Collation key for 0x0009: "+prettify(key));
            }
            CollationKey keyB = coll.getCollationKey("b");
            logln("Key for \"b\":"+  prettify(keyB));
            byte[] keyBInBytes = keyB.toByteArray();
            for (int j=0; j<keyBInBytes.length && j<secIgnKey.length; j++) {
                if (keyBInBytes[j]!=secIgnKey[j]) {
                    if ((char)keyBInBytes[j]<=(char)secIgnKey[j]) {
                        errln("Error! String \"b\" must be greater than [Last Secondary Ignorable]");
                    }
                    break;
                }
            }
            if (keyB.compareTo(coll.getCollationKey(firstVariable))>=0) {
                CollationKey key = coll.getCollationKey(firstVariable);
                logln("Collation key for 0x0009: "+prettify(key));
                errln("Error! String \"b\" must be less than 0x0009 - [First Variable]");
            }
        }   
    }
    
    public void TestUCAPrecontext()
    {
        String rules[] = {
                "& \u00B7<a ",
                "& L\u00B7 << a", // 'a' is an expansion. 
        };
        String cases[] = {
            "\u00B7", 
            "\u0387", 
            "a",
            "l",
            "L\u0332",
            "l\u00B7",
            "l\u0387",
            "L\u0387",
            "la\u0387",
            "La\u00b7",
        };

        // Test en sort
        RuleBasedCollator en = null;
        
        logln("EN sort:");
        try {
            en = (RuleBasedCollator)Collator.getInstance(
                    new Locale("en", ""));
            for (int j=0; j<cases.length; j++) {
                CollationKey key = en.getCollationKey(cases[j]);
                if (j>0) {
                    CollationKey prevKey = en.getCollationKey(cases[j-1]);
                    if (key.compareTo(prevKey)<0) {
                        errln("Error! EN test["+j+"]:"+"source:" + cases[j]+
                        "is not greater than previous test.");
                    }
                }
                /*
                if ( key.compareTo(expectingKey)!=0) {
                    errln("Error! Test case["+i+"]:"+"source:" + key.getSourceString());
                    errln("expecting:"+prettify(expectingKey)+ "got:"+  prettify(key));
                }
                */
                logln("String:"+cases[j]+"   Key:"+  prettify(key));
            }
        } catch (Exception e) {
            warnln("Error creating Vietnese collator");
            return;
        }
        
        // Test ja sort
        RuleBasedCollator ja = null;
        logln("JA sort:");
        try {
            ja = (RuleBasedCollator)Collator.getInstance(
                    new Locale("ja", ""));
            for (int j=0; j<cases.length; j++) {
                CollationKey key = ja.getCollationKey(cases[j]);
                if (j>0) {
                    CollationKey prevKey = ja.getCollationKey(cases[j-1]);
                    if (key.compareTo(prevKey)<0) {
                        errln("Error! JA test["+j+"]:"+"source:" + cases[j]+
                        "is not greater than previous test.");
                    }
                }
                logln("String:"+cases[j]+"   Key:"+  prettify(key));
            }
        } catch (Exception e) {
            warnln("Error creating Vietnese collator");
            return;
        }
        for(int i = 0; i < rules.length; i++) {
            
            RuleBasedCollator coll = null;
            logln("Tailoring rule:"+rules[i]);
            try {
                coll = new RuleBasedCollator(rules[i]);
            } catch (Exception e) {
                warnln("Unable to open collator with rules " + rules[i]);
            }

            for (int j=0; j<cases.length; j++) {
                CollationKey key = coll.getCollationKey(cases[j]);
                if (j>0) {
                    CollationKey prevKey = coll.getCollationKey(cases[j-1]);
                    if (i==1 && j==3) {
                        if (key.compareTo(prevKey)>0) {
                            errln("Error! Rule:"+rules[i]+" test["+j+"]:"+"source:"+
                            cases[j]+"is not greater than previous test.");
                        }
                    }
                    else {
                        if (key.compareTo(prevKey)<0) {
                            errln("Error! Rule:"+rules[i]+" test["+j+"]:"+"source:"+ 
                            cases[j]+"is not greater than previous test.");
                        }
                    }
                }
                logln("String:"+cases[j]+"   Key:"+  prettify(key));
            }
        }   
    }

    public void TestSameStrengthList() {
        String[] testSourceCases = {
            "\u0061",
            "\u0061",
            "\u006c\u0061",
            "\u0061\u0061\u0061",
            "\u0062",
        };
        
        String[] testTargetCases = {
            "\u0031",
            "\u0066",
            "\u006b\u0062",
            "\u0031\u0032\u0033",
            "\u007a",
        };
        
        int[] results = {
            0,
            -1,
            -1,
            0,
            -1
        };

        Collator  myCollation;
        String rules = "&a<*bcd &b<<*klm &k<<<*xyz &a=*123";
        try {
            myCollation = new RuleBasedCollator(rules);
        } catch (Exception e) {
            warnln("ERROR: in creation of rule based collator");
            return;
        }
        // logln("Testing some A letters, for some reason");
        myCollation.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
        myCollation.setStrength(Collator.TERTIARY);
        for (int i = 0; i < 5 ; i++)
        {
            CollationTest.doTest(this, (RuleBasedCollator)myCollation, 
                                 testSourceCases[i], testTargetCases[i], 
                                 results[i]);
        }
    }

    /*
     * Tests the method public boolean equals(Object target) in CollationKey
     */
    public void TestCollationKeyEquals() {
        CollationKey ck = new CollationKey("", (byte[]) null);

        // Tests when "if (!(target instanceof CollationKey))" is true
        if (ck.equals(new Object())) {
            errln("CollationKey.equals() was not suppose to return false "
                    + "since it is comparing to a non Collation Key object.");
        }
        if (ck.equals("")) {
            errln("CollationKey.equals() was not suppose to return false "
                    + "since it is comparing to a non Collation Key object.");
        }
        if (ck.equals(0)) {
            errln("CollationKey.equals() was not suppose to return false "
                    + "since it is comparing to a non Collation Key object.");
        }
        if (ck.equals(0.0)) {
            errln("CollationKey.equals() was not suppose to return false "
                    + "since it is comparing to a non Collation Key object.");
        }

        // Tests when "if (target == null)" is true
        if (ck.equals((CollationKey) null)) {
            errln("CollationKey.equals() was not suppose to return false "
                    + "since it is comparing to a null Collation Key object.");
        }
    }

    /*
     * Tests the method public int hashCode() in CollationKey
     */
    public void TestCollationKeyHashCode() {
        CollationKey ck = new CollationKey("", (byte[]) null);

        // Tests when "if (m_key_ == null)" is true
        if (ck.hashCode() != 1) {
            errln("CollationKey.hashCode() was suppose to return 1 "
                    + "when m_key is null due a null parameter in the " + "constructor.");
        }
    }

    /*
     * Tests the method public CollationKey getBound(int boundType, int noOfLevels)
     */
    public void TestGetBound() {
        CollationKey ck = new CollationKey("", (byte[]) null);

        // Tests when "if (noOfLevels > Collator.PRIMARY)" is false
        // Tests when "default: " is true for "switch (boundType)"
        try {
            ck.getBound(BoundMode.COUNT, -1);
            errln("CollationKey.getBound(int,int) was suppose to return an "
                    + "exception for an invalid boundType value.");
        } catch (Exception e) {
        }

        // Tests when "if (noOfLevels > 0)"
        byte b[] = {};
        CollationKey ck1 = new CollationKey("", b);
        try {
            ck1.getBound(0, 1);
            errln("CollationKey.getBound(int,int) was suppose to return an "
                    + "exception a value of noOfLevels that exceeds expected.");
        } catch (Exception e) {
        }
    }

    /*
     * Tests the method public CollationKey merge(CollationKey source)
     */
    public void TestMerge() {
        byte b[] = {};
        CollationKey ck = new CollationKey("", b);

        // Tests when "if (source == null || source.getLength() == 0)" is true
        try {
            ck.merge(null);
            errln("Collationkey.merge(CollationKey) was suppose to return " + "an exception for a null parameter.");
        } catch (Exception e) {
        }
        try {
            ck.merge(ck);
            errln("Collationkey.merge(CollationKey) was suppose to return " + "an exception for a null parameter.");
        } catch (Exception e) {
        }
    }
    
    /* Test the method public int compareTo(RawCollationKey rhs) */
    public void TestRawCollationKeyCompareTo(){
        RawCollationKey rck = new RawCollationKey();
        byte[] b = {(byte) 10, (byte) 20};
        RawCollationKey rck100 = new RawCollationKey(b, 2);
        
        if(rck.compareTo(rck) != 0){
            errln("RawCollatonKey.compareTo(RawCollationKey) was suppose to return 0 " +
                    "for two idential RawCollationKey objects.");
        }
        
        if(rck.compareTo(rck100) == 0){
            errln("RawCollatonKey.compareTo(RawCollationKey) was not suppose to return 0 " +
                    "for two different RawCollationKey objects.");
        }
    }
}
