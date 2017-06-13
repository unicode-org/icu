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
    return properties.getDecimalSeparatorAlwaysShown()
        || properties.getMaximumFractionDigits() != 0;
  }

  private static class ParameterStruct {

    // Properties
    boolean alwaysShowDecimal;
    int primaryGroupingSize;
    int secondaryGroupingSize;
    int minimumGroupingDigits;

    // Symbols
    String infinityString;
    String nanString;
    String groupingSeparator;
    String decimalSeparator;
    String[] digitStrings;
    int codePointZero;

    void setProperties(DecimalFormatSymbols symbols, IProperties properties) {
      int _primary = properties.getGroupingSize();
      int _secondary = properties.getSecondaryGroupingSize();
      primaryGroupingSize = _primary > 0 ? _primary : _secondary > 0 ? _secondary : 0;
      secondaryGroupingSize = _secondary > 0 ? _secondary : primaryGroupingSize;

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

      // Check to see if we can use code points instead of strings
      int _codePointZero = symbols.getCodePointZero();
      if (_codePointZero != -1) {
        // Fast Path (9-25% faster than slow path when formatting long strings)
        digitStrings = null;
        codePointZero = _codePointZero;
      } else {
        // Slow Path
        digitStrings = symbols.getDigitStrings(); // makes a copy
        codePointZero = -1;
      }
    }
  }

  private static class TransientStruct {
    FormatQuantity input;
    NumberStringBuilder string;
    int index;
    ParameterStruct params;
  }

  private final ParameterStruct params;

  public PositiveDecimalFormat(DecimalFormatSymbols symbols, IProperties properties) {
    params = new ParameterStruct();
    params.setProperties(symbols, properties);
  }

  //    private static void apply(
  //        FormatQuantity input,
  //        NumberStringBuilder string,
  //        int startIndex,
  //        DecimalFormatSymbols symbols,
  //        IProperties properties) {
  //
  //    }

  private static final ThreadLocal<TransientStruct> threadLocalTransientStruct =
      new ThreadLocal<TransientStruct>() {
        @Override
        protected TransientStruct initialValue() {
          return new TransientStruct();
        }
      };

  private static final TransientStruct staticTransientStruct = new TransientStruct();

  @Override
  public int target(FormatQuantity input, NumberStringBuilder string, int startIndex) {
    //    TransientStruct trans = staticTransientStruct;
    TransientStruct trans = threadLocalTransientStruct.get();
    trans.input = input;
    trans.string = string;
    trans.index = startIndex;
    trans.params = params;
    target(trans);
    return trans.index - startIndex;
  }

  private static void target(TransientStruct trans) {
    if (trans.input.isInfinite()) {
      trans.index +=
          trans.string.insert(trans.index, trans.params.infinityString, NumberFormat.Field.INTEGER);

    } else if (trans.input.isNaN()) {
      trans.index +=
          trans.string.insert(trans.index, trans.params.nanString, NumberFormat.Field.INTEGER);

    } else {
      // Add the integer digits
      trans.index += addIntegerDigits(trans);

      // Add the decimal point
      if (trans.input.getLowerDisplayMagnitude() < 0 || trans.params.alwaysShowDecimal) {
        trans.index +=
            trans.string.insert(
                trans.index, trans.params.decimalSeparator, NumberFormat.Field.DECIMAL_SEPARATOR);
      }

      // Add the fraction digits
      trans.index += addFractionDigits(trans);
    }
  }

  private static int addIntegerDigits(TransientStruct trans) {
    int length = 0;
    int integerCount = trans.input.getUpperDisplayMagnitude() + 1;
    for (int i = 0; i < integerCount; i++) {
      // Add grouping separator
      if (trans.params.primaryGroupingSize > 0
          && i == trans.params.primaryGroupingSize
          && integerCount - i >= trans.params.minimumGroupingDigits) {
        length +=
            trans.string.insert(
                trans.index, trans.params.groupingSeparator, NumberFormat.Field.GROUPING_SEPARATOR);
      } else if (trans.params.secondaryGroupingSize > 0
          && i > trans.params.primaryGroupingSize
          && (i - trans.params.primaryGroupingSize) % trans.params.secondaryGroupingSize == 0) {
        length +=
            trans.string.insert(
                trans.index, trans.params.groupingSeparator, NumberFormat.Field.GROUPING_SEPARATOR);
      }

      // Get and append the next digit value
      byte nextDigit = trans.input.getDigit(i);
      length += addDigit(nextDigit, trans.index, NumberFormat.Field.INTEGER, trans);
    }
    return length;
  }

  private static int addFractionDigits(TransientStruct trans) {
    int length = 0;
    int fractionCount = -trans.input.getLowerDisplayMagnitude();
    for (int i = 0; i < fractionCount; i++) {
      // Get and append the next digit value
      byte nextDigit = trans.input.getDigit(-i - 1);
      length += addDigit(nextDigit, trans.index + length, NumberFormat.Field.FRACTION, trans);
    }
    return length;
  }

  private static int addDigit(byte digit, int index, Field field, TransientStruct trans) {
    if (trans.params.codePointZero != -1) {
      return trans.string.insertCodePoint(index, trans.params.codePointZero + digit, field);
    } else {
      return trans.string.insert(index, trans.params.digitStrings[digit], field);
    }
  }

  @Override
  public void export(Properties properties) {
    // For backwards compatibility, export 0 as secondary grouping if primary and secondary are the same
    int effectiveSecondaryGroupingSize =
        params.secondaryGroupingSize == params.primaryGroupingSize
            ? 0
            : params.secondaryGroupingSize;

    properties.setDecimalSeparatorAlwaysShown(params.alwaysShowDecimal);
    properties.setGroupingSize(params.primaryGroupingSize);
    properties.setSecondaryGroupingSize(effectiveSecondaryGroupingSize);
    properties.setMinimumGroupingDigits(params.minimumGroupingDigits);
  }
}
