/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * A singleton object that handles output to the screen and to a log file. Get
 * the current instance of the logger with <code>getInstance</code> and use
 * the output functions to output to the screen, the log file, the status bar,
 * and in dialog messages.
 */
public class Logger {

    /**
     * A default name to use for creating a log file.
     */
    public static final String DEFAULT_FILENAME = "icutzu.log";

    /**
     * Normal mode.
     */
    public static final int NORMAL = 0;

    /**
     * Quiet mode.
     */
    public static final int QUIET = -1;

    /**
     * Verbose mode.
     */
    public static final int VERBOSE = 1;

    /**
     * The single instance of the logger.
     */
    private static Logger logger = null;

    /**
     * Gets the instance of the logger, constructing a new one with
     * <code>filename</code> and <code>verbosity</code> if one is not
     * already constructed.
     * 
     * @param logFile
     *            The file to use for logging output.
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
    public static synchronized Logger getInstance(File logFile, int verbosity)
            throws FileNotFoundException {
        if (logger == null) {
            logger = new Logger(logFile, verbosity, null, null);
        }
        return logger;
    }

    /**
     * Gets the instance of the logger, constructing a new one with
     * <code>filename</code> and <code>verbosity</code> if one is not
     * already constructed. If a statusbar is given, status messages will be
     * sent to it. If a dialogParent is specified, dialog messages will be
     * displayed.
     * 
     * @param logFile
     *            The file to use for logging output.
     * @param verbosity
     *            The verbosity for output to the screen. Should be one of the
     *            following:
     *            <ul>
     *            <li>QUIET</li>
     *            <li>NORMAL</li>
     *            <li>VERBOSE</li>
     *            </ul>
     * @param statusBar
     *            The status bar for status-bar messages, or null if none is
     *            present.
     * @param dialogParent
     *            The parent for dialog messages, or null if no dialog messages
     *            are wanted.
     * @return The instance of the logger.
     * @throws FileNotFoundException
     */
    public static synchronized Logger getInstance(File logFile, int verbosity,
            JLabel statusBar, Component dialogParent)
            throws FileNotFoundException {
        if (logger == null) {
            logger = new Logger(logFile, verbosity, statusBar, dialogParent);
        }
        return logger;
    }

    /**
     * The parent to use when displaying a dialog.
     */
    private Component dialogParent = null;

    /**
     * The means of output to the log file.
     */
    private PrintStream fileStream = null;

    /**
     * The status bar to display status messages.
     */
    private JLabel statusBar = null;

    /**
     * The verbosity of the logger.
     */
    private int verbosity = NORMAL;

    /**
     * Constructs a logger that outputs to the filename specified and outputs to
     * the screen with the specified verbosity. Used internally.
     * 
     * @param filename
     *            The filename to use for logging output.
     * @param verbosity
     *            The verbosity for output to the screen.
     * @param statusBar
     *            The status bar for status-bar messages, or null if none is
     *            present.
     * @param dialogParent
     *            The parent for dialog messages, or null if no dialog messages
     *            are wanted.
     * @throws FileNotFoundException
     */
    private Logger(File logFile, int verbosity, JLabel statusBar,
            Component dialogParent) throws FileNotFoundException {
        if (this.fileStream != null)
            this.fileStream.close();
        this.fileStream = new PrintStream(new FileOutputStream(logFile));
        this.verbosity = verbosity;
        this.statusBar = statusBar;
        this.dialogParent = dialogParent;
    }

    /**
     * Prints an error message to the screen and to the log.
     * 
     * @param message
     *            The message to print.
     */
    public void error(String message) {
        System.err.print(message);
        if (fileStream != null)
            fileStream.print(message);
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
        if (fileStream != null)
            fileStream.println(message);
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
     * Logs a message to the screen if the logger is in a mode higher than
     * normal mode (ie. VERBOSE), and always logs the message to the file.
     * 
     * @param message
     *            The message to print.
     */
    public void loglnToBoth(String message) {
        loglnToScreen(message);
        loglnToFile(message);
    }

    /**
     * Logs a message to the file used by this logger, and terminates the line.
     * 
     * @param message
     *            The message to print.
     */
    public void loglnToFile(String message) {
        if (fileStream != null)
            fileStream.println(message);
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
     * Logs a message to the screen if the logger is in a mode higher than
     * normal mode (ie. VERBOSE), and always logs the message to the file, and
     * terminates the line when printing.
     * 
     * @param message
     *            The message to print.
     */
    public void logToBoth(String message) {
        logToScreen(message);
        logToFile(message);
    }

    /**
     * Logs a message to the file used by this logger.
     * 
     * @param message
     *            The message to print.
     */
    public void logToFile(String message) {
        if (fileStream != null)
            fileStream.print(message);
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
     * Prints a message to the screen and to the log, and terminates the line.
     * 
     * @param message
     *            The message to print.
     */
    public void printlnToBoth(String message) {
        printlnToScreen(message);
        loglnToFile(message);
    }

    /**
     * Prints a message to the screen, and terminates the line.
     * 
     * @param message
     *            The message to print.
     */
    public void printlnToScreen(String message) {
        if (verbosity >= NORMAL)
            System.out.println(message);
    }

    /**
     * Prints a message to the screen and to the log.
     * 
     * @param message
     *            The message to print.
     */
    public void printToBoth(String message) {
        printToScreen(message);
        logToFile(message);
    }

    /**
     * Prints a message to the screen.
     * 
     * @param message
     *            The message to print.
     */
    public void printToScreen(String message) {
        if (verbosity >= NORMAL)
            System.out.print(message);
    }

    /**
     * If the status bar is not null, produces a status message so that the user
     * is aware of the current status.
     * 
     * @param message
     *            The status.
     * @throws InterruptedException
     */
    public void setStatus(String message) throws InterruptedException {
        if (statusBar != null) {
            statusBar.setText(message);

            // give a chance for the UI to display the message
            if (Thread.currentThread().isInterrupted())
                throw new InterruptedException();
            Thread.sleep(0);
        }
    }

    /**
     * If dialogParent is not null, brings up an informative dialog about
     * something the user should be aware of.
     * 
     * @param message
     *            The message to the user.
     */
    public void showInformationDialog(String message) {
        if (dialogParent != null)
            JOptionPane.showMessageDialog(dialogParent, message,
                    "INFORMATION MESSAGE", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * If dialogParent is not null, brings up an warning dialog about something
     * the user should be aware of.
     * 
     * @param message
     *            The message to the user.
     */
    public void showWarningDialog(String message) {
        if (dialogParent != null)
            JOptionPane.showMessageDialog(dialogParent, message,
                    "WARNING MESSAGE", JOptionPane.WARNING_MESSAGE);
    }
}
