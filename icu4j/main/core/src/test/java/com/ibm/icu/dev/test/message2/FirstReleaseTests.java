// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.ibm.icu.dev.test.CoreTestFmwk;
import java.io.Reader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/*
 * This is the equivalent of the `FromJsonTest` class in the previous release.
 * That class was originally a json file, converted to some hard-coded tests in the Java class.
 * Now that we can use gson for testing we reverted those tests back to json, tested in this class.
 */
@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class FirstReleaseTests extends CoreTestFmwk {
    private static final String JSON_FILE = "icu-test-previous-release.json";

    @Test
    public void test() throws Exception {
        try (Reader reader = TestUtils.jsonReader(JSON_FILE)) {
            Unit[] unitList = TestUtils.GSON.fromJson(reader, Unit[].class);
            for (Unit unit : unitList) {
                TestUtils.runTestCase(unit);
            }
        }
    }
}
