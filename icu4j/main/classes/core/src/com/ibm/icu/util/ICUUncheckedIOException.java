// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html#License
/*
 *******************************************************************************
 * Copyright (C) 2014-2015, International Business Machines Corporation and
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
 * @stable ICU 53
 */
public class ICUUncheckedIOException extends RuntimeException {
    private static final long serialVersionUID = 1210263498513384449L;

    /**
     * Default constructor.
     *
     * @stable ICU 53
     */
    public ICUUncheckedIOException() {
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @stable ICU 53
     */
    public ICUUncheckedIOException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param cause original exception (normally a {@link java.io.IOException})
     * @stable ICU 53
     */
    public ICUUncheckedIOException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @param cause original exception (normally a {@link java.io.IOException})
     * @stable ICU 53
     */
    public ICUUncheckedIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
