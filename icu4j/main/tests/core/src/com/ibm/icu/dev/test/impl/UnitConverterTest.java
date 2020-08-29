package com.ibm.icu.dev.test.impl;


import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.units.*;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;


public class UnitConverterTest {

    public static boolean compareTwoBigDecimal(BigDecimal expected, BigDecimal actual, BigDecimal delta) {

        BigDecimal diff = expected.abs().compareTo(BigDecimal.ZERO) < 1 ?
                expected.subtract(actual).abs() : (expected.subtract(actual).divide(expected, MathContext.DECIMAL128)).abs();

        if (diff.compareTo(delta) == -1) return true;

        return false;

    }

    @Test
    public void testExtractConvertibility() {
        class TestData {
            MeasureUnitImpl source;
            MeasureUnitImpl target;
            UnitConverter.Convertibility expected;
            TestData(String source, String target, UnitConverter.Convertibility convertibility) {
                this.source = MeasureUnitImpl.UnitsParser.parseForIdentifier(source);
                this.target = MeasureUnitImpl.UnitsParser.parseForIdentifier(target);
                this.expected = convertibility;
            }
        }

        TestData[] tests = {
                new TestData("meter", "foot", UnitConverter.Convertibility.CONVERTIBLE),
                new TestData("square-meter-per-square-hour", "hectare-per-square-second", UnitConverter.Convertibility.CONVERTIBLE),
                new TestData("hertz", "revolution-per-second", UnitConverter.Convertibility.CONVERTIBLE),
                new TestData("millimeter", "meter", UnitConverter.Convertibility.CONVERTIBLE),
                new TestData("yard", "meter", UnitConverter.Convertibility.CONVERTIBLE),
                new TestData("ounce-troy", "kilogram", UnitConverter.Convertibility.CONVERTIBLE),
                new TestData("percent", "portion", UnitConverter.Convertibility.CONVERTIBLE),
                new TestData("ofhg", "kilogram-per-square-meter-square-second", UnitConverter.Convertibility.CONVERTIBLE),

                new TestData("second-per-meter", "meter-per-second", UnitConverter.Convertibility.RECIPROCAL),
        };
        ConversionRates conversionRates = new ConversionRates();

        for (TestData test :
                tests) {
            assertEquals(test.expected, UnitConverter.extractConvertibility(test.source, test.target, conversionRates));
        }
    }

    @Test
    public void testConverter() {
        class TestData {
            MeasureUnitImpl source;
            MeasureUnitImpl target;
            BigDecimal input;
            BigDecimal expected;
            TestData(String source, String target, double input, double expected) {
                this.source = MeasureUnitImpl.UnitsParser.parseForIdentifier(source);
                this.target = MeasureUnitImpl.UnitsParser.parseForIdentifier(target);
                this.input = BigDecimal.valueOf(input);
                this.expected = BigDecimal.valueOf(expected);
            }

        }

        TestData[] tests = {
                new TestData("square-centimeter", "square-meter", 1000, 0.1),
                new TestData("celsius", "fahrenheit", 1000, 1832),
                new TestData("fahrenheit", "fahrenheit", 1000, 1000),
//                new TestData("per-square-hour", "per-square-second", 10, 10),
//                new TestData("hertz", "revolution-per-second", 10, 10),
//                new TestData("millimeter", "meter", 10, 10),
//                new TestData("yard", "meter", 10, 10),
//                new TestData("ounce-troy", "kilogram", 10, 10),
//                new TestData("percent", "portion", 10, 10),
//                new TestData("ofhg", "kilogram-per-square-meter-square-second", 10, 10),
//
//                new TestData("second-per-meter", "meter-per-second", 11, 11),
        };

        ConversionRates conversionRates = new ConversionRates();

        for (TestData test :
                tests) {
            UnitConverter converter = new UnitConverter(test.source, test.target, conversionRates);
            assertEquals(test.expected.doubleValue(), converter.convert(test.input).doubleValue(), (0.001));
        }

    }

    @Test
    public void testConverterFromUnitTests() throws IOException {
        class TestCase {
            String category;
            String sourceString;
            String targetString;
            MeasureUnitImpl source;
            MeasureUnitImpl target;
            BigDecimal input;
            BigDecimal expected;
            TestCase(String line) {
                String[] fields = line
                        .replaceAll(" ", "") // Remove all the spaces.
                        .replaceAll(",", "") // Remove all the commas.
                        .split(";");

                this.category = fields[0].replaceAll(" ", "");
                this.sourceString = fields[1];
                this.targetString = fields[2];
                this.source = MeasureUnitImpl.UnitsParser.parseForIdentifier(fields[1]);
                this.target = MeasureUnitImpl.UnitsParser.parseForIdentifier(fields[2]);
                this.input = BigDecimal.valueOf(1000);
                this.expected = new BigDecimal(fields[4]);
            }
        }

        String codePage = "UTF-8";
        BufferedReader f = TestUtil.getDataReader("units/unitsTest.txt", codePage);
        ArrayList<TestCase> tests = new ArrayList<>();
        while (true) {
            String line = f.readLine();
            if (line == null) break;
            if (line.isEmpty() || line.startsWith("#")) continue;
            tests.add(new TestCase(line));
        }

        ConversionRates conversionRates = new ConversionRates();

        for (TestCase testCase :
                tests) {
            UnitConverter converter = new UnitConverter(testCase.source, testCase.target, conversionRates);
            if (compareTwoBigDecimal(testCase.expected, converter.convert(testCase.input), BigDecimal.valueOf(0.000001))) {
                continue;
            } else {
                Assert.fail(new StringBuilder()
                        .append(testCase.category)
                        .append(" ")
                        .append(testCase.sourceString)
                        .append(" ")
                        .append(testCase.targetString)
                        .append(" ")
                        .append(converter.convert(testCase.input).toString())
                        .append(" expected  ")
                        .append(testCase.expected.toString())
                        .toString());
            }
        }
    }
}
