/*
***************************************************************************
* Copyright (C) 2008-2009, International Business Machines Corporation
* and others. All Rights Reserved.
***************************************************************************
*   file name:  uspoof.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2008Feb13
*   created by: Andy Heninger
*
*   Unicode Spoof Detection
*/

/**
 * \file
 * \brief C API: Unicode Spoof Detection
 */

#ifndef USPOOF_H
#define USPOOF_H

#include "unicode/utypes.h"
#include "unicode/uset.h"
#include "unicode/parseerr.h"

#ifdef XP_CPLUSPLUS
#include "unicode/unistr.h"
#include "unicode/uniset.h"
#endif


 /**
 *
 * <p>C API for Unicode Security and Spoofing Detection</p>
 *
 * These functions are intended to check strings, typically
 * identifiers or URLs, for the presence of combinations of
 * characters that are likely to be visually confusing - 
 * for cases where the displayed form of an identifier may
 * not be what it appears to be.
 *
 * Unicode security considerations, and descriptions of the checks
 * performed by these functions, are describe in 
 * Unicode Technical Report #36, http://unicode.org/reports/tr36, and
 * Unicode Technical Standard #39, http://unicode.org/reports/tr39
 *
 * Test functions fall into two general categorie:
 *   1.  Single String Tests.  Check whether a string (an identifier) is
 *       potentially potentiallyconfusable with any other string.
 *   2.  Two string tests.  Check whether two specific strings are confusable.
 *       This does not consider whether either of strings is potentially
 *       confusable with any string other than the exact one specified.
 *
 *
 *  Single Script Confusable Tests:
 *      Single Identifier tests:
 *         No Check
 *      Double String Tests:
 *         The two strings have the same script (or may be common only), and
 *         they are confusable.
 *
 *  Mixed Script Confusable
 *      Single Identifier Tests
 *         The source string is of mixed script, and there exists some string
 *         _in a single script_ that is confusable with it.  This test does not check
 *         for the existence of other mixed script strings that are confusable with
 *         the string being tested.
 *      Two Identifiers Test:
 *         The two strings are confusable.  At least one of them contains characters
 *         from more than one script.  Not all identifiers that are confusable with this
 *         check would fail the single string Mixed Script Confusable test.
 *         Example:   "Scripts-R-Us" with a Cylrillic (backwards looking) R would not
 *         fail the single ID test.  But, if it were written with a Cylrillic 'S',
 *         the two specific Identifiers would be mixed script confusable.
 *
 *  Whole Script Confusable
 *      Single String Test
 *         The Identifier is of a single script, and there exists another ID
 *         wholly in some other script that is confusable.
 *      Two Identifiers Test:
 *         The two IDs are confusable with the mixed script test,
 *         each ID is in a single script,
 *         and the scripts of the two IDs are not the same.
 *         
 *  Note on Scripts:
 *     Characters of script "Common" or "Inherited" are ignored when determining
 *     the script of an identifier.   When an Identifier contains only Common
 *     or Inherited characters (a somewhat pathological case), it is logically
 *     in _all_ scripts.  When a test says that two identifiers must be of the
 *     same script, such a perverse identifier always is.  (Think of the digits 1,
 *     with script = Common)
 *
 */

struct USpoofChecker;
typedef struct USpoofChecker USpoofChecker;

/**
 * Enum for the kinds of checks that USpoofChecker can perform.
 * These enum values are used both to select the set of checks that
 * will be performed, and to report results from the check function.
 *
 * @draft ICU 4.0
 */
