/*
 *******************************************************************************
 * Copyright (C) 2002, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/calendar/JapaneseTest.java,v $
 * $Date: 2002/08/08 23:06:09 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.TimeZone;

/**
 * Tests for the <code>IslamicCalendar</code> class.
 */
public class JapaneseTest extends CalendarTest {
    public static void main(String args[]) throws Exception {
        new JapaneseTest().run(args);
    }

    public void TestCoverage() {
	{
	    // new JapaneseCalendar(TimeZone)
	    JapaneseCalendar cal = new JapaneseCalendar(TimeZone.getDefault());
	}
	
	{
	    // new JapaneseCalendar(Locale)
	    JapaneseCalendar cal = new JapaneseCalendar(Locale.getDefault());
	}

	{
	    // new JapaneseCalendar(TimeZone, Locale)
	    JapaneseCalendar cal = new JapaneseCalendar(TimeZone.getDefault(), Locale.getDefault());
	}

	{
	    // new JapaneseCalendar(Date)
	    JapaneseCalendar cal = new JapaneseCalendar(new Date());
	}

	{
	    // new JapaneseCalendar(int year, int month, int date)
	    JapaneseCalendar cal = new JapaneseCalendar(1868, Calendar.JANUARY, 1);
	}

	{
	    // new JapaneseCalendar(int era, int year, int month, int date)
	    JapaneseCalendar cal = new JapaneseCalendar(JapaneseCalendar.MEIJI, 43, Calendar.JANUARY, 1);
	}

	{
	    // new JapaneseCalendar(int year, int month, int date, int hour, int minute, int second)
	    JapaneseCalendar cal = new JapaneseCalendar(1868, Calendar.JANUARY, 1, 1, 1, 1);
	}

	{
	    // limits
	    JapaneseCalendar cal = new JapaneseCalendar();
	    DateFormat fmt = cal.getDateTimeFormat(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH);

	    cal.set(cal.ERA, cal.MEIJI);
	    logln("date: " + cal.getTime());
	    logln("min era: " + cal.getMinimum(cal.ERA));
	    logln("min year: " + cal.getMinimum(cal.YEAR));
	    cal.set(cal.YEAR, cal.getActualMaximum(cal.YEAR));
	    logln("date: " + fmt.format(cal.getTime()));
	    cal.add(cal.YEAR, 1);
	    logln("date: " + fmt.format(cal.getTime()));
	}
	
	{
	    // data
	    JapaneseCalendar cal = new JapaneseCalendar(1868, Calendar.JANUARY, 1);
	    Date time = cal.getTime();

	    String[] calendarLocales = {
		"en", "ja_JP"
	    };

	    String[] formatLocales = {
		"en", "ja"
	    };
	    for (int i = 0; i < calendarLocales.length; ++i) {
		String calLocName = calendarLocales[i];
		Locale calLocale = LocaleUtility.getLocaleFromName(calLocName);
		cal = new JapaneseCalendar(calLocale);

		for (int j = 0; j < formatLocales.length; ++j) {
		    String locName = formatLocales[j];
		    Locale formatLocale = LocaleUtility.getLocaleFromName(locName);
		    DateFormat format = DateFormat.getDateTimeInstance(cal, DateFormat.FULL, DateFormat.FULL, formatLocale);
		    logln(calLocName + "/" + locName + " --> " + format.format(time));
		}
	    }
	}
    }
}

