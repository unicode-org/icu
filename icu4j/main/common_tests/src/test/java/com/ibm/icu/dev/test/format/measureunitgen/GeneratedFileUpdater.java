// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format.measureunitgen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Updates marked generated-code regions in source files and reads existing API status tags so
 * {@code @stable}, {@code @draft}, and {@code @deprecated} annotations are preserved automatically.
 */
public final class GeneratedFileUpdater {

    private GeneratedFileUpdater() {}

    /** Locates the text between unique {@code startMarker} and {@code endMarker} lines. */
    public static Region findRegion(
            String content, String startMarker, String endMarker, String path) {
        int startLine = findSingleMarkerLine(content, startMarker, path);
        int endLine = findSingleMarkerLine(content, endMarker, path);
        int regionStart = startLine + startMarker.length() + 1;
        if (regionStart > endLine) {
            throw new IllegalStateException(path + ": generated region markers are out of order.");
        }
        return new Region(
                content.substring(0, regionStart),
                content.substring(regionStart, endLine),
                content.substring(endLine));
    }

    private static int findSingleMarkerLine(String content, String marker, String path) {
        Pattern linePattern = Pattern.compile("^" + Pattern.quote(marker) + "$", Pattern.MULTILINE);
        Matcher matcher = linePattern.matcher(content);
        List<Integer> matches = new ArrayList<>();
        while (matcher.find()) {
            matches.add(matcher.start());
        }
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    path
                            + ": expected exactly 1 marker line '"
                            + marker
                            + "', found "
                            + matches.size()
                            + ".");
        }
        return matches.get(0);
    }

    /**
     * Parses API tags ({@code @stable}, {@code @draft}, {@code @deprecated}, etc., skipping {@code
     * @param}) from each declaration in {@code regionText}.
     */
    public static Map<String, List<String>> readApiTags(
            String regionText,
            Pattern declarationPattern,
            String expectedDeclarationPrefix,
            String path) {
        Map<String, List<String>> tagsBySymbol = new LinkedHashMap<>();
        Matcher matcher = declarationPattern.matcher(regionText);
        while (matcher.find()) {
            String commentBody = matcher.group(1);
            String name = matcher.group(2);
            if (name == null && matcher.groupCount() >= 3) {
                name = matcher.group(3);
            }
            tagsBySymbol.put(name, extractApiTagLines(commentBody, path));
        }

        int declarationCount = countOccurrences(regionText, expectedDeclarationPrefix);
        if (tagsBySymbol.size() != declarationCount) {
            throw new IllegalStateException(
                    path
                            + ": parsed API tags for "
                            + tagsBySymbol.size()
                            + " declarations, expected "
                            + declarationCount
                            + ".");
        }
        return tagsBySymbol;
    }

    private static List<String> extractApiTagLines(String commentBody, String path) {
        String[] rawLines = commentBody.split("\n", -1);
        // Trailing newline produces one empty trailing element; ignore it.
        int limit = rawLines[rawLines.length - 1].isEmpty() ? rawLines.length - 1 : rawLines.length;
        List<String> stripped = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            String line = rawLines[i];
            stripped.add(line.length() > 7 ? line.substring(7) : "");
        }
        for (int i = 0; i < stripped.size(); i++) {
            String line = stripped.get(i);
            if (line.startsWith("@") && !line.startsWith("@param")) {
                return List.copyOf(stripped.subList(i, stripped.size()));
            }
        }
        throw new IllegalStateException(
                path + ": doc comment has no API status tag:\n" + commentBody);
    }

    /** Returns the existing API tags for {@code name}, or {@code @draft ICU <major>} if new. */
    public static List<String> tagsOrDraft(
            Map<String, List<String>> existingTags, String name, int icuMajorVersion) {
        List<String> tags = existingTags.get(name);
        if (tags != null) {
            return tags;
        }
        return List.of("@draft ICU " + icuMajorVersion);
    }

    /** Fails before writing if any existing API constant would disappear from {@code path}. */
    public static void checkNoRemovedConstants(
            Set<String> existingNames, Set<String> newNames, String path) {
        Set<String> removed = new TreeSet<>(existingNames);
        removed.removeAll(newNames);
        if (!removed.isEmpty()) {
            throw new IllegalStateException(
                    path
                            + ": refusing to remove existing constants "
                            + removed
                            + " whose units were dropped from the unit data. Keep them with"
                            + " ALIASES or NAME_OVERRIDES in BuiltInUnits, or delete them manually"
                            + " first if they were still @draft.");
        }
    }

    /**
     * Adds or updates {@code TestCompatible<icuMajorVersion>()} only when the list of constants
     * differs from the newest existing {@code TestCompatible<N>()} method.
     */
    public static String updateCompatibilityTest(
            String content,
            String path,
            Pattern testBlockPattern,
            Pattern usedSymbolPattern,
            List<String> newTestLines,
            List<String> expectedSymbolOrder,
            int icuMajorVersion,
            List<String> extraLineTemplates) {
        Matcher matcher = testBlockPattern.matcher(content);
        int newestVersion = -1;
        int newestStart = -1;
        int newestEnd = -1;
        String newestBlock = null;
        while (matcher.find()) {
            int v = Integer.parseInt(matcher.group(1));
            if (v > newestVersion) {
                newestVersion = v;
                newestStart = matcher.start();
                newestEnd = matcher.end();
                newestBlock = matcher.group(0);
            }
        }
        if (newestBlock == null) {
            throw new IllegalStateException(path + ": found no TestCompatible methods.");
        }
        if (newestVersion > icuMajorVersion) {
            throw new IllegalStateException(
                    path
                            + ": found TestCompatible"
                            + newestVersion
                            + " newer than current ICU version "
                            + icuMajorVersion
                            + ".");
        }

        List<String> currentSymbols = new ArrayList<>();
        Matcher symbolMatcher = usedSymbolPattern.matcher(newestBlock);
        while (symbolMatcher.find()) {
            currentSymbols.add(symbolMatcher.group(1));
        }
        if (currentSymbols.equals(expectedSymbolOrder)) {
            return content;
        }

        String renderedTest = joinLines(newTestLines);
        if (newestVersion == icuMajorVersion) {
            return content.substring(0, newestStart) + renderedTest + content.substring(newestEnd);
        }

        String updated =
                content.substring(0, newestEnd)
                        + "\n"
                        + renderedTest
                        + content.substring(newestEnd);
        for (String template : extraLineTemplates) {
            String previousLine = String.format(template, newestVersion) + "\n";
            if (countOccurrences(updated, previousLine) != 1) {
                throw new IllegalStateException(
                        path + ": expected exactly one line matching: " + previousLine.trim());
            }
            String addedLine = String.format(template, icuMajorVersion) + "\n";
            updated = updated.replace(previousLine, previousLine + addedLine);
        }
        return updated;
    }

    public static String joinLines(List<String> lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) != -1) {
            count++;
            index += needle.length();
        }
        return count;
    }

    /** Split source file around a generated-code region. */
    public static final class Region {
        private final String before;
        private final String body;
        private final String after;

        Region(String before, String body, String after) {
            this.before = before;
            this.body = body;
            this.after = after;
        }

        public String getBody() {
            return body;
        }

        public String replaceWith(List<String> newBodyLines) {
            return before + joinLines(newBodyLines) + after;
        }
    }
}
