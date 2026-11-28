// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2012-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import com.ibm.icu.impl.Grego;
import com.ibm.icu.util.ULocale.Category;
import java.util.Date;

/**
 * <code>DangiCalendar</code> is a concrete subclass of {@link Calendar} that implements a
 * traditional Korean calendar.
 *
 * @internal
 * @deprecated This API is ICU internal only.
 */
@Deprecated
public class DangiCalendar extends ChineseCalendar {

    private static final long serialVersionUID = 8156297445349501985L;

    /**
     * The start year of the Korean traditional calendar (Dan-gi) is the inaugural year of Dan-gun
     * (BC 2333).
     */
    private static final int DANGI_EPOCH_YEAR = -2332;

    private static final int DANGI_YEAR_OFFSET = 1 - DANGI_EPOCH_YEAR;

    /**
     * The cutover date from lunar calendar to solar calendar is 1896-01-01 (Gregorian).
     * as part of the calendar migration in Korea, part of Gabo Reform.
     * Imperial Directive: https://sillok.history.go.kr/id/kza_13209009_001
     *
     * Due to this, the calendar needs to handle gregorian date switchover from
     * 4228-11-17 (lunar) to 4229-01-01 (solar), as national calendar have migrated.
     *
     * Dangi was at least utilized as Gregorian calendar base since 1948-09-25 (Gregorian),
     * and national migration to gregorian calendar was done by deducting 2333 years from
     * existing documents as specified in "Era Act" at 1962-01-01 (Gregorian)
     *
     * Migration to Dangi:
     * https://www.law.go.kr/LSW//lsInfoP.do?lsiSeq=4301&ancYd=19480925
     *
     * Migration to Gregorian (Proof of Dangi was using Gregorian calendar as base):
     * https://www.law.go.kr/LSW//lsInfoP.do?lsiSeq=4302&ancYd=19611202
     */
    private static final int GREGORIAN_CUTOVER_YEAR = 1896;
    private static final int SOLAR_CUTOVER_YEAR = GREGORIAN_CUTOVER_YEAR + DANGI_YEAR_OFFSET;
    private static final int SOLAR_CUTOVER_MONTH = JANUARY;
    private static final int SOLAR_CUTOVER_DAY = 1;
    private static final int GREGORIAN_CUTOVER_JULIAN_DAY =
            (int) Grego.fieldsToDay(
                            GREGORIAN_CUTOVER_YEAR, SOLAR_CUTOVER_MONTH, SOLAR_CUTOVER_DAY)
                    + EPOCH_JULIAN_DAY;
    private static final int LUNAR_CUTOVER_YEAR = SOLAR_CUTOVER_YEAR - 1;
    private static final int LUNAR_CUTOVER_MONTH = NOVEMBER;
    private static final int LUNAR_CUTOVER_JULIAN_DAY = GREGORIAN_CUTOVER_JULIAN_DAY;

    private static final GregorianCalendar GREGORIAN_LIMITS =
            new GregorianCalendar(TimeZone.GMT_ZONE, ULocale.ROOT);

    private transient boolean useGregorianSolar;

    /**
     * The time zone used for performing astronomical computations for Dangi calendar. In Korea
     * various timezones have been used historically (cf.
     * http://www.math.snu.ac.kr/~kye/others/lunar.html):
     *
     * <p>- 1908/04/01: GMT+8 1908/04/01 - 1911/12/31: GMT+8.5 1912/01/01 - 1954/03/20: GMT+9
     * 1954/03/21 - 1961/08/09: GMT+8.5 1961/08/10 - : GMT+9
     *
     * <p>Note that, in 1908-1911, the government did not apply the timezone change but used GMT+8.
     * In addition, 1954-1961's timezone change does not affect the lunar date calculation.
     * Therefore, the following simpler rule works:
     *
     * <p>-1911: GMT+8 1912-: GMT+9
     *
     * <p>Unfortunately, our astronomer's approximation doesn't agree with the references
     * (http://www.math.snu.ac.kr/~kye/others/lunar.html and
     * http://astro.kasi.re.kr/Life/ConvertSolarLunarForm.aspx?MenuID=115) in 1897/7/30. So the
     * following ad hoc fix is used here:
     *
     * <p>-1896: GMT+8 1897: GMT+7 1898-1911: GMT+8 1912- : GMT+9
     */
    private static final TimeZone KOREA_ZONE;

