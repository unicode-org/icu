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
#include "unitohex.h"

U_NAMESPACE_BEGIN

const char UnicodeToHexTransliterator::fgClassID=0;

/**
 * ID for this transliterator.
 */
const char UnicodeToHexTransliterator::_ID[] = "Any-Hex";

const UChar UnicodeToHexTransliterator::HEX_DIGITS[32] = {
    // Use Unicode hex values for EBCDIC compatibility
    0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, // 01234567
    0x38, 0x39, 0x61, 0x62, 0x63, 0x64, 0x65, 0x66, // 89abcdef
    0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, // 01234567
    0x38, 0x39, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, // 89ABCDEF
};

/**
 * Constructs a transliterator.
 */
UnicodeToHexTransliterator::UnicodeToHexTransliterator(
                                const UnicodeString& thePattern,
                                UBool isUppercase,
                                UnicodeFilter* adoptedFilter,
                                UErrorCode& status) :
    Transliterator(_ID, adoptedFilter),
    uppercase(isUppercase) {

    if (U_FAILURE(status)) {
        return;
    }
    applyPattern(thePattern, status);
}

/**
 * Constructs a transliterator.
 */
UnicodeToHexTransliterator::UnicodeToHexTransliterator(
                                const UnicodeString& thePattern,
                                UErrorCode& status) :
    Transliterator(_ID, 0),
    uppercase(TRUE) {

    if (U_FAILURE(status)) {
        return;
    }
    applyPattern(thePattern, status);
}

/**
 * Constructs a transliterator with the default prefix "&#092;u"
 * that outputs four uppercase hex digits.
 */
UnicodeToHexTransliterator::UnicodeToHexTransliterator(
                                UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter),
    pattern("\\\\u0000", ""),
    prefix("\\u", 2, ""),
    suffix(),
    minDigits(4),
    uppercase(TRUE) {
}

/**
 * Copy constructor.
 */
UnicodeToHexTransliterator::UnicodeToHexTransliterator(
                                const UnicodeToHexTransliterator& other) :
    Transliterator(other),
    pattern(other.pattern),
    prefix(other.prefix),
    suffix(other.suffix),
    minDigits(other.minDigits),
    uppercase(other.uppercase) {
}

/**
 * Assignment operator.
 */
UnicodeToHexTransliterator&
UnicodeToHexTransliterator::operator=(const UnicodeToHexTransliterator& other) {
    Transliterator::operator=(other);
    pattern = other.pattern;
    prefix = other.prefix;
    suffix = other.suffix;
    minDigits = other.minDigits;
    uppercase = other.uppercase;
    return *this;
}

Transliterator*
UnicodeToHexTransliterator::clone(void) const {
    return new UnicodeToHexTransliterator(*this);
}

void UnicodeToHexTransliterator::applyPattern(const UnicodeString& thePattern,
                                              UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }

    // POSSIBILE FUTURE MODIFICATION
    // Parse thePattern, and if this succeeds, set pattern to thePattern.
    // If it fails, call applyPattern(pattern) to restore the original
    // conditions.

    pattern = thePattern;
    prefix.truncate(0);
    suffix.truncate(0);
    minDigits = 0;
    int32_t maxDigits = 0;

    /* The mode specifies where we are in each spec.
     * mode 0 = in prefix
     * mode 1 = in optional digits (#)
     * mode 2 = in required digits (0)
     * mode 3 = in suffix
     */
    int32_t mode = 0;

    for (int32_t i=0; i<pattern.length(); ++i) {
        UChar c = pattern.charAt(i);
        UBool isLiteral = FALSE;
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
            default:
                isLiteral = TRUE;
                break;
            }
        }

        if (isLiteral) {
            if (mode == 0) {
                prefix.append(c);
            } else {
                // Any literal outside the prefix moves us into mode 3
                // (suffix)
                mode = 3;
                suffix.append(c);
            }
        }
    }

    if (minDigits < 1 || maxDigits > 4) {
        // Invalid min/max digit count
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
}

const UnicodeString& UnicodeToHexTransliterator::toPattern(void) const {
    return pattern;
}

/**
 * Returns true if this transliterator outputs uppercase hex digits.
 */
UBool UnicodeToHexTransliterator::isUppercase(void) const {
    return uppercase;
}

/**
 * Sets if this transliterator outputs uppercase hex digits.
 *
 * <p>Callers must take care if a transliterator is in use by
 * multiple threads.  The uppercase mode should not be changed by
 * one thread while another thread may be transliterating.
 * @param outputUppercase if true, then this transliterator
 * outputs uppercase hex digits.
 */
void UnicodeToHexTransliterator::setUppercase(UBool outputUppercase) {
    uppercase = outputUppercase;
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void UnicodeToHexTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                                     UBool /*isIncremental*/) const {
    /**
     * Performs transliteration changing all characters to
     * Unicode hexadecimal escapes.  For example, '@' -> "U+0040",
     * assuming the prefix is "U+". 
     */
    int32_t cursor = offsets.start;
    int32_t limit = offsets.limit;

    UnicodeString hex;

    while (cursor < limit) {
        UChar c = text.charAt(cursor);

        hex = prefix;
        UBool showRest = FALSE;
        for (int32_t i=3; i>=0; --i) {
            /* Get each nibble from left to right */
            int32_t d = (c >> (i<<2)) & 0xF;
            if (showRest || (d != 0) || minDigits > i) {
                hex.append(HEX_DIGITS[uppercase ? (d|16) : d]);
                showRest = TRUE;
            }
        }
        hex.append(suffix);

        text.handleReplaceBetween(cursor, cursor+1, hex);
        int32_t len = hex.length();
        cursor += len; // Advance cursor by 1 and adjust for new text
        --len;
        limit += len;
    }

    offsets.contextLimit += limit - offsets.limit;
    offsets.limit = limit;
    offsets.start = cursor;
}

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
