// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.impl;

import java.util.Date;

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
 * At the moment, it basically does just enough to support {@link com.ibm.icu.util.IslamicCalendar}
 * and {@link com.ibm.icu.util.ChineseCalendar}.
 *
 * @author Laura Werner
 * @author Alan Liu
 * @internal
 */
public class CalendarAstronomer {

    //-------------------------------------------------------------------------
    // Astronomical constants
    //-------------------------------------------------------------------------

    /**
     * The number of standard hours in one sidereal day.
     * Approximately 24.93.
     * @internal
     */
    public static final double SIDEREAL_DAY = 23.93446960027;

    /**
     * The number of sidereal hours in one mean solar day.
     * Approximately 24.07.
     * @internal
     */
    public static final double SOLAR_DAY =  24.065709816;

    /**
     * The average number of solar days from one new moon to the next.  This is the time
     * it takes for the moon to return the same ecliptic longitude as the sun.
     * It is longer than the sidereal month because the sun's longitude increases
     * during the year due to the revolution of the earth around the sun.
     * Approximately 29.53.
     *
     * @see #SIDEREAL_MONTH
     * @internal
     */
    public static final double SYNODIC_MONTH = 29.530588853;

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
     */
    public static final double SIDEREAL_MONTH = 27.32166;

    /**
     * The average number number of days between successive vernal equinoxes.
     * Due to the precession of the earth's
     * axis, this is not precisely the same as the sidereal year.
     * Approximately 365.24
     *
     * @see #SIDEREAL_YEAR
     * @internal
     */
    public static final double TROPICAL_YEAR = 365.242191;

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
     */
    public static final double SIDEREAL_YEAR = 365.25636;

    //-------------------------------------------------------------------------
    // Time-related constants
    //-------------------------------------------------------------------------

    /**
     * The number of milliseconds in one second.
     * @internal
     */
    public static final int  SECOND_MS = 1000;

    /**
     * The number of milliseconds in one minute.
     * @internal
     */
    public static final int  MINUTE_MS = 60*SECOND_MS;

    /**
     * The number of milliseconds in one hour.
     * @internal
     */
    public static final int  HOUR_MS   = 60*MINUTE_MS;

    /**
     * The number of milliseconds in one day.
     * @internal
     */
    public static final long DAY_MS    = 24*HOUR_MS;

    /**
     * The start of the julian day numbering scheme used by astronomers, which
     * is 1/1/4713 BC (Julian), 12:00 GMT.  This is given as the number of milliseconds
     * since 1/1/1970 AD (Gregorian), a negative number.
     * Note that julian day numbers and
     * the Julian calendar are <em>not</em> the same thing.  Also note that
     * julian days start at <em>noon</em>, not midnight.
     * @internal
     */
    public static final long JULIAN_EPOCH_MS = -210866760000000L;

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
    static final long EPOCH_2000_MS = 946598400000L;

    //-------------------------------------------------------------------------
    // Assorted private data used for conversions
    //-------------------------------------------------------------------------

    // My own copies of these so compilers are more likely to optimize them away
    static private final double PI = 3.14159265358979323846;
    static private final double PI2 = PI * 2.0;

    static private final double RAD_HOUR = 12 / PI;        // radians -> hours
    static private final double DEG_RAD  = PI / 180;        // degrees -> radians
    static private final double RAD_DEG  = 180 / PI;        // radians -> degrees

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Construct a new <code>CalendarAstronomer</code> object that is initialized to
     * the current date and time.
     * @internal
     */
    public CalendarAstronomer() {
        this(System.currentTimeMillis());
    }

    /**
     * Construct a new <code>CalendarAstronomer</code> object that is initialized to
     * the specified time.  The time is expressed as a number of milliseconds since
     * January 1, 1970 AD (Gregorian).
     *
     * @see java.util.Date#getTime()
     * @internal
     */
    public CalendarAstronomer(long aTime) {
        time = aTime;
    }

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
     */
    public void setTime(long aTime) {
        time = aTime;
        clearCache();
    }


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
     */
    public void setJulianDay(double jdn) {
        time = (long)(jdn * DAY_MS) + JULIAN_EPOCH_MS;
        clearCache();
        julianDay = jdn;
    }

