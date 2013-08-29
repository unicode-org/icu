/*
******************************************************************************
*
*   Copyright (C) 1997-2013, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File umutex.cpp
*
* Modification History:
*
*   Date        Name        Description
*   04/02/97    aliu        Creation.
*   04/07/99    srl         updated
*   05/13/99    stephen     Changed to umutex (from cmutex).
*   11/22/99    aliu        Make non-global mutex autoinitialize [j151]
******************************************************************************
*/

#include "umutex.h"

#include "unicode/utypes.h"
#include "uassert.h"
#include "cmemory.h"
#include "ucln_cmn.h"

// The ICU global mutex. Used when ICU implementation code passes NULL for the mutex pointer.
static UMutex   globalMutex = U_MUTEX_INITIALIZER;

/*
 * ICU Mutex wrappers.  Wrap operating system mutexes, giving the rest of ICU a
 * platform independent set of mutex operations.  For internal ICU use only.
 */

#if U_PLATFORM_HAS_WIN32_API

//-------------------------------------------------------------------------------------------
//
//    Windows Specific Definitions
//
//        Note: Cygwin (and possibly others) have both WIN32 and POSIX.
//              Prefer Win32 in these cases.  (Win32 comes ahead in the #if chain)
//
//-------------------------------------------------------------------------------------------

#if defined U_NO_PLATFORM_ATOMICS
#error ICU on Win32 requires support for low level atomic operations.
// Visual Studio, gcc, clang are OK. Shouldn't get here.
#endif


// This function is called when a test of a UInitOnce::fState reveals that
//   initialization has not completed, that we either need to call the
//   function on this thread, or wait for some other thread to complete.
//
// The actual call to the init function is made inline by template code
//   that knows the C++ types involved. This function returns TRUE if
//   the caller needs to call the Init function.
//

U_CAPI UBool U_EXPORT2 umtx_initImplPreInit(UInitOnce &uio) {
    for (;;) {
        int32_t previousState = InterlockedCompareExchange(
#if (U_PLATFORM == U_PF_MINGW) || (U_PLATFORM == U_PF_CYGWIN)
           (LONG volatile *) // this is the type given in the API doc for this function.
#endif
            &uio.fState,  //  Destination
            1,            //  Exchange Value
            0);           //  Compare value

        if (previousState == 0) {
            return true;   // Caller will next call the init function.
                           // Current state == 1.
        } else if (previousState == 2) {
            // Another thread already completed the initialization.
            //   We can simply return FALSE, indicating no
            //   further action is needed by the caller.
            return FALSE;
        } else {
            // Another thread is currently running the initialization.
            // Wait until it completes.
            do {
                Sleep(1);
                previousState = umtx_loadAcquire(uio.fState);
            } while (previousState == 1);
        }
    }
}

// This function is called by the thread that ran an initialization function,
// just after completing the function.
//
//   success: True:  the inialization succeeded. No further calls to the init
//                   function will be made.
//            False: the initializtion failed. The next call to umtx_initOnce()
//                   will retry the initialization.

U_CAPI void U_EXPORT2 umtx_initImplPostInit(UInitOnce &uio, UBool success) {
    int32_t nextState = success? 2: 0;
    umtx_storeRelease(uio.fState, nextState);
}


static void winMutexInit(CRITICAL_SECTION *cs) {
    InitializeCriticalSection(cs);
    return;
}

U_CAPI void  U_EXPORT2
umtx_lock(UMutex *mutex) {
    if (mutex == NULL) {
        mutex = &globalMutex;
    }
    CRITICAL_SECTION *cs = &mutex->fCS;
    umtx_initOnce(mutex->fInitOnce, winMutexInit, cs);
    EnterCriticalSection(cs);
}

U_CAPI void  U_EXPORT2
umtx_unlock(UMutex* mutex)
{
    if (mutex == NULL) {
        mutex = &globalMutex;
    }
    LeaveCriticalSection(&mutex->fCS);
}

#elif U_PLATFORM_IMPLEMENTS_POSIX

//-------------------------------------------------------------------------------------------
//
//  POSIX specific definitions
//
//-------------------------------------------------------------------------------------------

# include <pthread.h>

// Each UMutex consists of a pthread_mutex_t.
// All are statically initialized and ready for use.
// There is no runtime mutex initialization code needed.

U_CAPI void  U_EXPORT2
umtx_lock(UMutex *mutex) {
    if (mutex == NULL) {
        mutex = &globalMutex;
    }
    int sysErr = pthread_mutex_lock(&mutex->fMutex);
    (void)sysErr;   // Suppress unused variable warnings.
    U_ASSERT(sysErr == 0);
}


U_CAPI void  U_EXPORT2
umtx_unlock(UMutex* mutex)
{
    if (mutex == NULL) {
        mutex = &globalMutex;
    }
    int sysErr = pthread_mutex_unlock(&mutex->fMutex);
    (void)sysErr;   // Suppress unused variable warnings.
    U_ASSERT(sysErr == 0);
}

