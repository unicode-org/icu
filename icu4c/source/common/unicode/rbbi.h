/*
***************************************************************************
*   Copyright (C) 1999-2003 International Business Machines Corporation   *
*   and others. All rights reserved.                                      *
***************************************************************************

**********************************************************************
*   Date        Name        Description
*   10/22/99    alan        Creation.
*   11/11/99    rgillam     Complete port from Java.
**********************************************************************
*/

#ifndef RBBI_H
#define RBBI_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/brkiter.h"
#include "unicode/udata.h"
#include "unicode/parseerr.h"

struct UTrie;

U_NAMESPACE_BEGIN

struct RBBIDataHeader;
class  RuleBasedBreakIteratorTables;
class  BreakIterator;
class  RBBIDataWrapper;
struct RBBIStateTable;



/**
 * A subclass of BreakIterator whose behavior is specified using a list of rules.
 * <p>Instances of this class are most commonly created by the factory methods of
 *  BreakIterator::createWordInstance(), BreakIterator::createLineInstance(), etc.,
 *  and then used via the abstract API in class BreakIterator</p>
 *
 * <p>See the ICU User Guide for information on Break Iterator Rules.</p>
 *
 * <p>This class is not intended to be subclassed.  (Class DictionaryBasedBreakIterator
 *    is a subclass, but that relationship is effectively internal to the ICU 
 *    implementation.  The subclassing interface to RulesBasedBreakIterator is
 *    not part of the ICU API, and may not remain stable.</p>
 *
 */
class U_COMMON_API RuleBasedBreakIterator : public BreakIterator {

protected:
    /**
     * The character iterator through which this BreakIterator accesses the text
     * @internal
     */
    CharacterIterator*  fText;

    /**
     * The rule data for this BreakIterator instance
     * @internal
     */
    RBBIDataWrapper    *fData;

    /** Rule {tag} value for the most recent match. 
     *  @internal
    */
    int32_t             fLastBreakTag;

    /**
     * Rule tag value valid flag.
     * Some iterator operations don't intrinsically set the correct tag value.
     * This flag lets us lazily compute the value if we are ever asked for it.
     * @internal
     */
    UBool               fLastBreakTagValid;

    /**
     * Counter for the number of characters encountered with the "dictionary"
     *   flag set.  Normal RBBI iterators don't use it, although the code
     *   for updating it is live.  Dictionary Based break iterators (a subclass
     *   of us) access this field directly.
     * @internal
     */
    uint32_t           fDictionaryCharCount;

    /**
     * Debugging flag.  Trace operation of state machine when true.
     * @internal
     */
    static UBool        fTrace;


protected:
    //=======================================================================
    // constructors
    //=======================================================================

    /**
     * Constructor from a flattened set of RBBI data in malloced memory.
     *             RulesBasedBreakIterators built from a custom set of rules
     *             are created via this constructor; the rules are compiled
     *             into memory, then the break iterator is constructed here.
     *
     *             The break iterator adopts the memory, and will
     *             free it when done.
     * @internal
     */
    RuleBasedBreakIterator(RBBIDataHeader* data, UErrorCode &status);

    friend class RBBIRuleBuilder; /** @internal */
    friend class BreakIterator;



public:

    /** Default constructor.  Creates an empty shell of an iterator, with no
     *  rules or text to iterate over.   Object can subsequently be assigned to.
     *  @stable ICU 2.2
     */
    RuleBasedBreakIterator();

    /**
     * Copy constructor.  Will produce a break iterator with the same behavior,
     * and which iterates over the same text, as the one passed in.
     * @param that The RuleBasedBreakIterator passed to be copied
     * @stable ICU 2.0
     */
    RuleBasedBreakIterator(const RuleBasedBreakIterator& that);

