
/************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation *
 * and others. All Rights Reserved.                                     *
 ************************************************************************
 *  2003-nov-07   srl       Port from Java
 */

#ifndef ASTRO_H
#define ASTRO_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "gregoimp.h"  // for Math
#include "stdio.h"  // for sprintf
#include "unicode/unistr.h"

/**
 * <code>CalendarAstronomer</code> is a class that can perform the calculations to
 * determine the positions of the sun and moon, the time of sunrise and
 * sunset, and other astronomy-related data.  The calculations it performs
 * are in some cases quite complicated, and this utility class saves you
 * the trouble of worrying about them.
 * <p>
 * The measurement of time is a very important part of astronomy.  Because
 * astronomical bodies are constantly in motion, observations are only valid
 * at a given moment in time.  Accordingly, each <code>CalendarAstronomer</code>
 * object has a <code>time</code> property that determines the date
 * and time for which its calculations are performed.  You can set and
 * retrieve this property with {@link #setDate setDate}, {@link #getDate getDate}
 * and related methods.
 * <p>
 * Almost all of the calculations performed by this class, or by any
 * astronomer, are approximations to various degrees of accuracy.  The
 * calculations in this class are mostly modelled after those described
 * in the book
 * <a href="http://www.amazon.com/exec/obidos/ISBN=0521356997" target="_top">
 * Practical Astronomy With Your Calculator</a>, by Peter J.
 * Duffett-Smith, Cambridge University Press, 1990.  This is an excellent
 * book, and if you want a greater understanding of how these calculations
 * are performed it a very good, readable starting point.
 * <p>
 * <strong>WARNING:</strong> This class is very early in its development, and
 * it is highly likely that its API will change to some degree in the future.
 * At the moment, it basically does just enough to support {@link IslamicCalendar}
 * and {@link ChineseCalendar}.
 *
 * @author Laura Werner
 * @author Alan Liu
 * @internal
 */
class U_I18N_API CalendarAstronomer : public UMemory {
public:
  // some classes

public:
  /**
   * Represents the position of an object in the sky relative to the ecliptic,
   * the plane of the earth's orbit around the Sun. 
   * This is a spherical coordinate system in which the latitude
   * specifies the position north or south of the plane of the ecliptic.
   * The longitude specifies the position along the ecliptic plane
   * relative to the "First Point of Aries", which is the Sun's position in the sky
   * at the Vernal Equinox.
   * <p>
   * Note that Ecliptic objects are immutable and cannot be modified
   * once they are constructed.  This allows them to be passed and returned by
   * value without worrying about whether other code will modify them.
   *
   * @see CalendarAstronomer.Equatorial
   * @see CalendarAstronomer.Horizon
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  class U_I18N_API Ecliptic : public UMemory {
  public:
    /**
     * Constructs an Ecliptic coordinate object.
     * <p>
     * @param lat The ecliptic latitude, measured in radians.
     * @param lon The ecliptic longitude, measured in radians.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    Ecliptic(double lat, double lon) {
      latitude = lat;
      longitude = lon;
    }

    /**
     * Return a string representation of this object
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    UnicodeString toString() {
      char tmp[800];
      sprintf(tmp, "[%.5f,%.5f]", longitude*RAD_DEG, latitude*RAD_DEG);
      return UnicodeString(tmp);
    }
        
    /**
     * The ecliptic latitude, in radians.  This specifies an object's
     * position north or south of the plane of the ecliptic,
     * with positive angles representing north.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    double latitude;
        
    /**
     * The ecliptic longitude, in radians.
     * This specifies an object's position along the ecliptic plane
     * relative to the "First Point of Aries", which is the Sun's position
     * in the sky at the Vernal Equinox,
     * with positive angles representing east.
     * <p>
     * A bit of trivia: the first point of Aries is currently in the
     * constellation Pisces, due to the precession of the earth's axis.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    double longitude;
  };

  /**
   * Represents the position of an 
   * object in the sky relative to the plane of the earth's equator. 
   * The <i>Right Ascension</i> specifies the position east or west
   * along the equator, relative to the sun's position at the vernal
   * equinox.  The <i>Declination</i> is the position north or south
   * of the equatorial plane.
   * <p>
   * Note that Equatorial objects are immutable and cannot be modified
   * once they are constructed.  This allows them to be passed and returned by
   * value without worrying about whether other code will modify them.
   *
   * @see CalendarAstronomer.Ecliptic
   * @see CalendarAstronomer.Horizon
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  class U_I18N_API Equatorial : public UMemory {
  public:
    /**
     * Constructs an Equatorial coordinate object.
     * <p>
     * @param asc The right ascension, measured in radians.
     * @param dec The declination, measured in radians.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    Equatorial(double asc, double dec)
      : ascension(asc), declination(dec) { }

    /**
     * Return a string representation of this object, with the
     * angles measured in degrees.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    UnicodeString toString() const {
      char tmp[400];
      sprintf(tmp, "%f,%f", 
              (ascension*RAD_DEG), (declination*RAD_DEG));
      return UnicodeString(tmp);
    }
        
    /**
     * Return a string representation of this object with the right ascension
     * measured in hours, minutes, and seconds.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    //String toHmsString() {
    //return radToHms(ascension) + "," + radToDms(declination);
    //}
        
    /**
     * The right ascension, in radians. 
     * This is the position east or west along the equator
     * relative to the sun's position at the vernal equinox,
     * with positive angles representing East.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    double ascension;
        
    /**
     * The declination, in radians.
     * This is the position north or south of the equatorial plane,
     * with positive angles representing north.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    double declination;
  };

  /**
   * Represents the position of an  object in the sky relative to 
   * the local horizon.
   * The <i>Altitude</i> represents the object's elevation above the horizon,
   * with objects below the horizon having a negative altitude.
   * The <i>Azimuth</i> is the geographic direction of the object from the
   * observer's position, with 0 representing north.  The azimuth increases
   * clockwise from north.
   * <p>
   * Note that Horizon objects are immutable and cannot be modified
   * once they are constructed.  This allows them to be passed and returned by
   * value without worrying about whether other code will modify them.
   *
   * @see CalendarAstronomer.Ecliptic
   * @see CalendarAstronomer.Equatorial
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  class U_I18N_API Horizon : public UMemory {
  public:
    /**
     * Constructs a Horizon coordinate object.
     * <p>
     * @param alt  The altitude, measured in radians above the horizon.
     * @param azim The azimuth, measured in radians clockwise from north.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    Horizon(double alt, double azim)
      : altitude(alt), azimuth(azim) { }

    /**
     * Return a string representation of this object, with the
     * angles measured in degrees.
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    UnicodeString toString() {
      char tmp[800];
      sprintf(tmp, "[%.5f,%.5f]", altitude*RAD_DEG, azimuth*RAD_DEG);
      return UnicodeString(tmp);
    }
        
    /** 
     * The object's altitude above the horizon, in radians. 
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    const double altitude;
        
    /** 
     * The object's direction, in radians clockwise from north. 
     * @internal
     * @deprecated ICU 2.4. This class may be removed or modified.
     */
    const double azimuth;
  };

