/*
 *******************************************************************************
 * Copyright (C) 2003-2004 International Business Machines Corporation and     *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
 package com.ibm.icu.dev.test.rbbi;


// Monkey testing of RuleBasedBreakIterator
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Locale;


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

        StringBuffer              fText;


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


    void setText(StringBuffer s) {
        fText = s;        
    }
    
    List charClasses() {
        return fSets;
    }
    
    int next(int i) {
        return nextGC(fText, i);
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

        UnicodeSet                fKatakanaSet;
        UnicodeSet                fALetterSet;
        UnicodeSet                fMidLetterSet;
        UnicodeSet                fMidNumLetSet;
        UnicodeSet                fMidNumSet;
        UnicodeSet                fNumericSet;
        UnicodeSet                fFormatSet;
        UnicodeSet                fExtendSet;
        UnicodeSet                fOtherSet;

        
        RBBIWordMonkey() {
            fSets          = new ArrayList();

            fKatakanaSet   = new UnicodeSet("[\\p{script=KATAKANA}\\u30fc\\uff70\\uff9e\\uff9f]");

            String ALetterStr = "[[\\p{Alphabetic}\\u05f3]-[\\p{Ideographic}]-[\\p{Script=Thai}]" +
                                            "-[\\p{Script=Lao}]-[\\p{Script=Hiragana}]-" +
                                            "[\\p{script=KATAKANA}\\u30fc\\uff70\\uff9e\\uff9f]]";

            fALetterSet    = new UnicodeSet(ALetterStr);
            fMidLetterSet  = new UnicodeSet("[\\u0027\\u00b7\\u05f4\\u2019\\u2027]");
            fMidNumLetSet  = new UnicodeSet("[\\u002e\\u003a]");
            fMidNumSet     = new UnicodeSet("[\\p{Line_Break=Infix_Numeric}]");
            fNumericSet    = new UnicodeSet("[\\p{Line_Break=Numeric}]");
            fFormatSet     = new UnicodeSet("[\\p{Format}-\\p{Grapheme_Extend}]");
            fExtendSet     = new UnicodeSet("[\\p{Grapheme_Extend}]");
            fOtherSet      = new UnicodeSet();

            fOtherSet.complement();
            fOtherSet.removeAll(fKatakanaSet);
            fOtherSet.removeAll(fALetterSet);
            fOtherSet.removeAll(fMidLetterSet);
            fOtherSet.removeAll(fMidNumLetSet);
            fOtherSet.removeAll(fMidNumSet);
            fOtherSet.removeAll(fNumericSet);

            fSets.add(fALetterSet);
            fSets.add(fMidLetterSet);
            fSets.add(fMidNumLetSet);
            fSets.add(fMidNumSet);
            fSets.add(fNumericSet);
            fSets.add(fFormatSet);
            fSets.add(fOtherSet);
        }
        
        
        List  charClasses() {
         return fSets;  
        }
        
        void   setText(StringBuffer s) { 
            fText = s;        
        }   

        int   next(int prevPos) {  
            int    p0, p1, p2, p3;        // Indices of the significant code points around the 
                                        //   break position being tested.  The candidate break
                                        //   location is before p2.
            int     breakPos = -1;
            
            int c0, c1, c2, c3;   // The code points at p0, p1, p2 & p3.
            
            // Prev break at end of string.  return DONE.
            if (prevPos >= fText.length()) {
                return -1;
            }
            p0 = p1 = p2 = p3 = prevPos;
            c3 = UTF16.charAt(fText, prevPos);
            c0 = c1 = c2 = 0;
            
            
            // Format char after prev break?  Special case, see last Note for Word Boundaries TR.
            // break immdiately after the format char.
            if (breakPos >= 0 && fFormatSet.contains(c3) && breakPos < (fText.length() -1)) {
                breakPos = UTF16.moveCodePointOffset(fText, breakPos, 1);
                return breakPos;
}


            // Loop runs once per "significant" character position in the input text.
            for (;;) {
                // Move all of the positions forward in the input string.
                p0 = p1;  c0 = c1;
                p1 = p2;  c1 = c2;
                p2 = p3;  c2 = c3;
                
                // Advancd p3 by    (GC Format*)   Rules 3, 4
                p3 = nextGC(fText, p3);
                if (p3 == -1 || p3 >= fText.length()) {
                    p3 = fText.length();
                    c3 = 0;
                } else {
                    c3 = UTF16.charAt(fText, p3);
                    while (fFormatSet.contains(c3)) {
                        p3 = moveIndex32(fText, p3, 1);
                        c3 = 0;
                        if (p3 < fText.length()) {
                            c3 = UTF16.charAt(fText, p3);   
                        }
                    }
                }

                if (p1 == p2) {
                    // Still warming up the loop.  (won't work with zero length strings, but we don't care)
                    continue;
                }
                if (p2 == fText.length()) {
                    // Reached end of string.  Always a break position.
                    break;
                }

                // Rule (5).   ALetter x ALetter
                if (fALetterSet.contains(c1) &&
                        fALetterSet.contains(c2))  {
                    continue;
                }
                
                // Rule (6)  ALetter  x  (MidLetter | MidNumLet) ALetter
                //
                //    Also incorporates rule 7 by skipping pos ahead to position of the
                //    terminating ALetter.
                if ( fALetterSet.contains(c1) &&
                        (fMidLetterSet.contains(c2) || fMidNumLetSet.contains(c2)) &&
                        fALetterSet.contains(c3)) {
                    continue;
                }
                
                
                // Rule (7)  ALetter (MidLetter | MidNumLet)  x  ALetter
                if (fALetterSet.contains(c0) &&
                        (fMidLetterSet.contains(c1) || fMidNumLetSet.contains(c1) ) &&
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
                        (fMidNumSet.contains(c1) || fMidNumLetSet.contains(c1)) && 
                        fNumericSet.contains(c2)) {
                    continue;
                }
                
                // Rule (12)  Numeric x (MidNum | MidNumLet) Numeric
                if (fNumericSet.contains(c1) &&
                        (fMidNumSet.contains(c2) || fMidNumLetSet.contains(c2)) &&
                        fNumericSet.contains(c3)) {
                    continue;
                }
                
                // Rule (13)  Katakana x Katakana
                if (fKatakanaSet.contains(c1) &&
                        fKatakanaSet.contains(c2))  {
                    continue;
                }
                
                // Rule 14.  Break found here.
                break;
            }
            
            
            //  Rule 4 fixup,  back up before any trailing
            //         format characters at the end of the word.
            breakPos = p2;
            int  t = nextGC(fText, p1);
            if (t > p1) {
                breakPos = t;
            }
            return breakPos;
        }
        
    }

 
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
        UnicodeSet  fXX;
        
        BreakIterator  fCharBI;
        
        StringBuffer  fText;
        int           fOrigPositions;
        
        
        
        RBBILineMonkey()
        {
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
            fCL    = new UnicodeSet("[\\p{Line_break=CL}]");
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
            fXX    = new UnicodeSet("[\\p{Line_break=XX}]");
            
            fAL.addAll(fXX);     // Default behavior for XX is identical to AL
            fAL.addAll(fAI);     // Default behavior for AI is identical to AL
            fAL.addAll(fSA);     // Default behavior for SA is XX, which defaults to AL
            
            
            
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
            fSets.add(fCL);
            fSets.add(fEX);
            fSets.add(fIN);
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
            
            fCharBI = BreakIterator.getCharacterInstance(Locale.ENGLISH);
        }
        
        void setText(StringBuffer s) {
            fText       = s;
            fCharBI.setText(s.toString());
        }
        
        //
//      rule67Adjust
//      Line Break TR rules 6 and 7 implementation.
//      This deals with combining marks, Hangul Syllables, and other sequences that
//      that must be treated as if they were something other than what they actually are.
        //
//      This is factored out into a separate function because it must be applied twice for
//      each potential break, once to the chars before the position being checked, then
//      again to the text following the possible break.
        //
        int[] rule67Adjust(int pos, int posChar, int nextPos, int nextChar, int[] retVals) {
            if (retVals == null) {
                retVals = new int[3];   
            }
            retVals[0] = posChar;
            retVals[1] = nextPos;
            retVals[2] = nextChar;
            if (pos == -1) {
                // Invalid initial position.  Happens during the warmup iteration of the
                //   main loop in next().
                return retVals;
            }
            
            int  nPos = nextPos;
            
            // LB 6  Treat Korean Syllables as a single unit
            int  hangultype = UCharacter.getIntPropertyValue(posChar, UProperty.HANGUL_SYLLABLE_TYPE);
            if (hangultype != UCharacter.HangulSyllableType.NOT_APPLICABLE) {
                nPos = fCharBI.following(pos);   // Advance by grapheme cluster, which
                // contains the logic to locate Hangul syllables.
                // Grapheme Cluster Ugliness: some Grapheme_Extend chars, which are absorbed
                //   into a grapheme cluster, are NOT Line Break CM. (Some are GL, for example.)
                //   We don't want consume any of these.  The Approach is
                //      1.  Back nPos up, undoing the consumption of any
                //          Grapheme_Extend chars by the char break iterator.
                //      2.  Let the LB 7b logic below reconsume any Line Break CM chars.
                for (;;) {
                    nPos = moveIndex32(fText, nPos, -1);
                    int possiblyExtendChar = UTF16.charAt(fText, nPos);
                    if (fID.contains(possiblyExtendChar)) {
                        // We hit into the Hangul Syllable itself, class is ID.
                        nPos = moveIndex32(fText, nPos, +1);
                        break;
                    }
                    if (nPos == 0) {
                        break;   
                    }
                }
            }
            
            // LB 7b  Keep combining sequences together.
            //  advance over any CM class chars.  (Line Break CM class is different from
            //    grapheme cluster CM, so we need to do this even for HangulSyllables.
            //    Line Break may eat additional stuff as combining, beyond what graphem cluster did.
            if (!(fBK.contains(posChar) || fZW.contains(posChar) || posChar==0x0a
                    || posChar==0x0d || posChar==0x85)) {
                for (;;) {
                    if (nPos == fText.length()) {
                        break;   
                    }
                    nextChar = UTF16.charAt(fText, nPos);
                    if (!fCM.contains(nextChar)) {
                        break;
                    }
                    nPos = moveIndex32(fText, nPos, 1);
                }
            }
            
            
            // LB 7a In a SP CM* sequence, treat the SP as an ID
            if (nPos != nextPos && fSP.contains(posChar)) {
                posChar = 0x4e00;   // 0x4e00 is a CJK Ideograph, linebreak type is ID.
            }
            
            // LB 7b Treat X CM* as if it were x.
            //       No explicit action required.
            
            // LB 7c  Treat any remaining combining mark as AL
            if (fCM.contains(posChar)) {
                posChar = 'A';   
            }
            
            // Push the updated nextPos and nextChar back to our caller.
            // This only makes a difference if posChar got bigger, by slurping up a
            // combining sequence or Hangul syllable.
            nextPos  = nPos;
            nextChar = 0;
            if (nPos < fText.length()) {
                nextChar = UTF16.charAt(fText, nPos);
            }
            retVals[0] = posChar;
            retVals[1] = nextPos;
            retVals[2] = nextChar;
            return retVals;
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
            int    nextCPPos; //  Index of the code point following "pos."
            //     May point to a combining mark.
            int    tPos;      //  temp value.
            int    c;
            int     LB10match[] =   null;     // Regular expr match results for LB10.
            int    matchVals[]  = null;             // Regular Expression Match Results
            int    rule67vals[] = null;       //  Return values from Rule 6 & 7 adjust function.

            
            if (startPos >= fText.length()) {
                return -1;
            }
            
            
            // Initial values for loop.  Loop will run the first time without finding breaks,
            //                           while the invalid values shift out and the "this" and
            //                           "prev" positions are filled in with good values.
            pos      = prevPos   = -1;    // Invalid value, serves as flag for initial loop iteration.
            thisChar = prevChar  = 0;
            nextPos  = nextCPPos = startPos;
            
            
            // Loop runs once per position in the test text, until a break position
            //  is found.
            for (;;) {
                prevPos   = pos;
                prevChar  = thisChar;
                
                pos       = nextPos;
                // Break at end of text.
                if (pos >= fText.length()) {
                    break;
                }
                
                thisChar  = UTF16.charAt(fText, pos);
                
                nextCPPos = moveIndex32(fText, pos, 1);
                nextPos   = nextCPPos;
                
                // LB 3a  Always break after hard line breaks,
                if (fBK.contains(prevChar)) {
                    break;
                }
                
                // LB 3b  Break after CR, LF, NL, but not inside CR LF
                if (prevChar == 0x0d && thisChar == 0x0a) {
                    continue;
                }
                if (prevChar == 0x0d ||
                        prevChar == 0x0a ||
                        prevChar == 0x85)  {
                    break;
                }
                
                // LB 3c  Don't break before hard line breaks
                if (thisChar == 0x0d || thisChar == 0x0a || thisChar == 0x85 ||
                        fBK.contains(thisChar)) {
                    continue;
                }
                
                // LB 10    QU SP* x OP
                if (prevPos >= 0) {
                    matchVals = LB10Check(fText, prevPos, LB10match);   //   Test for match of  
                    if (matchVals[0] != -1) {                              //     /QU CM* SP* (OP) CM*/
                        pos      = matchVals[0];
                        nextPos  = matchVals[1];
                        thisChar = UTF16.charAt(fText, pos);
                        continue;
                    }
                }
                
                // LB 11   CL SP* x NS
                if (prevPos >= 0) {
                    matchVals = LB11Check(fText, prevPos, matchVals);
                    if (matchVals[0] != -1) {  //   /QU CM* SP* (OP) CM*/;
                        pos      = matchVals[0];
                        nextPos  = matchVals[1];
                        thisChar = UTF16.charAt(fText, pos);
                        continue;
                    }
                }
                
                // LB 4  Don't break before spaces or zero-width space.
                if (fSP.contains(thisChar)) {
                    continue;
                }
                
                if (fZW.contains(thisChar)) {
                    continue;
                }
                
                // LB 5  Break after zero width space
                if (fZW.contains(prevChar)) {
                    break;
                }
                
                // LB 6, LB 7
                /*int oldpos = pos;*/
                int  retVals[] = null;
                retVals = rule67Adjust(prevPos, prevChar, pos, thisChar, retVals);
                prevChar = retVals[0];
                pos      = retVals[1];
                thisChar = retVals[2];
                
                nextCPPos = moveIndex32(fText, pos, 1);
                nextPos   = nextCPPos;
                c = 0;
                if (nextPos < fText.length()) {
                    c = UTF16.charAt(fText, nextPos);
                }
                // another peculiarity of LB 4 - Dont break before space
                if (fSP.contains(thisChar)) {
                    continue;
                }
                rule67vals = rule67Adjust(pos,  thisChar, nextPos, c, rule67vals);
                thisChar = rule67vals[0];
                nextPos  = rule67vals[1];
                c        = rule67vals[2];
                
                // If the loop is still warming up - if we haven't shifted the initial
                //   -1 positions out of prevPos yet - loop back to advance the
                //    position in the input without any further looking for breaks.
                if (prevPos == -1) {
                    continue;
                }
                
                // Re-apply rules 3c, 4 because these could be affected by having
                //                      a new thisChar from doing rule 6 or 7.
                if (thisChar == 0x0d || thisChar == 0x0a || thisChar == 0x85 ||   // 3c
                        fBK.contains(thisChar)) {
                    continue;
                }
                if (fSP.contains(thisChar)) {    // LB 4
                    continue;
                }
                if (fZW.contains(thisChar)) {    // LB 4
                    continue;
                }
                
                
                // LB 8  Don't break before closings.
                //       NU x CL  and NU x IS are not matched here so that they will
                //       fall into LB 17 and the more general number regular expression.
                //
                if (!fNU.contains(prevChar) && fCL.contains(thisChar) ||
                        fEX.contains(thisChar) ||
                        !fNU.contains(prevChar) && fIS.contains(thisChar) ||
                        !fNU.contains(prevChar) && fSY.contains(thisChar))    {
                    continue;
                }
                
                // LB 9  Don't break after OP SP*
                //       Scan backwards, checking for this sequence.
                //       The OP char could include combining marks, so we acually check for
                //           OP CM* SP*
                //       Another Twist: The Rule 67 fixes may have changed a CP CM
                //       sequence into a ID char, so before scanning back through spaces,
                //       verify that prevChar is indeed a space.  The prevChar variable
                //       may differ from fText[prevPos]
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
                
                
                // LB 11a        B2 x B2
                if (fB2.contains(thisChar) && fB2.contains(prevChar)) {
                    continue;
                }
                
                // LB 11b
                //    x  GL
                //    GL  x
                if (fGL.contains(thisChar) || fGL.contains(prevChar)) {
                    continue;
                }
                if (fWJ.contains(thisChar) || fWJ.contains(prevChar)) {
                    continue;
                }
                
                // LB 12    break after space
                if (fSP.contains(prevChar)) {
                    break;
                }
                
                // LB 14
                //    x   QU
                //    QU  x
                if (fQU.contains(thisChar) || fQU.contains(prevChar)) {
                    continue;
                }
                
                // LB 14a  Break around a CB
                if (fCB.contains(thisChar) || fCB.contains(prevChar)) {
                    break;
                }
                
                // LB 15
                if (fBA.contains(thisChar) ||
                        fHY.contains(thisChar) ||
                        fNS.contains(thisChar) ||
                        fBB.contains(prevChar) )   {
                    continue;
                }
                
                // LB 16
                if (fAL.contains(prevChar) && fIN.contains(thisChar) ||
                        fID.contains(prevChar) && fIN.contains(thisChar) ||
                        fIN.contains(prevChar) && fIN.contains(thisChar) ||
                        fNU.contains(prevChar) && fIN.contains(thisChar) )   {
                    continue;
                }
                
                
                // LB 17    ID x PO    (Note:  Leading CM behaves like ID)
                //          AL x NU
                //          NU x AL
                if (fID.contains(prevChar) && fPO.contains(thisChar) ||
                        fCM.contains(prevChar) && fPO.contains(thisChar) ||
                        fAL.contains(prevChar) && fNU.contains(thisChar) ||
                        fNU.contains(prevChar) && fAL.contains(thisChar) )   {
                    continue;
                }
                
                // LB 18    Numbers
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
                            pos = fCharBI.preceding(numEndIdx);
                            thisChar = UTF16.charAt(fText, pos);
                            while (fCM.contains(thisChar)) {
                                pos = fCharBI.preceding(pos);
                                thisChar = UTF16.charAt(fText, pos);
                            }
                        }
                        continue;
                    }
                }
                if (fPR.contains(prevChar) && fAL.contains(thisChar)) {
                    continue;   
                }
                if (fPR.contains(prevChar) && fID.contains(thisChar)) {
                    continue;
                }
                // LB 18b
                if (fHY.contains(prevChar) || fBB.contains(thisChar)) {
                    break;
                }
                
                // LB 19
                if (fAL.contains(prevChar) && fAL.contains(thisChar)) {
                    continue;
                }
                
                // LB 19b
                if (fIS.contains(prevChar) && fAL.contains(thisChar)) {
                    continue;
                }
                
                // LB 20    Break everywhere else
                break;
                
            }
            
            return pos;
        }
        
        // Match the following regular expression in the input text.
        //     QU CM* SP* (OP) CM*
        //      0  1   2   3    4   (match states)
        //  Can't use Java regexp because supplementary chars must be handled,
        //                        because line break properties are needed, and
        //                        because Unicode Version must match ICU.
        //  retVals array  [0]  index of the OP in the match, or -1 if no match
        //                 [1]  index of first char following the match.
        private int[] LB10Check(StringBuffer s, int startIdx, int[] retVals) {
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
                        if (cLBType == UCharacter.LineBreak.QUOTATION) {
                            matchState = 1;  
                            break;
                        }
                        break matchLoop;   /* No Match  */
                    case 1:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            break;
                        }
                    case 2:
                        if (cLBType == UCharacter.LineBreak.SPACE) {
                            matchState = 2;
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.OPEN_PUNCTUATION) {
                            matchState = 4;
                            retVals[0] = idx;
                            retVals[1] = idx;
                            break;
                        }
                        break matchLoop;   /*  No Match */
                    case 4:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            retVals[1] = idx;
                            break;                           
                        }
                        break matchLoop;   //  Successful match.
                }
            }
            if (retVals[0] >= 0) {
                retVals[1] = moveIndex32(fText, retVals[1], 1);
            }
            return retVals;
        }
        
        
        // Match the following regular expression in the input text.
        //     CL CM* SP* (NS) CM*
        //      0  1   2        4   (match states)
        //  Can't use Java regexp because supplementary chars must be handled,
        //                        because line break properties are needed, and
        //                        because Unicode Version must match ICU.
        //  retVals array  [0]  index of the OP in the match, or -1 if no match
        //                 [1]  index of first char following the match.
        private int[] LB11Check(StringBuffer s, int startIdx, int[] retVals) {
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
                        if (cLBType == UCharacter.LineBreak.CLOSE_PUNCTUATION) {
                            matchState = 1;  
                            break;
                        }
                        break matchLoop;   /* No Match  */
                    case 1:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            break;
                        }
                    case 2:
                        if (cLBType == UCharacter.LineBreak.SPACE) {
                            matchState = 2;
                            break;
                        }
                        if (cLBType == UCharacter.LineBreak.NONSTARTER) {
                            matchState = 4;
                            retVals[0] = idx;
                            retVals[1] = idx;
                            break;
                        }
                        break matchLoop;   /*  No Match */
                    case 4:
                        if (cLBType == UCharacter.LineBreak.COMBINING_MARK) {
                            retVals[1] = idx;
                            break;                           
                        }
                        break matchLoop;   //  Successful match.
                }
                if (retVals[0] >= 0) {
                    retVals[1] = moveIndex32(fText, retVals[1], 1);
                }
            }
            return retVals;
        }
        
        
        // Match the following regular expression in the input text.
        //     (PR CM*)? ((OP | HY) CM*)? NU CM* ((NU | IS | SY) CM*) * (CL CM*)?  (PO CM*)?
        //      0  1       3    3    3        7     7    7    7    7      9   9     11 11    (match states)
        //  retVals array  [0]  index of the start of the match, or -1 if no match
        //                 [1]  index of first char following the match.
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
                        if (cLBType == UCharacter.LineBreak.PREFIX_NUMERIC) {
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
                        //     (PR CM*)? ((OP | HY) CM*)? NU CM* ((NU | IS | SY) CM*) * (CL CM*)?  (PO CM*)?
                        //      0  1       3    3    4    7   7     7    7    7    7      9   9     11 11    (match states)

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
                        if (cLBType == UCharacter.LineBreak.POSTFIX_NUMERIC) {
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
     * Move an index into a string by n code points.
     *   Similar to UTF16.moveCodePointOffset, but without the exceptions, which were
     *   complicating usage.
     * @param s   a Text string
     * @param i   The starting code unit index into the text string
     * @param amt  The amount to adjust the string by.
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
        if (c >= UTF16.SUPPLEMENTARY_MIN_VALUE) {
            retVal++;
        }
        return retVal;
    }


    //
    //  The following UnicodeSets are used in matching a Grapheme Cluster
    //
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
    private static int nextGC(StringBuffer s, int i) {
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
    // numIterations = 20;  
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
                errorText.append("<data>");

                String hexChars = "0123456789abcdef";
                int      c;    // Char from test data
                int      bn;
                for (ci = startContext;  ci <= endContext && ci != -1;  ci = nextCP(testText, ci)) {
                    if (ci == i) {
                        // This is the location of the error.
                        errorText.append("<?>");
                    } else if (expectedBreaks[ci]) {
                        // This a non-error expected break position.
                        errorText.append("<>");
                    }
                    if (ci < testText.length()) {
                        c = UTF16.charAt(testText, ci);
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

}

