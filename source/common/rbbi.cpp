/*
***************************************************************************
*   Copyright (C) 1999-2004 International Business Machines Corporation   *
*   and others. All rights reserved.                                      *
***************************************************************************
*/
//
//  file:  rbbi.c    Contains the implementation of the rule based break iterator
//                   runtime engine and the API implementation for
//                   class RuleBasedBreakIterator
//

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/rbbi.h"
#include "unicode/schriter.h"
#include "unicode/udata.h"
#include "unicode/uclean.h"
#include "rbbidata.h"
#include "rbbirb.h"
#include "cmemory.h"
#include "cstring.h"

#include "uassert.h"

U_NAMESPACE_BEGIN


static const int16_t START_STATE = 1;     // The state number of the starting state
static const int16_t STOP_STATE  = 0;     // The state-transition value indicating "stop"


UOBJECT_DEFINE_RTTI_IMPLEMENTATION(RuleBasedBreakIterator)


//=======================================================================
// constructors
//=======================================================================

/**
 * Constructs a RuleBasedBreakIterator that uses the already-created
 * tables object that is passed in as a parameter.
 */
RuleBasedBreakIterator::RuleBasedBreakIterator(RBBIDataHeader* data, UErrorCode &status)
{
    init();
    fData = new RBBIDataWrapper(data, status); // status checked in constructor
    if (U_FAILURE(status)) {return;}
    if(fData == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
}

//-------------------------------------------------------------------------------
//
//   Constructor   from a UDataMemory handle to precompiled break rules
//                 stored in an ICU data file.
//
//-------------------------------------------------------------------------------
RuleBasedBreakIterator::RuleBasedBreakIterator(UDataMemory* udm, UErrorCode &status)
{
    init();
    fData = new RBBIDataWrapper(udm, status); // status checked in constructor
    if (U_FAILURE(status)) {return;}
    if(fData == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
        return;
    }
}



//-------------------------------------------------------------------------------
//
//   Constructor       from a set of rules supplied as a string.
//
//-------------------------------------------------------------------------------
RuleBasedBreakIterator::RuleBasedBreakIterator( const UnicodeString  &rules,
                                                UParseError          &parseError,
                                                UErrorCode           &status)
{
    u_init(&status);      // Just in case ICU is not yet initialized
    init();
    if (U_FAILURE(status)) {return;}
    RuleBasedBreakIterator *bi = (RuleBasedBreakIterator *)
        RBBIRuleBuilder::createRuleBasedBreakIterator(rules, parseError, status);
    // Note:  This is a bit awkward.  The RBBI ruleBuilder has a factory method that
    //        creates and returns a complete RBBI.  From here, in a constructor, we
    //        can't just return the object created by the builder factory, hence
    //        the assignment of the factory created object to "this".
    if (U_SUCCESS(status)) {
        *this = *bi;
        delete bi;
    }
}


//-------------------------------------------------------------------------------
//
// Default Constructor.      Create an empty shell that can be set up later.
//                           Used when creating a RuleBasedBreakIterator from a set
//                           of rules.
//-------------------------------------------------------------------------------
RuleBasedBreakIterator::RuleBasedBreakIterator() {
    init();
}


//-------------------------------------------------------------------------------
//
//   Copy constructor.  Will produce a break iterator with the same behavior,
//                      and which iterates over the same text, as the one passed in.
//
//-------------------------------------------------------------------------------
RuleBasedBreakIterator::RuleBasedBreakIterator(const RuleBasedBreakIterator& other)
: BreakIterator(other)
{
    this->init();
    *this = other;
}


/**
 * Destructor
 */
RuleBasedBreakIterator::~RuleBasedBreakIterator() {
    delete fText;
    fText = NULL;
    if (fData != NULL) {
        fData->removeReference();
        fData = NULL;
    }
}

/**
 * Assignment operator.  Sets this iterator to have the same behavior,
 * and iterate over the same text, as the one passed in.
 */
RuleBasedBreakIterator&
RuleBasedBreakIterator::operator=(const RuleBasedBreakIterator& that) {
    if (this == &that) {
        return *this;
    }
    delete fText;
    fText = NULL;
    if (that.fText != NULL) {
        fText = that.fText->clone();
    }

    if (fData != NULL) {
        fData->removeReference();
        fData = NULL;
    }
    if (that.fData != NULL) {
        fData = that.fData->addReference();
    }
    fTrace = that.fTrace;

    return *this;
}



//-----------------------------------------------------------------------------
//
//    init()      Shared initialization routine.   Used by all the constructors.
//                Initializes all fields, leaving the object in a consistent state.
//
//-----------------------------------------------------------------------------
UBool RuleBasedBreakIterator::fTrace = FALSE;
void RuleBasedBreakIterator::init() {

    fText                 = NULL;
    fData                 = NULL;
    fLastRuleStatusIndex  = 0;
    fLastStatusIndexValid = TRUE;
    fDictionaryCharCount  = 0;

#ifdef RBBI_DEBUG
    static UBool debugInitDone = FALSE;
    if (debugInitDone == FALSE) {
        char *debugEnv = getenv("U_RBBIDEBUG");
        if (debugEnv && uprv_strstr(debugEnv, "trace")) {
            fTrace = TRUE;
        }
        debugInitDone = TRUE;
    }
#endif
}



//-----------------------------------------------------------------------------
//
//    clone - Returns a newly-constructed RuleBasedBreakIterator with the same
//            behavior, and iterating over the same text, as this one.
//            Virtual function: does the right thing with subclasses.
//
//-----------------------------------------------------------------------------
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
    UBool r = FALSE;
    if (that.getDynamicClassID() != getDynamicClassID()) {
        return r;
    }

    const RuleBasedBreakIterator& that2 = (const RuleBasedBreakIterator&) that;
    if (fText == that2.fText ||
        (fText != NULL && that2.fText != NULL && *that2.fText == *fText)) {
        if (that2.fData == fData ||
            (fData != NULL && that2.fData != NULL && *that2.fData == *fData)) {
            r = TRUE;
        }
    }
    return r;
}

