// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.util.Locale;

import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

public final class NumberFormatter {

  private static final UnlocalizedNumberFormatter BASE = new UnlocalizedNumberFormatter();

  // This could possibly be combined into MeasureFormat.FormatWidth
  public static enum CurrencyDisplay {
    SYMBOL, // ¤
    ISO_4217, // ¤¤
    DISPLAY_NAME, // ¤¤¤
    SYMBOL_NARROW, // ¤¤¤¤
    HIDDEN, // uses currency rounding and formatting but omits the currency symbol
    // TODO: For hidden, what to do if currency symbol appears in the middle, as in Portugal ?
  }

  public static enum DecimalMarkDisplay {
    AUTO,
    ALWAYS,
  }

  public static enum SignDisplay {
    AUTO,
    ALWAYS,
    NEVER,
  }

  public static UnlocalizedNumberFormatter fromSkeleton(String skeleton) {
    // FIXME
    throw new UnsupportedOperationException();
  }

  public static UnlocalizedNumberFormatter with() {
    return BASE;
  }

  public static LocalizedNumberFormatter withLocale(Locale locale) {
    return BASE.locale(locale);
  }

  public static LocalizedNumberFormatter withLocale(ULocale locale) {
    return BASE.locale(locale);
  }
}
