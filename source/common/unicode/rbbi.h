/*
***************************************************************************
*   Copyright (C) 1999-2002 International Business Machines Corporation   *
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
#include "unicode/brkiter.h"
#include "unicode/udata.h"
#include "unicode/parseerr.h"

struct UTrie;

U_NAMESPACE_BEGIN

struct RBBIDataHeader;
class RuleBasedBreakIteratorTables;
class BreakIterator;
class RBBIDataWrapper;



/**
 * <p>A subclass of BreakIterator whose behavior is specified using a list of rules.</p>
 *
 * <p>There are two kinds of rules, which are separated by semicolons: <i>variable definitions</i>
 * and <i>regular expressions.</i></p>
 *
 * <p>A varialbe definition defines a variable name that can be used in subsequent expressions.
 * It consists of a name preceded by a dollar sign, an equals
 * sign, and an expression.
 * A $variable is visible after its definition.
 * Variable definitions can contain other variables, as
 * long as those variables have been defined first. Variables are generally used to
 * make the regular expressions (which can get quite complex) shorter and easier to read.
 * They typically define either character categories or commonly-used subexpressions.</p>
 *
 * <p>A regular expression uses a subset of the normal Unix regular-expression syntax, and
 * defines a sequence of characters to be kept together. With one significant exception, the
 * iterator uses a longest-possible-match algorithm when matching text to regular
 * expressions. The iterator also treats descriptions containing multiple regular expressions
 * as if they were ORed together (i.e., as if they were separated by |).</p>
 *
 * <p>The special characters recognized by the regular-expression parser are as follows:</p>
 *
 * <blockquote>
 *   <table border="1" width="100%">
 *     <tr>
 *       <td width="6%">*</td>
 *       <td width="94%">Specifies that the expression preceding the asterisk may occur any number
 *       of times (including not at all).</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">()</td>
 *       <td width="94%">Encloses a sequence of characters.&nbsp; If followed by *, the sequence
 *       repeats.&nbsp; Otherwise, the parentheses are just a grouping device and a way to delimit
 *       the ends of expressions containing |.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">|</td>
 *       <td width="94%">Separates two alternative sequences of characters.&nbsp; Either one
 *       sequence or the other, but not both, matches this expression.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">.</td>
 *       <td width="94%">Matches any character.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">[]</td>
 *       <td width="94%">Specify a set of characters.&nbsp; A [] expression will
 *       match any single character that is specified in the [] expression.&nbsp; For more on the
 *       syntax of [] expressions, see the ICU User Guide description of UnicodeSet.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">/</td>
 *       <td width="94%">Specifies where the break position should go if text matches this
 *       expression.&nbsp; (e.g., &quot;[a-z]&#42;/[:Zs:]*1&quot; will match if the iterator sees a run
 *       of letters, followed by a run of whitespace, followed by a digit, but the break position
 *       will actually go before the whitespace).&nbsp; Expressions that don't contain / put the
 *       break position at the end of the matching text.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">\</td>
 *       <td width="94%">Escape character.&nbsp; The \ itself is ignored, but causes the next
 *       character to be treated as literal character.&nbsp;  Except for letters and numbers,
 *       characters in the ASCII range must be escaped to be considered as literals.</td>
 *     </tr>
 *     <tr>
 *       <td width="6%">!</td>
 *       <td width="94%">If ! appears at the beginning of a regular expression, it tells the regexp
 *       parser that this expression specifies the backwards-iteration behavior of the iterator,
 *       and not its normal iteration behavior.&nbsp;  The backwards rules must move the
 *       iterator to a safe position at or before the previous break position; forwards rules
 *       will then be used to find the exact previous position</td>
 *     </tr>
 *     <tr>
 *       <td width="6%"><em>(all others)</em></td>
 *       <td width="94%">All other characters are treated as literal characters, which must match
 *       the corresponding character(s) in the text exactly.</td>
 *     </tr>
 *   </table>
 * </blockquote>
 */




class U_COMMON_API RuleBasedBreakIterator : public BreakIterator {

protected:
    /**
     * The character iterator through which this BreakIterator accesses the text
     */
    CharacterIterator*  fText;

    /**
     * The rule data for this BreakIterator instance
     */
    RBBIDataWrapper    *fData;
    UTrie              *fCharMappings;

    /** Rule {tag} value for the most recent match. */
    int32_t             fLastBreakTag;

