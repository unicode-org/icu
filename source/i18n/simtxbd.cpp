/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File SIMTXBD.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.  Converted offset_type
*                           to UTextOffset.
*   05/06/97    aliu        Modified previousSafePosition to check for 0 offset.  Not
*                           sure why this wasn't there before. (?)
*   08/11/98    helena      Sync-up JDK1.2.
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file SimpleTextBoundary.java
// *****************************************************************************

#include "simtxbd.h"
#include "wdbktbl.h"
#include "unicdcm.h"
#include "schriter.h"
#ifdef _DEBUG
#include "unistrm.h"
#endif

// *****************************************************************************
// class SimpleTextBoundary
// This class is an implementation of the BreakIterator
// protocol.  SimpleTextBoundary uses a state machine to compute breaks.
// There are currently several subclasses of SimpleTextBoundary that
// compute breaks for sentences, words, lines, and characters.
// *****************************************************************************

char SimpleTextBoundary::fgClassID = 0; // Value is irrelevant
const UChar SimpleTextBoundary::kEND_OF_STRING = 0xFFFF;

// Creates a simple text boundary instance with the text boundary data.
SimpleTextBoundary::SimpleTextBoundary(const TextBoundaryData* data)
  : fData(data), fText(0), fPos(0)
{
    fForward = fData->forward();
    fBackward = fData->backward();
    fMap = fData->map();
}

// -------------------------------------

SimpleTextBoundary::~SimpleTextBoundary()
{
}

// -------------------------------------
// copy constructor

SimpleTextBoundary::SimpleTextBoundary(const SimpleTextBoundary& rhs)
  : fData(rhs.fData), fText(rhs.fText), fPos(rhs.fPos)
{
    fForward = fData->forward();
    fBackward = fData->backward();
    fMap = fData->map();
}

// -------------------------------------

BreakIterator*
SimpleTextBoundary::clone() const
{
  return new SimpleTextBoundary(*this);
}

// -------------------------------------

bool_t
SimpleTextBoundary::operator==(const BreakIterator& rhs) const
{
    const SimpleTextBoundary* other = (const SimpleTextBoundary*)&rhs;

    return (this == &rhs) ||
        (rhs.getDynamicClassID() == getStaticClassID() &&
        // Pointer equality on fData sufficices since these are singletons
        fData == other->fData &&
        fPos == other->fPos &&
        ((fText == other->fText) // This handles the case when both ptrs are 0
         || (fText != 0 && other->fText != 0 && *fText == *other->fText)));
}

// -------------------------------------
// Gets the target text.

CharacterIterator*
SimpleTextBoundary::createText() const
{
    return fText->clone();
}

// -------------------------------------
// Sets the target text.

void
SimpleTextBoundary::setText(const UnicodeString* text)
{
    delete fText;
    fText = 0;
    fText = new StringCharacterIterator(*text);
    fPos = 0;
}

// -------------------------------------
// Sets the target text.

void
SimpleTextBoundary::adoptText(CharacterIterator* text)
{
    delete fText;
    fText = 0;
    fText = text;
    fPos = 0;
}

// -------------------------------------
// Gets the first text offset of the target text.

UTextOffset
SimpleTextBoundary::first()
{
    fPos = fText->startIndex();
    return fPos;
}

// -------------------------------------
// Gets the last text offset of the target text.

UTextOffset
SimpleTextBoundary::last()
{
    fPos = (fText == 0) ? 0 : fText->endIndex();
    return fPos;
}

// -------------------------------------
// Gets the next offset of the target text after the cursor increment.

UTextOffset
SimpleTextBoundary::next(int32_t increment)
{
    UTextOffset result = current();

    // If increment is negative, step backwards until the beginning
    // of string is reached.
    if (increment < 0) {
        for (int32_t i = increment; (i < 0) && (result != DONE); ++i) {
            result = previous();
        }
    } else {
        for (int32_t i = increment; (i > 0) && (result != DONE); --i) {
            result = next();
        }
    }

    return result;
}

// -------------------------------------
// Gets the previous offset of the target at the cursor.

