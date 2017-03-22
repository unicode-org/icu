// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.impl.number;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.icu.impl.StandardPlural;
import com.ibm.icu.impl.TextTrieMap;
import com.ibm.icu.impl.number.formatters.BigDecimalMultiplier;
import com.ibm.icu.impl.number.formatters.CurrencyFormat;
import com.ibm.icu.impl.number.formatters.MagnitudeMultiplier;
import com.ibm.icu.impl.number.formatters.PaddingFormat;
import com.ibm.icu.impl.number.formatters.PositiveDecimalFormat;
import com.ibm.icu.impl.number.formatters.PositiveNegativeAffixFormat;
import com.ibm.icu.impl.number.formatters.ScientificFormat;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CurrencyPluralInfo;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyStringInfo;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;

/**
 * A parser designed to convert an arbitrary human-generated string to its best representation as a
 * number: a long, a BigInteger, or a BigDecimal.
 *
 * <p>The parser may traverse multiple parse paths in the same strings if there is ambiguity. For
 * example, the string "12,345.67" has two main interpretations: it could be "12.345" in a locale
 * that uses '.' as the grouping separator, or it could be "12345.67" in a locale that uses ',' as
 * the grouping separator. Since the second option has a longer parse path (consumes more of the
 * input string), the parser will accept the second option.
 */
public class Parse {

  /** Controls the set of rules for parsing a string. */
  public static enum ParseMode {
    /**
     * Lenient mode should be used if you want to accept malformed user input. It will use
     * heuristics to attempt to parse through typographical errors in the string.
     */
    LENIENT,

    /**
     * Strict mode should be used if you want to require that the input is well-formed. More
     * specifically, it differs from lenient mode in the following ways:
     *
     * <ul>
     *   <li>Grouping widths must match the grouping settings. For example, "12,3,45" will fail if
     *       the grouping width is 3, as in the pattern "#,##0".
     *   <li>The string must contain a complete prefix and suffix. For example, if the pattern is
     *       "{#};(#)", then "{123}" or "(123)" would match, but "{123", "123}", and "123" would all
     *       fail. (The latter strings would be accepted in lenient mode.)
     *   <li>Whitespace may not appear at arbitrary places in the string. In lenient mode,
     *       whitespace is allowed to occur arbitrarily before and after prefixes and exponent
     *       separators.
     *   <li>Leading grouping separators are not allowed, as in ",123".
     *   <li>Minus and plus signs can only appear if specified in the pattern. In lenient mode, a
     *       plus or minus sign can always precede a number.
     *   <li>The set of characters that can be interpreted as a decimal or grouping separator is
     *       smaller.
     *   <li><strong>If currency parsing is enabled,</strong> currencies must only appear where
     *       specified in either the current pattern string or in a valid pattern string for the
     *       current locale. For example, if the pattern is "¤0.00", then "$1.23" would match, but
     *       "1.23$" would fail to match.
     * </ul>
     */
    STRICT,

    /**
     * Fast mode should be used in applications that don't require prefixes and suffixes to match.
     *
     * <p>In addition to ignoring prefixes and suffixes, fast mode performs the following
     * optimizations:
     *
     * <ul>
     *   <li>Ignores digit strings from {@link DecimalFormatSymbols} and only uses the code point's
     *       Unicode digit property. If you are not using custom digit strings, this should not
     *       cause a change in behavior.
     *   <li>Instead of traversing multiple possible parse paths, a "greedy" parsing strategy is
     *       used, which might mean that fast mode won't accept strings that lenient or strict mode
     *       would accept. Since prefix and suffix strings are ignored, this is not an issue unless
     *       you are using custom symbols.
     * </ul>
     */
    FAST,
  }

  /** The set of properties required for {@link Parse}. Accepts a {@link Properties} object. */
  public static interface IProperties
      extends PositiveNegativeAffixFormat.IProperties,
          PaddingFormat.IProperties,
          CurrencyFormat.ICurrencyProperties,
          BigDecimalMultiplier.IProperties,
          MagnitudeMultiplier.IProperties,
          PositiveDecimalFormat.IProperties,
          ScientificFormat.IProperties {

    boolean DEFAULT_PARSE_INTEGER_ONLY = false;

    /** @see #setParseIntegerOnly */
    public boolean getParseIntegerOnly();

    /**
     * Whether to ignore the fractional part of numbers. For example, parses "123.4" to "123"
     * instead of "123.4".
     *
     * @param parseIntegerOnly true to parse integers only; false to parse integers with their
     *     fraction parts
     * @return The property bag, for chaining.
     */
    public IProperties setParseIntegerOnly(boolean parseIntegerOnly);

    boolean DEFAULT_PARSE_NO_EXPONENT = false;

    /** @see #setParseNoExponent */
    public boolean getParseNoExponent();

    /**
     * Whether to ignore the exponential part of numbers. For example, parses "123E4" to "123"
     * instead of "1230000".
     *
     * @param parseIgnoreExponent true to ignore exponents; false to parse them.
     * @return The property bag, for chaining.
     */
    public IProperties setParseNoExponent(boolean parseIgnoreExponent);

    boolean DEFAULT_DECIMAL_PATTERN_MATCH_REQUIRED = false;

    /** @see #setDecimalPatternMatchRequired */
    public boolean getDecimalPatternMatchRequired();

    /**
     * Whether to require that the presence of decimal point matches the pattern. If a decimal point
     * is not present, but the pattern contained a decimal point, parse will not succeed: null will
     * be returned from <code>parse()</code>, and an error index will be set in the {@link
     * ParsePosition}.
     *
     * @param decimalPatternMatchRequired true to set an error if decimal is not present
     * @return The property bag, for chaining.
     */
    public IProperties setDecimalPatternMatchRequired(boolean decimalPatternMatchRequired);

    ParseMode DEFAULT_PARSE_MODE = null;

    /** @see #setParseMode */
    public ParseMode getParseMode();

    /**
     * Controls certain rules for how strict this parser is when reading strings. See {@link
     * ParseMode#LENIENT} and {@link ParseMode#STRICT}.
     *
     * @param parseMode Either {@link ParseMode#LENIENT} or {@link ParseMode#STRICT}.
     * @return The property bag, for chaining.
     */
    public IProperties setParseMode(ParseMode parseMode);

    boolean DEFAULT_PARSE_TO_BIG_DECIMAL = false;

    /** @see #setParseToBigDecimal */
    public boolean getParseToBigDecimal();

    /**
     * Whether to always return a BigDecimal from {@link Parse#parse} and all other parse methods.
     * By default, a Long or a BigInteger are returned when possible.
     *
     * @param parseToBigDecimal true to always return a BigDecimal; false to return a Long or a
     *     BigInteger when possible.
     * @return The property bag, for chaining.
     */
    public IProperties setParseToBigDecimal(boolean parseToBigDecimal);

    boolean DEFAULT_PARSE_CASE_SENSITIVE = false;

    /** @see #setParseCaseSensitive */
    public boolean getParseCaseSensitive();

    /**
     * Whether to require cases to match when parsing strings; default is true. Case sensitivity
     * applies to prefixes, suffixes, the exponent separator, the symbol "NaN", and the infinity
     * symbol. Grouping separators, decimal separators, and padding are always case-sensitive.
     * Currencies are always case-insensitive.
     *
     * <p>This setting is ignored in fast mode. In fast mode, strings are always compared in a
     * case-sensitive way.
     *
     * @param parseCaseSensitive true to be case-sensitive when parsing; false to allow any case.
     * @return The property bag, for chaining.
     */
    public IProperties setParseCaseSensitive(boolean parseCaseSensitive);
  }

  /**
   * @see Parse#parse(String, ParsePosition, ParseMode, boolean, boolean, IProperties,
   *     DecimalFormatSymbols)
   */
  private static enum StateName {
    BEFORE_PREFIX,
    AFTER_PREFIX,
    AFTER_INTEGER_DIGIT,
    AFTER_FRACTION_DIGIT,
    AFTER_EXPONENT_SEPARATOR,
    AFTER_EXPONENT_DIGIT,
    BEFORE_SUFFIX,
    BEFORE_SUFFIX_SEEN_EXPONENT,
    AFTER_SUFFIX,
    INSIDE_CURRENCY,
    INSIDE_DIGIT,
    INSIDE_STRING,
    INSIDE_AFFIX_PATTERN;
  }

  // TODO: Does this set make sense for the whitespace characters?
  private static final UnicodeSet UNISET_WHITESPACE =
      new UnicodeSet("[[:whitespace:][\\u2000-\\u200D]]").freeze();

  // BiDi characters are skipped over and ignored at any point in the string, even in strict mode.
  private static final UnicodeSet UNISET_BIDI =
      new UnicodeSet("[[\\u200E\\u200F\\u061C]]").freeze();

  // TODO: Re-generate these sets from the database. They probably haven't been updated in a while.
  private static final UnicodeSet UNISET_PERIOD_LIKE =
      new UnicodeSet("[.\\u2024\\u3002\\uFE12\\uFE52\\uFF0E\\uFF61]").freeze();
  private static final UnicodeSet UNISET_STRICT_PERIOD_LIKE =
      new UnicodeSet("[.\\u2024\\uFE52\\uFF0E\\uFF61]").freeze();
  private static final UnicodeSet UNISET_COMMA_LIKE =
      new UnicodeSet("[,\\u060C\\u066B\\u3001\\uFE10\\uFE11\\uFE50\\uFE51\\uFF0C\\uFF64]").freeze();
  private static final UnicodeSet UNISET_STRICT_COMMA_LIKE =
      new UnicodeSet("[,\\u066B\\uFE10\\uFE50\\uFF0C]").freeze();
  private static final UnicodeSet UNISET_OTHER_GROUPING_SEPARATORS =
      new UnicodeSet(
              "[\\ '\\u00A0\\u066C\\u2000-\\u200A\\u2018\\u2019\\u202F\\u205F\\u3000\\uFF07]")
          .freeze();

