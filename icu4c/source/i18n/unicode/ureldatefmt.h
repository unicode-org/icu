/*
*****************************************************************************************
* Copyright (C) 2016, International Business Machines
* Corporation and others. All Rights Reserved.
*****************************************************************************************
*/

#ifndef URELDATEFMT_H
#define URELDATEFMT_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING && !UCONFIG_NO_BREAK_ITERATION

#include "unicode/unum.h"
#include "unicode/udisplaycontext.h"
#include "unicode/localpointer.h"

/**
 * \file
 * \brief C API: URelativeDateTimeFormatter, relative date formatting of unit + numeric offset.
 *
 * Provides simple formatting of relative dates, in two ways
 * <ul>
 *   <li>relative dates with a quantity e.g "in 5 days"</li>
 *   <li>relative dates without a quantity e.g "next Tuesday"</li>
 * </ul>
 * <p>
 * This does not provide compound formatting for multiple units,
 * other than the ability to combine a time string with a relative date,
 * as in "next Tuesday at 3:45 PM". It also does not provide support
 * for determining which unit to use, such as deciding between "in 7 days"
 * and "in 1 week".
 *
 * @draft ICU 57
 */

/**
 * The formatting style
 * @stable ICU 54
 */
typedef enum UDateRelativeDateTimeFormatterStyle {
  /**
   * Everything spelled out.
   * @stable ICU 54
   */
  UDAT_STYLE_LONG,

  /**
   * Abbreviations used when possible.
   * @stable ICU 54
   */
  UDAT_STYLE_SHORT,

  /**
   * Use the shortest possible form.
   * @stable ICU 54
   */
  UDAT_STYLE_NARROW,

  /**
   * The number of styles.
   * @stable ICU 54
   */
  UDAT_STYLE_COUNT
} UDateRelativeDateTimeFormatterStyle;

#ifndef U_HIDE_DRAFT_API
/**
 * Represents the unit for formatting a relative date. e.g "in 5 days"
 * or "next year"
 * @draft ICU 57
 */
typedef enum URelativeDateTimeUnit {
    /** @draft ICU 57 */
    UDAT_REL_UNIT_YEAR,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_QUARTER,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_MONTH,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_WEEK,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_DAY,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_HOUR,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_MINUTE,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_SECOND,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_SUNDAY,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_MONDAY,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_TUESDAY,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_WEDNESDAY,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_THURSDAY,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_FRIDAY,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_SATURDAY,
    /** @draft ICU 57 */
    UDAT_REL_UNIT_COUNT
} URelativeDateTimeUnit;
#endif  /* U_HIDE_DRAFT_API */

#ifndef U_HIDE_DRAFT_API

/**
 * Opaque URelativeDateTimeFormatter object for use in C programs.
 * @draft ICU 57
 */
struct URelativeDateTimeFormatter;
typedef struct URelativeDateTimeFormatter URelativeDateTimeFormatter;  /**< C typedef for struct URelativeDateTimeFormatter. @draft ICU 57 */


/**
 * Open a new URelativeDateTimeFormatter object for a given locale using the
 * specified width and capitalizationContext, along with a number formatter
 * (if desired) to override the default formatter that would be used for
 * display of numeric field offsets. The default formatter typically rounds
 * toward 0 and has a minimum of 0 fraction digits and a maximum of 3
 * fraction digits (i.e. it will show as many decimal places as necessary
 * up to 3, without showing trailing 0s).
 *
 * @param locale
 *          The locale
 * @param nfToAdopt
 *          A number formatter to set for this URelativeDateTimeFormatter
 *          object (instead of the default decimal formatter). Ownership of
 *          this UNumberFormat object will pass to the URelativeDateTimeFormatter
 *          object (the URelativeDateTimeFormatter adopts the UNumberFormat),
 *          which becomes responsible for closing it. If the caller wishes to
 *          retain ownership of the UNumberFormat object, the caller must clone
 *          it (with unum_clone) and pass the clone to ureldatefmt_open. May be
 *          NULL to use the default decimal formatter.
 * @param width
 *          The width - wide, short, narrow, etc.
 * @param capitalizationContext
 *          A value from UDisplayContext that pertains to capitalization, e.g.
 *          UDISPCTX_CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE.
 * @param status
 *          A pointer to a UErrorCode to receive any errors.
 * @return
 *          A pointer to a URelativeDateTimeFormatter object for the specified locale,
 *          or NULL if an error occurred.
 * @draft ICU 57
 */
U_DRAFT URelativeDateTimeFormatter* U_EXPORT2
ureldatefmt_open( const char*          locale,
                  UNumberFormat*       nfToAdopt,
                  UDateRelativeDateTimeFormatterStyle width,
                  UDisplayContext      capitalizationContext,
                  UErrorCode*          status );

/**
 * Close a URelativeDateTimeFormatter object. Once closed it may no longer be used.
 * @param reldatefmt
 *            The URelativeDateTimeFormatter object to close.
 * @draft ICU 57
 */
