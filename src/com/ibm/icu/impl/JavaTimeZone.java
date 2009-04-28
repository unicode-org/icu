//##header
/*
 *******************************************************************************
 * Copyright (C) 2008-2009, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import java.util.TreeSet;

import com.ibm.icu.util.TimeZone;

/**
 * JavaTimeZone inherits com.ibm.icu.util.TimeZone and wraps java.util.TimeZone.
 * We used to have JDKTimeZone which wrapped Java TimeZone and used it as primary
 * TimeZone implementation until ICU4J 3.4.1.  This class works exactly like
 * JDKTimeZone and allows ICU users who use ICU4J and JDK date/time/calendar
 * services in mix to maintain only JDK timezone rules.
 *
 * This TimeZone subclass is returned by the TimeZone factory method getTimeZone(String)
 * when the default timezone type in TimeZone class is TimeZone.TIMEZONE_JDK.
 */
public class JavaTimeZone extends TimeZone {

    private static final long serialVersionUID = 6977448185543929364L;

    private static final TreeSet AVAILABLESET;

    private java.util.TimeZone javatz;
    private transient java.util.Calendar javacal;

    static {
        AVAILABLESET = new TreeSet();
        String[] availableIds = java.util.TimeZone.getAvailableIDs();
        for (int i = 0; i < availableIds.length; i++) {
            AVAILABLESET.add(availableIds[i]);
        }
    }

    /**
     * Constructs a JavaTimeZone with the default Java TimeZone
     */
    public JavaTimeZone() {
        javatz = java.util.TimeZone.getDefault();
        setID(javatz.getID());
        javacal = new java.util.GregorianCalendar(javatz);
    }

    /**
     * Constructs a JavaTimeZone with the given timezone ID.
     * @param id A timezone ID, either a system ID or a custom ID.
     */
    public JavaTimeZone(String id) {
        if (AVAILABLESET.contains(id)) {
            javatz = java.util.TimeZone.getTimeZone(id);
        }
        if (javatz == null) {
            // Use ICU's canonical ID mapping
            boolean[] isSystemID = new boolean[1];
            String canonicalID = TimeZone.getCanonicalID(id, isSystemID);
            if (isSystemID[0] && AVAILABLESET.contains(canonicalID)) {
                javatz = java.util.TimeZone.getTimeZone(canonicalID);
            }
        }

        if (javatz == null){
            int[] fields = new int[4];
            if (ZoneMeta.parseCustomID(id, fields)) {
                // JDK does not support offset seconds.
                // If custom ID, we create java.util.SimpleTimeZone here.
                id = ZoneMeta.formatCustomID(fields[1], fields[2], fields[3], fields[0] < 0);
                int offset = fields[0] * ((fields[1] * 60 + fields[2]) * 60 + fields[3]) * 1000;
                javatz = new java.util.SimpleTimeZone(offset, id);
            }
        }
        if (javatz == null) {
            // Final fallback
            id = "GMT";
            javatz = java.util.TimeZone.getTimeZone(id);
        }
        setID(id);
        javacal = new java.util.GregorianCalendar(javatz);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#getOffset(int, int, int, int, int, int)
     */
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return javatz.getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#getOffset(long, boolean, int[])
     */
    public void getOffset(long date, boolean local, int[] offsets) {
        synchronized (javacal) {
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
                javacal.clear();
                javacal.set(fields[0], fields[1], fields[2], hour, min, sec);
                javacal.set(java.util.Calendar.MILLISECOND, mil);

                int doy1, hour1, min1, sec1, mil1;
                doy1 = javacal.get(java.util.Calendar.DAY_OF_YEAR);
                hour1 = javacal.get(java.util.Calendar.HOUR_OF_DAY);
                min1 = javacal.get(java.util.Calendar.MINUTE);
                sec1 = javacal.get(java.util.Calendar.SECOND);
                mil1 = javacal.get(java.util.Calendar.MILLISECOND);

                if (fields[4] != doy1 || hour != hour1 || min != min1 || sec != sec1 || mil != mil1) {
                    // Calendar field(s) were changed due to the adjustment for non-existing time
                    // Note: This code does not support non-existing local time at year boundary properly.
                    // But, it should work fine for real timezones.
                    int dayDelta = Math.abs(doy1 - fields[4]) > 1 ? 1 : doy1 - fields[4];
                    int delta = ((((dayDelta * 24) + hour1 - hour) * 60 + min1 - min) * 60 + sec1 - sec) * 1000 + mil1 - mil;

                    // In this case, we use the offsets before the transition
//#if defined(FOUNDATION10) || defined(J2SE13)
//##                    javacal.setTime(new Date(javacal.getTime().getTime() - delta - 1));
//#else
                   javacal.setTimeInMillis(javacal.getTimeInMillis() - delta - 1);
//#endif
                }
            } else {
//#if defined(FOUNDATION10) || defined(J2SE13)
//##                javacal.setTime(new Date(date));
//#else
                javacal.setTimeInMillis(date);
//#endif
            }
            offsets[0] = javacal.get(java.util.Calendar.ZONE_OFFSET);
            offsets[1] = javacal.get(java.util.Calendar.DST_OFFSET);
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#getRawOffset()
     */
    public int getRawOffset() {
        return javatz.getRawOffset();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#inDaylightTime(java.util.Date)
     */
    public boolean inDaylightTime(Date date) {
        return javatz.inDaylightTime(date);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#setRawOffset(int)
     */
    public void setRawOffset(int offsetMillis) {
        javatz.setRawOffset(offsetMillis);
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#useDaylightTime()
     */
    public boolean useDaylightTime() {
        return javatz.useDaylightTime();
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#getDSTSavings()
     */
    public int getDSTSavings() {
        int dstSavings = super.getDSTSavings();
        try {
            // hack so test compiles and runs in both JDK 1.3 and JDK 1.4+
            final Object[] args = new Object[0];
            final Class[] argtypes = new Class[0];
            java.lang.reflect.Method m = javatz.getClass().getMethod("getDSTSavings", argtypes); 
            dstSavings = ((Integer) m.invoke(javatz, args)).intValue();
        } catch (Exception e) {
            // just use the result returned by super.getDSTSavings()
        }
        return dstSavings;
    }

    public java.util.TimeZone unwrap() {
        return javatz;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#clone()
     */
    public Object clone() {
        JavaTimeZone other = (JavaTimeZone)super.clone();
        other.javatz = (java.util.TimeZone)javatz.clone();
        return other;
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.util.TimeZone#hashCode()
     */
    public int hashCode() {
        return super.hashCode() + javatz.hashCode();
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        javacal = new java.util.GregorianCalendar(javatz);
    }

}
