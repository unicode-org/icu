// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.number;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.impl.number.DecimalQuantity_64BitBCD;
import com.ibm.icu.dev.impl.number.DecimalQuantity_ByteArrayBCD;
import com.ibm.icu.dev.impl.number.DecimalQuantity_SimpleStorage;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.RoundingUtils;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.Notation;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.number.Precision;
import com.ibm.icu.number.Scale;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.PluralRules.Operand;
import com.ibm.icu.util.ULocale;

@RunWith(JUnit4.class)
public class DecimalQuantityTest extends CoreTestFmwk {

    @Ignore
    @Test
    public void testBehavior() throws ParseException {

        // Make a list of several formatters to test the behavior of DecimalQuantity.
        List<LocalizedNumberFormatter> formats = new ArrayList<>();

        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);

        DecimalFormatProperties properties = new DecimalFormatProperties();
        formats.add(
                NumberFormatter.fromDecimalFormat(properties, symbols, null).locale(ULocale.ENGLISH));

        properties = new DecimalFormatProperties().setMinimumSignificantDigits(3)
                .setMaximumSignificantDigits(3).setCompactStyle(CompactStyle.LONG);
        formats.add(
                NumberFormatter.fromDecimalFormat(properties, symbols, null).locale(ULocale.ENGLISH));

        properties = new DecimalFormatProperties().setMinimumExponentDigits(1).setMaximumIntegerDigits(3)
                .setMaximumFractionDigits(1);
        formats.add(
                NumberFormatter.fromDecimalFormat(properties, symbols, null).locale(ULocale.ENGLISH));

        properties = new DecimalFormatProperties().setRoundingIncrement(new BigDecimal("0.5"));
        formats.add(
                NumberFormatter.fromDecimalFormat(properties, symbols, null).locale(ULocale.ENGLISH));

        String[] cases = {
                "1.0",
                "2.01",
                "1234.56",
                "3000.0",
                "0.00026418",
                "0.01789261",
                "468160.0",
                "999000.0",
                "999900.0",
                "999990.0",
                "0.0",
                "12345678901.0",
                "-5193.48", };

        String[] hardCases = {
                "9999999999999900.0",
                "789000000000000000000000.0",
                "789123123567853156372158.0",
                "987654321987654321987654321987654321987654311987654321.0", };

        String[] doubleCases = {
                "512.0000000000017",
                "4095.9999999999977",
                "4095.999999999998",
                "4095.9999999999986",
                "4095.999999999999",
                "4095.9999999999995",
                "4096.000000000001",
                "4096.000000000002",
                "4096.000000000003",
                "4096.000000000004",
                "4096.000000000005",
                "4096.0000000000055",
                "4096.000000000006",
                "4096.000000000007", };

        int i = 0;
        for (String str : cases) {
            testDecimalQuantity(i++, str, formats, 0);
        }

        i = 0;
        for (String str : hardCases) {
            testDecimalQuantity(i++, str, formats, 1);
        }