    /**
     * Get the current time of this <code>CalendarAstronomer</code> object,
     * represented as the number of milliseconds since
     * 1/1/1970 AD 0:00 GMT (Gregorian).
     *
     * @see #setTime
     * @see #getDate
     * @internal
     */
    public long getTime() {
        return time;
    }

    /**
     * Get the current time of this <code>CalendarAstronomer</code> object,
     * represented as a <code>Date</code> object.
     *
     * @see #setDate
     * @see #getTime
     * @internal
     */
    public Date getDate() {
        return new Date(time);
    }

    /**
     * Get the current time of this <code>CalendarAstronomer</code> object,
     * expressed as a "julian day number", which is the number of elapsed
     * days since 1/1/4713 BC (Julian), 12:00 GMT.
     *
     * @see #setJulianDay
     * @see #JULIAN_EPOCH_MS
     * @internal
     */
    public double getJulianDay() {
        if (julianDay == INVALID) {
            julianDay = (double)(time - JULIAN_EPOCH_MS) / (double)DAY_MS;
        }
        return julianDay;
    }

    //-------------------------------------------------------------------------
    // Coordinate transformations, all based on the current time of this object
    //-------------------------------------------------------------------------

    /**
     * Convert from ecliptic to equatorial coordinates.
     *
     * @param eclipLong     The ecliptic longitude
     * @param eclipLat      The ecliptic latitude
     *
     * @return              The corresponding point in equatorial coordinates.
     * @internal
     */
    public final Equatorial eclipticToEquatorial(double eclipLong, double eclipLat)
    {
        // See page 42 of "Practical Astronomy with your Calculator",
        // by Peter Duffet-Smith, for details on the algorithm.

        double obliq = eclipticObliquity();
        double sinE = Math.sin(obliq);
        double cosE = Math.cos(obliq);

        double sinL = Math.sin(eclipLong);
        double cosL = Math.cos(eclipLong);

        double sinB = Math.sin(eclipLat);
        double cosB = Math.cos(eclipLat);
        double tanB = Math.tan(eclipLat);

        return new Equatorial(Math.atan2(sinL*cosE - tanB*sinE, cosL),
                               Math.asin(sinB*cosE + cosB*sinE*sinL) );
    }

    //-------------------------------------------------------------------------
    // The Sun
    //-------------------------------------------------------------------------

    //
    // Parameters of the Sun's orbit as of the epoch Jan 0.0 1990
    // Angles are in radians (after multiplying by PI/180)
    //
    static final double JD_EPOCH = 2447891.5; // Julian day of epoch

    static final double SUN_ETA_G   = 279.403303 * PI/180; // Ecliptic longitude at epoch
    static final double SUN_OMEGA_G = 282.768422 * PI/180; // Ecliptic longitude of perigee
    static final double SUN_E      =   0.016713;          // Eccentricity of orbit
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
     */
    public double getSunLongitude()
    {
        // See page 86 of "Practical Astronomy with your Calculator",
        // by Peter Duffet-Smith, for details on the algorithm.

        if (sunLongitude == INVALID) {
            double[] result = getSunLongitude(getJulianDay());
            sunLongitude = result[0];
            meanAnomalySun = result[1];
        }
        return sunLongitude;
    }

