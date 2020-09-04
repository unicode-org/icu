// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 1996-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;
import java.util.Date;
import java.util.Locale;

import com.ibm.icu.impl.CalType;
import com.ibm.icu.impl.EraRules;

/**
 * <code>JapaneseCalendar</code> is a subclass of <code>GregorianCalendar</code>
 * that numbers years and eras based on the reigns of the Japanese emperors.
 * The Japanese calendar is identical to the Gregorian calendar in all respects
 * except for the year and era.  The ascension of each  emperor to the throne
 * begins a new era, and the years of that era are numbered starting with the
 * year of ascension as year 1.
 * <p>
 * Note that in the year of an imperial ascension, there are two possible sets
 * of year and era values: that for the old era and for the new.  For example, a
 * new era began on January 7, 1989 AD.  Strictly speaking, the first six days
 * of that year were in the Showa era, e.g. "January 6, 64 Showa", while the rest
 * of the year was in the Heisei era, e.g. "January 7, 1 Heisei".  This class
 * handles this distinction correctly when computing dates.  However, in lenient
 * mode either form of date is acceptable as input.
 * <p>
 * In modern times, eras have started on January 8, 1868 AD, Gregorian (Meiji),
 * July 30, 1912 (Taisho), December 25, 1926 (Showa), and January 7, 1989 (Heisei).  Constants
 * for these eras, suitable for use in the <code>ERA</code> field, are provided
 * in this class.  Note that the <em>number</em> used for each era is more or
 * less arbitrary.  Currently, the era starting in 645 AD is era #0; however this
 * may change in the future.  Use the predefined constants rather than using actual,
 * absolute numbers.
 * <p>
 * Since ICU4J 63, start date of each era is imported from CLDR. CLDR era data
 * may contain tentative era in near future with placeholder names. By default,
 * such era data is not enabled. ICU4J users who want to test the behavior of
 * the future era can enable this by one of following settings (in the priority
 * order):
 * <ol>
 * <li>Java system property <code>ICU_ENABLE_TENTATIVE_ERA=true</code>.</li>
 * <li>Environment variable <code>ICU_ENABLE_TENTATIVE_ERA=true</code>.</li>
 * <li>Java system property <code>jdk.calendar.japanese.supplemental.era=xxx</code>.
 *     (Note: This configuration is used for specifying a new era's start date and
 *     names in OpenJDK. ICU4J implementation enables the CLDR tentative era when
 *     this property is defined, but it does not use the start date and names specified
 *     by the property value.)</li>
 * </ol>
 * <p>
 * This class should not be subclassed.</p>
 * <p>
 * JapaneseCalendar usually should be instantiated using
 * {@link com.ibm.icu.util.Calendar#getInstance(ULocale)} passing in a <code>ULocale</code>
 * with the tag <code>"@calendar=japanese"</code>.</p>
 *
 * @see com.ibm.icu.util.GregorianCalendar
 * @see com.ibm.icu.util.Calendar
 *
 * @author Laura Werner
 * @author Alan Liu
 * @stable ICU 2.8
 */
public class JapaneseCalendar extends GregorianCalendar {
    // jdk1.4.2 serialver
    private static final long serialVersionUID = -2977189902603704691L;

    //-------------------------------------------------------------------------
    // Constructors...
    //-------------------------------------------------------------------------

