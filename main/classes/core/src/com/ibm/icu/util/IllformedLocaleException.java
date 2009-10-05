/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Thrown by methods in Locale class to indicate that a locale string
 * is illformed.
 * 
 * @draft ICU 4.2
 * @provisional This API might change or be removed in a future release.
 */
public class IllformedLocaleException extends IllegalArgumentException {

    private static final long serialVersionUID = 1L;

    private int _errIdx = -1;

    /**
     * Constructs a new <code>IllformedLocaleException</code> with
     * the detail message.
     * @param msg the detail message
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public IllformedLocaleException(String msg) {
        this(msg, -1);
    }

    /**
     * Constructs a new <code>IllformedLocaleException</code> with
     * the detail message and the error index.
     * @param msg the detail message
     * @param errIdx the index where the error is found in a locale string
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public IllformedLocaleException(String msg, int errIdx) {
        super(msg + ((errIdx == -1) ? "" : " [at index " + errIdx + "]"));
        _errIdx = errIdx;
    }

    /**
     * Returns the index where the error is found in a locale string
     * @return the index where the error is found in a locale string or
     *         -1 if unknown.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public int getErrorIndex() {
        return _errIdx;
    }
}
