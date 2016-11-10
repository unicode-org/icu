/*
******************************************************************************
*
*   Copyright (C) 2016 and later: Unicode, Inc. and others.
*   License & terms of use: http://www.unicode.org/copyright.html
*
******************************************************************************
*
* File uassert.h
*
*  Contains U_ASSERT macro
*
*    By default, U_ASSERT just wraps the C library assert macro.
*    By changing the definition here, the assert behavior for ICU can be changed
*    without affecting other non-ICU uses of the C library assert().
*
******************************************************************************
*/

#ifndef U_ASSERT_H
#define U_ASSERT_H
/* utypes.h is included to get the proper define for uint8_t */
#include "unicode/utypes.h"
#if U_DEBUG
#   include <assert.h>
#   define U_ASSERT(exp) assert(exp)
#else
#   define U_ASSERT(exp)
#endif
#endif


