/*
* Copyright © {1997-1999}, International Business Machines Corporation and others. All Rights Reserved.
********************************************************************************
*
* File GREGOCAL.H
*
* Modification History:
*
*   Date        Name        Description
*   04/22/97    aliu        Overhauled header.
*    07/28/98    stephen        Sync with JDK 1.2
*    09/04/98    stephen        Re-sync with JDK 8/31 putback
*    09/14/98    stephen        Changed type of kOneDay, kOneWeek to double.
*                            Fixed bug in roll() 
*   10/15/99    aliu        Fixed j31, incorrect WEEK_OF_YEAR computation.
*                           Added documentation of WEEK_OF_YEAR computation.
*   10/15/99    aliu        Fixed j32, cannot set date to Feb 29 2000 AD.
*                           {JDK bug 4210209 4209272}
********************************************************************************
*/
         
#ifndef GREGOCAL_H
#define GREGOCAL_H


#include "unicode/calendar.h"

/**
 * Concrete class which provides the standard calendar used by most of the world.
 * <P>
 * The standard (Gregorian) calendar has 2 eras, BC and AD.
 * <P>
 * This implementation handles a single discontinuity, which corresponds by default to
 * the date the Gregorian calendar was originally instituted (October 15, 1582). Not all
 * countries adopted the Gregorian calendar then, so this cutover date may be changed by
 * the caller.
 * <P>
 * Prior to the institution of the Gregorian Calendar, New Year's Day was March 25. To
 * avoid confusion, this Calendar always uses January 1. A manual adjustment may be made
 * if desired for dates that are prior to the Gregorian changeover and which fall
 * between January 1 and March 24.
 *
 * <p>Values calculated for the <code>WEEK_OF_YEAR</code> field range from 1 to
 * 53.  Week 1 for a year is the first week that contains at least
 * <code>getMinimalDaysInFirstWeek()</code> days from that year.  It thus
 * depends on the values of <code>getMinimalDaysInFirstWeek()</code>,
 * <code>getFirstDayOfWeek()</code>, and the day of the week of January 1.
 * Weeks between week 1 of one year and week 1 of the following year are
 * numbered sequentially from 2 to 52 or 53 (as needed).
 *
 * <p>For example, January 1, 1998 was a Thursday.  If
 * <code>getFirstDayOfWeek()</code> is <code>MONDAY</code> and
 * <code>getMinimalDaysInFirstWeek()</code> is 4 (these are the values
 * reflecting ISO 8601 and many national standards), then week 1 of 1998 starts
 * on December 29, 1997, and ends on January 4, 1998.  If, however,
 * <code>getFirstDayOfWeek()</code> is <code>SUNDAY</code>, then week 1 of 1998
 * starts on January 4, 1998, and ends on January 10, 1998; the first three days
 * of 1998 then are part of week 53 of 1997.
 *
 * <p>Example for using GregorianCalendar:
 * <pre>
 * .    // get the supported ids for GMT-08:00 (Pacific Standard Time)
 * .    int32_t idsCount;
 * .    const UnicodeString** ids = TimeZone::createAvailableIDs(-8 * 60 * 60 * 1000, idsCount);
 * .    // if no ids were returned, something is wrong. get out.
 * .    if (idsCount == 0) {
 * .        return;
 * .    }
 * .
 * .    // begin output
 * .    cout << "Current Time" << endl;
 * .
 * .    // create a Pacific Standard Time time zone
 * .    SimpleTimeZone* pdt = new SimpleTimeZone(-8 * 60 * 60 * 1000, *(ids[0]));
 * .    
 * .    // set up rules for daylight savings time
 * .    pdt->setStartRule(Calendar::APRIL, 1, Calendar::SUNDAY, 2 * 60 * 60 * 1000);
 * .    pdt->setEndRule(Calendar::OCTOBER, -1, Calendar::SUNDAY, 2 * 60 * 60 * 1000);
 * .    
 * .    // create a GregorianCalendar with the Pacific Daylight time zone
 * .    // and the current date and time
 * .    UErrorCode success = U_ZERO_ERROR;
 * .    Calendar* calendar = new GregorianCalendar( pdt, success );
 * .    
 * .    // print out a bunch of interesting things
 * .    cout << "ERA: " << calendar->get( Calendar::ERA, success ) << endl;
 * .    cout << "YEAR: " << calendar->get( Calendar::YEAR, success ) << endl;
 * .    cout << "MONTH: " << calendar->get( Calendar::MONTH, success ) << endl;
 * .    cout << "WEEK_OF_YEAR: " << calendar->get( Calendar::WEEK_OF_YEAR, success ) << endl;
 * .    cout << "WEEK_OF_MONTH: " << calendar->get( Calendar::WEEK_OF_MONTH, success ) << endl;
 * .    cout << "DATE: " << calendar->get( Calendar::DATE, success ) << endl;
 * .    cout << "DAY_OF_MONTH: " << calendar->get( Calendar::DAY_OF_MONTH, success ) << endl;
 * .    cout << "DAY_OF_YEAR: " << calendar->get( Calendar::DAY_OF_YEAR, success ) << endl;
 * .    cout << "DAY_OF_WEEK: " << calendar->get( Calendar::DAY_OF_WEEK, success ) << endl;
 * .    cout << "DAY_OF_WEEK_IN_MONTH: " << calendar->get( Calendar::DAY_OF_WEEK_IN_MONTH, success ) << endl;
 * .    cout << "AM_PM: " << calendar->get( Calendar::AM_PM, success ) << endl;
 * .    cout << "HOUR: " << calendar->get( Calendar::HOUR, success ) << endl;
 * .    cout << "HOUR_OF_DAY: " << calendar->get( Calendar::HOUR_OF_DAY, success ) << endl;
 * .    cout << "MINUTE: " << calendar->get( Calendar::MINUTE, success ) << endl;
 * .    cout << "SECOND: " << calendar->get( Calendar::SECOND, success ) << endl;
 * .    cout << "MILLISECOND: " << calendar->get( Calendar::MILLISECOND, success ) << endl;
 * .    cout << "ZONE_OFFSET: " << (calendar->get( Calendar::ZONE_OFFSET, success )/(60*60*1000)) << endl;
 * .    cout << "DST_OFFSET: " << (calendar->get( Calendar::DST_OFFSET, success )/(60*60*1000)) << endl;
 * .
 * .    cout << "Current Time, with hour reset to 3" << endl;
 * .    calendar->clear(Calendar::HOUR_OF_DAY); // so doesn't override
 * .    calendar->set(Calendar::HOUR, 3);
 * .    cout << "ERA: " << calendar->get( Calendar::ERA, success ) << endl;
 * .    cout << "YEAR: " << calendar->get( Calendar::YEAR, success ) << endl;
 * .    cout << "MONTH: " << calendar->get( Calendar::MONTH, success ) << endl;
 * .    cout << "WEEK_OF_YEAR: " << calendar->get( Calendar::WEEK_OF_YEAR, success ) << endl;
 * .    cout << "WEEK_OF_MONTH: " << calendar->get( Calendar::WEEK_OF_MONTH, success ) << endl;
 * .    cout << "DATE: " << calendar->get( Calendar::DATE, success ) << endl;
 * .    cout << "DAY_OF_MONTH: " << calendar->get( Calendar::DAY_OF_MONTH, success ) << endl;
 * .    cout << "DAY_OF_YEAR: " << calendar->get( Calendar::DAY_OF_YEAR, success ) << endl;
 * .    cout << "DAY_OF_WEEK: " << calendar->get( Calendar::DAY_OF_WEEK, success ) << endl;
 * .    cout << "DAY_OF_WEEK_IN_MONTH: " << calendar->get( Calendar::DAY_OF_WEEK_IN_MONTH, success ) << endl;
 * .    cout << "AM_PM: " << calendar->get( Calendar::AM_PM, success ) << endl;
 * .    cout << "HOUR: " << calendar->get( Calendar::HOUR, success ) << endl;
 * .    cout << "HOUR_OF_DAY: " << calendar->get( Calendar::HOUR_OF_DAY, success ) << endl;
 * .    cout << "MINUTE: " << calendar->get( Calendar::MINUTE, success ) << endl;
 * .    cout << "SECOND: " << calendar->get( Calendar::SECOND, success ) << endl;
 * .    cout << "MILLISECOND: " << calendar->get( Calendar::MILLISECOND, success ) << endl;
 * .    cout << "ZONE_OFFSET: " << (calendar->get( Calendar::ZONE_OFFSET, success )/(60*60*1000)) << endl; // in hours
 * .    cout << "DST_OFFSET: " << (calendar->get( Calendar::DST_OFFSET, success )/(60*60*1000)) << endl; // in hours
 * .
 * .    delete[] ids;
 * .    delete calendar; // also deletes pdt
 * .
 * </pre>
 */
