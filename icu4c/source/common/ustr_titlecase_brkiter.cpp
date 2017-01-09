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

/* functions available in the common library (for unistr_case.cpp) */

/*
 * Set parameters on an empty UCaseMap, for UCaseMap-less API functions.
 * Do this fast because it is called with every function call.
 * Duplicate of the same function in ustrcase.cpp, to keep it inline.
 */
static inline void
setTempCaseMap(UCaseMap *csm, const char *locale) {
    if(csm->csp==NULL) {
        csm->csp=ucase_getSingleton();
    }
    if(locale!=NULL && locale[0]==0) {
        csm->locale[0]=0;
    } else {
        ustrcase_setTempCaseMapLocale(csm, locale);
    }
}

/* public API functions */

U_CAPI int32_t U_EXPORT2
u_strToTitle(UChar *dest, int32_t destCapacity,
             const UChar *src, int32_t srcLength,
             UBreakIterator *titleIter,
             const char *locale,
             UErrorCode *pErrorCode) {
    UCaseMap csm=UCASEMAP_INITIALIZER;
    setTempCaseMap(&csm, locale);
    icu::LocalPointer<icu::BreakIterator> ownedIter;
    icu::BreakIterator *iter;
    if(titleIter!=NULL) {
        iter=reinterpret_cast<icu::BreakIterator *>(titleIter);
    } else {
        iter=icu::BreakIterator::createWordInstance(icu::Locale(csm.locale), *pErrorCode);
        ownedIter.adoptInstead(iter);
    }
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    icu::UnicodeString s(srcLength<0, src, srcLength);
    iter->setText(s);
    return ustrcase_mapWithOverlap(
        &csm, iter,
        dest, destCapacity,
        src, srcLength,
        ustrcase_internalToTitle, *pErrorCode);
}

U_CAPI int32_t U_EXPORT2
ucasemap_toTitleWithEdits(const UCaseMap *csm, icu::BreakIterator *iter,
                          UChar *dest, int32_t destCapacity,
                          const UChar *src, int32_t srcLength,
                          icu::Edits *edits,
                          UErrorCode &errorCode) {
    icu::LocalPointer<icu::BreakIterator> ownedIter;
    if(iter==NULL) {
        if(csm->iter!=NULL) {
            iter=csm->iter->clone();
        } else {
            iter=icu::BreakIterator::createWordInstance(icu::Locale(csm->locale), errorCode);
        }
        ownedIter.adoptInsteadAndCheckErrorCode(iter, errorCode);
    }
    if(U_FAILURE(errorCode)) {
        return 0;
    }
    icu::UnicodeString s(srcLength<0, src, srcLength);
    iter->setText(s);
    return ustrcase_map(
        csm, iter,
        dest, destCapacity,
        src, srcLength,
        ustrcase_internalToTitle, edits, errorCode);
}

U_CAPI int32_t U_EXPORT2
ucasemap_toTitle(UCaseMap *csm,
                 UChar *dest, int32_t destCapacity,
                 const UChar *src, int32_t srcLength,
                 UErrorCode *pErrorCode) {
    if(csm->iter==NULL) {
        csm->iter=icu::BreakIterator::createWordInstance(icu::Locale(csm->locale), *pErrorCode);
    }
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }
    icu::UnicodeString s(srcLength<0, src, srcLength);
    csm->iter->setText(s);
    return ustrcase_map(
        csm, csm->iter,
        dest, destCapacity,
        src, srcLength,
        ustrcase_internalToTitle, NULL, *pErrorCode);
}

#endif  // !UCONFIG_NO_BREAK_ITERATION