    static {
        InitialTimeZoneRule initialTimeZone = new InitialTimeZoneRule("GMT+8", 8 * ONE_HOUR, 0);
        long[] millis1897 = {
            (1897 - 1970) * 365L * ONE_DAY
        }; // some days of error is not a problem here
        long[] millis1898 = {
            (1898 - 1970) * 365L * ONE_DAY
        }; // some days of error is not a problem here
        long[] millis1912 = {
            (1912 - 1970) * 365L * ONE_DAY
        }; // this doesn't create an issue for 1911/12/20
        TimeZoneRule rule1897 =
                new TimeArrayTimeZoneRule(
                        "Korean 1897", 7 * ONE_HOUR, 0, millis1897, DateTimeRule.STANDARD_TIME);
        TimeZoneRule rule1898to1911 =
                new TimeArrayTimeZoneRule(
                        "Korean 1898-1911",
                        8 * ONE_HOUR,
                        0,
                        millis1898,
                        DateTimeRule.STANDARD_TIME);
        TimeZoneRule ruleFrom1912 =
                new TimeArrayTimeZoneRule(
                        "Korean 1912-", 9 * ONE_HOUR, 0, millis1912, DateTimeRule.STANDARD_TIME);

        RuleBasedTimeZone tz = new RuleBasedTimeZone("KOREA_ZONE", initialTimeZone);
        tz.addTransitionRule(rule1897);
        tz.addTransitionRule(rule1898to1911);
        tz.addTransitionRule(ruleFrom1912);
        tz.freeze();
        KOREA_ZONE = tz;
    }
    ;