class U_I18N_API GregorianCalendar: public Calendar {
public:

    /**
     * Useful constants for GregorianCalendar and TimeZone.
     */
    enum EEras {
        BC,
        AD
    };

    /**
     * Constructs a default GregorianCalendar using the current time in the default time
     * zone with the default locale.
     *
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(UErrorCode& success);

    /**
     * Constructs a GregorianCalendar based on the current time in the given time zone
     * with the default locale. Clients are no longer responsible for deleting the given
     * time zone object after it's adopted.
     *
     * @param zoneToAdopt     The given timezone.
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(TimeZone* zoneToAdopt, UErrorCode& success);

    /**
     * Constructs a GregorianCalendar based on the current time in the given time zone
     * with the default locale.
     *
     * @param zone     The given timezone.
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(const TimeZone& zone, UErrorCode& success);

    /**
     * Constructs a GregorianCalendar based on the current time in the default time zone
     * with the given locale.
     *
     * @param aLocale  The given locale.
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(const Locale& aLocale, UErrorCode& success);

    /**
     * Constructs a GregorianCalendar based on the current time in the given time zone
     * with the given locale. Clients are no longer responsible for deleting the given
     * time zone object after it's adopted.
     *
     * @param zoneToAdopt     The given timezone.
     * @param aLocale  The given locale.
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(TimeZone* zoneToAdopt, const Locale& aLocale, UErrorCode& success);

    /**
     * Constructs a GregorianCalendar based on the current time in the given time zone
     * with the given locale.
     *
     * @param zone     The given timezone.
     * @param aLocale  The given locale.
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(const TimeZone& zone, const Locale& aLocale, UErrorCode& success);

    /**
     * Constructs a GregorianCalendar with the given AD date set in the default time
     * zone with the default locale.
     *
     * @param year     The value used to set the YEAR time field in the calendar.
     * @param month    The value used to set the MONTH time field in the calendar. Month
     *                 value is 0-based. e.g., 0 for January.
     * @param date     The value used to set the DATE time field in the calendar.
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(int32_t year, int32_t month, int32_t date, UErrorCode& success);

    /**
     * Constructs a GregorianCalendar with the given AD date and time set for the
     * default time zone with the default locale.
     *
     * @param year     The value used to set the YEAR time field in the calendar.
     * @param month    The value used to set the MONTH time field in the calendar. Month
     *                 value is 0-based. e.g., 0 for January.
     * @param date     The value used to set the DATE time field in the calendar.
     * @param hour     The value used to set the HOUR_OF_DAY time field in the calendar.
     * @param minute   The value used to set the MINUTE time field in the calendar.
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(int32_t year, int32_t month, int32_t date, int32_t hour, int32_t minute, UErrorCode& success);

    /**
     * Constructs a GregorianCalendar with the given AD date and time set for the
     * default time zone with the default locale.
     *
     * @param year     The value used to set the YEAR time field in the calendar.
     * @param month    The value used to set the MONTH time field in the calendar. Month
     *                 value is 0-based. e.g., 0 for January.
     * @param date     The value used to set the DATE time field in the calendar.
     * @param hour     The value used to set the HOUR_OF_DAY time field in the calendar.
     * @param minute   The value used to set the MINUTE time field in the calendar.
     * @param second   The value used to set the SECOND time field in the calendar.
     * @param success  Indicates the status of GregorianCalendar object construction.
     *                 Returns U_ZERO_ERROR if constructed successfully.
     */
    GregorianCalendar(int32_t year, int32_t month, int32_t date, int32_t hour, int32_t minute, int32_t second, UErrorCode& success);

