/*
 * $RCSfile: ChineseCalendar.java,v $ $Revision: 1.1 $ $Date: 2000/02/10 06:25:55 $
 */


package com.ibm.util;

import com.ibm.text.ChineseDateFormat;

import java.text.DateFormat;
import java.text.DateFormatSymbols;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;
import java.util.Calendar;

// for debugging
import java.text.SimpleDateFormat;
import java.util.SimpleTimeZone;

public class ChineseCalendar extends IBMCalendar {
    
    // Need an Astronomer object for the moon age calculations
    private static CalendarAstronomer astro = new CalendarAstronomer();
    
    // Useful millisecond constants
    private static final int    SECOND_MS = 1000;
    private static final int    MINUTE_MS = 60*SECOND_MS;
    private static final int    HOUR_MS   = 60*MINUTE_MS;
    private static final long   DAY_MS    = 24*HOUR_MS;
    private static final long   WEEK_MS   = 7*DAY_MS;
    private static final double MONTH_MS  = (long)(astro.SYNODIC_MONTH * DAY_MS);
    private static final double YEAR_MS   = (long)(astro.TROPICAL_YEAR * DAY_MS);
    static private final double PI = 3.14159265358979323846;
    static private final double DEG_RAD  = PI / 180;        // degrees -> radians

    //-------------------------------------------------------------------------
    // Public Constants
    //-------------------------------------------------------------------------
    public final static int LEAP_MONTH  = Calendar.FIELD_COUNT;
    public final static int SOLAR_TERM  = Calendar.FIELD_COUNT + 1;
    public final static int FIELD_COUNT = Calendar.FIELD_COUNT + 2;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Constructs a default <code>ChineseCalendar</code> using the current time
     * in the default time zone with the default locale.
     */
    public ChineseCalendar() {
        this(TimeZone.getDefault(), Locale.getDefault());
    }

    /**
     * Constructs a <code>ChineseCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone The time zone for the new calendar.
     *
     * @param aLocale The locale for the new calendar.
     */
    public ChineseCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        
        fields = new int[FIELD_COUNT];
        isSet  = new boolean[FIELD_COUNT];
        
