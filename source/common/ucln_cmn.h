/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2001, International Business Machines                   *
*                Corporation and others. All Rights Reserved.                *
*                                                                            *
******************************************************************************
*   file name:  ucln_cmn.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001July05
*   created by: George Rhoten
*/

#ifndef __UCLN_CMN_H__
#define __UCLN_CMN_H__

#include "unicode/utypes.h"

U_CAPI UBool U_EXPORT2 ucnv_cleanup(void);

U_CAPI UBool U_EXPORT2 ures_cleanup(void);

U_CAPI UBool U_EXPORT2 uloc_cleanup(void);

U_CAPI UBool U_EXPORT2 ustring_cleanup(void);

U_CAPI UBool U_EXPORT2 udata_cleanup(void);

U_CAPI UBool U_EXPORT2 ucnv_io_cleanup(void);

#endif
