/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Base class for unchecked, ICU-specific exceptions.
 *
 * @draft ICU 53
 * @provisional This API might change or be removed in a future release.
 */
public class ICUException extends RuntimeException {
    private static final long serialVersionUID = -3067399656455755650L;

    /**
     * Default constructor.
     *
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUException() {
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause original exception
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @param cause original exception
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUException(String message, Throwable cause) {
        super(message, cause);
    }
}
