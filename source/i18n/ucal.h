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

#ifndef UCAL_H
#define UCAL_H

#include "utypes.h"
/**
 * @name Calendar C API
 *
 * UCalendar C API is used  for converting between a <code>UDate</code> object
 * and a set of integer fields such as <code>UCAL_YEAR</code>, <code>UCAL_MONTH</code>, 
 * <code>UCAL_DAY</code>, <code>UCAL_HOUR</code>, and so on.
 * (A <code>UDate</code> object represents a specific instant in
 * time with millisecond precision. See
 * {@link UDate}
 * for information about the <code>UDate</code> .)
 *
 * <p>
 * Types of <code>UCalendar</code> interpret a <code>UDate</code>
 * according to the rules of a specific calendar system. The U_CAPI
 * provides the enum UCalendarType with UCAL_TRADITIONAL and 
 * UCAL_GREGORIAN.
 * <p>
 * Like other locale-sensitive C API, calendar API  provides a
 * function, <code>ucal_open()</code>, which returns a pointer to
 * <code>UCalendar</code> whose time fields have been initialized 
 * with the current date and time. We need to specify the type of
 * calendar to be opened and the  timezoneId. 
 * <blockquote>
 * <pre>
 * UCalendar *caldef;
 * UChar *tzId;
 * UErrorCode status;
 * tzId=(UChar*)malloc(sizeof(UChar) * (strlen("PST") +1) );
 * u_uastrcpy(tzId, "PST");
 * caldef=ucal_open(tzID, u_strlen(tzID), NULL, UCAL_TRADITIONAL, &status);
 * </pre>
 * </blockquote>
 *
 * <p>
 * A <code>UCalendar</code> object can produce all the time field values
 * needed to implement the date-time formatting for a particular language
 * and calendar style (for example, Japanese-Gregorian, Japanese-Traditional).
 *
 * <p>
 * When computing a <code>UDate</code> from time fields, two special circumstances
 * may arise: there may be insufficient information to compute the
 * <code>UDate</code> (such as only year and month but no day in the month),
 * or there may be inconsistent information (such as "Tuesday, July 15, 1996"
 * -- July 15, 1996 is actually a Monday).
 *
 * <p>
 * <strong>Insufficient information.</strong> The calendar will use default
 * information to specify the missing fields. This may vary by calendar; for
 * the Gregorian calendar, the default for a field is the same as that of the
 * start of the epoch: i.e., UCAL_YEAR = 1970, UCAL_MONTH = JANUARY, UCAL_DATE = 1, etc.
 *
 * <p>
 * <strong>Inconsistent information.</strong> If fields conflict, the calendar
 * will give preference to fields set more recently. For example, when
 * determining the day, the calendar will look for one of the following
 * combinations of fields.  The most recent combination, as determined by the
 * most recently set single field, will be used.
 *
 * <blockquote>
 * <pre>
 * UCAL_MONTH + UCAL_DAY_OF_MONTH
 * UCAL_MONTH + UCAL_WEEK_OF_MONTH + UCAL_DAY_OF_WEEK
 * UCAL_MONTH + UCAL_DAY_OF_WEEK_IN_MONTH + UCAL_DAY_OF_WEEK
 * UCAL_DAY_OF_YEAR
 * UCAL_DAY_OF_WEEK + UCAL_WEEK_OF_YEAR
 * </pre>
 * </blockquote>
 *
 * For the time of day:
 *
 * <blockquote>
 * <pre>
 * UCAL_HOUR_OF_DAY
 * UCAL_AM_PM + UCAL_HOUR
 * </pre>
 * </blockquote>
 *
 * <p>
 * <strong>Note:</strong> for some non-Gregorian calendars, different
 * fields may be necessary for complete disambiguation. For example, a full
 * specification of the historial Arabic astronomical calendar requires year,
 * month, day-of-month <em>and</em> day-of-week in some cases.
 *
 * <p>
 * <strong>Note:</strong> There are certain possible ambiguities in
 * interpretation of certain singular times, which are resolved in the
 * following ways:
 * <ol>
 *     <li> 24:00:00 "belongs" to the following day. That is,
 *          23:59 on Dec 31, 1969 &lt; 24:00 on Jan 1, 1970 &lt; 24:01:00 on Jan 1, 1970
 *
 *     <li> Although historically not precise, midnight also belongs to "am",
 *          and noon belongs to "pm", so on the same day,
 *          12:00 am (midnight) &lt; 12:01 am, and 12:00 pm (noon) &lt; 12:01 pm
 * </ol>
 *
 * <p>
 * The date or time format strings are not part of the definition of a
 * calendar, as those must be modifiable or overridable by the user at
 * runtime. Use {@link UDateFormat}
 * to format dates.
 *
 * <p>
 * <code>Calendar</code> provides an API for field "rolling", where fields
 * can be incremented or decremented, but wrap around. For example, rolling the
 * month up in the date <code>December 12, <b>1996</b></code> results in
 * <code>January 12, <b>1996</b></code>.
 *
 * <p>
 * <code>Calendar</code> also provides a date arithmetic function for
 * adding the specified (signed) amount of time to a particular time field.
 * For example, subtracting 5 days from the date <code>September 12, 1996</code>
 * results in <code>September 7, 1996</code>.
 *
 */

