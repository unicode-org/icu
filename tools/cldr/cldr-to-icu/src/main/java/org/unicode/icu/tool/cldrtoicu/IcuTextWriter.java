// © 2019 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package org.unicode.icu.tool.cldrtoicu;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Iterables;

/**
 * Writes an IcuData object to a text file. A lot of this class was copied directly from the
 * original {@code IcuTextWriter} in the CLDR project and has a number of very idiosyncratic
 * behaviours. The behaviour of this class is currently tuned to produce perfect parity with
 * the original conversion tools, but once migration of the tools is complete, it should
 * probably be revisited and tidied up.
 */
// TODO: Link to a definitive specification for the ICU data files and remove the hacks!
final class IcuTextWriter {
    private static final String INDENT = "    ";
    // List of characters to escape in UnicodeSets
    // ('\' followed by any of '\', '[', ']', '{', '}', '-', '&', ':', '^', '=').
    private static final Pattern UNICODESET_ESCAPE =
        Pattern.compile("\\\\[\\\\\\[\\]{}\\-&:^=]");
    // Only escape \ and " from other strings.
    private static final Pattern STRING_ESCAPE = Pattern.compile("(?!')\\\\\\\\(?!')");
    private static final Pattern QUOTE_ESCAPE = Pattern.compile("\\\\?\"");

    private static final OpenOption[] ONLY_NEW_FILES = { CREATE_NEW };
    private static final OpenOption[] OVERWRITE_FILES = { CREATE, TRUNCATE_EXISTING };

    /** Write a file in ICU data format with the specified header. */
    static void writeToFile(
        IcuData icuData, Path outDir, List<String> header, boolean allowOverwrite) {

        try {
            Files.createDirectories(outDir);
            Path file = outDir.resolve(icuData.getName() + ".txt");
            OpenOption[] fileOptions = allowOverwrite ? OVERWRITE_FILES : ONLY_NEW_FILES;
            try (Writer w = Files.newBufferedWriter(file, fileOptions);
                PrintWriter out = new PrintWriter(w)) {
                new IcuTextWriter(icuData).writeTo(out, header);
            }
        } catch (IOException e) {
            throw new RuntimeException("cannot write ICU data file: " + icuData.getName(), e);
        }
    }

    private final IcuData icuData;
    private int depth = 0;
    private boolean valueWasInline = false;

    IcuTextWriter(IcuData icuData) {
        this.icuData = checkNotNull(icuData);
    }

    // TODO: Write a UTF-8 header (see https://unicode-org.atlassian.net/browse/ICU-10197).
    private void writeTo(PrintWriter out, List<String> header) {
        out.write('\uFEFF');
        writeHeaderAndComments(out, header, icuData.getFileComment());

        // Write the ICU data to file. This takes the form:
        // ----
        // <name>{
        //     foo{
        //         bar{baz}
        //     }
        // }
        // ----
        // So it's like every RbPath has an implicit prefix of the IcuData name.
        String root = icuData.getName();
        if (!icuData.hasFallback()) {
            root += ":table(nofallback)";
        }
        // TODO: Replace with "open(root, out)" once happy with differences (it adds a blank line).
        out.print(root);
        out.print("{");
        depth++;

        RbPath lastPath = RbPath.of();
        for (RbPath path : icuData.getPaths()) {
            // Close any blocks up to the common path length. Since paths are all distinct, the
            // common length should always be shorter than either path. We add 1 since we must also
            // account for the implicit root segment.
            int commonDepth = RbPath.getCommonPrefixLength(lastPath, path) + 1;
            // Before closing, the "cursor" is at the end of the last value written.
            closeLastPath(commonDepth, out);
            // After opening the value will be ready for the next value to be written.
            openNextPath(path, out);
            valueWasInline = appendValues(icuData.getName(), path, icuData.get(path), out);
            lastPath = path;
        }
        closeLastPath(0, out);
        out.println();
        out.close();
    }