typedef enum USpoofChecks {
    /** Applies to Two Identifier tests only.
      *   The identifiers are both from the same script and are confusable.
      */
    USPOOF_SINGLE_SCRIPT_CONFUSABLE =   1,

    /** Applies to both Single & Two Identifier Tests.
     *  Single ID test:
     *    The identifier contains multiple scripts, and
     *    is confusable with some other identifer in a single script
     *  Two ID test:
     *    The two IDs are confusable, and at least one contains
     *    characters from more than one script.
     */
    USPOOF_MIXED_SCRIPT_CONFUSABLE  =   2,

    /** Applies to both Single & Two Identifier Tests
     *  Single ID test:
     *    The identifier is of a single script, and
     *    there exists a confusable identifier in another script.
     *  Two ID test:
     *    The identifiers are confusable,
     *    each is of a single script, and
     *    the scripts of the two Identifiers are different.
     */
    USPOOF_WHOLE_SCRIPT_CONFUSABLE  =   4,
    
    /** Modifier for single, mixed & whole script checks.
        Selects between Lower Case Confusable (0) and
        Any Case Confusable (1).  */
    USPOOF_ANY_CASE                 =   8,

    /** Check that an identifer contains only characters from a
      * single script (plus chars from the common and inherited scripts.)
      * Applies to single identifier check only.
      */
    USPOOF_SINGLE_SCRIPT            =  16,
    
    /** Check that an identifier for the presence of invisble characters,
      * characters, such as zero-width spaces, or character sequences that are
      * likely not to display, such as multiple occurences of the same
      * non-spacing mark.  This does not test the input string as a whole
      * for conformance to any particular syntax for identifiers.
      */
    USPOOF_INVISIBLE                =  32,
    
    USPOOF_LOCALE_LIMIT             =  64,
    USPOOF_CHAR_LIMIT               = 128,
    USPOOF_ALL_CHECKS               = 0x7f
    };
    
    
/**
 *  Create a Unicode Spoof Checker, configured to perform all 
 *  checks except for USPOOF_LOCALE_LIMIT and USPOOF_CHAR_LIMIT.
 *  Note that additional checks may be added in the future,
 *  resulting in the changes to the default checking behavior.
 *
 *  @param status  The error code, set if this function encounters a problem.
 *  @return        the newly created Spoof Checker
 *  @draft  ICU 4.0
 */
U_DRAFT USpoofChecker * U_EXPORT2
uspoof_open(UErrorCode *status);


/**
 * Open a Spoof checker from its serialized from, stored in 32-bit-aligned memory.
 * Inverse of uspoof_serialize().
 * The memory containing the serailized data must remain valid and unchanged
 * as long as the spoof checker, or any cloned copies of the spoof checker,
 * are in use.  Ownership of the memory remains with the caller.
 * The spoof checker (and any clones) must be closed prior to deleting the
 * serialized data.
 *
 * @param data a pointer to 32-bit-aligned memory containing the serialized form of spoof data
 * @param length the number of bytes available at data;
 *               can be more than necessary
 * @param pActualLength receives the actual number of bytes at data taken up by the data;
 *                      can be NULL
 * @param status an in/out ICU UErrorCode
 * @return the spoof checker.
 *
 * @see uspoof_open
 * @see uspoof_serialize
 */
U_CAPI USpoofChecker * U_EXPORT2
uspoof_openFromSerialized(const void *data, int32_t length, int32_t *pActualLength,
                          UErrorCode *pErrorCode);

