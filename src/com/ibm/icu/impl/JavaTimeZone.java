//##header J2SE15
/*
 *******************************************************************************
 * Copyright (C) 2008-2012, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.ParsePosition;
import java.util.Date;
import java.util.TreeSet;

import com.ibm.icu.text.NumberFormat;
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
            if (id.equals("Etc/Unknown")) {
                // Special CLDR ID
                javatz = java.util.TimeZone.getTimeZone("GMT");
                javatz.setID("Etc/Unknown");
                
            } else {
                String canonicalID = ZoneMeta.getOlsonCanonicalID(id);
                if (canonicalID != null && AVAILABLESET.contains(canonicalID)) {
                    javatz = java.util.TimeZone.getTimeZone(canonicalID);
                }
            }
        }

        if (javatz == null){
            int[] fields = new int[4];
            if (parseCustomID(id, fields)) {
                // JDK does not support offset seconds.
                // If custom ID, we create java.util.SimpleTimeZone here.
                id = formatCustomID(fields[1], fields[2], fields[3], fields[0] < 0);
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


    private static final String kGMT_ID   = "GMT";
    private static final String kCUSTOM_TZ_PREFIX = "GMT";

    // Maximum value of valid custom time zone hour/min
    private static final int kMAX_CUSTOM_HOUR = 23;
    private static final int kMAX_CUSTOM_MIN = 59;
    private static final int kMAX_CUSTOM_SEC = 59;

    /*
     * Parses the given custom time zone identifier
     * @param id id A string of the form GMT[+-]hh:mm, GMT[+-]hhmm, or
     * GMT[+-]hh.
     * @param fields An array of int (length = 4) to receive the parsed
     * offset time fields.  The sign is set to fields[0] (-1 or 1),
     * hour is set to fields[1], minute is set to fields[2] and second is
     * set to fields[3].
     * @return Returns true when the given custom id is valid.
     */
    static boolean parseCustomID(String id, int[] fields) {
        NumberFormat numberFormat = null;
        String idUppercase = id.toUpperCase();

        if (id != null && id.length() > kGMT_ID.length() &&
            idUppercase.startsWith(kGMT_ID)) {
            ParsePosition pos = new ParsePosition(kGMT_ID.length());
            int sign = 1;
            int hour = 0;
            int min = 0;
            int sec = 0;

            if (id.charAt(pos.getIndex()) == 0x002D /*'-'*/) {
                sign = -1;
            } else if (id.charAt(pos.getIndex()) != 0x002B /*'+'*/) {
                return false;
            }
            pos.setIndex(pos.getIndex() + 1);

            numberFormat = NumberFormat.getInstance();
            numberFormat.setParseIntegerOnly(true);

            // Look for either hh:mm, hhmm, or hh
            int start = pos.getIndex();

            Number n = numberFormat.parse(id, pos);
            if (pos.getIndex() == start) {
                return false;
            }
            hour = n.intValue();

            if (pos.getIndex() < id.length()){
                if (pos.getIndex() - start > 2
                        || id.charAt(pos.getIndex()) != 0x003A /*':'*/) {
                    return false;
                }
                // hh:mm
                pos.setIndex(pos.getIndex() + 1);
                int oldPos = pos.getIndex();
                n = numberFormat.parse(id, pos);
                if ((pos.getIndex() - oldPos) != 2) {
                    // must be 2 digits
                    return false;
                }
                min = n.intValue();
                if (pos.getIndex() < id.length()) {
                    if (id.charAt(pos.getIndex()) != 0x003A /*':'*/) {
                        return false;
                    }
                    // [:ss]
                    pos.setIndex(pos.getIndex() + 1);
                    oldPos = pos.getIndex();
                    n = numberFormat.parse(id, pos);
                    if (pos.getIndex() != id.length()
                            || (pos.getIndex() - oldPos) != 2) {
                        return false;
                    }
                    sec = n.intValue();
                }
            } else {
                // Supported formats are below -
                //
                // HHmmss
                // Hmmss
                // HHmm
                // Hmm
                // HH
                // H

                int length = pos.getIndex() - start;
                if (length <= 0 || 6 < length) {
                    // invalid length
                    return false;
                }
                switch (length) {
                    case 1:
                    case 2:
                        // already set to hour
                        break;
                    case 3:
                    case 4:
                        min = hour % 100;
                        hour /= 100;
                        break;
                    case 5:
                    case 6:
                        sec = hour % 100;
                        min = (hour/100) % 100;
                        hour /= 10000;
                        break;
                }
            }

            if (hour <= kMAX_CUSTOM_HOUR && min <= kMAX_CUSTOM_MIN && sec <= kMAX_CUSTOM_SEC) {
                if (fields != null) {
                    if (fields.length >= 1) {
                        fields[0] = sign;
                    }
                    if (fields.length >= 2) {
                        fields[1] = hour;
                    }
                    if (fields.length >= 3) {
                        fields[2] = min;
                    }
                    if (fields.length >= 4) {
                        fields[3] = sec;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /*
     * Returns the normalized custom TimeZone ID
     */
    static String formatCustomID(int hour, int min, int sec, boolean negative) {
        // Create normalized time zone ID - GMT[+|-]hhmm[ss]
        StringBuffer zid = new StringBuffer(kCUSTOM_TZ_PREFIX);
        if (hour != 0 || min != 0) {
            if(negative) {
                zid.append('-');
            } else {
                zid.append('+');
            }
            // Always use US-ASCII digits
            if (hour < 10) {
                zid.append('0');
            }
            zid.append(hour);
            if (min < 10) {
                zid.append('0');
            }
            zid.append(min);

            if (sec != 0) {
                // Optional second field
                if (sec < 10) {
                    zid.append('0');
                }
                zid.append(sec);
            }
        }
        return zid.toString();
    }
}
