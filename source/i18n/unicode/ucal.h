/*
* Copyright (C) 1996-2004, International Business Machines Corporation and others. All Rights Reserved.
*******************************************************************************
*/

#ifndef UCAL_H
#define UCAL_H

#include "unicode/utypes.h"
#include "unicode/uenum.h"
#include "unicode/uloc.h"

#if !UCONFIG_NO_FORMATTING

/**
 * \file
 * \brief C API: Calendar
 *
 * <h2>Calendar C API</h2>
 *
 * UCalendar C API is used  for converting between a <code>UDate</code> object
 * and a set of integer fields such as <code>UCAL_YEAR</code>, <code>UCAL_MONTH</code>,
 * <code>UCAL_DAY</code>, <code>UCAL_HOUR</code>, and so on.
 * (A <code>UDate</code> object represents a specific instant in
 * time with millisecond precision. See UDate
 * for information about the <code>UDate</code> .)
 *
 * <p>
 * Types of <code>UCalendar</code> interpret a <code>UDate</code>
 * according to the rules of a specific calendar system. The U_STABLE
 * provides the enum UCalendarType with UCAL_TRADITIONAL and
 * UCAL_GREGORIAN.
 * <p>
 * Like other locale-sensitive C API, calendar API  provides a
 * function, <code>ucal_open()</code>, which returns a pointer to
 * <code>UCalendar</code> whose time fields have been initialized
 * with the current date and time. We need to specify the type of
 * calendar to be opened and the  timezoneId.
 * \htmlonly<blockquote>\endhtmlonly
 * <pre>
 * \code
 * UCalendar *caldef;
 * UChar *tzId;
 * UErrorCode status;
 * tzId=(UChar*)malloc(sizeof(UChar) * (strlen("PST") +1) );
 * u_uastrcpy(tzId, "PST");
 * caldef=ucal_open(tzID, u_strlen(tzID), NULL, UCAL_TRADITIONAL, &status);
 * \endcode
 * </pre>
 * \htmlonly</blockquote>\endhtmlonly
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
 * \htmlonly<blockquote>\endhtmlonly
 * <pre>
 * \code
 * UCAL_MONTH + UCAL_DAY_OF_MONTH
 * UCAL_MONTH + UCAL_WEEK_OF_MONTH + UCAL_DAY_OF_WEEK
 * UCAL_MONTH + UCAL_DAY_OF_WEEK_IN_MONTH + UCAL_DAY_OF_WEEK
 * UCAL_DAY_OF_YEAR
 * UCAL_DAY_OF_WEEK + UCAL_WEEK_OF_YEAR
 * \endcode
 * </pre>
 * \htmlonly</blockquote>\endhtmlonly
 *
 * For the time of day:
 *
 * \htmlonly<blockquote>\endhtmlonly
 * <pre>
 * \code
 * UCAL_HOUR_OF_DAY
 * UCAL_AM_PM + UCAL_HOUR
 * \endcode
 * </pre>
 * \htmlonly</blockquote>\endhtmlonly
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
 * runtime. Use {@link DateFormat}
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
 * @stable ICU 2.0
 */

/** A calendar.
 *  For usage in C programs.
 * @stable ICU 2.0
 */
typedef void* UCalendar;

/** Possible types of UCalendars 
 * @stable ICU 2.0
 */
enum UCalendarType {
  /** A traditional calendar for the locale */
  UCAL_TRADITIONAL,
  /** The Gregorian calendar */
  UCAL_GREGORIAN
};

/** @stable ICU 2.0 */
typedef enum UCalendarType UCalendarType;

/** Possible fields in a UCalendar 
 * @stable ICU 2.0
 */
