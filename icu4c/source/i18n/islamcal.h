/*
 ********************************************************************************
 * Copyright (C) 2003-2013, International Business Machines Corporation
 * and others. All Rights Reserved.
 ******************************************************************************
 *
 * File ISLAMCAL.H
 *
 * Modification History:
 *
 *   Date        Name        Description
 *   10/14/2003  srl         ported from java IslamicCalendar
 *****************************************************************************
 */

#ifndef ISLAMCAL_H
#define ISLAMCAL_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/calendar.h"

U_NAMESPACE_BEGIN

/**
 * <code>IslamicCalendar</code> is a subclass of <code>Calendar</code>
 * that implements the Islamic civil and religious calendars.  It
 * is used as the civil calendar in most of the Arab world and the
 * liturgical calendar of the Islamic faith worldwide.  This calendar
 * is also known as the "Hijri" calendar, since it starts at the time
 * of Mohammed's emigration (or "hijra") to Medinah on Thursday, 
 * July 15, 622 AD (Julian).
 * <p>
 * The Islamic calendar is strictly lunar, and thus an Islamic year of twelve
 * lunar months does not correspond to the solar year used by most other
 * calendar systems, including the Gregorian.  An Islamic year is, on average,
 * about 354 days long, so each successive Islamic year starts about 11 days
 * earlier in the corresponding Gregorian year.
 * <p>
 * Each month of the calendar starts when the new moon's crescent is visible
 * at sunset.  However, in order to keep the time fields in this class
 * synchronized with those of the other calendars and with local clock time,
 * we treat days and months as beginning at midnight,
 * roughly 6 hours after the corresponding sunset.
 * <p>
 * There are two main variants of the Islamic calendar in existence.  The first
 * is the <em>civil</em> calendar, which uses a fixed cycle of alternating 29-
 * and 30-day months, with a leap day added to the last month of 11 out of
 * every 30 years.  This calendar is easily calculated and thus predictable in
 * advance, so it is used as the civil calendar in a number of Arab countries.
 * This is the default behavior of a newly-created <code>IslamicCalendar</code>
 * object.
 * <p>
 * The Islamic <em>religious</em> calendar, however, is based on the <em>observation</em>
 * of the crescent moon.  It is thus affected by the position at which the
 * observations are made, seasonal variations in the time of sunset, the
 * eccentricities of the moon's orbit, and even the weather at the observation
 * site.  This makes it impossible to calculate in advance, and it causes the
 * start of a month in the religious calendar to differ from the civil calendar
 * by up to three days.
 * <p>
 * Using astronomical calculations for the position of the sun and moon, the
 * moon's illumination, and other factors, it is possible to determine the start
 * of a lunar month with a fairly high degree of certainty.  However, these
 * calculations are extremely complicated and thus slow, so most algorithms,
 * including the one used here, are only approximations of the true astronical
 * calculations.  At present, the approximations used in this class are fairly
 * simplistic; they will be improved in later versions of the code.
 * <p>
 * The {@link #setCivil setCivil} method determines
 * which approach is used to determine the start of a month.  By default, the
 * fixed-cycle civil calendar is used.  However, if <code>setCivil(false)</code>
 * is called, an approximation of the true lunar calendar will be used.
 *
 * @see GregorianCalendar
 *
 * @author Laura Werner
 * @author Alan Liu
 * @author Steven R. Loomis
 * @internal
 */
class U_I18N_API IslamicCalendar : public Calendar {
 public:
  //-------------------------------------------------------------------------
  // Constants...
  //-------------------------------------------------------------------------
  
  /**
   * Calendar type - civil or religious or um alqura
   * @internal 
   */
  enum ECivil {
    ASTRONOMICAL,
    CIVIL,
	UMALQURA,
    TBLA
  };
  
  /**
   * Constants for the months
   * @internal
   */
  enum EMonths {
    /**
     * Constant for Muharram, the 1st month of the Islamic year. 
     * @internal
     */
    MUHARRAM = 0,

