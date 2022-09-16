// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2003-2016 International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.rbbi;


// Monkey testing of RuleBasedBreakIterator.
//    The old, original monkey test. TODO: remove
//    The new monkey test is class RBBIMonkeyTest.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

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
@RunWith(JUnit4.class)
public class RBBITestMonkey extends TestFmwk {
    //
    //     class RBBIMonkeyKind
    //
    //        Monkey Test for Break Iteration
    //        Abstract interface class.   Concrete derived classes independently
    //        implement the break rules for different iterator types.
    //
    //        The Monkey Test itself uses doesn't know which type of break iterator it is
    //        testing, but works purely in terms of the interface defined here.
    //
    abstract static class RBBIMonkeyKind {
        RBBIMonkeyKind() {
            fSets = new  ArrayList();
            fClassNames = new ArrayList();
            fAppliedRules = new ArrayList();
        }

        // Return a List of UnicodeSets, representing the character classes used
        //   for this type of iterator.
        abstract  List  charClasses();

        // Set the test text on which subsequent calls to next() will operate
        abstract  void   setText(StringBuffer text);

        // Find the next break position, starting from the specified position.
        // Return -1 after reaching end of string.
        abstract   int   next(int i);

        // Name of each character class, parallel with charClasses. Used for debugging output
        // of characters.
        List<String> characterClassNames() {
            return fClassNames;
        }

        void setAppliedRule(int position, String value) {
            fAppliedRules.set(position, value);
        }

        String getAppliedRule(int position) {
            return fAppliedRules.get(position);
        }

        String classNameFromCodepoint(int c) {
            // Simply iterate through fSets to find character's class
            for (int aClassNum = 0; aClassNum < charClasses().size(); aClassNum++) {
                UnicodeSet classSet = (UnicodeSet)charClasses().get(aClassNum);
                if (classSet.contains(c)) {
                    return fClassNames.get(aClassNum);
                }
            }
            return "bad class name";
        }

        int maxClassNameSize() {
            int maxSize = 0;
            for (int aClassNum = 0; aClassNum < charClasses().size(); aClassNum++) {
                if (fClassNames.get(aClassNum).length() > maxSize) {
                    maxSize = fClassNames.get(aClassNum).length();
                }
            }
            return maxSize;
        }

        // Clear `appliedRules` and fill it with empty strings in the size of test text.
        void prepareAppliedRules(int size) {
            // Remove all the information in the `appliedRules`.
            fAppliedRules.clear();
            fAppliedRules.ensureCapacity(size + 1);
            while (fAppliedRules.size() < size + 1) {
                fAppliedRules.add("");
            }
        }

        // A Character Property, one of the constants defined in class UProperty.
        //   The value of this property will be displayed for the characters
        //    near any test failure.
        int   fCharProperty;

        List fSets;
        ArrayList<String> fClassNames;
        ArrayList<String> fAppliedRules;
    }

    /**
     * Monkey test subclass for testing Character (Grapheme Cluster) boundaries.
     * Note: As of Unicode 6.1, fPrependSet is empty, so don't add it to fSets
     */
    static class RBBICharMonkey extends RBBIMonkeyKind {
        UnicodeSet                fCRLFSet;
        UnicodeSet                fControlSet;
        UnicodeSet                fExtendSet;
        UnicodeSet                fRegionalIndicatorSet;
        UnicodeSet                fPrependSet;
        UnicodeSet                fSpacingSet;
        UnicodeSet                fLSet;
        UnicodeSet                fVSet;
        UnicodeSet                fTSet;
        UnicodeSet                fLVSet;
        UnicodeSet                fLVTSet;
        UnicodeSet                fHangulSet;
        UnicodeSet                fZWJSet;
        UnicodeSet                fExtendedPictSet;
        UnicodeSet                fViramaSet;
        UnicodeSet                fLinkingConsonantSet;
        UnicodeSet                fExtCccZwjSet;
        UnicodeSet                fAnySet;


        StringBuffer              fText;

        RBBICharMonkey() {
            fText       = null;
            fCharProperty = UProperty.GRAPHEME_CLUSTER_BREAK;
            fCRLFSet    = new UnicodeSet("[\\r\\n]");
            fControlSet = new UnicodeSet("[\\p{Grapheme_Cluster_Break = Control}]");
            fExtendSet  = new UnicodeSet("[\\p{Grapheme_Cluster_Break = Extend}]");
            fZWJSet     = new UnicodeSet("[\\p{Grapheme_Cluster_Break = ZWJ}]");
            fRegionalIndicatorSet = new UnicodeSet("[\\p{Grapheme_Cluster_Break = Regional_Indicator}]");
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

            fExtendedPictSet  = new UnicodeSet("[:Extended_Pictographic:]");
            fViramaSet        = new UnicodeSet("[\\p{Gujr}\\p{sc=Telu}\\p{sc=Mlym}\\p{sc=Orya}\\p{sc=Beng}\\p{sc=Deva}&"
                                               + "\\p{Indic_Syllabic_Category=Virama}]");
            fLinkingConsonantSet = new UnicodeSet("[\\p{Gujr}\\p{sc=Telu}\\p{sc=Mlym}\\p{sc=Orya}\\p{sc=Beng}\\p{sc=Deva}&"
                                                  + "\\p{Indic_Syllabic_Category=Consonant}]");
            fExtCccZwjSet     = new UnicodeSet("[[\\p{gcb=Extend}-\\p{ccc=0}] \\p{gcb=ZWJ}]");
            fAnySet           = new UnicodeSet("[\\u0000-\\U0010ffff]");


            fSets.add(fCRLFSet);               fClassNames.add("CRLF");
            fSets.add(fControlSet);            fClassNames.add("Control");
            fSets.add(fExtendSet);             fClassNames.add("Extended");
            fSets.add(fRegionalIndicatorSet);  fClassNames.add("RegionalIndicator");
            if (!fPrependSet.isEmpty()) {
                fSets.add(fPrependSet);        fClassNames.add("Prepend");
            }
            fSets.add(fSpacingSet);            fClassNames.add("Spacing");
            fSets.add(fHangulSet);             fClassNames.add("Hangul");
            fSets.add(fAnySet);                fClassNames.add("Any");
            fSets.add(fZWJSet);                fClassNames.add("ZWJ");
            fSets.add(fExtendedPictSet);       fClassNames.add("ExtendedPict");
            fSets.add(fViramaSet);             fClassNames.add("Virama");
            fSets.add(fLinkingConsonantSet);   fClassNames.add("LinkingConsonant");
            fSets.add(fExtCccZwjSet);          fClassNames.add("ExtCccZwj");
        }


        @Override
        void setText(StringBuffer s) {
            fText = s;
            prepareAppliedRules(s.length());
        }

        @Override
        List charClasses() {
            return fSets;
        }