/** A calendar.
 *  For usage in C programs.
 */
typedef void* UCalendar;

/** Possible types of UCalendars */
enum UCalendarType {
  /** A traditional calendar for the locale */
  UCAL_TRADITIONAL,
  /** The Gregorian calendar */
  UCAL_GREGORIAN
};
typedef enum UCalendarType UCalendarType;
    
/** Possible fields in a UCalendar */
enum UCalendarDateFields {
  /** Era field */
  UCAL_ERA,
  /** Year field */
  UCAL_YEAR,
  /** Month field */
  UCAL_MONTH,
  /** Week of year field */
  UCAL_WEEK_OF_YEAR,
  /** Week of month field */
  UCAL_WEEK_OF_MONTH,
  /** Date field */
  UCAL_DATE,
  /** Day of year field */
  UCAL_DAY_OF_YEAR,
  /** Day of week field */
  UCAL_DAY_OF_WEEK,
  /** Day of week in month field */
  UCAL_DAY_OF_WEEK_IN_MONTH,
  /** AM/PM field */
  UCAL_AM_PM,
  /** Hour field */
  UCAL_HOUR,
  /** Hour of day field */
  UCAL_HOUR_OF_DAY,
  /** Minute field */
  UCAL_MINUTE,
  /** Second field */
  UCAL_SECOND,
  /** Millisecond field */
  UCAL_MILLISECOND,
  /** Zone offset field */
  UCAL_ZONE_OFFSET,
  /** DST offset field */
  UCAL_DST_OFFSET,
  /** Field count */
  UCAL_FIELD_COUNT
};
typedef enum UCalendarDateFields UCalendarDateFields;
    /**
     * Useful constant for days of week. Note: Calendar day-of-week is 1-based. Clients
     * who create locale resources for the field of first-day-of-week should be aware of
     * this. For instance, in US locale, first-day-of-week is set to 1, i.e., UCAL_SUNDAY.
     */
/** Possible days of the week in a UCalendar */
enum UCalendarDaysOfWeek {
  /** Sunday */
  UCAL_SUNDAY = 1,
  /** Monday */
  UCAL_MONDAY,
  /** Tuesday */
  UCAL_TUESDAY,
  /** Wednesday */
  UCAL_WEDNESDAY,
  /** Thursday */
  UCAL_THURSDAY,
  /** Friday */
  UCAL_FRIDAY,
  /** Saturday */
  UCAL_SATURDAY
};
typedef enum UCalendarDaysOfWeek UCalendarDaysOfWeek;

