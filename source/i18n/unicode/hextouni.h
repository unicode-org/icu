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
 * A transliterator that converts from hexadecimal Unicode
 * escape sequences to the characters they represent.  For example, "U+0040"
 * and '\u0040'.  It recognizes the
 * prefixes "U+", "u+", "&#92;U", and "&#92;u".  Hex values may be
 * upper- or lowercase.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: hextouni.h,v $ $Revision: 1.1 $ $Date: 1999/12/28 23:54:20 $
 */
class U_I18N_API HexToUnicodeTransliterator : public Transliterator {

    /**
     * ID for this transliterator.
     */
    static const char* _ID;

public:

    /**
     * Constructs a transliterator.
     */
    HexToUnicodeTransliterator(UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~HexToUnicodeTransliterator();

    /**
     * Copy constructor.
     */
    HexToUnicodeTransliterator(const HexToUnicodeTransliterator&);

    /**
     * Assignment operator.
     */
    HexToUnicodeTransliterator& operator=(const HexToUnicodeTransliterator&);

    /**
     * Transliterator API.
     */
    Transliterator* clone(void) const;

    /**
     * Transliterates a segment of a string.  <code>Transliterator</code> API.
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @return the new limit index
     */
    virtual int32_t transliterate(Replaceable &text,
                                  int32_t start, int32_t limit) const;

    /**
     * Implements {@link Transliterator#handleKeyboardTransliterate}.
     */
    virtual void handleKeyboardTransliterate(Replaceable& text,
                                             int32_t offsets[3]) const;

    /**
     * Return the length of the longest context required by this transliterator.
     * This is <em>preceding</em> context.
     * @param direction either <code>FORWARD</code> or <code>REVERSE</code>
     * @return maximum number of preceding context characters this
     * transliterator needs to examine
     */
    virtual int32_t getMaximumContextLength(void) const;

private:

    UChar filteredCharAt(Replaceable& text, int32_t i) const;
};

inline HexToUnicodeTransliterator::~HexToUnicodeTransliterator() {}

#endif
