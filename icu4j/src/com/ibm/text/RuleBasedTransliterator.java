package com.ibm.text;

import java.util.Hashtable;
import java.util.Vector;
import java.text.ParsePosition;

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
 * <p>Copyright (c) IBM Corporation 1999-2000. All rights reserved.</p>
 *
 * @author Alan Liu
 * @version $RCSfile: RuleBasedTransliterator.java,v $ $Revision: 1.10 $ $Date: 2000/01/13 23:53:23 $
 *
 * $Log: RuleBasedTransliterator.java,v $
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
public class RuleBasedTransliterator extends Transliterator {
    /**
     * Direction constant passed to constructor to create a transliterator
     * using the forward rules.
     */
    public static final int FORWARD = 0;

    /**
     * Direction constant passed to constructor to create a transliterator
     * using the reverse rules.
     */
    public static final int REVERSE = 1;    

    private Data data;

    static final boolean DEBUG = false;

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
    }

    static Data parse(String[] rules, int direction) {
        return new Parser(rules, direction).getData();
    }

    static Data parse(String rules, int direction) {
        return parse(new String[] { rules }, direction);
    }

    /**
     * Transliterates a segment of a string.  <code>Transliterator</code> API.
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @param result buffer to receive the transliterated text; previous
     * contents are discarded
     */
    public void transliterate(String text, int start, int limit,
                              StringBuffer result) {
        /* In the following loop there is a virtual buffer consisting of the
         * text transliterated so far followed by the untransliterated text.  There is
         * also a cursor, which may be in the already transliterated buffer or just
         * before the untransliterated text.
         *
         * Example: rules 1. ab>x|y
         *                2. yc>z
         *
         * []|eabcd  start - no match, copy e to tranlated buffer
         * [e]|abcd  match rule 1 - copy output & adjust cursor
         * [ex|y]cd  match rule 2 - copy output & adjust cursor
         * [exz]|d   no match, copy d to transliterated buffer
         * [exzd]|   done
         *
         * cursor: an index into the virtual buffer, 0..result.length()-1.
         * Matches take place at the cursor.  If there is no match, the cursor
         * is advanced, and one character is moved from the source text to the
         * result buffer.
         *         
         * start, limit: these designate the substring of the source text which
         * has not been processed yet.  The range of offsets is start..limit-1.
         * At any moment the virtual buffer consists of result +
         * text.substring(start, limit).
         */
        int cursor = 0;
        result.setLength(0);
        while (start < limit || cursor < result.length()) {
            TransliterationRule r = data.ruleSet.findMatch(text, start, limit, result,
                                                      cursor, data.setVariables, getFilter());
            if (DEBUG) {
                StringBuffer buf = new StringBuffer(
                        result.toString() + '#' + text.substring(start, limit));
                buf.insert(cursor <= result.length()
                           ? cursor : (cursor + 1),
                           '|');
                System.err.print((r == null ? "nomatch:" : ("match:" + r + ", "))
                                 + buf);
            }

            if (r == null) {
                if (cursor == result.length()) {
                    result.append(text.charAt(start++));
                }
                ++cursor;
            } else {
                // resultPad is length of result to right of cursor; >= 0
                int resultPad = result.length() - cursor;
                char[] tail = null;
                if (r.getKeyLength() > resultPad) {
                    start += r.getKeyLength() - resultPad;
                } else if (r.getKeyLength() < resultPad) {
                    tail = new char[resultPad - r.getKeyLength()];
                    result.getChars(cursor + r.getKeyLength(), result.length(),
                                    tail, 0);
                }
                result.setLength(cursor);
                result.append(r.getOutput());
                if (tail != null) {
                    result.append(tail);
                }
                cursor += r.getCursorPos();
            }

            if (DEBUG) {
                StringBuffer buf = new StringBuffer(
                        result.toString() + '#' + text.substring(start, limit));
                buf.insert(cursor <= result.length()
                           ? cursor : (cursor + 1),
                           '|');
                System.err.println(" => " + buf);
            }
        }
    }

    /**
     * Transliterates a segment of a string.  <code>Transliterator</code> API.
     * @param text the string to be transliterated
     * @param start the beginning index, inclusive; <code>0 <= start
     * <= limit</code>.
     * @param limit the ending index, exclusive; <code>start <= limit
     * <= text.length()</code>.
     * @return The new limit index
     */
    public int transliterate(Replaceable text, int start, int limit) {
        /* When using Replaceable, the algorithm is simpler, since we don't have
         * two separate buffers.  We keep start and limit fixed the entire time,
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
        int cursor = start;
        while (cursor < limit) {
            TransliterationRule r = data.ruleSet.findMatch(text, start, limit,
                                                      cursor, data.setVariables, getFilter());
            if (r == null) {
                ++cursor;
            } else {
                text.replace(cursor, cursor + r.getKeyLength(), r.getOutput());
                limit += r.getOutput().length() - r.getKeyLength();
                cursor += r.getCursorPos();
            }
        }
        return limit;
    }

    /**
     * Implements {@link Transliterator#handleKeyboardTransliterate}.
     */
    protected void handleKeyboardTransliterate(Replaceable text,
                                               int[] index) {
        int start = index[START];
        int limit = index[LIMIT];
        int cursor = index[CURSOR];

        if (DEBUG) {
            System.out.print("\"" +
                escape(rsubstring(text, start, cursor)) + '|' +
                escape(rsubstring(text, cursor, limit)) + "\"");
        }

        boolean partial[] = new boolean[1];

        while (cursor < limit) {
            TransliterationRule r = data.ruleSet.findIncrementalMatch(
                    text, start, limit, cursor, data.setVariables, partial, getFilter());
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
                    ++cursor;
                }
            } else {
                text.replace(cursor, cursor + r.getKeyLength(), r.getOutput());
                limit += r.getOutput().length() - r.getKeyLength();
                cursor += r.getCursorPos();
            }
        }

        if (DEBUG) {
            System.out.println(" -> \"" +
                escape(rsubstring(text, start, cursor)) + '|' + 
                escape(rsubstring(text, cursor, cursor)) + '|' + 
                escape(rsubstring(text, cursor, limit)) + "\"");
        }

        index[LIMIT] = limit;
        index[CURSOR] = cursor;
    }

    /**
     * Returns the length of the longest context required by this transliterator.
     * This is <em>preceding</em> context.
     * @return Maximum number of preceding context characters this
     * transliterator needs to examine
     */
    protected int getMaximumContextLength() {
        return data.ruleSet.getMaximumContextLength();
    }


    /**
     * FOR DEBUGGING: Return a substring of a Replaceable.
     */
    private static String rsubstring(Replaceable r, int start, int limit) {
        StringBuffer buf = new StringBuffer();
        while (start < limit) {
            buf.append(r.charAt(start++));
        }
        return buf.toString();
    }

    /**
     * FOR DEBUGGING: Escape non-ASCII characters as Unicode.
     */
    private static final String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c >= ' ' && c <= 0x007F) {
                if (c == '\\') {
                    buf.append("\\\\"); // That is, "\\"
                } else {
                    buf.append(c);
                }
            } else {
                buf.append("\\u");
                if (c < 0x1000) {
                    buf.append('0');
                    if (c < 0x100) {
                        buf.append('0');
                        if (c < 0x10) {
                            buf.append('0');
                        }
                    }
                }
                buf.append(Integer.toHexString(c));
            }
        }
        return buf.toString();
    }





    static class Data {
        public Data() {
            variableNames = new Hashtable();
            setVariables = new Hashtable();
            ruleSet = new TransliterationRuleSet();
        }

        /**
         * Rule table.  May be empty.
         */
        public TransliterationRuleSet ruleSet;

        /**
         * Map variable name (String) to variable (Character).  A variable
         * name may correspond to a single literal character, in which
         * case the character is stored in this hash.  It may also
         * correspond to a UnicodeSet, in which case a character is
         * again stored in this hash, but the character is a stand-in: it
         * is a key for a secondary lookup in data.setVariables.  The stand-in
         * also represents the UnicodeSet in the stored rules.
         */
        public Hashtable variableNames;

        /**
         * Map category variable (Character) to set (UnicodeSet).
         * Variables that correspond to a set of characters are mapped
         * from variable name to a stand-in character in data.variableNames.
         * The stand-in then serves as a key in this hash to lookup the
         * actual UnicodeSet object.  In addition, the stand-in is
         * stored in the rule text to represent the set of characters.
         */
        public Hashtable setVariables;
    }






    private static class Parser {
        /**
         * Current rule being parsed.
         */
        private String rules;

        private int direction;

        private Data data;

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
         * <code>variableNext..variableLimit-1</code>.
         */
        private char variableLimit;

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

        private static final char VARIABLE_REF_OPEN   = '{';
        private static final char VARIABLE_REF_CLOSE  = '}';
        private static final char CONTEXT_OPEN        = '(';
        private static final char CONTEXT_CLOSE       = ')';
        private static final char SET_OPEN            = '[';
        private static final char SET_CLOSE           = ']';
        private static final char CURSOR_POS          = '|';

        /**
         * @param rules list of rules, separated by semicolon characters
         * @exception IllegalArgumentException if there is a syntax error in the
         * rules
         */
        public Parser(String[] ruleArray, int direction) {
            this.direction = direction;
            data = new Data();
            parseRules(ruleArray);
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
        private void parseRules(String[] ruleArray) {
            determineVariableRange(ruleArray);

            StringBuffer errors = null;

            try {
                for (int i=0; i<ruleArray.length; ++i) {
                    String rule = ruleArray[i];
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
                        // We've found the start of a rule.  c is its first
                        // character, and pos points past c.  Lexically parse the
                        // rule into component pieces.
                        pos = parseRule(rule, --pos, limit);                    
                    }
                }
            } catch (IllegalArgumentException e) {
                // errors = new StringBuffer(e.getMessage());
            }
            
            // Index the rules
            try {
                data.ruleSet.freeze(data.setVariables);
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
         */
        private int parseRule(String rule, int pos, int limit) {
            // Locate the left side, operator, and right side
            int start = pos;
            char operator = 0;

            StringBuffer buf = new StringBuffer();
            int cursor = -1; // position of cursor in buf
            int ante = -1;   // position of ante context marker ')' in buf
            int post = -1;   // position of post context marker '(' in buf
            int postClose = -1; // position of post context close ')' in buf

            // Assigned to buf and its adjuncts after the LHS has been
            // parsed.  Thereafter, buf etc. refer to the RHS.
            String left = null;
            int leftCursor = -1, leftAnte = -1, leftPost = -1, leftPostClose = -1;

        main:
            while (pos < limit) {
                char c = rule.charAt(pos++);
                if (Character.isWhitespace(c)) {
                    // Ignore whitespace.  Note that this is not Unicode
                    // spaces, but Java spaces -- a subset, representing
                    // whitespace likely to be seen in code.
                    continue;
                }
                // Handle escapes
                if (c == ESCAPE) {
                    if (pos == limit) {
                        syntaxError("Trailing backslash", rule, start);
                    }
                    buf.append(rule.charAt(pos++));
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
                if (OPERATORS.indexOf(c) >= 0) {
                    if (operator != 0) {
                        syntaxError("Unquoted " + c, rule, start);
                    }
                    // Found an operator char.  Check for forward-reverse operator.
                    if (c == REVERSE_RULE_OP &&
                        (pos < limit && rule.charAt(pos) == FORWARD_RULE_OP)) {
                        ++pos;
                        operator = FWDREV_RULE_OP;
                    } else {
                        operator = c;
                    }
                    left = buf.toString(); // lhs
                    leftCursor = cursor;
                    leftAnte = ante;
                    leftPost = post;
                    leftPostClose = postClose;

                    buf.setLength(0);
                    cursor = ante = post = postClose = -1;
                    continue;
                }
                switch (c) {
                case END_OF_RULE:
                    break main;
                case VARIABLE_REF_OPEN:
                    {
                        int j = rule.indexOf(VARIABLE_REF_CLOSE, pos);
                        if (pos == j || j < 0) { // empty or unterminated
                            syntaxError("Malformed variable reference", rule, start);
                        }
                        String name = rule.substring(pos, j);
                        pos = j+1;
                        buf.append(getVariableDef(name));
                    }
                    break;
                case CONTEXT_OPEN:
                    if (post >= 0) {
                        syntaxError("Multiple post contexts", rule, start);
                    }
                    // Ignore CONTEXT_OPEN if buffer length is zero -- that means
                    // this is the optional opening delimiter for the ante context.
                    if (buf.length() > 0) {
                        post = buf.length();
                    }
                    break;
                case CONTEXT_CLOSE:
                    if (postClose >= 0) {
                        syntaxError("Unexpected " + c, rule, start);
                    }
                    if (post >= 0) {
                        // This is probably the optional closing delimiter
                        // for the post context; save the pos and check later.
                        postClose = buf.length();
                    } else if (ante >= 0) {
                        syntaxError("Multiple ante contexts", rule, start);
                    } else {
                        ante = buf.length();
                    }
                    break;
                case SET_OPEN:
                    ParsePosition pp = new ParsePosition(pos-1); // Backup to opening '['
                    buf.append(registerSet(new UnicodeSet(rule, pp,
                                   data.variableNames, data.setVariables)));
                    pos = pp.getIndex();
                    break;
                case VARIABLE_REF_CLOSE:
                case SET_CLOSE:
                    syntaxError("Unquoted " + c, rule, start);
                case CURSOR_POS:
                    if (cursor >= 0) {
                        syntaxError("Multiple cursors", rule, start);
                    }
                    cursor = buf.length();
                    break;
                default:
                    buf.append(c);
                    break;
                }
            }
            if (operator == 0) {
                syntaxError("No operator", rule, start);
            }

            // Check context close parameters
            if ((leftPostClose >= 0 && leftPostClose != left.length()) ||
                (postClose >= 0 && postClose != buf.length())) {
                syntaxError("Extra text after ]", rule, start);
            }

            // Context is only allowed on the input side; that is, the left side
            // for forward rules.  Cursors are only allowed on the output side;
            // that is, the right side for forward rules.  Bidirectional rules
            // ignore elements that do not apply.

            switch (operator) {
            case VARIABLE_DEF_OP:
                // LHS is the name.  RHS is a single character, either a literal
                // or a set (already parsed).  If RHS is longer than one
                // character, it is either a multi-character string, or multiple
                // sets, or a mixture of chars and sets -- syntax error.
                if (buf.length() != 1) {
                    syntaxError("Malformed RHS", rule, start);
                }
                if (data.variableNames.get(left) != null) {
                    syntaxError("Duplicate definition of {" +
                                left + "}", rule, start);
                }
                data.variableNames.put(left, new Character(buf.charAt(0)));
                break;

            case FORWARD_RULE_OP:
                if (direction == FORWARD) {
                    if (ante >= 0 || post >= 0 || leftCursor >= 0) {
                        syntaxError("Malformed rule", rule, start);
                    }
                    data.ruleSet.addRule(new TransliterationRule(
                                             left, leftAnte, leftPost,
                                             buf.toString(), cursor));
                } // otherwise ignore the rule; it's not the direction we want
                break;

            case REVERSE_RULE_OP:
                if (direction == REVERSE) {
                    if (leftAnte >= 0 || leftPost >= 0 || cursor >= 0) {
                        syntaxError("Malformed rule", rule, start);
                    }
                    data.ruleSet.addRule(new TransliterationRule(
                                             buf.toString(), ante, post,
                                             left, leftCursor));
                } // otherwise ignore the rule; it's not the direction we want
                break;

            case FWDREV_RULE_OP:
                if (direction == FORWARD) {
                    // The output side is the right; trim off any context
                    String output = buf.toString().substring(ante < 0 ? 0 : ante,
                                                             post < 0 ? buf.length() : post);
                    data.ruleSet.addRule(new TransliterationRule(
                                             left, leftAnte, leftPost,
                                             output, cursor));
                } else {
                    // The output side is the left; trim off any context
                    String output = left.substring(leftAnte < 0 ? 0 : leftAnte,
                                                   leftPost < 0 ? left.length() : leftPost);
                    data.ruleSet.addRule(new TransliterationRule(
                                             buf.toString(), ante, post,
                                             output, leftCursor));
                }
                break;
            }

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
        private static final void syntaxError(String msg, String rule, int start) {
            int end = quotedIndexOf(rule, start, rule.length(), ";");
            if (end < 0) {
                end = rule.length();
            }
            throw new IllegalArgumentException(msg + " in " +
                                               rule.substring(start, end));
        }
        
        /**
         * Allocate a private-use substitution character for the given set,
         * register it in the setVariables hash, and return the substitution
         * character.
         */
        private final char registerSet(UnicodeSet set) {
            if (variableNext >= variableLimit) {
                throw new RuntimeException("Private use variables exhausted");
            }
            Character c = new Character(variableNext++);
            data.setVariables.put(c, set);
            return c.charValue();
        }

        /**
         * Returns the single character value of the given variable name.  Defined
         * names are recognized.
         * @exception IllegalArgumentException if the name is unknown.
         */
        private char getVariableDef(String name) {
            Character ch = (Character) data.variableNames.get(name);
            if (ch == null) {
                throw new IllegalArgumentException("Undefined variable: "
                                                   + name);
            }
            return ch.charValue();
        }

        /**
         * Determines what part of the private use region of Unicode we can use for
         * variable stand-ins.  The correct way to do this is as follows: Parse each
         * rule, and for forward and reverse rules, take the FROM expression, and
         * make a hash of all characters used.  The TO expression should be ignored.
         * When done, everything not in the hash is available for use.  In practice,
         * this method may employ some other algorithm for improved speed.
         */
        private final void determineVariableRange(String[] ruleArray) {
            // As an initial implementation, we just run through all the
            // characters, ignoring any quoting.  This works since the quote
            // mechanisms are outside the private use area.

            Range r = new Range('\uE000', 0x1900); // Private use area
            r = r.largestUnusedSubrange(ruleArray);
            
            if (r == null) {
                throw new RuntimeException(
                    "No private use characters available for variables");
            }

            variableNext = r.start;
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
            Range largestUnusedSubrange(String[] strings) {
                Vector v = new Vector(1);
                v.addElement(clone());

                for (int k=0; k<strings.length; ++k) {
                    String str = strings[k];
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
