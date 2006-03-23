/**
 *******************************************************************************
 * Copyright (C) 2006, International Business Machines Corporation and others. *
 * All Rights Reserved.                                                        *
 *******************************************************************************
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "brkeng.h"
#include "dictbe.h"
#include "unicode/uniset.h"
#include "unicode/chariter.h"
#include "unicode/ubrk.h"
#include "uvector.h"
#include "triedict.h"

U_NAMESPACE_BEGIN

/*
 ******************************************************************
 */

DictionaryBreakEngine::DictionaryBreakEngine() {
    fTypes = 0;
}

DictionaryBreakEngine::DictionaryBreakEngine(uint32_t breakTypes) {
    fTypes = breakTypes;
}

DictionaryBreakEngine::~DictionaryBreakEngine() {
}

UBool
DictionaryBreakEngine::handles(UChar32 c, int32_t breakType) const {
    return (breakType >= 0 && breakType < 32 && (((uint32_t)1 << breakType) & fTypes)
            && fSet.contains(c));
}

int32_t
DictionaryBreakEngine::findBreaks( CharacterIterator *text,
                                 int32_t startPos,
                                 int32_t endPos,
                                 UBool reverse,
                                 int32_t breakType,
                                 UStack &foundBreaks ) const {
    int32_t result = 0;

    // Find the span of characters included in the set.
    int32_t start = text->getIndex();
    int32_t current;
    int32_t rangeStart;
    int32_t rangeEnd;
    UChar32 c = text->current32();
    if (reverse) {
        UBool   isDict = fSet.contains(c);
        while((current = text->getIndex()) > startPos && isDict) {
            c = text->previous32();
            isDict = fSet.contains(c);
        }
        rangeStart = (current < startPos) ? startPos : current+(isDict ? 0 : 1);
        rangeEnd = start + 1;
    }
    else {
        while((current = text->getIndex()) < endPos && fSet.contains(c)) {
            c = text->next32();
        }
        rangeStart = start;
        rangeEnd = current;
    }
    if (breakType >= 0 && breakType < 32 && (((uint32_t)1 << breakType) & fTypes)) {
        result = divideUpDictionaryRange(text, rangeStart, rangeEnd, foundBreaks);
        text->setIndex(current);
    }
    
    return result;
}

void
DictionaryBreakEngine::setCharacters( UnicodeSet &set ) {
    fSet = set;
}

void
DictionaryBreakEngine::setBreakTypes( uint32_t breakTypes ) {
    fTypes = breakTypes;
}

/*
 ******************************************************************
 */


// Helper class for improving readability of the Thai word break
// algorithm. The implementation is completely inline.

// List size, limited by the maximum number of words in the dictionary
// that form a nested sequence.
#define POSSIBLE_WORD_LIST_MAX 20

class PossibleWord {
 private:
  // list of word candidate lengths, in increasing length order
  int32_t   lengths[POSSIBLE_WORD_LIST_MAX];
  int       count;      // Count of candidates
  int32_t   prefix;     // The longest match with a dictionary word
  int32_t   offset;     // Offset in the text of these candidates
  int       mark;       // The preferred candidate's offset
  int       current;    // The candidate we're currently looking at

 public:
  PossibleWord();
  ~PossibleWord();
  
  // Fill the list of candidates if needed, select the longest, and return the number found
  int       candidates( CharacterIterator *text, const TrieWordDictionary *dict, int32_t rangeEnd );
  
  // Select the currently marked candidate, point after it in the text, and invalidate self
  int32_t   acceptMarked( CharacterIterator *text );
  
  // Back up from the current candidate to the next shorter one; return TRUE if that exists
  // and point the text after it
  UBool     backUp( CharacterIterator *text );
  
  // Return the longest prefix this candidate location shares with a dictionary word
  int32_t   longestPrefix();
  
  // Mark the current candidate as the one we like
  void      markCurrent();
};

inline
PossibleWord::PossibleWord() {
    offset = -1;
}

inline
PossibleWord::~PossibleWord() {
}

inline int
PossibleWord::candidates( CharacterIterator *text, const TrieWordDictionary *dict, int32_t rangeEnd ) {
    // TODO: If getIndex is too slow, use offset < 0 and add discardAll()
    int32_t start = text->getIndex();
    if (start != offset) {
        offset = start;
        prefix = dict->matches(text, rangeEnd-start, lengths, count, sizeof(lengths)/sizeof(lengths[0]));
        // Dictionary leaves text after longest prefix, not longest word. Back up.
        if (count <= 0) {
            text->setIndex(start);
        }
    }
    if (count > 0) {
        text->setIndex(start+lengths[count-1]);
    }
    current = count-1;
    mark = current;
    return count;
}

