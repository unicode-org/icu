/*
 *******************************************************************************
 * Copyright (C) 2001, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/test/format/Attic/DateFormatMiscTests.java,v $ 
 * $Date: 2001/10/19 11:26:32 $ 
 * $Revision: 1.2 $
 *
 *****************************************************************************************
 */

/** 
 * Port From:   ICU4C v1.8.1 : format : DateFormatMiscTests
 * Source File: $ICU4CRoot/source/test/intltest/miscdtfm.cpp
 **/

package com.ibm.icu.test.format;

import com.ibm.text.*;
import com.ibm.util.*;
import java.text.FieldPosition;
import java.text.ParseException;
import java.util.Locale;
import java.util.Date;

/** 
 * Performs miscellaneous tests for DateFormat, SimpleDateFormat, DateFormatSymbols
 **/
public class DateFormatMiscTests extends com.ibm.test.TestFmwk {

    public static void main(String[] args) throws Exception{
        new DateFormatMiscTests().run(args);
    }
    
    /*
     * @bug 4097450
     */
    public void Test4097450() {
        //
        // Date parse requiring 4 digit year.
        //
        String dstring[] = {
            "97", "1997", "97", "1997", "01", "2001", "01", "2001",
             "1", "1", "11", "11", "111", "111"}; 
    
        String dformat[] = 
            {
                "yy", "yy", "yyyy", "yyyy", "yy", "yy", "yyyy", "yyyy", 
                "yy", "yyyy", "yy", "yyyy", "yy", "yyyy"};         
    
        SimpleDateFormat formatter;
        SimpleDateFormat resultFormatter = new SimpleDateFormat("yyyy");
        logln("Format\tSource\tResult");
        logln("-------\t-------\t-------");
        for (int i = 0; i < dstring.length ; i++) {
            log(dformat[i] + "\t" + dstring[i] + "\t");
            formatter = new SimpleDateFormat(dformat[i]);
            try {
                StringBuffer str = new StringBuffer("");
                FieldPosition pos = new FieldPosition(0);
                logln(resultFormatter.format(formatter.parse(dstring[i]), str, pos).toString()); 
            }
            catch (ParseException exception) {
                errln("exception --> " + exception);
            }
            logln("");
        }
    }
    
    /*
     * @bug 4099975
     */
    public void Test4099975() {
        DateFormatSymbols symbols = new DateFormatSymbols();
        SimpleDateFormat df = new SimpleDateFormat("E hh:mm", symbols);
        logln(df.toLocalizedPattern());
        symbols.setLocalPatternChars("abcdefghijklmonpqr"); // change value of field
        logln(df.toLocalizedPattern());
        Date d = new Date();
        logln(df.format(d));
    }
    
    /*
     * @bug 4117335
     */
    public void Test4117335() {
        //char bcC[] = {0x7D00, 0x5143, 0x524D};
        String bc = "\u7D00\u5143\u524D";
        String ad = "\u897f\u66a6";
        //char adC[] = {0x897F, 0x66A6};
        //UnicodeString ad(adC, 2, 2);
        String jstLong = "\u65e5\u672c\u6a19\u6e96\u6642";
        char jstLongC[] = {0x65e5, 0x672c, 0x6a19, 0x6e96, 0x6642};
        //UnicodeString jstLong(jstLongC, 5, 5);
        String jstShort = "JST";
    
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.JAPAN);
        final String[] eras = symbols.getEras();
        int eraCount = eras.length;
        logln("BC = " + eras[0]);
        if (!eras[0].equals(bc)) {
            errln("*** Should have been " + bc);
        }
    
        logln("AD = " + eras[1]);
        if (!eras[1].equals(ad)) {
            errln("*** Should have been " + ad);
        }
    
        final String zones[][] = symbols.getZoneStrings();
        int rowCount = zones.length, colCount = zones[0].length;
        logln("Long zone name = " + zones[0][1]);
        if (!zones[0][1].equals(jstLong)) {
            errln("*** Should have been " + jstLong);
        }
        logln("Short zone name = " + zones[0][2]);
        if (!zones[0][2].equals(jstShort)) {
            errln("*** Should have been " + jstShort);
        }
        logln("Long zone name = " + zones[0][3]);
        if (zones[0][3] != jstLong) {
            errln("*** Should have been " + jstLong);
        }
        logln("SHORT zone name = " + zones[0][4]);
        if (zones[0][4] != jstShort) {
            errln("*** Should have been " + jstShort);
        }
    }
}
