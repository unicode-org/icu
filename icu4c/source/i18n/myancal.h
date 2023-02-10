// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 * File MYANCAL.H
 *
 */

#ifndef MYANCAL_H
#define MYANCAL_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/calendar.h"

U_NAMESPACE_BEGIN

/**
 * <code>MyanmarCalendar</code> is a subclass of <code>Calendar</code>
 * that implements the traditional Buddhist calendar used today in Myanmar (Burma).
 * <p>
 * The Myanmar is lunisolar, with months following the lunar cycle (29 or 30 days)
 * and additional days being inserted on a regular basis to sync with the solar year.
 * In practice the days of the month can be labeled waxing 1-14, full moon, waning 1-14, and new moon
 * There are two leap year types: little watat (with a full leap month named 2nd Waso) occurring every 2-3 years,
 * and big watat (which adds 2nd Waso and one more day in the month of Nayon).
 * Big Watat is declared officially by the Myanmar government and very rare (once in past 50 years).
 * <p>
 * The Myanmar calendar has been recalibrated over the years and has been in its current
 * era since 1951 CE. Additional months such as Late Tagu / Late Kason, and other concepts are
 * supported to accommodate older eras.
 *
 * @see GregorianCalendar
 *
 * @author Nick Doiron
 * @internal
 */
class MyanmarCalendar : public Calendar {
 public:
  //-------------------------------------------------------------------------
  // Constants...
  //-------------------------------------------------------------------------
  /**
   * Constants for the months
   * @internal
   */
  enum EMonths {
    /**
     * Months of Myanmar calendar
     * @internal
     */
    TAGU = 1,
    KASON = 2,
    NAYON = 3,
    WASO = 4,
    SECOND_WASO = 5,
    WAGAUNG = 6,
    TAWTHALIN = 7,
    THADINGYUT = 8,
    TAZAUNGMON = 9,
    NADAW = 10,
    PYATHO = 11,
    TABODWE = 12,
    TABAUNG = 13,
    LATE_TAGU = 14,
    LATE_KASON = 15,
    MYANMAR_MONTH_MAX
  };



  //-------------------------------------------------------------------------
  // Constructors...
  //-------------------------------------------------------------------------

  /**
   * Constructs a MyanmarCalendar based on the current time in the default time zone
   * with the given locale.
   *
   * @param aLocale  The given locale.
   * @param success  Indicates the status of MyanmarCalendar object construction.
   *                 Returns U_ZERO_ERROR if constructed successfully.
   * @internal
   */
  MyanmarCalendar(const Locale& aLocale, UErrorCode &success);

  /**
   * Copy Constructor
   * @internal
   */
  MyanmarCalendar(const MyanmarCalendar& other);

  /**
   * Destructor.
   * @internal
   */
  virtual ~MyanmarCalendar();

  // clone
  virtual Calendar* clone() const;

 private:
  /**
   * Determine whether a year is a common, little watat, or big watat year in the Myanmar calendar
   */
  static int32_t isLeapYear(int32_t year);

  /**
   * Return the day # on which the given year starts.  Days are counted
   * from the Myanmar Era epoch, origin 0.
   */
  int32_t yearStart(int32_t year);

  /**
   * Return the day # on which the given month starts.  Days are counted
   * from the Myanmar Era epoch, origin 0.
   *
   * @param year  The Myanmar year
   * @param year  The Myanmar month, 1-based
   */
  int32_t monthStart(int32_t year, int32_t month) const;
  long bSearch2(int32_t k, long (*A)[2], long u) const;
  long bSearch1(int32_t k,long* A, long u) const;
  void GetMyConst(int32_t my, double& EI, double& WO, double& NM, long& EW) const;
  void cal_watat(int32_t my, long& watat, long& fm) const;
  void cal_my(int32_t my, int32_t& myt, long& tg1, long& fm, long& werr) const;

  //----------------------------------------------------------------------
  // Calendar framework
  //----------------------------------------------------------------------
 protected:
  /**
   * @internal
   */
  virtual int32_t handleGetLimit(UCalendarDateFields field, ELimitType limitType) const;

  /**
   * Return the length (in days) of the given month.
   *
   * @param year  The Myanmar year
   * @param year  The Myanmar month, 1-based
   * @internal
   */
  virtual int32_t handleGetMonthLength(int32_t extendedYear, int32_t month) const;

  /**
   * Return the number of days in the given Myanmar year
   * @internal
   */
  virtual int32_t handleGetYearLength(int32_t extendedYear) const;

  //-------------------------------------------------------------------------
  // Functions for converting from field values to milliseconds....
  //-------------------------------------------------------------------------

  // Return JD of start of given month/year
  /**
   * @internal
   */
  virtual int32_t handleComputeMonthStart(int32_t eyear, int32_t month, UBool useMonth) const;

  //-------------------------------------------------------------------------
  // Functions for converting from milliseconds to field values
  //-------------------------------------------------------------------------

  /**
   * @internal
   */
  virtual int32_t handleGetExtendedYear();

  /**
   * Override Calendar to compute several fields specific to the Myanmar
   * calendar system.  These are:
   *
   * <ul><li>ERA
   * <li>YEAR
   * <li>MONTH
   * <li>DAY_OF_MONTH
   * <li>DAY_OF_YEAR
   * <li>EXTENDED_YEAR</ul>
   *
   * The DAY_OF_WEEK and DOW_LOCAL fields are already set when this
   * method is called. The getGregorianXxx() methods return Gregorian
   * calendar equivalents for the given Julian day.
   * @internal
   */
  virtual void handleComputeFields(int32_t julianDay, UErrorCode &status);

  // UObject stuff
 public:
  /**
   * @return   The class ID for this object. All objects of a given class have the
   *           same class ID. Objects of other classes have different class IDs.
   * @internal
   */
  virtual UClassID getDynamicClassID(void) const;

  /**
   * Return the class ID for this class. This is useful only for comparing to a return
   * value from getDynamicClassID(). For example:
   *
   *      Base* polymorphic_pointer = createPolymorphicObject();
   *      if (polymorphic_pointer->getDynamicClassID() ==
   *          Derived::getStaticClassID()) ...
   *
   * @return   The class ID for all objects of this class.
   * @internal
   */
  U_I18N_API static UClassID U_EXPORT2 getStaticClassID(void);

  /**
   * return the calendar type, "myanmar".
   *
   * @return calendar type
   * @internal
   */
  virtual const char * getType() const;

 private:
  MyanmarCalendar(); // default constructor not implemented

 protected:

  /**
   * Returns TRUE because the Myanmar Calendar does have a default century
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
