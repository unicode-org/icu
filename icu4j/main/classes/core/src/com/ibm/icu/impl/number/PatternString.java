// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.math.BigDecimal;

import com.ibm.icu.impl.number.formatters.PaddingFormat;
import com.ibm.icu.impl.number.formatters.PaddingFormat.PadPosition;
import com.ibm.icu.text.DecimalFormatSymbols;

/**
 * Handles parsing and creation of the compact pattern string representation of a decimal format.
 */
public class PatternString {

  /**
   * Parses a pattern string into a new property bag.
   *
   * @param pattern The pattern string, like "#,##0.00"
   * @param ignoreRounding Whether to leave out rounding information (minFrac, maxFrac, and rounding
   *     increment) when parsing the pattern. This may be desirable if a custom rounding mode, such
   *     as CurrencyUsage, is to be used instead. One of {@link #IGNORE_ROUNDING_ALWAYS}, {@link
   *     #IGNORE_ROUNDING_IF_CURRENCY}, or {@link #IGNORE_ROUNDING_NEVER}.
   * @return A property bag object.
   * @throws IllegalArgumentException If there is a syntax error in the pattern string.
   */
  public static Properties parseToProperties(String pattern, int ignoreRounding) {
    Properties properties = new Properties();
    LdmlDecimalPatternParser.parse(pattern, properties, ignoreRounding);
    return properties;
  }

  public static Properties parseToProperties(String pattern) {
    return parseToProperties(pattern, PatternString.IGNORE_ROUNDING_NEVER);
  }

  /**
   * Parses a pattern string into an existing property bag. All properties that can be encoded into
   * a pattern string will be overwritten with either their default value or with the value coming
   * from the pattern string. Properties that cannot be encoded into a pattern string, such as
   * rounding mode, are not modified.
   *
   * @param pattern The pattern string, like "#,##0.00"
   * @param properties The property bag object to overwrite.
   * @param ignoreRounding Whether to leave out rounding information (minFrac, maxFrac, and rounding
   *     increment) when parsing the pattern. This may be desirable if a custom rounding mode, such
   *     as CurrencyUsage, is to be used instead. One of {@link #IGNORE_ROUNDING_ALWAYS}, {@link
   *     #IGNORE_ROUNDING_IF_CURRENCY}, or {@link #IGNORE_ROUNDING_NEVER}.
   * @throws IllegalArgumentException If there was a syntax error in the pattern string.
   */
  public static void parseToExistingProperties(
      String pattern, Properties properties, int ignoreRounding) {
    LdmlDecimalPatternParser.parse(pattern, properties, ignoreRounding);
  }

  public static void parseToExistingProperties(String pattern, Properties properties) {
    parseToExistingProperties(pattern, properties, PatternString.IGNORE_ROUNDING_NEVER);
  }

