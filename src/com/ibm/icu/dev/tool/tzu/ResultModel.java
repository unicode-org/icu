/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Represents a list of ICUFiles that is usable by any class that uses AbstractTableModels (such as
 * a JTable in swing). Also contains methods to begin updates on those ICUFiles and methods to load
 * and save a result list from and to a file.
 */
class ResultModel extends AbstractTableModel {
    /**
     * The column designating filenames.
     */
    public static final int COLUMN_FILE_NAME = 0;

    /**
     * The column designating file paths.
     */
    public static final int COLUMN_FILE_PATH = 1;

    /**
     * The column designating ICU versions.
     */
    public static final int COLUMN_ICU_VERSION = 2;

    /**
     * The column designating timezone verisons.
     */
    public static final int COLUMN_TZ_VERSION = 3;

    /**
     * A list of names of the columns in a result model.
     */
    public static final String[] COLUMN_NAMES = new String[] { "Filename", "Path", "ICU Version",
            "TZ Version" };

    /**
     * The serializable UID.
     */
    public static final long serialVersionUID = 1338;

    /**
     * The list of ICUFiles represented by this result model.
     */
    private List icuFileList = new ArrayList();

    /**
     * The current logger.
     */
    private Logger logger;

    /**
     * The result list file where results are saved and stored.
     */
    private File resultListFile;

    /**
     * The filename of the result list file where results are saved and stored.
     */
    private String resultListFilename;

    /**
     * Constructs an empty result list.
     * 
     * @param resultFile
     *            The file to load and save results from and to.
     * @param logger
     *            The current logger.
     */
    public ResultModel(Logger logger, File resultFile) {
        this.logger = logger;
        this.resultListFile = resultFile;
        this.resultListFilename = resultFile.getName();
    }

