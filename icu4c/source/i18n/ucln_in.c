/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2001, International Business Machines                   *
*                Corporation and others. All Rights Reserved.                *
*                                                                            *
******************************************************************************
*   file name:  ucln_cmn.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001July05
*   created by: George Rhoten
*/

#include "ucln.h"
#include "ucln_in.h"

UBool i18n_cleanup(void)
{
    ucol_cleanup();
}

void i18n_registerCleanup()
{
    ucln_registerCleanup(UCLN_I18N, i18n_cleanup);
}

