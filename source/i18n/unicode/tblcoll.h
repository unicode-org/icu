/*
******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and
* others. All Rights Reserved.
******************************************************************************
*/

/**
* File tblcoll.h
*
* Created by: Helena Shih
*
* Modification History:
*
*  Date        Name        Description
*  2/5/97      aliu        Added streamIn and streamOut methods.  Added
*                          constructor which reads RuleBasedCollator object from
*                          a binary file.  Added writeToFile method which streams
*                          RuleBasedCollator out to a binary file.  The streamIn
*                          and streamOut methods use istream and ostream objects
*                          in binary mode.
*  2/12/97     aliu        Modified to use TableCollationData sub-object to
*                          hold invariant data.
*  2/13/97     aliu        Moved several methods into this class from Collation.
*                          Added a private RuleBasedCollator(Locale&) constructor,
*                          to be used by Collator::createDefault().  General
*                          clean up.
*  2/20/97     helena      Added clone, operator==, operator!=, operator=, and copy
*                          constructor and getDynamicClassID.
*  3/5/97      aliu        Modified constructFromFile() to add parameter
*                          specifying whether or not binary loading is to be
*                          attempted.  This is required for dynamic rule loading.
* 05/07/97     helena      Added memory allocation error detection.
*  6/17/97     helena      Added IDENTICAL strength for compare, changed getRules to
*                          use MergeCollation::getPattern.
*  6/20/97     helena      Java class name change.
*  8/18/97     helena      Added internal API documentation.
* 09/03/97     helena      Added createCollationKeyValues().
* 02/10/98     damiba      Added compare with "length" parameter
* 08/05/98     erm         Synched with 1.2 version of RuleBasedCollator.java
* 04/23/99     stephen     Removed EDecompositionMode, merged with
*                          Normalizer::EMode
* 06/14/99     stephen     Removed kResourceBundleSuffix
* 11/02/99     helena      Collator performance enhancements.  Eliminates the
*                          UnicodeString construction and special case for NO_OP.
* 11/23/99     srl         More performance enhancements. Updates to NormalizerIterator
*                          internal state management.
* 12/15/99     aliu        Update to support Thai collation.  Move NormalizerIterator
*                          to implementation file.
* 01/29/01     synwee      Modified into a C++ wrapper which calls C API
*                          (ucol.h)
*/

#ifndef TBLCOLL_H
#define TBLCOLL_H

#include "unicode/coll.h"
#include "unicode/sortkey.h"
#include "unicode/normlzr.h"

class CollationElementIterator;

