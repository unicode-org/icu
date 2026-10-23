// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2003-2011, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import org.junit.Assert;
import org.junit.AssumptionViolatedException;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTestLog implements TestLog {
    protected Logger logger;

    @Rule public LoggingTestWatcher watcher;

    protected AbstractTestLog() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.watcher = new LoggingTestWatcher(logger);
    }

    // --------------------------------------------------
    // Simple logging methods
    // --------------------------------------------------

    @Override
    public void logTrace(String message) {
        logger.trace(message);
    }

    @Override
    public void logDebug(String message) {
        logger.debug(message);
    }

    @Override
    public void logInfo(String message) {
        logger.info(message);
    }

    @Override
    public void logWarn(String message) {
        logger.warn(message);
    }

    @Override
    public void logError(String message) {
        logger.error(message);
    }

    // --------------------------------------------------
    // Backward compatible logging methods
    // --------------------------------------------------

    @Override
    public void logln(String message) {
        logger.debug(message);
    }

    @Override
    public void warnln(String message) {
        logger.warn(message);
        Assert.fail(message);
    }

    @Override
    public void errln(String message) {
        logger.error(message);
        Assert.fail(message);
    }

    // --------------------------------------------------
    // Obsolete logging methods
    // --------------------------------------------------

    @Override
    public void log(String message) {
        logger.debug(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
        Assert.fail(message);
    }

    @Override
    public void err(String message) {
        logger.error(message);
        Assert.fail(message);
    }

    public boolean isDateAtLeast(int year, int month, int day) {
        Date now = new Date();
        Calendar c = new GregorianCalendar(year, month, day);
        Date dt = c.getTime();
        if (now.compareTo(dt) >= 0) {
            return true;
        }
        return false;
    }

    static class LoggingTestWatcher extends TestWatcher {
        Logger logger;

        LoggingTestWatcher(Logger logger) {
            this.logger = logger;
        }

        @Override
        protected void starting(Description description) {
            logger.info("Starting: " + description.getDisplayName());
        }

        @Override
        protected void succeeded(Description description) {
            logger.info("Succeeded: " + description.getDisplayName());
        }

        @Override
        protected void failed(Throwable e, Description description) {
            logger.error("Failed: " + description.getDisplayName());
        }

        @Override
        protected void skipped(AssumptionViolatedException e, Description description) {
            logger.info("Skipped: " + description.getDisplayName());
        }
    }
}
