/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2003, International Business Machines                   *
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

/* These are the cleanup functions for various APIs. */
/* @return true if cleanup complete successfully.*/
U_CFUNC UBool unames_cleanup(void);

U_CFUNC UBool unorm_cleanup(void);

U_CFUNC UBool uchar_cleanup(void);

U_CFUNC UBool pname_cleanup(void);

U_CFUNC UBool locale_cleanup(void);

U_CFUNC UBool uloc_cleanup(void);

U_CFUNC UBool breakiterator_cleanup(void);

U_CFUNC UBool usprep_cleanup(void);

U_CFUNC UBool U_EXPORT2 ucnv_cleanup(void);

U_CFUNC UBool ucnv_io_cleanup(void);

U_CFUNC UBool ures_cleanup(void);

U_CFUNC UBool udata_cleanup(void);

U_CFUNC UBool putil_cleanup(void);

U_CFUNC UBool uset_cleanup(void);

U_CFUNC UBool service_cleanup(void);

U_CFUNC UBool cmemory_cleanup(void);

U_CFUNC UBool umtx_cleanup(void);

U_CFUNC UBool utrace_cleanup(void);

/* Only mutexes should be initialized in these functions. */

U_CFUNC void ucnv_init(UErrorCode *status);

U_CFUNC void ures_init(UErrorCode *status);


#endif
