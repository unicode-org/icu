/*
*******************************************************************************
* Copyright © {1996-1999}, International Business Machines Corporation and others. All Rights Reserved.
*******************************************************************************
*/

#ifndef UCOL_H
#define UCOL_H

#include "unicode/utypes.h"
#include "unicode/unorm.h"

/**
 * \file
 * \brief C API: Collator 
 *
 * <h2> Collator C API </h2>
 *
 * The C API for Collator performs locale-sensitive
 * string comparison. You use this class to build
 * searching and sorting routines for natural language text.
 * <em>Important: </em>The ICU collation implementation is being reworked.
 * This means that collation results and especially sort keys will change
 * from ICU 1.6 to 1.7 and again to 1.8.
 * For details, see the <a href="http://oss.software.ibm.com/icu/develop/ICU_collation_design.htm">collation design document</a>.
 *  
 * <p>
 * Like other locale-sensitive classes, you can use the function
 * <code>ucol_open()</code>, to obtain the appropriate pointer to 
 * <code>UCollator</code> object for a given locale. If you need
 * to understand the details of a particular collation strategy or
 * if you need to modify that strategy.
 *
 * <p>
 * The following example shows how to compare two strings using
 * the <code>UCollator</code> for the default locale.
 * <blockquote>
 * <pre>
 * \code
 * // Compare two strings in the default locale
 * UErrorCode success = U_ZERO_ERROR;
 * UCollator* myCollator = ucol_open(NULL, &success);
 * UChar source[4], target[4];
 * u_uastrcpy(source, "abc");
 * u_uastrcpy(target, "ABC");
 * if( u_strcoll(myCollator, source, u_strlen(source), target, u_strlen(target)) == UCOL_LESS) {
 *     printf("abc is less than ABC\n");
 * }else{
 *     printf("abc is greater than or equal to ABC\n");
 * }
 * \endcode
 * </pre>
 * </blockquote>
 *
 * <p>
 * You can set a <code>Collator</code>'s <em>strength</em> property
 * to determine the level of difference considered significant in
 * comparisons. Four strengths are provided: <code>UCOL_PRIMARY</code>,
 * <code>UCOL_SECONDARY</code>, <code>UCOL_TERTIARY</code>, and 
 * <code>UCOL_IDENTICAL</code>. The exact assignment of strengths to 
 * language features is locale dependant.  For example, in Czech, 
 * "e" and "f" are considered primary differences, while "e" and "\u00EA"
 * are secondary differences, "e" and "E" are tertiary differences and 
 * "e" and "e" are identical.
 * The following shows how both case and accents could be ignored for
 * US English.
 * <blockquote>
 * <pre>
 * \code
 * //Get the Collator for US English and set its strength to UCOL_PRIMARY
 * UErrorCode success = U_ZERO_ERROR;
 * UCollator* usCollator = ucol_open("en_US", &success);
 * ucol_setStrength(usCollator, UCOL_PRIMARY);
 * UChar source[4], target[4];
 * u_uastrcpy(source, "abc");
 * u_uastrcpy(target, "ABC");
 * if( u_strcoll(myCollator, source, u_strlen(source), target, u_strlen(target)) == UCOL_EQUAL) {
 *     printf("'abc' and 'ABC' strings are equivalent with strength UCOL_PRIMARY\n");
 * }
 * \endcode
 * </pre>
 * </blockquote>
 * <p>
 * For comparing strings exactly once, the <code>u_strcoll</code>
 * method provides the best performance. When sorting a list of
 * strings however, it is generally necessary to compare each
 * string multiple times. In this case, sort keys
 * provide better performance. The <code>ucol_getSortKey</code> method converts
 * a string to a series of bytes that can be compared bitwise
 * against other sort keys using <code>strcmp()</code>.
 * Sort keys are written as zero-terminated byte strings.
 * They consist of several substrings, one for each collation strength level,
 * that are delimited by 0x01 bytes.
 * If the string code points are appended for UCOL_IDENTICAL, then they are processed
 * for correct code point order comparison and may contain 0x01 bytes
 * but not zero bytes.</p>
 * <p>
 * <strong>Note:</strong> <code>UCollator</code>s with different Locale,
 * Collation Strength and Decomposition Mode settings will return different
 * sort orders for the same set of strings. Locales have specific 
 * collation rules, and the way in which secondary and tertiary differences 
 * are taken into account, for example, will result in a different sorting order
 * for same strings.
 * <p>
 * @see         UCollationResult
 * @see         UNormalizationMode
 * @see            UCollationStrength
 * @see         UCollationElements
 */
