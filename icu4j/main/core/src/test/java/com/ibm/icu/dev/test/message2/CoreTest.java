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
//            "alias-selector-annotations.json",
//            "duplicate-declarations.json",
//            "icu-parser-tests.json",
//            "icu-test-functions.json",
//            "icu-test-previous-release.json",
//            "icu-test-selectors.json",
//            "invalid-number-literals-diagnostics.json",
//            "invalid-options.json",
//            "markup.json",
//            "matches-whitespace.json",
//            "more-data-model-errors.json",
//            "more-functions.json",
//            "normalization.json", // new
//            "resolution-errors.json",
//            "runtime-errors.json",
//            "spec/bidi.json", // new // FAILS 9 / 27
//            "spec/data-model-errors.json",
//            "spec/syntax-errors.json",
//            "spec/syntax.json",
//            "spec/fallback.json", // new
//            "spec/functions/currency.json", // new // FAILS 3 / 12 // TODO 77.1: error for :currency without currency code
//            "spec/functions/date.json",
//            "spec/functions/datetime.json",
//            "spec/functions/integer.json", // FAILS 1 / 6
//            "spec/functions/math.json", // new // FAILS 2 / 16
//            "spec/functions/number.json",
            "spec/functions/string.json", // FAILS 2 / 9
//            "spec/functions/time.json",
//            "spec/pattern-selection.json", // new
//            "spec/u-options.json", // new // FAILS 10 / 11
//            "syntax-errors-diagnostics.json",
//            "syntax-errors-diagnostics-multiline.json",
//            "syntax-errors-end-of-input.json",
//            "syntax-errors-reserved.json",
//            "tricky-declarations.json",
//            "unsupported-expressions.json",
//            "unsupported-statements.json",
//            "valid-tests.json"
    };
    
    static boolean reportError(boolean firstTime, String jsonFile, String message) {
        if (firstTime) {
            System.out.println();
            System.out.println("============================");
            System.out.printf("= Reading json file: %s%n", jsonFile);
            System.out.println("============================");
        }
        System.out.println(message);
        System.out.println("----------");
        return false;
    }
    
    @Test
    public void test() throws Exception {
        for (String jsonFile : JSON_FILES) {
            boolean firstTime = true;
//            System.out.println("============================");
//            System.out.printf("= Reading json file: %s%n", jsonFile);
//            System.out.println("============================");
            try (Reader reader = TestUtils.jsonReader(jsonFile)) {
                int errorCount = 0;
                MF2Test tests;
                try {
                    tests = TestUtils.GSON.fromJson(reader, MF2Test.class);
                } catch (com.google.gson.JsonSyntaxException e) {
                    tests = new MF2Test(null, new Unit[0]);
                    errorCount = 1_000_000;
                    firstTime = reportError(firstTime, jsonFile, "JSON error: " + e.getMessage());
                }
                for (Unit unit : tests.tests) {
                    try {
                        TestUtils.runTestCase(tests.defaultTestProperties, unit);
                    } catch (AssertionError e) {
                        firstTime = reportError(firstTime, jsonFile, e.getMessage());
                        errorCount++;
                    }
                }
                String color = errorCount == 0 ? "\033[32m" : "\033[91m";
                if (errorCount != 0) {
                    System.out.printf("%s\t%d\t%d%n",
                            jsonFile, errorCount, tests.tests.length);
                }
            }
        }
    }
}
