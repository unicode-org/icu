// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2013-2016, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.dev.test.format.measureunitgen.IcuResourceUnitDataSource;
import com.ibm.icu.dev.test.format.measureunitgen.MeasureUnitGenerator;
import com.ibm.icu.dev.test.format.measureunitgen.MeasureUnitGenerator.FileChange;
import com.ibm.icu.dev.test.format.measureunitgen.UnitDataSource;
import com.ibm.icu.util.VersionInfo;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Entry point and verification tests for updating the generated Java and C++ {@code MeasureUnit}
 * built-in unit constants and compatibility tests.
 *
 * <p>Run with {@code -DgenerateMeasureUnitUpdate} to update all five generated files in place, or
 * {@code -DcheckMeasureUnitUpdate} to fail if any generated file is out of date. See {@code
 * docs/processes/release/tasks/updating-measure-unit.md} for details.
 */
@RunWith(JUnit4.class)
public class MeasureUnitGeneratorTest extends CoreTestFmwk {

    @Test
    public void generateUnitTestsUpdate() throws IOException {
        boolean writeUpdate = System.getProperty("generateMeasureUnitUpdate") != null;
        boolean checkOnly = System.getProperty("checkMeasureUnitUpdate") != null;
        if (!writeUpdate && !checkOnly) {
            return;
        }

        Path icuRoot = requireIcuRoot();
        int icuMajorVersion = VersionInfo.ICU_VERSION.getMajor();
        Map<String, FileChange> changes =
                MeasureUnitGenerator.generate(
                        icuRoot, new IcuResourceUnitDataSource(), icuMajorVersion);

        if (checkOnly) {
            List<String> stale = new ArrayList<>();
            for (FileChange change : changes.values()) {
                if (change.isChanged()) {
                    stale.add(change.getRelativePath());
                    System.out.println("Out of date: " + change.getRelativePath());
                } else {
                    System.out.println("Up to date:  " + change.getRelativePath());
                }
            }
            if (!stale.isEmpty()) {
                fail(
                        "Generated MeasureUnit files are out of date: "
                                + stale
                                + ". Run with -DgenerateMeasureUnitUpdate to update them.");
            }
            return;
        }

        List<String> written = MeasureUnitGenerator.writeUpdatedFiles(icuRoot, changes);
        for (FileChange change : changes.values()) {
            String status = change.isChanged() ? "Updated:   " : "Unchanged: ";
            System.out.println(status + change.getRelativePath());
        }
        System.out.println(
                "MeasureUnit generation finished (" + written.size() + " file(s) updated).");
    }

    @Test
    public void testGeneratedFilesMatchCurrentData() throws IOException {
        Path icuRoot = findIcuRootOrNull();
        Assume.assumeTrue(
                "Skipping full-repo check because ICU root files are not present.",
                icuRoot != null && hasAllTargetFiles(icuRoot));

        int icuMajorVersion = VersionInfo.ICU_VERSION.getMajor();
        Map<String, FileChange> changes =
                MeasureUnitGenerator.generate(
                        icuRoot, new IcuResourceUnitDataSource(), icuMajorVersion);

        List<String> mismatched = new ArrayList<>();
        for (FileChange change : changes.values()) {
            if (change.isChanged()) {
                mismatched.add(change.getRelativePath());
            }
        }
        assertEquals(
                "Checked-in MeasureUnit files should match MeasureUnitGenerator output",
                List.of(),
                mismatched);
    }