  /**
   * Creates a pattern string from a property bag.
   *
   * <p>Since pattern strings support only a subset of the functionality available in a property
   * bag, a new property bag created from the string returned by this function may not be the same
   * as the original property bag.
   *
   * @param properties The property bag to serialize.
   * @return A pattern string approximately serializing the property bag.
   */
  public static String propertiesToString(Properties properties) {
    StringBuilder sb = new StringBuilder();

    // Convenience references
    // The Math.min() calls prevent DoS
    int dosMax = 100;
    int groupingSize = Math.min(properties.getSecondaryGroupingSize(), dosMax);
    int firstGroupingSize = Math.min(properties.getGroupingSize(), dosMax);
    int paddingWidth = Math.min(properties.getFormatWidth(), dosMax);
    PadPosition paddingLocation = properties.getPadPosition();
    String paddingString = properties.getPadString();
    int minInt = Math.max(Math.min(properties.getMinimumIntegerDigits(), dosMax), 0);
    int maxInt = Math.min(properties.getMaximumIntegerDigits(), dosMax);
    int minFrac = Math.max(Math.min(properties.getMinimumFractionDigits(), dosMax), 0);
    int maxFrac = Math.min(properties.getMaximumFractionDigits(), dosMax);
    int minSig = Math.min(properties.getMinimumSignificantDigits(), dosMax);
    int maxSig = Math.min(properties.getMaximumSignificantDigits(), dosMax);
    boolean alwaysShowDecimal = properties.getDecimalSeparatorAlwaysShown();
    int exponentDigits = Math.min(properties.getMinimumExponentDigits(), dosMax);
    boolean exponentShowPlusSign = properties.getExponentSignAlwaysShown();
    String pp = properties.getPositivePrefix();
    String ppp = properties.getPositivePrefixPattern();
    String ps = properties.getPositiveSuffix();
    String psp = properties.getPositiveSuffixPattern();
    String np = properties.getNegativePrefix();
    String npp = properties.getNegativePrefixPattern();
    String ns = properties.getNegativeSuffix();
    String nsp = properties.getNegativeSuffixPattern();

    // Prefixes
    if (ppp != null) sb.append(ppp);
    AffixPatternUtils.escape(pp, sb);
    int afterPrefixPos = sb.length();

    // Figure out the grouping sizes.
    int grouping1, grouping2, grouping;
    if (groupingSize != Math.min(dosMax, Properties.DEFAULT_SECONDARY_GROUPING_SIZE)
        && firstGroupingSize != Math.min(dosMax, Properties.DEFAULT_GROUPING_SIZE)
        && groupingSize != firstGroupingSize) {
      grouping = groupingSize;
      grouping1 = groupingSize;
      grouping2 = firstGroupingSize;
    } else if (groupingSize != Math.min(dosMax, Properties.DEFAULT_SECONDARY_GROUPING_SIZE)) {
      grouping = groupingSize;
      grouping1 = 0;
      grouping2 = groupingSize;
    } else if (firstGroupingSize != Math.min(dosMax, Properties.DEFAULT_GROUPING_SIZE)) {
      grouping = groupingSize;
      grouping1 = 0;
      grouping2 = firstGroupingSize;
    } else {
      grouping = 0;
      grouping1 = 0;
      grouping2 = 0;
    }
    int groupingLength = grouping1 + grouping2 + 1;

    // Figure out the digits we need to put in the pattern.
    BigDecimal roundingInterval = properties.getRoundingIncrement();
    StringBuilder digitsString = new StringBuilder();
    int digitsStringScale = 0;
    if (maxSig != Math.min(dosMax, Properties.DEFAULT_MAXIMUM_SIGNIFICANT_DIGITS)) {
      // Significant Digits.
      while (digitsString.length() < minSig) {
        digitsString.append('@');
      }
      while (digitsString.length() < maxSig) {
        digitsString.append('#');
      }
    } else if (roundingInterval != Properties.DEFAULT_ROUNDING_INCREMENT) {
      // Rounding Interval.
      digitsStringScale = -roundingInterval.scale();
      // TODO: Check for DoS here?
      String str = roundingInterval.scaleByPowerOfTen(roundingInterval.scale()).toPlainString();
      if (str.charAt(0) == '\'') {
        // TODO: Unsupported operation exception or fail silently?
        digitsString.append(str, 1, str.length());
      } else {
        digitsString.append(str);
      }
    }
    while (digitsString.length() + digitsStringScale < minInt) {
      digitsString.insert(0, '0');
    }
    while (-digitsStringScale < minFrac) {
      digitsString.append('0');
      digitsStringScale--;
    }

    // Write the digits to the string builder
    int m0 = Math.max(groupingLength, digitsString.length() + digitsStringScale);
    m0 = (maxInt != dosMax) ? Math.max(maxInt, m0) - 1 : m0 - 1;
    int mN = (maxFrac != dosMax) ? Math.min(-maxFrac, digitsStringScale) : digitsStringScale;
    for (int magnitude = m0; magnitude >= mN; magnitude--) {
      int di = digitsString.length() + digitsStringScale - magnitude - 1;
      if (di < 0 || di >= digitsString.length()) {
        sb.append('#');
      } else {
        sb.append(digitsString.charAt(di));
      }
      if (magnitude > grouping2 && grouping > 0 && (magnitude - grouping2) % grouping == 0) {
        sb.append(',');
      } else if (magnitude > 0 && magnitude == grouping2) {
        sb.append(',');
      } else if (magnitude == 0 && (alwaysShowDecimal || mN < 0)) {
        sb.append('.');
      }
    }

    // Exponential notation
    if (exponentDigits != Math.min(dosMax, Properties.DEFAULT_MINIMUM_EXPONENT_DIGITS)) {
      sb.append('E');
      if (exponentShowPlusSign) {
        sb.append('+');
      }
      for (int i = 0; i < exponentDigits; i++) {
        sb.append('0');
      }
    }

    // Suffixes
    int beforeSuffixPos = sb.length();
    if (psp != null) sb.append(psp);
    AffixPatternUtils.escape(ps, sb);

    // Resolve Padding
    if (paddingWidth != Properties.DEFAULT_FORMAT_WIDTH) {
      while (paddingWidth - sb.length() > 0) {
        sb.insert(afterPrefixPos, '#');
        beforeSuffixPos++;
      }
      int addedLength;
      switch (paddingLocation) {
        case BEFORE_PREFIX:
          addedLength = escapePaddingString(paddingString, sb, 0);
          sb.insert(0, '*');
          afterPrefixPos += addedLength + 1;
          beforeSuffixPos += addedLength + 1;
          break;
        case AFTER_PREFIX:
          addedLength = escapePaddingString(paddingString, sb, afterPrefixPos);
          sb.insert(afterPrefixPos, '*');
          afterPrefixPos += addedLength + 1;
          beforeSuffixPos += addedLength + 1;
          break;
        case BEFORE_SUFFIX:
          escapePaddingString(paddingString, sb, beforeSuffixPos);
          sb.insert(beforeSuffixPos, '*');
          break;
        case AFTER_SUFFIX:
          sb.append('*');
          escapePaddingString(paddingString, sb, sb.length());
          break;
      }
    }

    // Negative affixes
    // Ignore if the negative prefix pattern is "-" and the negative suffix is empty
    if (np != null
        || ns != null
        || (npp == null && nsp != null)
        || (npp != null && (npp.length() != 1 || npp.charAt(0) != '-' || nsp.length() != 0))) {
      sb.append(';');
      if (npp != null) sb.append(npp);
      AffixPatternUtils.escape(np, sb);
      // Copy the positive digit format into the negative.
      // This is optional; the pattern is the same as if '#' were appended here instead.
      sb.append(sb, afterPrefixPos, beforeSuffixPos);
      if (nsp != null) sb.append(nsp);
      AffixPatternUtils.escape(ns, sb);
    }

    return sb.toString();
  }

