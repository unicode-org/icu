/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/
#ifndef TOLOWTRN_H
#define TOLOWTRN_H

#include "unicode/xformtrn.h"
#include "unicode/locid.h"

U_NAMESPACE_BEGIN

/**
 * A transliterator that performs locale-sensitive toLower()
 * case mapping.
 * @author Alan Liu
 */
class U_I18N_API LowercaseTransliterator : public TransformTransliterator {

 public:

    /**
     * Constructs a transliterator.
     */
    LowercaseTransliterator(const Locale& loc = Locale::getDefault(),
                             UnicodeFilter* adoptedFilter = 0);

    /**
     * Destructor.
     */
    virtual ~LowercaseTransliterator();

    /**
     * Copy constructor.
     */
    LowercaseTransliterator(const LowercaseTransliterator&);

    /**
     * Assignment operator.
     */
    LowercaseTransliterator& operator=(const LowercaseTransliterator&);

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

    static const char _ID[];
};

U_NAMESPACE_END

#endif
