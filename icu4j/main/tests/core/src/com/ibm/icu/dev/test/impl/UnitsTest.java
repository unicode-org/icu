// © 2020 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.Pair;
import com.ibm.icu.impl.units.ComplexUnitsConverter;
import com.ibm.icu.impl.units.ConversionRates;
import com.ibm.icu.impl.units.MeasureUnitImpl;
import com.ibm.icu.impl.units.UnitsConverter;
import com.ibm.icu.impl.units.UnitsRouter;
import com.ibm.icu.util.Measure;
import com.ibm.icu.util.MeasureUnit;


public class UnitsTest {

    public static boolean compareTwoBigDecimal(BigDecimal expected, BigDecimal actual, BigDecimal delta) {
        BigDecimal diff =
                expected.abs().compareTo(BigDecimal.ZERO) < 1 ?
                        expected.subtract(actual).abs() :
                        (expected.subtract(actual).divide(expected, MathContext.DECIMAL128)).abs();

        if (diff.compareTo(delta) == -1) return true;
        return false;
    }

    @Test
    public void testComplexUnitsConverter() {
        class TestCase {
            String input;
            String output;
            BigDecimal value;
            Measure[] expected;
            // For mixed units, accuracy of the smallest unit
            double accuracy;

            TestCase(String input, String output, BigDecimal value, Measure[] expected, double accuracy) {
                this.input = input;
                this.output = output;
                this.value = value;
                this.expected = expected;
                this.accuracy = accuracy;
            }

            void testATestCase(ComplexUnitsConverter converter) {
                List<Measure> measures = converter.convert(value, null).measures;

                assertEquals("measures length", expected.length, measures.size());
                int i = 0;
                for (Measure measure : measures) {
                    double accuracy = 0.0;
                    if (i == expected.length - 1) {
                        accuracy = this.accuracy;
                    }
                    assertTrue("input " + value + ", output measure " + i + ": expected " +
                                    expected[i] + ", expected unit " +
                                    expected[i].getUnit() + " got unit " + measure.getUnit(),
                            expected[i].getUnit().equals(measure.getUnit()));
                    assertEquals("input " + value + ", output measure " + i + ": expected " +
                                    expected[i] + ", expected number " +
                                    expected[i].getNumber() + " got number " + measure.getNumber(),
                            expected[i].getNumber().doubleValue(),
                            measure.getNumber().doubleValue(), accuracy);
                    i++;
                }
            }
        }

        TestCase[] testCases = new TestCase[] {
            // Significantly less than 2.0.
            new TestCase(
                "foot", "foot-and-inch", BigDecimal.valueOf(1.9999),
                new Measure[] {new Measure(1, MeasureUnit.FOOT), new Measure(11.9988, MeasureUnit.INCH)},
                0),

            // A minimal nudge under 2.0, rounding up to 2.0 ft, 0 in.
            // TODO(icu-units#108): this matches double precision calculations
            // from C++, but BigDecimal is in use: do we want Java to be more
            // precise than C++?
            new TestCase(
                "foot", "foot-and-inch", BigDecimal.valueOf(2.0).subtract(ComplexUnitsConverter.EPSILON),
                new Measure[] {new Measure(2, MeasureUnit.FOOT), new Measure(0, MeasureUnit.INCH)}, 0),

            // A slightly bigger nudge under 2.0, *not* rounding up to 2.0 ft!
            new TestCase("foot", "foot-and-inch",
                         BigDecimal.valueOf(2.0).subtract(
                             ComplexUnitsConverter.EPSILON.multiply(BigDecimal.valueOf(3.0))),
                         new Measure[] {new Measure(1, MeasureUnit.FOOT),
                                        new Measure(BigDecimal.valueOf(12.0).subtract(
                                                        ComplexUnitsConverter.EPSILON.multiply(
                                                            BigDecimal.valueOf(36.0))),
                                                    MeasureUnit.INCH)},
                         0),

            // Testing precision with meter and light-year.
            //
            // DBL_EPSILON light-years, ~2.22E-16 light-years, is ~2.1 meters
            // (maximum precision when exponent is 0).
            //
            // 1e-16 light years is 0.946073 meters.

            // A 2.1 meter nudge under 2.0 light years, rounding up to 2.0 ly, 0 m.
            // TODO(icu-units#108): this matches double precision calculations
            // from C++, but BigDecimal is in use: do we want Java to be more
            // precise than C++?
            new TestCase("light-year", "light-year-and-meter",
                         BigDecimal.valueOf(2.0).subtract(ComplexUnitsConverter.EPSILON),
                         new Measure[] {new Measure(2, MeasureUnit.LIGHT_YEAR),
                                        new Measure(0, MeasureUnit.METER)},
                         0),

            // // TODO(icu-units#108): figure out precision thresholds for BigDecimal?
            // // This test passes in C++ due to double-precision rounding.
            // // A 2.1 meter nudge under 1.0 light years, rounding up to 1.0 ly, 0 m.
            // new TestCase("light-year", "light-year-and-meter",
            //              BigDecimal.valueOf(1.0).subtract(ComplexUnitsConverter.EPSILON),
            //              new Measure[] {new Measure(1, MeasureUnit.LIGHT_YEAR),
            //                             new Measure(0, MeasureUnit.METER)},
            //              0),

            // 1e-15 light years is 9.46073 meters (calculated using "bc" and
            // the CLDR conversion factor). With double-precision maths in C++,
            // we get 10.5. In this case, we're off by a bit more than 1 meter.
            // With Java BigDecimal, we get accurate results.
            new TestCase("light-year", "light-year-and-meter", BigDecimal.valueOf(1.0 + 1e-15),
                         new Measure[] {new Measure(1, MeasureUnit.LIGHT_YEAR),
                                        new Measure(9.46073, MeasureUnit.METER)},
                         0 /* meters, precision */),

            // TODO(icu-units#108): reconsider whether epsilon rounding is desirable:
            //
            // 2e-16 light years is 1.892146 meters. For C++ double, we consider
            // this in the noise, and thus expect a 0. (This test fails when
            // 2e-16 is increased to 4e-16.) For Java, using BigDecimal, we
            // actually get a good result.
            new TestCase("light-year", "light-year-and-meter", BigDecimal.valueOf(1.0 + 2e-16),
                         new Measure[] {new Measure(1, MeasureUnit.LIGHT_YEAR),
                                        new Measure(1.892146, MeasureUnit.METER)},
                         0),
        };


        ConversionRates rates = new ConversionRates();
        MeasureUnit input, output;
        for (TestCase testCase : testCases) {
            input = MeasureUnit.forIdentifier(testCase.input);
            output = MeasureUnit.forIdentifier(testCase.output);
            final MeasureUnitImpl inputImpl = MeasureUnitImpl.forIdentifier(input.getIdentifier());
            final MeasureUnitImpl outputImpl = MeasureUnitImpl.forIdentifier(output.getIdentifier());
            ComplexUnitsConverter converter1 = new ComplexUnitsConverter(inputImpl, outputImpl, rates);

            testCase.testATestCase(converter1);

            // Test ComplexUnitsConverter created with CLDR units identifiers.
            ComplexUnitsConverter converter2 = new ComplexUnitsConverter(testCase.input, testCase.output);
            testCase.testATestCase(converter2);
        }

        // TODO(icu-units#63): test negative numbers!
    }


