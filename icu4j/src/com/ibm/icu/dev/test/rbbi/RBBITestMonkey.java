/*
 * Created on Apr 23, 2004
 *
 */
package com.ibm.icu.dev.test.rbbi;


// Monkey testing of RuleBasedBreakIterator
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.RuleBasedBreakIterator_New;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.impl.StringUCharacterIterator;
import com.ibm.icu.text.UnicodeSet;
import java.text.CharacterIterator;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Locale;


/**
 * @author andy
 *
 * TODO To change the template for this generated type comment go to 
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class RBBITestMonkey extends TestFmwk {
    
	public static void main(String[] args) {
        new RBBITestMonkey().run(args);
	}
    
//
//     classs RBBIMonkeyKind
//
//        Monkey Test for Break Iteration
//        Abstract interface class.   Concrete derived classes independently
//        implement the break rules for different iterator types.
//
//        The Monkey Test itself uses doesn't know which type of break iterator it is
//        testing, but works purely in terms of the interface defined here.
//
    abstract static class RBBIMonkeyKind {
    
        // Return a List of UnicodeSets, representing the character classes used
        //   for this type of iterator.
        abstract  List  charClasses();

        // Set the test text on which subsequent calls to next() will operate
        abstract  void   setText(String text);

        // Find the next break postion, starting from the specified position.
        // Return -1 after reaching end of string.
        abstract   int   next(int i);
    }

 
    /**
     * Monkey test subclass for testing Character (Grapheme Cluster) boundaries.
     */
    static class RBBICharMonkey extends RBBIMonkeyKind {
        List                      fSets;

        UnicodeSet                fCRLFSet;
        UnicodeSet                fControlSet;
        UnicodeSet                fExtendSet;
        UnicodeSet                fHangulSet;
        UnicodeSet                fAnySet;

        String                    fText;


    RBBICharMonkey() {
    	fText       = null;
        fCRLFSet    = new UnicodeSet("[\\r\\n]");
        fControlSet = new UnicodeSet("[[\\p{Zl}\\p{Zp}\\p{Cc}\\p{Cf}]-[\\n]-[\\r]]");
        fExtendSet  = new UnicodeSet("[\\p{Grapheme_Extend}]");
        fHangulSet  = new UnicodeSet(
            "[\\p{Hangul_Syllable_Type=L}\\p{Hangul_Syllable_Type=L}\\p{Hangul_Syllable_Type=T}" +
             "\\p{Hangul_Syllable_Type=LV}\\p{Hangul_Syllable_Type=LVT}]");
        fAnySet     = new UnicodeSet("[\\u0000-\\U0010ffff]");

        fSets       = new ArrayList();
        fSets.add(fCRLFSet);
        fSets.add(fControlSet);
        fSets.add(fExtendSet);
        fSets.add(fHangulSet);
        fSets.add(fAnySet);
     };


    void setText(String s) {
        fText = s;        
    }
    
    List charClasses() {
        return fSets;
    }
    
    int next(int i) {
        return nextGC(fText, i);
    }
    }


    static class RBBIWordMonkey extends RBBIMonkeyKind {
        List  charClasses() {
         return null;   // TODO:   
        }
        
        void   setText(String text) {  // TODO:
        }   

        int   next(int i) {      // TODO:  
            return 0;
        }
    
    }

 
    static class RBBILineMonkey extends RBBIMonkeyKind {
        List  charClasses() {
         return null;   // TODO:   
        }
        
        void   setText(String text) {  // TODO:
        }   

        int   next(int i) {      // TODO:    
            return 0;
        }
    
    }

    /**
     * return the index of the next code point in the input text.
     * @param i the preceding index
     * @return
     * @internal
     */
    static int  nextCP(String s, int i) {
        if (i == -1) {
            // End of Input indication.  Continue to return end value.
            return -1;
        }
        int  retVal = i + 1;
        if (retVal > s.length()) {
            return -1;
        }
        int  c = UTF16.charAt(s, i);
        if (c >= UTF16.SUPPLEMENTARY_MIN_VALUE) {
            retVal++;
        }
        return retVal;
    }


 
    private static UnicodeSet GC_Control =
         new UnicodeSet("[[:Zl:][:Zp:][:Cc:][:Cf:]-[\\u000d\\u000a]-[:Grapheme_Extend:]]");
    
    private static UnicodeSet GC_Extend = 
        new UnicodeSet("[[:Grapheme_Extend:]]");
    
    private static UnicodeSet GC_L = 
        new UnicodeSet("[[:Hangul_Syllable_Type=L:]]");
    
    private static UnicodeSet GC_V = 
        new UnicodeSet("[[:Hangul_Syllable_Type=V:]]");
    
    private static UnicodeSet GC_T = 
        new UnicodeSet("[[:Hangul_Syllable_Type=T:]]");
    
    private static UnicodeSet GC_LV = 
        new UnicodeSet("[[:Hangul_Syllable_Type=LV:]]");
    
    private static UnicodeSet GC_LVT = 
        new UnicodeSet("[[:Hangul_Syllable_Type=LVT:]]");
    
    /**
     * Find the end of the extent of a grapheme cluster.
     * This is the reference implementation used by the monkey test for comparison
     * with the RBBI results.
     * @param s  The string containing the text to be analyzed  
     * @param i  The index of the start of the grapheme cluster.
     * @return   The index of the first code point following the grapheme cluster
     * @internal
     */
    private static int nextGC(String s, int i) {
        if (i >= s.length() || i == -1 ) {
            return -1;
        }

    	int  c = UTF16.charAt(s, i);
        int  pos = i;
    	
    	if (c == 0x0d) {
    	    pos = nextCP(s, i);
            if (pos >= s.length()) {
                return pos;
            }
            c = UTF16.charAt(s, pos);
    		if (c == 0x0a) {
    		    pos = nextCP(s, pos);
            }
            return pos;
    	}
        
    	if (GC_Control.contains(c) || c == 0x0a) {
            pos = nextCP(s, pos);
    		return pos;   
    	}
    	
    	// Little state machine to consume Hangul Syllables
    	int  hangulState = 1;
    	state_loop: for (;;) {
    		switch (hangulState) {
    			case 1:
    				if (GC_L.contains(c)) {
                        hangulState = 2;
    					break;
    				}
    				if (GC_V.contains(c) || GC_LV.contains(c)) {
                        hangulState = 3;
    					break;
    				}
    				if (GC_T.contains(c) || GC_LVT.contains(c)) {
                        hangulState = 4;
    					break;
    				}
    				break state_loop;
    			case 2:
    				if (GC_L.contains(c)) {
    					// continue in state 2.
    					break;
    				}
    				if (GC_V.contains(c) || GC_LV.contains(c)) {
                        hangulState = 3;
    					break;
    				}
                    if (GC_LVT.contains(c)) {
                        hangulState = 4;
                        break;
                    }
    				if (GC_Extend.contains(c)) {
                        hangulState = 5;
    					break;
    				}
    				break state_loop;
    			case 3:
    				if (GC_V.contains(c)) {
    					// continue in state 3;
    					break;
    				}
    				if (GC_T.contains(c)) {
                        hangulState = 4;
    					break;
    				}
    				if (GC_Extend.contains(c)) {
                        hangulState = 5;
    					break;
    				}
    				break state_loop;
    			case 4:
    				if (GC_T.contains(c)) {
    					// continue in state 4
    					break;
    				}
    				if (GC_Extend.contains(c)) {
                        hangulState = 5;
    					break;
    				}
    				break state_loop;
    			case 5:
    				if (GC_Extend.contains(c)) {
    				    hangulState = 5;
    				    break; 
    				}
    				break state_loop;
    		}
    		// We have exited the switch statement, but are still in the loop.
    		// Still in a Hangul Syllable, advance to the next code point.
            pos = nextCP(s, pos); 
            if (pos >= s.length()) {
                break;
            }
    		c = UTF16.charAt(s, pos);    
    	}  // end of loop
    	
    	if (hangulState != 1) {
    		// We found a Hangul.  We're done.
    		return pos;
    	}
    	
    	// Ordinary characters.  Consume one codepoint unconditionally, then any following Extends.
    	for (;;) {
            pos = nextCP(s, pos); 
            if (pos >= s.length()) {
                break;
            }
            c = UTF16.charAt(s, pos);    
            if (GC_Extend.contains(c) == false) {
                break;
            }
    	}
    	
    	return pos;   
    }
    
    
    /**
     * random number generator.  Not using Java's built-in Randoms for two reasons:
     *    1.  Using this code allows obtaining the same sequences as those from the ICU4C monkey test.
     *    2.  We need to get and restore the seed from values occuring in the middle
     *        of a long sequence, to more easily reproduce failing cases.
     */
    private static int m_seed = 1;
    private static int  m_rand()
    {
        m_seed = m_seed * 1103515245 + 12345;
        return (int)(m_seed >>> 16) % 32768;
    }

    
