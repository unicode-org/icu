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
                                                "invalid-options.json",
                                                "markup.json",
                                                "matches-whitespace.json",
                                                "reserved-syntax.json",
                                                "resolution-errors.json",
                                                "runtime-errors.json",
                                                "spec/test-core.json",
                                                "syntax-errors-diagnostics.json",
                                                "tricky-declarations.json",
                                                "valid-tests.json"};

    @Test
    public void test() throws Exception {
        for (String jsonFile : JSON_FILES) {
            try (Reader reader = TestUtils.jsonReader(jsonFile)) {
                Unit[] unitList = TestUtils.GSON.fromJson(reader, Unit[].class);
                for (Unit unit : unitList) {
                    TestUtils.runTestCase(unit);
                }
            }
        }
    }
}