public:
  //-------------------------------------------------------------------------
  // Astronomical constants
  //-------------------------------------------------------------------------
  /**
   * The number of standard hours in one sidereal day.
   * Approximately 24.93.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const double SIDEREAL_DAY;
    
  /**
   * The number of sidereal hours in one mean solar day.
   * Approximately 24.07.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const double SOLAR_DAY;
    
  /**
   * The average number of solar days from one new moon to the next.  This is the time
   * it takes for the moon to return the same ecliptic longitude as the sun.
   * It is longer than the sidereal month because the sun's longitude increases
   * during the year due to the revolution of the earth around the sun.
   * Approximately 29.53.
   *
   * @see #SIDEREAL_MONTH
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const double SYNODIC_MONTH;
    
  /**
   * The average number of days it takes
   * for the moon to return to the same ecliptic longitude relative to the
   * stellar background.  This is referred to as the sidereal month.
   * It is shorter than the synodic month due to
   * the revolution of the earth around the sun.
   * Approximately 27.32.
   *
   * @see #SYNODIC_MONTH
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const double SIDEREAL_MONTH;
    
  /**
   * The average number number of days between successive vernal equinoxes.
   * Due to the precession of the earth's
   * axis, this is not precisely the same as the sidereal year.
   * Approximately 365.24
   *
   * @see #SIDEREAL_YEAR
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const double TROPICAL_YEAR;
    
  /**
   * The average number of days it takes
   * for the sun to return to the same position against the fixed stellar
   * background.  This is the duration of one orbit of the earth about the sun
   * as it would appear to an outside observer.
   * Due to the precession of the earth's
   * axis, this is not precisely the same as the tropical year.
   * Approximately 365.25.
   *
   * @see #TROPICAL_YEAR
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const double SIDEREAL_YEAR;

  //-------------------------------------------------------------------------
  // Time-related constants
  //-------------------------------------------------------------------------

  /** 
   * The number of milliseconds in one second. 
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const int32_t  SECOND_MS;

  /** 
   * The number of milliseconds in one minute. 
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const int32_t  MINUTE_MS;

  /** 
   * The number of milliseconds in one hour. 
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const int32_t  HOUR_MS;

  /** 
   * The number of milliseconds in one day. 
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const double DAY_MS;

  /**
   * The start of the julian day numbering scheme used by astronomers, which
   * is 1/1/4713 BC (Julian), 12:00 GMT.  This is given as the number of milliseconds
   * since 1/1/1970 AD (Gregorian), a negative number.
   * Note that julian day numbers and
   * the Julian calendar are <em>not</em> the same thing.  Also note that
   * julian days start at <em>noon</em>, not midnight.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const double JULIAN_EPOCH_MS;
    
  //  static {
  //      Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
  //      cal.clear();
  //      cal.set(cal.ERA, 0);
  //      cal.set(cal.YEAR, 4713);
  //      cal.set(cal.MONTH, cal.JANUARY);
  //      cal.set(cal.DATE, 1);
  //      cal.set(cal.HOUR_OF_DAY, 12);
  //      System.out.println("1.5 Jan 4713 BC = " + cal.getTime().getTime());

  //      cal.clear();
  //      cal.set(cal.YEAR, 2000);
  //      cal.set(cal.MONTH, cal.JANUARY);
  //      cal.set(cal.DATE, 1);
  //      cal.add(cal.DATE, -1);
  //      System.out.println("0.0 Jan 2000 = " + cal.getTime().getTime());
  //  }
    
  /**
   * Milliseconds value for 0.0 January 2000 AD.
   */
  static const double EPOCH_2000_MS;

  //-------------------------------------------------------------------------
  // Assorted private data used for conversions
  //-------------------------------------------------------------------------

  // My own copies of these so compilers are more likely to optimize them away
  static const double PI;
  static const double PI2;

  static const double RAD_HOUR;
  static const double DEG_RAD;
  static const double RAD_DEG;
    
  //-------------------------------------------------------------------------
  // Constructors
  //-------------------------------------------------------------------------

  /**
   * Construct a new <code>CalendarAstronomer</code> object that is initialized to
   * the current date and time.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  CalendarAstronomer();
    
  /**
   * Construct a new <code>CalendarAstronomer</code> object that is initialized to
   * the specified date and time.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  CalendarAstronomer(UDate d);
    
    
  /**
   * Construct a new <code>CalendarAstronomer</code> object with the given
   * latitude and longitude.  The object's time is set to the current
   * date and time.
   * <p>
   * @param longitude The desired longitude, in <em>degrees</em> east of
   *                  the Greenwich meridian.
   *
   * @param latitude  The desired latitude, in <em>degrees</em>.  Positive
   *                  values signify North, negative South.
   *
   * @see java.util.Date#getTime()
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  CalendarAstronomer(double longitude, double latitude);

  /**
   * Destructor
   * @internal
   */
  ~CalendarAstronomer();
    
  //-------------------------------------------------------------------------
  // Time and date getters and setters
  //-------------------------------------------------------------------------
    
  /**
   * Set the current date and time of this <code>CalendarAstronomer</code> object.  All
   * astronomical calculations are performed based on this time setting.
   *
   * @param aTime the date and time, expressed as the number of milliseconds since
   *              1/1/1970 0:00 GMT (Gregorian).
   *
   * @see #setDate
   * @see #getTime
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  void setTime(UDate aTime);
  

  /**
   * Set the current date and time of this <code>CalendarAstronomer</code> object.  All
   * astronomical calculations are performed based on this time setting.
   *
   * @param aTime the date and time, expressed as the number of milliseconds since
   *              1/1/1970 0:00 GMT (Gregorian).
   *
   * @see #getTime
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  void setDate(UDate aDate) { setTime(aDate); }
    
  /**
   * Set the current date and time of this <code>CalendarAstronomer</code> object.  All
   * astronomical calculations are performed based on this time setting.
   *
   * @param jdn   the desired time, expressed as a "julian day number",
   *              which is the number of elapsed days since 
   *              1/1/4713 BC (Julian), 12:00 GMT.  Note that julian day
   *              numbers start at <em>noon</em>.  To get the jdn for
   *              the corresponding midnight, subtract 0.5.
   *
   * @see #getJulianDay
   * @see #JULIAN_EPOCH_MS
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  void setJulianDay(double jdn);
    
  /**
   * Get the current time of this <code>CalendarAstronomer</code> object,
   * represented as the number of milliseconds since
   * 1/1/1970 AD 0:00 GMT (Gregorian).
   *
   * @see #setTime
   * @see #getDate
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  UDate getTime();
    
  /**
   * Get the current time of this <code>CalendarAstronomer</code> object,
   * expressed as a "julian day number", which is the number of elapsed
   * days since 1/1/4713 BC (Julian), 12:00 GMT.
   *
   * @see #setJulianDay
   * @see #JULIAN_EPOCH_MS
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  double getJulianDay();

  /**
   * Return this object's time expressed in julian centuries:
   * the number of centuries after 1/1/1900 AD, 12:00 GMT
   *
   * @see #getJulianDay
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  double getJulianCentury();

  /**
   * Returns the current Greenwich sidereal time, measured in hours
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  double getGreenwichSidereal();

private:    
  double getSiderealOffset();
public:    
  /**
   * Returns the current local sidereal time, measured in hours
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  double getLocalSidereal();
    
  /**
   * Converts local sidereal time to Universal Time.
   *
   * @param lst   The Local Sidereal Time, in hours since sidereal midnight
   *              on this object's current date.
   *
   * @return      The corresponding Universal Time, in milliseconds since
   *              1 Jan 1970, GMT.  
   */
  //private:
  double lstToUT(double lst);
    
  Equatorial* eclipticToEquatorial(Ecliptic& ecliptic);

  /**
   * Convert from ecliptic to equatorial coordinates.
   *
   * @param eclipLong     The ecliptic longitude
   * @param eclipLat      The ecliptic latitude
   *
   * @return              The corresponding point in equatorial coordinates.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  Equatorial* eclipticToEquatorial(double eclipLong, double eclipLat);

  /**
   * Convert from ecliptic longitude to equatorial coordinates.
   *
   * @param eclipLong     The ecliptic longitude
   *
   * @return              The corresponding point in equatorial coordinates.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  Equatorial* eclipticToEquatorial(double eclipLong);

  /**
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  Horizon* eclipticToHorizon(double eclipLong);

  //-------------------------------------------------------------------------
  // The Sun
  //-------------------------------------------------------------------------

  //
  // Parameters of the Sun's orbit as of the epoch Jan 0.0 1990
  // Angles are in radians (after multiplying by PI/180)
  //
  static const double JD_EPOCH;

  static const double SUN_ETA_G;
  static const double SUN_OMEGA_G;
  static const double SUN_E ;
  //double sunR0     =   1.495585e8;        // Semi-major axis in KM
  //double sunTheta0 =   0.533128 * PI/180; // Angular diameter at R0

  // The following three methods, which compute the sun parameters
  // given above for an arbitrary epoch (whatever time the object is
  // set to), make only a small difference as compared to using the
  // above constants.  E.g., Sunset times might differ by ~12
  // seconds.  Furthermore, the eta-g computation is befuddled by
  // Duffet-Smith's incorrect coefficients (p.86).  I've corrected
  // the first-order coefficient but the others may be off too - no
  // way of knowing without consulting another source.

  //  /**
  //   * Return the sun's ecliptic longitude at perigee for the current time.
  //   * See Duffett-Smith, p. 86.
  //   * @return radians
  //   */
  //  private double getSunOmegaG() {
  //      double T = getJulianCentury();
  //      return (281.2208444 + (1.719175 + 0.000452778*T)*T) * DEG_RAD;
  //  }

  //  /**
  //   * Return the sun's ecliptic longitude for the current time.
  //   * See Duffett-Smith, p. 86.
  //   * @return radians
  //   */
  //  private double getSunEtaG() {
  //      double T = getJulianCentury();
  //      //return (279.6966778 + (36000.76892 + 0.0003025*T)*T) * DEG_RAD;
  //      //
  //      // The above line is from Duffett-Smith, and yields manifestly wrong
  //      // results.  The below constant is derived empirically to match the
  //      // constant he gives for the 1990 EPOCH.
  //      //
  //      return (279.6966778 + (-0.3262541582718024 + 0.0003025*T)*T) * DEG_RAD;
  //  }

  //  /**
  //   * Return the sun's eccentricity of orbit for the current time.
  //   * See Duffett-Smith, p. 86.
  //   * @return double
  //   */
  //  private double getSunE() {
  //      double T = getJulianCentury();
  //      return 0.01675104 - (0.0000418 + 0.000000126*T)*T;
  //  }

  /**
   * The longitude of the sun at the time specified by this object.
   * The longitude is measured in radians along the ecliptic
   * from the "first point of Aries," the point at which the ecliptic
   * crosses the earth's equatorial plane at the vernal equinox.
   * <p>
   * Currently, this method uses an approximation of the two-body Kepler's
   * equation for the earth and the sun.  It does not take into account the
   * perturbations caused by the other planets, the moon, etc.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  double getSunLongitude();
  
  /**
   * TODO Make this public when the entire class is package-private.
   */
  /*public*/ void getSunLongitude(double julianDay, double &longitude, double &meanAnomaly);

  /**
   * The position of the sun at this object's current date and time,
   * in equatorial coordinates.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  Equatorial* getSunPosition();
    
public:
  class U_I18N_API SolarLongitude : public UMemory {
  public: 
    SolarLongitude(double l)
      :  value(l) { }
    double value;
  };
    
public:
  /**
   * Constant representing the vernal equinox.
   * For use with {@link #getSunTime getSunTime}. 
   * Note: In this case, "vernal" refers to the northern hemisphere's seasons.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const SolarLongitude VERNAL_EQUINOX;
    
  /**
   * Constant representing the summer solstice.
   * For use with {@link #getSunTime getSunTime}.
   * Note: In this case, "summer" refers to the northern hemisphere's seasons.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const SolarLongitude SUMMER_SOLSTICE;
    
  /**
   * Constant representing the autumnal equinox.
   * For use with {@link #getSunTime getSunTime}.
   * Note: In this case, "autumn" refers to the northern hemisphere's seasons.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const SolarLongitude AUTUMN_EQUINOX;
    
  /**
   * Constant representing the winter solstice.
   * For use with {@link #getSunTime getSunTime}.
   * Note: In this case, "winter" refers to the northern hemisphere's seasons.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const SolarLongitude WINTER_SOLSTICE;
    
  /**
   * Find the next time at which the sun's ecliptic longitude will have
   * the desired value.  
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  UDate getSunTime(UDate desired, UBool next);
  /**
   * Find the next time at which the sun's ecliptic longitude will have
   * the desired value.  
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  UDate getSunTime(const SolarLongitude& desired, UBool next);
    
  /**
   * Returns the time (GMT) of sunrise or sunset on the local date to which
   * this calendar is currently set.
   *
   * NOTE: This method only works well if this object is set to a
   * time near local noon.  Because of variations between the local
   * official time zone and the geographic longitude, the
   * computation can flop over into an adjacent day if this object
   * is set to a time near local midnight.
   * 
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  UDate getSunRiseSet(UBool rise);

  // Commented out - currently unused. ICU 2.6, Alan
  //    //-------------------------------------------------------------------------
  //    // Alternate Sun Rise/Set
  //    // See Duffett-Smith p.93
  //    //-------------------------------------------------------------------------
  //
  //    // This yields worse results (as compared to USNO data) than getSunRiseSet().
  //    /**
  //     * TODO Make this public when the entire class is package-private.
  //     */
  //    /*public*/ long getSunRiseSet2(boolean rise) {
  //        // 1. Calculate coordinates of the sun's center for midnight
  //        double jd = Math.floor(getJulianDay() - 0.5) + 0.5;
  //        double[] sl = getSunLongitude(jd);
  //        double lambda1 = sl[0];
  //        Equatorial pos1 = eclipticToEquatorial(lambda1, 0);
  //
  //        // 2. Add ... to lambda to get position 24 hours later
  //        double lambda2 = lambda1 + 0.985647*DEG_RAD;
  //        Equatorial pos2 = eclipticToEquatorial(lambda2, 0);
  //
  //        // 3. Calculate LSTs of rising and setting for these two positions
  //        double tanL = Math.tan(fLatitude);
  //        double H = Math.acos(-tanL * Math.tan(pos1.declination));
  //        double lst1r = (PI2 + pos1.ascension - H) * 24 / PI2;
  //        double lst1s = (pos1.ascension + H) * 24 / PI2;
  //               H = Math.acos(-tanL * Math.tan(pos2.declination));
  //        double lst2r = (PI2-H + pos2.ascension ) * 24 / PI2;
  //        double lst2s = (H + pos2.ascension ) * 24 / PI2;
  //        if (lst1r > 24) lst1r -= 24;
  //        if (lst1s > 24) lst1s -= 24;
  //        if (lst2r > 24) lst2r -= 24;
  //        if (lst2s > 24) lst2s -= 24;
  //        
  //        // 4. Convert LSTs to GSTs.  If GST1 > GST2, add 24 to GST2.
  //        double gst1r = lstToGst(lst1r);
  //        double gst1s = lstToGst(lst1s);
  //        double gst2r = lstToGst(lst2r);
  //        double gst2s = lstToGst(lst2s);
  //        if (gst1r > gst2r) gst2r += 24;
  //        if (gst1s > gst2s) gst2s += 24;
  //
  //        // 5. Calculate GST at 0h UT of this date
  //        double t00 = utToGst(0);
  //        
  //        // 6. Calculate GST at 0h on the observer's longitude
  //        double offset = Math.round(fLongitude*12/PI); // p.95 step 6; he _rounds_ to nearest 15 deg.
  //        double t00p = t00 - offset*1.002737909;
  //        if (t00p < 0) t00p += 24; // do NOT normalize
  //        
  //        // 7. Adjust
  //        if (gst1r < t00p) {
  //            gst1r += 24;
  //            gst2r += 24;
  //        }
  //        if (gst1s < t00p) {
  //            gst1s += 24;
  //            gst2s += 24;
  //        }
  //
  //        // 8.
  //        double gstr = (24.07*gst1r-t00*(gst2r-gst1r))/(24.07+gst1r-gst2r);
  //        double gsts = (24.07*gst1s-t00*(gst2s-gst1s))/(24.07+gst1s-gst2s);
  //
  //        // 9. Correct for parallax, refraction, and sun's diameter
  //        double dec = (pos1.declination + pos2.declination) / 2;
  //        double psi = Math.acos(Math.sin(fLatitude) / Math.cos(dec));
  //        double x = 0.830725 * DEG_RAD; // parallax+refraction+diameter
  //        double y = Math.asin(Math.sin(x) / Math.sin(psi)) * RAD_DEG;
  //        double delta_t = 240 * y / Math.cos(dec) / 3600; // hours
  //
  //        // 10. Add correction to GSTs, subtract from GSTr
  //        gstr -= delta_t;
  //        gsts += delta_t;
  //
  //        // 11. Convert GST to UT and then to local civil time
  //        double ut = gstToUt(rise ? gstr : gsts);
  //        //System.out.println((rise?"rise=":"set=") + ut + ", delta_t=" + delta_t);
  //        long midnight = DAY_MS * (time / DAY_MS); // Find UT midnight on this day
  //        return midnight + (long) (ut * 3600000);
  //    }

  // Commented out - currently unused. ICU 2.6, Alan
  //    /**
  //     * Convert local sidereal time to Greenwich sidereal time.
  //     * Section 15.  Duffett-Smith p.21
  //     * @param lst in hours (0..24)
  //     * @return GST in hours (0..24)
  //     */
  //    double lstToGst(double lst) {
  //        double delta = fLongitude * 24 / PI2;
  //        return normalize(lst - delta, 24);
  //    }
 
  // Commented out - currently unused. ICU 2.6, Alan
  //    /**
  //     * Convert UT to GST on this date.
  //     * Section 12.  Duffett-Smith p.17
  //     * @param ut in hours
  //     * @return GST in hours
  //     */
  //    double utToGst(double ut) {
  //        return normalize(getT0() + ut*1.002737909, 24);
  //    }

  // Commented out - currently unused. ICU 2.6, Alan
  //    /**
  //     * Convert GST to UT on this date.
  //     * Section 13.  Duffett-Smith p.18
  //     * @param gst in hours
  //     * @return UT in hours
  //     */
  //    double gstToUt(double gst) {
  //        return normalize(gst - getT0(), 24) * 0.9972695663;
  //    }

  // Commented out - currently unused. ICU 2.6, Alan
  //    double getT0() {
  //        // Common computation for UT <=> GST
  //
  //        // Find JD for 0h UT
  //        double jd = Math.floor(getJulianDay() - 0.5) + 0.5;
  //
  //        double s = jd - 2451545.0;
  //        double t = s / 36525.0;
  //        double t0 = 6.697374558 + (2400.051336 + 0.000025862*t)*t;
  //        return t0;
  //    }

  // Commented out - currently unused. ICU 2.6, Alan
  //    //-------------------------------------------------------------------------
  //    // Alternate Sun Rise/Set
  //    // See sci.astro FAQ
  //    // http://www.faqs.org/faqs/astronomy/faq/part3/section-5.html
  //    //-------------------------------------------------------------------------
  //
  //    // Note: This method appears to produce inferior accuracy as
  //    // compared to getSunRiseSet(). 
  //
  //    /**
  //     * TODO Make this public when the entire class is package-private.
  //     */
  //    /*public*/ long getSunRiseSet3(boolean rise) {
  //
  //        // Compute day number for 0.0 Jan 2000 epoch
  //        double d = (double)(time - EPOCH_2000_MS) / DAY_MS;
  //
  //        // Now compute the Local Sidereal Time, LST:
  //        // 
  //        double LST  =  98.9818  +  0.985647352 * d  +  /*UT*15  +  long*/
  //            fLongitude*RAD_DEG;
  //        // 
  //        // (east long. positive).  Note that LST is here expressed in degrees,
  //        // where 15 degrees corresponds to one hour.  Since LST really is an angle,
  //        // it's convenient to use one unit---degrees---throughout.
  //
  //        // 	COMPUTING THE SUN'S POSITION
  //        // 	----------------------------
  //        // 
  //        // To be able to compute the Sun's rise/set times, you need to be able to
  //        // compute the Sun's position at any time.  First compute the "day
  //        // number" d as outlined above, for the desired moment.  Next compute:
  //        // 
  //        double oblecl = 23.4393 - 3.563E-7 * d;
  //        // 
  //        double w  =  282.9404  +  4.70935E-5   * d;
  //        double M  =  356.0470  +  0.9856002585 * d;
  //        double e  =  0.016709  -  1.151E-9     * d;
  //        // 
  //        // This is the obliquity of the ecliptic, plus some of the elements of
  //        // the Sun's apparent orbit (i.e., really the Earth's orbit): w =
  //        // argument of perihelion, M = mean anomaly, e = eccentricity.
  //        // Semi-major axis is here assumed to be exactly 1.0 (while not strictly
  //        // true, this is still an accurate approximation).  Next compute E, the
  //        // eccentric anomaly:
  //        // 
  //        double E = M + e*(180/PI) * Math.sin(M*DEG_RAD) * ( 1.0 + e*Math.cos(M*DEG_RAD) );
  //        // 
  //        // where E and M are in degrees.  This is it---no further iterations are
  //        // needed because we know e has a sufficiently small value.  Next compute
  //        // the true anomaly, v, and the distance, r:
  //        // 
  //        /*      r * cos(v)  =  */ double A  =  Math.cos(E*DEG_RAD) - e;
  //        /*      r * sin(v)  =  */ double B  =  Math.sqrt(1 - e*e) * Math.sin(E*DEG_RAD);
  //        // 
  //        // and
  //        // 
  //        //      r  =  sqrt( A*A + B*B )
  //        double v  =  Math.atan2( B, A )*RAD_DEG;
  //        // 
  //        // The Sun's true longitude, slon, can now be computed:
  //        // 
  //        double slon  =  v + w;
  //        // 
  //        // Since the Sun is always at the ecliptic (or at least very very close to
  //        // it), we can use simplified formulae to convert slon (the Sun's ecliptic
  //        // longitude) to sRA and sDec (the Sun's RA and Dec):
  //        // 
  //        //                   sin(slon) * cos(oblecl)
  //        //     tan(sRA)  =  -------------------------
  //        // 			cos(slon)
  //        // 
  //        //     sin(sDec) =  sin(oblecl) * sin(slon)
  //        // 
  //        // As was the case when computing az, the Azimuth, if possible use an
  //        // atan2() function to compute sRA.
  //
  //        double sRA = Math.atan2(Math.sin(slon*DEG_RAD) * Math.cos(oblecl*DEG_RAD), Math.cos(slon*DEG_RAD))*RAD_DEG;
  //
  //        double sin_sDec = Math.sin(oblecl*DEG_RAD) * Math.sin(slon*DEG_RAD);
  //        double sDec = Math.asin(sin_sDec)*RAD_DEG;
  //
  //        // 	COMPUTING RISE AND SET TIMES
  //        // 	----------------------------
  //        // 
  //        // To compute when an object rises or sets, you must compute when it
  //        // passes the meridian and the HA of rise/set.  Then the rise time is
  //        // the meridian time minus HA for rise/set, and the set time is the
  //        // meridian time plus the HA for rise/set.
  //        // 
  //        // To find the meridian time, compute the Local Sidereal Time at 0h local
  //        // time (or 0h UT if you prefer to work in UT) as outlined above---name
  //        // that quantity LST0.  The Meridian Time, MT, will now be:
  //        // 
  //        //     MT  =  RA - LST0
  //        double MT = normalize(sRA - LST, 360);
  //        // 
  //        // where "RA" is the object's Right Ascension (in degrees!).  If negative,
  //        // add 360 deg to MT.  If the object is the Sun, leave the time as it is,
  //        // but if it's stellar, multiply MT by 365.2422/366.2422, to convert from
  //        // sidereal to solar time.  Now, compute HA for rise/set, name that
  //        // quantity HA0:
  //        // 
  //        //                 sin(h0)  -  sin(lat) * sin(Dec)
  //        // cos(HA0)  =  ---------------------------------
  //        //                      cos(lat) * cos(Dec)
  //        // 
  //        // where h0 is the altitude selected to represent rise/set.  For a purely
  //        // mathematical horizon, set h0 = 0 and simplify to:
  //        // 
  //        // 	cos(HA0)  =  - tan(lat) * tan(Dec)
  //        // 
  //        // If you want to account for refraction on the atmosphere, set h0 = -35/60
  //        // degrees (-35 arc minutes), and if you want to compute the rise/set times
  //        // for the Sun's upper limb, set h0 = -50/60 (-50 arc minutes).
  //        // 
  //        double h0 = -50/60 * DEG_RAD;
  //
  //        double HA0 = Math.acos(
  //          (Math.sin(h0) - Math.sin(fLatitude) * sin_sDec) /
  //          (Math.cos(fLatitude) * Math.cos(sDec*DEG_RAD)))*RAD_DEG;
  //
  //        // When HA0 has been computed, leave it as it is for the Sun but multiply
  //        // by 365.2422/366.2422 for stellar objects, to convert from sidereal to
  //        // solar time.  Finally compute:
  //        // 
  //        //    Rise time  =  MT - HA0
  //        //    Set  time  =  MT + HA0
  //        // 
  //        // convert the times from degrees to hours by dividing by 15.
  //        // 
  //        // If you'd like to check that your calculations are accurate or just
  //        // need a quick result, check the USNO's Sun or Moon Rise/Set Table,
  //        // <URL:http://aa.usno.navy.mil/AA/data/docs/RS_OneYear.html>.
  //
  //        double result = MT + (rise ? -HA0 : HA0); // in degrees
  //
  //        // Find UT midnight on this day
  //        long midnight = DAY_MS * (time / DAY_MS);
  //
  //        return midnight + (long) (result * 3600000 / 15);
  //    }

  //-------------------------------------------------------------------------
  // The Moon
  //-------------------------------------------------------------------------
    
  static const double moonL0;   // Mean long. at epoch
  static const double moonP0;   // Mean long. of perigee
  static const double moonN0;   // Mean long. of node
  static const double moonI;   // Inclination of orbit
  static const double moonE;            // Eccentricity of orbit
    
  // These aren't used right now
  static const double moonA;           // semi-major axis (km)
  static const double moonT0;     // Angular size at distance A
  static const double moonPi;     // Parallax at distance A
    
  /**
   * The position of the moon at the time set on this
   * object, in equatorial coordinates.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  Equatorial* getMoonPosition();
    
  /**
   * The "age" of the moon at the time specified in this object.
   * This is really the angle between the
   * current ecliptic longitudes of the sun and the moon,
   * measured in radians.
   *
   * @see #getMoonPhase
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  double getMoonAge();
    
  /**
   * Calculate the phase of the moon at the time set in this object.
   * The returned phase is a <code>double</code> in the range
   * <code>0 <= phase < 1</code>, interpreted as follows:
   * <ul>
   * <li>0.00: New moon
   * <li>0.25: First quarter
   * <li>0.50: Full moon
   * <li>0.75: Last quarter
   * </ul>
   *
   * @see #getMoonAge
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  double getMoonPhase();
    
  class U_I18N_API MoonAge : public UMemory {
  public: 
    MoonAge(double l)
      :  value(l) { }
    double value;
  };

  /**
   * Constant representing a new moon.
   * For use with {@link #getMoonTime getMoonTime}
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const MoonAge NEW_MOON;

  /**
   * Constant representing the moon's first quarter.
   * For use with {@link #getMoonTime getMoonTime}
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const MoonAge FIRST_QUARTER;
    
  /**
   * Constant representing a full moon.
   * For use with {@link #getMoonTime getMoonTime}
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const MoonAge FULL_MOON;
    
  /**
   * Constant representing the moon's last quarter.
   * For use with {@link #getMoonTime getMoonTime}
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  static const MoonAge LAST_QUARTER;
    
  /**
   * Find the next or previous time at which the Moon's ecliptic
   * longitude will have the desired value.  
   * <p>
   * @param desired   The desired longitude.
   * @param next      <tt>true</tt> if the next occurrance of the phase
   *                  is desired, <tt>false</tt> for the previous occurrance. 
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  UDate getMoonTime(double desired, UBool next);
  UDate getMoonTime(MoonAge desired, UBool next);
    
  /**
   * Returns the time (GMT) of sunrise or sunset on the local date to which
   * this calendar is currently set.
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  UDate getMoonRiseSet(UBool rise);

  //-------------------------------------------------------------------------
  // Interpolation methods for finding the time at which a given event occurs
  //-------------------------------------------------------------------------

  // private
  class U_I18N_API AngleFunc : public UMemory {
  public:
    virtual double eval(CalendarAstronomer&) = 0;
  };
  friend class AngleFunc;
    
  UDate timeOfAngle(AngleFunc& func, double desired,
                    double periodDays, double epsilon, UBool next);
    
  class U_I18N_API CoordFunc : public UMemory {
  public:
    virtual Equatorial* eval(CalendarAstronomer&) = 0;
  };
  friend class CoordFunc;
    
  double riseOrSet(CoordFunc& func, UBool rise,
                   double diameter, double refraction, 
                   double epsilon);

  //-------------------------------------------------------------------------
  // Other utility methods
  //-------------------------------------------------------------------------
private:
  /***
   * Given 'value', add or subtract 'range' until 0 <= 'value' < range.
   * The modulus operator.
   */
  inline static double normalize(double value, double range)  {
    return value - range * Math::floorDivide(value, range);
  }
    
  /**
   * Normalize an angle so that it's in the range 0 - 2pi.
   * For positive angles this is just (angle % 2pi), but the Java
   * mod operator doesn't work that way for negative numbers....
   */
  inline static double norm2PI(double angle)  {
    return normalize(angle, PI2);
  }
    
  /**
   * Normalize an angle into the range -PI - PI
   */
  inline static  double normPI(double angle)  {
    return normalize(angle + PI, PI2) - PI;
  }
    
  /**
   * Find the "true anomaly" (longitude) of an object from
   * its mean anomaly and the eccentricity of its orbit.  This uses
   * an iterative solution to Kepler's equation.
   *
   * @param meanAnomaly   The object's longitude calculated as if it were in
   *                      a regular, circular orbit, measured in radians
   *                      from the point of perigee.  
   *
   * @param eccentricity  The eccentricity of the orbit
   *
   * @return The true anomaly (longitude) measured in radians
   */
  double trueAnomaly(double meanAnomaly, double eccentricity);
    
  /**
   * Return the obliquity of the ecliptic (the angle between the ecliptic
   * and the earth's equator) at the current time.  This varies due to
   * the precession of the earth's axis.
   *
   * @return  the obliquity of the ecliptic relative to the equator,
   *          measured in radians.
   */
  double eclipticObliquity();
     
  //-------------------------------------------------------------------------
  // Private data
  //-------------------------------------------------------------------------
