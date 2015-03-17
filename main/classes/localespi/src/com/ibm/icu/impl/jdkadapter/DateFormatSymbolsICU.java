/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import com.ibm.icu.text.DateFormatSymbols;

/**
 * DateFormatSymbolsICU is an adapter class which wraps ICU4J DateFormatSymbols and
 * implements java.text.DateFormatSymbols APIs.
 */
public class DateFormatSymbolsICU extends java.text.DateFormatSymbols {

    private static final long serialVersionUID = -7313618555550964943L;

    private DateFormatSymbols fIcuDfs;

    private DateFormatSymbolsICU(DateFormatSymbols icuDfs) {
        fIcuDfs = icuDfs;
    }

    public static java.text.DateFormatSymbols wrap(DateFormatSymbols icuDfs) {
        return new DateFormatSymbolsICU(icuDfs);
    }

    public DateFormatSymbols unwrap() {
        return fIcuDfs;
    }

    @Override
    public Object clone() {
        DateFormatSymbolsICU other = (DateFormatSymbolsICU)super.clone();
        other.fIcuDfs = (DateFormatSymbols)this.fIcuDfs.clone();
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DateFormatSymbolsICU) {
            return ((DateFormatSymbolsICU)obj).fIcuDfs.equals(this.fIcuDfs);
        }
        return false;
    }

    @Override
    public String[] getAmPmStrings() {
        return fIcuDfs.getAmPmStrings();
    }

    @Override
    public String[] getEras() {
        return fIcuDfs.getEras();
    }

    public String getLocalePatternChars() {
        return fIcuDfs.getLocalPatternChars();
    }

    @Override
    public String[] getMonths() {
        return fIcuDfs.getMonths();
    }

    @Override
    public String[] getShortMonths() {
        return fIcuDfs.getShortMonths();
    }

    @Override
    public String[] getShortWeekdays() {
        return fIcuDfs.getShortWeekdays();
    }

    @Override
    public String[] getWeekdays() {
        return fIcuDfs.getWeekdays();
    }

    @Override
    public String[][] getZoneStrings() {
        return fIcuDfs.getZoneStrings();
    }

    @Override
    public int hashCode() {
        return fIcuDfs.hashCode();
    }

    @Override
    public void setAmPmStrings(String[] newAmpms) {
        fIcuDfs.setAmPmStrings(newAmpms);
    }

    @Override
    public void setEras(String[] newEras) {
        fIcuDfs.setEras(newEras);
    }

    @Override
    public void setLocalPatternChars(String newLocalPatternChars) {
        fIcuDfs.setLocalPatternChars(newLocalPatternChars);
    }

    @Override
    public void setMonths(String[] newMonths) {
        fIcuDfs.setMonths(newMonths);
    }

    @Override
    public void setShortMonths(String[] newShortMonths) {
        fIcuDfs.setShortMonths(newShortMonths);
    }

    @Override
    public void setShortWeekdays(String[] newShortWeekdays) {
        fIcuDfs.setShortWeekdays(newShortWeekdays);
    }

    @Override
    public void setWeekdays(String[] newWeekdays) {
        fIcuDfs.setWeekdays(newWeekdays);
    }

    @Override
    public void setZoneStrings(String[][] newZoneStrings) {
        fIcuDfs.setZoneStrings(newZoneStrings);
    }
}
