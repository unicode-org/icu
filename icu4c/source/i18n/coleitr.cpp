/*
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

/*
* File coleitr.cpp
*
* 
*
* Created by: Helena Shih
*
* Modification History:
*
*  Date      Name        Description
*
*  6/23/97   helena      Adding comments to make code more readable.
* 08/03/98   erm         Synched with 1.2 version of CollationElementIterator.java
* 12/10/99   aliu        Ported Thai collation support from Java.
* 01/25/01   swquek      Modified to a C++ wrapper calling C APIs (ucoliter.h)
* 02/19/01   swquek      Removed CollationElementsIterator() since it is 
*                        private constructor and no calls are made to it
*/

#include "unicode/coleitr.h"
#include "ucol_imp.h"
#include "cmemory.h"
#include "unicode/ustring.h"


/* Constants --------------------------------------------------------------- */

/* synwee : public can't remove */
int32_t const CollationElementIterator::NULLORDER = 0xffffffff;

/* CollationElementIterator public constructor/destructor ------------------ */

CollationElementIterator::CollationElementIterator(
                                         const CollationElementIterator& other) 
                                         : isDataOwned_(TRUE)
{
  UErrorCode status = U_ZERO_ERROR;
  m_data_ = ucol_openElements(other.m_data_->iteratordata_.coll, NULL, 0, 
                              &status);
  *this = other;
}

CollationElementIterator::~CollationElementIterator()
{
  if (isDataOwned_) {
    ucol_closeElements(m_data_);
  }
}

/* CollationElementIterator public methods --------------------------------- */

UTextOffset CollationElementIterator::getOffset() const
{
  return ucol_getOffset(m_data_);
}

/**
* Get the ordering priority of the next character in the string.
* @return the next character's ordering. Returns NULLORDER if an error has 
*         occured or if the end of string has been reached
*/
int32_t CollationElementIterator::next(UErrorCode& status)
{
  return ucol_next(m_data_, &status);
}

UBool CollationElementIterator::operator!=(
                                  const CollationElementIterator& other) const
{
  return !(*this == other);
}

UBool CollationElementIterator::operator==(
                                    const CollationElementIterator& that) const
{
    UBool result = TRUE;

    if (this == &that) {
        return TRUE;
    }
  
    if (m_data_ == that.m_data_) {
        return TRUE;
    }

    // option comparison
    result = this->m_data_->normalization_ == that.m_data_->normalization_ 
             && this->m_data_->reset_ == that.m_data_->reset_ &&  
             this->m_data_->iteratordata_.coll == 
             that.m_data_->iteratordata_.coll;

    int thislength = 0;
    if (this->m_data_->iteratordata_.flags & UCOL_ITER_HASLEN) {
        thislength = this->m_data_->iteratordata_.endp -
                     this->m_data_->iteratordata_.string;
    }
    else {
        thislength = u_strlen(this->m_data_->iteratordata_.string);
    }
    int thatlength = 0;
    if (that.m_data_->iteratordata_.endp != NULL) {
        thatlength = that.m_data_->iteratordata_.endp -
                     that.m_data_->iteratordata_.string;
    }
    else {
        thatlength = u_strlen(that.m_data_->iteratordata_.string);
    }

    if (thislength != thatlength) {
        return FALSE;
    }

    result = result && (uprv_memcmp(this->m_data_->iteratordata_.string, 
                         that.m_data_->iteratordata_.string, 
                         thislength * sizeof(UChar)) == 0);
    result = result && (this->getOffset() == that.getOffset());
  
    return result;
}

/**
* Get the ordering priority of the previous collation element in the string.
* @param status the error code status.
* @return the previous element's ordering. Returns NULLORDER if an error has 
*         occured or if the start of string has been reached.
*/
int32_t CollationElementIterator::previous(UErrorCode& status)
{
  return ucol_previous(m_data_, &status);
}

/**
* Resets the cursor to the beginning of the string.
*/
void CollationElementIterator::reset()
{
  ucol_reset(m_data_);
}

void CollationElementIterator::setOffset(UTextOffset newOffset, 
                                         UErrorCode& status)
{
  ucol_setOffset(m_data_, newOffset, &status);
}

