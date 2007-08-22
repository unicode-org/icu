/*
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.tests;

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class TimeZoneTest extends ICUTestCase {

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.hashCode()'
	 */
	public void testHashCode() {
		TimeZone tz1 = TimeZone.getTimeZone("PST");
		TimeZone tz2 = TimeZone.getTimeZone("PST");
		TimeZone tzn = TimeZone.getTimeZone("CST");
		testEHCS(tz1, tz2, tzn);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.TimeZone(TimeZone)'
	 */
	public void testTimeZone() {
		// implicitly tested everywhere
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getOffset(int, int, int, int, int, int)'
	 */
	public void testGetOffset() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		int offset = tz.getOffset(1, 2004, 0, 01, 1, 0);
		assertEquals(-28800000, offset);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.setRawOffset(int)'
	 */
	public void testSetRawOffset() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		int value = tz.getRawOffset();
		int value1 = value + 100000;
		tz.setRawOffset(value1);
		int result = tz.getRawOffset();
		assertNotEqual(value, result);
		assertEquals(value1, result);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getRawOffset()'
	 */
	public void testGetRawOffset() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		int offset = tz.getRawOffset();
		assertEquals(-28800000, offset);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getID()'
	 */
	public void testGetID() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertEquals("PST", tz.getID());
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.setID(String)'
	 */
	public void testSetID() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		String value1 = tz.getID();
		String value2 = value1 + "!";
		tz.setID(value2);
		String result = tz.getID();
		assertNotEqual(value1, result);
		assertEquals(value2, result);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getDisplayName()'
	 */
	public void testGetDisplayName() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertEquals("Pacific Standard Time", tz.getDisplayName());
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getDisplayName(Locale)'
	 */
	public void testGetDisplayNameLocale() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertEquals("Pacific Standard Time", tz.getDisplayName(Locale.US));
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getDisplayName(ULocale)'
	 */
	public void testGetDisplayNameULocale() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertEquals("Pacific Standard Time", tz.getDisplayName(ULocale.US));
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getDisplayName(boolean, int)'
	 */
	public void testGetDisplayNameBooleanInt() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertEquals("PDT", tz.getDisplayName(true, TimeZone.SHORT));
		assertEquals("Pacific Daylight Time", tz.getDisplayName(true, TimeZone.LONG));
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getDisplayName(boolean, int, Locale)'
	 */
	public void testGetDisplayNameBooleanIntLocale() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertEquals("PDT", tz.getDisplayName(true, TimeZone.SHORT, Locale.US));
		assertEquals("Pacific Daylight Time", tz.getDisplayName(true, TimeZone.LONG, Locale.US));
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getDisplayName(boolean, int, ULocale)'
	 */
	public void testGetDisplayNameBooleanIntULocale() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertEquals("PDT", tz.getDisplayName(true, TimeZone.SHORT, ULocale.US));
		assertEquals("Pacific Daylight Time", tz.getDisplayName(true, TimeZone.LONG, ULocale.US));
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getDSTSavings()'
	 */
	public void testGetDSTSavings() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertEquals(3600000, tz.getDSTSavings());
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.useDaylightTime()'
	 */
	public void testUseDaylightTime() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		assertTrue(tz.useDaylightTime());
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.inDaylightTime(Date)'
	 */
	public void testInDaylightTime() {
		TimeZone tz = TimeZone.getTimeZone("PST");
		Calendar cal = Calendar.getInstance();
		cal.set(2005, 0, 17);
		Date date = cal.getTime();
		assertFalse(tz.inDaylightTime(date));
		cal.set(2005, 6, 17);
		date = cal.getTime();
		assertTrue(tz.inDaylightTime(date));
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getTimeZone(String)'
	 */
	public void testGetTimeZone() {
		// implicitly tested everywhere
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getAvailableIDs(int)'
	 */
	public void testGetAvailableIDsInt() {
		String[] ids = TimeZone.getAvailableIDs(-28800000);
		assertNotNull(ids);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getAvailableIDs()'
	 */
	public void testGetAvailableIDs() {
		String[] ids = TimeZone.getAvailableIDs();
		assertNotNull(ids);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.getDefault()'
	 */
	public void testGetDefault() {
		TimeZone tz = TimeZone.getDefault();
		assertNotNull(tz);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.setDefault(TimeZone)'
	 */
	public void testSetDefault() {
		TimeZone tz1 = TimeZone.getDefault();
		String newCode = "PDT".equals(tz1.getID()) ? "CST" : "PDT";
		TimeZone tz2 = TimeZone.getTimeZone(newCode);
		TimeZone.setDefault(tz2);
		TimeZone result = TimeZone.getDefault();
		assertNotEqual(tz1, result);
		assertEquals(tz2, result);
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.hasSameRules(TimeZone)'
	 */
	public void testHasSameRules() {
		TimeZone tz1 = TimeZone.getTimeZone("PST");
		TimeZone tz2 = TimeZone.getTimeZone("America/Los_Angeles");
		assertTrue(tz1.hasSameRules(tz2));
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.clone()'
	 */
	public void testClone() {
		// tested by testHashCode
	}

	/*
	 * Test method for 'com.ibm.icu.util.TimeZone.equals(Object)'
	 */
	public void testEqualsObject() {
		// tested by testHashCode
	}
}
