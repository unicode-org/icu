/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/UnicodeSet.java,v $
 * $Date: 2002/11/08 01:19:58 $
 * $Revision: 1.75 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.text;

import java.text.*;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.*;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.UPropertyAliases;
import java.util.TreeSet;
import java.util.SortedSet;
import java.util.Iterator;

/**
 * A mutable set of Unicode characters and multicharacter strings.  Objects of this class
 * represent <em>character classes</em> used in regular expressions.
 * A character specifies a subset of Unicode code points.  Legal
 * code points are U+0000 to U+10FFFF, inclusive.
 *
 * <p><code>UnicodeSet</code> supports two APIs. The first is the
 * <em>operand</em> API that allows the caller to modify the value of
 * a <code>UnicodeSet</code> object. It conforms to Java 2's
 * <code>java.util.Set</code> interface, although
 * <code>UnicodeSet</code> does not actually implement that
 * interface. All methods of <code>Set</code> are supported, with the
 * modification that they take a character range or single character
 * instead of an <code>Object</code>, and they take a
 * <code>UnicodeSet</code> instead of a <code>Collection</code>.  The
 * operand API may be thought of in terms of boolean logic: a boolean
 * OR is implemented by <code>add</code>, a boolean AND is implemented
 * by <code>retain</code>, a boolean XOR is implemented by
 * <code>complement</code> taking an argument, and a boolean NOT is
 * implemented by <code>complement</code> with no argument.  In terms
 * of traditional set theory function names, <code>add</code> is a
 * union, <code>retain</code> is an intersection, <code>remove</code>
 * is an asymmetric difference, and <code>complement</code> with no
 * argument is a set complement with respect to the superset range
 * <code>MIN_VALUE-MAX_VALUE</code>
 *
 * <p>The second API is the
 * <code>applyPattern()</code>/<code>toPattern()</code> API from the
 * <code>java.text.Format</code>-derived classes.  Unlike the
 * methods that add characters, add categories, and control the logic
 * of the set, the method <code>applyPattern()</code> sets all
 * attributes of a <code>UnicodeSet</code> at once, based on a
 * string pattern.
 *
 * <p><b>Pattern syntax</b></p>
 *
 * Patterns are accepted by the constructors and the
 * <code>applyPattern()</code> methods and returned by the
 * <code>toPattern()</code> method.  These patterns follow a syntax
 * similar to that employed by version 8 regular expression character
 * classes:
 *
 * <blockquote>
 *   <table>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>pattern :=&nbsp; </code></td>
 *       <td valign="top"><code>('[' '^'? item* ']') |
 *       property</code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>item :=&nbsp; </code></td>
 *       <td valign="top"><code>char | (char '-' char) | pattern-expr<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>pattern-expr :=&nbsp; </code></td>
 *       <td valign="top"><code>pattern | pattern-expr pattern |
 *       pattern-expr op pattern<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>op :=&nbsp; </code></td>
 *       <td valign="top"><code>'&amp;' | '-'<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>special :=&nbsp; </code></td>
 *       <td valign="top"><code>'[' | ']' | '-'<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>char :=&nbsp; </code></td>
 *       <td valign="top"><em>any character that is not</em><code> special<br>
 *       | ('\u005C' </code><em>any character</em><code>)<br>
 *       | ('\u005Cu' hex hex hex hex)<br>
 *       </code></td>
 *     </tr>
 *     <tr align="top">
 *       <td nowrap valign="top" align="right"><code>hex :=&nbsp; </code></td>
 *       <td valign="top"><em>any character for which
 *       </em><code>Character.digit(c, 16)</code><em>
 *       returns a non-negative result</em></td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="right"><code>property :=&nbsp; </code></td>
 *       <td valign="top"><em>a Unicode property set pattern</td>
 *     </tr>
 *   </table>
 *   <br>
 *   <table border="1">
 *     <tr>
 *       <td>Legend: <table>
 *         <tr>
 *           <td nowrap valign="top"><code>a := b</code></td>
 *           <td width="20" valign="top">&nbsp; </td>
 *           <td valign="top"><code>a</code> may be replaced by <code>b</code> </td>
 *         </tr>
 *         <tr>
 *           <td nowrap valign="top"><code>a?</code></td>
 *           <td valign="top"></td>
 *           <td valign="top">zero or one instance of <code>a</code><br>
 *           </td>
 *         </tr>
 *         <tr>
 *           <td nowrap valign="top"><code>a*</code></td>
 *           <td valign="top"></td>
 *           <td valign="top">one or more instances of <code>a</code><br>
 *           </td>
 *         </tr>
 *         <tr>
 *           <td nowrap valign="top"><code>a | b</code></td>
 *           <td valign="top"></td>
 *           <td valign="top">either <code>a</code> or <code>b</code><br>
 *           </td>
 *         </tr>
 *         <tr>
 *           <td nowrap valign="top"><code>'a'</code></td>
 *           <td valign="top"></td>
 *           <td valign="top">the literal string between the quotes </td>
 *         </tr>
 *       </table>
 *       </td>
 *     </tr>
 *   </table>
 * </blockquote>
 *
 * Any character may be preceded by a backslash in order to remove any special
 * meaning.  White space characters, as defined by UCharacterProperty.isRuleWhiteSpace(), are
 * ignored, unless they are escaped.
 *
 * <p>Property patterns specify a set of characters having a certain
 * property as defined by the Unicode standard.  Both the POSIX-like
 * "[:Lu:]" and the Perl-like syntax "\p{Lu}" are recognized.  For a
 * complete list of supported property patterns, see the User's Guide
 * for UnicodeSet at
 * <a href="http://oss.software.ibm.com/icu/userguide/unicodeset.html">
 * http://oss.software.ibm.com/icu/userguide/unicodeset.html</a>.
 * Actual determination of property data is defined by the underlying
 * Unicode database as implemented by UCharacter.
 *
 * <p>Patterns specify individual characters, ranges of characters, and
 * Unicode property sets.  When elements are concatenated, they
 * specify their union.  To complement a set, place a '^' immediately
 * after the opening '['.  Property patterns are inverted by modifying
 * their delimiters; "[:^foo]" and "\P{foo}".  In any other location,
 * '^' has no special meaning.
 *
 * <p>Ranges are indicated by placing two a '-' between two
 * characters, as in "a-z".  This specifies the range of all
 * characters from the left to the right, in Unicode order.  If the
 * left character is greater than or equal to the
 * right character it is a syntax error.  If a '-' occurs as the first
 * character after the opening '[' or '[^', or if it occurs as the
 * last character before the closing ']', then it is taken as a
 * literal.  Thus "[a\u005C-b]", "[-ab]", and "[ab-]" all indicate the same
 * set of three characters, 'a', 'b', and '-'.
 *
 * <p>Sets may be intersected using the '&' operator or the asymmetric
 * set difference may be taken using the '-' operator, for example,
 * "[[:L:]&[\u005Cu0000-\u005Cu0FFF]]" indicates the set of all Unicode letters
 * with values less than 4096.  Operators ('&' and '|') have equal
 * precedence and bind left-to-right.  Thus
 * "[[:L:]-[a-z]-[\u005Cu0100-\u005Cu01FF]]" is equivalent to
 * "[[[:L:]-[a-z]]-[\u005Cu0100-\u005Cu01FF]]".  This only really matters for
 * difference; intersection is commutative.
 *
 * <table>
 * <tr valign=top><td nowrap><code>[a]</code><td>The set containing 'a'
 * <tr valign=top><td nowrap><code>[a-z]</code><td>The set containing 'a'
 * through 'z' and all letters in between, in Unicode order
 * <tr valign=top><td nowrap><code>[^a-z]</code><td>The set containing
 * all characters but 'a' through 'z',
 * that is, U+0000 through 'a'-1 and 'z'+1 through U+10FFFF
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>][<em>pat2</em>]]</code>
 * <td>The union of sets specified by <em>pat1</em> and <em>pat2</em>
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>]&[<em>pat2</em>]]</code>
 * <td>The intersection of sets specified by <em>pat1</em> and <em>pat2</em>
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>]-[<em>pat2</em>]]</code>
 * <td>The asymmetric difference of sets specified by <em>pat1</em> and
 * <em>pat2</em>
 * <tr valign=top><td nowrap><code>[:Lu:] or \p{Lu}</code>
 * <td>The set of characters having the specified
 * Unicode property; in
 * this case, Unicode uppercase letters
 * <tr valign=top><td nowrap><code>[:^Lu:] or \P{Lu}</code>
 * <td>The set of characters <em>not</em> having the given
 * Unicode property
 * </table>
 * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
 * @author Alan Liu
 * @version $RCSfile: UnicodeSet.java,v $ $Revision: 1.75 $ $Date: 2002/11/08 01:19:58 $
 */
public class UnicodeSet extends UnicodeFilter {

    private static final int LOW = 0x000000; // LOW <= all valid values. ZERO for codepoints
    private static final int HIGH = 0x110000; // HIGH > all valid values. 10000 for code units.
                                             // 110000 for codepoints

    /**
     * Minimum value that can be stored in a UnicodeSet.
     */
    public static final int MIN_VALUE = LOW;

    /**
     * Maximum value that can be stored in a UnicodeSet.
     */
    public static final int MAX_VALUE = HIGH - 1;

    private int len;      // length used; list may be longer to minimize reallocs
    private int[] list;   // MUST be terminated with HIGH
    private int[] rangeList; // internal buffer
    private int[] buffer; // internal buffer
    
    // NOTE: normally the field should be of type SortedSet; but that is missing a public clone!!
    // is not private so that UnicodeSetIterator can get access
    TreeSet strings = new TreeSet();

    /**
     * The pattern representation of this set.  This may not be the
     * most economical pattern.  It is the pattern supplied to
     * applyPattern(), with variables substituted and whitespace
     * removed.  For sets constructed without applyPattern(), or
     * modified using the non-pattern API, this string will be null,
     * indicating that toPattern() must generate a pattern
     * representation from the inversion list.
     */
    private String pat = null;

    private static final int START_EXTRA = 16;         // initial storage. Must be >= 0
    private static final int GROW_EXTRA = START_EXTRA; // extra amount for growth. Must be >= 0

    // Special property set IDs
    private static final String ANY_ID   = "ANY";   // [\u0000-\U0010FFFF]
    private static final String ASCII_ID = "ASCII"; // [\u0000-\u007F]

    /**
     * A set of all characters _except_ the second through last characters of
     * certain ranges.  These ranges are ranges of characters whose
     * properties are all exactly alike, e.g. CJK Ideographs from
     * U+4E00 to U+9FA5.
     *
     * TODO: Replace this with an API call when such API is implemented.
     */
    private static final UnicodeSet INCLUSIONS =
        new UnicodeSet("[^\\u3401-\\u4DB5 \\u4E01-\\u9FA5 \\uAC01-\\uD7A3 \\uD801-\\uDB7F \\uDB81-\\uDBFF \\uDC01-\\uDFFF \\uE001-\\uF8FF \\U0001044F-\\U0001CFFF \\U0001D801-\\U0001FFFF \\U00020001-\\U0002A6D6 \\U0002A6D8-\\U0002F7FF \\U0002FA1F-\\U000E0000 \\U000E0081-\\U000EFFFF \\U000F0001-\\U000FFFFD \\U00100001-\\U0010FFFD]");

    //----------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------

    /**
     * Constructs an empty set.
     */
    public UnicodeSet() {
        list = new int[1 + START_EXTRA];
        list[len++] = HIGH;
    }

    /**
     * Constructs a copy of an existing set.
     */
    public UnicodeSet(UnicodeSet other) {
        set(other);
    }

