---
layout: default
title: Custom ICU4C Synchronization
nav_order: 3
parent: Contributors
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Custom ICU4C Synchronization
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

> :warning: ***Support for including an alternate implementation of atomic and mutex
> operations has been withdrawn and removed from ICU4C.***
> See issue [ICU-20185](https://unicode-org.atlassian.net/browse/ICU-20185).

### Build Time User Provided Synchronization

Build time user synchronization provides a mechanism for platforms with special
requirements to provide their own mutex and one-time initialization
implementations to ICU. This facility was introduced in ICU 53. It may change
over time.

The alternative implementations are compiled directly into the ICU libraries.
Alternative implementations cannot be plugged in at run time.

The tables below show the items that must be defined by a custom ICU
synchronization implementation. The list includes both functions that are used
throughout ICU code and additional functions are for internal by other ICU
synchronization primitives.

**Low Level Atomics**, a set of platform or compiler dependent typedefs and
inlines. Provided in the internal header file
[`umutex.h`](https://github.com/unicode-org/icu/blob/main/icu4c/source/common/umutex.h).

| Type/Function                                           | Description                                                                   |
|---------------------------------------------------------|-------------------------------------------------------------------------------|
| `typedef u_atomic_int32_t`                              | A 32 bit integer that will work with low level atomic operations. (`typedef`) |
| `umtx_loadAcquire(u_atomic_int32_t &var)`               |                                                                               |
| `umtx_storeRelease(u_atomic_int32_t &var, int32_t val)` |                                                                               |
| `umtx_atomic_inc(u_atomic_int32_t &var)`                |                                                                               |
| `umtx_atomic_dec(u_atomic_int32_t &var)`                |                                                                               |

**Mutexes**. Type declarations for ICU mutex wrappers. Provided in a header file.

| Type                  | Description                                                                                       |
|-----------------------|---------------------------------------------------------------------------------------------------|
| `struct UMutex`       | An ICU mutex. All instances will be static. Typically just contains an underlying platform mutex. |
| `U_MUTEX_INITIALIZER` | A C style initializer for a static instance of a `UMutex`.                                          |

**Mutex and InitOnce implementations**. Out-of-line platform-specific code.
Provided in a .cpp file.

| Function                                | Description                                |
|-----------------------------------------|--------------------------------------------|
| `umtx_lock(UMutex *mutex)`              | Lock a mutex.                              |
| `umtx_unlock(UMutex* mutex)`            | Unlock a mutex.                            |
| `umtx_initImplPreInit(UInitOnce &uio)`  | `umtx_initOnce()` implementation function. |
| `umtx_initImplPostInit(UInitOnce &uio)` | `umtx_initOnce()` implementation function. |

`UInitOnce` and `umtx_initOnce()` are used internally by ICU for thread-safe
one-time initialization. Their implementation is split into a
platform-independent part (contained in
[`umutex.h`](https://github.com/unicode-org/icu/blob/main/icu4c/source/common/umutex.h)),
and the pair of platform-dependent implementation functions listed above.

**Build Setup**

Compiler preprocessor variables are used to name the custom files to be included
in the ICU build. If defined, the files are included at the top of the normal
platform `#ifdef` chains in the ICU sources, and effectively define a new
platform.

| Macro              | Description                                             |
|--------------------|---------------------------------------------------------|
| `U_USER_ATOMICS_H` | Set to the name of the low level atomics header file.   |
| `U_USER_MUTEX_H`   | Mutexes header file.                                    |
| `U_USER_MUTEX_CPP` | Mutexes and `InitOnce` implementation file.             |

It is possible (and reasonable) to supply only the two mutex files, while
retaining the ICU default implementation for the low level atomics.

Example ICU configure with user mutexes specified:

    CPPFLAGS='-DU_USER_ATOMICS_H=atomic_c11.h -DU_USER_MUTEX_H=mutex_c11.h -DU_USER_MUTEX_CPP=mutex_c11.cpp' ./runConfigureICU --enable-debug Linux

**Stability**

This interface may change between ICU releases. The required set of functions
may be extended, or details of the behavior required may be altered.

The types and functions defined by this interface reach deeply into the ICU
implementation, and we need to retain the ability to make changes should the
need arise.

**Examples**

The code below shows a complete set of ICU user synchronization files.

This implementation uses C++11 language mutexes and atomics. These make for a
convenient reference implementation because the C++11 constructs are well
defined and straight forward to use.

Similar implementations for POSIX and Windows can be found in files
`common/umutex.h` and `common/umutex.cpp`, in the platform `#ifdef` chains; these are
part of the standard ICU distribution.

**Mutex Header**
```c++
// Example of an ICU build time customized mutex header.
//
// Must define struct UMutex and an initializer that will work with static instances.
// All UMutex instances in ICU code will be static.

#ifndef ICU_MUTEX_C11_H
#define ICU_MUTEX_C11_H
#include <mutex>
#include <condition_variable>
struct UMutex {
    std::mutex fMutex;
};
#define U_MUTEX_INITIALIZER {}
#endif
```

**Atomics Header**
```c++
#include <atomic>

typedef std::atomic<int32_t> u_atomic_int32_t;
#define ATOMIC_INT32_T_INITIALIZER(val) ATOMIC_VAR_INIT(val)

inline int32_t umtx_loadAcquire(u_atomic_int32_t &var) {
    return var.load(std::memory_order_acquire);
}

inline void umtx_storeRelease(u_atomic_int32_t &var, int32_t val) {
    var.store(val, std::memory_order_release);
}

inline int32_t umtx_atomic_inc(u_atomic_int32_t &var) {
    return var.fetch_add(1) + 1;
}

inline int32_t umtx_atomic_dec(u_atomic_int32_t &var) {
    return var.fetch_sub(1) - 1;
}
```

**Mutex and InitOnce implementations**
```c++
//
// Example ICU build time custom mutex cpp file.
//
// Must implement these functions:
// umtx_lock(UMutex *mutex);
// umtx_unlock(UMutex *mutex);
// umtx_initImplPreInit(UInitOnce &uio);
// umtx_initImplPostInit(UInitOnce &uio);

U_CAPI void U_EXPORT2
umtx_lock(UMutex *mutex) {
    if (mutex == NULL) {
        // Note: globalMutex is pre-defined in the platform-independent ICU code.
        mutex = &globalMutex;
    }
    mutex->fMutex.lock();
}

U_CAPI void U_EXPORT2
umtx_unlock(UMutex* mutex) `{
    if (mutex == NULL) {
        mutex = &globalMutex;
    }
    mutex->fMutex.unlock();
}

// A mutex and a condition variable are used by the implementation of umtx_initOnce()
// The mutex is held only while the state of the InitOnce object is being changed or
// tested. It is not held while initialization functions are running.
// Threads needing to block, waiting for an initialization to complete, will wait
// on the condition variable.
// All InitOnce objects share a common mutex and condition variable. This means that
// all blocked threads will wake if any (possibly unrelated) initialization completes.
// Which does no harm, it should be statistically rare, and any spuriously woken
// threads will check their state and promptly wait again.

static std::mutex initMutex;
static std::condition_variable initCondition;

// This function is called from umtx_initOnce() when an initial test of a UInitOnce::fState flag
// reveals that initialization has not completed, that we either need to call the
// function on this thread, or wait for some other thread to complete the initialization.
//
// The actual call to the init function is made inline by template code
// that knows the C++ types involved. This function returns true if
// the inline code needs to invoke the Init function, or false if the initialization
// has completed on another thread.
//
// UInitOnce::fState values:
// 0: Initialization has not yet begun.
// 1: Initialization is in progress, not yet complete.
// 2: Initialization is complete.
//
UBool umtx_initImplPreInit(UInitOnce &uio) {
    std::unique_lock<std::mutex> initLock(initMutex);
    int32_t state = uio.fState;
    if (state == 0) {
        umtx_storeRelease(uio.fState, 1);
        return true; // Caller will next call the init function.
    } else {
        while (uio.fState == 1) {
            // Another thread is currently running the initialization.
            // Wait until it completes.
            initCondition.wait(initLock);
        }
        U_ASSERT(uio.fState == 2);
        return false;
    }
}

// This function is called from umtx_initOnce() just after an initializationfunction completes.
// Its purpose is to set the state of the UInitOnce object to initialized, and to
// unblock any threads that may be waiting on the initialization.
//
// Some threads may be waiting on the condition variable, requiring the notify_all().
// Some threads may be racing to test the fState flag outside of the mutex, 
// requiring the use of store-release when changing its value.

void umtx_initImplPostInit(UInitOnce &uio) {
    std::unique_lock<std::mutex> initLock(initMutex);
    umtx_storeRelease(uio.fState, 2);
    initCondition.notify_all();
}
```
