/*
******************************************************************************
*
*   Copyright (C) 1997-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File CMUTEX.C
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

/* Assume POSIX, and modify as necessary below */
#define POSIX

#if defined(_WIN32)
#undef POSIX
#endif
#if defined(macintosh)
#undef POSIX
#endif
#if defined(OS2)
#undef POSIX
#endif


/* Check our settings... */
#include "unicode/utypes.h"
#include "uassert.h"
#include "ucln_cmn.h"


#if defined(POSIX) && (ICU_USE_THREADS==1)
# include <pthread.h> /* must be first, so that we get the multithread versions of things. */

#endif /* POSIX && (ICU_USE_THREADS==1) */

#ifdef WIN32
# define WIN32_LEAN_AND_MEAN
# define NOGDI
# define NOUSER
# define NOSERVICE
# define NOIME
# define NOMCX
# include <windows.h>
#endif

#include "umutex.h"
#include "cmemory.h"


/* The global ICU mutex.    
 * Special cased to allow thread safe lazy initialization (windows)
 *   or pure static C style initialization (Posix)
 */
#if defined( WIN32 )
static UMTX              gGlobalMutex      = NULL;

#elif defined( POSIX )
static pthread_mutex_t   gGlobalPosixMutex = PTHREAD_MUTEX_INITIALIZER;
static UMTX              gGlobalMutex      = &gGlobalPosixMutex;

static UMTX              gIncDecMutex      = NULL;
#endif /* cascade of platforms */


/* Detect Recursive locking of the global mutex.  For debugging only. */
static int32_t gRecursionCount = 0;       


/*
 *  User mutex implementation functions.  If non-null, call back to these rather than
 *  directly using the system (Posix or Windows) APIs.
 *    (declarations are in uclean.h)
 */
static UMtxInitFn    *pMutexInitFn    = NULL;
static UMtxFn        *pMutexDestroyFn = NULL;
static UMtxFn        *pMutexLockFn    = NULL;
static UMtxFn        *pMutexUnlockFn  = NULL;
static const void    *gMutexContext   = NULL;



/*
 *   umtx_lock
 */
U_CAPI void  U_EXPORT2
umtx_lock(UMTX *mutex)
{
    if (mutex == NULL) {
        mutex = &gGlobalMutex;
    }

    if (*mutex == NULL) {
        /* Lock of an uninitialized mutex.  Initialize it before proceeding.   */
        umtx_init(mutex);    
    }

    if (pMutexLockFn != NULL) {
        (*pMutexLockFn)(gMutexContext, mutex);
    } else {

#if (ICU_USE_THREADS == 1)
#if defined(WIN32)
        EnterCriticalSection((CRITICAL_SECTION*) *mutex);
#elif defined(POSIX)
        pthread_mutex_lock((pthread_mutex_t*) *mutex);
#endif   /* cascade of platforms */
#endif /* ICU_USE_THREADS==1 */
    }

#if defined(WIN32) && defined(_DEBUG)
        if (mutex == &gGlobalMutex) {         /* Detect Reentrant locking of the global mutex.      */
            gRecursionCount++;                /* Recursion causes deadlocks on Unixes.              */
            U_ASSERT(gRecursionCount == 1);   /* Detection works on Windows.  Debug problems there. */
        }
#endif /*_DEBUG*/
}



/*
 * umtx_unlock
 */
U_CAPI void  U_EXPORT2
umtx_unlock(UMTX* mutex)
{
    if(mutex == NULL) {
        mutex = &gGlobalMutex;
    }

    if(*mutex == NULL)    {
        U_ASSERT(FALSE);  /* This mutex is not initialized.     */
        return; 
    }

#if defined (WIN32) && defined (_DEBUG) 
    if (mutex == &gGlobalMutex) {
        gRecursionCount--;
        U_ASSERT(gRecursionCount == 0);  /* Detect unlock of an already unlocked mutex */
    }
#endif

    if (pMutexUnlockFn) {
        (*pMutexUnlockFn)(gMutexContext, mutex);
    } else {
#if (ICU_USE_THREADS==1)
#if defined (WIN32)
        LeaveCriticalSection((CRITICAL_SECTION*)*mutex);
#elif defined (POSIX)
        pthread_mutex_unlock((pthread_mutex_t*)*mutex);
#endif  /* cascade of platforms */
#endif /* ICU_USE_THREADS == 1 */
    }
}



/*
 *   umtx_raw_init    Do the platform specific mutex allocation and initialization
 *                    for all ICU mutexes _except_ the ICU global mutex.
 */
