#include "uniset.h"

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
 * <blockquote><code>
 * pattern := ('[' '^'? item* ']') | ('[:' '^'? category ':]')<br>
 * item := char | (char '-' char) | pattern-expr<br>
 * pattern-expr := pattern | pattern-expr pattern | pattern-expr op pattern<br>
 * op := '&' | '-'<br>
 * special := '[' | ']' | '-'<br>
 * char := <em>any character that is not</em> special |
 *        ('\' <em>any character</em>) |
 *        ('\\u' hex hex hex hex)<br>
 * hex := <em>any hex digit, as defined by </em>Character.digit(c, 16)
 * </code>
 *
 * <br>Legend:
 *
 * <table>
 * <tr><td width=20%><code>a:=b</code>
 * <td><code>a</code> may be replaced by
 * <code>b</code>
 * <tr><td><code>a?</code>
 * <td>zero or one instance of <code>a</code><br>
 * <tr><td><code>a*</code>
 * <td>one or more instances of <code>a</code><br>
 * <tr><td><code>a|b</code>
 * <td>either <code>a</code> or <code>b</code><br>
 * <tr><td><code>'a'</code>
 * <td>the literal string between the quotes
 * </table>
 * </blockquote>
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
 * literal.  Thus "[a\-b]", "[-ab]", and "[ab-]" all indicate the same
 * set of three characters, 'a', 'b', and '-'.
 *
 * <p>Sets may be intersected using the '&' operator or the asymmetric
 * set difference may be taken using the '-' operator, for example,
 * "[[:L:]&[\u0000-\u0FFF]]" indicates the set of all Unicode letters
 * with values less than 4096.  Operators ('&' and '|') have equal
 * precedence and bind left-to-right.  Thus
 * "[[:L:]-[a-z]-[\u0100-\u01FF]]" is equivalent to
 * "[[[:L:]-[a-z]]-[\u0100-\u01FF]]".  This only really matters for
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
 * JDK 1.1.8, this is Unicode <b>2.x.x - fill in version here</b>.
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
 *     Pi = Punctuation, Initial quote
 *     Pf = Punctuation, Final quote
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
 * @version $RCSfile: uniset.cpp,v $ $Revision: 1.1 $ $Date: 1999/10/20 22:06:52 $
 */

// Note: This mapping is different in ICU and Java
const UnicodeString UnicodeSet::CATEGORY_NAMES(
    "CnLuLlLtLmLoMnMeMcNdNlNoZsZlZpCcCfCoCsPdPsPePcPoSmScSkSoPiPf");

/**
 * A cache mapping character category integers, as returned by
 * Character.getType(), to pairs strings.  Entries are initially
 * null and are created on demand.
 */
UnicodeString* UnicodeSet::CATEGORY_PAIRS_CACHE =
     new UnicodeString[Unicode::GENERAL_TYPES_COUNT];

//----------------------------------------------------------------
// Debugging and testing
//----------------------------------------------------------------

/**
 * Return the representation of this set as a list of character
 * ranges.  Ranges are listed in ascending Unicode order.  For
 * example, the set [a-zA-M3] is represented as "33AMaz".
 */
const UnicodeString& UnicodeSet::getPairs() const {
    return pairs;
}

//----------------------------------------------------------------
// Public API
//----------------------------------------------------------------

/**
 * Constructs an empty set.
 */
UnicodeSet::UnicodeSet() {}

/**
 * Constructs a set from the given pattern, optionally ignoring
 * white space.  See the class description for the syntax of the
 * pattern language.
 * @param pattern a string specifying what characters are in the set
 * @param ignoreSpaces if <code>true</code>, all spaces in the
 * pattern are ignored, except those preceded by '\\'.  Spaces are
 * those characters for which <code>Character.isSpaceChar()</code>
 * is <code>true</code>.
 * @exception <code>IllegalArgumentException</code> if the pattern
 * contains a syntax error.
 */
UnicodeSet::UnicodeSet(const UnicodeString& pattern, bool_t ignoreSpaces,
                       UErrorCode& status) {
    applyPattern(pattern, ignoreSpaces, status);
}

UnicodeSet::UnicodeSet(const UnicodeString& pattern,
                       UErrorCode& status) {
    applyPattern(pattern, status);
}

/**
 * Constructs a set from the given Unicode character category.
 * @param category an integer indicating the character category as
 * returned by <code>Character.getType()</code>.
 * @exception <code>IllegalArgumentException</code> if the given
 * category is invalid.
 */
UnicodeSet::UnicodeSet(int8_t category, UErrorCode& status) {
    if (U_SUCCESS(status)) {
        if (category < 0 || category >= Unicode::GENERAL_TYPES_COUNT) {
            status = U_ILLEGAL_ARGUMENT_ERROR;
        } else {
            pairs = getCategoryPairs(category);
        }
    }
}

/**
 * Modifies this set to represent the set specified by the given
 * pattern, optionally ignoring white space.  See the class
 * description for the syntax of the pattern language.
 * @param pattern a string specifying what characters are in the set
 * @param ignoreSpaces if <code>true</code>, all spaces in the
 * pattern are ignored.  Spaces are those characters for which
 * <code>Character.isSpaceChar()</code> is <code>true</code>.
 * Characters preceded by '\\' are escaped, losing any special
 * meaning they otherwise have.  Spaces may be included by
 * escaping them.
 * @exception <code>IllegalArgumentException</code> if the pattern
 * contains a syntax error.
 */
void UnicodeSet::applyPattern(const UnicodeString& pattern,
                              bool_t ignoreSpaces,
                              UErrorCode& status) {
    if (U_FAILURE(status)) {
        return;
    }

    ParsePosition pos(0);
	UnicodeString* pat = (UnicodeString*) &pattern;

    // To ignore spaces, create a new pattern without spaces.  We
    // have to process all '\' escapes.  If '\' is encountered,
    // insert it and the following character (if any -- let parse
    // deal with any syntax errors) in the pattern.  This allows
    // escaped spaces.
    if (ignoreSpaces) {
		pat = new UnicodeString();
        for (int32_t i=0; i<pattern.length(); ++i) {
            UChar c = pattern.charAt(i);
            if (Unicode::isSpaceChar(c)) {
                continue;
            }
            if (c == '\\' && (i+1) < pattern.length()) {
                pat->append(c);
                c = pattern.charAt(++i);
                // Fall through and append the following char
            }
            pat->append(c);
        }
    }

    parse(pairs, *pat, pos, status);
    if (pos.getIndex() != pat->length()) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
    }
	if (pat != &pattern) {
		delete pat;
	}
}

