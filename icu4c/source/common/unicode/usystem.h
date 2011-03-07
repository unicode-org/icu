/*
*******************************************************************************
*   Copyright (C) 2004-2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*
*   file name:  usystem.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   Created by: genheaders.pl, a perl script written by Ram Viswanadha
*
*  Contains data for commenting out APIs.
*  Gets included by umachine.h
*
*  THIS FILE IS MACHINE-GENERATED, DON'T PLAY WITH IT IF YOU DON'T KNOW WHAT
*  YOU ARE DOING, OTHERWISE VERY BAD THINGS WILL HAPPEN!
*/

#ifndef USYSTEM_H
#define USYSTEM_H

#ifdef U_HIDE_SYSTEM_API

#    if U_DISABLE_RENAMING
#        define u_cleanup(void) u_cleanup(void)_SYSTEM_API_DO_NOT_USE
#        define u_setAtomicIncDecFunctions(const void *context, UMtxAtomicFn *inc, UMtxAtomicFn *dec, UErrorCode *status) u_setAtomicIncDecFunctions(const void *context, UMtxAtomicFn *inc, UMtxAtomicFn *dec, UErrorCode *status)_SYSTEM_API_DO_NOT_USE
#        define u_setMemoryFunctions(const void *context, UMemAllocFn *a, UMemReallocFn *r, UMemFreeFn *f, UErrorCode *status) u_setMemoryFunctions(const void *context, UMemAllocFn *a, UMemReallocFn *r, UMemFreeFn *f, UErrorCode *status)_SYSTEM_API_DO_NOT_USE
#        define u_setMutexFunctions(const void *context, UMtxInitFn *init, UMtxFn *destroy, UMtxFn *lock, UMtxFn *unlock, UErrorCode *status) u_setMutexFunctions(const void *context, UMtxInitFn *init, UMtxFn *destroy, UMtxFn *lock, UMtxFn *unlock, UErrorCode *status)_SYSTEM_API_DO_NOT_USE
#        define ucnv_setDefaultName(const char *name) ucnv_setDefaultName(const char *name)_SYSTEM_API_DO_NOT_USE
#        define uloc_getDefault(void) uloc_getDefault(void)_SYSTEM_API_DO_NOT_USE
#        define uloc_setDefault(const char *localeID, UErrorCode *status) uloc_setDefault(const char *localeID, UErrorCode *status)_SYSTEM_API_DO_NOT_USE
#    else
#        define u_cleanup(void)_4_7 u_cleanup(void)_SYSTEM_API_DO_NOT_USE
#        define u_setAtomicIncDecFunctions(const void *context, UMtxAtomicFn *inc, UMtxAtomicFn *dec, UErrorCode *status)_4_7 u_setAtomicIncDecFunctions(const void *context, UMtxAtomicFn *inc, UMtxAtomicFn *dec, UErrorCode *status)_SYSTEM_API_DO_NOT_USE
#        define u_setMemoryFunctions(const void *context, UMemAllocFn *a, UMemReallocFn *r, UMemFreeFn *f, UErrorCode *status)_4_7 u_setMemoryFunctions(const void *context, UMemAllocFn *a, UMemReallocFn *r, UMemFreeFn *f, UErrorCode *status)_SYSTEM_API_DO_NOT_USE
#        define u_setMutexFunctions(const void *context, UMtxInitFn *init, UMtxFn *destroy, UMtxFn *lock, UMtxFn *unlock, UErrorCode *status)_4_7 u_setMutexFunctions(const void *context, UMtxInitFn *init, UMtxFn *destroy, UMtxFn *lock, UMtxFn *unlock, UErrorCode *status)_SYSTEM_API_DO_NOT_USE
#        define ucnv_setDefaultName(const char *name)_4_7 ucnv_setDefaultName(const char *name)_SYSTEM_API_DO_NOT_USE
#        define uloc_getDefault(void)_4_7 uloc_getDefault(void)_SYSTEM_API_DO_NOT_USE
#        define uloc_setDefault(const char *localeID, UErrorCode *status)_4_7 uloc_setDefault(const char *localeID, UErrorCode *status)_SYSTEM_API_DO_NOT_USE
#    endif /* U_DISABLE_RENAMING */

#endif /* U_HIDE_SYSTEM_API */
#endif /* USYSTEM_H */

