/*
**********************************************************************
* Copyright © {1999}, International Business Machines Corporation and others. All Rights Reserved.
**********************************************************************
*   Date        Name        Description
*   10/20/99    alan        Creation.
**********************************************************************
*/

#ifndef UNICODESET_H
#define UNICODESET_H

#include "unicode/unifilt.h"
#include "unicode/utypes.h"
#include "unicode/unistr.h"

class ParsePosition;
class SymbolTable;
class TransliterationRuleParser;
class TransliterationRule;

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
 * <code>Format</code>-derived classes.  Unlike the
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
 * <code>Unicode::getType()</code>, so it reflects the underlying
 * data used by <code>Unicode</code>.
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
 *
 * @author Alan Liu
 * @draft
 */
class U_I18N_API UnicodeSet : public UnicodeFilter {

    int32_t len; // length of list used; 0 <= len <= capacity
    int32_t capacity; // capacity of list
    int32_t bufferCapacity; // capacity of buffer
    UChar32* list; // MUST be terminated with HIGH
    UChar32* buffer; // internal buffer, may be NULL

    static const UChar32 LOW; // LOW <= all valid values. ZERO for codepoints
    static const UChar32 HIGH; // HIGH > all valid values. 110000 for codepoints

    static const int32_t START_EXTRA; // initial storage. Must be >= 0
    static const int32_t GROW_EXTRA; // extra amount for growth. Must be >= 0

    static const UnicodeString CATEGORY_NAMES;

    /**
     * A cache mapping character category integers, as returned by
     * Unicode::getType(), to pairs strings.  Entries are initially
     * zero length and are filled in on demand.
     */
    static UnicodeSet* CATEGORY_CACHE;

    /**
     * Delimiter string used in patterns to close a category reference:
     * ":]".  Example: "[:Lu:]".
     */
    static const UnicodeString CATEGORY_CLOSE;
    
    // More special characters...
    static const UChar SET_OPEN;
    static const UChar SET_CLOSE;
    static const UChar HYPHEN;
    static const UChar COMPLEMENT;
    static const UChar COLON;
    static const UChar BACKSLASH;
    static const UChar INTERSECTION;
    static const UChar UPPER_U;
    
public:

    /**
     * Minimum value that can be stored in a UnicodeSet.
     */
    static const UChar32 MIN_VALUE;

    /**
     * Maximum value that can be stored in a UnicodeSet.
     */
    static const UChar32 MAX_VALUE;

    //----------------------------------------------------------------
    // Constructors &c
    //----------------------------------------------------------------

public:

    /**
     * Constructs an empty set.
     * @draft
     */
    UnicodeSet();

    /**
     * Constructs a set containing the given range. If <code>end >
     * start</code> then an empty set is created.
     *
     * @param start first character, inclusive, of range
     * @param end last character, inclusive, of range
     */
    UnicodeSet(UChar32 start, UChar32 end);

    /**
     * Constructs a set from the given pattern.  See the class
     * description for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @exception <code>IllegalArgumentException</code> if the pattern
     * contains a syntax error.
     * @draft
     */
    UnicodeSet(const UnicodeString& pattern,
               UErrorCode& status);

    /**
     * Constructs a set from the given Unicode character category.
     * @param category an integer indicating the character category as
     * returned by <code>Character.getType()</code>.
     * @exception <code>IllegalArgumentException</code> if the given
     * category is invalid.
     * @draft
     */
    UnicodeSet(int8_t category, UErrorCode& status);

    /**
     * Constructs a set that is identical to the given UnicodeSet.
     * @draft
     */
    UnicodeSet(const UnicodeSet& o);

    /**
     * Destructs the set.
     * @draft
     */
    virtual ~UnicodeSet();

    /**
     * Assigns this object to be a copy of another.
     * @draft
     */
    UnicodeSet& operator=(const UnicodeSet& o);

    /**
     * Compares the specified object with this set for equality.  Returns
     * <tt>true</tt> if the two sets
     * have the same size, and every member of the specified set is
     * contained in this set (or equivalently, every member of this set is
     * contained in the specified set).
     *
     * @param o set to be compared for equality with this set.
     * @return <tt>true</tt> if the specified set is equal to this set.
     * @draft
     */
    virtual UBool operator==(const UnicodeSet& o) const;

    /**
     * Compares the specified object with this set for equality.  Returns
     * <tt>true</tt> if the specified set is not equal to this set.
     * @draft
     */
    UBool operator!=(const UnicodeSet& o) const;

    /**
     * Returns a copy of this object.  All UnicodeFilter objects have
     * to support cloning in order to allow classes using
     * UnicodeFilters, such as Transliterator, to implement cloning.
     * @draft
     */
    virtual UnicodeFilter* clone() const;

