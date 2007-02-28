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

            // init the logger
            logger = Logger.initLogger(Logger.DEFAULT_FILENAME,
                    options[QUIET].doesOccur ? Logger.QUIET
                            : options[VERBOSE].doesOccur ? Logger.VERBOSE
                                    : Logger.NORMAL);

            // create the resultModel, the pathModel, and the sourceModel
            resultModel = new ResultModel(logger);
            pathModel = new PathModel(resultModel, logger);
            sourceModel = new SourceModel(logger);

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

            // discoveronly implies auto
            if (options[DISCOVERONLY].doesOccur)
                options[AUTO].doesOccur = true;

            // auto implies best if no preference specified
            if (options[AUTO].doesOccur && choiceType == 0) {
                options[BEST].doesOccur = true;
                choiceType = 1;
            }

            // get the backup dir from the options
            if (options[BACKUP].doesOccur)
                backupDir = new File(options[BACKUP].value);

            // if we're running offline and the local file doesnt exist, we
            // can't update squat
            if (options[OFFLINE].doesOccur
                    && !SourceModel.TZ_LOCAL_FILE.exists()
                    && !options[DISCOVERONLY].doesOccur)
                throw new IllegalArgumentException(
                        "Running offline mode but local file does not exist (no sources available)");

            // if the user did not specify to stay offline, go online and find
            // zoneinfo.res files
            if (!options[OFFLINE].doesOccur)
                sourceModel.findSources();

            // load paths stored in the directory search file
            pathModel.loadPaths();

            // search the paths for updatable icu4j files
            try {
                logger.println("Search started.", Logger.NORMAL);
                pathModel.searchAll(options[RECURSE].doesOccur, backupDir);
                logger.println("Search done.", Logger.NORMAL);
            } catch (InterruptedException ex) {
                logger.println("Search interrupted.", Logger.NORMAL);
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
            // (do nothing in the case of DISCOVERONLY)

            // create a reader for user input
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    System.in));

            // iterate through each icu4j file in the search results
            Iterator resultIter = resultModel.iterator();
            while (resultIter.hasNext()) {
                try {
                    ICUFile entry = (ICUFile) resultIter.next();
                    logger.println("", Logger.NORMAL);
                    logger.println("Filename:  " + entry.getFile().getName(),
                            Logger.NORMAL);
                    logger.println("Location:  " + entry.getFile().getParent(),
                            Logger.NORMAL);
                    logger.println("Current Version: " + entry.getTZVersion(),
                            Logger.NORMAL);

                    if (!entry.getFile().canRead()
                            || !entry.getFile().canWrite()) {
                        logger.println("Missing permissions for "
                                + entry.getFile().getName() + ".",
                                Logger.NORMAL);
                        continue;
                    }

                    if (options[AUTO].doesOccur) // automatic mode
                    {
                        if (!options[DISCOVERONLY].doesOccur)
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
                    // error in command-line input ???
                    logger.errorln("Error in command-line input.");
                }
            }

            logger.println("", Logger.NORMAL);
            logger.println("ICUTZU finished successfully.", Logger.NORMAL);
        } catch (IllegalArgumentException ex) {
            logger.errorln(ex.getMessage());
            return;
        }
    }

    private String askConfirm(String chosenString, String chosenVersion,
            String currentVersion, BufferedReader reader) throws IOException {
        int betterness = chosenVersion.compareToIgnoreCase(currentVersion);
        if (betterness == 0) {
            logger.println("Updating should have no effect on this file.",
                    Logger.NORMAL);
            logger.println("Update anyway?", Logger.NORMAL);
        } else if (betterness < 0) {
            logger
                    .println(
                            "Warning: The version specified is older than the one present in the file.",
                            Logger.NORMAL);
            logger.println("Update anyway?", Logger.NORMAL);
        } else {
            logger.println("Update to " + chosenVersion + "?", Logger.NORMAL);
        }

        logger.println(" [yes (default), no]\n: ", Logger.NORMAL);
        return reader.readLine().trim().toLowerCase();
    }

    private String askChoice(BufferedReader reader) throws IOException {
        logger.println("Available Versions: ", Logger.NORMAL);
        Iterator sourceIter = sourceModel.iterator();

        logger.println(getLocalName(), Logger.NORMAL);
        while (sourceIter.hasNext())
            logger.println(", " + ((Map.Entry) sourceIter.next()).getKey(),
                    Logger.NORMAL);
        logger.println("", Logger.NORMAL);

        logger
                .println(
                        "Update to which version? [best (default), none, local copy, <specific version above>]",
                        Logger.NORMAL);
        logger.println(": ", Logger.NORMAL);
        return reader.readLine().trim().toLowerCase();
    }

    private void update(ICUFile entry, String chosenString, URL url) {
        logger.println("Updating to " + chosenString + "...", Logger.NORMAL);
        try {
            entry.updateJar(url, backupDir);
            logger.println("Update done.", Logger.NORMAL);
        } catch (IOException ex) {
            logger.error("Could not update " + entry.getFile().getName());
        }
    }

    private void skipUpdate() {
        logger.println("Update skipped.", Logger.NORMAL);
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

    private void showHelp() {
        logger.println("Help!", Logger.NORMAL);
    }

    private void syntaxError(String message) {
        throw new IllegalArgumentException("Error in argument list: " + message);
    }

    private ResultModel resultModel;

    private PathModel pathModel;

    private SourceModel sourceModel;

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

    private Logger logger;
}