  private enum SeparatorType {
    COMMA_LIKE,
    PERIOD_LIKE,
    OTHER_GROUPING,
    UNKNOWN;

    static SeparatorType fromCp(int cp, ParseMode mode) {
      if (mode == ParseMode.FAST) {
        return SeparatorType.UNKNOWN;
      } else if (mode == ParseMode.STRICT) {
        if (UNISET_STRICT_COMMA_LIKE.contains(cp)) return COMMA_LIKE;
        if (UNISET_STRICT_PERIOD_LIKE.contains(cp)) return PERIOD_LIKE;
        if (UNISET_OTHER_GROUPING_SEPARATORS.contains(cp)) return OTHER_GROUPING;
        return UNKNOWN;
      } else {
        if (UNISET_COMMA_LIKE.contains(cp)) return COMMA_LIKE;
        if (UNISET_PERIOD_LIKE.contains(cp)) return PERIOD_LIKE;
        if (UNISET_OTHER_GROUPING_SEPARATORS.contains(cp)) return OTHER_GROUPING;
        return UNKNOWN;
      }
    }
  }

  private static enum DigitType {
    INTEGER,
    FRACTION,
    EXPONENT
  }

  /**
   * Holds a snapshot in time of a single parse path. This includes the digits seen so far, the
   * current state name, and other properties like the grouping separator used on this parse path,
   * details about the exponent and negative signs, etc.
   */
  private static class StateItem {
    // Parser state:
    // The "trailingChars" is used to keep track of how many characters from the end of the string
    // are ignorable and should be removed from the parse position should this item be accepted.
    // The "score" is used to help rank two otherwise equivalent parse paths. Currently, the only
    // function giving points to the score is prefix/suffix.
    StateName name;
    int trailingCount;
    int score;

    // Numerical value:
    FormatQuantity4 fq = new FormatQuantity4();
    int numDigits;
    int trailingZeros;
    int exponent;

    // Other items that we've seen:
    int groupingCp;
    long groupingWidths;
    String isoCode;
    boolean sawNegative;
    boolean sawNegativeExponent;
    boolean sawCurrency;
    boolean sawNaN;
    boolean sawInfinity;
    AffixHolder affix;
    boolean sawPrefix;
    boolean sawSuffix;
    boolean sawDecimalPoint;
    boolean sawExponentDigit;

    // Data for intermediate parsing steps:
    StateName returnTo1;
    StateName returnTo2;
    // For string literals:
    CharSequence currentString;
    int currentOffset;
    boolean currentTrailing;
    // For affix patterns:
    CharSequence currentAffixPattern;
    long currentStepwiseParserTag;
    // For currency:
    TextTrieMap<CurrencyStringInfo>.ParseState currentCurrencyTrieState;
    // For multi-code-point digits:
    TextTrieMap<Byte>.ParseState currentDigitTrieState;
    DigitType currentDigitType;

    /**
     * Clears the instance so that it can be re-used.
     *
     * @return Myself, for chaining.
     */
    StateItem clear() {
      // Parser state:
      name = StateName.BEFORE_PREFIX;
      trailingCount = 0;
      score = 0;

      // Numerical value:
      fq.clear();
      numDigits = 0;
      trailingZeros = 0;
      exponent = 0;

      // Other items we've seen:
      groupingCp = -1;
      groupingWidths = 0L;
      isoCode = null;
      sawNegative = false;
      sawNegativeExponent = false;
      sawCurrency = false;
      sawNaN = false;
      sawInfinity = false;
      affix = null;
      sawPrefix = false;
      sawSuffix = false;
      sawDecimalPoint = false;
      sawExponentDigit = false;

      // Data for intermediate parsing steps:
      returnTo1 = null;
      returnTo2 = null;
      currentString = null;
      currentOffset = 0;
      currentTrailing = false;
      currentAffixPattern = null;
      currentStepwiseParserTag = 0L;
      currentCurrencyTrieState = null;
      currentDigitTrieState = null;
      currentDigitType = null;

      return this;
    }

    /**
     * Sets the internal value of this instance equal to another instance.
     *
     * <p>newName and cpOrN1 are required as parameters to this function because every time a code
     * point is consumed and a state item is copied, both of the corresponding fields should be
     * updated; it would be an error if they weren't updated.
     *
     * @param other The instance to copy from.
     * @param newName The state name that the new copy should take on.
     * @param trailing If positive, record this code point as trailing; if negative, reset the
     *     trailing count to zero.
     * @return Myself, for chaining.
     */
    StateItem copyFrom(StateItem other, StateName newName, int trailing) {
      // Parser state:
      name = newName;
      score = other.score;

      // Either reset trailingCount or add the width of the current code point.
      trailingCount = (trailing < 0) ? 0 : other.trailingCount + Character.charCount(trailing);

      // Numerical value:
      fq.copyFrom(other.fq);
      numDigits = other.numDigits;
      trailingZeros = other.trailingZeros;
      exponent = other.exponent;

      // Other items we've seen:
      groupingCp = other.groupingCp;
      groupingWidths = other.groupingWidths;
      isoCode = other.isoCode;
      sawNegative = other.sawNegative;
      sawNegativeExponent = other.sawNegativeExponent;
      sawCurrency = other.sawCurrency;
      sawNaN = other.sawNaN;
      sawInfinity = other.sawInfinity;
      affix = other.affix;
      sawPrefix = other.sawPrefix;
      sawSuffix = other.sawSuffix;
      sawDecimalPoint = other.sawDecimalPoint;
      sawExponentDigit = other.sawExponentDigit;

      // Data for intermediate parsing steps:
      returnTo1 = other.returnTo1;
      returnTo2 = other.returnTo2;
      currentString = other.currentString;
      currentOffset = other.currentOffset;
      currentTrailing = other.currentTrailing;
      currentAffixPattern = other.currentAffixPattern;
      currentStepwiseParserTag = other.currentStepwiseParserTag;
      currentCurrencyTrieState = other.currentCurrencyTrieState;
      currentDigitTrieState = other.currentDigitTrieState;
      currentDigitType = other.currentDigitType;

      return this;
    }

    /**
     * Adds a digit to the internal representation of this instance.
     *
     * @param digit The digit that was read from the string.
     * @param type Whether the digit occured after the decimal point.
     */
    void appendDigit(byte digit, DigitType type) {
      if (type == DigitType.EXPONENT) {
        sawExponentDigit = true;
        int newExponent = exponent * 10 + digit;
        if (newExponent < exponent) {
          // overflow
          exponent = Integer.MAX_VALUE;
        } else {
          exponent = newExponent;
        }
      } else {
        numDigits++;
        if (type == DigitType.FRACTION && digit == 0) {
          trailingZeros++;
        } else if (type == DigitType.FRACTION) {
          fq.appendDigit(digit, trailingZeros, false);
          trailingZeros = 0;
        } else {
          fq.appendDigit(digit, 0, true);
        }
      }
    }

    /** @return Whether or not this item contains a valid number. */
    public boolean hasNumber() {
      return numDigits > 0 || sawNaN || sawInfinity;
    }

    /**
     * Converts the internal digits from this instance into a Number, preferring a Long, then a
     * BigInteger, then a BigDecimal. A Double is used for NaN, infinity, and -0.0.
     *
     * @return The Number. Never null.
     */
    Number toNumber(IProperties properties) {
      // Check for NaN, infinity, and -0.0
      if (sawNaN) {
        return Double.NaN;
      }
      if (sawInfinity) {
        if (sawNegative) {
          return Double.NEGATIVE_INFINITY;
        } else {
          return Double.POSITIVE_INFINITY;
        }
      }
      if (fq.isZero() && sawNegative) {
        return -0.0;
      }

      // Check for exponent overflow
      boolean forceBigDecimal = properties.getParseToBigDecimal();
      if (exponent == Integer.MAX_VALUE) {
        if (sawNegativeExponent && sawNegative) {
          return -0.0;
        } else if (sawNegativeExponent) {
          return 0.0;
        } else if (sawNegative) {
          return Double.NEGATIVE_INFINITY;
        } else {
          return Double.POSITIVE_INFINITY;
        }
      } else if (exponent > 1000) {
        // BigDecimals can handle huge values better than BigIntegers.
        forceBigDecimal = true;
      }

      // Multipliers must be applied in reverse.
      BigDecimal multiplier = properties.getMultiplier();
      if (properties.getMagnitudeMultiplier() != 0) {
        if (multiplier == null) multiplier = BigDecimal.ONE;
        multiplier = multiplier.scaleByPowerOfTen(properties.getMagnitudeMultiplier());
      }
      int delta = (sawNegativeExponent ? -1 : 1) * exponent;

      // We need to use a math context in order to prevent non-terminating decimal expansions.
      // This is only used when dividing by the multiplier.
      MathContext mc = RoundingUtils.getMathContextOr16Digits(properties);

      // Construct the output number.
      // This is the only step during fast-mode parsing that incurs object creations.
      BigDecimal result = fq.toBigDecimal();
      if (sawNegative) result = result.negate();
      result = result.scaleByPowerOfTen(delta);
      if (multiplier != null) {
        result = result.divide(multiplier, mc);
      }
      result = result.stripTrailingZeros();
      if (forceBigDecimal || result.scale() > 0) {
        return result;
      } else if (-result.scale() + result.precision() <= 18) {
        return result.longValueExact();
      } else {
        return result.toBigIntegerExact();
      }
    }

