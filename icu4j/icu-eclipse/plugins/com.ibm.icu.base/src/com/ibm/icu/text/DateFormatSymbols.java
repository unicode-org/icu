/*
 *******************************************************************************
 * Copyright (C) 1996-2008, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.Serializable;
import java.util.Locale;

import com.ibm.icu.util.ULocale;

/**
 * <code>DateFormatSymbols</code> is a public class for encapsulating
 * localizable date-time formatting data, such as the names of the
 * months, the names of the days of the week, and the time zone data.
 * <code>DateFormat</code> and <code>SimpleDateFormat</code> both use
 * <code>DateFormatSymbols</code> to encapsulate this information.
 *
 * <p>
 * Typically you shouldn't use <code>DateFormatSymbols</code> directly.
 * Rather, you are encouraged to create a date-time formatter with the
 * <code>DateFormat</code> class's factory methods: <code>getTimeInstance</code>,
 * <code>getDateInstance</code>, or <code>getDateTimeInstance</code>.
 * These methods automatically create a <code>DateFormatSymbols</code> for
 * the formatter so that you don't have to. After the
 * formatter is created, you may modify its format pattern using the
 * <code>setPattern</code> method. For more information about
 * creating formatters using <code>DateFormat</code>'s factory methods,
 * see {@link DateFormat}.
 *
 * <p>
 * If you decide to create a date-time formatter with a specific
 * format pattern for a specific locale, you can do so with:
 * <blockquote>
 * <pre>
 * new SimpleDateFormat(aPattern, new DateFormatSymbols(aLocale)).
 * </pre>
 * </blockquote>
 *
 * <p>
 * <code>DateFormatSymbols</code> objects are clonable. When you obtain
 * a <code>DateFormatSymbols</code> object, feel free to modify the
 * date-time formatting data. For instance, you can replace the localized
 * date-time format pattern characters with the ones that you feel easy
 * to remember. Or you can change the representative cities
 * to your favorite ones.
 *
 * <p>
 * New <code>DateFormatSymbols</code> subclasses may be added to support
 * <code>SimpleDateFormat</code> for date-time formatting for additional locales.

 * @see          DateFormat
 * @see          SimpleDateFormat
 * @see          com.ibm.icu.util.SimpleTimeZone
 * @author       Chen-Lieh Huang
 * @stable ICU 2.0
 */
public class DateFormatSymbols implements Serializable, Cloneable {
    private static final long serialVersionUID = 1;
        
    /** @internal */
    public java.text.DateFormatSymbols dfs;
        
    /** @internal */
    public DateFormatSymbols(java.text.DateFormatSymbols delegate) {
        this.dfs = delegate;
    }
        
    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the default locale.
     *
     * @throws  java.util.MissingResourceException
     *          if the resources for the default locale cannot be
     *          found or cannot be loaded.
     * @stable ICU 2.0
     */
    public DateFormatSymbols() {
        this(new java.text.DateFormatSymbols());
    }

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the given locale.
     *
     * @throws  java.util.MissingResourceException
     *          if the resources for the specified locale cannot be
     *          found or cannot be loaded.
     * @stable ICU 2.0
     */
    public DateFormatSymbols(Locale locale) {
        this(new java.text.DateFormatSymbols(locale));
    }
        
    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the given ulocale.
     *
     * @throws  java.util.MissingResourceException
     *          if the resources for the specified locale cannot be
     *          found or cannot be loaded.
     * @stable ICU 3.2
     */
    public DateFormatSymbols(ULocale locale) {
        this(new java.text.DateFormatSymbols(locale.toLocale()));
    }

    /**
     * Gets era strings. For example: "AD" and "BC".
     * @return the era strings.
     * @stable ICU 2.0
     */
    public String[] getEras() {
        return dfs.getEras();
    }

    /**
     * Sets era strings. For example: "AD" and "BC".
     * @param newEras the new era strings.
     * @stable ICU 2.0
     */
    public void setEras(String[] newEras) {
        dfs.setEras(newEras);
    }

    /**
     * Gets month strings. For example: "January", "February", etc.
     * @return the month strings.
     * @stable ICU 2.0
     */
    public String[] getMonths() {
        return dfs.getMonths();
    }

    /**
     * Sets month strings. For example: "January", "February", etc.
     * @param newMonths the new month strings.
     * @stable ICU 2.0
     */
    public void setMonths(String[] newMonths) {
        dfs.setMonths(newMonths);
    }
            
