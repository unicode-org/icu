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
     * Standard digits '0'-'9' are used and letters 'A'-'Z' for
     * radices 11 through 36.
     * @param result the digits of the number are appended here
     * @param n the number to be converted to digits; may be negative.
     * If negative, a '-' is prepended to the digits.
     * @param radix a radix from 2 to 36 inclusive.
     * @param minDigits the minimum number of digits, not including
     * any '-', to produce.  Values less than 2 have no effect.  One
     * digit is always emitted regardless of this parameter.
     * @return a reference to result
     */
    static UnicodeString& appendNumber(UnicodeString& result, int32_t n,
                                       int32_t radix = 10,
                                       int32_t minDigits = 1);

};

U_NAMESPACE_END

//eof
