// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import java.io.File;
import java.util.Arrays;
import java.util.StringJoiner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.unicode.icu.tool.cldrtoicu.LdmlConverter.OutputType;

import com.ibm.icu.util.VersionInfo;

class Cldr2IcuCliOptions {
    private static final String HELP = "help";
    private static final String HELP_DESC = "this text";

    private static final String ICU_DIR = "icuDir";
    private static final String ICU_DIR_DESC = "Path top level ICU directory"
            + " (containing `.git`, `icu4c`, `icu4j`, `tools` directories)";
    private static final String ICU_DIR_DEFAULT = "${environ.ICU_DIR}";
    String icuDir;

    private static final String CLDR_DIR = "cldrDir";
    private static final String CLDR_DIR_DESC = "This is the path to the to root of standard CLDR sources,"
            + " (containing `common` and `tools` directories).";
    private static final String CLDR_DIR_DEFAULT = "${environ.CLDR_DIR}";
    String cldrDir;

    private static final String CLDR_DATA_DIR = "cldrDataDir";
    private static final String CLDR_DATA_DIR_DESC = "The top-level directory for the CLDR production data"
            + " (typically the `production` directory in the staging repository)."
            + " Usually generated locally or obtained from https://github.com/unicode-org/cldr-staging/tree/main/production";
    private static final String CLDR_DATA_DIR_DEFAULT = "${environ.CLDR_DATA_DIR}";
    String cldrDataDir;

    private static final String OUT_DIR = "outDir";
    final private static String OUT_DIR_DESC = "The output directory into which to write the converted ICU data. By default"
            + " this will overwrite (without deletion) the ICU data files in this ICU release,"
            + " so it is recommended that for testing, it be set to another value.";
    final private static String OUT_DIR_DEFAULT = "${icuDir}/icu4c/source/data";
    String outDir;

    private static final String GEN_C_CODE_DIR = "genCCodeDir";
    private static final String GEN_C_CODE_DIR_DESC = "The output directory into which to write generated C/C++ code."
            + " By default this will overwrite (without deletion) the generated C/C++ files in this ICU release,"
            + " so it is recommended that for testing, it be set to another value.";
    private static final String GEN_C_CODE_DIR_DEFAULT = "${icuDir}/icu4c/source";
    String genCCodeDir;

    private static final String GEN_JAVA_CODE_DIR = "genJavaCodeDir";
    private static final String GEN_JAVA_CODE_DIR_DESC = "The output directory into which to write generated Java code."
            + " By default this will overwrite (without deletion) the generated Java files in this ICU release,"
            + " so it is recommended that for testing, it be set to another value.";
    private static final String GEN_JAVA_CODE_DIR_DEFAULT = "${icuDir}/icu4j/main/core";
    String genJavaCodeDir;

    private static final String DONT_GEN_CODE = "dontGenCode";
    private static final String DONT_GEN_CODE_DESC = "Set this to prevent the generation of ICU source files";
    private static final String DONT_GEN_CODE_DEFAULT = "false";
    boolean dontGenCode;

    private static final String SPECIALS_DIR = "specialsDir";
    private static final String SPECIALS_DIR_DESC = "The directory in which the additional ICU XML data is stored.";
    private static final String SPECIALS_DIR_DEFAULT =  "${icuDir}/icu4c/source/data/xml";
    String specialsDir;

    private static final String ICU_VERSION = "icuVersion";
    private static final String ICU_VERSION_DESC = "Default value for ICU version (`icuver.txt`)."
            + " Update this for each release.";
    private static final String ICU_VERSION_DEFAULT = VersionInfo.ICU_VERSION.toString();
    String icuVersion;

    private static final String ICU_DATA_VERSION = "icuDataVersion";
    private static final String ICU_DATA_VERSION_DESC = "Default value for ICU data version (`icuver.txt`)."
            + " Update this for each release.";
    private static final String ICU_DATA_VERSION_DEFAULT = VersionInfo.ICU_DATA_VERSION.toString();
    String icuDataVersion;

    private static final String CLDR_VERSION = "cldrVersion";
    private static final String CLDR_VERSION_DESC = "An override for the CLDR version string (`icuver.txt` and others)."
            + " This will be extracted from the CLDR library used for building the data if not set here.";
    private static final String CLDR_VERSION_DEFAULT = "";
    String cldrVersion;

    private static final String MIN_DRAFT_STATUS = "minDraftStatus";
    private static final String MIN_DRAFT_STATUS_DESC = "The minimum draft status for CLDR data to be used in the conversion."
            + " See CldrDraftStatus for more details.";
    private static final String MIN_DRAFT_STATUS_DEFAULT = "CONTRIBUTED";
    String minDraftStatus;

