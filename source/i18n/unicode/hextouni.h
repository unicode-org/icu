/*
* Copyright © {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef HEXTOUNI_H
#define HEXTOUNI_H

#include "unicode/translit.h"

/**
 * A transliterator that converts from hexadecimal Unicode escape
 * sequences to the characters they represent.  For example, "U+0040"
 * and '\u0040'.  A default HexToUnicodeTransliterator recognizes the
 * prefixes "U+", "u+", "&#92;U", and "&#92;u".  Hex values may be
 * upper- or lowercase.  By calling the applyPattern() method, one
 * or more custom prefix/suffix pairs may be specified.  See
 * applyPattern() for details.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: hextouni.h,v $ $Revision: 1.7 $ $Date: 2000/06/27 19:00:38 $
 * @draft
 */
class U_I18N_API HexToUnicodeTransliterator : public Transliterator {

    /**
     * ID for this transliterator.
     */
    static const char* _ID;

    /**
     * The pattern used by the default constructor
     */
    static const UnicodeString DEFAULT_PATTERN;

    // Character constants defined here to avoid ASCII dependency
    enum {
        SEMICOLON = 0x003B, // ';'
        ZERO      = 0x0030, // '0'
        POUND     = 0x0023, // '#'
        BACKSLASH = 0x005C  // '\\'
    };

    /**
     * The pattern for this transliterator
     */
    UnicodeString pattern;

    /**
     * The processed pattern specification.  See applyPattern() for
     * details.
     */
    UnicodeString affixes;

    /**
     * The number of different affix sets in affixes.
     */
    int32_t affixCount;

public:

    /**
     * Constructs a transliterator that recognizes the standard
     * prefixes "&#92;u", "&#92;U", "u+", and "U+", each with no
     * suffix.
     * @draft
     */
    HexToUnicodeTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Constructs a custom transliterator with the given pattern.
     * @see #applyPattern
     */
    HexToUnicodeTransliterator(const UnicodeString& pattern,
                               UErrorCode& status);

    /**
     * Constructs a custom transliterator with the given pattern
     * and filter.
     * @see #applyPattern
     */
    HexToUnicodeTransliterator(const UnicodeString& pattern,
                               UnicodeFilter* adoptedFilter,
                               UErrorCode& status);

    /**
     * Destructor.
     * @draft
     */
    virtual ~HexToUnicodeTransliterator();

    /**
     * Copy constructor.
     * @draft
     */
    HexToUnicodeTransliterator(const HexToUnicodeTransliterator&);

    /**
     * Assignment operator.
     * @draft
     */
    HexToUnicodeTransliterator& operator=(const HexToUnicodeTransliterator&);

    /**
     * Transliterator API.
     * @draft
     */
    Transliterator* clone(void) const;

    /**
     * Set the patterns recognized by this transliterator.  One or
     * more patterns may be specified, separated by semicolons (';').
     * Each pattern contains zero or more prefix characters, one or
     * more digit characters, and zero or more suffix characters.  The
     * digit characters indicates optional digits ('#') followed by
     * required digits ('0').  The total number of digits cannot
     * exceed 4, and must be at least 1 required digit.  Use a
     * backslash ('\\') to escape any of the special characters.  An
     * empty pattern is allowed; it specifies a transliterator that
     * does nothing.
     *
     * <p>Example: "U+0000;<###0>" specifies two patterns.  The first
     * has a prefix of "U+", exactly four digits, and no suffix.  The
     * second has a prefix of "<", between one and four digits, and a
     * suffix of ">".
     *
     * <p><pre>
     * pattern := spec | ( pattern ';' spec )
     * spec := prefix-char* digit-spec suffix-char*
     * digit-spec := '#'* '0'+
     * prefix-char := [^special-char] | '\\' special-char
     * suffix-char := [^special-char] | '\\' special-char
     * special-char := ';' | '0' | '#' | '\\'
     * </pre>
     */
    void applyPattern(const UnicodeString& thePattern, UErrorCode& status);

    /**
     * Return this transliterator's pattern.
     */
    const UnicodeString& toPattern(void) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @draft
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offset,
                                     UBool isIncremental) const;
};

inline HexToUnicodeTransliterator::~HexToUnicodeTransliterator() {}

#endif
