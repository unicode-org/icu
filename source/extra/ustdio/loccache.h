/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
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

#include "locbund.h"
#include "uhash.h"

/* The global LocaleCacheInfo cache */
extern UHashtable *gLocaleCache;

ULocaleBundle*
u_loccache_get(const char *loc);

#endif
