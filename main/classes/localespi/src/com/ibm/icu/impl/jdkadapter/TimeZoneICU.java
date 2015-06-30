/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.impl.icuadapter.TimeZoneJDK;
import com.ibm.icu.util.TimeZone;

/**
 * TimeZoneICU is an adapter class which wraps ICU4J TimeZone and
 * implements java.util.TimeZone APIs.
 */
public class TimeZoneICU extends java.util.TimeZone {

    private static final long serialVersionUID = 6019030618408620277L;

    private TimeZone fIcuTz;

    private TimeZoneICU(TimeZone icuTz) {
        fIcuTz = icuTz;
    }

    public static java.util.TimeZone wrap(TimeZone icuTz) {
        if (icuTz instanceof TimeZoneJDK) {
            return ((TimeZoneJDK)icuTz).unwrap();
        }
        return new TimeZoneICU(icuTz);
    }

    public TimeZone unwrap() {
        return fIcuTz;
    }

    @Override
    public Object clone() {
        TimeZoneICU other = (TimeZoneICU)super.clone();
        other.fIcuTz = (TimeZone)fIcuTz.clone();
        return other;
    }

    //public String getDisplayName()
    //public String getDisplayName(boolean daylight, int style)
    //public String getDisplayName(Locale locale)

    @Override
    public String getDisplayName(boolean daylight, int style, Locale locale) {
        return fIcuTz.getDisplayName(daylight, style, locale);
    }

    @Override
    public int getDSTSavings() {
        return fIcuTz.getDSTSavings();
    }

    @Override
    public String getID() {
        return fIcuTz.getID();
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return fIcuTz.getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }

    @Override
    public int getOffset(long date) {
        return fIcuTz.getOffset(date);
    }

    @Override
    public int getRawOffset() {
        return fIcuTz.getRawOffset();
    }

    @Override
    public boolean hasSameRules(java.util.TimeZone other) {
        return other.hasSameRules(TimeZoneICU.wrap(fIcuTz));
    }

    @Override
    public boolean inDaylightTime(Date date) {
        return fIcuTz.inDaylightTime(date);
    }

    @Override
    public void setID(String ID) {
        fIcuTz.setID(ID);
    }

    @Override
    public void setRawOffset(int offsetMillis) {
        fIcuTz.setRawOffset(offsetMillis);
    }

    @Override
    public boolean useDaylightTime() {
        return fIcuTz.useDaylightTime();
    }
}
