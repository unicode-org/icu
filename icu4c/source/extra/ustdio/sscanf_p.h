/*
******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File sscnf_p.h
*
* Modification History:
*
*   Date        Name        Description
*   02/08/00    george      Creation. Copied from uscnf_p.h
******************************************************************************
*/

#ifndef _USSCANF_P_H
#define _USSCANF_P_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "sscanf.h"

/**
 * Struct encapsulating a single u_scanf format specification.
 */
struct u_sscanf_spec {
  u_sscanf_spec_info    fInfo;        /* Information on this spec */
  int32_t        fArgPos;    /* Position of data in arg list */
  UBool        fSkipArg;    /* TRUE if arg should be skipped */
};
typedef struct u_sscanf_spec u_sscanf_spec;

/**
 * Parse a single u_scanf format specifier.
 * @param fmt A pointer to a '%' character in a u_scanf format specification.
 * @param spec A pointer to a <TT>u_scanf_spec</TT> to receive the parsed
 * format specifier.
 * @return The number of characters contained in this specifier.
 */
int32_t
u_sscanf_parse_spec (const UChar     *fmt,
            u_sscanf_spec    *spec);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
