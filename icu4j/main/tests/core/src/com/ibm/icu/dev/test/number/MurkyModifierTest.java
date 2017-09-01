// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.icu.impl.number.PatternParser;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

import newapi.MutablePatternModifier;
import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnitWidth;

public class MurkyModifierTest {

  @Test
  public void basic() {
    MutablePatternModifier murky = new MutablePatternModifier(false);
    murky.setPatternInfo(PatternParser.parse("a0b"));
    murky.setPatternAttributes(SignDisplay.AUTO, false);
    murky.setSymbols(
        DecimalFormatSymbols.getInstance(ULocale.ENGLISH),
        Currency.getInstance("USD"),
        UnitWidth.SHORT,
        null);
    murky.setNumberProperties(false, null);
    assertEquals("a", getPrefix(murky));
    assertEquals("b", getSuffix(murky));
    murky.setPatternAttributes(SignDisplay.ALWAYS, false);
    assertEquals("+a", getPrefix(murky));
    assertEquals("b", getSuffix(murky));
    murky.setNumberProperties(true, null);
    assertEquals("-a", getPrefix(murky));
    assertEquals("b", getSuffix(murky));
    murky.setPatternAttributes(SignDisplay.NEVER, false);
    assertEquals("a", getPrefix(murky));
    assertEquals("b", getSuffix(murky));

    murky.setPatternInfo(PatternParser.parse("a0b;c-0d"));
    murky.setPatternAttributes(SignDisplay.AUTO, false);
    murky.setNumberProperties(false, null);
    assertEquals("a", getPrefix(murky));
    assertEquals("b", getSuffix(murky));
    murky.setPatternAttributes(SignDisplay.ALWAYS, false);
    assertEquals("c+", getPrefix(murky));
    assertEquals("d", getSuffix(murky));
    murky.setNumberProperties(true, null);
    assertEquals("c-", getPrefix(murky));
    assertEquals("d", getSuffix(murky));
    murky.setPatternAttributes(SignDisplay.NEVER, false);
    assertEquals("c-", getPrefix(murky)); // TODO: What should this behavior be?
    assertEquals("d", getSuffix(murky));
  }

  private static String getPrefix(MutablePatternModifier murky) {
      NumberStringBuilder nsb = new NumberStringBuilder();
      murky.apply(nsb, 0, 0);
      return nsb.subSequence(0, murky.getPrefixLength()).toString();
  }

  private static String getSuffix(MutablePatternModifier murky) {
      NumberStringBuilder nsb = new NumberStringBuilder();
      murky.apply(nsb, 0, 0);
      return nsb.subSequence(murky.getPrefixLength(), nsb.length()).toString();
  }
}
