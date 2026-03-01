// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 * Created on May 5, 2004
 *
 * Copyright (C) 2004-2016 International Business Machines Corporation and others.
 * All Rights Reserved.
 *
 */
package com.ibm.icu.dev.test.rbbi;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.util.ULocale;


/**
 * Rule based break iterator data driven test.
 *      Perform the tests from the file rbbitst.txt.
 *      The test data file is common to both ICU4C and ICU4J.
 *      See the data file for a description of the tests.
 *
 */
@RunWith(JUnit4.class)
public class RBBITestExtended extends TestFmwk {
public RBBITestExtended() {
    }



static class TestParams {
    BreakIterator   bi;
    StringBuilder   dataToBreak    = new StringBuilder();
    int[]           expectedBreaks = new int[4000];
    int[]           srcLine        = new int[4000];
    int[]           srcCol         = new int[4000];
    ULocale         currentLocale  = new ULocale("en_US");
}


@Test
public void TestExtended() {
    // The expectations in this test heavily depends on the Thai dictionary.
    // Therefore, we skip this test under the LSTM configuration.
    org.junit.Assume.assumeTrue(!TestUtil.skipDictionaryTest());
    TestParams     tp = new TestParams();


    //
    //  Open and read the test data file.
    //
    StringBuilder testFileBuf = new StringBuilder();
    InputStream is = null;
    try {
        is = RBBITestExtended.class.getResourceAsStream("rbbitst.txt");
        if (is == null) {
            errln("Could not open test data file rbbitst.txt");
            return;
        }
        InputStreamReader isr = new InputStreamReader(is, "UTF-8");
        try {
            int c;
            int count = 0;
            for (;;) {
                c = isr.read();
                if (c < 0) {
                    break;
                }
                count++;
                if (c == 0xFEFF && count == 1) {
                    // BOM in the test data file. Discard it.
                    continue;
                }

                testFileBuf.appendCodePoint(c);
            }
        } finally {
            isr.close();
        }
    } catch (IOException e) {
        errln(e.toString());
        try {
            is.close();
        } catch (IOException ignored) {
        }
        return;
    }

    String testString = testFileBuf.toString();


    final int  PARSE_COMMENT = 1;
    final int  PARSE_TAG     = 2;
    final int  PARSE_DATA    = 3;
    final int  PARSE_NUM     = 4;
    final int  PARSE_RULES   = 5;

    int parseState = PARSE_TAG;

    int savedState = PARSE_TAG;

    int    lineNum  = 1;
    int    colStart = 0;
    int    column   = 0;
    int    charIdx  = 0;
    int    i;

    int    tagValue = 0;       // The numeric value of a <nnn> tag.

    StringBuilder   rules = new StringBuilder();     // Holds rules from a <rules> ... </rules> block
    int             rulesFirstLine = 0;              // Line number of the start of current <rules> block

    int    len = testString.length();

    for (charIdx = 0; charIdx < len; ) {
        int c = testString.codePointAt(charIdx);
        charIdx++;
        if (c == '\r' && charIdx<len && testString.charAt(charIdx) == '\n') {
            // treat CRLF as a unit
            c = '\n';
            charIdx++;
        }
        if (c == '\n' || c == '\r') {
            lineNum++;
            colStart = charIdx;
        }
        column = charIdx - colStart + 1;

        switch (parseState) {
        case PARSE_COMMENT:
            if (c == 0x0a || c == 0x0d) {
                parseState = savedState;
            }
            break;

        case PARSE_TAG:
            {
            if (c == '#') {
                parseState = PARSE_COMMENT;
                savedState = PARSE_TAG;
                break;
            }
            if (UCharacter.isWhitespace(c)) {
                break;
            }
           if (testString.startsWith("<word>", charIdx-1)) {
                tp.bi = BreakIterator.getWordInstance(tp.currentLocale);
                charIdx += 5;
                break;
            }
            if (testString.startsWith("<char>", charIdx-1)) {
                tp.bi = BreakIterator.getCharacterInstance(tp.currentLocale);
                charIdx += 5;
                break;
            }
            if (testString.startsWith("<line>", charIdx-1)) {
                tp.bi = BreakIterator.getLineInstance(tp.currentLocale);
                charIdx += 5;
                break;
            }
            if (testString.startsWith("<sent>", charIdx-1)) {
                tp.bi = BreakIterator.getSentenceInstance(tp.currentLocale);
                charIdx += 5;
                break;
            }
            if (testString.startsWith("<title>", charIdx-1)) {
                tp.bi = BreakIterator.getTitleInstance(tp.currentLocale);
                charIdx += 6;
                break;
            }
            if (testString.startsWith("<rules>", charIdx-1) ||
                    testString.startsWith("<badrules>", charIdx-1)) {
                charIdx = testString.indexOf('>', charIdx) + 1;
                parseState = PARSE_RULES;
                rules.setLength(0);
                rulesFirstLine = lineNum;
                break;
            }

            if (testString.startsWith("<locale ", charIdx-1)) {
                int closeIndex = testString.indexOf(">", charIdx);
                if (closeIndex < 0) {
                    errln("line" + lineNum + ": missing close on <locale  tag.");
                    break;
                }
                String localeName = testString.substring(charIdx+6, closeIndex);
                localeName = localeName.trim();
                tp.currentLocale = new ULocale(localeName);
                charIdx = closeIndex+1;
                break;
            }
            if (testString.startsWith("<data>", charIdx-1)) {
                parseState = PARSE_DATA;
                charIdx += 5;
                tp.dataToBreak.setLength(0);
                Arrays.fill(tp.expectedBreaks, 0);
                Arrays.fill(tp.srcCol, 0);
                Arrays.fill(tp.srcLine, 0);
                break;
            }

            errln("line" + lineNum + ": Tag expected in test file.");
            return;
            //parseState = PARSE_COMMENT;
            //savedState = PARSE_DATA;
            }

        case PARSE_RULES:
            if (testString.startsWith("</rules>", charIdx-1)) {
                charIdx += 7;
                parseState = PARSE_TAG;
                try {
                    tp.bi = new RuleBasedBreakIterator(rules.toString());
                } catch (IllegalArgumentException e) {
                    errln(String.format("rbbitst.txt:%d  Error creating break iterator from rules.  %s", lineNum, e));
                }
            } else if (testString.startsWith("</badrules>", charIdx-1)) {
                charIdx += 10;
                parseState = PARSE_TAG;
                boolean goodRules = true;
                try {
                    new RuleBasedBreakIterator(rules.toString());
                } catch (IllegalArgumentException e) {
                    goodRules = false;
                }
                if (goodRules) {
                    errln(String.format(
                            "rbbitst.txt:%d  Expected, but did not get, a failure creating break iterator from rules.",
                            lineNum));
                }
            } else {
                rules.appendCodePoint(c);
            }
            break;

        case PARSE_DATA:
            if (c == '•') {
                int  breakIdx = tp.dataToBreak.length();
                if (tp.expectedBreaks[breakIdx] != 0) {
                    errln(String.format(
                            "rbbitst.txt:%d:%d adjacent expected breaks with no intervening test text",
                            lineNum, column));
                }
                tp.expectedBreaks[breakIdx] = -1;
                tp.srcLine[breakIdx]        = lineNum;
                tp.srcCol[breakIdx]         = column;
                break;
            }

            if (testString.startsWith("</data>", charIdx-1))  {
                // Add final entry to mappings from break location to source file position.
                //  Need one extra because last break position returned is after the
                //    last char in the data, not at the last char.
                int idx = tp.dataToBreak.length();
                tp.srcLine[idx] = lineNum;
                tp.srcCol[idx]  = column;

                parseState = PARSE_TAG;
                charIdx += 6;

                // RUN THE TEST!
                executeTest(tp);
                break;
            }

           if (testString.startsWith("\\N{", charIdx-1)) {
               int nameEndIdx = testString.indexOf('}', charIdx);
               if (nameEndIdx == -1) {
                   errln("Error in named character in test file at line " + lineNum +
                           ", col " + column);
               }
                // Named character, e.g. \N{COMBINING GRAVE ACCENT}
                // Get the code point from the name and insert it into the test data.
                String charName = testString.substring(charIdx+2, nameEndIdx);
                c = UCharacter.getCharFromName(charName);
                if (c == -1) {
                    errln("Error in named character in test file at line " + lineNum +
                            ", col " + column);
                } else {
                    // Named code point was recognized.  Insert it
                    //   into the test data.
                    tp.dataToBreak.appendCodePoint(c);
                    for (i = tp.dataToBreak.length()-1; i>=0 && tp.srcLine[i]==0; i--) {
                        tp.srcLine[i] = lineNum;
                        tp.srcCol[i]  = column;
                    }

                 }
                if (nameEndIdx > charIdx) {
                    charIdx = nameEndIdx+1;
                }
                break;
            }

            if (testString.startsWith("<>", charIdx-1)) {
                charIdx++;
                int  breakIdx = tp.dataToBreak.length();
                tp.expectedBreaks[breakIdx] = -1;
                tp.srcLine[breakIdx]        = lineNum;
                tp.srcCol[breakIdx]         = column;
                break;
            }

            if (c == '<') {
                tagValue   = 0;
                parseState = PARSE_NUM;
                break;
            }

            if (c == '#' && column==3) {   // TODO:  why is column off so far?
                parseState = PARSE_COMMENT;
                savedState = PARSE_DATA;
                break;
            }

            if (c == '\\') {
                // Check for \ at end of line, a line continuation.
                //     Advance over (discard) the newline
                int cp = testString.codePointAt(charIdx);
                if (cp == '\r' && charIdx<len && testString.codePointAt(charIdx+1) == '\n') {
                    // We have a CR LF
                    //  Need an extra increment of the input ptr to move over both of them
                    charIdx++;
                }
                if (cp == '\n' || cp == '\r') {
                    lineNum++;
                    column   = 0;
                    charIdx++;
                    colStart = charIdx;
                    break;
                }

                // Let unescape handle the back slash.
                int cpAndLength = Utility.unescapeAndLengthAt(testString, charIdx);
                if (cpAndLength >= 0) {
                    // Escape sequence was recognized.  Insert the char
                    //   into the test data.
                    charIdx += Utility.lengthFromCodePointAndLength(cpAndLength);
                    tp.dataToBreak.appendCodePoint(Utility.cpFromCodePointAndLength(cpAndLength));
                    for (i=tp.dataToBreak.length()-1; i>=0 && tp.srcLine[i]==0; i--) {
                        tp.srcLine[i] = lineNum;
                        tp.srcCol[i]  = column;
                    }

                    break;
                }


                // Not a recognized backslash escape sequence.
                // Take the next char as a literal.
                //  TODO:  Should this be an error?
                c = testString.codePointAt(charIdx);
                charIdx = testString.offsetByCodePoints(charIdx, 1);
             }

            // Normal, non-escaped data char.
            tp.dataToBreak.appendCodePoint(c);

            // Save the mapping from offset in the data to line/column numbers in
            //   the original input file.  Will be used for better error messages only.
            //   If there's an expected break before this char, the slot in the mapping
            //     vector will already be set for this char; don't overwrite it.
            for (i=tp.dataToBreak.length()-1; i>=0 && tp.srcLine[i]==0; i--) {
                tp.srcLine[i] = lineNum;
                tp.srcCol[i]  = column;
            }
            break;


        case PARSE_NUM:
            // We are parsing an expected numeric tag value, like <1234>,
            //   within a chunk of data.
            if (UCharacter.isWhitespace(c)) {
                break;
            }

            if (c == '>') {
                // Finished the number.  Add the info to the expected break data,
                //   and switch parse state back to doing plain data.
                parseState = PARSE_DATA;
                if (tagValue == 0) {
                    tagValue = -1;
                }
                int  breakIdx = tp.dataToBreak.length();
                if (tp.expectedBreaks[breakIdx] != 0) {
                    errln(String.format(
                            "rbbitst.txt:%d:%d adjacent expected breaks with no intervening test text",
                            lineNum, column));
                }
                tp.expectedBreaks[breakIdx] = tagValue;
                tp.srcLine[breakIdx]        = lineNum;
                tp.srcCol[breakIdx]         = column;
                break;
            }

            if (UCharacter.isDigit(c)) {
                tagValue = tagValue*10 + UCharacter.digit(c);
                break;
            }

            errln(String.format("Syntax Error in rbbitst.txt at line %d, col %d", lineNum, column));
            return;
        }
    }

    // Reached end of test file. Raise an error if parseState indicates that we are
    //   within a block that should have been terminated.
    if (parseState == PARSE_RULES) {
        errln(String.format("rbbitst.txt:%d <rules> block beginning at line %d is not closed.",
            lineNum, rulesFirstLine));
    }
    if (parseState == PARSE_DATA) {
        errln(String.format("rbbitst.txt:%d <data> block not closed.", lineNum));
    }
}

void executeTest(TestParams t) {
    // TODO: also rerun tests with a break iterator re-created from bi.getRules()
    //       and from bi.clone(). If in exhaustive mode only.
    int    bp;
    int    prevBP;
    int    i;

    if (t.bi == null) {
        return;
    }

    t.bi.setText(t.dataToBreak.toString());
    //
    //  Run the iterator forward
    //
    prevBP = -1;
    for (bp = t.bi.first(); bp != BreakIterator.DONE; bp = t.bi.next()) {
        if (prevBP ==  bp) {
            // Fail for lack of forward progress.
            errln("Forward Iteration, no forward progress.  Break Pos=" + bp +
                    "  File line,col=" + t.srcLine[bp] + ", " + t.srcCol[bp]);
            break;
        }

        // Check that there were we didn't miss an expected break between the last one
        //  and this one.
        for (i=prevBP+1; i<bp; i++) {
            if (t.expectedBreaks[i] != 0) {
                errln("Forward Iteration, break expected, but not found.  Pos=" + i +
                    "  File line,col= " + t.srcLine[i] + ", " + t.srcCol[i]);
            }
        }

        // Check that the break we did find was expected
        if (t.expectedBreaks[bp] == 0) {
            errln("Forward Iteration, break found, but not expected.  Pos=" + bp +
                    "  File line,col= " + t.srcLine[bp] + ", " + t.srcCol[bp]);
        } else {
            // The break was expected.
            //   Check that the {nnn} tag value is correct.
            int expectedTagVal = t.expectedBreaks[bp];
            if (expectedTagVal == -1) {
                expectedTagVal = 0;
            }
            int line = t.srcLine[bp];
            int rs = t.bi.getRuleStatus();
            if (rs != expectedTagVal) {
                errln("Incorrect status for forward break.  Pos = " + bp +
                        ".  File line,col = " + line + ", " + t.srcCol[bp] + "\n" +
                      "          Actual, Expected status = " + rs + ", " + expectedTagVal);
            }
            int[] fillInArray = new int[4];
            int numStatusVals = t.bi.getRuleStatusVec(fillInArray);
            assertTrue("", numStatusVals >= 1);
            assertEquals("", expectedTagVal, fillInArray[0]);
        }


        prevBP = bp;
    }

    // Verify that there were no missed expected breaks after the last one found
    for (i=prevBP+1; i<t.dataToBreak.length()+1; i++) {
        if (t.expectedBreaks[i] != 0) {
            errln("Forward Iteration, break expected, but not found.  Pos=" + i +
                    "  File line,col= " + t.srcLine[i] + ", " + t.srcCol[i]);
       }
    }


    //
    //  Run the iterator backwards, verify that the same breaks are found.
    //
    prevBP = t.dataToBreak.length()+2;  // start with a phony value for the last break pos seen.
    for (bp = t.bi.last(); bp != BreakIterator.DONE; bp = t.bi.previous()) {
        if (prevBP ==  bp) {
            // Fail for lack of progress.
            errln("Reverse Iteration, no progress.  Break Pos=" + bp +
                    "File line,col=" + t.srcLine[bp] + " " +  t.srcCol[bp]);
            break;
        }

        // Check that we didn't miss an expected break between the last one
        //  and this one.  (UVector returns zeros for index out of bounds.)
        for (i=prevBP-1; i>bp; i--) {
            if (t.expectedBreaks[i] != 0) {
                errln("Reverse Itertion, break expected, but not found.  Pos=" + i +
                    "  File line,col= " + t.srcLine[i] + ", " + t.srcCol[i]);
            }
        }

        // Check that the break we did find was expected
        if (t.expectedBreaks[bp] == 0) {
            errln("Reverse Itertion, break found, but not expected.  Pos=" + bp +
                    "  File line,col= " + t.srcLine[bp] + ", " + t.srcCol[bp]);
        } else {
            // The break was expected.
            //   Check that the {nnn} tag value is correct.
            int expectedTagVal = t.expectedBreaks[bp];
            if (expectedTagVal == -1) {
                expectedTagVal = 0;
            }
            int line = t.srcLine[bp];
            int rs = t.bi.getRuleStatus();
            if (rs != expectedTagVal) {
                errln("Incorrect status for reverse break.  Pos = " + bp +
                      "  File line,col= " + line + ", " + t.srcCol[bp] + "\n" +
                      "          Actual, Expected status = " + rs + ", " + expectedTagVal);
            }
        }

        prevBP = bp;
    }

    // Verify that there were no missed breaks prior to the last one found
    for (i=prevBP-1; i>=0; i--) {
        if (t.expectedBreaks[i] != 0) {
            errln("Reverse Itertion, break expected, but not found.  Pos=" + i +
                    "  File line,col= " + t.srcLine[i] + ", " + t.srcCol[i]);
         }
    }
    // Check isBoundary()
    for (i=0; i<=t.dataToBreak.length(); i++) {
        boolean boundaryExpected = (t.expectedBreaks[i] != 0);
        boolean boundaryFound    = t.bi.isBoundary(i);
        if (boundaryExpected != boundaryFound) {
            errln("isBoundary(" + i + ") incorrect.\n" +
                  "  File line,col= " + t.srcLine[i] + ", " + t.srcCol[i] +
                  "    Expected, Actual= " + boundaryExpected + ", " + boundaryFound);
        }
    }

    // Check following()
    for (i=0; i<=t.dataToBreak.length(); i++) {
        int actualBreak = t.bi.following(i);
        int expectedBreak = BreakIterator.DONE;
        for (int j=i+1; j < t.expectedBreaks.length; j++) {
            if (t.expectedBreaks[j] != 0) {
                expectedBreak = j;
                break;
            }
        }
        if (expectedBreak != actualBreak) {
            errln("following(" + i + ") incorrect.\n" +
                    "  File line,col= " + t.srcLine[i] + ", " + t.srcCol[i] +
                    "    Expected, Actual= " + expectedBreak + ", " + actualBreak);
        }
    }

    // Check preceding()
    for (i=t.dataToBreak.length(); i>=0; i--) {
        int actualBreak = t.bi.preceding(i);
        int expectedBreak = BreakIterator.DONE;

        for (int j=i-1; j >= 0; j--) {
            if (t.expectedBreaks[j] != 0) {
                expectedBreak = j;
                break;
            }
        }
        if (expectedBreak != actualBreak) {
            errln("preceding(" + i + ") incorrect.\n" +
                    "  File line,col= " + t.srcLine[i] + ", " + t.srcCol[i] +
                    "    Expected, Actual= " + expectedBreak + ", " + actualBreak);
        }
    }

}




}
