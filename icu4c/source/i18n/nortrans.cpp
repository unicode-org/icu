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
    UErrorCode status = U_ZERO_ERROR;
    Transliterator::_registerFactory(UnicodeString("Any-NFC", ""), _createNFC, status);
    Transliterator::_registerFactory(UnicodeString("Any-NFKC", ""), _createNFKC, status);
    Transliterator::_registerFactory(UnicodeString("Any-NFD", ""), _createNFD, status);
    Transliterator::_registerFactory(UnicodeString("Any-NFKD", ""), _createNFKD, status);
}

/**
 * Factory methods
 */
Transliterator* NormalizationTransliterator::_createNFC() {
    return new NormalizationTransliterator(UnicodeString("NFC", ""),
                                           Normalizer::COMPOSE, 0);
}
Transliterator* NormalizationTransliterator::_createNFKC() {
    return new NormalizationTransliterator(UnicodeString("NFKC", ""),
                                           Normalizer::COMPOSE_COMPAT, 0);
}
Transliterator* NormalizationTransliterator::_createNFD() {
    return new NormalizationTransliterator(UnicodeString("NFD", ""),
                                           Normalizer::DECOMP, 0);
}
Transliterator* NormalizationTransliterator::_createNFKD() {
    return new NormalizationTransliterator(UnicodeString("NFKD", ""),
                                           Normalizer::DECOMP_COMPAT, 0);
}

/**
 * Factory method.
 */
NormalizationTransliterator*
NormalizationTransliterator::createInstance(Normalizer::EMode m,
                                            int32_t opt) {
    UnicodeString id("NF", "");
    if ((m & Normalizer::COMPAT_BIT) != 0) {
        id.append((UChar)0x004B /*K*/);
    }
    id.append((UChar) (((m & Normalizer::COMPOSE_BIT) != 0) ? 0x0043 : 0x0044));
    return new NormalizationTransliterator(id, m, opt);
}

/**
 * Constructs a transliterator.
 */
NormalizationTransliterator::NormalizationTransliterator(
                                 const UnicodeString& id,
                                 Normalizer::EMode m, int32_t opt) :
    Transliterator(id, 0) {
    mode = m;
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
    mode = o.mode;
    options = o.options;
}

/**
 * Assignment operator.
 */
NormalizationTransliterator& NormalizationTransliterator::operator=(const NormalizationTransliterator& o) {
    Transliterator::operator=(o);
    mode = o.mode;
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
        Normalizer::normalize(input, mode, options, output, status);

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
