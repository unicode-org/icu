/*
 * @(#)TrimmedArrayProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubstitution.h"
#include "NonContextualGlyphSubstitutionProcessor.h"
#include "TrimmedArrayProcessor.h"
#include "LESwaps.h"

TrimmedArrayProcessor::TrimmedArrayProcessor()
{
}

TrimmedArrayProcessor::TrimmedArrayProcessor(MorphSubtableHeader *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(morphSubtableHeader)
{
    NonContextualGlyphSubstitutionHeader *header = (NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    trimmedArrayLookupTable = (TrimmedArrayLookupTable *) &header->table;
    firstGlyph = SWAPW(trimmedArrayLookupTable->firstGlyph);
    lastGlyph = firstGlyph + SWAPW(trimmedArrayLookupTable->glyphCount);
}

TrimmedArrayProcessor::~TrimmedArrayProcessor()
{
}

void TrimmedArrayProcessor::process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount)
{
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1)
    {
        if ((glyphs[glyph] > firstGlyph) && (glyphs[glyph] < lastGlyph))
        {
            le_int16 newGlyph = trimmedArrayLookupTable->valueArray[glyphs[glyph] - firstGlyph];

            glyphs[glyph] = SWAPW(newGlyph);
        }
    }
} 
