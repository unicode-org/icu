/*
 * @(#)Lookups.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "Lookups.h"
#include "CoverageTables.h"
#include "LESwaps.h"

LookupTable *LookupListTable::getLookupTable(le_uint16 lookupTableIndex)
{
    if (lookupTableIndex >= SWAPW(lookupCount)) {
        return 0;
    }

    Offset lookupTableOffset = lookupTableOffsetArray[lookupTableIndex];

    return (LookupTable *) ((char *) this + SWAPW(lookupTableOffset));
}

LookupSubtable *LookupTable::getLookupSubtable(le_uint16 subtableIndex)
{
    if (subtableIndex >= SWAPW(subTableCount)) {
        return 0;
    }

    Offset subtableOffset = subTableOffsetArray[subtableIndex];

    return (LookupSubtable *) ((char *) this + SWAPW(subtableOffset));
}

le_int32 LookupSubtable::getGlyphCoverage(Offset tableOffset, LEGlyphID glyphID)
{
    CoverageTable *coverageTable = (CoverageTable *) ((char *) this + SWAPW(tableOffset));

    return coverageTable->getGlyphCoverage(glyphID);
}

