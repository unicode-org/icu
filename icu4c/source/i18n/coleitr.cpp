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
//=============================================================================

#include "sortkey.h"
#include "coleitr.h"

#include "chariter.h"
#include "tables.h"
#include "normlzr.h"
#include "unicode.h"

int32_t const CollationElementIterator::NULLORDER = 0xffffffff;
int32_t const CollationElementIterator::UNMAPPEDCHARVALUE = 0x7fff0000;


// This private method will never be called, but it makes the linker happy

CollationElementIterator::CollationElementIterator()
: expIndex(0),
  text(0),
  swapOrder(0),
  bufferAlias(0),
  orderAlias(0)
{
}

// This private method will never be called, but it makes the linker happy

CollationElementIterator::CollationElementIterator(const RuleBasedCollator* order)
: expIndex(0),
  text(0),
  bufferAlias(0),
  swapOrder(0),
  orderAlias(order)
{
}

// This is the "real" constructor for this class; it constructs an iterator
// over the source text using the specified collator
CollationElementIterator::CollationElementIterator( const UnicodeString& sourceText,
                                                    const RuleBasedCollator* order,
                                                    UErrorCode& status) 
: expIndex(0), 
  swapOrder(0),
  text(NULL),
  bufferAlias(NULL),
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
  swapOrder(0),
  text(NULL),
  bufferAlias(NULL),
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
    : expIndex(other.expIndex), text(0), swapOrder(other.swapOrder)
{
    text = (Normalizer*) other.text->clone();
    bufferAlias = other.bufferAlias;
    orderAlias = other.orderAlias;
}

const   CollationElementIterator&
CollationElementIterator::operator=(const   CollationElementIterator& other)
{
    if (this != &other)
    {
        expIndex = other.expIndex;
        swapOrder = other.swapOrder;

        delete text;
        text = (Normalizer*)other.text->clone();

        bufferAlias = other.bufferAlias;
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
}

bool_t
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

    if (swapOrder != that.swapOrder)
    {
        return FALSE;
    }

    if (*bufferAlias != *(that.bufferAlias))
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

bool_t
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
  swapOrder = 0;
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
    swapOrder = 0;
    expIndex = 0;

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
    swapOrder = 0;
    expIndex = 0;

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
            expIndex = 0;
        }
    }
    else if (swapOrder != 0)
    {
        // If we find a character with no order, we return the marking
        // flag, UNMAPPEDCHARVALUE, 0x7fff0000, and then the character 
        // itself shifted left 16 bits as orders.  At this point, the
        // UNMAPPEDCHARVALUE flag has already been returned by the code
        // below, so just return the shifted character here.
        int32_t order = swapOrder << 16;

        swapOrder = 0;

        return order;
    }

    // Gets the next character from the string using decomposition iterator.
    UChar ch = text->current();
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
    int32_t value = orderAlias->getUnicodeOrder(ch);

    if (value == RuleBasedCollator::UNMAPPED)
    {
        // Returned an "unmapped" flag and save the character so it can be 
        // returned next time this method is called.
        if (ch == 0x0000) return ch;
        swapOrder = ch;  // \u0000 is not valid in C++'s UnicodeString
        return UNMAPPEDCHARVALUE;
    }
    
    if (value >= RuleBasedCollator::CONTRACTCHARINDEX)
    {
        value = nextContractChar(ch, status);
    }

    if (value >= RuleBasedCollator::EXPANDCHARINDEX)
    {
        bufferAlias = orderAlias->getExpandValueList(value);
        expIndex = 0;
        value = bufferAlias->at(expIndex++);
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
        expIndex = 0;
    }
    else if (swapOrder != 0)
    {
        int32_t order = swapOrder << 16;

        swapOrder = 0;
        return order;
    }

    UChar ch = text->previous();

    if (ch == Normalizer::DONE)
    {
        return NULLORDER;
    }

    int32_t value = orderAlias->getUnicodeOrder(ch);

    if (value == RuleBasedCollator::UNMAPPED)
    {
        if (ch == 0x0000) return ch;
        swapOrder = UNMAPPEDCHARVALUE;
        return ch;
    }
    
    if (value >= RuleBasedCollator::CONTRACTCHARINDEX)
    {
        value = prevContractChar(ch, status);
    }

    if (value >= RuleBasedCollator::EXPANDCHARINDEX)
    {
        bufferAlias = orderAlias->getExpandValueList(value);
        expIndex = bufferAlias->size();
        value = bufferAlias->at(--expIndex);
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
    expIndex = 0;
    swapOrder = 0;
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
CollationElementIterator::nextContractChar(UChar ch,
                                           UErrorCode& status)
{
    // First get the ordering of this single character
    VectorOfPToContractElement *list = orderAlias->getContractValues(ch);
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
int32_t CollationElementIterator::prevContractChar(UChar ch,
                                                   UErrorCode &status)
{
    // First get the ordering of this single character
    VectorOfPToContractElement *list = orderAlias->getContractValues(ch);
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
