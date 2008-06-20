/*****************************************************************************************
 *
 *   Copyright (C) 1996-2006, International Business Machines
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

    public void TestGetMonths2()
    {
        DateFormatSymbols symbol;
        symbol=new DateFormatSymbols(Locale.getDefault());

        int[] context = {DateFormatSymbols.STANDALONE, DateFormatSymbols.FORMAT};
        int[] width = {DateFormatSymbols.WIDE, DateFormatSymbols.ABBREVIATED, DateFormatSymbols.NARROW};
        
        for (int i = 0; i < context.length; i++) {
            for (int j = 0; j < width.length; j++) {
                String[] month =symbol.getMonths(context[i],width[j]);
                int cnt = month.length;

                logln("size = " + cnt);

                for (int k = 0; k < month.length; k++) {
                    logln(month[k]);
                }
            }
        }
    }
    
    public void TestGetWeekdays2(){
        DateFormatSymbols symbol;
        symbol=new DateFormatSymbols(Locale.getDefault());

        int[] context = {DateFormatSymbols.STANDALONE, DateFormatSymbols.FORMAT};
        int[] width = {DateFormatSymbols.WIDE, DateFormatSymbols.ABBREVIATED, DateFormatSymbols.NARROW};
        
        for (int i = 0; i < context.length; i++) {
            for (int j = 0; j < width.length; j++) {
                String[] wd =symbol.getWeekdays(context[i],width[j]);
                int cnt = wd.length;

                logln("size = " + cnt);

                for (int k = 0; k < wd.length; k++) {
                    logln(wd[k]);
                }
            }
        }
        
    }
    
    public void TestGetEraNames(){
        DateFormatSymbols symbol;
        symbol=new DateFormatSymbols(Locale.getDefault());
        String[] s = symbol.getEraNames();
        for (int i = 0; i < s.length; i++) {
            logln(s[i]);
        }
        
    }

    // Test the API of DateFormatSymbols; primarily a simple get/set set.
    public void TestSymbols()
    {
        DateFormatSymbols fr = new DateFormatSymbols(Locale.FRENCH);
        DateFormatSymbols fr2 = new DateFormatSymbols(Locale.FRENCH);

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

        final String[] wideMonths = en.getMonths(DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        fr2.setMonths(wideMonths,DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        final String[] wideMonths1 = fr2.getMonths(DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        count = wideMonths.length;
        if( count != wideMonths1.length) {
            errln("ERROR: setMonths(FORMAT,WIDE) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! wideMonths[i].equals(wideMonths1[i])) {
                    errln("ERROR: setMonths(FORMAT,WIDE) failed (different string values)");
                }
            }
        }

        final String[] abbrMonths = en.getMonths(DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        fr2.setMonths(abbrMonths,DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        final String[] abbrMonths1 = fr2.getMonths(DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        count = abbrMonths.length;
        if( count != abbrMonths1.length) {
            errln("ERROR: setMonths(FORMAT,ABBREVIATED) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! abbrMonths[i].equals(abbrMonths1[i])) {
                    errln("ERROR: setMonths(FORMAT,ABBREVIATED) failed (different string values)");
                }
            }
        }

        final String[] narrowMonths = en.getMonths(DateFormatSymbols.FORMAT,DateFormatSymbols.NARROW);
        fr.setMonths(narrowMonths,DateFormatSymbols.FORMAT,DateFormatSymbols.NARROW);
        final String[] narrowMonths1 = fr.getMonths(DateFormatSymbols.FORMAT,DateFormatSymbols.NARROW);
        count = narrowMonths.length;
        if( count != narrowMonths1.length) {
            errln("ERROR: setMonths(FORMAT,NARROW) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! narrowMonths[i].equals(narrowMonths1[i])) {
                    errln("ERROR: setMonths(FORMAT,NARROW) failed (different string values)");
                }
            }
        }

        final String[] standaloneMonths = en.getMonths(DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        fr.setMonths(standaloneMonths,DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        final String[] standaloneMonths1 = fr.getMonths(DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        count = standaloneMonths.length;
        if( count != standaloneMonths1.length) {
            errln("ERROR: setMonths(STANDALONE,WIDE) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! standaloneMonths[i].equals(standaloneMonths1[i])) {
                    errln("ERROR: setMonths(STANDALONE,WIDE) failed (different string values)");
                }
            }
        }

        final String[] standaloneShortMonths = en.getMonths(DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        fr.setMonths(standaloneShortMonths,DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        final String[] standaloneShortMonths1 = fr.getMonths(DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        count = standaloneShortMonths.length;
        if( count != standaloneShortMonths1.length) {
            errln("ERROR: setMonths(STANDALONE,ABBREVIATED) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! standaloneShortMonths[i].equals(standaloneShortMonths1[i])) {
                    errln("ERROR: setMonths(STANDALONE,ABBREVIATED) failed (different string values)");
                }
            }
        }

        final String[] standaloneNarrowMonths = en.getMonths(DateFormatSymbols.STANDALONE,DateFormatSymbols.NARROW);
        fr.setMonths(standaloneNarrowMonths,DateFormatSymbols.STANDALONE,DateFormatSymbols.NARROW);
        final String[] standaloneNarrowMonths1 = fr.getMonths(DateFormatSymbols.STANDALONE,DateFormatSymbols.NARROW);
        count = standaloneNarrowMonths.length;
        if( count != standaloneNarrowMonths1.length) {
            errln("ERROR: setMonths(STANDALONE,NARROW) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! standaloneNarrowMonths[i].equals(standaloneNarrowMonths1[i])) {
                    errln("ERROR: setMonths(STANDALONE,NARROW) failed (different string values)");
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

        final String[] wideWeekdays = en.getWeekdays(DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        fr2.setWeekdays(wideWeekdays,DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        final String[] wideWeekdays1 = fr2.getWeekdays(DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        count = wideWeekdays.length;
        if( count != wideWeekdays1.length) {
            errln("ERROR: setWeekdays(FORMAT,WIDE) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! wideWeekdays[i].equals(wideWeekdays1[i])) {
                    errln("ERROR: setWeekdays(FORMAT,WIDE) failed (different string values)");
                }
            }
        }

        final String[] abbrWeekdays = en.getWeekdays(DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        fr2.setWeekdays(abbrWeekdays,DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        final String[] abbrWeekdays1 = fr2.getWeekdays(DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        count = abbrWeekdays.length;
        if( count != abbrWeekdays1.length) {
            errln("ERROR: setWeekdays(FORMAT,ABBREVIATED) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! abbrWeekdays[i].equals(abbrWeekdays1[i])) {
                    errln("ERROR: setWeekdays(FORMAT,ABBREVIATED) failed (different string values)");
                }
            }
        }

        final String[] narrowWeekdays = en.getWeekdays(DateFormatSymbols.FORMAT,DateFormatSymbols.NARROW);
        fr.setWeekdays(narrowWeekdays,DateFormatSymbols.FORMAT,DateFormatSymbols.NARROW);
        final String[] narrowWeekdays1 = fr.getWeekdays(DateFormatSymbols.FORMAT,DateFormatSymbols.NARROW);
        count = narrowWeekdays.length;
        if( count != narrowWeekdays1.length) {
            errln("ERROR: setWeekdays(FORMAT,NARROW) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! narrowWeekdays[i].equals(narrowWeekdays1[i])) {
                    errln("ERROR: setWeekdays(FORMAT,NARROW) failed (different string values)");
                }
            }
        }

        final String[] standaloneWeekdays = en.getWeekdays(DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        fr.setWeekdays(standaloneWeekdays,DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        final String[] standaloneWeekdays1 = fr.getWeekdays(DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        count = standaloneWeekdays.length;
        if( count != standaloneWeekdays1.length) {
            errln("ERROR: setWeekdays(STANDALONE,WIDE) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! standaloneWeekdays[i].equals(standaloneWeekdays1[i])) {
                    errln("ERROR: setWeekdays(STANDALONE,WIDE) failed (different string values)");
                }
            }
        }

        final String[] standaloneShortWeekdays = en.getWeekdays(DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        fr.setWeekdays(standaloneShortWeekdays,DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        final String[] standaloneShortWeekdays1 = fr.getWeekdays(DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        count = standaloneShortWeekdays.length;
        if( count != standaloneShortWeekdays1.length) {
            errln("ERROR: setWeekdays(STANDALONE,ABBREVIATED) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! standaloneShortWeekdays[i].equals(standaloneShortWeekdays1[i])) {
                    errln("ERROR: setWeekdays(STANDALONE,ABBREVIATED) failed (different string values)");
                }
            }
        }

        final String[] standaloneNarrowWeekdays = en.getWeekdays(DateFormatSymbols.STANDALONE,DateFormatSymbols.NARROW);
        fr.setWeekdays(standaloneNarrowWeekdays,DateFormatSymbols.STANDALONE,DateFormatSymbols.NARROW);
        final String[] standaloneNarrowWeekdays1 = fr.getWeekdays(DateFormatSymbols.STANDALONE,DateFormatSymbols.NARROW);
        count = standaloneNarrowWeekdays.length;
        if( count != standaloneNarrowWeekdays1.length) {
            errln("ERROR: setWeekdays(STANDALONE,NARROW) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! standaloneNarrowWeekdays[i].equals(standaloneNarrowWeekdays1[i])) {
                    errln("ERROR: setWeekdays(STANDALONE,NARROW) failed (different string values)");
                }
            }
        }

        final String[] wideQuarters = en.getQuarters(DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        fr2.setQuarters(wideQuarters,DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        final String[] wideQuarters1 = fr2.getQuarters(DateFormatSymbols.FORMAT,DateFormatSymbols.WIDE);
        count = wideQuarters.length;
        if( count != wideQuarters1.length) {
            errln("ERROR: setQuarters(FORMAT, WIDE) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! wideQuarters[i].equals(wideQuarters1[i])) {
                    errln("ERROR: setQuarters(FORMAT, WIDE) failed (different string values)");
                }
            }
        }

        final String[] abbrQuarters = en.getQuarters(DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        fr2.setQuarters(abbrQuarters,DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        final String[] abbrQuarters1 = fr2.getQuarters(DateFormatSymbols.FORMAT,DateFormatSymbols.ABBREVIATED);
        count = abbrQuarters.length;
        if( count != abbrQuarters1.length) {
            errln("ERROR: setQuarters(FORMAT, ABBREVIATED) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! abbrQuarters[i].equals(abbrQuarters1[i])) {
                    errln("ERROR: setQuarters(FORMAT, ABBREVIATED) failed (different string values)");
                }
            }
        }

        final String[] standaloneQuarters = en.getQuarters(DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        fr.setQuarters(standaloneQuarters,DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        final String[] standaloneQuarters1 = fr.getQuarters(DateFormatSymbols.STANDALONE,DateFormatSymbols.WIDE);
        count = standaloneQuarters.length;
        if( count != standaloneQuarters1.length) {
            errln("ERROR: setQuarters(STANDALONE, WIDE) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! standaloneQuarters[i].equals(standaloneQuarters1[i])) {
                    errln("ERROR: setQuarters(STANDALONE, WIDE) failed (different string values)");
                }
            }
        }

        final String[] standaloneShortQuarters = en.getQuarters(DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        fr.setQuarters(standaloneShortQuarters,DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        final String[] standaloneShortQuarters1 = fr.getQuarters(DateFormatSymbols.STANDALONE,DateFormatSymbols.ABBREVIATED);
        count = standaloneShortQuarters.length;
        if( count != standaloneShortQuarters1.length) {
            errln("ERROR: setQuarters(STANDALONE, ABBREVIATED) failed (different size array)");
        }
        else {
            for(int i = 0; i < count; i++) {
                if(! standaloneShortQuarters[i].equals(standaloneShortQuarters1[i])) {
                    errln("ERROR: setQuarters(STANDALONE, ABBREVIATED) failed (different string values)");
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
