/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.util.UResourceBundleIterator;

/**
 * @author srl
 * @internal
 * @deprecated ICU Internal Use Only
 */
public class RelativeDateFormat extends DateFormat {

    /**
     * @author srl
     *
     */
    public class URelativeString {
        URelativeString(int offset, String string) {
            this.offset = offset;
            this.string = string;
        }
        URelativeString(String offset, String string) {
            this.offset = Integer.parseInt(offset);
            this.string = string;
        }
        public int    offset;
        public String string;
    }

    // copy c'tor?
    
    /**
     * @param timeStyle
     * @param dateStyle
     * @param locale
     */
    public RelativeDateFormat(int timeStyle, int dateStyle, ULocale locale) {
        fLocale = locale;
        fTimeStyle = timeStyle;
        fDateStyle = dateStyle;
        
        if(fDateStyle != DateFormat.NONE) {
            int newStyle = fDateStyle & ~DateFormat.RELATIVE;
            fDateFormat = DateFormat.getDateInstance(newStyle, locale);
        } else {
            fDateFormat = null;
        }
        if(fTimeStyle != DateFormat.NONE) {
            int newStyle = fTimeStyle & ~DateFormat.RELATIVE;
            fTimeFormat = DateFormat.getTimeInstance(newStyle, locale);
        } else {
            fTimeFormat = null;
        }
        
        initializeCalendar(null, fLocale);
        loadDates();
    }
    
    /**
     * serial version (generated)
     */
    private static final long serialVersionUID = 1131984966440549435L;

    /* (non-Javadoc)
     * @see com.ibm.icu.text.DateFormat#format(com.ibm.icu.util.Calendar, java.lang.StringBuffer, java.text.FieldPosition)
     */
    public StringBuffer format(Calendar cal, StringBuffer toAppendTo,
            FieldPosition fieldPosition) {
        // calculate the difference, in days, between 'cal' and now.
        int dayDiff = dayDifference(cal);

        // look up string
        String theString = getStringForDay(dayDiff);
        
        if(theString==null) {
            // didn't find it. Fall through to the fDateFormat 
            if(fDateFormat != null) {
                return fDateFormat.format(cal,toAppendTo,fieldPosition);
            } else {
                return toAppendTo; // no op
            }
        } else {
            // found a relative string
            return toAppendTo.append(theString);
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.DateFormat#parse(java.lang.String, com.ibm.icu.util.Calendar, java.text.ParsePosition)
     */
    public void parse(String text, Calendar cal, ParsePosition pos) {
        throw new UnsupportedOperationException("Relative Date parse is not implemented yet");
    }

    private DateFormat fDateFormat; // the held date format 
    private DateFormat fTimeFormat; // the held time format
    private MessageFormat fCombinedFormat; //  the {0} {1} format. 
    
    int fDateStyle;
    int fTimeStyle;
    ULocale  fLocale;
    
    private transient URelativeString fDates[] = null; // array of strings
    
    
    /**
     * Get the string at a specific offset.
     * @param day day offset ( -1, 0, 1, etc.. )
     * @return the string, or NULL if none at that location.
     */
    private String getStringForDay(int day) {
        if(fDates == null) {
            loadDates();
        }
        for(int i=0;i<fDates.length;i++) {
            if(fDates[i].offset == day) {
                return fDates[i].string;
            }
        }
        return null;
    }
    
    /** 
     * Load the Date string array
     */
    private synchronized void loadDates() {
        CalendarData calData = new CalendarData(fLocale, calendar.getType());
        UResourceBundle rb = calData.get("fields", "day", "relative");
        
        Set datesSet =new TreeSet(new Comparator() { 
            public int compare(Object o1, Object o2) {
                URelativeString r1 = (URelativeString)o1;
                URelativeString r2 = (URelativeString)o2;
                
                if(r1.offset == r2.offset) {
                    return 0;
                } else if(r1.offset < r2.offset) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }) ;
        
        for(UResourceBundleIterator i = rb.getIterator();i.hasNext();) {
            UResourceBundle line = i.next();
            
            String k = line.getKey();
            String v = line.getString();
            URelativeString rs = new URelativeString(k,v);
            datesSet.add(rs);
        }
        fDates = new URelativeString[0];
        fDates = (URelativeString[])datesSet.toArray(fDates);
    }
    
    /**
     * @return the number of days in "until-now"
     */
    private static int dayDifference(Calendar until) {
        Calendar nowCal = (Calendar)until.clone();
        Date nowDate = new Date(System.currentTimeMillis());
        nowCal.clear();
        nowCal.setTime(nowDate);
        int dayDiff = nowCal.fieldDifference(until.getTime(), Calendar.DATE);
        return dayDiff;
    }
    
    /**
     * initializes fCalendar from parameters.  Returns fCalendar as a convenience.
     * @param zone  Zone to be adopted, or NULL for TimeZone::createDefault().
     * @param locale Locale of the calendar
     * @param status Error code
     * @return the newly constructed fCalendar
     * @draft ICU 3.8
     */
    private Calendar initializeCalendar(TimeZone zone, ULocale locale) {
        if (calendar == null) {
            if(zone == null) {
                calendar = Calendar.getInstance(locale);
            } else {
                calendar = Calendar.getInstance(zone, locale);
            }
        }
        return calendar;
    }
}
