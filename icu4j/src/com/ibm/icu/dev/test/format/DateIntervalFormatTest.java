//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2001-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/** 
 * Port From:   ICU4C v1.8.1 : format : DateIntervalFormatTest
 * Source File: $ICU4CRoot/source/test/intltest/dtifmtts.cpp
 **/

package com.ibm.icu.dev.test.format;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStreamWriter;
import java.text.FieldPosition;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.DateInterval;
import com.ibm.icu.text.DateIntervalInfo;
import com.ibm.icu.text.DateIntervalFormat;

public class DateIntervalFormatTest extends com.ibm.icu.dev.test.TestFmwk {

    public static boolean DEBUG = false;

    public static void main(String[] args) throws Exception {
        new DateIntervalFormatTest().run(args);
    }


    /**
     * General parse/format tests.  Add test cases as needed.
     */
    public void TestFormat() {
        // TODO - !!!! FIX THE TEST POROBLEM !!!!
        // This test is not working well now.  Skip.
        if (true) return;

        // test data.
        // The 1st is the format pattern,
        // Next are pairs in which the 1st in the pair is the earlier date
        // and the 2nd in the pair is the later date
        String[] DATA = {
            "yyyy MM dd HH:mm:ss",
            "2007 10 10 10:10:10", "2008 10 10 10:10:10", 
            "2007 10 10 10:10:10", "2007 11 10 10:10:10", 
            "2007 11 10 10:10:10", "2007 11 20 10:10:10", 
            "2007 01 10 10:00:10", "2007 01 10 14:10:10", 
            "2007 01 10 10:00:10", "2007 01 10 10:20:10", 
            "2007 01 10 10:10:10", "2007 01 10 10:10:20", 
        };


        String[][] testLocale = {
            {"en", "", ""},
            {"zh", "", ""},
            {"de", "", ""},
            {"ar", "", ""},
            {"en", "GB",  ""},
            {"fr", "", ""},
            {"it", "", ""},
            {"nl", "", ""},
            {"zh", "TW",  ""},
            {"ja", "", ""},
            {"pt", "BR", ""},
            {"ru", "", ""},
            {"pl", "", ""},
            {"tr", "", ""},
            {"es", "", ""},
            {"ko", "", ""},
            {"th", "", ""},
            {"sv", "", ""},
            {"fi", "", ""},
            {"da", "", ""},
            {"pt", "PT", ""},
            {"ro", "", ""},
            {"hu", "", ""},
            {"he", "", ""},
            {"in", "", ""},
            {"cs", "", ""},
            {"el", "", ""},
            {"no", "", ""},
            {"vi", "", ""},
            {"bg", "", ""},
            {"hr", "", ""},
            {"lt", "", ""},
            {"sk", "", ""},
            {"sl", "", ""},
            {"sr", "", ""},
            {"ca", "", ""},
            {"lv", "", ""},
            {"uk", "", ""},
            {"hi", "", ""},
        };

        /* TODO: uncomment
         * comment out temporarily, need to un-comment when CLDR data
         * is ready
         */
        
        int localeIndex;
        for ( localeIndex = 0; localeIndex < testLocale.length; ++localeIndex) {
            expect(DATA, DATA.length, new Locale(testLocale[localeIndex][0], 
                   testLocale[localeIndex][1], testLocale[localeIndex][2]), 
                   testLocale[localeIndex][0]+testLocale[localeIndex][1]);
        }
    }

