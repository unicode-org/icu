/*
 *******************************************************************************
 * Copyright (C) 1996-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.text.*;

import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.*;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.UPropertyAliases;
import com.ibm.icu.impl.SortedSetRelation;
import com.ibm.icu.impl.RuleCharacterIterator;
import com.ibm.icu.util.VersionInfo;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.Collection;

/**
 * A mutable set of Unicode characters and multicharacter strings.  Objects of this class
 * represent <em>character classes</em> used in regular expressions.
 * A character specifies a subset of Unicode code points.  Legal
 * code points are U+0000 to U+10FFFF, inclusive.
 *
 * <p>The UnicodeSet class is not designed to be subclassed.
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
 * classes.  Here are some simple examples:
 *
 * <blockquote>
 *   <table>
 *     <tr align="top">
 *       <td nowrap valign="top" align="left"><code>[]</code></td>
 *       <td valign="top">No characters</td>
 *     </tr><tr align="top">
 *       <td nowrap valign="top" align="left"><code>[a]</code></td>
 *       <td valign="top">The character 'a'</td>
 *     </tr><tr align="top">
 *       <td nowrap valign="top" align="left"><code>[ae]</code></td>
 *       <td valign="top">The characters 'a' and 'e'</td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="left"><code>[a-e]</code></td>
 *       <td valign="top">The characters 'a' through 'e' inclusive, in Unicode code
 *       point order</td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="left"><code>[\\u4E01]</code></td>
 *       <td valign="top">The character U+4E01</td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="left"><code>[a{ab}{ac}]</code></td>
 *       <td valign="top">The character 'a' and the multicharacter strings &quot;ab&quot; and
 *       &quot;ac&quot;</td>
 *     </tr>
 *     <tr>
 *       <td nowrap valign="top" align="left"><code>[\p{Lu}]</code></td>
 *       <td valign="top">All characters in the general category Uppercase Letter</td>
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
 * <a href="http://oss.software.ibm.com/icu/userguide/unicodeSet.html">
 * http://oss.software.ibm.com/icu/userguide/unicodeSet.html</a>.
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
 * literal.  Thus "[a\\-b]", "[-ab]", and "[ab-]" all indicate the same
 * set of three characters, 'a', 'b', and '-'.
 *
 * <p>Sets may be intersected using the '&' operator or the asymmetric
 * set difference may be taken using the '-' operator, for example,
 * "[[:L:]&[\\u0000-\\u0FFF]]" indicates the set of all Unicode letters
 * with values less than 4096.  Operators ('&' and '|') have equal
 * precedence and bind left-to-right.  Thus
 * "[[:L:]-[a-z]-[\\u0100-\\u01FF]]" is equivalent to
 * "[[[:L:]-[a-z]]-[\\u0100-\\u01FF]]".  This only really matters for
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
 *
 * <p><b>Warning</b>: you cannot add an empty string ("") to a UnicodeSet.</p>
 *
 * <p><b>Formal syntax</b></p>
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
 *       | ('\\' </code><em>any character</em><code>)<br>
 *       | ('&#92;u' hex hex hex hex)<br>
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
 * @author Alan Liu
 * @stable ICU 2.0
 */
public class UnicodeSet extends UnicodeFilter {

    private static final int LOW = 0x000000; // LOW <= all valid values. ZERO for codepoints
    private static final int HIGH = 0x110000; // HIGH > all valid values. 10000 for code units.
                                             // 110000 for codepoints

    /**
     * Minimum value that can be stored in a UnicodeSet.
     * @stable ICU 2.0
     */
    public static final int MIN_VALUE = LOW;

    /**
     * Maximum value that can be stored in a UnicodeSet.
     * @stable ICU 2.0
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
     */
    private static UnicodeSet INCLUSIONS = null;

    //----------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------

    /**
     * Constructs an empty set.
     * @stable ICU 2.0
     */
    public UnicodeSet() {
        list = new int[1 + START_EXTRA];
        list[len++] = HIGH;
    }