/**
 *  Run a RBBI monkey test.  Common routine, for all break iterator types.
 *    Parameters:
 *       bi      - the break iterator to use
 *       mk      - MonkeyKind, abstraction for obtaining expected results
 *       name    - Name of test (char, word, etc.) for use in error messages
 *       seed    - Seed for starting random number generator (parameter from user)
 *       numIterations
 */
void RunMonkey(BreakIterator  bi, RBBIMonkeyKind mk, String name, int  seed, int numIterations) {
    int              TESTSTRINGLEN = 500;
    StringBuffer     testText         = new StringBuffer();
    int              numCharClasses;
    List             chClasses;
    int[]            expected         = new int[TESTSTRINGLEN*2 + 1];
    int              expectedCount    = 0;
    boolean[]        expectedBreaks   = new boolean[TESTSTRINGLEN*2 + 1];
    boolean[]        forwardBreaks    = new boolean[TESTSTRINGLEN*2 + 1];
    boolean[]        reverseBreaks    = new boolean[TESTSTRINGLEN*2 + 1];
    boolean[]        isBoundaryBreaks = new boolean[TESTSTRINGLEN*2 + 1];
    int              i;
    int              loopCount        = 0;
    boolean          printTestData    = false;
    boolean          printBreaksFromBI = false;

    m_seed = seed;

    numCharClasses = mk.charClasses().size();
    chClasses      = mk.charClasses();

    // Verify that the character classes all have at least one member.
    for (i=0; i<numCharClasses; i++) {
        UnicodeSet s = (UnicodeSet)chClasses.get(i);
        if (s == null || s.size() == 0) {
            errln("Character Class " + i + " is null or of zero size.");
            return;
        }
    }

    //--------------------------------------------------------------------------------------------
    //
    //  Debugging settings.  Comment out everything in the following block for normal operation
    //
    //--------------------------------------------------------------------------------------------
    // numIterations = -1;  
    //RuleBasedBreakIterator_New.fTrace = true;
    //m_seed = -1324359431;
    // TESTSTRINGLEN = 50;
    // printTestData = true;
    // printBreaksFromBI = true;
    // ((RuleBasedBreakIterator_New)bi).dump();
    
    //--------------------------------------------------------------------------------------------
    //
    //  End of Debugging settings.  
    //
    //--------------------------------------------------------------------------------------------
    
    int  dotsOnLine = 0;
    while (loopCount < numIterations || numIterations == -1) {
        if (numIterations == -1 && loopCount % 10 == 0) {
            // If test is running in an infinite loop, display a periodic tic so
            //   we can tell that it is making progress.
            System.out.print(".");
            if (dotsOnLine++ >= 80){
                System.out.println();
                dotsOnLine = 0;
            }
        }
        // Save current random number seed, so that we can recreate the random numbers
        //   for this loop iteration in event of an error.
        seed = m_seed;

        testText.setLength(0);
        // Populate a test string with data.
        if (printTestData) {
            System.out.println("Test Data string ..."); 
        }
        for (i=0; i<TESTSTRINGLEN; i++) {
            int        aClassNum = m_rand() % numCharClasses;
            UnicodeSet classSet  = (UnicodeSet)chClasses.get(aClassNum);
            int        charIdx   = m_rand() % classSet.size();
            int        c         = classSet.charAt(charIdx);
            if (c < 0) {   // TODO:  deal with sets containing strings.
                errln("c < 0");
            }
            UTF16.appendCodePoint(testText, c);
            if (printTestData) {
            	System.out.print(Integer.toHexString(c) + " ");
            }
        }
        if (printTestData) {
        	System.out.println(); 
        }

        Arrays.fill(expected, 0);
        Arrays.fill(expectedBreaks, false);
        Arrays.fill(forwardBreaks, false);
        Arrays.fill(reverseBreaks, false);
        Arrays.fill(isBoundaryBreaks, false);
 
        // Calculate the expected results for this test string.
        mk.setText(testText.toString());
        expectedCount = 0;
        expectedBreaks[0] = true;
        expected[expectedCount ++] = 0;
        int breakPos = 0;
        for (;;) {
            breakPos = mk.next(breakPos);
            if (breakPos == -1) {
                break;
            }
            if (breakPos > testText.length()) {
                errln("breakPos > testText.length()");
            }
            expectedBreaks[breakPos] = true;
            expected[expectedCount ++] = breakPos;
        }

        // Find the break positions using forward iteration
        if (printBreaksFromBI) {
        	System.out.println("Breaks from BI...");  
        }
        bi.setText(testText.toString());
        for (i=bi.first(); i != BreakIterator.DONE; i=bi.next()) {
            if (i < 0 || i > testText.length()) {
                errln(name + " break monkey test: Out of range value returned by breakIterator::next()");
                break;
            }
            if (printBreaksFromBI) {
                System.out.print(Integer.toHexString(i) + " ");
            }
            forwardBreaks[i] = true;
        }
        if (printBreaksFromBI) {
        	System.out.println();
        }

        // Find the break positions using reverse iteration
        for (i=bi.last(); i != BreakIterator.DONE; i=bi.previous()) {
            if (i < 0 || i > testText.length()) {
                errln(name + " break monkey test: Out of range value returned by breakIterator.next()" + name);
                break;
            }
            reverseBreaks[i] = true;
        }

        // Find the break positions using isBoundary() tests.
        for (i=0; i<=testText.length(); i++) {
            isBoundaryBreaks[i] = bi.isBoundary(i);
        }


        // Compare the expected and actual results.
        for (i=0; i<=testText.length(); i++) {
            String errorType = null;
            if  (forwardBreaks[i] != expectedBreaks[i]) {
                errorType = "next()";
            } else if (reverseBreaks[i] != forwardBreaks[i]) {
                errorType = "previous()";
            } else if (isBoundaryBreaks[i] != expectedBreaks[i]) {
                errorType = "isBoundary()";
            }


            if (errorType != null) {
                // Format a range of the test text that includes the failure as
                //  a data item that can be included in the rbbi test data file.

                // Start of the range is the last point where expected and actual results
                //   both agreed that there was a break position.
                int startContext = i;
                int count = 0;
                for (;;) {
                    if (startContext==0) { break; }
                    startContext --;
                    if (expectedBreaks[startContext]) {
                        if (count == 2) break;
                        count ++;
                    }
                }

                // End of range is two expected breaks past the start position.
                int endContext = i + 1;
                int ci;
                for (ci=0; ci<2; ci++) {  // Number of items to include in error text.
                    for (;;) {
                        if (endContext >= testText.length()) {break;}
                        if (expectedBreaks[endContext-1]) { 
                            if (count == 0) break;
                            count --;
                        }
                        endContext ++;
                    }
                }

                // Format looks like   "<data><>\uabcd\uabcd<>\U0001abcd...</data>"
                StringBuffer errorText = new StringBuffer();
                errorText.append("<data>");

                String hexChars = "0123456789abcdef";
                int      c;    // Char from test data
                int      bn;
                String   testData = testText.toString();
                for (ci = startContext;  ci <= endContext && ci != -1;  ci = nextCP(testData, ci)) {
                    if (ci == i) {
                        // This is the location of the error.
                        errorText.append("<?>");
                    } else if (expectedBreaks[ci]) {
                        // This a non-error expected break position.
                        errorText.append("<>");
                    }
                    if (ci < testData.length()) {
                    	c = UTF16.charAt(testData, ci);
                    	if (c < 0x10000) {
                    		errorText.append("\\u");
                    		for (bn=12; bn>=0; bn-=4) {
                    			errorText.append(hexChars.charAt((((int)c)>>bn)&0xf));
                    		}
                    	} else {
                    		errorText.append("\\U");
                    		for (bn=28; bn>=0; bn-=4) {
                    			errorText.append(hexChars.charAt((((int)c)>>bn)&0xf));
                    		}
                    	}
                    }
                }
                if (ci == testData.length() && ci != -1) {
                	errorText.append("<>");
                }
                errorText.append("</data>\n");

                // Output the error
                errln(name + " break monkey test error.  " + 
                     (expectedBreaks[i]? "Break expected but not found." : "Break found but not expected.") +
                      "\nOperation = " + errorType + "; random seed = " + seed + ";  buf Idx = " + i + "\n" +
                      errorText);
                break;
            }
        }

        loopCount++;
    }
}

