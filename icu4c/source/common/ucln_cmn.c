/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2001, International Business Machines                   *
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

#include "unicode/uclean.h"
#include "unicode/uchar.h"
#include "ucln_cmn.h"
#include "umutex.h"
#include "ucln.h"

static cleanupFunc *gCleanupFunctions[UCLN_COMMON] = {
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
    ustrprep_cleanup();
#if !UCONFIG_NO_BREAK_ITERATION
	breakiterator_cleanup();
#endif
#if !UCONFIG_NO_SERVICE
    service_cleanup();
#endif
    ures_cleanup();
    locale_cleanup();
    uloc_cleanup();
    unorm_cleanup();
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
    umtx_destroy(NULL);
}


static UMTX     InitMutex = NULL;  

U_CAPI void U_EXPORT2
u_init(UErrorCode *status) {
    /*
     * Lock and unlock the global mutex, thus forcing into existence
     *   if it hasn't already been created.
     */
    umtx_lock(NULL);
    umtx_unlock(NULL);


    /* Lock a private mutex for the duration of the initialization,
     *  ensuring that there are no races through here, and to serve as
     *  a memory barrier.  Don't use the global mutex, because of the 
     *  possibility that some of the functions called within here might
     *  use it.
     */
    umtx_lock(&InitMutex);

    /*
     *  Do a simple operation using each of the data-requiring APIs that do
     *   not have a service object, ensuring that the required data is loaded.
     */


    /* Char Properties */
    u_hasBinaryProperty(0x20, UCHAR_WHITE_SPACE);

    /* Char Names  */
    {
        char buf[100];
        u_charName(0x20, U_UNICODE_CHAR_NAME,  buf, 100,  status);
    }


    /*  TODO:  the rest of 'em  */


    umtx_unlock(&InitMutex);

}
