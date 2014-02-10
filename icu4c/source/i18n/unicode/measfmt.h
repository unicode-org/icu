/*
**********************************************************************
* Copyright (c) 2004-2014, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: April 20, 2004
* Since: ICU 3.0
**********************************************************************
*/
#ifndef MEASUREFORMAT_H
#define MEASUREFORMAT_H

#include "unicode/utypes.h"
#include "unicode/measure.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/format.h"
#include "unicode/udat.h"

/**
 * \file 
 * \brief C++ API: Formatter for measure objects.
 */

/**
 * Constants for various widths.
 * There are 3 widths: Wide, Short, Narrow.
 * For example, for English, when formatting "3 hours"
 * Wide is "3 hours"; short is "3 hrs"; narrow is "3h"
 * @draft ICU 53
 */
enum UMeasureFormatWidth {

    // Wide, short, and narrow must be first and in this order.
    /**
     * Spell out measure units.
     * @draft ICU 53 
     */
    UMEASFMT_WIDTH_WIDE,
 
    /**
     * Abbreviate measure units.
     * @draft ICU 53
     */
    UMEASFMT_WIDTH_SHORT,

    /**
     * Use symbols for measure units when possible.
     * @draft ICU 53
     */
    UMEASFMT_WIDTH_NARROW,

    /**
     * Completely omit measure units when possible. For example, format
     * '5 hours, 37 minutes' as '5:37'
     * @draft ICU 53
     */
    UMEASFMT_WIDTH_NUMERIC,

    /**
     * Count of values in this enum.
     * @draft ICU 53
     */
    UMEASFMT_WIDTH_COUNT
};
/** @draft ICU 53 */
typedef enum UMeasureFormatWidth UMeasureFormatWidth; 

U_NAMESPACE_BEGIN

class NumberFormat;
class PluralRules;
class MeasureFormatCacheData;
class SharedNumberFormat;
class SharedPluralRules;
class QuantityFormatter;
class ListFormatter;
class DateFormat;

/**
 * 
 * A formatter for measure objects.
 *
 * @see Format
 * @author Alan Liu
 * @stable ICU 3.0
 */
class U_I18N_API MeasureFormat : public Format {
 public:
    using Format::parseObject;
    using Format::format;

    /**
     * Constructor.
     * @draft ICU 53.
     */
    MeasureFormat(
            const Locale &locale, UMeasureFormatWidth width, UErrorCode &status);

    /**
     * Constructor.
     * @draft ICU 53.
     */
    MeasureFormat(
            const Locale &locale,
            UMeasureFormatWidth width,
            NumberFormat *nfToAdopt,
            UErrorCode &status);

    /**
     * Copy constructor.
     * @draft ICU 53.
     */
    MeasureFormat(const MeasureFormat &other);

    /**
     * Assignment operator.
     * @draft ICU 53.
     */
    MeasureFormat &operator=(const MeasureFormat &rhs);
    
    /**
     * Destructor.
     * @stable ICU 3.0
     */
    virtual ~MeasureFormat();

    /**
     * Return true if given Format objects are semantically equal.
     * @draft ICU 53
     */
    virtual UBool operator==(const Format &other) const;

    /**
     * Clones this object polymorphically.
     * @draft ICU 53
     */
    virtual Format *clone() const;

    /**
     * Formats object to produce a string.
     * @draft ICU 53
     */
    virtual UnicodeString &format(
            const Formattable &obj,
            UnicodeString &appendTo,
            FieldPosition &pos,
            UErrorCode &status) const;

    /**
     * Parse a string to produce an object. This implementation sets
     * status to U_UNSUPPORTED_ERROR.
     *
     * @draft ICU 53
     */
    virtual void parseObject(
            const UnicodeString &source,
            Formattable &reslt,
            ParsePosition &pos) const;