/**
 * Returns a string representation of this set.  If the result of
 * calling this function is passed to a UnicodeSet constructor, it
 * will produce another set that is equal to this one.
 */
UnicodeString& UnicodeSet::toPattern(UnicodeString& result) const {
    result.remove().append((UChar)'[');
    
    // iterate through the ranges in the CharSet
    for (int32_t i=0; i<pairs.length(); i+=2) {
        // for a range with the same beginning and ending point,
        // output that character, otherwise, output the start and
        // end points of the range separated by a dash
        result.append(pairs.charAt(i));
        if (pairs.charAt(i) != pairs.charAt(i+1)) {
            result.append((UChar)'-').append(pairs.charAt(i+1));
        }
    }
    
    return result.append((UChar)']');
}

/**
 * Returns the number of elements in this set (its cardinality).  If this
 * set contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
 * <tt>Integer.MAX_VALUE</tt>.
 *
 * @return the number of elements in this set (its cardinality).
 */
int32_t UnicodeSet::size() const {
    int32_t n = 0;
    for (int32_t i=0; i<pairs.length(); i+=2) {
        n += pairs.charAt(i+1) - pairs.charAt(i) + 1;
    }
    return n;
}

/**
 * Returns <tt>true</tt> if this set contains no elements.
 *
 * @return <tt>true</tt> if this set contains no elements.
 */
