/*
*******************************************************************************
* Copyright (C) 1996-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/
//=============================================================================
//
// File coleitr.cpp
//
// 
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date         Name          Description
//
//  6/23/97     helena      Adding comments to make code more readable.
// 08/03/98     erm         Synched with 1.2 version of CollationElementIterator.java
// 12/10/99      aliu          Ported Thai collation support from Java.
//=============================================================================

#include "unicode/sortkey.h"
#include "unicode/coleitr.h"

#include "unicode/chariter.h"
#include "tables.h"
#include "unicode/normlzr.h"
#include "unicode/unicode.h"
#include "tcoldata.h"
#include "ucmp32.h"


int32_t const CollationElementIterator::NULLORDER = 0xffffffff;
int32_t const CollationElementIterator::UNMAPPEDCHARVALUE = 0x7fff0000;


// This private method will never be called, but it makes the linker happy

CollationElementIterator::CollationElementIterator()
: expIndex(0),
  text(0),
  bufferAlias(0),
  ownBuffer(new VectorOfInt(2)),
  reorderBuffer(0),
  orderAlias(0)
{
}

// This private method will never be called, but it makes the linker happy

CollationElementIterator::CollationElementIterator(const RuleBasedCollator* order)
: expIndex(0),
  text(0),
  bufferAlias(0),
  ownBuffer(new VectorOfInt(2)),
  reorderBuffer(0),
  orderAlias(order)
{
}

// This is the "real" constructor for this class; it constructs an iterator
// over the source text using the specified collator
CollationElementIterator::CollationElementIterator( const UnicodeString& sourceText,
                                                    const RuleBasedCollator* order,
                                                    UErrorCode& status) 
: expIndex(0), 
  text(NULL),
  bufferAlias(NULL),
  ownBuffer(new VectorOfInt(2)),
  reorderBuffer(0),
  orderAlias(order)
{
    if (U_FAILURE(status)) {
        return;
    }

    if ( sourceText.length() != 0 ) {
        //
        // A CollationElementIterator is really a two-layered beast.
        // Internally it uses a Normalizer to munge the source text
        // into a form where all "composed" Unicode characters (such as ü) are
        // split into a normal character and a combining accent character.  
        // Afterward, CollationElementIterator does its own processing to handle
        // expanding and contracting collation sequences, ignorables, and so on.
        //
      Normalizer::EMode decomp = (order->getStrength() == Collator::IDENTICAL)
    ? Normalizer::NO_OP
    : order->getDecomposition();
      
      text = new Normalizer(sourceText, decomp);
      if (text == NULL) {
    status = U_MEMORY_ALLOCATION_ERROR;
      }
    }
}


// This is the "real" constructor for this class; it constructs an iterator
// over the source text using the specified collator
CollationElementIterator::CollationElementIterator( const CharacterIterator& sourceText,
                                                    const RuleBasedCollator* order,
                                                    UErrorCode& status) 
: expIndex(0), 
  text(NULL),
  bufferAlias(NULL),
  ownBuffer(new VectorOfInt(2)),
  reorderBuffer(0),
  orderAlias(order)
{
    if (U_FAILURE(status)) {
        return;
    }

    // **** should I just drop this test? ****
    if ( sourceText.endIndex() != 0 )
    {
        //
        // A CollationElementIterator is really a two-layered beast.
        // Internally it uses a Normalizer to munge the source text
        // into a form where all "composed" Unicode characters (such as ü) are
        // split into a normal character and a combining accent character.  
        // Afterward, CollationElementIterator does its own processing to handle
        // expanding and contracting collation sequences, ignorables, and so on.
        //
      Normalizer::EMode decomp = order->getStrength() == Collator::IDENTICAL
        ? Normalizer::NO_OP
        : order->getDecomposition();
      
      text = new Normalizer(sourceText, decomp);
      if (text == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
      }
    }
}

CollationElementIterator::CollationElementIterator(const    CollationElementIterator& other)
    : expIndex(other.expIndex), text(0),
      ownBuffer(new VectorOfInt(2)),
      reorderBuffer(0)
{
    *this = other;
}

const   CollationElementIterator&
CollationElementIterator::operator=(const   CollationElementIterator& other)
{
    if (this != &other)
    {
        expIndex = other.expIndex;

        delete text;
        text = (Normalizer*)other.text->clone();

        if (other.bufferAlias == other.ownBuffer) {
            *ownBuffer = *other.ownBuffer;
            bufferAlias = ownBuffer;
        } else if (other.bufferAlias != NULL &&
                   other.bufferAlias == other.reorderBuffer) {
            if (reorderBuffer == NULL) {
                reorderBuffer = new VectorOfInt(*other.reorderBuffer);
            } else {
                *reorderBuffer = *other.reorderBuffer;
            }
            bufferAlias = reorderBuffer;
        } else {
            bufferAlias = other.bufferAlias;
        }
        orderAlias = other.orderAlias;
    }

    return *this;
}

CollationElementIterator::~CollationElementIterator()
{
    delete text;
    text = NULL;
    bufferAlias = NULL;
    orderAlias = NULL;
    delete ownBuffer;
    delete reorderBuffer;
}

UBool
CollationElementIterator::operator==(const CollationElementIterator& that) const
{
    if (this == &that)
    {
        return TRUE;
    }

    if (*text != *(that.text))
    {
        return FALSE;
    }

    if (((bufferAlias == NULL) != (that.bufferAlias == NULL)) ||
        (bufferAlias != NULL && *bufferAlias != *(that.bufferAlias)))
    {
        return FALSE;
    }

    if (expIndex != that.expIndex)
    {
        return FALSE;
    }

    if (orderAlias != that.orderAlias)
    {
        return FALSE;
    }

    return TRUE;
}

UBool
CollationElementIterator::operator!=(const CollationElementIterator& other) const
{
    return !(*this == other);
}

/**
 * Resets the cursor to the beginning of the string.
 */
