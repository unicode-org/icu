/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2004, International Business Machines                   *
*                Corporation and others. All Rights Reserved.                *
*                                                                            *
******************************************************************************
*   file name:  uinit.c
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

static UBool gICUInitialized = FALSE;
static UMTX  gICUInitMutex   = NULL;


static cleanupFunc *gLibCleanupFunctions[UCLN_COMMON];

U_CAPI void U_EXPORT2
ucln_registerCleanup(ECleanupLibraryType type,
                     cleanupFunc *func)
{
    U_ASSERT(UCLN_START < type && type < UCLN_COMMON);
    if (UCLN_START < type && type < UCLN_COMMON)
    {
        gLibCleanupFunctions[type] = func;
    }
}

/************************************************
 The cleanup order is important in this function.
 Please be sure that you have read ucln.h
 ************************************************/
U_CAPI void U_EXPORT2
u_cleanup(void)
{
    ECleanupLibraryType libType;

    UTRACE_ENTRY_OC(UTRACE_U_CLEANUP);
    umtx_lock(NULL);     /* Force a memory barrier, so that we are sure to see   */
    umtx_unlock(NULL);   /*   all state left around by any other threads.        */

    for (libType = UCLN_START+1; libType<UCLN_COMMON; libType++) {
        if (gLibCleanupFunctions[libType])
        {
            gLibCleanupFunctions[libType]();
            gLibCleanupFunctions[libType] = NULL;
        }
    }

    ucln_common_lib_cleanup();

    umtx_destroy(&gICUInitMutex);
    umtx_cleanup();
    cmemory_cleanup();       /* undo any heap functions set by u_setMemoryFunctions(). */
    gICUInitialized = FALSE;
    UTRACE_EXIT();           /* Must be before utrace_cleanup(), which turns off tracing. */
    utrace_cleanup();
}

/*
 *
 *   ICU Initialization Function.  Force loading and/or initialization of
 *           any shared data that could potentially be used concurrently
 *           by multiple threads.
 */
U_CAPI void U_EXPORT2
u_init(UErrorCode *status) {
    UTRACE_ENTRY_OC(UTRACE_U_INIT);
    /* Make sure the global mutexes are initialized. */
    umtx_init(NULL);
    umtx_lock(&gICUInitMutex);
    if (gICUInitialized || U_FAILURE(*status)) {
        umtx_unlock(&gICUInitMutex);
        UTRACE_EXIT_STATUS(*status);
        return;
    }

    /* Do any required init for services that don't have open operations
     * and use "only" the double-check initialization method for performance
     * reasons (avoiding a mutex lock even for _checking_ whether the
     * initialization had occurred).
     */

    /* Char Properties */
    uprv_loadPropsData(status);

#if !UCONFIG_NO_NORMALIZATION
    /*  Normalization  */
    unorm_haveData(status);
#endif
    gICUInitialized = TRUE;    /* TODO:  don't set if U_FAILURE? */
    umtx_unlock(&gICUInitMutex);
    UTRACE_EXIT_STATUS(*status);
}

