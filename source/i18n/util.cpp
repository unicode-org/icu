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
#include "unicode/uchar.h"

// Define UChar constants using hex for EBCDIC compatibility
// Used #define to reduce private static exports and memory access time.
#define BACKSLASH       ((UChar)0x005C) /*\*/
#define UPPER_U         ((UChar)0x0055) /*U*/
#define LOWER_U         ((UChar)0x0075) /*u*/

#define QUOTE           ((UChar)0x0027) /*'*/
#define ESCAPE          ((UChar)0x005C) /*\*/

// "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
static const UChar DIGITS[] = {
    48,49,50,51,52,53,54,55,56,57,
    65,66,67,68,69,70,71,72,73,74,
    75,76,77,78,79,80,81,82,83,84,
    85,86,87,88,89,90
};

UnicodeString& ICU_Utility::appendNumber(UnicodeString& result, int32_t n,
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
UBool ICU_Utility::isUnprintable(UChar32 c) {
    return !(c == 0x0A || (c >= 0x20 && c <= 0x7E));
}

/**
 * Escape unprintable characters using \uxxxx notation for U+0000 to
 * U+FFFF and \Uxxxxxxxx for U+10000 and above.  If the character is
 * printable ASCII, then do nothing and return FALSE.  Otherwise,
 * append the escaped notation and return TRUE.
 */
UBool ICU_Utility::escapeUnprintable(UnicodeString& result, UChar32 c) {
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

/**
 * Returns the index of a character, ignoring quoted text.
 * For example, in the string "abc'hide'h", the 'h' in "hide" will not be
 * found by a search for 'h'.
 */
int32_t ICU_Utility::quotedIndexOf(const UnicodeString& text,
                               int32_t start, int32_t limit,
                               UChar charToFind) {
    for (int32_t i=start; i<limit; ++i) {
        UChar c = text.charAt(i);
        if (c == ESCAPE) {
            ++i;
        } else if (c == QUOTE) {
            while (++i < limit
                   && text.charAt(i) != QUOTE) {}
        } else if (c == charToFind) {
            return i;
        }
    }
    return -1;
}

/**
 * Skip over a sequence of zero or more white space characters
 * at pos.  Return the index of the first non-white-space character
 * at or after pos, or str.length(), if there is none.
 */
int32_t ICU_Utility::skipWhitespace(const UnicodeString& str, int32_t pos) {
    while (pos < str.length()) {
        UChar32 c = str.char32At(pos);
        if (!u_isWhitespace(c)) {
            break;
        }
        pos += UTF_CHAR_LENGTH(c);
    }
    return pos;
}

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
int32_t ICU_Utility::parsePattern(const UnicodeString& rule, int32_t pos, int32_t limit,
                              const UnicodeString& pattern, int32_t* parsedInts) {
    // TODO Update this to handle surrogates
    int32_t p;
    int32_t intCount = 0; // number of integers parsed
    for (int32_t i=0; i<pattern.length(); ++i) {
        UChar cpat = pattern.charAt(i);
        UChar c;
        switch (cpat) {
        case 32 /*' '*/:
            if (pos >= limit) {
                return -1;
            }
            c = rule.charAt(pos++);
            if (!u_isWhitespace(c)) {
                return -1;
            }
            // FALL THROUGH to skipWhitespace
        case 126 /*'~'*/:
            pos = skipWhitespace(rule, pos);
            break;
        case 35 /*'#'*/:
            p = pos;
            parsedInts[intCount++] = parseInteger(rule, p, limit);
            if (p == pos) {
                // Syntax error; failed to parse integer
                return -1;
            }
            pos = p;
            break;
        default:
            if (pos >= limit) {
                return -1;
            }
            c = (UChar) u_tolower(rule.charAt(pos++));
            if (c != cpat) {
                return -1;
            }
            break;
        }
    }
    return pos;
}

static const UChar ZERO_X[] = {48, 120, 0}; // "0x"

/**
 * Parse an integer at pos, either of the form \d+ or of the form
 * 0x[0-9A-Fa-f]+ or 0[0-7]+, that is, in standard decimal, hex,
 * or octal format.
 * @param pos INPUT-OUTPUT parameter.  On input, the first
 * character to parse.  On output, the character after the last
 * parsed character.
 */
int32_t ICU_Utility::parseInteger(const UnicodeString& rule, int32_t& pos, int32_t limit) {
    int32_t count = 0;
    int32_t value = 0;
    int32_t p = pos;
    int8_t radix = 10;

    if (0 == rule.caseCompare(p, 2, ZERO_X, U_FOLD_CASE_DEFAULT)) {
        p += 2;
        radix = 16;
    } else if (p < limit && rule.charAt(p) == 48 /*0*/) {
        p++;
        count = 1;
        radix = 8;
    }

    while (p < limit) {
        int32_t d = u_digit(rule.charAt(p++), radix);
        if (d < 0) {
            --p;
            break;
        }
        ++count;
        int32_t v = (value * radix) + d;
        if (v <= value) {
            // If there are too many input digits, at some point
            // the value will go negative, e.g., if we have seen
            // "0x8000000" already and there is another '0', when
            // we parse the next 0 the value will go negative.
            return 0;
        }
        value = v;
    }
    if (count > 0) {
        pos = p;
    }
    return value;
}

//eof
