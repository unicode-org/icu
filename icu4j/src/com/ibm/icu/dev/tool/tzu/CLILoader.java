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
            // make sure the result model hides unreadable/unwritable files
            resultModel.setHidden(false);

            // set some options to be true based on environment variables
            if ("true".equals(System.getProperty("discoveronly")))
                options[DISCOVERONLY].doesOccur = true;
            if ("true".equals(System.getProperty("silentpatch")))
                options[QUIET].doesOccur = true;

            // parse the arguments using UOption.parseArgs
            int argsleft = UOption.parseArgs(args, options);

            // if help is specified, show the help specs and do nothing else
            if (options[HELP].doesOccur) {
                showHelp();
                return;
            }

            // make sure only there is only one update mode in the options
            int choiceType = (options[OFFLINE].doesOccur ? 1 : 0)
                    + (options[TZVERSION].doesOccur ? 1 : 0)
                    + (options[BEST].doesOccur ? 1 : 0)
                    + (options[DISCOVERONLY].doesOccur ? 1 : 0);
            if (choiceType > 1)
                syntaxError("Options -o (--offline), -t (--tzversion), -b (--best) and -d (--discoveronly) are mutually exclusive");// error

            // make sure that quiet & verbose do not both occur
            if (options[QUIET].doesOccur && options[VERBOSE].doesOccur)
                syntaxError("Options -q (--quiet) and -v (--verbose) are mutually exclusive");// error

            // make sure that exactly one of backup & nobackup occurs
            if (options[BACKUP].doesOccur && options[NOBACKUP].doesOccur)
                syntaxError("Options -b (--backup) and -B (--nobackup) are mutually exclusive");// error
            if (!options[BACKUP].doesOccur && !options[NOBACKUP].doesOccur)
                syntaxError("One of the options -b (--backup) or -B (--nobackup) must occur");// error
            if (argsleft != 0)
                syntaxError("Too many arguments");// error

            // quiet implies auto
            if (options[QUIET].doesOccur)
                options[AUTO].doesOccur = true;

            // auto implies best if no preference specified
            if (options[AUTO].doesOccur && choiceType == 0) {
                options[BEST].doesOccur = true;
                choiceType = 1;
            }

            // get the backup dir from the options
            if (options[BACKUP].doesOccur)
                backupDir = new File(options[BACKUP].value);

            // if the user did not specify to stay offline, go online and find
            // zoneinfo.res files
            if (!options[OFFLINE].doesOccur)
                sourceModel.findSources();

            // load paths from the directory search file
            pathModel.loadPaths();

            // search the paths for updatable icu4j files
            try {
                System.out.println("Search started.");
                pathModel.searchAll(options[RECURSE].doesOccur, backupDir);
                System.out.println("Search done.");
            } catch (InterruptedException ex) {
                System.out.println("Search interrupted.");
            }

            // get the name and url associated with the update mode (or null if
            // unspecified)
            String chosenName = null;
            String chosenVersion = null;
            URL chosenURL = null;
            if (options[BEST].doesOccur) {
                chosenName = getBestName();
                chosenVersion = getBestVersion();
                chosenURL = getBestURL();
            } else if (options[OFFLINE].doesOccur) {
                chosenName = getLocalName();
                chosenVersion = getLocalVersion();
                chosenURL = getLocalURL();
            } else if (options[TZVERSION].doesOccur) {
                chosenName = getTZVersionName(options[TZVERSION].value);
                chosenVersion = getTZVersionVersion(options[TZVERSION].value);
                chosenURL = getTZVersionURL(options[TZVERSION].value);
            }

            // create a reader for user input
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    System.in));

            // iterate through each icu4j file in the search results
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
                        update(entry, chosenName, chosenURL);
                    } else if (choiceType == 1) // confirmation mode
                    {
                        String input = askConfirm(chosenName, chosenVersion,
                                entry.getTZVersion(), reader);

                        if ("yes".startsWith(input))
                            update(entry, chosenName, chosenURL);
                        else
                            skipUpdate();
                    } else // interactive mode
                    {
                        String input = askChoice(reader);

                        if ("best".startsWith(input))
                            update(entry, getBestName(), getBestURL());
                        else if ("local choice".startsWith(input))
                            update(entry, getLocalName(), getLocalURL());
                        else if (!"none".startsWith(input))
                            update(entry, getTZVersionName(input),
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

    private String askConfirm(String chosenString, String chosenVersion,
            String currentVersion, BufferedReader reader) throws IOException {
        int betterness = 1; // chosenString.compareToIgnoreCase(currentVersion);
        if (betterness == 0) {
            System.out.println("Updating should have no effect on this file.");
            System.out.print("Update anyway?");
        } else if (betterness < 0) {
            System.out
                    .println("Warning: The version specified is older than the one present in the file.");
            System.out.print("Update anyway?");
        } else {
            System.out.print("Update to " + chosenVersion + "?");
        }

        System.out.print(" [yes (default), no]\n: ");
        return reader.readLine().trim().toLowerCase();
    }

    private String askChoice(BufferedReader reader) throws IOException {
        System.out.print("Available Versions: ");
        Iterator sourceIter = sourceModel.iterator();

        if (sourceIter.hasNext())
            System.out.print(((Map.Entry) sourceIter.next()).getKey());
        while (sourceIter.hasNext())
            System.out.print(", " + ((Map.Entry) sourceIter.next()).getKey());

        System.out.println();
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
            System.err.println(ex);
        }
    }

    private void skipUpdate() {
        System.out.println("Update skipped.");
    }

    private String getBestName() {
        return (String) sourceModel.getSelectedItem();
    }

    private String getLocalName() {
        return SourceModel.TZ_LOCAL_CHOICE;
    }

    private String getTZVersionName(String version) {
        return version.trim().toLowerCase();
    }

    private String getBestVersion() {
        return sourceModel.getVersion(sourceModel.getSelectedItem());
    }

    private String getLocalVersion() {
        return SourceModel.TZ_LOCAL_VERSION;
    }

    private String getTZVersionVersion(String version) {
        return version.trim().toLowerCase();
    }

    private URL getBestURL() {
        return sourceModel.getURL(sourceModel.getSelectedItem());
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