/**
 *******************************************************************************
 * Copyright (C) 2001-2002, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/TestLog.java,v $
 * $Date: 2003/01/28 18:55:32 $
 * $Revision: 1.4 $
 *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

public interface TestLog {

    /**
     * Adds given string to the log if we are in verbose mode.
     */
    void log(String message);

    void logln(String message);

    /**
     * Report an error
     */
    void err(String message);

    void errln(String message);

    /**
     * Add infomational line to log
     */
     void info(String message);

     void infoln(String message);
}