    /**
     * Construct a <code>DangiCalendar</code> with the default time zone and locale.
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public DangiCalendar() {
        this(TimeZone.getDefault(), ULocale.getDefault(Category.FORMAT));
    }

    /**
     * Construct a <code>DangiCalendar</code> with the give date set in the default time zone with
     * the default locale.
     *
     * @param date The date to which the new calendar is set.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public DangiCalendar(Date date) {
        this(TimeZone.getDefault(), ULocale.getDefault(Category.FORMAT));
        setTime(date);
    }

    /**
     * Construct a <code>DangiCalendar</code> based on the current time with the given time zone
     * with the given locale.
     *
     * @param zone the given time zone
     * @param locale the given locale
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Deprecated
    public DangiCalendar(TimeZone zone, ULocale locale) {
        super(zone, locale, KOREA_ZONE);
    }

    @Override
    protected int handleGetExtendedYear() {
        // Dangi exposes continuous Dan-gi years, not Chinese cycle years.
        if (newerField(EXTENDED_YEAR, YEAR) == EXTENDED_YEAR) {
            return internalGet(EXTENDED_YEAR, 1970 + DANGI_YEAR_OFFSET);
        }
        return internalGet(YEAR, 1970 + DANGI_YEAR_OFFSET);
    }

    @Override
    protected void handleComputeFields(int julianDay) {
        // For dates after the cutover, compute fields based on the
        // proleptic Gregorian calendar, and then adjust the year and era fields.
        if (julianDay >= GREGORIAN_CUTOVER_JULIAN_DAY) {
            int gregorianYear = getGregorianYear();
            int dangiYear = gregorianYear + DANGI_YEAR_OFFSET;
            internalSet(ERA, 0);
            internalSet(YEAR, dangiYear);
            internalSet(EXTENDED_YEAR, dangiYear);
            internalSet(MONTH, getGregorianMonth());
            internalSet(ORDINAL_MONTH, getGregorianMonth());
            internalSet(DAY_OF_MONTH, getGregorianDayOfMonth());
            internalSet(DAY_OF_YEAR, getGregorianDayOfYear());
            internalSet(IS_LEAP_MONTH, 0);
            return;
        }

        // For dates before the cutover, compute fields using the lunar calendar.
        super.handleComputeFields(julianDay);
        int dangiYear = internalGet(EXTENDED_YEAR) + DANGI_YEAR_OFFSET;
        internalSet(ERA, 0);
        internalSet(YEAR, dangiYear);
        internalSet(EXTENDED_YEAR, dangiYear);
    }

    @Override
    protected int handleComputeJulianDay(int bestField) {
        boolean oldUseGregorianSolar = useGregorianSolar;
        useGregorianSolar = useGregorianSolarFields(bestField);
        int julianDay = super.handleComputeJulianDay(bestField);

        // If solar field resolution lands before the cutover, retry on the lunar side.
        if (useGregorianSolar && julianDay < GREGORIAN_CUTOVER_JULIAN_DAY) {
            useGregorianSolar = false;
            julianDay = super.handleComputeJulianDay(bestField);
        }
        if (!useGregorianSolar) {
            int dangiYear = handleGetExtendedYear();
            int month = internalGetMonth(getDefaultMonthInYear(dangiYear));
            if (isLunarCutoverMonthOrLater(dangiYear, month)
                    && julianDay >= LUNAR_CUTOVER_JULIAN_DAY) {
                // The lunar cutover label canonicalizes to the solar cutover instant.
                julianDay += GREGORIAN_CUTOVER_JULIAN_DAY - LUNAR_CUTOVER_JULIAN_DAY;
            }
        }

        useGregorianSolar = oldUseGregorianSolar;
        return julianDay;
    }

    @Override
    protected int handleComputeMonthStart(int eyear, int month, boolean useMonth) {
        int[] normalized = normalizeMonth(eyear, useMonth ? month : 0);
        if (useGregorianSolar) {
            return (int) Grego.fieldsToDay(
                            normalized[0] - DANGI_YEAR_OFFSET, normalized[1], 1)
                    + EPOCH_JULIAN_DAY
                    - 1;
        }
        return super.handleComputeMonthStart(
                lunarToGregorianYear(eyear), month, useMonth);
    }

    @Override
    protected int handleGetMonthLength(int extendedYear, int month) {
        int[] normalized = normalizeMonth(extendedYear, month);
        if (isSolarMonth(normalized[0], normalized[1])) {
            return Grego.monthLength(normalized[0] - DANGI_YEAR_OFFSET, normalized[1]);
        }
        return super.handleGetMonthLength(lunarToGregorianYear(extendedYear), month);
    }

    @Override
    protected int handleGetYearLength(int eyear) {
        if (eyear >= SOLAR_CUTOVER_YEAR) {
            int gregorianYear = eyear - DANGI_YEAR_OFFSET;
            return (int) (Grego.fieldsToDay(gregorianYear + 1, JANUARY, 1)
                    - Grego.fieldsToDay(gregorianYear, JANUARY, 1));
        }
        return super.handleGetYearLength(lunarToGregorianYear(eyear));
    }

    @Override
    protected int handleGetLimit(int field, int limitType) {
        switch (field) {
            case ERA:
                return 0;
            case YEAR:
                return super.handleGetLimit(EXTENDED_YEAR, limitType);
            case DAY_OF_MONTH:
                if (limitType == LEAST_MAXIMUM) {
                    return GREGORIAN_LIMITS.handleGetLimit(field, limitType);
                }
                if (limitType == MAXIMUM) {
                    return GREGORIAN_LIMITS.handleGetLimit(field, limitType);
                }
                return super.handleGetLimit(field, limitType);
            case DAY_OF_WEEK_IN_MONTH:
                if (limitType == LEAST_MAXIMUM) {
                    return GREGORIAN_LIMITS.handleGetLimit(field, limitType);
                }
                if (limitType == MAXIMUM) {
                    return GREGORIAN_LIMITS.handleGetLimit(field, limitType);
                }
                return super.handleGetLimit(field, limitType);
            default:
                return super.handleGetLimit(field, limitType);
        }
    }

    @Override
    protected int getDefaultDayInMonth(int extendedYear, int month) {
        if (extendedYear == SOLAR_CUTOVER_YEAR && month == SOLAR_CUTOVER_MONTH) {
            return SOLAR_CUTOVER_DAY;
        }
        return super.getDefaultDayInMonth(extendedYear, month);
    }

    @Override
    public void add(int field, int amount) {
        if (amount != 0 && (field == MONTH || field == ORDINAL_MONTH) && isSolarDate()) {
            addSolarMonth(amount);
            return;
        }
        super.add(field, amount);
    }

    @Override
    public void roll(int field, int amount) {
        if (amount != 0 && (field == MONTH || field == ORDINAL_MONTH) && isSolarDate()) {
            int month = get(MONTH);
            int newMonth = (month + amount) % 12;
            if (newMonth < 0) {
                newMonth += 12;
            }
            setSolarMonth(get(EXTENDED_YEAR), newMonth, get(DAY_OF_MONTH));
            return;
        }
        super.roll(field, amount);
    }

    @Override
    public int getActualMaximum(int field) {
        if (field == DAY_OF_MONTH) {
            DangiCalendar cal = (DangiCalendar) clone();
            cal.setLenient(true);
            cal.prepareGetActual(field, false);
            return cal.handleGetMonthLength(cal.get(EXTENDED_YEAR), cal.get(MONTH));
        }
        if (field == ORDINAL_MONTH && isSolarDate()) {
            return DECEMBER;
        }
        return super.getActualMaximum(field);
    }

    @Override
    public boolean inTemporalLeapYear() {
        if (isSolarDate()) {
            return getActualMaximum(DAY_OF_YEAR) == 366;
        }
        return super.inTemporalLeapYear();
    }

    /**
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    protected int getRelatedYearDifference() {
        return DANGI_EPOCH_YEAR - 1;
    }

    /**
     * {@inheritDoc}
     *
     * @internal
     * @deprecated This API is ICU internal only.
     */
    @Override
    @Deprecated
    public String getType() {
        return "dangi";
    }

