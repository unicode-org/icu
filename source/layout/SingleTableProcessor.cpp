/*
 * @(#)SingleTableProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"
#include "SingleTableProcessor.h"
#include "LESwaps.h"

SingleTableProcessor::SingleTableProcessor()
{
}

SingleTableProcessor::SingleTableProcessor(MorphSubtableHeader *moprhSubtableHeader)
  : NonContextualGlyphSubstitutionProcessor(moprhSubtableHeader)
{
    NonContextualGlyphSubstitutionHeader *header = (NonContextualGlyphSubstitutionHeader *) moprhSubtableHeader;

    singleTableLookupTable = (SingleTableLookupTable *) &header->table;
}

SingleTableProcessor::~SingleTableProcessor()
{
}

void SingleTableProcessor::process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount)
{
    LookupSingle *entries = singleTableLookupTable->entries;
    le_int32 glyph;

    for (glyph = 0; glyph < glyphCount; glyph += 1)
    {
        LookupSingle *lookupSingle = singleTableLookupTable->lookupSingle(entries, glyphs[glyph]);

        if (lookupSingle != NULL)
        {
            glyphs[glyph] = SWAPW(lookupSingle->value);
        }
    }
} 