    /**
     * Construct a RuleBasedBreakIterator from a set of rules supplied as a string.
     * @param rules The break rules to be used.
     * @param parseError  In the event of a syntax error in the rules, provides the location
     *                    within the rules of the problem.
     * @param status Information on any errors encountered.
     * @stable ICU 2.2
     */
    RuleBasedBreakIterator( const UnicodeString    &rules,
                             UParseError           &parseError,
                             UErrorCode            &status);


    /**
     * This constructor uses the udata interface to create a BreakIterator
     * whose internal tables live in a memory-mapped file.  "image" is an 
     * ICU UDataMemory handle for the pre-compiled break iterator tables.
     * @param image handle to the memory image for the break iterator data.
     *        Ownership of the UDataMemory handle passes to the Break Iterator,
     *        which will be responsible for closing it when it is no longer needed.
     * @param status Information on any errors encountered.
     * @see udata_open
     * @see #getBinaryRules
     * @draft ICU 2.8
     */
    RuleBasedBreakIterator(UDataMemory* image, UErrorCode &status);

    /**
     * Destructor
     *  @stable ICU 2.0
     */
    virtual ~RuleBasedBreakIterator();

    /**
     * Assignment operator.  Sets this iterator to have the same behavior,
     * and iterate over the same text, as the one passed in.
     * @param that The RuleBasedBreakItertor passed in
     * @return the newly created RuleBasedBreakIterator
     *  @stable ICU 2.0
     */
    RuleBasedBreakIterator& operator=(const RuleBasedBreakIterator& that);

    /**
     * Equality operator.  Returns TRUE if both BreakIterators are of the
     * same class, have the same behavior, and iterate over the same text.
     * @param that The BreakIterator to be compared for equality
     * @Return TRUE if both BreakIterators are of the
     * same class, have the same behavior, and iterate over the same text.
     *  @stable ICU 2.0
     */
    virtual UBool operator==(const BreakIterator& that) const;

    /**
     * Not-equal operator.  If operator== returns TRUE, this returns FALSE,
     * and vice versa.
     * @param that The BreakIterator to be compared for inequality
     * @return TRUE if both BreakIterators are not same.
     *  @stable ICU 2.0
     */
    UBool operator!=(const BreakIterator& that) const;

    /**
     * Returns a newly-constructed RuleBasedBreakIterator with the same
     * behavior, and iterating over the same text, as this one.
     * Differs from the copy constructor in that it is polymorphic, and
     *   will correctly clone (copy) a derived class.
     * clone() is thread safe.  Multiple threads may simultaeneously
     * clone the same source break iterator.
     *  @stable ICU 2.0
     */
    virtual BreakIterator* clone() const;

    /**
     * Compute a hash code for this BreakIterator
     * @return A hash code
     *  @stable ICU 2.0
     */
    virtual int32_t hashCode(void) const;

    /**
     * Returns the description used to create this iterator
     * @return the description used to create this iterator
     *  @stable ICU 2.0
     */
    virtual const UnicodeString& getRules(void) const;

    //=======================================================================
    // BreakIterator overrides
    //=======================================================================

    /**
     * Return a CharacterIterator over the text being analyzed.  This version
     * of this method returns the actual CharacterIterator we're using internally.
     * Changing the state of this iterator can have undefined consequences.  If
     * you need to change it, clone it first.
     * @return An iterator over the text being analyzed.
     *  @stable ICU 2.0
     */
    virtual const CharacterIterator& getText(void) const;


    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText An iterator over the text to analyze.  The BreakIterator
     * takes ownership of the character iterator.  The caller MUST NOT delete it!
     *  @stable ICU 2.0
     */
    virtual void adoptText(CharacterIterator* newText);

    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText The text to analyze.
     *  @stable ICU 2.0
     */
    virtual void setText(const UnicodeString& newText);

    /**
     * Sets the current iteration position to the beginning of the text.
     * (i.e., the CharacterIterator's starting offset).
     * @return The offset of the beginning of the text.
     *  @stable ICU 2.0
     */
    virtual int32_t first(void);

    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
     *  @stable ICU 2.0
     */
    virtual int32_t last(void);

