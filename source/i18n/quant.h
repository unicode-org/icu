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

class Quantifier : public UnicodeMatcher {

 public:

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

    UnicodeMatcher* matcher; // owned

    uint32_t minCount;

    uint32_t maxCount;
};

#endif
