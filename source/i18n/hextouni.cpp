/*
**********************************************************************
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/rep.h"
#include "unicode/unifilt.h"
#include "unicode/uchar.h"
#include "hextouni.h"


U_NAMESPACE_BEGIN

const char HexToUnicodeTransliterator::fgClassID=0;

/**
 * ID for this transliterator.
 */
const char HexToUnicodeTransliterator::_ID[] = "Hex-Any";

/**
 * This pattern encodes the following specs for the default constructor:
 *   \\u0000
 *   \\U0000
 *   u+0000
 *   U+0000
 * The multiple backslashes resolve to a single backslash
 * in the effective prefix.
 */
const UChar HexToUnicodeTransliterator::DEFAULT_PATTERN[] = {
    0x5C, 0x5C, 0x75, 0x30, 0x30, 0x30, 0x30, 0x3B,  /* "\\u0000;" */
    0x5C, 0x5C, 0x55, 0x30, 0x30, 0x30, 0x30, 0x3B,  /* "\\U0000;" */
    0x75, 0x2B, 0x30, 0x30, 0x30, 0x30, 0x3B,        /* "u+0000;" */
    0x55, 0x2B, 0x30, 0x30, 0x30, 0x30, 0           /* "U+0000" */
};  /* "\\u0000;\\U0000;u+0000;U+0000" */

static const UChar gQuadA[] = {
    0x41, 0x41, 0x41, 0x41, 0
};  /* "AAAA" */

/**
 * Constructs a transliterator.
 */
HexToUnicodeTransliterator::HexToUnicodeTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter) {
    // We don't need to pass the status back to the caller because
    // we know that the DEFAULT_PATTERN parses.
    UErrorCode status = U_ZERO_ERROR;
    applyPattern(DEFAULT_PATTERN, status);
}

/**
 * Constructs a transliterator.
 */
HexToUnicodeTransliterator::HexToUnicodeTransliterator(const UnicodeString& thePattern,
                                                       UErrorCode& status) :
    Transliterator(_ID, 0) {
    applyPattern(thePattern, status);
}

/**
 * Constructs a transliterator.
 */
HexToUnicodeTransliterator::HexToUnicodeTransliterator(const UnicodeString& thePattern,
                                                       UnicodeFilter* adoptedFilter,
                                                       UErrorCode& status) :
    Transliterator(_ID, adoptedFilter) {
    applyPattern(thePattern, status);
}

/**
 * Copy constructor.
 */
HexToUnicodeTransliterator::HexToUnicodeTransliterator(const HexToUnicodeTransliterator& o) :
    Transliterator(o),
    pattern(o.pattern),
    affixes(o.affixes),
    affixCount(o.affixCount) {
}

/**
 * Assignment operator.
 */
HexToUnicodeTransliterator& HexToUnicodeTransliterator::operator=(
                                             const HexToUnicodeTransliterator& o) {
    Transliterator::operator=(o);
    pattern = o.pattern;
    affixes = o.affixes;
    affixCount = o.affixCount;
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* HexToUnicodeTransliterator::clone(void) const {
    return new HexToUnicodeTransliterator(*this);
}

void HexToUnicodeTransliterator::applyPattern(const UnicodeString& thePattern,
                                              UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }

    /* The pattern is processed and stored in affixes.  The pattern
     * consists of zero or more affixes.  Each affix is parsed to
     * determine the prefix, suffix, minimum digit count, and maximum
     * digit count.  These values are then stored as a four character
     * header.  That is, their numeric values are cast to UChars and
     * stored in the string.  Following these four characters, the prefix
     * characters, then suffix characters are stored.  Each spec takes
     * n+4 characters, where n is the total length of the prefix and
     * suffix.
     */

    // POSSIBILE FUTURE MODIFICATION
    // Parse thePattern, and if this succeeds, set pattern to thePattern.
    // If it fails, call applyPattern(pattern) to restore the original
    // conditions.

    pattern = thePattern;
    affixes.truncate(0);
    affixCount = 0;

    /* The mode specifies where we are in each spec.
     * mode 0 = in prefix
     * mode 1 = in optional digits (#)
     * mode 2 = in required digits (0)
     * mode 3 = in suffix
     */
    int32_t mode = 0;

    int32_t prefixLen = 0, suffixLen = 0, minDigits = 0, maxDigits = 0;
    int32_t start = 0;

    /* To make parsing easier, we append a virtual ';' at the end of
     * the pattern string, if there isn't one already.  When we get to
     * the index pattern.length() (that is, one past the end), we
     * create a virtual ';' if necessary.
     */
    UChar c = 0; // These are outside the loop so we can see the
    UBool isLiteral = FALSE; // previous character...
    for (int32_t i=0; i<=pattern.length(); ++i) {
        // Create the virtual trailing ';' if necessary
        if (i == pattern.length()) {
            // If the last character was not a non-literal ';'...
            if (i > 0 && !(c == SEMICOLON && !isLiteral)) {
                c = SEMICOLON;
                isLiteral = FALSE;
            } else {
                break;
            }
        } else {
            c = pattern.charAt(i);
            isLiteral = FALSE;
        }

        if (c == BACKSLASH) {
            if ((i+1)<pattern.length()) {
                isLiteral = TRUE;
                c = pattern.charAt(++i);
            } else {
                // Trailing '\\'
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return;
            }
        }

        if (!isLiteral) {
            switch (c) {
            case POUND:
                // Seeing a '#' moves us from mode 0 (prefix) to mode 1
                // (optional digits).
                if (mode == 0) {
                    ++mode;
                } else if (mode != 1) {
                    // Unquoted '#'
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return;
                }
                ++maxDigits;
                break;
            case ZERO:
                // Seeing a '0' moves us to mode 2 (required digits)
                if (mode < 2) {
                    mode = 2;
                } else if (mode != 2) {
                    // Unquoted '0'
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return;
                }
                ++minDigits;
                ++maxDigits;
                break;
            case SEMICOLON:
                if (minDigits < 1 || maxDigits > 4
                    // Invalid min/max digit count
                    || prefixLen > 0xFFFF || suffixLen > 0xFFFF) {
                    // Suffix or prefix too long
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return;
                }
                // If there was no prefix and no suffix, then the
                // header will not have been allocated yet.  We need
                // allocate the header now.
                if (start == affixes.length()) {
                    affixes.append(gQuadA);
                }
                // Fill in 4-character header
                affixes.setCharAt(start++, (UChar) prefixLen);
                affixes.setCharAt(start++, (UChar) suffixLen);
                affixes.setCharAt(start++, (UChar) minDigits);
                affixes.setCharAt(start++, (UChar) maxDigits);
                start = affixes.length();
                ++affixCount;
                prefixLen = suffixLen = minDigits = maxDigits = mode = 0;
                break;
            default:
                isLiteral = TRUE;
                break;
            }
        }

        if (isLiteral) {
            if (start == affixes.length()) {
                // Make space for the header.  Append any four
                // characters as place holders for the header values.
                // We fill these in when we parse the ';'.
                affixes.append(gQuadA);
            }
            affixes.append(c);
            if (mode == 0) {
                ++prefixLen;
            } else {
                // Any literal outside the prefix moves us into mode 3
                // (suffix)
                mode = 3;
                ++suffixLen;
            }
        }
    }
}