        i = 0;
        for (String str : doubleCases) {
            testDecimalQuantity(i++, str, formats, 2);
        }
    }

    static void testDecimalQuantity(
            int t,
            String str,
            List<LocalizedNumberFormatter> formats,
            int mode) {
        if (mode == 2) {
            assertEquals("Double is not valid", Double.toString(Double.parseDouble(str)), str);
        }

        List<DecimalQuantity> qs = new ArrayList<>();
        BigDecimal d = new BigDecimal(str);
        qs.add(new DecimalQuantity_SimpleStorage(d));
        if (mode == 0)
            qs.add(new DecimalQuantity_64BitBCD(d));
        qs.add(new DecimalQuantity_ByteArrayBCD(d));
        qs.add(new DecimalQuantity_DualStorageBCD(d));

        if (new BigDecimal(Double.toString(d.doubleValue())).compareTo(d) == 0) {
            double dv = d.doubleValue();
            qs.add(new DecimalQuantity_SimpleStorage(dv));
            if (mode == 0)
                qs.add(new DecimalQuantity_64BitBCD(dv));
            qs.add(new DecimalQuantity_ByteArrayBCD(dv));
            qs.add(new DecimalQuantity_DualStorageBCD(dv));
        }

        if (new BigDecimal(Long.toString(d.longValue())).compareTo(d) == 0) {
            double lv = d.longValue();
            qs.add(new DecimalQuantity_SimpleStorage(lv));
            if (mode == 0)
                qs.add(new DecimalQuantity_64BitBCD(lv));
            qs.add(new DecimalQuantity_ByteArrayBCD(lv));
            qs.add(new DecimalQuantity_DualStorageBCD(lv));
        }

        testDecimalQuantityExpectedOutput(qs.get(0), str);

        if (qs.size() == 1) {
            return;
        }

        for (int i = 1; i < qs.size(); i++) {
            DecimalQuantity q0 = qs.get(0);
            DecimalQuantity q1 = qs.get(i);
            testDecimalQuantityExpectedOutput(q1, str);
            testDecimalQuantityRounding(q0, q1);
            testDecimalQuantityRoundingInterval(q0, q1);
            testDecimalQuantityMath(q0, q1);
            testDecimalQuantityWithFormats(q0, q1, formats);
        }
    }

    private static void testDecimalQuantityExpectedOutput(DecimalQuantity rq, String expected) {
        DecimalQuantity q0 = rq.createCopy();
        // Force an accurate double
        q0.roundToInfinity();
        q0.setMinInteger(1);
        q0.setMinFraction(1);
        String actual = q0.toPlainString();
        assertEquals("Unexpected output from simple string conversion (" + q0 + ")", expected, actual);
    }

    private static final MathContext MATH_CONTEXT_HALF_EVEN = new MathContext(0, RoundingMode.HALF_EVEN);
    private static final MathContext MATH_CONTEXT_CEILING = new MathContext(0, RoundingMode.CEILING);
    @SuppressWarnings("unused")
    private static final MathContext MATH_CONTEXT_FLOOR = new MathContext(0, RoundingMode.FLOOR);
    private static final MathContext MATH_CONTEXT_PRECISION = new MathContext(3, RoundingMode.HALF_UP);

    private static void testDecimalQuantityRounding(DecimalQuantity rq0, DecimalQuantity rq1) {
        DecimalQuantity q0 = rq0.createCopy();
        DecimalQuantity q1 = rq1.createCopy();
        q0.roundToMagnitude(-1, MATH_CONTEXT_HALF_EVEN);
        q1.roundToMagnitude(-1, MATH_CONTEXT_HALF_EVEN);
        testDecimalQuantityBehavior(q0, q1);

        q0 = rq0.createCopy();
        q1 = rq1.createCopy();
        q0.roundToMagnitude(-1, MATH_CONTEXT_CEILING);
        q1.roundToMagnitude(-1, MATH_CONTEXT_CEILING);
        testDecimalQuantityBehavior(q0, q1);

        q0 = rq0.createCopy();
        q1 = rq1.createCopy();
        q0.roundToMagnitude(-1, MATH_CONTEXT_PRECISION);
        q1.roundToMagnitude(-1, MATH_CONTEXT_PRECISION);
        testDecimalQuantityBehavior(q0, q1);
    }

    private static void testDecimalQuantityRoundingInterval(DecimalQuantity rq0, DecimalQuantity rq1) {
        DecimalQuantity q0 = rq0.createCopy();
        DecimalQuantity q1 = rq1.createCopy();
        q0.roundToIncrement(new BigDecimal("0.05"), MATH_CONTEXT_HALF_EVEN);
        q1.roundToIncrement(new BigDecimal("0.05"), MATH_CONTEXT_HALF_EVEN);
        testDecimalQuantityBehavior(q0, q1);

        q0 = rq0.createCopy();
        q1 = rq1.createCopy();
        q0.roundToIncrement(new BigDecimal("0.05"), MATH_CONTEXT_CEILING);
        q1.roundToIncrement(new BigDecimal("0.05"), MATH_CONTEXT_CEILING);
        testDecimalQuantityBehavior(q0, q1);
    }

    private static void testDecimalQuantityMath(DecimalQuantity rq0, DecimalQuantity rq1) {
        DecimalQuantity q0 = rq0.createCopy();
        DecimalQuantity q1 = rq1.createCopy();
        q0.adjustMagnitude(-3);
        q1.adjustMagnitude(-3);
        testDecimalQuantityBehavior(q0, q1);

        q0 = rq0.createCopy();
        q1 = rq1.createCopy();
        q0.multiplyBy(new BigDecimal("3.14159"));
        q1.multiplyBy(new BigDecimal("3.14159"));
        testDecimalQuantityBehavior(q0, q1);
    }

    private static void testDecimalQuantityWithFormats(
            DecimalQuantity rq0,
            DecimalQuantity rq1,
            List<LocalizedNumberFormatter> formats) {
        for (LocalizedNumberFormatter format : formats) {
            DecimalQuantity q0 = rq0.createCopy();
            DecimalQuantity q1 = rq1.createCopy();
            FormattedStringBuilder nsb1 = new FormattedStringBuilder();
            FormattedStringBuilder nsb2 = new FormattedStringBuilder();
            format.formatImpl(q0, nsb1);
            format.formatImpl(q1, nsb2);
            String s1 = nsb1.toString();
            String s2 = nsb2.toString();
            assertEquals("Different output from formatter (" + q0 + ", " + q1 + ")", s1, s2);
        }
    }

    private static void testDecimalQuantityBehavior(DecimalQuantity rq0, DecimalQuantity rq1) {
        DecimalQuantity q0 = rq0.createCopy();
        DecimalQuantity q1 = rq1.createCopy();

        assertEquals("Different sign (" + q0 + ", " + q1 + ")", q0.isNegative(), q1.isNegative());

        assertEquals("Different fingerprint (" + q0 + ", " + q1 + ")",
                q0.getPositionFingerprint(),
                q1.getPositionFingerprint());

        assertDoubleEquals("Different double values (" + q0 + ", " + q1 + ")",
                q0.toDouble(),
                q1.toDouble());

        assertBigDecimalEquals("Different BigDecimal values (" + q0 + ", " + q1 + ")",
                q0.toBigDecimal(),
                q1.toBigDecimal());

        assertEquals("Different long values (" + q0 + ", " + q1 + ")",
                q0.toLong(true),
                q1.toLong(true));

        q0.roundToInfinity();
        q1.roundToInfinity();

        assertEquals("Different lower display magnitude",
                q0.getLowerDisplayMagnitude(),
                q1.getLowerDisplayMagnitude());
        assertEquals("Different upper display magnitude",
                q0.getUpperDisplayMagnitude(),
                q1.getUpperDisplayMagnitude());

        for (int m = q0.getUpperDisplayMagnitude(); m >= q0.getLowerDisplayMagnitude(); m--) {
            assertEquals("Different digit at magnitude " + m + " (" + q0 + ", " + q1 + ")",
                    q0.getDigit(m),
                    q1.getDigit(m));
        }

        if (rq0 instanceof DecimalQuantity_DualStorageBCD) {
            String message = ((DecimalQuantity_DualStorageBCD) rq0).checkHealth();
            if (message != null)
                errln(message);
        }
        if (rq1 instanceof DecimalQuantity_DualStorageBCD) {
            String message = ((DecimalQuantity_DualStorageBCD) rq1).checkHealth();
            if (message != null)
                errln(message);
        }
    }

    @Test
    public void testSwitchStorage() {
        DecimalQuantity_DualStorageBCD fq = new DecimalQuantity_DualStorageBCD();

        fq.setToLong(1234123412341234L);
        assertFalse("Should not be using byte array", fq.isUsingBytes());
        assertEquals("Failed on initialize", "1.234123412341234E+15", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        // Long -> Bytes
        fq.appendDigit((byte) 5, 0, true);
        assertTrue("Should be using byte array", fq.isUsingBytes());
        assertEquals("Failed on multiply", "1.2341234123412345E+16", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        // Bytes -> Long
        fq.roundToMagnitude(5, MATH_CONTEXT_HALF_EVEN);
        assertFalse("Should not be using byte array", fq.isUsingBytes());
        assertEquals("Failed on round", "1.23412341234E+16", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        // Bytes with popFromLeft
        fq.setToBigDecimal(new BigDecimal("999999999999999999"));
        assertToStringAndHealth(fq, "<DecimalQuantity 0:0 bytes 999999999999999999E0>");
        fq.applyMaxInteger(17);
        assertToStringAndHealth(fq, "<DecimalQuantity 0:0 bytes 99999999999999999E0>");
        fq.applyMaxInteger(16);
        assertToStringAndHealth(fq, "<DecimalQuantity 0:0 long 9999999999999999E0>");
        fq.applyMaxInteger(15);
        assertToStringAndHealth(fq, "<DecimalQuantity 0:0 long 999999999999999E0>");
    }

    @Test
    public void testAppend() {
        DecimalQuantity_DualStorageBCD fq = new DecimalQuantity_DualStorageBCD();
        fq.appendDigit((byte) 1, 0, true);
        assertEquals("Failed on append", "1E+0", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        fq.appendDigit((byte) 2, 0, true);
        assertEquals("Failed on append", "1.2E+1", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        fq.appendDigit((byte) 3, 1, true);
        assertEquals("Failed on append", "1.203E+3", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        fq.appendDigit((byte) 0, 1, true);
        assertEquals("Failed on append", "1.203E+5", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        fq.appendDigit((byte) 4, 0, true);
        assertEquals("Failed on append", "1.203004E+6", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        fq.appendDigit((byte) 0, 0, true);
        assertEquals("Failed on append", "1.203004E+7", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        fq.appendDigit((byte) 5, 0, false);
        assertEquals("Failed on append", "1.20300405E+7", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        fq.appendDigit((byte) 6, 0, false);
        assertEquals("Failed on append", "1.203004056E+7", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        fq.appendDigit((byte) 7, 3, false);
        assertEquals("Failed on append", "1.2030040560007E+7", fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
        StringBuilder baseExpected = new StringBuilder("1.2030040560007");
        for (int i = 0; i < 10; i++) {
            fq.appendDigit((byte) 8, 0, false);
            baseExpected.append('8');
            StringBuilder expected = new StringBuilder(baseExpected);
            expected.append("E+7");
            assertEquals("Failed on append", expected.toString(), fq.toScientificString());
            assertNull("Failed health check", fq.checkHealth());
        }
        fq.appendDigit((byte) 9, 2, false);
        baseExpected.append("009");
        StringBuilder expected = new StringBuilder(baseExpected);
        expected.append("E+7");
        assertEquals("Failed on append", expected.toString(), fq.toScientificString());
        assertNull("Failed health check", fq.checkHealth());
    }

    @Test
    public void testUseApproximateDoubleWhenAble() {
        Object[][] cases = {
                { 1.2345678, 1, MATH_CONTEXT_HALF_EVEN, false },
                { 1.2345678, 7, MATH_CONTEXT_HALF_EVEN, false },
                { 1.2345678, 12, MATH_CONTEXT_HALF_EVEN, false },
                { 1.2345678, 13, MATH_CONTEXT_HALF_EVEN, true },
                { 1.235, 1, MATH_CONTEXT_HALF_EVEN, false },
                { 1.235, 2, MATH_CONTEXT_HALF_EVEN, true },
                { 1.235, 3, MATH_CONTEXT_HALF_EVEN, false },
                { 1.000000000000001, 0, MATH_CONTEXT_HALF_EVEN, false },
                { 1.000000000000001, 0, MATH_CONTEXT_CEILING, true },
                { 1.235, 1, MATH_CONTEXT_CEILING, false },
                { 1.235, 2, MATH_CONTEXT_CEILING, false },
                { 1.235, 3, MATH_CONTEXT_CEILING, true } };

        for (Object[] cas : cases) {
            double d = (Double) cas[0];
            int maxFrac = (Integer) cas[1];
            MathContext mc = (MathContext) cas[2];
            boolean usesExact = (Boolean) cas[3];

            DecimalQuantity_DualStorageBCD fq = new DecimalQuantity_DualStorageBCD(d);
            assertTrue("Should be using approximate double", !fq.explicitExactDouble);
            fq.roundToMagnitude(-maxFrac, mc);
            assertEquals(
                    "Using approximate double after rounding: " + d + " maxFrac=" + maxFrac + " " + mc,
                    usesExact,
                    fq.explicitExactDouble);
        }
    }

    @Test
    public void testDecimalQuantityBehaviorStandalone() {
        DecimalQuantity_DualStorageBCD fq = new DecimalQuantity_DualStorageBCD();
        assertToStringAndHealth(fq, "<DecimalQuantity 0:0 long 0E0>");
        fq.setToInt(51423);
        assertToStringAndHealth(fq, "<DecimalQuantity 0:0 long 51423E0>");
        fq.adjustMagnitude(-3);
        assertToStringAndHealth(fq, "<DecimalQuantity 0:0 long 51423E-3>");
        fq.setToLong(90909090909000L);
        assertToStringAndHealth(fq, "<DecimalQuantity 0:0 long 90909090909E3>");
        fq.setMinInteger(2);
        fq.applyMaxInteger(5);
        assertToStringAndHealth(fq, "<DecimalQuantity 2:0 long 9E3>");
        fq.setMinFraction(3);
        assertToStringAndHealth(fq, "<DecimalQuantity 2:-3 long 9E3>");
        fq.setToDouble(987.654321);
        assertToStringAndHealth(fq, "<DecimalQuantity 2:-3 long 987654321E-6>");
        fq.roundToInfinity();
        assertToStringAndHealth(fq, "<DecimalQuantity 2:-3 long 987654321E-6>");
        fq.roundToIncrement(new BigDecimal("0.005"), MATH_CONTEXT_HALF_EVEN);
        assertToStringAndHealth(fq, "<DecimalQuantity 2:-3 long 987655E-3>");
        fq.roundToMagnitude(-2, MATH_CONTEXT_HALF_EVEN);
        assertToStringAndHealth(fq, "<DecimalQuantity 2:-3 long 98766E-2>");
    }

    @Test
    public void testFitsInLong() {
        DecimalQuantity_DualStorageBCD quantity = new DecimalQuantity_DualStorageBCD();
        quantity.setToInt(0);
        assertTrue("Zero should fit", quantity.fitsInLong());
        quantity.setToInt(42);
        assertTrue("Small int should fit", quantity.fitsInLong());
        quantity.setToDouble(0.1);
        assertFalse("Fraction should not fit", quantity.fitsInLong());
        quantity.setToDouble(42.1);
        assertFalse("Fraction should not fit", quantity.fitsInLong());
        quantity.setToLong(1000000);
        assertTrue("Large low-precision int should fit", quantity.fitsInLong());
        quantity.setToLong(1000000000000000000L);
        assertTrue("10^19 should fit", quantity.fitsInLong());
        quantity.setToLong(1234567890123456789L);
        assertTrue("A number between 10^19 and max long should fit", quantity.fitsInLong());
        quantity.setToLong(1234567890000000000L);
        assertTrue("A number with trailing zeros less than max long should fit", quantity.fitsInLong());
        quantity.setToLong(9223372026854775808L);
        assertTrue("A number less than max long but with similar digits should fit",
                quantity.fitsInLong());
        quantity.setToLong(9223372036854775806L);
        assertTrue("One less than max long should fit", quantity.fitsInLong());
        quantity.setToLong(9223372036854775807L);
        assertTrue("Max long should fit", quantity.fitsInLong());
        assertEquals("Max long should equal toLong", 9223372036854775807L, quantity.toLong(false));
        quantity.setToBigInteger(new BigInteger("9223372036854775808"));
        assertFalse("One greater than max long should not fit", quantity.fitsInLong());
        assertEquals("toLong(true) should truncate", 223372036854775808L, quantity.toLong(true));
        try {
            quantity.toLong(false);
            fail("One greater than max long is not convertible to long");
        } catch (ArithmeticException | AssertionError e) {
            // expected
        }
        quantity.setToBigInteger(new BigInteger("9223372046854775806"));
        assertFalse("A number between max long and 10^20 should not fit", quantity.fitsInLong());
        quantity.setToBigInteger(new BigInteger("9223372046800000000"));
        assertFalse("A large 10^19 number with trailing zeros should not fit", quantity.fitsInLong());
        quantity.setToBigInteger(new BigInteger("10000000000000000000"));
        assertFalse("10^20 should not fit", quantity.fitsInLong());
    }

    @Test
    public void testHardDoubleConversion() {
        // This test is somewhat duplicated from previous tests, but it is needed
        // for ICU4C compatibility.
        Object[][] cases = {
                { 512.0000000000017, "512.0000000000017" },
                { 4095.9999999999977, "4095.9999999999977" },
                { 4095.999999999998, "4095.999999999998" },
                { 4095.9999999999986, "4095.9999999999986" },
                { 4095.999999999999, "4095.999999999999" },
                { 4095.9999999999995, "4095.9999999999995" },
                { 4096.000000000001, "4096.000000000001" },
                { 4096.000000000002, "4096.000000000002" },
                { 4096.000000000003, "4096.000000000003" },
                { 4096.000000000004, "4096.000000000004" },
                { 4096.000000000005, "4096.000000000005" },
                { 4096.0000000000055, "4096.0000000000055" },
                { 4096.000000000006, "4096.000000000006" },
                { 4096.000000000007, "4096.000000000007" } };

        for (Object[] cas : cases) {
            double input = (Double) cas[0];
            String expectedOutput = (String) cas[1];

            DecimalQuantity q = new DecimalQuantity_DualStorageBCD(input);
            q.roundToInfinity();
            String actualOutput = q.toPlainString();
            assertEquals("", expectedOutput, actualOutput);
        }
    }

    @Test
    public void testToDouble() {
        Object[][] cases = new Object[][] {
            { "0", 0.0 },
            { "514.23", 514.23 },
            { "-3.142E-271", -3.142e-271 }
        };

        for (Object[] cas : cases) {
            String input = (String) cas[0];
            double expected = (Double) cas[1];

            DecimalQuantity q = new DecimalQuantity_DualStorageBCD();
            q.setToBigDecimal(new BigDecimal(input));
            double actual = q.toDouble();
            assertEquals("Doubles should exactly equal", expected, actual);
        }
    }

    @Test
    public void testMaxDigits() {
        DecimalQuantity_DualStorageBCD dq = new DecimalQuantity_DualStorageBCD(876.543);
        dq.roundToInfinity();
        dq.setMinInteger(0);
        dq.applyMaxInteger(2);
        dq.setMinFraction(0);
        dq.roundToMagnitude(-2, RoundingUtils.mathContextUnlimited(RoundingMode.FLOOR));
        assertEquals("Should trim, toPlainString", "76.54", dq.toPlainString());
        assertEquals("Should trim, toScientificString", "7.654E+1", dq.toScientificString());
        assertEquals("Should trim, toLong", 76, dq.toLong(true));
        assertEquals("Should trim, toFractionLong", 54, dq.toFractionLong(false));
        assertEquals("Should trim, toDouble", 76.54, dq.toDouble());
        assertEquals("Should trim, toBigDecimal", new BigDecimal("76.54"), dq.toBigDecimal());
    }

    @Test
    public void testNickelRounding() {
        Object[][] cases = new Object[][] {
            {1.000, -2, RoundingMode.HALF_EVEN, "1"},
            {1.001, -2, RoundingMode.HALF_EVEN, "1"},
            {1.010, -2, RoundingMode.HALF_EVEN, "1"},
            {1.020, -2, RoundingMode.HALF_EVEN, "1"},
            {1.024, -2, RoundingMode.HALF_EVEN, "1"},
            {1.025, -2, RoundingMode.HALF_EVEN, "1"},
            {1.025, -2, RoundingMode.HALF_DOWN, "1"},
            {1.025, -2, RoundingMode.HALF_UP,   "1.05"},
            {1.026, -2, RoundingMode.HALF_EVEN, "1.05"},
            {1.030, -2, RoundingMode.HALF_EVEN, "1.05"},
            {1.040, -2, RoundingMode.HALF_EVEN, "1.05"},
            {1.050, -2, RoundingMode.HALF_EVEN, "1.05"},
            {1.060, -2, RoundingMode.HALF_EVEN, "1.05"},
            {1.070, -2, RoundingMode.HALF_EVEN, "1.05"},
            {1.074, -2, RoundingMode.HALF_EVEN, "1.05"},
            {1.075, -2, RoundingMode.HALF_DOWN, "1.05"},
            {1.075, -2, RoundingMode.HALF_UP,   "1.1"},
            {1.075, -2, RoundingMode.HALF_EVEN, "1.1"},
            {1.076, -2, RoundingMode.HALF_EVEN, "1.1"},
            {1.080, -2, RoundingMode.HALF_EVEN, "1.1"},
            {1.090, -2, RoundingMode.HALF_EVEN, "1.1"},
            {1.099, -2, RoundingMode.HALF_EVEN, "1.1"},
            {1.999, -2, RoundingMode.HALF_EVEN, "2"},
            {2.25, -1, RoundingMode.HALF_EVEN, "2"},
            {2.25, -1, RoundingMode.HALF_UP,   "2.5"},
            {2.75, -1, RoundingMode.HALF_DOWN, "2.5"},
            {2.75, -1, RoundingMode.HALF_EVEN, "3"},
            {3.00, -1, RoundingMode.CEILING, "3"},
            {3.25, -1, RoundingMode.CEILING, "3.5"},
            {3.50, -1, RoundingMode.CEILING, "3.5"},
            {3.75, -1, RoundingMode.CEILING, "4"},
            {4.00, -1, RoundingMode.FLOOR, "4"},
            {4.25, -1, RoundingMode.FLOOR, "4"},
            {4.50, -1, RoundingMode.FLOOR, "4.5"},
            {4.75, -1, RoundingMode.FLOOR, "4.5"},
            {5.00, -1, RoundingMode.UP, "5"},
            {5.25, -1, RoundingMode.UP, "5.5"},
            {5.50, -1, RoundingMode.UP, "5.5"},
            {5.75, -1, RoundingMode.UP, "6"},
            {6.00, -1, RoundingMode.DOWN, "6"},
            {6.25, -1, RoundingMode.DOWN, "6"},
            {6.50, -1, RoundingMode.DOWN, "6.5"},
            {6.75, -1, RoundingMode.DOWN, "6.5"},
            {7.00, -1, RoundingMode.UNNECESSARY, "7"},
            {7.50, -1, RoundingMode.UNNECESSARY, "7.5"},
        };
        for (Object[] cas : cases) {
            double input = (Double) cas[0];
            int magnitude = (Integer) cas[1];
            RoundingMode roundingMode = (RoundingMode) cas[2];
            String expected = (String) cas[3];
            String message = input + " @ " + magnitude + " / " + roundingMode;
            for (int i=0; i<2; i++) {
                DecimalQuantity dq;
                if (i == 0) {
                    dq = new DecimalQuantity_DualStorageBCD(input);
                } else {
                    dq = new DecimalQuantity_SimpleStorage(input);
                }
                dq.roundToNickel(magnitude, RoundingUtils.mathContextUnlimited(roundingMode));
                String actual = dq.toPlainString();
                assertEquals(message, expected, actual);
            }
        }
        try {
            DecimalQuantity_DualStorageBCD dq = new DecimalQuantity_DualStorageBCD(7.1);
            dq.roundToNickel(-1, RoundingUtils.mathContextUnlimited(RoundingMode.UNNECESSARY));
            fail("Expected ArithmeticException");
        } catch (ArithmeticException expected) {
            // pass
        }
    }

    @Test
    public void testScientificAndCompactSuppressedExponent() {
        ULocale locale = new ULocale("fr-FR");

        Object[][] casesData = {
                // unlocalized formatter skeleton, input, string output, long output,
                // double output, BigDecimal output, plain string,
                // suppressed scientific exponent, suppressed compact exponent
                {"",              123456789, "123 456 789",  123456789L, 123456789.0, new BigDecimal("123456789"), "123456789", 0, 0},
                {"compact-long",  123456789, "123 millions", 123000000L, 123000000.0, new BigDecimal("123000000"), "123000000", 6, 6},
                {"compact-short", 123456789, "123 M",        123000000L, 123000000.0, new BigDecimal("123000000"), "123000000", 6, 6},
                {"scientific",    123456789, "1,234568E8",   123456800L, 123456800.0, new BigDecimal("123456800"), "123456800", 8, 8},

                {"",              1234567, "1 234 567",   1234567L, 1234567.0, new BigDecimal("1234567"), "1234567", 0, 0},
                {"compact-long",  1234567, "1,2 million", 1200000L, 1200000.0, new BigDecimal("1200000"), "1200000", 6, 6},
                {"compact-short", 1234567, "1,2 M",       1200000L, 1200000.0, new BigDecimal("1200000"), "1200000", 6, 6},
                {"scientific",    1234567, "1,234567E6",  1234567L, 1234567.0, new BigDecimal("1234567"), "1234567", 6, 6},

                {"",              123456, "123 456",   123456L, 123456.0, new BigDecimal("123456"), "123456", 0, 0},
                {"compact-long",  123456, "123 mille", 123000L, 123000.0, new BigDecimal("123000"), "123000", 3, 3},
                {"compact-short", 123456, "123 k",     123000L, 123000.0, new BigDecimal("123000"), "123000", 3, 3},
                {"scientific",    123456, "1,23456E5", 123456L, 123456.0, new BigDecimal("123456"), "123456", 5, 5},

                {"",              123, "123",    123L, 123.0, new BigDecimal("123"), "123", 0, 0},
                {"compact-long",  123, "123",    123L, 123.0, new BigDecimal("123"), "123", 0, 0},
                {"compact-short", 123, "123",    123L, 123.0, new BigDecimal("123"), "123", 0, 0},
                {"scientific",    123, "1,23E2", 123L, 123.0, new BigDecimal("123"), "123", 2, 2},

                {"",              1.2, "1,2",   1L, 1.2, new BigDecimal("1.2"), "1.2", 0, 0},
                {"compact-long",  1.2, "1,2",   1L, 1.2, new BigDecimal("1.2"), "1.2", 0, 0},
                {"compact-short", 1.2, "1,2",   1L, 1.2, new BigDecimal("1.2"), "1.2", 0, 0},
                {"scientific",    1.2, "1,2E0", 1L, 1.2, new BigDecimal("1.2"), "1.2", 0, 0},

                {"",              0.12, "0,12",   0L, 0.12, new BigDecimal("0.12"), "0.12",  0,  0},
                {"compact-long",  0.12, "0,12",   0L, 0.12, new BigDecimal("0.12"), "0.12",  0,  0},
                {"compact-short", 0.12, "0,12",   0L, 0.12, new BigDecimal("0.12"), "0.12",  0,  0},
                {"scientific",    0.12, "1,2E-1", 0L, 0.12, new BigDecimal("0.12"), "0.12", -1, -1},

                {"",              0.012, "0,012",   0L, 0.012, new BigDecimal("0.012"), "0.012",  0,  0},
                {"compact-long",  0.012, "0,012",   0L, 0.012, new BigDecimal("0.012"), "0.012",  0,  0},
                {"compact-short", 0.012, "0,012",   0L, 0.012, new BigDecimal("0.012"), "0.012",  0,  0},
                {"scientific",    0.012, "1,2E-2",  0L, 0.012, new BigDecimal("0.012"), "0.012", -2, -2},

                {"",              999.9, "999,9",     999L,  999.9,  new BigDecimal("999.9"), "999.9", 0, 0},
                {"compact-long",  999.9, "mille",     1000L, 1000.0, new BigDecimal("1000"),  "1000",  3, 3},
                {"compact-short", 999.9, "1 k",       1000L, 1000.0, new BigDecimal("1000"),  "1000",  3, 3},
                {"scientific",    999.9, "9,999E2",   999L,  999.9,  new BigDecimal("999.9"), "999.9", 2, 2},

                {"",              1000.0, "1 000",     1000L, 1000.0, new BigDecimal("1000"), "1000", 0, 0},
                {"compact-long",  1000.0, "mille",     1000L, 1000.0, new BigDecimal("1000"), "1000", 3, 3},
                {"compact-short", 1000.0, "1 k",       1000L, 1000.0, new BigDecimal("1000"), "1000", 3, 3},
                {"scientific",    1000.0, "1E3",       1000L, 1000.0, new BigDecimal("1000"), "1000", 3, 3},
        };

        for (Object[] caseDatum : casesData) {
            // test the helper methods used to compute plural operand values

            String skeleton = (String) caseDatum[0];
            LocalizedNumberFormatter formatter =
                    NumberFormatter.forSkeleton(skeleton)
                        .locale(locale);
            double input = ((Number) caseDatum[1]).doubleValue();
            String expectedString = (String) caseDatum[2];
            long expectedLong = (long) caseDatum[3];
            double expectedDouble = (double) caseDatum[4];
            BigDecimal expectedBigDecimal = (BigDecimal) caseDatum[5];
            String expectedPlainString = (String) caseDatum[6];
            int expectedSuppressedScientificExponent = (int) caseDatum[7];
            int expectedSuppressedCompactExponent = (int) caseDatum[8];

            FormattedNumber fn = formatter.format(input);
            DecimalQuantity_DualStorageBCD dq = (DecimalQuantity_DualStorageBCD)
                    fn.getFixedDecimal();
            String actualString = fn.toString();
            long actualLong = dq.toLong(true);
            double actualDouble = dq.toDouble();
            BigDecimal actualBigDecimal = dq.toBigDecimal();
            String actualPlainString = dq.toPlainString();
            int actualSuppressedScientificExponent = dq.getExponent();
            int actualSuppressedCompactExponent = dq.getExponent();

            assertEquals(
                    String.format("formatted number %s toString: %f", skeleton, input),
                    expectedString,
                    actualString);
            assertEquals(
                    String.format("formatted number %s toLong: %f", skeleton, input),
                    expectedLong,
                    actualLong);
            assertDoubleEquals(
                    String.format("formatted number %s toDouble: %f", skeleton, input),
                    expectedDouble,
                    actualDouble);
            assertBigDecimalEquals(
                    String.format("formatted number %s toBigDecimal: %f", skeleton, input),
                    expectedBigDecimal,
                    actualBigDecimal);
            assertEquals(
                    String.format("formatted number %s toPlainString: %f", skeleton, input),
                    expectedPlainString,
                    actualPlainString);
            assertEquals(
                    String.format("formatted number %s suppressed scientific exponent: %f", skeleton, input),
                    expectedSuppressedScientificExponent,
                    actualSuppressedScientificExponent);
            assertEquals(
                    String.format("formatted number %s suppressed compact exponent: %f", skeleton, input),
                    expectedSuppressedCompactExponent,
                    actualSuppressedCompactExponent);

            // test the actual computed values of the plural operands

            double expectedNOperand = expectedDouble;
            double expectedIOperand = expectedLong;
            double expectedEOperand = expectedSuppressedScientificExponent;
            double expectedCOperand = expectedSuppressedCompactExponent;
            double actualNOperand = dq.getPluralOperand(Operand.n);
            double actualIOperand = dq.getPluralOperand(Operand.i);
            double actualEOperand = dq.getPluralOperand(Operand.e);
            double actualCOperand = dq.getPluralOperand(Operand.c);

            assertEquals(
                    String.format("formatted number %s toString: %s", skeleton, input),
                    expectedString,
                    actualString);
            assertDoubleEquals(
                    String.format("formatted number %s n operand: %f", skeleton, input),
                    expectedNOperand,
                    actualNOperand);
            assertDoubleEquals(
                    String.format("formatted number %s i operand: %f", skeleton, input),
                    expectedIOperand,
                    actualIOperand);
            assertDoubleEquals(
                    String.format("formatted number %s e operand: %f", skeleton, input),
                    expectedEOperand,
                    actualEOperand);
            assertDoubleEquals(
                    String.format("formatted number %s c operand: %f", skeleton, input),
                    expectedCOperand,
                    actualCOperand);
        }
    }


    @Test
    public void testCompactNotationFractionPluralOperands() {
        ULocale locale = new ULocale("fr-FR");
        LocalizedNumberFormatter formatter =
                NumberFormatter.withLocale(locale)
                    .notation(Notation.compactLong())
                    .precision(Precision.fixedFraction(5))
                    .scale(Scale.powerOfTen(-1));
        double formatterInput = 12345;
        double inputVal = 1234.5;
        FormattedNumber fn = formatter.format(formatterInput);
        DecimalQuantity_DualStorageBCD dq = (DecimalQuantity_DualStorageBCD)
                fn.getFixedDecimal();

        double expectedNOperand = 1234.5;
        double expectedIOperand = 1234;
        double expectedFOperand = 50;
        double expectedTOperand = 5;
        double expectedVOperand = 2;
        double expectedWOperand = 1;
        double expectedEOperand = 3;
        double expectedCOperand = 3;
        String expectedString = "1,23450 millier";
        double actualNOperand = dq.getPluralOperand(Operand.n);
        double actualIOperand = dq.getPluralOperand(Operand.i);
        double actualFOperand = dq.getPluralOperand(Operand.f);
        double actualTOperand = dq.getPluralOperand(Operand.t);
        double actualVOperand = dq.getPluralOperand(Operand.v);
        double actualWOperand = dq.getPluralOperand(Operand.w);
        double actualEOperand = dq.getPluralOperand(Operand.e);
        double actualCOperand = dq.getPluralOperand(Operand.c);
        String actualString = fn.toString();

        assertDoubleEquals(
                String.format("compact decimal fraction n operand: %f", inputVal),
                expectedNOperand,
                actualNOperand);
        assertDoubleEquals(
                String.format("compact decimal fraction i operand: %f", inputVal),
                expectedIOperand,
                actualIOperand);
        assertDoubleEquals(
                String.format("compact decimal fraction f operand: %f", inputVal),
                expectedFOperand,
                actualFOperand);
        assertDoubleEquals(
                String.format("compact decimal fraction t operand: %f", inputVal),
                expectedTOperand,
                actualTOperand);
        assertDoubleEquals(
                String.format("compact decimal fraction v operand: %f", inputVal),
                expectedVOperand,
                actualVOperand);
        assertDoubleEquals(
                String.format("compact decimal fraction w operand: %f", inputVal),
                expectedWOperand,
                actualWOperand);
        assertDoubleEquals(
                String.format("compact decimal fraction e operand: %f", inputVal),
                expectedEOperand,
                actualEOperand);
        assertDoubleEquals(
                String.format("compact decimal fraction c operand: %f", inputVal),
                expectedCOperand,
                actualCOperand);
        assertEquals(
                String.format("compact decimal fraction toString: %f", inputVal),
                expectedString,
                actualString);
    }

    @Test
    public void testSuppressedExponentUnchangedByInitialScaling() {
        ULocale locale = new ULocale("fr-FR");
        LocalizedNumberFormatter withLocale = NumberFormatter.withLocale(locale);
        LocalizedNumberFormatter compactLong =
                withLocale.notation(Notation.compactLong());
        LocalizedNumberFormatter compactScaled =
                compactLong.scale(Scale.powerOfTen(3));

        Object[][] casesData = {
                // input, compact long string output,
                // compact n operand, compact i operand, compact e operand,
                // compact c operand
                {123456789, "123 millions", 123000000.0, 123000000.0, 6.0, 6.0},
                {1234567,   "1,2 million",  1200000.0,   1200000.0,   6.0, 6.0},
                {123456,    "123 mille",    123000.0,    123000.0,    3.0, 3.0},
                {123,       "123",          123.0,       123.0,       0.0, 0.0},
        };

        for (Object[] caseDatum : casesData) {
            int input = (int) caseDatum[0];
            String expectedString = (String) caseDatum[1];
            double expectedNOperand = (double) caseDatum[2];
            double expectedIOperand = (double) caseDatum[3];
            double expectedEOperand = (double) caseDatum[4];
            double expectedCOperand = (double) caseDatum[5];

            FormattedNumber fnCompactScaled = compactScaled.format(input);
            DecimalQuantity_DualStorageBCD dqCompactScaled =
                    (DecimalQuantity_DualStorageBCD) fnCompactScaled.getFixedDecimal();
            double compactScaledCOperand = dqCompactScaled.getPluralOperand(Operand.c);

            FormattedNumber fnCompact = compactLong.format(input);
            DecimalQuantity_DualStorageBCD dqCompact =
                    (DecimalQuantity_DualStorageBCD) fnCompact.getFixedDecimal();
            String actualString = fnCompact.toString();
            double compactNOperand = dqCompact.getPluralOperand(Operand.n);
            double compactIOperand = dqCompact.getPluralOperand(Operand.i);
            double compactEOperand = dqCompact.getPluralOperand(Operand.e);
            double compactCOperand = dqCompact.getPluralOperand(Operand.c);
            assertEquals(
                    String.format("formatted number compactLong toString: %s", input),
                    expectedString,
                    actualString);
            assertDoubleEquals(
                    String.format("compact decimal %d, n operand vs. expected", input),
                    expectedNOperand,
                    compactNOperand);
            assertDoubleEquals(
                    String.format("compact decimal %d, i operand vs. expected", input),
                    expectedIOperand,
                    compactIOperand);
            assertDoubleEquals(
                    String.format("compact decimal %d, e operand vs. expected", input),
                    expectedEOperand,
                    compactEOperand);
            assertDoubleEquals(
                    String.format("compact decimal %d, c operand vs. expected", input),
                    expectedCOperand,
                    compactCOperand);

            // By scaling by 10^3 in a locale that has words / compact notation
            // based on powers of 10^3, we guarantee that the suppressed
            // exponent will differ by 3.
            assertDoubleEquals(
                    String.format("decimal %d, c operand for compact vs. compact scaled", input),
                    compactCOperand + 3,
                    compactScaledCOperand);
        }
    }

    @Test
    public void testDecimalQuantityParseFormatRoundTrip() {
        Object[] casesData = {
                // number string
                "0",
                "1",
                "1.0",
                "1.00",
                "1.1",
                "1.10",
                "-1.10",
                "0.0",
                "1c5",
                "1.0c5",
                "1.1c5",
                "1.10c5",
                "0.00",
                "0.1",
                "1c-1",
                "1.0c-1"
        };

        for (Object caseDatum : casesData) {
            String numStr = (String) caseDatum;
            DecimalQuantity dq = DecimalQuantity_DualStorageBCD.fromExponentString(numStr);
            String roundTrip = dq.toExponentString();

            assertEquals("DecimalQuantity format(parse(s)) should equal original s", numStr, roundTrip);
        }

        assertEquals("Zero ignored for visible exponent",
                "1",
                DecimalQuantity_DualStorageBCD.fromExponentString("1c0").toExponentString());
        assertEquals("Zero ignored for visible exponent",
                "1.0",
                DecimalQuantity_DualStorageBCD.fromExponentString("1.0c0").toExponentString());
    }

    static boolean doubleEquals(double d1, double d2) {
        return (Math.abs(d1 - d2) < 1e-6) || (Math.abs((d1 - d2) / d1) < 1e-6);
    }

    static void assertDoubleEquals(String message, double d1, double d2) {
        boolean equal = doubleEquals(d1, d2);
        handleAssert(equal, message, d1, d2, null, false);
    }

    static void assertBigDecimalEquals(String message, String d1, BigDecimal d2) {
        assertBigDecimalEquals(message, new BigDecimal(d1), d2);
    }

    static void assertBigDecimalEquals(String message, BigDecimal d1, BigDecimal d2) {
        boolean equal = d1.compareTo(d2) == 0;
        handleAssert(equal, message, d1, d2, null, false);
    }

    static void assertToStringAndHealth(DecimalQuantity_DualStorageBCD fq, String expected) {
        String actual = fq.toString();
        assertEquals("DecimalQuantity toString", expected, actual);
        String health = fq.checkHealth();
        assertNull("DecimalQuantity health", health);
    }
}