/**
  * Open a Spoof Checker from the source form of the spoof data.
  * The Three inputs correspond to the Unicode data files confusables.txt
  * confusablesWholeScript.txt and xidmdifications.txt as described in
  * Unicode UAX 39.  The syntax of the source data is as described in UAX 39 for
  * these files, and the content of these files is acceptable input.
  *
  * The character encoding of the (char *) input text is UTF-8.
  *
  * @param confusables a pointer to the confusable characters definitions,
  *                    as found in file confusables.txt from unicode.org.
  * @param confusablesLen The length of the confusables text, or -1 if the
  *                    input string is zero terminated.
  * @param confusablesWholeScript
  *                    a pointer to the whole script confusables definitions,
  *                    as found in the file xonfusablesWholeScript.txt from unicode.org.
  * @param confusablesWholeScriptLen The length of the whole script confusables text, or
  *                    -1 if the input string is zero terminated.
  * @param xidModifications A pointer to the list of additions and restrictions
  *                    to Unicode Identifier characters, as found in the
  *                    Uniocde data file xidmodifications.txt, described in UAX-39.
  * @param xidModificationsLen The length of the identifier modifications list, or
  *                    -1 if the input string is zero terminated.
  * @param errType     In the event of an error in the input, indicates
  *                    which of the input files contains the error.
  *                    The value is one of USPOOF_SINGLE_SCRIPT_CONFUSABLE or
  *                    USPOOF_WHOLE_SCRIPT_CONFUSABLE, or
  *                    zero if no errors are found.
  * @param pe          In the event of an error in the input, receives the position
  *                    in the input text (line, offset) of the error.
  * @param status      an in/out ICU UErrorCode.  Among the possible errors is
  *                    U_PARSE_ERROR, which is used to report syntax errors
  *                    in the input.
  * @return            A spoof checker that uses the rules from the input files.
  */
U_CAPI USpoofChecker * U_EXPORT2
uspoof_openFromSource(const char *confusables,  int32_t confusablesLen,
                      const char *confusablesWholeScript, int32_t confusablesWholeScriptLen,
                      int32_t *errType, UParseError *pe, UErrorCode *status);


/**
  * Close a Spoof Checker, freeing any memory that was being held by
  *   its implementation.
  */
U_DRAFT void U_EXPORT2
uspoof_close(USpoofChecker *sc);

/**
 * Clone a Spoof Checker.  The clone will be set to perform the same checks
 *   as the original source.
 *
 * @param sc       The source USpoofChecker
 * @param status   The error code, set if this function encounters a problem.
 * @return
 */
U_DRAFT USpoofChecker * U_EXPORT2
uspoof_clone(const USpoofChecker *sc, UErrorCode *status);


/**
 * Specify the set of checks that will be performed by the check
 * functions of this Spoof Checker.
 *
 * @param sc       The USpoofChecker
 * @checks         The set of checks that this spoof checker will perform.
 *                 The value is a bit set, obtained by OR-ing together
 *                 values from enum USpoofChecks.
 * @param status   The error code, set if this function encounters a problem.
 * @draft  ICU 4.0
 *
 */
U_DRAFT void U_EXPORT2
uspoof_setChecks(USpoofChecker *sc, int32_t checks, UErrorCode *status);

/**
 * Get the set of checks that this Spoof Checker has been configured to perform.
 * 
 * @param sc       The USpoofChecker
 * @param status   The error code, set if this function encounters a problem.
 * @return         The set of checks that this spoof checker will perform.
 *                 The value is a bit set, obtained by OR-ing together
 *                 values from enum USpoofChecks.
 * @draft  ICU 4.0
 *
 */
U_DRAFT int32_t U_EXPORT2
uspoof_getChecks(const USpoofChecker *sc, UErrorCode *status);

/**
 * Limit characters that are acceptable in identifiers being checked to those 
 * normally used with the languages associated with the specified locales.
 * Any previously specified list of locales is replaced by the new settings.
 *
 * A set of languages is determined from the locale(s), and
 * from those a set of acceptable Unicode scripts is determined.
 * Characters from this set of scripts, along with characters from
 * the "common" and "inherited" Unicode Script categories
 * will be permitted.
 *
 * Supplying an empty string removes all restrictions;
 * characters from any script will be allowed.
 *
 * The USPOOF_LOCALE_LIMIT test is automatically enabled for this
 * USpoofChecker when calling this function with a non-empty set
 * of locales.
 *
 * @param sc           The USpoofChecker 
 * @param localesList  A list list of locales, from which the language
 *                     and associated script are extracted.  The list
 *                     list has the format of an HTTP Accept-Language
 *                     header, and .
 * @param status       The error code, set if this function encounters a problem.
 * @draft  ICU 4.0
 */
