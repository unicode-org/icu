/*
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/** 
 * Port From:   ICU4C v1.8.1 : format : IntlTestDecimalFormatAPI
 * Source File: $ICU4CRoot/source/test/intltest/dcfmapts.cpp
 **/

package com.ibm.icu.dev.test.format;

import java.util.Locale;
import java.text.ParsePosition;
import java.text.Format;
import com.ibm.icu.text.*;
import java.text.FieldPosition;

// This is an API test, not a unit test.  It doesn't test very many cases, and doesn't
// try to test the full functionality.  It just calls each function in the class and
// verifies that it works on a basic level.
public class IntlTestDecimalFormatAPIC extends com.ibm.icu.dev.test.TestFmwk {
    
    public static void main(String[] args)  throws Exception {
        new IntlTestDecimalFormatAPIC().run(args);
    }

    // This test checks various generic API methods in DecimalFormat to achieve 100% API coverage.
    public void TestAPI() {
        
        logln("DecimalFormat API test---");
        logln("");
        Locale.setDefault(Locale.ENGLISH);
    
        // ======= Test constructors
    
        logln("Testing DecimalFormat constructors");
    
        DecimalFormat def = new DecimalFormat();
    
        final String pattern = new String("#,##0.# FF");
        DecimalFormat pat = null;
        try {
            pat = new DecimalFormat(pattern);
        } catch (IllegalArgumentException e) {
            errln("ERROR: Could not create DecimalFormat (pattern)");
        }
    
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.FRENCH);
    
        DecimalFormat cust1 = new DecimalFormat(pattern, symbols);
    
        // ======= Test clone(), assignment, and equality
    
        logln("Testing clone() and equality operators");
    
        Format clone = (Format) def.clone();
        if (!def.equals(clone)) {
            errln("ERROR: Clone() failed");
        }
    
        // ======= Test various format() methods
    
        logln("Testing various format() methods");
    
        //        final double d = -10456.0037; // this appears as -10456.003700000001 on NT
        //        final double d = -1.04560037e-4; // this appears as -1.0456003700000002E-4 on NT
        final double d = -10456.00370000000000; // this works!
        final long l = 100000000;
        logln("" + Double.toString(d) + " is the double value");
    
        StringBuffer res1 = new StringBuffer();
        StringBuffer res2 = new StringBuffer();
        StringBuffer res3 = new StringBuffer();
        StringBuffer res4 = new StringBuffer();
        FieldPosition pos1 = new FieldPosition(0);
        FieldPosition pos2 = new FieldPosition(0);
        FieldPosition pos3 = new FieldPosition(0);
        FieldPosition pos4 = new FieldPosition(0);
    
        res1 = def.format(d, res1, pos1);
        logln("" + Double.toString(d) + " formatted to " + res1);
    
        res2 = pat.format(l, res2, pos2);
        logln("" + l + " formatted to " + res2);
    
        res3 = cust1.format(d, res3, pos3);
        logln("" + Double.toString(d) + " formatted to " + res3);
    
        res4 = cust1.format(l, res4, pos4);
        logln("" + l + " formatted to " + res4);
    
        // ======= Test parse()
    
        logln("Testing parse()");
    
        String text = new String("-10,456.0037");
        ParsePosition pos = new ParsePosition(0);
        String patt = new String("#,##0.#");
        pat.applyPattern(patt);
        double d2 = pat.parse(text, pos).doubleValue();
        if (d2 != d) {
            errln(
                "ERROR: Roundtrip failed (via parse(" + Double.toString(d2) + " != " + Double.toString(d) + ")) for " + text); 
        }
        logln(text + " parsed into " + (long) d2);
    
        // ======= Test getters and setters
    
        logln("Testing getters and setters");
    
        final DecimalFormatSymbols syms = pat.getDecimalFormatSymbols();
        def.setDecimalFormatSymbols(syms);
        if (!pat.getDecimalFormatSymbols().equals(def.getDecimalFormatSymbols())) {
            errln("ERROR: set DecimalFormatSymbols() failed");
        }
    
        String posPrefix;
        pat.setPositivePrefix("+");
        posPrefix = pat.getPositivePrefix();
        logln("Positive prefix (should be +): " + posPrefix);
        if (posPrefix != "+") {
            errln("ERROR: setPositivePrefix() failed");
        }
    
        String negPrefix;
        pat.setNegativePrefix("-");
        negPrefix = pat.getNegativePrefix();
        logln("Negative prefix (should be -): " + negPrefix);
        if (negPrefix != "-") {
            errln("ERROR: setNegativePrefix() failed");
        }
    
