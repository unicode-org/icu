// © 2025 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.tool.errorprone;

import com.google.errorprone.BugCheckerInfo;
import com.google.errorprone.BugPattern;
import com.google.errorprone.scanner.BuiltInCheckerSuppliers;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final Map<String, BugCheckerInfo> KNOWN_ERRORS =
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
    static final List<String> SEVERITY_LEVELS_TO_REPORT =
            Arrays.asList(
                    // A special severity class, where we show first what ICU considers important to
                    // fix.
                    SEVERITY_ICU_PRI1,
                    SEVERITY_ICU_PRI2,
                    BugPattern.SeverityLevel.ERROR.toString(),
                    ErrorProneUtils.SEVERITY_UNKNOWN,
                    BugPattern.SeverityLevel.WARNING.toString(),
                    BugPattern.SeverityLevel.SUGGESTION.toString());

    // A special severity class, where we show first what ICU considers important to fix.
    static final Map<String, String> ICU_SPECIAL_SEVERITIES =
            Map.ofEntries(
                    // Example:
                    // Map.entry("MissingFail", SEVERITY_ICU_PRI1),
                    // Map.entry("ReferenceEquality", SEVERITY_ICU_PRI2)
                    Map.entry("DefaultCharset", SEVERITY_ICU_PRI1),
                    Map.entry("Finally", SEVERITY_ICU_PRI1),
                    Map.entry("InvalidBlockTag", SEVERITY_ICU_PRI1),
                    Map.entry("JdkObsolete", SEVERITY_ICU_PRI1),
                    Map.entry("MissingFail", SEVERITY_ICU_PRI1),
                    Map.entry("MutablePublicArray", SEVERITY_ICU_PRI1),
                    Map.entry("ObjectToString", SEVERITY_ICU_PRI1),
                    Map.entry("OperatorPrecedence", SEVERITY_ICU_PRI1),
                    Map.entry("ReferenceEquality", SEVERITY_ICU_PRI1),
                    Map.entry("StringSplitter", SEVERITY_ICU_PRI1),
                    Map.entry("SynchronizeOnNonFinalField", SEVERITY_ICU_PRI1),
                    Map.entry("UnicodeEscape", SEVERITY_ICU_PRI1),
                    Map.entry("UnusedMethod", SEVERITY_ICU_PRI1),
                    Map.entry("UnusedVariable", SEVERITY_ICU_PRI1),
                    Map.entry("AlmostJavadoc", SEVERITY_ICU_PRI2),
                    Map.entry("BadImport", SEVERITY_ICU_PRI2),
                    Map.entry("ClassCanBeStatic", SEVERITY_ICU_PRI2),
                    Map.entry("EmptyCatch", SEVERITY_ICU_PRI2),
                    Map.entry("EqualsGetClass", SEVERITY_ICU_PRI2),
                    Map.entry("EqualsUnsafeCast", SEVERITY_ICU_PRI2),
                    Map.entry("FallThrough", SEVERITY_ICU_PRI2),
                    Map.entry("Finalize", SEVERITY_ICU_PRI2),
                    Map.entry("FloatingPointLiteralPrecision", SEVERITY_ICU_PRI2),
                    Map.entry("IncrementInForLoopAndHeader", SEVERITY_ICU_PRI2),
                    Map.entry("JavaUtilDate", SEVERITY_ICU_PRI2),
                    Map.entry("LabelledBreakTarget", SEVERITY_ICU_PRI2),
                    Map.entry("LockOnNonEnclosingClassLiteral", SEVERITY_ICU_PRI2),
                    Map.entry("NarrowCalculation", SEVERITY_ICU_PRI2),
                    Map.entry("NarrowingCompoundAssignment", SEVERITY_ICU_PRI2),
                    Map.entry("ShortCircuitBoolean", SEVERITY_ICU_PRI2),
                    Map.entry("StaticAssignmentInConstructor", SEVERITY_ICU_PRI2),
                    Map.entry("StringCaseLocaleUsage", SEVERITY_ICU_PRI2),
                    Map.entry("StringCharset", SEVERITY_ICU_PRI2),
                    Map.entry("UndefinedEquals", SEVERITY_ICU_PRI2),
                    Map.entry("UnnecessaryStringBuilder", SEVERITY_ICU_PRI2),
                    Map.entry("UnsynchronizedOverridesSynchronized", SEVERITY_ICU_PRI2));

    /**
     * Given an error type (for example `BadImport`, `UnusedVariable`) it returns the error level
     * (error, warning, info, etc).
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
        return found == null ? SEVERITY_UNKNOWN : found.defaultSeverity().toString();
    }

    /**
     * Given an error type (for example `BadImport`, `UnusedVariable`) it returns the url to a
     * public page explaining the error.
     *
     * @param errorType the error type, as reported by errorprone
     * @return the url to a public explanation page
     */
    static String getUrl(String errorType) {
        BugCheckerInfo found = KNOWN_ERRORS.get(errorType);
        return found == null ? null : found.linkUrl();
    }

    /**
     * Given an error type (for example `BadImport`, `UnusedVariable`) it returns the tags
     * associtated with it (`FragileCode`, `Style`, etc.)
     *
     * @param errorType the error type, as reported by errorprone
     * @return the url to a public explanation page
     */
    static String getTags(String errorType) {
        BugCheckerInfo found = KNOWN_ERRORS.get(errorType);
        if (found == null) {
            return null;
        }
        Set<String> tags = found.getTags();
        return tags.isEmpty() ? null : tags.toString();
    }
}