U_DRAFT void U_EXPORT2
uspoof_setAllowedLocales(USpoofChecker *sc, const char *localesList, UErrorCode *status);

/**
 * Get a list of locales for the scripts that are acceptable in strings
 *  to be checked.  If no limitations on scripts have been specified,
 *  an empty string will be returned.
 *
 *  The format of the returned list is that of an HTTP Accept-Language
 *  header field, but it may not be identical to the original string passed
 *  to uspoof_setAllowedLocales();  the string may be
 *  reformatted, and information other than languages from the originally
 *  specified HTTP header may be omitted.
 *
 * @param sc           The USpoofChecker 
 * @param status       The error code, set if this function encounters a problem.
 * @return             A string containing a list of  locales corresponding
 *                     to the acceptable scripts, formatted like an
 *                     HTTP Accept Language value.
 *  
 * @draft ICU 4.0
 */
U_DRAFT const char * U_EXPORT2
uspoof_getAllowedLocales(USpoofChecker *sc, UErrorCode *status);


/**
 * Limit the acceptable characters to those specified by a Unicode Set.
 *   Any previously specified character limit is
 *   is replaced by the new settings.
 *
 * The USPOOF_CHAR_LIMIT test is automatically enabled for this
 * USpoofChecker by this function.
 *
 * @param sc       The USpoofChecker 
 * @param chars    A Unicode Set containing the list of
 *                 charcters that are permitted.  Ownership of the set
 *                 remains with the caller.  The incoming set is cloned by
 *                 this function, so there are no restrictions on modifying
 *                 or deleting the USet after calling this function.
 * @param status   The error code, set if this function encounters a problem.
 */
U_DRAFT void U_EXPORT2
uspoof_setAllowedChars(USpoofChecker *sc, const USet *chars, UErrorCode *status);


/**
 * Get a USet for the characters permitted in an identifier.
 * This corresponds to the limits imposed by the Set Allowed Characters
 * functions. Limitations imposed by other checks will not be
 * reflected in the set returned by this function.
 *
 * The returned set will be frozen, meaning that it cannot be modified
 * by the caller.
 *
 * Ownership of the returned set remains with the Spoof Detector.  The
 * returned set will become invalid if the spoof detector is closed,
 * or if a new set of allowed characters is specified.
 *
 *
 * @param sc       The USpoofChecker 
 * @param chars    A Unicode Set containing the complete list of
 *                 charcters that are permitted.  Ownership of the set
 *                 remains with the caller.  The incoming set is cloned by
 *                 this function, so there are no restrictions on modifying
 *                 or deleting the USet after calling this function.
 * @param status   The error code, set if this function encounters a problem.
 * @return         A USet containing the characters that are permitted by
 *                 the USPOOF_CHAR_LIMIT test.
 */
U_DRAFT const USet * U_EXPORT2
uspoof_getAllowedChars(USpoofChecker *sc, UErrorCode *status);


#ifdef XP_CPLUSPLUS
/**
 * Limit the acceptable characters to those specified by a Unicode Set.
 *   Any previously specified character limit is
 *   is replaced by the new settings.
 *
 * The USPOOF_CHAR_LIMIT test is automatically enabled for this
 * USoofChecker by this function.
 *
 * @param sc       The USpoofChecker 
 * @param chars    A Unicode Set containing the list of
 *                 charcters that are permitted.  Ownership of the set
 *                 remains with the caller.  The incoming set is cloned by
 *                 this function, so there are no restrictions on modifying
 *                 or deleting the USet after calling this function.
 * @param status   The error code, set if this function encounters a problem.
 */
U_DRAFT void U_EXPORT2
uspoof_setAllowedUnicodeSet(USpoofChecker *sc, const UnicodeSet *chars, UErrorCode *status);