  /** @return The number of chars inserted. */
  private static int escapePaddingString(CharSequence input, StringBuilder output, int startIndex) {
    if (input == null || input.length() == 0) input = PaddingFormat.FALLBACK_PADDING_STRING;
    int startLength = output.length();
    if (input.length() == 1) {
      if (input.equals("'")) {
        output.insert(startIndex, "''");
      } else {
        output.insert(startIndex, input);
      }
    } else {
      output.insert(startIndex, '\'');
      int offset = 1;
      for (int i = 0; i < input.length(); i++) {
        // it's okay to deal in chars here because the quote mark is the only interesting thing.
        char ch = input.charAt(i);
        if (ch == '\'') {
          output.insert(startIndex + offset, "''");
          offset += 2;
        } else {
          output.insert(startIndex + offset, ch);
          offset += 1;
        }
      }
      output.insert(startIndex + offset, '\'');
    }
    return output.length() - startLength;
  }

  /**
   * Converts a pattern between standard notation and localized notation. Localized notation means
   * that instead of using generic placeholders in the pattern, you use the corresponding
   * locale-specific characters instead. For example, in locale <em>fr-FR</em>, the period in the
   * pattern "0.000" means "decimal" in standard notation (as it does in every other locale), but it
   * means "grouping" in localized notation.
   *
   * @param input The pattern to convert.
   * @param symbols The symbols corresponding to the localized pattern.
   * @param toLocalized true to convert from standard to localized notation; false to convert from
   *     localized to standard notation.
   * @return The pattern expressed in the other notation.
   * @deprecated ICU 59 This method is provided for backwards compatibility and should not be used
   *     in any new code.
   */
  @Deprecated
  public static String convertLocalized(
      CharSequence input, DecimalFormatSymbols symbols, boolean toLocalized) {
    if (input == null) return null;

    /// This is not the prettiest function in the world, but it gets the job done. ///

    // Construct a table of code points to be converted between localized and standard.
    int[][] table = new int[6][2];
    int standIdx = toLocalized ? 0 : 1;
    int localIdx = toLocalized ? 1 : 0;
    table[0][standIdx] = '%';
    table[0][localIdx] = symbols.getPercent();
    table[1][standIdx] = '‰';
    table[1][localIdx] = symbols.getPerMill();
    table[2][standIdx] = '.';
    table[2][localIdx] = symbols.getDecimalSeparator();
    table[3][standIdx] = ',';
    table[3][localIdx] = symbols.getGroupingSeparator();
    table[4][standIdx] = '-';
    table[4][localIdx] = symbols.getMinusSign();
    table[5][standIdx] = '+';
    table[5][localIdx] = symbols.getPlusSign();

    // Special case: localIdx characters are NOT allowed to be quotes, like in de_CH.
    // Use '’' instead.
    for (int i = 0; i < table.length; i++) {
      if (table[i][localIdx] == '\'') {
        table[i][localIdx] = '’';
      }
    }

    // Iterate through the string and convert
    int offset = 0;
    int state = 0;
    StringBuilder result = new StringBuilder();
    for (; offset < input.length(); ) {
      int cp = Character.codePointAt(input, offset);
      int cpToAppend = cp;

      if (state == 1 || state == 3 || state == 4) {
        // Inside user-specified quote
        if (cp == '\'') {
          if (state == 1) {
            state = 0;
          } else if (state == 3) {
            state = 2;
            cpToAppend = -1;
          } else {
            state = 2;
          }
        }
      } else {
        // Base state or inside special character quote
        if (cp == '\'') {
          if (state == 2 && offset + 1 < input.length()) {
            int nextCp = Character.codePointAt(input, offset + 1);
            if (nextCp == '\'') {
              // escaped quote
              state = 4;
            } else {
              // begin user-specified quote sequence
              // we are already in a quote sequence, so omit the opening quote
              state = 3;
              cpToAppend = -1;
            }
          } else {
            state = 1;
          }
        } else {
          boolean needsSpecialQuote = false;
          for (int i = 0; i < table.length; i++) {
            if (table[i][0] == cp) {
              cpToAppend = table[i][1];
              needsSpecialQuote = false; // in case an earlier translation triggered it
              break;
            } else if (table[i][1] == cp) {
              needsSpecialQuote = true;
            }
          }
          if (state == 0 && needsSpecialQuote) {
            state = 2;
            result.appendCodePoint('\'');
          } else if (state == 2 && !needsSpecialQuote) {
            state = 0;
            result.appendCodePoint('\'');
          }
        }
      }
      if (cpToAppend != -1) {
        result.appendCodePoint(cpToAppend);
      }
      offset += Character.charCount(cp);
    }
    if (state == 2) {
      result.appendCodePoint('\'');
    }
    return result.toString();
  }