/**
 * Compute a hash code for this BreakIterator
 * @return A hash code
 */
int32_t
RuleBasedBreakIterator::hashCode(void) const {
    int32_t   hash = 0;
    if (fData != NULL) {
        hash = fData->hashCode();
    }
    return hash;
}

/**
 * Returns the description used to create this iterator
 */
const UnicodeString&
RuleBasedBreakIterator::getRules() const {
    if (fData != NULL) {
        return fData->getRuleSourceString();
    } else {
        static const UnicodeString *s;
        if (s == NULL) {
            // TODO:  something more elegant here.
            //        perhaps API should return the string by value.
            //        Note:  thread unsafe init & leak are semi-ok, better than
            //               what was before.  Sould be cleaned up, though.
            s = new UnicodeString;
        }
        return *s;
    }
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
    if (nonConstThis->fText == NULL) {
        // TODO:  do this in a way that does not do a default conversion!
        nonConstThis->fText = new StringCharacterIterator("");
    }
    return *nonConstThis->fText;
}

/**
 * Set the iterator to analyze a new piece of text.  This function resets
 * the current iteration position to the beginning of the text.
 * @param newText An iterator over the text to analyze.
 */
void
RuleBasedBreakIterator::adoptText(CharacterIterator* newText) {
    reset();
    delete fText;
    fText = newText;
    this->first();
}

/**
 * Set the iterator to analyze a new piece of text.  This function resets
 * the current iteration position to the beginning of the text.
 * @param newText An iterator over the text to analyze.
 */
void
RuleBasedBreakIterator::setText(const UnicodeString& newText) {
    reset();
    if (fText != NULL && fText->getDynamicClassID()
            == StringCharacterIterator::getStaticClassID()) {
        ((StringCharacterIterator*)fText)->setText(newText);
    }
    else {
        delete fText;
        fText = new StringCharacterIterator(newText);
    }
    this->first();
}



/**
 * Sets the current iteration position to the beginning of the text.
 * (i.e., the CharacterIterator's starting offset).
 * @return The offset of the beginning of the text.
 */
int32_t RuleBasedBreakIterator::first(void) {
    reset();
    fLastRuleStatusIndex  = 0;
    fLastStatusIndexValid = TRUE;
    if (fText == NULL)
        return BreakIterator::DONE;

    fText->first();
    return fText->getIndex();
}

/**
 * Sets the current iteration position to the end of the text.
 * (i.e., the CharacterIterator's ending offset).
 * @return The text's past-the-end offset.
 */
