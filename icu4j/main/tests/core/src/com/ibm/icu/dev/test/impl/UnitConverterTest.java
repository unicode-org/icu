package com.ibm.icu.dev.test.impl;


import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.units.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.StringJoiner;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class UnitConverterTest {

    @Test
    public void testExtractConvertibility() {
        class TestData {
            TestData(String source, String target, Convertibility convertibility) {
                this.source = UnitsParser.parseForIdentifier(source);
                this.target = UnitsParser.parseForIdentifier(target);
                this.expected = convertibility;
            }

            MeasureUnitImpl source;
            MeasureUnitImpl target;
            Convertibility expected;
        }

        TestData[] tests = {
                new TestData("meter", "foot", Convertibility.CONVERTIBLE),
                new TestData("square-meter-per-square-hour", "hectare-per-square-second", Convertibility.CONVERTIBLE),
                new TestData("hertz", "revolution-per-second", Convertibility.CONVERTIBLE),
                new TestData("millimeter", "meter", Convertibility.CONVERTIBLE),
                new TestData("yard", "meter", Convertibility.CONVERTIBLE),
                new TestData("ounce-troy", "kilogram", Convertibility.CONVERTIBLE),
                new TestData("percent", "portion", Convertibility.CONVERTIBLE),
                new TestData("ofhg", "kilogram-per-square-meter-square-second", Convertibility.CONVERTIBLE),

                new TestData("second-per-meter", "meter-per-second", Convertibility.RECIPROCAL),
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
            TestData(String source, String target, double input, double expected) {
                this.source = UnitsParser.parseForIdentifier(source);
                this.target = UnitsParser.parseForIdentifier(target);
                this.input = BigDecimal.valueOf(input);
                this.expected = BigDecimal.valueOf(expected);
            }

            MeasureUnitImpl source;
            MeasureUnitImpl target;
            BigDecimal input;
            BigDecimal expected;

        }

        TestData[] tests = {
                new TestData("square-centimeter", "square-meter", 1000, 0.1),
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
            TestCase(String line) {
                String[] fields = line
                        .replaceAll(" ", "") // Remove all the spaces.
                        .replaceAll(",", "") // Remove all the commas.
                        .split(";");

                this.category = fields[0].replaceAll(" ", "");
                this.sourceString = fields[1];
                this.targetString = fields[2];
                this.source = UnitsParser.parseForIdentifier(fields[1]);
                this.target = UnitsParser.parseForIdentifier(fields[2]);
                this.input = BigDecimal.valueOf(1000);
                this.expected = new BigDecimal(fields[4]);
            }

            String category;
            String sourceString;
            String targetString;
            MeasureUnitImpl source;
            MeasureUnitImpl target;
            BigDecimal input;
            BigDecimal expected;
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



    public static boolean compareTwoBigDecimal(BigDecimal expected, BigDecimal actual, BigDecimal delta) {

        BigDecimal diff = expected.abs().compareTo(BigDecimal.ZERO) < 1 ?
                expected.subtract(actual).abs() : expected.subtract(actual).divide(expected, MathContext.DECIMAL128);

        if (diff.compareTo(delta) == -1) return true;

        return false;

    }
}
