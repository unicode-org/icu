/*
*******************************************************************************
*   Copyright (C) 2007-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
package com.ibm.icu.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.MissingResourceException;

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

    private char[] digits;
    private char zeroDigit; // For backwards compatibility
    private char minusSign;
    private boolean positiveOnly = false;

    private transient char[] decimalBuf = new char[20]; // 20 digits is good enough to store Long.MAX_VALUE

    private static SimpleCache<ULocale, char[]> CACHE = new SimpleCache<ULocale, char[]>();

    private int maxIntDigits;
    private int minIntDigits;

    public DateNumberFormat(ULocale loc, String digitString, String nsName) {
        initialize(loc,digitString,nsName);
    }

    public DateNumberFormat(ULocale loc, char zeroDigit, String nsName) {
        StringBuffer buf = new StringBuffer();
        for ( int i = 0 ; i < 10 ; i++ ) {
            buf.append((char)(zeroDigit+i));
        }
        initialize(loc,buf.toString(),nsName);
    }

    private void initialize(ULocale loc,String digitString,String nsName) {
        char[] elems = CACHE.get(loc);
        if (elems == null) {
            // Missed cache
            String minusString;
            ICUResourceBundle rb = (ICUResourceBundle)UResourceBundle.getBundleInstance(ICUResourceBundle.ICU_BASE_NAME, loc);
            try {
                minusString = rb.getStringWithFallback("NumberElements/"+nsName+"/symbols/minusSign");
            } catch (MissingResourceException ex) {
                if ( !nsName.equals("latn") ) {
                    try {
                       minusString = rb.getStringWithFallback("NumberElements/latn/symbols/minusSign");                 
                    } catch (MissingResourceException ex1) {
                        minusString = "-";
                    }
                } else {
                    minusString = "-";
                }
            }
            elems = new char[11];
            for ( int i = 0 ; i < 10 ; i++ ) {
                 elems[i] = digitString.charAt(i);
            }
            elems[10] = minusString.charAt(0);
            CACHE.put(loc, elems);
        }
        
        digits = new char[10];
        System.arraycopy(elems, 0, digits, 0, 10);
        minusSign = elems[10];
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
        if ( digits != null ) {
            return digits[0];
        } else {
            return zeroDigit;
        }
    }

    public void setZeroDigit(char zero) {
        if ( digits == null ) {
            digits = new char[10];
        }
        digits[0] = zero;
        if (Character.digit(zero,10) == 0) {
            for ( int i = 1 ; i < 10 ; i++ ) {
                digits[i] = (char)(zero+i);
            }
        }
 
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
            decimalBuf[index] = digits[(number % 10)];
            number /= 10;
            if (index == 0 || number == 0) {
                break;
            }
            index--;
        }
        int padding = minIntDigits - (limit - index);
        for (; padding > 0; padding--) {
            decimalBuf[--index] = digits[0];
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
    private static final long PARSE_THRESHOLD = 922337203685477579L; // (Long.MAX_VALUE / 10) - 1

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
                int digit = ch - digits[0];
                if (digit < 0 || 9 < digit) {
                    digit = UCharacter.digit(ch);
                }
                if (digit < 0 || 9 < digit) {
                    for ( digit = 0 ; digit < 10 ; digit++ ) {
                        if ( ch == digits[digit]) {
                            break;
                        }
                    }
                }
                if (0 <= digit && digit <= 9 && num < PARSE_THRESHOLD) {
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

        for (int i = 0 ; i < 10 ; i++) {
            char check1, check2;
            if ( digits != null ) {
                check1 = digits[i];
            } else {
                check1 = (char)(zeroDigit+i);
            }
            if ( other.digits != null ) {
                check2 = other.digits[i];
            } else {
                check2 = (char)(other.zeroDigit+i);
            }
            
            if (check1 != check2) {
                return false;
            }
        }

        return (this.maxIntDigits == other.maxIntDigits
                && this.minIntDigits == other.minIntDigits
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
