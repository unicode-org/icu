// Copyright (C) 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*   Copyright (C) 2011, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*   file name:  ustr_titlecase_brkiter.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2011may30
*   created by: Markus W. Scherer
*
*   Titlecasing functions that are based on BreakIterator
*   were moved here to break dependency cycles among parts of the common library.
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_BREAK_ITERATION

#include "unicode/brkiter.h"
#include "unicode/localpointer.h"
#include "unicode/ubrk.h"
#include "unicode/ucasemap.h"
#include "cmemory.h"
#include "ucase.h"
#include "ustr_imp.h"

U_NAMESPACE_USE

using icu::internal::CaseMapFriend;

// TODO: create casemap.cpp

void icu::internal::CaseMapFriend::adoptIter(CaseMap &csm, BreakIterator *iter) {
    delete csm.iter;
    csm.iter = iter;
}

/* functions available in the common library (for unistr_case.cpp) */

/* public API functions */

U_CAPI int32_t U_EXPORT2
u_strToTitle(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             UBreakIterator *titleIter,
             const char *locale,
             UErrorCode *pErrorCode) {
    CaseMap csm(locale, 0, *pErrorCode);
    BreakIterator *iter;
    if(titleIter!=NULL) {
        iter=reinterpret_cast<BreakIterator *>(titleIter);
    } else {
        iter=BreakIterator::createWordInstance(CaseMapFriend::locale(csm), *pErrorCode);
        CaseMapFriend::adoptIter(csm, iter);
    }
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    UnicodeString s(srcLength<0, src, srcLength);
    iter->setText(s);
    return ustrcase_mapWithOverlap(
        csm, iter,
        dest, destCapacity,
        src, srcLength,
        ustrcase_internalToTitle, *pErrorCode);
}

U_NAMESPACE_BEGIN

int32_t CaseMap::toTitle(BreakIterator *it,
                         UChar *dest, int32_t destCapacity,
                         const UChar *src, int32_t srcLength,
                         Edits *edits,
                         UErrorCode &errorCode) const {
    LocalPointer<BreakIterator> ownedIter;
    if(it==NULL) {
        if(iter!=NULL) {
            it=iter->clone();
        } else {
            it=BreakIterator::createWordInstance(locale, errorCode);
        }
        ownedIter.adoptInsteadAndCheckErrorCode(it, errorCode);
    }
    if(U_FAILURE(errorCode)) {
        return 0;
    }
    UnicodeString s(srcLength<0, src, srcLength);
    it->setText(s);
    return ustrcase_map(
        *this, it,
        dest, destCapacity,
        src, srcLength,
        ustrcase_internalToTitle, edits, errorCode);
}

U_NAMESPACE_END

U_CAPI int32_t U_EXPORT2
ucasemap_toTitle(UCaseMap *ucsm,
                 UChar *dest, int32_t destCapacity,
                 const UChar *src, int32_t srcLength,
                 UErrorCode *pErrorCode) {
    if (U_FAILURE(*pErrorCode)) {
        return 0;
    }
    CaseMap &csm = *CaseMapFriend::fromUCaseMap(ucsm);
    if (CaseMapFriend::iter(csm) == NULL) {
        CaseMapFriend::adoptIter(
            csm, BreakIterator::createWordInstance(CaseMapFriend::locale(csm), *pErrorCode));
    }
    if (U_FAILURE(*pErrorCode)) {
        return 0;
    }
    UnicodeString s(srcLength<0, src, srcLength);
    CaseMapFriend::mutableIter(csm)->setText(s);
    return ustrcase_map(
        csm, CaseMapFriend::mutableIter(csm),
        dest, destCapacity,
        src, srcLength,
        ustrcase_internalToTitle, NULL, *pErrorCode);
}

#endif  // !UCONFIG_NO_BREAK_ITERATION
