/*
******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/ULocale.java,v $
* $Date: 2003/12/01 21:23:07 $
* $Revision: 1.7 $
*
******************************************************************************
*/

package com.ibm.icu.util;

import java.util.Locale;
import java.io.Serializable;
import java.io.IOException;

/**
 * A class for replacing the java.util.Locale. This class provides all the 
 * functionality that java.util.Locale has and in ICU 3.0 will be enhanced for 
 * supporting RFC 3066 language identifiers.
 * @author weiv
 * @draft ICU 2.8
 */
public final class ULocale implements Serializable {
    private transient Locale locale;
    private String locName;

    /** 
     * Actual locale where data is coming from 
     * Actual locale will make sense only after the alternate
     * ICU data handling framework is implemented in ICU 3.0
     * @draft ICU 2.8
     */
    public static final ULocaleDataType ACTUAL_LOCALE = new ULocaleDataType(0);
 
    /** 
     * Valid locale for an object 
     * @draft ICU 2.8
     */ 
    public static final ULocaleDataType VALID_LOCALE = new ULocaleDataType(1);
    
    /**
     * Type safe enum for representing the type of locale
     * @draft ICU 2.8
     */
    public static final class ULocaleDataType{
    
        private int localeType;
        
        private ULocaleDataType(int id){
            localeType = id;
        }
        private boolean equals(int id){
            return localeType == id;
        }
    }
        
    /**
     * Convert this ULocale object to java.util.Locale object
     * @return Locale object that represents the information in this object
     * @draft ICU 2.8
     */
    public Locale toLocale() {
        return locale;
    }
    
    /**
     * Construct a ULocale object from java.util.Locale object.
     * @param loc The locale object to be converted
     * @draft ICU 2.8
     */
    public ULocale(Locale loc) {
	this.locName = loc.toString();
        this.locale = loc;
    }
    
    /**
     * Construct a ULocale object from a string representing the locale
     * @param locName String representation of the locale, e.g: en_US, sy-Cyrl-YU
     * @draft ICU 2.8
     */ 
    public ULocale(String locName) {
	this.locName  = locName;
        this.locale = new Locale(locName, "");
    }

    /**
     * Return the current default ULocale.
     * @draft ICU 2.8
     */ 
    public static ULocale getDefault() {
	    return new ULocale(Locale.getDefault());
    }

    /**
     * Return the root ULocale.
     * @draft ICU 2.8
     */ 
    public static final ULocale ROOT = new ULocale("");

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
	    out.writeObject(locName);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
	    locName = (String)in.readObject();
	    locale = new Locale(locName, "");
    }
}
