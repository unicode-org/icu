/*
*******************************************************************************
* Copyright (C) 1998-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File schriter.cpp
*
* Modification History:
*
*   Date        Name        Description
*  05/05/99     stephen     Cleaned up.
*******************************************************************************
*/

#include "chariter.h"
#include "schriter.h"


UClassID StringCharacterIterator::fgClassID = 0;

StringCharacterIterator::StringCharacterIterator()
  : CharacterIterator(),
    text(),
    pos(0),
    begin(0),
    end(0)
{
  // NEVER DEFAULT CONSTRUCT!
}

StringCharacterIterator::StringCharacterIterator(const UnicodeString& text)
  : CharacterIterator(),
    text(text),
    pos(0),
    begin(0),
    end(text.length())
{}

StringCharacterIterator::StringCharacterIterator(const UnicodeString&    text,
                         UTextOffset              pos)
  : CharacterIterator(),
    text(text),
    pos(pos),
    begin(0),
    end(text.length())
{
  // the Java code checks the parameters and throws exceptions we've
  // decided to punt on this for the time being because changing this
  // constructor to accept an error code is an API change with
  // significant impact
}

StringCharacterIterator::StringCharacterIterator(const UnicodeString&    text,
                         UTextOffset             begin,
                         UTextOffset              end,
                         UTextOffset              pos)
  : CharacterIterator(),
    text(text),
    pos(pos),
    begin(begin),
    end(end)
{
  // the Java code checks the parameters and throws exceptions we've
  // decided to punt on this for the time being because changing this
  // constructor to accept an error code is an API change with
  // significant impact
}

StringCharacterIterator::StringCharacterIterator(const StringCharacterIterator& that)
  : CharacterIterator(that),
    text(that.text),
    pos(that.pos),
    begin(that.begin),
    end(that.end)
{
}

StringCharacterIterator::~StringCharacterIterator()
{}

StringCharacterIterator&
StringCharacterIterator::operator=(const StringCharacterIterator&   that)
{
  text = that.text;
  pos = that.pos;
  begin = that.begin;
  end = that.end;
  return *this;
}

bool_t
StringCharacterIterator::operator==(const CharacterIterator& that) const
{
  if (this == &that)
  return TRUE;
    
  if (getDynamicClassID() != that.getDynamicClassID())
  return FALSE;

  StringCharacterIterator&    realThat = (StringCharacterIterator&)that;

  return text == realThat.text
  && pos == realThat.pos
  && begin == realThat.begin
  && end == realThat.end;
}

int32_t
StringCharacterIterator::hashCode() const
{
  return text.hashCode() ^ pos ^ begin ^ end;
}

CharacterIterator*
StringCharacterIterator::clone() const
{
  return new StringCharacterIterator(*this);
}

UChar
StringCharacterIterator::first()
{
  pos = begin;
  return text.charAt(pos);
}

UChar
StringCharacterIterator::last()
{
  pos = end - 1;
  return text.charAt(pos);
}

UChar
StringCharacterIterator::setIndex(UTextOffset pos)
{
  // should check "pos" here and return an error code, but changing
  // this function would have significant impact across TIFC, so we
  // decided to hold off
  this->pos = pos;
  return text.charAt(pos);
}

UChar
StringCharacterIterator::current() const
{
  if (pos >= begin && pos < end)
    return text.charAt(pos);
  else
    return CharacterIterator::DONE;
}

UChar
StringCharacterIterator::next()
{
  if(pos < end - 1) {
    return text.charAt(++pos);
  }
  else {
    pos = end;
    return CharacterIterator::DONE;
  }
}

UChar
StringCharacterIterator::previous()
{
  if (pos > begin)
    return text.charAt(--pos);
  else
    return DONE;
}

UTextOffset
StringCharacterIterator::startIndex() const
{
  return begin;
}

UTextOffset
StringCharacterIterator::endIndex() const
{
  return end;
}

UTextOffset
StringCharacterIterator::getIndex() const
{
  return pos;
}

void
StringCharacterIterator::getText(UnicodeString& result)
{
  result = text;
}

