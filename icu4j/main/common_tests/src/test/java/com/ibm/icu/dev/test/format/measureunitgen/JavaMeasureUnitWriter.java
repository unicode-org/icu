// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format.measureunitgen;

import com.ibm.icu.dev.test.format.measureunitgen.BuiltInUnits.Alias;
import com.ibm.icu.dev.test.format.measureunitgen.BuiltInUnits.UnitConstant;
import com.ibm.icu.dev.test.format.measureunitgen.GeneratedFileUpdater.Region;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Generates the {@code MeasureUnit.java} constant block and the {@code
 * MeasureUnitCompatibilityTest.java} test method, formatted to match Spotless ({@code
 * google-java-format} AOSP, 100 columns).
 */
public final class JavaMeasureUnitWriter {

    private static final String START_MARKER = "    // Start generated MeasureUnit constants";
    private static final String END_MARKER = "    // End generated MeasureUnit constants";
    private static final int JAVA_LINE_LIMIT = 100;
    private static final String BACKWARD_COMPAT_COMMENT = " // backward compatibility API";

    private static final Pattern JAVA_DECLARATION =
            Pattern.compile(
                    "^    /\\*\\*\\n((?:     \\*.*\\n)*?)     \\*/\\n"
                            + "(?:    @Deprecated\\n)?"
                            + "    public static final (?:MeasureUnit|TimeUnit) (\\w+) =",
                    Pattern.MULTILINE);

    private static final Pattern JAVA_COMPAT_BLOCK =
            Pattern.compile(
                    "^    @Test\\n    public void TestCompatible(\\d+)\\(\\) \\{.*?^    \\}\\n",
                    Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern JAVA_COMPAT_SYMBOL = Pattern.compile("MeasureUnit\\.(\\w+),");

    private JavaMeasureUnitWriter() {}

    public static String updateMeasureUnitJava(
            String currentContent, String path, BuiltInUnits units, int icuMajorVersion) {
        Region region =
                GeneratedFileUpdater.findRegion(currentContent, START_MARKER, END_MARKER, path);
        Map<String, List<String>> existingTags =
                GeneratedFileUpdater.readApiTags(
                        region.getBody(), JAVA_DECLARATION, "public static final ", path);

        Set<String> newJavaNames = new LinkedHashSet<>();
        for (UnitConstant c : units.getConstants()) {
            newJavaNames.add(c.getJavaName());
        }
        GeneratedFileUpdater.checkNoRemovedConstants(existingTags.keySet(), newJavaNames, path);

        List<String> lines = new ArrayList<>();
        lines.add("");
        for (UnitConstant c : units.getConstants()) {
            List<String> tags =
                    GeneratedFileUpdater.tagsOrDraft(
                            existingTags, c.getJavaName(), icuMajorVersion);
            String summary = formatDocSummary(c);
            lines.addAll(formatJavadocComment(summary, tags));
            if (hasDeprecatedTag(tags)) {
                lines.add("    @Deprecated");
            }
            String call = "MeasureUnit.internalGetInstance";
            List<String> args = List.of("\"" + c.getType() + "\"", "\"" + c.getSubtype() + "\"");
            if (c.isTimeUnit()) {
                lines.addAll(formatField("TimeUnit", c.getJavaName(), "(TimeUnit) " + call, args));
            } else {
                lines.addAll(formatField("MeasureUnit", c.getJavaName(), call, args));
            }
            lines.add("");
        }
        return region.replaceWith(lines);
    }

    public static String updateCompatibilityTestJava(
            String currentContent, String path, BuiltInUnits units, int icuMajorVersion) {
        List<UnitConstant> constants = units.getConstants();
        List<String> javaNames = new ArrayList<>(constants.size());
        List<String> newTestLines = new ArrayList<>(constants.size() + 6);
        newTestLines.add("    @Test");
        newTestLines.add("    public void TestCompatible" + icuMajorVersion + "() {");
        newTestLines.add("        MeasureUnit[] units = {");
        for (UnitConstant c : constants) {
            javaNames.add(c.getJavaName());
            String suffix = c.getAlias() != null ? BACKWARD_COMPAT_COMMENT : "";
            newTestLines.add("            MeasureUnit." + c.getJavaName() + "," + suffix);
        }
        newTestLines.add("        };");
        newTestLines.add("        assertEquals(\"\", " + constants.size() + ", units.length);");
        newTestLines.add("    }");

        return GeneratedFileUpdater.updateCompatibilityTest(
                currentContent,
                path,
                JAVA_COMPAT_BLOCK,
                JAVA_COMPAT_SYMBOL,
                newTestLines,
                javaNames,
                icuMajorVersion,
                Collections.emptyList());
    }

    private static String formatDocSummary(UnitConstant c) {
        Alias alias = c.getAlias();
        if (alias != null) {
            return "Constant for unit of "
                    + c.getType()
                    + ": "
                    + alias.getOldUnit()
                    + " (renamed to "
                    + c.getSubtype()
                    + " in "
                    + alias.getRenamedIn()
                    + ").";
        }
        return "Constant for unit of " + c.getType() + ": " + c.getSubtype();
    }

    private static List<String> formatJavadocComment(String summary, List<String> tags) {
        int maxTextWidth = JAVA_LINE_LIMIT - "     * ".length();
        List<String> body = new ArrayList<>(wrapWords(summary, maxTextWidth));
        body.add("");
        body.addAll(tags);

        List<String> result = new ArrayList<>(body.size() + 2);
        result.add("    /**");
        for (String line : body) {
            result.add(line.isEmpty() ? "     *" : "     * " + line);
        }
        result.add("     */");
        return result;
    }

    private static List<String> wrapWords(String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split(" ")) {
            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() <= maxWidth) {
                current.append(' ').append(word);
            } else {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            }
        }
        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private static List<String> formatField(
            String javaType, String name, String call, List<String> args) {
        String head = "    public static final " + javaType + " " + name + " =";
        String joinedArgs = String.join(", ", args);
        String value = call + "(" + joinedArgs + ");";
        String indent = "            ";

        List<List<String>> candidates =
                List.of(
                        List.of(head + " " + value),
                        List.of(head, indent + value),
                        List.of(
                                head,
                                indent + call + "(",
                                indent + "        " + joinedArgs + ");"));
        for (List<String> candidate : candidates) {
            if (fitsLineLimit(candidate)) {
                return candidate;
            }
        }

        List<String> multiLine = new ArrayList<>(args.size() + 2);
        multiLine.add(head);
        multiLine.add(indent + call + "(");
        for (int i = 0; i < args.size() - 1; i++) {
            multiLine.add(indent + "        " + args.get(i) + ",");
        }
        multiLine.add(indent + "        " + args.get(args.size() - 1) + ");");
        return multiLine;
    }

    private static boolean fitsLineLimit(List<String> lines) {
        for (String line : lines) {
            if (line.length() > JAVA_LINE_LIMIT) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasDeprecatedTag(List<String> tags) {
        for (String tag : tags) {
            if (tag.startsWith("@deprecated")) {
                return true;
            }
        }
        return false;
    }
}
