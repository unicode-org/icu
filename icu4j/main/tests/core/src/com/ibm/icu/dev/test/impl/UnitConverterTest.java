package com.ibm.icu.dev.test.impl;


import com.ibm.icu.impl.units.*;
import java.math.BigDecimal;
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
                new TestData("meter", "foot", 10, 32.8084),
//                new TestData("square-meter-per-square-hour", "hectare-per-square-second", 10, 10),
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
            assertEquals(test.expected.doubleValue(), converter.convert(test.input).doubleValue(), (0.00001));
        }

    }
}
