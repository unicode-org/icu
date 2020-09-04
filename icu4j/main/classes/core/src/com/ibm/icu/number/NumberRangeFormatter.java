// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.number;

import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
 * The main entrypoint to the formatting of ranges of numbers, including currencies and other units of measurement.
 * <p>
 * Usage example:
 * <pre>
 * NumberRangeFormatter.with()
 *         .identityFallback(RangeIdentityFallback.APPROXIMATELY_OR_SINGLE_VALUE)
 *         .numberFormatterFirst(NumberFormatter.with().unit(MeasureUnit.METER))
 *         .numberFormatterSecond(NumberFormatter.with().unit(MeasureUnit.KILOMETER))
 *         .locale(ULocale.UK)
 *         .formatRange(750, 1.2)
 *         .toString();
 * // => "750 m - 1.2 km"
 * </pre>
 * <p>
 * Like NumberFormatter, NumberRangeFormatter instances (i.e., LocalizedNumberRangeFormatter
 * and UnlocalizedNumberRangeFormatter) are immutable and thread-safe. This API is based on the
 * <em>fluent</em> design pattern popularized by libraries such as Google's Guava.
 *
 * @author sffc
 * @stable ICU 63
 * @see NumberFormatter
 */
public abstract class NumberRangeFormatter {

    /**
     * Defines how to merge fields that are identical across the range sign.
     *
     * @stable ICU 63
     * @see NumberRangeFormatter
     */
    public enum RangeCollapse {
        /**
         * Use locale data and heuristics to determine how much of the string to collapse. Could end up collapsing none,
         * some, or all repeated pieces in a locale-sensitive way.
         * <p>
         * The heuristics used for this option are subject to change over time.
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        AUTO,

        /**
         * Do not collapse any part of the number. Example: "3.2 thousand kilograms – 5.3 thousand kilograms"
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        NONE,

        /**
         * Collapse the unit part of the number, but not the notation, if present. Example: "3.2 thousand – 5.3 thousand
         * kilograms"
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        UNIT,

        /**
         * Collapse any field that is equal across the range sign. May introduce ambiguity on the magnitude of the
         * number. Example: "3.2 – 5.3 thousand kilograms"
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        ALL
    }

    /**
     * Defines the behavior when the two numbers in the range are identical after rounding. To programmatically detect
     * when the identity fallback is used, compare the lower and upper BigDecimals via FormattedNumber.
     *
     * @stable ICU 63
     * @see NumberRangeFormatter
     */
    public static enum RangeIdentityFallback {
        /**
         * Show the number as a single value rather than a range. Example: "$5"
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        SINGLE_VALUE,

        /**
         * Show the number using a locale-sensitive approximation pattern. If the numbers were the same before rounding,
         * show the single value. Example: "~$5" or "$5"
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        APPROXIMATELY_OR_SINGLE_VALUE,

        /**
         * Show the number using a locale-sensitive approximation pattern. Use the range pattern always, even if the
         * inputs are the same. Example: "~$5"
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        APPROXIMATELY,

        /**
         * Show the number as the range of two equal values. Use the range pattern always, even if the inputs are the
         * same. Example (with RangeCollapse.NONE): "$5 – $5"
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        RANGE
    }

    /**
     * Used in the result class FormattedNumberRange to indicate to the user whether the numbers formatted in the range
     * were equal or not, and whether or not the identity fallback was applied.
     *
     * @stable ICU 63
     * @see NumberRangeFormatter
     */
    public static enum RangeIdentityResult {
        /**
         * Used to indicate that the two numbers in the range were equal, even before any rounding rules were applied.
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        EQUAL_BEFORE_ROUNDING,

        /**
         * Used to indicate that the two numbers in the range were equal, but only after rounding rules were applied.
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        EQUAL_AFTER_ROUNDING,

        /**
         * Used to indicate that the two numbers in the range were not equal, even after rounding rules were applied.
         *
         * @stable ICU 63
         * @see NumberRangeFormatter
         */
        NOT_EQUAL
    }

    private static final UnlocalizedNumberRangeFormatter BASE = new UnlocalizedNumberRangeFormatter();

    /**
     * Call this method at the beginning of a NumberRangeFormatter fluent chain in which the locale is not currently
     * known at the call site.
     *
     * @return An {@link UnlocalizedNumberRangeFormatter}, to be used for chaining.
     * @stable ICU 63
     */
    public static UnlocalizedNumberRangeFormatter with() {
        return BASE;
    }

    /**
     * Call this method at the beginning of a NumberRangeFormatter fluent chain in which the locale is known at the call
     * site.
     *
     * @param locale
     *            The locale from which to load formats and symbols for number range formatting.
     * @return A {@link LocalizedNumberRangeFormatter}, to be used for chaining.
     * @stable ICU 63
     */
    public static LocalizedNumberRangeFormatter withLocale(Locale locale) {
        return BASE.locale(locale);
    }

    /**
     * Call this method at the beginning of a NumberRangeFormatter fluent chain in which the locale is known at the call
     * site.
     *
     * @param locale
     *            The locale from which to load formats and symbols for number range formatting.
     * @return A {@link LocalizedNumberRangeFormatter}, to be used for chaining.
     * @stable ICU 63
     */
    public static LocalizedNumberRangeFormatter withLocale(ULocale locale) {
        return BASE.locale(locale);
    }

    /**
     * Private constructor - this class is not designed for instantiation
     */
    private NumberRangeFormatter() {
    }
}
