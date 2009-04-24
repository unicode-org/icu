/*
 *******************************************************************************
 * Copyright (C) 2001-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/** 
 * Port From:   ICU4C v1.8.1 : format : DateFormatRegressionTest
 * Source File: $ICU4CRoot/source/test/intltest/dtfmrgts.cpp
 **/

package com.ibm.icu.dev.test.format;

import com.ibm.icu.text.*;
import com.ibm.icu.util.*;

import java.io.*;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;

/** 
 * Performs regression test for DateFormat
 **/
public class DateFormatRegressionTest extends com.ibm.icu.dev.test.TestFmwk {

    public static void main(String[] args) throws Exception{
        new DateFormatRegressionTest().run(args);
    }
    
    /**
     * @bug 4029195
     */
    public void Test4029195() {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        logln("today: " + today);
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
        String pat = sdf.toPattern();
        logln("pattern: " + pat);
        StringBuffer fmtd = new StringBuffer("");
        FieldPosition pos = new FieldPosition(0);
        fmtd = sdf.format(today, fmtd, pos);
        logln("today: " + fmtd);
    
        sdf.applyPattern("G yyyy DDD");
        StringBuffer todayS = new StringBuffer("");
        todayS = sdf.format(today, todayS, pos);
        logln("today: " + todayS);
        try {
            today = sdf.parse(todayS.toString());
            logln("today date: " + today);
        } catch (Exception e) {
            errln("Error reparsing date: " + e.getMessage());
        }
    
        try {
            StringBuffer rt = new StringBuffer("");
            rt = sdf.format(sdf.parse(todayS.toString()), rt, pos);
            logln("round trip: " + rt);
            if (!rt.toString().equals(todayS.toString()))
                errln("Fail: Want " + todayS + " Got " + rt);
        } catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * @bug 4052408
     */
    public void Test4052408() {
    
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.US); 
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(97 + 1900, Calendar.MAY, 3, 8, 55);
        Date dt = cal.getTime();
        String str = fmt.format(dt);
        logln(str);
        
        if (!str.equals("5/3/97 8:55 AM"))
            errln("Fail: Test broken; Want 5/3/97 8:55 AM Got " + str);
    
        String expected[] = {
            "", //"ERA_FIELD",
            "97", //"YEAR_FIELD",
            "5", //"MONTH_FIELD",
            "3", //"DATE_FIELD",
            "", //"HOUR_OF_DAY1_FIELD",
            "", //"HOUR_OF_DAY0_FIELD",
            "55", //"MINUTE_FIELD",
            "", //"SECOND_FIELD",
            "", //"MILLISECOND_FIELD",
            "", //"DAY_OF_WEEK_FIELD",
            "", //"DAY_OF_YEAR_FIELD",
            "", //"DAY_OF_WEEK_IN_MONTH_FIELD",
            "", //"WEEK_OF_YEAR_FIELD",
            "", //"WEEK_OF_MONTH_FIELD",
            "AM", //"AM_PM_FIELD",
            "8", //"HOUR1_FIELD",
            "", //"HOUR0_FIELD",
            "" //"TIMEZONE_FIELD"
            };        
        String fieldNames[] = {
                "ERA_FIELD", 
                "YEAR_FIELD", 
                "MONTH_FIELD", 
                "DATE_FIELD", 
                "HOUR_OF_DAY1_FIELD", 
                "HOUR_OF_DAY0_FIELD", 
                "MINUTE_FIELD", 
                "SECOND_FIELD", 
                "MILLISECOND_FIELD", 
                "DAY_OF_WEEK_FIELD", 
                "DAY_OF_YEAR_FIELD", 
                "DAY_OF_WEEK_IN_MONTH_FIELD", 
                "WEEK_OF_YEAR_FIELD", 
                "WEEK_OF_MONTH_FIELD", 
                "AM_PM_FIELD", 
                "HOUR1_FIELD", 
                "HOUR0_FIELD", 
                "TIMEZONE_FIELD"}; 
    
        boolean pass = true;
        for (int i = 0; i <= 17; ++i) {
            FieldPosition pos = new FieldPosition(i);
            StringBuffer buf = new StringBuffer("");
            fmt.format(dt, buf, pos);
            //char[] dst = new char[pos.getEndIndex() - pos.getBeginIndex()];
            String dst = buf.substring(pos.getBeginIndex(), pos.getEndIndex());
            str = dst;
            log(i + ": " + fieldNames[i] + ", \"" + str + "\", "
                    + pos.getBeginIndex() + ", " + pos.getEndIndex()); 
            String exp = expected[i];
            if ((exp.length() == 0 && str.length() == 0) || str.equals(exp))
                logln(" ok");
            else {
                logln(" expected " + exp);
                pass = false;
            }
        }
        if (!pass)
            errln("Fail: FieldPosition not set right by DateFormat");
    }
    
