/*
 * @(#)IndicRearrangementProcessor.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __INDICREARRANGEMENTPROCESSOR_H
#define __INDICREARRANGEMENTPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "StateTableProcessor.h"
#include "IndicRearrangement.h"

U_NAMESPACE_BEGIN

class IndicRearrangementProcessor : public StateTableProcessor
{
public:
    virtual void beginStateTable();

    virtual ByteOffset processStateEntry(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 &currGlyph,
        le_int32 glyphCount, EntryTableIndex index);

    virtual void endStateTable();

    void doRearrangementAction(LEGlyphID *glyphs, le_int32 *charIndices, IndicRearrangementVerb verb) const;

    IndicRearrangementProcessor(const MorphSubtableHeader *morphSubtableHeader);
    virtual ~IndicRearrangementProcessor();

protected:
    le_int32 firstGlyph;
    le_int32 lastGlyph;

    const IndicRearrangementStateEntry *entryTable;
    const IndicRearrangementSubtableHeader *indicRearrangementSubtableHeader;
};

U_NAMESPACE_END
#endif
