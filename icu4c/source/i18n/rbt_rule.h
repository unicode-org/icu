/*
* Copyright (C) {1999-2001}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef RBT_RULE_H
#define RBT_RULE_H

#include "unicode/unistr.h"
#include "unicode/utrans.h"
#include "unicode/unimatch.h"

class Replaceable;
class TransliterationRuleData;

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
     * The character at index i, where i < contextStart || i >= contextLimit,
     * is ETHER.  This allows explicit matching by rules and UnicodeSets
     * of text outside the context.  In traditional terms, this allows anchoring
     * at the start and/or end.
     */
    static const UChar ETHER;

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
     * >>> Duplicated in rbt_pars.cpp and rbt_rule.h <<<
     *
     * The segments array encodes information about parentheses-
     * enclosed regions of the input string.  These are referenced in
     * the output string using the notation $1, $2, etc.  Numbering is
     * in order of appearance of the left parenthesis.  Number is
     * one-based.  Segments are defined as start, limit pairs.
     * Segments may nest.
     * 
     * In order two avoid allocating two subobjects, the segments
     * array actually comprises two arrays.  The first is gives the
     * index values of the open and close parentheses in the order
     * they appear.  The second maps segment numbers to the indices of
     * the first array.  The two arrays have the same length.
     *
     * Example:  (a b(c d)e f)
     *            0 1 2 3 4 5 6
     *
     * First array: Indices are 0, 2, 4, and 6.

     * Second array: $1 is at 0 and 6, and $2 is at 2 and 4, so the
     * second array is 0, 3, 1 2 -- these give the indices in the
     * first array at which $1:open, $1:close, $2:open, and $2:close
     * occur.
     *
     * The final array is: 2, 7, 0, 2, 4, 6, -1, 2, 5, 3, 4, -1
     *
     * Each subarray is terminated with a -1, and two leading entries
     * give the number of segments and the offset to the first entry
     * of the second array.  In addition, the second array value are
     * all offset by 2 so they index directly into the final array.
     * The total array size is 4*segments[0] + 4.  The second index is
     * 2*segments[0] + 3.
     *
     * In the output string, a segment reference is indicated by a
     * character in a special range, as defined by
     * RuleBasedTransliterator.Data.
     *
     * Most rules have no segments, in which case segments is null, and the
     * output string need not be checked for segment reference characters.
     */
    int32_t* segments;

    /**
     * A value we compute from segments.  The first index into segments[]
     * that is >= anteContextLength.  That is, the first one that is within
     * the forward scanned part of the pattern -- the key or the postContext.
     * If there are no segments, this has the value -1.
     */
    int32_t firstKeySeg;

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

    /**
     * Miscellaneous attributes.
     */
    int8_t flags;

    /**
     * Flag attributes.
     */
    enum {
        ANCHOR_START = 1,
        ANCHOR_END   = 2,
    };

    /**
     * A reference to the data for this rule.  The data provides
     * lookup services for matchers and segments.
     */
    const TransliterationRuleData& data;

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
     * @param cursorPosition offset into output at which cursor is located, or -1 if
     * none.  If less than zero, then the cursor is placed after the
     * <code>output</code>; that is, -1 is equivalent to
     * <code>output.length()</code>.  If greater than
     * <code>output.length()</code> then an exception is thrown.
     * @param cursorOffset an offset to be added to cursorPos to position the
     * cursor either in the ante context, if < 0, or in the post context, if >
     * 0.  For example, the rule "abc{def} > | @@@ xyz;" changes "def" to
     * "xyz" and moves the cursor to before "a".  It would have a cursorOffset
     * of -3.
     * @param adoptedSegs array of 2n integers.  Each of n pairs consists of offset,
     * limit for a segment of the input string.  Characters in the output string
     * refer to these segments if they are in a special range determined by the
     * associated RuleBasedTransliterator.Data object.  May be null if there are
     * no segments.
     * @param anchorStart TRUE if the the rule is anchored on the left to
     * the context start
     * @param anchorEnd TRUE if the rule is anchored on the right to the
     * context limit
     */
    TransliterationRule(const UnicodeString& input,
                        int32_t anteContextPos, int32_t postContextPos,
                        const UnicodeString& outputStr,
                        int32_t cursorPosition, int32_t cursorOffset,
                        int32_t* adoptedSegs,
                        UBool anchorStart, UBool anchorEnd,
                        const TransliterationRuleData& data,
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
     * @param cursorPosition offset into output at which cursor is located, or -1 if
     * none.  If less than zero, then the cursor is placed after the
     * <code>output</code>; that is, -1 is equivalent to
     * <code>output.length()</code>.  If greater than
     * <code>output.length()</code> then an exception is thrown.
     */
    TransliterationRule(const UnicodeString& input,
                        int32_t anteContextPos, int32_t postContextPos,
                        const UnicodeString& outputStr,
                        int32_t cursorPosition,
                        const TransliterationRuleData& data,
                        UErrorCode& status);

    /**
     * Copy constructor.
     */
    TransliterationRule(TransliterationRule& other);

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
     * <code>getMaximumContextLength()</code>.  Internally, this is
     * implemented as the anteContextLength, optionally plus one if
     * there is a start anchor.  The one character anchor gap is
     * needed to make repeated incremental transliteration with
     * anchors work.
     */
    virtual int32_t getContextLength(void) const;

    /**
     * Internal method.  Returns 8-bit index value for this rule.
     * This is the low byte of the first character of the key,
     * unless the first character of the key is a set.  If it's a
     * set, or otherwise can match multiple keys, the index value is -1.
     */
    int16_t getIndexValue() const;

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
    UBool matchesIndexValue(uint8_t v) const;

    /**
     * Return true if this rule masks another rule.  If r1 masks r2 then
     * r1 matches any input string that r2 matches.  If r1 masks r2 and r2 masks
     * r1 then r1 == r2.  Examples: "a>x" masks "ab>y".  "a>x" masks "a[b]>y".
     * "[c]a>x" masks "[dc]a>y".
     */
    virtual UBool masks(const TransliterationRule& r2) const;

    /**
     * Attempt a match and replacement at the given position.  Return
     * the degree of match between this rule and the given text.  The
     * degree of match may be mismatch, a partial match, or a full
     * match.  A mismatch means at least one character of the text
     * does not match the context or key.  A partial match means some
     * context and key characters match, but the text is not long
     * enough to match all of them.  A full match means all context
     * and key characters match.
     * 
     * If a full match is obtained, perform a replacement, update pos,
     * and return U_MATCH.  Otherwise both text and pos are unchanged.
     * 
     * @param text the text
     * @param pos the position indices
     * @param incremental if TRUE, test for partial matches that may
     * be completed by additional text inserted at pos.limit.
     * @return one of <code>U_MISMATCH</code>,
     * <code>U_PARTIAL_MATCH</code>, or <code>U_MATCH</code>.  If
     * incremental is FALSE then U_PARTIAL_MATCH will not be returned.
     */
    UMatchDegree matchAndReplace(Replaceable& text,
                                 UTransPosition& pos,
                                 UBool incremental) const;

    /**
     * Create a rule string that represents this rule object.  Append
     * it to the given string.
     */
    virtual UnicodeString& toRule(UnicodeString& pat,
                                  UBool escapeUnprintable) const;
 private:

    void init(const UnicodeString& input,
              int32_t anteContextPos, int32_t postContextPos,
              const UnicodeString& output,
              int32_t cursorPos, int32_t cursorOffset,
              int32_t* adoptedSegs,
              UBool anchorStart, UBool anchorEnd,
              UErrorCode& status);

 private:

    friend class StringMatcher;

    static void appendToRule(UnicodeString& rule,
                             UChar32 c,
                             UBool isLiteral,
                             UBool escapeUnprintable,
                             UnicodeString& quoteBuf);
    
    static void appendToRule(UnicodeString& rule,
                             const UnicodeString& text,
                             UBool isLiteral,
                             UBool escapeUnprintable,
                             UnicodeString& quoteBuf);
};

#endif
