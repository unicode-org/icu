// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format.measureunitgen;

import com.ibm.icu.dev.test.format.measureunitgen.BuiltInUnits.Alias;
import com.ibm.icu.dev.test.format.measureunitgen.BuiltInUnits.UnitConstant;
import com.ibm.icu.dev.test.format.measureunitgen.GeneratedFileUpdater.Region;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Generates the C++ built-in unit declarations in {@code measunit.h}, the tables and definitions in
 * {@code measunit.cpp}, and the compatibility test in {@code measfmttest.cpp}.
 */
public final class CppMeasureUnitWriter {

    private static final String HEADER_START_MARKER = "// Start generated createXXX methods";
    private static final String HEADER_END_MARKER = "// End generated createXXX methods";
    private static final String SOURCE_START_MARKER = "// Start generated code for measunit.cpp";
    private static final String SOURCE_END_MARKER = "// End generated code for measunit.cpp";
    private static final String BACKWARD_COMPAT_COMMENT = " // backward compatibility API";

    private static final Pattern CPP_DECLARATION =
            Pattern.compile(
                    "^    /\\*\\*\\n((?:     \\*.*\\n)*?)     \\*/\\n"
                            + "    static MeasureUnit "
                            + "(?:\\*(create\\w+)\\(UErrorCode &status\\)|(get\\w+)\\(\\));$",
                    Pattern.MULTILINE);

    private static final Pattern CPP_COMPAT_BLOCK =
            Pattern.compile(
                    "^void MeasureFormatTest::TestCompatible(\\d+)\\(\\) \\{.*?^\\}\\n",
                    Pattern.MULTILINE | Pattern.DOTALL);

    private static final Pattern CPP_COMPAT_SYMBOL =
            Pattern.compile("MeasureUnit::create(\\w+)\\(status\\)");

    private CppMeasureUnitWriter() {}

    public static String updateMeasUnitHeader(
            String currentContent, String path, BuiltInUnits units, int icuMajorVersion) {
        Region region =
                GeneratedFileUpdater.findRegion(
                        currentContent, HEADER_START_MARKER, HEADER_END_MARKER, path);
        Map<String, List<String>> existingTags =
                GeneratedFileUpdater.readApiTags(
                        region.getBody(), CPP_DECLARATION, "static MeasureUnit ", path);

        Set<String> oldCppNames = new LinkedHashSet<>();
        for (String method : existingTags.keySet()) {
            if (method.startsWith("create")) {
                oldCppNames.add(method.substring("create".length()));
            }
        }
        Set<String> newCppNames = new LinkedHashSet<>();
        for (UnitConstant c : units.getConstants()) {
            newCppNames.add(c.getCppName());
        }
        GeneratedFileUpdater.checkNoRemovedConstants(oldCppNames, newCppNames, path);

        List<String> lines = new ArrayList<>();
        lines.add("");
        for (UnitConstant c : units.getConstants()) {
            String createName = "create" + c.getCppName();
            String getName = "get" + c.getCppName();
            List<String> createTags =
                    GeneratedFileUpdater.tagsOrDraft(existingTags, createName, icuMajorVersion);
            List<String> getTags =
                    GeneratedFileUpdater.tagsOrDraft(existingTags, getName, icuMajorVersion);

            String macro = hidingMacro(createTags.get(0));
            if (!Objects.equals(macro, hidingMacro(getTags.get(0)))) {
                throw new IllegalStateException(
                        path
                                + ": "
                                + createName
                                + " and "
                                + getName
                                + " have mismatched API tags.");
            }

            if (macro != null) {
                lines.add("#ifndef " + macro);
            }
            lines.addAll(formatDoxygenComment(createDocLines(c), createTags));
            lines.add("    static MeasureUnit *" + createName + "(UErrorCode &status);");
            lines.add("");
            lines.addAll(formatDoxygenComment(getDocLines(c), getTags));
            lines.add("    static MeasureUnit " + getName + "();");
            if ("U_HIDE_DEPRECATED_API".equals(macro)) {
                lines.add("#endif  /* " + macro + " */");
            } else if (macro != null) {
                lines.add("#endif /* " + macro + " */");
            }
            lines.add("");
        }
        return region.replaceWith(lines);
    }

