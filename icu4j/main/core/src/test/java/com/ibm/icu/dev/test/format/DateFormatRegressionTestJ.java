// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2001-2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */

/*
 * New added, 2001-10-17 [Jing/GCL]
 */

package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DateFormatRegressionTestJ extends CoreTestFmwk {

    private static final String TIME_STRING = "2000/11/17 08:01:00";
    private static final long UTC_LONG = 974476860000L;
    private SimpleDateFormat sdf_;

    @Before
    public void init() throws Exception {
        sdf_ = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    }

    // Return value of getAmPmStrings
    @Test
    public void Test4103926() {
        String act_Ampms[];
        String exp_Ampms[] = {"AM", "PM"};
        Locale.setDefault(Locale.US);

        DateFormatSymbols dfs = new DateFormatSymbols();
        act_Ampms = dfs.getAmPmStrings();
        if (act_Ampms.length != exp_Ampms.length) {
            errln("The result is not expected!");
        } else {
            for (int i = 0; i < act_Ampms.length; i++) {
                if (!act_Ampms[i].equals(exp_Ampms[i])) errln("The result is not expected!");
            }
        }
    }

    // Missing digit in millisecond format in SimpleDateFormat
    @Test
    public void Test4148168() {
        Date d = new Date(1002705212906L);
        String[] ISOPattern = {
            "''yyyy-MM-dd-hh.mm.ss.S''", "''yyyy-MM-dd-hh.mm.ss.SS''",
            "''yyyy-MM-dd-hh.mm.ss.SSS''", "''yyyy-MM-dd-hh.mm.ss.SSSS''",
            "''yyyy-MM-dd-hh.mm.ss.SSSSS''", "''yyyy-MM-dd-hh.mm.ss.SSSSSS''",
            "''yyyy-MM-dd-hh.mm.ss.SSSSSSS''", "''yyyy-MM-dd-hh.mm.ss.SSS000''"
        };
        SimpleDateFormat aSimpleDF = (SimpleDateFormat) DateFormat.getDateTimeInstance();

        for (int i = 0; i < ISOPattern.length; i++) {
            aSimpleDF.applyPattern(ISOPattern[i]);
            logDebug("Pattern = " + aSimpleDF.toPattern());
            logDebug("Format = " + aSimpleDF.format(d));
        }
    }

    // DateFormat getDateTimeInstance(int, int), invalid styles no exception
    @Test
    public void Test4213086() {
        Date someDate = new Date();
        String d = null;
        try {
            DateFormat df2 = DateFormat.getDateTimeInstance(2, -2);
            d = df2.format(someDate);
            errln("we should catch an exception here");
        } catch (Exception e) {
            logDebug("dateStyle = 2" + "\t timeStyle = -2");
            logDebug("Exception caught!");
        }

        try {
            DateFormat df3 = DateFormat.getDateTimeInstance(4, 2);
            d = df3.format(someDate);
            errln("we should catch an exception here");
        } catch (Exception e) {
            logDebug("dateStyle = 4" + "\t timeStyle = 2");
            logDebug("Exception caught!");
            logDebug("********************************************");
        }

        try {
            DateFormat df4 = DateFormat.getDateTimeInstance(-12, -12);
            d = df4.format(someDate);
            errln("we should catch an exception here");
        } catch (Exception e) {
            logDebug("dateStyle = -12" + "\t timeStyle = -12");
            logDebug("Exception caught!");
            logDebug("********************************************");
        }

        try {
            DateFormat df5 = DateFormat.getDateTimeInstance(2, 123);
            d = df5.format(someDate);
            errln("we should catch an exception here");
        } catch (Exception e) {
            logDebug("dateStyle = 2" + "\t timeStyle = 123");
            logDebug("Exception caught!");
            logDebug("********************************************");
        }
        // read the value in d to get rid of the warning
        if (d != null) {
            logDebug("The value of d: " + d);
        }
    }

    // DateFormat.format works wrongly?
    @Test
    public void Test4250359() {
        Locale.setDefault(Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(101 + 1900, 9, 9, 17, 53);
        Date d = cal.getTime();
        DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT);
        String act_result = tf.format(d);
        String exp_result = "5:53\u202FPM";

        if (!act_result.equals(exp_result)) {
            errln("The result is not expected");
        }
    }

    // pattern "s.S, parse '1ms'"
    @Test
    public void Test4253490() {
        Date d = new Date(1002705212231L);

        String[] ISOPattern = {
            "''yyyy-MM-dd-hh.mm.ss.S''",
            "''yyyy-MM-dd-hh.mm.ss.SS''",
            "''yyyy-MM-dd-hh.mm.ss.SSS''",
            "''yyyy-MM-dd-hh.mm.ss.SSSS''",
            "''yyyy-MM-dd-hh.mm.ss.SSSSS''",
            "''yyyy-MM-dd-hh.mm.ss.SSSSSS''",
            "''yyyy-MM-dd-hh.mm.ss.SSSSSSS''"
        };

        SimpleDateFormat aSimpleDF = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        for (int i = 0; i < ISOPattern.length; i++) {
            aSimpleDF.applyPattern(ISOPattern[i]);
            logDebug("Pattern = " + aSimpleDF.toPattern());
            logDebug("Format = " + aSimpleDF.format(d));
        }
    }

    // about regression test
    @Test
    public void Test4266432() {
        Locale startLocale = Locale.getDefault();

        Locale.setDefault(Locale.JAPAN);
        Locale loc = Locale.getDefault();
        String dateFormat = "MM/dd/yy HH:mm:ss zzz";
        SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);

        ParsePosition p0 = new ParsePosition(0);
        logDebug("Under  " + loc + "  locale");
        Date d = fmt.parse("01/22/92 04:52:00 GMT", p0);
        logDebug(d.toString());

        Locale.setDefault(startLocale);
    }

    // SimpleDateFormat inconsistent for number of digits for years
    @Test
    public void Test4358730() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2001, 11, 10);
        Date today = cal.getTime();

        sdf.applyPattern("MM d y");
        logDebug(sdf.format(today));
        sdf.applyPattern("MM d yy");
        logDebug(sdf.format(today));

        sdf.applyPattern("MM d yyy");
        logDebug(sdf.format(today));

        sdf.applyPattern("MM d yyyy");
        logDebug(sdf.format(today));

        sdf.applyPattern("MM d yyyyy");
        logDebug(sdf.format(today));
    }

    // Parse invalid string
    @Test
    public void Test4375399() {
        final String pattern = new String("yyyy.MM.dd G 'at' hh:mm:ss z");
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.JAPAN);
        try {
            Date currentTime =
                    sdf.parse(
                            "vggf 20  01.0 9.29 ap. J.-C. at 05:26:33 GMT+08:00",
                            new ParsePosition(0));
            if (currentTime == null) logDebug("parse right");
        } catch (Exception e) {
            errln("Error");
        }
    }

    /*
    @Test
    public void Test4407042() {
        DateParseThread d1 = new DateParseThread();
        DateFormatThread d2 = new DateFormatThread();
        d1.start();
        d2.start();
        try {
            logln("test");
            Thread.sleep(1000);
        } catch (Exception e) {}
    }*/

    @Test
    public void Test4468663() {
        Date d = new Date(-93716671115767L);
        String origin_d = d.toString();
        String str;
        final String pattern = new String("EEEE, MMMM d, yyyy");
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);

        if (sdf.getTimeZone().useDaylightTime()) {
            logDebug("original date: " + origin_d.toString());
            str = sdf.format(d);
            logDebug(" after format----->" + str);

            d = sdf.parse(str, new ParsePosition(0));
            logDebug(" after parse----->" + d.toString());

            str = sdf.format(d);
            logDebug(" after format----->" + str);

            d = sdf.parse(str, new ParsePosition(0));
            logDebug(" after parse----->" + d.toString());

            str = sdf.format(d);
            logDebug(" after format----->" + str);
        }
    }

    // Class used by Test4407042
    class DateParseThread extends Thread {
        @Override
        public void run() {
            SimpleDateFormat sdf = sdf_.clone();
            TimeZone defaultTZ = TimeZone.getDefault();
            TimeZone PST = TimeZone.getTimeZone("PST");
            int defaultOffset = defaultTZ.getRawOffset();
            int PSTOffset = PST.getRawOffset();
            int offset = defaultOffset - PSTOffset;
            long ms = UTC_LONG - offset;
            try {
                int i = 0;
                while (i < 10000) {
                    Date date = sdf.parse(TIME_STRING);
                    long t = date.getTime();
                    i++;
                    if (t != ms) {
                        throw new ParseException(
                                "Parse Error: "
                                        + i
                                        + " ("
                                        + sdf.format(date)
                                        + ") "
                                        + t
                                        + " != "
                                        + ms,
                                0);
                    }
                }
            } catch (Exception e) {
                errln("parse error: " + e.getMessage());
            }
        }
    }

    // Class used by Test4407042
    class DateFormatThread extends Thread {
        @Override
        public void run() {
            SimpleDateFormat sdf = sdf_.clone();
            TimeZone tz = TimeZone.getTimeZone("PST");
            sdf.setTimeZone(tz);
            int i = 0;
            while (i < 10000) {
                i++;
                String s = sdf.format(new Date(UTC_LONG));
                if (!s.equals(TIME_STRING)) {
                    errln("Format Error: " + i + " " + s + " != " + TIME_STRING);
                }
            }
        }
    }
}
