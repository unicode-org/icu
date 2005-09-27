/*
***************************************************************************
*   Copyright (C) 1999-2005 International Business Machines Corporation   *
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
        nonConstThis->fText = new StringCharacterIterator(UnicodeString());
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

    //fText->first();
    fText->setToStart();
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
        // new rule syntax
        fText->setIndex(offset);

        int32_t newOffset = fText->getIndex();  
        if (newOffset != offset) {
            // Will come here if specified offset was not a code point boundary AND
            //   the underlying implmentation is using UText, which snaps any non-code-point-boundary
            //   indices to the containing code point.
            // For breakitereator::preceding only, these non-code-point indices need to be moved
            //   up to refer to the following codepoint.
            fText->next32();
            offset = fText->getIndex();
        }

        // TODO:  (synwee) would it be better to just check for being in the middle of a surrogate pair,
        //        rather than adjusting the position unconditionally?
        //        (Change would interact with safe rules.)
        // TODO:  change RBBI behavior for off-boundary indices to match that of UText?
        //        affects only preceding(), seems cleaner, but is slightly different.
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
    fText->setIndex(offset);
    int32_t  backOne = fText->move32(-1, CharacterIterator::kCurrent);
    UBool    result  = following(backOne) == offset;
    return result;
}

/**
 * Returns the current iteration position.
 * @return The current iteration position.
 */
