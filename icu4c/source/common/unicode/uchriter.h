/*
**********************************************************************
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#ifndef UCHRITER_H
#define UCHRITER_H

#include "unicode/utypes.h"
#include "unicode/chariter.h"


/**
 * A concrete subclass of CharacterIterator that iterates over the
 * characters in a UnicodeString.  It's possible not only to create an
 * iterator that iterates over an entire UnicodeString, but also to
 * create only that iterates over only a subrange of a UnicodeString
 * (iterators over different subranges of the same UnicodeString don't
 * compare equal).  */
class U_COMMON_API UCharCharacterIterator : public CharacterIterator {
public:
  /**
   * Create an iterator over the UnicodeString referred to by "text".
   * The iteration range is the whole string, and the starting
   * position is 0.  
   * @stable
   */
  UCharCharacterIterator(const UChar* text, int32_t len);

  /**
   * Copy constructor.  The new iterator iterates over the same range
   * of the same string as "that", and its initial position is the
   * same as "that"'s current position.  
   * @stable
   */
  UCharCharacterIterator(const UCharCharacterIterator&  that);

  /**
   * Destructor. 
   * @stable
   */
  ~UCharCharacterIterator();

  /**
   * Assignment operator.  *this is altered to iterate over the sane
   * range of the same string as "that", and refers to the same
   * character within that string as "that" does.  
   * @stable
   */
  UCharCharacterIterator&
  operator=(const UCharCharacterIterator&    that);

  /**
   * Returns true if the iterators iterate over the same range of the
   * same string and are pointing at the same character.  
   * @stable
   */
  virtual bool_t          operator==(const CharacterIterator& that) const;

  /**
   * Generates a hash code for this iterator.  
   * @stable
   */
  virtual int32_t         hashCode(void) const;

  /**
   * Returns a new StringCharacterIterator referring to the same
   * character in the same range of the same string as this one.  The
   * caller must delete the new iterator.  
   * @stable
   */
  virtual CharacterIterator* clone(void) const;
                                
  /**
   * Sets the iterator to refer to the first character in its
   * iteration range, and returns that character, 
   * @draft
   */
  virtual UChar         first(void);

  /**
   * Sets the iterator to refer to the last character in its iteration
   * range, and returns that character.  
   * @draft
   */
  virtual UChar         last(void);

  /**
   * Sets the iterator to refer to the "position"-th character in the
   * UnicodeString the iterator refers to, and returns that character.
   * If the index is outside the iterator's iteration range, the
   * behavior of the iterator is undefined.  
   * @draft
   */
  virtual UChar         setIndex(UTextOffset pos);

  /**
   * Returns the character the iterator currently refers to.  
   * @draft
   */
  virtual UChar         current(void) const;

  /**
   * Advances to the next character in the iteration range (toward
   * last()), and returns that character.  If there are no more
   * characters to return, returns DONE.  
   * @draft
   */
  virtual UChar         next(void);

  /**
   * Advances to the previous character in the iteration rance (toward
   * first()), and returns that character.  If there are no more
   * characters to return, returns DONE.  
   * @draft
   */
  virtual UChar         previous(void);

  /**
   * Returns the numeric index of the first character in this
   * iterator's iteration range.  
   * @stable
   */
  virtual UTextOffset      startIndex(void) const;

  /**
   * Returns the numeric index of the character immediately BEYOND the
   * last character in this iterator's iteration range.  
   * @stable
   */
  virtual UTextOffset      endIndex(void) const;

  /**
   * Returns the numeric index in the underlying UnicodeString of the
   * character the iterator currently refers to (i.e., the character
   * returned by current()).  
   * @stable
   */
  virtual UTextOffset      getIndex(void) const;

  /**
   * Sets the iterator to iterate over a new range of text
   * @draft
   */
  virtual void             setText(const UChar* newText,
                                   int32_t newTextLength);
  
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

private:
  UCharCharacterIterator();
        
  const UChar*            text;
  UTextOffset              pos;
  UTextOffset              begin;
  UTextOffset              end;

  static char             fgClassID;
};

#endif



