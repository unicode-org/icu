/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/toupptrn.h"

U_NAMESPACE_BEGIN

const char* UppercaseTransliterator::_ID = "Any-Upper";

/**
 * Constructs a transliterator.
 */
UppercaseTransliterator::UppercaseTransliterator(const Locale& theLoc,
                                                   UnicodeFilter* adoptedFilter) :
    TransformTransliterator(_ID, adoptedFilter),
    loc(theLoc) {
}

/**
 * Destructor.
 */
UppercaseTransliterator::~UppercaseTransliterator() {}

/**
 * Copy constructor.
 */
UppercaseTransliterator::UppercaseTransliterator(const UppercaseTransliterator& o) :
    TransformTransliterator(o),
    loc(o.loc) {}

/**
 * Assignment operator.
 */
UppercaseTransliterator& UppercaseTransliterator::operator=(
                             const UppercaseTransliterator& o) {
    TransformTransliterator::operator=(o);
    loc = o.loc;
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* UppercaseTransliterator::clone(void) const {
    return new UppercaseTransliterator(*this);
}

/**
 * TransformTransliterator framework method.
 */
UBool UppercaseTransliterator::hasTransform(UChar32 c) const {
    return c != u_toupper(c);
}

/**
 * TransformTransliterator framework method.
 */
void UppercaseTransliterator::transform(UnicodeString& s) const {
    s.toUpper(loc);
}

U_NAMESPACE_END

