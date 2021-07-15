// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2015, International Business Machines Corporation and    *
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

    // Implementation Note:
    //      On OpenJDK/Oracle/IBM Java 8, the super class constructor calls
    //      this.clone(). At this point, fIcuDfs is not yet initialized.
    //      The cloned instance is only used for optimizing Java's
    //      DateFormatSymbols initialization and we'll never have an instance
    //      of DateFormatSymbolsICU with fIcuDfs = null. However, for safety,
    //      all method implementation uses the pattern -
    //
    //          if (fIcuDfs == null) {
    //              return super.methodX();
    //          }
    //          return fIcuDfs.methodX();
    //
    //      to prevent NPE. For more details, please refer #11733
    
    private DateFormatSymbolsICU(DateFormatSymbols icuDfs) {
        fIcuDfs = icuDfs;
    }

    public static java.text.DateFormatSymbols wrap(DateFormatSymbols icuDfs) {
        if (icuDfs == null) {
            icuDfs = new DateFormatSymbols();
        }
        return new DateFormatSymbolsICU(icuDfs);
    }

    public DateFormatSymbols unwrap() {
        return fIcuDfs;
    }

    @Override
    public Object clone() {
        DateFormatSymbolsICU other = (DateFormatSymbolsICU)super.clone();
        if (fIcuDfs != null) {
            // fIcuDfs must not be null except for premature instance.
            // A premature instance might be created by Java DateFormatSymbols'
            // internal cache. See #11733 for more details.
            other.fIcuDfs = (DateFormatSymbols)this.fIcuDfs.clone();
        }
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof DateFormatSymbolsICU) {
            if (this.fIcuDfs == null) {
                return ((DateFormatSymbolsICU)obj).fIcuDfs == null;
            }
            return this.fIcuDfs.equals(((DateFormatSymbolsICU)obj).fIcuDfs);
        }
        return false;
    }

    @Override
    public String[] getAmPmStrings() {
        if (fIcuDfs == null) {
            return super.getAmPmStrings();
        }
        return fIcuDfs.getAmPmStrings();
    }

    @Override
    public String[] getEras() {
        if (fIcuDfs == null) {
            return super.getEras();
        }
        return fIcuDfs.getEras();
    }

    public String getLocalePatternChars() {
        if (fIcuDfs == null) {
            return super.getLocalPatternChars();
        }
        return fIcuDfs.getLocalPatternChars();
    }

    @Override
    public String[] getMonths() {
        if (fIcuDfs == null) {
            return super.getMonths();
        }
        return fIcuDfs.getMonths();
    }

    @Override
    public String[] getShortMonths() {
        if (fIcuDfs == null) {
            return super.getShortMonths();
        }
        return fIcuDfs.getShortMonths();
    }

    @Override
    public String[] getShortWeekdays() {
        if (fIcuDfs == null) {
            return super.getShortWeekdays();
        }
        return fIcuDfs.getShortWeekdays();
    }

    @Override
    public String[] getWeekdays() {
        if (fIcuDfs == null) {
            return super.getWeekdays();
        }
        return fIcuDfs.getWeekdays();
    }

    @Override
    public String[][] getZoneStrings() {
        if (fIcuDfs == null) {
            return super.getZoneStrings();
        }
        return fIcuDfs.getZoneStrings();
    }

    @Override
    public int hashCode() {
        if (fIcuDfs == null) {
            return super.hashCode();
        }
        return fIcuDfs.hashCode();
    }

    @Override
    public void setAmPmStrings(String[] newAmpms) {
        if (fIcuDfs == null) {
            super.setAmPmStrings(newAmpms);
            return;
        }
        fIcuDfs.setAmPmStrings(newAmpms);
    }

    @Override
    public void setEras(String[] newEras) {
        if (fIcuDfs == null) {
            super.setEras(newEras);
            return;
        }
        fIcuDfs.setEras(newEras);
    }

    @Override
    public void setLocalPatternChars(String newLocalPatternChars) {
        if (fIcuDfs == null) {
            super.setLocalPatternChars(newLocalPatternChars);
            return;
        }
        fIcuDfs.setLocalPatternChars(newLocalPatternChars);
    }

    @Override
    public void setMonths(String[] newMonths) {
        if (fIcuDfs == null) {
            super.setMonths(newMonths);
            return;
        }
        fIcuDfs.setMonths(newMonths);
    }

    @Override
    public void setShortMonths(String[] newShortMonths) {
        if (fIcuDfs == null) {
            super.setShortMonths(newShortMonths);
            return;
        }
        fIcuDfs.setShortMonths(newShortMonths);
    }

    @Override
    public void setShortWeekdays(String[] newShortWeekdays) {
        if (fIcuDfs == null) {
            super.setShortWeekdays(newShortWeekdays);
            return;
        }
        fIcuDfs.setShortWeekdays(newShortWeekdays);
    }

    @Override
    public void setWeekdays(String[] newWeekdays) {
        if (fIcuDfs == null) {
            super.setWeekdays(newWeekdays);
            return;
        }
        fIcuDfs.setWeekdays(newWeekdays);
    }

    @Override
    public void setZoneStrings(String[][] newZoneStrings) {
        if (fIcuDfs == null) {
            super.setZoneStrings(newZoneStrings);
            return;
        }
        fIcuDfs.setZoneStrings(newZoneStrings);
    }
}
