/*****************************************************************************************
 *
 *   Copyright (C) 1996-2004, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **/

/** 
 * Port From:   JDK 1.4b1 : java.text.Format.IntlTestDateFormatAPI
 * Source File: java/text/format/IntlTestDateFormatAPI.java
 **/

/*
    @test 1.4 98/03/06
    @summary test International Date Format API
*/

package com.ibm.icu.dev.test.format;

import com.ibm.icu.util.*;
import com.ibm.icu.text.*;
import java.util.Locale;
import java.util.Date;
import java.text.ParsePosition;
import java.text.FieldPosition;
import java.text.ParseException;

public class IntlTestDateFormatAPI extends com.ibm.icu.dev.test.TestFmwk
{
    public static void main(String[] args) throws Exception {
        new IntlTestDateFormatAPI().run(args);
    }

    // Test that the equals method works correctly.
    public void TestEquals()
    {
        // Create two objects at different system times
        DateFormat a = DateFormat.getInstance();
        Date start = Calendar.getInstance().getTime();
        while (true) {
            // changed to remove compiler warnings.
            if (!start.equals(Calendar.getInstance().getTime())) {
                break; // Wait for time to change
            }
        }
        DateFormat b = DateFormat.getInstance();

        if (!(a.equals(b)))
            errln("FAIL: DateFormat objects created at different times are unequal.");

        // Why has this test been disabled??? - aliu
//        if (b instanceof SimpleDateFormat)
//        {
//            //double ONE_YEAR = 365*24*60*60*1000.0; //The variable is never used
//            try {
//                ((SimpleDateFormat)b).setTwoDigitStartDate(start.getTime() + 50*ONE_YEAR);
//                if (a.equals(b))
//                    errln("FAIL: DateFormat objects with different two digit start dates are equal.");
//            }
//            catch (Exception e) {
//                errln("FAIL: setTwoDigitStartDate failed.");
//            }
//        }
    }

    // This test checks various generic API methods in DateFormat to achieve 100% API coverage.
    public void TestAPI()
    {
        logln("DateFormat API test---"); logln("");
        Locale.setDefault(Locale.ENGLISH);


        // ======= Test constructors

        logln("Testing DateFormat constructors");

        DateFormat def = DateFormat.getInstance();
        DateFormat fr = DateFormat.getTimeInstance(DateFormat.FULL, Locale.FRENCH);
        DateFormat it = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.ITALIAN);
        DateFormat de = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG, Locale.GERMAN);

        // ======= Test equality

        logln("Testing equality operator");

        if( fr.equals(it) ) {
            errln("ERROR: equals failed");
        }

        // ======= Test various format() methods

        logln("Testing various format() methods");

        Date d = new Date((long)837039928046.0);

        StringBuffer res1 = new StringBuffer();
        StringBuffer res2 = new StringBuffer();
        String res3 = new String();
        FieldPosition pos1 = new FieldPosition(0);
        FieldPosition pos2 = new FieldPosition(0);

        res1 = fr.format(d, res1, pos1);
        logln("" + d.getTime() + " formatted to " + res1);

        res2 = it.format(d, res2, pos2);
        logln("" + d.getTime() + " formatted to " + res2);

        res3 = de.format(d);
        logln("" + d.getTime() + " formatted to " + res3);

        // ======= Test parse()

        logln("Testing parse()");

        String text = new String("02/03/76 2:50 AM, CST");
        Object result1 = new Date();
        Date result2 = new Date();
        Date result3 = new Date();
        ParsePosition pos = new ParsePosition(0);
        ParsePosition pos01 = new ParsePosition(0);

        result1 = def.parseObject(text, pos);
        if (result1 == null) {
            errln("ERROR: parseObject() failed for " + text);
        }
        logln(text + " parsed into " + ((Date)result1).getTime());

        try {
            result2 = def.parse(text);
        }
        catch (ParseException e) {
            errln("ERROR: parse() failed");
        }
        logln(text + " parsed into " + result2.getTime());

        result3 = def.parse(text, pos01);
        if (result3 == null) {
            errln("ERROR: parse() failed for " + text);
        }
        logln(text + " parsed into " + result3.getTime());


        // ======= Test getters and setters

        logln("Testing getters and setters");

        final Locale[] locales = DateFormat.getAvailableLocales();
        long count = locales.length;
        logln("Got " + count + " locales" );
        for(int i = 0; i < count; i++) {
            String name;
            name = locales[i].getDisplayName();
            logln(name);
        }

        fr.setLenient(it.isLenient());
        if(fr.isLenient() != it.isLenient()) {
            errln("ERROR: setLenient() failed");
        }

        final Calendar cal = def.getCalendar();
        Calendar newCal = (Calendar) cal.clone();
        de.setCalendar(newCal);
        it.setCalendar(newCal);
        if( ! de.getCalendar().equals(it.getCalendar())) {
            errln("ERROR: set Calendar() failed");
        }

        final NumberFormat nf = def.getNumberFormat();
        NumberFormat newNf = (NumberFormat) nf.clone();
        de.setNumberFormat(newNf);
        it.setNumberFormat(newNf);
        if( ! de.getNumberFormat().equals(it.getNumberFormat())) {
            errln("ERROR: set NumberFormat() failed");
        }

        final TimeZone tz = def.getTimeZone();
        TimeZone newTz = (TimeZone) tz.clone();
        de.setTimeZone(newTz);
        it.setTimeZone(newTz);
        if( ! de.getTimeZone().equals(it.getTimeZone())) {
            errln("ERROR: set TimeZone() failed");
        }

        // ======= Test getStaticClassID()

//        logln("Testing instanceof()");

//        try {
//            DateFormat test = new SimpleDateFormat();

//            if (! (test instanceof SimpleDateFormat)) {
//                errln("ERROR: instanceof failed");
//            }
//        }
//        catch (Exception e) {
//            errln("ERROR: Couldn't create a DateFormat");
//        }
    }
}