    /**
     * Destructor
     */
    virtual ~GregorianCalendar();

    /**
     * Copy constructor
     */
    GregorianCalendar(const GregorianCalendar& source);

    /**
     * Default assignment operator
     */
    GregorianCalendar& operator=(const GregorianCalendar& right);

    /**
     * Create and return a polymorphic copy of this calendar.
     */
    virtual Calendar* clone(void) const;

    /**
     * Sets the GregorianCalendar change date. This is the point when the switch from
     * Julian dates to Gregorian dates occurred. Default is 00:00:00 local time, October
     * 15, 1582. Previous to this time and date will be Julian dates.
     *
     * @param date     The given Gregorian cutover date.
     * @param success  Output param set to success/failure code on exit.
     */
    void setGregorianChange(UDate date, UErrorCode& success);

    /**
     * Gets the Gregorian Calendar change date. This is the point when the switch from
     * Julian dates to Gregorian dates occurred. Default is 00:00:00 local time, October
     * 15, 1582. Previous to this time and date will be Julian dates.
     *
     * @return   The Gregorian cutover time for this calendar.
     */
    UDate getGregorianChange(void) const;

    /**
     * Return true if the given year is a leap year. Determination of whether a year is
     * a leap year is actually very complicated. We do something crude and mostly
     * correct here, but for a real determination you need a lot of contextual
     * information. For example, in Sweden, the change from Julian to Gregorian happened
     * in a complex way resulting in missed leap years and double leap years between
     * 1700 and 1753. Another example is that after the start of the Julian calendar in
     * 45 B.C., the leap years did not regularize until 8 A.D. This method ignores these
     * quirks, and pays attention only to the Julian onset date and the Gregorian
     * cutover (which can be changed).
     *
     * @param year  The given year.
     * @return      True if the given year is a leap year; false otherwise.
     */
    bool_t isLeapYear(int32_t year) const;

