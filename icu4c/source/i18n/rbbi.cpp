/*
**********************************************************************
*   Copyright (C) 1999 International Business Machines Corporation   *
*   and others. All rights reserved.                                 *
**********************************************************************
*   Date        Name        Description
*   11/11/99    rgillam     Complete port from Java.
**********************************************************************
*/

#include "unicode/rbbi.h"
#include "unicode/schriter.h"
#include "rbbi_tbl.h"
#include "filestrm.h"
#include "cmemory.h"

/**
 * A token used as a character-category value to identify ignore characters
 */
int8_t
RuleBasedBreakIterator::IGNORE = -1;

/**
 * The state number of the starting state
 */
int16_t
RuleBasedBreakIterator::START_STATE = 1;

/**
 * The state-transition value indicating "stop"
 */
int16_t
RuleBasedBreakIterator::STOP_STATE = 0;

/**
 * Class ID.  (value is irrelevant; address is important)
 */
char
RuleBasedBreakIterator::fgClassID = 0;

//=======================================================================
// constructors
//=======================================================================

/**
 * Constructs a RuleBasedBreakIterator that uses the already-created
 * tables object that is passed in as a parameter.
 */
RuleBasedBreakIterator::RuleBasedBreakIterator(RuleBasedBreakIteratorTables* adoptTables)
: text(NULL),
  tables(adoptTables)
{
}

// This constructor uses the udata interface to create a BreakIterator whose
// internal tables live in a memory-mapped file.  "image" is a pointer to the
// beginning of that file.
RuleBasedBreakIterator::RuleBasedBreakIterator(UDataMemory* image)
: text(NULL),
  tables(image != NULL ? new RuleBasedBreakIteratorTables(image) : NULL)
{
    if (tables != NULL)
        tables->addReference();
}

/**
 * Copy constructor.  Will produce a collator with the same behavior,
 * and which iterates over the same text, as the one passed in.
 */
RuleBasedBreakIterator::RuleBasedBreakIterator(const RuleBasedBreakIterator& that)
: text(that.text->clone()),
  tables(that.tables)
{
    tables->addReference();
}

//=======================================================================
// boilerplate
//=======================================================================
/**
 * Destructor
 */
RuleBasedBreakIterator::~RuleBasedBreakIterator() {
    delete text;
    tables->removeReference();
}

/**
 * Assignment operator.  Sets this iterator to have the same behavior,
 * and iterate over the same text, as the one passed in.
 */
RuleBasedBreakIterator&
RuleBasedBreakIterator::operator=(const RuleBasedBreakIterator& that) {
    delete text;
    text = that.text->clone();

    tables->removeReference();
    tables = that.tables;
    tables->addReference();

    return *this;
}

/**
 * Returns a newly-constructed RuleBasedBreakIterator with the same
 * behavior, and iterating over the same text, as this one.
 */
BreakIterator*
RuleBasedBreakIterator::clone(void) const {
    return new RuleBasedBreakIterator(*this);
}

/**
 * Equality operator.  Returns TRUE if both BreakIterators are of the
 * same class, have the same behavior, and iterate over the same text.
 */
UBool
RuleBasedBreakIterator::operator==(const BreakIterator& that) const {
    if (that.getDynamicClassID() != getDynamicClassID())
        return FALSE;

    
    const RuleBasedBreakIterator& that2 = (const RuleBasedBreakIterator&)that;
    return (that2.text == text || *that2.text == *text)
            && (that2.tables == tables || *that2.tables == *tables);
}

/**
 * Compute a hash code for this BreakIterator
 * @return A hash code
 */
int32_t
RuleBasedBreakIterator::hashCode(void) const {
    return tables->hashCode();
}

/**
 * Returns the description used to create this iterator
 */
const UnicodeString&
RuleBasedBreakIterator::getRules() const {
    return tables->getRules();
}

//=======================================================================
// BreakIterator overrides
//=======================================================================