void 
CollationElementIterator::reset()
{
  if (text != NULL)
    {
      text->reset();
      text->setMode(orderAlias->getDecomposition());
    }

  bufferAlias = NULL;
  expIndex = 0;
}

// Sets the source to the new source string.
void
CollationElementIterator::setText(const UnicodeString&  source,
                                        UErrorCode&      status)
{
    if (U_FAILURE(status))
    {
        return;
    }

    bufferAlias = 0;

    if (text == NULL)
    {
        text = new Normalizer(source, orderAlias->getDecomposition());
    }
    else
    {
        text->setText(source, status);
        text->setMode(orderAlias->getDecomposition());
    }
}

// Sets the source to the new character iterator.
void
CollationElementIterator::setText(CharacterIterator&  source,
                                        UErrorCode&      status)
{
    if (U_FAILURE(status)) {
        return;
    }

    bufferAlias = 0;

    if (text == NULL) {
        text = new Normalizer(source, orderAlias->getDecomposition());
    }
    else
    {
        text->setMode(orderAlias->getDecomposition());
        text->setText(source, status);
    }
}

/**
 * Get the ordering priority of the next character in the string.
 * @return the next character's ordering.  Returns NULLORDER if
 * the end of string is reached.
 */
int32_t
CollationElementIterator::next(UErrorCode& status)
{
    if (text == NULL || U_FAILURE(status))
    {
        return NULLORDER;
    }

    // Update the decomposition mode if necessary.
    text->setMode(orderAlias->getDecomposition());
    
    if (bufferAlias != NULL)
    {
        // bufferAlias needs a bit of an explanation.
        // When we hit an expanding character in the text, we call the order's
        // getExpandValues method to retrieve an array of the orderings for all
        // of the characters in the expansion (see the end of this method).
        // The first ordering is returned, and an alias to the orderings array
        // is saved so that the remaining orderings can be returned on subsequent
        // calls to next.  So, if the expanding buffer is not exhausted, 
        // all we have to do here is return the next ordering in the buffer.  
        if (expIndex < bufferAlias->size())
        {
            return strengthOrder(bufferAlias->at(expIndex++));
        }
        else
        {
            bufferAlias = NULL;
        }
    }

    // Gets the next character from the string using decomposition iterator.
    UChar32 ch = text->current();
    text->next();

    if (U_FAILURE(status))
    {
        return NULLORDER;
    }

    if (ch == Normalizer::DONE)
    {
        return NULLORDER;
    }
    
    // Ask the collator for this character's ordering.
    /* Used to be RuleBasedCollator.getUnicodeOrder().  It 
       can't be inlined in tblcoll.h file unfortunately. */
    int32_t value = ucmp32_get(orderAlias->data->mapping, ch);

    if (value == RuleBasedCollator::UNMAPPED)
    {
        // Returned an "unmapped" flag and save the character so it can be 
        // returned next time this method is called.
        if (ch == 0x0000) return ch;
        // \u0000 is not valid in C++'s UnicodeString
        ownBuffer->at(0) = UNMAPPEDCHARVALUE;
        ownBuffer->at(1) = ch << 16;
        bufferAlias = ownBuffer;
    }
    else {
        if (value >= RuleBasedCollator::CONTRACTCHARINDEX) {
            value = nextContractChar(ch, status);
        }
        if (value >= RuleBasedCollator::EXPANDCHARINDEX) {
            bufferAlias = orderAlias->getExpandValueList(value);
        }
        
        if (isThaiPreVowel(ch)) {
            UChar32 consonant = text->next();
            if (isThaiBaseConsonant(consonant)) {
                
                bufferAlias = makeReorderedBuffer((UChar)consonant, value, bufferAlias,
                                                  TRUE, status);
                
            }
            else {
                text->previous();
            }
        }
    }

    if (bufferAlias != NULL) {
        expIndex = 1;
        value = bufferAlias->at(0);
    }

    return strengthOrder(value);
}

 /**
  * Get the ordering priority of the previous collation element in the string.
  * @param status the error code status.
  * @return the previous element's ordering.  Returns NULLORDER if
  * the beginning of string is reached.
  */
