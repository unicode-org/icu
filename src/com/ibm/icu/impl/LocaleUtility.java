/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/impl/LocaleUtility.java,v $
 * $Date: 2002/03/12 17:49:15 $
 * $Revision: 1.2 $
 *  *****************************************************************************************
 */
 
package com.ibm.icu.impl;

import java.util.Locale;

/**
 * A class to hold utilitiy functions missing from java.util.Locale.
 */
public class LocaleUtility {

	/**
	 * A helper function to convert a string of the form
     * aa_BB_CC to a locale object.  Why isn't this in Locale?
	 */
	public static Locale getLocaleFromName(String name) {
		String language = "";
		String country = "";
		String variant = "";

		int i1 = name.indexOf('_');
		if (i1 < 0) {
			language = name;
		} else {
			language = name.substring(0, i1);
			++i1;
			int i2 = name.indexOf('_', i1);
			if (i2 < 0) {
				country = name.substring(i1);
			} else {
				country = name.substring(i1, i2);
				variant = name.substring(i2+1);
			}
		}

		return new Locale(language, country, variant);
	}

	/**
	 * Compare two locale strings of the form aa_BB_CC, and
     * return true if parent is a 'strict' fallback of child, that is,
	 * if child =~ "^parent(_.+)*" (roughly).
	 */
	public static boolean isFallbackOf(String parent, String child) {
		if (!child.startsWith(parent)) {
			return false;
		}
		int i = parent.length();
		return (i == child.length() ||
				child.charAt(i) == '_');
	}

	/**
	 * Compare two locales, and return true if the parent is a
	 * 'strict' fallback of the child (parent string is a fallback
	 * of child string).
	 */
	public static boolean isFallbackOf(Locale parent, Locale child) {
		return isFallbackOf(parent.toString(), child.toString());
	}
}
