/*
 * Created on Apr 23, 2004
 *
 */
package com.ibm.icu.dev.test.rbbi;


// Monkey testing of RuleBasedBreakIterator
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.UCharacterIterator;
import com.ibm.icu.impl.StringUCharacterIterator;
import com.ibm.icu.text.UnicodeSet;
import java.text.CharacterIterator;
import java.util.List;
import java.util.ArrayList;



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
        StringUCharacterIterator  fCI;


    RBBICharMonkey() {
    	fText       = null;
        fCI         = new StringUCharacterIterator();
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
        fCI.setText(fText);
        
    }


    int  next(int i) {
        int  retVal;
        fCI.setIndex(i);
        CINextGC(fCI);
        retVal = fCI.getIndex();
        if (retVal == UCharacterIterator.DONE) {
            retVal = -1;   
        }
        return retVal;
    }


    List  charClasses() {
        return fSets;
    }
    }

 
    private static UnicodeSet GC_Control =
         new UnicodeSet("[[:Zl:][:Zp:][:Cc:][:Cf:]-[\\u000d\\u000a]]");
    
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
    
    
    private static int CINextGC(UCharacterIterator ci) {
    	int    pos = ci.getIndex();
    	int    c = ci.nextCodePoint();
    	int    c2;
    	
    	if (c == 0x0d) {
    		c2 = ci.currentCodePoint();
    		if (c2 == 0x0a) {
    			ci.nextCodePoint();
    			return pos;
    		}
    	}
    	if (GC_Control.contains(c)) {
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
    		c = ci.nextCodePoint();           
    	}  // end of loop
    	
    	if (hangulState != 1) {
    		// We found a Hangul.  We're done.
    		return pos;
    	}
    	
    	// Ordinary characters.  Consume any following Extends.
    	for (;;) {
    		c2 = ci.currentCodePoint();
    		if (GC_Extend.contains(c2) == false) {
    			break;
    		}
    		ci.nextCodePoint();  
    	}
    	
    	return pos;   
    }
    
    public void TestCharMonkey() {
        logln("Hello from CharMonkeyTest");
    }

}
