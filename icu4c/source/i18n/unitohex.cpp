/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "unitohex.h"
#include "rep.h"
#include "unifilt.h"

/**
 * ID for this transliterator.
 */
const char* UnicodeToHexTransliterator::_ID = "Unicode-Hex";

const char* UnicodeToHexTransliterator::DEFAULT_PREFIX = "\\u";

/**
 * Constructs a transliterator.
 * @param prefix the string that will precede the four hex
 * digits for UNICODE_HEX transliterators.  Ignored
 * if direction is HEX_UNICODE.
 * @param uppercase if true, the four hex digits will be
 * converted to uppercase; otherwise they will be lowercase.
 * Ignored if direction is HEX_UNICODE.
 */
UnicodeToHexTransliterator::UnicodeToHexTransliterator(
                                const UnicodeString& hexPrefix,
                                bool_t isUppercase,
                                UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter),
    prefix(hexPrefix),
    uppercase(isUppercase) {
}

/**
 * Constructs a transliterator with the default prefix "&#092;u"
 * that outputs uppercase hex digits.
 */
UnicodeToHexTransliterator::UnicodeToHexTransliterator(
                                UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter),
    prefix(DEFAULT_PREFIX),
    uppercase(TRUE) {
}

/**
 * Copy constructor.
 */
UnicodeToHexTransliterator::UnicodeToHexTransliterator(
                                const UnicodeToHexTransliterator& other) :
    Transliterator(other), prefix(other.prefix),
    uppercase(other.uppercase) {
}

/**
 * Assignment operator.
 */
UnicodeToHexTransliterator&
UnicodeToHexTransliterator::operator=(const UnicodeToHexTransliterator& other) {
    Transliterator::operator=(other);
    prefix = other.prefix;
    uppercase = other.uppercase;
    return *this;
}

Transliterator*
UnicodeToHexTransliterator::clone() const {
    return new UnicodeToHexTransliterator(*this);
}

/**
 * Returns the string that precedes the four hex digits.
 * @return prefix string
 */
const UnicodeString& UnicodeToHexTransliterator::getPrefix() const {
    return prefix;
}

/**
 * Sets the string that precedes the four hex digits.
 *
 * <p>Callers must take care if a transliterator is in use by
 * multiple threads.  The prefix should not be changed by one
 * thread while another thread may be transliterating.
 * @param prefix prefix string
 */
void UnicodeToHexTransliterator::setPrefix(const UnicodeString& hexPrefix) {
    prefix = hexPrefix;
}

/**
 * Returns true if this transliterator outputs uppercase hex digits.
 */
bool_t UnicodeToHexTransliterator::isUppercase() const {
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
void UnicodeToHexTransliterator::setUppercase(bool_t outputUppercase) {
    uppercase = outputUppercase;
}

/**
 * Transliterates a segment of a string.  <code>Transliterator</code> API.
 * @param text the string to be transliterated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @return the new limit index
 */
int32_t UnicodeToHexTransliterator::transliterate(Replaceable& text,
                                                  int32_t start,
                                                  int32_t limit) const {
    int32_t offsets[3] = { start, limit, start };
    handleKeyboardTransliterate(text, offsets);
    return offsets[LIMIT];
}

/**
 * Implements {@link Transliterator#handleKeyboardTransliterate}.
 */
void UnicodeToHexTransliterator::handleKeyboardTransliterate(Replaceable& text,
                                     int32_t offsets[3]) const {
    /**
     * Performs transliteration changing all characters to
     * Unicode hexadecimal escapes.  For example, '@' -> "U+0040",
     * assuming the prefix is "U+". 
     */
    int32_t cursor = offsets[CURSOR];
    int32_t limit = offsets[LIMIT];

    const UnicodeFilter* filter = getFilter();
    UnicodeString hex;

    while (cursor < limit) {
        UChar c = text.charAt(cursor);
        if (filter != 0 && !filter->isIn(c)) {
            ++cursor;
            continue;
        }
        toHex(hex, c);
        text.handleReplaceBetween(cursor, cursor+1, hex);
        int32_t len = hex.length();
        cursor += len; // Advance cursor by 1 and adjust for new text
        --len;
        limit += len;
    }

    offsets[LIMIT] = limit;
    offsets[CURSOR] = cursor;
}

/**
 * Return the length of the longest context required by this transliterator.
 * This is <em>preceding</em> context.
 * @param direction either <code>FORWARD</code> or <code>REVERSE</code>
 * @return maximum number of preceding context characters this
 * transliterator needs to examine
 */
int32_t UnicodeToHexTransliterator::getMaximumContextLength() {
    return 0;
}

UChar UnicodeToHexTransliterator::HEX_DIGITS[32] = {
    // If necessary, replace these character constants with their hex values
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'a', 'b', 'c', 'd', 'e', 'f',
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
};

/**
 * Given an integer, return its least significant hex digit.
 */
UChar UnicodeToHexTransliterator::itoh(int32_t i) const {
    i &= 0xF;
    return HEX_DIGITS[uppercase ? (i|16) : i];
}

/**
 * Form escape sequence.
 */
UnicodeString& UnicodeToHexTransliterator::toHex(UnicodeString& result,
                                                 UChar c) const {
    result = prefix;
    result.append(itoh(c >> 12));
    result.append(itoh(c >> 8));
    result.append(itoh(c >> 4));
    result.append(itoh(c));
    return result;
}
