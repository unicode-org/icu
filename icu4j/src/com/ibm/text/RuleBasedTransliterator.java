package com.ibm.text;

import java.util.Hashtable;
import java.util.Vector;

/**
 * A transliterator that reads a set of rules in order to determine how to
 * perform translations.  Rules are stored in resource bundles indexed by name.
 * Rules are separated by semicolons (';').  To include a literal
 * semicolon, prefix it with a backslash ('\\;').  Whitespace is significant.  If
 * the first character on a line is '#', the entire line is ignored as a
 * comment.
 *
 * <p>Each set of rules consists of two groups, one forward, and one reverse.
 * This is a convention that is not enforced; rules for one direction may be
 * omitted, with the result that translations in that direction will not modify
 * the source text.
 *
 * <p><b>Rule syntax</b>
 *
 * <p>Rule statements take one of the following forms:
 * <dl>
 *   <dt><code>alefmadda=&#092;u0622</code></dt>
 *
 *   <dd><strong>Variable definition.</strong> The name on the left is
 *   assigned the character or expression on the right. Names may not
 *   contain any special characters (see list below). Duplicate names
 *   (including duplicates of simple variables or category names)
 *   cause an exception to be thrown.  If the right hand side consists
 *   of one character, then the variable stands for that character.
 *   In this example, after this statement, instances of the left hand
 *   name surrounded by braces, &quot;<code>{alefmadda}</code>&quot,
 *   will be replaced by the Unicode character U+0622.</dd> If the
 *   right hand side is longer than one character, then it is
 *   interpreted as a character category expression; see below for
 *   details.
 *
 *   <dt><code>softvowel=[eiyEIY]</code></dt>
 *
 *   <dd><strong>Category definition.</strong> The name on the left is assigned
 *   to stand for a set of characters.  The same rules for names of simple
 *   variables apply. After this statement, the left hand variable will be
 *   interpreted as indicating a set of characters in appropriate contexts. The
 *   pattern syntax defining sets of characters is defined by {@link UnicodeSet}.
 *   Examples of valid patterns are:<table>
 *
 *       <tr valign=top>
 *         <td nowrap><code>[abc]</code></td>
 *         <td>The set containing the characters 'a', 'b', and 'c'.</td>
 *       </tr>
 *       <tr valign=top>
 *         <td nowrap><code>[^abc]</code></td>
 *         <td>The set of all characters <em>except</em> 'a', 'b', and 'c'.</td>
 *       </tr>
 *       <tr valign=top>
 *         <td nowrap><code>[A-Z]</code></td>
 *         <td>The set of all characters from 'A' to 'Z' in Unicode order.</td>
 *       </tr>
 *       <tr valign=top>
 *         <td nowrap><code>[:Lu:]</code></td>
 *         <td>The set of Unicode uppercase letters. See
 *         <a href="http://www.unicode.org">www.unicode.org</a>
 *         for a complete list of categories and their two-letter codes.</td>
 *       </tr>
 *       <tr valign=top>
 *         <td nowrap><code>[^a-z[:Lu:][:Ll:]]</code></td>
 *         <td>The set of all characters <em>except</em> 'a' through 'z' and
 *         uppercase or lowercase letters.</td>
 *       </tr>
 *     </table>
 *
 *   See {@link UnicodeSet} for more documentation and examples.
 *   </dd>
 *
 *   <dt><code>ai&gt;{alefmadda}</code></dt>
 *
 *   <dd><strong>Forward translation rule.</strong> This rule states that the
 *   string on the left will be changed to the string on the right when
 *   performing forward transliteration.</dd>
 *
 *   <dt><code>ai&lt;{alefmadda}</code></dt>
 *
 *   <dd><strong>Reverse translation rule.</strong> This rule states that the
 *   string on the right will be changed to the string on the left when
 *   performing reverse transliteration.</dd>
 *
 * </dl>
 *
 * <p>Forward and reverse translation rules consist of a <em>match
 * pattern</em> and an <em>output string</em>.  The match pattern consists
 * of literal characters, optionally preceded by context, and optionally
 * followed by context.  Context characters, like literal pattern characters,
 * must be matched in the text being transliterated.  However, unlike literal
 * pattern characters, they are not replaced by the output text.  For example,
 * the pattern "<code>[abc]def</code>" indicates the characters
 * "<code>def</code>" must be preceded by "<code>abc</code>" for a successful
 * match.  If there is a successful match, "<code>def</code>" will be replaced,
 * but not "<code>abc</code>".  The initial '<code>[</code>' is optional, so
 * "<code>abc]def</code>" is equivalent to "<code>[abc]def</code>".  Another
 * example is "<code>123[456]</code>" (or "<code>123[456</code>") in which the
 * literal pattern "<code>123</code>" must be followed by "<code>456</code>".
 *
 * <p>The output string of a forward or reverse rule consists of characters to
 * replace the literal pattern characters.  If the output string contains the
 * character '<code>|</code>', this is taken to indicate the location of the
 * <em>cursor</em> after replacement.  The cursor is the point in the text
 * at which the next replacement, if any, will be applied.
 *
 * <p><b>Example</b>
 *
 * <p>The following example rules illustrate many of the features of the rule
 * language.
 * <table cellpadding="4">
 * <tr valign=top><td>Rule 1.</td>
 *     <td nowrap><code>abc]def&gt;x|y</code></td></tr>
 * <tr valign=top><td>Rule 2.</td>
 *     <td nowrap><code>xyz&gt;r</code></td></tr>
 * <tr valign=top><td>Rule 3.</td>
 *     <td nowrap><code>yz&gt;q</code></td></tr>
 * </table>
 *
 * <p>Applying these rules to the string "<code>adefabcdefz</code>" yields the
 * following results:
 *
 * <table cellpadding="4">
 * <tr valign=top><td nowrap><code>|adefabcdefz</code></td>
 *     <td>Initial state, no rules match.  Advance cursor.</td></tr>
 * <tr valign=top><td nowrap><code>a|defabcdefz</code></td>
 *     <td>Still no match.  Rule 1 does not match because the preceding
 *     context is not present.</td></tr>
 * <tr valign=top><td nowrap><code>ad|efabcdefz</code></td>
 *     <td>Still no match.  Keep advancing until there is a match...</td></tr>
 * <tr valign=top><td nowrap><code>ade|fabcdefz</code></td>
 *     <td>...</td></tr>
 * <tr valign=top><td nowrap><code>adef|abcdefz</code></td>
 *     <td>...</td></tr>
 * <tr valign=top><td nowrap><code>adefa|bcdefz</code></td>
 *     <td>...</td></tr>
 * <tr valign=top><td nowrap><code>adefab|cdefz</code></td>
 *     <td>...</td></tr>
 * <tr valign=top><td nowrap><code>adefabc|defz</code></td>
 *     <td>Rule 1 matches; replace "<code>def</code>" with "<code>xy</code>"
 *     and back up the cursor to before the '<code>y</code>'.</td></tr>
 * <tr valign=top><td nowrap><code>adefabcx|yz</code></td>
 *     <td>Although "<code>xyz</code>" is present, rule 2 does not match
 *     because the cursor is before the '<code>y</code>', not before the
 *     '<code>x</code>'.  Rule 3 does match.  Replace "<code>yz</code>" with
 *     "<code>q</code>".</td></tr>
 * <tr valign=top><td nowrap><code>adefabcxq|</code></td>
 *     <td>The cursor is at the end; transliteration is complete.</td></tr>
 * </table>
 *
 * <p>The order of rules is significant.  If multiple rules may match at some
 * point, the first matching rule is applied.
 *
 * <p>Forward and reverse rules may have an empty output string.  Otherwise, an
 * empty left or right hand side of any statement is a syntax error.
 *
 * <p>Single quotes are used to quote the special characters
 * <code>=&gt;&lt;{}[]|</code>.  To specify a single quote itself, inside or
 * outside of quotes, use two single quotes in a row.  For example, the rule
 * "<code>'&gt;'&gt;o''clock</code>" changes the string "<code>&gt;</code>" to
 * the string "<code>o'clock</code>".
 *
 * <p><b>Notes</b>
 *
 * <p>While a RuleBasedTransliterator is being built, it checks that the rules
 * are added in proper order.  For example, if the rule "a>x" is followed by the
 * rule "ab>y", then the second rule will throw an exception.  The reason is
 * that the second rule can never be triggered, since the first rule always
 * matches anything it matches.  In other words, the first rule <em>masks</em>
 * the second rule.  There is a cost of O(n^2) to make this check; in real-world
 * tests it appears to approximately double build time.
 *
 * <p>One optimization that can be made is to add a pragma to the rule language,
 * "#pragma order", that turns off ordering checking.  This pragma can then be
 * added to all of our resource-based rules (after we build these once and
 * determine that there are no ordering errors).  I haven't made this change yet
 * in the interests of keeping the code from getting too byzantine.
 *
 * <p>Copyright &copy; IBM Corporation 1999.  All rights reserved.
 *
 * @author Alan Liu
 * @version $RCSfile: RuleBasedTransliterator.java,v $ $Revision: 1.7 $ $Date: 2000/01/06 01:36:36 $
 *
 * $Log: RuleBasedTransliterator.java,v $
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
 *
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

    static final boolean CHECK_MASKING = true;

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
        private static final char FWDREV_RULE_OP    = '~'; // internal rep of FWDREF_OP_STRING

        private static final String OPERATORS = "=><";

        // Forward-Reverse operator
        // a<>b is equivalent to a<b;a>b
        private static final String FWDREV_OP_STRING  = "<>"; // must have length 2

        // Other special characters
        private static final char QUOTE               = '\'';
        private static final char VARIABLE_REF_OPEN   = '{';
        private static final char VARIABLE_REF_CLOSE  = '}';
        private static final char CONTEXT_OPEN        = '[';
        private static final char CONTEXT_CLOSE       = ']';
        private static final char CURSOR_POS          = '|';
        private static final char RULE_COMMENT_CHAR   = '#';

        /**
         * Specials must be quoted in rules to be used as literals.
         * Specials may not occur in variable names.
         */
        private static final String SPECIALS = "'{}[]|#" + OPERATORS;

        /**
         * Specials that must be quoted in variable definitions.
         */
        private static final String DEF_SPECIALS = "'{}";

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
         * Parse the given string as a sequence of rules, separated by semicolon
         * characters (';'), and cause this object to implement those rules.  Any
         * previous rules are discarded.  Typically this method is called exactly
         * once, during construction.
         * @exception IllegalArgumentException if there is a syntax error in the
         * rules
         */
        private void parseRules(String[] ruleArray) {
            determineVariableRange(ruleArray);

            StringBuffer errors = null;
            for (int irule=0; irule<ruleArray.length; ++irule) {
                rules = ruleArray[irule];
                int n = rules.length();
                int i = 0;
                while (i<n) {
                    int limit = rules.indexOf(';', i);

                    // Recognize "\\;" as an escaped ";"
                    while (limit>0 && rules.charAt(limit-1) == '\\') {
                        limit = rules.indexOf(';', limit+1);
                    }

                    if (limit == -1) {
                        limit = n;
                    }
                    // Skip over empty lines and line starting with #
                    if (limit > i && rules.charAt(i) != RULE_COMMENT_CHAR) {
                        try {
                            applyRule(i, limit);
                        } catch (IllegalArgumentException e) {
                            if (errors == null) {
                                errors = new StringBuffer(e.getMessage());
                            } else {
                                errors.append("\n").append(e.getMessage());
                            }
                        }
                    }
                    i = limit + 1;
                }
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
         * Parse the given substring as a rule, and append it to the rules currently
         * represented in this object.
         * @param start the beginning index, inclusive; <code>0 <= start
         * <= limit</code>.
         * @param limit the ending index, exclusive; <code>start <= limit
         * <= rules.length()</code>.
         * @exception IllegalArgumentException if there is a syntax error in the
         * rules
         */
        private void applyRule(int start, int limit) {
            /* General description of parsing: Initially, rules contain two types of
             * quoted characters.  First, there are variable references, such as
             * "{alpha}".  Second, there are quotes, such as "'<'" or "''".  One of
             * the first steps in parsing a rule is to resolve such quoted matter.
             * Quotes are removed early, leaving unquoted literal matter.  Variable
             * references are resolved and replaced by single characters.  In some
             * instances these characters represent themselves; in others, they
             * stand for categories of characters.  Character categories are either
             * predefined (e.g., "{Lu}"), or are defined by the user using a
             * statement (e.g., "vowels:aeiouAEIOU").
             *
             * Another early step in parsing is to split each rule into component
             * pieces.  These pieces are, for every rule, a left-hand side, a right-
             * hand side, and an operator.  The left- and right-hand sides may not
             * be empty, except for the output patterns of forward and reverse
             * rules.  In addition to this partitioning, the match patterns of
             * forward and reverse rules must be partitioned into antecontext,
             * postcontext, and literal pattern, where the context portions may or
             * may not be present.  Finally, output patterns must have the cursor
             * indicator '|' detected and removed, with its position recorded.
             *
             * Quote removal, variable resolution, and sub-pattern splitting must
             * all happen at once.  This is due chiefly to the quoting mechanism,
             * which allows special characters to appear at arbitrary positions in
             * the final unquoted text.  (For this reason, alteration of the rule
             * language is somewhat clumsy; it entails reassessment and revision of
             * the parsing methods as a whole.)
             *
             * After this processing of rules is complete, the final end products
             * are unquoted pieces of text of various types, and an integer cursor
             * position, if one is specified.  These processed raw materials are now
             * easy to deal with; other classes such as UnicodeSet and
             * TransliterationRule need know nothing of quoting or variables.
             */
            StringBuffer left = new StringBuffer();
            StringBuffer right = new StringBuffer();
            StringBuffer anteContext = new StringBuffer();
            StringBuffer postContext = new StringBuffer();
            int cursorPos[] = new int[1];

            char operator = parseRule(start, limit, left, right,
                                      anteContext, postContext, cursorPos);

            switch (operator) {
            case VARIABLE_DEF_OP:
                applyVariableDef(left.toString(), right.toString());
                break;
            case FORWARD_RULE_OP:
                if (direction == FORWARD) {
                    data.ruleSet.addRule(new TransliterationRule(
                                             left.toString(), right.toString(),
                                             anteContext.toString(), postContext.toString(),
                                             cursorPos[0]));
                } // otherwise ignore the rule; it's not the direction we want
                break;
            case REVERSE_RULE_OP:
                if (direction == REVERSE) {
                    data.ruleSet.addRule(new TransliterationRule(
                                             right.toString(), left.toString(),
                                             anteContext.toString(), postContext.toString(),
                                             cursorPos[0]));
                } // otherwise ignore the rule; it's not the direction we want
                break;
            case FWDREV_RULE_OP:
                data.ruleSet.addRule(new TransliterationRule(
                                         direction == FORWARD ? left.toString() : right.toString(),
                                         direction == FORWARD ? right.toString() : left.toString(),
                                         // Context & cursor disallowed
                                         "", "", -1));
                break;
            }
        }

        /**
         * Add a variable definition.
         * @param name the name of the variable.  It must not already be defined.
         * @param pattern the value of the variable.  It may be a single character
         * or a pattern describing a character set.
         * @exception IllegalArgumentException if there is a syntax error
         */
        private final void applyVariableDef(String name, String pattern) {
            validateVariableName(name);
            if (data.variableNames.get(name) != null) {
                throw new IllegalArgumentException("Duplicate variable definition: "
                                                   + name + '=' + pattern);
            }
//!         if (UnicodeSet.getCategoryID(name) >= 0) {
//!             throw new IllegalArgumentException("Reserved variable name: "
//!                                                + name);
//!         }
            if (pattern.length() < 1) {
                throw new IllegalArgumentException("Variable definition missing: "
                                                   + name);
            }
            if (pattern.length() == 1) {
                // Got a single character variable definition
                data.variableNames.put(name, new Character(pattern.charAt(0)));
            } else {
                // Got more than one character; parse it as a category
                if (variableNext >= variableLimit) {
                    throw new RuntimeException("Private use variables exhausted");
                }
                Character c = new Character(variableNext++);
                data.variableNames.put(name, c);
                data.setVariables.put(c, new UnicodeSet(pattern));
            }
        }

        /**
         * Given a rule, parses it into three pieces: The left side, the right side,
         * and the operator.  Returns the operator.  Quotes and variable references
         * are resolved; the otuput text in all <code>StringBuffer</code> parameters
         * is literal text.  This method delegates to other parsing methods to
         * handle the match pattern, output pattern, and other sub-patterns in the
         * rule.
         * @param start the beginning index, inclusive; <code>0 <= start
         * <= limit</code>.
         * @param limit the ending index, exclusive; <code>start <= limit
         * <= rules.length()</code>.
         * @param left left side of rule is appended to this buffer
         * with the quotes removed and variables resolved
         * @param right right side of rule is appended to this buffer
         * with the quotes removed and variables resolved
         * @param anteContext the preceding context of the match pattern,
         * if there is one, is appended to this buffer
         * @param postContext the following context of the match pattern,
         * if there is one, is appended to this buffer
         * @param cursorPos if there is a cursor in the output pattern, its
         * offset is stored in <code>cursorPos[0]</code>
         * @return The operator character, one of the characters in OPERATORS.
         */
        private char parseRule(int start, int limit,
                               StringBuffer left, StringBuffer right,
                               StringBuffer anteContext,
                               StringBuffer postContext,
                               int[] cursorPos) {
            if (false) {
                System.err.println("Parsing " + rules.substring(start, limit));
            }
            /* Parse the rule into three pieces -- left, operator, and right,
             * parsing out quotes.  The result is that left and right will have
             * unquoted text.  E.g., "gt<'>'" will have right = ">".  Unquoted
             * operators throw an exception.  Two quotes inside or outside
             * quotes indicates a quote literal.  E.g., "o''clock" -> "o'clock".
             */
            int i = quotedIndexOf(rules, start, limit, OPERATORS);
            if (i < 0) {
                throw new IllegalArgumentException(
                              "Syntax error: "
                              + rules.substring(start, limit));
            }
            char c = rules.charAt(i);
            
            // Look for "<>" double rules.
            if ((i+1) < limit && rules.substring(i, i+2).equals(FWDREV_OP_STRING)) {
                if (i == start) {
                    throw new IllegalArgumentException(
                                  "Empty left side: "
                                  + rules.substring(start, limit));
                }
                if (i+2 == limit) {
                    throw new IllegalArgumentException(
                                  "Empty right side: "
                                  + rules.substring(start, limit));
                }
                parseSubPattern(start, i, left, null, SPECIALS);
                parseSubPattern(i+2, limit, right, null, SPECIALS);
                return FWDREV_RULE_OP;
            }

            switch (c) {
            case FORWARD_RULE_OP:
                if (i == start) {
                    throw new IllegalArgumentException(
                                  "Empty left side: "
                                  + rules.substring(start, limit));
                }
                parseMatchPattern(start, i, left, anteContext, postContext);
                if (i != (limit-1)) {
                    parseOutputPattern(i+1, limit, right, cursorPos);
                }
                break;
            case REVERSE_RULE_OP:
                if (i == (limit-1)) {
                    throw new IllegalArgumentException(
                                  "Empty right side: "
                                  + rules.substring(start, limit));
                }
                if (i != start) {
                    parseOutputPattern(start, i, left, cursorPos);
                }
                parseMatchPattern(i+1, limit, right, anteContext, postContext);
                break;
            case VARIABLE_DEF_OP:
                if (i == start || i == (limit-1)) {
                    throw new IllegalArgumentException(
                                  "Empty left or right side: "
                                  + rules.substring(start, limit));
                }
                parseSubPattern(start, i, left);
                parseDefPattern(i+1, limit, right);
                break;
            default:
                throw new RuntimeException();
            }
            return c;
        }

        /**
         * Parses the match pattern of a forward or reverse rule.  Given the raw
         * match pattern, return the match text and the context on both sides, if
         * any.  Resolves all quotes and variables.
         * @param start the beginning index, inclusive; <code>0 <= start
         * <= limit</code>.
         * @param limit the ending index, exclusive; <code>start <= limit
         * <= rules.length()</code>.
         * @param text the key to be matched will be appended to this buffer
         * @param anteContext the preceding context, if any, will be appended
         * to this buffer.
         * @param postContext the following context, if any, will be appended
         * to this buffer.
         */
        private void parseMatchPattern(int start, int limit,
                                       StringBuffer text,
                                       StringBuffer anteContext,
                                       StringBuffer postContext) {
            if (start >= limit) {
                throw new IllegalArgumentException(
                              "Empty expression in rule: "
                              + rules.substring(start, limit));
            }
            if (anteContext != null) {
                // Ignore optional opening and closing context characters
                if (rules.charAt(start) == CONTEXT_OPEN) {
                    ++start;
                }
                if (rules.charAt(limit-1) == CONTEXT_CLOSE) {
                    --limit;
                }
                // The four possibilities are:
                //             key
                // anteContext]key
                // anteContext]key[postContext
                //             key[postContext
                int ante = quotedIndexOf(rules, start, limit, String.valueOf(CONTEXT_CLOSE));
                int post = quotedIndexOf(rules, start, limit, String.valueOf(CONTEXT_OPEN));
                if (ante >= 0 && post >= 0 && ante > post) {
                    throw new IllegalArgumentException(
                                  "Syntax error in context specifier: "
                                  + rules.substring(start, limit));
                }
                if (ante >= 0) {
                    parseSubPattern(start, ante, anteContext);
                    start = ante+1;
                }
                if (post >= 0) {
                    parseSubPattern(post+1, limit, postContext);
                    limit = post;
                }
            }
            parseSubPattern(start, limit, text);
        }

        private final void parseSubPattern(int start, int limit,
                                           StringBuffer text) {
            parseSubPattern(start, limit, text, null, SPECIALS);
        }

        /**
         * Parse a variable definition sub pattern.  This kind of sub
         * pattern differs in the set of characters that are considered
         * special.  In particular, the '[' and ']' characters are not
         * special, since these are used in UnicodeSet patterns.
         */
        private final void parseDefPattern(int start, int limit,
                                           StringBuffer text) {
            parseSubPattern(start, limit, text, null, DEF_SPECIALS);
        }

        /**
         * Parses the output pattern of a forward or reverse rule.  Given the
         * output pattern, return the output text and the position of the cursor,
         * if any.  Resolves all quotes and variables.
         * @param rules the string to be parsed
         * @param start the beginning index, inclusive; <code>0 <= start
         * <= limit</code>.
         * @param limit the ending index, exclusive; <code>start <= limit
         * <= rules.length()</code>.
         * @param text the output text will be appended to this buffer
         * @param cursorPos if this parameter is not null, then cursorPos[0]
         * will be set to the cursor position, or -1 if there is none.  If this
         * parameter is null, then cursors will be disallowed.
         */
        private final void parseOutputPattern(int start, int limit,
                                              StringBuffer text,
                                              int[] cursorPos) {
            parseSubPattern(start, limit, text, cursorPos, SPECIALS);
        }

        /**
         * Parses a sub-pattern of a rule.  Return the text and the position of the cursor,
         * if any.  Resolves all quotes and variables.
         * @param rules the string to be parsed
         * @param start the beginning index, inclusive; <code>0 <= start
         * <= limit</code>.
         * @param limit the ending index, exclusive; <code>start <= limit
         * <= rules.length()</code>.
         * @param text the output text will be appended to this buffer
         * @param cursorPos if this parameter is not null, then cursorPos[0]
         * will be set to the cursor position, or -1 if there is none.  If this
         * parameter is null, then cursors will be disallowed.
         * @param specials characters that must be quoted; typically either
         * SPECIALS or DEF_SPECIALS.
         */
        private void parseSubPattern(int start, int limit,
                                     StringBuffer text,
                                     int[] cursorPos,
                                     String specials) {
            boolean inQuote = false;

            if (start >= limit) {
                throw new IllegalArgumentException("Empty expression in rule");
            }
            if (cursorPos != null) {
                cursorPos[0] = -1;
            }
            for (int i=start; i<limit; ++i) {
                char c = rules.charAt(i);
                if (c == QUOTE) {
                    // Check for double quote
                    if ((i+1) < limit
                        && rules.charAt(i+1) == QUOTE) {
                        text.append(QUOTE);
                        ++i; // Skip over both quotes
                    } else {
                        inQuote = !inQuote;
                    }
                } else if (inQuote) {
                    text.append(c);
                } else if (c == VARIABLE_REF_OPEN) {
                    ++i;
                    int j = rules.indexOf(VARIABLE_REF_CLOSE, i);
                    if (i == j || j < 0) { // empty or unterminated
                        throw new IllegalArgumentException("Illegal variable reference: "
                                                           + rules.substring(start, limit));
                    }
                    String name = rules.substring(i, j);
                    validateVariableName(name);
                    text.append(getVariableDef(name).charValue());
                    i = j;
                } else if (c == CURSOR_POS && cursorPos != null) {
                    if (cursorPos[0] >= 0) {
                        throw new IllegalArgumentException("Multiple cursors: "
                                                           + rules.substring(start, limit));
                    }
                    cursorPos[0] = text.length();
                } else if (specials.indexOf(c) >= 0) {
                    throw new IllegalArgumentException("Unquoted special character: "
                                                       + rules.substring(start, limit));
                } else {
                    text.append(c);
                }
            }
        }

        private static void validateVariableName(String name) {
            if (indexOf(name, SPECIALS) >= 0) {
                throw new IllegalArgumentException(
                              "Special character in variable name: "
                              + name);
            }
        }

        /**
         * Returns the single character value of the given variable name.  Defined
         * names are recognized.
         *
         * NO LONGER SUPPORTED:
         * If a Unicode category name is given, a standard character variable
         * in the range firstCategoryVariable to lastCategoryVariable is returned,
         * with value firstCategoryVariable + n, where n is the category
         * number.
         * @exception IllegalArgumentException if the name is unknown.
         */
        private Character getVariableDef(String name) {
            Character ch = (Character) data.variableNames.get(name);
//!         if (ch == null) {
//!             int id = UnicodeSet.getCategoryID(name);
//!             if (id >= 0) {
//!                 ch = new Character((char) (firstCategoryVariable + id));
//!                 data.variableNames.put(name, ch);
//!                 data.setVariables.put(ch, new UnicodeSet(id));
//!             }
//!         }
            if (ch == null) {
                throw new IllegalArgumentException("Undefined variable: "
                                                   + name);
            }
            return ch;
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
                if (c == QUOTE) {
                    while (++i < limit
                           && text.charAt(i) != QUOTE) {}
                } else if (setOfChars.indexOf(c) >= 0) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Returns the index of the first character in a set.  Unlike
         * String.indexOf(), this method searches not for a single character, but
         * for any character of the string <code>setOfChars</code>.
         * @param text text to be searched
         * @param start the beginning index, inclusive; <code>0 <= start
         * <= limit</code>.
         * @param limit the ending index, exclusive; <code>start <= limit
         * <= text.length()</code>.
         * @param setOfChars string with one or more distinct characters
         * @return Offset of the first character in <code>setOfChars</code>
         * found, or -1 if not found.
         * @see #quotedIndexOf
         */
        private static int indexOf(String text, int start, int limit,
                                   String setOfChars) {
            for (int i=start; i<limit; ++i) {
                if (setOfChars.indexOf(text.charAt(i)) >= 0) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * Returns the index of the first character in a set.  Unlike
         * String.indexOf(), this method searches not for a single character, but
         * for any character of the string <code>setOfChars</code>.
         * @param text text to be searched
         * @param setOfChars string with one or more distinct characters
         * @return Offset of the first character in <code>setOfChars</code>
         * found, or -1 if not found.
         * @see #quotedIndexOf
         */
        private static int indexOf(String text, String setOfChars) {
            return indexOf(text, 0, text.length(), setOfChars);
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