    private static final String LOCALE_ID_FILTER = "localeIdFilter";
    private static final String LOCALE_ID_FILTER_DESC = "A regular expression to match the locale IDs to be generated"
            + " (useful for debugging specific regions). This is applied after locale ID specifications"
            + " have been expanded into full locale IDs, so the value `en` will NOT match `en_GB` or `en_001` etc.";
    private static final String LOCALE_ID_FILTER_DEFAULT = "";
    String localeIdFilter;

    private static final String INCLUDE_PSEUDO_LOCALES = "includePseudoLocales";
    private static final String INCLUDE_PSEUDO_LOCALES_DESC = "Whether to synthetically generate \"pseudo locale\" data"
            + " (`en_XA` and `ar_XB`).";
    private static final String INCLUDE_PSEUDO_LOCALES_DEFAULT = "false";
    boolean includePseudoLocales;

    private static final String EMIT_REPORT = "emitReport";
    private static final String EMIT_REPORT_DESC = "Whether to emit a debug report containing some possibly"
            + " useful information after the conversion has finished.";
    private static final String EMIT_REPORT_DEFAULT = "false";
    boolean emitReport;

    private static final String OUTPUT_TYPES = "outputTypes";
    private static final String OUTPUT_TYPES_DESC = "List of output \"types\" to be generated (e.g. `rbnf,plurals,locales`);"
            + " an empty list means \"build everything\".\n"
            + "Note that the grouping of types is based on the legacy converter behaviour and"
            + " is not always directly associated with an output directory (e.g. \"locales\") produces locale data"
            + " for `curr/`, `lang/`, `main/`, `region/`, `unit/`, `zone/` but NOT `coll/`, `brkitr/` or `rbnf/`).\n"
            // It would be nice to initialize this from OutputType, but to do that we need to read an XML file,
            // so we need to know what the cldrDir folder is. But we only know that AFTER we parse the command line.
            + "Use outputTypesList to get a list of currently know values.";
    private static final String OUTPUT_TYPES_DEFAULT = "";
    String outputTypes;

    private static final String OUTPUT_TYPES_LIST = "outputTypesList";
    private static final String OUTPUT_TYPES_LIST_DESC = "Show the complete list of knonw output types and exit.";
    private static final String OUTPUT_TYPES_LIST_DEFAULT = "false";

    private static final String FORCE_DELETE = "forceDelete";
    private static final String FORCE_DELETE_DESC = "Specify to force the 'clean' task to delete files it cannot"
            + " determine to be auto-generated by this tool. This is useful if the file header changes since"
            + " the heading is what's used to recognize auto-generated files.";
    private static final String FORCE_DELETE_DEFAULT = "false";
    boolean forceDelete;

    private static final String XML_CONFIG = "xmlConfig";
    private static final String XML_CONFIG_DESC = "Override to force the 'clean' task to delete files it cannot"
            + " determine to be auto-generated by this tool. This is useful if the file header changes since"
            + " the heading is what's used to recognize auto-generated files.";
    private static final String XML_CONFIG_DEFAULT = "${icuDir}/tools/cldr/cldr-to-icu/config.xml";
    String xmlConfig;

    private static final String PARALLEL = "parallel";
    private static final String PARALLEL_DESC = "Run the generation in parallel (multithreaded), to make it faster.";
    private static final String PARALLEL_DEFAULT = "false";
    boolean parallel;

