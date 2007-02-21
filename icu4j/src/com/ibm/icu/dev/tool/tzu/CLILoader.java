/**
 *******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import com.ibm.icu.dev.tool.UOption;
import java.util.*;
import java.io.*;
import java.net.*;

public class CLILoader {
    public static void main(String[] args) {
        new CLILoader(args);
    }

    public CLILoader(String[] args) {
        try {
            resultModel.setHidden(false);

            boolean nogui = "true".equals(System.getProperty("nogui"));
            boolean discoveronly = "true".equals(System
                    .getProperty("discoveronly"));
            boolean silentpatch = "true".equals(System
                    .getProperty("silentpatch"));

            System.out.println("nogui=" + nogui);
            System.out.println("discoveryonly=" + discoveronly);
            System.out.println("silentpatch=" + silentpatch);

            int argsleft = UOption.parseArgs(args, options);

            if (options[HELP].doesOccur) {
                showHelp();
                return;
            }

            int choiceType = (options[OFFLINE].doesOccur ? 1 : 0)
                    + (options[TZVERSION].doesOccur ? 1 : 0)
                    + (options[BEST].doesOccur ? 1 : 0);
            if (choiceType > 1)
                syntaxError("Options -o (--offline), -t (--tzversion), and -b (--best) are mutually exclusive");// error
            if (options[QUIET].doesOccur && options[VERBOSE].doesOccur)
                syntaxError("Options -q (--quiet) and -v (--verbose) are mutually exclusive");// error
            if (options[BACKUP].doesOccur && options[NOBACKUP].doesOccur)
                syntaxError("Options -b (--backup) and -B (--nobackup) are mutually exclusive");// error
            if (!options[BACKUP].doesOccur && !options[NOBACKUP].doesOccur)
                syntaxError("One of the options -b (--backup) or -B (--nobackup) must occur");// error
            if (argsleft != 0)
                syntaxError("Too many arguments");// error

            if (options[QUIET].doesOccur) // quiet implies auto
                options[AUTO].doesOccur = true;
            if (options[AUTO].doesOccur && choiceType == 0) // auto implies best
                                                            // by default
            {
                options[BEST].doesOccur = true;
                choiceType = 1;
            }

            if (options[BACKUP].doesOccur)
                backupDir = new File(options[BACKUP].value);
            if (!options[OFFLINE].doesOccur)
                sourceModel.findSources();

            pathModel.loadPaths();

            try {
                System.out.println("Search started.");
                pathModel.searchAll(options[RECURSE].doesOccur, backupDir);
                System.out.println("Search done.");
            } catch (InterruptedException ex) {
                System.out.println("Search interrupted.");
            }

            String chosenString = (options[BEST].doesOccur) ? getBestString()
                    : (options[OFFLINE].doesOccur) ? getLocalString()
                            : (options[TZVERSION].doesOccur) ? getTZVersionString(options[TZVERSION].value)
                                    : null;

            URL chosenURL = (options[BEST].doesOccur) ? getBestURL()
                    : (options[OFFLINE].doesOccur) ? getLocalURL()
                            : (options[TZVERSION].doesOccur) ? getTZVersionURL(options[TZVERSION].value)
                                    : null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    System.in));
            Iterator resultIter = resultModel.iterator();

            while (resultIter.hasNext()) {
                try {
                    ICUFile entry = (ICUFile) resultIter.next();
                    System.out.println();
                    System.out.println("Filename:  "
                            + entry.getFile().getName());
                    System.out.println("Location:  "
                            + entry.getFile().getParent());
                    System.out.println("Current Version: "
                            + entry.getTZVersion());

                    if (!entry.getFile().canRead()
                            || !entry.getFile().canWrite()) {
                        System.out.println("Missing permissions for "
                                + entry.getFile().getName() + ".");
                        continue;
                    }

                    if (options[AUTO].doesOccur) // automatic mode
                    {
                        update(entry, chosenString, chosenURL);
                    } else if (choiceType == 1) // confirmation mode
                    {
                        String input = askConfirm(chosenString, reader);

                        if ("yes".startsWith(input))
                            update(entry, chosenString, chosenURL);
                        else
                            skipUpdate();
                    } else // interactive mode
                    {
                        String input = askChoice(reader);

                        if ("best".startsWith(input))
                            update(entry, getBestString(), getBestURL());
                        else if (!"local choice".startsWith(input))
                            update(entry, getLocalString(), getLocalURL());
                        else if (!"none".startsWith(input))
                            update(entry, getTZVersionString(input),
                                    getTZVersionURL(input));
                        else
                            skipUpdate();
                    }
                } catch (IOException ex) {
                    // error in command-line input
                    ex.printStackTrace();
                }
            }

            System.out.println();
            System.out.println("ICUTZU finished successfully.");
        } catch (IllegalArgumentException ex) {
            System.out.println(ex);
            return;
        }
    }

    private String askConfirm(String chosenString, BufferedReader reader)
            throws IOException {
        System.out.println("Update to " + chosenString
                + "? [yes (default), no]");
        System.out.print(": ");
        return reader.readLine().trim().toLowerCase();
    }

    private String askChoice(BufferedReader reader) throws IOException {
        System.out.print("Available Versions: ");
        Iterator sourceIter = sourceModel.iterator();

        if (sourceIter.hasNext())
            System.out.print(((Map.Entry) sourceIter.next()).getKey());
        while (sourceIter.hasNext())
            System.out.print(", " + ((Map.Entry) sourceIter.next()).getKey());

        System.out
                .println("Update Version? [best (default), <specific version>, local copy, none]");
        System.out.print(": ");
        return reader.readLine().trim().toLowerCase();
    }

    private void update(ICUFile entry, String chosenString, URL url) {
        System.out.println("Updating to " + chosenString + "...");
        try {
            entry.updateJar(url, backupDir);
            System.out.println("Update done.");
        } catch (IOException ex) {
            System.out.println(ex);
        }
    }

    private void skipUpdate() {
        System.out.println("Update skipped.");
    }

    private String getBestString() {
        return (String) sourceModel.getSelectedItem();
    }

    private String getLocalString() {
        return SourceModel.TZ_LOCAL_CHOICE;
    }

    private String getTZVersionString(String version) {
        return version;
    }

    private URL getBestURL() {
        return sourceModel.getValue(sourceModel.getSelectedItem());
    }

    private URL getLocalURL() {
        return SourceModel.TZ_LOCAL_URL;
    }

    private URL getTZVersionURL(String version) {
        try {
            return new URL(SourceModel.TZ_BASE_URLSTRING_START + version
                    + SourceModel.TZ_BASE_URLSTRING_END);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static void showHelp() {
        System.out.println("Help!");
    }

    private static void syntaxError(String message) {
        throw new IllegalArgumentException("Error in argument list: " + message);
    }

    private ResultModel resultModel = new ResultModel();

    private PathModel pathModel = new PathModel(resultModel);

    private SourceModel sourceModel = new SourceModel();

    private File backupDir = null;

    // { pathModel.add(new ICUPath(new File("C:\\Documents and Settings\\Daniel
    // Kesserich\\Desktop\\Spring 2007\\IBM\\updatehere"), true)); }

    private static UOption options[] = new UOption[] {
            UOption.create("help", '?', UOption.NO_ARG),
            UOption.create("verbose", 'v', UOption.NO_ARG),
            UOption.create("quiet", 'q', UOption.NO_ARG),
            UOption.create("auto", 'a', UOption.NO_ARG),
            UOption.create("offline", 'o', UOption.NO_ARG),
            UOption.create("best", 'b', UOption.NO_ARG),
            UOption.create("tzversion", 't', UOption.REQUIRES_ARG),
            UOption.create("recurse", 'r', UOption.NO_ARG),
            UOption.create("backup", 'b', UOption.REQUIRES_ARG),
            UOption.create("nobackup", 'B', UOption.NO_ARG),
            UOption.create("discoveronly", 'd', UOption.NO_ARG), };

    private static final int HELP = 0;

    private static final int VERBOSE = 1;

    private static final int QUIET = 2;

    private static final int AUTO = 3;

    private static final int OFFLINE = 4;

    private static final int BEST = 5;

    private static final int TZVERSION = 6;

    private static final int RECURSE = 7;

    private static final int BACKUP = 8;

    private static final int NOBACKUP = 9;

    private static final int DISCOVERONLY = 10;
}