/**
 * Get a UnicodeSet for the characters permitted in an identifier.
 * This corresponds to the limits imposed by the Set Allowed Characters / 
 * UnicodeSet functions. Limitations imposed by other checks will not be
 * reflected in the set returned by this function.
 *
 * The returned set will be frozen, meaning that it cannot be modified
 * by the caller.
 *
 * Ownership of the returned set remains with the Spoof Detector.  The
 * returned set will become invalid if the spoof detector is closed,
 * or if a new set of allowed characters is specified.
 *
 *
 * @param sc       The USpoofChecker 
 * @param status   The error code, set if this function encounters a problem.
 * @return         A UnicodeSet containing the characters that are permitted by
 *                 the USPOOF_CHAR_LIMIT test.
 */
U_DRAFT const UnicodeSet * U_EXPORT2
uspoof_getAllowedUnicodeSet(USpoofChecker *sc, UErrorCode *status);
#endif


/**
 * Check the specified string for possible security issues.
 * The text to be checked will typically be an indentifier of some sort.
 * The set of checks to be performed is specified with uspoof_setChecks().
 * 
 * @param sc      The USpoofChecker 
 * @param text    The string to be checked for possible security issues,
 *                in UTF-16 format.
 * @param length  the length of the string to be checked, expressed in
 *                16 bit UTF-16 code units, or -1 if the string is 
 *                zero terminated.
 * @position      An out parameter that receives the index of the
 *                first string position that fails one of the checks.
 *                This parameter may be null if the position information
 *                is not needed.
 *                If the string passes all of the requested checks the 
 *                parameter value will not be set.
 * @param status  The error code, set if an error occured while attempting to
 *                perform the check.
 *                Spoofing or security issues detected with the input string are
 *                not reported here, but through the function's return value.
 * @return        An integer value with bits set for any potential security
 *                or spoofing issues detected.  The bits are defined by
 *                enum USpoofChecks.  Zero is returned if no issues
 *                are found with the input string.
 * @draft ICU 4.0
 */
U_DRAFT int32_t U_EXPORT2
uspoof_check(const USpoofChecker *sc,
			 const UChar *text, int32_t length, 
			 int32_t *position,
			 UErrorCode *status);


/**
 * Check the specified string for possible security issues.
 * The text to be checked will typically be an indentifier of some sort.
 * The set of checks to be performed is specified with uspoof_setChecks().
 * 
 * @param sc      The USpoofChecker 
 * @param text    A UTF-8 string to be checked for possible security issues.
 * @param length  the length of the string to be checked, or -1 if the string is 
 *                zero terminated.
 * @position      An out parameter that receives the index of the
 *                first string position that fails one of the checks.
 *                This parameter may be null if the position information
 *                is not needed.
 *                If the string passes all of the requested checks the 
 *                parameter value will not be set.
 * @param status  The error code, set if an error occured while attempting to
 *                perform the check.
 *                Spoofing or security issues detected with the input string are
 *                not reported here, but through the function's return value.
 * @return        An integer value with bits set for any potential security
 *                or spoofing issues detected.  The bits are defined by
 *                enum USpoofChecks.  Zero is returned if no issues
 *                are found with the input string.
 * @draft ICU 4.0
 */
U_DRAFT int32_t U_EXPORT2
uspoof_checkUTF8(const USpoofChecker *sc,
				 const char *text, int32_t length,
				 int32_t *position,
				 UErrorCode *status);


#ifdef XP_CPLUSPLUS
/**
 * Check the specified string for possible security issues.
 * The text to be checked will typically be an indentifier of some sort.
 * The set of checks to be performed is specified with uspoof_setChecks().
 * 
 * @param sc      The USpoofChecker 
 * @param text    A UnicodeString to be checked for possible security issues.
 * @position      An out parameter that receives the index of the
 *                first string position that fails one of the checks.
 *                This parameter may be null if the position information
 *                is not needed.
 *                If the string passes all of the requested checks the 
 *                parameter value will not be set.
 * @param status  The error code, set if an error occured while attempting to
 *                perform the check.
 *                Spoofing or security issues detected with the input string are
 *                not reported here, but through the function's return value.

 * @return        An integer value with bits set for any potential security
 *                or spoofing issues detected.  The bits are defined by
 *                enum USpoofChecks.  Zero is returned if no issues
 *                are found with the input string.
 * @draft ICU 4.0
 */
