/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucol_sol.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created 06/27/2001
*   created by: Vladimir Weinstein
*
* Just trying to help Paul Grinberg compile on Solaris 8 using Workshop 6 compiler
* 
*/

#ifndef UCOL_SOL_H
#define UCOL_SOL_H

#include "unicode/utypes.h"
#include "uhash.h"

U_CFUNC int32_t uhash_hashTokens(const UHashKey k);
U_CFUNC UBool uhash_compareTokens(const UHashKey key1, const UHashKey key2);
U_CFUNC void deleteToken(void *token);

#endif


