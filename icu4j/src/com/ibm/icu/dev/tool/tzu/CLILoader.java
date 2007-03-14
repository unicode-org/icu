/*
 * ******************************************************************************
 * Copyright (C) 2007, International Business Machines Corporation and others.
 * All Rights Reserved.
 * ******************************************************************************
 */
package com.ibm.icu.dev.tool.tzu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Loads the ICUTZU tool, command-line version.
 */
public class CLILoader {
    /**
     * Entry point for the command-line version of the tool.
     * 
     * @param curDir
     *            The base directory of the tool.
     * @param backupDir
     *            The location to store backups.
     * @param pathFile
     *            The file to load paths from.
     * @param resultFile
     *            The file to load/save results to/from.
     * @param tzFile
     *            The local timezone resource file.
     */
    public CLILoader(File curDir, File backupDir, File pathFile,
            File resultFile, File tzFile) {
        // create the logger based on the silentpatch option
        try {
            this.logger = Logger
                    .getInstance(Logger.DEFAULT_FILENAME,
                            "true".equalsIgnoreCase(System
                                    .getProperty("silentpatch")) ? Logger.QUIET
                                    : Logger.NORMAL);
        } catch (FileNotFoundException ex) {
            System.out.println("Could not open " + Logger.DEFAULT_FILENAME
                    + " for writing.");
            System.exit(-1);
        }

        this.pathFile = pathFile;
        this.resultFile = resultFile;
        this.tzFile = tzFile;
        this.backupDir = backupDir;

        // if discoveryonly is enabled, call the search method
        // otherwise, call the update method
        try {
            if ("true".equalsIgnoreCase(System.getProperty("discoveronly"))) {
                search();
            } else {
                update();
            }
        } catch (IllegalArgumentException ex) {
            logger.errorln(ex.getMessage());
        } catch (IOException ex) {
            logger.errorln(ex.getMessage());
        } catch (InterruptedException ex) {
            logger.errorln(ex.getMessage());
        }
    }

    /**
     * Discover Only Mode. Load the path list from the path file and save the
     * path of each updatable ICU jar files it finds to the result list
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     */
    private void search() throws IOException, IllegalArgumentException,
            InterruptedException {
        logger.printlnToScreen("");
        logger.printlnToScreen("*********** Command-Line \'Discover Only\'"
                + " Mode Started ***********");
        logger.printlnToScreen("");
        logger.printlnToScreen("\'Discover Only\' Mode:");
        logger.printlnToScreen("\tIn this mode, "
                + "the tool will search for ICU4J jars"
                + " in the directories specified in DirectorySearch.txt"
                + " and print the ICU4J jars detected and their respective"
                + " time zone version to the file ICUList.txt");
        logger.printlnToScreen("");

        // initialize the result model and the path model
        resultModel = new ResultModel(logger, resultFile);
        pathModel = new PathModel(logger, pathFile);

        // load paths stored in PathModel.PATHLIST_FILENAME
        pathModel.loadPaths();

        // perform the search, putting the the results in the result model,
        // searching all subdirectories of the included path, using the backup
        // directory specified, and without using a status bar (since this is
        // command-line)
        pathModel.searchAll(resultModel, true, curDir, backupDir);

        // save the results in PathModel.RESULTLIST_FILENAME
        resultModel.saveResults();

        logger.printlnToScreen("");
        logger.printlnToScreen("Please run with DISCOVERYONLY=false"
                + " in order to update ICU4J jars listed in ICUList.txt");
        logger.printlnToScreen("*********** Command-Line \'Discover Only\'"
                + " Mode Ended ***********");
        logger.printlnToScreen("");
    }

