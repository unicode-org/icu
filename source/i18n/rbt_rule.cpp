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
#include "unicode/rep.h"
#include "rbt_data.h"
#include "unicode/unifilt.h"
#include "unicode/uniset.h"

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
    output(theOutput),
    cursorPos(theCursorPos)
{
    if (U_FAILURE(status)) {
        return;
    }
    anteContextLength = theAnteContext.length();
    keyLength = theKey.length();
    pattern = theAnteContext;
    pattern.append(theKey).append(thePostContext);
    if (cursorPos < 0) {
        cursorPos = output.length();
    }
    if (cursorPos > output.length()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
}

/**
 * Construct a new rule with the given input, output text, and other
 * attributes.  A cursor position may be specified for the output text.
 * @param input input string, including key and optional ante and
 * post context
 * @param anteContextPos offset into input to end of ante context, or -1 if
 * none.  Must be <= input.length() if not -1.
 * @param postContextPos offset into input to start of post context, or -1
 * if none.  Must be <= input.length() if not -1, and must be >=
 * anteContextPos.
 * @param output output string
 * @param cursorPos offset into output at which cursor is located, or -1 if
 * none.  If less than zero, then the cursor is placed after the
 * <code>output</code>; that is, -1 is equivalent to
 * <code>output.length()</code>.  If greater than
 * <code>output.length()</code> then an exception is thrown.
 */
TransliterationRule::TransliterationRule(const UnicodeString& input,
                                         int32_t anteContextPos, int32_t postContextPos,
                                         const UnicodeString& output,
                                         int32_t cursorPos,
                                         UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }
    // Do range checks only when warranted to save time
    if (anteContextPos < 0) {
        anteContextLength = 0;
    } else {
        if (anteContextPos > input.length()) {
            // throw new IllegalArgumentException("Invalid ante context");
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        anteContextLength = anteContextPos;
    }
    if (postContextPos < 0) {
        keyLength = input.length() - anteContextLength;
    } else {
        if (postContextPos < anteContextLength ||
            postContextPos > input.length()) {
            // throw new IllegalArgumentException("Invalid post context");
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        keyLength = postContextPos - anteContextLength;
    }
    if (cursorPos < 0) {
        this->cursorPos = output.length();
    } else {
        if (cursorPos > output.length()) {
            // throw new IllegalArgumentException("Invalid cursor position");
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        this->cursorPos = cursorPos;
    }
    pattern = input;
    this->output = output;
}

TransliterationRule::~TransliterationRule() {}

/**
 * Return the length of the key.  Equivalent to <code>getKey().length()</code>.
 * @return the length of the match key.
 */
int32_t TransliterationRule::getKeyLength(void) const {
    return keyLength;
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
    return anteContextLength;
}

/**
 * Internal method.  Returns 8-bit index value for this rule.
 * This is the low byte of the first character of the key,
 * unless the first character of the key is a set.  If it's a
 * set, or otherwise can match multiple keys, the index value is -1.
 */
int16_t TransliterationRule::getIndexValue(const TransliterationRuleData& data) {
    if (anteContextLength == pattern.length()) {
        // A pattern with just ante context {such as foo)>bar} can
        // match any key.
        return -1;
    }
    UChar c = pattern.charAt(anteContextLength);
    return data.lookupSet(c) == NULL ? (c & 0xFF) : -1;
}

/**
 * Internal method.  Returns true if this rule matches the given
 * index value.  The index value is an 8-bit integer, 0..255,
 * representing the low byte of the first character of the key.
 * It matches this rule if it matches the first character of the
 * key, or if the first character of the key is a set, and the set
 * contains any character with a low byte equal to the index
 * value.  If the rule contains only ante context, as in foo)>bar,
 * then it will match any key.
 */
bool_t TransliterationRule::matchesIndexValue(uint8_t v,
                                   const TransliterationRuleData& data) {
    if (anteContextLength == pattern.length()) {
        // A pattern with just ante context {such as foo)>bar} can
        // match any key.
        return TRUE;
    }
    UChar c = pattern.charAt(anteContextLength);
    UnicodeSet* set = data.lookupSet(c);
    return set == NULL ? (uint8_t(c) == v) : set->containsIndexValue(v);
}

/**
 * Return true if this rule masks another rule.  If r1 masks r2 then
 * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
 * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
 * "[c]a>x" masks "[dc]a>y".
 */
bool_t TransliterationRule::masks(const TransliterationRule& r2) const {
    /* Rule r1 masks rule r2 if the string formed of the
     * antecontext, key, and postcontext overlaps in the following
     * way:
     *
     * r1:      aakkkpppp
     * r2:     aaakkkkkpppp
     *            ^
     * 
     * The strings must be aligned at the first character of the
     * key.  The length of r1 to the left of the alignment point
     * must be <= the length of r2 to the left; ditto for the
     * right.  The characters of r1 must equal (or be a superset
     * of) the corresponding characters of r2.  The superset
     * operation should be performed to check for UnicodeSet
     * masking.
     */

    /* LIMITATION of the current mask algorithm: Some rule
     * maskings are currently not detected.  For example,
     * "{Lu}]a>x" masks "A]a>y".  This can be added later. TODO
     */

    int32_t len = pattern.length();
    int32_t left = anteContextLength;
    int32_t left2 = r2.anteContextLength;
    int32_t right = len - left;
    int32_t right2 = r2.pattern.length() - left2;
    return left <= left2 && right <= right2 &&
        0 == r2.pattern.compare(left2 - left, len, pattern);
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
    // Match anteContext, key, and postContext
    return regionMatches(text, start, limit, result,
                         cursor - anteContextLength,
                         pattern, data, filter);
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
    // Match anteContext, key, and postContext
    return regionMatches(text, start, limit,
                         cursor - anteContextLength,
                         pattern, data, filter);
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
    int len = getRegionMatchLength(text, start, limit, cursor - anteContextLength,
                                   pattern, data, filter);
    return len < anteContextLength ? MISMATCH :
        (len < pattern.length() ? PARTIAL_MATCH : FULL_MATCH);
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
