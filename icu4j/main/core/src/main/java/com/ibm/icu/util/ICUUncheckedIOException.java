// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2014-2015, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.util;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.UncheckedIOException;

/**
 * Unchecked version of {@link IOException}. Some ICU APIs do not throw the standard exception but
 * instead wrap it into this unchecked version.
 *
 * <p>Since ICU 79, this is a deprecated subclass of {@link UncheckedIOException}. You should catch
 * the standard unchecked exception.
 *
 * <p>This class had been introduced as a subclass of {@link RuntimeException}, because at that time
 * ICU did not yet require Java 8+.
 *
 * @deprecated ICU 79
 */
@Deprecated
public class ICUUncheckedIOException extends UncheckedIOException {
    private static final long serialVersionUID = 1210263498513384449L;

    /**
     * Field added in ICU 79 for serialization format versioning.
     *
     * <p>Until ICU 78, the base class was just a RuntimeException. The cause could be null or any
     * Throwable.
     *
     * <p>Since ICU 79, the base class is an UncheckedIOException, and its cause must be a non-null
     * IOException.
     *
     * <p>Not final, so that (a) the compiler does not omit the readResolve() code that acts on it
     * being 0 from an old object, and (b) readResolve() can update it.
     */
    private int serialVersion = 1;

    /**
     * Default constructor.
     *
     * @deprecated ICU 79
     */
    @Deprecated
    public ICUUncheckedIOException() {
        super(new IOException());
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @deprecated ICU 79
     */
    @Deprecated
    public ICUUncheckedIOException(String message) {
        super(message, new IOException(message));
    }

    /**
     * Constructor.
     *
     * @param cause original exception (normally a {@link java.io.IOException})
     * @deprecated ICU 79
     */
    @Deprecated
    public ICUUncheckedIOException(Throwable cause) {
        super(cause instanceof IOException ? (IOException) cause : new IOException(cause));
    }

    /**
     * Constructor.
     *
     * @param message exception message string
     * @param cause original exception (normally a {@link java.io.IOException})
     * @deprecated ICU 79
     */
    @Deprecated
    public ICUUncheckedIOException(String message, Throwable cause) {
        super(message, cause instanceof IOException ? (IOException) cause : new IOException(cause));
    }

    private Object readResolve() throws ObjectStreamException {
        if (serialVersion == 0) {
            // The deserialized object was from ICU 78 or earlier.
            // The cause may be null, or else it may not be an IOException, in which case
            // we cannot even fetch it via ICU 79's new base class
            // UncheckedIOException.getCause().
            IOException io = null;
            boolean replace = false;
            try {
                io = getCause();
            } catch (NullPointerException e) {
                // No cause, fall through to initCause().
                // This is unreachable if getCause() on a deserialized object returns null;
                // the UncheckedIOException *constructors* do not allow a null cause.
            } catch (ClassCastException e) {
                // Non-null, incompatible, unrecoverable cause. initCause() cannot be called.
                replace = true;
            }
            if (io == null) {
                io = new IOException(getMessage());
                io.setStackTrace(getStackTrace());
                if (replace) {
                    ICUUncheckedIOException sub = new ICUUncheckedIOException(getMessage(), io);
                    sub.setStackTrace(getStackTrace());
                    return sub;
                }
                initCause(io);
            }
            serialVersion = 1;
        }
        return this;
    }
}
