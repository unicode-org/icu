/*
******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
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

#ifndef USSCANF_H
#define USSCANF_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "unicode/ustdio.h"
#include "ufmt_cmn.h"
#include "locbund.h"
#include "uscanf.h"

struct u_localized_string {
  UChar     *str;   /* Place to write the string */
  int32_t   pos;    /* Number of codeunits available to write to */
  int32_t   len;    /* Maximum number of code units that can be written to output */

  ULocaleBundle  fBundle;     /* formatters */
};
typedef struct u_localized_string u_localized_string;


#endif /* #if !UCONFIG_NO_FORMATTING */

#endif

