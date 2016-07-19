// Copyright (C) 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
* Copyright (C) 2015-2016, International Business Machines
* Corporation and others.  All Rights Reserved.
*******************************************************************************
* resource.cpp
*
* created on: 2015nov04
* created by: Markus W. Scherer
*/

#include "resource.h"

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/ures.h"

U_NAMESPACE_BEGIN

ResourceValue::~ResourceValue() {}

ResourceSink::~ResourceSink() {}

ResourceTableSink::~ResourceTableSink() {}

void ResourceTableSink::put(
        const char * /*key*/, const ResourceValue & /*value*/, UErrorCode & /*errorCode*/) {}

void ResourceTableSink::putNoFallback(const char * /*key*/, UErrorCode & /*errorCode*/) {}

ResourceTableSink *ResourceTableSink::getOrCreateTableSink(
        const char * /*key*/, UErrorCode & /*errorCode*/) {
    return NULL;
}

U_NAMESPACE_END