int32_t RuleBasedBreakIterator::last(void) {
    reset();
    if (fText == NULL) {
        fLastRuleStatusIndex  = 0;
        fLastStatusIndexValid = TRUE;
        return BreakIterator::DONE;
    }

    // I'm not sure why, but t.last() returns the offset of the last character,
    // rather than the past-the-end offset
    //
    //   (It's so a loop like for(p=it.last(); p!=DONE; p=it.previous()) ...
    //     will work correctly.)


    fLastStatusIndexValid = FALSE;
    int32_t pos = fText->endIndex();
    fText->setIndex(pos);

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
    if (fText == NULL || current() == fText->startIndex()) {
        fLastRuleStatusIndex  = 0;
        fLastStatusIndexValid = TRUE;
        return BreakIterator::DONE;
    }

    if (fData->fSafeRevTable != NULL || fData->fSafeFwdTable != NULL) {
        return handlePrevious(fData->fReverseTable);
    }

    // old rule syntax
    // set things up.  handlePrevious() will back us up to some valid
    // break position before the current position (we back our internal
    // iterator up one step to prevent handlePrevious() from returning
    // the current position), but not necessarily the last one before
    // where we started

    int32_t start = current();

    fText->previous32();
    int32_t lastResult    = handlePrevious();
    int32_t result        = lastResult;
    int32_t lastTag       = 0;
    UBool   breakTagValid = FALSE;

    // iterate forward from the known break position until we pass our
    // starting point.  The last break position before the starting
    // point is our return value

    for (;;) {
        result         = handleNext();
        if (result == BreakIterator::DONE || result >= start) {
            break;
        }
        lastResult     = result;
        lastTag        = fLastRuleStatusIndex;
        breakTagValid  = TRUE;
    }

    // fLastBreakTag wants to have the value for section of text preceding
    // the result position that we are to return (in lastResult.)  If
    // the backwards rules overshot and the above loop had to do two or more
    // handleNext()s to move up to the desired return position, we will have a valid
    // tag value. But, if handlePrevious() took us to exactly the correct result positon,
    // we wont have a tag value for that position, which is only set by handleNext().

    // set the current iteration position to be the last break position
    // before where we started, and then return that value
    fText->setIndex(lastResult);
    fLastRuleStatusIndex  = lastTag;       // for use by getRuleStatus()
    fLastStatusIndexValid = breakTagValid;
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
    fLastRuleStatusIndex  = 0;
    fLastStatusIndexValid = TRUE;
    if (fText == NULL || offset >= fText->endIndex()) {
        last();
        return next();
    }
    else if (offset < fText->startIndex()) {
        return first();
    }

    // otherwise, set our internal iteration position (temporarily)
    // to the position passed in.  If this is the _beginning_ position,
    // then we can just use next() to get our return value

    int32_t result = 0;

    if (fData->fSafeRevTable != NULL) {
        // new rule syntax
        /// todo synwee
        fText->setIndex(offset);
        // move forward one codepoint to prepare for moving back to a
        // safe point.
        // this handles offset being between a supplementary character
        fText->next32();
        // handlePrevious will move most of the time to < 1 boundary away
        handlePrevious(fData->fSafeRevTable);
        int32_t result = next();
        while (result <= offset) {
            result = next();
        }
        return result;
    }
    if (fData->fSafeFwdTable != NULL) {
        // backup plan if forward safe table is not available
        fText->setIndex(offset);
        fText->previous32();
        // handle next will give result >= offset
        handleNext(fData->fSafeFwdTable);
        // previous will give result 0 or 1 boundary away from offset,
        // most of the time
        // we have to
        int32_t oldresult = previous();
        while (oldresult > offset) {
            int32_t result = previous();
            if (result <= offset) {
                return oldresult;
            }
            oldresult = result;
        }
        int32_t result = next();
        if (result <= offset) {
            return next();
        }
        return result;
    }
    // otherwise, we have to sync up first.  Use handlePrevious() to back
    // us up to a known break position before the specified position (if
    // we can determine that the specified position is a break position,
    // we don't back up at all).  This may or may not be the last break
    // position at or before our starting position.  Advance forward
    // from here until we've passed the starting position.  The position
    // we stop on will be the first break position after the specified one.
    // old rule syntax

    fText->setIndex(offset);
    if (offset == fText->startIndex()) {
        return handleNext();
    }
    result = previous();

    while (result != BreakIterator::DONE && result <= offset) {
        result = next();
    }

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
    if (fText == NULL || offset > fText->endIndex()) {
        // return BreakIterator::DONE;
        return last();
    }
    else if (offset < fText->startIndex()) {
        return first();
    }

    // if we start by updating the current iteration position to the
    // position specified by the caller, we can just use previous()
    // to carry out this operation

    if (fData->fSafeFwdTable != NULL) {
        /// todo synwee
        // new rule syntax
        fText->setIndex(offset);
        // move backwards one codepoint to prepare for moving forwards to a
        // safe point.
        // this handles offset being between a supplementary character
        // TODO:  would it be better to just check for being in the middle of a surrogate pair,
        //        rather than adjusting the position unconditionally?
        //        (Change would interact with safe rules.)
        fText->previous32();
        handleNext(fData->fSafeFwdTable);
        int32_t result = fText->getIndex();
        while (result >= offset) {
            result = previous();
        }
        return result;
    }
    if (fData->fSafeRevTable != NULL) {
        // backup plan if forward safe table is not available
        fText->setIndex(offset);
        fText->next32();
        // handle previous will give result <= offset
        handlePrevious(fData->fSafeRevTable);

        // next will give result 0 or 1 boundary away from offset,
        // most of the time
        // we have to
        int32_t oldresult = next();
        while (oldresult < offset) {
            int32_t result = next();
            if (result >= offset) {
                return oldresult;
            }
            oldresult = result;
        }
        int32_t result = previous();
        if (result >= offset) {
            return previous();
        }
        return result;
    }

    // old rule syntax
    fText->setIndex(offset);
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
    if (fText == NULL || offset == fText->startIndex()) {
        first();       // For side effects on current position, tag values.
        return TRUE;
    }

    if (offset == fText->endIndex()) {
        last();       // For side effects on current position, tag values.
        return TRUE;
    }

    // out-of-range indexes are never boundary positions
    if (offset < fText->startIndex()) {
        first();       // For side effects on current position, tag values.
        return FALSE;
    }

    if (offset > fText->endIndex()) {
        last();        // For side effects on current position, tag values.
        return FALSE;
    }

    // otherwise, we can use following() on the position before the specified
    // one and return true if the position we get back is the one the user
    // specified
    return following(offset - 1) == offset;
}

