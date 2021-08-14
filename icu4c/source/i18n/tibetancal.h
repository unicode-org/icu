// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *****************************************************************************
 * Copyright (C) 2003-2021, International Business Machines Corporation
 * and others. All Rights Reserved.
 *****************************************************************************
 *
 * File TIBETANCAL.H
 *****************************************************************************
 */

#ifndef TIBETANCAL_H
#define TIBETANCAL_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/calendar.h"

U_NAMESPACE_BEGIN

/**
*  Concrete class which provides the Tibetan calendar.
*
*
*
*
*
*
*
*/


class U_I18N_API TibetanCalendar : public Calendar {
public:
  /**
   * Useful constants for TibetanCalendar.
   * @internal
   */


  /**
   * Calendar type - phugpa or tsurphu
   * @internal 
   */
  enum ECalculationType {
    PHUGPA,
    TSURPHU
  };

  //-------------------------------------------------------------------------
  // Constructors...
  //-------------------------------------------------------------------------

  /**
   * Constructs an TibetanCalendar based on the current time in the default time zone
   * with the given locale.
   *
   * @param aLocale  The given locale.
   * @param success  Indicates the status of TibetanCalendar object construction.
   *                 Returns U_ZERO_ERROR if constructed successfully.
   * @param type     The Tibetan calendar calculation type. The default vaue is PHUGPA.
   * @internal
   */
  TibetanCalendar(const Locale& aLocale, UErrorCode &success, ECalculationType type = PHUGPA);


  /**
   * Copy Constructor
   * @internal
   */
  TibetanCalendar(const TibetanCalendar& other);


  /**
   * Destructor.
   * @internal
   */
  virtual ~TibetanCalendar();


  /**
   * Sets Tibetan calendar calculation type used by this instance.
   *
   * @param type    The calendar calculation type, <code>PHUGPA</code> to use the phugpa
   *                calendar, <code>TSURPHU</code> to use the tsurphu calendar.
   * @internal
   */
  void setCalculationType(ECalculationType type, UErrorCode &status);


  /**
   * Returns <code>true</code> if this object is using the phugpa
   * calendar, or <code>false</code> if using the tsurphu calendar.
   * @internal
   */
  UBool isPhugpa();


  // clone
  virtual TibetanCalendar* clone() const;



  /**
   * return the calendar type, "tibetan-phugpa" or "tibetan-tsurphu".
   *
   * @return calendar type
   * @internal
   */
  virtual const char * getType() const;


private:

  //-------------------------------------------------------------------------
  // Internal data....
  //-------------------------------------------------------------------------

/**
 * Return the moonTab by using the table for the given integer value. 
 * @param moonTab An integer number
 */
int32_t moonTab(int32_t moonTab) const;


/**
 * Return the sunTab by using the table for the given integer value. 
 * @param sunTab An integer number
 */
int32_t sunTab(int32_t sunTab) const;


/**
 * Return the modulo of the number if(num % mod != 0) else is return mod.
 * @param num the number to be divided
 * @param mod the number to be divided with
 */
int32_t amod(int32_t num, int32_t mod) const;


/**
 * Return the month count(based on the epoch) from the given Tibetan year, month number and leap month indicator.
 * @param month the month number
 * @param is_leap_month flag indicating whether or not the given month is leap month
 */
int32_t toMonthCount(int32_t eyear, int32_t month, int32_t is_leap_month) const;


/**
 * Return the julian date at the end of the lunar day (similar to the month count since the beginning of the epoch, but for days)
 * It is calculated by first calculating a simpler mean date corresponding to the linear mean motion of the moon and then adjusting 
 * it by the equations of the sun and moon, which are determined by the anomalies of the sun and moon together with tables.
 * @param day the tibetan day
 * @param monthCount month count since the begining of epoch
 */
int32_t trueDate(int32_t day, int32_t monthCount) const;



public:

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
  virtual void add(UCalendarDateFields field, int32_t amount, UErrorCode& status);
    

  /**
   * @deprecated ICU 2.6 use UCalendarDateFields instead of EDateFields
   */
  virtual void add(EDateFields field, int32_t amount, UErrorCode& status)


  /**
   * (Overrides Calendar) Rolls up or down by the given amount in the specified field.
   * For more information, see the documentation for Calendar::roll().
   *
   * @param field   The time field.
   * @param amount  Indicates amount to roll.
   * @param status  Output param set to success/failure code on exit. If any value
   *                previously set in the time field is invalid, this will be set to
   *                an error status.
   * @internal
   */
  virtual void roll(UCalendarDateFields field, int32_t amount, UErrorCode& status);


  /**
   * @deprecated ICU 2.6. Use roll(UCalendarDateFields field, int32_t amount, UErrorCode& status) instead.
`  */
  virtual void roll(EDateFields field, int32_t amount, UErrorCode& status);


  //----------------------------------------------------------------------
  // Calendar framework
  //----------------------------------------------------------------------

private:
    TibetanCalendar(); // default constructor not implemented

protected:


  /**
   * Return the length (in days) of the given month.
   * @param eyear the extended year
   * @param month the month number
   * @param return the number of days in the given month
   * @internal
   */
  virtual int32_t handleGetMonthLength(int32_t eyear, int32_t month);


  /**
   * Return the Julian day number of day before the first day of the
   * given month in the given extended year.  Subclasses should override
   * this method to implement their calendar system.
   * @param eyear the extended year
   * @param month the month number, or 0 if useMonth is false
   * @param useMonth if false, compute the day before the first day of
   * the given year, otherwise, compute the day before the first day of
   * the given month
   * @param return the Julian day number of the day before the first
   * day of the given month and year
   * @internal
   */
  virtual int32_t handleComputeMonthStart(int32_t eyear, int32_t month,
                                                   UBool useMonth) const;


  /**
    * Subclasses may override this method to compute several fields
    * specific to each calendar system.  These are:
    *
    * <ul><li>ERA
    * <li>YEAR
    * <li>MONTH
    * <li>DAY_OF_MONTH
    * <li>DAY_OF_YEAR
    * <li>EXTENDED_YEAR</ul>
    *
    * <p>The GregorianCalendar implementation implements
    * a calendar with the specified Julian/Gregorian cutover date.
    * @internal
    */
  virtual void handleComputeFields(int32_t julianDay, UErrorCode &status);


  /**
   * Calculate the limit for a specified type of limit and field
   * @internal
   */
  virtual int32_t handleGetLimit(UCalendarDateFields field, ELimitType limitType) const;



  /**
   * Return the extended year defined by the current fields.
   * @internal
   */
  virtual int32_t handleGetExtendedYear();


  /**
   * (Overrides Calendar) Return true if the current date for this Calendar is in
   * Daylight Savings Time. Recognizes DST_OFFSET, if it is set.
   *
   * @param status Fill-in parameter which receives the status of this operation.
   * @return   True if the current date for this Calendar is in Daylight Savings Time,
   *           false, otherwise.
   * @internal
   */
  virtual UBool inDaylightTime(UErrorCode& status) const;


  /**
   * Returns true because the Tibetan Calendar does have a default century
   * @internal
   */
  virtual UBool haveDefaultCentury() const;


  /**
   * Returns the date of the start of the default century
   * @return start of century - in milliseconds since epoch, 1970
   * @internal
   */
  virtual UDate defaultCenturyStart() const;


  /**
   * Returns the year in which the default century begins
   * @internal
   */
  virtual int32_t defaultCenturyStartYear() const;

};

U_NAMESPACE_END

#endif
#endif