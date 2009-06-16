/*
 *******************************************************************************
 * Copyright (C) 2001-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/** 
 * Port From:   ICU4C v1.8.1 : format : DateFormatTest
 * Source File: $ICU4CRoot/source/test/intltest/dtfmttst.cpp
 **/

package com.ibm.icu.dev.test.format;

import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.text.ChineseDateFormat;
import com.ibm.icu.text.ChineseDateFormatSymbols;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.BuddhistCalendar;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ChineseCalendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.HebrewCalendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.VersionInfo;

public class DateFormatTest extends com.ibm.icu.dev.test.TestFmwk {
    
    public static void main(String[] args) throws Exception {
        new DateFormatTest().run(args);
    }

    /**
     * Verify that patterns have the correct values and could produce the 
     * the DateFormat instances that contain the correct localized patterns.
     */
    public void TestPatterns() {
        final String[][] EXPECTED = {
                {DateFormat.MINUTE_SECOND, "ms", "en", "m:s"},
                {DateFormat.HOUR24_MINUTE, "Hm", "en", "H:m"},
                {DateFormat.HOUR24_MINUTE_SECOND, "Hms","en","H:m:s"},
                {DateFormat.HOUR_MINUTE, "hm","en","h:m a"},
                {DateFormat.HOUR_MINUTE_SECOND, "hms","en","h:m:s a"},
                {DateFormat.DAY, "d","en","d"},
                {DateFormat.STANDALONE_MONTH, "LLLL","en","LLLL"},
                {DateFormat.ABBR_STANDALONE_MONTH, "LLL","en","LLL"},
                {DateFormat.YEAR, "y","en","y"},
                {DateFormat.MONTH_DAY, "MMMMd","en","MMMM d"},
                {DateFormat.ABBR_MONTH_DAY, "MMMd","en","MMM d"},
                {DateFormat.NUM_MONTH_DAY, "Md","en","M/d"},
                {DateFormat.MONTH_WEEKDAY_DAY, "MMMMEEEEd","en","EEEE, MMMM d"},
                {DateFormat.ABBR_MONTH_WEEKDAY_DAY, "MMMEd","en","E, MMM d"},
                {DateFormat.NUM_MONTH_WEEKDAY_DAY, "MEd","en","E, M/d"},
                {DateFormat.YEAR_MONTH, "yMMMM","en","MMMM y"},
                {DateFormat.YEAR_ABBR_MONTH, "yMMM","en","MMM y"},
                {DateFormat.YEAR_NUM_MONTH, "yM","en","M/y"}, 
                {DateFormat.YEAR_ABBR_MONTH_WEEKDAY_DAY, "yMMMEd", "en", "E, MMM d, y"},
                {DateFormat.YEAR_NUM_MONTH_WEEKDAY_DAY, "yMEd", "en", "E, M/d/y"},
                {DateFormat.YEAR_QUARTER, "yQQQ", "en", "QQQ y"},
                {DateFormat.YEAR_ABBR_QUARTER, "yQ", "en", "Q y"}
        };
        
        for (int i = 0; i < EXPECTED.length; i++) {
            
            // Verify that patterns have the correct values
            String actualPattern = EXPECTED[i][0];
            String expectedPattern = EXPECTED[i][1];
            ULocale locale = new ULocale(EXPECTED[i][2], "", "");
            if (!actualPattern.equals(expectedPattern)) {
                errln("FAILURE! Expected pattern: " + expectedPattern + 
                        " but was: " + actualPattern);
            }
            
            // Verify that DataFormat instances produced contain the correct 
            // localized patterns
            DateFormat date1 = DateFormat.getPatternInstance(actualPattern, 
                    locale);
            DateFormat date2 = DateFormat.getPatternInstance(Calendar.getInstance(locale),
                    actualPattern, locale);
            
            String expectedLocalPattern = EXPECTED[i][3];
            String actualLocalPattern1 = ((SimpleDateFormat)date1).toLocalizedPattern();
            String actualLocalPattern2 = ((SimpleDateFormat)date2).toLocalizedPattern();
            if (!actualLocalPattern1.equals(expectedLocalPattern)) {
                errln("FAILURE! Expected local pattern: " + expectedLocalPattern 
                        + " but was: " + actualLocalPattern1);
            }       
            if (!actualLocalPattern2.equals(expectedLocalPattern)) {
                errln("FAILURE! Expected local pattern: " + expectedLocalPattern 
                        + " but was: " + actualLocalPattern2);
            }      
        }
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
        int offset, hours, minutes, seconds;
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
            seconds = (offset % 60000) / 1000;
            String dstOffset = sign + (hours < 10 ? "0" : "") + hours
                    + ":" + (minutes < 10 ? "0" : "") + minutes; 
            if (seconds != 0) {
                dstOffset += ":" + (seconds < 10 ? "0" : "") + seconds;
            }
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
        if (VersionInfo.ICU_VERSION.compareTo(VersionInfo.getInstance(3, 7)) >= 0) {
            DateFormatSymbols rootSyms = new DateFormatSymbols(new Locale("", "", ""));
            assertEquals("patternChars", PATTERN_CHARS, rootSyms.getLocalPatternChars());
        }
        
        assertTrue("DATEFORMAT_FIELD_NAMES", DATEFORMAT_FIELD_NAMES.length == DateFormat.FIELD_COUNT);
        if(DateFormat.FIELD_COUNT != PATTERN_CHARS.length()){
            errln("Did not get the correct value for DateFormat.FIELD_COUNT. Expected:  "+ PATTERN_CHARS.length());
        }

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
            "Wednesday", "", "", "", "", "PM", "2", "", "Pacific Daylight Time", "", "", "", "", "", "", "","","","","","",

            "", "1997", "ao\u00FBt", "13", "", "14", "34", "12", "",
            "mercredi", "", "", "", "", "", "", "", "heure avanc\u00E9e du Pacifique", "", "", "", "", "", "", "","","","","","",

            "AD", "1997", "8", "13", "14", "14", "34", "12", "5",
            "Wed", "225", "2", "33", "2", "PM", "2", "2", "PDT", "1997", "4", "1997", "2450674", "52452513", "-0700", "PT","4","8","3","3","PDT",

            "Anno Domini", "1997", "August", "0013", "0014", "0014", "0034", "0012", "5130",
            "Wednesday", "0225", "0002", "0033", "0002", "PM", "0002", "0002", "Pacific Daylight Time", "1997",
            "Wednesday", "1997", "2450674", "52452513", "GMT-07:00", "Pacific Time","Wednesday","August", "3rd quarter", "3rd quarter","United States (Los Angeles)",
        };

        assertTrue("data size", EXPECTED.length == COUNT * DateFormat.FIELD_COUNT);

        final DateFormat.Field[] DTFMT_FIELDS = {
            DateFormat.Field.AM_PM,
            DateFormat.Field.DAY_OF_MONTH,
            DateFormat.Field.DAY_OF_WEEK,
            DateFormat.Field.DAY_OF_WEEK_IN_MONTH,
            DateFormat.Field.DAY_OF_YEAR,

            DateFormat.Field.DOW_LOCAL,
            DateFormat.Field.ERA,
            DateFormat.Field.EXTENDED_YEAR,
            DateFormat.Field.HOUR_OF_DAY0,
            DateFormat.Field.HOUR_OF_DAY1,

            DateFormat.Field.HOUR0,
            DateFormat.Field.HOUR1,
            DateFormat.Field.JULIAN_DAY,
            DateFormat.Field.MILLISECOND,
            DateFormat.Field.MILLISECONDS_IN_DAY,

            DateFormat.Field.MINUTE,
            DateFormat.Field.MONTH,
            DateFormat.Field.QUARTER,
            DateFormat.Field.SECOND,
            DateFormat.Field.TIME_ZONE,

            DateFormat.Field.WEEK_OF_MONTH,
            DateFormat.Field.WEEK_OF_YEAR,
            DateFormat.Field.YEAR,
            DateFormat.Field.YEAR_WOY,
        };

        final String[][] EXPECTED_BY_FIELD = {
            {"PM", "13", "Wednesday", "", "",
             "", "", "", "", "",
             "", "2", "", "", "",
             "34", "August", "", "12", "Pacific Daylight Time",
             "", "", "1997", ""},

            {"", "13", "mercredi", "", "",
             "", "", "", "14", "",
             "", "", "", "", "",
             "34", "ao\u00FBt", "", "12", "heure avanc\u00E9e du Pacifique",
             "", "", "1997", ""},

            {"PM", "13", "Wed", "2", "225",
             "4", "AD", "1997", "14", "14",
             "2", "2", "2450674", "5", "52452513",
             "34", "8", "3", "12", "PDT",
             "2", "33", "1997", "1997"},

            {"PM", "0013", "Wednesday", "0002", "0225",
             "Wednesday", "Anno Domini", "1997", "0014", "0014",
             "0002", "0002", "2450674", "5130", "52452513",
             "0034", "August", "3rd quarter", "0012", "Pacific Daylight Time",
             "0002", "0033", "1997", "1997"},
        };

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

            FieldPosition pos;
            String field;

            for (i = 0; i < DateFormat.FIELD_COUNT; ++i, ++exp) {
                pos = new FieldPosition(i);
                buf.setLength(0);
                df.format(aug13, buf, pos);    
                field = buf.substring(pos.getBeginIndex(), pos.getEndIndex());
                assertEquals("pattern#" + j + " field #" + i + " " + DATEFORMAT_FIELD_NAMES[i],
                             EXPECTED[exp], field);
            }

