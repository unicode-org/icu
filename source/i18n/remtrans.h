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

U_NAMESPACE_BEGIN

/**
 * A transliterator that removes text.
 * @author Alan Liu
 */
class U_I18N_API RemoveTransliterator : public Transliterator {

public:

    /**
     * Constructs a transliterator.
     */
    RemoveTransliterator();

    /**
     * Destructor.
     */
    virtual ~RemoveTransliterator();

    /**
     * System registration hook.
     */
    static void registerIDs();

    /**
     * Transliterator API.
     */
    Transliterator* clone(void) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offset,
                                     UBool isIncremental) const;

 private:

    /**
     * Factory method
     */
    static Transliterator* _create(const UnicodeString& ID, Token context);
};

U_NAMESPACE_END

#endif
