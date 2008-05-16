/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.text.AttributedCharacterIterator;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import com.ibm.icu.impl.icuadapter.NumberFormatJDK;
import com.ibm.icu.impl.icuadapter.TimeZoneJDK;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * SimpleDateFormatICU is an adapter class which wraps ICU4J SimpleDateFormat and
 * implements java.text.SimpleDateFormat APIs.
 */
public class SimpleDateFormatICU extends java.text.SimpleDateFormat {

    private SimpleDateFormat fIcuSdf;
    
    private SimpleDateFormatICU(SimpleDateFormat icuSdf) {
        fIcuSdf = icuSdf;
    }

    public static java.text.SimpleDateFormat wrap(SimpleDateFormat icuSdf) {
        return new SimpleDateFormatICU(icuSdf);
    }

    // Methods overriding java.text.SimpleDateFormat

    @Override
    public void applyLocalizedPattern(String pattern) {
        fIcuSdf.applyLocalizedPattern(pattern);
    }

    @Override
    public void applyPattern(String pattern) {
        fIcuSdf.applyPattern(pattern);
    }

    @Override
    public Object clone() {
        SimpleDateFormatICU other = (SimpleDateFormatICU)super.clone();
        other.fIcuSdf = (SimpleDateFormat)this.fIcuSdf.clone();
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SimpleDateFormatICU) {
            return ((SimpleDateFormatICU)obj).fIcuSdf.equals(this.fIcuSdf);
        }
        return false;
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
        return fIcuSdf.format(date, toAppendTo, pos);
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        // TODO
        return fIcuSdf.formatToCharacterIterator(obj);
    }

    @Override
    public Date get2DigitYearStart() {
        return fIcuSdf.get2DigitYearStart();
    }

    @Override
    public DateFormatSymbols getDateFormatSymbols() {
        return DateFormatSymbolsICU.wrap(fIcuSdf.getDateFormatSymbols());
    }

    @Override
    public int hashCode() {
        return fIcuSdf.hashCode();
    }

    @Override
    public Date parse(String text, ParsePosition pos) {
        return fIcuSdf.parse(text, pos);
    }

    @Override
    public void set2DigitYearStart(Date startDate) {
        fIcuSdf.set2DigitYearStart(startDate);
    }

    @Override
    public void setDateFormatSymbols(DateFormatSymbols newFormatSymbols) {
        com.ibm.icu.text.DateFormatSymbols icuDfs = null;
        if (newFormatSymbols instanceof DateFormatSymbolsICU) {
            icuDfs = ((DateFormatSymbolsICU)newFormatSymbols).unwrap();
        } else if (fIcuSdf.getCalendar() instanceof com.ibm.icu.util.GregorianCalendar) {
            // Java 6 uses DateFormatSymbols exclusively for Gregorian
            // calendar.

            String[] newJDK, curICU, newICU;
            icuDfs = fIcuSdf.getDateFormatSymbols();

            // Eras
            newJDK = newFormatSymbols.getEras();
            curICU = icuDfs.getEras();
            newICU = copySymbols(newJDK, curICU, true);

            // Months
            newJDK = newFormatSymbols.getMonths();
            curICU = icuDfs.getMonths();
            newICU = copySymbols(newJDK, curICU, false);
            icuDfs.setMonths(newICU);

            // ShortMonths
            newJDK = newFormatSymbols.getShortMonths();
            curICU = icuDfs.getShortMonths();
            newICU = copySymbols(newJDK, curICU, false);
            icuDfs.setShortMonths(newICU);

            // Weekdays
            newJDK = newFormatSymbols.getWeekdays();
            curICU = icuDfs.getWeekdays();
            newICU = copySymbols(newJDK, curICU, false);
            icuDfs.setWeekdays(newICU);

            // ShortWeekdays
            newJDK = newFormatSymbols.getShortWeekdays();
            curICU = icuDfs.getShortWeekdays();
            newICU = copySymbols(newJDK, curICU, false);
            icuDfs.setShortWeekdays(newICU);

            // AmPm
            newJDK = newFormatSymbols.getAmPmStrings();
            curICU = icuDfs.getAmPmStrings();
            newICU = copySymbols(newJDK, curICU, false);
            icuDfs.setAmPmStrings(newICU);
        } else {
            // For other calendars, JDK's standard DateFormatSymbols
            // cannot be used.
            throw new UnsupportedOperationException("JDK DateFormatSymbols cannot be used for the calendar type.");
        }
        fIcuSdf.setDateFormatSymbols(icuDfs);
    }

    @Override
    public String toLocalizedPattern() {
        return fIcuSdf.toLocalizedPattern();
    }

    @Override
    public String toPattern() {
        return fIcuSdf.toLocalizedPattern();
    }

    // Methods overriding java.text.DateFormat

    @Override
    public Calendar getCalendar() {
        return CalendarICU.wrap(fIcuSdf.getCalendar());
    }

    @Override
    public NumberFormat getNumberFormat() {
        com.ibm.icu.text.NumberFormat nfmt = fIcuSdf.getNumberFormat();
        if (nfmt instanceof NumberFormatJDK) {
            return ((NumberFormatJDK)nfmt).unwrap();
        }
        if (nfmt instanceof com.ibm.icu.text.DecimalFormat) {
            return DecimalFormatICU.wrap((com.ibm.icu.text.DecimalFormat)nfmt);
        }
        return NumberFormatICU.wrap(nfmt);
    }

    @Override
    public TimeZone getTimeZone() {
        return getCalendar().getTimeZone();
    }

    @Override
    public boolean isLenient() {
        return fIcuSdf.isLenient();
    }

    private static final long SAMPLE_TIME = 962409600000L; //2000-07-01T00:00:00Z
    private static final int JAPANESE_YEAR = 12; // Japanese calendar year @ SAMPLE_TIME
    private static final int THAI_YEAR = 2543; // Thai Buddhist calendar year @ SAMPLE_TIME

    @Override
    public void setCalendar(Calendar newCalendar) {
        com.ibm.icu.util.Calendar icuCal = null;
        if (newCalendar instanceof CalendarICU) {
            icuCal = ((CalendarICU)newCalendar).unwrap();
        } else {
            // Note:    There is no easy way to implement ICU Calendar with
            //          JDK Calendar implementation.  For now, this code assumes
            //          the given calendar is either Gregorian, Buddhist or
            //          JapaneseImperial.  Once the type is detected, this code
            //          creates an instance of ICU Calendar with the same type.
            com.ibm.icu.util.TimeZone icuTz = TimeZoneJDK.wrap(newCalendar.getTimeZone());
            if (newCalendar instanceof GregorianCalendar) {
                icuCal = new com.ibm.icu.util.GregorianCalendar(icuTz);
            } else {
                newCalendar.setTimeInMillis(SAMPLE_TIME);
                int year = newCalendar.get(Calendar.YEAR);
                if (year == JAPANESE_YEAR) {
                    icuCal = new com.ibm.icu.util.JapaneseCalendar(icuTz);
                } else if (year == THAI_YEAR) {
                    icuCal = new com.ibm.icu.util.BuddhistCalendar(icuTz);
                } else {
                    // We cannot support the case
                    throw new UnsupportedOperationException("Unsupported calendar type by ICU Calendar adapter.");
                }
            }
            // Copy the original calendar settings
            icuCal.setFirstDayOfWeek(newCalendar.getFirstDayOfWeek());
            icuCal.setLenient(newCalendar.isLenient());
            icuCal.setMinimalDaysInFirstWeek(newCalendar.getMinimalDaysInFirstWeek());
        }
        fIcuSdf.setCalendar(icuCal);
    }

    @Override
    public void setLenient(boolean lenient) {
        fIcuSdf.setLenient(lenient);
    }

    @Override
    public void setNumberFormat(NumberFormat newNumberFormat) {
        if (newNumberFormat instanceof DecimalFormatICU) {
            fIcuSdf.setNumberFormat(((DecimalFormatICU)newNumberFormat).unwrap());
        } else if (newNumberFormat instanceof NumberFormatICU) {
            fIcuSdf.setNumberFormat(((NumberFormatICU)newNumberFormat).unwrap());
        } else {
            fIcuSdf.setNumberFormat(NumberFormatJDK.wrap(newNumberFormat));
        }
    }

    @Override
    public void setTimeZone(TimeZone zone) {
        fIcuSdf.setTimeZone(TimeZoneJDK.wrap(zone));
    }

    private String[] copySymbols(String[] newData, String[] curData, boolean alignEnd) {
        if (newData.length >= curData.length) {
            return newData;
        }
        int startOffset = alignEnd ? curData.length - newData.length : 0;
        System.arraycopy(newData, 0, curData, startOffset, newData.length);
        return curData;
    }
}
