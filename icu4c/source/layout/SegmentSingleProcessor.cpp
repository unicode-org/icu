/*
 * @(#)SegmentSingleProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubstitution.h"
#include "NonContextualGlyphSubstitutionProcessor.h"
#include "SegmentSingleProcessor.h"
#include "LESwaps.h"

SegmentSingleProcessor::SegmentSingleProcessor()
{
}

SegmentSingleProcessor::SegmentSingleProcessor(MorphSubtableHeader *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(morphSubtableHeader)
{
    NonContextualGlyphSubstitutionHeader *header = (NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    segmentSingleLookupTable = (SegmentSingleLookupTable *) &header->table;
}

SegmentSingleProcessor::~SegmentSingleProcessor()
{
}

void SegmentSingleProcessor::process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount)
{
    LookupSegment *segments = segmentSingleLookupTable->segments;
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1)
    {
        LookupSegment *lookupSegment = segmentSingleLookupTable->lookupSegment(segments, glyphs[glyph]);

        if (lookupSegment != NULL)
        {
            glyphs[glyph] += SWAPW(lookupSegment->value);
        }
    }
}
