/*
**********************************************************************
*   Copyright (C) 1999-2000 IBM Corp. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   12/1/99    rgillam     Complete port from Java.
*   01/13/2000 helena      Added UErrorCode to ctors.
**********************************************************************
*/

#include "ucmp8.h"
#include "unicode/dbbi.h"
#include "dbbi_tbl.h"
#include "uvector.h"

char DictionaryBasedBreakIterator::fgClassID = 0;

//=======================================================================
// constructors
//=======================================================================

DictionaryBasedBreakIterator::DictionaryBasedBreakIterator(UDataMemory* tablesImage,
                                                           char* dictionaryFilename, 
                                                           UErrorCode& status)
: RuleBasedBreakIterator((UDataMemory*)NULL),
  dictionaryCharCount(0),
  cachedBreakPositions(NULL),
  numCachedBreakPositions(0),
  positionInCache(0)
{
    tables = new DictionaryBasedBreakIteratorTables(tablesImage, dictionaryFilename, status);
    if (U_FAILURE(status)) {
        delete tables;
        return;
    }
    tables->addReference();
}

//=======================================================================
// boilerplate
//=======================================================================

/**
 * Destructor
 */
DictionaryBasedBreakIterator::~DictionaryBasedBreakIterator()
{
    delete [] cachedBreakPositions;
}

/**
 * Assignment operator.  Sets this iterator to have the same behavior,
 * and iterate over the same text, as the one passed in.
 */
DictionaryBasedBreakIterator&
DictionaryBasedBreakIterator::operator=(const DictionaryBasedBreakIterator& that) {
    reset();
    RuleBasedBreakIterator::operator=(that);
    return *this;
}

/**
 * Returns a newly-constructed RuleBasedBreakIterator with the same
 * behavior, and iterating over the same text, as this one.
 */
BreakIterator*
DictionaryBasedBreakIterator::clone() const {
    return new DictionaryBasedBreakIterator(*this);
}

//=======================================================================
// BreakIterator overrides
//=======================================================================

/**
 * Advances the iterator one step backwards.
 * @return The position of the last boundary position before the
 * current iteration position
 */
int32_t
DictionaryBasedBreakIterator::previous()
{
    // if we have cached break positions and we're still in the range
    // covered by them, just move one step backward in the cache
    if (cachedBreakPositions != NULL && positionInCache > 0) {
        --positionInCache;
        text->setIndex(cachedBreakPositions[positionInCache]);
        return cachedBreakPositions[positionInCache];
    }

    // otherwise, dump the cache and use the inherited previous() method to move
    // backward.  This may fill up the cache with new break positions, in which
    // case we have to mark our position in the cache
    else {
        reset();
        int32_t result = RuleBasedBreakIterator::previous();
        if (cachedBreakPositions != NULL) {
            positionInCache = numCachedBreakPositions - 2;
        }
        return result;
    }
}

/**
 * Sets the current iteration position to the last boundary position
 * before the specified position.
 * @param offset The position to begin searching from
 * @return The position of the last boundary before "offset"
 */
int32_t
DictionaryBasedBreakIterator::preceding(int32_t offset)
{
    // if the offset passed in is already past the end of the text,
    // just return DONE; if it's before the beginning, return the
    // text's starting offset
    if (text == NULL || offset > text->endIndex()) {
        return BreakIterator::DONE;
    }
    else if (offset < text->startIndex()) {
        return text->startIndex();
    }

    // if we have no cached break positions, or "offset" is outside the
    // range covered by the cache, we can just call the inherited routine
    // (which will eventually call other routines in this class that may
    // refresh the cache)
    if (cachedBreakPositions == NULL || offset <= cachedBreakPositions[0] ||
            offset > cachedBreakPositions[numCachedBreakPositions - 1]) {
        reset();
        return RuleBasedBreakIterator::preceding(offset);
    }

    // on the other hand, if "offset" is within the range covered by the cache,
    // then all we have to do is search the cache for the last break position
    // before "offset"
    else {
        positionInCache = 0;
        while (positionInCache < numCachedBreakPositions
               && offset > cachedBreakPositions[positionInCache])
            ++positionInCache;
        --positionInCache;
        text->setIndex(cachedBreakPositions[positionInCache]);
        return text->getIndex();
    }
}

