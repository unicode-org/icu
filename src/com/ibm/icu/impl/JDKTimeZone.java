/*
**********************************************************************
* Copyright (c) 2003-2004, International Business Machines
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
import java.io.IOException;

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
    transient java.util.TimeZone zone;

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
        // The following code works only on 1.4 or later.  We no longer support JDK 1.3.
        try {
            if (zone instanceof sun.util.calendar.ZoneInfo) {
                ((sun.util.calendar.ZoneInfo) zone).getOffsets(date, offsets);
                if (local) {
                    date -= offsets[0] + offsets[1];
                    ((sun.util.calendar.ZoneInfo) zone).getOffsets(date, offsets);
                }
                return;
            } 
        }
        catch (SecurityException ex) {
            // ok; fall through, we're running in a protected context
        } 
        catch (Throwable th) {
            // System.out.println("caught: " + th);
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
     * Returns the amount of time in ms that the clock is advanced during DST.
     * @return the number of milliseconds the time is
     * advanced with respect to standard time when the daylight savings rules
     * are in effect. A positive number, typically one hour (3600000).
     * @stable ICU 2.0
     */
    public int getDSTSavings() {
        if (useDaylightTime()) {
            try {   
                // This is only to make a 1.3 compiler happy.  JDKTimeZone
                // is only used in JDK 1.4, where TimeZone has the getDSTSavings
                // API on it, so a straight call to getDSTSavings would actually
                // work if we could compile it.  Since on 1.4 the time zone is
                // not a SimpleTimeZone, we can't downcast in order to make
                // the direct call that a 1.3 compiler would like, because at
                // runtime the downcast would fail.
                // todo: remove when we no longer support compiling under 1.3

                // The following works if getDSTSavings is declared in   
                // TimeZone (JDK 1.4) or SimpleTimeZone (JDK 1.3).   
                final Object[] args = new Object[0];
                final Class[] argtypes = new Class[0];
                Method m = zone.getClass().getMethod("getDSTSavings", argtypes); 
                return ((Integer) m.invoke(zone, args)).intValue();   
            } catch (Exception e) {
                // if zone is in the sun.foo class hierarchy and we
                // are in a protection domain, we'll get a security
                // exception.  And if we claim to support DST, but 
                // return a value of 0, later java.util.SimpleTimeZone will
                // throw an illegalargument exception.  so... fake
                // the dstoffset;
                return 3600000;
            }   
        }
        return 0;
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

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeObject(zone.getID());
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        String id = (String)in.readObject();
        zone = java.util.TimeZone.getTimeZone(id);
    }
}

//eof
