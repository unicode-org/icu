// FooTest.java
package com.ibm.test.normalizer;

import com.ibm.test.*;
import com.ibm.text.*;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

public class FooTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
        new FooTest().run(args);
    }

    public void testTibetan() {
        String[][] decomp = {
            { "\u0f77", "\u0fb2\u0f71\u0f80", "\u0f77" }
        };
        String[][] compose = {
            { "\u0fb2\u0f71\u0f80", "\u0f77" }
        };

        staticTest(Normalizer.DECOMP, 0, decomp, 1);
        staticTest(Normalizer.COMPOSE, 0, compose, 1);
    }

    public void TestCombineZero() {
        String[][] decomp = {
            { "\u09CB", "\u09C7\u09BE", "\u09CB" }
        };
        String[][] compose = {
            { "\u09C7\u09BE", "\u09CB" }
        };

        staticTest(Normalizer.DECOMP, 0, decomp, 1);
        staticTest(Normalizer.COMPOSE, 0, compose, 1);
    }

    private void backAndForth(Normalizer iter, String[][] tests)
    {
        for (int i = 0; i < tests.length; i++)
        {
            iter.setText(tests[i][0]);

            // Run through the iterator forwards and stick it into a StringBuffer
            StringBuffer forward =  new StringBuffer();
            for (char ch = iter.first(); ch != iter.DONE; ch = iter.next()) {
                forward.append(ch);
            }

            // Now do it backwards
            StringBuffer reverse = new StringBuffer();
            for (char ch = iter.last(); ch != iter.DONE; ch = iter.previous()) {
                reverse.insert(0, ch);
            }

            if (!forward.toString().equals(reverse.toString())) {
                errln("Forward/reverse mismatch for input " + hex(tests[i][0])
                    + ", forward: " + hex(forward) + ", backward: " + hex(reverse));
            }
        }
    }

    private void staticTest(Normalizer.Mode mode, int options, String[][] tests, int outCol)
    {
        for (int i = 0; i < tests.length; i++)
        {
            String input = tests[i][0];
            String expect = tests[i][outCol];

            logln("Normalizing '" + input + "' (" + hex(input) + ")" );

            String output = Normalizer.normalize(input, mode, options);

            if (!output.equals(expect)) {
                errln("ERROR: case " + i
                    + " expected '" + expect + "' (" + hex(expect) + ")"
                    + " but got '" + output + "' (" + hex(output) + ")" );
            }
        }
    }

    private void iterateTest(Normalizer iter, String[][] tests, int outCol)
    {
        for (int i = 0; i < tests.length; i++)
        {
            String input = tests[i][0];
            String expect = tests[i][outCol];

            logln("Normalizing '" + input + "' (" + hex(input) + ")" );

            iter.setText(input);
            assertEqual(expect, iter, "ERROR: case " + i + " ");
        }
    }

    private void assertEqual(String expected, Normalizer iter, String errPrefix)
    {
        int index = 0;
        for (char ch = iter.first(); ch != iter.DONE; ch = iter.next())
        {
            if (index >= expected.length()) {
                errln(errPrefix + "Unexpected character '" + ch + "' (" + hex(ch) + ")"
                        + " at index " + index);
                break;
            }
            char want = expected.charAt(index);
            if (ch != want) {
                errln(errPrefix + "got '" + ch + "' (" + hex(ch) + ")"
                        + " but expected '" + want + "' (" + hex(want) + ")"
                        + " at index " + index);
            }
            index++;
        }
        if (index < expected.length()) {
            errln(errPrefix + "Only got " + index + " chars, expected " + expected.length());
        }
    }

}