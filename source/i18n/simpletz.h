/*
********************************************************************************
*                                                                              *
* COPYRIGHT:                                                                   *
*   (C) Copyright Taligent, Inc.,  1997                                        *
*   (C) Copyright International Business Machines Corporation,  1997-1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
*   US Government Users Restricted Rights - Use, duplication, or disclosure    *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
*                                                                              *
********************************************************************************
*
* File SIMPLETZ.H
*
* Modification History:
*
*   Date        Name        Description
*   04/21/97    aliu        Overhauled header.
*    08/10/98    stephen        JDK 1.2 sync
*                            Added setStartRule() / setEndRule() overloads
*                            Added hasSameRules()
*    09/02/98    stephen        Added getOffset(monthLen)
*                            Changed getOffset() to take UErrorCode
*    07/09/99    stephen     Removed millisPerHour (unused, for HP compiler)
********************************************************************************
*/

#ifndef SIMPLETZ_H
#define SIMPLETZ_H

#include "timezone.h"

/**
 * <code>SimpleTimeZone</code> is a concrete subclass of <code>TimeZone</code>
 * that represents a time zone for use with a Gregorian calendar. This
 * class does not handle historical changes.
 * <P>
 * When specifying daylight-savings-time begin and end dates, use a negative value for
 * <code>dayOfWeekInMonth</code> to indicate that <code>SimpleTimeZone</code> should
 * count from the end of the month backwards. For example, in the U.S., Daylight Savings
 * Time ends at the last (dayOfWeekInMonth = -1) Sunday in October, at 2 AM in standard
 * time.
 *
 * @see      Calendar
 * @see      GregorianCalendar
 * @see      TimeZone
 * @version  1.24 10/30/97
 * @author   David Goldsmith, Mark Davis, Chen-Lieh Huang, Alan Liu
 */
class U_I18N_API SimpleTimeZone: public TimeZone {
public:

    /**
     * Copy constructor
     */
    SimpleTimeZone(const SimpleTimeZone& source);

    /**
     * Default assignment operator
     */
    SimpleTimeZone& operator=(const SimpleTimeZone& right);

    /**
     * Destructor
     */
    virtual ~SimpleTimeZone();

    /**
     * Returns true if the two TimeZone objects are equal; that is, they have
     * the same ID, raw GMT offset, and DST rules.
     *
     * @param that  The SimpleTimeZone object to be compared with.
     * @return      True if the given time zone is equal to this time zone; false
     *              otherwise.
     */
    virtual bool_t operator==(const TimeZone& that) const;

    /**
     * Constructs a SimpleTimeZone with the given raw GMT offset and time zone ID,
     * and which doesn't observe daylight savings time.  Normally you should use
     * TimeZone::createInstance() to create a TimeZone instead of creating a
     * SimpleTimeZone directly with this constructor.
     *
     * @param rawOffset  The given base time zone offset to GMT.
     * @param ID         The timezone ID which is obtained from
     *                   TimeZone.getAvailableIDs.
     */
    SimpleTimeZone(int32_t rawOffset, const UnicodeString& ID);

