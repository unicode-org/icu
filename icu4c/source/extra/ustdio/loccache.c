/*
*******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File loccache.c
*
* Modification History:
*
*   Date        Name        Description
*   11/18/98    stephen     Creation.
*   03/11/99    stephen     Modified for new C API.
*   06/16/99    stephen     Added #include for uloc.h
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "loccache.h"
#include "uhash.h"
#include "unicode/uloc.h"
#include "umutex.h"
#include "ucln.h"

/* The global cache */
static UHashtable     *gLocaleCache = NULL;

static void U_CALLCONV
hash_freeBundle(void* obj)
{
    u_locbund_delete((ULocaleBundle *)obj);
}

ULocaleBundle*
u_loccache_get(const char *loc)
{
    ULocaleBundle *result;
    UErrorCode     status = U_ZERO_ERROR;

    /* Create the cache, if needed */
    if(gLocaleCache == NULL) {
        UHashtable    *tempCache;
        int32_t        locCount = uloc_countAvailable();

        tempCache = uhash_openSize(uhash_hashChars, uhash_compareChars, locCount, &status);
        if(U_FAILURE(status))
            return NULL;

        uhash_setValueDeleter(tempCache, hash_freeBundle);

        /* Lock the cache */
        umtx_lock(NULL);
        /* Make sure it didn't change while we were acquiring the lock */
        if(gLocaleCache == NULL) {
            gLocaleCache = tempCache;
        }
        else {
            uhash_close(tempCache);
        }

        /* Unlock the cache */
        umtx_unlock(NULL);
        ucln_ustdio_registerCleanup();
    }

    /* Try and get the bundle from the cache */
    /* This will be slightly wasteful the first time around, */
    /* since we know the cache will be empty.  But, it simplifies */
    /* the code a great deal. */

    result = (ULocaleBundle*)uhash_get(gLocaleCache, loc);

    /* If the bundle wasn't found, create it and add it to the cache */
    if(result == NULL) {
        /* Create the bundle */
        ULocaleBundle *tempBundle = u_locbund_new(loc);

        /* Lock the cache */
        umtx_lock(NULL);

        /* Make sure the cache didn't change while we were locking it */
        result = (ULocaleBundle*)uhash_get(gLocaleCache, loc);
        if(result == NULL) {
            result = tempBundle;
            uhash_put(gLocaleCache, tempBundle->fLocale, tempBundle, &status);
        }
        else {
            u_locbund_delete(tempBundle);
        }

        /* Unlock the cache */
        umtx_unlock(NULL);
    }

    return result;
}

static UBool loccache_cleanup()
{
    if (gLocaleCache) {
        uhash_close(gLocaleCache);
        gLocaleCache = NULL;
    }
    return TRUE;                   /* Everything was cleaned up */
}

static UBool ustdio_cleanup(void)
{
    return loccache_cleanup();
}

void ucln_ustdio_registerCleanup()
{
    ucln_registerCleanup(UCLN_USTDIO, ustdio_cleanup);
}

#endif /* #if !UCONFIG_NO_FORMATTING */
