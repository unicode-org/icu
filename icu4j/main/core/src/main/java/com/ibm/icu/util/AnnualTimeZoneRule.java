// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2007-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;
import java.util.Date;

import com.ibm.icu.impl.Grego;


/**
 * <code>AnnualTimeZoneRule</code> is a class used for representing a time zone
 * rule which takes effect annually.  Years used in this class are
 * all Gregorian calendar years.
 *
 * @stable ICU 3.8
 */
public class AnnualTimeZoneRule extends TimeZoneRule {

    private static final long serialVersionUID = -8870666707791230688L;

    /**
     * The constant representing the maximum year used for designating a rule is permanent.
     * @stable ICU 3.8
     */
    public static final int MAX_YEAR = Integer.MAX_VALUE;

    private final DateTimeRule dateTimeRule;
    private final int startYear;
    private final int endYear;

    /**
     * Constructs a <code>AnnualTimeZoneRule</code> with the name, the GMT offset of its
     * standard time, the amount of daylight saving offset adjustment,
     * the annual start time rule and the start/until years.
     *
     * @param name          The time zone name.
     * @param rawOffset     The GMT offset of its standard time in milliseconds.
     * @param dstSavings    The amount of daylight saving offset adjustment in
     *                      milliseconds.  If this ia a rule for standard time,
     *                      the value of this argument is 0.
     * @param dateTimeRule  The start date/time rule repeated annually.
     * @param startYear     The first year when this rule takes effect.
     * @param endYear       The last year when this rule takes effect.  If this
     *                      rule is effective forever in future, specify MAX_YEAR.
     *
     * @stable ICU 3.8
     */
    public AnnualTimeZoneRule(String name, int rawOffset, int dstSavings,
            DateTimeRule dateTimeRule, int startYear, int endYear) {
        super(name, rawOffset, dstSavings);
        this.dateTimeRule = dateTimeRule;
        this.startYear = startYear;
        this.endYear = endYear;
    }

    /**
     * Gets the start date/time rule associated used by this rule.
     *
     * @return  An <code>AnnualDateTimeRule</code> which represents the start date/time
     *          rule used by this time zone rule.
     *
     * @stable ICU 3.8
     */
    public DateTimeRule getRule() {
        return dateTimeRule;
    }

    /**
     * Gets the first year when this rule takes effect.
     *
     * @return  The start year of this rule.  The year is in Gregorian calendar
     *          with 0 == 1 BCE, -1 == 2 BCE, etc.
     *
     * @stable ICU 3.8
     */
    public int getStartYear() {
        return startYear;
    }

    /**
     * Gets the end year when this rule takes effect.
     *
     * @return  The end year of this rule (inclusive). The year is in Gregorian calendar
     *          with 0 == 1 BCE, -1 == 2 BCE, etc.
     *
     * @stable ICU 3.8
     */
    public int getEndYear() {
        return endYear;
    }

