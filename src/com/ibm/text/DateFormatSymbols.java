/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/DateFormatSymbols.java,v $
 * $Date: 2001/01/09 22:48:51 $
 * $Revision: 1.8 $
 *
 *****************************************************************************************
 */

package com.ibm.text;
import java.util.Locale;
import java.util.ResourceBundle;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;
import java.text.resources.DateFormatZoneData;
import com.ibm.util.Utility;
import com.ibm.util.Calendar;
import com.ibm.util.GregorianCalendar;
import com.ibm.util.TimeZone;
import java.util.MissingResourceException;

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
 * @see          com.ibm.util.SimpleTimeZone
 * @version      1.31 09/21/99
 * @author       Chen-Lieh Huang
 */
public class DateFormatSymbols implements Serializable, Cloneable {

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the default locale.
     *
     * @exception  java.util.MissingResourceException
     *             if the resources for the default locale cannot be
     *             found or cannot be loaded.
     */
    public DateFormatSymbols()
    {
        initializeData(Locale.getDefault());
    }

    /**
     * Construct a DateFormatSymbols object by loading format data from
     * resources for the given locale.
     *
     * @exception  java.util.MissingResourceException
     *             if the resources for the specified locale cannot be
     *             found or cannot be loaded.
     */
    public DateFormatSymbols(Locale locale)
    {
        initializeData(locale);
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
     * @see java.text.resources.DateFormatZoneData
     * @see com.ibm.util.TimeZone
     * @serial
     */
    String zoneStrings[][] = null;

    /**
     * Unlocalized date-time pattern characters. For example: 'y', 'd', etc.
     * All locales use the same these unlocalized pattern characters.
     */
    static final String  patternChars = "GyMdkHmsSEDFwWahKz";

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
     */
    public String[] getEras() {
        return duplicate(eras);
    }

    /**
     * Sets era strings. For example: "AD" and "BC".
     * @param newEras the new era strings.
     */
    public void setEras(String[] newEras) {
        eras = duplicate(newEras);
    }

    /**
     * Gets month strings. For example: "January", "February", etc.
     * @return the month strings.
     */
    public String[] getMonths() {
        return duplicate(months);
    }

    /**
     * Sets month strings. For example: "January", "February", etc.
     * @param newMonths the new month strings.
     */
    public void setMonths(String[] newMonths) {
        months = duplicate(newMonths);
    }

    /**
     * Gets short month strings. For example: "Jan", "Feb", etc.
     * @return the short month strings.
     */
    public String[] getShortMonths() {
        return duplicate(shortMonths);
    }

    /**
     * Sets short month strings. For example: "Jan", "Feb", etc.
     * @param newShortMonths the new short month strings.
     */
    public void setShortMonths(String[] newShortMonths) {
        shortMonths = duplicate(newShortMonths);
    }

    /**
     * Gets weekday strings. For example: "Sunday", "Monday", etc.
     * @return the weekday strings. Use <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc. to index the result array.
     */
    public String[] getWeekdays() {
        return duplicate(weekdays);
    }

    /**
     * Sets weekday strings. For example: "Sunday", "Monday", etc.
     * @param newWeekdays the new weekday strings. The array should
     * be indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     */
    public void setWeekdays(String[] newWeekdays) {
        weekdays = duplicate(newWeekdays);
    }

    /**
     * Gets short weekday strings. For example: "Sun", "Mon", etc.
     * @return the short weekday strings. Use <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc. to index the result array.
     */
    public String[] getShortWeekdays() {
        return duplicate(shortWeekdays);
    }

    /**
     * Sets short weekday strings. For example: "Sun", "Mon", etc.
     * @param newShortWeekdays the new short weekday strings. The array should
     * be indexed by <code>Calendar.SUNDAY</code>,
     * <code>Calendar.MONDAY</code>, etc.
     */
    public void setShortWeekdays(String[] newShortWeekdays) {
        shortWeekdays = duplicate(newShortWeekdays);
    }

    /**
     * Gets ampm strings. For example: "AM" and "PM".
     * @return the weekday strings.
     */
    public String[] getAmPmStrings() {
        return duplicate(ampms);
    }

    /**
     * Sets ampm strings. For example: "AM" and "PM".
     * @param newAmpms the new ampm strings.
     */
    public void setAmPmStrings(String[] newAmpms) {
        ampms = duplicate(newAmpms);
    }

    /**
     * Gets timezone strings.
     * @return the timezone strings.
     */
    public String[][] getZoneStrings() {
        String[][] aCopy = new String[zoneStrings.length][];
        for (int i = 0; i < zoneStrings.length; ++i)
            aCopy[i] = duplicate(zoneStrings[i]);
        return aCopy;
    }

    /**
     * Sets timezone strings.
     * @param newZoneStrings the new timezone strings.
     */
    public void setZoneStrings(String[][] newZoneStrings) {
        String[][] aCopy = new String[newZoneStrings.length][];
        for (int i = 0; i < newZoneStrings.length; ++i)
            aCopy[i] = duplicate(newZoneStrings[i]);
        zoneStrings = aCopy;
    }

    /**
     * Gets localized date-time pattern characters. For example: 'u', 't', etc.
     * @return the localized date-time pattern characters.
     */
    public String getLocalPatternChars() {
        return new String(localPatternChars);
    }

    /**
     * Sets localized date-time pattern characters. For example: 'u', 't', etc.
     * @param newLocalPatternChars the new localized date-time
     * pattern characters.
     */
    public void setLocalPatternChars(String newLocalPatternChars) {
        localPatternChars = new String(newLocalPatternChars);
    }

    /**
     * Overrides Cloneable
     */
    public Object clone()
    {
        try
        {
            DateFormatSymbols other = (DateFormatSymbols)super.clone();
            copyMembers(this, other);
            return other;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Override hashCode.
     * Generates a hash code for the DateFormatSymbols object.
     */
    public int hashCode() {
        int hashcode = 0;
        for (int index = 0; index < this.zoneStrings[0].length; ++index)
            hashcode ^= this.zoneStrings[0][index].hashCode();
        return hashcode;
    }

    /**
     * Override equals
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
     * Cache to hold the LocaleElements and DateFormatZoneData ResourceBundles
     * of a Locale.
     */
    private static Hashtable cachedLocaleData = new Hashtable(3);

    /**
     * cache to hold time zone localized strings. Keyed by Locale
     */
    private static Hashtable cachedZoneData = new Hashtable();

    /* Utility methods for fetching resource bundles */
    private ResourceBundle getLocaleElements(Locale desiredLocale) {
    return ResourceBundle.getBundle("java.text.resources.LocaleElements",
                    desiredLocale);
    }

    private ResourceBundle getZoneData(Locale desiredLocale) {
    return ResourceBundle.getBundle("java.text.resources.DateFormatZoneData",
                    desiredLocale);
    }

    /**
     * Look up resource data for the desiredLocale in the cache; update the
     * cache if necessary.
     */
    private ResourceBundle[] cacheLookup(Locale desiredLocale) {
	ResourceBundle[] rbs = new ResourceBundle[2];
	SoftReference[] data
	    = (SoftReference[])cachedLocaleData.get(desiredLocale);
	if (data == null) {
	    rbs[0] = getLocaleElements(desiredLocale);
	    rbs[1] = getZoneData(desiredLocale);
	    data = new SoftReference[] { new SoftReference(rbs[0]),
					     new SoftReference(rbs[1]) };
	    cachedLocaleData.put(desiredLocale, data);
	} else {
	    ResourceBundle r;
	    if ((r = (ResourceBundle)data[0].get()) == null) {
		r = getLocaleElements(desiredLocale);
		data[0] = new SoftReference(r);
	    }
	    rbs[0] = r;
	    if ((r = (ResourceBundle)data[1].get()) == null) {
		r = getZoneData(desiredLocale);
		data[1] = new SoftReference(r);
	    }
	    rbs[1] = r;
	}
	return rbs;
    }

    /**
     * Load time zone localized strings. Enumerate all keys (except
     * "localPatternChars" and "zoneStrings").
     */
    private String[][] loadZoneStrings(Locale desiredLocale,
				       ResourceBundle rsrc)
    {
        /* We have to handle two different formats of DateFormatZoneData.
         * The first is used in JDK 1.2.2:
         *
         * | public Object[][] getContents() {
         * |   return new Object[][] {
         * |     {"zoneStrings",
         * |       new String[][] {
         * |         {"America/Los_Angeles", "Pacific Standard Time", "PST",
         * |          "Pacific Daylight Time", "PDT" },
         * |         //...
         * |       }
         * |     },
         * |     {"localPatternChars", "GyMdkHmsSEDFwWahKz"},
         * |   };
         * | }
         *
         * The second is used in JDK 1.3:
         *
         * | public Object[][] getContents() {
         * |   return new Object[][] {
         * |     {"America/Los_Angeles", new String[] {"America/Los_Angeles", "Pacific Standard Time", "PST",
         * |                                           "Pacific Daylight Time", "PDT"}},
         * |     {"localPatternChars", "GyMdkHmsSEDFwWahKz"},
         * |   };
         * | }
         *
         * Let's pray they don't alter it further.
         */
        String[][] zones = null;
        SoftReference data = (SoftReference)cachedZoneData.get(desiredLocale);
        if (data == null || ((zones = (String[][])data.get()) == null)) {
            // For JDK 1.3, we have to enumerate over the keys.  For 1.2.2, a
            // single getObject() call works.  As a heuristic, we assume that if
            // the zoneStrings key is present, then we are have a 1.2.2 format.
            // Otherwise we parse the 1.3 format. - liu
            try {
                zones = (String[][])rsrc.getObject("zoneStrings");
            } catch (java.util.MissingResourceException e) {}
            if (zones == null || zones.length == 0) {
            Vector vec = new Vector();
            Enumeration keys = rsrc.getKeys();
            while(keys.hasMoreElements()) {
                String key = (String)keys.nextElement();
                if (!key.equals("localPatternChars") &&
                    !key.equals("zoneStrings")) {
                    vec.add(rsrc.getObject(key));
                }
            }
            zones = new String[vec.size()][];
            vec.toArray(zones);
            }
            data = new SoftReference(zones);
            cachedZoneData.put(desiredLocale, data);
        }
        return zones;
    }

    private void initializeData(Locale desiredLocale)
    {
	int i;
	ResourceBundle[] rbs = cacheLookup(desiredLocale);
	ResourceBundle resource = rbs[0];
	ResourceBundle zoneResource = rbs[1];

	// FIXME: cache only ResourceBundle. Hence every time, will do
	// getObject(). This won't be necessary if the Resource itself
	// is cached.
	eras = (String[])resource.getObject("Eras");
        months = resource.getStringArray("MonthNames");
        shortMonths = resource.getStringArray("MonthAbbreviations");
        String[] lWeekdays = resource.getStringArray("DayNames");
        weekdays = new String[8];
        weekdays[0] = "";  // 1-based
        for (i=0; i<lWeekdays.length; i++)
            weekdays[i+1] = lWeekdays[i];
        String[] sWeekdays = resource.getStringArray("DayAbbreviations");
        shortWeekdays = new String[8];
        shortWeekdays[0] = "";  // 1-based
        for (i=0; i<sWeekdays.length; i++)
            shortWeekdays[i+1] = sWeekdays[i];
        ampms = resource.getStringArray("AmPmMarkers");
	zoneStrings = (String[][])loadZoneStrings(desiredLocale,
						  zoneResource);
        localPatternChars
            = (String) zoneResource.getObject("localPatternChars");
    }

    /**
     * Package private: used by SimpleDateFormat
     * Gets the index for the given time zone ID to obtain the timezone
     * strings for formatting. The time zone ID is just for programmatic
     * lookup. NOT LOCALIZED!!!
     * @param ID the given time zone ID.
     * @return the index of the given time zone ID.  Returns -1 if
     * the given time zone ID can't be located in the DateFormatSymbols object.
     * @see com.ibm.util.SimpleTimeZone
     */
    final int getZoneIndex(String ID) {
        int result = _getZoneIndex(ID);
        if (result >= 0) {
            return result;
        }
        
        // Do a search through the equivalency group for the given ID
        int n = TimeZone.countEquivalentIDs(ID);
        if (n > 1) {
            for (int i=0; i<n; ++i) {
                String equivID = TimeZone.getEquivalentID(ID, i);
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
        for (int index=0; index<zoneStrings.length; index++)
        {
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
        String[] dstArray = new String[srcArray.length];
        System.arraycopy(srcArray, 0, dstArray, 0, srcArray.length);
        return dstArray;
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
        for (int i = 0; i < dst.zoneStrings.length; ++i)
            dst.zoneStrings[i] = duplicate(src.zoneStrings[i]);
        dst.localPatternChars = new String (src.localPatternChars);
    }

    /**
     * Compares the equality of the two arrays of String.
     * @param current this String array.
     * @param other that String array.
     */
    private final boolean equals(String[] current, String[] other)
    {
        int count = current.length;

        for (int i = 0; i < count; ++i)
            if (!current[i].equals(other[i]))
                return false;
        return true;
    }

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
     * For example, the bundle corresponding to "com.ibm.util.HebrewCalendar"
     * is "com.ibm.util.resources.HebrewCalendarSymbols".
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
     */
    public DateFormatSymbols(Calendar cal, Locale locale) {
        this(cal==null?null:cal.getClass(), locale);
    }

    public DateFormatSymbols(Class calendarClass, Locale locale) {
        this(locale); // old-style construction
        if (calendarClass != null) {
            ResourceBundle bundle = null;
            try {
                bundle = getDateFormatBundle(calendarClass, locale);
            } catch (MissingResourceException e) {
                //if (!(cal instanceof GregorianCalendar)) {
                if (!(GregorianCalendar.class.isAssignableFrom(calendarClass))) {
                    // Ok for symbols to be missing for a Gregorian calendar, but
                    // not for any other type.
                    throw e;
                }
            }
            constructCalendarSpecific(bundle);
        }
    }

    /**
     * Fetch a custom calendar's DateFormatSymbols out of the given resource
     * bundle.  Symbols that are not overridden are inherited from the
     * default DateFormatSymbols for the locale.
     * @see DateFormatSymbols#DateFormatSymbols
     */
    public DateFormatSymbols(ResourceBundle bundle, Locale locale) {
        // Get the default symbols for the locale, since most
        // calendars will only need to override month names and will
        // want everything else the same
        this(locale); // old-style construction
        constructCalendarSpecific(bundle);
    }

    /**
     * Given a resource bundle specific to the given Calendar class,
     * initialize this object.  Member variables will have already been
     * initialized using the default mechanism, so only those that differ
     * from or supplement the standard resource data need be handled here.
     * If subclasses override this method, they should call
     * <code>super.constructCalendarSpecific(bundle)</code> as needed to
     * handle the "DayNames", "DayAbbreviations", "MonthNames",
     * "MonthAbbreviations", and "Eras" resource data.
     */
    protected void constructCalendarSpecific(ResourceBundle bundle) {

        // Fetch the day names from the resource bundle.  If they're not found,
        // it's ok; we'll just use the default ones.
        // Allow a null ResourceBundle just for the sake of completeness;
        // this is useful for calendars that don't have any overridden symbols

        if (bundle != null) {
            try {
                String[] temp = bundle.getStringArray("DayNames");
                setWeekdays(temp);
                setShortWeekdays(temp);

                temp = bundle.getStringArray("DayAbbreviations");
                setShortWeekdays( temp );
            } catch (MissingResourceException e) {}

            try {
                String[] temp = bundle.getStringArray("MonthNames");
                setMonths( temp );
                setShortMonths( temp );

                temp = bundle.getStringArray("MonthAbbreviations");
                setShortMonths( temp );
            } catch (MissingResourceException e) {}

            try {
                String[] temp = bundle.getStringArray("Eras");
                setEras( temp );
            } catch (MissingResourceException e) {}
        }
    }

//~    private static final java.text.DateFormatSymbols oldStyleSymbols(DateFormatSymbols syms, Locale loc) {
//~        java.text.DateFormatSymbols result = new java.text.DateFormatSymbols(loc);
//~        result.setAmPmStrings(syms.getAmPmStrings());
//~        result.setEras(syms.getEras());
//~        result.setLocalPatternChars(syms.getLocalPatternChars());
//~        result.setMonths(syms.getMonths());
//~        result.setShortMonths(syms.getShortMonths());
//~        result.setShortWeekdays(syms.getShortWeekdays());
//~        result.setWeekdays(syms.getWeekdays());
//~        result.setZoneStrings(syms.getZoneStrings());
//~        return result;
//~    }

    /**
     * Find the ResourceBundle containing the date format information for
     * a specified calendar subclass in a given locale.
     * <p>
     * The resource bundle name is based on the calendar's fully-specified
     * class name, with ".resources" inserted at the end of the package name
     * (just before the class name) and "Symbols" appended to the end.
     * For example, the bundle corresponding to "com.ibm.util.HebrewCalendar"
     * is "com.ibm.util.resources.HebrewCalendarSymbols".
     */
    static public ResourceBundle getDateFormatBundle(Class calendarClass, Locale locale)
                                  throws MissingResourceException {

        // Find the calendar's class name, which we're going to use to construct the
        // resource bundle name.
        String fullName = calendarClass.getName();
        int lastDot = fullName.lastIndexOf('.');
        String className = fullName.substring(lastDot+1);

        // The name of the ResourceBundle itself is the calendar's fully-qualified
        // name, with ".resources" inserted in the package and "Symbols" appended.
        // E.g., "my.pkg.MyCalendar" -> "my.pkg.resources.MyCalendarSymbols"
        String bundleName = fullName.substring(0, lastDot+1) + "resources."
                                + className + "Symbols";

        ResourceBundle result = null;
        try {
            result = ResourceBundle.getBundle(bundleName, locale);
        }
        catch (MissingResourceException e) {
            //if (!(cal instanceof GregorianCalendar)) {
            if (!(GregorianCalendar.class.isAssignableFrom(calendarClass))) {
                // Ok for symbols to be missing for a Gregorian calendar, but
                // not for any other type.
                throw e;
            }
        }
        return result;
    }

    static public ResourceBundle getDateFormatBundle(Calendar cal, Locale locale)
                                  throws MissingResourceException {
        return getDateFormatBundle(cal==null?null:cal.getClass(), locale);
    }
}
