/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File TXTBDRY.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.  Added DONE.
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file BreakIterator.java
// *****************************************************************************

#include "utypes.h"
#include "brkiter.h"
#include "simtxbd.h"

#include <string.h>

// *****************************************************************************
// class BreakIterator
// This class implements methods for finding the location of boundaries in text. 
// Instances of BreakIterator maintain a current position and scan over text
// returning the index of characters where boundaries occur.
// *****************************************************************************

const UTextOffset BreakIterator::DONE = (int32_t)-1;

// -------------------------------------

// Creates a simple text boundary for word breaks.
BreakIterator*
BreakIterator::createWordInstance(const Locale& key)
{
    return new SimpleTextBoundary(&TextBoundaryData::kWordBreakData);
}

// -------------------------------------

// Creates a simple text boundary for line breaks.
BreakIterator*
BreakIterator::createLineInstance(const Locale& key)
{
    return new SimpleTextBoundary(&TextBoundaryData::kLineBreakData);
}

// -------------------------------------

// Creates a simple text boundary for character breaks.
BreakIterator*
BreakIterator::createCharacterInstance(const Locale& key)
{
    return new SimpleTextBoundary(&TextBoundaryData::kCharacterBreakData);
}

// -------------------------------------

// Creates a simple text boundary for sentence breaks.
BreakIterator*
BreakIterator::createSentenceInstance(const Locale& key)
{
    return new SimpleTextBoundary(&TextBoundaryData::kSentenceBreakData);
}

// -------------------------------------

// Gets all the available locales that has localized text boundary data.
const Locale*
BreakIterator::getAvailableLocales(int32_t& count)
{
    return Locale::getAvailableLocales(count);
}

// -------------------------------------
// Gets the objectLocale display name in the default locale language.
UnicodeString&
BreakIterator::getDisplayName(const Locale& objectLocale,
                             UnicodeString& name)
{
    return objectLocale.getDisplayName(name);
}

// -------------------------------------
// Gets the objectLocale display name in the displayLocale language.
UnicodeString&
BreakIterator::getDisplayName(const Locale& objectLocale,
                             const Locale& displayLocale,
                             UnicodeString& name)
{
    return objectLocale.getDisplayName(displayLocale, name);
}

// -------------------------------------

// Needed because we declare the copy constructor (in order to prevent synthesizing one) and
// so the default constructor is no longer synthesized.

BreakIterator::BreakIterator()
{
}

BreakIterator::~BreakIterator()
{
}

//eof
