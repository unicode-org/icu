/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Loads the ICUTZU tool, command-line version.
 */
public class CLILoader {
    /**
     * The filename of the log file in patch mode.
     */
    public static final String LOG_FILENAME_PATCH = "icutzu_patch.log";

    /**
     * The filename of the log file in discovery only mode.
     */
    public static final String LOG_FILENAME_DISCOVERYONLY = "icutzu_discovery.log";

    /**
     * The backup directory to use in patch mode, or null if there should be no backups.
     */
    private File backupDir = null;

    /**
     * The current directory.
     */
    private File curDir = null;

    /**
     * A logger that manages both output to the screen and to the log file.
     */
    private Logger logger;

    /**
     * The file that stores the path list.
     */
    private File pathFile = null;

    /**
     * A glorified list of IncludePath objects representing the list of directories used in
     * discovery mode.
     */
    private PathModel pathModel;

    /**
     * The file that stores the result list.
     */
    private File resultFile = null;

    /**
     * A glorified list of ICUFile objects representing the ICU4J jars found in discovery mode and
     * used in patch mode.
     */
    private ResultModel resultModel;

    /**
     * A glorified map of Strings to URLs populated by parsing the directory structure in the ICU
     * Time Zone repository online and used in patch mode.
     */
    private SourceModel sourceModel;

    /**
     * The local timezone resource file.
     */
    private File tzFile = null;

    /**
     * Entry point for the command-line version of the tool.
     * 
     * @param curDir
     *            The base directory of the tool.
     * @param backupDir
     *            The location to store backups.
     * @param pathFile
     *            The file to load paths from.
     * @param resultFile
     *            The file to load/save results to/from.
     * @param tzFile
     *            The local timezone resource file.
     */
    public CLILoader(File curDir, File backupDir, File pathFile, File resultFile, File tzFile) {
        // determine whether we are running in discover only mode or patch mode
        boolean discoverOnly = "true".equalsIgnoreCase(System.getProperty("discoveronly"));
        boolean silentPatch = "true".equalsIgnoreCase(System.getProperty("silentpatch"));
        File logFile = new File(curDir.getPath(), (discoverOnly ? "icutzu_discover.log"
                : "icutzu_patch.log"));

        // create the logger based on the silentpatch option
        try {
            this.logger = Logger.getInstance(logFile, silentPatch ? Logger.QUIET : Logger.NORMAL);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not open " + logFile.getPath() + " for writing.");
            System.exit(-1);
        }

        this.pathFile = pathFile;
        this.resultFile = resultFile;
        this.tzFile = tzFile;
        this.backupDir = backupDir;
        this.curDir = curDir;

        // if discoveryonly is enabled, call the search method
        // otherwise, call the update method
        try {
            if (discoverOnly)
                search();
            else
                update();
        } catch (IllegalArgumentException ex) {
            logger.errorln(ex.getMessage());
        } catch (IOException ex) {
            logger.errorln(ex.getMessage());
        } catch (InterruptedException ex) {
            logger.errorln(ex.getMessage());
        }
    }

    /**
     * Discover Only Mode. Load the path list from the path file and save the path of each updatable
     * ICU jar files it finds to the result list
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     */
    private void search() throws IOException, IllegalArgumentException, InterruptedException {
        logger.printlnToScreen("");
        logger.printlnToScreen("*********** Command-Line \'Discover Only\'"
                + " Mode Started ***********");
        logger.printlnToScreen("");
        logger.printlnToScreen("\'Discover Only\' Mode:");
        logger.printlnToScreen("In this mode, " + "the tool will search for ICU4J jars"
                + " in the directories specified in DirectorySearch.txt"
                + " and print the ICU4J jars detected and their respective"
                + " time zone version to the file ICUList.txt");
        logger.printlnToScreen("");

        // initialize the result model and the path model
        resultModel = new ResultModel(logger, resultFile);
        pathModel = new PathModel(logger, pathFile);

        // load paths stored in PathModel.PATHLIST_FILENAME
        pathModel.loadPaths();

        // perform the search, putting the the results in the result model,
        // searching all subdirectories of the included path, using the backup
        // directory specified, and without using a status bar (since this is
        // command-line)
        logger.printlnToScreen("");
        logger.printlnToScreen("Search started.");
        pathModel.searchAll(resultModel, true, curDir, backupDir);
        logger.printlnToScreen("Search ended.");
        logger.printlnToScreen("");

        // save the results in PathModel.RESULTLIST_FILENAME
        resultModel.saveResults();

        logger.printlnToScreen("");
        logger.printlnToScreen("Please run with DISCOVERYONLY=false"
                + " in order to update ICU4J jars listed in ICUList.txt");
        logger.printlnToScreen("*********** Command-Line \'Discover Only\'"
                + " Mode Ended ***********");
        logger.printlnToScreen("");
    }

    /**
     * Patch Mode. Load all the results from resultFile, populate the tz version list via the web,
     * and update all the results with the best available Time Zone data.
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     */
    private void update() throws IOException, IllegalArgumentException, InterruptedException {
        logger.printlnToScreen("");
        logger.printlnToScreen("*********** Command-Line \'Patch\'" + " Mode Started ***********");
        logger.printlnToScreen("");
        logger.printlnToScreen("\'Patch\' Mode:");
        logger.printlnToScreen("In this mode, the tool patches each of the"
                + " ICU4J jars listed in ICUList.txt with the new time zone" + " information.");
        logger.printlnToScreen("");

        // initialize the result model and the source model
        resultModel = new ResultModel(logger, resultFile);
        sourceModel = new SourceModel(logger, tzFile);

        // load the results from the result list file
        resultModel.loadResults();

        // if the offline is not set to true, populate the list of available
        // timezone resource versions
        if (!"true".equalsIgnoreCase(System.getProperty("offline")))
            sourceModel.findSources();

        // perform the updates using the best tz version available
        resultModel.updateAll(sourceModel.getURL(sourceModel.getSelectedItem()), backupDir);

        logger.printlnToScreen("");
        logger.printlnToScreen("*********** Command-Line \'Patch\'" + " Mode Ended ***********");
        logger.printlnToScreen("");
    }
}
