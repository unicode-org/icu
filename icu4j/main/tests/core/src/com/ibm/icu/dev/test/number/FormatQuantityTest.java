// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.FormatQuantity1;
import com.ibm.icu.impl.number.FormatQuantity2;
import com.ibm.icu.impl.number.FormatQuantity3;
import com.ibm.icu.impl.number.FormatQuantity4;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.CompactDecimalFormat.CompactStyle;
import com.ibm.icu.util.ULocale;

import newapi.impl.NumberFormatterImpl;
import newapi.impl.NumberPropertyMapper;

/** TODO: This is a temporary name for this class. Suggestions for a better name? */
public class FormatQuantityTest extends TestFmwk {

  @Test
  public void testBehavior() throws ParseException {

    // Make a list of several formatters to test the behavior of FormatQuantity.
    List<NumberFormatterImpl> formats = new ArrayList<NumberFormatterImpl>();

    Properties properties = new Properties();
    formats.add(NumberPropertyMapper.create(properties, null, ULocale.ENGLISH));

    properties =
        new Properties()
            .setMinimumSignificantDigits(3)
            .setMaximumSignificantDigits(3)
            .setCompactStyle(CompactStyle.LONG);
    formats.add(NumberPropertyMapper.create(properties, null, ULocale.ENGLISH));

    properties =
        new Properties()
            .setMinimumExponentDigits(1)
            .setMaximumIntegerDigits(3)
            .setMaximumFractionDigits(1);
    formats.add(NumberPropertyMapper.create(properties, null, ULocale.ENGLISH));

    properties = new Properties().setRoundingIncrement(new BigDecimal("0.5"));
    formats.add(NumberPropertyMapper.create(properties, null, ULocale.ENGLISH));

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
      "-5193.48",
    };

