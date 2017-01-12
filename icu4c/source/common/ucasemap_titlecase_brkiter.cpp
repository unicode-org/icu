// Copyright (C) 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  ucasemap_titlecase_brkiter.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2011jun02
*   created by: Markus W. Scherer
*
*   Titlecasing functions that are based on BreakIterator
*   were moved here to break dependency cycles among parts of the common library.
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/brkiter.h"
#include "unicode/ubrk.h"
#include "unicode/ucasemap.h"
#include "cmemory.h"
#include "ucase.h"
#include "ustr_imp.h"

U_NAMESPACE_USE

using icu::internal::CaseMapFriend;

U_CAPI const UBreakIterator * U_EXPORT2
ucasemap_getBreakIterator(const UCaseMap *csm) {
    return reinterpret_cast<const UBreakIterator *>(
        CaseMapFriend::iter(*CaseMapFriend::fromUCaseMap(csm)));
}

U_CAPI void U_EXPORT2
ucasemap_setBreakIterator(UCaseMap *csm, UBreakIterator *iterToAdopt, UErrorCode *pErrorCode) {
    if(U_FAILURE(*pErrorCode)) {
        return;
    }
    CaseMapFriend::adoptIter(*CaseMapFriend::fromUCaseMap(csm),
                             reinterpret_cast<BreakIterator *>(iterToAdopt));
}

U_CAPI int32_t U_EXPORT2
ucasemap_utf8ToTitle(UCaseMap *ucsm,
                     char *dest, int32_t destCapacity,
                     const char *src, int32_t srcLength,
                     UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return 0;
    }
    CaseMap &csm = *CaseMapFriend::fromUCaseMap(ucsm);
    UText utext=UTEXT_INITIALIZER;
    utext_openUTF8(&utext, (const char *)src, srcLength, pErrorCode);
    if (CaseMapFriend::iter(csm) == NULL) {
        CaseMapFriend::adoptIter(
            csm, BreakIterator::createWordInstance(CaseMapFriend::locale(csm), *pErrorCode));
    }
    if (U_FAILURE(*pErrorCode)) {
        return 0;
    }
    CaseMapFriend::mutableIter(csm)->setText(&utext, *pErrorCode);
    int32_t length=ucasemap_mapUTF8(csm,
            CaseMapFriend::mutableIter(csm),
            (uint8_t *)dest, destCapacity,
            (const uint8_t *)src, srcLength,
            ucasemap_internalUTF8ToTitle, pErrorCode);
    utext_close(&utext);
    return length;
}

#endif  // !UCONFIG_NO_BREAK_ITERATION