    /**
     * Advances the iterator either forward or backward the specified number of steps.
     * Negative values move backward, and positive values move forward.  This is
     * equivalent to repeatedly calling next() or previous().
     * @param n The number of steps to move.  The sign indicates the direction
     * (negative is backwards, and positive is forwards).
     * @return The character offset of the boundary position n boundaries away from
     * the current one.
     *  @stable ICU 2.0
     */
    virtual int32_t next(int32_t n);

    /**
     * Advances the iterator to the next boundary position.
     * @return The position of the first boundary after this one.
     *  @stable ICU 2.0
     */
    virtual int32_t next(void);

    /**
     * Moves the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     *  @stable ICU 2.0
     */
    virtual int32_t previous(void);

    /**
     * Sets the iterator to refer to the first boundary position following
     * the specified position.
     * @param offset The position from which to begin searching for a break position.
     * @return The position of the first break after the current position.
     *  @stable ICU 2.0
     */
    virtual int32_t following(int32_t offset);

    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @param offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     *  @stable ICU 2.0
     */
    virtual int32_t preceding(int32_t offset);

    /**
     * Returns true if the specfied position is a boundary position.  As a side
     * effect, leaves the iterator pointing to the first boundary position at
     * or after "offset".
     * @param offset the offset to check.
     * @return True if "offset" is a boundary position.
     *  @stable ICU 2.0
     */
    virtual UBool isBoundary(int32_t offset);

    /**
     * Returns the current iteration position.
     * @return The current iteration position.
     * @stable ICU 2.0
     */
    virtual int32_t current(void) const;


    /**
     * Return the status tag from the break rule that determined the most recently
     * returned break position.  The values appear in the rule source
     * within brackets, {123}, for example.  For rules that do not specify a
     * status, a default value of 0 is returned.
     * <p>
     * Of the standard types of ICU break iterators, only the word break
     * iterator provides status values.  The values are defined in
     * <code>enum UWordBreak</code>, and allow distinguishing between words
     * that contain alphabetic letters, "words" that appear to be numbers,
     * punctuation and spaces, words containing ideographic characters, and
     * more.  Call <code>getRuleStatus</code> after obtaining a boundary
     * position from <code>next()<code>, <code>previous()</code>, or 
     * any other break iterator functions that returns a boundary position.
     * <p>
     * @return the status from the break rule that determined the most recently
     * returned break position.
     *
     * @see UWordBreak
     * @stable ICU 2.2
     */
    virtual int32_t getRuleStatus() const;

    /**
     * Returns a unique class ID POLYMORPHICALLY.  Pure virtual override.
     * This method is to implement a simple version of RTTI, since not all
     * C++ compilers support genuine RTTI.  Polymorphic operator==() and
     * clone() methods call this method.
     *
     * @return          The class ID for this object. All objects of a
     *                  given class have the same class ID.  Objects of
     *                  other classes have different class IDs.
     * @stable ICU 2.0
     */
    virtual UClassID getDynamicClassID(void) const;

    /**
     * Returns the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     *
     *      Base* polymorphic_pointer = createPolymorphicObject();
     *      if (polymorphic_pointer->getDynamicClassID() ==
     *          Derived::getStaticClassID()) ...
     *
     * @return          The class ID for all objects of this class.
     * @stable ICU 2.0
     */
    static UClassID getStaticClassID(void);

    /*
     * Create a clone (copy) of this break iterator in memory provided
     *  by the caller.  The idea is to increase performance by avoiding
     *  a storage allocation.  Use of this functoin is NOT RECOMMENDED.
     *  Performance gains are minimal, and correct buffer management is
     *  tricky.  Use clone() instead.
     *
     * @param stackBuffer  The pointer to the memory into which the cloned object
     *                     should be placed.  If NULL,  allocate heap memory
     *                     for the cloned object.
     * @param BufferSize   The size of the buffer.  If zero, return the required
     *                     buffer size, but do not clone the object.  If the
     *                     size was too small (but not zero), allocate heap
     *                     storage for the cloned object.
     *
     * @param status       Error status.  U_SAFECLONE_ALLOCATED_WARNING will be
     *                     returned if the the provided buffer was too small, and
     *                     the clone was therefore put on the heap.
     *
     * @return  Pointer to the clone object.  This may differ from the stackBuffer
     *          address if the byte alignment of the stack buffer was not suitable
     *          or if the stackBuffer was too small to hold the clone.
     * @stable ICU 2.0
     */
    virtual BreakIterator *  createBufferClone(void *stackBuffer,
                                               int32_t &BufferSize,
                                               UErrorCode &status);


