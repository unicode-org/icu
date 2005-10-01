/*
 * Created on May 5, 2004
 * 
 * Copyright (C) 2004 International Business Machines Corporation and others.
 * All Rights Reserved.
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package com.ibm.icu.dev.test.rbbi;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUData;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.UTF16;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;


/**
 * @author andy
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RBBITestExtended extends TestFmwk {
    public static void main(String[] args)throws Exception {
        new RBBITestExtended().run(args);
    }

    
    
    public RBBITestExtended() { 
    }



static class TestParams {
    BreakIterator   bi;
    StringBuffer    dataToBreak    = new StringBuffer();
    int[]           expectedBreaks = new int[1000];
    int[]           srcLine        = new int[1000];
    int[]           srcCol         = new int[1000];
};


public void TestExtended() {

    String             rules;
    TestParams     tp = new TestParams();


    //
    //  Open and read the test data file.
    //
    InputStreamReader isr = null;
    StringBuffer  testFileBuf = new StringBuffer();
    try {
        InputStream is = RBBITestExtended.class.getResourceAsStream("rbbitst.txt");
        if (is == null) {
            errln("Could not open test data file rbbitst.txt");
            return;
        }
        isr = new InputStreamReader(is, "UTF-8");           
        int c;
        int count = 0;
        for (;;) {
            c = isr.read();
            if (c < 0) {
                break;
            }
            count++;
            if (c==0xFEFF && count==1) {
               // BOM in the test data file.  Discard it.
               continue;
            }
           
            UTF16.append(testFileBuf, c);
        }
        
    } catch (IOException e) {
        errln(e.toString());
        return;
    }
    
    String testString = testFileBuf.toString();
    

    final int  PARSE_COMMENT = 1;
    final int  PARSE_TAG     = 2;
    final int  PARSE_DATA    = 3;
    final int  PARSE_NUM     = 4;

    int parseState = PARSE_TAG;

    int savedState = PARSE_TAG;

    final char CH_LF        = 0x0a;
    final char CH_CR        = 0x0d;
    final char CH_HASH      = 0x23;
    /*static const UChar CH_PERIOD    = 0x2e;*/
    final char CH_LT        = 0x3c;
    final char CH_GT        = 0x3e;
    final char CH_BACKSLASH = 0x5c;
    final char CH_BULLET    = 0x2022;

    int    lineNum  = 1;
    int    colStart = 0;
    int    column   = 0;
    int    charIdx  = 0;
    int    i;

    int    tagValue = 0;       // The numeric value of a <nnn> tag.
    int    len = testString.length();

    for (charIdx = 0; charIdx < len; ) {
        int  c = UTF16.charAt(testString, charIdx);
        charIdx++;
        if (c == CH_CR && charIdx<len && testString.charAt(charIdx) == CH_LF) {
            // treat CRLF as a unit
            c = CH_LF;
            charIdx++;
        }
        if (c == CH_LF || c == CH_CR) {
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
            if (c == CH_HASH) {
                parseState = PARSE_COMMENT;
                savedState = PARSE_TAG;
                break;
            }
            if (UCharacter.isWhitespace(c)) {
                break;
            }
           if (testString.startsWith("<word>", charIdx-1)) {
                tp.bi = BreakIterator.getWordInstance(Locale.US);
                charIdx += 5;
                break;
            }
            if (testString.startsWith("<char>", charIdx-1)) {
                tp.bi = BreakIterator.getCharacterInstance(Locale.US);
                charIdx += 5;
                break;
            }
            if (testString.startsWith("<line>", charIdx-1)) {
                tp.bi = BreakIterator.getLineInstance(Locale.US);
                charIdx += 5;
                break;
            }
            if (testString.startsWith("<sent>", charIdx-1)) {
                tp.bi = BreakIterator.getSentenceInstance(Locale.US);
                charIdx += 5;
                break;
            }
            if (testString.startsWith("<title>", charIdx-1)) {
                tp.bi = BreakIterator.getTitleInstance(Locale.US);
                charIdx += 6;
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
            // break;   // TODO: don't stop on errors

        case PARSE_DATA:
            if (c == CH_BULLET) {
                int  breakIdx = tp.dataToBreak.length();
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
                charIdx += 7;

                // RUN THE TEST!
                executeTest(tp);
                break;
            }

           if (testString.startsWith("\\N{", charIdx-1)) {
               int nameEndIdx = testString.indexOf('}', charIdx);
               if (nameEndIdx == -1) {
                   errln("foo");  // TODO:
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
                    UTF16.append(tp.dataToBreak, c);
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

            if (c == CH_LT) {
                tagValue   = 0;
                parseState = PARSE_NUM;
                break;
            }

            if (c == CH_HASH && column==3) {   // TODO:  why is column off so far?
                parseState = PARSE_COMMENT;
                savedState = PARSE_DATA;
                break;
            }

            if (c == CH_BACKSLASH) {
                // Check for \ at end of line, a line continuation.
                //     Advance over (discard) the newline
                int cp = UTF16.charAt(testString, charIdx); 
                if (cp == CH_CR && charIdx<len && UTF16.charAt(testString, charIdx+1) == CH_LF) {
                    // We have a CR LF
                    //  Need an extra increment of the input ptr to move over both of them
                    charIdx++;
                }
                if (cp == CH_LF || cp == CH_CR) {
                    lineNum++;
                    colStart = charIdx;
                    charIdx++;
                    break;
                }

                // Let unescape handle the back slash.
                int  charIdxAr[] = new int[1];
                charIdxAr[0] = charIdx;
                cp = Utility.unescapeAt(testString, charIdxAr);
                if (cp != -1) {
                    // Escape sequence was recognized.  Insert the char
                    //   into the test data.
                    charIdx = charIdxAr[0];
                    UTF16.append(tp.dataToBreak, cp);
                    for (i=tp.dataToBreak.length()-1; i>=0 && tp.srcLine[i]==0; i--) {
                        tp.srcLine[i] = lineNum;
                        tp.srcCol[i]  = column;
                    }
                    
                    break;
                }


                // Not a recognized backslash escape sequence.
                // Take the next char as a literal.
                //  TODO:  Should this be an error?
                c = UTF16.charAt(testString,charIdx);
                charIdx = UTF16.moveCodePointOffset(testString, charIdx, 1);
             }

            // Normal, non-escaped data char.
            UTF16.append(tp.dataToBreak, c);
 
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

            if (c == CH_GT) {
                // Finished the number.  Add the info to the expected break data,
                //   and switch parse state back to doing plain data.
                parseState = PARSE_DATA;
                if (tagValue == 0) {
                    tagValue = -1;
                }
                int  breakIdx = tp.dataToBreak.length();
                tp.expectedBreaks[breakIdx] = tagValue;
                tp.srcLine[breakIdx]        = lineNum;
                tp.srcCol[breakIdx]         = column;
                break;
            }

            if (UCharacter.isDigit(c)) {
                tagValue = tagValue*10 + UCharacter.digit(c);
                break;
            }

            errln("Syntax Error in test file at line "+ lineNum +", col %d" + column);
            return;
            
            // parseState = PARSE_COMMENT;   // TODO: unreachable.  Don't stop on errors.
            // break;
        }


 
    }
}

void executeTest(TestParams t) {
    int    bp;
    int    prevBP;
    int    i;

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
            int rs = ((RuleBasedBreakIterator)t.bi).getRuleStatus();
            if (rs != expectedTagVal) {
                errln("Incorrect status for forward break.  Pos=  " + bp +
                        "File line,col= " + t.srcLine[bp] + ", " + t.srcCol[bp] + "\n" +
                      "          Actual, Expected status = " + rs + ", " + expectedTagVal);
            }
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

        // Check that there were we didn't miss an expected break between the last one
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
            int rs = ((RuleBasedBreakIterator)t.bi).getRuleStatus();
            if (rs != expectedTagVal) {
                errln("Incorrect status for reverse break.  Pos=  " + bp +
                        "File line,col= " + t.srcLine[bp] + ", " + t.srcCol[bp] + "\n" +
                      "          Actual, Expected status = " + rs + ", " + expectedTagVal);
                  }
        }

        prevBP = bp;
    }

    // Verify that there were no missed breaks prior to the last one found
    for (i=prevBP-1; i>=0; i--) {
        if (t.expectedBreaks[i] != 0) {
            errln("Forward Itertion, break expected, but not found.  Pos=" + i +
                    "  File line,col= " + t.srcLine[i] + ", " + t.srcCol[i]);
         }
    }
}




}
