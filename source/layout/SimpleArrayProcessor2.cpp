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
#include "SimpleArrayProcessor2.h"
#include "LEGlyphStorage.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(SimpleArrayProcessor2)

SimpleArrayProcessor2::SimpleArrayProcessor2()
{
}

SimpleArrayProcessor2::SimpleArrayProcessor2(const MorphSubtableHeader2 *morphSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor2(morphSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader2 *header = (const NonContextualGlyphSubstitutionHeader2 *) morphSubtableHeader;

    simpleArrayLookupTable = (const SimpleArrayLookupTable *) &header->table;
}

SimpleArrayProcessor2::~SimpleArrayProcessor2()
{
}

void SimpleArrayProcessor2::process(LEGlyphStorage &glyphStorage)
{
    le_int32 glyphCount = glyphStorage.getGlyphCount();
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1) {
        LEGlyphID thisGlyph = glyphStorage[glyph];
        if (LE_GET_GLYPH(thisGlyph) < 0xFFFF) {
            TTGlyphID newGlyph = SWAPW(simpleArrayLookupTable->valueArray[LE_GET_GLYPH(thisGlyph)]);

            glyphStorage[glyph] = LE_SET_GLYPH(thisGlyph, newGlyph);
        }
    }
}
 
U_NAMESPACE_END
