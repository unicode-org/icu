/*
*******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File uscnf_p.h
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/13/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef _USCANF_P
#define _USCANF_P

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "uscanf.h"

/**
 * Struct encapsulating a single u_scanf format specification.
 */
struct u_scanf_spec {
  u_scanf_spec_info    fInfo;        /* Information on this spec */
  int32_t        fArgPos;    /* Position of data in arg list */
  UBool        fSkipArg;    /* TRUE if arg should be skipped */
};
typedef struct u_scanf_spec u_scanf_spec;

/**
 * Parse a single u_scanf format specifier.
 * @param fmt A pointer to a '%' character in a u_scanf format specification.
 * @param spec A pointer to a <TT>u_scanf_spec</TT> to receive the parsed
 * format specifier.
 * @return The number of characters contained in this specifier.
 */
int32_t
u_scanf_parse_spec (const UChar     *fmt,
            u_scanf_spec    *spec);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