  public static final int IGNORE_ROUNDING_NEVER = 0;
  public static final int IGNORE_ROUNDING_IF_CURRENCY = 1;
  public static final int IGNORE_ROUNDING_ALWAYS = 2;

  /** Implements a recursive descent parser for decimal format patterns. */
  static class LdmlDecimalPatternParser {

    /**
     * An internal, intermediate data structure used for storing parse results before they are
     * finalized into a DecimalFormatPattern.Builder.
     */
    private static class PatternParseResult {
      SubpatternParseResult positive = new SubpatternParseResult();
      SubpatternParseResult negative = null;

      /** Finalizes the temporary data stored in the PatternParseResult to the Builder. */
      void saveToProperties(Properties properties, int _ignoreRounding) {
        // Translate from PatternState to Properties.
        // Note that most data from "negative" is ignored per the specification of DecimalFormat.

        boolean ignoreRounding;
        if (_ignoreRounding == IGNORE_ROUNDING_NEVER) {
          ignoreRounding = false;
        } else if (_ignoreRounding == IGNORE_ROUNDING_IF_CURRENCY) {
          ignoreRounding = positive.hasCurrencySign;
        } else {
          assert _ignoreRounding == IGNORE_ROUNDING_ALWAYS;
          ignoreRounding = true;
        }

        // Grouping settings
        if (positive.groupingSizes[1] != -1) {
          properties.setGroupingSize(positive.groupingSizes[0]);
        } else {
          properties.setGroupingSize(Properties.DEFAULT_GROUPING_SIZE);
        }
        if (positive.groupingSizes[2] != -1) {
          properties.setSecondaryGroupingSize(positive.groupingSizes[1]);
        } else {
          properties.setSecondaryGroupingSize(Properties.DEFAULT_SECONDARY_GROUPING_SIZE);
        }

        // For backwards compatibility, require that the pattern emit at least one min digit.
        int minInt, minFrac;
        if (positive.totalIntegerDigits == 0 && positive.maximumFractionDigits > 0) {
          // patterns like ".##"
          minInt = 0;
          minFrac = Math.max(1, positive.minimumFractionDigits);
        } else if (positive.minimumIntegerDigits == 0 && positive.minimumFractionDigits == 0) {
          // patterns like "#.##"
          minInt = 1;
          minFrac = 0;
        } else {
          minInt = positive.minimumIntegerDigits;
          minFrac = positive.minimumFractionDigits;
        }

        // Rounding settings
        // Don't set basic rounding when there is a currency sign; defer to CurrencyUsage
        if (positive.minimumSignificantDigits > 0) {
          properties.setMinimumFractionDigits(Properties.DEFAULT_MINIMUM_FRACTION_DIGITS);
          properties.setMaximumFractionDigits(Properties.DEFAULT_MAXIMUM_FRACTION_DIGITS);
          properties.setRoundingIncrement(Properties.DEFAULT_ROUNDING_INCREMENT);
          properties.setMinimumSignificantDigits(positive.minimumSignificantDigits);
          properties.setMaximumSignificantDigits(positive.maximumSignificantDigits);
        } else if (!positive.rounding.isZero()) {
          if (!ignoreRounding) {
            properties.setMinimumFractionDigits(minFrac);
            properties.setMaximumFractionDigits(positive.maximumFractionDigits);
            properties.setRoundingIncrement(positive.rounding.toBigDecimal());
          } else {
            properties.setMinimumFractionDigits(Properties.DEFAULT_MINIMUM_FRACTION_DIGITS);
            properties.setMaximumFractionDigits(Properties.DEFAULT_MAXIMUM_FRACTION_DIGITS);
            properties.setRoundingIncrement(Properties.DEFAULT_ROUNDING_INCREMENT);
          }
          properties.setMinimumSignificantDigits(Properties.DEFAULT_MINIMUM_SIGNIFICANT_DIGITS);
          properties.setMaximumSignificantDigits(Properties.DEFAULT_MAXIMUM_SIGNIFICANT_DIGITS);
        } else {
          if (!ignoreRounding) {
            properties.setMinimumFractionDigits(minFrac);
            properties.setMaximumFractionDigits(positive.maximumFractionDigits);
            properties.setRoundingIncrement(Properties.DEFAULT_ROUNDING_INCREMENT);
          } else {
            properties.setMinimumFractionDigits(Properties.DEFAULT_MINIMUM_FRACTION_DIGITS);
            properties.setMaximumFractionDigits(Properties.DEFAULT_MAXIMUM_FRACTION_DIGITS);
            properties.setRoundingIncrement(Properties.DEFAULT_ROUNDING_INCREMENT);
          }
          properties.setMinimumSignificantDigits(Properties.DEFAULT_MINIMUM_SIGNIFICANT_DIGITS);
          properties.setMaximumSignificantDigits(Properties.DEFAULT_MAXIMUM_SIGNIFICANT_DIGITS);
        }

        // If the pattern ends with a '.' then force the decimal point.
        if (positive.hasDecimal && positive.maximumFractionDigits == 0) {
          properties.setDecimalSeparatorAlwaysShown(true);
        } else {
          properties.setDecimalSeparatorAlwaysShown(false);
        }

        // Scientific notation settings
        if (positive.exponentDigits > 0) {
          properties.setExponentSignAlwaysShown(positive.exponentShowPlusSign);
          properties.setMinimumExponentDigits(positive.exponentDigits);
          if (positive.minimumSignificantDigits == 0) {
            // patterns without '@' can define max integer digits, used for engineering notation
            properties.setMinimumIntegerDigits(positive.minimumIntegerDigits);
            properties.setMaximumIntegerDigits(positive.totalIntegerDigits);
          } else {
            // patterns with '@' cannot define max integer digits
            properties.setMinimumIntegerDigits(1);
            properties.setMaximumIntegerDigits(Properties.DEFAULT_MAXIMUM_INTEGER_DIGITS);
          }
        } else {
          properties.setExponentSignAlwaysShown(Properties.DEFAULT_EXPONENT_SIGN_ALWAYS_SHOWN);
          properties.setMinimumExponentDigits(Properties.DEFAULT_MINIMUM_EXPONENT_DIGITS);
          properties.setMinimumIntegerDigits(minInt);
          properties.setMaximumIntegerDigits(Properties.DEFAULT_MAXIMUM_INTEGER_DIGITS);
        }

        // Padding settings
        if (positive.padding.length() > 0) {
          // The width of the positive prefix and suffix templates are included in the padding
          int paddingWidth =
              positive.paddingWidth
                  + AffixPatternUtils.unescapedLength(positive.prefix)
                  + AffixPatternUtils.unescapedLength(positive.suffix);
          properties.setFormatWidth(paddingWidth);
          if (positive.padding.length() == 1) {
            properties.setPadString(positive.padding.toString());
          } else if (positive.padding.length() == 2) {
            if (positive.padding.charAt(0) == '\'') {
              properties.setPadString("'");
            } else {
              properties.setPadString(positive.padding.toString());
            }
          } else {
            properties.setPadString(
                positive.padding.subSequence(1, positive.padding.length() - 1).toString());
          }
          assert positive.paddingLocation != null;
          properties.setPadPosition(positive.paddingLocation);
        } else {
          properties.setFormatWidth(Properties.DEFAULT_FORMAT_WIDTH);
          properties.setPadString(Properties.DEFAULT_PAD_STRING);
          properties.setPadPosition(Properties.DEFAULT_PAD_POSITION);
        }

        // Set the affixes
        // Always call the setter, even if the prefixes are empty, especially in the case of the
        // negative prefix pattern, to prevent default values from overriding the pattern.
        properties.setPositivePrefixPattern(positive.prefix.toString());
        properties.setPositiveSuffixPattern(positive.suffix.toString());
        if (negative != null) {
          properties.setNegativePrefixPattern(negative.prefix.toString());
          properties.setNegativeSuffixPattern(negative.suffix.toString());
        } else {
          properties.setNegativePrefixPattern(null);
          properties.setNegativeSuffixPattern(null);
        }

        // Set the magnitude multiplier
        if (positive.hasPercentSign) {
          properties.setMagnitudeMultiplier(2);
        } else if (positive.hasPerMilleSign) {
          properties.setMagnitudeMultiplier(3);
        } else {
          properties.setMagnitudeMultiplier(Properties.DEFAULT_MAGNITUDE_MULTIPLIER);
        }
      }
    }

