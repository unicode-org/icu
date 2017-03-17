// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number.formatters;

import com.ibm.icu.impl.number.Format;
import com.ibm.icu.impl.number.FormatQuantity;
import com.ibm.icu.impl.number.NumberStringBuilder;
import com.ibm.icu.impl.number.Properties;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.NumberFormat.Field;

public class PositiveDecimalFormat implements Format.TargetFormat {

  public static interface IProperties extends CurrencyFormat.IProperties {

    static int DEFAULT_GROUPING_SIZE = -1;

    /** @see #setGroupingSize */
    public int getGroupingSize();

    /**
     * Sets the number of digits between grouping separators. For example, the <em>en-US</em> locale
     * uses a grouping size of 3, so the number 1234567 would be formatted as "1,234,567". For
     * locales whose grouping sizes vary with magnitude, see {@link #setSecondaryGroupingSize(int)}.
     *
     * @param groupingSize The primary grouping size.
     * @return The property bag, for chaining.
     */
    public IProperties setGroupingSize(int groupingSize);

    static int DEFAULT_SECONDARY_GROUPING_SIZE = -1;

    /** @see #setSecondaryGroupingSize */
    public int getSecondaryGroupingSize();

    /**
     * Sets the number of digits between grouping separators higher than the least-significant
     * grouping separator. For example, the locale <em>hi</em> uses a primary grouping size of 3 and
     * a secondary grouping size of 2, so the number 1234567 would be formatted as "12,34,567".
     *
     * <p>The two levels of grouping separators can be specified in the pattern string. For example,
     * the <em>hi</em> locale's default decimal format pattern is "#,##,##0.###".
     *
     * @param secondaryGroupingSize The secondary grouping size.
     * @return The property bag, for chaining.
     */
    public IProperties setSecondaryGroupingSize(int secondaryGroupingSize);

    static boolean DEFAULT_DECIMAL_SEPARATOR_ALWAYS_SHOWN = false;

    /** @see #setDecimalSeparatorAlwaysShown */
    public boolean getDecimalSeparatorAlwaysShown();

    /**
     * Sets whether to always show the decimal point, even if the number doesn't require one. For
     * example, if always show decimal is true, the number 123 would be formatted as "123." in
     * locale <em>en-US</em>.
     *
     * @param decimalSeparatorAlwaysShown Whether to show the decimal point when it is optional.
     * @return The property bag, for chaining.
     */
    public IProperties setDecimalSeparatorAlwaysShown(boolean decimalSeparatorAlwaysShown);

    static int DEFAULT_MINIMUM_GROUPING_DIGITS = 1;

    /** @see #setMinimumGroupingDigits */
    public int getMinimumGroupingDigits();

    /**
     * Sets the minimum number of digits required to be beyond the first grouping separator in order
     * to enable grouping. For example, if the minimum grouping digits is 2, then 1234 would be
     * formatted as "1234" but 12345 would be formatted as "12,345" in <em>en-US</em>. Note that
     * 1234567 would still be formatted as "1,234,567", not "1234,567".
     *
     * @param minimumGroupingDigits How many digits must appear before a grouping separator before
     *     enabling grouping.
     * @return The property bag, for chaining.
     */
    public IProperties setMinimumGroupingDigits(int minimumGroupingDigits);
  }

  public static boolean useGrouping(IProperties properties) {
    return properties.getGroupingSize() != IProperties.DEFAULT_GROUPING_SIZE
        || properties.getSecondaryGroupingSize() != IProperties.DEFAULT_SECONDARY_GROUPING_SIZE;
  }

  public static boolean allowsDecimalPoint(IProperties properties) {
    return properties.getDecimalSeparatorAlwaysShown() || properties.getMaximumFractionDigits() != 0;
  }

  // Properties
  private final boolean alwaysShowDecimal;
  private final int groupingSize;
  private final int secondaryGroupingSize;
  private final int minimumGroupingDigits;

  // Symbols
  private final String infinityString;
  private final String nanString;
  private final String groupingSeparator;
  private final String decimalSeparator;
  private final String[] digitStrings;
  private final int codePointZero;