        String posSuffix;
        pat.setPositiveSuffix("_");
        posSuffix = pat.getPositiveSuffix();
        logln("Positive suffix (should be _): " + posSuffix);
        if (posSuffix != "_") {
            errln("ERROR: setPositiveSuffix() failed");
        }
    
        String negSuffix;
        pat.setNegativeSuffix("~");
        negSuffix = pat.getNegativeSuffix();
        logln("Negative suffix (should be ~): " + negSuffix);
        if (negSuffix != "~") {
            errln("ERROR: setNegativeSuffix() failed");
        }
    
        long multiplier = 0;
        pat.setMultiplier(8);
        multiplier = pat.getMultiplier();
        logln("Multiplier (should be 8): " + multiplier);
        if (multiplier != 8) {
            errln("ERROR: setMultiplier() failed");
        }
    
        int groupingSize = 0;
        pat.setGroupingSize(2);
        groupingSize = pat.getGroupingSize();
        logln("Grouping size (should be 2): " + (long) groupingSize);
        if (groupingSize != 2) {
            errln("ERROR: setGroupingSize() failed");
        }
    
        pat.setDecimalSeparatorAlwaysShown(true);
        boolean tf = pat.isDecimalSeparatorAlwaysShown();
        logln(
            "DecimalSeparatorIsAlwaysShown (should be true) is " + (tf ? "true" : "false")); 
        if (tf != true) {
            errln("ERROR: setDecimalSeparatorAlwaysShown() failed");
        }
    
        String funkyPat;
        funkyPat = pat.toPattern();
        logln("Pattern is " + funkyPat);
    
        String locPat;
        locPat = pat.toLocalizedPattern();
        logln("Localized pattern is " + locPat);
    
        // ======= Test applyPattern()
    
        logln("Testing applyPattern()");
    
        String p1 = new String("#,##0.0#;(#,##0.0#)");
        logln("Applying pattern " + p1);
        pat.applyPattern(p1);
        String s2;
        s2 = pat.toPattern();
        logln("Extracted pattern is " + s2);
        if (!s2.equals(p1)) {
            errln("ERROR: toPattern() result did not match pattern applied");
        }
    
        String p2 = new String("#,##0.0# FF;(#,##0.0# FF)");
        logln("Applying pattern " + p2);
        pat.applyLocalizedPattern(p2);
        String s3;
        s3 = pat.toLocalizedPattern();
        logln("Extracted pattern is " + s3);
        if (!s3.equals(p2)) {
            errln("ERROR: toLocalizedPattern() result did not match pattern applied");
        }
    
        // ======= Test getStaticClassID()
    
        //        logln("Testing instanceof()");
    
        //        try {
        //           NumberFormat test = new DecimalFormat();
    
        //            if (! (test instanceof DecimalFormat)) {
        //                errln("ERROR: instanceof failed");
        //            }
        //        }
        //        catch (Exception e) {
        //            errln("ERROR: Couldn't create a DecimalFormat");
        //        }
       
    }

    public void TestRounding() {
        double Roundingnumber = 2.55;
        double Roundingnumber1 = -2.55;
        //+2.55 results   -2.55 results
        double result[] = {
            3, -3,   
            2, -2, 
            3, -2, 
            2, -3, 
            3, -3, 
            3, -3, 
            3, -3 
        };
        DecimalFormat pat = new DecimalFormat();
        String s = "";
        s = pat.toPattern();
        logln("pattern = " + s);
        int mode;
        int i = 0;
        String message;
        String resultStr;
        for (mode = 0; mode < 7; mode++) {
            pat.setRoundingMode(mode);
            if (pat.getRoundingMode() != mode) {
                errln(
                     "SetRoundingMode or GetRoundingMode failed for mode=" + mode); 
            }
    
            //for +2.55 with RoundingIncrement=1.0
            pat.setRoundingIncrement(1.0);
            resultStr = pat.format(Roundingnumber);
            message = "round(" + (double) Roundingnumber
                    + "," + mode + ",FALSE) with RoundingIncrement=1.0==>"; 
            verify(message, resultStr, result[i++]);
            message = "";
            resultStr = "";
    
            //for -2.55 with RoundingIncrement=1.0
            resultStr = pat.format(Roundingnumber1);
            message = "round(" + (double) Roundingnumber1
                    + "," + mode + ",FALSE) with RoundingIncrement=1.0==>"; 
            verify(message, resultStr, result[i++]);
            message = "";
            resultStr = "";
        }
    }
    
    /*Helper functions */
    public void verify(String message, String got, double expected) {
        logln(message + got + " Expected : " + (long)expected);
        String expectedStr = "";
        expectedStr=expectedStr + (long)expected;
        if(!got.equals(expectedStr) ) {
            errln("ERROR: Round() failed:  " + message + got + "  Expected : " + expectedStr);
        }
    }
}