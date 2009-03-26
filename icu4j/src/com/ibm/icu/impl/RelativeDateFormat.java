/*
 *******************************************************************************
 * Copyright (C) 2007-2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.Date;
import java.util.MissingResourceException;
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
        initializeCombinedFormat(calendar, fLocale);
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

        //TODO: handle FieldPosition properly

        String dayString = null;
        String timeString = null;
        if (fDateStyle != DateFormat.NONE) {
            // calculate the difference, in days, between 'cal' and now.
            int dayDiff = dayDifference(cal);

            // look up string
            dayString = getStringForDay(dayDiff);

            if (dayString == null) {
                // didn't find it. Fall through to the fDateFormat 
                dayString = fDateFormat.format(cal);
            }
        }
        if (fTimeStyle != DateFormat.NONE) {
            timeString = fTimeFormat.format(cal);
        }

        if (dayString != null && timeString != null) {
            return fCombinedFormat.format(new Object[] {dayString, timeString}, toAppendTo,
                    new FieldPosition(0));
        } else if (dayString != null) {
            toAppendTo.append(dayString);
        } else if (timeString != null) {
            toAppendTo.append(timeString);
        }
        return toAppendTo;
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
        int dayDiff = until.get(Calendar.JULIAN_DAY) - nowCal.get(Calendar.JULIAN_DAY);
        return dayDiff;
    }
    
    /**
     * initializes fCalendar from parameters.  Returns fCalendar as a convenience.
     * @param zone  Zone to be adopted, or NULL for TimeZone::createDefault().
     * @param locale Locale of the calendar
     * @param status Error code
     * @return the newly constructed fCalendar
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

    private MessageFormat initializeCombinedFormat(Calendar cal, ULocale locale) {
        String pattern = "{1} {0}";
        try {
            CalendarData calData = new CalendarData(locale, cal.getType());
            String[] patterns = calData.getDateTimePatterns();
            if (patterns != null && patterns.length >= 9) {
                int glueIndex = 8;
                if (patterns.length >= 13)
                {
                    switch (fDateStyle)
                    {
                        case DateFormat.RELATIVE_FULL:
                        case DateFormat.FULL:
                            glueIndex += (DateFormat.FULL + 1);
                            break;
                        case DateFormat.RELATIVE_LONG:
                        case DateFormat.LONG:
                            glueIndex += (DateFormat.LONG +1);
                            break;
                        case DateFormat.RELATIVE_MEDIUM:
                        case DateFormat.MEDIUM:
                            glueIndex += (DateFormat.MEDIUM +1);
                            break;
                        case DateFormat.RELATIVE_SHORT:
                        case DateFormat.SHORT:
                            glueIndex += (DateFormat.SHORT + 1);
                            break;
                        default:
                            break;
                    }
                }
                pattern = patterns[glueIndex];
            }
        } catch (MissingResourceException e) {
            // use default
        }
        fCombinedFormat = new MessageFormat(pattern, locale);
        return fCombinedFormat;
    }
}
