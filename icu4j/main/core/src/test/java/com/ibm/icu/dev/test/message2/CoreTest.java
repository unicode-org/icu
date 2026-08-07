// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.ICUDebug;

import java.io.Reader;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class CoreTest extends CoreTestFmwk {
    private static final boolean DEBUG = ICUDebug.enabled("mf2");

    private static final String[] CLDR_SPEC_JSON_FILES = {
        "bidi.json",
        "data-model-errors.json",
        "fallback.json",
        "functions/currency.json",
        "functions/date.json",
        "functions/datetime.json",
        "functions/integer.json",
        "functions/number.json",
        "functions/offset.json",
        "functions/percent.json",
        "functions/string.json",
        "functions/time.json",
        "pattern-selection.json",
        "syntax-errors.json",
        "syntax.json",
        "u-options.json",
    };

    private static final String[] ICU_JSON_FILES = {
        "alias-selector-annotations.json",
        "duplicate-declarations.json",
        "icu-parser-tests.json",
        "icu-test-functions.json",
        "icu-test-previous-release.json",
        "icu-test-selectors.json",
        "invalid-options.json",
        "markup.json",
        "matches-whitespace.json",
        "more-data-model-errors.json",
        "more-functions.json",
        "normalization.json",
        "resolution-errors.json",
        "runtime-errors.json",
        "syntax-errors-diagnostics-multiline.json",
        "syntax-errors-diagnostics.json",
        "syntax-errors-end-of-input.json",
        "syntax-errors-reserved.json",
        "tricky-declarations.json",
        "unsupported-expressions.json",
        "unsupported-statements.json",
        "valid-tests.json"
    };

    @Test
    public void testCldrSpecJsonTests() throws Exception {
        runJsonTests(true, CLDR_SPEC_JSON_FILES);
    }

    @Test
    public void testIcuJsonTests() throws Exception {
        runJsonTests(false, ICU_JSON_FILES);
    }

    // Tests that exist in files (especially the ones from the official suite)
    // but that we still igore, for a certain reason.
    private static final Map<String, Set<Integer>> SKIP_TESTS =
            Map.of(
                    // CLDR
                    "functions/integer.json", Set.of(8, 9, 11, 12), // ICU-23225
                    "functions/number.json", Set.of(35, 36, 38, 39), // ICU-23225
                    "u-options.json", Set.of(1), // `:u:dir` on markup, MF2 issue #1005
                    // ICU only
                    "icu-test-previous-release.json", Set.of(8),
                    "invalid-options.json", Set.of(0)
                    );

    private void runJsonTests(boolean isCldrTest, String[] fileList) throws Exception {
        for (String jsonFile : fileList) {
            try (Reader reader = TestUtils.jsonReader(isCldrTest, jsonFile)) {
                Set<Integer> testsToSkip = SKIP_TESTS.getOrDefault(jsonFile, Set.of());
                if (DEBUG) {
                    System.out.println("==== " + jsonFile + " == " + testsToSkip);
                }
                MF2Test tests = TestUtils.GSON.fromJson(reader, MF2Test.class);
                for (int testIdx = 0; testIdx < tests.tests.length; testIdx++) {
                    Unit unit = tests.tests[testIdx];
                    if (DEBUG) {
                        System.out.printf("   %d. %s", testIdx, unit);
                    }
                    if (!testsToSkip.contains(testIdx)) {
                        TestUtils.runTestCase(tests.defaultTestProperties, unit);
                    } else {
                        if (DEBUG) {
                            System.out.printf(" => SKIPPED");
                        }
                    }
                    if (DEBUG) {
                        System.out.printf("%n");
                    }
                }
            }
        }
    }
}
