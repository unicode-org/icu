package com.ibm.text;

import java.text.*;

/**
 * A mutable set of Unicode characters.  Objects of this class
 * represent <em>character classes</em> used in regular expressions.
 * Such classes specify a subset of the set of all Unicode characters,
 * which in this implementation is the characters from U+0000 to
 * U+FFFF, ignoring surrogates.
 *
 * <p>This class supports two APIs.  The first is modeled after Java 2's
 * <code>java.util.Set</code> interface, although this class does not
 * implement that interface.  All methods of <code>Set</code> are
 * supported, with the modification that they take a character range
 * or single character instead of an <code>Object</code>, and they
 * take a <code>UnicodeSet</code> instead of a <code>Collection</code>.
 *
 * <p>The second API is the
 * <code>applyPattern()</code>/<code>toPattern()</code> API from the
 * <code>java.text.Format</code>-derived classes.  Unlike the
 * methods that add characters, add categories, and control the logic
 * of the set, the method <code>applyPattern()</code> sets all
 * attributes of a <code>UnicodeSet</code> at once, based on a
 * string pattern.
 *
 * <p>In addition, the set complement operation is supported through
 * the <code>complement()</code> method.
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
 *       ('[:' '^'? category ':]')</code></td>
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
 *       <td nowrap valign="top" align="right"><code>category :=&nbsp; </code></td>
 *       <td valign="top"><code>'M' | 'N' | 'Z' | 'C' | 'L' | 'P' |
 *       'S' | 'Mn' | 'Mc' | 'Me' | 'Nd' | 'Nl' | 'No' | 'Zs' | 'Zl' |
 *       'Zp' | 'Cc' | 'Cf' | 'Cs' | 'Co' | 'Cn' | 'Lu' | 'Ll' | 'Lt'
 *       | 'Lm' | 'Lo' | 'Pc' | 'Pd' | 'Ps' | 'Pe' | 'Po' | 'Sm' |
 *       'Sc' | 'Sk' | 'So'</code></td>
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
 * meaning.  White space characters, as defined by Character.isWhitespace(), are
 * ignored, unless they are escaped.
 *
 * Patterns specify individual characters, ranges of characters, and
 * Unicode character categories.  When elements are concatenated, they
 * specify their union.  To complement a set, place a '^' immediately
 * after the opening '[' or '[:'.  In any other location, '^' has no
 * special meaning.
 *
 * <p>Ranges are indicated by placing two a '-' between two
 * characters, as in "a-z".  This specifies the range of all
 * characters from the left to the right, in Unicode order.  If the
 * left and right characters are the same, then the range consists of
 * just that character.  If the left character is greater than the
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
 * that is, U+0000 through 'a'-1 and 'z'+1 through U+FFFF
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>][<em>pat2</em>]]</code>
 * <td>The union of sets specified by <em>pat1</em> and <em>pat2</em>
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>]&[<em>pat2</em>]]</code>
 * <td>The intersection of sets specified by <em>pat1</em> and <em>pat2</em>
 * <tr valign=top><td nowrap><code>[[<em>pat1</em>]-[<em>pat2</em>]]</code>
 * <td>The asymmetric difference of sets specified by <em>pat1</em> and
 * <em>pat2</em>
 * <tr valign=top><td nowrap><code>[:Lu:]</code>
 * <td>The set of characters belonging to the given
 * Unicode category, as defined by <code>Character.getType()</code>; in
 * this case, Unicode uppercase letters
 * <tr valign=top><td nowrap><code>[:L:]</code>
 * <td>The set of characters belonging to all Unicode categories
 * starting wih 'L', that is, <code>[[:Lu:][:Ll:][:Lt:][:Lm:][:Lo:]]</code>.
 * </table>
 *
 * <p><b>Character categories.</b>
 *
 * Character categories are specified using the POSIX-like syntax
 * '[:Lu:]'.  The complement of a category is specified by inserting
 * '^' after the opening '[:'.  The following category names are
 * recognized.  Actual determination of category data uses
 * <code>Character.getType()</code>, so it reflects the underlying
 * implmementation used by <code>Character</code>.  As of Java 2 and
 * JDK 1.1.8, this is Unicode 2.1.2.
 *
 * <pre>
 * Normative
 *     Mn = Mark, Non-Spacing
 *     Mc = Mark, Spacing Combining
 *     Me = Mark, Enclosing
 * 
 *     Nd = Number, Decimal Digit
 *     Nl = Number, Letter
 *     No = Number, Other
 * 
 *     Zs = Separator, Space
 *     Zl = Separator, Line
 *     Zp = Separator, Paragraph
 * 
 *     Cc = Other, Control
 *     Cf = Other, Format
 *     Cs = Other, Surrogate
 *     Co = Other, Private Use
 *     Cn = Other, Not Assigned
 * 
 * Informative
 *     Lu = Letter, Uppercase
 *     Ll = Letter, Lowercase
 *     Lt = Letter, Titlecase
 *     Lm = Letter, Modifier
 *     Lo = Letter, Other
 * 
 *     Pc = Punctuation, Connector
 *     Pd = Punctuation, Dash
 *     Ps = Punctuation, Open
 *     Pe = Punctuation, Close
 *    *Pi = Punctuation, Initial quote
 *    *Pf = Punctuation, Final quote
 *     Po = Punctuation, Other
 * 
 *     Sm = Symbol, Math
 *     Sc = Symbol, Currency
 *     Sk = Symbol, Modifier
 *     So = Symbol, Other
 * </pre>
 * *Unsupported by Java (and hence unsupported by UnicodeSet).
 *
 * @author Alan Liu
 * @version $RCSfile: UnicodeSet.java,v $ $Revision: 1.10 $ $Date: 2000/02/24 20:45:08 $
 */
