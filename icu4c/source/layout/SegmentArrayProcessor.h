/*
 * @(#)SegmentArrayProcessor.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __SEGMENTARRAYPROCESSOR_H
#define __SEGMENTARRAYPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"

class SegmentArrayProcessor : public NonContextualGlyphSubstitutionProcessor
{
public:
    virtual void process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyph);

    SegmentArrayProcessor(MorphSubtableHeader *morphSubtableHeader);

    virtual ~SegmentArrayProcessor();

private:
    SegmentArrayProcessor();

protected:
    SegmentArrayLookupTable *segmentArrayLookupTable;
};

#endif