    /**
     * Compares the equality of two GregorianCalendar objects. Objects of different
     * subclasses are considered unequal.  This is a strict equality test; see the
     * documentation for Calendar::operator==().
     *
     * @param that  The GregorianCalendar object to be compared with.
     * @return      True if the given GregorianCalendar is the same as this
     *              GregorianCalendar; false otherwise.
     */
    virtual bool_t operator==(const Calendar& that) const;

    /**
     * Calendar override.
     * Return true if another Calendar object is equivalent to this one.  An equivalent
     * Calendar will behave exactly as this one does, but may be set to a different time.
     */
    virtual bool_t equivalentTo(const Calendar& other) const;

    /**
     * (Overrides Calendar) UDate Arithmetic function. Adds the specified (signed) amount
     * of time to the given time field, based on the calendar's rules.  For more
     * information, see the documentation for Calendar::add().
     *
     * @param field   The time field.
     * @param amount  The amount of date or time to be added to the field.
     * @param status  Output param set to success/failure code on exit. If any value
     *                previously set in the time field is invalid, this will be set to
     *                an error status.
     */
    virtual void add(EDateFields field, int32_t amount, UErrorCode& status);

    /**
     * (Overrides Calendar) Rolls up or down by the given amount in the specified field.
     * For more information, see the documentation for Calendar::roll().
     *
     * @param field   The time field.
     * @param amount  Indicates amount to roll.
     * @param status  Output param set to success/failure code on exit. If any value
     *                previously set in the time field is invalid, this will be set to
     *                an error status.
     */
    virtual void roll(EDateFields field, int32_t amount, UErrorCode& status);

    /**
     * (Overrides Calendar) Returns minimum value for the given field. e.g. for
     * Gregorian DAY_OF_MONTH, 1.
     */
    virtual int32_t getMinimum(EDateFields field) const;