    /**
     * Constructs a set containing the given range. If <code>end >
     * start</code> then an empty set is created.
     *
     * @param start first character, inclusive, of range
     * @param end last character, inclusive, of range
     */
    public UnicodeSet(int start, int end) {
        this();
        complement(start, end);
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.  Whitespace is ignored.
     * @param pattern a string specifying what characters are in the set
     * @exception java.lang.IllegalArgumentException if the pattern contains
     * a syntax error.
     */
    public UnicodeSet(String pattern) {
        this(pattern, true);
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @param ignoreWhitespace if true, ignore characters for which
     * UCharacterProperty.isRuleWhiteSpace() returns true
     * @exception java.lang.IllegalArgumentException if the pattern contains
     * a syntax error.
     */
    public UnicodeSet(String pattern, boolean ignoreWhitespace) {
        this();
        applyPattern(pattern, ignoreWhitespace);
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @param pos on input, the position in pattern at which to start parsing.
     * On output, the position after the last character parsed.
     * @param symbols a symbol table mapping variables to char[] arrays
     * and chars to UnicodeSets
     * @exception java.lang.IllegalArgumentException if the pattern
     * contains a syntax error.
     */
    public UnicodeSet(String pattern, ParsePosition pos, SymbolTable symbols) {
        this();
        applyPattern(pattern, pos, symbols, true);
    }

    // Delete the following when the category constructor is removed
    private static final String CATEGORY_NAMES =
        //                    1 1 1 1 1 1 1   1 1 2 2 2 2 2 2 2 2 2
        //0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6   8 9 0 1 2 3 4 5 6 7 8
        "CnLuLlLtLmLoMnMeMcNdNlNoZsZlZpCcCf--CoCsPdPsPePcPoSmScSkSo";
    /**
     * DEPRECATED - Constructs a set from the given Unicode character
     * category.
     * @param category an integer indicating the character category as
     * returned by <code>java.lang.Character.getType()</code>.  Note
     * that this is <em>different</em> from the UCharacterCategory
     * codes.
     * @exception java.lang.IllegalArgumentException if the given
     * category is invalid.
     * @deprecated this will be removed Dec-31-2002
     */
    public UnicodeSet(int category) {
        if (category < 0 || category > java.lang.Character.OTHER_SYMBOL ||
            category == 17) {
            throw new IllegalArgumentException("Invalid category");
        }
        String pat = "[:" + CATEGORY_NAMES.substring(2*category, 2*category+2) + ":]";
        applyPattern(pat, false);
    }

    /**
     * Return a new set that is equivalent to this one.
     */
    public Object clone() {
        return new UnicodeSet(this);
    }

    /**
     * Make this object represent the range <code>start - end</code>.
     * If <code>end > start</code> then this object is set to an
     * an empty range.
     *
     * @param start first character in the set, inclusive
     * @param end last character in the set, inclusive
     */
    public UnicodeSet set(int start, int end) {
        clear();
        complement(start, end);
        return this;
    }

    /**
     * Make this object represent the same set as <code>other</code>.
     * @param other a <code>UnicodeSet</code> whose value will be
     * copied to this object
     */
    public UnicodeSet set(UnicodeSet other) {
        list = (int[]) other.list.clone();
        len = other.len;
        pat = other.pat;
        strings = (TreeSet)other.strings.clone();
        return this;
    }

    /**
     * Modifies this set to represent the set specified by the given pattern.
     * See the class description for the syntax of the pattern language.
     * Whitespace is ignored.
     * @param pattern a string specifying what characters are in the set
     * @exception java.lang.IllegalArgumentException if the pattern
     * contains a syntax error.
     */
    public final UnicodeSet applyPattern(String pattern) {
        return applyPattern(pattern, true);
    }

    /**
     * Modifies this set to represent the set specified by the given pattern,
     * optionally ignoring whitespace.
     * See the class description for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @param ignoreWhitespace if true then characters for which
     * UCharacterProperty.isRuleWhiteSpace() returns true are ignored
     * @exception java.lang.IllegalArgumentException if the pattern
     * contains a syntax error.
     */
    public UnicodeSet applyPattern(String pattern, boolean ignoreWhitespace) {
        ParsePosition pos = new ParsePosition(0);
        applyPattern(pattern, pos, null, ignoreWhitespace);

        int i = pos.getIndex();

        // Skip over trailing whitespace
        if (ignoreWhitespace) {
            i = Utility.skipWhitespace(pattern, i);
        }

        if (i != pattern.length()) {
            throw new IllegalArgumentException("Parse of \"" + pattern +
                                               "\" failed at " + i);
        }
        return this;
    }

    /**
     * Return true if the given position, in the given pattern, appears
     * to be the start of a UnicodeSet pattern.
     */
    public static boolean resemblesPattern(String pattern, int pos) {
        return ((pos+1) < pattern.length() &&
                pattern.charAt(pos) == '[') ||
            resemblesPropertyPattern(pattern, pos);
    }

    /**
     * Append the <code>toPattern()</code> representation of a
     * string to the given <code>StringBuffer</code>.
     */
    private static void _appendToPat(StringBuffer buf, String s, boolean useHexEscape) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            _appendToPat(buf, cp = UTF16.charAt(s, i), useHexEscape);
        }
    }

    /**
     * Append the <code>toPattern()</code> representation of a
     * character to the given <code>StringBuffer</code>.
     */
    private static void _appendToPat(StringBuffer buf, int c, boolean useHexEscape) {
        if (useHexEscape) {
            // Use hex escape notation (<backslash>uxxxx or <backslash>Uxxxxxxxx) for anything
            // unprintable
            if (Utility.escapeUnprintable(buf, c)) {
                return;
            }
        }
        // Okay to let ':' pass through
        switch (c) {
        case '[': // SET_OPEN:
        case ']': // SET_CLOSE:
        case '-': // HYPHEN:
        case '^': // COMPLEMENT:
        case '&': // INTERSECTION:
        case '\\': //BACKSLASH:
        case '{':
        case '}':
        case '$':
        case ':':
            buf.append('\\');
            break;
        default:
            // Escape whitespace
            if (UCharacterProperty.isRuleWhiteSpace(c)) {
                buf.append('\\');
            }
            break;
        }
        UTF16.append(buf, c);
    }

    /**
     * Returns a string representation of this set.  If the result of
     * calling this function is passed to a UnicodeSet constructor, it
     * will produce another set that is equal to this one.
     */
    public String toPattern(boolean escapeUnprintable) {
        StringBuffer result = new StringBuffer();
        return _toPattern(result, escapeUnprintable).toString();
    }

    /**
     * Append a string representation of this set to result.  This will be
     * a cleaned version of the string passed to applyPattern(), if there
     * is one.  Otherwise it will be generated.
     */
    private StringBuffer _toPattern(StringBuffer result,
                                    boolean escapeUnprintable) {
        if (pat != null) {
            int i;
            int backslashCount = 0;
            for (i=0; i<pat.length(); ) {
                int c = UTF16.charAt(pat, i);
                i += UTF16.getCharCount(c);
                if (escapeUnprintable && Utility.isUnprintable(c)) {
                    // If the unprintable character is preceded by an odd
                    // number of backslashes, then it has been escaped.
                    // Before unescaping it, we delete the final
                    // backslash.
                    if ((backslashCount % 2) == 1) {
                        result.setLength(result.length() - 1);
                    }
                    Utility.escapeUnprintable(result, c);
                    backslashCount = 0;
                } else {
                    UTF16.append(result, c);
                    if (c == '\\') {
                        ++backslashCount;
                    } else {
                        backslashCount = 0;
                    }
                }
            }
            return result;
        }

        return _generatePattern(result, escapeUnprintable);
    }

    /**
     * Generate and append a string representation of this set to result.
     * This does not use this.pat, the cleaned up copy of the string
     * passed to applyPattern().
     */
    public StringBuffer _generatePattern(StringBuffer result,
                                         boolean escapeUnprintable) {
        result.append('[');

//      // Check against the predefined categories.  We implicitly build
//      // up ALL category sets the first time toPattern() is called.
//      for (int cat=0; cat<CATEGORY_COUNT; ++cat) {
//          if (this.equals(getCategorySet(cat))) {
//              result.append(':');
//              result.append(CATEGORY_NAMES.substring(cat*2, cat*2+2));
//              return result.append(":]");
//          }
//      }

        int count = getRangeCount();

        // If the set contains at least 2 intervals and includes both
        // MIN_VALUE and MAX_VALUE, then the inverse representation will
        // be more economical.
        if (count > 1 &&
            getRangeStart(0) == MIN_VALUE &&
            getRangeEnd(count-1) == MAX_VALUE) {

            // Emit the inverse
            result.append('^');

            for (int i = 1; i < count; ++i) {
                int start = getRangeEnd(i-1)+1;
                int end = getRangeStart(i)-1;
                _appendToPat(result, start, escapeUnprintable);
                if (start != end) {
                    result.append('-');
                    _appendToPat(result, end, escapeUnprintable);
                }
            }
        }

        // Default; emit the ranges as pairs
        else {
            for (int i = 0; i < count; ++i) {
                int start = getRangeStart(i);
                int end = getRangeEnd(i);
                _appendToPat(result, start, escapeUnprintable);
                if (start != end) {
                    result.append('-');
                    _appendToPat(result, end, escapeUnprintable);
                }
            }
        }

        if (strings.size() > 0) {
            Iterator it = strings.iterator();
            while (it.hasNext()) {
                result.append('{');
                _appendToPat(result, (String) it.next(), escapeUnprintable);
                result.append('}');
            }
        }
        return result.append(']');
    }

    /**
     * Returns the number of elements in this set (its cardinality),
     * <em>n</em>, where <code>0 <= </code><em>n</em><code> <= 65536</code>.
     *
     * @return the number of elements in this set (its cardinality).
     */
    public int size() {
        int n = 0;
        int count = getRangeCount();
        for (int i = 0; i < count; ++i) {
            n += getRangeEnd(i) - getRangeStart(i) + 1;
        }
        return n + strings.size();
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     *
     * @return <tt>true</tt> if this set contains no elements.
     */
    public boolean isEmpty() {
        return len == 1 && strings.size() == 0;
    }

    /**
     * Implementation of UnicodeMatcher API.  Returns <tt>true</tt> if
     * this set contains any character whose low byte is the given
     * value.  This is used by <tt>RuleBasedTransliterator</tt> for
     * indexing.
     */
    public boolean matchesIndexValue(int v) {
        /* The index value v, in the range [0,255], is contained in this set if
         * it is contained in any pair of this set.  Pairs either have the high
         * bytes equal, or unequal.  If the high bytes are equal, then we have
         * aaxx..aayy, where aa is the high byte.  Then v is contained if xx <=
         * v <= yy.  If the high bytes are unequal we have aaxx..bbyy, bb>aa.
         * Then v is contained if xx <= v || v <= yy.  (This is identical to the
         * time zone month containment logic.)
         */
        for (int i=0; i<getRangeCount(); ++i) {
            int low = getRangeStart(i);
            int high = getRangeEnd(i);
            if ((low & ~0xFF) == (high & ~0xFF)) {
                if ((low & 0xFF) <= v && v <= (high & 0xFF)) {
                    return true;
                }
            } else if ((low & 0xFF) <= v || v <= (high & 0xFF)) {
                return true;
            }
        }
        if (strings.size() != 0) {
            Iterator it = strings.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                //if (s.length() == 0) {
                //    // Empty strings match everything
                //    return true;
                //}
                // assert(s.length() != 0); // We enforce this elsewhere
                int c = UTF16.charAt(s, 0);
                if ((c & 0xFF) == v) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Implementation of UnicodeMatcher.matches().  Always matches the
     * longest possible multichar string.
     */
    public int matches(Replaceable text,
                       int[] offset,
                       int limit,
                       boolean incremental) {
        
        if (offset[0] == limit) {
            // Strings, if any, have length != 0, so we don't worry
            // about them here.  If we ever allow zero-length strings
            // we much check for them here.
            if (contains(TransliterationRule.ETHER)) {
                return incremental ? U_PARTIAL_MATCH : U_MATCH;
            } else {
                return U_MISMATCH;
            }
        } else {
            if (strings.size() != 0) { // try strings first
            
                // might separate forward and backward loops later
                // for now they are combined

                // TODO Improve efficiency of this, at least in the forward
                // direction, if not in both.  In the forward direction we
                // can assume the strings are sorted.
            
                Iterator it = strings.iterator();
                boolean forward = offset[0] < limit;

                // firstChar is the leftmost char to match in the
                // forward direction or the rightmost char to match in
                // the reverse direction.
                char firstChar = text.charAt(offset[0]);

                // If there are multiple strings that can match we
                // return the longest match.
                int highWaterLength = 0;

                while (it.hasNext()) {
                    String trial = (String) it.next();

                    //if (trial.length() == 0) {
                    //    return U_MATCH; // null-string always matches
                    //}
                    // assert(trial.length() != 0); // We ensure this elsewhere

                    char c = trial.charAt(forward ? 0 : trial.length() - 1);

                    // Strings are sorted, so we can optimize in the
                    // forward direction.
                    if (forward && c > firstChar) break;
                    if (c != firstChar) continue;
                        
                    int len = matchRest(text, offset[0], limit, trial);

                    if (incremental) {
                        int maxLen = forward ? limit-offset[0] : offset[0]-limit;
                        if (len == maxLen) {
                            // We have successfully matched but only up to limit.
                            return U_PARTIAL_MATCH;
                        }
                    }

                    if (len == trial.length()) {
                        // We have successfully matched the whole string.
                        if (len > highWaterLength) {
                            highWaterLength = len;
                        }
                        // In the forward direction we know strings
                        // are sorted so we can bail early.
                        if (forward && len < highWaterLength) {
                            break;
                        }
                        continue;
                    }
                }

                // We've checked all strings without a partial match.
                // If we have full matches, return the longest one.
                if (highWaterLength != 0) {
                    offset[0] += forward ? highWaterLength : -highWaterLength;
                    return U_MATCH;
                }
            }
            return super.matches(text, offset, limit, incremental);
        }
    }
    
    /**
     * Returns the longest match for s in text at the given position.
     * If limit > start then match forward from start+1 to limit
     * matching all characters except s.charAt(0).  If limit < start,
     * go backward starting from start-1 matching all characters
     * except s.charAt(s.length()-1).  This method assumes that the
     * first character, text.charAt(start), matches s, so it does not
     * check it.
     * @param text the text to match
     * @param start the first character to match.  In the forward
     * direction, text.charAt(start) is matched against s.charAt(0).
     * In the reverse direction, it is matched against
     * s.charAt(s.length()-1).
     * @param limit the limit offset for matching, either last+1 in
     * the forward direction, or last-1 in the reverse direction,
     * where last is the index of the last character to match.
     * @return If part of s matches up to the limit, return |limit -
     * start|.  If all of s matches before reaching the limit, return
     * s.length().  If there is a mismatch between s and text, return
     * 0
     */
    private static int matchRest (Replaceable text, int start, int limit, String s) {
        int maxLen;
        int slen = s.length();
        if (start < limit) {
            maxLen = limit - start;
            if (maxLen > slen) maxLen = slen;
            for (int i = 1; i < maxLen; ++i) {
                if (text.charAt(start + i) != s.charAt(i)) return 0;
            }
        } else {
            maxLen = start - limit;
            if (maxLen > slen) maxLen = slen;
            --slen; // <=> slen = s.length() - 1;
            for (int i = 1; i < maxLen; ++i) {
                if (text.charAt(start - i) != s.charAt(slen - i)) return 0;
            }
        }
        return maxLen;
    }
        

    /**
     * Implementation of UnicodeMatcher API.  Union the set of all
     * characters that may be matched by this object into the given
     * set.
     * @param toUnionTo the set into which to union the source characters
     */
    public void addMatchSetTo(UnicodeSet toUnionTo) {
        toUnionTo.addAll(this);
    }

    /**
     * Returns the index of the given character within this set, where
     * the set is ordered by ascending code point.  If the character
     * is not in this set, return -1.  The inverse of this method is
     * <code>charAt()</code>.
     * @return an index from 0..size()-1, or -1
     */
    public int indexOf(int c) {
        if (c < MIN_VALUE || c > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(c, 6));
        }
        int i = 0;
        int n = 0;
        for (;;) {
            int start = list[i++];
            if (c < start) {
                return -1;
            }
            int limit = list[i++];
            if (c < limit) {
                return n + c - start;
            }
            n += limit - start;
        }
    }

    /**
     * Returns the character at the given index within this set, where
     * the set is ordered by ascending code point.  If the index is
     * out of range, return -1.  The inverse of this method is
     * <code>indexOf()</code>.
     * @param index an index from 0..size()-1
     * @return the character at the given index, or -1.
     */
    public int charAt(int index) {
        if (index >= 0) {
            // len2 is the largest even integer <= len, that is, it is len
            // for even values and len-1 for odd values.  With odd values
            // the last entry is UNICODESET_HIGH.
            int len2 = len & ~1;
            for (int i=0; i < len2;) {
                int start = list[i++];
                int count = list[i++] - start;
                if (index < count) {
                    return start + index;
                }
                index -= count;
            }
        }
        return -1;
    }

    /**
     * Adds the specified range to this set if it is not already
     * present.  If this set already contains the specified range,
     * the call leaves this set unchanged.  If <code>end > start</code>
     * then an empty range is added, leaving the set unchanged.
     *
     * @param start first character, inclusive, of range to be added
     * to this set.
     * @param end last character, inclusive, of range to be added
     * to this set.
     */
    public UnicodeSet add(int start, int end) {
        if (start < MIN_VALUE || start > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < MIN_VALUE || end > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start < end) {
            add(range(start, end), 2, 0);
        } else if (start == end) {
            add(start);
        }
        return this;
    }

//    /**
//     * Format out the inversion list as a string, for debugging.  Uncomment when
//     * needed.
//     */
//    public final String dump() {
//        StringBuffer buf = new StringBuffer("[");
//        for (int i=0; i<len; ++i) {
//            if (i != 0) buf.append(", ");
//            int c = list[i];
//            //if (c <= 0x7F && c != '\n' && c != '\r' && c != '\t' && c != ' ') {
//            //    buf.append((char) c);
//            //} else {
//                buf.append("U+").append(Utility.hex(c, (c<0x10000)?4:6));
//            //}
//        }
//        buf.append("]");
//        return buf.toString();
//    }

    /**
     * Adds the specified character to this set if it is not already
     * present.  If this set already contains the specified character,
     * the call leaves this set unchanged.
     */
    public final UnicodeSet add(int c) {
        if (c < MIN_VALUE || c > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(c, 6));
        }

        // find smallest i such that c < list[i]
        // if odd, then it is IN the set
        // if even, then it is OUT of the set
        int i = findCodePoint(c);

        // already in set?
        if ((i & 1) != 0) return this;

        // HIGH is 0x110000
        // assert(list[len-1] == HIGH);
        
        // empty = [HIGH]
        // [start_0, limit_0, start_1, limit_1, HIGH]

        // [..., start_i-1, limit_i-1, start_i, limit_i, ..., HIGH]
        //                             ^
        //                             list[i]

        // i == 0 means c is before the first range

        if (c == list[i]-1) {
            // c is before start of next range
            list[i] = c;
            // if we touched the HIGH mark, then add a new one
            if (c == MAX_VALUE) {
                ensureCapacity(len+1);
                list[len++] = HIGH;
            }
            if (i > 0 && c == list[i-1]) {
                // collapse adjacent ranges

                // [..., start_i-1, c, c, limit_i, ..., HIGH]
                //                     ^
                //                     list[i]
                System.arraycopy(list, i+1, list, i-1, len-i-1);
                len -= 2;
            }
        }

        else if (i > 0 && c == list[i-1]) {
            // c is after end of prior range
            list[i-1]++;
            // no need to chcek for collapse here
        }

        else {
            // At this point we know the new char is not adjacent to
            // any existing ranges, and it is not 10FFFF.


            // [..., start_i-1, limit_i-1, start_i, limit_i, ..., HIGH]
            //                             ^
            //                             list[i]

            // [..., start_i-1, limit_i-1, c, c+1, start_i, limit_i, ..., HIGH]
            //                             ^
            //                             list[i]

            // Don't use ensureCapacity() to save on copying.
            // NOTE: This has no measurable impact on performance,
            // but it might help in some usage patterns.
            if (len+2 > list.length) {
                int[] temp = new int[len + 2 + GROW_EXTRA];
                if (i != 0) System.arraycopy(list, 0, temp, 0, i);
                System.arraycopy(list, i, temp, i+2, len-i);
                list = temp;
            } else {
                System.arraycopy(list, i, list, i+2, len-i);
            }

            list[i] = c;
            list[i+1] = c+1;
            len += 2;
        }
        
        pat = null;
        return this;
    }

    /**
     * Adds the specified multicharacter to this set if it is not already
     * present.  If this set already contains the multicharacter,
     * the call leaves this set unchanged.
     * Thus "ch" => {"ch"}
	 * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
     * @param s the source string
     * @return this object, for chaining
     */
    public final UnicodeSet add(String s) {
        
        int cp = getSingleCP(s);
        if (cp < 0) {
        	strings.add(s);
        	pat = null;
		} else {
			add(cp, cp);
		}
        return this;
    }
    
    /**
     * @return a code point IF the string consists of a single one.
     * otherwise returns -1.
     * @param string to test
     */
    private static int getSingleCP(String s) {
        if (s.length() < 1) {
        	throw new IllegalArgumentException("Can't use zero-length strings in UnicodeSet");
        }
    	if (s.length() > 2) return -1;
    	if (s.length() == 1) return s.charAt(0);
    	
    	// at this point, len = 2
        int cp = UTF16.charAt(s, 0);
        if (cp > 0xFFFF) { // is surrogate pair
        	return cp;
        }
        return -1;
    }
    
    /**
     * Adds each of the characters in this string to the set. Thus "ch" => {"c", "h"}
     * If this set already any particular character, it has no effect on that character.
     * @param s the source string
     * @return this object, for chaining
     */
    public final UnicodeSet addAll(String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            add(cp, cp);
        }
        return this;
    }

    /**
     * Retains EACH of the characters in this string. Note: "ch" == {"c", "h"}
     * If this set already any particular character, it has no effect on that character.
     * @param s the source string
     * @return this object, for chaining
     */
    public final UnicodeSet retainAll(String s) {
    	return retainAll(fromAll(s));
    }

    /**
     * Complement EACH of the characters in this string. Note: "ch" == {"c", "h"}
     * If this set already any particular character, it has no effect on that character.
     * @param s the source string
     * @return this object, for chaining
     */
    public final UnicodeSet complementAll(String s) {
    	return complementAll(fromAll(s));
    }

    /**
     * Remove EACH of the characters in this string. Note: "ch" == {"c", "h"}
     * If this set already any particular character, it has no effect on that character.
     * @param s the source string
     * @return this object, for chaining
     */
    public final UnicodeSet removeAll(String s) {
    	return removeAll(fromAll(s));
    }

    /**
     * Makes a set from a multicharacter string. Thus "ch" => {"ch"}
	 * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
     * @param s the source string
     * @return a newly created set containing the given string
     */
    public static UnicodeSet from(String s) {
        return new UnicodeSet().add(s);
    }

    
    /**
     * Makes a set from each of the characters in the string. Thus "ch" => {"c", "h"}
     * @param s the source string
     * @return a newly created set containing the given characters
     */
    public static UnicodeSet fromAll(String s) {
        return new UnicodeSet().addAll(s);
    }


    /**
     * Retain only the elements in this set that are contained in the
     * specified range.  If <code>end > start</code> then an empty range is
     * retained, leaving the set empty.
     *
     * @param start first character, inclusive, of range to be retained
     * to this set.
     * @param end last character, inclusive, of range to be retained
     * to this set.
     */
    public UnicodeSet retain(int start, int end) {
        if (start < MIN_VALUE || start > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < MIN_VALUE || end > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start <= end) {
            retain(range(start, end), 2, 0);
        } else {
            clear();
        }
        return this;
    }

    /**
     * Retain the specified character from this set if it is present.
     */
    public final UnicodeSet retain(int c) {
        return retain(c, c);
    }
    
    /**
     * Retain the specified string in this set if it is present.
     * The set will not contain the specified character once the call
     * returns.
     * @param s the source string
     * @return this object, for chaining
     */
    public final UnicodeSet retain(String s) {
        int cp = getSingleCP(s);
        if (cp < 0) {
        	if (strings.size() == 1 && strings.contains(s)) return this;
        	strings.clear();
        	strings.add(s);
        	pat = null;
		} else {
			retain(cp, cp);
		}
		return this;
    }

    /**
     * Removes the specified range from this set if it is present.
     * The set will not contain the specified range once the call
     * returns.  If <code>end > start</code> then an empty range is
     * removed, leaving the set unchanged.
     *
     * @param start first character, inclusive, of range to be removed
     * from this set.
     * @param end last character, inclusive, of range to be removed
     * from this set.
     */
    public UnicodeSet remove(int start, int end) {
        if (start < MIN_VALUE || start > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < MIN_VALUE || end > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start <= end) {
            retain(range(start, end), 2, 2);
        }
        return this;
    }

    /**
     * Removes the specified character from this set if it is present.
     * The set will not contain the specified character once the call
     * returns.
     */
    public final UnicodeSet remove(int c) {
        return remove(c, c);
    }

    /**
     * Removes the specified string from this set if it is present.
     * The set will not contain the specified character once the call
     * returns.
     * @param s the source string
     * @return this object, for chaining
     */
    public final UnicodeSet remove(String s) {
        int cp = getSingleCP(s);
        if (cp < 0) {
        	strings.remove(s);
        	pat = null;
		} else {
			remove(cp, cp);
		}
		return this;
    }

    /**
     * Complements the specified range in this set.  Any character in
     * the range will be removed if it is in this set, or will be
     * added if it is not in this set.  If <code>end > start</code>
     * then an empty range is complemented, leaving the set unchanged.
     *
     * @param start first character, inclusive, of range to be removed
     * from this set.
     * @param end last character, inclusive, of range to be removed
     * from this set.
     */
    public UnicodeSet complement(int start, int end) {
        if (start < MIN_VALUE || start > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < MIN_VALUE || end > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        if (start <= end) {
            xor(range(start, end), 2, 0);
        }
        pat = null;
        return this;
    }

    /**
     * Complements the specified character in this set.  The character
     * will be removed if it is in this set, or will be added if it is
     * not in this set.
     */
    public final UnicodeSet complement(int c) {
        return complement(c, c);
    }

    /**
     * This is equivalent to
     * <code>complement(MIN_VALUE, MAX_VALUE)</code>.
     */
    public UnicodeSet complement() {
        if (list[0] == LOW) {
            System.arraycopy(list, 1, list, 0, len-1);
            --len;
        } else {
            ensureCapacity(len+1);
            System.arraycopy(list, 0, list, 1, len);
            list[0] = LOW;
            ++len;
        }
        pat = null;
        return this;
    }
    
    /**
     * Complement the specified string in this set.
     * The set will not contain the specified string once the call
     * returns.
	 * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
     * @param s the string to complement
     * @return this object, for chaining
     */
    public final UnicodeSet complement(String s) {
        int cp = getSingleCP(s);
        if (cp < 0) {
        	if (strings.contains(s)) strings.remove(s);
        	else strings.add(s);
        	pat = null;
		} else {
			complement(cp, cp);
		}
		return this;
    }    

    /**
     * Returns true if this set contains the given character.
     * @param c character to be checked for containment
     * @return true if the test condition is met
     */
    public boolean contains(int c) {
        if (c < MIN_VALUE || c > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(c, 6));
        }

        /*
        // Set i to the index of the start item greater than ch
        // We know we will terminate without length test!
        int i = -1;
        while (true) {
            if (c < list[++i]) break;
        }
        */

        int i = findCodePoint(c);

        return ((i & 1) != 0); // return true if odd
    }

    /**
     * Returns the smallest value i such that c < list[i].  Caller
     * must ensure that c is a legal value or this method will enter
     * an infinite loop.  This method performs a binary search.
     * @param c a character in the range MIN_VALUE..MAX_VALUE
     * inclusive
     * @return the smallest integer i in the range 0..len-1,
     * inclusive, such that c < list[i]
     */
    private final int findCodePoint(int c) {
        // Return the smallest i such that c < list[i].  Assume
        // list[len - 1] == HIGH and that c is legal (0..HIGH-1).
        if (c < list[0]) return 0;
        // High runner test.  c is often after the last range, so an
        // initial check for this condition pays off. 
        if (len >= 2 && c >= list[len-2]) return len-1;
        int lo = 0;
        int hi = len - 1;
        // invariant: c >= list[lo]
        // invariant: c < list[hi]
        for (;;) {
            int i = (lo + hi) >>> 1;
            if (i == lo) return hi;
            if (c < list[i]) {
                hi = i;
            } else {
                lo = i;
            }
        }
    }

//    //----------------------------------------------------------------
//    // Unrolled binary search
//    //----------------------------------------------------------------
//
//    private int validLen = -1; // validated value of len
//    private int topOfLow;
//    private int topOfHigh;
//    private int power;
//    private int deltaStart;
//
//    private void validate() {
//        if (len <= 1) {
//            throw new IllegalArgumentException("list.len==" + len + "; must be >1");
//        }
//
//        // find greatest power of 2 less than or equal to len
//        for (power = exp2.length-1; power > 0 && exp2[power] > len; power--) {}
//
//        // assert(exp2[power] <= len);
//
//        // determine the starting points
//        topOfLow = exp2[power] - 1;
//        topOfHigh = len - 1;
//        deltaStart = exp2[power-1];
//        validLen = len;
//    }
//
//    private static final int exp2[] = {
//        0x1, 0x2, 0x4, 0x8,
//        0x10, 0x20, 0x40, 0x80,
//        0x100, 0x200, 0x400, 0x800,
//        0x1000, 0x2000, 0x4000, 0x8000,
//        0x10000, 0x20000, 0x40000, 0x80000,
//        0x100000, 0x200000, 0x400000, 0x800000,
//        0x1000000, 0x2000000, 0x4000000, 0x8000000,
//        0x10000000, 0x20000000 // , 0x40000000 // no unsigned int in Java
//    };
//
//    /**
//     * Unrolled lowest index GT.
//     */
//    private final int leastIndexGT(int searchValue) {
//
//        if (len != validLen) {
//            if (len == 1) return 0;
//            validate();
//        }
//        int temp;
//        
//        // set up initial range to search. Each subrange is a power of two in length
//        int high = searchValue < list[topOfLow] ? topOfLow : topOfHigh;
//
//        // Completely unrolled binary search, folhighing "Programming Pearls"
//        // Each case deliberately falls through to the next
//        // Logically, list[-1] < all_search_values && list[count] > all_search_values
//        // although the values -1 and count are never actually touched.
//        
//        // The bounds at each point are low & high,
//        // where low == high - delta*2
//        // so high - delta is the midpoint
//        
//        // The invariant AFTER each line is that list[low] < searchValue <= list[high]
//        
//        switch (power) {
//        //case 31: if (searchValue < list[temp = high-0x40000000]) high = temp; // no unsigned int in Java
//        case 30: if (searchValue < list[temp = high-0x20000000]) high = temp;
//        case 29: if (searchValue < list[temp = high-0x10000000]) high = temp;
//        
//        case 28: if (searchValue < list[temp = high- 0x8000000]) high = temp;
//        case 27: if (searchValue < list[temp = high- 0x4000000]) high = temp;
//        case 26: if (searchValue < list[temp = high- 0x2000000]) high = temp;
//        case 25: if (searchValue < list[temp = high- 0x1000000]) high = temp;
//        
//        case 24: if (searchValue < list[temp = high-  0x800000]) high = temp;
//        case 23: if (searchValue < list[temp = high-  0x400000]) high = temp;
//        case 22: if (searchValue < list[temp = high-  0x200000]) high = temp;
//        case 21: if (searchValue < list[temp = high-  0x100000]) high = temp;
//        
//        case 20: if (searchValue < list[temp = high-   0x80000]) high = temp;
//        case 19: if (searchValue < list[temp = high-   0x40000]) high = temp;
//        case 18: if (searchValue < list[temp = high-   0x20000]) high = temp;
//        case 17: if (searchValue < list[temp = high-   0x10000]) high = temp;
//        
//        case 16: if (searchValue < list[temp = high-    0x8000]) high = temp;
//        case 15: if (searchValue < list[temp = high-    0x4000]) high = temp;
//        case 14: if (searchValue < list[temp = high-    0x2000]) high = temp;
//        case 13: if (searchValue < list[temp = high-    0x1000]) high = temp;
//        
//        case 12: if (searchValue < list[temp = high-     0x800]) high = temp;
//        case 11: if (searchValue < list[temp = high-     0x400]) high = temp;
//        case 10: if (searchValue < list[temp = high-     0x200]) high = temp;
//        case  9: if (searchValue < list[temp = high-     0x100]) high = temp;
//        
//        case  8: if (searchValue < list[temp = high-      0x80]) high = temp;
//        case  7: if (searchValue < list[temp = high-      0x40]) high = temp;
//        case  6: if (searchValue < list[temp = high-      0x20]) high = temp;
//        case  5: if (searchValue < list[temp = high-      0x10]) high = temp;
//        
//        case  4: if (searchValue < list[temp = high-       0x8]) high = temp;
//        case  3: if (searchValue < list[temp = high-       0x4]) high = temp;
//        case  2: if (searchValue < list[temp = high-       0x2]) high = temp;
//        case  1: if (searchValue < list[temp = high-       0x1]) high = temp;
//        }
//
//        return high;
//    }
//
//    // For debugging only
//    public int len() {
//        return len;
//    }
//
//    //----------------------------------------------------------------
//    //----------------------------------------------------------------

    /**
     * Returns true if this set contains every character
     * of the given range.
     * @param start first character, inclusive, of the range
     * @param end last character, inclusive, of the range
     * @return true if the test condition is met
     */
    public boolean contains(int start, int end) {
        if (start < MIN_VALUE || start > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < MIN_VALUE || end > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        //int i = -1;
        //while (true) {
        //    if (start < list[++i]) break;
        //}
        int i = findCodePoint(start);
        return ((i & 1) != 0 && end < list[i]);
    }

    /**
     * Returns <tt>true</tt> if this set contains the given
     * multicharacter string.
     * @param s string to be checked for containment
     * @return <tt>true</tt> if this set contains the specified string
     */
    public final boolean contains(String s) {
        
        int cp = getSingleCP(s);
        if (cp < 0) {
        	return strings.contains(s);
		} else {
			return contains(cp);
		}
    }
    
    /**
     * Returns true if this set contains all the characters and strings
     * of the given set.
     * @param c set to be checked for containment
     * @return true if the test condition is met
     */
    public boolean containsAll(UnicodeSet c) {
        // The specified set is a subset if all of its pairs are contained in
        // this set.  It's possible to code this more efficiently in terms of
        // direct manipulation of the inversion lists if the need arises.
        int n = c.getRangeCount();
        for (int i=0; i<n; ++i) {
            if (!contains(c.getRangeStart(i), c.getRangeEnd(i))) {
                return false;
            }
        }
        if (!strings.containsAll(c.strings)) return false;
        return true;
    }
    
    /**
     * Returns true if this set contains all the characters
     * of the given string.
     * @param s string containing characters to be checked for containment
     * @return true if the test condition is met
     */
    public boolean containsAll(String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (!contains(cp)) return false;
        }
        return true;
    }
    
    /**
     * Returns true if this set contains none of the characters
     * of the given range.
     * @param start first character, inclusive, of the range
     * @param end last character, inclusive, of the range
     * @return true if the test condition is met
     */
    public boolean containsNone(int start, int end) {
        if (start < MIN_VALUE || start > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(start, 6));
        }
        if (end < MIN_VALUE || end > MAX_VALUE) {
            throw new IllegalArgumentException("Invalid code point U+" + Utility.hex(end, 6));
        }
        int i = -1;
        while (true) {
            if (start < list[++i]) break;
        }
        return ((i & 1) == 0 && end < list[i]);
    }

