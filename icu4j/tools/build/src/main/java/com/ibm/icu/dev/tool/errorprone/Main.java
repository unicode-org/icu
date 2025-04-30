// © 2025 and later: Unicode, Inc. and others.

package com.ibm.icu.dev.tool.errorprone;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Parses a log that represents the standard output of a maven run with erroprone enabled
 * and generates two user friendly reports in html format.
 *
 * <pre><code>
 * mvn clean test -ntp -DskipTests -DskipITs -l errorprone.log -P errorprone
 * mvn exec:java -f tools/build/ -P errorprone_report
 * </code></pre>
 */
public class Main {
    private static final String HELP = "help";
    private static final String HELP_DESC = "this text";

    private static final String SRC_BASE_URL = "srcBaseUrl";
    private static final String SRC_BASE_URL_DESC = ""
            + "The url used as a base for links to the source.\n"
            + "The report will append to it the relative path of the Java files to create links.\n"
            + "It can point to a web server, or local folder.\n"
            + "Some examples:\n"
            + "  \u00a0 \u00a0 https://github.com/unicode-org/icu/blob/main\n"
            + "  \u00a0 \u00a0 http://localhost:8000\n"
            + "  \u00a0 \u00a0 file:///Users/john/icu/\n"
            + "  \u00a0 \u00a0 ../\n"
            + "Except for GitHub these options don't honow the line where to go.\n"
            + "And GitHub might be out of sync with the report, so the offset might be wrong.";
    private static final String SRC_BASE_URL_DEFAULT = "https://github.com/unicode-org/icu/blob/main/";
    String srcBaseUrl;

    private static final String LOG_FILE_NAME = "logFile";
    private static final String LOG_FILE_NAME_DESC = "The name of the errorprone log file.\n"
            + "It is in fact the standard output of a maven build with errorprone profile enabled.";
    private static final String LOG_FILE_NAME_DEFAULT = "errorprone.log";
    String logFile;

    private static final String OUT_DIR = "outDir";
    private static final String OUT_DIR_DESC =
            "The output directory where to write the converted ICU data.";
    private static final String OUT_DIR_DEFAULT = ".";
    String outDir;

    private static final String ICU_DIR = "icuDir";
    private static final String ICU_DIR_DESC = "Path top level ICU directory\n"
                    + "(containing `.git`, `icu4c`, `icu4j`, `tools` directories)";
    private static final String ICU_DIR_DEFAULT = null;
    String icuDir;

    // These must be kept in sync with getOptions().
    private static final Options options =
            new Options()
                    .addOption(Option.builder()
                            .longOpt(HELP)
                            .desc(HELP_DESC)
                            .build())
                    .addOption(Option.builder()
                            .longOpt(SRC_BASE_URL)
                            .hasArg()
                            .argName("path")
                            .desc(descWithDefault(SRC_BASE_URL_DESC, SRC_BASE_URL_DEFAULT))
                            .build())
                    .addOption(Option.builder()
                            .longOpt(LOG_FILE_NAME)
                            .hasArg()
                            .argName("path")
                            .desc(descWithDefault(LOG_FILE_NAME_DESC, LOG_FILE_NAME_DEFAULT))
                            .build())
                    .addOption(Option.builder()
                            .longOpt(ICU_DIR)
                            .hasArg()
                            .argName("path")
                            .desc(descWithDefault(ICU_DIR_DESC, ICU_DIR_DEFAULT))
                            .build())
                    .addOption(Option.builder()
                            .longOpt(OUT_DIR)
                            .hasArg()
                            .argName("path")
                            .desc(descWithDefault(OUT_DIR_DESC, OUT_DIR_DEFAULT))
                            .build());

    private static String descWithDefault(String description, String defaultValue) {
        if (defaultValue != null) {
            return description + "\nDefaults to: \"" + defaultValue + "\"";
        } else {
            return description;
        }
    }

    private void showUsageAndExit() {
        String thisClassName = this.getClass().getCanonicalName();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                /*width*/ 120,
                /*cmdLineSyntax*/ thisClassName + " [OPTIONS]\n",
                /*header*/ "\n"
                        + "This program generates an errorprone html report from"
                        + " the maven standard output.\n"
                        + "Options:",
                options,
                /*footer*/ "\nExample: "
                        + thisClassName
                        + " --outDir /tmp/debug --logFile=errorprone.log");
        System.exit(-1);
    }

    void processArgs(String[] args) {
        CommandLine cli = null;
        try {
            CommandLineParser parser = new DefaultParser();
            cli = parser.parse(options, args);
        } catch (Exception e) {
            cli = CommandLine.builder().build();
            showUsageAndExit();
        }
        if (cli.hasOption(HELP)) {
            showUsageAndExit();
        }

        icuDir = cli.getOptionValue(ICU_DIR, icuDir);
        outDir = cli.getOptionValue(OUT_DIR, outDir);
        srcBaseUrl = cli.getOptionValue(SRC_BASE_URL, srcBaseUrl);
        logFile = cli.getOptionValue(LOG_FILE_NAME, logFile);
    }

    private static void makeOutputFolder(String dirName) {
        File fileOutDir = new File(dirName);
        if (!fileOutDir.exists()) {
            fileOutDir.mkdirs();
        } else if (!fileOutDir.isDirectory()) {
            // already exists, but it is not a directory
            System.out.println("Error: " + dirName
                    + " already exists, but it is not a directory");
            System.exit(2);
        }
    }

    private static Path guessIcuRoot() {
        try {
            Path p = Paths.get(Main.class.getResource("/").toURI());
            do {
                if (p.resolve("icu4c").toFile().isDirectory()
                        && p.resolve("icu4j").toFile().isDirectory()
                        && p.resolve("LICENSE").toFile().isFile()) {
                    return p;
                }
                p = p.getParent();
            } while (p != null);
        } catch (URISyntaxException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(2);
        }
        System.out.println("Unable to find the ICU root");
        System.exit(2);
        return null;
    }

    void execute() {
        try {
            Path icuDirr = guessIcuRoot();
            icuDir = icuDirr.toString() + "/";
            if (srcBaseUrl == null) {
                srcBaseUrl = icuDir;
            }
            if (outDir == null) {
                outDir = icuDir + "icu4j/target/";
            }
            if (logFile == null) {
                logFile = icuDir + "icu4j/errorprone.log";
            }
            makeOutputFolder(outDir);
            ErrorProneReport.genReports(icuDir, logFile, outDir, srcBaseUrl);
        } catch (IOException e) {
            System.out.println("Error: " + e);
            System.exit(1);
        }
    }

    /** Program entry point. */
    public static void main(String[] args) throws IOException {
        /*
         * mvn clean test -ntp -DskipTests -DskipITs -l errorprone.log -P errorprone
         * mvn exec:java -f tools/build/ -P errorprone_report
         */
        Main self = new Main();
        self.processArgs(args);
        self.execute();
    }
}