    /**
     * Construct a SimpleTimeZone with the given raw GMT offset, time zone ID,
     * and times to start and end daylight savings time. To create a TimeZone that
     * doesn't observe daylight savings time, don't use this constructor; use 
     * SimpleTimeZone(rawOffset, ID) instead. Normally, you should use
     * TimeZone.createInstance() to create a TimeZone instead of creating a
     * SimpleTimeZone directly with this constructor.
     * <P>
     * Various types of daylight-savings time rules can be specfied by using different
     * values for startDay and startDayOfWeek and endDay and endDayOfWeek.  For a
     * complete explanation of how these parameters work, see the documentation for
     * setStartRule().
     *
     * @param rawOffset       The new SimpleTimeZone's raw GMT offset
     * @param ID              The new SimpleTimeZone's time zone ID.
     * @param startMonth      The daylight savings starting month. Month is
     *                        0-based. eg, 0 for January.
     * @param startDay        The daylight savings starting
     *                        day-of-week-in-month. See setStartRule() for a
     *                        complete explanation.
     * @param startDayOfWeek  The daylight savings starting day-of-week. See setStartRule()
     *                        for a complete explanation.
     * @param startTime       The daylight savings starting time, expressed as the
     *                        number of milliseconds after midnight.
     * @param endMonth        The daylight savings ending month. Month is
     *                        0-based. eg, 0 for January.
     * @param endDay          The daylight savings ending day-of-week-in-month.
     *                        See setStartRule() for a complete explanation.
     * @param endDayOfWeek    The daylight savings ending day-of-week. See setStartRule()
     *                        for a complete explanation.
     * @param endTime         The daylight savings ending time, expressed as the
     *                        number of milliseconds after midnight.
     * @param status          An UErrorCode to receive the status.
     */
    SimpleTimeZone(int32_t rawOffset, const UnicodeString& ID,
        int8_t startMonth, int8_t startDayOfWeekInMonth,
        int8_t startDayOfWeek, int32_t startTime,
        int8_t endMonth, int8_t endDayOfWeekInMonth,
        int8_t endDayOfWeek, int32_t endTime,
        UErrorCode& status);

    SimpleTimeZone(int32_t rawOffset, const UnicodeString& ID,
        int8_t startMonth, int8_t startDayOfWeekInMonth,
        int8_t startDayOfWeek, int32_t startTime,
        int8_t endMonth, int8_t endDayOfWeekInMonth,
        int8_t endDayOfWeek, int32_t endTime,
        int32_t dstSavings, UErrorCode& status);

    /**
     * Sets the daylight savings starting year, that is, the year this time zone began
     * observing its specified daylight savings time rules.  The time zone is considered
     * not to observe daylight savings time prior to that year; SimpleTimeZone doesn't
     * support historical daylight-savings-time rules.
     * @param year the daylight savings starting year.
     */
    void setStartYear(int32_t year);

    /**
     * Sets the daylight savings starting rule. For example, in the U.S., Daylight Savings
     * Time starts at the first Sunday in April, at 2 AM in standard time.
     * Therefore, you can set the start rule by calling:
     * setStartRule(TimeFields.APRIL, 1, TimeFields.SUNDAY, 2*60*60*1000);
     * The dayOfWeekInMonth and dayOfWeek parameters together specify how to calculate
     * the exact starting date.  Their exact meaning depend on their respective signs,
     * allowing various types of rules to be constructed, as follows:<ul>
     *   <li>If both dayOfWeekInMonth and dayOfWeek are positive, they specify the
     *       day of week in the month (e.g., (2, WEDNESDAY) is the second Wednesday
     *       of the month).
     *   <li>If dayOfWeek is positive and dayOfWeekInMonth is negative, they specify
     *       the day of week in the month counting backward from the end of the month.
     *       (e.g., (-1, MONDAY) is the last Monday in the month)
     *   <li>If dayOfWeek is zero and dayOfWeekInMonth is positive, dayOfWeekInMonth
     *       specifies the day of the month, regardless of what day of the week it is.
     *       (e.g., (10, 0) is the tenth day of the month)
     *   <li>If dayOfWeek is zero and dayOfWeekInMonth is negative, dayOfWeekInMonth
     *       specifies the day of the month counting backward from the end of the
     *       month, regardless of what day of the week it is (e.g., (-2, 0) is the
     *       next-to-last day of the month).
     *   <li>If dayOfWeek is negative and dayOfWeekInMonth is positive, they specify the
     *       first specified day of the week on or after the specfied day of the month.
     *       (e.g., (15, -SUNDAY) is the first Sunday after the 15th of the month
     *       [or the 15th itself if the 15th is a Sunday].)
     *   <li>If dayOfWeek and DayOfWeekInMonth are both negative, they specify the
     *       last specified day of the week on or before the specified day of the month.
     *       (e.g., (-20, -TUESDAY) is the last Tuesday before the 20th of the month
     *       [or the 20th itself if the 20th is a Tuesday].)</ul>
     * @param month the daylight savings starting month. Month is 0-based.
     * eg, 0 for January.
     * @param dayOfWeekInMonth the daylight savings starting
     * day-of-week-in-month. Please see the member description for an example.
     * @param dayOfWeek the daylight savings starting day-of-week. Please see
     * the member description for an example.
     * @param time the daylight savings starting time. Please see the member
     * description for an example.
     * @param status An UErrorCode
     */
    void setStartRule(int32_t month, int32_t dayOfWeekInMonth, int32_t dayOfWeek,
                             int32_t time, UErrorCode& status);