/**
 * Returns the current iteration position.
 * @return The current iteration position.
 */
int32_t RuleBasedBreakIterator::current(void) const {
    return (fText != NULL) ? fText->getIndex() : BreakIterator::DONE;
}

//=======================================================================
// implementation
//=======================================================================


//-----------------------------------------------------------------------------------
//
//  handleNext()
//     This method is the actual implementation of the next() method.  All iteration
//     vectors through here.  This method initializes the state machine to state 1
//     and advances through the text character by character until we reach the end
//     of the text or the state machine transitions to state 0.  We update our return
//     value every time the state machine passes through an accepting state.
//
//-----------------------------------------------------------------------------------
int32_t RuleBasedBreakIterator::handleNext() {
    return handleNext(fData->fForwardTable);
}

int32_t RuleBasedBreakIterator::handleNext(const RBBIStateTable *statetable) {
    if (fTrace) {
        RBBIDebugPrintf("Handle Next   pos   char  state category  \n");
    }

    // No matter what, handleNext alway correctly sets the break tag value.
    fLastStatusIndexValid = TRUE;

    // if we're already at the end of the text, return DONE.
    if (fText == NULL || fData == NULL || fText->hasNext() == FALSE) {
        fLastRuleStatusIndex = 0;
        return BreakIterator::DONE;
    }

    int32_t initialPosition = fText->getIndex();
    int32_t result          = initialPosition;
    int32_t lookaheadResult = 0;

    // Initialize the state machine.  Begin in state 1
    int32_t            state           = START_STATE;
    int16_t            category;
    UChar32            c               = fText->current32();
    RBBIStateTableRow *row;
    int32_t            lookaheadStatus = 0;
    int32_t            lookaheadTagIdx = 0;

    fLastRuleStatusIndex = 0;

    row = (RBBIStateTableRow *)    // Point to starting row of state table.
        (statetable->fTableData + (statetable->fRowLen * state));

    // Character Category fetch for starting character.
    //    See comments on character category code within loop, below.
    UTRIE_GET16(&fData->fTrie, c, category);
    if ((category & 0x4000) != 0)  {
          fDictionaryCharCount++;
          category &= ~0x4000;
        }

    // loop until we reach the end of the text or transition to state 0
    for (;;) {
        if (c == CharacterIterator::DONE && fText->hasNext()==FALSE) {
            // Reached end of input string.
            //    Note: CharacterIterator::DONE is 0xffff, which is also a legal
            //          character value.  Check for DONE first, because it's quicker,
            //          but also need to check fText->hasNext() to be certain.

            if (lookaheadResult > result) {
                // We ran off the end of the string with a pending look-ahead match.
                // Treat this as if the look-ahead condition had been met, and return
                //  the match at the / position from the look-ahead rule.
                result               = lookaheadResult;
                fLastRuleStatusIndex = lookaheadTagIdx;
                lookaheadStatus = 0;
            } else if (result == initialPosition) {
                // Ran off end, no match found.
                // move forward one
                fText->setIndex(initialPosition);
                fText->next32();
                fText->getIndex();
            }
            break;
        }
        // look up the current character's character category, which tells us
        // which column in the state table to look at.
        // Note:  the 16 in UTRIE_GET16 refers to the size of the data being returned,
        //        not the size of the character going in, which is a UChar32.
        //
        UTRIE_GET16(&fData->fTrie, c, category);

        // Check the dictionary bit in the character's category.
        //    Counter is only used by dictionary based iterators (subclasses).
        //    Chars that need to be handled by a dictionary have a flag bit set
        //    in their category values.
        //
        if ((category & 0x4000) != 0)  {
            fDictionaryCharCount++;
            //  And off the dictionary flag bit.
            category &= ~0x4000;
        }

        if (fTrace) {
            RBBIDebugPrintf("             %4d   ", fText->getIndex());
            if (0x20<=c && c<0x7f) {
                RBBIDebugPrintf("\"%c\"  ", c);
            } else {
                RBBIDebugPrintf("%5x  ", c);
            }
            RBBIDebugPrintf("%3d  %3d\n", state, category);
        }

        // look up a state transition in the state table
        state = row->fNextState[category];
        row = (RBBIStateTableRow *)
            (statetable->fTableData + (statetable->fRowLen * state));

        // Get the next character.  Doing it here positions the iterator
        //    to the correct position for recording matches in the code that
        //    follows.
        c = fText->next32();

        if (row->fAccepting == -1) {
            // Match found, common case, could have lookahead so we move on to check it
            result = fText->getIndex();
            /// added
            fLastRuleStatusIndex = row->fTagIdx;   // Remember the break status (tag) values.
        }

        if (row->fLookAhead != 0) {
            if (lookaheadStatus != 0
                && row->fAccepting == lookaheadStatus) {
                // Lookahead match is completed.  Set the result accordingly, but only
                // if no other rule has matched further in the mean time.
                result               = lookaheadResult;
                fLastRuleStatusIndex = lookaheadTagIdx;
                lookaheadStatus      = 0;
                /// i think we have to back up to read the lookahead character again
                /// fText->setIndex(lookaheadResult);
                /// TODO: this is a simple hack since reverse rules only have simple
                /// lookahead rules that we can definitely break out from.
                /// we need to make the lookahead rules not chain eventually.
                /// return result;
                /// this is going to be the longest match again
                goto continueOn;
            }

            int32_t  r = fText->getIndex();
            lookaheadResult = r;
            lookaheadStatus = row->fLookAhead;
            lookaheadTagIdx = row->fTagIdx;
            goto continueOn;
        }


        if (row->fAccepting == 0) {
            // No match, nothing of interest happening, common case.
            goto continueOn;
        }

        lookaheadStatus = 0;           // clear out any pending look-ahead matches.

continueOn:
        if (state == STOP_STATE) {
            // This is the normal exit from the lookup state machine.
            // We have advanced through the string until it is certain that no
            //   longer match is possible, no matter what characters follow.
            break;
        }
    }

    // The state machine is done.  Check whether it found a match...

    // If the iterator failed to advance in the match engine, force it ahead by one.
    //   (This really indicates a defect in the break rules.  They should always match
    //    at least one character.)
    if (result == initialPosition) {
        result = fText->setIndex(initialPosition);
        fText ->next32();
        result = fText->getIndex();
    }

    // Leave the iterator at our result position.
    fText->setIndex(result);
    if (fTrace) {
        RBBIDebugPrintf("result = %d\n\n", result);
    }
    return result;
}


