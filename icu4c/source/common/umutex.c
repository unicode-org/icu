/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright Taligent, Inc.,  1997                                       *
*   (C) Copyright International Business Machines Corporation,  1997-1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
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


#if defined(POSIX) && !defined(APP_NO_THREADS)
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
#endif /* POSIX && APP_NO_THREADS not defined */

#ifdef _WIN32
# include <WINDOWS.H>
#endif

#include "umutex.h"

/* the global mutex. Use it proudly and wash it often. */
UMTX    gGlobalMutex = NULL;

void umtx_lock( UMTX *mutex )
{
#ifndef APP_NO_THREADS
  if( mutex == NULL )
    {
      mutex = &gGlobalMutex;

      if(*mutex == NULL)
        {
          umtx_init(NULL);
        }
    }

#if defined(_WIN32)

    EnterCriticalSection((CRITICAL_SECTION*) *mutex);

#elif defined(POSIX)

#  ifdef POSIX_DEBUG_REENTRANCY
    if(gInMutex == TRUE) // in the mutex -- possible deadlock
      if(pthread_equal(gLastThread,pthread_self()))
        WeAreDeadlocked();
#  endif

    pthread_mutex_lock((pthread_mutex_t*) *mutex);

#  ifdef POSIX_DEBUG_REENTRANCY
    gLastThread = pthread_self();
    gInMutex = TRUE;
#  endif
#endif
#endif /* APP_NO_THREADS not defined */
}

void umtx_unlock( UMTX* mutex )
{
#ifndef APP_NO_THREADS
  if( mutex == NULL )
    {
      mutex = &gGlobalMutex;

      if(*mutex == NULL)
        {
          umtx_init(NULL);
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
#endif /* APP_NO_THREADS not defined */
}

CAPI void umtx_init( UMTX *mutex )
{
#ifndef APP_NO_THREADS

if( mutex == NULL ) /* initialize the global mutex */
    {
      mutex = &gGlobalMutex;
    }

  if(*mutex != NULL) /* someone already did it. */
    return;

#if defined( _WIN32 )
  *mutex = icu_malloc(sizeof(CRITICAL_SECTION));
  InitializeCriticalSection((CRITICAL_SECTION*)*mutex);

#elif defined( POSIX )

  *mutex = icu_malloc(sizeof(pthread_mutex_t));

#if defined(HPUX)
    pthread_mutex_init((pthread_mutex_t*)*mutex, pthread_mutexattr_default);
#else
    pthread_mutex_init((pthread_mutex_t*)*mutex,NULL);
#endif


# ifdef POSIX_DEBUG_REENTRANCY
    gInMutex = FALSE;
# endif

#endif
#endif /* APP_NO_THREADS not defined */
}







