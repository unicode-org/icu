/*
 * @(#)SimpleArrayProcessor.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __SIMPLEARRAYPROCESSOR_H
#define __SIMPLEARRAYPROCESSOR_H

#include "LETypes.h"
#include "MorphTables.h"
#include "SubtableProcessor.h"
#include "NonContextualGlyphSubst.h"
#include "NonContextualGlyphSubstProc.h"

U_NAMESPACE_BEGIN

class SimpleArrayProcessor : public NonContextualGlyphSubstitutionProcessor
{
public:
    virtual void process(LEGlyphID *glyphs, le_int32 *charIndices, le_int32 glyphCount);

    SimpleArrayProcessor(const MorphSubtableHeader *morphSubtableHeader);

    virtual ~SimpleArrayProcessor();

private:
    SimpleArrayProcessor();

protected:
    const SimpleArrayLookupTable *simpleArrayLookupTable;
};

U_NAMESPACE_END
#endif