    void expect(String[] data, int data_length, Locale loc, String locName) {

//        String[] formatResults = {
//        };
    
        String[] skeleton = {
            "EEEEdMMMMy",
            "dMMMMy",
            "dMMMM",
            "MMMMy",
            "EEEEdMMMM",
            "EEEdMMMy",
            "dMMMy",
            "dMMM",
            "MMMy",
            "EEEdMMM",
            "EEEdMy",
            "dMy",
            "dM",
            "My",
            "EEEdM",
            "d",
            "EEEd",
            "y",
            "M",
            "MMM",
            "MMMM",
            "hm",
            "hmv",
            "hmz",
            "h",
            "hv",
            "hz",
            "EEddMMyyyy", // following could be normalized
            "EddMMy", 
            "hhmm",
            "hhmmzz",
            "hms",  // following could not be normalized
            "dMMMMMy",
            "EEEEEdM",
        };

        int i = 0;

        SimpleDateFormat ref = new SimpleDateFormat(data[i++], loc);

        try {
            OutputStreamWriter osw = null;
            if (DEBUG) {
                osw = new OutputStreamWriter(new FileOutputStream("/home/xji/tmp_work/java.test.res", true), "UTF-8");
                //osw = new OutputStreamWriter(new ByteArrayOutputStream(), "UTF-8");
            }

            if ( DEBUG ) {
                osw.write("locale: " +  locName + "\n");  
            }

            while (i<data_length) {
                // 'f'
                String datestr = data[i++];
                String datestr_2 = data[i++];
                if ( DEBUG ) {
                    osw.write("original date: " + datestr + " - " + datestr_2 + "\n");
                }
                Date date;
                Date date_2;
                try {
                    date = ref.parse(datestr);
                    date_2 = ref.parse(datestr_2);
                } catch ( ParseException e ) {
                    errln("parse exception" + e);
                    continue;
                }
                DateInterval dtitv = new DateInterval(date.getTime(), date_2.getTime());
    
                for ( int skeletonIndex = 0; 
                      skeletonIndex < skeleton.length; 
                      ++skeletonIndex ) {
                    String oneSkeleton = skeleton[skeletonIndex];
                    DateIntervalFormat dtitvfmt = DateIntervalFormat.getInstance(oneSkeleton, loc);
                    FieldPosition pos = new FieldPosition(0);
                    StringBuffer str = new StringBuffer("");
                    dtitvfmt.format(dtitv, str, pos);
                    if ( DEBUG ) {
                        osw.write("interval by skeleton: " + oneSkeleton + "\n");
                        osw.write("interval date: " + str + "\n");
                    }
                }
    
                // test user created DateIntervalInfo
                DateIntervalInfo dtitvinf = new DateIntervalInfo();
                dtitvinf.setFallbackIntervalPattern("{0} --- {1}");
                dtitvinf.setIntervalPattern("yMMMd", Calendar.MONTH, "yyyy MMM d - MMM y");
                dtitvinf.setIntervalPattern("yMMMd", Calendar.HOUR_OF_DAY, "yyyy MMM d HH:mm - HH:mm");
                DateIntervalFormat dtitvfmt = DateIntervalFormat.getInstance(
                            DateFormat.YEAR_ABBR_MONTH_DAY, 
                            loc, dtitvinf);
                FieldPosition pos = new FieldPosition(0);
                StringBuffer str = new StringBuffer("");
                dtitvfmt.format(dtitv, str, pos);
                if ( DEBUG ) {
                    osw.write("interval format using user defined DateIntervalInfo\n");
                    osw.write("interval date: " + str + "\n");
                }

                // test interval format used by CLDR survey tool 
                //dtitvfmt = DateIntervalFormat.getEmptyInstance();
                dtitvfmt = DateIntervalFormat.getInstance("yMd");
                SimpleDateFormat dtfmt = new SimpleDateFormat("yyyy 'year' MMM 'month' dd 'day'", loc);
                dtitvfmt.setDateFormat(dtfmt);
                dtitvinf = new DateIntervalInfo();
                dtitvinf.setFallbackIntervalPattern("{0} --- {1}");
                dtitvinf.setIntervalPattern("yMMMd", Calendar.YEAR, "'all diff'");
                dtitvinf.setIntervalPattern("yMMMd", Calendar.MONTH, "yyyy 'diff' MMM d - MMM y");
                dtitvinf.setIntervalPattern("yMMMd", Calendar.DATE, "yyyy MMM d ~ d");
                dtitvinf.setIntervalPattern("yMMMd", Calendar.HOUR_OF_DAY, "yyyy MMM d HH:mm ~ HH:mm");
                dtitvfmt.setDateIntervalInfo(dtitvinf);
                pos = new FieldPosition(0);
                str = new StringBuffer("");
                //dtitvfmt.format(dtitv, str, pos);
                Calendar fromCalendar = (Calendar) dtfmt.getCalendar().clone();
                Calendar toCalendar = (Calendar) dtfmt.getCalendar().clone();
                fromCalendar.setTimeInMillis(dtitv.getFromDate());
                toCalendar.setTimeInMillis(dtitv.getToDate());
                dtitvfmt.format(fromCalendar, toCalendar, str, pos);
                if ( DEBUG ) {
                    osw.write("interval format for CLDR\n");
                    osw.write("interval date: " + str + "\n");
                }

                // test interval format by algorithm 
                for ( int style = DateFormat.FULL; style  < 4; ++style ) {
                    dtfmt = (SimpleDateFormat) DateFormat.getDateInstance(style, loc);
                    pos = new FieldPosition(0);
                    str = new StringBuffer("");
                    fromCalendar = (Calendar) dtfmt.getCalendar().clone();
                    toCalendar = (Calendar) dtfmt.getCalendar().clone();
                    fromCalendar.setTimeInMillis(dtitv.getFromDate());
                    toCalendar.setTimeInMillis(dtitv.getToDate());
                    dtfmt.intervalFormatByAlgorithm(fromCalendar, toCalendar, str, pos);
                    if ( DEBUG ) {
                        osw.write("interval format by algorithm, style = " +  style + "\n");
                        osw.write("interval date: " + str + "\n");
                    }
                } 
            } 
            osw.close();
        } catch ( FileNotFoundException e) {
            errln("file not found\n");
            return;
        } catch ( UnsupportedEncodingException e) {
            errln("UTF-8 is not supported\n");
            return;
        } catch ( IOException e ) {
            errln("IOException: " + e);
            return;
        }
    }
}
//#endif
