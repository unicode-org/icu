/*
* Copyright (C) 2001, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/26/01    aliu        Creation.
**********************************************************************
*/

#include "quant.h"

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

/**
 * Implement UnicodeMatcher
 */
UnicodeString& Quantifier::toPattern(UnicodeString& result,
                                     UBool escapeUnprintable) const {
    // TODO finish this
    return result;
}

/**
 * Implement UnicodeMatcher
 */
UBool Quantifier::matchesIndexValue(uint8_t v) const {
    return (minCount == 0) || matcher->matchesIndexValue(v);
}

//eof
