/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#include "unicode/chariter.h"

CharacterIterator::CharacterIterator(int32_t length)
  : textLength(length), pos(0), begin(0), end(length) {
  if(textLength < 0) {
    textLength = end = 0;
  }
}

CharacterIterator::CharacterIterator(int32_t length, UTextOffset position)
  : textLength(length), pos(position), begin(0), end(length) {
  if(textLength < 0) {
    textLength = end = 0;
  }
  if(pos < 0) {
    pos = 0;
  } else if(pos > end) {
    pos = end;
  }
}

CharacterIterator::CharacterIterator(int32_t length, UTextOffset begin, UTextOffset end, UTextOffset position)
 : textLength(length), pos(position), begin(begin), end(end) {
  if(textLength < 0) {
    textLength = 0;
  }
  if(begin < 0) {
    begin = 0;
  } else if(begin > textLength) {
    begin = textLength;
  }
  if(end < begin) {
    end = begin;
  } else if(end > textLength) {
    end = textLength;
  }
  if(pos < begin) {
    pos = begin;
  } else if(pos > end) {
    pos = end;
  }
}

CharacterIterator::CharacterIterator(const CharacterIterator &that) :
  textLength(that.textLength), pos(that.pos), begin(that.begin), end(that.end) {}

CharacterIterator &
CharacterIterator::operator=(const CharacterIterator &that) {
  textLength = that.textLength;
  pos = that.pos;
  begin = that.begin;
  end = that.end;
  return *this;
}

// implementing first[32]PostInc() directly in a subclass should be faster
// but these implementations make subclassing a little easier
UChar
CharacterIterator::firstPostInc(void) {
  setToStart();
  return nextPostInc();
}

UChar32
CharacterIterator::first32PostInc(void) {
  setToStart();
  return next32PostInc();
}
