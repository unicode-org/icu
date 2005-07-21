/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2004, International Business Machines                   *
*                Corporation and others. All Rights Reserved.                *
*                                                                            *
******************************************************************************
*   file name:  ucln_cmn.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001July05
*   created by: George Rhoten
*/

#include "unicode/utypes.h"
#include "unicode/uclean.h"
#include "utracimp.h"
#include "ustr_imp.h"
#include "unormimp.h"
#include "ucln_cmn.h"
#include "umutex.h"
#include "ucln.h"
#include "cmemory.h"
#include "uassert.h"

static cleanupFunc *gCommonCleanupFunctions[UCLN_COMMON_COUNT];

void ucln_common_registerCleanup(ECleanupCommonType type,
                                 cleanupFunc *func)
{
    U_ASSERT(UCLN_COMMON_START < type && type < UCLN_COMMON_COUNT);
    if (UCLN_COMMON_START < type && type < UCLN_COMMON_COUNT)
    {
        gCommonCleanupFunctions[type] = func;
    }
}

U_CFUNC UBool ucln_common_lib_cleanup(void) {
    ECleanupCommonType commonFunc;

    for (commonFunc = UCLN_COMMON_START+1; commonFunc<UCLN_COMMON_COUNT; commonFunc++) {
        if (gCommonCleanupFunctions[commonFunc])
        {
            gCommonCleanupFunctions[commonFunc]();
            gCommonCleanupFunctions[commonFunc] = NULL;
        }
    }
    return TRUE;
}