/**
 * The RuleBasedCollator class provides the simple implementation of
 * Collator, using data-driven tables. The user can create a customized
 * table-based collation.
 * <P>
 * RuleBasedCollator maps characters to collation keys.
 * <p>
 * Table Collation has the following restrictions for efficiency (other
 * subclasses may be used for more complex languages) :
 *       <p>1. If the French secondary ordering is specified in a collation
 *             object, it is applied to the whole object.
 *       <p>2. All non-mentioned Unicode characters are at the end of the
 *             collation order.
 *       <p>3. Private use characters are treated as identical.  The private
 *             use area in Unicode is 0xE800-0xF8FF.
 * <p>The collation table is composed of a list of collation rules, where each
 * rule is of three forms:
 * <pre>
 * \code
 *     <modifier >
 *     <relation > &lt; text-argument >
 *     <reset > &lt; text-argument >
 * \endcode
 * </pre>
 * The following demonstrates how to create your own collation rules:
 * <UL Type=round>
 *    <LI><strong>Text Argument</strong>: A text argument is any sequence of
 *        characters, excluding special characters (that is, whitespace
 *        characters and the characters used in modifier, relation and reset).
 *        If those characters are desired, you can put them in single quotes
 *        (e.g. ampersand => '&').<P>
 *    <LI><strong>Modifier</strong>: There is a single modifier,
 *        which is used to specify that all secondary differences are
 *        sorted backwards.
 *        <p>'@' : Indicates that secondary differences, such as accents, are
 *                 sorted backwards, as in French.<P>
 *    <LI><strong>Relation</strong>: The relations are the following:
 *        <UL Type=square>
 *            <LI>'&lt;' : Greater, as a letter difference (primary)
 *            <LI>';' : Greater, as an accent difference (secondary)
 *            <LI>',' : Greater, as a case difference (tertiary)
 *            <LI>'=' : Equal
 *        </UL><P>
 *    <LI><strong>Reset</strong>: There is a single reset,
 *        which is used primarily for contractions and expansions, but which
 *        can also be used to add a modification at the end of a set of rules.
 *        <p>'&' : Indicates that the next rule follows the position to where
 *            the reset text-argument would be sorted.
 * </UL>
 *
 * <p>
 * This sounds more complicated than it is in practice. For example, the
 * following are equivalent ways of expressing the same thing:
 * <pre>
 * \code
 *     a < b < c
 *     a < b & b  < c
 *     a < c & a  < b
 * \endcode
 * </pre>
 * Notice that the order is important, as the subsequent item goes immediately
 * after the text-argument. The following are not equivalent:
 * <pre>
 * \code
 *     a <  b & a  < c
 *     a <  c & a  < b
 * \endcode
 * </pre>
 * Either the text-argument must already be present in the sequence, or some
 * initial substring of the text-argument must be present. (e.g. "a &lt; b &
 * ae &lt; e" is valid since "a" is present in the sequence before "ae" is
 * reset). In this latter case, "ae" is not entered and treated as a single
 * character; instead, "e" is sorted as if it were expanded to two characters:
 * "a" followed by an "e". This difference appears in natural languages: in
 * traditional Spanish "ch" is treated as though it contracts to a single
 * character (expressed as "c &lt; ch &lt; d"), while in traditional German
 * "ä" (a-umlaut) is treated as though it expands to two characters (expressed
 * as "a & ae ; ä &lt; b").
 * <p><strong>Ignorable Characters</strong>
 * <p>For ignorable characters, the first rule must start with a relation (the
 * examples we have used above are really fragments; "a &lt; b" really should
 * be "&lt; a &lt; b"). If, however, the first relation is not "&lt;", then
 * all the text-arguments up to the first "&lt;" are ignorable. For example,
 * ", - &lt; a &lt; b" makes "-" an ignorable character, as we saw earlier in
 * the word "black-birds". In the samples for different languages, you see
 * that most accents are ignorable.
 * <p><strong>Normalization and Accents</strong>
 * <p>The Collator object automatically normalizes text internally to
 * separate accents from base characters where possible. This is done both
 * when processing the rules, and when comparing two strings. Collator also
 * uses the Unicode canonical mapping to ensure that combining sequences are
 * sorted properly (for more information, see
 * <A HREF="http://www.aw.com/devpress"> The Unicode Standard, Version 2.0</A>
 * .)</P>
 * <p><strong>Errors</strong>
 * <p>The following are errors:
 * <UL Type=round>
 *     <LI>A text-argument contains unquoted punctuation symbols
 *        (e.g. "a &lt; b-c &lt; d").
 *     <LI>A relation or reset character not followed by a text-argument
 *        (e.g. "a &lt; , b").
 *     <LI>A reset where the text-argument (or an initial substring of the
 *         text-argument) is not already in the sequence.
 *         (e.g. "a &lt; b & e &lt; f")
 * </UL>
 * <pre>
 * \code
 *     Examples:
 *     Simple:     "< a < b < c < d"
 *     Norwegian:  "< a,A< b,B< c,C< d,D< e,E< f,F< g,G< h,H< i,I< j,J
 *                  < k,K< l,L< m,M< n,N< o,O< p,P< q,Q< r,R< s,S< t,T
 *                  < u,U< v,V< w,W< x,X< y,Y< z,Z
 *                  < å=a°,Å=A°
 *                  ;aa,AA< æ,Æ< ø,Ø"
 * \endcode
 * </pre>
 * <p>To create a table-based collation object, simply supply the collation
 * rules to the RuleBasedCollator contructor.  For example:
 * <pre>
 * \code
 *     UErrorCode status = U_ZERO_ERROR;
 *     RuleBasedCollator *mySimple =
 *                                    new RuleBasedCollator(Simple, status);
 * \endcode
 * </pre>
 * <p>Another example:
 * <pre>
 * \code
 *     UErrorCode status = U_ZERO_ERROR;
 *     RuleBasedCollator *myNorwegian =
 *                                 new RuleBasedCollator(Norwegian, status);
 * \endcode
 * </pre>
 * To add rules on top of an existing table, simply supply the orginal rules
 * and modifications to RuleBasedCollator constructor.  For example,
 * <pre>
 * \code
 *      Traditional Spanish (fragment): ... & C < ch , cH , Ch , CH ...
 *      German (fragment) : ...< y , Y < z , Z
 *                          & AE, Ä & AE, ä
 *                          & OE , Ö & OE, ö
 *                          & UE , Ü & UE, ü
 *      Symbols (fragment): ...< y, Y < z , Z
 *                          & Question-mark ; '?'
 *                          & Ampersand ; '&'
 *                          & Dollar-sign ; '$'
 * \endcode
 * </pre>
 * <p>To create a collation object for traditional Spanish, the user can take
 * the English collation rules and add the additional rules to the table.
 * For example:
 * <pre>
 * \code
 *      UErrorCode status = U_ZERO_ERROR;
 *      UnicodeString rules(DEFAULTRULES);
 *      rules += "& C &lt; ch, cH, Ch, CH";
 *      RuleBasedCollator *mySpanish =
 *                                     new RuleBasedCollator(rules, status);
 * \endcode
 * </pre>
 * <p>In order to sort symbols in the similiar order of sorting their
 * alphabetic equivalents, you can do the following,
 * <pre>
 * \code
 *      UErrorCode status = U_ZERO_ERROR;
 *      UnicodeString rules(DEFAULTRULES);
 *      rules += "& Question-mark ; '?' & Ampersand ; '&' & Dollar-sign ;
 *               '$' ";
 *      RuleBasedCollator *myTable =
 *                                     new RuleBasedCollator(rules, status);
 * \endcode
 * </pre>
 * <p>Another way of creating the table-based collation object, mySimple,
 * is:
 * <pre>
 * \code
 *      UErrorCode status = U_ZERO_ERROR;
 *      RuleBasedCollator *mySimple = new
 *        RuleBasedCollator(" < a < b & b < c & c < d", status);
 * \endcode
 * </pre>
 * Or,
 * <pre>
 * \code
 *      UErrorCode status = U_ZERO_ERROR;
 *      RuleBasedCollator *mySimple = new
 *            RuleBasedCollator(" < a < b < d & b < c", status);
 * \endcode
 * </pre>
 * Because " &lt; a &lt; b &lt; c &lt; d" is the same as "a &lt; b &lt; d & b
 *          &lt; c" or "&lt; a &lt; b & b &lt; c & c &lt; d".
 *
 * <p>To combine collations from two locales, (without error handling for
 * clarity)
 * <pre>
 * \code
 *     // Create an en_US Collator object
 *     Locale locale_en_US("en", "US", "");
 *     RuleBasedCollator* en_USCollator = (RuleBasedCollator*)
 *         Collator::createInstance( locale_en_US, success );
 *
 *     // Create a da_DK Collator object
 *     Locale locale_da_DK("da", "DK", "");
 *     RuleBasedCollator* da_DKCollator = (RuleBasedCollator*)
 *         Collator::createInstance( locale_da_DK, success );
 *
 *     // Combine the two
 *     // First, get the collation rules from en_USCollator
 *     UnicodeString rules = en_USCollator->getRules();
 *     // Second, get the collation rules from da_DKCollator
 *     rules += da_DKCollator->getRules();
 *     RuleBasedCollator* newCollator =
 *                                    new RuleBasedCollator(rules, success);
 *     // newCollator has the combined rules
 * \endcode
 * </pre>
 * <p>Another more interesting example would be to make changes on an existing
 * table to create a new collation object.  For example, add
 * "& C &lt; ch, cH, Ch, CH" to the en_USCollation object to create your own
 * English collation object,
 * <pre>
 * \code
 *     // Create a new Collator object with additional rules
 *     rules = en_USCollator->getRules();
 *     rules += "& C < ch, cH, Ch, CH";
 *     RuleBasedCollator* myCollator =
 *                                    new RuleBasedCollator(rules, success);
 *     // myCollator contains the new rules
 * \endcode
 * </pre>
 *
 * <p>The following example demonstrates how to change the order of
 * non-spacing accents,
 * <pre>
 * \code
 *      UChar contents[] = {
 *          '=', 0x0301, ';', 0x0300, ';', 0x0302,
 *          ';', 0x0308, ';', 0x0327, ',', 0x0303,    // main accents
 *          ';', 0x0304, ';', 0x0305, ';', 0x0306,    // main accents
 *          ';', 0x0307, ';', 0x0309, ';', 0x030A,    // main accents
 *          ';', 0x030B, ';', 0x030C, ';', 0x030D,    // main accents
 *          ';', 0x030E, ';', 0x030F, ';', 0x0310,    // main accents
 *          ';', 0x0311, ';', 0x0312,                 // main accents
 *          '<', 'a', ',', 'A', ';', 'a', 'e', ',', 'A', 'E',
 *          ';', 0x00e6, ',', 0x00c6, '<', 'b', ',', 'B',
 *          '<', 'c', ',', 'C', '<', 'e', ',', 'E', '&',
 *          'C', '<', 'd', ',', 'D', 0 };
 *      UnicodeString oldRules(contents);
 *      UErrorCode status = U_ZERO_ERROR;
 *      // change the order of accent characters
 *      UChar addOn[] = { '&', ',', 0x0300, ';', 0x0308, ';', 0x0302, 0 };
 *      oldRules += addOn;
 *      RuleBasedCollator *myCollation =
 *                                  new RuleBasedCollator(oldRules, status);
 *  \endcode
 * </pre>
 *
 * <p> The last example shows how to put new primary ordering in before the
 * default setting. For example, in Japanese collation, you can either sort
 * English characters before or after Japanese characters,
 * <pre>
 * \code
 *      UErrorCode status = U_ZERO_ERROR;
 *      // get en_US collation rules
 *      RuleBasedCollator* en_USCollation = (RuleBasedCollator*)
 *                             Collator::createInstance(Locale::US, status);
 *      // Always check the error code after each call.
 *      if (U_FAILURE(status)) return;
 *      // add a few Japanese character to sort before English characters
 *      // suppose the last character before the first base letter 'a' in
 *      // the English collation rule is 0x2212
 *      UChar jaString[] = {'&', 0x2212, '<', 0x3041, ',', 0x3042, '<',
 *                          0x3043, ',', 0x3044, 0};
 *      UnicodeString rules(en_USCollation->getRules());
 *      rules += jaString;
 *      RuleBasedCollator *myJapaneseCollation =
 *                                     new RuleBasedCollator(rules, status);
 * \endcode
 * </pre>
 * <p><strong>NOTE</strong>: Typically, a collation object is created with
 * Collator::createInstance().
 * <p>
 * <strong>Note:</strong> <code>RuleBasedCollator</code>s with different
 * Locale, CollationStrength and Decomposition mode settings will return
 * different sort orders for the same set of strings. Locales have specific
 * collation rules, and the way in which secondary and tertiary differences
 * are taken into account, for example, will result in a different sorting
 * order for same strings.
 * <p>
 * @see        Collator
 * @version    1.8 Jan 8 2001
 */