/** Possible months in a UCalendar. Note: Calendar month is 0-based.*/
enum UCalendarMonths {
  /** January */
  UCAL_JANUARY,
  /** February */
  UCAL_FEBRUARY,
  /** March */
  UCAL_MARCH,
  /** April */
  UCAL_APRIL,
  /** May */
  UCAL_MAY,
  /** June */
  UCAL_JUNE,
  /** July */
  UCAL_JULY,
  /** August */
  UCAL_AUGUST,
  /** September */
  UCAL_SEPTEMBER,
  /** October */
  UCAL_OCTOBER,
  /** November */
  UCAL_NOVEMBER,
  /** December */
  UCAL_DECEMBER,
  /** Undecimber */
  UCAL_UNDECIMBER
};
typedef enum UCalendarMonths UCalendarMonths;

/** Possible AM/PM values in a UCalendar */
enum UCalendarAMPMs {
    /** AM */
  UCAL_AM,
  /** PM */
  UCAL_PM
};
typedef enum UCalendarAMPMs UCalendarAMPMs;

/**
* Get an available TimeZone ID.
* A Timezone ID is a string of the form "America/Los Angeles".
* @param rawOffset The desired GMT offset
* @param index The index of the desired TimeZone.
* @param status A pointer to an UErrorCode to receive any errors
* @return The requested TimeZone ID, or 0 if not found
* @see ucal_countAvailableTZIDs
*/
U_CAPI const UChar*
ucal_getAvailableTZIDs(        int32_t         rawOffset,
                int32_t         index,
                UErrorCode*     status);

/**
* Determine how many TimeZones exist with a certain offset.
* This function is most useful as determining the loop ending condition for
* calls to \Ref{ucal_getAvailableTZIDs}.
* @param rawOffset The desired GMT offset.
* @return The number of TimeZones with rawOffset.
* @see ucal_getAvailableTZIDs
*/
U_CAPI int32_t
ucal_countAvailableTZIDs(int32_t rawOffset);

/**
* Get the current date and time.
* The value returned is represented as milliseconds from the epoch.
* @return The current date and time.
*/
U_CAPI UDate 
ucal_getNow(void);

/**
* Open a UCalendar.
* A UCalendar may be used to convert a millisecond value to a year, 
* month, and day.
* @param zoneID The desired TimeZone ID.  If 0, use the default time zone.
* @param len The length of zoneID, or -1 if null-terminated.
* @param locale The desired locale
* @param type The type of UCalendar to open.
* @param status A pointer to an UErrorCode to receive any errors
* @return A pointer to a UCalendar, or 0 if an error occurred.
*/
U_CAPI UCalendar* 
ucal_open(    const    UChar*          zoneID,
            int32_t        len,
        const    char*           locale,
            UCalendarType     type,
            UErrorCode*    status);

/**
* Close a UCalendar.
* Once closed, a UCalendar may no longer be used.
* @param cal The UCalendar to close.
*/
U_CAPI void
ucal_close(UCalendar *cal);

/**
* Set the TimeZone used by a UCalendar.
* A UCalendar uses a timezone for converting from Greenwich time to local time.
* @param cal The UCalendar to set.
* @param zoneID The desired TimeZone ID.  If 0, use the default time zone.
* @param len The length of zoneID, or -1 if null-terminated.
* @param status A pointer to an UErrorCode to receive any errors.
*/
U_CAPI void 
ucal_setTimeZone(        UCalendar*      cal,
            const    UChar*        zoneID,
                int32_t        len,
                UErrorCode     *status);

/** Possible formats for a UCalendar's display name */
enum UCalendarDisplayNameType {
  /** Standard display name */
  UCAL_STANDARD,
  /** Short standard display name */
  UCAL_SHORT_STANDARD,
  /** Daylight savings display name */
  UCAL_DST,
  /** Short daylight savings display name */
  UCAL_SHORT_DST
};
typedef enum UCalendarDisplayNameType UCalendarDisplayNameType;