    /**
     * Constant for Safar, the 2nd month of the Islamic year. 
     * @internal
     */
    SAFAR = 1,

    /**
     * Constant for Rabi' al-awwal (or Rabi' I), the 3rd month of the Islamic year. 
     * @internal 
     */
    RABI_1 = 2,

    /**
     * Constant for Rabi' al-thani or (Rabi' II), the 4th month of the Islamic year. 
     * @internal 
     */
    RABI_2 = 3,

    /**
     * Constant for Jumada al-awwal or (Jumada I), the 5th month of the Islamic year. 
     * @internal 
     */
    JUMADA_1 = 4,

    /**
     * Constant for Jumada al-thani or (Jumada II), the 6th month of the Islamic year. 
     * @internal 
     */
    JUMADA_2 = 5,

    /**
     * Constant for Rajab, the 7th month of the Islamic year. 
     * @internal 
     */
    RAJAB = 6,

    /**
     * Constant for Sha'ban, the 8th month of the Islamic year. 
     * @internal 
     */
    SHABAN = 7,

    /**
     * Constant for Ramadan, the 9th month of the Islamic year. 
     * @internal 
     */
    RAMADAN = 8,

    /**
     * Constant for Shawwal, the 10th month of the Islamic year. 
     * @internal 
     */
    SHAWWAL = 9,

    /**
     * Constant for Dhu al-Qi'dah, the 11th month of the Islamic year. 
     * @internal 
     */
    DHU_AL_QIDAH = 10,

    /**
     * Constant for Dhu al-Hijjah, the 12th month of the Islamic year. 
     * @internal 
     */
    DHU_AL_HIJJAH = 11,
    
    ISLAMIC_MONTH_MAX
  }; 


  //-------------------------------------------------------------------------
  // Constructors...
  //-------------------------------------------------------------------------

  /**
   * Constructs an IslamicCalendar based on the current time in the default time zone
   * with the given locale.
   *
   * @param aLocale  The given locale.
   * @param success  Indicates the status of IslamicCalendar object construction.
   *                 Returns U_ZERO_ERROR if constructed successfully.
   * @param beCivil  Whether the calendar should be civil (default-TRUE) or religious (FALSE)
   * @internal
   */
  IslamicCalendar(const Locale& aLocale, UErrorCode &success, ECivil beCivil = CIVIL);

  /**
   * Copy Constructor
   * @internal
   */
  IslamicCalendar(const IslamicCalendar& other);

  /**
   * Destructor.
   * @internal
   */
  virtual ~IslamicCalendar();

  /**
   * Determines whether this object uses the fixed-cycle Islamic civil calendar
   * or an approximation of the religious, astronomical calendar.
   *
   * @param beCivil   <code>CIVIL</code> to use the civil calendar,
   *                  <code>ASTRONOMICAL</code> to use the astronomical calendar.
   * @internal
   */
  void setCivil(ECivil beCivil, UErrorCode &status);
    
  /**
   * Returns <code>true</code> if this object is using the fixed-cycle civil
   * calendar, or <code>false</code> if using the religious, astronomical
   * calendar.
   * @internal
   */
  UBool isCivil();


  // TODO: copy c'tor, etc

  // clone
  virtual Calendar* clone() const;

 private:
  /**
   * Determine whether a year is a leap year in the Islamic civil calendar
   */
  static UBool civilLeapYear(int32_t year);
    
  /**
   * Return the day # on which the given year starts.  Days are counted
   * from the Hijri epoch, origin 0.
   */
  int32_t yearStart(int32_t year) const;

  /**
   * Return the day # on which the given month starts.  Days are counted
   * from the Hijri epoch, origin 0.
   *
   * @param year  The hijri year
   * @param year  The hijri month, 0-based
   */
  int32_t monthStart(int32_t year, int32_t month) const;
    
  /**
   * Find the day number on which a particular month of the true/lunar
   * Islamic calendar starts.
   *
   * @param month The month in question, origin 0 from the Hijri epoch
   *
   * @return The day number on which the given month starts.
   */
  int32_t trueMonthStart(int32_t month) const;

