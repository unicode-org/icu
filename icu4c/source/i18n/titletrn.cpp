/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/uchar.h"
#include "unicode/titletrn.h"
#include "unicode/uniset.h"
#include "mutex.h"
#include "ucln_in.h"

U_NAMESPACE_BEGIN

/**
 * ID for this transliterator.
 */
const char TitlecaseTransliterator::_ID[] = "Any-Title";

/**
 * The set of characters we skip.  These are neither cased nor
 * non-cased, to us; we copy them verbatim.
 */
static const UnicodeSet* SKIP = NULL;

/**
 * The set of characters that cause the next non-SKIP character
 * to be lowercased.
 */
static const UnicodeSet* CASED = NULL;

TitlecaseTransliterator::TitlecaseTransliterator(UnicodeFilter* adoptedFilter) :
    Transliterator(_ID, adoptedFilter) {
    // Need to look back 2 characters in the case of "can't"
    setMaximumContextLength(2);
}

/**
 * Destructor.
 */
TitlecaseTransliterator::~TitlecaseTransliterator() {}

/**
 * Copy constructor.
 */
TitlecaseTransliterator::TitlecaseTransliterator(const TitlecaseTransliterator& o) :
    Transliterator(o) {}

/**
 * Assignment operator.
 */
TitlecaseTransliterator& TitlecaseTransliterator::operator=(
                             const TitlecaseTransliterator& o) {
    Transliterator::operator=(o);
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* TitlecaseTransliterator::clone(void) const {
    return new TitlecaseTransliterator(*this);
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void TitlecaseTransliterator::handleTransliterate(
                                  Replaceable& text, UTransPosition& offsets,
                                  UBool isIncremental) const {
    if (SKIP == NULL) {
        Mutex lock;
        if (SKIP == NULL) {
            UErrorCode ec = U_ZERO_ERROR;
            SKIP = new UnicodeSet(UnicodeString("[\\u00AD \\u2019 \\' [:Mn:] [:Me:] [:Cf:]]", ""), ec);
            CASED = new UnicodeSet(UnicodeString("[[:Lu:] [:Ll:] [:Lt:]]", ""), ec);
            ucln_i18n_registerCleanup();
        }
    }

    // Our mode; we are either converting letter toTitle or
    // toLower.
    UBool doTitle = TRUE;
    
    // Determine if there is a preceding context of CASED SKIP*,
    // in which case we want to start in toLower mode.  If the
    // prior context is anything else (including empty) then start
    // in toTitle mode.
    int32_t start = offsets.start;
    while (start > offsets.contextStart) {
        UChar c = text.charAt(--start);
        if (SKIP->contains(c)) {
            continue;
        }
        doTitle = !CASED->contains(c);
        break;
    }
    
    // Convert things after a CASED character toLower; things
    // after a non-CASED, non-SKIP character toTitle.  SKIP
    // characters are copied directly and do not change the mode.
    UnicodeString str("A", "");
    for (start=offsets.start; start<offsets.limit; ++start) {
        UChar c = text.charAt(start);
        if (SKIP->contains(c)) {
            continue;
        }
        UChar d = (UChar) (doTitle ? u_totitle(c)
                                   : u_tolower(c));
        if (c != d) {
            str.setCharAt(0, d);
            text.handleReplaceBetween(start, start+1, str);
        }
        doTitle = !CASED->contains(c);
    }
    
    offsets.start = start;
}

/**
 * Static memory cleanup function.
 */
void TitlecaseTransliterator::cleanup() {
    if (SKIP != NULL) {
        delete SKIP; SKIP = NULL;
        delete CASED; CASED = NULL;
    }
}

U_NAMESPACE_END