class U_I18N_API RuleBasedCollator : public Collator
{
public:

  // constructor -------------------------------------------------------------

  /**
   * RuleBasedCollator constructor. This takes the table rules and builds a
   * collation table out of them. Please see RuleBasedCollator class
   * description for more details on the collation rule syntax.
   * @param rules the collation rules to build the collation table from.
   * @param status reporting a success or an error.
   * @see Locale
   */
	RuleBasedCollator(const UnicodeString& rules, UErrorCode& status);

  /**
   * RuleBasedCollator constructor. This takes the table rules and builds a
   * collation table out of them. Please see RuleBasedCollator class
   * description for more details on the collation rule syntax.
   * @param rules the collation rules to build the collation table from.
   * @param collationStrength default strength for comparison
   * @param status reporting a success or an error.
   * @see Locale
   */
  RuleBasedCollator(const UnicodeString& rules,
                       ECollationStrength collationStrength,
                       UErrorCode& status);

  /**
   * RuleBasedCollator constructor. This takes the table rules and builds a
   * collation table out of them. Please see RuleBasedCollator class
   * description for more details on the collation rule syntax.
   * @param rules the collation rules to build the collation table from.
   * @param decompositionMode the normalisation mode
   * @param status reporting a success or an error.
   * @see Locale
   */
  RuleBasedCollator(const UnicodeString& rules,
                    Normalizer::EMode decompositionMode,
                    UErrorCode& status);

