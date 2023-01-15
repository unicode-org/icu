// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.text;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamField;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.ParsePosition;

import com.ibm.icu.impl.FormattedStringBuilder;
import com.ibm.icu.impl.FormattedValueStringBuilderImpl;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.number.AffixUtils;
import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.DecimalFormatProperties.ParseMode;
import com.ibm.icu.impl.number.DecimalQuantity;
import com.ibm.icu.impl.number.DecimalQuantity_DualStorageBCD;
import com.ibm.icu.impl.number.Padder;
import com.ibm.icu.impl.number.Padder.PadPosition;
import com.ibm.icu.impl.number.PatternStringParser;
import com.ibm.icu.impl.number.PatternStringUtils;
import com.ibm.icu.impl.number.parse.NumberParserImpl;
import com.ibm.icu.impl.number.parse.ParsedNumber;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.math.MathContext;
import com.ibm.icu.number.FormattedNumber;
import com.ibm.icu.number.LocalizedNumberFormatter;
import com.ibm.icu.number.NumberFormatter;
import com.ibm.icu.text.PluralRules.IFixedDecimal;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.Currency.CurrencyUsage;
import com.ibm.icu.util.CurrencyAmount;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;

/**
 * {@icuenhanced java.text.DecimalFormat}.{@icu _usage_}
 *
 * <p>
 * <strong>IMPORTANT:</strong> New users are strongly encouraged to see if
 * {@link NumberFormatter} fits their use case.  Although not deprecated, this
 * class, DecimalFormat, is only provided for java.text.DecimalFormat compatibility.
 * <hr>
 *
 * <code>DecimalFormat</code> is the primary
 * concrete subclass of {@link NumberFormat}. It has a variety of features designed to make it
 * possible to parse and format numbers in any locale, including support for Western, Arabic, or
 * Indic digits. It supports different flavors of numbers, including integers ("123"), fixed-point
 * numbers ("123.4"), scientific notation ("1.23E4"), percentages ("12%"), and currency amounts
 * ("$123.00", "USD123.00", "123.00 US dollars"). All of these flavors can be easily localized.
 *
 * <p>To obtain a number formatter for a specific locale (including the default locale), call one of
 * NumberFormat's factory methods such as {@link NumberFormat#getInstance}. Do not call
 * DecimalFormat constructors directly unless you know what you are doing.
 *
 * <p>DecimalFormat aims to comply with the specification <a
 * href="http://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns">UTS #35</a>. Read
 * the specification for more information on how all the properties in DecimalFormat fit together.
 *
 * <p><strong>NOTE:</strong> Starting in ICU 60, there is a new set of APIs for localized number
 * formatting that are designed to be an improvement over DecimalFormat.  New users are discouraged
 * from using DecimalFormat.  For more information, see the package com.ibm.icu.number.
 *
 * <h3>Example Usage</h3>
 *
 * <p>Customize settings on a DecimalFormat instance from the NumberFormat factory:
 *
 * <blockquote>
 *
 * <pre>
 * NumberFormat f = NumberFormat.getInstance(loc);
 * if (f instanceof DecimalFormat) {
 *     ((DecimalFormat) f).setDecimalSeparatorAlwaysShown(true);
 *     ((DecimalFormat) f).setMinimumGroupingDigits(2);
 * }
 * </pre>
 *
 * </blockquote>
 *
 * <p>Quick and dirty print out a number using the localized number, currency, and percent format
 * for each locale:
 *
 * <blockquote>
 *
 * <pre>
 * for (ULocale uloc : ULocale.getAvailableLocales()) {
 *     System.out.print(uloc + ":\t");
 *     System.out.print(NumberFormat.getInstance(uloc).format(1.23));
 *     System.out.print("\t");
 *     System.out.print(NumberFormat.getCurrencyInstance(uloc).format(1.23));
 *     System.out.print("\t");
 *     System.out.print(NumberFormat.getPercentInstance(uloc).format(1.23));
 *     System.out.println();
 * }
 * </pre>
 *
 * </blockquote>
 *
 * <h3>Properties and Symbols</h3>
 *
 * <p>A DecimalFormat object encapsulates a set of <em>properties</em> and a set of
 * <em>symbols</em>. Grouping size, rounding mode, and affixes are examples of properties. Locale
 * digits and the characters used for grouping and decimal separators are examples of symbols.
 *
 * <p>To set a custom set of symbols, use {@link #setDecimalFormatSymbols}. Use the various other
 * setters in this class to set custom values for the properties.
 *
 * <h3>Rounding</h3>
 *
 * <p>DecimalFormat provides three main strategies to specify the position at which numbers should
 * be rounded:
 *
 * <ol>
 *   <li><strong>Magnitude:</strong> Display a fixed number of fraction digits; this is the most
 *       common form.
 *   <li><strong>Increment:</strong> Round numbers to the closest multiple of a certain increment,
 *       such as 0.05. This is common in currencies.
 *   <li><strong>Significant Digits:</strong> Round numbers such that a fixed number of nonzero
 *       digits are shown. This is most common in scientific notation.
 * </ol>
 *
 * <p>It is not possible to specify more than one rounding strategy. For example, setting a rounding
 * increment in conjunction with significant digits results in undefined behavior.
 *
 * <p>It is also possible to specify the <em>rounding mode</em> to use. The default rounding mode is
 * "half even", which rounds numbers to their closest increment, with ties broken in favor of
 * trailing numbers being even. For more information, see {@link #setRoundingMode} and <a
 * href="https://unicode-org.github.io/icu/userguide/format_parse/numbers/rounding-modes">the ICU
 * User Guide</a>.
 *
 * <h3>Pattern Strings</h3>
 *
 * <p>A <em>pattern string</em> is a way to serialize some of the available properties for decimal
 * formatting. However, not all properties are capable of being serialized into a pattern string;
 * see {@link #applyPattern} for more information.
 *
 * <p>Most users should not need to interface with pattern strings directly.
 *
 * <p>ICU DecimalFormat aims to follow the specification for pattern strings in <a
 * href="http://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns">UTS #35</a>.
 * Refer to that specification for more information on pattern string syntax.
 *
 * <h4>Pattern String BNF</h4>
 *
 * The following BNF is used when parsing the pattern string into property values:
 *
 * <pre>
 * pattern    := subpattern (';' subpattern)?
 * subpattern := prefix? number exponent? suffix?
 * number     := (integer ('.' fraction)?) | sigDigits
 * prefix     := '&#92;u0000'..'&#92;uFFFD' - specialCharacters
 * suffix     := '&#92;u0000'..'&#92;uFFFD' - specialCharacters
 * integer    := '#'* '0'* '0'
 * fraction   := '0'* '#'*
 * sigDigits  := '#'* '@' '@'* '#'*
 * exponent   := 'E' '+'? '0'* '0'
 * padSpec    := '*' padChar
 * padChar    := '&#92;u0000'..'&#92;uFFFD' - quote
 * &#32;
 * Notation:
 *   X*       0 or more instances of X
 *   X?       0 or 1 instances of X
 *   X|Y      either X or Y
 *   C..D     any character from C up to D, inclusive
 *   S-T      characters in S, except those in T
 * </pre>
 *
 * <p>The first subpattern is for positive numbers. The second (optional) subpattern is for negative
 * numbers.
 *
 * <p>Not indicated in the BNF syntax above:
 *
 * <ul>
 *   <li>The grouping separator ',' can occur inside the integer and sigDigits elements, between any
 *       two pattern characters of that element, as long as the integer or sigDigits element is not
 *       followed by the exponent element.
 *   <li>Two grouping intervals are recognized: That between the decimal point and the first
 *       grouping symbol, and that between the first and second grouping symbols. These intervals
 *       are identical in most locales, but in some locales they differ. For example, the pattern
 *       &quot;#,##,###&quot; formats the number 123456789 as &quot;12,34,56,789&quot;.
 *   <li>The pad specifier <code>padSpec</code> may appear before the prefix, after the prefix,
 *       before the suffix, after the suffix, or not at all.
 *   <li>In place of '0', the digits '1' through '9' may be used to indicate a rounding increment.
 * </ul>
 *
 * <h3>Parsing</h3>
 *
 * <p>DecimalFormat aims to be able to parse anything that it can output as a formatted string.
 *
 * <p>There are two primary parse modes: <em>lenient</em> and <em>strict</em>. Lenient mode should
 * be used if the goal is to parse user input to a number; strict mode should be used if the goal is
 * validation. The default is lenient mode. For more information, see {@link #setParseStrict}.
 *
 * <p><code>DecimalFormat</code> parses all Unicode characters that represent decimal digits, as
 * defined by {@link UCharacter#digit}. In addition, <code>DecimalFormat</code> also recognizes as
 * digits the ten consecutive characters starting with the localized zero digit defined in the
 * {@link DecimalFormatSymbols} object. During formatting, the {@link DecimalFormatSymbols}-based
 * digits are output.
 *
 * <p>Grouping separators are ignored in lenient mode (default). In strict mode, grouping separators
 * must match the locale-specified grouping sizes.
 *
 * <p>When using {@link #parseCurrency}, all currencies are accepted, not just the currency
 * currently set in the formatter. In addition, the formatter is able to parse every currency style
 * format for a particular locale no matter which style the formatter is constructed with. For
 * example, a formatter instance gotten from NumberFormat.getInstance(ULocale,
 * NumberFormat.CURRENCYSTYLE) can parse both "USD1.00" and "3.00 US dollars".
 *
 * <p>Whitespace characters (lenient mode) and control characters (lenient and strict mode),
 * collectively called "ignorables", do not need to match in identity or quantity between the
 * pattern string and the input string. For example, the pattern "# %" matches "35 %" (with a single
 * space), "35%" (with no space), "35&nbsp;%" (with a non-breaking space), and "35&nbsp; %" (with
 * multiple spaces). Arbitrary ignorables are also allowed at boundaries between the parts of the
 * number: prefix, number, exponent separator, and suffix. Ignorable whitespace characters are those
 * having the Unicode "blank" property for regular expressions, defined in UTS #18 Annex C, which is
 * "horizontal" whitespace, like spaces and tabs, but not "vertical" whitespace, like line breaks.
 * Ignorable control characters are those in the Unicode set [:Default_Ignorable_Code_Point:].
 *
 * <p>If {@link #parse(String, ParsePosition)} fails to parse a string, it returns <code>null</code>
 * and leaves the parse position unchanged. The convenience method {@link #parse(String)} indicates
 * parse failure by throwing a {@link java.text.ParseException}.
 *
 * <p>Under the hood, a state table parsing engine is used. To debug a parsing failure during
 * development, use the following pattern to print details about the state table transitions:
 *
 * <pre>
 * com.ibm.icu.impl.number.Parse.DEBUGGING = true;
 * df.parse("123.45", ppos);
 * com.ibm.icu.impl.number.Parse.DEBUGGING = false;
 * </pre>
 *
 * <h3>Thread Safety and Best Practices</h3>
 *
 * <p>Starting with ICU 59, instances of DecimalFormat are thread-safe.
 *
 * <p>Under the hood, DecimalFormat maintains an immutable formatter object that is rebuilt whenever
 * any of the property setters are called. It is therefore best practice to call property setters
 * only during construction and not when formatting numbers online.
 *
 * @see java.text.Format
 * @see NumberFormat
 * @stable ICU 2.0
 */
public class DecimalFormat extends NumberFormat {

  /** New serialization in ICU 59: declare different version from ICU 58. */
  private static final long serialVersionUID = 864413376551465018L;

  /**
   * One non-transient field such that deserialization can determine the version of the class. This
   * field has existed since the very earliest versions of DecimalFormat.
   */
  @SuppressWarnings("unused")
  private final int serialVersionOnStream = 5;

  //=====================================================================================//
  //                                   INSTANCE FIELDS                                   //
  //=====================================================================================//

  // Fields are package-private, so that subclasses can use them.
  // properties should be final, but clone won't work if we make it final.
  // All fields are transient because custom serialization is used.

  /**
   * The property bag corresponding to user-specified settings and settings from the pattern string.
   * In principle this should be final, but serialize and clone won't work if it is final. Does not
   * need to be volatile because the reference never changes.
   */
  /* final */ transient DecimalFormatProperties properties;

  /**
   * The symbols for the current locale. Volatile because threads may read and write at the same
   * time.
   */
  transient volatile DecimalFormatSymbols symbols;

  /**
   * The pre-computed formatter object. Setters cause this to be re-computed atomically. The {@link
   * #format} method uses the formatter directly without needing to synchronize. Volatile because
   * threads may read and write at the same time.
   */
  transient volatile LocalizedNumberFormatter formatter;

