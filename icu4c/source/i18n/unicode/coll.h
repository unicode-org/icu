/*
******************************************************************************
*   Copyright (C) 1996-2001, International Business Machines                 *
*   Corporation and others.  All Rights Reserved.                            *
******************************************************************************
*/

/**
* File coll.h
* 
* Created by: Helena Shih
*
* Modification History:
*
*  Date        Name        Description
* 02/5/97      aliu        Modified createDefault to load collation data from
*                          binary files when possible.  Added related methods
*                          createCollationFromFile, chopLocale, createPathName.
* 02/11/97     aliu        Added members addToCache, findInCache, and fgCache.
* 02/12/97     aliu        Modified to create objects from RuleBasedCollator cache.
*                          Moved cache out of Collation class.
* 02/13/97     aliu        Moved several methods out of this class and into
*                          RuleBasedCollator, with modifications.  Modified
*                          createDefault() to call new RuleBasedCollator(Locale&)
*                          constructor.  General clean up and documentation.
* 02/20/97     helena      Added clone, operator==, operator!=, operator=, copy
*                          constructor and getDynamicClassID.
* 03/25/97     helena      Updated with platform independent data types.
* 05/06/97     helena      Added memory allocation error detection.
* 06/20/97     helena      Java class name change.
* 09/03/97     helena      Added createCollationKeyValues().
* 02/10/98     damiba      Added compare() with length as parameter.
* 04/23/99     stephen     Removed EDecompositionMode, merged with
*                          Normalizer::EMode.
* 11/02/99     helena      Collator performance enhancements.  Eliminates the 
*                          UnicodeString construction and special case for NO_OP.
* 11/23/99     srl         More performance enhancements. Inlining of
*                          critical accessors.
* 05/15/00     helena      Added version information API. 
* 01/29/01     synwee      Modified into a C++ wrapper which calls C apis 
*                          (ucoll.h). 
*/

#ifndef COLL_H
#define COLL_H

#include "unicode/ucol.h"
#include "unicode/normlzr.h"
#include "unicode/locid.h"

class CollationKey;

/**
* The <code>Collator</code> class performs locale-sensitive string 
* comparison.<br>
* You use this class to build searching and sorting routines for natural 
* language text.<br>
* <em>Important: </em>The ICU collation implementation is being reworked.
* This means that collation results and especially sort keys will change
* from ICU 1.6 to 1.7 and again to 1.8.
* For details, see the 
* <a href="http://oss.software.ibm.com/icu/develop/ICU_collation_design.htm">
* collation design document</a>.
* <p>
* <code>Collator</code> is an abstract base class. Subclasses implement 
* specific collation strategies. One subclass, 
* <code>RuleBasedCollator</code>, is currently provided and is applicable 
* to a wide set of languages. Other subclasses may be created to handle more 
* specialized needs.
* <p>
* Like other locale-sensitive classes, you can use the static factory method, 
* <code>createInstance</code>, to obtain the appropriate 
* <code>Collator</code> object for a given locale. You will only need to 
* look at the subclasses of <code>Collator</code> if you need to 
* understand the details of a particular collation strategy or if you need to 
* modify that strategy.
* <p>
* The following example shows how to compare two strings using the 
* <code>Collator</code> for the default locale.
* <blockquote>
* <pre>
* \code
* // Compare two strings in the default locale
* UErrorCode success = U_ZERO_ERROR;
* Collator* myCollator = Collator::createInstance(success);
* if (myCollator->compare("abc", "ABC") < 0)
*   cout << "abc is less than ABC" << endl;
* else
*   cout << "abc is greater than or equal to ABC" << endl;
* \endcode
* </pre>
* </blockquote>
* <p>
* You can set a <code>Collator</code>'s <em>strength</em> property to 
* determine the level of difference considered significant in comparisons. 
* Four strengths are provided: <code>PRIMARY</code>, <code>SECONDARY</code>, 
* <code>TERTIARY</code>, and <code>IDENTICAL</code>. The exact assignment of 
* strengths to language features is locale dependant. For example, in Czech, 
* "e" and "f" are considered primary differences, while "e" and "\u00EA" are 
* secondary differences, "e" and "E" are tertiary differences and "e" and "e" 
* are identical. The following shows how both case and accents could be 
* ignored for US English.
* <blockquote>
* <pre>
* \code
* //Get the Collator for US English and set its strength to PRIMARY 
* UErrorCode success = U_ZERO_ERROR;
* Collator* usCollator = 
*                            Collator::createInstance(Locale::US, success);
* usCollator->setStrength(Collator::PRIMARY);
* if (usCollator->compare("abc", "ABC") == 0)
*   cout << 
* "'abc' and 'ABC' strings are equivalent with strength PRIMARY" << 
* endl;
* \endcode
* </pre>
* </blockquote>
* <p>
* For comparing strings exactly once, the <code>compare</code> method 
* provides the best performance. When sorting a list of strings however, it 
* is generally necessary to compare each string multiple times. In this case, 
* sort keys provide better performance. The <code>getSortKey</code> methods 
* convert a string to a series of bytes that can be compared bitwise against 
* other sort keys using <code>strcmp()</code>. Sort keys are written as 
* zero-terminated byte strings. They consist of several substrings, one for 
* each collation strength level, that are delimited by 0x01 bytes.
* If the string code points are appended for UCOL_IDENTICAL, then they are 
* processed for correct code point order comparison and may contain 0x01 
* bytes but not zero bytes.
* </p>
* <p>
* An older set of APIs returns a <code>CollationKey</code> object that wraps 
* the sort key bytes instead of returning the bytes themselves.
* Its use is deprecated, but it is still available for compatibility with 
* Java.
* </p>
* <p>
* <strong>Note:</strong> <code>Collator</code>s with different Locale,
* CollationStrength and DecompositionMode settings will return different sort 
* orders for the same set of strings. Locales have specific collation rules, 
* and the way in which secondary and tertiary differences are taken into 
* account, for example, will result in a different sorting order for same 
* strings.
* </p>
* @see         RuleBasedCollator
* @see         CollationKey
* @see         CollationElementIterator
* @see         Locale
* @see         Normalizer
* @version     1.7 1/14/97
*/

