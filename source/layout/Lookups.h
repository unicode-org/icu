/*
 * @(#)Lookups.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __LOOKUPS_H
#define __LOOKUPS_H

#include "LETypes.h"
#include "OpenTypeTables.h"

enum LookupFlags
{
    lfReservedBit           = 0x0001,
    lfIgnoreBaseGlyphs      = 0x0002,
    lfIgnoreLigatures       = 0x0004,
    lfIgnoreMarks           = 0x0008,
    lfReservedMask          = 0x00F0,
    lfMarkAttachTypeMask    = 0xFF00,
    lfMarkAttachTypeShift   = 8
};

struct LookupSubtable
{
    le_uint16 subtableFormat;
    Offset    coverageTableOffset;

    le_int32  getGlyphCoverage(LEGlyphID glyphID) const;
    le_int32  getGlyphCoverage(Offset tableOffset, LEGlyphID glyphID) const;
};

struct LookupTable
{
    le_uint16       lookupType;
    le_uint16       lookupFlags;
    le_uint16       subTableCount;
    Offset          subTableOffsetArray[ANY_NUMBER];

    const LookupSubtable  *getLookupSubtable(le_uint16 subtableIndex) const;
};

struct LookupListTable
{
    le_uint16   lookupCount;
    Offset      lookupTableOffsetArray[ANY_NUMBER];

    const LookupTable *getLookupTable(le_uint16 lookupTableIndex) const;
};

inline le_int32 LookupSubtable::getGlyphCoverage(LEGlyphID glyphID) const
{
    return getGlyphCoverage(coverageTableOffset, glyphID);
}



#endif
