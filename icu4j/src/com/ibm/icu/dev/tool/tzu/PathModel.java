/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractListModel;

class PathModel extends AbstractListModel {
    public PathModel(ResultModel resultModel, Logger logger) {
        this.resultModel = resultModel;
        this.logger = logger;
    }

    public Iterator iterator() {
        return list.iterator();
    }

    public Object getElementAt(int index) {
        return list.get(index);
    }

    public int getSize() {
        return (list == null) ? 0 : list.size();
    }

    public void loadPaths() {
        BufferedReader reader = null;
        int lineNumber = 1;
        String line;
        char sign;

        try {
            reader = new BufferedReader(new FileReader(PATHLIST_FILENAME));
            while (reader.ready()) {
                line = reader.readLine().trim();

                if (line.length() >= 1) {
                    sign = line.charAt(0);
                    if (sign != '#') {
                        if (sign != '+' && sign != '-' && !"all".equals(line))
                            pathlistError(
                                    "Each path entry must start with a + or - to denote inclusion/exclusion",
                                    lineNumber);// error
                        if (!add(line))
                            pathlistError(
                                    "\""
                                            + line.substring(1).trim()
                                            + "\" is not a valid file or directory (perhaps it does not exist?)",
                                    lineNumber);// error
                    }
                }

                lineNumber++;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                reader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean add(String filename) {
        if ("all".equals(filename)) {
            addAllDrives();
            return true;
        } else {
            return add(new IncludePath(new File(filename.substring(1).trim()),
                    filename.charAt(0) == '+'));
        }
    }

    public boolean add(IncludePath path) {
        remove(path);

        if (path.getPath().exists()) {
            list.add(path);
            int index = list.size() - 1;
            fireIntervalAdded(this, index, index);
            return true;
        } else {
            return false;
        }
    }

    public void addAllDrives() {
        File[] roots = File.listRoots();
        for (int i = 0; i < roots.length; i++)
            add(new IncludePath(roots[i], true));
    }

    public boolean remove(IncludePath path) {
        int index = list.indexOf(path);
        if (index != -1) {
            list.remove(index);
            fireIntervalRemoved(this, index, index);
            return true;
        } else {
            return false;
        }
    }

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

    public void removeAll() {
        if (list.size() > 0) {
            int index = list.size() - 1;
            list.clear();
            fireIntervalRemoved(this, 0, index);
        }
    }

    public void search(int[] indices, boolean subdirs, File backupDir)
            throws InterruptedException {
        if (list.size() > 0 && indices.length > 0) {
            Arrays.sort(indices);
            int n = indices.length;
            IncludePath[] paths = new IncludePath[n];

            int k = 0;
            Iterator iter = list.iterator();
            for (int i = 0; k < n && iter.hasNext(); i++)
                if (i == indices[k])
                    paths[k++] = (IncludePath) iter.next();
                else
                    iter.next();

            ICUJarFinder.search(resultModel, logger, paths, subdirs, backupDir);
        }
    }

    public void searchAll(boolean subdirs, File backupDir)
            throws InterruptedException {
        if (list.size() > 0) {
            int n = list.size();
            IncludePath[] paths = new IncludePath[n];
            Iterator iter = list.iterator();
            for (int i = 0; i < n; i++)
                paths[i] = (IncludePath) iter.next();
            ICUJarFinder.search(resultModel, logger, paths, subdirs, backupDir);
        }
    }

    private static void pathlistError(String message, int lineNumber) {
        throw new IllegalArgumentException("Error in " + PATHLIST_FILENAME
                + " (line " + lineNumber + "): " + message);
    }

    private List list = new ArrayList(); // list of paths (Files)

    private ResultModel resultModel = null;

    public static final String PATHLIST_FILENAME = "DirectorySearch.txt";

    public static final long serialVersionUID = 1337;

    private Logger logger;
}