class U_I18N_API Collator
{
public:

  // Collator public enums -----------------------------------------------

  /**
  * Base letter represents a primary difference. Set comparison level to 
  * PRIMARY to ignore secondary and tertiary differences.<br>
  * Use this to set the strength of a Collator object.<br>
  * Example of primary difference, "abc" &lt; "abd"
  * 
  * Diacritical differences on the same base letter represent a secondary
  * difference. Set comparison level to SECONDARY to ignore tertiary
  * differences. Use this to set the strength of a Collator object.<br>
  * Example of secondary difference, "ä" >> "a".
  *
  * Uppercase and lowercase versions of the same character represents a
  * tertiary difference.  Set comparison level to TERTIARY to include all 
  * comparison differences. Use this to set the strength of a Collator
  * object.<br>
  * Example of tertiary difference, "abc" &lt;&lt;&lt; "ABC".
  *
  * Two characters are considered "identical" when they have the same unicode 
  * spellings.<br>
  * For example, "ä" == "ä".
  *
  * UCollationStrength is also used to determine the strength of sort keys 
  * generated from Collator objects.
  */
  enum ECollationStrength 
  {
    PRIMARY    = 0,
    SECONDARY  = 1, 
    TERTIARY   = 2,
    QUATERNARY = 3,
    IDENTICAL  = 15
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
  enum EComparisonResult 
  {
    LESS = -1,
    EQUAL = 0,
    GREATER = 1
  };
  
  // Collator public destructor -----------------------------------------
  
  /**
  * Destructor
  */
  virtual ~Collator();

  // Collator public methods --------------------------------------------

  /**
  * Returns true if "other" is the same as "this"
  * @param other Collator object to be compared
  * @return true if other is the same as this.
  */
  virtual UBool operator==(const Collator& other) const;

  /**
  * Returns true if "other" is not the same as "this".
  * @param other Collator object to be compared
  * @return true if other is not the same as this.
  */
  virtual UBool operator!=(const Collator& other) const;

  /**
  * Makes a shallow copy of the current object.
  * @return a copy of this object
  */
  virtual Collator* clone(void) const = 0;

  /**
  * Creates the Collator object for the current default locale.
  * The default locale is determined by Locale::getDefault.
  * The UErrorCode& err parameter is used to return status information to the user.
  * To check whether the construction succeeded or not, you should check the 
  * value of U_SUCCESS(err).  If you wish more detailed information, you can 
  * check for informational error results which still indicate success.
  * U_USING_FALLBACK_ERROR indicates that a fall back locale was used. For
  * example, 'de_CH' was requested, but nothing was found there, so 'de' was
  * used. U_USING_DEFAULT_ERROR indicates that the default locale data was
  * used; neither the requested locale nor any of its fall back locales
  * could be found.
  * The caller owns the returned object and is responsible for deleting it.
  * @return the collation object of the default locale.(for example, en_US)
  * @see Locale#getDefault
  */
  static Collator* createInstance(UErrorCode&  err);

