/*
******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and        *
* others. All Rights Reserved.                                               *
******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/util/ULocale.java,v $
* $Date: 2003/11/21 00:16:06 $
* $Revision: 1.3 $
*
******************************************************************************
*/

package com.ibm.icu.util;

import java.util.Locale;

/**
 * A class for replacing the java.util.Locale. This class provides all the 
 * functionality that java.util.Locale has and in ICU 3.0 will be enhanced for 
 * supporting RFC 3066 language identifiers.
 * @author weiv
 * @draft ICU 2.8
 */
public class ULocale {
    /** 
     * Actual locale where data is coming from 
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
	
	private	Locale locale;
    
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
		locale = loc;
	}
    
    /**
     * Consturct a ULocale object from a string representing the locale
     * @param locName String representation of the locale, e.g: en_US, sy-Cyrl-YU
     * @draft ICU 2.8
     */	
    public ULocale(String locName) {
		locale = new Locale(locName, "");
	}
}
