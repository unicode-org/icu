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
 * @version $RCSfile: hextouni.h,v $ $Revision: 1.2 $ $Date: 2000/01/18 18:27:27 $
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
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text,
                                     int32_t offsets[3]) const;
};

inline HexToUnicodeTransliterator::~HexToUnicodeTransliterator() {}

#endif
