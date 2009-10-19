/*
*******************************************************************************
*
*   Copyright (C) 2009, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*/

#ifndef _ICUGLUE_H
#define _ICUGLUE_H

/* Get utypes.h from whatever ICU we are under */
#include <unicode/utypes.h>

//#define GLUE_SYM(x) glue ## x ## ICUGLUE_VER
#define GLUE_SYM_V(x, v) glue ## x ## v

#endif