    private static int[] normalizeMonth(int year, int month) {
        if (month < JANUARY || month > DECEMBER) {
            int[] rem = new int[1];
            year += floorDivide(month, 12, rem);
            month = rem[0];
        }
        return new int[] {year, month};
    }

    private static boolean isSolarMonth(int dangiYear, int month) {
        return dangiYear > SOLAR_CUTOVER_YEAR
                || (dangiYear == SOLAR_CUTOVER_YEAR && month >= SOLAR_CUTOVER_MONTH);
    }

    private static boolean isLunarCutoverMonthOrLater(int dangiYear, int month) {
        return dangiYear > LUNAR_CUTOVER_YEAR
                || (dangiYear == LUNAR_CUTOVER_YEAR && month >= LUNAR_CUTOVER_MONTH);
    }

    private static int lunarToGregorianYear(int dangiYear) {
        return dangiYear - DANGI_YEAR_OFFSET;
    }

    private boolean useGregorianSolarFields(int bestField) {
        if (isSet(IS_LEAP_MONTH) && internalGet(IS_LEAP_MONTH) != 0) {
            return false;
        }

        int dangiYear;
        if (bestField == WEEK_OF_YEAR && newerField(YEAR_WOY, YEAR) == YEAR_WOY) {
            dangiYear = internalGet(YEAR_WOY);
        } else {
            dangiYear = handleGetExtendedYear();
        }

        if (dangiYear >= SOLAR_CUTOVER_YEAR) {
            return true;
        }
        return false;
    }

    private boolean isSolarDate() {
        return get(JULIAN_DAY) >= GREGORIAN_CUTOVER_JULIAN_DAY;
    }

    private void addSolarMonth(int amount) {
        int[] normalized =
                normalizeMonth(get(EXTENDED_YEAR) - DANGI_YEAR_OFFSET, get(MONTH) + amount);
        setSolarMonth(normalized[0] + DANGI_YEAR_OFFSET, normalized[1], get(DAY_OF_MONTH));
    }

    private void setSolarMonth(int dangiYear, int month, int dayOfMonth) {
        int gregorianYear = dangiYear - DANGI_YEAR_OFFSET;
        int pinnedDay = Math.min(dayOfMonth, Grego.monthLength(gregorianYear, month));
        set(
                JULIAN_DAY,
                (int) Grego.fieldsToDay(gregorianYear, month, pinnedDay) + EPOCH_JULIAN_DAY);
    }
}
