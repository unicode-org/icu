/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Finds all updatable ICU4J jars in a set of specified directories.
 */
public class ICUJarFinder {

    /**
     * Searchs the directories / files represented in <code>paths</code> for
     * valid ICU4J jars. The logic for determining if a file is an ICU4J jar is
     * taken care of by the constructor of ICUFile. The resulting ICUFile's are
     * then added to the result model.
     * 
     * @param resultModel
     *            The result model to add any discovered ICU4J jars to.
     * @param logger
     *            The current logger.
     * @param paths
     *            The list of paths to include to and exclude from the search.
     * @param subdirs
     *            Whether to include subdirectories in the search.
     * @param curDir
     *            The base directory of the tool.
     * @param backupDir
     *            The backup directory, or null if none. The backup directory is
     *            excluded from the search
     * @return The same result model as given.
     * @throws InterruptedException
     */
    public static ResultModel search(ResultModel resultModel, Logger logger,
            IncludePath[] paths, boolean subdirs, File curDir, File backupDir)
            throws InterruptedException {
        // sift the included / excluded paths into two seperate arraylists
        List included = new ArrayList();
        List excluded = new ArrayList();
        for (int i = 0; i < paths.length; i++) {
            IncludePath path = paths[i];
            if (path.isIncluded())
                included.add(path.getPath());
            else
                excluded.add(path.getPath());
        }

        // if the backup dir is specified, don't search it
        if (backupDir != null)
            excluded.add(backupDir);

        // the icutzu_home dir must be specified, don't search it
        excluded.add(curDir);

        // search each of the included files/directories
        for (int i = 0; i < included.size(); i++)
            search(resultModel, logger, (File) included.get(i), excluded,
                    subdirs, 0);

        // chain the result model
        return resultModel;
    }

    /**
     * Checks a specific file. If the file is an ICU4J jar that can be updated,
     * then the ICUFile representing that file is added to the result model. If
     * the file is a directory, the directory is then recursed.
     * 
     * @param resultModel
     *            The result model to add any discovered ICU4J jars to.
     * @param logger
     *            The current logger.
     * @param statusBar
     *            The status bar for status-bar messages, or null if none is
     *            present.
     * @param file
     *            The current file to check or directory to search.
     * @param excluded
     *            The list of all directories excluded in the search.
     * @param subdirs
     *            Whether to include subdirectories in the search.
     * @param depth
     *            The current depth of the search.
     * @return The same result model as given.
     * @throws InterruptedException
     */
    private static ResultModel search(ResultModel resultModel, Logger logger,
            File file, List excluded, boolean subdirs, int depth)
            throws InterruptedException {
        // check for interruptions
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException();

        // make sure the current file/directory isn't excluded
        Iterator iter = excluded.iterator();
        while (iter.hasNext())
            if (file.getAbsolutePath().equalsIgnoreCase(
                    ((File) iter.next()).getAbsolutePath()))
                return resultModel;
        if (file.isDirectory() && (subdirs || depth == 0)) {
            // recurse through each file/directory inside this directory
            File[] dirlist = file.listFiles();
            if (dirlist != null && dirlist.length > 0) {
                // notify the user that something is happening
                if (depth <= 2) {
                    logger.setStatus(file.getPath());
                    logger.printlnToScreen(file.getPath());
                }
                for (int i = 0; i < dirlist.length; i++)
                    search(resultModel, logger, dirlist[i], excluded, subdirs,
                            depth + 1);
            }
        } else {
            // attempt to create an ICUFile object on the current file and add
            // it to the result model if possible
            try {
                // if the file/directory is an ICU jar file that we can
                // update, add it to the results
                resultModel.add(new ICUFile(file, logger));
                logger.loglnToBoth("Added " + file.getPath() + ".");
            } catch (IOException ex) {
                // if it's not an ICU jar file that we can update, ignore it
            }
        }

        // chain the result model
        return resultModel;
    }

    /**
     * An empty constructor that restricts construction.
     */
    private ICUJarFinder() {
    }
}