/**
 * Return a CharacterIterator over the text being analyzed.  This version
 * of this method returns the actual CharacterIterator we're using internally.
 * Changing the state of this iterator can have undefined consequences.  If
 * you need to change it, clone it first.
 * @return An iterator over the text being analyzed.
 */
const CharacterIterator&
RuleBasedBreakIterator::getText() const {
    RuleBasedBreakIterator* nonConstThis = (RuleBasedBreakIterator*)this;
    
    // The iterator is initialized pointing to no text at all, so if this
    // function is called while we're in that state, we have to fudge an
    // an iterator to return.
    if (nonConstThis->text == NULL)
        nonConstThis->text = new StringCharacterIterator("");
    return *nonConstThis->text;
}

/**
 * Returns a newly-created CharacterIterator that the caller is to take
 * ownership of.
 * THIS FUNCTION SHOULD NOT BE HERE.  IT'S HERE BECAUSE BreakIterator DEFINES
 * IT AS PURE VIRTUAL, FORCING RBBI TO IMPLEMENT IT.  IT SHOULD BE REMOVED
 * FROM *BOTH* CLASSES.
 */
CharacterIterator*
RuleBasedBreakIterator::createText() const {
    if (text == NULL)
        return new StringCharacterIterator("");
    else
        return text->clone();
}


/**
 * Set the iterator to analyze a new piece of text.  This function resets
 * the current iteration position to the beginning of the text.
 * @param newText An iterator over the text to analyze.
 */
void
RuleBasedBreakIterator::adoptText(CharacterIterator* newText) {
    reset();
    delete text;
    text = newText;
    text->first();
}

/**
 * Set the iterator to analyze a new piece of text.  This function resets
 * the current iteration position to the beginning of the text.
 * @param newText An iterator over the text to analyze.
 */
void
RuleBasedBreakIterator::setText(const UnicodeString& newText) {
    reset();
    if (text != NULL && text->getDynamicClassID()
            == StringCharacterIterator::getStaticClassID()) {
        ((StringCharacterIterator*)text)->setText(newText);
    }
    else {
        delete text;
        text = new StringCharacterIterator(newText);
        text->first();
    }
}

/**
 * Set the iterator to analyze a new piece of text.  This function resets
 * the current iteration position to the beginning of the text.
 * @param newText The text to analyze.
 * THIS FUNCTION SHOULD NOT BE HERE.  IT'S HERE BECAUSE BreakIterator DEFINES
 * IT AS PURE VIRTUAL, FORCING RBBI TO IMPLEMENT IT.  IT SHOULD BE REMOVED
 * FROM *BOTH* CLASSES.
 */
void
RuleBasedBreakIterator::setText(const UnicodeString* newText) {
    setText(*newText);
}

/**
 * Sets the current iteration position to the beginning of the text.
 * (i.e., the CharacterIterator's starting offset).
 * @return The offset of the beginning of the text.
 */
int32_t RuleBasedBreakIterator::first(void) {
    reset();
    if (text == NULL)
        return BreakIterator::DONE;

    text->first();
    return text->getIndex();
}

/**
 * Sets the current iteration position to the end of the text.
 * (i.e., the CharacterIterator's ending offset).
 * @return The text's past-the-end offset.
 */
int32_t RuleBasedBreakIterator::last(void) {
    reset();
    if (text == NULL)
        return BreakIterator::DONE;
    
    // I'm not sure why, but t.last() returns the offset of the last character,
    // rather than the past-the-end offset

    int32_t pos = text->endIndex();
    text->setIndex(pos);
    return pos;
}

/**
 * Advances the iterator either forward or backward the specified number of steps.
 * Negative values move backward, and positive values move forward.  This is
 * equivalent to repeatedly calling next() or previous().
 * @param n The number of steps to move.  The sign indicates the direction
 * (negative is backwards, and positive is forwards).
 * @return The character offset of the boundary position n boundaries away from
 * the current one.
 */
int32_t RuleBasedBreakIterator::next(int32_t n) {
    int32_t result = current();
    while (n > 0) {
        result = handleNext();
        --n;
    }
    while (n < 0) {
        result = previous();
        ++n;
    }
    return result;
}