int32_t
CollationElementIterator::previous(UErrorCode& status)
{
    if (text == NULL || U_FAILURE(status))
    {
        return NULLORDER;
    }

    text->setMode(orderAlias->getDecomposition());

    if (bufferAlias != NULL)
    {
        if (expIndex > 0)
        {
            return strengthOrder(bufferAlias->at(--expIndex));
        }

        bufferAlias = NULL;
    }

    UChar32 ch = text->previous();

    if (ch == Normalizer::DONE)
    {
        return NULLORDER;
    }
    /* Used to be RuleBasedCollator.getUnicodeOrder().  It 
       can't be inlined in tblcoll.h file unfortunately. */
    int32_t value = ucmp32_get(orderAlias->data->mapping, ch);

    if (value == RuleBasedCollator::UNMAPPED)
    {
        if (ch == 0x0000) return ch;
        ownBuffer->at(0) = UNMAPPEDCHARVALUE;
        ownBuffer->at(1) = ch << 16;
        bufferAlias = ownBuffer;
    }
    else {
        if (value >= RuleBasedCollator::CONTRACTCHARINDEX) {
            value = prevContractChar(ch, status);
        }
        if (value >= RuleBasedCollator::EXPANDCHARINDEX) {
            bufferAlias = orderAlias->getExpandValueList(value);
        }

        if (isThaiBaseConsonant(ch)) {

            UChar32 vowel = text->previous();
            if (isThaiPreVowel(vowel)) {
                bufferAlias = makeReorderedBuffer((UChar)vowel, value, bufferAlias,
                                                  FALSE, status);
            }
            else {
                text->next();
            }
        }
    }

    if (bufferAlias != NULL) {
        expIndex = bufferAlias->size()-1;
        value = bufferAlias->at(expIndex);
    }

    return strengthOrder(value);
}

int32_t
CollationElementIterator::strengthOrder(int32_t order) const
{
    Collator::ECollationStrength s = orderAlias->getStrength();
    // Mask off the unwanted differences.
    if (s == Collator::PRIMARY)
    {
        order &= RuleBasedCollator::PRIMARYDIFFERENCEONLY;
    } else if (s == Collator::SECONDARY)
    {
        order &= RuleBasedCollator::SECONDARYDIFFERENCEONLY;
    }
    return order;
}

UTextOffset
CollationElementIterator::getOffset() const
{
    // Since the DecompositionIterator is doing the work of iterating through
    // the text string, we can just ask it what its offset is.
    return (text != NULL) ? text->getIndex() : 0;
}

void 
CollationElementIterator::setOffset(UTextOffset newOffset, 
                                    UErrorCode& status)
{
    if (U_FAILURE(status))
    {
        return;
    }

    if (text != NULL)
    {
        text->setIndex(newOffset);
    }

    bufferAlias = NULL;
}

//============================================================
// privates
//============================================================

/**
 * Get the ordering priority of the next contracting character in the
 * string.
 * @param ch the starting character of a contracting character token
 * @return the next contracting character's ordering.  Returns NULLORDER
 * if the end of string is reached.
 */
int32_t
CollationElementIterator::nextContractChar(UChar32 ch,
                                           UErrorCode& status)
{
    // First get the ordering of this single character
    VectorOfPToContractElement *list = orderAlias->getContractValues((UChar)ch);
    EntryPair *pair = (EntryPair *)list->at(0);
    int32_t order = pair->value;

    // Now iterate through the chars following it and
    // look for the longest match
    key.remove();
    key += ch;

    while ((ch = text->current()) != Normalizer::DONE)
    {
        if (U_FAILURE(status))
        {
            return NULLORDER;
        }

        key += ch;

        int32_t n = RuleBasedCollator::getEntry(list, key, TRUE);

        if (n == RuleBasedCollator::UNMAPPED)
        {
            break;
        }
        text->next();

        pair = (EntryPair *)list->at(n);
        order = pair->value;
    }

    return order;
}

