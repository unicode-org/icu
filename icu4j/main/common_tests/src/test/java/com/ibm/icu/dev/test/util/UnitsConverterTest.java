// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.util;

import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.util.CompoundUnitsConverter;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.UnitsConverter;

/**
 * @test
 * @summary General test of UnitsConverter
 */
@RunWith(JUnit4.class)
public class UnitsConverterTest {
    @Test
    public void testUnitsConverter() {

        class TestCase {
            public String from;
            public String to;
            public String inputValue;
            public String expected;

            TestCase(String from, String to, String inputValue, String expected) {
                this.from = from;
                this.to = to;
                this.inputValue = inputValue;
                this.expected = expected;
            }
        }

        List<TestCase> testCases = Arrays.asList(
                new TestCase("meter", "foot", "1", "3.28084"),
                new TestCase("foot", "meter", "1", "0.3048"),
                new TestCase("mile", "meter", "1", "1609.344"),
                new TestCase("meter", "mile", "1", "0.000621371"),
                new TestCase("foot", "mile", "1", "0.000189394"),
                new TestCase("mile", "foot", "1", "5280"),
                new TestCase("meter-per-second", "foot-per-second", "1", "3.28084"),
                new TestCase("foot-per-second", "meter-per-second", "1", "0.3048"),
                new TestCase("mile-per-hour", "meter-per-second", "1", "0.44704"),
                new TestCase("meter-per-second", "mile-per-hour", "1", "2.23694"),
                new TestCase("foot-per-minute", "meter-per-second", "1", "0.00508"),
                new TestCase("meter-per-second", "foot-per-minute", "1", "196.850394"),
                new TestCase("kilometer-per-hour", "meter-per-second", "1", "0.277778"),
                new TestCase("meter-per-second", "kilometer-per-hour", "1", "3.6"),
                new TestCase("mile-per-hour", "foot-per-second", "1", "1.46667"),
                new TestCase("foot-per-second", "mile-per-hour", "1", "0.681818"),
                new TestCase("kilometer-per-hour", "mile-per-hour", "1", "0.621371"),
                new TestCase("mile-per-hour", "kilometer-per-hour", "1", "1.60934"),
                new TestCase("knot", "meter-per-second", "1", "0.514444"),
                new TestCase("meter-per-second", "knot", "1", "1.94384"),
                new TestCase("knot", "mile-per-hour", "1", "1.15078"),
                new TestCase("mile-per-hour", "knot", "1", "0.868976"),
                new TestCase("knot", "kilometer-per-hour", "1", "1.852"),
                new TestCase("kilometer-per-hour", "knot", "1", "0.539957"),
                new TestCase("foot-per-minute", "mile-per-hour", "1", "0.0113636"),
                new TestCase("mile-per-hour", "foot-per-minute", "1", "88"),
                new TestCase("celsius", "fahrenheit", "1", "33.8"),
                new TestCase("fahrenheit", "celsius", "1", "-17.222222"),
                new TestCase("celsius", "kelvin", "1", "274.15"),
                new TestCase("kelvin", "celsius", "1", "-272.15"),
                new TestCase("fahrenheit", "kelvin", "1", "255.927778"),
                new TestCase("kelvin", "fahrenheit", "1", "-457.87"));

        for (TestCase testCase : testCases) {
            CompoundUnitsConverter converter = UnitsConverter //
                    .fromCompoundUnit(MeasureUnit.forIdentifier(testCase.from)) //
                    .toCompoundUnit(MeasureUnit.forIdentifier(testCase.to)) //
                    .build();

            BigDecimal expected = BigDecimal.valueOf(Double.parseDouble(testCase.expected));
            BigDecimal actual = converter.convert(new BigDecimal(testCase.inputValue));
            BigDecimal diff = expected.subtract(actual).abs();

            assertTrue(diff.compareTo(BigDecimal.valueOf(0.00001)) <= 0);
        }
    }
}