    /**
     * (Overrides Calendar) Returns maximum value for the given field. e.g. for
     * Gregorian DAY_OF_MONTH, 31.
     */
    virtual int32_t getMaximum(EDateFields field) const;

    /**
     * (Overrides Calendar) Returns highest minimum value for the given field if varies.
     * Otherwise same as getMinimum(). For Gregorian, no difference.
     */
    virtual int32_t getGreatestMinimum(EDateFields field) const;

    /**
     * (Overrides Calendar) Returns lowest maximum value for the given field if varies.
     * Otherwise same as getMaximum(). For Gregorian DAY_OF_MONTH, 28.
     */
    virtual int32_t getLeastMaximum(EDateFields field) const;

    /**
     * Return the minimum value that this field could have, given the current date.
     * For the Gregorian calendar, this is the same as getMinimum() and getGreatestMinimum().
     */
    int32_t getActualMinimum(EDateFields field) const;

    /**
     * Return the maximum value that this field could have, given the current date.
     * For example, with the date "Feb 3, 1997" and the DAY_OF_MONTH field, the actual
     * maximum would be 28; for "Feb 3, 1996" it s 29.  Similarly for a Hebrew calendar,
     * for some years the actual maximum for MONTH is 12, and for others 13.
     */
    int32_t getActualMaximum(EDateFields field) const;

    /**
     * (Overrides Calendar) Return true if the current date for this Calendar is in
     * Daylight Savings Time. Recognizes DST_OFFSET, if it is set.
     *
     * @param status Fill-in parameter which receives the status of this operation.
     * @return   True if the current date for this Calendar is in Daylight Savings Time,
     *           false, otherwise.
     */
    virtual bool_t inDaylightTime(UErrorCode& status) const;

public:

    /**
     * Override Calendar Returns a unique class ID POLYMORPHICALLY. Pure virtual
     * override. This method is to implement a simple version of RTTI, since not all C++
     * compilers support genuine RTTI. Polymorphic operator==() and clone() methods call
     * this method.
     *
     * @return   The class ID for this object. All objects of a given class have the
     *           same class ID. Objects of other classes have different class IDs.
     */
    virtual UClassID getDynamicClassID(void) const { return (UClassID)&fgClassID; }

    /**
     * Return the class ID for this class. This is useful only for comparing to a return
     * value from getDynamicClassID(). For example:
     *
     *      Base* polymorphic_pointer = createPolymorphicObject();
     *      if (polymorphic_pointer->getDynamicClassID() ==
     *          Derived::getStaticClassID()) ...
     *
     * @return   The class ID for all objects of this class.
     */
    static UClassID getStaticClassID(void) { return (UClassID)&fgClassID; }

protected:

    /**
     * (Overrides Calendar) Converts GMT as milliseconds to time field values.
     */
    virtual void computeFields(UErrorCode& status);

    /**
     * (Overrides Calendar) Converts Calendar's time field values to GMT as
     * milliseconds.
     *
     * @param status  Output param set to success/failure code on exit. If any value
     *                previously set in the time field is invalid, this will be set to
     *                an error status.
     */
    virtual void computeTime(UErrorCode& status);

private: 

    /**
     * Return the year that corresponds to the <code>WEEK_OF_YEAR</code> field.
     * This may be one year before or after the calendar year stored
     * in the <code>YEAR</code> field.  For example, January 1, 1999 is considered
     * Friday of week 53 of 1998 (if minimal days in first week is
     * 2 or less, and the first day of the week is Sunday).  Given
     * these same settings, the ISO year of January 1, 1999 is
     * 1998.
     * <p>
     * Warning: This method will complete all fields.
     * @return the year corresponding to the <code>WEEK_OF_YEAR</code> field, which
     * may be one year before or after the <code>YEAR</code> field.
     * @see #WEEK_OF_YEAR
     */
    int32_t getISOYear(UErrorCode& status);

    /**
     * Return the ERA.  We need a special method for this because the
     * default ERA is AD, but a zero (unset) ERA is BC.
     */
    int32_t internalGetEra() const;

