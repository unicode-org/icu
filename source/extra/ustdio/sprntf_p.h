/*
******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File sprntf_p.h
*
* Modification History:
*
*   Date        Name        Description
*   02/08/00    george      Creation. Copied from uprntf_p.h
******************************************************************************
*/

#ifndef USPRINTF_P_H
#define USPRINTF_P_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "sprintf.h"

/**
 * Struct encapsulating a single uprintf format specification.
 */
struct u_sprintf_spec {
  u_sprintf_spec_info    fInfo;        /* Information on this spec */
  int32_t        fWidthPos;     /* Position of width in arg list */
  int32_t        fPrecisionPos;    /* Position of precision in arg list */
  int32_t        fArgPos;    /* Position of data in arg list */
};
typedef struct u_sprintf_spec u_sprintf_spec;

/**
 * Parse a single u_printf format specifier.
 * @param fmt A pointer to a '%' character in a u_printf format specification.
 * @param spec A pointer to a <TT>u_printf_spec</TT> to receive the parsed
 * format specifier.
 * @return The number of characters contained in this specifier.
 */
int32_t
u_sprintf_parse_spec (const UChar     *fmt,
             u_sprintf_spec    *spec);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