    /**
     * Rule tag value valid flag.
     * Some iterator operations don't intrinsically set the correct tag value.
     * This flag lets us lazily compute it if we are ever asked for the value.
     */
    UBool               fLastBreakTagValid;

    /**
     * Counter for the number of characters encountered with the "dictionary"
     *   flag set.  Normal RBBI iterators don't use it, although the code
     *   for updating it is live.  Dictionary Based break iterators (a subclass
     *   of us) access this field directly.
     */
    uint32_t           fDictionaryCharCount;

    //
    // Debugging flag.
    //
    static UBool        fTrace;



private:
    /**
     * Class ID
     */
    static const char fgClassID;

protected:
    //=======================================================================
    // constructors
    //=======================================================================

    /**
     * This constructor uses the udata interface to create a BreakIterator
     * whose internal tables live in a memory-mapped file.  "image" is a pointer
     * to the beginning of that file.
     */
    RuleBasedBreakIterator(UDataMemory* image, UErrorCode &status);

    /**
     * Constructor from a flattened set of RBBI data in malloced memory.
     *             RulesBasedBreakIterators built from a custom set of rules
     *             are created via this constructor; the rules are compiled
     *             into memory, then the break iterator is constructed here.
     *
     *             The break iterator adopts the memory, and will
     *             free it when done.
     */
    RuleBasedBreakIterator(RBBIDataHeader* data, UErrorCode &status);

    friend class RBBIRuleBuilder;
    friend class BreakIterator;



public:

    /** Default constructor.  Creates an empty shell of an iterator, with no
     *  rules or text to iterate over.   Object can subsequently be assigned.
     */
    RuleBasedBreakIterator();

    /**
     * Copy constructor.  Will produce a break iterator with the same behavior,
     * and which iterates over the same text, as the one passed in.
     * @param that The RuleBasedBreakIterator passed to be copied
     */
    RuleBasedBreakIterator(const RuleBasedBreakIterator& that);

    /**
     * Construct a RuleBasedBreakIterator from a set of rules supplied as a string.
     */
    RuleBasedBreakIterator( const UnicodeString    &rules,
                             UParseError             &parseError,
                             UErrorCode              &status);
    /**
     * Destructor
     */
    virtual ~RuleBasedBreakIterator();

    /**
     * Assignment operator.  Sets this iterator to have the same behavior,
     * and iterate over the same text, as the one passed in.
     * @param that The RuleBasedBreakItertor passed in
     * @return the newly created RuleBasedBreakIterator
     */
    RuleBasedBreakIterator& operator=(const RuleBasedBreakIterator& that);

    /**
     * Equality operator.  Returns TRUE if both BreakIterators are of the
     * same class, have the same behavior, and iterate over the same text.
     * @param that The BreakIterator to be compared for equality
     * @Return TRUE if both BreakIterators are of the
     * same class, have the same behavior, and iterate over the same text.
     */
    virtual UBool operator==(const BreakIterator& that) const;

    /**
     * Not-equal operator.  If operator== returns TRUE, this returns FALSE,
     * and vice versa.
     * @param that The BreakIterator to be compared for inequality
     * @return TRUE if both BreakIterators are not same.
     */
    UBool operator!=(const BreakIterator& that) const;

    /**
     * Returns a newly-constructed RuleBasedBreakIterator with the same
     * behavior, and iterating over the same text, as this one.
     * Differs from the copy constructor in that it is polymorphic, and
     *   will correctly clone (copy) a derived class.
     */
    virtual BreakIterator* clone() const;

    /**
     * Compute a hash code for this BreakIterator
     * @return A hash code
     */
    virtual int32_t hashCode(void) const;

    /**
     * Returns the description used to create this iterator
     * @return the description used to create this iterator
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
     */
    virtual const CharacterIterator& getText(void) const;


    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText An iterator over the text to analyze.  The BreakIterator
     * takes ownership of the character iterator.  The caller MUST NOT delete it!
     */
    virtual void adoptText(CharacterIterator* newText);

    /**
     * Set the iterator to analyze a new piece of text.  This function resets
     * the current iteration position to the beginning of the text.
     * @param newText The text to analyze.
     */
    virtual void setText(const UnicodeString& newText);

