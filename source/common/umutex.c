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
  /* Usage: uncomment the following, and breakpoint WeAreDeadlocked to
     find reentrant issues. */
/* # define POSIX_DEBUG_REENTRANCY 1 */
# include <pthread.h> /* must be first, so that we get the multithread versions of things. */

# ifdef POSIX_DEBUG_REENTRANCY
 pthread_t      gLastThread;
 UBool         gInMutex;

 U_EXPORT void WeAreDeadlocked();

 void WeAreDeadlocked()
 {
    puts("ARGH!! We're deadlocked.. break on WeAreDeadlocked() next time.");
 }
# endif /* POSIX_DEBUG_REENTRANCY */
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

#if (ICU_USE_THREADS == 1)

/* the global mutex.  */
static UMTX    gGlobalMutex = NULL;

/* Detect Recursive entries.  For debugging only. */
# ifdef _DEBUG
static int32_t gRecursionCount = 0;       
# endif

/* Declare the predefined mutexes.  */
#if defined(WIN32)
static CRITICAL_SECTION gPlatformMutex;

#elif defined(POSIX)
static pthread_mutex_t gPlatformMutex;    /* The global ICU mutex   */
static pthread_mutex_t gIncDecMutex;      /* For use by atomic inc/dec, on Unixes only */    

#endif
#endif /* ICU_USE_THREADS==1 */


/*
 *  User mutex implementatin functions.  If non-null, call back to these rather than
 *  directly using the system (Posix or Windows) APIs.
 *    (declarations are in uclean.h)
 */
static UMtxInit      *pMutexInit    = NULL;
static UMtxDestroy   *pMutexDestroy = NULL;
static UMtxLock      *pMutexLock    = NULL;
static UMtxUnlock    *pMutexUnlock  = NULL;
static void          *gMutexContext = NULL;

/*
 * umtx_isInitialized
 */
U_CAPI UBool U_EXPORT2
umtx_isInitialized(UMTX *mutex)
{
#if (ICU_USE_THREADS == 1)
    if (mutex == NULL)
    {
        return (UBool)(gGlobalMutex != NULL);
    } else {
        UBool isInited;
        umtx_lock(NULL);
        isInited = (*mutex != NULL);
        umtx_unlock(NULL);
        return isInited;
    }
#else
    return TRUE;    /* Since we don't use threads, it's considered initialized. */
#endif /* ICU_USE_THREADS==1 */
}



/*
 * umtx_lock
 */
U_CAPI void  U_EXPORT2
umtx_lock(UMTX *mutex)
{
#if (ICU_USE_THREADS == 1)
    if (mutex == NULL)
    {
        mutex = &gGlobalMutex;
    }

    if (*mutex == NULL)
    {
        /* Lazy init of a non-global mutexes on first lock is NOT safe on processors
         *  that reorder memory operations.  */
        /* U_ASSERT(FALSE);    TODO:  Turn this back on */
        if (mutex != &gGlobalMutex) {
            umtx_init(mutex);
        } else {
            umtx_init(NULL);  /* initialize the global mutex - only get 
                                 here if C++ static init is NOT working,
                                 and u_init() hasn't been called.
                                 
                                 Not thread-safe if this call is contended! */
        }
    }

    if (pMutexLock != NULL) {
        (*pMutexLock)(gMutexContext, mutex);
    } else {

#if defined(WIN32)
        EnterCriticalSection((CRITICAL_SECTION*) *mutex);
#ifdef _DEBUG
        if (mutex == &gGlobalMutex) {
            gRecursionCount++;
            U_ASSERT(gRecursionCount == 1);
        }
#endif /*_DEBUG*/
  
#elif defined(POSIX)
#  ifdef POSIX_DEBUG_REENTRANCY
        if (gInMutex == TRUE && mutex == &gGlobalMutex) /* in the mutex -- possible deadlock*/
            if(pthread_equal(gLastThread, pthread_self()))
                WeAreDeadlocked();
#  endif
            pthread_mutex_lock((pthread_mutex_t*) *mutex);
            
#  ifdef POSIX_DEBUG_REENTRANCY
            if (mutex == &gGlobalMutex) {
                gLastThread = pthread_self();
                gInMutex = TRUE;
            }
#  endif /* POSIX_DEBUG_REENTRANCY */
#endif   /* cascade of platforms */
    }
#endif /* ICU_USE_THREADS==1 */
}



/*
 * umtx_unlock
 */
