// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.ibm.icu.impl.number.AffixPatternUtils;
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
      {"¤'-'%", true, 3, "$-٪\u061C"}
    };

    // ar_SA has an interesting percent sign and various Arabic letter marks
    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new ULocale("ar_SA"));
    NumberStringBuilder sb = new NumberStringBuilder();

    for (Object[] cas : cases) {
      String input = (String) cas[0];
      boolean curr = (Boolean) cas[1];
      int length = (Integer) cas[2];
      String output = (String) cas[3];

      assertEquals(
          "Currency on <" + input + ">", curr, AffixPatternUtils.hasCurrencySymbols(input));
      assertEquals("Length on <" + input + ">", length, AffixPatternUtils.unescapedLength(input));

      sb.clear();
      AffixPatternUtils.unescape(input, symbols, "$", "XXX", "long name", "−", sb);
      assertEquals("Output on <" + input + ">", output, sb.toString());
    }
  }

  @Test
  public void testInvalid() {
    String[] invalidExamples = {"'", "x'", "'x", "'x''", "''x'"};
    DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new ULocale("en_US"));
    NumberStringBuilder sb = new NumberStringBuilder();

    for (String str : invalidExamples) {
      try {
        AffixPatternUtils.hasCurrencySymbols(str);
        fail("No exception was thrown on an invalid string");
      } catch (IllegalArgumentException e) {
        // OK
      }
      try {
        AffixPatternUtils.unescapedLength(str);
        fail("No exception was thrown on an invalid string");
      } catch (IllegalArgumentException e) {
        // OK
      }
      try {
        AffixPatternUtils.unescape(str, symbols, "$", "XXX", "long name", "−", sb);
        fail("No exception was thrown on an invalid string");
      } catch (IllegalArgumentException e) {
        // OK
      }
    }
  }
}