inline int32_t
PossibleWord::acceptMarked( CharacterIterator *text ) {
    text->setIndex(offset + lengths[mark]);
    return lengths[mark];
}

inline UBool
PossibleWord::backUp( CharacterIterator *text ) {
    if (current > 0) {
        text->setIndex(offset + lengths[--current]);
        return TRUE;
    }
    return FALSE;
}

inline int32_t
PossibleWord::longestPrefix() {
    return prefix;
}

inline void
PossibleWord::markCurrent() {
    mark = current;
}

// How many words in a row are "good enough"?
#define THAI_LOOKAHEAD 3

// Will not combine a non-word with a preceding dictionary word longer than this
#define THAI_ROOT_COMBINE_THRESHOLD 3

// Will not combine a non-word that shares at least this much prefix with a
// dictionary word, with a preceding word
#define THAI_PREFIX_COMBINE_THRESHOLD 3

// Ellision character
#define THAI_PAIYANNOI 0x0E2F

// Repeat character
#define THAI_MAIYAMOK 0x0E46

// Minimum word size
#define THAI_MIN_WORD 2

// Minimum number of characters for two words
#define THAI_MIN_WORD_SPAN (THAI_MIN_WORD * 2)

ThaiBreakEngine::ThaiBreakEngine(const TrieWordDictionary *adoptDictionary, UErrorCode &status)
    : DictionaryBreakEngine((1<<UBRK_WORD) | (1<<UBRK_LINE)),
      fDictionary(adoptDictionary) {
    UnicodeString thaiSet("[[:Thai:]&[:LineBreak=SA:]]", -1, US_INV);
    UnicodeString markSet("[[:Thai:]&[:LineBreak=SA:]&[:M:]]", -1, US_INV);
    fThaiWordSet.applyPattern(thaiSet, status);
    if (U_SUCCESS(status)) {
        setCharacters(fThaiWordSet);
    }
    fMarkSet.applyPattern(markSet, status);
    fEndWordSet = fThaiWordSet;
    fEndWordSet.remove(0x0E31);             // MAI HAN-AKAT
    fEndWordSet.remove(0x0E40, 0x0E44);     // SARA E through SARA AI MAIMALAI
    fBeginWordSet.add(0x0E01, 0x0E2E);      // KO KAI through HO NOKHUK
    fBeginWordSet.add(0x0E40, 0x0E44);      // SARA E through SARA AI MAIMALAI
    fSuffixSet.add(THAI_PAIYANNOI);
    fSuffixSet.add(THAI_MAIYAMOK);
}

ThaiBreakEngine::~ThaiBreakEngine() {
    delete fDictionary;
}

