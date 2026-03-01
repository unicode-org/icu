// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;

import com.ibm.icu.impl.icuadapter.NumberFormatJDK;
import com.ibm.icu.impl.icuadapter.TimeZoneJDK;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.SimpleDateFormat;

/**
 * SimpleDateFormatICU is an adapter class which wraps ICU4J SimpleDateFormat and
 * implements java.text.SimpleDateFormat APIs.
 */
public class SimpleDateFormatICU extends java.text.SimpleDateFormat {

    private static final long serialVersionUID = -2060890659010258983L;

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
        AttributedCharacterIterator aci = fIcuSdf.formatToCharacterIterator(obj);

        // Create a new AttributedString
        StringBuilder sb = new StringBuilder(aci.getEndIndex() - aci.getBeginIndex());
        char c = aci.first();
        while (true) {
            sb.append(c);
            c = aci.next();
            if (c == CharacterIterator.DONE) {
                break;
            }
        }
        AttributedString resstr = new AttributedString(sb.toString());

        // Mapping attributes
        Map<AttributedCharacterIterator.Attribute,Object> attributes = null;
        int index = aci.getBeginIndex();
        int residx = 0;
        while (true) {
            if (aci.setIndex(index) == CharacterIterator.DONE) {
                break;
            }
            attributes = aci.getAttributes();
            if (attributes != null) {
                int end = aci.getRunLimit();
                Map<AttributedCharacterIterator.Attribute,Object> jdkAttributes = 
                    new HashMap<AttributedCharacterIterator.Attribute,Object>();
                for (Entry<AttributedCharacterIterator.Attribute, Object> entry
                        : attributes.entrySet()) {
                    AttributedCharacterIterator.Attribute key = entry.getKey();
                    AttributedCharacterIterator.Attribute jdkKey = mapAttribute(key);
                    Object jdkVal = entry.getValue();
                    if (jdkVal instanceof AttributedCharacterIterator.Attribute) {
                        jdkVal = mapAttribute((AttributedCharacterIterator.Attribute)jdkVal);
                    }
                    jdkAttributes.put(jdkKey, jdkVal);
                }
                int resend = residx + (end - index);
                resstr.addAttributes(jdkAttributes, residx, resend);

                index = end;
                residx = resend;
            }
        }
        return resstr.getIterator();
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

    private static AttributedCharacterIterator.Attribute mapAttribute(AttributedCharacterIterator.Attribute icuAttribute) {
        AttributedCharacterIterator.Attribute jdkAttribute = icuAttribute;

        if (icuAttribute == DateFormat.Field.AM_PM) {
            jdkAttribute = java.text.DateFormat.Field.AM_PM;
        } else if (icuAttribute == DateFormat.Field.DAY_OF_MONTH) {
            jdkAttribute = java.text.DateFormat.Field.DAY_OF_MONTH;
        } else if (icuAttribute == DateFormat.Field.DAY_OF_WEEK) {
            jdkAttribute = java.text.DateFormat.Field.DAY_OF_WEEK;
        } else if (icuAttribute == DateFormat.Field.DAY_OF_WEEK_IN_MONTH) {
            jdkAttribute = java.text.DateFormat.Field.DAY_OF_WEEK_IN_MONTH;
        } else if (icuAttribute == DateFormat.Field.DAY_OF_YEAR) {
            jdkAttribute = java.text.DateFormat.Field.DAY_OF_YEAR;
        } else if (icuAttribute == DateFormat.Field.ERA) {
            jdkAttribute = java.text.DateFormat.Field.ERA;
        } else if (icuAttribute == DateFormat.Field.HOUR_OF_DAY0) {
            jdkAttribute = java.text.DateFormat.Field.HOUR_OF_DAY0;
        } else if (icuAttribute == DateFormat.Field.HOUR_OF_DAY1) {
            jdkAttribute = java.text.DateFormat.Field.HOUR_OF_DAY1;
        } else if (icuAttribute == DateFormat.Field.HOUR0) {
            jdkAttribute = java.text.DateFormat.Field.HOUR0;
        } else if (icuAttribute == DateFormat.Field.HOUR1) {
            jdkAttribute = java.text.DateFormat.Field.HOUR1;
        } else if (icuAttribute == DateFormat.Field.MILLISECOND) {
            jdkAttribute = java.text.DateFormat.Field.MILLISECOND;
        } else if (icuAttribute == DateFormat.Field.MINUTE) {
            jdkAttribute = java.text.DateFormat.Field.MINUTE;
        } else if (icuAttribute == DateFormat.Field.MONTH) {
            jdkAttribute = java.text.DateFormat.Field.MONTH;
        } else if (icuAttribute == DateFormat.Field.SECOND) {
            jdkAttribute = java.text.DateFormat.Field.SECOND;
        } else if (icuAttribute == DateFormat.Field.TIME_ZONE) {
            jdkAttribute = java.text.DateFormat.Field.TIME_ZONE;
        } else if (icuAttribute == DateFormat.Field.WEEK_OF_MONTH) {
            jdkAttribute = java.text.DateFormat.Field.WEEK_OF_MONTH;
        } else if (icuAttribute == DateFormat.Field.WEEK_OF_YEAR) {
            jdkAttribute = java.text.DateFormat.Field.WEEK_OF_YEAR;
        } else if (icuAttribute == DateFormat.Field.YEAR) {
            jdkAttribute = java.text.DateFormat.Field.YEAR;
        }
        // There are other DateFormat.Field constants defined in
        // ICU4J DateFormat below.
        //
        //   DOW_LOCAL
        //   EXTENDED_YEAR
        //   JULIAN_DAY
        //   MILLISECONDS_IN_DAY
        //   QUARTER
        //   YEAR_WOY
        //
        // However, the corresponding pattern characters are not used by
        // the default factory method - getXXXInstance.  So these constants
        // are only used when user intentionally set a pattern including
        // these ICU4J specific pattern letters.  Even it happens,
        // ICU4J's DateFormat.Field extends java.text.Format.Field, so
        // it does not break the contract of formatToCharacterIterator.

        return jdkAttribute;
    }

}
