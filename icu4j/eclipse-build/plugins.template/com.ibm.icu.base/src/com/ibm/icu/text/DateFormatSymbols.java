// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import java.io.Serializable;
import java.util.Locale;

import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.ULocale.Category;

/**
 * {@icuenhanced java.text.DateFormatSymbols}.{@icu _usage_}
 *
 * <p><code>DateFormatSymbols</code> is a public class for encapsulating
 * localizable date-time formatting data, such as the names of the
 * months, the names of the days of the week, and the time zone data.
 * <code>DateFormat</code> and <code>SimpleDateFormat</code> both use
 * <code>DateFormatSymbols</code> to encapsulate this information.
 *
 * <p>Typically you shouldn't use <code>DateFormatSymbols</code> directly.
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
 * <p>If you decide to create a date-time formatter with a specific
 * format pattern for a specific locale, you can do so with:
 * <blockquote>
 * <pre>
 * new SimpleDateFormat(aPattern, new DateFormatSymbols(aLocale)).
 * </pre>
 * </blockquote>
 *
 * <p><code>DateFormatSymbols</code> objects are clonable. When you obtain
 * a <code>DateFormatSymbols</code> object, feel free to modify the
 * date-time formatting data. For instance, you can replace the localized
 * date-time format pattern characters with the ones that you feel easy
 * to remember. Or you can change the representative cities
 * to your favorite ones.
 *
 * <p>New <code>DateFormatSymbols</code> subclasses may be added to support
 * <code>SimpleDateFormat</code> for date-time formatting for additional locales.
 *
 * @see          DateFormat
 * @see          SimpleDateFormat
 * @see          com.ibm.icu.util.SimpleTimeZone
 * @author       Chen-Lieh Huang
 * @stable ICU 2.0
 */
