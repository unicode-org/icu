/*
 * @(#)StateTableProcessor.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __STATETABLEPROCESSOR_H
#define __STATETABLEPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"
#include "MorphStateTables.h"
#include "SubtableProcessor.h"

class StateTableProcessor : public SubtableProcessor
{
public:
    void process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyph);

    virtual void beginStateTable() = 0;

    virtual ByteOffset processStateEntry(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 &currGlyph,
        le_int32 glyphCount, EntryTableIndex index) = 0;

    virtual void endStateTable() = 0;

protected:
    StateTableProcessor(MorphSubtableHeader *morphSubtableHeader);
    virtual ~StateTableProcessor();

    StateTableProcessor();

    le_int16 stateSize;
    ByteOffset classTableOffset;
    ByteOffset stateArrayOffset;
    ByteOffset entryTableOffset;

    ClassTable *classTable;
    le_int16 firstGlyph;
    le_int16 lastGlyph;

    MorphStateTableHeader *stateTableHeader;
};

#endif
