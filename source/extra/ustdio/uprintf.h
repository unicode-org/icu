/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1999           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File uprintf.h
*
* Modification History:
*
*   Date        Name        Description
*   11/19/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef UPRINTF_H
#define UPRINTF_H

#include "unicode/utypes.h"
#include "ustdio.h"
#include "ufmt_cmn.h"

/**
 * Struct encapsulating a single uprintf format specification.
 */
struct u_printf_spec_info {
  UChar     fSpec;            /* Conversion specification */

  int32_t    fPrecision;        /* Precision  */
  int32_t    fWidth;            /* Width  */

  UChar     fPadChar;        /* Padding character  */

  bool_t     fAlt;            /* # flag  */
  bool_t     fSpace;            /* Space flag  */
  bool_t     fLeft;            /* - flag  */
  bool_t     fShowSign;        /* + flag  */
  bool_t     fZero;            /* 0 flag  */

  bool_t     fIsLongDouble;        /* L flag  */
  bool_t     fIsShort;        /* h flag  */
  bool_t     fIsLong;        /* l flag  */
  bool_t     fIsLongLong;        /* ll flag  */
};
typedef struct u_printf_spec_info u_printf_spec_info;

/**
 * A u_printf info function.
 * A u_printf info is reponsible for reporting to u_printf how many
 * arguments are required for the <TT>u_printf_spec_info</TT> <TT>info</TT>,
 * and what their types are.
 * @param info A pointer to a <TT>u_print_info</TT> struct containing
 * information on the format specification.
 * @param argtypes The array to receive the types of arguments specified
 * by <TT>info</TT>.
 * @param n The number of available slots in the array <TT>argtypes</TT>
 * @return The number of arguments required by <TT>info</TT>.
 */
typedef int32_t (*u_printf_info) (const u_printf_spec_info     *info,
                  int32_t             *argtypes,
                  int32_t             n);

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

/**
 * Register a uprintf handler function with uprintf.
 * @param spec The format specififier handled by the handler <TT>func</TT>.
 * @param info A pointer to the <TT>uprintf_info</TT> function used
 * to determine how many arguments are required for <TT>spec</TT>, and
 * what their types are.
 * @param handler A pointer to the <TT>uprintf_handler</TT> function.
 * @return 0 if successful
 */
int32_t
u_printf_register_handler(UChar            spec, 
              u_printf_info     info,
              u_printf_handler     handler);

#endif