    /**
     * Converts the internal digits to a number, and also associates the number with the parsed
     * currency.
     *
     * @return The CurrencyAmount. Never null.
     */
    public CurrencyAmount toCurrencyAmount(IProperties properties) {
      assert isoCode != null;
      Number number = toNumber(properties);
      Currency currency = Currency.getInstance(isoCode);
      return new CurrencyAmount(number, currency);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("<ParserStateItem ");
      sb.append(name.name());
      if (name == StateName.INSIDE_STRING) {
        sb.append("{");
        sb.append(currentString);
        sb.append(":");
        sb.append(currentOffset);
        sb.append("}");
      }
      if (name == StateName.INSIDE_AFFIX_PATTERN) {
        sb.append("{");
        sb.append(currentAffixPattern);
        sb.append(":");
        sb.append(AffixPatternUtils.getOffset(currentStepwiseParserTag));
        sb.append("}");
      }
      sb.append(" ");
      sb.append(fq.toBigDecimal());
      sb.append(" grouping:");
      sb.append(groupingCp == -1 ? new char[] {'?'} : Character.toChars(groupingCp));
      sb.append(" widths:");
      sb.append(Long.toHexString(groupingWidths));
      sb.append(" seen:");
      sb.append(sawNegative ? 1 : 0);
      sb.append(sawNegativeExponent ? 1 : 0);
      sb.append(sawNaN ? 1 : 0);
      sb.append(sawInfinity ? 1 : 0);
      sb.append(sawPrefix ? 1 : 0);
      sb.append(sawSuffix ? 1 : 0);
      sb.append(sawDecimalPoint ? 1 : 0);
      sb.append(" score:");
      sb.append(score);
      sb.append(" affix:");
      sb.append(affix);
      sb.append(" currency:");
      sb.append(isoCode);
      sb.append(">");
      return sb.toString();
    }
  }

  /**
   * Holds an ordered list of {@link StateItem} and other metadata about the string to be parsed.
   * There are two internal arrays of {@link StateItem}, which are swapped back and forth in order
   * to avoid object creations. The items in one array can be populated at the same time that items
   * in the other array are being read from.
   */
  private static class ParserState {

    // Basic ParserStateItem lists:
    StateItem[] items = new StateItem[16];
    StateItem[] prevItems = new StateItem[16];
    int length;
    int prevLength;

    // Properties and Symbols memory:
    IProperties properties;
    DecimalFormatSymbols symbols;
    ParseMode mode;
    boolean caseSensitive;
    boolean parseCurrency;

    // Other pre-computed fields:
    int decimalCp1;
    int decimalCp2;
    int groupingCp1;
    int groupingCp2;
    SeparatorType decimalType1;
    SeparatorType decimalType2;
    // TODO(sffc): Remove this field if it is not necessary.
    @SuppressWarnings("unused")
    SeparatorType groupingType1;
    // TODO(sffc): Remove this field if it is not necessary.
    @SuppressWarnings("unused")
    SeparatorType groupingType2;
    TextTrieMap<Byte> digitTrie;
    Set<AffixHolder> affixHolders = new HashSet<AffixHolder>();

    ParserState() {
      for (int i = 0; i < items.length; i++) {
        items[i] = new StateItem();
        prevItems[i] = new StateItem();
      }
    }

    /**
     * Clears the internal state in order to prepare for parsing a new string.
     *
     * @return Myself, for chaining.
     */
    ParserState clear() {
      length = 0;
      prevLength = 0;
      digitTrie = null;
      affixHolders.clear();
      return this;
    }

    /**
     * Swaps the internal arrays of {@link StateItem}. Sets the length of the primary list to zero,
     * so that it can be appended to.
     */
    void swap() {
      StateItem[] temp = prevItems;
      prevItems = items;
      items = temp;
      prevLength = length;
      length = 0;
    }

    /**
     * Swaps the internal arrays of {@link StateItem}. Sets the length of the primary list to the
     * length of the previous list, so that it can be read from.
     */
    void swapBack() {
      StateItem[] temp = prevItems;
      prevItems = items;
      items = temp;
      length = prevLength;
      prevLength = 0;
    }

    /**
     * Gets the next available {@link StateItem} from the primary list for writing. This method
     * should be thought of like a list append method, except that there are no object creations
     * taking place.
     *
     * <p>It is the caller's responsibility to call either {@link StateItem#clear} or {@link
     * StateItem#copyFrom} on the returned object.
     *
     * @return A dirty {@link StateItem}.
     */
    StateItem getNext() {
      if (length >= items.length) {
        // TODO: What to do here? Expand the array?
        // This case is rare and would happen only with specially designed input.
        // For now, just overwrite the last entry.
        length = items.length - 1;
      }
      StateItem item = items[length];
      length++;
      return item;
    }

    /** @return The index of the last inserted StateItem via a call to {@link #getNext}. */
    public int lastInsertedIndex() {
      assert length > 0;
      return length - 1;
    }

    /**
     * Gets a {@link StateItem} from the primary list. Assumes that the item has already been added
     * via a call to {@link #getNext}.
     *
     * @param i The index of the item to get.
     * @return The item.
     */
    public StateItem getItem(int i) {
      assert i >= 0 && i < length;
      return items[i];
    }
  }

  private static class AffixHolder {
    final String p; // prefix
    final String s; // suffix
    final boolean strings;
    final boolean negative;

    static final AffixHolder EMPTY_POSITIVE = new AffixHolder("", "", true, false);
    static final AffixHolder EMPTY_NEGATIVE = new AffixHolder("", "", true, true);
    static final AffixHolder DEFAULT_POSITIVE = new AffixHolder("+", "", false, false);
    static final AffixHolder DEFAULT_NEGATIVE = new AffixHolder("-", "", false, true);

    static void addToState(ParserState state, IProperties properties) {
      AffixHolder pp = fromPropertiesPositivePattern(properties);
      AffixHolder np = fromPropertiesNegativePattern(properties);
      AffixHolder ps = fromPropertiesPositiveString(properties);
      AffixHolder ns = fromPropertiesNegativeString(properties);
      if (pp == null && ps == null) {
        if (properties.getPlusSignAlwaysShown()) {
          state.affixHolders.add(DEFAULT_POSITIVE);
        } else {
          state.affixHolders.add(EMPTY_POSITIVE);
        }
      } else {
        if (pp != null) state.affixHolders.add(pp);
        if (ps != null) state.affixHolders.add(ps);
      }
      if (np == null && ns == null) {
        state.affixHolders.add(DEFAULT_NEGATIVE);
      } else {
        if (np != null) state.affixHolders.add(np);
        if (ns != null) state.affixHolders.add(ns);
      }
    }

    static AffixHolder fromPropertiesPositivePattern(IProperties properties) {
      String ppp = properties.getPositivePrefixPattern();
      String psp = properties.getPositiveSuffixPattern();
      return getInstance(ppp, psp, false, false);
    }

    static AffixHolder fromPropertiesNegativePattern(IProperties properties) {
      String npp = properties.getNegativePrefixPattern();
      String nsp = properties.getNegativeSuffixPattern();
      return getInstance(npp, nsp, false, true);
    }

    static AffixHolder fromPropertiesPositiveString(IProperties properties) {
      String pp = properties.getPositivePrefix();
      String ps = properties.getPositiveSuffix();
      return getInstance(pp, ps, true, false);
    }

    static AffixHolder fromPropertiesNegativeString(IProperties properties) {
      String np = properties.getNegativePrefix();
      String ns = properties.getNegativeSuffix();
      return getInstance(np, ns, true, true);
    }

    static AffixHolder getInstance(String p, String s, boolean strings, boolean negative) {
      if (p == null && s == null) return null;
      if (p == null) p = "";
      if (s == null) s = "";
      if (p.length() == 0 && s.length() == 0) return negative ? EMPTY_NEGATIVE : EMPTY_POSITIVE;
      return new AffixHolder(p, s, strings, negative);
    }

    AffixHolder(String pp, String sp, boolean strings, boolean negative) {
      this.p = pp;
      this.s = sp;
      this.strings = strings;
      this.negative = negative;
    }

    @Override
    public boolean equals(Object other) {
      if (other == null) return false;
      if (this == other) return true;
      if (!(other instanceof AffixHolder)) return false;
      AffixHolder _other = (AffixHolder) other;
      if (!p.equals(_other.p)) return false;
      if (!s.equals(_other.s)) return false;
      if (strings != _other.strings) return false;
      if (negative != _other.negative) return false;
      return true;
    }

    @Override
    public int hashCode() {
      return p.hashCode() ^ s.hashCode();
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append(p);
      sb.append("|");
      sb.append(s);
      sb.append("|");
      sb.append(strings ? 'S' : 'P');
      sb.append("}");
      return sb.toString();
    }
  }

  /**
   * A class that holds information about all currency affix patterns for the locale. This allows
   * the parser to accept currencies in any format that are valid for the locale.
   */
  private static class CurrencyAffixPatterns {
    private final Set<AffixHolder> set = new HashSet<AffixHolder>();

    private static final ConcurrentHashMap<ULocale, CurrencyAffixPatterns> currencyAffixPatterns =
        new ConcurrentHashMap<ULocale, CurrencyAffixPatterns>();

    static void addToState(ULocale uloc, ParserState state) {
      if (!currencyAffixPatterns.containsKey(uloc)) {
        // There can be multiple threads computing the same CurrencyAffixPatterns simultaneously,
        // but that scenario is harmless.
        CurrencyAffixPatterns value = new CurrencyAffixPatterns(uloc);
        currencyAffixPatterns.put(uloc, value);
      }
      CurrencyAffixPatterns instance = currencyAffixPatterns.get(uloc);
      state.affixHolders.addAll(instance.set);
    }

    private CurrencyAffixPatterns(ULocale uloc) {
      // Get the basic currency pattern.
      String pattern = NumberFormat.getPattern(uloc, NumberFormat.CURRENCYSTYLE);
      addPattern(pattern);

      // Get the currency plural patterns.
      // TODO: Update this after CurrencyPluralInfo is replaced.
      CurrencyPluralInfo pluralInfo = CurrencyPluralInfo.getInstance(uloc);
      for (StandardPlural plural : StandardPlural.VALUES) {
        pattern = pluralInfo.getCurrencyPluralPattern(plural.getKeyword());
        addPattern(pattern);
      }
    }

    private static final ThreadLocal<Properties> threadLocalProperties =
        new ThreadLocal<Properties>() {
          @Override
          protected Properties initialValue() {
            return new Properties();
          }
        };

