/*
 *******************************************************************************
 * Copyright (C) 2001-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/** 
 * Port From:   ICU4C v1.8.1 : format : DateFormatTest
 * Source File: $ICU4CRoot/source/test/intltest/dtfmttst.cpp
 **/

package com.ibm.icu.dev.test.format;

import com.ibm.icu.text.*;
import com.ibm.icu.util.*;
import com.ibm.icu.impl.*;
import java.util.Date;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Locale;
import java.text.FieldPosition;
import java.util.ResourceBundle;

public class DateFormatTest extends com.ibm.icu.dev.test.TestFmwk {
    
    public static void main(String[] args) throws Exception {
        new DateFormatTest().run(args);
    }
    
    // Test written by Wally Wedel and emailed to me.
    public void TestWallyWedel() {
        /*
         * Instantiate a TimeZone so we can get the ids.
         */
        //TimeZone tz = new SimpleTimeZone(7, ""); //The variable is never used
        /*
         * Computational variables.
         */
        int offset, hours, minutes;
        /*
         * Instantiate a SimpleDateFormat set up to produce a full time
         zone name.
         */
        SimpleDateFormat sdf = new SimpleDateFormat("zzzz");
        /*
         * A String array for the time zone ids.
         */
    
        final String[] ids = TimeZone.getAvailableIDs();
        int ids_length = ids.length; //when fixed the bug should comment it out
    
        /*
         * How many ids do we have?
         */
        logln("Time Zone IDs size:" + ids_length);
        /*
         * Column headings (sort of)
         */
        logln("Ordinal ID offset(h:m) name");
        /*
         * Loop through the tzs.
         */
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < ids_length; i++) {
            logln(i + " " + ids[i]);
            TimeZone ttz = TimeZone.getTimeZone(ids[i]);
            // offset = ttz.getRawOffset();
            cal.setTimeZone(ttz);
            cal.setTime(today);
            offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
            // logln(i + " " + ids[i] + " offset " + offset);
            String sign = "+";
            if (offset < 0) {
                sign = "-";
                offset = -offset;
            }
            hours = offset / 3600000;
            minutes = (offset % 3600000) / 60000;
            String dstOffset = sign + (hours < 10 ? "0" : "") + hours
                    + ":" + (minutes < 10 ? "0" : "") + minutes; 
            /*
             * Instantiate a date so we can display the time zone name.
             */
            sdf.setTimeZone(ttz);
            /*
             * Format the output.
             */
            StringBuffer fmtOffset = new StringBuffer("");
            FieldPosition pos = new FieldPosition(0);
            
            try {
                fmtOffset = sdf.format(today, fmtOffset, pos);
            } catch (Exception e) {            
                logln("Exception:" + e);
                continue;
            }
            // UnicodeString fmtOffset = tzS.toString();
            String fmtDstOffset = null;
            if (fmtOffset.toString().startsWith("GMT")) {
                //fmtDstOffset = fmtOffset.substring(3);
                fmtDstOffset = fmtOffset.substring(3, fmtOffset.length());
            }
            /*
             * Show our result.
             */
    
            boolean ok = fmtDstOffset == null || fmtDstOffset.equals("") || fmtDstOffset.equals(dstOffset);
            if (ok) {
                logln(i + " " + ids[i] + " " + dstOffset + " "
                      + fmtOffset + (fmtDstOffset != null ? " ok" : " ?")); 
            } else {
                errln(i + " " + ids[i] + " " + dstOffset + " " + fmtOffset + " *** FAIL ***");
            }
        
        }
    }
    
    public void TestEquals() {
        DateFormat fmtA = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL); 
        DateFormat fmtB = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL); 
        if (!fmtA.equals(fmtB))
            errln("FAIL");    
    }
    
    /**
     * Test the parsing of 2-digit years.
     */
    public void TestTwoDigitYearDSTParse() {
    
        SimpleDateFormat fullFmt = new SimpleDateFormat("EEE MMM dd HH:mm:ss.SSS zzz yyyy G"); 
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yy h:mm:ss 'o''clock' a z", Locale.ENGLISH); 
        String s = "03-Apr-04 2:20:47 o'clock AM PST";
    
        /*
         * SimpleDateFormat(pattern, locale) Construct a SimpleDateDateFormat using
         * the given pattern, the locale and using the TimeZone.getDefault();
         * So it need to add the timezone offset on hour field. 
         * ps. the Method Calendar.getTime() used by SimpleDateFormat.parse() always 
         * return Date value with TimeZone.getDefault() [Richard/GCL]
         */
        
        TimeZone defaultTZ = TimeZone.getDefault();
        TimeZone PST = TimeZone.getTimeZone("PST");
        int defaultOffset = defaultTZ.getRawOffset();
        int PSTOffset = PST.getRawOffset();
        int hour = 2 + (defaultOffset - PSTOffset) / (60*60*1000);
        // hour is the expected hour of day, in units of seconds
        hour = ((hour < 0) ? hour + 24 : hour) * 60*60;
        try {
            Date d = fmt.parse(s);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            //DSTOffset
            hour += defaultTZ.inDaylightTime(d) ? 1 : 0;
            
            logln(s + " P> " + ((DateFormat) fullFmt).format(d));
            // hr is the actual hour of day, in units of seconds
            // adjust for DST
            int hr = cal.get(Calendar.HOUR_OF_DAY) * 60*60 -
                cal.get(Calendar.DST_OFFSET) / 1000;
            if (hr != hour)
                errln("FAIL: Hour (-DST) = " + hr / (60*60.0)+
                      "; expected " + hour / (60*60.0));
        } catch (ParseException e) {
            errln("Parse Error:" + e.getMessage());
        }
    
    }
    
    /**
     * Verify that returned field position indices are correct.
     */
    public void TestFieldPosition() {
        int i, j, exp;
        StringBuffer buf = new StringBuffer();

        // Verify data
        DateFormatSymbols rootSyms = new DateFormatSymbols(new Locale("", "", ""));
        assertEquals("patternChars", PATTERN_CHARS, rootSyms.getLocalPatternChars());
        assertTrue("DATEFORMAT_FIELD_NAMES", DATEFORMAT_FIELD_NAMES.length == DateFormat.FIELD_COUNT);
        assertTrue("Data", DateFormat.FIELD_COUNT == PATTERN_CHARS.length());

        // Create test formatters
        final int COUNT = 4;
        DateFormat[] dateFormats = new DateFormat[COUNT];
        dateFormats[0] = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
        dateFormats[1] = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.FRANCE);
        // Make the pattern "G y M d..."
        buf.append(PATTERN_CHARS);
        for (j=buf.length()-1; j>=0; --j) buf.insert(j, ' ');
        dateFormats[2] = new SimpleDateFormat(buf.toString(), Locale.US);
        // Make the pattern "GGGG yyyy MMMM dddd..."
        for (j=buf.length()-1; j>=0; j-=2) {
            for (i=0; i<3; ++i) {
                buf.insert(j, buf.charAt(j));
            }
        }
        dateFormats[3] = new SimpleDateFormat(buf.toString(), Locale.US);

        Date aug13 = new Date((long) 871508052513.0);

        // Expected output field values for above DateFormats on aug13
        // Fields are given in order of DateFormat field number
        final String EXPECTED[] = {
            "", "1997", "August", "13", "", "", "34", "12", "",
            "Wednesday", "", "", "", "", "PM", "2", "", "PDT", "", "", "", "", "", "",

            "", "1997", "ao\u00FBt", "13", "", "14", "34", "", "",
            "mercredi", "", "", "", "", "", "", "", "HAP (\u00C9UA)", "", "", "", "", "", "",

            "AD", "1997", "8", "13", "14", "14", "34", "12", "5",
            "Wed", "225", "2", "33", "3", "PM", "2", "2", "PDT", "1997", "4", "1997", "2450674", "52452513", "-0700",

            "AD", "1997", "August", "0013", "0014", "0014", "0034", "0012", "5130",
            "Wednesday", "0225", "0002", "0033", "0003", "PM", "0002", "0002", "Pacific Daylight Time", "1997", "0004", "1997", "2450674", "52452513", "-0700",
        };

        assertTrue("data size", EXPECTED.length == COUNT * DateFormat.FIELD_COUNT);

        TimeZone PT = TimeZone.getTimeZone("America/Los_Angeles");
        for (j = 0, exp = 0; j < COUNT; ++j) {
          //  String str;
            DateFormat df = dateFormats[j];
            df.setTimeZone(PT);
            logln(" Pattern = " + ((SimpleDateFormat) df).toPattern());
            try {
                logln("  Result = " + df.format(aug13));
            } catch (Exception e) {
                errln("FAIL: " + e);
                e.printStackTrace();
                continue;
            }

            for (i = 0; i < DateFormat.FIELD_COUNT; ++i, ++exp) {
                FieldPosition pos = new FieldPosition(i);
                buf.setLength(0);
                df.format(aug13, buf, pos);    
                String field = buf.substring(pos.getBeginIndex(), pos.getEndIndex());
                assertEquals("field #" + i + " " + DATEFORMAT_FIELD_NAMES[i],
                             EXPECTED[exp], field);
            }
        }
    }

    /**
     * This MUST be kept in sync with DateFormatSymbols.patternChars.
     */
    static final String PATTERN_CHARS = "GyMdkHmsSEDFwWahKzYeugAZ";
        
    /**
     * A list of the names of all the fields in DateFormat.
     * This MUST be kept in sync with DateFormat.
     */
    static final String DATEFORMAT_FIELD_NAMES[] = {
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
        "TIMEZONE_FIELD",
        "YEAR_WOY_FIELD",
        "DOW_LOCAL_FIELD",
        "EXTENDED_YEAR_FIELD",
        "JULIAN_DAY_FIELD",
        "MILLISECONDS_IN_DAY_FIELD",
        "TIMEZONE_RFC_FIELD"
    };
    
    /**
     * General parse/format tests.  Add test cases as needed.
     */
    public void TestGeneral() {
        String DATA[] = {
            "yyyy MM dd HH:mm:ss.SSS",

            // Milliseconds are left-justified, since they format as fractions of a second
            // Both format and parse should round HALF_UP
            "y/M/d H:mm:ss.S", "fp", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.6", "2004 03 10 16:36:31.600",
            "y/M/d H:mm:ss.SS", "fp", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.57", "2004 03 10 16:36:31.570",
            "y/M/d H:mm:ss.SSS", "F", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.567",
            "y/M/d H:mm:ss.SSSS", "pf", "2004/3/10 16:36:31.5679", "2004 03 10 16:36:31.568", "2004/3/10 16:36:31.5680",
        };
        expect(DATA, new Locale("en", "", ""));
    }

    /**
     * Verify that strings which contain incomplete specifications are parsed
     * correctly.  In some instances, this means not being parsed at all, and
     * returning an appropriate error.
     */
    public void TestPartialParse994() {
    
        SimpleDateFormat f = new SimpleDateFormat();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1997, 1 - 1, 17, 10, 11, 42);
        Date date = null;
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10:11:42", cal.getTime());
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10:", date);
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10", date);
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 ", date);
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17", date);
    }
    
    // internal test subroutine, used by TestPartialParse994
    public void tryPat994(SimpleDateFormat format, String pat, String str, Date expected) {
        Date Null = null;
        logln("Pattern \"" + pat + "\"   String \"" + str + "\"");
        try {
            format.applyPattern(pat);
            Date date = format.parse(str);    
            String f = ((DateFormat) format).format(date);
            logln(" parse(" + str + ") -> " + date);
            logln(" format -> " + f);
            if (expected.equals(Null) || !date.equals(expected))
                errln("FAIL: Expected null"); //" + expected);
            if (!f.equals(str))
                errln("FAIL: Expected " + str);
        } catch (ParseException e) {
            logln("ParseException: " + e.getMessage());
            if (!(expected ==Null))
                errln("FAIL: Expected " + expected);
        } catch (Exception e) {
            errln("*** Exception:");
            e.printStackTrace();
        }
    }
    
    /**
     * Verify the behavior of patterns in which digits for different fields run together
     * without intervening separators.
     */
    public void TestRunTogetherPattern985() {
        String format = "yyyyMMddHHmmssSSS";
        String now, then;
        //UBool flag;
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        Date date1 = new Date();
        now = ((DateFormat) formatter).format(date1);
        logln(now);
        ParsePosition pos = new ParsePosition(0);
        Date date2 = formatter.parse(now, pos);
        if (date2 == null)
            then = "Parse stopped at " + pos.getIndex();
        else
            then = ((DateFormat) formatter).format(date2);
        logln(then);
        if (date2 == null || !date2.equals(date1))
            errln("FAIL");
    }

    /**
     * Verify the behavior of patterns in which digits for different fields run together
     * without intervening separators.
     */
    public void TestRunTogetherPattern917() {
        SimpleDateFormat fmt;
        String myDate;
        fmt = new SimpleDateFormat("yyyy/MM/dd");
        myDate = "1997/02/03";
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1997, 2 - 1, 3);
        _testIt917(fmt, myDate, cal.getTime());
        fmt = new SimpleDateFormat("yyyyMMdd");
        myDate = "19970304";
        cal.clear();
        cal.set(1997, 3 - 1, 4);
        _testIt917(fmt, myDate, cal.getTime());
    
    }
    
    // internal test subroutine, used by TestRunTogetherPattern917
    public void _testIt917(SimpleDateFormat fmt, String str, Date expected) {
        logln("pattern=" + fmt.toPattern() + "   string=" + str);
        Date o = new Date();
        o = (Date) ((DateFormat) fmt).parseObject(str, new ParsePosition(0));
        logln("Parsed object: " + o);
        if (o == null || !o.equals(expected))
            errln("FAIL: Expected " + expected);
        String formatted = o==null? "null" : ((DateFormat) fmt).format(o);
        logln( "Formatted string: " + formatted);
        if (!formatted.equals(str))
            errln( "FAIL: Expected " + str);
    }
    
    /**
     * Verify the handling of Czech June and July, which have the unique attribute that
     * one is a proper prefix substring of the other.
     */
    public void TestCzechMonths459() {
        DateFormat fmt = DateFormat.getDateInstance(DateFormat.FULL, new Locale("cs", "", "")); 
        logln("Pattern " + ((SimpleDateFormat) fmt).toPattern());
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1997, Calendar.JUNE, 15);
        Date june = cal.getTime();
        cal.clear();
        cal.set(1997, Calendar.JULY, 15);
        Date july = cal.getTime();
        String juneStr = fmt.format(june);
        String julyStr = fmt.format(july);
        try {
            logln("format(June 15 1997) = " + juneStr);
            Date d = fmt.parse(juneStr);
            String s = fmt.format(d);
            int month, yr, day, hr, min, sec;
            cal.setTime(d);
            yr = cal.get(Calendar.YEAR) - 1900;
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_WEEK);
            hr = cal.get(Calendar.HOUR_OF_DAY);
            min = cal.get(Calendar.MINUTE);
            sec = cal.get(Calendar.SECOND);
            logln("  . parse . " + s + " (month = " + month + ")");
            if (month != Calendar.JUNE)
                errln("FAIL: Month should be June");
            logln("format(July 15 1997) = " + julyStr);
            d = fmt.parse(julyStr);
            s = fmt.format(d);
            cal.setTime(d);
            yr = cal.get(Calendar.YEAR) - 1900;
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_WEEK);
            hr = cal.get(Calendar.HOUR_OF_DAY);
            min = cal.get(Calendar.MINUTE);
            sec = cal.get(Calendar.SECOND);
            logln("  . parse . " + s + " (month = " + month + ")");
            if (month != Calendar.JULY)
                errln("FAIL: Month should be July");
        } catch (ParseException e) {
            errln(e.getMessage());
        }
    }
    
    /**
     * Test the handling of 'D' in patterns.
     */
    public void TestLetterDPattern212() {
        String dateString = "1995-040.05:01:29";
        String bigD = "yyyy-DDD.hh:mm:ss";
        String littleD = "yyyy-ddd.hh:mm:ss";
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1995, 0, 1, 5, 1, 29);
        Date expLittleD = cal.getTime();
        Date expBigD = new Date((long) (expLittleD.getTime() + 39 * 24 * 3600000.0));
        expLittleD = expBigD; // Expect the same, with default lenient parsing
        logln("dateString= " + dateString);
        SimpleDateFormat formatter = new SimpleDateFormat(bigD);
        ParsePosition pos = new ParsePosition(0);
        Date myDate = formatter.parse(dateString, pos);
        logln("Using " + bigD + " . " + myDate);
        if (!myDate.equals(expBigD))
            errln("FAIL: Expected " + expBigD);
        formatter = new SimpleDateFormat(littleD);
        pos = new ParsePosition(0);
        myDate = formatter.parse(dateString, pos);
        logln("Using " + littleD + " . " + myDate);
        if (!myDate.equals(expLittleD))
            errln("FAIL: Expected " + expLittleD);
    }
    
    /**
     * Test the day of year pattern.
     */
    public void TestDayOfYearPattern195() {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        int year,month,day; 
        year = cal.get(Calendar.YEAR);
        month = cal.get(Calendar.MONTH);
        day = cal.get(Calendar.DAY_OF_MONTH);
        cal.clear();
        cal.set(year, month, day);
        Date expected = cal.getTime();
        logln("Test Date: " + today);
        SimpleDateFormat sdf = (SimpleDateFormat)DateFormat.getDateInstance();
        tryPattern(sdf, today, null, expected);
        tryPattern(sdf, today, "G yyyy DDD", expected);
    }
    
    // interl test subroutine, used by TestDayOfYearPattern195
    public void tryPattern(SimpleDateFormat sdf, Date d, String pattern, Date expected) {
        if (pattern != null)
            sdf.applyPattern(pattern);
        logln("pattern: " + sdf.toPattern());
        String formatResult = ((DateFormat) sdf).format(d);
        logln(" format -> " + formatResult);
        try {
            Date d2 = sdf.parse(formatResult);
            logln(" parse(" + formatResult + ") -> " + d2);
            if (!d2.equals(expected))
                errln("FAIL: Expected " + expected);
            String format2 = ((DateFormat) sdf).format(d2);
            logln(" format -> " + format2);
            if (!formatResult.equals(format2))
                errln("FAIL: Round trip drift");
        } catch (Exception e) {
            errln(e.getMessage());
        }
    }
    
    /**
     * Test the handling of single quotes in patterns.
     */
    public void TestQuotePattern161() {
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy 'at' hh:mm:ss a zzz", Locale.US); 
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1997, Calendar.AUGUST, 13, 10, 42, 28);
        Date currentTime_1 = cal.getTime();
        String dateString = ((DateFormat) formatter).format(currentTime_1);
        String exp = "08/13/1997 at 10:42:28 AM ";
        logln("format(" + currentTime_1 + ") = " + dateString);
        if (!dateString.substring(0, exp.length()).equals(exp))
            errln("FAIL: Expected " + exp);
    
    }
        
    /**
     * Verify the correct behavior when handling invalid input strings.
     */
    public void TestBadInput135() {
        int looks[] = {DateFormat.SHORT, DateFormat.MEDIUM, DateFormat.LONG, DateFormat.FULL}; 
        int looks_length = looks.length;
        final String[] strings = {"Mar 15", "Mar 15 1997", "asdf", "3/1/97 1:23:", "3/1/00 1:23:45 AM"}; 
        int strings_length = strings.length;
        DateFormat full = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.US); 
        String expected = "March 1, 2000 1:23:45 AM ";
        for (int i = 0; i < strings_length; ++i) {
            final String text = strings[i];
            for (int j = 0; j < looks_length; ++j) {
                int dateLook = looks[j];
                for (int k = 0; k < looks_length; ++k) {
                    int timeLook = looks[k];
                    DateFormat df = DateFormat.getDateTimeInstance(dateLook, timeLook, Locale.US); 
                    String prefix = text + ", " + dateLook + "/" + timeLook + ": "; 
                    try {
                        Date when = df.parse(text);
                        if (when == null) {
                            errln(prefix + "SHOULD NOT HAPPEN: parse returned null.");
                            continue;
                        }  
                        if (when != null) {
                            String format;
                            format = full.format(when);
                            logln(prefix + "OK: " + format);
                            if (!format.substring(0, expected.length()).equals(expected))
                                errln("FAIL: Expected " + expected);
                        }
                    } catch(java.text.ParseException e) {
                        logln(e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Verify the correct behavior when parsing an array of inputs against an
     * array of patterns, with known results.  The results are encoded after
     * the input strings in each row.
     */
    public void TestBadInput135a() {
    
        SimpleDateFormat dateParse = new SimpleDateFormat("", Locale.US);
        final String ss;
        Date date;
        String[] parseFormats ={"MMMM d, yyyy", "MMMM d yyyy", "M/d/yy",
                                "d MMMM, yyyy", "d MMMM yyyy",  "d MMMM",
                                "MMMM d", "yyyy", "h:mm a MMMM d, yyyy" };
        String[] inputStrings = {
            "bogus string", null, null, null, null, null, null, null, null, null,
                "April 1, 1997", "April 1, 1997", null, null, null, null, null, "April 1", null, null,
                "Jan 1, 1970", "January 1, 1970", null, null, null, null, null, "January 1", null, null,
                "Jan 1 2037", null, "January 1 2037", null, null, null, null, "January 1", null, null,
                "1/1/70", null, null, "1/1/70", null, null, null, null, "0001", null,
                "5 May 1997", null, null, null, null, "5 May 1997", "5 May", null, "0005", null,
                "16 May", null, null, null, null, null, "16 May", null, "0016", null,
                "April 30", null, null, null, null, null, null, "April 30", null, null,
                "1998", null, null, null, null, null, null, null, "1998", null,
                "1", null, null, null, null, null, null, null, "0001", null,
                "3:00 pm Jan 1, 1997", null, null, null, null, null, null, null, "0003", "3:00 PM January 1, 1997",
                };
        final int PF_LENGTH = parseFormats.length;
        final int INPUT_LENGTH = inputStrings.length;
    
        dateParse.applyPattern("d MMMM, yyyy");
        dateParse.setTimeZone(TimeZone.getDefault());
        ss = "not parseable";
        //    String thePat;
        logln("Trying to parse \"" + ss + "\" with " + dateParse.toPattern());
        try {
            date = dateParse.parse(ss);
        } catch (Exception ex) {
            logln("FAIL:" + ex);
        }
        for (int i = 0; i < INPUT_LENGTH; i += (PF_LENGTH + 1)) {
            ParsePosition parsePosition = new ParsePosition(0);
            String s = inputStrings[i];
            for (int index = 0; index < PF_LENGTH; ++index) {
                final String expected = inputStrings[i + 1 + index];
                dateParse.applyPattern(parseFormats[index]);
                dateParse.setTimeZone(TimeZone.getDefault());
                try {
                    parsePosition.setIndex(0);
                    date = dateParse.parse(s, parsePosition);
                    if (parsePosition.getIndex() != 0) {
                        String s1, s2;
                        s1 = s.substring(0, parsePosition.getIndex());
                        s2 = s.substring(parsePosition.getIndex(), s.length());
                        if (date == null) {
                            errln("ERROR: null result fmt=\"" + parseFormats[index]
                                    + "\" pos=" + parsePosition.getIndex()
                                    + " " + s1 + "|" + s2);
                        } else {
                            String result = ((DateFormat) dateParse).format(date);
                            logln("Parsed \"" + s + "\" using \"" + dateParse.toPattern() + "\" to: " + result);
                            if (expected == null)
                                errln("FAIL: Expected parse failure");
                            else
                                if (!result.equals(expected))
                                    errln("FAIL: Expected " + expected);
                        }
                    } else
                        if (expected != null) {
                            errln("FAIL: Expected " + expected + " from \"" + s
                                    + "\" with \"" + dateParse.toPattern()+ "\"");
                        }
                } catch (Exception ex) {
                    logln("FAIL:" + ex);
                }
            }
        }
    
    }
    
    /**
     * Test the parsing of two-digit years.
     */
    public void TestTwoDigitYear() {
        DateFormat fmt = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(117 + 1900, Calendar.JUNE, 5);
        parse2DigitYear(fmt, "6/5/17", cal.getTime());
        cal.clear();
        cal.set(34 + 1900, Calendar.JUNE, 4);
        parse2DigitYear(fmt, "6/4/34", cal.getTime());
    }
    
    // internal test subroutine, used by TestTwoDigitYear
    public void parse2DigitYear(DateFormat fmt, String str, Date expected) {
        try {
            Date d = fmt.parse(str);
            logln("Parsing \""+ str+ "\" with "+ ((SimpleDateFormat) fmt).toPattern()
                    + "  => "+ d); 
            if (!d.equals(expected))
                errln( "FAIL: Expected " + expected);
        } catch (ParseException e) {
            errln(e.getMessage());
        }
    }
    
    /**
     * Test the formatting of time zones.
     */
    public void TestDateFormatZone061() {
        Date date;
        DateFormat formatter;
        date = new Date(859248000000l);
        logln("Date 1997/3/25 00:00 GMT: " + date);
        formatter = new SimpleDateFormat("dd-MMM-yyyyy HH:mm", Locale.UK);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        String temp = formatter.format(date);
        logln("Formatted in GMT to: " + temp);
        try {
            Date tempDate = formatter.parse(temp);
            logln("Parsed to: " + tempDate);
            if (!tempDate.equals(date))
                errln("FAIL: Expected " + date);
        } catch (Throwable t) {
            System.out.println(t);
        }
    
    }
    
    /**
     * Test the formatting of time zones.
     */
    public void TestDateFormatZone146() {
        TimeZone saveDefault = TimeZone.getDefault();
    
        //try {
        TimeZone thedefault = TimeZone.getTimeZone("GMT");
        TimeZone.setDefault(thedefault);
        // java.util.Locale.setDefault(new java.util.Locale("ar", "", ""));
    
        // check to be sure... its GMT all right
        TimeZone testdefault = TimeZone.getDefault();
        String testtimezone = testdefault.getID();
        if (testtimezone.equals("GMT"))
            logln("Test timezone = " + testtimezone);
        else
            errln("Test timezone should be GMT, not " + testtimezone);
    
        // now try to use the default GMT time zone
        GregorianCalendar greenwichcalendar = new GregorianCalendar(1997, 3, 4, 23, 0);
        //*****************************greenwichcalendar.setTimeZone(TimeZone.getDefault());
        //greenwichcalendar.set(1997, 3, 4, 23, 0);
        // try anything to set hour to 23:00 !!!
        greenwichcalendar.set(Calendar.HOUR_OF_DAY, 23);
        // get time
        Date greenwichdate = greenwichcalendar.getTime();
        // format every way
        String DATA[] = {
                "simple format:  ", "04/04/97 23:00 GMT", 
                "MM/dd/yy HH:mm z", "full format:    ", 
                "Friday, April 4, 1997 11:00:00 o'clock PM GMT", 
                "EEEE, MMMM d, yyyy h:mm:ss 'o''clock' a z", 
                "long format:    ", "April 4, 1997 11:00:00 PM GMT", 
                "MMMM d, yyyy h:mm:ss a z", "default format: ", 
                "04-Apr-97 11:00:00 PM", "dd-MMM-yy h:mm:ss a", 
                "short format:   ", "4/4/97 11:00 PM", 
                "M/d/yy h:mm a"}; 
        int DATA_length = DATA.length;
    
        for (int i = 0; i < DATA_length; i += 3) {
            DateFormat fmt = new SimpleDateFormat(DATA[i + 2], Locale.ENGLISH);
            fmt.setCalendar(greenwichcalendar);
            String result = fmt.format(greenwichdate);
            logln(DATA[i] + result);
            if (!result.equals(DATA[i + 1]))
                errln("FAIL: Expected " + DATA[i + 1] + ", got " + result);
        }
        //}
        //finally {
        TimeZone.setDefault(saveDefault);
        //}
    
    }
    
    /**
     * Test the formatting of dates in different locales.
     */
    public void TestLocaleDateFormat() {
    
        Date testDate = new Date(874306800000l); //Mon Sep 15 00:00:00 PDT 1997
        DateFormat dfFrench = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.FRENCH);
        DateFormat dfUS = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, Locale.US);
        //Set TimeZone = PDT
        TimeZone tz = TimeZone.getTimeZone("PST");
        dfFrench.setTimeZone(tz);
        dfUS.setTimeZone(tz);
        String expectedFRENCH_JDK12 = "lundi 15 septembre 1997 00 h 00 HAP (\u00C9UA)";
        //String expectedFRENCH = "lundi 15 septembre 1997 00 h 00 PDT";
        String expectedUS = "Monday, September 15, 1997 12:00:00 AM PDT";
        logln("Date set to : " + testDate);
        String out = dfFrench.format(testDate);
        logln("Date Formated with French Locale " + out);
        //fix the jdk resources differences between jdk 1.2 and jdk 1.3
        /* our own data only has GMT-xxxx information here
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.2")) {
            if (!out.equals(expectedFRENCH_JDK12))
                errln("FAIL: Expected " + expectedFRENCH_JDK12);
        } else {
            if (!out.equals(expectedFRENCH))
                errln("FAIL: Expected " + expectedFRENCH);
        }
        */
        if (!out.equals(expectedFRENCH_JDK12))
            errln("FAIL: Expected " + expectedFRENCH_JDK12);
        out = dfUS.format(testDate);
        logln("Date Formated with US Locale " + out);
        if (!out.equals(expectedUS))
            errln("FAIL: Expected " + expectedUS);
    }

    /**
     * Test DateFormat(Calendar) API
     */
    public void TestDateFormatCalendar() {
        DateFormat date=null, time=null, full=null;
        Calendar cal=null;
        ParsePosition pos = new ParsePosition(0);
        String str;
        Date when;

        /* Create a formatter for date fields. */
        date = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
        if (date == null) {
            errln("FAIL: getDateInstance failed");
            return;
        }

        /* Create a formatter for time fields. */
        time = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.US);
        if (time == null) {
            errln("FAIL: getTimeInstance failed");
            return;
        }

        /* Create a full format for output */
        full = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL,
                                              Locale.US);
        if (full == null) {
            errln("FAIL: getInstance failed");
            return;
        }

        /* Create a calendar */
        cal = Calendar.getInstance(Locale.US);
        if (cal == null) {
            errln("FAIL: Calendar.getInstance failed");
            return;
        }

        /* Parse the date */
        cal.clear();
        str = "4/5/2001";
        pos.setIndex(0);
        date.parse(str, cal, pos);
        if (pos.getIndex() != str.length()) {
            errln("FAIL: DateFormat.parse(4/5/2001) failed at " +
                  pos.getIndex());
            return;
        }

        /* Parse the time */
        str = "5:45 PM";
        pos.setIndex(0);
        time.parse(str, cal, pos);
        if (pos.getIndex() != str.length()) {
            errln("FAIL: DateFormat.parse(17:45) failed at " +
                  pos.getIndex());
            return;
        }
    
        /* Check result */
        when = cal.getTime();
        str = full.format(when);
        // Thursday, April 5, 2001 5:45:00 PM PDT 986517900000
        if (when.getTime() == 986517900000.0) {
            logln("Ok: Parsed result: " + str);
        } else {
            errln("FAIL: Parsed result: " + str + ", exp 4/5/2001 5:45 PM");
        }
    }

    /**
     * Test DateFormat's parsing of space characters.  See jitterbug 1916.
     */
    public void TestSpaceParsing() {

        String DATA[] = {
            "yyyy MM dd",

            // pattern, input, expexted output (in quotes)
            "MMMM d yy", " 04 05 06",  null, // MMMM wants Apr/April
            null,        "04 05 06",   null,
            "MM d yy",   " 04 05 06",  "2006 04 05",
            null,        "04 05 06",   "2006 04 05",
            "MMMM d yy", " Apr 05 06", "2006 04 05",
            null,        "Apr 05 06",  "2006 04 05",
        };

        expectParse(DATA, new Locale("en", "", ""));
    }

    /**
     * Test handling of "HHmmss" pattern.
     */
    public void TestExactCountFormat() {
        String DATA[] = {
            "yyyy MM dd HH:mm:ss",

            // pattern, input, expected parse or null if expect parse failure
            "HHmmss", "123456", "1970 01 01 12:34:56",
            null,     "12345",  "1970 01 01 01:23:45",
            null,     "1234",   null,
            null,     "00-05",  null,
            null,     "12-34",  null,
            null,     "00+05",  null,
            "ahhmm",  "PM730",  "1970 01 01 19:30:00",
        };

        expectParse(DATA, new Locale("en", "", ""));
    }

    /**
     * Test handling of white space.
     */
    public void TestWhiteSpaceParsing() {
        String DATA[] = {
            "yyyy MM dd",

            // pattern, input, expected parse or null if expect parse failure

            // Pattern space run should parse input text space run
            "MM   d yy",   " 04 01 03",    "2003 04 01",
            null,          " 04  01   03 ", "2003 04 01",
        };

        expectParse(DATA, new Locale("en", "", ""));
    }

    public void TestInvalidPattern() {
        Exception e = null;
        SimpleDateFormat f = null;
        String out = null;
        try {
            f = new SimpleDateFormat("Yesterday");
            out = f.format(new Date(0));
        } catch (IllegalArgumentException e1) {
            e = e1;
        }
        if (e != null) {
            logln("Ok: Received " + e.getMessage());
        } else {
            errln("FAIL: Expected exception, got " + f.toPattern() +
                  "; " + out);
        }
    }

    public void TestGreekMay() {
        Date date = new Date(-9896080848000L);
        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, dd MMMM yyyy h:mm:ss a",
                             new Locale("el", "", ""));
        String str = fmt.format(date);
        ParsePosition pos = new ParsePosition(0);
        Date d2 = fmt.parse(str, pos);
        if (!date.equals(d2)) {
            errln("FAIL: unable to parse strings where case-folding changes length");
        }
    }

    public void testErrorChecking() {
        try {
            DateFormat sdf = DateFormat.getDateTimeInstance(-1, -1, Locale.US);
            errln("Expected exception for getDateTimeInstance(-1, -1, Locale)");
        }
        catch(IllegalArgumentException e) {
            logln("one ok");
        }
        catch(Exception e) {
            errln("Expected IllegalArgumentException, got: " + e);
        }
        
        try {
            DateFormat df = new SimpleDateFormat("aabbccc");
            df.format(new Date());
            errln("Expected exception for format with bad pattern");
        }
        catch(IllegalArgumentException ex) {
            logln("two ok");
        }
        catch(Exception e) {
            errln("Expected IllegalArgumentException, got: " + e);
        }
        
        {
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yy"); // opposite of text
            fmt.set2DigitYearStart(getDate(2003, Calendar.DECEMBER, 25));
            String text = "12/25/03";
            Calendar xcal = new GregorianCalendar();
            xcal.setLenient(false);
            ParsePosition pp = new ParsePosition(0);
            fmt.parse(text, xcal, pp); // should get parse error on second field, not lenient
            if (pp.getErrorIndex() == -1) {
                errln("Expected parse error");
            } else {
                logln("three ok");
            }
        }
    }

    public void TestCoverage() {
        Date now = new Date();
        Calendar cal = new GregorianCalendar();
        DateFormat f = DateFormat.getTimeInstance();
        logln("time: " + f.format(now));

        int hash = f.hashCode(); // sigh, everyone overrides this
        
        f = DateFormat.getInstance(cal);
        if(hash == f.hashCode()){
            errln("FAIL: hashCode equal for inequal objects");
        }
        logln("time again: " + f.format(now));

        f = DateFormat.getTimeInstance(cal, DateFormat.FULL);
        logln("time yet again: " + f.format(now));

        f = DateFormat.getDateInstance();
        logln("time yet again: " + f.format(now));

        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME,"de_DE");
        DateFormatSymbols sym = new DateFormatSymbols(rb, Locale.GERMANY);
        DateFormatSymbols sym2 = (DateFormatSymbols)sym.clone();
        if (sym.hashCode() != sym2.hashCode()) {
            errln("fail, date format symbols hashcode not equal");
        }
        if (!sym.equals(sym2)) {
            errln("fail, date format symbols not equal");
        }
        
        Locale foo = new Locale("fu", "FU", "BAR");
        rb = null;
        sym = new DateFormatSymbols(GregorianCalendar.class, foo);
        sym.equals(null);
        
        sym = new ChineseDateFormatSymbols();
        sym = new ChineseDateFormatSymbols(new ChineseCalendar(), foo);
        // cover new ChineseDateFormatSymbols(Calendar, ULocale)
        ChineseCalendar ccal = new ChineseCalendar();
        sym = new ChineseDateFormatSymbols(ccal, ULocale.CHINA); //gclsh1 add
        
        StringBuffer buf = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        
        f.format((Object)cal, buf, pos);
        f.format((Object)now, buf, pos);
        f.format((Object)new Long(now.getTime()), buf, pos);
        try {
            f.format((Object)"Howdy", buf, pos);
        }
        catch (Exception e) {
        }

        NumberFormat nf = f.getNumberFormat();
        f.setNumberFormat(nf);
        
        boolean lenient = f.isLenient();
        f.setLenient(lenient);
        
        ULocale uloc = f.getLocale(ULocale.ACTUAL_LOCALE);
        
        int hashCode = f.hashCode();
        
        boolean eq = f.equals(f);
        eq = f.equals(null);
        eq = f.equals(new SimpleDateFormat());
        
        {
            ChineseDateFormat fmt = new ChineseDateFormat("yymm", Locale.US);
            try {
                Date d = fmt.parse("2"); // fewer symbols than required 2
                errln("whoops");
            }
            catch (ParseException e) {
                logln("ok");
            }

            try {
                Date d = fmt.parse("2255"); // should succeed with obeycount
                logln("ok");
            }
            catch (ParseException e) {
                logln("whoops");
            }

            try {
                Date d = fmt.parse("ni hao"); // not a number, should fail
                errln("whoops ni hao");
            }
            catch (ParseException e) {
                logln("ok ni hao");
            }
        }
        {
            Calendar xcal = new GregorianCalendar();
            xcal.set(Calendar.HOUR_OF_DAY, 0);
            DateFormat fmt = new SimpleDateFormat("k");
            StringBuffer xbuf = new StringBuffer();
            FieldPosition fpos = new FieldPosition(Calendar.HOUR_OF_DAY);
            fmt.format(xcal, xbuf, fpos);
            try {
                Date d = fmt.parse(xbuf.toString());
                logln("ok");
                
                xbuf.setLength(0);
                xcal.set(Calendar.HOUR_OF_DAY, 25);
                fmt.format(xcal, xbuf, fpos);
                Date d2 = fmt.parse(xbuf.toString());
                logln("ok again");
            }
            catch (ParseException e) {
                errln("whoops");
            }
        }
        
        {
            // cover gmt+hh:mm
            DateFormat fmt = new SimpleDateFormat("MM/dd/yy z");
            try {
                Date d = fmt.parse("07/10/53 GMT+10:00");
                logln("ok");
            }
            catch (ParseException e) {
                errln("whoops");
            }
            
            // cover invalid separator after GMT
            try {
                Date d = fmt.parse("07/10/53 GMT=10:00");
                logln("whoops");
            }
            catch (ParseException e) {
                errln("ok");
            }
            
            // cover bad text after GMT+.
            try {
                Date d = fmt.parse("07/10/53 GMT+blecch");
                errln("whoops GMT+blecch");
            }
            catch (ParseException e) {
                logln("ok GMT+blecch");
            }
            
            // cover bad text after GMT+hh:.
            try {
                Date d = fmt.parse("07/10/53 GMT+07:blecch");
                errln("whoops GMT+xx:blecch");
            }
            catch (ParseException e) {
                logln("ok GMT+xx:blecch");
            }
            
            // cover no ':' GMT+#, # < 24 (hh)
            try {
                Date d = fmt.parse("07/10/53 GMT+07");
                logln("ok");
            }
            catch (ParseException e) {
                errln("whoops");
            }
            
            // cover no ':' GMT+#, # > 24 (hhmm)
            try {
                Date d = fmt.parse("07/10/53 GMT+0730");
                logln("ok");
            }
            catch (ParseException e) {
                errln("whoops");
            }
            
            // cover no ':' GMT+#, # > 2400 (this should fail, i suspect, but doesn't)
            try {
                Date d = fmt.parse("07/10/53 GMT+07300");
                logln("should GMT+9999 fail?");
            }
            catch (ParseException e) {
                logln("ok, I guess");
            }
            
            // cover raw digits with no leading sign (bad RFC822) 
            try {
                Date d = fmt.parse("07/10/53 07");
                errln("whoops");
            }
            catch (ParseException e) {
                logln("ok");
            }
            
            // cover raw digits (RFC822) 
            try {
                Date d = fmt.parse("07/10/53 +07");
                logln("ok");
            }
            catch (ParseException e) {
                errln("whoops");
            }
            
            // cover raw digits (RFC822) 
            try {
                Date d = fmt.parse("07/10/53 -0730");
                logln("ok");
            }
            catch (ParseException e) {
                errln("whoops");
            }
            
            // cover raw digits (RFC822) in DST
            try {
                fmt.setTimeZone(TimeZone.getTimeZone("PDT"));
                Date d = fmt.parse("07/10/53 -0730");
                logln("ok");
            }
            catch (ParseException e) {
                errln("whoops");
            }
        }
        
        {
            SimpleDateFormat fmt = new SimpleDateFormat("aabbcc");
            try {
                String pat = fmt.toLocalizedPattern();
                errln("whoops, shouldn't have been able to localize aabbcc");
            }
            catch (IllegalArgumentException e) {
                logln("aabbcc localize ok");
            }
        }

        {
            SimpleDateFormat fmt = new SimpleDateFormat("'aabbcc");
            try {
                String pat = fmt.toLocalizedPattern();
                errln("whoops, localize unclosed quote");
            }
            catch (IllegalArgumentException e) {
                logln("localize unclosed quote ok");
            }
        }
        {
            SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yy z");
            String text = "08/15/58 DBDY"; // bogus time zone
            try {
                fmt.parse(text);
                errln("recognized bogus time zone DBDY");
            }
            catch (ParseException e) {
                logln("time zone ex ok");
            }
        }
        
        {
            // force fallback to default timezone when fmt timezone 
            // is not named
            SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yy z");
            // force fallback to default time zone, still fails
            fmt.setTimeZone(TimeZone.getTimeZone("GMT+0147")); // not in equivalency group
            String text = "08/15/58 DBDY";
            try {
                fmt.parse(text);
                errln("whoops");
            }
            catch (ParseException e) {
                logln("time zone ex2 ok");
            }
            
            // force success on fallback
            text = "08/15/58 " + TimeZone.getDefault().getID();
            try {
                fmt.parse(text);
                logln("found default tz");
            }
            catch (ParseException e) {
                errln("whoops, got parse exception");
            }
        }
        
        {
            // force fallback to symbols list of timezones when neither 
            // fmt and default timezone is named
            SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yy z");
            TimeZone oldtz = TimeZone.getDefault();
            TimeZone newtz = TimeZone.getTimeZone("GMT+0137"); // nonstandard tz
            fmt.setTimeZone(newtz);
            TimeZone.setDefault(newtz); // todo: fix security issue

            // fallback to symbol list, but fail
            String text = "08/15/58 DBDY"; // try to parse the bogus time zone
            try {
                fmt.parse(text);
                errln("whoops again");
            }
            catch (ParseException e) {
                logln("time zone ex3 ok");
            }
            catch (Exception e) {
                // hmmm... this shouldn't happen.  don't want to exit this
                // fn with timezone improperly set, so just in case
                TimeZone.setDefault(oldtz);
                throw new InternalError(e.getMessage());
            }

            // create DFS that recognizes our bogus time zone, sortof
            DateFormatSymbols xsym = new DateFormatSymbols();
            String[][] tzids = xsym.getZoneStrings();
            boolean changedGMT = false;
            for (int i = 0; i < tzids.length; ++i) {
                if (tzids[i][0].equals("Etc/GMT")) {
                    tzids[i][1] = "DBDY"; // change a local name
                    logln("replaced GMT with DBDY");
                    changedGMT = true;
                    break;
                }
            }
            xsym.setZoneStrings(tzids);
            fmt.setDateFormatSymbols(xsym);
            if(changedGMT==true){
                try {
                    fmt.parse(text);
                    logln("we parsed DBDY (as GMT, but still...)");
                }
                catch (ParseException e) {
                    errln("hey, still didn't recognize DBDY");
                }
                finally {
                    TimeZone.setDefault(oldtz);
                }
            }else{
                errln("Could not find Etc/GMT in the tzids returned.");
            }
        }

        {
            //cover getAvailableULocales
            final ULocale[] locales = DateFormat.getAvailableULocales();
            long count = locales.length;
            if (count==0) {
                errln(" got a empty list for getAvailableULocales");
            }else{
                logln("" + count + " available ulocales");            
            }
        }
        
        {
            //cover DateFormatSymbols.getDateFormatBundle
            cal = new GregorianCalendar();
            Locale loc = Locale.getDefault();
            DateFormatSymbols mysym = new DateFormatSymbols(cal, loc);
            if (mysym == null) 
                errln("FAIL: constructs DateFormatSymbols with calendar and locale failed");
            
            uloc = ULocale.getDefault();
            ResourceBundle resb = DateFormatSymbols.getDateFormatBundle(cal, loc);
            ResourceBundle resb2 = DateFormatSymbols.getDateFormatBundle(cal, uloc);
            ResourceBundle resb3 = DateFormatSymbols.getDateFormatBundle(cal.getClass(), loc);
            ResourceBundle resb4 = DateFormatSymbols.getDateFormatBundle(cal.getClass(), uloc);
            
            /* (ToDo) Not sure how to construct resourceBundle for this test
                So comment out the verifying code. 
            if (!resb.equals(resb2) || 
                !resb.equals(resb3) ||
                !resb.equals(resb4) )
                errln("FAIL: getDateFormatBundle failed!");            
            */
        }
    }

    /**
     * Test parsing.  Input is an array that starts with the following
     * header:
     *
     * [0]   = pattern string to parse [i+2] with
     *
     * followed by test cases, each of which is 3 array elements:
     *
     * [i]   = pattern, or null to reuse prior pattern
     * [i+1] = input string
     * [i+2] = expected parse result (parsed with pattern [0])
     *
     * If expect parse failure, then [i+2] should be null.
     */
    void expectParse(String[] data, Locale loc) {
        Date FAIL = null;
        String FAIL_STR = "parse failure";
        int i = 0;

        SimpleDateFormat fmt = new SimpleDateFormat("", loc);
        SimpleDateFormat ref = new SimpleDateFormat(data[i++], loc);
        SimpleDateFormat gotfmt = new SimpleDateFormat("G yyyy MM dd HH:mm:ss z", loc);

        String currentPat = null;
        while (i<data.length) {
            String pattern  = data[i++];
            String input    = data[i++];
            String expected = data[i++];

            if (pattern != null) {
                fmt.applyPattern(pattern);
                currentPat = pattern;
            }
            String gotstr = FAIL_STR;
            Date got;
            try {
                got = fmt.parse(input);
                gotstr = gotfmt.format(got);
            } catch (ParseException e1) {
                got = FAIL;
            }

            Date exp = FAIL;
            String expstr = FAIL_STR;
            if (expected != null) {
                expstr = expected;
                try {
                    exp = ref.parse(expstr);
                } catch (ParseException e2) {
                    errln("FAIL: Internal test error");
                }
            }

            if (got == exp || (got != null && got.equals(exp))) {
                logln("Ok: " + input + " x " +
                      currentPat + " => " + gotstr);                
            } else {
                errln("FAIL: " + input + " x " +
                      currentPat + " => " + gotstr + ", expected " +
                      expstr);
            }
        }    
    }

    /**
     * Test formatting and parsing.  Input is an array that starts
     * with the following header:
     *
     * [0]   = pattern string to parse [i+2] with
     *
     * followed by test cases, each of which is 3 array elements:
     *
     * [i]   = pattern, or null to reuse prior pattern
     * [i+1] = control string, either "fp", "pf", or "F".
     * [i+2..] = data strings
     *
     * The number of data strings depends on the control string.
     * Examples:
     * 1. "y/M/d H:mm:ss.SS", "fp", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.56", "2004 03 10 16:36:31.560",
     * 'f': Format date [i+2] (as parsed using pattern [0]) and expect string [i+3].
     * 'p': Parse string [i+3] and expect date [i+4].
     *
     * 2. "y/M/d H:mm:ss.SSS", "F", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.567"
     * 'F': Format date [i+2] and expect string [i+3],
     *      then parse string [i+3] and expect date [i+2].
     *
     * 3. "y/M/d H:mm:ss.SSSS", "pf", "2004/3/10 16:36:31.5679", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.5670",
     * 'p': Parse string [i+2] and expect date [i+3].
     * 'f': Format date [i+3] and expect string [i+4].
     */
    void expect(String[] data, Locale loc) {
        int i = 0;

        SimpleDateFormat fmt = new SimpleDateFormat("", loc);
        SimpleDateFormat ref = new SimpleDateFormat(data[i++], loc);
        SimpleDateFormat univ = new SimpleDateFormat("EE G yyyy MM dd HH:mm:ss.SSS z", loc);

        String currentPat = null;
        while (i<data.length) {
            String pattern  = data[i++];
            if (pattern != null) {
                fmt.applyPattern(pattern);
                currentPat = pattern;
            }

            String control = data[i++];

            if (control.equals("fp")) {
                // 'f'
                String datestr = data[i++];
                String string = data[i++];
                Date date = null;
                try {
                    date = ref.parse(datestr);
                } catch (ParseException e) {
                    errln("FAIL: Internal test error; can't parse " + datestr);
                    return;
                }
                assertEquals("\"" + currentPat + "\".format(" + datestr + ")",
                             string,
                             fmt.format(date));
                // 'p'
                datestr = data[i++];
                try {
                    date = ref.parse(datestr);
                } catch (ParseException e2) {
                    errln("FAIL: Internal test error; can't parse " + datestr);
                    return;
                }
                try {
                    Date parsedate = fmt.parse(string);
                    assertEquals("\"" + currentPat + "\".parse(" + string + ")",
                                 univ.format(date),
                                 univ.format(parsedate));
                } catch (ParseException e3) {
                    errln("FAIL: \"" + currentPat + "\".parse(" + string + ") => " +
                          e3);
                }
            }

            else if (control.equals("pf")) {
                // 'p'
                String string = data[i++];
                String datestr = data[i++];
                Date date = null;
                try {
                    date = ref.parse(datestr);
                } catch (ParseException e) {
                    errln("FAIL: Internal test error; can't parse " + datestr);
                    return;
                }
                try {
                    Date parsedate = fmt.parse(string);
                    assertEquals("\"" + currentPat + "\".parse(" + string + ")",
                                 univ.format(date),
                                 univ.format(parsedate));
                } catch (ParseException e2) {
                    errln("FAIL: \"" + currentPat + "\".parse(" + string + ") => " +
                          e2);
                }
                // 'f'
                string = data[i++];
                assertEquals("\"" + currentPat + "\".format(" + datestr + ")",
                             string,
                             fmt.format(date));
            }

            else if (control.equals("F")) {
                String datestr  = data[i++];
                String string   = data[i++];
                Date date = null;
                try {
                    date = ref.parse(datestr);
                } catch (ParseException e) {
                    errln("FAIL: Internal test error; can't parse " + datestr);
                    return;
                }

                assertEquals("\"" + currentPat + "\".format(" + datestr + ")",
                             string,
                             fmt.format(date));

                try {
                    Date parsedate = fmt.parse(string);
                    assertEquals("\"" + currentPat + "\".parse(" + string + ")",
                                 univ.format(date),
                                 univ.format(parsedate));
                } catch (ParseException e2) {
                    errln("FAIL: \"" + currentPat + "\".parse(" + string + ") => " +
                          e2);
                }
            }

            else {
                errln("FAIL: Invalid control string " + control);
                return;
            }
        }
    }
}
