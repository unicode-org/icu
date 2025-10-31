// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.tool.errorprone;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParseMavenOutForErrorProne {
    // The `(?:[A-F]:)?` in the beginning is for the Windows drive letter (for example D:)
    private static final String RE_ERROR_PRONE_START =
            "^\\[WARNING\\] ((?:[A-F]:)?[\\\\/a-zA-Z0-9_.\\-]+\\.java):\\[([0-9,]+)\\]"
                    + " \\[(\\S+)\\] (.+)";
    private static final Pattern PATTERN = Pattern.compile(RE_ERROR_PRONE_START);

    // These are ICU custom tags, but errorprone does not allow us to exclude them.
    // So we will filter them out in our code.
    private static final Set<String> CUSTOM_ICU_TAGS =
            Set.of(
                    "bug",
                    "category",
                    "discouraged",
                    "draft",
                    "icuenhanced",
                    "internal",
                    "icu",
                    "icunote",
                    "obsolete",
                    "provisional",
                    "stable",
                    "summary",
                    "test");

    /**
     * The result contains the issues reported by errorprone.
     *
     * <p>The key is the issue type (for example `MissingOverride` or `InvalidThrows`). The value is
     * a list with all the issues of that type, in the order in which they were reported.
     *
     * @param baseDir the "prefix" to remove from the file paths (usually the root of icu)
     * @param fileName the name of the maven stdout log file to parse
     * @return the summary, a map where the key is the issue type, and the value a list of all
     *     issues of that type
     * @throws IOException for any kind of file system problems.
     */
    static Map<String, List<ErrorProneEntry>> parse(String baseDir, String fileName)
            throws IOException {
        Map<String, List<ErrorProneEntry>> errorReport = new TreeMap<>();
        ErrorProneEntry currentError = null;

        if (baseDir != null) {
            baseDir = baseDir.replace('\\', '/');
        }
        int currentLine = 0;
        for (String line : Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8)) {
            currentLine++;
            Matcher m = PATTERN.matcher(line);
            if (m.find()) {
                String path = line.substring(m.start(1), m.end(1)).replace('\\', '/');
                if (baseDir != null) {
                    if (path.startsWith(baseDir)) {
                        path = path.substring(baseDir.length());
                    }
                }
                // If we already had an error report in progress, save it.
                addErrorToReportAndReset(errorReport, currentError);
                String lineColumn = line.substring(m.start(2), m.end(2));
                int columnNo = 0;
                // Some entries have a only line info, others have comma separated line,column
                int commaIndex = lineColumn.indexOf(',');
                if (commaIndex != -1) {
                    columnNo = Integer.parseInt(lineColumn.substring(commaIndex + 1));
                    lineColumn = lineColumn.substring(0, commaIndex);
                }
                int lineNo = Integer.parseInt(lineColumn);
                currentError =
                        new ErrorProneEntry(
                                path,
                                lineNo,
                                columnNo,
                                line.substring(m.start(3), m.end(3)), // error code
                                line.substring(m.start(4), m.end(4)) // message
                                );
            } else if (line.trim().isEmpty()) {
                // Skip empty lines
            } else if (line.startsWith("  Did you mean ")) {
                if (currentError == null) {
                    error(fileName, currentLine, line, "Parse error: unexpected 'Did you mean' ");
                } else {
                    currentError.addExtra(line.trim());
                }
            } else if (line.startsWith("    (see https://") && line.endsWith(")")) {
                if (currentError == null) {
                    error(fileName, currentLine, line, "Parse error: unexpected '(see <url>)'");
                } else {
                    // 9 is the length of "    (see "
                    currentError.url = line.substring(9, line.length() - 1);
                }
            } else if (line.equals(
                    "[WARNING] Unable to autodetect 'javac' path, using 'javac' from the"
                            + " environment.")) {
                currentError = addErrorToReportAndReset(errorReport, currentError);
            } else if (line.startsWith("[INFO]")) {
                currentError = addErrorToReportAndReset(errorReport, currentError);
            } else {
                error(fileName, currentLine, line, "Parse error: I don't know what this is");
                currentError = addErrorToReportAndReset(errorReport, currentError);
            }
        }
        // In case we had an error report in progress, save it.
        addErrorToReportAndReset(errorReport, currentError);
        return errorReport;
    }

    // Tab-separated, convenient to import in a spreadsheet
    private static boolean isCustomIcuTag(ErrorProneEntry crtError) {
        String errorType = crtError.type;
        if (!errorType.equals("InvalidBlockTag")) {
            return false;
        }
        String message = crtError.message;
        // We will filter out the custom ICU tags.
        // There is no programatic way to find the name of the tag, we must extract it from the
        // error message.
        // Message text 1:
        // "Tag name `stable` is unknown. If this is a commonly-used custom tag, please click 'not
        // useful' and file a bug."
        int firstBackTick = message.indexOf('`');
        if (firstBackTick >= 0) {
            int secondBackTick = message.indexOf('`', firstBackTick + 1);
            if (secondBackTick >= 0) {
                String tagName = message.substring(firstBackTick + 1, secondBackTick);
                if (CUSTOM_ICU_TAGS.contains(tagName)) {
                    return true;
                }
            }
        }
        // Message text 2:
        // "@summary is not a valid block tag. Should it be an inline tag instead?"
        if (message.startsWith("@")) {
            int firstSpace = message.indexOf(' ');
            if (firstSpace >= 0) {
                String tagName = message.substring(1, firstSpace);
                if (CUSTOM_ICU_TAGS.contains(tagName)) {
                    return true;
                }
            }
        }
        return false;
    }

    static ErrorProneEntry addErrorToReportAndReset(
            Map<String, List<ErrorProneEntry>> errorReport, ErrorProneEntry crtError) {
        // We want to reset the currentError after we record it.
        // One errorprone issue can take several lines in the log.
        // The parsing creates currentError when the start of an issue is detected.
        // We add more info to the currentError from the following lines.
        // When we find something that does not look like an errorprone line, or at the end of the
        // log, we add currentError to the report, and then we set it to null.
        // By returning null here the call in the main loop can be one single line:
        //   currentError = addErrorToReport(errorReport, currentError); => report AND reset
        // If we return nothing (void method) we would need to do this in the caller (several
        // times):
        //   addErrorToReport(errorReport, currentError); => report
        //   currentError = null; => reset
        if (crtError == null) {
            return null;
        }
        if (isCustomIcuTag(crtError)) {
            return null;
        }
        // Fix the severity from parsing, which is never error, to the proper errorprone one
        String errorType = crtError.type;
        crtError.severity = ErrorProneUtils.getErrorLevel(errorType);
        List<ErrorProneEntry> list =
                errorReport.computeIfAbsent(errorType, e -> new ArrayList<ErrorProneEntry>());
        list.add(crtError);
        return null;
    }

    private static void error(String fileName, int lineNo, String line, String msg) {
        System.out.printf("\033[91m%s[%s] %s %n   '%s'\033[m%n", fileName, lineNo, msg, line);
    }
}
