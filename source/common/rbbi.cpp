//
//  file:  rbbi.c    Contains the implementation of the rule based break iterator
//                   runtime engine and the API implementation for
//                   class RuleBasedBreakIterator
//
/*
***************************************************************************
*   Copyright (C) 1999-2003 International Business Machines Corporation   *
*   and others. All rights reserved.                                      *
***************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/rbbi.h"
#include "unicode/schriter.h"
#include "unicode/udata.h"
#include "rbbidata.h"
#include "rbbirb.h"
#include "cmemory.h"
#include "cstring.h"

#include "uassert.h"

U_NAMESPACE_BEGIN


static const int16_t START_STATE = 1;     // The state number of the starting state
static const int16_t STOP_STATE  = 0;     // The state-transition value indicating "stop"

/**
 * Class ID.  (value is irrelevant; address is important)
 */
const char
RuleBasedBreakIterator::fgClassID = 0;


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
    if (U_FAILURE(status)) {return;}
    fData = new RBBIDataWrapper(data, status);
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
    if (U_FAILURE(status)) {return;}
    fData = new RBBIDataWrapper(udm, status);
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

    fText                = NULL;
    fData                = NULL;
    fCharMappings        = NULL;
    fLastBreakTag        = 0;
    fLastBreakTagValid   = TRUE;
    fDictionaryCharCount = 0;

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
    fLastBreakTag      = 0;
    fLastBreakTagValid = TRUE;
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
        fLastBreakTag      = 0;
        fLastBreakTagValid = TRUE;
        return BreakIterator::DONE;
    }

    // I'm not sure why, but t.last() returns the offset of the last character,
    // rather than the past-the-end offset
    //
    //   (It's so a loop like for(p=it.last(); p!=DONE; p=it.previous()) ...
    //     will work correctly.)


    fLastBreakTagValid = FALSE;
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
        fLastBreakTag      = 0;
        fLastBreakTagValid = TRUE;
        return BreakIterator::DONE;
    }

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
        lastTag        = fLastBreakTag;
        breakTagValid  = TRUE;
    }

    // fLastBreakTag wants to have the value for section of text preceding
    // the result position that we are to return (in lastResult.)  If
    // the backwards rules overshot and the above loop had to do two or more
    //  handleNext()s to move up to the desired return position, we will have a valid
    //  tag value.  But, if handlePrevious() took us to exactly the correct result positon,
    //  we wont have a tag value for that position, which is only set by handleNext().


    // set the current iteration position to be the last break position
    // before where we started, and then return that value
    fText->setIndex(lastResult);
    fLastBreakTag      = lastTag;       // for use by getRuleStatus()
    fLastBreakTagValid = breakTagValid;
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
    fLastBreakTag = 0;
    fLastBreakTagValid = TRUE;
    if (fText == NULL || offset >= fText->endIndex()) {
        // fText->setToEnd();
        // return BreakIterator::DONE;
        last();
        return next();
    }
    else if (offset < fText->startIndex()) {
        // fText->setToStart();
        // return fText->startIndex();
        return first();
    }

    // otherwise, set our internal iteration position (temporarily)
    // to the position passed in.  If this is the _beginning_ position,
    // then we can just use next() to get our return value
    fText->setIndex(offset);
    if (offset == fText->startIndex())
        return handleNext();

    // otherwise, we have to sync up first.  Use handlePrevious() to back
    // us up to a known break position before the specified position (if
    // we can determine that the specified position is a break position,
    // we don't back up at all).  This may or may not be the last break
    // position at or before our starting position.  Advance forward
    // from here until we've passed the starting position.  The position
    // we stop on will be the first break position after the specified one.

    int32_t result = previous();
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
//     value every time the state machine passes through a possible end state.
//
//-----------------------------------------------------------------------------------
int32_t RuleBasedBreakIterator::handleNext(void) {
    if (fTrace) {
        RBBIDebugPrintf("Handle Next   pos   char  state category  \n");
    }

    // No matter what, handleNext alway correctly sets the break tag value.
    fLastBreakTagValid = TRUE;

    // if we're already at the end of the text, return DONE.
    if (fText == NULL || fData == NULL || fText->getIndex() == fText->endIndex()) {
        fLastBreakTag = 0;
        return BreakIterator::DONE;
    }

    // no matter what, we always advance at least one character forward
    int32_t temp = fText->getIndex();
    fText->next32();
    int32_t result = fText->getIndex();
    fText->setIndex(temp);

    int32_t lookaheadResult = 0;

    // Initialize the state machine.  Begin in state 1
    int32_t            state           = START_STATE;
    int16_t            category;
    UChar32            c               = fText->current32();
    RBBIStateTableRow *row;
    int32_t            lookaheadStatus = 0;
    int32_t            lookaheadTag    = 0;

    fLastBreakTag = 0;

    row = (RBBIStateTableRow *)    // Point to starting row of state table.
        (fData->fForwardTable->fTableData + (fData->fForwardTable->fRowLen * state));

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
            // Note: CharacterIterator::DONE is 0xffff, which is also a legal
            //       character value.  Check for DONE first, because it's quicker,
            //       but also need to check fText->hasNext() to be certain.
            break;
        }
        // look up the current character's character category, which tells us
        // which column in the state table to look at.
        // Note:  the 16 in UTRIE_GET16 refers to the size of the data being returned,
        //        not the size of the character going in.
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
            (fData->fForwardTable->fTableData + (fData->fForwardTable->fRowLen * state));

        // Get the next character.  Doing it here positions the iterator
        //    to the correct position for recording matches in the code that
        //    follows.
        //  TODO:  16 bit next, and a 16 bit TRIE lookup, with escape code
        //         for non-BMP chars, would be faster.
        c = fText->next32();

        if (row->fAccepting == 0 && row->fLookAhead == 0) {
            // No match, nothing of interest happening, common case.
            goto continueOn;
        }

        if (row->fAccepting == -1) {
            // Match found, common case, no lookahead involved.
            //    (It's possible that some lookahead rule matched here also,
            //     but since there's an unconditional match, we'll favor that.)
            result          = fText->getIndex();
            lookaheadStatus = 0;           // clear out any pending look-ahead matches.
            fLastBreakTag   = row->fTag;   // Remember the break status (tag) value.
            goto continueOn;
        }

        if (row->fAccepting == 0 && row->fLookAhead != 0) {
            // Lookahead match point.  Remember it, but only if no other rule has
            //                         unconitionally matched up to this point.
            // TODO:  handle case where there's a pending match from a different rule -
            //        where lookaheadStatus != 0  && lookaheadStatus != row->fLookAhead.
            int32_t  r = fText->getIndex();
            if (r > result) {
                lookaheadResult = r;
                lookaheadStatus = row->fLookAhead;
                lookaheadTag   = row->fTag;
            }
            goto continueOn;
        }

        if (row->fAccepting != 0 && row->fLookAhead != 0) {
            // Lookahead match is completed.  Set the result accordingly, but only
            //   if no other rule has matched further in the mean time.
            if (lookaheadResult > result) {
                U_ASSERT(row->fAccepting == lookaheadStatus);   // TODO:  handle this case
                //    of overlapping lookahead matches.
                result          = lookaheadResult;
                fLastBreakTag   = lookaheadTag;
                lookaheadStatus = 0;
            }
            goto continueOn;
        }

