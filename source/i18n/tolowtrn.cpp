/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/tolowtrn.h"

U_NAMESPACE_BEGIN

const char LowercaseTransliterator::_ID[] = "Any-Lower";

/**
 * Constructs a transliterator.
 */
LowercaseTransliterator::LowercaseTransliterator(const Locale& theLoc,
                                                   UnicodeFilter* adoptedFilter) :
    TransformTransliterator(_ID, adoptedFilter),
    loc(theLoc) {
}

/**
 * Destructor.
 */
LowercaseTransliterator::~LowercaseTransliterator() {}

/**
 * Copy constructor.
 */
LowercaseTransliterator::LowercaseTransliterator(const LowercaseTransliterator& o) :
    TransformTransliterator(o),
    loc(o.loc) {}

/**
 * Assignment operator.
 */
LowercaseTransliterator& LowercaseTransliterator::operator=(
                             const LowercaseTransliterator& o) {
    TransformTransliterator::operator=(o);
    loc = o.loc;
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* LowercaseTransliterator::clone(void) const {
    return new LowercaseTransliterator(*this);
}

/**
 * TransformTransliterator framework method.
 */
UBool LowercaseTransliterator::hasTransform(UChar32 c) const {
    return c != u_tolower(c);
}

/**
 * TransformTransliterator framework method.
 */
void LowercaseTransliterator::transform(UnicodeString& s) const {
    s.toLower(loc);
}

U_NAMESPACE_END

