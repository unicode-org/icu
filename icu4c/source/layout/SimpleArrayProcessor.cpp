/*
 * @(#)SimpleArrayProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"
#include "SimpleArrayProcessor.h"
#include "LESwaps.h"

SimpleArrayProcessor::SimpleArrayProcessor()
{
}

SimpleArrayProcessor::SimpleArrayProcessor(MorphSubtableHeader *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(morphSubtableHeader)
{
    NonContextualGlyphSubstitutionHeader *header = (NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    simpleArrayLookupTable = (SimpleArrayLookupTable *) &header->table;
}

SimpleArrayProcessor::~SimpleArrayProcessor()
{
}

void SimpleArrayProcessor::process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount)
{
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1)
    {
        if (glyphs[glyph] < 0xFFFF)
        {
            le_int16 newGlyph = simpleArrayLookupTable->valueArray[glyphs[glyph]];

            glyphs[glyph] = SWAPW(newGlyph);
        }
    }
}
 
