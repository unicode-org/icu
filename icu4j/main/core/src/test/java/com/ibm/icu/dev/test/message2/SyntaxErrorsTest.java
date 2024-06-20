// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import java.io.Reader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.message2.MessageFormatter;

@SuppressWarnings({"static-method", "javadoc"})
@RunWith(JUnit4.class)
public class SyntaxErrorsTest extends CoreTestFmwk {
    private static final String[] JSON_FILES = {"more-syntax-errors.json",
                                                "spec/syntax-errors.json"};

    @Test
    public void test() throws Exception {
        for (String jsonFile : JSON_FILES) {
            try (Reader reader = TestUtils.jsonReader(jsonFile)) {
                String[] srcList = TestUtils.GSON.fromJson(reader, String[].class);
                for (String source : srcList) {
                    try {
                        MessageFormatter.builder().setPattern(source).build();
                        fail("Pattern expected to fail, but didn't: '" + source + "'");
                    } catch (Exception e) {
                        // If we get here it is fine
                    }
                }
            }
        }
    }
}
