/*
******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File uscanf.h
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/13/99    stephen     Modified for new C API.
******************************************************************************
*/

#ifndef USCANF_H
#define USCANF_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ustdio.h"
#include "ufmt_cmn.h"

/**
 * Struct encapsulating a single uscanf format specification.
 */
struct u_scanf_spec_info {
  UChar     fSpec;            /* Format specification  */

  int32_t    fWidth;            /* Width  */

  UChar     fPadChar;        /* Padding character  */

  UBool     fIsLongDouble;        /* L flag  */
  UBool     fIsShort;        /* h flag  */
  UBool     fIsLong;        /* l flag  */
  UBool     fIsLongLong;        /* ll flag  */
};
typedef struct u_scanf_spec_info u_scanf_spec_info;

/**
 * A u_scanf handler function.  
 * A u_scanf handler is responsible for handling a single u_scanf 
 * format specification, for example 'd' or 's'.
 * @param stream The UFILE to which to write output.
 * @param info A pointer to a <TT>u_scanf_spec_info</TT> struct containing
 * information on the format specification.
 * @param args A pointer to the argument data
 * @param fmt A pointer to the first character in the format string
 * following the spec.
 * @param consumed On output, set to the number of characters consumed
 * in <TT>fmt</TT>.
 * @return The number of arguments converted and assigned, or -1 if an
 * error occurred.
 */
typedef int32_t (*u_scanf_handler) (UFILE            *stream,
                   const u_scanf_spec_info     *info,
                   ufmt_args  *args,
                   const UChar            *fmt,
                   int32_t            *consumed);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
