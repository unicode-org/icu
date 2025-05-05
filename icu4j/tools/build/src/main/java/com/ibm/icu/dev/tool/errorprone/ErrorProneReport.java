// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.tool.errorprone;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.collect.ImmutableBiMap;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;

class ErrorProneReport {
    private static final String HTML_REPORT_FILE = "errorprone1.html";
    private static final String HTML_REPORT_FILE2 = "errorprone2.html";
    private static final String SORTTABLE_JS_FILE = "sorttable.js";
    private static final String SORTTABLE_CSS_FILE = "errorprone.css";
    private static final String [] EMBEDDED_FILES =
        { SORTTABLE_JS_FILE, SORTTABLE_CSS_FILE };
    private static final ImmutableBiMap<String, BugCheckerInfo> KNOWN_ERRORS =
            BuiltInCheckerSuppliers.allChecks().getAllChecks();

    public static void genReports(String icuDir, String mavenStdOut, String outDir, String baseUrl)
            throws IOException {

        Map<String, List<ErrorProneEntry>> errors =
                ParseMavenOutForErrorProne.parse(icuDir, mavenStdOut);

        extractExtraFiles(outDir);
        genReport1(errors, outDir, baseUrl);
        genReport2(errors, outDir, baseUrl);
    }

    // Extract additional files used by the reports (css, js, etc)
    private static void extractExtraFiles(String outFolder) throws IOException {
        for (String fileName : EMBEDDED_FILES) {
            try (InputStream is = ErrorProneReport.class.getResourceAsStream(fileName)) {
                Files.copy(is, Paths.get(outFolder, fileName), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void genReport1(Map<String, List<ErrorProneEntry>> errors,
            String outDir, String baseUrl) throws IOException {

        Path outFileName = Paths.get(outDir, HTML_REPORT_FILE);
        System.out.println("Report generated: " + outFileName);
        try (PrintStream wrt = new PrintStream(outFileName.toString(), StandardCharsets.UTF_8)) {
            HtmlUtils hu = new HtmlUtils(wrt);
            hu.openTag("html");

            outHtmlHead(hu);

            hu.openTag("body");

            outTitle(hu);

            hu.openTag("table", Map.of("class", "sortable"));

            // Table header
            hu.openTag("thead");
            hu.openTag("tr");
            hu.openTag("th").text("File and line number").closeTag("th");
            hu.openTag("th").text("Severity").closeTag("th");
            hu.openTag("th").text("Issue type").closeTag("th");
            hu.openTag("th").text("Message").closeTag("th");
            hu.closeTag("tr");
            hu.closeTag("thead");

            for (Map.Entry<String, List<ErrorProneEntry>> e : errors.entrySet()) {
                for (ErrorProneEntry error : e.getValue()) {
                    hu.openTag("tr");

                    outFilePath(hu, error, baseUrl);

                    // Error severity
                    hu.openTag("td", Map.of("class", "severity_" + error.severity))
                            .text(error.severity)
                            .closeTag("td");

                    // Error type
                    hu.openTag("td", Map.of("class", "tag"));
                    Map<String, String> attr = error.url == null
                            ? Map.of("target", "errWin")
                            : Map.of("href", error.url, "target", "errWin");
                    hu.openTag("a", attr).text(error.type).closeTag("a");
                    hu.closeTag("td");

                    outDescription(hu, error);

                    hu.closeTag("tr");
                }
            }

            hu.closeTag("table");

            hu.closeTag("body");
            hu.closeTag("html");
        }
    }

    private static void genReport2(Map<String, List<ErrorProneEntry>> errors,
            String outDir, String baseUrl) throws IOException {
        Path outFileName = Paths.get(outDir, HTML_REPORT_FILE2);
        System.out.println("Report generated: " + outFileName);
        try (PrintStream wrt = new PrintStream(outFileName.toString(), StandardCharsets.UTF_8)) {
            HtmlUtils hu = new HtmlUtils(wrt);
            hu.openTag("html");

            outHtmlHead(hu);

            hu.openTag("body");

            outTitle(hu);

            hu.openTag("div");
            hu.openTag("h2").text("Summary").closeTag("h2");
            for (String severityLevel : ErrorProneUtils.SEVERITY_LEVELS_TO_REPORT) {
                outSummary(hu, errors, severityLevel);
            }
            hu.closeTag("div");

            hu.openTag("hr");

            hu.openTag("h2").text("Detailed report").closeTag("h2");
            for (Map.Entry<String, List<ErrorProneEntry>> e : errors.entrySet()) {
                String errorType = e.getKey();
                List<ErrorProneEntry> errorsOfType = e.getValue();
                if (errorsOfType.isEmpty()) {
                    continue;
                }
                ErrorProneEntry firstEntry = errorsOfType.get(0);
                String errorSeverity = e.getKey();
                // "class", "severity_" + errorSeverity)
                hu.openTag("h3", Map.of("id", "name_" + errorType))
                        .text("[" + firstEntry.severity + "] ")
                        .openTag("span", Map.of("class", "tag")).text(errorType).closeTag("span")
                        .text(" [" + errorsOfType.size() + "] ");
                String url = ErrorProneUtils.getUrl(errorType);
                if (url != null) {
                    hu.openTag("a", Map.of("href", url, "target", "errWin"))
                            .text("\uD83D\uDD17") // link emoji, U+1F517
                            .closeTag("a");
                }
                hu.closeTag("h2");

                Map<String, String> attr = errorType == null
                        ? Map.of("target", "errWin")
                        : Map.of("href", errorType, "target", "errWin");

                hu.openTag("table", Map.of("class", "sortable"));

                // Table header
                hu.openTag("thead");
                hu.openTag("tr");
                hu.openTag("th").text("File and line number").closeTag("th");
                hu.openTag("th").text("Message").closeTag("th");
                hu.closeTag("tr");
                hu.closeTag("thead");

                for (ErrorProneEntry error : errorsOfType) {
                    if (!error.type.equals(e.getKey())) {
                        continue;
                    }
                    hu.openTag("tr");
                    outFilePath(hu, error, baseUrl);
                    outDescription(hu, error);
                    hu.closeTag("tr");
                }
                hu.closeTag("table");
            }

            hu.closeTag("body");
            hu.closeTag("html");
        }
    }

    private static void outHtmlHead(HtmlUtils hu) {
        hu.openTag("head");
        hu.openTag("meta", Map.of("charset", "UTF-8"));
        hu.openTag("link", Map.of("rel", "stylesheet", "href", SORTTABLE_CSS_FILE));
        hu.openTag("script", Map.of("src", SORTTABLE_JS_FILE)).closeTag("script");
        hu.closeTag("head");
    }

    private static void outFilePath(HtmlUtils hu, ErrorProneEntry error, String baseUrl) {
        String visiblePath = error.path + ":[" + error.line + "," + error.column + "]";
        String url = baseUrl + "/" + error.path + "#L" + error.line;
        hu.openTag("td", Map.of("class", "file_name"));
        hu.openTag("a", Map.of("href", url, "target", "codeWin")).text(visiblePath).closeTag("a");
        hu.closeTag("td");
    }

    private static void outDescription(HtmlUtils hu, ErrorProneEntry error) {
        hu.openTag("td", Map.of("class", "desc"));
        hu.text(error.message);
        if (error.extra != null) {
            hu.openTag("hr");
            String extra = error.extra;
            if (extra.startsWith("Did you mean '") && extra.endsWith("'?")) {
                hu.indent();
                hu.text("Did you mean ");
                hu.openTag("br");
                hu.indent();
                hu.openTag("code");
                extra = extra.substring(14, extra.length() - 2);
                hu.text(extra);
                hu.closeTag("code");
            } else {
                hu.text(extra);
            }
        }
        hu.closeTag("td");
    }

    private static void outTitle(HtmlUtils hu) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMMM-dd, HH:mm:ss", Locale.US);
        String title = "ErrorProne report, " + sdf.format(new Date());
        hu.openTag("h1").text(title).closeTag("h1");
    }

    private static void outSummary(HtmlUtils hu, Map<String, List<ErrorProneEntry>> errors,
            String severityLevel) {
        boolean first = true;
        for (Map.Entry<String, List<ErrorProneEntry>> e : errors.entrySet()) {
            String errorSeverity = ErrorProneUtils.getErrorLevel(e.getKey());
            if (!severityLevel.equals(errorSeverity)) {
                continue;
            }
            if (first) {
                hu.openTag("h3").text(severityLevel).closeTag("h3");
                hu.openTag("p");
                first = false;
            } else {
                hu.text(" \u2022 "); // bullet
            }
            // <a href="#name_error"><code>MissingFail</code></a> [3]
            hu.openTag("a", Map.of(
                            "href", "#name_" + e.getKey(),
                            "class", "severity_" + errorSeverity))
                    .openTag("span", Map.of("class", "tag"))
                    .text(e.getKey())
                    .closeTag("span")
                    .closeTag("a")
                    .text(" [" + e.getValue().size() + "]\n");
        }
        if (!first) {
            hu.closeTag("p").nl();
        }
    }
}
