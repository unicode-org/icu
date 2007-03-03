/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.text.JTextComponent;

public class ICUJarFinder {
    private ICUJarFinder() {
    }

    public static ResultModel search(ResultModel resultModel, Logger logger, JTextComponent statusBar,
            IncludePath[] paths, boolean subdirs, File backupDir) throws InterruptedException {
        List included = new ArrayList();
        List excluded = new ArrayList();

        for (int i = 0; i < paths.length; i++) {
            IncludePath path = (IncludePath) paths[i];
            if (path.isIncluded())
                included.add(path.getPath());
            else
                excluded.add(path.getPath());
        }

        if (backupDir != null)
            excluded.add(backupDir);

        logger.logln("*************", Logger.VERBOSE);
        logger.logln("Search Paths", Logger.VERBOSE);
        logger.logln("Included:", Logger.VERBOSE);
        for (int i = 0; i < included.size(); i++)
            logger.logln(included.get(i).toString(), Logger.VERBOSE);
        logger.logln("Excluded:", Logger.VERBOSE);
        for (int i = 0; i < excluded.size(); i++)
            logger.logln(excluded.get(i).toString(), Logger.VERBOSE);
        logger.logln("*************", Logger.VERBOSE);

        for (int i = 0; i < included.size(); i++)
            search(resultModel, logger, statusBar, (File) included.get(i), excluded, subdirs, 0);

        return resultModel;
    }

    private static ResultModel search(ResultModel resultModel, Logger logger, JTextComponent statusBar, File file,
            List excluded, boolean subdirs, int depth) throws InterruptedException {
        // check for interruptions
        if (Thread.interrupted())
            throw new InterruptedException();

        // make sure the current file/directory isn't excluded
        Iterator iter = excluded.iterator();
        while (iter.hasNext())
            if (file.getAbsolutePath().equalsIgnoreCase(((File) iter.next()).getAbsolutePath()))
                return resultModel;

        if (file.exists()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                try {
                    // if the file/directory is an ICU jar file that we can
                    // update, add it to the results
                    resultModel.add(new ICUFile(file, logger));
                    logger.logln("Added " + file.getPath() + ".", Logger.NORMAL);
                } catch (IOException ex) {
                    // if it's not an ICU jar file that we can update, ignore it
                    logger.logln("Skipped " + file.getPath() + " (" + ex.getMessage() + ").", Logger.VERBOSE);
                }
            } else if (file.isDirectory() && (subdirs || depth == 0)) {
                // notify the user that something is happening
                logger.println(file.getPath(), Logger.VERBOSE);
                if (statusBar != null)
                    statusBar.setText(file.getPath());

                // recurse through each file/directory inside this directory
                File[] dirlist = file.listFiles();
                if (dirlist != null)
                    for (int i = 0; i < dirlist.length; i++)
                        search(resultModel, logger, statusBar, dirlist[i], excluded, subdirs, depth + 1);
            } else if (file.isFile() && (file.getName().endsWith(".ear") || file.getName().endsWith(".war"))) {
                // for now, ear and .war
                logger.logln("Skipped " + file.getPath() + " (ear/war files not supported).", Logger.NORMAL);
            }
        }
        return resultModel;
    }
}