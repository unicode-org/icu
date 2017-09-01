// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;

import java.math.RoundingMode;

import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberingSystem;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.MeasureUnit;
import com.ibm.icu.util.NoUnit;
import com.ibm.icu.util.ULocale;

import newapi.Grouper;
import newapi.Notation;
import newapi.NumberFormatter;
import newapi.NumberFormatter.DecimalMarkDisplay;
import newapi.NumberFormatter.SignDisplay;
import newapi.NumberFormatter.UnitWidth;
import newapi.Rounder;
import newapi.UnlocalizedNumberFormatter;

public class demo {
  public static void main(String[] args) {
    System.out.println(NumberingSystem.LATIN.getDescription());
    UnlocalizedNumberFormatter formatter =
        NumberFormatter.with()
            .notation(Notation.compactShort())
            .notation(Notation.scientific().withExponentSignDisplay(SignDisplay.ALWAYS))
            .notation(Notation.engineering().withMinExponentDigits(2))
            .notation(Notation.simple())
            .unit(Currency.getInstance("GBP"))
            .unit(NoUnit.PERCENT)
            .unit(MeasureUnit.CUBIC_METER)
            .unitWidth(UnitWidth.SHORT)
            // .rounding(Rounding.fixedSignificantDigits(3))
//            .rounding(
//                (BigDecimal input) -> {
//                  return input.divide(new BigDecimal("0.02"), 0).multiply(new BigDecimal("0.02"));
//                })
            .rounding(Rounder.fixedFraction(2).withMode(RoundingMode.HALF_UP))
            .rounding(Rounder.integer().withMode(RoundingMode.CEILING))
            .rounding(Rounder.currency(CurrencyUsage.STANDARD))
//            .grouping(
//                (int position, BigDecimal number) -> {
//                  return (position % 3) == 0;
//                })
            .grouping(Grouper.defaults())
            .grouping(Grouper.none())
            .grouping(Grouper.min2())
            // .padding(Padding.codePoints(' ', 8, PadPosition.AFTER_PREFIX))
            .sign(SignDisplay.ALWAYS)
            .decimal(DecimalMarkDisplay.ALWAYS)
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
