// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2000-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

public interface ITestLog {
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
    // Traditional test logging methods. This is kept for
    // backward compatibility support.
    //

    /**
     * Emits an err line. This methods will through an AssertionError after the message is emitted.
     *
     * @param message a log message
     */
    void errln(String message);
}