  /**
   * The effective properties as exported from the formatter object. Volatile because threads may
   * read and write at the same time.
   */
  transient volatile DecimalFormatProperties exportedProperties;

  transient volatile NumberParserImpl parser;
  transient volatile NumberParserImpl currencyParser;

  //=====================================================================================//
  //                                    CONSTRUCTORS                                     //
  //=====================================================================================//

  /**
   * Creates a DecimalFormat based on the number pattern and symbols for the default locale. This is
   * a convenient way to obtain a DecimalFormat instance when internationalization is not the main
   * concern.
   *
   * <p>Most users should call the factory methods on NumberFormat, such as {@link
   * NumberFormat#getNumberInstance}, which return localized formatter objects, instead of the
   * DecimalFormat constructors.
   *
   * @see NumberFormat#getInstance
   * @see NumberFormat#getNumberInstance
   * @see NumberFormat#getCurrencyInstance
   * @see NumberFormat#getPercentInstance
   * @see Category#FORMAT
   * @stable ICU 2.0
   */
  public DecimalFormat() {
    // Use the locale's default pattern
    ULocale def = ULocale.getDefault(ULocale.Category.FORMAT);
    String pattern = getPattern(def, NumberFormat.NUMBERSTYLE);
    symbols = getDefaultSymbols();
    properties = new DecimalFormatProperties();
    exportedProperties = new DecimalFormatProperties();
    // Regression: ignore pattern rounding information if the pattern has currency symbols.
    setPropertiesFromPattern(pattern, PatternStringParser.IGNORE_ROUNDING_IF_CURRENCY);
    refreshFormatter();
  }

  /**
   * Creates a DecimalFormat based on the given pattern, using symbols for the default locale. This
   * is a convenient way to obtain a DecimalFormat instance when internationalization is not the
   * main concern.
   *
   * <p>Most users should call the factory methods on NumberFormat, such as {@link
   * NumberFormat#getNumberInstance}, which return localized formatter objects, instead of the
   * DecimalFormat constructors.
   *
   * @param pattern A pattern string such as "#,##0.00" conforming to <a
   *     href="http://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns">UTS
   *     #35</a>.
   * @throws IllegalArgumentException if the given pattern is invalid.
   * @see NumberFormat#getInstance
   * @see NumberFormat#getNumberInstance
   * @see NumberFormat#getCurrencyInstance
   * @see NumberFormat#getPercentInstance
   * @see Category#FORMAT
   * @stable ICU 2.0
   */
  public DecimalFormat(String pattern) {
    symbols = getDefaultSymbols();
    properties = new DecimalFormatProperties();
    exportedProperties = new DecimalFormatProperties();
    // Regression: ignore pattern rounding information if the pattern has currency symbols.
    setPropertiesFromPattern(pattern, PatternStringParser.IGNORE_ROUNDING_IF_CURRENCY);
    refreshFormatter();
  }

  /**
   * Creates a DecimalFormat based on the given pattern and symbols. Use this constructor if you
   * want complete control over the behavior of the formatter.
   *
   * <p>Most users should call the factory methods on NumberFormat, such as {@link
   * NumberFormat#getNumberInstance}, which return localized formatter objects, instead of the
   * DecimalFormat constructors.
   *
   * @param pattern A pattern string such as "#,##0.00" conforming to <a
   *     href="http://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns">UTS
   *     #35</a>.
   * @param symbols The set of symbols to be used.
   * @exception IllegalArgumentException if the given pattern is invalid
   * @see NumberFormat#getInstance
   * @see NumberFormat#getNumberInstance
   * @see NumberFormat#getCurrencyInstance
   * @see NumberFormat#getPercentInstance
   * @see DecimalFormatSymbols
   * @stable ICU 2.0
   */
  public DecimalFormat(String pattern, DecimalFormatSymbols symbols) {
    this.symbols = (DecimalFormatSymbols) symbols.clone();
    properties = new DecimalFormatProperties();
    exportedProperties = new DecimalFormatProperties();
    // Regression: ignore pattern rounding information if the pattern has currency symbols.
    setPropertiesFromPattern(pattern, PatternStringParser.IGNORE_ROUNDING_IF_CURRENCY);
    refreshFormatter();
  }

  /**
   * Creates a DecimalFormat based on the given pattern and symbols, with additional control over
   * the behavior of currency. The style argument determines whether currency rounding rules should
   * override the pattern, and the {@link CurrencyPluralInfo} object is used for customizing the
   * plural forms used for currency long names.
   *
   * <p>Most users should call the factory methods on NumberFormat, such as {@link
   * NumberFormat#getNumberInstance}, which return localized formatter objects, instead of the
   * DecimalFormat constructors.
   *
   * @param pattern a non-localized pattern string
   * @param symbols the set of symbols to be used
   * @param infoInput the information used for currency plural format, including currency plural
   *     patterns and plural rules.
   * @param style the decimal formatting style, it is one of the following values:
   *     NumberFormat.NUMBERSTYLE; NumberFormat.CURRENCYSTYLE; NumberFormat.PERCENTSTYLE;
   *     NumberFormat.SCIENTIFICSTYLE; NumberFormat.INTEGERSTYLE; NumberFormat.ISOCURRENCYSTYLE;
   *     NumberFormat.PLURALCURRENCYSTYLE;
   * @stable ICU 4.2
   */
  public DecimalFormat(
      String pattern, DecimalFormatSymbols symbols, CurrencyPluralInfo infoInput, int style) {
    this(pattern, symbols, style);
    properties.setCurrencyPluralInfo(infoInput);
    refreshFormatter();
  }

  /** Package-private constructor used by NumberFormat. */
  DecimalFormat(String pattern, DecimalFormatSymbols symbols, int choice) {
    this.symbols = (DecimalFormatSymbols) symbols.clone();
    properties = new DecimalFormatProperties();
    exportedProperties = new DecimalFormatProperties();
    // If choice is a currency type, ignore the rounding information.
    if (choice == CURRENCYSTYLE
        || choice == ISOCURRENCYSTYLE
        || choice == ACCOUNTINGCURRENCYSTYLE
        || choice == CASHCURRENCYSTYLE
        || choice == STANDARDCURRENCYSTYLE
        || choice == PLURALCURRENCYSTYLE) {
      setPropertiesFromPattern(pattern, PatternStringParser.IGNORE_ROUNDING_ALWAYS);
    } else {
      setPropertiesFromPattern(pattern, PatternStringParser.IGNORE_ROUNDING_IF_CURRENCY);
    }
    refreshFormatter();
  }

  private static DecimalFormatSymbols getDefaultSymbols() {
    return DecimalFormatSymbols.getInstance();
  }

  /**
   * Parses the given pattern string and overwrites the settings specified in the pattern string.
   * The properties corresponding to the following setters are overwritten, either with their
   * default values or with the value specified in the pattern string:
   *
   * <ol>
   *   <li>{@link #setDecimalSeparatorAlwaysShown}
   *   <li>{@link #setExponentSignAlwaysShown}
   *   <li>{@link #setFormatWidth}
   *   <li>{@link #setGroupingSize}
   *   <li>{@link #setMultiplier} (percent/permille)
   *   <li>{@link #setMaximumFractionDigits}
   *   <li>{@link #setMaximumIntegerDigits}
   *   <li>{@link #setMaximumSignificantDigits}
   *   <li>{@link #setMinimumExponentDigits}
   *   <li>{@link #setMinimumFractionDigits}
   *   <li>{@link #setMinimumIntegerDigits}
   *   <li>{@link #setMinimumSignificantDigits}
   *   <li>{@link #setPadPosition}
   *   <li>{@link #setPadCharacter}
   *   <li>{@link #setRoundingIncrement}
   *   <li>{@link #setSecondaryGroupingSize}
   * </ol>
   *
   * All other settings remain untouched.
   *
   * <p>For more information on pattern strings, see <a
   * href="http://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns">UTS #35</a>.
   *
   * @stable ICU 2.0
   */
  public synchronized void applyPattern(String pattern) {
    setPropertiesFromPattern(pattern, PatternStringParser.IGNORE_ROUNDING_NEVER);
    // Backwards compatibility: clear out user-specified prefix and suffix,
    // as well as CurrencyPluralInfo.
    properties.setPositivePrefix(null);
    properties.setNegativePrefix(null);
    properties.setPositiveSuffix(null);
    properties.setNegativeSuffix(null);
    properties.setCurrencyPluralInfo(null);
    refreshFormatter();
  }

  /**
   * Converts the given string to standard notation and then parses it using {@link #applyPattern}.
   * This method is provided for backwards compatibility and should not be used in new projects.
   *
   * <p>Localized notation means that instead of using generic placeholders in the pattern, you use
   * the corresponding locale-specific characters instead. For example, in locale <em>fr-FR</em>,
   * the period in the pattern "0.000" means "decimal" in standard notation (as it does in every
   * other locale), but it means "grouping" in localized notation.
   *
   * @param localizedPattern The pattern string in localized notation.
   * @stable ICU 2.0
   */
  public synchronized void applyLocalizedPattern(String localizedPattern) {
    String pattern = PatternStringUtils.convertLocalized(localizedPattern, symbols, false);
    applyPattern(pattern);
  }

  //=====================================================================================//
  //                                CLONE AND SERIALIZE                                  //
  //=====================================================================================//

  /** @stable ICU 2.0 */
  @Override
  public Object clone() {
    DecimalFormat other = (DecimalFormat) super.clone();
    other.symbols = (DecimalFormatSymbols) symbols.clone();
    other.properties = properties.clone();
    other.exportedProperties = new DecimalFormatProperties();
    other.refreshFormatter();
    return other;
  }

  /**
   * Custom serialization: save property bag and symbols; the formatter object can be re-created
   * from just that amount of information.
   */
  private synchronized void writeObject(ObjectOutputStream oos) throws IOException {
    // ICU 59 custom serialization.
    // Write class metadata and serialVersionOnStream field:
    oos.defaultWriteObject();
    // Extra int for possible future use:
    oos.writeInt(0);
    // 1) Property Bag
    oos.writeObject(properties);
    // 2) DecimalFormatSymbols
    oos.writeObject(symbols);
  }

  /**
   * Custom serialization: re-create object from serialized property bag and symbols. Also supports
   * reading from the legacy (pre-ICU4J 59) format and converting it to the new form.
   */
  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ObjectInputStream.GetField fieldGetter = ois.readFields();
    ObjectStreamField[] serializedFields = fieldGetter.getObjectStreamClass().getFields();
    int serialVersion = fieldGetter.get("serialVersionOnStream", -1);

