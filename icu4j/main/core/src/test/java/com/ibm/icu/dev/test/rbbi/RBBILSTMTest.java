// © 2021 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
package com.ibm.icu.dev.test.rbbi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.CoreTestFmwk;
import com.ibm.icu.impl.breakiter.LSTMBreakEngine;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.BreakIterator;

/**
 * RBBILSTMTest data driven test.
 *      Perform the tests from the file *_Test.txt against the RBBI method
 *      under LSTM configuration. The test will not run if it is not under LSTM configuration.
 *      The test data file is common to both ICU4C and ICU4J.
 *      See the data file for a description of the tests.
 */
@RunWith(JUnit4.class)
public class RBBILSTMTest extends CoreTestFmwk {
    public RBBILSTMTest() {
    }

    @Test
    public void TestLSTMThai() {
        runTestFromFile("Thai_graphclust_model4_heavy_Test.txt", UScript.THAI);
    }

    @Test
    public void TestLSTMBurmese() {
        runTestFromFile("Burmese_graphclust_model5_heavy_Test.txt", UScript.MYANMAR);
    }

    private void runTestFromFile(String filename, int script) {
        // The expectation in this test depends on LSTM, skip the test if the
        // configuration is not build with LSTM data.
        org.junit.Assume.assumeTrue(!RBBITstUtils.skipLSTMTest());

        BreakIterator bi = BreakIterator.getWordInstance();
        String testString;
        InputStream is = RBBILSTMTest.class.getResourceAsStream("/com/ibm/icu/dev/test/rbbi/" + filename);
        if (is == null) {
            errln("Could not open test data file " + filename);
            return;
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        int caseNum = 0;
        String expected = "";
        String actual = "";
        LSTMBreakEngine engine = null;
        String line;
        try {
            while ((line = br.readLine()) != null) {
                String fields[] = line.split("\t");
                if (fields[0].equals("Model:")) {
                    String actualModelName = LSTMBreakEngine.createData(script).fName;
                    if (!actualModelName.equals(fields[1])) {
                        errln("The name of the built in model " + actualModelName +
                            " does not match the model (" + fields[1] + ") expected for this test");
                        return;
                    }
                } else if (fields[0].equals("Input:")) {
                    caseNum++;
                    int length = fields[1].length();
                    String input = "prefix " + fields[1] + " suffix";
                    bi.setText(input);
                    System.out.println("Input = " + input);
                    StringBuilder sb = new StringBuilder();
                    sb.append('{');
                    for (int bp = bi.first(); bp != BreakIterator.DONE; bp = bi.next()) {
                        sb.append(bp);
                        if (bp != input.length()) {
                            sb.append(", ");
                        }
                    }
                    sb.append('}');
                    actual =  sb.toString();
                } else if (fields[0].equals("Output:")) {
                    StringBuilder sb = new StringBuilder();
                    int sep;
                    int start = 0;
                    int curr = 0;
                    sb.append("{0, ");
                    String input = "prefix| |" + fields[1] + "| |suffix";
                    while ((sep = input.indexOf('|', start)) >= 0) {
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
                    sb.append(", ").append(curr + input.length() - start);
                    sb.append('}');
                    expected =  sb.toString();
                    assertEquals(input + " Test Case#" + caseNum , expected, actual);
                    actual = "";
                }
            }
        } catch (IOException e) {
           errln("Exception while reading lines of test data file " + filename + e.toString());
        }
    }
}