  /**
  * Gets the table-based collation object for the desired locale. The
  * resource of the desired locale will be loaded by ResourceLoader. 
  * Locale::ENGLISH is the base collation table and all other languages are 
  * built on top of it with additional language-specific modifications.
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
  * @param desiredLocale the desired locale to create the collation table
  *        with.
  * @return the created table-based collation object based on the desired
  *         locale.
  * @see Locale
  * @see ResourceLoader
  */
  static Collator* createInstance(const Locale& loc, UErrorCode& err);

  /**
   * Create a Collator with a specific version.
   * This is the same as createInstance(loc, err) except that getVersion() of
   * the returned object is guaranteed to be the same as the version
   * parameter.
   * This is designed to be used to open the same collator for a given
   * locale even when ICU is updated.
   * The same locale and version guarantees the same sort keys and
   * comparison results.
   *
   * @param loc The locale ID for which to open a collator.
   * @param version The requested collator version.
   * @param err A reference to a UErrorCode,
   *            must not indicate a failure before calling this function.
   * @return A pointer to a Collator, or 0 if an error occurred
   *         or a collator with the requested version is not available.
   *
   * @see getVersion
   * @draft ICU 1.8
   */
  static Collator *createInstance(const Locale &loc, UVersionInfo version, UErrorCode &err);

  /**
  * The comparison function compares the character data stored in two
  * different strings. Returns information about whether a string is less 
  * than, greater than or equal to another string.
  * <p>Example of use:
  * <pre>
  * \code
  *   UErrorCode status = U_ZERO_ERROR;
  *   Collator*myCollation = Collator::createInstance(Locale::US, 
  *                                                          status);
  *   if (U_FAILURE(status)) return;
  *   myCollation->setStrength(Collator::PRIMARY);
  *   // result would be Collator::EQUAL ("abc" == "ABC")
  *   // (no primary difference between "abc" and "ABC")
  *   UCollationResult result = myCollation->compare("abc", "ABC");
  *   myCollation->setStrength(Collator::TERTIARY);
  *   // result would be Collator::LESS ("abc" <<< "ABC")
  *   // (with tertiary difference between "abc" and "ABC")
  *   UCollationResult result = myCollation->compare("abc", "ABC");
  * \endcode
  * </pre>
  * @param source the source string to be compared with.
  * @param target the string that is to be compared with the source string.
  * @return Returns a byte value. GREATER if source is greater
  * than target; EQUAL if source is equal to target; LESS if source is less
  * than target
  * @stable
  **/
  virtual EComparisonResult compare(const UnicodeString& source, 
                                    const UnicodeString& target) const = 0;

  /**
  * Does the same thing as compare but limits the comparison to a specified 
  * length
  *
  *
  *
  * <p>Example of use:
  * <pre>
  * \code
  *   UErrorCode status = U_ZERO_ERROR;
  *   Collator*myCollation = Collator::createInstance(Locale::US, 
  *                                                          status);
  *   if (U_FAILURE(status)) return;
  *   myCollation->setStrength(Collator::PRIMARY);
  *   // result would be Collator::EQUAL ("abc" == "ABC")
  *   // (no primary difference between "abc" and "ABC")
  *   UCollationResult result = myCollation->compare("abc", "ABC",3);
  *   myCollation->setStrength(Collator::TERTIARY);
  *   // result would be Collator::LESS (abc" <<< "ABC")
  *   // (with tertiary difference between "abc" and "ABC")
  *   UCollationResult result = myCollation->compare("abc", "ABC",3);
  * \endcode
  * </pre>
  * @param source the source string to be compared with.
  * @param target the string that is to be compared with the source string.
  * @param length the length the comparison is limitted to
  * @return Returns a byte value. GREATER if source (up to the specified 
  *         length) is greater than target; EQUAL if source (up to specified 
  *         length) is equal to target; LESS if source (up to the specified 
  *         length) is less  than target.   
  */
  virtual EComparisonResult compare(const UnicodeString& source,
                                    const UnicodeString& target,
                                    int32_t length) const = 0;
    
