/*
*******************************************************************************
*   Copyright (C) 1996-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*******************************************************************************
*/
//=============================================================================
//
// File coll.h
//
// 
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date        Name        Description
// 02/5/97      aliu        Modified createDefault to load collation data from
//                          binary files when possible.  Added related methods
//                          createCollationFromFile, chopLocale, createPathName.
// 02/11/97     aliu        Added members addToCache, findInCache, and fgCache.
// 02/12/97     aliu        Modified to create objects from RuleBasedCollator cache.
//                          Moved cache out of Collation class.
// 02/13/97     aliu        Moved several methods out of this class and into
//                          RuleBasedCollator, with modifications.  Modified
//                          createDefault() to call new RuleBasedCollator(Locale&)
//                          constructor.  General clean up and documentation.
// 02/20/97     helena      Added clone, operator==, operator!=, operator=, copy
//                          constructor and getDynamicClassID.
// 03/25/97     helena      Updated with platform independent data types.
// 05/06/97     helena      Added memory allocation error detection.
//  6/20/97     helena      Java class name change.
// 09/03/97     helena      Added createCollationKeyValues().
// 02/10/98     damiba      Added compare() with length as parameter.
// 04/23/99     stephen     Removed EDecompositionMode, merged with
//                          Normalizer::EMode.
//=============================================================================

#ifndef COLL_H
#define COLL_H


#include "locid.h"
#include "utypes.h"
#include "unistr.h"
#include "normlzr.h"

class CollationKey;

/**
 * The <code>Collator</code> class performs locale-sensitive
 * <code>String</code> comparison. You use this class to build
 * searching and sorting routines for natural language text.
 *
 * <p>
 * <code>Collator</code> is an abstract base class. Subclasses
 * implement specific collation strategies. One subclass,
 * <code>RuleBasedCollator</code>, is currently provided
 * and is applicable to a wide set of languages. Other
 * subclasses may be created to handle more specialized needs.
 *
 * <p>
 * Like other locale-sensitive classes, you can use the static
 * factory method, <code>getInstance</code>, to obtain the appropriate
 * <code>Collator</code> object for a given locale. You will only need
 * to look at the subclasses of <code>Collator</code> if you need
 * to understand the details of a particular collation strategy or
 * if you need to modify that strategy.
 *
 * <p>
 * The following example shows how to compare two strings using
 * the <code>Collator</code> for the default locale.
 * <blockquote>
 * <pre>
 * // Compare two strings in the default locale
 * UErrorCode success = U_ZERO_ERROR;
 * Collator* myCollator = Collator::createInstance(success);
 * if( myCollator->compare("abc", "ABC") &lt; 0 ) {
 *     cout &lt;&lt; "abc is less than ABC" &lt;&lt; endl;
 * }else{
 *     cout &lt;&lt; "abc is greater than or equal to ABC" &lt;&lt; endl;
 * }
 * </pre>
 * </blockquote>
 *
 * <p>
 * You can set a <code>Collator</code>'s <em>strength</em> property
 * to determine the level of difference considered significant in
 * comparisons. Four strengths are provided: <code>PRIMARY</code>,
 * <code>SECONDARY</code>, <code>TERTIARY</code>, and <code>IDENTICAL</code>.
 * The exact assignment of strengths to language features is
 * locale dependant.  For example, in Czech, "e" and "f" are considered
 * primary differences, while "e" and "\u00EA" are secondary differences,
 * "e" and "E" are tertiary differences and "e" and "e" are identical.
 * The following shows how both case and accents could be ignored for
 * US English.
 * <blockquote>
 * <pre>
 * //Get the Collator for US English and set its strength to PRIMARY
 * UErrorCode success = U_ZERO_ERROR;
 * Collator* usCollator = Collator::createInstance(Locale::US, success);
 * usCollator->setStrength(Collator::PRIMARY);
 * if( usCollator->compare("abc", "ABC") == 0 ) {
 *     cout &lt;&lt; "'abc' and 'ABC' strings are equivalent with strength PRIMARY" &lt;&lt; endl;
 * }
 * </pre>
 * </blockquote>
 * <p>
 * For comparing <code>String</code>s exactly once, the <code>compare</code>
 * method provides the best performance. When sorting a list of
 * <code>String</code>s however, it is generally necessary to compare each
 * <code>String</code> multiple times. In this case, <code>CollationKey</code>s
 * provide better performance. The <code>CollationKey</code> class converts
 * a <code>String</code> to a series of bits that can be compared bitwise
 * against other <code>CollationKey</code>s. A <code>CollationKey</code> is
 * created by a <code>Collator</code> object for a given <code>String</code>.
 * <p>
 * <strong>Note:</strong> <code>Collator</code>s with different Locale,
 * CollationStrength and DecompositionMode settings will return different
 * sort orders for the same set of strings. Locales have specific 
 * collation rules, and the way in which secondary and tertiary differences 
 * are taken into account, for example, will result in a different sorting order
 * for same strings.
 * <p>
 * 
 * @see         RuleBasedCollator
 * @see         CollationKey
 * @see         CollationElementIterator
 * @see         Locale
 * @see         Normalizer
 * @version     1.7 1/14/97
 * @author      Helena Shih
 */

