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
 * @author Alan Liu
 */
class U_I18N_API UnicodeToHexTransliterator : public Transliterator {

private:

    /**
     * ID for this transliterator.
     */
    static const char* _ID;

    static const char* DEFAULT_PREFIX;

    UnicodeString prefix;

    bool_t uppercase;

public:

    /**
     * Constructs a transliterator.
     * @param prefix the string that will precede the four hex
     * digits for UNICODE_HEX transliterators.  Ignored
     * if direction is HEX_UNICODE.
     * @param uppercase if true, the four hex digits will be
     * converted to uppercase; otherwise they will be lowercase.
     * Ignored if direction is HEX_UNICODE.
     */
    UnicodeToHexTransliterator(const UnicodeString& hexPrefix,
                               bool_t isUppercase,
                               UnicodeFilter* adoptedFilter = 0);

    /**
     * Constructs a transliterator with the default prefix "\u"
     * that outputs uppercase hex digits.
     */
    UnicodeToHexTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~UnicodeToHexTransliterator();

    /**
     * Copy constructor.
     */
    UnicodeToHexTransliterator(const UnicodeToHexTransliterator&);

    /**
     * Assignment operator.
     */
    UnicodeToHexTransliterator& operator=(const UnicodeToHexTransliterator&);

    /**
     * Transliterator API.
     */
    virtual Transliterator* clone(void) const;

    /**
     * Returns the string that precedes the four hex digits.
     * @return prefix string
     */
    virtual const UnicodeString& getPrefix(void) const;

    /**
     * Sets the string that precedes the four hex digits.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The prefix should not be changed by one
     * thread while another thread may be transliterating.
     * @param prefix prefix string
     */
    virtual void setPrefix(const UnicodeString& prefix);

    /**
     * Returns true if this transliterator outputs uppercase hex digits.
     */
    virtual bool_t isUppercase(void) const;

    /**
     * Sets if this transliterator outputs uppercase hex digits.
     *
     * <p>Callers must take care if a transliterator is in use by
     * multiple threads.  The uppercase mode should not be changed by
     * one thread while another thread may be transliterating.
     * @param outputUppercase if true, then this transliterator
     * outputs uppercase hex digits.
     */
    virtual void setUppercase(bool_t outputUppercase);

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text,
                                     int32_t offsets[3]) const;

private:

    static UChar HEX_DIGITS[32];

    /**
     * Given an integer, return its least significant hex digit.
     */
    UChar itoh(int32_t i) const;

    /**
     * Form escape sequence.
     */
    UnicodeString& toHex(UnicodeString& result, UChar c) const;
};

inline UnicodeToHexTransliterator::~UnicodeToHexTransliterator() {}

#endif