//----------------------------------------------------------------
//
//   handlePrevious(void)     This is the variant used with old style rules
//                            (Overshoot to a safe point, then move forward)
//
//----------------------------------------------------------------
int32_t RuleBasedBreakIterator::handlePrevious(void) {
    if (fText == NULL || fData == NULL) {
        return 0;
    }
    if (fData->fReverseTable == NULL) {
        return fText->setToStart();
    }

    int32_t            state           = START_STATE;
    int32_t            category;
    int32_t            lastCategory    = 0;
    int32_t            result          = fText->getIndex();
    int32_t            lookaheadStatus = 0;
    int32_t            lookaheadResult = 0;
    int32_t            lookaheadTagIdx = 0;
    UChar32            c               = fText->current32();
    RBBIStateTableRow *row;

    row = (RBBIStateTableRow *)
        (this->fData->fReverseTable->fTableData + (state * fData->fReverseTable->fRowLen));
    UTRIE_GET16(&fData->fTrie, c, category);
    if ((category & 0x4000) != 0)  {
        fDictionaryCharCount++;
        category &= ~0x4000;
    }

    if (fTrace) {
        RBBIDebugPrintf("Handle Prev   pos   char  state category  \n");
    }

    // loop until we reach the beginning of the text or transition to state 0
    for (;;) {
        if (c == CharacterIterator::DONE && fText->hasPrevious()==FALSE) {
            break;
        }

        // save the last character's category and look up the current
        // character's category
        lastCategory = category;
        UTRIE_GET16(&fData->fTrie, c, category);

        // Check the dictionary bit in the character's category.
        //    Counter is only used by dictionary based iterators.
        //
        if ((category & 0x4000) != 0)  {
            fDictionaryCharCount++;
            category &= ~0x4000;
        }

        if (fTrace) {
            RBBIDebugPrintf("             %4d   ", fText->getIndex());
            if (0x20<=c && c<0x7f) {
                RBBIDebugPrintf("\"%c\"  ", c);
            } else {
                RBBIDebugPrintf("%5x  ", c);
            }
            RBBIDebugPrintf("%3d  %3d\n", state, category);
        }

        // look up a state transition in the backwards state table
        state = row->fNextState[category];
        row = (RBBIStateTableRow *)
            (this->fData->fReverseTable->fTableData + (state * fData->fReverseTable->fRowLen));

        if (row->fAccepting == 0 && row->fLookAhead == 0) {
            // No match, nothing of interest happening, common case.
            goto continueOn;
        }

        if (row->fAccepting == -1) {
            // Match found, common case, no lookahead involved.
            result = fText->getIndex();
            lookaheadStatus = 0;     // clear out any pending look-ahead matches.
            goto continueOn;
        }

        if (row->fAccepting == 0 && row->fLookAhead != 0) {
            // Lookahead match point.  Remember it, but only if no other rule
            //                         has unconditionally matched to this point.
            // TODO:  handle case where there's a pending match from a different rule
            //        where lookaheadStatus != 0  && lookaheadStatus != row->fLookAhead.
            int32_t  r = fText->getIndex();
            if (r > result) {
                lookaheadResult = r;
                lookaheadStatus = row->fLookAhead;
                lookaheadTagIdx = row->fTagIdx;
            }
            goto continueOn;
        }

        if (row->fAccepting != 0 && row->fLookAhead != 0) {
            // Lookahead match is completed.  Set the result accordingly, but only
            //   if no other rule has matched further in the mean time.
            if (lookaheadResult > result) {
                U_ASSERT(row->fAccepting == lookaheadStatus);   // TODO:  handle this case
                //    of overlapping lookahead matches.
                result               = lookaheadResult;
                fLastRuleStatusIndex = lookaheadTagIdx;
                lookaheadStatus = 0;
            }
            goto continueOn;
        }

continueOn:
        if (state == STOP_STATE) {
            break;
        }

        // then advance one character backwards
        c = fText->previous32();
    }

    // Note:  the result postion isn't what is returned to the user by previous(),
    //        but where the implementation of previous() turns around and
    //        starts iterating forward again.
    if (c == CharacterIterator::DONE && fText->hasPrevious()==FALSE) {
        result = fText->startIndex();
    }
    fText->setIndex(result);

    return result;
}


