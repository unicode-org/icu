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
#include "unicode/uloc.h"
#include "unicode/uidna.h"
#include "unormimp.h"
#include "ucln_cmn.h"
#include "umutex.h"
#include "ucln.h"

static UMTX     InitMutex = NULL;  

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
    umtx_destroy(&InitMutex);
    umtx_destroy(NULL);
}



/*
 *
 *   ICU Initialization Function.  Force loading and/or initialization of
 *           any shared data that could potentially be used concurrently
 *           multiple threads.
 */

U_CAPI void U_EXPORT2
u_init(UErrorCode *status) {
    /* Lock a private mutex for the duration of the initialization,
     *  ensuring that there are no races through here, and to serve as
     *  a memory barrier.  Don't use the global mutex, because of the 
     *  possibility that some of the functions called within here might
     *  use it.
     *
     * First use of a private mutex also forces global mutex into existance,
     *   it it wasn't already set up.
     */
    umtx_lock(&InitMutex);

    /* Do any required init for services that don't have open operations. 
     * TODO:  use of public APIs for the side effect of data initialization
     *        is risky because implementation changes might inadvertantly
     *        cause problems.
     */


    /* Locales */
    uprv_getDefaultLocaleID();
    uloc_countAvailable();


    /* IDNA.    */
    {
        UChar  nameSrc[] = {0x41, 0x42, 0x43, 0x00};
        UChar  nameDst[100];
        uidna_toASCII(nameSrc, 3, 
                      nameDst, 100, UIDNA_DEFAULT,
                      NULL,     /* UParseError pointer */
                      status);
    }


    /* Char Properties */
    uprv_haveProperties(status);

    /* Char Names.                         */
    {
        char buf[100];
        u_charName(0x20, U_UNICODE_CHAR_NAME,  buf, 100,  status);
    }


    /*  Normalization  */
    unorm_haveData(status);

    /* Time Zone.  TODO:  move data loading from I18n lib to common, so we don't   */
    /*                    have a dependency?                                       */
    /*  TODO: an implementation();                                                 */


    umtx_unlock(&InitMutex);
}
