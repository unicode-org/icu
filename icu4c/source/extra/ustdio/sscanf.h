/*
******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File sscanf.h
*
* Modification History:
*
*   Date        Name        Description
*   02/08/00    george      Creation. Copied from uscanf.h
******************************************************************************
*/

#ifndef _USSCANF_H
#define _USSCANF_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ustdio.h"
#include "ufmt_cmn.h"
#include "locbund.h"

/**
 * Struct encapsulating a single uscanf format specification.
 */
struct u_sscanf_spec_info {
  UChar     fSpec;            /* Format specification  */

  int32_t    fWidth;            /* Width  */

  UChar     fPadChar;        /* Padding character  */

  UBool     fIsLongDouble;        /* L flag  */
  UBool     fIsShort;        /* h flag  */
  UBool     fIsLong;        /* l flag  */
  UBool     fIsLongLong;        /* ll flag  */
};
typedef struct u_sscanf_spec_info u_sscanf_spec_info;

struct u_localized_string {
  UChar     *str;   /* Place to write the string */
  int32_t   pos;    /* Number of codeunits available to write to */
  int32_t   len;    /* Maximum number of code units that can be written to output */

  ULocaleBundle  *fBundle;     /* formatters */
  UBool        fOwnBundle;     /* TRUE if fBundle should be deleted */
};
typedef struct u_localized_string u_localized_string;

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
typedef int32_t (*u_sscanf_handler) (u_localized_string    *input,
                   const u_sscanf_spec_info     *info,
                   ufmt_args  *args,
                   const UChar            *fmt,
                   int32_t            *consumed);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