    String[] hardCases = {
      "9999999999999900.0",
      "789000000000000000000000.0",
      "789123123567853156372158.0",
      "987654321987654321987654321987654321987654311987654321.0",
    };

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
      "4096.000000000007",
    };

    int i = 0;
    for (String str : cases) {
      testFormatQuantity(i++, str, formats, 0);
    }

    i = 0;
    for (String str : hardCases) {
      testFormatQuantity(i++, str, formats, 1);
    }

    i = 0;
    for (String str : doubleCases) {
      testFormatQuantity(i++, str, formats, 2);
    }
  }

  static void testFormatQuantity(int t, String str, List<NumberFormatterImpl> formats, int mode) {
    if (mode == 2) {
      assertEquals("Double is not valid", Double.toString(Double.parseDouble(str)), str);
    }

    List<FormatQuantity> qs = new ArrayList<FormatQuantity>();
    BigDecimal d = new BigDecimal(str);
    qs.add(new FormatQuantity1(d));
    if (mode == 0) qs.add(new FormatQuantity2(d));
    qs.add(new FormatQuantity3(d));
    qs.add(new FormatQuantity4(d));

    if (new BigDecimal(Double.toString(d.doubleValue())).compareTo(d) == 0) {
      double dv = d.doubleValue();
      qs.add(new FormatQuantity1(dv));
      if (mode == 0) qs.add(new FormatQuantity2(dv));
      qs.add(new FormatQuantity3(dv));
      qs.add(new FormatQuantity4(dv));
    }

    if (new BigDecimal(Long.toString(d.longValue())).compareTo(d) == 0) {
      double lv = d.longValue();
      qs.add(new FormatQuantity1(lv));
      if (mode == 0) qs.add(new FormatQuantity2(lv));
      qs.add(new FormatQuantity3(lv));
      qs.add(new FormatQuantity4(lv));
    }

    testFormatQuantityExpectedOutput(qs.get(0), str);

    if (qs.size() == 1) {
      return;
    }

    for (int i = 1; i < qs.size(); i++) {
      FormatQuantity q0 = qs.get(0);
      FormatQuantity q1 = qs.get(i);
      testFormatQuantityExpectedOutput(q1, str);
      testFormatQuantityRounding(q0, q1);
      testFormatQuantityRoundingInterval(q0, q1);
      testFormatQuantityMath(q0, q1);
      testFormatQuantityWithFormats(q0, q1, formats);
    }
  }

  private static void testFormatQuantityExpectedOutput(FormatQuantity rq, String expected) {
    StringBuilder sb = new StringBuilder();
    FormatQuantity q0 = rq.createCopy();
    // Force an accurate double
    q0.roundToInfinity();
    q0.setIntegerLength(1, Integer.MAX_VALUE);
    q0.setFractionLength(1, Integer.MAX_VALUE);
    for (int m = q0.getUpperDisplayMagnitude(); m >= q0.getLowerDisplayMagnitude(); m--) {
      sb.append(q0.getDigit(m));
      if (m == 0) sb.append('.');
    }
    if (q0.isNegative()) {
      sb.insert(0, '-');
    }
    String actual = sb.toString();
    assertEquals("Unexpected output from simple string conversion (" + q0 + ")", expected, actual);
  }

  private static final MathContext MATH_CONTEXT_HALF_EVEN =
      new MathContext(0, RoundingMode.HALF_EVEN);
  private static final MathContext MATH_CONTEXT_CEILING = new MathContext(0, RoundingMode.CEILING);
  private static final MathContext MATH_CONTEXT_PRECISION =
      new MathContext(3, RoundingMode.HALF_UP);

  private static void testFormatQuantityRounding(FormatQuantity rq0, FormatQuantity rq1) {
    FormatQuantity q0 = rq0.createCopy();
    FormatQuantity q1 = rq1.createCopy();
    q0.roundToMagnitude(-1, MATH_CONTEXT_HALF_EVEN);
    q1.roundToMagnitude(-1, MATH_CONTEXT_HALF_EVEN);
    testFormatQuantityBehavior(q0, q1);

    q0 = rq0.createCopy();
    q1 = rq1.createCopy();
    q0.roundToMagnitude(-1, MATH_CONTEXT_CEILING);
    q1.roundToMagnitude(-1, MATH_CONTEXT_CEILING);
    testFormatQuantityBehavior(q0, q1);

    q0 = rq0.createCopy();
    q1 = rq1.createCopy();
    q0.roundToMagnitude(-1, MATH_CONTEXT_PRECISION);
    q1.roundToMagnitude(-1, MATH_CONTEXT_PRECISION);
    testFormatQuantityBehavior(q0, q1);
  }

  private static void testFormatQuantityRoundingInterval(FormatQuantity rq0, FormatQuantity rq1) {
    FormatQuantity q0 = rq0.createCopy();
    FormatQuantity q1 = rq1.createCopy();
    q0.roundToIncrement(new BigDecimal("0.05"), MATH_CONTEXT_HALF_EVEN);
    q1.roundToIncrement(new BigDecimal("0.05"), MATH_CONTEXT_HALF_EVEN);
    testFormatQuantityBehavior(q0, q1);

    q0 = rq0.createCopy();
    q1 = rq1.createCopy();
    q0.roundToIncrement(new BigDecimal("0.05"), MATH_CONTEXT_CEILING);
    q1.roundToIncrement(new BigDecimal("0.05"), MATH_CONTEXT_CEILING);
    testFormatQuantityBehavior(q0, q1);
  }

  private static void testFormatQuantityMath(FormatQuantity rq0, FormatQuantity rq1) {
    FormatQuantity q0 = rq0.createCopy();
    FormatQuantity q1 = rq1.createCopy();
    q0.adjustMagnitude(-3);
    q1.adjustMagnitude(-3);
    testFormatQuantityBehavior(q0, q1);

    q0 = rq0.createCopy();
    q1 = rq1.createCopy();
    q0.multiplyBy(new BigDecimal("3.14159"));
    q1.multiplyBy(new BigDecimal("3.14159"));
    testFormatQuantityBehavior(q0, q1);
  }

  private static void testFormatQuantityWithFormats(
      FormatQuantity rq0, FormatQuantity rq1, List<NumberFormatterImpl> formats) {
    for (NumberFormatterImpl format : formats) {
      FormatQuantity q0 = rq0.createCopy();
      FormatQuantity q1 = rq1.createCopy();
      String s1 = format.format(q0).toString();
      String s2 = format.format(q1).toString();
      assertEquals("Different output from formatter (" + q0 + ", " + q1 + ")", s1, s2);
    }
  }

  private static void testFormatQuantityBehavior(FormatQuantity rq0, FormatQuantity rq1) {
    FormatQuantity q0 = rq0.createCopy();
    FormatQuantity q1 = rq1.createCopy();

    assertEquals("Different sign (" + q0 + ", " + q1 + ")", q0.isNegative(), q1.isNegative());

    assertEquals(
        "Different fingerprint (" + q0 + ", " + q1 + ")",
        q0.getPositionFingerprint(),
        q1.getPositionFingerprint());

    assertDoubleEquals(
        "Different double values (" + q0 + ", " + q1 + ")", q0.toDouble(), q1.toDouble());

    assertBigDecimalEquals(
        "Different BigDecimal values (" + q0 + ", " + q1 + ")",
        q0.toBigDecimal(),
        q1.toBigDecimal());

    q0.roundToInfinity();
    q1.roundToInfinity();

    assertEquals(
        "Different lower display magnitude",
        q0.getLowerDisplayMagnitude(),
        q1.getLowerDisplayMagnitude());
    assertEquals(
        "Different upper display magnitude",
        q0.getUpperDisplayMagnitude(),
        q1.getUpperDisplayMagnitude());

    for (int m = q0.getUpperDisplayMagnitude(); m >= q0.getLowerDisplayMagnitude(); m--) {
      assertEquals(
          "Different digit at magnitude " + m + " (" + q0 + ", " + q1 + ")",
          q0.getDigit(m),
          q1.getDigit(m));
    }

    if (rq0 instanceof FormatQuantity4) {
      String message = ((FormatQuantity4) rq0).checkHealth();
      if (message != null) errln(message);
    }
    if (rq1 instanceof FormatQuantity4) {
      String message = ((FormatQuantity4) rq1).checkHealth();
      if (message != null) errln(message);
    }
  }

  @Test
  public void testSwitchStorage() {
    FormatQuantity4 fq = new FormatQuantity4();

    fq.setToLong(1234123412341234L);
    assertFalse("Should not be using byte array", fq.usingBytes());
    assertBigDecimalEquals("Failed on initialize", "1234123412341234", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    // Long -> Bytes
    fq.appendDigit((byte) 5, 0, true);
    assertTrue("Should be using byte array", fq.usingBytes());
    assertBigDecimalEquals("Failed on multiply", "12341234123412345", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    // Bytes -> Long
    fq.roundToMagnitude(5, MATH_CONTEXT_HALF_EVEN);
    assertFalse("Should not be using byte array", fq.usingBytes());
    assertBigDecimalEquals("Failed on round", "12341234123400000", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
  }

  @Test
  public void testAppend() {
    FormatQuantity4 fq = new FormatQuantity4();
    fq.appendDigit((byte) 1, 0, true);
    assertBigDecimalEquals("Failed on append", "1.", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    fq.appendDigit((byte) 2, 0, true);
    assertBigDecimalEquals("Failed on append", "12.", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    fq.appendDigit((byte) 3, 1, true);
    assertBigDecimalEquals("Failed on append", "1203.", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    fq.appendDigit((byte) 0, 1, true);
    assertBigDecimalEquals("Failed on append", "120300.", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    fq.appendDigit((byte) 4, 0, true);
    assertBigDecimalEquals("Failed on append", "1203004.", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    fq.appendDigit((byte) 0, 0, true);
    assertBigDecimalEquals("Failed on append", "12030040.", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    fq.appendDigit((byte) 5, 0, false);
    assertBigDecimalEquals("Failed on append", "12030040.5", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    fq.appendDigit((byte) 6, 0, false);
    assertBigDecimalEquals("Failed on append", "12030040.56", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    fq.appendDigit((byte) 7, 3, false);
    assertBigDecimalEquals("Failed on append", "12030040.560007", fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
    StringBuilder expected = new StringBuilder("12030040.560007");
    for (int i = 0; i < 10; i++) {
      fq.appendDigit((byte) 8, 0, false);
      expected.append("8");
      assertBigDecimalEquals("Failed on append", expected.toString(), fq.toBigDecimal());
      assertNull("Failed health check", fq.checkHealth());
    }
    fq.appendDigit((byte) 9, 2, false);
    expected.append("009");
    assertBigDecimalEquals("Failed on append", expected.toString(), fq.toBigDecimal());
    assertNull("Failed health check", fq.checkHealth());
  }

  @Test
  public void testConvertToAccurateDouble() {
    // based on https://github.com/google/double-conversion/issues/28
    double[] hardDoubles = {
      1651087494906221570.0,
      -5074790912492772E-327,
      83602530019752571E-327,
      2.207817077636718750000000000000,
      1.818351745605468750000000000000,
      3.941719055175781250000000000000,
      3.738609313964843750000000000000,
      3.967735290527343750000000000000,
      1.328025817871093750000000000000,
      3.920967102050781250000000000000,
      1.015235900878906250000000000000,
      1.335227966308593750000000000000,
      1.344520568847656250000000000000,
      2.879127502441406250000000000000,
      3.695838928222656250000000000000,
      1.845344543457031250000000000000,
      3.793952941894531250000000000000,
      3.211402893066406250000000000000,
      2.565971374511718750000000000000,
      0.965156555175781250000000000000,
      2.700004577636718750000000000000,
      0.767097473144531250000000000000,
      1.780448913574218750000000000000,
      2.624839782714843750000000000000,
      1.305290222167968750000000000000,
      3.834922790527343750000000000000,
    };

    double[] integerDoubles = {
      51423,
      51423e10,
      4.503599627370496E15,
      6.789512076111555E15,
      9.007199254740991E15,
      9.007199254740992E15
    };

    for (double d : hardDoubles) {
      checkDoubleBehavior(d, true, "");
    }

    for (double d : integerDoubles) {
      checkDoubleBehavior(d, false, "");
    }

    assertEquals("NaN check failed", Double.NaN, new FormatQuantity4(Double.NaN).toDouble());
    assertEquals(
        "Inf check failed",
        Double.POSITIVE_INFINITY,
        new FormatQuantity4(Double.POSITIVE_INFINITY).toDouble());
    assertEquals(
        "-Inf check failed",
        Double.NEGATIVE_INFINITY,
        new FormatQuantity4(Double.NEGATIVE_INFINITY).toDouble());

    // Generate random doubles
    String alert = "UNEXPECTED FAILURE: PLEASE REPORT THIS MESSAGE TO THE ICU TEAM: ";
    Random rnd = new Random();
    for (int i = 0; i < 1000000; i++) {
      double d = Double.longBitsToDouble(rnd.nextLong());
      if (Double.isNaN(d) || Double.isInfinite(d)) continue;
      checkDoubleBehavior(d, false, alert);
    }
  }

  private static void checkDoubleBehavior(double d, boolean explicitRequired, String alert) {
    FormatQuantity4 fq = new FormatQuantity4(d);
    if (explicitRequired)
      assertTrue(alert + "Should be using approximate double", !fq.explicitExactDouble);
    assertEquals(alert + "Initial construction from hard double", d, fq.toDouble());
    fq.roundToInfinity();
    if (explicitRequired)
      assertTrue(alert + "Should not be using approximate double", fq.explicitExactDouble);
    assertDoubleEquals(alert + "After conversion to exact BCD (double)", d, fq.toDouble());
    assertBigDecimalEquals(
        alert + "After conversion to exact BCD (BigDecimal)",
        new BigDecimal(Double.toString(d)),
        fq.toBigDecimal());
  }

  @Test
  public void testUseApproximateDoubleWhenAble() {
    Object[][] cases = {
      {1.2345678, 1, MATH_CONTEXT_HALF_EVEN, false},
      {1.2345678, 7, MATH_CONTEXT_HALF_EVEN, false},
      {1.2345678, 12, MATH_CONTEXT_HALF_EVEN, false},
      {1.2345678, 13, MATH_CONTEXT_HALF_EVEN, true},
      {1.235, 1, MATH_CONTEXT_HALF_EVEN, false},
      {1.235, 2, MATH_CONTEXT_HALF_EVEN, true},
      {1.235, 3, MATH_CONTEXT_HALF_EVEN, false},
      {1.000000000000001, 0, MATH_CONTEXT_HALF_EVEN, false},
      {1.000000000000001, 0, MATH_CONTEXT_CEILING, true},
      {1.235, 1, MATH_CONTEXT_CEILING, false},
      {1.235, 2, MATH_CONTEXT_CEILING, false},
      {1.235, 3, MATH_CONTEXT_CEILING, true}
    };

    for (Object[] cas : cases) {
      double d = (Double) cas[0];
      int maxFrac = (Integer) cas[1];
      MathContext mc = (MathContext) cas[2];
      boolean usesExact = (Boolean) cas[3];

      FormatQuantity4 fq = new FormatQuantity4(d);
      assertTrue("Should be using approximate double", !fq.explicitExactDouble);
      fq.roundToMagnitude(-maxFrac, mc);
      assertEquals(
          "Using approximate double after rounding: " + d + " maxFrac=" + maxFrac + " " + mc,
          usesExact,
          fq.explicitExactDouble);
    }
  }

  static void assertDoubleEquals(String message, double d1, double d2) {
    boolean equal = (Math.abs(d1 - d2) < 1e-6) || (Math.abs((d1 - d2) / d1) < 1e-6);
    handleAssert(equal, message, d1, d2, null, false);
  }

  static void assertBigDecimalEquals(String message, String d1, BigDecimal d2) {
    assertBigDecimalEquals(message, new BigDecimal(d1), d2);
  }

  static void assertBigDecimalEquals(String message, BigDecimal d1, BigDecimal d2) {
    boolean equal = d1.compareTo(d2) == 0;
    handleAssert(equal, message, d1, d2, null, false);
  }
}
