/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * A singleton object that handles output to the screen and to a log file. Get
 * the current instance of the logger with <code>getInstance</code>,
 */
public class Logger {

    /**
     * Constructs a logger that outputs to the filename specified and outputs to
     * the screen with the specified verbosity. Used internally.
     * 
     * @param filename
     *            The filename to use for logging output.
     * @param verbosity
     *            The verbosity for output to the screen.
     * @throws FileNotFoundException
     */
    private Logger(String filename, int verbosity) throws FileNotFoundException {
        System.out.println("Log file: " + filename);
        if (this.log != null)
            this.log.close();
        this.log = new PrintStream(new FileOutputStream(filename));
        this.verbosity = verbosity;
    }

    /**
     * Gets the instance of the logger, constructing a new one with
     * <code>filename</code> and <code>verbosity</code> if one is not
     * already constructed.
     * 
     * @param filename
     *            The filename to use for logging output.
     * @param verbosity
     *            The verbosity for output to the screen. Should be one of the
     *            following:
     *            <ul>
     *            <li>QUIET</li>
     *            <li>NORMAL</li>
     *            <li>VERBOSE</li>
     *            </ul>
     * @return The instance of the logger.
     * @throws FileNotFoundException
     */
    public static synchronized Logger getInstance(String filename, int verbosity) throws FileNotFoundException {
        if (verbosity > 1)
            verbosity = 1;
        if (verbosity < -1)
            verbosity = -1;
        if (logger == null) {
            logger = new Logger(filename, verbosity);
        }
        return logger;
    }

    /**
     * Returns the current allowed verbosity.
     * 
     * @return The current allowed verbosity for output to the screen. Should be
     *         one of the following:
     *         <ul>
     *         <li>QUIET</li>
     *         <li>NORMAL</li>
     *         <li>VERBOSE</li>
     *         </ul>
     */
    public int getVerbosity() {
        return verbosity;
    }

    /**
     * Prints a message to the screen.
     * 
     * @param message
     *            The message to print.
     */
    public void printToScreen(String message) {
        System.out.print(message);
    }

    /**
     * Prints a message to the screen, and terminates the line.
     * 
     * @param message
     *            The message to print.
     */
    public void printlnToScreen(String message) {
        System.out.println(message);
    }

    /**
     * Logs a message to the screen if the logger is in a mode higher than
     * normal mode (ie. VERBOSE).
     * 
     * @param message
     *            The message to print.
     */
    public void logToScreen(String message) {
        if (verbosity > NORMAL)
            System.out.print(message);
    }

    /**
     * Logs a message to the screen if the logger is in a mode higher than
     * normal mode (ie. VERBOSE), and terminates the line.
     * 
     * @param message
     *            The message to print.
     */
    public void loglnToScreen(String message) {
        if (verbosity > NORMAL)
            System.out.println(message);
    }

    /**
     * Logs a message to the file used by this logger.
     * 
     * @param message
     *            The message to print.
     */
    public void logToFile(String message) {
        log.print(message);
    }

    /**
     * Logs a message to the file used by this logger, and terminates the line.
     * 
     * @param message
     *            The message to print.
     */
    public void loglnToFile(String message) {
        log.println(message);
    }

    /**
     * Prints a message to the screen and to the log.
     * 
     * @param message
     *            The message to print.
     */
    public void printToBoth(String message) {
        log.print(message);
    }

    /**
     * Prints a message to the screen and to the log, and terminates the line.
     * 
     * @param message
     *            The message to print.
     */
    public void printlnToBoth(String message) {
        log.println(message);
    }

    /**
     * Logs a message to the screen if the logger is in a mode higher than
     * normal mode (ie. VERBOSE), and always logs the message to the file, and
     * terminates the line when printing.
     * 
     * @param message
     *            The message to print.
     */
    public void logToBoth(String message) {
        log.print(message);
    }

    /**
     * Logs a message to the screen if the logger is in a mode higher than
     * normal mode (ie. VERBOSE), and always logs the message to the file.
     * 
     * @param message
     *            The message to print.
     */
    public void loglnToBoth(String message) {
        log.println(message);
    }

    /**
     * Prints an error message to the screen and to the log.
     * 
     * @param message
     *            The message to print.
     */
    public void error(String message) {
        System.err.print(message);
        if (log != null)
            log.print(message);
    }

    /**
     * Prints an error message to the screen and to the log, and terminates the
     * line.
     * 
     * @param message
     *            The message to print.
     */
    public void errorln(String message) {
        System.err.println(message);
        if (log != null)
            log.println(message);
    }

    public static final int QUIET = -1;

    public static final int NORMAL = 0;

    public static final int VERBOSE = 1;

    public static final String DEFAULT_FILENAME = "icutzu.log";

    private int verbosity = NORMAL;

    private PrintStream log;

    private static Logger logger = null;
}
