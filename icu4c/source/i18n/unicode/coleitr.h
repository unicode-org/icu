/*
*****************************************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*/
//===============================================================================
//
// File coleitr.h
//
// 
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date         Name          Description
//
//  8/18/97     helena      Added internal API documentation.
// 08/03/98        erm            Synched with 1.2 version CollationElementIterator.java
// 12/10/99      aliu          Ported Thai collation support from Java.
//===============================================================================

#ifndef COLEITR_H
#define COLEITR_H


#include "unicode/unistr.h"
#include "unicode/tblcoll.h"
#include "unicode/chariter.h"

class Normalizer;
class VectorOfPToContractElement;

/**
 * The CollationElementIterator class is used as an iterator to walk through
 * each character of an international string. Use the iterator to return the
 * ordering priority of the positioned character. The ordering priority of
 * a character, which we refer to as a key, defines how a character is
 * collated in the given collation object.
 * For example, consider the following in Spanish:
 * <pre>
 * .       "ca" -> the first key is key('c') and second key is key('a').
 * .       "cha" -> the first key is key('ch') and second key is key('a').
 * </pre>
 * And in German,
 * <pre>
 * .       "æb"-> the first key is key('a'), the second key is key('e'), and
 * .       the third key is key('b').
 * </pre>
 * The key of a character, is an integer composed of primary order(short),
 * secondary order(char), and tertiary order(char).  Java strictly defines
 * the size and signedness of its primitive data types.  Therefore, the static
 * functions primaryOrder(), secondaryOrder(), and tertiaryOrder() return int32_t
 * to ensure the correctness of the key value.
 * <p>Example of the iterator usage: (without error checking)
 * <pre>
 * .  void CollationElementIterator_Example()
 * .  {
 * .      UnicodeString str = "This is a test";
 * .      UErrorCode success = U_ZERO_ERROR;
 * .      RuleBasedCollator* rbc =
 * .          (RuleBasedCollator*) RuleBasedCollator::createInstance(success);
 * .      CollationElementIterator* c =
 * .          rbc->createCollationElementIterator( str );
 * .      int32_t order = c->next(success);
 * .      int32_t primaryOrder = CollationElementIterator::primaryOrder( order );
 * .      delete c;
 * .      delete rbc;
 * .  }
 * </pre>
 * <p>
 * CollationElementIterator::next returns the collation order of the next
 * character based on the comparison level of the collator.  A collation order 
 * consists of primary order, secondary order and tertiary order.  The data 
 * type of the collation order is <strong>int32_t</strong>.  The first 16 bits of 
 * a collation order is its primary order; the next 8 bits is the secondary 
 * order and the last 8 bits is the tertiary order.
 *
 * @see                Collator
 * @see                RuleBasedCollator
 * @version            1.9 1/30/97
 * @author             Helena Shih
 */
class U_I18N_API CollationElementIterator
{
public:     
    /**
     * NULLORDER indicates the iterator has consumed the last element.
     */
    static  int32_t const   NULLORDER;

    /** Destructor
     */
                                ~CollationElementIterator();
    /**
     * Returns true if "other" is the same as "this"
     */
    virtual     bool_t          operator==(const CollationElementIterator& other) const;

    /**
     * Returns true if "other" is not the same as "this".
     */
    virtual     bool_t          operator!=(const CollationElementIterator& other) const;

    /**
     * Resets the cursor to the beginning of the string.
     */
            void                reset(void);
    /**
     * Gets the ordering priority of the next character in the string.
     * @param status the error code status.
     * @return the next character's ordering.  Returns NULLORDER if
     * the end of string is reached.
     */
            int32_t             next(UErrorCode& status);
     /**
      * Get the ordering priority of the previous collation element in the string.
      * @param status the error code status.
      * @return the previous element's ordering.  Returns NULLORDER if
      * the beginning of string is reached.
      */
            int32_t                 previous(UErrorCode& status);

    /**
     * Gets the primary order of a collation order.
     * @param order the collation order
     * @return the primary order of a collation order.
     */
    static  int32_t             primaryOrder(int32_t order);
    /**
     * Gets the secondary order of a collation order.
     * @param order the collation order
     * @return the secondary order of a collation order.
     */
    static  int32_t             secondaryOrder(int32_t order);
    /**
     * Gets the tertiary order of a collation order.
     * @param order the collation order
     * @return the tertiary order of a collation order.
     */
    static  int32_t             tertiaryOrder(int32_t order);
    /**
     *  Return the maximum length of any expansion sequences that end
     *  with the specified comparison order.
     *  @param order a collation order returned by previous or next.
     *  @return the maximum length of any expansion sequences ending
     *          with the specified order.
     */
        int32_t                    getMaxExpansion(int32_t order) const;

public:
    /**
     *  Gets the comparison order in the desired strength.  Ignore the other
     *  differences.
     *  @param order The order value
     */
           int32_t              strengthOrder(int32_t order) const;
    /**
     * Sets the source string.
     * @param str the source string.
     * @param status the error code status.
     */
            void                setText(const   UnicodeString&  str,
                                                UErrorCode&      status);
     /**
     * Sets the source string.
     * @param str the source character iterator.
     * @param status the error code status.
     */
            void                setText(CharacterIterator&  str,
                                        UErrorCode&            status);
   /**
     *  Checks if a comparison order is ignorable.
     *  @param order the collation order.
     *  @return TRUE if a character is ignorable, FALSE otherwise.
     */
    static    bool_t              isIgnorable(int32_t order);
    /**
     *  Gets the offset of the currently processed character in the source string.
     *  @return the offset of the character.
     */
            UTextOffset          getOffset(void) const;
    /**
     *  Sets the offset of the currently processed character in the source string.
     *  @param newOffset the new offset.
     *  @param status the error code status.
     *  @return the offset of the character.
     */
            void                setOffset(UTextOffset newOffset,
                                          UErrorCode& status);
protected:
    /**
     * CollationElementIterator constructor.  This takes the source string and
     * the collation object.  The cursor will walk thru the source string based
     * on the predefined collation rules.  If the source string is empty,
     * NULLORDER will be returned on the calls to next().
     * @param sourceText the source string.
     * @param startOffset the beginning offset of the string where the cursor
     * starts the iterating.
     * @param endOffset the ending offset of the string where the cursor
     * stops the iterating.
     * @param order the collation object.
     */
                                CollationElementIterator(   const   UnicodeString&  sourceText,
                                                            const   RuleBasedCollator*  order,
                                                            UErrorCode& status);