class U_I18N_API Collator {
public:
  /**
   * Base letter represents a primary difference.  Set comparison
   * level to PRIMARY to ignore secondary and tertiary differences.
   * Use this to set the strength of a Collator object.
   * Example of primary difference, "abc" &lt; "abd"
   * 
   * Diacritical differences on the same base letter represent a secondary
   * difference.  Set comparison level to SECONDARY to ignore tertiary
   * differences. Use this to set the strength of a Collator object.
   * Example of secondary difference, "ä" >> "a".
   *
   * Uppercase and lowercase versions of the same character represents a
   * tertiary difference.  Set comparison level to TERTIARY to include
   * all comparison differences. Use this to set the strength of a Collator
   * object.
   * Example of tertiary difference, "abc" &lt;&lt;&lt; "ABC".
   *
   * Two characters are considered "identical" when they have the same
   * unicode spellings.
   * For example, "ä" == "ä".
   *
   * ECollationStrength is also used to determine the strength of sort keys 
   * generated from Collator objects.
   */
  enum ECollationStrength {
    PRIMARY = 0,
    SECONDARY = 1, 
    TERTIARY = 2,
    IDENTICAL = 3
  };

  /**
   * LESS is returned if source string is compared to be less than target
   * string in the compare() method.
   * EQUAL is returned if source string is compared to be equal to target
   * string in the compare() method.
   * GREATER is returned if source string is compared to be greater than
   * target string in the compare() method.
   * @see Collator#compare
   */
  enum EComparisonResult {
    LESS = -1,
    EQUAL = 0,
    GREATER = 1
  };
  
  /**
   * Destructor
   */
  virtual                         ~Collator();

  /**
   * Returns true if "other" is the same as "this"
   */
  virtual     bool_t              operator==(const Collator& other) const;

  /**
   * Returns true if "other" is not the same as "this".
   */
  virtual     bool_t              operator!=(const Collator& other) const;

  /**
   * Makes a shallow copy of the current object.
   */
  virtual     Collator*           clone(void) const = 0;
  /**
   * Creates the collator object for the current default locale.
   * The default locale is determined by Locale::getDefault.
   * @return the collation object of the default locale.(for example, en_US)
   * @see Locale#getDefault
   * The UErrorCode& err parameter is used to return status information to the user.
   * To check whether the construction succeeded or not, you should check
   * the value of U_SUCCESS(err).  If you wish more detailed information, you
   * can check for informational error results which still indicate success.
   * U_USING_FALLBACK_ERROR indicates that a fall back locale was used.  For
   * example, 'de_CH' was requested, but nothing was found there, so 'de' was
   * used.  U_USING_DEFAULT_ERROR indicates that the default locale data was
   * used; neither the requested locale nor any of its fall back locales
   * could be found.
   * The caller owns the returned object and is responsible for deleting it.
   */
  static  Collator*           createInstance( UErrorCode&  err);

