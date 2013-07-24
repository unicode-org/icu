/*
**********************************************************************
*   Copyright (C) 1997-2013, International Business Machines
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
#include "putilimp.h"



// Forward Declarations
struct UMutex;
struct UInitOnce;


/****************************************************************************
 *
 *   Low Level Atomic Operations. 
 *      Compiler dependent. Not operating system dependent.
 *
 ****************************************************************************/
#if U_HAVE_STD_ATOMICS

//  C++11 atomics are available.

#include <atomic>

typedef std::atomic<int32_t> atomic_int32_t;
#define ATOMIC_INT32_T_INITIALIZER(val) ATOMIC_VAR_INIT(val)

inline int32_t umtx_loadAcquire(atomic_int32_t &var) {
    return var.load(std::memory_order_acquire);
};

inline void umtx_storeRelease(atomic_int32_t &var, int32_t val) {
    var.store(val, std::memory_order_release);
};

inline int32_t umtx_atomic_inc(atomic_int32_t *var) {
    return var->fetch_add(1) + 1;
}
     
inline int32_t umtx_atomic_dec(atomic_int32_t *var) {
    return var->fetch_sub(1) - 1;
}
     

#elif U_PLATFORM_HAS_WIN32_API

// MSVC compiler. Reads and writes of volatile variables have
//                acquire and release memory semantics, respectively.
//                This is a Microsoft extension, not standard C++ behavior.
//
//   Update:      can't use this because of MinGW, built with gcc.
//                Original plan was to use gcc atomics for MinGW, but they
//                aren't supported, so we fold MinGW into this path.

# define WIN32_LEAN_AND_MEAN
# define VC_EXTRALEAN
# define NOUSER
# define NOSERVICE
# define NOIME
# define NOMCX
# ifndef NOMINMAX
# define NOMINMAX
# endif
# include <windows.h>

typedef volatile LONG atomic_int32_t;
#define ATOMIC_INT32_T_INITIALIZER(val) val

#ifdef __cplusplus
inline int32_t umtx_loadAcquire(atomic_int32_t &var) {
    return InterlockedCompareExchange(&var, 0, 0);
}

inline void umtx_storeRelease(atomic_int32_t &var, int32_t val) {
    InterlockedExchange(&var, val);
}


inline int32_t umtx_atomic_inc(atomic_int32_t *var) {
    return InterlockedIncrement(var);
}

inline int32_t umtx_atomic_dec(atomic_int32_t *var) {
    return InterlockedDecrement(var);
}
#endif /* __cplusplus */



#elif U_HAVE_GCC_ATOMICS
/*
 * gcc atomic ops. These are available on several other compilers as well.
 */
typedef int32_t atomic_int32_t;
#define ATOMIC_INT32_T_INITIALIZER(val) val

#ifdef __cplusplus
inline int32_t umtx_loadAcquire(atomic_int32_t &var) {
    int32_t val = var;
    __sync_synchronize();
    return val;
}

inline void umtx_storeRelease(atomic_int32_t &var, int32_t val) {
    __sync_synchronize();
    var = val;
}

inline int32_t umtx_atomic_inc(atomic_int32_t *p)  {
   return __sync_add_and_fetch(p, 1);
}

inline int32_t umtx_atomic_dec(atomic_int32_t *p)  {
   return __sync_sub_and_fetch(p, 1);
}

#endif /* __cplusplus */

#else

/*
 * Unknown Platform. Use out-of-line functions, which in turn use mutexes.
 *                   Slow but correct.
 */

#define U_NO_PLATFORM_ATOMICS

typedef int32_t atomic_int32_t;
#define ATOMIC_INT32_T_INITIALIZER(val) val

#ifdef __cplusplus
U_INTERNAL int32_t U_EXPORT2 umtx_loadAcquire(atomic_int32_t &var);

U_INTERNAL void U_EXPORT2 umtx_storeRelease(atomic_int32_t &var, int32_t val);
#endif /* __cplusplus */

U_INTERNAL int32_t U_EXPORT2 umtx_atomic_inc(atomic_int32_t *p);

U_INTERNAL int32_t U_EXPORT2 umtx_atomic_dec(atomic_int32_t *p);

#endif  /* Low Level Atomic Ops Platfrom Chain */



/*************************************************************************************************
 *
 *  UInitOnce Definitions.
 *     These are platform neutral.
 *
 *************************************************************************************************/

struct UInitOnce {
    atomic_int32_t   fState;
    UErrorCode       fErrCode;
#ifdef __cplusplus
    void reset() {fState = 0; fState=0;};
    UBool isReset() {return umtx_loadAcquire(fState) == 0;};
// Note: isReset() is used by service registration code.
//                 Thread safety of this usage needs review.
#endif
};
typedef struct UInitOnce UInitOnce;
#define U_INITONCE_INITIALIZER {ATOMIC_INT32_T_INITIALIZER(0), U_ZERO_ERROR}

#ifdef __cplusplus
// TODO: get all ICU files using umutex converted to C++,
//       then remove the __cpluplus conditionals from this file.

U_CAPI UBool U_EXPORT2 umtx_initImplPreInit(UInitOnce &);
U_CAPI void  U_EXPORT2 umtx_initImplPostInit(UInitOnce &, UBool success);

