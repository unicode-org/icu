// © 2018 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UPRV_INCOMPLETE_CPP11_SUPPORT
#ifndef __UNUMBERFORMATTER_H__
#define __UNUMBERFORMATTER_H__

#include "unicode/ufieldpositer.h"
#include "unicode/umisc.h"


/**
 * \file
 * \brief C-compatible API for localized number formatting; not recommended for C++.
 *
 * This is the C-compatible version of the NumberFormatter API introduced in ICU 60. C++ users should
 * include unicode/numberformatter.h and use the proper C++ APIs.
 *
 * The C API accepts a number skeleton string for specifying the settings for formatting, which covers a
 * very large subset of all possible number formatting features. For more information on number skeleton
 * strings, see unicode/numberformatter.h.
 *
 * When using UNumberFormatter, which is treated as immutable, the results are exported to a mutable
 * UFormattedNumber object, which you subsequently use for populating your string buffer or iterating over
 * the fields.
 *
 * Example code:
 * <pre>
 * // Setup:
 * UErrorCode ec = U_ZERO_ERROR;
 * UNumberFormatter* uformatter = unumf_openFromSkeletonAndLocale(u"round-integer", -1, "en", &ec);
 * UFormattedNumber* uresult = unumf_openResult(&ec);
 * if (U_FAILURE(ec)) { return; }
 *
 * // Format a double:
 * unumf_formatDouble(uformatter, 5142.3, uresult, &ec);
 * if (U_FAILURE(ec)) { return; }
 *
 * // Export the string:
 * int32_t len = unumf_resultToString(uresult, NULL, 0, &ec);
 * UChar* buffer = (UChar*) malloc((len+1)*sizeof(UChar));
 * unumf_resultToString(uresult, buffer, len+1, &ec);
 * if (U_FAILURE(ec)) { return; }
 *
 * // Cleanup:
 * unumf_close(uformatter);
 * unumf_closeResult(uresult);
 * free(buffer);
 * </pre>
 */


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
 * This enum is similar to {@link com.ibm.icu.text.MeasureFormat.FormatWidth}.
 *
 * @draft ICU 60
 */
typedef enum UNumberUnitWidth {
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
     */
            UNUM_UNIT_WIDTH_NARROW,

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
     */
            UNUM_UNIT_WIDTH_SHORT,

    /**
     * Print the full name of the unit, without any abbreviations.
     *
     * <p>
     * In CLDR, this option corresponds to the default format for measure units and the "¤¤¤" placeholder for
     * currencies.
     *
     * @draft ICU 60
     */
            UNUM_UNIT_WIDTH_FULL_NAME,

    /**
     * Use the three-digit ISO XXX code in place of the symbol for displaying currencies. The behavior of this
     * option is currently undefined for use with measure units.
     *
     * <p>
     * In CLDR, this option corresponds to the "¤¤" placeholder for currencies.
     *
     * @draft ICU 60
     */
            UNUM_UNIT_WIDTH_ISO_CODE,

    /**
     * Format the number according to the specified unit, but do not display the unit. For currencies, apply
     * monetary symbols and formats as with SHORT, but omit the currency symbol. For measure units, the behavior is
     * equivalent to not specifying the unit at all.
     *
     * @draft ICU 60
     */
            UNUM_UNIT_WIDTH_HIDDEN,

    /**
     * One more than the highest UNumberUnitWidth value.
     *
     * @internal ICU 60: The numeric value may change over time; see ICU ticket #12420.
     */
            UNUM_UNIT_WIDTH_COUNT
} UNumberUnitWidth;

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
 * @draft ICU 61 -- TODO: This should be renamed to UNumberGroupingStrategy before promoting to stable,
 * for consistency with the other enums.
 */
typedef enum UGroupingStrategy {
    /**
     * Do not display grouping separators in any locale.
     *
     * @draft ICU 61
     */
            UNUM_GROUPING_OFF,

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
     * @draft ICU 61
     */
            UNUM_GROUPING_MIN2,

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
     * @draft ICU 61
     */
            UNUM_GROUPING_AUTO,

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
     * @draft ICU 61
     */
            UNUM_GROUPING_ON_ALIGNED,

    /**
     * Use the Western defaults: groups of 3 and enabled for all numbers 1000 or greater. Do not use
     * locale data for determining the grouping strategy.
     *
     * @draft ICU 61
     */
            UNUM_GROUPING_THOUSANDS,

    /**
     * One more than the highest UNumberSignDisplay value.
     *
     * @internal ICU 62: The numeric value may change over time; see ICU ticket #12420.
     */
            UNUM_GROUPING_COUNT

} UGroupingStrategy;

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
 * @draft ICU 60
 */
