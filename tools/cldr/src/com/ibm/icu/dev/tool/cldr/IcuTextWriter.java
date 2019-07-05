package com.ibm.icu.dev.tool.cldr;

import com.google.common.base.Strings;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Writes an IcuData object to a text file.
 */
final class IcuTextWriter {
    private static final String INDENT = "    ";
    // List of characters to escape in UnicodeSets
    // ('\' followed by any of '\', '[', ']', '{', '}', '-', '&', ':', '^', '=').
    private static final Pattern UNICODESET_ESCAPE =
        Pattern.compile("\\\\[\\\\\\[\\]\\{\\}\\-&:^=]");
    // Only escape \ and " from other strings.
    private static final Pattern STRING_ESCAPE = Pattern.compile("(?!')\\\\\\\\(?!')");
    private static final Pattern QUOTE_ESCAPE = Pattern.compile("\\\\?\"");

    /**
     * ICU paths have a simple comparison, alphabetical within a level. We do
     * have to catch the / so that it is lower than everything.
     */
    public static final Comparator<String> PATH_COMPARATOR = new Comparator<String>() {
        @Override
        public int compare(String arg0, String arg1) {
            int min = Math.min(arg0.length(), arg1.length());
            for (int i = 0; i < min; ++i) {
                int ch0 = arg0.charAt(i);
                int ch1 = arg1.charAt(i);
                int diff = ch0 - ch1;
                if (diff == 0) {
                    continue;
                }
                if (ch0 == '/') {
                    return -1;
                } else if (ch1 == '/') {
                    return 1;
                }
                // make * greater than everything, because of languageMatch
                // while it is a pain to have it be unordered, this fix is sufficient to put all the *'s after anything else
                if (ch0 == '*') {
                    return 1;
                } else if (ch1 == '*') {
                    return -1;
                }
                return diff;
            }
            return arg0.length() - arg1.length();
        }
    };

    private static String getHeader() throws IOException {
        Path header =
            PathUtils.getPackageDirectoryFor(IcuTextWriter.class).resolve("ldml2icu_header.txt");
        return Files.readAllLines(header).stream().collect(Collectors.joining("\n", "", "\n"));
    }

    /**
     * Write a file in ICU format. LDML2ICUConverter currently has some funny formatting in a few
     * cases; don't try to match everything.
     */
    public static void writeToFile(IcuData icuData, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        try (Writer w = Files.newBufferedWriter(outDir.resolve(icuData.getName() + ".txt"));
            PrintWriter out = new PrintWriter(w)) {
            write(icuData, out);
        }
    }

    public static void write(IcuData icuData, PrintWriter out) throws IOException {
        out.write('\uFEFF');
        // Append the header.
        String header = getHeader();
        out.print(header);
        if (icuData.getFileComment() != null) {
            out.println("/**");
            out.append(" * ").append(icuData.getFileComment()).println();
            out.println(" */");
        }

        // Write the ICU data to file.
        out.append(icuData.getName());
        if (!icuData.hasFallback()) out.append(":table(nofallback)");
        List<String> sortedPaths = new ArrayList<String>(icuData.keySet());
        Collections.sort(sortedPaths, PATH_COMPARATOR);
        String[] lastLabels = new String[] {};
        boolean wasSingular = false;
        for (String path : sortedPaths) {
            // Write values to file.
            String[] labels = path.split("/", -1); // Don't discard trailing slashes.
            int common = getCommon(lastLabels, labels);
            for (int i = lastLabels.length - 1; i > common; --i) {
                if (wasSingular) {
                    wasSingular = false;
                } else {
                    out.append(Strings.repeat(INDENT, i));
                }
                out.println("}");
            }
            for (int i = common + 1; i < labels.length; ++i) {
                final String pad = Strings.repeat(INDENT, i);
                out.append(pad);
                String label = labels[i];
                if (!label.startsWith("<") && !label.endsWith(">")) {
                    out.append(label);
                }
                out.append('{');
                if (i != labels.length - 1) {
                    out.println();
                }
            }
            List<String[]> values = icuData.get(path);
            try {
                wasSingular = appendValues(icuData.getName(), path, values, labels.length, out);
            } catch (NullPointerException npe) {
                System.err.println("Null value encountered in " + path);
            }
            out.flush();
            lastLabels = labels;
        }
        // Add last closing braces.
        for (int i = lastLabels.length - 1; i > 0; --i) {
            if (wasSingular) {
                wasSingular = false;
            } else {
                out.append(Strings.repeat(INDENT, i));
            }
            out.println("}");
        }
        out.println("}");
        out.close();
    }

