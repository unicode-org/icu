// © 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.impl.breakiter;

import java.io.IOException;
import java.text.CharacterIterator;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.UnicodeSet;

public class ThaiBreakEngine extends DictionaryBreakEngine {

    // Constants for ThaiBreakIterator
    // How many words in a row are "good enough"?
    private static final byte THAI_LOOKAHEAD = 3;
    // Will not combine a non-word with a preceding dictionary word longer than this
    private static final byte THAI_ROOT_COMBINE_THRESHOLD = 3;
    // Will not combine a non-word that shares at least this much prefix with a
    // dictionary word with a preceding word
    private static final byte THAI_PREFIX_COMBINE_THRESHOLD = 3;
    // Ellision character
    private static final char THAI_PAIYANNOI = 0x0E2F;
    // Repeat character
    private static final char THAI_MAIYAMOK = 0x0E46;
    // Minimum word size
    private static final byte THAI_MIN_WORD = 2;
    // Minimum number of characters for two words
    private static final byte THAI_MIN_WORD_SPAN = THAI_MIN_WORD * 2;

    private DictionaryMatcher fDictionary;
    private UnicodeSet fEndWordSet;
    private UnicodeSet fBeginWordSet;
    private UnicodeSet fSuffixSet;
    private UnicodeSet fMarkSet;

    public ThaiBreakEngine() throws IOException {
        // Initialize UnicodeSets
        UnicodeSet thaiWordSet = new UnicodeSet("[[:Thai:]&[:LineBreak=SA:]]");
        fMarkSet = new UnicodeSet("[[:Thai:]&[:LineBreak=SA:]&[:M:]]");
        fMarkSet.add(0x0020);
        fBeginWordSet = new UnicodeSet(0x0E01, 0x0E2E,  //KO KAI through HO NOKHUK
                                       0x0E40, 0x0E44); // SARA E through SARA AI MAIMALAI
        fSuffixSet = new UnicodeSet();
        fSuffixSet.add(THAI_PAIYANNOI);
        fSuffixSet.add(THAI_MAIYAMOK);

        thaiWordSet.compact();

        fEndWordSet = new UnicodeSet(thaiWordSet);
        fEndWordSet.remove(0x0E31); // MAI HAN-AKAT
        fEndWordSet.remove(0x0E40, 0x0E44); // SARA E through SARA AI MAIMALAI

        // Compact for caching
        fMarkSet.compact();
        fEndWordSet.compact();
        fBeginWordSet.compact();
        fSuffixSet.compact();

        // Freeze the static UnicodeSet
        thaiWordSet.freeze();
        fMarkSet.freeze();
        fEndWordSet.freeze();
        fBeginWordSet.freeze();
        fSuffixSet.freeze();

        setCharacters(thaiWordSet);
        // Initialize dictionary
        fDictionary = DictionaryData.loadDictionaryFor("Thai");
    }