template<class T> void umtx_initOnce(UInitOnce &uio, T *obj, void (T::*fp)()) {
    if (umtx_loadAcquire(uio.fState) == 2) {
        return;
    }
    if (umtx_initImplPreInit(uio)) {
        (obj->*fp)();
        umtx_initImplPostInit(uio, TRUE);
    }
}


// umtx_initOnce variant for plain functions, or static class functions.
//               No context parameter.
inline void umtx_initOnce(UInitOnce &uio, void (*fp)()) {
    if (umtx_loadAcquire(uio.fState) == 2) {
        return;
    }
    if (umtx_initImplPreInit(uio)) {
        (*fp)();
        umtx_initImplPostInit(uio, TRUE);
    }
}

// umtx_initOnce variant for plain functions, or static class functions.
//               With ErrorCode, No context parameter.
inline void umtx_initOnce(UInitOnce &uio, void (*fp)(UErrorCode &), UErrorCode &errCode) {
    if (U_FAILURE(errCode)) {
        return;
    }    
    if (umtx_loadAcquire(uio.fState) != 2 && umtx_initImplPreInit(uio)) {
        // We run the initialization.
        (*fp)(errCode);
        uio.fErrCode = errCode;
        umtx_initImplPostInit(uio, TRUE);
    } else {
        // Someone else already ran the initialization.
        if (U_FAILURE(uio.fErrCode)) {
            errCode = uio.fErrCode;
        }
    }
}

// umtx_initOnce variant for plain functions, or static class functions,
//               with a context parameter.
template<class T> void umtx_initOnce(UInitOnce &uio, void (*fp)(T), T context) {
    if (umtx_loadAcquire(uio.fState) == 2) {
        return;
    }
    if (umtx_initImplPreInit(uio)) {
        (*fp)(context);
        umtx_initImplPostInit(uio, TRUE);
    }
}

// umtx_initOnce variant for plain functions, or static class functions,
//               with a context parameter and an error code.
template<class T> void umtx_initOnce(UInitOnce &uio, void (*fp)(T, UErrorCode &), T context, UErrorCode &errCode) {
    if (U_FAILURE(errCode)) {
        return;
    }    
    if (umtx_loadAcquire(uio.fState) != 2 && umtx_initImplPreInit(uio)) {
        // We run the initialization.
        (*fp)(context, errCode);
        uio.fErrCode = errCode;
        umtx_initImplPostInit(uio, TRUE);
    } else {
        // Someone else already ran the initialization.
        if (U_FAILURE(uio.fErrCode)) {
            errCode = uio.fErrCode;
        }
    }
}

#endif /*  __cplusplus */



/*************************************************************************************************
 *
 *  Mutex Definitions. Platform Dependent, #if platform chain follows.
 *         TODO:  Add a C++11 version.
 *                Need to convert all mutex using files to C++ first.
 *
 *************************************************************************************************/

#if U_PLATFORM_HAS_WIN32_API

/* Windows Definitions.
 *    Windows comes first in the platform chain.
 *    Cygwin (and possibly others) have both WIN32 and POSIX APIs. Prefer Win32 in this case.
 */


/* For CRITICAL_SECTION */

/*
 *   Note: there is an earlier include of windows.h in this file, but it is in 
 *         different conditionals.
 *         This one is needed if we are using C++11 for atomic ops, but
 *         win32 APIs for Critical Sections.
 */
 
# define WIN32_LEAN_AND_MEAN
# define VC_EXTRALEAN
# define NOUSER
# define NOSERVICE
# define NOIME
# define NOMCX
# ifndef NOMINMAX
# define NOMINMAX
# endif
# include <windows.h>


typedef struct UMutex {
    UInitOnce         fInitOnce;
    CRITICAL_SECTION  fCS;
} UMutex;

/* Initializer for a static UMUTEX. Deliberately contains no value for the
 *  CRITICAL_SECTION.
 */
#define U_MUTEX_INITIALIZER {U_INITONCE_INITIALIZER}



#elif U_PLATFORM_IMPLEMENTS_POSIX

/*
 *  POSIX platform
 */

#include <pthread.h>

struct UMutex {
    pthread_mutex_t  fMutex;
};
typedef struct UMutex UMutex;
#define U_MUTEX_INITIALIZER  {PTHREAD_MUTEX_INITIALIZER}


#else

/* 
 *  Unknow platform type. 
 *      This is an error condition. ICU requires mutexes.
 */

#error Unknown Platform.

#endif


    
/**************************************************************************************
 *
 *  Mutex Implementation function declaratations.
 *     Declarations are platform neutral.
 *     Implementations, in umutex.cpp, are platform specific.
 *
 ************************************************************************************/

/* Lock a mutex.
 * @param mutex The given mutex to be locked.  Pass NULL to specify
 *              the global ICU mutex.  Recursive locks are an error
 *              and may cause a deadlock on some platforms.
 */
U_INTERNAL void U_EXPORT2 umtx_lock(UMutex* mutex); 

/* Unlock a mutex.
 * @param mutex The given mutex to be unlocked.  Pass NULL to specify
 *              the global ICU mutex.
 */
U_INTERNAL void U_EXPORT2 umtx_unlock (UMutex* mutex);

#endif /* UMUTEX_H */
/*eof*/
