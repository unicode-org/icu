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

#include "unicode/utypes.h"
#include "unicode/uclean.h"
#include "ustr_imp.h"
#include "unormimp.h"
#include "ucln_cmn.h"
#include "umutex.h"
#include "ucln.h"
#include "cmemory.h"

static UBool gICUInitialized = FALSE;
static UMTX  gICUInitMutex;


static cleanupFunc *gCleanupFunctions[UCLN_COMMON] = {
    NULL,
    NULL,
    NULL,
    NULL,
    NULL
};

U_CAPI void U_EXPORT2
ucln_registerCleanup(ECleanupLibraryType type,
                     cleanupFunc *func)
{
    if (UCLN_START < type && type < UCLN_COMMON)
    {
        gCleanupFunctions[type] = func;
    }
}

/************************************************
 The cleanup order is important in this function.
 Please be sure that you have read ucln.h
 ************************************************/
U_CAPI void U_EXPORT2
u_cleanup(void)
{

    ECleanupLibraryType libType = UCLN_START;
    while (++libType < UCLN_COMMON)
    {
        if (gCleanupFunctions[libType])
        {
            gCleanupFunctions[libType]();
            gCleanupFunctions[libType] = NULL;
        }

    }
#if !UCONFIG_NO_IDNA
    usprep_cleanup();
#endif
#if !UCONFIG_NO_BREAK_ITERATION
	breakiterator_cleanup();
#endif
#if !UCONFIG_NO_SERVICE
    service_cleanup();
#endif
    ures_cleanup();
    locale_cleanup();
    uloc_cleanup();
#if !UCONFIG_NO_NORMALIZATION
    unorm_cleanup();
#endif
    uset_cleanup();
    unames_cleanup();
    pname_cleanup();
    uchar_cleanup();
    ucnv_cleanup();
    ucnv_io_cleanup();
    udata_cleanup();
    putil_cleanup();

    umtx_destroy(&gICUInitMutex);
    umtx_cleanup();
    cmemory_cleanup();       /* undo any heap functions set by u_setMemoryFunctions(). */
    gICUInitialized = FALSE;
}



/*
 *
 *   ICU Initialization Function.  Force loading and/or initialization of
 *           any shared data that could potentially be used concurrently
 *           by multiple threads.
 */

U_CAPI void U_EXPORT2
u_init(UErrorCode *status) {
    /* Make sure the global mutexes are initialized. */
    umtx_init(NULL);
    umtx_lock(&gICUInitMutex);
    if (gICUInitialized || U_FAILURE(*status)) {
        umtx_unlock(&gICUInitMutex);
        return;
    }

    ucnv_init(status);
    ures_init(status);

    /* Do any required init for services that don't have open operations
     * and use "only" the double-check initialization method for performance
     * reasons (avoiding a mutex lock even for _checking_ whether the
     * initialization had occurred).
     */

    /* Char Properties */
    uprv_haveProperties(status);

#if !UCONFIG_NO_NORMALIZATION
    /*  Normalization  */
    unorm_haveData(status);
#endif
    gICUInitialized = TRUE;    /* TODO:  don't set if U_FAILURE? */
    umtx_unlock(&gICUInitMutex);
}


U_CFUNC UBool u_isUInit() {
    return gICUInitialized;
}