U_DRAFT int32_t U_EXPORT2
uspoof_checkUnicodeString(const USpoofChecker *sc,
                          const U_NAMESPACE_QUALIFIER UnicodeString &text, 
                          int32_t *position,
                          UErrorCode *status);

#endif


/**
 * Check the whether two specified strings are visually confusable.
 * The types of confusability to be tested - single script, mixed script,
 * or whole script - are determined by the check options set for the
 * USpoofChecker.
 *
 * @param sc      The USpoofChecker
 * @param s1      The first of the two strings to be compared for 
 *                confusability.  The strings are in UTF-16 format.
 * @param length1 the length of the first string, expressed in
 *                16 bit UTF-16 code units, or -1 if the string is 
 *                zero terminated.
 * @param s2      The second of the two strings to be compared for 
 *                confusability.  The strings are in UTF-16 format.
 * @param length2 The length of the second string, expressed in
 *                16 bit UTF-16 code units, or -1 if the string is 
 *                zero terminated.
 * @position      An out parameter that receives the index of the
 *                first confusable position in the strings being checked.
 *                This parameter may be null if the position information
 *                is not needed.
 *                If the strings are not confusable the parameter value
 *                will not be set.

 * @param status  The error code, set if an error occured while attempting to
 *                perform the check.
 *                Confusability of the strings is not reported here,
 *                but through this function's return value.
 * @return        An integer value with bit(s) set corresponding to
 *                the type of confusability found, as defined by
 *                enum USpoofChecks.  Zero is returned if the strings
 *                are not confusable.
 * @draft ICU 4.0
 */
U_DRAFT int32_t U_EXPORT2
uspoof_areConfusable(const USpoofChecker *sc,
                     const UChar *s1, int32_t length1,
                     const UChar *s2, int32_t length2,
					 int32_t *position,
                     UErrorCode *status);



/**
 * Check the whether two specified strings are visually confusable.
 * The types of confusability to be tested - single script, mixed script,
 * or whole script - are determined by the check options set for the
 * USpoofChecker.
 *
 * @param sc      The USpoofChecker
 * @param s1      The first of the two strings to be compared for 
 *                confusability.  The strings are in UTF-8 format.
 * @param length1 the length of the first string, in bytes, or -1 
 *                if the string is zero terminated.
 * @param s2      The second of the two strings to be compared for 
 *                confusability.  The strings are in UTF-18 format.
 * @param length2 The length of the second string in bytes, or -1 
 *                if the string is zero terminated.
 * @position      An out parameter that receives the index of the
 *                first confusable position in the strings being checked.
 *                This parameter may be null if the position information
 *                is not needed.
 *                If the strings are not confusable the parameter value
 *                will not be set.
 * @param status  The error code, set if an error occured while attempting to
 *                perform the check.
 *                Confusability of the strings is not reported here,
 *                but through this function's return value.
 * @return        An integer value with bit(s) set corresponding to
 *                the type of confusability found, as defined by
 *                enum USpoofChecks.  Zero is returned if the strings
 *                are not confusable.
 * @draft ICU 4.0
 */
U_DRAFT int32_t U_EXPORT2
uspoof_areConfusableUTF8(const USpoofChecker *sc,
                         const char *s1, int32_t length1,
                         const char *s2, int32_t length2,
						 int32_t *position,
                         UErrorCode *status);