    // this is 2^52 - 1, the largest allowable mantissa with a 0 exponent in a 64-bit double
    static const UDate EARLIEST_SUPPORTED_MILLIS;
    static const UDate LATEST_SUPPORTED_MILLIS;

    int32_t monthLength(int32_t month) const;
    int32_t monthLength(int32_t month, int32_t year) const;

    int32_t yearLength(int32_t year) const;

    int32_t yearLength(void) const;

    /**
     * After adjustments such as add(MONTH), add(YEAR), we don't want the
     * month to jump around.  E.g., we don't want Jan 31 + 1 month to go to Mar
     * 3, we want it to go to Feb 28.  Adjustments which might run into this
     * problem call this method to retain the proper month.
     */
    void pinDayOfMonth(void);

    /**
     * Return the day number with respect to the epoch.  January 1, 1970 (Gregorian)
     * is day zero.
     */
    UDate getEpochDay(UErrorCode& status);

    /**
     * Compute the Julian day number under either the Gregorian or the
     * Julian calendar, using the given year and the remaining fields.
     * @param isGregorian if true, use the Gregorian calendar
     * @param year the adjusted year number, with 0 indicating the
     * year 1 BC, -1 indicating 2 BC, etc.
     * @return the Julian day number
     */
    double computeJulianDay(bool_t isGregorian, int32_t year);

    /**
     * Compute the date-based fields given the milliseconds since the epoch start. Do
     * not compute the time-based fields (HOUR, MINUTE, etc.).
     *
     * @param theTime the time in wall millis (either Standard or DST),
     * whichever is in effect
     * @param quick if true, only compute the ERA, YEAR, MONTH, DATE,
     * DAY_OF_WEEK, and DAY_OF_YEAR.
     */
    void timeToFields(UDate theTime, bool_t quick, UErrorCode& status);


    /**
     * Return the week number of a day, within a period. This may be the week number in
     * a year, or the week number in a month. Usually this will be a value >= 1, but if
     * some initial days of the period are excluded from week 1, because
     * minimalDaysInFirstWeek is > 1, then the week number will be zero for those
     * initial days. Requires the day of week for the given date in order to determine
     * the day of week of the first day of the period.
     *
     * @param date  Day-of-year or day-of-month. Should be 1 for first day of period.
     * @param day   Day-of-week for given dayOfPeriod. 1-based with 1=Sunday.
     * @return      Week number, one-based, or zero if the day falls in part of the
     *              month before the first week, when there are days before the first
     *              week because the minimum days in the first week is more than one.
     */
    int32_t weekNumber(int32_t date, int32_t day);

    /**
     * Validates the values of the set time fields.  True if they're all valid.
     */
    bool_t validateFields(void) const;

    /**
     * Validates the value of the given time field.  True if it's valid.
     */
    bool_t boundsCheck(int32_t value, EDateFields field) const;

    /**
     * Return the pseudo-time-stamp for two fields, given their
     * individual pseudo-time-stamps.  If either of the fields
     * is unset, then the aggregate is unset.  Otherwise, the
     * aggregate is the later of the two stamps.
     */
    EStampValues aggregateStamp(EStampValues stamp_a, EStampValues stamp_b);

    /**
     * The point at which the Gregorian calendar rules are used, measured in
     * milliseconds from the standard epoch.  Default is October 15, 1582
     * (Gregorian) 00:00:00 UTC, that is, October 4, 1582 (Julian) is followed
     * by October 15, 1582 (Gregorian).  This corresponds to Julian day number
     * 2299161.
     */
    // This is measured from the standard epoch, not in Julian Days.
    UDate                fGregorianCutover;

    /**
     * Midnight, local time (using this Calendar's TimeZone) at or before the
     * gregorianCutover. This is a pure date value with no time of day or
     * timezone component.
     */
    UDate                 fNormalizedGregorianCutover;// = gregorianCutover;

    /**
     * The year of the gregorianCutover, with 0 representing
     * 1 BC, -1 representing 2 BC, etc.
     */
    int32_t fGregorianCutoverYear;// = 1582;

