/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998, 1999     *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
* File uprint.h
*
* Modification History:
*
*   Date        Name        Description
*   06/14/99    stephen     Creation.
*******************************************************************************
*/

#ifndef UPRINT_H
#define UPRINT_H 1

#include <stdio.h>

#include "unicode/utypes.h"

/* Print a ustring to the specified FILE* in the default codepage */
U_CAPI void uprint(const UChar *s, FILE *f, UErrorCode *status);

#endif /* ! UPRINT_H */
