/*
 * @(#)SegmentArrayProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"
#include "SegmentArrayProcessor.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const char SegmentArrayProcessor::fgClassID=0;

SegmentArrayProcessor::SegmentArrayProcessor()
{
}

SegmentArrayProcessor::SegmentArrayProcessor(const MorphSubtableHeader *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(morphSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader *header = (const NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    segmentArrayLookupTable = (const SegmentArrayLookupTable *) &header->table;
}

SegmentArrayProcessor::~SegmentArrayProcessor()
{
}

void SegmentArrayProcessor::process(LEGlyphID *glyphs, le_int32 * /*charIndices*/, le_int32 glyphCount)
{
    const LookupSegment *segments = segmentArrayLookupTable->segments;
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1) {
        const LookupSegment *lookupSegment = segmentArrayLookupTable->lookupSegment(segments, glyphs[glyph]);

        if (lookupSegment != NULL)  {
            TTGlyphID firstGlyph = SWAPW(lookupSegment->firstGlyph);
            le_int16  offset = SWAPW(lookupSegment->value);

            if (offset != 0) {
                TTGlyphID *glyphArray = (TTGlyphID *) ((char *) subtableHeader + offset);
                TTGlyphID  newGlyph   = SWAPW(glyphArray[LE_GET_GLYPH(glyphs[glyph]) - firstGlyph]);
                
                glyphs[glyph] = LE_SET_GLYPH(glyphs[glyph], newGlyph);
            } 
        }
    }
}
 
U_NAMESPACE_END
