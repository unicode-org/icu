/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;

/**
 * Represents a list of IncludePaths that is usable by any class that uses
 * AbstractListModels (such as a JList in swing). Also contains methods to begin
 * a search on those paths using ICUJarFinder and placing the results in a
 * ResultModel, and methods to load a path list from a file.
 */
class PathModel extends AbstractListModel {
    /**
     * The serializable UID.
     */
    public static final long serialVersionUID = 1337;

    /**
     * The list of paths as IncludePaths.
     */
    private List list = new ArrayList();

    /**
     * The current logger.
     */
    private Logger logger;

    /**
     * The paths file where the paths are stored.
     */
    private File pathListFile;

    /**
     * The filename of the paths file where the paths are stored.
     */
    private String pathListFilename;

    /**
     * Constructs an empty path model.
     * 
     * @param pathFile
     *            The file to load the path list from.
     * @param logger
     *            The current logger.
     */
    public PathModel(Logger logger, File pathFile) {
        this.logger = logger;
        this.pathListFile = pathFile;
        this.pathListFilename = pathFile.getName();
    }

    /**
     * Adds an IncludePath to the path list if it exists and is unique.
     * 
     * @param path
     *            An existing path.
     * @return Whether or not the given IncludePath exists.
     */
    public boolean add(IncludePath path) {
        remove(path);

        if (path.getPath().exists()) {
            list.add(path);
            int index = list.size() - 1;
            fireIntervalAdded(this, index, index);
            return true;
        }

        return false;
    }

    /**
     * Adds a filename to the path list if it is valid and unique. The filename
     * must either be of the form (<b>+</b>|<b>-</b>)<i>pathstring</i> and
     * exist, or of the form <b>all</b>. In the case of the latter, all drives
     * are added to the path list.
     * 
     * @param includeFilename
     *            A filename in the form above.
     * @return Whether or not <code>includeFilename</code> is both of the form
     *         detailed above and exists.
     */
    public boolean add(String includeFilename) {
        if ("all".equalsIgnoreCase(includeFilename)) {
            logger
                    .printlnToScreen("The tool will search all drives for ICU4J jars except any excluded directories specified");
            addAllDrives();
            return true;
        }

        return add(new IncludePath(
                new File(includeFilename.substring(1).trim()), includeFilename
                        .charAt(0) == '+'));
    }

