/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2003, International Business Machines                   *
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

#include "ucln.h"
#include "ucln_in.h"

/* Leave this copyright notice here! It needs to go somewhere in this library. */
static const char copyright[] = U_COPYRIGHT_STRING;

static cleanupFunc *gCleanupFunctions[UCLN_I18N_COUNT];

static UBool i18n_cleanup(void)
{
    ECleanupLibraryType libType;

    for (libType = UCLN_I18N_START+1; libType<UCLN_I18N_COUNT; libType++) {
        if (gCleanupFunctions[libType])
        {
            gCleanupFunctions[libType]();
            gCleanupFunctions[libType] = NULL;
        }
    }
    return TRUE;
}

void ucln_i18n_registerCleanup(ECleanupI18NType type,
                               cleanupFunc *func)
{
    U_ASSERT(UCLN_I18N_START < type && type < UCLN_I18N_COUNT);
    ucln_registerCleanup(UCLN_I18N, i18n_cleanup);
    if (UCLN_I18N_START < type && type < UCLN_I18N_COUNT)
    {
        gCleanupFunctions[type] = func;
    }
}

