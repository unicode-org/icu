/*
**********************************************************************
*   Copyright (c) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   04/02/2001  aliu        Creation.
**********************************************************************
*/
#ifndef REMTRANS_H
#define REMTRANS_H

#include "unicode/translit.h"

/**
 * A transliterator that removes text.
 * @author Alan Liu
 */
class U_I18N_API RemoveTransliterator : public Transliterator {

public:

    /**
     * ID for this transliterator.
     */
    static const UChar ID[]; // public for Transliterator

    /**
     * Constructs a transliterator.
     */
    RemoveTransliterator();

    /**
     * Destructor.
     */
    virtual ~RemoveTransliterator();

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

inline RemoveTransliterator::RemoveTransliterator() : Transliterator(ID, 0) {}

inline RemoveTransliterator::~RemoveTransliterator() {}

#endif
