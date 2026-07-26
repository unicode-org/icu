// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.io.Reader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class CoreTest extends CoreTestFmwk {
    private static final boolean DEBUG = false;

    private static final String[] CLDR_SPEC_JSON_FILES = {
        "bidi.json",
        "data-model-errors.json",
        "syntax-errors.json",
        "syntax.json",
        "fallback.json",
        "functions/currency.json",
        "functions/date.json",
        "functions/datetime.json",
        "functions/integer.json",
        "functions/offset.json",
        "functions/number.json",
        "functions/percent.json",
        "functions/string.json",
        "functions/time.json",
        "pattern-selection.json",
        "u-options.json", // FAILS 1 / 11, `:u:` on markup, issue #1005
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
        "syntax-errors-diagnostics.json",
        "syntax-errors-diagnostics-multiline.json",
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

    private void runJsonTests(boolean isCldrTest, String[] fileList) throws Exception {
        for (String jsonFile : fileList) {
            try (Reader reader = TestUtils.jsonReader(isCldrTest, jsonFile)) {
                if (DEBUG) {
                    System.out.println("==== " + jsonFile);
                }
                MF2Test tests = TestUtils.GSON.fromJson(reader, MF2Test.class);
                for (Unit unit : tests.tests) {
                    if (DEBUG) {
                        System.out.println("    " + unit);
                    }
                    TestUtils.runTestCase(tests.defaultTestProperties, unit);
                }
            }
        }
    }
}
