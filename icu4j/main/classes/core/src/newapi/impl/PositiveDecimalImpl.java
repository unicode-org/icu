// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package newapi.impl;
// License & terms of use: http://www.unicode.org/copyright.html#License

import com.ibm.icu.impl.number.Format;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.NumberFormat;

import newapi.NumberFormatter.DecimalMarkDisplay;

public class PositiveDecimalImpl implements Format.TargetFormat {

  @Override
  public int target(FormatQuantity input, NumberStringBuilder string, int startIndex) {
    // FIXME
    throw new UnsupportedOperationException();
  }

  /**
   * @param micros
   * @param fq
   * @param output
   * @return
   */
  public static int apply(MicroProps micros, FormatQuantity input, NumberStringBuilder string) {
    int length = 0;
    if (input.isInfinite()) {
      length += string.insert(length, micros.symbols.getInfinity(), NumberFormat.Field.INTEGER);

    } else if (input.isNaN()) {
      length += string.insert(length, micros.symbols.getNaN(), NumberFormat.Field.INTEGER);

    } else {
      // Add the integer digits
      length += addIntegerDigits(micros, input, string);

      // Add the decimal point
      if (input.getLowerDisplayMagnitude() < 0
          || micros.decimal == DecimalMarkDisplay.ALWAYS_SHOWN) {
        length +=
            string.insert(
                length,
                micros.useCurrency
                    ? micros.symbols.getMonetaryDecimalSeparatorString()
                    : micros.symbols.getDecimalSeparatorString(),
                NumberFormat.Field.DECIMAL_SEPARATOR);
      }

      // Add the fraction digits
      length += addFractionDigits(micros, input, string);
    }

    return length;
  }

  private static int addIntegerDigits(
      MicroProps micros, FormatQuantity input, NumberStringBuilder string) {
    int length = 0;
    int integerCount = input.getUpperDisplayMagnitude() + 1;
    for (int i = 0; i < integerCount; i++) {
      // Add grouping separator
      if (micros.grouping.groupAtPosition(i, input)) {
        length +=
            string.insert(
                0,
                micros.useCurrency
                    ? micros.symbols.getMonetaryGroupingSeparatorString()
                    : micros.symbols.getGroupingSeparatorString(),
                NumberFormat.Field.GROUPING_SEPARATOR);
      }

      // Get and append the next digit value
      byte nextDigit = input.getDigit(i);
      if (micros.symbols.getCodePointZero() != -1) {
        length +=
            string.insertCodePoint(
                0, micros.symbols.getCodePointZero() + nextDigit, NumberFormat.Field.INTEGER);
      } else {
        length +=
            string.insert(
                0, micros.symbols.getDigitStringsLocal()[nextDigit], NumberFormat.Field.INTEGER);
      }
    }
    return length;
  }

  private static int addFractionDigits(
      MicroProps micros, FormatQuantity input, NumberStringBuilder string) {
    int length = 0;
    int fractionCount = -input.getLowerDisplayMagnitude();
    for (int i = 0; i < fractionCount; i++) {
      // Get and append the next digit value
      byte nextDigit = input.getDigit(-i - 1);
      if (micros.symbols.getCodePointZero() != -1) {
        length +=
            string.appendCodePoint(
                micros.symbols.getCodePointZero() + nextDigit, NumberFormat.Field.FRACTION);
      } else {
        length +=
            string.append(
                micros.symbols.getDigitStringsLocal()[nextDigit], NumberFormat.Field.FRACTION);
      }
    }
    return length;
  }

  @Override
  public void export(Properties properties) {
    throw new UnsupportedOperationException();
  }
}
