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

#include "unicode/chariter.h"
#include "unicode/schriter.h"


UClassID StringCharacterIterator::fgClassID = 0;

StringCharacterIterator::StringCharacterIterator()
  : UCharCharacterIterator(),
    text()
{
  // NEVER DEFAULT CONSTRUCT!
}

StringCharacterIterator::StringCharacterIterator(const UnicodeString& text)
  : UCharCharacterIterator(text.fArray, text.length()),
    text(text)
{
    // we had set the input parameter's array, now we need to set our copy's array
    UCharCharacterIterator::text = this->text.fArray;
}

StringCharacterIterator::StringCharacterIterator(const UnicodeString& text,
                                                 UTextOffset pos)
  : UCharCharacterIterator(text.fArray, text.length(), pos),
    text(text)
{
    // we had set the input parameter's array, now we need to set our copy's array
    UCharCharacterIterator::text = this->text.fArray;
}

StringCharacterIterator::StringCharacterIterator(const UnicodeString& text,
                                                 UTextOffset begin,
                                                 UTextOffset end,
                                                 UTextOffset pos)
  : UCharCharacterIterator(text.fArray, text.length(), begin, end, pos),
    text(text)
{
    // we had set the input parameter's array, now we need to set our copy's array
    UCharCharacterIterator::text = this->text.fArray;
}

StringCharacterIterator::StringCharacterIterator(const StringCharacterIterator& that)
  : UCharCharacterIterator(that),
    text(that.text)
{
    // we had set the input parameter's array, now we need to set our copy's array
    UCharCharacterIterator::text = this->text.fArray;
}

StringCharacterIterator::~StringCharacterIterator() {
}

StringCharacterIterator&
StringCharacterIterator::operator=(const StringCharacterIterator& that) {
    UCharCharacterIterator::operator=(that);
    text = that.text;
    // we had set the input parameter's array, now we need to set our copy's array
    UCharCharacterIterator::text = this->text.fArray;
    return *this;
}

bool_t
StringCharacterIterator::operator==(const CharacterIterator& that) const {
    if (this == &that) {
        return TRUE;
    }

    // do not call UCharCharacterIterator::operator==()
    // because that checks for array pointer equality
    // while we compare UnicodeString objects

    if (getDynamicClassID() != that.getDynamicClassID()) {
        return FALSE;
    }

    StringCharacterIterator&    realThat = (StringCharacterIterator&)that;

    return text == realThat.text
        && pos == realThat.pos
        && begin == realThat.begin
        && end == realThat.end;
}

CharacterIterator*
StringCharacterIterator::clone() const {
    return new StringCharacterIterator(*this);
}

void
StringCharacterIterator::setText(const UnicodeString& newText) {
    text = newText;
    UCharCharacterIterator::setText(text.fArray, text.length());
}

void
StringCharacterIterator::getText(UnicodeString& result) {
    result = text;
}
