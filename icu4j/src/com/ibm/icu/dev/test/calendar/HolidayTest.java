/*
 *******************************************************************************
 * Copyright (C) 1996-2003, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/calendar/HolidayTest.java,v $ 
 * $Date: 2003/06/03 18:49:28 $ 
 * $Revision: 1.5 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;

import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.EasterHoliday;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.Holiday;
import com.ibm.icu.util.RangeDateRule;
import com.ibm.icu.util.SimpleDateRule;
import com.ibm.icu.util.SimpleHoliday;

/**
 * Tests for the <code>Holiday</code> class.
 */
public class HolidayTest extends TestFmwk {
	public static void main(String args[]) throws Exception {
		new HolidayTest().run(args);
	}

	static final Calendar cal = new GregorianCalendar(1, 0, 1);
	static final Date longTimeAgo = cal.getTime();
	static final Date now = new Date();
	static final long awhile = 3600L * 24 * 28; // 28 days

	public void TestAPI() {
		{
			// getHolidays
			Holiday[] holidays = Holiday.getHolidays();
			exerciseHolidays(holidays, Locale.getDefault());
		}

		{
			// getHolidays(Locale)
			String[] localeNames =
				{
					"en_US",
					"da",
					"da_DK",
					"de",
					"de_AT",
					"de_DE",
					"el",
					"el_GR",
					"en",
					"en_CA",
					"en_GB",
					"es",
					"es_MX",
					"fr",
					"fr_CA",
					"fr_FR",
					"it",
					"it_IT",
					"iw",
					"iw_IL",
					"ja",
					"ja_JP",
					};

			for (int i = 0; i < localeNames.length; ++i) {
				Locale locale = LocaleUtility.getLocaleFromName(localeNames[i]);
				Holiday[] holidays = Holiday.getHolidays(locale);
				exerciseHolidays(holidays, locale);
			}
		}
	}

	void exerciseHolidays(Holiday[] holidays, Locale locale) {
		for (int i = 0; i < holidays.length; ++i) {
			exerciseHoliday(holidays[i], locale);
		}
	}

	void exerciseHoliday(Holiday h, Locale locale) {
		logln("holiday: " + h.getDisplayName());
		logln("holiday in " + locale + ": " + h.getDisplayName(locale));

		Date first = h.firstAfter(longTimeAgo);
		logln("firstAfter: " + longTimeAgo + " is " + first);
		if (first == null) {
			first = longTimeAgo;
		}
		first.setTime(first.getTime() + awhile);

		Date second = h.firstBetween(first, now);
		logln("firstBetween: " + first + " and " + now + " is " + second);
		if (second == null) {
			second = now;
		}

		logln("is on " + first + ": " + h.isOn(first));
		logln("is on " + now + ": " + h.isOn(now));
		logln(
			"is between "
				+ first
				+ " and "
				+ now
				+ ": "
				+ h.isBetween(first, now));
		logln(
			"is between "
				+ first
				+ " and "
				+ second
				+ ": "
				+ h.isBetween(first, second));

		logln("rule: " + h.getRule().toString());

		h.setRule(h.getRule());
	}
    
    public void TestCoverage(){
        Holiday[] h = { new EasterHoliday("Ram's Easter"),
                        new SimpleHoliday(2, 29, 0, "Leap year", 1900, 2100)};
        exerciseHolidays(h, Locale.getDefault());

        RangeDateRule rdr = new RangeDateRule();
        rdr.add(new SimpleDateRule(7, 15));
    }
 }