    /**
     * Adds all drives to the path list.
     */
    public void addAllDrives() {
        File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++)
            add(new IncludePath(roots[i], true));
    }

    /**
     * Returns the path at the element at a particular index of the path list.
     * 
     * @param index
     *            The index of the element of the path list to return.
     * @return The path at the specified index of the path list. Guaranteed to
     *         always be an IncludePath.
     */
    public Object getElementAt(int index) {
        return list.get(index);
    }

    /**
     * Returns the size of the path list.
     * 
     * @return The size of the path list.
     */
    public int getSize() {
        return (list == null) ? 0 : list.size();
    }

    /**
     * Returns an iterator of the path list.
     * 
     * @return An iterator of the path list.
     */
    public Iterator iterator() {
        return list.iterator();
    }

    /**
     * Loads a list of paths from the given path list file. Each path must be of
     * the form
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void loadPaths() throws IOException, IllegalArgumentException {
        logger.printlnToScreen("Scanning " + pathListFilename + " file...");
        logger.printlnToScreen(pathListFilename + " file contains");

        BufferedReader reader = null;
        int lineNumber = 1;
        String line;
        char sign;

        try {
            reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(pathListFile), "UTF-8"), 4 * 1024);
            while ((line = reader.readLine()) != null) {
                if (line.length() >= 1 && line.charAt(0) == '\ufeff')
                    line = line.substring(1);
                line = line.trim();

                if (line.length() >= 1) {
                    sign = line.charAt(0);
                    if (sign != '#') {
                        logger.printlnToScreen(line);
                        if (sign != '+' && sign != '-'
                                && !"all".equalsIgnoreCase(line))
                            pathListError(
                                    "Each path entry must start with a + or - to denote inclusion/exclusion",
                                    lineNumber);
                        if (!add(line))
                            logger
                                    .errorln(line.substring(1).trim()
                                            + " is not a valid file or directory (perhaps it does not exist?)");
                    }
                }

                lineNumber++;
            }
        } catch (FileNotFoundException ex) {
            pathListError("The " + pathListFilename + " file doesn't exist.");
        } catch (IOException ex) {
            pathListError("Could not read the " + pathListFilename + " file.");
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException ex) {
            }
        }
    }

    /**
     * Removes a path from the path list. Since there are no duplicates in the
     * path list, this method either removes a single path or removes none.
     * 
     * @param path
     *            The path to remove from the path list.
     */
    public void remove(IncludePath path) {
        int index = list.indexOf(path);
        if (index != -1) {
            list.remove(index);
            fireIntervalRemoved(this, index, index);
        }
    }

    /**
     * Removes a selection of paths from the path list by index.
     * 
     * @param indices
     *            The indices of the path list to remove.
     */
    public void remove(int[] indices) {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int max = indices[indices.length - 1];
            int min = indices[0];
            for (int i = indices.length - 1; i >= 0; i--)
                list.remove(indices[i]);
            fireIntervalRemoved(this, min, max);
        }
    }

    /**
     * Clears the path list.
     */
    public void removeAll() {
        if (list.size() > 0) {
            int index = list.size() - 1;
            list.clear();
            fireIntervalRemoved(this, 0, index);
        }
    }

    /**
     * Searches a selection of paths in the path list for updatable ICU4J jars.
     * Results are added to the result model. The indices provided are the
     * indices of the path list to search.
     * 
     * @param resultModel
     *            The result model to store the results of the search.
     * @param indices
     *            The indices of the path list to use in the search.
     * @param subdirs
     *            Whether to search subdiretories.
     * @param curDir
     *            The base directory of the tool.
     * @param backupDir
     *            Where to store backup files.
     * @throws InterruptedException
     */
    public void search(ResultModel resultModel, int[] indices, boolean subdirs,
            File curDir, File backupDir) throws InterruptedException {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int n = indices.length;
            IncludePath[] paths = new IncludePath[n];

            int k = 0;
            Iterator iter = iterator();
            for (int i = 0; k < n && iter.hasNext(); i++)
                if (i == indices[k])
                    paths[k++] = (IncludePath) iter.next();
                else
                    iter.next();

            ICUJarFinder.search(resultModel, logger, paths, subdirs, curDir,
                    backupDir);
        }
    }

    /**
     * Searches each path in the path list for updatable ICU4J jars. Results are
     * added to the result model.
     * 
     * @param resultModel
     *            The result model to store the results of the search.
     * @param subdirs
     *            Whether to search subdiretories.
     * @param curDir
     *            The base directory of the tool.
     * @param backupDir
     *            Where to store backup files.
     * @throws InterruptedException
     */
    public void searchAll(ResultModel resultModel, boolean subdirs,
            File curDir, File backupDir) throws InterruptedException {
        if (list.size() > 0) {
            int n = list.size();
            IncludePath[] paths = new IncludePath[n];
            Iterator iter = iterator();
            for (int i = 0; i < n; i++)
                paths[i] = (IncludePath) iter.next();
            ICUJarFinder.search(resultModel, logger, paths, subdirs, curDir,
                    backupDir);
        }
    }

    /**
     * Throws an IOException with the specified message.
     * 
     * @param message
     *            The message to put in the exception.
     * @throws IOException
     */
    private void pathListError(String message) throws IOException {
        throw new IOException("Error in " + pathListFilename + ": " + message);
    }

    /**
     * Throws an IllegalArgumentException with the specified message and line
     * number.
     * 
     * @param message
     *            The message to put in the exception.
     * @param lineNumber
     *            The line number to put in the exception.
     */
    private void pathListError(String message, int lineNumber) {
        logger.errorln("Error in " + pathListFilename + " (line " + lineNumber
                + "): " + message);
    }
}