enum UCalendarDateFields {
  /** 
   * Era field
   * @stable ICU 2.6 
   */
  UCAL_ERA,
  /**
   * Year field
   * @stable ICU 2.6 
   */
  UCAL_YEAR,
  /**
   * Month field
   * @stable ICU 2.6 
   */
  UCAL_MONTH,
  /**
   * Week of year field
   * @stable ICU 2.6 
   */
  UCAL_WEEK_OF_YEAR,
  /**
   * Week of month field
   * @stable ICU 2.6 
   */
  UCAL_WEEK_OF_MONTH,
  /**
   * Date field
   * @stable ICU 2.6 
   */
  UCAL_DATE,
  /**
   * Day of year field
   * @stable ICU 2.6 
   */
  UCAL_DAY_OF_YEAR,
  /**
   * Day of week field
   * @stable ICU 2.6 
   */
  UCAL_DAY_OF_WEEK,
  /**
   * Day of week in month field
   * @stable ICU 2.6 
   */
  UCAL_DAY_OF_WEEK_IN_MONTH,
  /**
   * AM/PM field
   * @stable ICU 2.6 
   */
  UCAL_AM_PM,
  /**
   * Hour field
   * @stable ICU 2.6 
   */
  UCAL_HOUR,
  /**
   * Hour of day field
   * @stable ICU 2.6 
   */
  UCAL_HOUR_OF_DAY,
  /**
   * Minute field
   * @stable ICU 2.6 
   */
  UCAL_MINUTE,
  /**
   * Second field
   * @stable ICU 2.6 
   */
  UCAL_SECOND,
  /**
   * Millisecond field
   * @stable ICU 2.6 
   */
  UCAL_MILLISECOND,
  /**
   * Zone offset field
   * @stable ICU 2.6 
   */
  UCAL_ZONE_OFFSET,
  /**
   * DST offset field
   * @stable ICU 2.6 
   */
  UCAL_DST_OFFSET,
  /**
   * Year / week of year 
   * @stable ICU 2.6
   */
  UCAL_YEAR_WOY,
  /**
   * Day of week, localized (1..7) 
   * @stable ICU 2.6
   */
#ifndef U_HIDE_DRAFT_API

  UCAL_DOW_LOCAL,
  /**
   * Year of this calendar system, encompassing all supra-year fields. For example, in Gregorian/Julian calendars, positive Extended Year values indicate years AD,  1 BC = 0 extended, 2 BC = -1 extended, and so on. 
   * @draft ICU 2.8 
   */
  UCAL_EXTENDED_YEAR,       
  /**
   * Modified Julian day number, encompassing all date-related fields.  Demarcates at local midnight.
   * @draft ICU 2.8
   */
  UCAL_JULIAN_DAY, 
  /**
   * Ranges from 0 to 23:59:59.999 (regardless of DST).  This field behaves <em>exactly</em> like a composite of all time-related fields, not including the zone fields.  As such, it also reflects discontinuities of those fields on DST transition days.  On a day of DST onset, it will jump forward.  On a day of DST cessation, it will jump backward.  This reflects the fact that is must be combined with the DST_OFFSET field to obtain a unique local time value.
   * @draft ICU 2.8
   */
  UCAL_MILLISECONDS_IN_DAY,

#endif /* U_HIDE_DRAFT_API */
  
  /**
   * Field count
   * @stable ICU 2.6
   */
  UCAL_FIELD_COUNT,

#ifndef U_HIDE_DRAFT_API

  /**
   * Synonym for UCAL_DATE
   * @draft ICU 2.8
   **/
  UCAL_DAY_OF_MONTH=UCAL_DATE

#endif /*U_HIDE_DRAFT_API*/
};

/** @stable ICU 2.0 */
typedef enum UCalendarDateFields UCalendarDateFields;
    /**
     * Useful constant for days of week. Note: Calendar day-of-week is 1-based. Clients
     * who create locale resources for the field of first-day-of-week should be aware of
     * this. For instance, in US locale, first-day-of-week is set to 1, i.e., UCAL_SUNDAY.
     */
/** Possible days of the week in a UCalendar 
 * @stable ICU 2.0
 */
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

/** @stable ICU 2.0 */
typedef enum UCalendarDaysOfWeek UCalendarDaysOfWeek;

/** Possible months in a UCalendar. Note: Calendar month is 0-based.
 * @stable ICU 2.0
 */
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

/** @stable ICU 2.0 */
typedef enum UCalendarMonths UCalendarMonths;

/** Possible AM/PM values in a UCalendar 
 * @stable ICU 2.0
 */
enum UCalendarAMPMs {
    /** AM */
  UCAL_AM,
  /** PM */
  UCAL_PM
};

/** @stable ICU 2.0 */
typedef enum UCalendarAMPMs UCalendarAMPMs;

