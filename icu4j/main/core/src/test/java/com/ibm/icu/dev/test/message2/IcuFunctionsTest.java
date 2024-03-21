// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.google.gson.reflect.TypeToken;
import com.ibm.icu.dev.test.CoreTestFmwk;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class IcuFunctionsTest extends CoreTestFmwk {
    private static final String JSON_FILE = "icu-test-functions.json";

    // Some default parameters for all messages, to use if the message does not have its own
    private static final Map<String, Object> ARGS =
            Args.of(
                    "user", "John",
                    "exp", new Date(2024 - 1900, 7, 3, 21, 43, 57), // Aug 3, 2024, at 9:43:57 pm
                    "tsOver", "full");

    @Test
    public void test() throws Exception {
        try (Reader reader = TestUtils.jsonReader(JSON_FILE)) {
            Type mapType =
                    new TypeToken<Map<String, Unit[]>>() {
                        /* not code */
                    }.getType();
            Map<String, Unit[]> unitList = TestUtils.GSON.fromJson(reader, mapType);
            for (Entry<String, Unit[]> testGroup : unitList.entrySet()) {
                for (Unit unit : testGroup.getValue()) {
                    TestUtils.runTestCase(unit, ARGS);
                }
            }
        }
    }
}
