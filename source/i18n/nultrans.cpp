/*
**********************************************************************
*   Copyright (c) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   01/11/2000  aliu        Creation.
**********************************************************************
*/
#include "unicode/nultrans.h"

U_NAMESPACE_BEGIN


// "Any-Null"
const UChar NullTransliterator::ID[] = {65,110,121,45,0x4E, 0x75, 0x6C, 0x6C, 0x00};

// "Null"
const UChar NullTransliterator::SHORT_ID[] = {0x4E, 0x75, 0x6C, 0x6C, 0x00};

Transliterator* NullTransliterator::clone(void) const {
    return new NullTransliterator();
}

void NullTransliterator::handleTransliterate(Replaceable& /*text*/, UTransPosition& offsets,
                                             UBool /*isIncremental*/) const {
    offsets.start = offsets.limit;
}

U_NAMESPACE_END

