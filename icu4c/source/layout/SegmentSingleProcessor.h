/*
 * @(#)SegmentSingleProcessor.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __SEGMENTSINGLEPROCESSOR_H
#define __SEGMENTSINGLEPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"

class SegmentSingleProcessor : public NonContextualGlyphSubstitutionProcessor
{
public:
    virtual void process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount);

    SegmentSingleProcessor(const MorphSubtableHeader *morphSubtableHeader);

    virtual ~SegmentSingleProcessor();

private:
    SegmentSingleProcessor();

protected:
    const SegmentSingleLookupTable *segmentSingleLookupTable;
};

#endif

