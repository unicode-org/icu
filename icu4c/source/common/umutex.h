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
#include "unicode/uclean.h"  


/* APP_NO_THREADS is an old symbol. We'll honour it if present. */
#ifdef APP_NO_THREADS
# define ICU_USE_THREADS 0
#endif

/* Default: use threads. */
#ifndef ICU_USE_THREADS
# define ICU_USE_THREADS 1
#endif

/*
 * Code within ICU that accesses shared static or global data should
 * instantiate a Mutex object while doing so.  The unnamed global mutex
 * is used throughout ICU, so keep locking short and sweet.
 *
 * For example:
 *
 * void Function(int arg1, int arg2)
 * {
 *   static Object* foo;     // Shared read-write object
 *   umtx_lock(NULL);        // Lock the ICU global mutex
 *   foo->Method();
 *   umtx_unlock(NULL);
 * }
 *
 * an alternative C++ mutex API is defined in the file common/mutex.h
 */

/* Lock a mutex. 
 * @param mutex The given mutex to be locked.  Pass NULL to specify
 *              the global ICU mutex.  Recursive locks are an error
 *              and may cause a deadlock on some platforms.
 */
U_CAPI void U_EXPORT2 umtx_lock   ( UMTX* mutex ); 

/* Unlock a mutex. Pass in NULL if you want the single global
   mutex. 
 * @param mutex The given mutex to be unlocked.  Pass NULL to specify
 *              the global ICU mutex.
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

/* Test whether an ICU mutex is initialized. 
 * This function is intended for use from test programs only,
 * there should be no need for it from normal code.
 * If there is any question about whether a mutex has been initialized,
 *  simply initialize it again with umtx_init(), which will have
 *  no effect if the mutex is already initialized.
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



