/*
**********************************************************************
* Copyright (c) 2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: October 2 2003
* Since: ICU 2.8
**********************************************************************
*/
package com.ibm.icu.impl;
import com.ibm.icu.util.TimeZone;
import java.util.Date;

/**
 * A wrapper around a java.util.TimeZone object.  This is a concrete
 * instance of TimeZone.  All TimeZones that wrap a java.util.TimeZone
 * should inherit from this class.  TimeZones that do not wrap a
 * java.util.TimeZone inherit directly from TimeZone.
 *
 * The complement of this is TimeZoneAdapter, which makes a
 * com.ibm.icu.util.TimeZone look like a java.util.TimeZone.
 *
 * @see com.ibm.icu.impl.JDKTimeZone
 * @author Alan Liu
 * @since ICU 2.8
 */
public class JDKTimeZone extends TimeZone {

    /**
     * Constructs a JDKTimeZone given a java.util.TimeZone reference
     * which must not be null.
     * @param tz the time zone to wrap
     */
    public JDKTimeZone(java.util.TimeZone tz) {
        super(tz);
        if (tz == null) {
            throw new NullPointerException();
        }
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public int getOffset(int era, int year, int month, int day,
                         int dayOfWeek, int milliseconds) {
        return getJDKZone().getOffset(era, year, month, day,
                                      dayOfWeek, milliseconds);
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public void setRawOffset(int offsetMillis) {
        getJDKZone().setRawOffset(offsetMillis);
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public int getRawOffset() {
        return getJDKZone().getRawOffset();
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public boolean useDaylightTime() {
        return getJDKZone().useDaylightTime();
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public boolean inDaylightTime(Date date) {
        return getJDKZone().inDaylightTime(date);
    }

    /**
     * Boilerplate API; calls through to wrapped object.
     */
    public synchronized int hashCode() {
        return getJDKZone().hashCode();
    }

    /**
     * Boilerplate API; calls through to wrapped object.
     */
    public boolean equals(Object obj) {
        try {
            return obj != null &&
                getJDKZone().equals(((JDKTimeZone) obj).getJDKZone());
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns a string representation of this object.
     * @return  a string representation of this object.
     */
    public String toString() {
        return "JDKTimeZone: " + getJDKZone().toString();
    }
}

//eof