/**
 * Sets the current iteration position to the first boundary position after
 * the specified position.
 * @param offset The position to begin searching forward from
 * @return The position of the first boundary after "offset"
 */
int32_t
DictionaryBasedBreakIterator::following(int32_t offset)
{
    // if the offset passed in is already past the end of the text,
    // just return DONE; if it's before the beginning, return the
    // text's starting offset
    if (text == NULL || offset > text->endIndex()) {
        return BreakIterator::DONE;
    }
    else if (offset < text->startIndex()) {
        return text->startIndex();
    }

    // if we have no cached break positions, or if "offset" is outside the
    // range covered by the cache, then dump the cache and call our
    // inherited following() method.  This will call other methods in this
    // class that may refresh the cache.
    if (cachedBreakPositions == NULL || offset < cachedBreakPositions[0] ||
            offset >= cachedBreakPositions[numCachedBreakPositions - 1]) {
        reset();
        return RuleBasedBreakIterator::following(offset);
    }

    // on the other hand, if "offset" is within the range covered by the
    // cache, then just search the cache for the first break position
    // after "offset"
    else {
        positionInCache = 0;
        while (positionInCache < numCachedBreakPositions
               && offset >= cachedBreakPositions[positionInCache])
            ++positionInCache;
        text->setIndex(cachedBreakPositions[positionInCache]);
        return text->getIndex();
    }
}

/**
 * This is the implementation function for next().
 */
int32_t
DictionaryBasedBreakIterator::handleNext()
{
    // if there are no cached break positions, or if we've just moved
    // off the end of the range covered by the cache, we have to dump
    // and possibly regenerate the cache
    if (cachedBreakPositions == NULL || positionInCache == numCachedBreakPositions - 1) {

        // start by using the inherited handleNext() to find a tentative return
        // value.   dictionaryCharCount tells us how many dictionary characters
        // we passed over on our way to the tentative return value
        int32_t startPos = text->getIndex();
        dictionaryCharCount = 0;
        int32_t result = RuleBasedBreakIterator::handleNext();

        // if we passed over more than one dictionary character, then we use
        // divideUpDictionaryRange() to regenerate the cached break positions
        // for the new range
        if (dictionaryCharCount > 1 && result - startPos > 1) {
            divideUpDictionaryRange(startPos, result);
        }

        // otherwise, the value we got back from the inherited fuction
        // is our return value, and we can dump the cache
        else {
            reset();
            return result;
        }
    }

    // if the cache of break positions has been regenerated (or existed all
    // along), then just advance to the next break position in the cache
    // and return it
    if (cachedBreakPositions != NULL) {
        ++positionInCache;
        text->setIndex(cachedBreakPositions[positionInCache]);
        return cachedBreakPositions[positionInCache];
    }
    return -9999;   // SHOULD NEVER GET HERE!
}

void
DictionaryBasedBreakIterator::reset()
{
    delete [] cachedBreakPositions;
    cachedBreakPositions = NULL;
    numCachedBreakPositions = 0;
    dictionaryCharCount = 0;
    positionInCache = 0;
}

/**
 * This is the function that actually implements the dictionary-based
 * algorithm.  Given the endpoints of a range of text, it uses the
 * dictionary to determine the positions of any boundaries in this
 * range.  It stores all the boundary positions it discovers in
 * cachedBreakPositions so that we only have to do this work once
 * for each time we enter the range.
 */
