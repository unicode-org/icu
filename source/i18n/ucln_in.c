/*
******************************************************************************
*                                                                            *
* Copyright (C) 2001-2003, International Business Machines                   *
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

/* Leave this copyright notice here! It needs to go somewhere in this library. */
static const char copyright[] = U_COPYRIGHT_STRING;

static UBool i18n_cleanup(void)
{
#if !UCONFIG_NO_TRANSLITERATION
    transliterator_cleanup();
#endif

#if !UCONFIG_NO_REGULAR_EXPRESSIONS
    regex_cleanup();
#endif

#if !UCONFIG_NO_FORMATTING
    calendar_cleanup();
    numfmt_cleanup();
    currency_cleanup();
    timeZone_cleanup();
#endif

#if !UCONFIG_NO_COLLATION
    collator_cleanup();
    ucol_cleanup();
    ucol_bld_cleanup();
#endif

    return TRUE;
}

void ucln_i18n_registerCleanup()
{
    ucln_registerCleanup(UCLN_I18N, i18n_cleanup);
}

