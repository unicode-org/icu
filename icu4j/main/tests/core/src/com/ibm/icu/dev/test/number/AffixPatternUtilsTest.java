// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.ibm.icu.impl.number.AffixPatternUtils;
import com.ibm.icu.impl.number.AffixPatternUtils.SymbolProvider;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.ULocale;

public class AffixPatternUtilsTest {

  @Test
  public void testEscape() {
    Object[][] cases = {
      {"", ""},
      {"abc", "abc"},
      {"-", "'-'"},
      {"-!", "'-'!"},
      {"−", "−"},
      {"---", "'---'"},
      {"-%-", "'-%-'"},
      {"'", "''"},
      {"-'", "'-'''"},
      {"-'-", "'-''-'"},
      {"a-'-", "a'-''-'"}
    };

    StringBuilder sb = new StringBuilder();
    for (Object[] cas : cases) {
      String input = (String) cas[0];
      String expected = (String) cas[1];
      sb.setLength(0);
      AffixPatternUtils.escape(input, sb);
      assertEquals(expected, sb.toString());
    }
  }

  @Test
  public void testUnescape() {
    Object[][] cases = {
      {"", false, 0, ""},
      {"abc", false, 3, "abc"},
      {"-", false, 1, "−"},
      {"-!", false, 2, "−!"},
      {"+", false, 1, "\u061C+"},
      {"+!", false, 2, "\u061C+!"},
      {"‰", false, 1, "؉"},
      {"‰!", false, 2, "؉!"},
      {"-x", false, 2, "−x"},
      {"'-'x", false, 2, "-x"},
      {"'--''-'-x", false, 6, "--'-−x"},
      {"''", false, 1, "'"},
      {"''''", false, 2, "''"},
      {"''''''", false, 3, "'''"},
      {"''x''", false, 3, "'x'"},
      {"¤", true, 1, "$"},
      {"¤¤", true, 2, "XXX"},
      {"¤¤¤", true, 3, "long name"},
      {"¤¤¤¤", true, 4, "\uFFFD"},
      {"¤¤¤¤¤", true, 5, "\uFFFD"},
      {"¤¤¤¤¤¤", true, 6, "\uFFFD"},
      {"¤¤¤a¤¤¤¤", true, 8, "long namea\uFFFD"},
      {"a¤¤¤¤b¤¤¤¤¤c", true, 12, "a\uFFFDb\uFFFDc"},
      {"¤!", true, 2, "$!"},
      {"¤¤!", true, 3, "XXX!"},
      {"¤¤¤!", true, 4, "long name!"},
      {"-¤¤", true, 3, "−XXX"},
      {"¤¤-", true, 3, "XXX−"},
      {"'¤'", false, 1, "¤"},
      {"%", false, 1, "٪\u061C"},
      {"'%'", false, 1, "%"},
      {"¤'-'%", true, 3, "$-٪\u061C"},
      {"#0#@#*#;#", false, 9, "#0#@#*#;#"}
    };

    for (Object[] cas : cases) {
      String input = (String) cas[0];
      boolean curr = (Boolean) cas[1];
      int length = (Integer) cas[2];
      String output = (String) cas[3];

      assertEquals(
          "Currency on <" + input + ">", curr, AffixPatternUtils.hasCurrencySymbols(input));
      assertEquals("Length on <" + input + ">", length, AffixPatternUtils.estimateLength(input));

      String actual = unescapeWithDefaults(input);
      assertEquals("Output on <" + input + ">", output, actual);
    }
  }

  @Test
  public void testContainsReplaceType() {
    Object[][] cases = {
      {"", false, ""},
      {"-", true, "+"},
      {"-a", true, "+a"},
      {"a-", true, "a+"},
      {"a-b", true, "a+b"},
      {"--", true, "++"},
      {"x", false, "x"}
    };

    for (Object[] cas : cases) {
      String input = (String) cas[0];
      boolean hasMinusSign = (Boolean) cas[1];
      String output = (String) cas[2];

      assertEquals(
          "Contains on input " + input,
          hasMinusSign,
          AffixPatternUtils.containsType(input, AffixPatternUtils.TYPE_MINUS_SIGN));
      assertEquals(
          "Replace on input" + input,
          output,
          AffixPatternUtils.replaceType(input, AffixPatternUtils.TYPE_MINUS_SIGN, '+'));
    }
  }