    // These must be kept in sync with getOptions().
    private static final Options options = new Options()
            .addOption(Option.builder()
                    .longOpt(HELP)
                    .desc(HELP_DESC)
                    .build())
            .addOption(Option.builder()
                    .longOpt(ICU_DIR)
                    .hasArg()
                    .argName("path")
                    .desc(descWithDefault(ICU_DIR_DESC, ICU_DIR_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(CLDR_DIR)
                    .hasArg()
                    .argName("path")
                    .desc(descWithDefault(CLDR_DIR_DESC, CLDR_DIR_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(CLDR_DATA_DIR)
                    .hasArg()
                    .argName("path")
                    .desc(descWithDefault(CLDR_DATA_DIR_DESC, CLDR_DATA_DIR_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(OUT_DIR)
                    .hasArg()
                    .argName("path")
                    .desc(descWithDefault(OUT_DIR_DESC, OUT_DIR_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(GEN_C_CODE_DIR)
                    .hasArg()
                    .argName("path")
                    .desc(descWithDefault(GEN_C_CODE_DIR_DESC, GEN_C_CODE_DIR_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(GEN_JAVA_CODE_DIR)
                    .hasArg()
                    .argName("path")
                    .desc(descWithDefault(GEN_JAVA_CODE_DIR_DESC, GEN_JAVA_CODE_DIR_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(DONT_GEN_CODE)
                    .desc(descWithDefault(DONT_GEN_CODE_DESC, DONT_GEN_CODE_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(SPECIALS_DIR)
                    .hasArg()
                    .argName("path")
                    .desc(descWithDefault(SPECIALS_DIR_DESC, SPECIALS_DIR_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(OUTPUT_TYPES)
                    .hasArg()
                    .argName("out_types")
                    .desc(descWithDefault(OUTPUT_TYPES_DESC, OUTPUT_TYPES_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(OUTPUT_TYPES_LIST)
                    .desc(descWithDefault(OUTPUT_TYPES_LIST_DESC, OUTPUT_TYPES_LIST_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(ICU_VERSION)
                    .hasArg()
                    .argName("version")
                    .desc(descWithDefault(ICU_VERSION_DESC, ICU_VERSION_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(ICU_DATA_VERSION)
                    .hasArg()
                    .argName("version")
                    .desc(descWithDefault(ICU_DATA_VERSION_DESC, ICU_DATA_VERSION_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(CLDR_VERSION)
                    .hasArg()
                    .argName("version")
                    .desc(descWithDefault(CLDR_VERSION_DESC, CLDR_VERSION_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(MIN_DRAFT_STATUS)
                    .hasArg()
                    .argName("draft_status")
                    .desc(descWithDefault(MIN_DRAFT_STATUS_DESC, MIN_DRAFT_STATUS_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(LOCALE_ID_FILTER)
                    .hasArg()
                    .argName("locale_list")
                    .desc(descWithDefault(LOCALE_ID_FILTER_DESC, LOCALE_ID_FILTER_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(INCLUDE_PSEUDO_LOCALES)
                    .desc(descWithDefault(INCLUDE_PSEUDO_LOCALES_DESC, INCLUDE_PSEUDO_LOCALES_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(EMIT_REPORT)
                    .desc(descWithDefault(EMIT_REPORT_DESC, EMIT_REPORT_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(FORCE_DELETE)
                    .desc(descWithDefault(FORCE_DELETE_DESC, FORCE_DELETE_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(XML_CONFIG)
                    .hasArg()
                    .argName("path")
                    .desc(descWithDefault(XML_CONFIG_DESC, XML_CONFIG_DEFAULT))
                    .build())
            .addOption(Option.builder()
                    .longOpt(PARALLEL)
                    .desc(descWithDefault(PARALLEL_DESC, PARALLEL_DEFAULT))
                    .build())
            ;

    void processArgs(String[] args) {
        CommandLine cli = null;
        try{
            CommandLineParser parser = new DefaultParser();
            cli = parser.parse(options, args);
        } catch (Exception e){
            cli = CommandLine.builder().build();
            showUsageAndExit();
        }
        if (cli.hasOption(HELP)) {
            showUsageAndExit();
        }

        icuDir = cli.getOptionValue(ICU_DIR, icuDir);
        cldrDir = cli.getOptionValue(CLDR_DIR, cldrDir);
        cldrDataDir = cli.getOptionValue(CLDR_DATA_DIR, cldrDataDir);

        outDir = cli.getOptionValue(OUT_DIR, expandFolders(OUT_DIR_DEFAULT));
        genCCodeDir = cli.getOptionValue(GEN_C_CODE_DIR, expandFolders(GEN_C_CODE_DIR_DEFAULT));
        genJavaCodeDir = cli.getOptionValue(GEN_JAVA_CODE_DIR, expandFolders(GEN_JAVA_CODE_DIR_DEFAULT));
        dontGenCode = cli.hasOption(DONT_GEN_CODE);
        specialsDir = cli.getOptionValue(SPECIALS_DIR, expandFolders(SPECIALS_DIR_DEFAULT));
        outputTypes = cli.getOptionValue(OUTPUT_TYPES, ""); // empty means all
        icuVersion = cli.getOptionValue(ICU_VERSION, ICU_VERSION_DEFAULT);
        icuDataVersion = cli.getOptionValue(ICU_DATA_VERSION, ICU_DATA_VERSION_DEFAULT);
        cldrVersion = cli.getOptionValue(CLDR_VERSION, CLDR_VERSION_DEFAULT);
        minDraftStatus = cli.getOptionValue(MIN_DRAFT_STATUS, MIN_DRAFT_STATUS_DEFAULT);
        localeIdFilter = cli.getOptionValue(LOCALE_ID_FILTER, LOCALE_ID_FILTER_DEFAULT);
        includePseudoLocales = cli.hasOption(INCLUDE_PSEUDO_LOCALES);
        emitReport = cli.hasOption(EMIT_REPORT);
        forceDelete = cli.hasOption(FORCE_DELETE);
        xmlConfig = cli.getOptionValue(XML_CONFIG, expandFolders(XML_CONFIG_DEFAULT));
        parallel = cli.hasOption(PARALLEL);

        if (cli.hasOption(OUTPUT_TYPES_LIST)) {
            OutputType[] outTypesToSort = OutputType.values();
            Arrays.sort(outTypesToSort, (o1, o2) -> o1.name().compareTo(o2.name()));
            StringJoiner strOutType = new StringJoiner(", ");
            for (OutputType ot : outTypesToSort) {
                strOutType.add(ot.name());
            }
            System.out.println("Known output types: " + strOutType);
            System.exit(2);
        }
    }

    private static String descWithDefault(String description, String defaultValue) {
        if (defaultValue != null) {
            return description + "\nDefaults to: \"" + defaultValue + "\"";
        } else {
            return description;
        }
    }

    private void showUsageAndExit() {
        String thisClassName = Cldr2Icu.class.getCanonicalName();
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(
                /*width*/ 120,
                /*cmdLineSyntax*/ thisClassName + " [OPTIONS]\n",
                /*header*/ "\n"
                        + "This program is used to convert CLDR xml files to ICU ResourceBundle txt files.\n"
                        + "Options:",
                options,
                /*footer*/ "\nExample: " + thisClassName + " --outDir /tmp/debug --localeIdFilter=fr");
        System.exit(-1);
    }

    Cldr2IcuCliOptions() {
        // This will initialize icuDir, cldrDir, and cldrDataDir from environment variables
        validateEnvironment();
    }

    String expandFolders(String str) {
        return str
                .replace("${icuDir}", icuDir)
                .replace("${cldrDir}", cldrDir)
                .replace("${cldrDataDir}", cldrDataDir);
    }

    // For certain things we want to check both the environment, and Java properties
    // (passed with -Dkey=value)
    // The property takes precedence.
    private static String getEnvironOrProperty(String key) {
        String result = System.getProperty(key);
        if (result == null) {
            result = System.getenv(key);
        }
        return result;
    }

    // Check that the environment variables point to the proper `icu` / `cldr` / `cldr-staging` folders
    private void validateEnvironment() {
        icuDir = getEnvironOrProperty("ICU_DIR");
        cldrDir = getEnvironOrProperty("CLDR_DIR");
        cldrDataDir = getEnvironOrProperty("CLDR_DATA_DIR");

        String icuMessage = "Set the ICU_DIR environment variable to the top level ICU directory (containing `.git`, `icu4c`, `icu4j`, `tools` directories)";
        String cldrMessage = "Set the CLDR_DIR environment variable to the top level CLDR directory (containing `common` and `tools` directories)";
        String cldrDataMessage = "Set the CLDR_DATA_DIR environment variable to the top level CLDR production data directory (typically the `production` directory in the staging repository)\n"
            + "Usually generated locally or obtained from: https://github.com/unicode-org/cldr-staging/tree/main/production";
        if (icuDir == null) {
            System.err.println(icuMessage);
            System.exit(1);
        }
        if (cldrDir == null) {
            System.err.println(cldrMessage);
            System.exit(1);
        }
        if (cldrDataDir == null) {
            System.err.println(cldrDataMessage);
            System.exit(1);
        }

        if (!new File(icuDir).isDirectory()
                || ! new File(icuDir, "icu4c").isDirectory()
                || ! new File(icuDir, "icu4j").isDirectory()
                || ! new File(icuDir, "tools/cldr/cldr-to-icu").isDirectory()
                || ! new File(icuDir, "tools/cldr/cldr-to-icu/pom.xml").isFile()) {
            System.err.println("The `" + icuDir + "` directory does not look like a valid icu root.");
            System.err.println(icuMessage);
            System.exit(1);
        }
        if (!new File(cldrDir).isDirectory()
                || ! new File(cldrDir, "tools/cldr-code").isDirectory()
                || ! new File(cldrDir, "tools/cldr-code/pom.xml").isFile()) {
            System.err.println("The `" + cldrDir + "` directory does not look like a valid cldr root.");
            System.err.println(cldrMessage);
            System.exit(1);
        }
        if (!new File(cldrDataDir).isDirectory()
                || ! new File(cldrDataDir, "common/supplemental").isDirectory()
                || ! new File(cldrDataDir, "common/main").isDirectory()
                || ! new File(cldrDataDir, "common/main/en.xml").isFile()) {
            System.err.println("The `" + cldrDataDir + "` directory does not look like a valid cldr-staging/ root.");
            System.err.println(cldrDataMessage);
            System.exit(1);
        }

        // The cldr-code library checks for CLDR_DIR in the Java properties.
        // So if we got cldrDir from or from environment or command line we update the property.
        System.setProperty("CLDR_DIR", cldrDir);
    }
}
