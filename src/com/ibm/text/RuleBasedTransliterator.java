/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/text/Attic/RuleBasedTransliterator.java,v $ 
 * $Date: 2001/02/20 17:59:40 $ 
 * $Revision: 1.42 $
 *
 *****************************************************************************************
 */
package com.ibm.text;

import java.util.Hashtable;
import java.util.Vector;
import java.text.ParsePosition;
import com.ibm.util.Utility;
import com.ibm.text.resources.ResourceReader;

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
 * @version $RCSfile: RuleBasedTransliterator.java,v $ $Revision: 1.42 $ $Date: 2001/02/20 17:59:40 $
 */
public class RuleBasedTransliterator extends Transliterator {

    private Data data;

    private static final String COPYRIGHT =
        "\u00A9 IBM Corporation 1999. All rights reserved.";

    /**
     * Constructs a new transliterator from the given rules.
     * @param rules rules, separated by ';'
     * @param direction either FORWARD or REVERSE.
     * @exception IllegalArgumentException if rules are malformed
     * or direction is invalid.
     */
    public RuleBasedTransliterator(String ID, String rules, int direction,
                                   UnicodeFilter filter) {
        super(ID, filter);
        if (direction != FORWARD && direction != REVERSE) {
            throw new IllegalArgumentException("Invalid direction");
        }
        data = parse(rules, direction);
        setMaximumContextLength(data.ruleSet.getMaximumContextLength());
    }

    /**
     * Constructs a new transliterator from the given rules in the
     * <code>FORWARD</code> direction.
     * @param rules rules, separated by ';'
     * @exception IllegalArgumentException if rules are malformed
     * or direction is invalid.
     */
    public RuleBasedTransliterator(String ID, String rules) {
        this(ID, rules, FORWARD, null);
    }

    RuleBasedTransliterator(String ID, Data data, UnicodeFilter filter) {
        super(ID, filter);
        this.data = data;
        setMaximumContextLength(data.ruleSet.getMaximumContextLength());
    }

    static Data parse(String[] rules, int direction) {
        return new Parser(rules, direction).getData();
    }

    static Data parse(String rules, int direction) {
        return parse(new String[] { rules }, direction);
    }

    static Data parse(ResourceReader rules, int direction) {
        return new Parser(rules, direction).getData();
    }

