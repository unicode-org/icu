/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File schriter.h
*
* Modification History:
*
*   Date        Name        Description
*  05/05/99     stephen     Cleaned up.
*******************************************************************************
*/

#ifndef SCHRITER_H
#define SCHRITER_H

#include "unicode/utypes.h"
#include "unicode/chariter.h"
#include "unicode/uchriter.h"

/**
 * A concrete subclass of CharacterIterator that iterates over the
 * characters (code units or code points) in a UnicodeString.
 * It's possible not only to create an
 * iterator that iterates over an entire UnicodeString, but also to
 * create one that iterates over only a subrange of a UnicodeString
 * (iterators over different subranges of the same UnicodeString don't
 * compare equal).
 */
class U_COMMON_API StringCharacterIterator : public UCharCharacterIterator {
public:
  /**
   * Create an iterator over the UnicodeString referred to by "text".
   * The UnicodeString object is copied.
   * The iteration range is the whole string, and the starting position is 0.
   * @stable
   */
  StringCharacterIterator(const UnicodeString& text);

  /**
   * Create an iterator over the UnicodeString referred to by "text".
   * The iteration range is the whole string, and the starting
   * position is specified by "pos".  If "pos" is outside the valid
   * iteration range, the behavior of this object is undefined.  
   * @stable
   */
  StringCharacterIterator(const UnicodeString&    text,
              UTextOffset              pos);

  /**
   * Create an iterator over the UnicodeString referred to by "text".
   * The UnicodeString object is copied.
   * The iteration range begins with the code unit specified by
   * "begin" and ends with the code unit BEFORE the code unit specfied
   * by "end".  The starting position is specified by "pos".  If
   * "begin" and "end" don't form a valid range on "text" (i.e., begin
   * >= end or either is negative or greater than text.size()), or
   * "pos" is outside the range defined by "begin" and "end", the
   * behavior of this iterator is undefined.  
   * @stable
   */
  StringCharacterIterator(const UnicodeString&    text,
              UTextOffset              begin,
              UTextOffset              end,
              UTextOffset              pos);

  /**
   * Copy constructor.  The new iterator iterates over the same range
   * of the same string as "that", and its initial position is the
   * same as "that"'s current position.  
   * The UnicodeString object in "that" is copied.
   * @stable
   */
  StringCharacterIterator(const StringCharacterIterator&  that);

  /**
   * Destructor.  
   * @stable
   */
  virtual ~StringCharacterIterator();

  /**
   * Assignment operator.  *this is altered to iterate over the same
   * range of the same string as "that", and refers to the same
   * character within that string as "that" does.  
   * @stable
   */
  StringCharacterIterator&
  operator=(const StringCharacterIterator&    that);

  /**
   * Returns true if the iterators iterate over the same range of the
   * same string and are pointing at the same character.  
   * @stable
   */
  virtual bool_t          operator==(const CharacterIterator& that) const;

  /**
   * Returns a new StringCharacterIterator referring to the same
   * character in the same range of the same string as this one.  The
   * caller must delete the new iterator.  
   * @draft
   */
  virtual CharacterIterator* clone(void) const;
                                
  /**
   * Sets the iterator to iterate over the provided string.
   * @draft
   */
  void setText(const UnicodeString& newText);

  /**
   * Copies the UnicodeString under iteration into the UnicodeString
   * referred to by "result".  Even if this iterator iterates across
   * only a part of this string, the whole string is copied.  @param
   * result Receives a copy of the text under iteration.  
   * @stable
   */
  virtual void            getText(UnicodeString& result);

  /**
   * Return a class ID for this object (not really public) 
   * @stable
   */
  virtual UClassID         getDynamicClassID(void) const 
    { return getStaticClassID(); }

  /**
   * Return a class ID for this class (not really public) 
   * @stable
   */
  static UClassID          getStaticClassID(void) 
    { return (UClassID)(&fgClassID); }

protected:
  StringCharacterIterator();
  void setText(const UChar* newText, int32_t newTextLength);
        
  UnicodeString            text;

  static UClassID           fgClassID;
};

#endif
