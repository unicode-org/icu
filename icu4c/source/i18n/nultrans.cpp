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

const char* NullTransliterator::_ID = "Null";

Transliterator* NullTransliterator::clone(void) const {
    return new NullTransliterator();
}

void NullTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                             UBool isIncremental) const {
    offsets.start = offsets.limit;
}
