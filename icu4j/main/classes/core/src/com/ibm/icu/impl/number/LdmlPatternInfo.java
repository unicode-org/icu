// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import com.ibm.icu.impl.number.formatters.PaddingFormat.PadPosition;

import newapi.impl.AffixPatternProvider;

/** Implements a recursive descent parser for decimal format patterns. */
public class LdmlPatternInfo {

  public static PatternParseResult parse(String patternString) {
    ParserState state = new ParserState(patternString);
    PatternParseResult result = new PatternParseResult(patternString);
    consumePattern(state, result);
    return result;
  }

  /**
   * An internal, intermediate data structure used for storing parse results before they are
   * finalized into a DecimalFormatPattern.Builder.
   */
  public static class PatternParseResult implements AffixPatternProvider {
    public String pattern;
    public LdmlPatternInfo.SubpatternParseResult positive;
    public LdmlPatternInfo.SubpatternParseResult negative;

    private PatternParseResult(String pattern) {
      this.pattern = pattern;
    }

    @Override
    public char charAt(int flags, int index) {
      long endpoints = getEndpoints(flags);
      int left = (int) (endpoints & 0xffffffff);
      int right = (int) (endpoints >>> 32);
      if (index < 0 || index >= right - left) {
        throw new IndexOutOfBoundsException();
      }
      return pattern.charAt(left + index);
    }

    @Override
    public int length(int flags) {
      return getLengthFromEndpoints(getEndpoints(flags));
    }

    public static int getLengthFromEndpoints(long endpoints) {
      int left = (int) (endpoints & 0xffffffff);
      int right = (int) (endpoints >>> 32);
      return right - left;
    }

    public String getString(int flags) {
      long endpoints = getEndpoints(flags);
      int left = (int) (endpoints & 0xffffffff);
      int right = (int) (endpoints >>> 32);
      if (left == right) {
        return "";
      }
      return pattern.substring(left, right);
    }

    private long getEndpoints(int flags) {
      boolean prefix = (flags & Flags.PREFIX) != 0;
      boolean isNegative = (flags & Flags.NEGATIVE_SUBPATTERN) != 0;
      boolean padding = (flags & Flags.PADDING) != 0;
      if (isNegative && padding) {
        return negative.paddingEndpoints;
      } else if (padding) {
        return positive.paddingEndpoints;
      } else if (prefix && isNegative) {
        return negative.prefixEndpoints;
      } else if (prefix) {
        return positive.prefixEndpoints;
      } else if (isNegative) {
        return negative.suffixEndpoints;
      } else {
        return positive.suffixEndpoints;
      }
    }

    @Override
    public boolean positiveHasPlusSign() {
      return positive.hasPlusSign;
    }

    @Override
    public boolean hasNegativeSubpattern() {
      return negative != null;
    }

    @Override
    public boolean negativeHasMinusSign() {
      return negative.hasMinusSign;
    }

    @Override
    public boolean hasCurrencySign() {
      return positive.hasCurrencySign || (negative != null && negative.hasCurrencySign);
    }

    @Override
    public boolean containsSymbolType(int type) {
      return AffixPatternUtils.containsType(pattern, type);
    }
  }

  public static class SubpatternParseResult {
    public long groupingSizes = 0x0000ffffffff0000L;
    public int minimumIntegerDigits = 0;
    public int totalIntegerDigits = 0;
    public int minimumFractionDigits = 0;
    public int maximumFractionDigits = 0;
    public int minimumSignificantDigits = 0;
    public int maximumSignificantDigits = 0;
    public boolean hasDecimal = false;
    public int paddingWidth = 0;
    public PadPosition paddingLocation = null;
    public FormatQuantity4 rounding = null;
    public boolean exponentShowPlusSign = false;
    public int exponentDigits = 0;
    public boolean hasPercentSign = false;
    public boolean hasPerMilleSign = false;
    public boolean hasCurrencySign = false;
    public boolean hasMinusSign = false;
    public boolean hasPlusSign = false;

    public long prefixEndpoints = 0;
    public long suffixEndpoints = 0;
    public long paddingEndpoints = 0;
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

  private static void consumePattern(
      LdmlPatternInfo.ParserState state, LdmlPatternInfo.PatternParseResult result) {
    // pattern := subpattern (';' subpattern)?
    result.positive = new SubpatternParseResult();
    consumeSubpattern(state, result.positive);
    if (state.peek() == ';') {
      state.next(); // consume the ';'
      // Don't consume the negative subpattern if it is empty (trailing ';')
      if (state.peek() != -1) {
        result.negative = new SubpatternParseResult();
        consumeSubpattern(state, result.negative);
      }
    }
    if (state.peek() != -1) {
      throw state.toParseException("Found unquoted special character");
    }
  }

  private static void consumeSubpattern(
      LdmlPatternInfo.ParserState state, LdmlPatternInfo.SubpatternParseResult result) {
    // subpattern := literals? number exponent? literals?
    consumePadding(state, result, PadPosition.BEFORE_PREFIX);
    result.prefixEndpoints = consumeAffix(state, result);
    consumePadding(state, result, PadPosition.AFTER_PREFIX);
    consumeFormat(state, result);
    consumeExponent(state, result);
    consumePadding(state, result, PadPosition.BEFORE_SUFFIX);
    result.suffixEndpoints = consumeAffix(state, result);
    consumePadding(state, result, PadPosition.AFTER_SUFFIX);
  }

