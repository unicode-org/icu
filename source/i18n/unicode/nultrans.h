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
 * @deprecated To be removed after 2002-sep-30; use Transliterator::createInstance factory method
 */
class U_I18N_API NullTransliterator : public Transliterator {

public:

    /**
     * ID for this transliterator.
     * @deprecated To be removed after 2002-sep-30.
     */
    static const UChar ID[]; // public for Transliterator

    /**
     * ID for this transliterator.
     * @deprecated To be removed after 2002-sep-30.
     */
    static const UChar SHORT_ID[]; // public for Transliterator

    /**
     * Constructs a transliterator.
     * @deprecated To be removed after 2002-sep-30.
     */
    NullTransliterator();

    /**
     * Destructor.
     * @deprecated To be removed after 2002-sep-30.
     */
    virtual ~NullTransliterator();

    /**
     * Transliterator API.
     * @deprecated To be removed after 2002-sep-30.
     */
    Transliterator* clone(void) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @deprecated To be removed after 2002-sep-30.
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offset,
                                     UBool isIncremental) const;
};

inline NullTransliterator::NullTransliterator() : Transliterator(ID, 0) {}

inline NullTransliterator::~NullTransliterator() {}

U_NAMESPACE_END

#endif
