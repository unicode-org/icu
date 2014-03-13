/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.util;

/**
 * Unchecked version of {@link java.io.IOException}.
 * Some ICU APIs do not throw the standard exception but instead wrap it
 * into this unchecked version.
 *
 * <p>This currently extends {@link RuntimeException},
 * but when ICU can rely on Java 8 this class should be changed to extend
 * java.io.UncheckedIOException instead.
 *
 * @draft ICU 53
 * @provisional This API might change or be removed in a future release.
 */
public class ICUUncheckedIOException extends RuntimeException {
    private static final long serialVersionUID = 1210263498513384449L;

    /**
     * Default constructor.
     *
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUUncheckedIOException() {
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUUncheckedIOException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause original exception (normally a {@link java.io.IOException})
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUUncheckedIOException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @param cause original exception (normally a {@link java.io.IOException})
     * @draft ICU 53
     * @provisional This API might change or be removed in a future release.
     */
    public ICUUncheckedIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
