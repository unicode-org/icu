/*
******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File sprintf.h
*
* Modification History:
*
*   Date        Name        Description
*   02/08/00    george      Creation. Copied from uprintf.h
******************************************************************************
*/

#ifndef USPRINTF_H
#define USPRINTF_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ustdio.h"
#include "uprintf.h"
#include "ufmt_cmn.h"
#include "locbund.h"

struct u_localized_string {
  UChar     *str;     /* Place to write the string */
  int32_t   available;/* Number of codeunits available to write to */
  int32_t   len;      /* Maximum number of code units that can be written to output */

  ULocaleBundle  *fBundle;     /* formatters */
  UBool        fOwnBundle;     /* TRUE if fBundle should be deleted */
};
typedef struct u_localized_string u_localized_string;

/**
 * A u_printf handler function.  
 * A u_printf handler is responsible for handling a single u_printf 
 * format specification, for example 'd' or 's'.
 * @param info A pointer to a <TT>u_printf_spec_info</TT> struct containing
 * information on the format specification.
 * @param args A pointer to the argument data
 * @return The number of Unicode characters written to <TT>stream</TT>.
 */
typedef int32_t (*u_sprintf_handler) (u_localized_string *output,
                        const u_printf_spec_info  *info,
                        const ufmt_args            *args);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