/**
* Sets the source to the new source string.
*/
void CollationElementIterator::setText(const UnicodeString& source,
                                       UErrorCode& status)
{
  if (U_FAILURE(status)) {
    return;
  }
  	
  int32_t length = source.length();
  UChar *string = NULL;
  if (m_data_->isWritable && m_data_->iteratordata_.string != NULL) {
    uprv_free(m_data_->iteratordata_.string);
  }
  m_data_->isWritable = TRUE;
  if (length > 0) {
    string = (UChar *)uprv_malloc(sizeof(UChar) * length);
    source.extract(0, length, string);
  }
  else {
    string = (UChar *)uprv_malloc(sizeof(UChar));
    *string = 0;
  }
  init_collIterate(m_data_->iteratordata_.coll, string, length, 
                   &m_data_->iteratordata_);

  m_data_->reset_   = TRUE;
}

// Sets the source to the new character iterator.
void CollationElementIterator::setText(CharacterIterator& source, 
                                       UErrorCode& status)
{
  if (U_FAILURE(status)) 
    return;
    
  int32_t length = source.getLength();
  UChar *buffer = NULL;

  if (length == 0) {
    buffer = (UChar *)uprv_malloc(sizeof(UChar));
    *buffer = 0;
  }
  else {
      buffer = (UChar *)uprv_malloc(sizeof(UChar) * length);
      /* 
      Using this constructor will prevent buffer from being removed when
      string gets removed
      */
      UnicodeString string;
      source.getText(string);
      string.extract(0, length, buffer);
  }
  
  if (m_data_->isWritable && m_data_->iteratordata_.string != NULL)
    uprv_free(m_data_->iteratordata_.string);
  m_data_->isWritable = TRUE;
  init_collIterate(m_data_->iteratordata_.coll, buffer, length, 
                   &m_data_->iteratordata_);
  m_data_->reset_   = TRUE;
}

int32_t CollationElementIterator::strengthOrder(int32_t order) const
{
  UCollationStrength s = ucol_getStrength(m_data_->iteratordata_.coll);
  // Mask off the unwanted differences.
  if (s == UCOL_PRIMARY)
    order &= RuleBasedCollator::PRIMARYDIFFERENCEONLY;
  else 
    if (s == UCOL_SECONDARY)
      order &= RuleBasedCollator::SECONDARYDIFFERENCEONLY;
    
  return order;
}

/* CollationElementIterator private constructors/destructors --------------- */

/** 
* This is the "real" constructor for this class; it constructs an iterator
* over the source text using the specified collator
*/
CollationElementIterator::CollationElementIterator(
                                               const UnicodeString& sourceText,
                                               const RuleBasedCollator* order,
                                               UErrorCode& status)
                                               : isDataOwned_(TRUE)
{
  if (U_FAILURE(status))
    return;
 
  int32_t length = sourceText.length();
  UChar *string = NULL;
  
  if (length > 0) {
      string = (UChar *)uprv_malloc(sizeof(UChar) * length);
      /* 
      Using this constructor will prevent buffer from being removed when
      string gets removed
      */
      sourceText.extract(0, length, string);
  }
  else {
      string = (UChar *)uprv_malloc(sizeof(UChar));
      *string = 0;
  }
  m_data_ = ucol_openElements(order->ucollator, string, length, &status);
  m_data_->isWritable = TRUE;
}

/** 
* This is the "real" constructor for this class; it constructs an iterator over 
* the source text using the specified collator
*/
CollationElementIterator::CollationElementIterator(
                                           const CharacterIterator& sourceText,
                                           const RuleBasedCollator* order,
                                           UErrorCode& status)
                                           : isDataOwned_(TRUE)
{
  if (U_FAILURE(status))
    return;
    
  // **** should I just drop this test? ****
  /*
  if ( sourceText.endIndex() != 0 )
  {
    // A CollationElementIterator is really a two-layered beast.
    // Internally it uses a Normalizer to munge the source text into a form 
    // where all "composed" Unicode characters (such as ü) are split into a 
    // normal character and a combining accent character.  
    // Afterward, CollationElementIterator does its own processing to handle
    // expanding and contracting collation sequences, ignorables, and so on.
    
    Normalizer::EMode decomp = order->getStrength() == Collator::IDENTICAL
                               ? Normalizer::NO_OP : order->getDecomposition();
      
    text = new Normalizer(sourceText, decomp);
    if (text == NULL)
      status = U_MEMORY_ALLOCATION_ERROR;    
  }
  */
  int32_t length = sourceText.getLength();
  UChar *buffer;
  if (length > 0) {
      buffer = (UChar *)uprv_malloc(sizeof(UChar) * length);
      /* 
      Using this constructor will prevent buffer from being removed when
      string gets removed
      */
      UnicodeString string(buffer, length, length);
      ((CharacterIterator &)sourceText).getText(string);
      string.extract(0, length, buffer);
  }
  else {
      buffer = (UChar *)uprv_malloc(sizeof(UChar));
      *buffer = 0;
  }
  m_data_ = ucol_openElements(order->ucollator, buffer, length, &status);
  m_data_->isWritable = TRUE;
}