int32_t RuleBasedBreakIterator::current(void) const {
    return (fText != NULL) ? fText->getIndex() : (int32_t)BreakIterator::DONE;
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
    int32_t             state;
    int16_t             category        = 0;
    RBBIStateTableRow  *row;
    UChar32 c;
    int32_t             lookaheadStatus = 0;
    int32_t             lookaheadTagIdx = 0;
    int32_t             result          = 0;
    int32_t             initialPosition = 0;
    int32_t             lookaheadResult = 0;
    int32_t             endCount        = 0;
    UBool               lookAheadHardBreak = (statetable->fFlags & RBBI_LOOKAHEAD_HARD_BREAK) != 0;

    if (fTrace) {
        RBBIDebugPuts("Handle Next   pos   char  state category");
    }

    // No matter what, handleNext alway correctly sets the break tag value.
    fLastStatusIndexValid = TRUE;
    fLastRuleStatusIndex = 0;

    // if we're already at the end of the text, return DONE.
    if (fText == NULL || fData == NULL || fText->hasNext() == FALSE) {
        return BreakIterator::DONE;
    }

    //  Set up the starting char.
    initialPosition = fText->getIndex();
    result          = initialPosition;
    c               = fText->current32();

    //  Set the initial state for the state machine
    state = START_STATE;
    row = (RBBIStateTableRow *)
            (statetable->fTableData + (statetable->fRowLen * state));
    category = (statetable->fFlags & RBBI_BOF_REQUIRED)?  2 : 3;


    // loop until we reach the end of the text or transition to state 0
    //
    for (;;) {
        if (c == CharacterIterator::DONE && fText->hasNext()==FALSE) {
            // Reached end of input string.
            //    Note: CharacterIterator::DONE is 0xffff, which is also a legal
            //          character value.  Check for DONE first, because it's quicker,
            //          but also need to check fText->hasNext() to be certain.
            if (endCount++ >= 1) {
                // We have already run the loop one last time with the 
                //   character set to the psueudo {eof} value.  Now it is time
                //   to unconditionally bail out.
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
                }
                break;
            }
            // Run the loop one last time with the fake end-of-input character category.
            category = 1;
        }

        //
        // Get the char category.  An incoming category of 1 or 2 means that
        //      we are preset for doing the beginning or end of input, and
        //      that we shouldn't get a category from an actual text input character.
        //
        if (category >= 3) {
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
        }

        #ifdef RBBI_DEBUG
            if (fTrace) {
                RBBIDebugPrintf("             %4d   ", fText->getIndex());
                if (0x20<=c && c<0x7f) {
                    RBBIDebugPrintf("\"%c\"  ", c);
                } else {
                    RBBIDebugPrintf("%5x  ", c);
                }
                RBBIDebugPrintf("%3d  %3d\n", state, category);
            }
        #endif

        // State Transition - move machine to its next state
        //
        state = row->fNextState[category];
        row = (RBBIStateTableRow *)
            (statetable->fTableData + (statetable->fRowLen * state));

        // Advance to the next character.  
        // If this is a beginning-of-input loop iteration, don't advance
        //    the input position.  The next iteration will be processing the
        //    first real input character.
        if (category != 2) {
            c = fText->next32();
        }
        category = 3;  // Flag that we aren't at the start of input.
                       // Exact category doesn't matter, so long as it's >=3.


        if (row->fAccepting == -1) {
            // Match found, common case.
            result = fText->getIndex();
            fLastRuleStatusIndex = row->fTagIdx;   // Remember the break status (tag) values.
        }

        if (row->fLookAhead != 0) {
            if (lookaheadStatus != 0
                && row->fAccepting == lookaheadStatus) {
                // Lookahead match is completed.  
                result               = lookaheadResult;
                fLastRuleStatusIndex = lookaheadTagIdx;
                lookaheadStatus      = 0;
                // TODO:  make a standalone hard break in a rule work.
                if (lookAheadHardBreak) {
                    fText->setIndex(result);
                    return result;
                }
                // Look-ahead completed, but other rules may match further.  Continue on
                //  TODO:  junk this feature?  I don't think it's used anywhwere.
                goto continueOn;
            }

            int32_t  r = fText->getIndex();
            lookaheadResult = r;
            lookaheadStatus = row->fLookAhead;
            lookaheadTagIdx = row->fTagIdx;
            goto continueOn;
        }


        if (row->fAccepting != 0) {
            // Because this is an accepting state, any in-progress look-ahead match
            //   is no longer relavant.  Clear out the pending lookahead status.
            lookaheadStatus = 0;           // clear out any pending look-ahead match.
        }

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
    #ifdef RBBI_DEBUG
        if (fTrace) {
            RBBIDebugPrintf("result = %d\n\n", result);
        }
    #endif
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
    int32_t            result          = fText->getIndex();
    int32_t            lookaheadStatus = 0;
    int32_t            lookaheadResult = 0;
    int32_t            lookaheadTagIdx = 0;
    UChar32            c               = fText->current32();
    UBool              doingBOF        = (fData->fReverseTable->fFlags & RBBI_BOF_REQUIRED) != 0;
    RBBIStateTableRow *row;


    //
    // Initial (startup) state tble row
    //
    row = (RBBIStateTableRow *)
        (this->fData->fReverseTable->fTableData + (state * fData->fReverseTable->fRowLen));

    //
    // Initial char category (state table column).
    //   If this table required a beginning-of-input test,
    //     hardwire to column 2
    //   otherwise do the normal char category thing.
    //
    if (doingBOF) {
        // The rules included a test for being at the start {bof}, which
        //   requires that we start the run with an extra iteration
        //   of the state machine with the reserved character category of 2.
        category = 2;
    } else {
        UTRIE_GET16(&fData->fTrie, c, category);
        if ((category & 0x4000) != 0)  {
            fDictionaryCharCount++;
            category &= ~0x4000;
        }
    }

    if (fTrace) {
        RBBIDebugPuts("Handle Prev   pos   char  state category");
    }

    // loop until we reach the beginning of the text or transition to state 0
    for (;;) {
        if (c == CharacterIterator::DONE && fText->hasPrevious()==FALSE) {
            break;
        }


        #ifdef RBBI_DEBUG
            if (fTrace) {
                RBBIDebugPrintf("             %4d   ", fText->getIndex());
                if (0x20<=c && c<0x7f) {
                    RBBIDebugPrintf("\"%c\"  ", c);
                } else {
                    RBBIDebugPrintf("%5x  ", c);
                }
                RBBIDebugPrintf("%3d  %3d\n", state, category);
            }
        #endif

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
        if (doingBOF == FALSE) {
            c = fText->previous32();
        }
        doingBOF = FALSE;
        UTRIE_GET16(&fData->fTrie, c, category);
        U_ASSERT(category>=2);

        // Check the dictionary bit in the character's category.
        //    Counter is only used by dictionary based iterators.
        //
        if ((category & 0x4000) != 0)  {
            fDictionaryCharCount++;
            category &= ~0x4000;
        }
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
//      Iterate backwards, according to the logic of the reverse rules.
//      This version handles the exact style backwards rules.
//
//      The logic of this function is very similar to handleNext(), above.
//
//-----------------------------------------------------------------------------------
int32_t RuleBasedBreakIterator::handlePrevious(const RBBIStateTable *statetable) {
    int32_t             state;
    int16_t             category        = 0;
    RBBIStateTableRow  *row;
    UChar32 c;
    int32_t             lookaheadStatus = 0;
    int32_t             result          = 0;
    int32_t             initialPosition = 0;
    int32_t             lookaheadResult = 0;
    int32_t             endCount        = 0;
    UBool               lookAheadHardBreak = (statetable->fFlags & RBBI_LOOKAHEAD_HARD_BREAK) != 0;

    if (fTrace) {
        RBBIDebugPuts("Handle Previous   pos   char  state category");
    }

    // handlePrevious() never gets the rule status.
    // Flag the status as invalid; if the user ever asks for status, we will need
    // to back up, then re-find the break position using handleNext(), which does
    // get the status value.
    fLastStatusIndexValid = FALSE;
    fLastRuleStatusIndex = 0;

    // if we're already at the start of the text, return DONE.
    if (fText == NULL || fData == NULL || fText->hasPrevious() == FALSE) {
        return BreakIterator::DONE;
    }

    //  Set up the starting char.
    initialPosition = fText->getIndex();
    result          = initialPosition;
    c               = fText->previous32();

    //  Set the initial state for the state machine
    state = START_STATE;
    row = (RBBIStateTableRow *)
            (statetable->fTableData + (statetable->fRowLen * state));
    category = (statetable->fFlags & RBBI_BOF_REQUIRED)?  2 : 3;


    // loop until we reach the start of the text or transition to state 0
    //
    for (;;) {
        if (c == CharacterIterator::DONE && fText->hasPrevious()==FALSE) {
            // Reached end of input string.
            //    Note: CharacterIterator::DONE is 0xffff, which is also a legal
            //          character value.  Check for DONE first, because it's quicker,
            //          but also need to check fText->hasNext() to be certain.
            if (endCount++ >= 1 || 
                *(int32_t *)fData->fHeader->fFormatVersion == 1 ) {
                // We have already run the loop one last time with the 
                //   character set to the psueudo {eof} value.  Now it is time
                //   to unconditionally bail out.
                //  (Or we have an old format binary rule file that does not support {eof}.)
                if (lookaheadResult < result) {
                    // We ran off the end of the string with a pending look-ahead match.
                    // Treat this as if the look-ahead condition had been met, and return
                    //  the match at the / position from the look-ahead rule.
                    result               = lookaheadResult;
                    lookaheadStatus = 0;
                } else if (result == initialPosition) {
                    // Ran off start, no match found.
                    // move one index one (towards the start, since we are doing a previous())
                    fText->setIndex(initialPosition);
                    fText->previous32();   // TODO:  shouldn't be necessary.  We're already at beginning.  Check.
                }
                break;
            }
            // Run the loop one last time with the fake end-of-input character category.
            category = 1;
        }

        //
        // Get the char category.  An incoming category of 1 or 2 means that
        //      we are preset for doing the beginning or end of input, and
        //      that we shouldn't get a category from an actual text input character.
        //
        if (category >= 3) {
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
        }

        #ifdef RBBI_DEBUG
            if (fTrace) {
                RBBIDebugPrintf("             %4d   ", fText->getIndex());
                if (0x20<=c && c<0x7f) {
                    RBBIDebugPrintf("\"%c\"  ", c);
                } else {
                    RBBIDebugPrintf("%5x  ", c);
                }
                RBBIDebugPrintf("%3d  %3d\n", state, category);
            }
        #endif

        // State Transition - move machine to its next state
        //
        state = row->fNextState[category];
        row = (RBBIStateTableRow *)
            (statetable->fTableData + (statetable->fRowLen * state));

        if (row->fAccepting == -1) {
            // Match found, common case.
            result = fText->getIndex();
        }

        if (row->fLookAhead != 0) {
            if (lookaheadStatus != 0
                && row->fAccepting == lookaheadStatus) {
                // Lookahead match is completed.  
                result               = lookaheadResult;
                lookaheadStatus      = 0;
                // TODO:  make a standalone hard break in a rule work.
                if (lookAheadHardBreak) {
                    fText->setIndex(result);
                    return result;
                }
                // Look-ahead completed, but other rules may match further.  Continue on
                //  TODO:  junk this feature?  I don't think it's used anywhwere.
                goto continueOn;
            }

            int32_t  r = fText->getIndex();
            lookaheadResult = r;
            lookaheadStatus = row->fLookAhead;
            goto continueOn;
        }


        if (row->fAccepting != 0) {
            // Because this is an accepting state, any in-progress look-ahead match
            //   is no longer relavant.  Clear out the pending lookahead status.
            lookaheadStatus = 0;           // clear out any pending look-ahead match.
        }

continueOn:
        if (state == STOP_STATE) {
            // This is the normal exit from the lookup state machine.
            // We have advanced through the string until it is certain that no
            //   longer match is possible, no matter what characters follow.
            break;
        }

        // Move (backwards) to the next character to process.  
        // If this is a beginning-of-input loop iteration, don't advance
        //    the input position.  The next iteration will be processing the
        //    first real input character.
        if (category != 2) {
            c = fText->previous32();
        }
        category = 3;  // Flag that this is no longer the first loop iteration.
                        // Exact category doesn't matter, so long as it's >=3.


    }

    // The state machine is done.  Check whether it found a match...

    // If the iterator failed to advance in the match engine, force it ahead by one.
    //   (This really indicates a defect in the break rules.  They should always match
    //    at least one character.)
    if (result == initialPosition) {
        result = fText->setIndex(initialPosition);
        fText ->previous32();
        result = fText->getIndex();
    }

    // Leave the iterator at our result position.
    fText->setIndex(result);
    #ifdef RBBI_DEBUG
        if (fTrace) {
            RBBIDebugPrintf("result = %d\n\n", result);
        }
    #endif
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
    // TODO:  Status tags are broken for DictionaryBasedBreakIterator.  Bug 4730.
    if (this->getDynamicClassID() == RuleBasedBreakIterator::getStaticClassID()) {
        U_ASSERT(fLastStatusIndexValid == TRUE);
        U_ASSERT(fLastRuleStatusIndex >= 0  &&  fLastRuleStatusIndex < fData->fStatusMaxIdx);
    }
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


//-------------------------------------------------------------------------------
//
//  UText functions      As a temporary implementation, create a type of CharacterIterator
//                       that works over UText, and let the RBBI engine continue to
//                       work on CharacterIterator, which it always has.
//
//                       The permanent solution is to rework the RBBI engine to use
//                       UText directly, which will be more efficient for all input
//                       sources.
//
//                       This CharacterIterator implementation over UText is not complete,
//                       it has only what is needed for RBBI, and is not intended
//                       to ever become public.
//
//-------------------------------------------------------------------------------

class CharacterIteratorUT: public CharacterIterator {
public:
    CharacterIteratorUT(UText  *ut);
    virtual ~CharacterIteratorUT();

    virtual CharacterIterator *clone() const;
    virtual UBool       operator==(const ForwardCharacterIterator& that) const;
    virtual UChar       setIndex(int32_t position);
    virtual UChar32     previous32(void);
    virtual UChar32     next32(void);
    virtual UBool       hasNext();
    virtual UChar32     current32(void) const;
    virtual UBool       hasPrevious();
    virtual int32_t     move(int32_t delta, EOrigin origin);
    static  UClassID    getStaticClassID(void);
    virtual UClassID    getDynamicClassID(void) const;

    UText   *fUText;
    virtual void        resetTo(const UText *ut, UErrorCode *status);

private:
    CharacterIteratorUT();

    // The following functions are not needed by RBBI,
    //   but are pure virtual in CharacterIterator, so must be defined.
    //   Only stubs are provided in this implementation.
    virtual int32_t       hashCode(void) const                   {U_ASSERT(FALSE); return 0;};
    virtual UChar         nextPostInc(void)                      {U_ASSERT(FALSE); return 0;};
    virtual UChar32       next32PostInc(void)                    {U_ASSERT(FALSE); return 0;};
    virtual UChar         first(void)                            {U_ASSERT(FALSE); return 0;};
    virtual UChar32       first32(void)                          {U_ASSERT(FALSE); return 0;};
    virtual UChar         last(void)                             {U_ASSERT(FALSE); return 0;};
    virtual UChar32       last32(void)                           {U_ASSERT(FALSE); return 0;};
    virtual UChar32       setIndex32(int32_t)                    {U_ASSERT(FALSE); return 0;};
    virtual UChar         current(void) const                    {U_ASSERT(FALSE); return 0;};
    virtual UChar         next(void)                             {U_ASSERT(FALSE); return 0;};
    virtual UChar         previous(void)                         {U_ASSERT(FALSE); return 0;};
    virtual int32_t       move32(int32_t, EOrigin)               {U_ASSERT(FALSE); return 0;};
    virtual void          getText(UnicodeString        &)        {U_ASSERT(FALSE);};
};



//
//  The following fields are inherited from CharacterIterator.
//  This implementation __MUST__ keep them current because of non-virtual inline
//  functions defined in CharacterIterator.
//    int32_t  textLength;       // length of the text.
//    int32_t  pos;              // current index position
//    int32_t  begin;            // starting index.  Always 0 for us.
//    int32_t  end;              // ending index
//
// CharacterIterator was designed assuming that utf-16 indexing would be used,
// but native indexing will pass through OK.  This partial implementation only
// provides the '32' flavored code point access, not UChar access.
//

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(CharacterIteratorUT)

CharacterIteratorUT::CharacterIteratorUT(UText *ut) {
    UErrorCode status = U_ZERO_ERROR;
    fUText = utext_clone(NULL, ut, FALSE, &status);
    
    // Set the inherited CharacterItertor fields
    textLength = utext_nativeLength(ut);
    pos = 0;
    begin = 0;
    end = textLength;
}

CharacterIteratorUT::CharacterIteratorUT() {
    fUText     = NULL;
    textLength = 0;
    pos        = 0;
    begin      = 0;
    end        = 0;
}

CharacterIteratorUT::~CharacterIteratorUT() {
    utext_close(fUText);
}


CharacterIterator *CharacterIteratorUT::clone() const {
    UErrorCode status = U_ZERO_ERROR;
    CharacterIteratorUT *result = new  CharacterIteratorUT();
    result->fUText = utext_clone(NULL, fUText, TRUE, &status);
    if (U_SUCCESS(status)) {
        result->textLength = utext_nativeLength(fUText);
        result->pos = 0;
        result->begin = 0;
        result->end = textLength;
    }
    return result;
}

UBool CharacterIteratorUT::operator==(const ForwardCharacterIterator& that) const {
    if (this->getDynamicClassID() != that.getDynamicClassID()) {
        return FALSE;
    }
    const CharacterIteratorUT *realThat = (const CharacterIteratorUT *)&that;
    UBool result = this->fUText->context == realThat->fUText->context;
    return result;
}

UChar CharacterIteratorUT::setIndex(int32_t position) {
    pos = position;
    if (pos < 0) {
        pos = 0;
    } else if (pos > end) {
        pos = end;
    }
    utext_setNativeIndex(fUText, pos);
    pos = utext_getNativeIndex(fUText);  // because utext snaps to code point boundary.
    return 0x0000ffff;  // RBBI doesn't use return value, and UText can't return a UChar easily.
}

UChar32 CharacterIteratorUT::previous32(void) {
    UChar32 result = UTEXT_PREVIOUS32(fUText);
    pos = utext_getNativeIndex(fUText);  // TODO:  maybe optimize common case?
    if (result < 0) {
        result = 0x0000ffff;
    }
    return result;
}

UChar32 CharacterIteratorUT::next32(void) {
    // TODO: optimize.
    UTEXT_NEXT32(fUText);
    pos = utext_getNativeIndex(fUText);
    UChar32 result = UTEXT_NEXT32(fUText);
    if (result < 0) {
        result = 0x0000ffff;
    } else {
        UTEXT_PREVIOUS32(fUText);
    }
    return result;
}

UBool CharacterIteratorUT::hasNext() {
    // What would really be best for RBBI is a hasNext32()
    UBool result = TRUE;
    if (pos >= end) { 
        result  = FALSE;
    }
    return result;
}

UChar32 CharacterIteratorUT::current32(void) const {
    UChar32 result = utext_current32(fUText);
    if (result < 0) {
        result = 0x0000ffff;
    }
    return result;
}

UBool CharacterIteratorUT::hasPrevious() {
    UBool result = pos > 0;
    return result;
}

int32_t CharacterIteratorUT::move(int32_t delta, EOrigin origin) {
    // only needed for the inherited inline implementation of setToStart().
    int32_t result = pos;
    switch (origin) {
case kStart:
    result = delta;
    break;
case kCurrent: 
    result = pos + delta;
    break;
case kEnd:
    result = end + delta;
    break;
default:
    U_ASSERT(FALSE);
    }
    utext_setNativeIndex(fUText, result);
    pos = utext_getNativeIndex(fUText);  // align to cp boundary
    return result;
}



void  CharacterIteratorUT::resetTo(const UText *ut, UErrorCode *status) {
    // Reset this CharacterIteratorUT to use a new UText.
    fUText = utext_clone(fUText, ut, FALSE, status);
    utext_setNativeIndex(fUText, 0);
    textLength = utext_nativeLength(fUText);
    pos = 0;
    end = textLength;
}

void RuleBasedBreakIterator::setText(UText *ut, UErrorCode &status) {
    if (U_FAILURE(status)) {
        return;
    }
    reset();
    if (fText != NULL && 
        fText->getDynamicClassID() == CharacterIteratorUT::getStaticClassID()) 
    {
        // The break iterator is already using a UText based character iterator.
        //  Copy the new UText into the existing character iterator's UText.
        CharacterIteratorUT *utcr = (CharacterIteratorUT *)fText;
        utcr->resetTo(ut, &status);
    } else {
        delete fText;
        fText = new CharacterIteratorUT(ut);
    }
    this->first();
}


UText *RuleBasedBreakIterator::getUText(UText *fillIn, UErrorCode &status) const {
    UText *result = NULL;
    if (U_SUCCESS(status) && fText!=NULL && 
        fText->getDynamicClassID() == CharacterIteratorUT::getStaticClassID()) 
    {
        CharacterIteratorUT *utcr = (CharacterIteratorUT *)fText;
        result = utext_clone(fillIn, utcr->fUText, FALSE, &status);
    }
    return result;
}


U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */
