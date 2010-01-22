/*
 *******************************************************************************
 * Copyright (C) 1996-2010, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package com.ibm.icu.text;

import java.io.IOException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.MissingResourceException;
import java.util.TreeSet;

import com.ibm.icu.impl.BMPSet;
import com.ibm.icu.impl.NormalizerImpl;
import com.ibm.icu.impl.RuleCharacterIterator;
import com.ibm.icu.impl.SortedSetRelation;
import com.ibm.icu.impl.UBiDiProps;
import com.ibm.icu.impl.UCaseProps;
import com.ibm.icu.impl.UCharacterProperty;
import com.ibm.icu.impl.UPropertyAliases;
import com.ibm.icu.impl.UnicodeSetStringSpan;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UProperty;
import com.ibm.icu.util.Freezable;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

/**
 * A mutable set of Unicode characters and multicharacter strings.
 * Objects of this class represent <em>character classes</em> used
 * in regular expressions. A character specifies a subset of Unicode
 * code points.  Legal code points are U+0000 to U+10FFFF, inclusive.
 *
 * Note: method freeze() will not only makes the set immutable, but
 * also makes important methods much higher performance:
 * containsNone(...), span(...), spanBack(...) etc.
 * After the object is frozen, any subsequent call that wants to change
 * the object will throw UnsupportedOperationException.
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
 * <a href="http://www.icu-project.org/userguide/unicodeSet.html">
 * http://www.icu-project.org/userguide/unicodeSet.html</a>.
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
 * <p>To iterate over contents of UnicodeSet, use UnicodeSetIterator class.
 *
 * @author Alan Liu
 * @stable ICU 2.0
 * @see UnicodeSetIterator
 */
public class UnicodeSet extends UnicodeFilter implements Iterable<String>, Comparable<UnicodeSet>, Freezable<UnicodeSet> {

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
    TreeSet<String> strings = new TreeSet<String>();

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
    private static final String ASSIGNED = "Assigned"; // [:^Cn:]

    /**
     * A set of all characters _except_ the second through last characters of
     * certain ranges.  These ranges are ranges of characters whose
     * properties are all exactly alike, e.g. CJK Ideographs from
     * U+4E00 to U+9FA5.
     */
    private static UnicodeSet INCLUSIONS[] = null;

    private BMPSet bmpSet; // The set is frozen iff either bmpSet or stringSpan is not null.
    private UnicodeSetStringSpan stringSpan;
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
     * Quickly constructs a set from a set of ranges <s0, e0, s1, e1, s2, e2, ..., sn, en>.
     * There must be an even number of integers, and they must be all greater than zero,
     * all less than or equal to Character.MAX_CODE_POINT.
     * In each pair (..., si, ei, ...) it must be true that si <= ei
     * Between adjacent pairs (...ei, sj...), it must be true that ei+1 < sj
     * @param pairs pairs of character representing ranges
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public UnicodeSet(int... pairs) {
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("Must have even number of integers");
        }
        list = new int[pairs.length + 1]; // don't allocate extra space, because it is likely that this is a fixed set.
        len = list.length;
        int last = -1; // used to ensure that the results are monotonically increasing.
        int i = 0;
        while (i < pairs.length) {
            // start of pair
            int start = pairs[i];
            if (last >= start) {
                throw new IllegalArgumentException("Must be monotonically increasing.");
            }
            list[i++] = last = start;
            // end of pair
            int end = pairs[i] + 1;
            if (last >= end) {
                throw new IllegalArgumentException("Must be monotonically increasing.");
            }
            list[i++] = last = end;
        }
        list[i] = HIGH; // terminate
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
     * @stable ICU 3.8
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
     * @stable ICU 3.2
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
        UnicodeSet result = new UnicodeSet(this);
        result.bmpSet = this.bmpSet;
        result.stringSpan = this.stringSpan;
        return result;
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
        checkFrozen();
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
        checkFrozen();
        list = other.list.clone();
        len = other.len;
        pat = other.pat;
        strings = new TreeSet<String>(other.strings);
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
        checkFrozen();
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
        checkFrozen();
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
     * @stable ICU 3.8
     */
    public UnicodeSet applyPattern(String pattern, int options) {
        checkFrozen();
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
        // "Utility.isUnprintable(c)" seems redundant since the the call
        //      "Utility.escapeUnprintable(buf, c)" does it again inside the if statement
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

        return _generatePattern(result, escapeUnprintable, true);
    }

    /**
     * Generate and append a string representation of this set to result.
     * This does not use this.pat, the cleaned up copy of the string
     * passed to applyPattern().
     * @param result the buffer into which to generate the pattern
     * @param escapeUnprintable escape unprintable characters if true
     * @stable ICU 2.0
     */
    public StringBuffer _generatePattern(StringBuffer result, boolean escapeUnprintable) {
        return _generatePattern(result, escapeUnprintable, true);
    }

