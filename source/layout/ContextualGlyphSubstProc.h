/*
 * @(#)ContextualGlyphSubstProc.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#ifndef __CONTEXTUALGLYPHSUBSTITUTIONPROCESSOR_H
#define __CONTEXTUALGLYPHSUBSTITUTIONPROCESSOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "StateTableProcessor.h"
#include "ContextualGlyphSubstitution.h"

U_NAMESPACE_BEGIN

class ContextualGlyphSubstitutionProcessor : public StateTableProcessor
{
public:
    virtual void beginStateTable();

    virtual ByteOffset processStateEntry(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 &currGlyph,
        le_int32 glyphCount, EntryTableIndex index);

    virtual void endStateTable();

    ContextualGlyphSubstitutionProcessor(const MorphSubtableHeader *morphSubtableHeader);
    virtual ~ContextualGlyphSubstitutionProcessor();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

private:
    ContextualGlyphSubstitutionProcessor();

protected:
    ByteOffset substitutionTableOffset;
    const ContextualGlyphSubstitutionStateEntry *entryTable;

    le_int32 markGlyph;

    const ContextualGlyphSubstitutionHeader *contextualGlyphSubstitutionHeader;

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END
#endif
