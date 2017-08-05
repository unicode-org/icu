// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi;

import java.math.RoundingMode;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.MeasureFormat.FormatWidth;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.Dimensionless;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.ULocale;

import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.Grouping;
import newapi.NumberFormatter.Notation;
import newapi.NumberFormatter.Rounding;
import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnlocalizedNumberFormatter;

public class demo {
  public static void main(String[] args) {
    System.out.println(NumberingSystem.LATIN.getDescription());
    UnlocalizedNumberFormatter formatter =
        NumberFormatter.with()
            .notation(Notation.COMPACT_SHORT)
            .notation(Notation.SCIENTIFIC.withExponentSignDisplay(SignDisplay.ALWAYS_SHOWN))
            .notation(Notation.ENGINEERING.withMinExponentDigits(2))
            .notation(Notation.SIMPLE)
            .unit(Currency.getInstance("GBP"))
            .unit(Dimensionless.PERCENT)
            .unit(MeasureUnit.CUBIC_METER)
            .unitWidth(FormatWidth.SHORT)
            // .rounding(Rounding.fixedSignificantDigits(3))
//            .rounding(
//                (BigDecimal input) -> {
//                  return input.divide(new BigDecimal("0.02"), 0).multiply(new BigDecimal("0.02"));
//                })
            .rounding(Rounding.fixedFraction(2).withMode(RoundingMode.HALF_UP))
            .rounding(Rounding.INTEGER.withMode(RoundingMode.CEILING))
            .rounding(Rounding.currency(CurrencyUsage.STANDARD))
//            .grouping(
//                (int position, BigDecimal number) -> {
//                  return (position % 3) == 0;
//                })
            .grouping(Grouping.DEFAULT)
            .grouping(Grouping.NONE)
            .grouping(Grouping.DEFAULT_MIN_2_DIGITS)
            // .padding(Padding.codePoints(' ', 8, PadPosition.AFTER_PREFIX))
            .sign(SignDisplay.ALWAYS_SHOWN)
            .decimal(DecimalMarkDisplay.ALWAYS_SHOWN)
            .symbols(DecimalFormatSymbols.getInstance(new ULocale("fr@digits=ascii")))
            .symbols(NumberingSystem.getInstanceByName("arab"))
            .symbols(NumberingSystem.LATIN);
    System.out.println(formatter.toSkeleton());
    System.out.println(formatter.locale(ULocale.ENGLISH).format(0.98381).toString());
    //            .locale(Locale.ENGLISH)
    //            .format(123.45)
    //            .toString();
  }
}
