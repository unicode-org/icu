/*
**********************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File UMUTEX.H
*
* Modification History:
*
*   Date        Name        Description
*   04/02/97  aliu        Creation.
*   04/07/99  srl         rewrite - C interface, multiple mutices
*   05/13/99  stephen     Changed to umutex (from cmutex)
********************************************************************************
*/

#ifndef UMUTEX_H
#define UMUTEX_H

#include "unicode/utypes.h"

#ifndef XP_CPLUSPLUS
typedef void * Mutex;
#endif

/*
 * Code within this library which accesses protected data should
 * instantiate a Mutex object while doing so.  Notice that there is
 * only one coarse-grained lock which applies to this entire library,
 * so keep locking short and sweet.
 *
 * For example:
 *
 * void Function(int arg1, int arg2)
 * {
 *   static Object* foo; // Shared read-write object
 *   Mutex mutex;
 *   foo->Method();
 *   // When 'mutex' goes out of scope and gets destroyed here
 *   // the lock is released
 * }
 *
 * Note: Do NOT use the form 'Mutex mutex();' as that merely
 * forward-declares a function returning a Mutex. This is a common
 * mistake which silently slips through the compiler!!  */


/* Lock a mutex. Pass in NULL if you want the (ick) Single Global
   Mutex. */
U_CAPI void  U_EXPORT2 umtx_lock   ( UMTX* mutex ); 

/* Unlock a mutex. Pass in NULL if you want the single global
   mutex. */
U_CAPI void U_EXPORT2 umtx_unlock ( UMTX* mutex );

/* Initialize a mutex. Use it this way:
   umtx_init( &aMutex ); */
U_CAPI void U_EXPORT2 umtx_init   ( UMTX* mutex );

#endif /*_CMUTEX*/
/*eof*/



