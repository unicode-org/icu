/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/HebrewCalendar.java,v $ 
 * $Date: 2000/10/27 22:25:52 $ 
 * $Revision: 1.5 $
 *
 *****************************************************************************************
 */
package com.ibm.util;

import java.util.Date;
import java.util.Locale;

/**
 * <code>HebrewCalendar</code> is a subclass of <code>Calendar</code>
 * that that implements the traditional Hebrew calendar.
 * This is the civil calendar in Israel and the liturgical calendar
 * of the Jewish faith worldwide.
 * <p>
 * The Hebrew calendar is lunisolar and thus has a number of interesting
 * properties that distinguish it from the Gregorian.  Months start
 * on the day of (an arithmetic approximation of) each new moon.  Since the
 * solar year (approximately 365.24 days) is not an even multiple of
 * the lunar month (approximately 29.53 days) an extra "leap month" is
 * inserted in 7 out of every 19 years.  To make matters even more
 * interesting, the start of a year can be delayed by up to three days
 * in order to prevent certain holidays from falling on the Sabbath and
 * to prevent certain illegal year lengths.  Finally, the lengths of certain
 * months can vary depending on the number of days in the year.
 * <p>
 * The leap month is known as "Adar 1" and is inserted between the
 * months of Shevat and Adar in leap years.  Since the leap month does
 * not come at the end of the year, calculations involving
 * month numbers are particularly complex.  Users of this class should
 * make sure to use the {@link #roll roll} and {@link #add add} methods
 * rather than attempting to perform date arithmetic by manipulating
 * the fields directly.
 * <p>
 * <b>Note:</b> In the traditional Hebrew calendar, days start at sunset.
 * However, in order to keep the time fields in this class
 * synchronized with those of the other calendars and with local clock time,
 * we treat days and months as beginning at midnight,
 * roughly 6 hours after the corresponding sunset.
 * <p>
 * If you are interested in more information on the rules behind the Hebrew
 * calendar, see one of the following references:
 * <ul>
 * <li>"<a href="http://www.amazon.com/exec/obidos/ASIN/0521564743">Calendrical Calculations</a>",
 *      by Nachum Dershowitz & Edward Reingold, Cambridge University Press, 1997, pages 85-91.
 *
 * <li>Hebrew Calendar Science and Myths,
 *      <a href="http://www.geocities.com/Athens/1584/">
 *      http://www.geocities.com/Athens/1584/</a>
 *
 * <li>The Calendar FAQ,
 *      <a href="http://www.pip.dknet.dk/~pip10160/calendar.html">
 *      http://www.pip.dknet.dk/~pip10160/calendar.html</a>
 * </ul>
 * <p>
 * @see com.ibm.util.GregorianCalendar
 *
 * @author Laura Werner
 */
public class HebrewCalendar extends Calendar {

    private static String copyright = "Copyright \u00a9 1997-1998 IBM Corp. All Rights Reserved.";

    //-------------------------------------------------------------------------
    // Tons o' Constants...
    //-------------------------------------------------------------------------

    /** Constant for Tishri, the 1st month of the Hebrew year. */
    public static final int TISHRI = 0;

    /** Constant for Heshvan, the 2nd month of the Hebrew year. */
    public static final int HESHVAN = 1;

    /** Constant for Kislev, the 3rd month of the Hebrew year. */
    public static final int KISLEV = 2;

    /** Constant for Tevet, the 4th month of the Hebrew year. */
    public static final int TEVET = 3;

    /** Constant for Shevat, the 5th month of the Hebrew year. */
    public static final int SHEVAT = 4;

    /**
     * Constant for Adar I, the 6th month of the Hebrew year
     * (present in leap years only). In non-leap years, the calendar
     * jumps from Shevat (5th month) to Adar (7th month).
     */
    public static final int ADAR_1 = 5;

    /** Constant for the Adar, the 7th month of the Hebrew year. */
    public static final int ADAR = 6;

    /** Constant for Nisan, the 8th month of the Hebrew year. */
    public static final int NISAN = 7;

    /** Constant for Iyar, the 9th month of the Hebrew year. */
    public static final int IYAR = 8;

    /** Constant for Sivan, the 10th month of the Hebrew year. */
    public static final int SIVAN = 9;

    /** Constant for Tammuz, the 11th month of the Hebrew year. */
    public static final int TAMUZ = 10;

    /** Constant for Av, the 12th month of the Hebrew year. */
    public static final int AV = 11;

