/*
**********************************************************************
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef HEXTOUNI_H
#define HEXTOUNI_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/translit.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator that converts from hexadecimal Unicode escape
 * sequences to the characters they represent.  For example, "U+0040"
 * and '\u0040'.  A default HexToUnicodeTransliterator recognizes the
 * prefixes "U+", "u+", "&#92;U", and "&#92;u".  Hex values may be
 * upper- or lowercase.  By calling the applyPattern() method, one
 * or more custom prefix/suffix pairs may be specified.  See
 * applyPattern() for details.
 *
 * @author Alan Liu
 * @internal Use transliterator factory methods instead since this class will be removed in that release.
 */
class U_I18N_API HexToUnicodeTransliterator : public Transliterator {

    /**
     * ID for this transliterator.
     */
    static const char _ID[];

    /**
     * The pattern used by the default constructor
     */
    static const UChar DEFAULT_PATTERN[];

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

    /**
     * The address of this static class variable serves as this class's ID
     * for ICU "poor man's RTTI".
     */
    static const char fgClassID;

public:

    /**
     * Constructs a transliterator that recognizes the standard
     * prefixes "&#92;u", "&#92;U", "u+", and "U+", each with no
     * suffix.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    HexToUnicodeTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Constructs a custom transliterator with the given pattern.
     * @see #applyPattern
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    HexToUnicodeTransliterator(const UnicodeString& pattern,
                               UErrorCode& status);

    /**
     * Constructs a custom transliterator with the given pattern
     * and filter.
     * @see #applyPattern
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    HexToUnicodeTransliterator(const UnicodeString& pattern,
                               UnicodeFilter* adoptedFilter,
                               UErrorCode& status);

    /**
     * Destructor.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    virtual ~HexToUnicodeTransliterator();

    /**
     * Copy constructor.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    HexToUnicodeTransliterator(const HexToUnicodeTransliterator&);

    /**
     * Assignment operator.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    HexToUnicodeTransliterator& operator=(const HexToUnicodeTransliterator&);

    /**
     * Transliterator API.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
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
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    void applyPattern(const UnicodeString& thePattern, UErrorCode& status);

    /**
     * Return this transliterator's pattern.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    const UnicodeString& toPattern(void) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @internal Use transliterator factory methods instead since this class will be removed in that release.
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offset,
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

inline HexToUnicodeTransliterator::~HexToUnicodeTransliterator() {}

inline UClassID
HexToUnicodeTransliterator::getStaticClassID()
{ return (UClassID)&fgClassID; }

inline UClassID
HexToUnicodeTransliterator::getDynamicClassID() const
{ return HexToUnicodeTransliterator::getStaticClassID(); }

U_NAMESPACE_END

#endif /* #if !UCONFIG_NO_TRANSLITERATION */

#endif