U_DRAFT void U_EXPORT2
ureldatefmt_close(URelativeDateTimeFormatter *reldatefmt);

#if U_SHOW_CPLUSPLUS_API

U_NAMESPACE_BEGIN

/**
 * \class LocalURelativeDateTimeFormatterPointer
 * "Smart pointer" class, closes a URelativeDateTimeFormatter via ureldatefmt_close().
 * For most methods see the LocalPointerBase base class.
 *
 * @see LocalPointerBase
 * @see LocalPointer
 * @draft ICU 57
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalURelativeDateTimeFormatterPointer, URelativeDateTimeFormatter, ureldatefmt_close);

U_NAMESPACE_END

#endif

/**
 * Format a combination of URelativeDateTimeUnit and numeric
 * offset using a numeric style, e.g. "1 week ago", "in 1 week",
 * "5 weeks ago", "in 5 weeks".
 *
 * @param reldatefmt
 *          The URelativeDateTimeFormatter object specifying the
 *          format conventions.
 * @param offset
 *          The signed offset for the specified unit. This will
 *          be formatted according to this object's UNumberFormat
 *          object.
 * @param unit
 *          The unit to use when formatting the relative
 *          date, e.g. UDAT_REL_UNIT_WEEK, UDAT_REL_UNIT_FRIDAY.
 * @param result
 *          A pointer to a buffer to receive the formatted result.
 * @param resultCapacity
 *          The maximum size of result.
 * @param status
 *          A pointer to a UErrorCode to receive any errors. In
 *          case of error status, the contents of result are
 *          undefined.
 * @return
 *          The length of the formatted result; may be greater
 *          than resultCapacity, in which case an error is returned.
 * @draft ICU 57
 */
U_DRAFT int32_t U_EXPORT2
ureldatefmt_formatNumeric( const URelativeDateTimeFormatter* reldatefmt,
                    double                offset,
                    URelativeDateTimeUnit unit,
                    UChar*                result,
                    int32_t               resultCapacity,
                    UErrorCode*           status);

/**
 * Format a combination of URelativeDateTimeUnit and numeric offset
 * using a text style if possible, e.g. "last week", "this week",
 * "next week", "yesterday", "tomorrow". Falls back to numeric
 * style if no appropriate text term is available for the specified
 * offset in the object’s locale.
 *
 * @param reldatefmt
 *          The URelativeDateTimeFormatter object specifying the
 *          format conventions.
 * @param offset
 *          The signed offset for the specified unit.
 * @param unit
 *          The unit to use when formatting the relative
 *          date, e.g. UDAT_REL_UNIT_WEEK, UDAT_REL_UNIT_FRIDAY.
 * @param result
 *          A pointer to a buffer to receive the formatted result.
 * @param resultCapacity
 *          The maximum size of result.
 * @param status
 *          A pointer to a UErrorCode to receive any errors. In
 *          case of error status, the contents of result are
 *          undefined.
 * @return
 *          The length of the formatted result; may be greater
 *          than resultCapacity, in which case an error is returned.
 * @draft ICU 57
 */
U_DRAFT int32_t U_EXPORT2
ureldatefmt_format( const URelativeDateTimeFormatter* reldatefmt,
                    double                offset,
                    URelativeDateTimeUnit unit,
                    UChar*                result,
                    int32_t               resultCapacity,
                    UErrorCode*           status);

/**
 * Combines a relative date string and a time string in this object's
 * locale. This is done with the same date-time separator used for the
 * default calendar in this locale to produce a result such as
 * "yesterday at 3:45 PM".
 *
 * @param reldatefmt
 *          The URelativeDateTimeFormatter object specifying the format conventions.
 * @param relativeDateString
 *          The relative date string.
 * @param relativeDateStringLen
 *          The length of relativeDateString; may be -1 if relativeDateString
 *          is zero-terminated.
 * @param timeString
 *          The time string.
 * @param timeStringLen
 *          The length of timeString; may be -1 if timeString is zero-terminated.
 * @param result
 *          A pointer to a buffer to receive the formatted result.
 * @param resultCapacity
 *          The maximum size of result.
 * @param status
 *          A pointer to a UErrorCode to receive any errors. In case of error status,
 *          the contents of result are undefined.
 * @return
 *          The length of the formatted result; may be greater than resultCapacity,
 *          in which case an error is returned.
 * @draft ICU 57
 */
U_DRAFT int32_t U_EXPORT2
ureldatefmt_combineDateAndTime( const URelativeDateTimeFormatter* reldatefmt,
                    const UChar *     relativeDateString,
                    int32_t           relativeDateStringLen,
                    const UChar *     timeString,
                    int32_t           timeStringLen,
                    UChar*            result,
                    int32_t           resultCapacity,
                    UErrorCode*       status );

#endif  /* U_HIDE_DRAFT_API */

#endif /* !UCONFIG_NO_FORMATTING && !UCONFIG_NO_BREAK_ITERATION */

#endif