    /**
     * TODO Make this public when the entire class is package-private.
     */
    /*public*/ double[] getSunLongitude(double julian)
    {
        // See page 86 of "Practical Astronomy with your Calculator",
        // by Peter Duffet-Smith, for details on the algorithm.

        double day = julian - JD_EPOCH;       // Days since epoch

        // Find the angular distance the sun in a fictitious
        // circular orbit has travelled since the epoch.
        double epochAngle = norm2PI(PI2/TROPICAL_YEAR*day);

        // The epoch wasn't at the sun's perigee; find the angular distance
        // since perigee, which is called the "mean anomaly"
        double meanAnomaly = norm2PI(epochAngle + SUN_ETA_G - SUN_OMEGA_G);

        // Now find the "true anomaly", e.g. the real solar longitude
        // by solving Kepler's equation for an elliptical orbit
        // NOTE: The 3rd ed. of the book lists omega_g and eta_g in different
        // equations; omega_g is to be correct.
        return new double[] {
            norm2PI(trueAnomaly(meanAnomaly, SUN_E) + SUN_OMEGA_G),
            meanAnomaly
        };
    }

    private static class SolarLongitude {
        double value;
        SolarLongitude(double val) { value = val; }
    }

    /**
     * Constant representing the winter solstice.
     * For use with {@link #getSunTime(SolarLongitude, boolean) getSunTime}.
     * Note: In this case, "winter" refers to the northern hemisphere's seasons.
     * @internal
     */
    public static final SolarLongitude WINTER_SOLSTICE = new SolarLongitude((PI*3)/2);

    /**
     * Find the next time at which the sun's ecliptic longitude will have
     * the desired value.
     * @internal
     */
    public long getSunTime(double desired, boolean next)
    {
        return timeOfAngle( new AngleFunc() { @Override
        public double eval() { return getSunLongitude(); } },
                            desired,
                            TROPICAL_YEAR,
                            MINUTE_MS,
                            next);
    }

    /**
     * Find the next time at which the sun's ecliptic longitude will have
     * the desired value.
     * @internal
     */
    public long getSunTime(SolarLongitude desired, boolean next) {
        return getSunTime(desired.value, next);
    }

    //-------------------------------------------------------------------------
    // The Moon
    //-------------------------------------------------------------------------

    static final double moonL0 = 318.351648 * PI/180;   // Mean long. at epoch
    static final double moonP0 =  36.340410 * PI/180;   // Mean long. of perigee
    static final double moonN0 = 318.510107 * PI/180;   // Mean long. of node
    static final double moonI  =   5.145366 * PI/180;   // Inclination of orbit
    static final double moonE  =   0.054900;            // Eccentricity of orbit

    // These aren't used right now
    static final double moonA  =   3.84401e5;           // semi-major axis (km)
    static final double moonT0 =   0.5181 * PI/180;     // Angular size at distance A
    static final double moonPi =   0.9507 * PI/180;     // Parallax at distance A

