/*
 * @(#)ContextualGlyphSubstProc.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "StateTables.h"
#include "MorphStateTables.h"
#include "SubtableProcessor.h"
#include "StateTableProcessor.h"
#include "ContextualGlyphSubstProc.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const char ContextualGlyphSubstitutionProcessor::fgClassID=0;

ContextualGlyphSubstitutionProcessor::ContextualGlyphSubstitutionProcessor(const MorphSubtableHeader *morphSubtableHeader)
  : StateTableProcessor(morphSubtableHeader)
{
    contextualGlyphSubstitutionHeader = (const ContextualGlyphSubstitutionHeader *) morphSubtableHeader;
    substitutionTableOffset = SWAPW(contextualGlyphSubstitutionHeader->substitutionTableOffset);

    entryTable = (const ContextualGlyphSubstitutionStateEntry *) ((char *) &stateTableHeader->stHeader + entryTableOffset);
}

ContextualGlyphSubstitutionProcessor::~ContextualGlyphSubstitutionProcessor()
{
}

void ContextualGlyphSubstitutionProcessor::beginStateTable()
{
    markGlyph = 0;
}

ByteOffset ContextualGlyphSubstitutionProcessor::processStateEntry(LEGlyphID *glyphs, le_int32 * /*charIndices*/, le_int32 &currGlyph, le_int32 /*glyphCount*/, EntryTableIndex index)
{
    const ContextualGlyphSubstitutionStateEntry *entry = &entryTable[index];
    ByteOffset newState = SWAPW(entry->newStateOffset);
    le_int16 flags = SWAPW(entry->flags);
    WordOffset markOffset = SWAPW(entry->markOffset);
    WordOffset currOffset = SWAPW(entry->currOffset);

    if (markOffset != 0) {
        const le_int16 *table = (const le_int16 *) ((char *) &stateTableHeader->stHeader + markOffset * 2);
        TTGlyphID newGlyph = SWAPW(table[LE_GET_GLYPH(glyphs[markGlyph])]);

         glyphs[markGlyph] = LE_SET_GLYPH(glyphs[markGlyph], newGlyph);
    }

    if (currOffset != 0) {
        const le_int16 *table = (const le_int16 *) ((char *) &stateTableHeader->stHeader + currOffset * 2);
        le_int16 newGlyph = SWAPW(table[LE_GET_GLYPH(glyphs[currGlyph])]);

        glyphs[currGlyph] = LE_SET_GLYPH(glyphs[currGlyph], newGlyph);
    }

    if (flags & cgsSetMark) {
        markGlyph = currGlyph;
    }

    if (!(flags & cgsDontAdvance)) {
        // should handle reverse too!
        currGlyph += 1;
    }

    return newState;
}

void ContextualGlyphSubstitutionProcessor::endStateTable()
{
}

U_NAMESPACE_END
