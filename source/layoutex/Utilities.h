/*
 **********************************************************************
 *   Copyright (C) 2002, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
 */

#ifndef __UTILITIES_H

#define __UTILITIES_H

#include "layout/LETypes.h"

U_NAMESPACE_BEGIN

class Utilities
{
public:
    static le_int8 highBit(le_int32 value);
    static le_int32 search(le_int32 value, const le_int32 array[], le_int32 count);
    static void reverse(le_int32 array[], le_int32 count);
    static void reverse(float array[], le_int32 count);
};

U_NAMESPACE_END
#endif
