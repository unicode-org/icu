/*
**********************************************************************
*   Copyright (C) 1999-2000 IBM and others. All rights reserved.
**********************************************************************
*   Date        Name        Description
*  03/22/2000   helena      Creation.
**********************************************************************
*/
#ifndef STRSRCH_H
#define STRSRCH_H

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/tblcoll.h"
#include "unicode/brkiter.h"
#include "srchiter.h"

class SearchIterator;
/**
 * <code>StringSearch</code> is a <code>SearchIterator</code> that provides
 * language-sensitive text searching based on the comparison rules defined
 * in a {@link RuleBasedCollator} object.
 * Instances of <code>StringSearch</code> function as iterators
 * maintain a current position and scan over text returning the index of
 * characters where the pattern occurs and the length of each match.
 * <p>
 * <code>StringSearch</code> uses a version of the fast Boyer-Moore search
 * algorithm that has been adapted to work with the large character set of
 * Unicode.  See "Efficient Text Searching in Java", to be published in
 * <i>Java Report</i> in February, 1999, for further information on the algorithm.
 * <p>
 * Consult the <code>SearchIterator</code> documentation for information on
 * and examples of how to use instances of this class to implement text
 * searching.  <code>SearchIterator</code> provides all of the necessary
 * API; this class only provides constructors and internal implementation
 * methods.
 *
 * @see SearchIterator
 * @see RuleBasedCollator
 *
 * @author Laura Werner
 * @version 1.0
 */

class StringSearch : public SearchIterator
{
public:
    /**
     * Construct a <code>StringSearch</code> object using a specific collator and set
     * of boundary-detection rules.
     * <p>
     * @param pat       The text for which this object will search.
     *
     * @param target    The text in which to search for the pattern.
     *
     * @param coll      A <code>RuleBasedCollator</code> object which defines the
     *                  language-sensitive comparison rules used to determine 
     *                  whether text in the pattern and target matches.
     *
     * @param breaker   A <code>BreakIterator</code> object used to constrain the matches
     *                  that are found.  Matches whose start and end indices
     *                  in the target text are not boundaries as determined
     *                  by the <code>BreakIterator</code> are ignored.  If this behavior
     *                  is not desired, <code>null</code> can be passed in instead.
     */
    StringSearch(const UnicodeString& pat, 
                        CharacterIterator* target,
                        RuleBasedCollator* coll, 
                        BreakIterator* breaker,
                        UErrorCode& status);

    /**
     * Construct a <code>StringSearch</code> object using a specific collator.
     * <p>
     * @param pattern   The text for which this object will search.
     *
     * @param target    The text in which to search for the pattern.
     *
     * @param collator  A <code>RuleBasedCollator</code> object which defines the
     *                  language-sensitive comparison rules used to determine 
     *                  whether text in the pattern and target matches.
     */
    StringSearch(const UnicodeString& pattern,
                 CharacterIterator* target,
                 RuleBasedCollator* collator,
                 UErrorCode& status);

    /**
     * copy constructor
     */
    StringSearch(const StringSearch& that);

    /**
     * Construct a <code>StringSearch</code> object using the collator and
     * character boundary detection rules for a given locale
     * <p>
     * @param pattern   The text for which this object will search.
     *
     * @param target    The text in which to search for the pattern.
     *
     * @param loc       The locale whose collation and break-detection rules
     *                  should be used.
     *
     * @exception       ClassCastException thrown if the collator for the specified
     *                  locale is not a RuleBasedCollator.
     */
    StringSearch(const UnicodeString& pattern, 
                 CharacterIterator* target, 
                 const Locale& loc,
                 UErrorCode& status);
    /**
     * Construct a <code>StringSearch</code> object using the collator for the default
     * locale
     * <p>
     * @param pattern   The text for which this object will search.
     *
     * @param target    The text in which to search for the pattern.
     *
     * @param collator  A <code>RuleBasedCollator</code> object which defines the
     *                  language-sensitive comparison rules used to determine 
     *                  whether text in the pattern and target matches.
     */
    StringSearch(const UnicodeString& pattern, 
                 const UnicodeString& target,
                 UErrorCode& status);

