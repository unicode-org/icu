/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/19/2001  aliu        Creation.
**********************************************************************
*/

#include "util.h"

// Define UChar constants using hex for EBCDIC compatibility
// Used #define to reduce private static exports and memory access time.
#define BACKSLASH       ((UChar)0x005C) /*\*/
#define UPPER_U         ((UChar)0x0055) /*U*/
#define LOWER_U         ((UChar)0x0075) /*u*/

// "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
static const UChar DIGITS[] = {
    48,49,50,51,52,53,54,55,56,57,
    65,66,67,68,69,70,71,72,73,74,
    75,76,77,78,79,80,81,82,83,84,
    85,86,87,88,89,90
};

UnicodeString& Utility::appendNumber(UnicodeString& result, int32_t n,
                                     int32_t radix, int32_t minDigits) {
    if (radix < 2 || radix > 36) {
        // Bogus radix
        return result.append((UChar)63/*?*/);
    }
    // Handle negatives
    if (n < 0) {
        n = -n;
        result.append((UChar)45/*-*/);
    }
    // First determine the number of digits
    int32_t nn = n;
    int32_t r = 1;
    while (nn >= radix) {
        nn /= radix;
        r *= radix;
        --minDigits;
    }
    // Now generate the digits
    while (--minDigits > 0) {
        result.append(DIGITS[0]);
    }
    while (r > 0) {
        int32_t digit = n / r;
        result.append(DIGITS[digit]);
        n -= digit * r;
        r /= radix;
    }
    return result;
}

static const UChar HEX[16] = {48,49,50,51,52,53,54,55,  // 0-7
                              56,57,65,66,67,68,69,70}; // 8-9 A-F

/**
 * Return true if the character is NOT printable ASCII.
 */
UBool Utility::isUnprintable(UChar32 c) {
    return !(c == 0x0A || (c >= 0x20 && c <= 0x7E));
}

/**
 * Escape unprintable characters using \uxxxx notation for U+0000 to
 * U+FFFF and \Uxxxxxxxx for U+10000 and above.  If the character is
 * printable ASCII, then do nothing and return FALSE.  Otherwise,
 * append the escaped notation and return TRUE.
 */
UBool Utility::escapeUnprintable(UnicodeString& result, UChar32 c) {
    if (isUnprintable(c)) {
        result.append(BACKSLASH);
        if (c & ~0xFFFF) {
            result.append(UPPER_U);
            result.append(HEX[0xF&(c>>28)]);
            result.append(HEX[0xF&(c>>24)]);
            result.append(HEX[0xF&(c>>20)]);
            result.append(HEX[0xF&(c>>16)]);
        } else {
            result.append(LOWER_U);
        }
        result.append(HEX[0xF&(c>>12)]);
        result.append(HEX[0xF&(c>>8)]);
        result.append(HEX[0xF&(c>>4)]);
        result.append(HEX[0xF&c]);
        return TRUE;
    }
    return FALSE;
}

//eof
