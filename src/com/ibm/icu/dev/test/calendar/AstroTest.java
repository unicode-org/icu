/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/calendar/AstroTest.java,v $ 
 * $Date: 2002/10/24 19:31:46 $ 
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

// AstroTest

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.dev.test.*;
import com.ibm.icu.util.*;
import com.ibm.icu.util.CalendarAstronomer.*;
import com.ibm.icu.text.DateFormat;

// TODO: try finding next new moon after  07/28/1984 16:00 GMT

public class AstroTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new AstroTest().run(args);
    }

    static final double PI = Math.PI;
    
    static GregorianCalendar gc = new GregorianCalendar(new SimpleTimeZone(0, "UTC"));
    static CalendarAstronomer astro = new CalendarAstronomer();
    
    public void TestSolarLongitude() {
        final double tests[][] = {
            { 1980, 7, 27, 00, 00, 124.114347 },
            { 1988, 7, 27, 00, 00, 124.187732 },
        };
        logln("");
        for (int i = 0; i < tests.length; i++) {
            gc.clear();
            gc.set((int)tests[i][0], (int)tests[i][1]-1, (int)tests[i][2], (int)tests[i][3], (int) tests[i][4]);
            
            astro.setDate(gc.getTime());
            
            double longitude = astro.getSunLongitude();
            longitude = 0;
            Equatorial result = astro.getSunPosition();
            result = null;
        }
    }
    
    public void TestLunarPosition() {
        final double tests[][] = {
            { 1979, 2, 26, 16, 00,  0, 0 },
        };
        logln("");
        
        for (int i = 0; i < tests.length; i++) {
            gc.clear();
            gc.set((int)tests[i][0], (int)tests[i][1]-1, (int)tests[i][2], (int)tests[i][3], (int) tests[i][4]);
            astro.setDate(gc.getTime());
            
            Equatorial result = astro.getMoonPosition();
            result = null;
        }

    }
    
    public void TestCoordinates() {
        Equatorial result = astro.eclipticToEquatorial(139.686111 * PI/ 180.0, 4.875278* PI / 180.0);
        logln("result is " + result + ";  " + result.toHmsString());
    }
    
    public void TestCoverage() {
	GregorianCalendar cal = new GregorianCalendar(1958, Calendar.AUGUST, 15);
	Date then = cal.getTime();
	CalendarAstronomer myastro = new CalendarAstronomer(then);

	//Latitude:  34 degrees 05' North  
	//Longitude:  118 degrees 22' West  
	double laLat = 34 + 5d/60, laLong = 360 - (118 + 22d/60);
	CalendarAstronomer myastro2 = new CalendarAstronomer(laLong, laLat);

	double eclLat = laLat * Math.PI / 360;
	double eclLong = laLong * Math.PI / 360;
	Ecliptic ecl = new Ecliptic(eclLat, eclLong);
	logln("ecliptic: " + ecl);

	CalendarAstronomer myastro3 = new CalendarAstronomer();
	myastro3.setJulianDay((4713 + 2000) * 365.25);

	CalendarAstronomer[] astronomers = {
	    myastro, myastro2, myastro3, myastro2 // check cache
	};

	for (int i = 0; i < astronomers.length; ++i) {
	    CalendarAstronomer astro = astronomers[i];

	    logln("astro: " + astro);
	    logln("   time: " + astro.getTime());
	    logln("   date: " + astro.getDate());
	    logln("   cent: " + astro.getJulianCentury());
	    logln("   gw sidereal: " + astro.getGreenwichSidereal());
	    logln("   loc sidereal: " + astro.getLocalSidereal());
	    logln("   equ ecl: " + astro.eclipticToEquatorial(ecl));
	    logln("   equ long: " + astro.eclipticToEquatorial(eclLong));
	    logln("   horiz: " + astro.eclipticToHorizon(eclLong));
	    logln("   sunrise: " + new Date(astro.getSunRiseSet(true)));
	    logln("   sunset: " + new Date(astro.getSunRiseSet(false)));
	    logln("   moon phase: " + astro.getMoonPhase());
	    logln("   moonrise: " + new Date(astro.getMoonRiseSet(true)));
	    logln("   moonset: " + new Date(astro.getMoonRiseSet(false)));
	    logln("   prev summer solstice: " + new Date(astro.getSunTime(astro.SUMMER_SOLSTICE, false)));
	    logln("   next summer solstice: " + new Date(astro.getSunTime(astro.SUMMER_SOLSTICE, true)));
	    logln("   prev full moon: " + new Date(astro.getMoonTime(astro.FULL_MOON, false)));
	    logln("   next full moon: " + new Date(astro.getMoonTime(astro.FULL_MOON, true)));
	}
    }

    public void TestSunriseTimes() {
        //        logln("Sunrise/Sunset times for San Jose, California, USA");
        //        CalendarAstronomer astro = new CalendarAstronomer(-121.55, 37.20);
        //        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");

        logln("Sunrise/Sunset times for Toronto, Canada");
        CalendarAstronomer astro = new CalendarAstronomer(-79.38, 43.65);
        TimeZone tz = TimeZone.getTimeZone("America/Detroit");

        GregorianCalendar cal = new GregorianCalendar(tz, Locale.US);
        cal.set(cal.YEAR, 2001);
        cal.set(cal.MONTH, cal.APRIL);
        cal.set(cal.DAY_OF_MONTH, 1);

        DateFormat df = DateFormat.getTimeInstance(cal, DateFormat.MEDIUM, Locale.US);
        DateFormat day = DateFormat.getDateInstance(cal, DateFormat.MEDIUM, Locale.US);
		
		
        for (int i=0; i < 30; i++) {
            astro.setDate(cal.getTime());
			
            Date sunrise = new Date(astro.getSunRiseSet(true));
            Date sunset = new Date(astro.getSunRiseSet(false));

            logln("Date: " + day.format(cal.getTime()) +
                  ", Sunrise: " + df.format(sunrise) +
                  ", Sunset: " + df.format(sunset));
			
            cal.add(Calendar.DATE, 1);
        }		
    }
}
