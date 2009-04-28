/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.math.RoundingMode;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Currency;

import com.ibm.icu.impl.icuadapter.NumberFormatJDK;
import com.ibm.icu.text.NumberFormat;

/**
 * NumberFormatICU is an adapter class which wraps ICU4J NumberFormat and
 * implements java.text.NumberFormat APIs.
 */
public class NumberFormatICU extends java.text.NumberFormat {

    private static final long serialVersionUID = 4892903815641574060L;

    private NumberFormat fIcuNfmt;

    private NumberFormatICU(NumberFormat icuNfmt) {
        fIcuNfmt = icuNfmt;
    }

    public static java.text.NumberFormat wrap(NumberFormat icuNfmt) {
        if (icuNfmt instanceof NumberFormatJDK) {
            return ((NumberFormatJDK)icuNfmt).unwrap();
        }
        return new NumberFormatICU(icuNfmt);
    }

    public NumberFormat unwrap() {
        return fIcuNfmt;
    }

    @Override
    public Object clone() {
        NumberFormatICU other = (NumberFormatICU)super.clone();
        other.fIcuNfmt = (NumberFormat)fIcuNfmt.clone();
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NumberFormatICU) {
            return ((NumberFormatICU)obj).fIcuNfmt.equals(fIcuNfmt);
        }
        return false;
    }

    //public String format(double number)

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        return fIcuNfmt.format(number, toAppendTo, pos);
    }

    //public String format(long number);

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return fIcuNfmt.format(number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        return fIcuNfmt.format(number, toAppendTo, pos);
    }

    @Override
    public Currency getCurrency() {
        com.ibm.icu.util.Currency icuCurrency = fIcuNfmt.getCurrency();
        if (icuCurrency == null) {
            return null;
        }
        return Currency.getInstance(icuCurrency.getCurrencyCode());
    }

    @Override
    public int getMaximumFractionDigits() {
        return fIcuNfmt.getMaximumFractionDigits();
    }

    @Override
    public int getMaximumIntegerDigits() {
        return fIcuNfmt.getMaximumIntegerDigits();
    }

    @Override
    public int getMinimumFractionDigits() {
        return fIcuNfmt.getMinimumFractionDigits();
    }

    @Override
    public int getMinimumIntegerDigits() {
        return fIcuNfmt.getMinimumIntegerDigits();
    }

    @Override
    public RoundingMode getRoundingMode() {
        int icuMode = fIcuNfmt.getRoundingMode();
        RoundingMode mode = RoundingMode.UP;
        switch (icuMode) {
        case com.ibm.icu.math.BigDecimal.ROUND_CEILING:
            mode = RoundingMode.CEILING;
            break;
        case com.ibm.icu.math.BigDecimal.ROUND_DOWN:
            mode = RoundingMode.DOWN;
            break;
        case com.ibm.icu.math.BigDecimal.ROUND_FLOOR:
            mode = RoundingMode.FLOOR;
            break;
        case com.ibm.icu.math.BigDecimal.ROUND_HALF_DOWN:
            mode = RoundingMode.HALF_DOWN;
            break;
        case com.ibm.icu.math.BigDecimal.ROUND_HALF_EVEN:
            mode = RoundingMode.HALF_EVEN;
            break;
        case com.ibm.icu.math.BigDecimal.ROUND_HALF_UP:
            mode = RoundingMode.HALF_UP;
            break;
        case com.ibm.icu.math.BigDecimal.ROUND_UNNECESSARY:
            mode = RoundingMode.UNNECESSARY;
            break;
        case com.ibm.icu.math.BigDecimal.ROUND_UP:
            mode = RoundingMode.UP;
            break;
        }
        return mode;
    }

    @Override
    public int hashCode() {
        return fIcuNfmt.hashCode();
    }

    @Override
    public boolean isGroupingUsed() {
        return fIcuNfmt.isGroupingUsed();
    }

    @Override
    public boolean isParseIntegerOnly() {
        return fIcuNfmt.isParseIntegerOnly();
    }

    @Override
    public Number parse(String source) throws ParseException {
        return fIcuNfmt.parse(source);
    }

    @Override
    public Number parse(String source, ParsePosition parsePosition) {
        return fIcuNfmt.parse(source, parsePosition);
    }

    //public Object parseObject(String source, ParsePosition pos)

    @Override
    public void setCurrency(Currency currency) {
        if (currency == null) {
            fIcuNfmt.setCurrency(null);
        } else {
            fIcuNfmt.setCurrency(com.ibm.icu.util.Currency.getInstance(currency.getCurrencyCode()));
        }
    }

    @Override
    public void setGroupingUsed(boolean newValue) {
        fIcuNfmt.setGroupingUsed(newValue);
    }

    @Override
    public void setMaximumFractionDigits(int newValue) {
        fIcuNfmt.setMaximumFractionDigits(newValue);
    }

    @Override
    public void setMaximumIntegerDigits(int newValue) {
        fIcuNfmt.setMaximumIntegerDigits(newValue);
    }

    @Override
    public void setMinimumFractionDigits(int newValue) {
        fIcuNfmt.setMinimumFractionDigits(newValue);
    }

    @Override
    public void setMinimumIntegerDigits(int newValue) {
        fIcuNfmt.setMinimumIntegerDigits(newValue);
    }

    @Override
    public void setParseIntegerOnly(boolean value) {
        fIcuNfmt.setParseIntegerOnly(value);
    }

    @Override
    public void setRoundingMode(RoundingMode roundingMode) {
        if (roundingMode.equals(RoundingMode.CEILING)) {
            fIcuNfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_CEILING);
        } else if (roundingMode.equals(RoundingMode.DOWN)) {
            fIcuNfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_DOWN);
        } else if (roundingMode.equals(RoundingMode.FLOOR)) {
            fIcuNfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_FLOOR);
        } else if (roundingMode.equals(RoundingMode.HALF_DOWN)) {
            fIcuNfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_HALF_DOWN);
        } else if (roundingMode.equals(RoundingMode.HALF_EVEN)) {
            fIcuNfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_HALF_EVEN);
        } else if (roundingMode.equals(RoundingMode.HALF_UP)) {
            fIcuNfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_HALF_UP);
        } else if (roundingMode.equals(RoundingMode.UNNECESSARY)) {
            fIcuNfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_UNNECESSARY);
        } else if (roundingMode.equals(RoundingMode.UP)) {
            fIcuNfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_UP);
        } else {
            throw new IllegalArgumentException("Invalid rounding mode was specified.");
        }
    }
}
