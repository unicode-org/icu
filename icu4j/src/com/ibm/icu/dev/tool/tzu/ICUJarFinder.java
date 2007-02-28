/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.util.*;
import java.io.*;

public class ICUJarFinder {
    private ICUJarFinder() {
    }

    public static ResultModel search(ResultModel resultModel, Logger logger,
            IncludePath[] paths, boolean subdirs, File backupDir)
            throws InterruptedException {
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

        logger.println("*************", Logger.VERBOSE);
        logger.println("Included:", Logger.VERBOSE);
        for (int i = 0; i < included.size(); i++)
            logger.println(included.get(i).toString(), Logger.VERBOSE);
        logger.println("Excluded:", Logger.VERBOSE);
        for (int i = 0; i < excluded.size(); i++)
            logger.println(excluded.get(i).toString(), Logger.VERBOSE);
        logger.println("*************", Logger.VERBOSE);

        for (int i = 0; i < included.size(); i++)
            search(resultModel, logger, (File) included.get(i), excluded,
                    subdirs, 0);

        return resultModel;
    }

    private static ResultModel search(ResultModel resultModel, Logger logger,
            File file, List excluded, boolean subdirs, int depth)
            throws InterruptedException {
        Iterator iter = excluded.iterator();
        while (iter.hasNext())
            if (file.getAbsolutePath().equalsIgnoreCase(
                    ((File) iter.next()).getAbsolutePath()))
                return resultModel;

        if (file.exists()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                try {
                    resultModel.add(new ICUFile(file, logger));
                    logger.println("Added " + file.getPath() + ".",
                            Logger.NORMAL);
                    logger.logln("Added " + file.getPath() + ".");
                } catch (IOException ex) {
                    // if it's not an ICU file we care about, ignore it
                    logger.println("Skipped " + file.getPath() + " ("
                            + ex.getMessage() + ").", Logger.VERBOSE);
                    logger.logln("Skipped " + file.getPath() + " ("
                            + ex.getMessage() + ").");
                }
            } else if (file.isDirectory() && (subdirs || depth == 0)) {
                logger.println(file.getPath(), Logger.VERBOSE);
                File[] dirlist = file.listFiles();
                if (dirlist != null)
                    for (int i = 0; i < dirlist.length; i++)
                        search(resultModel, logger, dirlist[i], excluded,
                                subdirs, depth + 1);
                if (Thread.interrupted())
                    throw new InterruptedException();
            } else if (file.isFile()
                    && (file.getName().endsWith(".ear") || file.getName()
                            .endsWith(".war"))) {
                logger.logln("Skipped " + file.getPath()
                        + " (ear/war files not supported).");
                logger.println("Skipped " + file.getPath()
                        + " (ear/war files not supported).", Logger.NORMAL);
            }
        }
        return resultModel;
    }
}