#ifdef XP_CPLUSPLUS
/**
 * Check the whether two specified strings are visually confusable.
 * The types of confusability to be tested - single script, mixed script,
 * or whole script - are determined by the check options set for the
 * USpoofChecker.
 *
 * @param sc      The USpoofChecker
 * @param s1      The first of the two strings to be compared for 
 *                confusability.  The strings are in UTF-8 format.
 * @param s2      The second of the two strings to be compared for 
 *                confusability.  The strings are in UTF-18 format.
 * @position      An out parameter that receives the index of the
 *                first confusable position in the strings being checked.
 *                This parameter may be null if the position information
 *                is not needed.
 *                If the strings are not confusable the parameter value
 *                will not be set.
 * @param status  The error code, set if an error occured while attempting to
 *                perform the check.
 *                Confusability of the strings is not reported here,
 *                but through this function's return value.
 * @return        An integer value with bit(s) set corresponding to
 *                the type of confusability found, as defined by
 *                enum USpoofChecks.  Zero is returned if the strings
 *                are not confusable.
 * @draft ICU 4.0
 */
U_DRAFT int32_t U_EXPORT2
uspoof_areConfusableUnicodeString(const USpoofChecker *sc,
                                  const U_NAMESPACE_QUALIFIER UnicodeString &s1,
                                  const U_NAMESPACE_QUALIFIER UnicodeString &s2,
								  int32_t *position,
                                  UErrorCode *status);
#endif


/**
  *  Get the "skeleton" for an identifier string.
  *  Skeletons are a transformation of the input string;
  *  Two strings are confusable if their skeletons are identical.
  *  See Unicode UAX 39 for additional information.
  *
  *  Using skeletons directly makes it possible to quickly check
  *  whether an identifier is confusable with any of some large
  *  set of existing identifiers, by creating an efficiently
  *  searchable collection of the skeletons.
  *
  * @param sc      The USpoofChecker
  * @param type    The type of skeleton, corresponding to which
  *                of the Unicode confusable data tables to use.
  *                The default is Mixed-Script, Lowercase.
  *                Allowed options are USPOOF_SINGLE_SCRIPT_CONFUSABLE and
  *                USPOOF_ANY_CASE_CONFUSABLE.  The two flags may be ORed.
  * @param s       The input string whose skeleton will be computed.
  * @param length  The length of the input string, expressed in 16 bit
  *                UTF-16 code units, or -1 if the string is zero terminated.
  * @param dest    The output buffer, to receive the skeleton string.
  * @param destCapacity  The length of the output buffer, in 16 bit units.
  *                The destCapacity may be zero, in which case the function will
  *                return the actual length of the skeleton.
  * @param status  The error code, set if an error occured while attempting to
  *                perform the check.
  * @return        The length of the skeleton string.  The returned length
  *                is always that of the complete skeleton, even when the
  *                supplied buffer is too small (or of zero length)
  *                
  * @draft ICU 4.0
  */
U_DRAFT int32_t U_EXPORT2
uspoof_getSkeleton(const USpoofChecker *sc,
                   USpoofChecks type,
                   const UChar *s,  int32_t length,
                   UChar *dest, int32_t destCapacity,
                   UErrorCode *status);
    
