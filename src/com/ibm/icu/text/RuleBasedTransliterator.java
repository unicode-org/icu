/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.util.Hashtable;


/**
 * <code>RuleBasedTransliterator</code> is a transliterator
 * that reads a set of rules in order to determine how to perform
 * translations. Rule sets are stored in resource bundles indexed by
 * name. Rules within a rule set are separated by semicolons (';').
 * To include a literal semicolon, prefix it with a backslash ('\').
 * Whitespace, as defined by <code>UCharacterProperty.isRuleWhiteSpace()</code>,
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
 * <p><b>Anchors</b></p>
 *
 * <p>Patterns can be anchored to the beginning or the end of the text. This is done with the
 * special characters '<code>^</code>' and '<code>$</code>'. For example:</p>
 *
 * <blockquote>
 *   <p><code>^ a&nbsp;&nbsp; &gt; 'BEG_A'; &nbsp;&nbsp;# match 'a' at start of text<br>
 *   &nbsp; a&nbsp;&nbsp; &gt; 'A';&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; # match other instances
 *   of 'a'<br>
 *   &nbsp; z $ &gt; 'END_Z'; &nbsp;&nbsp;# match 'z' at end of text<br>
 *   &nbsp; z&nbsp;&nbsp; &gt; 'Z';&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; # match other instances
 *   of 'z'</code></p>
 * </blockquote>
 *
 * <p>It is also possible to match the beginning or the end of the text using a <code>UnicodeSet</code>.
 * This is done by including a virtual anchor character '<code>$</code>' at the end of the
 * set pattern. Although this is usually the match chafacter for the end anchor, the set will
 * match either the beginning or the end of the text, depending on its placement. For
 * example:</p>
 *
 * <blockquote>
 *   <p><code>$x = [a-z$]; &nbsp;&nbsp;# match 'a' through 'z' OR anchor<br>
 *   $x 1&nbsp;&nbsp;&nbsp; &gt; 2;&nbsp;&nbsp; # match '1' after a-z or at the start<br>
 *   &nbsp;&nbsp; 3 $x &gt; 4; &nbsp;&nbsp;# match '3' before a-z or at the end</code></p>
 * </blockquote>
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
 * <p>Copyright (c) IBM Corporation 1999-2000. All rights reserved.</p>
 *
 * @author Alan Liu
 * @internal
 */
class RuleBasedTransliterator extends Transliterator {

    private Data data;

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Constructs a new transliterator from the given rules.
     * @param rules rules, separated by ';'
     * @param direction either FORWARD or REVERSE.
     * @exception IllegalArgumentException if rules are malformed
     * or direction is invalid.
     * @internal
     */
    public RuleBasedTransliterator(String ID, String rules, int direction,
                                   UnicodeFilter filter) {
        super(ID, filter);
        if (direction != FORWARD && direction != REVERSE) {
            throw new IllegalArgumentException("Invalid direction");
        }

        TransliteratorParser parser = new TransliteratorParser();
        parser.parse(rules, direction);
        if (parser.idBlock.length() != 0 ||
            parser.compoundFilter != null) {
            throw new IllegalArgumentException("::ID blocks illegal in RuleBasedTransliterator constructor");
        }

        data = parser.data;
        setMaximumContextLength(data.ruleSet.getMaximumContextLength());
    }

    /**
     * Constructs a new transliterator from the given rules in the
     * <code>FORWARD</code> direction.
     * @param rules rules, separated by ';'
     * @exception IllegalArgumentException if rules are malformed
     * or direction is invalid.
     * @internal
     */
    public RuleBasedTransliterator(String ID, String rules) {
        this(ID, rules, FORWARD, null);
    }