    public static String updateMeasUnitCpp(String currentContent, String path, BuiltInUnits units) {
        Region region =
                GeneratedFileUpdater.findRegion(
                        currentContent, SOURCE_START_MARKER, SOURCE_END_MARKER, path);

        Map<String, List<String>> sortedTypes = units.getSortedTypes();
        List<String> typeNames = new ArrayList<>(sortedTypes.keySet());
        List<String> allSubtypes = new ArrayList<>();
        for (List<String> subs : sortedTypes.values()) {
            allSubtypes.addAll(subs);
        }

        List<String> lines = new ArrayList<>();
        lines.add("");
        lines.add("// Maps from Type ID to offset in gSubTypes.");
        lines.add("static const int32_t gOffsets[] = {");
        int offset = 0;
        for (List<String> subs : sortedTypes.values()) {
            lines.add("    " + offset + ",");
            offset += subs.size();
        }
        lines.add("    " + offset);
        lines.add("};");
        lines.add("");
        lines.add(
                "static const int32_t kCurrencyOffset = "
                        + typeNames.indexOf(BuiltInUnits.CURRENCY_TYPE)
                        + ";");
        lines.add("");
        lines.add("// Must be sorted alphabetically.");
        lines.add("static const char * const gTypes[] = {");
        appendQuotedArrayElements(lines, typeNames);
        lines.add("};");
        lines.add("");
        lines.add("// Must be grouped by type and sorted alphabetically within each type.");
        lines.add("static const char * const gSubTypes[] = {");
        appendQuotedArrayElements(lines, allSubtypes);
        lines.add("};");
        lines.add("");
        lines.add("// Shortcuts to the base unit in order to make the default constructor fast");
        lines.add(
                "static const int32_t kBaseTypeIdx = "
                        + typeNames.indexOf(BuiltInUnits.NONE_TYPE)
                        + ";");
        lines.add("static const int32_t kBaseSubTypeIdx = 0;");
        lines.add("");

        for (UnitConstant c : units.getConstants()) {
            String indices = c.getTypeIndex() + ", " + c.getSubtypeIndex();
            lines.add(
                    "MeasureUnit *MeasureUnit::create" + c.getCppName() + "(UErrorCode &status) {");
            lines.add("    return MeasureUnit::create(" + indices + ", status);");
            lines.add("}");
            lines.add("");
            lines.add("MeasureUnit MeasureUnit::get" + c.getCppName() + "() {");
            lines.add("    return MeasureUnit(" + indices + ");");
            lines.add("}");
            lines.add("");
        }
        return region.replaceWith(lines);
    }

    public static String updateCompatibilityTestCpp(
            String currentContent, String path, BuiltInUnits units, int icuMajorVersion) {
        List<UnitConstant> constants = units.getConstants();
        List<String> cppNames = new ArrayList<>(constants.size());
        List<String> newTestLines = new ArrayList<>(constants.size() * 2 + 6);
        newTestLines.add("void MeasureFormatTest::TestCompatible" + icuMajorVersion + "() {");
        newTestLines.add("    UErrorCode status = U_ZERO_ERROR;");
        newTestLines.add("    LocalPointer<MeasureUnit> measureUnit;");
        newTestLines.add("    MeasureUnit measureUnitValue;");
        for (UnitConstant c : constants) {
            cppNames.add(c.getCppName());
            String suffix = c.getAlias() != null ? BACKWARD_COMPAT_COMMENT : "";
            newTestLines.add(
                    "    measureUnit.adoptInstead(MeasureUnit::create"
                            + c.getCppName()
                            + "(status));"
                            + suffix);
            newTestLines.add(
                    "    measureUnitValue = MeasureUnit::get" + c.getCppName() + "();" + suffix);
        }
        newTestLines.add("    assertSuccess(\"\", status);");
        newTestLines.add("}");

        return GeneratedFileUpdater.updateCompatibilityTest(
                currentContent,
                path,
                CPP_COMPAT_BLOCK,
                CPP_COMPAT_SYMBOL,
                newTestLines,
                cppNames,
                icuMajorVersion,
                List.of("    void TestCompatible%d();", "    TESTCASE_AUTO(TestCompatible%d);"));
    }