    /**
     * Gets short month strings. For example: "Jan", "Feb", etc.
     * @return the short month strings.
     * @stable ICU 2.0
     */
    public String[] getShortMonths() {
        return dfs.getShortMonths();
    }

    /**
     * Sets short month strings. For example: "Jan", "Feb", etc.
     * @param newShortMonths the new short month strings.
     * @stable ICU 2.0
     */
    public void setShortMonths(String[] newShortMonths) {
        dfs.setShortMonths(newShortMonths);
    }

    /**
     * Gets weekday strings. For example: "Sunday", "Monday", etc.
     * @return the weekday strings. Use <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc. to index the result array.
     * @stable ICU 2.0
     */
    public String[] getWeekdays() {
        return dfs.getWeekdays();
    }

    /**
     * Sets weekday strings. For example: "Sunday", "Monday", etc.
     * @param newWeekdays the new weekday strings. The array should
     * be indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     * @stable ICU 2.0
     */
    public void setWeekdays(String[] newWeekdays) {
        dfs.setWeekdays(newWeekdays);
    }

    /**
     * Gets short weekday strings. For example: "Sun", "Mon", etc.
     * @return the short weekday strings. Use <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc. to index the result array.
     * @stable ICU 2.0
     */
    public String[] getShortWeekdays() {
        return dfs.getShortWeekdays();
    }

    /**
     * Sets short weekday strings. For example: "Sun", "Mon", etc.
     * @param newShortWeekdays the new short weekday strings. The array should
     * be indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     * @stable ICU 2.0
     */
    public void setShortWeekdays(String[] newShortWeekdays) {
        dfs.setShortWeekdays(newShortWeekdays);
    }

    /**
     * Gets ampm strings. For example: "AM" and "PM".
     * @return the weekday strings.
     * @stable ICU 2.0
     */
    public String[] getAmPmStrings() {
        return dfs.getAmPmStrings();
    }

    /**
     * Sets ampm strings. For example: "AM" and "PM".
     * @param newAmpms the new ampm strings.
     * @stable ICU 2.0
     */
    public void setAmPmStrings(String[] newAmpms) {
        dfs.setAmPmStrings(newAmpms);
    }

    /**
     * Gets timezone strings.
     * @return the timezone strings.
     * @stable ICU 2.0
     */
    public String[][] getZoneStrings() {
        return dfs.getZoneStrings();
    }

    /**
     * Sets timezone strings.
     * @param newZoneStrings the new timezone strings.
     * @stable ICU 2.0
     */
    public void setZoneStrings(String[][] newZoneStrings) {
        dfs.setZoneStrings(newZoneStrings);
    }

    /**
     * Gets localized date-time pattern characters. For example: 'u', 't', etc.
     * @return the localized date-time pattern characters.
     * @stable ICU 2.0
     */
    public String getLocalPatternChars() {
        return dfs.getLocalPatternChars();
    }

    /**
     * Sets localized date-time pattern characters. For example: 'u', 't', etc.
     * @param newLocalPatternChars the new localized date-time
     * pattern characters.
     * @stable ICU 2.0
     */
    public void setLocalPatternChars(String newLocalPatternChars) {
        dfs.setLocalPatternChars(newLocalPatternChars);
    }

    /**
     * Return a string suitable for debugging.
     * @return a string suitable for debugging
     * @stable ICU 3.4.2
     */
    public String toString() {
        return dfs.toString();
    }
        
    /**
     * Return a clone of this DateFormatSymbols.
     * @return a clone of this DateFormatSymbols
     * @stable ICU 3.4.2
     */
    public Object clone() {
        return new DateFormatSymbols((java.text.DateFormatSymbols)dfs.clone());
    }

    /**
     * Return true if rhs is a DateFormatSymbols and has the same symbols as this.
     * @return true if rhs equals this
     * @stable ICU 3.4.2
     */
    public boolean equals(Object rhs) {
        try {
            return dfs.equals(((DateFormatSymbols)rhs).dfs);
        }
        catch (Exception e) {
            return false;
        }
    }
        
    /**
     * Return a hashCode.
     * @return a hashCode
     * @stable ICU 3.4.2
     */
    public int hashCode() {
        return dfs.hashCode();
    }
}
