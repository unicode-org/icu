/*
**********************************************************************
*   Copyright (c) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   01/11/2000  aliu        Creation.
**********************************************************************
*/
#ifndef NULTRANS_H
#define NULTRANS_H

#include "unicode/translit.h"

/**
 * A transliterator that leaves text unchanged.
 * @author Alan Liu
 */
class U_I18N_API NullTransliterator : public Transliterator {

    /**
     * ID for this transliterator.
     */
    static const char* _ID;

public:

    /**
     * Constructs a transliterator.
     */
    NullTransliterator();

    /**
     * Destructor.
     */
    virtual ~NullTransliterator();

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
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text,
                                     int32_t offsets[3]) const;
};

inline NullTransliterator::NullTransliterator() : Transliterator(_ID, 0) {}

inline NullTransliterator::~NullTransliterator() {}

#endif
