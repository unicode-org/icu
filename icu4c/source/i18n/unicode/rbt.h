/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   11/17/99    aliu        Creation.
**********************************************************************
*/
#ifndef RBT_H
#define RBT_H

#include "unicode/translit.h"
#include "unicode/utypes.h"

class TransliterationRuleData;

/**
 * A transliterator that reads a set of rules in order to determine how to perform
 * translations. Rules are stored in resource bundles indexed by name. Rules are separated by
 * semicolons (';'). To include a literal semicolon, prefix it with a backslash ('\;').
 * Whitespace, as defined by <code>Character.isWhitespace()</code>, is ignored. If the first
 * non-blank character on a line is '#', the entire line is ignored as a comment. </p>
 * 
 * <p>Each set of rules consists of two groups, one forward, and one reverse. This is a
 * convention that is not enforced; rules for one direction may be omitted, with the result
 * that translations in that direction will not modify the source text. </p>
 * 
 * <p><b>Rule syntax</b> </p>
 * 
 * <p>Rule statements take one of the following forms: 
 * 
 * <dl>
 *   <dt><code>alefmadda=\u0622</code></dt>
 *   <dd><strong>Variable definition.</strong> The name on the left is assigned the character or
 *     expression on the right. Names may not contain any special characters (see list below).
 *     Duplicate names (including duplicates of simple variables or category names) cause an
 *     exception to be thrown. If the right hand side consists of one character, then the
 *     variable stands for that character. In this example, after this statement, instances of
 *     the left hand name surrounded by braces, &quot;<code>{alefmadda}</code>&quot;, will be
 *     replaced by the Unicode character U+0622. If the right hand side is longer than one
 *     character, then it is interpreted as a character category expression; see below for
 *     details.</dd>
 *   <dt>&nbsp;</dt>
 *   <dt><code>softvowel=[eiyEIY]</code></dt>
 *   <dd><strong>Category definition.</strong> The name on the left is assigned to stand for a
 *     set of characters. The same rules for names of simple variables apply. After this
 *     statement, the left hand variable will be interpreted as indicating a set of characters in
 *     appropriate contexts. The pattern syntax defining sets of characters is defined by {@link
 *     UnicodeSet}. Examples of valid patterns are:<table>
 *       <tr valign="top">
 *         <td nowrap><code>[abc]</code></td>
 *         <td>The set containing the characters 'a', 'b', and 'c'.</td>
 *       </tr>
 *       <tr valign="top">
 *         <td nowrap><code>[^abc]</code></td>
 *         <td>The set of all characters <em>except</em> 'a', 'b', and 'c'.</td>
 *       </tr>
 *       <tr valign="top">
 *         <td nowrap><code>[A-Z]</code></td>
 *         <td>The set of all characters from 'A' to 'Z' in Unicode order.</td>
 *       </tr>
 *       <tr valign="top">
 *         <td nowrap><code>[:Lu:]</code></td>
 *         <td>The set of Unicode uppercase letters. See <a href="http://www.unicode.org">www.unicode.org</a>
 *         for a complete list of categories and their two-letter codes.</td>
 *       </tr>
 *       <tr valign="top">
 *         <td nowrap><code>[^a-z[:Lu:][:Ll:]]</code></td>
 *         <td>The set of all characters <em>except</em> 'a' through 'z' and uppercase or lowercase
 *         letters.</td>
 *       </tr>
 *     </table>
 *     <p>See {@link UnicodeSet} for more documentation and examples. </p>
 *   </dd>
 *   <dt><code>ai&gt;{alefmadda}</code></dt>
 *   <dd><strong>Forward translation rule.</strong> This rule states that the string on the left
 *     will be changed to the string on the right when performing forward transliteration.</dd>
 *   <dt>&nbsp;</dt>
 *   <dt><code>ai&lt;{alefmadda}</code></dt>
 *   <dd><strong>Reverse translation rule.</strong> This rule states that the string on the right
 *     will be changed to the string on the left when performing reverse transliteration.</dd>
 * </dl>
 * 
 * <dl>
 *   <dt><code>ai&lt;&gt;{alefmadda}</code></dt>
 *   <dd><strong>Bidirectional translation rule.</strong> This rule states that the string on the
 *     right will be changed to the string on the left when performing forward transliteration,
 *     and vice versa when performing reverse transliteration.</dd>
 * </dl>
 * 
 * <p>Forward and reverse translation rules consist of a <em>match pattern</em> and an <em>output
 * string</em>. The match pattern consists of literal characters, optionally preceded by
 * context, and optionally followed by context. Context characters, like literal pattern
 * characters, must be matched in the text being transliterated. However, unlike literal
 * pattern characters, they are not replaced by the output text. For example, the pattern
 * &quot;<code>(abc)def</code>&quot; indicates the characters &quot;<code>def</code>&quot;
 * must be preceded by &quot;<code>abc</code>&quot; for a successful match. If there is a
 * successful match, &quot;<code>def</code>&quot; will be replaced, but not &quot;<code>abc</code>&quot;.
 * The initial '<code>(</code>' is optional, so &quot;<code>abc)def</code>&quot; is
 * equivalent to &quot;<code>(abc)def</code>&quot;. Another example is &quot;<code>123(456)</code>&quot;
 * (or &quot;<code>123(456</code>&quot;) in which the literal pattern &quot;<code>123</code>&quot;
 * must be followed by &quot;<code>456</code>&quot;. </p>
 * 
 * <p>The output string of a forward or reverse rule consists of characters to replace the
 * literal pattern characters. If the output string contains the character '<code>|</code>',
 * this is taken to indicate the location of the <em>cursor</em> after replacement. The
 * cursor is the point in the text at which the next replacement, if any, will be applied. </p>
 * 
 * <p>In addition to being defined in variables, <code>UnicodeSet</code> patterns may be
 * embedded directly into rule strings. Thus, the following two rules are equivalent:</p>
 * 
 * <blockquote>
 *   <p><code>vowel=[aeiou]; {vowel}&gt;*; # One way to do this<br>
 *   [aeiou]&gt;*;
 *   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; #
 *   Another way</code></p>
 * </blockquote>
 * 
 * <p><b>Example</b> </p>
 * 
 * <p>The following example rules illustrate many of the features of the rule language. </p>
 * 
 * <table cellpadding="4">
 *   <tr valign="top">
 *     <td>Rule 1.</td>
 *     <td nowrap><code>(abc)def&gt;x|y</code></td>
 *   </tr>
 *   <tr valign="top">
 *     <td>Rule 2.</td>
 *     <td nowrap><code>xyz&gt;r</code></td>
 *   </tr>
 *   <tr valign="top">
 *     <td>Rule 3.</td>
 *     <td nowrap><code>yz&gt;q</code></td>
 *   </tr>
 * </table>
 * 
 * <p>Applying these rules to the string &quot;<code>adefabcdefz</code>&quot; yields the
 * following results: </p>
 * 
 * <table cellpadding="4">
 *   <tr valign="top">
 *     <td nowrap><code>|adefabcdefz</code></td>
 *     <td>Initial state, no rules match. Advance cursor.</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>a|defabcdefz</code></td>
 *     <td>Still no match. Rule 1 does not match because the preceding context is not present.</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>ad|efabcdefz</code></td>
 *     <td>Still no match. Keep advancing until there is a match...</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>ade|fabcdefz</code></td>
 *     <td>...</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>adef|abcdefz</code></td>
 *     <td>...</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>adefa|bcdefz</code></td>
 *     <td>...</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>adefab|cdefz</code></td>
 *     <td>...</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>adefabc|defz</code></td>
 *     <td>Rule 1 matches; replace &quot;<code>def</code>&quot; with &quot;<code>xy</code>&quot;
 *     and back up the cursor to before the '<code>y</code>'.</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>adefabcx|yz</code></td>
 *     <td>Although &quot;<code>xyz</code>&quot; is present, rule 2 does not match because the
 *     cursor is before the '<code>y</code>', not before the '<code>x</code>'. Rule 3 does match.
 *     Replace &quot;<code>yz</code>&quot; with &quot;<code>q</code>&quot;.</td>
 *   </tr>
 *   <tr valign="top">
 *     <td nowrap><code>adefabcxq|</code></td>
 *     <td>The cursor is at the end; transliteration is complete.</td>
 *   </tr>
 * </table>
 * 
 * <p>The order of rules is significant. If multiple rules may match at some point, the first
 * matching rule is applied. </p>
 * 
 * <p>Forward and reverse rules may have an empty output string. Otherwise, an empty left or
 * right hand side of any statement is a syntax error. </p>
 * 
 * <p>Single quotes are used to quote the special characters <code>=&gt;&lt;{}[]()|</code>.
 * To specify a single quote itself, inside or outside of quotes, use two single quotes in a
 * row. For example, the rule &quot;<code>'&gt;'&gt;o''clock</code>&quot; changes the string
 * &quot;<code>&gt;</code>&quot; to the string &quot;<code>o'clock</code>&quot;. </p>
 * 
 * <p><b>Notes</b> </p>
 * 
 * <p>While a RuleBasedTransliterator is being built, it checks that the rules are added in
 * proper order. For example, if the rule &quot;a&gt;x&quot; is followed by the rule
 * &quot;ab&gt;y&quot;, then the second rule will throw an exception. The reason is that the
 * second rule can never be triggered, since the first rule always matches anything it
 * matches. In other words, the first rule <em>masks</em> the second rule. </p>
 *
 * @author Alan Liu
 */