    private static class SubpatternParseResult {
      int[] groupingSizes = new int[] {0, -1, -1};
      int minimumIntegerDigits = 0;
      int totalIntegerDigits = 0;
      int minimumFractionDigits = 0;
      int maximumFractionDigits = 0;
      int minimumSignificantDigits = 0;
      int maximumSignificantDigits = 0;
      boolean hasDecimal = false;
      int paddingWidth = 0;
      PadPosition paddingLocation = null;
      FormatQuantity4 rounding = new FormatQuantity4();
      boolean exponentShowPlusSign = false;
      int exponentDigits = 0;
      boolean hasPercentSign = false;
      boolean hasPerMilleSign = false;
      boolean hasCurrencySign = false;

      StringBuilder padding = new StringBuilder();
      StringBuilder prefix = new StringBuilder();
      StringBuilder suffix = new StringBuilder();
    }

    /** An internal class used for tracking the cursor during parsing of a pattern string. */
    private static class ParserState {
      final String pattern;
      int offset;

      ParserState(String pattern) {
        this.pattern = pattern;
        this.offset = 0;
      }

      int peek() {
        if (offset == pattern.length()) {
          return -1;
        } else {
          return pattern.codePointAt(offset);
        }
      }

      int next() {
        int codePoint = peek();
        offset += Character.charCount(codePoint);
        return codePoint;
      }

