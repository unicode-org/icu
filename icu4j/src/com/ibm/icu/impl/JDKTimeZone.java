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
import com.ibm.icu.util.SimpleTimeZone;
import java.util.Date;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

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
     * The java.util.TimeZone wrapped by this object.  Must not be null.
     */
    java.util.TimeZone zone;

    /**
     * Given a java.util.TimeZone, wrap it in the appropriate adapter
     * subclass of com.ibm.icu.util.TimeZone and return the adapter.
     */
    public static TimeZone wrap(java.util.TimeZone tz) {
        if (tz instanceof TimeZoneAdapter) {
            return ((TimeZoneAdapter) tz).unwrap();
        }
        if (tz instanceof java.util.SimpleTimeZone) {
            return new SimpleTimeZone((java.util.SimpleTimeZone) tz);
        }
        return new JDKTimeZone(tz);
    }

    /**
     * Return the java.util.TimeZone wrapped by this object.
     */
    public java.util.TimeZone unwrap() {
        return zone;
    }

    /**
     * Constructs a JDKTimeZone given a java.util.TimeZone reference
     * which must not be null.
     * @param tz the time zone to wrap
     */
    protected JDKTimeZone(java.util.TimeZone tz) {
        zone = tz;
        super.setID(zone.getID());
    }

    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @param ID the new time zone ID.
     */
    public void setID(String ID) {
        super.setID(ID);
        zone.setID(ID);
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public int getOffset(int era, int year, int month, int day,
                         int dayOfWeek, int milliseconds) {
        return unwrap().getOffset(era, year, month, day,
                                  dayOfWeek, milliseconds);
    }

    /**
     * Override TimeZone to handle wrapped ZoneInfo.
     */
    public void getOffset(long date, boolean local, int[] offsets) {
        // The following code works on 1.4 or later.  Since we need to
        // be compatible with 1.3, we have to use reflection.
        //| if (zone instanceof ZoneInfo) {
        //|     ((ZoneInfo) zone).getOffsets(date, offsets);
        //|     if (local) {
        //|         date -= offsets[0] + offsets[1];
        //|         ((ZoneInfo) zone).getOffsets(date, offsets);
        //|     }
        //| } else {
        //|     super.getOffset(date, local, offsets);
        //| }

        try {
            Class zoneInfo = Class.forName("sun.util.calendar.ZoneInfo");
            if (zoneInfo.isInstance(zone)) {
                Method getOffsets = zoneInfo.getMethod("getOffsets",
                       new Class[] { Long.TYPE, Class.forName("[I") });
                Object[] args = new Object[] { new Long(date), offsets };
                getOffsets.invoke(zone, args);
                if (local) {
                    date -= offsets[0] + offsets[1];
                    args[0] = new Long(date);
                    getOffsets.invoke(zone, args);
                }
                return;
            }
        } catch (ClassNotFoundException e1) {
            // ok; fall through
        } catch (SecurityException ex) {
            // ok; fall through, we're running in a protected context
        } catch (NoSuchMethodException e2) {
            throw new RuntimeException(); // should not occur
        } catch (IllegalAccessException e3) {
            throw new RuntimeException(); // should not occur
        } catch (InvocationTargetException e4) {
            throw new RuntimeException(); // should not occur
        }
        super.getOffset(date, local, offsets);
    }
 
    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public void setRawOffset(int offsetMillis) {
        unwrap().setRawOffset(offsetMillis);
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public int getRawOffset() {
        return unwrap().getRawOffset();
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public boolean useDaylightTime() {
        return unwrap().useDaylightTime();
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public boolean inDaylightTime(Date date) {
        return unwrap().inDaylightTime(date);
    }

    /**
     * TimeZone API.
     */
    public boolean hasSameRules(TimeZone other) {
        if (other == null) {
            return false;
        }
        if (other instanceof JDKTimeZone) {
            return zone.hasSameRules(((JDKTimeZone) other).zone);
        }
        return super.hasSameRules(other);
    }

    /**
     * Boilerplate API; calls through to wrapped object.
     */
    public Object clone() {
        return wrap((java.util.TimeZone)zone.clone());
    }

    /**
     * Boilerplate API; calls through to wrapped object.
     */
    public synchronized int hashCode() {
        return unwrap().hashCode();
    }

    /**
     * Boilerplate API; calls through to wrapped object.
     */
    public boolean equals(Object obj) {
        try {
            return obj != null &&
                unwrap().equals(((JDKTimeZone) obj).unwrap());
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns a string representation of this object.
     * @return  a string representation of this object.
     */
    public String toString() {
        return "JDKTimeZone: " + unwrap().toString();
    }
}

//eof
