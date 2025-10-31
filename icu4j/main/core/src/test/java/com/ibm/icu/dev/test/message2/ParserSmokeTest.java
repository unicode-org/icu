// © 2024 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package com.ibm.icu.dev.test.message2;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.message2.MFParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/*
 * A list of tests for the parser.
 */
@RunWith(JUnit4.class)
@SuppressWarnings({"static-method", "javadoc"})
public class ParserSmokeTest extends CoreTestFmwk {

    @Test(expected = IllegalArgumentException.class)
    public void testNullInput() throws Exception {
        MFParser.parse(null);
    }

    // Other tests in CoreTest.java
}