  /**
   * Gets the table-based collation object for the desired locale.  The
   * resource of the desired locale will be loaded by ResourceLoader. 
   * Locale::ENGLISH is the base collation table and all other languages are 
   * built on top of it with additional language-specific modifications.
   * @param desiredLocale the desired locale to create the collation table
   * with.
   * @return the created table-based collation object based on the desired
   * locale.
   * @see Locale
   * @see ResourceLoader
   * The UErrorCode& err parameter is used to return status information to the user.
   * To check whether the construction succeeded or not, you should check
   * the value of U_SUCCESS(err).  If you wish more detailed information, you
   * can check for informational error results which still indicate success.
   * U_USING_FALLBACK_ERROR indicates that a fall back locale was used.  For
   * example, 'de_CH' was requested, but nothing was found there, so 'de' was
   * used.  U_USING_DEFAULT_ERROR indicates that the default locale data was
   * used; neither the requested locale nor any of its fall back locales
   * could be found.
   * The caller owns the returned object and is responsible for deleting it.
   */
  static  Collator*           createInstance( const Locale&   loc,
                          UErrorCode&      err);

  // comparison
  /**
   * The comparison function compares the character data stored in two
   * different strings.  Returns information about whether a string
   * is less than, greater than or equal to another string.
   * <p>Example of use:
   * <pre>
   * .       UErrorCode status = U_ZERO_ERROR;
   * .       Collator *myCollation = Collator::createInstance(Locale::US, status);
   * .       if (U_FAILURE(status)) return;
   * .       myCollation->setStrength(Collator::PRIMARY);
   * .       // result would be Collator::EQUAL ("abc" == "ABC")
   * .       // (no primary difference between "abc" and "ABC")
   * .       Collator::EComparisonResult result = myCollation->compare("abc", "ABC");
   * .       myCollation->setStrength(Collator::TERTIARY);
   * .       // result would be Collator::LESS (abc" &lt;&lt;&lt; "ABC")
   * .       // (with tertiary difference between "abc" and "ABC")
   * .       Collator::EComparisonResult result = myCollation->compare("abc", "ABC");
   * </pre>
   * @param source the source string to be compared with.
   * @param target the string that is to be compared with the source string.
   * @return Returns a byte value. GREATER if source is greater
   * than target; EQUAL if source is equal to target; LESS if source is less
   * than target
   **/
  virtual EComparisonResult   compare(    const   UnicodeString&  source, 
                      const   UnicodeString&  target) const = 0;

  /**
   * Does the same thing as compare but limits the comparison to a specified length
   * <p>Example of use:
   * <pre>
   * .       UErrorCode status = U_ZERO_ERROR;
   * .       Collator *myCollation = Collator::createInstance(Locale::US, status);
   * .       if (U_FAILURE(status)) return;
   * .       myCollation->setStrength(Collator::PRIMARY);
   * .       // result would be Collator::EQUAL ("abc" == "ABC")
   * .       // (no primary difference between "abc" and "ABC")
   * .       Collator::EComparisonResult result = myCollation->compare("abc", "ABC",3);
   * .       myCollation->setStrength(Collator::TERTIARY);
   * .       // result would be Collator::LESS (abc" &lt;&lt;&lt; "ABC")
   * .       // (with tertiary difference between "abc" and "ABC")
   * .       Collator::EComparisonResult result = myCollation->compare("abc", "ABC",3);
   * </pre>
   * @param source the source string to be compared with.
   * @param target the string that is to be compared with the source string.
   * @param length the length the comparison is limitted to
   * @return Returns a byte value. GREATER if source (up to the specified length) is greater
   * than target; EQUAL if source (up to specified length) is equal to target; LESS if source
   * (up to the specified length) is less  than target.   
   **/