public class UnicodeSet implements UnicodeFilter {
    /**
     * The internal representation is a StringBuffer of even length.
     * Each pair of characters represents a range that is included in
     * the set.  A single character c is represented as cc.  Thus, the
     * ranges in the set are (a,b), a and b inclusive, where a =
     * pairs.charAt(i) and b = pairs.charAt(i+1) for all even i, 0 <=
     * i <= pairs.length()-2.  Pairs are always stored in ascending
     * Unicode order.  Pairs are always stored in shortest form.  For
     * example, if the pair "hh", representing the single character
     * 'h', is added to the pairs list "agik", representing the ranges
     * 'a'-'g' and 'i'-'k', the result is "ak", not "aghhik".
     *
     * This representation format was originally used in Richard
     * Gillam's CharSet class.
     */
    private StringBuffer pairs;

    private static final String CATEGORY_NAMES =
        //                    1 1 1 1 1 1 1   1 1 2 2 2 2 2 2 2 2 2
        //0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6   8 9 0 1 2 3 4 5 6 7 8
        "CnLuLlLtLmLoMnMeMcNdNlNoZsZlZpCcCf--CoCsPdPsPePcPoSmScSkSo";

    private static final int UNSUPPORTED_CATEGORY = 17;

    private static final char VARIABLE_REF_OPEN = '{';
    private static final char VARIABLE_REF_CLOSE = '}';

    private static final int CATEGORY_COUNT = 29;

    /**
     * A cache mapping character category integers, as returned by
     * Character.getType(), to pairs strings.  Entries are initially
     * null and are created on demand.
     */
    private static final String[] CATEGORY_PAIRS_CACHE =
        new String[CATEGORY_COUNT];

    //----------------------------------------------------------------
    // Debugging and testing
    //----------------------------------------------------------------

    /**
     * Return the representation of this set as a list of character
     * ranges.  Ranges are listed in ascending Unicode order.  For
     * example, the set [a-zA-M3] is represented as "33AMaz".
     */
    public String getPairs() {
        return pairs.toString();
    }

    //----------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------

    /**
     * Constructs an empty set.
     */
    public UnicodeSet() {
        pairs = new StringBuffer();
    }

