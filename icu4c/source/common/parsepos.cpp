/*
**********************************************************************
*   Copyright (C) 2016 and later: Unicode, Inc. and others.
*   License & terms of use: http://www.unicode.org/copyright.html
**********************************************************************
*/

#include "unicode/parsepos.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(ParsePosition)

ParsePosition::~ParsePosition() {}

ParsePosition *
ParsePosition::clone() const {
    return new ParsePosition(*this);
}

U_NAMESPACE_END