/**
 * Create an enumeration over all time zones.
 *
 * @param ec input/output error code
 *
 * @return an enumeration object that the caller must dispose of using
 * uenum_close(), or NULL upon failure. In case of failure *ec will
 * indicate the error.
 *
 * @draft ICU 2.6
 */
U_DRAFT UEnumeration* U_EXPORT2
ucal_openTimeZones(UErrorCode* ec);

/**
 * Create an enumeration over all time zones associated with the given
 * country. Some zones are affiliated with no country (e.g., "UTC");
 * these may also be retrieved, as a group.
 *
 * @param country the ISO 3166 two-letter country code, or NULL to
 * retrieve zones not affiliated with any country
 *
 * @param ec input/output error code
 *
 * @return an enumeration object that the caller must dispose of using
 * uenum_close(), or NULL upon failure. In case of failure *ec will
 * indicate the error.
 *
 * @draft ICU 2.6
 */
U_DRAFT UEnumeration* U_EXPORT2
ucal_openCountryTimeZones(const char* country, UErrorCode* ec);

/**
 * Return the default time zone. The default is determined initially
 * by querying the host operating system. It may be changed with
 * ucal_setDefaultTimeZone() or with the C++ TimeZone API.
 *
 * @param result A buffer to receive the result, or NULL
 *
 * @param resultCapacity The capacity of the result buffer
 *
 * @param ec input/output error code
 *
 * @return The result string length, not including the terminating
 * null
 *
 * @draft ICU 2.6
 */
U_DRAFT int32_t U_EXPORT2
ucal_getDefaultTimeZone(UChar* result, int32_t resultCapacity, UErrorCode* ec);

/**
 * Set the default time zone.
 *
 * @param zoneID null-terminated time zone ID
 *
 * @param ec input/output error code
 *
 * @draft ICU 2.6
 */
U_DRAFT void U_EXPORT2
ucal_setDefaultTimeZone(const UChar* zoneID, UErrorCode* ec);

/**
 * Return the amount of time in milliseconds that the clock is
 * advanced during daylight savings time for the given time zone, or
 * zero if the time zone does not observe daylight savings time.
 *
 * @param zoneID null-terminated time zone ID
 *
 * @param ec input/output error code
 *
 * @return the number of milliseconds the time is advanced with
 * respect to standard time when the daylight savings rules are in
 * effect. This is always a non-negative number, most commonly either
 * 3,600,000 (one hour) or zero.
 *
 * @draft ICU 2.6
 */
U_DRAFT int32_t U_EXPORT2
ucal_getDSTSavings(const UChar* zoneID, UErrorCode* ec);

/**
 * Get the current date and time.
 * The value returned is represented as milliseconds from the epoch.
 * @return The current date and time.
 * @stable ICU 2.0
 */
U_STABLE UDate U_EXPORT2 
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
 * @stable ICU 2.0
 */
U_STABLE UCalendar* U_EXPORT2 
ucal_open(const UChar*   zoneID,
          int32_t        len,
          const char*    locale,
          UCalendarType  type,
          UErrorCode*    status);

/**
 * Close a UCalendar.
 * Once closed, a UCalendar may no longer be used.
 * @param cal The UCalendar to close.
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_close(UCalendar *cal);

/**
 * Set the TimeZone used by a UCalendar.
 * A UCalendar uses a timezone for converting from Greenwich time to local time.
 * @param cal The UCalendar to set.
 * @param zoneID The desired TimeZone ID.  If 0, use the default time zone.
 * @param len The length of zoneID, or -1 if null-terminated.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_setTimeZone(UCalendar*    cal,
                 const UChar*  zoneID,
                 int32_t       len,
                 UErrorCode*   status);

/**
 * Possible formats for a UCalendar's display name 
 * @stable ICU 2.0
 */
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

/** @stable ICU 2.0 */
typedef enum UCalendarDisplayNameType UCalendarDisplayNameType;

