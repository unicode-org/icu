/*
 * @(#)SimpleArrayProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"
#include "SimpleArrayProcessor.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const char SimpleArrayProcessor::fgClassID=0;

SimpleArrayProcessor::SimpleArrayProcessor()
{
}

SimpleArrayProcessor::SimpleArrayProcessor(const MorphSubtableHeader *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(morphSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader *header = (const NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    simpleArrayLookupTable = (const SimpleArrayLookupTable *) &header->table;
}

SimpleArrayProcessor::~SimpleArrayProcessor()
{
}

void SimpleArrayProcessor::process(LEGlyphID *glyphs, le_int32 * /*charIndices*/, le_int32 glyphCount)
{
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1) {
        if (glyphs[glyph] < 0xFFFF) {
            le_int16 newGlyph = SWAPW(simpleArrayLookupTable->valueArray[glyphs[glyph]]);

            glyphs[glyph] = LE_SET_GLYPH(glyphs[glyph], newGlyph);
        }
    }
}
 
U_NAMESPACE_END
