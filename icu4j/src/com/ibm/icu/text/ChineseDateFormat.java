/*
 * $RCSfile: ChineseDateFormat.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:49 $
 */

package com.ibm.text;

import com.ibm.util.ChineseCalendar;

import java.io.ObjectInputStream;
import java.io.IOException;

import java.lang.ClassNotFoundException;
import java.lang.StringIndexOutOfBoundsException;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;

import java.util.TimeZone;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SimpleTimeZone;
import java.util.GregorianCalendar;
import java.util.Hashtable;


public class ChineseDateFormat extends SimpleDateFormat {

    public ChineseDateFormat(ChineseCalendar cal, int dateStyle, int timeStyle, 
                             Locale loc)
    {
        super(patternHelper(dateStyle, timeStyle, loc),
                            new ChineseDateSymbols(loc));
        setCalendar(cal);
    }
    
    private static String patternHelper(int dateStyle, int timeStyle, Locale loc)
    {
        // PERF: bundle is loaded twice; symbols loaded it too
        ResourceBundle bundle = ResourceBundle.getBundle(
                                    "com.ibm.util.resources.ChineseCalendarSymbols");
        
        String[] patterns = bundle.getStringArray("DateTimePatterns");

        String pattern = null;
        if ((timeStyle >= 0) && (dateStyle >= 0)) {
            Object[] dateTimeArgs = { patterns[timeStyle],
                                        patterns[dateStyle + 4] };
            pattern = MessageFormat.format(patterns[8], dateTimeArgs);
        }
        else if (timeStyle >= 0) {
            pattern = patterns[timeStyle];
        }
        else if (dateStyle >= 0) {
            pattern = patterns[dateStyle + 4];
        }
        else {
            throw new IllegalArgumentException("No date or time style specified");
        }
        return pattern;
    }
    
    // Private member function that does the real date/time formatting.
    protected String subFormat(char ch, int count, int beginOffset,
                             FieldPosition pos, DateFormatSymbols formatData)
         throws IllegalArgumentException
    {
        ChineseDateSymbols symbols = (ChineseDateSymbols)formatData;
        
        int     patternCharIndex = -1;
        int     maxIntCount = Integer.MAX_VALUE;
        String result = null;

        if ((patternCharIndex = patternChars.indexOf(ch)) == -1) {
            return super.subFormat(ch, count, beginOffset, pos, formatData);
        }
        
        int field = PATTERN_INDEX_TO_CALENDAR_FIELD[patternCharIndex];
        int value = calendar.get(field);

        switch (patternCharIndex) {
        case '0': // ERA
            result = zeroPaddingNumber(value, count, maxIntCount);
            break;
        case '1': // LEAP_MONTH
            result = zeroPaddingNumber(value, count, maxIntCount);
            break;
        case '2': // SOLAR_TERM
            result = zeroPaddingNumber(value, count, maxIntCount);
            break;
        }
        return result;
    }


    /**
     * Cache to hold the DateTimePatterns of a Locale.
     */
    private static Hashtable cachedLocaleData = new Hashtable(3);

    static final String  patternChars = "GLT";

    // Map index into pattern character string to Calendar field number
    private static final int[] PATTERN_INDEX_TO_CALENDAR_FIELD =
    {
        ChineseCalendar.LEAP_MONTH, ChineseCalendar.SOLAR_TERM,
    };

};
