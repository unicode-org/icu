/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

package com.ibm.icu.dev.tool.tzu;

import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

public class Logger {

    static {
        try {
            log = new PrintStream(new FileOutputStream("icutzu.log"));
        } catch (FileNotFoundException ex) {
            System.err.println("Could not create a log file.");
        }
    }

    private Logger() {
    }

    public static void setVerbosity(int verbosity) {
        Logger.verbosity = verbosity;
    }

    public static int getVerbosity() {
        return verbosity;
    }

    public static void print(Object output, int verbosity) {
        if (verbosity >= Logger.verbosity)
            System.out.print(output);
    }

    public static void println(Object output, int verbosity) {
        if (verbosity >= Logger.verbosity)
            System.out.println(output);
    }

    public static void error(Object output) {
        System.err.print(output);
    }

    public static void errorln(Object output) {
        System.err.println(output);
    }

    public static void log(Object output) {
        log.print(output);
    }

    public static void logln(Object output) {
        log.println(output);
    }

    public static final int QUIET = -1;

    public static final int NORMAL = 0;

    public static final int VERBOSE = 1;

    private static int verbosity = NORMAL;

    private static PrintStream log;
}
