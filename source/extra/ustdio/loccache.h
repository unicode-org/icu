/*
*******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File loccache.h
*
* Modification History:
*
*   Date        Name        Description
*   11/18/98    stephen        Creation.
*   03/11/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef LOCCACHE_H
#define LOCCACHE_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "locbund.h"

ULocaleBundle*
u_loccache_get(const char *loc);

/* Main library cleanup function. */
U_CFUNC void ucln_ustdio_registerCleanup(void);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
