/*
 * @(#)TrimmedArrayProcessor.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998-2003 - All Rights Reserved
 *
 */

#ifndef __TRIMMEDARRAYPROCESSOR_H
#define __TRIMMEDARRAYPROCESSOR_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"

U_NAMESPACE_BEGIN

class TrimmedArrayProcessor : public NonContextualGlyphSubstitutionProcessor
{
public:
    virtual void process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount);

    TrimmedArrayProcessor(const MorphSubtableHeader *morphSubtableHeader);

    virtual ~TrimmedArrayProcessor();

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
    TrimmedArrayProcessor();

protected:
    le_int16 firstGlyph;
    le_int16 lastGlyph;
    const TrimmedArrayLookupTable *trimmedArrayLookupTable;

private:

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;
};

U_NAMESPACE_END
#endif

