// © 2025 and later: Unicode, Inc. and others.

package com.ibm.icu.dev.tool.errorprone;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableBiMap;
import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;

class ErrorProneUtils {

    /*
     * This is from errorprone library, a list of all known issue types.
     *
     * The reason why we need all of this complication:
     *
     * The errorprone tool is implemented as a javac compiler extension.
     * So when the code is compiled, the errorprone "hooks" are invoked by the
     * Java compiler.
     * As such, errorprone can only use the Java compiler mechanisms to report issues.
     * But if it reports an error, the compiler stops compiling the rest of the files.
     * In order to generate a report on the whole source tree, errorprone has a flag
     * to report all issues as warnings (-XepAllErrorsAsWarnings).
     * That way the compiler continues on the whole project and does not stop.
     * But because of that, even the worst errorprone violations that would qualify
     * as errors are still reported as warnings.
     * So to "get back" the proper severity level we depend on the errorprone library,
     * get all known errors, and then use the `defaultSeverity` field within each `BugCheckerInfo`.
     */
    private static final ImmutableBiMap<String, BugCheckerInfo> KNOWN_ERRORS =
            BuiltInCheckerSuppliers.allChecks().getAllChecks();
    static final String SEVERITY_UNKNOWN = "UNKNOWN";
    // Some special severity classes, to group what ICU considers important to fix.
    static final String SEVERITY_ICU_PRI1 = "ICU_PRIORITY_1";
    static final String SEVERITY_ICU_PRI2 = "ICU_PRIORITY_2";

    // This gives the order of the summary in the report
    // The extra "unknown" option seems weird, but at this point errorprone reports an issue
    // (type `dep-ann`) but does not list it as a known check. (April 2025)
    // It is likely a bug, might be fixed in the meantime, but another one might come back
    // at some point in the future.
    // We never know, so we take care of that special case.
    static final List<String> SEVERITY_LEVELS_TO_REPORT = Arrays.asList(
            // A special severity class, where we show first what ICU considers important to fix.
            SEVERITY_ICU_PRI1,
            SEVERITY_ICU_PRI2,
            BugPattern.SeverityLevel.ERROR.toString(),
            ErrorProneUtils.SEVERITY_UNKNOWN,
            BugPattern.SeverityLevel.WARNING.toString(),
            BugPattern.SeverityLevel.SUGGESTION.toString());

    // A special severity class, where we show first what ICU considers important to fix.
    static final Map<String, String> ICU_SPECIAL_SEVERITIES = Map.of(
            // Example:
            // "MissingFail", SEVERITY_ICU_PRI1,
            // "ReferenceEquality", SEVERITY_ICU_PRI2
    );

    /**
     * Given an error type (for example `BadImport`, `UnusedVariable`)
     * it returns the error level (error, warning, info, etc).
     *
     * @param errorType the error type, as reported by errorprone
     * @return the error level (severity)
     */
    static String getErrorLevel(String errorType) {
        String icuSpecialSeverity = ICU_SPECIAL_SEVERITIES.get(errorType);
        if (icuSpecialSeverity != null) {
            return icuSpecialSeverity;
        }
        BugCheckerInfo found = KNOWN_ERRORS.get(errorType);
        return found == null
                ? SEVERITY_UNKNOWN
                : found.defaultSeverity().toString();
    }

    /**
     * Given an error type (for example `BadImport`, `UnusedVariable`)
     * it returns the url to a public page explaining the error.
     *
     * @param errorType the error type, as reported by errorprone
     * @return the url to a public explanation page
     */
    static String getUrl(String errorType) {
        BugCheckerInfo found = KNOWN_ERRORS.get(errorType);
        return found == null ? null : found.linkUrl();
    }
}
