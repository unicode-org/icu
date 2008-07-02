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

import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class SimpleDateFormatTest extends ICUTestCase {
    private static final String mdy = "MMM dd yyyy";
    private static final String md2 = "MMM dd yy";
    private static final String hmz = "'The time is' HH:mm:ss zzz";
    private static final String hmzmdy = hmz + " 'on' " + mdy;
    private static final String hmzmdyStr = "The time is 15:05:20 CST on Jan 10 2006";
        
    private static final TimeZone tzc = TimeZone.getTimeZone("CST");
    private static final TimeZone tzp = TimeZone.getTimeZone("PST");
    private static final Calendar cal = Calendar.getInstance(tzc);
    private static final Date date;
    static {
        cal.clear();
        cal.set(2006, 0, 10, 15, 5, 20); // arrgh, doesn't clear millis
        date = cal.getTime();
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.format(Calendar, StringBuffer, FieldPosition)'
     */
    public void testFormatCalendarStringBufferFieldPosition() {
        StringBuffer buf = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        SimpleDateFormat sdf = new SimpleDateFormat(hmzmdy);
        sdf.format(cal, buf, fp);
        assertEquals(hmzmdyStr, buf.toString());
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.parse(String, Calendar, ParsePosition)'
     */
    public void testParseStringCalendarParsePosition() {
        Calendar cal = Calendar.getInstance(tzp);
        cal.clear();
        ParsePosition pp = new ParsePosition(0);
        SimpleDateFormat sdf = new SimpleDateFormat(hmzmdy);
        sdf.parse(hmzmdyStr, cal, pp);
        assertEquals(date, cal.getTime());
        // note: java doesn't return the parsed time zone
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.clone()'
     */
    public void testClone() {

    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.SimpleDateFormat()'
     */
    public void testSimpleDateFormat() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        java.text.SimpleDateFormat jsdf = new java.text.SimpleDateFormat();
        assertEquals(jsdf.format(date), sdf.format(date));
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.SimpleDateFormat(String)'
     */
    public void testSimpleDateFormatString() {
        SimpleDateFormat sdf = new SimpleDateFormat(mdy);
        java.text.SimpleDateFormat jsdf = new java.text.SimpleDateFormat(mdy);
        assertEquals(jsdf.format(date), sdf.format(date));
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.SimpleDateFormat(String, Locale)'
     */
    public void testSimpleDateFormatStringLocale() {
        Locale l = Locale.JAPAN;
        SimpleDateFormat sdf = new SimpleDateFormat(mdy, l);
        java.text.SimpleDateFormat jsdf = new java.text.SimpleDateFormat(mdy, l);
        assertEquals(jsdf.format(date), sdf.format(date));
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.SimpleDateFormat(String, ULocale)'
     */
    public void testSimpleDateFormatStringULocale() {
        ULocale l = ULocale.JAPAN;
        SimpleDateFormat sdf = new SimpleDateFormat(mdy, l);
        java.text.SimpleDateFormat jsdf = new java.text.SimpleDateFormat(mdy, l.toLocale());
        assertEquals(jsdf.format(date), sdf.format(date));
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.SimpleDateFormat(String, DateFormatSymbols)'
     */
    public void testSimpleDateFormatStringDateFormatSymbols() {
        Locale l = Locale.US;
        DateFormatSymbols dfs = new DateFormatSymbols(l);
        java.text.DateFormatSymbols jdfs = new java.text.DateFormatSymbols(l);
        SimpleDateFormat sdf = new SimpleDateFormat(mdy, dfs);
        java.text.SimpleDateFormat jsdf = new java.text.SimpleDateFormat(mdy, jdfs);
        assertEquals(jsdf.format(date), sdf.format(date));
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.set2DigitYearStart(Date)'
     */
    public void testSet2DigitYearStart() {
        SimpleDateFormat sdf = new SimpleDateFormat(md2);
        sdf.set2DigitYearStart(date);
        try {
            Date d = sdf.parse("Jan 15 04");
            assertNotEqual(-1, d.toString().indexOf("2104"));
        }
        catch (ParseException pe) {
            fail(pe.getMessage());
        }
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.get2DigitYearStart()'
     */
    public void testGet2DigitYearStart() {
        SimpleDateFormat sdf = new SimpleDateFormat(md2);
        sdf.set2DigitYearStart(date);
        assertEquals(date, sdf.get2DigitYearStart());
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.toPattern()'
     */
    public void testToPattern() {
        SimpleDateFormat sdf = new SimpleDateFormat(mdy);
        assertEquals(mdy, sdf.toPattern());
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.toLocalizedPattern()'
     */
    public void testToLocalizedPattern() {
        Locale l = Locale.getDefault();
        Locale.setDefault(Locale.US);
        SimpleDateFormat sdf = new SimpleDateFormat(mdy);
        assertEquals(mdy, sdf.toLocalizedPattern());
        Locale.setDefault(l);
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.applyPattern(String)'
     */
    public void testApplyPattern() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(tzc);
        sdf.applyPattern(hmzmdy);
        assertEquals(hmzmdyStr, sdf.format(date));
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.applyLocalizedPattern(String)'
     */
    public void testApplyLocalizedPattern() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(tzc);
        sdf.applyLocalizedPattern(hmzmdy);
        assertEquals(hmzmdyStr, sdf.format(date));
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.getDateFormatSymbols()'
     */
    public void testGetDateFormatSymbols() {
        DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
        SimpleDateFormat sdf = new SimpleDateFormat(mdy, dfs);
        assertEquals(dfs, sdf.getDateFormatSymbols());
    }

    /*
     * Test method for 'com.ibm.icu.text.SimpleDateFormat.setDateFormatSymbols(DateFormatSymbols)'
     */
    public void testSetDateFormatSymbols() {
        DateFormatSymbols dfs = new DateFormatSymbols(Locale.JAPAN);
        SimpleDateFormat sdf = new SimpleDateFormat(hmzmdy);
        sdf.setDateFormatSymbols(dfs);
        // assumes Japanese symbols do not have gregorian month names
        assertEquals(-1, sdf.format(date).indexOf("Jan"));
    }
}
