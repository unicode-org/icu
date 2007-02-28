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

    private Logger() {
    }

    public static Logger initLogger(String filename, int verbosity) {
        try {
            System.out.println("Log file: " + filename);
            if (logger.log != null)
                logger.log.close();
            logger.log = new PrintStream(new FileOutputStream(filename));
            logger.verbosity = verbosity;
        } catch (FileNotFoundException ex) {
            System.err.println("Could not create " + filename + ".");
        }
        return logger;
    }

    public static Logger getInstance() {
        return logger;
    }

    public void setVerbosity(int verbosity) {
        this.verbosity = verbosity;
    }

    public int getVerbosity() {
        return verbosity;
    }

    public void print(String output, int verbosity) {
        if (verbosity >= this.verbosity)
            System.out.print(output);
    }

    public void println(String output, int verbosity) {
        if (verbosity >= this.verbosity)
            System.out.println(output);
    }

    public void error(String output) {
        System.err.print(output);
    }

    public void errorln(String output) {
        System.err.println(output);
    }

    public void log(String output) {
        if (log != null)
            log.print(output);
    }

    public void logln(String output) {
        if (log != null)
            log.println(output);
    }

    public static final int QUIET = -1;

    public static final int NORMAL = 0;

    public static final int VERBOSE = 1;

    private int verbosity = NORMAL;

    private PrintStream log;

    private static Logger logger = new Logger();

    public static final String DEFAULT_FILENAME = "icutzu.log";
}