class U_I18N_API RuleBasedTransliterator : public Transliterator {

    /**
     * The data object is immutable, so we can freely share it with
     * other instances of RBT, as long as we do NOT own this object.
     */
    TransliterationRuleData* data;

    /**
     * If true, we own the data object and must delete it.
     */
    bool_t isDataOwned;

public:

    /**
     * Constructs a new transliterator from the given rules.
     * @param rules rules, separated by ';'
     * @param direction either FORWARD or REVERSE.
     * @exception IllegalArgumentException if rules are malformed
     * or direction is invalid.
     */
    RuleBasedTransliterator(const UnicodeString& ID,
                            const UnicodeString& rules,
                            Direction direction,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status);

    /**
     * Covenience constructor with no filter.
     */
    RuleBasedTransliterator(const UnicodeString& ID,
                            const UnicodeString& rules,
                            Direction direction,
                            UErrorCode& status);

    /**
     * Covenience constructor with no filter and FORWARD direction.
     */
    RuleBasedTransliterator(const UnicodeString& ID,
                            const UnicodeString& rules,
                            UErrorCode& status);

    /**
     * Covenience constructor with FORWARD direction.
     */
    RuleBasedTransliterator(const UnicodeString& ID,
                            const UnicodeString& rules,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status);

    RuleBasedTransliterator(const UnicodeString& ID,
                            const TransliterationRuleData* theData,
                            UnicodeFilter* adoptedFilter = 0);

