/*
 * @(#)SubtableProcessor.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __SUBTABLEPROCESSOR_H
#define __SUBTABLEPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"

U_NAMESPACE_BEGIN

class SubtableProcessor
{
public:
    virtual void process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyph) = 0;
    virtual ~SubtableProcessor();

protected:
    SubtableProcessor(const MorphSubtableHeader *morphSubtableHeader);

    SubtableProcessor();

    le_int16 length;
    SubtableCoverage coverage;
    FeatureFlags subtableFeatures;

    const MorphSubtableHeader *subtableHeader;
};

U_NAMESPACE_END
#endif