struct collIterate;
typedef struct collIterate collIterate;

struct incrementalContext;
typedef struct incrementalContext incrementalContext;

/** A collator.
*  For usage in C programs.
*/
struct UCollator;
typedef struct UCollator UCollator;


    /**
     * UCOL_LESS is returned if source string is compared to be less than target
     * string in the u_strcoll() method.
     * UCOL_EQUAL is returned if source string is compared to be equal to target
     * string in the u_strcoll() method.
     * UCOL_GREATER is returned if source string is compared to be greater than
     * target string in the u_strcoll() method.
     * @see u_strcoll()
     **/
/** Possible values for a comparison result */
typedef enum {
  /** string a == string b */
  UCOL_EQUAL    = 0,
  /** string a > string b */
  UCOL_GREATER    = 1,
  /** string a < string b */
  UCOL_LESS    = -1
} UCollationResult ;


typedef enum {
  /* accepted by most attributes */
  UCOL_DEFAULT = -1,

  /* for UCOL_STRENGTH */
  /** Primary collation strength */
  UCOL_PRIMARY = 0,
  /** Secondary collation strength */
  UCOL_SECONDARY = 1,
  /** Tertiary collation strength */
  UCOL_TERTIARY = 2,
  /** Default collation strength */
  UCOL_DEFAULT_STRENGTH = UCOL_TERTIARY,
  UCOL_CE_STRENGTH_LIMIT,
  /** Quaternary collation strength */
  UCOL_QUATERNARY=3,
  /** Identical collation strength */
  UCOL_IDENTICAL=15,
  UCOL_STRENGTH_LIMIT,

  /* for UCOL_FRENCH_COLLATION, UCOL_CASE_LEVEL & UCOL_DECOMPOSITION_MODE*/
  UCOL_OFF = 16,
  UCOL_ON = 17,
  
  /* for UCOL_ALTERNATE_HANDLING */
  UCOL_SHIFTED = 20,
  UCOL_NON_IGNORABLE = 21,

  /* for UCOL_CASE_FIRST */
  UCOL_LOWER_FIRST = 24,
  UCOL_UPPER_FIRST = 25,

  /* for UCOL_NORMALIZATION_MODE */
  UCOL_ON_WITHOUT_HANGUL = 28,

  /** No more attribute values after this*/
  UCOL_ATTRIBUTE_VALUE_COUNT

} UColAttributeValue;

    /**
     * Base letter represents a primary difference.  Set comparison
     * level to UCOL_PRIMARY to ignore secondary and tertiary differences.
     * Use this to set the strength of a Collator object.
     * Example of primary difference, "abc" &lt; "abd"
     * 
     * Diacritical differences on the same base letter represent a secondary
     * difference.  Set comparison level to UCOL_SECONDARY to ignore tertiary
     * differences. Use this to set the strength of a Collator object.
     * Example of secondary difference, "ä" >> "a".
     *
     * Uppercase and lowercase versions of the same character represents a
     * tertiary difference.  Set comparison level to UCOL_TERTIARY to include
     * all comparison differences. Use this to set the strength of a Collator
     * object.
     * Example of tertiary difference, "abc" &lt;&lt;&lt; "ABC".
     *
     * Two characters are considered "identical" when they have the same
     * unicode spellings.  UCOL_IDENTICAL.
     * For example, "ä" == "ä".
     *
     * UCollationStrength is also used to determine the strength of sort keys 
     * generated from UCollatorOld objects
     **/
/** Possible collation strengths  - all under UColAttributeValue*/
typedef UColAttributeValue UCollationStrength;

typedef enum {
     UCOL_FRENCH_COLLATION, /* attribute for direction of secondary weights*/
     UCOL_ALTERNATE_HANDLING, /* attribute for handling variable elements*/
     UCOL_CASE_FIRST, /* who goes first, lower case or uppercase */
     UCOL_CASE_LEVEL, /* do we have an extra case level */
     UCOL_NORMALIZATION_MODE, /* attribute for normalization */
     UCOL_STRENGTH,         /* attribute for strength */
     UCOL_ATTRIBUTE_COUNT
} UColAttribute;

typedef enum {
	UCOL_TAILORING_ONLY,
	UCOL_FULL_RULES
}  UColRuleOption ;

