/*
**********************************************************************
*   Copyright (C) 1997-2001, International Business Machines
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
******************************************************************************
*/

#ifndef UMUTEX_H
#define UMUTEX_H

#include "unicode/utypes.h"

/**
 * Mutex data type.
 * @internal
 */
typedef void *UMTX;

/* APP_NO_THREADS is an old symbol. We'll honour it if present. */
#ifdef APP_NO_THREADS
# define ICU_USE_THREADS 0
#endif

/* Default: use threads. */
#ifndef ICU_USE_THREADS
# define ICU_USE_THREADS 1
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
   Mutex. 
 * @param mutex The given mutex to be locked
 */
U_CAPI void U_EXPORT2 umtx_lock   ( UMTX* mutex ); 

/* Unlock a mutex. Pass in NULL if you want the single global
   mutex. 
 * @param mutex The given mutex to be unlocked
 */
U_CAPI void U_EXPORT2 umtx_unlock ( UMTX* mutex );

/* Initialize a mutex. Use it this way:
   umtx_init( &aMutex ); 
 * ICU Mutexes, aside from the global mutex, must be explicitly initialized
 * before use.
 * @param mutex The given mutex to be initialized
 */
U_CAPI void U_EXPORT2 umtx_init   ( UMTX* mutex );

/* Destroy a mutex. This will free the resources of a mutex.
   Use it this way:
   umtx_destroy( &aMutex ); 
 * @param mutex The given mutex to be destroyed
 */
U_CAPI void U_EXPORT2 umtx_destroy( UMTX *mutex );

/* Is a mutex initialized? 
   Use it this way:
      umtx_isInitialized( &aMutex ); 
   This function is not normally needed.  It is more efficient to 
   unconditionally call umtx_init(&aMutex) than it is to check first. 
 * @param mutex The given mutex to be tested
*/
U_CAPI UBool U_EXPORT2 umtx_isInitialized( UMTX *mutex );

/*
 * Atomic Increment and Decrement of an int32_t value.
 *
 * Return Values:
 *   If the result of the operation is zero, the return zero.
 *   If the result of the operation is not zero, the sign of returned value
 *      is the same as the sign of the result, but the returned value itself may
 *      be different from the result of the operation.
 */
U_CAPI int32_t U_EXPORT2 umtx_atomic_inc(int32_t *);
U_CAPI int32_t U_EXPORT2 umtx_atomic_dec(int32_t *);


#endif /*_CMUTEX*/
/*eof*/



