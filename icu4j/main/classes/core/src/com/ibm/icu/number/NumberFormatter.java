// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.util.Locale;

import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.ULocale;

/**
 * All-in-one formatter for localized numbers, currencies, and units.
 *
 * For a full list of options, see {@link NumberFormatterSettings}.
 *
 * <pre>
 * // Most basic usage:
 * NumberFormatter.withLocale(...).format(123).toString();  // 1,234 in en-US
 *
 * // Custom notation, unit, and rounding strategy:
 * NumberFormatter.with()
 *     .notation(Notation.compactShort())
 *     .unit(Currency.getInstance("EUR"))
 *     .precision(Precision.maxDigits(2))
 *     .locale(...)
 *     .format(1234)
 *     .toString();  // €1.2K in en-US
 *
 * // Create a formatter in a private static final field:
 * private static final LocalizedNumberFormatter formatter = NumberFormatter.withLocale(...)
 *     .unit(NoUnit.PERCENT)
 *     .precision(Precision.fixedFraction(3));
 * formatter.format(5.9831).toString();  // 5.983% in en-US
 *
 * // Create a "template" in a private static final field but without setting a locale until the call site:
 * private static final UnlocalizedNumberFormatter template = NumberFormatter.with()
 *     .sign(SignDisplay.ALWAYS)
 *     .unitWidth(UnitWidth.FULL_NAME);
 * template.locale(...).format(new Measure(1234, MeasureUnit.METER)).toString();  // +1,234 meters in en-US
 * </pre>
 *
 * <p>
 * This API offers more features than {@link com.ibm.icu.text.DecimalFormat} and is geared toward new
 * users of ICU.
 *
 * <p>
 * NumberFormatter instances (i.e., LocalizedNumberFormatter and UnlocalizedNumberFormatter)
 * are immutable and thread safe. This means that invoking a configuration
 * method has no effect on the receiving instance; you must store and use the new number formatter
 * instance it returns instead.
 *
 * <pre>
 * UnlocalizedNumberFormatter formatter = UnlocalizedNumberFormatter.with()
 *         .notation(Notation.scientific());
 * formatter.precision(Precision.maxFraction(2)); // does nothing!
 * formatter.locale(ULocale.ENGLISH).format(9.8765).toString(); // prints "9.8765E0", not "9.88E0"
 * </pre>
 *
 * <p>
 * This API is based on the <em>fluent</em> design pattern popularized by libraries such as Google's
 * Guava. For extensive details on the design of this API, read <a href="https://goo.gl/szi5VB">the
 * design doc</a>.
 *
 * @author Shane Carr
 * @stable ICU 60
 */
public final class NumberFormatter {

    private static final UnlocalizedNumberFormatter BASE = new UnlocalizedNumberFormatter();

    /**
     * An enum declaring how to resolve conflicts between maximum fraction digits and maximum
     * significant digits.
     *
     * <p>There are two modes, RELAXED and STRICT:
     *
     * <ul>
     * <li> RELAXED: Relax one of the two constraints (fraction digits or significant digits) in order
     *   to round the number to a higher level of precision.
     * <li> STRICT: Enforce both constraints, resulting in the number being rounded to a lower
     *   level of precision.
     * </ul>
     *
     * <p>The default settings for compact notation rounding are Max-Fraction = 0 (round to the nearest
     * integer), Max-Significant = 2 (round to 2 significant digits), and priority RELAXED (choose
     * the constraint that results in more digits being displayed).
     *
     * <p>Conflicting *minimum* fraction and significant digits are always resolved in the direction that
     * results in more trailing zeros.
     *
     * <p>Example 1: Consider the number 3.141, with various different settings:
     *
     * <ul>
     * <li> Max-Fraction = 1: "3.1"
     * <li> Max-Significant = 3: "3.14"
     * </ul>
     *
     * <p>The rounding priority determines how to resolve the conflict when both Max-Fraction and
     * Max-Significant are set. With RELAXED, the less-strict setting (the one that causes more digits
     * to be displayed) will be used; Max-Significant wins. With STRICT, the more-strict setting (the
     * one that causes fewer digits to be displayed) will be used; Max-Fraction wins.
     *
     * <p>Example 2: Consider the number 8317, with various different settings:
     *
     * <ul>
     * <li> Max-Fraction = 1: "8317"
     * <li> Max-Significant = 3: "8320"
     * </ul>
     *
     * <p>Here, RELAXED favors Max-Fraction and STRICT favors Max-Significant. Note that this larger
     * number caused the two modes to favor the opposite result.
     *
     * @stable ICU 69
     */
    public static enum RoundingPriority {
        /**
         * Favor greater precision by relaxing one of the rounding constraints.
         *
         * @stable ICU 69
         */
        RELAXED,