/**
 * Get the display name for a UCalendar's TimeZone.
 * A display name is suitable for presentation to a user.
 * @param cal          The UCalendar to query.
 * @param type         The desired display name format; one of UCAL_STANDARD, UCAL_SHORT_STANDARD,
 *                     UCAL_DST, UCAL_SHORT_DST
 * @param locale       The desired locale for the display name.
 * @param result       A pointer to a buffer to receive the formatted number.
 * @param resultLength The maximum size of result.
 * @param status       A pointer to an UErrorCode to receive any errors
 * @return             The total buffer size needed; if greater than resultLength, the output was truncated.
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2 
ucal_getTimeZoneDisplayName(const UCalendar*          cal,
                            UCalendarDisplayNameType  type,
                            const char*               locale,
                            UChar*                    result,
                            int32_t                   resultLength,
                            UErrorCode*               status);

/**
 * Determine if a UCalendar is currently in daylight savings time.
 * Daylight savings time is not used in all parts of the world.
 * @param cal The UCalendar to query.
 * @param status A pointer to an UErrorCode to receive any errors
 * @return TRUE if cal is currently in daylight savings time, FALSE otherwise
 * @stable ICU 2.0
 */
U_STABLE UBool U_EXPORT2 
ucal_inDaylightTime(const UCalendar*  cal,
                    UErrorCode*       status );

/**
 * Types of UCalendar attributes 
 * @stable ICU 2.0
 */
enum UCalendarAttribute {
    /** Lenient parsing */
  UCAL_LENIENT,
  /** First day of week */
  UCAL_FIRST_DAY_OF_WEEK,
  /** Minimum number of days in first week */
  UCAL_MINIMAL_DAYS_IN_FIRST_WEEK
};

/** @stable ICU 2.0 */
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
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2 
ucal_getAttribute(const UCalendar*    cal,
                  UCalendarAttribute  attr);

/**
 * Set a numeric attribute associated with a UCalendar.
 * Numeric attributes include the first day of the week, or the minimal numbers
 * of days in the first week of the month.
 * @param cal The UCalendar to set.
 * @param attr The desired attribute; one of UCAL_LENIENT, UCAL_FIRST_DAY_OF_WEEK,
 * or UCAL_MINIMAL_DAYS_IN_FIRST_WEEK
 * @param newValue The new value of attr.
 * @see ucal_getAttribute
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_setAttribute(UCalendar*          cal,
                  UCalendarAttribute  attr,
                  int32_t             newValue);

/**
 * Get a locale for which calendars are available.
 * A UCalendar in a locale returned by this function will contain the correct
 * day and month names for the locale.
 * @param index The index of the desired locale.
 * @return A locale for which calendars are available, or 0 if none.
 * @see ucal_countAvailable
 * @stable ICU 2.0
 */
U_STABLE const char* U_EXPORT2 
ucal_getAvailable(int32_t index);

/**
 * Determine how many locales have calendars available.
 * This function is most useful as determining the loop ending condition for
 * calls to \ref ucal_getAvailable.
 * @return The number of locales for which calendars are available.
 * @see ucal_getAvailable
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2 
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
 * @stable ICU 2.0
 */
U_STABLE UDate U_EXPORT2 
ucal_getMillis(const UCalendar*  cal,
               UErrorCode*       status);

/**
 * Set a UCalendar's current time in millis.
 * The time is represented as milliseconds from the epoch.
 * @param cal The UCalendar to set.
 * @param dateTime The desired date and time.
 * @param status A pointer to an UErrorCode to receive any errors
 * @see ucal_getMillis
 * @see ucal_setDate
 * @see ucal_setDateTime
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_setMillis(UCalendar*   cal,
               UDate        dateTime,
               UErrorCode*  status );

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
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_setDate(UCalendar*   cal,
             int32_t      year,
             int32_t      month,
             int32_t      date,
             UErrorCode*  status);

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
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_setDateTime(UCalendar*   cal,
                 int32_t      year,
                 int32_t      month,
                 int32_t      date,
                 int32_t      hour,
                 int32_t      minute,
                 int32_t      second,
                 UErrorCode*  status);

/**
 * Returns TRUE if two UCalendars are equivalent.  Equivalent
 * UCalendars will behave identically, but they may be set to
 * different times.
 * @param cal1 The first of the UCalendars to compare.
 * @param cal2 The second of the UCalendars to compare.
 * @return TRUE if cal1 and cal2 are equivalent, FALSE otherwise.
 * @stable ICU 2.0
 */