    @Test
    public void testComplexUnitsConverterSorting() {
        class TestCase {
            String message;
            String inputUnit;
            String outputUnit;
            double inputValue;
            Measure[] expectedMeasures;
            double accuracy;

            public TestCase(String message, String inputUnit, String outputUnit, double inputValue, Measure[] expectedMeasures, double accuracy) {
                this.message = message;
                this.inputUnit = inputUnit;
                this.outputUnit = outputUnit;
                this.inputValue = inputValue;
                this.expectedMeasures = expectedMeasures;
                this.accuracy = accuracy;
            }
        }

        TestCase[] testCases = new TestCase[]{
                new TestCase(
                        "inch-and-foot",
                        "meter",
                        "inch-and-foot",
                        10.0,
                        new Measure[]{
                                new Measure(9.70079, MeasureUnit.INCH),
                                new Measure(32, MeasureUnit.FOOT),
                        },
                        0.0001
                ),
                new TestCase(
                        "inch-and-yard-and-foot",
                        "meter",
                        "inch-and-yard-and-foot",
                        100.0,
                        new Measure[]{
                                new Measure(1.0079, MeasureUnit.INCH),
                                new Measure(109, MeasureUnit.YARD),
                                new Measure(1, MeasureUnit.FOOT),
                        },
                        0.0001
                ),
        };

        ConversionRates conversionRates = new ConversionRates();
        for (TestCase testCase : testCases) {
            MeasureUnitImpl input = MeasureUnitImpl.forIdentifier(testCase.inputUnit);
            MeasureUnitImpl output = MeasureUnitImpl.forIdentifier(testCase.outputUnit);

            ComplexUnitsConverter converter = new ComplexUnitsConverter(input, output, conversionRates);
            List<Measure> actualMeasures = converter.convert(BigDecimal.valueOf(testCase.inputValue), null).measures;

            assertEquals(testCase.message, testCase.expectedMeasures.length, actualMeasures.size());
            for (int i = 0; i < testCase.expectedMeasures.length; i++) {
                assertEquals(testCase.message, testCase.expectedMeasures[i].getUnit(), actualMeasures.get(i).getUnit());
                assertEquals(testCase.message,
                        testCase.expectedMeasures[i].getNumber().doubleValue(),
                        actualMeasures.get(i).getNumber().doubleValue(),
                        testCase.accuracy);
            }
        }
    }


