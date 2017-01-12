// Copyright (C) 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  ustrcase_locale.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2011may31
*   created by: Markus W. Scherer
*
*   Locale-sensitive case mapping functions (ones that call uloc_getDefault())
*   were moved here to break dependency cycles among parts of the common library.
*/

#include "unicode/utypes.h"
#include "uassert.h"
#include "unicode/brkiter.h"
#include "unicode/ucasemap.h"
#include "unicode/uloc.h"
#include "unicode/ustring.h"
#include "ucase.h"
#include "ustr_imp.h"

U_NAMESPACE_BEGIN

// TODO: new casemap_locale.cpp

CaseMap::CaseMap(const Locale &loc, int32_t caseLoc, uint32_t opts, UErrorCode & /*errorCode*/) :
        caseLocale(caseLoc), options(opts), locale(loc)
#if !UCONFIG_NO_BREAK_ITERATION
        , iter(NULL)
#endif
        {
    if (caseLoc == 0) {  // UCASE_LOC_UNKNOWN
        setCaseLocale(locale.getBaseName());
    }
}

CaseMap::CaseMap(const Locale &locale, uint32_t options, UErrorCode &errorCode) :
        CaseMap(locale, /* UCASE_LOC_UNKNOWN = */ 0, options, errorCode) {}

// small optimization for localeID=="", a little slower otherwise
CaseMap::CaseMap(const char *localeID, uint32_t options, UErrorCode &errorCode) :
        CaseMap(Locale::getRoot(), /* UCASE_LOC_ROOT = */ 1, options, errorCode) {
    if (localeID == NULL || *localeID != 0) {
        setLocale(localeID, errorCode);  // not root
    }
}

CaseMap::~CaseMap() {
#if !UCONFIG_NO_BREAK_ITERATION
    delete iter;
#endif
}

void CaseMap::setCaseLocale(const char *localeID) {
    U_ASSERT(localeID != NULL);
    caseLocale = UCASE_LOC_UNKNOWN;
    ucase_getCaseLocale(localeID, &caseLocale);
}

void CaseMap::setLocale(const char *localeID, UErrorCode &errorCode) {
    if (U_FAILURE(errorCode)) { return; }
    if (localeID == NULL) {
        locale = Locale::getDefault();
        localeID = locale.getBaseName();
    } else {
        locale = Locale(localeID);
        if (locale.isBogus()) {
            errorCode = U_MEMORY_ALLOCATION_ERROR;
            localeID = "";
        }
    }
    setCaseLocale(localeID);
}

U_NAMESPACE_END

U_NAMESPACE_USE

/* public API functions */

U_CAPI int32_t U_EXPORT2
u_strToLower(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             const char *locale,
             UErrorCode *pErrorCode) {
    CaseMap csm(locale, 0, *pErrorCode);
    return ustrcase_mapWithOverlap(
        csm, UCASEMAP_BREAK_ITERATOR_NULL
        dest, destCapacity,
        src, srcLength,
        ustrcase_internalToLower, *pErrorCode);
}

U_CAPI int32_t U_EXPORT2
u_strToUpper(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             const char *locale,
             UErrorCode *pErrorCode) {
    CaseMap csm(locale, 0, *pErrorCode);
    return ustrcase_mapWithOverlap(
        csm, UCASEMAP_BREAK_ITERATOR_NULL
        dest, destCapacity,
        src, srcLength,
        ustrcase_internalToUpper, *pErrorCode);
}
