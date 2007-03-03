/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.tzu;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Logger {

    private Logger(String filename, int verbosity) throws FileNotFoundException {
        System.out.println("Log file: " + filename);
        if (this.log != null)
            this.log.close();
        this.log = new PrintStream(new FileOutputStream(filename));
        this.verbosity = verbosity;
    }

    public static synchronized Logger getInstance(String filename, int verbosity) throws FileNotFoundException {
        if (logger == null) {
            logger = new Logger(filename, verbosity);
        }
        return logger;
    }

    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

    public int getVerbosity() {
        return verbosity;
    }

    public void log(String output, int verbosity) {
        if (verbosity <= this.verbosity) {
            System.out.print(output);
            if (log != null)
                log.print(output);
        }
    }

    public void logln(String output, int verbosity) {
        if (verbosity <= this.verbosity) {
            System.out.println(output);
            if (log != null)
                log.println(output);
        }
    }

    public void print(String output, int verbosity) {
        if (verbosity <= this.verbosity)
            System.out.print(output);
    }

    public void println(String output, int verbosity) {
        if (verbosity <= this.verbosity)
            System.out.println(output);
    }

    public void error(String output) {
        System.err.print(output);
        if (log != null)
            log.print(output);
    }

    public void errorln(String output) {
        System.err.println(output);
        if (log != null)
            log.println(output);
    }

    public static final int QUIET = -1;

    public static final int NORMAL = 0;

    public static final int VERBOSE = 1;

    private int verbosity = NORMAL;

    private PrintStream log;

    private static Logger logger = null;

    public static final String DEFAULT_FILENAME = "icutzu.log";
}
