/*
 **********************************************************************
 *   Copyright (C) 1997-2003, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 **********************************************************************
*/

// C++ static initialization.
//
//    The purpose of this C++ file is to trigger the calling of
//      u_ICUStaticInitFunc() during static initialization.
//      (The function itself is in ucln_cmn.c, along with the rest
//       of ICU's initialization and clean up code.)
// 
#include "ucln_cmn.h"

//static UBool initializesGlobalMutex = u_ICUStaticInitFunc();