  virtual EComparisonResult   compare(    const   UnicodeString&  source,
                      const   UnicodeString&  target,
                      int32_t length) const = 0;
    
    

  /** Transforms the string into a series of characters that can be compared
   * with CollationKey::compareTo. It is not possible to restore the original
   * string from the chars in the sort key.  The generated sort key handles 
   * only a limited number of ignorable characters.
   * <p>Use CollationKey::equals or CollationKey::compare to compare the
   * generated sort keys.
   * <p>Example of use:
   * <pre>
   * .       UErrorCode status = U_ZERO_ERROR;
   * .       Collator *myCollation = Collator::createInstance(Locale::US, status);
   * .       if (U_FAILURE(status)) return;
   * .       myCollation->setStrength(Collator::PRIMARY);
   * .       UErrorCode key1Status, key2Status;
   * .       CollationKey CollationKey1
   * .       CollationKey1 = myCollation->getCollationKey("abc", CollationKey1, key1Status);
   * .       CollationKey CollationKey2
   * .       CollationKey2 = myCollation->getCollationKey("ABC", CollationKey2, key2Status);
   * .       if (U_FAILURE(key1Status) || U_FAILURE(key2Status)) { delete myCollation; return; }
   * .       // Use CollationKey::compare() to compare the sort keys
   * .       // result would be 0 (CollationKey1 == CollationKey2)
   * .       int result = CollationKey1.compare(CollationKey2);
   * .       myCollation->setStrength(Collator::TERTIARY);
   * .       CollationKey1 = myCollation->getCollationKey("abc", CollationKey1, key1Status);
   * .       CollationKey2 = myCollation->getCollationKey("ABC", CollationKey2, key2Status);
   * .       if (U_FAILURE(key1Status) || U_FAILURE(key2Status)) { delete myCollation; return; }
   * .       // Use CollationKey::compareTo to compare the collation keys
   * .       // result would be -1 (CollationKey1 &lt; CollationKey2)
   * .       result = CollationKey1.compareTo(CollationKey2);
   * .       delete myCollation;
   * </pre>
   * <p>If the source string is null, a null collation key will be returned.
   * @param source the source string to be transformed into a sort key.
   * @param key the collation key to be filled in
   * @return the collation key of the string based on the collation rules.
   * @see CollationKey#compare
   */
  virtual CollationKey&       getCollationKey(const   UnicodeString&  source,
                          CollationKey&       key,
                          UErrorCode&      status) const = 0;
  /**
   * Generates the hash code for the collation object
   */
  virtual int32_t             hashCode(void) const = 0;

  /**
   * Convenience method for comparing two strings based on
   * the collation rules.
   * @param source the source string to be compared with.
   * @param target the target string to be compared with.
   * @return true if the first string is greater than the second one,
   * according to the collation rules. false, otherwise.
   * @see Collator#compare
   */
  bool_t              greater(    const   UnicodeString& source, 
                  const   UnicodeString& target) const;
  /**
   * Convenience method for comparing two strings based on the collation
   * rules.
   * @param source the source string to be compared with.
   * @param target the target string to be compared with.
   * @return true if the first string is greater than or equal to the
   * second one, according to the collation rules. false, otherwise.
   * @see Collator#compare
   */
  bool_t              greaterOrEqual( const   UnicodeString& source, 
                      const   UnicodeString& target) const;
  /**
   * Convenience method for comparing two strings based on the collation
   * rules.
   * @param source the source string to be compared with.
   * @param target the target string to be compared with.
   * @return true if the strings are equal according to the collation
   * rules.  false, otherwise.
   * @see Collator#compare
   */
  bool_t              equals( const   UnicodeString& source, 
                  const   UnicodeString& target) const;
        