        /**
         * Favor adherence to all rounding constraints by producing lower precision.
         *
         * @stable ICU 69
         */
        STRICT,
    }

    /**
     * An enum declaring how to render units, including currencies. Example outputs when formatting 123
     * USD and 123 meters in <em>en-CA</em>:
     *
     * <ul>
     * <li>NARROW: "$123.00" and "123 m"
     * <li>SHORT: "US$ 123.00" and "123 m"
     * <li>FULL_NAME: "123.00 US dollars" and "123 meters"
     * <li>ISO_CODE: "USD 123.00" and undefined behavior
     * <li>HIDDEN: "123.00" and "123"
     * </ul>
     *
     * <p>
     * This enum is similar to {@link com.ibm.icu.text.MeasureFormat.FormatWidth}.
     *
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static enum UnitWidth {
        /**
         * Print an abbreviated version of the unit name. Similar to SHORT, but always use the shortest
         * available abbreviation or symbol. This option can be used when the context hints at the
         * identity of the unit. For more information on the difference between NARROW and SHORT, see
         * SHORT.
         *
         * <p>
         * In CLDR, this option corresponds to the "Narrow" format for measure units and the "¤¤¤¤¤"
         * placeholder for currencies.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        NARROW,

        /**
         * Print an abbreviated version of the unit name. Similar to NARROW, but use a slightly wider
         * abbreviation or symbol when there may be ambiguity. This is the default behavior.
         *
         * <p>
         * For example, in <em>es-US</em>, the SHORT form for Fahrenheit is "{0} °F", but the NARROW form
         * is "{0}°", since Fahrenheit is the customary unit for temperature in that locale.
         *
         * <p>
         * In CLDR, this option corresponds to the "Short" format for measure units and the "¤"
         * placeholder for currencies.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        SHORT,

        /**
         * Print the full name of the unit, without any abbreviations.
         *
         * <p>
         * In CLDR, this option corresponds to the default format for measure units and the "¤¤¤"
         * placeholder for currencies.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        FULL_NAME,

        /**
         * Use the three-digit ISO XXX code in place of the symbol for displaying currencies.
         *
         * <p>
         * Behavior of this option with non-currency units is not defined at this time.
         *
         * <p>
         * In CLDR, this option corresponds to the "¤¤" placeholder for currencies.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        ISO_CODE,

        /**
         * Use the formal variant of the currency symbol; for example, "NT$" for the New Taiwan
         * dollar in zh-TW.
         *
         * <p>
         * Behavior of this option with non-currency units is not defined at this time.
         *
         * @stable ICU 67
         * @see NumberFormatter
         */
        FORMAL,

        /**
         * Use the alternate variant of the currency symbol; for example, "TL" for the Turkish
         * lira (TRY).
         *
         * <p>
         * Behavior of this option with non-currency units is not defined at this time.
         *
         * @stable ICU 67
         * @see NumberFormatter
         */
        VARIANT,

        /**
         * Format the number according to the specified unit, but do not display the unit. For
         * currencies, apply monetary symbols and formats as with SHORT, but omit the currency symbol.
         * For measure units, the behavior is equivalent to not specifying the unit at all.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        HIDDEN,
    }

    /**
     * An enum declaring the strategy for when and how to display grouping separators (i.e., the
     * separator, often a comma or period, after every 2-3 powers of ten). The choices are several
     * pre-built strategies for different use cases that employ locale data whenever possible. Example
     * outputs for 1234 and 1234567 in <em>en-IN</em>:
     *
     * <ul>
     * <li>OFF: 1234 and 12345
     * <li>MIN2: 1234 and 12,34,567
     * <li>AUTO: 1,234 and 12,34,567
     * <li>ON_ALIGNED: 1,234 and 12,34,567
     * <li>THOUSANDS: 1,234 and 1,234,567
     * </ul>
     *
     * <p>
     * The default is AUTO, which displays grouping separators unless the locale data says that grouping
     * is not customary. To force grouping for all numbers greater than 1000 consistently across locales,
     * use ON_ALIGNED. On the other hand, to display grouping less frequently than the default, use MIN2
     * or OFF. See the docs of each option for details.
     *
     * <p>
     * Note: This enum specifies the strategy for grouping sizes. To set which character to use as the
     * grouping separator, use the "symbols" setter.
     *
     * @stable ICU 61
     * @see NumberFormatter
     */
    public static enum GroupingStrategy {
        /**
         * Do not display grouping separators in any locale.
         *
         * @stable ICU 61
         * @see NumberFormatter
         */
        OFF,