/**
  *  Get the "skeleton" for an identifier string.
  *  Skeletons are a transformation of the input string;
  *  Two strings are confusable if their skeletons are identical.
  *  See Unicode UAX 39 for additional information.
  *
  *  Using skeletons directly makes it possible to quickly check
  *  whether an identifier is confusable with any of some large
  *  set of existing identifiers, by creating an efficiently
  *  searchable collection of the skeletons.
  *
  * @param sc      The USpoofChecker 
  * @param type    The type of skeleton, corresponding to which
  *                of the Unicode confusable data tables to use.
  *                The default is Mixed-Script, Lowercase.
  *                Allowed options are USPOOF_SINGLE_SCRIPT_CONFUSABLE and
  *                USPOOF_ANY_CASE_CONFUSABLE.  The two flags may be ORed.
  * @param s       The UTF-8 format input string whose skeleton will be computed.
  * @param length  The length of the input string, in bytes,
  *                or -1 if the string is zero terminated.
  * @param dest    The output buffer, to receive the skeleton string.
  * @param destCapacity  The length of the output buffer, in bytes.
  *                The destCapacity may be zero, in which case the function will
  *                return the actual length of the skeleton.
  * @param status  The error code, set if an error occured while attempting to
  *                perform the check.  Possible Errors include U_INVALID_CHAR_FOUND
  *                   for invalid UTF-8 sequences, and
  *                   U_BUFFER_OVERFLOW_ERROR if the destination buffer is too small
  *                   to hold the complete skeleton.
  * @return        The length of the skeleton string, in bytes.  The returned length
  *                is always that of the complete skeleton, even when the
  *                supplied buffer is too small (or of zero length)
  *                
  * @draft ICU 4.0
  */   
U_DRAFT int32_t U_EXPORT2
uspoof_getSkeletonUTF8(const USpoofChecker *sc,
                       USpoofChecks type,
                       const char *s,  int32_t length,
                       char *dest, int32_t destCapacity,
                       UErrorCode *status);
    
#ifdef XP_CPLUSPLUS
/**
  *  Get the "skeleton" for an identifier string.
  *  Skeletons are a transformation of the input string;
  *  Two strings are confusable if their skeletons are identical.
  *  See Unicode UAX 39 for additional information.
  *
  *  Using skeletons directly makes it possible to quickly check
  *  whether an identifier is confusable with any of some large
  *  set of existing identifiers, by creating an efficiently
  *  searchable collection of the skeletons.
  *
  * @param sc      The USpoofChecker.
  * @param type    The type of skeleton, corresponding to which
  *                of the Unicode confusable data tables to use.
  *                The default is Mixed-Script, Lowercase.
  *                Allowed options are USPOOF_SINGLE_SCRIPT_CONFUSABLE and
  *                USPOOF_ANY_CASE_CONFUSABLE.  The two flags may be ORed.
  * @param s       The input string whose skeleton will be computed.
  * @param dest    The output string, to receive the skeleton string.
  * @param destCapacity  The length of the output buffer, in bytes.
  *                The destCapacity may be zero, in which case the function will
  *                return the actual length of the skeleton.
  * @param status  The error code, set if an error occured while attempting to
  *                perform the check.
  * @return        A reference to the destination (skeleton) string.
  *                
  * @draft ICU 4.0
  */   
U_DRAFT UnicodeString & U_EXPORT2
uspoof_getSkeletonUnicodeString(const USpoofChecker *sc,
                                USpoofChecks type,
                                const UnicodeString &s,
                                UnicodeString &dest,
                                UErrorCode *status);
#endif   /* XP_CPLUSPLUS */


/**
 * Serialize the data for a spoof detector into a chunk of memory.
 * The flattened spoof detection tables can later be used to efficiently
 * instantiate a new Spoof Detector.
 *
 * @param sc   the Spoof Detector whose data is to be serialized.
 * @param data a pointer to 32-bit-aligned memory to be filled with the data,
 *             can be NULL if capacity==0
 * @param capacity the number of bytes available at data,
 *                 or 0 for preflighting
 * @param status an in/out ICU UErrorCode; possible errors include:
 * - U_BUFFER_OVERFLOW_ERROR if the data storage block is too small for serialization
 * - U_ILLEGAL_ARGUMENT_ERROR  the data or capacity parameters are bad
 * @return the number of bytes written or needed for the spoof data
 *
 * @see utrie2_openFromSerialized()
 */
U_CAPI int32_t U_EXPORT2
uspoof_serialize(USpoofChecker *sc,
                 void *data, int32_t capacity,
                 UErrorCode *status);


#endif   /* USPOOF_H */