//-----------------------------------------------------------------------------------
//
//  handlePrevious()
//
//      This method backs the iterator back up to a "safe position" in the text.
//      This is a position that we know, without any context, may be any position
//      not more than 2 breaks away. Occasionally, the position may be less than
//      one break away.
//      The various calling methods then iterate forward from this safe position to
//      the appropriate position to return.
//
//      The logic of this function is very similar to handleNext(), above.
//
//-----------------------------------------------------------------------------------
int32_t RuleBasedBreakIterator::handlePrevious(const RBBIStateTable *statetable) {
    if (fText == NULL || statetable == NULL) {
        return 0;
    }
    // break tag is no longer valid after icu switched to exact backwards
    // positioning.
    fLastStatusIndexValid = FALSE;
    if (statetable == NULL) {
        return fText->setToStart();
    }

    int32_t            state              = START_STATE;
    int32_t            category;
    int32_t            lastCategory       = 0;
    UBool              hasPassedStartText = !fText->hasPrevious();
    UChar32            c                  = fText->previous32();
    // previous character
    int32_t            result             = fText->getIndex();
    int32_t            lookaheadStatus    = 0;
    int32_t            lookaheadResult    = 0;
    int32_t            lookaheadTagIdx    = 0;
    UBool              lookAheadHardBreak = (statetable->fFlags & RBBI_LOOKAHEAD_HARD_BREAK) != 0;

    RBBIStateTableRow *row;

    row = (RBBIStateTableRow *)
        (statetable->fTableData + (state * statetable->fRowLen));
    UTRIE_GET16(&fData->fTrie, c, category);
    if ((category & 0x4000) != 0)  {
        fDictionaryCharCount++;
        category &= ~0x4000;
    }

    if (fTrace) {
        RBBIDebugPrintf("Handle Prev   pos   char  state category  \n");
    }

    // loop until we reach the beginning of the text or transition to state 0
    for (;;) {
        // if (c == CharacterIterator::DONE && fText->hasPrevious()==FALSE) {
        if (hasPassedStartText) {
            // if we have already considered the start of the text
            if (row->fLookAhead != 0 && lookaheadResult == 0) {
                result = 0;
            }
            break;
        }

        // save the last character's category and look up the current
        // character's category
        lastCategory = category;
        UTRIE_GET16(&fData->fTrie, c, category);

        // Check the dictionary bit in the character's category.
        //    Counter is only used by dictionary based iterators.
        //
        if ((category & 0x4000) != 0)  {
            fDictionaryCharCount++;
            category &= ~0x4000;
        }

        if (fTrace) {
            RBBIDebugPrintf("             %4d   ", fText->getIndex());
            if (0x20<=c && c<0x7f) {
                RBBIDebugPrintf("\"%c\"  ", c);
            } else {
                RBBIDebugPrintf("%5x  ", c);
            }
            RBBIDebugPrintf("%3d  %3d\n", state, category);
        }

        // look up a state transition in the backwards state table
        state = row->fNextState[category];
        row = (RBBIStateTableRow *)
            (statetable->fTableData + (state * statetable->fRowLen));

        if (row->fAccepting == -1) {
            // Match found, common case, could have lookahead so we move on to check it
            result = fText->getIndex();
            /// added
            fLastRuleStatusIndex   = row->fTagIdx;   // Remember the break status (tag) value.
        }

        if (row->fLookAhead != 0) {
            if (lookaheadStatus != 0
                && row->fAccepting == lookaheadStatus) {
                // Lookahead match is completed.  Set the result accordingly, but only
                // if no other rule has matched further in the mean time.
                result               = lookaheadResult;
                fLastRuleStatusIndex = lookaheadTagIdx;
                lookaheadStatus      = 0;
                /// i think we have to back up to read the lookahead character again
                /// fText->setIndex(lookaheadResult);
                /// TODO: this is a simple hack since reverse rules only have simple
                /// lookahead rules that we can definitely break out from.
                /// we need to make the lookahead rules not chain eventually.
                /// return result;
                /// this is going to be the longest match again

                /// syn wee todo hard coded for line breaks stuff
                /// needs to provide a tag in rules to ensure a stop.

                if (lookAheadHardBreak) {
                    fText->setIndex(result);
                    return result;
                }
                category = lastCategory;
                fText->setIndex(result);

                goto continueOn;
            }

            int32_t    r         = fText->getIndex();
            lookaheadResult      = r;
            lookaheadStatus      = row->fLookAhead;
            fLastRuleStatusIndex = row->fTagIdx;
            goto continueOn;
        }

        // not lookahead
        if (row->fAccepting == 0) {
            // No match, nothing of interest happening, common case.
            goto continueOn;
        }

        lookaheadStatus = 0;     // clear out any pending look-ahead matches.

continueOn:
        if (state == STOP_STATE) {
            break;
        }

        // then advance one character backwards
        hasPassedStartText = !fText->hasPrevious();
        c = fText->previous32();
    }

    // Note:  the result postion isn't what is returned to the user by previous(),
    //        but where the implementation of previous() turns around and
    //        starts iterating forward again.
    fText->setIndex(result);

    return result;
}