    /**
     * Returns the hash code value for this set.
     *
     * @return the hash code value for this set.
     * @see Object#hashCode()
     * @draft
     */
    virtual int32_t hashCode(void) const;

    //----------------------------------------------------------------
    // Public API
    //----------------------------------------------------------------

    /**
     * Make this object represent the range <code>start - end</code>.
     * If <code>end > start</code> then this object is set to an
     * an empty range.
     *
     * @param start first character in the set, inclusive
     * @rparam end last character in the set, inclusive
     */
    void set(UChar32 start, UChar32 end);

    /**
     * Modifies this set to represent the set specified by the given
     * pattern, optionally ignoring white space.  See the class
     * description for the syntax of the pattern language.
     * @param pattern a string specifying what characters are in the set
     * @exception <code>IllegalArgumentException</code> if the pattern
     * contains a syntax error.
     * @draft
     */
    virtual void applyPattern(const UnicodeString& pattern,
                              UErrorCode& status);

    /**
     * Returns a string representation of this set.  If the result of
     * calling this function is passed to a UnicodeSet constructor, it
     * will produce another set that is equal to this one.
     * @draft
     */
    virtual UnicodeString& toPattern(UnicodeString& result) const;

    /**
     * Returns the number of elements in this set (its cardinality),
     * <em>n</em>, where <code>0 <= </code><em>n</em><code> <= 65536</code>.
     *
     * @return the number of elements in this set (its cardinality).
     * @draft
     */
    virtual int32_t size(void) const;

    /**
     * Returns <tt>true</tt> if this set contains no elements.
     *
     * @return <tt>true</tt> if this set contains no elements.
     * @draft
     */
    virtual UBool isEmpty(void) const;

    /**
     * Returns <tt>true</tt> if this set contains the specified range
     * of chars.
     *
     * @return <tt>true</tt> if this set contains the specified range
     * of chars.
     * @draft
     */
    virtual UBool contains(UChar32 start, UChar32 end) const;

    /**
     * Returns <tt>true</tt> if this set contains the specified char.
     *
     * @return <tt>true</tt> if this set contains the specified char.
     * @draft
     */
    virtual UBool contains(UChar32 c) const;

    /**
     * Implement UnicodeFilter:
     * Returns <tt>true</tt> if this set contains the specified char.
     *
     * @return <tt>true</tt> if this set contains the specified char.
     * @draft
     */
    virtual UBool contains(UChar c) const;

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
     * @draft
     */
    virtual void add(UChar32 start, UChar32 end);

    /**
     * Adds the specified character to this set if it is not already
     * present.  If this set already contains the specified character,
     * the call leaves this set unchanged.
     * @draft
     */
    void add(UChar32 c);

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
    virtual void retain(UChar32 start, UChar32 end);


    /**
     * Retain the specified character from this set if it is present.
     */
    void retain(UChar32 c);

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
     * @draft
     */
    virtual void remove(UChar32 start, UChar32 end);

    /**
     * Removes the specified character from this set if it is present.
     * The set will not contain the specified range once the call
     * returns.
     * @draft
     */
    void remove(UChar32 c);

    /**
     * Complements the specified range in this set.  Any character in
     * the range will be removed if it is in this set, or will be
     * added if it is not in this set.  If <code>end > start</code>
     * then an empty range is xor'ed, leaving the set unchanged.
     *
     * @param start first character, inclusive, of range to be removed
     * from this set.
     * @param end last character, inclusive, of range to be removed
     * from this set.
     */
    virtual void xor(UChar32 start, UChar32 end);


    /**
     * Complements the specified character in this set.  The character
     * will be removed if it is in this set, or will be added if it is
     * not in this set.
     */
    void xor(UChar32 c);

    /**
     * Returns <tt>true</tt> if the specified set is a <i>subset</i>
     * of this set.
     *
     * @param c set to be checked for containment in this set.
     * @return <tt>true</tt> if this set contains all of the elements of the
     * 	       specified set.
     * @draft
     */
    virtual UBool containsAll(const UnicodeSet& c) const;

    /**
     * Adds all of the elements in the specified set to this set if
     * they're not already present.  This operation effectively
     * modifies this set so that its value is the <i>union</i> of the two
     * sets.  The behavior of this operation is unspecified if the specified
     * collection is modified while the operation is in progress.
     *
     * @param c set whose elements are to be added to this set.
     * @see #add(char, char)
     * @draft
     */
    virtual void addAll(const UnicodeSet& c);

    /**
     * Retains only the elements in this set that are contained in the
     * specified set.  In other words, removes from this set all of
     * its elements that are not contained in the specified set.  This
     * operation effectively modifies this set so that its value is
     * the <i>intersection</i> of the two sets.
     *
     * @param c set that defines which elements this set will retain.
     * @draft
     */
    virtual void retainAll(const UnicodeSet& c);

