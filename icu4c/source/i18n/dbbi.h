/*
**********************************************************************
*   Copyright (C) 1999 IBM Corp. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   12/1/99    rgillam     Complete port from Java.
**********************************************************************
*/

#ifndef DBBI_H
#define DBBI_H

#include "rbbi.h"

/**
 * A subclass of RuleBasedBreakIterator that adds the ability to use a dictionary
 * to further subdivide ranges of text beyond what is possible using just the
 * state-table-based algorithm.  This is necessary, for example, to handle
 * word and line breaking in Thai, which doesn't use spaces between words.  The
 * state-table-based algorithm used by RuleBasedBreakIterator is used to divide
 * up text as far as possible, and then contiguous ranges of letters are
 * repeatedly compared against a list of known words (i.e., the dictionary)
 * to divide them up into words.
 *
 * DictionaryBasedBreakIterator uses the same rule language as RuleBasedBreakIterator,
 * but adds one more special substitution name: &lt;dictionary&gt;.  This substitution
 * name is used to identify characters in words in the dictionary.  The idea is that
 * if the iterator passes over a chunk of text that includes two or more characters
 * in a row that are included in &lt;dictionary&gt;, it goes back through that range and
 * derives additional break positions (if possible) using the dictionary.
 *
 * DictionaryBasedBreakIterator is also constructed with the filename of a dictionary
 * file.  It follows a prescribed search path to locate the dictionary (right now,
 * it looks for it in /com/ibm/text/resources in each directory in the classpath,
 * and won't find it in JAR files, but this location is likely to change).  The
 * dictionary file is in a serialized binary format.  We have a very primitive (and
 * slow) BuildDictionaryFile utility for creating dictionary files, but aren't
 * currently making it public.  Contact us for help.
 */
class U_I18N_API DictionaryBasedBreakIterator : public RuleBasedBreakIterator {

private:
    /**
     * a temporary hiding place for the number of dictionary characters in the
     * last range passed over by next()
     */
    int32_t dictionaryCharCount;

    /**
     * when a range of characters is divided up using the dictionary, the break
     * positions that are discovered are stored here, preventing us from having
     * to use either the dictionary or the state table again until the iterator
     * leaves this range of text
     */
    int32_t* cachedBreakPositions;

    /**
     * The number of elements in cachedBreakPositions
     */
    int32_t numCachedBreakPositions;

    /**
     * if cachedBreakPositions is not null, this indicates which item in the
     * cache the current iteration position refers to
     */
    int32_t positionInCache;

    /**
     * Class ID
     */
    static char fgClassID;

public:
    //=======================================================================
    // constructors
    //=======================================================================

DictionaryBasedBreakIterator(const void* tablesImage, char* dictionaryFilename);

    //=======================================================================
    // boilerplate
    //=======================================================================

    /**
     * Destructor
     */
    virtual ~DictionaryBasedBreakIterator();

    /**
     * Assignment operator.  Sets this iterator to have the same behavior,
     * and iterate over the same text, as the one passed in.
     */
    DictionaryBasedBreakIterator& operator=(const DictionaryBasedBreakIterator& that);

    /**
     * Returns a newly-constructed RuleBasedBreakIterator with the same
     * behavior, and iterating over the same text, as this one.
     */
    virtual BreakIterator* clone() const;

    //=======================================================================
    // BreakIterator overrides
    //=======================================================================
    /**
     * Advances the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     */
    virtual int32_t previous();

    /**
     * Sets the iterator to refer to the first boundary position following
     * the specified position.
     * @offset The position from which to begin searching for a break position.
     * @return The position of the first break after the current position.
     */
    virtual int32_t following(int32_t offset);

    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     */
    virtual int32_t preceding(int32_t offset);

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
    virtual UClassID getDynamicClassID() const;

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
    static UClassID getStaticClassID();

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
    virtual int32_t handleNext();

    /**
     * dumps the cache of break positions (usually in response to a change in
     * position of some sort)
     */
    virtual void reset();

private:
    /**
     * This is the function that actually implements the dictionary-based
     * algorithm.  Given the endpoints of a range of text, it uses the
     * dictionary to determine the positions of any boundaries in this
     * range.  It stores all the boundary positions it discovers in
     * cachedBreakPositions so that we only have to do this work once
     * for each time we enter the range.
     */
    void divideUpDictionaryRange(int32_t startPos, int32_t endPos);

    /**
     * Used by the tables object to increment the count of dictionary characters
     * during iteration
     */
    void bumpDictionaryCharCount();

    friend class DictionaryBasedBreakIteratorTables;
};

inline UClassID DictionaryBasedBreakIterator::getDynamicClassID() const {
    return RuleBasedBreakIterator::getStaticClassID();
}

inline UClassID DictionaryBasedBreakIterator::getStaticClassID() {
    return (UClassID)(&fgClassID);
}

inline void DictionaryBasedBreakIterator::bumpDictionaryCharCount() {
    ++dictionaryCharCount;
}

#endif