    /**
     * Patch Mode. Load all the results from resultFile, populate the tz version
     * list via the web, and update all the results with the best available Time
     * Zone data.
     * 
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws InterruptedException
     */
    private void update() throws IOException, IllegalArgumentException,
            InterruptedException {
        logger.printlnToScreen("");
        logger.printlnToScreen("*********** Command-Line \'Patch\'"
                + " Mode Started ***********");
        logger.printlnToScreen("");
        logger.printlnToScreen("\'Patch\' Mode:");
        logger.printlnToScreen("\tIn this mode, the tool patches each of the"
                + " ICU4J jars listed in ICUList.txt with the new time zone"
                + " information.");
        logger.printlnToScreen("");

        // initialize the result model and the source model
        resultModel = new ResultModel(logger, resultFile);
        sourceModel = new SourceModel(logger, tzFile);

        // load the results from the result list file
        resultModel.loadResults();

        // populate the list of tz versions
        sourceModel.findSources();

        // perform the updates using the best tz version available
        resultModel.updateAll(
                sourceModel.getURL(sourceModel.getSelectedItem()), backupDir);

        logger.printlnToScreen("");
        logger.printlnToScreen("*********** Command-Line \'Patch\'"
                + " Mode Ended ***********");
        logger.printlnToScreen("");
    }

    /**
     * A glorified list of IncludePath objects representing the list of
     * directories used in discovery mode.
     */
    private PathModel pathModel;

    /**
     * A glorified list of ICUFile objects representing the ICU4J jars found in
     * discovery mode and used in patch mode.
     */
    private ResultModel resultModel;

    /**
     * A glorified map of Strings to URLs populated by parsing the directory
     * structure in the ICU Time Zone repository online and used in patch mode.
     */
    private SourceModel sourceModel;

    /**
     * The file that stores the path list.
     */
    private File pathFile = null;

    /**
     * The file that stores the result list.
     */
    private File resultFile = null;

    /**
     * The local timezone resource file.
     */
    private File tzFile = null;

    /**
     * The backup directory to use in patch mode, or null if there should be no
     * backups.
     */
    private File backupDir = null;

    /**
     * The current directory.
     */
    private File curDir = null;

    /**
     * A logger that manages both output to the screen and to the log file.
     */
    private Logger logger;