  private static void consumePadding(
      LdmlPatternInfo.ParserState state,
      LdmlPatternInfo.SubpatternParseResult result,
      PadPosition paddingLocation) {
    if (state.peek() != '*') {
      return;
    }
    result.paddingLocation = paddingLocation;
    state.next(); // consume the '*'
    result.paddingEndpoints |= state.offset;
    consumeLiteral(state);
    result.paddingEndpoints |= ((long) state.offset) << 32;
  }

  private static long consumeAffix(
      LdmlPatternInfo.ParserState state, LdmlPatternInfo.SubpatternParseResult result) {
    // literals := { literal }
    long endpoints = state.offset;
    outer:
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
          break outer;

        case '%':
          result.hasPercentSign = true;
          break;

        case '‰':
          result.hasPerMilleSign = true;
          break;

        case '¤':
          result.hasCurrencySign = true;
          break;

        case '-':
          result.hasMinusSign = true;
          break;

        case '+':
          result.hasPlusSign = true;
          break;
      }
      consumeLiteral(state);
    }
    endpoints |= ((long) state.offset) << 32;
    return endpoints;
  }

  private static void consumeLiteral(LdmlPatternInfo.ParserState state) {
    if (state.peek() == -1) {
      throw state.toParseException("Expected unquoted literal but found EOL");
    } else if (state.peek() == '\'') {
      state.next(); // consume the starting quote
      while (state.peek() != '\'') {
        if (state.peek() == -1) {
          throw state.toParseException("Expected quoted literal but found EOL");
        } else {
          state.next(); // consume a quoted character
        }
      }
      state.next(); // consume the ending quote
    } else {
      // consume a non-quoted literal character
      state.next();
    }
  }

  private static void consumeFormat(
      LdmlPatternInfo.ParserState state, LdmlPatternInfo.SubpatternParseResult result) {
    consumeIntegerFormat(state, result);
    if (state.peek() == '.') {
      state.next(); // consume the decimal point
      result.hasDecimal = true;
      result.paddingWidth += 1;
      consumeFractionFormat(state, result);
    }
  }

  private static void consumeIntegerFormat(
      LdmlPatternInfo.ParserState state, LdmlPatternInfo.SubpatternParseResult result) {
    boolean seenSignificantDigitMarker = false;
    boolean seenDigit = false;

    outer:
    while (true) {
      switch (state.peek()) {
        case ',':
          result.paddingWidth += 1;
          result.groupingSizes <<= 16;
          break;

        case '#':
          if (seenDigit) throw state.toParseException("# cannot follow 0 before decimal point");
          result.paddingWidth += 1;
          result.groupingSizes += 1;
          result.totalIntegerDigits += (seenSignificantDigitMarker ? 0 : 1);
          // no change to result.minimumIntegerDigits
          // no change to result.minimumSignificantDigits
          result.maximumSignificantDigits += (seenSignificantDigitMarker ? 1 : 0);
          if (result.rounding != null) {
            result.rounding.appendDigit((byte) 0, 0, true);
          }
          break;

        case '@':
          seenSignificantDigitMarker = true;
          if (seenDigit) throw state.toParseException("Cannot mix 0 and @");
          result.paddingWidth += 1;
          result.groupingSizes += 1;
          result.totalIntegerDigits += 1;
          // no change to result.minimumIntegerDigits
          result.minimumSignificantDigits += 1;
          result.maximumSignificantDigits += 1;
          if (result.rounding != null) {
            result.rounding.appendDigit((byte) 0, 0, true);
          }
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
          result.groupingSizes += 1;
          result.totalIntegerDigits += 1;
          result.minimumIntegerDigits += 1;
          // no change to result.minimumSignificantDigits
          // no change to result.maximumSignificantDigits
          if (state.peek() != '0' && result.rounding == null) {
            result.rounding = new FormatQuantity4();
          }
          if (result.rounding != null) {
            result.rounding.appendDigit((byte) (state.peek() - '0'), 0, true);
          }
          break;

        default:
          break outer;
      }
      state.next(); // consume the symbol
    }

    // Disallow patterns with a trailing ',' or with two ',' next to each other
    short grouping1 = (short) (result.groupingSizes & 0xffff);
    short grouping2 = (short) ((result.groupingSizes >>> 16) & 0xffff);
    short grouping3 = (short) ((result.groupingSizes >>> 32) & 0xffff);
    if (grouping1 == 0 && grouping2 != -1) {
      throw state.toParseException("Trailing grouping separator is invalid");
    }
    if (grouping2 == 0 && grouping3 != -1) {
      throw state.toParseException("Grouping width of zero is invalid");
    }
  }

  private static void consumeFractionFormat(
      LdmlPatternInfo.ParserState state, LdmlPatternInfo.SubpatternParseResult result) {
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
            if (result.rounding == null) {
              result.rounding = new FormatQuantity4();
            }
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

  private static void consumeExponent(
      LdmlPatternInfo.ParserState state, LdmlPatternInfo.SubpatternParseResult result) {
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