static void umtx_raw_init(UMTX *mutex) {
    if (pMutexInitFn != NULL) {
        UErrorCode status = U_ZERO_ERROR;
        (*pMutexInitFn)(gMutexContext, mutex, &status);
        if (U_FAILURE(status)) {
            /* TODO:  how should errors here be handled? */
            return;
        }
    } else {

#if (ICU_USE_THREADS == 1)
    #if defined (WIN32)
        CRITICAL_SECTION *cs = uprv_malloc(sizeof(CRITICAL_SECTION));
        if (cs == NULL) {
            return;
        }
        InitializeCriticalSection(cs);
        *mutex = cs;
    #elif defined( POSIX )
        pthread_mutex_t *m = (pthread_mutex_t *)uprv_malloc(sizeof(pthread_mutex_t));
        if (m == NULL) {
            return;
        }
        # if defined (HPUX_CMA)
            pthread_mutex_init(m, pthread_mutexattr_default);
        # else
            pthread_mutex_init(m, NULL);
        # endif
        *mutex = m;
    #endif /* cascade of platforms */
#else  /* ICU_USE_THREADS */
        *mutex = mutex;      /* With no threads, we must still set the mutex to
                              * some non-null value to make the rest of the
                              *   (not ifdefed) mutex code think that it is initialized.
                              */
#endif /* ICU_USE_THREADS */
    }
}



/*
 *   initGlobalMutex    Do the platform specific initialization of the ICU global mutex.
 *                      Separated out from the other mutexes because it is different:
 *                      Mutex storage is static for POSIX, init must be thread safe 
 *                      without the use of another mutex.
 */
static void initGlobalMutex() {
    /*
     * Call user mutex init function if one has been specified and the global mutex
     *  is not already initialized.  For POSIX, the default C style initialization
     *  will is used, so we need to consider the global mutex to not yet be
     *  initialized if it has just its C initial value.
     */
    if (pMutexInitFn != NULL) {
        if (gGlobalMutex==NULL 
#if defined(POSIX)
                                || gGlobalMutex==&gGlobalPosixMutex 
#endif
       ) {
            UErrorCode status = U_ZERO_ERROR;
            (*pMutexInitFn)(gMutexContext, &gGlobalMutex, &status);
            if (U_FAILURE(status)) {
                /* TODO:  how should errors here be handled? */
                return;
            }
        }
        return;
    }

    /* No user override of mutex functions.
     *   Use default ICU mutex implementations.
     */
#if (ICU_USE_THREADS == 1)
    #if defined (WIN32)
    {
        void              *t;
        CRITICAL_SECTION  *ourCritSec = uprv_malloc(sizeof(CRITICAL_SECTION)); 
        InitializeCriticalSection(ourCritSec);
        t = InterlockedCompareExchangePointer(&gGlobalMutex, ourCritSec, NULL);
        if (t != NULL) {
            /* Some other thread stored into gGlobalMutex first.  Discard the critical
             *  section we just created; the system will go with the other one.
             */
            DeleteCriticalSection(ourCritSec);
            uprv_free(ourCritSec);
        }
    }
    #elif defined( POSIX )
        /*  No Action Required.  Global mutex set up with C static initialization. */
        U_ASSERT(gGlobalMutex = &gGlobalPosixMutex);
    #endif /* cascade of platforms */
#else  /* ICU_USE_THREADS */
        gGlobalMutex = &gGlobalMutex  /* With no threads, we must still set the mutex to
                                        * some non-null value to make the rest of the
                                        *   (not ifdefed) mutex code think that it is initialized.
                                        */
#endif /* ICU_USE_THREADS */
}





U_CAPI void  U_EXPORT2
umtx_init(UMTX *mutex)
{
    if (mutex == NULL || mutex == &gGlobalMutex) {
        initGlobalMutex();
    } else {
        /* 
         *  Thread safe initialization of mutexes other than the global one,
         *  using the global mutex.
         */  
        UBool isInitialized; 
        UMTX tMutex = NULL;

        umtx_lock(NULL);
        isInitialized = (*mutex != NULL);
        umtx_unlock(NULL);
        if (isInitialized) {  
            return;
        }

        umtx_raw_init(&tMutex);

        umtx_lock(NULL);
        if (*mutex == NULL) {
            *mutex = tMutex;
            tMutex = NULL;
        }
        umtx_unlock(NULL);
        
        if (tMutex != NULL) {
            umtx_destroy(&tMutex); 
        }
    }
}


/*
 *  umtx_destroy.    Un-initialize a mutex, releasing any underlying resources
 *                   that it may be holding.  Destroying an already destroyed
 *                   mutex has no effect.  Unlike umtx_init(), this function
 *                   is not thread safe;  two threads must not concurrently try to
 *                   destroy the same mutex.
 */                  
U_CAPI void  U_EXPORT2
umtx_destroy(UMTX *mutex) {
    if (mutex == NULL) {  /* destroy the global mutex */
        mutex = &gGlobalMutex;
    }
    
    if (*mutex == NULL) {  /* someone already did it. */
        return;
    }

#if defined (POSIX)
    /*  The life of the inc/dec mutex for POSIX is tied to that of the global mutex. */
    if (mutex == &gGlobalMutex) {
        umtx_destroy(&gIncDecMutex);
    }
#endif

    if (pMutexDestroyFn != NULL) {
        (*pMutexDestroyFn)(gMutexContext, mutex);
        *mutex = NULL;
    } else {
#if (ICU_USE_THREADS == 1)
#if defined (WIN32)
        DeleteCriticalSection((CRITICAL_SECTION*)*mutex);
        uprv_free(*mutex);
        *mutex = NULL;
#elif defined (POSIX)
        if (*mutex != &gGlobalPosixMutex) {
            /* Only POSIX mutexes other than the ICU global mutex get destroyed. */
            pthread_mutex_destroy((pthread_mutex_t*)*mutex);
            uprv_free(*mutex);
            *mutex = NULL;
        } 
#endif /* chain of platforms */
#else  /* ICU_USE_THREADS==1 */
           /* NO ICU Threads.  We still need to zero out the mutex pointer, so that
            * it appears to be uninitialized     */
        *mutex = NULL;
#endif   /* ICU_USE_THREADS */
        
    }

}



