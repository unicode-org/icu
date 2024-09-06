// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.google.gson.reflect.TypeToken;
import com.ibm.icu.dev.test.CoreTestFmwk;

@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class IcuFunctionsTest extends CoreTestFmwk {
    private static final String JSON_FILE = "icu-test-functions.json";

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
                    TestUtils.runTestCase(unit);
                }
            }
        }
    }
}