  /**
   * RuleBasedCollator constructor. This takes the table rules and builds a
   * collation table out of them. Please see RuleBasedCollator class
   * description for more details on the collation rule syntax.
   * @param rules the collation rules to build the collation table from.
   * @param collationStrength default strength for comparison
   * @param decompositionMode the normalisation mode
   * @param status reporting a success or an error.
   * @see Locale
   */
  RuleBasedCollator(const UnicodeString& rules,
                    ECollationStrength collationStrength,
                    Normalizer::EMode decompositionMode,
                    UErrorCode& status);

  /**
   * Copy constructor.
   * @param the RuleBasedCollator object to be copied
   * @see Locale
   */
	RuleBasedCollator(const RuleBasedCollator& other);

  // destructor --------------------------------------------------------------

  /**
   * Destructor.
   */
	virtual ~RuleBasedCollator();

  // public methods ----------------------------------------------------------

  /**
   * Assignment operator.
   * @param other other RuleBasedCollator object to compare with.
   */
	RuleBasedCollator& operator=(const RuleBasedCollator& other);

  /**
   * Returns true if argument is the same as this object.
   * @param other Collator object to be compared.
   * @return true if arguments is the same as this object.
   */
  virtual UBool operator==(const Collator& other) const;

  /**
   * Returns true if argument is not the same as this object.
   * @param other Collator object to be compared
   * @return returns true if argument is not the same as this object.
   */
  virtual UBool operator!=(const Collator& other) const;

  /**
   * Makes a deep copy of the object.
   * The caller owns the returned object.
   * @return the cloned object.
   */
  virtual Collator* clone(void) const;

