/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
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

#include "loccache.h"

#include "uloc.h"
#include "umutex.h"

/* The global cache */
UHashtable     *gLocaleCache         = 0;

/* my custom hashing function */
int32_t
u_locbund_hash(const void *parm)
{
  return uhash_hashString(((ULocaleBundle*)parm)->fLocale);
}

ULocaleBundle*
u_loccache_get(const char *loc)
{
  ULocaleBundle *result;
  ULocaleBundle *tempBundle;
  /*Mutex     *lock;*/
  UHashtable     *tempCache;
  int32_t     locCount;
  int32_t     hashKey;
  UErrorCode     status = U_ZERO_ERROR;

  /* Create the cache, if needed */
  if(gLocaleCache == 0) {
    locCount = uloc_countAvailable();
    
    tempCache = uhash_openSize(u_locbund_hash, locCount, &status);
    if(U_FAILURE(status)) return 0;
    
    /* Lock the cache */
    umtx_lock(0);
    /* Make sure it didn't change while we were acquiring the lock */
    if(gLocaleCache == 0) {
      gLocaleCache = tempCache;
    }
    else {
      uhash_close(tempCache);
    }
    
    /* Unlock the cache */
    umtx_unlock(0);
  }
  
  /* Try and get the bundle from the cache */
  /* This will be slightly wasteful the first time around, */
  /* since we know the cache will be empty.  But, it simplifies */
  /* the code a great deal. */
  
  hashKey = uhash_hashString(loc);
  result = uhash_get(gLocaleCache, hashKey);

  /* If the bundle wasn't found, create it and add it to the cache */
  if(result == 0) {
    /* Create the bundle */
    tempBundle = u_locbund_new(loc);

    /* Lock the cache */
    umtx_lock(0);
    
    /* Make sure the cache didn't change while we were locking it */
    result = uhash_get(gLocaleCache, hashKey);
    if(result == 0) {
      result = tempBundle;
      uhash_put(gLocaleCache, result, &status);
    }
    else
      u_locbund_delete(tempBundle);
    
    /* Unlock the cache */
    umtx_unlock(0);
  }
  
  return result;
}
