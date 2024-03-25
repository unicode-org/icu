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
import com.ibm.icu.message2.MFParser;

/*
 * A list of tests for the parser.
 */
@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class ParserSmokeTest extends CoreTestFmwk {
    private static final String JSON_FILE = "icu-parser-tests.json";

    @Test(expected = IllegalArgumentException.class)
    public void testNullInput() throws Exception {
        MFParser.parse(null);
    }

    @Test
    public void test() throws Exception {
        try (Reader reader = TestUtils.jsonReader(JSON_FILE)) {
            Type mapType = new TypeToken<Map<String, String[]>>(){/* not code */}.getType();
            Map<String, String[]> unitList = TestUtils.GSON.fromJson(reader, mapType);
            for (Entry<String, String[]> testGroup : unitList.entrySet()) {
                for (String unit : testGroup.getValue()) {
                    MFParser.parse(unit);
                }
            }
        }
    }
}