void
DictionaryBasedBreakIterator::divideUpDictionaryRange(int32_t startPos, int32_t endPos)
{
    // to avoid casts throughout the rest of this function
    DictionaryBasedBreakIteratorTables* tables
            = (DictionaryBasedBreakIteratorTables*)(this->tables);

    // the range we're dividing may begin or end with non-dictionary characters
    // (i.e., for line breaking, we may have leading or trailing punctuation
    // that needs to be kept with the word).  Seek from the beginning of the
    // range to the first dictionary character
    text->setIndex(startPos);
    UChar c = text->current();
    int category = tables->lookupCategory(c, this);
    while (category == IGNORE || !tables->categoryFlags[category]) {
        c = text->next();
        category = tables->lookupCategory(c, this);
    }
    

    // initialize.  We maintain two stacks: currentBreakPositions contains
    // the list of break positions that will be returned if we successfully
    // finish traversing the whole range now.  possibleBreakPositions lists
    // all other possible word ends we've passed along the way.  (Whenever
    // we reach an error [a sequence of characters that can't begin any word
    // in the dictionary], we back up, possibly delete some breaks from
    // currentBreakPositions, move a break from possibleBreakPositions
    // to currentBreakPositions, and start over from there.  This process
    // continues in this way until we either successfully make it all the way
    // across the range, or exhaust all of our combinations of break
    // positions.) wrongBreakPositions is used to keep track of paths we've
    // tried on previous iterations.  As the iterator backs up further and
    // further, this saves us from having to follow each possible path
    // through the text all the way to the error (hopefully avoiding many
    // future recursive calls as well).
    UStack currentBreakPositions;
    UStack possibleBreakPositions;
    UVector wrongBreakPositions;

    // the dictionary is implemented as a trie, which is treated as a state
    // machine.  -1 represents the end of a legal word.  Every word in the
    // dictionary is represented by a path from the root node to -1.  A path
    // that ends in state 0 is an illegal combination of characters.
    int16_t state = 0;

    // these two variables are used for error handling.  We keep track of the
    // farthest we've gotten through the range being divided, and the combination
    // of breaks that got us that far.  If we use up all possible break
    // combinations, the text contains an error or a word that's not in the
    // dictionary.  In this case, we "bless" the break positions that got us the
    // farthest as real break positions, and then start over from scratch with
    // the character where the error occurred.
    int32_t farthestEndPoint = text->getIndex();
    UStack bestBreakPositions;
    UBool bestBreakPositionsInitialized = FALSE;

    // initialize (we always exit the loop with a break statement)
    c = text->current();
    while (TRUE) {

        // if we can transition to state "-1" from our current state, we're
        // on the last character of a legal word.  Push that position onto
        // the possible-break-positions stack
        if (tables->dictionary.at(state, (int32_t)0) == -1) {
            possibleBreakPositions.push((void*)text->getIndex());
        }

        // look up the new state to transition to in the dictionary
        state = tables->dictionary.at(state, c);

        // if the character we're sitting on causes us to transition to
        // the "end of word" state, then it was a non-dictionary character
        // and we've successfully traversed the whole range.  Drop out
        // of the loop.
        if (state == -1) {
            currentBreakPositions.push((void*)text->getIndex());
            break;
        }

        // if the character we're sitting on causes us to transition to
        // the error state, or if we've gone off the end of the range
        // without transitioning to the "end of word" state, we've hit
        // an error...
        else if (state == 0 || text->getIndex() >= endPos) {

            // if this is the farthest we've gotten, take note of it in
            // case there's an error in the text
            if (text->getIndex() > farthestEndPoint) {
                farthestEndPoint = text->getIndex();
                bestBreakPositions.removeAllElements();
                bestBreakPositionsInitialized = TRUE;
                for (int32_t i = 0; i < currentBreakPositions.size(); i++) {
                    bestBreakPositions.push(currentBreakPositions.elementAt(i));
                }
            }

            // wrongBreakPositions is a list of all break positions we've tried starting
            // that didn't allow us to traverse all the way through the text.  Every time
            // we pop a break position off of currentBreakPositions, we put it into
            // wrongBreakPositions to avoid trying it again later.  If we make it to this
            // spot, we're either going to back up to a break in possibleBreakPositions
            // and try starting over from there, or we've exhausted all possible break
            // positions and are going to do the fallback procedure.  This loop prevents
            // us from messing with anything in possibleBreakPositions that didn't work as
            // a starting point the last time we tried it (this is to prevent a bunch of
            // repetitive checks from slowing down some extreme cases)
            while (!possibleBreakPositions.isEmpty() && wrongBreakPositions.contains(
                        possibleBreakPositions.peek())) {
                possibleBreakPositions.pop();
            }
            
            // if we've used up all possible break-position combinations, there's
            // an error or an unknown word in the text.  In this case, we start
            // over, treating the farthest character we've reached as the beginning
            // of the range, and "blessing" the break positions that got us that
            // far as real break positions
            if (possibleBreakPositions.isEmpty()) {
                if (bestBreakPositionsInitialized) {
                    currentBreakPositions.removeAllElements();
                    for (int32_t i = 0; i < bestBreakPositions.size(); i++) {
                        currentBreakPositions.push(bestBreakPositions.elementAt(i));
                    }
                    bestBreakPositions.removeAllElements();
                    if (farthestEndPoint < endPos) {
                        text->setIndex(farthestEndPoint + 1);
                    }
                    else {
                        break;
                    }
                }
                else {
                    if ((currentBreakPositions.isEmpty()
                            || (int32_t)(unsigned long)currentBreakPositions.peek() != text->getIndex())
                            && text->getIndex() != startPos) {
                        currentBreakPositions.push((void*)text->getIndex());
                    }
                    text->next();
                    currentBreakPositions.push((void*)text->getIndex());
                }
            }

            // if we still have more break positions we can try, then promote the
            // last break in possibleBreakPositions into currentBreakPositions,
            // and get rid of all entries in currentBreakPositions that come after
            // it.  Then back up to that position and start over from there (i.e.,
            // treat that position as the beginning of a new word)
            else {
                int32_t temp = (int32_t)(unsigned long)possibleBreakPositions.pop();
                void* temp2 = NULL;
                while (!currentBreakPositions.isEmpty() && temp <
                       (int32_t)(unsigned long)currentBreakPositions.peek()) {
                    temp2 = currentBreakPositions.pop();
                    wrongBreakPositions.addElement(temp2);
                }
                currentBreakPositions.push((void*)temp);
                text->setIndex((int32_t)(unsigned long)currentBreakPositions.peek());
            }

            // re-sync "c" for the next go-round, and drop out of the loop if
            // we've made it off the end of the range
            c = text->current();
            if (text->getIndex() >= endPos) {
                break;
            }
        }

        // if we didn't hit any exceptional conditions on this last iteration,
        // just advance to the next character and loop
        else {
            c = text->next();
        }
    }

    // dump the last break position in the list, and replace it with the actual
    // end of the range (which may be the same character, or may be further on
    // because the range actually ended with non-dictionary characters we want to
    // keep with the word)
    if (!currentBreakPositions.isEmpty()) {
        currentBreakPositions.pop();
    }
    currentBreakPositions.push((void*)endPos);

    // create a regular array to hold the break positions and copy
    // the break positions from the stack to the array (in addition,
    // our starting position goes into this array as a break position).
    // This array becomes the cache of break positions used by next()
    // and previous(), so this is where we actually refresh the cache.
    cachedBreakPositions = new int32_t[currentBreakPositions.size() + 1];
    numCachedBreakPositions = currentBreakPositions.size() + 1;
    cachedBreakPositions[0] = startPos;

    for (int32_t i = 0; i < currentBreakPositions.size(); i++) {
        cachedBreakPositions[i + 1] = (int32_t)(unsigned long)currentBreakPositions.elementAt(i);
    }
    positionInCache = 0;
}