  /**
   * Creates a collation element iterator for the source string. The caller of
   * this method is responsible for the memory management of the return
   * pointer.
   * @param source the string over which the CollationElementIterator will
   *        iterate.
   * @return the collation element iterator of the source string using this as
   *         the based Collator.
   */
	virtual CollationElementIterator* createCollationElementIterator(
                                           const UnicodeString& source) const;

  /**
   * Creates a collation element iterator for the source. The caller of this
   * method is responsible for the memory management of the returned pointer.
   * @param source the CharacterIterator which produces the characters over
   *        which the CollationElementItgerator will iterate.
   * @return the collation element iterator of the source using this as the
   *         based Collator.
   */
  virtual CollationElementIterator* createCollationElementIterator(
                                       const CharacterIterator& source) const;

  /**
   * Compares a range of character data stored in two different strings based
   * on the collation rules. Returns information about whether a string is
   * less than, greater than or equal to another string in a language.
   * This can be overriden in a subclass.
   * @param source the source string.
   * @param target the target string to be compared with the source string.
   * @return the comparison result. GREATER if the source string is greater
   *         than the target string, LESS if the source is less than the
   *         target. Otherwise, returns EQUAL.
   */
  virtual EComparisonResult compare(const UnicodeString& source,
                                    const UnicodeString& target) const;


  /**
   * Compares a range of character data stored in two different strings based
   * on the collation rules up to the specified length. Returns information
   * about whether a string is less than, greater than or equal to another
   * string in a language. This can be overriden in a subclass.
   * @param source the source string.
   * @param target the target string to be compared with the source string.
   * @param length compares up to the specified length
   * @return the comparison result. GREATER if the source string is greater
   *         than the target string, LESS if the source is less than the
   *         target. Otherwise, returns EQUAL.
   */
  virtual EComparisonResult compare(const UnicodeString& source,
                                    const UnicodeString&  target,
                                    int32_t length) const;

  /**
   * The comparison function compares the character data stored in two
   * different string arrays. Returns information about whether a string array
   * is less than, greater than or equal to another string array.
   * <p>Example of use:
   * <pre>
   * .       UErrorCode status = U_ZERO_ERROR;
   * .       Collator *myCollation =
   * .                         Collator::createInstance(Locale::US, status);
   * .       if (U_FAILURE(status)) return;
   * .       myCollation->setStrength(Collator::PRIMARY);
   * .       // result would be Collator::EQUAL ("abc" == "ABC")
   * .       // (no primary difference between "abc" and "ABC")
   * .       Collator::UCollationResult result =
   * .                              myCollation->compare(L"abc", 3, L"ABC", 3);
   * .       myCollation->setStrength(Collator::TERTIARY);
   * .       // result would be Collator::LESS (abc" &lt;&lt;&lt; "ABC")
   * .       // (with tertiary difference between "abc" and "ABC")
   * .       Collator::UCollationResult result =
   * .                              myCollation->compare(L"abc", 3, L"ABC", 3);
   * </pre>
   * @param source the source string array to be compared with.
   * @param sourceLength the length of the source string array. If this value
   *        is equal to -1, the string array is null-terminated.
   * @param target the string that is to be compared with the source string.
   * @param targetLength the length of the target string array. If this value
   *        is equal to -1, the string array is null-terminated.
   * @return Returns a byte value. GREATER if source is greater than target;
   *         EQUAL if source is equal to target; LESS if source is less than
   *         target
   */
  virtual EComparisonResult compare(const UChar* source, int32_t sourceLength,
                                    const UChar* target, int32_t targetLength)
                                    const;

  /**
  * Transforms a specified region of the string into a series of characters
  * that can be compared with CollationKey.compare. Use a CollationKey when
  * you need to do repeated comparisions on the same string. For a single
  * comparison the compare method will be faster.
  * @param source the source string.
  * @param key the transformed key of the source string.
  * @param status the error code status.
  * @return the transformed key.
  * @see CollationKey
  */
  virtual CollationKey& getCollationKey(const UnicodeString& source,
                                        CollationKey& key,
                                        UErrorCode& status) const;

  /**
  * Transforms a specified region of the string into a series of characters
  * that can be compared with CollationKey.compare. Use a CollationKey when
  * you need to do repeated comparisions on the same string. For a single
  * comparison the compare method will be faster.
  * @param source the source string.
  * @param key the transformed key of the source string.
  * @param status the error code status.
  * @return the transformed key.
  * @see CollationKey
  */
  virtual CollationKey& getCollationKey(const UChar *source,
                                        int32_t sourceLength,
                                        CollationKey& key,
                                        UErrorCode& status) const;

