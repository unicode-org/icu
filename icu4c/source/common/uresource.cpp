/*
*******************************************************************************
* Copyright (C) 2015, International Business Machines
* Corporation and others.  All Rights Reserved.
*******************************************************************************
* uresource.cpp
*
* created on: 2015nov04
* created by: Markus W. Scherer
*/

#include "uresource.h"

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/ures.h"

U_NAMESPACE_BEGIN

UResourceValue::~UResourceValue() {}


UResourceArraySink::~UResourceArraySink() {}

void UResourceArraySink::put(
        int32_t /*index*/, UResourceValue & /*value*/, UErrorCode & /*errorCode*/) {}

UResourceArraySink *UResourceArraySink::getOrCreateArraySink(
        int32_t /*index*/, int32_t /*size*/, UErrorCode & /*errorCode*/) {
    return NULL;
}

UResourceTableSink *UResourceArraySink::getOrCreateTableSink(
        int32_t /*index*/, int32_t /*initialSize*/, UErrorCode & /*errorCode*/) {
    return NULL;
}


UResourceTableSink::~UResourceTableSink() {}

void UResourceTableSink::put(
        const char * /*key*/, UResourceValue & /*value*/, UErrorCode & /*errorCode*/) {}

void UResourceTableSink::putNoFallback(const char * /*key*/, UErrorCode & /*errorCode*/) {}

UResourceArraySink *UResourceTableSink::getOrCreateArraySink(
        const char * /*key*/, int32_t /*size*/, UErrorCode & /*errorCode*/) {
    return NULL;
}

UResourceTableSink *UResourceTableSink::getOrCreateTableSink(
        const char * /*key*/, int32_t /*initialSize*/, UErrorCode & /*errorCode*/) {
    return NULL;
}

U_NAMESPACE_END
