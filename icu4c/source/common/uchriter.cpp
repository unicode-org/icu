/*
*******************************************************************************
* Copyright (C) 1998-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

#include "unicode/uchriter.h"
#include "uhash.h"

UCharCharacterIterator::UCharCharacterIterator()
  : CharacterIterator(),
  text(0),
  textLength(0),
  pos(0),
  begin(0),
  end(0)
{
    // never default construct!
}

UCharCharacterIterator::UCharCharacterIterator(const UChar* text,
                                               int32_t textLength)
  : CharacterIterator(),
  text(text),
  textLength(textLength),
  pos(0),
  begin(0),
  end(textLength)
{
    if(text == 0 || textLength < 0) {
        textLength = end = 0;
    }
}

UCharCharacterIterator::UCharCharacterIterator(const UChar* text,
                                               int32_t textLength,
                                               UTextOffset pos)
  : CharacterIterator(),
  text(text),
  textLength(textLength),
  pos(pos),
  begin(0),
  end(textLength)
{
    if(text == 0 || textLength < 0) {
        textLength = end = 0;
    }
    if(pos < 0) {
        pos = 0;
    } else if(pos > end) {
        pos = end;
    }
}

UCharCharacterIterator::UCharCharacterIterator(const UChar* text,
                                               int32_t textLength,
                                               UTextOffset begin,
                                               UTextOffset end,
                                               UTextOffset pos)
  : CharacterIterator(),
  text(text),
  textLength(textLength),
  pos(pos),
  begin(begin),
  end(end)
{
    if(text == 0 || textLength < 0) {
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

UCharCharacterIterator::UCharCharacterIterator(const UCharCharacterIterator& that)
: CharacterIterator(that),
  text(that.text),
  textLength(that.textLength),
  pos(that.pos),
  begin(that.begin),
  end(that.end)
{
}

UCharCharacterIterator&
UCharCharacterIterator::operator=(const UCharCharacterIterator& that) {
    text = that.text;
    textLength = that.textLength;
    pos = that.pos;
    begin = that.begin;
    end = that.end;
    return *this;
}

UCharCharacterIterator::~UCharCharacterIterator() {
}

bool_t
UCharCharacterIterator::operator==(const CharacterIterator& that) const {
    if (this == &that) {
        return TRUE;
    }
    
    if (getDynamicClassID() != that.getDynamicClassID()) {
        return FALSE;
    }

    UCharCharacterIterator&    realThat = (UCharCharacterIterator&)that;

    return text == realThat.text
        && textLength == realThat.textLength
        && pos == realThat.pos
        && begin == realThat.begin
        && end == realThat.end;
}

int32_t
UCharCharacterIterator::hashCode() const {
    return uhash_hashUCharsN(text, textLength) ^ pos ^ begin ^ end;
}

CharacterIterator*
UCharCharacterIterator::clone() const {
    return new UCharCharacterIterator(*this);
}

UChar
UCharCharacterIterator::first() {
    pos = begin;
    if(pos < end) {
        return text[pos];
    } else {
        return DONE;
    }
}

UTextOffset
UCharCharacterIterator::setToStart() {
    return pos = begin;
}

UChar
UCharCharacterIterator::last() {
    pos = end;
    if(pos > begin) {
        return text[--pos];
    } else {
        return DONE;
    }
}

UTextOffset
UCharCharacterIterator::setToEnd() {
    return pos = end;
}

UChar
UCharCharacterIterator::setIndex(UTextOffset pos) {
    if(pos < begin) {
        pos = begin;
    } else if(pos > end) {
        pos = end;
    }
    this->pos = pos;
    if(pos < end) {
        return text[pos];
    } else {
        return DONE;
    }
}

UChar
UCharCharacterIterator::current() const {
    if (pos >= begin && pos < end) {
        return text[pos];
    } else {
        return DONE;
    }
}

UChar
UCharCharacterIterator::next() {
    if (pos + 1 < end) {
        return text[++pos];
    } else {
        /* make current() return DONE */
        pos = end;
        return DONE;
    }
}

UChar
UCharCharacterIterator::nextPostInc() {
    if (pos < end) {
        return text[pos++];
    } else {
        return DONE;
    }
}

bool_t
UCharCharacterIterator::hasNext() {
    return pos < end ? TRUE : FALSE;
}

UChar
UCharCharacterIterator::previous() {
    if (pos > begin) {
        return text[--pos];
    } else {
        return DONE;
    }
}

bool_t
UCharCharacterIterator::hasPrevious() {
    return pos > begin ? TRUE : FALSE;
}

UChar32
UCharCharacterIterator::first32() {
    pos = begin;
    if(pos < end) {
        UTextOffset i = pos;
        UChar32 c;
        UTF_NEXT_CHAR(text, i, end, c);
        return c;
    } else {
        return DONE;
    }
}

UChar32
UCharCharacterIterator::last32() {
    pos = end;
    if(pos > begin) {
        UChar32 c;
        UTF_PREV_CHAR(text, begin, pos, c);
        return c;
    } else {
        return DONE;
    }
}

UChar32
UCharCharacterIterator::setIndex32(UTextOffset pos) {
    if(pos < begin) {
        pos = begin;
    } else if(pos > end) {
        pos = end;
    }
    if(pos < end) {
        UTF_SET_CHAR_START(text, begin, pos);
        UTextOffset i = this->pos = pos;
        UChar32 c;
        UTF_NEXT_CHAR(text, i, end, c);
        return c;
    } else {
        this->pos = pos;
        return DONE;
    }
}

UChar32
UCharCharacterIterator::current32() const {
    if (pos >= begin && pos < end) {
        UChar32 c;
        UTF_GET_CHAR(text, begin, pos, end, c);
        return c;
    } else {
        return DONE;
    }
}

UChar32
UCharCharacterIterator::next32() {
    if (pos < end) {
        UTF_FWD_1(text, pos, end);
        if(pos < end) {
            UTextOffset i = pos;
            UChar32 c;
            UTF_NEXT_CHAR(text, i, end, c);
            return c;
        }
    }
    /* make current() return DONE */
    pos = end;
    return DONE;
}

UChar32
UCharCharacterIterator::next32PostInc() {
    if (pos < end) {
        UChar32 c;
        UTF_NEXT_CHAR(text, pos, end, c);
        return c;
    } else {
        return DONE;
    }
}

UChar32
UCharCharacterIterator::previous32() {
    if (pos > begin) {
        UChar32 c;
        UTF_PREV_CHAR(text, begin, pos, c);
        return c;
    } else {
        return DONE;
    }
}

UTextOffset
UCharCharacterIterator::startIndex() const {
    return begin;
}

UTextOffset
UCharCharacterIterator::endIndex() const {
    return end;
}

UTextOffset
UCharCharacterIterator::getIndex() const {
    return pos;
}

void UCharCharacterIterator::setText(const UChar* newText,
                                     int32_t      newTextLength) {
    text = newText;
    if(newText == 0 || newTextLength < 0) {
        newTextLength = 0;
    }
    end = textLength = newTextLength;
    pos = begin = 0;
}

void
UCharCharacterIterator::getText(UnicodeString& result) {
    result = UnicodeString(text, textLength);
}

char UCharCharacterIterator::fgClassID = 0;