    RuleBasedTransliterator(String ID, Data data, UnicodeFilter filter) {
        super(ID, filter);
        this.data = data;
        setMaximumContextLength(data.ruleSet.getMaximumContextLength());
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     * @internal
     */
    protected synchronized void handleTransliterate(Replaceable text,
                                       Position index, boolean incremental) {
        /* We keep start and limit fixed the entire time,
         * relative to the text -- limit may move numerically if text is
         * inserted or removed.  The cursor moves from start to limit, with
         * replacements happening under it.
         *
         * Example: rules 1. ab>x|y
         *                2. yc>z
         *
         * |eabcd   start - no match, advance cursor
         * e|abcd   match rule 1 - change text & adjust cursor
         * ex|ycd   match rule 2 - change text & adjust cursor
         * exz|d    no match, advance cursor
         * exzd|    done
         */

        /* A rule like
         *   a>b|a
         * creates an infinite loop. To prevent that, we put an arbitrary
         * limit on the number of iterations that we take, one that is
         * high enough that any reasonable rules are ok, but low enough to
         * prevent a server from hanging.  The limit is 16 times the
         * number of characters n, unless n is so large that 16n exceeds a
         * uint32_t.
         */
        int loopCount = 0;
        int loopLimit = (index.limit - index.start) << 4;
        if (loopLimit < 0) {
            loopLimit = 0x7FFFFFFF;
        }

        while (index.start < index.limit &&
               loopCount <= loopLimit &&
               data.ruleSet.transliterate(text, index, incremental)) {
            ++loopCount;
        }
    }


    static class Data {
        public Data() {
            variableNames = new Hashtable();
            ruleSet = new TransliterationRuleSet();
        }

        /**
         * Rule table.  May be empty.
         */
        public TransliterationRuleSet ruleSet;

        /**
         * Map variable name (String) to variable (char[]).  A variable name
         * corresponds to zero or more characters, stored in a char[] array in
         * this hash.  One or more of these chars may also correspond to a
         * UnicodeSet, in which case the character in the char[] in this hash is
         * a stand-in: it is an index for a secondary lookup in
         * data.variables.  The stand-in also represents the UnicodeSet in
         * the stored rules.
         */
        Hashtable variableNames;

        /**
         * Map category variable (Character) to UnicodeMatcher or UnicodeReplacer.
         * Variables that correspond to a set of characters are mapped
         * from variable name to a stand-in character in data.variableNames.
         * The stand-in then serves as a key in this hash to lookup the
         * actual UnicodeSet object.  In addition, the stand-in is
         * stored in the rule text to represent the set of characters.
         * variables[i] represents character (variablesBase + i).
         */
        Object[] variables;

        /**
         * The character that represents variables[0].  Characters
         * variablesBase through variablesBase +
         * variables.length - 1 represent UnicodeSet objects.
         */
        char variablesBase;

        /**
         * Return the UnicodeMatcher represented by the given character, or
         * null if none.
         */
        public UnicodeMatcher lookupMatcher(int standIn) {
            int i = standIn - variablesBase;
            return (i >= 0 && i < variables.length)
                ? (UnicodeMatcher) variables[i] : null;
        }

        /**
         * Return the UnicodeReplacer represented by the given character, or
         * null if none.
         */
        public UnicodeReplacer lookupReplacer(int standIn) {
            int i = standIn - variablesBase;
            return (i >= 0 && i < variables.length)
                ? (UnicodeReplacer) variables[i] : null;
        }
    }


    /**
     * Return a representation of this transliterator as source rules.
     * These rules will produce an equivalent transliterator if used
     * to construct a new transliterator.
     * @param escapeUnprintable if TRUE then convert unprintable
     * character to their hex escape representations, \\uxxxx or
     * \\Uxxxxxxxx.  Unprintable characters are those other than
     * U+000A, U+0020..U+007E.
     * @return rules string
     * @internal
     */
    public String toRules(boolean escapeUnprintable) {
        return data.ruleSet.toRules(escapeUnprintable);
    }

    /**
     * Return the set of all characters that may be modified by this
     * Transliterator, ignoring the effect of our filter.
     * @internal
     */
    protected UnicodeSet handleGetSourceSet() {
        return data.ruleSet.getSourceTargetSet(false);
    }

    /**
     * Returns the set of all characters that may be generated as
     * replacement text by this transliterator.
     * @internal
     */
    public UnicodeSet getTargetSet() {
        return data.ruleSet.getSourceTargetSet(true);
    }
}

/**
 * Revision 1.61  2004/02/25 01:26:23  alan
 * jitterbug 3517: make concrete transilterators package private and @internal
 *
 * Revision 1.60  2003/06/03 18:49:35  alan
 * jitterbug 2959: update copyright dates to include 2003
 *
 * Revision 1.59  2003/05/14 19:03:30  rviswanadha
 * jitterbug 2836: fix compiler warnings
 *
 * Revision 1.58  2002/12/03 18:57:36  alan
 * jitterbug 2087: fix @ tags
 *
 * Revision 1.57  2002/07/26 21:12:36  alan
 * jitterbug 1997: use UCharacterProperty.isRuleWhiteSpace() in parsers
 *
 * Revision 1.56  2002/06/28 19:15:52  alan
 * jitterbug 1434: improve method names; minor cleanup
 *
 * Revision 1.55  2002/06/26 18:12:39  alan
 * jitterbug 1434: initial public implementation of getSourceSet and getTargetSet
 *
 * Revision 1.54  2002/02/25 22:43:58  ram
 * Move Utility class to icu.impl
 *
 * Revision 1.53  2002/02/16 03:06:13  Mohan
 * ICU4J reorganization
 *
 * Revision 1.52  2002/02/07 00:53:54  alan
 * jitterbug 1234: make output side of RBTs object-oriented; rewrite ID parsers and modularize them; implement &Any-Lower() support
 *
 * Revision 1.51  2001/11/29 22:31:18  alan
 * jitterbug 1560: add source-set methods and TransliteratorUtility class
 *
 * Revision 1.50  2001/11/27 22:07:33  alan
 * jitterbug 1389: incorporate Mark's review comments - comments only
 *
 * Revision 1.49  2001/10/10 20:26:27  alan
 * jitterbug 81: initial implementation of compound filters in IDs and ::ID blocks
 *
 * Revision 1.48  2001/10/05 18:15:54  alan
 * jitterbug 74: finish port of Source-Target/Variant code incl. TransliteratorRegistry and tests
 *
 * Revision 1.47  2001/10/03 00:14:22  alan
 * jitterbug 73: finish quantifier and supplemental char support
 *
 * Revision 1.46  2001/09/26 18:00:06  alan
 * jitterbug 67: sync parser with icu4c, allow unlimited, nested segments
 *
 * Revision 1.45  2001/09/24 19:57:17  alan
 * jitterbug 60: implement toPattern in UnicodeSet; update UnicodeFilter.contains to take an int; update UnicodeSet to support code points to U+10FFFF
 *
 * Revision 1.44  2001/09/21 21:24:04  alan
 * jitterbug 64: allow ::ID blocks in rules
 *
 * Revision 1.43  2001/09/19 17:43:37  alan
 * jitterbug 60: initial implementation of toRules()
 *
 * Revision 1.42  2001/02/20 17:59:40  alan4j
 * Remove backslash-u from log
 *
 * Revision 1.41  2001/02/16 18:53:55  alan4j
 * Handle backslash-u escapes
 *
 * Revision 1.40  2001/02/03 00:46:21  alan4j
 * Load RuleBasedTransliterator files from UTF8 files instead of ResourceBundles
 *
 * Revision 1.39  2000/08/31 17:11:42  alan4j
 * Implement anchors.
 *
 * Revision 1.38  2000/08/30 20:40:30  alan4j
 * Implement anchors.
 *
 * Revision 1.37  2000/07/12 16:31:36  alan4j
 * Simplify loop limit logic
 *
 * Revision 1.36  2000/06/29 21:59:23  alan4j
 * Fix handling of Transliterator.Position fields
 *
 * Revision 1.35  2000/06/28 20:49:54  alan4j
 * Fix handling of Positions fields
 *
 * Revision 1.34  2000/06/28 20:36:32  alan4j
 * Clean up Transliterator::Position - rename temporary names
 *
 * Revision 1.33  2000/06/28 20:31:43  alan4j
 * Clean up Transliterator::Position and rename fields (related to jitterbug 450)
 *
 * Revision 1.32  2000/05/24 22:21:00  alan4j
 * Compact UnicodeSets
 *
 * Revision 1.31  2000/05/23 16:48:27  alan4j
 * Fix doc; remove unused auto
 *
 * Revision 1.30  2000/05/18 22:49:51  alan
 * Update docs
 *
 * Revision 1.29  2000/04/28 00:25:42  alan
 * Improve error reporting
 *
 * Revision 1.28  2000/04/25 17:38:00  alan
 * Minor parser cleanup.
 *
 * Revision 1.27  2000/04/25 01:42:58  alan
 * Allow arbitrary length variable values. Clean up Data API. Update javadocs.
 *
 * Revision 1.26  2000/04/22 01:25:10  alan
 * Add support for cursor positioner '@'; update javadoc
 *
 * Revision 1.25  2000/04/22 00:08:43  alan
 * Narrow range to 21 - 7E for mandatory quoting.
 *
 * Revision 1.24  2000/04/22 00:03:54  alan
 * Disallow unquoted special chars. Report multiple errors at once.
 *
 * Revision 1.23  2000/04/21 22:23:40  alan
 * Clean up parseReference. Previous log should read 'delegate', not 'delete'.
 *
 * Revision 1.22  2000/04/21 22:16:29  alan
 * Delete variable name parsing to SymbolTable interface to consolidate parsing code.
 *
 * Revision 1.21  2000/04/21 21:16:40  alan
 * Modify rule syntax
 *
 * Revision 1.20  2000/04/19 17:35:23  alan
 * Update javadoc; fix compile error
 *
 * Revision 1.19  2000/04/19 16:34:18  alan
 * Add segment support.
 *
 * Revision 1.18  2000/04/12 20:17:45  alan
 * Delegate replace operation to rule object
 *
 * Revision 1.17  2000/03/10 04:07:23  johnf
 * Copyright update
 *
 * Revision 1.16  2000/02/24 20:46:49  liu
 * Add infinite loop check
 *
 * Revision 1.15  2000/02/10 07:36:25  johnf
 * fixed imports for com.ibm.icu.impl.Utility
 *
 * Revision 1.14  2000/02/03 18:18:42  Alan
 * Use array rather than hashtable for char-to-set map
 *
 * Revision 1.13  2000/01/27 18:59:19  Alan
 * Use Position rather than int[] and move all subclass overrides to one method (handleTransliterate)
 *
 * Revision 1.12  2000/01/18 17:51:09  Alan
 * Remove "keyboard" from method names. Make maximum context a field of Transliterator, and have subclasses set it.
 *
 * Revision 1.11  2000/01/18 02:30:49  Alan
 * Add Jamo-Hangul, Hangul-Jamo, fix rules, add compound ID support
 *
 * Revision 1.10  2000/01/13 23:53:23  Alan
 * Fix bugs found during ICU port
 *
 * Revision 1.9  2000/01/11 04:12:06  Alan
 * Cleanup, embellish comments
 *
 * Revision 1.8  2000/01/11 02:25:03  Alan
 * Rewrite UnicodeSet and RBT parsers for better performance and new syntax
 *
 * Revision 1.7  2000/01/06 01:36:36  Alan
 * Allow string arrays in rule resource bundles
 *
 * Revision 1.6  2000/01/04 21:43:57  Alan
 * Add rule indexing, and move masking check to TransliterationRuleSet.
 *
 * Revision 1.5  1999/12/22 01:40:54  Alan
 * Consolidate rule pattern anteContext, key, and postContext into one string.
 *
 * Revision 1.4  1999/12/22 01:05:54  Alan
 * Improve masking checking; turn it off by default, for better performance
 */