    private void addPattern(String pattern) {
      Properties properties = threadLocalProperties.get();
      try {
        PatternString.parseToExistingProperties(pattern, properties);
      } catch (IllegalArgumentException e) {
        // This should only happen if there is a bug in CLDR data. Fail silently.
      }
      set.add(AffixHolder.fromPropertiesPositivePattern(properties));
      set.add(AffixHolder.fromPropertiesNegativePattern(properties));
    }
  }

  /**
   * Makes a {@link TextTrieMap} for parsing digit strings. A trie is required only if the digit
   * strings are longer than one code point. In order for this to be the case, the user would have
   * needed to specify custom multi-character digits, like "(0)".
   *
   * @param digitStrings The list of digit strings from DecimalFormatSymbols.
   * @return A trie, or null if a trie is not required.
   */
  static TextTrieMap<Byte> makeDigitTrie(String[] digitStrings) {
    boolean requiresTrie = false;
    for (int i = 0; i < 10; i++) {
      String str = digitStrings[i];
      if (Character.charCount(Character.codePointAt(str, 0)) != str.length()) {
        requiresTrie = true;
        break;
      }
    }
    if (!requiresTrie) return null;

    TextTrieMap<Byte> trieMap = new TextTrieMap<Byte>(false);
    for (int i = 0; i < 10; i++) {
      trieMap.put(digitStrings[i], (byte) i);
    }
    return trieMap;
  }

  protected static final ThreadLocal<ParserState> threadLocalParseState =
      new ThreadLocal<ParserState>() {
        @Override
        protected ParserState initialValue() {
          return new ParserState();
        }
      };

  protected static final ThreadLocal<ParsePosition> threadLocalParsePosition =
      new ThreadLocal<ParsePosition>() {
        @Override
        protected ParsePosition initialValue() {
          return new ParsePosition(0);
        }
      };

  /**
   * @internal
   * @deprecated This API is ICU internal only. TODO: Remove this set from ScientificNumberFormat.
   */
  @Deprecated
  public static final UnicodeSet UNISET_PLUS =
      new UnicodeSet(
              0x002B, 0x002B, 0x207A, 0x207A, 0x208A, 0x208A, 0x2795, 0x2795, 0xFB29, 0xFB29,
              0xFE62, 0xFE62, 0xFF0B, 0xFF0B)
          .freeze();

  /**
   * @internal
   * @deprecated This API is ICU internal only. TODO: Remove this set from ScientificNumberFormat.
   */
  @Deprecated
  public static final UnicodeSet UNISET_MINUS =
      new UnicodeSet(
              0x002D, 0x002D, 0x207B, 0x207B, 0x208B, 0x208B, 0x2212, 0x2212, 0x2796, 0x2796,
              0xFE63, 0xFE63, 0xFF0D, 0xFF0D)
          .freeze();

  public static Number parse(String input, IProperties properties, DecimalFormatSymbols symbols) {
    ParsePosition ppos = threadLocalParsePosition.get();
    ppos.setIndex(0);
    return parse(input, ppos, properties, symbols);
  }

  // TODO: DELETE ME once debugging is finished
  public static volatile boolean DEBUGGING = false;

  /**
   * Implements an iterative parser that maintains a lists of possible states at each code point in
   * the string. At each code point in the string, the list of possible states is updated based on
   * the states coming from the previous code point. The parser stops when it reaches the end of the
   * string or when there are no possible parse paths remaining in the string.
   *
   * <p>TODO: This API is not fully flushed out. Right now this is internal-only.
   *
   * @param input The string to parse.
   * @param ppos A {@link ParsePosition} to hold the index at which parsing stopped.
   * @param properties A property bag, used only for determining the prefix/suffix strings and the
   *     padding character.
   * @param symbols A {@link DecimalFormatSymbols} object, used for determining locale-specific
   *     symbols for grouping/decimal separators, digit strings, and prefix/suffix substitutions.
   * @return A Number matching the parser's best interpretation of the string.
   */
  public static Number parse(
      CharSequence input,
      ParsePosition ppos,
      IProperties properties,
      DecimalFormatSymbols symbols) {
    StateItem best = _parse(input, ppos, false, properties, symbols);
    return (best == null) ? null : best.toNumber(properties);
  }

  public static CurrencyAmount parseCurrency(
      String input, IProperties properties, DecimalFormatSymbols symbols) throws ParseException {
    return parseCurrency(input, null, properties, symbols);
  }

  public static CurrencyAmount parseCurrency(
      CharSequence input, ParsePosition ppos, IProperties properties, DecimalFormatSymbols symbols)
      throws ParseException {
    if (ppos == null) {
      ppos = threadLocalParsePosition.get();
      ppos.setIndex(0);
      ppos.setErrorIndex(-1);
    }
    StateItem best = _parse(input, ppos, true, properties, symbols);
    return (best == null) ? null : best.toCurrencyAmount(properties);
  }

