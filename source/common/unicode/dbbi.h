/*
**********************************************************************
*   Copyright (C) 1999-2004 IBM Corp. All rights reserved.
**********************************************************************
*   Date        Name        Description
*   12/1/99    rgillam     Complete port from Java.
*   01/13/2000 helena      Added UErrorCode to ctors.
**********************************************************************
*/

#ifndef DBBI_H
#define DBBI_H

#include "unicode/rbbi.h"

#if !UCONFIG_NO_BREAK_ITERATION

U_NAMESPACE_BEGIN

/* forward declaration */
class DictionaryBasedBreakIteratorTables;

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
 * <p>Applications do not normally need to include this header.</p>
 *
 * <p>This class will probably be deprecated in a future release of ICU, and replaced
 *  with a more flexible and capable dictionary based break iterator.  This change
 *  should be invisible to applications, because creation and use of instances of
 *  DictionaryBasedBreakIterator is through the factories and abstract
 *  API on class BreakIterator, which will remain stable.</p>
 *
 * <p>This class is not intended to be subclassed.</p>
 *
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
 * <p>
 * <b> NOTE </b>  The DictionaryBasedIterator class is still under development.  The
 * APIs are not in stable condition yet.  
 */
class U_COMMON_API DictionaryBasedBreakIterator : public RuleBasedBreakIterator {

private:

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

    DictionaryBasedBreakIteratorTables  *fTables;

    /**=======================================================================
     * Create a dictionary based break boundary detection iterator.  
     * @param tablesImage The location for the dictionary to be loaded into memory
     * @param dictionaryFilename The name of the dictionary file 
     * @param status the error code status
     * @return A dictionary based break detection iterator.  The UErrorCode& status 
     * parameter is used to return status information to the user.
     * To check whether the construction succeeded or not, you should check
     * the value of U_SUCCESS(err).  If you wish more detailed information, you
     * can check for informational error results which still indicate success.  For example,
     * U_FILE_ACCESS_ERROR will be returned if the file does not exist.
     * The caller owns the returned object and is responsible for deleting it.
     ======================================================================= */
    DictionaryBasedBreakIterator(UDataMemory* tablesImage, const char* dictionaryFilename, UErrorCode& status);

public:
    //=======================================================================
    // boilerplate
    //=======================================================================

    /**
     * Destructor
     * @stable ICU 2.0
     */
    virtual ~DictionaryBasedBreakIterator();

    /**
     * Default constructor.  Creates an "empty" break iterator.
     * Such an iterator can subsequently be assigned to.
     * @return the newly created DictionaryBaseBreakIterator.
     * @stable ICU 2.0
     */
     DictionaryBasedBreakIterator();

     /**
      * Copy constructor.
      * @param other The DictionaryBasedBreakIterator to be copied.
      * @return the newly created DictionaryBasedBreakIterator.
      * @stable ICU 2.0
      */
     DictionaryBasedBreakIterator(const DictionaryBasedBreakIterator &other);

    /**
     * Assignment operator. 
     * @param that The object to be copied.
     * @return the newly set DictionaryBasedBreakIterator.
     * @stable ICU 2.0
     */
    DictionaryBasedBreakIterator& operator=(const DictionaryBasedBreakIterator& that);

    /**
     * Returns a newly-constructed RuleBasedBreakIterator with the same
     * behavior, and iterating over the same text, as this one.
     * @return Returns a newly-constructed RuleBasedBreakIterator.
     * @stable ICU 2.0
     */
    virtual BreakIterator* clone(void) const;

    //=======================================================================
    // BreakIterator overrides
    //=======================================================================
    /**
     * Advances the iterator backwards, to the last boundary preceding this one.
     * @return The position of the last boundary position preceding this one.
     * @stable ICU 2.0
     */
    virtual int32_t previous(void);

    /**
     * Sets the iterator to refer to the first boundary position following
     * the specified position.
     * @param offset The position from which to begin searching for a break position.
     * @return The position of the first break after the current position.
     * @stable ICU 2.0
     */
    virtual int32_t following(int32_t offset);

    /**
     * Sets the iterator to refer to the last boundary position before the
     * specified position.
     * @param offset The position to begin searching for a break from.
     * @return The position of the last boundary before the starting position.
     * @stable ICU 2.0
     */
    virtual int32_t preceding(int32_t offset);

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
     * removes the cache of break positions (usually in response to a change in
     * position of some sort)
     * @internal
     */
    virtual void reset(void);

    /**
     *  init    Initialize a dbbi.  Common routine for use by constructors.
     *  @internal
     */
    void init();

    /**
     * @param stackBuffer user allocated space for the new clone. If NULL new memory will be allocated. 
     * If buffer is not large enough, new memory will be allocated.
     * @param BufferSize reference to size of allocated space. 
     * If BufferSize == 0, a sufficient size for use in cloning will 
     * be returned ('pre-flighting')
     * If BufferSize is not enough for a stack-based safe clone, 
     * new memory will be allocated.
     * @param status to indicate whether the operation went on smoothly or there were errors
     *  An informational status value, U_SAFECLONE_ALLOCATED_ERROR, is used if any allocations were 
     *  necessary.
     * @return pointer to the new clone
     * @internal
     */
    virtual BreakIterator *  createBufferClone(void *stackBuffer,
                                               int32_t &BufferSize,
                                               UErrorCode &status);


private:
    /**
     * This is the function that actually implements the dictionary-based
     * algorithm.  Given the endpoints of a range of text, it uses the
     * dictionary to determine the positions of any boundaries in this
     * range.  It stores all the boundary positions it discovers in
     * cachedBreakPositions so that we only have to do this work once
     * for each time we enter the range.
     * @param startPos The start position of a range of text
     * @param endPos The end position of a range of text
     * @param status The error code status
     */
    void divideUpDictionaryRange(int32_t startPos, int32_t endPos, UErrorCode &status);


    /*
     * HSYS : Please revisit with Rich, the ctors of the DBBI class is currently
     * marked as private.
     */
    friend class DictionaryBasedBreakIteratorTables;
    friend class BreakIterator;
};

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_BREAK_ITERATION */

#endif
