/*
 * @(#)TrimmedArrayProcessor.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __TRIMMEDARRAYPROCESSOR_H
#define __TRIMMEDARRAYPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubstitution.h"
#include "NonContextualGlyphSubstitutionProcessor.h"

class TrimmedArrayProcessor : public NonContextualGlyphSubstitutionProcessor
{
public:
    virtual void process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount);

    TrimmedArrayProcessor(MorphSubtableHeader *morphSubtableHeader);

    virtual ~TrimmedArrayProcessor();

private:
    TrimmedArrayProcessor();

protected:
    le_int16 firstGlyph;
    le_int16 lastGlyph;
    TrimmedArrayLookupTable *trimmedArrayLookupTable;
};

#endif