  private static StateItem _parse(
      CharSequence input,
      ParsePosition ppos,
      boolean parseCurrency,
      IProperties properties,
      DecimalFormatSymbols symbols) {

    if (input == null || ppos == null || properties == null || symbols == null) {
      throw new IllegalArgumentException("All arguments are required for parse.");
    }

    ParseMode mode = properties.getParseMode();
    if (mode == null) mode = ParseMode.LENIENT;
    boolean integerOnly = properties.getParseIntegerOnly();
    boolean ignoreExponent = properties.getParseNoExponent();

    // Set up the initial state
    ParserState state = threadLocalParseState.get().clear();
    state.properties = properties;
    state.symbols = symbols;
    state.mode = mode;
    state.parseCurrency = parseCurrency;
    state.caseSensitive = properties.getParseCaseSensitive();
    state.decimalCp1 = Character.codePointAt(symbols.getDecimalSeparatorString(), 0);
    state.decimalCp2 = Character.codePointAt(symbols.getMonetaryDecimalSeparatorString(), 0);
    state.groupingCp1 = Character.codePointAt(symbols.getGroupingSeparatorString(), 0);
    state.groupingCp2 = Character.codePointAt(symbols.getMonetaryGroupingSeparatorString(), 0);
    state.decimalType1 = SeparatorType.fromCp(state.decimalCp1, mode);
    state.decimalType2 = SeparatorType.fromCp(state.decimalCp2, mode);
    state.groupingType1 = SeparatorType.fromCp(state.groupingCp1, mode);
    state.groupingType2 = SeparatorType.fromCp(state.groupingCp2, mode);
    StateItem initialStateItem = state.getNext().clear();
    initialStateItem.name = StateName.BEFORE_PREFIX;

    if (mode == ParseMode.LENIENT || mode == ParseMode.STRICT) {
      state.digitTrie = makeDigitTrie(symbols.getDigitStringsLocal());
      AffixHolder.addToState(state, properties);
      if (parseCurrency) {
        CurrencyAffixPatterns.addToState(symbols.getULocale(), state);
      }
    }

    if (DEBUGGING) {
      System.out.println("Parsing: " + input);
      System.out.println(properties);
      System.out.println(state.affixHolders);
    }

    // Start walking through the string, one codepoint at a time. Backtracking is not allowed. This
    // is to enforce linear runtime and prevent cases that could result in an infinite loop.
    int offset = ppos.getIndex();
    for (; offset < input.length(); ) {
      int cp = Character.codePointAt(input, offset);
      state.swap();
      for (int i = 0; i < state.prevLength; i++) {
        StateItem item = state.prevItems[i];
        if (DEBUGGING) {
          System.out.println(":" + offset + " " + item);
        }

        // In the switch statement below, if you see a line like:
        //    if (state.length > 0 && mode == ParseMode.FAST) break;
        // it is used for accelerating the fast parse mode. The check is performed only in the
        // states BEFORE_PREFIX, AFTER_INTEGER_DIGIT, and AFTER_FRACTION_DIGIT, which are the
        // most common states.

        switch (item.name) {
          case BEFORE_PREFIX:
            // Beginning of string
            if (mode == ParseMode.LENIENT || mode == ParseMode.FAST) {
              acceptMinusOrPlusSign(cp, StateName.BEFORE_PREFIX, state, item, false);
              if (state.length > 0 && mode == ParseMode.FAST) break;
            }
            acceptIntegerDigit(cp, StateName.AFTER_INTEGER_DIGIT, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptBidi(cp, StateName.BEFORE_PREFIX, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptWhitespace(cp, StateName.BEFORE_PREFIX, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptPadding(cp, StateName.BEFORE_PREFIX, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptNan(cp, StateName.BEFORE_SUFFIX, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptInfinity(cp, StateName.BEFORE_SUFFIX, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            if (!integerOnly) {
              acceptDecimalPoint(cp, StateName.AFTER_FRACTION_DIGIT, state, item);
              if (state.length > 0 && mode == ParseMode.FAST) break;
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.STRICT) {
              acceptPrefix(cp, StateName.AFTER_PREFIX, state, item);
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.FAST) {
              acceptGrouping(cp, StateName.AFTER_INTEGER_DIGIT, state, item);
              if (state.length > 0 && mode == ParseMode.FAST) break;
              if (parseCurrency) {
                acceptCurrency(cp, StateName.BEFORE_PREFIX, state, item);
              }
            }
            break;

          case AFTER_PREFIX:
            // Prefix is consumed
            acceptBidi(cp, StateName.AFTER_PREFIX, state, item);
            acceptPadding(cp, StateName.AFTER_PREFIX, state, item);
            acceptNan(cp, StateName.BEFORE_SUFFIX, state, item);
            acceptInfinity(cp, StateName.BEFORE_SUFFIX, state, item);
            acceptIntegerDigit(cp, StateName.AFTER_INTEGER_DIGIT, state, item);
            if (!integerOnly) {
              acceptDecimalPoint(cp, StateName.AFTER_FRACTION_DIGIT, state, item);
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.FAST) {
              acceptWhitespace(cp, StateName.AFTER_PREFIX, state, item);
              acceptGrouping(cp, StateName.AFTER_INTEGER_DIGIT, state, item);
              if (parseCurrency) {
                acceptCurrency(cp, StateName.AFTER_PREFIX, state, item);
              }
            }
            break;

          case AFTER_INTEGER_DIGIT:
            // Previous character was an integer digit (or grouping/whitespace)
            acceptIntegerDigit(cp, StateName.AFTER_INTEGER_DIGIT, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            if (!integerOnly) {
              acceptDecimalPoint(cp, StateName.AFTER_FRACTION_DIGIT, state, item);
              if (state.length > 0 && mode == ParseMode.FAST) break;
            }
            acceptGrouping(cp, StateName.AFTER_INTEGER_DIGIT, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptBidi(cp, StateName.AFTER_INTEGER_DIGIT, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptPadding(cp, StateName.BEFORE_SUFFIX, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            if (!ignoreExponent) {
              acceptExponentSeparator(cp, StateName.AFTER_EXPONENT_SEPARATOR, state, item);
              if (state.length > 0 && mode == ParseMode.FAST) break;
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.STRICT) {
              acceptSuffix(cp, StateName.AFTER_SUFFIX, state, item);
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.FAST) {
              acceptWhitespace(cp, StateName.BEFORE_SUFFIX, state, item);
              if (state.length > 0 && mode == ParseMode.FAST) break;
              // TODO(sffc): acceptMinusOrPlusSign(cp, StateName.BEFORE_SUFFIX, state, item, false);
              if (state.length > 0 && mode == ParseMode.FAST) break;
              if (parseCurrency) {
                acceptCurrency(cp, StateName.BEFORE_SUFFIX, state, item);
              }
            }
            break;

          case AFTER_FRACTION_DIGIT:
            // We encountered a decimal point
            acceptFractionDigit(cp, StateName.AFTER_FRACTION_DIGIT, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptBidi(cp, StateName.AFTER_FRACTION_DIGIT, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            acceptPadding(cp, StateName.BEFORE_SUFFIX, state, item);
            if (state.length > 0 && mode == ParseMode.FAST) break;
            if (!ignoreExponent) {
              acceptExponentSeparator(cp, StateName.AFTER_EXPONENT_SEPARATOR, state, item);
              if (state.length > 0 && mode == ParseMode.FAST) break;
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.STRICT) {
              acceptSuffix(cp, StateName.AFTER_SUFFIX, state, item);
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.FAST) {
              acceptWhitespace(cp, StateName.BEFORE_SUFFIX, state, item);
              if (state.length > 0 && mode == ParseMode.FAST) break;
              // TODO(sffc): acceptMinusOrPlusSign(cp, StateName.BEFORE_SUFFIX, state, item, false);
              if (state.length > 0 && mode == ParseMode.FAST) break;
              if (parseCurrency) {
                acceptCurrency(cp, StateName.BEFORE_SUFFIX, state, item);
              }
            }
            break;

          case AFTER_EXPONENT_SEPARATOR:
            acceptBidi(cp, StateName.AFTER_EXPONENT_SEPARATOR, state, item);
            acceptMinusOrPlusSign(cp, StateName.AFTER_EXPONENT_SEPARATOR, state, item, true);
            acceptExponentDigit(cp, StateName.AFTER_EXPONENT_DIGIT, state, item);
            break;

          case AFTER_EXPONENT_DIGIT:
            acceptBidi(cp, StateName.AFTER_EXPONENT_DIGIT, state, item);
            acceptPadding(cp, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, state, item);
            acceptExponentDigit(cp, StateName.AFTER_EXPONENT_DIGIT, state, item);
            if (mode == ParseMode.LENIENT || mode == ParseMode.STRICT) {
              acceptSuffix(cp, StateName.AFTER_SUFFIX, state, item);
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.FAST) {
              acceptWhitespace(cp, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, state, item);
              // TODO(sffc): acceptMinusOrPlusSign(cp, StateName.BEFORE_SUFFIX, state, item, false);
              if (parseCurrency) {
                acceptCurrency(cp, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, state, item);
              }
            }
            break;

          case BEFORE_SUFFIX:
            // Accept whitespace, suffixes, and exponent separators
            acceptBidi(cp, StateName.BEFORE_SUFFIX, state, item);
            acceptPadding(cp, StateName.BEFORE_SUFFIX, state, item);
            if (!ignoreExponent) {
              acceptExponentSeparator(cp, StateName.AFTER_EXPONENT_SEPARATOR, state, item);
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.STRICT) {
              acceptSuffix(cp, StateName.AFTER_SUFFIX, state, item);
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.FAST) {
              acceptWhitespace(cp, StateName.BEFORE_SUFFIX, state, item);
              // TODO(sffc): acceptMinusOrPlusSign(cp, StateName.BEFORE_SUFFIX, state, item, false);
              if (parseCurrency) {
                acceptCurrency(cp, StateName.BEFORE_SUFFIX, state, item);
              }
            }
            break;

          case BEFORE_SUFFIX_SEEN_EXPONENT:
            // Accept whitespace and suffixes but not exponent separators
            acceptBidi(cp, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, state, item);
            acceptPadding(cp, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, state, item);
            if (mode == ParseMode.LENIENT || mode == ParseMode.STRICT) {
              acceptSuffix(cp, StateName.AFTER_SUFFIX, state, item);
            }
            if (mode == ParseMode.LENIENT || mode == ParseMode.FAST) {
              acceptWhitespace(cp, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, state, item);
              // TODO(sffc): acceptMinusOrPlusSign(cp, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, state, item, false);
              if (parseCurrency) {
                acceptCurrency(cp, StateName.BEFORE_SUFFIX_SEEN_EXPONENT, state, item);
              }
            }
            break;

          case AFTER_SUFFIX:
            if ((mode == ParseMode.LENIENT || mode == ParseMode.FAST) && parseCurrency) {
              // Continue traversing in case there is a currency symbol to consume
              acceptBidi(cp, StateName.AFTER_SUFFIX, state, item);
              acceptPadding(cp, StateName.AFTER_SUFFIX, state, item);
              acceptWhitespace(cp, StateName.AFTER_SUFFIX, state, item);
              // TODO(sffc): acceptMinusOrPlusSign(cp, StateName.AFTER_SUFFIX, state, item, false);
              if (parseCurrency) {
                acceptCurrency(cp, StateName.AFTER_SUFFIX, state, item);
              }
            }
            // Otherwise, do not accept any more characters.
            break;

          case INSIDE_CURRENCY:
            acceptCurrencyOffset(cp, state, item);
            break;

          case INSIDE_DIGIT:
            acceptDigitTrieOffset(cp, state, item);
            break;

          case INSIDE_STRING:
            acceptStringOffset(cp, state, item);
            // Accept arbitrary bidi in the middle of strings.
            if (state.length == 0 && UNISET_BIDI.contains(cp)) {
              state.getNext().copyFrom(item, item.name, cp);
            }
            break;

          case INSIDE_AFFIX_PATTERN:
            acceptAffixPatternOffset(cp, state, item);
            // Accept arbitrary bidi and whitespace (if lenient) in the middle of affixes.
            if (state.length == 0 && isIgnorable(cp, state)) {
              state.getNext().copyFrom(item, item.name, cp);
            }
            break;
        }
      }

      if (state.length == 0) {
        // No parse paths continue past this point. We have found the longest parsable string
        // from the input. Restore previous state without the offset and break.
        state.swapBack();
        break;
      }

      offset += Character.charCount(cp);
    }

    // Post-processing
    if (state.length == 0) {
      if (DEBUGGING) {
        System.out.println("No matches found");
        System.out.println("- - - - - - - - - -");
      }
      return null;
    } else {

      // Loop through the candidates.  "continue" skips a candidate as invalid.
      StateItem best = null;
      outer:
      for (int i = 0; i < state.length; i++) {
        StateItem item = state.items[i];

        if (DEBUGGING) {
          System.out.println(":end " + item);
        }

        // Check that at least one digit was read.
        if (!item.hasNumber()) {
          if (DEBUGGING) System.out.println("-> rejected due to no number value");
          continue;
        }

        if (mode == ParseMode.STRICT) {
          // Perform extra checks for strict mode.
          // We require that the affixes match.
          boolean sawPrefix = item.sawPrefix || (item.affix != null && item.affix.p.isEmpty());
          boolean sawSuffix = item.sawSuffix || (item.affix != null && item.affix.s.isEmpty());
          boolean hasEmptyAffix =
              state.affixHolders.contains(AffixHolder.EMPTY_POSITIVE)
                  || state.affixHolders.contains(AffixHolder.EMPTY_NEGATIVE);
          if (sawPrefix && sawSuffix) {
            // OK
          } else if (!sawPrefix && !sawSuffix && hasEmptyAffix) {
            // OK
          } else {
            // Has a prefix or suffix that doesn't match
            if (DEBUGGING) System.out.println("-> rejected due to mismatched prefix/suffix");
            continue;
          }

          // Check for scientific notation.
          if (properties.getMinimumExponentDigits() > 0 && !item.sawExponentDigit) {
            if (DEBUGGING) System.out.println("-> reject due to lack of exponent");
            continue;
          }

          // Check that grouping sizes are valid.
          int grouping1 = properties.getGroupingSize();
          int grouping2 = properties.getSecondaryGroupingSize();
          grouping1 = grouping1 > 0 ? grouping1 : grouping2;
          grouping2 = grouping2 > 0 ? grouping2 : grouping1;
          long groupingWidths = item.groupingWidths;
          int numGroupingRegions = 16 - Long.numberOfLeadingZeros(groupingWidths) / 4;
          // If the last grouping is zero, accept strings like "1," but reject string like "1,.23"
          // Strip off multiple last-groupings to handle cases like "123,," or "123  "
          while (numGroupingRegions > 1 && (groupingWidths & 0xf) == 0) {
            if (item.sawDecimalPoint) {
              if (DEBUGGING) System.out.println("-> rejected due to decimal point after grouping");
              continue outer;
            } else {
              groupingWidths >>>= 4;
              numGroupingRegions--;
            }
          }
          if (grouping1 < 0) {
            // OK (no grouping data available)
          } else if (numGroupingRegions <= 1) {
            // OK (no grouping digits)
          } else if ((groupingWidths & 0xf) != grouping1) {
            // First grouping size is invalid
            if (DEBUGGING) System.out.println("-> rejected due to first grouping violation");
            continue;
          } else if (((groupingWidths >>> ((numGroupingRegions - 1) * 4)) & 0xf) > grouping2) {
            // String like "1234,567" where the highest grouping is too large
            if (DEBUGGING) System.out.println("-> rejected due to final grouping violation");
            continue;
          } else {
            for (int j = 1; j < numGroupingRegions - 1; j++) {
              if (((groupingWidths >>> (j * 4)) & 0xf) != grouping2) {
                // A grouping size somewhere in the middle is invalid
                if (DEBUGGING) System.out.println("-> rejected due to inner grouping violation");
                continue outer;
              }
            }
          }
        }

        // Optionally require that the presence of a decimal point matches the pattern.
        if (properties.getDecimalPatternMatchRequired()
            && item.sawDecimalPoint != PositiveDecimalFormat.allowsDecimalPoint(properties)) {
          if (DEBUGGING) System.out.println("-> rejected due to decimal point violation");
          continue;
        }

        // When parsing currencies, require that a currency symbol was found.
        if (parseCurrency && !item.sawCurrency) {
          if (DEBUGGING) System.out.println("-> rejected due to lack of currency");
          continue;
        }

        // If we get here, then this candidate is acceptable.
        // Use the earliest candidate in the list, or the one with the highest score.
        if (best == null) {
          best = item;
        } else if (item.score > best.score) {
          best = item;
        }
      }

      if (DEBUGGING) {
        System.out.println("- - - - - - - - - -");
      }

      if (best != null) {
        ppos.setIndex(offset - best.trailingCount);
        return best;
      } else {
        ppos.setErrorIndex(offset);
        return null;
      }
    }
  }

  /**
   * If <code>cp</code> is whitespace (as determined by the unicode set {@link #UNISET_WHITESPACE}),
   * copies <code>item</code> to the new list in <code>state</code> and sets its state name to
   * <code>nextName</code>.
   *
   * @param cp The code point to check.
   * @param nextName The new state name if the check passes.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   */
  private static void acceptWhitespace(
      int cp, StateName nextName, ParserState state, StateItem item) {
    if (UNISET_WHITESPACE.contains(cp)) {
      state.getNext().copyFrom(item, nextName, cp);
    }
  }

  /**
   * If <code>cp</code> is a bidi control character (as determined by the unicode set {@link
   * #UNISET_BIDI}), copies <code>item</code> to the new list in <code>state</code> and sets its
   * state name to <code>nextName</code>.
   *
   * @param cp The code point to check.
   * @param nextName The new state name if the check passes.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   */
  private static void acceptBidi(int cp, StateName nextName, ParserState state, StateItem item) {
    if (UNISET_BIDI.contains(cp)) {
      state.getNext().copyFrom(item, nextName, cp);
    }
  }

  /**
   * If <code>cp</code> is a padding character (as determined by {@link ParserState#paddingCp}),
   * copies <code>item</code> to the new list in <code>state</code> and sets its state name to
   * <code>nextName</code>.
   *
   * @param cp The code point to check.
   * @param nextName The new state name if the check passes.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   */
  private static void acceptPadding(int cp, StateName nextName, ParserState state, StateItem item) {
    CharSequence padding = state.properties.getPadString();
    if (padding == null || padding.length() == 0) return;
    int referenceCp = Character.codePointAt(padding, 0);
    if (cp == referenceCp) {
      state.getNext().copyFrom(item, nextName, cp);
    }
  }

  private static void acceptIntegerDigit(
      int cp, StateName nextName, ParserState state, StateItem item) {
    acceptDigitHelper(cp, nextName, state, item, DigitType.INTEGER);
  }

  private static void acceptFractionDigit(
      int cp, StateName nextName, ParserState state, StateItem item) {
    acceptDigitHelper(cp, nextName, state, item, DigitType.FRACTION);
  }

  private static void acceptExponentDigit(
      int cp, StateName nextName, ParserState state, StateItem item) {
    acceptDigitHelper(cp, nextName, state, item, DigitType.EXPONENT);
  }

  /**
   * If <code>cp</code> is a digit character (as determined by either {@link UCharacter#digit} or
   * {@link ParserState#digitCps}), copies <code>item</code> to the new list in <code>state</code>
   * and sets its state name to one determined by <code>type</code>. Also copies the digit into a
   * field in the new item determined by <code>type</code>.
   *
   * <p>This function guarantees that it will add no more than one {@link StateItem} to the {@link
   * ParserState}. This means that {@link ParserState#lastInsertedIndex()} can be called to access
   * the {@link StateItem} that was inserted.
   *
   * @param cp The code point to check.
   * @param nextName The state to set if a digit is accepted.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   * @param type The digit type, which determines the next state and the field into which to insert
   *     the digit.
   */
  private static void acceptDigitHelper(
      int cp, StateName nextName, ParserState state, StateItem item, DigitType type) {
    // Check the Unicode digit character property
    byte digit = (byte) UCharacter.digit(cp, 10);
    StateItem next = null;

    // Look for the digit:
    if (digit >= 0) {
      // Code point is a number
      next = state.getNext().copyFrom(item, nextName, -1);
    }

    // Do not perform the expensive string manipulations in fast mode.
    if (digit < 0 && (state.mode == ParseMode.LENIENT || state.mode == ParseMode.STRICT)) {
      if (state.digitTrie == null) {
        // Check custom digits, all of which are at most one code point
        for (byte d = 0; d < 10; d++) {
          int referenceCp = Character.codePointAt(state.symbols.getDigitStringsLocal()[d], 0);
          if (cp == referenceCp) {
            digit = d;
            next = state.getNext().copyFrom(item, nextName, -1);
          }
        }
      } else {
        // Custom digits have more than one code point
        acceptDigitTrie(cp, nextName, state, item, type);
      }
    }

    // Save state:
    if (next != null) {
      next.appendDigit(digit, type);
      if (type == DigitType.INTEGER && (next.groupingWidths & 0xf) < 15) {
        next.groupingWidths++;
      }
    }
  }

  /**
   * If <code>cp</code> is a sign (as determined by the unicode sets {@link #UNISET_PLUS} and {@link
   * #UNISET_MINUS}), copies <code>item</code> to the new list in <code>state</code>. Loops back to
   * the same state name.
   *
   * @param cp The code point to check.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   */
  private static void acceptMinusOrPlusSign(
      int cp, StateName nextName, ParserState state, StateItem item, boolean exponent) {
    acceptMinusOrPlusSign(cp, nextName, null, state, item, exponent);
  }

  private static void acceptMinusOrPlusSign(
      int cp,
      StateName returnTo1,
      StateName returnTo2,
      ParserState state,
      StateItem item,
      boolean exponent) {
    if (UNISET_PLUS.contains(cp)) {
      StateItem next = state.getNext().copyFrom(item, returnTo1, -1);
      next.returnTo1 = returnTo2;
    } else if (UNISET_MINUS.contains(cp)) {
      StateItem next = state.getNext().copyFrom(item, returnTo1, -1);
      next.returnTo1 = returnTo2;
      if (exponent) {
        next.sawNegativeExponent = true;
      } else {
        next.sawNegative = true;
      }
    }
  }

  /**
   * If <code>cp</code> is a grouping separator (as determined by the unicode set {@link
   * #UNISET_GROUPING}), copies <code>item</code> to the new list in <code>state</code> and loops
   * back to the same state. Also accepts if <code>cp</code> is the locale-specific grouping
   * separator in {@link ParserState#groupingCp}, in which case the {@link
   * StateItem#usesLocaleSymbols} flag is also set.
   *
   * @param cp The code point to check.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   */
  private static void acceptGrouping(
      int cp, StateName nextName, ParserState state, StateItem item) {
    // Do not accept mixed grouping separators in the same string.
    if (item.groupingCp == -1) {
      // First time seeing a grouping separator.
      SeparatorType cpType = SeparatorType.fromCp(cp, state.mode);

      // Always accept if exactly the same as the locale symbol.
      // Otherwise, reject if UNKNOWN or in the same class as the decimal separator.
      if (cp != state.groupingCp1 && cp != state.groupingCp2) {
        if (cpType == SeparatorType.UNKNOWN) {
          return;
        }
        if (cpType == SeparatorType.COMMA_LIKE
            && (state.decimalType1 == SeparatorType.COMMA_LIKE
                || state.decimalType2 == SeparatorType.COMMA_LIKE)) {
          return;
        }
        if (cpType == SeparatorType.PERIOD_LIKE
            && (state.decimalType1 == SeparatorType.PERIOD_LIKE
                || state.decimalType2 == SeparatorType.PERIOD_LIKE)) {
          return;
        }
      }

      // A match was found.
      StateItem next = state.getNext().copyFrom(item, nextName, cp);
      next.groupingCp = cp;
      next.groupingWidths <<= 4;
    } else {
      // Have already seen a grouping separator.
      if (cp == item.groupingCp) {
        StateItem next = state.getNext().copyFrom(item, nextName, cp);
        next.groupingWidths <<= 4;
      }
    }
  }

  /**
   * If <code>cp</code> is a decimal (as determined by the unicode set {@link #UNISET_DECIMAL}),
   * copies <code>item</code> to the new list in <code>state</code> and goes to {@link
   * StateName#AFTER_FRACTION_DIGIT}. Also accepts if <code>cp</code> is the locale-specific decimal
   * point in {@link ParserState#decimalCp}, in which case the {@link StateItem#usesLocaleSymbols}
   * flag is also set.
   *
   * @param cp The code point to check.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   */
  private static void acceptDecimalPoint(
      int cp, StateName nextName, ParserState state, StateItem item) {
    if (cp == item.groupingCp) {
      // Don't accept a decimal point that is the same as the grouping separator
      return;
    }

    SeparatorType cpType = SeparatorType.fromCp(cp, state.mode);

    // We require that the decimal separator be in the same class as the locale.
    if (cpType != state.decimalType1 && cpType != state.decimalType2) {
      return;
    }

    // If in UNKNOWN or OTHER, require an exact match.
    if (cpType == SeparatorType.OTHER_GROUPING || cpType == SeparatorType.UNKNOWN) {
      if (cp != state.decimalCp1 && cp != state.decimalCp2) {
        return;
      }
    }

    // A match was found.
    StateItem next = state.getNext().copyFrom(item, nextName, -1);
    next.sawDecimalPoint = true;
  }

  private static void acceptNan(int cp, StateName nextName, ParserState state, StateItem item) {
    CharSequence nan = state.symbols.getNaN();
    long added = acceptString(cp, nextName, null, state, item, nan, 0, false);

    // Set state in the items that were added by the function call
    for (int i = Long.numberOfTrailingZeros(added); (1L << i) <= added; i++) {
      if (((1L << i) & added) != 0) {
        state.getItem(i).sawNaN = true;
      }
    }
  }

  private static void acceptInfinity(
      int cp, StateName nextName, ParserState state, StateItem item) {
    CharSequence inf = state.symbols.getInfinity();
    long added = acceptString(cp, nextName, null, state, item, inf, 0, false);

    // Set state in the items that were added by the function call
    for (int i = Long.numberOfTrailingZeros(added); (1L << i) <= added; i++) {
      if (((1L << i) & added) != 0) {
        state.getItem(i).sawInfinity = true;
      }
    }
  }

  private static void acceptExponentSeparator(
      int cp, StateName nextName, ParserState state, StateItem item) {
    CharSequence exp = state.symbols.getExponentSeparator();
    acceptString(cp, nextName, null, state, item, exp, 0, true);
  }

  private static void acceptPrefix(int cp, StateName nextName, ParserState state, StateItem item) {
    for (AffixHolder holder : state.affixHolders) {
      acceptAffixHolder(cp, nextName, state, item, holder, true);
    }
  }

  private static void acceptSuffix(int cp, StateName nextName, ParserState state, StateItem item) {
    if (item.affix != null) {
      acceptAffixHolder(cp, nextName, state, item, item.affix, false);
    } else {
      for (AffixHolder holder : state.affixHolders) {
        acceptAffixHolder(cp, nextName, state, item, holder, false);
      }
    }
  }

  private static void acceptAffixHolder(
      int cp,
      StateName nextName,
      ParserState state,
      StateItem item,
      AffixHolder holder,
      boolean prefix) {
    if (holder == null) return;
    String str = prefix ? holder.p : holder.s;
    if (holder.strings) {
      long added = acceptString(cp, nextName, null, state, item, str, 0, false);
      // At most one item can be added upon consuming a string.
      if (added != 0) {
        int i = state.lastInsertedIndex();
        // The following six lines are duplicated below; not enough for their own function.
        state.getItem(i).affix = holder;
        if (prefix) state.getItem(i).sawPrefix = true;
        if (!prefix) state.getItem(i).sawSuffix = true;
        if (holder.negative) state.getItem(i).sawNegative = true;
        state.getItem(i).score++; // reward for consuming a prefix/suffix.
      }
    } else {
      long added = acceptAffixPattern(cp, nextName, state, item, str, 0);
      // Multiple items can be added upon consuming an affix pattern.
      for (int i = Long.numberOfTrailingZeros(added); (1L << i) <= added; i++) {
        if (((1L << i) & added) != 0) {
          // The following six lines are duplicated above; not enough for their own function.
          state.getItem(i).affix = holder;
          if (prefix) state.getItem(i).sawPrefix = true;
          if (!prefix) state.getItem(i).sawSuffix = true;
          if (holder.negative) state.getItem(i).sawNegative = true;
          state.getItem(i).score++; // reward for consuming a prefix/suffix.
        }
      }
    }
  }

  private static void acceptStringOffset(int cp, ParserState state, StateItem item) {
    acceptString(
        cp,
        item.returnTo1,
        item.returnTo2,
        state,
        item,
        item.currentString,
        item.currentOffset,
        item.currentTrailing);
  }

  /**
   * Accepts a code point if the code point is compatible with the string at the given offset.
   *
   * <p>This method will add no more than one {@link StateItem} to the {@link ParserState}, which
   * means that at most one bit will be set in the return value, corresponding to the return value
   * of {@link ParserState#lastInsertedIndex()}.
   *
   * @param cp The current code point, which will be checked for a match to the string.
   * @param returnTo1 The state to return to after reaching the end of the string.
   * @param returnTo2 The state to save in <code>returnTo1</code> after reaching the end of the
   *     string. Set to null if returning to the main state loop.
   * @param trailing true if this string should be ignored for the purposes of recording trailing
   *     code points; false if it trailing count should be reset after reading the string.
   * @param state The current {@link ParserState}
   * @param item The current {@link StateItem}
   * @param str The string against which to check for a match.
   * @param offset The number of chars into the string. Initial value should be 0.
   * @return A bitmask where the bits correspond to the items that were added. Set to 0L if no items
   *     were added.
   */
  private static long acceptString(
      int cp,
      StateName returnTo1,
      StateName returnTo2,
      ParserState state,
      StateItem item,
      CharSequence str,
      int offset,
      boolean trailing) {
    if (str == null || str.length() == 0) return 0L;

    // Fast path for fast mode
    if (state.mode == ParseMode.FAST && Character.codePointAt(str, offset) != cp) return 0L;

    // Skip over bidi code points at the beginning of the string.
    // They will be accepted in the main loop.
    int count = 0;
    int referenceCp = -1;
    boolean equals = false;
    for (; offset < str.length(); offset += count) {
      referenceCp = Character.codePointAt(str, offset);
      count = Character.charCount(referenceCp);
      equals = codePointEquals(cp, referenceCp, state);
      if (!UNISET_BIDI.contains(cp)) break;
    }

    if (equals) {
      // Matches first code point of the string
      StateItem next = state.getNext().copyFrom(item, null, cp);

      // Skip over ignorable code points in the middle of the string.
      // They will be accepted in the main loop.
      offset += count;
      for (; offset < str.length(); offset += count) {
        referenceCp = Character.codePointAt(str, offset);
        count = Character.charCount(referenceCp);
        if (!UNISET_BIDI.contains(cp)) break;
      }

      if (offset < str.length()) {
        // String has more interesting code points.
        next.name = StateName.INSIDE_STRING;
        next.returnTo1 = returnTo1;
        next.returnTo2 = returnTo2;
        next.currentString = str;
        next.currentOffset = offset;
        next.currentTrailing = trailing;
      } else {
        // We've reached the end of the string.
        next.name = returnTo1;
        if (!trailing) next.trailingCount = 0;
        next.returnTo1 = returnTo2;
        next.returnTo2 = null;
      }
      return 1L << state.lastInsertedIndex();
    }
    return 0L;
  }

  private static void acceptAffixPatternOffset(int cp, ParserState state, StateItem item) {
    acceptAffixPattern(
        cp, item.returnTo1, state, item, item.currentAffixPattern, item.currentStepwiseParserTag);
  }

  /**
   * Accepts a code point if the code point is compatible with the affix pattern at the offset
   * encoded in the tag argument.
   *
   * @param cp The current code point, which will be checked for a match to the string.
   * @param returnTo The state to return to after reaching the end of the string.
   * @param state The current {@link ParserState}
   * @param item The current {@link StateItem}
   * @param str The string containing the affix pattern.
   * @param tag The current state of the stepwise parser. Initial value should be 0L.
   * @return A bitmask where the bits correspond to the items that were added. Set to 0L if no items
   *     were added.
   */
  private static long acceptAffixPattern(
      int cp, StateName returnTo, ParserState state, StateItem item, CharSequence str, long tag) {
    if (str == null || str.length() == 0) return 0L;

    // Skip over ignorable code points at the beginning of the affix pattern.
    // They will be accepted in the main loop.
    int typeOrCp = 0;
    boolean hasNext = true;
    while (hasNext) {
      tag = AffixPatternUtils.nextToken(tag, str);
      typeOrCp = AffixPatternUtils.getTypeOrCp(tag);
      hasNext = AffixPatternUtils.hasNext(tag, str);
      if (typeOrCp < 0 || !isIgnorable(typeOrCp, state)) break;
    }

    // Convert from the returned tag to a code point, string, or currency to check
    int resolvedCp = -1;
    CharSequence resolvedStr = null;
    boolean resolvedMinusSign = false;
    boolean resolvedPlusSign = false;
    boolean resolvedCurrency = false;
    if (typeOrCp < 0) {
      // Symbol
      switch (typeOrCp) {
        case AffixPatternUtils.TYPE_MINUS_SIGN:
          resolvedMinusSign = true;
          break;
        case AffixPatternUtils.TYPE_PLUS_SIGN:
          resolvedPlusSign = true;
          break;
        case AffixPatternUtils.TYPE_PERCENT:
          resolvedCp = '%'; // accept ASCII percent as well as locale percent
          resolvedStr = state.symbols.getPercentString();
          break;
        case AffixPatternUtils.TYPE_PERMILLE:
          resolvedCp = '‰'; // accept ASCII permille as well as locale permille
          resolvedStr = state.symbols.getPerMillString();
          break;
        case AffixPatternUtils.TYPE_CURRENCY_SINGLE:
        case AffixPatternUtils.TYPE_CURRENCY_DOUBLE:
        case AffixPatternUtils.TYPE_CURRENCY_TRIPLE:
          resolvedCurrency = true;
          break;
        default:
          throw new AssertionError();
      }
    } else {
      resolvedCp = typeOrCp;
    }

    // Skip over ignorable code points in the middle of the affix pattern.
    // They will be accepted in the main loop.
    while (hasNext) {
      long futureTag = AffixPatternUtils.nextToken(tag, str);
      int futureTypeOrCp = AffixPatternUtils.getTypeOrCp(futureTag);
      if (futureTypeOrCp < 0 || !isIgnorable(futureTypeOrCp, state)) break;
      tag = futureTag;
      typeOrCp = futureTypeOrCp;
      hasNext = AffixPatternUtils.hasNext(tag, str);
    }

    long added = 0L;
    if (resolvedCp >= 0) {
      // Code point
      if (!codePointEquals(cp, resolvedCp, state)) return 0L;
      StateItem next = state.getNext().copyFrom(item, null, cp);

      if (hasNext) {
        // Additional tokens in affix string.
        next.name = StateName.INSIDE_AFFIX_PATTERN;
        next.returnTo1 = returnTo;
      } else {
        // Reached last token in affix string.
        next.name = returnTo;
        next.trailingCount = 0;
        next.returnTo1 = null;
      }
      added |= 1L << state.lastInsertedIndex();
    }
    if (resolvedMinusSign || resolvedPlusSign) {
      // Sign
      if (hasNext) {
        acceptMinusOrPlusSign(cp, StateName.INSIDE_AFFIX_PATTERN, returnTo, state, item, false);
      } else {
        acceptMinusOrPlusSign(cp, returnTo, null, state, item, false);
      }
      // Decide whether to accept a custom string
      if (resolvedMinusSign) {
        String mss = state.symbols.getMinusSignString();
        int mssCp = Character.codePointAt(mss, 0);
        if (mss.length() != Character.charCount(mssCp) || !UNISET_MINUS.contains(mssCp)) {
          resolvedStr = mss;
        }
      }
      if (resolvedPlusSign) {
        String pss = state.symbols.getPlusSignString();
        int pssCp = Character.codePointAt(pss, 0);
        if (pss.length() != Character.charCount(pssCp) || !UNISET_MINUS.contains(pssCp)) {
          resolvedStr = pss;
        }
      }
    }
    if (resolvedStr != null) {
      // String symbol
      if (hasNext) {
        added |=
            acceptString(
                cp, StateName.INSIDE_AFFIX_PATTERN, returnTo, state, item, resolvedStr, 0, false);
      } else {
        added |= acceptString(cp, returnTo, null, state, item, resolvedStr, 0, false);
      }
    }
    if (resolvedCurrency) {
      // Currency symbol
      if (hasNext) {
        added |= acceptCurrency(cp, StateName.INSIDE_AFFIX_PATTERN, returnTo, state, item);
      } else {
        added |= acceptCurrency(cp, returnTo, null, state, item);
      }
    }

    // Set state in the items that were added by the function calls
    for (int i = Long.numberOfTrailingZeros(added); (1L << i) <= added; i++) {
      if (((1L << i) & added) != 0) {
        state.getItem(i).currentAffixPattern = str;
        state.getItem(i).currentStepwiseParserTag = tag;
      }
    }
    return added;
  }

  /**
   * This method can add up to four items to the new list in <code>state</code>.
   *
   * <p>If <code>cp</code> is equal to any known ISO code or long name, copies <code>item</code> to
   * the new list in <code>state</code> and sets its ISO code to the corresponding currency.
   *
   * <p>If <code>cp</code> is the first code point of any ISO code or long name having more them one
   * code point in length, copies <code>item</code> to the new list in <code>state</code> along with
   * an instance of {@link TextTrieMap.ParseState} for tracking the following code points.
   *
   * @param cp The code point to check.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   */
  private static void acceptCurrency(
      int cp, StateName nextName, ParserState state, StateItem item) {
    acceptCurrency(cp, nextName, null, state, item);
  }

  private static long acceptCurrency(
      int cp, StateName returnTo1, StateName returnTo2, ParserState state, StateItem item) {
    if (item.sawCurrency) return 0L;
    long added = 0L;

    // Accept from local currency information
    String str1, str2;
    Currency currency = state.properties.getCurrency();
    if (currency != null) {
      str1 = currency.getName(state.symbols.getULocale(), Currency.SYMBOL_NAME, null);
      str2 = currency.getCurrencyCode();
      // TODO: Should we also accept long names? In currency mode, they are in the CLDR data.
    } else {
      currency = state.symbols.getCurrency();
      str1 = state.symbols.getCurrencySymbol();
      str2 = state.symbols.getInternationalCurrencySymbol();
    }
    added |= acceptString(cp, returnTo1, returnTo2, state, item, str1, 0, false);
    added |= acceptString(cp, returnTo1, returnTo2, state, item, str2, 0, false);
    for (int i = Long.numberOfTrailingZeros(added); (1L << i) <= added; i++) {
      if (((1L << i) & added) != 0) {
        state.getItem(i).sawCurrency = true;
        state.getItem(i).isoCode = str2;
      }
    }

    // Accept from CLDR data
    if (state.parseCurrency) {
      ULocale uloc = state.symbols.getULocale();
      TextTrieMap<Currency.CurrencyStringInfo>.ParseState trie1 =
          Currency.openParseState(uloc, cp, Currency.LONG_NAME);
      TextTrieMap<Currency.CurrencyStringInfo>.ParseState trie2 =
          Currency.openParseState(uloc, cp, Currency.SYMBOL_NAME);
      added |= acceptCurrencyHelper(cp, returnTo1, returnTo2, state, item, trie1);
      added |= acceptCurrencyHelper(cp, returnTo1, returnTo2, state, item, trie2);
    }

    return added;
  }

  /**
   * If <code>cp</code> is the next code point of any currency, copies <code>item</code> to the new
   * list in <code>state</code> along with an instance of {@link TextTrieMap.ParseState} for
   * tracking the following code points.
   *
   * <p>This method should only be called in a state following {@link #acceptCurrency}.
   *
   * @param cp The code point to check.
   * @param state The state object to update.
   * @param item The old state leading into the code point.
   */
  private static void acceptCurrencyOffset(int cp, ParserState state, StateItem item) {
    acceptCurrencyHelper(
        cp, item.returnTo1, item.returnTo2, state, item, item.currentCurrencyTrieState);
  }

  private static long acceptCurrencyHelper(
      int cp,
      StateName returnTo1,
      StateName returnTo2,
      ParserState state,
      StateItem item,
      TextTrieMap<Currency.CurrencyStringInfo>.ParseState trieState) {
    if (trieState == null) return 0L;
    trieState.accept(cp);
    long added = 0L;
    Iterator<Currency.CurrencyStringInfo> currentMatches = trieState.getCurrentMatches();
    if (currentMatches != null) {
      // Match on current code point
      // TODO: What should happen with multiple currency matches?
      StateItem next = state.getNext().copyFrom(item, returnTo1, -1);
      next.returnTo1 = returnTo2;
      next.returnTo2 = null;
      next.sawCurrency = true;
      next.isoCode = currentMatches.next().getISOCode();
      added |= 1L << state.lastInsertedIndex();
    }
    if (!trieState.atEnd()) {
      // Prepare for matches on future code points
      StateItem next = state.getNext().copyFrom(item, StateName.INSIDE_CURRENCY, -1);
      next.returnTo1 = returnTo1;
      next.returnTo2 = returnTo2;
      next.currentCurrencyTrieState = trieState;
      added |= 1L << state.lastInsertedIndex();
    }
    return added;
  }

  private static long acceptDigitTrie(
      int cp, StateName nextName, ParserState state, StateItem item, DigitType type) {
    assert state.digitTrie != null;
    TextTrieMap<Byte>.ParseState trieState = state.digitTrie.openParseState(cp);
    if (trieState == null) return 0L;
    return acceptDigitTrieHelper(cp, nextName, state, item, type, trieState);
  }

  private static void acceptDigitTrieOffset(int cp, ParserState state, StateItem item) {
    acceptDigitTrieHelper(
        cp, item.returnTo1, state, item, item.currentDigitType, item.currentDigitTrieState);
  }

  private static long acceptDigitTrieHelper(
      int cp,
      StateName returnTo1,
      ParserState state,
      StateItem item,
      DigitType type,
      TextTrieMap<Byte>.ParseState trieState) {
    if (trieState == null) return 0L;
    trieState.accept(cp);
    long added = 0L;
    Iterator<Byte> currentMatches = trieState.getCurrentMatches();
    if (currentMatches != null) {
      // Match on current code point
      byte digit = currentMatches.next();
      StateItem next = state.getNext().copyFrom(item, returnTo1, -1);
      next.returnTo1 = null;
      next.appendDigit(digit, type);
      added |= 1L << state.lastInsertedIndex();
    }
    if (!trieState.atEnd()) {
      // Prepare for matches on future code points
      StateItem next = state.getNext().copyFrom(item, StateName.INSIDE_DIGIT, -1);
      next.returnTo1 = returnTo1;
      next.currentDigitTrieState = trieState;
      next.currentDigitType = type;
      added |= 1L << state.lastInsertedIndex();
    }
    return added;
  }

  /**
   * Checks whether the two given code points are equal after applying case mapping as requested in
   * the ParserState.
   *
   * @see #acceptString
   * @see #acceptAffixPattern
   */
  private static boolean codePointEquals(int cp1, int cp2, ParserState state) {
    if (!state.caseSensitive) {
      cp1 = UCharacter.foldCase(cp1, true);
      cp2 = UCharacter.foldCase(cp2, true);
    }
    return cp1 == cp2;
  }

  /**
   * Checks whether the given code point is "ignorable" and should be skipped. BiDi characters are
   * always ignorable, and whitespace is ignorable in lenient mode.
   *
   * @param cp The code point to test. Returns false if cp is negative.
   * @param state The current {@link ParserState}, used for determining strict mode.
   * @return true if cp is bidi or whitespace in lenient mode; false otherwise.
   */
  private static boolean isIgnorable(int cp, ParserState state) {
    if (cp < 0) return false;
    if (UNISET_BIDI.contains(cp)) return true;
    return state.mode == ParseMode.LENIENT && UNISET_WHITESPACE.contains(cp);
  }
}