    // Before: Cursor is at the end of the previous line.
    // After: Cursor is positioned immediately after the last closed '}'
    private void closeLastPath(int minDepth, PrintWriter out) {
        if (valueWasInline) {
            depth--;
            out.print('}');
            valueWasInline = false;
        }
        while (depth > minDepth) {
            close(out);
        }
    }

    // Before: Cursor is at the end of the previous line.
    // After: Cursor is positioned immediately after the newly opened '{'
    private void openNextPath(RbPath path, PrintWriter out) {
        while (depth <= path.length()) {
            // The -1 is to adjust for the implicit root element which means indentation (depth)
            // no longer matches the index of the segment we are writing.
            open(path.getSegment(depth - 1), out);
        }
    }

    private void open(String label, PrintWriter out) {
        newLineAndIndent(out);
        depth++;
        // This handles the "magic" pseudo indexing paths that are added by RegexTransformer.
        // These take the form of "<any-string>" and are used to ensure that path order can be
        // well defined even for anonymous lists of items.
        if (!label.startsWith("<") && !label.endsWith(">")) {
            out.print(label);
        }
        out.print('{');
    }

    private void close(PrintWriter out) {
        depth--;
        newLineAndIndent(out);
        out.print('}');
    }

    private void newLineAndIndent(PrintWriter out) {
        out.println();
        for (int i = 0; i < depth; i++) {
            out.print(INDENT);
        }
    }

    // Currently the "header" uses '//' line comments but the comments are in a block.
    // TODO: Sort this out so there isn't a messy mix of comment styles in the data files.
    private static void writeHeaderAndComments(
        PrintWriter out, List<String> header, List<String> comments) {

        header.forEach(s -> out.println("// " + s));
        if (!comments.isEmpty()) {
            // TODO: Don't use /* */ block quotes, just use inline // quotes.
            out.println(
                comments.stream().collect(joining("\n * ", "/**\n * ", "\n */")));
        }
    }

    /** Inserts padding and values between braces. */
    private boolean appendValues(
        String name, RbPath rbPath, List<RbValue> values, PrintWriter out) {

        RbValue onlyValue;
        boolean wasSingular = false;
        boolean quote = !rbPath.isIntPath();
        boolean isSequence = rbPath.endsWith(RB_SEQUENCE);
        if (values.size() == 1 && !mustBeArray(true, name, rbPath)) {
            onlyValue = values.get(0);
            if (onlyValue.isSingleton() && !mustBeArray(false, name, rbPath)) {
                // Value has a single element and is not being forced to be an array.
                String onlyElement = Iterables.getOnlyElement(onlyValue.getElements());
                if (quote) {
                    onlyElement = quoteInside(onlyElement);
                }
                // The numbers below are simply tuned to match the line wrapping in the original
                // CLDR code. The behaviour it produces is sometimes strange (wrapping a line just
                // for a single character) and could definitely be improved.
                // TODO: Simplify this and add hysteresis to ensure less "jarring" line wrapping.
                int maxWidth = Math.max(68, 80 - Math.min(4, rbPath.length()) * INDENT.length());
                if (onlyElement.length() <= maxWidth) {
                    // Single element for path: don't add newlines.
                    printValue(out, onlyElement, quote);
                    wasSingular = true;
                } else {
                    // Element too long to fit in one line, so wrap.
                    int end;
                    for (int i = 0; i < onlyElement.length(); i = end) {
                        end = goodBreak(onlyElement, i + maxWidth);
                        String part = onlyElement.substring(i, end);
                        newLineAndIndent(out);
                        printValue(out, part, quote);
                    }
                }
            } else {
                // Only one array for the rbPath, so don't add an extra set of braces.
                printArray(onlyValue, quote, isSequence, out);
            }
        } else {
            for (RbValue value : values) {
                if (value.isSingleton()) {
                    // Single-value array: print normally.
                    printArray(value, quote, isSequence, out);
                } else {
                    // Enclose this array in braces to separate it from other values.
                    open("", out);
                    printArray(value, quote, isSequence, out);
                    close(out);
                }
            }
        }
        return wasSingular;
    }