    private static void appendQuotedArrayElements(List<String> lines, List<String> values) {
        for (int i = 0; i < values.size(); i++) {
            String comma = (i + 1 < values.size()) ? "," : "";
            lines.add("    \"" + values.get(i) + "\"" + comma);
        }
    }

    private static List<String> createDocLines(UnitConstant c) {
        String name = c.getCppName();
        Alias alias = c.getAlias();
        if (alias == null) {
            return List.of(
                    "Returns by pointer, unit of " + c.getType() + ": " + c.getSubtype() + ".",
                    "Caller owns returned value and must free it.",
                    "Also see {@link #get" + name + "()}.",
                    "@param status ICU error code.");
        }
        String target = c.getTargetCppName();
        String renamed = "(renamed to " + c.getSubtype() + " in " + alias.getRenamedIn() + ").";
        if (alias.hasShortCppComment()) {
            return List.of(
                    "Returns by pointer, unit of " + c.getType() + ": " + alias.getOldUnit(),
                    renamed,
                    "Caller owns returned value and must free it.",
                    "Also see {@link #get" + name + "()} and {@link #create" + target + "()}.",
                    "@param status ICU error code.");
        }
        return List.of(
                "Returns by pointer, unit of " + c.getType() + ": " + alias.getOldUnit() + ".",
                renamed,
                "Caller owns returned value and must free it.",
                "Also see {@link #create" + target + "()}.",
                "Also see {@link #get" + name + "()}.",
                "@param status ICU error code.");
    }

    private static List<String> getDocLines(UnitConstant c) {
        String name = c.getCppName();
        Alias alias = c.getAlias();
        if (alias == null) {
            return List.of(
                    "Returns by value, unit of " + c.getType() + ": " + c.getSubtype() + ".",
                    "Also see {@link #create" + name + "()}.");
        }
        String target = c.getTargetCppName();
        String renamed = "(renamed to " + c.getSubtype() + " in " + alias.getRenamedIn() + ").";
        if (alias.hasShortCppComment()) {
            return List.of(
                    "Returns by value, unit of " + c.getType() + ": " + alias.getOldUnit(),
                    renamed,
                    "Also see {@link #create" + name + "()} and {@link #get" + target + "()}.");
        }
        return List.of(
                "Returns by value, unit of " + c.getType() + ": " + alias.getOldUnit() + ".",
                renamed,
                "Also see {@link #get" + target + "()}.",
                "Also see {@link #create" + name + "()}.");
    }

    private static List<String> formatDoxygenComment(List<String> textLines, List<String> tags) {
        List<String> result = new ArrayList<>(textLines.size() + tags.size() + 2);
        result.add("    /**");
        for (String line : textLines) {
            result.add(line.isEmpty() ? "     *" : "     * " + line);
        }
        for (String tag : tags) {
            result.add(tag.isEmpty() ? "     *" : "     * " + tag);
        }
        result.add("     */");
        return result;
    }

    private static String hidingMacro(String firstTagLine) {
        int space = firstTagLine.indexOf(' ');
        String tag = (space == -1) ? firstTagLine : firstTagLine.substring(0, space);
        switch (tag) {
            case "@stable":
                return null;
            case "@draft":
                return "U_HIDE_DRAFT_API";
            case "@deprecated":
                return "U_HIDE_DEPRECATED_API";
            default:
                throw new IllegalStateException("Unexpected API tag: " + firstTagLine);
        }
    }
}
