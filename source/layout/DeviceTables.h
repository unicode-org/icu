/*
 * @(#)DeviceTables.h   1.5 00/03/15
 *
 * Copyright (C) 2016 and later: Unicode, Inc. and others. License & terms of use: http://www.unicode.org/copyright.html
 *
 */

#ifndef __DEVICETABLES_H
#define __DEVICETABLES_H

/**
 * \file
 * \internal
 */

#include "LETypes.h"
#include "OpenTypeTables.h"

U_NAMESPACE_BEGIN

struct DeviceTable
{
    le_uint16  startSize;
    le_uint16  endSize;
    le_uint16  deltaFormat;
    le_uint16  deltaValues[ANY_NUMBER];

    le_int16   getAdjustment(le_uint16 ppem) const;

private:
    static const le_uint16 fieldMasks[];
    static const le_uint16 fieldSignBits[];
    static const le_uint16 fieldBits[];
};
LE_VAR_ARRAY(DeviceTable, deltaValues)

U_NAMESPACE_END
#endif


