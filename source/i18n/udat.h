/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1996                                       *
*   (C) Copyright International Business Machines Corporation,  1998-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*/

#ifndef UDAT_H
#define UDAT_H

#include "utypes.h"
#include "ucal.h"
#include "unum.h"
/**
 * Date Format C API  consists of functions that convert dates and
 * times from their internal representations to textual form and back again in a
 * language-independent manner. Converting from the internal representation (milliseconds
 * since midnight, January 1, 1970) to text is known as "formatting," and converting
 * from text to millis is known as "parsing."  We currently define only one concrete
 * structure UDateFormat, which can handle pretty much all normal
 * date formatting and parsing actions.
 * <P>
 * Date Format helps you to format and parse dates for any locale. Your code can
 * be completely independent of the locale conventions for months, days of the
 * week, or even the calendar format: lunar vs. solar.
 * <P>
 * To format a date for the current Locale with default time and date style, 
 * use one of the static factory methods:
 * <pre>
 * .    UErrorCode status;
 * .    UFieldPosition pos;
 * .    UChar *myString;
 * .    t_int32 myStrlen=0;
 * .    UDateFormat* dfmt = udat_open(UCAL_DEFAULT, UCAL_DEFAULT, NULL, "PST", &status);
 * .    myStrlen = udat_format(dfmt, myDate, NULL, myStrlen, &pos, &status);
 * .    if(status==U_BUFFER_OVERFLOW_ERROR){
 * .    status=U_ZERO_ERROR;
 * .    myString=(UChar*)malloc(sizeof(UChar) * (myStrlen+1) );
 * .    udat_format(dfmt, myDate, myString, myStrlen+1, &pos, &status);
 * .    }
 * </pre>
 * If you are formatting multiple numbers, it is more efficient to get the
 * format and use it multiple times so that the system doesn't have to fetch the
 * information about the local language and country conventions multiple times.
 * <pre>
 * .    t_int32 i, myStrlen=0;
 * .    UChar* myString;
 * .    UDate myDateArr[] = { 0.0, 100000000.0, 2000000000.0 }; // test values
 * .    UDateFormat* df = udat_open(UCAL_DEFAULT, UCAL_DEFAULT, NULL, "GMT", &status);
 * .    for (i = 0; i < 3; ++i) {
 * .    myStrlen = udat_format(df, myDate, NULL, myStrlen, &pos, &status);
 * .    if(status==U_BUFFER_OVERFLOW_ERROR){
 * .    status=U_ZERO_ERROR;
 * .    myString=(UChar*)malloc(sizeof(UChar) * (myStrlen+1) );
 * .    udat_format(df, myDate, myString, myStrlen+1, &pos, &status);
 * .    }
 * .    printf("%s \n", austrdup(myString) ); //austrdup( a function used to convert UChar* to char*)
 * .    free(myString);
 * .    }
 * </pre>
 * To format a date for a different Locale, specify it in the call to
 * udat_open()
 * <pre>
 * .       UDateFormat* df = udat_open(UDAT_SHORT, UDAT_SHORT, "fr_FR", "GMT", &status);
 * </pre>
 * You can use a DateFormat API udat_parse() to parse.
 * <pre>
 * .       UErrorCode status = U_ZERO_ERROR;
 * .       t_int32 parsepos=0;     
 * .       UDate myDate = udat_parse(df, myString, u_strlen(myString), &parsepos, &status);
 * </pre>
 * . You can pass in different options for the arguments for date and time style 
 * . to control the length of the result; from SHORT to MEDIUM to LONG to FULL. 
 * . The exact result depends on the locale, but generally:
 * . see UDateFormatStyle for more details
 * <ul type=round>
 *   <li>   UDAT_SHORT is completely numeric, such as 12/13/52 or 3:30pm
 *   <li>   UDAT_MEDIUM is longer, such as Jan 12, 1952
 *   <li>   UDAT_LONG is longer, such as January 12, 1952 or 3:30:32pm
 *   <li>   UDAT_FULL is pretty completely specified, such as
 *          Tuesday, April 12, 1952 AD or 3:30:42pm PST.
 * </ul>
 * You can also set the time zone on the format if you wish. 
 * <P>
 * You can also use forms of the parse and format methods with Parse Position and
 * UFieldPosition to allow you to
 * <ul type=round>
 *   <li>   Progressively parse through pieces of a string.
 *   <li>   Align any particular field, or find out where it is for selection
 *          on the screen.
 * </ul>
 */
/** A date formatter */
typedef void* UDateFormat;