static pthread_mutex_t initMutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_cond_t initCondition = PTHREAD_COND_INITIALIZER;


// This function is called when a test of a UInitOnce::fState reveals that
//   initialization has not completed, that we either need to call the
//   function on this thread, or wait for some other thread to complete.
//
// The actual call to the init function is made inline by template code
//   that knows the C++ types involved. This function returns TRUE if
//   the caller needs to call the Init function.
//
UBool umtx_initImplPreInit(UInitOnce &uio) {
    pthread_mutex_lock(&initMutex);
    int32_t state = uio.fState;
    if (state == 0) {
        umtx_storeRelease(uio.fState, 1);
        pthread_mutex_unlock(&initMutex);
        return true;   // Caller will next call the init function.
    } else if (state == 2) {
        // Another thread already completed the initialization, in
        //   a race with this thread. We can simply return FALSE, indicating no
        //   further action is needed by the caller.
        pthread_mutex_unlock(&initMutex);
        return FALSE;
    } else {
        // Another thread is currently running the initialization.
        // Wait until it completes.
        U_ASSERT(state == 1);
        while (uio.fState == 1) {
            pthread_cond_wait(&initCondition, &initMutex);
        }
        UBool returnVal = uio.fState == 0;
        if (returnVal) {
            // Initialization that was running in another thread failed.
            // We will retry it in this thread.
            // (This is only used by SimpleSingleton)
            umtx_storeRelease(uio.fState, 1);
        }
        pthread_mutex_unlock(&initMutex);
        return returnVal;
    }
}

// This function is called by the thread that ran an initialization function,
// just after completing the function.
//   Some threads may be waiting on the condition, requiring the broadcast wakeup.
//   Some threads may be racing to test the fState variable outside of the mutex,
//   requiring the use of store/release when changing its value.
//
//   success: True:  the inialization succeeded. No further calls to the init
//                   function will be made.
//            False: the initializtion failed. The next call to umtx_initOnce()
//                   will retry the initialization.

void umtx_initImplPostInit(UInitOnce &uio, UBool success) {
    int32_t nextState = success? 2: 0;
    pthread_mutex_lock(&initMutex);
    umtx_storeRelease(uio.fState, nextState);
    pthread_cond_broadcast(&initCondition);
    pthread_mutex_unlock(&initMutex);
}


void umtx_initOnceReset(UInitOnce &uio) {
    // Not a thread safe function, we can use an ordinary assignment.
    uio.fState = 0;
}

// End of POSIX specific umutex implementation.

#else  // Platform #define chain.

#error Unknown Platform

#endif  // Platform #define chain.


//-------------------------------------------------------------------------------
//
//   Atomic Operations, out-of-line versions.
//                      These are conditional, only defined if better versions
//                      were not available for the platform.
//
//                      These versions are platform neutral.
//
//--------------------------------------------------------------------------------

#if defined U_NO_PLATFORM_ATOMICS
static UMutex   gIncDecMutex = U_MUTEX_INITIALIZER;

U_INTERNAL int32_t U_EXPORT2
umtx_atomic_inc(int32_t *p)  {
    int32_t retVal;
    umtx_lock(&gIncDecMutex);
    retVal = ++(*p);
    umtx_unlock(&gIncDecMutex);
    return retVal;
}


U_INTERNAL int32_t U_EXPORT2
umtx_atomic_dec(int32_t *p) {
    int32_t retVal;
    umtx_lock(&gIncDecMutex);
    retVal = --(*p);
    umtx_unlock(&gIncDecMutex);
    return retVal;
}

U_INTERNAL int32_t U_EXPORT2
umtx_loadAcquire(atomic_int32_t &var) {
    int32_t val = var;
    umtx_lock(&gIncDecMutex);
    umtx_unlock(&gIncDecMutex);
    return val;
}

U_INTERNAL void U_EXPORT2
umtx_storeRelease(atomic_int32_t &var, int32_t val) {
    umtx_lock(&gIncDecMutex);
    umtx_unlock(&gIncDecMutex);
    var = val;
}

#endif

//--------------------------------------------------------------------------
//
//  Deprecated functions for setting user mutexes.
//
//--------------------------------------------------------------------------

U_DEPRECATED void U_EXPORT2
u_setMutexFunctions(const void * /*context */, UMtxInitFn *, UMtxFn *,
                    UMtxFn *,  UMtxFn *, UErrorCode *status) {
    if (U_SUCCESS(*status)) {
        *status = U_UNSUPPORTED_ERROR;
    }
    return;
}



U_DEPRECATED void U_EXPORT2
u_setAtomicIncDecFunctions(const void * /*context */, UMtxAtomicFn *, UMtxAtomicFn *,
                           UErrorCode *status) {
    if (U_SUCCESS(*status)) {
        *status = U_UNSUPPORTED_ERROR;
    }
    return;
}
