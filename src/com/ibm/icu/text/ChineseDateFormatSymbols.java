/****************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and
 * others. All Rights Reserved.
 ****************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.impl.CalendarData;
import com.ibm.icu.util.*;
import java.util.Locale;

/**
 * A subclass of {@link DateFormatSymbols} for {@link ChineseDateFormat}.
 * This class contains additional symbols corresponding to the
 * <code>ChineseCalendar.IS_LEAP_MONTH</code> field.
 *
 * @see ChineseDateFormat
 * @see com.ibm.icu.util.ChineseCalendar
 * @author Alan Liu
 * @stable ICU 2.0
 */
public class ChineseDateFormatSymbols extends DateFormatSymbols {
    
    /**
     * Package-private array that ChineseDateFormat needs to be able to
     * read.
     */
    String isLeapMonth[]; // Do NOT add =null initializer

    /**
     * Construct a ChineseDateFormatSymbols for the default locale.
     * @stable ICU 2.0
     */
    public ChineseDateFormatSymbols() {
        this(ULocale.getDefault());
    }

    /**
     * Construct a ChineseDateFormatSymbols for the provided locale.
     * @param locale the locale
     * @stable ICU 2.0
     */
    public ChineseDateFormatSymbols(Locale locale) {
        super(ChineseCalendar.class, ULocale.forLocale(locale));
    }

    /**
     * Construct a ChineseDateFormatSymbols for the provided locale.
     * @param locale the locale
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public ChineseDateFormatSymbols(ULocale locale) {
        super(ChineseCalendar.class, locale);
    }

    /**
     * Construct a ChineseDateFormatSymbols for the provided calendar and locale.
     * @param cal the Calendar
     * @param locale the locale
     * @stable ICU 2.0
     */
    public ChineseDateFormatSymbols(Calendar cal, Locale locale) {
        super(cal==null?null:cal.getClass(), locale);
    }

    /**
     * Construct a ChineseDateFormatSymbols for the provided calendar and locale.
     * @param cal the Calendar
     * @param locale the locale
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public ChineseDateFormatSymbols(Calendar cal, ULocale locale) {
        super(cal == null ? null : cal.getClass(), locale);
    }

    // New API
    /**
     * @stable ICU 2.0
     */
    public String getLeapMonth(int isLeapMonth) {
        return this.isLeapMonth[isLeapMonth];
    }

    /**
     * @draft ICU 3.0
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    protected void initializeData(ULocale loc, CalendarData calData) {
        super.initializeData(loc, calData);
        isLeapMonth = calData.getStringArray("isLeapMonth");
    }
}