    /**
     * CollationElementIterator constructor.  This takes the source string and
     * the collation object.  The cursor will walk thru the source string based
     * on the predefined collation rules.  If the source string is empty,
     * NULLORDER will be returned on the calls to next().
     * @param sourceText the source string.
     * @param startOffset the beginning offset of the string where the cursor
     * starts the iterating.
     * @param endOffset the ending offset of the string where the cursor
     * stops the iterating.
     * @param order the collation object.
     */
                                CollationElementIterator(   const    CharacterIterator&  sourceText,
                                                            const   RuleBasedCollator*  order,
                                                            UErrorCode& status);
    /**
     * Assignment operator
     */
    const   CollationElementIterator&
                                operator=(const CollationElementIterator& other);
public:
    /**
     * Copy constructor.
     */
                                CollationElementIterator(const  CollationElementIterator& other);
    //============================================================
    // privates
    //============================================================
private:
    /**
     * Default constructor.
     */
                                CollationElementIterator();
    /**
     * Copy constructor.
     */
                                CollationElementIterator(const RuleBasedCollator* order);

    /**
     * Gets the ordering priority of the next contracting character in the
     * string.
     * @param ch the starting character of a contracting character token
     * @param status the error code status.
     * @return the next contracting character's ordering.  Returns NULLORDER
     * if the end of string is reached.
     */
            int32_t             nextContractChar(   UChar     ch,
                                                    UErrorCode&  status);

    /**
     * Gets the ordering priority of the previous contracting character in the
     * string.
     * @param ch the starting character of a contracting character token
     * @param status the error code status.
     * @return the previous contracting character's ordering.  Returns NULLORDER
     * if the start of string is reached.
     */
            int32_t             prevContractChar(   UChar     ch,
                                                    UErrorCode&  status);
    
    inline static bool_t isThaiPreVowel(UChar ch);
                 
    inline static bool_t isThaiBaseConsonant(UChar ch);
                 
    VectorOfInt* makeReorderedBuffer(UChar colFirst,
                                     int32_t lastValue,
                                     VectorOfInt* lastExpansion,
                                     bool_t forward, UErrorCode& status);

    friend  class   RuleBasedCollator;
    static  const   int32_t         UNMAPPEDCHARVALUE;

            Normalizer*            text;       // owning 

            VectorOfInt*        bufferAlias; // not owned

    /**
     * ownBuffer wants to be a subobject, not a pointer, but that
     * means exposing the internal class VectorOfInt by #including the
     * internal header "tables.h" -- not allowed!  ownBuffer is a
     * fixed-size 2-element vector that is used to handle Thai
     * collation; bufferAlias points to ownBuffer in some situations.
     * [j159 - aliu]
     */
            VectorOfInt*        ownBuffer;

    /**
     * reorderBuffer is created on demand, so it doesn't want to be
     * a subobject -- pointer is fine.  It is created and bufferAlias
     * is set to it under certain conditions.  Once created, it is
     * reused for the life of this object.  Because of the implementation
     * of VectorOfInt, it grows monotonically.  [j159 - aliu]
     */
            VectorOfInt*        reorderBuffer;

            int32_t             expIndex;
            UnicodeString       key;
    const   RuleBasedCollator*  orderAlias;
};


/**
 * Get the primary order of a collation order.
 * @param order the collation order
 * @return the primary order of a collation order.
 */
inline int32_t
CollationElementIterator::primaryOrder(int32_t order)
{
    order &= RuleBasedCollator::PRIMARYORDERMASK;
    return (order >> RuleBasedCollator::PRIMARYORDERSHIFT);
}
/**
 * Get the secondary order of a collation order.
 * @param order the collation order
 * @return the secondary order of a collation order.
 */
inline int32_t
CollationElementIterator::secondaryOrder(int32_t order)
{
    order = order & RuleBasedCollator::SECONDARYORDERMASK;
    return (order >> RuleBasedCollator::SECONDARYORDERSHIFT);
}
/**
 * Get the tertiary order of a collation order.
 * @param order the collation order
 * @return the tertiary order of a collation order.
 */
inline int32_t
CollationElementIterator::tertiaryOrder(int32_t order)
{
    return (order &= RuleBasedCollator::TERTIARYORDERMASK);
}

inline int32_t
CollationElementIterator::getMaxExpansion(int32_t order) const
{
    return orderAlias->getMaxExpansion(order);
}

inline bool_t
CollationElementIterator::isIgnorable(int32_t order)
{
    return (primaryOrder(order) == 0);
}

/**
 * Determine if a character is a Thai vowel (which sorts after
 * its base consonant).
 */
inline bool_t CollationElementIterator::isThaiPreVowel(UChar ch) {
    return (ch >= (UChar)0x0E40) && (ch <= (UChar)0X0E44);
}

/**
 * Determine if a character is a Thai base consonant
 */
inline bool_t CollationElementIterator::isThaiBaseConsonant(UChar ch) {
    return (ch >= (UChar)0x0E01) && (ch <= (UChar)0x0E2E);
}

#endif