/**
* Get the display name for a UCalendar's TimeZone.
* A display name is suitable for presentation to a user.
* @param cal The UCalendar to query.
* @param type The desired display name format; one of UCAL_STANDARD, UCAL_SHORT_STANDARD, 
* UCAL_DST, UCAL_SHORT_DST
* @param locale The desired locale for the display name.
* @param status A pointer to an UErrorCode to receive any errors
* @param result A pointer to a buffer to receive the formatted number.
* @param resultLength The maximum size of result.
* @param resultLengthNeeded If not 0, on output the number of characters actually
* written to result.
* @return The total buffer size needed; if greater than resultLength, the output was truncated.
*/
U_CAPI int32_t
ucal_getTimeZoneDisplayName(    const     UCalendar*                 cal,
                    UCalendarDisplayNameType     type,
                const      char                     *locale,
                    UChar*                  result,
                    int32_t                 resultLength,
                    UErrorCode*             status);

/**
* Determine if a UCalendar is currently in daylight savings time.
* Daylight savings time is not used in all parts of the world.
* @param cal The UCalendar to query.
* @param status A pointer to an UErrorCode to receive any errors
* @return TRUE if cal is currently in daylight savings time, FALSE otherwise
*/
U_CAPI bool_t 
ucal_inDaylightTime(    const    UCalendar*      cal, 
                UErrorCode*     status );

/** Types of UCalendar attributes */
enum UCalendarAttribute {
    /** Lenient parsing */
  UCAL_LENIENT,
  /** First day of week */
  UCAL_FIRST_DAY_OF_WEEK,
  /** Minimum number of days in first week */
  UCAL_MINIMAL_DAYS_IN_FIRST_WEEK    
};
typedef enum UCalendarAttribute UCalendarAttribute;

/** 
* Get a numeric attribute associated with a UCalendar.
* Numeric attributes include the first day of the week, or the minimal numbers
* of days in the first week of the month.
* @param cal The UCalendar to query.
* @param attr The desired attribute; one of UCAL_LENIENT, UCAL_FIRST_DAY_OF_WEEK, 
* or UCAL_MINIMAL_DAYS_IN_FIRST_WEEK
* @return The value of attr.
* @see ucal_setAttribute
*/
U_CAPI int32_t
ucal_getAttribute(    const    UCalendar*              cal,
                UCalendarAttribute      attr);

/** 
* Set a numeric attribute associated with a UCalendar.
* Numeric attributes include the first day of the week, or the minimal numbers
* of days in the first week of the month.
* @param cal The UCalendar to set.
* @param attr The desired attribute; one of UCAL_LENIENT, UCAL_FIRST_DAY_OF_WEEK, 
* or UCAL_MINIMAL_DAYS_IN_FIRST_WEEK
* @param newValue The new value of attr.
* @see ucal_getAttribute
*/
U_CAPI void
ucal_setAttribute(      UCalendar*              cal,
            UCalendarAttribute      attr,
            int32_t                 newValue);

/**
* Get a locale for which calendars are available.
* A UCalendar in a locale returned by this function will contain the correct
* day and month names for the locale.
* @param index The index of the desired locale.
* @return A locale for which calendars are available, or 0 if none.
* @see ucal_countAvailable
*/
U_CAPI const char*
ucal_getAvailable(int32_t index);

/**
* Determine how many locales have calendars available.
* This function is most useful as determining the loop ending condition for
* calls to \Ref{ucal_getAvailable}.
* @return The number of locales for which calendars are available.
* @see ucal_getAvailable
*/
U_CAPI int32_t
ucal_countAvailable(void);

/**
* Get a UCalendar's current time in millis.
* The time is represented as milliseconds from the epoch.
* @param cal The UCalendar to query.
* @param status A pointer to an UErrorCode to receive any errors
* @return The calendar's current time in millis.
* @see ucal_setMillis
* @see ucal_setDate
* @see ucal_setDateTime
*/
U_CAPI UDate 
ucal_getMillis(    const    UCalendar*      cal,
            UErrorCode*     status);