/** The possible date/time format styles */
enum UDateFormatStyle {
    /** Full style */
    UDAT_FULL,
    /** Long style */
    UDAT_LONG,
    /** Medium style */
    UDAT_MEDIUM,
    /** Short style */
    UDAT_SHORT,
    /** Default style */
    UDAT_DEFAULT = UDAT_MEDIUM,
    /** No style */
    UDAT_NONE = -1
};
typedef enum UDateFormatStyle UDateFormatStyle;

/**
* Open a new UDateFormat for formatting and parsing dates and times.
* A UDateFormat may be used to format dates in calls to \Ref{udat_format},
* and to parse dates in calls to \Ref{udat_parse}.
* @param timeStyle The style used to format times; one of UDAT_FULL_STYLE, UDAT_LONG_STYLE, 
* UDAT_MEDIUM_STYLE, UDAT_SHORT_STYLE, or UDAT_DEFAULT_STYLE
* @param dateStyle The style used to format dates; one of UDAT_FULL_STYLE, UDAT_LONG_STYLE, 
* UDAT_MEDIUM_STYLE, UDAT_SHORT_STYLE, or UDAT_DEFAULT_STYLE
* @param locale The locale specifying the formatting conventions
* @param tzID A timezone ID specifying the timezone to use.  If 0, use
* the default timezone.
* @param tzIDLength The length of tzID, or -1 if null-terminated.
* @param status A pointer to an UErrorCode to receive any errors
* @return A pointer to a UDateFormat to use for formatting dates and times, or 0 if
* an error occurred.
* @see udat_openPattern
*/
CAPI UDateFormat*
udat_open(UDateFormatStyle  timeStyle, 
          UDateFormatStyle  dateStyle,
          const char        *locale,
	  const UChar       *tzID,
	  int32_t           tzIDLength,
          UErrorCode        *status);

/**
* Open a new UDateFormat for formatting dates and times.
* A UDateFormat may be used to format dates in calls to \Ref{udat_format},
* and to parse dates in calls to \Ref{udat_parse}.
* @param pattern A pattern specifying the format to use.
* @param patternLength The number of characters in the pattern, or -1 if null-terminated.
* @param locale The locale specifying the formatting conventions
* @param status A pointer to an UErrorCode to receive any errors
* @return A pointer to a UDateFormat to use for formatting dates and times, or 0 if
* an error occurred.
* @see udat_open
*/
CAPI UDateFormat*
udat_openPattern(    const   UChar           *pattern, 
            int32_t         patternLength,
            const   char         *locale,
            UErrorCode      *status);

/**
* Close a UDateFormat.
* Once closed, a UDateFormat may no longer be used.
* @param fmt The formatter to close.
*/
CAPI void
udat_close(UDateFormat* format);

/**
 * Open a copy of a UDateFormat.
 * This function performs a deep copy.
 * @param fmt The format to copy
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return A pointer to a UDateFormat identical to fmt.
 */
CAPI UDateFormat*
udat_clone(const UDateFormat *fmt,
       UErrorCode *status);

/**
* Format a date using an UDateFormat.
* The date will be formatted using the conventions specified in \Ref{udat_open}
* or \Ref{udat_openPattern}
* @param format The formatter to use
* @param dateToFormat The date to format
* @param result A pointer to a buffer to receive the formatted number.
* @param resultLength The maximum size of result.
* @param pos If not 0, a UFieldPosition which will receive the information on a specific field.
* @param status A pointer to an UErrorCode to receive any errors
* @return The total buffer size needed; if greater than resultLength, the output was truncated.
* @see udat_parse
*/
CAPI int32_t
udat_format(    const    UDateFormat*    format,
                        UDate           dateToFormat,
                        UChar*          result,
                        int32_t         resultLength,
                        UFieldPosition* position,
                        UErrorCode*     status);

/**
* Parse a string into an date/time using a UDateFormat.
* The date will be parsed using the conventions specified in \Ref{udat_open}
* or \Ref{udat_openPattern}
* @param fmt The formatter to use.
* @param text The text to parse.
* @param textLength The length of text, or -1 if null-terminated.
* @param parsePos If not 0, on input a pointer to an integer specifying the offset at which
* to begin parsing.  If not 0, on output the offset at which parsing ended.
* @param status A pointer to an UErrorCode to receive any errors
* @return The value of the parsed date/time
* @see udat_format
*/
CAPI UDate
udat_parse(    const    UDateFormat*    format,
            const    UChar*          text,
                    int32_t         textLength,
                    int32_t         *parsePos,
                    UErrorCode      *status);

