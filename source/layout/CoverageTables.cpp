/*
 * @(#)CoverageTables.cpp	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "OpenTypeUtilities.h"
#include "CoverageTables.h"
#include "LESwaps.h"

U_NAMESPACE_BEGIN

le_int32 CoverageTable::getGlyphCoverage(LEGlyphID glyphID) const
{
    switch(SWAPW(coverageFormat))
    {
    case 0:
        return -1;

    case 1:
    {
        const CoverageFormat1Table *f1Table = (const CoverageFormat1Table *) this;

        return f1Table->getGlyphCoverage(glyphID);
    }

    case 2:
    {
        const CoverageFormat2Table *f2Table = (const CoverageFormat2Table *) this;

        return f2Table->getGlyphCoverage(glyphID);
    }

    default:
        return -1;
    }
}

le_int32 CoverageFormat1Table::getGlyphCoverage(LEGlyphID glyphID) const
{
    le_uint16 count = SWAPW(glyphCount);
    le_uint8 bit = OpenTypeUtilities::highBit(count);
    le_uint16 power = 1 << bit;
    le_uint16 extra = count - power;
    le_uint16 probe = power;
    le_uint16 index = 0;

    if (SWAPW(glyphArray[extra]) <= glyphID) {
        index = extra;
    }

    while (probe > (1 << 0)) {
        probe >>= 1;

        if (SWAPW(glyphArray[index + probe]) <= glyphID) {
            index += probe;
        }
    }

    if (SWAPW(glyphArray[index]) == glyphID) {
        return index;
    }

    return -1;
}

le_int32 CoverageFormat2Table::getGlyphCoverage(LEGlyphID glyphID) const
{
    le_uint16 count = SWAPW(rangeCount);
    le_int32 rangeIndex =
        OpenTypeUtilities::getGlyphRangeIndex(glyphID, rangeRecordArray, count);

    if (rangeIndex < 0) {
        return -1;
    }

    LEGlyphID firstInRange = SWAPW(rangeRecordArray[rangeIndex].firstGlyph);
    le_uint16  startCoverageIndex = SWAPW(rangeRecordArray[rangeIndex].rangeValue);

    return startCoverageIndex + (glyphID - firstInRange);
}

U_NAMESPACE_END
