// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.dev.test.number;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ibm.icu.impl.number.LdmlPatternInfo;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.SignDisplay;
import newapi.impl.MurkyModifier;

public class MurkyModifierTest {

  @Test
  public void basic() {
    MurkyModifier murky = new MurkyModifier(false);
    murky.setPatternInfo(LdmlPatternInfo.parse("a0b"));
    murky.setPatternAttributes(SignDisplay.AUTO, false);
    murky.setSymbols(
        DecimalFormatSymbols.getInstance(ULocale.ENGLISH),
        Currency.getInstance("USD"),
        FormatWidth.SHORT,
        null);
    murky.setNumberProperties(false, null);
    assertEquals("a", murky.getPrefix());
    assertEquals("b", murky.getSuffix());
    murky.setPatternAttributes(SignDisplay.ALWAYS_SHOWN, false);
    assertEquals("+a", murky.getPrefix());
    assertEquals("b", murky.getSuffix());
    murky.setNumberProperties(true, null);
    assertEquals("-a", murky.getPrefix());
    assertEquals("b", murky.getSuffix());
    murky.setPatternAttributes(SignDisplay.NEVER_SHOWN, false);
    assertEquals("a", murky.getPrefix());
    assertEquals("b", murky.getSuffix());

    murky.setPatternInfo(LdmlPatternInfo.parse("a0b;c-0d"));
    murky.setPatternAttributes(SignDisplay.AUTO, false);
    murky.setNumberProperties(false, null);
    assertEquals("a", murky.getPrefix());
    assertEquals("b", murky.getSuffix());
    murky.setPatternAttributes(SignDisplay.ALWAYS_SHOWN, false);
    assertEquals("c+", murky.getPrefix());
    assertEquals("d", murky.getSuffix());
    murky.setNumberProperties(true, null);
    assertEquals("c-", murky.getPrefix());
    assertEquals("d", murky.getSuffix());
    murky.setPatternAttributes(SignDisplay.NEVER_SHOWN, false);
    assertEquals("c-", murky.getPrefix()); // TODO: What should this behavior be?
    assertEquals("d", murky.getSuffix());
  }
}