/* CollationElementIterator protected methods ----------------------------- */

const CollationElementIterator& CollationElementIterator::operator=(
                                         const CollationElementIterator& other)
{
  if (this != &other)
  {
      UCollationElements *ucolelem      = this->m_data_;
      UCollationElements *otherucolelem = other.m_data_;
      collIterate        *coliter       = &(ucolelem->iteratordata_);
      collIterate        *othercoliter  = &(otherucolelem->iteratordata_);
      int                length         = 0;
      
      // checking only UCOL_ITER_HASLEN is not enough here as we may be in 
      // the normalization buffer
      if (othercoliter->endp != NULL) {
          length = othercoliter->endp - othercoliter->string;
      }
      else {
          if (othercoliter->string == NULL) {
              length = 0;
          }
          else {
            length = u_strlen(othercoliter->string);
          }
      }
                                    

      ucolelem->normalization_ = otherucolelem->normalization_;
      ucolelem->reset_         = otherucolelem->reset_;
      ucolelem->isWritable     = TRUE;
    
      /* create a duplicate of string */
      if (length > 0) {
          coliter->string = (UChar *)uprv_malloc(length * sizeof(UChar));
          uprv_memcpy(coliter->string, othercoliter->string,
                      length * sizeof(UChar));
      }
      else {
          coliter->string = NULL;
      }

      /* start and end of string */
      coliter->endp = coliter->string + length;

      /* handle writable buffer here */
      
      if (othercoliter->flags & UCOL_ITER_INNORMBUF) {
          uint32_t wlength = u_strlen(othercoliter->writableBuffer) + 1;
          if (wlength < coliter->writableBufSize) {
              uprv_memcpy(coliter->stackWritableBuffer, 
                        othercoliter->stackWritableBuffer, 
                        othercoliter->writableBufSize * sizeof(UChar));
          }
          else {
              if (coliter->writableBuffer != coliter->stackWritableBuffer) {
                  delete coliter->writableBuffer;
              }
              coliter->writableBuffer = (UChar *)uprv_malloc(
                                         wlength * sizeof(UChar));
              uprv_memcpy(coliter->writableBuffer, 
                          othercoliter->writableBuffer,
                          wlength * sizeof(UChar));
              coliter->writableBufSize = wlength;
          }
      }
         
      /* current position */
      if (othercoliter->pos >= othercoliter->string && 
          othercoliter->pos <= othercoliter->endp) {
          coliter->pos = coliter->string + 
                        (othercoliter->pos - othercoliter->string);
      }
      else {
        coliter->pos = coliter->writableBuffer + 
                        (othercoliter->pos - othercoliter->writableBuffer);
      }

      /* CE buffer */
      uprv_memcpy(coliter->CEs, othercoliter->CEs, 
                  UCOL_EXPAND_CE_BUFFER_SIZE * sizeof(uint32_t));
      coliter->toReturn = coliter->CEs + 
                         (othercoliter->toReturn - othercoliter->CEs);
      coliter->CEpos    = othercoliter->CEs + 
                         (othercoliter->CEpos - othercoliter->CEs);
    
      coliter->fcdPosition = coliter->string + 
                            (othercoliter->fcdPosition - othercoliter->string);
      coliter->flags       = othercoliter->flags | UCOL_ITER_HASLEN;
      coliter->origFlags   = othercoliter->origFlags;
      coliter->coll = othercoliter->coll;
      this->isDataOwned_ = TRUE;
  }

  return *this;
}