    /**
     * The position of the moon at the time set on this
     * object, in equatorial coordinates.
     * @internal
     */
    public Equatorial getMoonPosition()
    {
        //
        // See page 142 of "Practical Astronomy with your Calculator",
        // by Peter Duffet-Smith, for details on the algorithm.
        //
        if (moonPosition == null) {
            // Calculate the solar longitude.  Has the side effect of
            // filling in "meanAnomalySun" as well.
            double sunLong = getSunLongitude();

            //
            // Find the # of days since the epoch of our orbital parameters.
            // TODO: Convert the time of day portion into ephemeris time
            //
            double day = getJulianDay() - JD_EPOCH;       // Days since epoch

            // Calculate the mean longitude and anomaly of the moon, based on
            // a circular orbit.  Similar to the corresponding solar calculation.
            double meanLongitude = norm2PI(13.1763966*PI/180*day + moonL0);
            double meanAnomalyMoon = norm2PI(meanLongitude - 0.1114041*PI/180 * day - moonP0);

            //
            // Calculate the following corrections:
            //  Evection:   the sun's gravity affects the moon's eccentricity
            //  Annual Eqn: variation in the effect due to earth-sun distance
            //  A3:         correction factor (for ???)
            //
            double evection = 1.2739*PI/180 * Math.sin(2 * (meanLongitude - sunLong)
                                                - meanAnomalyMoon);
            double annual   = 0.1858*PI/180 * Math.sin(meanAnomalySun);
            double a3       = 0.3700*PI/180 * Math.sin(meanAnomalySun);

            meanAnomalyMoon += evection - annual - a3;

            //
            // More correction factors:
            //  center  equation of the center correction
            //  a4      yet another error correction (???)
            //
            // TODO: Skip the equation of the center correction and solve Kepler's eqn?
            //
            double center = 6.2886*PI/180 * Math.sin(meanAnomalyMoon);
            double a4 =     0.2140*PI/180 * Math.sin(2 * meanAnomalyMoon);

            // Now find the moon's corrected longitude
            double moonLongitude = meanLongitude + evection + center - annual + a4;

            //
            // And finally, find the variation, caused by the fact that the sun's
            // gravitational pull on the moon varies depending on which side of
            // the earth the moon is on
            //
            double variation = 0.6583*PI/180 * Math.sin(2*(moonLongitude - sunLong));

            moonLongitude += variation;

            //
            // What we've calculated so far is the moon's longitude in the plane
            // of its own orbit.  Now map to the ecliptic to get the latitude
            // and longitude.  First we need to find the longitude of the ascending
            // node, the position on the ecliptic where it is crossed by the moon's
            // orbit as it crosses from the southern to the northern hemisphere.
            //
            double nodeLongitude = norm2PI(moonN0 - 0.0529539*PI/180 * day);

            nodeLongitude -= 0.16*PI/180 * Math.sin(meanAnomalySun);

            double y = Math.sin(moonLongitude - nodeLongitude);
            double x = Math.cos(moonLongitude - nodeLongitude);

            moonEclipLong = Math.atan2(y*Math.cos(moonI), x) + nodeLongitude;
            double moonEclipLat = Math.asin(y * Math.sin(moonI));

            moonPosition = eclipticToEquatorial(moonEclipLong, moonEclipLat);
        }
        return moonPosition;
    }

    /**
     * The "age" of the moon at the time specified in this object.
     * This is really the angle between the
     * current ecliptic longitudes of the sun and the moon,
     * measured in radians.
     *
     * @see #getMoonPhase
     * @internal
     */
    public double getMoonAge() {
        // See page 147 of "Practical Astronomy with your Calculator",
        // by Peter Duffet-Smith, for details on the algorithm.
        //
        // Force the moon's position to be calculated.  We're going to use
        // some the intermediate results cached during that calculation.
        //
        getMoonPosition();

        return norm2PI(moonEclipLong - sunLongitude);
    }

    private static class MoonAge {
        double value;
        MoonAge(double val) { value = val; }
    }

    /**
     * Constant representing a new moon.
     * For use with {@link #getMoonTime(MoonAge, boolean) getMoonTime}
     * @internal
     */
    public static final MoonAge NEW_MOON      = new MoonAge(0);

    /**
     * Find the next or previous time at which the Moon's ecliptic
     * longitude will have the desired value.
     * <p>
     * @param desired   The desired longitude.
     * @param next      <tt>true</tt> if the next occurrance of the phase
     *                  is desired, <tt>false</tt> for the previous occurrance.
     * @internal
     */
    public long getMoonTime(double desired, boolean next)
    {
        return timeOfAngle( new AngleFunc() {
                            @Override
                            public double eval() { return getMoonAge(); } },
                            desired,
                            SYNODIC_MONTH,
                            MINUTE_MS,
                            next);
    }

    /**
     * Find the next or previous time at which the moon will be in the
     * desired phase.
     * <p>
     * @param desired   The desired phase of the moon.
     * @param next      <tt>true</tt> if the next occurrance of the phase
     *                  is desired, <tt>false</tt> for the previous occurrance.
     * @internal
     */
    public long getMoonTime(MoonAge desired, boolean next) {
        return getMoonTime(desired.value, next);
    }

    //-------------------------------------------------------------------------
    // Interpolation methods for finding the time at which a given event occurs
    //-------------------------------------------------------------------------

    private interface AngleFunc {
        public double eval();
    }

