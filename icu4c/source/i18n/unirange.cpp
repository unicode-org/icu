/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "unirange.h"
#include "uvector.h"
#include "unistr.h"

UnicodeRange::UnicodeRange(UChar theStart, int32_t theLength) {
    start = theStart;
    length = theLength;
}

UnicodeRange* UnicodeRange::clone() const {
    return new UnicodeRange(start, length);
}

/**
 * CALLER OWNS RESULT.
 */
bool_t UnicodeRange::contains(UChar c) const {
    return c >= start && (c - start) < length;
}

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
UnicodeRange* UnicodeRange::split(UChar c) {
    if (c == start) {
        ++start;
        --length;
        return 0;
    } else if (c - start == length - 1) {
        --length;
        return 0;
    } else {
        ++c;
        UnicodeRange* r = new UnicodeRange(c, start + length - c);
        length = --c - start;
        return r;
    }
}

/**
 * Finds the largest unused subrange by the given string.  A
 * subrange is unused by a string if the string contains no
 * characters in that range.  If the given string contains no
 * characters in this range, then this range itself is
 * returned.
 *
 * CALLER OWNS RESULT.
 */
UnicodeRange*
UnicodeRange::largestUnusedSubrange(const UnicodeString& str) const {
    int32_t n = str.length();

    UVector v;
    v.setDeleter(UnicodeRange::deleter);
    v.addElement(clone());
    for (int32_t i=0; i<n; ++i) {
        UChar c = str.charAt(i);
        if (contains(c)) {
            for (int32_t j=0; j<v.size(); ++j) {
                UnicodeRange* r = (UnicodeRange*) v.elementAt(j);
                if (r->contains(c)) {
                    r = r->split(c);
                    if (r != 0) {
                        v.addElement(r);
                    }
                    break;
                }
            }
        }
    }

    UnicodeRange* bestRange = 0;
    int32_t ibest = -1;
    for (int32_t j=0; j<v.size(); ++j) {
        UnicodeRange* r = (UnicodeRange*) v.elementAt(j);
        if (bestRange == 0 || r->length > bestRange->length) {
            bestRange = r;
            ibest = j;
        }
    }

    v.orphanElementAt(ibest); // So bestRange doesn't get deleted

    return bestRange;
}

// For UVector of UnicodeRange* objects
void UnicodeRange::deleter(void* e) {
    delete (UnicodeRange*) e;
}