    /**
     * Generate and append a string representation of this set to result.
     * This does not use this.pat, the cleaned up copy of the string
     * passed to applyPattern().
     * @param includeStrings if false, doesn't include the strings.
     * @stable ICU 3.8
     */
    public StringBuffer _generatePattern(StringBuffer result,
            boolean escapeUnprintable, boolean includeStrings) {
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

        if (includeStrings && strings.size() > 0) {
            for (String s : strings) {
                result.append('{');
                _appendToPat(result, s, escapeUnprintable);
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
            for (String s : strings) {
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

                boolean forward = offset[0] < limit;

                // firstChar is the leftmost char to match in the
                // forward direction or the rightmost char to match in
                // the reverse direction.
                char firstChar = text.charAt(offset[0]);

                // If there are multiple strings that can match we
                // return the longest match.
                int highWaterLength = 0;

                for (String trial : strings) {
                    //if (trial.length() == 0) {
                    //    return U_MATCH; // null-string always matches
                    //}
                    // assert(trial.length() != 0); // We ensure this elsewhere

                    char c = trial.charAt(forward ? 0 : trial.length() - 1);

                    // Strings are sorted, so we can optimize in the
                    // forward direction.
                    if (forward && c > firstChar) break;
                    if (c != firstChar) continue; 

                    int length = matchRest(text, offset[0], limit, trial);

                    if (incremental) {
                        int maxLen = forward ? limit-offset[0] : offset[0]-limit;
                        if (length == maxLen) {
                            // We have successfully matched but only up to limit.
                            return U_PARTIAL_MATCH;
                        }
                    }

                    if (length == trial.length()) {
                        // We have successfully matched the whole string.
                        if (length > highWaterLength) {
                            highWaterLength = length;
                        }
                        // In the forward direction we know strings
                        // are sorted so we can bail early.
                        if (forward && length < highWaterLength) {
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
     * Tests whether the text matches at the offset. If so, returns the end of the longest substring that it matches. If not, returns -1. 
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public int matchesAt(CharSequence text, int offset) {
        int lastLen = -1;
        strings:
            if (strings.size() != 0) {
                char firstChar = text.charAt(offset);
                String trial = null;
                // find the first string starting with firstChar
                Iterator<String> it = strings.iterator();
                while (it.hasNext()) {
                    trial = it.next();
                    char firstStringChar = trial.charAt(0);
                    if (firstStringChar < firstChar) continue;
                    if (firstStringChar > firstChar) break strings;
                }

                // now keep checking string until we get the longest one
                for (;;) {
                    int tempLen = matchesAt(text, offset, trial);
                    if (lastLen > tempLen) break strings;
                    lastLen = tempLen;
                    if (!it.hasNext()) break;
                    trial = it.next();
                }
            }

        if (lastLen < 2) {
            int cp = UTF16.charAt(text, offset);
            if (contains(cp)) lastLen = UTF16.getCharCount(cp);
        }

        return offset+lastLen;
    }

    /**
     * Does one string contain another, starting at a specific offset?
     * @param text
     * @param offset
     * @param other
     * @return
     */
    // Note: This method was moved from CollectionUtilities
    private static int matchesAt(CharSequence text, int offset, CharSequence other) {
        int len = other.length();
        int i = 0;
        int j = offset;
        for (; i < len; ++i, ++j) {
            char pc = other.charAt(i);
            char tc = text.charAt(j);
            if (pc != tc) return -1;
        }
        return i;
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
        checkFrozen();
        return add_unchecked(start, end);
    }

    /**
     * Adds all characters in range (uses preferred naming convention).
     * @param start The index of where to start on adding all characters.
     * @param end The index of where to end on adding all characters.
     * @return a reference to this object
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public UnicodeSet addAll(int start, int end) {
        checkFrozen();
        return add_unchecked(start, end);
    }

    // for internal use, after checkFrozen has been called
    private UnicodeSet add_unchecked(int start, int end) {
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
        checkFrozen();
        return add_unchecked(c);
    }

    // for internal use only, after checkFrozen has been called
    private final UnicodeSet add_unchecked(int c) {
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
        // TODO: Is the "list[i]-1" a typo? Even if you pass MAX_VALUE into
        //      add_unchecked, the maximum value that "c" will be compared to
        //      is "MAX_VALUE-1" meaning that "if (c == MAX_VALUE)" will
        //      never be reached according to this logic.
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
        checkFrozen();
        int cp = getSingleCP(s);
        if (cp < 0) {
            strings.add(s);
            pat = null;
        } else {
            add_unchecked(cp, cp);
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
        checkFrozen();
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            add_unchecked(cp, cp);
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
     * Remove all strings from this UnicodeSet
     * @return this object, for chaining
     * @stable ICU 4.2
     */
    public final UnicodeSet removeAllStrings() {
        checkFrozen();
        if (strings.size() != 0) {
            strings.clear();
            pat = null;
        }
        return this;
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
        checkFrozen();
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
        checkFrozen();
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
        checkFrozen();
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
        checkFrozen();
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
        checkFrozen();
        int cp = getSingleCP(s);
        if (cp < 0) {
            if (strings.contains(s)) {
                strings.remove(s);
            } else {
                strings.add(s);
            }
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
     * @param b set to be checked for containment
     * @return true if the test condition is met
     * @stable ICU 2.0
     */
    public boolean containsAll(UnicodeSet b) {
        // The specified set is a subset if all of its pairs are contained in
        // this set. This implementation accesses the lists directly for speed.
        // TODO: this could be faster if size() were cached. But that would affect building speed
        // so it needs investigation.
        int[] listB = b.list;
        boolean needA = true;
        boolean needB = true;
        int aPtr = 0;
        int bPtr = 0;
        int aLen = len - 1;
        int bLen = b.len - 1;
        int startA = 0, startB = 0, limitA = 0, limitB = 0;
        while (true) {
            // double iterations are such a pain...
            if (needA) {
                if (aPtr >= aLen) {
                    // ran out of A. If B is also exhausted, then break;
                    if (needB && bPtr >= bLen) {
                        break;
                    }
                    return false;
                }
                startA = list[aPtr++];
                limitA = list[aPtr++];
            }
            if (needB) {
                if (bPtr >= bLen) {
                    // ran out of B. Since we got this far, we have an A and we are ok so far
                    break;
                }
                startB = listB[bPtr++];
                limitB = listB[bPtr++];
            }
            // if B doesn't overlap and is greater than A, get new A
            if (startB >= limitA) {
                needA = true;
                needB = false;
                continue;
            }
            // if B is wholy contained in A, then get a new B
            if (startB >= startA && limitB <= limitA) {
                needA = false;
                needB = true;
                continue;
            }
            // all other combinations mean we fail
            return false;
        }

        if (!strings.containsAll(b.strings)) return false;
        return true;
    }

    //    /**
    //     * Returns true if this set contains all the characters and strings
    //     * of the given set.
    //     * @param c set to be checked for containment
    //     * @return true if the test condition is met
    //     * @stable ICU 2.0
    //     */
    //    public boolean containsAllOld(UnicodeSet c) {
    //        // The specified set is a subset if all of its pairs are contained in
    //        // this set.  It's possible to code this more efficiently in terms of
    //        // direct manipulation of the inversion lists if the need arises.
    //        int n = c.getRangeCount();
    //        for (int i=0; i<n; ++i) {
    //            if (!contains(c.getRangeStart(i), c.getRangeEnd(i))) {
    //                return false;
    //            }
    //        }
    //        if (!strings.containsAll(c.strings)) return false;
    //        return true;
    //    }

    /**
     * Returns true if there is a partition of the string such that this set contains each of the partitioned strings.
     * For example, for the Unicode set [a{bc}{cd}]<br>
     * containsAll is true for each of: "a", "bc", ""cdbca"<br>
     * containsAll is false for each of: "acb", "bcda", "bcx"<br>
     * @param s string containing characters to be checked for containment
     * @return true if the test condition is met
     * @stable ICU 2.0
     */
    public boolean containsAll(String s) {
        int cp;
        for (int i = 0; i < s.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(s, i);
            if (!contains(cp))  {
                if (strings.size() == 0) {
                    return false;
                }
                return containsAll(s, 0);
            }
        }
        return true;
    }

    /**
     * Recursive routine called if we fail to find a match in containsAll, and there are strings
     * @param s source string
     * @param i point to match to the end on
     * @return true if ok
     */
    private boolean containsAll(String s, int i) {
        if (i >= s.length()) {
            return true;
        }
        int  cp= UTF16.charAt(s, i);
        if (contains(cp) && containsAll(s, i+UTF16.getCharCount(cp))) {
            return true;
        }
        for (String setStr : strings) {
            if (s.startsWith(setStr, i) &&  containsAll(s, i+setStr.length())) {
                return true;
            }
        }
        return false;

    }

    /**
     * Get the Regex equivalent for this UnicodeSet
     * @return regex pattern equivalent to this UnicodeSet
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public String getRegexEquivalent() {
        if (strings.size() == 0) {
            return toString();
        }
        StringBuffer result = new StringBuffer("(?:");
        _generatePattern(result, true, false);
        for (String s : strings) {
            result.append('|');
            _appendToPat(result, s, true);
        }
        return result.append(")").toString();
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
     * Returns true if none of the characters or strings in this UnicodeSet appears in the string.
     * For example, for the Unicode set [a{bc}{cd}]<br>
     * containsNone is true for: "xy", "cb"<br>
     * containsNone is false for: "a", "bc", "bcd"<br>
     * @param b set to be checked for containment
     * @return true if the test condition is met
     * @stable ICU 2.0
     */
    public boolean containsNone(UnicodeSet b) {
        // The specified set is a subset if some of its pairs overlap with some of this set's pairs.
        // This implementation accesses the lists directly for speed.
        int[] listB = b.list;
        boolean needA = true;
        boolean needB = true;
        int aPtr = 0;
        int bPtr = 0;
        int aLen = len - 1;
        int bLen = b.len - 1;
        int startA = 0, startB = 0, limitA = 0, limitB = 0;
        while (true) {
            // double iterations are such a pain...
            if (needA) {
                if (aPtr >= aLen) {
                    // ran out of A: break so we test strings
                    break;
                }
                startA = list[aPtr++];
                limitA = list[aPtr++];
            }
            if (needB) {
                if (bPtr >= bLen) {
                    // ran out of B: break so we test strings
                    break;
                }
                startB = listB[bPtr++];
                limitB = listB[bPtr++];
            }
            // if B is higher than any part of A, get new A
            if (startB >= limitA) {
                needA = true;
                needB = false;
                continue;
            }
            // if A is higher than any part of B, get new B
            if (startA >= limitB) {
                needA = false;
                needB = true;
                continue;
            }
            // all other combinations mean we fail
            return false;
        }

        if (!SortedSetRelation.hasRelation(strings, SortedSetRelation.DISJOINT, b.strings)) return false;
        return true;
    }

    //    /**
    //     * Returns true if none of the characters or strings in this UnicodeSet appears in the string.
    //     * For example, for the Unicode set [a{bc}{cd}]<br>
    //     * containsNone is true for: "xy", "cb"<br>
    //     * containsNone is false for: "a", "bc", "bcd"<br>
    //     * @param c set to be checked for containment
    //     * @return true if the test condition is met
    //     * @stable ICU 2.0
    //     */
    //    public boolean containsNoneOld(UnicodeSet c) {
    //        // The specified set is a subset if all of its pairs are contained in
    //        // this set.  It's possible to code this more efficiently in terms of
    //        // direct manipulation of the inversion lists if the need arises.
    //        int n = c.getRangeCount();
    //        for (int i=0; i<n; ++i) {
    //            if (!containsNone(c.getRangeStart(i), c.getRangeEnd(i))) {
    //                return false;
    //            }
    //        }
    //        if (!SortedSetRelation.hasRelation(strings, SortedSetRelation.DISJOINT, c.strings)) return false;
    //        return true;
    //    }

    /**
     * Returns true if this set contains none of the characters
     * of the given string.
     * @param s string containing characters to be checked for containment
     * @return true if the test condition is met
     * @stable ICU 2.0
     */
    public boolean containsNone(String s) {
        return span(s, SpanCondition.NOT_CONTAINED) == s.length();
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
        checkFrozen();
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
        checkFrozen();
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
        checkFrozen();
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
        checkFrozen();
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
        checkFrozen();
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
        checkFrozen();
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
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public UnicodeSet applyPattern(String pattern,
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

        StringBuffer patBuf = new StringBuffer(), buf = null;
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
            //Eclipse stated the following is "dead code"
            /*
            if (false) {
                // Debugging assertion
                if (!((lastItem == 0 && op == 0) ||
                        (lastItem == 1 && (op == 0 || op == '-')) ||
                        (lastItem == 2 && (op == 0 || op == '-' || op == '&')))) {
                    throw new IllegalArgumentException();
                }
            }*/

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
                        patBuf.append('[');
                        backup = chars.getPos(backup); // prepare to backup
                        c = chars.next(opts);
                        literal = chars.isEscaped();
                        if (c == '^' && !literal) {
                            invert = true;
                            patBuf.append('^');
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
                    add_unchecked(lastChar, lastChar);
                    _appendToPat(patBuf, lastChar, false);
                    lastItem = op = 0;
                }

                if (op == '-' || op == '&') {
                    patBuf.append(op);
                }

                if (nested == null) {
                    if (scratch == null) scratch = new UnicodeSet();
                    nested = scratch;
                }
                switch (setMode) {
                case 1:
                    nested.applyPattern(chars, symbols, patBuf, options);
                    break;
                case 2:
                    chars.skipIgnored(opts);
                    nested.applyPropertyPattern(chars, patBuf, symbols);
                    break;
                case 3: // `nested' already parsed
                    nested._toPattern(patBuf, false);
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
                        add_unchecked(lastChar, lastChar);
                        _appendToPat(patBuf, lastChar, false);
                    }
                    // Treat final trailing '-' as a literal
                    if (op == '-') {
                        add_unchecked(op, op);
                        patBuf.append(op);
                    } else if (op == '&') {
                        syntaxError(chars, "Trailing '&'");
                    }
                    patBuf.append(']');
                    mode = 2;
                    continue;
                case '-':
                    if (op == 0) {
                        if (lastItem != 0) {
                            op = (char) c;
                            continue;
                        } else {
                            // Treat final trailing '-' as a literal
                            add_unchecked(c, c);
                            c = chars.next(opts);
                            literal = chars.isEscaped();
                            if (c == ']' && !literal) {
                                patBuf.append("-]");
                                mode = 2;
                                continue;
                            }
                        }
                    }
                    syntaxError(chars, "'-' not after char or set");
                    break;
                case '&':
                    if (lastItem == 2 && op == 0) {
                        op = (char) c;
                        continue;
                    }
                    syntaxError(chars, "'&' not after set");
                    break;
                case '^':
                    syntaxError(chars, "'^' not after '['");
                    break;
                case '{':
                    if (op != 0) {
                        syntaxError(chars, "Missing operand after operator");
                    }
                    if (lastItem == 1) {
                        add_unchecked(lastChar, lastChar);
                        _appendToPat(patBuf, lastChar, false);
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
                    patBuf.append('{');
                    _appendToPat(patBuf, buf.toString(), false);
                    patBuf.append('}');
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
                            add_unchecked(lastChar, lastChar);
                            _appendToPat(patBuf, lastChar, false);
                        }
                        add_unchecked(UnicodeMatcher.ETHER);
                        usePat = true;
                        patBuf.append(SymbolTable.SYMBOL_REF).append(']');
                        mode = 2;
                        continue;
                    }
                    syntaxError(chars, "Unquoted '$'");
                    break;
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
                    add_unchecked(lastChar, c);
                    _appendToPat(patBuf, lastChar, false);
                    patBuf.append(op);
                    _appendToPat(patBuf, c, false);
                    lastItem = op = 0;
                } else {
                    add_unchecked(lastChar, lastChar);
                    _appendToPat(patBuf, lastChar, false);
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
            rebuiltPat.append(patBuf.toString());
        } else {
            _generatePattern(rebuiltPat, false, true);
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
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public <T extends Collection<String>> T addAllTo(T target) {
        return addAllTo(this, target);
    }


    /**
     * Add the contents of the UnicodeSet (as strings) into a collection.
     * @param target collection to add into
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public String[] addAllTo(String[] target) {
        return addAllTo(this, target);
    }

    /**
     * Add the contents of the UnicodeSet (as strings) into an array.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static String[] toArray(UnicodeSet set) {
        return addAllTo(set, new String[set.size()]);
    }

    /**
     * Add the contents of the collection (as strings) into this UnicodeSet.
     * @param source the collection to add
     * @return a reference to this object
     * @stable ICU 2.8
     */
    public UnicodeSet add(Collection<?> source) {
        return addAll(source);
    }

    /**
     * Add the contents of the UnicodeSet (as strings) into a collection.
     * Uses standard naming convention.
     * @param source collection to add into
     * @return a reference to this object
     * @stable ICU 4.2
     */
    public UnicodeSet addAll(Collection<?> source) {
        checkFrozen();
        for (Object o : source) {
            add(o.toString());
        }
        return this;
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
        // TODO: Based on the call hierarchy, polarity of 1 or 2 is never used
        //      so the following if statement will not be called.
        ///CLOVER:OFF
        if (polarity == 1 || polarity == 2) {
            b = LOW;
            if (other[j] == LOW) { // skip base if already LOW
                ++j;
                b = other[j];
            }
            ///CLOVER:ON
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

    private static synchronized UnicodeSet getInclusions(int src) {
        if (INCLUSIONS == null) {
            INCLUSIONS = new UnicodeSet[UCharacterProperty.SRC_COUNT];
        }
        if(INCLUSIONS[src] == null) {
            UnicodeSet incl = new UnicodeSet();
            try {
                switch(src) {
                case UCharacterProperty.SRC_CHAR:
                    UCharacterProperty.INSTANCE.addPropertyStarts(incl);
                    break;
                case UCharacterProperty.SRC_PROPSVEC:
                    UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
                    break;
                case UCharacterProperty.SRC_CHAR_AND_PROPSVEC:
                    UCharacterProperty.INSTANCE.addPropertyStarts(incl);
                    UCharacterProperty.INSTANCE.upropsvec_addPropertyStarts(incl);
                    break;
                case UCharacterProperty.SRC_NORM:
                    NormalizerImpl.addPropertyStarts(incl);
                    break;
                case UCharacterProperty.SRC_CASE_AND_NORM:
                    NormalizerImpl.addPropertyStarts(incl);
                    UCaseProps.getSingleton().addPropertyStarts(incl);
                    break;
                case UCharacterProperty.SRC_CASE:
                    UCaseProps.getSingleton().addPropertyStarts(incl);
                    break;
                case UCharacterProperty.SRC_BIDI:
                    UBiDiProps.getSingleton().addPropertyStarts(incl);
                    break;
                default:
                    throw new IllegalStateException("UnicodeSet.getInclusions(unknown src "+src+")");
                }
            } catch(IOException e) {
                throw new MissingResourceException(e.getMessage(),"","");
            }
            INCLUSIONS[src] = incl;
        }
        return INCLUSIONS[src];
    }

    /**
     * Generic filter-based scanning code for UCD property UnicodeSets.
     */
    private UnicodeSet applyFilter(Filter filter, int src) {
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
        UnicodeSet inclusions = getInclusions(src);
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
                    add_unchecked(startHasProperty, ch-1);
                    startHasProperty = -1;
                }
            }
        }
        if (startHasProperty >= 0) {
            add_unchecked(startHasProperty, 0x10FFFF);
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
        checkFrozen();
        if (prop == UProperty.GENERAL_CATEGORY_MASK) {
            applyFilter(new GeneralCategoryMaskFilter(value), UCharacterProperty.SRC_CHAR);
        } else {
            applyFilter(new IntPropertyFilter(prop, value), UCharacterProperty.INSTANCE.getSource(prop));
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
     * @param propertyAlias A string of the property alias.
     * @param valueAlias A string of the value alias.
     * @param symbols if not null, then symbols are first called to see if a property
     * is available. If true, then everything else is skipped.
     * @return this set
     * @stable ICU 3.2
     */
    public UnicodeSet applyPropertyAlias(String propertyAlias,
            String valueAlias, SymbolTable symbols) {
        checkFrozen();
        int p;
        int v;
        boolean mustNotBeEmpty = false, invert = false;

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
                        //mustNotBeEmpty = true;
                        // old code was wrong; anything between 0 and 255 is valid even if unused.
                        if (v < 0 || v > 255) throw e;
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
                    applyFilter(new NumericValueFilter(value), UCharacterProperty.SRC_CHAR);
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
                                add_unchecked(ch);
                                return this;
                }
                case UProperty.AGE:
                {
                    // Must munge name, since
                    // VersionInfo.getInstance() does not do
                    // 'loose' matching.
                    VersionInfo version = VersionInfo.getInstance(mungeCharName(valueAlias));
                    applyFilter(new VersionFilter(version), UCharacterProperty.SRC_PROPSVEC);
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
            UPropertyAliases pnames = UPropertyAliases.INSTANCE;
            p = UProperty.GENERAL_CATEGORY_MASK;
            v = pnames.getPropertyValueEnum(p, propertyAlias);
            if (v == UProperty.UNDEFINED) {
                p = UProperty.SCRIPT;
                v = pnames.getPropertyValueEnum(p, propertyAlias);
                if (v == UProperty.UNDEFINED) {
                    p = pnames.getPropertyEnum(propertyAlias);
                    if (p == UProperty.UNDEFINED) {
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
                        } else if (0 == UPropertyAliases.compare(ASSIGNED, propertyAlias)) {
                            // [:Assigned:]=[:^Cn:]
                            p = UProperty.GENERAL_CATEGORY_MASK;
                            v = (1<<UCharacter.UNASSIGNED);
                            invert = true;
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
        if(invert) {
            complement();
        }

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
        String patStr = chars.lookahead();
        ParsePosition pos = new ParsePosition(0);
        applyPropertyPattern(patStr, pos, symbols);
        if (pos.getIndex() == 0) {
            syntaxError(chars, "Invalid property pattern");
        }
        chars.jumpahead(pos.getIndex());
        rebuiltPat.append(patStr.substring(0, pos.getIndex()));
    }

    //----------------------------------------------------------------
    // Case folding API
    //----------------------------------------------------------------

    /**
     * Bitmask for constructor and applyPattern() indicating that
     * white space should be ignored.  If set, ignore characters for
     * which UCharacterProperty.isRuleWhiteSpace() returns true,
     * unless they are quoted or escaped.  This may be ORed together
     * with other selectors.
     * @stable ICU 3.8
     */
    public static final int IGNORE_SPACE = 1;

    /**
     * Bitmask for constructor, applyPattern(), and closeOver()
     * indicating letter case.  This may be ORed together with other
     * selectors.
     *
     * Enable case insensitive matching.  E.g., "[ab]" with this flag
     * will match 'a', 'A', 'b', and 'B'.  "[^ab]" with this flag will
     * match all except 'a', 'A', 'b', and 'B'. This performs a full
     * closure over case mappings, e.g. U+017F for s.
     *
     * The resulting set is a superset of the input for the code points but
     * not for the strings.
     * It performs a case mapping closure of the code points and adds
     * full case folding strings for the code points, and reduces strings of
     * the original set to their full case folding equivalents.
     *
     * This is designed for case-insensitive matches, for example
     * in regular expressions. The full code point case closure allows checking of
     * an input character directly against the closure set.
     * Strings are matched by comparing the case-folded form from the closure
     * set with an incremental case folding of the string in question.
     *
     * The closure set will also contain single code points if the original
     * set contained case-equivalent strings (like U+00DF for "ss" or "Ss" etc.).
     * This is not necessary (that is, redundant) for the above matching method
     * but results in the same closure sets regardless of whether the original
     * set contained the code point or a string.
     * @stable ICU 3.8
     */
    public static final int CASE = 2;

    /**
     * Alias for UnicodeSet.CASE, for ease of porting from C++ where ICU4C
     * also has both USET_CASE and USET_CASE_INSENSITIVE (see uset.h).
     * @see #CASE
     * @stable ICU 3.4
     */
    public static final int CASE_INSENSITIVE = 2;

    /**
     * Bitmask for constructor, applyPattern(), and closeOver()
     * indicating letter case.  This may be ORed together with other
     * selectors.
     *
     * Enable case insensitive matching.  E.g., "[ab]" with this flag
     * will match 'a', 'A', 'b', and 'B'.  "[^ab]" with this flag will
     * match all except 'a', 'A', 'b', and 'B'. This adds the lower-,
     * title-, and uppercase mappings as well as the case folding
     * of each existing element in the set.
     * @stable ICU 3.4
     */
    public static final int ADD_CASE_MAPPINGS = 4;

    //  add the result of a full case mapping to the set
    //  use str as a temporary string to avoid constructing one
    private static final void addCaseMapping(UnicodeSet set, int result, StringBuffer full) {
        if(result >= 0) {
            if(result > UCaseProps.MAX_STRING_LENGTH) {
                // add a single-code point case mapping
                set.add(result);
            } else {
                // add a string case mapping from full with length result
                set.add(full.toString());
                full.setLength(0);
            }
        }
        // result < 0: the code point mapped to itself, no need to add it
        // see UCaseProps
    }

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
     * @stable ICU 3.8
     */
    public UnicodeSet closeOver(int attribute) {
        checkFrozen();
        if ((attribute & (CASE | ADD_CASE_MAPPINGS)) != 0) {
            UCaseProps csp;
            try {
                csp = UCaseProps.getSingleton();
            } catch(IOException e) {
                return this;
            }
            UnicodeSet foldSet = new UnicodeSet(this);
            ULocale root = ULocale.ROOT;

            // start with input set to guarantee inclusion
            // CASE: remove strings because the strings will actually be reduced (folded);
            //       therefore, start with no strings and add only those needed
            if((attribute & CASE) != 0) {
                foldSet.strings.clear();
            }

            int n = getRangeCount();
            int result;
            StringBuffer full = new StringBuffer();
            int locCache[] = new int[1];

            for (int i=0; i<n; ++i) {
                int start = getRangeStart(i);
                int end   = getRangeEnd(i);

                if((attribute & CASE) != 0) {
                    // full case closure
                    for (int cp=start; cp<=end; ++cp) {
                        csp.addCaseClosure(cp, foldSet);
                    }
                } else {
                    // add case mappings
                    // (does not add long s for regular s, or Kelvin for k, for example)
                    for (int cp=start; cp<=end; ++cp) {
                        result = csp.toFullLower(cp, null, full, root, locCache);
                        addCaseMapping(foldSet, result, full);

                        result = csp.toFullTitle(cp, null, full, root, locCache);
                        addCaseMapping(foldSet, result, full);

                        result = csp.toFullUpper(cp, null, full, root, locCache);
                        addCaseMapping(foldSet, result, full);

                        result = csp.toFullFolding(cp, full, 0);
                        addCaseMapping(foldSet, result, full);
                    }
                }
            }
            if (!strings.isEmpty()) {
                if ((attribute & CASE) != 0) {
                    for (String s : strings) {
                        String str = UCharacter.foldCase(s, 0);
                        if(!csp.addStringCaseClosure(str, foldSet)) {
                            foldSet.add(str); // does not map to code points: add the folded string itself
                        }
                    }
                } else {
                    BreakIterator bi = BreakIterator.getWordInstance(root);
                    for (String str : strings) {
                        foldSet.add(UCharacter.toLowerCase(root, str));
                        foldSet.add(UCharacter.toTitleCase(root, str, bi));
                        foldSet.add(UCharacter.toUpperCase(root, str));
                        foldSet.add(UCharacter.foldCase(str, 0));
                    }
                }
            }
            set(foldSet);
        }
        return this;
    }

    /**
     * Internal class for customizing UnicodeSet parsing of properties.
     * TODO: extend to allow customizing of codepoint ranges
     * @draft ICU3.8
     * @provisional This API might change or be removed in a future release.
     * @author medavis
     */
    abstract public static class XSymbolTable implements SymbolTable {
        /**
         * Default constructor
         * @draft ICU3.8
         * @provisional This API might change or be removed in a future release.
         */
        public XSymbolTable(){}
        /**
         * Supplies default implementation for SymbolTable (no action).
         * @draft ICU3.8
         * @provisional This API might change or be removed in a future release.
         */
        public UnicodeMatcher lookupMatcher(int i) {
            return null;
        }
        /**
         * Apply a new property alias. Is called when parsing [:xxx=yyy:]. Results are to put into result.
         * @param propertyName the xxx in [:xxx=yyy:]
         * @param propertyValue the yyy in [:xxx=yyy:]
         * @param result where the result is placed
         * @return true if handled
         * @draft ICU3.8
         * @provisional This API might change or be removed in a future release.
         */
        public boolean applyPropertyAlias(String propertyName, String propertyValue, UnicodeSet result) {
            return false;
        }
        /**
         * Supplies default implementation for SymbolTable (no action).
         * @draft ICU3.8
         * @provisional This API might change or be removed in a future release.
         */
        public char[] lookup(String s) {
            return null;
        }
        /**
         * Supplies default implementation for SymbolTable (no action).
         * @draft ICU3.8
         * @provisional This API might change or be removed in a future release.
         */
        public String parseReference(String text, ParsePosition pos, int limit) {
            return null;
        }
    }

    /**
     * Is this frozen, according to the Freezable interface?
     * 
     * @return value
     * @stable ICU 3.8
     */
    public boolean isFrozen() {
        return (bmpSet != null || stringSpan != null);
    }

    /**
     * Freeze this class, according to the Freezable interface.
     * 
     * @return this
     * @stable ICU 4.4
     */
    public UnicodeSet freeze() {
        if (!isFrozen()) {
            // Do most of what compact() does before freezing because
            // compact() will not work when the set is frozen.
            // Small modification: Don't shrink if the savings would be tiny (<=GROW_EXTRA).

            // Delete buffer first to defragment memory less.
            buffer = null;
            if (list.length > (len + GROW_EXTRA)) {
                // Make the capacity equal to len or 1.
                // We don't want to realloc of 0 size.
                int capacity = (len == 0) ? 1 : len;
                int[] oldList = list;
                list = new int[capacity];
                for (int i = capacity; i-- > 0;) {
                    list[i] = oldList[i];
                }
            }

            // Optimize contains() and span() and similar functions.
            if (!strings.isEmpty()) {
                stringSpan = new UnicodeSetStringSpan(this, new ArrayList<String>(strings), UnicodeSetStringSpan.ALL);
                if (!stringSpan.needsStringSpanUTF16()) {
                    // All strings are irrelevant for span() etc. because
                    // all of each string's code points are contained in this set.
                    // Do not check needsStringSpanUTF8() because UTF-8 has at most as
                    // many relevant strings as UTF-16.
                    // (Thus needsStringSpanUTF8() implies needsStringSpanUTF16().)
                    stringSpan = null;
                }
            }
            if (stringSpan == null) {
                // No span-relevant strings: Optimize for code point spans.
                bmpSet = new BMPSet(list, len);
            }
        }
        return this;
    }

    /**
     * Span a string using this UnicodeSet.
     * 
     * @param s The string to be spanned
     * @param spanCondition The span condition
     * @return the length of the span
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public int span(CharSequence s, SpanCondition spanCondition) {
        return span(s, 0, spanCondition);
    }

    /**
     * Span a string using this UnicodeSet.
     *   If the start index is less than 0, span will start from 0.
     *   If the start index is greater than the string length, span returns the string length.
     * 
     * @param s The string to be spanned
     * @param start The start index that the span begins
     * @param spanCondition The span condition
     * @return the string index which ends the span (i.e. exclusive)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public int span(CharSequence s, int start, SpanCondition spanCondition) {
        if (start < 0) {
          start = 0;
        } else if (start > s.length()) {
          return s.length();
        }
        int end = s.length();
        int len = end - start;
        if (len <= 0) {
            return start;
        }
        if (bmpSet != null) {
            return start + bmpSet.span(s, start, end, spanCondition);
        }

        if (stringSpan != null) {
            return start + stringSpan.span(s, start, len, spanCondition);
        } else if (!strings.isEmpty()) {
            int which = spanCondition == SpanCondition.NOT_CONTAINED ? UnicodeSetStringSpan.FWD_UTF16_NOT_CONTAINED
                    : UnicodeSetStringSpan.FWD_UTF16_CONTAINED;
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList<String>(strings), which);
            if (strSpan.needsStringSpanUTF16()) {
                return start + strSpan.span(s, start, len, spanCondition);
            }
        }

        // Pin to 0/1 values.
        boolean spanContained = (spanCondition != SpanCondition.NOT_CONTAINED);

        int c;
        int next = start;
        do {
            c = Character.codePointAt(s, next);
            if (spanContained != contains(c)) {
                break;
            }
            next = Character.offsetByCodePoints(s, next, 1);
        } while (next < end);
        return next;
    }

    /**
     * Span a string backwards (from the end) using this UnicodeSet.
     * 
     * @param s The string to be spanned
     * @param spanCondition The span condition
     * @return The string index which starts the span (i.e. inclusive).
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public int spanBack(CharSequence s, SpanCondition spanCondition) {
      return spanBack(s, s.length(), spanCondition);
    }

    /**
     * Span a string backwards (from the fromIndex) using this UnicodeSet.
     *   If the fromIndex is less than 0 or greater than string length, it will span back from the string length.
     * 
     * @param s The string to be spanned
     * @param fromIndex The index of the char (exclusive) that the string should be spanned backwards
     * @param spanCondition The span condition
     * @return The string index which starts the span (i.e. inclusive).
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public int spanBack(CharSequence s, int fromIndex, SpanCondition spanCondition) {
        if (fromIndex < 0 ||
            fromIndex > s.length()) {
            fromIndex = s.length();
        }
        if (fromIndex > 0 && bmpSet != null) {
            return bmpSet.spanBack(s, fromIndex, spanCondition);
        }
        if (fromIndex == 0) {
            return 0;
        }
        if (stringSpan != null) {
            return stringSpan.spanBack(s, fromIndex, spanCondition);
        } else if (!strings.isEmpty()) {
            int which = (spanCondition == SpanCondition.NOT_CONTAINED)
                    ? UnicodeSetStringSpan.BACK_UTF16_NOT_CONTAINED
                    : UnicodeSetStringSpan.BACK_UTF16_CONTAINED;
            UnicodeSetStringSpan strSpan = new UnicodeSetStringSpan(this, new ArrayList<String>(strings), which);
            if (strSpan.needsStringSpanUTF16()) {
                return strSpan.spanBack(s, fromIndex, spanCondition);
            }
        }

        // Pin to 0/1 values.
        boolean spanContained = (spanCondition != SpanCondition.NOT_CONTAINED);

        int c;
        int prev = fromIndex;
        do {
            c = Character.codePointBefore(s, prev);
            if (spanContained != contains(c)) {
                break;
            }
            prev = Character.offsetByCodePoints(s, prev, -1);
        } while (prev > 0);
        return prev;
    }

    /**
     * Clone a thawed version of this class, according to the Freezable interface.
     * @return this
     * @stable ICU 4.4
     */
    public UnicodeSet cloneAsThawed() {
        UnicodeSet result = (UnicodeSet) clone();
        result.bmpSet = null;
        result.stringSpan = null;
        return result;
    }

    // internal function
    private void checkFrozen() {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify frozen object");
        }
    }

    // ************************
    // Additional methods for integration with Generics and Collections
    // ************************

    /**
     * Returns a string iterator. Uses the same order of iteration as {@link UnicodeSetIterator}.
     * @see java.util.Set#iterator()
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public Iterator<String> iterator() {
        return new UnicodeSetIterator2(this);
    }

    // Cover for string iteration. 
    private static class UnicodeSetIterator2 implements Iterator<String> {
        // Invariants:
        // sourceList != null then sourceList[item] is a valid character
        // sourceList == null then delegates to stringIterator
        private int[] sourceList;
        private int len;
        private int item;
        private int current;
        private int limit;
        private TreeSet<String> sourceStrings;
        private Iterator<String> stringIterator;
        private char[] buffer;

        UnicodeSetIterator2(UnicodeSet source) {
            // set according to invariants
            len = source.len - 1;
            if (item >= len) {
                stringIterator = source.strings.iterator();
                sourceList = null;
            } else {
                sourceStrings = source.strings;
                sourceList = source.list;
                current = sourceList[item++];
                limit = sourceList[item++];
            }
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return sourceList != null || stringIterator.hasNext();
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#next()
         */
        public String next() {
            if (sourceList == null) {
                return stringIterator.next();
            }
            int codepoint = current++;
            // we have the codepoint we need, but we may need to adjust the state
            if (current >= limit) {
                if (item >= len) {
                    stringIterator = sourceStrings.iterator();
                    sourceList = null;
                } else {
                    current = sourceList[item++];
                    limit = sourceList[item++];
                }
            }
            // Now return. Single code point is easy
            if (codepoint <= 0xFFFF) {
                return String.valueOf((char)codepoint);
            }
            // But Java lacks a valueOfCodePoint, so we handle ourselves for speed
            // allocate a buffer the first time, to make conversion faster.
            if (buffer == null) {
                buffer = new char[2];
            }
            // compute ourselves, to save tests and calls
            int offset = codepoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
            buffer[0] = (char)((offset >>> 10) + Character.MIN_HIGH_SURROGATE);
            buffer[1] = (char)((offset & 0x3ff) + Character.MIN_LOW_SURROGATE);
            return String.valueOf(buffer);
        }

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }  
    }

    /**
     * @see #containsAll(com.ibm.icu.text.UnicodeSet)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public boolean containsAll(Collection<String> collection) {
        for (String o : collection) {
            if (!contains(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see #containsNone(com.ibm.icu.text.UnicodeSet)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public boolean containsNone(Collection<String> collection) {
        for (String o : collection) {
            if (contains(o)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @see #containsAll(com.ibm.icu.text.UnicodeSet)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public final boolean containsSome(Collection<String> collection) {
        return !containsNone(collection);
    }

    /**
     * @see #addAll(com.ibm.icu.text.UnicodeSet)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public UnicodeSet addAll(String... collection) {
        checkFrozen();
        for (String str : collection) {
            add(str);
        }
        return this;
    }


    /**
     * @see #removeAll(com.ibm.icu.text.UnicodeSet)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public UnicodeSet removeAll(Collection<String> collection) {
        checkFrozen();
        for (String o : collection) {
            remove(o);
        }
        return this;
    }

    /**
     * @see #retainAll(com.ibm.icu.text.UnicodeSet)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public UnicodeSet retainAll(Collection<String> collection) {
        checkFrozen();
        // TODO optimize
        UnicodeSet toRetain = new UnicodeSet();
        toRetain.addAll(collection);
        retainAll(toRetain);
        return this;
    }

    /**
     * Comparison style enums used by {@link UnicodeSet#compareTo(UnicodeSet, ComparisonStyle)}.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public enum ComparisonStyle {
        /**
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        SHORTER_FIRST,
        /**
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        LEXICOGRAPHIC,
        /**
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        LONGER_FIRST}

    /**
     * Compares UnicodeSets, where shorter come first, and otherwise lexigraphically
     * (according to the comparison of the first characters that differ).
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public int compareTo(UnicodeSet o) {
        return compareTo(o, ComparisonStyle.SHORTER_FIRST);
    }
    /**
     * Compares UnicodeSets, in three different ways.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public int compareTo(UnicodeSet o, ComparisonStyle style) {
        if (style != ComparisonStyle.LEXICOGRAPHIC) {
            int diff = size() - o.size();
            if (diff != 0) {
                return (diff < 0) == (style == ComparisonStyle.SHORTER_FIRST) ? -1 : 1;
            }
        }
        int result;
        for (int i = 0; ; ++i) {
            if (0 != (result = list[i] - o.list[i])) {
                // if either list ran out, compare to the last string
                if (list[i] == HIGH) {
                    if (strings.isEmpty()) return 1;
                    String item = strings.first();
                    return compare(item, o.list[i]);
                }
                if (o.list[i] == HIGH) {
                    if (o.strings.isEmpty()) return -1;
                    String item = o.strings.first();
                    return -compare(item, list[i]);
                }
                // otherwise return the result if even index, or the reversal if not
                return (i & 1) == 0 ? result : -result;
            }
            if (list[i] == HIGH) {
                break;
            }
        }
        return compare(strings, o.strings);
    }

    /**
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public int compareTo(Iterable<String> other) {
        return compare(this, other);
    }

    /**
     * Utility to compare a string to a code point.
     * Same results as turning the code point into a string (with the [ugly] new StringBuilder().appendCodePoint(codepoint).toString())
     * and comparing, but much faster (no object creation). 
     * Note that this (=String) order is UTF-16 order -- *not* code point order.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static int compare(String string, int codePoint) {
        if (codePoint < Character.MIN_CODE_POINT || codePoint > Character.MAX_CODE_POINT) {
            throw new IllegalArgumentException();
        }
        int stringLength = string.length();
        if (stringLength == 0) {
            return -1;
        }
        char firstChar = string.charAt(0);
        int offset = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;

        if (offset < 0) { // BMP codePoint
            int result = firstChar - codePoint;
            if (result != 0) {
                return result;
            }
            return stringLength - 1;
        } 
        // non BMP
        char lead = (char)((offset >>> 10) + Character.MIN_HIGH_SURROGATE);
        int result = firstChar - lead;
        if (result != 0) {
            return result;
        }
        if (stringLength > 1) {
            char trail = (char)((offset & 0x3ff) + Character.MIN_LOW_SURROGATE);
            result = string.charAt(1) - trail;
            if (result != 0) {
                return result;
            }
        }
        return stringLength - 2;
    }

    /**
     * Utility to compare a string to a code point.
     * Same results as turning the code point into a string and comparing, but much faster (no object creation). 
     * Actually, there is one difference; a null compares as less.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static int compare(int codepoint, String a) {
        return -compare(a, codepoint);
    }

    /**
     * Utility to compare two iterables. Warning: the ordering in iterables is important. For Collections that are ordered,
     * like Lists, that is expected. However, Sets in Java violate Leibniz's law when it comes to iteration.
     * That means that sets can't be compared directly with this method, unless they are TreeSets without
     * (or with the same) comparator. Unfortunately, it is impossible to reliably detect in Java whether subclass of
     * Collection satisfies the right criteria, so it is left to the user to avoid those circumstances.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static <T extends Comparable<T>> int compare(Iterable<T> collection1, Iterable<T> collection2) {
        Iterator<T> first = collection1.iterator();
        Iterator<T> other = collection2.iterator();
        while (true) {
            if (!first.hasNext()) {
                return other.hasNext() ? -1 : 0;
            } else if (!other.hasNext()) {
                return 1;
            }
            T item1 = first.next();
            T item2 = other.next();
            int result = item1.compareTo(item2);
            if (result != 0) {
                return result;
            }
        }
    }

    /**
     * Utility to compare two collections, optionally by size, and then lexicographically.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static <T extends Comparable<T>> int compare(Collection<T> collection1, Collection<T> collection2, ComparisonStyle style) {
        if (style != ComparisonStyle.LEXICOGRAPHIC) {
            int diff = collection1.size() - collection2.size();
            if (diff != 0) {
                return (diff < 0) == (style == ComparisonStyle.SHORTER_FIRST) ? -1 : 1;
            }
        }
        return compare(collection1, collection2);
    }

    /**
     * Utility for adding the contents of an iterable to a collection.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static <T, U extends Collection<T>> U addAllTo(Iterable<T> source, U target) {
        for (T item : source) {
            target.add(item);
        }
        return target;
    }

    /**
     * Utility for adding the contents of an iterable to a collection.
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public static <T> T[] addAllTo(Iterable<T> source, T[] target) {
        int i = 0;
        for (T item : source) {
            target[i++] = item;
        }
        return target;
    }

    /**
     * For iterating through the strings in the set. Example:
     * <pre>
     * for (String key : myUnicodeSet.strings()) {
     *   doSomethingWith(key);
     * }
     * </pre>
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public Iterable<String> strings() {
        return Collections.unmodifiableSortedSet(strings);
    }

    /**
     * Return the value of the first code point, if the string is exactly one code point. Otherwise return Integer.MAX_VALUE.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public static int getSingleCodePoint(String s) {
        int length = s.length();
        if (length < 1 || length > 2) {
            return Integer.MAX_VALUE;
        }
        int result = s.codePointAt(0);
        return (result < 0x10000) == (length == 1) ? result : Integer.MAX_VALUE;
    }

    /**
     * Simplify the ranges in a Unicode set by merging any ranges that are only separated by characters in the dontCare set. 
     * For example, the ranges: \\u2E80-\\u2E99\\u2E9B-\\u2EF3\\u2F00-\\u2FD5\\u2FF0-\\u2FFB\\u3000-\\u303E change to \\u2E80-\\u303E 
     * if the dontCare set includes unassigned characters (for a particular version of Unicode).
     * @param dontCare Set with the don't-care characters for spanning
     * @return the input set, modified
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public UnicodeSet addBridges(UnicodeSet dontCare) {
        UnicodeSet notInInput = new UnicodeSet(this).complement();
        for (UnicodeSetIterator it = new UnicodeSetIterator(notInInput); it.nextRange();) {
            if (it.codepoint != 0 && it.codepoint != UnicodeSetIterator.IS_STRING && it.codepointEnd != 0x10FFFF && dontCare.contains(it.codepoint,it.codepointEnd)) {
                add(it.codepoint,it.codepointEnd);
            }
        }
        return this;
    }

    /**
     * Find the first index at or after fromIndex where the UnicodeSet matches at that index.
     * If findNot is true, then reverse the sense of the match: find the first place where the UnicodeSet doesn't match.
     * If there is no match, length is returned.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public int findIn(CharSequence value, int fromIndex, boolean findNot) {
        //TODO add strings, optimize, using ICU4C algorithms
        int cp;
        for (; fromIndex < value.length(); fromIndex += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(value, fromIndex);
            if (contains(cp) != findNot) {
                break;
            }
        }
        return fromIndex;
    }

    /**
     * Find the last index before fromIndex where the UnicodeSet matches at that index.
     * If findNot is true, then reverse the sense of the match: find the last place where the UnicodeSet doesn't match.
     * If there is no match, -1 is returned.
     * BEFORE index is not in the UnicodeSet.
     * @internal
     * @deprecated This API is ICU internal only.
     */
    public int findLastIn(CharSequence value, int fromIndex, boolean findNot) {
        //TODO add strings, optimize, using ICU4C algorithms
        int cp;
        fromIndex -= 1;
        for (; fromIndex >= 0; fromIndex -= UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(value, fromIndex);
            if (contains(cp) != findNot) {
                break;
            }
        }
        return fromIndex < 0 ? -1 : fromIndex;
    }

    /**
     * Strips code points from source. If matches is true, script all that match <i>this</i>. If matches is false, then strip all that <i>don't</i> match.
     * @param source The source of the CharSequence to strip from.
     * @param matches A boolean to either strip all that matches or don't match with the current UnicodeSet object.
     * @return The string after it has been stripped.
     */
    public String stripFrom(CharSequence source, boolean matches) {
        StringBuilder result = new StringBuilder();
        for (int pos = 0; pos < source.length();) {
            int inside = findIn(source, pos, !matches);
            result.append(source.subSequence(pos, inside));
            pos = findIn(source, inside, matches); // get next start
        }
        return result.toString();
    }

    /**
     * Argument values for whether span() and similar functions continue while the current character is contained vs.
     * not contained in the set.
     * <p>
     * The functionality is straightforward for sets with only single code points, without strings (which is the common
     * case):
     * <ul>
     * <li>CONTAINED and SIMPLE work the same.
     * <li>span() and spanBack() partition any string the
     * same way when alternating between span(NOT_CONTAINED) and span(either "contained" condition).
     * <li>Using a
     * complemented (inverted) set and the opposite span conditions yields the same results.
     * </ul>
     * When a set contains multi-code point strings, then these statements may not be true, depending on the strings in
     * the set (for example, whether they overlap with each other) and the string that is processed. For a set with
     * strings:
     * <ul>
     * <li>The complement of the set contains the opposite set of code points, but the same set of strings.
     * Therefore, complementing both the set and the span conditions may yield different results.
     * <li>When starting spans
     * at different positions in a string (span(s, ...) vs. span(s+1, ...)) the ends of the spans may be different
     * because a set string may start before the later position.
     * <li>span(SIMPLE) may be shorter than
     * span(CONTAINED) because it will not recursively try all possible paths. For example, with a set which
     * contains the three strings "xy", "xya" and "ax", span("xyax", CONTAINED) will return 4 but span("xyax",
     * SIMPLE) will return 3. span(SIMPLE) will never be longer than span(CONTAINED).
     * <li>With either "contained" condition, span() and spanBack() may partition a string in different ways. For example,
     * with a set which contains the two strings "ab" and "ba", and when processing the string "aba", span() will yield
     * contained/not-contained boundaries of { 0, 2, 3 } while spanBack() will yield boundaries of { 0, 1, 3 }.
     * </ul>
     * Note: If it is important to get the same boundaries whether iterating forward or backward through a string, then
     * either only span() should be used and the boundaries cached for backward operation, or an ICU BreakIterator could
     * be used.
     * <p>
     * Note: Unpaired surrogates are treated like surrogate code points. Similarly, set strings match only on code point
     * boundaries, never in the middle of a surrogate pair.
     * 
     * @draft ICU 4.4
     * @provisional This API might change or be removed in a future release.
     */
    public enum SpanCondition {
        /**
         * Continue a span() while there is no set element at the current position. Stops before the first set element
         * (character or string). (For code points only, this is like while contains(current)==FALSE).
         * <p>
         * When span() returns, the substring between where it started and the position it returned consists only of
         * characters that are not in the set, and none of its strings overlap with the span.
         * 
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        NOT_CONTAINED,
        /**
         * Continue a span() while there is a set element at the current position. (For characters only, this is like
         * while contains(current)==TRUE).
         * <p>
         * When span() returns, the substring between where it started and the position it returned consists only of set
         * elements (characters or strings) that are in the set.
         * <p>
         * If a set contains strings, then the span will be the longest substring matching any of the possible
         * concatenations of set elements (characters or strings). (There must be a single, non-overlapping
         * concatenation of characters or strings.) This is equivalent to a POSIX regular expression for (OR of each set
         * element)*.
         * 
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        CONTAINED,
        /**
         * Continue a span() while there is a set element at the current position. (For characters only, this is like
         * while contains(current)==TRUE).
         * <p>
         * When span() returns, the substring between where it started and the position it returned consists only of set
         * elements (characters or strings) that are in the set.
         * <p>
         * If a set only contains single characters, then this is the same as CONTAINED.
         * <p>
         * If a set contains strings, then the span will be the longest substring with a match at each position with the
         * longest single set element (character or string).
         * <p>
         * Use this span condition together with other longest-match algorithms, such as ICU converters
         * (ucnv_getUnicodeSet()).
         * 
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        SIMPLE,
        /**
         * One more than the last span condition.
         * 
         * @draft ICU 4.4
         * @provisional This API might change or be removed in a future release.
         */
        CONDITION_COUNT
    }
}
//eof