UTextOffset
SimpleTextBoundary::previous()
{
  if (fPos > fText->startIndex()) { // != 0, DON'T need to check for != DONE as well
    UTextOffset startBoundary = fPos;
    // finds the previous safe position to backtrack to
    fPos = previousSafePosition(fPos - 1);
    UTextOffset prevPos = fPos;
    UTextOffset nextPos = next();
    // if the next position does not point to the start of a text boundary,
    // finds the next start point.
    while (nextPos < startBoundary && nextPos != DONE) {
        prevPos = nextPos;
        nextPos = next();
    }
    fPos = prevPos;
    return fPos;
  }
  // already at the beginning of the target text
  else {
    return DONE;
  }
}

// -------------------------------------
// Gets the next offset of the target at the cursor.

UTextOffset
SimpleTextBoundary::next()
{
    UTextOffset result = fPos;

    // Finds the next position of the end of string is not reached.
    if (fPos < ((fText == 0) ? 0 : fText->endIndex())) {
        fPos = nextPosition(fPos);
        result = fPos;
    }
    else {
        result = DONE;
    }

    return result;
}

// -------------------------------------
// Finds the offset of the next text boundary after the specified offset.

UTextOffset
SimpleTextBoundary::following(UTextOffset offset)
{
    if (offset >= ((fText == 0) ? 0 : fText->endIndex()))
    {
        fPos = (fText == 0) ? 0 : fText->endIndex();
        return DONE;
    }
    else if (offset < fText->startIndex())
    {
        fPos = 0;
        return 0;
    }

    fPos = previousSafePosition(offset);
    UTextOffset result;
    do {
        result = next();
    }
    while (result <= offset && result != DONE);

    return result;
}

// -------------------------------------
// Finds the offset of the next text boundary before the specified offset.

UTextOffset
SimpleTextBoundary::preceding(UTextOffset offset)
{
    if (offset <= fText->startIndex())
    {
        fPos = 0;
        return DONE;
    }
    else if (offset > ((fText == 0) ? 0 : fText->endIndex()))
    {
        fPos = (fText == 0) ? 0 : fText->endIndex();
        return fPos;
    }

    fPos = previousSafePosition(offset);
    UTextOffset p = fPos;
    UTextOffset last;
    do {
        last = p;
        p = next();
    }
    while (p < offset && p != DONE);

    fPos = last;
    return last;
}

bool_t
SimpleTextBoundary::isBoundary(UTextOffset offset)
{
    UTextOffset begin = fText->startIndex();
    if (offset < begin || offset >= ((fText == 0) ? 0 : fText->endIndex())) {
        return FALSE;
    } if (offset == begin) {
        return TRUE;
    } else {
        return following(offset - 1) == offset;
    }
}
     

// -------------------------------------
// Returns the current text offset.

UTextOffset
SimpleTextBoundary::current() const
{
    return fPos;
}

// -------------------------------------
// Finds the previous safe position; stepping backwards in the target text 
// until the end state is reached.

UTextOffset
SimpleTextBoundary::previousSafePosition(UTextOffset offset)
{
    UTextOffset result = fText->startIndex();

    // Stepping the target text backwards to find the previous safe spot.
    TextBoundaryData::Node state = fBackward->initialState();
    UChar c;

    if (offset == result) 
      ++offset;

    for (c = fText->setIndex(offset - 1);
    c != CharacterIterator::DONE && !fBackward->isEndState(state);
        c = fText->previous()) {

        state = fBackward->get(state, fMap->mappedChar(c));
        if (fBackward->isMarkState(state)) {
            result = fText->getIndex();
        }
    }
    return result;
}

// -------------------------------------
// Finds the next position; stepping forwards in the target text 
// until the end state is reached.

UTextOffset
SimpleTextBoundary::nextPosition(UTextOffset offset)
{
    UTextOffset endIndex = (fText == 0) ? 0 : fText->endIndex();

    TextBoundaryData::Node state = fForward->initialState();
    UTextOffset p = offset;
    UChar c;

    for (c = fText->setIndex(offset);
         c != CharacterIterator::DONE && !fForward->isEndState(state);
         c = fText->next()) {
        state = fForward->get(state, fMap->mappedChar(c));
        if (fForward->isMarkState(state)) {
            endIndex = fText->getIndex();
        }
    }
    if (fForward->isEndState(state))
        return endIndex;
    else {
        state = fForward->get(state, fMap->mappedChar(kEND_OF_STRING));
        if (fForward->isMarkState(state))
            return ((fText == 0) ? 0 : fText->endIndex());
        else
            return endIndex;
    }
}

//eof
