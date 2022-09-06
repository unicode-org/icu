// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
******************************************************************************
*
*   Copyright (C) 2006-2008, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*  FILE NAME : testTimezone.c
*
*   Date        Name        Description
*   03/02/2006  grhoten     Creation.
******************************************************************************
*/
#include "unicode/putil.h"
#include "unicode/ucnv.h"
#include "unicode/uloc.h"
#include "unicode/ures.h"
#include <stdbool.h>
#include <stdio.h>
#include <string.h>

int main(int argc, const char* const argv[]) {
    UErrorCode status = U_ZERO_ERROR;

    ures_close(ures_open(NULL, NULL, &status));
    if (status != U_ZERO_ERROR) {
        printf("uloc_getDefault = %s\n", uloc_getDefault());
        printf("Locale available in ICU = %s\n", status == U_ZERO_ERROR ? "true" : "false");
    }
    if (strcmp(ucnv_getDefaultName(), "US-ASCII") == 0) {
        printf("uprv_getDefaultCodepage = %s\n", uprv_getDefaultCodepage());
        printf("ucnv_getDefaultName = %s\n", ucnv_getDefaultName());
    }
    return 0;
}