    RuleBasedTransliterator(const RuleBasedTransliterator&);

    virtual ~RuleBasedTransliterator();

    /**
     * Implement Transliterator API.
     */
    Transliterator* clone(void) const;

    /**
     * Transliterates a segment of a string.  <code>Transliterator</code> API.
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @return The new limit index
     */
    virtual int32_t transliterate(Replaceable& text,
                                  int32_t start, int32_t limit) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    virtual void handleTransliterate(Replaceable& text, Position& offsets,
                                     bool_t isIncremental) const;

private:

    void _construct(const UnicodeString& rules,
                    Direction direction,
                    UErrorCode& status);
};

/**
 * Constructs a new transliterator from the given rules.
 * @param rules rules, separated by ';'
 * @param direction either FORWARD or REVERSE.
 * @exception IllegalArgumentException if rules are malformed
 * or direction is invalid.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& ID,
                            const UnicodeString& rules,
                            Direction direction,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status) :
    Transliterator(ID, adoptedFilter) {
    _construct(rules, direction, status);
}

/**
 * Covenience constructor with no filter.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& ID,
                            const UnicodeString& rules,
                            Direction direction,
                            UErrorCode& status) :
    Transliterator(ID, 0) {
    _construct(rules, direction, status);
}

/**
 * Covenience constructor with no filter and FORWARD direction.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& ID,
                            const UnicodeString& rules,
                            UErrorCode& status) :
    Transliterator(ID, 0) {
    _construct(rules, FORWARD, status);
}

/**
 * Covenience constructor with FORWARD direction.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& ID,
                            const UnicodeString& rules,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status) :
    Transliterator(ID, adoptedFilter) {
    _construct(rules, FORWARD, status);
}

#endif
