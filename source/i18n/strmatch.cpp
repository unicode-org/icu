/*
* Copyright (C) 2001, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   07/23/01    aliu        Creation.
**********************************************************************
*/

#include "strmatch.h"
#include "rbt_data.h"
#include "rbt_rule.h"

U_NAMESPACE_BEGIN

StringMatcher::StringMatcher(const UnicodeString& theString,
                             int32_t start,
                             int32_t limit,
                             UBool isSeg,
                             const TransliterationRuleData& theData) :
    data(theData),
    isSegment(isSeg)
{
    theString.extractBetween(start, limit, pattern);
}

StringMatcher::StringMatcher(const StringMatcher& o) :
    UnicodeMatcher(o),
    pattern(o.pattern),
    data(o.data),
    isSegment(o.isSegment)
{
}

/**
 * Destructor
 */
StringMatcher::~StringMatcher() {
}

/**
 * Implement UnicodeMatcher
 */
UnicodeMatcher* StringMatcher::clone() const {
    return new StringMatcher(*this);
}

/**
 * Implement UnicodeMatcher
 */
UMatchDegree StringMatcher::matches(const Replaceable& text,
                                    int32_t& offset,
                                    int32_t limit,
                                    UBool incremental) const {
    int32_t i;
    int32_t cursor = offset;
    if (limit < cursor) {
        for (i=pattern.length()-1; i>=0; --i) {
            UChar keyChar = pattern.charAt(i);
            const UnicodeMatcher* subm = data.lookup(keyChar);
            if (subm == 0) {
                if (cursor >= limit &&
                    keyChar == text.charAt(cursor)) {
                    --cursor;
                } else {
                    return U_MISMATCH;
                }
            } else {
                UMatchDegree m =
                    subm->matches(text, cursor, limit, incremental);
                if (m != U_MATCH) {
                    return m;
                }
            }
        }
    } else {
        for (i=0; i<pattern.length(); ++i) {
            if (incremental && cursor == limit) {
                // We've reached the context limit without a mismatch and
                // without completing our match.
                return U_PARTIAL_MATCH;
            }
            UChar keyChar = pattern.charAt(i);
            const UnicodeMatcher* subm = data.lookup(keyChar);
            if (subm == 0) {
                // Don't need the cursor < limit check if
                // incremental is TRUE (because it's done above); do need
                // it otherwise.
                if (cursor < limit &&
                    keyChar == text.charAt(cursor)) {
                    ++cursor;
                } else {
                    return U_MISMATCH;
                }
            } else {
                UMatchDegree m =
                    subm->matches(text, cursor, limit, incremental);
                if (m != U_MATCH) {
                    return m;
                }
            }
        }
    }

    offset = cursor;
    return U_MATCH;
}

/**
 * Implement UnicodeMatcher
 */
UnicodeString& StringMatcher::toPattern(UnicodeString& result,
                                        UBool escapeUnprintable) const {
    UnicodeString str, quoteBuf;
    if (isSegment) {
        result.append((UChar)40); /*(*/
    }
    for (int32_t i=0; i<pattern.length(); ++i) {
        UChar keyChar = pattern.charAt(i);
        const UnicodeMatcher* m = data.lookup(keyChar);
        if (m == 0) {
            TransliterationRule::appendToRule(result, keyChar, FALSE, escapeUnprintable, quoteBuf);
        } else {
            TransliterationRule::appendToRule(result, m->toPattern(str, escapeUnprintable),
                         TRUE, escapeUnprintable, quoteBuf);
        }
    }
    if (isSegment) {
        result.append((UChar)41); /*)*/
    }
    // Flush quoteBuf out to result
    TransliterationRule::appendToRule(result, (UChar32)(isSegment?41/*)*/:-1),
                                          TRUE, escapeUnprintable, quoteBuf);
    return result;
}

/**
 * Implement UnicodeMatcher
 */
UBool StringMatcher::matchesIndexValue(uint8_t v) const {
    if (pattern.length() == 0) {
        return TRUE;
    }
    UChar32 c = pattern.char32At(0);
    const UnicodeMatcher *m = data.lookup(c);
    return (m == 0) ? ((c & 0xFF) == v) : m->matchesIndexValue(v);
}

U_NAMESPACE_END

//eof

