/*
 *******************************************************************************
 * Copyright (C) 2006-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class CalendarTest extends ICUTestCase {

    /*
     * Test method for 'com.ibm.icu.util.Calendar.hashCode()'
     */
    public void testHashCode() {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        Calendar cal3 = Calendar.getInstance();
        cal3.setMinimalDaysInFirstWeek(cal3.getMinimalDaysInFirstWeek()+1);
        testEHCS(cal1, cal2, cal3);
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.Calendar(Calendar)'
     */
    public void testCalendar() {
        // tested implicitly everywhere
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getInstance()'
     */
    public void testGetInstance() {
        // tested by testEHCS
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getInstance(TimeZone)'
     */
    public void testGetInstanceTimeZone() {
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar cal = Calendar.getInstance(tz);
        assertNotNull(cal);
        assertNotNull(cal.getTime());
        assertEquals(tz, cal.getTimeZone());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getInstance(Locale)'
     */
    public void testGetInstanceLocale() {
        Calendar cal = Calendar.getInstance(Locale.US);
        assertNotNull(cal);
        assertNotNull(cal.getTime());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getInstance(ULocale)'
     */
    public void testGetInstanceULocale() {
        Calendar cal = Calendar.getInstance(ULocale.US);
        assertNotNull(cal);
        assertNotNull(cal.getTime());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getInstance(TimeZone, Locale)'
     */
    public void testGetInstanceTimeZoneLocale() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Calendar.getInstance(tz, Locale.US);
        assertNotNull(cal);
        assertNotNull(cal.getTime());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getInstance(TimeZone, ULocale)'
     */
    public void testGetInstanceTimeZoneULocale() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        Calendar cal = Calendar.getInstance(tz, ULocale.US);
        assertNotNull(cal);
        assertNotNull(cal.getTime());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getAvailableLocales()'
     */
    public void testGetAvailableLocales() {
        assertNotNull(Calendar.getAvailableLocales());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getAvailableULocales()'
     */
    public void testGetAvailableULocales() {
        assertNotNull(Calendar.getAvailableULocales());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getTime()'
     */
    public void testGetTime() {
        Calendar cal = Calendar.getInstance();
        assertNotNull(cal.getTime());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.setTime(Date)'
     */
    public void testSetTime() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2006, 0, 20, 9, 30, 0);
        Date date = cal.getTime();
        cal = Calendar.getInstance();
        cal.setTime(date);
        assertEquals(date, cal.getTime());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getTimeInMillis()'
     */
    public void testGetTimeInMillis() {
        Calendar cal = Calendar.getInstance();
        assertTrue(0 != cal.getTimeInMillis());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.setTimeInMillis(long)'
     */
    public void testSetTimeInMillis() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2006, 0, 20, 9, 30, 0);
        long millis = cal.getTimeInMillis();
        Date date = cal.getTime();
        
        cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        
        assertEquals(date, cal.getTime());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.get(int)'
     */
    public void testGet() {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2006, 0, 20, 9, 30, 0);
        assertEquals(0, cal.get(Calendar.MONTH));
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(30, cal.get(Calendar.MINUTE));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.set(int, int)'
     */
    public void testSetIntInt() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1977);
        assertEquals(1977, cal.get(Calendar.YEAR));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.set(int, int, int)'
     */
    public void testSetIntIntInt() {
        Calendar cal = Calendar.getInstance();
        cal.set(1997, 9, 15);
        assertEquals(15, cal.get(Calendar.DATE));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.set(int, int, int, int, int)'
     */
    public void testSetIntIntIntIntInt() {
        Calendar cal = Calendar.getInstance();
        cal.set(1997, 9, 15, 14, 25);
        assertEquals(25, cal.get(Calendar.MINUTE));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.set(int, int, int, int, int, int)'
     */
    public void testSetIntIntIntIntIntInt() {
        Calendar cal = Calendar.getInstance();
        cal.set(1997, 9, 15, 14, 25, 51);
        assertEquals(51, cal.get(Calendar.SECOND));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.clear()'
     */
    public void testClear() {
        Calendar cal = Calendar.getInstance();
        cal.set(1997, 9, 15, 14, 25, 51);
        cal.clear();
        assertEquals(0, cal.get(Calendar.MONTH));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.clear(int)'
     */
    public void testClearInt() {
        Calendar cal = Calendar.getInstance();
        cal.set(1997, 9, 15, 14, 25, 51);
        assertTrue(cal.isSet(Calendar.DAY_OF_MONTH));
        cal.clear(Calendar.DAY_OF_MONTH);
        assertFalse(cal.isSet(Calendar.DAY_OF_MONTH));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.isSet(int)'
     */
    public void testIsSet() {
        // see testClearInt
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.equals(Object)'
     */
    public void testEqualsObject() {
        // tested by testHashCode
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.isEquivalentTo(Calendar)'
     */
    public void testIsEquivalentTo() {
        Calendar cal = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal2.set(1994, 6, 21, 8, 7);
        assertTrue(cal.isEquivalentTo(cal2));
        cal.setTimeZone(TimeZone.getTimeZone("CST"));
        cal2.setTimeZone(TimeZone.getTimeZone("PDT"));
        assertFalse(cal.isEquivalentTo(cal2));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.before(Object)'
     */
    public void testBefore() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1990);
        assertTrue(cal.before(new Date()));
        assertTrue(cal.before(Calendar.getInstance()));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.after(Object)'
     */
    public void testAfter() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 3058);
        assertTrue(cal.after(new Date()));
        assertTrue(cal.after(Calendar.getInstance()));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getActualMaximum(int)'
     */
    public void testGetActualMaximum() {
        Calendar cal = Calendar.getInstance(Locale.US);
        assertEquals(11, cal.getActualMaximum(Calendar.MONTH));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getActualMinimum(int)'
     */
    public void testGetActualMinimum() {
        Calendar cal = Calendar.getInstance(Locale.US);
        assertEquals(0, cal.getActualMinimum(Calendar.MONTH));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.roll(int, boolean)'
     */
    public void testRollIntBoolean() {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(1997, 1, 27);
        cal.roll(Calendar.DATE, true);
        assertEquals(28, cal.get(Calendar.DATE));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.roll(int, int)'
     */
    public void testRollIntInt() {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(1997, 1, 27);
        cal.roll(Calendar.DATE, 3);
        assertEquals(2, cal.get(Calendar.DATE));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.add(int, int)'
     */
    public void testAdd() {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(1997, 1, 27);
        cal.add(Calendar.DATE, 3);
        assertEquals(2, cal.get(Calendar.DATE));
        assertEquals(2, cal.get(Calendar.MONTH));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getDisplayName(Locale)'
     */
    public void testGetDisplayNameLocale() {
        Calendar cal = Calendar.getInstance();
        assertEquals("Calendar", cal.getDisplayName(Locale.US));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getDisplayName(ULocale)'
     */
    public void testGetDisplayNameULocale() {
        Calendar cal = Calendar.getInstance();
        assertEquals("Calendar", cal.getDisplayName(ULocale.US));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.compareTo(Calendar)'
     */
    public void testCompareToCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1990);
        assertTrue(0 > cal.compareTo(Calendar.getInstance()));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.compareTo(Object)'
     */
    public void testCompareToObject() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 1990);
        assertTrue(0 > cal.compareTo((Object)Calendar.getInstance()));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getDateTimeFormat(int, int, Locale)'
     */
    public void testGetDateTimeFormatIntIntLocale() {
        Calendar cal = Calendar.getInstance();
        cal.set(1990, 8, 16, 20, 3);
        DateFormat df = cal.getDateTimeFormat(DateFormat.LONG, DateFormat.SHORT, Locale.US);
        assertEquals("September 16, 1990 8:03 PM", df.format(cal));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getDateTimeFormat(int, int, ULocale)'
     */
    public void testGetDateTimeFormatIntIntULocale() {
        Calendar cal = Calendar.getInstance();
        cal.set(1990, 8, 16, 20, 3);
        DateFormat df = cal.getDateTimeFormat(DateFormat.LONG, DateFormat.SHORT, ULocale.US);
        assertEquals("September 16, 1990 8:03 PM", df.format(cal));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.fieldDifference(Date, int)'
     */
    public void testFieldDifference() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 0);
        Date date = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 5);
        assertEquals(-5, cal.fieldDifference(date, Calendar.DAY_OF_MONTH));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getTimeZone()'
     */
    public void testGetTimeZone() {
        Calendar cal = Calendar.getInstance();
        assertNotNull(cal.getTimeZone());
    }
    
    /*
     * Test method for 'com.ibm.icu.util.Calendar.setTimeZone(TimeZone)'
     */
    public void testSetTimeZone() {
        Calendar cal = Calendar.getInstance();
        TimeZone value1 = cal.getTimeZone();
        String tzn = "PDT".equals(value1.getID()) ? "CST" : "PDT";
        TimeZone value2 = TimeZone.getTimeZone(tzn);
        cal.setTimeZone(value2);
        TimeZone result = cal.getTimeZone();
        assertNotEqual(value1, result);
        assertEquals(value2, result);
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.setLenient(boolean)'
     */
    public void testSetLenient() {
        Calendar cal = Calendar.getInstance();
        boolean lenient = cal.isLenient();
        cal.setLenient(!lenient);
        assertFalse(lenient == cal.isLenient());
        
        // not testing if it has the expected effect
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.isLenient()'
     */
    public void testIsLenient() {
        // tested by testSetLenient
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.setFirstDayOfWeek(int)'
     */
    public void testSetFirstDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        int firstDay = cal.getFirstDayOfWeek();
        cal.setFirstDayOfWeek(firstDay+1);
        assertEquals(firstDay+1, cal.getFirstDayOfWeek());

        // don't test functionality
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getFirstDayOfWeek()'
     */
    public void testGetFirstDayOfWeek() {
        // tested by testSetFirstDayOfWeek
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.setMinimalDaysInFirstWeek(int)'
     */
    public void testSetMinimalDaysInFirstWeek() {
        Calendar cal = Calendar.getInstance();
        int firstDay = cal.getMinimalDaysInFirstWeek();
        cal.setMinimalDaysInFirstWeek(firstDay+1);
        assertEquals(firstDay+1, cal.getMinimalDaysInFirstWeek());

        // don't test functionality
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getMinimalDaysInFirstWeek()'
     */
    public void testGetMinimalDaysInFirstWeek() {
        // tested by testSetMinimalDaysInFirstWeek
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getMinimum(int)'
     */
    public void testGetMinimum() {
        Calendar cal = Calendar.getInstance();
        assertEquals(1, cal.getMinimum(Calendar.DAY_OF_WEEK));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getMaximum(int)'
     */
    public void testGetMaximum() {
        Calendar cal = Calendar.getInstance();
        assertEquals(7, cal.getMaximum(Calendar.DAY_OF_WEEK));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getGreatestMinimum(int)'
     */
    public void testGetGreatestMinimum() {
        Calendar cal = Calendar.getInstance();
        assertEquals(1, cal.getGreatestMinimum(Calendar.DATE));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getLeastMaximum(int)'
     */
    public void testGetLeastMaximum() {
        Calendar cal = Calendar.getInstance();
        assertEquals(28, cal.getLeastMaximum(Calendar.DATE));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getDayOfWeekType(int)'
     */
    public void testGetDayOfWeekType() {
        Calendar cal = Calendar.getInstance(Locale.US);
        assertEquals(Calendar.WEEKDAY, cal.getDayOfWeekType(Calendar.FRIDAY));
        assertEquals(Calendar.WEEKEND, cal.getDayOfWeekType(Calendar.SATURDAY));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getWeekendTransition(int)'
     */
    public void testGetWeekendTransition() {
        Calendar cal = Calendar.getInstance(Locale.US);
        try {
            cal.getWeekendTransition(Calendar.WEEKEND_ONSET);
            fail("expected IllegalArgumentException from getWeekendTransition");
        }
        catch (IllegalArgumentException e) {
            // ok
        }
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.isWeekend(Date)'
     */
    public void testIsWeekendDate() {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        assertTrue(cal.isWeekend(cal.getTime()));
        cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        assertFalse(cal.isWeekend(cal.getTime()));
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.isWeekend()'
     */
    public void testIsWeekend() {
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
        assertTrue(cal.isWeekend());
        cal.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
        assertFalse(cal.isWeekend());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.clone()'
     */
    public void testClone() {
        // tested by testHashCode
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.toString()'
     */
    public void testToString() {
        Calendar cal = Calendar.getInstance();
        assertNotNull(cal.toString());
    }

    /*
     * Test method for 'com.ibm.icu.util.Calendar.getType()'
     */
    public void testGetType() {
        Calendar cal = Calendar.getInstance(Locale.US);
        assertEquals("gregorian", cal.getType());
    }
}
