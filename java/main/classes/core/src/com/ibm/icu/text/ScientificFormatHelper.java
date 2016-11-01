/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.AttributedCharacterIterator;
import java.text.AttributedCharacterIterator.Attribute;
import java.text.CharacterIterator;
import java.util.Map;

import com.ibm.icu.lang.UCharacter;

/**
 * A helper class for formatting in user-friendly scientific notation.
 * 
 * ScientificFormatHelper instances are immutable and thread-safe. However, the
 * AttributedCharacterIterator instances that ScientificFormatHelper instances format must
 * not be shared across multiple threads.
 *
 * Sample code:
 * <pre>
 * DecimalFormat decfmt = (DecimalFormat) NumberFormat.getScientificInstance(new ULocale("en"));
 * AttributedCharacterIterator iterator = decfmt.formatToCharacterIterator(1.23456e-78);
 * ScientificFormatHelper helper = ScientificFormatHelper.getInstance(
                decfmt.getDecimalFormatSymbols());
 * <pre>
 * // Output: "1.23456×10<sup>-78</sup>"
 *  System.out.println(helper.insertMarkup(iterator, "<sup>", "</sup>"));
 * </pre>
 *
 * @see NumberFormat
 * @draft ICU 54
 * @provisional This API might change or be removed in a future release.
 *
 */
public final class ScientificFormatHelper {
    
    private static final char[] SUPERSCRIPT_DIGITS = {
        0x2070, 0xB9, 0xB2, 0xB3, 0x2074, 0x2075, 0x2076, 0x2077, 0x2078, 0x2079
    };

    private static final char SUPERSCRIPT_PLUS_SIGN = 0x207A;
    private static final char SUPERSCRIPT_MINUS_SIGN = 0x207B;
    
    private final String preExponent;
    
    private ScientificFormatHelper(String preExponent) {
        this.preExponent = preExponent;
    }

