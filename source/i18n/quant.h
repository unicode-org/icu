/*
* Copyright (C) 2001, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/26/01    aliu        Creation.
**********************************************************************
*/
#ifndef QUANT_H
#define QUANT_H

#include "unicode/unifunct.h"
#include "unicode/unimatch.h"

U_NAMESPACE_BEGIN

class Quantifier : public UnicodeFunctor, public UnicodeMatcher {

 public:

    enum { MAX = 0x7FFFFFFF };

    Quantifier(UnicodeFunctor *adoptedMatcher,
               uint32_t minCount, uint32_t maxCount);

    Quantifier(const Quantifier& o);

    virtual ~Quantifier();

    /**
     * UnicodeFunctor API.  Cast 'this' to a UnicodeMatcher* pointer
     * and return the pointer.
     */
    virtual UnicodeMatcher* toMatcher() const;

    /**
     * Implement UnicodeFunctor
     */
    virtual UnicodeFunctor* clone() const;

    /**
     * Implement UnicodeMatcher
     */
    virtual UMatchDegree matches(const Replaceable& text,
                                 int32_t& offset,
                                 int32_t limit,
                                 UBool incremental);

    /**
     * Implement UnicodeMatcher
     */
    virtual UnicodeString& toPattern(UnicodeString& result,
                                     UBool escapeUnprintable = FALSE) const;

    /**
     * Implement UnicodeMatcher
     */
    virtual UBool matchesIndexValue(uint8_t v) const;

    /**
     * Implement UnicodeMatcher
     */
    virtual void addMatchSetTo(UnicodeSet& toUnionTo) const;

    /**
     * UnicodeFunctor API
     */
    virtual void setData(const TransliterationRuleData*);

 private:

    static void appendNumber(UnicodeString& result, int32_t n);

    UnicodeFunctor* matcher; // owned

    uint32_t minCount;

    uint32_t maxCount;
};

U_NAMESPACE_END

#endif
