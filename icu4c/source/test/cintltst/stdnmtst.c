/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2000, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/
/*
* File stdnmtst.c
*
* Modification History:
*
*   Date          Name        Description
*   08/05/2000    Yves       Creation 
*******************************************************************************
*/

#include "unicode/ucnv.h"
#include "cstring.h"
#include "cintltst.h"

#include <stdio.h>

void TestStandardNames();



void
addStandardNamesTest(TestNode** root)
{
  addTest(root, &TestStandardNames,    "stdnmtst/TestStandardNames");
}

static void dotestname(const char *name, const char *standard, const char *expected) {
    UErrorCode error;
    const char *tag;

    error = U_ZERO_ERROR;
    tag = ucnv_getStandardName(name, standard, &error);
    if (!tag) {
        log_err("FAIL: could not find %s standard name for %s\n", standard, name);
    } else if (expected && uprv_strcmp(expected, tag)) {
        log_err("FAIL: expected %s for %s standard name for %s, got %s\n", expected, standard, name, tag);
    }
}

void TestStandardNames()
{
    int i, count;
    UErrorCode err;

#if  0
    /* Iterate over all standards. */
 
    for (i = 0, count = ucnv_countStandards(); i < count; ++i) {
        const char *std;

        err = U_ZERO_ERROR;
        std = ucnv_getStandard(i, &err);
        if (U_FAILURE(err)) {
            log_err("FAIL: ucnv_getStandard(%d), error=%s\n", i, u_errorName(err));
        } else if (!std || !*std) {
            log_err("FAIL: %s standard name at index %d\n", (std ? "empty" :
                "null"), i);
        }
    }
    err = U_ZERO_ERROR;
    if (ucnv_getStandard(i, &err)) {
        log_err("FAIL: ucnv_getStandard(%d) should return NULL\n", i);
    }

#endif

    /* Test for some expected results. */

    dotestname("ibm-1208", "MIME", "utf-8");
    dotestname("cp1252", "MIME", "windows-1252");
    dotestname("ascii", "MIME", "us-ascii");
    dotestname("ascii", "IANA", "ANSI_X3.4-1968");
}