    virtual ~StringSearch(void);
    /**
     * Assignment operator.  Sets this iterator to have the same behavior,
     * and iterate over the same text, as the one passed in.
     */
    StringSearch& operator=(const StringSearch& that);

    /**
     * Equality operator.  Returns TRUE if both BreakIterators are of the
     * same class, have the same behavior, and iterate over the same text.
     */
    virtual bool_t operator==(const SearchIterator& that) const;

    /**
     * Not-equal operator.  If operator== returns TRUE, this returns FALSE,
     * and vice versa.
     */
    bool_t operator!=(const SearchIterator& that) const;

    /**
     * Returns a newly-constructed RuleBasedBreakIterator with the same
     * behavior, and iterating over the same text, as this one.
     */
    virtual SearchIterator* clone(void) const;

    //-------------------------------------------------------------------
    // Getters and Setters
    //-------------------------------------------------------------------
    
    /**
     * Sets this object's strength property. The strength determines the
     * minimum level of difference considered significant during a
     * search.  Generally, {@link Collator#TERTIARY} and 
     * {@link Collator#IDENTICAL} indicate that all differences are
     * considered significant, {@link Collator#SECONDARY} indicates
     * that upper/lower case distinctions should be ignored, and
     * {@link Collator#PRIMARY} indicates that both case and accents
     * should be ignored.  However, the exact meanings of these constants
     * are determined by individual Collator objects.
     * <p>
     * @see Collator#PRIMARY
     * @see Collator#SECONDARY
     * @see Collator#TERTIARY
     * @see Collator#IDENTICAL
     */
     void setStrength(Collator::ECollationStrength newStrength, UErrorCode& status);
    
    
    /**
     * Returns this object's strength property, which indicates what level
     * of differences are considered significant during a search.
     * <p>
     * @see #setStrength
     */
     Collator::ECollationStrength getStrength(void) const;
    
    /**
     * Set the collator to be used for this string search.  Also changes
     * the search strength to match that of the new collator.
     * <p>
     * This method causes internal data such as Boyer-Moore shift tables
     * to be recalculated, but the iterator's position is unchanged.
     * <p>
     * @see #getCollator
     */
     void setCollator(const RuleBasedCollator* coll, UErrorCode& status);
    
    /**
     * Return the RuleBasedCollator being used for this string search.
     */
    const RuleBasedCollator&     getCollator() const;
    
    /**
     * Set the pattern for which to search.  
     * This method causes internal data such as Boyer-Moore shift tables
     * to be recalculated, but the iterator's position is unchanged.
     */
    void setPattern(const UnicodeString& pat, UErrorCode& status);
    
    /**
     * Returns the pattern for which this object is searching.
     */
    const UnicodeString& getPattern() const;
    
    /**
     * Set the target text which should be searched and resets the
     * iterator's position to point before the start of the new text.
     * This method is useful if you want to re-use an iterator to
     * search for the same pattern within a different body of text.
     */
    virtual void setTarget(const UnicodeString& newText);    

    /**
     * Set the target text which should be searched and resets the
     * iterator's position to point before the start of the target text.
     * This method is useful if you want to re-use an iterator to
     * search for the same pattern within a different body of text.
     *
     * @see #getTarget
     */
    virtual void adoptTarget(CharacterIterator* iterator);

    /** Reset iterator
     */
    virtual void reset(void);
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

protected:
    //-------------------------------------------------------------------
    // Privates
    //-------------------------------------------------------------------