/**
 * Advances the iterator to the next boundary position.
 * @return The position of the first boundary after this one.
 */
int32_t RuleBasedBreakIterator::next(void) {
    return handleNext();
}

/**
 * Advances the iterator backwards, to the last boundary preceding this one.
 * @return The position of the last boundary position preceding this one.
 */
int32_t RuleBasedBreakIterator::previous(void) {
    // if we're already sitting at the beginning of the text, return DONE
    if (text == NULL || current() == text->startIndex())
        return BreakIterator::DONE;

    // set things up.  handlePrevious() will back us up to some valid
    // break position before the current position (we back our internal
    // iterator up one step to prevent handlePrevious() from returning
    // the current position), but not necessarily the last one before
    // where we started
    int32_t start = current();
    text->previous();
    int32_t lastResult = handlePrevious();
    int32_t result = lastResult;
    
    // iterate forward from the known break position until we pass our
    // starting point.  The last break position before the starting
    // point is our return value
    while (result != BreakIterator::DONE && result < start) {
        lastResult = result;
        result = handleNext();
    }
    
    // set the current iteration position to be the last break position
    // before where we started, and then return that value
    text->setIndex(lastResult);
    return lastResult;
}

/**
 * Sets the iterator to refer to the first boundary position following
 * the specified position.
 * @offset The position from which to begin searching for a break position.
 * @return The position of the first break after the current position.
 */
int32_t RuleBasedBreakIterator::following(int32_t offset) {
    // if the offset passed in is already past the end of the text,
    // just return DONE; if it's before the beginning, return the
    // text's starting offset
    if (text == NULL || offset >= text->endIndex()) {
        return BreakIterator::DONE;
    }
    else if (offset < text->startIndex()) {
        return text->startIndex();
    }

    // otherwise, set our internal iteration position (temporarily)
    // to the position passed in.  If this is the _beginning_ position,
    // then we can just use next() to get our return value
    text->setIndex(offset);
    if (offset == text->startIndex())
        return handleNext();

    // otherwise, we have to sync up first.  Use handlePrevious() to back
    // us up to a known break position before the specified position (if
    // we can determine that the specified position is a break position,
    // we don't back up at all).  This may or may not be the last break
    // position at or before our starting position.  Advance forward
    // from here until we've passed the starting position.  The position
    // we stop on will be the first break position after the specified one.
    int32_t result = handlePrevious();
    while (result != BreakIterator::DONE && result <= offset)
        result = handleNext();
    return result;
}

/**
 * Sets the iterator to refer to the last boundary position before the
 * specified position.
 * @offset The position to begin searching for a break from.
 * @return The position of the last boundary before the starting position.
 */
int32_t RuleBasedBreakIterator::preceding(int32_t offset) {
    // if the offset passed in is already past the end of the text,
    // just return DONE; if it's before the beginning, return the
    // text's starting offset
    if (text == NULL || offset > text->endIndex()) {
        return BreakIterator::DONE;
    }
    else if (offset < text->startIndex()) {
        return text->startIndex();
    }
    
    // if we start by updating the current iteration position to the
    // position specified by the caller, we can just use previous()
    // to carry out this operation
    text->setIndex(offset);
    return previous();
}

/**
 * Returns true if the specfied position is a boundary position.  As a side
 * effect, leaves the iterator pointing to the first boundary position at
 * or after "offset".
 * @param offset the offset to check.
 * @return True if "offset" is a boundary position.
 */
UBool RuleBasedBreakIterator::isBoundary(int32_t offset) {
    // the beginning index of the iterator is always a boundary position by definition
    if (text == NULL || offset == text->startIndex()) {
        return TRUE;
    }

    // out-of-range indexes are never boundary positions
    else if (offset < text->startIndex() || offset > text->endIndex()) {
        return FALSE;
    }
        
    // otherwise, we can use following() on the position before the specified
    // one and return true of the position we get back is the one the user
    // specified
    else
        return following(offset - 1) == offset;
}

/**
 * Returns the current iteration position.
 * @return The current iteration position.
 */
