// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

public interface TestLog {
    // --------------------------------------------------
    // Simple logging methods used for logging in test codes.
    // Any of these methods won't throw an AssertionError, unlike
    // errln/warnln

    /**
     * Emits a trace log.
     *
     * @param message a log message
     */
    void logTrace(String message);

    /**
     * Emits a debug log.
     *
     * @param message a log message
     */
    void logDebug(String message);

    /**
     * Emits an info log.
     *
     * @param message a log message
     */
    void logInfo(String message);

    /**
     * Emits a warn log. This method won't throw an AssertionError.
     *
     * @param message a log message
     */
    void logWarn(String message);

    /**
     * Emits an error log. This method won't throw an AssertionError.
     *
     * @param message a log message
     */
    void logError(String message);

    // --------------------------------------------------
    // Traditional test logging methods. These are kept for
    // backward compatibility support.
    //

    /**
     * Emits a debug log line. This is equivalent to {@link #logDebug(String)}.
     *
     * @param message a log message
     */
    void logln(String message);

    /**
     * Emits a warn line. This methods will throw an AssertionError after the message is emitted.
     *
     * @param message a log message
     * @deprecated Use {@link #errln(String)} instead. The distinction between warning and error is
     *     obsolete with JUnit.
     */
    @Deprecated
    void warnln(String message);

    /**
     * Emits an err line. This methods will through an AssertionError after the message is emitted.
     *
     * @param message a log message
     */
    void errln(String message);

    // --------------------------------------------------
    // Methods previously used for writing a text to log without
    // new line codes. The latest implementation is not designed
    // for writing out partial messages. The use of these methods
    // should be migrated to the new logging methods by composing
    // a single message text.

    /**
     * Emits an info text. This is equivalent to {@link #logln(String)}.
     *
     * @param message a log text
     * @deprecated Use {@link #logln(String)} instead.
     */
    @Deprecated
    void log(String message);

    /**
     * Emits a warn text. This is equivalent to (@link #warnln(String)).
     *
     * @param message a warn text
     * @deprecated Use {@link #errln(String)} instead.
     */
    @Deprecated
    void warn(String message);

    /**
     * Emits an err text. This is equivalent to {@link #errln(String)}.
     *
     * @param message an error text
     * @deprecated Use {@link #errln(String)} instead.
     */
    @Deprecated
    void err(String message);
}