  // getter/setter
  /**
   * Get the decomposition mode of the collator object.
   * @return the decomposition mode
   * @see Collator#setDecomposition
   */
  Normalizer::EMode  getDecomposition(void) const;
  /**
   * Set the decomposition mode of the collator object. success is equal
   * to U_ILLEGAL_ARGUMENT_ERROR if error occurs.
   * @param the new decomposition mode
   * @see Collator#getDecomposition
   */
  void                setDecomposition(Normalizer::EMode  mode);
  /**
   * Determines the minimum strength that will be use in comparison or
   * transformation.
   * <p>E.g. with strength == SECONDARY, the tertiary difference is ignored
   * <p>E.g. with strength == PRIMARY, the secondary and tertiary difference
   * are ignored.
   * @return the current comparison level.
   * @see Collator#setStrength
   */
  ECollationStrength  getStrength(void) const;
  /**
   * Sets the minimum strength to be used in comparison or transformation.
   * <p>Example of use:
   * <pre>
   * .       UErrorCode status = U_ZERO_ERROR;
   * .       Collator *myCollation = Collator::createInstance(Locale::US, status);
   * .       if (U_FAILURE(status)) return;
   * .       myCollation->setStrength(Collator::PRIMARY);
   * .       // result will be "abc" == "ABC"
   * .       // tertiary differences will be ignored
   * .       Collator::ComparisonResult result = myCollation->compare("abc", "ABC");
   * </pre>
   * @see Collator#getStrength
   * @param newStrength the new comparison level.
   */
  void                setStrength(    ECollationStrength  newStrength);
  /**
   * Get name of the object for the desired Locale, in the desired langauge
   * @param objectLocale must be from getAvailableLocales
   * @param displayLocale specifies the desired locale for output
   * @param name the fill-in parameter of the return value
   * @return display-able name of the object for the object locale in the
   * desired language
   */
  static  UnicodeString&      getDisplayName( const   Locale&     objectLocale,
                          const   Locale&     displayLocale,
                          UnicodeString& name) ;
  /**
   * Get name of the object for the desired Locale, in the langauge of the
   * default locale.
   * @param objectLocale must be from getAvailableLocales
   * @param name the fill-in parameter of the return value
   * @return name of the object for the desired locale in the default
   * language
   */
  static  UnicodeString&      getDisplayName( const   Locale&         objectLocale,
                          UnicodeString&  name) ;

  /**
   * Get the set of Locales for which Collations are installed
   * @param count the output parameter of number of elements in the locale list
   * @return the list of available locales which collations are installed
   */
  static  const   Locale*     getAvailableLocales(int32_t& count);

  /**
   * Returns a unique class ID POLYMORPHICALLY.  Pure virtual method.
   * This method is to implement a simple version of RTTI, since not all
   * C++ compilers support genuine RTTI.  Polymorphic operator==() and
   * clone() methods call this method.
   *
   * Concrete subclasses of Format must implement getDynamicClassID()
   * and also a static method and data member:
   *
   *      static UClassID getStaticClassID() { return (UClassID)&fgClassID; }
   *      static char fgClassID;
   *
   * @return          The class ID for this object. All objects of a
   *                  given class have the same class ID.  Objects of
   *                  other classes have different class IDs.
   */
  virtual UClassID getDynamicClassID(void) const = 0;

protected:
  /**
   * Constructors
   */
  Collator();
  Collator(ECollationStrength collationStrength,
       Normalizer::EMode decompositionMode);
  Collator(const  Collator&   other);

  /**
   * Assignment operator
   */
  const       Collator&       operator=(const Collator&   other);

  //--------------------------------------------------------------------------
private:
            
  ECollationStrength  strength;
  Normalizer::EMode  decmp;
};

inline bool_t
Collator::operator==(const Collator& other) const
{
  bool_t result;
  if (this == &other) result = TRUE;
  else result = ((strength == other.strength) && (decmp == other.decmp));
  return result;
}

inline bool_t
Collator::operator!=(const Collator& other) const
{
  bool_t result;
  result = !(*this == other);
  return result;
}

#endif
