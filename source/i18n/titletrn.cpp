/*
**********************************************************************
*   Copyright (C) 2001-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   05/24/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_TRANSLITERATION

#include "unicode/uchar.h"
#include "unicode/uniset.h"
#include "unicode/ustring.h"
#include "titletrn.h"
#include "umutex.h"
#include "ucln_in.h"
#include "ustr_imp.h"
#include "cpputils.h"

U_NAMESPACE_BEGIN

UOBJECT_DEFINE_RTTI_IMPLEMENTATION(TitlecaseTransliterator)

/**
 * ID for this transliterator.
 */
const char CURR_ID[] = "Any-Title";

/**
 * The set of characters we skip.  These are neither cased nor
 * non-cased, to us; we copy them verbatim.  INVARIANT: Either SKIP
 * and CASED are both NULL, or neither is NULL.
 */
static UnicodeSet* SKIP = NULL;

/**
 * The set of characters that cause the next non-SKIP character to be
 * lowercased.  INVARIANT: Either SKIP and CASED are both NULL, or
 * neither is NULL.
 */
static UnicodeSet* CASED = NULL;

TitlecaseTransliterator::TitlecaseTransliterator(const Locale& theLoc) :
    Transliterator(UnicodeString(CURR_ID, ""), 0),
    loc(theLoc), 
    buffer(0)
{
    buffer = (UChar *)uprv_malloc(u_getMaxCaseExpansion()*sizeof(buffer[0]));
    // Need to look back 2 characters in the case of "can't"
    setMaximumContextLength(2);

    umtx_lock(NULL);
    UBool f = (SKIP == NULL);
    umtx_unlock(NULL);

    if (f) {
        UErrorCode ec = U_ZERO_ERROR;
        UnicodeSet* skip =
            new UnicodeSet(UNICODE_STRING_SIMPLE("[\\u00AD \\u2019 \\' [:Mn:] [:Me:] [:Cf:] [:Lm:] [:Sk:]]"), ec);
        UnicodeSet* cased =
            new UnicodeSet(UNICODE_STRING_SIMPLE("[[:Lu:] [:Ll:] [:Lt:]]"), ec);
        if (skip != NULL && cased != NULL && U_SUCCESS(ec)) {
            umtx_lock(NULL);
            if (SKIP == NULL) {
                SKIP = skip;
                CASED = cased;
                skip = cased = NULL;
            }
            umtx_unlock(NULL);
        }
        delete skip;
        delete cased;
        ucln_i18n_registerCleanup();
    }
}

/**
 * Destructor.
 */
TitlecaseTransliterator::~TitlecaseTransliterator() {
    uprv_free(buffer);
}

/**
 * Copy constructor.
 */
TitlecaseTransliterator::TitlecaseTransliterator(const TitlecaseTransliterator& o) :
    Transliterator(o),
    loc(o.loc),
    buffer(0)
{
    buffer = (UChar *)uprv_malloc(u_getMaxCaseExpansion()*sizeof(buffer[0]));
    uprv_arrayCopy(o.buffer, 0, this->buffer, 0, u_getMaxCaseExpansion());
}

/**
 * Assignment operator.
 */
TitlecaseTransliterator& TitlecaseTransliterator::operator=(
                             const TitlecaseTransliterator& o) {
    Transliterator::operator=(o);
    loc = o.loc;
    uprv_arrayCopy(o.buffer, 0, this->buffer, 0, u_getMaxCaseExpansion());
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
                                  UBool /*isIncremental*/) const
{
    /* TODO: Verify that isIncremental can be ignored */
    if (SKIP == NULL) {
        return;
    }

    // Our mode; we are either converting letter toTitle or
    // toLower.
    UBool doTitle = TRUE;
    
    // Determine if there is a preceding context of CASED SKIP*,
    // in which case we want to start in toLower mode.  If the
    // prior context is anything else (including empty) then start
    // in toTitle mode.
    UChar32 c;
    int32_t start;
    for (start = offsets.start - 1; start >= offsets.contextStart; start -= UTF_CHAR_LENGTH(c)) {
        c = text.char32At(start);
        if (SKIP->contains(c)) {
            continue;
        }
        doTitle = !CASED->contains(c);
        break;
    }
    
    // Convert things after a CASED character toLower; things
    // after a non-CASED, non-SKIP character toTitle.  SKIP
    // characters are copied directly and do not change the mode.
    int32_t textPos = offsets.start;
    if (textPos >= offsets.limit) return;

    UnicodeString original;
    text.extractBetween(offsets.contextStart, offsets.contextLimit, original);

    UCharIterator iter;
    uiter_setReplaceable(&iter, &text);
    iter.start = offsets.contextStart;
    iter.limit = offsets.contextLimit;

    // Walk through original string
    // If there is a case change, modify corresponding position in replaceable

    int32_t i = textPos - offsets.contextStart;
    int32_t limit = offsets.limit - offsets.contextStart;
    UChar32 cp;
    int32_t oldLen;
    int32_t newLen;

    for (; i < limit; ) {
        UTF_GET_CHAR(original.getBuffer(), 0, i, original.length(), cp);
        oldLen = UTF_CHAR_LENGTH(cp);
        i += oldLen;
        iter.index = i; // Point _past_ current char
        if (!SKIP->contains(cp)) {
            if (doTitle) {
                newLen = u_internalToTitle(cp, &iter, buffer, u_getMaxCaseExpansion(), loc.getName());
            } else {
                newLen = u_internalToLower(cp, &iter, buffer, u_getMaxCaseExpansion(), loc.getName());
            }
            doTitle = !CASED->contains(cp);
            if (newLen >= 0) {
                UnicodeString temp(buffer, newLen);
                text.handleReplaceBetween(textPos, textPos + oldLen, temp);
                if (newLen != oldLen) {
                    textPos += newLen;
                    offsets.limit += newLen - oldLen;
                    offsets.contextLimit += newLen - oldLen;
                    continue;
                }
            }
        }
        textPos += oldLen;
    }
    offsets.start = offsets.limit;
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

#endif /* #if !UCONFIG_NO_TRANSLITERATION */
