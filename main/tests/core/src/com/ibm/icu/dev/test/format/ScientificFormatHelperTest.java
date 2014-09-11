/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.text.AttributedCharacterIterator;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.ScientificFormatHelper;
import com.ibm.icu.util.ULocale;

public class ScientificFormatHelperTest extends TestFmwk {
    
    public static void main(String[] args) throws Exception {
        new ScientificFormatHelperTest().run(args);
    }
    
    public void TestBasic() {
        ULocale en = new ULocale("en");
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getScientificInstance(en);
        AttributedCharacterIterator iterator = decfmt.formatToCharacterIterator(1.23456e-78);
        ScientificFormatHelper helper = ScientificFormatHelper.getInstance(
                decfmt.getDecimalFormatSymbols());
        assertEquals(
                "insetMarkup",
                "1.23456\u00d710<sup>-78</sup>",
                helper.insertMarkup(iterator, "<sup>", "</sup>"));
        assertEquals(
                "toSuperscriptExponentDigits",
                "1.23456\u00d710\u207b\u2077\u2078",
                helper.toSuperscriptExponentDigits(iterator));
    }

    public void TestPlusSignInExponentMarkup() {
        ULocale en = new ULocale("en");
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getScientificInstance(en);
        decfmt.applyPattern("0.00E+0");
        AttributedCharacterIterator iterator = decfmt.formatToCharacterIterator(6.02e23);
        ScientificFormatHelper helper = ScientificFormatHelper.getInstance(
                decfmt.getDecimalFormatSymbols());
        assertEquals(
                "",
                "6.02\u00d710<sup>+23</sup>",
                helper.insertMarkup(iterator, "<sup>", "</sup>"));
    }

    
    public void TestPlusSignInExponentSuperscript() {
        ULocale en = new ULocale("en");
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getScientificInstance(en);
        decfmt.applyPattern("0.00E+0");
        AttributedCharacterIterator iterator = decfmt.formatToCharacterIterator(6.02e23);
        ScientificFormatHelper helper = ScientificFormatHelper.getInstance(
                decfmt.getDecimalFormatSymbols());
        assertEquals(
                "",
                "6.02\u00d710\u207a\u00b2\u00b3",
                helper.toSuperscriptExponentDigits(iterator));
    }
    
    public void TestFixedDecimalMarkup() {
        ULocale en = new ULocale("en");
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getInstance(en);
        AttributedCharacterIterator iterator = decfmt.formatToCharacterIterator(123456.0);
        ScientificFormatHelper helper = ScientificFormatHelper.getInstance(
                decfmt.getDecimalFormatSymbols());
        try {
            helper.insertMarkup(iterator, "<sup>", "</sup>");
            fail("expected illegal argument exception");
        } catch (IllegalArgumentException expected) {
            // do nothing
        }
    }
    
    public void TestFixedDecimalSuperscript() {
        ULocale en = new ULocale("en");
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getInstance(en);
        AttributedCharacterIterator iterator = decfmt.formatToCharacterIterator(123456.0);
        ScientificFormatHelper helper = ScientificFormatHelper.getInstance(
                decfmt.getDecimalFormatSymbols());
        try {
            helper.toSuperscriptExponentDigits(iterator);
            fail("expected illegal argument exception");
        } catch (IllegalArgumentException expected) {
            // do nothing
        }
    }
}
