/**
 *******************************************************************************
 * Copyright (C) 2003-2004, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.test;

import com.ibm.icu.util.VersionInfo;

public abstract class AbstractTestLog implements TestLog {

    public static boolean dontSkipForVersion = false;
    public boolean skipIfBeforeICU(int major, int minor) {
        if (dontSkipForVersion || VersionInfo.ICU_VERSION.compareTo(VersionInfo.getInstance(major, minor)) > 0) {
            return false;
        } 
        logln("Test skipped before ICU release " + major + "." + minor);
        return true;
    }
    
    /**
     * Add a message.
     */
    public final void log(String message) {
        msg(message, LOG, true, false);
    }

    /**
     * Add a message and newline.
     */
    public final void logln(String message) {
        msg(message, LOG, true, true);
    }

    /**
     * Report an error.
     */
    public final void err(String message) {
        msg(message, ERR, true, false);
    }

    /**
     * Report an error and newline.
     */
    public final void errln(String message) {
        msg(message, ERR, true, true);
    }

    /**
     * Report a warning (generally missing tests or data).
     */
    public final void warn(String message) {
        msg(message, WARN, true, false);
    }

    /**
     * Report a warning (generally missing tests or data) and newline.
     */
    public final void warnln(String message) {
        msg(message, WARN, true, true);
    }

    /**
     * Vector for logging.  Callers can force the logging system to
     * not increment the error or warning level by passing false for incCount.
     *
     * @param message the message to output.
     * @param level the message level, either LOG, WARN, or ERR.
     * @param incCount if true, increments the warning or error count
     * @param newln if true, forces a newline after the message
     */
    public abstract void msg(String message, int level, boolean incCount, boolean newln);

    /**
     * Not sure if this class is useful.  This lets you log without first testing
     * if logging is enabled.  The Delegating log will either silently ignore the
     * message, if the delegate is null, or forward it to the delegate.
     */
    public static final class DelegatingLog extends AbstractTestLog {
        private TestLog delegate;

        public DelegatingLog(TestLog delegate) {
            this.delegate = delegate;
        }

        public void msg(String message, int level, boolean incCount, boolean newln) {
            if (delegate != null) {
                delegate.msg(message, level, incCount, newln);
            }
        }
    }
}
