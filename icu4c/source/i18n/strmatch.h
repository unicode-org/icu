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

U_NAMESPACE_BEGIN

class TransliterationRuleData;

/**
 * An object that matches a string.
 */
class StringMatcher : public UnicodeMatcher {

 public:

    StringMatcher(const UnicodeString& string,
                  int32_t start,
                  int32_t limit,
                  UBool isSegment,
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
     * Remove any match data.  This must be called before performing a
     * set of matches with this segment.
     */
    void resetMatch();

    /**
     * Return the start offset, in the match text, of the <em>rightmost</em>
     * match.  This method may get moved up into the UnicodeMatcher if
     * it turns out to be useful to generalize this.
     */
    int32_t getMatchStart() const;

    /**
     * Return the limit offset, in the match text, of the <em>rightmost</em>
     * match.  This method may get moved up into the UnicodeMatcher if
     * it turns out to be useful to generalize this.
     */
    int32_t getMatchLimit() const;

 private:

    UnicodeString pattern;

    const TransliterationRuleData& data;

    UBool isSegment;

    int32_t matchStart;

    int32_t matchLimit;
};

U_NAMESPACE_END

#endif
