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
 * characters (code units or code points) in a UChar array.
 * It's possible not only to create an
 * iterator that iterates over an entire UChar array, but also to
 * create one that iterates over only a subrange of a UChar array
 * (iterators over different subranges of the same UChar array don't
 * compare equal).
 */
class U_COMMON_API UCharCharacterIterator : public CharacterIterator {
public:
  /**
   * Create an iterator over the UChar array referred to by "text".
   * The iteration range is 0 to <code>len-1</code>.
   * text is only aliased, not adopted (the
   * destructor will not delete it).
   * @stable
   */
  UCharCharacterIterator(const UChar* text, int32_t len);

  /**
   * Create an iterator over the UChar array referred to by "text".
   * The iteration range is 0 to <code>len-1</code>.
   * text is only aliased, not adopted (the
   * destructor will not delete it).
   * The starting
   * position is specified by "pos". If "pos" is outside the valid
   * iteration range, the behavior of this object is undefined.
   * @stable
   */
  UCharCharacterIterator(const UChar* text, int32_t len,
                         UTextOffset pos);

  /**
   * Create an iterator over the UChar array referred to by "text".
   * The iteration range is 0 to <code>end-1</code>.
   * text is only aliased, not adopted (the
   * destructor will not delete it).
   * The starting
   * position is specified by "pos". If begin and end do not
   * form a valid iteration range or "pos" is outside the valid
   * iteration range, the behavior of this object is undefined.
   * @stable
   */
  UCharCharacterIterator(const UChar* text, int32_t len,
                         UTextOffset begin,
                         UTextOffset end,
                         UTextOffset pos);

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
   * Returns a new UCharCharacterIterator referring to the same
   * character in the same range of the same string as this one.  The
   * caller must delete the new iterator.  
   * @stable
   */
  virtual CharacterIterator* clone(void) const;
                                
  /**
   * Sets the iterator to refer to the first code unit in its
   * iteration range, and returns that code unit, 
   * @draft
   */
  virtual UChar         first(void);

  /**
   * Sets the iterator to refer to the first code point in its
   * iteration range, and returns that code point, 
   * @draft
   */
  virtual UChar32       first32(void);

  virtual UTextOffset   setToStart();

  /**
   * Sets the iterator to refer to the last code unit in its iteration
   * range, and returns that code unit.  
   * @draft
   */
  virtual UChar         last(void);

  /**
   * Sets the iterator to refer to the last code point in its iteration
   * range, and returns that code point.  
   * @draft
   */
  virtual UChar32       last32(void);

  virtual UTextOffset   setToEnd();

  /**
   * Sets the iterator to refer to the "position"-th code unit in the
   * UChar array the iterator refers to, and returns that code unit.
   * If the index is outside the iterator's iteration range, the
   * behavior of the iterator is undefined.  
   * @draft
   */
  virtual UChar         setIndex(UTextOffset pos);

  /**
   * Sets the iterator to refer to the "position"-th code point in the
   * UChar array the iterator refers to, and returns that code point.
   * If the index is outside the iterator's iteration range, the
   * behavior of the iterator is undefined.  
   * @draft
   */
  virtual UChar32       setIndex32(UTextOffset pos);

  /**
   * Returns the code unit the iterator currently refers to.  
   * @draft
   */
  virtual UChar         current(void) const;

  /**
   * Returns the code point the iterator currently refers to.  
   * @draft
   */
  virtual UChar32       current32(void) const;

  /**
   * Advances to the next code unit in the iteration range (toward
   * last()), and returns that code unit.  If there are no more
   * code units to return, returns DONE.  
   * @draft
   */
  virtual UChar         next(void);

  virtual UChar         nextPostInc(void);
        
  /**
   * Advances to the next code point in the iteration range (toward
   * last()), and returns that code point.  If there are no more
   * code points to return, returns DONE.  
   * @draft
   */
  virtual UChar32       next32(void);

  virtual UChar32       next32PostInc(void);
        
  virtual bool_t        hasNext();

  /**
   * Advances to the previous code unit in the iteration rance (toward
   * first()), and returns that code unit.  If there are no more
   * code units to return, returns DONE.  
   * @draft
   */
  virtual UChar         previous(void);

  /**
   * Advances to the previous code point in the iteration rance (toward
   * first()), and returns that code point.  If there are no more
   * code points to return, returns DONE.  
   * @draft
   */
  virtual UChar32       previous32(void);

  virtual bool_t        hasPrevious();

  /**
   * Returns the numeric index of the first code unit in this
   * iterator's iteration range.  
   * @stable
   */
  virtual UTextOffset      startIndex(void) const;

  /**
   * Returns the numeric index of the code unit immediately BEYOND the
   * last code unit in this iterator's iteration range.  
   * @stable
   */
  virtual UTextOffset      endIndex(void) const;

  /**
   * Returns the numeric index in the underlying UChar array of the
   * code unit the iterator currently refers to (i.e., the code unit
   * returned by current()).  
   * @stable
   */
  virtual UTextOffset      getIndex(void) const;

  /**
   * Sets the iterator to iterate over a new range of text
   * @draft
   */
  void setText(const UChar* newText, int32_t newTextLength);
  
  /**
   * Copies the UChar array under iteration into the UnicodeString
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
  UCharCharacterIterator();
        
  const UChar*            text;
  int32_t                  textLength; // need this for correct getText() and hashCode()
  UTextOffset              pos;
  UTextOffset              begin;
  UTextOffset              end;

  static char             fgClassID;
};

#endif