    /**
     * Sets the DST start rule to a fixed date within a month.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    The date in that month (1-based).
     * @param time          The time of that day (number of millis after midnight)
     *                      when DST takes effect in local wall time, which is
     *                      standard time in this case.
     * @param status An UErrorCode
     */
    void setStartRule(int32_t month, int32_t dayOfMonth, int32_t time, 
                        UErrorCode& status);

    /**
     * Sets the DST start rule to a weekday before or after a give date within
     * a month, e.g., the first Monday on or after the 8th.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    A date within that month (1-based).
     * @param dayOfWeek     The day of the week on which this rule occurs.
     * @param time          The time of that day (number of millis after midnight)
     *                      when DST takes effect in local wall time, which is
     *                      standard time in this case.
     * @param after         If true, this rule selects the first dayOfWeek on
     *                      or after dayOfMonth.  If false, this rule selects
     *                      the last dayOfWeek on or before dayOfMonth.
     * @param status An UErrorCode
     */
    void setStartRule(int32_t month, int32_t dayOfMonth, int32_t dayOfWeek, 
                        int32_t time, bool_t after, UErrorCode& status);

    /**
     * Sets the daylight savings ending rule. For example, in the U.S., Daylight
     * Savings Time ends at the last (-1) Sunday in October, at 2 AM in standard time.
     * Therefore, you can set the end rule by calling:
     * <pre>
     * .   setEndRule(TimeFields.OCTOBER, -1, TimeFields.SUNDAY, 2*60*60*1000);
     * </pre>
     * Various other types of rules can be specified by manipulating the dayOfWeek
     * and dayOfWeekInMonth parameters.  For complete details, see the documentation
     * for setStartRule().
     *
     * @param month the daylight savings ending month. Month is 0-based.
     * eg, 0 for January.
     * @param dayOfWeekInMonth the daylight savings ending
     * day-of-week-in-month. See setStartRule() for a complete explanation.
     * @param dayOfWeek the daylight savings ending day-of-week. See setStartRule()
     * for a complete explanation.
     * @param time the daylight savings ending time. Please see the member
     * description for an example.
     * @param status An UErrorCode
     */
    void setEndRule(int32_t month, int32_t dayOfWeekInMonth, int32_t dayOfWeek,
                           int32_t time, UErrorCode& status);

    /**
     * Sets the DST end rule to a fixed date within a month.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    The date in that month (1-based).
     * @param time          The time of that day (number of millis after midnight)
     *                      when DST ends in local wall time, which is daylight
     *                      time in this case.
     * @param status An UErrorCode
     */
    void setEndRule(int32_t month, int32_t dayOfMonth, int32_t time, UErrorCode& status);

    /**
     * Sets the DST end rule to a weekday before or after a give date within
     * a month, e.g., the first Monday on or after the 8th.
     *
     * @param month         The month in which this rule occurs (0-based).
     * @param dayOfMonth    A date within that month (1-based).
     * @param dayOfWeek     The day of the week on which this rule occurs.
     * @param time          The time of that day (number of millis after midnight)
     *                      when DST ends in local wall time, which is daylight
     *                      time in this case.
     * @param after         If true, this rule selects the first dayOfWeek on
     *                      or after dayOfMonth.  If false, this rule selects
     *                      the last dayOfWeek on or before dayOfMonth.
     * @param status An UErrorCode
     */
    void setEndRule(int32_t month, int32_t dayOfMonth, int32_t dayOfWeek, 
                        int32_t time, bool_t after, UErrorCode& status);

