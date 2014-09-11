/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Unchecked version of {@link CloneNotSupportedException}.
 * Some ICU APIs do not throw the standard exception but instead wrap it
 * into this unchecked version.
 *
 * @draft ICU 53
 * @provisional This API might change or be removed in a future release.
 */
public class ICUCloneNotSupportedException extends ICUException {
    private static final long serialVersionUID = -4824446458488194964L;

    /**
     * Default constructor.
     *
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUCloneNotSupportedException() {
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUCloneNotSupportedException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause original exception (normally a {@link CloneNotSupportedException})
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUCloneNotSupportedException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @param cause original exception (normally a {@link CloneNotSupportedException})
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUCloneNotSupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
