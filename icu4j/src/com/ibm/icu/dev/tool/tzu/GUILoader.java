/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 * Loads the ICUTZU tool, GUI version.
 */
public class GUILoader {
    /**
     * Entry point for the GUI version of the tool.
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
     * @param iconFile
     *            The icon file.
     */
    public GUILoader(File curDir, File backupDir, File pathFile,
            File resultFile, File tzFile, File iconFile) {
        // set the backup dir
        this.backupDir = backupDir;
        this.curDir = curDir;

        // get the icon
        Image icon = Toolkit.getDefaultToolkit().getImage(iconFile.getPath());

        // initialize the path list gui
        pathGUI = new PathComponent(this);
        pathFrame = new JFrame(TITLE + " - Search Paths");
        pathFrame.getContentPane().add(pathGUI);
        pathFrame.pack();
        pathFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        pathFrame.setIconImage(icon);
        pathFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (resultClosed)
                    System.exit(0);
                pathClosed = true;
            }
        });

        // initialize the result list gui
        resultGUI = new ResultComponent(this);
        resultFrame = new JFrame(TITLE + " - Updatable ICU4J Jars");
        resultFrame.getContentPane().add(resultGUI);
        resultFrame.pack();
        resultFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        resultFrame.setIconImage(icon);
        resultFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (pathClosed)
                    System.exit(0);
                resultClosed = true;
                makeThreadDead();
            }
        });

        // get the logger instance
        try {
            logger = Logger.getInstance(Logger.DEFAULT_FILENAME, Logger.NORMAL,
                    resultGUI.getStatusBar(), pathFrame);
        } catch (FileNotFoundException ex) {
            String error = "Could not open " + Logger.DEFAULT_FILENAME
                    + " for writing.";
            System.out.println(error);
            JOptionPane.showMessageDialog(null, error, TITLE,
                    JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }

        // initialize the models
        resultModel = new ResultModel(logger, resultFile);
        pathModel = new PathModel(logger, pathFile);
        sourceModel = new SourceModel(logger, tzFile);

        // attach the models to the guis
        resultGUI.setResultModel(resultModel);
        pathGUI.setPathModel(pathModel);
        resultGUI.setSourceModel(sourceModel);

        // load all the paths into the path model
        try {
            pathModel.loadPaths();
        } catch (IOException ex) {
            // failed to load the directory search file
            pathModel.addAllDrives();
        } catch (IllegalArgumentException ex) {
            // failed to load the directory search file
            pathModel.addAllDrives();
        }

        // find sources for updating
        sourceModel.findSources();

        // make sure that search and update cancelation is disabled (since we
        // are initially neither updating nor searching, so there is nothing to
        // cancel)
        setCancelSearchEnabled(false);
        setCancelUpdateEnabled(false);

        // show the path list gui
        pathFrame.setVisible(true);
    }

    /**
     * Searchs all the paths in the path model.
     * 
     * @param subdirs
     *            Whether to search subdirectories.
     */
    public void searchAll(final boolean subdirs) {
        makeThreadDead();

        workerThread = new Thread(new Runnable() {
            public void run() {
                logger.printlnToScreen("Search started.");
                setCancelSearchEnabled(true);
                setUpdateEnabled(false);
                setSearchEnabled(false);
                try {
                    resultFrame.setVisible(true);
                    resultClosed = false;
                    pathModel
                            .searchAll(resultModel, subdirs, curDir, backupDir);
                } catch (InterruptedException ex) { /* i escaped! i'm free! */
                }
                setSearchEnabled(true);
                setUpdateEnabled(true);
                setCancelSearchEnabled(false);
                logger.printlnToScreen("Search ended.");
            }
        });

        workerThread.start();
    }

    /**
     * Searchs the selected paths in the path model.
     * 
     * @param indices
     *            Which paths in the path models to be used in the search.
     * @param subdirs
     *            Whether to search subdirectories.
     */
    public void search(final int[] indices, final boolean subdirs) {
        makeThreadDead();

        workerThread = new Thread(new Runnable() {
            public void run() {
                logger.printlnToScreen("Search started.");
                setCancelSearchEnabled(true);
                setUpdateEnabled(false);
                setSearchEnabled(false);
                try {
                    resultFrame.setVisible(true);
                    resultClosed = false;
                    pathModel.search(resultModel, indices, subdirs, curDir,
                            backupDir);
                } catch (InterruptedException ex) { /* i escaped! i'm free! */
                }
                setSearchEnabled(true);
                setUpdateEnabled(true);
                setCancelSearchEnabled(false);
                logger.printlnToScreen("Search ended.");
            }
        });

        workerThread.start();
    }

    /**
     * Updates all the results in the result model.
     * 
     * @param updateURL
     *            The URL to use as the update for each ICU4J jar.
     */
    public void updateAll(final URL updateURL) {
        makeThreadDead();

        workerThread = new Thread(new Runnable() {
            public void run() {
                logger.printlnToScreen("Update started.");
                setCancelUpdateEnabled(true);
                setUpdateEnabled(false);
                setSearchEnabled(false);
                try {
                    resultModel.updateAll(updateURL, backupDir);
                } catch (InterruptedException ex) { /* i escaped! i'm free! */
                }
                setUpdateEnabled(true);
                setSearchEnabled(true);
                setCancelUpdateEnabled(false);
                logger.printlnToScreen("Update ended.");
            }
        });

        workerThread.start();
    }

    /**
     * Updates the selected results in the result model.
     * 
     * @param indices
     *            Which ICU4J jars in the result model to be used in the update.
     * @param updateURL
     *            The URL to use as the update for each ICU4J jar.
     */
    public void update(final int[] indices, final URL updateURL) {
        makeThreadDead();

        workerThread = new Thread(new Runnable() {
            public void run() {
                logger.printlnToScreen("Update started.");
                setCancelUpdateEnabled(true);
                setUpdateEnabled(false);
                setSearchEnabled(false);
                try {
                    resultModel.update(indices, updateURL, backupDir);
                } catch (InterruptedException ex) { /* i escaped! i'm free! */
                }
                setUpdateEnabled(true);
                setSearchEnabled(true);
                setCancelUpdateEnabled(false);
                logger.printlnToScreen("Update ended.");
            }
        });

        workerThread.start();
    }

    /**
     * Cancels a search.
     */
    public void cancelSearch() {
        makeThreadDead();
    }

    /**
     * Cancels an update.
     */
    public void cancelUpdate() {
        makeThreadDead();
    }

    /**
     * Sets whether the search button should be enabled.
     * 
     * @param value
     *            Whether the search button should be enabled.
     */
    private void setSearchEnabled(boolean value) {
        pathGUI.setSearchEnabled(value);
    }

    /**
     * Sets whether the update button should be enabled.
     * 
     * @param value
     *            Whether the update button should be enabled.
     */
    private void setUpdateEnabled(boolean value) {
        resultGUI.setUpdateEnabled(value);
    }

    /**
     * Sets whether the cancel search button should be enabled.
     * 
     * @param value
     *            Whether the cancel search button should be enabled.
     */
    private void setCancelSearchEnabled(boolean value) {
        resultGUI.setCancelSearchEnabled(value);
    }

    /**
     * Sets whether the cancel update button should be enabled.
     * 
     * @param value
     *            Whether the cancel update button should be enabled.
     */
    private void setCancelUpdateEnabled(boolean value) {
        resultGUI.setCancelUpdateEnabled(value);
    }

    /**
     * Interrupts the worker thread and waits for it to finish.
     */
    private void makeThreadDead() {
        if (workerThread != null)
            try {
                workerThread.interrupt();
                workerThread.join();
            } catch (Exception ex) {
                // do nothing -- if an exception was thrown, the worker thread
                // must have already been dead, which is perfectly fine
            }
    }

    /**
     * The thread that partakes in the searching and updating.
     */
    private Thread workerThread = null;

    /**
     * Whether the paths frame has been closed or not.
     */
    private boolean pathClosed = false;

    /**
     * Whether the results frame has been closed or not.
     */
    private boolean resultClosed = true;

    /**
     * The path model that stores all the paths and takes care of searching.
     */
    private PathModel pathModel;

    /**
     * The result model that stores all the results and takes care of updating.
     */
    private ResultModel resultModel;

    /**
     * The source model that stores all the update sources and accesses the
     * repository for more sources.
     */
    private SourceModel sourceModel;

    /**
     * The component that allows the user to interact with the path model.
     */
    private PathComponent pathGUI;

    /**
     * The component that allows the user to interact with the result model.
     */
    private ResultComponent resultGUI;

    /**
     * The frame that displays the path model component (<code>pathGUI</code>).
     */
    private JFrame pathFrame;

    /**
     * The frame that displays the result model component (<code>resultGUI</code>).
     */
    private JFrame resultFrame;

    /**
     * The backup directory to store files.
     */
    private File backupDir;

    /**
     * The tool's home directory.
     */
    private File curDir;

    /**
     * The current logger.
     */
    private Logger logger;

    /**
     * The title for the application.
     */
    public static final String TITLE = "ICUTZU (ICU4J Time Zone Updater)";
}
