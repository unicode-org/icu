/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class DateFormatTest extends ICUTestCase {
    private Calendar aCal;
    private Calendar anESTCal;
    private Date aDate;
    private String aDateString;
    private String aTimeString;
    private String anESTTimeString;
    private String aDateTimeString;
    private String aShortDateTimeString;
    private String aDefaultESTDateTimeString;
    private DateFormat aDF;
    private StringBuffer aBuf;
    private FieldPosition anFP;
        
    protected void setUp() throws Exception {
        super.setUp();
                
        java.util.GregorianCalendar gcal = new java.util.GregorianCalendar();
        gcal.clear();
        gcal.set(java.util.GregorianCalendar.YEAR, 1990);
        gcal.set(java.util.GregorianCalendar.MONTH, java.util.GregorianCalendar.DECEMBER);
        gcal.set(java.util.GregorianCalendar.DATE, 17);
        gcal.set(java.util.GregorianCalendar.HOUR, 5);
        gcal.set(java.util.GregorianCalendar.MINUTE, 17);
        aCal = new Calendar(gcal);
        anESTCal = Calendar.getInstance();
        anESTCal.setTimeZone(TimeZone.getTimeZone("EST"));
        aDate = gcal.getTime();
        aDateString = "Dec 17, 1990"; // medium -- the default
        aTimeString = "5:17:00 AM"; // medium
        anESTTimeString = "8:17:00 AM";
        aDateTimeString = "Dec 17, 1990 5:17:00 AM"; // medium, medium
        aDefaultESTDateTimeString = "Dec 17, 1990 8:17 AM"; // medium, short -- the default
        aShortDateTimeString = "12/17/90 5:17 AM"; // short, short
        aDF = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
        aBuf = new StringBuffer();
        anFP = new FieldPosition(0);
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.hashCode()'
     */
    public final void testHashCode() {
        DateFormat df = DateFormat.getInstance();
        DateFormat eq = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        testEHCS(df, eq, aDF);
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.DateFormat(DateFormat)'
     */
    public final void testDateFormat() {
        DateFormat df = new DateFormat(java.text.DateFormat.getInstance());
        assertEquals(DateFormat.getInstance(), df);
    }

    private void assertEqualDateString(StringBuffer buf) {
        assertEquals(aDateTimeString, buf.toString());
    }
        
    private void assertEqualDateString(String str) {
        assertEquals(aDateTimeString, str);
    }
        
    /*
     * Test method for 'com.ibm.icu.text.DateFormat.format(Object, StringBuffer, FieldPosition)'
     */
    public final void testFormatObjectStringBufferFieldPosition() {
        assertEqualDateString(aDF.format(aDate, aBuf, anFP));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.format(Calendar, StringBuffer, FieldPosition)'
     */
    public final void testFormatCalendarStringBufferFieldPosition() {
        assertEqualDateString(aDF.format(aCal, aBuf, anFP));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.format(Date, StringBuffer, FieldPosition)'
     */
    public final void testFormatDateStringBufferFieldPosition() {
        assertEqualDateString(aDF.format(aDate, aBuf, anFP));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.format(Date)'
     */
    public final void testFormatDate() {
        assertEqualDateString(aDF.format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.parse(String)'
     */
    public final void testParseString() throws Exception {
        assertEquals(aDate, aDF.parse(aDateTimeString));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.parse(String, Calendar, ParsePosition)'
     */
    public final void testParseStringCalendarParsePosition() {
        aDF.parse(aDateTimeString, aCal, new ParsePosition(0));
        assertEquals(aDate, aCal.getTime());
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.parse(String, ParsePosition)'
     */
    public final void testParseStringParsePosition() {
        assertEquals(aDate, aDF.parse(aDateTimeString, new ParsePosition(0)));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.parseObject(String, ParsePosition)'
     */
    public final void testParseObjectStringParsePosition() {
        assertEquals(aDate, aDF.parseObject(aDateTimeString, new ParsePosition(0)));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getTimeInstance()'
     */
    public final void testGetTimeInstance() {
        assertEquals(aTimeString, DateFormat.getTimeInstance().format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getTimeInstance(int)'
     */
    public final void testGetTimeInstanceInt() {
        assertEquals(aTimeString, DateFormat.getTimeInstance(DateFormat.MEDIUM).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getTimeInstance(int, Locale)'
     */
    public final void testGetTimeInstanceIntLocale() {
        assertEquals(aTimeString, DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getTimeInstance(int, ULocale)'
     */
    public final void testGetTimeInstanceIntULocale() {
        assertEquals(aTimeString, DateFormat.getTimeInstance(DateFormat.MEDIUM, ULocale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateInstance()'
     */
    public final void testGetDateInstance() {
        assertEquals(aDateString, DateFormat.getDateInstance().format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateInstance(int)'
     */
    public final void testGetDateInstanceInt() {
        assertEquals(aDateString, DateFormat.getDateInstance(DateFormat.MEDIUM).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateInstance(int, Locale)'
     */
    public final void testGetDateInstanceIntLocale() {
        assertEquals(aDateString, DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateInstance(int, ULocale)'
     */
    public final void testGetDateInstanceIntULocale() {
        assertEquals(aDateString, DateFormat.getDateInstance(DateFormat.MEDIUM, ULocale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateTimeInstance()'
     */
    public final void testGetDateTimeInstance() {
        assertEquals(aDateTimeString, DateFormat.getDateTimeInstance().format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateTimeInstance(int, int)'
     */
    public final void testGetDateTimeInstanceIntInt() {
        assertEquals(aDateTimeString, 
                     DateFormat.getDateTimeInstance(
                                                    DateFormat.MEDIUM, DateFormat.MEDIUM).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateTimeInstance(int, int, Locale)'
     */
    public final void testGetDateTimeInstanceIntIntLocale() {
        assertEquals(aDateTimeString, 
                     DateFormat.getDateTimeInstance(
                                                    DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateTimeInstance(int, int, ULocale)'
     */
    public final void testGetDateTimeInstanceIntIntULocale() {
        assertEquals(aDateTimeString, 
                     DateFormat.getDateTimeInstance(
                                                    DateFormat.MEDIUM, DateFormat.MEDIUM, ULocale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getInstance()'
     */
    public final void testGetInstance() {
        assertEquals(aShortDateTimeString, DateFormat.getInstance().format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getAvailableLocales()'
     */
    public final void testGetAvailableLocales() {
        Locale[] locales = DateFormat.getAvailableLocales();
        if (ICUTestCase.testingWrapper) {
            ICUTestCase.assertArraysEqual(java.text.DateFormat.getAvailableLocales(), locales);
        } else {
            assertNotNull(locales);
        }
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.setCalendar(Calendar)'
     */
    public final void testSetCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("EST"));
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        df.setCalendar(cal);
        assertEquals("8:17 AM", df.format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getCalendar()'
     */
    public final void testGetCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("EST"));
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        df.setCalendar(cal);
        assertEquals(cal, df.getCalendar());
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.setNumberFormat(NumberFormat)'
     */
    public final void testSetNumberFormat() {
        // no easy way to test effect of setting the number format
        NumberFormat nf = NumberFormat.getInstance();
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        df.setNumberFormat(nf);
        // note, can't actually USE the dateformat since it changes the calendar
        assertEquals(nf, df.getNumberFormat());
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getNumberFormat()'
     */
    public final void testGetNumberFormat() {
        // see testSetNumberFormat
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.setTimeZone(TimeZone)'
     */
    public final void testSetTimeZone() {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        TimeZone tz = TimeZone.getTimeZone("EST");
        df.setTimeZone(tz);
        assertEquals("8:17 AM", df.format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getTimeZone()'
     */
    public final void testGetTimeZone() {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        TimeZone tz = TimeZone.getTimeZone("EST");
        df.setTimeZone(tz);
        assertEquals(tz, df.getTimeZone());
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.setLenient(boolean)'
     */
    public final void testSetLenient() throws Exception {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        df.parse("2/31/90"); // succeeds, default is lenient
        df.setLenient(false);
        try {
            df.parse("2/31/90");
            throw new Exception("strict parse should have failed");
        }
        catch (ParseException e) {
            // ok, this is what we expect
        }
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.isLenient()'
     */
    public final void testIsLenient() {
        DateFormat df = DateFormat.getInstance();
        assertTrue(df.isLenient());
        df.setLenient(false);
        assertFalse(df.isLenient());
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateInstance(Calendar, int, Locale)'
     */
    public final void testGetDateInstanceCalendarIntLocale() {
        assertEquals(aDateString, DateFormat.getDateInstance(aCal, DateFormat.MEDIUM, Locale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateInstance(Calendar, int, ULocale)'
     */
    public final void testGetDateInstanceCalendarIntULocale() {
        assertEquals(aDateString, DateFormat.getDateInstance(aCal, DateFormat.MEDIUM, ULocale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getTimeInstance(Calendar, int, Locale)'
     */
    public final void testGetTimeInstanceCalendarIntLocale() {
        assertEquals(anESTTimeString, DateFormat.getTimeInstance(anESTCal, DateFormat.MEDIUM, Locale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getTimeInstance(Calendar, int, ULocale)'
     */
    public final void testGetTimeInstanceCalendarIntULocale() {
        assertEquals(anESTTimeString, DateFormat.getTimeInstance(anESTCal, DateFormat.MEDIUM, ULocale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateTimeInstance(Calendar, int, int, Locale)'
     */
    public final void testGetDateTimeInstanceCalendarIntIntLocale() {
        assertEquals(aDefaultESTDateTimeString, DateFormat.getDateTimeInstance(anESTCal, DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateTimeInstance(Calendar, int, int, ULocale)'
     */
    public final void testGetDateTimeInstanceCalendarIntIntULocale() {
        assertEquals(aDefaultESTDateTimeString, DateFormat.getDateTimeInstance(anESTCal, DateFormat.MEDIUM, DateFormat.SHORT, ULocale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getInstance(Calendar, Locale)'
     */
    public final void testGetInstanceCalendarLocale() {
        assertEquals(aDefaultESTDateTimeString, DateFormat.getInstance(anESTCal, Locale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getInstance(Calendar, ULocale)'
     */
    public final void testGetInstanceCalendarULocale() {
        assertEquals(aDefaultESTDateTimeString, DateFormat.getInstance(anESTCal, ULocale.US).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getInstance(Calendar)'
     */
    public final void testGetInstanceCalendar() {
        assertEquals(aDefaultESTDateTimeString, DateFormat.getInstance(anESTCal).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateInstance(Calendar, int)'
     */
    public final void testGetDateInstanceCalendarInt() {
        assertEquals(aDateString, DateFormat.getDateInstance(aCal, DateFormat.MEDIUM).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getTimeInstance(Calendar, int)'
     */
    public final void testGetTimeInstanceCalendarInt() {
        assertEquals(anESTTimeString, DateFormat.getTimeInstance(anESTCal, DateFormat.MEDIUM).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.getDateTimeInstance(Calendar, int, int)'
     */
    public final void testGetDateTimeInstanceCalendarIntInt() {
        assertEquals(aDefaultESTDateTimeString, DateFormat.getDateTimeInstance(anESTCal, DateFormat.MEDIUM, DateFormat.SHORT).format(aDate));
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.toString()'
     */
    public final void testToString() {
        assertNotNull(aDF.toString());
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.clone()'
     */
    public final void testClone() {
        // see testHashCode
    }

    /*
     * Test method for 'com.ibm.icu.text.DateFormat.equals(Object)'
     */
    public final void testEqualsObject() {
        // see testHashCode
    }
}
