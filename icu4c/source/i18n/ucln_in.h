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

/* Main library cleanup function. */
U_CFUNC void ucln_i18n_registerCleanup(void);

U_CFUNC UBool ucol_cleanup(void);

U_CFUNC UBool ucol_bld_cleanup(void);

U_CFUNC UBool timeZone_cleanup(void);

U_CFUNC UBool transliterator_cleanup(void);

#endif