    @Test
    public void testExtractConvertibility() {
        class TestData {
            MeasureUnitImpl source;
            MeasureUnitImpl target;
            UnitsConverter.Convertibility expected;

            TestData(String source, String target, UnitsConverter.Convertibility convertibility) {
                this.source = MeasureUnitImpl.UnitsParser.parseForIdentifier(source);
                this.target = MeasureUnitImpl.UnitsParser.parseForIdentifier(target);
                this.expected = convertibility;
            }
        }

        TestData[] tests = {
                new TestData("meter", "foot", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("kilometer", "foot", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("hectare", "square-foot", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("kilometer-per-second", "second-per-meter", UnitsConverter.Convertibility.RECIPROCAL),
                new TestData("square-meter", "square-foot", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("kilometer-per-second", "foot-per-second", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("square-hectare", "pow4-foot", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("square-kilometer-per-second", "second-per-square-meter", UnitsConverter.Convertibility.RECIPROCAL),
                new TestData("cubic-kilometer-per-second-meter", "second-per-square-meter", UnitsConverter.Convertibility.RECIPROCAL),
                new TestData("square-meter-per-square-hour", "hectare-per-square-second", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("hertz", "revolution-per-second", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("millimeter", "meter", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("yard", "meter", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("ounce-troy", "kilogram", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("percent", "portion", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("ofhg", "kilogram-per-square-meter-square-second", UnitsConverter.Convertibility.CONVERTIBLE),
                new TestData("second-per-meter", "meter-per-second", UnitsConverter.Convertibility.RECIPROCAL),
        };
        ConversionRates conversionRates = new ConversionRates();

        for (TestData test :
                tests) {
            assertEquals(test.expected, UnitsConverter.extractConvertibility(test.source, test.target, conversionRates));
        }
    }

    @Test
    public void testConversionInfo() {
        class TestData {
            String source;
            String target;
            UnitsConverter.ConversionInfo expected = new UnitsConverter.ConversionInfo();

            public TestData(String source, String target, double conversionRate, double offset, Boolean reciprocal) {
                this.source = source;
                this.target = target;
                this.expected.conversionRate = BigDecimal.valueOf(conversionRate);
                this.expected.offset = BigDecimal.valueOf(offset);
                this.expected.reciprocal = reciprocal;
            }
        }

        TestData[] tests = {
                new TestData(
                        "meter",
                        "meter",
                        1.0, 0, false),
                new TestData(
                        "meter",
                        "foot",
                        3.28084, 0, false),
                new TestData(
                        "foot",
                        "meter",
                        0.3048, 0, false),
                new TestData(
                        "celsius",
                        "kelvin",
                        1, 273.15, false),
                new TestData(
                        "fahrenheit",
                        "kelvin",
                        5.0 / 9.0, 255.372, false),
                new TestData(
                        "fahrenheit",
                        "celsius",
                        5.0 / 9.0, -17.7777777778, false),
                new TestData(
                        "celsius",
                        "fahrenheit",
                        9.0 / 5.0, 32, false),
                new TestData(
                        "fahrenheit",
                        "fahrenheit",
                        1.0, 0, false),
                new TestData(
                        "mile-per-gallon",
                        "liter-per-100-kilometer",
                        0.00425143707, 0, true),
        };

        ConversionRates conversionRates = new ConversionRates();
        for (TestData test : tests) {
            MeasureUnitImpl sourceImpl = MeasureUnitImpl.forIdentifier(test.source);
            MeasureUnitImpl targetImpl = MeasureUnitImpl.forIdentifier(test.target);
            UnitsConverter unitsConverter = new UnitsConverter(sourceImpl, targetImpl, conversionRates);

            UnitsConverter.ConversionInfo actual = unitsConverter.getConversionInfo();

            // Test conversion Rate
            double maxDelta = 1e-6 * Math.abs(test.expected.conversionRate.doubleValue());
            if (test.expected.conversionRate.doubleValue() == 0) {
                maxDelta = 1e-12;
            }
            assertEquals("testConversionInfo for conversion rate: " + test.source + " to " + test.target,
                    test.expected.conversionRate.doubleValue(), actual.conversionRate.doubleValue(),
                    maxDelta);

            // Test offset
            maxDelta = 1e-6 * Math.abs(test.expected.offset.doubleValue());
            if (test.expected.offset.doubleValue() == 0) {
                maxDelta = 1e-12;
            }
            assertEquals("testConversionInfo for offset: " + test.source + " to " + test.target,
                    test.expected.offset.doubleValue(), actual.offset.doubleValue(),
                    maxDelta);

            // Test Reciprocal
            assertEquals("testConversionInfo for reciprocal: " + test.source + " to " + test.target,
                    test.expected.reciprocal, actual.reciprocal);
        }
    }

    @Test
    public void testConverter() {
        class TestData {
            final String sourceIdentifier;
            final String targetIdentifier;
            final BigDecimal input;
            final BigDecimal expected;

            TestData(String sourceIdentifier, String targetIdentifier, double input, double expected) {
                this.sourceIdentifier = sourceIdentifier;
                this.targetIdentifier = targetIdentifier;
                this.input = BigDecimal.valueOf(input);
                this.expected = BigDecimal.valueOf(expected);
            }
        }
        TestData[] tests = {
                // SI Prefixes
                new TestData("gram", "kilogram", 1.0, 0.001),
                new TestData("milligram", "kilogram", 1.0, 0.000001),
                new TestData("microgram", "kilogram", 1.0, 0.000000001),
                new TestData("megagram", "gram", 1.0, 1000000),
                new TestData("megagram", "kilogram", 1.0, 1000),
                new TestData("gigabyte", "byte", 1.0, 1000000000),
                new TestData("megawatt", "watt", 1.0, 1000000),
                new TestData("megawatt", "kilowatt", 1.0, 1000),
                // Binary Prefixes
                new TestData("kilobyte", "byte", 1, 1000),
                new TestData("kibibyte", "byte", 1, 1024),
                new TestData("mebibyte", "byte", 1, 1048576),
                new TestData("gibibyte", "kibibyte", 1, 1048576),
                new TestData("pebibyte", "tebibyte", 4, 4096),
                new TestData("zebibyte", "pebibyte", 1.0/16, 65536.0),
                new TestData("yobibyte", "exbibyte", 1, 1048576),
                // Mass
                new TestData("gram", "kilogram", 1.0, 0.001),
                new TestData("pound", "kilogram", 1.0, 0.453592),
                new TestData("pound", "kilogram", 2.0, 0.907185),
                new TestData("ounce", "pound", 16.0, 1.0),
                new TestData("ounce", "kilogram", 16.0, 0.453592),
                new TestData("ton", "pound", 1.0, 2000),
                new TestData("stone", "pound", 1.0, 14),
                new TestData("stone", "kilogram", 1.0, 6.35029),
                // Temperature
                new TestData("celsius", "fahrenheit", 0.0, 32.0),
                new TestData("celsius", "fahrenheit", 10.0, 50.0),
                new TestData("celsius", "fahrenheit", 1000, 1832),
                new TestData("fahrenheit", "celsius", 32.0, 0.0),
                new TestData("fahrenheit", "celsius", 89.6, 32),
                new TestData("fahrenheit", "fahrenheit", 1000, 1000),
                new TestData("kelvin", "fahrenheit", 0.0, -459.67),
                new TestData("kelvin", "fahrenheit", 300, 80.33),
                new TestData("kelvin", "celsius", 0.0, -273.15),
                new TestData("kelvin", "celsius", 300.0, 26.85),
                // Area
                new TestData("square-meter", "square-yard", 10.0, 11.9599),
                new TestData("hectare", "square-yard", 1.0, 11959.9),
                new TestData("square-mile", "square-foot", 0.0001, 2787.84),
                new TestData("hectare", "square-yard", 1.0, 11959.9),
                new TestData("hectare", "square-meter", 1.0, 10000),
                new TestData("hectare", "square-meter", 0.0, 0.0),
                new TestData("square-mile", "square-foot", 0.0001, 2787.84),
                new TestData("square-yard", "square-foot", 10, 90),
                new TestData("square-yard", "square-foot", 0, 0),
                new TestData("square-yard", "square-foot", 0.000001, 0.000009),
                new TestData("square-mile", "square-foot", 0.0, 0.0),
                // Fuel Consumption
                new TestData("cubic-meter-per-meter", "mile-per-gallon", 2.1383143939394E-6, 1.1),
                new TestData("cubic-meter-per-meter", "mile-per-gallon", 2.6134953703704E-6, 0.9),
        };

        ConversionRates conversionRates = new ConversionRates();
        for (TestData test : tests) {
            MeasureUnitImpl source = MeasureUnitImpl.forIdentifier(test.sourceIdentifier);
            MeasureUnitImpl target = MeasureUnitImpl.forIdentifier(test.targetIdentifier);

            UnitsConverter converter = new UnitsConverter(source, target, conversionRates);

            double maxDelta = 1e-6 * Math.abs(test.expected.doubleValue());
            if (test.expected.doubleValue() == 0) {
                maxDelta = 1e-12;
            }
            assertEquals("testConverter: " + test.sourceIdentifier + " to " + test.targetIdentifier,
                    test.expected.doubleValue(), converter.convert(test.input).doubleValue(),
                    maxDelta);

            maxDelta = 1e-6 * Math.abs(test.input.doubleValue());
            if (test.input.doubleValue() == 0) {
                maxDelta = 1e-12;
            }
            assertEquals(
                    "testConverter inverse: " + test.targetIdentifier + " back to " + test.sourceIdentifier,
                    test.input.doubleValue(), converter.convertInverse(test.expected).doubleValue(),
                    maxDelta);


            // TODO: Test UnitsConverter created using CLDR separately.
            // Test UnitsConverter created by CLDR unit identifiers
            UnitsConverter converter2 = new UnitsConverter(test.sourceIdentifier, test.targetIdentifier);

            maxDelta = 1e-6 * Math.abs(test.expected.doubleValue());
            if (test.expected.doubleValue() == 0) {
                maxDelta = 1e-12;
            }
            assertEquals("testConverter2: " + test.sourceIdentifier + " to " + test.targetIdentifier,
                    test.expected.doubleValue(), converter2.convert(test.input).doubleValue(),
                    maxDelta);

            maxDelta = 1e-6 * Math.abs(test.input.doubleValue());
            if (test.input.doubleValue() == 0) {
                maxDelta = 1e-12;
            }
            assertEquals("testConverter2 inverse: " + test.targetIdentifier + " back to " + test.sourceIdentifier,
                    test.input.doubleValue(), converter2.convertInverse(test.expected).doubleValue(),
                    maxDelta);
        }
    }

    @Test
    public void testConverterWithCLDRTests() throws IOException {
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
                        .replaceAll("\t", "")
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
        ArrayList<TestCase> tests = new ArrayList<>();
        try (BufferedReader f = TestUtil.getDataReader("cldr/units/unitsTest.txt", codePage)) {
            while (true) {
                String line = f.readLine();
                if (line == null) break;
                if (line.isEmpty() || line.startsWith("#")) continue;
                tests.add(new TestCase(line));
            }
        }

        ConversionRates conversionRates = new ConversionRates();

        for (TestCase testCase :
                tests) {
            UnitsConverter converter = new UnitsConverter(testCase.source, testCase.target, conversionRates);
            BigDecimal got = converter.convert(testCase.input);
            if (compareTwoBigDecimal(testCase.expected, got, BigDecimal.valueOf(0.000001))) {
                continue;
            } else {
                fail(new StringBuilder()
                        .append(testCase.category)
                        .append(": Converting 1000 ")
                        .append(testCase.sourceString)
                        .append(" to ")
                        .append(testCase.targetString)
                        .append(", got ")
                        .append(got)
                        .append(", expected ")
                        .append(testCase.expected.toString())
                        .toString());
            }
            BigDecimal inverted = converter.convertInverse(testCase.input);
            if (compareTwoBigDecimal(BigDecimal.valueOf(1000), inverted, BigDecimal.valueOf(0.000001))) {
                continue;
            } else {
                fail(new StringBuilder()
                .append("Converting back to ")
                .append(testCase.sourceString)
                .append(" from ")
                .append(testCase.targetString)
                .append(": got ")
                .append(inverted)
                .append(", expected 1000")
                .toString());
            }
        }
    }

    @Test
    public void testUnitPreferencesWithCLDRTests() throws IOException {
        class TestCase {

            // TODO: content of outputUnitInOrder isn't checked? Only size?
            final ArrayList<Pair<String, MeasureUnitImpl>> outputUnitInOrder = new ArrayList<>();
            final ArrayList<BigDecimal> expectedInOrder = new ArrayList<>();
            /**
             * Test Case Data
             */
            String category;
            String usage;
            String region;
            Pair<String, MeasureUnitImpl> inputUnit;
            BigDecimal input;

            TestCase(String line) {
                String[] fields = line
                        .replaceAll(" ", "") // Remove all the spaces.
                        .replaceAll(",", "") // Remove all the commas.
                        .replaceAll("\t", "")
                        .split(";");

                String category = fields[0];
                String usage = fields[1];
                String region = fields[2];
                String inputValue = fields[4];
                String inputUnit = fields[5];
                ArrayList<Pair<String, String>> outputs = new ArrayList<>();

                for (int i = 6; i < fields.length - 2; i += 2) {
                    if (i == fields.length - 3) { // last field
                        outputs.add(Pair.of(fields[i + 2], fields[i + 1]));
                    } else {
                        outputs.add(Pair.of(fields[i + 1], fields[i]));
                    }
                }

                this.insertData(category, usage, region, inputUnit, inputValue, outputs);
            }

            private void insertData(String category,
                                    String usage,
                                    String region,
                                    String inputUnitString,
                                    String inputValue,
                                    ArrayList<Pair<String, String>> outputs /* Unit Identifier, expected value */) {
                this.category = category;
                this.usage = usage;
                this.region = region;
                this.inputUnit = Pair.of(inputUnitString, MeasureUnitImpl.UnitsParser.parseForIdentifier(inputUnitString));
                this.input = new BigDecimal(inputValue);
                for (Pair<String, String> output :
                        outputs) {
                    outputUnitInOrder.add(Pair.of(output.first, MeasureUnitImpl.UnitsParser.parseForIdentifier(output.first)));
                    expectedInOrder.add(new BigDecimal(output.second));
                }
            }

            @Override
            public String toString() {
                ArrayList<MeasureUnitImpl> outputUnits = new ArrayList<>();
                for (Pair<String, MeasureUnitImpl> unit : outputUnitInOrder) {
                    outputUnits.add(unit.second);
                }
                return "TestCase: " + category + ", " + usage + ", " + region + "; Input: " + input +
                    " " + inputUnit.first + "; Expected Values: " + expectedInOrder +
                    ", Expected Units: " + outputUnits;
            }
        }

        // Read Test data from the unitPreferencesTest
        String codePage = "UTF-8";
        ArrayList<TestCase> tests = new ArrayList<>();

        try (BufferedReader f = TestUtil.getDataReader("cldr/units/unitPreferencesTest.txt", codePage)) {
            while (true) {
                String line = f.readLine();
                if (line == null) break;
                if (line.isEmpty() || line.startsWith("#")) continue;
                tests.add(new TestCase(line));
            }
        }

        for (TestCase testCase : tests) {
            UnitsRouter router = new UnitsRouter(testCase.inputUnit.second, testCase.region, testCase.usage);
            List<Measure> measures = router.route(testCase.input, null).complexConverterResult.measures;

            assertEquals("For " + testCase.toString() + ", Measures size must be the same as expected units",
                    measures.size(), testCase.expectedInOrder.size());
            assertEquals("For " + testCase.toString() + ", Measures size must be the same as output units",
                    measures.size(), testCase.outputUnitInOrder.size());


            for (int i = 0; i < measures.size(); i++) {
                if (!UnitsTest
                        .compareTwoBigDecimal(testCase.expectedInOrder.get(i),
                                BigDecimal.valueOf(measures.get(i).getNumber().doubleValue()),
                                BigDecimal.valueOf(0.0000000001))) {
                    fail("Test failed: " + testCase + "; Got unexpected result: " + measures);
                }
            }
        }

        // Test UnitsRouter created with CLDR units identifiers.
        for (TestCase testCase : tests) {
            UnitsRouter router = new UnitsRouter(testCase.inputUnit.first, testCase.region, testCase.usage);
            List<Measure> measures = router.route(testCase.input, null).complexConverterResult.measures;

            assertEquals("Measures size must be the same as expected units",
                    measures.size(), testCase.expectedInOrder.size());
            assertEquals("Measures size must be the same as output units",
                    measures.size(), testCase.outputUnitInOrder.size());


            for (int i = 0; i < measures.size(); i++) {
                if (!UnitsTest
                        .compareTwoBigDecimal(testCase.expectedInOrder.get(i),
                                BigDecimal.valueOf(measures.get(i).getNumber().doubleValue()),
                                BigDecimal.valueOf(0.0000000001))) {
                    fail("Test failed: " + testCase + "; Got unexpected result: " + measures);
                }
            }
        }

    }
}
