/*
 *
 * (C) Copyright IBM Corp.  and others 1998-2013 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor2.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc2.h"
#include "SegmentSingleProcessor2.h"
#include "LEGlyphStorage.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(SegmentSingleProcessor2)

SegmentSingleProcessor2::SegmentSingleProcessor2()
{
}

SegmentSingleProcessor2::SegmentSingleProcessor2(const MorphSubtableHeader2 *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor2(morphSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader2 *header = (const NonContextualGlyphSubstitutionHeader2 *) morphSubtableHeader;

    segmentSingleLookupTable = (const SegmentSingleLookupTable *) &header->table;
}

SegmentSingleProcessor2::~SegmentSingleProcessor2()
{
}

void SegmentSingleProcessor2::process(LEGlyphStorage &glyphStorage)
{
    const LookupSegment *segments = segmentSingleLookupTable->segments;
    le_int32 glyphCount = glyphStorage.getGlyphCount();
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1) {
        LEGlyphID thisGlyph = glyphStorage[glyph];
        const LookupSegment *lookupSegment = segmentSingleLookupTable->lookupSegment(segments, thisGlyph);

        if (lookupSegment != NULL) {
            TTGlyphID   newGlyph  = (TTGlyphID) LE_GET_GLYPH(thisGlyph) + SWAPW(lookupSegment->value);

            glyphStorage[glyph] = LE_SET_GLYPH(thisGlyph, newGlyph);
        }
    }
}

U_NAMESPACE_END
