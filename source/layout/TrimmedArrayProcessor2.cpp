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
#include "TrimmedArrayProcessor2.h"
#include "LEGlyphStorage.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(TrimmedArrayProcessor2)

TrimmedArrayProcessor2::TrimmedArrayProcessor2()
{
}

TrimmedArrayProcessor2::TrimmedArrayProcessor2(const MorphSubtableHeader2 *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor2(morphSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader2 *header = (const NonContextualGlyphSubstitutionHeader2 *) morphSubtableHeader;

    trimmedArrayLookupTable = (const TrimmedArrayLookupTable *) &header->table;
    firstGlyph = SWAPW(trimmedArrayLookupTable->firstGlyph);
    lastGlyph = firstGlyph + SWAPW(trimmedArrayLookupTable->glyphCount);
}

TrimmedArrayProcessor2::~TrimmedArrayProcessor2()
{
}

void TrimmedArrayProcessor2::process(LEGlyphStorage &glyphStorage)
{
    le_int32 glyphCount = glyphStorage.getGlyphCount();
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1) {
        LEGlyphID thisGlyph = glyphStorage[glyph];
        TTGlyphID ttGlyph = (TTGlyphID) LE_GET_GLYPH(thisGlyph);

        if ((ttGlyph > firstGlyph) && (ttGlyph < lastGlyph)) {
            TTGlyphID newGlyph = SWAPW(trimmedArrayLookupTable->valueArray[ttGlyph - firstGlyph]);

            glyphStorage[glyph] = LE_SET_GLYPH(thisGlyph, newGlyph);
        }
    }
} 

U_NAMESPACE_END