    /**
     * Old code... removed for a variety of reasons, but most importantly
     * because DISCOVERY_ONLY must be implemented correctly and there were
     * conflicts with the old argument structure.
     * 
     * <pre>
     * public CLILoader(String[] args) {
     *     try { // set some options to be
     *         // true based on environment variables
     *         if (&quot;true&quot;.equals(System.getProperty(&quot;discoveronly&quot;)))
     *             options[DISCOVERONLY].doesOccur = true;
     *         if (&quot;true&quot;.equals(System.getProperty(&quot;silentpatch&quot;)))
     *             options[QUIET].doesOccur = true; // parse the arguments using
     *         // UOption.parseArgs
     *         int argsleft = UOption.parseArgs(args, options); // if
     *         // help is specified, show the help specs and do nothing else
     *         if (options[HELP].doesOccur) {
     *             showHelp();
     *             return;
     *         }
     *         try { // init the
     *             // logger
     *             logger = Logger.getInstance(Logger.DEFAULT_FILENAME,
     *                     options[QUIET].doesOccur ? Logger.QUIET
     *                             : options[VERBOSE].doesOccur ? Logger.VERBOSE
     *                                     : Logger.NORMAL);
     *         } catch (FileNotFoundException ex) {
     *             System.out.println(&quot;Could not open &quot; + Logger.DEFAULT_FILENAME
     *                     + &quot; for writing.&quot;);
     *             System.exit(-1);
     *         }
     *         // create the resultModel, the pathModel, and the sourceModel
     *         resultModel = new ResultModel(logger);
     *         pathModel = new PathModel(resultModel, logger);
     *         sourceModel = new SourceModel(logger);
     *         // make sure the result model hides unreadable/unwritable files
     *         resultModel.setHidden(false);
     *         // make sure only there is only one update mode in the options
     *         int choiceType = (options[OFFLINE].doesOccur ? 1 : 0)
     *                 + (options[TZVERSION].doesOccur ? 1 : 0)
     *                 + (options[BEST].doesOccur ? 1 : 0)
     *                 + (options[DISCOVERONLY].doesOccur ? 1 : 0);
     *         if (choiceType &gt; 1)
     *             syntaxError(&quot;Options -o (--offline), -t (--tzversion), -b (--best) and -d (--discoveronly) are mutually exclusive&quot;);
     *         // make sure that quiet &amp; verbose do not both occur
     *         if (options[QUIET].doesOccur &amp;&amp; options[VERBOSE].doesOccur)
     *             syntaxError(&quot;Options -q (--quiet) and -v (--verbose) are mutually exclusive&quot;);
     *         // make sure that exactly one of backup and nobackup occur
     *         if (options[BACKUP].doesOccur &amp;&amp; options[NOBACKUP].doesOccur)
     *             syntaxError(&quot;Options -b (--backup) and -B (--nobackup) are mutually exclusive&quot;);
     *         if (!options[BACKUP].doesOccur &amp;&amp; !options[NOBACKUP].doesOccur)
     *             syntaxError(&quot;One of the options -b (--backup) or -B (--nobackup) must occur&quot;);
     *         if (argsleft != 0)
     *             syntaxError(&quot;Too many arguments&quot;);
     *         // error // quiet implies auto
     *         if (options[QUIET].doesOccur)
     *             options[AUTO].doesOccur = true;
     *         // discoveronly implies auto
     *         if (options[DISCOVERONLY].doesOccur)
     *             options[AUTO].doesOccur = true;
     *         // auto implies best if no preference specified
     *         if (options[AUTO].doesOccur &amp;&amp; choiceType == 0) {
     *             options[BEST].doesOccur = true;
     *             choiceType = 1;
     *         }
     *         // get the backup dir from the options
     * 
     *         if (options[BACKUP].doesOccur)
     *             backupDir = new File(options[BACKUP].value);
     *         // if we're running offline and the local file doesnt exist, we
     *         // can't update anything
     * 
     *         if (options[OFFLINE].doesOccur &amp;&amp; !SourceModel.TZ_LOCAL_FILE.exists()
     *                 &amp;&amp; !options[DISCOVERONLY].doesOccur)
     *             throw new IllegalArgumentException(
     *                     &quot;Running offline mode but local file does not exist (no sources available)&quot;);
     *         // if the user did not specify to stay offline, go online and find
     *         // zoneinfo.res files if (!options[OFFLINE].doesOccur)
     *         sourceModel.findSources();
     *         // load paths stored in the directory search file
     *         try {
     *             pathModel.loadPaths();
     *         } catch (IOException ex) {
     *             //
     *         }
     * 
     *         // search the paths for updatable icu4j // files
     *         try {
     *             logger.println(&quot;Search started.&quot;, Logger.NORMAL);
     *             pathModel.searchAll(options[RECURSE].doesOccur, backupDir, null);
     *             logger.println(&quot;Search done.&quot;, Logger.NORMAL);
     *         } catch (InterruptedException ex) {
     *             logger.println(&quot;Search interrupted.&quot;, Logger.NORMAL);
     *         }
     *         // get the name and url associated with the update mode (or null if
     *         // unspecified)
     *         String chosenName = null;
     *         String chosenVersion = null;
     *         URL chosenURL = null;
     *         if (options[BEST].doesOccur) {
     *             chosenName = getBestName();
     *             chosenVersion = getBestVersion();
     *             chosenURL = getBestURL();
     *         } else if (options[OFFLINE].doesOccur) {
     *             chosenName = getLocalName();
     *             chosenVersion = getLocalVersion();
     *             chosenURL = getLocalURL();
     *         } else if (options[TZVERSION].doesOccur) {
     *             chosenName = getTZVersionName(options[TZVERSION].value);
     *             chosenVersion = getTZVersionVersion(options[TZVERSION].value);
     *             chosenURL = getTZVersionURL(options[TZVERSION].value);
     *         }
     *         // (do nothing in the case of DISCOVERONLY)
     *         // create a reader for user input
     *         BufferedReader reader = new BufferedReader(new InputStreamReader(
     *                 System.in));
     *         // iterate through each icu4j file in the search results
     *         Iterator resultIter = resultModel.iterator();
     *         while (resultIter.hasNext()) {
     *             try {
     *                 ICUFile entry = (ICUFile) resultIter.next();
     *                 logger.println(&quot;&quot;, Logger.NORMAL);
     *                 logger.println(&quot;Filename: &quot; + entry.getFile().getName(),
     *                         Logger.NORMAL);
     *                 logger.println(&quot;Location: &quot; + entry.getFile().getParent(),
     *                         Logger.NORMAL);
     *                 logger.println(&quot;Current Version: &quot; + entry.getTZVersion(),
     *                         Logger.NORMAL);
     * 
     *                 if (!entry.getFile().canRead() || !entry.getFile().canWrite()) {
     *                     logger.println(&quot;Missing permissions for &quot;
     *                             + entry.getFile().getName() + &quot;.&quot;, Logger.NORMAL);
     *                     continue;
     *                 }
     * 
     *                 if (options[AUTO].doesOccur) // automatic mode
     *                 {
     *                     if (!options[DISCOVERONLY].doesOccur)
     *                         update(entry, chosenName, chosenURL);
     *                 } else if (choiceType == 1) // confirmation mode
     *                 {
     *                     String input = askConfirm(chosenName, chosenVersion, entry
     *                             .getTZVersion(), reader);
     * 
     *                     if (&quot;yes&quot;.startsWith(input))
     *                         update(entry, chosenName, chosenURL);
     *                     else
     *                         skipUpdate();
     *                 } else {
     *                     String input = askChoice(reader);
     * 
     *                     if (&quot;best&quot;.startsWith(input))
     *                         update(entry, getBestName(), getBestURL());
     *                     else if (&quot;local choice&quot;.startsWith(input))
     *                         update(entry, getLocalName(), getLocalURL());
     *                     else if (!&quot;none&quot;.startsWith(input))
     *                         update(entry, getTZVersionName(input),
     *                                 getTZVersionURL(input));
     *                     else
     *                         skipUpdate();
     *                 }
     *             } catch (IOException ex) {
     *                 // error in command-line input ???
     *                 logger.errorln(&quot;Error in command-line input.&quot;);
     *             }
     *         }
     * 
     *         logger.println(&quot;&quot;, Logger.NORMAL);
     *         logger.println(&quot;ICUTZU finished successfully.&quot;, Logger.NORMAL);
     *     } catch (IllegalArgumentException ex) {
     *         logger.errorln(ex.getMessage());
     *         return;
     *     }
     * }
     * 
     * private String askConfirm(String chosenString, String chosenVersion,
     *         String currentVersion, BufferedReader reader) throws IOException {
     *     int betterness = chosenVersion.compareToIgnoreCase(currentVersion);
     *     if (betterness == 0) {
     *         logger.println(&quot;Updating should have no effect on this file.&quot;,
     *                 Logger.NORMAL);
     *         logger.println(&quot;Update anyway?&quot;, Logger.NORMAL);
     *     } else if (betterness &lt; 0) {
     *         logger
     *                 .println(
     *                         &quot;Warning: The version specified is older than the one present in the file.&quot;,
     *                         Logger.NORMAL);
     *         logger.println(&quot;Update anyway?&quot;, Logger.NORMAL);
     *     } else {
     *         logger.println(&quot;Update to &quot; + chosenVersion + &quot;?&quot;, Logger.NORMAL);
     *     }
     * 
     *     logger.println(&quot; [yes (default), no]\n: &quot;, Logger.NORMAL);
     *     return reader.readLine().trim().toLowerCase();
     * }
     * 
     * private String askChoice(BufferedReader reader) throws IOException {
     *     logger.println(&quot;Available Versions: &quot;, Logger.NORMAL);
     *     Iterator sourceIter = sourceModel.iterator();
     * 
     *     logger.println(getLocalName(), Logger.NORMAL);
     *     while (sourceIter.hasNext())
     *         logger.println(&quot;, &quot; + ((Map.Entry) sourceIter.next()).getKey(),
     *                 Logger.NORMAL);
     *     logger.println(&quot;&quot;, Logger.NORMAL);
     * 
     *     logger
     *             .println(
     *                     &quot;Update to which version? [best (default), none, local copy, &lt;specific version above&gt;]&quot;,
     *                     Logger.NORMAL);
     *     logger.println(&quot;: &quot;, Logger.NORMAL);
     *     return reader.readLine().trim().toLowerCase();
     * }
     * 
     * private void update(ICUFile entry, String chosenString, URL url) {
     *     logger.println(&quot;Updating to &quot; + chosenString + &quot;...&quot;, Logger.NORMAL);
     *     try {
     *         entry.updateJar(url, backupDir);
     *         logger.println(&quot;Update done.&quot;, Logger.NORMAL);
     *     } catch (IOException ex) {
     *         logger.error(&quot;Could not update &quot; + entry.getFile().getName());
     *     }
     * }
     * 
     * private void skipUpdate() {
     *     logger.println(&quot;Update skipped.&quot;, Logger.NORMAL);
     * }
     * 
     * private String getBestName() {
     *     return (String) sourceModel.getSelectedItem();
     * }
     * 
     * private String getLocalName() {
     *     return SourceModel.TZ_LOCAL_CHOICE;
     * }
     * 
     * private String getTZVersionName(String version) {
     *     return version.trim().toLowerCase();
     * }
     * 
     * private String getBestVersion() {
     *     return sourceModel.getVersion(sourceModel.getSelectedItem());
     * }
     * 
     * private String getLocalVersion() {
     *     return SourceModel.TZ_LOCAL_VERSION;
     * }
     * 
     * private String getTZVersionVersion(String version) {
     *     return version.trim().toLowerCase();
     * }
     * 
     * private URL getBestURL() {
     *     return sourceModel.getURL(sourceModel.getSelectedItem());
     * }
     * 
     * private URL getLocalURL() {
     *     return SourceModel.TZ_LOCAL_URL;
     * }
     * 
     * private URL getTZVersionURL(String version) {
     *     try {
     *         return new URL(SourceModel.TZ_BASE_URLSTRING_START + version
     *                 + SourceModel.TZ_BASE_URLSTRING_END);
     *     } catch (MalformedURLException ex) {
     *         ex.printStackTrace();
     *         return null;
     *     }
     * }
     * 
     * private void showHelp() {
     *     logger.println(&quot;Help!&quot;, Logger.NORMAL);
     * }
     * 
     * private void syntaxError(String message) {
     *     throw new IllegalArgumentException(&quot;Error in argument list: &quot; + message);
     * }
     * 
     * private static UOption options[] = new UOption[] {
     *         UOption.create(&quot;help&quot;, '?', UOption.NO_ARG),
     *         UOption.create(&quot;verbose&quot;, 'v', UOption.NO_ARG),
     *         UOption.create(&quot;quiet&quot;, 'q', UOption.NO_ARG),
     *         UOption.create(&quot;auto&quot;, 'a', UOption.NO_ARG),
     *         UOption.create(&quot;offline&quot;, 'o', UOption.NO_ARG),
     *         UOption.create(&quot;best&quot;, 's', UOption.NO_ARG),
     *         UOption.create(&quot;tzversion&quot;, 't', UOption.REQUIRES_ARG),
     *         UOption.create(&quot;recurse&quot;, 'r', UOption.NO_ARG),
     *         UOption.create(&quot;backup&quot;, 'b', UOption.REQUIRES_ARG),
     *         UOption.create(&quot;nobackup&quot;, 'B', UOption.NO_ARG),
     *         UOption.create(&quot;discoveronly&quot;, 'd', UOption.NO_ARG), };
     * 
     * private static final int HELP = 0;
     * 
     * private static final int VERBOSE = 1;
     * 
     * private static final int QUIET = 2;
     * 
     * private static final int AUTO = 3;
     * 
     * private static final int OFFLINE = 4;
     * 
     * private static final int BEST = 5;
     * 
     * private static final int TZVERSION = 6;
     * 
     * private static final int RECURSE = 7;
     * 
     * private static final int BACKUP = 8;
     * 
     * private static final int NOBACKUP = 9;
     * 
     * private static final int DISCOVERONLY = 10;
     * </pre>
     */
}
