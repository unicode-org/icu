/*
*******************************************************************************
*
*   Copyright (C) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
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
*******************************************************************************
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

/* APP_NO_THREADS is an old symbol. We'll honour it if present. */
#ifdef APP_NO_THREADS
# define ICU_USE_THREADS 0
#endif

/* Default: use threads. */
#ifndef ICU_USE_THREADS
# define ICU_USE_THREADS 1
#endif


#if defined(POSIX) && (ICU_USE_THREADS==1)
  /* Usage: uncomment the following, and breakpoint WeAreDeadlocked to
     find reentrant issues. */
/* # define POSIX_DEBUG_REENTRANCY 1 */
# include <pthread.h> /* must be first, so that we get the multithread versions of things. */

# ifdef POSIX_DEBUG_REENTRANCY
 pthread_t      gLastThread;
 bool_t         gInMutex;

 U_EXPORT void WeAreDeadlocked();

 void WeAreDeadlocked()
 {
    puts("ARGH!! We're deadlocked.. break on WeAreDeadlocked() next time.");
 }
# endif /* POSIX_DEBUG_REENTRANCY */
#endif /* POSIX && (ICU_USE_THREADS==1) */

#ifdef _WIN32
# include <WINDOWS.H>
#endif

#include "umutex.h"
#include "cmemory.h"

/* the global mutex. Use it proudly and wash it often. */
UMTX    gGlobalMutex = NULL;

void umtx_lock( UMTX *mutex )
{
#if (ICU_USE_THREADS == 1)
  if( mutex == NULL )
    {
      mutex = &gGlobalMutex;
    }

  if(*mutex == NULL)
    {
      umtx_init(mutex);
    }

#if defined(_WIN32)

    EnterCriticalSection((CRITICAL_SECTION*) *mutex);

#elif defined(POSIX)

#  ifdef POSIX_DEBUG_REENTRANCY
    if(gInMutex == TRUE) /* in the mutex -- possible deadlock*/
      if(pthread_equal(gLastThread,pthread_self()))
        WeAreDeadlocked();
#  endif

    pthread_mutex_lock((pthread_mutex_t*) *mutex);

#  ifdef POSIX_DEBUG_REENTRANCY
    gLastThread = pthread_self();
    gInMutex = TRUE;
#  endif
#endif
#endif /* ICU_USE_THREADS==1 */
}

void umtx_unlock( UMTX* mutex )
{
#if (ICU_USE_THREADS==1)
  if( mutex == NULL )
    {
      mutex = &gGlobalMutex;

      if(*mutex == NULL)
        {
          return; /* jitterbug 135, fix for multiprocessor machines */
        }
    }

#if defined(_WIN32)

    LeaveCriticalSection((CRITICAL_SECTION*)*mutex);

#elif defined(POSIX)
    pthread_mutex_unlock((pthread_mutex_t*)*mutex);
#ifdef POSIX_DEBUG_REENTRANCY
    gInMutex = FALSE;
#endif

#endif
#endif /* ICU_USE_THREADS == 1 */
}

U_CAPI void umtx_init( UMTX *mutex )
{
#if (ICU_USE_THREADS == 1)

if( mutex == NULL ) /* initialize the global mutex */
    {
      mutex = &gGlobalMutex;
    }

  if(*mutex != NULL) /* someone already did it. */
    return;

#if defined( _WIN32 )
  *mutex = uprv_malloc(sizeof(CRITICAL_SECTION));
  InitializeCriticalSection((CRITICAL_SECTION*)*mutex);

#elif defined( POSIX )

  *mutex = uprv_malloc(sizeof(pthread_mutex_t));

#if defined(HPUX_CMA)
    pthread_mutex_init((pthread_mutex_t*)*mutex, pthread_mutexattr_default);
#else
    pthread_mutex_init((pthread_mutex_t*)*mutex,NULL);
#endif


# ifdef POSIX_DEBUG_REENTRANCY
    gInMutex = FALSE;
# endif

#endif
#endif /* ICU_USE_THREADS==1 */
}







