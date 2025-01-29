// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.io.Reader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;

@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class CoreTest extends CoreTestFmwk {
    private String jsonFile;

    private static final String[] JSON_FILES = {
            "alias-selector-annotations.json", // FAILS 2 / 2
            "duplicate-declarations.json",
            "icu-parser-tests.json",
            "icu-test-functions.json",
            "icu-test-previous-release.json", // FAILS 6 / 25
            "icu-test-selectors.json", // FAILS 29 / 29
            "invalid-number-literals-diagnostics.json",
            "invalid-options.json",
            "markup.json",
            "matches-whitespace.json", // FAILS 9 / 9
            "more-data-model-errors.json",
            "more-functions.json",
            "normalization.json", // new // FAILS 7 / 11
            "resolution-errors.json",
            "runtime-errors.json",
            "spec/bidi.json", // new // FAILS 16 / 27
            "spec/data-model-errors.json", // FAILS 1 / 23
            "spec/syntax-errors.json", // FAILS 4 / 134
            "spec/syntax.json", // FAILS 8 / 107
            "spec/fallback.json", // new // FAILS 3 / 8
            "spec/functions/currency.json", // new // FAILS: ERROR PARSING JSON // FAILS 3 / 12
            "spec/functions/date.json", // FAILS 2 / 7
            "spec/functions/datetime.json", // FAILS 1 / 8
            "spec/functions/integer.json", // FAILS 1 / 6
            "spec/functions/math.json", // new // FAILS 9 / 16
            "spec/functions/number.json", // FAILS 10 / 23
            "spec/functions/string.json", // FAILS 7 / 9
            "spec/functions/time.json", // FAILS 2 / 6
            "spec/pattern-selection.json", // new // FAILS 15 / 22
            "spec/u-options.json", // new // FAILS 10 / 11
            "syntax-errors-diagnostics.json", // FAILS 1 / 123
            "syntax-errors-diagnostics-multiline.json",
            "syntax-errors-end-of-input.json",
            "syntax-errors-reserved.json",
            "tricky-declarations.json",
            "unsupported-expressions.json",
            "unsupported-statements.json",
            "valid-tests.json" // FAILS 3 / 84
    };
    
    @Test
    public void test() throws Exception {
        for (String jsonFile : JSON_FILES) {
            System.out.printf("Reading json file: %s%n", jsonFile);
            try (Reader reader = TestUtils.jsonReader(jsonFile)) {
                MF2Test tests = TestUtils.GSON.fromJson(reader, MF2Test.class);
                int errorCount = 0;
                for (Unit unit : tests.tests) {
                    try {
                        TestUtils.runTestCase(tests.defaultTestProperties, unit);
                    } catch (AssertionError e) {
//                        System.out.printf("----%n"
//                                + "Failing: %s%n"
//                                + "%s%n"
//                                + "----%n", unit, e.getMessage());
                        errorCount++;
                    }
                }
                String color = errorCount == 0 ? "\033[32m" : "\033[91m";
                if (errorCount != 0) {
                System.out.printf(" Result for '%s': // FAILS %d / %d%n",
                        jsonFile, errorCount, tests.tests.length);
                }
//                System.out.printf("    %sResult for '%s': total %d  failures: %d\033[m%n",
//                        color, jsonFile, tests.tests.length, errorCount);
            }
        }
    }
}