      IllegalArgumentException toParseException(String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("Malformed pattern for ICU DecimalFormat: \"");
        sb.append(pattern);
        sb.append("\": ");
        sb.append(message);
        sb.append(" at position ");
        sb.append(offset);
        return new IllegalArgumentException(sb.toString());
      }
    }

    static void parse(String pattern, Properties properties, int ignoreRounding) {
      if (pattern == null || pattern.length() == 0) {
        // Backwards compatibility requires that we reset to the default values.
        // TODO: Only overwrite the properties that "saveToProperties" normally touches?
        properties.clear();
        return;
      }

      // TODO: Use whitespace characters from PatternProps
      // TODO: Use thread locals here.
      ParserState state = new ParserState(pattern);
      PatternParseResult result = new PatternParseResult();
      consumePattern(state, result);
      result.saveToProperties(properties, ignoreRounding);
    }

    private static void consumePattern(ParserState state, PatternParseResult result) {
      // pattern := subpattern (';' subpattern)?
      consumeSubpattern(state, result.positive);
      if (state.peek() == ';') {
        state.next(); // consume the ';'
        result.negative = new SubpatternParseResult();
        consumeSubpattern(state, result.negative);
      }
      if (state.peek() != -1) {
        throw state.toParseException("Found unquoted special character");
      }
    }

