/*
**********************************************************************
*   Copyright (C) 1999 International Business Machines Corporation   *
*   and others. All rights reserved.                                 *
**********************************************************************
*   Date        Name        Description
*   10/22/99    alan        Creation.
**********************************************************************
*/

#include "rbbi.h"
#include "rbbi_bld.h"

/**
 * A token used as a character-category value to identify ignore characters
 */
int8_t RuleBasedBreakIterator::IGNORE = -1;

/**
 * The state number of the starting state
 */
int16_t RuleBasedBreakIterator::START_STATE = 1;

/**
 * The state-transition value indicating "stop"
 */
int16_t RuleBasedBreakIterator::STOP_STATE = 0;

//=======================================================================
// constructors
//=======================================================================

/**
 * Constructs a RuleBasedBreakIterator according to the description
 * provided.  If the description is malformed, throws an
 * IllegalArgumentException.  Normally, instead of constructing a
 * RuleBasedBreakIterator directory, you'll use the factory methods
 * on BreakIterator to create one indirectly from a description
 * in the framework's resource files.  You'd use this when you want
 * special behavior not provided by the built-in iterators.
 */
RuleBasedBreakIterator::RuleBasedBreakIterator(const UnicodeString& description) {
    this.description = description;
    
    // the actual work is done by the Builder class
    Builder builder;
    builder.buildBreakIterator(*this, description);
}

//=======================================================================
// boilerplate
//=======================================================================
/**
 * Clones this iterator.
 * @return A newly-constructed RuleBasedBreakIterator with the same
 * behavior as this one.
 */
RuleBasedBreakIterator* RuleBasedBreakIterator::clone() const {
    return new RuleBasedBreakIterator(*this);
}

/**
 * Returns true if both BreakIterators are of the same class, have the same
 * rules, and iterate over the same text.
 */
bool_t RuleBasedBreakIterator::operator==(const RuleBasedBreakIterator& that) {
    return description.equals(((RuleBasedBreakIterator)that).description)
        && text.equals(((RuleBasedBreakIterator)that).text);
}

/**
 * Returns the description used to create this iterator
 */
UnicodeString RuleBasedBreakIterator::toString() {
    return description;
}

/**
 * Compute a hashcode for this BreakIterator
 * @return A hash code
 */
int32_t RuleBasedBreakIterator::hashCode() {
    return description.hashCode();
}

//=======================================================================
// BreakIterator overrides
//=======================================================================
/**
 * Sets the current iteration position to the beginning of the text.
 * (i.e., the CharacterIterator's starting offset).
 * @return The offset of the beginning of the text.
 */
int32_t RuleBasedBreakIterator::first() {
    CharacterIterator t = getText();

    t.first();
    return t.getIndex();
}

/**
 * Sets the current iteration position to the end of the text.
 * (i.e., the CharacterIterator's ending offset).
 * @return The text's past-the-end offset.
 */