  /**
   * Generates the hash code for the rule-based collation object.
   * @return the hash code.
   */
  virtual int32_t hashCode(void) const;

  /**
   * Gets the table-based rules for the collation object.
   * @return returns the collation rules that the table collation object was
   *         created from.
   */
  const UnicodeString& getRules(void) const;

  /**
   * Return the maximum length of any expansion sequences that end with the
   * specified comparison order.
   * @param order a collation order returned by previous or next.
   * @return maximum size of the expansion sequences ending with the collation
   *         element or 1 if collation element does not occur at the end of
   *         any expansion sequence
   * @see CollationElementIterator#getMaxExpansion
   */
	int32_t getMaxExpansion(int32_t order) const;

  /**
   * Returns a unique class ID POLYMORPHICALLY. Pure virtual override. This
   * method is to implement a simple version of RTTI, since not all C++
   * compilers support genuine RTTI. Polymorphic operator==() and clone()
   * methods call this method.
   * @return The class ID for this object. All objects of a given class have
   *         the same class ID. Objects of other classes have different class
   *         IDs.
   */
  virtual UClassID getDynamicClassID(void) const
  {
    return RuleBasedCollator::getStaticClassID();
  }

  /**
   * Returns the class ID for this class. This is useful only for comparing to
   * a return value from getDynamicClassID(). For example:
   * <pre>
   * Base* polymorphic_pointer = createPolymorphicObject();
   * if (polymorphic_pointer->getDynamicClassID() ==
   *                                          Derived::getStaticClassID()) ...
   * </pre>
   * @return The class ID for all objects of this class.
   */
  static UClassID getStaticClassID(void)
  {
    return (UClassID)&fgClassID;
  }

  /**
   * Returns the binary format of the class's rules. The format is that of
   * .col files.
   * @param length Returns the length of the data, in bytes
   * @param status the error code status.
   * @return memory, owned by the caller, of size 'length' bytes.
   */
  uint8_t *cloneRuleData(int32_t &length, UErrorCode &status);

	/**
	 * Returns current rules. Delta defines whether full rules are returned or
   * just the tailoring.
	 * @param delta one of 	UCOL_TAILORING_ONLY, UCOL_FULL_RULES.
	 * @return UnicodeString with rules
	 */
	UnicodeString getRules(UColRuleOption delta);

  /**
   * Universal attribute setter
   * @param attr attribute type
   * @param value attribute value
   * @param status to indicate whether the operation went on smoothly or there were errors
   */
  virtual void setAttribute(UColAttribute attr, UColAttributeValue value,
                            UErrorCode &status);

  /**
   * Universal attribute getter.
   * @param attr attribute type
   * @param status to indicate whether the operation went on smoothly or there were errors
   * @return attribute value
   */
  virtual UColAttributeValue getAttribute(UColAttribute attr,
                                          UErrorCode &status);

  /** 
   * Sets the variable top to a collation element value of a string supplied. 
   * @param varTop one or more (if contraction) UChars to which the variable top should be set
   * @param len length of variable top string. If -1 it is considered to be zero terminated.
   * @param status error code. If error code is set, the return value is undefined. Errors set by this function are: <br>
   *    U_CE_NOT_FOUND_ERROR if more than one character was passed and there is no such a contraction<br>
   *    U_PRIMARY_TOO_LONG_ERROR if the primary for the variable top has more than two bytes
   * @return a 32 bit value containing the value of the variable top in upper 16 bits. Lower 16 bits are undefined
   * @draft
   */
  virtual uint32_t setVariableTop(const UChar *varTop, int32_t len, UErrorCode &status);

  /** 
   * Sets the variable top to a collation element value of a string supplied. 
   * @param varTop an UnicodeString size 1 or more (if contraction) of UChars to which the variable top should be set
   * @param status error code. If error code is set, the return value is undefined. Errors set by this function are: <br>
   *    U_CE_NOT_FOUND_ERROR if more than one character was passed and there is no such a contraction<br>
   *    U_PRIMARY_TOO_LONG_ERROR if the primary for the variable top has more than two bytes
   * @return a 32 bit value containing the value of the variable top in upper 16 bits. Lower 16 bits are undefined
   * @draft
   */
  virtual uint32_t setVariableTop(const UnicodeString varTop, UErrorCode &status);

  /** 
   * Sets the variable top to a collation element value supplied. Variable top is set to the upper 16 bits. 
   * Lower 16 bits are ignored.
   * @param varTop CE value, as returned by setVariableTop or ucol)getVariableTop
   * @param status error code (not changed by function)
   * @draft
   */
  virtual void setVariableTop(const uint32_t varTop, UErrorCode &status);

