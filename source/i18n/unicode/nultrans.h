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

U_NAMESPACE_BEGIN

/**
 * A transliterator that leaves text unchanged.
 * @author Alan Liu
 */
class U_I18N_API NullTransliterator : public Transliterator {

public:

    /**
     * ID for this transliterator.
     */
    static const UChar ID[]; // public for Transliterator

    /**
     * ID for this transliterator.
     */
    static const UChar SHORT_ID[]; // public for Transliterator

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
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offset,
                                     UBool isIncremental) const;
};

inline NullTransliterator::NullTransliterator() : Transliterator(ID, 0) {}

inline NullTransliterator::~NullTransliterator() {}

U_NAMESPACE_END

#endif