void
RuleBasedBreakIterator::reset()
{
    // Base-class version of this function is a no-op.
    // Subclasses may override with their own reset behavior.
}



//-------------------------------------------------------------------------------
//
//   getRuleStatus()   Return the break rule tag associated with the current
//                     iterator position.  If the iterator arrived at its current
//                     position by iterating forwards, the value will have been
//                     cached by the handleNext() function.
//
//                     If no cached status value is available, the status is
//                     found by doing a previous() followed by a next(), which
//                     leaves the iterator where it started, and computes the
//                     status while doing the next().
//
//-------------------------------------------------------------------------------
void RuleBasedBreakIterator::makeRuleStatusValid() {
    if (fLastStatusIndexValid == FALSE) {
        //  No cached status is available.
        if (fText == NULL || current() == fText->startIndex()) {
            //  At start of text, or there is no text.  Status is always zero.
            fLastRuleStatusIndex = 0;
            fLastStatusIndexValid = TRUE;
        } else {
            //  Not at start of text.  Find status the tedious way.
            int32_t pa = current();
            previous();
            int32_t pb = next();
            if (pa != pb) {
                // note: the if (pa != pb) test is here only to eliminate warnings for
                //       unused local variables on gcc.  Logically, it isn't needed.
                U_ASSERT(pa == pb);
            }
        }
    }
    U_ASSERT(fLastStatusIndexValid == TRUE);
    U_ASSERT(fLastRuleStatusIndex >= 0  &&  fLastRuleStatusIndex < fData->fStatusMaxIdx);
}


int32_t  RuleBasedBreakIterator::getRuleStatus() const {
    RuleBasedBreakIterator *nonConstThis  = (RuleBasedBreakIterator *)this;
    nonConstThis->makeRuleStatusValid();

    // fLastRuleStatusIndex indexes to the start of the appropriate status record
    //                                                 (the number of status values.)
    //   This function returns the last (largest) of the array of status values.
    int32_t  idx = fLastRuleStatusIndex + fData->fRuleStatusTable[fLastRuleStatusIndex];
    int32_t  tagVal = fData->fRuleStatusTable[idx];

    return tagVal;
}




