/*
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __MARKARRAYS_H
#define __MARKARRAYS_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"

U_NAMESPACE_BEGIN

struct MarkRecord
{
    le_uint16   markClass;
    Offset      markAnchorTableOffset;
};

struct MarkArray
{
    le_uint16   markCount;
    MarkRecord  markRecordArray[ANY_NUMBER];

    le_int32 getMarkClass(LEGlyphID glyphID, le_int32 coverageIndex, const LEFontInstance *fontInstance,
        LEPoint &anchor) const;
};
LE_VAR_ARRAY(MarkArray, markRecordArray)

U_NAMESPACE_END
#endif


