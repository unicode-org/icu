/*
 * @(#)AnchorTables.h	1.6 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __ANCHORTABLES_H
#define __ANCHORTABLES_H

#include "LETypes.h"
#include "LEFontInstance.h"
#include "OpenTypeTables.h"

U_NAMESPACE_BEGIN

struct AnchorTable
{
    le_uint16  anchorFormat;
    le_int16   xCoordinate;
    le_int16   yCoordinate;

    void    getAnchor(LEGlyphID glyphID, const LEFontInstance *fontInstance,
                      LEPoint &anchor) const;
};

struct Format1AnchorTable : AnchorTable
{
    void getAnchor(const LEFontInstance *fontInstance, LEPoint &anchor) const;
};

struct Format2AnchorTable : AnchorTable
{
    le_uint16  anchorPoint;

    void getAnchor(LEGlyphID glyphID, const LEFontInstance *fontInstance, LEPoint &anchor) const;
};

struct Format3AnchorTable : AnchorTable
{
    Offset  xDeviceTableOffset;
    Offset  yDeviceTableOffset;

    void getAnchor(const LEFontInstance *fontInstance, LEPoint &anchor) const;
};

U_NAMESPACE_END
#endif


