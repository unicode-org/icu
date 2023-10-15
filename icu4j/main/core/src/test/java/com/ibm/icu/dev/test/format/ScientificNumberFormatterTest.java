// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.CoreTestFmwk;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.ScientificNumberFormatter;
import com.ibm.icu.util.ULocale;

/**
 * @author rocketman
 *
 */
@RunWith(JUnit4.class)
public class ScientificNumberFormatterTest extends CoreTestFmwk {
    @Test
    public void TestBasic() {
        ScientificNumberFormatter markup = ScientificNumberFormatter.getMarkupInstance(
                ULocale.ENGLISH, "<sup>", "</sup>");
        ScientificNumberFormatter superscript = ScientificNumberFormatter.getSuperscriptInstance(ULocale.ENGLISH);
        assertEquals(
                "toMarkupExponentDigits",
                "1.23456×10<sup>-78</sup>",
                markup.format(1.23456e-78));
        assertEquals(
                "toSuperscriptExponentDigits",
                "1.23456×10⁻⁷⁸",
                superscript.format(1.23456e-78));
    }


    @Test
    public void TestFarsi() {
        ScientificNumberFormatter fmt = ScientificNumberFormatter.getMarkupInstance(
                new ULocale("fa"), "<sup>", "</sup>");
        assertEquals(
                "",
                "۱٫۲۳۴۵۶×۱۰<sup>‎−۷۸</sup>",
                fmt.format(1.23456e-78));
    }


    @Test
    public void TestPlusSignInExponentMarkup() {
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getScientificInstance(ULocale.ENGLISH);
        decfmt.applyPattern("0.00E+0");
        ScientificNumberFormatter fmt = ScientificNumberFormatter.getMarkupInstance(
                decfmt, "<sup>", "</sup>");

        assertEquals(
                "",
                "6.02×10<sup>+23</sup>",
                fmt.format(6.02e23));
    }


    @Test
    public void TestPlusSignInExponentSuperscript() {
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getScientificInstance(ULocale.ENGLISH);
        decfmt.applyPattern("0.00E+0");
        ScientificNumberFormatter fmt = ScientificNumberFormatter.getSuperscriptInstance(
                decfmt);
        assertEquals(
                "",
                "6.02×10⁺²³",
                fmt.format(6.02e23));
    }

    @Test
    public void TestFixedDecimalMarkup() {
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
        ScientificNumberFormatter fmt = ScientificNumberFormatter.getMarkupInstance(
                decfmt, "<sup>", "</sup>");
        assertEquals(
                "",
                "123,456",
                fmt.format(123456.0));
    }

    @Test
    public void TestFixedDecimalSuperscript() {
        DecimalFormat decfmt = (DecimalFormat) NumberFormat.getInstance(ULocale.ENGLISH);
        ScientificNumberFormatter fmt = ScientificNumberFormatter.getSuperscriptInstance(decfmt);
        assertEquals(
                "",
                "123,456",
                fmt.format(123456.0));
    }
}
