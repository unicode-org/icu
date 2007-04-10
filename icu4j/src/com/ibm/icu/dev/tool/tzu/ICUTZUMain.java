/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.File;
import java.util.Locale;

/**
 * Entry point for the ICUTZU tool.
 */
public class ICUTZUMain {
    /**
     * Argument number for the backup directory.
     */
    public static final int BACKUP_DIR = 4;

    /**
     * Argument number for the current directory.
     */
    public static final int CUR_DIR = 0;

    /**
     * Argument number for the icon file.
     */
    public static final int ICON_FILE = 5;

    /**
     * Number of arguments.
     */
    public static final int NUM_ARGS = 6;

    /**
     * Argument number for the path list file.
     */
    public static final int PATH_FILE = 1;

    /**
     * Argument number for the result list file.
     */
    public static final int RESULT_FILE = 2;

    /**
     * Argument number for the timezone resource file.
     */
    public static final int TZ_FILE = 3;

    /**
     * Entry point for the ICUTZU tool.
     * 
     * @param args
     *            The list of arguments. Should be in the following order:
     *            <ul>
     *            <li>Current directory</li>
     *            <li>Path list file</li>
     *            <li>Result list file</li>
     *            <li>Timezone resource file</li>
     *            <li>Backup directory</li>
     *            </ul>
     *            All directories and paths should be relative to the given
     *            current directory.
     */
    public static void main(String[] args) {
        try {
            String agent = "ICUTZU/1.0 (" + System.getProperty("os.name") + " "
                    + System.getProperty("os.version") + "; "
                    + Locale.getDefault().toString() + ")";
            System.setProperty("http.agent", agent);

            if (args.length == 0) {
                // TODO: Remove this once this has been tested!!!
                System.out.println("Backup directory: "
                        + new File("Temp").getPath());

                // in the case of running without commandline options
                new GUILoader(new File("."), new File("Temp"), new File(
                        "DirectoryList.txt"), new File("ICUList.txt"),
                        new File("zoneinfo.res"), new File("icu.gif"));
                return;
            }

            if (args.length != NUM_ARGS) {
                System.err.println("Incorrect number of arguments.");
                System.err
                        .println("Syntax: ICUTZUMain <cur dir> <path file> <result file> <tz file> <backup dir>");
                System.exit(-1);
            }

            File curDir = new File(args[CUR_DIR]);
            File backupDir = new File(args[CUR_DIR] + File.separator
                    + args[BACKUP_DIR]);
            File pathFile = new File(args[CUR_DIR] + File.separator
                    + args[PATH_FILE]);
            File resultFile = new File(args[CUR_DIR] + File.separator
                    + args[RESULT_FILE]);
            File tzFile = new File(args[CUR_DIR] + File.separator
                    + args[TZ_FILE]);
            File iconFile = new File(args[CUR_DIR] + File.separator
                    + args[ICON_FILE]);

            if ("true".equalsIgnoreCase(System.getProperty("nogui")))
                new CLILoader(curDir, backupDir, pathFile, resultFile, tzFile);
            else
                new GUILoader(curDir, backupDir, pathFile, resultFile, tzFile,
                        iconFile);
        } catch (Throwable ex) {
            // should any unexplained exception occur, we should exit
            // abnormally. ideally, this should never happen.
            System.err.println("Internal program error.");
            ex.printStackTrace();
            System.exit(-1);
        }
    }
}
