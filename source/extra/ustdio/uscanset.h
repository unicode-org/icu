/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
* File uscanset.h
*
* Modification History:
*
*   Date        Name        Description
*   12/03/98    stephen        Creation.
*   03/12/99    stephen     Modified for new C API.
*******************************************************************************
*/

#ifndef USCANSET_H
#define USCANSET_H

#include "unicode/utypes.h"


/**
 * Simple struct for a scanset pair, ie a-z or A-Z
 */
struct u_scanf_scanset_pair {
  UChar start;
  UChar end;
};
typedef struct u_scanf_scanset_pair u_scanf_scanset_pair;

#define U_SCANF_MAX_SCANSET_SIZE 512

/**
 * Struct representing a scanset
 */
struct u_scanf_scanset {
  UBool        is_inclusive;    /* false if '^' is given */

  UChar            singles        [U_SCANF_MAX_SCANSET_SIZE];
  u_scanf_scanset_pair     pairs         [U_SCANF_MAX_SCANSET_SIZE];

  int32_t        single_count;    /* count of single chars in set */
  int32_t        pair_count;     /* count of pairs in set */
};
typedef struct u_scanf_scanset u_scanf_scanset;

/**
 * Init a u_scanf_scanset.
 * @param scanset A pointer to the u_scanf_scanset to init.
 * @param s A pointer to the first character in the scanset
 * @param len On input, a pointer to the length of <TT>s</TT>.  On output,
 * a pointer to the number of characters parsed, excluding the final ']'
 * @return TRUE if successful, FALSE otherwise.
 */
UBool
u_scanf_scanset_init(u_scanf_scanset     *scanset,
             const UChar    *s,
             int32_t        *len);

/**
 * Determine if a UChar is in a u_scanf_scanset
 * @param scanset A pointer to a u_scanf_scanset
 * @param c The UChar to test.
 * @return TRUE if the UChar is in the scanset, FALSE otherwise
 */
UBool
u_scanf_scanset_in(u_scanf_scanset     *scanset,
           UChar         c);

#endif