    @Test
    public void testAddNewUnitUpdatesAllFiveFilesAndPreservesApiTags() throws IOException {
        Path icuRoot = findIcuRootOrNull();
        Assume.assumeTrue(
                "Skipping full-repo simulation because ICU root files are not present.",
                icuRoot != null && hasAllTargetFiles(icuRoot));

        Path tempRoot = copyTargetFilesToTempDir(icuRoot);
        try {
            UnitDataSource.Data baseData = new IcuResourceUnitDataSource().readData();
            Map<String, Set<String>> augmentedUnits = new TreeMap<>();
            for (Map.Entry<String, Set<String>> entry : baseData.getUnitsByType().entrySet()) {
                augmentedUnits.put(entry.getKey(), new TreeSet<>(entry.getValue()));
            }
            augmentedUnits.get("length").add("smoot");
            UnitDataSource augmentedSource =
                    () -> new UnitDataSource.Data(augmentedUnits, baseData.getCurrencyCodes());

            Map<String, FileChange> firstRun =
                    MeasureUnitGenerator.generate(tempRoot, augmentedSource, 80);
            List<String> updated = MeasureUnitGenerator.writeUpdatedFiles(tempRoot, firstRun);
            assertEquals(
                    "Adding a new unit should update all five generated files",
                    MeasureUnitGenerator.GENERATED_FILE_PATHS,
                    updated);

            String javaConstants =
                    Files.readString(
                            tempRoot.resolve(MeasureUnitGenerator.JAVA_CONSTANTS_PATH),
                            StandardCharsets.UTF_8);
            assertTrue(
                    "New Java constant SMOOT should be @draft ICU 80",
                    javaConstants.contains(
                            "     * Constant for unit of length: smoot\n"
                                    + "     *\n"
                                    + "     * @draft ICU 80\n"
                                    + "     */\n"
                                    + "    public static final MeasureUnit SMOOT = "
                                    + "MeasureUnit.internalGetInstance(\"length\", \"smoot\");"));
            // Existing @stable, @draft, and @deprecated tags must stay untouched.
            assertTrue(
                    "NIGHT must stay @stable ICU 76",
                    javaConstants.contains(
                            "     * Constant for unit of duration: night\n"
                                    + "     *\n"
                                    + "     * @stable ICU 76"));
            assertTrue(
                    "MILLIINCH must stay @draft ICU 79",
                    javaConstants.contains(
                            "     * Constant for unit of length: milliinch\n"
                                    + "     *\n"
                                    + "     * @draft ICU 79"));
            assertTrue(
                    "METRIC_TON must stay @deprecated ICU 78",
                    javaConstants.contains("     * @deprecated ICU 78 use TONNE"));

            String cppHeader =
                    Files.readString(
                            tempRoot.resolve(MeasureUnitGenerator.CPP_HEADER_PATH),
                            StandardCharsets.UTF_8);
            assertTrue(
                    "New C++ methods must be wrapped in U_HIDE_DRAFT_API",
                    cppHeader.contains(
                            "#ifndef U_HIDE_DRAFT_API\n"
                                    + "    /**\n"
                                    + "     * Returns by pointer, unit of length: smoot.\n"
                                    + "     * Caller owns returned value and must free it.\n"
                                    + "     * Also see {@link #getSmoot()}.\n"
                                    + "     * @param status ICU error code.\n"
                                    + "     * @draft ICU 80\n"
                                    + "     */\n"
                                    + "    static MeasureUnit *createSmoot(UErrorCode &status);\n"
                                    + "\n"
                                    + "    /**\n"
                                    + "     * Returns by value, unit of length: smoot.\n"
                                    + "     * Also see {@link #createSmoot()}.\n"
                                    + "     * @draft ICU 80\n"
                                    + "     */\n"
                                    + "    static MeasureUnit getSmoot();\n"
                                    + "#endif /* U_HIDE_DRAFT_API */"));

            String javaCompat =
                    Files.readString(
                            tempRoot.resolve(MeasureUnitGenerator.JAVA_COMPAT_TEST_PATH),
                            StandardCharsets.UTF_8);
            assertTrue(
                    "Java compatibility test should gain TestCompatible80",
                    javaCompat.contains("public void TestCompatible80()"));
            assertTrue(
                    "TestCompatible80 should reference MeasureUnit.SMOOT",
                    javaCompat.contains("MeasureUnit.SMOOT,"));

            String cppCompat =
                    Files.readString(
                            tempRoot.resolve(MeasureUnitGenerator.CPP_COMPAT_TEST_PATH),
                            StandardCharsets.UTF_8);
            assertTrue(
                    "C++ test header should declare TestCompatible80",
                    cppCompat.contains("    void TestCompatible80();"));
            assertTrue(
                    "C++ test runner should register TestCompatible80",
                    cppCompat.contains("    TESTCASE_AUTO(TestCompatible80);"));
            assertTrue(
                    "C++ test body should define TestCompatible80",
                    cppCompat.contains("void MeasureFormatTest::TestCompatible80()"));

            // Running a second time with the same data must be a no-op.
            Map<String, FileChange> secondRun =
                    MeasureUnitGenerator.generate(tempRoot, augmentedSource, 80);
            for (FileChange change : secondRun.values()) {
                assertFalse(
                        "Second run should not modify " + change.getRelativePath(),
                        change.isChanged());
            }
        } finally {
            deleteRecursively(tempRoot);
        }
    }

