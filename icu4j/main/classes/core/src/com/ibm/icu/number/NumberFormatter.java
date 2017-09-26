// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
package com.ibm.icu.number;

import java.util.Locale;

import com.ibm.icu.impl.number.DecimalFormatProperties;
import com.ibm.icu.impl.number.MacroProps;
import com.ibm.icu.text.DecimalFormatSymbols;
import com.ibm.icu.util.ULocale;

public final class NumberFormatter {

    private static final UnlocalizedNumberFormatter BASE = new UnlocalizedNumberFormatter();

    /**
     * An enum declaring how to render units, including currencies. Example outputs when formatting 123 USD and 123
     * meters in <em>en-CA</em>:
     *
     * <p>
     * <ul>
     * <li>NARROW*: "$123.00" and "123 m"
     * <li>SHORT: "US$ 123.00" and "123 m"
     * <li>FULL_NAME: "123.00 US dollars" and "123 meters"
     * <li>ISO_CODE: "USD 123.00" and undefined behavior
     * <li>HIDDEN: "123.00" and "123"
     * </ul>
     *
     * <p>
     * * The narrow format for currencies is not currently supported; this is a known issue that will be fixed in a
     * future version. See #11666 for more information.
     *
     * <p>
     * This enum is similar to {@link com.ibm.icu.text.MeasureFormat.FormatWidth}.
     *
     * @draft ICU 60
     * @see NumberFormatter
     */
    public static enum UnitWidth {
        /**
         * Print an abbreviated version of the unit name. Similar to SHORT, but always use the shortest available
         * abbreviation or symbol. This option can be used when the context hints at the identity of the unit. For more
         * information on the difference between NARROW and SHORT, see SHORT.
         *
         * <p>
         * In CLDR, this option corresponds to the "Narrow" format for measure units and the "¤¤¤¤¤" placeholder for
         * currencies.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        NARROW,

        /**
         * Print an abbreviated version of the unit name. Similar to NARROW, but use a slightly wider abbreviation or
         * symbol when there may be ambiguity. This is the default behavior.
         *
         * <p>
         * For example, in <em>es-US</em>, the SHORT form for Fahrenheit is "{0} °F", but the NARROW form is "{0}°",
         * since Fahrenheit is the customary unit for temperature in that locale.
         *
         * <p>
         * In CLDR, this option corresponds to the "Short" format for measure units and the "¤" placeholder for
         * currencies.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        SHORT,

        /**
         * Print the full name of the unit, without any abbreviations.
         *
         * <p>
         * In CLDR, this option corresponds to the default format for measure units and the "¤¤¤" placeholder for
         * currencies.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        FULL_NAME,

        /**
         * Use the three-digit ISO XXX code in place of the symbol for displaying currencies. The behavior of this
         * option is currently undefined for use with measure units.
         *
         * <p>
         * In CLDR, this option corresponds to the "¤¤" placeholder for currencies.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        ISO_CODE,

        /**
         * Format the number according to the specified unit, but do not display the unit. For currencies, apply
         * monetary symbols and formats as with SHORT, but omit the currency symbol. For measure units, the behavior is
         * equivalent to not specifying the unit at all.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        HIDDEN,
    }

    /**
     * An enum declaring how to denote positive and negative numbers. Example outputs when formatting 123 and -123 in
     * <em>en-US</em>:
     *
     * <p>
     * <ul>
     * <li>AUTO: "123" and "-123"
     * <li>ALWAYS: "+123" and "-123"
     * <li>NEVER: "123" and "123"
     * <li>ACCOUNTING: "$123" and "($123)"
     * <li>ACCOUNTING_ALWAYS: "+$123" and "($123)"
     * </ul>
     *
     * <p>
     * The exact format, including the position and the code point of the sign, differ by locale.
     *
     * @draft ICU 60
     * @see NumberFormatter
     */
    public static enum SignDisplay {
        /**
         * Show the minus sign on negative numbers, and do not show the sign on positive numbers. This is the default
         * behavior.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        AUTO,

        /**
         * Show the minus sign on negative numbers and the plus sign on positive numbers.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        ALWAYS,

        /**
         * Do not show the sign on positive or negative numbers.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        NEVER,

        /**
         * Use the locale-dependent accounting format on negative numbers, and do not show the sign on positive numbers.
         *
         * <p>
         * The accounting format is defined in CLDR and varies by locale; in many Western locales, the format is a pair
         * of parentheses around the number.
         *
         * <p>
         * Note: Since CLDR defines the accounting format in the monetary context only, this option falls back to the
         * AUTO sign display strategy when formatting without a currency unit. This limitation may be lifted in the
         * future.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        ACCOUNTING,

        /**
         * Use the locale-dependent accounting format on negative numbers, and show the plus sign on positive numbers.
         * For more information on the accounting format, see the ACCOUNTING sign display strategy.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        ACCOUNTING_ALWAYS,
    }

    /**
     * An enum declaring how to render the decimal separator. Example outputs when formatting 1 and 1.1 in
     * <em>en-US</em>:
     *
     * <p>
     * <ul>
     * <li>AUTO: "1" and "1.1"
     * <li>ALWAYS: "1." and "1.1"
     * </ul>
     */
    public static enum DecimalSeparatorDisplay {
        /**
         * Show the decimal separator when there are one or more digits to display after the separator, and do not show
         * it otherwise. This is the default behavior.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        AUTO,

        /**
         * Always show the decimal separator, even if there are no digits to display after the separator.
         *
         * @draft ICU 60
         * @see NumberFormatter
         */
        ALWAYS,
    }

    /**
     * Use a default threshold of 3. This means that the third time .format() is called, the data structures get built
     * using the "safe" code path. The first two calls to .format() will trigger the unsafe code path.
     */
    static final long DEFAULT_THRESHOLD = 3;

    /**
     * Call this method at the beginning of a NumberFormatter fluent chain in which the locale is not currently known at
     * the call site.
     *
     * @return An {@link UnlocalizedNumberFormatter}, to be used for chaining.
     * @draft ICU 60
     */
    public static UnlocalizedNumberFormatter with() {
        return BASE;
    }

    /**
     * Call this method at the beginning of a NumberFormatter fluent chain in which the locale is known at the call
     * site.
     *
     * @param locale
     *            The locale from which to load formats and symbols for number formatting.
     * @return A {@link LocalizedNumberFormatter}, to be used for chaining.
     * @draft ICU 60
     */
    public static LocalizedNumberFormatter withLocale(Locale locale) {
        return BASE.locale(locale);
    }

    /**
     * Call this method at the beginning of a NumberFormatter fluent chain in which the locale is known at the call
     * site.
     *
     * @param locale
     *            The locale from which to load formats and symbols for number formatting.
     * @return A {@link LocalizedNumberFormatter}, to be used for chaining.
     * @draft ICU 60
     */
    public static LocalizedNumberFormatter withLocale(ULocale locale) {
        return BASE.locale(locale);
    }

    /**
     * @internal
     * @deprecated ICU 60 This API is ICU internal only.
     */
    @Deprecated
    public static UnlocalizedNumberFormatter fromDecimalFormat(DecimalFormatProperties properties,
            DecimalFormatSymbols symbols, DecimalFormatProperties exportedProperties) {
        MacroProps macros = NumberPropertyMapper.oldToNew(properties, symbols, exportedProperties);
        return NumberFormatter.with().macros(macros);
    }
}