    /** Constant for Elul, the 13th month of the Hebrew year. */
    public static final int ELUL = 12;

    /**
     * The absolute date, in milliseconds since 1/1/1970 AD, Gregorian,
     * of the start of the Hebrew calendar.  In order to keep this calendar's
     * time of day in sync with that of the Gregorian calendar, we use
     * midnight, rather than sunset the day before.
     */
    private static final long EPOCH_MILLIS = -180799862400000L; // 1/1/1 HY

    // Useful millisecond constants
    private static final int  SECOND_MS = 1000;
    private static final int  MINUTE_MS = 60*SECOND_MS;
    private static final int  HOUR_MS   = 60*MINUTE_MS;
    private static final long DAY_MS    = 24*HOUR_MS;
    private static final long WEEK_MS   = 7*DAY_MS;

    /**
     * The minimum and maximum values for all of the fields, for validation
     */
    private static final int MinMax[][] = {
        // Min         Greatest Min    Least Max            Max
        {   0,              0,              0,              0         },  // ERA
        {   1,              1,        5000000,        5000000         },  // YEAR
        {   0,              0,             12,             12         },  // MONTH
        {   0,              0,             51,             56         },  // WEEK_OF_YEAR
        {   0,              0,              5,              6         },  // WEEK_OF_MONTH
        {   1,              1,             29,             30         },  // DAY_OF_MONTH
        {   1,              1,            353,            385         },  // DAY_OF_YEAR
        {   1,              1,              7,              7         },  // DAY_OF_WEEK
        {  -1,             -1,              4,              6         },  // DAY_OF_WEEK_IN_MONTH
        {   0,              0,              1,              1         },  // AM_PM
        {   0,              0,             11,             11         },  // HOUR
        {   0,              0,             23,             23         },  // HOUR_OF_DAY
        {   0,              0,             59,             59         },  // MINUTE
        {   0,              0,             59,             59         },  // SECOND
        {   0,              0,            999,            999         },  // MILLISECOND
        { -12*HOUR_MS,    -12*HOUR_MS,     12*HOUR_MS,     12*HOUR_MS },  // ZONE_OFFSET
        {   0,              0,              1*HOUR_MS,      1*HOUR_MS },
    };

    /**
     * The lengths of the Hebrew months.  This is complicated, because there
     * are three different types of years, or six if you count leap years.
     * Due to the rules for postponing the start of the year to avoid having
     * certain holidays fall on the sabbath, the year can end up being three
     * different lengths, called "deficient", "normal", and "complete".
     */
    private static final int MONTH_LENGTH[][] = {
        // Deficient  Normal     Complete
        {   30,         30,         30     },           //Tishri
        {   29,         29,         30     },           //Heshvan
        {   29,         30,         30     },           //Kislev
        {   29,         29,         29     },           //Tevet
        {   30,         30,         30     },           //Shevat
        {   30,         30,         30     },           //Adar I (leap years only)
        {   29,         29,         29     },           //Adar
        {   30,         30,         30     },           //Nisan
        {   29,         29,         29     },           //Iyar
        {   30,         30,         30     },           //Sivan
        {   29,         29,         29     },           //Tammuz
        {   30,         30,         30     },           //Av
        {   29,         29,         29     },           //Elul
    };

    /**
     * The cumulative # of days to the end of each month in a non-leap year
     * Although this can be calculated from the MONTH_LENGTH table,
     * keeping it around separately makes some calculations a lot faster
     */
    private static final int NUM_DAYS[][] = {
        // Deficient  Normal     Complete
        {    0,          0,          0  },          // (placeholder)
        {   30,         30,         30  },          // Tishri
        {   59,         59,         60  },          // Heshvan
        {   88,         89,         90  },          // Kislev
        {  117,        118,        119  },          // Tevet
        {  147,        148,        149  },          // Shevat
        {  147,        148,        149  },          // (Adar I)
        {  176,        177,        178  },          // Adar
        {  206,        207,        208  },          // Nisan
        {  235,        236,        237  },          // Iyar
        {  265,        266,        267  },          // Sivan
        {  294,        295,        296  },          // Tammuz
        {  324,        325,        326  },          // Av
        {  353,        354,        355  },          // Elul
    };

