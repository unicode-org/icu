/*
 * @(#)DeviceTables.h	1.5 00/03/15
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000, 2001 - All Rights Reserved
 *
 */

#ifndef __DEVICETABLES_H
#define __DEVICETABLES_H

#include "LETypes.h"
#include "OpenTypeTables.h"
#include "GlyphIterator.h"
#include "GlyphPositionAdjustments.h"

U_NAMESPACE_BEGIN

struct DeviceTable
{
    le_uint16  startSize;
    le_uint16  endSize;
    le_uint16  deltaFormat;
    le_uint16  deltaValues[ANY_NUMBER];

    le_int16   getAdjustment(le_uint16 ppem) const;

private:
    static le_uint16 fieldMasks[];
    static le_uint16 fieldSignBits[];
    static le_uint16 fieldBits[];
};

U_NAMESPACE_END
#endif