/**
* Set a UCalendar's current time in millis.
* The time is represented as milliseconds from the epoch.
* @param cal The UCalendar to set.
* @param dateTime The desired date and time.
* @param status A pointer to an UErrorCode to receive any errors
* @see ucal_getMillis
* @see ucal_setDate
* @see ucal_setDateTime
*/
U_CAPI void 
ucal_setMillis(        UCalendar*      cal,
            UDate           dateTime,
            UErrorCode*     status );

/**
* Set a UCalendar's current date.
* The date is represented as a series of 32-bit integers.
* @param cal The UCalendar to set.
* @param year The desired year.
* @param month The desired month; one of UCAL_JANUARY, UCAL_FEBRUARY, UCAL_MARCH, UCAL_APRIL, UCAL_MAY, 
* UCAL_JUNE, UCAL_JULY, UCAL_AUGUST, UCAL_SEPTEMBER, UCAL_OCTOBER, UCAL_NOVEMBER, UCAL_DECEMBER, UCAL_UNDECIMBER
* @param date The desired day of the month.
* @param status A pointer to an UErrorCode to receive any errors
* @see ucal_getMillis
* @see ucal_setMillis
* @see ucal_setDateTime
*/
U_CAPI void 
ucal_setDate(        UCalendar*        cal,
            int32_t            year,
            int32_t            month,
            int32_t            date,
            UErrorCode        *status);

/**
* Set a UCalendar's current date.
* The date is represented as a series of 32-bit integers.
* @param cal The UCalendar to set.
* @param year The desired year.
* @param month The desired month; one of UCAL_JANUARY, UCAL_FEBRUARY, UCAL_MARCH, UCAL_APRIL, UCAL_MAY, 
* UCAL_JUNE, UCAL_JULY, UCAL_AUGUST, UCAL_SEPTEMBER, UCAL_OCTOBER, UCAL_NOVEMBER, UCAL_DECEMBER, UCAL_UNDECIMBER
* @param date The desired day of the month.
* @param hour The desired hour of day.
* @param minute The desired minute.
* @param second The desirec second.
* @param status A pointer to an UErrorCode to receive any errors
* @see ucal_getMillis
* @see ucal_setMillis
* @see ucal_setDate
*/
U_CAPI void 
ucal_setDateTime(    UCalendar*        cal,
            int32_t            year,
            int32_t            month,
            int32_t            date,
            int32_t            hour,
            int32_t            minute,
            int32_t            second,
            UErrorCode        *status);

/**
* Determine if two UCalendars represent the same date.
* Two UCalendars may represent the same date and have different fields
* if they are in different time zones.
* @param cal1 The first of the UCalendars to compare.
* @param cal2 The second of the UCalendars to compare.
* @return TRUE if cal1 and cal2 represent the same date, FALSE otherwise.
*/
U_CAPI bool_t 
ucal_equivalentTo(    const UCalendar*      cal1,
            const UCalendar*      cal2);

/**
* Add a specified signed amount to a particular field in a UCalendar.
* This can modify more significant fields in the calendar.
* @param cal The UCalendar to which to add.
* @param field The field to which to add the signed value; one of UCAL_ERA, UCAL_YEAR, UCAL_MONTH, 
* UCAL_WEEK_OF_YEAR, UCAL_WEEK_OF_MONTH, UCAL_DATE, UCAL_DAY_OF_YEAR, UCAL_DAY_OF_WEEK, 
* UCAL_DAY_OF_WEEK_IN_MONTH, UCAL_AM_PM, UCAL_HOUR, UCAL_HOUR_OF_DAY, UCAL_MINUTE, UCAL_SECOND, 
* UCAL_MILLISECOND, UCAL_ZONE_OFFSET, UCAL_DST_OFFSET.
* @param amount The signed amount to add to field. If the amount causes the value
* to exceed to maximum or minimum values for that field, other fields are modified
* to preserve the magnitude of the change.
* @param status A pointer to an UErrorCode to receive any errors
* @see ucal_roll
*/
U_CAPI void 
ucal_add(    UCalendar*            cal,
        UCalendarDateFields        field,
        int32_t                amount,
        UErrorCode*            status);

