/*
**********************************************************************
* Copyright © {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef UNIRANGE_H
#define UNIRANGE_H

#include "unicode/utypes.h"

class UnicodeString;

/**
 * %%% INTERNAL CLASS USED BY RuleBasedTransliterator %%%
 *
 * A range of Unicode characters.  Support the operations of testing for
 * inclusion (does this range contain this character?) and splitting.
 * Splitting involves breaking a range into two smaller ranges around a
 * character inside the original range.  The split character is not included
 * in either range.  If the split character is at either extreme end of the
 * range, one of the split products is an empty range.
 *
 * This class is used internally to determine the largest available private
 * use character range for variable stand-ins.
 */
class UnicodeRange {

public:

    UChar start;

    int32_t length;

    UnicodeRange(UChar start, int32_t length);

    /**
     * CALLER OWNS RESULT.
     */
    UnicodeRange* clone() const;

    bool_t contains(UChar c) const;

    /**
     * Assume that contains(c) is true.  Split this range into two new
     * ranges around the character c.  Make this range one of the new ranges
     * (modify it in place) and return the other new range.  The character
     * itself is not included in either range.  If the split results in an
     * empty range (that is, if c == start or c == start + length - 1) then
     * return null.
     *
     * MODIFIES THIS RANGE IN PLACE.
     *
     * CALLER OWNS RESULT.
     */
    UnicodeRange* split(UChar c);

    /**
     * Finds the largest subrange of this range that is unused by the
     * given string.  A subrange is unused by a string if the string
     * contains no characters in that range.  If the given string
     * contains no characters in this range, then this range itself is
     * returned.
     *
     * CALLER OWNS RESULT.
     */
    UnicodeRange* largestUnusedSubrange(const UnicodeString& str) const;

private:

    // For UVector of UnicodeRange* objects
    static void deleter(void*);

};

#endif