        setTimeInMillis(System.currentTimeMillis());
    }

    //-------------------------------------------------------------------------
    // Functions for converting from field values to milliseconds and back...
    //
    // These are overrides of abstract methods on java.util.Calendar
    //-------------------------------------------------------------------------

    private static long stdOffset(long utMillis)
    {
        return (utMillis > TZ_EPOCH) ? MODERN_OFFSET : BEIJING_OFFSET;
    }
    
    /**
     * Converts UTC as milliseconds to time field values.
     * The time is <em>not</em>
     * recomputed first; to recompute the time, then the fields, call the
     * {@link #complete} method.
     */
    protected void computeFields()
    {

        // Figure out the current time in China as well, since months
        // are based on the day of the solstice and new moon in China
        long chinaOffset = stdOffset(time);
        long chinaMillis = time + chinaOffset;
        
        // The basic rule governing the Chinese calendar is that the
        // Winter Solstice always falls in the 11th month of the year.  
        
        // Find the previous and next winter solstices, in GMT
        long solstice1 = getSolstice(time, false);
        long solstice2 = getSolstice(time, true);
        
        debug("");
        debug("solstice1 = " + debug(solstice1));
        debug("solstice2 = " + debug(solstice2));
        
        // Find the new moon after the previous solstice and before the next one
        long moon1 = newMoon(solstice1 + DAY_MS, true);
        long moon2 = newMoon(solstice2 + DAY_MS, false);
        
        debug("moon1 =     " + debug(moon1));
        debug("moon2 =     " + debug(moon2));
        
        // And the day when the current month started
        long monthStart = newMoon(time, false);
        
        debug("monthStart= " + debug(monthStart));
        
        fields[DATE] = (int)((time - monthStart) / DAY_MS) + 1;
        
        // Is it a leap year?
        int months = (int)Math.round((moon2 - moon1) / MONTH_MS);
        
        boolean leapYear = (months == 12);
        
        debug("months=" + months + ", leapYear=" + leapYear);
        
        int month = (int)Math.round((monthStart - moon1) / MONTH_MS);
        debug("month=" + month);
        fields[LEAP_MONTH] = 0;
        
        if (leapYear) {
            // There's a leap month between these two solstices.
            // It's always the first month that does *not* contain the start
            // of a major term, i.e. a solar longitude that's an even
            // multiple of 30 degrees.
            long mStart = moon1;
            int tStart = getSolarTerm(mStart) / 2;
            
            for (int m = 0; m < 13; m++) {
                long mEnd = newMoon(mStart + 29*DAY_MS, true);  // End of this month
                int tEnd = getSolarTerm(mEnd) / 2;
                
                debug("m=" + m + ", mStart=" + debug(mStart) + ", tStart=" + tStart + "\n"
                    + "     mEnd=  " + debug(mEnd) + ", tEnd=" + tEnd);
                    
                if (tEnd == tStart) {
                    // A new major solar term did *not* start in this month
                    debug("Month " + m + " is leap month");
                    if (m == month) {
                        fields[LEAP_MONTH] = 1;
                    }
                    if (m <= month) {
                        month--;
                    }
                    break;
                }
                
                mStart = mEnd;  // Start over with next month
                tStart = tEnd;
            }
        }
        debug("month=" + month);

        // The winter solstice occurred in the 11th month of the last year....
        fields[MONTH] = (month + 11) % 12;  // 0-based
        
        debug("fields[MONTH]=" + fields[MONTH]);

        int year = (int)Math.round((moon1 - EPOCH) / (TROPICAL_YEAR*DAY_MS))
                    + ((month > 0) ? 1 : 0);
        
        fields[ERA]  = (int)Math.floor((year-1) / 60) + 1;
        fields[YEAR] = (year-1) % 60 + 1;
        
        
        int dayOfWeek = (int)((time - EPOCH) / DAY_MS + MONDAY) % 7 + SUNDAY;
        if (dayOfWeek < 0) dayOfWeek += 7;
        
        fields[DAY_OF_WEEK] = dayOfWeek;
        fields[WEEK_OF_MONTH] = weekNumber(fields[DATE], dayOfWeek);
        fields[DAY_OF_WEEK_IN_MONTH] = (fields[DATE]-1) / 7 + 1;
        
        //
        // Compute the time zone offset and DST offset.
        // Since the TimeZone API expects the Gregorian year, month, etc.,
        // We have to convert to local Gregorian time in order to
        // figure out the time zone calculations.  This is a bit slow, but
        // it saves us from doing some *really* nasty calculations here.
        //
        TimeZone zone = getTimeZone();
        int rawOffset = zone.getRawOffset();
        int dstOffset = 0;                     // Extra DST offset

        if (zone.useDaylightTime()) {
            synchronized(gregorian) {
                gregorian.setTimeZone(zone);
                gregorian.setTime(new Date(time));
                dstOffset = gregorian.get(DST_OFFSET);
            }
        }
        long localMillis = time + rawOffset + dstOffset;

        // Do the time-related fields....
        int millisInDay = (int)(localMillis % DAY_MS);

        // Fill in all time-related fields based on millisInDay.
        fields[MILLISECOND] = millisInDay % 1000;
        millisInDay /= 1000;
        fields[SECOND] = millisInDay % 60;
        millisInDay /= 60;
        fields[MINUTE] = millisInDay % 60;
        millisInDay /= 60;
        fields[HOUR_OF_DAY] = millisInDay;
        fields[AM_PM] = millisInDay / 12;
        fields[HOUR]  = millisInDay % 12;

        fields[ZONE_OFFSET] = rawOffset;
        fields[DST_OFFSET] = dstOffset;

        areFieldsSet = true;

        // Careful here: We are manually setting the isSet[] flags to true, so we
        // must be sure that the above code actually does set all these fields.
        for (int i=0; i<FIELD_COUNT; ++i) isSet[i] = true;
    }
    
    /**
     * Converts time field values to UTC as milliseconds.
     *
     * @exception IllegalArgumentException if a field has an invalid value 
     * and {@link #isLenient isLenient} returns <code>false</code>.
     */
    protected void computeTime() {
    }
    
    //-------------------------------------------------------------------------
    // Min and Max methods....
    //-------------------------------------------------------------------------

    /**
     * The minimum and maximum values for all of the fields, for validation
     */
    private static final int MinMax[][] = {
        // Min         Greatest Min    Least Max            Max
        {   0,              0,              0,              0         },  // ERA
        {   0,              0,             59,             59         },  // YEAR
        {   0,              0,             12,             12         },  // MONTH
        {   0,              0,             51,             56         },  // WEEK_OF_YEAR
        {   0,              0,              5,              6         },  // WEEK_OF_MONTH
        {   1,              1,             29,             30         },  // DAY_OF_MONTH
        {   1,              1,            365,            365         },  // DAY_OF_YEAR
        {   1,              1,              7,              7         },  // DAY_OF_WEEK
        {  -1,             -1,              4,              6         },  // DAY_OF_WEEK_IN_MONTH
        {   0,              0,              1,              1         },  // AM_PM
        {   0,              0,             11,             11         },  // HOUR
        {   0,              0,             23,             23         },  // HOUR_OF_DAY
        {   0,              0,             59,             59         },  // MINUTE
        {   0,              0,             59,             59         },  // SECOND
        {   0,              0,            999,            999         },  // MILLISECOND
        { -12*HOUR_MS,    -12*HOUR_MS,     12*HOUR_MS,     12*HOUR_MS },  // ZONE_OFFSET
        {   0,              0,              1*HOUR_MS,      1*HOUR_MS },  // DST_OFFSET
        {   0,              0,              1,              1         },  // LEAP_MONTH
        {   1,              1,             12,             12         },  // MAJOR_TERM
        {   1,              1,             12,             12         },  // MINOR_TERM
    };

    /**
     * Returns the minimum value for the given field.
     * e.g. for DAY_OF_MONTH, 1
     *
     * @param field The field whose minimum value is desired.
     *
     * @see java.util.Calendar#getMinimum
     */
    public int getMinimum(int field)
    {
        return MinMax[field][0];
    }

    /**
     * Returns the highest minimum value for the given field.  For the Hebrew
     * calendar, this always returns the same result as <code>getMinimum</code>.
     *
     * @param field The field whose greatest minimum value is desired.
     *
     * @see #getMinimum
     */
    public int getGreatestMinimum(int field)
    {
        return MinMax[field][1];
    }

    /**
     * Returns the maximum value for the given field.
     * e.g. for {@link #DAY_OF_MONTH DAY_OF_MONTH}, 30
     *
     * @param field The field whose maximum value is desired.
     *
     * @see #getLeastMaximum
     * @see #getActualMaximum
     */
    public int getMaximum(int field)
    {
        return MinMax[field][3];
    }

    /**
     * Returns the lowest maximum value for the given field.  For most fields,
     * this returns the same result as {@link #getMaximum getMaximum}.  However,
     * for some fields this can be a lower number. For example,
     * the maximum {@link #DAY_OF_MONTH DAY_OF_MONTH} in the Hebrew caleandar varies
     * from month to month, so this method returns 29 while <code>getMaximum</code>
     * returns 30.
     *
     * @param field The field whose least maximum value is desired.
     *
     * @see #getMaximum
     * @see #getActualMaximum
     */
    public int getLeastMaximum(int field)
    {
        return MinMax[field][2];
    }

    //-------------------------------------------------------------------------
    // Date format stuff
    //-------------------------------------------------------------------------
    
    protected DateFormat getDateTimeFormat(int dateStyle, int timeStyle, Locale loc) {
        debug("in ChineseCalendar.getDateTimeFormat(dateStyle,timeStyle,loc)");
        
        return new ChineseDateFormat(this, dateStyle, timeStyle, loc);
    }
    
    //-------------------------------------------------------------------------
    // Internal utilities
    //-------------------------------------------------------------------------
    
    //-------------------------------------------------------------------------
    // Astronomy
    //-------------------------------------------------------------------------
    
    private static long getSolstice(long utAfter, boolean next) {
        long result = 0;
        synchronized (astro) {
            astro.setTime(utAfter);
            result = astro.getSunTime(CalendarAstronomer.WINTER_SOLSTICE, next);
        }
        long localMillis = result + stdOffset(result);
        return result - (localMillis % DAY_MS);
    }
    
    private static long newMoon(long utAfter, boolean next) {
        long result = 0;
        synchronized (astro) {
            astro.setTime(utAfter);
            result = astro.getMoonTime(CalendarAstronomer.NEW_MOON, next);
        }
        long localMillis = result + stdOffset(result);
        return result - (localMillis % DAY_MS);
    }
    
    private static int getSolarTerm(long uTime) {
        synchronized (astro) {
            astro.setTime(uTime);
            return (int)(2 + Math.floor(astro.getSunLongitude() / (15*DEG_RAD))) % 24;
        }
    }
    
   
    
    //-------------------------------------------------------------------------
    // Internal Data
    //-------------------------------------------------------------------------
    
    // Timezone stuff
    private static final long BEIJING_OFFSET = 7*HOUR_MS + 45*MINUTE_MS + 40*SECOND_MS;
    private static final long MODERN_OFFSET  = 8*HOUR_MS;
    private static final long TZ_EPOCH = -1293822000000L;  // 1/1/1929 AD
    
    // Start of year 1 of the Chinese calendar
    private static final long EPOCH = -145349319600000L; // 2/15/2635 BC
    
    private static final double SYNODIC_MONTH = CalendarAstronomer.SYNODIC_MONTH;
    private static final double TROPICAL_YEAR = CalendarAstronomer.TROPICAL_YEAR;
    
    // Need a GregorianCalendar object for doing time zone calculations
    private static GregorianCalendar gregorian = new GregorianCalendar();
    
   
    private static CalendarCache cache = new CalendarCache();

    static private void debug(String str) {
        if (true) {
            System.out.println(str);
        }
    }

    
    static SimpleDateFormat gmtDf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    static SimpleDateFormat bstDf = new SimpleDateFormat("MM/dd/yyyy HH:mm");
    static {
        gmtDf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        bstDf.setTimeZone(new SimpleTimeZone(8*HOUR_MS, "BST"));
    }
    
    private static String debug(long t) {
        Date d = new Date(t);
        return bstDf.format(d) + " BST [" + gmtDf.format(d) + " GMT]";
    }
};