public class DateFormatSymbols implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;
    
    /** @internal */
    public java.text.DateFormatSymbols dfs;
        
    /** @internal */
    public DateFormatSymbols(java.text.DateFormatSymbols delegate) {
        this.dfs = delegate;
    }

    // TODO make sure local pattern char string is 18 characters long,
    // that is, that it encompasses the new 'u' char for
    // EXTENDED_YEAR.  Two options: 1. Make sure resource data is
    // correct; 2. Make code add in 'u' at end if len == 17.

    // Constants for context
    /**
     * {@icu} Constant for context.
     * @stable ICU 3.6
     */
    public static final int FORMAT = 0;

    /**
     * {@icu} Constant for context.
     * @stable ICU 3.6
     */
    public static final int STANDALONE = 1;

    /**
     * {@icu} Constant for context.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final int DT_CONTEXT_COUNT = 2;

    // Constants for width

    /**
     * {@icu} Constant for width.
     * @stable ICU 3.6
     */
    public static final int ABBREVIATED = 0;

    /**
     * {@icu} Constant for width.
     * @stable ICU 3.6
     */
    public static final int WIDE = 1;

    /**
     * {@icu} Constant for width.
     * @stable ICU 3.6
     */
    public static final int NARROW = 2;

    /**
     * {@icu} Constant for width.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static final int DT_WIDTH_COUNT = 3;

    /**
     * Constructs a DateFormatSymbols object by loading format data from
     * resources for the default locale.
     *
     * @throws java.util.MissingResourceException if the resources for the default locale
     *          cannot be found or cannot be loaded.
     * @stable ICU 2.0
     */
    public DateFormatSymbols()
    {
        this(new java.text.DateFormatSymbols(ULocale.getDefault(Category.FORMAT).toLocale()));
    }

    /**
     * Constructs a DateFormatSymbols object by loading format data from
     * resources for the given locale.
     *
     * @throws java.util.MissingResourceException if the resources for the specified
     *          locale cannot be found or cannot be loaded.
     * @stable ICU 2.0
     */
    public DateFormatSymbols(Locale locale)
    {
        this(new java.text.DateFormatSymbols(locale));
    }

    /**
     * {@icu} Constructs a DateFormatSymbols object by loading format data from
     * resources for the given ulocale.
     *
     * @throws java.util.MissingResourceException if the resources for the specified
     *          locale cannot be found or cannot be loaded.
     * @stable ICU 3.2
     */
    public DateFormatSymbols(ULocale locale)
    {
        this(new java.text.DateFormatSymbols(locale.toLocale()));
    }

    /**
     * Returns a DateFormatSymbols instance for the default locale.
     *
     * {@icunote} Unlike <code>java.text.DateFormatSymbols#getInstance</code>,
     * this method simply returns <code>new com.ibm.icu.text.DateFormatSymbols()</code>.
     * ICU does not support <code>DateFormatSymbolsProvider</code> introduced in Java 6
     * or its equivalent implementation for now.
     *
     * @return A DateFormatSymbols instance.
     * @stable ICU 3.8
     */
    public static DateFormatSymbols getInstance() {
        return new DateFormatSymbols();
    }

    /**
     * Returns a DateFormatSymbols instance for the given locale.
     *
     * {@icunote} Unlike <code>java.text.DateFormatSymbols#getInstance</code>,
     * this method simply returns <code>new com.ibm.icu.text.DateFormatSymbols(locale)</code>.
     * ICU does not support <code>DateFormatSymbolsProvider</code> introduced in Java 6
     * or its equivalent implementation for now.
     *
     * @param locale the locale.
     * @return A DateFormatSymbols instance.
     * @stable ICU 3.8
     */
    public static DateFormatSymbols getInstance(Locale locale) {
        return new DateFormatSymbols(new java.text.DateFormatSymbols(locale));
    }

    /**
     * {@icu} Returns a DateFormatSymbols instance for the given locale.
     *
     * {@icunote} Unlike <code>java.text.DateFormatSymbols#getInstance</code>,
     * this method simply returns <code>new com.ibm.icu.text.DateFormatSymbols(locale)</code>.
     * ICU does not support <code>DateFormatSymbolsProvider</code> introduced in Java 6
     * or its equivalent implementation for now.
     *
     * @param locale the locale.
     * @return A DateFormatSymbols instance.
     * @stable ICU 3.8
     */
    public static DateFormatSymbols getInstance(ULocale locale) {
        return new DateFormatSymbols(new java.text.DateFormatSymbols(locale.toLocale()));
    }

    /**
     * Returns an array of all locales for which the <code>getInstance</code> methods of
     * this class can return localized instances.
     *
     * {@icunote} Unlike <code>java.text.DateFormatSymbols#getAvailableLocales</code>,
     * this method simply returns the array of <code>Locale</code>s available in this
     * class.  ICU does not support <code>DateFormatSymbolsProvider</code> introduced in
     * Java 6 or its equivalent implementation for now.
     *
     * @return An array of <code>Locale</code>s for which localized
     * <code>DateFormatSymbols</code> instances are available.
     * @stable ICU 3.8
     */
    public static Locale[] getAvailableLocales() {
        return java.text.DateFormat.getAvailableLocales();
    }

    /**
     * {@icu} Returns an array of all locales for which the <code>getInstance</code>
     * methods of this class can return localized instances.
     *
     * {@icunote} Unlike <code>java.text.DateFormatSymbols#getAvailableLocales</code>,
     * this method simply returns the array of <code>ULocale</code>s available in this
     * class.  ICU does not support <code>DateFormatSymbolsProvider</code> introduced in
     * Java 6 or its equivalent implementation for now.
     *
     * @return An array of <code>ULocale</code>s for which localized
     * <code>DateFormatSymbols</code> instances are available.
     * @draft ICU 3.8 (retain)
     * @provisional This API might change or be removed in a future release.
     */
    public static ULocale[] getAvailableULocales() {
        Locale[] locales = getAvailableLocales();
        ULocale[] ulocales = new ULocale[locales.length];
        for (int i = 0; i < locales.length; ++i) {
            ulocales[i] = ULocale.forLocale(locales[i]);
        }
        return ulocales;
    }

    /**
     * Returns era strings. For example: "AD" and "BC".
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
     * {@icu} Returns era name strings. For example: "Anno Domini" and "Before Christ".
     * @return the era strings.
     * @stable ICU 3.4
     */
    public String[] getEraNames() {
        return getEras(); // Java has no distinction between era strings and era name strings
    }

    /**
     * {@icu} Sets era name strings. For example: "Anno Domini" and "Before Christ".
     * @param newEraNames the new era strings.
     * @stable ICU 3.8
     */
    public void setEraNames(String[] newEraNames) {
        setEras(newEraNames); // Java has no distinction between era strings and era name strings
    }

    /**
     * Returns month strings. For example: "January", "February", etc.
     * @return the month strings.
     * @stable ICU 2.0
     */
    public String[] getMonths() {
        return dfs.getMonths();
    }

    /**
     * Returns month strings. For example: "January", "February", etc.
     * @param context    The month context, FORMAT or STANDALONE.
     * @param width      The width or the returned month string,
     *                   either WIDE, ABBREVIATED, or NARROW.
     * @return the month strings.
     * @stable ICU 3.4
     */
    public String[] getMonths(int context, int width) {
        // JDK does not support context / narrow months
        switch (width) {
        case WIDE:
            return dfs.getMonths();

        case ABBREVIATED:
        case NARROW:
            return dfs.getShortMonths();

        default:
            throw new IllegalArgumentException("Unsupported width argument value");
        }
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
     * Sets month strings. For example: "January", "February", etc.
     * @param newMonths the new month strings.
     * @param context    The formatting context, FORMAT or STANDALONE.
     * @param width      The width of the month string,
     *                   either WIDE, ABBREVIATED, or NARROW.
     * @stable ICU 3.8
     */
    public void setMonths(String[] newMonths, int context, int width) {
        // JDK does not support context / narrow months
        switch (width) {
        case WIDE:
            dfs.setMonths(newMonths);
            break;

        case ABBREVIATED:
        case NARROW:
            dfs.setShortMonths(newMonths);
            break;

        default:
            throw new IllegalArgumentException("Unsupported width argument value");
        }
    }

    /**
     * Returns short month strings. For example: "Jan", "Feb", etc.
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
     * Returns weekday strings. For example: "Sunday", "Monday", etc.
     * @return the weekday strings. Use <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc. to index the result array.
     * @stable ICU 2.0
     */
    public String[] getWeekdays() {
        return dfs.getWeekdays();
    }

    /**
     * Returns weekday strings. For example: "Sunday", "Monday", etc.
     * @return the weekday strings. Use <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc. to index the result array.
     * @param context    Formatting context, either FORMAT or STANDALONE.
     * @param width      Width of strings to be returned, either
     *                   WIDE, ABBREVIATED, or NARROW
     * @stable ICU 3.4
     */
    public String[] getWeekdays(int context, int width) {
        // JDK does not support context / narrow weekdays
        switch (width) {
        case WIDE:
            return dfs.getWeekdays();

        case ABBREVIATED:
        case NARROW:
            return dfs.getShortWeekdays();

        default:
            throw new IllegalArgumentException("Unsupported width argument value");
        }
    }

    /**
     * Sets weekday strings. For example: "Sunday", "Monday", etc.
     * @param newWeekdays The new weekday strings.
     * @param context     The formatting context, FORMAT or STANDALONE.
     * @param width       The width of the strings,
     *                    either WIDE, ABBREVIATED, or NARROW.
     * @stable ICU 3.8
     */
    public void setWeekdays(String[] newWeekdays, int context, int width) {
        // JDK does not support context / narrow weekdays
        switch (width) {
        case WIDE:
            dfs.setWeekdays(newWeekdays);
            break;

        case ABBREVIATED:
        case NARROW:
            dfs.setShortWeekdays(newWeekdays);
            break;

        default:
            throw new IllegalArgumentException("Unsupported width argument value");
        }
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
     * Returns short weekday strings. For example: "Sun", "Mon", etc.
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

//    /**
//     * {@icu} Returns quarter strings. For example: "1st Quarter", "2nd Quarter", etc.
//     * @param context    The quarter context, FORMAT or STANDALONE.
//     * @param width      The width or the returned quarter string,
//     *                   either WIDE or ABBREVIATED. There are no NARROW quarters.
//     * @return the quarter strings.
//     * @stable ICU 3.6
//     */
//    public String[] getQuarters(int context, int width) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * {@icu} Sets quarter strings. For example: "1st Quarter", "2nd Quarter", etc.
//     * @param newQuarters the new quarter strings.
//     * @param context    The formatting context, FORMAT or STANDALONE.
//     * @param width      The width of the quarter string,
//     *                   either WIDE or ABBREVIATED. There are no NARROW quarters.
//     * @stable ICU 3.8
//     */
//    public void setQuarters(String[] newQuarters, int context, int width) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

    /**
     * Returns am/pm strings. For example: "AM" and "PM".
     * @return the weekday strings.
     * @stable ICU 2.0
     */
    public String[] getAmPmStrings() {
        return dfs.getAmPmStrings();
    }

    /**
     * Sets am/pm strings. For example: "AM" and "PM".
     * @param newAmpms the new ampm strings.
     * @stable ICU 2.0
     */
    public void setAmPmStrings(String[] newAmpms) {
        dfs.setAmPmStrings(newAmpms);
    }

    /**
     * Returns timezone strings.
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
     * Returns localized date-time pattern characters. For example: 'u', 't', etc.
     *
     * <p>Note: ICU no longer provides localized date-time pattern characters for a locale
     * starting ICU 3.8.  This method returns the non-localized date-time pattern
     * characters unless user defined localized data is set by setLocalPatternChars.
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
     * Overrides clone.
     * @stable ICU 2.0
     */
    public Object clone()
    {
        return new DateFormatSymbols((java.text.DateFormatSymbols)dfs.clone());
    }

    /**
     * Override hashCode.
     * Generates a hash code for the DateFormatSymbols object.
     * @stable ICU 2.0
     */
    public int hashCode() {
        return dfs.hashCode();
    }

    /**
     * Overrides equals.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj)
    {
        try {
            return dfs.equals(((DateFormatSymbols)obj).dfs);
        }
        catch (Exception e) {
            return false;
        }
    }

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

//    /**
//     * Returns the {@link DateFormatSymbols} object that should be used to format a
//     * calendar system's dates in the given locale.
//     * <p>
//     * <b>Subclassing:</b><br>
//     * When creating a new Calendar subclass, you must create the
//     * {@link ResourceBundle ResourceBundle}
//     * containing its {@link DateFormatSymbols DateFormatSymbols} in a specific place.
//     * The resource bundle name is based on the calendar's fully-specified
//     * class name, with ".resources" inserted at the end of the package name
//     * (just before the class name) and "Symbols" appended to the end.
//     * For example, the bundle corresponding to "com.ibm.icu.util.HebrewCalendar"
//     * is "com.ibm.icu.impl.data.HebrewCalendarSymbols".
//     * <p>
//     * Within the ResourceBundle, this method searches for five keys:
//     * <ul>
//     * <li><b>DayNames</b> -
//     *      An array of strings corresponding to each possible
//     *      value of the <code>DAY_OF_WEEK</code> field.  Even though
//     *      <code>DAY_OF_WEEK</code> starts with <code>SUNDAY</code> = 1,
//     *      This array is 0-based; the name for Sunday goes in the
//     *      first position, at index 0.  If this key is not found
//     *      in the bundle, the day names are inherited from the
//     *      default <code>DateFormatSymbols</code> for the requested locale.
//     *
//     * <li><b>DayAbbreviations</b> -
//     *      An array of abbreviated day names corresponding
//     *      to the values in the "DayNames" array.  If this key
//     *      is not found in the resource bundle, the "DayNames"
//     *      values are used instead.  If neither key is found,
//     *      the day abbreviations are inherited from the default
//     *      <code>DateFormatSymbols</code> for the locale.
//     *
//     * <li><b>MonthNames</b> -
//     *      An array of strings corresponding to each possible
//     *      value of the <code>MONTH</code> field.  If this key is not found
//     *      in the bundle, the month names are inherited from the
//     *      default <code>DateFormatSymbols</code> for the requested locale.
//     *
//     * <li><b>MonthAbbreviations</b> -
//     *      An array of abbreviated day names corresponding
//     *      to the values in the "MonthNames" array.  If this key
//     *      is not found in the resource bundle, the "MonthNames"
//     *      values are used instead.  If neither key is found,
//     *      the day abbreviations are inherited from the default
//     *      <code>DateFormatSymbols</code> for the locale.
//     *
//     * <li><b>Eras</b> -
//     *      An array of strings corresponding to each possible
//     *      value of the <code>ERA</code> field.  If this key is not found
//     *      in the bundle, the era names are inherited from the
//     *      default <code>DateFormatSymbols</code> for the requested locale.
//     * </ul>
//     * <p>
//     * @param cal       The calendar system whose date format symbols are desired.
//     * @param locale    The locale whose symbols are desired.
//     *
//     * @see DateFormatSymbols#DateFormatSymbols(java.util.Locale)
//     * @stable ICU 2.0
//     */
//    public DateFormatSymbols(Calendar cal, Locale locale) {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the {@link DateFormatSymbols} object that should be used to format a
//     * calendar system's dates in the given locale.
//     * <p>
//     * <b>Subclassing:</b><br>
//     * When creating a new Calendar subclass, you must create the
//     * {@link ResourceBundle ResourceBundle}
//     * containing its {@link DateFormatSymbols DateFormatSymbols} in a specific place.
//     * The resource bundle name is based on the calendar's fully-specified
//     * class name, with ".resources" inserted at the end of the package name
//     * (just before the class name) and "Symbols" appended to the end.
//     * For example, the bundle corresponding to "com.ibm.icu.util.HebrewCalendar"
//     * is "com.ibm.icu.impl.data.HebrewCalendarSymbols".
//     * <p>
//     * Within the ResourceBundle, this method searches for five keys:
//     * <ul>
//     * <li><b>DayNames</b> -
//     *      An array of strings corresponding to each possible
//     *      value of the <code>DAY_OF_WEEK</code> field.  Even though
//     *      <code>DAY_OF_WEEK</code> starts with <code>SUNDAY</code> = 1,
//     *      This array is 0-based; the name for Sunday goes in the
//     *      first position, at index 0.  If this key is not found
//     *      in the bundle, the day names are inherited from the
//     *      default <code>DateFormatSymbols</code> for the requested locale.
//     *
//     * <li><b>DayAbbreviations</b> -
//     *      An array of abbreviated day names corresponding
//     *      to the values in the "DayNames" array.  If this key
//     *      is not found in the resource bundle, the "DayNames"
//     *      values are used instead.  If neither key is found,
//     *      the day abbreviations are inherited from the default
//     *      <code>DateFormatSymbols</code> for the locale.
//     *
//     * <li><b>MonthNames</b> -
//     *      An array of strings corresponding to each possible
//     *      value of the <code>MONTH</code> field.  If this key is not found
//     *      in the bundle, the month names are inherited from the
//     *      default <code>DateFormatSymbols</code> for the requested locale.
//     *
//     * <li><b>MonthAbbreviations</b> -
//     *      An array of abbreviated day names corresponding
//     *      to the values in the "MonthNames" array.  If this key
//     *      is not found in the resource bundle, the "MonthNames"
//     *      values are used instead.  If neither key is found,
//     *      the day abbreviations are inherited from the default
//     *      <code>DateFormatSymbols</code> for the locale.
//     *
//     * <li><b>Eras</b> -
//     *      An array of strings corresponding to each possible
//     *      value of the <code>ERA</code> field.  If this key is not found
//     *      in the bundle, the era names are inherited from the
//     *      default <code>DateFormatSymbols</code> for the requested locale.
//     * </ul>
//     * <p>
//     * @param cal       The calendar system whose date format symbols are desired.
//     * @param locale    The ulocale whose symbols are desired.
//     *
//     * @see DateFormatSymbols#DateFormatSymbols(java.util.Locale)
//     * @stable ICU 3.2
//     */
//    public DateFormatSymbols(Calendar cal, ULocale locale) {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * Variant of DateFormatSymbols(Calendar, Locale) that takes the Calendar class
//     * instead of a Calandar instance.
//     * @see #DateFormatSymbols(Calendar, Locale)
//     * @stable ICU 2.2
//     */
//    public DateFormatSymbols(Class<? extends Calendar> calendarClass, Locale locale) {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * Variant of DateFormatSymbols(Calendar, ULocale) that takes the Calendar class
//     * instead of a Calandar instance.
//     * @see #DateFormatSymbols(Calendar, Locale)
//     * @stable ICU 3.2
//     */
//    public DateFormatSymbols(Class<? extends Calendar> calendarClass, ULocale locale) {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * Fetches a custom calendar's DateFormatSymbols out of the given resource
//     * bundle.  Symbols that are not overridden are inherited from the
//     * default DateFormatSymbols for the locale.
//     * @see DateFormatSymbols#DateFormatSymbols(java.util.Locale)
//     * @stable ICU 2.0
//     */
//    public DateFormatSymbols(ResourceBundle bundle, Locale locale) {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * Fetches a custom calendar's DateFormatSymbols out of the given resource
//     * bundle.  Symbols that are not overridden are inherited from the
//     * default DateFormatSymbols for the locale.
//     * @see DateFormatSymbols#DateFormatSymbols(java.util.Locale)
//     * @stable ICU 3.2
//     */
//    public DateFormatSymbols(ResourceBundle bundle, ULocale locale) {
//        throw new UnsupportedOperationException("Constructor not supported by com.ibm.icu.base");
//    }

//    /**
//     * Finds the ResourceBundle containing the date format information for
//     * a specified calendar subclass in a given locale.
//     * <p>
//     * The resource bundle name is based on the calendar's fully-specified
//     * class name, with ".resources" inserted at the end of the package name
//     * (just before the class name) and "Symbols" appended to the end.
//     * For example, the bundle corresponding to "com.ibm.icu.util.HebrewCalendar"
//     * is "com.ibm.icu.impl.data.HebrewCalendarSymbols".
//     * <p>
//     * <b>Note:</b>Because of the structural changes in the ICU locale bundle,
//     * this API no longer works as described.  This method always returns null.
//     * @deprecated ICU 4.0
//     */
//    // This API was formerly @stable ICU 2.0
//    static public ResourceBundle getDateFormatBundle(Class<? extends Calendar> calendarClass,
//                                                     Locale locale) throws MissingResourceException {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Finds the ResourceBundle containing the date format information for
//     * a specified calendar subclass in a given locale.
//     * <p>
//     * The resource bundle name is based on the calendar's fully-specified
//     * class name, with ".resources" inserted at the end of the package name
//     * (just before the class name) and "Symbols" appended to the end.
//     * For example, the bundle corresponding to "com.ibm.icu.util.HebrewCalendar"
//     * is "com.ibm.icu.impl.data.HebrewCalendarSymbols".
//     * <p>
//     * <b>Note:</b>Because of the structural changes in the ICU locale bundle,
//     * this API no longer works as described.  This method always returns null.
//     * @deprecated ICU 4.0
//     */
//    // This API was formerly @stable ICU 3.2
//    static public ResourceBundle getDateFormatBundle(Class<? extends Calendar> calendarClass,
//                                                     ULocale locale) throws MissingResourceException {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Variant of getDateFormatBundle(java.lang.Class, java.util.Locale) that takes
//     * a Calendar instance instead of a Calendar class.
//     * <p>
//     * <b>Note:</b>Because of the structural changes in the ICU locale bundle,
//     * this API no longer works as described.  This method always returns null.
//     * @see #getDateFormatBundle(java.lang.Class, java.util.Locale)
//     * @deprecated ICU 4.0
//     */
//    // This API was formerly @stable ICU 2.2
//    public static ResourceBundle getDateFormatBundle(Calendar cal, Locale locale) throws MissingResourceException {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Variant of getDateFormatBundle(java.lang.Class, java.util.Locale) that takes
//     * a Calendar instance instead of a Calendar class.
//     * <p>
//     * <b>Note:</b>Because of the structural changes in the ICU locale bundle,
//     * this API no longer works as described.  This method always returns null.
//     * @see #getDateFormatBundle(java.lang.Class, java.util.Locale)
//     * @deprecated ICU 4.0
//     */
//    // This API was formerly @stable ICU 3.2
//    public static ResourceBundle getDateFormatBundle(Calendar cal, ULocale locale) throws MissingResourceException {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }

//    /**
//     * Returns the locale that was used to create this object, or null.
//     * This may may differ from the locale requested at the time of
//     * this object's creation.  For example, if an object is created
//     * for locale <tt>en_US_CALIFORNIA</tt>, the actual data may be
//     * drawn from <tt>en</tt> (the <i>actual</i> locale), and
//     * <tt>en_US</tt> may be the most specific locale that exists (the
//     * <i>valid</i> locale).
//     *
//     * <p>Note: This method will be implemented in ICU 3.0; ICU 2.8
//     * contains a partial preview implementation.  The * <i>actual</i>
//     * locale is returned correctly, but the <i>valid</i> locale is
//     * not, in most cases.
//     * @param type type of information requested, either {@link
//     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
//     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
//     * @return the information specified by <i>type</i>, or null if
//     * this object was not constructed from locale data.
//     * @see com.ibm.icu.util.ULocale
//     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
//     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
//     * @draft ICU 2.8 (retain)
//     * @provisional This API might change or be removed in a future release.
//     */
//    public final ULocale getLocale(ULocale.Type type) {
//        throw new UnsupportedOperationException("Method not supported by com.ibm.icu.base");
//    }
}
