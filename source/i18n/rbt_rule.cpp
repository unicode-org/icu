/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#include "rbt_rule.h"
#include "rep.h"
#include "rbt_data.h"
#include "unifilt.h"
#include "uniset.h"

/**
 * Construct a new rule with the given key, output text, and other
 * attributes.  Zero, one, or two context strings may be specified.  A
 * cursor position may be specified for the output text.
 * @param key the string to match
 * @param output the string to produce when the <code>key</code> is seen
 * @param anteContext if not null and not empty, then it must be matched
 * before the <code>key</code>
 * @param postContext if not null and not empty, then it must be matched
 * after the <code>key</code>
 * @param cursorPos a position for the cursor after the <code>output</code>
 * is emitted.  If less than zero, then the cursor is placed after the
 * <code>output</code>; that is, -1 is equivalent to
 * <code>output.length()</code>.  If greater than
 * <code>output.length()</code> then an exception is thrown.
 * @exception IllegalArgumentException if the cursor position is out of
 * range.
 */
TransliterationRule::TransliterationRule(const UnicodeString& theKey,
                                         const UnicodeString& theOutput,
                                         const UnicodeString& theAnteContext,
                                         const UnicodeString& thePostContext,
                                         int32_t theCursorPos,
                                         UErrorCode &status) :
    key(theKey), output(theOutput),
    anteContext(theAnteContext),
    postContext(thePostContext),
    cursorPos(theCursorPos),
    maskKey(0) {

    if (U_FAILURE(status)) {
        return;
    }

    if (cursorPos < 0) {
        cursorPos = output.length();
    }
    if (cursorPos > output.length()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
    /* The mask key is needed when we are adding individual rules to a rule
     * set, for performance.  Here are the numbers: Without mask key, 13.0
     * seconds.  With mask key, 6.2 seconds.  However, once the rules have
     * been added to the set, then they can be discarded to free up space.
     * This is what the freeze() method does.  After freeze() has been
     * called, the method masks() must NOT be called.
     */
    maskKey = new UnicodeString(key);
    if (maskKey == 0) {
        status = U_MEMORY_ALLOCATION_ERROR;
    } else {
        maskKey->append(postContext);
    }
}

TransliterationRule::~TransliterationRule() {
    delete maskKey;
}

/**
 * Return the length of the key.  Equivalent to <code>getKey().length()</code>.
 * @return the length of the match key.
 */
int32_t TransliterationRule::getKeyLength(void) const {
    return key.length();
}

/**
 * Return the key.
 * @return the match key.
 */
const UnicodeString& TransliterationRule::getKey(void) const {
    return key;
}

/**
 * Return the output string.
 * @return the output string.
 */
const UnicodeString& TransliterationRule::getOutput(void) const {
    return output;
}

/**
 * Return the position of the cursor within the output string.
 * @return a value from 0 to <code>getOutput().length()</code>, inclusive.
 */
int32_t TransliterationRule::getCursorPos(void) const {
    return cursorPos;
}

/**
 * Return the preceding context length.  This method is needed to
 * support the <code>Transliterator</code> method
 * <code>getMaximumContextLength()</code>.
 */
int32_t TransliterationRule::getAnteContextLength(void) const {
    return anteContext.length();
}

/**
 * Return true if this rule masks another rule.  If r1 masks r2 then
 * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
 * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
 * "[c]a>x" masks "[dc]a>y".
 *
 * <p>This method must not be called after freeze() is called.
 */
bool_t TransliterationRule::masks(const TransliterationRule& r2) const {
    /* There are three cases of masking.  In each instance, rule1
     * masks rule2.
     *
     * 1. KEY mask: len(key1) < len(key2), key2 starts with key1.
     *
     * 2. PREFIX mask: key1 == key2, len(prefix1) < len(prefix2),
     * prefix2 ends with prefix1, suffix2 starts with suffix1.
     *
     * 3. SUFFIX mask: key1 == key2, len(suffix1) < len(suffix2),
     * prefix2 ends with prefix1, suffix2 starts with suffix1.
     */

    /* LIMITATION of the current mask algorithm: Some rule
     * maskings are currently not detected.  For example,
     * "{Lu}]a>x" masks "A]a>y".  To detect these sorts of masking,
     * we need a subset operator on UnicodeSet objects, which we
     * currently do not have.  This can be added later.
     */
    return ((maskKey->length() < r2.maskKey->length() &&
             r2.maskKey->startsWith(*maskKey)) ||
            (r2.anteContext.length() != 0 && *maskKey == *r2.maskKey &&
             ((anteContext.length() == 0) ||
              (anteContext.length() < r2.anteContext.length() &&
               r2.anteContext.endsWith(anteContext)))));
}

/**
 * Free up space.  Once this method is called, masks() must NOT be called.
 * If it is called, an exception will be thrown.
 */
void TransliterationRule::freeze(void) {
    delete maskKey;
    maskKey = 0;
}

/**
 * Return true if this rule matches the given text.  The text being matched
 * occupies a virtual buffer consisting of the contents of
 * <code>result</code> concatenated to a substring of <code>text</code>.
 * The substring is specified by <code>start</code> and <code>limit</code>.
 * The value of <code>cursor</code> is an index into this virtual buffer,
 * from 0 to the length of the buffer.  In terms of the parameters,
 * <code>cursor</code> must be between 0 and <code>result.length() + limit -
 * start</code>.
 * @param text the untranslated text
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param result translated text so far
 * @param cursor position at which to translate next, an offset into result.
 * If greater than or equal to result.length(), represents offset start +
 * cursor - result.length() into text.
 * @param filter the filter.  Any character for which
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 */
bool_t TransliterationRule::matches(const UnicodeString& text,
                                    int32_t start, int32_t limit,
                                    const UnicodeString& result,
                                    int32_t cursor,
                                    const TransliterationRuleData& data,
                                    const UnicodeFilter* filter) const {
    return
        (anteContext.length() == 0
         || regionMatches(text, start, limit, result,
                          cursor - anteContext.length(),
                          anteContext, data, filter)) &&
        regionMatches(text, start, limit, result, cursor,
                      key, data, filter) &&
        (postContext.length() == 0
         || regionMatches(text, start, limit, result,
                          cursor + key.length(),
                          postContext, data, filter));
}

/**
 * Return true if this rule matches the given text.
 * @param text the text, both translated and untranslated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param cursor position at which to translate next, representing offset
 * into text.  This value must be between <code>start</code> and
 * <code>limit</code>.
 * @param filter the filter.  Any character for which
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 */
bool_t TransliterationRule::matches(const Replaceable& text,
                                    int32_t start, int32_t limit,
                                    int32_t cursor,
                                    const TransliterationRuleData& data,
                                    const UnicodeFilter* filter) const {
    return
        (anteContext.length() == 0
         || regionMatches(text, start, limit, cursor - anteContext.length(),
                          anteContext, data, filter)) &&
        regionMatches(text, start, limit, cursor,
                      key, data, filter) &&
        (postContext.length() == 0
         || regionMatches(text, start, limit, cursor + key.length(),
                          postContext, data, filter));
}

/**
 * Return the degree of match between this rule and the given text.  The
 * degree of match may be mismatch, a partial match, or a full match.  A
 * mismatch means at least one character of the text does not match the
 * context or key.  A partial match means some context and key characters
 * match, but the text is not long enough to match all of them.  A full
 * match means all context and key characters match.
 * @param text the text, both translated and untranslated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param cursor position at which to translate next, representing offset
 * into text.  This value must be between <code>start</code> and
 * <code>limit</code>.
 * @param filter the filter.  Any character for which
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 * @return one of <code>MISMATCH</code>, <code>PARTIAL_MATCH</code>, or
 * <code>FULL_MATCH</code>.
 * @see #MISMATCH
 * @see #PARTIAL_MATCH
 * @see #FULL_MATCH
 */
int32_t TransliterationRule::getMatchDegree(const Replaceable& text,
                                            int32_t start, int32_t limit,
                                            int32_t cursor,
                                            const TransliterationRuleData& data,
                                            const UnicodeFilter* filter) const {
    if (anteContext.length() != 0
        && !regionMatches(text, start, limit, cursor - anteContext.length(),
                          anteContext, data, filter)) {
        return MISMATCH;
    }
    int32_t len = getRegionMatchLength(text, start, limit, cursor,
                                       key, data, filter);
    if (len < 0) {
        return MISMATCH;
    }
    if (len < key.length()) {
        return PARTIAL_MATCH;
    }
    if (postContext.length() == 0) {
        return FULL_MATCH;
    }
    len = getRegionMatchLength(text, start, limit,
                               cursor + key.length(),
                               postContext, data, filter);
    return (len < 0) ? MISMATCH
                     : ((len == postContext.length()) ? FULL_MATCH
                                                      : PARTIAL_MATCH);
}

/**
 * Return true if a template matches the text.  The entire length of the
 * template is compared to the text at the cursor.  As in
 * <code>matches()</code>, the text being matched occupies a virtual buffer
 * consisting of the contents of <code>result</code> concatenated to a
 * substring of <code>text</code>.  See <code>matches()</code> for details.
 * @param text the untranslated text
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param result translated text so far
 * @param cursor position at which to translate next, an offset into result.
 * If greater than or equal to result.length(), represents offset start +
 * cursor - result.length() into text.
 * @param templ the text to match against.  All characters must match.
 * @param data a dictionary of variables mapping <code>Character</code>
 * to <code>UnicodeSet</code>
 * @param filter the filter.  Any character for which
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 * @return true if there is a match
 */
bool_t TransliterationRule::regionMatches(const UnicodeString& text,
                                          int32_t start, int32_t limit,
                                          const UnicodeString& result,
                                          int32_t cursor,
                                          const UnicodeString& templ,
                                          const TransliterationRuleData& data,
                                          const UnicodeFilter* filter) const {
    int32_t rlen = result.length();
    if (cursor < 0
        || (cursor + templ.length()) > (rlen + limit - start)) {
        return FALSE;
    }
    for (int32_t i=0; i<templ.length(); ++i, ++cursor) {
        if (!charMatches(templ.charAt(i),
                         cursor < rlen ? result.charAt(cursor)
                                       : text.charAt(cursor - rlen + start),
                         data, filter)) {
            return FALSE;
        }
    }
    return TRUE;
}

/**
 * Return true if a template matches the text.  The entire length of the
 * template is compared to the text at the cursor.
 * @param text the text, both translated and untranslated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param cursor position at which to translate next, representing offset
 * into text.  This value must be between <code>start</code> and
 * <code>limit</code>.
 * @param templ the text to match against.  All characters must match.
 * @param data a dictionary of variables mapping <code>Character</code>
 * to <code>UnicodeSet</code>
 * @param filter the filter.  Any character for which
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 * @return true if there is a match
 */
bool_t TransliterationRule::regionMatches(const Replaceable& text,
                                          int32_t start, int32_t limit,
                                          int32_t cursor,
                                          const UnicodeString& templ,
                                          const TransliterationRuleData& data,
                                          const UnicodeFilter* filter) const {
    if (cursor < start
        || (cursor + templ.length()) > limit) {
        return FALSE;
    }
    for (int32_t i=0; i<templ.length(); ++i, ++cursor) {
        if (!charMatches(templ.charAt(i), text.charAt(cursor),
                         data, filter)) {
            return FALSE;
        }
    }
    return TRUE;
}

/**
 * Return the number of characters of the text that match this rule.  If
 * there is a mismatch, return -1.  If the text is not long enough to match
 * any characters, return 0.
 * @param text the text, both translated and untranslated
 * @param start the beginning index, inclusive; <code>0 <= start
 * <= limit</code>.
 * @param limit the ending index, exclusive; <code>start <= limit
 * <= text.length()</code>.
 * @param cursor position at which to translate next, representing offset
 * into text.  This value must be between <code>start</code> and
 * <code>limit</code>.
 * @param templ the text to match against.  All characters must match.
 * @param data a dictionary of variables mapping <code>Character</code>
 * to <code>UnicodeSet</code>
 * @param filter the filter.  Any character for which
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 * @return -1 if there is a mismatch, 0 if the text is not long enough to
 * match any characters, otherwise the number of characters of text that
 * match this rule.
 */
int32_t TransliterationRule::getRegionMatchLength(const Replaceable& text,
                                          int32_t start,
                                          int32_t limit, int32_t cursor,
                                          const UnicodeString& templ,
                                          const TransliterationRuleData& data,
                                          const UnicodeFilter* filter) const {
    if (cursor < start) {
        return -1;
    }
    int32_t i;
    for (i=0; i<templ.length() && cursor<limit; ++i, ++cursor) {
        if (!charMatches(templ.charAt(i), text.charAt(cursor),
                         data, filter)) {
            return -1;
        }
    }
    return i;
}

/**
 * Return true if the given key matches the given text.  This method
 * accounts for the fact that the key character may represent a character
 * set.  Note that the key and text characters may not be interchanged
 * without altering the results.
 * @param keyChar a character in the match key
 * @param textChar a character in the text being transliterated
 * @param data a dictionary of variables mapping <code>Character</code>
 * to <code>UnicodeSet</code>
 * @param filter the filter.  Any character for which
 * <tt>filter.isIn()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 */
bool_t TransliterationRule::charMatches(UChar keyChar, UChar textChar,
                                        const TransliterationRuleData& data,
                                        const UnicodeFilter* filter) const {
    UnicodeSet* set = 0;
    return (filter == 0 || filter->isIn(textChar)) &&
        ((set = data.lookupSet(keyChar)) == 0) ?
        keyChar == textChar : set->contains(textChar);
}