    /**
     * Constructs a default <code>JapaneseCalendar</code> using the current time
     * in the default time zone with the default locale.
     * @stable ICU 2.8
     */
    public JapaneseCalendar() {
        super();
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> based on the current time
     * in the given time zone with the default locale.
     * @param zone the given time zone.
     * @stable ICU 2.8
     */
    public JapaneseCalendar(TimeZone zone) {
        super(zone);
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> based on the current time
     * in the default time zone with the given locale.
     * @param aLocale the given locale.
     * @stable ICU 2.8
     */
    public JapaneseCalendar(Locale aLocale) {
        super(aLocale);
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> based on the current time
     * in the default time zone with the given locale.
     * @param locale the given ulocale.
     * @stable ICU 3.2
     */
    public JapaneseCalendar(ULocale locale) {
        super(locale);
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     *
     * @param aLocale the given locale.
     * @stable ICU 2.8
     */
    public JapaneseCalendar(TimeZone zone, Locale aLocale) {
        super(zone, aLocale);
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> based on the current time
     * in the given time zone with the given locale.
     *
     * @param zone the given time zone.
     *
     * @param locale the given ulocale.
     * @stable ICU 3.2
     */
    public JapaneseCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale);
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param date      The date to which the new calendar is set.
     * @stable ICU 2.8
     */
    public JapaneseCalendar(Date date) {
        this();
        setTime(date);
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param era       The imperial era used to set the calendar's {@link #ERA ERA} field.
     *                  Eras are numbered starting with the Tenki era, which
     *                  began in 1053 AD Gregorian, as era zero.  Recent
     *                  eras can be specified using the constants
     *                  {@link #MEIJI} (which started in 1868 AD),
     *                  {@link #TAISHO} (1912 AD),
     *                  {@link #SHOWA} (1926 AD), and
     *                  {@link #HEISEI} (1989 AD).
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} field,
     *                  in terms of the era.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} field.
     *                  The value is 0-based. e.g., 0 for January.
     *
     * @param date      The value used to set the calendar's DATE field.
     * @stable ICU 2.8
     */
    public JapaneseCalendar(int era, int year, int month, int date) {
        super(year, month, date);
        set(ERA, era);
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> with the given date set
     * in the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} field,
     *                  in the era Heisei, the most current at the time this
     *                  class was last updated.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} field.
     *                  The value is 0-based. e.g., 0 for January.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} field.
     * @stable ICU 2.8
     */
    public JapaneseCalendar(int year, int month, int date) {
        super(year, month, date);
        set(ERA, CURRENT_ERA);
    }

    /**
     * Constructs a <code>JapaneseCalendar</code> with the given date
     * and time set for the default time zone with the default locale.
     *
     * @param year      The value used to set the calendar's {@link #YEAR YEAR} time field,
     *                  in the era Heisei, the most current at the time of this
     *                  writing.
     *
     * @param month     The value used to set the calendar's {@link #MONTH MONTH} time field.
     *                  The value is 0-based. e.g., 0 for January.
     *
     * @param date      The value used to set the calendar's {@link #DATE DATE} time field.
     *
     * @param hour      The value used to set the calendar's {@link #HOUR_OF_DAY HOUR_OF_DAY} time field.
     *
     * @param minute    The value used to set the calendar's {@link #MINUTE MINUTE} time field.
     *
     * @param second    The value used to set the calendar's {@link #SECOND SECOND} time field.
     * @stable ICU 2.8
     */
    public JapaneseCalendar(int year, int month, int date, int hour,
                             int minute, int second)
    {
        super(year, month, date, hour, minute, second);
        set(ERA, CURRENT_ERA);
    }

    //-------------------------------------------------------------------------

    // Use 1970 as the default value of EXTENDED_YEAR
    private static final int GREGORIAN_EPOCH = 1970;

    private static final EraRules ERA_RULES;

    static {
        ERA_RULES = EraRules.getInstance(CalType.JAPANESE, enableTentativeEra());
    }

    /**
     * Check environment variable that enables use of future eras.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public static boolean enableTentativeEra() {
        // Although start date of next Japanese era is planned ahead, a name of
        // new era might not be available. This implementation allows tester to
        // check a new era without era names by settings below (in priority order).
        // By default, such tentative era is disabled.
        //
        // 1. System property ICU_ENABLE_TENTATIVE_ERA=true or false
        // 2. Environment variable ICU_ENABLE_TENTATIVE_ERA=true or false
        // 3. Java system property - jdk.calendar.japanese.supplemental.era for Japanese
        //
        // Note: Java system property specifies the start date of new Japanese era,
        //      but this implementation always uses the date read from ICU data.

        boolean includeTentativeEra = false;

        final String VAR_NAME = "ICU_ENABLE_TENTATIVE_ERA";

        String eraConf = System.getProperty(VAR_NAME);
        if (eraConf == null) {
            eraConf = System.getenv(VAR_NAME);
        }

        if (eraConf != null) {
            includeTentativeEra = eraConf.equalsIgnoreCase("true");
        } else {
            String jdkEraConf = System.getProperty("jdk.calendar.japanese.supplemental.era");
            includeTentativeEra = jdkEraConf != null;
        }
        return includeTentativeEra;
    }

    /**
     * @stable ICU 2.8
     */
    @Override
    protected int handleGetExtendedYear() {
        // EXTENDED_YEAR in JapaneseCalendar is a Gregorian year
        // The default value of EXTENDED_YEAR is 1970 (Showa 45)
        int year;
        if (newerField(EXTENDED_YEAR, YEAR) == EXTENDED_YEAR &&
            newerField(EXTENDED_YEAR, ERA) == EXTENDED_YEAR) {
            year = internalGet(EXTENDED_YEAR, GREGORIAN_EPOCH);
        } else {
            // extended year is a gregorian year, where 1 = 1AD,  0 = 1BC, -1 = 2BC, etc
            year = internalGet(YEAR, 1)                                     // pin to minimum of year 1 (first year)
                    + ERA_RULES.getStartYear(internalGet(ERA, CURRENT_ERA)) // add gregorian starting year
                    - 1;                                                    // Subtract one because year starts at 1
        }
        return year;
    }

    /**
     * Called by handleComputeJulianDay.  Returns the default month (0-based) for the year,
     * taking year and era into account.  Defaults to 0 (JANUARY) for Gregorian.
     * @param extendedYear the extendedYear, as returned by handleGetExtendedYear
     * @return the default month
     * @provisional ICU 3.6
     * @draft ICU 3.6 (retain)
     * @see #MONTH
     */
    @Override
    protected int getDefaultMonthInYear(int extendedYear) {
        int era = internalGet(ERA, CURRENT_ERA);
        // computeFields(status); // No need to compute fields here - expect the caller already did so.

        // Find out if we are at the edge of an era
        int[] eraStart = ERA_RULES.getStartDate(era, null);
        if (extendedYear == eraStart[0]) {
            return eraStart[1]          // month..
                    - 1;                // return 0-based month
        } else {
            return super.getDefaultMonthInYear(extendedYear);
        }
    }

    /**
     * Called by handleComputeJulianDay.  Returns the default day (1-based) for the month,
     * taking currently-set year and era into account.  Defaults to 1 for Gregorian.
     * @param extendedYear the extendedYear, as returned by handleGetExtendedYear
     * @param month the month, as returned by getDefaultMonthInYear
     * @return the default day of the month
     * @draft ICU 3.6 (retain)
     * @provisional ICU 3.6
     * @see #DAY_OF_MONTH
     */
    @Override
    protected int getDefaultDayInMonth(int extendedYear, int month) {
        int era = internalGet(ERA, CURRENT_ERA);
        int[] eraStart = ERA_RULES.getStartDate(era, null);

        if (extendedYear == eraStart[0]) {      // if it is year 1..
            if (month == (eraStart[1] - 1)) {   // if it is the emperor's first month..
                return eraStart[2];             // return the D_O_M of accession
            }
        }

        return super.getDefaultDayInMonth(extendedYear, month);
    }

    /**
     * @stable ICU 2.8
     */
    @Override
    protected void handleComputeFields(int julianDay) {
        super.handleComputeFields(julianDay);
        int year = internalGet(EXTENDED_YEAR);
        int eraIdx = ERA_RULES.getEraIndex(year, internalGet(MONTH) + 1 /* 1-base */, internalGet(DAY_OF_MONTH));

        internalSet(ERA, eraIdx);
        internalSet(YEAR, year - ERA_RULES.getStartYear(eraIdx) + 1);
    }

    //-------------------------------------------------------------------------
    // Public constants for some of the recent eras that folks might use...
    //-------------------------------------------------------------------------

    // Constant for the current era.  This must be regularly updated.
    /**
     * @stable ICU 2.8
     */
    static public final int CURRENT_ERA;

    /**
     * Constant for the era starting on Sept. 8, 1868 AD.
     * @stable  ICU 2.8
     */
    static public final int MEIJI;

    /**
     * Constant for the era starting on July 30, 1912 AD.
     * @stable ICU 2.8
     */
    static public final int TAISHO;

    /**
     * Constant for the era starting on Dec. 25, 1926 AD.
     * @stable ICU 2.8
     */
    static public final int SHOWA;

    /**
     * Constant for the era starting on Jan. 7, 1989 AD.
     * @stable ICU 2.8
     */
    static public final int HEISEI;

    /**
     * Constant for the era starting on May 1, 2019 AD.
     * @stable ICU 64
     */
    static public final int REIWA;

    // We want to make these era constants initialized in a static initializer
    // block to prevent javac to inline these values in a consumer code.
    // By doing so, we can keep better binary compatibility across versions even
    // these values are changed.
    static {
        MEIJI = 232;
        TAISHO = 233;
        SHOWA = 234;
        HEISEI = 235;
        REIWA = 236;
        CURRENT_ERA = ERA_RULES.getCurrentEraIndex();
    }

    /**
     * Override GregorianCalendar.  We should really handle YEAR_WOY and
     * EXTENDED_YEAR here too to implement the 1..5000000 range, but it's
     * not critical.
     * @stable ICU 2.8
     */
    @Override
    @SuppressWarnings("fallthrough")
    protected int handleGetLimit(int field, int limitType) {
        switch (field) {
        case ERA:
            if (limitType == MINIMUM || limitType == GREATEST_MINIMUM) {
                return 0;
            }
            return ERA_RULES.getNumberOfEras() - 1; // max known era, not always CURRENT_ERA
        case YEAR:
        {
            switch (limitType) {
            case MINIMUM:
            case GREATEST_MINIMUM:
                return 1;
            case LEAST_MAXIMUM:
                return 1;
            case MAXIMUM:
                return super.handleGetLimit(field, MAXIMUM) - ERA_RULES.getStartYear(CURRENT_ERA);
            }
            //Fall through to the default if not handled above
        }
        default:
            return super.handleGetLimit(field, limitType);
        }
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public String getType() {
        return "japanese";
    }

    /**
     * {@inheritDoc}
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public boolean haveDefaultCentury() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 4.0
     */
    @Override
    public int getActualMaximum(int field) {
        if (field == YEAR) {
            int era = get(Calendar.ERA);
            if (era == ERA_RULES.getNumberOfEras() - 1) {
                // TODO: Investigate what value should be used here - revisit after 4.0.
                return handleGetLimit(YEAR, MAXIMUM);
            } else {
                int[] nextEraStart = ERA_RULES.getStartDate(era + 1, null);
                int nextEraYear = nextEraStart[0];
                int nextEraMonth = nextEraStart[1]; // 1-base
                int nextEraDate = nextEraStart[2];

                int maxYear = nextEraYear - ERA_RULES.getStartYear(era) + 1; // 1-base
                if (nextEraMonth == 1 && nextEraDate == 1) {
                    // Substract 1, because the next era starts at Jan 1
                    maxYear--;
                }
                return maxYear;
            }
        }
        return super.getActualMaximum(field);
    }

}
