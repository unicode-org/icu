/********************************************************************
*
*   Copyright (C) 1997-2004, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*********************************************************************/

/**
   API's for writing UCMPs.
   Return nbytes written.
 */

#ifndef _UCMPWRIT
#define _UCMPWRIT

#include "unicode/utypes.h"
#include "unewdata.h"

#include "ucmp8.h"

/* udata filestream variants */
U_CAPI  uint32_t U_EXPORT2 udata_write_ucmp8 (UNewDataMemory *pData, const CompactByteArray* array);

#endif