    /** Inserts padding and values between braces. */
    private static boolean appendValues(String name, String rbPath, List<String[]> values,
        int numTabs,
        PrintWriter out) {
        String[] firstArray;
        boolean wasSingular = false;
        boolean quote = !IcuData.isIntRbPath(rbPath);
        boolean isSequence = rbPath.endsWith("/Sequence");
        if (values.size() == 1 && !mustBeArray(true, name, rbPath)) {
            if ((firstArray = values.get(0)).length == 1 && !mustBeArray(false, name, rbPath)) {
                String value = firstArray[0];
                if (quote) {
                    value = quoteInside(value);
                }
                int maxWidth = 84 - Math.min(4, numTabs) * INDENT.length();
                if (value.length() <= maxWidth) {
                    // Single value for path: don't add newlines.
                    appendValue(value, quote, out);
                    wasSingular = true;
                } else {
                    // Value too long to fit in one line, so wrap.
                    final String pad = Strings.repeat(INDENT, numTabs);
                    out.println();
                    int end;
                    for (int i = 0; i < value.length(); i = end) {
                        end = goodBreak(value, i + maxWidth);
                        String part = value.substring(i, end);
                        out.append(pad);
                        appendValue(part, quote, out).println();
                    }
                }
            } else {
                // Only one array for the rbPath, so don't add an extra set of braces.
                final String pad = Strings.repeat(INDENT, numTabs);
                out.println();
                appendArray(pad, firstArray, quote, isSequence, out);
            }
        } else {
            final String pad = Strings.repeat(INDENT, numTabs);
            out.println();
            for (String[] valueArray : values) {
                if (valueArray.length == 1) {
                    // Single-value array: print normally.
                    appendArray(pad, valueArray, quote, isSequence, out);
                } else {
                    // Enclose this array in braces to separate it from other values.
                    out.append(pad).println("{");
                    appendArray(pad + INDENT, valueArray, quote, isSequence, out);
                    out.append(pad).println("}");
                }
            }
        }
        return wasSingular;
    }

    /**
     * Wrapper for a hack to determine if the given rb path should always
     * present its values as an array. This hack is required for an ICU data test to pass.
     */
    private static boolean mustBeArray(boolean topValues, String name, String rbPath) {
        // TODO(jchye): Add this as an option to the locale file instead of hardcoding.
        // System.out.println(name + "\t" + rbPath);
        if (topValues) {
            return (rbPath.startsWith("/rules/set")
                && name.equals("pluralRanges"));
        }
        return rbPath.equals("/LocaleScript")
            || (rbPath.contains("/eras/") && !rbPath.endsWith(":alias")
                && !rbPath.endsWith("/named"))
            || rbPath.startsWith("/calendarPreferenceData")
            || rbPath.startsWith("/metazoneInfo");
    }

    private static PrintWriter appendArray(String padding, String[] valueArray,
        boolean quote, boolean isSequence, PrintWriter out) {
        for (String value : valueArray) {
            out.append(padding);
            appendValue(quoteInside(value), quote, out);
            if (!isSequence) {
                out.print(",");
            }
            out.println();
        }
        return out;
    }

    private static PrintWriter appendValue(String value, boolean quote, PrintWriter out) {
        if (quote) {
            return out.append('"').append(value).append('"');
        } else {
            return out.append(value);
        }
    }

    // Can a string be broken here? If not, backup until we can.
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
        Pattern pattern = item.startsWith("[") && item.endsWith("]") ? UNICODESET_ESCAPE
            : STRING_ESCAPE;
        Matcher matcher = pattern.matcher(item);

        if (!matcher.find()) {
            return item;
        }
        StringBuilder buffer = new StringBuilder();
        int start = 0;
        do {
            buffer.append(item.substring(start, matcher.start()));
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

    // Find the initial labels (from a path) that are identical.
    private static int getCommon(String[] lastLabels, String[] labels) {
        int min = Math.min(lastLabels.length, labels.length);
        int i;
        for (i = 0; i < min; ++i) {
            if (!lastLabels[i].equals(labels[i])) {
                return i - 1;
            }
        }
        return i - 1;
    }
}