        /**
         * Display grouping using locale defaults, except do not show grouping on values smaller than
         * 10000 (such that there is a <em>minimum of two digits</em> before the first separator).
         *
         * <p>
         * Note that locales may restrict grouping separators to be displayed only on 1 million or
         * greater (for example, ee and hu) or disable grouping altogether (for example, bg currency).
         *
         * <p>
         * Locale data is used to determine whether to separate larger numbers into groups of 2
         * (customary in South Asia) or groups of 3 (customary in Europe and the Americas).
         *
         * @stable ICU 61
         * @see NumberFormatter
         */
        MIN2,

        /**
         * Display grouping using the default strategy for all locales. This is the default behavior.
         *
         * <p>
         * Note that locales may restrict grouping separators to be displayed only on 1 million or
         * greater (for example, ee and hu) or disable grouping altogether (for example, bg currency).
         *
         * <p>
         * Locale data is used to determine whether to separate larger numbers into groups of 2
         * (customary in South Asia) or groups of 3 (customary in Europe and the Americas).
         *
         * @stable ICU 61
         * @see NumberFormatter
         */
        AUTO,

        /**
         * Always display the grouping separator on values of at least 1000.
         *
         * <p>
         * This option ignores the locale data that restricts or disables grouping, described in MIN2 and
         * AUTO. This option may be useful to normalize the alignment of numbers, such as in a
         * spreadsheet.
         *
         * <p>
         * Locale data is used to determine whether to separate larger numbers into groups of 2
         * (customary in South Asia) or groups of 3 (customary in Europe and the Americas).
         *
         * @stable ICU 61
         * @see NumberFormatter
         */
        ON_ALIGNED,

        /**
         * Use the Western defaults: groups of 3 and enabled for all numbers 1000 or greater. Do not use
         * locale data for determining the grouping strategy.
         *
         * @stable ICU 61
         * @see NumberFormatter
         */
        THOUSANDS
    }

    /**
     * An enum declaring how to denote positive and negative numbers. Example outputs when formatting
     * 123, 0, and -123 in <em>en-US</em>:
     *
     * <ul>
     * <li>AUTO: "123", "0", and "-123"
     * <li>ALWAYS: "+123", "+0", and "-123"
     * <li>NEVER: "123", "0", and "123"
     * <li>ACCOUNTING: "$123", "$0", and "($123)"
     * <li>ACCOUNTING_ALWAYS: "+$123", "+$0", and "($123)"
     * <li>EXCEPT_ZERO: "+123", "0", and "-123"
     * <li>ACCOUNTING_EXCEPT_ZERO: "+$123", "$0", and "($123)"
     * </ul>
     *
     * <p>
     * The exact format, including the position and the code point of the sign, differ by locale.
     *
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static enum SignDisplay {
        /**
         * Show the minus sign on negative numbers, and do not show the sign on positive numbers. This is
         * the default behavior.
         *
         * If using this option, a sign will be displayed on negative zero, including negative numbers
         * that round to zero. To hide the sign on negative zero, use the NEGATIVE option.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        AUTO,

        /**
         * Show the minus sign on negative numbers and the plus sign on positive numbers, including zero.
         * To hide the sign on zero, see {@link #EXCEPT_ZERO}.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        ALWAYS,

        /**
         * Do not show the sign on positive or negative numbers.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        NEVER,

        /**
         * Use the locale-dependent accounting format on negative numbers, and do not show the sign on
         * positive numbers.
         *
         * <p>
         * The accounting format is defined in CLDR and varies by locale; in many Western locales, the
         * format is a pair of parentheses around the number.
         *
         * <p>
         * Note: Since CLDR defines the accounting format in the monetary context only, this option falls
         * back to the AUTO sign display strategy when formatting without a currency unit. This
         * limitation may be lifted in the future.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        ACCOUNTING,

        /**
         * Use the locale-dependent accounting format on negative numbers, and show the plus sign on
         * positive numbers, including zero. For more information on the accounting format, see the
         * ACCOUNTING sign display strategy. To hide the sign on zero, see
         * {@link #ACCOUNTING_EXCEPT_ZERO}.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        ACCOUNTING_ALWAYS,

        /**
         * Show the minus sign on negative numbers and the plus sign on positive numbers. Do not show a
         * sign on zero, numbers that round to zero, or NaN.
         *
         * @stable ICU 61
         * @see NumberFormatter
         */
        EXCEPT_ZERO,

        /**
         * Use the locale-dependent accounting format on negative numbers, and show the plus sign on
         * positive numbers. Do not show a sign on zero, numbers that round to zero, or NaN. For more
         * information on the accounting format, see the ACCOUNTING sign display strategy.
         *
         * @stable ICU 61
         * @see NumberFormatter
         */
        ACCOUNTING_EXCEPT_ZERO,

