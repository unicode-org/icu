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
#include "unicode/parseerr.h"

class TransliterationRuleData;

/**
 * <code>RuleBasedTransliterator</code> is a transliterator
 * that reads a set of rules in order to determine how to perform
 * translations. Rule sets are stored in resource bundles indexed by
 * name. Rules within a rule set are separated by semicolons (';').
 * To include a literal semicolon, prefix it with a backslash ('\').
 * Whitespace, as defined by <code>Character.isWhitespace()</code>,
 * is ignored. If the first non-blank character on a line is '#',
 * the entire line is ignored as a comment. </p>
 * 
 * <p>Each set of rules consists of two groups, one forward, and one
 * reverse. This is a convention that is not enforced; rules for one
 * direction may be omitted, with the result that translations in
 * that direction will not modify the source text. In addition,
 * bidirectional forward-reverse rules may be specified for
 * symmetrical transformations.</p>
 * 
 * <p><b>Rule syntax</b> </p>
 * 
 * <p>Rule statements take one of the following forms: </p>
 * 
 * <dl>
 *     <dt><code>$alefmadda=\u0622;</code></dt>
 *     <dd><strong>Variable definition.</strong> The name on the
 *         left is assigned the text on the right. In this example,
 *         after this statement, instances of the left hand name,
 *         &quot;<code>$alefmadda</code>&quot;, will be replaced by
 *         the Unicode character U+0622. Variable names must begin
 *         with a letter and consist only of letters, digits, and
 *         underscores. Case is significant. Duplicate names cause
 *         an exception to be thrown, that is, variables cannot be
 *         redefined. The right hand side may contain well-formed
 *         text of any length, including no text at all (&quot;<code>$empty=;</code>&quot;).
 *         The right hand side may contain embedded <code>UnicodeSet</code>
 *         patterns, for example, &quot;<code>$softvowel=[eiyEIY]</code>&quot;.</dd>
 *     <dd>&nbsp;</dd>
 *     <dt><code>ai&gt;$alefmadda;</code></dt>
 *     <dd><strong>Forward translation rule.</strong> This rule
 *         states that the string on the left will be changed to the
 *         string on the right when performing forward
 *         transliteration.</dd>
 *     <dt>&nbsp;</dt>
 *     <dt><code>ai&lt;$alefmadda;</code></dt>
 *     <dd><strong>Reverse translation rule.</strong> This rule
 *         states that the string on the right will be changed to
 *         the string on the left when performing reverse
 *         transliteration.</dd>
 * </dl>
 * 
 * <dl>
 *     <dt><code>ai&lt;&gt;$alefmadda;</code></dt>
 *     <dd><strong>Bidirectional translation rule.</strong> This
 *         rule states that the string on the right will be changed
 *         to the string on the left when performing forward
 *         transliteration, and vice versa when performing reverse
 *         transliteration.</dd>
 * </dl>
 * 
 * <p>Translation rules consist of a <em>match pattern</em> and an <em>output
 * string</em>. The match pattern consists of literal characters,
 * optionally preceded by context, and optionally followed by
 * context. Context characters, like literal pattern characters,
 * must be matched in the text being transliterated. However, unlike
 * literal pattern characters, they are not replaced by the output
 * text. For example, the pattern &quot;<code>abc{def}</code>&quot;
 * indicates the characters &quot;<code>def</code>&quot; must be
 * preceded by &quot;<code>abc</code>&quot; for a successful match.
 * If there is a successful match, &quot;<code>def</code>&quot; will
 * be replaced, but not &quot;<code>abc</code>&quot;. The final '<code>}</code>'
 * is optional, so &quot;<code>abc{def</code>&quot; is equivalent to
 * &quot;<code>abc{def}</code>&quot;. Another example is &quot;<code>{123}456</code>&quot;
 * (or &quot;<code>123}456</code>&quot;) in which the literal
 * pattern &quot;<code>123</code>&quot; must be followed by &quot;<code>456</code>&quot;.
 * </p>
 * 
 * <p>The output string of a forward or reverse rule consists of
 * characters to replace the literal pattern characters. If the
 * output string contains the character '<code>|</code>', this is
 * taken to indicate the location of the <em>cursor</em> after
 * replacement. The cursor is the point in the text at which the
 * next replacement, if any, will be applied. The cursor is usually
 * placed within the replacement text; however, it can actually be
 * placed into the precending or following context by using the
 * special character '<code>@</code>'. Examples:</p>
 * 
 * <blockquote>
 *     <p><code>a {foo} z &gt; | @ bar; # foo -&gt; bar, move cursor
 *     before a<br>
 *     {foo} xyz &gt; bar @@|; #&nbsp;foo -&gt; bar, cursor between
 *     y and z</code></p>
 * </blockquote>
 * 
 * <p><b>UnicodeSet</b></p>
 * 
 * <p><code>UnicodeSet</code> patterns may appear anywhere that
 * makes sense. They may appear in variable definitions.
 * Contrariwise, <code>UnicodeSet</code> patterns may themselves
 * contain variable references, such as &quot;<code>$a=[a-z];$not_a=[^$a]</code>&quot;,
 * or &quot;<code>$range=a-z;$ll=[$range]</code>&quot;.</p>
 * 
 * <p><code>UnicodeSet</code> patterns may also be embedded directly
 * into rule strings. Thus, the following two rules are equivalent:</p>
 * 
 * <blockquote>
 *     <p><code>$vowel=[aeiou]; $vowel&gt;'*'; # One way to do this<br>
 *     [aeiou]&gt;'*';
 *     &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;#
 *     Another way</code></p>
 * </blockquote>
 * 
 * <p>See {@link UnicodeSet} for more documentation and examples.</p>
 * 
 * <p><b>Segments</b></p>
 * 
 * <p>Segments of the input string can be matched and copied to the
 * output string. This makes certain sets of rules simpler and more
 * general, and makes reordering possible. For example:</p>
 * 
 * <blockquote>
 *     <p><code>([a-z]) &gt; $1 $1;
 *     &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;#
 *     double lowercase letters<br>
 *     ([:Lu:]) ([:Ll:]) &gt; $2 $1; # reverse order of Lu-Ll pairs</code></p>
 * </blockquote>
 * 
 * <p>The segment of the input string to be copied is delimited by
 * &quot;<code>(</code>&quot; and &quot;<code>)</code>&quot;. Up to
 * nine segments may be defined. Segments may not overlap. In the
 * output string, &quot;<code>$1</code>&quot; through &quot;<code>$9</code>&quot;
 * represent the input string segments, in left-to-right order of
 * definition.</p>
 * 
 * <p><b>Example</b> </p>
 * 
 * <p>The following example rules illustrate many of the features of
 * the rule language. </p>
 * 
 * <table border="0" cellpadding="4">
 *     <tr>
 *         <td valign="top">Rule 1.</td>
 *         <td valign="top" nowrap><code>abc{def}&gt;x|y</code></td>
 *     </tr>
 *     <tr>
 *         <td valign="top">Rule 2.</td>
 *         <td valign="top" nowrap><code>xyz&gt;r</code></td>
 *     </tr>
 *     <tr>
 *         <td valign="top">Rule 3.</td>
 *         <td valign="top" nowrap><code>yz&gt;q</code></td>
 *     </tr>
 * </table>
 * 
 * <p>Applying these rules to the string &quot;<code>adefabcdefz</code>&quot;
 * yields the following results: </p>
 * 
 * <table border="0" cellpadding="4">
 *     <tr>
 *         <td valign="top" nowrap><code>|adefabcdefz</code></td>
 *         <td valign="top">Initial state, no rules match. Advance
 *         cursor.</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>a|defabcdefz</code></td>
 *         <td valign="top">Still no match. Rule 1 does not match
 *         because the preceding context is not present.</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>ad|efabcdefz</code></td>
 *         <td valign="top">Still no match. Keep advancing until
 *         there is a match...</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>ade|fabcdefz</code></td>
 *         <td valign="top">...</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>adef|abcdefz</code></td>
 *         <td valign="top">...</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>adefa|bcdefz</code></td>
 *         <td valign="top">...</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>adefab|cdefz</code></td>
 *         <td valign="top">...</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>adefabc|defz</code></td>
 *         <td valign="top">Rule 1 matches; replace &quot;<code>def</code>&quot;
 *         with &quot;<code>xy</code>&quot; and back up the cursor
 *         to before the '<code>y</code>'.</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>adefabcx|yz</code></td>
 *         <td valign="top">Although &quot;<code>xyz</code>&quot; is
 *         present, rule 2 does not match because the cursor is
 *         before the '<code>y</code>', not before the '<code>x</code>'.
 *         Rule 3 does match. Replace &quot;<code>yz</code>&quot;
 *         with &quot;<code>q</code>&quot;.</td>
 *     </tr>
 *     <tr>
 *         <td valign="top" nowrap><code>adefabcxq|</code></td>
 *         <td valign="top">The cursor is at the end;
 *         transliteration is complete.</td>
 *     </tr>
 * </table>
 * 
 * <p>The order of rules is significant. If multiple rules may match
 * at some point, the first matching rule is applied. </p>
 * 
 * <p>Forward and reverse rules may have an empty output string.
 * Otherwise, an empty left or right hand side of any statement is a
 * syntax error. </p>
 * 
 * <p>Single quotes are used to quote any character other than a
 * digit or letter. To specify a single quote itself, inside or
 * outside of quotes, use two single quotes in a row. For example,
 * the rule &quot;<code>'&gt;'&gt;o''clock</code>&quot; changes the
 * string &quot;<code>&gt;</code>&quot; to the string &quot;<code>o'clock</code>&quot;.
 * </p>
 * 
 * <p><b>Notes</b> </p>
 * 
 * <p>While a RuleBasedTransliterator is being built, it checks that
 * the rules are added in proper order. For example, if the rule
 * &quot;a&gt;x&quot; is followed by the rule &quot;ab&gt;y&quot;,
 * then the second rule will throw an exception. The reason is that
 * the second rule can never be triggered, since the first rule
 * always matches anything it matches. In other words, the first
 * rule <em>masks</em> the second rule. </p>
 * 
 * @author Alan Liu
 * @draft
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
    UBool isDataOwned;

public:

    /**
     * Constructs a new transliterator from the given rules.
     * @param rules rules, separated by ';'
     * @param direction either FORWARD or REVERSE.
     * @exception IllegalArgumentException if rules are malformed
     * or direction is invalid.
     * @draft
     */
    RuleBasedTransliterator(const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UnicodeFilter* adoptedFilter,
                            UParseError& parseError,
                            UErrorCode& status);

    /**
     * Constructs a new transliterator from the given rules.
     * @param rules rules, separated by ';'
     * @param direction either FORWARD or REVERSE.
     * @exception IllegalArgumentException if rules are malformed
     * or direction is invalid.
     */
    RuleBasedTransliterator(const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status);

    /**
     * Covenience constructor with no filter.
     * @draft
     */
    RuleBasedTransliterator(const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UErrorCode& status);

    /**
     * Covenience constructor with no filter and FORWARD direction.
     * @draft
     */
    RuleBasedTransliterator(const UnicodeString& id,
                            const UnicodeString& rules,
                            UErrorCode& status);

    /**
     * Covenience constructor with FORWARD direction.
     * @draft
     */
    RuleBasedTransliterator(const UnicodeString& id,
                            const UnicodeString& rules,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status);

    /**
     * Covenience constructor.
     * @draft
     */
    RuleBasedTransliterator(const UnicodeString& id,
                            const TransliterationRuleData* theData,
                            UnicodeFilter* adoptedFilter = 0);

    /**
     * Copy constructor.
     * @draft
     */
    RuleBasedTransliterator(const RuleBasedTransliterator&);

    virtual ~RuleBasedTransliterator();

    /**
     * Implement Transliterator API.
     * @draft
     */
    Transliterator* clone(void) const;

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @draft
     */
    virtual void handleTransliterate(Replaceable& text, UTransPosition& offsets,
                                     UBool isIncremental) const;

    /**
     * Parse error codes generated by RuleBasedTransliterator.
     * See parseerr.h.
     */
    enum {
        PARSE_ERROR_BASE = 0x10000,
        BAD_VARIABLE_DEFINITION,
        MALFORMED_RULE,
        MALFORMED_SET,
        MALFORMED_SYMBOL_REFERENCE,
        MALFORMED_UNICODE_ESCAPE,
        MALFORMED_VARIABLE_DEFINITION,
        MALFORMED_VARIABLE_REFERENCE,
        MISMATCHED_SEGMENT_DELIMITERS,
        MISPLACED_CURSOR_OFFSET,
        MISSING_OPERATOR,
        MISSING_SEGMENT_CLOSE,
        MULTIPLE_ANTE_CONTEXTS,
        MULTIPLE_CURSORS,
        MULTIPLE_POST_CONTEXTS,
        TRAILING_BACKSLASH,
        UNDEFINED_SEGMENT_REFERENCE,
        UNDEFINED_VARIABLE,
        UNQUOTED_SPECIAL,
        UNTERMINATED_QUOTE
    };