int32_t RuleBasedBreakIterator::getRuleStatusVec(
             int32_t *fillInVec, int32_t capacity, UErrorCode &status)
{
    if (U_FAILURE(status)) {
        return 0;
    }

    RuleBasedBreakIterator *nonConstThis  = (RuleBasedBreakIterator *)this;
    nonConstThis->makeRuleStatusValid();
    int32_t  numVals = fData->fRuleStatusTable[fLastRuleStatusIndex];
    int32_t  numValsToCopy = numVals;
    if (numVals > capacity) {
        status = U_BUFFER_OVERFLOW_ERROR;
        numValsToCopy = capacity;
    }
    int i;
    for (i=0; i<numValsToCopy; i++) {
        fillInVec[i] = fData->fRuleStatusTable[fLastRuleStatusIndex + i + 1];
    }
    return numVals;
}



//-------------------------------------------------------------------------------
//
//   getBinaryRules        Access to the compiled form of the rules,
//                         for use by build system tools that save the data
//                         for standard iterator types.
//
//-------------------------------------------------------------------------------
const uint8_t  *RuleBasedBreakIterator::getBinaryRules(uint32_t &length) {
    const uint8_t  *retPtr = NULL;
    length = 0;

    if (fData != NULL) {
        retPtr = (const uint8_t *)fData->fHeader;
        length = fData->fHeader->fLength;
    }
    return retPtr;
}




//-------------------------------------------------------------------------------
//
//  BufferClone       TODO:  In my (Andy) opinion, this function should be deprecated.
//                    Saving one heap allocation isn't worth the trouble.
//                    Cloning shouldn't be done in tight loops, and
//                    making the clone copy involves other heap operations anyway.
//                    And the application code for correctly dealing with buffer
//                    size problems and the eventual object destruction is ugly.
//
//-------------------------------------------------------------------------------
BreakIterator *  RuleBasedBreakIterator::createBufferClone(void *stackBuffer,
                                   int32_t &bufferSize,
                                   UErrorCode &status)
{
    if (U_FAILURE(status)){
        return NULL;
    }

    //
    //  If user buffer size is zero this is a preflight operation to
    //    obtain the needed buffer size, allowing for worst case misalignment.
    //
    if (bufferSize == 0) {
        bufferSize = sizeof(RuleBasedBreakIterator) + U_ALIGNMENT_OFFSET_UP(0);
        return NULL;
    }


    //
    //  Check the alignment and size of the user supplied buffer.
    //  Allocate heap memory if the user supplied memory is insufficient.
    //
    char    *buf   = (char *)stackBuffer;
    uint32_t s      = bufferSize;

    if (stackBuffer == NULL) {
        s = 0;   // Ignore size, force allocation if user didn't give us a buffer.
    }
    if (U_ALIGNMENT_OFFSET(stackBuffer) != 0) {
        uint32_t offsetUp = (uint32_t)U_ALIGNMENT_OFFSET_UP(buf);
        s   -= offsetUp;
        buf += offsetUp;
    }
    if (s < sizeof(RuleBasedBreakIterator)) {
        buf = (char *) new RuleBasedBreakIterator;
        if (buf == 0) {
            status = U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        status = U_SAFECLONE_ALLOCATED_WARNING;
    }

    //
    //  Clone the object.
    //    TODO:  using an overloaded operator new to directly initialize the
    //           copy in the user's buffer would be better, but it doesn't seem
    //           to get along with namespaces.  Investigate why.
    //
    //           The memcpy is only safe with an empty (default constructed)
    //           break iterator.  Use on others can screw up reference counts
    //           to data.  memcpy-ing objects is not really a good idea...
    //
    RuleBasedBreakIterator localIter;        // Empty break iterator, source for memcpy
    RuleBasedBreakIterator *clone = (RuleBasedBreakIterator *)buf;
    uprv_memcpy(clone, &localIter, sizeof(RuleBasedBreakIterator)); // clone = empty, but initialized, iterator.
    *clone = *this;                          // clone = the real one we want.
    if (status != U_SAFECLONE_ALLOCATED_WARNING) {
        clone->fBufferClone = TRUE;
    }

    return clone;
}



//-------------------------------------------------------------------------------
//
//  isDictionaryChar      Return true if the category lookup for this char
//                        indicates that it is in the set of dictionary lookup
//                        chars.
//
//                        This function is intended for use by dictionary based
//                        break iterators.
//
//-------------------------------------------------------------------------------
UBool RuleBasedBreakIterator::isDictionaryChar(UChar32   c) {
    if (fData == NULL) {
        return FALSE;
    }
    uint16_t category;
    UTRIE_GET16(&fData->fTrie, c, category);
    return (category & 0x4000) != 0;
}



U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
