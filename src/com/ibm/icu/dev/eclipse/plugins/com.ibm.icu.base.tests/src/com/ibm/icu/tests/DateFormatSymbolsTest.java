/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.util.Locale;

import com.ibm.icu.text.DateFormatSymbols;
import com.ibm.icu.util.ULocale;

public class DateFormatSymbolsTest extends ICUTestCase {

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.hashCode()'
	 */
	public void testHashCode() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		DateFormatSymbols dfs2 = new DateFormatSymbols(ULocale.US);
		DateFormatSymbols dfsn = new DateFormatSymbols(Locale.US);
		dfsn.setAmPmStrings(new String[] { "sw", "xw" });
		testEHCS(dfs, dfs2, dfsn);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.DateFormatSymbols(DateFormatSymbols)'
	 */
	public void testDateFormatSymbolsDateFormatSymbols() {
		// implicitly tested everywhere
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.DateFormatSymbols()'
	 */
	public void testDateFormatSymbols() {
		DateFormatSymbols dfs = new DateFormatSymbols();
		assertNotNull(dfs.getWeekdays());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.DateFormatSymbols(Locale)'
	 */
	public void testDateFormatSymbolsLocale() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getWeekdays());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.DateFormatSymbols(ULocale)'
	 */
	public void testDateFormatSymbolsULocale() {
		DateFormatSymbols dfs = new DateFormatSymbols(ULocale.US);
		assertNotNull(dfs.getWeekdays());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.getEras()'
	 */
	public void testGetEras() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getEras());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.setEras(String[])'
	 */
	public void testSetEras() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		String[] oldvalue = dfs.getEras();
		String[] newvalue = (String[])oldvalue.clone();
		newvalue[0] = newvalue[0] + "!";
		dfs.setEras(newvalue);
		String[] result = dfs.getEras();
		assertArraysNotEqual(oldvalue, result);
		assertArraysEqual(newvalue, result);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.getMonths()'
	 */
	public void testGetMonths() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getMonths());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.setMonths(String[])'
	 */
	public void testSetMonths() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		String[] oldvalue = dfs.getMonths();
		String[] newvalue = (String[])oldvalue.clone();
		newvalue[0] = newvalue[0] + "!";
		dfs.setMonths(newvalue);
		String[] result = dfs.getMonths();
		assertArraysNotEqual(oldvalue, result);
		assertArraysEqual(newvalue, result);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.getShortMonths()'
	 */
	public void testGetShortMonths() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getShortMonths());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.setShortMonths(String[])'
	 */
	public void testSetShortMonths() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		String[] oldvalue = dfs.getShortMonths();
		String[] newvalue = (String[])oldvalue.clone();
		newvalue[0] = newvalue[0] + "!";
		dfs.setShortMonths(newvalue);
		String[] result = dfs.getShortMonths();
		assertArraysNotEqual(oldvalue, result);
		assertArraysEqual(newvalue, result);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.getWeekdays()'
	 */
	public void testGetWeekdays() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getShortMonths());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.setWeekdays(String[])'
	 */
	public void testSetWeekdays() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		String[] oldvalue = dfs.getWeekdays();
		String[] newvalue = (String[])oldvalue.clone();
		newvalue[0] = newvalue[0] + "!";
		dfs.setWeekdays(newvalue);
		String[] result = dfs.getWeekdays();
		assertArraysNotEqual(oldvalue, result);
		assertArraysEqual(newvalue, result);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.getShortWeekdays()'
	 */
	public void testGetShortWeekdays() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getShortWeekdays());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.setShortWeekdays(String[])'
	 */
	public void testSetShortWeekdays() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		String[] oldvalue = dfs.getShortWeekdays();
		String[] newvalue = (String[])oldvalue.clone();
		newvalue[0] = newvalue[0] + "!";
		dfs.setShortWeekdays(newvalue);
		String[] result = dfs.getShortWeekdays();
		assertArraysNotEqual(oldvalue, result);
		assertArraysEqual(newvalue, result);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.getAmPmStrings()'
	 */
	public void testGetAmPmStrings() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getAmPmStrings());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.setAmPmStrings(String[])'
	 */
	public void testSetAmPmStrings() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		String[] oldvalue = dfs.getAmPmStrings();
		String[] newvalue = (String[])oldvalue.clone();
		newvalue[0] = newvalue[0] + "!";
		dfs.setAmPmStrings(newvalue);
		String[] result = dfs.getAmPmStrings();
		assertArraysNotEqual(oldvalue, result);
		assertArraysEqual(newvalue, result);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.getZoneStrings()'
	 */
	public void testGetZoneStrings() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getZoneStrings());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.setZoneStrings(String[][])'
	 */
	public void testSetZoneStrings() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		String[][] oldvalue = dfs.getZoneStrings();
		String[][] newvalue = (String[][])cloneComplex(oldvalue);
		newvalue[0][0] = newvalue[0][0] + "!";
		dfs.setZoneStrings(newvalue);
		String[][] result = dfs.getZoneStrings();
		assertArraysNotEqual(oldvalue, result);
		assertArraysEqual(newvalue, result);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.getLocalPatternChars()'
	 */
	public void testGetLocalPatternChars() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.getLocalPatternChars());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.setLocalPatternChars(String)'
	 */
	public void testSetLocalPatternChars() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		String pat = dfs.getLocalPatternChars();
		StringBuffer buf = new StringBuffer(pat);
		buf.setCharAt(0, (char)(pat.charAt(0) + 1));
		String pat2 = buf.toString();
		dfs.setLocalPatternChars(pat2);
		String pat3 = dfs.getLocalPatternChars();
		assertNotEqual(pat, pat2);
		assertEquals(pat2, pat3);
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.toString()'
	 */
	public void testToString() {
		DateFormatSymbols dfs = new DateFormatSymbols(Locale.US);
		assertNotNull(dfs.toString());
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.clone()'
	 */
	public void testClone() {
		// tested by testHashCode
	}

	/*
	 * Test method for 'com.ibm.icu.text.DateFormatSymbols.equals(Object)'
	 */
	public void testEqualsObject() {
		// tested by testHashCode
	}
}
