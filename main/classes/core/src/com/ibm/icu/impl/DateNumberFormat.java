/*
*******************************************************************************
*   Copyright (C) 2007-2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
package com.ibm.icu.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.ParsePosition;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.math.BigDecimal;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.UResourceBundle;

/*
 * NumberFormat implementation dedicated/optimized for DateFormat,
 * used by SimpleDateFormat implementation.
 */
public final class DateNumberFormat extends NumberFormat {

    private static final long serialVersionUID = -6315692826916346953L;

    private char zeroDigit;
    private char minusSign;
    private boolean positiveOnly = false;

    private transient char[] decimalBuf = new char[20]; // 20 digits is good enough to store Long.MAX_VALUE

    private static SimpleCache<ULocale, char[]> CACHE = new SimpleCache<ULocale, char[]>();

    private int maxIntDigits;
    private int minIntDigits;
 
    public DateNumberFormat(ULocale loc, char zeroDigitIn) {
        initialize(loc,zeroDigitIn);
    }

/*    public DateNumberFormat(char zeroDigit, char minusSign) {
        this.zeroDigit = zeroDigit;
        this.minusSign = minusSign;
    }
*/

    private void initialize(ULocale loc,char zeroDigitIn) {
        char[] elems = (char[])CACHE.get(loc);
        if (elems == null) {
            // Missed cache
            ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, loc);
            String[] numberElements = rb.getStringArray("NumberElements");
            elems = new char[2];
            elems[0] = zeroDigitIn;
            elems[1] = numberElements[6].charAt(0);
            CACHE.put(loc, elems);
        }
        zeroDigit = elems[0];
        minusSign = elems[1];
    }

    public void setMaximumIntegerDigits(int newValue) {
        maxIntDigits = newValue;
    }

    public int getMaximumIntegerDigits() {
        return maxIntDigits;
    }

    public void setMinimumIntegerDigits(int newValue) {
        minIntDigits = newValue;
    }

    public int getMinimumIntegerDigits() {
        return minIntDigits;
    }

    /* For supporting SimpleDateFormat.parseInt */
    public void setParsePositiveOnly(boolean isPositiveOnly) {
        positiveOnly = isPositiveOnly;
    }

    public char getZeroDigit() {
        return zeroDigit;
    }

    public void setZeroDigit(char zero) {
        zeroDigit = zero;
    }

    public StringBuffer format(double number, StringBuffer toAppendTo,
            FieldPosition pos) {
        throw new UnsupportedOperationException("StringBuffer format(double, StringBuffer, FieldPostion) is not implemented");
    }

    public StringBuffer format(long numberL, StringBuffer toAppendTo,
            FieldPosition pos) {

        if (numberL < 0) {
            // negative
            toAppendTo.append(minusSign);
        }

        // Note: NumberFormat used by DateFormat only uses int numbers.
        // Remainder operation on 32bit platform using long is significantly slower
        // than int.  So, this method casts long number into int.
        int number = (int)numberL;

        int limit = decimalBuf.length < maxIntDigits ? decimalBuf.length : maxIntDigits;
        int index = limit - 1;
        while (true) {
            decimalBuf[index] = (char)((number % 10) + zeroDigit);
            number /= 10;
            if (index == 0 || number == 0) {
                break;
            }
            index--;
        }
        int padding = minIntDigits - (limit - index);
        for (; padding > 0; padding--) {
            decimalBuf[--index] = zeroDigit;
        }
        int length = limit - index;
        toAppendTo.append(decimalBuf, index, length);
        pos.setBeginIndex(0);
        if (pos.getField() == NumberFormat.INTEGER_FIELD) {
            pos.setEndIndex(length);
        } else {
            pos.setEndIndex(0);
        }
        return toAppendTo;
    }
    
    public StringBuffer format(BigInteger number, StringBuffer toAppendTo,
            FieldPosition pos) {
        throw new UnsupportedOperationException("StringBuffer format(BigInteger, StringBuffer, FieldPostion) is not implemented");
    }

    public StringBuffer format(java.math.BigDecimal number, StringBuffer toAppendTo,
            FieldPosition pos) {
        throw new UnsupportedOperationException("StringBuffer format(BigDecimal, StringBuffer, FieldPostion) is not implemented");
    }

    public StringBuffer format(BigDecimal number,
            StringBuffer toAppendTo, FieldPosition pos) {
        throw new UnsupportedOperationException("StringBuffer format(BigDecimal, StringBuffer, FieldPostion) is not implemented");
    }

    /*
     * Note: This method only parse integer numbers which can be represented by long
     */
    public Number parse(String text, ParsePosition parsePosition) {
        long num = 0;
        boolean sawNumber = false;
        boolean negative = false;
        int base = parsePosition.getIndex();
        int offset = 0;
        for (; base + offset < text.length(); offset++) {
            char ch = text.charAt(base + offset);
            if (offset == 0 && ch == minusSign) {
                if (positiveOnly) {
                    break;
                }
                negative = true;
            } else {
                int digit = ch - zeroDigit;
                if (digit < 0 || 9 < digit) {
                    digit = UCharacter.digit(ch);
                }
                if (0 <= digit && digit <= 9) {
                    sawNumber = true;
                    num = num * 10 + digit;
                } else {
                    break;
                }
            }
        }
        Number result = null;
        if (sawNumber) {
            num = negative ? num * (-1) : num;
            result = new Long(num);
            parsePosition.setIndex(base + offset);
        }
        return result;
    }

    public boolean equals(Object obj) {
        if (obj == null || !super.equals(obj) || !(obj instanceof DateNumberFormat)) {
            return false;
        }
        DateNumberFormat other = (DateNumberFormat)obj;
        return (this.maxIntDigits == other.maxIntDigits
                && this.minIntDigits == other.minIntDigits
                && this.zeroDigit == other.zeroDigit
                && this.minusSign == other.minusSign
                && this.positiveOnly == other.positiveOnly);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        // re-allocate the work buffer
        decimalBuf = new char[20];
    }
}

//eof