typedef enum UNumberSignDisplay {
    /**
     * Show the minus sign on negative numbers, and do not show the sign on positive numbers. This is the default
     * behavior.
     *
     * @draft ICU 60
     */
            UNUM_SIGN_AUTO,

    /**
     * Show the minus sign on negative numbers and the plus sign on positive numbers, including zero.
     * To hide the sign on zero, see {@link UNUM_SIGN_EXCEPT_ZERO}.
     *
     * @draft ICU 60
     */
            UNUM_SIGN_ALWAYS,

    /**
     * Do not show the sign on positive or negative numbers.
     *
     * @draft ICU 60
     */
            UNUM_SIGN_NEVER,

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
     */
            UNUM_SIGN_ACCOUNTING,

    /**
     * Use the locale-dependent accounting format on negative numbers, and show the plus sign on
     * positive numbers, including zero. For more information on the accounting format, see the
     * ACCOUNTING sign display strategy. To hide the sign on zero, see
     * {@link UNUM_SIGN_ACCOUNTING_EXCEPT_ZERO}.
     *
     * @draft ICU 60
     */
            UNUM_SIGN_ACCOUNTING_ALWAYS,

    /**
     * Show the minus sign on negative numbers and the plus sign on positive numbers. Do not show a
     * sign on zero.
     *
     * @draft ICU 61
     */
            UNUM_SIGN_EXCEPT_ZERO,

    /**
     * Use the locale-dependent accounting format on negative numbers, and show the plus sign on
     * positive numbers. Do not show a sign on zero. For more information on the accounting format,
     * see the ACCOUNTING sign display strategy.
     *
     * @draft ICU 61
     */
            UNUM_SIGN_ACCOUNTING_EXCEPT_ZERO,

    /**
     * One more than the highest UNumberSignDisplay value.
     *
     * @internal ICU 60: The numeric value may change over time; see ICU ticket #12420.
     */
            UNUM_SIGN_COUNT
} UNumberSignDisplay;

/**
 * An enum declaring how to render the decimal separator.
 *
 * <p>
 * <ul>
 * <li>UNUM_DECIMAL_SEPARATOR_AUTO: "1", "1.1"
 * <li>UNUM_DECIMAL_SEPARATOR_ALWAYS: "1.", "1.1"
 * </ul>
 */
typedef enum UNumberDecimalSeparatorDisplay {
    /**
     * Show the decimal separator when there are one or more digits to display after the separator, and do not show
     * it otherwise. This is the default behavior.
     *
     * @draft ICU 60
     */
            UNUM_DECIMAL_SEPARATOR_AUTO,

    /**
     * Always show the decimal separator, even if there are no digits to display after the separator.
     *
     * @draft ICU 60
     */
            UNUM_DECIMAL_SEPARATOR_ALWAYS,

    /**
     * One more than the highest UNumberDecimalSeparatorDisplay value.
     *
     * @internal ICU 60: The numeric value may change over time; see ICU ticket #12420.
     */
            UNUM_DECIMAL_SEPARATOR_COUNT
} UNumberDecimalSeparatorDisplay;


