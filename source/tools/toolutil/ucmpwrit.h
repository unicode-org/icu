/********************************************************************
*
*   Copyright (C) 1997-2000, International Business Machines
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
#include "umemstrm.h"

#include "ucmp8.h"
#if 0
#include "ucmp16.h"
#include "ucmp32.h"
#endif

/* udata filestream variants */
U_CAPI  uint32_t U_EXPORT2 udata_write_ucmp8 (UNewDataMemory *pData, const CompactByteArray* array);
#if 0
U_CAPI  uint32_t U_EXPORT2 udata_write_ucmp16(UNewDataMemory *pData, const CompactShortArray* array);
U_CAPI  uint32_t U_EXPORT2 udata_write_ucmp32(UNewDataMemory *pData, const CompactIntArray* array);
#endif

#endif