    /**
     * Search forward for matching text, starting at a given location.
     * Clients should not call this method directly; instead they should call
     * {@link SearchIterator#next}.
     * <p>
     * If a match is found, this method returns the index at which the match
     * starts and calls {@link SearchIterator#setMatchLength}
     * with the number of characters in the target
     * text that make up the match.  If no match is found, the method returns
     * <code>DONE</code> and does not call <tt>setMatchLength</tt>.
     * <p>
     * @param start The index in the target text at which the search starts.
     *
     * @return      The index at which the matched text in the target starts, or DONE
     *              if no match was found.
     * <p>
     * @see SearchIterator#next
     * @see SearchIterator#DONE
     */
    virtual int32_t handleNext(int32_t start, UErrorCode& status);
    /**
     * Search backward for matching text ,starting at a given location.
     * Clients should not call this method directly; instead they should call
     * <code>SearchIterator.previous()</code>, which this method overrides.
     * <p>
     * If a match is found, this method returns the index at which the match
     * starts and calls {@link SearchIterator#setMatchLength}
     * with the number of characters in the target
     * text that make up the match.  If no match is found, the method returns
     * <code>DONE</code> and does not call <tt>setMatchLength</tt>.
     * <p>
     * @param start The index in the target text at which the search starts.
     *
     * @return      The index at which the matched text in the target starts, or DONE
     *              if no match was found.
     * <p>
     * @see SearchIterator#previous
     * @see SearchIterator#DONE
     */
    virtual int32_t handlePrev(int32_t start, UErrorCode& status);
private:
    /**
     * Return a bitmask that will select only the portions of a collation 
     * element that are significant at the given strength level.
     */
    static int32_t getMask(Collator::ECollationStrength strength);
    

    void initialize(UErrorCode& status);
    /**
     * Method used by StringSearch to determine how far to the right to
     * shift the pattern during a Boyer-Moore search.  
     *
     * @param curValue  The current value in the target text
     * @param curIndex  The index in the pattern at which we failed to match
     *                  curValue in the target text.
     */
    int32_t getShift( int32_t curValue, int32_t curIndex ) const;

    /**
     * Method used by StringSearch to determine how far to the left to
     * shift the pattern during a reverse Boyer-Moore search.  
     *
     * @param curValue  The current value in the target text
     * @param curIndex  The index in the pattern at which we failed to match
     *                  curValue in the target text.
     */
    int32_t getBackShift( int32_t curValue, int32_t curIndex ) const;

    /**
     * Hash a collation element from its full size (32 bits) down into a
     * value that can be used as an index into the shift tables.  Right
     * now we do a modulus by the size of the hash table.
     *
     * TODO: At some point I should experiment to see whether a slightly
     * more complicated hash function gives us a better distribution
     * on multilingual text.  I doubt it will have much effect on
     * performance, though.
     */
    static int32_t hash(int32_t order);

    //------------------------------------------------------------------------
    // Private Data
    //
    CollationElementIterator      *iter;
    RuleBasedCollator             *collator;
    /* HSYS ? Why?  Changes to this will not affect collator.  no changes to the comparsion result */
    Collator::ECollationStrength  strength;
    
    //------------------------------------------------------------------------
    // Everything from here on down is the data used to represent the
    // Boyer-Moore shift tables and the code that generates and manipulates
    // them.
    //    
    int32_t         *valueList;
    int32_t         valueListLen;
    int32_t         shiftTable[256];
    int32_t         backShiftTable[256];

    UnicodeString   pattern;            // The pattern string
    int32_t         normLen;        // num. of collation elements in pattern.
    int32_t         minLen;         // Min of composed, decomposed versions
    int32_t         maxLen;         // Max
    CollationElementIterator *it;   // to be removed

private:
    /* to be removed */
    void dumpTables();
    /**
     * Class ID
     */
    static char fgClassID;
};

inline bool_t StringSearch::operator!=(const SearchIterator& that) const 
{
    return !operator==(that);
}

inline UClassID StringSearch::getDynamicClassID(void) const 
{
    return StringSearch::getStaticClassID();
}

inline UClassID StringSearch::getStaticClassID(void) 
{
    return (UClassID)(&fgClassID);
}


#endif