/**
 * C-compatible version of icu::number::LocalizedNumberFormatter.
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
typedef struct UNumberFormatter UNumberFormatter;


/**
 * C-compatible version of icu::number::FormattedNumber.
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
typedef struct UFormattedNumber UFormattedNumber;


/**
 * Creates a new UNumberFormatter from the given skeleton string and locale. This is currently the only
 * method for creating a new UNumberFormatter.
 *
 * For more details on skeleton strings, see the documentation in numberformatter.h. For more details on
 * the usage of this API, see the documentation at the top of unumberformatter.h.
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
U_DRAFT UNumberFormatter* U_EXPORT2
unumf_openFromSkeletonAndLocale(const UChar* skeleton, int32_t skeletonLen, const char* locale,
                                UErrorCode* ec);


/**
 * Creates a new UFormattedNumber for holding the result of a number formatting operation.
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
U_DRAFT UFormattedNumber* U_EXPORT2
unumf_openResult(UErrorCode* ec);


/**
 * Uses a UNumberFormatter to format an integer to a UFormattedNumber. A string, field position, and other
 * information can be retrieved from the UFormattedNumber.
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
U_DRAFT void U_EXPORT2
unumf_formatInt(const UNumberFormatter* uformatter, int64_t value, UFormattedNumber* uresult,
                UErrorCode* ec);


/**
 * Uses a UNumberFormatter to format a double to a UFormattedNumber. A string, field position, and other
 * information can be retrieved from the UFormattedNumber.
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
U_DRAFT void U_EXPORT2
unumf_formatDouble(const UNumberFormatter* uformatter, double value, UFormattedNumber* uresult,
                   UErrorCode* ec);


/**
 * Uses a UNumberFormatter to format a decimal number to a UFormattedNumber. A string, field position, and
 * other information can be retrieved from the UFormattedNumber.
 *
 * The syntax of the unformatted number is a "numeric string" as defined in the Decimal Arithmetic
 * Specification, available at http://speleotrove.com/decimal
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
U_DRAFT void U_EXPORT2
unumf_formatDecimal(const UNumberFormatter* uformatter, const char* value, int32_t valueLen,
                    UFormattedNumber* uresult, UErrorCode* ec);


/**
 * Extracts the result number string out of a UFormattedNumber to a UChar buffer if possible.
 * If bufferCapacity is greater than the required length, a terminating NUL is written.
 * If bufferCapacity is less than the required length, an error code is set.
 *
 * If NULL is passed as the buffer argument, the required length is returned without setting an error.
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @return The required length.
 *
 * @draft ICU 62
 */
U_DRAFT int32_t U_EXPORT2
unumf_resultToString(const UFormattedNumber* uresult, UChar* buffer, int32_t bufferCapacity,
                     UErrorCode* ec);


/**
 * Determines the start and end indices of the first occurrence of the given field in the output string.
 * This allows you to determine the locations of the integer part, fraction part, and sign.
 *
 * If a field occurs multiple times in an output string, such as a grouping separator, this method will
 * only ever return the first occurrence. Use unumf_resultGetAllFields() to access all occurrences of an
 * attribute.
 *
 * @param fpos
 *         A pointer to a UFieldPosition. On input, position->field is read. On output,
 *         position->beginIndex and position->endIndex indicate the beginning and ending indices of field
 *         number position->field, if such a field exists.
 */
U_DRAFT void U_EXPORT2
unumf_resultGetField(const UFormattedNumber* uresult, UFieldPosition* ufpos, UErrorCode* ec);


/**
 * Populates the given iterator with all fields in the formatted output string. This allows you to
 * determine the locations of the integer part, fraction part, and sign.
 *
 * If you need information on only one field, consider using unumf_resultGetField().
 *
 * @param fpositer
 *         A pointer to a UFieldPositionIterator created by {@link #ufieldpositer_open}. Iteration
 *         information already present in the UFieldPositionIterator is deleted, and the iterator is reset
 *         to apply to the fields in the formatted string created by this function call. The field values
 *         and indexes returned by {@link #ufieldpositer_next} represent fields denoted by
 *         the UNumberFormatFields enum. Fields are not returned in a guaranteed order. Fields cannot
 *         overlap, but they may nest. For example, 1234 could format as "1,234" which might consist of a
 *         grouping separator field for ',' and an integer field encompassing the entire string.
 */
U_DRAFT void U_EXPORT2
unumf_resultGetAllFields(const UFormattedNumber* uresult, UFieldPositionIterator* ufpositer,
                         UErrorCode* ec);


/**
 * Releases the UNumberFormatter created by unumf_openFromSkeletonAndLocale().
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
U_DRAFT void U_EXPORT2
unumf_close(UNumberFormatter* uformatter);


/**
 * Releases the UFormattedNumber created by unumf_openResult().
 *
 * NOTE: This is a C-compatible API; C++ users should build against numberformatter.h instead.
 *
 * @draft ICU 62
 */
U_DRAFT void U_EXPORT2
unumf_closeResult(const UFormattedNumber* uresult);


#endif //__UNUMBERFORMATTER_H__
#endif /* #if !UCONFIG_NO_FORMATTING */



















































