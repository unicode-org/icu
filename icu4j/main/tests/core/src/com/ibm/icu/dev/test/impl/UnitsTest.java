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
import com.ibm.icu.impl.units.UnitConverter;
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
        ConversionRates rates = new ConversionRates();
        MeasureUnit input = MeasureUnit.FOOT;
        MeasureUnit output = MeasureUnit.forIdentifier("foot-and-inch");
        final MeasureUnitImpl inputImpl = MeasureUnitImpl.forIdentifier(input.getIdentifier());
        final MeasureUnitImpl outputImpl = MeasureUnitImpl.forIdentifier(output.getIdentifier());
        ComplexUnitsConverter converter = new ComplexUnitsConverter(inputImpl, outputImpl, rates);

        // Significantly less than 2.0.
        List<Measure> measures = converter.convert(BigDecimal.valueOf(1.9999), null);
        assertEquals("measures length", 2, measures.size());
        assertEquals("1.9999: measures[0] value", BigDecimal.valueOf(1), measures.get(0).getNumber());
        assertEquals("1.9999: measures[0] unit", MeasureUnit.FOOT.getIdentifier(),
                measures.get(0).getUnit().getIdentifier());

        assertTrue("1.9999: measures[1] value", compareTwoBigDecimal(BigDecimal.valueOf(11.9988),
                BigDecimal.valueOf(measures.get(1).getNumber().doubleValue()), BigDecimal.valueOf(0.0001)));
        assertEquals("1.9999: measures[1] unit", MeasureUnit.INCH.getIdentifier(),
                measures.get(1).getUnit().getIdentifier());

        // TODO(icu-units#100): consider factoring out the set of tests to make
        // this function more data-driven, *after* dealing appropriately with
        // the C++ memory leaks that can be demonstrated by the C++ version of
        // this code.

        // A minimal nudge under 2.0.
        List<Measure> measures2 =
            converter.convert(BigDecimal.valueOf(2.0).subtract(ComplexUnitsConverter.EPSILON), null);
        assertEquals("measures length", 2, measures2.size());
        assertEquals("1 - eps: measures[0] value", BigDecimal.valueOf(2), measures2.get(0).getNumber());
        assertEquals("1 - eps: measures[0] unit", MeasureUnit.FOOT.getIdentifier(),
                measures2.get(0).getUnit().getIdentifier());
        assertEquals("1 - eps: measures[1] value", BigDecimal.ZERO, measures2.get(1).getNumber());
        assertEquals("1 - eps: measures[1] unit", MeasureUnit.INCH.getIdentifier(),
                measures2.get(1).getUnit().getIdentifier());

        // Testing precision with meter and light-year. 1e-16 light years is
        // 0.946073 meters, and double precision can provide only ~15 decimal
        // digits, so we don't expect to get anything less than 1 meter.

        // An epsilon's nudge under one light-year: should give 1 ly, 0 m.
        input = MeasureUnit.LIGHT_YEAR;
        output = MeasureUnit.forIdentifier("light-year-and-meter");
        final MeasureUnitImpl inputImpl3 = MeasureUnitImpl.forIdentifier(input.getIdentifier());
        final MeasureUnitImpl outputImpl3 = MeasureUnitImpl.forIdentifier(output.getIdentifier());

        ComplexUnitsConverter converter3 = new ComplexUnitsConverter(inputImpl3, outputImpl3, rates);

        List<Measure> measures3 =
            converter3.convert(BigDecimal.valueOf(2.0).subtract(ComplexUnitsConverter.EPSILON), null);
        assertEquals("measures length", 2, measures3.size());
        assertEquals("light-year test: measures[0] value", BigDecimal.valueOf(2), measures3.get(0).getNumber());
        assertEquals("light-year test: measures[0] unit", MeasureUnit.LIGHT_YEAR.getIdentifier(),
                measures3.get(0).getUnit().getIdentifier());
        assertEquals("light-year test: measures[1] value", BigDecimal.ZERO, measures3.get(1).getNumber());
        assertEquals("light-year test: measures[1] unit", MeasureUnit.METER.getIdentifier(),
                measures3.get(1).getUnit().getIdentifier());

        // 1e-15 light years is 9.46073 meters (calculated using "bc" and the CLDR
        // conversion factor). With double-precision maths, we get 10.5. In this
        // case, we're off by almost 1 meter.
        List<Measure> measures4 = converter3.convert(BigDecimal.valueOf(1.0 + 1e-15), null);
        assertEquals("measures length", 2, measures4.size());
        assertEquals("light-year test: measures[0] value", BigDecimal.ONE, measures4.get(0).getNumber());
        assertEquals("light-year test: measures[0] unit", MeasureUnit.LIGHT_YEAR.getIdentifier(),
                measures4.get(0).getUnit().getIdentifier());
        assertTrue("light-year test: measures[1] value", compareTwoBigDecimal(BigDecimal.valueOf(10),
                BigDecimal.valueOf(measures4.get(1).getNumber().doubleValue()),
                BigDecimal.valueOf(1)));
        assertEquals("light-year test: measures[1] unit", MeasureUnit.METER.getIdentifier(),
                measures4.get(1).getUnit().getIdentifier());

        // 2e-16 light years is 1.892146 meters. We consider this in the noise, and
        // thus expect a 0. (This test fails when 2e-16 is increased to 4e-16.)
        List<Measure> measures5 = converter3.convert(BigDecimal.valueOf(1.0 + 2e-17), null);
        assertEquals("measures length", 2, measures5.size());
        assertEquals("light-year test: measures[0] value", BigDecimal.ONE, measures5.get(0).getNumber());
        assertEquals("light-year test: measures[0] unit", MeasureUnit.LIGHT_YEAR.getIdentifier(),
                measures5.get(0).getUnit().getIdentifier());
        assertEquals("light-year test: measures[1] value", BigDecimal.valueOf(0.0),
                measures5.get(1).getNumber());
        assertEquals("light-year test: measures[1] unit", MeasureUnit.METER.getIdentifier(),
                measures5.get(1).getUnit().getIdentifier());

        // TODO(icu-units#63): test negative numbers!
    }


    @Test
    public void testComplexUnitConverterSorting() {

        MeasureUnitImpl source = MeasureUnitImpl.forIdentifier("meter");
        MeasureUnitImpl target = MeasureUnitImpl.forIdentifier("inch-and-foot");
        ConversionRates conversionRates = new ConversionRates();

        ComplexUnitsConverter complexConverter = new ComplexUnitsConverter(source, target, conversionRates);
        List<Measure> measures = complexConverter.convert(BigDecimal.valueOf(10.0), null);

        assertEquals(measures.size(), 2);
        assertEquals("inch-and-foot unit 0", "inch", measures.get(0).getUnit().getIdentifier());
        assertEquals("inch-and-foot unit 1", "foot", measures.get(1).getUnit().getIdentifier());

        assertEquals("inch-and-foot value 0", 9.7008, measures.get(0).getNumber().doubleValue(), 0.0001);
        assertEquals("inch-and-foot value 1", 32, measures.get(1).getNumber().doubleValue(), 0.0001);
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
    public void testConverterForTemperature() {
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
                new TestData("celsius", "fahrenheit", 1000, 1832),
                new TestData("fahrenheit", "fahrenheit", 1000, 1000),
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
            UnitConverter converter = new UnitConverter(testCase.source, testCase.target, conversionRates);
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
    public void testUnitPreferencesFromUnitTests() throws IOException {
        class TestCase {

            final ArrayList<Pair<String, MeasureUnitImpl>> outputUnitInOrder = new ArrayList<>();
            final ArrayList<BigDecimal> expectedInOrder = new ArrayList<>();
            /**
             * Test Case Data
             */
            @SuppressWarnings("unused")
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

        for (TestCase testCase :
                tests) {
            UnitsRouter router = new UnitsRouter(testCase.inputUnit.second, testCase.region, testCase.usage);
            List<Measure> measures = router.route(testCase.input, null).measures;

            assertEquals("Measures size must be the same as expected units",
                    measures.size(), testCase.expectedInOrder.size());
            assertEquals("Measures size must be the same as output units",
                    measures.size(), testCase.outputUnitInOrder.size());


            for (int i = 0; i < measures.size(); i++) {
                if (!UnitsTest
                        .compareTwoBigDecimal(testCase.expectedInOrder.get(i),
                                BigDecimal.valueOf(measures.get(i).getNumber().doubleValue()),
                                BigDecimal.valueOf(0.00001))) {
                    fail(testCase.toString() + measures.toString());
                }
            }
        }
    }
}