    /**
     * Returns true if this set contains none of the characters and strings
     * of the given set.
     * @param c set to be checked for containment
     * @return true if the test condition is met
     */
    public boolean containsNone(UnicodeSet c) {
        // The specified set is a subset if all of its pairs are contained in
        // this set.  It's possible to code this more efficiently in terms of
        // direct manipulation of the inversion lists if the need arises.
        int n = c.getRangeCount();
        for (int i=0; i<n; ++i) {
            if (!containsNone(c.getRangeStart(i), c.getRangeEnd(i))) {
                return false;
            }
        }
        if (!hasRelation(strings, DISJOINT, c.strings)) return false;
        return true;
    }
    
    /**
     * Returns true if this set contains none of the characters
     * of the given string.
     * @param s string containing characters to be checked for containment
     * @return true if the test condition is met
     */
    public boolean containsNone(String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (contains(cp)) return false;
        }
        return true;
    }
        
    /**
     * Returns true if this set contains one or more of the characters
     * in the given range.
     * @param start first character, inclusive, of the range
     * @param end last character, inclusive, of the range
     * @return true if the condition is met
     */
    public final boolean containsSome(int start, int end) {
    	return !containsNone(start, end);
    }
        
    /**
     * Returns true if this set contains one or more of the characters
     * and strings of the given set.
     * @param c set to be checked for containment
     * @return true if the condition is met
     */
    public final boolean containsSome(UnicodeSet s) {
    	return !containsNone(s);
    }
        
    /**
     * Returns true if this set contains one or more of the characters
     * of the given string.
     * @param s string containing characters to be checked for containment
     * @return true if the condition is met
     */
    public final boolean containsSome(String s) {
    	return !containsNone(s);
    }
        
        
    /**
     * Adds all of the elements in the specified set to this set if
     * they're not already present.  This operation effectively
     * modifies this set so that its value is the <i>union</i> of the two
     * sets.  The behavior of this operation is unspecified if the specified
     * collection is modified while the operation is in progress.
     *
     * @param c set whose elements are to be added to this set.
     */
    public UnicodeSet addAll(UnicodeSet c) {
        add(c.list, c.len, 0);
        strings.addAll(c.strings);
        return this;
    }

    /**
     * Retains only the elements in this set that are contained in the
     * specified set.  In other words, removes from this set all of
     * its elements that are not contained in the specified set.  This
     * operation effectively modifies this set so that its value is
     * the <i>intersection</i> of the two sets.
     *
     * @param c set that defines which elements this set will retain.
     */
    public UnicodeSet retainAll(UnicodeSet c) {
        retain(c.list, c.len, 0);
        strings.retainAll(c.strings);
        return this;
    }

    /**
     * Removes from this set all of its elements that are contained in the
     * specified set.  This operation effectively modifies this
     * set so that its value is the <i>asymmetric set difference</i> of
     * the two sets.
     *
     * @param c set that defines which elements will be removed from
     *          this set.
     */
    public UnicodeSet removeAll(UnicodeSet c) {
        retain(c.list, c.len, 2);
        strings.removeAll(c.strings);
        return this;
    }

    /**
     * Complements in this set all elements contained in the specified
     * set.  Any character in the other set will be removed if it is
     * in this set, or will be added if it is not in this set.
     *
     * @param c set that defines which elements will be complemented from
     *          this set.
     */
    public UnicodeSet complementAll(UnicodeSet c) {
        xor(c.list, c.len, 0);
        doOperation(strings, COMPLEMENTALL, c.strings);
        return this;
    }

    /**
     * Removes all of the elements from this set.  This set will be
     * empty after this call returns.
     */
    public UnicodeSet clear() {
        list[0] = HIGH;
        len = 1;
        pat = null;
        strings.clear();
        return this;
    }

    /**
     * Iteration method that returns the number of ranges contained in
     * this set.
     * @see #getRangeStart
     * @see #getRangeEnd
     */
    public int getRangeCount() {
        return len/2;
    }

    /**
     * Iteration method that returns the first character in the
     * specified range of this set.
     * @exception ArrayIndexOutOfBoundsException if index is outside
     * the range <code>0..getRangeCount()-1</code>
     * @see #getRangeCount
     * @see #getRangeEnd
     */
    public int getRangeStart(int index) {
        return list[index*2];
    }

    /**
     * Iteration method that returns the last character in the
     * specified range of this set.
     * @exception ArrayIndexOutOfBoundsException if index is outside
     * the range <code>0..getRangeCount()-1</code>
     * @see #getRangeStart
     * @see #getRangeEnd
     */
    public int getRangeEnd(int index) {
        return (list[index*2 + 1] - 1);
    }

    /**
     * Reallocate this objects internal structures to take up the least
     * possible space, without changing this object's value.
     */
    public UnicodeSet compact() {
        if (len != list.length) {
            int[] temp = new int[len];
            System.arraycopy(list, 0, temp, 0, len);
            list = temp;
        }
        rangeList = null;
        buffer = null;
        return this;
    }

    /**
     * Compares the specified object with this set for equality.  Returns
     * <tt>true</tt> if the specified object is also a set, the two sets
     * have the same size, and every member of the specified set is
     * contained in this set (or equivalently, every member of this set is
     * contained in the specified set).
     *
     * @param o Object to be compared for equality with this set.
     * @return <tt>true</tt> if the specified Object is equal to this set.
     */
    public boolean equals(Object o) {
        try {
            UnicodeSet that = (UnicodeSet) o;
            if (len != that.len) return false;
            for (int i = 0; i < len; ++i) {
                if (list[i] != that.list[i]) return false;
            }
            if (!strings.equals(that.strings)) return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * Returns the hash code value for this set.
     *
     * @return the hash code value for this set.
     * @see Object#hashCode()
     */
    public int hashCode() {
        int result = len;
        for (int i = 0; i < len; ++i) {
            result *= 1000003;
            result += list[i];
        }
        return result;
    }

    /**
     * Return a programmer-readable string representation of this object.
     */
    public String toString() {
        return toPattern(true);
    }

    //----------------------------------------------------------------
    // Implementation: Pattern parsing
    //----------------------------------------------------------------

    /**
     * Parses the given pattern, starting at the given position.  The character
     * at pattern.charAt(pos.getIndex()) must be '[', or the parse fails.
     * Parsing continues until the corresponding closing ']'.  If a syntax error
     * is encountered between the opening and closing brace, the parse fails.
     * Upon return from a successful parse, the ParsePosition is updated to
     * point to the character following the closing ']', and an inversion
     * list for the parsed pattern is returned.  This method
     * calls itself recursively to parse embedded subpatterns.
     *
     * @param pattern the string containing the pattern to be parsed.  The
     * portion of the string from pos.getIndex(), which must be a '[', to the
     * corresponding closing ']', is parsed.
     * @param pos upon entry, the position at which to being parsing.  The
     * character at pattern.charAt(pos.getIndex()) must be a '['.  Upon return
     * from a successful parse, pos.getIndex() is either the character after the
     * closing ']' of the parsed pattern, or pattern.length() if the closing ']'
     * is the last character of the pattern string.
     * @return an inversion list for the parsed substring
     * of <code>pattern</code>
     * @exception java.lang.IllegalArgumentException if the parse fails.
     */
    void applyPattern(String pattern,
                      ParsePosition pos,
                      SymbolTable symbols,
                      boolean ignoreWhitespace) {

        // Need to build the pattern in a temporary string because
        // _applyPattern calls add() etc., which set pat to empty.
        StringBuffer rebuiltPat = new StringBuffer();
        _applyPattern(pattern, pos, symbols, rebuiltPat, ignoreWhitespace);
        pat = rebuiltPat.toString();
    }

    void _applyPattern(String pattern, ParsePosition pos,
                       SymbolTable symbols, StringBuffer rebuiltPat,
                       boolean ignoreWhitespace) {

        // If the pattern contains any of the following, we save a
        // rebuilt (variable-substituted) copy of the source pattern:
        // - a category
        // - an intersection or subtraction operator
        // - an anchor (trailing '$', indicating RBT ether)
        boolean rebuildPattern = false;
        StringBuffer newPat = new StringBuffer("[");
        int nestedPatStart = -1; // see below for usage
        boolean nestedPatDone = false; // see below for usage
        StringBuffer multiCharBuffer = new StringBuffer();
        

        boolean invert = false;
        clear();

        final int NONE = -1;
        int lastChar = NONE; // This is either a char (0..10FFFF) or -1
        boolean isLastLiteral = false; // TRUE if lastChar was a literal
        char lastOp = 0;

        /* This loop iterates over the characters in the pattern.  We start at
         * the position specified by pos.  We exit the loop when either a
         * matching closing ']' is seen, or we read all characters of the
         * pattern.  In the latter case an error will be thrown.
         */

        /* Pattern syntax:
         *  pat := '[' '^'? elem* ']'
         *  elem := a | a '-' a | set | set op set
         *  set := pat | (a set variable)
         *  op := '&' | '-'
         *  a := (a character, possibly defined by a var)
         */

        // mode 0: No chars parsed yet; next must be '['
        // mode 1: '[' seen; if next is '^' or ':' then special
        // mode 2: '[' '^'? seen; parse pattern and close with ']'
        // mode 3: '[:' seen; parse category and close with ':]'
        // mode 4: ']' seen; parse complete
        // mode 5: Top-level property pattern seen
        int mode = 0;
        int start = pos.getIndex();
        int i = start;
        int limit = pattern.length();
        /* In the case of an embedded SymbolTable variable, we look it up and
         * then take characters from the resultant char[] array.  These chars
         * are subjected to an extra level of lookup in the SymbolTable in case
         * they are stand-ins for a nested UnicodeSet.  */
        char[] varValueBuffer = null;
        int ivarValueBuffer = 0;
        int anchor = 0;
        int c;
        while (i<limit) {
            /* If the next element is a single character, c will be set to it,
             * and nestedSet will be null.  In this case isLiteral indicates
             * whether the character should assume special meaning if it has
             * one.  If the next element is a nested set, either via a variable
             * reference, or via an embedded "[..]"  or "[:..:]" pattern, then
             * nestedSet will be set to the pairs list for the nested set, and
             * c's value should be ignored.
             */
            UnicodeSet nestedSet = null;
            boolean isLiteral = false;
            if (varValueBuffer != null) {
                if (ivarValueBuffer < varValueBuffer.length) {
                    c = UTF16.charAt(varValueBuffer, 0, varValueBuffer.length, ivarValueBuffer);
                    ivarValueBuffer += UTF16.getCharCount(c);
                    UnicodeMatcher m = symbols.lookupMatcher(c); // may be NULL
                    try {
                        nestedSet = (UnicodeSet) m;
                    } catch (ClassCastException e) {
                        throw new IllegalArgumentException("Syntax error");
                    }
                    nestedPatDone = false;
                } else {
                    varValueBuffer = null;
                    c = UTF16.charAt(pattern, i);
                    i += UTF16.getCharCount(c);
                }
            } else {
                c = UTF16.charAt(pattern, i);
                i += UTF16.getCharCount(c);
            }

            if (ignoreWhitespace && UCharacterProperty.isRuleWhiteSpace(c)) {
                continue;
            }

            // Keep track of the count of characters after an alleged anchor
            if (anchor > 0) {
                ++anchor;
            }

            // Parse the opening '[' and optional following '^'
            switch (mode) {
            case 0:
                if (resemblesPropertyPattern(pattern, i-1)) {
                    mode = 3;
                    break; // Fall through
                } else if (c == '[') {
                    mode = 1; // Next look for '^'
                    continue;
                } else {
                    throw new IllegalArgumentException("Missing opening '['");
                }
            case 1:
                mode = 2;
                switch (c) {
                case '^':
                    invert = true;
                    newPat.append((char) c);
                    continue; // Back to top to fetch next character
                case '-':
                    isLiteral = true; // Treat leading '-' as a literal
                    break; // Fall through
                }
                // else fall through and parse this character normally
            }

            // After opening matter is parsed ("[", "[^", or "[:"), the mode
            // will be 2 if we want a closing ']', or 3 if we should parse a
            // category and close with ":]".

            // Only process escapes, variable references, and nested sets
            // if we are _not_ retrieving characters from the variable
            // buffer.  Characters in the variable buffer have already
            // benn through escape and variable reference processing.
            if (varValueBuffer == null) {
                /**
                 * Handle property set patterns.
                 */
                if (resemblesPropertyPattern(pattern, i-1)) {
                    ParsePosition pp = new ParsePosition(i-1);
                    nestedSet = new UnicodeSet();
                    nestedSet.applyPropertyPattern(pattern, pp);
                    nestedPatStart = newPat.length();
                    nestedPatDone = true; // we're going to do it just below

                    switch (lastOp) {
                    case '-':
                    case '&':
                        newPat.append(lastOp);
                        break;
                    }

                    // If we have a top-level property pattern, then trim
                    // off the opening '[' and use the property pattern
                    // as the entire pattern.
                    if (mode == 3) {
                        newPat.deleteCharAt(0);
                    }
                    newPat.append(pattern.substring(i-1, pp.getIndex()));
                    rebuildPattern = true;

                    i = pp.getIndex(); // advance past property pattern
                    
                    if (mode == 3) {
                        // Entire pattern is a category; leave parse
                        // loop.  This is one of 2 ways we leave this
                        // loop if the pattern is well-formed.
                        set(nestedSet);
                        mode = 5;
                        break;
                    }
                }

                /* Handle escapes.  If a character is escaped, then it assumes its
                 * literal value.  This is true for all characters, both special
                 * characters and characters with no special meaning.  We also
                 * interpret '\\uxxxx' Unicode escapes here (as literals).
                 */
                else if (c == '\\') {
                    int[] offset = new int[] { i };
                    int escaped = Utility.unescapeAt(pattern, offset);
                    if (escaped == -1) {
                        int sta = Math.max(i - 8, 0);
                        int lim = Math.min(i + 16, pattern.length());
                        throw new IllegalArgumentException("Invalid escape sequence " +
                                                           pattern.substring(sta, i-1) +
                                                           "|" +
                                                           pattern.substring(i-1, lim));
                    }
                    i = offset[0];
                    isLiteral = true;
                    c = escaped;
                }

                /* Parse variable references.  These are treated as literals.  If a
                 * variable refers to a UnicodeSet, its stand in character is
                 * returned in the char[] buffer.
                 * Variable names are only parsed if varNameToChar is not null.
                 * Set variables are only looked up if varCharToSet is not null.
                 */
                else if (symbols != null && !isLiteral && c == SymbolTable.SYMBOL_REF) {
                    pos.setIndex(i);
                    String name = symbols.parseReference(pattern, pos, limit);
                    if (name != null) {
                        varValueBuffer = symbols.lookup(name);
                        if (varValueBuffer == null) {
                            throw new IllegalArgumentException("Undefined variable: "
                                                               + name);
                        }
                        ivarValueBuffer = 0;
                        i = pos.getIndex(); // Make i point PAST last char of var name
                    } else {
                        // Got a null; this means we have an isolated $.
                        // Tentatively assume this is an anchor.
                        anchor = 1;
                    }
                    continue; // Back to the top to get varValueBuffer[0]
                }

                /* An opening bracket indicates the first bracket of a nested
                 * subpattern.
                 */
                else if (!isLiteral && c == '[') {
                    // Record position before nested pattern
                    nestedPatStart = newPat.length();

                    // Recurse to get the pairs for this nested set.
                    // Backup i to '['.
                    pos.setIndex(--i);
                    switch (lastOp) {
                    case '-':
                    case '&':
                        newPat.append(lastOp);
                        break;
                    }
                    nestedSet = new UnicodeSet();
                    nestedSet._applyPattern(pattern, pos, symbols, newPat, ignoreWhitespace);
                    nestedPatDone = true;
                    i = pos.getIndex();
                } else if (!isLiteral && c == '{') {
                    // start of a string. find the rest.
                    int length = 0;
                    int st = i;
                    multiCharBuffer.setLength(0);
                    while (i < pattern.length()) {
                        int ch = UTF16.charAt(pattern, i);
                        i += UTF16.getCharCount(ch); 
                        if (ch == '}') {
                            length = -length; // signal that we saw '}'
                            break;
                        } else if (ch == '\\') {
                            int[] offset = new int[] { i };
                            ch = Utility.unescapeAt(pattern, offset);
                            if (ch == -1) {
                                int sta = Math.max(i - 8, 0);
                                int lim = Math.min(i + 16, pattern.length());
                                throw new IllegalArgumentException("Invalid escape sequence " +
                                                                   pattern.substring(sta, i-1) +
                                                                   "|" +
                                                                   pattern.substring(i-1, lim));
                            }
                            i = offset[0];
                        }
                        --length; // sic; see above
                        UTF16.append(multiCharBuffer, ch);
                    }
                    if (length < 1) {
                        throw new IllegalArgumentException("Invalid multicharacter string");
                    }
                    // We have new string. Add it to set and continue;
                    // we don't need to drop through to the further
                    // processing
                    add(multiCharBuffer.toString());
                    newPat.append('{').append(pattern.substring(st, i));
                    rebuildPattern = true;
                    continue;
                }
            }

            /* At this point we have either a character c, or a nested set.  If
             * we have encountered a nested set, either embedded in the pattern,
             * or as a variable, we have a non-null nestedSet, and c should be
             * ignored.  Otherwise c is the current character, and isLiteral
             * indicates whether it is an escaped literal (or variable) or a
             * normal unescaped character.  Unescaped characters '-', '&', and
             * ']' have special meanings.
             */
            if (nestedSet != null) {
                if (lastChar != NONE) {
                    if (lastOp != 0) {
                        throw new IllegalArgumentException("Illegal rhs for " + lastChar + lastOp);
                    }
                    add(lastChar, lastChar);
                    if (nestedPatDone) {
                        // If there was a character before the nested set,
                        // then we need to insert it in newPat before the
                        // pattern for the nested set.  This position was
                        // recorded in nestedPatStart.
                        StringBuffer s = new StringBuffer();
                        _appendToPat(s, lastChar, false);
                        newPat.insert(nestedPatStart, s.toString());
                    } else {
                        _appendToPat(newPat, lastChar, false);
                    }
                    lastChar = NONE;
                }
                switch (lastOp) {
                case '-':
                    removeAll(nestedSet);
                    break;
                case '&':
                    retainAll(nestedSet);
                    break;
                case 0:
                    addAll(nestedSet);
                    break;
                }

                // Get the pattern for the nested set, if we haven't done so
                // already.
                if (!nestedPatDone) {
                    if (lastOp != 0) {
                        newPat.append(lastOp);
                    }
                    nestedSet._toPattern(newPat, false);
                }
                rebuildPattern = true;

                lastOp = 0;

            } else if (!isLiteral && c == ']') {
                // Final closing delimiter.  This is the only way we leave this
                // loop if the pattern is well-formed.
                if (anchor > 2 || anchor == 1) {
                    throw new IllegalArgumentException("Syntax error near $" + pattern);

                }
                if (anchor == 2) {
                    rebuildPattern = true;
                    newPat.append(SymbolTable.SYMBOL_REF);
                    add(TransliterationRule.ETHER);
                }
                mode = 4;
                break;
            } else if (lastOp == 0 && !isLiteral && (c == '-' || c == '&')) {
                lastOp = (char) c;
            } else if (lastOp == '-') {
                if (lastChar >= c) {
                    // Don't allow redundant (a-a) or empty (b-a) ranges;
                    // these are most likely typos.
                    throw new IllegalArgumentException("Invalid range " + lastChar +
                                                       '-' + c);
                }
                add(lastChar, c);
                _appendToPat(newPat, lastChar, false);
                newPat.append('-');
                _appendToPat(newPat, c, false);
                lastOp = 0;
                lastChar = NONE;
            } else if (lastOp != 0) {
                // We have <set>&<char> or <char>&<char>
                throw new IllegalArgumentException("Unquoted " + lastOp);
            } else {
                if (lastChar != NONE) {
                    // We have <char><char>
                    add(lastChar, lastChar);
                    _appendToPat(newPat, lastChar, false);
                }
                lastChar = c;
                isLastLiteral = isLiteral;
            }
        }

        if (mode < 4) {
            throw new IllegalArgumentException("Missing ']'");
        }

        // Treat a trailing '$' as indicating ETHER.  This code is only
        // executed if symbols == NULL; otherwise other code parses the
        // anchor.
        if (lastChar == SymbolTable.SYMBOL_REF && !isLastLiteral) {
            rebuildPattern = true;
            newPat.append((char) lastChar);
            add(TransliterationRule.ETHER);
        }
        
        else if (lastChar != NONE) {
            add(lastChar, lastChar);
            _appendToPat(newPat, lastChar, false);
        }

        // Handle unprocessed stuff preceding the closing ']'
        if (lastOp == '-') {
            // Trailing '-' is treated as literal
            add(lastOp, lastOp);
            newPat.append('-');
        } else if (lastOp == '&') {
            throw new IllegalArgumentException("Unquoted trailing " + lastOp);
        }

        if (mode == 4) {
            newPat.append(']');
        }

        /**
         * If we saw a '^' after the initial '[' of this pattern, then perform
         * the complement.  (Inversion after '[:' is handled elsewhere.)
         */
        if (invert) {
            complement();
        }

        pos.setIndex(i);

        // Use the rebuilt pattern (newPat) only if necessary.  Prefer the
        // generated pattern.
        if (rebuildPattern) {
            rebuiltPat.append(newPat.toString());
        } else {
            _generatePattern(rebuiltPat, false);
        }

        if (false) {
            // Debug parser
            System.out.println("UnicodeSet(" +
                               pattern.substring(start, i+1) + ") -> " +
                               Utility.escape(toString()));
        }
    }

    //----------------------------------------------------------------
    // Implementation: Utility methods
    //----------------------------------------------------------------

    private void ensureCapacity(int newLen) {
        if (newLen <= list.length) return;
        int[] temp = new int[newLen + GROW_EXTRA];
        System.arraycopy(list, 0, temp, 0, len);
        list = temp;
    }

    private void ensureBufferCapacity(int newLen) {
        if (buffer != null && newLen <= buffer.length) return;
        buffer = new int[newLen + GROW_EXTRA];
    }

    /**
     * Assumes start <= end.
     */
    private int[] range(int start, int end) {
        if (rangeList == null) {
            rangeList = new int[] { start, end+1, HIGH };
        } else {
            rangeList[0] = start;
            rangeList[1] = end+1;
        }
        return rangeList;
    }

    //----------------------------------------------------------------
    // Implementation: Fundamental operations
    //----------------------------------------------------------------

    // polarity = 0, 3 is normal: x xor y
    // polarity = 1, 2: x xor ~y == x === y

    private UnicodeSet xor(int[] other, int otherLen, int polarity) {
        ensureBufferCapacity(len + otherLen);
        int i = 0, j = 0, k = 0;
        int a = list[i++];
        int b;
        if (polarity == 1 || polarity == 2) {
            b = LOW;
            if (other[j] == LOW) { // skip base if already LOW
                ++j;
                b = other[j];
            }
        } else {
            b = other[j++];
        }
        // simplest of all the routines
        // sort the values, discarding identicals!
        while (true) {
            if (a < b) {
                buffer[k++] = a;
                a = list[i++];
            } else if (b < a) {
                buffer[k++] = b;
                b = other[j++];
            } else if (a != HIGH) { // at this point, a == b
                // discard both values!
                a = list[i++];
                b = other[j++];
            } else { // DONE!
                buffer[k++] = HIGH;
                len = k;
                break;
            }
        }
        // swap list and buffer
        int[] temp = list;
        list = buffer;
        buffer = temp;
        pat = null;
        return this;
    }

    // polarity = 0 is normal: x union y
    // polarity = 2: x union ~y
    // polarity = 1: ~x union y
    // polarity = 3: ~x union ~y

    private UnicodeSet add(int[] other, int otherLen, int polarity) {
        ensureBufferCapacity(len + otherLen);
        int i = 0, j = 0, k = 0;
        int a = list[i++];
        int b = other[j++];
        // change from xor is that we have to check overlapping pairs
        // polarity bit 1 means a is second, bit 2 means b is.
        main:
        while (true) {
            switch (polarity) {
              case 0: // both first; take lower if unequal
                if (a < b) { // take a
                    // Back up over overlapping ranges in buffer[]
                    if (k > 0 && a <= buffer[k-1]) {
                        // Pick latter end value in buffer[] vs. list[]
                        a = max(list[i], buffer[--k]);
                    } else {
                        // No overlap
                        buffer[k++] = a;
                        a = list[i];
                    }
                    i++; // Common if/else code factored out
                    polarity ^= 1;
                } else if (b < a) { // take b
                    if (k > 0 && b <= buffer[k-1]) {
                        b = max(other[j], buffer[--k]);
                    } else {
                        buffer[k++] = b;
                        b = other[j];
                    }
                    j++;
                    polarity ^= 2;
                } else { // a == b, take a, drop b
                    if (a == HIGH) break main;
                    // This is symmetrical; it doesn't matter if
                    // we backtrack with a or b. - liu
                    if (k > 0 && a <= buffer[k-1]) {
                        a = max(list[i], buffer[--k]);
                    } else {
                        // No overlap
                        buffer[k++] = a;
                        a = list[i];
                    }
                    i++;
                    polarity ^= 1;
                    b = other[j++]; polarity ^= 2;
                }
                break;
              case 3: // both second; take higher if unequal, and drop other
                if (b <= a) { // take a
                    if (a == HIGH) break main;
                    buffer[k++] = a;
                } else { // take b
                    if (b == HIGH) break main;
                    buffer[k++] = b;
                }
                a = list[i++]; polarity ^= 1;   // factored common code
                b = other[j++]; polarity ^= 2;
                break;
              case 1: // a second, b first; if b < a, overlap
                if (a < b) { // no overlap, take a
                    buffer[k++] = a; a = list[i++]; polarity ^= 1;
                } else if (b < a) { // OVERLAP, drop b
                    b = other[j++]; polarity ^= 2;
                } else { // a == b, drop both!
                    if (a == HIGH) break main;
                    a = list[i++]; polarity ^= 1;
                    b = other[j++]; polarity ^= 2;
                }
                break;
              case 2: // a first, b second; if a < b, overlap
                if (b < a) { // no overlap, take b
                    buffer[k++] = b; b = other[j++]; polarity ^= 2;
                } else  if (a < b) { // OVERLAP, drop a
                    a = list[i++]; polarity ^= 1;
                } else { // a == b, drop both!
                    if (a == HIGH) break main;
                    a = list[i++]; polarity ^= 1;
                    b = other[j++]; polarity ^= 2;
                }
                break;
            }
        }
        buffer[k++] = HIGH;    // terminate
        len = k;
        // swap list and buffer
        int[] temp = list;
        list = buffer;
        buffer = temp;
        pat = null;
        return this;
    }

    // polarity = 0 is normal: x intersect y
    // polarity = 2: x intersect ~y == set-minus
    // polarity = 1: ~x intersect y
    // polarity = 3: ~x intersect ~y

    private UnicodeSet retain(int[] other, int otherLen, int polarity) {
        ensureBufferCapacity(len + otherLen);
        int i = 0, j = 0, k = 0;
        int a = list[i++];
        int b = other[j++];
        // change from xor is that we have to check overlapping pairs
        // polarity bit 1 means a is second, bit 2 means b is.
        main:
        while (true) {
            switch (polarity) {
              case 0: // both first; drop the smaller
                if (a < b) { // drop a
                    a = list[i++]; polarity ^= 1;
                } else if (b < a) { // drop b
                    b = other[j++]; polarity ^= 2;
                } else { // a == b, take one, drop other
                    if (a == HIGH) break main;
                    buffer[k++] = a; a = list[i++]; polarity ^= 1;
                    b = other[j++]; polarity ^= 2;
                }
                break;
              case 3: // both second; take lower if unequal
                if (a < b) { // take a
                    buffer[k++] = a; a = list[i++]; polarity ^= 1;
                } else if (b < a) { // take b
                    buffer[k++] = b; b = other[j++]; polarity ^= 2;
                } else { // a == b, take one, drop other
                    if (a == HIGH) break main;
                    buffer[k++] = a; a = list[i++]; polarity ^= 1;
                    b = other[j++]; polarity ^= 2;
                }
                break;
              case 1: // a second, b first;
                if (a < b) { // NO OVERLAP, drop a
                    a = list[i++]; polarity ^= 1;
                } else if (b < a) { // OVERLAP, take b
                    buffer[k++] = b; b = other[j++]; polarity ^= 2;
                } else { // a == b, drop both!
                    if (a == HIGH) break main;
                    a = list[i++]; polarity ^= 1;
                    b = other[j++]; polarity ^= 2;
                }
                break;
              case 2: // a first, b second; if a < b, overlap
                if (b < a) { // no overlap, drop b
                    b = other[j++]; polarity ^= 2;
                } else  if (a < b) { // OVERLAP, take a
                    buffer[k++] = a; a = list[i++]; polarity ^= 1;
                } else { // a == b, drop both!
                    if (a == HIGH) break main;
                    a = list[i++]; polarity ^= 1;
                    b = other[j++]; polarity ^= 2;
                }
                break;
            }
        }
        buffer[k++] = HIGH;    // terminate
        len = k;
        // swap list and buffer
        int[] temp = list;
        list = buffer;
        buffer = temp;
        pat = null;
        return this;
    }

    private static final int max(int a, int b) {
        return (a > b) ? a : b;
    }
    
    /**
     * The relationship between two sets A and B can be determined by looking at:
     * A - B
     * A & B (intersection)
     * B - A
     * These are represented by a set of bits.
     * Bit 2 is true if A - B is not empty
     * Bit 1 is true if A & B is not empty
     * BIT 0 is true if B - A is not empty
     */
    
    public static final int
        A_NOT_B = 4,
        A_AND_B = 2,
        B_NOT_A = 1;
    
    /**
     * There are 8 combinations of the relationship bits. These correspond to
     * the filters (combinations of allowed bits) in hasRelation. They also
     * correspond to the modification functions, listed in comments.
     */
     
    public static final int 
       ANY =            A_NOT_B |   A_AND_B |   B_NOT_A,    // union,           addAll
       CONTAINS =       A_NOT_B |   A_AND_B,                // A                (unnecessary)
       DISJOINT =       A_NOT_B |               B_NOT_A,    // A xor B,         missing Java function
       ISCONTAINED =                A_AND_B |   B_NOT_A,    // B                (unnecessary)
       NO_B =           A_NOT_B,                            // A setDiff B,     removeAll
       EQUALS =                     A_AND_B,                // A intersect B,   retainAll
       NO_A =                                   B_NOT_A,    // B setDiff A,     removeAll
       NONE =           0,                                  // null             (unnecessary)
       
       ADDALL = ANY,                // union,           addAll
       A = CONTAINS,                // A                (unnecessary)
       COMPLEMENTALL = DISJOINT,    // A xor B,         missing Java function
       B = ISCONTAINED,             // B                (unnecessary)
       REMOVEALL = NO_B,            // A setDiff B,     removeAll
       RETAINALL = EQUALS,          // A intersect B,   retainAll
       B_REMOVEALL = NO_A;          // B setDiff A,     removeAll
       
  
    /**
     * Utility that could be on SortedSet. Faster implementation than
     * what is in Java for doing contains, equals, etc.
     * @param a first set
     * @param allow filter, using ANY, CONTAINS, etc.
     * @param b second set
     * @return whether the filter relationship is true or not.
     */
    
    public static boolean hasRelation(SortedSet a, int allow, SortedSet b) {
        if (allow < NONE || allow > ANY) {
            throw new IllegalArgumentException("Relation " + allow + " out of range");
        }
        
        // extract filter conditions
        // these are the ALLOWED conditions Set
        
        boolean anb = (allow & A_NOT_B) != 0;
        boolean ab = (allow & A_AND_B) != 0;
        boolean bna = (allow & B_NOT_A) != 0;
        
        // quick check on sizes
        switch(allow) {
            case CONTAINS: if (a.size() < b.size()) return false; break;
            case ISCONTAINED: if (a.size() > b.size()) return false; break;
            case EQUALS: if (a.size() != b.size()) return false; break;
        }
        
        // check for null sets
        if (a.size() == 0) {
            if (b.size() == 0) return true;
            return bna;
        } else if (b.size() == 0) {
            return anb;
        }
        
        // pick up first strings, and start comparing
        Iterator ait = a.iterator();
        Iterator bit = b.iterator();
        
        Comparable aa = (Comparable) ait.next();
        Comparable bb = (Comparable) bit.next();
        
        while (true) {
            int comp = aa.compareTo(bb);
            if (comp == 0) {
                if (!ab) return false;
                if (!ait.hasNext()) {
                    if (!bit.hasNext()) return true;
                    return bna;
                } else if (!bit.hasNext()) {
                    return anb;
                }
                aa = (Comparable) ait.next();
                bb = (Comparable) bit.next();
            } else if (comp < 0) {
                if (!anb) return false;
                if (!ait.hasNext()) {
                    return bna;
                }
                aa = (Comparable) ait.next(); 
            } else  {
                if (!bna) return false;
                if (!bit.hasNext()) {
                    return anb;
                }
                bb = (Comparable) bit.next();
            }
        }
    }
    
    /**
     * Utility that could be on SortedSet. Allows faster implementation than
     * what is in Java for doing addAll, removeAll, retainAll, (complementAll).
     * @param a first set
     * @param allow filter, using ANY, CONTAINS, etc.
     * @param b second set
     * @return whether the filter relationship is true or not.
     */
    
    public static SortedSet doOperation(SortedSet a, int relation, SortedSet b) {
        // TODO: optimize this as above
        TreeSet temp;
        switch (relation) {
            case ADDALL:
                a.addAll(b); 
                return a;
            case A:
                return a; // no action
            case B:
                a.clear(); 
                a.addAll(b); 
                return a;
            case REMOVEALL: 
                a.removeAll(b);
                return a;
            case RETAINALL: 
                a.retainAll(b);
                return a;
            // the following is the only case not really supported by Java
            // although all could be optimized
            case COMPLEMENTALL:
                temp = new TreeSet(b);
                temp.removeAll(a);
                a.removeAll(b);
                a.addAll(temp);
                return a;
            case B_REMOVEALL:
                temp = new TreeSet(b);
                temp.removeAll(a);
                a.clear();
                a.addAll(temp);
                return a;
            case NONE:
                a.clear();
                return a;
            default: 
                throw new IllegalArgumentException("Relation " + relation + " out of range");
        }
    }    

    //----------------------------------------------------------------
    // Generic filter-based scanning code
    //----------------------------------------------------------------

    private static interface Filter {
        boolean contains(int codePoint);
    }

    private static class NumericValueFilter implements Filter {
        double value;
        NumericValueFilter(double value) { this.value = value; }
        public boolean contains(int ch) {
            return UCharacter.getUnicodeNumericValue(ch) == value;
        }
    }

    private static class GeneralCategoryFilter implements Filter {
        int mask;
        GeneralCategoryFilter(int mask) { this.mask = mask; }
        public boolean contains(int ch) {
            return ((1 << UCharacter.getType(ch)) & mask) != 0;
        }
    }

    private static class IntPropertyFilter implements Filter {
        int prop;
        int value;
        IntPropertyFilter(int prop, int value) {
            this.prop = prop;
            this.value = value;
        }
        public boolean contains(int ch) {
            return UCharacter.getIntPropertyValue(ch, prop) == value;
        }
    }

    /**
     * Generic filter-based scanning code for UCD property UnicodeSets.
     */
    private UnicodeSet applyFilter(Filter filter) {
        // Walk through all Unicode characters, noting the start
        // and end of each range for which filter.contain(c) is
        // true.  Add each range to a set.
        //
        // To improve performance, use the INCLUSIONS set, which
        // encodes information about character ranges that are known
        // to have identical properties, such as the CJK Ideographs
        // from U+4E00 to U+9FA5.  INCLUSIONS contains all characters
        // except the first characters of such ranges.
        //
        // TODO Where possible, instead of scanning over code points,
        // use internal property data to initialize UnicodeSets for
        // those properties.  Scanning code points is slow.

        clear();

        int startHasProperty = -1;
        int limitRange = INCLUSIONS.getRangeCount();

        for (int j=0; j<limitRange; ++j) {
            // get current range
            int start = INCLUSIONS.getRangeStart(j);
            int end = INCLUSIONS.getRangeEnd(j);

            // for all the code points in the range, process
            for (int ch = start; ch <= end; ++ch) {
                // only add to the unicodeset on inflection points --
                // where the hasProperty value changes to false
                if (filter.contains(ch)) {
                    if (startHasProperty < 0) {
                        startHasProperty = ch;
                    }
                } else if (startHasProperty >= 0) {
                    add(startHasProperty, ch-1);
                    startHasProperty = -1;
                }
            }
        }
        if (startHasProperty >= 0) {
            add(startHasProperty, 0x10FFFF);
        }

        return this;
    }


    /**
     * Remove leading and trailing rule white space and compress
     * internal rule white space to a single space character.
     *
     * @see UCharacterProperty#isRuleWhiteSpace
     */
    private static String mungeCharName(String source) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<source.length(); ) {
            int ch = UTF16.charAt(source, i);
            i += UTF16.getCharCount(ch);
            if (UCharacterProperty.isRuleWhiteSpace(ch)) {
                if (buf.length() == 0 ||
                    buf.charAt(buf.length() - 1) == ' ') {
                    continue;
                }
                ch = ' '; // convert to ' '
            }
            UTF16.append(buf, ch);
        }
        if (buf.length() != 0 &&
            buf.charAt(buf.length() - 1) == ' ') {
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    //----------------------------------------------------------------
    // Property set API
    //----------------------------------------------------------------
    
    /**
     * Modifies this set to contain those code points which have the
     * given value for the given binary or enumerated property, as
     * returned by UCharacter.getIntPropertyValue.  Prior contents of
     * this set are lost.
     *
     * @param prop a property in the range
     * UProperty.BIN_START..UProperty.BIN_LIMIT-1 or
     * UProperty.INT_START..UProperty.INT_LIMIT-1.
     *
     * @param value a value in the range
     * UCharacter.getIntPropertyMinValue(prop)..
     * UCharacter.getIntPropertyMaxValue(prop), with one exception.
     * If prop is UProperty.GENERAL_CATEGORY, then value should not be
     * a UCharacter.getType() result, but rather a mask value produced
     * by logically ORing (1 << UCharacter.getType()) values together.
     * This allows grouped categories such as [:L:] to be repregsented.
     *
     * @return a reference to this set
     *
     * @draft ICU 2.2
     */
    public UnicodeSet applyIntPropertyValue(int prop, int value) {
        if (prop == UProperty.GENERAL_CATEGORY) {
            applyFilter(new GeneralCategoryFilter(value));
        } else {
            applyFilter(new IntPropertyFilter(prop, value));
        }
        return this;
    }

    /**
     * Modifies this set to contain those code points which have the
     * given value for the given property.  Prior contents of this
     * set are lost.
     *
     * @param propertyAlias a property alias, either short or long.
     * The name is matched loosely.  See PropertyAliases.txt for names
     * and a description of loose matching.  If the value string is
     * empty, then this string is interpreted as either a
     * General_Category value alias, a Script value alias, a binary
     * property alias, or a special ID.  Special IDs are matched
     * loosely and correspond to the following sets:
     *
     * "ANY" = [\u0000-\U0010FFFF],
     * "ASCII" = [\u0000-\u007F].
     *
     * @param valueAlias a value alias, either short or long.  The
     * name is matched loosely.  See PropertyValueAliases.txt for
     * names and a description of loose matching.  In addition to
     * aliases listed, numeric values and canonical combining classes
     * may be expressed numerically, e.g., ("nv", "0.5") or ("ccc",
     * "220").  The value string may also be empty.
     *
     * @return a reference to this set
     *
     * @draft ICU 2.2
     */
    public UnicodeSet applyPropertyAlias(String propertyAlias,
                                         String valueAlias) {
        int p;
        int v;
        boolean mustNotBeEmpty = false;

        if (valueAlias.length() > 0) {
            p = UCharacter.getPropertyEnum(propertyAlias);

            if ((p >= UProperty.BINARY_START && p < UProperty.BINARY_LIMIT) ||
                (p >= UProperty.INT_START && p < UProperty.INT_LIMIT)) {
                try {
                    v = UCharacter.getPropertyValueEnum(p, valueAlias);
                } catch (IllegalArgumentException e) {
                    // Handle numeric CCC
                    if (p == UProperty.CANONICAL_COMBINING_CLASS) {
                        v = Integer.parseInt(Utility.deleteRuleWhiteSpace(valueAlias));
                        // If the resultant set is empty then the numeric value
                        // was invalid.
                        mustNotBeEmpty = true;
                    } else {
                        throw e;
                    }
                }
            }

            else {

                switch (p) {
                case UProperty.NUMERIC_VALUE:
                    {
                        double value = Double.parseDouble(Utility.deleteRuleWhiteSpace(valueAlias));
                        applyFilter(new NumericValueFilter(value));
                        return this;
                    }
                case UProperty.NAME:
                case UProperty.UNICODE_1_NAME:
                    {
                        // Must munge name, since
                        // UCharacter.charFromName() does not do
                        // 'loose' matching.
                        String buf = mungeCharName(valueAlias);
                        int ch =
                            (p == UProperty.NAME) ?
                            UCharacter.getCharFromExtendedName(buf) :
                            UCharacter.getCharFromName1_0(buf);
                        if (ch == -1) {
                            throw new IllegalArgumentException("Invalid character name");
                        }
                        clear();
                        add(ch);
                        return this;
                    }
                }

                // p is a non-binary, non-enumerated property that we
                // don't support (yet).
                throw new IllegalArgumentException("Unsupported property");
            }
        }

        else {
            // valueAlias is empty.  Interpret as General Category, Script,
            // Binary property, or ANY or ASCII.  Upon success, p and v will
            // be set.
            try { 
                p = UProperty.GENERAL_CATEGORY;
                v = UCharacter.getPropertyValueEnum(p, propertyAlias);
            } catch (IllegalArgumentException e) {
                try {
                    p = UProperty.SCRIPT;
                    v = UCharacter.getPropertyValueEnum(p, propertyAlias);
                } catch (IllegalArgumentException e2) {
                    try {
                        p = UCharacter.getPropertyEnum(propertyAlias);
                    } catch (IllegalArgumentException e3) {
                        p = -1;
                    }
                    if (p >= UProperty.BINARY_START && p < UProperty.BINARY_LIMIT) {
                        v = 1;
                    } else if (p == -1) {
                        if (0 == UPropertyAliases.compare(ANY_ID, propertyAlias)) {
                            set(MIN_VALUE, MAX_VALUE);
                            return this;
                        } else if (0 == UPropertyAliases.compare(ASCII_ID, propertyAlias)) {
                            set(0, 0x7F);
                            return this;
                        } else {
                            // Property name was never matched.
                            throw new IllegalArgumentException("Invalid property alias");
                        }
                    } else {
                        // Valid propery name, but it isn't binary, so the value
                        // must be supplied.
                        throw new IllegalArgumentException("Missing property value");
                    }
                }
            }
        }

        applyIntPropertyValue(p, v);

        if (mustNotBeEmpty && isEmpty()) {
            // mustNotBeEmpty is set to true if an empty set indicates
            // invalid input.
            throw new IllegalArgumentException("Invalid property value");
        }

        return this;
    }

    //----------------------------------------------------------------
    // Property set patterns
    //----------------------------------------------------------------
    
    /**
     * Return true if the given position, in the given pattern, appears
     * to be the start of a property set pattern.
     */
    private static boolean resemblesPropertyPattern(String pattern, int pos) {
        // Patterns are at least 5 characters long
        if ((pos+5) > pattern.length()) {
            return false;
        }

        // Look for an opening [:, [:^, \p, or \P
        return pattern.regionMatches(pos, "[:", 0, 2) ||
            pattern.regionMatches(true, pos, "\\p", 0, 2) ||
            pattern.regionMatches(pos, "\\N", 0, 2);
    }

    /**
     * Parse the given property pattern at the given parse position.
     */
    private UnicodeSet applyPropertyPattern(String pattern, ParsePosition ppos) {
        int pos = ppos.getIndex();

        // On entry, ppos should point to one of the following locations:

        // Minimum length is 5 characters, e.g. \p{L}
        if ((pos+5) > pattern.length()) {
            return null;
        }

        boolean posix = false; // true for [:pat:], false for \p{pat} \P{pat} \N{pat}
        boolean isName = false; // true for \N{pat}, o/w false
        boolean invert = false;

        // Look for an opening [:, [:^, \p, or \P
        if (pattern.regionMatches(pos, "[:", 0, 2)) {
            posix = true;
            pos = Utility.skipWhitespace(pattern, pos+2);
            if (pos < pattern.length() && pattern.charAt(pos) == '^') {
                ++pos;
                invert = true;
            }
        } else if (pattern.regionMatches(true, pos, "\\p", 0, 2) ||
                   pattern.regionMatches(pos, "\\N", 0, 2)) {
            char c = pattern.charAt(pos+1);
            invert = (c == 'P');
            isName = (c == 'N');
            pos = Utility.skipWhitespace(pattern, pos+2);
            if (pos == pattern.length() || pattern.charAt(pos++) != '{') {
                // Syntax error; "\p" or "\P" not followed by "{"
                return null;
            }
        } else {
            // Open delimiter not seen
            return null;
        }

        // Look for the matching close delimiter, either :] or }
        int close = pattern.indexOf(posix ? ":]" : "}", pos);
        if (close < 0) {
            // Syntax error; close delimiter missing
            return null;
        }

        // Look for an '=' sign.  If this is present, we will parse a
        // medium \p{gc=Cf} or long \p{GeneralCategory=Format}
        // pattern.
        int equals = pattern.indexOf('=', pos);
        String propName, valueName;
        if (equals >= 0 && equals < close && !isName) {
            // Equals seen; parse medium/long pattern
            propName = pattern.substring(pos, equals);
            valueName = pattern.substring(equals+1, close);
        }
        
        else {
            // Handle case where no '=' is seen, and \N{}
            propName = pattern.substring(pos, close);
            valueName = "";
            
            // Handle \N{name}
            if (isName) {
                // This is a little inefficient since it means we have to
                // parse "na" back to UProperty.NAME even though we already
                // know it's UProperty.NAME.  If we refactor the API to
                // support args of (int, String) then we can remove
                // "na" and make this a little more efficient.
                valueName = propName;
                propName = "na";
            }
        }

        applyPropertyAlias(propName, valueName);

        if (invert) {
            complement();
        }

        // Move to the limit position after the close delimiter
        ppos.setIndex(close + (posix ? 2 : 1));

        return this;
    }
}
