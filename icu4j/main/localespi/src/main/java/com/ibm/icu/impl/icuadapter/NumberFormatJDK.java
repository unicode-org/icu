// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.icuadapter;

import java.math.RoundingMode;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;

import com.ibm.icu.impl.jdkadapter.NumberFormatICU;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.util.Currency;
import com.ibm.icu.util.CurrencyAmount;

/**
 * NumberFormatJDK is an adapter class which wraps java.text.NumberFormat and
 * implements ICU4J NumberFormat APIs.
 */
public class NumberFormatJDK extends com.ibm.icu.text.NumberFormat {

    private static final long serialVersionUID = -1739846528146803964L;

    private NumberFormat fJdkNfmt;

    private NumberFormatJDK(NumberFormat jdkNfmt) {
        fJdkNfmt = jdkNfmt;
    }

    public static com.ibm.icu.text.NumberFormat wrap(NumberFormat jdkNfmt) {
        if (jdkNfmt instanceof NumberFormatICU) {
            return ((NumberFormatICU)jdkNfmt).unwrap();
        }
        return new NumberFormatJDK(jdkNfmt);
    }

    public NumberFormat unwrap() {
        return fJdkNfmt;
    }

    @Override
    public Object clone() {
        NumberFormatJDK other = (NumberFormatJDK)super.clone();
        other.fJdkNfmt = (NumberFormat)fJdkNfmt.clone();
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof NumberFormatJDK) {
            return ((NumberFormatJDK)obj).fJdkNfmt.equals(fJdkNfmt);
        }
        return false;
    }

    //public String format(java.math.BigDecimal number)
    //public String format(BigDecimal number)

    @Override
    public StringBuffer format(java.math.BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return fJdkNfmt.format(number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(BigDecimal number, StringBuffer toAppendTo, FieldPosition pos) {
        return fJdkNfmt.format(number.toBigDecimal(), toAppendTo, pos);
    }

    @Override
    public StringBuffer format(java.math.BigInteger number, StringBuffer toAppendTo, FieldPosition pos) {
        return fJdkNfmt.format(number, toAppendTo, pos);
    }

    //public String format(java.math.BigInteger number) 

    //String format(CurrencyAmount currAmt)

    @Override
    public StringBuffer format(CurrencyAmount currAmt, StringBuffer toAppendTo, FieldPosition pos) {
        java.util.Currency save = fJdkNfmt.getCurrency();
        String currCode = currAmt.getCurrency().getCurrencyCode();
        boolean same = save.getCurrencyCode().equals(currCode);
        if (!same) {
            fJdkNfmt.setCurrency(java.util.Currency.getInstance(currCode));
        }
        fJdkNfmt.format(currAmt.getNumber(), toAppendTo, pos);
        if (!same) {
            fJdkNfmt.setCurrency(save);
        }
        return toAppendTo;
    }

    //public String format(double number)

    @Override
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        return fJdkNfmt.format(number, toAppendTo, pos);
    }

    //public String format(long number)

    @Override
    public StringBuffer format(long number, StringBuffer toAppendTo, FieldPosition pos) {
        return fJdkNfmt.format(number, toAppendTo, pos);
    }

    @Override
    public StringBuffer format(Object number, StringBuffer toAppendTo, FieldPosition pos) {
        return fJdkNfmt.format(number, toAppendTo, pos);
    }

    @Override
    public Currency getCurrency() {
        java.util.Currency jdkCurrency = fJdkNfmt.getCurrency();
        if (jdkCurrency == null) {
            return null;
        }
        return Currency.getInstance(jdkCurrency.getCurrencyCode());
    }

    //protected Currency getEffectiveCurrency()

    @Override
    public int getMaximumFractionDigits() {
        return fJdkNfmt.getMaximumFractionDigits();
    }

    @Override
    public int getMaximumIntegerDigits() {
        return fJdkNfmt.getMaximumIntegerDigits();
    }

    @Override
    public int getMinimumFractionDigits() {
        return fJdkNfmt.getMinimumFractionDigits();
    }

    public int getMinumumIntegerDigits() {
        return fJdkNfmt.getMinimumIntegerDigits();
    }

    @Override
    public int getRoundingMode() {
        RoundingMode jdkMode = fJdkNfmt.getRoundingMode();
        int icuMode = BigDecimal.ROUND_UP;

        if (jdkMode.equals(RoundingMode.CEILING)) {
            icuMode = BigDecimal.ROUND_CEILING;
        } else if (jdkMode.equals(RoundingMode.DOWN)) {
            icuMode = BigDecimal.ROUND_DOWN;
        } else if (jdkMode.equals(RoundingMode.FLOOR)) {
            icuMode = BigDecimal.ROUND_FLOOR;
        } else if (jdkMode.equals(RoundingMode.HALF_DOWN)) {
            icuMode = BigDecimal.ROUND_HALF_DOWN;
        } else if (jdkMode.equals(RoundingMode.HALF_EVEN)) {
            icuMode = BigDecimal.ROUND_HALF_EVEN;
        } else if (jdkMode.equals(RoundingMode.HALF_UP)) {
            icuMode = BigDecimal.ROUND_HALF_UP;
        } else if (jdkMode.equals(RoundingMode.UNNECESSARY)) {
            icuMode = BigDecimal.ROUND_UNNECESSARY;
        } else if (jdkMode.equals(RoundingMode.UP)) {
            icuMode = BigDecimal.ROUND_UP;
        }
        return icuMode;
    }

    @Override
    public int hashCode() {
        return fJdkNfmt.hashCode();
    }

    @Override
    public boolean isGroupingUsed() {
        return fJdkNfmt.isGroupingUsed();
    }

    @Override
    public boolean isParseIntegerOnly() {
        return fJdkNfmt.isParseIntegerOnly();
    }

    @Override
    public boolean isParseStrict() {
        // JDK NumberFormat does not support strict parsing
        return false;
    }

    @Override
    public Number parse(String text) throws ParseException {
        return fJdkNfmt.parse(text);
    }

    @Override
    public Number parse(String text, ParsePosition parsePosition) {
        return fJdkNfmt.parse(text, parsePosition);
    }

    //public Object parseObject(String source, ParsePosition parsePosition)

    @Override
    public void setCurrency(Currency theCurrency) {
        if (theCurrency == null) {
            fJdkNfmt.setCurrency(null);
            return;
        } else {
            fJdkNfmt.setCurrency(java.util.Currency.getInstance(theCurrency.getCurrencyCode()));
        }
    }

    @Override
    public void setGroupingUsed(boolean newValue) {
        fJdkNfmt.setGroupingUsed(newValue);
    }

    @Override
    public void setMaximumFractionDigits(int newValue) {
        fJdkNfmt.setMaximumFractionDigits(newValue);
    }

    @Override
    public void setMaximumIntegerDigits(int newValue) {
        fJdkNfmt.setMaximumIntegerDigits(newValue);
    }

    @Override
    public void setMinimumFractionDigits(int newValue) {
        fJdkNfmt.setMinimumFractionDigits(newValue);
    }

    @Override
    public void setMinimumIntegerDigits(int newValue) {
        fJdkNfmt.setMinimumIntegerDigits(newValue);
    }

    @Override
    public void setParseIntegerOnly(boolean value) {
        fJdkNfmt.setParseIntegerOnly(value);
    }

    @Override
    public void setParseStrict(boolean value) {
        // JDK NumberFormat does not support strict parsing - ignore this operation
    }

    @Override
    public void setRoundingMode(int roundingMode) {
        RoundingMode mode = null;
        switch (roundingMode) {
        case BigDecimal.ROUND_CEILING:
            mode = RoundingMode.CEILING;
            break;
        case BigDecimal.ROUND_DOWN:
            mode = RoundingMode.DOWN;
            break;
        case BigDecimal.ROUND_FLOOR:
            mode = RoundingMode.FLOOR;
            break;
        case BigDecimal.ROUND_HALF_DOWN:
            mode = RoundingMode.HALF_DOWN;
            break;
        case BigDecimal.ROUND_HALF_EVEN:
            mode = RoundingMode.HALF_EVEN;
            break;
        case BigDecimal.ROUND_HALF_UP:
            mode = RoundingMode.HALF_UP;
            break;
        case BigDecimal.ROUND_UNNECESSARY:
            mode = RoundingMode.UNNECESSARY;
            break;
        case BigDecimal.ROUND_UP:
            mode = RoundingMode.UP;
            break;
        }
        if (mode == null) {
            throw new IllegalArgumentException("Invalid rounding mode: " + roundingMode);
        }
        fJdkNfmt.setRoundingMode(mode);
    }
}