    /**
     * Constructs a copy of an existing set.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public UnicodeSet(String pattern) {
        this();
        applyPattern(pattern, null, null, IGNORE_SPACE);
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @param ignoreWhitespace if true, ignore characters for which
     * UCharacterProperty.isRuleWhiteSpace() returns true
     * @exception java.lang.IllegalArgumentException if the pattern contains
     * a syntax error.
     * @stable ICU 2.0
     */
    public UnicodeSet(String pattern, boolean ignoreWhitespace) {
        this();
        applyPattern(pattern, null, null, ignoreWhitespace ? IGNORE_SPACE : 0);
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @param options a bitmask indicating which options to apply.
     * Valid options are IGNORE_SPACE and CASE.
     * @exception java.lang.IllegalArgumentException if the pattern contains
     * a syntax error.
     * @internal
     */
    public UnicodeSet(String pattern, int options) {
        this();
        applyPattern(pattern, null, null, options);
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
     * @stable ICU 2.0
     */
    public UnicodeSet(String pattern, ParsePosition pos, SymbolTable symbols) {
        this();
        applyPattern(pattern, pos, symbols, IGNORE_SPACE);
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @param pos on input, the position in pattern at which to start parsing.
     * On output, the position after the last character parsed.
     * @param symbols a symbol table mapping variables to char[] arrays
     * and chars to UnicodeSets
     * @param options a bitmask indicating which options to apply.
     * Valid options are IGNORE_SPACE and CASE.
     * @exception java.lang.IllegalArgumentException if the pattern
     * contains a syntax error.
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public UnicodeSet(String pattern, ParsePosition pos, SymbolTable symbols, int options) {
        this();
        applyPattern(pattern, pos, symbols, options);
    }


    /**
     * Return a new set that is equivalent to this one.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public final UnicodeSet applyPattern(String pattern) {
        return applyPattern(pattern, null, null, IGNORE_SPACE);
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
     * @stable ICU 2.0
     */
    public UnicodeSet applyPattern(String pattern, boolean ignoreWhitespace) {
        return applyPattern(pattern, null, null, ignoreWhitespace ? IGNORE_SPACE : 0);
    }

    /**
     * Modifies this set to represent the set specified by the given pattern,
     * optionally ignoring whitespace.
     * See the class description for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @param options a bitmask indicating which options to apply.
     * Valid options are IGNORE_SPACE and CASE.
     * @exception java.lang.IllegalArgumentException if the pattern
     * contains a syntax error.
     * @internal
     */
    public UnicodeSet applyPattern(String pattern, int options) {
        return applyPattern(pattern, null, null, options);
    }

    /**
     * Return true if the given position, in the given pattern, appears
     * to be the start of a UnicodeSet pattern.
     * @stable ICU 2.0
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
    private static void _appendToPat(StringBuffer buf, String s, boolean escapeUnprintable) {
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(i)) {
            _appendToPat(buf, UTF16.charAt(s, i), escapeUnprintable);
        }
    }

    /**
     * Append the <code>toPattern()</code> representation of a
     * character to the given <code>StringBuffer</code>.
     */
    private static void _appendToPat(StringBuffer buf, int c, boolean escapeUnprintable) {
        if (escapeUnprintable && Utility.isUnprintable(c)) {
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
                    if ((start+1) != end) {
                        result.append('-');
                    }
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
                    if ((start+1) != end) {
                        result.append('-');
                    }
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
     * Returns the number of elements in this set (its cardinality)
     * Note than the elements of a set may include both individual
     * codepoints and strings.
     *
     * @return the number of elements in this set (its cardinality).
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public boolean isEmpty() {
        return len == 1 && strings.size() == 0;
    }

    /**
     * Implementation of UnicodeMatcher API.  Returns <tt>true</tt> if
     * this set contains any character whose low byte is the given
     * value.  This is used by <tt>RuleBasedTransliterator</tt> for
     * indexing.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public int matches(Replaceable text,
                       int[] offset,
                       int limit,
                       boolean incremental) {

        if (offset[0] == limit) {
            // Strings, if any, have length != 0, so we don't worry
            // about them here.  If we ever allow zero-length strings
            // we much check for them here.
            if (contains(UnicodeMatcher.ETHER)) {
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
     * @stable ICU 2.2
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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

        // [..., start_k-1, limit_k-1, start_k, limit_k, ..., HIGH]
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

                // [..., start_k-1, c, c, limit_k, ..., HIGH]
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


            // [..., start_k-1, limit_k-1, start_k, limit_k, ..., HIGH]
            //                             ^
            //                             list[i]

            // [..., start_k-1, limit_k-1, c, c+1, start_k, limit_k, ..., HIGH]
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public final UnicodeSet retainAll(String s) {
        return retainAll(fromAll(s));
    }

    /**
     * Complement EACH of the characters in this string. Note: "ch" == {"c", "h"}
     * If this set already any particular character, it has no effect on that character.
     * @param s the source string
     * @return this object, for chaining
     * @stable ICU 2.0
     */
    public final UnicodeSet complementAll(String s) {
        return complementAll(fromAll(s));
    }

    /**
     * Remove EACH of the characters in this string. Note: "ch" == {"c", "h"}
     * If this set already any particular character, it has no effect on that character.
     * @param s the source string
     * @return this object, for chaining
     * @stable ICU 2.0
     */
    public final UnicodeSet removeAll(String s) {
        return removeAll(fromAll(s));
    }

    /**
     * Makes a set from a multicharacter string. Thus "ch" => {"ch"}
     * <br><b>Warning: you cannot add an empty string ("") to a UnicodeSet.</b>
     * @param s the source string
     * @return a newly created set containing the given string
     * @stable ICU 2.0
     */
    public static UnicodeSet from(String s) {
        return new UnicodeSet().add(s);
    }


    /**
     * Makes a set from each of the characters in the string. Thus "ch" => {"c", "h"}
     * @param s the source string
     * @return a newly created set containing the given characters
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * Upon return this set will be empty if it did not contain c, or
     * will only contain c if it did contain c.
     * @param c the character to be retained
     * @return this object, for chaining
     * @stable ICU 2.0
     */
    public final UnicodeSet retain(int c) {
        return retain(c, c);
    }

    /**
     * Retain the specified string in this set if it is present.
     * Upon return this set will be empty if it did not contain s, or
     * will only contain s if it did contain s.
     * @param s the string to be retained
     * @return this object, for chaining
     * @stable ICU 2.0
     */
    public final UnicodeSet retain(String s) {
        int cp = getSingleCP(s);
        if (cp < 0) {
            boolean isIn = strings.contains(s);
            if (isIn && size() == 1) {
                return this;
            }
            clear();
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
     * @stable ICU 2.0
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
     * @param c the character to be removed
     * @return this object, for chaining
     * @stable ICU 2.0
     */
    public final UnicodeSet remove(int c) {
        return remove(c, c);
    }

    /**
     * Removes the specified string from this set if it is present.
     * The set will not contain the specified string once the call
     * returns.
     * @param s the string to be removed
     * @return this object, for chaining
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public final UnicodeSet complement(int c) {
        return complement(c, c);
    }

    /**
     * This is equivalent to
     * <code>complement(MIN_VALUE, MAX_VALUE)</code>.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
        /* Examples:
                                           findCodePoint(c)
           set              list[]         c=0 1 3 4 7 8
           ===              ==============   ===========
           []               [110000]         0 0 0 0 0 0
           [\u0000-\u0003]  [0, 4, 110000]   1 1 1 2 2 2
           [\u0004-\u0007]  [4, 8, 110000]   0 0 0 1 1 2
           [:all:]          [0, 110000]      1 1 1 1 1 1
         */

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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
        if (!SortedSetRelation.hasRelation(strings, SortedSetRelation.DISJOINT, c.strings)) return false;
        return true;
    }

    /**
     * Returns true if this set contains none of the characters
     * of the given string.
     * @param s string containing characters to be checked for containment
     * @return true if the test condition is met
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public final boolean containsSome(int start, int end) {
        return !containsNone(start, end);
    }

    /**
     * Returns true if this set contains one or more of the characters
     * and strings of the given set.
     * @param s set to be checked for containment
     * @return true if the condition is met
     * @stable ICU 2.0
     */
    public final boolean containsSome(UnicodeSet s) {
        return !containsNone(s);
    }

    /**
     * Returns true if this set contains one or more of the characters
     * of the given string.
     * @param s string containing characters to be checked for containment
     * @return true if the condition is met
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public UnicodeSet complementAll(UnicodeSet c) {
        xor(c.list, c.len, 0);
        SortedSetRelation.doOperation(strings, SortedSetRelation.COMPLEMENTALL, c.strings);
        return this;
    }

    /**
     * Removes all of the elements from this set.  This set will be
     * empty after this call returns.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @stable ICU 2.0
     */
    public int getRangeEnd(int index) {
        return (list[index*2 + 1] - 1);
    }

    /**
     * Reallocate this objects internal structures to take up the least
     * possible space, without changing this object's value.
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
     * @see java.lang.Object#hashCode()
     * @stable ICU 2.0
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
     * @stable ICU 2.0
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
    UnicodeSet applyPattern(String pattern,
                      ParsePosition pos,
                      SymbolTable symbols,
                      int options) {

        // Need to build the pattern in a temporary string because
        // _applyPattern calls add() etc., which set pat to empty.
        boolean parsePositionWasNull = pos == null;
        if (parsePositionWasNull) {
            pos = new ParsePosition(0);
        }

        StringBuffer rebuiltPat = new StringBuffer();
        RuleCharacterIterator chars =
            new RuleCharacterIterator(pattern, symbols, pos);
        applyPattern(chars, symbols, rebuiltPat, options);
        if (chars.inVariable()) {
            syntaxError(chars, "Extra chars in variable value");
        }
        pat = rebuiltPat.toString();
        if (parsePositionWasNull) {
            int i = pos.getIndex();

            // Skip over trailing whitespace
            if ((options & IGNORE_SPACE) != 0) {
                i = Utility.skipWhitespace(pattern, i);
            }

            if (i != pattern.length()) {
                throw new IllegalArgumentException("Parse of \"" + pattern +
                                                   "\" failed at " + i);
            }
        }
        return this;
    }

    /**
     * Parse the pattern from the given RuleCharacterIterator.  The
     * iterator is advanced over the parsed pattern.
     * @param chars iterator over the pattern characters.  Upon return
     * it will be advanced to the first character after the parsed
     * pattern, or the end of the iteration if all characters are
     * parsed.
     * @param symbols symbol table to use to parse and dereference
     * variables, or null if none.
     * @param rebuiltPat the pattern that was parsed, rebuilt or
     * copied from the input pattern, as appropriate.
     * @param options a bit mask of zero or more of the following:
     * IGNORE_SPACE, CASE.
     */
    void applyPattern(RuleCharacterIterator chars, SymbolTable symbols,
                      StringBuffer rebuiltPat, int options) {

        // Syntax characters: [ ] ^ - & { }

        // Recognized special forms for chars, sets: c-c s-s s&s

        int opts = RuleCharacterIterator.PARSE_VARIABLES |
                   RuleCharacterIterator.PARSE_ESCAPES;
        if ((options & IGNORE_SPACE) != 0) {
            opts |= RuleCharacterIterator.SKIP_WHITESPACE;
        }

        StringBuffer pat = new StringBuffer(), buf = null;
        boolean usePat = false;
        UnicodeSet scratch = null;
        Object backup = null;

        // mode: 0=before [, 1=between [...], 2=after ]
        // lastItem: 0=none, 1=char, 2=set
        int lastItem = 0, lastChar = 0, mode = 0;
        char op = 0;

        boolean invert = false;

        clear();

        while (mode != 2 && !chars.atEnd()) {
            if (false) {
                // Debugging assertion
                if (!((lastItem == 0 && op == 0) ||
                      (lastItem == 1 && (op == 0 || op == '-')) ||
                      (lastItem == 2 && (op == 0 || op == '-' || op == '&')))) {
                    throw new IllegalArgumentException();
                }
            }

            int c = 0;
            boolean literal = false;
            UnicodeSet nested = null;

            // -------- Check for property pattern

            // setMode: 0=none, 1=unicodeset, 2=propertypat, 3=preparsed
            int setMode = 0;
            if (resemblesPropertyPattern(chars, opts)) {
                setMode = 2;
            }

            // -------- Parse '[' of opening delimiter OR nested set.
            // If there is a nested set, use `setMode' to define how
            // the set should be parsed.  If the '[' is part of the
            // opening delimiter for this pattern, parse special
            // strings "[", "[^", "[-", and "[^-".  Check for stand-in
            // characters representing a nested set in the symbol
            // table.

            else {
                // Prepare to backup if necessary
                backup = chars.getPos(backup);
                c = chars.next(opts);
                literal = chars.isEscaped();

                if (c == '[' && !literal) {
                    if (mode == 1) {
                        chars.setPos(backup); // backup
                        setMode = 1;
                    } else {
                        // Handle opening '[' delimiter
                        mode = 1;
                        pat.append('[');
                        backup = chars.getPos(backup); // prepare to backup
                        c = chars.next(opts);
                        literal = chars.isEscaped();
                        if (c == '^' && !literal) {
                            invert = true;
                            pat.append('^');
                            backup = chars.getPos(backup); // prepare to backup
                            c = chars.next(opts);
                            literal = chars.isEscaped();
                        }
                        // Fall through to handle special leading '-';
                        // otherwise restart loop for nested [], \p{}, etc.
                        if (c == '-') {
                            literal = true;
                            // Fall through to handle literal '-' below
                        } else {
                            chars.setPos(backup); // backup
                            continue;
                        }
                    }
                } else if (symbols != null) {
                     UnicodeMatcher m = symbols.lookupMatcher(c); // may be null
                     if (m != null) {
                         try {
                             nested = (UnicodeSet) m;
                             setMode = 3;
                         } catch (ClassCastException e) {
                             syntaxError(chars, "Syntax error");
                         }
                     }
                }
            }

            // -------- Handle a nested set.  This either is inline in
            // the pattern or represented by a stand-in that has
            // previously been parsed and was looked up in the symbol
            // table.

            if (setMode != 0) {
                if (lastItem == 1) {
                    if (op != 0) {
                        syntaxError(chars, "Char expected after operator");
                    }
                    add(lastChar, lastChar);
                    _appendToPat(pat, lastChar, false);
                    lastItem = op = 0;
                }

                if (op == '-' || op == '&') {
                    pat.append(op);
                }

                if (nested == null) {
                    if (scratch == null) scratch = new UnicodeSet();
                    nested = scratch;
                }
                switch (setMode) {
                case 1:
                    nested.applyPattern(chars, symbols, pat, options);
                    break;
                case 2:
                    chars.skipIgnored(opts);
                    nested.applyPropertyPattern(chars, pat, symbols);
                    break;
                case 3: // `nested' already parsed
                    nested._toPattern(pat, false);
                    break;
                }

                usePat = true;

                if (mode == 0) {
                    // Entire pattern is a category; leave parse loop
                    set(nested);
                    mode = 2;
                    break;
                }

                switch (op) {
                case '-':
                    removeAll(nested);
                    break;
                case '&':
                    retainAll(nested);
                    break;
                case 0:
                    addAll(nested);
                    break;
                }

                op = 0;
                lastItem = 2;

                continue;
            }

            if (mode == 0) {
                syntaxError(chars, "Missing '['");
            }

            // -------- Parse special (syntax) characters.  If the
            // current character is not special, or if it is escaped,
            // then fall through and handle it below.

            if (!literal) {
                switch (c) {
                case ']':
                    if (lastItem == 1) {
                        add(lastChar, lastChar);
                        _appendToPat(pat, lastChar, false);
                    }
                    // Treat final trailing '-' as a literal
                    if (op == '-') {
                        add(op, op);
                        pat.append(op);
                    } else if (op == '&') {
                        syntaxError(chars, "Trailing '&'");
                    }
                    pat.append(']');
                    mode = 2;
                    continue;
                case '-':
                    if (op == 0) {
                        if (lastItem != 0) {
                            op = (char) c;
                            continue;
                        } else {
                            // Treat final trailing '-' as a literal
                            add(c, c);
                            c = chars.next(opts);
                            literal = chars.isEscaped();
                            if (c == ']' && !literal) {
                                pat.append("-]");
                                mode = 2;
                                continue;
                            }
                        }
                    }
                    syntaxError(chars, "'-' not after char or set");
                case '&':
                    if (lastItem == 2 && op == 0) {
                        op = (char) c;
                        continue;
                    }
                    syntaxError(chars, "'&' not after set");
                case '^':
                    syntaxError(chars, "'^' not after '['");
                case '{':
                    if (op != 0) {
                        syntaxError(chars, "Missing operand after operator");
                    }
                    if (lastItem == 1) {
                        add(lastChar, lastChar);
                        _appendToPat(pat, lastChar, false);
                    }
                    lastItem = 0;
                    if (buf == null) {
                        buf = new StringBuffer();
                    } else {
                        buf.setLength(0);
                    }
                    boolean ok = false;
                    while (!chars.atEnd()) {
                        c = chars.next(opts);
                        literal = chars.isEscaped();
                        if (c == '}' && !literal) {
                            ok = true;
                            break;
                        }
                        UTF16.append(buf, c);
                    }
                    if (buf.length() < 1 || !ok) {
                        syntaxError(chars, "Invalid multicharacter string");
                    }
                    // We have new string. Add it to set and continue;
                    // we don't need to drop through to the further
                    // processing
                    add(buf.toString());
                    pat.append('{');
                    _appendToPat(pat, buf.toString(), false);
                    pat.append('}');
                    continue;
                case SymbolTable.SYMBOL_REF:
                    //         symbols  nosymbols
                    // [a-$]   error    error (ambiguous)
                    // [a$]    anchor   anchor
                    // [a-$x]  var "x"* literal '$'
                    // [a-$.]  error    literal '$'
                    // *We won't get here in the case of var "x"
                    backup = chars.getPos(backup);
                    c = chars.next(opts);
                    literal = chars.isEscaped();
                    boolean anchor = (c == ']' && !literal);
                    if (symbols == null && !anchor) {
                        c = SymbolTable.SYMBOL_REF;
                        chars.setPos(backup);
                        break; // literal '$'
                    }
                    if (anchor && op == 0) {
                        if (lastItem == 1) {
                            add(lastChar, lastChar);
                            _appendToPat(pat, lastChar, false);
                        }
                        add(UnicodeMatcher.ETHER);
                        usePat = true;
                        pat.append(SymbolTable.SYMBOL_REF).append(']');
                        mode = 2;
                        continue;
                    }
                    syntaxError(chars, "Unquoted '$'");
                default:
                    break;
                }
            }

            // -------- Parse literal characters.  This includes both
            // escaped chars ("\u4E01") and non-syntax characters
            // ("a").

            switch (lastItem) {
            case 0:
                lastItem = 1;
                lastChar = c;
                break;
            case 1:
                if (op == '-') {
                    if (lastChar >= c) {
                        // Don't allow redundant (a-a) or empty (b-a) ranges;
                        // these are most likely typos.
                        syntaxError(chars, "Invalid range");
                    }
                    add(lastChar, c);
                    _appendToPat(pat, lastChar, false);
                    pat.append(op);
                    _appendToPat(pat, c, false);
                    lastItem = op = 0;
                } else {
                    add(lastChar, lastChar);
                    _appendToPat(pat, lastChar, false);
                    lastChar = c;
                }
                break;
            case 2:
                if (op != 0) {
                    syntaxError(chars, "Set expected after operator");
                }
                lastChar = c;
                lastItem = 1;
                break;
            }
        }

        if (mode != 2) {
            syntaxError(chars, "Missing ']'");
        }

        chars.skipIgnored(opts);

        /**
         * Handle global flags (invert, case insensitivity).  If this
         * pattern should be compiled case-insensitive, then we need
         * to close over case BEFORE COMPLEMENTING.  This makes
         * patterns like /[^abc]/i work.
         */
        if ((options & CASE) != 0) {
            closeOver(CASE);
        }
        if (invert) {
            complement();
        }

        // Use the rebuilt pattern (pat) only if necessary.  Prefer the
        // generated pattern.
        if (usePat) {
            rebuiltPat.append(pat.toString());
        } else {
            _generatePattern(rebuiltPat, false);
        }
    }

    private static void syntaxError(RuleCharacterIterator chars, String msg) {
        throw new IllegalArgumentException("Error: " + msg + " at \"" +
                                           Utility.escape(chars.toString()) +
                                           '"');
    }

    /**
     * Add the contents of the UnicodeSet (as strings) into a collection.
     * @param target collection to add into
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public void addAllTo(Collection target) {
        UnicodeSetIterator it = new UnicodeSetIterator(this);
        while (it.next()) {
            target.add(it.getString());
        }
    }

    /**
     * Add the contents of the collection (as strings) into this UnicodeSet.
     * @param source the collection to add
     * @draft ICU 2.8
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public void addAll(Collection source) {
        Iterator it = source.iterator();
        while (it.hasNext()) {
            add(it.next().toString());
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

    private static class GeneralCategoryMaskFilter implements Filter {
        int mask;
        GeneralCategoryMaskFilter(int mask) { this.mask = mask; }
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

    // VersionInfo for unassigned characters
    static final VersionInfo NO_VERSION = VersionInfo.getInstance(0, 0, 0, 0);

    private static class VersionFilter implements Filter {
        VersionInfo version;
        VersionFilter(VersionInfo version) { this.version = version; }
        public boolean contains(int ch) {
            VersionInfo v = UCharacter.getAge(ch);
            // Reference comparison ok; VersionInfo caches and reuses
            // unique objects.
            return v != NO_VERSION &&
                   v.compareTo(version) <= 0;
        }
    }

    private static synchronized UnicodeSet getInclusions() {
        if (INCLUSIONS == null) {
            UCharacterProperty property = UCharacterProperty.getInstance();
            INCLUSIONS = property.getInclusions();
        }
        return INCLUSIONS;
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
        UnicodeSet inclusions = getInclusions();
        int limitRange = inclusions.getRangeCount();

        for (int j=0; j<limitRange; ++j) {
            // get current range
            int start = inclusions.getRangeStart(j);
            int end = inclusions.getRangeEnd(j);

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
     * UProperty.INT_START..UProperty.INT_LIMIT-1 or.
     * UProperty.MASK_START..UProperty.MASK_LIMIT-1.
     *
     * @param value a value in the range
     * UCharacter.getIntPropertyMinValue(prop)..
     * UCharacter.getIntPropertyMaxValue(prop), with one exception.
     * If prop is UProperty.GENERAL_CATEGORY_MASK, then value should not be
     * a UCharacter.getType() result, but rather a mask value produced
     * by logically ORing (1 << UCharacter.getType()) values together.
     * This allows grouped categories such as [:L:] to be represented.
     *
     * @return a reference to this set
     *
     * @stable ICU 2.4
     */
    public UnicodeSet applyIntPropertyValue(int prop, int value) {
        if (prop == UProperty.GENERAL_CATEGORY_MASK) {
            applyFilter(new GeneralCategoryMaskFilter(value));
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
     * @stable ICU 2.4
     */
    public UnicodeSet applyPropertyAlias(String propertyAlias, String valueAlias) {
        return applyPropertyAlias(propertyAlias, valueAlias, null);
    }

    /**
     * Modifies this set to contain those code points which have the
     * given value for the given property.  Prior contents of this
     * set are lost.
     * @param propertyAlias
     * @param valueAlias
     * @param symbols if not null, then symbols are first called to see if a property
     * is available. If true, then everything else is skipped.
     * @return this set
     * @draft ICU 3.2
     * @deprecated This is a draft API and might change in a future release of ICU.
     */
    public UnicodeSet applyPropertyAlias(String propertyAlias,
                                         String valueAlias, SymbolTable symbols) {
        int p;
        int v;
        boolean mustNotBeEmpty = false;

        if (symbols != null
                && (symbols instanceof XSymbolTable)
                && ((XSymbolTable)symbols).applyPropertyAlias(propertyAlias, valueAlias, this)) {
                return this;
        }

        if (valueAlias.length() > 0) {
            p = UCharacter.getPropertyEnum(propertyAlias);

            // Treat gc as gcm
            if (p == UProperty.GENERAL_CATEGORY) {
                p = UProperty.GENERAL_CATEGORY_MASK;
            }

            if ((p >= UProperty.BINARY_START && p < UProperty.BINARY_LIMIT) ||
                (p >= UProperty.INT_START && p < UProperty.INT_LIMIT) ||
                (p >= UProperty.MASK_START && p < UProperty.MASK_LIMIT)) {
                try {
                    v = UCharacter.getPropertyValueEnum(p, valueAlias);
                } catch (IllegalArgumentException e) {
                    // Handle numeric CCC
                    if (p == UProperty.CANONICAL_COMBINING_CLASS ||
                        p == UProperty.LEAD_CANONICAL_COMBINING_CLASS ||
                        p == UProperty.TRAIL_CANONICAL_COMBINING_CLASS) {
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
                case UProperty.AGE:
                    {
                        // Must munge name, since
                        // VersionInfo.getInstance() does not do
                        // 'loose' matching.
                        VersionInfo version = VersionInfo.getInstance(mungeCharName(valueAlias));
                        applyFilter(new VersionFilter(version));
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
                p = UProperty.GENERAL_CATEGORY_MASK;
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
                            throw new IllegalArgumentException("Invalid property alias: " + propertyAlias + "=" + valueAlias);
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
     * Return true if the given iterator appears to point at a
     * property pattern.  Regardless of the result, return with the
     * iterator unchanged.
     * @param chars iterator over the pattern characters.  Upon return
     * it will be unchanged.
     * @param iterOpts RuleCharacterIterator options
     */
    private static boolean resemblesPropertyPattern(RuleCharacterIterator chars,
                                                    int iterOpts) {
        boolean result = false;
        iterOpts &= ~RuleCharacterIterator.PARSE_ESCAPES;
        Object pos = chars.getPos(null);
        int c = chars.next(iterOpts);
        if (c == '[' || c == '\\') {
            int d = chars.next(iterOpts & ~RuleCharacterIterator.SKIP_WHITESPACE);
            result = (c == '[') ? (d == ':') :
                     (d == 'N' || d == 'p' || d == 'P');
        }
        chars.setPos(pos);
        return result;
    }

    /**
     * Parse the given property pattern at the given parse position.
     * @param symbols TODO
     */
    private UnicodeSet applyPropertyPattern(String pattern, ParsePosition ppos, SymbolTable symbols) {
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

        applyPropertyAlias(propName, valueName, symbols);

        if (invert) {
            complement();
        }

        // Move to the limit position after the close delimiter
        ppos.setIndex(close + (posix ? 2 : 1));

        return this;
    }

    /**
     * Parse a property pattern.
     * @param chars iterator over the pattern characters.  Upon return
     * it will be advanced to the first character after the parsed
     * pattern, or the end of the iteration if all characters are
     * parsed.
     * @param rebuiltPat the pattern that was parsed, rebuilt or
     * copied from the input pattern, as appropriate.
     * @param symbols TODO
     */
    private void applyPropertyPattern(RuleCharacterIterator chars,
                                      StringBuffer rebuiltPat, SymbolTable symbols) {
        String pat = chars.lookahead();
        ParsePosition pos = new ParsePosition(0);
        applyPropertyPattern(pat, pos, symbols);
        if (pos.getIndex() == 0) {
            syntaxError(chars, "Invalid property pattern");
        }
        chars.jumpahead(pos.getIndex());
        rebuiltPat.append(pat.substring(0, pos.getIndex()));
    }

    //----------------------------------------------------------------
    // Case folding API
    //----------------------------------------------------------------

    // NOTE: The closeOver API, originally slated for 2.6, was
    // withdrawn to allow for modifications under discussion.

    /**
     * Bitmask for constructor and applyPattern() indicating that
     * white space should be ignored.  If set, ignore characters for
     * which UCharacterProperty.isRuleWhiteSpace() returns true,
     * unless they are quoted or escaped.  This may be ORed together
     * with other selectors.
     * @internal
     */
    public static final int IGNORE_SPACE = 1;

    /**
     * Bitmask for constructor, applyPattern(), and closeOver()
     * indicating letter case.  This may be ORed together with other
     * selectors.
     * @internal
     */
    public static final int CASE = 2;

    /**
     * Close this set over the given attribute.  For the attribute
     * CASE, the result is to modify this set so that:
     *
     * 1. For each character or string 'a' in this set, all strings
     * 'b' such that foldCase(a) == foldCase(b) are added to this set.
     * (For most 'a' that are single characters, 'b' will have
     * b.length() == 1.)
     *
     * 2. For each string 'e' in the resulting set, if e !=
     * foldCase(e), 'e' will be removed.
     *
     * Example: [aq\u00DF{Bc}{bC}{Fi}] => [aAqQ\u00DF\uFB01{ss}{bc}{fi}]
     *
     * (Here foldCase(x) refers to the operation
     * UCharacter.foldCase(x, true), and a == b actually denotes
     * a.equals(b), not pointer comparison.)
     *
     * @param attribute bitmask for attributes to close over.
     * Currently only the CASE bit is supported.  Any undefined bits
     * are ignored.
     * @return a reference to this set.
     * @internal
     */
    public UnicodeSet closeOver(int attribute) {
        if ((attribute & CASE) != 0) {
            UnicodeSet foldSet = new UnicodeSet();
            int n = getRangeCount();
            for (int i=0; i<n; ++i) {
                int start = getRangeStart(i);
                int end   = getRangeEnd(i);
                for (int cp=start; cp<=end; ++cp) {
                    foldSet.caseCloseOne(UTF16.valueOf(cp));
                }
            }
            if (strings.size() > 0) {
                Iterator it = strings.iterator();
                while (it.hasNext()) {
                    foldSet.caseCloseOne(
                      UCharacter.foldCase((String) it.next(), DEFAULT_CASE_MAP));
                }
            }
            set(foldSet);
        }
        return this;
    }

    //----------------------------------------------------------------
    // Case folding implementation
    //----------------------------------------------------------------

    /**
     * Add to this set all members of the case fold equivalency class
     * that contains 'folded'.
     * @param folded a string within a case fold equivalency class.
     * It must have the property that UCharacter.foldCase(folded,
     * DEFAULT_CASE_MAP).equals(folded).
     */
    private void caseCloseOne(String folded) {
        // Look up equivalency class
        String[] equiv = (String[]) CASE_EQUIV_CLASS.get(folded);
        if (equiv == null) {
            // If there is no entry then the equivalency class
            // contains only 'folded' itself.
            add(folded);
        } else {
            for (int i=0; i<equiv.length; ++i) {
                add(equiv[i]);
            }
        }
    }

    /**
     * Map from string to case folding equivalence class.
     *
     * A case folding equivalence class contains all strings (single
     * characters are represented as strings of length 1) which map to
     * the same value via UCharacter.foldCase(x, DEFAULT_CASE_MAP).
     * Note that for multicharacter strings, only the 'identity'
     * variant is included {the variant x for which
     * UCharacter.foldCase(x, DEFAULT_CASE_MAP).equals(x)}.  For
     * example, in the equivalence class { \uFB01, fi }, the entries {
     * Fi, fI, FI } are not included.
     *
     * Given an equivalence class, each member of the class is a key
     * in this map.  They all map to the same Java reference v, which
     * is a String[] array.  For example,
     *
     * MAP.get("\uFB01") == MAP.get("fi") == String[] { "fi", "\uFB01" }
     *
     * Note that == here is a reference comparison, not an equals()
     * comparison.  This reduces memory requirements.
     */
    private static Map CASE_EQUIV_CLASS = null;

    private static final boolean DEFAULT_CASE_MAP = true; // false for Turkish

    // Machine-generated case fold equivalency class data.  To
    // regenerate (when the Unicode database changes, or when
    // DEFAULT_CASE_MAP is changed), set CASE_GENERATE to true and
    // load this class.  The new data will be emitted to System.out.

    // MACHINE-GENERATED: Do not edit
    private static final String CASE_PAIRS =
        "AaBbCcDdEeFfGgHhIiJjLlMmNnOoPpQqRrTtUuVvWwXxYyZz\u00C0\u00E0\u00C1\u00E1"+
        "\u00C2\u00E2\u00C3\u00E3\u00C4\u00E4\u00C6\u00E6\u00C7\u00E7\u00C8\u00E8"+
        "\u00C9\u00E9\u00CA\u00EA\u00CB\u00EB\u00CC\u00EC\u00CD\u00ED\u00CE\u00EE"+
        "\u00CF\u00EF\u00D0\u00F0\u00D1\u00F1\u00D2\u00F2\u00D3\u00F3\u00D4\u00F4"+
        "\u00D5\u00F5\u00D6\u00F6\u00D8\u00F8\u00D9\u00F9\u00DA\u00FA\u00DB\u00FB"+
        "\u00DC\u00FC\u00DD\u00FD\u00DE\u00FE\u00FF\u0178\u0100\u0101\u0102\u0103"+
        "\u0104\u0105\u0106\u0107\u0108\u0109\u010A\u010B\u010C\u010D\u010E\u010F"+
        "\u0110\u0111\u0112\u0113\u0114\u0115\u0116\u0117\u0118\u0119\u011A\u011B"+
        "\u011C\u011D\u011E\u011F\u0120\u0121\u0122\u0123\u0124\u0125\u0126\u0127"+
        "\u0128\u0129\u012A\u012B\u012C\u012D\u012E\u012F\u0132\u0133\u0134\u0135"+
        "\u0136\u0137\u0139\u013A\u013B\u013C\u013D\u013E\u013F\u0140\u0141\u0142"+
        "\u0143\u0144\u0145\u0146\u0147\u0148\u014A\u014B\u014C\u014D\u014E\u014F"+
        "\u0150\u0151\u0152\u0153\u0154\u0155\u0156\u0157\u0158\u0159\u015A\u015B"+
        "\u015C\u015D\u015E\u015F\u0160\u0161\u0162\u0163\u0164\u0165\u0166\u0167"+
        "\u0168\u0169\u016A\u016B\u016C\u016D\u016E\u016F\u0170\u0171\u0172\u0173"+
        "\u0174\u0175\u0176\u0177\u0179\u017A\u017B\u017C\u017D\u017E\u0182\u0183"+
        "\u0184\u0185\u0187\u0188\u018B\u018C\u0191\u0192\u0195\u01F6\u0198\u0199"+
        "\u019E\u0220\u01A0\u01A1\u01A2\u01A3\u01A4\u01A5\u01A7\u01A8\u01AC\u01AD"+
        "\u01AF\u01B0\u01B3\u01B4\u01B5\u01B6\u01B8\u01B9\u01BC\u01BD\u01BF\u01F7"+
        "\u01CD\u01CE\u01CF\u01D0\u01D1\u01D2\u01D3\u01D4\u01D5\u01D6\u01D7\u01D8"+
        "\u01D9\u01DA\u01DB\u01DC\u018E\u01DD\u01DE\u01DF\u01E0\u01E1\u01E2\u01E3"+
        "\u01E4\u01E5\u01E6\u01E7\u01E8\u01E9\u01EA\u01EB\u01EC\u01ED\u01EE\u01EF"+
        "\u01F4\u01F5\u01F8\u01F9\u01FA\u01FB\u01FC\u01FD\u01FE\u01FF\u0200\u0201"+
        "\u0202\u0203\u0204\u0205\u0206\u0207\u0208\u0209\u020A\u020B\u020C\u020D"+
        "\u020E\u020F\u0210\u0211\u0212\u0213\u0214\u0215\u0216\u0217\u0218\u0219"+
        "\u021A\u021B\u021C\u021D\u021E\u021F\u0222\u0223\u0224\u0225\u0226\u0227"+
        "\u0228\u0229\u022A\u022B\u022C\u022D\u022E\u022F\u0230\u0231\u0232\u0233"+
        "\u0181\u0253\u0186\u0254\u0189\u0256\u018A\u0257\u018F\u0259\u0190\u025B"+
        "\u0193\u0260\u0194\u0263\u0197\u0268\u0196\u0269\u019C\u026F\u019D\u0272"+
        "\u019F\u0275\u01A6\u0280\u01A9\u0283\u01AE\u0288\u01B1\u028A\u01B2\u028B"+
        "\u01B7\u0292\u0386\u03AC\u0388\u03AD\u0389\u03AE\u038A\u03AF\u0391\u03B1"+
        "\u0393\u03B3\u0394\u03B4\u0396\u03B6\u0397\u03B7\u039B\u03BB\u039D\u03BD"+
        "\u039E\u03BE\u039F\u03BF\u03A4\u03C4\u03A5\u03C5\u03A7\u03C7\u03A8\u03C8"+
        "\u03AA\u03CA\u03AB\u03CB\u038C\u03CC\u038E\u03CD\u038F\u03CE\u03D8\u03D9"+
        "\u03DA\u03DB\u03DC\u03DD\u03DE\u03DF\u03E0\u03E1\u03E2\u03E3\u03E4\u03E5"+
        "\u03E6\u03E7\u03E8\u03E9\u03EA\u03EB\u03EC\u03ED\u03EE\u03EF\u0410\u0430"+
        "\u0411\u0431\u0412\u0432\u0413\u0433\u0414\u0434\u0415\u0435\u0416\u0436"+
        "\u0417\u0437\u0418\u0438\u0419\u0439\u041A\u043A\u041B\u043B\u041C\u043C"+
        "\u041D\u043D\u041E\u043E\u041F\u043F\u0420\u0440\u0421\u0441\u0422\u0442"+
        "\u0423\u0443\u0424\u0444\u0425\u0445\u0426\u0446\u0427\u0447\u0428\u0448"+
        "\u0429\u0449\u042A\u044A\u042B\u044B\u042C\u044C\u042D\u044D\u042E\u044E"+
        "\u042F\u044F\u0400\u0450\u0401\u0451\u0402\u0452\u0403\u0453\u0404\u0454"+
        "\u0405\u0455\u0406\u0456\u0407\u0457\u0408\u0458\u0409\u0459\u040A\u045A"+
        "\u040B\u045B\u040C\u045C\u040D\u045D\u040E\u045E\u040F\u045F\u0460\u0461"+
        "\u0462\u0463\u0464\u0465\u0466\u0467\u0468\u0469\u046A\u046B\u046C\u046D"+
        "\u046E\u046F\u0470\u0471\u0472\u0473\u0474\u0475\u0476\u0477\u0478\u0479"+
        "\u047A\u047B\u047C\u047D\u047E\u047F\u0480\u0481\u048A\u048B\u048C\u048D"+
        "\u048E\u048F\u0490\u0491\u0492\u0493\u0494\u0495\u0496\u0497\u0498\u0499"+
        "\u049A\u049B\u049C\u049D\u049E\u049F\u04A0\u04A1\u04A2\u04A3\u04A4\u04A5"+
        "\u04A6\u04A7\u04A8\u04A9\u04AA\u04AB\u04AC\u04AD\u04AE\u04AF\u04B0\u04B1"+
        "\u04B2\u04B3\u04B4\u04B5\u04B6\u04B7\u04B8\u04B9\u04BA\u04BB\u04BC\u04BD"+
        "\u04BE\u04BF\u04C1\u04C2\u04C3\u04C4\u04C5\u04C6\u04C7\u04C8\u04C9\u04CA"+
        "\u04CB\u04CC\u04CD\u04CE\u04D0\u04D1\u04D2\u04D3\u04D4\u04D5\u04D6\u04D7"+
        "\u04D8\u04D9\u04DA\u04DB\u04DC\u04DD\u04DE\u04DF\u04E0\u04E1\u04E2\u04E3"+
        "\u04E4\u04E5\u04E6\u04E7\u04E8\u04E9\u04EA\u04EB\u04EC\u04ED\u04EE\u04EF"+
        "\u04F0\u04F1\u04F2\u04F3\u04F4\u04F5\u04F8\u04F9\u0500\u0501\u0502\u0503"+
        "\u0504\u0505\u0506\u0507\u0508\u0509\u050A\u050B\u050C\u050D\u050E\u050F"+
        "\u0531\u0561\u0532\u0562\u0533\u0563\u0534\u0564\u0535\u0565\u0536\u0566"+
        "\u0537\u0567\u0538\u0568\u0539\u0569\u053A\u056A\u053B\u056B\u053C\u056C"+
        "\u053D\u056D\u053E\u056E\u053F\u056F\u0540\u0570\u0541\u0571\u0542\u0572"+
        "\u0543\u0573\u0544\u0574\u0545\u0575\u0546\u0576\u0547\u0577\u0548\u0578"+
        "\u0549\u0579\u054A\u057A\u054B\u057B\u054C\u057C\u054D\u057D\u054E\u057E"+
        "\u054F\u057F\u0550\u0580\u0551\u0581\u0552\u0582\u0553\u0583\u0554\u0584"+
        "\u0555\u0585\u0556\u0586\u1E00\u1E01\u1E02\u1E03\u1E04\u1E05\u1E06\u1E07"+
        "\u1E08\u1E09\u1E0A\u1E0B\u1E0C\u1E0D\u1E0E\u1E0F\u1E10\u1E11\u1E12\u1E13"+
        "\u1E14\u1E15\u1E16\u1E17\u1E18\u1E19\u1E1A\u1E1B\u1E1C\u1E1D\u1E1E\u1E1F"+
        "\u1E20\u1E21\u1E22\u1E23\u1E24\u1E25\u1E26\u1E27\u1E28\u1E29\u1E2A\u1E2B"+
        "\u1E2C\u1E2D\u1E2E\u1E2F\u1E30\u1E31\u1E32\u1E33\u1E34\u1E35\u1E36\u1E37"+
        "\u1E38\u1E39\u1E3A\u1E3B\u1E3C\u1E3D\u1E3E\u1E3F\u1E40\u1E41\u1E42\u1E43"+
        "\u1E44\u1E45\u1E46\u1E47\u1E48\u1E49\u1E4A\u1E4B\u1E4C\u1E4D\u1E4E\u1E4F"+
        "\u1E50\u1E51\u1E52\u1E53\u1E54\u1E55\u1E56\u1E57\u1E58\u1E59\u1E5A\u1E5B"+
        "\u1E5C\u1E5D\u1E5E\u1E5F\u1E62\u1E63\u1E64\u1E65\u1E66\u1E67\u1E68\u1E69"+
        "\u1E6A\u1E6B\u1E6C\u1E6D\u1E6E\u1E6F\u1E70\u1E71\u1E72\u1E73\u1E74\u1E75"+
        "\u1E76\u1E77\u1E78\u1E79\u1E7A\u1E7B\u1E7C\u1E7D\u1E7E\u1E7F\u1E80\u1E81"+
        "\u1E82\u1E83\u1E84\u1E85\u1E86\u1E87\u1E88\u1E89\u1E8A\u1E8B\u1E8C\u1E8D"+
        "\u1E8E\u1E8F\u1E90\u1E91\u1E92\u1E93\u1E94\u1E95\u1EA0\u1EA1\u1EA2\u1EA3"+
        "\u1EA4\u1EA5\u1EA6\u1EA7\u1EA8\u1EA9\u1EAA\u1EAB\u1EAC\u1EAD\u1EAE\u1EAF"+
        "\u1EB0\u1EB1\u1EB2\u1EB3\u1EB4\u1EB5\u1EB6\u1EB7\u1EB8\u1EB9\u1EBA\u1EBB"+
        "\u1EBC\u1EBD\u1EBE\u1EBF\u1EC0\u1EC1\u1EC2\u1EC3\u1EC4\u1EC5\u1EC6\u1EC7"+
        "\u1EC8\u1EC9\u1ECA\u1ECB\u1ECC\u1ECD\u1ECE\u1ECF\u1ED0\u1ED1\u1ED2\u1ED3"+
        "\u1ED4\u1ED5\u1ED6\u1ED7\u1ED8\u1ED9\u1EDA\u1EDB\u1EDC\u1EDD\u1EDE\u1EDF"+
        "\u1EE0\u1EE1\u1EE2\u1EE3\u1EE4\u1EE5\u1EE6\u1EE7\u1EE8\u1EE9\u1EEA\u1EEB"+
        "\u1EEC\u1EED\u1EEE\u1EEF\u1EF0\u1EF1\u1EF2\u1EF3\u1EF4\u1EF5\u1EF6\u1EF7"+
        "\u1EF8\u1EF9\u1F00\u1F08\u1F01\u1F09\u1F02\u1F0A\u1F03\u1F0B\u1F04\u1F0C"+
        "\u1F05\u1F0D\u1F06\u1F0E\u1F07\u1F0F\u1F10\u1F18\u1F11\u1F19\u1F12\u1F1A"+
        "\u1F13\u1F1B\u1F14\u1F1C\u1F15\u1F1D\u1F20\u1F28\u1F21\u1F29\u1F22\u1F2A"+
        "\u1F23\u1F2B\u1F24\u1F2C\u1F25\u1F2D\u1F26\u1F2E\u1F27\u1F2F\u1F30\u1F38"+
        "\u1F31\u1F39\u1F32\u1F3A\u1F33\u1F3B\u1F34\u1F3C\u1F35\u1F3D\u1F36\u1F3E"+
        "\u1F37\u1F3F\u1F40\u1F48\u1F41\u1F49\u1F42\u1F4A\u1F43\u1F4B\u1F44\u1F4C"+
        "\u1F45\u1F4D\u1F51\u1F59\u1F53\u1F5B\u1F55\u1F5D\u1F57\u1F5F\u1F60\u1F68"+
        "\u1F61\u1F69\u1F62\u1F6A\u1F63\u1F6B\u1F64\u1F6C\u1F65\u1F6D\u1F66\u1F6E"+
        "\u1F67\u1F6F\u1F70\u1FBA\u1F71\u1FBB\u1F72\u1FC8\u1F73\u1FC9\u1F74\u1FCA"+
        "\u1F75\u1FCB\u1F76\u1FDA\u1F77\u1FDB\u1F78\u1FF8\u1F79\u1FF9\u1F7A\u1FEA"+
        "\u1F7B\u1FEB\u1F7C\u1FFA\u1F7D\u1FFB\u1FB0\u1FB8\u1FB1\u1FB9\u1FD0\u1FD8"+
        "\u1FD1\u1FD9\u1FE0\u1FE8\u1FE1\u1FE9\u1FE5\u1FEC\u2160\u2170\u2161\u2171"+
        "\u2162\u2172\u2163\u2173\u2164\u2174\u2165\u2175\u2166\u2176\u2167\u2177"+
        "\u2168\u2178\u2169\u2179\u216A\u217A\u216B\u217B\u216C\u217C\u216D\u217D"+
        "\u216E\u217E\u216F\u217F\u24B6\u24D0\u24B7\u24D1\u24B8\u24D2\u24B9\u24D3"+
        "\u24BA\u24D4\u24BB\u24D5\u24BC\u24D6\u24BD\u24D7\u24BE\u24D8\u24BF\u24D9"+
        "\u24C0\u24DA\u24C1\u24DB\u24C2\u24DC\u24C3\u24DD\u24C4\u24DE\u24C5\u24DF"+
        "\u24C6\u24E0\u24C7\u24E1\u24C8\u24E2\u24C9\u24E3\u24CA\u24E4\u24CB\u24E5"+
        "\u24CC\u24E6\u24CD\u24E7\u24CE\u24E8\u24CF\u24E9\uFF21\uFF41\uFF22\uFF42"+
        "\uFF23\uFF43\uFF24\uFF44\uFF25\uFF45\uFF26\uFF46\uFF27\uFF47\uFF28\uFF48"+
        "\uFF29\uFF49\uFF2A\uFF4A\uFF2B\uFF4B\uFF2C\uFF4C\uFF2D\uFF4D\uFF2E\uFF4E"+
        "\uFF2F\uFF4F\uFF30\uFF50\uFF31\uFF51\uFF32\uFF52\uFF33\uFF53\uFF34\uFF54"+
        "\uFF35\uFF55\uFF36\uFF56\uFF37\uFF57\uFF38\uFF58\uFF39\uFF59\uFF3A\uFF5A";

    // MACHINE-GENERATED: Do not edit
    private static final String[][] CASE_NONPAIRS = {
        {"a\u02BE", "\u1E9A"},
        {"ff", "\uFB00"},
        {"ffi", "\uFB03"},
        {"ffl", "\uFB04"},
        {"fi", "\uFB01"},
        {"fl", "\uFB02"},
        {"h\u0331", "\u1E96"},
        {"i\u0307", "\u0130"},
        {"j\u030C", "\u01F0"},
        {"K", "k", "\u212A"},
        {"S", "s", "\u017F"},
        {"ss", "\u00DF"},
        {"st", "\uFB05", "\uFB06"},
        {"t\u0308", "\u1E97"},
        {"w\u030A", "\u1E98"},
        {"y\u030A", "\u1E99"},
        {"\u00C5", "\u00E5", "\u212B"},
        {"\u01C4", "\u01C5", "\u01C6"},
        {"\u01C7", "\u01C8", "\u01C9"},
        {"\u01CA", "\u01CB", "\u01CC"},
        {"\u01F1", "\u01F2", "\u01F3"},
        {"\u0149", "\u02BCn"},
        {"\u03AC\u03B9", "\u1FB4"},
        {"\u03AE\u03B9", "\u1FC4"},
        {"\u03B1\u0342", "\u1FB6"},
        {"\u03B1\u0342\u03B9", "\u1FB7"},
        {"\u03B1\u03B9", "\u1FB3", "\u1FBC"},
        {"\u0392", "\u03B2", "\u03D0"},
        {"\u0395", "\u03B5", "\u03F5"},
        {"\u03B7\u0342", "\u1FC6"},
        {"\u03B7\u0342\u03B9", "\u1FC7"},
        {"\u03B7\u03B9", "\u1FC3", "\u1FCC"},
        {"\u0398", "\u03B8", "\u03D1", "\u03F4"},
        {"\u0345", "\u0399", "\u03B9", "\u1FBE"},
        {"\u03B9\u0308\u0300", "\u1FD2"},
        {"\u0390", "\u03B9\u0308\u0301", "\u1FD3"},
        {"\u03B9\u0308\u0342", "\u1FD7"},
        {"\u03B9\u0342", "\u1FD6"},
        {"\u039A", "\u03BA", "\u03F0"},
        {"\u00B5", "\u039C", "\u03BC"},
        {"\u03A0", "\u03C0", "\u03D6"},
        {"\u03A1", "\u03C1", "\u03F1"},
        {"\u03C1\u0313", "\u1FE4"},
        {"\u03A3", "\u03C2", "\u03C3", "\u03F2"},
        {"\u03C5\u0308\u0300", "\u1FE2"},
        {"\u03B0", "\u03C5\u0308\u0301", "\u1FE3"},
        {"\u03C5\u0308\u0342", "\u1FE7"},
        {"\u03C5\u0313", "\u1F50"},
        {"\u03C5\u0313\u0300", "\u1F52"},
        {"\u03C5\u0313\u0301", "\u1F54"},
        {"\u03C5\u0313\u0342", "\u1F56"},
        {"\u03C5\u0342", "\u1FE6"},
        {"\u03A6", "\u03C6", "\u03D5"},
        {"\u03A9", "\u03C9", "\u2126"},
        {"\u03C9\u0342", "\u1FF6"},
        {"\u03C9\u0342\u03B9", "\u1FF7"},
        {"\u03C9\u03B9", "\u1FF3", "\u1FFC"},
        {"\u03CE\u03B9", "\u1FF4"},
        {"\u0565\u0582", "\u0587"},
        {"\u0574\u0565", "\uFB14"},
        {"\u0574\u056B", "\uFB15"},
        {"\u0574\u056D", "\uFB17"},
        {"\u0574\u0576", "\uFB13"},
        {"\u057E\u0576", "\uFB16"},
        {"\u1E60", "\u1E61", "\u1E9B"},
        {"\u1F00\u03B9", "\u1F80", "\u1F88"},
        {"\u1F01\u03B9", "\u1F81", "\u1F89"},
        {"\u1F02\u03B9", "\u1F82", "\u1F8A"},
        {"\u1F03\u03B9", "\u1F83", "\u1F8B"},
        {"\u1F04\u03B9", "\u1F84", "\u1F8C"},
        {"\u1F05\u03B9", "\u1F85", "\u1F8D"},
        {"\u1F06\u03B9", "\u1F86", "\u1F8E"},
        {"\u1F07\u03B9", "\u1F87", "\u1F8F"},
        {"\u1F20\u03B9", "\u1F90", "\u1F98"},
        {"\u1F21\u03B9", "\u1F91", "\u1F99"},
        {"\u1F22\u03B9", "\u1F92", "\u1F9A"},
        {"\u1F23\u03B9", "\u1F93", "\u1F9B"},
        {"\u1F24\u03B9", "\u1F94", "\u1F9C"},
        {"\u1F25\u03B9", "\u1F95", "\u1F9D"},
        {"\u1F26\u03B9", "\u1F96", "\u1F9E"},
        {"\u1F27\u03B9", "\u1F97", "\u1F9F"},
        {"\u1F60\u03B9", "\u1FA0", "\u1FA8"},
        {"\u1F61\u03B9", "\u1FA1", "\u1FA9"},
        {"\u1F62\u03B9", "\u1FA2", "\u1FAA"},
        {"\u1F63\u03B9", "\u1FA3", "\u1FAB"},
        {"\u1F64\u03B9", "\u1FA4", "\u1FAC"},
        {"\u1F65\u03B9", "\u1FA5", "\u1FAD"},
        {"\u1F66\u03B9", "\u1FA6", "\u1FAE"},
        {"\u1F67\u03B9", "\u1FA7", "\u1FAF"},
        {"\u1F70\u03B9", "\u1FB2"},
        {"\u1F74\u03B9", "\u1FC2"},
        {"\u1F7C\u03B9", "\u1FF2"},
        {"\uD801\uDC00", "\uD801\uDC28"},
        {"\uD801\uDC01", "\uD801\uDC29"},
        {"\uD801\uDC02", "\uD801\uDC2A"},
        {"\uD801\uDC03", "\uD801\uDC2B"},
        {"\uD801\uDC04", "\uD801\uDC2C"},
        {"\uD801\uDC05", "\uD801\uDC2D"},
        {"\uD801\uDC06", "\uD801\uDC2E"},
        {"\uD801\uDC07", "\uD801\uDC2F"},
        {"\uD801\uDC08", "\uD801\uDC30"},
        {"\uD801\uDC09", "\uD801\uDC31"},
        {"\uD801\uDC0A", "\uD801\uDC32"},
        {"\uD801\uDC0B", "\uD801\uDC33"},
        {"\uD801\uDC0C", "\uD801\uDC34"},
        {"\uD801\uDC0D", "\uD801\uDC35"},
        {"\uD801\uDC0E", "\uD801\uDC36"},
        {"\uD801\uDC0F", "\uD801\uDC37"},
        {"\uD801\uDC10", "\uD801\uDC38"},
        {"\uD801\uDC11", "\uD801\uDC39"},
        {"\uD801\uDC12", "\uD801\uDC3A"},
        {"\uD801\uDC13", "\uD801\uDC3B"},
        {"\uD801\uDC14", "\uD801\uDC3C"},
        {"\uD801\uDC15", "\uD801\uDC3D"},
        {"\uD801\uDC16", "\uD801\uDC3E"},
        {"\uD801\uDC17", "\uD801\uDC3F"},
        {"\uD801\uDC18", "\uD801\uDC40"},
        {"\uD801\uDC19", "\uD801\uDC41"},
        {"\uD801\uDC1A", "\uD801\uDC42"},
        {"\uD801\uDC1B", "\uD801\uDC43"},
        {"\uD801\uDC1C", "\uD801\uDC44"},
        {"\uD801\uDC1D", "\uD801\uDC45"},
        {"\uD801\uDC1E", "\uD801\uDC46"},
        {"\uD801\uDC1F", "\uD801\uDC47"},
        {"\uD801\uDC20", "\uD801\uDC48"},
        {"\uD801\uDC21", "\uD801\uDC49"},
        {"\uD801\uDC22", "\uD801\uDC4A"},
        {"\uD801\uDC23", "\uD801\uDC4B"},
        {"\uD801\uDC24", "\uD801\uDC4C"},
        {"\uD801\uDC25", "\uD801\uDC4D"},
    };

    static {
        // Create case-fold equivalency class map CASE_EQUIV_CLASS.

        // To regenerate the equivalency class data, see class
        // com.ibm.icu.dev.tools.translit.UnicodeSetCloseOver.

        // Read the pre-compiled case fold equivalency classes.  Store
        // each class in a Map, so that for any equivalency class 'E',
        // each member 'e' of the class has an entry:
        // CASE_EQUIV_CLASS.get(e) == E, where 'E' is a reference to a
        // String[] array.  Because the same reference 'E' is used,
        // there is only one in-memory instance of each equivalency
        // class, with multiple pointers to it.  This minimizes
        // run-time memory cost.

        CASE_EQUIV_CLASS = new HashMap();
        // Add pairs.  These are simple equivalency classes like {"a", "A"}.
        int i;
        for (i=0; i<CASE_PAIRS.length(); i+=2) {
            String[] a = new String[2];
            a[0] = String.valueOf(CASE_PAIRS.charAt(i));
            a[1] = String.valueOf(CASE_PAIRS.charAt(i+1));
            CASE_EQUIV_CLASS.put(a[0], a);
            CASE_EQUIV_CLASS.put(a[1], a);
        }
        // Add non-pairs.  These are more complex equivalency classes
        // like {"st", "\uFB05", "\uFB06"}.
        for (i=0; i<CASE_NONPAIRS.length; ++i) {
            String[] a = CASE_NONPAIRS[i];
            for (int j=0; j<a.length; ++j) {
                CASE_EQUIV_CLASS.put(a[j], a);
            }
        }
    }

    /**
     * Internal class for customizing UnicodeSet parsing of properties.
     * TODO: extend to allow customizing of codepoint ranges
     * @internal
     * @deprecated
     * @author medavis
     */
    abstract static class XSymbolTable implements SymbolTable {
        public UnicodeMatcher lookupMatcher(int i) {
        return null;
    }
        public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
        return false;
        }
    public char[] lookup(String s) {
        return null;
    }
    public String parseReference(String text, ParsePosition pos, int limit) {
        return null;
    }
    }
}
