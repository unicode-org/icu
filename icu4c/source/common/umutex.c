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

/* the global mutexes.  */
static UMTX    gGlobalMutex = NULL;     /* The global ICU mutex                        */
static UMTX    gIncDecMutex = NULL;     /* mutex for atomic inc/dec, for platforms     */
                                        /*   that can't do those ops directly.         */

/* Detect Recursive locking of the global mutex.  For debugging only. */
static int32_t gRecursionCount = 0;       


/*
 *  User mutex implementatin functions.  If non-null, call back to these rather than
 *  directly using the system (Posix or Windows) APIs.
 *    (declarations are in uclean.h)
 */
static UMtxInit      *pMutexInit    = NULL;
static UMtxDestroy   *pMutexDestroy = NULL;
static UMtxLock      *pMutexLock    = NULL;
static UMtxUnlock    *pMutexUnlock  = NULL;
static const void    *gMutexContext = NULL;



/*
 * umtx_lock
 */
U_CAPI void  U_EXPORT2
umtx_lock(UMTX *mutex)
{
    if (mutex == NULL)
    {
        mutex = &gGlobalMutex;
    }

    if (*mutex == NULL)
    {
        /* Attempt to lock an uninitialized mutex.  Not Supported.
         *  Note that earlier versions of ICU supported lazy mutex initialization.
         *    That is not thread safe on CPUs that reorder memory operations.  */
        U_ASSERT(FALSE); 
        umtx_init(mutex);    /*  But, in case someone really screwed up, we will
                              *   still do the lazy init to try to avoid a crash  */
    }

    if (pMutexLock != NULL) {
        (*pMutexLock)(gMutexContext, mutex);
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
    if(mutex == NULL)
    {
        mutex = &gGlobalMutex;
    }

    if(*mutex == NULL)
    {
        U_ASSERT(FALSE);  /* This mutex is not initialized.     */
        return; 
    }

#if defined (WIN32) && defined (_DEBUG) 
    if (mutex == &gGlobalMutex) {
        gRecursionCount--;
        U_ASSERT(gRecursionCount == 0);  /* Detect unlock of an already unlocked mutex */
    }
#endif

    if (pMutexUnlock) {
        (*pMutexUnlock)(gMutexContext, mutex);
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
 */
static void umtx_raw_init(UMTX *mutex) {
    if (pMutexInit != NULL) {
        UErrorCode status = U_ZERO_ERROR;
        (*pMutexInit)(gMutexContext, mutex, &status);
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
        pthread_mutex_t *m = uprv_malloc(sizeof(pthread_mutex_t));
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



U_CAPI void  U_EXPORT2
umtx_init(UMTX *mutex)
{
    if (mutex == NULL) {
        mutex = &gGlobalMutex;
    }

    if (mutex == &gGlobalMutex) {
        /* Initialization of the global mutex. */
        if (*mutex != NULL) {
            /* Global mutex is already initialized.  Nothing more required   */
            return;
        }
        umtx_raw_init(mutex);
        gRecursionCount = 0;

        /* Initialize the inc/dec mutex, if needed, at the same time as the global ICU mutex */
       #ifdef POSIX
       umtx_raw_init(&gIncDecMutex);
       #endif

    } else {
        /* The mutex to initialize is not the global mutex.
         *  Thread safe initialization, using the global mutex.
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
    if (mutex == NULL) /* destroy the global mutex */
    {
        mutex = &gGlobalMutex;
    }
    
    if (*mutex == NULL) /* someone already did it. */
        return;
    
    if (pMutexDestroy != NULL) {
        (*pMutexDestroy)(gMutexContext, mutex);
    } else {
#if (ICU_USE_THREADS == 1)
#if defined (WIN32)
        DeleteCriticalSection((CRITICAL_SECTION*)*mutex);
        
#elif defined (POSIX)
        pthread_mutex_destroy((pthread_mutex_t*)*mutex);
        
#endif
#endif /* ICU_USE_THREADS==1 */
        
        uprv_free(*mutex);
    }
    *mutex = NULL;

#if defined (POSIX)
    if (mutex == &gGlobalMutex) {
        umtx_destroy(&gIncDecMutex);
    }
#endif /* POSIX */
}



U_CAPI void U_EXPORT2 
u_setMutexFunctions(const void *context, UMtxInit *i, UMtxDestroy *d, UMtxLock *l, UMtxUnlock *u,
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

    /* Destroy the mutexes that C++ static initialization creates */
    u_ICUStaticUnInitFunc();

    /* Swap in the mutex function pointers.  */
    pMutexInit    = i;
    pMutexDestroy = d;
    pMutexLock    = l;
    pMutexUnlock  = u;
    gMutexContext = context;

    /*
     * Re-do the equivalent of ICU's static initialization,
     *   which will recreate the default, resource bundle and converter mutexes.
     *   TODO:  remove this when remove all ICU static init.
     */
    u_ICUStaticInitFunc();
}


/*
 *  Mutex Cleanup Function
 *
 *      Destroy the global mutex, and reset the mutex function callback pointers.
 */
U_CFUNC UBool umtx_cleanup(void) {
    umtx_destroy(NULL);
    pMutexInit    = NULL;
    pMutexDestroy = NULL;
    pMutexLock    = NULL;
    pMutexUnlock  = NULL;
    gMutexContext = NULL;
    return TRUE;
}



/*-----------------------------------------------------------------
 *
 *  umtx_atomic_inc
 *  umtx_atomic_dec
 *
 *----------------------------------------------------------------*/
#if (ICU_USE_THREADS == 1) 

#if defined (WIN32)
/*
 * Win32 - use the Windows API functions for atomic increment and decrement.
 */
U_CAPI int32_t U_EXPORT2
umtx_atomic_inc(int32_t *p)
{
    return InterlockedIncrement(p);
}

U_CAPI int32_t U_EXPORT2
umtx_atomic_dec(int32_t *p)
{
    return InterlockedDecrement(p); 
}

#elif defined (POSIX)
/*
 * POSIX platforms without specific atomic operations.  Use a posix mutex
 *   to protect the increment and decrement.
 */

U_CAPI int32_t U_EXPORT2
umtx_atomic_inc(int32_t *p)
{
    int32_t    retVal;

    pthread_mutex_t *m = (pthread_mutex_t*) gIncDecMutex;
    pthread_mutex_lock(m);
    retVal = ++(*p);
    pthread_mutex_unlock(m);
    return retVal;
}


U_CAPI int32_t U_EXPORT2
umtx_atomic_dec(int32_t *p)
{
    int32_t    retVal;

    pthread_mutex_t *m = (pthread_mutex_t*) gIncDecMutex;
    pthread_mutex_lock(m);
    retVal = --(*p);
    pthread_mutex_unlock(m);
    return retVal;
}


/* TODO:  Some POSIXy platforms have atomic inc/dec functions available.  Use them. */
#else 
   
/* No recognized platform.  */
#error  No atomic increment and decrement defined for this platform. \
        Either use the --disable-threads configure option, or define those functions in this file.

#endif   /* Platform selection for atomic_inc and dec. */


#else  /* (ICU_USE_THREADS == 0) */

/* Threads disabled here */

U_CAPI int32_t U_EXPORT2
umtx_atomic_inc(int32_t *p) {
    return ++(*p);
}

U_CAPI int32_t U_EXPORT2
umtx_atomic_dec(int32_t *p) {
    return --(*p);
}

#endif /* (ICU_USE_THREADS == 1) */




