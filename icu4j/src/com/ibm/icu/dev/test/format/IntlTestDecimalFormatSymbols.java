/*****************************************************************************************
 *
 *   Copyright (C) 1996-2009, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **/

/** 
 * Port From:   JDK 1.4b1 : java.text.Format.IntlTestDecimalFormatSymbols
 * Source File: java/text/format/IntlTestDecimalFormatSymbols.java
 **/
 
/*
    @test 1.4 98/03/06
    @summary test International Decimal Format Symbols
*/


package com.ibm.icu.dev.test.format;

import com.ibm.icu.text.*;
import com.ibm.icu.util.Currency;

import java.util.Locale;

public class IntlTestDecimalFormatSymbols extends com.ibm.icu.dev.test.TestFmwk
{
    public static void main(String[] args) throws Exception {
        new IntlTestDecimalFormatSymbols().run(args);
    }

    // Test the API of DecimalFormatSymbols; primarily a simple get/set set.
    public void TestSymbols()
    {
        DecimalFormatSymbols fr = new DecimalFormatSymbols(Locale.FRENCH);

        DecimalFormatSymbols en = new DecimalFormatSymbols(Locale.ENGLISH);

        if(en.equals(fr)) {
            errln("ERROR: English DecimalFormatSymbols equal to French");
        }

        // just do some VERY basic tests to make sure that get/set work

        char zero = en.getZeroDigit();
        fr.setZeroDigit(zero);
        if(fr.getZeroDigit() != en.getZeroDigit()) {
            errln("ERROR: get/set ZeroDigit failed");
        }

        char sigDigit = en.getSignificantDigit();
        fr.setSignificantDigit(sigDigit); 
        if(fr.getSignificantDigit() != en.getSignificantDigit()) {
            errln("ERROR: get/set SignificantDigit failed");
        }

        Currency currency = Currency.getInstance("USD");
        fr.setCurrency(currency);
        if (!fr.getCurrency().equals(currency)){
            errln("ERROR: get/set Currency failed");
        }
            
        char group = en.getGroupingSeparator();
        fr.setGroupingSeparator(group);
        if(fr.getGroupingSeparator() != en.getGroupingSeparator()) {
            errln("ERROR: get/set GroupingSeparator failed");
        }

        char decimal = en.getDecimalSeparator();
        fr.setDecimalSeparator(decimal);
        if(fr.getDecimalSeparator() != en.getDecimalSeparator()) {
            errln("ERROR: get/set DecimalSeparator failed");
        }

        char monetaryGroup = en.getMonetaryGroupingSeparator();
        fr.setMonetaryGroupingSeparator(monetaryGroup);
        if(fr.getMonetaryGroupingSeparator() != en.getMonetaryGroupingSeparator()) {
            errln("ERROR: get/set MonetaryGroupingSeparator failed");
        }

        char monetaryDecimal = en.getMonetaryDecimalSeparator();
        fr.setMonetaryDecimalSeparator(monetaryDecimal);
        if(fr.getMonetaryDecimalSeparator() != en.getMonetaryDecimalSeparator()) {
            errln("ERROR: get/set MonetaryDecimalSeparator failed");
        }

        char perMill = en.getPerMill();
        fr.setPerMill(perMill);
        if(fr.getPerMill() != en.getPerMill()) {
            errln("ERROR: get/set PerMill failed");
        }

        char percent = en.getPercent();
        fr.setPercent(percent);
        if(fr.getPercent() != en.getPercent()) {
            errln("ERROR: get/set Percent failed");
        }

        char digit = en.getDigit();
        fr.setDigit(digit);
        if(fr.getPercent() != en.getPercent()) {
            errln("ERROR: get/set Percent failed");
        }

        char patternSeparator = en.getPatternSeparator();
        fr.setPatternSeparator(patternSeparator);
        if(fr.getPatternSeparator() != en.getPatternSeparator()) {
            errln("ERROR: get/set PatternSeparator failed");
        }

        String infinity = en.getInfinity();
        fr.setInfinity(infinity);
        String infinity2 = fr.getInfinity();
        if(! infinity.equals(infinity2)) {
            errln("ERROR: get/set Infinity failed");
        }

        String nan = en.getNaN();
        fr.setNaN(nan);
        String nan2 = fr.getNaN();
        if(! nan.equals(nan2)) {
            errln("ERROR: get/set NaN failed");
        }

        char minusSign = en.getMinusSign();
        fr.setMinusSign(minusSign);
        if(fr.getMinusSign() != en.getMinusSign()) {
            errln("ERROR: get/set MinusSign failed");
        }

        char plusSign = en.getPlusSign();
        fr.setPlusSign(plusSign);
        if(fr.getPlusSign() != en.getPlusSign()) {
            errln("ERROR: get/set PlusSign failed");
        }

        char padEscape = en.getPadEscape();
        fr.setPadEscape(padEscape);
        if(fr.getPadEscape() != en.getPadEscape()) {
            errln("ERROR: get/set PadEscape failed");
        }

        String exponential = en.getExponentSeparator();
        fr.setExponentSeparator(exponential);
        if(fr.getExponentSeparator() != en.getExponentSeparator()) {
            errln("ERROR: get/set Exponential failed");
        }
        
        // Test CurrencySpacing.
        // In CLDR 1.7, only root.txt has CurrencySpacing data. This data might 
        // be different between en and fr in the future.
        for (int i = en.CURRENCY_SPC_CURRENCY_MATCH; i <= en.CURRENCY_SPC_INSERT; i++) {
            if (en.getPatternForCurrencySpacing(i, true) !=
                fr.getPatternForCurrencySpacing(i, true)) {
                errln("ERROR: get currency spacing item:"+ i+" before the currency");
                if (en.getPatternForCurrencySpacing(i, false) !=
                    fr.getPatternForCurrencySpacing(i, false)) {
                    errln("ERROR: get currency spacing item:" + i + " after currency");
                }
            }
        }
        
        String dash = "-";
        en.setPatternForCurrencySpacing(en.CURRENCY_SPC_INSERT, true, dash);
        if (dash != en.getPatternForCurrencySpacing(en.CURRENCY_SPC_INSERT, true)) {
            errln("ERROR: set currency spacing pattern for before currency.");
        }

        //DecimalFormatSymbols foo = new DecimalFormatSymbols(); //The variable is never used

        en = (DecimalFormatSymbols) fr.clone();

        if(! en.equals(fr)) {
            errln("ERROR: Clone failed");
        }
    }
    
    public void testCoverage() {
        DecimalFormatSymbols df = new DecimalFormatSymbols();
        DecimalFormatSymbols df2 = (DecimalFormatSymbols)df.clone();
        if (!df.equals(df2) || df.hashCode() != df2.hashCode()) {
            errln("decimal format symbols clone, equals, or hashCode failed");            
        }
    }
}
