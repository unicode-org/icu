/********************************************************************
*
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*********************************************************************/

/**
   API's for writing UCMPs
 */

#ifndef _UCMPWRIT
#define _UCMPWRIT

#include "utypes.h"
#include "unewdata.h"

#include "ucmp8.h"
#include "ucmp16.h"
#include "ucmp32.h"

U_CAPI  void U_EXPORT2 udata_write_ucmp8 (UNewDataMemory *pData, const CompactByteArray* array);

U_CAPI  void U_EXPORT2 udata_write_ucmp16(UNewDataMemory *pData, const CompactShortArray* array);

/*Not implemented. [yet] */
/*U_CAPI  void U_EXPORT2 udata_write_ucmp32(UNewDataMemory *pData, const CompactIntArray* array);*/


#endif
