/*
* Copyright (C) 2001, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/26/01    aliu        Creation.
**********************************************************************
*/

#include "quant.h"
#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

Quantifier::Quantifier(UnicodeMatcher *adopted,
                       uint32_t minCount, uint32_t maxCount) {
    // assert(adopted != 0);
    // assert(minCount <= maxCount);
    matcher = adopted;
    this->minCount = minCount;
    this->maxCount = maxCount;
}

Quantifier::Quantifier(const Quantifier& o) :
    matcher(o.matcher->clone()),
    minCount(o.minCount),
    maxCount(o.maxCount) {
    delete matcher;
}

Quantifier::~Quantifier() {
    delete matcher;
}

/**
 * Implement UnicodeMatcher
 */
UnicodeMatcher* Quantifier::clone() const {
    return new Quantifier(*this);
}

UMatchDegree Quantifier::matches(const Replaceable& text,
                                 int32_t& offset,
                                 int32_t limit,
                                 UBool incremental) const {
    int32_t start = offset;
    uint32_t count = 0;
    while (count < maxCount) {
        UMatchDegree m = matcher->matches(text, offset, limit, incremental);
        if (m == U_MATCH) {
            ++count;
        } else if (incremental && m == U_PARTIAL_MATCH) {
            return U_PARTIAL_MATCH;
        } else {
            break;
        }
    }
    if (incremental && offset == limit) {
        return U_PARTIAL_MATCH;
    }
    if (count >= minCount) {
        return U_MATCH;
    }
    offset = start;
    return U_MISMATCH;
}

static const int32_t POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000,
                                10000000, 100000000, 1000000000};

void Quantifier::appendNumber(UnicodeString& result, int32_t n) {
    // assert(n >= 0);
    // assert(n < 1e10);
    UBool show = FALSE; // TRUE if we should display digits
    for (int32_t p=9; p>=0; --p) {
        int32_t d = n / POW10[p];
        n -= d * POW10[p];
        if (d != 0 || p == 0) {
            show = TRUE;
        }
        if (show) {
            result.append((UChar)(48+d));
        }
    }
}

/**
 * Implement UnicodeMatcher
 */
UnicodeString& Quantifier::toPattern(UnicodeString& result,
                                     UBool escapeUnprintable) const {
    matcher->toPattern(result, escapeUnprintable);
    if (minCount == 0) {
        if (maxCount == 1) {
            return result.append((UChar)63); /*?*/
        } else if (maxCount == MAX) {
            return result.append((UChar)42); /***/
        }
        // else fall through
    } else if (minCount == 1 && maxCount == MAX) {
        return result.append((UChar)43); /*+*/
    }
    result.append((UChar)123); /*{*/
    appendNumber(result, minCount);
    result.append((UChar)44); /*,*/
    if (maxCount != MAX) {
        appendNumber(result, maxCount);
    }
    result.append((UChar)125); /*}*/
    return result;
}

/**
 * Implement UnicodeMatcher
 */
UBool Quantifier::matchesIndexValue(uint8_t v) const {
    return (minCount == 0) || matcher->matchesIndexValue(v);
}

U_NAMESPACE_END

//eof
