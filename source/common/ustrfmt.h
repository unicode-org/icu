/*
**********************************************************************
*   Copyright (C) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/

#ifndef USTRFMT_H
#define USTRFMT_H

#include "unicode/utypes.h"

U_CAPI double U_EXPORT2 
uprv_strtod(const char* source, char** end);
U_CAPI char* U_EXPORT2 
uprv_dtostr(double value, char *buffer, int maximumDigits,UBool fixedPoint);
U_CAPI int32_t U_EXPORT2
uprv_itou (UChar * buffer, int32_t capacity, uint32_t i, uint32_t radix, int32_t minwidth);


#endif