/**
* Add a specified signed amount to a particular field in a UCalendar.
* This will not modify more significant fields in the calendar.
* @param cal The UCalendar to which to add.
* @param field The field to which to add the signed value; one of UCAL_ERA, UCAL_YEAR, UCAL_MONTH, 
* UCAL_WEEK_OF_YEAR, UCAL_WEEK_OF_MONTH, UCAL_DATE, UCAL_DAY_OF_YEAR, UCAL_DAY_OF_WEEK, 
* UCAL_DAY_OF_WEEK_IN_MONTH, UCAL_AM_PM, UCAL_HOUR, UCAL_HOUR_OF_DAY, UCAL_MINUTE, UCAL_SECOND, 
* UCAL_MILLISECOND, UCAL_ZONE_OFFSET, UCAL_DST_OFFSET.
* @param amount The signed amount to add to field. If the amount causes the value
* to exceed to maximum or minimum values for that field, the field is pinned to a permissible
* value.
* @param status A pointer to an UErrorCode to receive any errors
* @see ucal_add
*/
U_CAPI void 
ucal_roll(        UCalendar*        cal,
            UCalendarDateFields     field,
            int32_t            amount,
            UErrorCode*        status);

/**
* Get the current value of a field from a UCalendar.
* All fields are represented as 32-bit integers.
* @param cal The UCalendar to query.
* @param field The desired field; one of UCAL_ERA, UCAL_YEAR, UCAL_MONTH, 
* UCAL_WEEK_OF_YEAR, UCAL_WEEK_OF_MONTH, UCAL_DATE, UCAL_DAY_OF_YEAR, UCAL_DAY_OF_WEEK, 
* UCAL_DAY_OF_WEEK_IN_MONTH, UCAL_AM_PM, UCAL_HOUR, UCAL_HOUR_OF_DAY, UCAL_MINUTE, UCAL_SECOND, 
* UCAL_MILLISECOND, UCAL_ZONE_OFFSET, UCAL_DST_OFFSET.
* @param status A pointer to an UErrorCode to receive any errors
* @return The value of the desired field.
* @see ucal_set
* @see ucal_isSet
* @see ucal_clearField
* @see ucal_clear
*/
U_CAPI int32_t 
ucal_get(    const    UCalendar*            cal,
            UCalendarDateFields        field,
            UErrorCode*            status );

/**
* Set the value of a field in a UCalendar.
* All fields are represented as 32-bit integers.
* @param cal The UCalendar to set.
* @param field The field to set; one of UCAL_ERA, UCAL_YEAR, UCAL_MONTH, 
* UCAL_WEEK_OF_YEAR, UCAL_WEEK_OF_MONTH, UCAL_DATE, UCAL_DAY_OF_YEAR, UCAL_DAY_OF_WEEK, 
* UCAL_DAY_OF_WEEK_IN_MONTH, UCAL_AM_PM, UCAL_HOUR, UCAL_HOUR_OF_DAY, UCAL_MINUTE, UCAL_SECOND, 
* UCAL_MILLISECOND, UCAL_ZONE_OFFSET, UCAL_DST_OFFSET.
* @param value The desired value of field.
* @see ucal_get
* @see ucal_isSet
* @see ucal_clearField
* @see ucal_clear
*/
U_CAPI void 
ucal_set(    UCalendar*            cal,
        UCalendarDateFields        field,
        int32_t                value);