    private long timeOfAngle(AngleFunc func, double desired,
                             double periodDays, long epsilon, boolean next)
    {
        // Find the value of the function at the current time
        double lastAngle = func.eval();

        // Find out how far we are from the desired angle
        double deltaAngle = norm2PI(desired - lastAngle) ;

        // Using the average period, estimate the next (or previous) time at
        // which the desired angle occurs.
        double deltaT =  (deltaAngle + (next ? 0 : -PI2)) * (periodDays*DAY_MS) / PI2;

        double lastDeltaT = deltaT; // Liu
        long startTime = time; // Liu

        setTime(time + (long)deltaT);

        // Now iterate until we get the error below epsilon.  Throughout
        // this loop we use normPI to get values in the range -Pi to Pi,
        // since we're using them as correction factors rather than absolute angles.
        do {
            // Evaluate the function at the time we've estimated
            double angle = func.eval();

            // Find the # of milliseconds per radian at this point on the curve
            double factor = Math.abs(deltaT / normPI(angle-lastAngle));

            // Correct the time estimate based on how far off the angle is
            deltaT = normPI(desired - angle) * factor;

            // HACK:
            //
            // If abs(deltaT) begins to diverge we need to quit this loop.
            // This only appears to happen when attempting to locate, for
            // example, a new moon on the day of the new moon.  E.g.:
            //
            // This result is correct:
            // newMoon(7508(Mon Jul 23 00:00:00 CST 1990,false))=
            //   Sun Jul 22 10:57:41 CST 1990
            //
            // But attempting to make the same call a day earlier causes deltaT
            // to diverge:
            // CalendarAstronomer.timeOfAngle() diverging: 1.348508727575625E9 ->
            //   1.3649828540224032E9
            // newMoon(7507(Sun Jul 22 00:00:00 CST 1990,false))=
            //   Sun Jul 08 13:56:15 CST 1990
            //
            // As a temporary solution, we catch this specific condition and
            // adjust our start time by one eighth period days (either forward
            // or backward) and try again.
            // Liu 11/9/00
            if (Math.abs(deltaT) > Math.abs(lastDeltaT)) {
                long delta = (long) (periodDays * DAY_MS / 8);
                setTime(startTime + (next ? delta : -delta));
                return timeOfAngle(func, desired, periodDays, epsilon, next);
            }

            lastDeltaT = deltaT;
            lastAngle = angle;

            setTime(time + (long)deltaT);
        }
        while (Math.abs(deltaT) > epsilon);

        return time;
    }

    //-------------------------------------------------------------------------
    // Other utility methods
    //-------------------------------------------------------------------------

    /***
     * Given 'value', add or subtract 'range' until 0 <= 'value' < range.
     * The modulus operator.
     */
    private static final double normalize(double value, double range) {
        return value - range * Math.floor(value / range);
    }

    /**
     * Normalize an angle so that it's in the range 0 - 2pi.
     * For positive angles this is just (angle % 2pi), but the Java
     * mod operator doesn't work that way for negative numbers....
     */
    private static final double norm2PI(double angle) {
        return normalize(angle, PI2);
    }