            // FieldPostion initialized by DateFormat.Field trac#6089
            for(i = 0; i < DTFMT_FIELDS.length; i++) {
                // The format method only set position for the first occurrence of
                // the specified field.
                pos = new FieldPosition(DTFMT_FIELDS[i]);
                buf.setLength(0);
                df.format(aug13, buf, pos);
                field = buf.substring(pos.getBeginIndex(), pos.getEndIndex());
                assertEquals("pattern#" + j + " " + DTFMT_FIELDS[i].toString(), EXPECTED_BY_FIELD[j][i], field);
            }
        }
    }
    /**
     * This MUST be kept in sync with DateFormatSymbols.patternChars.
     */
    static final String PATTERN_CHARS = "GyMdkHmsSEDFwWahKzYeugAZvcLQqV";

    /**
     * A list of the DateFormat.Field.
     * This MUST be kept in sync with PATTERN_CHARS above.
     */
    static final DateFormat.Field[] DATEFORMAT_FIELDS = {
        DateFormat.Field.ERA,           // G
        DateFormat.Field.YEAR,          // y
        DateFormat.Field.MONTH,         // M
        DateFormat.Field.DAY_OF_MONTH,  // d
        DateFormat.Field.HOUR_OF_DAY1,  // k
        DateFormat.Field.HOUR_OF_DAY0,  // H
        DateFormat.Field.MINUTE,        // m
        DateFormat.Field.SECOND,        // s
        DateFormat.Field.MILLISECOND,   // S
        DateFormat.Field.DAY_OF_WEEK,   // E
        DateFormat.Field.DAY_OF_YEAR,   // D
        DateFormat.Field.DAY_OF_WEEK_IN_MONTH,  // F
        DateFormat.Field.WEEK_OF_YEAR,  // w
        DateFormat.Field.WEEK_OF_MONTH, // W
        DateFormat.Field.AM_PM,         // a
        DateFormat.Field.HOUR1,         // h
        DateFormat.Field.HOUR0,         // K
        DateFormat.Field.TIME_ZONE,     // z
        DateFormat.Field.YEAR_WOY,      // Y
        DateFormat.Field.DOW_LOCAL,     // e
        DateFormat.Field.EXTENDED_YEAR, // u
        DateFormat.Field.JULIAN_DAY,    // g
        DateFormat.Field.MILLISECONDS_IN_DAY,   // A
        DateFormat.Field.TIME_ZONE,     // Z
        DateFormat.Field.TIME_ZONE,     // v
        DateFormat.Field.DAY_OF_WEEK,   // c
        DateFormat.Field.MONTH,         // L
        DateFormat.Field.QUARTER,       // Q
        DateFormat.Field.QUARTER,       // q
        DateFormat.Field.TIME_ZONE,     // V
    };

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
        "TIMEZONE_RFC_FIELD",
        "GENERIC_TIMEZONE_FIELD",
        "STAND_ALONE_DAY_FIELD",
        "STAND_ALONE_MONTH_FIELD",
        "QUARTER_FIELD",
        "STAND_ALONE_QUARTER_FIELD",
        "TIMEZONE_SPECIAL_FIELD",
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

    public void TestGenericTime() {


        // any zone pattern should parse any zone
        Locale en = new Locale("en", "", "");
        String ZDATA[] = {
            "yyyy MM dd HH:mm zzz",
            // round trip
            "y/M/d H:mm zzzz", "F", "2004 01 01 01:00 PST", "2004/1/1 1:00 Pacific Standard Time",
            "y/M/d H:mm zzz", "F", "2004 01 01 01:00 PST", "2004/1/1 1:00 PST",
            "y/M/d H:mm vvvv", "F", "2004 01 01 01:00 PST", "2004/1/1 1:00 Pacific Time",
            "y/M/d H:mm v", "F", "2004 01 01 01:00 PST", "2004/1/1 1:00 PT",
            // non-generic timezone string influences dst offset even if wrong for date/time
            "y/M/d H:mm zzz", "pf", "2004/1/1 1:00 PDT", "2004 01 01 01:00 PDT", "2004/1/1 0:00 PST",
            "y/M/d H:mm vvvv", "pf", "2004/1/1 1:00 PDT", "2004 01 01 01:00 PDT", "2004/1/1 0:00 Pacific Time",
            "y/M/d H:mm zzz", "pf", "2004/7/1 1:00 PST", "2004 07 01 02:00 PDT", "2004/7/1 2:00 PDT",
            "y/M/d H:mm vvvv", "pf", "2004/7/1 1:00 PST", "2004 07 01 02:00 PDT", "2004/7/1 2:00 Pacific Time",
            // generic timezone generates dst offset appropriate for local time
            "y/M/d H:mm zzz", "pf", "2004/1/1 1:00 PT", "2004 01 01 01:00 PST", "2004/1/1 1:00 PST",
            "y/M/d H:mm vvvv", "pf", "2004/1/1 1:00 PT", "2004 01 01 01:00 PST", "2004/1/1 1:00 Pacific Time",
            "y/M/d H:mm zzz", "pf", "2004/7/1 1:00 PT", "2004 07 01 01:00 PDT", "2004/7/1 1:00 PDT",
            "y/M/d H:mm vvvv", "pf", "2004/7/1 1:00 PT", "2004 07 01 01:00 PDT", "2004/7/1 1:00 Pacific Time",
            // daylight savings time transition edge cases.
            // time to parse does not really exist, PT interpreted as earlier time
            "y/M/d H:mm zzz", "pf", "2005/4/3 2:30 PT", "2005 04 03 03:30 PDT", "2005/4/3 3:30 PDT",
            "y/M/d H:mm zzz", "pf", "2005/4/3 2:30 PST", "2005 04 03 03:30 PDT", "2005/4/3 3:30 PDT",
            "y/M/d H:mm zzz", "pf", "2005/4/3 2:30 PDT", "2005 04 03 01:30 PST", "2005/4/3 1:30 PST",
            "y/M/d H:mm v", "pf", "2005/4/3 2:30 PT", "2005 04 03 03:30 PDT", "2005/4/3 3:30 PT",
            "y/M/d H:mm v", "pf", "2005/4/3 2:30 PST", "2005 04 03 03:30 PDT", "2005/4/3 3:30 PT",
            "y/M/d H:mm v", "pf", "2005/4/3 2:30 PDT", "2005 04 03 01:30 PST", "2005/4/3 1:30 PT",
            "y/M/d H:mm", "pf", "2005/4/3 2:30", "2005 04 03 03:30 PDT", "2005/4/3 3:30",
            // time to parse is ambiguous, PT interpreted as later time
            "y/M/d H:mm zzz", "pf", "2005/10/30 1:30 PT", "2005 10 30 01:30 PST", "2005/10/30 1:30 PST",
            "y/M/d H:mm v", "pf", "2005/10/30 1:30 PT", "2005 10 30  01:30 PST", "2005/10/30 1:30 PT",
            "y/M/d H:mm", "pf", "2005/10/30 1:30 PT", "2005 10 30 01:30 PST", "2005/10/30 1:30",

            "y/M/d H:mm zzz", "pf", "2004/10/31 1:30 PT", "2004 10 31 01:30 PST", "2004/10/31 1:30 PST",
             "y/M/d H:mm zzz", "pf", "2004/10/31 1:30 PST", "2004 10 31 01:30 PST", "2004/10/31 1:30 PST",
             "y/M/d H:mm zzz", "pf", "2004/10/31 1:30 PDT", "2004 10 31 01:30 PDT", "2004/10/31 1:30 PDT",
             "y/M/d H:mm v", "pf", "2004/10/31 1:30 PT", "2004 10 31 01:30 PST", "2004/10/31 1:30 PT",
             "y/M/d H:mm v", "pf", "2004/10/31 1:30 PST", "2004 10 31 01:30 PST", "2004/10/31 1:30 PT",
             "y/M/d H:mm v", "pf", "2004/10/31 1:30 PDT", "2004 10 31 01:30 PDT", "2004/10/31 1:30 PT",
             "y/M/d H:mm", "pf", "2004/10/31 1:30", "2004 10 31 01:30 PST", "2004/10/31 1:30",
            // Below is actually an invalid test case.  See the note in #5910.  Disable the case for now.
            // TODO: Revisit after 3.8
            //"y/M/d H:mm vvvv", "pf", "2004/10/31 1:30 Argentina Time", "2004 10 30 21:30 PDT", "2004/10/31 1:30 Argentina Time",
        };
        expect(ZDATA, en);

        logln("cross format/parse tests");
        final String basepat = "yy/MM/dd H:mm ";
        final SimpleDateFormat[] formats = { 
            new SimpleDateFormat(basepat + "v", en),
            new SimpleDateFormat(basepat + "vvvv", en),
            new SimpleDateFormat(basepat + "zzz", en),
            new SimpleDateFormat(basepat + "zzzz", en)
        };

        final SimpleDateFormat univ = new SimpleDateFormat("yyyy MM dd HH:mm zzz", en);
        final String[] times = { "2004 01 02 03:04 PST", "2004 07 08 09:10 PDT" };
        for (int i = 0; i < times.length; ++i) {
            try {
                Date d = univ.parse(times[i]);
                logln("time: " + d);
                for (int j = 0; j < formats.length; ++j) {
                    String test = formats[j].format(d);
                    logln("test: '" + test + "'");
                    for (int k = 0; k < formats.length; ++k) {
                        try {
                            Date t = formats[k].parse(test);
                            if (!d.equals(t)) {
                                errln("format " + k + 
                                      " incorrectly parsed output of format " + j + 
                                      " (" + test + "), returned " +
                                      t + " instead of " + d);
                            } else {
                                logln("format " + k + " parsed ok");
                            }
                        }
                        catch (ParseException e) {
                            errln("format " + k + 
                                  " could not parse output of format " + j + 
                                  " (" + test + ")");
                        }
                    }
                }
            }
            catch (ParseException e) {
                errln("univ could not parse: " + times[i]);
            }
        }

    }

    public void TestGenericTimeZoneOrder() {
        // generic times should parse the same no matter what the placement of the time zone string
        // should work for standard and daylight times

        String XDATA[] = {
            "yyyy MM dd HH:mm zzz",
            // standard time, explicit daylight/standard
            "y/M/d H:mm zzz", "pf", "2004/1/1 1:00 PT", "2004 01 01 01:00 PST", "2004/1/1 1:00 PST",
            "y/M/d zzz H:mm", "pf", "2004/1/1 PT 1:00", "2004 01 01 01:00 PST", "2004/1/1 PST 1:00",
            "zzz y/M/d H:mm", "pf", "PT 2004/1/1 1:00", "2004 01 01 01:00 PST", "PST 2004/1/1 1:00",

            // standard time, generic
            "y/M/d H:mm vvvv", "pf", "2004/1/1 1:00 PT", "2004 01 01 01:00 PST", "2004/1/1 1:00 Pacific Time",
            "y/M/d vvvv H:mm", "pf", "2004/1/1 PT 1:00", "2004 01 01 01:00 PST", "2004/1/1 Pacific Time 1:00",
            "vvvv y/M/d H:mm", "pf", "PT 2004/1/1 1:00", "2004 01 01 01:00 PST", "Pacific Time 2004/1/1 1:00",

            // daylight time, explicit daylight/standard
            "y/M/d H:mm zzz", "pf", "2004/7/1 1:00 PT", "2004 07 01 01:00 PDT", "2004/7/1 1:00 PDT",
            "y/M/d zzz H:mm", "pf", "2004/7/1 PT 1:00", "2004 07 01 01:00 PDT", "2004/7/1 PDT 1:00",
            "zzz y/M/d H:mm", "pf", "PT 2004/7/1 1:00", "2004 07 01 01:00 PDT", "PDT 2004/7/1 1:00",

            // daylight time, generic
            "y/M/d H:mm vvvv", "pf", "2004/7/1 1:00 PT", "2004 07 01 01:00 PDT", "2004/7/1 1:00 Pacific Time",
            "y/M/d vvvv H:mm", "pf", "2004/7/1 PT 1:00", "2004 07 01 01:00 PDT", "2004/7/1 Pacific Time 1:00",
            "vvvv y/M/d H:mm", "pf", "PT 2004/7/1 1:00", "2004 07 01 01:00 PDT", "Pacific Time 2004/7/1 1:00",
        };
        Locale en = new Locale("en", "", "");
        expect(XDATA, en);
    }

    public void TestTimeZoneDisplayName() {
        Calendar cal = new GregorianCalendar();
        SimpleDateFormat testfmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        for (int i = 0; i < fallbackTests.length; ++i) {
            String[] info = fallbackTests[i];
            logln(info[0] + ";" + info[1] + ";" + info[2] + ";" + info[3]);

            long time = 0;
            try {
                Date testd = testfmt.parse(info[2]);
                time = testd.getTime();
            } catch (ParseException pe) {
                errln("Failed to parse test date data");
                continue;
            }
            ULocale l = new ULocale(info[0]);
            TimeZone tz = TimeZone.getTimeZone(info[1]);
            SimpleDateFormat fmt = new SimpleDateFormat(info[3], l);
            cal.setTimeInMillis(time);
            cal.setTimeZone(tz);
            String result = fmt.format(cal);
            if (!result.equals(info[4])) {
                errln(info[0] + ";" + info[1] + ";" + info[2] + ";" + info[3] + " expected: '" + 
                      info[4] + "' but got: '" + result + "'");
            }
        }
    }

    private static final String GMT_BG = "\u0413\u0440\u0438\u0438\u043D\u0443\u0438\u0447";
    private static final String GMT_ZH = "\u683C\u6797\u5C3C\u6CBB\u6807\u51C6\u65F6\u95F4";
    //private static final String GMT_BG = "GMT";

    private static final String[][] fallbackTests  = {
        { "en", "America/Los_Angeles", "2004-01-15T00:00:00Z", "Z", "-0800", "-8:00" },
        { "en", "America/Los_Angeles", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-08:00", "-8:00" },
        { "en", "America/Los_Angeles", "2004-01-15T00:00:00Z", "z", "PST", "America/Los_Angeles" },
        { "en", "America/Los_Angeles", "2004-01-15T00:00:00Z", "V", "PST", "America/Los_Angeles" },
        { "en", "America/Los_Angeles", "2004-01-15T00:00:00Z", "zzzz", "Pacific Standard Time", "America/Los_Angeles" },
        { "en", "America/Los_Angeles", "2004-07-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "en", "America/Los_Angeles", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-07:00", "-7:00" },
        { "en", "America/Los_Angeles", "2004-07-15T00:00:00Z", "z", "PDT", "America/Los_Angeles" },
        { "en", "America/Los_Angeles", "2004-07-15T00:00:00Z", "V", "PDT", "America/Los_Angeles" },
        { "en", "America/Los_Angeles", "2004-07-15T00:00:00Z", "zzzz", "Pacific Daylight Time", "America/Los_Angeles" },
        { "en", "America/Los_Angeles", "2004-07-15T00:00:00Z", "v", "PT", "America/Los_Angeles" },
        { "en", "America/Los_Angeles", "2004-07-15T00:00:00Z", "vvvv", "Pacific Time", "America/Los_Angeles" },
        { "en", "America/Los_Angeles", "2004-07-15T00:00:00Z", "VVVV", "United States (Los Angeles)", "America/Los_Angeles" },
        { "en_GB", "America/Los_Angeles", "2004-01-15T12:00:00Z", "z", "PST", "America/Los_Angeles" },
        { "en", "America/Phoenix", "2004-01-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "en", "America/Phoenix", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-07:00", "-7:00" },
        { "en", "America/Phoenix", "2004-01-15T00:00:00Z", "z", "MST", "America/Phoenix" },
        { "en", "America/Phoenix", "2004-01-15T00:00:00Z", "V", "MST", "America/Phoenix" },
        { "en", "America/Phoenix", "2004-01-15T00:00:00Z", "zzzz", "Mountain Standard Time", "America/Phoenix" },
        { "en", "America/Phoenix", "2004-07-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "en", "America/Phoenix", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-07:00", "-7:00" },
        { "en", "America/Phoenix", "2004-07-15T00:00:00Z", "z", "MST", "America/Phoenix" },
        { "en", "America/Phoenix", "2004-07-15T00:00:00Z", "V", "MST", "America/Phoenix" },
        { "en", "America/Phoenix", "2004-07-15T00:00:00Z", "zzzz", "Mountain Standard Time", "America/Phoenix" },
        { "en", "America/Phoenix", "2004-07-15T00:00:00Z", "v", "MST", "America/Phoenix" },
        { "en", "America/Phoenix", "2004-07-15T00:00:00Z", "vvvv", "Mountain Standard Time", "America/Phoenix" },
        { "en", "America/Phoenix", "2004-07-15T00:00:00Z", "VVVV", "United States (Phoenix)", "America/Phoenix" },

        { "en", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "V", "ART", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "Argentina Time", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "V", "ART", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "Argentina Time", "-3:00" },
        { "en", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "Argentina (Buenos Aires)", "America/Buenos_Aires" },
        { "en", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "Argentina Time", "America/Buenos_Aires" },
        { "en", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "VVVV", "Argentina (Buenos Aires)", "America/Buenos_Aires" },

        { "en", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "V", "ART", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "Argentina Time", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "V", "ART", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "Argentina Time", "-3:00" },
        { "en", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "Argentina (Buenos Aires)", "America/Buenos_Aires" },
        { "en", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "Argentina Time", "America/Buenos_Aires" },
        { "en", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "VVVV", "Argentina (Buenos Aires)", "America/Buenos_Aires" },

        { "en", "America/Havana", "2004-01-15T00:00:00Z", "Z", "-0500", "-5:00" },
        { "en", "America/Havana", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-05:00", "-5:00" },
        { "en", "America/Havana", "2004-01-15T00:00:00Z", "z", "GMT-05:00", "-5:00" },
        { "en", "America/Havana", "2004-01-15T00:00:00Z", "V", "CST (Cuba)", "-5:00" },
        { "en", "America/Havana", "2004-01-15T00:00:00Z", "zzzz", "Cuba Standard Time", "-5:00" },
        { "en", "America/Havana", "2004-07-15T00:00:00Z", "Z", "-0400", "-4:00" },
        { "en", "America/Havana", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-04:00", "-4:00" },
        { "en", "America/Havana", "2004-07-15T00:00:00Z", "z", "GMT-04:00", "-4:00" },
        { "en", "America/Havana", "2004-07-15T00:00:00Z", "V", "CDT (Cuba)", "-4:00" },
        { "en", "America/Havana", "2004-07-15T00:00:00Z", "zzzz", "Cuba Daylight Time", "-4:00" },
        { "en", "America/Havana", "2004-07-15T00:00:00Z", "v", "Cuba Time", "America/Havana" },
        { "en", "America/Havana", "2004-07-15T00:00:00Z", "vvvv", "Cuba Time", "America/Havana" },
        { "en", "America/Havana", "2004-07-15T00:00:00Z", "VVVV", "Cuba Time", "America/Havana" },

        { "en", "Australia/ACT", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "en", "Australia/ACT", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+11:00", "+11:00" },
        { "en", "Australia/ACT", "2004-01-15T00:00:00Z", "z", "GMT+11:00", "+11:00" },
        { "en", "Australia/ACT", "2004-01-15T00:00:00Z", "V", "AEDT", "+11:00" },
        { "en", "Australia/ACT", "2004-01-15T00:00:00Z", "zzzz", "Australian Eastern Daylight Time", "+11:00" },
        { "en", "Australia/ACT", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "en", "Australia/ACT", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+10:00", "+10:00" },
        { "en", "Australia/ACT", "2004-07-15T00:00:00Z", "z", "GMT+10:00", "+10:00" },
        { "en", "Australia/ACT", "2004-07-15T00:00:00Z", "V", "AEST", "+10:00" },
        { "en", "Australia/ACT", "2004-07-15T00:00:00Z", "zzzz", "Australian Eastern Standard Time", "+10:00" },
        { "en", "Australia/ACT", "2004-07-15T00:00:00Z", "v", "Australia (Sydney)", "Australia/Sydney" },
        { "en", "Australia/ACT", "2004-07-15T00:00:00Z", "vvvv", "Eastern Australia Time", "Australia/Sydney" },
        { "en", "Australia/ACT", "2004-07-15T00:00:00Z", "VVVV", "Australia (Sydney)", "Australia/Sydney" },

        { "en", "Australia/Sydney", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "en", "Australia/Sydney", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+11:00", "+11:00" },
        { "en", "Australia/Sydney", "2004-01-15T00:00:00Z", "z", "GMT+11:00", "+11:00" },
        { "en", "Australia/Sydney", "2004-01-15T00:00:00Z", "V", "AEDT", "+11:00" },
        { "en", "Australia/Sydney", "2004-01-15T00:00:00Z", "zzzz", "Australian Eastern Daylight Time", "+11:00" },
        { "en", "Australia/Sydney", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "en", "Australia/Sydney", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+10:00", "+10:00" },
        { "en", "Australia/Sydney", "2004-07-15T00:00:00Z", "z", "GMT+10:00", "+10:00" },
        { "en", "Australia/Sydney", "2004-07-15T00:00:00Z", "V", "AEST", "+10:00" },
        { "en", "Australia/Sydney", "2004-07-15T00:00:00Z", "zzzz", "Australian Eastern Standard Time", "+10:00" },
        { "en", "Australia/Sydney", "2004-07-15T00:00:00Z", "v", "Australia (Sydney)", "Australia/Sydney" },
        { "en", "Australia/Sydney", "2004-07-15T00:00:00Z", "vvvv", "Eastern Australia Time", "Australia/Sydney" },
        { "en", "Australia/Sydney", "2004-07-15T00:00:00Z", "VVVV", "Australia (Sydney)", "Australia/Sydney" },

        { "en", "Europe/London", "2004-01-15T00:00:00Z", "Z", "+0000", "+0:00" },
        { "en", "Europe/London", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+00:00", "+0:00" },
        { "en", "Europe/London", "2004-01-15T00:00:00Z", "z", "GMT", "+0:00" },
        { "en", "Europe/London", "2004-01-15T00:00:00Z", "V", "GMT", "+0:00" },
        { "en", "Europe/London", "2004-01-15T00:00:00Z", "zzzz", "Greenwich Mean Time", "+0:00" },
        { "en", "Europe/London", "2004-07-15T00:00:00Z", "Z", "+0100", "+1:00" },
        { "en", "Europe/London", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+01:00", "+1:00" },
        { "en", "Europe/London", "2004-07-15T00:00:00Z", "z", "GMT+01:00", "Europe/London" },
        { "en", "Europe/London", "2004-07-15T00:00:00Z", "V", "BST", "Europe/London" },
        { "en", "Europe/London", "2004-07-15T00:00:00Z", "zzzz", "British Summer Time", "Europe/London" },
    // icu en.txt has exemplar city for this time zone
        { "en", "Europe/London", "2004-07-15T00:00:00Z", "v", "United Kingdom Time", "Europe/London" },
        { "en", "Europe/London", "2004-07-15T00:00:00Z", "vvvv", "United Kingdom Time", "Europe/London" },
        { "en", "Europe/London", "2004-07-15T00:00:00Z", "VVVV", "United Kingdom Time", "Europe/London" },

        { "en", "Etc/GMT+3", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "en", "Etc/GMT+3", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "en", "Etc/GMT+3", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "en", "Etc/GMT+3", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "en", "Etc/GMT+3", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "en", "Etc/GMT+3", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "en", "Etc/GMT+3", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "en", "Etc/GMT+3", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "en", "Etc/GMT+3", "2004-07-15T00:00:00Z", "v", "GMT-03:00", "-3:00" },
        { "en", "Etc/GMT+3", "2004-07-15T00:00:00Z", "vvvv", "GMT-03:00", "-3:00" },

        // JB#5150
        { "en", "Asia/Calcutta", "2004-01-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "en", "Asia/Calcutta", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+05:30", "+5:30" },
        { "en", "Asia/Calcutta", "2004-01-15T00:00:00Z", "z", "GMT+05:30", "+5:30" },
        { "en", "Asia/Calcutta", "2004-01-15T00:00:00Z", "V", "IST", "+5:30" },
        { "en", "Asia/Calcutta", "2004-01-15T00:00:00Z", "zzzz", "India Standard Time", "+5:30" },
        { "en", "Asia/Calcutta", "2004-07-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "en", "Asia/Calcutta", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+05:30", "+5:30" },
        { "en", "Asia/Calcutta", "2004-07-15T00:00:00Z", "z", "GMT+05:30", "+05:30" },
        { "en", "Asia/Calcutta", "2004-07-15T00:00:00Z", "V", "IST", "+05:30" },
        { "en", "Asia/Calcutta", "2004-07-15T00:00:00Z", "zzzz", "India Standard Time", "+5:30" },
        { "en", "Asia/Calcutta", "2004-07-15T00:00:00Z", "v", "India Time", "Asia/Calcutta" },
        { "en", "Asia/Calcutta", "2004-07-15T00:00:00Z", "vvvv", "India Standard Time", "Asia/Calcutta" },
        
        // ==========

        { "de", "America/Los_Angeles", "2004-01-15T00:00:00Z", "Z", "-0800", "-8:00" },
        { "de", "America/Los_Angeles", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-08:00", "-8:00" },
        { "de", "America/Los_Angeles", "2004-01-15T00:00:00Z", "z", "GMT-08:00", "-8:00" },
        { "de", "America/Los_Angeles", "2004-01-15T00:00:00Z", "zzzz", "GMT-08:00", "-8:00" },
        { "de", "America/Los_Angeles", "2004-07-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "de", "America/Los_Angeles", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-07:00", "-7:00" },
        { "de", "America/Los_Angeles", "2004-07-15T00:00:00Z", "z", "GMT-07:00", "-7:00" },
        { "de", "America/Los_Angeles", "2004-07-15T00:00:00Z", "zzzz", "GMT-07:00", "-7:00" },
        { "de", "America/Los_Angeles", "2004-07-15T00:00:00Z", "v", "Vereinigte Staaten (Los Angeles)", "America/Los_Angeles" },
        { "de", "America/Los_Angeles", "2004-07-15T00:00:00Z", "vvvv", "Vereinigte Staaten (Los Angeles)", "America/Los_Angeles" },

        { "de", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "de", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "de", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "de", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "de", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "de", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "de", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "de", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "de", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "Argentinien (Buenos Aires)", "America/Buenos_Aires" },
        { "de", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "Argentinien (Buenos Aires)", "America/Buenos_Aires" },

        { "de", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "de", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "de", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "de", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "de", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "de", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "de", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "de", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "de", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "Argentinien (Buenos Aires)", "America/Buenos_Aires" },
        { "de", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "Argentinien (Buenos Aires)", "America/Buenos_Aires" },

        { "de", "America/Havana", "2004-01-15T00:00:00Z", "Z", "-0500", "-5:00" },
        { "de", "America/Havana", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-05:00", "-5:00" },
        { "de", "America/Havana", "2004-01-15T00:00:00Z", "z", "GMT-05:00", "-5:00" },
        { "de", "America/Havana", "2004-01-15T00:00:00Z", "zzzz", "GMT-05:00", "-5:00" },
        { "de", "America/Havana", "2004-07-15T00:00:00Z", "Z", "-0400", "-4:00" },
        { "de", "America/Havana", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-04:00", "-4:00" },
        { "de", "America/Havana", "2004-07-15T00:00:00Z", "z", "GMT-04:00", "-4:00" },
        { "de", "America/Havana", "2004-07-15T00:00:00Z", "zzzz", "GMT-04:00", "-4:00" },
        { "de", "America/Havana", "2004-07-15T00:00:00Z", "v", "Kuba", "America/Havana" },
        { "de", "America/Havana", "2004-07-15T00:00:00Z", "vvvv", "Kuba", "America/Havana" },
        // added to test proper fallback of country name
        { "de_CH", "America/Havana", "2004-07-15T00:00:00Z", "v", "Kuba", "America/Havana" },
        { "de_CH", "America/Havana", "2004-07-15T00:00:00Z", "vvvv", "Kuba", "America/Havana" },

        { "de", "Australia/ACT", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "de", "Australia/ACT", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+11:00", "+11:00" },
        { "de", "Australia/ACT", "2004-01-15T00:00:00Z", "z", "GMT+11:00", "+11:00" },
        { "de", "Australia/ACT", "2004-01-15T00:00:00Z", "zzzz", "GMT+11:00", "+11:00" },
        { "de", "Australia/ACT", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "de", "Australia/ACT", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+10:00", "+10:00" },
        { "de", "Australia/ACT", "2004-07-15T00:00:00Z", "z", "GMT+10:00", "+10:00" },
        { "de", "Australia/ACT", "2004-07-15T00:00:00Z", "zzzz", "GMT+10:00", "+10:00" },
        { "de", "Australia/ACT", "2004-07-15T00:00:00Z", "v", "Australien (Sydney)", "Australia/Sydney" },
        { "de", "Australia/ACT", "2004-07-15T00:00:00Z", "vvvv", "Australien (Sydney)", "Australia/Sydney" },

        { "de", "Australia/Sydney", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "de", "Australia/Sydney", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+11:00", "+11:00" },
        { "de", "Australia/Sydney", "2004-01-15T00:00:00Z", "z", "GMT+11:00", "+11:00" },
        { "de", "Australia/Sydney", "2004-01-15T00:00:00Z", "zzzz", "GMT+11:00", "+11:00" },
        { "de", "Australia/Sydney", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "de", "Australia/Sydney", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+10:00", "+10:00" },
        { "de", "Australia/Sydney", "2004-07-15T00:00:00Z", "z", "GMT+10:00", "+10:00" },
        { "de", "Australia/Sydney", "2004-07-15T00:00:00Z", "zzzz", "GMT+10:00", "+10:00" },
        { "de", "Australia/Sydney", "2004-07-15T00:00:00Z", "v", "Australien (Sydney)", "Australia/Sydney" },
        { "de", "Australia/Sydney", "2004-07-15T00:00:00Z", "vvvv", "Australien (Sydney)", "Australia/Sydney" },

        { "de", "Europe/London", "2004-01-15T00:00:00Z", "Z", "+0000", "+0:00" },
        { "de", "Europe/London", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+00:00", "+0:00" },
        { "de", "Europe/London", "2004-01-15T00:00:00Z", "z", "GMT+00:00", "+0:00" },
        { "de", "Europe/London", "2004-01-15T00:00:00Z", "zzzz", "GMT+00:00", "+0:00" },
        { "de", "Europe/London", "2004-07-15T00:00:00Z", "Z", "+0100", "+1:00" },
        { "de", "Europe/London", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+01:00", "+1:00" },
        { "de", "Europe/London", "2004-07-15T00:00:00Z", "z", "GMT+01:00", "+1:00" },
        { "de", "Europe/London", "2004-07-15T00:00:00Z", "zzzz", "GMT+01:00", "+1:00" },
        { "de", "Europe/London", "2004-07-15T00:00:00Z", "v", "Vereinigtes K\u00f6nigreich", "Europe/London" },
        { "de", "Europe/London", "2004-07-15T00:00:00Z", "vvvv", "Vereinigtes K\u00f6nigreich", "Europe/London" },

        { "de", "Etc/GMT+3", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "de", "Etc/GMT+3", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "de", "Etc/GMT+3", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "de", "Etc/GMT+3", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "de", "Etc/GMT+3", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "de", "Etc/GMT+3", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "de", "Etc/GMT+3", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "de", "Etc/GMT+3", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "de", "Etc/GMT+3", "2004-07-15T00:00:00Z", "v", "GMT-03:00", "-3:00" },
        { "de", "Etc/GMT+3", "2004-07-15T00:00:00Z", "vvvv", "GMT-03:00", "-3:00" },

        // JB#5150
        { "de", "Asia/Calcutta", "2004-01-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "de", "Asia/Calcutta", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+05:30", "+5:30" },
        { "de", "Asia/Calcutta", "2004-01-15T00:00:00Z", "z", "GMT+05:30", "+5:30" },
        { "de", "Asia/Calcutta", "2004-01-15T00:00:00Z", "zzzz", "GMT+05:30", "+5:30" },
        { "de", "Asia/Calcutta", "2004-07-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "de", "Asia/Calcutta", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+05:30", "+5:30" },
        { "de", "Asia/Calcutta", "2004-07-15T00:00:00Z", "z", "GMT+05:30", "+05:30" },
        { "de", "Asia/Calcutta", "2004-07-15T00:00:00Z", "zzzz", "GMT+05:30", "+5:30" },
        { "de", "Asia/Calcutta", "2004-07-15T00:00:00Z", "v", "Indien", "Asia/Calcutta" },
        { "de", "Asia/Calcutta", "2004-07-15T00:00:00Z", "vvvv", "Indien", "Asia/Calcutta" },

        // ==========

        { "zh", "America/Los_Angeles", "2004-01-15T00:00:00Z", "Z", "-0800", "-8:00" },
        { "zh", "America/Los_Angeles", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0800", "-8:00" },
        { "zh", "America/Los_Angeles", "2004-01-15T00:00:00Z", "z", GMT_ZH+"-0800", "America/Los_Angeles" },
        { "zh", "America/Los_Angeles", "2004-01-15T00:00:00Z", "zzzz", "\u592a\u5e73\u6d0b\u6807\u51c6\u65f6\u95f4", "America/Los_Angeles" },
        { "zh", "America/Los_Angeles", "2004-07-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "zh", "America/Los_Angeles", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0700", "-7:00" },
        { "zh", "America/Los_Angeles", "2004-07-15T00:00:00Z", "z", GMT_ZH+"-0700", "America/Los_Angeles" },
        { "zh", "America/Los_Angeles", "2004-07-15T00:00:00Z", "zzzz", "\u592a\u5e73\u6d0b\u590f\u4ee4\u65f6\u95f4", "America/Los_Angeles" },
    // icu zh.txt has exemplar city for this time zone
        { "zh", "America/Los_Angeles", "2004-07-15T00:00:00Z", "v", "\u7f8e\u56fd (\u6d1b\u6749\u77f6)", "America/Los_Angeles" },
        { "zh", "America/Los_Angeles", "2004-07-15T00:00:00Z", "vvvv", "\u7f8e\u56fd\u592a\u5e73\u6d0b\u65f6\u95f4", "America/Los_Angeles" },

        { "zh", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "zh", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0300", "-3:00" },
        { "zh", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "z", GMT_ZH+"-0300", "-3:00" },
        { "zh", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "\u963f\u6839\u5ef7\u6807\u51c6\u65f6\u95f4", "-3:00" },
        { "zh", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "zh", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0300", "-3:00" },
        { "zh", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "z", GMT_ZH+"-0300", "-3:00" },
        { "zh", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "\u963f\u6839\u5ef7\u6807\u51c6\u65f6\u95f4", "-3:00" },
    // icu zh.txt does not have info for this time zone
        { "zh", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "\u963f\u6839\u5ef7 (\u5e03\u5b9c\u8bfa\u65af\u827e\u5229\u65af)", "America/Buenos_Aires" },
        { "zh", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "\u963f\u6839\u5ef7\u6807\u51c6\u65f6\u95f4", "America/Buenos_Aires" },

        { "zh", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "zh", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0300", "-3:00" },
        { "zh", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "z", GMT_ZH+"-0300", "-3:00" },
        { "zh", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "\u963f\u6839\u5ef7\u6807\u51c6\u65f6\u95f4", "-3:00" },
        { "zh", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "zh", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0300", "-3:00" },
        { "zh", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "z", GMT_ZH+"-0300", "-3:00" },
        { "zh", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "\u963f\u6839\u5ef7\u6807\u51c6\u65f6\u95f4", "-3:00" },
        { "zh", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "\u963f\u6839\u5ef7 (\u5e03\u5b9c\u8bfa\u65af\u827e\u5229\u65af)", "America/Buenos_Aires" },
        { "zh", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "\u963f\u6839\u5ef7\u6807\u51c6\u65f6\u95f4", "America/Buenos_Aires" },

        { "zh", "America/Havana", "2004-01-15T00:00:00Z", "Z", "-0500", "-5:00" },
        { "zh", "America/Havana", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0500", "-5:00" },
        { "zh", "America/Havana", "2004-01-15T00:00:00Z", "z", GMT_ZH+"-0500", "-5:00" },
        { "zh", "America/Havana", "2004-01-15T00:00:00Z", "zzzz", "\u53e4\u5df4\u6807\u51c6\u65f6\u95f4", "-5:00" },
        { "zh", "America/Havana", "2004-07-15T00:00:00Z", "Z", "-0400", "-4:00" },
        { "zh", "America/Havana", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0400", "-4:00" },
        { "zh", "America/Havana", "2004-07-15T00:00:00Z", "z", GMT_ZH+"-0400", "-4:00" },
        { "zh", "America/Havana", "2004-07-15T00:00:00Z", "zzzz", "\u53e4\u5df4\u590f\u4ee4\u65f6\u95f4", "-4:00" },
        { "zh", "America/Havana", "2004-07-15T00:00:00Z", "v", "\u53e4\u5df4\u65f6\u95f4", "America/Havana" },
        { "zh", "America/Havana", "2004-07-15T00:00:00Z", "vvvv", "\u53e4\u5df4\u65f6\u95f4", "America/Havana" },

        { "zh", "Australia/ACT", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "zh", "Australia/ACT", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"+1100", "+11:00" },
        { "zh", "Australia/ACT", "2004-01-15T00:00:00Z", "z", GMT_ZH+"+1100", "+11:00" },
        { "zh", "Australia/ACT", "2004-01-15T00:00:00Z", "zzzz", "\u6fb3\u5927\u5229\u4e9a\u4e1c\u90e8\u590f\u4ee4\u65f6\u95f4", "+11:00" },
        { "zh", "Australia/ACT", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "zh", "Australia/ACT", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"+1000", "+10:00" },
        { "zh", "Australia/ACT", "2004-07-15T00:00:00Z", "z", GMT_ZH+"+1000", "+10:00" },
        { "zh", "Australia/ACT", "2004-07-15T00:00:00Z", "zzzz", "\u6fb3\u5927\u5229\u4e9a\u4e1c\u90e8\u6807\u51c6\u65f6\u95f4", "+10:00" },
        { "zh", "Australia/ACT", "2004-07-15T00:00:00Z", "v", "\u6fb3\u5927\u5229\u4e9a (\u6089\u5c3c)", "Australia/Sydney" },
        { "zh", "Australia/ACT", "2004-07-15T00:00:00Z", "vvvv", "\u6fb3\u5927\u5229\u4e9a\u4e1c\u90e8\u65f6\u95f4", "Australia/Sydney" },

        { "zh", "Australia/Sydney", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "zh", "Australia/Sydney", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"+1100", "+11:00" },
        { "zh", "Australia/Sydney", "2004-01-15T00:00:00Z", "z", GMT_ZH+"+1100", "+11:00" },
        { "zh", "Australia/Sydney", "2004-01-15T00:00:00Z", "zzzz", "\u6fb3\u5927\u5229\u4e9a\u4e1c\u90e8\u590f\u4ee4\u65f6\u95f4", "+11:00" },
        { "zh", "Australia/Sydney", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "zh", "Australia/Sydney", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"+1000", "+10:00" },
        { "zh", "Australia/Sydney", "2004-07-15T00:00:00Z", "z", GMT_ZH+"+1000", "+10:00" },
        { "zh", "Australia/Sydney", "2004-07-15T00:00:00Z", "zzzz", "\u6fb3\u5927\u5229\u4e9a\u4e1c\u90e8\u6807\u51c6\u65f6\u95f4",  "+10:00" },
        { "zh", "Australia/Sydney", "2004-07-15T00:00:00Z", "v", "\u6fb3\u5927\u5229\u4e9a (\u6089\u5c3c)", "Australia/Sydney" },
        { "zh", "Australia/Sydney", "2004-07-15T00:00:00Z", "vvvv", "\u6fb3\u5927\u5229\u4e9a\u4e1c\u90e8\u65f6\u95f4", "Australia/Sydney" },

        { "zh", "Europe/London", "2004-01-15T00:00:00Z", "Z", "+0000", "+0:00" },
        { "zh", "Europe/London", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"+0000", "+0:00" },
        { "zh", "Europe/London", "2004-01-15T00:00:00Z", "z", GMT_ZH+"+0000", "+0:00" },
        { "zh", "Europe/London", "2004-01-15T00:00:00Z", "V", "GMT", "+0:00" },
        { "zh", "Europe/London", "2004-01-15T00:00:00Z", "zzzz", "\u683C\u6797\u5C3C\u6CBB\u6807\u51C6\u65F6\u95F4", "+0:00" },
        { "zh", "Europe/London", "2004-07-15T00:00:00Z", "Z", "+0100", "+1:00" },
        { "zh", "Europe/London", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"+0100", "+1:00" },
        { "zh", "Europe/London", "2004-07-15T00:00:00Z", "z", GMT_ZH+"+0100", "+1:00" },
        { "zh", "Europe/London", "2004-07-15T00:00:00Z", "V", "BST", "+1:00" },
        { "zh", "Europe/London", "2004-07-15T00:00:00Z", "zzzz", GMT_ZH+"+0100", "+1:00" },
        { "zh", "Europe/London", "2004-07-15T00:00:00Z", "v", "\u82f1\u56fd\u65f6\u95f4", "Europe/London" },
        { "zh", "Europe/London", "2004-07-15T00:00:00Z", "vvvv", "\u82f1\u56fd\u65f6\u95f4", "Europe/London" },
        { "zh", "Europe/London", "2004-07-15T00:00:00Z", "VVVV", "\u82f1\u56fd\u65f6\u95f4", "Europe/London" },

        { "zh", "Etc/GMT+3", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-01-15T00:00:00Z", "z", GMT_ZH+"-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-01-15T00:00:00Z", "zzzz", GMT_ZH+"-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-07-15T00:00:00Z", "z", GMT_ZH+"-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-07-15T00:00:00Z", "zzzz", GMT_ZH+"-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-07-15T00:00:00Z", "v", GMT_ZH+"-0300", "-3:00" },
        { "zh", "Etc/GMT+3", "2004-07-15T00:00:00Z", "vvvv", GMT_ZH+"-0300", "-3:00" },

        // JB#5150
        { "zh", "Asia/Calcutta", "2004-01-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "zh", "Asia/Calcutta", "2004-01-15T00:00:00Z", "ZZZZ", GMT_ZH+"+0530", "+5:30" },
        { "zh", "Asia/Calcutta", "2004-01-15T00:00:00Z", "z", GMT_ZH+"+0530", "+5:30" },
        { "zh", "Asia/Calcutta", "2004-01-15T00:00:00Z", "zzzz", "\u5370\u5ea6\u6807\u51c6\u65f6\u95f4", "+5:30" },
        { "zh", "Asia/Calcutta", "2004-07-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "zh", "Asia/Calcutta", "2004-07-15T00:00:00Z", "ZZZZ", GMT_ZH+"+0530", "+5:30" },
        { "zh", "Asia/Calcutta", "2004-07-15T00:00:00Z", "z", GMT_ZH+"+0530", "+05:30" },
        { "zh", "Asia/Calcutta", "2004-07-15T00:00:00Z", "zzzz", "\u5370\u5ea6\u6807\u51c6\u65f6\u95f4", "+5:30" },
        { "zh", "Asia/Calcutta", "2004-07-15T00:00:00Z", "v", "\u5370\u5ea6\u65f6\u95f4", "Asia/Calcutta" },
        { "zh", "Asia/Calcutta", "2004-07-15T00:00:00Z", "vvvv", "\u5370\u5EA6\u6807\u51c6\u65f6\u95f4", "Asia/Calcutta" },

        // ==========

        { "hi", "America/Los_Angeles", "2004-01-15T00:00:00Z", "Z", "-0800", "-8:00" },
        { "hi", "America/Los_Angeles", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u096e:\u0966\u0966", "-8:00" },
        { "hi", "America/Los_Angeles", "2004-01-15T00:00:00Z", "z", "GMT-\u0966\u096e:\u0966\u0966", "-8:00" },
        { "hi", "America/Los_Angeles", "2004-01-15T00:00:00Z", "zzzz", "GMT-\u0966\u096e:\u0966\u0966", "-8:00" },
        { "hi", "America/Los_Angeles", "2004-07-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "hi", "America/Los_Angeles", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u096d:\u0966\u0966", "-7:00" },
        { "hi", "America/Los_Angeles", "2004-07-15T00:00:00Z", "z", "GMT-\u0966\u096d:\u0966\u0966", "-7:00" },
        { "hi", "America/Los_Angeles", "2004-07-15T00:00:00Z", "zzzz", "GMT-\u0966\u096d:\u0966\u0966", "-7:00" },
        { "hi", "America/Los_Angeles", "2004-07-15T00:00:00Z", "v", "\u0938\u0902\u092f\u0941\u0915\u094d\u0924 \u0930\u093e\u091c\u094d\u092f \u0905\u092e\u0947\u0930\u093f\u0915\u093e (\u0932\u094b\u0938 \u090f\u0902\u091c\u093f\u0932\u0947\u0938)", "America/Los_Angeles" },
        { "hi", "America/Los_Angeles", "2004-07-15T00:00:00Z", "vvvv", "\u0938\u0902\u092f\u0941\u0915\u094d\u0924 \u0930\u093e\u091c\u094d\u092f \u0905\u092e\u0947\u0930\u093f\u0915\u093e (\u0932\u094b\u0938 \u090f\u0902\u091c\u093f\u0932\u0947\u0938)", "America/Los_Angeles" },

        { "hi", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "\u0905\u0930\u094d\u091c\u0947\u0928\u094d\u091f\u0940\u0928\u093e (\u092c\u094d\u092f\u0942\u0928\u0938 \u0906\u092f\u0930\u0938)", "America/Buenos_Aires" },
        { "hi", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "\u0905\u0930\u094d\u091c\u0947\u0928\u094d\u091f\u0940\u0928\u093e (\u092c\u094d\u092f\u0942\u0928\u0938 \u0906\u092f\u0930\u0938)", "America/Buenos_Aires" },

        { "hi", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "hi", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "hi", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "\u0905\u0930\u094d\u091c\u0947\u0928\u094d\u091f\u0940\u0928\u093e (\u092c\u094d\u092f\u0942\u0928\u0938 \u0906\u092f\u0930\u0938)", "America/Buenos_Aires" },
        { "hi", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "\u0905\u0930\u094d\u091c\u0947\u0928\u094d\u091f\u0940\u0928\u093e (\u092c\u094d\u092f\u0942\u0928\u0938 \u0906\u092f\u0930\u0938)", "America/Buenos_Aires" },

        { "hi", "America/Havana", "2004-01-15T00:00:00Z", "Z", "-0500", "-5:00" },
        { "hi", "America/Havana", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u096b:\u0966\u0966", "-5:00" },
        { "hi", "America/Havana", "2004-01-15T00:00:00Z", "z", "GMT-\u0966\u096b:\u0966\u0966", "-5:00" },
        { "hi", "America/Havana", "2004-01-15T00:00:00Z", "zzzz", "GMT-\u0966\u096b:\u0966\u0966", "-5:00" },
        { "hi", "America/Havana", "2004-07-15T00:00:00Z", "Z", "-0400", "-4:00" },
        { "hi", "America/Havana", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u096a:\u0966\u0966", "-4:00" },
        { "hi", "America/Havana", "2004-07-15T00:00:00Z", "z", "GMT-\u0966\u096a:\u0966\u0966", "-4:00" },
        { "hi", "America/Havana", "2004-07-15T00:00:00Z", "zzzz", "GMT-\u0966\u096a:\u0966\u0966", "-4:00" },
        { "hi", "America/Havana", "2004-07-15T00:00:00Z", "v", "\u0915\u094d\u092f\u0942\u092c\u093e", "America/Havana" },
        { "hi", "America/Havana", "2004-07-15T00:00:00Z", "vvvv", "\u0915\u094d\u092f\u0942\u092c\u093e", "America/Havana" },

        { "hi", "Australia/ACT", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "hi", "Australia/ACT", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+\u0967\u0967:\u0966\u0966", "+11:00" },
        { "hi", "Australia/ACT", "2004-01-15T00:00:00Z", "z", "GMT+\u0967\u0967:\u0966\u0966", "+11:00" },
        { "hi", "Australia/ACT", "2004-01-15T00:00:00Z", "zzzz", "GMT+\u0967\u0967:\u0966\u0966", "+11:00" },
        { "hi", "Australia/ACT", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "hi", "Australia/ACT", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+\u0967\u0966:\u0966\u0966", "+10:00" },
        { "hi", "Australia/ACT", "2004-07-15T00:00:00Z", "z", "GMT+\u0967\u0966:\u0966\u0966", "+10:00" },
        { "hi", "Australia/ACT", "2004-07-15T00:00:00Z", "zzzz", "GMT+\u0967\u0966:\u0966\u0966", "+10:00" },
        { "hi", "Australia/ACT", "2004-07-15T00:00:00Z", "v", "\u0911\u0938\u094d\u091f\u094d\u0930\u0947\u0932\u093f\u092f\u093e (\u0938\u093f\u0921\u0928\u0940)", "Australia/Sydney" },
        { "hi", "Australia/ACT", "2004-07-15T00:00:00Z", "vvvv", "\u0911\u0938\u094d\u091f\u094d\u0930\u0947\u0932\u093f\u092f\u093e (\u0938\u093f\u0921\u0928\u0940)", "Australia/Sydney" },

        { "hi", "Australia/Sydney", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "hi", "Australia/Sydney", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+\u0967\u0967:\u0966\u0966", "+11:00" },
        { "hi", "Australia/Sydney", "2004-01-15T00:00:00Z", "z", "GMT+\u0967\u0967:\u0966\u0966", "+11:00" },
        { "hi", "Australia/Sydney", "2004-01-15T00:00:00Z", "zzzz", "GMT+\u0967\u0967:\u0966\u0966", "+11:00" },
        { "hi", "Australia/Sydney", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "hi", "Australia/Sydney", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+\u0967\u0966:\u0966\u0966", "+10:00" },
        { "hi", "Australia/Sydney", "2004-07-15T00:00:00Z", "z", "GMT+\u0967\u0966:\u0966\u0966", "+10:00" },
        { "hi", "Australia/Sydney", "2004-07-15T00:00:00Z", "zzzz", "GMT+\u0967\u0966:\u0966\u0966", "+10:00" },
        { "hi", "Australia/Sydney", "2004-07-15T00:00:00Z", "v", "\u0911\u0938\u094d\u091f\u094d\u0930\u0947\u0932\u093f\u092f\u093e (\u0938\u093f\u0921\u0928\u0940)", "Australia/Sydney" },
        { "hi", "Australia/Sydney", "2004-07-15T00:00:00Z", "vvvv", "\u0911\u0938\u094d\u091f\u094d\u0930\u0947\u0932\u093f\u092f\u093e (\u0938\u093f\u0921\u0928\u0940)", "Australia/Sydney" },

        { "hi", "Europe/London", "2004-01-15T00:00:00Z", "Z", "+0000", "+0:00" },
        { "hi", "Europe/London", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+\u0966\u0966:\u0966\u0966", "+0:00" },
        { "hi", "Europe/London", "2004-01-15T00:00:00Z", "z", "GMT+\u0966\u0966:\u0966\u0966", "+0:00" },
        { "hi", "Europe/London", "2004-01-15T00:00:00Z", "zzzz", "GMT+\u0966\u0966:\u0966\u0966", "+0:00" },
        { "hi", "Europe/London", "2004-07-15T00:00:00Z", "Z", "+0100", "+1:00" },
        { "hi", "Europe/London", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+\u0966\u0967:\u0966\u0966", "+1:00" },
        { "hi", "Europe/London", "2004-07-15T00:00:00Z", "z", "GMT+\u0966\u0967:\u0966\u0966", "+1:00" },
        { "hi", "Europe/London", "2004-07-15T00:00:00Z", "zzzz", "GMT+\u0966\u0967:\u0966\u0966", "+1:00" },
        { "hi", "Europe/London", "2004-07-15T00:00:00Z", "v", "\u092C\u094D\u0930\u093F\u0924\u0928", "Europe/London" },
        { "hi", "Europe/London", "2004-07-15T00:00:00Z", "vvvv", "\u092C\u094D\u0930\u093F\u0924\u0928", "Europe/London" },

        { "hi", "Etc/GMT+3", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-01-15T00:00:00Z", "z", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-01-15T00:00:00Z", "zzzz", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-07-15T00:00:00Z", "z", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-07-15T00:00:00Z", "zzzz", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-07-15T00:00:00Z", "v", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },
        { "hi", "Etc/GMT+3", "2004-07-15T00:00:00Z", "vvvv", "GMT-\u0966\u0969:\u0966\u0966", "-3:00" },

        { "hi", "Asia/Calcutta", "2004-01-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "hi", "Asia/Calcutta", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+\u0966\u096B:\u0969\u0966", "+5:30" },
        { "hi", "Asia/Calcutta", "2004-01-15T00:00:00Z", "z", "IST", "+5:30" },
        { "hi", "Asia/Calcutta", "2004-01-15T00:00:00Z", "zzzz", "\u092D\u093E\u0930\u0924\u0940\u092F \u0938\u092E\u092F", "+5:30" },
        { "hi", "Asia/Calcutta", "2004-07-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "hi", "Asia/Calcutta", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+\u0966\u096B:\u0969\u0966", "+5:30" },
        { "hi", "Asia/Calcutta", "2004-07-15T00:00:00Z", "z", "IST", "+05:30" },
        { "hi", "Asia/Calcutta", "2004-07-15T00:00:00Z", "zzzz", "\u092D\u093E\u0930\u0924\u0940\u092F \u0938\u092E\u092F", "+5:30" },
        { "hi", "Asia/Calcutta", "2004-07-15T00:00:00Z", "v", "IST", "Asia/Calcutta" },
        { "hi", "Asia/Calcutta", "2004-07-15T00:00:00Z", "vvvv", "\u092D\u093E\u0930\u0924\u0940\u092F \u0938\u092E\u092F", "Asia/Calcutta" },

        // ==========

        { "bg", "America/Los_Angeles", "2004-01-15T00:00:00Z", "Z", "-0800", "-8:00" },
        { "bg", "America/Los_Angeles", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"-0800", "-8:00" },
        { "bg", "America/Los_Angeles", "2004-01-15T00:00:00Z", "z", GMT_BG+"-0800", "America/Los_Angeles" },
        { "bg", "America/Los_Angeles", "2004-01-15T00:00:00Z", "V", "PST", "America/Los_Angeles" },
        { "bg", "America/Los_Angeles", "2004-01-15T00:00:00Z", "zzzz", "\u0422\u0438\u0445\u043E\u043E\u043A\u0435\u0430\u043D\u0441\u043A\u0430 \u0447\u0430\u0441\u043E\u0432\u0430 \u0437\u043E\u043D\u0430", "America/Los_Angeles" },
        { "bg", "America/Los_Angeles", "2004-07-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "bg", "America/Los_Angeles", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"-0700", "-7:00" },
        { "bg", "America/Los_Angeles", "2004-07-15T00:00:00Z", "z", GMT_BG+"-0700", "America/Los_Angeles" },
        { "bg", "America/Los_Angeles", "2004-07-15T00:00:00Z", "V", "PDT", "America/Los_Angeles" },
        { "bg", "America/Los_Angeles", "2004-07-15T00:00:00Z", "zzzz", "\u0422\u0438\u0445\u043E\u043E\u043A\u0435\u0430\u043D\u0441\u043A\u0430 \u043B\u044F\u0442\u043D\u0430 \u0447\u0430\u0441\u043E\u0432\u0430 \u0437\u043E\u043D\u0430", "America/Los_Angeles" },
    // icu bg.txt has exemplar city for this time zone
        { "bg", "America/Los_Angeles", "2004-07-15T00:00:00Z", "v", "\u0421\u0410\u0429 (\u041b\u043e\u0441 \u0410\u043d\u0436\u0435\u043b\u0438\u0441)", "America/Los_Angeles" },
        { "bg", "America/Los_Angeles", "2004-07-15T00:00:00Z", "vvvv", "\u0421\u0410\u0429 (\u041b\u043e\u0441 \u0410\u043d\u0436\u0435\u043b\u0438\u0441)", "America/Los_Angeles" },
        { "bg", "America/Los_Angeles", "2004-07-15T00:00:00Z", "VVVV", "\u0421\u0410\u0429 (\u041b\u043e\u0441 \u0410\u043d\u0436\u0435\u043b\u0438\u0441)", "America/Los_Angeles" },

        { "bg", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "z", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "z", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "\u0410\u0440\u0436\u0435\u043d\u0442\u0438\u043d\u0430 (\u0411\u0443\u0435\u043D\u043E\u0441 \u0410\u0439\u0440\u0435\u0441)", "America/Buenos_Aires" },
        { "bg", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "\u0410\u0440\u0436\u0435\u043d\u0442\u0438\u043d\u0430 (\u0411\u0443\u0435\u043D\u043E\u0441 \u0410\u0439\u0440\u0435\u0441)", "America/Buenos_Aires" },

        { "bg", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "bg", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "z", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "bg", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "z", GMT_BG+"-0300", "-3:00" },
        { "bg", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", GMT_BG+"-0300", "-3:00" },
    // icu bg.txt does not have info for this time zone
        { "bg", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "\u0410\u0440\u0436\u0435\u043d\u0442\u0438\u043d\u0430 (\u0411\u0443\u0435\u043D\u043E\u0441 \u0410\u0439\u0440\u0435\u0441)", "America/Buenos_Aires" },
        { "bg", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "\u0410\u0440\u0436\u0435\u043d\u0442\u0438\u043d\u0430 (\u0411\u0443\u0435\u043D\u043E\u0441 \u0410\u0439\u0440\u0435\u0441)", "America/Buenos_Aires" },

        { "bg", "America/Havana", "2004-01-15T00:00:00Z", "Z", "-0500", "-5:00" },
        { "bg", "America/Havana", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"-0500", "-5:00" },
        { "bg", "America/Havana", "2004-01-15T00:00:00Z", "z", GMT_BG+"-0500", "-5:00" },
        { "bg", "America/Havana", "2004-01-15T00:00:00Z", "zzzz", GMT_BG+"-0500", "-5:00" },
        { "bg", "America/Havana", "2004-07-15T00:00:00Z", "Z", "-0400", "-4:00" },
        { "bg", "America/Havana", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"-0400", "-4:00" },
        { "bg", "America/Havana", "2004-07-15T00:00:00Z", "z", GMT_BG+"-0400", "-4:00" },
        { "bg", "America/Havana", "2004-07-15T00:00:00Z", "zzzz", GMT_BG+"-0400", "-4:00" },
        { "bg", "America/Havana", "2004-07-15T00:00:00Z", "v", "\u041a\u0443\u0431\u0430", "America/Havana" },
        { "bg", "America/Havana", "2004-07-15T00:00:00Z", "vvvv", "\u041a\u0443\u0431\u0430", "America/Havana" },

        { "bg", "Australia/ACT", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "bg", "Australia/ACT", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"+1100", "+11:00" },
        { "bg", "Australia/ACT", "2004-01-15T00:00:00Z", "z", GMT_BG+"+1100", "+11:00" },
        { "bg", "Australia/ACT", "2004-01-15T00:00:00Z", "zzzz", GMT_BG+"+1100", "+11:00" },
        { "bg", "Australia/ACT", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "bg", "Australia/ACT", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"+1000", "+10:00" },
        { "bg", "Australia/ACT", "2004-07-15T00:00:00Z", "z", GMT_BG+"+1000", "+10:00" },
        { "bg", "Australia/ACT", "2004-07-15T00:00:00Z", "zzzz", GMT_BG+"+1000", "+10:00" },
        { "bg", "Australia/ACT", "2004-07-15T00:00:00Z", "v", "\u0410\u0432\u0441\u0442\u0440\u0430\u043b\u0438\u044f (\u0421\u0438\u0434\u043D\u0438)", "Australia/Sydney" },
        { "bg", "Australia/ACT", "2004-07-15T00:00:00Z", "vvvv", "\u0410\u0432\u0441\u0442\u0440\u0430\u043b\u0438\u044f (\u0421\u0438\u0434\u043D\u0438)", "Australia/Sydney" },

        { "bg", "Australia/Sydney", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "bg", "Australia/Sydney", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"+1100", "+11:00" },
        { "bg", "Australia/Sydney", "2004-01-15T00:00:00Z", "z", GMT_BG+"+1100", "+11:00" },
        { "bg", "Australia/Sydney", "2004-01-15T00:00:00Z", "zzzz", GMT_BG+"+1100", "+11:00" },
        { "bg", "Australia/Sydney", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "bg", "Australia/Sydney", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"+1000", "+10:00" },
        { "bg", "Australia/Sydney", "2004-07-15T00:00:00Z", "z", GMT_BG+"+1000", "+10:00" },
        { "bg", "Australia/Sydney", "2004-07-15T00:00:00Z", "zzzz", GMT_BG+"+1000", "+10:00" },
        { "bg", "Australia/Sydney", "2004-07-15T00:00:00Z", "v", "\u0410\u0432\u0441\u0442\u0440\u0430\u043b\u0438\u044f (\u0421\u0438\u0434\u043D\u0438)", "Australia/Sydney" },
        { "bg", "Australia/Sydney", "2004-07-15T00:00:00Z", "vvvv", "\u0410\u0432\u0441\u0442\u0440\u0430\u043b\u0438\u044f (\u0421\u0438\u0434\u043D\u0438)", "Australia/Sydney" },

        { "bg", "Europe/London", "2004-01-15T00:00:00Z", "Z", "+0000", "+0:00" },
        { "bg", "Europe/London", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"+0000", "+0:00" },
        { "bg", "Europe/London", "2004-01-15T00:00:00Z", "z", GMT_BG+"+0000", "+0:00" },
        { "bg", "Europe/London", "2004-01-15T00:00:00Z", "zzzz", "\u0427\u0430\u0441\u043E\u0432\u0430 \u0437\u043E\u043D\u0430 \u0413\u0440\u0438\u043D\u0443\u0438\u0447", "+0:00" },
        { "bg", "Europe/London", "2004-07-15T00:00:00Z", "Z", "+0100", "+1:00" },
        { "bg", "Europe/London", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"+0100", "+1:00" },
        { "bg", "Europe/London", "2004-07-15T00:00:00Z", "z", GMT_BG+"+0100", "+1:00" },
        { "bg", "Europe/London", "2004-07-15T00:00:00Z", "zzzz", GMT_BG+"+0100", "+1:00" },
        { "bg", "Europe/London", "2004-07-15T00:00:00Z", "v", "\u041e\u0431\u0435\u0434\u0438\u043d\u0435\u043d\u043e \u043a\u0440\u0430\u043b\u0441\u0442\u0432\u043e", "Europe/London" },
        { "bg", "Europe/London", "2004-07-15T00:00:00Z", "vvvv", "\u041e\u0431\u0435\u0434\u0438\u043d\u0435\u043d\u043e \u043a\u0440\u0430\u043b\u0441\u0442\u0432\u043e", "Europe/London" },

        { "bg", "Etc/GMT+3", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-01-15T00:00:00Z", "z", GMT_BG+"-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-01-15T00:00:00Z", "zzzz", GMT_BG+"-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-07-15T00:00:00Z", "z", GMT_BG+"-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-07-15T00:00:00Z", "zzzz", GMT_BG+"-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-07-15T00:00:00Z", "v", GMT_BG+"-0300", "-3:00" },
        { "bg", "Etc/GMT+3", "2004-07-15T00:00:00Z", "vvvv", GMT_BG+"-0300", "-3:00" },

        // JB#5150
        { "bg", "Asia/Calcutta", "2004-01-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "bg", "Asia/Calcutta", "2004-01-15T00:00:00Z", "ZZZZ", GMT_BG+"+0530", "+5:30" },
        { "bg", "Asia/Calcutta", "2004-01-15T00:00:00Z", "z", GMT_BG+"+0530", "+5:30" },
        { "bg", "Asia/Calcutta", "2004-01-15T00:00:00Z", "zzzz", GMT_BG+"+0530", "+5:30" },
        { "bg", "Asia/Calcutta", "2004-07-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "bg", "Asia/Calcutta", "2004-07-15T00:00:00Z", "ZZZZ", GMT_BG+"+0530", "+5:30" },
        { "bg", "Asia/Calcutta", "2004-07-15T00:00:00Z", "z", GMT_BG+"+0530", "+05:30" },
        { "bg", "Asia/Calcutta", "2004-07-15T00:00:00Z", "zzzz", GMT_BG+"+0530", "+5:30" },
        { "bg", "Asia/Calcutta", "2004-07-15T00:00:00Z", "v", "\u0418\u043D\u0434\u0438\u044F", "Asia/Calcutta" },
        { "bg", "Asia/Calcutta", "2004-07-15T00:00:00Z", "vvvv", "\u0418\u043D\u0434\u0438\u044F", "Asia/Calcutta" },

    // ==========

        { "ja", "America/Los_Angeles", "2004-01-15T00:00:00Z", "Z", "-0800", "-8:00" },
        { "ja", "America/Los_Angeles", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-08:00", "-8:00" },
        { "ja", "America/Los_Angeles", "2004-01-15T00:00:00Z", "z", "GMT-08:00", "America/Los_Angeles" },
        { "ja", "America/Los_Angeles", "2004-01-15T00:00:00Z", "V", "PST", "America/Los_Angeles" },
        { "ja", "America/Los_Angeles", "2004-01-15T00:00:00Z", "zzzz", "\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u6a19\u6e96\u6642", "America/Los_Angeles" },
        { "ja", "America/Los_Angeles", "2004-07-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "ja", "America/Los_Angeles", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-07:00", "-7:00" },
        { "ja", "America/Los_Angeles", "2004-07-15T00:00:00Z", "z", "GMT-07:00", "America/Los_Angeles" },
        { "ja", "America/Los_Angeles", "2004-07-15T00:00:00Z", "V", "PDT", "America/Los_Angeles" },
        { "ja", "America/Los_Angeles", "2004-07-15T00:00:00Z", "zzzz", "\u30a2\u30e1\u30ea\u30ab\u592a\u5e73\u6d0b\u590f\u6642\u9593", "America/Los_Angeles" },
    // icu ja.txt has exemplar city for this time zone
        { "ja", "America/Los_Angeles", "2004-07-15T00:00:00Z", "v", "\u30A2\u30E1\u30EA\u30AB\u5408\u8846\u56FD (\u30ed\u30b5\u30f3\u30bc\u30eb\u30b9)", "America/Los_Angeles" },
        { "ja", "America/Los_Angeles", "2004-07-15T00:00:00Z", "vvvv", "\u30A2\u30E1\u30EA\u30AB\u592A\u5E73\u6D0B\u6642\u9593", "America/Los_Angeles" },
        { "ja", "America/Los_Angeles", "2004-07-15T00:00:00Z", "VVVV", "\u30A2\u30E1\u30EA\u30AB\u5408\u8846\u56FD (\u30ed\u30b5\u30f3\u30bc\u30eb\u30b9)", "America/Los_Angeles" },

        { "ja", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "ja", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "ja", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "ja", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "ja", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "ja", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "ja", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "ja", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
    // icu ja.txt does not have info for this time zone
        { "ja", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "\u30a2\u30eb\u30bc\u30f3\u30c1\u30f3 (\u30D6\u30A8\u30CE\u30B9\u30A2\u30A4\u30EC\u30B9)", "America/Buenos_Aires" },
        { "ja", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "\u30a2\u30eb\u30bc\u30f3\u30c1\u30f3\u6642\u9593", "America/Buenos_Aires" },

        { "ja", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "ja", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "ja", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "ja", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "ja", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "ja", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "ja", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "ja", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
    // icu ja.txt does not have info for this time zone
        { "ja", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "\u30a2\u30eb\u30bc\u30f3\u30c1\u30f3 (\u30D6\u30A8\u30CE\u30B9\u30A2\u30A4\u30EC\u30B9)", "America/Buenos_Aires" },
        { "ja", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "\u30a2\u30eb\u30bc\u30f3\u30c1\u30f3\u6642\u9593", "America/Buenos_Aires" },

        { "ja", "America/Havana", "2004-01-15T00:00:00Z", "Z", "-0500", "-5:00" },
        { "ja", "America/Havana", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-05:00", "-5:00" },
        { "ja", "America/Havana", "2004-01-15T00:00:00Z", "z", "GMT-05:00", "-5:00" },
        { "ja", "America/Havana", "2004-01-15T00:00:00Z", "zzzz", "GMT-05:00", "-5:00" },
        { "ja", "America/Havana", "2004-07-15T00:00:00Z", "Z", "-0400", "-4:00" },
        { "ja", "America/Havana", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-04:00", "-4:00" },
        { "ja", "America/Havana", "2004-07-15T00:00:00Z", "z", "GMT-04:00", "-4:00" },
        { "ja", "America/Havana", "2004-07-15T00:00:00Z", "zzzz", "GMT-04:00", "-4:00" },
        { "ja", "America/Havana", "2004-07-15T00:00:00Z", "v", "\u30ad\u30e5\u30fc\u30d0\u6642\u9593", "America/Havana" },
        { "ja", "America/Havana", "2004-07-15T00:00:00Z", "vvvv", "\u30ad\u30e5\u30fc\u30d0\u6642\u9593", "America/Havana" },

        { "ja", "Australia/ACT", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "ja", "Australia/ACT", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+11:00", "+11:00" },
        { "ja", "Australia/ACT", "2004-01-15T00:00:00Z", "z", "GMT+11:00", "+11:00" },
        { "ja", "Australia/ACT", "2004-01-15T00:00:00Z", "zzzz", "GMT+11:00", "+11:00" },
        { "ja", "Australia/ACT", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "ja", "Australia/ACT", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+10:00", "+10:00" },
        { "ja", "Australia/ACT", "2004-07-15T00:00:00Z", "z", "GMT+10:00", "+10:00" },
        { "ja", "Australia/ACT", "2004-07-15T00:00:00Z", "zzzz", "GMT+10:00", "+10:00" },
    // icu ja.txt does not have info for this time zone
        { "ja", "Australia/ACT", "2004-07-15T00:00:00Z", "v", "\u30aa\u30fc\u30b9\u30c8\u30e9\u30ea\u30a2 (\u30b7\u30c9\u30cb\u30fc)", "Australia/Sydney" },
        { "ja", "Australia/ACT", "2004-07-15T00:00:00Z", "vvvv", "\u30aa\u30fc\u30b9\u30c8\u30e9\u30ea\u30a2 (\u30b7\u30c9\u30cb\u30fc)", "Australia/Sydney" },

        { "ja", "Australia/Sydney", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "ja", "Australia/Sydney", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+11:00", "+11:00" },
        { "ja", "Australia/Sydney", "2004-01-15T00:00:00Z", "z", "GMT+11:00", "+11:00" },
        { "ja", "Australia/Sydney", "2004-01-15T00:00:00Z", "zzzz", "GMT+11:00", "+11:00" },
        { "ja", "Australia/Sydney", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "ja", "Australia/Sydney", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+10:00", "+10:00" },
        { "ja", "Australia/Sydney", "2004-07-15T00:00:00Z", "z", "GMT+10:00", "+10:00" },
        { "ja", "Australia/Sydney", "2004-07-15T00:00:00Z", "zzzz", "GMT+10:00", "+10:00" },
        { "ja", "Australia/Sydney", "2004-07-15T00:00:00Z", "v", "\u30aa\u30fc\u30b9\u30c8\u30e9\u30ea\u30a2 (\u30b7\u30c9\u30cb\u30fc)", "Australia/Sydney" },
        { "ja", "Australia/Sydney", "2004-07-15T00:00:00Z", "vvvv", "\u30aa\u30fc\u30b9\u30c8\u30e9\u30ea\u30a2 (\u30b7\u30c9\u30cb\u30fc)", "Australia/Sydney" },

        { "ja", "Europe/London", "2004-01-15T00:00:00Z", "Z", "+0000", "+0:00" },
        { "ja", "Europe/London", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+00:00", "+0:00" },
        { "ja", "Europe/London", "2004-01-15T00:00:00Z", "z", "GMT+00:00", "+0:00" },
        { "ja", "Europe/London", "2004-01-15T00:00:00Z", "V", "GMT", "+0:00" },
        { "ja", "Europe/London", "2004-01-15T00:00:00Z", "zzzz", "\u30B0\u30EA\u30CB\u30C3\u30B8\u6A19\u6E96\u6642", "+0:00" },
        { "ja", "Europe/London", "2004-07-15T00:00:00Z", "Z", "+0100", "+1:00" },
        { "ja", "Europe/London", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+01:00", "+1:00" },
        { "ja", "Europe/London", "2004-07-15T00:00:00Z", "z", "GMT+01:00", "+1:00" },
        { "ja", "Europe/London", "2004-07-15T00:00:00Z", "V", "GMT+01:00", "+1:00" },
        { "ja", "Europe/London", "2004-07-15T00:00:00Z", "zzzz", "GMT+01:00", "+1:00" },
        { "ja", "Europe/London", "2004-07-15T00:00:00Z", "v", "\u30a4\u30ae\u30ea\u30b9\u6642\u9593", "Europe/London" },
        { "ja", "Europe/London", "2004-07-15T00:00:00Z", "vvvv", "\u30a4\u30ae\u30ea\u30b9\u6642\u9593", "Europe/London" },
        { "ja", "Europe/London", "2004-07-15T00:00:00Z", "VVVV", "\u30a4\u30ae\u30ea\u30b9\u6642\u9593", "Europe/London" },

        { "ja", "Etc/GMT+3", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-07-15T00:00:00Z", "v", "GMT-03:00", "-3:00" },
        { "ja", "Etc/GMT+3", "2004-07-15T00:00:00Z", "vvvv", "GMT-03:00", "-3:00" },

        // JB#5150
        { "ja", "Asia/Calcutta", "2004-01-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "ja", "Asia/Calcutta", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+05:30", "+5:30" },
        { "ja", "Asia/Calcutta", "2004-01-15T00:00:00Z", "z", "GMT+05:30", "+5:30" },
        { "ja", "Asia/Calcutta", "2004-01-15T00:00:00Z", "zzzz", "GMT+05:30", "+5:30" },
        { "ja", "Asia/Calcutta", "2004-07-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "ja", "Asia/Calcutta", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+05:30", "+5:30" },
        { "ja", "Asia/Calcutta", "2004-07-15T00:00:00Z", "z", "GMT+05:30", "+05:30" },
        { "ja", "Asia/Calcutta", "2004-07-15T00:00:00Z", "zzzz", "GMT+05:30", "+5:30" },
        { "ja", "Asia/Calcutta", "2004-07-15T00:00:00Z", "v", "\u30A4\u30F3\u30C9\u6642\u9593", "Asia/Calcutta" },
        { "ja", "Asia/Calcutta", "2004-07-15T00:00:00Z", "vvvv", "\u30A4\u30F3\u30C9\u6642\u9593", "Asia/Calcutta" },

    // ==========

        { "si", "America/Los_Angeles", "2004-01-15T00:00:00Z", "Z", "-0800", "-8:00" },
        { "si", "America/Los_Angeles", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-08:00", "-8:00" },
        { "si", "America/Los_Angeles", "2004-01-15T00:00:00Z", "z", "GMT-08:00", "-8:00" },
        { "si", "America/Los_Angeles", "2004-01-15T00:00:00Z", "zzzz", "GMT-08:00", "-8:00" },
        { "si", "America/Los_Angeles", "2004-07-15T00:00:00Z", "Z", "-0700", "-7:00" },
        { "si", "America/Los_Angeles", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-07:00", "-7:00" },
        { "si", "America/Los_Angeles", "2004-07-15T00:00:00Z", "z", "GMT-07:00", "-7:00" },
        { "si", "America/Los_Angeles", "2004-07-15T00:00:00Z", "zzzz", "GMT-07:00", "-7:00" },
        { "si", "America/Los_Angeles", "2004-07-15T00:00:00Z", "v", "US (Los Angeles)", "America/Los_Angeles" },
        { "si", "America/Los_Angeles", "2004-07-15T00:00:00Z", "vvvv", "US (Los Angeles)", "America/Los_Angeles" },

        { "si", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "si", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "si", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "si", "America/Argentina/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "si", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "si", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "si", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "si", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "si", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "AR (Buenos Aires)", "America/Buenos_Aires" },
        { "si", "America/Argentina/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "AR (Buenos Aires)", "America/Buenos_Aires" },

        { "si", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "si", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "si", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "si", "America/Buenos_Aires", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "si", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "si", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "si", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "si", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "si", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "v", "AR (Buenos Aires)", "America/Buenos_Aires" },
        { "si", "America/Buenos_Aires", "2004-07-15T00:00:00Z", "vvvv", "AR (Buenos Aires)", "America/Buenos_Aires" },

        { "si", "America/Havana", "2004-01-15T00:00:00Z", "Z", "-0500", "-5:00" },
        { "si", "America/Havana", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-05:00", "-5:00" },
        { "si", "America/Havana", "2004-01-15T00:00:00Z", "z", "GMT-05:00", "-5:00" },
        { "si", "America/Havana", "2004-01-15T00:00:00Z", "zzzz", "GMT-05:00", "-5:00" },
        { "si", "America/Havana", "2004-07-15T00:00:00Z", "Z", "-0400", "-4:00" },
        { "si", "America/Havana", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-04:00", "-4:00" },
        { "si", "America/Havana", "2004-07-15T00:00:00Z", "z", "GMT-04:00", "-4:00" },
        { "si", "America/Havana", "2004-07-15T00:00:00Z", "zzzz", "GMT-04:00", "-4:00" },
        { "si", "America/Havana", "2004-07-15T00:00:00Z", "v", "CU", "America/Havana" },
        { "si", "America/Havana", "2004-07-15T00:00:00Z", "vvvv", "CU", "America/Havana" },

        { "si", "Australia/ACT", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "si", "Australia/ACT", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+11:00", "+11:00" },
        { "si", "Australia/ACT", "2004-01-15T00:00:00Z", "z", "GMT+11:00", "+11:00" },
        { "si", "Australia/ACT", "2004-01-15T00:00:00Z", "zzzz", "GMT+11:00", "+11:00" },
        { "si", "Australia/ACT", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "si", "Australia/ACT", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+10:00", "+10:00" },
        { "si", "Australia/ACT", "2004-07-15T00:00:00Z", "z", "GMT+10:00", "+10:00" },
        { "si", "Australia/ACT", "2004-07-15T00:00:00Z", "zzzz", "GMT+10:00", "+10:00" },
        { "si", "Australia/ACT", "2004-07-15T00:00:00Z", "v", "AU (Sydney)", "Australia/Sydney" },
        { "si", "Australia/ACT", "2004-07-15T00:00:00Z", "vvvv", "AU (Sydney)", "Australia/Sydney" },

        { "si", "Australia/Sydney", "2004-01-15T00:00:00Z", "Z", "+1100", "+11:00" },
        { "si", "Australia/Sydney", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+11:00", "+11:00" },
        { "si", "Australia/Sydney", "2004-01-15T00:00:00Z", "z", "GMT+11:00", "+11:00" },
        { "si", "Australia/Sydney", "2004-01-15T00:00:00Z", "zzzz", "GMT+11:00", "+11:00" },
        { "si", "Australia/Sydney", "2004-07-15T00:00:00Z", "Z", "+1000", "+10:00" },
        { "si", "Australia/Sydney", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+10:00", "+10:00" },
        { "si", "Australia/Sydney", "2004-07-15T00:00:00Z", "z", "GMT+10:00", "+10:00" },
        { "si", "Australia/Sydney", "2004-07-15T00:00:00Z", "zzzz", "GMT+10:00", "+10:00" },
        { "si", "Australia/Sydney", "2004-07-15T00:00:00Z", "v", "AU (Sydney)", "Australia/Sydney" },
        { "si", "Australia/Sydney", "2004-07-15T00:00:00Z", "vvvv", "AU (Sydney)", "Australia/Sydney" },

        { "si", "Europe/London", "2004-01-15T00:00:00Z", "Z", "+0000", "+0:00" },
        { "si", "Europe/London", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+00:00", "+0:00" },
        { "si", "Europe/London", "2004-01-15T00:00:00Z", "z", "GMT+00:00", "+0:00" },
        { "si", "Europe/London", "2004-01-15T00:00:00Z", "zzzz", "GMT+00:00", "+0:00" },
        { "si", "Europe/London", "2004-07-15T00:00:00Z", "Z", "+0100", "+1:00" },
        { "si", "Europe/London", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+01:00", "+1:00" },
        { "si", "Europe/London", "2004-07-15T00:00:00Z", "z", "GMT+01:00", "+1:00" },
        { "si", "Europe/London", "2004-07-15T00:00:00Z", "zzzz", "GMT+01:00", "+1:00" },
        { "si", "Europe/London", "2004-07-15T00:00:00Z", "v", "GB", "Europe/London" },
        { "si", "Europe/London", "2004-07-15T00:00:00Z", "vvvv", "GB", "Europe/London" },

        { "si", "Etc/GMT+3", "2004-01-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "si", "Etc/GMT+3", "2004-01-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "si", "Etc/GMT+3", "2004-01-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "si", "Etc/GMT+3", "2004-01-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "si", "Etc/GMT+3", "2004-07-15T00:00:00Z", "Z", "-0300", "-3:00" },
        { "si", "Etc/GMT+3", "2004-07-15T00:00:00Z", "ZZZZ", "GMT-03:00", "-3:00" },
        { "si", "Etc/GMT+3", "2004-07-15T00:00:00Z", "z", "GMT-03:00", "-3:00" },
        { "si", "Etc/GMT+3", "2004-07-15T00:00:00Z", "zzzz", "GMT-03:00", "-3:00" },
        { "si", "Etc/GMT+3", "2004-07-15T00:00:00Z", "v", "GMT-03:00", "-3:00" },
        { "si", "Etc/GMT+3", "2004-07-15T00:00:00Z", "vvvv", "GMT-03:00", "-3:00" },

        // JB#5150
        { "si", "Asia/Calcutta", "2004-01-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "si", "Asia/Calcutta", "2004-01-15T00:00:00Z", "ZZZZ", "GMT+05:30", "+5:30" },
        { "si", "Asia/Calcutta", "2004-01-15T00:00:00Z", "z", "GMT+05:30", "+5:30" },
        { "si", "Asia/Calcutta", "2004-01-15T00:00:00Z", "zzzz", "GMT+05:30", "+5:30" },
        { "si", "Asia/Calcutta", "2004-07-15T00:00:00Z", "Z", "+0530", "+5:30" },
        { "si", "Asia/Calcutta", "2004-07-15T00:00:00Z", "ZZZZ", "GMT+05:30", "+5:30" },
        { "si", "Asia/Calcutta", "2004-07-15T00:00:00Z", "z", "GMT+05:30", "+05:30" },
        { "si", "Asia/Calcutta", "2004-07-15T00:00:00Z", "zzzz", "GMT+05:30", "+5:30" },
        { "si", "Asia/Calcutta", "2004-07-15T00:00:00Z", "v", "IN", "Asia/Calcutta" },
        { "si", "Asia/Calcutta", "2004-07-15T00:00:00Z", "vvvv", "IN", "Asia/Calcutta" },
        
    };

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
            int month, yr, day;
            cal.setTime(d);
            yr = cal.get(Calendar.YEAR);
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_MONTH);
            logln("  . parse . " + s + " (month = " + month + ")");
            if (month != Calendar.JUNE)
                errln("FAIL: Month should be June");
            if (yr != 1997)
                errln("FAIL: Year should be 1997");
            if (day != 15)
                errln("FAIL: day should be 15");
            logln("format(July 15 1997) = " + julyStr);
            d = fmt.parse(julyStr);
            s = fmt.format(d);
            cal.setTime(d);
            yr = cal.get(Calendar.YEAR) - 1900;
            month = cal.get(Calendar.MONTH);
            day = cal.get(Calendar.DAY_OF_WEEK);
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
                errln("FAIL: Expected " + date + " Got: " + tempDate);
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
                "simple format:  ", "04/04/97 23:00 GMT+00:00", 
                "MM/dd/yy HH:mm zzz", "full format:    ", 
                "Friday, April 4, 1997 11:00:00 o'clock PM GMT+00:00", 
                "EEEE, MMMM d, yyyy h:mm:ss 'o''clock' a zzz", 
                "long format:    ", "April 4, 1997 11:00:00 PM GMT+00:00", 
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
        String expectedFRENCH_JDK12 = "lundi 15 septembre 1997 00:00:00 heure avanc\u00E9e du Pacifique";
        //String expectedFRENCH = "lundi 15 septembre 1997 00 h 00 PDT";
        String expectedUS = "Monday, September 15, 1997 12:00:00 AM Pacific Daylight Time";
        logln("Date set to : " + testDate);
        String out = dfFrench.format(testDate);
        logln("Date Formated with French Locale " + out);
        //fix the jdk resources differences between jdk 1.2 and jdk 1.3
        /* our own data only has GMT-xxxx information here
        String javaVersion = System.getProperty("java.version");
        if (javaVersion.startsWith("1.2")) {
            if (!out.equals(expectedFRENCH_JDK12))
                errln("FAIL: Expected " + expectedFRENCH_JDK12+" Got "+out);
        } else {
            if (!out.equals(expectedFRENCH))
                errln("FAIL: Expected " + expectedFRENCH);
        }
        */
        if (!out.equals(expectedFRENCH_JDK12))
            errln("FAIL: Expected " + expectedFRENCH_JDK12+" Got "+out);
        out = dfUS.format(testDate);
        logln("Date Formated with US Locale " + out);
        if (!out.equals(expectedUS))
            errln("FAIL: Expected " + expectedUS+" Got "+out);
    }

    /**
     * Test the formatting of dates with the 'NONE' keyword.
     */
    public void TestDateFormatNone() {
    
        Date testDate = new Date(874306800000l); //Mon Sep 15 00:00:00 PDT 1997
        DateFormat dfFrench = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.NONE, Locale.FRENCH);
        //Set TimeZone = PDT
        TimeZone tz = TimeZone.getTimeZone("PST");
        dfFrench.setTimeZone(tz);
        String expectedFRENCH_JDK12 = "lundi 15 septembre 1997";
        //String expectedFRENCH = "lundi 15 septembre 1997 00 h 00 PDT";
        logln("Date set to : " + testDate);
        String out = dfFrench.format(testDate);
        logln("Date Formated with French Locale " + out);
        if (!out.equals(expectedFRENCH_JDK12))
            errln("FAIL: Expected " + expectedFRENCH_JDK12+" Got "+out);
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

            // pattern, input, expected output (in quotes)
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
            DateFormat.getDateTimeInstance(-1, -1, Locale.US);
            errln("Expected exception for getDateTimeInstance(-1, -1, Locale)");
        }
        catch(IllegalArgumentException e) {
            logln("one ok");
        }
        catch(Exception e) {
            warnln("Expected IllegalArgumentException, got: " + e);
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
            warnln("Expected IllegalArgumentException, got: " + e);
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

    public void TestChineseDateFormatLocalizedPatternChars() {
        // jb 4904
        // make sure we can display localized versions of the chars used in the default
        // chinese date format patterns
        Calendar chineseCalendar = new ChineseCalendar();
        chineseCalendar.setTimeInMillis((new Date()).getTime());
        SimpleDateFormat longChineseDateFormat = 
            (SimpleDateFormat)chineseCalendar.getDateTimeFormat(DateFormat.LONG, DateFormat.LONG, Locale.CHINA );
        DateFormatSymbols dfs = new ChineseDateFormatSymbols( chineseCalendar, Locale.CHINA );
        longChineseDateFormat.setDateFormatSymbols( dfs );
        // This next line throws the exception
        try {
            longChineseDateFormat.toLocalizedPattern();
        }
        catch (Exception e) {
            errln("could not localized pattern: " + e.getMessage());
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
        
        DateFormat sdfmt = new SimpleDateFormat();
        
        if (f.hashCode() != f.hashCode()) {
            errln("hashCode is not stable");
        }
        if (!f.equals(f)) {
            errln("f != f");
        }
        if (f.equals(null)) {
            errln("f should not equal null");
        }
        if (f.equals(sdfmt)) {
            errln("A time instance shouldn't equal a default date format");
        }
        
        Date d;
        {
            ChineseDateFormat fmt = new ChineseDateFormat("yymm", Locale.US);
            try {
                fmt.parse("2"); // fewer symbols than required 2
                errln("whoops");
            }
            catch (ParseException e) {
                logln("ok");
            }

            try {
                fmt.parse("2255"); // should succeed with obeycount
                logln("ok");
            }
            catch (ParseException e) {
                errln("whoops");
            }

            try {
                fmt.parse("ni hao"); // not a number, should fail
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
                fmt.parse(xbuf.toString());
                logln("ok");
                
                xbuf.setLength(0);
                xcal.set(Calendar.HOUR_OF_DAY, 25);
                fmt.format(xcal, xbuf, fpos);
                Date d2 = fmt.parse(xbuf.toString());
                logln("ok again - d2=" + d2);
            }
            catch (ParseException e) {
                errln("whoops");
            }
        }
        
        {
            // cover gmt+hh:mm
            DateFormat fmt = new SimpleDateFormat("MM/dd/yy z");
            try {
                d = fmt.parse("07/10/53 GMT+10:00");
                logln("ok : d = " + d);
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 GMT+10:00 for pattern MM/dd/yy z");
            }
            
            // cover invalid separator after GMT
            {
                ParsePosition pp = new ParsePosition(0);
                String text = "07/10/53 GMT=10:00";
                d = fmt.parse(text, pp);
                if(pp.getIndex()!=12){
                    errln("Parse of 07/10/53 GMT=10:00 for pattern MM/dd/yy z");
                }
                logln("Parsing of the text stopped at pos: " + pp.getIndex() + " as expected and length is "+text.length());
            }
            
            // cover bad text after GMT+.
            try {
                fmt.parse("07/10/53 GMT+blecch");
                logln("ok GMT+blecch");
            }
            catch (ParseException e) {
                errln("whoops GMT+blecch");
            }
            
            // cover bad text after GMT+hh:.
            try {
                fmt.parse("07/10/53 GMT+07:blecch");
                logln("ok GMT+xx:blecch");
            }
            catch (ParseException e) {
                errln("whoops GMT+xx:blecch");
            }
            
            // cover no ':' GMT+#, # < 24 (hh)
            try {
                d = fmt.parse("07/10/53 GMT+07");
                logln("ok GMT+07");
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 GMT+07 for pattern MM/dd/yy z");
            }
            
            // cover no ':' GMT+#, # > 24 (hhmm)
            try {
                d = fmt.parse("07/10/53 GMT+0730");
                logln("ok");
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 GMT+0730 for pattern MM/dd/yy z");
            }
            
            // cover GMT+#, # with second field
            try {
                d = fmt.parse("07/10/53 GMT+07:30:15");
                logln("ok GMT+07:30:15");
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 GMT+07:30:15 for pattern MM/dd/yy z");
            }

            // cover no ':' GMT+#, # with second field, no leading zero
            try {
                d = fmt.parse("07/10/53 GMT+73015");
                logln("ok GMT+73015");
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 GMT+73015 for pattern MM/dd/yy z");
            }

            // cover no ':' GMT+#, # with 1 digit second field
            try {
                d = fmt.parse("07/10/53 GMT+07300");
                logln("ok GMT+07300");
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 GMT+07300 for pattern MM/dd/yy z");
            }
            
            // cover raw digits with no leading sign (bad RFC822) 
            try {
                d = fmt.parse("07/10/53 07");
                errln("Parse of 07/10/53 07 for pattern MM/dd/yy z passed!");
            }
            catch (ParseException e) {
                logln("ok");
            }
            
            // cover raw digits (RFC822) 
            try {
                d = fmt.parse("07/10/53 +07");
                logln("ok");
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 +07 for pattern MM/dd/yy z failed");
            }
            
            // cover raw digits (RFC822) 
            try {
                d = fmt.parse("07/10/53 -0730");
                logln("ok");
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 -00730 for pattern MM/dd/yy z failed");
            }
            
            // cover raw digits (RFC822) in DST
            try {
                fmt.setTimeZone(TimeZone.getTimeZone("PDT"));
                d = fmt.parse("07/10/53 -0730");
                logln("ok");
            }
            catch (ParseException e) {
                errln("Parse of 07/10/53 -0730 for pattern MM/dd/yy z failed");
            }
        }
        
        // TODO: revisit toLocalizedPattern
        if (false) {
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
                fmt.toLocalizedPattern();
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
                errln("Parse of 07/10/53 DBDY for pattern MM/dd/yy z passed");
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
                errln("Parse of 07/10/53 DBDY for pattern MM/dd/yy z passed");
            }
            catch (ParseException e) {
                logln("time zone ex3 ok");
            }
            catch (Exception e) {
                // hmmm... this shouldn't happen.  don't want to exit this
                // fn with timezone improperly set, so just in case
                TimeZone.setDefault(oldtz);
                throw new IllegalStateException(e.getMessage());
            }

            // create DFS that recognizes our bogus time zone, sortof
            DateFormatSymbols xsym = new DateFormatSymbols();
            String[][] tzids = xsym.getZoneStrings();
            if (tzids.length > 0) { // let's hope!
                tzids[0][1] = "DBDY"; // change a local name
                logln("replaced '" + tzids[0][0] + "' with DBDY");

                xsym.setZoneStrings(tzids);
                fmt.setDateFormatSymbols(xsym);

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
            // These APIs are obsolete and return null
            ResourceBundle resb = DateFormatSymbols.getDateFormatBundle(cal, loc);
            ResourceBundle resb2 = DateFormatSymbols.getDateFormatBundle(cal, uloc);
            ResourceBundle resb3 = DateFormatSymbols.getDateFormatBundle(cal.getClass(), loc);
            ResourceBundle resb4 = DateFormatSymbols.getDateFormatBundle(cal.getClass(), uloc);

            if (resb != null) {
                logln("resb is not null");
            }
            if (resb2 != null) {
                logln("resb2 is not null");
            }
            if (resb3 != null) {
                logln("resb3 is not null");
            }
            if (resb4 != null) {
                logln("resb4 is not null");
            }
        }

        {
            //cover DateFormatSymbols.getInstance
            DateFormatSymbols datsym1 = DateFormatSymbols.getInstance();
            DateFormatSymbols datsym2 = new DateFormatSymbols();
            if (!datsym1.equals(datsym2)) {
                errln("FAIL: DateFormatSymbols returned by getInstance()" +
                        "does not match new DateFormatSymbols().");
            }
            datsym1 = DateFormatSymbols.getInstance(Locale.JAPAN);
            datsym2 = DateFormatSymbols.getInstance(ULocale.JAPAN);
            if (!datsym1.equals(datsym2)) {
                errln("FAIL: DateFormatSymbols returned by getInstance(Locale.JAPAN)" +
                        "does not match the one returned by getInstance(ULocale.JAPAN).");
            }
        }
        {
            //cover DateFormatSymbols.getAvailableLocales/getAvailableULocales
            Locale[] allLocales = DateFormatSymbols.getAvailableLocales();
            if (allLocales.length == 0) {
                errln("FAIL: Got a empty list for DateFormatSymbols.getAvailableLocales");
            } else {
                logln("PASS: " + allLocales.length +
                        " available locales returned by DateFormatSymbols.getAvailableLocales");            
            }

            ULocale[] allULocales = DateFormatSymbols.getAvailableULocales();
            if (allULocales.length == 0) {
                errln("FAIL: Got a empty list for DateFormatSymbols.getAvailableLocales");
            } else {
                logln("PASS: " + allULocales.length +
                        " available locales returned by DateFormatSymbols.getAvailableULocales");            
            }
        }
    }

    public void TestStandAloneMonths()
    {
        String EN_DATA[] = {
            "yyyy MM dd HH:mm:ss",

            "yyyy LLLL dd H:mm:ss", "fp", "2004 03 10 16:36:31", "2004 March 10 16:36:31", "2004 03 10 16:36:31",
            "yyyy LLL dd H:mm:ss",  "fp", "2004 03 10 16:36:31", "2004 Mar 10 16:36:31",   "2004 03 10 16:36:31",
            "yyyy LLLL dd H:mm:ss", "F",  "2004 03 10 16:36:31", "2004 March 10 16:36:31",
            "yyyy LLL dd H:mm:ss",  "pf", "2004 Mar 10 16:36:31", "2004 03 10 16:36:31", "2004 Mar 10 16:36:31",
            
            "LLLL", "fp", "1970 01 01 0:00:00", "January",   "1970 01 01 0:00:00",
            "LLLL", "fp", "1970 02 01 0:00:00", "February",  "1970 02 01 0:00:00",
            "LLLL", "fp", "1970 03 01 0:00:00", "March",     "1970 03 01 0:00:00",
            "LLLL", "fp", "1970 04 01 0:00:00", "April",     "1970 04 01 0:00:00",
            "LLLL", "fp", "1970 05 01 0:00:00", "May",       "1970 05 01 0:00:00",
            "LLLL", "fp", "1970 06 01 0:00:00", "June",      "1970 06 01 0:00:00",
            "LLLL", "fp", "1970 07 01 0:00:00", "July",      "1970 07 01 0:00:00",
            "LLLL", "fp", "1970 08 01 0:00:00", "August",    "1970 08 01 0:00:00",
            "LLLL", "fp", "1970 09 01 0:00:00", "September", "1970 09 01 0:00:00",
            "LLLL", "fp", "1970 10 01 0:00:00", "October",   "1970 10 01 0:00:00",
            "LLLL", "fp", "1970 11 01 0:00:00", "November",  "1970 11 01 0:00:00",
            "LLLL", "fp", "1970 12 01 0:00:00", "December",  "1970 12 01 0:00:00",
            
            "LLL", "fp", "1970 01 01 0:00:00", "Jan", "1970 01 01 0:00:00",
            "LLL", "fp", "1970 02 01 0:00:00", "Feb", "1970 02 01 0:00:00",
            "LLL", "fp", "1970 03 01 0:00:00", "Mar", "1970 03 01 0:00:00",
            "LLL", "fp", "1970 04 01 0:00:00", "Apr", "1970 04 01 0:00:00",
            "LLL", "fp", "1970 05 01 0:00:00", "May", "1970 05 01 0:00:00",
            "LLL", "fp", "1970 06 01 0:00:00", "Jun", "1970 06 01 0:00:00",
            "LLL", "fp", "1970 07 01 0:00:00", "Jul", "1970 07 01 0:00:00",
            "LLL", "fp", "1970 08 01 0:00:00", "Aug", "1970 08 01 0:00:00",
            "LLL", "fp", "1970 09 01 0:00:00", "Sep", "1970 09 01 0:00:00",
            "LLL", "fp", "1970 10 01 0:00:00", "Oct", "1970 10 01 0:00:00",
            "LLL", "fp", "1970 11 01 0:00:00", "Nov", "1970 11 01 0:00:00",
            "LLL", "fp", "1970 12 01 0:00:00", "Dec", "1970 12 01 0:00:00",
        };
        
        String CS_DATA[] = {
            "yyyy MM dd HH:mm:ss",

            "yyyy LLLL dd H:mm:ss", "fp", "2004 04 10 16:36:31", "2004 duben 10 16:36:31", "2004 04 10 16:36:31",
            "yyyy MMMM dd H:mm:ss", "fp", "2004 04 10 16:36:31", "2004 dubna 10 16:36:31", "2004 04 10 16:36:31",
            "yyyy LLL dd H:mm:ss",  "fp", "2004 04 10 16:36:31", "2004 4. 10 16:36:31",   "2004 04 10 16:36:31",
            "yyyy LLLL dd H:mm:ss", "F",  "2004 04 10 16:36:31", "2004 duben 10 16:36:31",
            "yyyy MMMM dd H:mm:ss", "F",  "2004 04 10 16:36:31", "2004 dubna 10 16:36:31",
            "yyyy LLLL dd H:mm:ss", "pf", "2004 duben 10 16:36:31", "2004 04 10 16:36:31", "2004 duben 10 16:36:31",
            "yyyy MMMM dd H:mm:ss", "pf", "2004 dubna 10 16:36:31", "2004 04 10 16:36:31", "2004 dubna 10 16:36:31",
            
            "LLLL", "fp", "1970 01 01 0:00:00", "leden",               "1970 01 01 0:00:00",
            "LLLL", "fp", "1970 02 01 0:00:00", "\u00FAnor",           "1970 02 01 0:00:00",
            "LLLL", "fp", "1970 03 01 0:00:00", "b\u0159ezen",         "1970 03 01 0:00:00",
            "LLLL", "fp", "1970 04 01 0:00:00", "duben",               "1970 04 01 0:00:00",
            "LLLL", "fp", "1970 05 01 0:00:00", "kv\u011Bten",         "1970 05 01 0:00:00",
            "LLLL", "fp", "1970 06 01 0:00:00", "\u010Derven",         "1970 06 01 0:00:00",
            "LLLL", "fp", "1970 07 01 0:00:00", "\u010Dervenec",       "1970 07 01 0:00:00",
            "LLLL", "fp", "1970 08 01 0:00:00", "srpen",               "1970 08 01 0:00:00",
            "LLLL", "fp", "1970 09 01 0:00:00", "z\u00E1\u0159\u00ED", "1970 09 01 0:00:00",
            "LLLL", "fp", "1970 10 01 0:00:00", "\u0159\u00EDjen",     "1970 10 01 0:00:00",
            "LLLL", "fp", "1970 11 01 0:00:00", "listopad",            "1970 11 01 0:00:00",
            "LLLL", "fp", "1970 12 01 0:00:00", "prosinec",            "1970 12 01 0:00:00",

            "LLL", "fp", "1970 01 01 0:00:00", "1.",  "1970 01 01 0:00:00",
            "LLL", "fp", "1970 02 01 0:00:00", "2.",  "1970 02 01 0:00:00",
            "LLL", "fp", "1970 03 01 0:00:00", "3.",  "1970 03 01 0:00:00",
            "LLL", "fp", "1970 04 01 0:00:00", "4.",  "1970 04 01 0:00:00",
            "LLL", "fp", "1970 05 01 0:00:00", "5.",  "1970 05 01 0:00:00",
            "LLL", "fp", "1970 06 01 0:00:00", "6.",  "1970 06 01 0:00:00",
            "LLL", "fp", "1970 07 01 0:00:00", "7.",  "1970 07 01 0:00:00",
            "LLL", "fp", "1970 08 01 0:00:00", "8.",  "1970 08 01 0:00:00",
            "LLL", "fp", "1970 09 01 0:00:00", "9.",  "1970 09 01 0:00:00",
            "LLL", "fp", "1970 10 01 0:00:00", "10.", "1970 10 01 0:00:00",
            "LLL", "fp", "1970 11 01 0:00:00", "11.", "1970 11 01 0:00:00",
            "LLL", "fp", "1970 12 01 0:00:00", "12.", "1970 12 01 0:00:00",
        };
        
        expect(EN_DATA, new Locale("en", "", ""));
        expect(CS_DATA, new Locale("cs", "", ""));
    }
    
    public void TestStandAloneDays()
    {
        String EN_DATA[] = {
            "yyyy MM dd HH:mm:ss",

            "cccc", "fp", "1970 01 04 0:00:00", "Sunday",    "1970 01 04 0:00:00",
            "cccc", "fp", "1970 01 05 0:00:00", "Monday",    "1970 01 05 0:00:00",
            "cccc", "fp", "1970 01 06 0:00:00", "Tuesday",   "1970 01 06 0:00:00",
            "cccc", "fp", "1970 01 07 0:00:00", "Wednesday", "1970 01 07 0:00:00",
            "cccc", "fp", "1970 01 01 0:00:00", "Thursday",  "1970 01 01 0:00:00",
            "cccc", "fp", "1970 01 02 0:00:00", "Friday",    "1970 01 02 0:00:00",
            "cccc", "fp", "1970 01 03 0:00:00", "Saturday",  "1970 01 03 0:00:00",
            
            "ccc", "fp", "1970 01 04 0:00:00", "Sun", "1970 01 04 0:00:00",
            "ccc", "fp", "1970 01 05 0:00:00", "Mon", "1970 01 05 0:00:00",
            "ccc", "fp", "1970 01 06 0:00:00", "Tue", "1970 01 06 0:00:00",
            "ccc", "fp", "1970 01 07 0:00:00", "Wed", "1970 01 07 0:00:00",
            "ccc", "fp", "1970 01 01 0:00:00", "Thu", "1970 01 01 0:00:00",
            "ccc", "fp", "1970 01 02 0:00:00", "Fri", "1970 01 02 0:00:00",
            "ccc", "fp", "1970 01 03 0:00:00", "Sat", "1970 01 03 0:00:00",
        };
            
        String CS_DATA[] = {
            "yyyy MM dd HH:mm:ss",

            "cccc", "fp", "1970 01 04 0:00:00", "ned\u011Ble",       "1970 01 04 0:00:00",
            "cccc", "fp", "1970 01 05 0:00:00", "pond\u011Bl\u00ED", "1970 01 05 0:00:00",
            "cccc", "fp", "1970 01 06 0:00:00", "\u00FAter\u00FD",   "1970 01 06 0:00:00",
            "cccc", "fp", "1970 01 07 0:00:00", "st\u0159eda",       "1970 01 07 0:00:00",
            "cccc", "fp", "1970 01 01 0:00:00", "\u010Dtvrtek",      "1970 01 01 0:00:00",
            "cccc", "fp", "1970 01 02 0:00:00", "p\u00E1tek",        "1970 01 02 0:00:00",
            "cccc", "fp", "1970 01 03 0:00:00", "sobota",            "1970 01 03 0:00:00",
            
            "ccc", "fp", "1970 01 04 0:00:00", "ne",      "1970 01 04 0:00:00",
            "ccc", "fp", "1970 01 05 0:00:00", "po",      "1970 01 05 0:00:00",
            "ccc", "fp", "1970 01 06 0:00:00", "\u00FAt", "1970 01 06 0:00:00",
            "ccc", "fp", "1970 01 07 0:00:00", "st",      "1970 01 07 0:00:00",
            "ccc", "fp", "1970 01 01 0:00:00", "\u010Dt", "1970 01 01 0:00:00",
            "ccc", "fp", "1970 01 02 0:00:00", "p\u00E1", "1970 01 02 0:00:00",
            "ccc", "fp", "1970 01 03 0:00:00", "so",      "1970 01 03 0:00:00",
        };
        
        expect(EN_DATA, new Locale("en", "", ""));
        expect(CS_DATA, new Locale("cs", "", ""));
    }
    
    public void TestNarrowNames()
    {
        String EN_DATA[] = {
                "yyyy MM dd HH:mm:ss",

                "yyyy MMMMM dd H:mm:ss", "2004 03 10 16:36:31", "2004 M 10 16:36:31",
                "yyyy LLLLL dd H:mm:ss",  "2004 03 10 16:36:31", "2004 M 10 16:36:31",
                
                "MMMMM", "1970 01 01 0:00:00", "J",
                "MMMMM", "1970 02 01 0:00:00", "F",
                "MMMMM", "1970 03 01 0:00:00", "M",
                "MMMMM", "1970 04 01 0:00:00", "A",
                "MMMMM", "1970 05 01 0:00:00", "M",
                "MMMMM", "1970 06 01 0:00:00", "J",
                "MMMMM", "1970 07 01 0:00:00", "J",
                "MMMMM", "1970 08 01 0:00:00", "A",
                "MMMMM", "1970 09 01 0:00:00", "S",
                "MMMMM", "1970 10 01 0:00:00", "O",
                "MMMMM", "1970 11 01 0:00:00", "N",
                "MMMMM", "1970 12 01 0:00:00", "D",
                
                "LLLLL", "1970 01 01 0:00:00", "J",
                "LLLLL", "1970 02 01 0:00:00", "F",
                "LLLLL", "1970 03 01 0:00:00", "M",
                "LLLLL", "1970 04 01 0:00:00", "A",
                "LLLLL", "1970 05 01 0:00:00", "M",
                "LLLLL", "1970 06 01 0:00:00", "J",
                "LLLLL", "1970 07 01 0:00:00", "J",
                "LLLLL", "1970 08 01 0:00:00", "A",
                "LLLLL", "1970 09 01 0:00:00", "S",
                "LLLLL", "1970 10 01 0:00:00", "O",
                "LLLLL", "1970 11 01 0:00:00", "N",
                "LLLLL", "1970 12 01 0:00:00", "D",

                "EEEEE", "1970 01 04 0:00:00", "S",
                "EEEEE", "1970 01 05 0:00:00", "M",
                "EEEEE", "1970 01 06 0:00:00", "T",
                "EEEEE", "1970 01 07 0:00:00", "W",
                "EEEEE", "1970 01 01 0:00:00", "T",
                "EEEEE", "1970 01 02 0:00:00", "F",
                "EEEEE", "1970 01 03 0:00:00", "S",
                
                "ccccc", "1970 01 04 0:00:00", "S",
                "ccccc", "1970 01 05 0:00:00", "M",
                "ccccc", "1970 01 06 0:00:00", "T",
                "ccccc", "1970 01 07 0:00:00", "W",
                "ccccc", "1970 01 01 0:00:00", "T",
                "ccccc", "1970 01 02 0:00:00", "F",
                "ccccc", "1970 01 03 0:00:00", "S",
            };
            
            String CS_DATA[] = {
                "yyyy MM dd HH:mm:ss",

                "yyyy LLLLL dd H:mm:ss", "2004 04 10 16:36:31", "2004 d 10 16:36:31",
                "yyyy MMMMM dd H:mm:ss", "2004 04 10 16:36:31", "2004 d 10 16:36:31",
                
                "MMMMM", "1970 01 01 0:00:00", "l",
                "MMMMM", "1970 02 01 0:00:00", "\u00FA",
                "MMMMM", "1970 03 01 0:00:00", "b",
                "MMMMM", "1970 04 01 0:00:00", "d",
                "MMMMM", "1970 05 01 0:00:00", "k",
                "MMMMM", "1970 06 01 0:00:00", "\u010D",
                "MMMMM", "1970 07 01 0:00:00", "\u010D",
                "MMMMM", "1970 08 01 0:00:00", "s",
                "MMMMM", "1970 09 01 0:00:00", "z",
                "MMMMM", "1970 10 01 0:00:00", "\u0159",
                "MMMMM", "1970 11 01 0:00:00", "l",
                "MMMMM", "1970 12 01 0:00:00", "p",
                
                "LLLLL", "1970 01 01 0:00:00", "l",
                "LLLLL", "1970 02 01 0:00:00", "\u00FA",
                "LLLLL", "1970 03 01 0:00:00", "b",
                "LLLLL", "1970 04 01 0:00:00", "d",
                "LLLLL", "1970 05 01 0:00:00", "k",
                "LLLLL", "1970 06 01 0:00:00", "\u010D",
                "LLLLL", "1970 07 01 0:00:00", "\u010D",
                "LLLLL", "1970 08 01 0:00:00", "s",
                "LLLLL", "1970 09 01 0:00:00", "z",
                "LLLLL", "1970 10 01 0:00:00", "\u0159",
                "LLLLL", "1970 11 01 0:00:00", "l",
                "LLLLL", "1970 12 01 0:00:00", "p",

                "EEEEE", "1970 01 04 0:00:00", "N",
                "EEEEE", "1970 01 05 0:00:00", "P",
                "EEEEE", "1970 01 06 0:00:00", "\u00DA",
                "EEEEE", "1970 01 07 0:00:00", "S",
                "EEEEE", "1970 01 01 0:00:00", "\u010C",
                "EEEEE", "1970 01 02 0:00:00", "P",
                "EEEEE", "1970 01 03 0:00:00", "S",

                "ccccc", "1970 01 04 0:00:00", "N",
                "ccccc", "1970 01 05 0:00:00", "P",
                "ccccc", "1970 01 06 0:00:00", "\u00DA",
                "ccccc", "1970 01 07 0:00:00", "S",
                "ccccc", "1970 01 01 0:00:00", "\u010C",
                "ccccc", "1970 01 02 0:00:00", "P",
                "ccccc", "1970 01 03 0:00:00", "S",
            };
            
            expectFormat(EN_DATA, new Locale("en", "", ""));
            expectFormat(CS_DATA, new Locale("cs", "", ""));
    }
    
    public void TestEras()
    {
        String EN_DATA[] = {
            "yyyy MM dd",

            "MMMM dd yyyy G",    "fp", "1951 07 17", "July 17 1951 AD",          "1951 07 17",
            "MMMM dd yyyy GG",   "fp", "1951 07 17", "July 17 1951 AD",          "1951 07 17",
            "MMMM dd yyyy GGG",  "fp", "1951 07 17", "July 17 1951 AD",          "1951 07 17",
            "MMMM dd yyyy GGGG", "fp", "1951 07 17", "July 17 1951 Anno Domini", "1951 07 17",

            "MMMM dd yyyy G",    "fp", "-438 07 17", "July 17 0439 BC",            "-438 07 17",
            "MMMM dd yyyy GG",   "fp", "-438 07 17", "July 17 0439 BC",            "-438 07 17",
            "MMMM dd yyyy GGG",  "fp", "-438 07 17", "July 17 0439 BC",            "-438 07 17",
            "MMMM dd yyyy GGGG", "fp", "-438 07 17", "July 17 0439 Before Christ", "-438 07 17",
       };
        
        expect(EN_DATA, new Locale("en", "", ""));
    }
/*    
    public void TestQuarters()
    {
        String EN_DATA[] = {
            "yyyy MM dd",

            "Q",    "fp", "1970 01 01", "1",           "1970 01 01",
            "QQ",   "fp", "1970 04 01", "02",          "1970 04 01",
            "QQQ",  "fp", "1970 07 01", "Q3",          "1970 07 01",
            "QQQQ", "fp", "1970 10 01", "4th quarter", "1970 10 01",

            "q",    "fp", "1970 01 01", "1",           "1970 01 01",
            "qq",   "fp", "1970 04 01", "02",          "1970 04 01",
            "qqq",  "fp", "1970 07 01", "Q3",          "1970 07 01",
            "qqqq", "fp", "1970 10 01", "4th quarter", "1970 10 01",
       };
        
        expect(EN_DATA, new Locale("en", "", ""));
    }
*/

    /**
     * Test DateFormat's parsing of default GMT variants.  See ticket#6135
     */
    public void TestGMTParsing() {
        String DATA[] = {
            "HH:mm:ss Z",

            // pattern, input, expected output (in quotes)
            "HH:mm:ss Z",       "10:20:30 GMT+03:00",   "10:20:30 +0300",
            "HH:mm:ss Z",       "10:20:30 UT-02:00",    "10:20:30 -0200",
            "HH:mm:ss Z",       "10:20:30 GMT",         "10:20:30 +0000",
            "HH:mm:ss vvvv",    "10:20:30 UT+10:00",    "10:20:30 +1000",
            "HH:mm:ss zzzz",    "10:20:30 UTC",         "10:20:30 +0000",   // standalone "UTC"
            "ZZZZ HH:mm:ss",    "UT 10:20:30",          "10:20:30 +0000",
            "V HH:mm:ss",       "UT+0130 10:20:30",     "10:20:30 +0130",
            "V HH:mm:ss",       "UTC+0130 10:20:30",    null,               // UTC+0130 is not a supported pattern
            "HH mm Z ss",       "10 20 GMT-1100 30",    "10:20:30 -1100",
        };
        expectParse(DATA, new Locale("en", "", ""));
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
     * Test formatting.  Input is an array of String that starts
     * with a single 'header' element
     *
     * [0]   = reference dateformat pattern string (ref)
     *
     * followed by test cases, each of which is 4 or 5 elements:
     *
     * [i]   = test dateformat pattern string (test), or null to reuse prior test pattern
     * [i+1] = data string A
     * [i+2] = data string B
     *
     * Formats a date, checks the result.
     *
     * Examples:
     * "y/M/d H:mm:ss.SSS", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.567"
     * -- ref.parse A, get t0
     * -- test.format t0, get r0
     * -- compare r0 to B, fail if not equal
     */
    void expectFormat(String[] data, Locale loc)
    {
        int i = 1;
        String currentPat = null;
        SimpleDateFormat ref = new SimpleDateFormat(data[0], loc);

        while (i<data.length) {
            SimpleDateFormat fmt = new SimpleDateFormat("", loc);
            String pattern  = data[i++];
            if (pattern != null) {
                fmt.applyPattern(pattern);
                currentPat = pattern;
            }

            String datestr = data[i++];
            String string = data[i++];
            Date date = null;
            
            try {
                date = ref.parse(datestr);
            } catch (ParseException e) {
                errln("FAIL: Internal test error; can't parse " + datestr);
                continue;
            }
            
            assertEquals("\"" + currentPat + "\".format(" + datestr + ")",
                         string,
                         fmt.format(date));
        }
    }

    /**
     * Test formatting and parsing.  Input is an array of String that starts
     * with a single 'header' element
     *
     * [0]   = reference dateformat pattern string (ref)
     *
     * followed by test cases, each of which is 4 or 5 elements:
     *
     * [i]   = test dateformat pattern string (test), or null to reuse prior test pattern
     * [i+1] = control string, either "fp", "pf", or "F".
     * [i+2] = data string A
     * [i+3] = data string B
     * [i+4] = data string C (not present for 'F' control string)
     *
     * Note: the number of data strings depends on the control string.
     *
     * fp formats a date, checks the result, then parses the result and checks against a (possibly different) date
     * pf parses a string, checks the result, then formats the result and checks against a (possibly different) string
     * F is a shorthand for fp when the second date is the same as the first
     * P is a shorthand for pf when the second string is the same as the first
     *
     * Examples:
     * (fp) "y/M/d H:mm:ss.SS", "fp", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.56", "2004 03 10 16:36:31.560",
     * -- ref.parse A, get t0
     * -- test.format t0, get r0
     * -- compare r0 to B, fail if not equal
     * -- test.parse B, get t1
     * -- ref.parse C, get t2
     * -- compare t1 and t2, fail if not equal
     *
     * (F) "y/M/d H:mm:ss.SSS", "F", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.567"
     * -- ref.parse A, get t0
     * -- test.format t0, get r0
     * -- compare r0 to B, fail if not equal
     * -- test.parse B, get t1
     * -- compare t1 and t0, fail if not equal
     *
     * (pf) "y/M/d H:mm:ss.SSSS", "pf", "2004/3/10 16:36:31.5679", "2004 03 10 16:36:31.567", "2004/3/10 16:36:31.5670",
     * -- test.parse A, get t0
     * -- ref.parse B, get t1
     * -- compare t0 to t1, fail if not equal
     * -- test.format t1, get r0
     * -- compare r0 and C, fail if not equal
     *
     * (P) "y/M/d H:mm:ss.SSSS", "P", "2004/3/10 16:36:31.5679", "2004 03 10 16:36:31.567"",
     * -- test.parse A, get t0
     * -- ref.parse B, get t1
     * -- compare t0 to t1, fail if not equal
     * -- test.format t1, get r0
     * -- compare r0 and A, fail if not equal
     */
    void expect(String[] data, Locale loc) {
        int i = 1;
        SimpleDateFormat univ = new SimpleDateFormat("EE G yyyy MM dd HH:mm:ss.SSS zzz", loc);
        String currentPat = null;
        SimpleDateFormat ref = new SimpleDateFormat(data[0], loc);

        while (i<data.length) {
            SimpleDateFormat fmt = new SimpleDateFormat("", loc);
            String pattern  = data[i++];
            if (pattern != null) {
                fmt.applyPattern(pattern);
                currentPat = pattern;
            }

            String control = data[i++];

            if (control.equals("fp") || control.equals("F")) {
                // 'f'
                String datestr = data[i++];
                String string = data[i++];
                String datestr2 = datestr;
                if (control.length() == 2) {
                    datestr2 = data[i++];
                }
                Date date = null;
                try {
                    date = ref.parse(datestr);
                } catch (ParseException e) {
                    errln("FAIL: Internal test error; can't parse " + datestr);
                    continue;
                }
                assertEquals("\"" + currentPat + "\".format(" + datestr + ")",
                             string,
                             fmt.format(date));
                // 'p'
                if (!datestr2.equals(datestr)) {
                    try {
                        date = ref.parse(datestr2);
                    } catch (ParseException e2) {
                        errln("FAIL: Internal test error; can't parse " + datestr2);
                        continue;
                    }
                }
                try {
                    Date parsedate = fmt.parse(string);
                    assertEquals("\"" + currentPat + "\".parse(" + string + ")",
                                 univ.format(date),
                                 univ.format(parsedate));
                } catch (ParseException e3) {
                    errln("FAIL: \"" + currentPat + "\".parse(" + string + ") => " +
                          e3);
                    continue;
                }
            }
            else if (control.equals("pf") || control.equals("P")) {
                // 'p'
                String string = data[i++];
                String datestr = data[i++];
                String string2 = string;
                if (control.length() == 2) {
                    string2 = data[i++];
                }

                Date date = null;
                try {
                    date = ref.parse(datestr);
                } catch (ParseException e) {
                    errln("FAIL: Internal test error; can't parse " + datestr);
                    continue;
                }
                try {
                    Date parsedate = fmt.parse(string);
                    assertEquals("\"" + currentPat + "\".parse(" + string + ")",
                                 univ.format(date),
                                 univ.format(parsedate));
                } catch (ParseException e2) {
                    errln("FAIL: \"" + currentPat + "\".parse(" + string + ") => " +
                          e2);
                    continue;
                }
                // 'f'
                assertEquals("\"" + currentPat + "\".format(" + datestr + ")",
                             string2,
                             fmt.format(date));
            }
            else {
                errln("FAIL: Invalid control string " + control);
                return;
            }
        }
    }
    /*
    public void TestJB4757(){
        DateFormat dfmt = DateFormat.getDateInstance(DateFormat.FULL, ULocale.ROOT);
    }
    */

    /*
     * Test case for formatToCharacterIterator
     */
    public void TestFormatToCharacterIterator() {
        // Generate pattern string including all pattern letters with various length
        AttributedCharacterIterator acit;
        final char SEPCHAR = '~';
        String[] patterns = new String[5];
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < patterns.length; i++) {
            sb.setLength(0);
            for (int j = 0; j < PATTERN_CHARS.length(); j++) {
                if (j != 0) {
                    for (int k = 0; k <= i; k++) {
                        sb.append(SEPCHAR);
                    }
                }
                char letter = PATTERN_CHARS.charAt(j);
                for (int k = 0; k <= i; k++) {
                    sb.append(letter);
                }
            }
            patterns[i] = sb.toString();
        }
        if (isVerbose()) {
            for (int i = 0; i < patterns.length; i++) {
                logln("patterns[" + i + "] = " + patterns[i]);
            }
        }

        Calendar cal = Calendar.getInstance();
        cal.set(2007, Calendar.JULY, 16, 8, 20, 25);
        cal.set(Calendar.MILLISECOND, 567);
        final Date d = cal.getTime();

        // Test AttributedCharacterIterator returned by SimpleDateFormat
        for (int i = 0; i < patterns.length; i++) {
            SimpleDateFormat sdf = new SimpleDateFormat(patterns[i]);
            acit = sdf.formatToCharacterIterator(d);
            int patidx = 0;

            while (true) {
                Map map = acit.getAttributes();
                int limit = acit.getRunLimit();
                if (map.isEmpty()) {
                    // Must be pattern literal - '~'
                    while (acit.getIndex() < limit) {
                        if (acit.current() != SEPCHAR) {
                            errln("FAIL: Invalid pattern literal at " + acit.current() + " in patterns[" + i + "]");
                        }
                        acit.next();
                    }
                } else {
                    Set keySet = map.keySet();
                    if (keySet.size() == 1) {
                        // Check the attribute
                        Iterator keyIterator = keySet.iterator();
                        DateFormat.Field attr = (DateFormat.Field)keyIterator.next();
                        if (!DATEFORMAT_FIELDS[patidx].equals(attr)) {
                            errln("FAIL: The attribute at " + acit.getIndex() + " in patterns[" + i + "" +
                                    "] is " + attr + " - Expected: " + DATEFORMAT_FIELDS[patidx]);
                        }
                    } else {
                        // SimpleDateFormat#formatToCharacterIterator never set multiple
                        // attributes to a single text run.
                        errln("FAIL: Multiple attributes were set");
                    }
                    patidx++;
                    // Move to the run limit
                    acit.setIndex(limit);
                }
                if (acit.current() == CharacterIterator.DONE) {
                    break;
                }
            }
        }

        // ChineseDateFormat has pattern letter 'l' for leap month marker in addition to regular DateFormat
        cal.clear();
        cal.set(2009, Calendar.JUNE, 22); // 26x78-5-30
        Date nonLeapMonthDate = cal.getTime(); // non-leap month
        cal.set(2009, Calendar.JUNE, 23); // 26x78-5*-1
        Date leapMonthDate = cal.getTime(); // leap month

        ChineseDateFormat cdf = new ChineseDateFormat("y'x'G-Ml-d", ULocale.US);
        acit = cdf.formatToCharacterIterator(nonLeapMonthDate);
        Set keys = acit.getAllAttributeKeys();
        if (keys.contains(ChineseDateFormat.Field.IS_LEAP_MONTH)) {
            errln("FAIL: IS_LEAP_MONTH attribute must not present for Chinese calendar date "
                    + cdf.format(nonLeapMonthDate));
        }
        acit = cdf.formatToCharacterIterator(leapMonthDate);
        keys = acit.getAllAttributeKeys();
        if (!keys.contains(ChineseDateFormat.Field.IS_LEAP_MONTH)) {
            errln("FAIL: IS_LEAP_MONTH attribute must present for Chinese calendar date "
                    + cdf.format(leapMonthDate));
        }
    }

    /*
     * API coverage test case for formatToCharacterIterator
     */
    public void TestFormatToCharacterIteratorCoverage() {
        // Calling formatToCharacterIterator, using various argument types
        DateFormat df = DateFormat.getDateTimeInstance();
        AttributedCharacterIterator acit = null;

        Calendar cal = Calendar.getInstance();
        try {
            acit = df.formatToCharacterIterator(cal);
            if (acit == null) {
                errln("FAIL: null AttributedCharacterIterator returned by formatToCharacterIterator(Calendar)");
            }
        } catch (IllegalArgumentException iae) {
            errln("FAIL: Calendar must be accepted by formatToCharacterIterator");
        }

        Date d = cal.getTime();
        try {
            acit = df.formatToCharacterIterator(d);
            if (acit == null) {
                errln("FAIL: null AttributedCharacterIterator returned by formatToCharacterIterator(Date)");
            }
        } catch (IllegalArgumentException iae) {
            errln("FAIL: Date must be accepted by formatToCharacterIterator");
        }

        Number num = new Long(d.getTime());
        try {
            acit = df.formatToCharacterIterator(num);
            if (acit == null) {
                errln("FAIL: null AttributedCharacterIterator returned by formatToCharacterIterator(Number)");
            }
        } catch (IllegalArgumentException iae) {
            errln("FAIL: Number must be accepted by formatToCharacterIterator");
        }

        boolean isException = false;
        String str = df.format(d);
        try {
            acit = df.formatToCharacterIterator(str);
            if (acit == null) {
                errln("FAIL: null AttributedCharacterIterator returned by formatToCharacterIterator(String)");
            }
        } catch (IllegalArgumentException iae) {
            logln("IllegalArgumentException is thrown by formatToCharacterIterator");
            isException = true;
        }
        if (!isException) {
            errln("FAIL: String must not be accepted by formatToCharacterIterator");
        }

        // DateFormat.Field#ofCalendarField and getCalendarField
        for (int i = 0; i < DATEFORMAT_FIELDS.length; i++) {
            int calField = DATEFORMAT_FIELDS[i].getCalendarField();
            if (calField != -1) {
                DateFormat.Field field = DateFormat.Field.ofCalendarField(calField);
                if (field != DATEFORMAT_FIELDS[i]) {
                    errln("FAIL: " + field + " is returned for a Calendar field " + calField
                            + " - Expected: " + DATEFORMAT_FIELDS[i]);
                }
            }
        }

        // IllegalArgument for ofCalendarField
        isException = false;
        try {
            DateFormat.Field.ofCalendarField(-1);
        } catch (IllegalArgumentException iae) {
            logln("IllegalArgumentException is thrown by ofCalendarField");
            isException = true;
        }
        if (!isException) {
            errln("FAIL: IllegalArgumentException must be thrown by ofCalendarField for calendar field value -1");
        }

        // ChineseDateFormat.Field#ofCalendarField and getCalendarField
        int ccalField = ChineseDateFormat.Field.IS_LEAP_MONTH.getCalendarField();
        if (ccalField != ChineseCalendar.IS_LEAP_MONTH) {
            errln("FAIL: ChineseCalendar field " + ccalField + " is returned for ChineseDateFormat.Field.IS_LEAP_MONTH.getCalendarField()");
        } else {
            DateFormat.Field cfield = ChineseDateFormat.Field.ofCalendarField(ccalField);
            if (cfield != ChineseDateFormat.Field.IS_LEAP_MONTH) {
                errln("FAIL: " + cfield + " is returned for a ChineseCalendar field " + ccalField
                        + " - Expected: " + ChineseDateFormat.Field.IS_LEAP_MONTH);
            }
        }
    }

    /*
     * Test for checking SimpleDateFormat/DateFormatSymbols creation
     * honor the calendar keyword in the given locale.  See ticket#6100
     */
    public void TestCalendarType() {
        final String testPattern = "GGGG y MMMM d EEEE";

        final ULocale[] testLocales = {
                new ULocale("de"),
                new ULocale("fr_FR@calendar=gregorian"),
                new ULocale("en@calendar=islamic"),
                new ULocale("ja_JP@calendar=japanese"),
                new ULocale("zh_Hans_CN@calendar=bogus"),
        };

        SimpleDateFormat[] formatters = new SimpleDateFormat[5];
        for (int i = 0; i < testLocales.length; i++) {
            // Create a locale with no keywords
            StringBuffer locStrBuf = new StringBuffer();
            if (testLocales[i].getLanguage().length() > 0) {
                locStrBuf.append(testLocales[i].getLanguage());
            }
            if (testLocales[i].getScript().length() > 0) {
                locStrBuf.append('_');
                locStrBuf.append(testLocales[i].getScript());
            }
            if (testLocales[i].getCountry().length() > 0) {
                locStrBuf.append('_');
                locStrBuf.append(testLocales[i].getCountry());
            }
            ULocale locNoKeywords = new ULocale(locStrBuf.toString());

            Calendar cal = Calendar.getInstance(testLocales[i]);

            // Calendar getDateFormat method
            DateFormat df = cal.getDateTimeFormat(DateFormat.MEDIUM, DateFormat.MEDIUM, locNoKeywords);
            if (df instanceof SimpleDateFormat) {
                formatters[0] = (SimpleDateFormat)df;
                formatters[0].applyPattern(testPattern);
            } else {
                formatters[0] = null;
            }

            // DateFormat constructor with locale
            df = DateFormat.getDateInstance(DateFormat.MEDIUM, testLocales[i]);
            if (df instanceof SimpleDateFormat) {
                formatters[1] = (SimpleDateFormat)df;
                formatters[1].applyPattern(testPattern);
            } else {
                formatters[1] = null;
            }

            // DateFormat constructor with Calendar
            df = DateFormat.getDateInstance(cal, DateFormat.MEDIUM, locNoKeywords);
            if (df instanceof SimpleDateFormat) {
                formatters[2] = (SimpleDateFormat)df;
                formatters[2].applyPattern(testPattern);
            } else {
                formatters[2] = null;
            }

            // SimpleDateFormat constructor
            formatters[3] = new SimpleDateFormat(testPattern, testLocales[i]);
 
            // SimpleDateFormat with DateFormatSymbols
            DateFormatSymbols dfs = new DateFormatSymbols(testLocales[i]);
            formatters[4] = new SimpleDateFormat(testPattern, dfs, testLocales[i]);

            // All SimpleDateFormat instances should produce the exact
            // same result.
            String expected = null;
            Date d = new Date();
            for (int j = 0; j < formatters.length; j++) {
                if (formatters[j] != null) {
                    String tmp = formatters[j].format(d);
                    if (expected == null) {
                        expected = tmp;
                    } else if (!expected.equals(tmp)) {
                        errln("FAIL: formatter[" + j + "] returned \"" + tmp + "\" in locale " +
                                testLocales[i] + " - expected: " + expected);
                    }
                }
            }
        }
    }

    /*
     * Test for format/parse method with calendar which is different
     * from what DateFormat instance internally use.  See ticket#6420.
     */
    public void TestRoundtripWithCalendar() {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        TimeZone gmt = TimeZone.getTimeZone("Etc/GMT");

        final Calendar[] calendars = {
            new GregorianCalendar(tz),
            new BuddhistCalendar(tz),
            new HebrewCalendar(tz),
            new IslamicCalendar(tz),
            new JapaneseCalendar(tz),
        };

        final String pattern = "GyMMMMdEEEEHHmmssVVVV";

        //FIXME The formatters commented out below are currently failing because of
        // the calendar calculation problem reported by #6691

        // The order of test formatters mus match the order of calendars above.
        final DateFormat[] formatters = {
            DateFormat.getPatternInstance(pattern, new ULocale("en_US")), //calendar=gregorian
            DateFormat.getPatternInstance(pattern, new ULocale("th_TH")), //calendar=buddhist
            DateFormat.getPatternInstance(pattern, new ULocale("he_IL@calendar=hebrew")),
//            DateFormat.getPatternInstance(pattern, new ULocale("ar_EG@calendar=islamic")),
//            DateFormat.getPatternInstance(pattern, new ULocale("ja_JP@calendar=japanese")),
        };

        Date d = new Date();
        StringBuffer buf = new StringBuffer();
        FieldPosition fpos = new FieldPosition(0);
        ParsePosition ppos = new ParsePosition(0);

        for (int i = 0; i < formatters.length; i++) {
            buf.setLength(0);
            fpos.setBeginIndex(0);
            fpos.setEndIndex(0);
            calendars[i].setTime(d);

            // Normal case output - the given calendar matches the calendar
            // used by the formatter
            formatters[i].format(calendars[i], buf, fpos);
            String refStr = buf.toString();

            for (int j = 0; j < calendars.length; j++) {
                if (j == i) {
                    continue;
                }
                buf.setLength(0);
                fpos.setBeginIndex(0);
                fpos.setEndIndex(0);
                calendars[j].setTime(d);

                // Even the different calendar type is specified,
                // we should get the same result.
                formatters[i].format(calendars[j], buf, fpos);
                if (!refStr.equals(buf.toString())) {
                    errln("FAIL: Different format result with a different calendar for the same time -"
                            + "\n Reference calendar type=" + calendars[i].getType()
                            + "\n Another calendar type=" + calendars[j].getType()
                            + "\n Expected result=" + refStr
                            + "\n Actual result=" + buf.toString());
                }
            }

            calendars[i].setTimeZone(gmt);
            calendars[i].clear();
            ppos.setErrorIndex(-1);
            ppos.setIndex(0);

            // Normal case parse result - the given calendar matches the calendar
            // used by the formatter
            formatters[i].parse(refStr, calendars[i], ppos);

            for (int j = 0; j < calendars.length; j++) {
                if (j == i) {
                    continue;
                }
                calendars[j].setTimeZone(gmt);
                calendars[j].clear();
                ppos.setErrorIndex(-1);
                ppos.setIndex(0);

                // Even the different calendar type is specified,
                // we should get the same time and time zone.
                formatters[i].parse(refStr, calendars[j], ppos);
                if (calendars[i].getTimeInMillis() != calendars[j].getTimeInMillis()
                        || !calendars[i].getTimeZone().equals(calendars[j].getTimeZone())) {
                    errln("FAIL: Different parse result with a different calendar for the same string -"
                            + "\n Reference calendar type=" + calendars[i].getType()
                            + "\n Another calendar type=" + calendars[j].getType()
                            + "\n Date string=" + refStr
                            + "\n Expected time=" + calendars[i].getTimeInMillis()
                            + "\n Expected time zone=" + calendars[i].getTimeZone().getID()
                            + "\n Actual time=" + calendars[j].getTimeInMillis()
                            + "\n Actual time zone=" + calendars[j].getTimeZone().getID());
                }
            }
        }
    }

    // based on TestRelativeDateFormat() in icu/trunk/source/test/cintltst/cdattst.c
    public void TestRelativeDateFormat() {
        ULocale loc = ULocale.US;
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        Calendar cal = new GregorianCalendar(tz, loc);
        Date now = new Date();
        cal.setTime(now);
        cal.set(Calendar.HOUR_OF_DAY, 18);
        cal.set(Calendar.MINUTE, 49);
        cal.set(Calendar.SECOND, 0);
        Date today = cal.getTime();
        String minutesStr = "49"; // minutes string to search for in formatted result
        int[] dateStylesList = { DateFormat.RELATIVE_FULL, DateFormat.RELATIVE_LONG, DateFormat.RELATIVE_MEDIUM, DateFormat.RELATIVE_SHORT };

        for (int i = 0; i < dateStylesList.length; i++) {
            int dateStyle = dateStylesList[i];
            DateFormat fmtRelDateTime = DateFormat.getDateTimeInstance(dateStyle, DateFormat.SHORT, loc);
            DateFormat fmtRelDate = DateFormat.getDateInstance(dateStyle, loc);
            DateFormat fmtTime = DateFormat.getTimeInstance(DateFormat.SHORT, loc);

            for (int dayOffset = -2; dayOffset <= 2; dayOffset++ ) {
                StringBuffer dateTimeStr = new StringBuffer(64);
                StringBuffer dateStr = new StringBuffer(64);
                StringBuffer timeStr = new StringBuffer(64);
                FieldPosition fp = new FieldPosition(DateFormat.MINUTE_FIELD);
                cal.setTime(today);
                cal.add(Calendar.DATE, dayOffset);

                fmtRelDateTime.format(cal, dateTimeStr, fp);
                fmtRelDate.format(cal, dateStr, new FieldPosition(0) );
                fmtTime.format(cal, timeStr, new FieldPosition(0) );

                // check that dateStr is in dateTimeStr
                if ( dateTimeStr.toString().indexOf( dateStr.toString() ) < 0 ) {
                    errln("relative date string not found in datetime format with timeStyle SHORT, dateStyle " +
                            dateStyle + " for dayOffset " + dayOffset );
                    errln("datetime format is " + dateTimeStr.toString() + ", date string is " + dateStr.toString() );
                }
                // check that timeStr is in dateTimeStr
                if ( dateTimeStr.toString().indexOf( timeStr.toString() ) < 0 ) {
                    errln("short time string not found in datetime format with timeStyle SHORT, dateStyle " +
                            dateStyle + " for dayOffset " + dayOffset );
                    errln("datetime format is " + dateTimeStr.toString() + ", time string is " + timeStr.toString() );
                }
                // check index of minutesStr
                int minutesStrIndex = dateTimeStr.toString().indexOf( minutesStr );
                if ( fp.getBeginIndex() != minutesStrIndex ) {
                    errln("FieldPosition beginIndex " + fp.getBeginIndex() + " instead of " + minutesStrIndex + " for datetime format with timeStyle SHORT, dateStyle " +
                            dateStyle + " for dayOffset " + dayOffset );
                    errln("datetime format is " + dateTimeStr.toString() );
                }
            }
        }
    }

    public void Test6880() {
        Date d1, d2, dp1, dp2, dexp1, dexp2;
        String s1, s2;

        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        GregorianCalendar gcal = new GregorianCalendar(tz);

        gcal.clear();
        gcal.set(1910, Calendar.JANUARY, 1, 12, 00);    // offset 8:05:52
        d1 = gcal.getTime();

        gcal.clear();
        gcal.set(1950, Calendar.JANUARY, 1, 12, 00);    // offset 8:00
        d2 = gcal.getTime();

        gcal.clear();
        gcal.set(1970, Calendar.JANUARY, 1, 12, 00);
        dexp2 = gcal.getTime();
        dexp1 = new Date(dexp2.getTime() - (5*60 + 52)*1000);   // subtract 5m52s

        DateFormat fmt = DateFormat.getTimeInstance(DateFormat.FULL, new ULocale("zh"));
        fmt.setTimeZone(tz);

        s1 = fmt.format(d1);
        s2 = fmt.format(d2);

        try {
            dp1 = fmt.parse(s1);
            dp2 = fmt.parse(s2);

            if (!dp1.equals(dexp1)) {
                errln("FAIL: Failed to parse " + s1 + " parsed: " + dp1 + " expected: " + dexp1);
            }
            if (!dp2.equals(dexp2)) {
                errln("FAIL: Failed to parse " + s2 + " parsed: " + dp2 + " expected: " + dexp2);
            }
        } catch (ParseException pe) {
            errln("FAIL: Parse failure");
        }
    }
}