    /**
     * Returns the TimeZone's adjusted GMT offset (i.e., the number of milliseconds to add
     * to GMT to get local time in this time zone, taking daylight savings time into
     * account) as of a particular reference date.  The reference date is used to determine
     * whether daylight savings time is in effect and needs to be figured into the offset
     * that is returned (in other words, what is the adjusted GMT offset in this time zone
     * at this particular date and time?).  For the time zones produced by createTimeZone(),
     * the reference data is specified according to the Gregorian calendar, and the date
     * and time fields are in GMT, NOT local time.
     *
     * @param era        The reference date's era
     * @param year       The reference date's year
     * @param month      The reference date's month (0-based; 0 is January)
     * @param day        The reference date's day-in-month (1-based)
     * @param dayOfWeek  The reference date's day-of-week (1-based; 1 is Sunday)
     * @param millis     The reference date's milliseconds in day, UTT (NOT local time).
     * @return           The offset in milliseconds to add to GMT to get local time.
     */
    virtual int32_t getOffset(uint8_t era, int32_t year, int32_t month, int32_t day,
                              uint8_t dayOfWeek, int32_t millis, UErrorCode& status) const;

    virtual int32_t getOffset(uint8_t era, int32_t year, int32_t month, int32_t day,
                              uint8_t dayOfWeek, int32_t millis) const;

    /**
     * Gets the time zone offset, for current date, modified in case of
     * daylight savings. This is the offset to add *to* UTC to get local time.
     * @param era the era of the given date.
     * @param year the year in the given date.
     * @param month the month in the given date.
     * Month is 0-based. e.g., 0 for January.
     * @param day the day-in-month of the given date.
     * @param dayOfWeek the day-of-week of the given date.
     * @param milliseconds the millis in day in <em>standard</em> local time.
     * @param monthLength the length of the given month in days.
     * @return the offset to add *to* GMT to get local time.
     */
    virtual int32_t getOffset(uint8_t era, int32_t year, int32_t month, int32_t day,
                           uint8_t dayOfWeek, int32_t milliseconds, 
                           int32_t monthLength, UErrorCode& status) const;

    /**
     * Returns the TimeZone's raw GMT offset (i.e., the number of milliseconds to add
     * to GMT to get local time, before taking daylight savings time into account).
     *
     * @return   The TimeZone's raw GMT offset.
     */
    virtual int32_t getRawOffset(void) const;

    /**
     * Sets the TimeZone's raw GMT offset (i.e., the number of milliseconds to add
     * to GMT to get local time, before taking daylight savings time into account).
     *
     * @param offsetMillis  The new raw GMT offset for this time zone.
     */
    virtual void setRawOffset(int32_t offsetMillis);

    /**
     * Sets the amount of time in ms that the clock is advanced during DST.
     * @param millisSavedDuringDST the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     */
    void setDSTSavings(int32_t millisSavedDuringDST, UErrorCode& status);

    // deprecated
    void setDSTSavings(int32_t millisSavedDuringDST);

    /**
     * Returns the amount of time in ms that the clock is advanced during DST.
     * @return the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     */
    int32_t getDSTSavings(void) const;

    /**
     * Queries if this TimeZone uses Daylight Savings Time.
     *
     * @return   True if this TimeZone uses Daylight Savings Time; false otherwise.
     */
    virtual bool_t useDaylightTime(void) const;

    /**
     * Returns true if the given date is within the period when daylight savings time
     * is in effect; false otherwise.  If the TimeZone doesn't observe daylight savings
     * time, this functions always returns false.
     * @param date The date to test.
     * @return true if the given date is in Daylight Savings Time;
     * false otherwise.
     */
    virtual bool_t inDaylightTime(UDate date, UErrorCode& status) const;

