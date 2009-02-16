/*
 *******************************************************************************
 * Copyright (C) 2009, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * This exception is used to indicate that a locale field has an
 * invalid syntax.
 * 
 * @draft ICU 4.2
 * @provisional This API might change or be removed in a future release.
 */
public class InvalidLocaleException extends Exception {

    private static final long serialVersionUID = 4129352440101206300L;

    /**
     * Constructs the exception with the given message.
     * @param msg the error message for the exception.
     * @draft ICU 4.2
     * @provisional This API might change or be removed in a future release.
     */
    public InvalidLocaleException(String msg) {
        super(msg);
    }
}
