/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/test/calendar/Attic/AstroTest.java,v $ 
 * $Date: 2000/05/12 23:19:12 $ 
 * $Revision: 1.4 $
 *
 *****************************************************************************************
 */
package com.ibm.test.calendar;

// AstroTest

import com.ibm.test.*;
import com.ibm.util.*;

import com.ibm.util.CalendarAstronomer.*;

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
            
            Equatorial result = astro.getSunPosition();
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
        }

    }
    
    public void TestCoordinates() {
        Equatorial result = astro.eclipticToEquatorial(139.686111 * PI/ 180.0, 4.875278* PI / 180.0);
        logln("result is " + result + ";  " + result.toHmsString());
    }
    
}