/**
 * Open a UCollatorOld for comparing strings.
 * The UCollatorOld may be used in calls to \Ref{ucol_strcoll}.
 * @param loc The locale containing the comparison conventions.
 * @param status A pointer to an UErrorCode to receive any errors
 * @return A pointer to a UCollator, or 0 if an error occurred.
 * @see ucol_openRules
 * @stable
 */

U_CAPI UCollator*
ucol_open(    const    char         *loc,
        UErrorCode      *status);

/**
 * Open a UCollatorOld for comparing strings.
 * The UCollatorOld may be used in calls to \Ref{ucol_strcoll}.
 * @param rules A string describing the collation rules.
 * @param rulesLength The length of rules, or -1 if null-terminated.
 * @param mode The normalization mode; one of UCOL_NO_NORMALIZATION,
 * UCOL_CAN_DECOMP, UCOL_COMPAT_DECOMP, UCOL_CAN_DECOMP_COMPAT_COMP,
 * UCOL_COMPAT_DECOMP_CAN_COMP, UCOL_DEFAULT_NORMALIZATION
 * @param strength The collation strength; one of UCOL_PRIMARY, UCOL_SECONDARY,
 * UCOL_TERTIARY, UCOL_IDENTICAL,UCOL_DEFAULT_STRENGTH
 * @param status A pointer to an UErrorCode to receive any errors
 * @return A pointer to a UCollator, or 0 if an error occurred.
 * @see ucol_open
 * @stable
 */
U_CAPI UCollator*
ucol_openRules(    const    UChar        *rules,
        int32_t                 rulesLength,
        UNormalizationMode      mode,
        UCollationStrength      strength,
        UErrorCode              *status);

/** 
 * Close a UCollator.
 * Once closed, a UCollatorOld should not be used.
 * @param coll The UCollatorOld to close.
 * @stable
 */
U_CAPI void
ucol_close(UCollator *coll);

/**
 * Compare two strings.
 * The strings will be compared using the normalization mode and options
 * specified in \Ref{ucol_open} or \Ref{ucol_openRules}
 * @param coll The UCollatorOld containing the comparison rules.
 * @param source The source string.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param target The target string.
 * @param targetLength The length of target, or -1 if null-terminated.
 * @return The result of comparing the strings; one of UCOL_EQUAL,
 * UCOL_GREATER, UCOL_LESS
 * @see ucol_greater
 * @see ucol_greaterOrEqual
 * @see ucol_equal
 * @stable
 */

U_CAPI UCollationResult
ucol_strcoll(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength);

/**
 * Determine if one string is greater than another.
 * This function is equivalent to \Ref{ucol_strcoll} == UCOL_GREATER
 * @param coll The UCollatorOld containing the comparison rules.
 * @param source The source string.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param target The target string.
 * @param targetLength The length of target, or -1 if null-terminated.
 * @return TRUE if source is greater than target, FALSE otherwise.
 * @see ucol_strcoll
 * @see ucol_greaterOrEqual
 * @see ucol_equal
 * @stable
 */
U_CAPI UBool
ucol_greater(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength);

/**
 * Determine if one string is greater than or equal to another.
 * This function is equivalent to \Ref{ucol_strcoll} != UCOL_LESS
 * @param coll The UCollatorOld containing the comparison rules.
 * @param source The source string.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param target The target string.
 * @param targetLength The length of target, or -1 if null-terminated.
 * @return TRUE if source is greater than or equal to target, FALSE otherwise.
 * @see ucol_strcoll
 * @see ucol_greater
 * @see ucol_equal
 * @stable
 */
U_CAPI UBool
ucol_greaterOrEqual(    const    UCollator    *coll,
            const    UChar        *source,
            int32_t            sourceLength,
            const    UChar        *target,
            int32_t            targetLength);

/**
 * Compare two strings for equality.
 * This function is equivalent to \Ref{ucol_strcoll} == UCOL_EQUAL
 * @param coll The UCollatorOld containing the comparison rules.
 * @param source The source string.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param target The target string.
 * @param targetLength The length of target, or -1 if null-terminated.
 * @return TRUE if source is equal to target, FALSE otherwise
 * @see ucol_strcoll
 * @see ucol_greater
 * @see ucol_greaterOrEqual
 * @stable
 */
U_CAPI UBool
ucol_equal(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength);

