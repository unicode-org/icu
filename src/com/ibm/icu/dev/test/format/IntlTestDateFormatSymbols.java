/*****************************************************************************************
 *
 *   Copyright (C) 1996-2004, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **/

/** 
 * Port From:   JDK 1.4b1 : java.text.Format.IntlTestDateFormatSymbols
 * Source File: java/text/format/IntlTestDateFormatSymbols.java
 **/
 
/*
    @test 1.4 98/03/06
    @summary test International Date Format Symbols
*/

package com.ibm.icu.dev.test.format;

import com.ibm.icu.text.*;
import java.util.Locale;

public class IntlTestDateFormatSymbols extends com.ibm.icu.dev.test.TestFmwk
{
    public static void main(String[] args) throws Exception {
        new IntlTestDateFormatSymbols().run(args);
    }

    // Test getMonths
    public void TestGetMonths()
    {
        final String[] month;
        DateFormatSymbols symbol;

        symbol=new DateFormatSymbols(Locale.getDefault());

        month=symbol.getMonths();
        int cnt = month.length;

        logln("size = " + cnt);

        for (int i=0; i<cnt; ++i)
        {
            logln(month[i]);
        }
    }

    // Test the API of DateFormatSymbols; primarily a simple get/set set.
    public void TestSymbols()
    {
        DateFormatSymbols fr = new DateFormatSymbols(Locale.FRENCH);

        DateFormatSymbols en = new DateFormatSymbols(Locale.ENGLISH);

        if(en.equals(fr)) {
            errln("ERROR: English DateFormatSymbols equal to French");
        }

        // just do some VERY basic tests to make sure that get/set work

        long count;
        final String[] eras = en.getEras();
        fr.setEras(eras);
        final String[] eras1 = fr.getEras();
        count = eras.length;
        if( count != eras1.length) {
            errln("ERROR: setEras() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! eras[i].equals(eras1[i])) {
                    errln("ERROR: setEras() failed (different string values)");
                }
            }
        }


        final String[] months = en.getMonths();
        fr.setMonths(months);
        final String[] months1 = fr.getMonths();
        count = months.length;
        if( count != months1.length) {
            errln("ERROR: setMonths() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! months[i].equals(months1[i])) {
                    errln("ERROR: setMonths() failed (different string values)");
                }
            }
        }

        final String[] shortMonths = en.getShortMonths();
        fr.setShortMonths(shortMonths);
        final String[] shortMonths1 = fr.getShortMonths();
        count = shortMonths.length;
        if( count != shortMonths1.length) {
            errln("ERROR: setShortMonths() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! shortMonths[i].equals(shortMonths1[i])) {
                    errln("ERROR: setShortMonths() failed (different string values)");
                }
            }
        }

        final String[] weekdays = en.getWeekdays();
        fr.setWeekdays(weekdays);
        final String[] weekdays1 = fr.getWeekdays();
        count = weekdays.length;
        if( count != weekdays1.length) {
            errln("ERROR: setWeekdays() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! weekdays[i].equals(weekdays1[i])) {
                    errln("ERROR: setWeekdays() failed (different string values)");
                }
            }
        }

        final String[] shortWeekdays = en.getShortWeekdays();
        fr.setShortWeekdays(shortWeekdays);
        final String[] shortWeekdays1 = fr.getShortWeekdays();
        count = shortWeekdays.length;
        if( count != shortWeekdays1.length) {
            errln("ERROR: setShortWeekdays() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! shortWeekdays[i].equals(shortWeekdays1[i])) {
                    errln("ERROR: setShortWeekdays() failed (different string values)");
                }
            }
        }

        final String[] ampms = en.getAmPmStrings();
        fr.setAmPmStrings(ampms);
        final String[] ampms1 = fr.getAmPmStrings();
        count = ampms.length;
        if( count != ampms1.length) {
            errln("ERROR: setAmPmStrings() failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! ampms[i].equals(ampms1[i])) {
                    errln("ERROR: setAmPmStrings() failed (different string values)");
                }
            }
        }

        long rowCount = 0, columnCount = 0;
        final String[][] strings = en.getZoneStrings();
        fr.setZoneStrings(strings);
        final String[][] strings1 = fr.getZoneStrings();
        rowCount = strings.length;
        for(int i = 0; i < rowCount; i++) {
            columnCount = strings[i].length;
            for(int j = 0; j < columnCount; j++) {
                if( strings[i][j] != strings1[i][j] ) {
                    errln("ERROR: setZoneStrings() failed");
                }
            }
        }

//        final String pattern = DateFormatSymbols.getPatternChars();

        String localPattern; // pat1, pat2; //The variable is never used
        localPattern = en.getLocalPatternChars();
        fr.setLocalPatternChars(localPattern);
        if(! en.getLocalPatternChars().equals(fr.getLocalPatternChars())) {
            errln("ERROR: setLocalPatternChars() failed");
        }


        //DateFormatSymbols foo = new DateFormatSymbols(); //The variable is never used

        en = (DateFormatSymbols) fr.clone();

        if(! en.equals(fr)) {
            errln("ERROR: Clone failed");
        }
    }
}