int32_t RuleBasedBreakIterator::current(void) const {
    return (text != NULL) ? text->getIndex() : BreakIterator::DONE;
}

//=======================================================================
// implementation
//=======================================================================

/**
 * This method is the actual implementation of the next() method.  All iteration
 * vectors through here.  This method initializes the state machine to state 1
 * and advances through the text character by character until we reach the end
 * of the text or the state machine transitions to state 0.  We update our return
 * value every time the state machine passes through a possible end state.
 */
int32_t RuleBasedBreakIterator::handleNext(void) {
    // if we're already at the end of the text, return DONE.
    if (text == NULL || tables == NULL || text->getIndex() == text->endIndex())
        return BreakIterator::DONE;

    // no matter what, we always advance at least one character forward
    int32_t result = text->getIndex() + 1;
    int32_t lookaheadResult = 0;
    
    // begin in state 1
    int32_t state = START_STATE;
    int32_t category;
    UChar c = text->current();
    UChar lastC = c;
    int32_t lastCPos = 0;


    // loop until we reach the end of the text or transition to state 0
    while (c != CharacterIterator::DONE && state != STOP_STATE) {

        // look up the current character's character category (which tells us
        // which column in the state table to look at)
        category = tables->lookupCategory(c, this);
        
        // if the character isn't an ignore character, look up a state
        // transition in the state table
        if (category != IGNORE) {
            state = tables->lookupState(state, category);
        }
        
        // if the state we've just transitioned to is a lookahead state,
        // (but not also an end state), save its position.  If it's
        // both a lookahead state and an end state, update the break position
        // to the last saved lookup-state position
        if (tables->isLookaheadState(state)) {
            if (tables->isEndState(state)) {
                result = lookaheadResult;
            }
            else {
                lookaheadResult = text->getIndex() + 1;
            }
        }

        // otherwise, if the state we've just transitioned to is an accepting state,
        // update our return value to be the current iteration position
        else {
            if (tables->isEndState(state)) {
                result = text->getIndex() + 1;
            }
        }
            
        // keep track of the last "real" character we saw.  If this character isn't an
        // ignore character, take note of it and its position in the text
        if (category != IGNORE && state != STOP_STATE) {
            lastC = c;
            lastCPos = text->getIndex();
        }
        c = text->next();
    }

    // if we've run off the end of the text, and the very last character took us into
    // a lookahead state, advance the break position to the lookahead position
    // (the theory here is that if there are no characters at all after the lookahead
    // position, that always matches the lookahead criteria)
    if (c == CharacterIterator::DONE && lookaheadResult == text->endIndex()) {
        result = lookaheadResult;
    }
        
    // if the last character we saw before the one that took us into the stop state
    // was a mandatory breaking character, then the break position goes right after it
    // (this is here so that breaks come before, rather than after, a string of
    // ignore characters when they follow a mandatory break character)
    else if (lastC == 0x0a || lastC == 0x0d || lastC == 0x0c || lastC == 0x2028
            || lastC == 0x2029) {
        result = lastCPos + 1;
    }

    text->setIndex(result);
    return result;
}

/**
 * This method backs the iterator back up to a "safe position" in the text.
 * This is a position that we know, without any context, must be a break position.
 * The various calling methods then iterate forward from this safe position to
 * the appropriate position to return.  (For more information, see the description
 * of buildBackwardsStateTable() in RuleBasedBreakIterator.Builder.)
 */
int32_t RuleBasedBreakIterator::handlePrevious(void) {
    if (text == NULL || tables == NULL)
        return 0;
    
    int32_t state = START_STATE;
    int32_t category = 0;
    int32_t lastCategory = 0;
    UChar c = text->current();
    
    // loop until we reach the beginning of the text or transition to state 0
    while (c != CharacterIterator::DONE && state != STOP_STATE) {

        // save the last character's category and look up the current
        // character's category
        lastCategory = category;
        category = tables->lookupCategory(c, this);
        
        // if the current character isn't an ignore character, look up a
        // state transition in the backwards state table
        if (category != IGNORE)
            state = tables->lookupBackwardState(state, category);
            
        // then advance one character backwards
        c = text->previous();
    }
    
    // if we didn't march off the beginning of the text, we're either one or two
    // positions away from the real break position.  (One because of the call to
    // previous() at the end of the loop above, and another because the character
    // that takes us into the stop state will always be the character BEFORE
    // the break position.)
    if (c != CharacterIterator::DONE) {
        if (lastCategory != IGNORE)
            text->setIndex(text->getIndex() + 2);
        else
            text->next();
    }

    return text->getIndex();
}