    /**
     * Constructs a copy of an existing set.
     */
    public UnicodeSet(UnicodeSet other) {
        pairs = new StringBuffer(other.getPairs());
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @exception java.lang.IllegalArgumentException if the pattern contains
     * a syntax error.
     */
    public UnicodeSet(String pattern) {
        applyPattern(pattern);
    }

    /**
     * Constructs a set from the given pattern.  See the class description
     * for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @param pos on input, the position in pattern at which to start parsing.
     * On output, the position after the last character parsed.
     * @param varNameToChar a mapping from variable names (String) to characters
     * (Character).  May be null.  If varCharToSet is non-null, then names may
     * map to either single characters or sets, depending on whether a mapping
     * exists in varCharToSet.  If varCharToSet is null then all names map to
     * single characters.
     * @param varCharToSet a mapping from characters (Character objects from
     * varNameToChar) to UnicodeSet objects.  May be null.  Is only used if
     * varNameToChar is also non-null.
     * @exception java.lang.IllegalArgumentException if the pattern
     * contains a syntax error.
     */
    public UnicodeSet(String pattern, ParsePosition pos, SymbolTable symbols) {
        applyPattern(pattern, pos, symbols);
    }

    /**
     * Constructs a set from the given Unicode character category.
     * @param category an integer indicating the character category as
     * returned by <code>Character.getType()</code>.
     * @exception java.lang.IllegalArgumentException if the given
     * category is invalid.
     */
    public UnicodeSet(int category) {
        if (category < 0 || category >= CATEGORY_COUNT ||
            category == UNSUPPORTED_CATEGORY) {
            throw new IllegalArgumentException("Invalid category");
        }
        pairs = new StringBuffer(getCategoryPairs(category));
    }

    /**
     * Modifies this set to represent the set specified by the given pattern.
     * See the class description for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @exception java.lang.IllegalArgumentException if the pattern
     * contains a syntax error.
     */
    public void applyPattern(String pattern) {
        ParsePosition pos = new ParsePosition(0);
        pairs = parse(pattern, pos, null);

        // Skip over trailing whitespace
        int i = pos.getIndex();
        int n = pattern.length();
        while (i < n && Character.isWhitespace(pattern.charAt(i))) {
            ++i;
        }

        if (i != n) {
            throw new IllegalArgumentException("Parse of \"" + pattern +
                                               "\" failed at " + i);
        }
    }

    /**
     * Modifies this set to represent the set specified by the given pattern.
     * @param pattern a string specifying what characters are in the set
     * @param pos on input, the position in pattern at which to start parsing.
     * On output, the position after the last character parsed.
     * @param varNameToChar a mapping from variable names (String) to characters
     * (Character).  May be null.  If varCharToSet is non-null, then names may
     * map to either single characters or sets, depending on whether a mapping
     * exists in varCharToSet.  If varCharToSet is null then all names map to
     * single characters.
     * @param varCharToSet a mapping from characters (Character objects from
     * varNameToChar) to UnicodeSet objects.  May be null.  Is only used if
     * varNameToChar is also non-null.
     * @exception java.lang.IllegalArgumentException if the pattern
     * contains a syntax error.
     */
    private void applyPattern(String pattern, ParsePosition pos, SymbolTable symbols) {
        pairs = parse(pattern, pos, symbols);
    }

    /**
     * Returns a string representation of this set.  If the result of
     * calling this function is passed to a UnicodeSet constructor, it
     * will produce another set that is equal to this one.
     */
    public String toPattern() {
        StringBuffer result = new StringBuffer();
        result.append('[');
        
        // iterate through the ranges in the UnicodeSet
        for (int i=0; i<pairs.length(); i+=2) {
            // for a range with the same beginning and ending point,
            // output that character, otherwise, output the start and
            // end points of the range separated by a dash
            result.append(pairs.charAt(i));
            if (pairs.charAt(i) != pairs.charAt(i+1)) {
                result.append('-').append(pairs.charAt(i+1));
            }
        }
        
        return result.append(']').toString();        
    }

    /**
     * Returns the number of elements in this set (its cardinality),
     * <em>n</em>, where <code>0 <= </code><em>n</em><code> <= 65536</code>.
     *
     * @return the number of elements in this set (its cardinality).
     */
    public int size() {
        int n = 0;
        for (int i=0; i<pairs.length(); i+=2) {
            n += pairs.charAt(i+1) - pairs.charAt(i) + 1;
        }
        return n;
    }

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     *
     * @return <tt>true</tt> if this set contains no elements.
     */
    public boolean isEmpty() {
        return pairs.length() == 0;
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified range
     * of chars.
     *
     * @return <tt>true</tt> if this set contains the specified range
     * of chars.
     */
    public boolean contains(char first, char last) {
        // Set i to the end of the smallest range such that its end
        // point >= last, or pairs.length() if no such range exists.
        int i = 1;
        while (i<pairs.length() && last>pairs.charAt(i)) i+=2;
        return i<pairs.length() && first>=pairs.charAt(i-1);
    }

    /**
     * Returns <tt>true</tt> if this set contains the specified char.
     *
     * @return <tt>true</tt> if this set contains the specified char.
     */
    public boolean contains(char c) {
        return contains(c, c);
    }

    /**
     * Returns <tt>true</tt> if this set contains any character whose low byte
     * is the given value.  This is used by <tt>RuleBasedTransliterator</tt> for
     * indexing.
     */
    public boolean containsIndexValue(int v) {
        /* The index value v, in the range [0,255], is contained in this set if
         * it is contained in any pair of this set.  Pairs either have the high
         * bytes equal, or unequal.  If the high bytes are equal, then we have
         * aaxx..aayy, where aa is the high byte.  Then v is contained if xx <=
         * v <= yy.  If the high bytes are unequal we have aaxx..bbyy, bb>aa.
         * Then v is contained if xx <= v || v <= yy.  (This is identical to the
         * time zone month containment logic.)
         */
        for (int i=0; i<pairs.length(); i+=2) {
            char low = pairs.charAt(i);
            char high = pairs.charAt(i+1);
            if ((low & 0xFF00) == (high & 0xFF00)) {
                if ((low & 0xFF) <= v && v <= (high & 0xFF)) {
                    return true;
                }
            } else if ((low & 0xFF) <= v || v <= (high & 0xFF)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds the specified range to this set if it is not already
     * present.  If this set already contains the specified range,
     * the call leaves this set unchanged.  If <code>last > first</code>
     * then an empty range is added, leaving the set unchanged.
     *
     * @param first first character, inclusive, of range to be added
     * to this set.
     * @param last last character, inclusive, of range to be added
     * to this set.
     */
    public void add(char first, char last) {
        if (first <= last) {
            addPair(pairs, first, last);
        }
    }

    /**
     * Adds the specified character to this set if it is not already
     * present.  If this set already contains the specified character,
     * the call leaves this set unchanged.
     */
    public final void add(char c) {
        add(c, c);
    }

    /**
     * Removes the specified range from this set if it is present.
     * The set will not contain the specified range once the call
     * returns.  If <code>last > first</code> then an empty range is
     * removed, leaving the set unchanged.
     * 
     * @param first first character, inclusive, of range to be removed
     * from this set.
     * @param last last character, inclusive, of range to be removed
     * from this set.
     */
    public void remove(char first, char last) {
        if (first <= last) {
            removePair(pairs, first, last);
        }
    }

    /**
     * Removes the specified character from this set if it is present.
     * The set will not contain the specified range once the call
     * returns.
     */
    public final void remove(char c) {
        remove(c, c);
    }

    /**
     * Returns <tt>true</tt> if the specified set is a <i>subset</i>
     * of this set.
     *
     * @param c set to be checked for containment in this set.
     * @return <tt>true</tt> if this set contains all of the elements of the
     * 	       specified set.
     */
    public boolean containsAll(UnicodeSet c) {
        // The specified set is a subset if all of its pairs are contained
        // in this set.
        int i = 1;
        for (int j=0; j<c.pairs.length(); j+=2) {
            char last = c.pairs.charAt(j+1);
            // Set i to the end of the smallest range such that its
            // end point >= last, or pairs.length() if no such range
            // exists.
            while (i<pairs.length() && last>pairs.charAt(i)) i+=2;
            if (i>pairs.length() || c.pairs.charAt(j) < pairs.charAt(i-1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Adds all of the elements in the specified set to this set if
     * they're not already present.  This operation effectively
     * modifies this set so that its value is the <i>union</i> of the two
     * sets.  The behavior of this operation is unspecified if the specified
     * collection is modified while the operation is in progress.
     *
     * @param c set whose elements are to be added to this set.
     * @see #add(char, char)
     */
    public void addAll(UnicodeSet c) {
        doUnion(pairs, c.pairs.toString());
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
    public void retainAll(UnicodeSet c) {
        doIntersection(pairs, c.pairs.toString());
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
    public void removeAll(UnicodeSet c) {
        doDifference(pairs, c.pairs.toString());
    }

    /**
     * Inverts this set.  This operation modifies this set so that
     * its value is its complement.  This is equivalent to the pseudo code:
     * <code>this = new UnicodeSet("[\u0000-\uFFFF]").removeAll(this)</code>.
     */
    public void complement() {
        doComplement(pairs);
    }

    /**
     * Removes all of the elements from this set.  This set will be
     * empty after this call returns.
     */
    public void clear() {
        pairs.setLength(0);
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
        return o instanceof UnicodeSet &&
            pairs.equals(((UnicodeSet)o).pairs);
    }

    /**
     * Returns the hash code value for this set.
     *
     * @return the hash code value for this set.
     * @see Object#hashCode()
     */
    public int hashCode() {
        return pairs.hashCode();
    }

    /**
     * Return a programmer-readable string representation of this object.
     */
    public String toString() {
        return getClass().getName() + '{' + toPattern() + '}';
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
     * point to the character following the closing ']', and a StringBuffer
     * containing a pairs list for the parsed pattern is returned.  This method
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
     * @return a StringBuffer containing a pairs list for the parsed substring
     * of <code>pattern</code>
     * @exception java.lang.IllegalArgumentException if the parse fails.
     */
    private static StringBuffer parse(String pattern, ParsePosition pos,
                                      SymbolTable symbols) {

        StringBuffer pairsBuf = new StringBuffer();
        boolean invert = false;

        int lastChar = -1; // This is either a char (0..FFFF) or -1
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
        int mode = 0;
        int openPos = 0; // offset to opening '['
        int i = pos.getIndex();
        int limit = pattern.length();
        for (; i<limit; ++i) {
            /* If the next element is a single character, c will be set to it,
             * and nestedPairs will be null.  In this case isLiteral indicates
             * whether the character should assume special meaning if it has
             * one.  If the next element is a nested set, either via a variable
             * reference, or via an embedded "[..]"  or "[:..:]" pattern, then
             * nestedPairs will be set to the pairs list for the nested set, and
             * c's value should be ignored.
             */
            char c = pattern.charAt(i);
            String nestedPairs = null;
            boolean isLiteral = false;

            // Ignore whitespace.  This is not Unicode whitespace, but Java
            // whitespace, a subset of Unicode whitespace.
            if (Character.isWhitespace(c)) {
                continue;
            }

            // Parse the opening '[' and optional following '^'
            switch (mode) {
            case 0:
                if (c == '[') {
                    mode = 1; // Next look for '^'
                    openPos = i;
                    continue;
                } else {
                    throw new IllegalArgumentException("Missing opening '['");
                }
            case 1:
                mode = 2;
                switch (c) {
                case '^':
                    invert = true;
                    continue; // Back to top to fetch next character
                case ':':
                    if (i == openPos+1) {
                        // '[:' cannot have whitespace in it
                        --i;
                        c = '[';
                        mode = 3;
                        // Fall through and parse category normally
                    }
                    break; // Fall through
                case '-':
                    isLiteral = true; // Treat leading '-' as a literal
                    break; // Fall through
                }
                // else fall through and parse this character normally
            }

            // After opening matter is parsed ("[", "[^", or "[:"), the mode
            // will be 2 if we want a closing ']', or 3 if we should parse a
            // category and close with ":]".

            /* Handle escapes.  If a character is escaped, then it assumes its
             * literal value.  This is true for all characters, both special
             * characters and characters with no special meaning.  We also
             * interpret '\\uxxxx' Unicode escapes here (as literals).
             */
            if (c == '\\') {
                ++i;
                if (i < limit) {
                    c = pattern.charAt(i);
                    isLiteral = true;
                    if (c == 'u') {
                        if ((i+4) >= limit) {
                            throw new IllegalArgumentException("Invalid \\u escape");
                        }
                        c = '\u0000';
                        for (int j=(++i)+4; i<j; ++i) { // [sic]
                            int digit = Character.digit(pattern.charAt(i), 16);
                            if (digit<0) {
                                throw new IllegalArgumentException("Invalid \\u escape");
                            }
                            c = (char) ((c << 4) | digit);
                        }
                        --i; // Move i back to last parsed character
                    }
                } else {
                    throw new IllegalArgumentException("Trailing '\\'");
                }
            }

            /* Parse variable references.  These are treated as literals.  If a
             * variable refers to a UnicodeSet, nestedPairs is assigned here.
             * Variable names are only parsed if varNameToChar is not null.
             * Set variables are only looked up if varCharToSet is not null.
             */
            else if (symbols != null && !isLiteral && c == VARIABLE_REF_OPEN) {
                ++i;
                int j = pattern.indexOf(VARIABLE_REF_CLOSE, i);
                if (i == j || j < 0) { // empty or unterminated
                    throw new IllegalArgumentException("Illegal variable reference");
                }
                String name = pattern.substring(i, j);
                Object obj = symbols.lookup(name);
                if (obj == null) {
                    throw new IllegalArgumentException("Undefined variable: "
                                                       + name);
                }
                isLiteral = true;
                if (obj instanceof Character) {
                    c = ((Character) obj).charValue();
                } else {
                    nestedPairs = ((UnicodeSet) obj).pairs.toString();
                }
                i = j+1; // Make i point to ']'
            }

            /* An opening bracket indicates the first bracket of a nested
             * subpattern, either a normal pattern or a category pattern.  We
             * recognize these here and set nestedPairs accordingly.
             */
            else if (!isLiteral && c == '[') {
                // Handle "[:...:]", representing a character category
                char d = charAfter(pattern, i);
                if (d == ':') {
                    i += 2;
                    int j = pattern.indexOf(":]", i);
                    if (j < 0) {
                        throw new IllegalArgumentException("Missing \":]\"");
                    }
                    nestedPairs = getCategoryPairs(pattern.substring(i, j));
                    i = j+1; // Make i point to ']' in ":]"
                    if (mode == 3) {
                        // Entire pattern is a category; leave parse loop
                        pairsBuf.append(nestedPairs);
                        break;
                    }
                } else {
                    // Recurse to get the pairs for this nested set.
                    pos.setIndex(i); // Add 2 to point AFTER op
                    nestedPairs = parse(pattern, pos, symbols).toString();
                    i = pos.getIndex() - 1; // - 1 to point at ']'
                }
            }

            /* At this point we have either a character c, or a nested set.  If
             * we have encountered a nested set, either embedded in the pattern,
             * or as a variable, we have a non-null nestedPairs, and c should be
             * ignored.  Otherwise c is the current character, and isLiteral
             * indicates whether it is an escaped literal (or variable) or a
             * normal unescaped character.  Unescaped characters '-', '&', and
             * ']' have special meanings.
             */
            if (nestedPairs != null) {
                if (lastChar >= 0) {
                    if (lastOp != 0) {
                        throw new IllegalArgumentException("Illegal rhs for " + lastChar + lastOp);
                    }
                    addPair(pairsBuf, (char)lastChar, (char)lastChar);
                    lastChar = -1;
                }
                switch (lastOp) {
                case '-':
                    doDifference(pairsBuf, nestedPairs);
                    break;
                case '&':
                    doIntersection(pairsBuf, nestedPairs);
                    break;
                case 0:
                    doUnion(pairsBuf, nestedPairs);
                    break;
                }
                lastOp = 0;
            } else if (!isLiteral && c == ']') {
                // Final closing delimiter.  This is the only way we leave this
                // loop if the pattern is well-formed.
                break;
            } else if (lastOp == 0 && !isLiteral && (c == '-' || c == '&')) {
                lastOp = c;
            } else if (lastOp == '-') {
                addPair(pairsBuf, (char)lastChar, c);
                lastOp = 0;
                lastChar = -1;
            } else if (lastOp != 0) {
                // We have <set>&<char> or <char>&<char>
                throw new IllegalArgumentException("Unquoted " + lastOp);
            } else {
                if (lastChar >= 0) {
                    // We have <char><char>
                    addPair(pairsBuf, (char)lastChar, (char)lastChar);
                }
                lastChar = c;
            }
        }

        // Handle unprocessed stuff preceding the closing ']'
        if (lastOp == '-') {
            // Trailing '-' is treated as literal
            addPair(pairsBuf, lastOp, lastOp);
        } else if (lastOp == '&') {
            throw new IllegalArgumentException("Unquoted trailing " + lastOp);
        }
        if (lastChar >= 0) {
            addPair(pairsBuf, (char)lastChar, (char)lastChar);                    
        }

        /**
         * If we saw a '^' after the initial '[' of this pattern, then perform
         * the complement.  (Inversion after '[:' is handled elsewhere.)
         */
        if (invert) {
            doComplement(pairsBuf);
        }

        /**
         * i indexes the last character we parsed or is pattern.length().  In
         * the latter case, we have run off the end without finding a closing
         * ']'.  Otherwise, we know i < pattern.length(), and we set the
         * ParsePosition to the next character to be parsed.
         */
        if (i == limit) {
            throw new IllegalArgumentException("Missing ']'");
        }
        pos.setIndex(i+1);

        return pairsBuf;
    }

    //----------------------------------------------------------------
    // Implementation: Efficient in-place union & difference
    //----------------------------------------------------------------

    /**
     * Performs a union operation: adds the range 'c'-'d' to the given
     * pairs list.  The pairs list is modified in place.  The result
     * is normalized (in order and as short as possible).  For
     * example, addPair("am", 'l', 'q') => "aq".  addPair("ampz", 'n',
     * 'o') => "az".
     */
    private static void addPair(StringBuffer pairs, char c, char d) {
        char a = 0;
        char b = 0;
        for (int i=0; i<pairs.length(); i+=2) {
            char e = pairs.charAt(i);
            char f = pairs.charAt(i+1);
            if (e <= (d+1) && c <= (f+1)) {
                // Merge with this range
                f = (char) Math.max(d, f);

                // Check to see if we need to merge with the
                // subsequent range also.  This happens if we have
                // "abdf" and are merging in "cc".  We only need to
                // check on the right side -- never on the left.
                if ((i+2) < pairs.length() &&
                    pairs.charAt(i+2) == (f+1)) {
                    f = pairs.charAt(i+3);
                    stringBufferDelete(pairs, i+2, i+4);
                }
                pairs.setCharAt(i, (char) Math.min(c, e));
                pairs.setCharAt(i+1, f);
                return;
            } else if ((b+1) < c && (d+1) < e) {
                // Insert before this range
                pairs.insert(i, new char[] { c, d });
                return;
            }
            a = e;
            b = f;
        }
        // If nothing else, fall through and append this new range to
        // the end.
        pairs.append(c).append(d);
    }

    /**
     * Performs an asymmetric difference: removes the range 'c'-'d'
     * from the pairs list.  The pairs list is modified in place.  The
     * result is normalized (in order and as short as possible).  For
     * example, removePair("am", 'l', 'q') => "ak".
     * removePair("ampz", 'l', 'q') => "akrz".
     */
    private static void removePair(StringBuffer pairs, char c, char d) {
        // Iterate over pairs until we find a pair that overlaps
        // with the given range.
        for (int i=0; i<pairs.length(); i+=2) {
            char b = pairs.charAt(i+1);
            if (b < c) {
                // Range at i is entirely before the given range,
                // since we have a-b < c-d.  No overlap yet...keep
                // iterating.
                continue;
            }
            char a = pairs.charAt(i);
            if (d < a) {
                // Range at i is entirely after the given range; c-d <
                // a-b.  Since ranges are in order, nothing else will
                // overlap.
                break;
            }
            // Once we get here, we know c <= b and d >= a.
            // rangeEdited is set to true if we have modified the
            // range a-b (the range at i) in place.
            boolean rangeEdited = false;
            if (c > a) {
                // If c is after a and before b, then we have overlap
                // of this sort: a--c==b--d or a--c==d--b, where a-b
                // and c-d are the ranges of interest.  We need to
                // add the range a,c-1.
                pairs.setCharAt(i+1, (char)(c-1));
                // i is already a
                rangeEdited = true;
            }
            if (d < b) {
                // If d is after a and before b, we overlap like this:
                // c--a==d--b or a--c==d--b, where a-b is the range at
                // i and c-d is the range being removed.  We need to
                // add the range d+1,b.
                if (rangeEdited) {
                    pairs.insert(i+2, new char[] { (char)(d+1), b });
                    i += 2;
                } else {
                    pairs.setCharAt(i, (char)(d+1));
                    // i+1 is already b
                    rangeEdited = true;
                }
            }
            if (!rangeEdited) {
                // If we didn't add any ranges, that means the entire
                // range a-b must be deleted, since we have
                // c--a==b--d.
                stringBufferDelete(pairs, i, i+2);
                i -= 2;
            }
        }
    }

    //----------------------------------------------------------------
    // Implementation: Fundamental operators
    //----------------------------------------------------------------

    /**
     * Changes the pairs list to represent the complement of the set it
     * currently represents.  The pairs list will be normalized (in
     * order and in shortest possible form) if the original pairs list
     * was normalized.
     */
    private static void doComplement(StringBuffer pairs) {
        if (pairs.length() == 0) {
            pairs.append('\u0000').append('\uffff');
            return;
        }

        // Change each end to a start and each start to an end of the
        // gaps between the ranges.  That is, 3-7 9-12 becomes x-2 8-8
        // 13-x, where 'x' represents a range that must now be fixed
        // up.
        for (int i=0; i<pairs.length(); i+=2) {
            pairs.setCharAt(i,   (char) (pairs.charAt(i)   - 1));
            pairs.setCharAt(i+1, (char) (pairs.charAt(i+1) + 1));
        }

        // Fix up the initial range, either by adding a start point of
        // U+0000, or by deleting the range altogether, if the
        // original range was U+0000 - x.
        if (pairs.charAt(0) == '\uFFFF') {
            stringBufferDelete(pairs, 0, 1);
        } else {
            pairs.insert(0, '\u0000');
        }

        // Fix up the final range, either by adding an end point of
        // U+FFFF, or by deleting the range altogether, if the
        // original range was x - U+FFFF.
        if (pairs.charAt(pairs.length() - 1) == '\u0000') {
            pairs.setLength(pairs.length() - 1);
        } else {
            pairs.append('\uFFFF');
        }
    }

    /**
     * Given two pairs lists, changes the first in place to represent
     * the union of the two sets.
     *
     * This implementation format was stolen from Richard Gillam's
     * CharSet class.
     */
    private static void doUnion(StringBuffer pairs, String c2) {
        StringBuffer result = new StringBuffer();
        String c1 = pairs.toString();

        int i = 0;
        int j = 0;

        // consider all the characters in both strings
        while (i < c1.length() && j < c2.length()) {
            char ub;
            
            // the first character in the result is the lower of the
            // starting characters of the two strings, and "ub" gets
            // set to the upper bound of that range
            if (c1.charAt(i) < c2.charAt(j)) {
                result.append(c1.charAt(i));
                ub = c1.charAt(++i);
            }
            else {
                result.append(c2.charAt(j));
                ub = c2.charAt(++j);
            }
            
            // for as long as one of our two pointers is pointing to a range's
            // end point, or i is pointing to a character that is less than
            // "ub" plus one (the "plus one" stitches touching ranges together)...
            while (i % 2 == 1 || j % 2 == 1 || (i < c1.length() && c1.charAt(i)
                            <= ub + 1)) {
                // advance i to the first character that is greater than
                // "ub" plus one
                while (i < c1.length() && c1.charAt(i) <= ub + 1)
                    ++i;
                    
                // if i points to the endpoint of a range, update "ub"
                // to that character, or if i points to the start of
                // a range and the endpoint of the preceding range is
                // greater than "ub", update "up" to _that_ character
                if (i % 2 == 1)
                    ub = c1.charAt(i);
                else if (i > 0 && c1.charAt(i - 1) > ub)
                    ub = c1.charAt(i - 1);

                // now advance j to the first character that is greater
                // that "ub" plus one
                while (j < c2.length() && c2.charAt(j) <= ub + 1)
                    ++j;
                    
                // if j points to the endpoint of a range, update "ub"
                // to that character, or if j points to the start of
                // a range and the endpoint of the preceding range is
                // greater than "ub", update "up" to _that_ character
                if (j % 2 == 1)
                    ub = c2.charAt(j);
                else if (j > 0 && c2.charAt(j - 1) > ub)
                    ub = c2.charAt(j - 1);
            }
            // when we finally fall out of this loop, we will have stitched
            // together a series of ranges that overlap or touch, i and j
            // will both point to starting points of ranges, and "ub" will
            // be the endpoint of the range we're working on.  Write "ub"
            // to the result
            result.append(ub);
            
        // loop back around to create the next range in the result
        }
        
        // we fall out to here when we've exhausted all the characters in
        // one of the operands.  We can append all of the remaining characters
        // in the other operand without doing any extra work.
        if (i < c1.length())
            result.append(c1.substring(i));
        if (j < c2.length())
            result.append(c2.substring(j));

        pairs.setLength(0);
        pairs.append(result.toString());
    }

    /**
     * Given two pairs lists, changes the first in place to represent
     * the asymmetric difference of the two sets.
     */
    private static void doDifference(StringBuffer pairs, String pairs2) {
        StringBuffer p2 = new StringBuffer(pairs2);
        doComplement(p2);
        doIntersection(pairs, p2.toString());
    }

    /**
     * Given two pairs lists, changes the first in place to represent
     * the intersection of the two sets.
     *
     * This implementation format was stolen from Richard Gillam's
     * CharSet class.
     */
    private static void doIntersection(StringBuffer pairs, String c2) {
        StringBuffer result = new StringBuffer();
        String c1 = pairs.toString();

        int i = 0;
        int j = 0;
        int oldI;
        int oldJ;

        // iterate until we've exhausted one of the operands
        while (i < c1.length() && j < c2.length()) {
            
            // advance j until it points to a character that is larger than
            // the one i points to.  If this is the beginning of a one-
            // character range, advance j to point to the end
            if (i < c1.length() && i % 2 == 0) {
                while (j < c2.length() && c2.charAt(j) < c1.charAt(i))
                    ++j;
                if (j < c2.length() && j % 2 == 0 && c2.charAt(j) == c1.charAt(i))
                    ++j;
            }

            // if j points to the endpoint of a range, save the current
            // value of i, then advance i until it reaches a character
            // which is larger than the character pointed at
            // by j.  All of the characters we've advanced over (except
            // the one currently pointed to by i) are added to the result
            oldI = i;
            while (j % 2 == 1 && i < c1.length() && c1.charAt(i) <= c2.charAt(j))
                ++i;
            result.append(c1.substring(oldI, i));

            // if i points to the endpoint of a range, save the current
            // value of j, then advance j until it reaches a character
            // which is larger than the character pointed at
            // by i.  All of the characters we've advanced over (except
            // the one currently pointed to by i) are added to the result
            oldJ = j;
            while (i % 2 == 1 && j < c2.length() && c2.charAt(j) <= c1.charAt(i))
                ++j;
            result.append(c2.substring(oldJ, j));

            // advance i until it points to a character larger than j
            // If it points at the beginning of a one-character range,
            // advance it to the end of that range
            if (j < c2.length() && j % 2 == 0) {
                while (i < c1.length() && c1.charAt(i) < c2.charAt(j))
                    ++i;
                if (i < c1.length() && i % 2 == 0 && c2.charAt(j) == c1.charAt(i))
                    ++i;
            }
        }

        pairs.setLength(0);
        pairs.append(result.toString());
    }

    //----------------------------------------------------------------
    // Implementation: Generation of pairs for Unicode categories
    //----------------------------------------------------------------
    
    /**
     * Returns a pairs string for the given category, given its name.
     * The category name must be either a two-letter name, such as
     * "Lu", or a one letter name, such as "L".  One-letter names
     * indicate the logical union of all two-letter names that start
     * with that letter.  Case is significant.  If the name starts
     * with the character '^' then the complement of the given
     * character set is returned.
     *
     * Although individual categories such as "Lu" are cached, we do
     * not currently cache single-letter categories such as "L" or
     * complements such as "^Lu" or "^L".  It would be easy to cache
     * these as well in a hashtable should the need arise.
     */
    private static String getCategoryPairs(String catName) {
        boolean invert = (catName.length() > 1 &&
                          catName.charAt(0) == '^');
        if (invert) {
            catName = catName.substring(1);
        }

        StringBuffer cat = null;
        
        // if we have two characters, search the category map for that
        // code and either construct and return a UnicodeSet from the
        // data in the category map or throw an exception
        if (catName.length() == 2) {
            int i = CATEGORY_NAMES.indexOf(catName);
            if (i>=0 && i%2==0) {
                i /= 2;
                if (i != UNSUPPORTED_CATEGORY) {
                    String pairs = getCategoryPairs(i);
                    if (!invert) {
                        return pairs;
                    }
                    cat = new StringBuffer(pairs);
                }
            }
        } else if (catName.length() == 1) {
            // if we have one character, search the category map for
            // codes beginning with that letter, and union together
            // all of the matching sets that we find (or throw an
            // exception if there are no matches)
            for (int i=0; i<CATEGORY_COUNT; ++i) {
                if (i != UNSUPPORTED_CATEGORY &&
                    CATEGORY_NAMES.charAt(2*i) == catName.charAt(0)) {
                    String pairs = getCategoryPairs(i);
                    if (cat == null) {
                        cat = new StringBuffer(pairs);
                    } else {
                        doUnion(cat, pairs);
                    }
                }
            }
        }

        if (cat == null) {
            throw new IllegalArgumentException("Bad category");            
        }

        if (invert) {
            doComplement(cat);
        }
        return cat.toString();
    }

    /**
     * Returns a pairs string for the given category.  This string is
     * cached and returned again if this method is called again with
     * the same parameter.
     */
    private static String getCategoryPairs(int cat) {
        if (CATEGORY_PAIRS_CACHE[cat] == null) {
            // Walk through all Unicode characters, noting the start
            // and end of each range for which Character.getType(c)
            // returns the given category integer.  Since we are
            // iterating in order, we can simply append the resulting
            // ranges to the pairs string.
            StringBuffer pairs = new StringBuffer();
            int first = -1;
            int last = -2;
            for (int i=0; i<=0xFFFF; ++i) {
                if (Character.getType((char)i) == cat) {
                    if ((last+1) == i) {
                        last = i;
                    } else {
                        if (first >= 0) {
                            pairs.append((char)first).append((char)last);
                        }
                        first = last = i;
                    }
                }
            }
            if (first >= 0) {
                pairs.append((char)first).append((char)last);
            }
            CATEGORY_PAIRS_CACHE[cat] = pairs.toString();
        }
        return CATEGORY_PAIRS_CACHE[cat];
    }

    //----------------------------------------------------------------
    // Implementation: Utility methods
    //----------------------------------------------------------------

    /**
     * Returns the character after the given position, or '\uFFFF' if
     * there is none.
     */
    private static final char charAfter(String str, int i) {
        return ((++i) < str.length()) ? str.charAt(i) : '\uFFFF';
    }
    
    /**
     * Deletes a range of character from a StringBuffer, from start to
     * limit-1.  This is not part of JDK 1.1 StringBuffer, but is
     * present in Java 2.
     * @param start inclusive start of range
     * @param limit exclusive end of range
     */
    private static void stringBufferDelete(StringBuffer buf,
                                           int start, int limit) {
        // In Java 2 just use:
        //   buf.delete(start, limit);
        char[] chars = null;
        if (buf.length() > limit) {
            chars = new char[buf.length() - limit];
            buf.getChars(limit, buf.length(), chars, 0);
        }
        buf.setLength(start);
        if (chars != null) {
            buf.append(chars);
        }
    }
}
