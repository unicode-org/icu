/*
*****************************************************************************************
*
*   Copyright (C) 2004-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*****************************************************************************************
*/

#include "unicode/utypes.h"

/*Deals with imports and exports of the dynamic library*/
#if !defined(U_STATIC_IMPLEMENTATION) && (defined(_WIN32) || defined(U_CYGWIN))
    #define T_CTEST_EXPORT __declspec(dllexport)
    #define T_CTEST_IMPORT __declspec(dllimport)
#else
    #define T_CTEST_EXPORT
    #define T_CTEST_IMPORT
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

