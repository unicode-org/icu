/*
 * @(#)OpenTypeUtilities.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __OPENTYPEUTILITIES_H
#define __OPENTYPEUTILITIES_H

#include "LETypes.h"
#include "OpenTypeTables.h"

U_NAMESPACE_BEGIN

class OpenTypeUtilities
{
public:
    static le_int8 highBit(le_int32 value);
    static Offset getTagOffset(LETag tag, const TagAndOffsetRecord *records, le_int32 recordCount);
    static le_int32 getGlyphRangeIndex(LEGlyphID glyphID, const GlyphRangeRecord *records, le_int32 recordCount);
    static le_int32 search(le_uint16 value, const le_uint16 array[], le_int32 count);
    static le_int32 search(le_uint32 value, const le_uint32 array[], le_int32 count);
};

U_NAMESPACE_END
#endif
