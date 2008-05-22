/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.util.Currency;

import com.ibm.icu.text.DecimalFormatSymbols;

/**
 * DecimalFormatSymbolsICU is an adapter class which wraps ICU4J DecimalFormatSymbols and
 * implements java.text.DecimalFormatSymbols APIs.
 */
public class DecimalFormatSymbolsICU extends java.text.DecimalFormatSymbols {

    private static final long serialVersionUID = -8226875908479009580L;

    private DecimalFormatSymbols fIcuDecfs;

    private DecimalFormatSymbolsICU(DecimalFormatSymbols icuDecfs) {
        fIcuDecfs = icuDecfs;
    }

    public static java.text.DecimalFormatSymbols wrap(DecimalFormatSymbols icuDecfs) {
        return new DecimalFormatSymbolsICU(icuDecfs);
    }

    public DecimalFormatSymbols unwrap() {
        return fIcuDecfs;
    }

    @Override
    public Object clone() {
        DecimalFormatSymbolsICU other = (DecimalFormatSymbolsICU)super.clone();
        other.fIcuDecfs = (DecimalFormatSymbols)fIcuDecfs.clone();
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DecimalFormatSymbolsICU) {
            return ((DecimalFormatSymbolsICU)obj).fIcuDecfs.equals(fIcuDecfs);
        }
        return false;
    }

    @Override
    public Currency getCurrency() {
        com.ibm.icu.util.Currency icuCurrency = fIcuDecfs.getCurrency();
        if (icuCurrency == null) {
            return null;
        }
        return Currency.getInstance(icuCurrency.getCurrencyCode());
    }

    @Override
    public String getCurrencySymbol() {
        return fIcuDecfs.getCurrencySymbol();
    }

    @Override
    public char getDecimalSeparator() {
        return fIcuDecfs.getDecimalSeparator();
    }

    @Override
    public char getDigit() {
        return fIcuDecfs.getDigit();
    }

    @Override
    public String getExponentSeparator() {
        return fIcuDecfs.getExponentSeparator();
    }

    @Override
    public char getGroupingSeparator() {
        return fIcuDecfs.getGroupingSeparator();
    }

    @Override
    public String getInfinity() {
        return fIcuDecfs.getInfinity();
    }

    @Override
    public String getInternationalCurrencySymbol() {
        return fIcuDecfs.getInternationalCurrencySymbol();
    }

    @Override
    public char getMinusSign() {
        return fIcuDecfs.getMinusSign();
    }

    @Override
    public char getMonetaryDecimalSeparator() {
        return fIcuDecfs.getMonetaryDecimalSeparator();
    }

    @Override
    public String getNaN() {
        return fIcuDecfs.getNaN();
    }

    @Override
    public char getPatternSeparator() {
        return fIcuDecfs.getPatternSeparator();
    }

    @Override
    public char getPercent() {
        return fIcuDecfs.getPercent();
    }

    @Override
    public char getPerMill() {
        return fIcuDecfs.getPerMill();
    }

    @Override
    public char getZeroDigit() {
        return fIcuDecfs.getZeroDigit();
    }

    @Override
    public int hashCode() {
        return fIcuDecfs.hashCode();
    }
}