/**
 * Get the collation strength used in a UCollator.
 * The strength influences how strings are compared.
 * @param coll The UCollatorOld to query.
 * @return The collation strength; one of UCOL_PRIMARY, UCOL_SECONDARY,
 * UCOL_TERTIARY, UCOL_IDENTICAL, UCOL_DEFAULT_STRENGTH
 * @see ucol_setStrength
 * @stable
 */
U_CAPI UCollationStrength
ucol_getStrength(const UCollator *coll);

/**
 * Set the collation strength used in a UCollator.
 * The strength influences how strings are compared.
 * <p>Example of use:
 * <pre>
 * .       UCollationResult result;
 * .       UChar *source, *target;
 * .       UErrorCode status = U_ZERO_ERROR;
 * .       UCollatorOld *myCollation = ucol_open("en_US", status);
 * .       if (U_FAILURE(&status)) return;
 * .       ucol_setStrength(myCollation, UCOL_PRIMARY);
 * .       u_uastrcpy(source, "abc");
 * .       u_uastrcpy(target, "ABC");
 * .       // result will be "abc" == "ABC"
 * .       // tertiary differences will be ignored
 * .       result = ucol_strcoll(myCollation, source, u_strlen(source), target, u_strlen(target));
 * </pre>
 * @param coll The UCollatorOld to set.
 * @param strength The desired collation strength; one of UCOL_PRIMARY, 
 * UCOL_SECONDARY, UCOL_TERTIARY, UCOL_IDENTICAL, UCOL_DEFAULT_STRENGTH
 * @see ucol_getStrength
 * @stable
 */
U_CAPI void
ucol_setStrength(    UCollator            *coll,
            UCollationStrength        strength);

/**
 * Get the normalization mode used in a UCollator.
 * The normalization mode influences how strings are compared.
 * @param coll The UCollatorOld to query.
 * @return The normalization mode; one of UCOL_NO_NORMALIZATION, 
 * UCOL_CAN_DECOMP, UCOL_COMPAT_DECOMP, UCOL_CAN_DECOMP_COMPAT_COMP,
 * UCOL_COMPAT_DECOMP_CAN_COMP, UCOL_DEFAULT_NORMALIZATION
 * @see ucol_setNormalization
 * @stable
 */
U_CAPI UNormalizationMode
ucol_getNormalization(const UCollator* coll);

/**
 * Set the normalization mode used in a UCollator.
 * The normalization mode influences how strings are compared.
 * @param coll The UCollatorOld to set.
 * @param mode The desired normalization mode; one of UCOL_NO_NORMALIZATION,
 * UCOL_CAN_DECOMP, UCOL_COMPAT_DECOMP, UCOL_CAN_DECOMP_COMPAT_COMP, 
 * UCOL_COMPAT_DECOMP_CAN_COMP, UCOL_DEFAULT_NORMALIZATION
 * @see ucol_getNormalization
 * @stable
 */
U_CAPI void
ucol_setNormalization(  UCollator        *coll,
            UNormalizationMode    mode);

/**
 * Get the display name for a UCollator.
 * The display name is suitable for presentation to a user.
 * @param objLoc The locale of the collator in question.
 * @param dispLoc The locale for display.
 * @param result A pointer to a buffer to receive the attribute.
 * @param resultLength The maximum size of result.
 * @param status A pointer to an UErrorCode to receive any errors
 * @return The total buffer size needed; if greater than resultLength,
 * the output was truncated.
 * @stable
 */
U_CAPI int32_t
ucol_getDisplayName(    const    char        *objLoc,
            const    char        *dispLoc,
            UChar             *result,
            int32_t         resultLength,
            UErrorCode        *status);

/**
 * Get a locale for which collation rules are available.
 * A UCollator in a locale returned by this function will perform the correct
 * collation for the locale.
 * @param index The index of the desired locale.
 * @return A locale for which collation rules are available, or 0 if none.
 * @see ucol_countAvailable
 * @stable
 */
U_CAPI const char*
ucol_getAvailable(int32_t index);

/**
 * Determine how many locales have collation rules available.
 * This function is most useful as determining the loop ending condition for
 * calls to \Ref{ucol_getAvailable}.
 * @return The number of locales for which collation rules are available.
 * @see ucol_getAvailable
 * @stable
 */
U_CAPI int32_t
ucol_countAvailable(void);

/**
 * Get the collation rules from a UCollator.
 * The rules will follow the rule syntax.
 * @param coll The UCollatorOld to query.
 * @param length 
 * @return The collation rules.
 * @stable
 */
U_CAPI const UChar*
ucol_getRules(    const    UCollator    *coll, 
        int32_t            *length);

