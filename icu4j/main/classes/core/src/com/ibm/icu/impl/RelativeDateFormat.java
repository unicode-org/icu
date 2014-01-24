/*
 *******************************************************************************
 * Copyright (C) 2007-2014, International Business Machines Corporation and    *
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

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DisplayContext;
import com.ibm.icu.text.MessageFormat;
import com.ibm.icu.text.SimpleDateFormat;
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
    public static class URelativeString {
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
     * @param timeStyle The time style for the date and time.
     * @param dateStyle The date style for the date and time.
     * @param locale The locale for the date.
     * @param cal The calendar to be used
     */
    public RelativeDateFormat(int timeStyle, int dateStyle, ULocale locale, Calendar cal) {
        calendar = cal;

        fLocale = locale;
        fTimeStyle = timeStyle;
        fDateStyle = dateStyle;

        if (fDateStyle != DateFormat.NONE) {
            int newStyle = fDateStyle & ~DateFormat.RELATIVE;
            DateFormat df = DateFormat.getDateInstance(newStyle, locale);
            if (df instanceof SimpleDateFormat) {
                fDateTimeFormat = (SimpleDateFormat)df;
            } else {
                throw new IllegalArgumentException("Can't create SimpleDateFormat for date style");
            }
            fDatePattern = fDateTimeFormat.toPattern();
            if (fTimeStyle != DateFormat.NONE) {
                newStyle = fTimeStyle & ~DateFormat.RELATIVE;
                df = DateFormat.getTimeInstance(newStyle, locale);
                if (df instanceof SimpleDateFormat) {
                    fTimePattern = ((SimpleDateFormat)df).toPattern();
                }
            }
        } else {
            // does not matter whether timeStyle is UDAT_NONE, we need something for fDateTimeFormat
            int newStyle = fTimeStyle & ~DateFormat.RELATIVE;
            DateFormat df = DateFormat.getTimeInstance(newStyle, locale);
            if (df instanceof SimpleDateFormat) {
                fDateTimeFormat = (SimpleDateFormat)df;
            } else {
                throw new IllegalArgumentException("Can't create SimpleDateFormat for time style");
            }
            fTimePattern = fDateTimeFormat.toPattern();
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

        String relativeDayString = null;
        DisplayContext capitalizationContext = getContext(DisplayContext.Type.CAPITALIZATION);

        if (fDateStyle != DateFormat.NONE) {
            // calculate the difference, in days, between 'cal' and now.
            int dayDiff = dayDifference(cal);

            // look up string
            relativeDayString = getStringForDay(dayDiff);
        }

        if ( relativeDayString != null && fDatePattern != null &&
                (fTimePattern == null || fCombinedFormat == null || combinedFormatHasDateAtStart) ) {
            // capitalize relativeDayString according to context for tense, set formatter no context
            if ( capitalizationContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE ||
                    (capitalizationContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU && capitalizationForRelativeUnitsListOrMenu) ||
                    (capitalizationContext == DisplayContext.CAPITALIZATION_FOR_STANDALONE && capitalizationForRelativeUnitsStandAlone) ) {
                // titlecase first word of relativeDayString, do like LocaleDisplayNamesImpl.adjustForUsageAndContext
                // Note that for languages that *have* case, using the word break properties here works reasonably
                // well (CLDR word break tailoring is for non-casing languages, and the POSIX locale).
                int stopPos, stopPosLimit = 8, len = relativeDayString.length();
                if ( stopPosLimit > len ) {
                    stopPosLimit = len;
                }
                for ( stopPos = 0; stopPos < stopPosLimit; stopPos++ ) {
                    int ch = relativeDayString.codePointAt(stopPos);
                    int wb = UCharacter.getIntPropertyValue(ch, UProperty.WORD_BREAK);
                    if ( !(UCharacter.isLowerCase(ch) || wb==UCharacter.WordBreak.EXTEND || wb==UCharacter.WordBreak.SINGLE_QUOTE ||
                            wb==UCharacter.WordBreak.MIDNUMLET || wb==UCharacter.WordBreak.MIDLETTER) ) {
                        break;
                    }
                    if (ch >= 0x10000) {
                        stopPos++;
                    }
                }
                if ( stopPos > 0 && stopPos < len ) {
                    String firstWord = relativeDayString.substring(0, stopPos);
                    firstWord = UCharacter.toTitleCase(fLocale, firstWord, null,
                            UCharacter.TITLECASE_NO_LOWERCASE | UCharacter.TITLECASE_NO_BREAK_ADJUSTMENT);
                    relativeDayString = firstWord.concat(relativeDayString.substring(stopPos));
                } else {
                    // no stopPos, titlecase the whole text
                    relativeDayString = UCharacter.toTitleCase(fLocale, relativeDayString, null,
                            UCharacter.TITLECASE_NO_LOWERCASE | UCharacter.TITLECASE_NO_BREAK_ADJUSTMENT);
                }
            }
            fDateTimeFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
        } else {
            // set our context for the formatter
            fDateTimeFormat.setContext(capitalizationContext);
        }

        if (fDateTimeFormat != null && (fDatePattern != null || fTimePattern != null)) {
            // The new way
            if (fDatePattern == null) {
                // must have fTimePattern
                fDateTimeFormat.applyPattern(fTimePattern);
                fDateTimeFormat.format(cal, toAppendTo, fieldPosition);
            } else if (fTimePattern == null) {
                // must have fDatePattern
                if (relativeDayString != null) {
                    toAppendTo.append(relativeDayString);
                } else {
                    fDateTimeFormat.applyPattern(fDatePattern);
                    fDateTimeFormat.format(cal, toAppendTo, fieldPosition);
                }
            } else {
                String datePattern = fDatePattern; // default;
                if (relativeDayString != null) {
                    // Need to quote the relativeDayString to make it a legal date pattern
                    datePattern = "'" + relativeDayString.replace("'", "''") + "'";
                }
                StringBuffer combinedPattern = new StringBuffer("");
                fCombinedFormat.format(new Object[] {fTimePattern, datePattern}, combinedPattern, new FieldPosition(0));
                fDateTimeFormat.applyPattern(combinedPattern.toString());
                fDateTimeFormat.format(cal, toAppendTo, fieldPosition);
            }
        } else if (fDateFormat != null) {
            // A subset of the old way, for serialization compatibility
            // (just do the date part)
            if (relativeDayString != null) {
                toAppendTo.append(relativeDayString);
            } else {
                fDateFormat.format(cal, toAppendTo, fieldPosition);
            }
        }

        return toAppendTo;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.text.DateFormat#parse(java.lang.String, com.ibm.icu.util.Calendar, java.text.ParsePosition)
     */
    public void parse(String text, Calendar cal, ParsePosition pos) {
        throw new UnsupportedOperationException("Relative Date parse is not implemented yet");
    }

    private DateFormat fDateFormat; // keep for serialization compatibility
    @SuppressWarnings("unused")
    private DateFormat fTimeFormat; // now unused, keep for serialization compatibility
    private MessageFormat fCombinedFormat; //  the {0} {1} format. 
    private SimpleDateFormat fDateTimeFormat = null; // the held date/time formatter
    private String fDatePattern = null;
    private String fTimePattern = null;

    int fDateStyle;
    int fTimeStyle;
    ULocale  fLocale;
    
    private transient URelativeString fDates[] = null; // array of strings
    
    private transient boolean capitalizationForRelativeUnitsListOrMenu = false;
    private transient boolean capitalizationForRelativeUnitsStandAlone = false;
    private transient boolean combinedFormatHasDateAtStart = false;
   
    
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
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, fLocale);
        ICUResourceBundle rdb = rb.getWithFallback("fields/day/relative");
        
        Set<URelativeString> datesSet = new TreeSet<URelativeString>(new Comparator<URelativeString>() { 
            public int compare(URelativeString r1, URelativeString r2) {
                
                if(r1.offset == r2.offset) {
                    return 0;
                } else if(r1.offset < r2.offset) {
                    return -1;
                } else {
                    return 1;
                }
            }
        }) ;
        
        for(UResourceBundleIterator i = rdb.getIterator();i.hasNext();) {
            UResourceBundle line = i.next();
            
            String k = line.getKey();
            String v = line.getString();
            URelativeString rs = new URelativeString(k,v);
            datesSet.add(rs);
        }
        fDates = datesSet.toArray(new URelativeString[0]);

        try {
            rdb = rb.getWithFallback("contextTransforms/tense");
            int[] intVector = rdb.getIntVector();
            if (intVector.length >= 2) {
                capitalizationForRelativeUnitsListOrMenu = (intVector[0] != 0);
                capitalizationForRelativeUnitsStandAlone = (intVector[1] != 0);
            }
        } catch (MissingResourceException e) {
            // use default
        }
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
        combinedFormatHasDateAtStart = pattern.startsWith("{1}");
        fCombinedFormat = new MessageFormat(pattern, locale);
        return fCombinedFormat;
    }
}