    private static final RbPath RB_SEQUENCE = RbPath.of("Sequence");
    private static final RbPath RB_RULES = RbPath.of("rules");
    private static final RbPath RB_LOCALE_SCRIPT = RbPath.of("LocaleScript");
    private static final RbPath RB_ERAS = RbPath.of("eras");
    private static final RbPath RB_NAMED = RbPath.of("named");
    private static final RbPath RB_CALENDAR_PREFERENCE_DATA = RbPath.of("calendarPreferenceData");
    private static final RbPath RB_METAZONE_INFO = RbPath.of("metazoneInfo");

    /**
     * Wrapper for a hack to determine if the given rb path should always present its values as an
     * array.
     */
    // TODO: Verify this is still needed, and either make it less hacky, or delete it.
    private static boolean mustBeArray(boolean topValues, String name, RbPath rbPath) {
        if (topValues) {
            // matches "rules/setNN" (hence the mucking about with raw segments).
            return name.equals("pluralRanges")
                && rbPath.startsWith(RB_RULES)
                && rbPath.getSegment(1).startsWith("set");
        }
        return rbPath.equals(RB_LOCALE_SCRIPT)
            || (rbPath.contains(RB_ERAS)
                && !rbPath.getSegment(rbPath.length() - 1).endsWith(":alias")
                && !rbPath.endsWith(RB_NAMED))
            || rbPath.startsWith(RB_CALENDAR_PREFERENCE_DATA)
            || rbPath.startsWith(RB_METAZONE_INFO);
    }

    private void printArray(RbValue rbValue, boolean quote, boolean isSequence, PrintWriter out) {
        for (String v : rbValue.getElements()) {
            newLineAndIndent(out);
            printValue(out, quoteInside(v), quote);
            if (!isSequence) {
                out.print(",");
            }
        }
    }

    private static void printValue(PrintWriter out, String value, boolean quote) {
        if (quote) {
            out.append('"').append(value).append('"');
        } else {
            out.append(value);
        }
    }

    // Can a string be broken here? If not, backup until we can.
    // TODO: Either don't bother line wrapping or look at making this use a line-break iterator.
    private static int goodBreak(String quoted, int end) {
        if (end > quoted.length()) {
            return quoted.length();
        }
        // Don't break escaped Unicode characters.
        // Need to handle both e.g. \u4E00 and \U00020000
        for (int i = end - 1; i > end - 10;) {
            char current = quoted.charAt(i--);
            if (!Character.toString(current).matches("[0-9A-Fa-f]")) {
                if ((current == 'u' || current == 'U') && i > end - 10
                    && quoted.charAt(i) == '\\') {
                    return i;
                }
                break;
            }
        }
        while (end > 0) {
            char ch = quoted.charAt(end - 1);
            if (ch != '\\' && (ch < '\uD800' || ch > '\uDFFF')) {
                break;
            }
            --end;
        }
        return end;
    }

    // Fix characters inside strings.
    private static String quoteInside(String item) {
        // Unicode-escape all quotes.
        item = QUOTE_ESCAPE.matcher(item).replaceAll("\\\\u0022");
        // Double up on backslashes, ignoring Unicode-escaped characters.
        Pattern pattern =
            item.startsWith("[") && item.endsWith("]") ? UNICODESET_ESCAPE : STRING_ESCAPE;
        Matcher matcher = pattern.matcher(item);

        if (!matcher.find()) {
            return item;
        }
        StringBuilder buffer = new StringBuilder();
        int start = 0;
        do {
            buffer.append(item, start, matcher.start());
            int punctuationChar = item.codePointAt(matcher.end() - 1);
            buffer.append("\\");
            if (punctuationChar == '\\') {
                buffer.append('\\');
            }
            buffer.append(matcher.group());
            start = matcher.end();
        } while (matcher.find());
        buffer.append(item.substring(start));
        return buffer.toString();
    }
}
