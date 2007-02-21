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

    public static void search(ResultModel resultModel, IncludePath[] paths,
            boolean subdirs, File backupDir) throws InterruptedException {
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

        System.out.println("*************");
        System.out.println("Included:");
        for (int i = 0; i < included.size(); i++)
            System.out.println(included.get(i));
        System.out.println("Excluded:");
        for (int i = 0; i < excluded.size(); i++)
            System.out.println(excluded.get(i));
        System.out.println("*************");

        for (int i = 0; i < included.size(); i++)
            search(resultModel, (File) included.get(i), excluded, subdirs, true);

    }

    private static void search(ResultModel resultModel, File file,
            List excluded, boolean subdirs, boolean firstdip)
            throws InterruptedException {
        Iterator iter = excluded.iterator();
        while (iter.hasNext())
            if (file.getAbsolutePath().equalsIgnoreCase(
                    ((File) iter.next()).getAbsolutePath()))
                return;

        if (file.exists()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                try {
                    resultModel.add(new ICUFile(file));
                } catch (IOException ex) {
                    // if it's not an ICU file we care about, ignore it
                }
            } else if (file.isDirectory() && (subdirs || firstdip)) {
                System.out.println(file);
                File[] dirlist = file.listFiles();
                if (dirlist != null)
                    for (int i = 0; i < dirlist.length; i++)
                        search(resultModel, dirlist[i], excluded, subdirs,
                                false);
                Thread.sleep(0);
            }
        }
    }

}