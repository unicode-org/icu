/*
******************************************************************************
*
*   Copyright (C) 1998-2003, International Business Machines
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
#include "locbund.h"

/**
 * Struct encapsulating a single uprintf format specification.
 */
typedef struct u_printf_spec_info {
  int32_t    fPrecision;    /* Precision  */
  int32_t    fWidth;        /* Width  */

  UChar     fSpec;          /* Conversion specification */
  UChar     fPadChar;       /* Padding character  */

  UBool     fAlt;           /* # flag  */
  UBool     fSpace;         /* Space flag  */
  UBool     fLeft;          /* - flag  */
  UBool     fShowSign;      /* + flag  */
  UBool     fZero;          /* 0 flag  */

  UBool     fIsLongDouble;  /* L flag  */
  UBool     fIsShort;       /* h flag  */
  UBool     fIsLong;        /* l flag  */
  UBool     fIsLongLong;    /* ll flag  */
} u_printf_spec_info;

typedef int32_t U_EXPORT2
u_printf_write_stream(void          *context,
                      const UChar   *str,
                      int32_t       count);

typedef int32_t U_EXPORT2
u_printf_pad_and_justify_stream(void                        *context,
                                const u_printf_spec_info    *info,
                                const UChar                 *result,
                                int32_t                     resultLen);

typedef struct u_printf_stream_handler {
    u_printf_write_stream *write;
    u_printf_pad_and_justify_stream *pad_and_justify;
} u_printf_stream_handler;

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
typedef int32_t U_EXPORT2
u_printf_handler(const u_printf_stream_handler  *handler,
                 void                           *context,
                 ULocaleBundle                  *formatBundle,
                 const u_printf_spec_info       *info,
                 const ufmt_args                *args);

#endif /* #if !UCONFIG_NO_FORMATTING */

#endif