        @Override
        int next(int prevPos) {
            int    /*p0,*/ p1, p2, p3;    // Indices of the significant code points around the
            //   break position being tested.  The candidate break
            //   location is before p2.

            int     breakPos = -1;

            int   c0, c1, c2, c3;     // The code points at p0, p1, p2 & p3.
            int   cBase;              // for (X Extend*) patterns, the X character.

            // Previous break at end of string.  return DONE.
            if (prevPos >= fText.length()) {
                return -1;
            }
            /* p0 = */ p1 = p2 = p3 = prevPos;
            c3 =  UTF16.charAt(fText, prevPos);
            c0 = c1 = c2 = cBase = 0;

            // Loop runs once per "significant" character position in the input text.
            for (;;) {
                // Move all of the positions forward in the input string.
                /* p0 = p1;*/  c0 = c1;
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
                    setAppliedRule(p2, "End of String");
                    break;
                }

                //     No Extend or Format characters may appear between the CR and LF,
                //     which requires the additional check for p2 immediately following p1.
                //
                if (c1==0x0D && c2==0x0A && p1==(p2-1)) {
                    setAppliedRule(p2, "GB 3   CR x LF");
                    continue;
                }

                if (fControlSet.contains(c1) ||
                        c1 == 0x0D ||
                        c1 == 0x0A)  {
                    setAppliedRule(p2, "GB 4   ( Control | CR | LF ) <break>");
                    break;
                }

                if (fControlSet.contains(c2) ||
                        c2 == 0x0D ||
                        c2 == 0x0A)  {
                    setAppliedRule(p2, "GB 5   <break>  ( Control | CR | LF )");
                    break;
                }


                if (fLSet.contains(c1) &&
                        (fLSet.contains(c2)  ||
                                fVSet.contains(c2)  ||
                                fLVSet.contains(c2) ||
                                fLVTSet.contains(c2))) {
                    setAppliedRule(p2, "GB 6   L x ( L | V | LV | LVT )");
                    continue;
                }

                if ((fLVSet.contains(c1) || fVSet.contains(c1)) &&
                        (fVSet.contains(c2) || fTSet.contains(c2)))  {
                    setAppliedRule(p2, "GB 7   ( LV | V )  x  ( V | T )");
                    continue;
                }

                if ((fLVTSet.contains(c1) || fTSet.contains(c1)) &&
                        fTSet.contains(c2))  {
                    setAppliedRule(p2, "GB 8   ( LVT | T)  x T");
                    continue;
                }

                if (fExtendSet.contains(c2) || fZWJSet.contains(c2))  {
                    if (!fExtendSet.contains(c1)) {
                        cBase = c1;
                    }
                    setAppliedRule(p2, "GB 9   x (Extend | ZWJ)");
                    continue;
                }

                if (fSpacingSet.contains(c2)) {
                    setAppliedRule(p2, "GB 9a  x  SpacingMark");
                    continue;
                }

                if (fPrependSet.contains(c1)) {
                    setAppliedRule(p2, "GB 9b  Prepend x");
                    continue;
                }

                //   Note: Viramas are also included in the ExtCccZwj class.
                if (fLinkingConsonantSet.contains(c2)) {
                    int pi = p1;
                    boolean sawVirama = false;
                    while (pi > 0 && fExtCccZwjSet.contains(fText.codePointAt(pi))) {
                        if (fViramaSet.contains(fText.codePointAt(pi))) {
                            sawVirama = true;
                        }
                        pi = fText.offsetByCodePoints(pi, -1);
                    }
                    if (sawVirama && fLinkingConsonantSet.contains(fText.codePointAt(pi))) {
                        setAppliedRule(p2, "GB 9.3 LinkingConsonant ExtCccZwj* Virama ExtCccZwj* × LinkingConsonant");
                        continue;
                    }
                }

                if (fExtendedPictSet.contains(cBase) && fZWJSet.contains(c1) && fExtendedPictSet.contains(c2) ) {
                    setAppliedRule(p2, "GB 11  Extended_Pictographic Extend * ZWJ x Extended_Pictographic");
                    continue;
                }

                //                  Note: The first if condition is a little tricky. We only need to force
                //                      a break if there are three or more contiguous RIs. If there are
                //                      only two, a break following will occur via other rules, and will include
                //                      any trailing extend characters, which is needed behavior.
                if (fRegionalIndicatorSet.contains(c0) && fRegionalIndicatorSet.contains(c1)
                        && fRegionalIndicatorSet.contains(c2)) {
                    setAppliedRule(p2, "GB 12-13 Regional_Indicator x Regional_Indicator");
                    break;
                }
                if (fRegionalIndicatorSet.contains(c1) && fRegionalIndicatorSet.contains(c2)) {
                    setAppliedRule(p2, "GB 12-13 Regional_Indicator x Regional_Indicator");
                    continue;
                }

                setAppliedRule(p2, "GB 999 Any <break> Any");
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
        StringBuffer              fText;

        UnicodeSet                fCRSet;
        UnicodeSet                fLFSet;
        UnicodeSet                fNewlineSet;
        UnicodeSet                fRegionalIndicatorSet;
        UnicodeSet                fKatakanaSet;
        UnicodeSet                fHebrew_LetterSet;
        UnicodeSet                fALetterSet;
        UnicodeSet                fSingle_QuoteSet;
        UnicodeSet                fDouble_QuoteSet;
        UnicodeSet                fMidNumLetSet;
        UnicodeSet                fMidLetterSet;
        UnicodeSet                fMidNumSet;
        UnicodeSet                fNumericSet;
        UnicodeSet                fFormatSet;
        UnicodeSet                fExtendSet;
        UnicodeSet                fExtendNumLetSet;
        UnicodeSet                fWSegSpaceSet;
        UnicodeSet                fOtherSet;
        UnicodeSet                fDictionarySet;
        UnicodeSet                fZWJSet;
        UnicodeSet                fExtendedPictSet;

        RBBIWordMonkey() {
            fCharProperty    = UProperty.WORD_BREAK;

            fCRSet           = new UnicodeSet("[\\p{Word_Break = CR}]");
            fLFSet           = new UnicodeSet("[\\p{Word_Break = LF}]");
            fNewlineSet      = new UnicodeSet("[\\p{Word_Break = Newline}]");
            fRegionalIndicatorSet = new UnicodeSet("[\\p{Word_Break = Regional_Indicator}]");
            fKatakanaSet     = new UnicodeSet("[\\p{Word_Break = Katakana}]");
            fHebrew_LetterSet = new UnicodeSet("[\\p{Word_Break = Hebrew_Letter}]");
            fALetterSet      = new UnicodeSet("[\\p{Word_Break = ALetter} @]");
            fSingle_QuoteSet = new UnicodeSet("[\\p{Word_Break = Single_Quote}]");
            fDouble_QuoteSet = new UnicodeSet("[\\p{Word_Break = Double_Quote}]");
            fMidNumLetSet    = new UnicodeSet("[\\p{Word_Break = MidNumLet}]");
            fMidLetterSet    = new UnicodeSet("[\\p{Word_Break = MidLetter} - [\\: \\uFE55 \\uFF1A]]");
            fMidNumSet       = new UnicodeSet("[\\p{Word_Break = MidNum}]");
            fNumericSet      = new UnicodeSet("[\\p{Word_Break = Numeric}]");
            fFormatSet       = new UnicodeSet("[\\p{Word_Break = Format}]");
            fExtendNumLetSet = new UnicodeSet("[\\p{Word_Break = ExtendNumLet}]");
            // There are some sc=Hani characters with WB=Extend.
            // The break rules need to pick one or the other because
            // Extend overlapping with something else is messy.
            // For Unicode 13, we chose to keep U+16FF0 & U+16FF1
            // in $Han (for $dictionary) and out of $Extend.
            fExtendSet       = new UnicodeSet("[\\p{Word_Break = Extend}-[:Hani:]]");
            fWSegSpaceSet    = new UnicodeSet("[\\p{Word_Break = WSegSpace}]");
            fZWJSet          = new UnicodeSet("[\\p{Word_Break = ZWJ}]");
            fExtendedPictSet = new UnicodeSet("[:Extended_Pictographic:]");

            fDictionarySet = new UnicodeSet("[[\\uac00-\\ud7a3][:Han:][:Hiragana:]]");
            fDictionarySet.addAll(fKatakanaSet);
            fDictionarySet.addAll(new UnicodeSet("[\\p{LineBreak = Complex_Context}]"));

            fALetterSet.removeAll(fDictionarySet);

            fOtherSet        = new UnicodeSet();
            fOtherSet.complement();
            fOtherSet.removeAll(fCRSet);
            fOtherSet.removeAll(fLFSet);
            fOtherSet.removeAll(fNewlineSet);
            fOtherSet.removeAll(fALetterSet);
            fOtherSet.removeAll(fSingle_QuoteSet);
            fOtherSet.removeAll(fDouble_QuoteSet);
            fOtherSet.removeAll(fKatakanaSet);
            fOtherSet.removeAll(fHebrew_LetterSet);
            fOtherSet.removeAll(fMidLetterSet);
            fOtherSet.removeAll(fMidNumSet);
            fOtherSet.removeAll(fNumericSet);
            fOtherSet.removeAll(fFormatSet);
            fOtherSet.removeAll(fExtendSet);
            fOtherSet.removeAll(fExtendNumLetSet);
            fOtherSet.removeAll(fWSegSpaceSet);
            fOtherSet.removeAll(fRegionalIndicatorSet);
            fOtherSet.removeAll(fZWJSet);
            fOtherSet.removeAll(fExtendedPictSet);

            // Inhibit dictionary characters from being tested at all.
            // remove surrogates so as to not generate higher CJK characters
            fOtherSet.removeAll(new UnicodeSet("[[\\p{LineBreak = Complex_Context}][:Line_Break=Surrogate:]]"));
            fOtherSet.removeAll(fDictionarySet);

            fSets.add(fCRSet);                    fClassNames.add("CR");
            fSets.add(fLFSet);                    fClassNames.add("LF");
            fSets.add(fNewlineSet);               fClassNames.add("Newline");
            fSets.add(fRegionalIndicatorSet);     fClassNames.add("RegionalIndicator");
            fSets.add(fHebrew_LetterSet);         fClassNames.add("Hebrew");
            fSets.add(fALetterSet);               fClassNames.add("ALetter");
            //fSets.add(fKatakanaSet);  // Omit Katakana from fSets, which omits Katakana characters
            // from the test data. They are all in the dictionary set,
            // which this (old, to be retired) monkey test cannot handle.
            fSets.add(fSingle_QuoteSet);          fClassNames.add("Single Quote");
            fSets.add(fDouble_QuoteSet);          fClassNames.add("Double Quote");
            fSets.add(fMidLetterSet);             fClassNames.add("MidLetter");
            fSets.add(fMidNumLetSet);             fClassNames.add("MidNumLet");
            fSets.add(fMidNumSet);                fClassNames.add("MidNum");
            fSets.add(fNumericSet);               fClassNames.add("Numeric");
            fSets.add(fFormatSet);                fClassNames.add("Format");
            fSets.add(fExtendSet);                fClassNames.add("Extend");
            fSets.add(fExtendNumLetSet);          fClassNames.add("ExtendNumLet");
            fSets.add(fWSegSpaceSet);             fClassNames.add("WSegSpace");
            fSets.add(fZWJSet);                   fClassNames.add("ZWJ");
            fSets.add(fExtendedPictSet);          fClassNames.add("ExtendedPict");
            fSets.add(fOtherSet);                 fClassNames.add("Other");
        }


        @Override
        List  charClasses() {
            return fSets;
        }

        @Override
        void   setText(StringBuffer s) {
            fText = s;
            prepareAppliedRules(s.length());
        }

        @Override
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

                // Advance p3 by    X(Extend | Format)*   Rule 4
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
                while (setContains(fFormatSet, c3) || setContains(fExtendSet, c3) || setContains(fZWJSet, c3));

                if (p1 == p2) {
                    // Still warming up the loop.  (won't work with zero length strings, but we don't care)
                    continue;
                }
                if (p2 == fText.length()) {
                    // Reached end of string.  Always a break position.
                    break;
                }

                //     No Extend or Format characters may appear between the CR and LF,
                //     which requires the additional check for p2 immediately following p1.
                //
                if (c1==0x0D && c2==0x0A) {
                    setAppliedRule(p2, "WB 3   CR x LF");
                    continue;
                }

                //
                if (fCRSet.contains(c1) || fLFSet.contains(c1) || fNewlineSet.contains(c1)) {
                    setAppliedRule(p2, "WB 3a  Break before and after newlines (including CR and LF)");
                    break;
                }
                if (fCRSet.contains(c2) || fLFSet.contains(c2) || fNewlineSet.contains(c2)) {
                    setAppliedRule(p2, "WB 3a  Break before and after newlines (including CR and LF)");
                    break;
                }

                //              Not ignoring extend chars, so peek into input text to
                //              get the potential ZWJ, the character immediately preceding c2.
                if (fZWJSet.contains(fText.codePointBefore(p2)) && fExtendedPictSet.contains(c2)) {
                    setAppliedRule(p2, "WB 3c  ZWJ x Extended_Pictographic");
                    continue;
                }

                if (fWSegSpaceSet.contains(fText.codePointBefore(p2)) && fWSegSpaceSet.contains(c2)) {
                    setAppliedRule(p2, "WB 3d  Keep horizontal whitespace together");
                    continue;
                }

                if ((fALetterSet.contains(c1) || fHebrew_LetterSet.contains(c1)) &&
                        (fALetterSet.contains(c2) || fHebrew_LetterSet.contains(c2)))  {
                    setAppliedRule(p2, "WB 4   (ALetter | Hebrew_Letter) x (ALetter | Hebrew_Letter)");
                    continue;
                }

                if ( (fALetterSet.contains(c1) || fHebrew_LetterSet.contains(c1))   &&
                        (fMidLetterSet.contains(c2) || fMidNumLetSet.contains(c2) || fSingle_QuoteSet.contains(c2)) &&
                        (setContains(fALetterSet, c3) || setContains(fHebrew_LetterSet, c3))) {
                    setAppliedRule(p2, "WB 6   (ALetter | Hebrew_Letter)  x  (MidLetter | MidNumLet | Single_Quote) (ALetter | Hebrew_Letter)");
                    continue;
                }

                if ((fALetterSet.contains(c0) || fHebrew_LetterSet.contains(c0)) &&
                        (fMidLetterSet.contains(c1) || fMidNumLetSet.contains(c1) || fSingle_QuoteSet.contains(c1)) &&
                        (fALetterSet.contains(c2) || fHebrew_LetterSet.contains(c2))) {
                    setAppliedRule(p2, "WB 7   (ALetter | Hebrew_Letter) (MidLetter | MidNumLet | Single_Quote)  x  (ALetter | Hebrew_Letter)");
                    continue;
                }

                if (fHebrew_LetterSet.contains(c1) && fSingle_QuoteSet.contains(c2)) {
                    setAppliedRule(p2, "WB 7a  Hebrew_Letter x Single_Quote");
                    continue;
                }

                if (fHebrew_LetterSet.contains(c1) && fDouble_QuoteSet.contains(c2) && setContains(fHebrew_LetterSet,c3)) {
                    setAppliedRule(p2, "WB 7b  Hebrew_Letter x Single_Quote");
                    continue;
                }

                if (fHebrew_LetterSet.contains(c0) && fDouble_QuoteSet.contains(c1) && fHebrew_LetterSet.contains(c2)) {
                    setAppliedRule(p2, "WB 7c  Hebrew_Letter Double_Quote x Hebrew_Letter");
                    continue;
                }

                if (fNumericSet.contains(c1) &&
                        fNumericSet.contains(c2))  {
                    setAppliedRule(p2, "WB 8   Numeric x Numeric");
                    continue;
                }

                if ((fALetterSet.contains(c1) || fHebrew_LetterSet.contains(c1)) &&
                        fNumericSet.contains(c2))  {
                    setAppliedRule(p2, "WB 9   (ALetter | Hebrew_Letter) x Numeric");
                    continue;
                }

                if (fNumericSet.contains(c1) &&
                        (fALetterSet.contains(c2) || fHebrew_LetterSet.contains(c2)))  {
                    setAppliedRule(p2, "WB 10  Numeric x (ALetter | Hebrew_Letter)");
                    continue;
                }

                if (fNumericSet.contains(c0) &&
                        (fMidNumSet.contains(c1) || fMidNumLetSet.contains(c1) || fSingle_QuoteSet.contains(c1))  &&
                        fNumericSet.contains(c2)) {
                    setAppliedRule(p2, "WB 11  Numeric (MidNum | MidNumLet | Single_Quote)  x  Numeric");
                    continue;
                }

                if (fNumericSet.contains(c1) &&
                        (fMidNumSet.contains(c2) || fMidNumLetSet.contains(c2) || fSingle_QuoteSet.contains(c2))  &&
                        setContains(fNumericSet, c3)) {
                    setAppliedRule(p2, "WB 12  Numeric x (MidNum | MidNumLet | SingleQuote) Numeric");
                    continue;
                }

                //            Note: matches UAX 29 rules, but doesn't come into play for ICU because
                //                  all Katakana are handled by the dictionary breaker.
                if (fKatakanaSet.contains(c1) &&
                        fKatakanaSet.contains(c2))  {
                    setAppliedRule(p2, "WB 13  Katakana x Katakana");
                    continue;
                }

                if ((fALetterSet.contains(c1) || fHebrew_LetterSet.contains(c1) ||fNumericSet.contains(c1) ||
                        fKatakanaSet.contains(c1) || fExtendNumLetSet.contains(c1)) &&
                        fExtendNumLetSet.contains(c2)) {
                    setAppliedRule(p2, "WB 13a (ALetter | Hebrew_Letter | Numeric | KataKana | ExtendNumLet) x ExtendNumLet");
                    continue;
                }

                if (fExtendNumLetSet.contains(c1) &&
                        (fALetterSet.contains(c2) || fHebrew_LetterSet.contains(c2) ||
                                fNumericSet.contains(c2) || fKatakanaSet.contains(c2)))  {
                    setAppliedRule(p2, "WB 13b ExtendNumLet x (ALetter | Hebrew_Letter | Numeric | Katakana)");
                    continue;
                }


                if (fRegionalIndicatorSet.contains(c0) && fRegionalIndicatorSet.contains(c1)) {
                    setAppliedRule(p2, "WB 15-17 Group pairs of Regional Indicators.");
                    break;
                }
                if (fRegionalIndicatorSet.contains(c1) && fRegionalIndicatorSet.contains(c2)) {
                    setAppliedRule(p2, "WB 15-17 Group pairs of Regional Indicators.");
                    continue;
                }

                setAppliedRule(p2, "WB 999");
                break;
            }

