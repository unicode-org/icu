/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/19/2001  aliu        Creation.
**********************************************************************
*/
#ifndef ICU_UTIL_H
#define ICU_UTIL_H

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

    /**
     * Return true if the character is NOT printable ASCII.
     *
     * This method should really be in UnicodeString (or similar).  For
     * now, we implement it here and share it with friend classes.
     */
    static UBool isUnprintable(UChar32 c);

    /**
     * Escape unprintable characters using \uxxxx notation for U+0000 to
     * U+FFFF and \Uxxxxxxxx for U+10000 and above.  If the character is
     * printable ASCII, then do nothing and return FALSE.  Otherwise,
     * append the escaped notation and return TRUE.
     */
    static UBool escapeUnprintable(UnicodeString& result, UChar32 c);
};

U_NAMESPACE_END

#endif
//eof
