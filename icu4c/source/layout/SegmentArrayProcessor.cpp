/*
 * @(#)SegmentArrayProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubstitution.h"
#include "NonContextualGlyphSubstitutionProcessor.h"
#include "SegmentArrayProcessor.h"
#include "LESwaps.h"

SegmentArrayProcessor::SegmentArrayProcessor()
{
}

SegmentArrayProcessor::SegmentArrayProcessor(MorphSubtableHeader *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(morphSubtableHeader)
{
    NonContextualGlyphSubstitutionHeader *header = (NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    segmentArrayLookupTable = (SegmentArrayLookupTable *) &header->table;
}

SegmentArrayProcessor::~SegmentArrayProcessor()
{
}

void SegmentArrayProcessor::process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount)
{
    LookupSegment *segments = segmentArrayLookupTable->segments;
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1)
    {
        LookupSegment *lookupSegment = segmentArrayLookupTable->lookupSegment(segments, glyphs[glyph]);

        if (lookupSegment != NULL)
        {
            le_int16 firstGlyph = SWAPW(lookupSegment->firstGlyph);
            le_int16 offset = SWAPW(lookupSegment->value);

            if (offset != 0)
            {
                le_int16 *glyphArray = (le_int16 *) ((char *) subtableHeader + offset);
                le_int16 newGlyph = glyphArray[glyphs[glyph] - firstGlyph];
                
                glyphs[glyph] = SWAPW(newGlyph);
            } 
        }
    }
}
 