/**
 * Get a sort key for a string from a UCollator.
 * Sort keys may be compared using <TT>strcmp</TT>.
 * @param coll The UCollatorOld containing the collation rules.
 * @param source The string to transform.
 * @param sourecLength The length of source, or -1 if null-terminated.
 * @param result A pointer to a buffer to receive the attribute.
 * @param resultLength The maximum size of result.
 * @return The size needed to fully store the sort key..
 * @see ucol_keyHashCode
 * @stable
 */
U_CAPI int32_t
ucol_getSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength);



/**
 * Gets the version information for a Collator. 
 * @param info the version # information, the result will be filled in
 * @stable
 */
U_CAPI void U_EXPORT2
ucol_getVersion(const UCollator* coll, UVersionInfo info);


/* Following are the new APIs for 1.7. They are all draft and most are not even implemented */

/**
 * Universal attribute setter
 * @param coll collator which attributes are to be changed
 * @param attr attribute type 
 * @param value attribute value
 * @param status to indicate whether the operation went on smoothly or there were errors
 * @draft API 1.7 freeze
 */
U_CAPI void ucol_setAttribute(UCollator *coll, UColAttribute attr, UColAttributeValue value, UErrorCode *status);

/**
 * Universal attribute getter
 * @param coll collator which attributes are to be changed
 * @param attr attribute type
 * @return attribute value
 * @param status to indicate whether the operation went on smoothly or there were errors
 * @draft API 1.7 freeze
 */
U_CAPI UColAttributeValue ucol_getAttribute(const UCollator *coll, UColAttribute attr, UErrorCode *status);

/**
 * Thread safe cloning operation
 * @param coll collator to be cloned
 * @param stackBuffer user allocated space for the new clone. If NULL new memory will be allocated. 
	If buffer is not lareg enough, new memory will be allocated.
	Clients can use the U_COL_SAFECLONE_BUFFERSIZE. 
	This will probably be enough to avoid memory allocations.
 * @param pBufferSize pointer to size of allocated space. 
	If *pBufferSize == 0, a sufficient size for use in cloning will 
	be returned ('pre-flighting')
	If *pBufferSize is not enough for a stack-based safe clone, 
	new memory will be allocated.
 * @param status to indicate whether the operation went on smoothly or there were errors
    An informational status value, U_SAFECLONE_ALLOCATED_ERROR, is used if any allocations were necessary.
 * @return pointer to the new clone
 * @draft API 1.8 freeze
 */


U_CAPI UCollator * ucol_safeClone(
          const UCollator     *coll,
          void                *stackBuffer,
          int32_t        *pBufferSize,
          UErrorCode          *status);

#define U_COL_SAFECLONE_BUFFERSIZE 256

/* declaration for forward iterating function */
typedef UChar UCharForwardIterator(void *context);

/**
 * String compare that uses user supplied character iteration.
 * The idea is to prevent users from having to convert the whole string into UChar's before comparing
 * since sometimes strings differ on first couple of characters.
 * @param coll collator to be used for comparing
 * @param source pointer to function for iterating over the first string
 * @param sourceContext data to be passed to the first iterating function.
 * @param target pointer to function for iterating over the second string
 * @param targetContext data to be passed to the second iterating function.
 * @return The result of comparing the strings; one of UCOL_EQUAL,
 * UCOL_GREATER, UCOL_LESS
 */
U_CAPI UCollationResult ucol_strcollinc(const UCollator *coll, 
								 UCharForwardIterator *source, void *sourceContext,
								 UCharForwardIterator *target, void *targetContext);

/**
 * Returns current rules. Delta defines whether full rules are returned or just the tailoring. 
 * Returns number of UChars needed to store rules. If buffer is NULL or bufferLen is not enough 
 * to store rules, will store up to available space.
 * @param coll collator to get the rules from
 * @param delta one of 	UCOL_TAILORING_ONLY, UCOL_FULL_RULES. 
 * @param buffer buffer to store the result in. If NULL, you'll get no rules.
 * @param bufferLen lenght of buffer to store rules in. If less then needed you'll get only the part that fits in.
 */
U_CAPI int32_t ucol_getRulesEx(const UCollator *coll, UColRuleOption delta, UChar *buffer, int32_t bufferLen);

/* This is the C API wrapper for CollationIterator that got booted out from here, including just for */
/* include backward compatibility */
#include "unicode/ucoleitr.h"

#endif
