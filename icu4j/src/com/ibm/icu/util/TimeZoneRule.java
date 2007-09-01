/*
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.io.Serializable;
import java.util.Date;

/**
 * <code>TimeZoneRule</code> is an abstract class representing a rule for time zone.
 * <code>TimeZoneRule</code> has a set of time zone attributes, such as zone name,
 * raw offset (UTC offset for standard time) and daylight saving time offset.
 * 
 * @see com.ibm.icu.util.TimeZoneTransition
 * @see com.ibm.icu.util.RuleBasedTimeZone
 * 
 * @draft ICU 3.8
 * @provisional This API might change or be removed in a future release.
 */
public abstract class TimeZoneRule implements Serializable {

    private final String name;
    private final int rawOffset;
    private final int dstSavings;

    /**
     * Constructs a <code>TimeZoneRule</code> with the name, the GMT offset of its
     * standard time and the amount of daylight saving offset adjustment.
     * 
     * @param name          The time zone name.
     * @param rawOffset     The UTC offset of its standard time in milliseconds.
     * @param dstSavings    The amount of daylight saving offset adjustment in milliseconds.
     *                      If this is a rule for standard time, the value of this argument is 0.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public TimeZoneRule(String name, int rawOffset, int dstSavings) {
        this.name = name;
        this.rawOffset = rawOffset;
        this.dstSavings = dstSavings;
    }

    /**
     * Gets the name of this time zone.
     * 
     * @return The name of this time zone.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the standard time offset.
     * 
     * @return The standard time offset from UTC in milliseconds.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public int getRawOffset() {
        return rawOffset;
    }

    /**
     * Gets the amount of daylight saving delta time from the standard time.
     * 
     * @return  The amount of daylight saving offset used by this rule
     *          in milliseconds.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public int getDSTSavings() {
        return dstSavings;
    }

    /**
     * Returns if this rule represents the same rule and offsets as another.
     * When two <code>TimeZoneRule</code> objects differ only its names, this method returns
     * true.
     *
     * @param other The <code>TimeZoneRule</code> object to be compared with.
     * @return true if the other <code>TimeZoneRule</code> is the same as this one.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public boolean isEquivalentTo(TimeZoneRule other) {
        if (rawOffset == other.rawOffset && dstSavings == other.dstSavings) {
            return true;
        }
        return false;
    }
 
    /**
     * Gets the very first time when this rule takes effect.
     * 
     * @param prevRawOffset     The standard time offset from UTC before this rule
     *                          takes effect in milliseconds.
     * @param prevDSTSavings    The amount of daylight saving offset from the
     *                          standard time. 
     * 
     * @return  The very first time when this rule takes effect.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public abstract Date getFirstStart(int prevRawOffset, int prevDSTSavings);

    /**
     * Gets the final time when this rule takes effect.
     * 
     * @param prevRawOffset     The standard time offset from UTC before this rule
     *                          takes effect in milliseconds.
     * @param prevDSTSavings    The amount of daylight saving offset from the
     *                          standard time. 
     * 
     * @return  The very last time when this rule takes effect,
     *          or null if this rule is applied for future dates infinitely.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public abstract Date getFinalStart(int prevRawOffset, int prevDSTSavings);

    /**
     * Gets the first time when this rule takes effect after the specified time.
     * 
     * @param base              The first time after this time is returned.
     * @param prevRawOffset     The standard time offset from UTC before this rule
     *                          takes effect in milliseconds.
     * @param prevDSTSavings    The amount of daylight saving offset from the
     *                          standard time. 
     * @param inclusive         Whether the base time is inclusive or not.
     * 
     * @return  The first time when this rule takes effect after the specified time,
     *          or null when this rule never takes effect after the specified time.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public abstract Date getNextStart(long base, int prevRawOffset, int prevDSTSavings, boolean inclusive);

    /**
     * Gets the most recent time when this rule takes effect before the specified time.
     * 
     * @param base              The most recent time when this rule takes effect before
     *                          this time is returned.
     * @param prevRawOffset     The standard time offset from UTC before this rule
     *                          takes effect in milliseconds.
     * @param prevDSTSavings    The amount of daylight saving offset from the
     *                          standard time. 
     * @param inclusive         Whether the base time is inclusive or not.
     * 
     * @return  The most recent time when this rule takes effect before the specified time,
     *          or null when this rule never takes effect before the specified time.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public abstract Date getPreviousStart(long base, int prevRawOffset, int prevDSTSavings, boolean inclusive);

    /**
     * Returns if this <code>TimeZoneRule</code> has one or more start times.
     * 
     * @return true if this <TimeZoneRule</code> has one or more start times.
     * 
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public abstract boolean isTransitionRule();

    /**
     * Returns a <code>String</code> representation of this <code>TimeZoneRule</code> object.
     * This method is used for debugging purpose only.  The string representation can be changed
     * in future version of ICU without any notice.
     * @draft ICU 3.8
     * @provisional This API might change or be removed in a future release.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("name=" + name);
        buf.append(", stdOffset=" + rawOffset);
        buf.append(", dstSaving=" + dstSavings);
        return buf.toString();
    }
}
