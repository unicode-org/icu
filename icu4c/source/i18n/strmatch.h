/*
* Copyright (C) 2001, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/23/01    aliu        Creation.
**********************************************************************
*/
#ifndef STRMATCH_H
#define STRMATCH_H

#include "unicode/unistr.h"
#include "unicode/unimatch.h"

class TransliterationRuleData;

/**
 * An object that matches a string.
 */
class StringMatcher : public UnicodeMatcher {

 public:

    StringMatcher(const UnicodeString& string,
                  int32_t start,
                  int32_t limit,
                  const TransliterationRuleData& data);

    StringMatcher(const UnicodeString& string,
                  const TransliterationRuleData& data);

    StringMatcher(const StringMatcher& o);
        
    /**
     * Destructor
     */
    virtual ~StringMatcher();

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

    UnicodeString pattern;

    const TransliterationRuleData& data;
};

#endif
