/*
 * @(#)SingleTableProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"
#include "SingleTableProcessor.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const char SingleTableProcessor::fgClassID=0;

SingleTableProcessor::SingleTableProcessor()
{
}

SingleTableProcessor::SingleTableProcessor(const MorphSubtableHeader *moprhSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(moprhSubtableHeader)
{
    const NonContextualGlyphSubstitutionHeader *header = (const NonContextualGlyphSubstitutionHeader *) moprhSubtableHeader;

    singleTableLookupTable = (const SingleTableLookupTable *) &header->table;
}

SingleTableProcessor::~SingleTableProcessor()
{
}

void SingleTableProcessor::process(LEGlyphID *glyphs, le_int32 * /*charIndices*/, le_int32 glyphCount)
{
    const LookupSingle *entries = singleTableLookupTable->entries;
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1) {
        const LookupSingle *lookupSingle = singleTableLookupTable->lookupSingle(entries, glyphs[glyph]);

        if (lookupSingle != NULL) {
            glyphs[glyph] = SWAPW(lookupSingle->value);
        }
    }
} 

U_NAMESPACE_END
