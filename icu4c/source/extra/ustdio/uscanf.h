/*
******************************************************************************
*
*   Copyright (C) 1998-2004, International Business Machines
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
  int32_t    fWidth;        /* Width  */

  UChar     fSpec;          /* Format specification  */

  UChar     fPadChar;       /* Padding character  */

  UBool     fIsLongDouble;  /* L flag  */
  UBool     fIsShort;       /* h flag  */
  UBool     fIsLong;        /* l flag  */
  UBool     fIsLongLong;    /* ll flag  */
};
typedef struct u_scanf_spec_info u_scanf_spec_info;


#endif /* #if !UCONFIG_NO_FORMATTING */

#endif