U_CAPI void U_EXPORT2 
u_setMutexFunctions(const void *context, UMtxInitFn *i, UMtxFn *d, UMtxFn *l, UMtxFn *u,
                    UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return;
    }

    /* Can not set a mutex function to a NULL value  */
    if (i==NULL || d==NULL || l==NULL || u==NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }

    /* If ICU is not in an initial state, disallow this operation. */
    if (cmemory_inUse()) {
        *status = U_INVALID_STATE_ERROR;
        return;
    }

    /* Swap in the mutex function pointers.  */
    pMutexInitFn    = i;
    pMutexDestroyFn = d;
    pMutexLockFn    = l;
    pMutexUnlockFn  = u;
    gMutexContext   = context;
    gGlobalMutex    = NULL;         /* For POSIX, the global mutex will be pre-initialized */
                                    /*   Undo that, force re-initialization when u_init()  */
                                    /*   happens.                                          */
}



/*-----------------------------------------------------------------
 *
 *  Atomic Increment and Decrement
 *     umtx_atomic_inc
 *     umtx_atomic_dec
 *
 *----------------------------------------------------------------*/

/* Pointers to user-supplied inc/dec functions.  Null if not funcs have been set.  */
static UMtxAtomicFn  *pIncFn = NULL;
static UMtxAtomicFn  *pDecFn = NULL;
static void *gIncDecContext;


U_CAPI int32_t U_EXPORT2
umtx_atomic_inc(int32_t *p)  {
    int32_t retVal;
    if (pIncFn) {
        retVal = (*pIncFn)(gIncDecContext, p);
    } else {
        #if defined (WIN32) && ICU_USE_THREADS == 1
            retVal = InterlockedIncrement(p);
        #elif defined (POSIX) && ICU_USE_THREADS == 1
            umtx_lock(&gIncDecMutex);
            retVal = ++(*p);
            umtx_unlock(&gIncDecMutex);
        #else
            /* Unknown Platform, or ICU thread support compiled out. */
            retVal = ++(*p);
        #endif
    }
    return retVal;
}

U_CAPI int32_t U_EXPORT2
umtx_atomic_dec(int32_t *p) {
    int32_t retVal;
    if (pDecFn) {
        retVal = (*pDecFn)(gIncDecContext, p);
    } else {
        #if defined (WIN32) && ICU_USE_THREADS == 1
            retVal = InterlockedDecrement(p);
        #elif defined (POSIX) && ICU_USE_THREADS == 1
            umtx_lock(&gIncDecMutex);
            retVal = --(*p);
            umtx_unlock(&gIncDecMutex);
        #else
            /* Unknown Platform, or ICU thread support compiled out. */
            retVal = --(*p);
        #endif
    }
    return retVal;
}

/* TODO:  Some POSIXy platforms have atomic inc/dec functions available.  Use them. */





U_CAPI void U_EXPORT2
u_setAtomicIncDecFunctions(const void *context, UMtxAtomicFn *ip, UMtxAtomicFn *dp,
                                UErrorCode *status) {
    int32_t   testInt;
    if (U_FAILURE(*status)) {
        return;
    }
    /* Can not set a mutex function to a NULL value  */
    if (ip==NULL || dp==NULL) {
        *status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    /* If ICU is not in an initial state, disallow this operation. */
    if (cmemory_inUse()) {
        *status = U_INVALID_STATE_ERROR;
        return;
    }

    pIncFn = ip;
    pDecFn = dp;

    testInt = 0;
    U_ASSERT(umtx_atomic_inc(&testInt) == 1);     /* Sanity Check.    Do the functions work at all? */
    U_ASSERT(testInt == 1);
    U_ASSERT(umtx_atomic_dec(&testInt) == 0);
    U_ASSERT(testInt == 0);
}



/*
 *  Mutex Cleanup Function
 *
 *      Destroy the global mutex(es), and reset the mutex function callback pointers.
 */
U_CFUNC UBool umtx_cleanup(void) {
    umtx_destroy(NULL);
    pMutexInitFn    = NULL;
    pMutexDestroyFn = NULL;
    pMutexLockFn    = NULL;
    pMutexUnlockFn  = NULL;
    gMutexContext   = NULL;

    gGlobalMutex    = NULL;
#if defined (POSIX)
    gIncDecMutex    = NULL;
    gGlobalMutex    = &gGlobalPosixMutex;
#endif

    pIncFn         = NULL;
    pDecFn         = NULL;

    return TRUE;
}