    /**
     * The cumulative # of days to the end of each month in a leap year
     */
    private static final int LEAP_NUM_DAYS[][] = {
        // Deficient  Normal     Complete
        {    0,          0,          0  },          // (placeholder)
        {   30,         30,         30  },          // Tishri
        {   59,         59,         60  },          // Heshvan
        {   88,         89,         90  },          // Kislev
        {  117,        118,        119  },          // Tevet
        {  147,        148,        149  },          // Shevat
        {  177,        178,        179  },          // Adar I
        {  206,        207,        208  },          // Adar II
        {  236,        237,        238  },          // Nisan
        {  265,        266,        267  },          // Iyar
        {  295,        296,        297  },          // Sivan
        {  324,        325,        326  },          // Tammuz
        {  354,        355,        356  },          // Av
        {  383,        384,        385  },          // Elul
    };

    //-------------------------------------------------------------------------
    // Data Members...
    //-------------------------------------------------------------------------

    /**
     * Since TimeZone rules are all defined in terms of GregorianCalendar,
     * we need a GregorianCalendar object for doing time zone calculations
     * There's no point in lazy-allocating this since it's needed for
     * almost anything this class does.
     */
    private static GregorianCalendar gregorian = new GregorianCalendar();

    private static CalendarCache cache = new CalendarCache();
    
    //-------------------------------------------------------------------------
    // Constructors...
    //-------------------------------------------------------------------------

    /**
     * Constructs a default <code>HebrewCalendar</code> using the current time
     * in the default time zone with the default locale.
     */
    public HebrewCalendar() {
        this(TimeZone.getDefault(), Locale.getDefault());
    }

    /**
     * Constructs a <code>HebrewCalendar</code> based on the current time
     * in the given time zone with the default locale.
     *
     * @param zone The time zone for the new calendar.
     */
    public HebrewCalendar(TimeZone zone) {
        this(zone, Locale.getDefault());
    }

    /**
     * Constructs a <code>HebrewCalendar</code> based on the current time
     * in the default time zone with the given locale.
     *
     * @param aLocale The locale for the new calendar.
     */
    public HebrewCalendar(Locale aLocale) {
        this(TimeZone.getDefault(), aLocale);
    }

