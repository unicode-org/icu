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
    TransliterationRule(const UnicodeString& theKey,
                        const UnicodeString& theOutput,
                        const UnicodeString& theAnteContext,
                        const UnicodeString& thePostContext,
                        int32_t theCursorPos,
                        UErrorCode &status);

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
     * Return the length of the key.  Equivalent to <code>getKey().length()</code>.
     * @return the length of the match key.
     */
    virtual int32_t getKeyLength(void) const;

    /**
     * Return the output string.
     * @return the output string.
     */
    virtual const UnicodeString& getOutput(void) const;

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

private:
    friend class TransliterationRuleSet;

    /**
     * Internal method.  Returns 8-bit index value for this rule.
     * This is the low byte of the first character of the key,
     * unless the first character of the key is a set.  If it's a
     * set, or otherwise can match multiple keys, the index value is -1.
     */
    int16_t getIndexValue(const TransliterationRuleData& data);

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
    bool_t matchesIndexValue(uint8_t v,
                             const TransliterationRuleData& data);

public:
    /**
     * Return true if this rule masks another rule.  If r1 masks r2 then
     * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
     * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
     * "[c]a>x" masks "[dc]a>y".
     */
    virtual bool_t masks(const TransliterationRule& r2) const;

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
    virtual bool_t matches(const UnicodeString& text,
                           int32_t start, int32_t limit,
                           const UnicodeString& result,
                           int32_t cursor,
                           const TransliterationRuleData& data,
                           const UnicodeFilter* filter) const;

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
    virtual bool_t matches(const Replaceable& text,
                           int32_t start, int32_t limit,
                           int32_t cursor,
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
                                   int32_t start, int32_t limit,
                                   int32_t cursor,
                                   const TransliterationRuleData& data,
                                   const UnicodeFilter* filter) const;

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
    virtual bool_t regionMatches(const UnicodeString& text,
                                 int32_t start, int32_t limit,
                                 const UnicodeString& result,
                                 int32_t cursor,
                                 const UnicodeString& templ,
                                 const TransliterationRuleData& data,
                                 const UnicodeFilter* filter) const;

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
    virtual bool_t regionMatches(const Replaceable& text,
                                 int32_t start, int32_t limit,
                                 int32_t cursor,
                                 const UnicodeString& templ,
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
    virtual int32_t getRegionMatchLength(const Replaceable& text, int32_t start,
                                         int32_t limit, int32_t cursor,
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
    virtual bool_t charMatches(UChar keyChar, UChar textChar,
                               const TransliterationRuleData& data,
                               const UnicodeFilter* filter) const;
};

#endif
