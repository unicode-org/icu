/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test.calendar;

import java.util.Date;
import java.util.Locale;

import com.ibm.icu.impl.LocaleUtility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.JapaneseCalendar;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

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
        if(cal == null){
            errln("could not create JapaneseCalendar with TimeZone");
        }
    }

    {
        // new JapaneseCalendar(ULocale)
        JapaneseCalendar cal = new JapaneseCalendar(ULocale.getDefault());
        if(cal == null){
            errln("could not create JapaneseCalendar with ULocale");
        }
    }

    {
        // new JapaneseCalendar(TimeZone, ULocale)
        JapaneseCalendar cal = new JapaneseCalendar(TimeZone.getDefault(), ULocale.getDefault());
        if(cal == null){
            errln("could not create JapaneseCalendar with TimeZone ULocale");
        }
    }
    
    {
        // new JapaneseCalendar(Locale)
        JapaneseCalendar cal = new JapaneseCalendar(Locale.getDefault());
        if(cal == null){
            errln("could not create JapaneseCalendar with Locale");
        }
    }

    {
        // new JapaneseCalendar(TimeZone, Locale)
        JapaneseCalendar cal = new JapaneseCalendar(TimeZone.getDefault(), Locale.getDefault());
        if(cal == null){
            errln("could not create JapaneseCalendar with TimeZone Locale");
        }
    }

    {
        // new JapaneseCalendar(Date)
        JapaneseCalendar cal = new JapaneseCalendar(new Date());
        if(cal == null){
            errln("could not create JapaneseCalendar with Date");
        }
    }

    {
        // new JapaneseCalendar(int year, int month, int date)
        JapaneseCalendar cal = new JapaneseCalendar(1868, Calendar.JANUARY, 1);
        if(cal == null){
            errln("could not create JapaneseCalendar with year,month,date");
        }
    }

    {
        // new JapaneseCalendar(int era, int year, int month, int date)
        JapaneseCalendar cal = new JapaneseCalendar(JapaneseCalendar.MEIJI, 43, Calendar.JANUARY, 1);
        if(cal == null){
            errln("could not create JapaneseCalendar with era,year,month,date");
        }
    }

    {
        // new JapaneseCalendar(int year, int month, int date, int hour, int minute, int second)
        JapaneseCalendar cal = new JapaneseCalendar(1868, Calendar.JANUARY, 1, 1, 1, 1);
        if(cal == null){
            errln("could not create JapaneseCalendar with year,month,date,hour,min,second");
        }
    }

    {
        // limits
        JapaneseCalendar cal = new JapaneseCalendar();
        DateFormat fmt = cal.getDateTimeFormat(DateFormat.FULL, DateFormat.FULL, Locale.ENGLISH);

        cal.set(Calendar.ERA, JapaneseCalendar.MEIJI);
        logln("date: " + cal.getTime());
        logln("min era: " + cal.getMinimum(Calendar.ERA));
        logln("min year: " + cal.getMinimum(Calendar.YEAR));
        cal.set(Calendar.YEAR, cal.getActualMaximum(Calendar.YEAR));
        logln("date: " + fmt.format(cal.getTime()));
        cal.add(Calendar.YEAR, 1);
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