U_STABLE UBool U_EXPORT2 
ucal_equivalentTo(const UCalendar*  cal1,
                  const UCalendar*  cal2);

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
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_add(UCalendar*           cal,
         UCalendarDateFields  field,
         int32_t              amount,
         UErrorCode*          status);

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
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_roll(UCalendar*           cal,
          UCalendarDateFields  field,
          int32_t              amount,
          UErrorCode*          status);

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
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2 
ucal_get(const UCalendar*     cal,
         UCalendarDateFields  field,
         UErrorCode*          status );

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
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_set(UCalendar*           cal,
         UCalendarDateFields  field,
         int32_t              value);

/**
 * Determine if a field in a UCalendar is set.
 * All fields are represented as 32-bit integers.
 * @param cal The UCalendar to query.
 * @param field The desired field; one of UCAL_ERA, UCAL_YEAR, UCAL_MONTH,
 * UCAL_WEEK_OF_YEAR, UCAL_WEEK_OF_MONTH, UCAL_DATE, UCAL_DAY_OF_YEAR, UCAL_DAY_OF_WEEK,
 * UCAL_DAY_OF_WEEK_IN_MONTH, UCAL_AM_PM, UCAL_HOUR, UCAL_HOUR_OF_DAY, UCAL_MINUTE, UCAL_SECOND,
 * UCAL_MILLISECOND, UCAL_ZONE_OFFSET, UCAL_DST_OFFSET.
 * @return TRUE if field is set, FALSE otherwise.
 * @see ucal_get
 * @see ucal_set
 * @see ucal_clearField
 * @see ucal_clear
 * @stable ICU 2.0
 */
U_STABLE UBool U_EXPORT2 
ucal_isSet(const UCalendar*     cal,
           UCalendarDateFields  field);

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
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_clearField(UCalendar*           cal,
                UCalendarDateFields  field);

/**
 * Clear all fields in a UCalendar.
 * All fields are represented as 32-bit integers.
 * @param calendar The UCalendar to clear.
 * @see ucal_get
 * @see ucal_set
 * @see ucal_isSet
 * @see ucal_clearField
 * @stable ICU 2.0
 */
U_STABLE void U_EXPORT2 
ucal_clear(UCalendar* calendar);

/**
 * Possible limit values for a UCalendar 
 * @stable ICU 2.0
 */
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

/** @stable ICU 2.0 */
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
 * @stable ICU 2.0
 */
U_STABLE int32_t U_EXPORT2 
ucal_getLimit(const UCalendar*     cal,
              UCalendarDateFields  field,
              UCalendarLimitType   type,
              UErrorCode*          status);

#ifdef U_USE_UCAL_OBSOLETE_2_8
/**
 * Get an available TimeZone ID.
 * A Timezone ID is a string of the form "America/Los Angeles".
 * @param rawOffset The desired GMT offset
 * @param index The index of the desired TimeZone.
 * @param status A pointer to an UErrorCode to receive any errors
 * @return The requested TimeZone ID, or 0 if not found
 * @see ucal_countAvailableTZIDs
 * @obsolete ICU 2.8. Use ucal_openTimeZoneEnumeration instead since this API will be removed in that release.
 */
U_OBSOLETE const UChar* U_EXPORT2 
ucal_getAvailableTZIDs(int32_t      rawOffset,
                       int32_t      index,
                       UErrorCode*  status);

/**
 * Determine how many TimeZones exist with a certain offset.
 * This function is most useful as determining the loop ending condition for
 * calls to \ref ucal_getAvailableTZIDs.
 * @param rawOffset The desired GMT offset.
 * @return The number of TimeZones with rawOffset.
 * @see ucal_getAvailableTZIDs
 * @obsolete ICU 2.8.  Use ucal_openTimeZoneEnumeration instead since this API will be removed in that release.
 */
U_OBSOLETE int32_t U_EXPORT2 
ucal_countAvailableTZIDs(int32_t rawOffset);
#endif

/** Get the locale for this calendar object. You can choose between valid and actual locale.
 *  @param cal The calendar object
 *  @param type type of the locale we're looking for (valid or actual) 
 *  @param status error code for the operation
 *  @return the locale name
 * @draft ICU 2.8 likely to change in ICU 3.0, based on feedback
 */
U_DRAFT const char * U_EXPORT2
ucal_getLocaleByType(const UCalendar *cal, ULocDataLocaleType type, UErrorCode* status);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