    /**
     * Return true if this zone has the same rules and offset as another zone.
     * @param other the TimeZone object to be compared with
     * @return true if the given zone has the same rules and offset as this one
     */
    bool_t hasSameRules(const TimeZone& other) const;

    /**
     * Clones TimeZone objects polymorphically. Clients are responsible for deleting
     * the TimeZone object cloned.
     *
     * @return   A new copy of this TimeZone object.
     */
    virtual TimeZone* clone(void) const;

public:

    /**
     * Override TimeZone Returns a unique class ID POLYMORPHICALLY. Pure virtual
     * override. This method is to implement a simple version of RTTI, since not all C++
     * compilers support genuine RTTI. Polymorphic operator==() and clone() methods call
     * this method.
     *
     * @return   The class ID for this object. All objects of a given class have the
     *           same class ID. Objects of other classes have different class IDs.
     */
    virtual ClassID getDynamicClassID(void) const { return (ClassID)&fgClassID; }

    /**
     * Return the class ID for this class. This is useful only for comparing to a return
     * value from getDynamicClassID(). For example:
     * <pre>
     * .   Base* polymorphic_pointer = createPolymorphicObject();
     * .   if (polymorphic_pointer->getDynamicClassID() ==
     * .       Derived::getStaticClassID()) ...
     * </pre>
     * @return   The class ID for all objects of this class.
     */
    static ClassID getStaticClassID(void) { return (ClassID)&fgClassID; }

private:
    /** 
     * Constants specifying values of startMode and endMode.
     */
    enum EMode
    {
        DOM_MODE = 1,
        DOW_IN_MONTH_MODE,
        DOW_GE_DOM_MODE,
        DOW_LE_DOM_MODE
    };

    /**
     * Compare a given date in the year to a rule. Return 1, 0, or -1, depending
     * on whether the date is after, equal to, or before the rule date. The
     * millis are compared directly against the ruleMillis, so any
     * standard-daylight adjustments must be handled by the caller.
     *
     * @return  1 if the date is after the rule date, -1 if the date is before
     *          the rule date, or 0 if the date is equal to the rule date.
     */
    static int32_t compareToRule(int32_t month, int32_t monthLen, int32_t dayOfMonth,
                                     int32_t dayOfWeek, int32_t millis,
                                     EMode ruleMode, int32_t ruleMonth, int32_t ruleDayOfWeek,
                                     int32_t ruleDay, int32_t ruleMillis);

    /**
     * Given a set of encoded rules in startDay and startDayOfMonth, decode
     * them and set the startMode appropriately.  Do the same for endDay and
     * endDayOfMonth.  
     * <P>
     * Upon entry, the day of week variables may be zero or
     * negative, in order to indicate special modes.  The day of month
     * variables may also be negative.
     * <P>
     * Upon exit, the mode variables will be
     * set, and the day of week and day of month variables will be positive.
     * <P>
     * This method also recognizes a startDay or endDay of zero as indicating
     * no DST.
     */
    void decodeRules(UErrorCode& status);
    void decodeStartRule(UErrorCode& status);
    void decodeEndRule(UErrorCode& status);

    static char     fgClassID;

    int32_t startMonth, startDay, startDayOfWeek;   // the month, day, DOW, and time DST starts
    int32_t startTime;
    int32_t endMonth, endDay, endDayOfWeek; // the month, day, DOW, and time DST ends
    int32_t endTime;
    int32_t startYear;  // the year these DST rules took effect
    int32_t rawOffset;  // the TimeZone's raw GMT offset
    bool_t useDaylight; // flag indicating whether this TimeZone uses DST
    static const int32_t staticMonthLength[12]; // lengths of the months
    EMode startMode, endMode;   // flags indicating what kind of rules the DST rules are

    /**
     * A positive value indicating the amount of time saved during DST in ms.
     * Typically one hour; sometimes 30 minutes.
     */
    int32_t dstSavings;
};

#endif // _SIMPLETZ
