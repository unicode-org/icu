/*
 * @(#)LigatureSubstProc.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __LIGATURESUBSTITUTIONPROCESSOR_H
#define __LIGATURESUBSTITUTIONPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "StateTableProcessor.h"
#include "LigatureSubstitution.h"


#define nComponents 16

class LigatureSubstitutionProcessor : public StateTableProcessor
{
public:
    virtual void beginStateTable();

    virtual ByteOffset processStateEntry(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 &currGlyph,
        le_int32 glyphCount, EntryTableIndex index);

    virtual void endStateTable();

    LigatureSubstitutionProcessor(MorphSubtableHeader *morphSubtableHeader);
    virtual ~LigatureSubstitutionProcessor();

private:
    LigatureSubstitutionProcessor();

protected:
    ByteOffset ligatureActionTableOffset;
    ByteOffset componentTableOffset;
    ByteOffset ligatureTableOffset;

    LigatureSubstitutionStateEntry *entryTable;

    le_int32 componentStack[nComponents];
    le_int16 m;

    LigatureSubstitutionHeader *ligatureSubstitutionHeader;
};

#endif
