/*
 * @(#)IndicRearrangementProcessor.cpp	1.7 00/03/15
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
#include "IndicRearrangementProcessor.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

const char IndicRearrangementProcessor::fgClassID=0;

IndicRearrangementProcessor::IndicRearrangementProcessor(const MorphSubtableHeader *morphSubtableHeader)
  : StateTableProcessor(morphSubtableHeader)
{
    indicRearrangementSubtableHeader = (const IndicRearrangementSubtableHeader *) morphSubtableHeader;
    entryTable = (const IndicRearrangementStateEntry *) ((char *) &stateTableHeader->stHeader + entryTableOffset);
}

IndicRearrangementProcessor::~IndicRearrangementProcessor()
{
}

void IndicRearrangementProcessor::beginStateTable()
{
    firstGlyph = 0;
    lastGlyph = 0;
}

ByteOffset IndicRearrangementProcessor::processStateEntry(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 &currGlyph,
        le_int32 /*glyphCount*/, EntryTableIndex index)
{
    const IndicRearrangementStateEntry *entry = &entryTable[index];
    ByteOffset newState = SWAPW(entry->newStateOffset);
    IndicRearrangementFlags flags = (IndicRearrangementFlags) SWAPW(entry->flags);

    if (flags & irfMarkFirst) {
        firstGlyph = currGlyph;
    }

    if (flags & irfMarkLast) {
        lastGlyph = currGlyph;
    }

    doRearrangementAction(glyphs, charIndices, (IndicRearrangementVerb) (flags & irfVerbMask));

    if (!(flags & irfDontAdvance)) {
        // XXX: Should handle reverse too...
        currGlyph += 1;
    }

    return newState;
}

void IndicRearrangementProcessor::endStateTable()
{
}