  /** 
   * Gets the variable top value of a Collator. 
   * Lower 16 bits are undefined and should be ignored.
   * @param status error code (not changed by function). If error code is set, the return value is undefined.
   * @draft
   */
  virtual uint32_t getVariableTop(UErrorCode &status) const;

  /**
   * Thread safe cloning operation.
   * @return pointer to the new clone, user should remove it.
   */
  virtual Collator* safeClone(void);

  /**
   * Get the sort key as an array of bytes from an UnicodeString.
   * @param source string to be processed.
   * @param result buffer to store result in. If NULL, number of bytes needed
   *        will be returned.
   * @param resultLength length of the result buffer. If if not enough the
   *        buffer will be filled to capacity.
   * @return Number of bytes needed for storing the sort key
   */
  virtual int32_t getSortKey(const UnicodeString& source, uint8_t *result,
						                 int32_t resultLength) const;

  /**
   * Get the sort key as an array of bytes from an UChar buffer.
   * @param source string to be processed.
   * @param sourceLength length of string to be processed. If -1, the string
   *        is 0 terminated and length will be decided by the function.
   * @param result buffer to store result in. If NULL, number of bytes needed
   *        will be returned.
   * @param resultLength length of the result buffer. If if not enough the
   *        buffer will be filled to capacity.
   * @return Number of bytes needed for storing the sort key
   */
  virtual int32_t getSortKey(const UChar *source, int32_t sourceLength,
						                 uint8_t *result, int32_t resultLength) const;

  /**
  * Determines the minimum strength that will be use in comparison or
  * transformation.
  * <p>E.g. with strength == SECONDARY, the tertiary difference is ignored
  * <p>E.g. with strength == PRIMARY, the secondary and tertiary difference
  * are ignored.
  * @return the current comparison level.
  * @see RuleBasedCollator#setStrength
  */
  virtual ECollationStrength getStrength(void) const;

  /**
  * Sets the minimum strength to be used in comparison or transformation.
  * <p>Example of use:
  * <pre>
  * . UErrorCode status = U_ZERO_ERROR;
  * . Collator*myCollation = Collator::createInstance(Locale::US,
  *                                                         status);
  * . if (U_FAILURE(status)) return;
  * . myCollation->setStrength(Collator::PRIMARY);
  * . // result will be "abc" == "ABC"
  * . // tertiary differences will be ignored
  * . Collator::ComparisonResult result = myCollation->compare("abc",
  *                                                               "ABC");
  * </pre>
  * @see RuleBasedCollator#getStrength
  * @param newStrength the new comparison level.
  * @stable
  */
  virtual void setStrength(ECollationStrength newStrength);

  /**
  * Set the decomposition mode of the Collator object. success is equal to
  * U_ILLEGAL_ARGUMENT_ERROR if error occurs.
  * @param the new decomposition mode
  * @see Collator#getDecomposition
  */
  virtual void setDecomposition(Normalizer::EMode  mode);

  /**
  * Get the decomposition mode of the Collator object.
  * @return the decomposition mode
  * @see Collator#setDecomposition
  */
  virtual Normalizer::EMode getDecomposition(void) const;

private:

  // private static constants -----------------------------------------------

  static const int32_t UNMAPPED;
  static const int32_t CHARINDEX;  // need look up in .commit()
  static const int32_t EXPANDCHARINDEX; // Expand index follows
  static const int32_t CONTRACTCHARINDEX;  // contract indexes follow

  static const int32_t PRIMARYORDERINCREMENT;
  static const int32_t SECONDARYORDERINCREMENT;
  static const int32_t TERTIARYORDERINCREMENT;
  static const int32_t PRIMARYORDERMASK;
  static const int32_t SECONDARYORDERMASK;
  static const int32_t TERTIARYORDERMASK;
  static const int32_t IGNORABLEMASK;
  static const int32_t PRIMARYDIFFERENCEONLY;
  static const int32_t SECONDARYDIFFERENCEONLY;
  static const int32_t PRIMARYORDERSHIFT;
  static const int32_t SECONDARYORDERSHIFT;

  static const int32_t COLELEMENTSTART;
  static const int32_t PRIMARYLOWZEROMASK;
  static const int32_t RESETSECONDARYTERTIARY;
  static const int32_t RESETTERTIARY;

  static const int32_t PRIMIGNORABLE;

  static const int16_t FILEID;
  static const char    *kFilenameSuffix;

  // private static variables -----------------------------------------------

  /**
  * static class id
  */
  static char fgClassID;

  // private data members ---------------------------------------------------

  UBool dataIsOwned;

  /**
  * c struct for collation. All initialisation for it has to be done through
  * setUCollator().
  */
  UCollator *ucollator;

