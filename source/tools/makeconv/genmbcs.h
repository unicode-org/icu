/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genmbcs.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jul10
*   created by: Markus W. Scherer
*/

#ifndef __GENMBCS_H__
#define __GENMBCS_H__

#include "makeconv.h"

U_CFUNC NewConverter *
MBCSOpen(uint8_t maxCharLength);

U_CFUNC UBool
MBCSAddState(NewConverter *cnvData, const char *s);

#endif
