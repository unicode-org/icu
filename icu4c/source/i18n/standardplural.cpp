/*
 *******************************************************************************
 * Copyright (C) 2015, International Business Machines Corporation
 * and others. All Rights Reserved.
 *******************************************************************************
 * standardplural.cpp
 *
 * created on: 2015dec14
 * created by: Markus W. Scherer
 */

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "cstring.h"
#include "standardplural.h"
#include "uassert.h"

U_NAMESPACE_BEGIN

static const char *gKeywords[StandardPlural::COUNT] = {
    "zero", "one", "two", "few", "many", "other"
};

const char *StandardPlural::getKeyword(Form p) {
    U_ASSERT(ZERO <= p && p < COUNT);
    return gKeywords[p];
}

int32_t StandardPlural::indexOrNegativeFromString(const char *keyword) {
    switch (*keyword++) {
    case 'f':
        if (uprv_strcmp(keyword, "ew") == 0) {
            return FEW;
        }
        break;
    case 'm':
        if (uprv_strcmp(keyword, "any") == 0) {
            return MANY;
        }
        break;
    case 'o':
        if (uprv_strcmp(keyword, "ther") == 0) {
            return OTHER;
        } else if (uprv_strcmp(keyword, "ne") == 0) {
            return ONE;
        }
        break;
    case 't':
        if (uprv_strcmp(keyword, "wo") == 0) {
            return TWO;
        }
        break;
    case 'z':
        if (uprv_strcmp(keyword, "ero") == 0) {
            return ZERO;
        }
        break;
    default:
        break;
    }
    return -1;
}

int32_t StandardPlural::indexFromString(const char *keyword, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return OTHER; }
    int32_t i = indexOrNegativeFromString(keyword);
    if (i >= 0) {
        return i;
    } else {
        errorCode = U_ILLEGAL_ARGUMENT_ERROR;
        return OTHER;
    }
}

U_NAMESPACE_END

#endif  // !UCONFIG_NO_FORMATTING