    /**
     * Return the binary form of compiled break rules,
     * which can then be used to create a new break iterator at some
     * time in the future.  Creating a break iterator from pre-compiled rules
     * is much faster than building one from the source form of the
     * break rules.
     *
     * The binary data can only be used with the same version of ICU
     *  and on the same platform type (processor endian-ness)
     *
     * @param length Returns the length of the binary data.  (Out paramter.)
     *
     * @return   A pointer to the binary (compiled) rule data.  The storage
     *           belongs to the RulesBasedBreakIterator object, not the
     *           caller, and must not be modified or deleted.
     * @internal
     */
    virtual const uint8_t *getBinaryRules(uint32_t &length);


protected:
    //=======================================================================
    // implementation
    //=======================================================================
    /**
     * This method is the actual implementation of the next() method.  All iteration
     * vectors through here.  This method initializes the state machine to state 1
     * and advances through the text character by character until we reach the end
     * of the text or the state machine transitions to state 0.  We update our return
     * value every time the state machine passes through a possible end state.
     * @internal
     */
    virtual int32_t handleNext(void);

    /**
     * This method backs the iterator back up to a "safe position" in the text.
     * This is a position that we know, without any context, must be a break position.
     * The various calling methods then iterate forward from this safe position to
     * the appropriate position to return.  (For more information, see the description
     * of buildBackwardsStateTable() in RuleBasedBreakIterator.Builder.)
     * @internal
     */
    virtual int32_t handlePrevious(void);

    /**
     * Dumps caches and performs other actions associated with a complete change
     * in text or iteration position.  This function is a no-op in RuleBasedBreakIterator,
     * but subclasses can and do override it.
     * @internal
     */
    virtual void reset(void);

    /**
      * Return true if the category lookup for this char
      * indicates that it is in the set of dictionary lookup chars.
      * This function is intended for use by dictionary based break iterators.
      * @return true if the category lookup for this char
      * indicates that it is in the set of dictionary lookup chars.
      * @internal
      */
    virtual UBool isDictionaryChar(UChar32);

    /**
      * Common initialization function, used by constructors and bufferClone.
      *   (Also used by DictionaryBasedBreakIterator::createBufferClone().)
      * @internal
      */
    void init();

private:

    /**
     * This method backs the iterator back up to a "safe position" in the text.
     * This is a position that we know, without any context, must be a break position.
     * The various calling methods then iterate forward from this safe position to
     * the appropriate position to return.  (For more information, see the description
     * of buildBackwardsStateTable() in RuleBasedBreakIterator.Builder.)
     * @param statetable state table used of moving backwards
     * @internal
     */
    int32_t handlePrevious(const RBBIStateTable *statetable);

    /**
     * This method is the actual implementation of the next() method.  All iteration
     * vectors through here.  This method initializes the state machine to state 1
     * and advances through the text character by character until we reach the end
     * of the text or the state machine transitions to state 0.  We update our return
     * value every time the state machine passes through a possible end state.
     * @param statetable state table used of moving forwards
     * @internal
     */
    int32_t handleNext(const RBBIStateTable *statetable);
};

//------------------------------------------------------------------------------
//
//   Inline Functions Definitions ...
//
//------------------------------------------------------------------------------

inline UBool RuleBasedBreakIterator::operator!=(const BreakIterator& that) const {
    return !operator==(that);
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif
