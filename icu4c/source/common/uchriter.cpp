/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998-1999      *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*/

#include "uchriter.h"

UCharCharacterIterator::UCharCharacterIterator(const UChar* text,
                           int32_t textLength)
  : CharacterIterator(),
  text(text),
  pos(0),
  begin(0),
  end(textLength)
{
}

UCharCharacterIterator::UCharCharacterIterator(const UCharCharacterIterator& that)
: CharacterIterator(that),
  text(that.text),
  pos(that.pos),
  begin(that.begin),
  end(that.end)
{
}

UCharCharacterIterator&
UCharCharacterIterator::operator=(const UCharCharacterIterator&   that)
{
    text = that.text;
    pos = that.pos;
    begin = that.begin;
    end = that.end;
    return *this;
}

UCharCharacterIterator::~UCharCharacterIterator()
{}

bool_t
UCharCharacterIterator::operator==(const CharacterIterator& that) const
{
    if (this == &that)
        return TRUE;
    
    if (getDynamicClassID() != that.getDynamicClassID())
        return FALSE;

    UCharCharacterIterator&    realThat = (UCharCharacterIterator&)that;

    return text == realThat.text
        && pos == realThat.pos
        && begin == realThat.begin
        && end == realThat.end;
}

int32_t
UCharCharacterIterator::hashCode() const
{
    return pos ^ begin ^ end;
}

CharacterIterator*
UCharCharacterIterator::clone() const
{
    return new UCharCharacterIterator(*this);
}

UChar
UCharCharacterIterator::first()
{
    pos = begin;
    return text[pos];
}

UChar
UCharCharacterIterator::last()
{
    pos = end - 1;
    return text[pos];
}

UChar
UCharCharacterIterator::setIndex(UTextOffset pos)
{
    // should check "pos" here and return an error code, but changing this
    // function would have significant impact across TIFC, so we decided to hold off
    this->pos = pos;
    return text[pos];
}

UChar
UCharCharacterIterator::current() const
{
    if (pos >= begin && pos < end)
        return text[pos];
    else
        return CharacterIterator::DONE;
}

UChar
UCharCharacterIterator::next()
{
    if (pos < end - 1)
    {
        pos += 1;
        return text[pos];
    }
    else
    {
        pos = end;
        return CharacterIterator::DONE;
    }
}

UChar
UCharCharacterIterator::previous()
{
    if (pos > begin)
        return text[--pos];
    else
        return DONE;
}

UTextOffset
UCharCharacterIterator::startIndex() const
{
    return begin;
}

UTextOffset
UCharCharacterIterator::endIndex() const
{
    return end;
}

UTextOffset
UCharCharacterIterator::getIndex() const
{
    return pos;
}

void
UCharCharacterIterator::getText(UnicodeString& result)
{
    result = UnicodeString(text, end);
}

char UCharCharacterIterator::fgClassID = 0;
