/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File uscanf.h
*
* Modification History:
*
*   Date        Name        Description
*   12/02/98    stephen        Creation.
*   03/13/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef USCANF_H
#define USCANF_H

#include "unicode/ustdio.h"
#include "ufmt_cmn.h"

/**
 * Struct encapsulating a single uscanf format specification.
 */
struct u_scanf_spec_info {
  UChar     fSpec;            /* Format specification  */

  int32_t    fWidth;            /* Width  */

  UChar     fPadChar;        /* Padding character  */

  bool_t     fIsLongDouble;        /* L flag  */
  bool_t     fIsShort;        /* h flag  */
  bool_t     fIsLong;        /* l flag  */
  bool_t     fIsLongLong;        /* ll flag  */
};
typedef struct u_scanf_spec_info u_scanf_spec_info;

/**
 * A u_scanf info function.
 * A u_scanf info is reponsible for reporting to u_scanf how many
 * arguments are required for the <TT>u_scanf_spec_info</TT> <TT>info</TT>,
 * and what their types are.
 * @param info A pointer to a <TT>uscan_info</TT> struct containing
 * information on the format specification.
 * @param argtypes The array to receive the types of arguments specified
 * by <TT>info</TT>.
 * @param n The number of available slots in the array <TT>argtypes</TT>
 * @return The number of arguments required by <TT>info</TT>.
 */
typedef int32_t (*u_scanf_info) (const u_scanf_spec_info     *info,
                int32_t             *argtypes,
                int32_t             n);

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

/**
 * Register a u_scanf handler function with u_scanf.
 * @param spec The format specififier handled by the handler <TT>func</TT>.
 * @param nfo A pointer to the <TT>u_scanf_info</TT> function used
 * to determine how many arguments are required for <TT>spec</TT>, and
 * what their types are.
 * @param handler A pointer to the <TT>u_scanf_handler</TT> function.
 * @return 0 if successful
 */
int32_t
u_scanf_register_handler (UChar            spec, 
             u_scanf_info         info,
             u_scanf_handler     handler);

#endif