  public PositiveDecimalFormat(DecimalFormatSymbols symbols, IProperties properties) {
    groupingSize =
        (properties.getGroupingSize() < 0)
            ? properties.getSecondaryGroupingSize()
            : properties.getGroupingSize();
    secondaryGroupingSize =
        (properties.getSecondaryGroupingSize() < 0)
            ? properties.getGroupingSize()
            : properties.getSecondaryGroupingSize();

    minimumGroupingDigits = properties.getMinimumGroupingDigits();
    alwaysShowDecimal = properties.getDecimalSeparatorAlwaysShown();
    infinityString = symbols.getInfinity();
    nanString = symbols.getNaN();

    if (CurrencyFormat.useCurrency(properties)) {
      groupingSeparator = symbols.getMonetaryGroupingSeparatorString();
      decimalSeparator = symbols.getMonetaryDecimalSeparatorString();
    } else {
      groupingSeparator = symbols.getGroupingSeparatorString();
      decimalSeparator = symbols.getDecimalSeparatorString();
    }

    // Check to see if we can use code points instead of strings (~15% format performance boost)
    int _codePointZero = -1;
    String[] _digitStrings = symbols.getDigitStringsLocal();
    for (int i = 0; i < _digitStrings.length; i++) {
      int cp = Character.codePointAt(_digitStrings[i], 0);
      int cc = Character.charCount(cp);
      if (cc != _digitStrings[i].length()) {
        _codePointZero = -1;
        break;
      } else if (i == 0) {
        _codePointZero = cp;
      } else if (cp != _codePointZero + i) {
        _codePointZero = -1;
        break;
      }
    }
    if (_codePointZero != -1) {
      digitStrings = null;
      codePointZero = _codePointZero;
    } else {
      digitStrings = symbols.getDigitStrings(); // makes a copy
      codePointZero = -1;
    }
  }

  @Override
  public int target(FormatQuantity input, NumberStringBuilder string, int startIndex) {
    int length = 0;

    if (input.isInfinite()) {
      length += string.insert(startIndex, infinityString, NumberFormat.Field.INTEGER);

    } else if (input.isNaN()) {
      length += string.insert(startIndex, nanString, NumberFormat.Field.INTEGER);

    } else {
      // Add the integer digits
      length += addIntegerDigits(input, string, startIndex);

      // Add the decimal point
      if (input.getLowerDisplayMagnitude() < 0 || alwaysShowDecimal) {
        length += string.insert(startIndex + length, decimalSeparator, NumberFormat.Field.DECIMAL_SEPARATOR);
      }

      // Add the fraction digits
      length += addFractionDigits(input, string, startIndex + length);
    }

    return length;
  }

  private int addIntegerDigits(FormatQuantity input, NumberStringBuilder string, int startIndex) {
    int length = 0;
    int integerCount = input.getUpperDisplayMagnitude() + 1;
    for (int i = 0; i < integerCount; i++) {
      // Add grouping separator
      if (groupingSize > 0 && i == groupingSize && integerCount - i >= minimumGroupingDigits) {
        length += string.insert(startIndex, groupingSeparator, NumberFormat.Field.GROUPING_SEPARATOR);
      } else if (secondaryGroupingSize > 0
          && i > groupingSize
          && (i - groupingSize) % secondaryGroupingSize == 0) {
        length += string.insert(startIndex, groupingSeparator, NumberFormat.Field.GROUPING_SEPARATOR);
      }

      // Get and append the next digit value
      byte nextDigit = input.getDigit(i);
      length += addDigit(nextDigit, string, startIndex, NumberFormat.Field.INTEGER);
    }

    return length;
  }

  private int addFractionDigits(FormatQuantity input, NumberStringBuilder string, int index) {
    int length = 0;
    int fractionCount = -input.getLowerDisplayMagnitude();
    for (int i = 0; i < fractionCount; i++) {
      // Get and append the next digit value
      byte nextDigit = input.getDigit(-i - 1);
      length += addDigit(nextDigit, string, index + length, NumberFormat.Field.FRACTION);
    }
    return length;
  }

  private int addDigit(byte digit, NumberStringBuilder outputString, int index, Field field) {
    if (codePointZero != -1) {
      return outputString.insertCodePoint(index, codePointZero + digit, field);
    } else {
      return outputString.insert(index, digitStrings[digit], field);
    }
  }

  @Override
  public void export(Properties properties) {
    properties.setDecimalSeparatorAlwaysShown(alwaysShowDecimal);
    properties.setGroupingSize(groupingSize);
    properties.setSecondaryGroupingSize(secondaryGroupingSize);
    properties.setMinimumGroupingDigits(minimumGroupingDigits);
  }
}
