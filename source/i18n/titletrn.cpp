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
#include "titletrn.h"
#include "unicode/uniset.h"
#include "mutex.h"
#include "ucln_in.h"
#include "unicode/ustring.h"
#include "ustr_imp.h"
#include "cpputils.h"

U_NAMESPACE_BEGIN

/**
 * ID for this transliterator.
 */
const char TitlecaseTransliterator::_ID[] = "Any-Title";

/**
 * Mutex for statics IN THIS FILE
 */
static UMTX MUTEX = 0;

/**
 * The set of characters we skip.  These are neither cased nor
 * non-cased, to us; we copy them verbatim.
 */
static UnicodeSet* SKIP = NULL;

/**
 * The set of characters that cause the next non-SKIP character
 * to be lowercased.
 */
static UnicodeSet* CASED = NULL;

TitlecaseTransliterator::TitlecaseTransliterator(const Locale& theLoc) :
    Transliterator(_ID, 0),
    loc(theLoc), 
    buffer(0) {
    buffer = new UChar[u_getMaxCaseExpansion()];
    // Need to look back 2 characters in the case of "can't"
    setMaximumContextLength(2);
}

/**
 * Destructor.
 */
TitlecaseTransliterator::~TitlecaseTransliterator() {
    delete [] buffer;
}

/**
 * Copy constructor.
 */
TitlecaseTransliterator::TitlecaseTransliterator(const TitlecaseTransliterator& o) :
    Transliterator(o),
    loc(o.loc),
    buffer(0) {
    buffer = new UChar[u_getMaxCaseExpansion()];    
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
                                  UBool isIncremental) const {
    if (SKIP == NULL) {
        Mutex lock(&MUTEX);
        if (SKIP == NULL) {
            UErrorCode ec = U_ZERO_ERROR;
            SKIP = new UnicodeSet(UnicodeString("[\\u00AD \\u2019 \\' [:Mn:] [:Me:] [:Cf:] [:Lm:]]", ""), ec);
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

    // get string for context
    // TODO: add convenience method to do this, since we do it all over

    int32_t loop = 0;
    UnicodeString original;
    /* UChar *original = new UChar[offsets.contextLimit - offsets.contextStart+1]; */// get whole context
    /* Extract the characters from Replaceable */
    for (loop = offsets.contextStart; loop < offsets.contextLimit; loop++) {
        original.append(text.charAt(loop));
    }
    // Walk through original string
    // If there is a case change, modify corresponding position in replaceable

    int32_t i = textPos - offsets.contextStart;
    int32_t limit = offsets.limit - offsets.contextStart;
    UChar32 cp, bufferCH;
    int32_t oldLen;
    int32_t newLen;

    for (; i < limit; ) {
        UErrorCode status = U_ZERO_ERROR;
        int32_t s = i;

        UTF_GET_CHAR(original.getBuffer(), 0, i, original.length(), cp);
        oldLen = UTF_CHAR_LENGTH(cp);
        i += oldLen;
        if (!SKIP->contains(cp)) {
            if (doTitle) {
                newLen = u_internalTitleCase(cp, buffer, u_getMaxCaseExpansion(), loc.getName());
            } else {
                int32_t len = u_strToLower(buffer, u_getMaxCaseExpansion(), original.getBuffer()+s, i-s, loc.getName(), &status);
                UTF_GET_CHAR(buffer, 0, 0, len, bufferCH);
                newLen = (bufferCH == original.char32At(s) ? -1 : len);
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
        umtx_destroy(&MUTEX);
    }
}

U_NAMESPACE_END