  /**
  * The comparison function compares the character data stored in two
  * different string arrays. Returns information about whether a string array 
  * is less than, greater than or equal to another string array.
  * <p>Example of use:
  * <pre>
  * \code
  *   UErrorCode status = U_ZERO_ERROR;
  *   Collator*myCollation = Collator::createInstance(Locale::US, 
  *                                                          status);
  *   if (U_FAILURE(status)) return;
  *   myCollation->setStrength(Collator::PRIMARY);
  *   // result would be Collator::EQUAL ("abc" == "ABC")
  *   // (no primary difference between "abc" and "ABC")
  *   UCollationResult result = myCollation->compare(L"abc", 3, L"ABC", 3);
  *   myCollation->setStrength(Collator::TERTIARY);
  *   // result would be Collator::LESS (abc" <<< "ABC")
  *   // (with tertiary difference between "abc" and "ABC")
  *   UCollationResult result = myCollation->compare(L"abc", 3, L"ABC", 3);
  * \endcode
  * </pre>
  * @param source the source string array to be compared with.
  * @param sourceLength the length of the source string array.  If this value
  *        is equal to -1, the string array is null-terminated.
  * @param target the string that is to be compared with the source string.
  * @param targetLength the length of the target string array.  If this value
  *        is equal to -1, the string array is null-terminated.
  * @return Returns a byte value. GREATER if source is greater than target; 
  *         EQUAL if source is equal to target; LESS if source is less than 
  *         target
  */
  virtual EComparisonResult compare(const UChar* source, int32_t sourceLength,
                                    const UChar* target, int32_t targetLength) 
                                    const = 0;

  /** 
  * Transforms the string into a series of characters that can be compared
  * with CollationKey::compareTo. It is not possible to restore the original
  * string from the chars in the sort key.  The generated sort key handles 
  * only a limited number of ignorable characters.
  * <p>Use CollationKey::equals or CollationKey::compare to compare the
  * generated sort keys.
  * <p>Example of use:
  * <pre>
  * \code
  *  UErrorCode status = U_ZERO_ERROR;
  *  Collator*myCollation = Collator::createInstance(Locale::US, 
  *                                                        status);
  *  if (U_FAILURE(status)) return;
  *  myCollation->setStrength(Collator::PRIMARY);
  *  UErrorCode key1Status, key2Status;
  *  CollationKey CollationKey1
  *  CollationKey1 = myCollation->getCollationKey("abc", CollationKey1, 
  *                                               key1Status);
  *  CollationKey CollationKey2
  *  CollationKey2 = myCollation->getCollationKey("ABC", CollationKey2, 
  *                                               key2Status);
  *  if (U_FAILURE(key1Status) || U_FAILURE(key2Status)) 
  *  {
  *    delete myCollation; 
  *    return;
  *  }
  *  // Use CollationKey::compare() to compare the sort keys
  *  // result would be 0 (CollationKey1 == CollationKey2)
  *  int result = CollationKey1.compare(CollationKey2);
  *  myCollation->setStrength(Collator::TERTIARY);
  *  CollationKey1 = myCollation->getCollationKey("abc", CollationKey1, 
  *                                               key1Status);
  *  CollationKey2 = myCollation->getCollationKey("ABC", CollationKey2, 
  *                                               key2Status);
  *  if (U_FAILURE(key1Status) || U_FAILURE(key2Status)) 
  *  { 
  *    delete myCollation; 
  *    return; 
  *  }
  *  // Use CollationKey::compareTo to compare the collation keys
  *  // result would be -1 (CollationKey1 < CollationKey2)
  *  result = CollationKey1.compareTo(CollationKey2);
  *  delete myCollation;
  * \endcode
  * </pre>
  * <p>
  * If the source string is null, a null collation key will be returned.
  * @param source the source string to be transformed into a sort key.
  * @param key the collation key to be filled in
  * @return the collation key of the string based on the collation rules.
  * @see CollationKey#compare
  */
  virtual CollationKey& getCollationKey(const UnicodeString&  source,
                                        CollationKey& key,
                                        UErrorCode& status) const = 0;

  /** 
  * Transforms the string into a series of characters that can be compared
  * with CollationKey::compareTo. It is not possible to restore the original
  * string from the chars in the sort key.  The generated sort key handles 
  * only a limited number of ignorable characters.
  * <p>Use CollationKey::equals or CollationKey::compare to compare the
  * generated sort keys.
  * <p>If the source string is null, a null collation key will be returned.
  * @param source the source string to be transformed into a sort key.
  * @param sourceLength length of the collation key
  * @param key the collation key to be filled in
  * @return the collation key of the string based on the collation rules.
  * @see CollationKey#compare
  */
  virtual CollationKey& getCollationKey(const UChar*source, 
                                        int32_t sourceLength,
					                              CollationKey& key,
					                              UErrorCode& status) const = 0;
  /**
  * Generates the hash code for the collation object
  */
  virtual int32_t hashCode(void) const = 0;

