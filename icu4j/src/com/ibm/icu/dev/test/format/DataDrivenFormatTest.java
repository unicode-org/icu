/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Iterator;

import com.ibm.icu.dev.test.ModuleTest;
import com.ibm.icu.dev.test.TestDataModule;
import com.ibm.icu.dev.test.TestDataModule.DataMap;
import com.ibm.icu.dev.test.util.CalendarFieldsSet;
import com.ibm.icu.dev.test.util.DateTimeStyleSet;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.ULocale;

/**
 * @author srl
 *
 */
public class DataDrivenFormatTest extends ModuleTest {

    /**
     * @param baseName
     * @param locName
     */
    public DataDrivenFormatTest() {
        super("com/ibm/icu/dev/data/testdata/", "format");
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.test.ModuleTest#processModules()
     */
    public void processModules() {
        //String testName = t.getName().toString();

        for (Iterator siter = t.getSettingsIterator(); siter.hasNext();) {
            // Iterate through and get each of the test case to process
            DataMap settings = (DataMap) siter.next();
            
            String type = settings.getString("Type");

            if(type.equals("date_format")) {
                testConvertDate(t, settings, true);
            } else if(type.equals("date_parse")) {
                testConvertDate(t, settings, false);
            } else {
                errln("Unknown type: " + type);
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
         new DataDrivenFormatTest().run(args);
    }
   
    private static final String kPATTERN = "PATTERN=";
    private static final String kMILLIS = "MILLIS=";
    private static final String kRELATIVE_MILLIS = "RELATIVE_MILLIS=";
    
    private void testConvertDate(TestDataModule.TestData testData, DataMap  settings, boolean fmt) {
        DateFormat basicFmt = new SimpleDateFormat("EEE MMM dd yyyy / YYYY'-W'ww-ee");

        int n = 0;
        for (Iterator iter = testData.getDataIterator(); iter.hasNext();) {
            ++n;
            long now = System.currentTimeMillis();
            DataMap currentCase = (DataMap) iter.next();
            String caseString = "["+testData.getName()+"#"+n+(fmt?"format":"parse")+"]";
            
            String locale = currentCase.getString("locale");
            String spec = currentCase.getString("spec");
            String date = currentCase.getString("date");
            String str = currentCase.getString("str");
            
            Date fromDate = null;
            boolean useDate = false;
            
            ULocale loc = new ULocale(locale);
            String pattern = null;
//            boolean usePattern = false;
            DateFormat format = null;
            DateTimeStyleSet styleSet;
            CalendarFieldsSet fromSet = null;
            
            // parse 'spec'  - either 'PATTERN=yy mm dd' or 'DATE=x,TIME=y'
            if(spec.startsWith(kPATTERN)) {
                pattern = spec.substring(kPATTERN.length());
//                usePattern = true;
                format = new SimpleDateFormat(pattern, loc);
            } else {
                styleSet = new DateTimeStyleSet();
                styleSet.parseFrom(spec);
                format = DateFormat.getDateTimeInstance(styleSet.getDateStyle(), styleSet.getTimeStyle(), loc);
            }
            
            // parse 'date' - either 'MILLIS=12345' or  a CalendarFieldsSet
            if(date.startsWith(kMILLIS)) {
                useDate = true;
                fromDate = new Date(Long.parseLong(date.substring(kMILLIS.length())));
            } else if(date.startsWith(kRELATIVE_MILLIS)) {
                useDate = true;
                fromDate = new Date(now+Long.parseLong(date.substring(kRELATIVE_MILLIS.length())));
            } else {
                fromSet = new CalendarFieldsSet();
                fromSet.parseFrom(date);
            }
            
            // run the test
            Calendar cal = Calendar.getInstance(loc);
            if(fmt) {
                StringBuffer output = new StringBuffer();
                cal.clear();
                FieldPosition pos = new FieldPosition(0);
                if(useDate) {
                    output = format.format(fromDate, output, pos);
                } else {
                    fromSet.setOnCalendar(cal);
                    format.format(cal, output, pos);
                }
                
                if(output.toString().equals(str)) {
                    logln(caseString + " Success - strings match: " + output);
                } else {
                    errln(caseString + " FAIL: got " + output + " expected " + str);
                }
            } else { // parse
                cal.clear();
                ParsePosition pos = new ParsePosition(0);
                format.parse(str, cal, pos);
                if(useDate) {
                    Date gotDate = cal.getTime(); 
                    if(gotDate.equals(fromDate)) {
                        logln(caseString + " SUCCESS: got=parse="+str);
                    } else {
                        errln(caseString + " FAIL: parsed " + str + " but got " + 
                                basicFmt.format(gotDate) + " - " + gotDate + "  expected " + 
                                basicFmt.format(fromDate));
                    }
                } else  {
                    CalendarFieldsSet diffSet = new CalendarFieldsSet();
                    if(!fromSet.matches(cal, diffSet)) {
                        String diffs = diffSet.diffFrom(fromSet);
                        errln(caseString + " FAIL:  differences: " + diffs);
                    } else {
                        logln(caseString + " SUCCESS: got=parse: " + str + " - " + fromSet.toString());
                    }
                }
            }
        }
    }
}
