/*
**********************************************************************
* Copyright (c) 2002, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* $Source: /xsrl/Nsvn/icu/icu/source/common/unicode/usetiter.h,v $ 
* $Revision: 1.1 $
**********************************************************************
*/
#ifndef USETITER_H
#define USETITER_H

#include "unicode/unistr.h"

U_NAMESPACE_BEGIN

class UnicodeSet;
class UnicodeString;

/**
 * UnicodeSetIterator iterates over the contents of a UnicodeSet.  It
 * iterates over either code points or code point ranges.  After all
 * code points or ranges have been returned, it returns the
 * multicharacter strings of the UnicodSet, if any.
 *
 * <p>To iterate over code points, use a loop like this:
 * <pre>
 * UnicodeSetIterator it(set);
 * while (set.next()) {
 *   if (set.isString()) {
 *     processString(set.getString());
 *   } else {
 *     processCodepoint(set.getCodepoint());
 *   }
 * }
 * </pre>
 *
 * <p>To iterate over code point ranges, use a loop like this:
 * <pre>
 * UnicodeSetIterator it(set);
 * while (set.nextRange()) {
 *   if (set.isString()) {
 *     processString(set.getString());
 *   } else {
 *     processCodepointRange(set.getCodepoint(), set.getCodepointEnd());
 *   }
 * }
 * </pre>
 * @author M. Davis
 * @draft
 */
class U_COMMON_API UnicodeSetIterator {

 protected:
	
    /**
     * Value of <tt>codepoint</tt> if the iterator points to a string.
     * If <tt>codepoint == IS_STRING</tt>, then examine
     * <tt>string</tt> for the current iteration result.
     */
	enum { IS_STRING = -1 };

	/**
     * Current code point, or the special value <tt>IS_STRING</tt>, if
     * the iterator points to a string.
     */
	UChar32 codepoint;

    /**
     * When iterating over ranges using <tt>nextRange()</tt>,
     * <tt>codepointEnd</tt> contains the inclusive end of the
     * iteration range, if <tt>codepoint != IS_STRING</tt>.  If
     * iterating over code points using <tt>next()</tt>, or if
     * <tt>codepoint == IS_STRING</tt>, then the value of
     * <tt>codepointEnd</tt> is undefined.
     */
	UChar32 codepointEnd;

    /**
     * If <tt>codepoint == IS_STRING</tt>, then <tt>string</tt> points
     * to the current string.  If <tt>codepoint != IS_STRING</tt>, the
     * value of <tt>string</tt> is undefined.
     */
	const UnicodeString* string;

 public:

    /**
     * Create an iterator over the given set.  The iterator is valid
     * only so long as <tt>set</tt> is valid.
     * @param set set to iterate over
     */
    UnicodeSetIterator(const UnicodeSet& set);
        
    /**
     * Create an iterator over nothing.  <tt>next()</tt> and
     * <tt>nextRange()</tt> return false. This is a convenience
     * constructor allowing the target to be set later.
     */
    UnicodeSetIterator();
        
    /**
     * Destructor.
     */
    virtual ~UnicodeSetIterator();

    /**
     * Returns true if the current element is a string.  If so, the
     * caller can retrieve it with <tt>getString()</tt>.  If this
     * method returns false, the current element is a code point or
     * code point range, depending on whether <tt>next()</tt> or
     * <tt>nextRange()</tt> was called, and the caller can retrieve it
     * with <tt>getCodepoint()</tt> and, for a range,
     * <tt>getCodepointEnd()</tt>.
     */
    inline UBool isString() const;

    /**
     * Returns the current code point, if <tt>isString()</tt> returned
     * false.  Otherwise returns an undefined result.
     */
    inline UChar32 getCodepoint() const;

    /**
     * Returns the end of the current code point range, if
     * <tt>isString()</tt> returned false and <tt>nextRange()</tt> was
     * called.  Otherwise returns an undefined result.
     */
    inline UChar32 getCodepointEnd() const;

    /**
     * Returns the current string, if <tt>isString()</tt> returned
     * true.  Otherwise returns an undefined result.
     */
    inline const UnicodeString& getString() const;

    /**
     * Returns the next element in the set, either a single code point
     * or a string.  If there are no more elements in the set, return
     * false.  If <tt>codepoint == IS_STRING</tt>, the value is a
     * string in the <tt>string</tt> field.  Otherwise the value is a
     * single code point in the <tt>codepoint</tt> field.
     * 
     * <p>The order of iteration is all code points in sorted order,
     * followed by all strings sorted order.  <tt>codepointEnd</tt> is
     * undefined after calling this method.  <tt>string</tt> is
     * undefined unless <tt>codepoint == IS_STRING</tt>.  Do not mix
     * calls to <tt>next()</tt> and <tt>nextRange()</tt> without
     * calling <tt>reset()</tt> between them.  The results of doing so
     * are undefined.
     *
     * @return true if there was another element in the set and this
     * object contains the element.
     */
    UBool next();
        
    /**
     * Returns the next element in the set, either a code point range
     * or a string.  If there are no more elements in the set, return
     * false.  If <tt>codepoint == IS_STRING</tt>, the value is a
     * string in the <tt>string</tt> field.  Otherwise the value is a
     * range of one or more code points from <tt>codepoint</tt> to
     * <tt>codepointeEnd</tt> inclusive.
     * 
     * <p>The order of iteration is all code points ranges in sorted
     * order, followed by all strings sorted order.  Ranges are
     * disjoint and non-contiguous.  <tt>string</tt> is undefined
     * unless <tt>codepoint == IS_STRING</tt>.  Do not mix calls to
     * <tt>next()</tt> and <tt>nextRange()</tt> without calling
     * <tt>reset()</tt> between them.  The results of doing so are
     * undefined.
     *
     * @return true if there was another element in the set and this
     * object contains the element.
     */
    UBool nextRange();
        
    /**
     * Sets this iterator to visit the elements of the given set and
     * resets it to the start of that set.  The iterator is valid only
     * so long as <tt>set</tt> is valid.
     * @param set the set to iterate over.
     */
    void reset(const UnicodeSet& set);
        
    /**
     * Resets this iterator to the start of the set.
     */
    void reset();
    
    // ======================= PRIVATES ===========================
    
 protected:

    // endElement and nextElements are really UChar32's, but we keep
    // them as signed int32_t's so we can do comparisons with
    // endElement set to -1.  Leave them as int32_t's.
    const UnicodeSet* set;
    int32_t endRange;
    int32_t range;
    int32_t endElement;
    int32_t nextElement;
    //UBool abbreviated;
    int32_t nextString;
    int32_t stringCount;

    UnicodeSetIterator(const UnicodeSetIterator&); // disallow

    UnicodeSetIterator& operator=(const UnicodeSetIterator&); // disallow

    virtual void loadRange(int32_t range);
};

inline UBool UnicodeSetIterator::isString() const {
    return codepoint == IS_STRING;
}

inline UChar32 UnicodeSetIterator::getCodepoint() const {
    return codepoint;
}

inline UChar32 UnicodeSetIterator::getCodepointEnd() const {
    return codepointEnd;
}

inline const UnicodeString& UnicodeSetIterator::getString() const {
    return *string;
}

U_NAMESPACE_END

#endif
