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
    isSegment(isSeg),
    matchStart(-1),
    matchLimit(-1)
{
    theString.extractBetween(start, limit, pattern);
}

StringMatcher::StringMatcher(const StringMatcher& o) :
    UnicodeMatcher(o),
    pattern(o.pattern),
    data(o.data),
    isSegment(o.isSegment),
    matchStart(o.matchStart),
    matchLimit(o.matchLimit)
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
                                    UBool incremental) {
    int32_t i;
    int32_t cursor = offset;
    if (limit < cursor) {
        // Match in the reverse direction
        for (i=pattern.length()-1; i>=0; --i) {
            UChar keyChar = pattern.charAt(i);
            UnicodeMatcher* subm = data.lookup(keyChar);
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
        // Record the match position, but adjust for a normal
        // forward start, limit, and only if a prior match does not
        // exist -- we want the rightmost match.
        if (matchStart < 0) {
            matchStart = cursor+1;
            matchLimit = offset+1;
        }
    } else {
        for (i=0; i<pattern.length(); ++i) {
            if (incremental && cursor == limit) {
                // We've reached the context limit without a mismatch and
                // without completing our match.
                return U_PARTIAL_MATCH;
            }
            UChar keyChar = pattern.charAt(i);
            UnicodeMatcher* subm = data.lookup(keyChar);
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
        // Record the match position
        matchStart = offset;
        matchLimit = cursor;
    }

    offset = cursor;
    return U_MATCH;
}

/**
 * Implement UnicodeMatcher
 */
UnicodeString& StringMatcher::toPattern(UnicodeString& result,
                                        UBool escapeUnprintable) const {
	result.truncate(0);
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
    TransliterationRule::appendToRule(result, -1,
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

/**
 * Remove any match data.  This must be called before performing a
 * set of matches with this segment.
 */
 void StringMatcher::resetMatch() {
    matchStart = matchLimit = -1;
}

/**
 * Return the start offset, in the match text, of the <em>rightmost</em>
 * match.  This method may get moved up into the UnicodeMatcher if
 * it turns out to be useful to generalize this.
 */
int32_t StringMatcher::getMatchStart() const {
    return matchStart;
}

/**
 * Return the limit offset, in the match text, of the <em>rightmost</em>
 * match.  This method may get moved up into the UnicodeMatcher if
 * it turns out to be useful to generalize this.
 */
int32_t StringMatcher::getMatchLimit() const {
    return matchLimit;
}

U_NAMESPACE_END

//eof

