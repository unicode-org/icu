/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.AttributedCharacterIterator;

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
        return new ScientificFormatHelper(ScientificNumberFormatter.getPreExponent(dfs));
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
        return format(
                iterator,
                new ScientificNumberFormatter.MarkupStyle(
                        beginMarkup.toString(), endMarkup.toString()));
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
        return format(iterator, ScientificNumberFormatter.SUPER_SCRIPT);
    }
    
    private String format(
            AttributedCharacterIterator iterator, ScientificNumberFormatter.Style option) {
        return option.format(iterator, preExponent);
    }
}