private:

    void _construct(const UnicodeString& rules,
                    UTransDirection direction,
                    UErrorCode& status,
                    UParseError* parseError = 0);
};

/**
 * Constructs a new transliterator from the given rules.
 * @param rules rules, separated by ';'
 * @param direction either FORWARD or REVERSE.
 * @exception IllegalArgumentException if rules are malformed
 * or direction is invalid.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UnicodeFilter* adoptedFilter,
                            UParseError& parseError,
                            UErrorCode& status) :
    Transliterator(id, adoptedFilter) {
    _construct(rules, direction, status, &parseError);
}

/**
 * Constructs a new transliterator from the given rules.
 * @param rules rules, separated by ';'
 * @param direction either FORWARD or REVERSE.
 * @exception IllegalArgumentException if rules are malformed
 * or direction is invalid.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status) :
    Transliterator(id, adoptedFilter) {
    _construct(rules, direction, status);
}

/**
 * Covenience constructor with no filter.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UTransDirection direction,
                            UErrorCode& status) :
    Transliterator(id, 0) {
    _construct(rules, direction, status);
}

/**
 * Covenience constructor with no filter and FORWARD direction.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UErrorCode& status) :
    Transliterator(id, 0) {
    _construct(rules, UTRANS_FORWARD, status);
}

/**
 * Covenience constructor with FORWARD direction.
 */
inline RuleBasedTransliterator::RuleBasedTransliterator(
                            const UnicodeString& id,
                            const UnicodeString& rules,
                            UnicodeFilter* adoptedFilter,
                            UErrorCode& status) :
    Transliterator(id, adoptedFilter) {
    _construct(rules, UTRANS_FORWARD, status);
}

#endif
