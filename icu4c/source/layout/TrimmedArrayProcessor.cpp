/*
 * @(#)TrimmedArrayProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"
#include "TrimmedArrayProcessor.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const char TrimmedArrayProcessor::fgClassID=0;

TrimmedArrayProcessor::TrimmedArrayProcessor()
{
}

TrimmedArrayProcessor::TrimmedArrayProcessor(const MorphSubtableHeader *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(morphSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader *header = (const NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    trimmedArrayLookupTable = (const TrimmedArrayLookupTable *) &header->table;
    firstGlyph = SWAPW(trimmedArrayLookupTable->firstGlyph);
    lastGlyph = firstGlyph + SWAPW(trimmedArrayLookupTable->glyphCount);
}

TrimmedArrayProcessor::~TrimmedArrayProcessor()
{
}

void TrimmedArrayProcessor::process(LEGlyphID *glyphs, le_int32 * /*charIndices*/, le_int32 glyphCount)
{
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1) {
        TTGlyphID ttGlyph = (TTGlyphID) LE_GET_GLYPH(glyphs[glyph]);

        if ((ttGlyph > firstGlyph) && (ttGlyph < lastGlyph)) {
            TTGlyphID newGlyph = SWAPW(trimmedArrayLookupTable->valueArray[ttGlyph - firstGlyph]);

            glyphs[glyph] = LE_SET_GLYPH(glyphs[glyph], newGlyph);
        }
    }
} 

U_NAMESPACE_END