    /**
     * Sets the current iteration position to the beginning of the text.
     * (i.e., the CharacterIterator's starting offset).
     * @return The offset of the beginning of the text.
     */
    virtual int32_t first(void);

    /**
     * Sets the current iteration position to the end of the text.
     * (i.e., the CharacterIterator's ending offset).
     * @return The text's past-the-end offset.
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
     */
    virtual int32_t next(int32_t n);

    /**
     * Advances the iterator to the next boundary position.
     * @return The position of the first boundary after this one.
     */
    virtual int32_t next(void);

    /**
     * Advances the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     */
    virtual int32_t previous(void);

    /**
     * Sets the iterator to refer to the first boundary position following
     * the specified position.
     * @param offset The position from which to begin searching for a break position.
     * @return The position of the first break after the current position.
     */
    virtual int32_t following(int32_t offset);

    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @param offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     */
    virtual int32_t preceding(int32_t offset);

    /**
     * Returns true if the specfied position is a boundary position.  As a side
     * effect, leaves the iterator pointing to the first boundary position at
     * or after "offset".
     * @param offset the offset to check.
     * @return True if "offset" is a boundary position.
     */
    virtual UBool isBoundary(int32_t offset);

    /**
     * Returns the current iteration position.
     * @return The current iteration position.
     */
    virtual int32_t current(void) const;


    /**
     * Return the status from the break rule that determined the most recently
     * returned break position.  The values appear in the rule source
     * within brackets, {123}, for example.  For rules that do not specify a
     * status, a default value of 0 is returned.
     * @return the status from the break rule that determined the most recently
     * returned break position.
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
     */
    inline virtual UClassID getDynamicClassID(void) const;

    /**
     * Returns the class ID for this class.  This is useful only for
     * comparing to a return value from getDynamicClassID().  For example:
     *
     *      Base* polymorphic_pointer = createPolymorphicObject();
     *      if (polymorphic_pointer->getDynamicClassID() ==
     *          Derived::getStaticClassID()) ...
     *
     * @return          The class ID for all objects of this class.
     */
    inline static UClassID getStaticClassID(void);

    virtual BreakIterator *  createBufferClone(void *stackBuffer,
                                               int32_t &BufferSize,
                                               UErrorCode &status);


    /**
     * Return the binary form of compiled break rules,
     * which can then be used to create a new break iterator at some
     * time in the future.  Creating a break iterator in this way
     * is much faster than building one from the source form of the
     * break rules.
     *
     * The binary data is can only be used with the same version of ICU
     *  and on the same platform type (processor endian-ness)
     *
     * @return   A pointer to the binary (compiled) rule data.  The storage
     *           belongs to the RulesBasedBreakIterator object, no the
     *           caller, and must not be modified or deleted.
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
     */
    virtual int32_t handleNext(void);

    /**
     * This method backs the iterator back up to a "safe position" in the text.
     * This is a position that we know, without any context, must be a break position.
     * The various calling methods then iterate forward from this safe position to
     * the appropriate position to return.  (For more information, see the description
     * of buildBackwardsStateTable() in RuleBasedBreakIterator.Builder.)
     */
    virtual int32_t handlePrevious(void);

    /**
     * Dumps caches and performs other actions associated with a complete change
     * in text or iteration position.  This function is a no-op in RuleBasedBreakIterator,
     * but subclasses can and do override it.
     */
    virtual void reset(void);

    /**
      * Return true if the category lookup for this char
      * indicates that it is in the set of dictionary lookup chars.
      * This function is intended for use by dictionary based break iterators.
      * @return true if the category lookup for this char
      * indicates that it is in the set of dictionary lookup chars.
      */
    virtual UBool isDictionaryChar(UChar32);

    /**
      * Common initialization function, used by constructors and bufferClone.
      *   (Also used by DictionaryBasedBreakIterator::createBufferClone().)
      */
    void init();

};




//----------------------------------------------------------------------------------
//
//   Inline Functions Definitions ...
//
//----------------------------------------------------------------------------------

inline UBool RuleBasedBreakIterator::operator!=(const BreakIterator& that) const {
    return !operator==(that);
}

inline UClassID RuleBasedBreakIterator::getDynamicClassID(void) const {
    return RuleBasedBreakIterator::getStaticClassID();
}

inline UClassID RuleBasedBreakIterator::getStaticClassID(void) {
    return (UClassID)(&fgClassID);
}



U_NAMESPACE_END

#endif