    @Test
    public void testRemovingUnitWithExistingConstantFailsWithoutWriting() throws IOException {
        Path icuRoot = findIcuRootOrNull();
        Assume.assumeTrue(
                "Skipping full-repo removal check because ICU root files are not present.",
                icuRoot != null && hasAllTargetFiles(icuRoot));

        UnitDataSource.Data baseData = new IcuResourceUnitDataSource().readData();
        Map<String, Set<String>> reducedUnits = new TreeMap<>();
        for (Map.Entry<String, Set<String>> entry : baseData.getUnitsByType().entrySet()) {
            reducedUnits.put(entry.getKey(), new TreeSet<>(entry.getValue()));
        }
        reducedUnits.get("length").remove("milliinch");
        UnitDataSource reducedSource =
                () -> new UnitDataSource.Data(reducedUnits, baseData.getCurrencyCodes());

        try {
            MeasureUnitGenerator.generate(icuRoot, reducedSource, 80);
            fail("Expected IllegalStateException when an existing constant's unit is removed.");
        } catch (IllegalStateException expected) {
            assertTrue(
                    "Error message should name MILLIINCH: " + expected.getMessage(),
                    expected.getMessage().contains("MILLIINCH"));
        }
    }

    private static Path requireIcuRoot() {
        Path root = findIcuRootOrNull();
        if (root == null || !hasAllTargetFiles(root)) {
            throw new IllegalStateException(
                    "Could not locate ICU repository root. Pass -DicuRoot=/path/to/icu.");
        }
        return root;
    }

    private static Path findIcuRootOrNull() {
        String explicit = System.getProperty("icuRoot");
        if (explicit != null && !explicit.isEmpty()) {
            return Paths.get(explicit).toAbsolutePath().normalize();
        }
        URL resource = MeasureUnitGeneratorTest.class.getResource(".");
        if (resource != null && "file".equals(resource.getProtocol())) {
            try {
                for (Path dir = Paths.get(resource.toURI()); dir != null; dir = dir.getParent()) {
                    if (hasAllTargetFiles(dir)) {
                        return dir;
                    }
                }
            } catch (URISyntaxException ignored) {
                // Fall back to user.dir search below.
            }
        }
        Path cwd = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (Path dir = cwd; dir != null; dir = dir.getParent()) {
            if (hasAllTargetFiles(dir)) {
                return dir;
            }
        }
        return null;
    }

    private static boolean hasAllTargetFiles(Path root) {
        for (String rel : MeasureUnitGenerator.GENERATED_FILE_PATHS) {
            if (!Files.isRegularFile(root.resolve(rel))) {
                return false;
            }
        }
        return true;
    }

    private static Path copyTargetFilesToTempDir(Path icuRoot) throws IOException {
        Path tempRoot = Files.createTempDirectory("icu-measureunit-gen-test");
        for (String rel : MeasureUnitGenerator.GENERATED_FILE_PATHS) {
            Path dest = tempRoot.resolve(rel);
            Files.createDirectories(dest.getParent());
            Files.copy(icuRoot.resolve(rel), dest);
        }
        return tempRoot;
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(
                            p -> {
                                try {
                                    Files.deleteIfExists(p);
                                } catch (IOException ignored) {
                                    // Best-effort temp cleanup.
                                }
                            });
        }
    }
}