    /**
     * Returns a new ScientificFormatHelper.
     * @param dfs comes from the DecimalFormat instance used for default scientific notation.
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public static ScientificFormatHelper getInstance(DecimalFormatSymbols dfs) {
        StringBuilder preExponent = new StringBuilder();
        preExponent.append(getMultiplicationSymbol(dfs));
        char[] digits = dfs.getDigits();
        preExponent.append(digits[1]).append(digits[0]);
        return new ScientificFormatHelper(preExponent.toString());
    }
    
    private static String getMultiplicationSymbol(DecimalFormatSymbols dfs) {
        return dfs.getExponentMultiplicationSign();
    }

    /**
     * Makes scientific notation user-friendly by surrounding exponent with
     * html to make it superscript.
     * @param iterator the value that DecimalFormat.formatToCharacterIterator() returned.
     * @param beginMarkup the start html for the exponent e.g "<sup>"
     * @param endMarkup the end html for the exponent e.g "</sup>"
     * @return the user-friendly scientific notation.
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public String insertMarkup(
            AttributedCharacterIterator iterator,
            CharSequence beginMarkup,
            CharSequence endMarkup) {
        int copyFromOffset = 0;
        StringBuilder result = new StringBuilder();
        boolean exponentSymbolFieldPresent = false;
        boolean exponentFieldPresent = false;
        for (
                iterator.first();
                iterator.current() != CharacterIterator.DONE;
            ) {
            Map<Attribute, Object> attributeSet = iterator.getAttributes();
            if (attributeSet.containsKey(NumberFormat.Field.EXPONENT_SYMBOL)) {
                exponentSymbolFieldPresent = true;
                append(
                        iterator,
                        copyFromOffset,
                        iterator.getRunStart(NumberFormat.Field.EXPONENT_SYMBOL),
                        result);
                copyFromOffset = iterator.getRunLimit(NumberFormat.Field.EXPONENT_SYMBOL);
                iterator.setIndex(copyFromOffset);
                result.append(preExponent);
                result.append(beginMarkup);
            } else if (attributeSet.containsKey(NumberFormat.Field.EXPONENT)) {
                exponentFieldPresent = true;
                int limit = iterator.getRunLimit(NumberFormat.Field.EXPONENT);
                append(
                        iterator,
                        copyFromOffset,
                        limit,
                        result);
                copyFromOffset = limit;
                iterator.setIndex(copyFromOffset);
                result.append(endMarkup);
            } else {
                iterator.next();
            }
        }
        if (!exponentSymbolFieldPresent || !exponentFieldPresent) {
            throw new IllegalArgumentException("Must start with standard e notation.");
        }
        append(iterator, copyFromOffset, iterator.getEndIndex(), result);
        return result.toString();
    }

    
    private static void append(
            AttributedCharacterIterator iterator,
            int start,
            int limit,
            StringBuilder result) {
        int oldIndex = iterator.getIndex();
        iterator.setIndex(start);
        for (int i = start; i < limit; i++) {
            result.append(iterator.current());
            iterator.next();
        }
        iterator.setIndex(oldIndex);
    }

    /**
     * Makes scientific notation user-friendly by using specific code points
     * for superscript 0..9, -, and + in the exponent rather than by using
     * html.
     * @param iterator the value that DecimalFormat.formatToCharacterIterator() returned.
     * @return the user-friendly scientific notation.
     * @draft ICU 54
     * @provisional This API might change or be removed in a future release.
     */
    public String toSuperscriptExponentDigits(AttributedCharacterIterator iterator) {
        int copyFromOffset = 0;
        StringBuilder result = new StringBuilder();
        boolean exponentSymbolFieldPresent = false;
        boolean exponentFieldPresent = false;
        for (
                iterator.first();
                iterator.current() != CharacterIterator.DONE;
            ) {
            Map<Attribute, Object> attributeSet = iterator.getAttributes();
            if (attributeSet.containsKey(NumberFormat.Field.EXPONENT_SYMBOL)) {
                exponentSymbolFieldPresent = true;
                append(
                        iterator,
                        copyFromOffset,
                        iterator.getRunStart(NumberFormat.Field.EXPONENT_SYMBOL),
                        result);
                copyFromOffset = iterator.getRunLimit(NumberFormat.Field.EXPONENT_SYMBOL);
                iterator.setIndex(copyFromOffset);
                result.append(preExponent);
            } else if (attributeSet.containsKey(NumberFormat.Field.EXPONENT_SIGN)) {
                int start = iterator.getRunStart(NumberFormat.Field.EXPONENT_SIGN);
                int limit = iterator.getRunLimit(NumberFormat.Field.EXPONENT_SIGN);
                int aChar = char32AtAndAdvance(iterator);
                if (DecimalFormat.minusSigns.contains(aChar)) {
                    append(
                            iterator,
                            copyFromOffset,
                            start,
                            result);
                    result.append(SUPERSCRIPT_MINUS_SIGN);
                } else if (DecimalFormat.plusSigns.contains(aChar)) {
                    append(
                            iterator,
                            copyFromOffset,
                            start,
                            result);
                    result.append(SUPERSCRIPT_PLUS_SIGN);
                } else {
                    throw new IllegalArgumentException();
                }
                copyFromOffset = limit;
                iterator.setIndex(copyFromOffset);
            } else if (attributeSet.containsKey(NumberFormat.Field.EXPONENT)) {
                exponentFieldPresent = true;
                int start = iterator.getRunStart(NumberFormat.Field.EXPONENT);
                int limit = iterator.getRunLimit(NumberFormat.Field.EXPONENT);
                append(
                        iterator,
                        copyFromOffset,
                        start,
                        result);
                copyAsSuperscript(iterator, start, limit, result);
                copyFromOffset = limit;
                iterator.setIndex(copyFromOffset);
            } else {
                iterator.next();
            }
        } 
        if (!exponentSymbolFieldPresent || !exponentFieldPresent) {
            throw new IllegalArgumentException("Must start with standard e notation.");
        }
        append(iterator, copyFromOffset, iterator.getEndIndex(), result);
        return result.toString();
    }

    private static void copyAsSuperscript(
            AttributedCharacterIterator iterator, int start, int limit, StringBuilder result) {
        int oldIndex = iterator.getIndex();
        iterator.setIndex(start);
        while (iterator.getIndex() < limit) {
            int aChar = char32AtAndAdvance(iterator);
            int digit = UCharacter.digit(aChar);
            if (digit < 0) {
                throw new IllegalArgumentException();
            }
            result.append(SUPERSCRIPT_DIGITS[digit]);
        }
        iterator.setIndex(oldIndex);
    }

    private static int char32AtAndAdvance(AttributedCharacterIterator iterator) {
        char c1 = iterator.current();
        iterator.next();
        if (UCharacter.isHighSurrogate(c1)) {
            char c2 = iterator.current();
            if (c2 != CharacterIterator.DONE) {
                if (UCharacter.isLowSurrogate(c2)) {
                    iterator.next();
                    return UCharacter.toCodePoint(c1, c2);
                }
            }
        }
        return c1;
    }

}