/**
* Determine if an UDateFormat will perform lenient parsing.
* With lenient parsing, the parser may use heuristics to interpret inputs that do not 
* precisely match the pattern. With strict parsing, inputs must match the pattern. 
* @param fmt The formatter to query
* @return TRUE if fmt is set to perform lenient parsing, FALSE otherwise.
* @see udat_setLenient
*/
CAPI bool_t
udat_isLenient(const UDateFormat* fmt);

/**
* Specify whether an UDateFormat will perform lenient parsing.
* With lenient parsing, the parser may use heuristics to interpret inputs that do not 
* precisely match the pattern. With strict parsing, inputs must match the pattern. 
* @param fmt The formatter to set
* @param isLenient TRUE if fmt should perform lenient parsing, FALSE otherwise.
* @see dat_isLenient
*/
CAPI void
udat_setLenient(    UDateFormat*    fmt,
                    bool_t          isLenient);

/**
* Get the UCalendar associated with an UDateFormat.
* A UDateFormat uses a UCalendar to convert a raw value to, for example,
* the day of the week.
* @param fmt The formatter to query.
* @return A pointer to the UCalendar used by fmt.
* @see udat_setCalendar
*/
CAPI const UCalendar*
udat_getCalendar(const UDateFormat* fmt);

/**
* Set the UCalendar associated with an UDateFormat.
* A UDateFormat uses a UCalendar to convert a raw value to, for example,
* the day of the week.
* @param fmt The formatter to set.
* @param calendarToSet A pointer to an UCalendar to be used by fmt.
* @see udat_setCalendar
*/
CAPI void
udat_setCalendar(            UDateFormat*    fmt,
                    const   UCalendar*      calendarToSet);

/**
* Get the UNumberFormat associated with an UDateFormat.
* A UDateFormat uses a UNumberFormat to format numbers within a date,
* for example the day number.
* @param fmt The formatter to query.
* @return A pointer to the UNumberFormat used by fmt to format numbers.
* @see udat_setNumberFormat
*/
CAPI const UNumberFormat*
udat_getNumberFormat(const UDateFormat* fmt);

/**
* Set the UNumberFormat associated with an UDateFormat.
* A UDateFormat uses a UNumberFormat to format numbers within a date,
* for example the day number.
* @param fmt The formatter to set.
* @param numberFormatToSet A pointer to the UNumberFormat to be used by fmt to format numbers.
* @see udat_getNumberFormat
*/
CAPI void
udat_setNumberFormat(            UDateFormat*    fmt,
                        const   UNumberFormat*  numberFormatToSet);

/**
* Get a locale for which date/time formatting patterns are available.
* A UDateFormat in a locale returned by this function will perform the correct
* formatting and parsing for the locale.
* @param index The index of the desired locale.
* @return A locale for which date/time formatting patterns are available, or 0 if none.
* @see udat_countAvailable
*/
CAPI const char*
udat_getAvailable(int32_t index);

/**
* Determine how many locales have date/time  formatting patterns available.
* This function is most useful as determining the loop ending condition for
* calls to \Ref{udat_getAvailable}.
* @return The number of locales for which date/time formatting patterns are available.
* @see udat_getAvailable
*/
CAPI int32_t
udat_countAvailable(void);

/**
* Get the year relative to which all 2-digit years are interpreted.
* For example, if the 2-digit start year is 2100, the year 99 will be
* interpreted as 2199.
* @param fmt The formatter to query.
* @param status A pointer to an UErrorCode to receive any errors
* @return The year relative to which all 2-digit years are interpreted.
* @see udat_Set2DigitYearStart
*/
CAPI UDate
udat_get2DigitYearStart(    const   UDateFormat     *fmt,
                                    UErrorCode      *status);

/**
* Set the year relative to which all 2-digit years will be interpreted.
* For example, if the 2-digit start year is 2100, the year 99 will be
* interpreted as 2199.
* @param fmt The formatter to set.
* @param d The year relative to which all 2-digit years will be interpreted.
* @param status A pointer to an UErrorCode to receive any errors
* @see udat_Set2DigitYearStart
*/
CAPI void
udat_set2DigitYearStart(    UDateFormat     *fmt,
                            UDate           d,
                            UErrorCode      *status);