void IndicRearrangementProcessor::doRearrangementAction(LEGlyphID *glyphs, le_int32 *charIndices, IndicRearrangementVerb verb) const
{
    LEGlyphID a, b, c, d;
    le_int32 ia, ib, ic, id, x;

    switch(verb)
    {
    case irvNoAction:
        break;

    case irvxA:
        a = glyphs[firstGlyph];
        ia = charIndices[firstGlyph];
        x = firstGlyph + 1;

        while (x <= lastGlyph) {
            glyphs[x - 1] = glyphs[x];
            charIndices[x - 1] = charIndices[x];
            x += 1;
        }

        glyphs[lastGlyph] = a;
        charIndices[lastGlyph] = ia;
        break;

    case irvDx:
        d = glyphs[lastGlyph];
        id = charIndices[lastGlyph];
        x = lastGlyph - 1;

        while (x >= firstGlyph) {
            glyphs[x + 1] = glyphs[x];
            charIndices[x + 1] = charIndices[x];
            x -= 1;
        }

        glyphs[firstGlyph] = d;
        charIndices[firstGlyph] = id;
        break;

    case irvDxA:
        a = glyphs[firstGlyph];
        ia = charIndices[firstGlyph];

        glyphs[firstGlyph] = glyphs[lastGlyph];
        glyphs[lastGlyph] = a;

        charIndices[firstGlyph] = charIndices[lastGlyph];
        charIndices[lastGlyph] = ia;
        break;
        
    case irvxAB:
        a = glyphs[firstGlyph];
        b = glyphs[firstGlyph + 1];
        ia = charIndices[firstGlyph];
        ib = charIndices[firstGlyph + 1];
        x = firstGlyph + 2;

        while (x <= lastGlyph) {
            glyphs[x - 2] = glyphs[x];
            charIndices[x - 2] = charIndices[x];
            x += 1;
        }

        glyphs[lastGlyph - 1] = a;
        glyphs[lastGlyph] = b;

        charIndices[lastGlyph - 1] = ia;
        charIndices[lastGlyph] = ib;
        break;

    case irvxBA:
        a = glyphs[firstGlyph];
        b = glyphs[firstGlyph + 1];
        ia = charIndices[firstGlyph];
        ib = charIndices[firstGlyph + 1];
        x = firstGlyph + 2;

        while (x <= lastGlyph) {
            glyphs[x - 2] = glyphs[x];
            charIndices[x - 2] = charIndices[x];
            x += 1;
        }

        glyphs[lastGlyph - 1] = b;
        glyphs[lastGlyph] = a;

        charIndices[lastGlyph - 1] = ib;
        charIndices[lastGlyph] = ia;
        break;

    case irvCDx:
        c = glyphs[lastGlyph - 1];
        d = glyphs[lastGlyph];
        ic = charIndices[lastGlyph - 1];
        id = charIndices[lastGlyph];
        x = lastGlyph - 2;

        while (x >= lastGlyph) {
            glyphs[x + 2] = glyphs[x];
            charIndices[x + 2] = charIndices[x];
            x -= 1;
        }
        
        glyphs[firstGlyph] = c;
        glyphs[firstGlyph + 1] = d;

        charIndices[firstGlyph] = ic;
        charIndices[firstGlyph + 1] = id;
        break; 

    case irvDCx:
        c = glyphs[lastGlyph - 1];
        d = glyphs[lastGlyph];
        ic = charIndices[lastGlyph - 1];
        id = charIndices[lastGlyph];
        x = lastGlyph - 2;

        while (x >= lastGlyph) {
            glyphs[x + 2] = glyphs[x];
            charIndices[x + 2] = charIndices[x];
            x -= 1;
        }
        
        glyphs[firstGlyph] = d;
        glyphs[firstGlyph + 1] = c;

        charIndices[firstGlyph] = id;
        charIndices[firstGlyph + 1] = ic;
        break; 

    case irvCDxA:
        a = glyphs[firstGlyph];
        c = glyphs[lastGlyph - 1];
        d = glyphs[lastGlyph];
        ia = charIndices[firstGlyph];
        ic = charIndices[lastGlyph - 1];
        id = charIndices[lastGlyph];
        x = lastGlyph - 2;

        while (x > firstGlyph) {
            glyphs[x + 1] = glyphs[x];
            charIndices[x + 1] = charIndices[x];
            x -= 1;
        }
        
        glyphs[firstGlyph] = c;
        glyphs[firstGlyph + 1] = d;
        glyphs[lastGlyph] = a;

        charIndices[firstGlyph] = ic;
        charIndices[firstGlyph + 1] = id;
        charIndices[lastGlyph] = ia;
        break; 

    case irvDCxA:
        a = glyphs[firstGlyph];
        c = glyphs[lastGlyph - 1];
        d = glyphs[lastGlyph];
        ia = charIndices[firstGlyph];
        ic = charIndices[lastGlyph - 1];
        id = charIndices[lastGlyph];
        x = lastGlyph - 2;

        while (x > firstGlyph) {
            glyphs[x + 1] = glyphs[x];
            charIndices[x + 1] = charIndices[x];
            x -= 1;
        }
        
        glyphs[firstGlyph] = d;
        glyphs[firstGlyph + 1] = c;
        glyphs[lastGlyph] = a;

        charIndices[firstGlyph] = id;
        charIndices[firstGlyph + 1] = ic;
        charIndices[lastGlyph] = ia;
        break; 

    case irvDxAB:
        a = glyphs[firstGlyph];
        b = glyphs[firstGlyph + 1];
        d = glyphs[lastGlyph];
        ia = charIndices[firstGlyph];
        ib = charIndices[firstGlyph + 1];
        id = charIndices[lastGlyph];
        x = firstGlyph + 2;

        while (x < lastGlyph) {
            glyphs[x - 2] = glyphs[x];
            charIndices[x - 2] = charIndices[x];
            x += 1;
        }

        glyphs[firstGlyph] = d;
        glyphs[lastGlyph - 1] = a;
        glyphs[lastGlyph] = b;

        charIndices[firstGlyph] = id;
        charIndices[lastGlyph - 1] = ia;
        charIndices[lastGlyph] = ib;
        break;

    case irvDxBA:
        a = glyphs[firstGlyph];
        b = glyphs[firstGlyph + 1];
        d = glyphs[lastGlyph];
        ia = charIndices[firstGlyph];
        ib = charIndices[firstGlyph + 1];
        id = charIndices[lastGlyph];
        x = firstGlyph + 2;

        while (x < lastGlyph) {
            glyphs[x - 2] = glyphs[x];
            charIndices[x - 2] = charIndices[x];
            x += 1;
        }

        glyphs[firstGlyph] = d;
        glyphs[lastGlyph - 1] = b;
        glyphs[lastGlyph] = a;

        charIndices[firstGlyph] = id;
        charIndices[lastGlyph - 1] = ib;
        charIndices[lastGlyph] = ia;
        break;

    case irvCDxAB:
        a = glyphs[firstGlyph];
        b = glyphs[firstGlyph + 1];

        glyphs[firstGlyph] = glyphs[lastGlyph - 1];
        glyphs[firstGlyph + 1] = glyphs[lastGlyph];

        glyphs[lastGlyph - 1] = a;
        glyphs[lastGlyph] = b;

        ia = charIndices[firstGlyph];
        ib = charIndices[firstGlyph + 1];

        charIndices[firstGlyph] = charIndices[lastGlyph - 1];
        charIndices[firstGlyph + 1] = charIndices[lastGlyph];

        charIndices[lastGlyph - 1] = ia;
        charIndices[lastGlyph] = ib;
        break;

    case irvCDxBA:
        a = glyphs[firstGlyph];
        b = glyphs[firstGlyph + 1];

        glyphs[firstGlyph] = glyphs[lastGlyph - 1];
        glyphs[firstGlyph + 1] = glyphs[lastGlyph];

        glyphs[lastGlyph - 1] = b;
        glyphs[lastGlyph] = a;

        ia = charIndices[firstGlyph];
        ib = charIndices[firstGlyph + 1];

        charIndices[firstGlyph] = charIndices[lastGlyph - 1];
        charIndices[firstGlyph + 1] = charIndices[lastGlyph];

        charIndices[lastGlyph - 1] = ib;
        charIndices[lastGlyph] = ia;
        break;

    case irvDCxAB:
        a = glyphs[firstGlyph];
        b = glyphs[firstGlyph + 1];

        glyphs[firstGlyph] = glyphs[lastGlyph];
        glyphs[firstGlyph + 1] = glyphs[lastGlyph - 1];

        glyphs[lastGlyph - 1] = a;
        glyphs[lastGlyph] = b;

        ia = charIndices[firstGlyph];
        ib = charIndices[firstGlyph + 1];

        charIndices[firstGlyph] = charIndices[lastGlyph];
        charIndices[firstGlyph + 1] = charIndices[lastGlyph - 1];

        charIndices[lastGlyph - 1] = ia;
        charIndices[lastGlyph] = ib;
        break;

    case irvDCxBA:
        a = glyphs[firstGlyph];
        b = glyphs[firstGlyph + 1];

        glyphs[firstGlyph] = glyphs[lastGlyph];
        glyphs[firstGlyph + 1] = glyphs[lastGlyph - 1];

        glyphs[lastGlyph - 1] = b;
        glyphs[lastGlyph] = a;

        ia = charIndices[firstGlyph];
        ib = charIndices[firstGlyph + 1];

        charIndices[firstGlyph] = charIndices[lastGlyph];
        charIndices[firstGlyph + 1] = charIndices[lastGlyph - 1];

        charIndices[lastGlyph - 1] = ib;
        charIndices[lastGlyph] = ia;
        break;
    
    default:
        break;
    }
}

U_NAMESPACE_END
