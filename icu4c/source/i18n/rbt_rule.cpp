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
 * @param adoptedSegs array of 2n integers.  Each of n pairs consists of offset,
 * limit for a segment of the input string.  Characters in the output string
 * refer to these segments if they are in a special range determined by the
 * associated RuleBasedTransliterator.Data object.  May be null if there are
 * no segments.
 */
TransliterationRule::TransliterationRule(const UnicodeString& input,
                                         int32_t anteContextPos, int32_t postContextPos,
                                         const UnicodeString& output,
                                         int32_t cursorPos, int32_t cursorOffset,
                                         int32_t* adoptedSegs,
                                         UErrorCode& status) {
    init(input, anteContextPos, postContextPos,
         output, cursorPos, cursorOffset, adoptedSegs, status);
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
    init(input, anteContextPos, postContextPos,
         output, cursorPos, 0, NULL, status);
}

void TransliterationRule::init(const UnicodeString& input,
                               int32_t anteContextPos, int32_t postContextPos,
                               const UnicodeString& output,
                               int32_t cursorPos, int32_t cursorOffset,
                               int32_t* adoptedSegs,
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
        cursorPos = output.length();
    } else {
        if (cursorPos > output.length()) {
            // throw new IllegalArgumentException("Invalid cursor position");
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
    }
    this->cursorPos = cursorPos + cursorOffset;
    pattern = input;
    this->output = output;
    // We don't validate the segments array.  The caller must
    // guarantee that the segments are well-formed.
    this->segments = adoptedSegs;
}

TransliterationRule::~TransliterationRule() {
    delete[] segments;
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
int16_t TransliterationRule::getIndexValue(const TransliterationRuleData& data) const {
    if (anteContextLength == pattern.length()) {
        // A pattern with just ante context {such as foo)>bar} can
        // match any key.
        return -1;
    }
    UChar c = pattern.charAt(anteContextLength);
    return data.lookupSet(c) == NULL ? (c & 0xFF) : -1;
}

/**
 * Do a replacement of the input pattern with the output text in
 * the given string, at the given offset.  This method assumes
 * that a match has already been found in the given text at the
 * given position.
 * @param text the text containing the substring to be replaced
 * @param offset the offset into the text at which the pattern
 * matches.  This is the offset to the point after the ante
 * context, if any, and before the match string and any post
 * context.
 * @param data the RuleBasedTransliterator.Data object specifying
 * context for this transliterator.
 * @return the change in the length of the text
 */
int32_t TransliterationRule::replace(Replaceable& text, int32_t offset,
                                     const TransliterationRuleData& data) const {
    if (segments == NULL) {
        text.handleReplaceBetween(offset, offset + keyLength, output);
        return output.length() - keyLength;
    } else {
        /* When there are segments to be copied, use the Replaceable.copy()
         * API in order to retain out-of-band data.  Copy everything to the
         * point after the key, then delete the key.  That is, copy things
         * into offset + keyLength, then replace offset .. offset +
         * keyLength with the empty string.
         *
         * Minimize the number of calls to Replaceable.replace() and
         * Replaceable.copy().
         */
        int32_t textStart = offset - anteContextLength;
        int32_t dest = offset + keyLength; // copy new text to here
        UnicodeString buf;
        for (int32_t i=0; i<output.length(); ++i) {
            UChar c = output.charAt(i);
            int32_t b = data.lookupSegmentReference(c);
            if (b < 0) {
                // Accumulate straight (non-segment) text.
                buf.append(c);
            } else {
                // Insert any accumulated straight text.
                if (buf.length() > 0) {
                    text.handleReplaceBetween(dest, dest, buf);
                    dest += buf.length();
                    buf.remove();
                }
                // Copy segment with out-of-band data
                b *= 2;
                text.copy(textStart + segments[b],
                          textStart + segments[b+1], dest);
                dest += segments[b+1] - segments[b];
            }

        }
        // Insert any accumulated straight text.
        if (buf.length() > 0) {
            text.handleReplaceBetween(dest, dest, buf);
            dest += buf.length();
        }
        // Delete the key
        buf.remove();
        text.handleReplaceBetween(offset, offset + keyLength, buf);
        return dest - (offset + keyLength) - keyLength;
    }
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
UBool TransliterationRule::matchesIndexValue(uint8_t v,
                                   const TransliterationRuleData& data) const {
    if (anteContextLength == pattern.length()) {
        // A pattern with just ante context {such as foo)>bar} can
        // match any key.
        return TRUE;
    }
    UChar c = pattern.charAt(anteContextLength);
    const UnicodeSet* set = data.lookupSet(c);
    return set == NULL ? (uint8_t(c) == v) : set->containsIndexValue(v);
}

/**
 * Return true if this rule masks another rule.  If r1 masks r2 then
 * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
 * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
 * "[c]a>x" masks "[dc]a>y".
 */
UBool TransliterationRule::masks(const TransliterationRule& r2) const {
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
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 */
UBool TransliterationRule::matches(const Replaceable& text,
                                    int32_t start, int32_t limit,
                                    int32_t cursor,
                                    const TransliterationRuleData& data,
                                    const UnicodeFilter* filter) const {
    // Match anteContext, key, and postContext
    cursor -= anteContextLength;
    if (cursor < start || (cursor + pattern.length()) > limit) {
        return FALSE;
    }
    for (int32_t i=0; i<pattern.length(); ++i, ++cursor) {
        if (!charMatches(pattern.charAt(i), text.charAt(cursor),
                         data, filter)) {
            return FALSE;
        }
    }
    return TRUE;
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
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
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
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
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
 * <tt>filter.contains()</tt> returns <tt>false</tt> will not be
 * altered by this transliterator.  If <tt>filter</tt> is
 * <tt>null</tt> then no filtering is applied.
 */
UBool TransliterationRule::charMatches(UChar keyChar, UChar textChar,
                                        const TransliterationRuleData& data,
                                        const UnicodeFilter* filter) const {
    const UnicodeSet* set = 0;
    return (filter == 0 || filter->contains(textChar)) &&
        (((set = data.lookupSet(keyChar)) == 0) ?
         keyChar == textChar : set->contains(textChar));
}
