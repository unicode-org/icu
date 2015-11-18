/*
*******************************************************************************
* Copyright (C) 2015, International Business Machines
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


ResourceArraySink::~ResourceArraySink() {}

void ResourceArraySink::put(
        int32_t /*index*/, ResourceValue & /*value*/, UErrorCode & /*errorCode*/) {}

ResourceArraySink *ResourceArraySink::getOrCreateArraySink(
        int32_t /*index*/, int32_t /*size*/, UErrorCode & /*errorCode*/) {
    return NULL;
}

ResourceTableSink *ResourceArraySink::getOrCreateTableSink(
        int32_t /*index*/, int32_t /*initialSize*/, UErrorCode & /*errorCode*/) {
    return NULL;
}


ResourceTableSink::~ResourceTableSink() {}

void ResourceTableSink::put(
        const char * /*key*/, ResourceValue & /*value*/, UErrorCode & /*errorCode*/) {}

void ResourceTableSink::putNoFallback(const char * /*key*/, UErrorCode & /*errorCode*/) {}

ResourceArraySink *ResourceTableSink::getOrCreateArraySink(
        const char * /*key*/, int32_t /*size*/, UErrorCode & /*errorCode*/) {
    return NULL;
}

ResourceTableSink *ResourceTableSink::getOrCreateTableSink(
        const char * /*key*/, int32_t /*initialSize*/, UErrorCode & /*errorCode*/) {
    return NULL;
}

U_NAMESPACE_END
