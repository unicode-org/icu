// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
 * The main entrypoint to the formatting of ranges of numbers, including currencies and other units of measurement.
 *
 * @author sffc
 * @draft ICU 63
 * @provisional This API might change or be removed in a future release.
 * @see NumberFormatter
 */
public abstract class NumberRangeFormatter {

    /**
     * Defines how to merge fields that are identical across the range sign.
     *
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     */
    public enum RangeCollapse {
        /**
         * Use locale data and heuristics to determine how much of the string to collapse. Could end up collapsing none,
         * some, or all repeated pieces in a locale-sensitive way.
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         * @see NumberRangeFormatter
         */
        AUTO,

        /**
         * Do not collapse any part of the number. Example: "3.2 thousand kilograms – 5.3 thousand kilograms"
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         * @see NumberRangeFormatter
         */
        NONE,

        /**
         * Collapse the unit part of the number, but not the notation, if present. Example: "3.2 thousand – 5.3 thousand
         * kilograms"
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         * @see NumberRangeFormatter
         */
        UNIT,

        /**
         * Collapse any field that is equal across the range sign. May introduce ambiguity on the magnitude of the
         * number. Example: "3.2 – 5.3 thousand kilograms"
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         * @see NumberRangeFormatter
         */
        ALL
    }

    /**
     * Defines the behavior when the two numbers in the range are identical after rounding. To programmatically detect
     * when the identity fallback is used, compare the lower and upper BigDecimals via FormattedNumber.
     *
     * @draft ICU 63
     * @provisional This API might change or be removed in a future release.
     * @see NumberRangeFormatter
     */
    public enum IdentityFallback {
        /**
         * Show the number as a single value rather than a range. Example: "$5"
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         * @see NumberRangeFormatter
         */
        SINGLE_VALUE,

        /**
         * Show the number using a locale-sensitive approximation pattern. If the numbers were the same before rounding,
         * show the single value. Example: "~$5" or "$5"
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         * @see NumberRangeFormatter
         */
        APPROXIMATELY_OR_SINGLE_VALUE,

        /**
         * Show the number using a locale-sensitive approximation pattern. Use the range pattern always, even if the
         * inputs are the same. Example: "~$5"
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         * @see NumberRangeFormatter
         */
        APPROXIMATELY,

        /**
         * Show the number as the range of two equal values. Use the range pattern always, even if the inputs are the
         * same. Example (with RangeCollapse.NONE): "$5 – $5"
         *
         * @draft ICU 63
         * @provisional This API might change or be removed in a future release.
         * @see NumberRangeFormatter
         */
        RANGE
    }

    /**
     * Defines the behavior when the two numbers in the range are identical after rounding. To programmatically detect
     * when the identity fallback is used, compare the lower and upper BigDecimals via FormattedNumber.
     */

    public static enum RangeIdentityFallback {
        /**
         * Show the number as a single value rather than a range. Example: "$5"
         */
        SINGLE_VALUE,

        /**
         * Show the number using a locale-sensitive approximation pattern. If the numbers were the same before rounding,
         * show the single value. Example: "~$5" or "$5"
         */
        APPROXIMATELY_OR_SINGLE_VALUE,

        /**
         * Show the number using a locale-sensitive approximation pattern. Use the range pattern always, even if the
         * inputs are the same. Example: "~$5"
         */
        APPROXIMATELY,

        /**
         * Show the number as the range of two equal values. Use the range pattern always, even if the inputs are the
         * same. Example (with RangeCollapse.NONE): "$5 – $5"
         */
        RANGE
    }

    private static final UnlocalizedNumberRangeFormatter BASE = new UnlocalizedNumberRangeFormatter();

    public static UnlocalizedNumberRangeFormatter with() {
        return BASE;
    }

    public static LocalizedNumberRangeFormatter withLocale(Locale locale) {
        return BASE.locale(locale);
    }

    public static LocalizedNumberRangeFormatter withLocale(ULocale locale) {
        return BASE.locale(locale);
    }

}
