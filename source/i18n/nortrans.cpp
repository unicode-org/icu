/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/03/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/nortrans.h"

/**
 * System registration hook.
 */
void NormalizationTransliterator::registerIDs() {
    Transliterator::_registerFactory(UnicodeString("Any-NFC", ""), _createNFC);
    Transliterator::_registerFactory(UnicodeString("Any-NFKC", ""), _createNFKC);
    Transliterator::_registerFactory(UnicodeString("Any-NFD", ""), _createNFD);
    Transliterator::_registerFactory(UnicodeString("Any-NFKD", ""), _createNFKD);
}

/**
 * Factory methods
 */
Transliterator* NormalizationTransliterator::_createNFC() {
    return new NormalizationTransliterator(UNICODE_STRING("NFC", 3),
                                           UNORM_NFC, 0);
}
Transliterator* NormalizationTransliterator::_createNFKC() {
    return new NormalizationTransliterator(UNICODE_STRING("NFKC", 4),
                                           UNORM_NFKC, 0);
}
Transliterator* NormalizationTransliterator::_createNFD() {
    return new NormalizationTransliterator(UNICODE_STRING("NFD", 3),
                                           UNORM_NFD, 0);
}
Transliterator* NormalizationTransliterator::_createNFKD() {
    return new NormalizationTransliterator(UNICODE_STRING("NFKD", 4),
                                           UNORM_NFKD, 0);
}

/**
 * Factory method.
 */
NormalizationTransliterator*
NormalizationTransliterator::createInstance(UNormalizationMode mode,
                                            int32_t opt) {
    switch(mode) {
    case UNORM_NFC:
        return (NormalizationTransliterator *)_createNFC();
    case UNORM_NFKC:
        return (NormalizationTransliterator *)_createNFKC();
    case UNORM_NFD:
        return (NormalizationTransliterator *)_createNFD();
    case UNORM_NFKD:
        return (NormalizationTransliterator *)_createNFKD();
    default:
        return 0;
    }
}

/**
 * Constructs a transliterator.
 */
NormalizationTransliterator::NormalizationTransliterator(
                                 const UnicodeString& id,
                                 UNormalizationMode mode, int32_t opt) :
    Transliterator(id, 0) {
    fMode = mode;
    options = opt;
}

/**
 * Destructor.
 */
NormalizationTransliterator::~NormalizationTransliterator() {
}

/**
 * Copy constructor.
 */
NormalizationTransliterator::NormalizationTransliterator(const NormalizationTransliterator& o) :
Transliterator(o) {
    fMode = o.fMode;
    options = o.options;
}

/**
 * Assignment operator.
 */
NormalizationTransliterator& NormalizationTransliterator::operator=(const NormalizationTransliterator& o) {
    Transliterator::operator=(o);
    fMode = o.fMode;
    options = o.options;
    return *this;
}

/**
 * Transliterator API.
 */
Transliterator* NormalizationTransliterator::clone(void) const {
    return new NormalizationTransliterator(*this);
}

// TODO
// TODO
// TODO
// Get rid of this function and use the official Replaceable
// extractBetween() method, when possible
// TODO
// TODO
// TODO
static void _Replaceable_extractBetween(const Replaceable& text,
                                        int32_t start,
                                        int32_t limit,
                                        UChar* buffer) {
    while (start < limit) {
        *buffer++ = text.charAt(start++);
    }
}

/**
 * Implements {@link Transliterator#handleTransliterate}.
 */
void NormalizationTransliterator::handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                                      UBool isIncremental) const {
    int32_t start = offsets.start;
    int32_t limit = offsets.limit;

    // For the non-incremental case normalize right up to
    // offsets.limit.  In the incremental case, find the last base
    // character b, and pass everything from the start up to the
    // character before b to normalizer.
    if (isIncremental) {
        --limit;
        while (limit > start &&
               u_getCombiningClass(text.charAt(limit)) != 0) {
            --limit;
        }
    }

    if (limit > start) {

        UChar staticChars[256];
        UChar* chars = staticChars;

        if ((limit - start) > 255) {
            // Allocate extra buffer space if needed
            chars = new UChar[limit-start+1];
            if (chars == NULL) {
                return;
            }
        }

        _Replaceable_extractBetween(text, start, limit, chars);

        UnicodeString input(FALSE, chars, limit-start); // readonly alias
        UnicodeString output;
        UErrorCode status = U_ZERO_ERROR;
        Normalizer::normalize(input, fMode, options, output, status);

        if (chars != staticChars) {
            delete[] chars;
        }

        text.handleReplaceBetween(start, limit, output);

        int32_t delta = output.length() - input.length();
        offsets.contextLimit += delta;
        offsets.limit += delta;
        offsets.start = limit;
    }
}
