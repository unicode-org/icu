/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/
#ifndef TOUPPTRN_H
#define TOUPPTRN_H

#include "unicode/xformtrn.h"
#include "unicode/locid.h"

/**
 * A transliterator that performs locale-sensitive toUpper()
 * case mapping.
 * @author Alan Liu
 */
class U_I18N_API UppercaseTransliterator : public TransformTransliterator {

 public:

    /**
     * Constructs a transliterator.
     */
    UppercaseTransliterator(const Locale& loc = Locale::getDefault(),
                             UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~UppercaseTransliterator();

    /**
     * Copy constructor.
     */
    UppercaseTransliterator(const UppercaseTransliterator&);

    /**
     * Assignment operator.
     */
    UppercaseTransliterator& operator=(const UppercaseTransliterator&);

    /**
     * Transliterator API.
     */
    Transliterator* clone(void) const;

 protected:

    /**
     * TransformTransliterator framework method.
     */
    virtual UBool hasTransform(UChar32 c) const;

    /**
     * TransformTransliterator framework method.
     */
    virtual void transform(UnicodeString& s) const;

 private:

    Locale loc;

    static const char* _ID;
};

#endif
