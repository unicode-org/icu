/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/19/2001  aliu        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/unistr.h"

//--------------------------------------------------------------------
// class Utility
// i18n utility functions, scoped into the class Utility.
//--------------------------------------------------------------------

U_NAMESPACE_BEGIN

class Utility {
 public:

    /**
     * Append a number to the given UnicodeString in the given radix.
     * The radix must be between 2 and 36, inclusive.  Standard digits
     * '0'-'9' are used and letters 'A'-'Z' for radices 11 through 36.
     * If n is negative, a '-' is prepended.
     */
    static UnicodeString& appendNumber(UnicodeString& result, int32_t n,
                                       int32_t radix = 10);

};

U_NAMESPACE_END

//eof