    private static void consumeSubpattern(ParserState state, SubpatternParseResult result) {
      // subpattern := literals? number exponent? literals?
      consumePadding(state, result, PadPosition.BEFORE_PREFIX);
      consumeAffix(state, result, result.prefix);
      consumePadding(state, result, PadPosition.AFTER_PREFIX);
      consumeFormat(state, result);
      consumeExponent(state, result);
      consumePadding(state, result, PadPosition.BEFORE_SUFFIX);
      consumeAffix(state, result, result.suffix);
      consumePadding(state, result, PadPosition.AFTER_SUFFIX);
    }

    private static void consumePadding(
        ParserState state, SubpatternParseResult result, PadPosition paddingLocation) {
      if (state.peek() != '*') {
        return;
      }
      result.paddingLocation = paddingLocation;
      state.next(); // consume the '*'
      consumeLiteral(state, result.padding);
    }

    private static void consumeAffix(
        ParserState state, SubpatternParseResult result, StringBuilder destination) {
      // literals := { literal }
      while (true) {
        switch (state.peek()) {
          case '#':
          case '@':
          case ';':
          case '*':
          case '.':
          case ',':
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
          case -1:
            // Characters that cannot appear unquoted in a literal
            return;

          case '%':
            result.hasPercentSign = true;
            break;

          case '‰':
            result.hasPerMilleSign = true;
            break;

          case '¤':
            result.hasCurrencySign = true;
            break;
        }
        consumeLiteral(state, destination);
      }
    }

    private static void consumeLiteral(ParserState state, StringBuilder destination) {
      if (state.peek() == -1) {
        throw state.toParseException("Expected unquoted literal but found EOL");
      } else if (state.peek() == '\'') {
        destination.appendCodePoint(state.next()); // consume the starting quote
        while (state.peek() != '\'') {
          if (state.peek() == -1) {
            throw state.toParseException("Expected quoted literal but found EOL");
          } else {
            destination.appendCodePoint(state.next()); // consume a quoted character
          }
        }
        destination.appendCodePoint(state.next()); // consume the ending quote
      } else {
        // consume a non-quoted literal character
        destination.appendCodePoint(state.next());
      }
    }