  /**
  * Convenience method for comparing two strings based on the collation rules.
  * @param source the source string to be compared with.
  * @param target the target string to be compared with.
  * @return true if the first string is greater than the second one,
  *         according to the collation rules. false, otherwise.
  * @see Collator#compare
  */
  UBool greater(const UnicodeString& source, const UnicodeString& target) 
                const;

  /**
  * Convenience method for comparing two strings based on the collation rules.
  * @param source the source string to be compared with.
  * @param target the target string to be compared with.
  * @return true if the first string is greater than or equal to the second 
  *         one, according to the collation rules. false, otherwise.
  * @see Collator#compare
  */
  UBool greaterOrEqual(const UnicodeString& source, 
                       const UnicodeString& target) const;
  /**
  * Convenience method for comparing two strings based on the collation rules.
  * @param source the source string to be compared with.
  * @param target the target string to be compared with.
  * @return true if the strings are equal according to the collation rules.  
  *         false, otherwise.
  * @see Collator#compare
  */
  UBool equals(const UnicodeString& source, const UnicodeString& target) const;
        
  /**
  * Get the decomposition mode of the Collator object.
  * @return the decomposition mode
  * @see Collator#setDecomposition
  */
  virtual Normalizer::EMode getDecomposition(void) const = 0;

  /**
  * Set the decomposition mode of the Collator object. success is equal to 
  * U_ILLEGAL_ARGUMENT_ERROR if error occurs.
  * @param the new decomposition mode
  * @see Collator#getDecomposition
  */
  virtual void setDecomposition(Normalizer::EMode  mode) = 0;

  /**
  * Determines the minimum strength that will be use in comparison or
  * transformation.
  * <p>E.g. with strength == SECONDARY, the tertiary difference is ignored
  * <p>E.g. with strength == PRIMARY, the secondary and tertiary difference
  * are ignored.
  * @return the current comparison level.
  * @see Collator#setStrength
  */
  virtual ECollationStrength getStrength(void) const = 0;
  
  /**
  * Sets the minimum strength to be used in comparison or transformation.
  * <p>Example of use:
  * <pre>
  *  \code
  *  UErrorCode status = U_ZERO_ERROR;
  *  Collator*myCollation = Collator::createInstance(Locale::US, 
  *                                                         status);
  *  if (U_FAILURE(status)) return;
  *  myCollation->setStrength(Collator::PRIMARY);
  *  // result will be "abc" == "ABC"
  *  // tertiary differences will be ignored
  *  Collator::ComparisonResult result = myCollation->compare("abc", 
  *                                                              "ABC");
  * \endcode 
  * </pre>
  * @see Collator#getStrength
  * @param newStrength the new comparison level.
  * @stable
  */
  virtual void setStrength(ECollationStrength newStrength) = 0;

  /**
  * Get name of the object for the desired Locale, in the desired langauge
  * @param objectLocale must be from getAvailableLocales
  * @param displayLocale specifies the desired locale for output
  * @param name the fill-in parameter of the return value
  * @return display-able name of the object for the object locale in the
  *         desired language
  */
  static UnicodeString& getDisplayName(const Locale& objectLocale,
                                       const Locale& displayLocale,
                                       UnicodeString& name);
  /**
  * Get name of the object for the desired Locale, in the langauge of the
  * default locale.
  * @param objectLocale must be from getAvailableLocales
  * @param name the fill-in parameter of the return value
  * @return name of the object for the desired locale in the default language
  */
  static UnicodeString& getDisplayName(const Locale& objectLocale,
                                       UnicodeString& name);

  /**
  * Get the set of Locales for which Collations are installed
  * @param count the output parameter of number of elements in the locale list
  * @return the list of available locales which collations are installed
  */
  static const Locale* getAvailableLocales(int32_t& count);

  /**
  * Gets the version information for a Collator. 
  * @param info the version # information, the result will be filled in
  */
  void getVersion(UVersionInfo info) const;

  /**
  * Returns a unique class ID POLYMORPHICALLY. Pure virtual method.
  * This method is to implement a simple version of RTTI, since not all C++ 
  * compilers support genuine RTTI. Polymorphic operator==() and clone() 
  * methods call this method.
  * Concrete subclasses of Format must implement getDynamicClassID() and also 
  * a static method and data member:
  *   static UClassID getStaticClassID() 
  *   { 
  *      return (UClassID)&fgClassID; 
  *   }
  *   static char fgClassID;
  * @return The class ID for this object. All objects of a given class have 
  *         the same class ID.  Objects of other classes have different class 
  *         IDs.
  */
  virtual UClassID getDynamicClassID(void) const = 0;