    /**
     * Implements {@link Transliterator#handleTransliterate}.
     */
    protected void handleTransliterate(Replaceable text,
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

        boolean partial[] = new boolean[1];
        partial[0] = false;

        while (index.start < index.limit && loopCount <= loopLimit) {
            TransliterationRule r = incremental ?
                data.ruleSet.findIncrementalMatch(text, index,
                                                  data, partial, getFilter()) :
                data.ruleSet.findMatch(text, index,
                                       data, getFilter());
            /* If we match a rule then apply it by replacing the key
             * with the rule output and repositioning the cursor
             * appropriately.  If we get a partial match, then we
             * can't do anything without more text; return with the
             * cursor at the current position.  If we get null, then
             * there is no match at this position, and we can advance
             * the cursor.
             */
            if (r == null) {
                if (partial[0]) {
                    break;
                } else {
                    ++index.start;
                }
            } else {
                // Delegate replacement to TransliterationRule object
                int lenDelta = r.replace(text, index.start, data);
                index.limit += lenDelta;
                index.contextLimit += lenDelta;
                index.start += r.getCursorPos();
                ++loopCount;
            }
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
         * data.setVariables.  The stand-in also represents the UnicodeSet in
         * the stored rules.
         */
        private Hashtable variableNames;

        /**
         * Map category variable (Character) to set (UnicodeSet).
         * Variables that correspond to a set of characters are mapped
         * from variable name to a stand-in character in data.variableNames.
         * The stand-in then serves as a key in this hash to lookup the
         * actual UnicodeSet object.  In addition, the stand-in is
         * stored in the rule text to represent the set of characters.
         * setVariables[i] represents character (setVariablesBase + i).
         */
        private UnicodeSet[] setVariables;

        /**
         * The character that represents setVariables[0].  Characters
         * setVariablesBase through setVariablesBase +
         * setVariables.length - 1 represent UnicodeSet objects.
         */
        private char setVariablesBase;

        /**
         * The character that represents segment 1.  Characters segmentBase
         * through segmentBase + 8 represent segments 1 through 9.
         */
        private char segmentBase;

        /**
         * Return the UnicodeSet represented by the given character, or
         * null if none.
         */
        public UnicodeSet lookupSet(char c) {
            int i = c - setVariablesBase;
            return (i >= 0 && i < setVariables.length)
                ? setVariables[i] : null;
        }

        /**
         * Return the zero-based index of the segment represented by the given
         * character, or -1 if none.  Repeat: This is a zero-based return value,
         * 0..8, even though these are notated "$1".."$9".
         */
        public int lookupSegmentReference(char c) {
            int i = c - segmentBase;
            return (i >= 0 && i < 9) ? i : -1;
        }
    }



    private static class Parser {
        /**
         * Current rule being parsed.
         */
        private String rules;

        private int direction;

        private Data data;

        /**
         * This class implements the SymbolTable interface.  It is used
         * during parsing to give UnicodeSet access to variables that
         * have been defined so far.  Note that it uses setVariablesVector,
         * _not_ data.setVariables.
         */
        private class ParseData implements SymbolTable {
            
            /**
             * Implement SymbolTable API.
             */
            public char[] lookup(String name) {
                return (char[]) data.variableNames.get(name);
            }

            /**
             * Implement SymbolTable API.
             */
            public UnicodeSet lookupSet(char ch) {
                // Note that we cannot use data.lookupSet() because the
                // set array has not been constructed yet.
                int i = ch - data.setVariablesBase;
                if (i >= 0 && i < setVariablesVector.size()) {
                    return (UnicodeSet) setVariablesVector.elementAt(i);
                }
                return null;
            }

            /**
             * Implement SymbolTable API.  Parse out a symbol reference
             * name.
             */
            public String parseReference(String text, ParsePosition pos, int limit) {
                int start = pos.getIndex();
                int i = start;
                while (i < limit) {
                    char c = text.charAt(i);
                    if ((i==start && !Character.isUnicodeIdentifierStart(c)) ||
                        !Character.isUnicodeIdentifierPart(c)) {
                        break;
                    }
                    ++i;
                }
                if (i == start) { // No valid name chars
                    return null;
                }
                pos.setIndex(i);
                return text.substring(start, i);
            }
        }

        /**
         * Temporary symbol table used during parsing.
         */
        private ParseData parseData;

        /**
         * Temporary vector of set variables.  When parsing is complete, this
         * is copied into the array data.setVariables.  As with data.setVariables,
         * element 0 corresponds to character data.setVariablesBase.
         */
        private Vector setVariablesVector;

        /**
         * The next available stand-in for variables.  This starts at some point in
         * the private use area (discovered dynamically) and increments up toward
         * <code>variableLimit</code>.  At any point during parsing, available
         * variables are <code>variableNext..variableLimit-1</code>.
         */
        private char variableNext;

        /**
         * The last available stand-in for variables.  This is discovered
         * dynamically.  At any point during parsing, available variables are
         * <code>variableNext..variableLimit-1</code>.  During variable definition
         * we use the special value variableLimit-1 as a placeholder.
         */
        private char variableLimit;

        /**
         * When we encounter an undefined variable, we do not immediately signal
         * an error, in case we are defining this variable, e.g., "$a = [a-z];".
         * Instead, we save the name of the undefined variable, and substitute
         * in the placeholder char variableLimit - 1, and decrement
         * variableLimit.
         */
        private String undefinedVariableName;

        // Operators
        private static final char VARIABLE_DEF_OP   = '=';
        private static final char FORWARD_RULE_OP   = '>';
        private static final char REVERSE_RULE_OP   = '<';
        private static final char FWDREV_RULE_OP    = '~'; // internal rep of <> op

        private static final String OPERATORS = "=><";

        // Other special characters
        private static final char QUOTE               = '\'';
        private static final char ESCAPE              = '\\';
        private static final char END_OF_RULE         = ';';
        private static final char RULE_COMMENT_CHAR   = '#';

        private static final char CONTEXT_ANTE        = '{'; // ante{key
        private static final char CONTEXT_POST        = '}'; // key}post
        private static final char SET_OPEN            = '[';
        private static final char SET_CLOSE           = ']';
        private static final char CURSOR_POS          = '|';
        private static final char CURSOR_OFFSET       = '@';
        private static final char ANCHOR_START        = '^';

        // By definition, the ANCHOR_END special character is a
        // trailing SymbolTable.SYMBOL_REF character.
        // private static final char ANCHOR_END       = '$';

        // Segments of the input string are delimited by "(" and ")".  In the
        // output string these segments are referenced as "$1" through "$9".
        private static final char SEGMENT_OPEN        = '(';
        private static final char SEGMENT_CLOSE       = ')';

        /**
         * A private abstract class representing the interface to rule
         * source code that is broken up into lines.  Handles the
         * folding of lines terminated by a backslash.  This folding
         * is limited; it does not account for comments, quotes, or
         * escapes, so its use to be limited.
         */
        private abstract class RuleBody {

            /**
             * Retrieve the next line of the source, or return null if
             * none.  Folds lines terminated by a backslash into the
             * next line, without regard for comments, quotes, or
             * escapes.
             */
            String nextLine() {
                String s = handleNextLine();
                if (s != null &&
                    s.length() > 0 &&
                    s.charAt(s.length() - 1) == '\\') {

                    StringBuffer b = new StringBuffer(s);
                    do {
                        b.deleteCharAt(b.length()-1);
                        s = handleNextLine();
                        if (s == null) {
                            break;
                        }
                        b.append(s);
                    } while (s.length() > 0 &&
                             s.charAt(s.length() - 1) == '\\');

                    s = b.toString();
                }
                return s;
            }

            /**
             * Reset to the first line of the source.
             */
            abstract void reset();

            /**
             * Subclass method to return the next line of the source.
             */
            abstract String handleNextLine();
        };

        /**
         * RuleBody subclass for a String[] array.
         */
        private class RuleArray extends RuleBody {
            String[] array;
            int i;
            public RuleArray(String[] array) { this.array = array; i = 0; }
            public String handleNextLine() {
                return (i < array.length) ? array[i++] : null;
            }
            public void reset() {
                i = 0;
            }
        };

        /**
         * RuleBody subclass for a ResourceReader.
         */
        private class RuleReader extends RuleBody {
            ResourceReader reader;
            public RuleReader(ResourceReader reader) { this.reader = reader; }
            public String handleNextLine() {
                try {
                    return reader.readLine();
                } catch (java.io.IOException e) {}
                return null;
            }
            public void reset() {
                reader.reset();
            }
        };

        /**
         * @param rules list of rules, separated by semicolon characters
         * @exception IllegalArgumentException if there is a syntax error in the
         * rules
         */
        public Parser(String[] ruleArray, int direction) {
            this.direction = direction;
            data = new Data();
            parseRules(new RuleArray(ruleArray));
        }

        /**
         * @param rules resource reader for the rules
         */
        public Parser(ResourceReader rules, int direction) {
            this.direction = direction;
            data = new Data();
            parseRules(new RuleReader(rules));
        }

        public Data getData() {
            return data;
        }

        /**
         * Parse an array of zero or more rules.  The strings in the array are
         * treated as if they were concatenated together, with rule terminators
         * inserted between array elements if not present already.
         *
         * Any previous rules are discarded.  Typically this method is called exactly
         * once, during construction.
         * @exception IllegalArgumentException if there is a syntax error in the
         * rules
         */
        private void parseRules(RuleBody ruleArray) {
            determineVariableRange(ruleArray);
            setVariablesVector = new Vector();
            parseData = new ParseData();

            StringBuffer errors = null;
            int errorCount = 0;

            ruleArray.reset();
        main:
            for (;;) {
                String rule = ruleArray.nextLine();
                if (rule == null) {
                    break;
                }
                int pos = 0;
                int limit = rule.length();
                while (pos < limit) {
                    char c = rule.charAt(pos++);
                    if (Character.isWhitespace(c)) {
                        // Ignore leading whitespace.  Note that this is not
                        // Unicode spaces, but Java spaces -- a subset,
                        // representing whitespace likely to be seen in code.
                        continue;
                    }
                    // Skip lines starting with the comment character
                    if (c == RULE_COMMENT_CHAR) {
                        pos = rule.indexOf("\n", pos) + 1;
                        if (pos == 0) {
                            break; // No "\n" found; rest of rule is a commnet
                        }
                        continue; // Either fall out or restart with next line
                    }
                    // Often a rule file contains multiple errors.  It's
                    // convenient to the rule author if these are all reported
                    // at once.  We keep parsing rules even after a failure, up
                    // to a specified limit, and report all errors at once.
                    try {
                        // We've found the start of a rule.  c is its first
                        // character, and pos points past c.  Lexically parse the
                        // rule into component pieces.
                        pos = parseRule(rule, --pos, limit);                    
                    } catch (IllegalArgumentException e) {
                        if (errorCount == 30) {
                            errors.append("\nMore than 30 errors; further messages squelched");
                            break main;
                        }
                        if (errors == null) {
                            errors = new StringBuffer(e.getMessage());
                        } else {
                            errors.append("\n" + e.getMessage());
                        }
                        ++errorCount;
                        pos = ruleEnd(rule, pos, limit) + 1; // +1 advances past ';'
                    }
                }
            }

            // Convert the set vector to an array
            data.setVariables = new UnicodeSet[setVariablesVector.size()];
            setVariablesVector.copyInto(data.setVariables);
            setVariablesVector = null;
            
            // Index the rules
            try {
                data.ruleSet.freeze(data);
            } catch (IllegalArgumentException e) {
                if (errors == null) {
                    errors = new StringBuffer(e.getMessage());
                } else {
                    errors.append("\n").append(e.getMessage());
                }
            }

            if (errors != null) {
                throw new IllegalArgumentException(errors.toString());
            }
        }

        /**
         * A class representing one side of a rule.  This class knows how to
         * parse half of a rule.  It is tightly coupled to the method
         * RuleBasedTransliterator.Parser.parseRule().
         */
        static class RuleHalf {

            public String text;

            public int cursor = -1; // position of cursor in text
            public int ante = -1;   // position of ante context marker '{' in text
            public int post = -1;   // position of post context marker '}' in text

            // Record the position of the segment substrings and references.  A
            // given side should have segments or segment references, but not
            // both.
            public Vector segments = null; // ref substring start,limits
            public int maxRef = -1; // index of largest ref (1..9)

            // Record the offset to the cursor either to the left or to the
            // right of the key.  This is indicated by characters on the output
            // side that allow the cursor to be positioned arbitrarily within
            // the matching text.  For example, abc{def} > | @@@ xyz; changes
            // def to xyz and moves the cursor to before abc.  Offset characters
            // must be at the start or end, and they cannot move the cursor past
            // the ante- or postcontext text.  Placeholders are only valid in
            // output text.
            public int cursorOffset = 0; // only nonzero on output side

            public boolean anchorStart = false;
            public boolean anchorEnd   = false;

            /**
             * Parse one side of a rule, stopping at either the limit,
             * the END_OF_RULE character, or an operator.  Return
             * the pos of the terminating character (or limit).
             */
            public int parse(String rule, int pos, int limit,
                             RuleBasedTransliterator.Parser parser) {
                int start = pos;
                StringBuffer buf = new StringBuffer();
                ParsePosition pp = null;
                int cursorOffsetPos = 0; // Position of first CURSOR_OFFSET on _right_

            main:
                while (pos < limit) {
                    char c = rule.charAt(pos++);
                    if (Character.isWhitespace(c)) {
                        // Ignore whitespace.  Note that this is not Unicode
                        // spaces, but Java spaces -- a subset, representing
                        // whitespace likely to be seen in code.
                        continue;
                    }
                    if (OPERATORS.indexOf(c) >= 0) {
                        --pos; // Backup to point to operator
                        break main;
                    }
                    if (anchorEnd) {
                        // Text after a presumed end anchor is a syntax err
                        syntaxError("Syntax error: $", rule, start);
                    }
                    // Handle escapes
                    if (c == ESCAPE) {
                        if (pos == limit) {
                            syntaxError("Trailing backslash", rule, start);
                        }
                        c = rule.charAt(pos++);
                        if (c == 'u') {
                            if ((pos+4) > limit) {
                                syntaxError("Invalid \\u escape", rule, start);
                            }
                            c = '\u0000';
                            for (int j=pos+4; pos<j;) {
                                int digit = Character.digit(rule.charAt(pos++), 16);
                                if (digit<0) {
                                    syntaxError("Invalid \\u escape", rule, start);
                                }
                                c = (char) ((c << 4) | digit);
                            }                            
                        }
                        buf.append(c);
                        continue;
                    }
                    // Handle quoted matter
                    if (c == QUOTE) {
                        int iq = rule.indexOf(QUOTE, pos);
                        if (iq == pos) {
                            buf.append(c); // Parse [''] outside quotes as [']
                            ++pos;
                        } else {
                            /* This loop picks up a segment of quoted text of the
                             * form 'aaaa' each time through.  If this segment
                             * hasn't really ended ('aaaa''bbbb') then it keeps
                             * looping, each time adding on a new segment.  When it
                             * reaches the final quote it breaks.
                             */
                            for (;;) {
                                if (iq < 0) {
                                    syntaxError("Unterminated quote", rule, start);
                                }
                                buf.append(rule.substring(pos, iq));
                                pos = iq+1;
                                if (pos < limit && rule.charAt(pos) == QUOTE) {
                                // Parse [''] inside quotes as [']
                                    iq = rule.indexOf(QUOTE, pos+1);
                                // Continue looping
                                } else {
                                    break;
                                }
                            }
                        }
                        continue;
                    }
                    switch (c) {
                    case ANCHOR_START:
                        if (buf.length() == 0 && !anchorStart) {
                            anchorStart = true;
                        } else {
                            syntaxError("Misplaced anchor start",
                                        rule, start);
                        }
                        break;
                    case SEGMENT_OPEN:
                    case SEGMENT_CLOSE:
                        // Handle segment definitions "(" and ")"
                        // Parse "(", ")"
                        if (segments == null) {
                            segments = new Vector();
                        }
                        if ((c == SEGMENT_OPEN) !=
                            (segments.size() % 2 == 0)) {
                            syntaxError("Mismatched segment delimiters",
                                        rule, start);
                        }
                        segments.addElement(new Integer(buf.length()));
                        break;
                    case END_OF_RULE:
                        --pos; // Backup to point to END_OF_RULE
                        break main;
                    case SymbolTable.SYMBOL_REF:
                        // Handle variable references and segment references "$1" .. "$9"
                        {
                            // A variable reference must be followed immediately
                            // by a Unicode identifier start and zero or more
                            // Unicode identifier part characters, or by a digit
                            // 1..9 if it is a segment reference.
                            if (pos == limit) {
                                // A variable ref character at the end acts as
                                // an anchor to the context limit, as in perl.
                                anchorEnd = true;
                                break;
                            }
                            // Parse "$1" "$2" .. "$9"
                            c = rule.charAt(pos);
                            int r = Character.digit(c, 10);
                            if (r >= 1 && r <= 9) {
                                if (r > maxRef) {
                                    maxRef = r;
                                }
                                buf.append((char) (parser.data.segmentBase + r - 1));
                                ++pos;
                            } else {
                                if (pp == null) { // Lazy create
                                    pp = new ParsePosition(0);
                                }
                                pp.setIndex(pos);
                                String name = parser.parseData.
                                                parseReference(rule, pp, limit);
                                if (name == null) {
                                    // This means the '$' was not followed by a
                                    // valid name.  Try to interpret it as an
                                    // end anchor then.  If this also doesn't work
                                    // (if we see a following character) then signal
                                    // an error.
                                    anchorEnd = true;
                                    break;
                                }
                                pos = pp.getIndex();
                                // If this is a variable definition statement,
                                // then the LHS variable will be undefined.  In
                                // that case appendVariableDef() will append the
                                // special placeholder char variableLimit-1.
                                parser.appendVariableDef(name, buf);
                            }
                        }
                        break;
                    case CONTEXT_ANTE:
                        if (ante >= 0) {
                            syntaxError("Multiple ante contexts", rule, start);
                        }
                        ante = buf.length();
                        break;
                    case CONTEXT_POST:
                        if (post >= 0) {
                            syntaxError("Multiple post contexts", rule, start);
                        }
                        post = buf.length();
                        break;
                    case SET_OPEN:
                        if (pp == null) {
                            pp = new ParsePosition(0);
                        }
                        pp.setIndex(pos-1); // Backup to opening '['
                        buf.append(parser.parseSet(rule, pp));
                        pos = pp.getIndex();
                        break;
                    case CURSOR_POS:
                        if (cursor >= 0) {
                            syntaxError("Multiple cursors", rule, start);
                        }
                        cursor = buf.length();
                        break;
                    case CURSOR_OFFSET:
                        if (cursorOffset < 0) {
                            if (buf.length() > 0) {
                                syntaxError("Misplaced " + c, rule, start);
                            }
                            --cursorOffset;
                        } else if (cursorOffset > 0) {
                            if (buf.length() != cursorOffsetPos || cursor >= 0) {
                                syntaxError("Misplaced " + c, rule, start);
                            }
                            ++cursorOffset;
                        } else {
                            if (cursor == 0 && buf.length() == 0) {
                                cursorOffset = -1;
                            } else if (cursor < 0) {
                                cursorOffsetPos = buf.length();
                                cursorOffset = 1;
                            } else {
                                syntaxError("Misplaced " + c, rule, start);
                            }
                        }
                        break;
                    // case SET_CLOSE:
                    default:
                        // Disallow unquoted characters other than [0-9A-Za-z]
                        // in the printable ASCII range.  These characters are
                        // reserved for possible future use.
                        if (c >= 0x0021 && c <= 0x007E &&
                            !((c >= '0' && c <= '9') ||
                              (c >= 'A' && c <= 'Z') ||
                              (c >= 'a' && c <= 'z'))) {
                            syntaxError("Unquoted " + c, rule, start);
                        }
                        buf.append(c);
                        break;
                    }
                }

                if (cursorOffset > 0 && cursor != cursorOffsetPos) {
                    syntaxError("Misplaced " + CURSOR_POS, rule, start);
                }
                text = buf.toString();
                return pos;
            }

            /**
             * Remove context.
             */
            void removeContext() {
                text = text.substring(ante < 0 ? 0 : ante,
                                      post < 0 ? text.length() : post);
                ante = post = -1;
                anchorStart = anchorEnd = false;
            }

            /**
             * Create and return an int[] array of segments.
             */
            int[] getSegments() {
                if (segments == null) {
                    return null;
                }
                int[] result = new int[segments.size()];
                for (int i=0; i<segments.size(); ++i) {
                    result[i] = ((Number)segments.elementAt(i)).intValue();
                }
                return result;
            }
        }

        /**
         * MAIN PARSER.  Parse the next rule in the given rule string, starting
         * at pos.  Return the index after the last character parsed.  Do not
         * parse characters at or after limit.
         *
         * Important:  The character at pos must be a non-whitespace character
         * that is not the comment character.
         *
         * This method handles quoting, escaping, and whitespace removal.  It
         * parses the end-of-rule character.  It recognizes context and cursor
         * indicators.  Once it does a lexical breakdown of the rule at pos, it
         * creates a rule object and adds it to our rule list.
         *
         * This method is tightly coupled to the inner class RuleHalf.
         */
        private int parseRule(String rule, int pos, int limit) {
            // Locate the left side, operator, and right side
            int start = pos;
            char operator = 0;

            RuleHalf left  = new RuleHalf();
            RuleHalf right = new RuleHalf();

            undefinedVariableName = null;
            pos = left.parse(rule, pos, limit, this);

            if (pos == limit ||
                OPERATORS.indexOf(operator = rule.charAt(pos++)) < 0) {
                syntaxError("No operator", rule, start);
            }

            // Found an operator char.  Check for forward-reverse operator.
            if (operator == REVERSE_RULE_OP &&
                (pos < limit && rule.charAt(pos) == FORWARD_RULE_OP)) {
                ++pos;
                operator = FWDREV_RULE_OP;
            }

            pos = right.parse(rule, pos, limit, this);

            if (pos < limit) {
                if (rule.charAt(pos) == END_OF_RULE) {
                    ++pos;
                } else {
                    // RuleHalf parser must have terminated at an operator
                    syntaxError("Unquoted operator", rule, start);
                }
            }

            if (operator == VARIABLE_DEF_OP) {
                // LHS is the name.  RHS is a single character, either a literal
                // or a set (already parsed).  If RHS is longer than one
                // character, it is either a multi-character string, or multiple
                // sets, or a mixture of chars and sets -- syntax error.

                // We expect to see a single undefined variable (the one being
                // defined).
                if (undefinedVariableName == null) {
                    syntaxError("Missing '$' or duplicate definition", rule, start);
                }
                if (left.text.length() != 1 || left.text.charAt(0) != variableLimit) {
                    syntaxError("Malformed LHS", rule, start);
                }
                if (left.anchorStart || left.anchorEnd ||
                    right.anchorStart || right.anchorEnd) {
                    syntaxError("Malformed variable def", rule, start);
                }
                // We allow anything on the right, including an empty string.
                int n = right.text.length();
                char[] value = new char[n];
                right.text.getChars(0, n, value, 0);
                data.variableNames.put(undefinedVariableName, value);

                ++variableLimit;
                return pos;
            }

            // If this is not a variable definition rule, we shouldn't have
            // any undefined variable names.
            if (undefinedVariableName != null) {
                syntaxError("Undefined variable $" + undefinedVariableName,
                            rule, start);
            }

            // If the direction we want doesn't match the rule
            // direction, do nothing.
            if (operator != FWDREV_RULE_OP &&
                ((direction == FORWARD) != (operator == FORWARD_RULE_OP))) {
                return pos;
            }

            // Transform the rule into a forward rule by swapping the
            // sides if necessary.
            if (direction == REVERSE) {
                RuleHalf temp = left;
                left = right;
                right = temp;
            }

            // Remove non-applicable elements in forward-reverse
            // rules.  Bidirectional rules ignore elements that do not
            // apply.
            if (operator == FWDREV_RULE_OP) {
                right.removeContext();
                right.segments = null;
                left.cursor = left.maxRef = -1;
                left.cursorOffset = 0;
            }

            // Normalize context
            if (left.ante < 0) {
                left.ante = 0;
            }
            if (left.post < 0) {
                left.post = left.text.length();
            }

            // Context is only allowed on the input side.  Cursors are only
            // allowed on the output side.  Segment delimiters can only appear
            // on the left, and references on the right.  Cursor offset
            // cannot appear without an explicit cursor.  Cursor offset
            // cannot place the cursor outside the limits of the context.
            // Anchors are only allowed on the input side.
            if (right.ante >= 0 || right.post >= 0 || left.cursor >= 0 ||
                right.segments != null || left.maxRef >= 0 ||
                (right.cursorOffset != 0 && right.cursor < 0) ||
                (right.cursorOffset > (left.text.length() - left.post)) ||
                (-right.cursorOffset > left.ante) ||
                right.anchorStart || right.anchorEnd) {
                syntaxError("Malformed rule", rule, start);
            }

            // Check integrity of segments and segment references.  Each
            // segment's start must have a corresponding limit, and the
            // references must not refer to segments that do not exist.
            if (left.segments != null) {
                int n = left.segments.size();
                if (n % 2 != 0) {
                    syntaxError("Odd length segments", rule, start);
                }
                n /= 2;
                if (right.maxRef > n) {
                    syntaxError("Undefined segment reference " + right.maxRef, rule, start);
                }
            }

            data.ruleSet.addRule(new TransliterationRule(
                                         left.text, left.ante, left.post,
                                         right.text, right.cursor, right.cursorOffset,
                                         left.getSegments(),
                                         left.anchorStart, left.anchorEnd));
            
            return pos;
        }

        /**
         * Throw an exception indicating a syntax error.  Search the rule string
         * for the probable end of the rule.  Of course, if the error is that
         * the end of rule marker is missing, then the rule end will not be found.
         * In any case the rule start will be correctly reported.
         * @param msg error description
         * @param rule pattern string
         * @param start position of first character of current rule
         */
        static final void syntaxError(String msg, String rule, int start) {
            int end = ruleEnd(rule, start, rule.length());
            throw new IllegalArgumentException(msg + " in \"" +
                                               Utility.escape(rule.substring(start, end)) + '"');
        }

        static final int ruleEnd(String rule, int start, int limit) {
            int end = quotedIndexOf(rule, start, limit, ";");
            if (end < 0) {
                end = limit;
            }
            return end;
        }

        /**
         * Parse a UnicodeSet out, store it, and return the stand-in character
         * used to represent it.
         */
        private final char parseSet(String rule, ParsePosition pos) {
            UnicodeSet set = new UnicodeSet(rule, pos, parseData);
            if (variableNext >= variableLimit) {
                throw new RuntimeException("Private use variables exhausted");
            }
            set.compact();
            setVariablesVector.addElement(set);
            return variableNext++;
        }

        /**
         * Append the value of the given variable name to the given
         * StringBuffer.
         * @exception IllegalArgumentException if the name is unknown.
         */
        private void appendVariableDef(String name, StringBuffer buf) {
            char[] ch = (char[]) data.variableNames.get(name);
            if (ch == null) {
                // We allow one undefined variable so that variable definition
                // statements work.  For the first undefined variable we return
                // the special placeholder variableLimit-1, and save the variable
                // name.
                if (undefinedVariableName == null) {
                    undefinedVariableName = name;
                    if (variableNext >= variableLimit) {
                        throw new RuntimeException("Private use variables exhausted");
                    }
                    buf.append((char) --variableLimit);
                } else {
                    throw new IllegalArgumentException("Undefined variable $"
                                                       + name);
                }
            } else {
                buf.append(ch);
            }
        }

        /**
         * Determines what part of the private use region of Unicode we can use for
         * variable stand-ins.  The correct way to do this is as follows: Parse each
         * rule, and for forward and reverse rules, take the FROM expression, and
         * make a hash of all characters used.  The TO expression should be ignored.
         * When done, everything not in the hash is available for use.  In practice,
         * this method may employ some other algorithm for improved speed.
         */
        private final void determineVariableRange(RuleBody ruleArray) {
            // As an initial implementation, we just run through all the
            // characters, ignoring any quoting.  This works since the quote
            // mechanisms are outside the private use area.

            Range r = new Range('\uE000', 0x1900); // Private use area
            r = r.largestUnusedSubrange(ruleArray);
            
            if (r == null) {
                throw new RuntimeException(
                    "No private use characters available for variables");
            }

            // Allocate 9 characters for segment references 1 through 9
            data.segmentBase = r.start;
            data.setVariablesBase = variableNext = (char) (data.segmentBase + 9);
            variableLimit = (char) (r.start + r.length);

            if (variableNext >= variableLimit) {
                throw new RuntimeException(
                        "Too few private use characters available for variables");
            }
        }

        /**
         * Returns the index of the first character in a set, ignoring quoted text.
         * For example, in the string "abc'hide'h", the 'h' in "hide" will not be
         * found by a search for "h".  Unlike String.indexOf(), this method searches
         * not for a single character, but for any character of the string
         * <code>setOfChars</code>.
         * @param text text to be searched
         * @param start the beginning index, inclusive; <code>0 <= start
         * <= limit</code>.
         * @param limit the ending index, exclusive; <code>start <= limit
         * <= text.length()</code>.
         * @param setOfChars string with one or more distinct characters
         * @return Offset of the first character in <code>setOfChars</code>
         * found, or -1 if not found.
         * @see #indexOf
         */
        private static int quotedIndexOf(String text, int start, int limit,
                                         String setOfChars) {
            for (int i=start; i<limit; ++i) {
                char c = text.charAt(i);
                if (c == ESCAPE) {
                    ++i;
                } else if (c == QUOTE) {
                    while (++i < limit
                           && text.charAt(i) != QUOTE) {}
                } else if (setOfChars.indexOf(c) >= 0) {
                    return i;
                }
            }
            return -1;
        }



        /**
         * A range of Unicode characters.  Support the operations of testing for
         * inclusion (does this range contain this character?) and splitting.
         * Splitting involves breaking a range into two smaller ranges around a
         * character inside the original range.  The split character is not included
         * in either range.  If the split character is at either extreme end of the
         * range, one of the split products is an empty range.
         *
         * This class is used internally to determine the largest available private
         * use character range for variable stand-ins.
         */
        private static class Range implements Cloneable {
            char start;
            int length;

            Range(char start, int length) {
                this.start = start;
                this.length = length;
            }

            public Object clone() {
                return new Range(start, length);
            }

            boolean contains(char c) {
                return c >= start && (c - start) < length;
            }

            /**
             * Assume that contains(c) is true.  Split this range into two new
             * ranges around the character c.  Make this range one of the new ranges
             * (modify it in place) and return the other new range.  The character
             * itself is not included in either range.  If the split results in an
             * empty range (that is, if c == start or c == start + length - 1) then
             * return null.
             */
            Range split(char c) {
                if (c == start) {
                    ++start;
                    --length;
                    return null;
                } else if (c - start == length - 1) {
                    --length;
                    return null;
                } else {
                    ++c;
                    Range r = new Range(c, start + length - c);
                    length = --c - start;
                    return r;
                }
            }

            /**
             * Finds the largest unused subrange by the given string.  A
             * subrange is unused by a string if the string contains no
             * characters in that range.  If the given string contains no
             * characters in this range, then this range itself is
             * returned.
             */
            Range largestUnusedSubrange(RuleBody strings) {
                Vector v = new Vector(1);
                v.addElement(clone());

                strings.reset();
                for (;;) {
                    String str = strings.nextLine();
                    if (str == null) {
                        break;
                    }
                    int n = str.length();
                    for (int i=0; i<n; ++i) {
                        char c = str.charAt(i);
                        if (contains(c)) {
                            for (int j=0; j<v.size(); ++j) {
                                Range r = (Range) v.elementAt(j);
                                if (r.contains(c)) {
                                    r = r.split(c);
                                    if (r != null) {
                                        v.addElement(r);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }

                Range bestRange = null;
                for (int j=0; j<v.size(); ++j) {
                    Range r = (Range) v.elementAt(j);
                    if (bestRange == null || r.length > bestRange.length) {
                        bestRange = r;
                    }
                }

                return bestRange;
            }
        }
    }
}

/**
 * $Log: RuleBasedTransliterator.java,v $
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
 * fixed imports for com.ibm.util.Utility
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
