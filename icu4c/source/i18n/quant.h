/*
* Copyright (C) 2001, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/26/01    aliu        Creation.
**********************************************************************
*/
#ifndef QUANT_H
#define QUANT_H

#include "unicode/unimatch.h"

U_NAMESPACE_BEGIN

class Quantifier : public UnicodeMatcher {

 public:

    enum { MAX = 0x7FFFFFFF };

    Quantifier(UnicodeMatcher *adopted,
               uint32_t minCount, uint32_t maxCount);

    Quantifier(const Quantifier& o);

    virtual ~Quantifier();

    /**
     * Implement UnicodeMatcher
     */
    virtual UnicodeMatcher* clone() const;

    /**
     * Implement UnicodeMatcher
     */
    virtual UMatchDegree matches(const Replaceable& text,
                                 int32_t& offset,
                                 int32_t limit,
                                 UBool incremental) const;

    /**
     * Implement UnicodeMatcher
     */
    virtual UnicodeString& toPattern(UnicodeString& result,
                                     UBool escapeUnprintable = FALSE) const;

    /**
     * Implement UnicodeMatcher
     */
    virtual UBool matchesIndexValue(uint8_t v) const;

 private:

    static void appendNumber(UnicodeString& result, int32_t n);

    UnicodeMatcher* matcher; // owned

    uint32_t minCount;

    uint32_t maxCount;
};

U_NAMESPACE_END

#endif
