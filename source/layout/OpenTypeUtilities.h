/*
 * @(#)OpenTypeUtilities.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __OPENTYPEUTILITIES_H
#define __OPENTYPEUTILITIES_H

#include "LETypes.h"
#include "OpenTypeTables.h"

class OpenTypeUtilities
{
public:
    static le_int8 highBit(le_int32 value);
    static Offset getTagOffset(LETag tag, TagAndOffsetRecord *records, le_int32 recordCount);
    static le_int32 getGlyphRangeIndex(LEGlyphID glyphID, GlyphRangeRecord *records, le_int32 recordCount);
    static le_int32 search(le_uint16 value, le_uint16 array[], le_int32 count);
    static le_int32 search(le_uint32 value, le_uint32 array[], le_int32 count);
};

#endif
