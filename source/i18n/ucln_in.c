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

static UBool i18n_cleanup(void)
{
#if !UCONFIG_NO_TRANSLITERATION
    transliterator_cleanup();
#endif

#if !UCONFIG_NO_FORMATTING
    timeZone_cleanup();
#endif

#if !UCONFIG_NO_COLLATION
    ucol_cleanup();
    ucol_bld_cleanup();
#endif

#if !UCONFIG_NO_REGULAR_EXPRESSIONS
    regex_cleanup();
#endif

    return TRUE;
}

void ucln_i18n_registerCleanup()
{
    ucln_registerCleanup(UCLN_I18N, i18n_cleanup);
}

