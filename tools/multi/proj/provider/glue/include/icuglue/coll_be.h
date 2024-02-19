// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#ifndef _COLL_BE
#define _COLL_BE

#include <icuglue/icuglue.h>
#include <unicode/ucoll.h> /* for definitions */

#if defined(XP_CPLUSPLUS)
extern "C" {
#endif

void * GLUE_SYM(ucol_open) (const char *locale);
void * GLUE_SYM(ucol_close) (void *coll);
void * GLUE_SYM(


#if defined(XP_CPLUSPLUS) 
}
#endif

#endif