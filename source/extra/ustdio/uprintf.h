/*
******************************************************************************
*
*   Copyright (C) 1998-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
* File uprintf.h
*
* Modification History:
*
*   Date        Name        Description
*   11/19/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
******************************************************************************
*/

#ifndef UPRINTF_H
#define UPRINTF_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ustdio.h"
#include "ufmt_cmn.h"

/**
 * Struct encapsulating a single uprintf format specification.
 */
struct u_printf_spec_info {
  UChar     fSpec;            /* Conversion specification */

  int32_t    fPrecision;        /* Precision  */
  int32_t    fWidth;            /* Width  */

  UChar     fPadChar;        /* Padding character  */

  UBool     fAlt;            /* # flag  */
  UBool     fSpace;            /* Space flag  */
  UBool     fLeft;            /* - flag  */
  UBool     fShowSign;        /* + flag  */
  UBool     fZero;            /* 0 flag  */

  UBool     fIsLongDouble;        /* L flag  */
  UBool     fIsShort;        /* h flag  */
  UBool     fIsLong;        /* l flag  */
  UBool     fIsLongLong;        /* ll flag  */
};
typedef struct u_printf_spec_info u_printf_spec_info;


/**
 * A u_printf handler function.  
 * A u_printf handler is responsible for handling a single u_printf 
 * format specification, for example 'd' or 's'.
 * @param stream The UFILE to which to write output.
 * @param info A pointer to a <TT>u_printf_spec_info</TT> struct containing
 * information on the format specification.
 * @param args A pointer to the argument data
 * @return The number of Unicode characters written to <TT>stream</TT>.
 */
typedef int32_t (*u_printf_handler) (UFILE             *stream,
                     const u_printf_spec_info     *info,
                     const ufmt_args            *args);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
