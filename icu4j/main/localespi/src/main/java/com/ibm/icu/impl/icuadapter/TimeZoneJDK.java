// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2014, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.icuadapter;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import com.ibm.icu.impl.Grego;
import com.ibm.icu.impl.jdkadapter.TimeZoneICU;
import com.ibm.icu.util.ULocale;

/**
 * TimeZoneJDK is an adapter class which wraps java.util.TimeZone and
 * implements ICU4J TimeZone APIs.
 */
public class TimeZoneJDK extends com.ibm.icu.util.TimeZone {

    private static final long serialVersionUID = -1137052823551791933L;

    private TimeZone fJdkTz;
    private transient Calendar fJdkCal;

    private TimeZoneJDK(TimeZone jdkTz) {
        fJdkTz = (TimeZone)jdkTz.clone();
    }

    public static com.ibm.icu.util.TimeZone wrap(TimeZone jdkTz) {
        if (jdkTz instanceof TimeZoneICU) {
            return ((TimeZoneICU)jdkTz).unwrap();
        }
        return new TimeZoneJDK(jdkTz);
    }

    public TimeZone unwrap() {
        return (TimeZone)fJdkTz.clone();
    }

    @Override
    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TimeZoneJDK) {
            return (((TimeZoneJDK)obj).fJdkTz).equals(fJdkTz);
        }
        return false;
    }

    //public String getDisplayName()
    //public String getDisplayName(boolean daylight, int style)
    //public String getDisplayName(Locale locale)
    //public String getDisplayName(ULocale locale)
    @Override
    public String getDisplayName(boolean daylight, int style, Locale locale) {
        return fJdkTz.getDisplayName(daylight, style, locale);
    }

    @Override
    public String getDisplayName(boolean daylight, int style, ULocale locale) {
        return fJdkTz.getDisplayName(daylight, style, locale.toLocale());
    }

    @Override
    public int getDSTSavings() {
        return fJdkTz.getDSTSavings();
    }

    @Override
    public String getID() {
        return fJdkTz.getID();
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return fJdkTz.getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }

    @Override
    public int getOffset(long date) {
        return fJdkTz.getOffset(date);
    }

    @Override
    public void getOffset(long date, boolean local, int[] offsets) {
        synchronized(this) {
            if (fJdkCal == null) {
                fJdkCal = new GregorianCalendar(fJdkTz);
            }
            if (local) {
                int fields[] = new int[6];
                Grego.timeToFields(date, fields);
                int hour, min, sec, mil;
                int tmp = fields[5];
                mil = tmp % 1000;
                tmp /= 1000;
                sec = tmp % 60;
                tmp /= 60;
                min = tmp % 60;
                hour = tmp / 60;
                fJdkCal.clear();
                fJdkCal.set(fields[0], fields[1], fields[2], hour, min, sec);
                fJdkCal.set(java.util.Calendar.MILLISECOND, mil);

                int doy1, hour1, min1, sec1, mil1;
                doy1 = fJdkCal.get(java.util.Calendar.DAY_OF_YEAR);
                hour1 = fJdkCal.get(java.util.Calendar.HOUR_OF_DAY);
                min1 = fJdkCal.get(java.util.Calendar.MINUTE);
                sec1 = fJdkCal.get(java.util.Calendar.SECOND);
                mil1 = fJdkCal.get(java.util.Calendar.MILLISECOND);

                if (fields[4] != doy1 || hour != hour1 || min != min1 || sec != sec1 || mil != mil1) {
                    // Calendar field(s) were changed due to the adjustment for non-existing time
                    // Note: This code does not support non-existing local time at year boundary properly.
                    // But, it should work fine for real timezones.
                    int dayDelta = Math.abs(doy1 - fields[4]) > 1 ? 1 : doy1 - fields[4];
                    int delta = ((((dayDelta * 24) + hour1 - hour) * 60 + min1 - min) * 60 + sec1 - sec) * 1000 + mil1 - mil;

                    // In this case, we use the offsets before the transition
                    fJdkCal.setTimeInMillis(fJdkCal.getTimeInMillis() - delta - 1);
                }
            } else {
                fJdkCal.setTimeInMillis(date);
            }
            offsets[0] = fJdkCal.get(java.util.Calendar.ZONE_OFFSET);
            offsets[1] = fJdkCal.get(java.util.Calendar.DST_OFFSET);
        }
    }

    @Override
    public int getRawOffset() {
        return fJdkTz.getRawOffset();
    }

    @Override
    public int hashCode() {
        return fJdkTz.hashCode();
    }

    @Override
    public boolean hasSameRules(com.ibm.icu.util.TimeZone other) {
        return other.hasSameRules(TimeZoneJDK.wrap(fJdkTz));
    }

    @Override
    public boolean inDaylightTime(Date date) {
        return fJdkTz.inDaylightTime(date);
    }

    @Override
    public void setID(String ID) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen TimeZoneJDK instance.");
        }
        fJdkTz.setID(ID);
    }

    @Override
    public void setRawOffset(int offsetMillis) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen TimeZoneJDK instance.");
        }
        fJdkTz.setRawOffset(offsetMillis);
    }

    @Override
    public boolean useDaylightTime() {
        return fJdkTz.useDaylightTime();
    }

    @Override
    public boolean observesDaylightTime() {
        return fJdkTz.observesDaylightTime();
    }

    // Freezable stuffs
    private volatile transient boolean fIsFrozen = false;

    @Override
    public boolean isFrozen() {
        return fIsFrozen;
    }

    @Override
    public com.ibm.icu.util.TimeZone freeze() {
        fIsFrozen = true;
        return this;
    }

    @Override
    public com.ibm.icu.util.TimeZone cloneAsThawed() {
        TimeZoneJDK tz = (TimeZoneJDK)super.cloneAsThawed();
        tz.fJdkTz = (TimeZone)fJdkTz.clone();
        tz.fJdkCal = null;  // To be instantiated when necessary
        tz.fIsFrozen = false;
        return tz;
    }

}
