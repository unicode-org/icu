/*
**********************************************************************
*   Copyright (C) 1999 IBM Corp. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   11/11/99    rgillam     Complete port from Java.
**********************************************************************
*/

#ifndef RBBI_TBL_H
#define RBBI_TBL_H

#include "ucmp8.h"
#include "utypes.h"
#include "unistr.h"
#include "unicode/brkiter.h"
#include "filestrm.h"

/**
 * This class contains the internal static tables that are used by the
 * RuleBasedBreakIterator.  Once created, these tables are immutable,
 * so they can be shared among all break iterators using a particular
 * set of rules.  This class uses a reference-counting scheme to
 * manage the sharing.
 *
 * @author Richard Gillam
 */
class RuleBasedBreakIteratorTables {

private:
    /**
     * The number of RuleBasedBreakIterators using this object.
     */
    int16_t refCount;

protected:
    /**
     * Whether or not we own the storage for the tables (the tables may be
     * stored in a memory-mapped file)
     */
    bool_t ownTables;

private:
    /**
     * The textual description that was used to create these tables
     */
    UnicodeString description;

    /**
     * A table that indexes from character values to character category numbers
     */
    CompactByteArray* charCategoryTable;

    /**
     * The table of state transitions used for forward iteration
     */
    int16_t* stateTable;

    /**
     * The table of state transitions used to sync up the iterator with the
     * text in backwards and random-access iteration
     */
    int16_t* backwardsStateTable;

    /**
     * A list of flags indicating which states in the state table are accepting
     * ("end") states
     */
    int8_t* endStates;

    /**
     * A list of flags indicating which states in the state table are
     * lookahead states (states which turn lookahead on and off)
     */
    int8_t* lookaheadStates;

    /**
     * The number of character categories (and, thus, the number of columns in
     * the state tables)
     */
    int32_t numCategories;

    //=======================================================================
    // constructor
    //=======================================================================

    /**
     * Creates a tables object, adopting all of the tables that are passed in.
     */
protected:
    RuleBasedBreakIteratorTables();
    
    RuleBasedBreakIteratorTables(const void* image);

private:
    /**
     * The copy constructor is declared private and is a no-op.
     * THIS CLASS MAY NOT BE COPIED.
     */
    RuleBasedBreakIteratorTables(const RuleBasedBreakIteratorTables& that);

    //=======================================================================
    // boilerplate
    //=======================================================================

protected:
    /**
     * Destructor
     */
    virtual ~RuleBasedBreakIteratorTables();

private:
    /**
     * The assignment operator is declared private and is a no-op.
     * THIS CLASS MAY NOT BE COPIED.
     */
    RuleBasedBreakIteratorTables& operator=(const RuleBasedBreakIteratorTables& that);

    /**
     * Equality operator.  Returns TRUE if both tables objects are of the
     * same class, have the same behavior, and iterate over the same text.
     */
    virtual bool_t operator==(const RuleBasedBreakIteratorTables& that) const;

    /**
     * Not-equal operator.  If operator== returns TRUE, this returns FALSE,
     * and vice versa.
     */
    bool_t operator!=(const RuleBasedBreakIteratorTables& that) const;

    /**
     * Compute a hash code for these tables
     * @return A hash code
     */
    virtual int32_t hashCode() const;

    /**
     * Returns the description used to create these tables
     */
    const UnicodeString& getRules() const;

    //=======================================================================
    // reference counting
    //=======================================================================
    
    /**
     * increments the reference count.
     */
    void addReference();

    /**
     * decrements the reference count and deletes the object if it reaches zero
     */
    void removeReference();

protected:
    //=======================================================================
    // implementation
    //=======================================================================
    /**
     * Looks up a character's category (i.e., its category for breaking purposes,
     * not its Unicode category)
     */
    virtual int32_t lookupCategory(UChar c, BreakIterator* bi) const;

    /**
     * Given a current state and a character category, looks up the
     * next state to transition to in the state table.
     */
    virtual int32_t lookupState(int32_t state, int32_t category) const;

    /**
     * Given a current state and a character category, looks up the
     * next state to transition to in the backwards state table.
     */
    virtual int32_t lookupBackwardState(int32_t state, int32_t category) const;

    /**
     * Returns true if the specified state is an accepting state.
     */
    virtual bool_t isEndState(int32_t state) const;

    /**
     * Returns true if the specified state is a lookahead state.
     */
    virtual bool_t isLookaheadState(int32_t state) const;

    friend class RuleBasedBreakIterator;
    friend class DictionaryBasedBreakIterator;
};

inline bool_t
RuleBasedBreakIteratorTables::operator!=(const RuleBasedBreakIteratorTables& that) const {
    return !operator==(that);
}

inline const UnicodeString&
RuleBasedBreakIteratorTables::getRules() const {
    return description;
}

inline void
RuleBasedBreakIteratorTables::addReference() {
    ++refCount;
}

inline void
RuleBasedBreakIteratorTables::removeReference() {
    if (--refCount <= 0)
        delete this;
}

#endif