    static char fgClassID;

    /**
     * Converts time as milliseconds to Julian date. The Julian date used here is not a
     * true Julian date, since it is measured from midnight, not noon.
     *
     * @param millis  The given milliseconds.
     * @return        The Julian date number.
     */
    static double millisToJulianDay(UDate millis);

    /**
     * Converts Julian date to time as milliseconds. The Julian date used here is not a
     * true Julian date, since it is measured from midnight, not noon.
     *
     * @param julian  The given Julian date number.
     * @return        Time as milliseconds.
     */
    static UDate julianDayToMillis(double julian);

    /**
     * Convert a quasi Julian date to the day of the week. The Julian date used here is
     * not a true Julian date, since it is measured from midnight, not noon. Return
     * value is one-based.
     *
     * @return   Day number from 1..7 (SUN..SAT).
     */
    static uint8_t julianDayToDayOfWeek(double julian);

    /**
     * Divide two long integers, returning the floor of the quotient.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0
     * but <code>floorDivide(-1,4)</code> => -1.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @return the floor of the quotient.
     */
    static double floorDivide(double numerator, double denominator);

    /**
     * Divide two integers, returning the floor of the quotient.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0
     * but <code>floorDivide(-1,4)</code> => -1.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @return the floor of the quotient.
     */
    static int32_t floorDivide(int32_t numerator, int32_t denominator);

    /**
     * Divide two integers, returning the floor of the quotient, and
     * the modulus remainder.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0 and <code>-1%4</code> => -1,
     * but <code>floorDivide(-1,4)</code> => -1 with <code>remainder[0]</code> => 3.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @param remainder an array of at least one element in which the value
     * <code>numerator mod denominator</code> is returned. Unlike <code>numerator
     * % denominator</code>, this will always be non-negative.
     * @return the floor of the quotient.
     */
    static int32_t floorDivide(int32_t numerator, int32_t denominator, int32_t remainder[]);

    /**
     * Divide two integers, returning the floor of the quotient, and
     * the modulus remainder.
     * <p>
     * Unlike the built-in division, this is mathematically well-behaved.
     * E.g., <code>-1/4</code> => 0 and <code>-1%4</code> => -1,
     * but <code>floorDivide(-1,4)</code> => -1 with <code>remainder[0]</code> => 3.
     * @param numerator the numerator
     * @param denominator a divisor which must be > 0
     * @param remainder an array of at least one element in which the value
     * <code>numerator mod denominator</code> is returned. Unlike <code>numerator
     * % denominator</code>, this will always be non-negative.
     * @return the floor of the quotient.
     */
    static int32_t floorDivide(double numerator, int32_t denominator, int32_t remainder[]);


    static const UDate       kPapalCutover;             // Cutover decreed by Pope Gregory

    static const int32_t     kJan1_1JulianDay;        // January 1, year 1 (Gregorian)
    static const int32_t     kEpochStartAsJulianDay; // January 1, 1970 (Gregorian)
    static const int32_t     kEpochYear;

    static const int32_t     kNumDays [];
    static const int32_t     kLeapNumDays [];
    static const int32_t     kMonthLength [];
    static const int32_t     kLeapMonthLength [];

    static const int32_t     kMinValues [];
    static const int32_t     kLeastMaxValues [];
    static const int32_t     kMaxValues [];

    // Useful millisecond constants
    static const int32_t    kOneSecond;
    static const int32_t    kOneMinute;
    static const int32_t    kOneHour;
    static const double        kOneDay;
    static const double        kOneWeek;
};


inline uint8_t GregorianCalendar::julianDayToDayOfWeek(double julian)
{
  // If julian is negative, then julian%7 will be negative, so we adjust
  // accordingly.  We add 1 because Julian day 0 is Monday.
  int8_t dayOfWeek = (int8_t) uprv_fmod(julian + 1, 7);
  
  uint8_t result = dayOfWeek + ((dayOfWeek < 0) ? (7 + SUNDAY) : SUNDAY);
  return result;
}

#endif // _GREGOCAL
//eof
        
