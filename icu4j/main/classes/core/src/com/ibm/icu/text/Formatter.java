/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.FieldPosition;

/**
 * General Interface for formatting.
 * @author markdavis
 */
public interface Formatter<T, U extends Appendable> {
    /**
     * Formats an object to produce a string. This is equivalent to
     * <blockquote>
     * {@link #format(Object, Appendable, FieldPosition)}<code>(obj,
     *         new StringBuilder(), new FieldPosition(0)).toString();</code>
     * </blockquote>
     * 
     * @param obj    The object to format
     * @return       Formatted string.
     * @exception IllegalArgumentException if the Format cannot format the given
     *            object
     * @draft ICU 4.4
     */
    public String format (T obj);

    /**
     * Formats an object and appends the resulting text to a given {@link java.lang.Appendable}.
     * If the <code>pos</code> argument identifies a field used by the format,
     * then its indices are set to the beginning and end of the first such
     * field encountered.
     *
     * @param obj    The object to format
     * @param toAppendTo    where the text is to be appended
     * @param pos    A <code>FieldPosition</code> identifying a field
     *               in the formatted text
     * @return       the {@link java.lang.Appendable} passed in as <code>toAppendTo</code>,
     *               with formatted text appended
     * @exception NullPointerException if <code>toAppendTo</code> or
     *            <code>pos</code> is null
     * @exception IllegalArgumentException if the Format cannot format the given
     *            object. If the Appendable throws an exception, then the cause is that exception.
     * @draft ICU 4.4
     */
    public U format(T obj, U toAppendTo, FieldPosition pos);
}
