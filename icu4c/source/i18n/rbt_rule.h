/*
* Copyright © {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef RBT_RULE_H
#define RBT_RULE_H

#include "unicode/unistr.h"
#include "unicode/utrans.h"

class Replaceable;
class TransliterationRuleData;
class UnicodeFilter;

/**
 * A transliteration rule used by
 * <code>RuleBasedTransliterator</code>.
 * <code>TransliterationRule</code> is an immutable object.
 *
 * <p>A rule consists of an input pattern and an output string.  When
 * the input pattern is matched, the output string is emitted.  The
 * input pattern consists of zero or more characters which are matched
 * exactly (the key) and optional context.  Context must match if it
 * is specified.  Context may be specified before the key, after the
 * key, or both.  The key, preceding context, and following context
 * may contain variables.  Variables represent a set of Unicode
 * characters, such as the letters <i>a</i> through <i>z</i>.
 * Variables are detected by looking up each character in a supplied
 * variable list to see if it has been so defined. 
 *
 * @author Alan Liu
 */
class TransliterationRule {

public:

    /**
     * Constants returned by <code>getMatchDegree()</code> indicating
     * the degree of match between the text and this rule.
     * @see #getMatchDegree
     */
    enum {
        /**
         * Constant returned by <code>getMatchDegree()</code>
         * indicating a mismatch between the text and this rule.  One
         * or more characters of the context or key do not match the
         * text.
         */
        MISMATCH,

        /**
         * Constant returned by <code>getMatchDegree()</code>
         * indicating a partial match between the text and this rule.
         * All characters of the text match the corresponding context
         * or key, but more characters are required for a complete
         * match.  There are some key or context characters at the end
         * of the pattern that remain unmatched because the text isn't
         * long enough.
         */
        PARTIAL_MATCH,
        
        /**
         * Constant returned by <code>getMatchDegree()</code>
         * indicating a complete match between the text and this rule.
         * The text matches all context and key characters.
         */
        FULL_MATCH
    };

private:

    /**
     * The string that must be matched, consisting of the anteContext, key,
     * and postContext, concatenated together, in that order.  Some components
     * may be empty (zero length).
     * @see anteContextLength
     * @see keyLength
     */
    UnicodeString pattern;

    /**
     * The string that is emitted if the key, anteContext, and postContext
     * are matched.
     */
    UnicodeString output;

    /**
     * Array of segments.  These are segments of the input string that may be
     * referenced and appear in the output string.  Each segment is stored as an
     * offset, limit pair.  Segments are referenced by a 1-based index;
     * reference i thus includes characters at offset segments[2*i-2] to
     * segments[2*i-1]-1 in the pattern string.
     *
     * In the output string, a segment reference is indicated by a character in
     * a special range, as defined by RuleBasedTransliterator.Data.
     *
     * Most rules have no segments, in which case segments is null, and the
     * output string need not be checked for segment reference characters.
     */
    int32_t* segments;

    /**
     * The length of the string that must match before the key.  If
     * zero, then there is no matching requirement before the key.
     * Substring [0,anteContextLength) of pattern is the anteContext.
     */
    int32_t anteContextLength;

    /**
     * The length of the key.  Substring [anteContextLength,
     * anteContextLength + keyLength) is the key.

     */
    int32_t keyLength;

    /**
     * The position of the cursor after emitting the output string, from 0 to
     * output.length().  For most rules with no special cursor specification,
     * the cursorPos is output.length().
     */
    int32_t cursorPos;

public:

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
    TransliterationRule(const UnicodeString& input,
                        int32_t anteContextPos, int32_t postContextPos,
                        const UnicodeString& output,
                        int32_t cursorPos, int32_t cursorOffset,
                        int32_t* adoptedSegs,
                        UErrorCode& status);

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
    TransliterationRule(const UnicodeString& input,
                        int32_t anteContextPos, int32_t postContextPos,
                        const UnicodeString& output,
                        int32_t cursorPos,
                        UErrorCode& status);

    /**
     * Destructor.
     */
    virtual ~TransliterationRule();

    /**
     * Return the position of the cursor within the output string.
     * @return a value from 0 to <code>getOutput().length()</code>, inclusive.
     */
    virtual int32_t getCursorPos(void) const;

    /**
     * Return the preceding context length.  This method is needed to
     * support the <code>Transliterator</code> method
     * <code>getMaximumContextLength()</code>.
     */
    virtual int32_t getAnteContextLength(void) const;

    /**
     * Internal method.  Returns 8-bit index value for this rule.
     * This is the low byte of the first character of the key,
     * unless the first character of the key is a set.  If it's a
     * set, or otherwise can match multiple keys, the index value is -1.
     */
    int16_t getIndexValue(const TransliterationRuleData& data) const;

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
    int32_t replace(Replaceable& text, int32_t offset,
                    const TransliterationRuleData& data) const;

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
    UBool matchesIndexValue(uint8_t v,
                             const TransliterationRuleData& data) const;

    /**
     * Return true if this rule masks another rule.  If r1 masks r2 then
     * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
     * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
     * "[c]a>x" masks "[dc]a>y".
     */
    virtual UBool masks(const TransliterationRule& r2) const;

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
    virtual UBool matches(const Replaceable& text,
                          const UTransPosition& pos,
                          const TransliterationRuleData& data,
                          const UnicodeFilter* filter) const;

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
    virtual int32_t getMatchDegree(const Replaceable& text,
                                   const UTransPosition& pos,
                                   const TransliterationRuleData& data,
                                   const UnicodeFilter* filter) const;

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
    virtual int32_t getRegionMatchLength(const Replaceable& text,
                                         const UTransPosition& pos,
                                         const UnicodeString& templ,
                                         const TransliterationRuleData& data,
                                         const UnicodeFilter* filter) const;
    
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
    virtual UBool charMatches(UChar keyChar, UChar textChar,
                               const TransliterationRuleData& data,
                               const UnicodeFilter* filter) const;

private:

    void init(const UnicodeString& input,
              int32_t anteContextPos, int32_t postContextPos,
              const UnicodeString& output,
              int32_t cursorPos, int32_t cursorOffset,
              int32_t* adoptedSegs,
              UErrorCode& status);
};

#endif