    @Override
    public boolean equals(Object obj) {
        // Normally is a singleton, but it's possible to have duplicates
        //   during initialization. All are equivalent.
        return obj instanceof ThaiBreakEngine;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean handles(int c) {
        int script = UCharacter.getIntPropertyValue(c, UProperty.SCRIPT);
        return (script == UScript.THAI);
    }

    @Override
    public int divideUpDictionaryRange(CharacterIterator fIter, int rangeStart, int rangeEnd,
            DequeI foundBreaks, boolean isPhraseBreaking) {

        if ((rangeEnd - rangeStart) < THAI_MIN_WORD_SPAN) {
            return 0;  // Not enough characters for word
        }
        int wordsFound = 0;
        int wordLength;
        PossibleWord words[] = new PossibleWord[THAI_LOOKAHEAD];
        for (int i = 0; i < THAI_LOOKAHEAD; i++) {
            words[i] = new PossibleWord();
        }

        int uc;
        fIter.setIndex(rangeStart);
        int current;
        while ((current = fIter.getIndex()) < rangeEnd) {
            wordLength = 0;

            //Look for candidate words at the current position
            int candidates = words[wordsFound%THAI_LOOKAHEAD].candidates(fIter, fDictionary, rangeEnd);

            // If we found exactly one, use that
            if (candidates == 1) {
                wordLength = words[wordsFound%THAI_LOOKAHEAD].acceptMarked(fIter);
                wordsFound += 1;
            }

            // If there was more than one, see which one can take us forward the most words
            else if (candidates > 1) {
                // If we're already at the end of the range, we're done
                if (fIter.getIndex() < rangeEnd) {
                  foundBest:
                    do {
                        if (words[(wordsFound+1)%THAI_LOOKAHEAD].candidates(fIter, fDictionary, rangeEnd) > 0) {
                            // Followed by another dictionary word; mark first word as a good candidate
                            words[wordsFound%THAI_LOOKAHEAD].markCurrent();

                            // If we're already at the end of the range, we're done
                            if (fIter.getIndex() >= rangeEnd) {
                                break foundBest;
                            }

                            // See if any of the possible second words is followed by a third word
                            do {
                                // If we find a third word, stop right away
                                if (words[(wordsFound+2)%THAI_LOOKAHEAD].candidates(fIter, fDictionary, rangeEnd) > 0) {
                                    words[wordsFound%THAI_LOOKAHEAD].markCurrent();
                                    break foundBest;
                                }
                            } while (words[(wordsFound+1)%THAI_LOOKAHEAD].backUp(fIter));
                        }
                    }
                    while (words[wordsFound%THAI_LOOKAHEAD].backUp(fIter));
                    // foundBest: end of loop
                }
                wordLength = words[wordsFound%THAI_LOOKAHEAD].acceptMarked(fIter);
                wordsFound += 1;
            }

            // We come here after having either found a word or not. We look ahead to the
            // next word. If it's not a dictionary word, we will combine it with the word we
            // just found (if there is one), but only if the preceding word does not exceed
            // the threshold.
            // The text iterator should now be positioned at the end of the word we found.
            if (fIter.getIndex() < rangeEnd && wordLength < THAI_ROOT_COMBINE_THRESHOLD) {
                // If it is a dictionary word, do nothing. If it isn't, then if there is
                // no preceding word, or the non-word shares less than the minimum threshold
                // of characters with a dictionary word, then scan to resynchronize
                if (words[wordsFound%THAI_LOOKAHEAD].candidates(fIter, fDictionary, rangeEnd) <= 0 &&
                        (wordLength == 0 ||
                                words[wordsFound%THAI_LOOKAHEAD].longestPrefix() < THAI_PREFIX_COMBINE_THRESHOLD)) {
                    // Look for a plausible word boundary
                    int remaining = rangeEnd - (current + wordLength);
                    int pc = fIter.current();
                    int chars = 0;
                    for (;;) {
                        fIter.next();
                        uc = fIter.current();
                        chars += 1;
                        if (--remaining <= 0) {
                            break;
                        }
                        if (fEndWordSet.contains(pc) && fBeginWordSet.contains(uc)) {
                            // Maybe. See if it's in the dictionary.
                            // Note: In the original Apple code, checked that the next
                            // two characters after uc were not 0x0E4C THANTHAKHAT before
                            // checking the dictionary. That is just a performance filter,
                            // but it's not clear it's faster than checking the trie
                            int candidate = words[(wordsFound + 1) %THAI_LOOKAHEAD].candidates(fIter, fDictionary, rangeEnd);
                            fIter.setIndex(current + wordLength + chars);
                            if (candidate > 0) {
                                break;
                            }
                        }
                        pc = uc;
                    }

                    // Bump the word count if there wasn't already one
                    if (wordLength <= 0) {
                        wordsFound += 1;
                    }

                    // Update the length with the passed-over characters
                    wordLength += chars;
                } else {
                    // Backup to where we were for next iteration
                    fIter.setIndex(current+wordLength);
                }
            }

            // Never stop before a combining mark.
            int currPos;
            while ((currPos = fIter.getIndex()) < rangeEnd && fMarkSet.contains(fIter.current())) {
                fIter.next();
                wordLength += fIter.getIndex() - currPos;
            }

            // Look ahead for possible suffixes if a dictionary word does not follow.
            // We do this in code rather than using a rule so that the heuristic
            // resynch continues to function. For example, one of the suffix characters
            // could be a typo in the middle of a word.
            if (fIter.getIndex() < rangeEnd && wordLength > 0) {
                if (words[wordsFound%THAI_LOOKAHEAD].candidates(fIter, fDictionary, rangeEnd) <= 0 &&
                        fSuffixSet.contains(uc = fIter.current())) {
                    if (uc == THAI_PAIYANNOI) {
                        if (!fSuffixSet.contains(fIter.previous())) {
                            // Skip over previous end and PAIYANNOI
                            fIter.next();
                            fIter.next();
                            wordLength += 1;
                            uc = fIter.current();
                        } else {
                            // Restore prior position
                            fIter.next();
                        }
                    }
                    if (uc == THAI_MAIYAMOK) {
                        if (fIter.previous() != THAI_MAIYAMOK) {
                            // Skip over previous end and MAIYAMOK
                            fIter.next();
                            fIter.next();
                            wordLength += 1;
                        } else {
                            // restore prior position
                            fIter.next();
                        }
                    }
                } else {
                    fIter.setIndex(current + wordLength);
                }
            }

            // Did we find a word on this iteration? If so, push it on the break stack
            if (wordLength > 0) {
                foundBreaks.push(Integer.valueOf(current + wordLength));
            }
        }

        // Don't return a break for the end of the dictionary range if there is one there
        if (foundBreaks.peek() >= rangeEnd) {
            foundBreaks.pop();
            wordsFound -= 1;
        }

        return wordsFound;
    }

}
