/*
 * @(#)SegmentSingleProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"
#include "SegmentSingleProcessor.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const char SegmentSingleProcessor::fgClassID=0;

SegmentSingleProcessor::SegmentSingleProcessor()
{
}

SegmentSingleProcessor::SegmentSingleProcessor(const MorphSubtableHeader *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(morphSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader *header = (const NonContextualGlyphSubstitutionHeader *) morphSubtableHeader;

    segmentSingleLookupTable = (const SegmentSingleLookupTable *) &header->table;
}

SegmentSingleProcessor::~SegmentSingleProcessor()
{
}

void SegmentSingleProcessor::process(LEGlyphID *glyphs, le_int32 * /*charIndices*/, le_int32 glyphCount)
{
    const LookupSegment *segments = segmentSingleLookupTable->segments;
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1) {
        const LookupSegment *lookupSegment = segmentSingleLookupTable->lookupSegment(segments, glyphs[glyph]);

        if (lookupSegment != NULL) {
            TTGlyphID newGlyph = (TTGlyphID) LE_GET_GLYPH(glyphs[glyph]) + SWAPW(lookupSegment->value);

            glyphs[glyph] = LE_SET_GLYPH(glyphs[glyph], newGlyph);
        }
    }
}

U_NAMESPACE_END