    /**
     * Formats measure objects to produce a string.
     * @param measures measure objects.
     * @param measureCount the number of measure objects.
     * @param appendTo formatted string appended here.
     * @param pos the field position.
     * @param status the error.
     * @return appendTo reference
     *
     * @draft ICU 53
     */
    UnicodeString &formatMeasures(
            const Measure *measures,
            int32_t measureCount,
            UnicodeString &appendTo,
            FieldPosition &pos,
            UErrorCode &status) const;


    /**
     * Return a formatter for CurrencyAmount objects in the given
     * locale.
     * @param locale desired locale
     * @param ec input-output error code
     * @return a formatter object, or NULL upon error
     * @stable ICU 3.0
     */
    static MeasureFormat* U_EXPORT2 createCurrencyFormat(const Locale& locale,
                                               UErrorCode& ec);

    /**
     * Return a formatter for CurrencyAmount objects in the default
     * locale.
     * @param ec input-output error code
     * @return a formatter object, or NULL upon error
     * @stable ICU 3.0
     */
    static MeasureFormat* U_EXPORT2 createCurrencyFormat(UErrorCode& ec);

 protected:
    /**
     * Default constructor.
     * @stable ICU 3.0
     */
    MeasureFormat();

#ifndef U_HIDE_INTERNAL_API 

    /**
     * ICU use only.
     * Initialize or change MeasureFormat class from subclass.
     * @internal.
     */
    void initMeasureFormat(
            const Locale &locale,
            UMeasureFormatWidth width,
            NumberFormat *nfToAdopt,
            UErrorCode &status);

    /**
     * ICU use only.
     * Allows subclass to change locale. Note that this method also changes
     * the NumberFormat object. Returns TRUE if locale changed; FALSE if no
     * change was made.
     * @internal.
     */
    UBool setMeasureFormatLocale(const Locale &locale, UErrorCode &status);

    /**
     * ICU use only.
     * Let subclass change NumberFormat.
     * @internal.
     */
    void adoptNumberFormat(NumberFormat *nfToAdopt, UErrorCode &status);

    /**
     * ICU use only.
     * @internal.
     */
    const NumberFormat &getNumberFormat() const;

    /**
     * ICU use only.
     * @internal.
     */
    const PluralRules &getPluralRules() const;

    /**
     * ICU use only.
     * @internal.
     */
    Locale getLocale(UErrorCode &status) const;

    /**
     * ICU use only.
     * @internal.
     */
    const char *getLocaleID(UErrorCode &status) const;

#endif /* U_HIDE_INTERNAL_API */

 private:
    const MeasureFormatCacheData *cache;
    const SharedNumberFormat *numberFormat;
    const SharedPluralRules *pluralRules;
    UMeasureFormatWidth width;    

    // Declared outside of MeasureFormatSharedData because ListFormatter
    // objects are relatively cheap to copy; therefore, they don't need to be
    // shared across instances.
    ListFormatter *listFormatter;

    const QuantityFormatter *getQuantityFormatter(
            int32_t index,
            int32_t widthIndex,
            UErrorCode &status) const;

    UnicodeString &formatMeasure(
        const Measure &measure,
        UnicodeString &appendTo,
        FieldPosition &pos,
        UErrorCode &status) const;

    UnicodeString &formatMeasuresSlowTrack(
        const Measure *measures,
        int32_t measureCount,
        UnicodeString& appendTo,
        FieldPosition& pos,
        UErrorCode& status) const;

    UnicodeString &formatNumeric(
        const Formattable *hms,  // always length 3: [0] is hour; [1] is
                                 // minute; [2] is second.
        int32_t bitMap,   // 1=hour set, 2=minute set, 4=second set
        UnicodeString &appendTo,
        UErrorCode &status) const;

    UnicodeString &formatNumeric(
        UDate date,
        const DateFormat &dateFmt,
        UDateFormatField smallestField,
        const Formattable &smallestAmount,
        UnicodeString &appendTo,
        UErrorCode &status) const;
};

U_NAMESPACE_END

#endif // #if !UCONFIG_NO_FORMATTING
#endif // #ifndef MEASUREFORMAT_H