const UnicodeString& HexToUnicodeTransliterator::toPattern(void) const {
    return pattern;
}

void HexToUnicodeTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                                     UBool isIncremental) const {
    int32_t cursor = offsets.start;
    int32_t limit = offsets.limit;
    int32_t i, j, ipat;

    while (cursor < limit) {
        // Loop over the specs in affixes.  If affixCount is zero (an
        // empty pattern), then we do nothing.  We exit this loop when
        // we match one of the specs.  We exit this function (by
        // jumping to exit: below) if a partial match is detected and
        // isIncremental is true.
        for (j=0, ipat=0; j<affixCount; ++j) {

            // Read the header
            int32_t prefixLen = affixes.charAt(ipat++);
            int32_t suffixLen = affixes.charAt(ipat++);
            int32_t minDigits = affixes.charAt(ipat++);
            int32_t maxDigits = affixes.charAt(ipat++);
    
            // curs is a copy of cursor that is advanced over the
            // characters as we parse them.
            int32_t curs = cursor;
            UBool match = TRUE;
            
            for (i=0; i<prefixLen; ++i) {
                if (curs >= limit) {
                    if (i > 0) {
                        // We've already matched a character.  This is
                        // a partial match, so we return if in
                        // incremental mode.  In non-incremental mode,
                        // go to the next spec.
                        if (isIncremental) {
                            goto exit;
                        }
                        match = FALSE;
                        break;
                    }
                }
                UChar c = text.charAt(curs++);
                if (c != affixes.charAt(ipat + i)) {
                    match = FALSE;
                    break;
                }
            }
            
            if (match) {
                UChar u = 0;
                int32_t digitCount = 0;
                for (;;) {
                    if (curs >= limit) {
                        // Check for partial match in incremental mode.
                        if (curs > cursor && isIncremental) {
                            goto exit;
                        }
                        break;
                    }
                    int32_t digit = u_digit(text.charAt(curs), 16);
                    if (digit < 0) {
                        break;
                    }
                    ++curs;
                    u <<= 4;
                    u |= digit;
                    if (++digitCount == maxDigits) {
                        break;
                    }
                }

                match = (digitCount >= minDigits);
                
                if (match) {
                    for (i=0; i<suffixLen; ++i) {
                        if (curs >= limit) {
                            // Check for partial match in incremental mode.
                            if (curs > cursor && isIncremental) {
                                goto exit;
                            }
                            match = FALSE;
                            break;
                        }
                        UChar c = text.charAt(curs++);
                        if (c != affixes.charAt(ipat + prefixLen + i)) {
                            match = FALSE;
                            break;
                        }
                    }
                    
                    if (match) {
                        // This is a temporary one-character string
                        UnicodeString str(u);

                        // At this point, we have a match
                        text.handleReplaceBetween(cursor, curs, str);
                        limit -= curs - cursor - 1;
                        // The following break statement leaves the
                        // loop that is traversing the specs in
                        // affixes.  We then parse the next input
                        // character.
                        break;
                    }
                }
            }

            ipat += prefixLen + suffixLen;
        }
            
        ++cursor;
    }

  exit:
    offsets.contextLimit += limit - offsets.limit;
    offsets.limit = limit;
    offsets.start = cursor;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