    if (serialVersion > 5) {
      throw new IOException(
          "Cannot deserialize newer com.ibm.icu.text.DecimalFormat (v" + serialVersion + ")");
    } else if (serialVersion == 5) {
      ///// ICU 59+ SERIALIZATION FORMAT /////
      // We expect this field and no other fields:
      if (serializedFields.length > 1) {
        throw new IOException("Too many fields when reading serial version 5");
      }
      // Extra int for possible future use:
      ois.readInt();
      // 1) Property Bag
      Object serializedProperties = ois.readObject();
      if (serializedProperties instanceof DecimalFormatProperties) {
        // ICU 60+
        properties = (DecimalFormatProperties) serializedProperties;
      } else {
        // ICU 59
        properties = ((com.ibm.icu.impl.number.Properties) serializedProperties).getInstance();
      }
      // 2) DecimalFormatSymbols
      symbols = (DecimalFormatSymbols) ois.readObject();
      // Re-build transient fields
      exportedProperties = new DecimalFormatProperties();
      refreshFormatter();
    } else {
      ///// LEGACY SERIALIZATION FORMAT /////
      properties = new DecimalFormatProperties();
      // Loop through the fields. Not all fields necessarily exist in the serialization.
      String pp = null, ppp = null, ps = null, psp = null;
      String np = null, npp = null, ns = null, nsp = null;
      for (ObjectStreamField field : serializedFields) {
        String name = field.getName();
        if (name.equals("decimalSeparatorAlwaysShown")) {
          setDecimalSeparatorAlwaysShown(fieldGetter.get("decimalSeparatorAlwaysShown", false));
        } else if (name.equals("exponentSignAlwaysShown")) {
          setExponentSignAlwaysShown(fieldGetter.get("exponentSignAlwaysShown", false));
        } else if (name.equals("formatWidth")) {
          setFormatWidth(fieldGetter.get("formatWidth", 0));
        } else if (name.equals("groupingSize")) {
          setGroupingSize(fieldGetter.get("groupingSize", (byte) 3));
        } else if (name.equals("groupingSize2")) {
          setSecondaryGroupingSize(fieldGetter.get("groupingSize2", (byte) 0));
        } else if (name.equals("maxSignificantDigits")) {
          setMaximumSignificantDigits(fieldGetter.get("maxSignificantDigits", 6));
        } else if (name.equals("minExponentDigits")) {
          setMinimumExponentDigits(fieldGetter.get("minExponentDigits", (byte) 0));
        } else if (name.equals("minSignificantDigits")) {
          setMinimumSignificantDigits(fieldGetter.get("minSignificantDigits", 1));
        } else if (name.equals("multiplier")) {
          setMultiplier(fieldGetter.get("multiplier", 1));
        } else if (name.equals("pad")) {
          setPadCharacter(fieldGetter.get("pad", '\u0020'));
        } else if (name.equals("padPosition")) {
          setPadPosition(fieldGetter.get("padPosition", 0));
        } else if (name.equals("parseBigDecimal")) {
          setParseBigDecimal(fieldGetter.get("parseBigDecimal", false));
        } else if (name.equals("parseRequireDecimalPoint")) {
          setDecimalPatternMatchRequired(fieldGetter.get("parseRequireDecimalPoint", false));
        } else if (name.equals("roundingMode")) {
          setRoundingMode(fieldGetter.get("roundingMode", 0));
        } else if (name.equals("useExponentialNotation")) {
          setScientificNotation(fieldGetter.get("useExponentialNotation", false));
        } else if (name.equals("useSignificantDigits")) {
          setSignificantDigitsUsed(fieldGetter.get("useSignificantDigits", false));
        } else if (name.equals("currencyPluralInfo")) {
          setCurrencyPluralInfo((CurrencyPluralInfo) fieldGetter.get("currencyPluralInfo", null));
        } else if (name.equals("mathContext")) {
          setMathContextICU((MathContext) fieldGetter.get("mathContext", null));
        } else if (name.equals("negPrefixPattern")) {
          npp = (String) fieldGetter.get("negPrefixPattern", null);
        } else if (name.equals("negSuffixPattern")) {
          nsp = (String) fieldGetter.get("negSuffixPattern", null);
        } else if (name.equals("negativePrefix")) {
          np = (String) fieldGetter.get("negativePrefix", null);
        } else if (name.equals("negativeSuffix")) {
          ns = (String) fieldGetter.get("negativeSuffix", null);
        } else if (name.equals("posPrefixPattern")) {
          ppp = (String) fieldGetter.get("posPrefixPattern", null);
        } else if (name.equals("posSuffixPattern")) {
          psp = (String) fieldGetter.get("posSuffixPattern", null);
        } else if (name.equals("positivePrefix")) {
          pp = (String) fieldGetter.get("positivePrefix", null);
        } else if (name.equals("positiveSuffix")) {
          ps = (String) fieldGetter.get("positiveSuffix", null);
        } else if (name.equals("roundingIncrement")) {
          setRoundingIncrement((java.math.BigDecimal) fieldGetter.get("roundingIncrement", null));
        } else if (name.equals("symbols")) {
          setDecimalFormatSymbols((DecimalFormatSymbols) fieldGetter.get("symbols", null));
        } else {
          // The following fields are ignored:
          // "PARSE_MAX_EXPONENT"
          // "currencySignCount"
          // "style"
          // "attributes"
          // "currencyChoice"
          // "formatPattern"
          // "currencyUsage" => ignore this because the old code puts currencyUsage directly into min/max fraction.
        }
      }
      // Resolve affixes
      if (npp == null) {
        properties.setNegativePrefix(np);
      } else {
        properties.setNegativePrefixPattern(npp);
      }
      if (nsp == null) {
        properties.setNegativeSuffix(ns);
      } else {
        properties.setNegativeSuffixPattern(nsp);
      }
      if (ppp == null) {
        properties.setPositivePrefix(pp);
      } else {
        properties.setPositivePrefixPattern(ppp);
      }
      if (psp == null) {
        properties.setPositiveSuffix(ps);
      } else {
        properties.setPositiveSuffixPattern(psp);
      }
      // Extract values from parent NumberFormat class.  Have to use reflection here.
      java.lang.reflect.Field getter;
      try {
        getter = NumberFormat.class.getDeclaredField("groupingUsed");
        getter.setAccessible(true);
        setGroupingUsed((Boolean) getter.get(this));
        getter = NumberFormat.class.getDeclaredField("parseIntegerOnly");
        getter.setAccessible(true);
        setParseIntegerOnly((Boolean) getter.get(this));
        getter = NumberFormat.class.getDeclaredField("maximumIntegerDigits");
        getter.setAccessible(true);
        setMaximumIntegerDigits((Integer) getter.get(this));
        getter = NumberFormat.class.getDeclaredField("minimumIntegerDigits");
        getter.setAccessible(true);
        setMinimumIntegerDigits((Integer) getter.get(this));
        getter = NumberFormat.class.getDeclaredField("maximumFractionDigits");
        getter.setAccessible(true);
        setMaximumFractionDigits((Integer) getter.get(this));
        getter = NumberFormat.class.getDeclaredField("minimumFractionDigits");
        getter.setAccessible(true);
        setMinimumFractionDigits((Integer) getter.get(this));
        getter = NumberFormat.class.getDeclaredField("currency");
        getter.setAccessible(true);
        setCurrency((Currency) getter.get(this));
        getter = NumberFormat.class.getDeclaredField("parseStrict");
        getter.setAccessible(true);
        setParseStrict((Boolean) getter.get(this));
      } catch (IllegalArgumentException e) {
        throw new IOException(e);
      } catch (IllegalAccessException e) {
        throw new IOException(e);
      } catch (NoSuchFieldException e) {
        throw new IOException(e);
      } catch (SecurityException e) {
        throw new IOException(e);
      }
      // Finish initialization
      if (symbols == null) {
        symbols = getDefaultSymbols();
      }
      exportedProperties = new DecimalFormatProperties();
      refreshFormatter();
    }
  }

  //=====================================================================================//
  //                               FORMAT AND PARSE APIS                                 //
  //=====================================================================================//

  /**
   * {@inheritDoc}
   *
   * @stable ICU 2.0
   */
  @Override
  public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
    DecimalQuantity dq = new DecimalQuantity_DualStorageBCD(number);
    FormattedStringBuilder string = new FormattedStringBuilder();
    formatter.formatImpl(dq, string);
    fieldPositionHelper(dq, string, fieldPosition, result.length());
    Utility.appendTo(string, result);
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 2.0
   */
  @Override
  public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
    DecimalQuantity dq = new DecimalQuantity_DualStorageBCD(number);
    FormattedStringBuilder string = new FormattedStringBuilder();
    formatter.formatImpl(dq, string);
    fieldPositionHelper(dq, string, fieldPosition, result.length());
    Utility.appendTo(string, result);
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 2.0
   */
  @Override
  public StringBuffer format(BigInteger number, StringBuffer result, FieldPosition fieldPosition) {
    DecimalQuantity dq = new DecimalQuantity_DualStorageBCD(number);
    FormattedStringBuilder string = new FormattedStringBuilder();
    formatter.formatImpl(dq, string);
    fieldPositionHelper(dq, string, fieldPosition, result.length());
    Utility.appendTo(string, result);
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 2.0
   */
  @Override
  public StringBuffer format(
      java.math.BigDecimal number, StringBuffer result, FieldPosition fieldPosition) {
    DecimalQuantity dq = new DecimalQuantity_DualStorageBCD(number);
    FormattedStringBuilder string = new FormattedStringBuilder();
    formatter.formatImpl(dq, string);
    fieldPositionHelper(dq, string, fieldPosition, result.length());
    Utility.appendTo(string, result);
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 2.0
   */
  @Override
  public StringBuffer format(BigDecimal number, StringBuffer result, FieldPosition fieldPosition) {
    DecimalQuantity dq = new DecimalQuantity_DualStorageBCD(number);
    FormattedStringBuilder string = new FormattedStringBuilder();
    formatter.formatImpl(dq, string);
    fieldPositionHelper(dq, string, fieldPosition, result.length());
    Utility.appendTo(string, result);
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 3.6
   */
  @Override
  public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
    if (!(obj instanceof Number)) throw new IllegalArgumentException();
    Number number = (Number) obj;
    FormattedNumber output = formatter.format(number);
    return output.toCharacterIterator();
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 3.0
   */
  @Override
  public StringBuffer format(CurrencyAmount currAmt, StringBuffer result, FieldPosition fieldPosition) {
    // We need to make localSymbols in order for monetary symbols to be initialized.
    // Also, bypass the CurrencyAmount override of LocalizedNumberFormatter#format,
    // because its caching mechanism will not provide any benefit here.
    DecimalFormatSymbols localSymbols = (DecimalFormatSymbols) symbols.clone();
    localSymbols.setCurrency(currAmt.getCurrency());

    DecimalQuantity dq = new DecimalQuantity_DualStorageBCD(currAmt.getNumber());
    FormattedStringBuilder string = new FormattedStringBuilder();
    formatter.symbols(localSymbols)
            .unit(currAmt.getCurrency())
            .formatImpl(dq, string);
    fieldPositionHelper(dq, string, fieldPosition, result.length());
    Utility.appendTo(string, result);
    return result;
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 2.0
   */
  @Override
  public Number parse(String text, ParsePosition parsePosition) {
      if (text == null) {
          throw new IllegalArgumentException("Text cannot be null");
      }
      if (parsePosition == null) {
          parsePosition = new ParsePosition(0);
      }
      if (parsePosition.getIndex() < 0) {
          throw new IllegalArgumentException("Cannot start parsing at a negative offset");
      }
      if (parsePosition.getIndex() >= text.length()) {
          // For backwards compatibility, this is not an exception, just an empty result.
          return null;
      }

      ParsedNumber result = new ParsedNumber();
      // Note: if this is a currency instance, currencies will be matched despite the fact that we are not in the
      // parseCurrency method (backwards compatibility)
      int startIndex = parsePosition.getIndex();
      NumberParserImpl parser = getParser();
      parser.parse(text, startIndex, true, result);
      if (result.success()) {
          parsePosition.setIndex(result.charEnd);
          // TODO: Accessing properties here is technically not thread-safe
          Number number = result.getNumber(parser.getParseFlags());
          // Backwards compatibility: return com.ibm.icu.math.BigDecimal
          if (number instanceof java.math.BigDecimal) {
              number = safeConvertBigDecimal((java.math.BigDecimal) number);
          }
          return number;
      } else {
          parsePosition.setErrorIndex(startIndex + result.charEnd);
          return null;
      }
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 49
   */
  @Override
  public CurrencyAmount parseCurrency(CharSequence text, ParsePosition parsePosition) {
      if (text == null) {
          throw new IllegalArgumentException("Text cannot be null");
      }
      if (parsePosition == null) {
          parsePosition = new ParsePosition(0);
      }
      if (parsePosition.getIndex() < 0) {
          throw new IllegalArgumentException("Cannot start parsing at a negative offset");
      }
      if (parsePosition.getIndex() >= text.length()) {
          // For backwards compatibility, this is not an exception, just an empty result.
          return null;
      }

      ParsedNumber result = new ParsedNumber();
      int startIndex = parsePosition.getIndex();
      NumberParserImpl parser = getCurrencyParser();
      parser.parse(text.toString(), startIndex, true, result);
      if (result.success()) {
          parsePosition.setIndex(result.charEnd);
          // TODO: Accessing properties here is technically not thread-safe
          Number number = result.getNumber(parser.getParseFlags());
          // Backwards compatibility: return com.ibm.icu.math.BigDecimal
          if (number instanceof java.math.BigDecimal) {
              number = safeConvertBigDecimal((java.math.BigDecimal) number);
          }
          Currency currency = Currency.getInstance(result.currencyCode);
          return new CurrencyAmount(number, currency);
      } else {
          parsePosition.setErrorIndex(startIndex + result.charEnd);
          return null;
      }
  }

  //=====================================================================================//
  //                                GETTERS AND SETTERS                                  //
  //=====================================================================================//

  /**
   * Returns a copy of the decimal format symbols used by this formatter.
   *
   * @return desired DecimalFormatSymbols
   * @see DecimalFormatSymbols
   * @stable ICU 2.0
   */
  public synchronized DecimalFormatSymbols getDecimalFormatSymbols() {
    return (DecimalFormatSymbols) symbols.clone();
  }

  /**
   * Sets the decimal format symbols used by this formatter. The formatter uses a copy of the
   * provided symbols.
   *
   * @param newSymbols desired DecimalFormatSymbols
   * @see DecimalFormatSymbols
   * @stable ICU 2.0
   */
  public synchronized void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
    symbols = (DecimalFormatSymbols) newSymbols.clone();
    refreshFormatter();
  }

  /**
   * <strong>Affixes:</strong> Gets the positive prefix string currently being used to format
   * numbers.
   *
   * <p>If the affix was specified via the pattern, the string returned by this method will have
   * locale symbols substituted in place of special characters according to the LDML specification.
   * If the affix was specified via {@link #setPositivePrefix}, the string will be returned
   * literally.
   *
   * @return The string being prepended to positive numbers.
   * @category Affixes
   * @stable ICU 2.0
   */
  public synchronized String getPositivePrefix() {
    return formatter.getAffixImpl(true, false);
  }

  /**
   * <strong>Affixes:</strong> Sets the string to prepend to positive numbers. For example, if you
   * set the value "#", then the number 123 will be formatted as "#123" in the locale
   * <em>en-US</em>.
   *
   * <p>Using this method overrides the affix specified via the pattern, and unlike the pattern, the
   * string given to this method will be interpreted literally WITHOUT locale symbol substitutions.
   *
   * @param prefix The literal string to prepend to positive numbers.
   * @category Affixes
   * @stable ICU 2.0
   */
  public synchronized void setPositivePrefix(String prefix) {
    if (prefix == null) {
      throw new NullPointerException();
    }
    properties.setPositivePrefix(prefix);
    refreshFormatter();
  }

  /**
   * <strong>Affixes:</strong> Gets the negative prefix string currently being used to format
   * numbers.
   *
   * <p>If the affix was specified via the pattern, the string returned by this method will have
   * locale symbols substituted in place of special characters according to the LDML specification.
   * If the affix was specified via {@link #setNegativePrefix}, the string will be returned
   * literally.
   *
   * @return The string being prepended to negative numbers.
   * @category Affixes
   * @stable ICU 2.0
   */
  public synchronized String getNegativePrefix() {
    return formatter.getAffixImpl(true, true);
  }

  /**
   * <strong>Affixes:</strong> Sets the string to prepend to negative numbers. For example, if you
   * set the value "#", then the number -123 will be formatted as "#123" in the locale
   * <em>en-US</em> (overriding the implicit default '-' in the pattern).
   *
   * <p>Using this method overrides the affix specified via the pattern, and unlike the pattern, the
   * string given to this method will be interpreted literally WITHOUT locale symbol substitutions.
   *
   * @param prefix The literal string to prepend to negative numbers.
   * @category Affixes
   * @stable ICU 2.0
   */
  public synchronized void setNegativePrefix(String prefix) {
    if (prefix == null) {
      throw new NullPointerException();
    }
    properties.setNegativePrefix(prefix);
    refreshFormatter();
  }

  /**
   * <strong>Affixes:</strong> Gets the positive suffix string currently being used to format
   * numbers.
   *
   * <p>If the affix was specified via the pattern, the string returned by this method will have
   * locale symbols substituted in place of special characters according to the LDML specification.
   * If the affix was specified via {@link #setPositiveSuffix}, the string will be returned
   * literally.
   *
   * @return The string being appended to positive numbers.
   * @category Affixes
   * @stable ICU 2.0
   */
  public synchronized String getPositiveSuffix() {
    return formatter.getAffixImpl(false, false);
  }

  /**
   * <strong>Affixes:</strong> Sets the string to append to positive numbers. For example, if you
   * set the value "#", then the number 123 will be formatted as "123#" in the locale
   * <em>en-US</em>.
   *
   * <p>Using this method overrides the affix specified via the pattern, and unlike the pattern, the
   * string given to this method will be interpreted literally WITHOUT locale symbol substitutions.
   *
   * @param suffix The literal string to append to positive numbers.
   * @category Affixes
   * @stable ICU 2.0
   */
  public synchronized void setPositiveSuffix(String suffix) {
    if (suffix == null) {
      throw new NullPointerException();
    }
    properties.setPositiveSuffix(suffix);
    refreshFormatter();
  }

  /**
   * <strong>Affixes:</strong> Gets the negative suffix string currently being used to format
   * numbers.
   *
   * <p>If the affix was specified via the pattern, the string returned by this method will have
   * locale symbols substituted in place of special characters according to the LDML specification.
   * If the affix was specified via {@link #setNegativeSuffix}, the string will be returned
   * literally.
   *
   * @return The string being appended to negative numbers.
   * @category Affixes
   * @stable ICU 2.0
   */
  public synchronized String getNegativeSuffix() {
    return formatter.getAffixImpl(false, true);
  }

  /**
   * <strong>Affixes:</strong> Sets the string to append to negative numbers. For example, if you
   * set the value "#", then the number 123 will be formatted as "123#" in the locale
   * <em>en-US</em>.
   *
   * <p>Using this method overrides the affix specified via the pattern, and unlike the pattern, the
   * string given to this method will be interpreted literally WITHOUT locale symbol substitutions.
   *
   * @param suffix The literal string to append to negative numbers.
   * @category Affixes
   * @stable ICU 2.0
   */
  public synchronized void setNegativeSuffix(String suffix) {
    if (suffix == null) {
      throw new NullPointerException();
    }
    properties.setNegativeSuffix(suffix);
    refreshFormatter();
  }

  /**
   * {@icu} Returns whether the sign is being shown on positive numbers.
   *
   * @return Whether the sign is shown on positive numbers and zero.
   * @see #setSignAlwaysShown
   * @category Affixes
   * @stable ICU 64
   */
  public synchronized boolean isSignAlwaysShown() {
    // This is not in the exported properties
    return properties.getSignAlwaysShown();
  }

  /**
   * Sets whether to always shown the plus sign ('+' in <em>en</em>) on positive numbers. The rules
   * in UTS #35 section 3.2.1 will be followed to ensure a locale-aware placement of the sign.
   *
   * <p>More specifically, the following strategy will be used to place the plus sign:
   *
   * <ol>
   *   <li><em>Patterns without a negative subpattern:</em> The locale's plus sign will be prepended
   *       to the positive prefix.
   *   <li><em>Patterns with a negative subpattern without a '-' sign (e.g., accounting):</em> The
   *       locale's plus sign will be prepended to the positive prefix, as in case 1.
   *   <li><em>Patterns with a negative subpattern that has a '-' sign:</em> The locale's plus sign
   *       will substitute the '-' in the negative subpattern. The positive subpattern will be
   *       unused.
   * </ol>
   *
   * This method is designed to be used <em>instead of</em> applying a pattern containing an
   * explicit plus sign, such as "+0;-0". The behavior when combining this method with explicit plus
   * signs in the pattern is undefined.
   *
   * @param value true to always show a sign; false to hide the sign on positive numbers and zero.
   * @category Affixes
   * @stable ICU 64
   */
  public synchronized void setSignAlwaysShown(boolean value) {
    properties.setSignAlwaysShown(value);
    refreshFormatter();
  }

  /**
   * Returns the multiplier being applied to numbers before they are formatted.
   *
   * @see #setMultiplier
   * @category Multipliers
   * @stable ICU 2.0
   */
  public synchronized int getMultiplier() {
    if (properties.getMultiplier() != null) {
      return properties.getMultiplier().intValue();
    } else {
      return (int) Math.pow(10, properties.getMagnitudeMultiplier());
    }
  }

  /**
   * Sets a number that will be used to multiply all numbers prior to formatting. For example, when
   * formatting percents, a multiplier of 100 can be used.
   *
   * <p>If a percent or permille sign is specified in the pattern, the multiplier is automatically
   * set to 100 or 1000, respectively.
   *
   * <p>If the number specified here is a power of 10, a more efficient code path will be used.
   *
   * @param multiplier The number by which all numbers passed to {@link #format} will be multiplied.
   * @throws IllegalArgumentException If the given multiplier is zero.
   * @throws ArithmeticException when inverting multiplier produces a non-terminating decimal result
   *         in conjunction with MathContext of unlimited precision.
   * @category Multipliers
   * @stable ICU 2.0
   */
  public synchronized void setMultiplier(int multiplier) {
    if (multiplier == 0) {
      throw new IllegalArgumentException("Multiplier must be nonzero.");
    }

    // Try to convert to a magnitude multiplier first
    int delta = 0;
    int value = multiplier;
    while (value != 1) {
      delta++;
      int temp = value / 10;
      if (temp * 10 != value) {
        delta = -1;
        break;
      }
      value = temp;
    }
    if (delta != -1) {
      properties.setMagnitudeMultiplier(delta);
      properties.setMultiplier(null);
    } else {
      properties.setMagnitudeMultiplier(0);
      properties.setMultiplier(java.math.BigDecimal.valueOf(multiplier));
    }
    refreshFormatter();
  }

  /**
   * {@icu} Returns the increment to which numbers are being rounded.
   *
   * @see #setRoundingIncrement
   * @category Rounding
   * @stable ICU 2.0
   */
  public synchronized java.math.BigDecimal getRoundingIncrement() {
    return exportedProperties.getRoundingIncrement();
  }

  /**
   * {@icu} <strong>Rounding and Digit Limits:</strong> Sets an increment, or interval, to which
   * numbers are rounded. For example, a rounding increment of 0.05 will cause the number 1.23 to be
   * rounded to 1.25 in the default rounding mode.
   *
   * <p>The rounding increment can be specified via the pattern string: for example, the pattern
   * "#,##0.05" encodes a rounding increment of 0.05.
   *
   * <p>The rounding increment is applied <em>after</em> any multipliers might take effect; for
   * example, in scientific notation or when {@link #setMultiplier} is used.
   *
   * <p>See {@link #setMaximumFractionDigits} and {@link #setMaximumSignificantDigits} for two other
   * ways of specifying rounding strategies.
   *
   * @param increment The increment to which numbers are to be rounded.
   * @see #setRoundingMode
   * @see #setMaximumFractionDigits
   * @see #setMaximumSignificantDigits
   * @category Rounding
   * @stable ICU 2.0
   */
  public synchronized void setRoundingIncrement(java.math.BigDecimal increment) {
    // Backwards compatibility: ignore rounding increment if zero,
    // and instead set maximum fraction digits.
    if (increment != null && increment.compareTo(java.math.BigDecimal.ZERO) == 0) {
      properties.setMaximumFractionDigits(Integer.MAX_VALUE);
      return;
    }

    properties.setRoundingIncrement(increment);
    refreshFormatter();
  }

  /**
   * {@icu} <strong>Rounding and Digit Limits:</strong> Overload of {@link
   * #setRoundingIncrement(java.math.BigDecimal)}.
   *
   * @param increment The increment to which numbers are to be rounded.
   * @see #setRoundingIncrement
   * @category Rounding
   * @stable ICU 3.6
   */
  public synchronized void setRoundingIncrement(BigDecimal increment) {
    java.math.BigDecimal javaBigDecimal = (increment == null) ? null : increment.toBigDecimal();
    setRoundingIncrement(javaBigDecimal);
  }

  /**
   * {@icu} <strong>Rounding and Digit Limits:</strong> Overload of {@link
   * #setRoundingIncrement(java.math.BigDecimal)}.
   *
   * @param increment The increment to which numbers are to be rounded.
   * @see #setRoundingIncrement
   * @category Rounding
   * @stable ICU 2.0
   */
  public synchronized void setRoundingIncrement(double increment) {
    if (increment == 0) {
      setRoundingIncrement((java.math.BigDecimal) null);
    } else {
      // ICU-20425: Since doubles have no concept of trailing zeros, we should strip
      // trailing zeros from the BigDecimal.
      java.math.BigDecimal javaBigDecimal = java.math.BigDecimal.valueOf(increment)
        .stripTrailingZeros();
      setRoundingIncrement(javaBigDecimal);
    }
  }

  /**
   * Returns the rounding mode being used to round numbers.
   *
   * @see #setRoundingMode
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized int getRoundingMode() {
    RoundingMode mode = exportedProperties.getRoundingMode();
    return (mode == null) ? 0 : mode.ordinal();
  }

  /**
   * <strong>Rounding and Digit Limits:</strong> Sets the {@link RoundingMode} used to round
   * numbers. The default rounding mode is HALF_EVEN, which rounds decimals to their closest whole
   * number, and rounds to the closest even number if at the midpoint.
   *
   * <p>For more detail on rounding modes, see <a
   * href="https://unicode-org.github.io/icu/userguide/format_parse/numbers/rounding-modes">the ICU
   * User Guide</a>.
   *
   * <p>For backwards compatibility, the rounding mode is specified as an int argument, which can be
   * from either the constants in {@link BigDecimal} or the ordinal value of {@link RoundingMode}.
   * The following two calls are functionally equivalent.
   *
   * <pre>
   * df.setRoundingMode(BigDecimal.ROUND_CEILING);
   * df.setRoundingMode(RoundingMode.CEILING.ordinal());
   * </pre>
   *
   * @param roundingMode The integer constant rounding mode to use when formatting numbers.
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized void setRoundingMode(int roundingMode) {
    properties.setRoundingMode(RoundingMode.valueOf(roundingMode));
    refreshFormatter();
  }

  /**
   * {@icu} Returns the {@link java.math.MathContext} being used to round numbers.
   *
   * @see #setMathContext
   * @category Rounding
   * @stable ICU 4.2
   */
  public synchronized java.math.MathContext getMathContext() {
    java.math.MathContext mathContext = exportedProperties.getMathContext();
    assert mathContext != null;
    return mathContext;
  }

  /**
   * {@icu} <strong>Rounding and Digit Limits:</strong> Sets the {@link java.math.MathContext} used
   * to round numbers. A "math context" encodes both a rounding mode and a number of significant
   * digits. Most users should call {@link #setRoundingMode} and/or {@link
   * #setMaximumSignificantDigits} instead of this method.
   *
   * <p>When formatting, since no division is ever performed, the default MathContext is unlimited
   * significant digits. However, when division occurs during parsing to correct for percentages and
   * multipliers, a MathContext of 34 digits, the IEEE 754R Decimal128 standard, is used by default.
   * If you require more than 34 digits when parsing, you can set a custom MathContext using this
   * method.
   *
   * @param mathContext The MathContext to use when rounding numbers.
   * @throws ArithmeticException when inverting multiplier produces a non-terminating decimal result
   *         in conjunction with MathContext of unlimited precision.
   * @see java.math.MathContext
   * @category Rounding
   * @stable ICU 4.2
   */
  public synchronized void setMathContext(java.math.MathContext mathContext) {
    properties.setMathContext(mathContext);
    refreshFormatter();
  }

  // Remember the ICU math context form in order to be able to return it from the API.
  // NOTE: This value is not serialized. (should it be?)
  private transient int icuMathContextForm = MathContext.PLAIN;

  /**
   * {@icu} Returns the {@link com.ibm.icu.math.MathContext} being used to round numbers.
   *
   * @see #setMathContext
   * @category Rounding
   * @stable ICU 4.2
   */
  public synchronized MathContext getMathContextICU() {
    java.math.MathContext mathContext = getMathContext();
    return new MathContext(
        mathContext.getPrecision(),
        icuMathContextForm,
        false,
        mathContext.getRoundingMode().ordinal());
  }

  /**
   * {@icu} <strong>Rounding and Digit Limits:</strong> Overload of {@link #setMathContext} for
   * {@link com.ibm.icu.math.MathContext}.
   *
   * @param mathContextICU The MathContext to use when rounding numbers.
   * @throws ArithmeticException when inverting multiplier produces a non-terminating decimal result
   *         in conjunction with MathContext of unlimited precision.
   * @see #setMathContext(java.math.MathContext)
   * @category Rounding
   * @stable ICU 4.2
   */
  public synchronized void setMathContextICU(MathContext mathContextICU) {
    icuMathContextForm = mathContextICU.getForm();
    java.math.MathContext mathContext;
    if (mathContextICU.getLostDigits()) {
      // The getLostDigits() feature in ICU MathContext means "throw an ArithmeticException if
      // rounding causes digits to be lost". That feature is called RoundingMode.UNNECESSARY in
      // Java MathContext.
      mathContext = new java.math.MathContext(mathContextICU.getDigits(), RoundingMode.UNNECESSARY);
    } else {
      mathContext =
          new java.math.MathContext(
              mathContextICU.getDigits(), RoundingMode.valueOf(mathContextICU.getRoundingMode()));
    }
    setMathContext(mathContext);
  }

  /**
   * Returns the effective minimum number of digits before the decimal separator.
   *
   * @see #setMinimumIntegerDigits
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized int getMinimumIntegerDigits() {
    return exportedProperties.getMinimumIntegerDigits();
  }

  /**
   * <strong>Rounding and Digit Limits:</strong> Sets the minimum number of digits to display before
   * the decimal separator. If the number has fewer than this many digits, the number is padded with
   * zeros.
   *
   * <p>For example, if minimum integer digits is 3, the number 12.3 will be printed as "001.23".
   *
   * <p>Minimum integer and minimum and maximum fraction digits can be specified via the pattern
   * string. For example, "#,#00.00#" has 2 minimum integer digits, 2 minimum fraction digits, and 3
   * maximum fraction digits. Note that it is not possible to specify maximum integer digits in the
   * pattern except in scientific notation.
   *
   * <p>If minimum and maximum integer, fraction, or significant digits conflict with each other,
   * the most recently specified value is used. For example, if there is a formatter with minInt=5,
   * and then you set maxInt=3, then minInt will be changed to 3.
   *
   * @param value The minimum number of digits before the decimal separator.
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized void setMinimumIntegerDigits(int value) {
    // For backwards compatibility, conflicting min/max need to keep the most recent setting.
    int max = properties.getMaximumIntegerDigits();
    if (max >= 0 && max < value) {
      properties.setMaximumIntegerDigits(value);
    }
    properties.setMinimumIntegerDigits(value);
    refreshFormatter();
  }

  /**
   * Returns the effective maximum number of digits before the decimal separator.
   *
   * @see #setMaximumIntegerDigits
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized int getMaximumIntegerDigits() {
    return exportedProperties.getMaximumIntegerDigits();
  }

  /**
   * <strong>Rounding and Digit Limits:</strong> Sets the maximum number of digits to display before
   * the decimal separator. If the number has more than this many digits, the number is truncated.
   *
   * <p>For example, if maximum integer digits is 3, the number 12345 will be printed as "345".
   *
   * <p>Minimum integer and minimum and maximum fraction digits can be specified via the pattern
   * string. For example, "#,#00.00#" has 2 minimum integer digits, 2 minimum fraction digits, and 3
   * maximum fraction digits. Note that it is not possible to specify maximum integer digits in the
   * pattern except in scientific notation.
   *
   * <p>If minimum and maximum integer, fraction, or significant digits conflict with each other,
   * the most recently specified value is used. For example, if there is a formatter with minInt=5,
   * and then you set maxInt=3, then minInt will be changed to 3.
   *
   * @param value The maximum number of digits before the decimal separator.
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized void setMaximumIntegerDigits(int value) {
    int min = properties.getMinimumIntegerDigits();
    if (min >= 0 && min > value) {
      properties.setMinimumIntegerDigits(value);
    }
    properties.setMaximumIntegerDigits(value);
    refreshFormatter();
  }

  /**
   * Returns the effective minimum number of integer digits after the decimal separator.
   *
   * @see #setMaximumIntegerDigits
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized int getMinimumFractionDigits() {
    return exportedProperties.getMinimumFractionDigits();
  }

  /**
   * <strong>Rounding and Digit Limits:</strong> Sets the minimum number of digits to display after
   * the decimal separator. If the number has fewer than this many digits, the number is padded with
   * zeros.
   *
   * <p>For example, if minimum fraction digits is 2, the number 123.4 will be printed as "123.40".
   *
   * <p>Minimum integer and minimum and maximum fraction digits can be specified via the pattern
   * string. For example, "#,#00.00#" has 2 minimum integer digits, 2 minimum fraction digits, and 3
   * maximum fraction digits. Note that it is not possible to specify maximum integer digits in the
   * pattern except in scientific notation.
   *
   * <p>If minimum and maximum integer, fraction, or significant digits conflict with each other,
   * the most recently specified value is used. For example, if there is a formatter with minInt=5,
   * and then you set maxInt=3, then minInt will be changed to 3.
   *
   * <p>See {@link #setRoundingIncrement} and {@link #setMaximumSignificantDigits} for two other
   * ways of specifying rounding strategies.
   *
   * @param value The minimum number of integer digits after the decimal separator.
   * @see #setRoundingMode
   * @see #setRoundingIncrement
   * @see #setMaximumSignificantDigits
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized void setMinimumFractionDigits(int value) {
    int max = properties.getMaximumFractionDigits();
    if (max >= 0 && max < value) {
      properties.setMaximumFractionDigits(value);
    }
    properties.setMinimumFractionDigits(value);
    refreshFormatter();
  }

  /**
   * Returns the effective maximum number of integer digits after the decimal separator.
   *
   * @see #setMaximumIntegerDigits
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized int getMaximumFractionDigits() {
    return exportedProperties.getMaximumFractionDigits();
  }

  /**
   * <strong>Rounding and Digit Limits:</strong> Sets the maximum number of digits to display after
   * the decimal separator. If the number has more than this many digits, the number is rounded
   * according to the rounding mode.
   *
   * <p>For example, if maximum fraction digits is 2, the number 123.456 will be printed as
   * "123.46".
   *
   * <p>Minimum integer and minimum and maximum fraction digits can be specified via the pattern
   * string. For example, "#,#00.00#" has 2 minimum integer digits, 2 minimum fraction digits, and 3
   * maximum fraction digits. Note that it is not possible to specify maximum integer digits in the
   * pattern except in scientific notation.
   *
   * <p>If minimum and maximum integer, fraction, or significant digits conflict with each other,
   * the most recently specified value is used. For example, if there is a formatter with minInt=5,
   * and then you set maxInt=3, then minInt will be changed to 3.
   *
   * @param value The maximum number of integer digits after the decimal separator.
   * @see #setRoundingMode
   * @category Rounding
   * @stable ICU 2.0
   */
  @Override
  public synchronized void setMaximumFractionDigits(int value) {
    int min = properties.getMinimumFractionDigits();
    if (min >= 0 && min > value) {
      properties.setMinimumFractionDigits(value);
    }
    properties.setMaximumFractionDigits(value);
    refreshFormatter();
  }

  /**
   * {@icu} Returns whether significant digits are being used in rounding.
   *
   * @see #setSignificantDigitsUsed
   * @category Rounding
   * @stable ICU 3.0
   */
  public synchronized boolean areSignificantDigitsUsed() {
    return properties.getMinimumSignificantDigits() != -1
        || properties.getMaximumSignificantDigits() != -1;
  }

  /**
   * {@icu} <strong>Rounding and Digit Limits:</strong> Sets whether significant digits are to be
   * used in rounding.
   *
   * <p>Calling <code>df.setSignificantDigitsUsed(true)</code> is functionally equivalent to:
   *
   * <pre>
   * df.setMinimumSignificantDigits(1);
   * df.setMaximumSignificantDigits(6);
   * </pre>
   *
   * @param useSignificantDigits true to enable significant digit rounding; false to disable it.
   * @category Rounding
   * @stable ICU 3.0
   */
  public synchronized void setSignificantDigitsUsed(boolean useSignificantDigits) {
    int oldMinSig = properties.getMinimumSignificantDigits();
    int oldMaxSig = properties.getMaximumSignificantDigits();
    // These are the default values from the old implementation.
    if (useSignificantDigits) {
      if (oldMinSig != -1 || oldMaxSig != -1) {
        return;
      }
    } else {
      if (oldMinSig == -1 && oldMaxSig == -1) {
        return;
      }
    }
    int minSig = useSignificantDigits ? 1 : -1;
    int maxSig = useSignificantDigits ? 6 : -1;
    properties.setMinimumSignificantDigits(minSig);
    properties.setMaximumSignificantDigits(maxSig);
    refreshFormatter();
  }

  /**
   * {@icu} Returns the effective minimum number of significant digits displayed.
   *
   * @see #setMinimumSignificantDigits
   * @category Rounding
   * @stable ICU 3.0
   */
  public synchronized int getMinimumSignificantDigits() {
    return exportedProperties.getMinimumSignificantDigits();
  }

  /**
   * {@icu} <strong>Rounding and Digit Limits:</strong> Sets the minimum number of significant
   * digits to be displayed. If the number of significant digits is less than this value, the number
   * will be padded with zeros as necessary.
   *
   * <p>For example, if minimum significant digits is 3 and the number is 1.2, the number will be
   * printed as "1.20".
   *
   * <p>If minimum and maximum integer, fraction, or significant digits conflict with each other,
   * the most recently specified value is used. For example, if there is a formatter with minInt=5,
   * and then you set maxInt=3, then minInt will be changed to 3.
   *
   * @param value The minimum number of significant digits to display.
   * @category Rounding
   * @stable ICU 3.0
   */
  public synchronized void setMinimumSignificantDigits(int value) {
    int max = properties.getMaximumSignificantDigits();
    if (max >= 0 && max < value) {
      properties.setMaximumSignificantDigits(value);
    }
    properties.setMinimumSignificantDigits(value);
    refreshFormatter();
  }

  /**
   * {@icu} Returns the effective maximum number of significant digits displayed.
   *
   * @see #setMaximumSignificantDigits
   * @category Rounding
   * @stable ICU 3.0
   */
  public synchronized int getMaximumSignificantDigits() {
    return exportedProperties.getMaximumSignificantDigits();
  }

  /**
   * {@icu} <strong>Rounding and Digit Limits:</strong> Sets the maximum number of significant
   * digits to be displayed. If the number of significant digits in the number exceeds this value,
   * the number will be rounded according to the current rounding mode.
   *
   * <p>For example, if maximum significant digits is 3 and the number is 12345, the number will be
   * printed as "12300".
   *
   * <p>If minimum and maximum integer, fraction, or significant digits conflict with each other,
   * the most recently specified value is used. For example, if there is a formatter with minInt=5,
   * and then you set maxInt=3, then minInt will be changed to 3.
   *
   * <p>See {@link #setRoundingIncrement} and {@link #setMaximumFractionDigits} for two other ways
   * of specifying rounding strategies.
   *
   * @param value The maximum number of significant digits to display.
   * @see #setRoundingMode
   * @see #setRoundingIncrement
   * @see #setMaximumFractionDigits
   * @category Rounding
   * @stable ICU 3.0
   */
  public synchronized void setMaximumSignificantDigits(int value) {
    int min = properties.getMinimumSignificantDigits();
    if (min >= 0 && min > value) {
      properties.setMinimumSignificantDigits(value);
    }
    properties.setMaximumSignificantDigits(value);
    refreshFormatter();
  }

  /**
   * Returns the minimum number of characters in formatted output.
   *
   * @see #setFormatWidth
   * @category Padding
   * @stable ICU 2.0
   */
  public synchronized int getFormatWidth() {
    return properties.getFormatWidth();
  }

  /**
   * <strong>Padding:</strong> Sets the minimum width of the string output by the formatting
   * pipeline. For example, if padding is enabled and paddingWidth is set to 6, formatting the
   * number "3.14159" with the pattern "0.00" will result in "··3.14" if '·' is your padding string.
   *
   * <p>If the number is longer than your padding width, the number will display as if no padding
   * width had been specified, which may result in strings longer than the padding width.
   *
   * <p>Padding can be specified in the pattern string using the '*' symbol. For example, the format
   * "*x######0" has a format width of 7 and a pad character of 'x'.
   *
   * <p>Padding is currently counted in UTF-16 code units; see <a
   * href="https://unicode-org.atlassian.net/browse/ICU-13034">ticket #13034</a> for more information.
   *
   * @param width The minimum number of characters in the output.
   * @see #setPadCharacter
   * @see #setPadPosition
   * @category Padding
   * @stable ICU 2.0
   */
  public synchronized void setFormatWidth(int width) {
    properties.setFormatWidth(width);
    refreshFormatter();
  }

  /**
   * {@icu} Returns the character used for padding.
   *
   * @see #setPadCharacter
   * @category Padding
   * @stable ICU 2.0
   */
  public synchronized char getPadCharacter() {
    CharSequence paddingString = properties.getPadString();
    if (paddingString == null) {
      return Padder.FALLBACK_PADDING_STRING.charAt(0);
    } else {
      return paddingString.charAt(0);
    }
  }

  /**
   * {@icu} <strong>Padding:</strong> Sets the character used to pad numbers that are narrower than
   * the width specified in {@link #setFormatWidth}.
   *
   * <p>In the pattern string, the padding character is the token that follows '*' before or after
   * the prefix or suffix.
   *
   * @param padChar The character used for padding.
   * @see #setFormatWidth
   * @category Padding
   * @stable ICU 2.0
   */
  public synchronized void setPadCharacter(char padChar) {
    properties.setPadString(Character.toString(padChar));
    refreshFormatter();
  }

  /**
   * {@icu} Returns the position used for padding.
   *
   * @see #setPadPosition
   * @category Padding
   * @stable ICU 2.0
   */
  public synchronized int getPadPosition() {
    PadPosition loc = properties.getPadPosition();
    return (loc == null) ? PAD_BEFORE_PREFIX : loc.toOld();
  }

  /**
   * {@icu} <strong>Padding:</strong> Sets the position where to insert the pad character when
   * narrower than the width specified in {@link #setFormatWidth}. For example, consider the pattern
   * "P123S" with padding width 8 and padding char "*". The four positions are:
   *
   * <ul>
   *   <li>{@link DecimalFormat#PAD_BEFORE_PREFIX} ⇒ "***P123S"
   *   <li>{@link DecimalFormat#PAD_AFTER_PREFIX} ⇒ "P***123S"
   *   <li>{@link DecimalFormat#PAD_BEFORE_SUFFIX} ⇒ "P123***S"
   *   <li>{@link DecimalFormat#PAD_AFTER_SUFFIX} ⇒ "P123S***"
   * </ul>
   *
   * @param padPos The position used for padding.
   * @see #setFormatWidth
   * @category Padding
   * @stable ICU 2.0
   */
  public synchronized void setPadPosition(int padPos) {
    properties.setPadPosition(PadPosition.fromOld(padPos));
    refreshFormatter();
  }

  /**
   * {@icu} Returns whether scientific (exponential) notation is enabled on this formatter.
   *
   * @see #setScientificNotation
   * @category ScientificNotation
   * @stable ICU 2.0
   */
  public synchronized boolean isScientificNotation() {
    return properties.getMinimumExponentDigits() != -1;
  }

  /**
   * {@icu} <strong>Scientific Notation:</strong> Sets whether this formatter should print in
   * scientific (exponential) notation. For example, if scientific notation is enabled, the number
   * 123000 will be printed as "1.23E5" in locale <em>en-US</em>. A locale-specific symbol is used
   * as the exponent separator.
   *
   * <p>Calling <code>df.setScientificNotation(true)</code> is functionally equivalent to calling
   * <code>df.setMinimumExponentDigits(1)</code>.
   *
   * @param useScientific true to enable scientific notation; false to disable it.
   * @see #setMinimumExponentDigits
   * @category ScientificNotation
   * @stable ICU 2.0
   */
  public synchronized void setScientificNotation(boolean useScientific) {
    if (useScientific) {
      properties.setMinimumExponentDigits(1);
    } else {
      properties.setMinimumExponentDigits(-1);
    }
    refreshFormatter();
  }

  /**
   * {@icu} Returns the minimum number of digits printed in the exponent in scientific notation.
   *
   * @see #setMinimumExponentDigits
   * @category ScientificNotation
   * @stable ICU 2.0
   */
  public synchronized byte getMinimumExponentDigits() {
    return (byte) properties.getMinimumExponentDigits();
  }

  /**
   * {@icu} <strong>Scientific Notation:</strong> Sets the minimum number of digits to be printed in
   * the exponent. For example, if minimum exponent digits is 3, the number 123000 will be printed
   * as "1.23E005".
   *
   * <p>This setting corresponds to the number of zeros after the 'E' in a pattern string such as
   * "0.00E000".
   *
   * @param minExpDig The minimum number of digits in the exponent.
   * @category ScientificNotation
   * @stable ICU 2.0
   */
  public synchronized void setMinimumExponentDigits(byte minExpDig) {
    properties.setMinimumExponentDigits(minExpDig);
    refreshFormatter();
  }

  /**
   * {@icu} Returns whether the sign (plus or minus) is always printed in scientific notation.
   *
   * @see #setExponentSignAlwaysShown
   * @category ScientificNotation
   * @stable ICU 2.0
   */
  public synchronized boolean isExponentSignAlwaysShown() {
    return properties.getExponentSignAlwaysShown();
  }

  /**
   * {@icu} <strong>Scientific Notation:</strong> Sets whether the sign (plus or minus) is always to
   * be shown in the exponent in scientific notation. For example, if this setting is enabled, the
   * number 123000 will be printed as "1.23E+5" in locale <em>en-US</em>. The number 0.0000123 will
   * always be printed as "1.23E-5" in locale <em>en-US</em> whether or not this setting is enabled.
   *
   * <p>This setting corresponds to the '+' in a pattern such as "0.00E+0".
   *
   * @param expSignAlways true to always shown the sign in the exponent; false to show it for
   *     negatives but not positives.
   * @category ScientificNotation
   * @stable ICU 2.0
   */
  public synchronized void setExponentSignAlwaysShown(boolean expSignAlways) {
    properties.setExponentSignAlwaysShown(expSignAlways);
    refreshFormatter();
  }

  /**
   * Returns whether or not grouping separators are being printed in the output.
   *
   * @see #setGroupingUsed
   * @category Separators
   * @stable ICU 2.0
   */
  @Override
  public synchronized boolean isGroupingUsed() {
    return properties.getGroupingUsed();
  }

  /**
   * <strong>Grouping:</strong> Sets whether grouping is to be used when formatting numbers.
   * Grouping means whether the thousands, millions, billions, and larger powers of ten should be
   * separated by a grouping separator (a comma in <em>en-US</em>).
   *
   * <p>For example, if grouping is enabled, 12345 will be printed as "12,345" in <em>en-US</em>. If
   * grouping were disabled, it would instead be printed as simply "12345".
   *
   * @param enabled true to enable grouping separators; false to disable them.
   * @see #setGroupingSize
   * @see #setSecondaryGroupingSize
   * @category Separators
   * @stable ICU 2.0
   */
  @Override
  public synchronized void setGroupingUsed(boolean enabled) {
    properties.setGroupingUsed(enabled);
    refreshFormatter();
  }

  /**
   * Returns the primary grouping size in use.
   *
   * @see #setGroupingSize
   * @category Separators
   * @stable ICU 2.0
   */
  public synchronized int getGroupingSize() {
    if (properties.getGroupingSize() < 0) {
      return 0;
    }
    return properties.getGroupingSize();
  }

  /**
   * <strong>Grouping:</strong> Sets the primary grouping size (distance between grouping
   * separators) used when formatting large numbers. For most locales, this defaults to 3: the
   * number of digits between the ones and thousands place, between thousands and millions, and so
   * forth.
   *
   * <p>For example, with a grouping size of 3, the number 1234567 will be formatted as "1,234,567".
   *
   * <p>Grouping size can also be specified in the pattern: for example, "#,##0" corresponds to a
   * grouping size of 3.
   *
   * @param width The grouping size to use.
   * @see #setSecondaryGroupingSize
   * @category Separators
   * @stable ICU 2.0
   */
  public synchronized void setGroupingSize(int width) {
    properties.setGroupingSize(width);
    refreshFormatter();
  }

  /**
   * {@icu} Returns the secondary grouping size in use.
   *
   * @see #setSecondaryGroupingSize
   * @category Separators
   * @stable ICU 2.0
   */
  public synchronized int getSecondaryGroupingSize() {
    int grouping2 = properties.getSecondaryGroupingSize();
    if (grouping2 < 0) {
      return 0;
    }
    return grouping2;
  }

  /**
   * {@icu} <strong>Grouping:</strong> Sets the secondary grouping size (distance between grouping
   * separators after the first separator) used when formatting large numbers. In many south Asian
   * locales, this is set to 2.
   *
   * <p>For example, with primary grouping size 3 and secondary grouping size 2, the number 1234567
   * will be formatted as "12,34,567".
   *
   * <p>Grouping size can also be specified in the pattern: for example, "#,##,##0" corresponds to a
   * primary grouping size of 3 and a secondary grouping size of 2.
   *
   * @param width The secondary grouping size to use.
   * @see #setGroupingSize
   * @category Separators
   * @stable ICU 2.0
   */
  public synchronized void setSecondaryGroupingSize(int width) {
    properties.setSecondaryGroupingSize(width);
    refreshFormatter();
  }

  /**
   * {@icu} Returns the minimum number of digits before grouping is triggered.
   *
   * @see #setMinimumGroupingDigits
   * @category Separators
   * @stable ICU 64
   */
  public synchronized int getMinimumGroupingDigits() {
    if (properties.getMinimumGroupingDigits() > 0) {
      return properties.getMinimumGroupingDigits();
    }
    return 1;
  }

  /**
   * {@icu} Sets the minimum number of digits that must be before the first grouping separator in
   * order for the grouping separator to be printed. For example, if minimum grouping digits is set
   * to 2, in <em>en-US</em>, 1234 will be printed as "1234" and 12345 will be printed as "12,345".
   *
    * Set the value to:
   * <ul>
   * <li>1 to turn off minimum grouping digits.</li>
   * <li>MINIMUM_GROUPING_DIGITS_AUTO to display grouping using the default
   * strategy for all locales.</li>
   * <li>MINIMUM_GROUPING_DIGITS_MIN2 to display grouping using locale defaults,
   * except do not show grouping on values smaller than 10000 (such that there is a minimum of
   * two digits before the first separator).</li>
   * </ul>
   *
   * @param number The minimum number of digits before grouping is triggered.
   * @category Separators
   * @stable ICU 64
   */
  public synchronized void setMinimumGroupingDigits(int number) {
    properties.setMinimumGroupingDigits(number);
    refreshFormatter();
  }

  /**
   * {@icu} Constant for {@link #setMinimumGroupingDigits(int)} to specify display
   * grouping using the default strategy for all locales.
   *
   * @see #setMinimumGroupingDigits(int)
   * @see #MINIMUM_GROUPING_DIGITS_MIN2
   * @category Separators
   * @stable ICU 68
   */
  public static final int MINIMUM_GROUPING_DIGITS_AUTO = -2;

  /**
   * {@icu} Constant for {@link #setMinimumGroupingDigits(int)} to specify display
   * grouping using locale defaults, except do not show grouping on values smaller than
   * 10000 (such that there is a minimum of two digits before the first separator).
   *
   * @see #setMinimumGroupingDigits(int)
   * @see #MINIMUM_GROUPING_DIGITS_AUTO
   * @category Separators
   * @stable ICU 68
   */
  public static final int MINIMUM_GROUPING_DIGITS_MIN2 = -3;

  /**
   * Returns whether the decimal separator is shown on integers.
   *
   * @see #setDecimalSeparatorAlwaysShown
   * @category Separators
   * @stable ICU 2.0
   */
  public synchronized boolean isDecimalSeparatorAlwaysShown() {
    return properties.getDecimalSeparatorAlwaysShown();
  }

  /**
   * <strong>Separators:</strong> Sets whether the decimal separator (a period in <em>en-US</em>) is
   * shown on integers. For example, if this setting is turned on, formatting 123 will result in
   * "123." with the decimal separator.
   *
   * <p>This setting can be specified in the pattern for integer formats: "#,##0." is an example.
   *
   * @param value true to always show the decimal separator; false to show it only when there is a
   *     fraction part of the number.
   * @category Separators
   * @stable ICU 2.0
   */
  public synchronized void setDecimalSeparatorAlwaysShown(boolean value) {
    properties.setDecimalSeparatorAlwaysShown(value);
    refreshFormatter();
  }

  /**
   * Returns the currency used to display currency amounts. May be null.
   *
   * @see #setCurrency
   * @see DecimalFormatSymbols#getCurrency
   * @category Currency
   * @stable ICU 2.6
   */
  @Override
  public synchronized Currency getCurrency() {
    return exportedProperties.getCurrency();
  }

  /**
   * Sets the currency to be used when formatting numbers. The effect is twofold:
   *
   * <ol>
   *   <li>Substitutions for currency symbols in the pattern string will use this currency
   *   <li>The rounding mode will obey the rules for this currency (see {@link #setCurrencyUsage})
   * </ol>
   *
   * <strong>Important:</strong> Displaying the currency in the output requires that the patter
   * associated with this formatter contains a currency symbol '¤'. This will be the case if the
   * instance was created via {@link #getCurrencyInstance} or one of its friends.
   *
   * @param currency The currency to use.
   * @category Currency
   * @stable ICU 2.2
   */
  @Override
  public synchronized void setCurrency(Currency currency) {
    properties.setCurrency(currency);
    if (currency != null) {
      symbols.setCurrency(currency);
    }
    refreshFormatter();
  }

  /**
   * {@icu} Returns the strategy for rounding currency amounts.
   *
   * @see #setCurrencyUsage
   * @category Currency
   * @stable ICU 54
   */
  public synchronized CurrencyUsage getCurrencyUsage() {
    // CurrencyUsage is not exported, so we have to get it from the input property bag.
    // TODO: Should we export CurrencyUsage instead?
    CurrencyUsage usage = properties.getCurrencyUsage();
    if (usage == null) {
      usage = CurrencyUsage.STANDARD;
    }
    return usage;
  }

  /**
   * {@icu} Sets the currency-dependent strategy to use when rounding numbers. There are two
   * strategies:
   *
   * <ul>
   *   <li>STANDARD: When the amount displayed is intended for banking statements or electronic
   *       transfer.
   *   <li>CASH: When the amount displayed is intended to be representable in physical currency,
   *       like at a cash register.
   * </ul>
   *
   * CASH mode is relevant in currencies that do not have tender down to the penny. For more
   * information on the two rounding strategies, see <a
   * href="http://unicode.org/reports/tr35/tr35-numbers.html#Supplemental_Currency_Data">UTS
   * #35</a>. If omitted, the strategy defaults to STANDARD. To override currency rounding
   * altogether, use {@link #setMinimumFractionDigits} and {@link #setMaximumFractionDigits} or
   * {@link #setRoundingIncrement}.
   *
   * @param usage The strategy to use when rounding in the current currency.
   * @category Currency
   * @stable ICU 54
   */
  public synchronized void setCurrencyUsage(CurrencyUsage usage) {
    properties.setCurrencyUsage(usage);
    refreshFormatter();
  }

  /**
   * {@icu} Returns the current instance of CurrencyPluralInfo.
   *
   * @see #setCurrencyPluralInfo
   * @category Currency
   * @stable ICU 4.2
   */
  public synchronized CurrencyPluralInfo getCurrencyPluralInfo() {
    // CurrencyPluralInfo also is not exported.
    return properties.getCurrencyPluralInfo();
  }

  /**
   * {@icu} Sets a custom instance of CurrencyPluralInfo. CurrencyPluralInfo generates pattern
   * strings for printing currency long names.
   *
   * <p><strong>Most users should not call this method directly.</strong> You should instead create
   * your formatter via <code>NumberFormat.getInstance(NumberFormat.PLURALCURRENCYSTYLE)</code>.
   *
   * @param newInfo The CurrencyPluralInfo to use when printing currency long names.
   * @category Currency
   * @stable ICU 4.2
   */
  public synchronized void setCurrencyPluralInfo(CurrencyPluralInfo newInfo) {
    properties.setCurrencyPluralInfo(newInfo);
    refreshFormatter();
  }

  /**
   * Returns whether {@link #parse} will always return a BigDecimal.
   *
   * @see #setParseBigDecimal
   * @category Parsing
   * @stable ICU 3.6
   */
  public synchronized boolean isParseBigDecimal() {
    return properties.getParseToBigDecimal();
  }

  /**
   * Whether to make {@link #parse} prefer returning a {@link com.ibm.icu.math.BigDecimal} when
   * possible. For strings corresponding to return values of Infinity, -Infinity, NaN, and -0.0, a
   * Double will be returned even if ParseBigDecimal is enabled.
   *
   * @param value true to cause {@link #parse} to prefer BigDecimal; false to let {@link #parse}
   *     return additional data types like Long or BigInteger.
   * @category Parsing
   * @stable ICU 3.6
   */
  public synchronized void setParseBigDecimal(boolean value) {
    properties.setParseToBigDecimal(value);
    refreshFormatter();
  }

  /**
   * Always returns 1000, the default prior to ICU 59.
   *
   * @category Parsing
   * @deprecated Setting max parse digits has no effect since ICU4J 59.
   */
  @Deprecated
  public int getParseMaxDigits() {
    return 1000;
  }

  /**
   * @param maxDigits Prior to ICU 59, the maximum number of digits in the output number after
   *     exponential notation is applied.
   * @category Parsing
   * @deprecated Setting max parse digits has no effect since ICU4J 59.
   */
  @Deprecated
  public void setParseMaxDigits(int maxDigits) {}

  /**
   * {@inheritDoc}
   *
   * @category Parsing
   * @stable ICU 3.6
   */
  @Override
  public synchronized boolean isParseStrict() {
    return properties.getParseMode() == ParseMode.STRICT;
  }

  /**
   * {@inheritDoc}
   *
   * @category Parsing
   * @stable ICU 3.6
   */
  @Override
  public synchronized void setParseStrict(boolean parseStrict) {
    ParseMode mode = parseStrict ? ParseMode.STRICT : ParseMode.LENIENT;
    properties.setParseMode(mode);
    refreshFormatter();
  }

  /**
   * Android libcore uses this internal method to set {@link ParseMode#JAVA_COMPATIBILITY}.
   * @internal
   * @deprecated This API is ICU internal only.
   */
  @Deprecated
public synchronized void setParseStrictMode(ParseMode parseMode) {
    properties.setParseMode(parseMode);
    refreshFormatter();
  }

  /**
   * {@inheritDoc}
   *
   * @see #setParseIntegerOnly
   * @category Parsing
   * @stable ICU 2.0
   */
  @Override
  public synchronized boolean isParseIntegerOnly() {
    return properties.getParseIntegerOnly();
  }

  /**
   * <strong>Parsing:</strong> {@inheritDoc}
   *
   * <p>This is functionally equivalent to calling {@link #setDecimalPatternMatchRequired} and a
   * pattern without a decimal point.
   *
   * @param parseIntegerOnly true to ignore fractional parts of numbers when parsing; false to
   *     consume fractional parts.
   * @category Parsing
   * @stable ICU 2.0
   */
  @Override
  public synchronized void setParseIntegerOnly(boolean parseIntegerOnly) {
    properties.setParseIntegerOnly(parseIntegerOnly);
    refreshFormatter();
  }

  /**
   * {@icu} Returns whether the presence of a decimal point must match the pattern.
   *
   * @see #setDecimalPatternMatchRequired
   * @category Parsing
   * @stable ICU 54
   */
  public synchronized boolean isDecimalPatternMatchRequired() {
    return properties.getDecimalPatternMatchRequired();
  }

  /**
   * {@icu} <strong>Parsing:</strong> This method is used to either <em>require</em> or
   * <em>forbid</em> the presence of a decimal point in the string being parsed (disabled by
   * default). This feature was designed to be an extra layer of strictness on top of strict
   * parsing, although it can be used in either lenient mode or strict mode.
   *
   * <p>To <em>require</em> a decimal point, call this method in combination with either a pattern
   * containing a decimal point or with {@link #setDecimalSeparatorAlwaysShown}.
   *
   * <pre>
   * // Require a decimal point in the string being parsed:
   * df.applyPattern("#.");
   * df.setDecimalPatternMatchRequired(true);
   *
   * // Alternatively:
   * df.setDecimalSeparatorAlwaysShown(true);
   * df.setDecimalPatternMatchRequired(true);
   * </pre>
   *
   * To <em>forbid</em> a decimal point, call this method in combination with a pattern containing
   * no decimal point. Alternatively, use {@link #setParseIntegerOnly} for the same behavior without
   * depending on the contents of the pattern string.
   *
   * <pre>
   * // Forbid a decimal point in the string being parsed:
   * df.applyPattern("#");
   * df.setDecimalPatternMatchRequired(true);
   * </pre>
   *
   * @param value true to either require or forbid the decimal point according to the pattern; false
   *     to disable this feature.
   * @see #setParseIntegerOnly
   * @category Parsing
   * @stable ICU 54
   */
  public synchronized void setDecimalPatternMatchRequired(boolean value) {
    properties.setDecimalPatternMatchRequired(value);
    refreshFormatter();
  }

  /**
   * {@icu} Returns whether to ignore exponents when parsing.
   *
   * @see #setParseNoExponent
   * @category Parsing
   * @stable ICU 64
   */
  public synchronized boolean isParseNoExponent() {
    return properties.getParseNoExponent();
  }

  /**
   * {@icu} Specifies whether to stop parsing when an exponent separator is encountered. For
   * example, parses "123E4" to 123 (with parse position 3) instead of 1230000 (with parse position
   * 5).
   *
   * @param value true to prevent exponents from being parsed; false to allow them to be parsed.
   * @category Parsing
   * @stable ICU 64
   */
  public synchronized void setParseNoExponent(boolean value) {
    properties.setParseNoExponent(value);
    refreshFormatter();
  }

  /**
   * {@icu} Returns whether to force case (uppercase/lowercase) to match when parsing.
   *
   * @see #setParseNoExponent
   * @category Parsing
   * @stable ICU 64
   */
  public synchronized boolean isParseCaseSensitive() {
    return properties.getParseCaseSensitive();
  }

  /**
   * {@icu} Specifies whether parsing should require cases to match in affixes, exponent separators,
   * and currency codes. Case mapping is performed for each code point using {@link
   * UCharacter#foldCase}.
   *
   * @param value true to force case (uppercase/lowercase) to match when parsing; false to ignore
   *     case and perform case folding.
   * @category Parsing
   * @stable ICU 64
   */
  public synchronized void setParseCaseSensitive(boolean value) {
    properties.setParseCaseSensitive(value);
    refreshFormatter();
  }

  // TODO(sffc): Uncomment for ICU 60 API proposal.
  //
  //  /**
  //   * {@icu} Returns the strategy used for choosing between grouping and decimal separators when
  //   * parsing.
  //   *
  //   * @see #setParseGroupingMode
  //   * @category Parsing
  //   */
  //  public synchronized GroupingMode getParseGroupingMode() {
  //    return properties.getParseGroupingMode();
  //  }
  //
  //  /**
  //   * {@icu} Sets the strategy used during parsing when a code point needs to be interpreted as
  //   * either a decimal separator or a grouping separator.
  //   *
  //   * <p>The comma, period, space, and apostrophe have different meanings in different locales. For
  //   * example, in <em>en-US</em> and most American locales, the period is used as a decimal
  //   * separator, but in <em>es-PY</em> and most European locales, it is used as a grouping separator.
  //   *
  //   * Suppose you are in <em>fr-FR</em> the parser encounters the string "1.234".  In <em>fr-FR</em>,
  //   * the grouping is a space and the decimal is a comma.  The <em>grouping mode</em> is a mechanism
  //   * to let you specify whether to accept the string as 1234 (GroupingMode.DEFAULT) or whether to reject it since the separators
  //   * don't match (GroupingMode.RESTRICTED).
  //   *
  //   * When resolving grouping separators, it is the <em>equivalence class</em> of separators that is considered.
  //   * For example, a period is seen as equal to a fixed set of other period-like characters.
  //   *
  //   * @param groupingMode The strategy to use; either DEFAULT or RESTRICTED.
  //   * @category Parsing
  //   */
  //  public synchronized void setParseGroupingMode(GroupingMode groupingMode) {
  //    properties.setParseGroupingMode(groupingMode);
  //    refreshFormatter();
  //  }

  //=====================================================================================//
  //                                     UTILITIES                                       //
  //=====================================================================================//

  /**
   * Tests for equality between this formatter and another formatter.
   *
   * <p>If two DecimalFormat instances are equal, then they will always produce the same output.
   * However, the reverse is not necessarily true: if two DecimalFormat instances always produce the
   * same output, they are not necessarily equal.
   *
   * @stable ICU 2.0
   */
  @Override
  public synchronized boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (!(obj instanceof DecimalFormat)) return false;
    DecimalFormat other = (DecimalFormat) obj;
    return properties.equals(other.properties) && symbols.equals(other.symbols);
  }

  /**
   * {@inheritDoc}
   *
   * @stable ICU 2.0
   */
  @Override
  public synchronized int hashCode() {
    return properties.hashCode() ^ symbols.hashCode();
  }

  /**
   * Returns the default value of toString() with extra DecimalFormat-specific information appended
   * to the end of the string. This extra information is intended for debugging purposes, and the
   * format is not guaranteed to be stable.
   *
   * @stable ICU 2.0
   */
  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append(getClass().getName());
    result.append("@");
    result.append(Integer.toHexString(hashCode()));
    result.append(" { symbols@");
    result.append(Integer.toHexString(symbols.hashCode()));
    synchronized (this) {
      properties.toStringBare(result);
    }
    result.append(" }");
    return result.toString();
  }

  /**
   * Serializes this formatter object to a decimal format pattern string. The result of this method
   * is guaranteed to be <em>functionally</em> equivalent to the pattern string used to create this
   * instance after incorporating values from the setter methods.
   *
   * <p>For more information on decimal format pattern strings, see <a
   * href="http://unicode.org/reports/tr35/tr35-numbers.html#Number_Format_Patterns">UTS #35</a>.
   *
   * <p><strong>Important:</strong> Not all properties are capable of being encoded in a pattern
   * string. See a list of properties in {@link #applyPattern}.
   *
   * @return A decimal format pattern string.
   * @stable ICU 2.0
   */
  public synchronized String toPattern() {
    // Pull some properties from exportedProperties and others from properties
    // to keep affix patterns intact.  In particular, pull rounding properties
    // so that CurrencyUsage is reflected properly.
    // TODO: Consider putting this logic in PatternString.java instead.
    DecimalFormatProperties tprops = new DecimalFormatProperties().copyFrom(properties);
    boolean useCurrency = ((tprops.getCurrency() != null)
            || tprops.getCurrencyPluralInfo() != null
            || tprops.getCurrencyUsage() != null
            || tprops.getCurrencyAsDecimal()
            || AffixUtils.hasCurrencySymbols(tprops.getPositivePrefixPattern())
            || AffixUtils.hasCurrencySymbols(tprops.getPositiveSuffixPattern())
            || AffixUtils.hasCurrencySymbols(tprops.getNegativePrefixPattern())
            || AffixUtils.hasCurrencySymbols(tprops.getNegativeSuffixPattern()));
    if (useCurrency) {
      tprops.setMinimumFractionDigits(exportedProperties.getMinimumFractionDigits());
      tprops.setMaximumFractionDigits(exportedProperties.getMaximumFractionDigits());
      tprops.setRoundingIncrement(exportedProperties.getRoundingIncrement());
    }
    return PatternStringUtils.propertiesToPatternString(tprops);
  }

  /**
   * Calls {@link #toPattern} and converts the string to localized notation. For more information on
   * localized notation, see {@link #applyLocalizedPattern}. This method is provided for backwards
   * compatibility and should not be used in new projects.
   *
   * @return A decimal format pattern string in localized notation.
   * @stable ICU 2.0
   */
  public synchronized String toLocalizedPattern() {
    String pattern = toPattern();
    return PatternStringUtils.convertLocalized(pattern, symbols, true);
  }

  /**
   * Converts this DecimalFormat to a NumberFormatter.  Starting in ICU 60,
   * NumberFormatter is the recommended way to format numbers.
   *
   * @return An instance of {@link LocalizedNumberFormatter} with the same behavior as this instance of
   * DecimalFormat.
   * @see NumberFormatter
   * @stable ICU 60
   */
  public LocalizedNumberFormatter toNumberFormatter() {
      return formatter;
  }

  /**
   * @internal
   * @deprecated This API is ICU internal only.
   */
  @Deprecated
  public IFixedDecimal getFixedDecimal(double number) {
    return formatter.format(number).getFixedDecimal();
  }

  /** Rebuilds the formatter object from the property bag. */
  void refreshFormatter() {
    if (exportedProperties == null) {
      // exportedProperties is null only when the formatter is not ready yet.
      // The only time when this happens is during legacy deserialization.
      return;
    }
    ULocale locale = this.getLocale(ULocale.ACTUAL_LOCALE);
    if (locale == null) {
      // Constructor
      locale = symbols.getLocale(ULocale.ACTUAL_LOCALE);
    }
    if (locale == null) {
      // Deserialization
      locale = symbols.getULocale();
    }
    assert locale != null;
    formatter = NumberFormatter.fromDecimalFormat(properties, symbols, exportedProperties).locale(locale);

    // Lazy-initialize the parsers only when we need them.
    parser = null;
    currencyParser = null;
  }

  NumberParserImpl getParser() {
    if (parser == null) {
      parser = NumberParserImpl.createParserFromProperties(properties, symbols, false);
    }
    return parser;
  }

  NumberParserImpl getCurrencyParser() {
    if (currencyParser == null) {
      currencyParser = NumberParserImpl.createParserFromProperties(properties, symbols, true);
    }
    return currencyParser;
  }

  /**
   * Converts a java.math.BigDecimal to a com.ibm.icu.math.BigDecimal with fallback for numbers
   * outside of the range supported by com.ibm.icu.math.BigDecimal.
   *
   * @param number
   * @return
   */
  private Number safeConvertBigDecimal(java.math.BigDecimal number) {
    try {
      return new com.ibm.icu.math.BigDecimal(number);
    } catch (NumberFormatException e) {
      if (number.signum() > 0 && number.scale() < 0) {
        return Double.POSITIVE_INFINITY;
      } else if (number.scale() < 0) {
        return Double.NEGATIVE_INFINITY;
      } else if (number.signum() < 0) {
        return -0.0;
      } else {
        return 0.0;
      }
    }
  }

  /**
   * Updates the property bag with settings from the given pattern.
   *
   * @param pattern The pattern string to parse.
   * @param ignoreRounding Whether to leave out rounding information (minFrac, maxFrac, and rounding
   *     increment) when parsing the pattern. This may be desirable if a custom rounding mode, such
   *     as CurrencyUsage, is to be used instead. One of {@link
   *     PatternStringParser#IGNORE_ROUNDING_ALWAYS}, {@link PatternStringParser#IGNORE_ROUNDING_IF_CURRENCY},
   *     or {@link PatternStringParser#IGNORE_ROUNDING_NEVER}.
   * @see PatternAndPropertyUtils#parseToExistingProperties
   */
  void setPropertiesFromPattern(String pattern, int ignoreRounding) {
    if (pattern == null) {
      throw new NullPointerException();
    }
    PatternStringParser.parseToExistingProperties(pattern, properties, ignoreRounding);
  }

  static void fieldPositionHelper(
          DecimalQuantity dq, FormattedStringBuilder string, FieldPosition fieldPosition, int offset) {
      // always return first occurrence:
      fieldPosition.setBeginIndex(0);
      fieldPosition.setEndIndex(0);
      dq.populateUFieldPosition(fieldPosition);
      boolean found = FormattedValueStringBuilderImpl.nextFieldPosition(string, fieldPosition);;
      if (found && offset != 0) {
          fieldPosition.setBeginIndex(fieldPosition.getBeginIndex() + offset);
          fieldPosition.setEndIndex(fieldPosition.getEndIndex() + offset);
      }
  }

  /**
   * @internal
   * @deprecated This API is ICU internal only.
   */
  @Deprecated
  public synchronized void setProperties(PropertySetter func) {
    func.set(properties);
    refreshFormatter();
  }

  /**
   * @internal
   * @deprecated This API is ICU internal only.
   */
  @Deprecated
  public static interface PropertySetter {
    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public void set(DecimalFormatProperties props);
  }

  /**
   * {@icu} Constant for {@link #getPadPosition()} and {@link #setPadPosition(int)} to specify pad
   * characters inserted before the prefix.
   *
   * @see #setPadPosition
   * @see #getPadPosition
   * @see #PAD_AFTER_PREFIX
   * @see #PAD_BEFORE_SUFFIX
   * @see #PAD_AFTER_SUFFIX
   * @stable ICU 2.0
   */
  public static final int PAD_BEFORE_PREFIX = 0;

  /**
   * {@icu} Constant for {@link #getPadPosition()} and {@link #setPadPosition(int)} to specify pad
   * characters inserted after the prefix.
   *
   * @see #setPadPosition
   * @see #getPadPosition
   * @see #PAD_BEFORE_PREFIX
   * @see #PAD_BEFORE_SUFFIX
   * @see #PAD_AFTER_SUFFIX
   * @stable ICU 2.0
   */
  public static final int PAD_AFTER_PREFIX = 1;

  /**
   * {@icu} Constant for {@link #getPadPosition()} and {@link #setPadPosition(int)} to specify pad
   * characters inserted before the suffix.
   *
   * @see #setPadPosition
   * @see #getPadPosition
   * @see #PAD_BEFORE_PREFIX
   * @see #PAD_AFTER_PREFIX
   * @see #PAD_AFTER_SUFFIX
   * @stable ICU 2.0
   */
  public static final int PAD_BEFORE_SUFFIX = 2;

  /**
   * {@icu} Constant for {@link #getPadPosition()} and {@link #setPadPosition(int)} to specify pad
   * characters inserted after the suffix.
   *
   * @see #setPadPosition
   * @see #getPadPosition
   * @see #PAD_BEFORE_PREFIX
   * @see #PAD_AFTER_PREFIX
   * @see #PAD_BEFORE_SUFFIX
   * @stable ICU 2.0
   */
  public static final int PAD_AFTER_SUFFIX = 3;
}