  /**
  * Universal attribute setter
  * @param attr attribute type 
  * @param value attribute value
  * @param status to indicate whether the operation went on smoothly or 
  *        there were errors
  */
  virtual void setAttribute(UColAttribute attr, UColAttributeValue value, 
                            UErrorCode &status) = 0;

  /**
  * Universal attribute getter
  * @param attr attribute type
  * @param status to indicate whether the operation went on smoothly or 
  *        there were errors
  * @return attribute value
  */
  virtual UColAttributeValue getAttribute(UColAttribute attr, 
                                          UErrorCode &status) = 0;

  /**
  * Thread safe cloning operation
  * @return pointer to the new clone, user should remove it.
  */
  virtual Collator* safeClone(void) = 0;


  /**
  * String compare that uses user supplied character iteration.
  * The idea is to prevent users from having to convert the whole string into 
  * UChar's before comparing since sometimes strings differ on first couple of 
  * characters.
  * @param coll Collator to be used for comparing
  * @param source pointer to function for iterating over the first string
  * @param target pointer to function for iterating over the second string
  * @return The result of comparing the strings; one of UCOL_EQUAL,
  *         UCOL_GREATER, UCOL_LESS
  */
  virtual EComparisonResult compare(ForwardCharacterIterator &source,
								                   ForwardCharacterIterator &target) = 0;

  /**
  * Get the sort key as an array of bytes from an UnicodeString.
  * Sort key byte arrays are zero-terminated and can be compared using 
  * strcmp().
  * @param source string to be processed.
  * @param result buffer to store result in. If NULL, number of bytes needed 
  *        will be returned.
  * @param resultLength length of the result buffer. If if not enough the 
  *        buffer will be filled to capacity. 
  * @return Number of bytes needed for storing the sort key
  */
  virtual int32_t getSortKey(const UnicodeString& source, uint8_t* result,
						                 int32_t resultLength) const = 0;

  /**
  * Get the sort key as an array of bytes from an UChar buffer.
  * Sort key byte arrays are zero-terminated and can be compared using 
  * strcmp().
  * @param source string to be processed.
  * @param sourceLength length of string to be processed. 
  *			   If -1, the string is 0 terminated and length will be decided by the 
  *        function.
  * @param result buffer to store result in. If NULL, number of bytes needed 
  *        will be returned.
  * @param resultLength length of the result buffer. If if not enough the 
  *        buffer will be filled to capacity. 
  * @return Number of bytes needed for storing the sort key
  */
  virtual int32_t getSortKey(const UChar*source, int32_t sourceLength,
						                 uint8_t*result, int32_t resultLength) const = 0;

protected:

  // Collator protected constructors -------------------------------------

  /**
  * Default constructor.
  * Constructor is different from the old default Collator constructor.
  * The task for determing the default collation strength and normalization 
  * mode is left to the child class.
  */
  Collator();

  /**
  * Constructor.
  * Empty constructor, does not handle the arguments.
  * This constructor is done for backward compatibility with 1.7 and 1.8.
  * The task for handling the argument collation strength and normalization 
  * mode is left to the child class.
  * @param collationStrength collation strength
  * @param decompositionMode 
  */
  Collator(UCollationStrength collationStrength, 
              UNormalizationMode decompositionMode);
  
  /**
  * Copy constructor.
  * @param other Collator object to be copied from
  */
  Collator(const Collator& other);
  
  // Collator protected methods -----------------------------------------

private:
 
  // Collator private data members ---------------------------------------

  /*
  synwee : removed as attributes to be handled by child class
  UCollationStrength  strength;
  Normalizer::EMode  decmp;
  */
  static const UVersionInfo fVersion;
};

// Collator inline methods -----------------------------------------------

inline UBool Collator::operator==(const Collator& other) const
{
  return (UBool)(this == &other);
}

inline UBool Collator::operator!=(const Collator& other) const
{
  return (UBool)!(*this == other);
}

/*
synwee : removed since there's no attribute to be retrieved here
inline UCollationStrength Collator::getStrength() const
{
  return strength;
}

inline Normalizer::EMode Collator::getDecomposition() const
{
  return decmp;
}
*/
#endif