int32_t RuleBasedBreakIterator::last() {
    CharacterIterator t = getText();

    // I'm not sure why, but t.last() returns the offset of the last character,
    // rather than the past-the-end offset
    t.setIndex(t.getEndIndex());
    return t.getIndex();
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
int32_t RuleBasedBreakIterator::next() {
    return handleNext();
}

/**
 * Advances the iterator backwards, to the last boundary preceding this one.
 * @return The position of the last boundary position preceding this one.
 */
int32_t RuleBasedBreakIterator::previous() {
    // if we're already sitting at the beginning of the text, return DONE
    CharacterIterator text = getText();
    if (current() == text.getBeginIndex())
        return BreakIterator.DONE;

    // set things up.  handlePrevious() will back us up to some valid
    // break position before the current position (we back our internal
    // iterator up one step to prevent handlePrevious() from returning
    // the current position), but not necessarily the last one before
    // where we started
    int32_t start = current();
    text.previous();
    int32_t lastResult = handlePrevious();
    int32_t result = lastResult;
    
    // iterate forward from the known break position until we pass our
    // starting point.  The last break position before the starting
    // point is our return value
    while (result != BreakIterator.DONE && result < start) {
        lastResult = result;
        result = handleNext();
    }
    
    // set the current iteration position to be the last break position
    // before where we started, and then return that value
    text.setIndex(lastResult);
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
    // just return DONE
    CharacterIterator text = getText();
    if (offset == text.getEndIndex())
        return BreakIterator.DONE;

    // otherwise, set our internal iteration position (temporarily)
    // to the position passed in.  If this is the _beginning_ position,
    // then we can just use next() to get our return value
    text.setIndex(offset);
    if (offset == text.getBeginIndex())
        return handleNext();

    // otherwise, we have to sync up first.  Use handlePrevious() to back
    // us up to a known break position before the specified position (if
    // we can determine that the specified position is a break position,
    // we don't back up at all).  This may or may not be the last break
    // position at or before our starting position.  Advance forward
    // from here until we've passed the starting position.  The position
    // we stop on will be the first break position after the specified one.
    int32_t result = handlePrevious();
    while (result != BreakIterator.DONE && result <= offset)
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
    // if we start by updating the current iteration position to the
    // position specified by the caller, we can just use previous()
    // to carry out this operation
    CharacterIterator text = getText();
    text.setIndex(offset);
    return previous();
}

/**
 * Returns true if the specfied position is a boundary position.  As a side
 * effect, leaves the iterator pointing to the first boundary position at
 * or after "offset".
 * @param offset the offset to check.
 * @return True if "offset" is a boundary position.
 */
bool_t RuleBasedBreakIterator::isBoundary(int32_t offset) {
    // 0 is always a boundary position (I suspect this code is wrong; I think
    // we're supposed to be comparing "offset" against text.getBeginIndex(). )
    if (offset == 0)
        return TRUE;
        
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
int32_t RuleBasedBreakIterator::current() {
    return getText().getIndex();
}

/**
 * Return a CharacterIterator over the text being analyzed.  This version
 * of this method returns the actual CharacterIterator we're using internally.
 * Changing the state of this iterator can have undefined consequences.  If
 * you need to change it, clone it first.
 * @return An iterator over the text being analyzed.
 */
CharacterIterator RuleBasedBreakIterator::getText() {
    // The iterator is initialized pointing to no text at all, so if this
    // function is called while we're in that state, we have to fudge an
    // an iterator to return.
    if (text == 0)
        text = new StringCharacterIterator("");
    return text;
}

/**
 * Set the iterator to analyze a new piece of text.  This function resets
 * the current iteration position to the beginning of the text.
 * @param newText An iterator over the text to analyze.
 */
void RuleBasedBreakIterator::setText(CharacterIterator newText) {
    text = newText;
    text.first();
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
int32_t RuleBasedBreakIterator::handleNext() {
    // if we're already at the end of the text, return DONE.
    CharacterIterator text = getText();
    if (text.getIndex() == text.getEndIndex())
        return BreakIterator.DONE;

    // no matter what, we always advance at least one character forward
    int32_t result = text.getIndex() + 1;
    
    // begin in state 1
    int32_t state = START_STATE;
    int32_t category;
    UChar c = text.current();

    // loop until we reach the end of the text or transition to state 0
    while (c != CharacterIterator.DONE && state != STOP_STATE) {

        // look up the current character's character category (which tells us
        // which column in the state table to look at)
        category = lookupCategory(c);
        
        // if the character isn't an ignore character, look up a state
        // transition in the state table
        if (category != IGNORE) {
            state = lookupState(state, category);
        }
        
        // if the state we've just transitioned to is an accepting state,
        // update our return value to be the current iteration position
        if (endStates[state])
            result = text.getIndex() + 1;
        c = text.next();
    }
    text.setIndex(result);
    return result;
}

/**
 * This method backs the iterator back up to a "safe position" in the text.
 * This is a position that we know, without any context, must be a break position.
 * The various calling methods then iterate forward from this safe position to
 * the appropriate position to return.  (For more information, see the description
 * of buildBackwardsStateTable() in RuleBasedBreakIterator.Builder.)
 */
int32_t RuleBasedBreakIterator::handlePrevious() {
    CharacterIterator text = getText();
    int32_t state = START_STATE;
    int32_t category = 0;
    int32_t lastCategory = 0;
    UChar c = text.current();
    
    // loop until we reach the beginning of the text or transition to state 0
    while (c != CharacterIterator.DONE && state != STOP_STATE) {

        // save the last character's category and look up the current
        // character's category
        lastCategory = category;
        category = lookupCategory(c);
        
        // if the current character isn't an ignore character, look up a
        // state transition in the backwards state table
        if (category != IGNORE)
            state = lookupBackwardState(state, category);
            
        // then advance one character backwards
        c = text.previous();
    }
    
    // if we didn't march off the beginning of the text, we're either one or two
    // positions away from the real break position.  (One because of the call to
    // previous() at the end of the loop above, and another because the character
    // that takes us into the stop state will always be the character BEFORE
    // the break position.)
    if (c != CharacterIterator.DONE) {
        if (lastCategory != IGNORE)
            text.setIndex(text.getIndex() + 2);
        else
            text.next();
    }
    return text.getIndex();
}

/**
 * Looks up a character's category (i.e., its category for breaking purposes,
 * not its Unicode category)
 */
int32_t RuleBasedBreakIterator::lookupCategory(UChar c) {
    return UCharCategoryTable.elementAt(c);
}

/**
 * Given a current state and a character category, looks up the
 * next state to transition to in the state table.
 */
int32_t RuleBasedBreakIterator::lookupState(int32_t state, int32_t category) {
    return stateTable[state * numCategories + category];
}

/**
 * Given a current state and a character category, looks up the
 * next state to transition to in the backwards state table.
 */
int32_t RuleBasedBreakIterator::lookupBackwardState(int32_t state, int32_t category) {
    return backwardsStateTable[state * numCategories + category];
}