private:    
  /**
   * Current time in milliseconds since 1/1/1970 AD
   * @see java.util.Date#getTime
   */
  UDate fTime;
    
  /* These aren't used yet, but they'll be needed for sunset calculations
   * and equatorial to horizon coordinate conversions
   */
  double fLongitude;
  double fLatitude;
  double fGmtOffset;
    
  //
  // The following fields are used to cache calculated results for improved
  // performance.  These values all depend on the current time setting
  // of this object, so the clearCache method is provided.
  //

  double    julianDay       ;
  double    julianCentury   ;
  double    sunLongitude    ;
  double    meanAnomalySun  ;
  double    moonLongitude   ;
  double    moonEclipLong   ;
  double    meanAnomalyMoon ;
  double    eclipObliquity  ;
  double    siderealT0      ;
  double    siderealTime    ;
   
  void clearCache();
  
  Equatorial  *moonPosition;

  //private static void out(String s) {
  //    System.out.println(s);
  //}
    
  //private static String deg(double rad) {
  //    return Double.toString(rad * RAD_DEG);
  //}
    
  //private static String hours(long ms) {
  //    return Double.toString((double)ms / HOUR_MS) + " hours";
  //}

  /**
   * @internal
   * @deprecated ICU 2.4. This class may be removed or modified.
   */
  UDate local(UDate localMillis);
};
#endif
#endif