    /**
     * Gets the time when this rule takes effect in the given year.
     *
     * @param year              The Gregorian year, with 0 == 1 BCE, -1 == 2 BCE, etc.
     * @param prevRawOffset     The standard time offset from UTC before this rule
     *                          takes effect in milliseconds.
     * @param prevDSTSavings    The amount of daylight saving offset from the
     *                          standard time.
     *
     * @return  The time when this rule takes effect in the year, or
     *          null if this rule is not applicable in the year.
     *
     * @stable ICU 3.8
     */
    public Date getStartInYear(int year, int prevRawOffset, int prevDSTSavings) {
        if (year < startYear || year > endYear) {
            return null;
        }

        long ruleDay;
        int type = dateTimeRule.getDateRuleType();

        if (type == DateTimeRule.DOM) {
            ruleDay = Grego.fieldsToDay(year, dateTimeRule.getRuleMonth(), dateTimeRule.getRuleDayOfMonth());
        } else {
            boolean after = true;
            if (type == DateTimeRule.DOW) {
                int weeks = dateTimeRule.getRuleWeekInMonth();
                if (weeks > 0) {
                    ruleDay = Grego.fieldsToDay(year, dateTimeRule.getRuleMonth(), 1);
                    ruleDay += 7 * (weeks - 1);
                } else {
                    after = false;
                    ruleDay = Grego.fieldsToDay(year, dateTimeRule.getRuleMonth(),
                            Grego.monthLength(year, dateTimeRule.getRuleMonth()));
                    ruleDay += 7 * (weeks + 1);
                }
            } else {
                int month = dateTimeRule.getRuleMonth();
                int dom = dateTimeRule.getRuleDayOfMonth();
                if (type == DateTimeRule.DOW_LEQ_DOM) {
                    after = false;
                    // Handle Feb <=29
                    if (month == Calendar.FEBRUARY && dom == 29 && !Grego.isLeapYear(year)) {
                        dom--;
                    }
                }
                ruleDay = Grego.fieldsToDay(year, month, dom);
            }

            int dow = Grego.dayOfWeek(ruleDay);
            int delta = dateTimeRule.getRuleDayOfWeek() - dow;
            if (after) {
                delta = delta < 0 ? delta + 7 : delta;
            } else {
                delta = delta > 0 ? delta - 7 : delta;
            }
            ruleDay += delta;
        }

        long ruleTime = ruleDay * Grego.MILLIS_PER_DAY + dateTimeRule.getRuleMillisInDay();
        if (dateTimeRule.getTimeRuleType() != DateTimeRule.UTC_TIME) {
            ruleTime -= prevRawOffset;
        }
        if (dateTimeRule.getTimeRuleType() == DateTimeRule.WALL_TIME) {
            ruleTime -= prevDSTSavings;
        }
        return new Date(ruleTime);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public Date getFirstStart(int prevRawOffset, int prevDSTSavings) {
        return getStartInYear(startYear, prevRawOffset, prevDSTSavings);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public Date getFinalStart(int prevRawOffset, int prevDSTSavings) {
        if (endYear == MAX_YEAR) {
            return null;
        }
        return getStartInYear(endYear, prevRawOffset, prevDSTSavings);
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public Date getNextStart(long base, int prevRawOffset, int prevDSTSavings, boolean inclusive) {
        int[] fields = Grego.timeToFields(base, null);
        int year = fields[0];
        if (year < startYear) {
            return getFirstStart(prevRawOffset, prevDSTSavings);
        }
        Date d = getStartInYear(year, prevRawOffset, prevDSTSavings);
        if (d != null && (d.getTime() < base || (!inclusive && (d.getTime() == base)))) {
            d = getStartInYear(year + 1, prevRawOffset, prevDSTSavings);
        }
        return d;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public Date getPreviousStart(long base, int prevRawOffset, int prevDSTSavings, boolean inclusive) {
        int[] fields = Grego.timeToFields(base, null);
        int year = fields[0];
        if (year > endYear) {
            return getFinalStart(prevRawOffset, prevDSTSavings);
        }
        Date d = getStartInYear(year, prevRawOffset, prevDSTSavings);
        if (d != null && (d.getTime() > base || (!inclusive && (d.getTime() == base)))) {
            d = getStartInYear(year - 1, prevRawOffset, prevDSTSavings);
        }
        return d;
    }

    /**
     * {@inheritDoc}
     * @stable ICU 3.8
     */
    @Override
    public boolean isEquivalentTo(TimeZoneRule other) {
        if (!(other instanceof AnnualTimeZoneRule)) {
            return false;
        }
        AnnualTimeZoneRule otherRule = (AnnualTimeZoneRule)other;
        if (startYear == otherRule.startYear
                && endYear == otherRule.endYear
                && dateTimeRule.equals(otherRule.dateTimeRule)) {
            return super.isEquivalentTo(other);
        }
        return false;
    }

    /**
     * {@inheritDoc}<br><br>
     * Note: This method in <code>AnnualTimeZoneRule</code> always returns true.
     * @stable ICU 3.8
     */
    @Override
    public boolean isTransitionRule() {
        return true;
    }

    /**
     * Returns a <code>String</code> representation of this <code>AnnualTimeZoneRule</code> object.
     * This method is used for debugging purpose only.  The string representation can be changed
     * in future version of ICU without any notice.
     *
     * @stable ICU 3.8
     */
    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(super.toString());
        buf.append(", rule={" + dateTimeRule + "}");
        buf.append(", startYear=" + startYear);
        buf.append(", endYear=");
        if (endYear == MAX_YEAR) {
            buf.append("max");
        } else {
            buf.append(endYear);
        }
        return buf.toString();
    }
}