    /**
     * Constructs a <code>HebrewCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone The time zone for the new calendar.
     *
     * @param aLocale The locale for the new calendar.
     */
    public HebrewCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
        setTimeInMillis(System.currentTimeMillis());
    }

    /**
     * Constructs a <code>HebrewCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for Tishri.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     */
    public HebrewCalendar(int year, int month, int date) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.set(YEAR, year);
        this.set(MONTH, month);
        this.set(DATE, date);
    }

    /**
     * Constructs a <code>HebrewCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     */
    public HebrewCalendar(Date date) {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.setTime(date);
    }

    /**
     * Constructs a <code>HebrewCalendar</code> with the given date
     * and time set for the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for Tishri.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     *
     * @param hour      The value used to set the calendar's {@link #HOUR_OF_DAY HOUR_OF_DAY} time field.
     *
     * @param minute    The value used to set the calendar's {@link #MINUTE MINUTE} time field.
     *
     * @param second    The value used to set the calendar's {@link #SECOND SECOND} time field.
     */
    public HebrewCalendar(int year, int month, int date, int hour,
                             int minute, int second)
    {
        super(TimeZone.getDefault(), Locale.getDefault());
        this.set(YEAR, year);
        this.set(MONTH, month);
        this.set(DATE, date);
        this.set(HOUR_OF_DAY, hour);
        this.set(MINUTE, minute);
        this.set(SECOND, second);
    }

    //-------------------------------------------------------------------------
    // Minimum / Maximum access functions
    //-------------------------------------------------------------------------

    /**
     * Returns the minimum value for the given field.
     * e.g. for DAY_OF_MONTH, 1
     *
     * @param field The field whose minimum value is desired.
     *
     * @see com.ibm.util.Calendar#getMinimum
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

    /**
     * Return the maximum value that a field could have, given the current date.
     * For example, with the date "Kislev 3, 5757" and the {@link #DAY_OF_MONTH DAY_OF_MONTH} field,
     * the actual maximum would be 29; for "Kislev 3, 5758" it would be 30,
     * since the length of the month Kislev varies from year to year.
     *
     * @param field The field whose actual maximum value is desired.
     *
     * @see #getMaximum
     * @see #getLeastMaximum
     */
    public int getActualMaximum(int field)
    {
        if (!isSet(YEAR) || !isSet(MONTH)) {
            complete();
        }
        switch (field) {
          case MONTH:
            return isLeapYear(fields[YEAR]) ? 13 : 12;

          case DAY_OF_MONTH:
            return monthLength(fields[YEAR], fields[MONTH]);

          case DAY_OF_YEAR:
            return yearLength(fields[YEAR]);

          default:
            return super.getActualMaximum(field);
        }   
    }
        
    //-------------------------------------------------------------------------
    // Rolling and adding functions overridden from Calendar
    //
    // These methods call through to the default implementation in IBMCalendar
    // for most of the fields and only handle the unusual ones themselves.
    //-------------------------------------------------------------------------

    /**
     * Add a signed amount to a specified field, using this calendar's rules.
     * For example, to add three days to the current date, you can call
     * <code>add(Calendar.DATE, 3)</code>. 
     * <p>
     * When adding to certain fields, the values of other fields may conflict and
     * need to be changed.  For example, when adding one to the {@link #MONTH MONTH} field
     * for the date "30 Av 5758", the {@link #DAY_OF_MONTH DAY_OF_MONTH} field
     * must be adjusted so that the result is "29 Elul 5758" rather than the invalid
     * "30 Elul 5758".
     * <p>
     * This method is able to add to
     * all fields except for {@link #ERA ERA}, {@link #DST_OFFSET DST_OFFSET},
     * and {@link #ZONE_OFFSET ZONE_OFFSET}.
     * <p>
     * <b>Note:</b> You should always use {@link #roll roll} and add rather
     * than attempting to perform arithmetic operations directly on the fields
     * of a <tt>HebrewCalendar</tt>.  Since the {@link #MONTH MONTH} field behaves
     * discontinuously in non-leap years, simple arithmetic can give invalid results.
     * <p>
     * @param field     the time field.
     * @param amount    the amount to add to the field.
     *
     * @exception   IllegalArgumentException if the field is invalid or refers
     *              to a field that cannot be handled by this method.
     */
    public void add(int field, int amount)
    {
        switch (field) {
          case MONTH: 
            {
                //
                // MONTH is tricky, because the number of months per year varies
                // It's easiest to just convert to an absolute # of months
                // since the epoch, do the addition, and convert back.
                //
                int month = (235 * get(YEAR) - 234) / 19 + get(MONTH);
                month += amount;

                // Now convert back to year and month values
                int year = (19 * month + 234) / 235;
                month -= (235 * year - 234) / 19;
                
                // In a non-leap year, months after the (missing) leap month
                // must be bumped up by one.
                // TODO: but only if we started before the leap month
                if (month >= ADAR_1 && !isLeapYear(year)) {
                    month++;
                }
                this.set(YEAR, year);
                this.set(MONTH, month);

                pinField(DAY_OF_MONTH);
                break;
            }

          default:
            super.add(field, amount);
            break;
        }
    }

    /**
     * Rolls (up/down) a specified amount time on the given field.  For
     * example, to roll the current date up by three days, you can call
     * <code>roll(Calendar.DATE, 3)</code>.  If the
     * field is rolled past its maximum allowable value, it will "wrap" back
     * to its minimum and continue rolling.  
     * For example, calling <code>roll(Calendar.DATE, 10)</code>
     * on a Hebrew calendar set to "25 Av 5758" will result in the date "5 Av 5758".
     * <p>
     * When rolling certain fields, the values of other fields may conflict and
     * need to be changed.  For example, when rolling the {@link #MONTH MONTH} field
     * upward by one for the date "30 Av 5758", the {@link #DAY_OF_MONTH DAY_OF_MONTH} field
     * must be adjusted so that the result is "29 Elul 5758" rather than the invalid
     * "30 Elul".
     * <p>
     * This method is able to roll
     * all fields except for {@link #ERA ERA}, {@link #DST_OFFSET DST_OFFSET},
     * and {@link #ZONE_OFFSET ZONE_OFFSET}.  Subclasses may, of course, add support for
     * additional fields in their overrides of <code>roll</code>.
     * <p>
     * <b>Note:</b> You should always use roll and {@link #add add} rather
     * than attempting to perform arithmetic operations directly on the fields
     * of a <tt>HebrewCalendar</tt>.  Since the {@link #MONTH MONTH} field behaves
     * discontinuously in non-leap years, simple arithmetic can give invalid results.
     * <p>
     * @param field     the time field.
     * @param amount    the amount by which the field should be rolled.
     *
     * @exception   IllegalArgumentException if the field is invalid or refers
     *              to a field that cannot be handled by this method.
     */
    public void roll(int field, int amount)
    {
        switch (field) {
          case MONTH:
            {
                int month = get(MONTH);
                int year = get(YEAR);
                
                boolean leapYear = isLeapYear(year);
                int yearLength = leapYear ? 13 : 12;
                int newMonth = month + (amount % yearLength);
                //
                // If it's not a leap year and we're rolling past the missing month
                // of ADAR_1, we need to roll an extra month to make up for it.
                // TODO: fix cases like Av + 12 -> Tammuz
                //
                if (!leapYear) {
                    if (amount > 0 && month < ADAR_1 && newMonth >= ADAR_1) {
                        newMonth++;
                    } else if (amount < 0 && month > ADAR_1 && newMonth <= ADAR_1) {
                        newMonth--;
                    }
                }
                set(MONTH, (newMonth + 13) % 13);
                pinField(DAY_OF_MONTH);
                return;
            }
          default:
            super.roll(field, amount);
        }
    }

    //-------------------------------------------------------------------------
    // Functions for converting from field values to milliseconds and back...
    //
    // These are overrides of abstract methods on com.ibm.util.Calendar
    //-------------------------------------------------------------------------

    /**
     * Converts time field values to UTC as milliseconds.
     *
     * @exception IllegalArgumentException if an unknown field is given.
     */
    protected void computeTime()
    {
        if (isTimeSet) return;

        if (!isLenient() && !validateFields())
            throw new IllegalArgumentException("Invalid field values for HebrewCalendar");

        if (isSet(ERA) && internalGet(ERA) != 0)
            throw new IllegalArgumentException("ERA out of range in HebrewCalendar");

        // The year is required.  We don't have to check if it's unset,
        // because if it is, by definition it will be 0.

        int year = internalGet(YEAR);
        long dayNumber = 0, date = 0;

        if (year <= 0) {
            throw new IllegalArgumentException("YEAR out of range in HebrewCalendar");
        }

        // The following code is somewhat convoluted. The various nested
        //  if's handle the different cases of what fields are present.
        if (isSet(MONTH) &&
            (isSet(DATE) ||
             (isSet(DAY_OF_WEEK) &&
              (isSet(WEEK_OF_MONTH) || isSet(DAY_OF_WEEK_IN_MONTH))
             )))
        {
            // We have the month specified. Make it 1-based for the algorithm.
            int month = internalGet(MONTH);
            
            // normalize month
            // TODO: I think this is wrong, since months/year can vary
            if (month < 0) {
                year += month / 13 - 1;
                month = 13 + month % 13;
            } else if (month > 12) {
                year += month / 13;
                month = month % 13;
            }

            dayNumber = startOfYear(year);
            if (isLeapYear(year)) {
                dayNumber += LEAP_NUM_DAYS[month][yearType(year)];
            } else {
                dayNumber += NUM_DAYS[month][yearType(year)];
            }

            if (isSet(DATE))
            {
                date = internalGet(DATE);
            }
            else
            {
                // Compute from day of week plus week number or from the day of
                // week plus the day of week in month.  The computations are
                // almost identical.

                // Find the day of the week for the first of this month.  This
                // is zero-based, with 0 being the locale-specific first day of
                // the week.  Add 1 to get the 1st day of month.  Subtract
                // getFirstDayOfWeek() to make 0-based.
                int fdm = absoluteDayToDayOfWeek(dayNumber + 1) - getFirstDayOfWeek();
                if (fdm < 0) fdm += 7;

                // Find the start of the first week.  This will be a date from
                // 1..-6.  It represents the locale-specific first day of the
                // week of the first day of the month, ignoring minimal days in
                // first week.
                date = 1 - fdm + internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();

                if (isSet(WEEK_OF_MONTH))
                {
                    // Adjust for minimal days in first week.
                    if ((7 - fdm) < getMinimalDaysInFirstWeek()) date += 7;

                    // Now adjust for the week number.
                    date += 7 * (internalGet(WEEK_OF_MONTH) - 1);
                }
                else
                {
                    // Adjust into the month, if needed.
                    if (date < 1) date += 7;

                    // We are basing this on the day-of-week-in-month.  The only
                    // trickiness occurs if the day-of-week-in-month is
                    // negative.
                    int dim = internalGet(DAY_OF_WEEK_IN_MONTH);
                    if (dim >= 0) {
                        date += 7*(dim - 1);
                    } else {
                        // Move date to the last of this day-of-week in this
                        // month, then back up as needed.  If dim==-1, we don't
                        // back up at all.  If dim==-2, we back up once, etc.
                        // Don't back up past the first of the given day-of-week
                        // in this month.  Note that we handle -2, -3,
                        // etc. correctly, even though values < -1 are
                        // technically disallowed.
                        date += ((monthLength(year, fields[MONTH]) - date) / 7 + dim + 1) * 7;
                    }
                }
            }
            dayNumber += date;
        }
        else if (isSet(DAY_OF_YEAR)) {
            dayNumber = startOfYear(year) + internalGet(DAY_OF_YEAR);
        }
        else if (isSet(DAY_OF_WEEK) && isSet(WEEK_OF_YEAR))
        {
            dayNumber = startOfYear(year);

            // Compute from day of week plus week of year

            // Find the day of the week for the first of this year.  This
            // is zero-based, with 0 being the locale-specific first day of
            // the week.  Add 1 to get the 1st day of month.  Subtract
            // getFirstDayOfWeek() to make 0-based.
            int fdy = absoluteDayToDayOfWeek(dayNumber + 1) - getFirstDayOfWeek();
            if (fdy < 0) fdy += 7;

            // Find the start of the first week.  This may be a valid date
            // from 1..7, or a date before the first, from 0..-6.  It
            // represents the locale-specific first day of the week
            // of the first day of the year.

            // First ignore the minimal days in first week.
            date = 1 - fdy + internalGet(DAY_OF_WEEK) - getFirstDayOfWeek();

            // Adjust for minimal days in first week.
            if ((7 - fdy) < getMinimalDaysInFirstWeek()) date += 7;

            // Now adjust for the week number.
            date += 7 * (internalGet(WEEK_OF_YEAR) - 1);

            dayNumber += date;
        }
        else {    // Not enough information
            throw new IllegalArgumentException("Not enough fields set to calculate time");
        }

        long millis = dayNumber * DAY_MS + EPOCH_MILLIS;

        // Now we can do the time portion of the conversion.
        int millisInDay = 0;

        // Hours
        if (isSet(HOUR_OF_DAY))
            // Don't normalize here; let overflow bump into the next period.
            // This is consistent with how we handle other fields.
            millisInDay += internalGet(HOUR_OF_DAY);

        else if (isSet(HOUR))
        {
            // Don't normalize here; let overflow bump into the next period.
            // This is consistent with how we handle other fields.
            millisInDay += internalGet(HOUR);
            millisInDay += 12 * internalGet(AM_PM);
        }

        // Minutes. We use the fact that unset == 0
        millisInDay *= 60;
        millisInDay += internalGet(MINUTE);

        // Seconds. unset == 0
        millisInDay *= 60;
        millisInDay += internalGet(SECOND);

        // Milliseconds. unset == 0
        millisInDay *= 1000;
        millisInDay += internalGet(MILLISECOND);

        // Now add date and millisInDay together, to make millis contain local wall
        // millis, with no zone or DST adjustments
        millis += millisInDay;

        //
        // Compute the time zone offset and DST offset.
        // Since the TimeZone API expects the Gregorian year, month, etc.,
        // We have to convert to local Gregorian time in order to
        // figure out the time zone calculations.  This is a bit slow, but
        // it saves us from doing some *really* nasty calculations here.
        //
        TimeZone zone = getTimeZone();
        int dstOffset = 0;
        
        if (zone.useDaylightTime()) {
            synchronized(gregorian) {
                gregorian.setTimeZone(zone);
                gregorian.setTime(new Date(millis));    // "millis" is local wall clock time
                dstOffset = gregorian.get(DST_OFFSET);
            }
        }
        // Store our final computed GMT time, with timezone adjustments.
        time = millis - dstOffset - zone.getRawOffset();
        isTimeSet = true;
    }

    /**
     * Validates the value of the given time field.
     */
    private boolean boundsCheck(int value, int field)
    {
        return value >= getMinimum(field) && value <= getMaximum(field);
    }


    /**
     * Validates the values of the set time fields.
     */
    private boolean validateFields()
    {
        for (int field = 0; field < FIELD_COUNT; field++)
        {
            // Ignore DATE and DAY_OF_YEAR which are handled below
            if (field != DATE &&
                field != DAY_OF_YEAR &&
                isSet(field) &&
                !boundsCheck(internalGet(field), field))

                return false;
        }

        // Values differ in Least-Maximum and Maximum should be handled
        // specially.
        if (isSet(DATE))
        {
            int date = internalGet(DATE);
            return (date >= getMinimum(DATE) &&
                    date <= monthLength(fields[YEAR], fields[MONTH]));
        }

        if (isSet(DAY_OF_YEAR))
        {
            int days = internalGet(DAY_OF_YEAR);

            if (days < 1 || days > yearLength(internalGet(YEAR)))
                    return false;
        }

        if (isSet(YEAR))
        {
            int year = internalGet(YEAR);
            if (year < 1)
                return false;
        }

        // Handle DAY_OF_WEEK_IN_MONTH, which must not have the value zero.
        // We've checked against minimum and maximum above already.
        if (isSet(DAY_OF_WEEK_IN_MONTH) &&
            0 == internalGet(DAY_OF_WEEK_IN_MONTH)) return false;

        return true;
    }

    /**
     * Convert the time as milliseconds since 1/1/1970 to the Calendar fields
     * such as YEAR, MONTH and DAY.
     */
    protected void computeFields()
    {
        if (areFieldsSet) return;

        // The following algorithm only works for dates from the start of the Hebrew
        // calendar onward.
        if (time < EPOCH_MILLIS && !isLenient()) {
            throw new IllegalArgumentException("HebrewCalendar does not handle dates before 1/1/1 AM");
        }

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

        // We need to find out which Hebrew year the given time is in.
        // Once we know that, we find the time when the year started,
        // and everything else is straightforward

        long epochMillis = localMillis - EPOCH_MILLIS;  // Millis since epoch
        long d = epochMillis / DAY_MS;                  // Days
        long m = (d * DAY_PARTS) / MONTH_PARTS;         // Months (approx)

        int year = (int)((19 * m + 234) / 235) + 1;     // Years (approx)
        long ys  = startOfYear(year);                   // 1st day of year
        int dayOfYear = (int)(d - ys);

        // Because of the postponement rules, it's possible to guess wrong.  Fix it.
        while (dayOfYear < 1) {
            year--;
            ys  = startOfYear(year);
            dayOfYear = (int)(d - ys);
        }

        int dayOfWeek = absoluteDayToDayOfWeek((long)d);

        // Now figure out which month we're in, and the date within that month
        int yearType = yearType(year);
        int numDays[][] = isLeapYear(year) ? LEAP_NUM_DAYS : NUM_DAYS;

        int month = 0;
        while (dayOfYear > numDays[month][yearType]) {
            month++;
        }
        month--;
        int date = dayOfYear - numDays[month][yearType];

        fields[ERA] = 0;
        fields[YEAR] = year;
        fields[MONTH] = month;
        fields[DATE] = date;

        fields[DAY_OF_YEAR] = dayOfYear;
        fields[DAY_OF_WEEK] = dayOfWeek;

        fields[WEEK_OF_YEAR] = weekNumber(dayOfYear, dayOfWeek);
        fields[WEEK_OF_MONTH] = weekNumber(date, dayOfWeek);

        fields[DAY_OF_WEEK_IN_MONTH] = (date-1) / 7 + 1;

        //long days = (long) (localMillis / DAY_MS);
        //int millisInDay = (int) (localMillis - (days * DAY_MS));
        //if (millisInDay < 0) millisInDay += DAY_MS;
        
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
        fields[HOUR] = millisInDay % 12;

        fields[ZONE_OFFSET] = rawOffset;
        fields[DST_OFFSET] = dstOffset;

        areFieldsSet = true;

        // Careful here: We are manually setting the isSet flags to true, so we
        // must be sure that the above code actually does set all these fields.
        _TEMPORARY_markAllFieldsSet();
    }

    //-------------------------------------------------------------------------
    // Functions for converting from milliseconds to field values
    //-------------------------------------------------------------------------

    // Hebrew date calculations are performed in terms of days, hours, and
    // "parts" (or halakim), which are 1/1080 of an hour, or 3 1/3 seconds.
    private static final long HOUR_PARTS = 1080;
    private static final long DAY_PARTS  = 24*HOUR_PARTS;
    
    // An approximate value for the length of a lunar month.
    // It is used to calculate the approximate year and month of a given
    // absolute date.
    static private final int  MONTH_DAYS = 29;
    static private final long MONTH_FRACT = 12*HOUR_PARTS + 793;
    static private final long MONTH_PARTS = MONTH_DAYS*DAY_PARTS + MONTH_FRACT;
    
    // The time of the new moon (in parts) on 1 Tishri, year 1 (the epoch)
    // counting from noon on the day before.  BAHARAD is an abbreviation of
    // Bet (Monday), Hey (5 hours from sunset), Resh-Daled (204).
    static private final long BAHARAD = 11*HOUR_PARTS + 204;

    /**
     * Finds the day # of the first day in the given Hebrew year.
     * To do this, we want to calculate the time of the Tishri 1 new moon
     * in that year.
     * <p>
     * The algorithm here is similar to ones described in a number of
     * references, including:
     * <ul>
     * <li>"Calendrical Calculations", by Nachum Dershowitz & Edward Reingold,
     *     Cambridge University Press, 1997, pages 85-91.
     *
     * <li>Hebrew Calendar Science and Myths,
     *     <a href="http://www.geocities.com/Athens/1584/">
     *     http://www.geocities.com/Athens/1584/</a>
     *
     * <li>The Calendar FAQ,
     *      <a href="http://www.pip.dknet.dk/~pip10160/calendar.faq2.txt">
     *      http://www.pip.dknet.dk/~pip10160/calendar.html</a>
     * </ul>
     */
    private static long startOfYear(int year)
    {
        long day = cache.get(year);
        
        if (day == CalendarCache.EMPTY) {
            int months = (235 * year - 234) / 19;           // # of months before year

            long frac = months * MONTH_FRACT + BAHARAD;     // Fractional part of day #
            day  = months * 29 + (frac / DAY_PARTS);   // Whole # part of calculation
            frac = frac % DAY_PARTS;                        // Time of day

            int wd = (int)(day % 7);                        // Day of week (0 == Monday)

            if (wd == 2 || wd == 4 || wd == 6) {
                // If the 1st is on Sun, Wed, or Fri, postpone to the next day
                day += 1;
                wd = (int)(day % 7);
            }
            if (wd == 1 && frac > 15*HOUR_PARTS+204 && !isLeapYear(year) ) {
                // If the new moon falls after 3:11:20am (15h204p from the previous noon)
                // on a Tuesday and it is not a leap year, postpone by 2 days.
                // This prevents 356-day years.
                day += 2;
            }
            else if (wd == 0 && frac > 21*HOUR_PARTS+589 && isLeapYear(year-1) ) {
                // If the new moon falls after 9:32:43 1/3am (21h589p from yesterday noon)
                // on a Monday and *last* year was a leap year, postpone by 1 day.
                // Prevents 382-day years.
                day += 1;
            }
            cache.put(year, day);
        }
        return day;
    };

    /**
     * Find the day of the week for a given day
     *
     * @param day   The # of days since the start of the Hebrew calendar,
     *              1-based (i.e. 1/1/1 AM is day 1).
     */
    private static int absoluteDayToDayOfWeek(long day)
    {
        // We know that 1/1/1 AM is a Monday, which makes the math easy...
        return (int)(day % 7) + 1;
    }

    /**
     * Returns the number of days in the given Hebrew year
     */
    private static int yearLength(int year)
    {
        return (int)(startOfYear(year+1) - startOfYear(year));
    }

    /**
     * Returns the the type of a given year.
     *  0   "Deficient" year with 353 or 383 days
     *  1   "Normal"    year with 354 or 384 days
     *  2   "Complete"  year with 355 or 385 days
     */
    private static int yearType(int year)
    {
        int yearLength = yearLength(year);

        if (yearLength > 380) {
           yearLength -= 30;        // Subtract length of leap month.
        }

        int type = 0;

        switch (yearLength) {
            case 353:
                type = 0; break;
            case 354:
                type = 1; break;
            case 355:
                type = 2; break;
            default:
                System.out.println("Illegal year length " + yearLength + " in yearType");

        }
        return type;
    }

    /**
     * Returns the length of the given month in the given year
     */
    private static int monthLength(int year, int month)
    {
        switch (month) {
            case HESHVAN:
            case KISLEV:
                // These two month lengths can vary
                return MONTH_LENGTH[month][yearType(year)];
                
            default:
                // The rest are a fixed length
                return MONTH_LENGTH[month][0];
        }
    }

    /**
     * Determine whether a given Hebrew year is a leap year
     *
     * The rule here is that if (year % 19) == 0, 3, 6, 8, 11, 14, or 17.
     * The formula below performs the same test, believe it or not.
     */
    private static boolean isLeapYear(int year)
    {
        return (year * 12 + 17) % 19 >= 12;
    }

    
    static private void debug(String str) {
        if (false) {
            System.out.println(str);
        }
    }
};
