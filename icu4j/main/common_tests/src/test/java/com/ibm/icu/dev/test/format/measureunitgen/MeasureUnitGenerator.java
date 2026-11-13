// © 2026 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.format.measureunitgen;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Updates all five generated ICU4J and ICU4C {@code MeasureUnit} files from a {@link
 * UnitDataSource}.
 */
public final class MeasureUnitGenerator {

    public static final String JAVA_CONSTANTS_PATH =
            "icu4j/main/core/src/main/java/com/ibm/icu/util/MeasureUnit.java";
    public static final String CPP_HEADER_PATH = "icu4c/source/i18n/unicode/measunit.h";
    public static final String CPP_SOURCE_PATH = "icu4c/source/i18n/measunit.cpp";
    public static final String JAVA_COMPAT_TEST_PATH =
            "icu4j/main/common_tests/src/test/java/com/ibm/icu/dev/test/format/"
                    + "MeasureUnitCompatibilityTest.java";
    public static final String CPP_COMPAT_TEST_PATH = "icu4c/source/test/intltest/measfmttest.cpp";

    public static final List<String> GENERATED_FILE_PATHS =
            List.of(
                    JAVA_CONSTANTS_PATH,
                    CPP_HEADER_PATH,
                    CPP_SOURCE_PATH,
                    JAVA_COMPAT_TEST_PATH,
                    CPP_COMPAT_TEST_PATH);

    private MeasureUnitGenerator() {}

    /** Computes the updated contents of all five generated files without writing to disk. */
    public static Map<String, FileChange> generate(
            Path icuRoot, UnitDataSource dataSource, int icuMajorVersion) throws IOException {
        BuiltInUnits units = BuiltInUnits.fromData(dataSource.readData());
        Map<String, FileChange> changes = new LinkedHashMap<>();

        changes.put(
                JAVA_CONSTANTS_PATH,
                computeChange(
                        icuRoot,
                        JAVA_CONSTANTS_PATH,
                        text ->
                                JavaMeasureUnitWriter.updateMeasureUnitJava(
                                        text, JAVA_CONSTANTS_PATH, units, icuMajorVersion)));
        changes.put(
                CPP_HEADER_PATH,
                computeChange(
                        icuRoot,
                        CPP_HEADER_PATH,
                        text ->
                                CppMeasureUnitWriter.updateMeasUnitHeader(
                                        text, CPP_HEADER_PATH, units, icuMajorVersion)));
        changes.put(
                CPP_SOURCE_PATH,
                computeChange(
                        icuRoot,
                        CPP_SOURCE_PATH,
                        text ->
                                CppMeasureUnitWriter.updateMeasUnitCpp(
                                        text, CPP_SOURCE_PATH, units)));
        changes.put(
                JAVA_COMPAT_TEST_PATH,
                computeChange(
                        icuRoot,
                        JAVA_COMPAT_TEST_PATH,
                        text ->
                                JavaMeasureUnitWriter.updateCompatibilityTestJava(
                                        text, JAVA_COMPAT_TEST_PATH, units, icuMajorVersion)));
        changes.put(
                CPP_COMPAT_TEST_PATH,
                computeChange(
                        icuRoot,
                        CPP_COMPAT_TEST_PATH,
                        text ->
                                CppMeasureUnitWriter.updateCompatibilityTestCpp(
                                        text, CPP_COMPAT_TEST_PATH, units, icuMajorVersion)));

        return Collections.unmodifiableMap(changes);
    }

    /** Writes any modified files in {@code changes} in place and returns their relative paths. */
    public static List<String> writeUpdatedFiles(Path icuRoot, Map<String, FileChange> changes)
            throws IOException {
        List<String> written = new ArrayList<>();
        for (FileChange change : changes.values()) {
            if (change.isChanged()) {
                Files.writeString(
                        icuRoot.resolve(change.getRelativePath()),
                        change.getOutputForDisk(),
                        StandardCharsets.UTF_8);
                written.add(change.getRelativePath());
            }
        }
        return written;
    }

    private static FileChange computeChange(Path icuRoot, String relativePath, TextUpdater updater)
            throws IOException {
        String raw = Files.readString(icuRoot.resolve(relativePath), StandardCharsets.UTF_8);
        boolean crlf = raw.contains("\r\n");
        String normalizedOld = crlf ? raw.replace("\r\n", "\n") : raw;
        String normalizedNew = updater.apply(normalizedOld);
        return new FileChange(relativePath, normalizedOld, normalizedNew, crlf);
    }

    @FunctionalInterface
    private interface TextUpdater {
        String apply(String content);
    }

    /** Before/after content of a single generated file. */
    public static final class FileChange {
        private final String relativePath;
        private final String oldContent;
        private final String newContent;
        private final boolean crlf;

        FileChange(String relativePath, String oldContent, String newContent, boolean crlf) {
            this.relativePath = relativePath;
            this.oldContent = oldContent;
            this.newContent = newContent;
            this.crlf = crlf;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public String getOldContent() {
            return oldContent;
        }

        public String getNewContent() {
            return newContent;
        }

        public boolean isChanged() {
            return !oldContent.equals(newContent);
        }

        String getOutputForDisk() {
            return crlf ? newContent.replace("\n", "\r\n") : newContent;
        }
    }
}
