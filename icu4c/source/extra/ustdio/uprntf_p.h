/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File uprntf_p.h
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef UPRNTF_P_H
#define UPRNTF_P_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "uprintf.h"

/**
 * Struct encapsulating a single uprintf format specification.
 */
struct u_printf_spec {
  u_printf_spec_info    fInfo;        /* Information on this spec */
  int32_t        fWidthPos;     /* Position of width in arg list */
  int32_t        fPrecisionPos;    /* Position of precision in arg list */
  int32_t        fArgPos;    /* Position of data in arg list */
};
typedef struct u_printf_spec u_printf_spec;

/**
 * Parse a single u_printf format specifier.
 * @param fmt A pointer to a '%' character in a u_printf format specification.
 * @param spec A pointer to a <TT>u_printf_spec</TT> to receive the parsed
 * format specifier.
 * @return The number of characters contained in this specifier.
 */
int32_t
u_printf_parse_spec (const UChar     *fmt,
             u_printf_spec    *spec);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
