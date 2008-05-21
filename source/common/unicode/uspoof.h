/*
***************************************************************************
* Copyright (C) 2008, International Business Machines Corporation
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
 */
#ifndef USPOOF_H
#define USPOOF_H

#include "unicode/utypes.h"
#include "unicode/uset.h"

#ifdef XP_CPLUSPLUS
#include "unicode/unistr.h"
#include "unicode/uniset.h"
#endif


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
    USPOOF_SINGLE_SCRIPT_CONFUSABLE =  1,
    USPOOF_MIXED_SCRIPT_CONFUSABLE  =  2,
    USPOOF_WHOLE_SCRIPT_CONFUSABLE  =  4,
    USPOOF_SECURE_ID                =  8,
    USPOOF_MIXED_SCRIPT             = 16,
    USPOOF_LOCALE_LIMIT             = 32,
    USPOOF_CHAR_LIMIT               = 64,
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
 * Specify the set of checks that will be performed by the check
 * functions of this Spoof Checker.
 *
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
uspoof_getChecks(const USpoofChecker sc, UErrorCode *status);

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
uspoof_check(const USpoofChecker *sc, const UChar *text, int32_t length, UErrorCode *status);

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
uspoof_check(const USpoofChecker *sc, const UChar *text, int32_t length, UErrorCode *status);


/**
 * Check the specified string for possible security issues.
 * The text to be checked will typically be an indentifier of some sort.
 * The set of checks to be performed is specified with uspoof_setChecks().
 * 
 * @param sc      The USpoofChecker 
 * @param text    A UTF-8 string to be checked for possible security issues.
 * @param length  the length of the string to be checked, or -1 if the string is 
 *                zero terminated.
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
uspoof_checkUTF8(const USpoofChecker *sc, const char *text, int32_t length, UErrorCode *status);


#ifdef XP_CPLUSPLUS
/**
 * Check the specified string for possible security issues.
 * The text to be checked will typically be an indentifier of some sort.
 * The set of checks to be performed is specified with uspoof_setChecks().
 * 
 * @param sc      The USpoofChecker 
 * @param text    A UnicodeString to be checked for possible security issues.
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
                          int32_t length, UErrorCode *status);

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
  * @param s       The UTF-8 format input string whose skeleton will be computed.
  * @param length  The length of the input string, in bytes,
  *                or -1 if the string is zero terminated.
  * @param dest    The output buffer, to receive the skeleton string.
  * @param destCapacity  The length of the output buffer, in bytes.
  *                The destCapacity may be zero, in which case the function will
  *                return the actual length of the skeleton.
  * @param status  The error code, set if an error occured while attempting to
  *                perform the check.
  * @return        The length of the skeleton string, in bytes.  The returned length
  *                is always that of the complete skeleton, even when the
  *                supplied buffer is too small (or of zero length)
  *                
  * @draft ICU 4.0
  */   
U_DRAFT int32_t U_EXPORT2
uspoof_getSkeletonUTF8(const USpoofChecker *sc,
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
  * @param sc      The USpoofChecker 
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
                                const UnicodeString &s,
                                UnicodeString &dest,
                                UErrorCode *status);
#endif

#endif


