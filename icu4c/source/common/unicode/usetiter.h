/*
**********************************************************************
* Copyright (c) 2002-2004, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
*/
#ifndef USETITER_H
#define USETITER_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
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
 * while (it.nextRange()) {
 *   if (it.isString()) {
 *     processString(it.getString());
 *   } else {
 *     processCodepointRange(it.getCodepoint(), it.getCodepointEnd());
 *   }
 * }
 * </pre>
 * @author M. Davis
 * @stable ICU 2.4
 */
class U_COMMON_API UnicodeSetIterator : public UObject {

 protected:
    
    /**
     * Value of <tt>codepoint</tt> if the iterator points to a string.
     * If <tt>codepoint == IS_STRING</tt>, then examine
     * <tt>string</tt> for the current iteration result.
     * @stable ICU 2.4
     */
    enum { IS_STRING = -1 };

    /**
     * Current code point, or the special value <tt>IS_STRING</tt>, if
     * the iterator points to a string.
     * @stable ICU 2.4
     */
    UChar32 codepoint;

    /**
     * When iterating over ranges using <tt>nextRange()</tt>,
     * <tt>codepointEnd</tt> contains the inclusive end of the
     * iteration range, if <tt>codepoint != IS_STRING</tt>.  If
     * iterating over code points using <tt>next()</tt>, or if
     * <tt>codepoint == IS_STRING</tt>, then the value of
     * <tt>codepointEnd</tt> is undefined.
     * @stable ICU 2.4
     */
    UChar32 codepointEnd;

    /**
     * If <tt>codepoint == IS_STRING</tt>, then <tt>string</tt> points
     * to the current string.  If <tt>codepoint != IS_STRING</tt>, the
     * value of <tt>string</tt> is undefined.
     * @stable ICU 2.4
     */
    const UnicodeString* string;

 public:

    /**
     * Create an iterator over the given set.  The iterator is valid
     * only so long as <tt>set</tt> is valid.
     * @param set set to iterate over
     * @stable ICU 2.4
     */
    UnicodeSetIterator(const UnicodeSet& set);
        
    /**
     * Create an iterator over nothing.  <tt>next()</tt> and
     * <tt>nextRange()</tt> return false. This is a convenience
     * constructor allowing the target to be set later.
     * @stable ICU 2.4
     */
    UnicodeSetIterator();
        
    /**
     * Destructor.
     * @stable ICU 2.4
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
     * @stable ICU 2.4
     */
    inline UBool isString() const;

    /**
     * Returns the current code point, if <tt>isString()</tt> returned
     * false.  Otherwise returns an undefined result.
     * @stable ICU 2.4
     */
    inline UChar32 getCodepoint() const;

    /**
     * Returns the end of the current code point range, if
     * <tt>isString()</tt> returned false and <tt>nextRange()</tt> was
     * called.  Otherwise returns an undefined result.
     * @stable ICU 2.4
     */
    inline UChar32 getCodepointEnd() const;

    /**
     * Returns the current string, if <tt>isString()</tt> returned
     * true.  Otherwise returns an undefined result.
     * @stable ICU 2.4
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
     * @stable ICU 2.4
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
     * @stable ICU 2.4
     */
    UBool nextRange();
        
    /**
     * Sets this iterator to visit the elements of the given set and
     * resets it to the start of that set.  The iterator is valid only
     * so long as <tt>set</tt> is valid.
     * @param set the set to iterate over.
     * @stable ICU 2.4
     */
    void reset(const UnicodeSet& set);
        
    /**
     * Resets this iterator to the start of the set.
     * @stable ICU 2.4
     */
    void reset();
    
    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @stable ICU 2.4
     */
    static UClassID getStaticClassID();

    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @stable ICU 2.4
     */
    virtual UClassID getDynamicClassID() const;

    // ======================= PRIVATES ===========================
    
 protected:

    // endElement and nextElements are really UChar32's, but we keep
    // them as signed int32_t's so we can do comparisons with
    // endElement set to -1.  Leave them as int32_t's.
    /** The set
     * @stable ICU 2.4
     */
    const UnicodeSet* set;
    /** End range
     * @stable ICU 2.4
     */
    int32_t endRange;
    /** Range
     * @stable ICU 2.4
     */
    int32_t range;
    /** End element
     * @stable ICU 2.4
     */
    int32_t endElement;
    /** Next element
     * @stable ICU 2.4
     */
    int32_t nextElement;
    //UBool abbreviated;
    /** Next string
     * @stable ICU 2.4
     */
    int32_t nextString;
    /** String count
     * @stable ICU 2.4
     */
    int32_t stringCount;

    /** Copy constructor. Disallowed.
     * @stable ICU 2.4
     */
    UnicodeSetIterator(const UnicodeSetIterator&); // disallow

    /** Assignment operator. Disallowed.
     * @stable ICU 2.4
     */
    UnicodeSetIterator& operator=(const UnicodeSetIterator&); // disallow

    /** Load range
     * @stable ICU 2.4
     */
    virtual void loadRange(int32_t range);

};

inline UBool UnicodeSetIterator::isString() const {
    return codepoint == (UChar32)IS_STRING;
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
