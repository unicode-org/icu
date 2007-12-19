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
     * The delay in milliseconds between showing directories to the command line user.
     */
    public static final long DELAY = 5000; // 5 seconds

    /**
     * The delay in milliseconds between showing directories to the command line user.
     */
    public static long lastShowtime = 0; // 5 seconds

    /**
     * Searchs the directories / files represented in <code>paths</code> for valid ICU4J jars. The
     * logic for determining if a file is an ICU4J jar is taken care of by the constructor of
     * ICUFile. The resulting ICUFile's are then added to the result model.
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
     *            The backup directory, or null if none. The backup directory is excluded from the
     *            search
     * @return The same result model as given.
     * @throws InterruptedException
     */
    public static ResultModel search(ResultModel resultModel, Logger logger, IncludePath[] paths,
            boolean subdirs, File curDir, File backupDir) throws InterruptedException {
        // sift the included / excluded paths into two seperate arraylists
        List included = new ArrayList();
        List excluded = new ArrayList();
        for (int i = 0; i < paths.length; i++) {
            IncludePath path = paths[i];
            File file = path.getPath();
            try {
                file = file.getCanonicalFile();
            } catch (IOException ex) {
                // recover in the simplest way, but report the error
                file = file.getAbsoluteFile();
                logger.errorln(ex.getMessage());
            }
            if (path.isIncluded())
                included.add(file);
            else
                excluded.add(file);
        }

        // if the backup dir is specified, don't search it
        if (backupDir != null) {
            File file = backupDir;
            try {
                file = file.getCanonicalFile();
            } catch (IOException ex) {
                // recover in the simplest way, but report the error
                file = file.getAbsoluteFile();
                logger.errorln(ex.getMessage());
            }
            excluded.add(file);
        }

        // exclude the icu4j.jar that comes with this tool
        File file = new File(curDir.getPath(), "icu4j.jar");
        try {
            file = file.getCanonicalFile();
        } catch (IOException ex) {
            // recover in the simplest way, but report the error
            file = file.getAbsoluteFile();
            logger.errorln(ex.getMessage());
        }
        excluded.add(file);

        // search each of the included files/directories
        for (int i = 0; i < included.size(); i++)
            search(resultModel, logger, (File) included.get(i), excluded, subdirs, 0);

        // chain the result model
        return resultModel;
    }

    /**
     * Checks a specific file. If the file is an ICU4J jar that can be updated, then the ICUFile
     * representing that file is added to the result model. If the file is a directory, the
     * directory is then recursed.
     * 
     * @param resultModel
     *            The result model to add any discovered ICU4J jars to.
     * @param logger
     *            The current logger.
     * @param statusBar
     *            The status bar for status-bar messages, or null if none is present.
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
    private static ResultModel search(ResultModel resultModel, Logger logger, File file,
            List excluded, boolean subdirs, int depth) throws InterruptedException {
        // ensure we are not following a symbolic link
        if (isSymbolic(file))
            return resultModel;

        // ensure that the file is in canonical form
        try {
            file = file.getCanonicalFile();
        } catch (IOException ex) {
            logger.errorln(ex.getMessage());
            return resultModel;
        }

        // check for interruptions
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException();

        // make sure the current file/directory isn't excluded
        Iterator iter = excluded.iterator();
        while (iter.hasNext())
            if (file.equals(((File) iter.next())))
                return resultModel;

        if ((subdirs || depth == 0) && file.isDirectory()) {
            // recurse through each file/directory inside this directory
            File[] dirlist = file.listFiles();
            if (dirlist != null && dirlist.length > 0) {
                // notify the user that something is happening
                long curTime = System.currentTimeMillis();
                if (depth <= 1 || curTime - lastShowtime > DELAY) {
                    lastShowtime = curTime;
                    logger.printlnToScreen(file.getPath());

                    // give a chance for the UI to display the message
                    if (Thread.currentThread().isInterrupted())
                        throw new InterruptedException();
                    Thread.sleep(0);
                }

                // recurse
                for (int i = 0; i < dirlist.length; i++)
                    search(resultModel, logger, dirlist[i], excluded, subdirs, depth + 1);
            }
        } else {
            // attempt to create an ICUFile object on the current file and add
            // it to the result model if possible
            try {
                // if the file/directory is an ICU jar file that we can
                // update, add it to the results
                resultModel.add(new ICUFile(file, logger));
                logger.printlnToBoth("Added " + file.getPath());
            } catch (IOException ex) {
                // if it's not an ICU jar file that we can update, ignore it
            }
        }

        // chain the result model
        return resultModel;
    }

    /**
     * Tests whether a file is a symbolic link by comparing the absolute path with the canonical
     * path.
     * 
     * @param file
     *            The file to check.
     * @return Whether the file is a symbolic link.
     */
    private static boolean isSymbolic(File file) {
        try {
            File parent = file.getParentFile();
            if (parent == null)
                parent = new File(".");
            File betterFile = new File(parent.getCanonicalPath(), file.getName());
            return !betterFile.getAbsoluteFile().equals(betterFile.getCanonicalFile());
        } catch (IOException ex) {
            // if getCanonicalFile throws an IOException for this file, we won't
            // want to dig into this path
            return false;
        }
    }

    /**
     * An empty constructor that restricts construction.
     */
    private ICUJarFinder() {
    }
}
