/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2001, International Business Machines                   *
*                Corporation and others. All Rights Reserved.                *
*                                                                            *
******************************************************************************
*   file name:  ucln_cmn.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001July05
*   created by: George Rhoten
*/

#ifndef __UCLN_H__
#define __UCLN_H__

#include "unicode/utypes.h"

typedef enum ECleanupLibraryType {
    UCLN_START = -1,
    UCLN_LAYOUT,
    UCLN_USTDIO,
    UCLN_I18N,
    UCLN_COMMON /* This must be the last one to cleanup. */
};

#ifndef XP_CPLUSPLUS
typedef enum ECleanupLibraryType ECleanupLibraryType;
#endif

typedef UBool cleanupFunc(void);

U_CAPI void U_EXPORT2 ucln_registerCleanup(ECleanupLibraryType type,
                                           cleanupFunc *func);

#endif