        /**
         * Same as AUTO, but do not show the sign on negative zero.
         *
         * @stable ICU 69
         */
        NEGATIVE,

        /**
         * Same as ACCOUNTING, but do not show the sign on negative zero.
         *
         * @stable ICU 69
         */
        ACCOUNTING_NEGATIVE,
    }

    /**
     * An enum declaring how to render the decimal separator. Example outputs when formatting 1 and 1.1
     * in <em>en-US</em>:
     *
     * <ul>
     * <li>AUTO: "1" and "1.1"
     * <li>ALWAYS: "1." and "1.1"
     * </ul>
     *
     * @stable ICU 60
     * @see NumberFormatter
     */
    public static enum DecimalSeparatorDisplay {
        /**
         * Show the decimal separator when there are one or more digits to display after the separator,
         * and do not show it otherwise. This is the default behavior.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        AUTO,

        /**
         * Always show the decimal separator, even if there are no digits to display after the separator.
         *
         * @stable ICU 60
         * @see NumberFormatter
         */
        ALWAYS,
    }

    /**
     * An enum declaring how to render trailing zeros.
     *
     * <ul>
     * <li>AUTO: 0.90, 1.00, 1.10
     * <li>HIDE_IF_WHOLE: 0.90, 1, 1.10
     * </ul>
     *
     * @stable ICU 69
     */
    public static enum TrailingZeroDisplay {
        /**
         * Display trailing zeros according to the settings for minimum fraction and significant digits.
         *
         * @stable ICU 69
         */
        AUTO,

        /**
         * Same as AUTO, but hide trailing zeros after the decimal separator if they are all zero.
         *
         * @stable ICU 69
         */
        HIDE_IF_WHOLE,
    }

    /**
     * Use a default threshold of 3. This means that the third time .format() is called, the data
     * structures get built using the "safe" code path. The first two calls to .format() will trigger the
     * unsafe code path.
     */
    static final long DEFAULT_THRESHOLD = 3;

    /**
     * Private constructor, not designed for instantiation.
     */
    private NumberFormatter() {
    }

    /**
     * Call this method at the beginning of a NumberFormatter fluent chain in which the locale is not
     * currently known at the call site.
     *
     * @return An {@link UnlocalizedNumberFormatter}, to be used for chaining.
     * @stable ICU 60
     */
    public static UnlocalizedNumberFormatter with() {
        return BASE;
    }

    /**
     * Call this method at the beginning of a NumberFormatter fluent chain in which the locale is known
     * at the call site.
     *
     * @param locale
     *            The locale from which to load formats and symbols for number formatting.
     * @return A {@link LocalizedNumberFormatter}, to be used for chaining.
     * @stable ICU 60
     */
    public static LocalizedNumberFormatter withLocale(Locale locale) {
        return BASE.locale(locale);
    }

    /**
     * Call this method at the beginning of a NumberFormatter fluent chain in which the locale is known
     * at the call site.
     *
     * @param locale
     *            The locale from which to load formats and symbols for number formatting.
     * @return A {@link LocalizedNumberFormatter}, to be used for chaining.
     * @stable ICU 60
     */
    public static LocalizedNumberFormatter withLocale(ULocale locale) {
        return BASE.locale(locale);
    }

    /**
     * Call this method at the beginning of a NumberFormatter fluent chain to create an instance based
     * on a given number skeleton string.
     *
     * For more information on number skeleton strings, see:
     * https://unicode-org.github.io/icu/userguide/format_parse/numbers/skeletons.html
     *
     * @param skeleton
     *            The skeleton string off of which to base this NumberFormatter.
     * @return An {@link UnlocalizedNumberFormatter}, to be used for chaining.
     * @throws SkeletonSyntaxException If the given string is not a valid number formatting skeleton.
     * @stable ICU 62
     */
    public static UnlocalizedNumberFormatter forSkeleton(String skeleton) {
        return NumberSkeletonImpl.getOrCreate(skeleton);
    }

    /**
     * Note: In Java, since NumberPropertyMapper is package-private, this method is here so that it is
     * accessible to tests.
     *
     * @internal
     * @deprecated ICU 60 This API is ICU internal only.
     */
    @Deprecated
    public static UnlocalizedNumberFormatter fromDecimalFormat(
            DecimalFormatProperties properties,
            DecimalFormatSymbols symbols,
            DecimalFormatProperties exportedProperties) {
        return NumberPropertyMapper.create(properties, symbols, exportedProperties);
    }
}
