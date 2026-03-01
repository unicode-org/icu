// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2008-2011, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.jdkadapter;

import java.math.RoundingMode;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.text.CharacterIterator;
import java.text.DecimalFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;

/**
 * DecimalFormatICU is an adapter class which wraps ICU4J DecimalFormat and
 * implements java.text.DecimalFormat APIs.
 */
public class DecimalFormatICU extends java.text.DecimalFormat {

    private static final long serialVersionUID = 6441573352964019403L;

    private DecimalFormat fIcuDecfmt;

    private DecimalFormatICU(DecimalFormat icuDecfmt) {
        fIcuDecfmt = icuDecfmt;
    }

    public static java.text.DecimalFormat wrap(DecimalFormat icuDecfmt) {
        return new DecimalFormatICU(icuDecfmt);
    }

    public DecimalFormat unwrap() {
        return fIcuDecfmt;
    }

    // Methods overriding java.text.DecimalFormat
    @Override
    public void applyLocalizedPattern(String pattern) {
        fIcuDecfmt.applyLocalizedPattern(pattern);
    }

    @Override
    public void applyPattern(String pattern) {
        fIcuDecfmt.applyPattern(pattern);
    }

    @Override
    public Object clone() {
        DecimalFormatICU other = (DecimalFormatICU)super.clone();
        other.fIcuDecfmt = (DecimalFormat)fIcuDecfmt.clone();
        return other;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DecimalFormatICU) {
            return ((DecimalFormatICU)obj).fIcuDecfmt.equals(fIcuDecfmt);
        }
        return false;
    }

    @Override
    public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
        return fIcuDecfmt.format(number, result, fieldPosition);
    }

    @Override
    public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
        return fIcuDecfmt.format(number, result, fieldPosition);
    }

    @Override
    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        AttributedCharacterIterator aci = fIcuDecfmt.formatToCharacterIterator(obj);

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
    public Currency getCurrency() {
        com.ibm.icu.util.Currency icuCurrency = fIcuDecfmt.getCurrency();
        if (icuCurrency == null) {
            return null;
        }
        return Currency.getInstance(icuCurrency.getCurrencyCode());
    }

    @Override
    public DecimalFormatSymbols getDecimalFormatSymbols() {
        return DecimalFormatSymbolsICU.wrap(fIcuDecfmt.getDecimalFormatSymbols());
    }

    @Override
    public int getGroupingSize() {
        return fIcuDecfmt.getGroupingSize();
    }

    @Override
    public int getMaximumFractionDigits() {
        return fIcuDecfmt.getMaximumFractionDigits();
    }

    @Override
    public int getMaximumIntegerDigits() {
        return fIcuDecfmt.getMaximumIntegerDigits();
    }

    @Override
    public int getMinimumFractionDigits() {
        return fIcuDecfmt.getMinimumFractionDigits();
    }

    @Override
    public int getMinimumIntegerDigits() {
        return fIcuDecfmt.getMinimumIntegerDigits();
    }

    @Override
    public int getMultiplier() {
        return fIcuDecfmt.getMultiplier();
    }

    @Override
    public String getNegativePrefix() {
        return fIcuDecfmt.getNegativePrefix();
    }

    @Override
    public String getNegativeSuffix() {
        return fIcuDecfmt.getNegativeSuffix();
    }

    @Override
    public String getPositivePrefix() {
        return fIcuDecfmt.getPositivePrefix();
    }

    @Override
    public String getPositiveSuffix() {
        return fIcuDecfmt.getPositiveSuffix();
    }

    @Override
    public RoundingMode getRoundingMode() {
        int icuMode = fIcuDecfmt.getRoundingMode();
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
        return fIcuDecfmt.hashCode();
    }

    @Override
    public boolean isDecimalSeparatorAlwaysShown() {
        return fIcuDecfmt.isDecimalSeparatorAlwaysShown();
    }

    @Override
    public boolean isParseBigDecimal() {
        return fIcuDecfmt.isParseBigDecimal();
    }

    @Override
    public Number parse(String text, ParsePosition pos) {
        return fIcuDecfmt.parse(text, pos);
    }

    @Override
    public void setCurrency(Currency currency) {
        if (currency == null) {
            fIcuDecfmt.setCurrency(null);
        } else {
            fIcuDecfmt.setCurrency(com.ibm.icu.util.Currency.getInstance(currency.getCurrencyCode()));
        }
    }

    @Override
    public void setDecimalFormatSymbols(DecimalFormatSymbols newSymbols) {
        com.ibm.icu.text.DecimalFormatSymbols icuDecfs = null;
        if (newSymbols instanceof DecimalFormatSymbolsICU) {
            icuDecfs = ((DecimalFormatSymbolsICU)newSymbols).unwrap();
        } else {
            icuDecfs = fIcuDecfmt.getDecimalFormatSymbols();

            Currency currency = newSymbols.getCurrency();
            if (currency == null) {
                icuDecfs.setCurrency(null);
            } else {
                icuDecfs.setCurrency(com.ibm.icu.util.Currency.getInstance(currency.getCurrencyCode()));
            }

            // Copy symbols
            icuDecfs.setCurrencySymbol(newSymbols.getCurrencySymbol());
            icuDecfs.setDecimalSeparator(newSymbols.getDecimalSeparator());
            icuDecfs.setDigit(newSymbols.getDigit());
            icuDecfs.setExponentSeparator(newSymbols.getExponentSeparator());
            icuDecfs.setGroupingSeparator(newSymbols.getGroupingSeparator());
            icuDecfs.setInfinity(newSymbols.getInfinity());
            icuDecfs.setInternationalCurrencySymbol(newSymbols.getInternationalCurrencySymbol());
            icuDecfs.setMinusSign(newSymbols.getMinusSign());
            icuDecfs.setMonetaryDecimalSeparator(newSymbols.getMonetaryDecimalSeparator());
            icuDecfs.setNaN(newSymbols.getNaN());
            icuDecfs.setPatternSeparator(newSymbols.getPatternSeparator());
            icuDecfs.setPercent(newSymbols.getPercent());
            icuDecfs.setPerMill(newSymbols.getPerMill());
            icuDecfs.setZeroDigit(newSymbols.getZeroDigit());
        }
        fIcuDecfmt.setDecimalFormatSymbols(icuDecfs);
    }

    @Override
    public void setDecimalSeparatorAlwaysShown(boolean newValue) {
        if (fIcuDecfmt != null) {
            fIcuDecfmt.setDecimalSeparatorAlwaysShown(newValue);
        }
    }

    @Override
    public void setGroupingSize(int newValue) {
        if (fIcuDecfmt != null) {
            fIcuDecfmt.setGroupingSize(newValue);
        }
    }

    @Override
    public void setMaximumFractionDigits(int newValue) {
        if (fIcuDecfmt != null) {
            fIcuDecfmt.setMaximumFractionDigits(newValue);
        }
    }

    @Override
    public void setMaximumIntegerDigits(int newValue) {
        if (fIcuDecfmt != null) {
            fIcuDecfmt.setMaximumIntegerDigits(newValue);
        }
    }

    @Override
    public void setMinimumFractionDigits(int newValue) {
        if (fIcuDecfmt != null) {
            fIcuDecfmt.setMinimumFractionDigits(newValue);
        }
    }

    @Override
    public void setMinimumIntegerDigits(int newValue) {
        if (fIcuDecfmt != null) {
            fIcuDecfmt.setMinimumIntegerDigits(newValue);
        }
    }

    @Override
    public void setMultiplier(int newValue) {
        fIcuDecfmt.setMultiplier(newValue);
    }

    @Override
    public void setNegativePrefix(String newValue) {
        fIcuDecfmt.setNegativePrefix(newValue);
    }

    @Override
    public void setNegativeSuffix(String newValue) {
        fIcuDecfmt.setNegativeSuffix(newValue);
    }

    @Override
    public void setParseBigDecimal(boolean newValue) {
        fIcuDecfmt.setParseBigDecimal(newValue);
    }

    @Override
    public void setPositivePrefix(String newValue) {
        fIcuDecfmt.setPositivePrefix(newValue);
    }

    @Override
    public void setPositiveSuffix(String newValue) {
        fIcuDecfmt.setPositiveSuffix(newValue);
    }

    @Override
    public void setRoundingMode(RoundingMode roundingMode) {
        if (roundingMode.equals(RoundingMode.CEILING)) {
            fIcuDecfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_CEILING);
        } else if (roundingMode.equals(RoundingMode.DOWN)) {
            fIcuDecfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_DOWN);
        } else if (roundingMode.equals(RoundingMode.FLOOR)) {
            fIcuDecfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_FLOOR);
        } else if (roundingMode.equals(RoundingMode.HALF_DOWN)) {
            fIcuDecfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_HALF_DOWN);
        } else if (roundingMode.equals(RoundingMode.HALF_EVEN)) {
            fIcuDecfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_HALF_EVEN);
        } else if (roundingMode.equals(RoundingMode.HALF_UP)) {
            fIcuDecfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_HALF_UP);
        } else if (roundingMode.equals(RoundingMode.UNNECESSARY)) {
            fIcuDecfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_UNNECESSARY);
        } else if (roundingMode.equals(RoundingMode.UP)) {
            fIcuDecfmt.setRoundingMode(com.ibm.icu.math.BigDecimal.ROUND_UP);
        } else {
            throw new IllegalArgumentException("Invalid rounding mode was specified.");
        }
    }

    @Override
    public String toLocalizedPattern() {
        return fIcuDecfmt.toLocalizedPattern();
    }

    @Override
    public String toPattern() {
        return fIcuDecfmt.toPattern();
    }

    // Methods overriding java.text.NumberFormat

    @Override
    public boolean isGroupingUsed() {
        return fIcuDecfmt.isGroupingUsed();
    }

    @Override
    public boolean isParseIntegerOnly() {
        return fIcuDecfmt.isParseIntegerOnly();
    }

    @Override
    public void setGroupingUsed(boolean newValue) {
        if (fIcuDecfmt != null) {
            fIcuDecfmt.setGroupingUsed(newValue);
        }
    }

    @Override
    public void setParseIntegerOnly(boolean value) {
        fIcuDecfmt.setParseIntegerOnly(value);
    }

    private static AttributedCharacterIterator.Attribute mapAttribute(AttributedCharacterIterator.Attribute icuAttribute) {
        AttributedCharacterIterator.Attribute jdkAttribute = icuAttribute;

        if (icuAttribute == NumberFormat.Field.CURRENCY) {
            jdkAttribute = java.text.NumberFormat.Field.CURRENCY;
        } else if (icuAttribute == NumberFormat.Field.DECIMAL_SEPARATOR) {
            jdkAttribute = java.text.NumberFormat.Field.DECIMAL_SEPARATOR;
        } else if (icuAttribute == NumberFormat.Field.EXPONENT) {
            jdkAttribute = java.text.NumberFormat.Field.EXPONENT;
        } else if (icuAttribute == NumberFormat.Field.EXPONENT_SIGN) {
            jdkAttribute = java.text.NumberFormat.Field.EXPONENT_SIGN;
        } else if (icuAttribute == NumberFormat.Field.EXPONENT_SYMBOL) {
            jdkAttribute = java.text.NumberFormat.Field.EXPONENT_SYMBOL;
        } else if (icuAttribute == NumberFormat.Field.FRACTION) {
            jdkAttribute = java.text.NumberFormat.Field.FRACTION;
        } else if (icuAttribute == NumberFormat.Field.GROUPING_SEPARATOR) {
            jdkAttribute = java.text.NumberFormat.Field.GROUPING_SEPARATOR;
        } else if (icuAttribute == NumberFormat.Field.INTEGER) {
            jdkAttribute = java.text.NumberFormat.Field.INTEGER;
        } else if (icuAttribute == NumberFormat.Field.PERCENT) {
            jdkAttribute = java.text.NumberFormat.Field.PERCENT;
        } else if (icuAttribute == NumberFormat.Field.PERMILLE) {
            jdkAttribute = java.text.NumberFormat.Field.PERMILLE;
        } else if (icuAttribute == NumberFormat.Field.SIGN) {
            jdkAttribute = java.text.NumberFormat.Field.SIGN;
        }

        return jdkAttribute;
    }

}