  /**
  * Rule UnicodeString
  */
  UnicodeString *urulestring;

  // friend classes --------------------------------------------------------

  /**
  * Streamer used to read/write binary collation data files.
  */
  friend class RuleBasedCollatorStreamer;

  /**
  * Used to iterate over collation elements in a character source.
  */
  friend class CollationElementIterator;

  /**
  * Collator ONLY needs access to RuleBasedCollator(const Locale&,
  *                                                       UErrorCode&)
  */
  friend class Collator;

  // private constructors --------------------------------------------------

  /**
   * Default constructor
   */
  RuleBasedCollator();

  /**
  * Constructor that takes in a UCollator struct
  * @param collator UCollator struct
  */
  RuleBasedCollator(UCollator *collator, UnicodeString *rule);

  /**
   * RuleBasedCollator constructor. This constructor takes a locale. The
   * only caller of this class should be Collator::createInstance(). If
   * createInstance() happens to know that the requested locale's collation is
   * implemented as a RuleBasedCollator, it can then call this constructor.
   * OTHERWISE IT SHOULDN'T, since this constructor ALWAYS RETURNS A VALID
   * COLLATION TABLE. It does this by falling back to defaults.
   * @param desiredLocale locale used
   * @param status error code status
   */
  RuleBasedCollator(const Locale& desiredLocale, UErrorCode& status);

  // private methods -------------------------------------------------------

  /**
  * Creates the c struct for ucollator
  * @param locale desired locale
  * @param status error status
  */
  void setUCollator(const Locale& locale, UErrorCode& status);

  /**
  * Creates the c struct for ucollator
  * @param locale desired locale name
  * @param status error status
  */
  void setUCollator(const char* locale, UErrorCode& status);

  /**
  * Creates the c struct for ucollator
  * @param collator new ucollator data
  * @param status error status
  */
  void setUCollator(UCollator *collator);

  /**
  * Converts C's UCollationResult to EComparisonResult
  * @param result member of the enum UComparisonResult
  * @return EComparisonResult equivalent of UCollationResult
  */
  Collator::EComparisonResult getEComparisonResult(
                                          const UCollationResult &result) const;

  /**
  * Converts C's UCollationStrength to ECollationStrength
  * @param strength member of the enum UCollationStrength
  * @return ECollationStrength equivalent of UCollationStrength
  */
  Collator::ECollationStrength getECollationStrength(
                                      const UCollationStrength &strength) const;

  /**
  * Converts C++'s ECollationStrength to UCollationStrength
  * @param strength member of the enum ECollationStrength
  * @return UCollationStrength equivalent of ECollationStrength
  */
  UCollationStrength getUCollationStrength(
    const Collator::ECollationStrength &strength) const;
};

// inline method implementation ---------------------------------------------

inline UBool RuleBasedCollator::operator!=(const Collator& other) const
{
  return !(*this == other);
}

inline void RuleBasedCollator::setUCollator(const char *locale,
                                               UErrorCode &status)
{
  if (U_FAILURE(status))
    return;
  if (ucollator && dataIsOwned)
    ucol_close(ucollator);
  ucollator = ucol_open(locale, &status);
}

inline void RuleBasedCollator::setUCollator(const Locale &locale,
                                               UErrorCode &status)
{
  setUCollator(locale.getName(), status);
}

inline void RuleBasedCollator::setUCollator(UCollator *collator)
{
  if (ucollator && dataIsOwned)
    ucol_close(ucollator);
  ucollator = collator;
}

inline Collator::EComparisonResult RuleBasedCollator::getEComparisonResult(
                                           const UCollationResult &result) const
{
  switch (result)
  {
  case UCOL_LESS :
    return Collator::LESS;
  case UCOL_EQUAL :
    return Collator::EQUAL;
  default :
    return Collator::GREATER;
  }
}

inline Collator::ECollationStrength RuleBasedCollator::getECollationStrength(
                                       const UCollationStrength &strength) const
{
  switch (strength)
  {
  case UCOL_PRIMARY :
    return Collator::PRIMARY;
  case UCOL_SECONDARY :
    return Collator::SECONDARY;
  case UCOL_TERTIARY :
    return Collator::TERTIARY;
  default :
    return Collator::IDENTICAL;
  }
}

inline UCollationStrength RuleBasedCollator::getUCollationStrength(
                             const Collator::ECollationStrength &strength) const
{
  switch (strength)
  {
  case Collator::PRIMARY :
    return UCOL_PRIMARY;
  case Collator::SECONDARY :
    return UCOL_SECONDARY;
  case Collator::TERTIARY :
    return UCOL_TERTIARY;
  default :
    return UCOL_IDENTICAL;
  }
}

#endif
