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
    private static final String[] JSON_FILES = {"alias-selector-annotations.json",
                                                "duplicate-declarations.json",
                                                "icu-parser-tests.json",
                                                "icu-test-functions.json",
                                                "icu-test-previous-release.json",
                                                "icu-test-selectors.json",
                                                "invalid-number-literals-diagnostics.json",
                                                "invalid-options.json",
                                                "markup.json",
                                                "matches-whitespace.json",
                                                "more-data-model-errors.json",
                                                "more-functions.json",
                                                "resolution-errors.json",
                                                "runtime-errors.json",
                                                "spec/data-model-errors.json",
                                                "spec/syntax-errors.json",
                                                "spec/syntax.json",
                                                "spec/functions/date.json",
                                                "spec/functions/datetime.json",
                                                "spec/functions/integer.json",
                                                "spec/functions/number.json",
                                                "spec/functions/string.json",
                                                "spec/functions/time.json",
                                                "syntax-errors-diagnostics.json",
                                                "syntax-errors-diagnostics-multiline.json",
                                                "syntax-errors-end-of-input.json",
                                                "syntax-errors-reserved.json",
                                                "tricky-declarations.json",
                                                "unsupported-expressions.json",
                                                "unsupported-statements.json",
                                                "valid-tests.json"};

    @Test
    public void test() throws Exception {
        for (String jsonFile : JSON_FILES) {
            try (Reader reader = TestUtils.jsonReader(jsonFile)) {
                MF2Test tests = TestUtils.GSON.fromJson(reader, MF2Test.class);
                for (Unit unit : tests.tests) {
                    TestUtils.runTestCase(tests.defaultTestProperties, unit);
                }
            }
        }
    }
}