/**
* Extract the pattern from a UDateFormat.
* The pattern will follow the pattern syntax rules.
* @param fmt The formatter to query.
* @param localized TRUE if the pattern should be localized, FALSE otherwise.
* @param result A pointer to a buffer to receive the pattern.
* @param resultLength The maximum size of result.
* @param status A pointer to an UErrorCode to receive any errors
* @return The total buffer size needed; if greater than resultLength, the output was truncated.
* @see udat_applyPattern
*/
CAPI int32_t
udat_toPattern(    const   UDateFormat     *fmt,
                        bool_t          localized,
                        UChar           *result,
                        int32_t         resultLength,
                        UErrorCode      *status);

/**
* Set the pattern used by an UDateFormat.
* The pattern should follow the pattern syntax rules.
* @param fmt The formatter to set.
* @param localized TRUE if the pattern is localized, FALSE otherwise.
* @param pattern The new pattern
* @param patternLength The length of pattern, or -1 if null-terminated.
* @see udat_toPattern
*/
CAPI void
udat_applyPattern(            UDateFormat     *format,
                            bool_t          localized,
                    const   UChar           *pattern,
                            int32_t         patternLength);

/** The possible types of date format symbols */
enum UDateFormatSymbolType {
    /** The era names, for example AD */
    UDAT_ERAS,
    /** The month names, for example February */
    UDAT_MONTHS,
    /** The short month names, for example Feb. */
    UDAT_SHORT_MONTHS,
    /** The weekday names, for example Monday */
    UDAT_WEEKDAYS,
    /** The short weekday names, for example Mon. */
    UDAT_SHORT_WEEKDAYS,
    /** The AM/PM names, for example AM */
    UDAT_AM_PMS,
    /** The localized characters */
    UDAT_LOCALIZED_CHARS
};
typedef enum UDateFormatSymbolType UDateFormatSymbolType;

/** Date format symbols */
struct UDateFormatSymbols;
typedef struct UDateFormatSymbols UDateFormatSymbols;

/**
* Get the symbols associated with an UDateFormat.
* The symbols are what a UDateFormat uses to represent locale-specific data,
* for example month or day names.
* @param fmt The formatter to query.
* @param type The type of symbols to get.  One of UDAT_ERAS, UDAT_MONTHS, UDAT_SHORT_MONTHS, 
* UDAT_WEEKDAYS, UDAT_SHORT_WEEKDAYS, UDAT_AM_PMS, or UDAT_LOCALIZED_CHARS
* @param index The desired symbol of type type.
* @param result A pointer to a buffer to receive the pattern.
* @param resultLength The maximum size of result.
* @param status A pointer to an UErrorCode to receive any errors
* @return The total buffer size needed; if greater than resultLength, the output was truncated.
* @see udat_countSymbols
* @see udat_setSymbols
*/
CAPI int32_t
udat_getSymbols(const   UDateFormat             *fmt,
                        UDateFormatSymbolType   type,
                        int32_t                 index,
                        UChar                   *result,
                        int32_t                 resultLength,
                        UErrorCode              *status);

/**
* Count the number of particular symbols for an UDateFormat.
* This function is most useful as for detemining the loop termination condition
* for calls to \Ref{udat_getSymbols}.
* @param fmt The formatter to query.
* @param type The type of symbols to count.  One of UDAT_ERAS, UDAT_MONTHS, UDAT_SHORT_MONTHS, 
* UDAT_WEEKDAYS, UDAT_SHORT_WEEKDAYS, UDAT_AM_PMS, or UDAT_LOCALIZED_CHARS
* @return The number of symbols of type type.
* @see udat_getSymbols
* @see udat_setSymbols
*/
CAPI int32_t
udat_countSymbols(    const    UDateFormat                *fmt,
                            UDateFormatSymbolType    type);

/**
* Set the symbols associated with an UDateFormat.
* The symbols are what a UDateFormat uses to represent locale-specific data,
* for example month or day names.
* @param fmt The formatter to set
* @param type The type of symbols to set.  One of UDAT_ERAS, UDAT_MONTHS, UDAT_SHORT_MONTHS, 
* UDAT_WEEKDAYS, UDAT_SHORT_WEEKDAYS, UDAT_AM_PMS, or UDAT_LOCALIZED_CHARS
* @param index The index of the symbol to set of type type.
* @param value The new value
* @param valueLength The length of value, or -1 if null-terminated
* @param status A pointer to an UErrorCode to receive any errors
* @return A pointer to result.
* @see udat_getSymbols
* @see udat_countSymbols
*/
CAPI void
udat_setSymbols(    UDateFormat             *format,
                    UDateFormatSymbolType   type,
                    int32_t                 index,
                    UChar                   *value,
                    int32_t                 valueLength,
                    UErrorCode              *status);

#endif
