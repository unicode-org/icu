/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.text;

import com.ibm.icu.impl.ICULocaleData;
import com.ibm.icu.impl.ICUResourceBundle;
import com.ibm.icu.impl.CalendarData;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.UResourceBundle;
import com.ibm.icu.impl.ZoneMeta;
import com.ibm.icu.util.ULocale;

import java.io.Serializable;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

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

    // TODO make sure local pattern char string is 18 characters long,
    // that is, that it encompasses the new 'u' char for
    // EXTENDED_YEAR.  Two options: 1. Make sure resource data is
    // correct; 2. Make code add in 'u' at end if len == 17.

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the default locale.
     *
     * @throws  java.util.MissingResourceException
     *          if the resources for the default locale cannot be
     *          found or cannot be loaded.
     * @stable ICU 2.0
     */
    public DateFormatSymbols()
    {
        initializeData(ULocale.getDefault(), ""); // TODO: type?
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
    public DateFormatSymbols(Locale locale)
    {
        initializeData(ULocale.forLocale(locale), ""); // TODO: type?
    }

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the given ulocale.
     *
     * @throws  java.util.MissingResourceException
     *          if the resources for the specified locale cannot be
     *          found or cannot be loaded.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public DateFormatSymbols(ULocale locale)
    {
        initializeData(locale, ""); // TODO: type?
    }

    /**
     * Era strings. For example: "AD" and "BC".  An array of 2 strings,
     * indexed by <code>Calendar.BC</code> and <code>Calendar.AD</code>.
     * @serial
     */
    String eras[] = null;

    /**
     * Month strings. For example: "January", "February", etc.  An array
     * of 13 strings (some calendars have 13 months), indexed by
     * <code>Calendar.JANUARY</code>, <code>Calendar.FEBRUARY</code>, etc.
     * @serial
     */
    String months[] = null;

    /**
     * Short month strings. For example: "Jan", "Feb", etc.  An array of
     * 13 strings (some calendars have 13 months), indexed by
     * <code>Calendar.JANUARY</code>, <code>Calendar.FEBRUARY</code>, etc.

     * @serial
     */
    String shortMonths[] = null;

    /**
     * Weekday strings. For example: "Sunday", "Monday", etc.  An array
     * of 8 strings, indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     * The element <code>weekdays[0]</code> is ignored.
     * @serial
     */
    String weekdays[] = null;

    /**
     * Short weekday strings. For example: "Sun", "Mon", etc.  An array
     * of 8 strings, indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     * The element <code>shortWeekdays[0]</code> is ignored.
     * @serial
     */
    String shortWeekdays[] = null;

    /**
     * AM and PM strings. For example: "AM" and "PM".  An array of
     * 2 strings, indexed by <code>Calendar.AM</code> and
     * <code>Calendar.PM</code>.
     * @serial
     */
    String ampms[] = null;

    /**
     * Localized names of time zones in this locale.  This is a
     * two-dimensional array of strings of size <em>n</em> by <em>m</em>,
     * where <em>m</em> is at least 5.  Each of the <em>n</em> rows is an
     * entry containing the localized names for a single <code>TimeZone</code>.
     * Each such row contains (with <code>i</code> ranging from
     * 0..<em>n</em>-1):
     * <ul>
     * <li><code>zoneStrings[i][0]</code> - time zone ID</li>
     * <li><code>zoneStrings[i][1]</code> - long name of zone in standard
     * time</li>
     * <li><code>zoneStrings[i][2]</code> - short name of zone in
     * standard time</li>
     * <li><code>zoneStrings[i][3]</code> - long name of zone in daylight
     * savings time</li>
     * <li><code>zoneStrings[i][4]</code> - short name of zone in daylight
     * savings time</li>
     * </ul>
     * The zone ID is <em>not</em> localized; it corresponds to the ID
     * value associated with a system time zone object.  All other entries
     * are localized names.  If a zone does not implement daylight savings
     * time, the daylight savings time names are ignored.
     * @see com.ibm.icu.util.TimeZone
     * @serial
     */
    String zoneStrings[][] = null;

    /**
     * Unlocalized date-time pattern characters. For example: 'y', 'd', etc.
     * All locales use the same unlocalized pattern characters.
     */
    static final String  patternChars = "GyMdkHmsSEDFwWahKzYeugAZ";

    /**
     * Localized date-time pattern characters. For example, a locale may
     * wish to use 'u' rather than 'y' to represent years in its date format
     * pattern strings.
     * This string must be exactly 18 characters long, with the index of
     * the characters described by <code>DateFormat.ERA_FIELD</code>,
     * <code>DateFormat.YEAR_FIELD</code>, etc.  Thus, if the string were
     * "Xz...", then localized patterns would use 'X' for era and 'z' for year.
     * @serial
     */
    String  localPatternChars = null;

    /* use serialVersionUID from JDK 1.1.4 for interoperability */
    static final long serialVersionUID = -5987973545549424702L;

    /**
     * Gets era strings. For example: "AD" and "BC".
     * @return the era strings.
     * @stable ICU 2.0
     */
    public String[] getEras() {
        return duplicate(eras);
    }

    /**
     * Sets era strings. For example: "AD" and "BC".
     * @param newEras the new era strings.
     * @stable ICU 2.0
     */
    public void setEras(String[] newEras) {
        eras = duplicate(newEras);
    }

    /**
     * Gets month strings. For example: "January", "February", etc.
     * @return the month strings.
     * @stable ICU 2.0
     */
    public String[] getMonths() {
        return duplicate(months);
    }

    /**
     * Sets month strings. For example: "January", "February", etc.
     * @param newMonths the new month strings.
     * @stable ICU 2.0
     */
    public void setMonths(String[] newMonths) {
        months = duplicate(newMonths);
    }

    /**
     * Gets short month strings. For example: "Jan", "Feb", etc.
     * @return the short month strings.
     * @stable ICU 2.0
     */
    public String[] getShortMonths() {
        return duplicate(shortMonths);
    }

    /**
     * Sets short month strings. For example: "Jan", "Feb", etc.
     * @param newShortMonths the new short month strings.
     * @stable ICU 2.0
     */
    public void setShortMonths(String[] newShortMonths) {
        shortMonths = duplicate(newShortMonths);
    }

    /**
     * Gets weekday strings. For example: "Sunday", "Monday", etc.
     * @return the weekday strings. Use <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc. to index the result array.
     * @stable ICU 2.0
     */
    public String[] getWeekdays() {
        return duplicate(weekdays);
    }

    /**
     * Sets weekday strings. For example: "Sunday", "Monday", etc.
     * @param newWeekdays the new weekday strings. The array should
     * be indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     * @stable ICU 2.0
     */
    public void setWeekdays(String[] newWeekdays) {
        weekdays = duplicate(newWeekdays);
    }

    /**
     * Gets short weekday strings. For example: "Sun", "Mon", etc.
     * @return the short weekday strings. Use <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc. to index the result array.
     * @stable ICU 2.0
     */
    public String[] getShortWeekdays() {
        return duplicate(shortWeekdays);
    }

    /**
     * Sets short weekday strings. For example: "Sun", "Mon", etc.
     * @param newShortWeekdays the new short weekday strings. The array should
     * be indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     * @stable ICU 2.0
     */
    public void setShortWeekdays(String[] newShortWeekdays) {
        shortWeekdays = duplicate(newShortWeekdays);
    }

    /**
     * Gets ampm strings. For example: "AM" and "PM".
     * @return the weekday strings.
     * @stable ICU 2.0
     */
    public String[] getAmPmStrings() {
        return duplicate(ampms);
    }

    /**
     * Sets ampm strings. For example: "AM" and "PM".
     * @param newAmpms the new ampm strings.
     * @stable ICU 2.0
     */
    public void setAmPmStrings(String[] newAmpms) {
        ampms = duplicate(newAmpms);
    }

    /**
     * Gets timezone strings.
     * @return the timezone strings.
     * @stable ICU 2.0
     */
    public String[][] getZoneStrings() {
        return duplicate(zoneStrings);
    }

    /**
     * Sets timezone strings.
     * @param newZoneStrings the new timezone strings.
     * @stable ICU 2.0
     */
    public void setZoneStrings(String[][] newZoneStrings) {
        zoneStrings = duplicate(newZoneStrings);
    }

    /**
     * Gets localized date-time pattern characters. For example: 'u', 't', etc.
     * @return the localized date-time pattern characters.
     * @stable ICU 2.0
     */
    public String getLocalPatternChars() {
        return new String(localPatternChars);
    }

    /**
     * Sets localized date-time pattern characters. For example: 'u', 't', etc.
     * @param newLocalPatternChars the new localized date-time
     * pattern characters.
     * @stable ICU 2.0
     */
    public void setLocalPatternChars(String newLocalPatternChars) {
        localPatternChars = newLocalPatternChars;
    }

    /**
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone()
    {
        try {
            DateFormatSymbols other = (DateFormatSymbols)super.clone();
            copyMembers(this, other);
            return other;
        } catch (CloneNotSupportedException e) {
            ///CLOVER:OFF
            throw new InternalError();
            ///CLOVER:ON
        }
    }

    /**
     * Override hashCode.
     * Generates a hash code for the DateFormatSymbols object.
     * @stable ICU 2.0
     */
    public int hashCode() {
        int hashcode = 0;
        for (int index = 0; index < this.zoneStrings[0].length; ++index)
            hashcode ^= this.zoneStrings[0][index].hashCode();
        return hashcode;
    }

    /**
     * Override equals
     * @stable ICU 2.0
     */
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DateFormatSymbols that = (DateFormatSymbols) obj;
        return (Utility.arrayEquals(eras, that.eras)
                && Utility.arrayEquals(months, that.months)
                && Utility.arrayEquals(shortMonths, that.shortMonths)
                && Utility.arrayEquals(weekdays, that.weekdays)
                && Utility.arrayEquals(shortWeekdays, that.shortWeekdays)
                && Utility.arrayEquals(ampms, that.ampms)
                && Utility.arrayEquals(zoneStrings, that.zoneStrings)
                && Utility.arrayEquals(localPatternChars,
                                       that.localPatternChars));
    }

    // =======================privates===============================

    /**
     * Useful constant for defining timezone offsets.
     */
    static final int millisPerHour = 60*60*1000;
    /**
     * 
     * @param desiredLocale
     * @param type
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    protected void initializeData(ULocale desiredLocale, String type)
    {
        CalendarData calData = new CalendarData(desiredLocale, type);
        initializeData(desiredLocale, calData);
    }

    /**
     * 
     * @param desiredLocale
     * @param calData
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    protected void initializeData(ULocale desiredLocale, CalendarData calData)
    {

        // FIXME: cache only ResourceBundle. Hence every time, will do
        // getObject(). This won't be necessary if the Resource itself
        // is cached.
        eras = calData.getEras("abbreviated");
        months = calData.getStringArray("monthNames", "wide");
        shortMonths = calData.getStringArray("monthNames", "abbreviated");

        String[] lWeekdays = calData.getStringArray("dayNames", "wide");
        weekdays = new String[8];
        weekdays[0] = "";  // 1-based
        System.arraycopy(lWeekdays, 0, weekdays, 1, lWeekdays.length);

        String[] sWeekdays = calData.getStringArray("dayNames", "abbreviated");
        shortWeekdays = new String[8];
        shortWeekdays[0] = "";  // 1-based
        System.arraycopy(sWeekdays, 0, shortWeekdays, 1, sWeekdays.length);

        ampms = calData.getStringArray("AmPmMarkers");

        // These really do use rb and not calData
        ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, desiredLocale);
        // hack around class cast problem
        // zoneStrings = (String[][])rb.getObject("zoneStrings");
        ICUResourceBundle zoneObject = rb.get("zoneStrings");
        zoneStrings = new String[zoneObject.getSize()][];
        for(int i =0; i< zoneObject.getSize(); i++){
            ICUResourceBundle zoneArr = zoneObject.get(i);
            String[] strings = new String[zoneArr.getSize()];
            for(int j=0; j<zoneArr.getSize(); j++){
                strings[j]=zoneArr.get(j).getString();
            }
            zoneStrings[i] = strings;
        }
        localPatternChars = rb.getString("localPatternChars");

        // TODO: obtain correct actual/valid locale later
        ULocale uloc = rb.getULocale();
        setLocale(uloc, uloc);
    }

    /**
     * Package private: used by SimpleDateFormat
     * Gets the index for the given time zone ID to obtain the timezone
     * strings for formatting. The time zone ID is just for programmatic
     * lookup. NOT LOCALIZED!!!
     * @param ID the given time zone ID.
     * @return the index of the given time zone ID.  Returns -1 if
     * the given time zone ID can't be located in the DateFormatSymbols object.
     * @see com.ibm.icu.util.SimpleTimeZone
     */
    final int getZoneIndex(String ID) {
        int result = _getZoneIndex(ID);
        if (result >= 0) {
            return result;
        }
        // Do a search through the equivalency group for the given ID
        int n = ZoneMeta.countEquivalentIDs(ID);
        if (n > 1) {
            for (int i=0; i<n; ++i) {
                String equivID = ZoneMeta.getEquivalentID(ID, i);
                if (!equivID.equals(ID)) {
                    int equivResult = _getZoneIndex(equivID);
                    if (equivResult >= 0) {
                        return equivResult;
                    }
                }
            }
        }
        return -1;
    }
    
    /**
     * Lookup the given ID.  Do NOT do an equivalency search.
     */
    private int _getZoneIndex(String ID)
    {
        for (int index=0; index<zoneStrings.length; index++) {
            if (ID.equalsIgnoreCase(zoneStrings[index][0])) return index;
        }
        return -1;
    }

    /**
     * Clones an array of Strings.
     * @param srcArray the source array to be cloned.
     * @param count the number of elements in the given source array.
     * @return a cloned array.
     */
    private final String[] duplicate(String[] srcArray)
    {
        return (String[])srcArray.clone();
    }

    private final String[][] duplicate(String[][] srcArray)
    {
        String[][] aCopy = new String[srcArray.length][];
        for (int i = 0; i < srcArray.length; ++i)
            aCopy[i] = duplicate(srcArray[i]);
        return aCopy;
    }

    /**
     * Clones all the data members from the source DateFormatSymbols to
     * the target DateFormatSymbols. This is only for subclasses.
     * @param src the source DateFormatSymbols.
     * @param dst the target DateFormatSymbols.
     */
    private final void copyMembers(DateFormatSymbols src, DateFormatSymbols dst)
    {
        dst.eras = duplicate(src.eras);
        dst.months = duplicate(src.months);
        dst.shortMonths = duplicate(src.shortMonths);
        dst.weekdays = duplicate(src.weekdays);
        dst.shortWeekdays = duplicate(src.shortWeekdays);
        dst.ampms = duplicate(src.ampms);
        dst.zoneStrings = duplicate(src.zoneStrings);
        dst.localPatternChars = new String (src.localPatternChars);
    }

    /**
     * Compares the equality of the two arrays of String.
     * @param current this String array.
     * @param other that String array.
    private final boolean equals(String[] current, String[] other)
    {
        int count = current.length;

        for (int i = 0; i < count; ++i)
            if (!current[i].equals(other[i]))
                return false;
        return true;
    }
     */

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Get the {@link DateFormatSymbols} object that should be used to format a
     * calendar system's dates in the given locale.
     * <p>
     * <b>Subclassing:</b><br>
     * When creating a new Calendar subclass, you must create the
     * {@link ResourceBundle ResourceBundle}
     * containing its {@link DateFormatSymbols DateFormatSymbols} in a specific place.
     * The resource bundle name is based on the calendar's fully-specified
     * class name, with ".resources" inserted at the end of the package name
     * (just before the class name) and "Symbols" appended to the end.
     * For example, the bundle corresponding to "com.ibm.icu.util.HebrewCalendar"
     * is "com.ibm.icu.impl.data.HebrewCalendarSymbols".
     * <p>
     * Within the ResourceBundle, this method searches for five keys:
     * <ul>
     * <li><b>DayNames</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>DAY_OF_WEEK</code> field.  Even though
     *      <code>DAY_OF_WEEK</code> starts with <code>SUNDAY</code> = 1,
     *      This array is 0-based; the name for Sunday goes in the
     *      first position, at index 0.  If this key is not found
     *      in the bundle, the day names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     *
     * <li><b>DayAbbreviations</b> -
     *      An array of abbreviated day names corresponding
     *      to the values in the "DayNames" array.  If this key
     *      is not found in the resource bundle, the "DayNames"
     *      values are used instead.  If neither key is found,
     *      the day abbreviations are inherited from the default
     *      <code>DateFormatSymbols</code> for the locale.
     *
     * <li><b>MonthNames</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>MONTH</code> field.  If this key is not found
     *      in the bundle, the month names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     *
     * <li><b>MonthAbbreviations</b> -
     *      An array of abbreviated day names corresponding
     *      to the values in the "MonthNames" array.  If this key
     *      is not found in the resource bundle, the "MonthNames"
     *      values are used instead.  If neither key is found,
     *      the day abbreviations are inherited from the default
     *      <code>DateFormatSymbols</code> for the locale.
     *
     * <li><b>Eras</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>ERA</code> field.  If this key is not found
     *      in the bundle, the era names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     * </ul>
     * <p>
     * @param cal       The calendar system whose date format symbols are desired.
     * @param locale    The locale whose symbols are desired.
     *
     * @see DateFormatSymbols#DateFormatSymbols(java.util.Locale)
     * @stable ICU 2.0
     */
    public DateFormatSymbols(Calendar cal, Locale locale) {
        initializeData(ULocale.forLocale(locale), cal.getType());
    }

    /**
     * Get the {@link DateFormatSymbols} object that should be used to format a
     * calendar system's dates in the given locale.
     * <p>
     * <b>Subclassing:</b><br>
     * When creating a new Calendar subclass, you must create the
     * {@link ResourceBundle ResourceBundle}
     * containing its {@link DateFormatSymbols DateFormatSymbols} in a specific place.
     * The resource bundle name is based on the calendar's fully-specified
     * class name, with ".resources" inserted at the end of the package name
     * (just before the class name) and "Symbols" appended to the end.
     * For example, the bundle corresponding to "com.ibm.icu.util.HebrewCalendar"
     * is "com.ibm.icu.impl.data.HebrewCalendarSymbols".
     * <p>
     * Within the ResourceBundle, this method searches for five keys:
     * <ul>
     * <li><b>DayNames</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>DAY_OF_WEEK</code> field.  Even though
     *      <code>DAY_OF_WEEK</code> starts with <code>SUNDAY</code> = 1,
     *      This array is 0-based; the name for Sunday goes in the
     *      first position, at index 0.  If this key is not found
     *      in the bundle, the day names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     *
     * <li><b>DayAbbreviations</b> -
     *      An array of abbreviated day names corresponding
     *      to the values in the "DayNames" array.  If this key
     *      is not found in the resource bundle, the "DayNames"
     *      values are used instead.  If neither key is found,
     *      the day abbreviations are inherited from the default
     *      <code>DateFormatSymbols</code> for the locale.
     *
     * <li><b>MonthNames</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>MONTH</code> field.  If this key is not found
     *      in the bundle, the month names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     *
     * <li><b>MonthAbbreviations</b> -
     *      An array of abbreviated day names corresponding
     *      to the values in the "MonthNames" array.  If this key
     *      is not found in the resource bundle, the "MonthNames"
     *      values are used instead.  If neither key is found,
     *      the day abbreviations are inherited from the default
     *      <code>DateFormatSymbols</code> for the locale.
     *
     * <li><b>Eras</b> -
     *      An array of strings corresponding to each possible
     *      value of the <code>ERA</code> field.  If this key is not found
     *      in the bundle, the era names are inherited from the
     *      default <code>DateFormatSymbols</code> for the requested locale.
     * </ul>
     * <p>
     * @param cal       The calendar system whose date format symbols are desired.
     * @param locale    The ulocale whose symbols are desired.
     *
     * @see DateFormatSymbols#DateFormatSymbols(java.util.Locale)
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public DateFormatSymbols(Calendar cal, ULocale locale) {
        initializeData(locale, cal.getType());
    }

    /**
     * Variant of DateFormatSymbols(Calendar, Locale) that takes the Calendar class
     * instead of a Calandar instance.
     * @see #DateFormatSymbols(Calendar, Locale)
     * @stable ICU 2.2
     */
    public DateFormatSymbols(Class calendarClass, Locale locale) {
        this(calendarClass, ULocale.forLocale(locale));
    }

    /**
     * Variant of DateFormatSymbols(Calendar, ULocale) that takes the Calendar class
     * instead of a Calandar instance.
     * @see #DateFormatSymbols(Calendar, Locale)
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public DateFormatSymbols(Class calendarClass, ULocale locale) {
        String fullName = calendarClass.getName();
        int lastDot = fullName.lastIndexOf('.');
        String className = fullName.substring(lastDot+1);
        String calType = className.replaceAll("Calendar","").toLowerCase();
        
        initializeData(locale, calType);
    }

    /**
     * Fetch a custom calendar's DateFormatSymbols out of the given resource
     * bundle.  Symbols that are not overridden are inherited from the
     * default DateFormatSymbols for the locale.
     * @see DateFormatSymbols#DateFormatSymbols
     * @stable ICU 2.0
     */
    public DateFormatSymbols(ResourceBundle bundle, Locale locale) {
        this(bundle, ULocale.forLocale(locale));
    }

    /**
     * Fetch a custom calendar's DateFormatSymbols out of the given resource
     * bundle.  Symbols that are not overridden are inherited from the
     * default DateFormatSymbols for the locale.
     * @see DateFormatSymbols#DateFormatSymbols
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public DateFormatSymbols(ResourceBundle bundle, ULocale locale) {
        initializeData(locale, 
            new CalendarData((ICUResourceBundle)bundle, null));
    }

    /**
     * Find the ResourceBundle containing the date format information for
     * a specified calendar subclass in a given locale.
     * <p>
     * The resource bundle name is based on the calendar's fully-specified
     * class name, with ".resources" inserted at the end of the package name
     * (just before the class name) and "Symbols" appended to the end.
     * For example, the bundle corresponding to "com.ibm.icu.util.HebrewCalendar"
     * is "com.ibm.icu.impl.data.HebrewCalendarSymbols".
     * @stable ICU 2.0
     */
    static public ResourceBundle getDateFormatBundle(Class calendarClass, Locale locale)
        throws MissingResourceException {
        return getDateFormatBundle(calendarClass, ULocale.forLocale(locale));
    }
        
    /**
     * Find the ResourceBundle containing the date format information for
     * a specified calendar subclass in a given locale.
     * <p>
     * The resource bundle name is based on the calendar's fully-specified
     * class name, with ".resources" inserted at the end of the package name
     * (just before the class name) and "Symbols" appended to the end.
     * For example, the bundle corresponding to "com.ibm.icu.util.HebrewCalendar"
     * is "com.ibm.icu.impl.data.HebrewCalendarSymbols".
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    static public ResourceBundle getDateFormatBundle(Class calendarClass, ULocale locale)
        throws MissingResourceException {
        
        // Find the calendar's class name, which we're going to use to construct the
        // resource bundle name.
        String fullName = calendarClass.getName();
        int lastDot = fullName.lastIndexOf('.');
        String className = fullName.substring(lastDot+1);

        String bundleName = className + "Symbols";

        ResourceBundle result = null;
        try {
            result = ICULocaleData.getResourceBundle(bundleName, locale);
        }
        catch (MissingResourceException e) {
            ///CLOVER:OFF
            // coverage requires test without data, so skip
            //if (!(cal instanceof GregorianCalendar)) {
            if (!(GregorianCalendar.class.isAssignableFrom(calendarClass))) {
                // Ok for symbols to be missing for a Gregorian calendar, but
                // not for any other type.
                throw e;
            }
            ///CLOVER:ON
        }
        return result;
    }

    /**
     * Variant of getDateFormatBundle(java.lang.Class, java.util.Locale) that takes
     * a Calendar instance instead of a Calendar class.
     * @see #getDateFormatBundle(java.lang.Class, java.util.Locale)
     * @stable ICU 2.2
     */
    public static ResourceBundle getDateFormatBundle(Calendar cal, Locale locale)
        throws MissingResourceException {
        return getDateFormatBundle(cal.getClass(), ULocale.forLocale(locale));
    }
    
    /**
     * Variant of getDateFormatBundle(java.lang.Class, java.util.Locale) that takes
     * a Calendar instance instead of a Calendar class.
     * @see #getDateFormatBundle(java.lang.Class, java.util.Locale)
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public static ResourceBundle getDateFormatBundle(Calendar cal, ULocale locale)
        throws MissingResourceException {
        return getDateFormatBundle(cal.getClass(), locale);
    }
    
    // -------- BEGIN ULocale boilerplate --------

    /**
     * Return the locale that was used to create this object, or null.
     * This may may differ from the locale requested at the time of
     * this object's creation.  For example, if an object is created
     * for locale <tt>en_US_CALIFORNIA</tt>, the actual data may be
     * drawn from <tt>en</tt> (the <i>actual</i> locale), and
     * <tt>en_US</tt> may be the most specific locale that exists (the
     * <i>valid</i> locale).
     *
     * <p>Note: This method will be implemented in ICU 3.0; ICU 2.8
     * contains a partial preview implementation.  The * <i>actual</i>
     * locale is returned correctly, but the <i>valid</i> locale is
     * not, in most cases.
     * @param type type of information requested, either {@link
     * com.ibm.icu.util.ULocale#VALID_LOCALE} or {@link
     * com.ibm.icu.util.ULocale#ACTUAL_LOCALE}.
     * @return the information specified by <i>type</i>, or null if
     * this object was not constructed from locale data.
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public final ULocale getLocale(ULocale.Type type) {
        return type == ULocale.ACTUAL_LOCALE ?
            this.actualLocale : this.validLocale;
    }

    /**
     * Set information about the locales that were used to create this
     * object.  If the object was not constructed from locale data,
     * both arguments should be set to null.  Otherwise, neither
     * should be null.  The actual locale must be at the same level or
     * less specific than the valid locale.  This method is intended
     * for use by factories or other entities that create objects of
     * this class.
     * @param valid the most specific locale containing any resource
     * data, or null
     * @param actual the locale containing data used to construct this
     * object, or null
     * @see com.ibm.icu.util.ULocale
     * @see com.ibm.icu.util.ULocale#VALID_LOCALE
     * @see com.ibm.icu.util.ULocale#ACTUAL_LOCALE
     * @internal
     */
    final void setLocale(ULocale valid, ULocale actual) {
        // Change the following to an assertion later
        if ((valid == null) != (actual == null)) {
            ///CLOVER:OFF
            throw new IllegalArgumentException();
            ///CLOVER:ON
        }
        // Another check we could do is that the actual locale is at
        // the same level or less specific than the valid locale.
        this.validLocale = valid;
        this.actualLocale = actual;
    }

    /**
     * The most specific locale containing any resource data, or null.
     * @see com.ibm.icu.util.ULocale
     * @internal
     */
    private ULocale validLocale;

    /**
     * The locale containing data used to construct this object, or
     * null.
     * @see com.ibm.icu.util.ULocale
     * @internal
     */
    private ULocale actualLocale;

    // -------- END ULocale boilerplate --------
}