    /**
     * @bug 4056591
     * Verify the function of the [s|g]et2DigitYearStart() API.
     */
    public void Test4056591() {
    
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyMMdd", Locale.US);
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(1809, Calendar.DECEMBER, 25);
            Date start = cal.getTime();
            fmt.set2DigitYearStart(start);
            if ((fmt.get2DigitYearStart() != start))
                errln("get2DigitYearStart broken");
            cal.clear();
            cal.set(1809, Calendar.DECEMBER, 25);
            Date d1 = cal.getTime();
            cal.clear();
            cal.set(1909, Calendar.DECEMBER, 24);
            Date d2 = cal.getTime();
            cal.clear();
            cal.set(1809, Calendar.DECEMBER, 26);
            Date d3 = cal.getTime();
            cal.clear();
            cal.set(1861, Calendar.DECEMBER, 25);
            Date d4 = cal.getTime();
            
            Date dates[] = {d1, d2, d3, d4};
    
            String strings[] = {"091225", "091224", "091226", "611225"};            
    
            for (int i = 0; i < 4; i++) {
                String s = strings[i];
                Date exp = dates[i];
                Date got = fmt.parse(s);
                logln(s + " . " + got + "; exp " + exp);
                if (got.getTime() != exp.getTime())
                    errln("set2DigitYearStart broken");
            }
        } catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }
    
    /**
     * @bug 4059917
     */
    public void Test4059917() {        
        SimpleDateFormat fmt;
        String myDate;
        fmt = new SimpleDateFormat("yyyy/MM/dd");
        myDate = "1997/01/01";
        aux917( fmt, myDate );        
        fmt = new SimpleDateFormat("yyyyMMdd");
        myDate = "19970101";
        aux917( fmt, myDate );
    }
    
    public void aux917(SimpleDateFormat fmt, String str) {
    
        String pat = fmt.toPattern();
        logln("==================");
        logln("testIt: pattern=" + pat + " string=" + str);
        ParsePosition pos = new ParsePosition(0);
        Object o = fmt.parseObject(str, pos);
        //logln( UnicodeString("Parsed object: ") + o );
    
        StringBuffer formatted = new StringBuffer("");
        FieldPosition poss = new FieldPosition(0);
        formatted = fmt.format(o, formatted, poss);
    
        logln("Formatted string: " + formatted);
        if (!formatted.toString().equals(str))
            errln("Fail: Want " + str + " Got " + formatted);
    }
    
    /**
     * @bug 4060212
     */
    public void Test4060212() {
        String dateString = "1995-040.05:01:29";
        logln("dateString= " + dateString);
        logln("Using yyyy-DDD.hh:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-DDD.hh:mm:ss");
        ParsePosition pos = new ParsePosition(0);
        Date myDate = formatter.parse(dateString, pos);
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.LONG); 
        String myString = fmt.format(myDate);
        logln(myString);
        Calendar cal = new GregorianCalendar();
        cal.setTime(myDate);
        if ((cal.get(Calendar.DAY_OF_YEAR) != 40))
            errln("Fail: Got " + cal.get(Calendar.DAY_OF_YEAR) + " Want 40");
    
        logln("Using yyyy-ddd.hh:mm:ss");
        formatter = new SimpleDateFormat("yyyy-ddd.hh:mm:ss");
        pos.setIndex(0);
        myDate = formatter.parse(dateString, pos);
        myString = fmt.format(myDate);
        logln(myString);
        cal.setTime(myDate);
        if ((cal.get(Calendar.DAY_OF_YEAR) != 40))
            errln("Fail: Got " + cal.get(Calendar.DAY_OF_YEAR) + " Want 40");
    }
    /**
     * @bug 4061287
     */
    public void Test4061287() {
    
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        try {
            logln(df.parse("35/01/1971").toString());
        } catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
        df.setLenient(false);
        boolean ok = false;
        try {
            logln(df.parse("35/01/1971").toString());
        } catch (ParseException e) {
            ok = true;
        }
        if (!ok)
            errln("Fail: Lenient not working");
    }
    
    /**
     * @bug 4065240
     */
    public void Test4065240() {
        Date curDate;
        DateFormat shortdate, fulldate;
        String strShortDate, strFullDate;
        Locale saveLocale = Locale.getDefault();
        TimeZone saveZone = TimeZone.getDefault();
    
        try {
            Locale curLocale = new Locale("de", "DE");
            Locale.setDefault(curLocale);
            // {sfb} adoptDefault instead of setDefault
            //TimeZone.setDefault(TimeZone.createTimeZone("EST"));
            TimeZone.setDefault(TimeZone.getTimeZone("EST"));
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(98 + 1900, 0, 1);
            curDate = cal.getTime();
            shortdate = DateFormat.getDateInstance(DateFormat.SHORT);
            fulldate = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
            strShortDate = "The current date (short form) is ";
            String temp;
            temp = shortdate.format(curDate);
            strShortDate += temp;
            strFullDate = "The current date (long form) is ";
            String temp2 = fulldate.format(curDate);
            strFullDate += temp2;
    
            logln(strShortDate);
            logln(strFullDate);
    
            // {sfb} What to do with resource bundle stuff?????
    
            // Check to see if the resource is present; if not, we can't test
            //ResourceBundle bundle = //The variable is never used
            //    ICULocaleData.getBundle("DateFormatZoneData", curLocale); 
    
            // {sfb} API change to ResourceBundle -- add getLocale()
            /*if (bundle.getLocale().getLanguage().equals("de")) {
                // UPDATE THIS AS ZONE NAME RESOURCE FOR <EST> in de_DE is updated
                if (!strFullDate.endsWith("GMT-05:00"))
                    errln("Fail: Want GMT-05:00");
            } else {
                logln("*** TEST COULD NOT BE COMPLETED BECAUSE DateFormatZoneData ***");
                logln("*** FOR LOCALE de OR de_DE IS MISSING ***");
            }*/
        } catch (Exception e) {
            logln(e.getMessage());
        } finally {
            Locale.setDefault(saveLocale);
            TimeZone.setDefault(saveZone);
        }
    
    }
    
    /*
      DateFormat.equals is too narrowly defined.  As a result, MessageFormat
      does not work correctly.  DateFormat.equals needs to be written so
      that the Calendar sub-object is not compared using Calendar.equals,
      but rather compared for equivalency.  This may necessitate adding a
      (package private) method to Calendar to test for equivalency.
      
      Currently this bug breaks MessageFormat.toPattern
      */
    /**
     * @bug 4071441
     */
    public void Test4071441() {
        DateFormat fmtA = DateFormat.getInstance();
        DateFormat fmtB = DateFormat.getInstance();
    
        // {sfb} Is it OK to cast away const here?
        Calendar calA = fmtA.getCalendar();
        Calendar calB = fmtB.getCalendar();
        calA.clear();
        calA.set(1900, 0 ,0);
        calB.clear();
        calB.set(1900, 0, 0);
        if (!calA.equals(calB))
            errln("Fail: Can't complete test; Calendar instances unequal");
        if (!fmtA.equals(fmtB))
            errln("Fail: DateFormat unequal when Calendars equal");
        calB.clear();
        calB.set(1961, Calendar.DECEMBER, 25);
        if (calA.equals(calB))
            errln("Fail: Can't complete test; Calendar instances equal");
        if (!fmtA.equals(fmtB))
            errln("Fail: DateFormat unequal when Calendars equivalent");
        logln("DateFormat.equals ok");
    }
        
    /* The java.text.DateFormat.parse(String) method expects for the
      US locale a string formatted according to mm/dd/yy and parses it
      correctly.
    
      When given a string mm/dd/yyyy it only parses up to the first
      two y's, typically resulting in a date in the year 1919.
      
      Please extend the parsing method(s) to handle strings with
      four-digit year values (probably also applicable to various
      other locales.  */
    /**
     * @bug 4073003
     */
    public void Test4073003() {
        try {
            DateFormat fmt = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
            String tests[] = {"12/25/61", "12/25/1961", "4/3/2010", "4/3/10"};
            for (int i = 0; i < 4; i += 2) {
                Date d = fmt.parse(tests[i]);
                Date dd = fmt.parse(tests[i + 1]);
                String s;
                s = fmt.format(d);
                String ss;
                ss = fmt.format(dd);
                if (d.getTime() != dd.getTime())
                    errln("Fail: " + d + " != " + dd);
                if (!s.equals(ss))
                    errln("Fail: " + s + " != " + ss);
                logln("Ok: " + s + " " + d);
            }
        } catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }    
    }
    
    /**
     * @bug 4089106
     */
    public void Test4089106() {
        TimeZone def = TimeZone.getDefault();
        try {
            TimeZone z = new SimpleTimeZone((int) (1.25 * 3600000), "FAKEZONE");
            TimeZone.setDefault(z);
            SimpleDateFormat f = new SimpleDateFormat();
            if (!f.getTimeZone().equals(z))
                errln("Fail: SimpleTimeZone should use TimeZone.getDefault()");
        } finally {
            TimeZone.setDefault(def);
        }
    }
    
    /**
     * @bug 4100302
     */
    public void Test4100302() {
        
        Locale locales[] = {
            Locale.CANADA, Locale.CANADA_FRENCH, Locale.CHINA, 
            Locale.CHINESE, Locale.ENGLISH, Locale.FRANCE, Locale.FRENCH, 
            Locale.GERMAN, Locale.GERMANY, Locale.ITALIAN, Locale.ITALY, 
            Locale.JAPAN, Locale.JAPANESE, Locale.KOREA, Locale.KOREAN, 
            Locale.PRC, Locale.SIMPLIFIED_CHINESE, Locale.TAIWAN, 
            Locale.TRADITIONAL_CHINESE, Locale.UK, Locale.US}; 
        try {
            boolean pass = true;
            for (int i = 0; i < 21; i++) {
                Format format = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locales[i]); 
                byte[] bytes;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(format);
                oos.flush();
                baos.close();
                bytes = baos.toByteArray();
                ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
                Object o = ois.readObject();
                if (!format.equals(o)) {
                    pass = false;
                    logln("DateFormat instance for locale " + locales[i] + " is incorrectly serialized/deserialized."); 
                } else {
                    logln("DateFormat instance for locale " + locales[i] + " is OKAY.");
                }
            }
            if (!pass)
                errln("Fail: DateFormat serialization/equality bug");
        } catch (OptionalDataException e) {
            errln("Fail: " + e);
        } catch (IOException e) {
            errln("Fail: " + e);
        } catch (ClassNotFoundException e) {
            errln("Fail: " + e);
        }
    
    }
    
    /**
     * @bug 4101483
     */
    public void Test4101483() {
        SimpleDateFormat sdf = new SimpleDateFormat("z", Locale.US);
        FieldPosition fp = new FieldPosition(DateFormat.TIMEZONE_FIELD);
        Date d = new Date(9234567890L);
        StringBuffer buf = new StringBuffer("");
        sdf.format(d, buf, fp);
        logln(sdf.format(d, buf, fp).toString());
        logln("beginIndex = " + fp.getBeginIndex());
        logln("endIndex = " + fp.getEndIndex());
        if (fp.getBeginIndex() == fp.getEndIndex())
            errln("Fail: Empty field");
    }
    
    /**
     * @bug 4103340
     * @bug 4138203
     * This bug really only works in Locale.US, since that's what the locale
     * used for Date.toString() is.  Bug 4138203 reports that it fails on Korean
     * NT; it would actually have failed on any non-US locale.  Now it should
     * work on all locales.
     */
    public void Test4103340() {
    
        // choose a date that is the FIRST of some month 
        // and some arbitrary time
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1997, 3, 1, 1, 1, 1);
        Date d = cal.getTime(); 
        SimpleDateFormat df = new SimpleDateFormat("MMMM", Locale.US);
        String s = d.toString();
        StringBuffer s2 = new StringBuffer("");
        FieldPosition pos = new FieldPosition(0);
        s2 = df.format(d, s2, pos);
        logln("Date=" + s); 
        logln("DF=" + s2);
        String substr = s2.substring(0,2);
        if (s.indexOf(substr) == -1)
          errln("Months should match");
    }
    
    /**
     * @bug 4103341
     */
    public void Test4103341() {
        TimeZone saveZone = TimeZone.getDefault();
        try {
            // {sfb} changed from adoptDefault to setDefault
            TimeZone.setDefault(TimeZone.getTimeZone("CST"));
            SimpleDateFormat simple = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            TimeZone temp = TimeZone.getDefault();
            if (!simple.getTimeZone().equals(temp))
                errln("Fail: SimpleDateFormat not using default zone");
        } finally {
            TimeZone.setDefault(saveZone);
        }
    }
    
    /**
     * @bug 4104136
     */
    public void Test4104136() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        String pattern = "'time' hh:mm";
        sdf.applyPattern(pattern);
        logln("pattern: \"" + pattern + "\"");
        String strings[] = {"time 10:30", "time 10:x", "time 10x"};
        ParsePosition ppos[] = {new ParsePosition(10), new ParsePosition(0), new ParsePosition(0)};
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1970, Calendar.JANUARY, 1, 10, 30);
        Date dates[] = {cal.getTime(), new Date(-1), new Date(-1)};
        for (int i = 0; i < 3; i++) {
            String text = strings[i];
            ParsePosition finish = ppos[i];
            Date exp = dates[i];
            ParsePosition pos = new ParsePosition(0);
            Date d = sdf.parse(text, pos);
            logln(" text: \"" + text + "\"");
            logln(" index: %d" + pos.getIndex());
            logln(" result: " + d);
            if (pos.getIndex() != finish.getIndex())
                errln("Fail: Expected pos " + finish.getIndex());
            if (!((d == null && exp.equals(new Date(-1))) || (d.equals(exp))))
                errln( "Fail: Expected result " + exp);
        }
    }
    
    /**
     * @bug 4104522
     * CANNOT REPRODUCE
     * According to the bug report, this test should throw a
     * StringIndexOutOfBoundsException during the second parse.  However,
     * this is not seen.
     */
    public void Test4104522() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        String pattern = "'time' hh:mm";
        sdf.applyPattern(pattern);
        logln("pattern: \"" + pattern + "\"");
        // works correctly
        ParsePosition pp = new ParsePosition(0);
        String text = "time ";
        Date dt = sdf.parse(text, pp);
        logln(" text: \"" + text + "\"" + " date: " + dt);
        // works wrong
        pp.setIndex(0);
        text = "time";
        dt = sdf.parse(text, pp);
        logln(" text: \"" + text + "\"" + " date: " + dt);    
    }
    
    /**
     * @bug 4106807
     */
    public void Test4106807() {
        Date dt;
        DateFormat df = DateFormat.getDateTimeInstance();
    
        SimpleDateFormat sdfs[] = {
                new SimpleDateFormat("yyyyMMddHHmmss"), 
                new SimpleDateFormat("yyyyMMddHHmmss'Z'"), 
                new SimpleDateFormat("yyyyMMddHHmmss''"), 
                new SimpleDateFormat("yyyyMMddHHmmss'a''a'"), 
                new SimpleDateFormat("yyyyMMddHHmmss %")}; 
        String strings[] = {
                "19980211140000", 
                "19980211140000", 
                "19980211140000", 
                "19980211140000a", 
                "19980211140000 "}; 
        GregorianCalendar gc = new GregorianCalendar();
        TimeZone timeZone = TimeZone.getDefault();
        TimeZone gmt = (TimeZone) timeZone.clone();
        gmt.setRawOffset(0);
        for (int i = 0; i < 5; i++) {
            SimpleDateFormat format = sdfs[i];
            String dateString = strings[i];
            try {
                format.setTimeZone(gmt);
                dt = format.parse(dateString);
                // {sfb} some of these parses will fail purposely
    
                StringBuffer fmtd = new StringBuffer("");
                FieldPosition pos = new FieldPosition(0);
                fmtd = df.format(dt, fmtd, pos);
                logln(fmtd.toString());
                //logln(df.format(dt)); 
                gc.setTime(dt);
                logln("" + gc.get(Calendar.ZONE_OFFSET));
                StringBuffer s = new StringBuffer("");
                s = format.format(dt, s, pos);
                logln(s.toString());
            } catch (ParseException e) {
                logln("No way Jose");
            }
        }
    }
    
    /*
      Synopsis: Chinese time zone CTT is not recogonized correctly.
      Description: Platform Chinese Windows 95 - ** Time zone set to CST ** 
      */
    /**
     * @bug 4108407
     */
    
    // {sfb} what to do with this one ?? 
    public void Test4108407() {
        /*
    // TODO user.timezone is a protected system property, catch securityexception and warn
    // if this is reenabled
        long l = System.currentTimeMillis(); 
        logln("user.timezone = " + System.getProperty("user.timezone", "?"));
        logln("Time Zone :" + 
                           DateFormat.getDateInstance().getTimeZone().getID()); 
        logln("Default format :" + 
                           DateFormat.getDateInstance().format(new Date(l))); 
        logln("Full format :" + 
                           DateFormat.getDateInstance(DateFormat.FULL).format(new 
                                                                              Date(l))); 
        logln("*** Set host TZ to CST ***");
        logln("*** THE RESULTS OF THIS TEST MUST BE VERIFIED MANUALLY ***");
        */
    }
    
    /**
     * @bug 4134203
     * SimpleDateFormat won't parse "GMT"
     */
    public void Test4134203() {
        String dateFormat = "MM/dd/yy HH:mm:ss zzz";
        SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);
    
        ParsePosition p0 = new ParsePosition(0);
        Date d = fmt.parse("01/22/92 04:52:00 GMT", p0);
        logln(d.toString());
        if(p0.equals(new ParsePosition(0)))
            errln("Fail: failed to parse 'GMT'");
        // In the failure case an exception is thrown by parse();
        // if no exception is thrown, the test passes.
    }
    
    /**
     * @bug 4151631
     * SimpleDateFormat incorrect handling of 2 single quotes in format()
     */
    public void Test4151631() {
        String pattern = 
            "'TO_DATE('''dd'-'MM'-'yyyy HH:mm:ss''' , ''DD-MM-YYYY HH:MI:SS'')'"; 
        logln("pattern=" + pattern);
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        StringBuffer result = new StringBuffer("");
        FieldPosition pos = new FieldPosition(0);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1998, Calendar.JUNE, 30, 13, 30, 0);
        Date d = cal.getTime();
        result = format.format(d, result, pos); 
        if (!result.toString().equals("TO_DATE('30-06-1998 13:30:00' , 'DD-MM-YYYY HH:MI:SS')")) {
            errln("Fail: result=" + result);
        } else {
            logln("Pass: result=" + result);
        }
    }
    
    /**
     * @bug 4151706
     * 'z' at end of date format throws index exception in SimpleDateFormat
     * CANNOT REPRODUCE THIS BUG ON 1.2FCS
     */
    public void Test4151706() {
        String dateString = "Thursday, 31-Dec-98 23:00:00 GMT";
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", Locale.US);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
        cal.clear();
        cal.set(1998, Calendar.DECEMBER, 31, 23, 0, 0);
        Date d = new Date();
        try {
            d = fmt.parse(dateString);
            // {sfb} what about next two lines?
            if (d.getTime() != cal.getTime().getTime())
                errln("Incorrect value: " + d);
        } catch (Exception e) {
            errln("Fail: " + e);
        }
        StringBuffer temp = new StringBuffer("");
        FieldPosition pos = new FieldPosition(0);
        logln(dateString + " . " + fmt.format(d, temp, pos));
    }
    
    /**
     * @bug 4162071
     * Cannot reproduce this bug under 1.2 FCS -- it may be a convoluted duplicate
     * of some other bug that has been fixed.
     */
    public void Test4162071() {
        String dateString = "Thu, 30-Jul-1999 11:51:14 GMT";
        String format = "EEE', 'dd-MMM-yyyy HH:mm:ss z"; // RFC 822/1123
        SimpleDateFormat df = new SimpleDateFormat(format, Locale.US);
        try {
            Date x = df.parse(dateString);
            StringBuffer temp = new StringBuffer("");
            FieldPosition pos = new FieldPosition(0);
            logln(dateString + " -> " + df.format(x, temp, pos));
        } catch (Exception e) {
            errln("Parse format \"" + format + "\" failed.");
        }
    }
    
    /**
     * DateFormat shouldn't parse year "-1" as a two-digit year (e.g., "-1" . 1999).
     */
    public void Test4182066() {
        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yy", Locale.US);
        SimpleDateFormat dispFmt = new SimpleDateFormat("MMM dd yyyy HH:mm:ss GG", Locale.US);
        /* We expect 2-digit year formats to put 2-digit years in the right
         * window.  Out of range years, that is, anything less than "00" or
         * greater than "99", are treated as literal years.  So "1/2/3456"
         * becomes 3456 AD.  Likewise, "1/2/-3" becomes -3 AD == 2 BC.
         */
        final String STRINGS[] = 
            {"02/29/00", "01/23/01", "04/05/-1", "01/23/-9", "11/12/1314", "10/31/1", "09/12/+1", "09/12/001",}; 
        int STRINGS_COUNT = STRINGS.length;
                
        Calendar cal = Calendar.getInstance();
        Date FAIL_DATE = cal.getTime();
        cal.clear();
        cal.set(2000, Calendar.FEBRUARY, 29);
        Date d0 = cal.getTime();
        cal.clear();
        cal.set(2001, Calendar.JANUARY, 23);
        Date d1 = cal.getTime();
        cal.clear();
        cal.set(-1, Calendar.APRIL, 5);
        Date d2 = cal.getTime();
        cal.clear();
        cal.set(-9, Calendar.JANUARY, 23);
        Date d3 = cal.getTime();
        cal.clear();
        cal.set(1314, Calendar.NOVEMBER, 12);
        Date d4 = cal.getTime();
        cal.clear();
        cal.set(1, Calendar.OCTOBER, 31);
        Date d5 = cal.getTime();
        cal.clear();        
        cal.set(1, Calendar.SEPTEMBER, 12);
        Date d7 = cal.getTime();
        Date DATES[] = {d0, d1, d2, d3, d4, d5, FAIL_DATE, d7};
    
        String out = "";
        boolean pass = true;
        for (int i = 0; i < STRINGS_COUNT; ++i) {
            String str = STRINGS[i];
            Date expected = DATES[i];            
            Date actual = null;
            try {
                actual = fmt.parse(str);
            } catch (ParseException e) {
                actual = FAIL_DATE;
            }
            String actStr = "";
            if ((actual.getTime()) == FAIL_DATE.getTime()) {
                actStr += "null";
            } else {
                // Yuck: See j25
                actStr = ((DateFormat) dispFmt).format(actual);
            }
                               
            if (expected.getTime() == (actual.getTime())) {
                out += str + " => " + actStr + "\n";
            } else {
                String expStr = "";
                if (expected.getTime() == FAIL_DATE.getTime()) {
                    expStr += "null";
                } else {
                    // Yuck: See j25
                    expStr = ((DateFormat) dispFmt).format(expected);
                }
                out += "FAIL: " + str + " => " + actStr + ", expected " + expStr + "\n";
                pass = false;
            }
        }
        if (pass) {
            log(out);
        } else {
            err(out);
        }
    }
    
    /**
     * j32 {JDK Bug 4210209 4209272}
     * DateFormat cannot parse Feb 29 2000 when setLenient(false)
     */
    public void Test4210209() {
    
        String pattern = "MMM d, yyyy";
        DateFormat fmt = new SimpleDateFormat(pattern, Locale.US);
        DateFormat disp = new SimpleDateFormat("MMM dd yyyy GG", Locale.US);
    
        Calendar calx = fmt.getCalendar();
        calx.setLenient(false);
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(2000, Calendar.FEBRUARY, 29);
        Date d = calendar.getTime();
        String s = fmt.format(d);
        logln(disp.format(d) + " f> " + pattern + " => \"" + s + "\"");
        ParsePosition pos = new ParsePosition(0);
        d = fmt.parse(s, pos);
        logln("\"" + s + "\" p> " + pattern + " => " +
              (d!=null?disp.format(d):"null"));
        logln("Parse pos = " + pos.getIndex() + ", error pos = " + pos.getErrorIndex());
        if (pos.getErrorIndex() != -1) {
            errln("FAIL: Error index should be -1");
        }
    
        // The underlying bug is in GregorianCalendar.  If the following lines
        // succeed, the bug is fixed.  If the bug isn't fixed, they will throw
        // an exception.
        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.setLenient(false);
        cal.set(2000, Calendar.FEBRUARY, 29); // This should work!
        d = cal.getTime();
        logln("Attempt to set Calendar to Feb 29 2000: " + disp.format(d));
    }
    
    public void Test714() {
        //TimeZone Offset
        TimeZone defaultTZ = TimeZone.getDefault();
        TimeZone PST = TimeZone.getTimeZone("PST");
        int defaultOffset = defaultTZ.getRawOffset();
        int PSTOffset = PST.getRawOffset();
        Date d = new Date(978103543000l - (defaultOffset - PSTOffset));
        d = new Date(d.getTime() - (defaultTZ.inDaylightTime(d) ? 3600000 : 0));
        DateFormat fmt = DateFormat.getDateTimeInstance(-1, DateFormat.MEDIUM, Locale.US);
        String tests = "7:25:43 AM";
        String s = fmt.format(d);
        if (!s.equals(tests)) {
            errln("Fail: " + s + " != " + tests);
        } else {
            logln("OK: " + s + " == " + tests);
        }
    }

    public void Test_GEec() {
        class PatternAndResult {
            private String pattern;
            private String result;
            PatternAndResult(String pat, String res) {
                pattern = pat;
                result = res;
            }
            public String getPattern()  { return pattern; }
            public String getResult()  { return result; }
        }
        final PatternAndResult[] tests = {
            new PatternAndResult( "dd MMM yyyy GGG",   "02 Jul 2008 AD" ),
            new PatternAndResult( "dd MMM yyyy GGGGG", "02 Jul 2008 A" ),
            new PatternAndResult( "e dd MMM yyyy",     "3 02 Jul 2008" ),
            new PatternAndResult( "ee dd MMM yyyy",    "03 02 Jul 2008" ),
            new PatternAndResult( "c dd MMM yyyy",     "3 02 Jul 2008" ),
            new PatternAndResult( "cc dd MMM yyyy",    "3 02 Jul 2008" ),
            new PatternAndResult( "eee dd MMM yyyy",   "Wed 02 Jul 2008" ),
            new PatternAndResult( "EEE dd MMM yyyy",   "Wed 02 Jul 2008" ),
            new PatternAndResult( "EE dd MMM yyyy",    "Wed 02 Jul 2008" ),
            new PatternAndResult( "eeee dd MMM yyyy",  "Wednesday 02 Jul 2008" ),
            new PatternAndResult( "eeeee dd MMM yyyy", "W 02 Jul 2008" ),
            new PatternAndResult( "e ww YYYY",         "3 27 2008" ),
            new PatternAndResult( "c ww YYYY",         "3 27 2008" ),
        };
        ULocale loc = ULocale.ENGLISH;
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar cal = new GregorianCalendar(tz, loc);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MMM-dd", loc);
        for ( int i = 0; i < tests.length; i++ ) {
            PatternAndResult item = tests[i];
            dateFormat.applyPattern( item.getPattern() );
            cal.set(2008, 6, 2, 5, 0); // 2008 July 02 5 AM PDT
            StringBuffer buf = new StringBuffer(32);
            FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
            dateFormat.format(cal, buf, fp);
            if ( buf.toString().compareTo(item.getResult()) != 0 ) {
                errln("for pattern " + item.getPattern() + ", expected " + item.getResult() + ", got " + buf );
            }
            ParsePosition pos = new ParsePosition(0);
            dateFormat.parse( item.getResult(), cal, pos);
            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DATE);
            if ( year != 2008 || month != 6 || day != 2 ) {
            	errln("use pattern " + item.getPattern() + " to parse " + item.getResult() +
            	        ", expected y2008 m6 d2, got " + year + " " + month + " " + day );
            }
        }
    }

    static final char kArabicZero = 0x0660;
    static final char kHindiZero  = 0x0966;
    static final char kLatinZero  = 0x0030;

    public void TestHindiArabicDigits()
    {
        String s;
        char first;
        String what;

        {
            DateFormat df = DateFormat.getInstance(new GregorianCalendar(), new Locale("hi","IN"));
            what = "Gregorian Calendar, hindi";
            s = df.format(new Date(0)); /* 31/12/1969 */
            logln(what + "=" + s);
            first = s.charAt(0);
            if(first<kHindiZero || first>(kHindiZero+9)) {
                errln(what + "- wrong digit,  got " + s + " (integer digit value " + new Integer((int)first).toString());
            }
        }

        {
            DateFormat df = DateFormat.getInstance(new IslamicCalendar(), new Locale("ar","IQ"));
            s = df.format(new Date(0)); /* 21/10/1989 */
            what = "Islamic Calendar, Arabic";
            logln(what + ": " + s);
            first = s.charAt(0);
            if(first<kArabicZero || first>(kArabicZero+9)) {
                errln(what + " wrong digit, got " + s + " (integer digit value " + new Integer((int)first).toString());
            }
        }

        {
            DateFormat df = DateFormat.getInstance(new GregorianCalendar(), new Locale("ar","IQ"));
            s = df.format(new Date(0)); /* 31/12/1969 */
            what = "Gregorian,  ar_IQ, df.getInstance";
            logln(what + ": " + s);
            first = s.charAt(0);
            if(first<kArabicZero || first>(kArabicZero+9)) {
                errln(what + " wrong  digit but got " + s + " (integer digit value " + new Integer((int)first).toString());
            }
        }
        {
            DateFormat df = DateFormat.getInstance(new GregorianCalendar(), new Locale("mt","MT"));
            s = df.format(new Date(0)); /* 31/12/1969 */
            what = "Gregorian,  mt_MT, df.getInstance";
            logln(what + ": " + s);
            first = s.charAt(0);
            if(first<kLatinZero || first>(kLatinZero+9)) {
                errln(what + " wrong  digit but got " + s + " (integer digit value " + new Integer((int)first).toString());
            }
        }

        {
            DateFormat df = DateFormat.getInstance(new IslamicCalendar(), new Locale("ar","IQ"));
            s = df.format(new Date(0)); /* 31/12/1969 */
            what = "Islamic calendar, ar_IQ, df.getInstance";
            logln(what+ ": " + s);
            first = s.charAt(0);
            if(first<kArabicZero || first>(kArabicZero+9)) {
                errln(what + " wrong  digit but got " + s + " (integer digit value " + new Integer((int)first).toString());
            }
        }

        {
            DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("ar","IQ"));
            s = df.format(new Date(0)); /* 31/12/1969 */
            what = "ar_IQ, getDateTimeInstance";
            logln(what+ ": " + s);
            first = s.charAt(0);
            if(first<kArabicZero || first>(kArabicZero+9)) {
                errln(what + " wrong  digit but got " + s + " (integer digit value " + new Integer((int)first).toString());
            }
        }

        {
            DateFormat df = DateFormat.getInstance(new JapaneseCalendar(), new Locale("ar","IQ"));
            s = df.format(new Date(0)); /* 31/12/1969 */
            what = "ar_IQ, Japanese Calendar, getInstance";
            logln(what+ ": " + s);
            // Note: The default date pattern for Japanese calendar starts with era in CLDR 1.7
            char last = s.charAt(s.length() - 1);
            if(last<kArabicZero || last>(kArabicZero+9)) {
                errln(what + " wrong  digit but got " + s + " (integer digit value " + new Integer((int)last).toString());
            }
        }
    }

    // Ticket#5683
    // Some ICU4J 3.6 data files contain garbage data which prevent the code to resolve another
    // bundle as an alias.  zh_TW should be equivalent to zh_Hant_TW
    public void TestT5683() {
        Locale[] aliasLocales = {
            new Locale("zh", "CN"),
            new Locale("zh", "TW"),
            new Locale("zh", "HK"),
            new Locale("zh", "SG"),
            new Locale("zh", "MO")
        };

        ULocale[] canonicalLocales = {
            new ULocale("zh_Hans_CN"),
            new ULocale("zh_Hant_TW"),
            new ULocale("zh_Hant_HK"),
            new ULocale("zh_Hans_SG"),
            new ULocale("zh_Hant_MO")
        };

        Date d = new Date(0);

        for (int i = 0; i < aliasLocales.length; i++) {
            DateFormat dfAlias = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, aliasLocales[i]);
            DateFormat dfCanonical = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, canonicalLocales[i]);

            String sAlias = dfAlias.format(d);
            String sCanonical = dfCanonical.format(d);

            if (!sAlias.equals(sCanonical)) {
                errln("Fail: The format result for locale " + aliasLocales[i] + " is different from the result for locale " + canonicalLocales[i]
                        + ": " + sAlias + "[" + aliasLocales[i] + "] / " + sCanonical + "[" + canonicalLocales[i] + "]");
            }
        }
    }
    
    
    public void Test5006GetShortMonths() throws Exception {

        // Currently supported NLV locales
        Locale ENGLISH = new Locale("en", "US"); // We don't support 'en' alone
        Locale ARABIC = new Locale("ar", "");
        Locale CZECH = new Locale("cs", "");
        Locale GERMAN = new Locale("de", "");
        Locale GREEK = new Locale("el", "");
        Locale SPANISH = new Locale("es", "");
        Locale FRENCH = new Locale("fr", "");
        Locale HUNGARIAN = new Locale("hu", "");
        Locale ITALIAN = new Locale("it", "");
        Locale HEBREW = new Locale("iw", "");
        Locale JAPANESE = new Locale("ja", "");
        Locale KOREAN = new Locale("ko", "");
        Locale POLISH = new Locale("pl", "");
        Locale PORTUGUESE = new Locale("pt", "BR");
        Locale RUSSIAN = new Locale("ru", "");
        Locale TURKISH = new Locale("tr", "");
        Locale CHINESE_SIMPLIFIED = new Locale("zh", "CN");
        Locale CHINESE_TRADITIONAL = new Locale("zh", "TW");

        Locale[] locales = new Locale[] { ENGLISH, ARABIC, CZECH, GERMAN, GREEK, SPANISH, FRENCH,
                HUNGARIAN, ITALIAN, HEBREW, JAPANESE, KOREAN, POLISH, PORTUGUESE, RUSSIAN, TURKISH,
                CHINESE_SIMPLIFIED, CHINESE_TRADITIONAL };

        String[] islamicTwelfthMonthLocalized = new String[locales.length];
        String[] gregorianTwelfthMonthLocalized = new String[locales.length];

        for (int i = 0; i < locales.length; i++) {

            Locale locale = locales[i];

            // Islamic
            com.ibm.icu.util.Calendar islamicCalendar = new com.ibm.icu.util.IslamicCalendar(locale);
            com.ibm.icu.text.SimpleDateFormat islamicDateFormat = (com.ibm.icu.text.SimpleDateFormat) islamicCalendar
                    .getDateTimeFormat(com.ibm.icu.text.DateFormat.FULL, -1, locale);
            com.ibm.icu.text.DateFormatSymbols islamicDateFormatSymbols = islamicDateFormat
                    .getDateFormatSymbols();

            String[] shortMonths = islamicDateFormatSymbols.getShortMonths();
            String twelfthMonthLocalized = shortMonths[11];

            islamicTwelfthMonthLocalized[i] = twelfthMonthLocalized;

            // Gregorian
            com.ibm.icu.util.Calendar gregorianCalendar = new com.ibm.icu.util.GregorianCalendar(
                    locale);
            com.ibm.icu.text.SimpleDateFormat gregorianDateFormat = (com.ibm.icu.text.SimpleDateFormat) gregorianCalendar
                    .getDateTimeFormat(com.ibm.icu.text.DateFormat.FULL, -1, locale);

            com.ibm.icu.text.DateFormatSymbols gregorianDateFormatSymbols = gregorianDateFormat
                    .getDateFormatSymbols();
            shortMonths = gregorianDateFormatSymbols.getShortMonths();
            twelfthMonthLocalized = shortMonths[11];

            gregorianTwelfthMonthLocalized[i] = twelfthMonthLocalized;

        }

        // Compare
        for (int i = 0; i < locales.length; i++) {

            String gregorianTwelfthMonth = gregorianTwelfthMonthLocalized[i];
            String islamicTwelfthMonth = islamicTwelfthMonthLocalized[i];

            logln(locales[i] + ": " + gregorianTwelfthMonth + ", " + islamicTwelfthMonth);
            if (gregorianTwelfthMonth.equalsIgnoreCase(islamicTwelfthMonth)) {
                errln(locales[i] + ": gregorian and islamic are same: " + gregorianTwelfthMonth
                        + ", " + islamicTwelfthMonth);
            }
        }
    }
}
