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
    /*
     * WARNING! Destroying the global mutex can cause synchronization
     * problems.  ICU must be reinitialized from a single thread
     * before the library is used again.  You never want two
     * threads trying to initialize the global mutex at the same
     * time. The global mutex is being destroyed so that heap and
     * resource checkers don't complain. [grhoten]
     */
    umtx_cleanup();
    cmemory_cleanup();       /* undo any heap functions set by u_setMemoryFunctions(). */
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
     u_ICUStaticInitFunc();

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
}


/*
 *  ICU Static Initialization Function
 *
 *     Does that portion of ICU's initialization that wants to happen at C++
 *     static initialization time.  Can also be called directly if the same
 *     initialization is needed later.
 *
 *     The effect is to initialize mutexes that are required during the
 *     lazy initialization of other parts of ICU.
 */
U_CFUNC UBool u_ICUStaticInitFunc()
{
    UErrorCode status = U_ZERO_ERROR;

#if (ICU_USE_THREADS == 1)
/* Initialize mutexes only if threading is supported */
    UBool heapInUse = cmemory_inUse();
    umtx_init(NULL);
    ucnv_init(&status);
    ures_init(&status);
    if (heapInUse == FALSE) {
        /* If there was no use of ICU prior to calling this static init function,
         *  pretend that there is still no use, even though the various inits may
         *  have done some heap allocation. */
        cmemory_clearInUse();
    }
#endif /* ICU_USE_THREADS==1 */
    return TRUE;
}


/*
 *  Static Uninitialization Function
 *
 *     Reverse the effects of ICU static initialization.
 *     This is needed by u_setMutexFunctions(), which must get rid of any mutexes,
 *     and associated memory, before swapping in the user's mutex funcs.
 *
 *     Do NOT call cmemory_cleanup().  We don't want to cancel the effect of
 *     any u_setHeapFunctions().
 *
 *     Similarly, do not call umtx_cleanup(); we need to keep any user-set
 *     mutex callback functions.
 */
U_CFUNC void u_ICUStaticUnInitFunc() {
    ucnv_cleanup();
    ures_cleanup();
    umtx_destroy(NULL);
}