/**
 * Get the ordering priority of the previous contracting character in the
 * string.
 * @param ch the starting character of a contracting character token
 * @return the next contracting character's ordering.  Returns NULLORDER
 * if the end of string is reached.
 */
int32_t CollationElementIterator::prevContractChar(UChar32 ch,
                                                   UErrorCode &status)
{
    // First get the ordering of this single character
    VectorOfPToContractElement *list = orderAlias->getContractValues((UChar)ch);
    EntryPair *pair = (EntryPair *)list->at(0);
    int32_t order = pair->value;

    // Now iterate through the chars following it and
    // look for the longest match
    key.remove();
    key += ch;

    while ((ch = text->previous()) != Normalizer::DONE)
    {
        key += ch;

        int32_t n = RuleBasedCollator::getEntry(list, key, FALSE);

        if (n == RuleBasedCollator::UNMAPPED)
        {
            ch = text->next();

            if (U_FAILURE(status))
            {
                return NULLORDER;
            }

            break;
        }

        pair = (EntryPair *)list->at(n);
        order = pair->value;
    }

    return order;
}

/**
 * This method produces a buffer which contains the collation
 * elements for the two characters, with colFirst's values preceding
 * another character's.  Presumably, the other character precedes colFirst
 * in logical
 * order (otherwise you wouldn't need this method would you?).
 * The assumption is that the other char's value(s) have already been
 * computed.  If this char has a single element it is passed to this
 * method as lastValue, and lastExpasion is null.  If it has an
 * expasion it is passed in lastExpansion, and colLastValue is ignored.
 * This method may return the ownBuffer array as its value so ownBuffer
 * had better not be in use anywhere else.
 */
VectorOfInt* CollationElementIterator::makeReorderedBuffer(UChar colFirst,
                                                           int32_t lastValue,
                                                           VectorOfInt* lastExpansion,
                                                           UBool forward,
                                                           UErrorCode& status) {

    VectorOfInt* result;

    int32_t firstValue = ucmp32_get(orderAlias->data->mapping, colFirst);
    if (firstValue >= RuleBasedCollator::CONTRACTCHARINDEX) {
        firstValue = forward ? nextContractChar(colFirst, status)
                             : prevContractChar(colFirst, status);
    }

    VectorOfInt* firstExpansion = NULL;
    if (firstValue >= RuleBasedCollator::EXPANDCHARINDEX) {
        firstExpansion = orderAlias->getExpandValueList(firstValue);
    }

    if (!forward) {
        int32_t temp1 = firstValue;
        firstValue = lastValue;
        lastValue = temp1;
        VectorOfInt* temp2 = firstExpansion;
        firstExpansion = lastExpansion;
        lastExpansion = temp2;
    }

    if (firstExpansion == NULL && lastExpansion == NULL) {
        ownBuffer->at(0) = firstValue;
        ownBuffer->at(1) = lastValue;
        result = ownBuffer;
    }
    else {
        int32_t firstLength = firstExpansion==NULL? 1 : firstExpansion->size();
        int32_t lastLength = lastExpansion==NULL? 1 : lastExpansion->size();
        if (reorderBuffer == NULL) {
            reorderBuffer = new VectorOfInt(firstLength+lastLength);
        }
        // reorderdBuffer gets reused for the life of this object.
        // Since its internal buffer only grows, there is a danger
        // that it will get really, really big, and never shrink.  If
        // this is actually happening, insert code here to check for
        // the condition.  Something along the lines of:
        //! else if (reorderBuffer->size() >= 256 &&
        //!          (firstLength+lastLength) < 16) {
        //!     delete reorderBuffer;
        //!     reorderBuffer = new VectorOfInt(firstLength+lastLength);
        //! }
        // The specific numeric values need to be determined
        // empirically. [aliu]
        result = reorderBuffer;

        if (firstExpansion == NULL) {
            result->atPut(0, firstValue);
        }
        else {
            // System.arraycopy(firstExpansion, 0, result, 0, firstLength);
            *result = *firstExpansion;
        }

        if (lastExpansion == NULL) {
            result->atPut(firstLength, lastValue);
        }
        else {
            // System.arraycopy(lastExpansion, 0, result, firstLength, lastLength);
            for (int32_t i=0; i<lastLength; ++i) {
                result->atPut(firstLength + i, lastExpansion->at(i));
            }
        }
        result->setSize(firstLength+lastLength);
    }

    return result;
}
