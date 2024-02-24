// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.ibm.icu.impl.icuadapter.TimeZoneJDK;
import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.util.Calendar;

/**
 * CalendarICU is an adapter class which wraps ICU4J Calendar and
 * implements java.util.Calendar APIs.
 */
public class CalendarICU extends java.util.Calendar {

    private static final long serialVersionUID = -8641226371713600671L;

    private Calendar fIcuCal;

    private CalendarICU(Calendar icuCal) {
        fIcuCal = icuCal;
        init();
    }

    public static java.util.Calendar wrap(Calendar icuCal) {
        return new CalendarICU(icuCal);
    }

    public Calendar unwrap() {
        sync();
        return fIcuCal;
    }

    @Override
    public void add(int field, int amount) {
        sync();
        fIcuCal.add(field, amount);
    }

    // Note:    We do not need to override followings.  These methods
    //          call int compareTo(Calendar anotherCalendar) and we
    //          override the method.
    //public boolean after(Object when)
    //public boolean before(Object when)

    // Note:    Jeez!  These methods are final and we cannot override them.
    //          We do not want to rewrite ICU Calendar implementation classes
    //          as subclasses of java.util.Calendar.  This adapter class
    //          wraps an ICU Calendar instance and the calendar calculation
    //          is actually done independently from java.util.Calendar
    //          implementation.  Thus, we need to monitor the status of
    //          superclass fields in some methods and call ICU Calendar's
    //          clear if superclass clear update the status of superclass's
    //          calendar fields.  See private void sync().
    //public void clear()
    //public void clear(int field)

    @Override
    public Object clone() {
        sync();
        CalendarICU other = (CalendarICU)super.clone();
        other.fIcuCal = (Calendar)fIcuCal.clone();
        return other;
    }

    public int compareTo(Calendar anotherCalendar)  {
        sync();
        long thisMillis = getTimeInMillis();
        long otherMillis = anotherCalendar.getTimeInMillis();
        return thisMillis > otherMillis ? 1 : (thisMillis == otherMillis ? 0 : -1);
    }

