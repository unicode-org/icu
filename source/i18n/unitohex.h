/*
**********************************************************************
* Copyright (C) 1999-2003, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef UNITOHEX_H
#define UNITOHEX_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

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
 * @internal Use transliterator factory methods instead since this class will be removed in that release.
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
    static const char _ID[];

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
    UBool uppercase;

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;

public:

    /**
     * Constructs a transliterator.
     * @param pattern The pattern for this transliterator.  See
     * applyPattern() for pattern syntax.
     * @param isUppercase if true, the four hex digits will be
     * converted to uppercase; otherwise they will be lowercase.
     * @param adoptedFilter the filter for this transliterator, or
     * NULL if none.  Adopted by this transliterator.
     * @param status Error code indicating success or failure
     * to parse pattern.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    UnicodeToHexTransliterator(const UnicodeString& pattern,
                               UBool isUppercase,
                               UnicodeFilter* adoptedFilter,
                               UErrorCode& status);

    /**
     * Constructs an uppercase transliterator with no filter.
     * @param pattern The pattern for this transliterator.  See
     * applyPattern() for pattern syntax.
     * @param status Error code indicating success or failure
     * to parse pattern.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    UnicodeToHexTransliterator(const UnicodeString& pattern,
                               UErrorCode& status);

    /**
     * Constructs a transliterator with the default prefix "\u"
     * that outputs uppercase hex digits.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    UnicodeToHexTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    virtual ~UnicodeToHexTransliterator();

    /**
     * Copy constructor.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    UnicodeToHexTransliterator(const UnicodeToHexTransliterator&);

    /**
     * Assignment operator.
     * @stable ICU 2.0
     */
    UnicodeToHexTransliterator& operator=(const UnicodeToHexTransliterator&);

    /**
     * Transliterator API.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
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
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    void applyPattern(const UnicodeString& thePattern, UErrorCode& status);

    /**
     * Return this transliterator's pattern.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    const UnicodeString& toPattern(void) const;

    /**
     * Returns true if this transliterator outputs uppercase hex digits.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    virtual UBool isUppercase(void) const;

    /**
     * Sets if this transliterator outputs uppercase hex digits.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    virtual void setUppercase(UBool outputUppercase);

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                     UBool isIncremental) const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @draft ICU 2.2
     */
    virtual inline UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @draft ICU 2.2
     */
    static inline UClassID getStaticClassID();
};

inline UnicodeToHexTransliterator::~UnicodeToHexTransliterator() {}

inline UClassID
UnicodeToHexTransliterator::getStaticClassID()
{ return (UClassID)&fgClassID; }

inline UClassID
UnicodeToHexTransliterator::getDynamicClassID() const
{ return UnicodeToHexTransliterator::getStaticClassID(); }

U_NAMESPACE_END
#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
