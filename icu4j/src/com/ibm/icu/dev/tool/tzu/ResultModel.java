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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Represents a list of ICUFiles that is usable by any class that uses
 * AbstractTableModels (such as a JTable in swing). Also contains methods to
 * begin updates on those ICUFiles and methods to load and save a result list
 * from and to a file.
 */
class ResultModel extends AbstractTableModel {
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
     * Returns the number of columns for each represented ICUFile.
     * 
     * @return The number of columns for each represented ICUFile.
     */
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    /**
     * Returns the number of ICUFiles represented.
     * 
     * @return The number of ICUFiles represented.
     */
    public int getRowCount() {
        List list = hidden ? permissibleList : completeList;
        return (list == null) ? 0 : list.size();
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
     * Returns the item at the given row and column. The row determines which
     * ICUFile is used, and the column determines which piece of data should be
     * used.
     * 
     * @param row
     *            Which ICU file to use.
     * @param col
     *            Which piece of data to use. Should be one of the following:
     *            <ul>
     *            <li>COLUMN_FILE_PATH</li>
     *            <li>COLUMN_ICU_VERSION</li>
     *            <li>COLUMN_TZ_VERSION</li>
     *            <li>COLUMN_READABLE</li>
     *            <li>COLUMN_WRITABLE</li>
     *            </ul>
     * @return The item at the given row and column. Will always be a String.
     */
    public Object getValueAt(int row, int col) {
        List list = hidden ? permissibleList : completeList;
        ICUFile entry = ((ICUFile) list.get(row));
        switch (col) {
        case COLUMN_FILE_NAME:
            return entry.getPath();
        case COLUMN_FILE_PATH:
            return entry.getFilename();
        case COLUMN_ICU_VERSION:
            return entry.getICUVersion();
        case COLUMN_TZ_VERSION:
            return entry.getTZVersion();
        case COLUMN_READABLE:
            return entry.getFile().canRead() ? "Yes" : "No";
        case COLUMN_WRITABLE:
            return entry.getFile().canWrite() ? "Yes" : "No";
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
        List list = hidden ? permissibleList : completeList;
        return list.iterator();
    }

    /**
     * Sets whether the table should hide ICUFiles that lack permissions.
     * 
     * @param value
     *            Whether the table should hide ICUFiles that lack permissions.
     */
    public void setHidden(boolean value) {
        hidden = value;
        fireTableDataChanged();
    }

    /**
     * Adds a file to the ICUFile list.
     * 
     * @param filename
     *            The name of the file.
     * @return Whether the file was added successfully (which is determined by
     *         if it is an updatable ICU4J jar).
     */
    public boolean add(String filename) {
        return add(new File(filename));
    }

    /**
     * Adds a file to the ICUFile list.
     * 
     * @param file
     *            The file.
     * @return Whether the file was added successfully (which is determined by
     *         if it is an updatable ICU4J jar).
     */
    public boolean add(File file) {
        try {
            ICUFile entry = new ICUFile(file, logger);
            if (file.canRead() && file.canWrite())
                add(permissibleList, hidden, entry);
            add(completeList, !hidden, entry);
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Adds a file to the ICUFile list.
     * 
     * @param entry
     *            The file.
     */
    public void add(ICUFile entry) {
        File file = entry.getFile();
        if (file.canRead() && file.canWrite())
            add(permissibleList, hidden, entry);

        add(completeList, !hidden, entry);
    }

    /**
     * Removes a file from the ICUFile list.
     * 
     * @param file
     *            The file to remove.
     */
    public void remove(File file) {
        remove(permissibleList, hidden, file);
        remove(completeList, !hidden, file);
    }

    /**
     * Removes a selection of files from the ICUFile list.
     * 
     * @param indices
     *            The indices of the files to remove.
     */
    public void remove(int[] indices) {
        remove(permissibleList, hidden, indices);
        remove(completeList, !hidden, indices);
    }

    /**
     * Clears the ICUFile list.
     */
    public void removeAll() {
        removeAll(permissibleList, hidden);
        removeAll(completeList, !hidden);
    }

    /**
     * Adds a given ICUFile to the given list.
     * 
     * @param list
     *            The list to add to.
     * @param fire
     *            Whether to fire off an event for this adding.
     * @param entry
     *            The ICUFile to add.
     */
    private void add(List list, boolean fire, ICUFile entry) {
        remove(list, fire, entry.getFile());
        list.add(entry);
        int index = list.size() - 1;
        if (fire)
            fireTableRowsInserted(index, index);
    }

    /**
     * Removes a given ICUFile from the given list.
     * 
     * @param list
     *            The list to remove from.
     * @param fire
     *            Whether to fire off an event for this removal.
     * @param entry
     *            The ICUFile to remove.
     */
    private void remove(List list, boolean fire, File file) {
        if (list.size() > 0) {
            Iterator iter = list.iterator();
            int i = 0;
            while (iter.hasNext()) {
                ICUFile entry = (ICUFile) iter.next();
                if (entry.getFile().equals(file)) {
                    list.remove(entry);
                    if (fire)
                        fireTableRowsDeleted(i, i);
                    return;
                }
                i++;
            }
        }
    }

    /**
     * Removes a selection of ICUFiles from the given list.
     * 
     * @param list
     *            The list to remove from.
     * @param fire
     *            Whether to fire off events for removals.
     * @param indices
     *            The indices of ICUFiles in the list to remove.
     */
    private void remove(List list, boolean fire, int[] indices) {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int max = indices[indices.length - 1];
            int min = indices[0];
            for (int i = indices.length - 1; i >= 0; i--)
                list.remove(indices[i]);
            if (fire)
                fireTableRowsDeleted(min, max);
        }
    }

    /**
     * Removes all ICUFiles from the given list.
     * 
     * @param list
     *            The list to remove from.
     * @param fire
     *            Whether to fire off events for removals.
     */
    private void removeAll(List list, boolean fire) {
        if (list.size() > 0) {
            int index = list.size() - 1;
            list.clear();
            if (fire)
                fireTableRowsDeleted(0, index);
        }
    }

    /**
     * Updates a selection of the ICUFiles given a URL as the source of the
     * update and a backup directory as a place to store a copy of the
     * un-updated file.
     * 
     * @param indices
     *            The indices of the ICUFiles to update.
     * @param updateURL
     *            The URL to use a source of the update.
     * @param backupDir
     *            The directory in which to store backups.
     * @throws InterruptedException
     */
    public void update(int[] indices, URL updateURL, File backupDir)
            throws InterruptedException {
        if (hidden)
            update(permissibleList, indices, updateURL, backupDir);
        else
            update(completeList, indices, updateURL, backupDir);
    }

    /**
     * Updates all of the ICUFiles given a URL as the source of the update and a
     * backup directory as a place to store a copy of the un-updated file.
     * 
     * @param updateURL
     *            The URL to use a source of the update.
     * @param backupDir
     *            The directory in which to store backups.
     * @throws InterruptedException
     */
    public void updateAll(URL updateURL, File backupDir)
            throws InterruptedException {
        if (hidden)
            updateAll(permissibleList, updateURL, backupDir);
        else
            updateAll(completeList, updateURL, backupDir);
    }

    /**
     * Updates a selection of the ICUFiles given a URL as the source of the
     * update and a backup directory as a place to store a copy of the
     * un-updated file.
     * 
     * @param indices
     *            The indices of the ICUFiles to update.
     * @param updateURL
     *            The URL to use a source of the update.
     * @param backupDir
     *            The directory in which to store backups.
     * @throws InterruptedException
     */
    private void update(List list, int[] indices, URL updateURL, File backupDir)
            throws InterruptedException {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int n = indices.length;

            int k = 0;
            Iterator iter = list.iterator();
            for (int i = 0; k < n && iter.hasNext(); i++)
                if (i == indices[k])
                    try {
                        k++;
                        ((ICUFile) iter.next()).update(updateURL, backupDir);
                        fireTableRowsUpdated(i, i);
                        Thread.sleep(0);
                    } catch (IOException ex) {
                        // could not update the jar
                        ex.printStackTrace();
                    }
                else
                    iter.next();
        }
    }

    /**
     * Updates all of the ICUFiles given a URL as the source of the update and a
     * backup directory as a place to store a copy of the un-updated file.
     * 
     * @param updateURL
     *            The URL to use a source of the update.
     * @param backupDir
     *            The directory in which to store backups.
     * @throws InterruptedException
     */
    private void updateAll(List list, URL updateURL, File backupDir)
            throws InterruptedException {
        if (list.size() > 0) {
            int n = list.size();
            Iterator iter = list.iterator();
            for (int i = 0; i < n; i++)
                try {
                    ((ICUFile) iter.next()).update(updateURL, backupDir);
                    fireTableRowsUpdated(i, i);
                    Thread.sleep(0);
                } catch (IOException ex) {
                    // could not update the jar
                    ex.printStackTrace();
                }
        }
    }

    /**
     * Loads a list of ICUFiles from the given result list file. Lines should be
     * of the form <b><i>pathstring</i><tab><i>tzversion</i></b>.
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
            reader = new BufferedReader(new FileReader(resultListFile));
            while (reader.ready()) {
                line = reader.readLine().trim();
                logger.printlnToScreen(line);

                if (line.length() >= 1 && (tab = line.lastIndexOf('\t')) >= 0) {
                    if (!add(filename = line.substring(0, tab)))
                        resultListError(filename
                                + " is not an updatable ICU4J file", lineNumber);
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
     * Saves a list of ICUFiles to the given result list file. Lines will be of
     * the form <b><i>pathstring</i><tab><i>tzversion</i></b>.
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void saveResults() throws IOException, IllegalArgumentException {
        logger.printlnToScreen("Saving to file " + resultListFilename + " ...");
        BufferedWriter writer = null;
        ICUFile icuFile = null;

        try {
            writer = new BufferedWriter(new FileWriter(resultListFile));
            Iterator iter = (hidden ? permissibleList : completeList)
                    .iterator();
            while (iter.hasNext()) {
                icuFile = (ICUFile) iter.next();
                String line = icuFile.getFile().getPath() + '\t'
                        + icuFile.getTZVersion() + "\n";
                logger.printlnToScreen(line);
                writer.write(line);
            }
        } catch (FileNotFoundException ex) {
            resultListError("Could not create the " + resultListFilename
                    + " file.");
        } catch (IOException ex) {
            resultListError("Could not write to the " + resultListFilename
                    + " file.");
        } finally {
            try {
                if (writer != null)
                    writer.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Throws an IllegalArgumentException with the given message and line
     * number.
     * 
     * @param message
     *            The message.
     * @param lineNumber
     *            The line number.
     * @throws IllegalArgumentException
     */
    private void resultListError(String message, int lineNumber)
            throws IllegalArgumentException {
        throw new IllegalArgumentException("Error in " + resultListFilename
                + " (line " + lineNumber + "): " + message);
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
     * A list of names of the columns in a result model.
     */
    public static final String[] COLUMN_NAMES = new String[] { "Path", "Name",
            "ICU Version", "TZ Version", "Readable", "Writable" };

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
     * The column designating whether a file is readable.
     */
    public static final int COLUMN_READABLE = 4;

    /**
     * The column designating whether a file is writable.
     */
    public static final int COLUMN_WRITABLE = 5;

    /**
     * The complete list of ICUFiles represented by this result model.
     */
    private List completeList = new ArrayList();

    /**
     * The list of only ICUFiles that are readable and writable.
     */
    private List permissibleList = new ArrayList();

    /**
     * If hidden is true, only ICUFiles that are readable and writable are
     * active. Otherwise, all ICUFiles are readable and active.
     */
    private boolean hidden = true;

    /**
     * The result list file where results are saved and stored.
     */
    private File resultListFile;

    /**
     * The filename of the result list file where results are saved and stored.
     */
    private String resultListFilename;

    /**
     * The current logger.
     */
    private Logger logger;

    /**
     * The serializable UID.
     */
    public static final long serialVersionUID = 1338;

}