    // Note:    These methods are supposed to be implemented by java.util.Calendar
    //          subclasses.  But we actually use a instance of ICU Calendar
    //          for all calendar calculation, we do nothing here.
    @Override
    protected void complete() {}
    @Override
    protected void computeFields() {}
    @Override
    protected void computeTime() {}

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CalendarICU) {
            sync();
            return ((CalendarICU)obj).fIcuCal.equals(fIcuCal);
        }
        return false;
    }

    @Override
    public int get(int field) {
        sync();
        return fIcuCal.get(field);
    }

    @Override
    public int getActualMaximum(int field) {
        return fIcuCal.getActualMaximum(field);
    }

    @Override
    public int getActualMinimum(int field) {
        return fIcuCal.getActualMinimum(field);
    }

    @Override
    public String getDisplayName(int field, int style, Locale locale) {
        if (field < 0 || field >= FIELD_COUNT || (style != SHORT && style != LONG && style != ALL_STYLES)) {
            throw new IllegalArgumentException("Bad field or style.");
        }
        DateFormatSymbols dfs = DateFormatSymbols.getInstance(locale);
        String[] array = getFieldStrings(field, style, dfs);
        if (array != null) {
            int fieldVal = get(field);
            if (fieldVal < array.length) {
                return array[fieldVal];
            }
        }
        return null;
    }

    @Override
    public Map<String,Integer> getDisplayNames(int field, int style, Locale locale) {
        if (field < 0 || field >= FIELD_COUNT || (style != SHORT && style != LONG && style != ALL_STYLES)) {
            throw new IllegalArgumentException("Bad field or style.");
        }
        DateFormatSymbols dfs = DateFormatSymbols.getInstance(locale);
        if (style != ALL_STYLES) {
            return getFieldStringsMap(field, style, dfs);
        }

        Map<String,Integer> result = getFieldStringsMap(field, SHORT, dfs);
        if (result == null) {
            return null;
        }
        if (field == MONTH || field == DAY_OF_WEEK) {
            Map<String,Integer> longMap = getFieldStringsMap(field, LONG, dfs);
            if (longMap != null) {
                result.putAll(longMap);
            }
        }
        return result;
    }

    @Override
    public int getFirstDayOfWeek() {
        return fIcuCal.getFirstDayOfWeek();
    }

    @Override
    public int getGreatestMinimum(int field) {
        return fIcuCal.getGreatestMinimum(field);
    }

    @Override
    public int getLeastMaximum(int field) {
        return fIcuCal.getLeastMaximum(field);
    }

    @Override
    public int getMaximum(int field) {
        return fIcuCal.getMaximum(field);
    }

    @Override
    public int getMinimalDaysInFirstWeek() {
        return fIcuCal.getMinimalDaysInFirstWeek();
    }

    @Override
    public int getMinimum(int field) {
        return fIcuCal.getMinimum(field);
    }

    // Note:    getTime() calls getTimeInMillis()
    //public Date getTime()

    @Override
    public long getTimeInMillis() {
        sync();
        return fIcuCal.getTimeInMillis();
    }

    @Override
    public TimeZone getTimeZone() {
        return TimeZoneICU.wrap(fIcuCal.getTimeZone());
    }

    @Override
    public int hashCode() {
        sync();
        return fIcuCal.hashCode();
    }

    //protected int internalGet(int field)

    @Override
    public boolean isLenient() {
        return fIcuCal.isLenient();
    }

    //public boolean isSet(int field)

    @Override
    public void roll(int field, boolean up) {
        sync();
        fIcuCal.roll(field, up);
    }

    @Override
    public void roll(int field, int amount) {
        sync();
        fIcuCal.roll(field, amount);
    }

    @Override
    public void set(int field, int value) {
        sync();
        fIcuCal.set(field, value);
    }

    // Note:    These set methods call set(int field, int value) for each field.
    //          These are final, so we cannot override them, but we override
    //          set(int field, int value), so the superclass implementations
    //          still work as we want.
    //public void set(int year, int month, int date)
    //public void set(int year, int month, int date, int hourOfDay, int minute)
    //public void set(int year, int month, int date, int hourOfDay, int minute, int second)

    @Override
    public void setFirstDayOfWeek(int value) {
        fIcuCal.setFirstDayOfWeek(value);
    }

    @Override
    public void setLenient(boolean lenient) {
        fIcuCal.setLenient(lenient);
    }

    @Override
    public void setMinimalDaysInFirstWeek(int value) {
        fIcuCal.setMinimalDaysInFirstWeek(value);
    }

    // Note:    This method calls setTimeInMillis(long millis).
    //          This method is final, so we cannot override it, but we
    //          override setTimeInMillis(long millis), so the superclass
    //          implementation still works as we want.
    //public void setTime(Date date)

    @Override
    public void setTimeInMillis(long millis) {
        fIcuCal.setTimeInMillis(millis);
    }

    @Override
    public void setTimeZone(TimeZone value) {
        fIcuCal.setTimeZone(TimeZoneJDK.wrap(value));
    }

    @Override
    public String toString() {
        sync();
        return "CalendarICU: " + fIcuCal.toString();
    }

    private void sync() {
        // Check if clear is called for each JDK Calendar field.
        // If it was, then call clear for the field in the wrapped
        // ICU Calendar.
        for (int i = 0; i < isSet.length; i++) {
            if (!isSet[i]) {
                isSet[i] = true;
                try {
                    fIcuCal.clear(i);
                } catch (ArrayIndexOutOfBoundsException e) {
                    // More fields in JDK calendar, which is unlikely
                }
            }
        }
    }

    private void init() {
        // Mark "set" for all fields, so we can detect the invocation of
        // clear() later.
        for (int i = 0; i < isSet.length; i++) {
            isSet[i] = true;
        }
    }

    private static String[] getFieldStrings(int field, int style, DateFormatSymbols dfs) {
        String[] result = null;
        switch (field) {
        case AM_PM:
            result = dfs.getAmPmStrings();
            break;
        case DAY_OF_WEEK:
            result = (style == LONG) ? dfs.getWeekdays() : dfs.getShortWeekdays();
            break;
        case ERA:
            //result = (style == LONG) ? dfs.getEraNames() : dfs.getEras();
            result = dfs.getEras();
            break;
        case MONTH:
            result = (style == LONG) ? dfs.getMonths() : dfs.getShortMonths();
            break;
        }
        return result;
    }

    private static Map<String,Integer> getFieldStringsMap(int field, int style, DateFormatSymbols dfs) {
        String[] strings = getFieldStrings(field, style, dfs);
        if (strings == null) {
            return null;
        }
        Map<String,Integer> res = new HashMap<String,Integer>();
        for (int i = 0; i < strings.length; i++) {
            if (strings[i].length() != 0) {
                res.put(strings[i], i);
            }
        }
        return res;
    }
}