    /**
     * Removes from this set all of its elements that are contained in the
     * specified set.  This operation effectively modifies this
     * set so that its value is the <i>asymmetric set difference</i> of
     * the two sets.
     *
     * @param c set that defines which elements will be removed from
     *          this set.
     * @draft
     */
    virtual void removeAll(const UnicodeSet& c);

    /**
     * Complements in this set all elements contained in the specified
     * set.  Any character in the other set will be removed if it is
     * in this set, or will be added if it is not in this set.
     *
     * @param c set that defines which elements will be xor'ed from
     *          this set.
     */
    virtual void xorAll(const UnicodeSet& c);

    /**
     * Inverts this set.  This operation modifies this set so that
     * its value is its complement.  This is equivalent to the pseudo code:
     * <code>this = new CharSet("[\u0000-\uFFFF]").removeAll(this)</code>.
     * @draft
     */
    virtual void complement(void);

    /**
     * Removes all of the elements from this set.  This set will be
     * empty after this call returns.
     * @draft
     */
    virtual void clear(void);

    /**
     * Iteration method that returns the number of ranges contained in
     * this set.
     * @see #getRangeStart
     * @see #getRangeEnd
     */
    virtual int32_t getRangeCount(void) const;

    /**
     * Iteration method that returns the first character in the
     * specified range of this set.
     * @see #getRangeCount
     * @see #getRangeEnd
     */
    virtual UChar32 getRangeStart(int32_t index) const;

    /**
     * Iteration method that returns the last character in the
     * specified range of this set.
     * @see #getRangeStart
     * @see #getRangeEnd
     */
    virtual UChar32 getRangeEnd(int32_t index) const;

    /**
     * Reallocate this objects internal structures to take up the least
     * possible space, without changing this object's value.
     */
    virtual void compact();

private:

    //----------------------------------------------------------------
    // RuleBasedTransliterator support
    //----------------------------------------------------------------

    friend class TransliterationRuleParser;
    friend class TransliterationRule;

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
     * @exception <code>IllegalArgumentException</code> if the pattern
     * contains a syntax error.
     */
    UnicodeSet(const UnicodeString& pattern, ParsePosition& pos,
               const SymbolTable& symbols,
               UErrorCode& status);

    /**
     * Returns <tt>true</tt> if this set contains any character whose low byte
     * is the given value.  This is used by <tt>RuleBasedTransliterator</tt> for
     * indexing.
     */
    UBool containsIndexValue(uint8_t v) const;

private:

    //----------------------------------------------------------------
    // Implementation: Pattern parsing
    //----------------------------------------------------------------

    /**
     * Parses the given pattern, starting at the given position.  The
     * character at pattern.charAt(pos.getIndex()) must be '[', or the
     * parse fails.  Parsing continues until the corresponding closing
     * ']'.  If a syntax error is encountered between the opening and
     * closing brace, the parse fails.  Upon return from a successful
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
     * Upon return from a successful parse, pos.getIndex() is either
     * the character after the closing ']' of the parsed pattern, or
     * pattern.length() if the closing ']' is the last character of
     * the pattern string.
     * @return a StringBuffer containing a pairs list for the parsed
     * substring of <code>pattern</code>
     * @exception IllegalArgumentException if the parse fails.
     */
    void applyPattern(const UnicodeString& pattern,
                      ParsePosition& pos,
                      const SymbolTable* symbols,
                      UErrorCode& status);

    //----------------------------------------------------------------
    // Implementation: Generation of pairs for Unicode categories
    //----------------------------------------------------------------

    /**
     * Sets this object to the given category, given its name.
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
    void applyCategory(const UnicodeString& catName,
                       UErrorCode& status);

    /**
     * Returns a pairs string for the given category.  This string is
     * cached and returned again if this method is called again with
     * the same parameter.
     */
    static const UnicodeSet& getCategorySet(int8_t cat);

    //----------------------------------------------------------------
    // Implementation: Utility methods
    //----------------------------------------------------------------

    /**
     * Returns the character after the given position, or '\uFFFF' if
     * there is none.
     */
    static UChar charAfter(const UnicodeString& str, int32_t i);

    void ensureCapacity(int32_t newLen);
    
    void ensureBufferCapacity(int32_t newLen);
    
    void swapBuffers(void);

    static const UChar HEX[16];
    
    static void _toPat(UnicodeString& buf, UChar32 c);

    //----------------------------------------------------------------
    // Implementation: Fundamental operators
    //----------------------------------------------------------------

    void xor(const UChar32* other, int32_t otherLen, int8_t polarity);
        
    void add(const UChar32* other, int32_t otherLen, int8_t polarity);
            
    void retain(const UChar32* other, int32_t otherLen, int8_t polarity);
};

inline UBool UnicodeSet::operator!=(const UnicodeSet& o) const {
	return !operator==(o);
}

#endif
