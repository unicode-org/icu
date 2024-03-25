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
import com.ibm.icu.message2.MessageFormatter;

@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class DataModelErrorsTest extends CoreTestFmwk {
    private static final String JSON_FILE = "data-model-errors.json";

    @Test
    public void test() throws Exception {
        try (Reader reader = TestUtils.jsonReader(JSON_FILE)) {
            Type mapType = new TypeToken<Map<String, String[]>>(){/* not code */}.getType();
            Map<String, String[]> unitList = TestUtils.GSON.fromJson(reader, mapType);
            for (Entry<String, String[]> tests : unitList.entrySet()) {
                for (String pattern : tests.getValue()) {
                    try {
                        MessageFormatter.builder().setPattern(pattern).build().formatToString(null);
                        fail("Undetected errors in '" + tests.getKey() + "': '" + pattern + "'");
                    } catch (Exception e) {
                        // We expected an error, so it's all good
                    }
                }
            }
        }
    }
}
