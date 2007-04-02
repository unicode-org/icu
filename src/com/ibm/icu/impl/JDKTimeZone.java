/*
**********************************************************************
* Copyright (c) 2003-2006, International Business Machines
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
import java.io.IOException;

/**
 * Wrapper around OlsonTimeZone object. Due to serialziation constraints
 * SimpleTimeZone cannot be a subclass of OlsonTimeZone.
 * 
 * The complement of this is TimeZoneAdapter, which makes a
 * com.ibm.icu.util.TimeZone look like a java.util.TimeZone.
 *
 * @see com.ibm.icu.impl.JDKTimeZone
 * @author Alan Liu
 * @since ICU 2.8
 */
public class JDKTimeZone extends TimeZone {

    private static final long serialVersionUID = -3724907649889455280L;
    
    /**
     * The java.util.TimeZone wrapped by this object.  Must not be null.
     */
    // give access to SimpleTimeZone
    protected transient OlsonTimeZone zone;
    /*
     * Given a java.util.TimeZone, wrap it in the appropriate adapter
     * subclass of com.ibm.icu.util.TimeZone and return the adapter.
     */
//    public static TimeZone wrap(java.util.TimeZone tz) {
//        if (tz instanceof TimeZoneAdapter) {
//            return ((TimeZoneAdapter) tz).unwrap();
//        }
//        if (tz instanceof java.util.SimpleTimeZone) {
//            return new SimpleTimeZone((java.util.SimpleTimeZone) tz, tz.getID());
//        }
//        return new JDKTimeZone(tz);
//    }


    /**
     * Constructs a JDKTimeZone given a java.util.TimeZone reference
     * which must not be null.
     * @param tz the time zone to wrap
     */
    public JDKTimeZone(java.util.TimeZone tz) {
        String id = tz.getID();
        try{
            zone = new OlsonTimeZone(id);
        }catch(Exception ex){
            // throw away exception
        }
        super.setID(id);
    }
    protected JDKTimeZone(OlsonTimeZone tz) {
        zone = tz;
        super.setID(zone.getID());
    }
    /**
     * Default constructor
     */
    protected JDKTimeZone() {
    }
    /**
     * Sets the time zone ID. This does not change any other data in
     * the time zone object.
     * @param ID the new time zone ID.
     */
    public void setID(String ID) {
        super.setID(ID);
        if(zone!=null){
            zone.setID(ID);
        }
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public int getOffset(int era, int year, int month, int day,
                         int dayOfWeek, int milliseconds) {

        if(zone!=null){
            return zone.getOffset(era, year, month, day,
                                      dayOfWeek, milliseconds);
        }
        // should never occur except 
        // when old object of older version of JDKTimeZone are de-serialized.
        // these objects may contain ids that OlsonTimeZone may not understand 
        // in such cases zone will be null
        return 0;
    }


    public void getOffset(long date, boolean local, int[] offsets) {

        if(zone!=null){
            zone.getOffset(date, local, offsets);
        }else{
            super.getOffset(date, local, offsets);
        }
    }
 
    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public void setRawOffset(int offsetMillis) {
        if(zone!=null){
            zone.setRawOffset(offsetMillis);
        }
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public int getRawOffset() {
        if(zone!=null){
            return zone.getRawOffset();
        }
        // should never occur except 
        // when old object of older version of JDKTimeZone are de-serialized.
        // these objects may contain ids that OlsonTimeZone may not understand 
        // in such cases zone will be null
        return 0;
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public boolean useDaylightTime() {
        if(zone!=null){
            return zone.useDaylightTime();
        }
        // should never occur except 
        // when old object of older version of JDKTimeZone are de-serialized.
        // these objects may contain ids that OlsonTimeZone may not understand 
        // in such cases zone will be null
        return false;
    }

    /**
     * TimeZone API; calls through to wrapped time zone.
     */
    public boolean inDaylightTime(Date date) {
        if(zone!=null){
            return zone.inDaylightTime(date);
        }
        // should never occur except 
        // when old object of older version of JDKTimeZone are de-serialized.
        // these objects may contain ids that OlsonTimeZone may not understand 
        // in such cases zone will be null
        return false;
    }

    /**
     * TimeZone API.
     */
    public boolean hasSameRules(TimeZone other) {
        if (other == null) {
            return false;
        }
        if (other instanceof JDKTimeZone) {
            if(zone!=null){
                return zone.hasSameRules(((JDKTimeZone) other).zone);
            }
        }
        return super.hasSameRules(other);
    }

    /**
     * Boilerplate API; calls through to wrapped object.
     */
    public Object clone() {
        JDKTimeZone clone = new JDKTimeZone();
        if(zone!=null){
            clone.zone = (OlsonTimeZone)zone.clone();
        }
        return clone;
    }

    /**
     * Boilerplate API; calls through to wrapped object.
     */
    public synchronized int hashCode() {
        if(zone!=null){
            return zone.hashCode();
        }
        return super.hashCode();
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
            if(zone!=null){
                return zone.getDSTSavings();
            }
			return 3600000;
		}
        return 0;
    }

    /**
     * Boilerplate API; calls through to wrapped object.
     */
    public boolean equals(Object obj) {
        try {
            if(obj !=null){
                TimeZone tz1 = zone;
                TimeZone tz2 = ((JDKTimeZone) obj).zone;
                boolean equal = true;
                if(tz1!=null && tz2!=null){
                    equal = tz1.equals(tz2);
                }
                return equal;
            }
            return false;
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Returns a string representation of this object.
     * @return  a string representation of this object.
     */
    public String toString() {
        return "JDKTimeZone: " + zone.toString();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        if(zone!=null){
            out.writeObject(zone.getID());
        }else{
            out.writeObject(getID());
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        String id = (String)in.readObject();

        // create the TimeZone object if reading the old version of object
        try{
            zone = new OlsonTimeZone(id);
        }catch(Exception ex){
            //throw away exception
        }							 
        setID(id);
    }
}

//eof
