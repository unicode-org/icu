/*
 * @(#)ContextualGlyphSubstProc.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __CONTEXTUALGLYPHSUBSTITUTIONPROCESSOR_H
#define __CONTEXTUALGLYPHSUBSTITUTIONPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "StateTableProcessor.h"
#include "ContextualGlyphSubstitution.h"

class ContextualGlyphSubstitutionProcessor : public StateTableProcessor
{
public:
    virtual void beginStateTable();

    virtual ByteOffset processStateEntry(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 &currGlyph,
        le_int32 glyphCount, EntryTableIndex index);

    virtual void endStateTable();

    ContextualGlyphSubstitutionProcessor(const MorphSubtableHeader *morphSubtableHeader);
    virtual ~ContextualGlyphSubstitutionProcessor();

private:
    ContextualGlyphSubstitutionProcessor();

protected:
    ByteOffset substitutionTableOffset;
    const ContextualGlyphSubstitutionStateEntry *entryTable;

    le_int32 markGlyph;

    const ContextualGlyphSubstitutionHeader *contextualGlyphSubstitutionHeader;
};

#endif