int32_t
ThaiBreakEngine::divideUpDictionaryRange( CharacterIterator *text,
                                                int32_t rangeStart,
                                                int32_t rangeEnd,
                                                UStack &foundBreaks ) const {
    if ((rangeEnd - rangeStart) < THAI_MIN_WORD_SPAN) {
        return 0;       // Not enough characters for two words
    }

    uint32_t wordsFound = 0;
    int32_t wordLength;
    int32_t current;
    UErrorCode status = U_ZERO_ERROR;
    PossibleWord words[THAI_LOOKAHEAD];
    UChar32 uc;
    
    text->setIndex(rangeStart);
    
    while (U_SUCCESS(status) && (current = text->getIndex()) < rangeEnd) {
        wordLength = 0;

        // Look for candidate words at the current position
        int candidates = words[wordsFound%THAI_LOOKAHEAD].candidates(text, fDictionary, rangeEnd);
        
        // If we found exactly one, use that
        if (candidates == 1) {
            wordLength = words[wordsFound%THAI_LOOKAHEAD].acceptMarked(text);
            wordsFound += 1;
        }
        
        // If there was more than one, see which one can take us forward the most words
        else if (candidates > 1) {
            // If we're already at the end of the range, we're done
            if (text->getIndex() >= rangeEnd) {
                goto foundBest;
            }
            do {
                int wordsMatched = 1;
                if (words[(wordsFound+1)%THAI_LOOKAHEAD].candidates(text, fDictionary, rangeEnd) > 0) {
                    if (wordsMatched < 2) {
                        // Followed by another dictionary word; mark first word as a good candidate
                        words[wordsFound%THAI_LOOKAHEAD].markCurrent();
                        wordsMatched = 2;
                    }
                    
                    // If we're already at the end of the range, we're done
                    if (text->getIndex() >= rangeEnd) {
                        goto foundBest;
                    }
                    
                    // See if any of the possible second words is followed by a third word
                    do {
                        // If we find a third word, stop right away
                        if (words[(wordsFound+2)%THAI_LOOKAHEAD].candidates(text, fDictionary, rangeEnd)) {
                            words[wordsFound%THAI_LOOKAHEAD].markCurrent();
                            goto foundBest;
                        }
                    }
                    while (words[(wordsFound+1)%THAI_LOOKAHEAD].backUp(text));
                }
            }
            while (words[wordsFound%THAI_LOOKAHEAD].backUp(text));
foundBest:
            wordLength = words[wordsFound%THAI_LOOKAHEAD].acceptMarked(text);
            wordsFound += 1;
        }
        
        // We come here after having either found a word or not. We look ahead to the
        // next word. If it's not a dictionary word, we will combine it withe the word we
        // just found (if there is one), but only if the preceding word does not exceed
        // the threshold.
        // The text iterator should now be positioned at the end of the word we found.
        if (text->getIndex() < rangeEnd && wordLength < THAI_ROOT_COMBINE_THRESHOLD) {
            // if it is a dictionary word, do nothing. If it isn't, then if there is
            // no preceding word, or the non-word shares less than the minimum threshold
            // of characters with a dictionary word, then scan to resynchronize
            if (words[wordsFound%THAI_LOOKAHEAD].candidates(text, fDictionary, rangeEnd) <= 0
                  && (wordLength == 0
                      || words[wordsFound%THAI_LOOKAHEAD].longestPrefix() < THAI_PREFIX_COMBINE_THRESHOLD)) {
                // Look for a plausible word boundary
                //TODO: This section will need a rework for UText.
                int32_t remaining = rangeEnd - (current+wordLength);
                UChar32 pc = text->current32();
                int32_t chars = 0;
                while (TRUE) {
                    uc = text->next32();
                    // TODO: Here we're counting on the fact that the SA languages are all
                    // in the BMP. This should get fixed with the UText rework.
                    chars += 1;
                    if (--remaining <= 0) {
                        break;
                    }
                    if (fEndWordSet.contains(pc) && fBeginWordSet.contains(uc)) {
                        // Maybe. See if it's in the dictionary.
                        // NOTE: In the original Apple code, checked that the next
                        // two characters after uc were not 0x0E4C THANTHAKHAT before
                        // checking the dictionary. That is just a performance filter,
                        // but it's not clear it's faster than checking the trie.
                        int candidates = words[(wordsFound+1)%THAI_LOOKAHEAD].candidates(text, fDictionary, rangeEnd);
                        text->setIndex(current+wordLength+chars);
                        if (candidates > 0) {
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
            }
            else {
                // Back up to where we were for next iteration
                text->setIndex(current+wordLength);
            }
        }
        
        // Never stop before a combining mark.
        int32_t currPos;
        while ((currPos = text->getIndex()) < rangeEnd && fMarkSet.contains(text->current32())) {
            wordLength += text->move32(1, CharacterIterator::kCurrent) - currPos;
        }
        
        // Look ahead for possible suffixes if a dictionary word does not follow.
        // We do this in code rather than using a rule so that the heuristic
        // resynch continues to function. For example, one of the suffix characters
        // could be a typo in the middle of a word.
        if (text->getIndex() < rangeEnd && wordLength > 0) {
            if (words[wordsFound%THAI_LOOKAHEAD].candidates(text, fDictionary, rangeEnd) <= 0
                && fSuffixSet.contains(uc = text->current32())) {
                if (uc == THAI_PAIYANNOI) {
                    if (!fSuffixSet.contains(text->previous32())) {
                        // Skip over previous end and PAIYANNOI
                        text->move32(2, CharacterIterator::kCurrent);
                        wordLength += 1;            // Add PAIYANNOI to word
                        uc = text->current32();     // Fetch next character
                    }
                    else {
                        // Restore prior position
                        text->move32(1, CharacterIterator::kCurrent);
                    }
                }
                if (uc == THAI_MAIYAMOK) {
                    if (text->previous32() != THAI_MAIYAMOK) {
                        // Skip over previous end and MAIYAMOK
                        text->move32(2, CharacterIterator::kCurrent);
                        wordLength += 1;            // Add MAIYAMOK to word
                    }
                    else {
                        // Restore prior position
                        text->move32(1, CharacterIterator::kCurrent);
                    }
                }
            }
            else {
                text->setIndex(current+wordLength);
            }
        }
        
        // Did we find a word on this iteration? If so, push it on the break stack
        if (wordLength > 0) {
            foundBreaks.push((current+wordLength), status);
        }
    }
    
    // Don't return a break for the end of the dictionary range if there is one there.
    if (foundBreaks.peeki() >= rangeEnd) {
        (void) foundBreaks.popi();
        wordsFound -= 1;
    }

    return wordsFound;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
