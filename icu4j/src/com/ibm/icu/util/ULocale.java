/*
 * Created on Nov 11, 2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.icu.util;

import java.util.Locale;

/**
 * @author weiv
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ULocale {
	public static final class ULocaleDataType{
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
    
		private int localeType;
		private ULocaleDataType(int id){
			localeType = id;
		}
		private boolean equals(int id){
			return localeType == id;
		}
	}
	
	private	Locale locale;
	public Locale toLocale() {
		return locale;
	}
	public ULocale(Locale loc) {
		locale = loc;
	}
	public ULocale(String locName) {
		locale = new Locale(locName);
	}
}