  /**
   * Return the "age" of the moon at the given time; this is the difference
   * in ecliptic latitude between the moon and the sun.  This method simply
   * calls CalendarAstronomer.moonAge, converts to degrees, 
   * and adjusts the resultto be in the range [-180, 180].
   *
   * @param time  The time at which the moon's age is desired,
   *              in millis since 1/1/1970.
   */
  static double moonAge(UDate time, UErrorCode &status);

  //-------------------------------------------------------------------------
  // Internal data....
  //
    
  /**
   * <code>CIVIL</code> if this object uses the fixed-cycle Islamic civil calendar,
   * and <code>ASTRONOMICAL</code> if it approximates the true religious calendar using
   * astronomical calculations for the time of the new moon.
   */
  ECivil civil;

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
   * @param year  The hijri year
   * @param year  The hijri month, 0-based
   * @internal
   */
  virtual int32_t handleGetMonthLength(int32_t extendedYear, int32_t month) const;
  
  /**
   * Return the number of days in the given Islamic year
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
   * Override Calendar to compute several fields specific to the Islamic
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
  /*U_I18N_API*/ static UClassID U_EXPORT2 getStaticClassID(void);

  /**
   * return the calendar type, "buddhist".
   *
   * @return calendar type
   * @internal
   */
  virtual const char * getType() const;

 private:
  IslamicCalendar(); // default constructor not implemented

  // Default century.
 protected:

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
   * Returns TRUE because the Islamic Calendar does have a default century
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

 private: // default century stuff.
  /**
   * The system maintains a static default century start date.  This is initialized
   * the first time it is used.  Before then, it is set to SYSTEM_DEFAULT_CENTURY to
   * indicate an uninitialized state.  Once the system default century date and year
   * are set, they do not change.
   */
  static UDate         fgSystemDefaultCenturyStart;

  /**
   * See documentation for systemDefaultCenturyStart.
   */
  static int32_t          fgSystemDefaultCenturyStartYear;

  
  /**
   * Default value that indicates the defaultCenturyStartYear is unitialized
   */
  static const int32_t    fgSystemDefaultCenturyYear;

  /**
   * start of default century, as a date
   */
  static const UDate        fgSystemDefaultCentury;

  static const int32_t UMALQURA_YEAR_START = 1318;
  static const int32_t UMALQURA_YEAR_END = 1480;
  

    /**
     * Friday EPOC
     */
    static const int32_t CIVIL_EPOC = 1948440;

    /**
     * Thursday EPOC
     */
    static const int32_t ASTRONOMICAL_EPOC = 1948439;