    /**
     * Normalize an angle into the range -PI - PI
     */
    private static final double normPI(double angle) {
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
    private double trueAnomaly(double meanAnomaly, double eccentricity)
    {
        // First, solve Kepler's equation iteratively
        // Duffett-Smith, p.90
        double delta;
        double E = meanAnomaly;
        do {
            delta = E - eccentricity * Math.sin(E) - meanAnomaly;
            E = E - delta / (1 - eccentricity * Math.cos(E));
        }
        while (Math.abs(delta) > 1e-5); // epsilon = 1e-5 rad

        return 2.0 * Math.atan( Math.tan(E/2) * Math.sqrt( (1+eccentricity)
                                                          /(1-eccentricity) ) );
    }

    /**
     * Return the obliquity of the ecliptic (the angle between the ecliptic
     * and the earth's equator) at the current time.  This varies due to
     * the precession of the earth's axis.
     *
     * @return  the obliquity of the ecliptic relative to the equator,
     *          measured in radians.
     */
    private double eclipticObliquity() {
        final double epoch = 2451545.0;     // 2000 AD, January 1.5

        double T = (getJulianDay() - epoch) / 36525;

        double eclipObliquity = 23.439292
                           - 46.815/3600 * T
                           - 0.0006/3600 * T*T
                           + 0.00181/3600 * T*T*T;

        return eclipObliquity * DEG_RAD;
    }


    //-------------------------------------------------------------------------
    // Private data
    //-------------------------------------------------------------------------

    /**
     * Current time in milliseconds since 1/1/1970 AD
     * @see java.util.Date#getTime
     */
    private long time;

    //
    // The following fields are used to cache calculated results for improved
    // performance.  These values all depend on the current time setting
    // of this object, so the clearCache method is provided.
    //
    static final private double INVALID = Double.MIN_VALUE;

    private transient double    julianDay       = INVALID;
    private transient double    sunLongitude    = INVALID;
    private transient double    meanAnomalySun  = INVALID;
    private transient double    moonEclipLong   = INVALID;

    private transient Equatorial  moonPosition = null;

    private void clearCache() {
        julianDay       = INVALID;
        sunLongitude    = INVALID;
        meanAnomalySun  = INVALID;
        moonEclipLong   = INVALID;
        moonPosition    = null;
    }

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
     * @internal
     */
    public static final class Ecliptic {
        /**
         * Constructs an Ecliptic coordinate object.
         * <p>
         * @param lat The ecliptic latitude, measured in radians.
         * @param lon The ecliptic longitude, measured in radians.
         * @internal
         */
        public Ecliptic(double lat, double lon) {
            latitude = lat;
            longitude = lon;
        }

        /**
         * Return a string representation of this object
         * @internal
         */
        @Override
        public String toString() {
            return Double.toString(longitude*RAD_DEG) + "," + (latitude*RAD_DEG);
        }

        /**
         * The ecliptic latitude, in radians.  This specifies an object's
         * position north or south of the plane of the ecliptic,
         * with positive angles representing north.
         * @internal
         */
        public final double latitude;

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
         */
        public final double longitude;
    }

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
     * @internal
     */
    public static final class Equatorial {
        /**
         * Constructs an Equatorial coordinate object.
         * <p>
         * @param asc The right ascension, measured in radians.
         * @param dec The declination, measured in radians.
         * @internal
         */
        public Equatorial(double asc, double dec) {
            ascension = asc;
            declination = dec;
        }

        /**
         * Return a string representation of this object, with the
         * angles measured in degrees.
         * @internal
         */
        @Override
        public String toString() {
            return Double.toString(ascension*RAD_DEG) + "," + (declination*RAD_DEG);
        }

        /**
         * Return a string representation of this object with the right ascension
         * measured in hours, minutes, and seconds.
         * @internal
         */
        public String toHmsString() {
            return radToHms(ascension) + "," + radToDms(declination);
        }

        /**
         * The right ascension, in radians.
         * This is the position east or west along the equator
         * relative to the sun's position at the vernal equinox,
         * with positive angles representing East.
         * @internal
         */
        public final double ascension;

        /**
         * The declination, in radians.
         * This is the position north or south of the equatorial plane,
         * with positive angles representing north.
         * @internal
         */
        public final double declination;
    }

    static private String radToHms(double angle) {
        int hrs = (int) (angle*RAD_HOUR);
        int min = (int)((angle*RAD_HOUR - hrs) * 60);
        int sec = (int)((angle*RAD_HOUR - hrs - min/60.0) * 3600);

        return Integer.toString(hrs) + "h" + min + "m" + sec + "s";
    }

    static private String radToDms(double angle) {
        int deg = (int) (angle*RAD_DEG);
        int min = (int)((angle*RAD_DEG - deg) * 60);
        int sec = (int)((angle*RAD_DEG - deg - min/60.0) * 3600);

        return Integer.toString(deg) + "\u00b0" + min + "'" + sec + "\"";
    }
}