            breakPos = p2;
            return breakPos;
        }
    }


    static class RBBILineMonkey extends RBBIMonkeyKind {
        // UnicodeSets for each of the Line Breaking character classes.
        // Order matches that of Unicode UAX 14, Table 1, which makes it a little easier
        // to verify that they are all accounted for.

        // XUnicodeSet is like UnicodeSet, except that the method contains(int codePoint) does not
        // throw exceptions on out-of-range codePoints. This matches ICU4C behavior.
        // The LineMonkey test (ported from ICU4C) relies on this behavior, it uses a value of -1
        // to represent a non-codepoint that is not included in any of the property sets.
        // This happens for rule 30a.
        class XUnicodeSet extends UnicodeSet {
            XUnicodeSet(String pattern) { super(pattern); }
            XUnicodeSet() { super(); }
            @Override
            public boolean contains(int codePoint) {
                return codePoint < UnicodeSet.MIN_VALUE || codePoint > UnicodeSet.MAX_VALUE ?
                        false : super.contains(codePoint);
            }
        }

        // Declare these variables as XUnicodeSet, not merely as UnicodeSet,
        // so that when we copy a new declaration from C++ (where only UnicodeSet exists),
        // the missing 'X' prefix is visible;
        // and when the prefix is there and we copy a new initializer we get a compiler error.
        // (Otherwise we rely on the caller catching the IAE from using codePoint=-1
        // and failing with a message that tells us what to do.)
        XUnicodeSet fBK;
        XUnicodeSet fCR;
        XUnicodeSet fLF;
        XUnicodeSet fCM;
        XUnicodeSet fNL;
        XUnicodeSet fSG;
        XUnicodeSet fWJ;
        XUnicodeSet fZW;
        XUnicodeSet fGL;
        XUnicodeSet fSP;
        XUnicodeSet fB2;
        XUnicodeSet fBA;
        XUnicodeSet fBB;
        XUnicodeSet fHH;
        XUnicodeSet fHY;
        XUnicodeSet fCB;
        XUnicodeSet fCL;
        XUnicodeSet fCP;
        XUnicodeSet fEX;
        XUnicodeSet fIN;
        XUnicodeSet fNS;
        XUnicodeSet fOP;
        XUnicodeSet fQU;
        XUnicodeSet fIS;
        XUnicodeSet fNU;
        XUnicodeSet fPO;
        XUnicodeSet fPR;
        XUnicodeSet fSY;
        XUnicodeSet fAI;
        XUnicodeSet fAL;
        XUnicodeSet fCJ;
        XUnicodeSet fH2;
        XUnicodeSet fH3;
        XUnicodeSet fHL;
        XUnicodeSet fID;
        XUnicodeSet fJL;
        XUnicodeSet fJV;
        XUnicodeSet fJT;
        XUnicodeSet fRI;
        XUnicodeSet fXX;
        XUnicodeSet fEB;
        XUnicodeSet fEM;
        XUnicodeSet fZWJ;
        XUnicodeSet fOP30;
        XUnicodeSet fCP30;
        XUnicodeSet fExtPictUnassigned;

        StringBuffer  fText;
        int           fOrigPositions;

        RBBILineMonkey()
        {
            fCharProperty  = UProperty.LINE_BREAK;

            fBK    = new XUnicodeSet("[\\p{Line_Break=BK}]");
            fCR    = new XUnicodeSet("[\\p{Line_break=CR}]");
            fLF    = new XUnicodeSet("[\\p{Line_break=LF}]");
            fCM    = new XUnicodeSet("[\\p{Line_break=CM}]");
            fNL    = new XUnicodeSet("[\\p{Line_break=NL}]");
            fSG    = new XUnicodeSet("[\\ud800-\\udfff]");
            fWJ    = new XUnicodeSet("[\\p{Line_break=WJ}]");
            fZW    = new XUnicodeSet("[\\p{Line_break=ZW}]");
            fGL    = new XUnicodeSet("[\\p{Line_break=GL}]");
            fSP    = new XUnicodeSet("[\\p{Line_break=SP}]");
            fB2    = new XUnicodeSet("[\\p{Line_break=B2}]");
            fBA    = new XUnicodeSet("[\\p{Line_break=BA}]");
            fBB    = new XUnicodeSet("[\\p{Line_break=BB}]");
            fHH    = new XUnicodeSet();
            fHY    = new XUnicodeSet("[\\p{Line_break=HY}]");
            fCB    = new XUnicodeSet("[\\p{Line_break=CB}]");
            fCL    = new XUnicodeSet("[\\p{Line_break=CL}]");
            fCP    = new XUnicodeSet("[\\p{Line_break=CP}]");
            fEX    = new XUnicodeSet("[\\p{Line_break=EX}]");
            fIN    = new XUnicodeSet("[\\p{Line_break=IN}]");
            fNS    = new XUnicodeSet("[\\p{Line_break=NS}]");
            fOP    = new XUnicodeSet("[\\p{Line_break=OP}]");
            fQU    = new XUnicodeSet("[\\p{Line_break=QU}]");
            fIS    = new XUnicodeSet("[\\p{Line_break=IS}]");
            fNU    = new XUnicodeSet("[\\p{Line_break=NU}]");
            fPO    = new XUnicodeSet("[\\p{Line_break=PO}]");
            fPR    = new XUnicodeSet("[\\p{Line_break=PR}]");
            fSY    = new XUnicodeSet("[\\p{Line_break=SY}]");
            fAI    = new XUnicodeSet("[\\p{Line_break=AI}]");
            fAL    = new XUnicodeSet("[\\p{Line_break=AL}]");
            fCJ    = new XUnicodeSet("[\\p{Line_break=CJ}]");
            fH2    = new XUnicodeSet("[\\p{Line_break=H2}]");
            fH3    = new XUnicodeSet("[\\p{Line_break=H3}]");
            fHL    = new XUnicodeSet("[\\p{Line_break=HL}]");
            fID    = new XUnicodeSet("[\\p{Line_break=ID}]");
            fJL    = new XUnicodeSet("[\\p{Line_break=JL}]");
            fJV    = new XUnicodeSet("[\\p{Line_break=JV}]");
            fJT    = new XUnicodeSet("[\\p{Line_break=JT}]");
            fRI    = new XUnicodeSet("[\\p{Line_break=RI}]");
            fXX    = new XUnicodeSet("[\\p{Line_break=XX}]");
            fEB    = new XUnicodeSet("[\\p{Line_break=EB}]");
            fEM    = new XUnicodeSet("[\\p{Line_break=EM}]");
            fZWJ   = new XUnicodeSet("[\\p{Line_break=ZWJ}]");
            fOP30  = new XUnicodeSet("[\\p{Line_break=OP}-[\\p{ea=F}\\p{ea=W}\\p{ea=H}]]");
            fCP30  = new XUnicodeSet("[\\p{Line_break=CP}-[\\p{ea=F}\\p{ea=W}\\p{ea=H}]]");
            fExtPictUnassigned = new XUnicodeSet("[\\p{Extended_Pictographic}&\\p{Cn}]");

            // Remove dictionary characters.
            // The monkey test reference implementation of line break does not replicate the dictionary behavior,
            // so dictionary characters are omitted from the monkey test data.
            @SuppressWarnings("unused")
            UnicodeSet dictionarySet = new UnicodeSet(
                    "[[:LineBreak = Complex_Context:] & [[:Script = Thai:][:Script = Lao:][:Script = Khmer:] [:script = Myanmar:]]]");

            fAL.addAll(fXX);     // Default behavior for XX is identical to AL
            fAL.addAll(fAI);     // Default behavior for AI is identical to AL
            fAL.addAll(fSG);     // Default behavior for SG (unpaired surrogates) is AL

            fNS.addAll(fCJ);     // Default behavior for CJ is identical to NS.
            fCM.addAll(fZWJ);    // ZWJ behaves as a CM.

            fHH.add('\u2010');   // Hyphen, '‐'

            fSets.add(fBK);     fClassNames.add("BK");
            fSets.add(fCR);     fClassNames.add("CR");
            fSets.add(fLF);     fClassNames.add("LF");
            fSets.add(fCM);     fClassNames.add("CM");
            fSets.add(fNL);     fClassNames.add("NL");
            fSets.add(fWJ);     fClassNames.add("WJ");
            fSets.add(fZW);     fClassNames.add("ZW");
            fSets.add(fGL);     fClassNames.add("GL");
            fSets.add(fSP);     fClassNames.add("SP");
            fSets.add(fB2);     fClassNames.add("B2");
            fSets.add(fBA);     fClassNames.add("BA");
            fSets.add(fBB);     fClassNames.add("BB");
            fSets.add(fHY);     fClassNames.add("HY");
            fSets.add(fCB);     fClassNames.add("CB");
            fSets.add(fCL);     fClassNames.add("CL");
            fSets.add(fCP);     fClassNames.add("CP");
            fSets.add(fEX);     fClassNames.add("EX");
            fSets.add(fIN);     fClassNames.add("IN");
            fSets.add(fJL);     fClassNames.add("JL");
            fSets.add(fJT);     fClassNames.add("JT");
            fSets.add(fJV);     fClassNames.add("JV");
            fSets.add(fNS);     fClassNames.add("NV");
            fSets.add(fOP);     fClassNames.add("OP");
            fSets.add(fQU);     fClassNames.add("QU");
            fSets.add(fIS);     fClassNames.add("IS");
            fSets.add(fNU);     fClassNames.add("NU");
            fSets.add(fPO);     fClassNames.add("PO");
            fSets.add(fPR);     fClassNames.add("PR");
            fSets.add(fSY);     fClassNames.add("SY");
            fSets.add(fAI);     fClassNames.add("AI");
            fSets.add(fAL);     fClassNames.add("AL");
            fSets.add(fH2);     fClassNames.add("H2");
            fSets.add(fH3);     fClassNames.add("H3");
            fSets.add(fHL);     fClassNames.add("HL");
            fSets.add(fID);     fClassNames.add("ID");
            fSets.add(fRI);     fClassNames.add("RI");
            fSets.add(fSG);     fClassNames.add("SG");
            fSets.add(fEB);     fClassNames.add("EB");
            fSets.add(fEM);     fClassNames.add("EM");
            fSets.add(fZWJ);    fClassNames.add("ZWJ");
            // TODO: fOP30 & fCP30 overlap with plain fOP. Probably OK, but fOP/CP chars will be over-represented.
            fSets.add(fOP30);   fClassNames.add("OP30");
            fSets.add(fCP30);   fClassNames.add("CP30");
            fSets.add(fExtPictUnassigned); fClassNames.add("fExtPictUnassigned");
        }

        @Override
        void setText(StringBuffer s) {
            fText       = s;
            prepareAppliedRules(s.length());
        }




        @Override
        int next(int startPos) {
            int    pos;       //  Index of the char following a potential break position
            int    thisChar;  //  Character at above position "pos"

            int    prevPos;   //  Index of the char preceding a potential break position
            int    prevChar;  //  Character at above position.  Note that prevChar
            //                //  and thisChar may not be adjacent because combining
            //                //  characters between them will be ignored.

            int    prevPosX2;
            int    prevCharX2; //  Character before prevChar, more context for LB 21a

            int    nextPos;   //  Index of the next character following pos.
            //                //  Usually skips over combining marks.
            int    tPos;      //  temp value.
            int    matchVals[]  = null;       // Number  Expression Match Results


            if (startPos >= fText.length()) {
                return -1;
            }


            // Initial values for loop.  Loop will run the first time without finding breaks,
            //                           while the invalid values shift out and the "this" and
            //                           "prev" positions are filled in with good values.
            pos      = prevPos   = prevPosX2  = -1;    // Invalid value, serves as flag for initial loop iteration.
            thisChar = prevChar  = prevCharX2 =  0;
            nextPos  = startPos;


            // Loop runs once per position in the test text, until a break position
            //  is found.  In each iteration, we are testing for a possible break
            //  just preceding the character at index "pos".  The character preceding
            //  this char is at position "prevPos"; because of combining sequences,
            //  "prevPos" can be arbitrarily far before "pos".
            for (;;) {
                // Advance to the next position to be tested.
                prevPosX2  = prevPos;
                prevCharX2 = prevChar;
                prevPos   = pos;
                prevChar  = thisChar;
                pos       = nextPos;
                nextPos   = moveIndex32(fText, pos, 1);

                if (pos >= fText.length()) {
                    setAppliedRule(pos, "LB 2   Break at end of text");
                    break;
                }

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
                    setAppliedRule(pos, "LB 9   adjust for combining sequences.");
                    continue;
                }

                if (fBK.contains(prevChar)) {
                    setAppliedRule(pos, "LB 4   Always break after hard line breaks");
                    break;
                }

                if (fCR.contains(prevChar) && fLF.contains(thisChar)) {
                    setAppliedRule(pos, "LB 5   Break after CR, LF, NL, but not inside CR LF");
                    continue;
                }
                if  (fCR.contains(prevChar) ||
                        fLF.contains(prevChar) ||
                        fNL.contains(prevChar))  {
                    setAppliedRule(pos, "LB 5   Break after CR, LF, NL, but not inside CR LF");
                    break;
                }

                if (fBK.contains(thisChar) || fCR.contains(thisChar) ||
                        fLF.contains(thisChar) || fNL.contains(thisChar) ) {
                    setAppliedRule(pos, "LB 6   Don't break before hard line breaks");
                    continue;
                }


                if (fSP.contains(thisChar)) {
                    setAppliedRule(pos, "LB 7   Don't break before spaces or zero-width space");
                    continue;
                }

                if (fZW.contains(thisChar)) {
                    setAppliedRule(pos, "LB 7   Don't break before spaces or zero-width space");
                    continue;
                }

                //       ZW SP* ÷
                //       Scan backwards from prevChar for SP* ZW
                tPos = prevPos;
                while (tPos > 0 && fSP.contains(UTF16.charAt(fText, tPos))) {
                    tPos = moveIndex32(fText, tPos, -1);
                }
                if (fZW.contains(UTF16.charAt(fText, tPos))) {
                    setAppliedRule(pos, "LB 8   Break after zero width space");
                    break;
                }

                //          Move this test up, before LB8a, because numbers can match a longer sequence that would
                //          also match 8a.  e.g. NU ZWJ IS PO     (ZWJ acts like CM)
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
                        setAppliedRule(pos, "LB 25  Numbers");
                        continue;
                    }
                }

                //       The monkey test's way of ignoring combining characters doesn't work
                //       for this rule. ZWJ is also a CM. Need to get the actual character
                //       preceding "thisChar", not ignoring combining marks, possibly ZWJ.
                {
                    int prevC = fText.codePointBefore(pos);
                    if (fZWJ.contains(prevC)) {
                        setAppliedRule(pos, "LB 8a  ZWJ x");
                        continue;
                    }
                }

                // appliedRule: "LB 9, 10"; //  Already done, at top of loop.";


                //    x  WJ
                //    WJ  x
                if (fWJ.contains(thisChar) || fWJ.contains(prevChar)) {
                    setAppliedRule(pos, "LB 11  Do not break before or after WORD JOINER and related characters.");
                    continue;
                }


                if (fGL.contains(prevChar)) {
                    setAppliedRule(pos, "LB 12  GL  x");
                    continue;
                }

                if (!(fSP.contains(prevChar) ||
                        fBA.contains(prevChar) ||
                        fHY.contains(prevChar)     ) && fGL.contains(thisChar)) {
                    setAppliedRule(pos, "LB 12a [^SP BA HY] x GL");
                    continue;
                }

                if (fCL.contains(thisChar) ||
                        fCP.contains(thisChar) ||
                        fEX.contains(thisChar) ||
                        fSY.contains(thisChar)) {
                    setAppliedRule(pos, "LB 13  Don't break before closings");
                    continue;
                }

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
                    setAppliedRule(pos, "LB 14  Don't break after OP SP*");
                    continue;
                }

                if (nextPos < fText.length()) {
                    int nextChar = fText.codePointAt(nextPos);
                    if (fSP.contains(prevChar) && fIS.contains(thisChar) && fNU.contains(nextChar)) {
                        setAppliedRule(pos, "LB 14a Break before an IS that begins a number and follows a space");
                        break;
                    }
                }

                if (fIS.contains(thisChar)) {
                    setAppliedRule(pos, "LB 14b Do not break before numeric separators, even after spaces");
                    continue;
                }

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
                        setAppliedRule(pos, "LB 15  QU SP* x OP");
                        continue;
                    }
                }

                if (fNS.contains(thisChar)) {
                    tPos = prevPos;
                    while (tPos > 0 && fSP.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    while (tPos > 0 && fCM.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    if (fCL.contains(UTF16.charAt(fText, tPos)) || fCP.contains(UTF16.charAt(fText, tPos))) {
                        setAppliedRule(pos, "LB 16  (CL | CP) SP* x NS");
                        continue;
                    }
                }


                if (fB2.contains(thisChar)) {
                    tPos = prevPos;
                    while (tPos > 0 && fSP.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    while (tPos > 0 && fCM.contains(UTF16.charAt(fText, tPos))) {
                        tPos = moveIndex32(fText, tPos, -1);
                    }
                    if (fB2.contains(UTF16.charAt(fText, tPos))) {
                        setAppliedRule(pos, "LB 17  B2 SP* x B2");
                        continue;
                    }
                }

                if (fSP.contains(prevChar)) {
                    setAppliedRule(pos, "LB 18  break after space");
                    break;
                }

                //    x   QU
                //    QU  x
                if (fQU.contains(thisChar) || fQU.contains(prevChar)) {
                        setAppliedRule(pos, "LB 19");
                    continue;
                }

                if (fCB.contains(thisChar) || fCB.contains(prevChar)) {
                    setAppliedRule(pos, "LB 20  Break around a CB");
                    break;
                }

                //           Don't break between Hyphens and letters if a break precedes the hyphen.
                //           Formerly this was a Finnish tailoring.
                //           Moved to root in ICU 63. This is an ICU customization, not in UAX-14.
                //    ^($HY | $HH) $AL;
                if (fAL.contains(thisChar) && (fHY.contains(prevChar) || fHH.contains(prevChar)) &&
                        prevPosX2 == -1) {
                    setAppliedRule(pos, "LB 20.09");
                    continue;
                }

                if (fBA.contains(thisChar) ||
                        fHY.contains(thisChar) ||
                        fNS.contains(thisChar) ||
                        fBB.contains(prevChar) )   {
                    setAppliedRule(pos, "LB 21");
                    continue;
                }

                if (fHL.contains(prevCharX2) && (fHY.contains(prevChar) || fBA.contains(prevChar))) {
                    setAppliedRule(pos, "LB 21a HL (HY | BA) x");
                    continue;
                }

                if (fSY.contains(prevChar) && fHL.contains(thisChar)) {
                    setAppliedRule(pos, "LB 21b SY x HL");
                    continue;
                }

                if (fIN.contains(thisChar)) {
                    setAppliedRule(pos, "LB 22");
                    continue;
                }

                //          (AL | HL) x NU
                //          NU x (AL | HL)
                if ((fAL.contains(prevChar) || fHL.contains(prevChar)) && fNU.contains(thisChar)) {
                    setAppliedRule(pos, "LB 23");
                    continue;
                }
                if (fNU.contains(prevChar) && (fAL.contains(thisChar) || fHL.contains(thisChar))) {
                    setAppliedRule(pos, "LB 23");
                    continue;
                }

                // Do not break between numeric prefixes and ideographs, or between ideographs and numeric postfixes.
                //      PR x (ID | EB | EM)
                //     (ID | EB | EM) x PO
                if (fPR.contains(prevChar) &&
                        (fID.contains(thisChar) || fEB.contains(thisChar) || fEM.contains(thisChar)))  {
                    setAppliedRule(pos, "LB 23a");
                    continue;
                }
                if ((fID.contains(prevChar) || fEB.contains(prevChar) || fEM.contains(prevChar)) &&
                        fPO.contains(thisChar)) {
                    setAppliedRule(pos, "LB 23a");
                    continue;
                }

                // Do not break between prefix and letters or ideographs.
                //         (PR | PO) x (AL | HL)
                //         (AL | HL) x (PR | PO)
                if ((fPR.contains(prevChar) || fPO.contains(prevChar)) &&
                        (fAL.contains(thisChar) || fHL.contains(thisChar))) {
                    setAppliedRule(pos, "LB 24  no break between prefix and letters or ideographs");
                    continue;
                }
                if ((fAL.contains(prevChar) || fHL.contains(prevChar)) &&
                        (fPR.contains(thisChar) || fPO.contains(thisChar))) {
                    setAppliedRule(pos, "LB 24  no break between prefix and letters or ideographs");
                    continue;
                }

                // appliedRule: "LB 25 numbers match"; // moved up, before LB 8a,

                if (fJL.contains(prevChar) && (fJL.contains(thisChar) ||
                        fJV.contains(thisChar) ||
                        fH2.contains(thisChar) ||
                        fH3.contains(thisChar))) {
                    setAppliedRule(pos, "LB 26  Do not break a Korean syllable.");
                    continue;
                }

                if ((fJV.contains(prevChar) || fH2.contains(prevChar))  &&
                        (fJV.contains(thisChar) || fJT.contains(thisChar))) {
                    setAppliedRule(pos, "LB 26  Do not break a Korean syllable.");
                    continue;
                }

                if ((fJT.contains(prevChar) || fH3.contains(prevChar)) &&
                        fJT.contains(thisChar)) {
                    setAppliedRule(pos, "LB 26  Do not break a Korean syllable.");
                    continue;
                }

                if ((fJL.contains(prevChar) || fJV.contains(prevChar) ||
                        fJT.contains(prevChar) || fH2.contains(prevChar) || fH3.contains(prevChar)) &&
                        fPO.contains(thisChar)) {
                    setAppliedRule(pos, "LB 27  Treat a Korean Syllable Block the same as ID.");
                    continue;
                }
                if (fPR.contains(prevChar) && (fJL.contains(thisChar) || fJV.contains(thisChar) ||
                        fJT.contains(thisChar) || fH2.contains(thisChar) || fH3.contains(thisChar))) {
                    setAppliedRule(pos, "LB 27  Treat a Korean Syllable Block the same as ID.");
                    continue;
                }



                if ((fAL.contains(prevChar) || fHL.contains(prevChar)) && (fAL.contains(thisChar) || fHL.contains(thisChar))) {
                    setAppliedRule(pos, "LB 28  Do not break between alphabetics");
                    continue;
                }

                if (fIS.contains(prevChar) && (fAL.contains(thisChar) || fHL.contains(thisChar))) {
                    setAppliedRule(pos, "LB 29  Do not break between numeric punctuation and alphabetics");
                    continue;
                }

                //          (AL | NU) x OP
                //          CP x (AL | NU)
                if ((fAL.contains(prevChar) || fHL.contains(prevChar) || fNU.contains(prevChar)) &&
                        fOP30.contains(thisChar)) {
                    setAppliedRule(pos, "LB 30  Do not break between letters, numbers, or ordinary symbols and opening or closing punctuation.");
                    continue;
                }
                if (fCP30.contains(prevChar) &&
                        (fAL.contains(thisChar) || fHL.contains(thisChar) || fNU.contains(thisChar))) {
                    setAppliedRule(pos, "LB 30  Do not break between letters, numbers, or ordinary symbols and opening or closing punctuation.");
                    continue;
                }

                //             RI RI  ÷  RI
                //                RI  x  RI
                if (fRI.contains(prevCharX2) && fRI.contains(prevChar) && fRI.contains(thisChar)) {
                    setAppliedRule(pos, "LB 30a Break between pairs of Regional Indicators.");
                    break;
                }
                if (fRI.contains(prevChar) && fRI.contains(thisChar)) {
                    // Two Regional Indicators have been paired.
                    // Over-write the trailing one (thisChar) to prevent it from forming another pair with a
                    // following RI. This is a hack.
                    thisChar = -1;
                    setAppliedRule(pos, "LB 30a Break between pairs of Regional Indicators.");
                    continue;
                }

                // LB30b Do not break between an emoji base (or potential emoji) and an emoji modifier.
                if (fEB.contains(prevChar) && fEM.contains(thisChar)) {
                    setAppliedRule(pos, "LB 30b Emoji Base x Emoji Modifier");
                    continue;
                }

                if (fExtPictUnassigned.contains(prevChar) && fEM.contains(thisChar)) {
                    setAppliedRule(pos, "LB30b    [\\p{Extended_Pictographic}&\\p{Cn}] × EM");
                    continue;
                }

                // LB 31    Break everywhere else
                setAppliedRule(pos, "LB 31 Break everywhere else");
                break;
            }

            return pos;
        }



        // Match the following regular expression in the input text.
        //    ((PR | PO) CM*)? ((OP | HY) CM*)? (IS CM*)? NU CM* ((NU | IS | SY) CM*) * ((CL | CP) CM*)?  (PR | PO) CM*)?
        //      0    0   1       4    4    4      5  5              7    7    7    7      9    9    9     11   11    (match states)
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
                    if (cLBType == UCharacter.LineBreak.INFIX_NUMERIC) {
                        matchState = 5;
                        break;
                    }
                    if (cLBType == UCharacter.LineBreak.NUMERIC) {
                        matchState = 7;
                        break;
                    }
                    break matchLoop;   /* No Match  */

                case 1:
                    if (cLBType == UCharacter.LineBreak.COMBINING_MARK || cLBType == UCharacter.LineBreak.ZWJ) {
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
                    if (cLBType == UCharacter.LineBreak.INFIX_NUMERIC) {
                        matchState = 5;
                        break;
                    }
                    if (cLBType == UCharacter.LineBreak.NUMERIC) {
                        matchState = 7;
                        break;
                    }
                    break matchLoop;   /* No Match  */

                case 4:
                    if (cLBType == UCharacter.LineBreak.COMBINING_MARK || cLBType == UCharacter.LineBreak.ZWJ) {
                        matchState = 4;
                        break;
                    }
                    if (cLBType == UCharacter.LineBreak.INFIX_NUMERIC) {
                        matchState = 5;
                        break;
                    }
                    if (cLBType == UCharacter.LineBreak.NUMERIC) {
                        matchState = 7;
                        break;
                    }
                    break matchLoop;   /* No Match  */

                case 5:
                    if (cLBType == UCharacter.LineBreak.COMBINING_MARK || cLBType == UCharacter.LineBreak.ZWJ) {
                        matchState = 5;
                        break;
                    }
                    if (cLBType == UCharacter.LineBreak.NUMERIC) {
                        matchState = 7;
                        break;
                    }
                    break matchLoop;   /* No Match  */


                case 7:
                    if (cLBType == UCharacter.LineBreak.COMBINING_MARK || cLBType == UCharacter.LineBreak.ZWJ) {
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
                    if (cLBType == UCharacter.LineBreak.CLOSE_PARENTHESIS) {
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
                case 9:
                    if (cLBType == UCharacter.LineBreak.COMBINING_MARK || cLBType == UCharacter.LineBreak.ZWJ) {
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
                    if (cLBType == UCharacter.LineBreak.COMBINING_MARK || cLBType == UCharacter.LineBreak.ZWJ) {
                        matchState = 11;
                        break;
                    }
                    break matchLoop;    // Match Complete.
                }
            }
            if (matchState >= 7) {
                retVals[0] = startIdx;
                retVals[1] = idx;
            }
            return retVals;
        }


        @Override
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

            fSets.add(fSepSet);         fClassNames.add("Sep");
            fSets.add(fFormatSet);      fClassNames.add("Format");

            fSets.add(fSpSet);          fClassNames.add("Sp");
            fSets.add(fLowerSet);       fClassNames.add("Lower");
            fSets.add(fUpperSet);       fClassNames.add("Upper");
            fSets.add(fOLetterSet);     fClassNames.add("OLetter");
            fSets.add(fNumericSet);     fClassNames.add("Numeric");
            fSets.add(fATermSet);       fClassNames.add("ATerm");
            fSets.add(fSContinueSet);   fClassNames.add("SContinue");
            fSets.add(fSTermSet);       fClassNames.add("STerm");
            fSets.add(fCloseSet);       fClassNames.add("Close");
            fSets.add(fOtherSet);       fClassNames.add("Other");
            fSets.add(fExtendSet);      fClassNames.add("Extend");
        }


        @Override
        List  charClasses() {
            return fSets;
        }

        @Override
        void   setText(StringBuffer s) {
            fText = s;
            prepareAppliedRules(s.length());
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

        @Override
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

                // Advance p3 by  X(Extend | Format)*   Rule 4
                p3 = moveForward(p3);
                c3 = cAt(p3);

                if (c1==0x0d && c2==0x0a && p2==(p1+1)) {
                    setAppliedRule(p2, "SB3   CR x LF");
                    continue;
                }

                if (fSepSet.contains(c1)) {
                    p2 = p1+1;   // Separators don't combine with Extend or Format
                    setAppliedRule(p2, "SB4   Sep  <break>");
                    break;
                }

                if (p2 >= fText.length()) {
                    // Reached end of string.  Always a break position.
                    setAppliedRule(p2, "SB4   Sep  <break>");
                    break;
                }

                if (p2 == prevPos) {
                    // Still warming up the loop.  (won't work with zero length strings, but we don't care)
                    setAppliedRule(p2, "SB4   Sep  <break>");
                    continue;
                }

                if (fATermSet.contains(c1) &&  fNumericSet.contains(c2))  {
                    setAppliedRule(p2, "SB6   ATerm x Numeric");
                    continue;
                }

                if ((fUpperSet.contains(c0) || fLowerSet.contains(c0)) &&
                        fATermSet.contains(c1) && fUpperSet.contains(c2)) {
                    setAppliedRule(p2, "SB7   (Upper | Lower) ATerm  x  Uppper");
                    continue;
                }

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
                            setAppliedRule(p2, "SB8   ATerm Close* Sp*  x  (not (OLettter | Upper | Lower | Sep))* Lower");
                            break;
                        }
                        p8 = moveForward(p8);
                    }
                    if (p8<fText.length() && fLowerSet.contains(cAt(p8))) {
                        setAppliedRule(p2, "SB8   ATerm Close* Sp*  x  (not (OLettter | Upper | Lower | Sep))* Lower");
                        continue;
                    }
                }

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
                        setAppliedRule(p2, "SB8a  (STerm | ATerm) Close* Sp* x (SContinue | Sterm | ATerm)");
                        continue;
                    }
                }


                int p9 = p1;
                while (p9>0 && fCloseSet.contains(cAt(p9))) {
                    p9 = moveBack(p9);
                }
                c = cAt(p9);
                if ((fSTermSet.contains(c) || fATermSet.contains(c))) {
                    if (fCloseSet.contains(c2) || fSpSet.contains(c2) || fSepSet.contains(c2)) {
                        setAppliedRule(p2, "SB9   (STerm | ATerm) Close*  x  (Close | Sp | Sep | CR | LF)");
                        continue;
                    }
                }

                int p10 = p1;
                while (p10>0 && fSpSet.contains(cAt(p10))) {
                    p10 = moveBack(p10);
                }
                while (p10>0 && fCloseSet.contains(cAt(p10))) {
                    p10 = moveBack(p10);
                }
                if (fSTermSet.contains(cAt(p10)) || fATermSet.contains(cAt(p10))) {
                    if (fSpSet.contains(c2) || fSepSet.contains(c2)) {
                        setAppliedRule(p2, "SB10  (Sterm | ATerm) Close* Sp*  x  (Sp | Sep | CR | LF)");
                        continue;
                    }
                }

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
                    setAppliedRule(p2, "SB11  (STerm | ATerm) Close* Sp*   <break>");
                    break;
                }

                setAppliedRule(p2, "SB12  Any x Any");
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
        return (m_seed >>> 16) % 32768;
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
    @SuppressWarnings("unused")
    private static void appendCharToBuf(StringBuffer dest, int c, int fieldLen) {
        String hexChars = "0123456789abcdef";
        if (c < 0x10000) {
            dest.append("\\u");
            for (int bn=12; bn>=0; bn-=4) {
                dest.append(hexChars.charAt(((c)>>bn)&0xf));
            }
            appendToBuf(dest, " ", fieldLen-6);
        } else {
            dest.append("\\U");
            for (int bn=28; bn>=0; bn-=4) {
                dest.append(hexChars.charAt(((c)>>bn)&0xf));
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
        @SuppressWarnings("unused")
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
        // numIterations = 10000;   // Same as exhaustive.
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

        // For minimizing width of class name output.
        int classNameSize = mk.maxClassNameSize();

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
                // Do not assemble a supplementary character from randomly generated separate surrogates.
                //   (It could be a dictionary character)
                if (c < 0x10000 && Character.isLowSurrogate((char)c) && testText.length() > 0 &&
                        Character.isHighSurrogate(testText.charAt(testText.length()-1))) {
                    continue;
                }
                testText.appendCodePoint(c);
                if (printTestData) {
                    System.out.print(Integer.toHexString(c) + " ");
                }
            }
            if (printTestData) {
                System.out.println();
            }

            Arrays.fill(expectedBreaks, false);
            Arrays.fill(forwardBreaks, false);
            Arrays.fill(reverseBreaks, false);
            Arrays.fill(isBoundaryBreaks, false);
            Arrays.fill(followingBreaks, false);
            Arrays.fill(precedingBreaks, false);

            // Calculate the expected results for this test string and reset applied rules.
            mk.setText(testText);
            expectedCount = 0;
            expectedBreaks[0] = true;
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
                boolean[] currentBreakData = null;
                if  (forwardBreaks[i] != expectedBreaks[i]) {
                    errorType = "next()";
                    currentBreakData = forwardBreaks;
                } else if (reverseBreaks[i] != forwardBreaks[i]) {
                    errorType = "previous()";
                    currentBreakData = reverseBreaks;
                } else if (isBoundaryBreaks[i] != expectedBreaks[i]) {
                    errorType = "isBoundary()";
                    currentBreakData = isBoundaryBreaks;
                } else if (followingBreaks[i] != expectedBreaks[i]) {
                    errorType = "following()";
                    currentBreakData = followingBreaks;
                } else if (precedingBreaks[i] != expectedBreaks[i]) {
                    errorType = "preceding()";
                    currentBreakData = precedingBreaks;
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

                    // Formatting of each line includes:
                    //   character code
                    //   reference break: '|' -> a break, '.' -> no break
                    //   actual break:    '|' -> a break, '.' -> no break
                    //   (name of character clase)
                    //   Unicode name of character
                    //   '--→' indicates location of the difference.

                    StringBuilder buffer = new StringBuilder();
                    buffer.append("\n")
                        .append((expectedBreaks[i] ? "Break expected but not found." : "Break found but not expected."))
                        .append(
                            String.format(" at index %d. Parameters to reproduce: @\"type=%s  seed=%d  loop=1\"\n",
                              i, name, seed));

                    int c;  // Char from test data
                    for (ci = startContext;  ci <= endContext && ci != -1;  ci = nextCP(testText, ci)) {

                        c = testText.codePointAt(ci);
                        buffer.append((ci == i) ? " --→" : "    ")
                            .append(String.format(" %3d : ", ci))
                            .append(!expectedBreaks[ci] ? " . " : " | ")  // Reference break
                            .append(!currentBreakData[ci] ? " . " : " | "); // Actual break

                        // BMP or SMP character in hex
                        if (c >= 0x10000) {
                            buffer.append("\\U").append(String.format("%08x", c));
                        } else {
                            buffer.append("    \\u").append(String.format("%04x", c));
                        }

                        buffer.append(
                            String.format(String.format(" %%-%ds", classNameSize),
                              mk.classNameFromCodepoint(c)))
                            .append(String.format(" %-40s", mk.getAppliedRule(ci)))
                            .append(String.format(" %-40s\n", UCharacter.getExtendedName(c)));

                        if (ci >= endContext) { break; }
                    }
                    errln(buffer.toString());

                    break;
                }
            }

            loopCount++;
        }
    }

    // Test parameters are passed on the command line, or
    // via the Eclipse Run Configuration settings, arguments tab, VM parameters.
    // For example,
    //      -ea -Dseed=554654 -Dloop=1

    @Test
    public void TestCharMonkey() {
        int loopCount = getIntProperty("loop", isQuick() ? 500 : 10000);
        int seed = getIntProperty("seed", 1);

        RBBICharMonkey  m = new RBBICharMonkey();
        BreakIterator   bi = BreakIterator.getCharacterInstance(Locale.US);
        RunMonkey(bi, m, "char", seed, loopCount);
    }

    @Test
    public void TestWordMonkey() {
        int loopCount = getIntProperty("loop", isQuick() ? 500 : 10000);
        int seed = getIntProperty("seed", 1);

        logln("Word Break Monkey Test");
        RBBIWordMonkey  m = new RBBIWordMonkey();
        BreakIterator   bi = BreakIterator.getWordInstance(Locale.US);
        RunMonkey(bi, m, "word", seed, loopCount);
    }

    @Test
    public void TestLineMonkey() {
        int loopCount = getIntProperty("loop", isQuick() ? 500 : 10000);
        int seed = getIntProperty("seed", 1);

        logln("Line Break Monkey Test");
        RBBILineMonkey  m = new RBBILineMonkey();
        BreakIterator   bi = BreakIterator.getLineInstance(Locale.US);
        try {
            RunMonkey(bi, m, "line", seed, loopCount);
        } catch(IllegalArgumentException e) {
            if (e.getMessage().equals("Invalid code point U+-000001")) {
                // Looks like you used class UnicodeSet instead of class XUnicodeSet
                // (note the leading 'X').
                // See the comment before the definition of class XUnicodeSet.
                errln("Probable program error: use XUnicodeSet in RBBILineMonkey code");
            } else {
                throw e;
            }
        }
    }

    @Test
    public void TestSentMonkey() {
        int loopCount = getIntProperty("loop", isQuick() ? 500 : 3000);
        int seed = getIntProperty("seed", 1);

        logln("Sentence Break Monkey Test");
        RBBISentenceMonkey  m = new RBBISentenceMonkey();
        BreakIterator   bi = BreakIterator.getSentenceInstance(Locale.US);
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
    @Test
    public void TestRTCharMonkey() {
        int loopCount = getIntProperty("loop", isQuick() ? 200 : 2000);
        int seed = getIntProperty("seed", 1);

        RBBICharMonkey  m = new RBBICharMonkey();
        BreakIterator   bi = BreakIterator.getCharacterInstance(Locale.US);
        String rules = bi.toString();
        BreakIterator rtbi = new RuleBasedBreakIterator(rules);
        RunMonkey(rtbi, m, "char", seed, loopCount);
    }

    @Test
    public void TestRTWordMonkey() {
        int loopCount = getIntProperty("loop", isQuick() ? 200 : 2000);
        int seed = getIntProperty("seed", 1);

        logln("Word Break Monkey Test");
        RBBIWordMonkey  m = new RBBIWordMonkey();
        BreakIterator   bi = BreakIterator.getWordInstance(Locale.US);
        String rules = bi.toString();
        BreakIterator rtbi = new RuleBasedBreakIterator(rules);
        RunMonkey(rtbi, m, "word", seed, loopCount);
    }

    @Test
    public void TestRTLineMonkey() {
        int loopCount = getIntProperty("loop", isQuick() ? 200 : 2000);
        int seed = getIntProperty("seed", 1);

        logln("Line Break Monkey Test");
        RBBILineMonkey  m = new RBBILineMonkey();
        BreakIterator   bi = BreakIterator.getLineInstance(Locale.US);
        String rules = bi.toString();
        BreakIterator rtbi = new RuleBasedBreakIterator(rules);
        try {
            RunMonkey(rtbi, m, "line", seed, loopCount);
        } catch(IllegalArgumentException e) {
            if (e.getMessage().equals("Invalid code point U+-000001")) {
                // Looks like you used class UnicodeSet instead of class XUnicodeSet
                // (note the leading 'X').
                // See the comment before the definition of class XUnicodeSet.
                errln("Probable program error: use XUnicodeSet in RBBILineMonkey code");
            } else {
                throw e;
            }
        }
    }

    @Test
    public void TestRTSentMonkey() {
        int loopCount = getIntProperty("loop", isQuick() ? 200 : 1000);
        int seed = getIntProperty("seed", 1);

        logln("Sentence Break Monkey Test");
        RBBISentenceMonkey  m = new RBBISentenceMonkey();
        BreakIterator   bi = BreakIterator.getSentenceInstance(Locale.US);
        String rules = bi.toString();
        BreakIterator rtbi = new RuleBasedBreakIterator(rules);
        RunMonkey(rtbi, m, "sent", seed, loopCount);
    }
}

