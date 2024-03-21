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
public class SelectorsWithVariousArgumentsTest extends CoreTestFmwk {
    private static final String JSON_FILE = "icu-test-selectors.json";

    @Test
    public void test() throws Exception {
        try (Reader reader = TestUtils.jsonReader(JSON_FILE)) {
            TestWithVariations[] unitList =
                    TestUtils.GSON.fromJson(reader, TestWithVariations[].class);
            for (TestWithVariations testWithVar : unitList) {
                Unit sharedUnit = testWithVar.shared;
                for (Unit variation : testWithVar.variations) {
                    Unit mergedUnit = sharedUnit.merge(variation);
                    TestUtils.runTestCase(mergedUnit);
                }
            }
        }
    }

    class TestWithVariations {
        String comment;
        Unit shared;
        Unit[] variations;
    }
}
