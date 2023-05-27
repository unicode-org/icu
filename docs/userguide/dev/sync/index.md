---
layout: default
title: Synchronization
parent: Contributors
---
<!--
© 2020 and later: Unicode, Inc. and others.
License & terms of use: http://www.unicode.org/copyright.html
-->

# Synchronization Issues
{: .no_toc }

## Contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

## Overview

ICU is designed for use in multi-threaded environments. Guidelines for
developers using ICU are in the [ICU Design](../../design.md) section of the
user guide.

Within the ICU implementation, access to shared or global data sometimes must be
protected in order to provide the threading model promised by the ICU design.
The information on this page is intended for developers of ICU library code
itself.

ICU4J uses normal JDK synchronization services.

ICU4C faces a more difficult problem, as there is no standard, fully portable
set of C or C++ synchronization primitives. Internally, ICU4C provides a small
set of synchronization operations, and requires that all synchronization needed
within the ICU library code be implemented using them.

The ICU4C synchronization primitives are for internal use only; they are not
exported as API to normal users of ICU.

ICU provides implementations of its synchronization functions for Windows, POSIX
and C++11 platforms, and provides a build-time interface to allow [custom
implementations](custom.md) for other platforms.

## ICU4C Synchronization Primitives

The functions and types listed below are intended for use throughout the ICU
library code, where ever synchronization is required. They are defined in the
internal header
[umutex.h](https://github.com/unicode-org/icu/blob/main/icu4c/source/common/umutex.h).

All synchronization within ICU4C implementation code must use these, and avoid
direct use of functions provided by a particular operating system or compiler.

For examples of use, search the ICU library code.

**Low Level Atomics**

| Type/Function                            | Description                                                     |
|------------------------------------------|-----------------------------------------------------------------|
| `typedef u_atomic_int32_t`               | A 32 bit integer type for use with low level atomic operations. |
| `umtx_atomic_inc(u_atomic_int32_t &var)` |                                                                 |
| `umtx_atomic_dec(u_atomic_int32_t &var)` |                                                                 |

**Mutexes**

| Type/Function                | Description                                                           |
|------------------------------|-----------------------------------------------------------------------|
| `struct UMutex`              | An ICU mutex. All instances must be `static`.                         |
| `U_MUTEX_INITIALIZER`        | A C style initializer for a `UMutex`.                                 |
| `umtx_lock(UMutex *mutex)`   | Lock a mutex.                                                         |
| `umtx_unlock(UMutex* mutex)` | Unlock a mutex.                                                       |
| `class Mutex`                | C++ Mutex wrapper with automatic lock & unlock. See header `mutex.h.` |

**One Time Initialization**

| Type/Function                   | Description                                                                             |
|---------------------------------|-----------------------------------------------------------------------------------------|
| `struct UInitOnce`              | Provides an efficient facility for one-time initialization of static or global objects. |
| `umtx_initOnce(UInitOnce, ...)` | A family of initialization functions.                                                   |

All of these functions are for internal ICU implementation use only. They are
not exported, and not intended for external use.
