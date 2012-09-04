/*
 *******************************************************************************
 * Copyright (C) 2012, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.text.CharacterIterator;
import java.util.Stack;

import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.lang.UScript;

class ThaiBreakEngine implements LanguageBreakEngine {
    /* Helper class for improving readability of the Thai word break
     * algorithm.
     */
    static class PossibleWord {
        // List size, limited by the maximum number of words in the dictionary
        // that form a nested sequence.
        private final static int POSSIBLE_WORD_LIST_MAX = 20;
        //list of word candidate lengths, in increasing length order
        private int lengths[];
        private int count[];    // Count of candidates
        private int prefix;     // The longest match with a dictionary word
        private int offset;     // Offset in the text of these candidates
        private int mark;       // The preferred candidate's offset
        private int current;    // The candidate we're currently looking at

        // Default constructor
        public PossibleWord() {
            lengths = new int[POSSIBLE_WORD_LIST_MAX];
            count = new int[1]; // count needs to be an array of 1 so that it can be pass as reference
            offset = -1;
        }

        // Fill the list of candidates if needed, select the longest, and return the number found
        public int candidates(CharacterIterator fIter, DictionaryMatcher dict, int rangeEnd) {
            int start = fIter.getIndex();
            if (start != offset) {
                offset = start;
                prefix = dict.matches(fIter, rangeEnd - start, lengths, count, lengths.length);
                // Dictionary leaves text after longest prefix, not longest word. Back up.
                if (count[0] <= 0) {
                    fIter.setIndex(start);
                }
            }
            if (count[0] > 0) {
                fIter.setIndex(start + lengths[count[0]-1]);
            }
            current = count[0] - 1;
            mark = current;
            return count[0];
        }

        // Select the currently marked candidate, point after it in the text, and invalidate self
        public int acceptMarked(CharacterIterator fIter) {
            fIter.setIndex(offset + lengths[mark]);
            return lengths[mark];
        }

        // Backup from the current candidate to the next shorter one; return true if that exists
        // and point the text after it
        public boolean backUp(CharacterIterator fIter) {
            if (current > 0) {
                fIter.setIndex(offset + lengths[--current]);
                return true;
            }
            return false;
        }

        // Return the longest prefix this candidate location shares with a dictionary word
        public int longestPrefix() {
            return prefix;
        }

        // Mark the current candidate as the one we like
        public void markCurrent() {
            mark = current;
        }
    }
    
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
    
    private DictionaryMatcher fDictionary;
    private static UnicodeSet fThaiWordSet;
    private static UnicodeSet fEndWordSet;
    private static UnicodeSet fBeginWordSet;
    private static UnicodeSet fSuffixSet;
    private static UnicodeSet fMarkSet;
    
    static {
        // Initialize UnicodeSets
        fThaiWordSet = new UnicodeSet();
        fMarkSet = new UnicodeSet();
        fEndWordSet = new UnicodeSet();
        fBeginWordSet = new UnicodeSet();
        fSuffixSet = new UnicodeSet();

        fThaiWordSet.applyPattern("[[:Thai:]&[:LineBreak=SA:]]");
        fThaiWordSet.compact();

        fMarkSet.applyPattern("[[:Thai:]&[:LineBreak=SA:]&[:M:]]");
        fMarkSet.add(0x0020);
        fEndWordSet = fThaiWordSet;
        fEndWordSet.remove(0x0E31); // MAI HAN-AKAT
        fEndWordSet.remove(0x0E40, 0x0E44); // SARA E through SARA AI MAIMALAI
        fBeginWordSet.add(0x0E01, 0x0E2E); //KO KAI through HO NOKHUK
        fBeginWordSet.add(0x0E40, 0x0E44); // SARA E through SARA AI MAIMALAI
        fSuffixSet.add(THAI_PAIYANNOI);
        fSuffixSet.add(THAI_MAIYAMOK);

        // Compact for caching
        fMarkSet.compact();
        fEndWordSet.compact();
        fBeginWordSet.compact();
        fSuffixSet.compact();
        
        // Freeze the static UnicodeSet
        fThaiWordSet.freeze();
        fMarkSet.freeze();
        fEndWordSet.freeze();
        fBeginWordSet.freeze();
        fSuffixSet.freeze();
    }
    
    public ThaiBreakEngine() throws IOException {
        // Initialize dictionary
        fDictionary = DictionaryData.loadDictionaryFor("Thai");
    }

    public boolean handles(int c, int breakType) {
        if (breakType == BreakIterator.KIND_WORD || breakType == BreakIterator.KIND_LINE) {
            int script = UCharacter.getIntPropertyValue(c, UProperty.SCRIPT);
            return (script == UScript.THAI);
        }
        return false;
    }

    public int findBreaks(CharacterIterator fIter, int rangeStart, int rangeEnd, boolean reverse, int breakType,
            Stack<Integer> foundBreaks) {
        if ((rangeEnd - rangeStart) < THAI_MIN_WORD) {
            return 0;  // Not enough characters for word
        }
        int wordsFound = 0;
        int wordLength;
        int current;
        PossibleWord words[] = new PossibleWord[THAI_LOOKAHEAD];
        for (int i = 0; i < THAI_LOOKAHEAD; i++) {
            words[i] = new PossibleWord();
        }
        int uc;

        fIter.setIndex(rangeStart);

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
                boolean foundBest = false;
                // If we're already at the end of the range, we're done
                if (fIter.getIndex() < rangeEnd) {
                    do {
                        int wordsMatched = 1;
                        if (words[(wordsFound+1)%THAI_LOOKAHEAD].candidates(fIter, fDictionary, rangeEnd) > 0) {
                            if (wordsMatched < 2) {
                                // Followed by another dictionary word; mark first word as a good candidate
                                words[wordsFound%THAI_LOOKAHEAD].markCurrent();
                                wordsMatched = 2;
                            }

                            // If we're already at the end of the range, we're done
                            if (fIter.getIndex() >= rangeEnd) {
                                break;
                            }

                            // See if any of the possible second words is followed by a third word
                            do {
                                // If we find a third word, stop right away
                                if (words[(wordsFound+2)%THAI_LOOKAHEAD].candidates(fIter, fDictionary, rangeEnd) > 0) {
                                    words[wordsFound%THAI_LOOKAHEAD].markCurrent();
                                    foundBest = true;
                                    break;
                                }
                            } while (words[(wordsFound+1)%THAI_LOOKAHEAD].backUp(fIter));
                        }
                    } while (words[wordsFound%THAI_LOOKAHEAD].backUp(fIter) && !foundBest);
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
        if (foundBreaks.peek().intValue() >= rangeEnd) {
            foundBreaks.pop();
            wordsFound -= 1;
        }

        return wordsFound;
    }

}