    private static void consumeFormat(ParserState state, SubpatternParseResult result) {
      consumeIntegerFormat(state, result);
      if (state.peek() == '.') {
        state.next(); // consume the decimal point
        result.hasDecimal = true;
        result.paddingWidth += 1;
        consumeFractionFormat(state, result);
      }
    }

    private static void consumeIntegerFormat(ParserState state, SubpatternParseResult result) {
      boolean seenSignificantDigitMarker = false;
      boolean seenDigit = false;

      while (true) {
        switch (state.peek()) {
          case ',':
            result.paddingWidth += 1;
            result.groupingSizes[2] = result.groupingSizes[1];
            result.groupingSizes[1] = result.groupingSizes[0];
            result.groupingSizes[0] = 0;
            break;

          case '#':
            if (seenDigit) throw state.toParseException("# cannot follow 0 before decimal point");
            result.paddingWidth += 1;
            result.groupingSizes[0] += 1;
            result.totalIntegerDigits += (seenSignificantDigitMarker ? 0 : 1);
            // no change to result.minimumIntegerDigits
            // no change to result.minimumSignificantDigits
            result.maximumSignificantDigits += (seenSignificantDigitMarker ? 1 : 0);
            result.rounding.appendDigit((byte) 0, 0, true);
            break;

          case '@':
            seenSignificantDigitMarker = true;
            if (seenDigit) throw state.toParseException("Cannot mix 0 and @");
            result.paddingWidth += 1;
            result.groupingSizes[0] += 1;
            result.totalIntegerDigits += 1;
            // no change to result.minimumIntegerDigits
            result.minimumSignificantDigits += 1;
            result.maximumSignificantDigits += 1;
            result.rounding.appendDigit((byte) 0, 0, true);
            break;

          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            seenDigit = true;
            if (seenSignificantDigitMarker) throw state.toParseException("Cannot mix @ and 0");
            // TODO: Crash here if we've seen the significant digit marker? See NumberFormatTestCases.txt
            result.paddingWidth += 1;
            result.groupingSizes[0] += 1;
            result.totalIntegerDigits += 1;
            result.minimumIntegerDigits += 1;
            // no change to result.minimumSignificantDigits
            // no change to result.maximumSignificantDigits
            result.rounding.appendDigit((byte) (state.peek() - '0'), 0, true);
            break;

          default:
            return;
        }
        state.next(); // consume the symbol
      }
    }

    private static void consumeFractionFormat(ParserState state, SubpatternParseResult result) {
      int zeroCounter = 0;
      boolean seenHash = false;
      while (true) {
        switch (state.peek()) {
          case '#':
            seenHash = true;
            result.paddingWidth += 1;
            // no change to result.minimumFractionDigits
            result.maximumFractionDigits += 1;
            zeroCounter++;
            break;

          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            if (seenHash) throw state.toParseException("0 cannot follow # after decimal point");
            result.paddingWidth += 1;
            result.minimumFractionDigits += 1;
            result.maximumFractionDigits += 1;
            if (state.peek() == '0') {
              zeroCounter++;
            } else {
              result.rounding.appendDigit((byte) (state.peek() - '0'), zeroCounter, false);
              zeroCounter = 0;
            }
            break;

          default:
            return;
        }
        state.next(); // consume the symbol
      }
    }

    private static void consumeExponent(ParserState state, SubpatternParseResult result) {
      if (state.peek() != 'E') {
        return;
      }
      state.next(); // consume the E
      result.paddingWidth++;
      if (state.peek() == '+') {
        state.next(); // consume the +
        result.exponentShowPlusSign = true;
        result.paddingWidth++;
      }
      while (state.peek() == '0') {
        state.next(); // consume the 0
        result.exponentDigits += 1;
        result.paddingWidth++;
      }
    }
  }
}
