/*
 * @(#)LigatureSubstitutionProcessor.cpp	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "StateTables.h"
#include "MorphStateTables.h"
#include "SubtableProcessor.h"
#include "StateTableProcessor.h"
#include "LigatureSubstitutionProcessor.h"
#include "LESwaps.h"

#define ExtendedComplement(m) ((le_int32) (~((le_uint32) (m))))
#define SignBit(m) ((ExtendedComplement(m) >> 1) & (m))
#define SignExtend(v,m) (((v) & SignBit(m))? ((v) | ExtendedComplement(m)): (v))

LigatureSubstitutionProcessor::LigatureSubstitutionProcessor(MorphSubtableHeader *morphSubtableHeader)
  : StateTableProcessor(morphSubtableHeader)
{
    ligatureSubstitutionHeader = (LigatureSubstitutionHeader *) morphSubtableHeader;
    ligatureActionTableOffset = SWAPW(ligatureSubstitutionHeader->ligatureActionTableOffset);
    componentTableOffset = SWAPW(ligatureSubstitutionHeader->componentTableOffset);
    ligatureTableOffset = SWAPW(ligatureSubstitutionHeader->ligatureTableOffset);

    entryTable = (LigatureSubstitutionStateEntry *) ((char *) &stateTableHeader->stHeader + entryTableOffset);
}

LigatureSubstitutionProcessor::~LigatureSubstitutionProcessor()
{
}

void LigatureSubstitutionProcessor::beginStateTable()
{
    m = -1;
}

ByteOffset LigatureSubstitutionProcessor::processStateEntry(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 &currGlyph, le_int32 glyphCount, EntryTableIndex index)
{
    LigatureSubstitutionStateEntry *entry = &entryTable[index];
    ByteOffset newState = SWAPW(entry->newStateOffset);
    le_int16 flags = SWAPW(entry->flags);

    if (flags & lsfSetComponent)
    {
        if (++m >= nComponents)
        {
            m = 0;
        }

        componentStack[m] = currGlyph;
    }

    ByteOffset actionOffset = flags & lsfActionOffsetMask;

    if (actionOffset != 0)
    {
        LigatureActionEntry *ap = (LigatureActionEntry *) ((char *) &ligatureSubstitutionHeader->stHeader + actionOffset);
        LigatureActionEntry action;
        le_int32 offset, i = 0;
        le_int32 stack[nComponents];
        le_int16 mm = -1;

        do
        {
            le_uint32 componentGlyph = componentStack[m--];

            action = SWAPL(*ap++);

            if (m < 0)
            {
                m = nComponents - 1;
            }

            offset = action & lafComponentOffsetMask;
            if (offset != 0)
            {
                le_int16 *offsetTable = (le_int16 *)((char *) &ligatureSubstitutionHeader->stHeader + 2 * SignExtend(offset, lafComponentOffsetMask));

                i += SWAPW(offsetTable[glyphs[componentGlyph]]);

                if (action & (lafLast | lafStore))
                {
                    le_int16 *ligatureOffset = (le_int16 *) ((char *) &ligatureSubstitutionHeader->stHeader + i);

                    glyphs[componentGlyph] = SWAPW(*ligatureOffset);
                    stack[++mm] = componentGlyph;
                    i = 0;
                }
                else
                {
                    glyphs[componentGlyph] = 0xFFFF;
                }
            }
        }
        while (!(action & lafLast));

        while (mm >= 0)
        {
            if (++m >= nComponents)
            {
                m = 0;
            }

            componentStack[m] = stack[mm--];
        }
    }

    if (!(flags & lsfDontAdvance))
    {
        // should handle reverse too!
        currGlyph += 1;
    }

    return newState;
}

void LigatureSubstitutionProcessor::endStateTable()
{
}
