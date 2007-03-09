/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.text.JTextComponent;

public class GUILoader {
    public static void main(String[] args) {
        new GUILoader(args);
    }

    public GUILoader(String[] args) {
        try {
            logger = Logger.getInstance(Logger.DEFAULT_FILENAME, Logger.NORMAL);
        } catch (FileNotFoundException ex) {
            String error = "Could not open " + Logger.DEFAULT_FILENAME + " for writing.";
            System.out.println(error);
            JOptionPane.showMessageDialog(null, error, TITLE, JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }
        resultModel = new ResultModel(logger);
        pathModel = new PathModel(logger);
        sourceModel = new SourceModel(logger);

        pathGUI = new PathComponent(this, pathModel);
        pathFrame = new JFrame(TITLE + " - Search Paths");
        pathFrame.getContentPane().add(pathGUI);
        pathFrame.pack();
        // pathFrame.setLocationRelativeTo(null);

        pathFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        pathFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (resultClosed)
                    System.exit(0);
                pathClosed = true;
            }
        });

        resultGUI = new ResultComponent(this, resultModel, sourceModel);
        resultFrame = new JFrame(TITLE + " - Updatable ICU4J Jars");
        resultFrame.getContentPane().add(resultGUI);
        resultFrame.pack();
        // resultFrame.setLocationRelativeTo(null);

        resultFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        resultFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                if (pathClosed)
                    System.exit(0);
                resultClosed = true;
                makeThreadDead();
            }
        });

        statusBar = resultGUI.getStatusBar();

        backupDir = (args.length == 1) ? new File(args[0]) : null;

        setCancelSearchEnabled(false);
        setCancelUpdateEnabled(false);
        pathFrame.setVisible(true);

        sourceModel.findSources();
    }

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
                    pathModel.searchAll(resultModel, subdirs, backupDir, statusBar);
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
                    pathModel.search(resultModel, indices, subdirs, backupDir, statusBar);
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

    public void cancelSearch() {
        makeThreadDead();
    }

    public void cancelUpdate() {
        makeThreadDead();
    }

    private void setSearchEnabled(boolean value) {
        pathGUI.setSearchEnabled(value);
    }

    private void setUpdateEnabled(boolean value) {
        resultGUI.setUpdateEnabled(value);
    }

    private void setCancelSearchEnabled(boolean value) {
        resultGUI.setCancelSearchEnabled(value);
    }

    private void setCancelUpdateEnabled(boolean value) {
        resultGUI.setCancelUpdateEnabled(value);
    }

    private void makeThreadDead() {
        if (workerThread != null)
            try {
                workerThread.interrupt();
                workerThread.join();
            } catch (Exception ex) {
                // do nothing
            }
    }

    private Thread workerThread = null;

    private boolean pathClosed = false;

    private boolean resultClosed = true;

    private PathModel pathModel;

    private PathComponent pathGUI;

    private ResultModel resultModel;

    private SourceModel sourceModel;

    private ResultComponent resultGUI;

    private JFrame pathFrame;

    private JFrame resultFrame;

    private File backupDir;

    private Logger logger;

    private JTextComponent statusBar;

    public static final String TITLE = "ICUTZU (ICU4J Time Zone Updater)";
}