bool_t UnicodeSet::isEmpty() const {
    return pairs.length() == 0;
}

/**
 * Returns <tt>true</tt> if this set contains the specified range
 * of chars.
 *
 * @return <tt>true</tt> if this set contains the specified range
 * of chars.
 */
bool_t UnicodeSet::contains(UChar first, UChar last) const {
    // Set i to the end of the smallest range such that its end
    // point >= last, or pairs.length() if no such range exists.
    int32_t i = 1;
    while (i<pairs.length() && last>pairs.charAt(i)) i+=2;
    return i<pairs.length() && first>=pairs.charAt(i-1);
}

/**
 * Returns <tt>true</tt> if this set contains the specified char.
 *
 * @return <tt>true</tt> if this set contains the specified char.
 */
bool_t UnicodeSet::contains(UChar c) const {
    return contains(c, c);
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
void UnicodeSet::add(UChar first, UChar last) {
    if (first <= last) {
        addPair(pairs, first, last);
    }
}

/**
 * Adds the specified character to this set if it is not already
 * present.  If this set already contains the specified character,
 * the call leaves this set unchanged.
 */
void UnicodeSet::add(UChar c) {
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
void UnicodeSet::remove(UChar first, UChar last) {
    if (first <= last) {
        removePair(pairs, first, last);
    }
}

/**
 * Removes the specified character from this set if it is present.
 * The set will not contain the specified range once the call
 * returns.
 */
void UnicodeSet::remove(UChar c) {
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
bool_t UnicodeSet::containsAll(const UnicodeSet& c) const {
    // The specified set is a subset if all of its pairs are contained
    // in this set.
    int32_t i = 1;
    for (int32_t j=0; j<c.pairs.length(); j+=2) {
        UChar last = c.pairs.charAt(j+1);
        // Set i to the end of the smallest range such that its
        // end point >= last, or pairs.length() if no such range
        // exists.
        while (i<pairs.length() && last>pairs.charAt(i)) i+=2;
        if (i>pairs.length() || c.pairs.charAt(j) < pairs.charAt(i-1)) {
            return FALSE;
        }
    }
    return TRUE;
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
void UnicodeSet::addAll(const UnicodeSet& c) {
    doUnion(pairs, c.pairs);
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
void UnicodeSet::retainAll(const UnicodeSet& c) {
    doIntersection(pairs, c.pairs);
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
void UnicodeSet::removeAll(const UnicodeSet& c) {
    doDifference(pairs, c.pairs);
}

/**
 * Inverts this set.  This operation modifies this set so that
 * its value is its complement.  This is equivalent to the pseudo code:
 * <code>this = new CharSet("[\u0000-\uFFFF]").removeAll(this)</code>.
 */
void UnicodeSet::complement() {
    doComplement(pairs);
}

/**
 * Removes all of the elements from this set.  This set will be
 * empty after this call returns.
 */
void UnicodeSet::clear() {
    pairs.remove();
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
bool_t UnicodeSet::operator==(const UnicodeSet& o) const {
    return pairs == o.pairs;
}

/**
 * Returns the hash code value for this set.
 *
 * @return the hash code value for this set.
 * @see Object#hashCode()
 */
int32_t UnicodeSet::hashCode() const {
    return pairs.hashCode();
}

//----------------------------------------------------------------
// Implementation: Pattern parsing
//----------------------------------------------------------------

/**
 * Parses the given pattern, starting at the given position.  The
 * character at pattern.charAt(pos.getIndex()) must be '[', or the
 * parse fails.  Parsing continues until the corresponding closing
 * ']'.  If a syntax error is encountered between the opening and
 * closing brace, the parse fails.  Upon return from a U_SUCCESSful
 * parse, the ParsePosition is updated to point to the character
 * following the closing ']', and a StringBuffer containing a
 * pairs list for the parsed pattern is returned.  This method calls
 * itself recursively to parse embedded subpatterns.
 *
 * @param pattern the string containing the pattern to be parsed.
 * The portion of the string from pos.getIndex(), which must be a
 * '[', to the corresponding closing ']', is parsed.
 * @param pos upon entry, the position at which to being parsing.
 * The character at pattern.charAt(pos.getIndex()) must be a '['.
 * Upon return from a U_SUCCESSful parse, pos.getIndex() is either
 * the character after the closing ']' of the parsed pattern, or
 * pattern.length() if the closing ']' is the last character of
 * the pattern string.
 * @return a StringBuffer containing a pairs list for the parsed
 * substring of <code>pattern</code>
 * @exception IllegalArgumentException if the parse fails.
 */
UnicodeString& UnicodeSet::parse(UnicodeString& pairsBuf /*result*/,
                                 const UnicodeString& pattern,
                                 ParsePosition& pos,
                                 UErrorCode& status) {
    if (U_FAILURE(status)) {
        return pairsBuf;
    }

    bool_t invert = FALSE;
    pairsBuf.remove();

    /**
     * Nodes:  0 - idle, waiting for '['
     *        10 - like 11, but immediately after "[" or "[^"
     *        11 - awaiting x, "]", "[...]", or "[:...:]"
     *        21 - after x
     *        23 - after x-
     * 
     * The parsing state machine moves from node 0 through zero or more
     * other nodes back to node 0, in a U_SUCCESSful parse.
     */
    int32_t node = 0;
    UChar first = 0;
    int32_t i;
    /**
     * This loop iterates over the characters in the pattern.  We
     * start at the position specified by pos.  We exit the loop
     * when either a matching closing ']' is seen, or we read all
     * characters of the pattern.
     */
    for (i=pos.getIndex(); i<pattern.length(); ++i) {
        UChar c = pattern.charAt(i);            /**
         * Handle escapes here.  If a character is escaped, then
         * it assumes its literal value.  This is true for all
         * characters, both special characters and characters with
         * no special meaning.  We also interpret '\\uxxxx' Unicode
         * escapes here.
         */
        bool_t isLiteral = FALSE;
        if (c == '\\') {
            ++i;
            if (i < pattern.length()) {
                c = pattern.charAt(i);
                isLiteral = TRUE;
                if (c == 'u') {
                    if ((i+4) >= pattern.length()) {
						status = U_ILLEGAL_ARGUMENT_ERROR;
						return pairsBuf;
                    }
                    c = (UChar)0x0000;
                    for (int32_t j=(++i)+4; i<j; ++i) { // [sic]
						// TO DO: Change this to use Unicode::digit()
						// when that method exists.
                        int32_t digit = /*Unicode::*/UnicodeSet::digit(pattern.charAt(i), 16);
                        if (digit<0) {
                            status = U_ILLEGAL_ARGUMENT_ERROR;
                            return pairsBuf;
                        }
                        c = (UChar) ((c << 4) | digit);
                    }
                    --i; // Move i back to last parsed character
                }
            } else {
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return pairsBuf;
            }
        }
        /**
         * Within this loop, we handle each of the four
         * conditions: '[', ']', '-', other.  The first three
         * characters must not be escaped.
         */

        /**
         * An opening bracket indicates either the first bracket
         * of the entire subpattern we are parsing, in which case
         * we are in node 0 and move into node 10.  We also check
         * for an immediately following '^', indicating the
         * complement of the following pattern.  ('^' is any other
         * position has no special meaning.)  If we are not in
         * node 0, '[' represents a nested subpattern that must be
         * recursively parsed and checked for following operators
         * ('&' or '|').  If two nested subpatterns follow one
         * another with no operator, their union is formed, just
         * as with any other elements that follow one another
         * without intervening operator.  The other thing we
         * handle here is the syntax "[:Xx:]" or "[:X:]" that
         * indicates a Unicode category or supercategory.
         */
        if (!isLiteral && c == '[') {
            bool_t parseOp = FALSE;
            UChar d = charAfter(pattern, i);
            // "[:...:]" represents a character category
            if (d == ':') {
                if (node == 23) {
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return pairsBuf;
                }
                if (node == 21) {
                    addPair(pairsBuf, first, first);
                    node = 11;
                }
                i += 2;
                int32_t j = pattern.indexOf(":]", i);
                if (j < 0) {
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return pairsBuf;
                }
                UnicodeString categoryName;
                pattern.extract(i, j-i, categoryName);
                UnicodeString temp;
                doUnion(pairsBuf,
                        getCategoryPairs(temp, categoryName, status));
                if (U_FAILURE(status)) {
                    return pairsBuf;
                }
                i = j+1;
                if (node == 10) {
                    node = 11;
                    parseOp = TRUE;
                } else if (node == 0) {
                    break;
                }
            } else {
                if (node == 0) {
                    node = 10;
                    if (d == '^') {
                        invert = TRUE;
                        ++i;
                    }
                } else {
                    // Nested '['
                    pos.setIndex(i);
                    UnicodeString subPairs; // Pairs for the nested []
                    doUnion(pairsBuf, parse(subPairs, pattern, pos, status));
                    if (U_FAILURE(status)) {
                        return pairsBuf;
                    }
                    i = pos.getIndex() - 1; // Subtract 1 to point at ']'
                    parseOp = TRUE;
                }
            }
            /**
             * parseOp is true after "[:...:]" or a nested
             * "[...]".  It is false only after the final closing
             * ']'.  If parseOp is true, we look past the closing
             * ']' to see if we have an operator character.  If
             * so, we parse the subsequent "[...]" recursively,
             * then perform the operation.  We do this in a loop
             * until there are no more operators.  Note that this
             * means the operators have equal precedence and are
             * bound left-to-right.
             */
            if (parseOp) {
                for (;;) {
                    // Is the next character an operator?
                    UChar op = charAfter(pattern, i);
                    if (op == '-' || op == '&') {
                        pos.setIndex(i+2); // Add 2 to point AFTER op
                        UnicodeString rhs;
                        parse(rhs, pattern, pos, status);
                        if (U_FAILURE(status)) {
                            return pairsBuf;
                        }
                        if (op == '-') {
                            doDifference(pairsBuf, rhs);
                        } else if (op == '&') {
                            doIntersection(pairsBuf, rhs);
                        }
                        i = pos.getIndex() - 1; // - 1 to point at ']'
                    } else {
                        break;
                    }
                }
            }          
        }
        /**
         * A closing bracket can only be a closing bracket for
         * "[...]", since the closing bracket for "[:...:]" is
         * taken care of when the initial "[:" is seen.  When we
         * see a closing bracket, we then know, if we were in node
         * 21 (after x) or 23 (after x-) that nothing more is
         * coming, and we add the last character(s) we saw to the
         * set.  Note that a trailing '-' assumes its literal
         * meaning, just as a leading '-' after "[" or "[^".
         */
        else if (!isLiteral && c == ']') {
            if (node == 0) {
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return pairsBuf;
            }
            if (node == 21 || node == 23) {
                addPair(pairsBuf, first, first);
                if (node == 23) {
                    addPair(pairsBuf, '-', '-');
                }
            }
            node = 0;
            break;
        }
        /**
         * '-' has the following interpretations: 1. Within
         * "[...]", between two letters, it indicates a range.
         * 2. Between two nested bracket patterns, "[[...]-[...]",
         * it indicates asymmetric difference.  3. At the start of
         * a bracket pattern, "[-...]", "[^-...]", it indicates
         * the literal character '-'.  4. At the end of a bracket
         * pattern, "[...-]", it indicates the literal character
         * '-'.
         *
         * We handle cases 1 and 3 here.  Cases 2 and 4 are
         * handled in the ']' parsing code.
         */
        else if (!isLiteral && c == '-') {
            if (node == 10) {
                addPair(pairsBuf, c, c); // Handle "[-...]", "[^-...]"
            } else if (node == 21) {
                node = 23;
            } else {
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return pairsBuf;
            }
        }
        /**
         * If we fall through to this point, we have a literal
         * character, either one that has been escaped with a
         * backslash, escaped with a backslash u, or that isn't
         * a special '[', ']', or '-'.
         *
         * Literals can either start a range "x-...", end a range,
         * "...-x", or indicate a single character "x".
         */
        else {
            if (node == 10 || node == 11) {
                first = c;
                node = 21;
            } else if (node == 21) {
                addPair(pairsBuf, first, first);
                first = c;
                node = 21;
            } else if (node == 23) {
                if (c < first) {
                    status = U_ILLEGAL_ARGUMENT_ERROR;
                    return pairsBuf;
                }
                addPair(pairsBuf, first, c);
                node = 11;
            } else {
                status = U_ILLEGAL_ARGUMENT_ERROR;
                return pairsBuf;
            }
        }
    }

    if (node != 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return pairsBuf;
    }
    /**
     * i indexes the last character we parsed or is
     * pattern.length().  In the latter case, the node will not be
     * zero, since we have run off the end without finding a
     * closing ']'.  Therefore, the above statement will have
     * thrown an exception, and we'll never get here.  If we get
     * here, we know i < pattern.length(), and we set the
     * ParsePosition to the next character to be parsed.
     */
    pos.setIndex(i+1);
    /**
     * If we saw a '^' after the initial '[' of this pattern, then
     * perform the complement.  (Inversion after '[:' is handled
     * elsewhere.)
     */
    if (invert) {
        doComplement(pairsBuf);
    }

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
void UnicodeSet::addPair(UnicodeString& pairs, UChar c, UChar d) {
    UChar a = 0;
    UChar b = 0;
    for (int32_t i=0; i<pairs.length(); i+=2) {
        UChar e = pairs.charAt(i);
        UChar f = pairs.charAt(i+1);
        if (e <= (d+1) && c <= (f+1)) {
            // Merge with this range
            f = (UChar) icu_max(d, f);

            // Check to see if we need to merge with the
            // subsequent range also.  This happens if we have
            // "abdf" and are merging in "cc".  We only need to
            // check on the right side -- never on the left.
            if ((i+2) < pairs.length() &&
                pairs.charAt(i+2) == (f+1)) {
                f = pairs.charAt(i+3);
                pairs.remove(i+2, 2);
            }
            pairs.setCharAt(i, (UChar) icu_min(c, e));
            pairs.setCharAt(i+1, f);
            return;
        } else if ((b+1) < c && (d+1) < e) {
            // Insert before this range c, then d
            pairs.insert(i, d); // d gets moved to i+1 by next insert
            pairs.insert(i, c);
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
void UnicodeSet::removePair(UnicodeString& pairs, UChar c, UChar d) {
    // Iterate over pairs until we find a pair that overlaps
    // with the given range.
    for (int32_t i=0; i<pairs.length(); i+=2) {
        UChar b = pairs.charAt(i+1);
        if (b < c) {
            // Range at i is entirely before the given range,
            // since we have a-b < c-d.  No overlap yet...keep
            // iterating.
            continue;
        }
        UChar a = pairs.charAt(i);
        if (d < a) {
            // Range at i is entirely after the given range; c-d <
            // a-b.  Since ranges are in order, nothing else will
            // overlap.
            break;
        }
        // Once we get here, we know c <= b and d >= a.
        // rangeEdited is set to true if we have modified the
        // range a-b (the range at i) in place.
        bool_t rangeEdited = FALSE;
        if (c > a) {
            // If c is after a and before b, then we have overlap
            // of this sort: a--c==b--d or a--c==d--b, where a-b
            // and c-d are the ranges of interest.  We need to
            // add the range a,c-1.
            pairs.setCharAt(i+1, (UChar)(c-1));
            // i is already a
            rangeEdited = TRUE;
        }
        if (d < b) {
            // If d is after a and before b, we overlap like this:
            // c--a==d--b or a--c==d--b, where a-b is the range at
            // i and c-d is the range being removed.  We need to
            // add the range d+1,b.
            if (rangeEdited) {
                // Insert {d+1, b}
                pairs.insert(i+2, b); // b moves to i+3 by next insert:
                pairs.insert(i+2, (UChar)(d+1));
                i += 2;
            } else {
                pairs.setCharAt(i, (UChar)(d+1));
                // i+1 is already b
                rangeEdited = TRUE;
            }
        }
        if (!rangeEdited) {
            // If we didn't add any ranges, that means the entire
            // range a-b must be deleted, since we have
            // c--a==b--d.
            pairs.remove(i, 2);
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
void UnicodeSet::doComplement(UnicodeString& pairs) {
    if (pairs.length() == 0) {
        pairs.append((UChar)0x0000).append((UChar)0xffff);
        return;
    }

    // Change each end to a start and each start to an end of the
    // gaps between the ranges.  That is, 3-7 9-12 becomes x-2 8-8
    // 13-x, where 'x' represents a range that must now be fixed
    // up.
    for (int32_t i=0; i<pairs.length(); i+=2) {
        pairs.setCharAt(i,   (UChar) (pairs.charAt(i)   - 1));
        pairs.setCharAt(i+1, (UChar) (pairs.charAt(i+1) + 1));
    }

    // Fix up the initial range, either by adding a start point of
    // U+0000, or by deleting the range altogether, if the
    // original range was U+0000 - x.
    if (pairs.charAt(0) == (UChar)0xFFFF) {
        pairs.remove(0, 1);
    } else {
        pairs.insert(0, (UChar)0x0000);
    }

    // Fix up the final range, either by adding an end point of
    // U+FFFF, or by deleting the range altogether, if the
    // original range was x - U+FFFF.
    if (pairs.charAt(pairs.length() - 1) == (UChar)0x0000) {
        pairs.remove(pairs.length() - 1);
    } else {
        pairs.append((UChar)0xFFFF);
    }
}

/**
 * Given two pairs lists, changes the first in place to represent
 * the union of the two sets.
 */
void UnicodeSet::doUnion(UnicodeString& c1, const UnicodeString& c2) {
    UnicodeString result;

    int32_t i = 0;
    int32_t j = 0;

    // consider all the characters in both strings
    while (i < c1.length() && j < c2.length()) {
        UChar ub;
        
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
        result.append(c1, i, LONG_MAX);
    if (j < c2.length())
        result.append(c2, j, LONG_MAX);

    c1 = result;
}

/**
 * Given two pairs lists, changes the first in place to represent
 * the asymmetric difference of the two sets.
 */
void UnicodeSet::doDifference(UnicodeString& pairs, const UnicodeString& pairs2) {
    UnicodeString p2(pairs2);
    doComplement(p2);
    doIntersection(pairs, p2);
}

/**
 * Given two pairs lists, changes the first in place to represent
 * the intersection of the two sets.
 */
void UnicodeSet::doIntersection(UnicodeString& c1, const UnicodeString& c2) {
    UnicodeString result;

    int32_t i = 0;
    int32_t j = 0;
    int32_t oldI;
    int32_t oldJ;

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
        result.append(c1, oldI, i-oldI);

        // if i points to the endpoint of a range, save the current
        // value of j, then advance j until it reaches a character
        // which is larger than the character pointed at
        // by i.  All of the characters we've advanced over (except
        // the one currently pointed to by i) are added to the result
        oldJ = j;
        while (i % 2 == 1 && j < c2.length() && c2.charAt(j) <= c1.charAt(i))
            ++j;
        result.append(c2, oldJ, j-oldJ);

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

    c1 = result;
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
UnicodeString& UnicodeSet::getCategoryPairs(UnicodeString& result,
                                            const UnicodeString& catName,
                                            UErrorCode& status) {
    if (U_FAILURE(status)) {
        return result;
    }

	// The temporary cat is only really needed if invert is true.
	// TO DO: Allocate cat on the heap only if needed.
	UnicodeString cat(catName);
    bool_t invert = (catName.length() > 1 &&
                     catName.charAt(0) == '^');
    if (invert) {
        cat.remove(0, 1);
    }

    result.remove();
    
    // if we have two characters, search the category map for that
    // code and either construct and return a UnicodeSet from the
    // data in the category map or throw an exception
    if (cat.length() == 2) {
        int32_t i = CATEGORY_NAMES.indexOf(cat);
        if (i>=0 && i%2==0) {
            i /= 2;
            result = getCategoryPairs((int8_t)i);
            if (!invert) {
                return result;
            }
        }
    } else if (cat.length() == 1) {
        // if we have one character, search the category map for
        // codes beginning with that letter, and union together
        // all of the matching sets that we find (or throw an
        // exception if there are no matches)
        for (int32_t i=0; i<Unicode::GENERAL_TYPES_COUNT; ++i) {
            if (CATEGORY_NAMES.charAt(2*i) == cat.charAt(0)) {
                const UnicodeString& pairs = getCategoryPairs((int8_t)i);
                if (result.length() == 0) {
                    result = pairs;
                } else {
                    doUnion(result, pairs);
                }
            }
        }
    }

    if (result.length() == 0) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return result;
    }

    if (invert) {
        doComplement(result);
    }
    return result;
}

/**
 * Returns a pairs string for the given category.  This string is
 * cached and returned again if this method is called again with
 * the same parameter.
 */
const UnicodeString& UnicodeSet::getCategoryPairs(int8_t cat) {
    // In order to tell what cache entries are empty, we assume
    // every category specifies at least one character.  Thus
    // pair lists in the cache that are empty are uninitialized.
    if (CATEGORY_PAIRS_CACHE[cat].length() == 0) {
        // Walk through all Unicode characters, noting the start
        // and end of each range for which Character.getType(c)
        // returns the given category integer.  Since we are
        // iterating in order, we can simply append the resulting
        // ranges to the pairs string.
        UnicodeString& pairs = CATEGORY_PAIRS_CACHE[cat];
        int32_t first = -1;
        int32_t last = -2;
        for (int32_t i=0; i<=0xFFFF; ++i) {
            if (Unicode::getType((UChar)i) == cat) {
                if ((last+1) == i) {
                    last = i;
                } else {
                    if (first >= 0) {
                        pairs.append((UChar)first).append((UChar)last);
                    }
                    first = last = i;
                }
            }
        }
        if (first >= 0) {
            pairs.append((UChar)first).append((UChar)last);
        }
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
UChar UnicodeSet::charAfter(const UnicodeString& str, int32_t i) {
    return ((++i) < str.length()) ? str.charAt(i) : (UChar)0xFFFF;
}

/**
 * TEMPORARY WORKAROUND UNTIL Unicode::digit() exists.
 * Return the digit value of the given UChar, or -1.  The radix
 * value is ignored for now and hardcoded as 16.
 */
int8_t UnicodeSet::digit(UChar c, int8_t radix) {
	int32_t d = Unicode::digitValue(c);
	if (d < 0) {
		if (c >= (UChar)'a' && c <= (UChar)'f') {
			d = c - (UChar)('a' - 10);
		} else if (c >= (UChar)'A' && c <= (UChar)'F') {
			d = c - (UChar)('A' - 10);
		}
	}
	return (int8_t)d;
}
