/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
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
