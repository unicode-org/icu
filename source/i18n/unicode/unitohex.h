/*
**********************************************************************
* Copyright © {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef UNITOHEX_H
#define UNITOHEX_H

#include "unicode/translit.h"
#include "unicode/unistr.h"

class UnicodeFilter;

/**
 * A transliterator that converts from Unicode characters to 
 * hexadecimal Unicode escape sequences.  It outputs a
 * prefix specified in the constructor and optionally converts the hex
 * digits to uppercase.
 *
 * <p>The format of the output is set by a pattern.  This pattern
 * follows the same syntax as <code>HexToUnicodeTransliterator</code>,
 * except it does not allow multiple specifications.  The pattern sets
 * the prefix string, suffix string, and minimum and maximum digit
 * count.  There are no setters or getters for these attributes; they
 * are set only through the pattern.
 *
 * <p>The setUppercase() and isUppercase() methods control whether 'a'
 * through 'f' or 'A' through 'F' are output as hex digits.  This is
 * not controlled through the pattern; only through the methods.  The
 * default is uppercase.
 *
 * @author Alan Liu
 */
class U_I18N_API UnicodeToHexTransliterator : public Transliterator {

private:

    // Character constants defined here to avoid ASCII dependency
    enum {
        ZERO      = 0x0030, // '0'
        POUND     = 0x0023, // '#'
        BACKSLASH = 0x005C  // '\\'
    };

    static const UChar HEX_DIGITS[32];

    /**
     * ID for this transliterator.
     */
    static const char* _ID;

    /**
     * The pattern set by applyPattern() and returned by toPattern().
     */
    UnicodeString pattern;

    /**
     * The string preceding the hex digits, parsed from the pattern.
     */
    UnicodeString prefix;

    /**
     * The string following the hex digits, parsed from the pattern.
     */
    UnicodeString suffix;

    /**
     * The minimum number of hex digits to output, between 1 and 4,
     * inclusive.  Parsed from the pattern.
     */
    int8_t minDigits;

    /**
     * If TRUE, output uppercase hex digits; otherwise output
     * lowercase.  Set by setUppercase() and returned by isUppercase().
     */
    bool_t uppercase;

public:

    /**
     * Constructs a transliterator.
     * @param pattern The pattern for this transliterator.  See
     * applyPattern() for pattern syntax.
     * @param uppercase if true, the four hex digits will be
     * converted to uppercase; otherwise they will be lowercase.
     * @param adoptedFilter the filter for this transliterator, or
     * NULL if none.  Adopted by this transliterator.
     * @param status Error code indicating success or failure
     * to parse pattern.
     * @stable
     */
    UnicodeToHexTransliterator(const UnicodeString& pattern,
                               bool_t isUppercase,
                               UnicodeFilter* adoptedFilter,
                               UErrorCode& status);

    /**
     * Constructs an uppercase transliterator with no filter.
     * @param pattern The pattern for this transliterator.  See
     * applyPattern() for pattern syntax.
     * @param status Error code indicating success or failure
     * to parse pattern.
     */
    UnicodeToHexTransliterator(const UnicodeString& pattern,
                               UErrorCode& status);

    /**
     * Constructs a transliterator with the default prefix "\u"
     * that outputs uppercase hex digits.
     * @stable
     */
    UnicodeToHexTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     * @stable
     */
    virtual ~UnicodeToHexTransliterator();

    /**
     * Copy constructor.
     * @stable
     */
    UnicodeToHexTransliterator(const UnicodeToHexTransliterator&);

    /**
     * Assignment operator.
     * @stable
     */
    UnicodeToHexTransliterator& operator=(const UnicodeToHexTransliterator&);

    /**
     * Transliterator API.
     * @stable
     */
    virtual Transliterator* clone(void) const;

    /**
     * Set the pattern recognized by this transliterator.  The pattern
     * must contain zero or more prefix characters, one or more digit
     * characters, and zero or more suffix characters.  The digit
     * characters indicates optional digits ('#') followed by required
     * digits ('0').  The total number of digits cannot exceed 4, and
     * must be at least 1 required digit.  Use a backslash ('\\') to
     * escape any of the special characters.  An empty pattern is not
     * allowed.
     *
     * <p>Example: "U+0000" specifies a prefix of "U+", exactly four
     * digits, and no suffix.  "<###0>" has a prefix of "<", between
     * one and four digits, and a suffix of ">".
     *
     * <p><pre>
     * pattern := prefix-char* digit-spec suffix-char*
     * digit-spec := '#'* '0'+
     * prefix-char := [^special-char] | '\\' special-char
     * suffix-char := [^special-char] | '\\' special-char
     * special-char := ';' | '0' | '#' | '\\'
     * </pre>
     *
     * <p>Limitations: There is no way to set the uppercase attribute
     * in the pattern.  (applyPattern() does not alter the uppercase
     * attribute.)
     * @stable
     */
    void applyPattern(const UnicodeString& thePattern, UErrorCode& status);

    /**
     * Return this transliterator's pattern.
     * @stable
     */
    const UnicodeString& toPattern(void) const;

    /**
     * Returns true if this transliterator outputs uppercase hex digits.
     * @stable
     */
    virtual bool_t isUppercase(void) const;

    /**
     * Sets if this transliterator outputs uppercase hex digits.
     * @stable
     */
    virtual void setUppercase(bool_t outputUppercase);

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @draft
     */
    virtual void handleTransliterate(Replaceable& text, Position& offsets,
                                     bool_t isIncremental) const;
};

inline UnicodeToHexTransliterator::~UnicodeToHexTransliterator() {}

#endif