/**
* Determine if a field in a UCalendar is set.
* All fields are represented as 32-bit integers.
* @param cal The UCalendar to query.
* @param field The desired field; one of UCAL_ERA, UCAL_YEAR, UCAL_MONTH, 
* UCAL_WEEK_OF_YEAR, UCAL_WEEK_OF_MONTH, UCAL_DATE, UCAL_DAY_OF_YEAR, UCAL_DAY_OF_WEEK, 
* UCAL_DAY_OF_WEEK_IN_MONTH, UCAL_AM_PM, UCAL_HOUR, UCAL_HOUR_OF_DAY, UCAL_MINUTE, UCAL_SECOND, 
* UCAL_MILLISECOND, UCAL_ZONE_OFFSET, UCAL_DST_OFFSET.
* @param return TRUE if field is set, FALSE otherwise.
* @see ucal_get
* @see ucal_set
* @see ucal_clearField
* @see ucal_clear
*/
U_CAPI bool_t 
ucal_isSet(    const    UCalendar*        cal,
            UCalendarDateFields    field);

/**
* Clear a field in a UCalendar.
* All fields are represented as 32-bit integers.
* @param cal The UCalendar containing the field to clear.
* @param field The field to clear; one of UCAL_ERA, UCAL_YEAR, UCAL_MONTH, 
* UCAL_WEEK_OF_YEAR, UCAL_WEEK_OF_MONTH, UCAL_DATE, UCAL_DAY_OF_YEAR, UCAL_DAY_OF_WEEK, 
* UCAL_DAY_OF_WEEK_IN_MONTH, UCAL_AM_PM, UCAL_HOUR, UCAL_HOUR_OF_DAY, UCAL_MINUTE, UCAL_SECOND, 
* UCAL_MILLISECOND, UCAL_ZONE_OFFSET, UCAL_DST_OFFSET.
* @see ucal_get
* @see ucal_set
* @see ucal_isSet
* @see ucal_clear
*/
U_CAPI void 
ucal_clearField(    UCalendar*        cal,
            UCalendarDateFields     field);

/**
* Clear all fields in a UCalendar.
* All fields are represented as 32-bit integers.
* @param cal The UCalendar to clear.
* @see ucal_get
* @see ucal_set
* @see ucal_isSet
* @see ucal_clearField
*/
U_CAPI void 
ucal_clear(UCalendar* calendar);

/** Possible limit values for a UCalendar */
enum UCalendarLimitType {
  /** Minimum value */
  UCAL_MINIMUM,
  /** Maximum value */
  UCAL_MAXIMUM,
  /** Greatest minimum value */
  UCAL_GREATEST_MINIMUM,
  /** Leaest maximum value */
  UCAL_LEAST_MAXIMUM,
  /** Actual minimum value */
  UCAL_ACTUAL_MINIMUM,
  /** Actual maximum value */
  UCAL_ACTUAL_MAXIMUM
};
typedef enum UCalendarLimitType UCalendarLimitType;

/**
* Determine a limit for a field in a UCalendar.
* A limit is a maximum or minimum value for a field.
* @param cal The UCalendar to query.
* @param field The desired field; one of UCAL_ERA, UCAL_YEAR, UCAL_MONTH, 
* UCAL_WEEK_OF_YEAR, UCAL_WEEK_OF_MONTH, UCAL_DATE, UCAL_DAY_OF_YEAR, UCAL_DAY_OF_WEEK, 
* UCAL_DAY_OF_WEEK_IN_MONTH, UCAL_AM_PM, UCAL_HOUR, UCAL_HOUR_OF_DAY, UCAL_MINUTE, UCAL_SECOND, 
* UCAL_MILLISECOND, UCAL_ZONE_OFFSET, UCAL_DST_OFFSET.
* @param type The desired critical point; one of UCAL_MINIMUM, UCAL_MAXIMUM, UCAL_GREATEST_MINIMUM, 
* UCAL_LEAST_MAXIMUM, UCAL_ACTUAL_MINIMUM, UCAL_ACTUAL_MAXIMUM
* @param status A pointer to an UErrorCode to receive any errors.
* @return The requested value.
*/
U_CAPI int32_t 
ucal_getLimit(    const    UCalendar*              cal,
            UCalendarDateFields     field,
            UCalendarLimitType      type,
            UErrorCode        *status);

#endif