public void TestCharMonkey() {
    
    int        loopCount = 500;
    int        seed      = 1;
    String     breakType = "all";
    
    if (params.inclusion >= 9) {
        loopCount = 10000;
    }
    
    RBBICharMonkey  m = new RBBICharMonkey();
    BreakIterator   bi = BreakIterator.getCharacterInstance(Locale.US);
    RunMonkey(bi, m, "char", seed, loopCount);
}

public void TestWordMonkey() {
    
    int        loopCount = 500;
    int        seed      = 1;
    String     breakType = "all";
    
    if (params.inclusion >= 9) {
        loopCount = 10000;
    }
    
    logln("Word Break Monkey Test");
    RBBIWordMonkey  m = new RBBIWordMonkey();
    BreakIterator   bi = BreakIterator.getWordInstance(Locale.US);
    //RunMonkey(bi, m, "word", seed, loopCount);
}

public void TestLineMonkey() {
    
    int        loopCount = 500;
    int        seed      = 1;
    String     breakType = "all";
    
    if (params.inclusion >= 9) {
        loopCount = 10000;
    }
    
    logln("Line Break Monkey Test");
    RBBILineMonkey  m = new RBBILineMonkey();
    BreakIterator   bi = BreakIterator.getLineInstance(Locale.US);
    if (params == null) {
        loopCount = 50;
    }
    // RunMonkey(bi, m, "line", seed, loopCount);
}

}