void
RuleBasedBreakIterator::reset()
{
    // Base-class version of this function is a no-op.
    // Subclasses may override with their own reset behavior.
}

// internal type for BufferClone 
struct bufferCloneStructUChar
{
    uint8_t bi   [sizeof(RuleBasedBreakIterator)] ;
    uint8_t text [sizeof(UCharCharacterIterator)] ;
};

struct bufferCloneStructString
{
    uint8_t bi   [sizeof(RuleBasedBreakIterator)] ;
    uint8_t text [sizeof(StringCharacterIterator)] ;
};

BreakIterator *  RuleBasedBreakIterator::createBufferClone(void *stackBuffer,
                                   int32_t &BufferSize,
                                   UErrorCode &status)
{
    RuleBasedBreakIterator * localIterator;
    int32_t bufferSizeNeeded; 
    UBool IterIsUChar;
    UBool IterIsString;

    if (U_FAILURE(status)){
        return 0;
    }
    if (!this){
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if (text == NULL)
    {
        bufferSizeNeeded = sizeof(RuleBasedBreakIterator);
        IterIsString = IterIsUChar = FALSE;
    }
    else if (text->getDynamicClassID() == StringCharacterIterator::getStaticClassID()) 
    {
        bufferSizeNeeded = sizeof(struct bufferCloneStructString);
        IterIsString = TRUE;
        IterIsUChar = FALSE;
    } 
    else if (text->getDynamicClassID() == UCharCharacterIterator::getStaticClassID()) 
    {
        bufferSizeNeeded = sizeof(struct bufferCloneStructUChar);
        IterIsString = FALSE;
        IterIsUChar = TRUE;
    }
    else
    {
        // code has changed - time to make a real CharacterIterator::CreateBufferClone()
    }
    if (BufferSize == 0){ /* 'preflighting' request - set needed size into *pBufferSize */
        BufferSize = bufferSizeNeeded;
        return 0;
    }
    if (BufferSize < bufferSizeNeeded || !stackBuffer)
    {
        /* allocate one here...*/
        localIterator = new RuleBasedBreakIterator(*this);
        status = U_SAFECLONE_ALLOCATED_ERROR;
        return localIterator;
    }
    if (IterIsUChar) {
        struct bufferCloneStructUChar * localClone 
                = (struct bufferCloneStructUChar  *)stackBuffer;
        localIterator = (RuleBasedBreakIterator *)&localClone->bi;
        uprv_memcpy(localIterator, this, sizeof(RuleBasedBreakIterator));
        uprv_memcpy(&localClone->text, text, sizeof(UCharCharacterIterator));
        localIterator->text = (CharacterIterator *) &localClone->text;
    } else if (IterIsString) {
        struct bufferCloneStructString * localClone 
                = (struct bufferCloneStructString  *)stackBuffer;
        localIterator = (RuleBasedBreakIterator *)&localClone->bi;
        uprv_memcpy(localIterator, this, sizeof(RuleBasedBreakIterator));
        uprv_memcpy(&localClone->text, text, sizeof(StringCharacterIterator));
        localIterator->text = (CharacterIterator *)&localClone->text;
    } else {
        RuleBasedBreakIterator * localClone 
                = (RuleBasedBreakIterator *)stackBuffer;
        localIterator = localClone;
        uprv_memcpy(localIterator, this, sizeof(RuleBasedBreakIterator));
    }
 
    localIterator->fBufferClone = TRUE;
 
    return localIterator;    
}