continueOn:
        if (state == STOP_STATE) {
            break;
        }

        // c = fText->next32();
    }

    // if we've run off the end of the text, and the very last character took us into
    // a lookahead state, advance the break position to the lookahead position
    // (the theory here is that if there are no characters at all after the lookahead
    // position, that always matches the lookahead criteria)
    //   TODO:  is this really the right behavior?
    if (c == CharacterIterator::DONE &&
        fText->hasNext()==FALSE &&
        lookaheadResult == fText->endIndex()) {
            result          = lookaheadResult;
            fLastBreakTag   = lookaheadTag;
    }


    fText->setIndex(result);
    if (fTrace) {
        RBBIDebugPrintf("result = %d\n\n", result);
    }
    return result;
}

//-----------------------------------------------------------------------------------
//
//  handlePrevious()
//
//      This method backs the iterator back up to a "safe position" in the text.
//      This is a position that we know, without any context, must be a break position.
//      The various calling methods then iterate forward from this safe position to
//      the appropriate position to return.
//
//      The logic of this function is very similar to handleNext(), above.
//
//-----------------------------------------------------------------------------------
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
    int32_t            lookaheadTag    = 0;
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
                lookaheadTag    = row->fTag;
            }
            goto continueOn;
        }

        if (row->fAccepting != 0 && row->fLookAhead != 0) {
            // Lookahead match is completed.  Set the result accordingly, but only
            //   if no other rule has matched further in the mean time.
            if (lookaheadResult > result) {
                U_ASSERT(row->fAccepting == lookaheadStatus);   // TODO:  handle this case
                //    of overlapping lookahead matches.
                result          = lookaheadResult;
                fLastBreakTag   = lookaheadTag;
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
int32_t  RuleBasedBreakIterator::getRuleStatus() const {
    RuleBasedBreakIterator *nonConstThis  = (RuleBasedBreakIterator *)this;
    if (fLastBreakTagValid == FALSE) {
        //  No cached status is available.
        if (fText == NULL || current() == fText->startIndex()) {
            //  At start of text, or there is no text.  Status is always zero.
            nonConstThis->fLastBreakTag = 0;
            nonConstThis->fLastBreakTagValid = TRUE;
        } else {
            //  Not at start of text.  Find status the tedious way.
            int32_t pa = current();
            nonConstThis->previous();
            int32_t pb = nonConstThis->next();
            U_ASSERT(pa == pb);
        }
    }
    return nonConstThis->fLastBreakTag;
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
