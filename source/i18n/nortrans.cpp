/*
**********************************************************************
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/03/01    aliu        Creation.
**********************************************************************
*/

#include "unicode/utypes.h"
#include "unicode/nortrans.h"
#include "unormimp.h"

U_NAMESPACE_BEGIN

U_CDECL_BEGIN

/*
 * This is an implementation of a code unit (UChar) iterator
 * based on a Replaceable object.
 * It is used with the internal API for incremental normalization.
 *
 * The UCharIterator.context field holds a pointer to the Replaceable.
 * UCharIterator.length and UCharIterator.index hold Replaceable.length()
 * and the iteration index.
 */

static int32_t U_CALLCONV
replaceableIteratorMove(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin) {
    int32_t pos;

    switch(origin) {
    case UITERATOR_START:
        pos=iter->start+delta;
        break;
    case UITERATOR_CURRENT:
        pos=iter->index+delta;
        break;
    case UITERATOR_END:
        pos=iter->limit+delta;
        break;
    default:
        /* not a valid origin, no move */
        break;
    }

    if(pos<iter->start) {
        pos=iter->start;
    } else if(pos>iter->limit) {
        pos=iter->limit;
    }

    return iter->index=pos;
}

static UBool U_CALLCONV
replaceableIteratorHasNext(UCharIterator *iter) {
    return iter->index<iter->limit;
}

static UBool U_CALLCONV
replaceableIteratorHasPrevious(UCharIterator *iter) {
    return iter->index>iter->start;
}

static UChar U_CALLCONV
replaceableIteratorCurrent(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((Replaceable *)(iter->context))->charAt(iter->index);
    } else {
        return 0xffff;
    }
}

static UChar U_CALLCONV
replaceableIteratorNext(UCharIterator *iter) {
    if(iter->index<iter->limit) {
        return ((Replaceable *)(iter->context))->charAt(iter->index++);
    } else {
        return 0xffff;
    }
}

static UChar U_CALLCONV
replaceableIteratorPrevious(UCharIterator *iter) {
    if(iter->index>iter->start) {
        return ((Replaceable *)(iter->context))->charAt(--iter->index);
    } else {
        return 0xffff;
    }
}

static const UCharIterator replaceableIterator={
    0, 0, 0, 0, 0,
    replaceableIteratorMove,
    replaceableIteratorHasNext,
    replaceableIteratorHasPrevious,
    replaceableIteratorCurrent,
    replaceableIteratorNext,
    replaceableIteratorPrevious
};

U_CDECL_END

/**
 * System registration hook.
 */
void NormalizationTransliterator::registerIDs() {
    UErrorCode errorCode = U_ZERO_ERROR;
    if(!unorm_haveData(&errorCode)) {
        return;
    }

    Transliterator::_registerFactory(UnicodeString("Any-NFC", ""),
                                     _create, integerToken(UNORM_NFC));
    Transliterator::_registerFactory(UnicodeString("Any-NFKC", ""),
                                     _create, integerToken(UNORM_NFKC));
    Transliterator::_registerFactory(UnicodeString("Any-NFD", ""),
                                     _create, integerToken(UNORM_NFD));
    Transliterator::_registerFactory(UnicodeString("Any-NFKD", ""),
                                     _create, integerToken(UNORM_NFKD));
}

/**
 * Factory methods
 */
Transliterator* NormalizationTransliterator::_create(const UnicodeString& ID,
                                                     Token context) {
    return new NormalizationTransliterator(ID, (UNormalizationMode) context.integer, 0);
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
    // start and limit of the input range
    int32_t start = offsets.start;
    int32_t limit = offsets.limit;
    int32_t length, delta;

    if(start >= limit) {
        return;
    }

    // a C code unit iterator, implemented around the Replaceable
    UCharIterator iter = replaceableIterator;
    iter.context = &text;
    // iter.length = text.length(); is not used

    // the output string and buffer pointer
    UnicodeString output;
    UChar *buffer;

    UErrorCode errorCode;

    /*
     * Normalize as short chunks at a time as possible even in
     * bulk mode, so that styled text is minimally disrupted.
     * In incremental mode, a chunk that ends with offsets.limit
     * must not be normalized.
     *
     * If it was known that the input text is not styled, then
     * a bulk mode normalization could look like this:
     *

    UChar staticChars[256];
    UnicodeString input;

    length = limit - start;
    input.setTo(staticChars, 0, sizeof(staticChars)/U_SIZEOF_UCHAR); // writable alias

    _Replaceable_extractBetween(text, start, limit, input.getBuffer(length));
    input.releaseBuffer(length);

    UErrorCode status = U_ZERO_ERROR;
    Normalizer::normalize(input, fMode, options, output, status);

    text.handleReplaceBetween(start, limit, output);

    int32_t delta = output.length() - length;
    offsets.contextLimit += delta;
    offsets.limit += delta;
    offsets.start = limit + delta;

     *
     */
    while(start < limit) {
        // set the iterator limits for the remaining input range
        // this is a moving target because of the replacements in the text object
        iter.start = iter.index = start;
        iter.limit = limit;

        // incrementally normalize a small chunk of the input
        buffer = output.getBuffer(-1);
        errorCode = U_ZERO_ERROR;
        length = unorm_nextNormalize(buffer, output.getCapacity(), &iter,
                                     fMode, FALSE, &errorCode);
        output.releaseBuffer(length);

        if(errorCode == U_BUFFER_OVERFLOW_ERROR) {
            // use a larger output string buffer and do it again from the start
            iter.index = start;
            buffer = output.getBuffer(length);
            errorCode = U_ZERO_ERROR;
            length = unorm_nextNormalize(buffer, output.getCapacity(), &iter,
                                         fMode, FALSE, &errorCode);
            output.releaseBuffer(length);
        }

        if(U_FAILURE(errorCode)) {
            break;
        }

        limit = iter.index;
        if(isIncremental && limit == iter.limit) {
            // stop in incremental mode when we reach the input limit
            // in case there are additional characters that could change the
            // normalization result
            break;
        }

        // replace the input chunk with its normalized form
        text.handleReplaceBetween(start, limit, output);

        // update all necessary indexes accordingly
        delta = length - (limit - start);   // length change in the text object
        start = limit += delta;             // the next chunk starts where this one ends, with adjustment
        limit = offsets.limit += delta;     // set the iteration limit to the adjusted end of the input range
        offsets.contextLimit += delta;
    }

    offsets.start = start;
}

U_NAMESPACE_END
