/* Copyright (c) 2000 International Business Machines Corporation and
 * others. All Rights Reserved.
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/Attic/SimpleTimeZoneAdapter.java,v $ 
 * $Date: 2002/12/05 01:25:49 $ 
 * $Revision: 1.4 $
 */
package com.ibm.icu.util;
import java.util.Date;

/**
 * <code>SimpleTimeZoneAdapter</code> wraps a
 * com.ibm.icu.util.SimpleTimeZone and inherits from java.util.TimeZone.
 * Without this class, we would need to 'port' java.util.Date to
 * com.ibm.icu.util as well, so that Date could interoperate properly with
 * the com.ibm.icu.util TimeZone and Calendar classes.  With this class,
 * we can (mostly) use java.util.Date together with com.ibm.icu.util
 * classes.
 *
 * <p>This solution is imperfect because of the faulty design of
 * java.util.TimeZone.  Specifically, TZ contains a package private
 * method, getOffset(), that should really be public.  Because it is
 * package private, it cannot be overridden from where we are, and we
 * cannot properly delegate its operation to our contained
 * com.ibm.icu.util.STZ object.
 *
 * <p>For the moment we live with this problem.  It appear not to
 * cause too much trouble since most real computations happen using
 * the com.ibm.icu.util classes.  However, if this becomes a problem in
 * the future, we will have to stop using this adapter, and 'port'
 * java.util.Date into com.ibm.icu.util.
 *
 * @see com.ibm.icu.util.TimeZone#setDefault
 * @author Alan Liu
 * @stable ICU 2.0
 */
public class SimpleTimeZoneAdapter extends java.util.TimeZone {

    /**
     * The contained com.ibm.icu.util.SimpleTimeZone object.
     * We delegate all methods to this object.
     */
    private SimpleTimeZone zone;
    
    /**
     * Constructs an adapter for a SimpleTimeZone object.
     * @stable ICU 2.0
     */
    public SimpleTimeZoneAdapter(SimpleTimeZone zone) {
        this.zone = zone;
    }

    /**
     * Override TimeZone
     * @stable ICU 2.0
     */
    public String getID() {
        return zone.getID();
    }

    /**
     * Override TimeZone
     * @stable ICU 2.0
     */
    public void setID(String ID) {
        zone.setID(ID);
    }    

    /**
     * Override TimeZone
     * @stable ICU 2.0
     */
    public boolean hasSameRules(java.util.TimeZone other) {
        return other instanceof SimpleTimeZoneAdapter &&
            zone.hasSameRules(((SimpleTimeZoneAdapter)other).zone);
    }

    /**
     * Override TimeZone
     * @stable ICU 2.0
     */
    public int getOffset(int era, int year, int month, int day, int dayOfWeek,
                         int millis) {
        return zone.getOffset(era, year, month, day, dayOfWeek, millis);
    }

    // This doesn't work! Because this is a package-private method,
    // it cannot override the corresponding method in java.util.TZ.
    // This reflects a fundamental bug in the architecture of
    // java.util.TZ.  If not for this, this adapter class would
    // work flawlessly. - liu
//!    /**
//!     * Override TimeZone
//!     */
//!    int getOffset(int era, int year, int month, int day, int dayOfWeek,
//!                  int millis, int monthLength, int prevMonthLength) {
//!        return zone.getOffset(era, year, month, day, dayOfWeek,
//!                              millis, monthLength, prevMonthLength);
//!    }

    /**
     * Overrides TimeZone
     * Gets the GMT offset for this time zone.
     * @stable ICU 2.0
     */
    public int getRawOffset() {
        return zone.getRawOffset();
    }

    /**
     * Overrides TimeZone
     * @stable ICU 2.0
     */
    public void setRawOffset(int offsetMillis) {
        zone.setRawOffset(offsetMillis);
    }

    /**
     * Overrides TimeZone
     * @stable ICU 2.0
     */
    public boolean useDaylightTime() {
        return zone.useDaylightTime();
    }

    /**
     * Overrides TimeZone
     * @stable ICU 2.0
     */
    public boolean inDaylightTime(Date date) {
        return zone.inDaylightTime(date);
    }

    /**
     * Overrides Cloneable
     * @stable ICU 2.0
     */
    public Object clone() {
        return new SimpleTimeZoneAdapter((SimpleTimeZone)zone.clone());
    }

    /**
     * Override hashCode.
     * @stable ICU 2.0
     */
    public synchronized int hashCode() {
        return zone.hashCode();
    }

    /**
     * Compares the equality of two SimpleTimeZone objects.
     *
     * @param obj  The SimpleTimeZone object to be compared with.
     * @return     True if the given obj is the same as this SimpleTimeZone
     *             object; false otherwise.
     * @stable ICU 2.0
     */
    public boolean equals(Object obj) {
        if (obj instanceof SimpleTimeZoneAdapter) {
            obj = ((SimpleTimeZoneAdapter)obj).zone;
        }
        return zone.equals(obj);
    }

    /**
     * Return a string representation of this time zone.
     * @return  a string representation of this time zone.
     * @stable ICU 2.0
     */
    public String toString() {
        // Should probably show our class name here...fix later.
        return zone.toString();
    }
}
