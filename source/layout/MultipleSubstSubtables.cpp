/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "LEGlyphFilter.h"
#include "OpenTypeTables.h"
#include "GlyphSubstitutionTables.h"
#include "MultipleSubstSubtables.h"
#include "GlyphIterator.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

le_uint32 MultipleSubstitutionSubtable::process(GlyphIterator *glyphIterator, const LEGlyphFilter *filter) const
{
    LEGlyphID glyph = (LEGlyphID) glyphIterator->getCurrGlyphID();
    le_int32 coverageIndex = getGlyphCoverage(glyph);
    le_uint16 seqCount = SWAPW(sequenceCount);

    if (coverageIndex >= 0 && coverageIndex < seqCount)
    {
        Offset sequenceTableOffset = SWAPW(sequenceTableOffsetArray[coverageIndex]);
        const SequenceTable *sequenceTable = (const SequenceTable *) ((char *) this + sequenceTableOffset);
        le_uint16 glyphCount = SWAPW(sequenceTable->glyphCount);

        if (glyphCount == 0) {
            glyphIterator->setCurrGlyphID(0xFFFF);
            return 1;
        }
        else {
            // Can't do this 'till there's a way to
            // grow the glyph array...
            return 1;
        }
    }

    return 0;
}

U_NAMESPACE_END