  @Test
  public void testInvalid() {
    String[] invalidExamples = {"'", "x'", "'x", "'x''", "''x'"};

    for (String str : invalidExamples) {
      try {
        AffixPatternUtils.hasCurrencySymbols(str);
        fail("No exception was thrown on an invalid string");
      } catch (IllegalArgumentException e) {
        // OK
      }
      try {
        AffixPatternUtils.estimateLength(str);
        fail("No exception was thrown on an invalid string");
      } catch (IllegalArgumentException e) {
        // OK
      }
      try {
        unescapeWithDefaults(str);
        fail("No exception was thrown on an invalid string");
      } catch (IllegalArgumentException e) {
        // OK
      }
    }
  }

  @Test
  public void testUnescapeWithSymbolProvider() {
    String[][] cases = {
      {"", ""},
      {"-", "1"},
      {"'-'", "-"},
      {"- + % ‰ ¤ ¤¤ ¤¤¤ ¤¤¤¤ ¤¤¤¤¤", "1 2 3 4 5 6 7 8 9"},
      {"'¤¤¤¤¤¤'", "¤¤¤¤¤¤"},
      {"¤¤¤¤¤¤", "\uFFFD"}
    };

    SymbolProvider provider =
        new SymbolProvider() {
          @Override
          public CharSequence getSymbol(int type) {
            return Integer.toString(Math.abs(type));
          }
        };

    NumberStringBuilder sb = new NumberStringBuilder();
    for (String[] cas : cases) {
      String input = cas[0];
      String expected = cas[1];
      sb.clear();
      AffixPatternUtils.unescape(input, sb, 0, provider);
      assertEquals("With symbol provider on <" + input + ">", expected, sb.toString());
    }

    // Test insertion position
    sb.clear();
    sb.append("abcdefg", null);
    AffixPatternUtils.unescape("-+%", sb, 4, provider);
    assertEquals("Symbol provider into middle", "abcd123efg", sb.toString());
  }

  private static final SymbolProvider DEFAULT_SYMBOL_PROVIDER =
      new SymbolProvider() {
        // ar_SA has an interesting percent sign and various Arabic letter marks
        private final DecimalFormatSymbols SYMBOLS =
            DecimalFormatSymbols.getInstance(new ULocale("ar_SA"));

        @Override
        public CharSequence getSymbol(int type) {
          switch (type) {
            case AffixPatternUtils.TYPE_MINUS_SIGN:
              return "−";
            case AffixPatternUtils.TYPE_PLUS_SIGN:
              return SYMBOLS.getPlusSignString();
            case AffixPatternUtils.TYPE_PERCENT:
              return SYMBOLS.getPercentString();
            case AffixPatternUtils.TYPE_PERMILLE:
              return SYMBOLS.getPerMillString();
            case AffixPatternUtils.TYPE_CURRENCY_SINGLE:
              return "$";
            case AffixPatternUtils.TYPE_CURRENCY_DOUBLE:
              return "XXX";
            case AffixPatternUtils.TYPE_CURRENCY_TRIPLE:
              return "long name";
            case AffixPatternUtils.TYPE_CURRENCY_QUAD:
              return "\uFFFD";
            case AffixPatternUtils.TYPE_CURRENCY_QUINT:
              // TODO: Add support for narrow currency symbols here.
              return "\uFFFD";
            case AffixPatternUtils.TYPE_CURRENCY_OVERFLOW:
              return "\uFFFD";
            default:
              throw new AssertionError();
          }
        }
      };

  private static String unescapeWithDefaults(String input) {
    NumberStringBuilder nsb = new NumberStringBuilder();
    AffixPatternUtils.unescape(input, nsb, 0, DEFAULT_SYMBOL_PROVIDER);
    return nsb.toString();
  }
}