    /**
     * Adds a file to the ICUFile list.
     * 
     * @param file
     *            The file.
     * @return Whether the file was added successfully (which is determined by if it is an updatable
     *         ICU4J jar).
     */
    public boolean add(File file) {
        try {
            ICUFile icuFile = new ICUFile(file, logger);
            add(icuFile);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Adds a file to the ICUFile list.
     * 
     * @param icuFile
     *            The file.
     */
    public void add(ICUFile icuFile) {
        remove(icuFile.getFile());
        icuFileList.add(icuFile);
        int index = icuFileList.size() - 1;
        fireTableRowsInserted(index, index);
    }

    /**
     * Adds a file to the ICUFile list.
     * 
     * @param filename
     *            The name of the file.
     * @return Whether the file was added successfully (which is determined by if it is an updatable
     *         ICU4J jar).
     */
    public boolean add(String filename) {
        return add(new File(filename));
    }

    /**
     * Returns the number of columns for each represented ICUFile.
     * 
     * @return The number of columns for each represented ICUFile.
     */
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    /**
     * Returns the column names as stored in COLUMN_NAMES.
     * 
     * @param col
     *            The index of the column.
     * @return <code>COLUMN_NAMES[col]</code>
     */
    public String getColumnName(int col) {
        return COLUMN_NAMES[col];
    }

    /**
     * Returns the number of ICUFiles represented.
     * 
     * @return The number of ICUFiles represented.
     */
    public int getRowCount() {
        return (icuFileList == null) ? 0 : icuFileList.size();
    }

    /**
     * Returns the item at the given row and column. The row determines which ICUFile is used, and
     * the column determines which piece of data should be used.
     * 
     * @param row
     *            Which ICU file to use.
     * @param col
     *            Which piece of data to use. Should be one of the following:
     *            <ul>
     *            <li>COLUMN_FILE_PATH</li>
     *            <li>COLUMN_ICU_VERSION</li>
     *            <li>COLUMN_TZ_VERSION</li>
     *            </ul>
     * @return The item at the given row and column. Will always be a String.
     */
    public Object getValueAt(int row, int col) {
        ICUFile icuFile = ((ICUFile) icuFileList.get(row));
        switch (col) {
        case COLUMN_FILE_NAME:
            return icuFile.getFilename();
        case COLUMN_FILE_PATH:
            return icuFile.getPath();
        case COLUMN_ICU_VERSION:
            return icuFile.getICUVersion();
        case COLUMN_TZ_VERSION:
            return icuFile.getTZVersion();
        default:
            return null;
        }
    }

    /**
     * Returns an iterator on the list of ICUFiles.
     * 
     * @return An iterator on the list of ICUFiles.
     */
    public Iterator iterator() {
        return icuFileList.iterator();
    }

    /**
     * Loads a list of ICUFiles from the given result list file. Lines should be of the form <b><i>pathstring</i><tab><i>tzversion</i></b>.
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void loadResults() throws IOException, IllegalArgumentException {
        logger.printlnToScreen("Scanning " + resultListFilename + " file...");
        logger.printlnToScreen(resultListFilename + " file contains");

        BufferedReader reader = null;
        int lineNumber = 1;
        String line;
        int tab;
        String filename;

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(resultListFile),
                    "UTF-8"), 4 * 1024);
            while ((line = reader.readLine()) != null) {
                if (line.length() >= 1 && line.charAt(0) == '\ufeff')
                    line = line.substring(1);
                line = line.trim();
                logger.printlnToScreen(line);

                if (line.length() >= 1 && (tab = line.lastIndexOf('\t')) >= 0) {
                    if (!add(filename = line.substring(0, tab)))
                        resultListError(filename + " is not an updatable ICU4J file", lineNumber);
                }

                lineNumber++;
            }
        } catch (FileNotFoundException ex) {
            resultListError("The "
                    + resultListFilename
                    + " file doesn't exist. Please re-run the tool with -Ddiscoveronly=true option to generate the list of ICU4J jars.");
        } catch (IOException ex) {
            resultListError("Could not read the "
                    + resultListFilename
                    + " file. Please re-run the tool with -Ddiscoveronly=true option to generate the list of ICU4J jars.");
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Removes a file from the ICUFile list.
     * 
     * @param file
     *            The file to remove.
     */
    public void remove(File file) {
        if (icuFileList.size() > 0) {
            Iterator iter = iterator();
            int i = 0;
            while (iter.hasNext()) {
                ICUFile icuFile = (ICUFile) iter.next();
                if (icuFile.getFile().getAbsoluteFile().equals(file.getAbsoluteFile())) {
                    icuFileList.remove(icuFile);
                    fireTableRowsDeleted(i, i);
                    return;
                }
                i++;
            }
        }
    }

    /**
     * Removes a selection of files from the ICUFile list.
     * 
     * @param indices
     *            The indices of the files to remove.
     */
    public void remove(int[] indices) {
        if (icuFileList.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            for (int i = indices.length - 1; i >= 0; i--) {
                icuFileList.remove(indices[i]);
                fireTableRowsDeleted(indices[i], indices[i]);
            }
        }
    }

    /**
     * Clears the ICUFile list.
     */
    public void removeAll() {
        if (icuFileList.size() > 0) {
            int lastIndex = icuFileList.size() - 1;
            icuFileList.clear();
            fireTableRowsDeleted(0, lastIndex);
        }
    }

    /**
     * Saves a list of ICUFiles to the given result list file. Lines will be of the form <b><i>pathstring</i><tab><i>tzversion</i></b>.
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void saveResults() throws IOException, IllegalArgumentException {
        logger.printlnToScreen("Saving to file " + resultListFilename + " ...");
        BufferedWriter writer = null;
        ICUFile icuFile = null;

        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(resultListFile), "UTF-8"), 4 * 1024);
            Iterator iter = iterator();
            while (iter.hasNext()) {
                icuFile = (ICUFile) iter.next();
                String line = icuFile.getFile().getPath() + '\t' + icuFile.getTZVersion();
                logger.printlnToScreen(line);
                writer.write(line);
            }
        } catch (FileNotFoundException ex) {
            resultListError("Could not create the " + resultListFilename + " file.");
        } catch (IOException ex) {
            resultListError("Could not write to the " + resultListFilename + " file.");
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Updates a selection of the ICUFiles given a URL as the source of the update and a backup
     * directory as a place to store a copy of the un-updated file.
     * 
     * @param indices
     *            The indices of the ICUFiles to update.
     * @param updateURL
     *            The URL to use a source of the update.
     * @param backupDir
     *            The directory in which to store backups.
     * @throws InterruptedException
     */
    public int update(int[] indices, URL updateURL, File backupDir) throws InterruptedException {
        int numberFailed = 0;
        if (icuFileList.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int n = indices.length;

            int k = 0;
            Iterator iter = iterator();
            for (int i = 0; k < n && iter.hasNext(); i++)
                if (i == indices[k])
                    try {
                        // update the file
                        ((ICUFile) iter.next()).update(updateURL, backupDir);
                        fireTableRowsUpdated(i, i);
                        k++;
                    } catch (IOException ex) {
                        // could not update the jar
                        logger.errorln(ex.getMessage());
                        numberFailed++;
                    }
                else
                    iter.next();
        }

        return numberFailed;
    }

    /**
     * Updates all of the ICUFiles given a URL as the source of the update and a backup directory as
     * a place to store a copy of the un-updated file.
     * 
     * @param updateURL
     *            The URL to use a source of the update.
     * @param backupDir
     *            The directory in which to store backups.
     * @throws InterruptedException
     */
    public int updateAll(URL updateURL, File backupDir) throws InterruptedException {
        int numberFailed = 0;
        if (icuFileList.size() > 0) {
            int n = icuFileList.size();
            Iterator iter = iterator();
            for (int i = 0; i < n; i++)
                try {
                    ((ICUFile) iter.next()).update(updateURL, backupDir);
                    fireTableRowsUpdated(i, i);
                } catch (IOException ex) {
                    // could not update the jar
                    logger.errorln(ex.getMessage());
                    numberFailed++;
                }
        }
        return numberFailed;
    }

    /**
     * Throws an IllegalArgumentException with the given message.
     * 
     * @param message
     *            The message.
     * @throws IllegalArgumentException
     */
    private void resultListError(String message) throws IOException {
        throw new IOException("Error in " + resultListFilename + ": " + message);
    }

    /**
     * Logs as an error a given message and line number.
     * 
     * @param message
     *            The message.
     * @param lineNumber
     *            The line number.
     */
    private void resultListError(String message, int lineNumber) {
        logger.errorln("Error in " + resultListFilename + " (line " + lineNumber + "): " + message);
    }
}
