// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.ibm.icu.impl.number.PatternString;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.ULocale;

/** @author sffc */
public class PatternStringTest {

  @Test
  public void testLocalized() {
    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(ULocale.ENGLISH);
    symbols.setDecimalSeparatorString("a");
    symbols.setPercentString("b");
    symbols.setMinusSignString(".");
    symbols.setPlusSignString("'");

    String standard = "+-abcb''a''#,##0.0%'a%'";
    String localized = "’.'ab'c'b''a'''#,##0a0b'a%'";
    String toStandard = "+-'ab'c'b''a'''#,##0.0%'a%'";

    assertEquals(localized, PatternString.convertLocalized(standard, symbols, true));
    assertEquals(toStandard, PatternString.convertLocalized(localized, symbols, false));
  }

  @Test
  public void testToPatternSimple() {
    String[][] cases = {
      {"#", "0"},
      {"0", "0"},
      {"#0", "0"},
      {"###", "0"},
      {"0.##", "0.##"},
      {"0.00", "0.00"},
      {"0.00#", "0.00#"},
      {"#E0", "#E0"},
      {"0E0", "0E0"},
      {"#00E00", "#00E00"},
      {"#,##0", "#,##0"},
      {"#,##0E0", "#,##0E0"},
      {"#;#", "0;0"},
      {"#;-#", "0"}, // ignore a negative prefix pattern of '-' since that is the default
      {"**##0", "**##0"},
      {"*'x'##0", "*x##0"},
      {"a''b0", "a''b0"},
      {"*''##0", "*''##0"},
      {"*📺##0", "*'📺'##0"},
      {"*'நி'##0", "*'நி'##0"},
    };

    for (String[] cas : cases) {
      String input = cas[0];
      String output = cas[1];

      Properties properties = PatternString.parseToProperties(input);
      String actual = PatternString.propertiesToString(properties);
      assertEquals(
          "Failed on input pattern '" + input + "', properties " + properties, output, actual);
    }
  }

  @Test
  public void testToPatternWithProperties() {
    Object[][] cases = {
      {new Properties().setPositivePrefix("abc"), "abc#"},
      {new Properties().setPositiveSuffix("abc"), "#abc"},
      {new Properties().setPositivePrefixPattern("abc"), "abc#"},
      {new Properties().setPositiveSuffixPattern("abc"), "#abc"},
      {new Properties().setNegativePrefix("abc"), "#;abc#"},
      {new Properties().setNegativeSuffix("abc"), "#;#abc"},
      {new Properties().setNegativePrefixPattern("abc"), "#;abc#"},
      {new Properties().setNegativeSuffixPattern("abc"), "#;#abc"},
      {new Properties().setPositivePrefix("+"), "'+'#"},
      {new Properties().setPositivePrefixPattern("+"), "+#"},
      {new Properties().setPositivePrefix("+'"), "'+'''#"},
      {new Properties().setPositivePrefix("'+"), "'''+'#"},
      {new Properties().setPositivePrefix("'"), "''#"},
      {new Properties().setPositivePrefixPattern("+''"), "+''#"},
    };

    for (Object[] cas : cases) {
      Properties input = (Properties) cas[0];
      String output = (String) cas[1];

      String actual = PatternString.propertiesToString(input);
      assertEquals("Failed on input properties " + input, output, actual);
    }
  }

  @Test
  public void testExceptionOnInvalid() {
    String[] invalidPatterns = {"#.#.#", "0#", "0#.", ".#0", "0#.#0", "@0", "0@"};

    for (String pattern : invalidPatterns) {
      try {
        PatternString.parseToProperties(pattern);
        fail("Didn't throw IllegalArgumentException when parsing pattern: " + pattern);
      } catch (IllegalArgumentException e) {
      }
    }
  }

  @Test
  public void testBug13117() {
    Properties expected = PatternString.parseToProperties("0");
    Properties actual = PatternString.parseToProperties("0;");
    assertEquals("Should not consume negative subpattern", expected, actual);
  }
}