  static const int getUmalqura_MonthLength(int i, int j){

    static const int UMALQURA_MONTHLENGTH[] = {    	    	
        //* 1318 -1322 */ "0101 0111 0100", "1001 0111 0110", "0100 1011 0111", "0010 0101 0111", "0101 0010 1011",
                               0x0574,           0x0975,           0x06A7,           0x0257,           0x052B,            
        //* 1323 -1327 */ "0110 1001 0101", "0110 1100 1010", "1010 1101 0101", "0101 0101 1011", "0010 0101 1101",                 
                               0x0695,           0x06CA,           0x0AD5,           0x055B,           0x025B,                  
        //* 1328 -1332 */ "1001 0010 1101", "1100 1001 0101", "1101 0100 1010", "1110 1010 0101", "0110 1101 0010",                 
                               0x092D,           0x0C95,           0x0D4A,           0x0E5B,           0x025B,                  
        //* 1333 -1337 */ "1010 1101 0101", "0101 0101 1010", "1010 1010 1011", "0100 0100 1011", "0110 1010 0101",                 
                               0x0AD5,           0x055A,           0x0AAB,           0x044B,           0x06A5,                  
        //* 1338 -1342 */ "0111 0101 0010", "1011 1010 1001", "0011 0111 0100", "1010 1011 0110", "0101 0101 0110",                  
                               0x0752,           0x0BA9,           0x0374,           0x0AB6,           0x0556,                  
        //* 1343 -1347 */ "1010 1010 1010", "1101 0101 0010", "1101 1010 1001", "0101 1101 0100", "1010 1110 1010", 
                               0x0AAA,           0x0D52,           0x0DA9,           0x05D4,           0x0AEA,                  
        //* 1348 -1352 */ "0100 1101 1101", "0010 0110 1110", "1001 0010 1110", "1010 1010 0110", "1101 0101 0100", 
                               0x04DD,           0x026E,           0x092E,           0x0AA6,           0x0D54,                  
        //* 1353 -1357 */ "0101 1010 1010", "0101 1011 0101", "0010 1011 0100", "1001 0011 0111", "0100 1001 1011", 
                               0x05AA,           0x05B5,           0x02B4,           0x0937,           0x049B,                  
        //* 1358 -1362 */ "1010 0100 1011", "1011 0010 0101", "1011 0101 0100", "1011 0110 1010", "0101 0110 1101", 
                               0x0A4B,           0x0B25,           0x0B54,           0x0B6A,           0x056D,                  
        //* 1363 -1367 */ "0100 1010 1101", "1010 0101 0101", "1101 0010 0101", "1110 1001 0010", "1110 1100 1001", 
                               0x04AD,           0x0A55,           0x0D25,           0x0E92,           0x0EC9,                  
        //* 1368 -1372 */ "0110 1101 0100", "1010 1110 1010", "0101 0110 1011", "0100 1010 1011", "0110 1000 0101", 
                               0x06D4,           0x0ADA,           0x056B,           0x04AB,           0x0685,                  
        //* 1373 -1377 */ "1011 0100 1001", "1011 1010 0100", "1011 1011 0010", "0101 1011 0101", "0010 1011 1010", 
                               0x0B49,           0x0BA4,           0x0BB2,           0x05B5,           0x02BA,                  
        //* 1378 -1382 */ "1001 0101 1011", "0100 1010 1011", "0101 0101 0101", "0110 1011 0010", "0110 1101 1001", 
                               0x095B,           0x04AB,           0x0555,           0x06B2,           0x06D9,                  
        //* 1383 -1387 */ "0010 1110 1100", "1001 0110 1110", "0100 1010 1110", "1010 0101 0110", "1101 0010 1010", 
                               0x02EC,           0x096E,           0x04AE,           0x0A56,           0x0D2A,                  
        //* 1388 -1392 */ "1101 0101 0101", "0101 1010 1010", "1010 1011 0101", "0100 1011 1011", "0000 0101 1011", 
                               0x0D55,           0x05AA,           0x0AB5,           0x04BB,           0x005B,                  
        //* 1393 -1397 */ "1001 0010 1011", "1010 1001 0101", "0011 0100 1010", "1011 1010 0101", "0101 1010 1010", 
                               0x092B,           0x0A95,           0x034A,           0x0BA5,           0x05AA,                  
        //* 1398 -1402 */ "1010 1011 0101", "0101 0101 0110", "1010 1001 0110", "1101 0100 1010", "1110 1010 0101", 
                               0x0AB5,           0x0556,           0x0A96,           0x0B4A,           0x0EA5,                  
        //* 1403 -1407 */ "0111 0101 0010", "0110 1110 1001", "0011 0110 1010", "1010 1010 1101", "0101 0101 0101", 
                               0x0752,           0x06E9,           0x036A,           0x0AAD,           0x0555,                  
        //* 1408 -1412 */ "1010 1010 0101", "1011 0101 0010", "1011 1010 1001", "0101 1011 0100", "1001 1011 1010", 
                               0x0AA5,           0x0B52,           0x0BA9,           0x05B4,           0x09BA,                  
        //* 1413 -1417 */ "0100 1101 1011", "0010 0101 1101", "0101 0010 1101", "1010 1010 0101", "1010 1101 0100", 
                               0x04DB,           0x025D,           0x052D,           0x0AA5,           0x0AD4,              
        //* 1418 -1422 */ "1010 1110 1010", "0101 0110 1101", "0100 1011 1101", "0010 0011 1101", "1001 0001 1101", 
                               0x0AEA,           0x056D,           0x04BD,           0x023D,           0x091D,                  
        //* 1423 -1427 */ "1010 1001 0101", "1011 0100 1010", "1011 0101 1010", "0101 0110 1101", "0010 1011 0110", 
                               0x0A95,           0x0B4A,           0x0B5A,           0x056D,           0x02B6,                  
        //* 1428 -1432 */ "1001 0011 1011", "0100 1001 1011", "0110 0101 0101", "0110 1010 1001", "0111 0101 0100", 
                               0x093B,           0x049B,           0x0655,           0x06A9,           0x0754,                  
        //* 1433 -1437 */ "1011 0110 1010", "0101 0110 1100", "1010 1010 1101", "0101 0101 0101", "1011 0010 1001", 
                               0x0B6A,           0x056C,           0x0AAD,           0x0555,           0x0B29,                  
        //* 1438 -1442 */ "1011 1001 0010", "1011 1010 1001", "0101 1101 0100", "1010 1101 1010", "0101 0101 1010", 
                               0x0B92,           0x0BA9,           0x05D4,           0x0ADA,           0x055A,                  
        //* 1443 -1447 */ "1010 1010 1011", "0101 1001 0101", "0111 0100 1001", "0111 0110 0100", "1011 1010 1010", 
                               0x0AAB,           0x0595,           0x0749,           0x0764,           0x0BAA,                  
        //* 1448 -1452 */ "0101 1011 0101", "0010 1011 0110", "1010 0101 0110", "1110 0100 1101", "1011 0010 0101",
                               0x05B5,           0x02B6,           0x0A56,           0x0E4D,           0x0B25,                  
        //* 1453 -1457 */ "1011 0101 0010", "1011 0110 1010", "0101 1010 1101", "0010 1010 1110", "1001 0010 1111",
                               0x0B52,           0x0B6A,           0x05AD,           0x02AE,           0x092F,                  
        //* 1458 -1462 */ "0100 1001 0111", "0110 0100 1011", "0110 1010 0101", "0110 1010 1100", "1010 1101 0110",
                               0x0497,           0x064B,           0x06A5,           0x06AC,           0x0AD6,                  
        //* 1463 -1467 */ "0101 0101 1101", "0100 1001 1101", "1010 0100 1101", "1101 0001 0110", "1101 1001 0101",
                               0x055D,           0x049D,           0x0A4D,           0x0D16,           0x0D95,                  
        //* 1468 -1472 */ "0101 1010 1010", "0101 1011 0101", "0010 1001 1010", "1001 0101 1011", "0100 1010 1100",
                               0x05AA,           0x05B5,           0x029A,           0x095B,           0x04AC,                  
        //* 1473 -1477 */ "0101 1001 0101", "0110 1100 1010", "0110 1110 0100", "1010 1110 1010", "0100 1111 0101",
                               0x0595,           0x06CA,           0x06E4,           0x0AEA,           0x04F5,                  
        //* 1478 -1480 */ "0010 1011 0110", "1001 0101 0110", "1010 1010 1010"   
                               0x02B6,           0x0956,           0x0AAA                  
    };

        int mask = (int) (0x01 << (11 - j));            // set mask for bit corresponding to month
        if((UMALQURA_MONTHLENGTH[i] & mask) == 0 )    
        	return 29;
        else
        	return 30;
  }

 
  /**
   * Returns the beginning date of the 100-year window that dates 
   * with 2-digit years are considered to fall within.
   */
  UDate         internalGetDefaultCenturyStart(void) const;

  /**
   * Returns the first year of the 100-year window that dates with 
   * 2-digit years are considered to fall within.
   */
  int32_t          internalGetDefaultCenturyStartYear(void) const;

  /**
   * Initializes the 100-year window that dates with 2-digit years
   * are considered to fall within so that its start date is 80 years
   * before the current time.
   */
  static void  initializeSystemDefaultCentury(void);
};

U_NAMESPACE_END

#endif
#endif



