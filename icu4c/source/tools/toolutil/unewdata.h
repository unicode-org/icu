/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*   file name:  unewdata.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 1999oct25
*   created by: Markus W. Scherer
*/

#ifndef __UNEWDATA_H__
#define __UNEWDATA_H__

#include "utypes.h"
#include "udata.h"

/* API for writing data -----------------------------------------------------*/

/* forward declaration of the data memory creation type */
typedef struct UNewDataMemory UNewDataMemory;

U_CAPI UNewDataMemory * U_EXPORT2
udata_create(const char *type, const char *name,
             const UDataInfo *pInfo,
             const char *comment,
             UErrorCode *pErrorCode);

U_CAPI uint32_t U_EXPORT2
udata_finish(UNewDataMemory *pData, UErrorCode *pErrorCode);

U_CAPI void U_EXPORT2
udata_write8(UNewDataMemory *pData, uint8_t byte);

U_CAPI void U_EXPORT2
udata_write16(UNewDataMemory *pData, uint16_t word);

U_CAPI void U_EXPORT2
udata_write32(UNewDataMemory *pData, uint32_t wyde);

U_CAPI void U_EXPORT2
udata_writeBlock(UNewDataMemory *pData, const void *s, UTextOffset length);

U_CAPI void U_EXPORT2
udata_writeString(UNewDataMemory *pData, const char *s, UTextOffset length);

U_CAPI void U_EXPORT2
udata_writeUString(UNewDataMemory *pData, const UChar *s, UTextOffset length);

#endif
