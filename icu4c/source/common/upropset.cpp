/*
**********************************************************************
*   Copyright (c) 2001-2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#include "upropset.h"
#include "unicode/uniset.h"

U_NAMESPACE_BEGIN

/**
 * NOTE: This class is almost gone...TODO: Figure out where to put
 * getRuleWhiteSpaceSet(), move it there, and remove this file.
 */

UnicodeSet
UnicodePropertySet::getRuleWhiteSpaceSet(UErrorCode &status) {
    UErrorCode ec = U_ZERO_ERROR;
    /* "white space" in the sense of ICU rule parsers: Cf+White_Space */
    UnicodeSet set("[[:Cf:][:WSpace:]]", ec);
    return set; /* return by value */
}

U_NAMESPACE_END

//eof
