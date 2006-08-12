/*
*****************************************************************************************
*
*   Copyright (C) 2004-2006, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
*/

#include "unicode/utypes.h"

/*Deals with imports and exports of the dynamic library*/
#if !defined(U_STATIC_IMPLEMENTATION) && (defined(U_WINDOWS) || defined(U_CYGWIN))
    #define T_CTEST_EXPORT __declspec(dllexport)
    #define T_CTEST_IMPORT __declspec(dllimport)
#elif defined(USE_GCC_VISIBILITY_ATTRIBUTE)
    #define T_CTEST_EXPORT __attribute__((visibility("default")))
    #define T_CTEST_IMPORT
#else
    #define T_CTEST_EXPORT
    #define T_CTEST_IMPORT
#endif

#if defined(U_WINDOWS)
#define T_CTEST_EXPORT2 __cdecl
#else
#define T_CTEST_EXPORT2
#endif

#ifdef __cplusplus
    #define C_CTEST_API extern "C"
#else
    #define C_CTEST_API
#endif

#ifdef T_CTEST_IMPLEMENTATION
    #define T_CTEST_API C_CTEST_API  T_CTEST_EXPORT
    #define T_CTEST_EXPORT_API T_CTEST_EXPORT
#else
    #define T_CTEST_API C_CTEST_API  T_CTEST_IMPORT
    #define T_CTEST_EXPORT_API T_CTEST_IMPORT
#endif