U_CAPI void  U_EXPORT2
umtx_unlock(UMTX* mutex)
{
#if (ICU_USE_THREADS==1)
    if(mutex == NULL)
    {
        mutex = &gGlobalMutex;
    }

    if(*mutex == NULL)
    {
        return; /* jitterbug 135, fix for multiprocessor machines */
    }

    if (pMutexUnlock) {
        (*pMutexUnlock)(gMutexContext, mutex);
    } else {
#if defined (WIN32)
    #ifdef _DEBUG
        if (mutex == &gGlobalMutex) {
        gRecursionCount--;
        U_ASSERT(gRecursionCount == 0);
    }
    #endif /*_DEBUG*/
    LeaveCriticalSection((CRITICAL_SECTION*)*mutex);

#elif defined (POSIX)
    pthread_mutex_unlock((pthread_mutex_t*)*mutex);

#ifdef POSIX_DEBUG_REENTRANCY
    if (mutex == &gGlobalMutex) {
        gInMutex = FALSE;
    }
#endif

#endif  /* cascade of platforms */
    }
#endif /* ICU_USE_THREADS == 1 */
}



/*
 *   umtx_raw_init    Do the platform specific mutex allocation and initialization
 */
#if (ICU_USE_THREADS == 1)
static UMTX umtx_raw_init(void  *mem) {
    if (pMutexInit != NULL) {
        UErrorCode status = U_ZERO_ERROR;
        if (mem == NULL) {
            mem = uprv_malloc(sizeof(UMTX));
            if (mem == NULL) {return NULL;}
        }
        (*pMutexInit)(gMutexContext, (UMTX *)mem, &status);
        if (U_FAILURE(status)) {
            /* TODO:  how should errors here be handled? */
            uprv_free(mem);
            return NULL;
        }
    } else {

    #if defined (WIN32)
        if (mem == NULL) {
            mem = uprv_malloc(sizeof(CRITICAL_SECTION));
            if (mem == NULL) {return NULL;}
        }
        InitializeCriticalSection((CRITICAL_SECTION*)mem);
    #elif defined( POSIX )
        if (mem == NULL) {
            /* TODO:  think about eliminating this malloc.  */
            mem = uprv_malloc(sizeof(pthread_mutex_t));
            if (mem == NULL) {return NULL;}
        }
        # if defined (HPUX_CMA)
            pthread_mutex_init((pthread_mutex_t*)mem, pthread_mutexattr_default);
        # else
            pthread_mutex_init((pthread_mutex_t*)mem, NULL);
        # endif
    #endif /* cascade of platforms */
    }

    return (UMTX *)mem;
}
#endif  /* ICU_USE_THREADS */


U_CAPI void  U_EXPORT2
umtx_init(UMTX *mutex)
{
#if (ICU_USE_THREADS == 1)

    if (mutex == NULL) /* initialize the global mutex */
    {
        /* Note:  The initialization of the global mutex is NOT thread safe.   */
        if (gGlobalMutex != NULL) {
            return;
        }
        gGlobalMutex = umtx_raw_init(&gPlatformMutex);

       # ifdef POSIX_DEBUG_REENTRANCY
           gInMutex = FALSE;
       # endif
       #ifdef _DEBUG
           gRecursionCount = 0;
       #endif

       #ifdef POSIX
       umtx_raw_init(&gIncDecMutex);
       #endif

    } else {
        /* Not the global mutex.
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

        tMutex = umtx_raw_init(NULL);

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
#endif /* ICU_USE_THREADS==1 */
}

U_CAPI void  U_EXPORT2
umtx_destroy(UMTX *mutex) {
#if (ICU_USE_THREADS == 1)
    if (mutex == NULL) /* destroy the global mutex */
    {
        mutex = &gGlobalMutex;
    }

    if (*mutex == NULL) /* someone already did it. */
        return;

#if defined (WIN32)
    DeleteCriticalSection((CRITICAL_SECTION*)*mutex);

#elif defined (POSIX)
    pthread_mutex_destroy((pthread_mutex_t*)*mutex);

#endif

    if (*mutex != gGlobalMutex)
    {
        uprv_free(*mutex);
    }

    *mutex = NULL;
#endif /* ICU_USE_THREADS==1 */
}



U_CAPI void U_EXPORT2 
u_setMutexFunctions(void *context, UMtxInit *i, UMtxDestroy *d, UMtxLock *l, UMtxUnlock *u,
                    UErrorCode *status) {
    if (U_FAILURE(*status)) {
        return;
    }
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
     *   which will recreate the default, resource bundle and converter mutexes
     */
    u_ICUStaticInitFunc();
}



/*
 *  umtx_atomic_inc
 *  umtx_atomic_dec
 */
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
 *   The IncDecMutex is in static storage so we don't have to come back and delete it
 *   when the process exits.
 */

U_CAPI int32_t U_EXPORT2
umtx_atomic_inc(int32_t *p)
{
    int32_t    retVal;

    pthread_mutex_lock(&gIncDecMutex);
    retVal = ++(*p);
    pthread_mutex_unlock(&gIncDecMutex);
    return retVal;
}


U_CAPI int32_t U_EXPORT2
umtx_atomic_dec(int32_t *p)
{
    int32_t    retVal;

    pthread_mutex_lock(&gIncDecMutex);
    retVal = --(*p);
    pthread_mutex_unlock(&gIncDecMutex);
    return retVal;
}


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




