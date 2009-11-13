/*
 *******************************************************************************
 * Copyright (C) 2003-2009 International Business Machines Corporation and     *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
 package com.ibm.icu.dev.test.rbbi;


// Monkey testing of RuleBasedBreakIterator
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;


/**
 * Monkey tests for RBBI.  These tests have independent implementations of
 * the Unicode TR boundary rules, and compare results between these and ICU's
 * implementation, using random data.
 * 
 * Tests cover Grapheme Cluster (char), Word and Line breaks
 * 
 * Ported from ICU4C, original code in file source/test/intltest/rbbitst.cpp
 *
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
        abstract  void   setText(StringBuffer text);

        // Find the next break postion, starting from the specified position.
        // Return -1 after reaching end of string.
        abstract   int   next(int i);
        
        // A Character Property, one of the constants defined in class UProperty.
        //   The value fo this property will be displayed for the characters
        //    near any test failure.  
        int   fCharProperty;
    }

 
    /**
     * Monkey test subclass for testing Character (Grapheme Cluster) boundaries.
     */
    static class RBBICharMonkey extends RBBIMonkeyKind {
        List                      fSets;

        UnicodeSet                fCRLFSet;
        UnicodeSet                fControlSet;
        UnicodeSet                fExtendSet;
        UnicodeSet                fPrependSet;
        UnicodeSet                fSpacingSet;
        UnicodeSet                fLSet;
        UnicodeSet                fVSet;
        UnicodeSet                fTSet;
        UnicodeSet                fLVSet;
        UnicodeSet                fLVTSet;
        UnicodeSet                fHangulSet;
        UnicodeSet                fAnySet;

        StringBuffer              fText;


    RBBICharMonkey() {
        fText       = null;
        fCharProperty = UProperty.GRAPHEME_CLUSTER_BREAK;
        fCRLFSet    = new UnicodeSet("[\\r\\n]");
        fControlSet = new UnicodeSet("[\\p{Grapheme_Cluster_Break = Control}]");
        fExtendSet  = new UnicodeSet("[\\p{Grapheme_Cluster_Break = Extend}]");
        fPrependSet = new UnicodeSet("[\\p{Grapheme_Cluster_Break = Prepend}]");
        fSpacingSet = new UnicodeSet("[\\p{Grapheme_Cluster_Break = SpacingMark}]");
        fLSet       = new UnicodeSet("[\\p{Grapheme_Cluster_Break = L}]");
        fVSet       = new UnicodeSet("[\\p{Grapheme_Cluster_Break = V}]");
        fTSet       = new UnicodeSet("[\\p{Grapheme_Cluster_Break = T}]");
        fLVSet      = new UnicodeSet("[\\p{Grapheme_Cluster_Break = LV}]");
        fLVTSet     = new UnicodeSet("[\\p{Grapheme_Cluster_Break = LVT}]");
        fHangulSet  = new UnicodeSet();
        fHangulSet.addAll(fLSet);
        fHangulSet.addAll(fVSet);
        fHangulSet.addAll(fTSet);
        fHangulSet.addAll(fLVSet);
        fHangulSet.addAll(fLVTSet);

        fAnySet     = new UnicodeSet("[\\u0000-\\U0010ffff]");

        fSets       = new ArrayList();
        fSets.add(fCRLFSet);
        fSets.add(fControlSet);
        fSets.add(fExtendSet);
        fSets.add(fPrependSet);
        fSets.add(fSpacingSet);
        fSets.add(fHangulSet);
        fSets.add(fAnySet);
     }


    void setText(StringBuffer s) {
        fText = s;
    }
    
    List charClasses() {
        return fSets;
    }
    
    int next(int prevPos) {
        int    p1, p2, p3;    // Indices of the significant code points around the
                              //   break position being tested.  The candidate break
                              //   location is before p2.
    
        int     breakPos = -1;
    
        int   c1, c2, c3;     // The code points at p0, p1, p2 & p3.
        
        // Previous break at end of string.  return DONE.
        if (prevPos >= fText.length()) {
            return -1;
        }
        p1 = p2 = p3 = prevPos;
        c3 =  UTF16.charAt(fText, prevPos);
        c1 = c2 = 0;
    
        // Loop runs once per "significant" character position in the input text.
        for (;;) {
            // Move all of the positions forward in the input string.
            p1 = p2;  c1 = c2;
            p2 = p3;  c2 = c3;
    
            // Advance p3 by one codepoint
            p3 = moveIndex32(fText, p3, 1);
            c3 = (p3>=fText.length())? -1: UTF16.charAt(fText, p3);
    
            if (p1 == p2) {
                // Still warming up the loop.  (won't work with zero length strings, but we don't care)
                continue;
            }
            if (p2 == fText.length()) {
                // Reached end of string.  Always a break position.
                break;
            }
    
            // Rule  GB3   CR x LF
            //     No Extend or Format characters may appear between the CR and LF,
            //     which requires the additional check for p2 immediately following p1.
            //
            if (c1==0x0D && c2==0x0A && p1==(p2-1)) {
                continue;
            }
    
            // Rule (GB4).   ( Control | CR | LF ) <break>
            if (fControlSet.contains(c1) ||
                c1 == 0x0D ||
                c1 == 0x0A)  {
                break;
            }
    
            // Rule (GB5)    <break>  ( Control | CR | LF )
            //
            if (fControlSet.contains(c2) ||
                c2 == 0x0D ||
                c2 == 0x0A)  {
                break;
            }
    
    
            // Rule (GB6)  L x ( L | V | LV | LVT )
            if (fLSet.contains(c1) &&
                (fLSet.contains(c2)  ||
                    fVSet.contains(c2)  ||
                    fLVSet.contains(c2) ||
                    fLVTSet.contains(c2))) {
                continue;
            }
    
            // Rule (GB7)    ( LV | V )  x  ( V | T )
            if ((fLVSet.contains(c1) || fVSet.contains(c1)) &&
                (fVSet.contains(c2) || fTSet.contains(c2)))  {
                continue;
            }
    
            // Rule (GB8)    ( LVT | T)  x T
            if ((fLVTSet.contains(c1) || fTSet.contains(c1)) &&
                fTSet.contains(c2))  {
                continue;
            }
    
            // Rule (GB9)    Numeric x ALetter
            if (fExtendSet.contains(c2))  {
                continue;
            }
    
            // Rule (GB9a)   x  SpacingMark
            if (fSpacingSet.contains(c2)) {
                continue;
            }
    
            // Rule (GB9b)   Prepend x
            if (fPrependSet.contains(c1)) {
                continue;
            }
    
            // Rule (GB10)  Any  <break>  Any
            break;
        }
    
        breakPos = p2;
        return breakPos;
        }
    }


    /**
     * 
     * Word Monkey Test Class
     *
     * 
     * 
     */
    static class RBBIWordMonkey extends RBBIMonkeyKind {
        List                      fSets;
        StringBuffer              fText;

        UnicodeSet                fCRSet;
        UnicodeSet                fLFSet;
        UnicodeSet                fNewlineSet;
        UnicodeSet                fKatakanaSet;
        UnicodeSet                fALetterSet;
        UnicodeSet                fMidNumLetSet;
        UnicodeSet                fMidLetterSet;
        UnicodeSet                fMidNumSet;
        UnicodeSet                fNumericSet;
        UnicodeSet                fFormatSet;
        UnicodeSet                fExtendSet;
        UnicodeSet                fExtendNumLetSet;
        UnicodeSet                fOtherSet;

        
        RBBIWordMonkey() {
            fCharProperty    = UProperty.WORD_BREAK;

            fCRSet           = new UnicodeSet("[\\p{Word_Break = CR}]");
            fLFSet           = new UnicodeSet("[\\p{Word_Break = LF}]");
            fNewlineSet      = new UnicodeSet("[\\p{Word_Break = Newline}]");
            fALetterSet      = new UnicodeSet("[\\p{Word_Break = ALetter}]");
            fKatakanaSet     = new UnicodeSet("[\\p{Word_Break = Katakana}]");
            fMidNumLetSet    = new UnicodeSet("[\\p{Word_Break = MidNumLet}]");
            fMidLetterSet    = new UnicodeSet("[\\p{Word_Break = MidLetter}]");
            fMidNumSet       = new UnicodeSet("[\\p{Word_Break = MidNum}]");
            fNumericSet      = new UnicodeSet("[\\p{Word_Break = Numeric}]");
            fFormatSet       = new UnicodeSet("[\\p{Word_Break = Format}]");
            fExtendNumLetSet = new UnicodeSet("[\\p{Word_Break = ExtendNumLet}]");
            fExtendSet       = new UnicodeSet("[\\p{Word_Break = Extend}]");

            fOtherSet        = new UnicodeSet();
            fOtherSet.complement();
            fOtherSet.removeAll(fCRSet);
            fOtherSet.removeAll(fLFSet);
            fOtherSet.removeAll(fNewlineSet);
            fOtherSet.removeAll(fALetterSet);
            fOtherSet.removeAll(fKatakanaSet);
            fOtherSet.removeAll(fMidLetterSet);
            fOtherSet.removeAll(fMidNumSet);
            fOtherSet.removeAll(fNumericSet);
            fOtherSet.removeAll(fFormatSet);
            fOtherSet.removeAll(fExtendSet);
            fOtherSet.removeAll(fExtendNumLetSet);
            // Inhibit dictionary characters from being tested at all.
            fOtherSet.removeAll(new UnicodeSet("[\\p{LineBreak = Complex_Context}]"));

            fSets            = new ArrayList();
            fSets.add(fCRSet);
            fSets.add(fLFSet);
            fSets.add(fNewlineSet);
            fSets.add(fALetterSet);
            fSets.add(fKatakanaSet);
            fSets.add(fMidLetterSet);
            fSets.add(fMidNumLetSet);
            fSets.add(fMidNumSet);
            fSets.add(fNumericSet);
            fSets.add(fFormatSet);
            fSets.add(fExtendSet);
            fSets.add(fExtendNumLetSet);
            fSets.add(fOtherSet);
        }
        
        
        List  charClasses() {
         return fSets;  
        }
        
        void   setText(StringBuffer s) { 
            fText = s;        
        }   

        int   next(int prevPos) {  
            int    /*p0,*/ p1, p2, p3;      // Indices of the significant code points around the 
                                        //   break position being tested.  The candidate break
                                        //   location is before p2.
            int     breakPos = -1;
            
            int c0, c1, c2, c3;   // The code points at p0, p1, p2 & p3.
            
            // Previous break at end of string.  return DONE.
            if (prevPos >= fText.length()) {
                return -1;
            }
            /*p0 =*/ p1 = p2 = p3 = prevPos;
            c3 = UTF16.charAt(fText, prevPos);
            c0 = c1 = c2 = 0;
            
            

            // Loop runs once per "significant" character position in the input text.
            for (;;) {
                // Move all of the positions forward in the input string.
                /*p0 = p1;*/  c0 = c1;
                p1 = p2;  c1 = c2;
                p2 = p3;  c2 = c3;
                
                // Advancd p3 by    X(Extend | Format)*   Rule 4
                //    But do not advance over Extend & Format following a new line. (Unicode 5.1 change)
                do {
                    p3 = moveIndex32(fText, p3, 1);
                    c3 = -1;
                    if (p3>=fText.length()) {
                        break;
                    }
                    c3 = UTF16.charAt(fText, p3);
                    if (fCRSet.contains(c2) || fLFSet.contains(c2) || fNewlineSet.contains(c2)) {
                        break;
                    }
                }
                while (setContains(fFormatSet, c3) || setContains(fExtendSet, c3));

                if (p1 == p2) {
                    // Still warming up the loop.  (won't work with zero length strings, but we don't care)
                    continue;
                }
                if (p2 == fText.length()) {
                    // Reached end of string.  Always a break position.
                    break;
                }

                // Rule (3)   CR x LF
                //     No Extend or Format characters may appear between the CR and LF,
                //     which requires the additional check for p2 immediately following p1.
                //
                if (c1==0x0D && c2==0x0A) {
                    continue;
                }
                
                // Rule (3a)  Break before and after newlines (including CR and LF)
                //
                if (fCRSet.contains(c1) || fLFSet.contains(c1) || fNewlineSet.contains(c1)) {
                    break;
                }
                if (fCRSet.contains(c2) || fLFSet.contains(c2) || fNewlineSet.contains(c2)) {
                    break;
                }

                // Rule (5).   ALetter x ALetter
                if (fALetterSet.contains(c1) &&
                        fALetterSet.contains(c2))  {
                    continue;
                }
                
                // Rule (6)  ALetter  x  (MidLetter | MidNumLet)  ALetter
                //
                if ( fALetterSet.contains(c1) &&
                        (fMidLetterSet.contains(c2) || fMidNumLetSet.contains(c2)) &&
                        setContains(fALetterSet, c3)) {
                    continue;
                }
                
                
                // Rule (7)  ALetter (MidLetter | MidNumLet)   x  ALetter
                if (fALetterSet.contains(c0) &&
                        (fMidLetterSet.contains(c1) ||  fMidNumLetSet.contains(c1))  &&
                        fALetterSet.contains(c2)) {
                    continue;
                }
                
                //  Rule (8)    Numeric x Numeric
                if (fNumericSet.contains(c1) &&
                        fNumericSet.contains(c2))  {
                    continue;
                }
                
                // Rule (9)    ALetter x Numeric
                if (fALetterSet.contains(c1) &&
                        fNumericSet.contains(c2))  {
                    continue;
                }

                // Rule (10)    Numeric x ALetter
                if (fNumericSet.contains(c1) &&
                        fALetterSet.contains(c2))  {
                    continue;
                }
                
                // Rule (11)   Numeric (MidNum | MidNumLet)  x  Numeric
                if ( fNumericSet.contains(c0) &&
                        (fMidNumSet.contains(c1) || fMidNumLetSet.contains(c1))  && 
                        fNumericSet.contains(c2)) {
                    continue;
                }
                
                // Rule (12)  Numeric x (MidNum | MidNumLet) Numeric
                if (fNumericSet.contains(c1) &&
                        (fMidNumSet.contains(c2) || fMidNumLetSet.contains(c2)) &&
                        setContains(fNumericSet, c3)) {
                    continue;
                }
                
                // Rule (13)  Katakana x Katakana
                if (fKatakanaSet.contains(c1) &&
                        fKatakanaSet.contains(c2))  {
                    continue;
                }
                
                // Rule 13a  (ALetter | Numeric | Katakana | ExtendNumLet) x ExtendNumLet
                if ((fALetterSet.contains(c1) || fNumericSet.contains(c1) ||
                        fKatakanaSet.contains(c1) || fExtendNumLetSet.contains(c1)) &&
                        fExtendNumLetSet.contains(c2)) {
                    continue;
                }
                // Rule 13b   ExtendNumLet x (ALetter | Numeric | Katakana | ExtendNumLet)
                if (fExtendNumLetSet.contains(c1) &&
                        (fALetterSet.contains(c2) || fNumericSet.contains(c2) ||
                        fKatakanaSet.contains(c2) || fExtendNumLetSet.contains(c2))) {
                    continue;
                }
               
                // Rule 14.  Break found here.
                break;
            }
            
            breakPos = p2;
            return breakPos;
        }
        
    }

 
    // TODO:  for class fCP, fCL: when Unicode 5.2 properties become available, change the definitions of
    //        these classes to use them.
    static class RBBILineMonkey extends RBBIMonkeyKind {
        
        List        fSets;
        
        UnicodeSet  fBK;
        UnicodeSet  fCR;
        UnicodeSet  fLF;
        UnicodeSet  fCM;
        UnicodeSet  fNL;
        UnicodeSet  fSG;
        UnicodeSet  fWJ;
        UnicodeSet  fZW;
        UnicodeSet  fGL;
        UnicodeSet  fCB;
        UnicodeSet  fSP;
        UnicodeSet  fB2;
        UnicodeSet  fBA;
        UnicodeSet  fBB;
        UnicodeSet  fHY;
        UnicodeSet  fCL;
        UnicodeSet  fCP;
        UnicodeSet  fEX;
        UnicodeSet  fIN;
        UnicodeSet  fNS;
        UnicodeSet  fOP;
        UnicodeSet  fQU;
        UnicodeSet  fIS;
        UnicodeSet  fNU;
        UnicodeSet  fPO;
        UnicodeSet  fPR;
        UnicodeSet  fSY;
        UnicodeSet  fAI;
        UnicodeSet  fAL;
        UnicodeSet  fID;
        UnicodeSet  fSA;
        UnicodeSet  fJL;
        UnicodeSet  fJV;
        UnicodeSet  fJT;
        UnicodeSet  fH2;
        UnicodeSet  fH3;
        UnicodeSet  fXX;
        
        StringBuffer  fText;
        int           fOrigPositions;
        
        
        
        RBBILineMonkey()
        {
            fCharProperty  = UProperty.LINE_BREAK;
            fSets          = new ArrayList();
            
            fBK    = new UnicodeSet("[\\p{Line_Break=BK}]");
            fCR    = new UnicodeSet("[\\p{Line_break=CR}]");
            fLF    = new UnicodeSet("[\\p{Line_break=LF}]");
            fCM    = new UnicodeSet("[\\p{Line_break=CM}]");
            fNL    = new UnicodeSet("[\\p{Line_break=NL}]");
            fWJ    = new UnicodeSet("[\\p{Line_break=WJ}]");
            fZW    = new UnicodeSet("[\\p{Line_break=ZW}]");
            fGL    = new UnicodeSet("[\\p{Line_break=GL}]");
            fCB    = new UnicodeSet("[\\p{Line_break=CB}]");
            fSP    = new UnicodeSet("[\\p{Line_break=SP}]");
            fB2    = new UnicodeSet("[\\p{Line_break=B2}]");
            fBA    = new UnicodeSet("[\\p{Line_break=BA}]");
            fBB    = new UnicodeSet("[\\p{Line_break=BB}]");
            fHY    = new UnicodeSet("[\\p{Line_break=HY}]");
            fCL    = new UnicodeSet("[[\\p{Line_break=CL}]-[\\u0029\\u005d]]");
            fCP    = new UnicodeSet("[\\u0029\\u005d]");
            // fCP    = new UnicodeSet("[\\p{Line_break=CP}]");
            fEX    = new UnicodeSet("[\\p{Line_break=EX}]");
            fIN    = new UnicodeSet("[\\p{Line_break=IN}]");
            fNS    = new UnicodeSet("[\\p{Line_break=NS}]");
            fOP    = new UnicodeSet("[\\p{Line_break=OP}]");
            fQU    = new UnicodeSet("[\\p{Line_break=QU}]");
            fIS    = new UnicodeSet("[\\p{Line_break=IS}]");
            fNU    = new UnicodeSet("[\\p{Line_break=NU}]");
            fPO    = new UnicodeSet("[\\p{Line_break=PO}]");
            fPR    = new UnicodeSet("[\\p{Line_break=PR}]");
            fSY    = new UnicodeSet("[\\p{Line_break=SY}]");
            fAI    = new UnicodeSet("[\\p{Line_break=AI}]");
            fAL    = new UnicodeSet("[\\p{Line_break=AL}]");
            fID    = new UnicodeSet("[\\p{Line_break=ID}]");
            fSA    = new UnicodeSet("[\\p{Line_break=SA}]");
            fJL    = new UnicodeSet("[\\p{Line_break=JL}]");
            fJV    = new UnicodeSet("[\\p{Line_break=JV}]");
            fJT    = new UnicodeSet("[\\p{Line_break=JT}]");
            fH2    = new UnicodeSet("[\\p{Line_break=H2}]");
            fH3    = new UnicodeSet("[\\p{Line_break=H3}]");
            fSG    = new UnicodeSet("[\\ud800-\\udfff]");
            fXX    = new UnicodeSet("[\\p{Line_break=XX}]");

            
            fAL.addAll(fXX);     // Default behavior for XX is identical to AL
            fAL.addAll(fAI);     // Default behavior for AI is identical to AL
            fAL.addAll(fSA);     // Default behavior for SA is XX, which defaults to AL
            fAL.addAll(fSG);     // Default behavior for SG (unpaired surrogates) is AL
            
            
            
            fSets.add(fBK);
            fSets.add(fCR);
            fSets.add(fLF);
            fSets.add(fCM);
            fSets.add(fNL);
            fSets.add(fWJ);
            fSets.add(fZW);
            fSets.add(fGL);
            fSets.add(fCB);
            fSets.add(fSP);
            fSets.add(fB2);
            fSets.add(fBA);
            fSets.add(fBB);
            fSets.add(fHY);
            fSets.add(fH2);
            fSets.add(fH3);
            fSets.add(fCL);
            fSets.add(fCP);
            fSets.add(fEX);
            fSets.add(fIN);
            fSets.add(fJL);
            fSets.add(fJT);
            fSets.add(fJV);
            fSets.add(fNS);
            fSets.add(fOP);
            fSets.add(fQU);
            fSets.add(fIS);
            fSets.add(fNU);
            fSets.add(fPO);
            fSets.add(fPR);
            fSets.add(fSY);
            fSets.add(fAI);
            fSets.add(fAL);
            fSets.add(fID);
            fSets.add(fWJ);
            fSets.add(fSA);
            fSets.add(fSG);
            
        }
        
        void setText(StringBuffer s) {
            fText       = s;
        }
        
        
        

        int next(int startPos) {
            int    pos;       //  Index of the char following a potential break position
            int    thisChar;  //  Character at above position "pos"
            
            int    prevPos;   //  Index of the char preceding a potential break position
            int    prevChar;  //  Character at above position.  Note that prevChar
                              //   and thisChar may not be adjacent because combining
                              //   characters between them will be ignored.
            
            int    nextPos;   //  Index of the next character following pos.
                              //     Usually skips over combining marks.
            int    tPos;      //  temp value.
            int    matchVals[]  = null;       // Number  Expression Match Results
 
            
            if (startPos >= fText.length()) {
                return -1;
            }
            
            
            // Initial values for loop.  Loop will run the first time without finding breaks,
            //                           while the invalid values shift out and the "this" and
            //                           "prev" positions are filled in with good values.
            pos      = prevPos   = -1;    // Invalid value, serves as flag for initial loop iteration.
            thisChar = prevChar  = 0;
            nextPos  = startPos;
            
            
            // Loop runs once per position in the test text, until a break position
            //  is found.  In each iteration, we are testing for a possible break
            //  just preceding the character at index "pos".  The character preceding
            //  this char is at postion "prevPos"; because of combining sequences,
            //  "prevPos" can be arbitrarily far before "pos".
            for (;;) {
                // Advance to the next position to be tested.
                prevPos   = pos;
                prevChar  = thisChar;
                pos       = nextPos;
                nextPos   = moveIndex32(fText, pos, 1);
                
                // Rule LB2 - Break at end of text.
                if (pos >= fText.length()) {
                    break;
                }
                
                // Rule LB 9 - adjust for combining sequences.
                //             We do this rule out-of-order because the adjustment does
                //             not effect the way that rules LB 3 through LB 6 match,
                //             and doing it here rather than after LB 6 is substantially
                //             simpler when combining sequences do occur.
                
                
                // LB 9         Keep combining sequences together.
                //              advance over any CM class chars at "pos", 
                //              result is "nextPos" for the following loop iteration.
                thisChar  = UTF16.charAt(fText, pos);
                if (!(fSP.contains(thisChar) || fBK.contains(thisChar) || thisChar==0x0d ||
                        thisChar==0x0a || fNL.contains(thisChar) || fZW.contains(thisChar) )) {
                    for (;;) {
                        if (nextPos == fText.length()) {
                            break;   
                        }
                        int nextChar = UTF16.charAt(fText, nextPos);
                        if (!fCM.contains(nextChar)) {
                            break;
                        }
                        nextPos = moveIndex32(fText, nextPos, 1);
                    }
                }
                
                // LB 9 Treat X CM* as if it were X
                //        No explicit action required.
                
                // LB 10     Treat any remaining combining mark as AL
                if (fCM.contains(thisChar)) {
                    thisChar = 'A';   
                }

                
                // If the loop is still warming up - if we haven't shifted the initial
                //   -1 positions out of prevPos yet - loop back to advance the
                //    position in the input without any further looking for breaks.
                if (prevPos == -1) {
                    continue;
                }
                
                // LB 4  Always break after hard line breaks,
                if (fBK.contains(prevChar)) {
                    break;
                }
                
                // LB 5  Break after CR, LF, NL, but not inside CR LF
                if (fCR.contains(prevChar) && fLF.contains(thisChar)) {
                    continue;
                }
                if  (fCR.contains(prevChar) ||
                     fLF.contains(prevChar) ||
                     fNL.contains(prevChar))  {
                    break;
                }
                
                // LB 6  Don't break before hard line breaks
                if (fBK.contains(thisChar) || fCR.contains(thisChar) ||
                        fLF.contains(thisChar) || fNL.contains(thisChar) ) {
                    continue;
                }
                
                
                // LB 7  Don't break before spaces or zero-width space.
                if (fSP.contains(thisChar)) {
                    continue;
                }
                
                if (fZW.contains(thisChar)) {
                    continue;
                }
                
                // LB 8  Break after zero width space
                if (fZW.contains(prevChar)) {
                    break;
                }
                
                //  LB 9, 10  Already done, at top of loop.
                //
                
                
                // LB 11
                //    x  WJ
                //    WJ  x
                if (fWJ.contains(thisChar) || fWJ.contains(prevChar)) {
                    continue;
                }
                
                
                // LB 12
                //        GL x
                if (fGL.contains(prevChar)) {
                    continue;
                }
                
                // LB 12a
                //    [^SP BA HY] x GL
                if (!(fSP.contains(prevChar) ||
                      fBA.contains(prevChar) ||
                      fHY.contains(prevChar)     ) && fGL.contains(thisChar)) {
                    continue;
                }

                
                
                // LB 13  Don't break before closings.
                //       NU x CL, NU x CP  and NU x IS are not matched here so that they will
                //       fall into LB 17 and the more general number regular expression.
                //
                if (!fNU.contains(prevChar) && fCL.contains(thisChar) ||
                    !fNU.contains(prevChar) && fCP.contains(thisChar) ||
                                               fEX.contains(thisChar) ||
                    !fNU.contains(prevChar) && fIS.contains(thisChar) ||
                    !fNU.contains(prevChar) && fSY.contains(thisChar))    {
                    continue;
                }
                
                // LB 14  Don't break after OP SP*
                //       Scan backwards, checking for this sequence.
                //       The OP char could include combining marks, so we actually check for
                //           OP CM* SP* x
                tPos = prevPos;
                if (fSP.contains(prevChar)) {
                    while (tPos > 0 && fSP.contains(UTF16.charAt(fText, tPos))) {
                        tPos=moveIndex32(fText, tPos, -1);
                    }
                }
                while (tPos > 0 && fCM.contains(UTF16.charAt(fText, tPos))) {
                    tPos=moveIndex32(fText, tPos, -1);
                }
                if (fOP.contains(UTF16.charAt(fText, tPos))) {
                    continue;
                }
                
                // LB 15 Do not break within "[ 
                //       QU CM* SP* x OP
                if (fOP.contains(thisChar)) {
                    // Scan backwards from prevChar to see if it is preceded by QU CM* SP*
                    tPos = prevPos;
                    while (tPos > 0 && fSP.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    while (tPos > 0 && fCM.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    if (fQU.contains(UTF16.charAt(fText, tPos))) {
                        continue;
                    }
                }               
                
                // LB 16   (CL | CP) SP* x NS
                if (fNS.contains(thisChar)) {
                    tPos = prevPos;
                    while (tPos > 0 && fSP.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    while (tPos > 0 && fCM.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    if (fCL.contains(UTF16.charAt(fText, tPos)) || fCP.contains(UTF16.charAt(fText, tPos))) {
                        continue;
                    }
                }               
                
                               
                // LB 17        B2 SP* x B2
                if (fB2.contains(thisChar)) {
                    tPos = prevPos;
                    while (tPos > 0 && fSP.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    while (tPos > 0 && fCM.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    if (fB2.contains(UTF16.charAt(fText, tPos))) {
                        continue;
                    }
                }               
                
                // LB 18    break after space
                if (fSP.contains(prevChar)) {
                    break;
                }
                
                // LB 19
                //    x   QU
                //    QU  x
                if (fQU.contains(thisChar) || fQU.contains(prevChar)) {
                    continue;
                }
                
                // LB 20  Break around a CB
                if (fCB.contains(thisChar) || fCB.contains(prevChar)) {
                    break;
                }
                
                // LB 21
                if (fBA.contains(thisChar) ||
                        fHY.contains(thisChar) ||
                        fNS.contains(thisChar) ||
                        fBB.contains(prevChar) )   {
                    continue;
                }
                
                // LB 22
                if (fAL.contains(prevChar) && fIN.contains(thisChar) ||
                        fID.contains(prevChar) && fIN.contains(thisChar) ||
                        fIN.contains(prevChar) && fIN.contains(thisChar) ||
                        fNU.contains(prevChar) && fIN.contains(thisChar) )   {
                    continue;
                }
                
                
                // LB 23    ID x PO    (Note:  Leading CM behaves like ID)
                //          AL x NU
                //          NU x AL
                if (fID.contains(prevChar) && fPO.contains(thisChar) ||
                        fAL.contains(prevChar) && fNU.contains(thisChar) ||
                        fNU.contains(prevChar) && fAL.contains(thisChar) )   {
                    continue;
                }
                
                // LB 24  Do not break between prefix and letters or ideographs.
                //        PR x ID
                //        PR x AL
                //        PO x AL
                if (fPR.contains(prevChar) && fID.contains(thisChar) ||
                    fPR.contains(prevChar) && fAL.contains(thisChar) ||
                    fPO.contains(prevChar) && fAL.contains(thisChar))  {
                    continue;
                }
                
                
                // LB 25    Numbers
                matchVals = LBNumberCheck(fText, prevPos, matchVals);
                if (matchVals[0] != -1) {
                    // Matched a number.  But could have been just a single digit, which would
                    //    not represent a "no break here" between prevChar and thisChar
                    int numEndIdx = matchVals[1];  // idx of first char following num
                    if (numEndIdx > pos) {
                        // Number match includes at least the two chars being checked
                        if (numEndIdx > nextPos) {
                            // Number match includes additional chars.  Update pos and nextPos
                            //   so that next loop iteration will continue at the end of the number,
                            //   checking for breaks between last char in number & whatever follows.
                            nextPos = numEndIdx;
                            pos     = numEndIdx;
                            do {
                                pos = moveIndex32(fText, pos, -1);  
                                thisChar = UTF16.charAt(fText, pos);
                            }
                            while (fCM.contains(thisChar));
                        }
                        continue;
                    }
                }
                
                
                // LB 26  Do not break Korean Syllables
                if (fJL.contains(prevChar) && (fJL.contains(thisChar) ||
                                                fJV.contains(thisChar) ||
                                                fH2.contains(thisChar) ||
                                                fH3.contains(thisChar))) {
                                                    continue;
                                                }

                if ((fJV.contains(prevChar) || fH2.contains(prevChar))  &&
                    (fJV.contains(thisChar) || fJT.contains(thisChar))) {
                        continue;
                }

                if ((fJT.contains(prevChar) || fH3.contains(prevChar)) &&
                    fJT.contains(thisChar)) {
                        continue;
                }

                // LB 27 Treat a Korean Syllable Block the same as ID
                if ((fJL.contains(prevChar) || fJV.contains(prevChar) ||
                    fJT.contains(prevChar) || fH2.contains(prevChar) || fH3.contains(prevChar)) &&
                    fIN.contains(thisChar)) {
                        continue;
                    }
                if ((fJL.contains(prevChar) || fJV.contains(prevChar) ||
                    fJT.contains(prevChar) || fH2.contains(prevChar) || fH3.contains(prevChar)) &&
                    fPO.contains(thisChar)) {
                        continue;
                    }
                if (fPR.contains(prevChar) && (fJL.contains(thisChar) || fJV.contains(thisChar) ||
                    fJT.contains(thisChar) || fH2.contains(thisChar) || fH3.contains(thisChar))) {
                        continue;
                    }

                
                
                // LB 28 Do not break between alphabetics
                if (fAL.contains(prevChar) && fAL.contains(thisChar)) {
                    continue;
                }
                
                // LB 29  Do not break between numeric punctuation and alphabetics
                if (fIS.contains(prevChar) && fAL.contains(thisChar)) {
                    continue;
                }
                
                // LB 30    Do not break between letters, numbers, or ordinary symbols and opening or closing punctuation.
                //          (AL | NU) x OP
                //          CP x (AL | NU)
                if ((fAL.contains(prevChar) || fNU.contains(prevChar)) && fOP.contains(thisChar)) {
                    continue;
                }
                if (fCP.contains(prevChar) && (fAL.contains(thisChar) || fNU.contains(thisChar))) {
                    continue;
                }

              
                // LB 31    Break everywhere else
                break;            
            }
            
            return pos;
        }
        
        
        
        // Match the following regular expression in the input text.
        //    ((PR | PO) CM*)? ((OP | HY) CM*)? NU CM* ((NU | IS | SY) CM*) * ((CL | CP) CM*)?  (PR | PO) CM*)?
        //      0    0   1       3    3    4              7    7    7    7      9    9    9     11   11    (match states)
        //  retVals array  [0]  index of the start of the match, or -1 if no match
        //                 [1]  index of first char following the match.
        //  Can not use Java regex because need supplementary character support,
        //     and because Unicode char properties version must be the same as in
        //     the version of ICU being tested.
        private int[] LBNumberCheck(StringBuffer s, int startIdx, int[] retVals) {
            if (retVals == null) {
                retVals = new int[2];
             }
            retVals[0]     = -1;  // Indicates no match.
            int matchState = 0;
            int idx        = startIdx;
            
            matchLoop: for (idx = startIdx; idx<s.length(); idx = moveIndex32(s, idx, 1)){
                int c = UTF16.charAt(s, idx);
                int cLBType = UCharacter.getIntPropertyValue(c, UProperty.LINE_BREAK);
                switch (matchState) {
                    case 0:   
                        if (cLBType == UCharacter.LineBreak.PREFIX_NUMERIC ||
                            cLBType == UCharacter.LineBreak.POSTFIX_NUMERIC) {
                            matchState = 1;  
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.OPEN_PUNCTUATION) {
                            matchState = 4;
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.HYPHEN) {
                            matchState = 4;
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.NUMERIC) {
                            matchState = 7;
                            break;
                        }
                        break matchLoop;   /* No Match  */
                        
                    case 1:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            matchState = 1;
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.OPEN_PUNCTUATION) {
                            matchState = 4;
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.HYPHEN) {
                            matchState = 4;
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.NUMERIC) {
                            matchState = 7;
                            break;
                        }
                        break matchLoop;   /* No Match  */
                        
                        
                    case 4:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            matchState = 4;
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.NUMERIC) {
                            matchState = 7;
                            break;
                        }
                        break matchLoop;   /* No Match  */
                        //    ((PR | PO) CM*)? ((OP | HY) CM*)? NU CM* ((NU | IS | SY) CM*) * (CL CM*)?  (PR | PO) CM*)?
                        //      0    0   1       3    3    4              7    7    7    7      9   9     11   11    (match states)
                 
                    case 7:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            matchState = 7;
                            break;                           
                        }
                        if (cLBType == UCharacter.LineBreak.NUMERIC) {
                            matchState = 7;
                            break;                           
                        }
                        if (cLBType == UCharacter.LineBreak.INFIX_NUMERIC) {
                            matchState = 7;
                            break;                           
                        }
                        if (cLBType == UCharacter.LineBreak.BREAK_SYMBOLS) {
                            matchState = 7;
                            break;       
                        }
                        if (cLBType == UCharacter.LineBreak.CLOSE_PUNCTUATION) {
                            matchState = 9;
                            break;                           
                        }
                        //if (cLBType == UCharacter.LineBreak.CLOSE_PARENTHESIS) {
                        //    matchState = 9;
                        //    break;                           
                        //}
                        if (cLBType == UCharacter.LineBreak.POSTFIX_NUMERIC) {
                            matchState = 11;
                            break;                           
                        }
                        if (cLBType == UCharacter.LineBreak.PREFIX_NUMERIC) {
                            matchState = 11;
                            break;                           
                        }

                        break matchLoop;    // Match Complete.
                    case 9:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            matchState = 9;
                            break;                           
                        }
                        if (cLBType == UCharacter.LineBreak.POSTFIX_NUMERIC) {
                            matchState = 11;
                            break;                           
                        }
                        if (cLBType == UCharacter.LineBreak.PREFIX_NUMERIC) {
                            matchState = 11;
                            break;                           
                        }
                        break matchLoop;    // Match Complete.
                    case 11:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            matchState = 11;
                            break;                           
                        }
                        break matchLoop;    // Match Complete.
                }
            }
            if (matchState > 4) {
                retVals[0] = startIdx;   
                 retVals[1] = idx;   
            }
            return retVals;
        }
        
        
        List  charClasses() {
            return fSets;
        }
        
        
    
    }

     
    /**
     * 
     * Sentence Monkey Test Class
     *
     * 
     * 
     */
    static class RBBISentenceMonkey extends RBBIMonkeyKind {
        List                 fSets;
        StringBuffer         fText;

        UnicodeSet           fSepSet;
        UnicodeSet           fFormatSet;
        UnicodeSet           fSpSet;
        UnicodeSet           fLowerSet;
        UnicodeSet           fUpperSet;
        UnicodeSet           fOLetterSet;
        UnicodeSet           fNumericSet;
        UnicodeSet           fATermSet;
        UnicodeSet           fSContinueSet;
        UnicodeSet           fSTermSet;
        UnicodeSet           fCloseSet;
        UnicodeSet           fOtherSet;
        UnicodeSet           fExtendSet;

 
        
        RBBISentenceMonkey() {
            fCharProperty  = UProperty.SENTENCE_BREAK;

            fSets            = new ArrayList();

            //  Separator Set Note:  Beginning with Unicode 5.1, CR and LF were removed from the separator
            //                       set and made into character classes of their own.  For the monkey impl,
            //                       they remain in SEP, since Sep always appears with CR and LF in the rules.
            fSepSet          = new UnicodeSet("[\\p{Sentence_Break = Sep} \\u000a \\u000d]");
            fFormatSet       = new UnicodeSet("[\\p{Sentence_Break = Format}]");
            fSpSet           = new UnicodeSet("[\\p{Sentence_Break = Sp}]");
            fLowerSet        = new UnicodeSet("[\\p{Sentence_Break = Lower}]");
            fUpperSet        = new UnicodeSet("[\\p{Sentence_Break = Upper}]");
            fOLetterSet      = new UnicodeSet("[\\p{Sentence_Break = OLetter}]");
            fNumericSet      = new UnicodeSet("[\\p{Sentence_Break = Numeric}]");
            fATermSet        = new UnicodeSet("[\\p{Sentence_Break = ATerm}]");
            fSContinueSet    = new UnicodeSet("[\\p{Sentence_Break = SContinue}]");
            fSTermSet        = new UnicodeSet("[\\p{Sentence_Break = STerm}]");
            fCloseSet        = new UnicodeSet("[\\p{Sentence_Break = Close}]");
            fExtendSet       = new UnicodeSet("[\\p{Sentence_Break = Extend}]");
            fOtherSet        = new UnicodeSet();


            fOtherSet.complement();
            fOtherSet.removeAll(fSepSet);
            fOtherSet.removeAll(fFormatSet);
            fOtherSet.removeAll(fSpSet);
            fOtherSet.removeAll(fLowerSet);
            fOtherSet.removeAll(fUpperSet);
            fOtherSet.removeAll(fOLetterSet);
            fOtherSet.removeAll(fNumericSet);
            fOtherSet.removeAll(fATermSet);
            fOtherSet.removeAll(fSContinueSet);
            fOtherSet.removeAll(fSTermSet);
            fOtherSet.removeAll(fCloseSet);
            fOtherSet.removeAll(fExtendSet);

            fSets.add(fSepSet);
            fSets.add(fFormatSet);

            fSets.add(fSpSet);
            fSets.add(fLowerSet);
            fSets.add(fUpperSet);
            fSets.add(fOLetterSet);
            fSets.add(fNumericSet);
            fSets.add(fATermSet);
            fSets.add(fSContinueSet);
            fSets.add(fSTermSet);
            fSets.add(fCloseSet);
            fSets.add(fOtherSet);
            fSets.add(fExtendSet);
        }
        
        
        List  charClasses() {
            return fSets;  
        }
        
        void   setText(StringBuffer s) { 
            fText = s;        
        }   

        
        //      moveBack()   Find the "significant" code point preceding the index i.
        //      Skips over ($Extend | $Format)*
        // 
        private int moveBack(int i) {
            
            if (i <= 0) {
                return -1;
            }
            
            int      c;
            int      j = i;
            do {
                j = moveIndex32(fText, j, -1);
                c = UTF16.charAt(fText, j);
            }
            while (j>0 &&(fFormatSet.contains(c) || fExtendSet.contains(c)));
            return j;
        }
        
        
        int moveForward(int i) {
            if (i>=fText.length()) {
                return fText.length();
            }
            int   c;
            int   j = i;
            do {
                j = moveIndex32(fText, j, 1);
                c = cAt(j);
            }
            while (c>=0 && (fFormatSet.contains(c) || fExtendSet.contains(c)));
            return j;
           
        }
        
        int cAt(int pos) {
            if (pos<0 || pos>=fText.length()) {
                return -1;
            }
            return UTF16.charAt(fText, pos);
        }

        int   next(int prevPos) {  
            int    /*p0,*/ p1, p2, p3;      // Indices of the significant code points around the 
                                        //   break position being tested.  The candidate break
                                        //   location is before p2.
            int     breakPos = -1;
            
            int c0, c1, c2, c3;         // The code points at p0, p1, p2 & p3.
            int c;
            
            // Prev break at end of string.  return DONE.
            if (prevPos >= fText.length()) {
                return -1;
            }
            /*p0 =*/ p1 = p2 = p3 = prevPos;
            c3 = UTF16.charAt(fText, prevPos);
            c0 = c1 = c2 = 0;
            
            // Loop runs once per "significant" character position in the input text.
            for (;;) {
                // Move all of the positions forward in the input string.
                /*p0 = p1;*/  c0 = c1;
                p1 = p2;  c1 = c2;
                p2 = p3;  c2 = c3;
                
                // Advancd p3 by  X(Extend | Format)*   Rule 4
                p3 = moveForward(p3);
                c3 = cAt(p3);
                
                // Rule (3) CR x LF
                if (c1==0x0d && c2==0x0a && p2==(p1+1)) {
                    continue;
                }
                
                // Rule (4)    Sep  <break>
                if (fSepSet.contains(c1)) {
                    p2 = p1+1;   // Separators don't combine with Extend or Format
                    break;
                }               

                if (p2 >= fText.length()) {
                    // Reached end of string.  Always a break position.
                    break;
                }

                if (p2 == prevPos) {
                    // Still warming up the loop.  (won't work with zero length strings, but we don't care)
                    continue;
                }

                // Rule (6).   ATerm x Numeric
                if (fATermSet.contains(c1) &&  fNumericSet.contains(c2))  {
                    continue;
                }

                // Rule (7).  Upper ATerm  x  Uppper
                if (fUpperSet.contains(c0) && fATermSet.contains(c1) && fUpperSet.contains(c2)) {
                    continue;
                }

                // Rule (8)  ATerm Close* Sp*  x  (not (OLettter | Upper | Lower | Sep))* Lower
                //           Note:  Sterm | ATerm are added to the negated part of the expression by a 
                //                  note to the Unicode 5.0 documents.
                int p8 = p1;
                while (p8>0 && fSpSet.contains(cAt(p8))) {
                    p8 = moveBack(p8);
                }
                while (p8>0 && fCloseSet.contains(cAt(p8))) {
                    p8 = moveBack(p8);
                }
                if (fATermSet.contains(cAt(p8))) {
                    p8=p2;
                    for (;;) {
                        c = cAt(p8);
                        if (c==-1 || fOLetterSet.contains(c) || fUpperSet.contains(c) ||
                            fLowerSet.contains(c) || fSepSet.contains(c) ||
                            fATermSet.contains(c) || fSTermSet.contains(c))  
                         {
                            break;
                        }
                        p8 = moveForward(p8);
                    }
                    if (p8<fText.length() && fLowerSet.contains(cAt(p8))) {
                        continue;
                    }
                }
                
                // Rule 8a  (STerm | ATerm) Close* Sp* x (SContinue | Sterm | ATerm)
                if (fSContinueSet.contains(c2) || fSTermSet.contains(c2) || fATermSet.contains(c2)) {
                    p8 = p1;
                    while (setContains(fSpSet, cAt(p8))) {
                        p8 = moveBack(p8);
                    }
                    while (setContains(fCloseSet, cAt(p8))) {
                        p8 = moveBack(p8);
                    }
                    c = cAt(p8);
                    if (setContains(fSTermSet, c) || setContains(fATermSet, c)) {
                        continue;
                    }
                }


                // Rule (9)  (STerm | ATerm) Close*  x  (Close | Sp | Sep | CR | LF)
                int p9 = p1;
                while (p9>0 && fCloseSet.contains(cAt(p9))) {
                    p9 = moveBack(p9);
                }
                c = cAt(p9);
                if ((fSTermSet.contains(c) || fATermSet.contains(c))) {
                    if (fCloseSet.contains(c2) || fSpSet.contains(c2) || fSepSet.contains(c2)) {
                        continue;
                    }
                }

                // Rule (10)  (Sterm | ATerm) Close* Sp*  x  (Sp | Sep | CR | LF)
                int p10 = p1;
                while (p10>0 && fSpSet.contains(cAt(p10))) {
                    p10 = moveBack(p10);
                }
                while (p10>0 && fCloseSet.contains(cAt(p10))) {
                    p10 = moveBack(p10);
                }
                if (fSTermSet.contains(cAt(p10)) || fATermSet.contains(cAt(p10))) {
                    if (fSpSet.contains(c2) || fSepSet.contains(c2)) {
                        continue;
                    }
                }

                // Rule (11)  (STerm | ATerm) Close* Sp*   <break>
                int p11 = p1;
                if (p11>0 && fSepSet.contains(cAt(p11))) {
                    p11 = moveBack(p11);
                }
                while (p11>0 && fSpSet.contains(cAt(p11))) {
                    p11 = moveBack(p11);
                }
                while (p11>0 && fCloseSet.contains(cAt(p11))) {
                    p11 = moveBack(p11);
                }
                if (fSTermSet.contains(cAt(p11)) || fATermSet.contains(cAt(p11))) {
                    break;
                }

                //  Rule (12)  Any x Any
                continue;
            }
            breakPos = p2;
            return breakPos;
        }
           

        
    }

 
    /**
     * Move an index into a string by n code points.
     *   Similar to UTF16.moveCodePointOffset, but without the exceptions, which were
     *   complicating usage.
     * @param s   a Text string
     * @param pos The starting code unit index into the text string
     * @param amt The amount to adjust the string by.
     * @return    The adjusted code unit index, pinned to the string's length, or
     *            unchanged if input index was outside of the string.
     */
    static int moveIndex32(StringBuffer s, int pos, int amt) {
        int i;
        char  c;
        if (amt>0) {
            for (i=0; i<amt; i++) {
                if (pos >= s.length()) {
                    return s.length();                   
                }
                c = s.charAt(pos);
                pos++;
                if (UTF16.isLeadSurrogate(c) && pos < s.length()) {
                    c = s.charAt(pos);
                    if (UTF16.isTrailSurrogate(c)) {
                        pos++;   
                    }
                }
            }
        } else {
            for (i=0; i>amt; i--) {
                if (pos <= 0) {
                    return 0;   
                }
                pos--;
                c = s.charAt(pos);
                if (UTF16.isTrailSurrogate(c) && pos >= 0) {
                    c = s.charAt(pos);
                    if (UTF16.isLeadSurrogate(c)) {
                        pos--;   
                    }
                }
            }
        }
        return pos;
    }
    
    /**
     * No-exceptions form of UnicodeSet.contains(c).
     *    Simplifies loops that terminate with an end-of-input character value.
     * @param s  A unicode set
     * @param c  A code point value
     * @return   true if the set contains c.
     */
    static boolean setContains(UnicodeSet s, int c) {
        if (c<0 || c>UTF16.CODEPOINT_MAX_VALUE ) {
            return false;
        }
        return s.contains(c);
    }
    
    
    /**
     * return the index of the next code point in the input text.
     * @param i the preceding index
     * @return
     * @internal
     */
    static int  nextCP(StringBuffer s, int i) {
        if (i == -1) {
            // End of Input indication.  Continue to return end value.
            return -1;
        }
        int  retVal = i + 1;
        if (retVal > s.length()) {
            return -1;
        }
        int  c = UTF16.charAt(s, i);
        if (c >= UTF16.SUPPLEMENTARY_MIN_VALUE && UTF16.isLeadSurrogate(s.charAt(i))) {
            retVal++;
        }
        return retVal;
    }
    
    
    /**
     * random number generator.  Not using Java's built-in Randoms for two reasons:
     *    1.  Using this code allows obtaining the same sequences as those from the ICU4C monkey test.
     *    2.  We need to get and restore the seed from values occurring in the middle
     *        of a long sequence, to more easily reproduce failing cases.
     */
    private static int m_seed = 1;
    private static int  m_rand()
    {
        m_seed = m_seed * 1103515245 + 12345;
        return (int)(m_seed >>> 16) % 32768;
    }

    // Helper function for formatting error output.
    //   Append a string into a fixed-size field in a StringBuffer.
    //   Blank-pad the string if it is shorter than the field.
    //   Truncate the source string if it is too long.
    //
    private static void appendToBuf(StringBuffer dest, String src, int fieldLen) {
        int appendLen = src.length();
        if (appendLen >= fieldLen) {
            dest.append(src.substring(0, fieldLen));
        } else {
            dest.append(src);
            while (appendLen < fieldLen) {
                dest.append(' ');
                appendLen++;
            }
        }
    }

    // Helper function for formatting error output.
    // Display a code point in "\\uxxxx" or "\Uxxxxxxxx" format
    private static void appendCharToBuf(StringBuffer dest, int c, int fieldLen) {
           String hexChars = "0123456789abcdef";
           if (c < 0x10000) {
                dest.append("\\u");
                for (int bn=12; bn>=0; bn-=4) {
                    dest.append(hexChars.charAt((((int)c)>>bn)&0xf));
                }
                appendToBuf(dest, " ", fieldLen-6);
            } else {
                dest.append("\\U");
                for (int bn=28; bn>=0; bn-=4) {
                    dest.append(hexChars.charAt((((int)c)>>bn)&0xf));
                }
                appendToBuf(dest, " ", fieldLen-10);

            }
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
    boolean[]        followingBreaks  = new boolean[TESTSTRINGLEN*2 + 1];
    boolean[]        precedingBreaks  = new boolean[TESTSTRINGLEN*2 + 1];
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
    // RuleBasedBreakIterator_New.fTrace = true;
    // m_seed = 859056465;
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
        Arrays.fill(followingBreaks, false);
        Arrays.fill(precedingBreaks, false);
 
        // Calculate the expected results for this test string.
        mk.setText(testText);
        expectedCount = 0;
        expectedBreaks[0] = true;
        expected[expectedCount ++] = 0;
        int breakPos = 0;
        int lastBreakPos = -1;
        for (;;) {
            lastBreakPos = breakPos;
            breakPos = mk.next(breakPos);
            if (breakPos == -1) {
                break;
            }
            if (breakPos > testText.length()) {
                errln("breakPos > testText.length()");
            }
            if (lastBreakPos >= breakPos) {
                errln("Next() not increasing.");
                // break;
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

        // Find the break positions using the following() function.
        lastBreakPos = 0;
        followingBreaks[0] = true;
        for (i=0; i<testText.length(); i++) {
            breakPos = bi.following(i);
            if (breakPos <= i ||
                breakPos < lastBreakPos ||
                breakPos > testText.length() ||
                breakPos > lastBreakPos && lastBreakPos > i ) {
                errln(name + " break monkey test: " +
                    "Out of range value returned by BreakIterator::following().\n" +
                    "index=" + i + "following returned=" + breakPos +
                    "lastBreak=" + lastBreakPos);
                precedingBreaks[i] = !expectedBreaks[i];   // Forces an error.
            } else {
                followingBreaks[breakPos] = true;
                lastBreakPos = breakPos;
            }
        }
        
        // Find the break positions using the preceding() function.
        lastBreakPos = testText.length();
        precedingBreaks[testText.length()] = true;
        for (i=testText.length(); i>0; i--) {
            breakPos = bi.preceding(i);
            if (breakPos >= i ||
                breakPos > lastBreakPos ||
                breakPos < 0 ||
                breakPos < lastBreakPos && lastBreakPos < i ) {
                errln(name + " break monkey test: " +
                        "Out of range value returned by BreakIterator::preceding().\n" +
                        "index=" + i + "preceding returned=" + breakPos +
                        "lastBreak=" + lastBreakPos);
                precedingBreaks[i] = !expectedBreaks[i];   // Forces an error.
            } else {
                precedingBreaks[breakPos] = true;
                lastBreakPos = breakPos;
            }
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
            } else if (followingBreaks[i] != expectedBreaks[i]) {
                errorType = "following()";
            } else if (precedingBreaks[i] != expectedBreaks[i]) {
                errorType = "preceding()";
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

                int      c;    // Char from test data
                for (ci = startContext;  ci <= endContext && ci != -1;  ci = nextCP(testText, ci)) {
                    if (ci == i) {
                        // This is the location of the error.
                        errorText.append("<?>---------------------------------\n");
                    } else if (expectedBreaks[ci]) {
                        // This a non-error expected break position.
                        errorText.append("------------------------------------\n");
                    }
                    if (ci < testText.length()) {
                        c = UTF16.charAt(testText, ci);
                        appendCharToBuf(errorText, c, 11);
                        String gc = UCharacter.getPropertyValueName(UProperty.GENERAL_CATEGORY, UCharacter.getType(c), UProperty.NameChoice.SHORT);
                        appendToBuf(errorText, gc, 8);
                        int extraProp = UCharacter.getIntPropertyValue(c, mk.fCharProperty);
                        String extraPropValue = 
                            UCharacter.getPropertyValueName(mk.fCharProperty, extraProp, UProperty.NameChoice.LONG);
                        appendToBuf(errorText, extraPropValue, 20);

                        String charName = UCharacter.getExtendedName(c);
                        appendToBuf(errorText, charName, 40);
                        errorText.append('\n');
                    }
                }
                if (ci == testText.length() && ci != -1) {
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
    
    if (params.inclusion >= 9) {
        loopCount = 10000;
    }
    
    logln("Word Break Monkey Test");
    RBBIWordMonkey  m = new RBBIWordMonkey();
    BreakIterator   bi = BreakIterator.getWordInstance(Locale.US);
    RunMonkey(bi, m, "word", seed, loopCount);
}

public void TestLineMonkey() {
    if(skipIfBeforeICU(4, 3, 4)) {
        return;  // TODO: Fix me!
    }
    int        loopCount = 500;
    int        seed      = 1;
    
    if (params.inclusion >= 9) {
        loopCount = 10000;
    }
    
    logln("Line Break Monkey Test");
    RBBILineMonkey  m = new RBBILineMonkey();
    BreakIterator   bi = BreakIterator.getLineInstance(Locale.US);
    if (params == null) {
        loopCount = 50;
    }
    RunMonkey(bi, m, "line", seed, loopCount);
}

public void TestSentMonkey() {
    
    int        loopCount = 500;
    int        seed      = 1;
    
    if (params.inclusion >= 9) {
        loopCount = 3000;
    }
    
    logln("Sentence Break Monkey Test");
    RBBISentenceMonkey  m = new RBBISentenceMonkey();
    BreakIterator   bi = BreakIterator.getSentenceInstance(Locale.US);
    if (params == null) {
        loopCount = 30;
    }
    RunMonkey(bi, m, "sent", seed, loopCount);
}
//
//  Round-trip monkey tests.
//  Verify that break iterators created from the rule source from the default
//    break iterators still pass the monkey test for the iterator type.
//
//  This is a major test for the Rule Compiler.  The default break iterators are built
//  from pre-compiled binary rule data that was created using ICU4C; these
//  round-trip rule recompile tests verify that the Java rule compiler can
//  rebuild break iterators from the original source rules.
//
public void TestRTCharMonkey() {
    
    int        loopCount = 200;
    int        seed      = 1;
    
    if (params.inclusion >= 9) {
        loopCount = 2000;
    }
    
    RBBICharMonkey  m = new RBBICharMonkey();
    BreakIterator   bi = BreakIterator.getCharacterInstance(Locale.US);
    String rules = bi.toString();
    BreakIterator rtbi = new RuleBasedBreakIterator(rules);
    RunMonkey(rtbi, m, "char", seed, loopCount);
}

public void TestRTWordMonkey() {
    
    int        loopCount = 200;
    int        seed      = 1;
    
    if (params.inclusion >= 9) {
        loopCount = 2000;
    }
    
    logln("Word Break Monkey Test");
    RBBIWordMonkey  m = new RBBIWordMonkey();
    BreakIterator   bi = BreakIterator.getWordInstance(Locale.US);
    String rules = bi.toString();
    BreakIterator rtbi = new RuleBasedBreakIterator(rules);
    RunMonkey(rtbi, m, "word", seed, loopCount);
}

public void TestRTLineMonkey() {
    if(skipIfBeforeICU(4, 3, 4)) {
        return;  // TODO: Fix me!
    }
    int        loopCount = 200;
    int        seed      = 1;
    
    if (params.inclusion >= 9) {
        loopCount = 2000;
    }
    
    logln("Line Break Monkey Test");
    RBBILineMonkey  m = new RBBILineMonkey();
    BreakIterator   bi = BreakIterator.getLineInstance(Locale.US);
    String rules = bi.toString();
    BreakIterator rtbi = new RuleBasedBreakIterator(rules);
    if (params == null) {
        loopCount = 50;
    }
    RunMonkey(rtbi, m, "line", seed, loopCount);
}

public void TestRTSentMonkey() {
    
    int        loopCount = 200;
    int        seed      = 1;
    
    if (params.inclusion >= 9) {
        loopCount = 1000;
    }
    
    logln("Sentence Break Monkey Test");
    RBBISentenceMonkey  m = new RBBISentenceMonkey();
    BreakIterator   bi = BreakIterator.getSentenceInstance(Locale.US);
    String rules = bi.toString();
    BreakIterator rtbi = new RuleBasedBreakIterator(rules);
    if (params == null) {
        loopCount = 30;
    }
    RunMonkey(rtbi, m, "sent", seed, loopCount);
}



}

