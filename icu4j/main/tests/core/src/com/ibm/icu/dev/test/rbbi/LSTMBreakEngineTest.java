// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.rbbi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.breakiter.DictionaryBreakEngine;
import com.ibm.icu.impl.breakiter.LSTMBreakEngine;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.util.UResourceBundle;

/**
 * LSTMBreakEngine data driven test.
 *      Perform the tests from the file *_Test.txt.
 *      The test data file is common to both ICU4C and ICU4J.
 *      See the data file for a description of the tests.
 *
 */
@RunWith(JUnit4.class)
public class LSTMBreakEngineTest extends TestFmwk {

    private static final ClassLoader testLoader = LSTMBreakEngineTest.class.getClassLoader();

    public LSTMBreakEngineTest() {
    }

    @Test
    public void TestThaiGraphclust() {
        runTestFromFile("Thai_graphclust_model4_heavy_Test.txt", UScript.THAI);
    }

    @Test
    public void TestThaiCodepoints() {
        runTestFromFile("Thai_codepoints_exclusive_model5_heavy_Test.txt", UScript.THAI);
    }

    @Test
    public void TestBurmeseGraphclust() {
        runTestFromFile("Burmese_graphclust_model5_heavy_Test.txt", UScript.MYANMAR);
    }

    private LSTMBreakEngine createEngineFromTestData(String modelName, int script) {
        UResourceBundle bundle = UResourceBundle.getBundleInstance(
            "com/ibm/icu/dev/data/testdata", modelName, testLoader);
        return LSTMBreakEngine.create(script, LSTMBreakEngine.createData(bundle));
    }

    private void runTestFromFile(String filename, int script) {

        String testString;
        InputStream is = LSTMBreakEngineTest.class.getResourceAsStream("/com/ibm/icu/dev/test/rbbi/" + filename);
        if (is == null) {
            errln("Could not open test data file " + filename);
            return;
        }
        BufferedReader br = (new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)));
        int caseNum = 0;
        String expected = "";
        String actual = "";
        LSTMBreakEngine engine = null;
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String fields[] = line.split("\t");
                if (fields[0].equals("Model:")) {
                    engine = createEngineFromTestData(fields[1], script);
                } else if (fields[0].equals("Input:")) {
                    caseNum++;
                    int length = fields[1].length();
                    CharacterIterator input = new StringCharacterIterator(fields[1]);
                    DictionaryBreakEngine.DequeI foundBreaks = new DictionaryBreakEngine.DequeI();
                    int ret = engine.findBreaks(input, 0, length, foundBreaks, false);
                    StringBuilder sb = new StringBuilder();
                    sb.append('{');
                    for (int i = 0; i < foundBreaks.size(); i++) {
                        sb.append(foundBreaks.elementAt(i)).append(", ");
                    }
                    sb.append(length).append('}');
                    actual =  sb.toString();
                } else if (fields[0].equals("Output:")) {
                    StringBuilder sb = new StringBuilder();
                    int sep;
                    int start = 0;
                    int curr = 0;
                    sb.append('{');
                    while ((sep = fields[1].indexOf('|', start)) >= 0) {
                        int len = sep - start;
                        if (len > 0) {
                            if (curr > 0) {
                                sb.append(", ");
                            }
                            curr += len;
                            sb.append(curr);
                        }
                        start = sep + 1;
                    }
                    sb.append('}');
                    expected =  sb.toString();
                    assertEquals(line + " Test Case#" + caseNum , expected, actual);
                }
            }
        } catch (IOException e) {
            errln("Exception while reading lines of test data file " + filename + e.toString());
        }
    }
}
