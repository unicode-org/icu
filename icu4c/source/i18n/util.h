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
// class ICU_Utility
// i18n utility functions, scoped into the class ICU_Utility.
//--------------------------------------------------------------------

U_NAMESPACE_BEGIN

class ICU_Utility {
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

    /**
     * Returns the index of a character, ignoring quoted text.
     * For example, in the string "abc'hide'h", the 'h' in "hide" will not be
     * found by a search for 'h'.
     * @param text text to be searched
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param c character to search for
     * @return Offset of the first instance of c, or -1 if not found.
     */
    static int32_t quotedIndexOf(const UnicodeString& text,
                                 int32_t start, int32_t limit,
                                 UChar c);

    /**
     * Skip over a sequence of zero or more white space characters
     * at pos.  Return the index of the first non-white-space character
     * at or after pos, or str.length(), if there is none.
     */
    static int32_t skipWhitespace(const UnicodeString& str, int32_t pos);

    /**
     * Parse a pattern string starting at offset pos.  Keywords are
     * matched case-insensitively.  Spaces may be skipped and may be
     * optional or required.  Integer values may be parsed, and if
     * they are, they will be returned in the given array.  If
     * successful, the offset of the next non-space character is
     * returned.  On failure, -1 is returned.
     * @param pattern must only contain lowercase characters, which
     * will match their uppercase equivalents as well.  A space
     * character matches one or more required spaces.  A '~' character
     * matches zero or more optional spaces.  A '#' character matches
     * an integer and stores it in parsedInts, which the caller must
     * ensure has enough capacity.
     * @param parsedInts array to receive parsed integers.  Caller
     * must ensure that parsedInts.length is >= the number of '#'
     * signs in 'pattern'.
     * @return the position after the last character parsed, or -1 if
     * the parse failed
     */
    static int32_t parsePattern(const UnicodeString& rule, int32_t pos, int32_t limit,
                                const UnicodeString& pattern, int32_t* parsedInts);
        
    /**
     * Parse an integer at pos, either of the form \d+ or of the form
     * 0x[0-9A-Fa-f]+ or 0[0-7]+, that is, in standard decimal, hex,
     * or octal format.
     * @param pos INPUT-OUTPUT parameter.  On input, the first
     * character to parse.  On output, the character after the last
     * parsed character.
     */
    static int32_t parseInteger(const UnicodeString& rule, int32_t& pos, int32_t limit);
};

U_NAMESPACE_END

#endif
//eof
