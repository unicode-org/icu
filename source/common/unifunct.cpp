/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/common/unifunct.cpp,v $ 
* $Date: 2002/05/29 18:33:33 $ 
* $Revision: 1.1 $
**********************************************************************
*/

#include "unicode/unifunct.h"

U_NAMESPACE_BEGIN

const char UnicodeFunctor::fgClassID = 0;

UnicodeMatcher* UnicodeFunctor::toMatcher() const {
    return 0;
}

UnicodeReplacer* UnicodeFunctor::toReplacer() const {
    return 0;
}

U_NAMESPACE_END

//eof
