/*
**********************************************************************
* Copyright (C) 1998-2001, International Business Machines Corporation
* and others.  All Rights Reserved.
**********************************************************************
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
