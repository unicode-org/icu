/*
*******************************************************************************
* Copyright (c) 1996-2003, International Business Machines Corporation and others.
* All Rights Reserved.
*******************************************************************************
*/

#ifndef UCOL_H
#define UCOL_H

#include "unicode/utypes.h"

#if !UCONFIG_NO_COLLATION

#include "unicode/unorm.h"
#include "unicode/parseerr.h"
#include "unicode/uloc.h"
#include "unicode/uset.h"

/**
 * \file
 * \brief C API: Collator 
 *
 * <h2> Collator C API </h2>
 *
 * The C API for Collator performs locale-sensitive
 * string comparison. You use this service to build
 * searching and sorting routines for natural language text.
 * <em>Important: </em>The ICU collation service has been reimplemented 
 * in order to achieve better performance and UCA compliance. 
 * For details, see the 
 * <a href="http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/collation/ICU_collation_design.htm">
 * collation design document</a>.
 * <p>
 * For more information about the collation service see 
 * <a href="http://oss.software.ibm.com/icu/userguide/Collate_Intro.html">the users guide</a>.
 * <p>
 * Collation service provides correct sorting orders for most locales supported in ICU. 
 * If specific data for a locale is not available, the orders eventually falls back
 * to the <a href="http://www.unicode.org/unicode/reports/tr10/">UCA sort order</a>. 
 * <p>
 * Sort ordering may be customized by providing your own set of rules. For more on
 * this subject see the 
 * <a href="http://oss.software.ibm.com/icu/userguide/Collate_Customization.html">
 * Collation customization</a> section of the users guide.
 * <p>
 * @see         UCollationResult
 * @see         UNormalizationMode
 * @see         UCollationStrength
 * @see         UCollationElements
 */

/** A collation element iterator.
*  For usage in C programs.
*/
struct collIterate;
/** structure representing collation element iterator instance 
 * @stable ICU 2.0
 */
typedef struct collIterate collIterate;

/** A collator.
*  For usage in C programs.
*/
struct UCollator;
/** structure representing a collator object instance 
 * @stable ICU 2.0
 */
typedef struct UCollator UCollator;


/**
 * UCOL_LESS is returned if source string is compared to be less than target
 * string in the u_strcoll() method.
 * UCOL_EQUAL is returned if source string is compared to be equal to target
 * string in the u_strcoll() method.
 * UCOL_GREATER is returned if source string is compared to be greater than
 * target string in the u_strcoll() method.
 * @see u_strcoll()
 * <p>
 * Possible values for a comparison result 
 * @stable ICU 2.0
 */
typedef enum {
  /** string a == string b */
  UCOL_EQUAL    = 0,
  /** string a > string b */
  UCOL_GREATER    = 1,
  /** string a < string b */
  UCOL_LESS    = -1
} UCollationResult ;


/** Enum containing attribute values for controling collation behavior.
 * Here are all the allowable values. Not every attribute can take every value. The only
 * universal value is UCOL_DEFAULT, which resets the attribute value to the predefined  
 * value for that locale 
 * @stable ICU 2.0
 */
typedef enum {
  /** accepted by most attributes */
  UCOL_DEFAULT = -1,

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

  /** Turn the feature off - works for UCOL_FRENCH_COLLATION, 
      UCOL_CASE_LEVEL, UCOL_HIRAGANA_QUATERNARY_MODE
      & UCOL_DECOMPOSITION_MODE*/
  UCOL_OFF = 16,
  /** Turn the feature on - works for UCOL_FRENCH_COLLATION, 
      UCOL_CASE_LEVEL, UCOL_HIRAGANA_QUATERNARY_MODE
      & UCOL_DECOMPOSITION_MODE*/
  UCOL_ON = 17,
  
  /** Valid for UCOL_ALTERNATE_HANDLING. Alternate handling will be shifted */
  UCOL_SHIFTED = 20,
  /** Valid for UCOL_ALTERNATE_HANDLING. Alternate handling will be non ignorable */
  UCOL_NON_IGNORABLE = 21,

  /** Valid for UCOL_CASE_FIRST - 
      lower case sorts before upper case */
  UCOL_LOWER_FIRST = 24,
  /** upper case sorts before lower case */
  UCOL_UPPER_FIRST = 25,

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
 * generated from UCollator objects
 * These values can be now found in the UColAttributeValue enum.
 * @stable ICU 2.0
 **/
typedef UColAttributeValue UCollationStrength;

/** Attributes that collation service understands. All the attributes can take UCOL_DEFAULT
 * value, as well as the values specific to each one. 
 * @stable ICU 2.0
 */
typedef enum {
     /** Attribute for direction of secondary weights - used in French.\ 
      * Acceptable values are UCOL_ON, which results in secondary weights
      * being considered backwards and UCOL_OFF which treats secondary
      * weights in the order they appear.*/
     UCOL_FRENCH_COLLATION, 
     /** Attribute for handling variable elements.\ 
      * Acceptable values are UCOL_NON_IGNORABLE (default)
      * which treats all the codepoints with non-ignorable 
      * primary weights in the same way,
      * and UCOL_SHIFTED which causes codepoints with primary 
      * weights that are equal or below the variable top value
      * to be ignored on primary level and moved to the quaternary 
      * level.*/
     UCOL_ALTERNATE_HANDLING, 
     /** Controls the ordering of upper and lower case letters.\ 
      * Acceptable values are UCOL_OFF (default), which orders
      * upper and lower case letters in accordance to their tertiary
      * weights, UCOL_UPPER_FIRST which forces upper case letters to 
      * sort before lower case letters, and UCOL_LOWER_FIRST which does 
      * the opposite. */
     UCOL_CASE_FIRST, 
     /** Controls whether an extra case level (positioned before the third
      * level) is generated or not.\ Acceptable values are UCOL_OFF (default), 
      * when case level is not generated, and UCOL_ON which causes the case
      * level to be generated.\ Contents of the case level are affected by
      * the value of UCOL_CASE_FIRST attribute.\ A simple way to ignore 
      * accent differences in a string is to set the strength to UCOL_PRIMARY
      * and enable case level. */
     UCOL_CASE_LEVEL,
     /** Controls whether the normalization check and necessary normalizations
      * are performed.\ When set to UCOL_OFF (default) no normalization check
      * is performed.\ The correctness of the result is guaranteed only if the 
      * input data is in so-called FCD form (see users manual for more info).\ 
      * When set to UCOL_ON, an incremental check is performed to see whether the input data
      * is in the FCD form.\ If the data is not in the FCD form, incremental 
      * NFD normalization is performed. */
     UCOL_NORMALIZATION_MODE, 
     /** An alias for UCOL_NORMALIZATION_MODE attribute */
     UCOL_DECOMPOSITION_MODE = UCOL_NORMALIZATION_MODE,
     /** The strength attribute.\ Can be either UCOL_PRIMARY, UCOL_SECONDARY,
      * UCOL_TERTIARY, UCOL_QUATERNARY or UCOL_IDENTICAL.\ The usual strength
      * for most locales (except Japanese) is tertiary.\ Quaternary strength 
      * is useful when combined with shifted setting for alternate handling
      * attribute and for JIS x 4061 collation, when it is used to distinguish
      * between Katakana  and Hiragana (this is achieved by setting the 
      * UCOL_HIRAGANA_QUATERNARY mode to on.\ Otherwise, quaternary level
      * is affected only by the number of non ignorable code points in
      * the string.\ Identical strength is rarely useful, as it amounts 
      * to codepoints of the NFD form of the string. */
     UCOL_STRENGTH,  
     /** when turned on, this attribute 
      * positions Hiragana before all  
      * non-ignorables on quaternary level
      * This is a sneaky way to produce JIS
      * sort order */     
     UCOL_HIRAGANA_QUATERNARY_MODE, 
     /** when turned on, this attribute 
      * generates a collation key
      * for the numeric value of substrings
      * of digits. This is a way to get '100' 
      * to sort AFTER '2'.*/          
     UCOL_NUMERIC_COLLATION, 
     UCOL_ATTRIBUTE_COUNT
} UColAttribute;

/** Options for retrieving the rule string 
 *  @stable ICU 2.0
 */
typedef enum {
  /** Retrieve tailoring only */
  UCOL_TAILORING_ONLY, 
  /** Retrieve UCA rules and tailoring */
  UCOL_FULL_RULES 
} UColRuleOption ;

/**
 * Open a UCollator for comparing strings.
 * The UCollator pointer is used in all the calls to the Collation 
 * service. After finished, collator must be disposed of by calling
 * \Ref{ucol_close}.
 * @param loc The locale containing the required collation rules. 
 *            Special values for locales can be passed in - 
 *            if NULL is passed for the locale, the default locale
 *            collation rules will be used. If empty string ("") or
 *            "root" are passed, UCA rules will be used.
 * @param status A pointer to an UErrorCode to receive any errors
 * @return A pointer to a UCollator, or 0 if an error occurred.
 * @see ucol_openRules
 * @see ucol_safeClone
 * @see ucol_close
 * @stable ICU 2.0
 */
U_CAPI UCollator* U_EXPORT2 
ucol_open(const char *loc, UErrorCode *status);

/**
 * Produce an UCollator instance according to the rules supplied.
 * The rules are used to change the default ordering, defined in the
 * UCA in a process called tailoring. The resulting UCollator pointer
 * can be used in the same way as the one obtained by \Ref{ucol_strcoll}.
 * @param rules A string describing the collation rules. For the syntax
 *              of the rules please see users guide.
 * @param rulesLength The length of rules, or -1 if null-terminated.
 * @param normalizationMode The normalization mode: One of
 *             UCOL_OFF     (expect the text to not need normalization),
 *             UCOL_ON      (normalize), or
 *             UCOL_DEFAULT (set the mode according to the rules)
 * @param strength The default collation strength; one of UCOL_PRIMARY, UCOL_SECONDARY,
 * UCOL_TERTIARY, UCOL_IDENTICAL,UCOL_DEFAULT_STRENGTH - can be also set in the rules.
 * @param parseError  A pointer to UParseError to recieve information about errors
 *                    occurred during parsing. This argument can currently be set
 *                    to NULL, but at users own risk. Please provide a real structure.
 * @param status A pointer to an UErrorCode to receive any errors
 * @return A pointer to a UCollator.\ It is not guaranteed that NULL be returned in case
 *         of error - please use status argument to check for errors.
 * @see ucol_open
 * @see ucol_safeClone
 * @see ucol_close
 * @stable ICU 2.0
 */
U_CAPI UCollator* U_EXPORT2 
ucol_openRules( const UChar        *rules,
                int32_t            rulesLength,
                UColAttributeValue normalizationMode,
                UCollationStrength strength,
                UParseError        *parseError,
                UErrorCode         *status);

/** 
 * Close a UCollator.
 * Once closed, a UCollator should not be used.\ Every open collator should
 * be closed.\ Otherwise, a memory leak will result.
 * @param coll The UCollator to close.
 * @see ucol_open
 * @see ucol_openRules
 * @see ucol_safeClone
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 
ucol_close(UCollator *coll);

/**
 * Compare two strings.
 * The strings will be compared using the options already specified.
 * @param coll The UCollator containing the comparison rules.
 * @param source The source string.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param target The target string.
 * @param targetLength The length of target, or -1 if null-terminated.
 * @return The result of comparing the strings; one of UCOL_EQUAL,
 * UCOL_GREATER, UCOL_LESS
 * @see ucol_greater
 * @see ucol_greaterOrEqual
 * @see ucol_equal
 * @stable ICU 2.0
 */
U_CAPI UCollationResult U_EXPORT2 
ucol_strcoll(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength);

/**
 * Determine if one string is greater than another.
 * This function is equivalent to \Ref{ucol_strcoll} == UCOL_GREATER
 * @param coll The UCollator containing the comparison rules.
 * @param source The source string.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param target The target string.
 * @param targetLength The length of target, or -1 if null-terminated.
 * @return TRUE if source is greater than target, FALSE otherwise.
 * @see ucol_strcoll
 * @see ucol_greaterOrEqual
 * @see ucol_equal
 * @stable ICU 2.0
 */
U_CAPI UBool U_EXPORT2 
ucol_greater(const UCollator *coll,
             const UChar     *source, int32_t sourceLength,
             const UChar     *target, int32_t targetLength);

/**
 * Determine if one string is greater than or equal to another.
 * This function is equivalent to \Ref{ucol_strcoll} != UCOL_LESS
 * @param coll The UCollator containing the comparison rules.
 * @param source The source string.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param target The target string.
 * @param targetLength The length of target, or -1 if null-terminated.
 * @return TRUE if source is greater than or equal to target, FALSE otherwise.
 * @see ucol_strcoll
 * @see ucol_greater
 * @see ucol_equal
 * @stable ICU 2.0
 */
U_CAPI UBool U_EXPORT2 
ucol_greaterOrEqual(const UCollator *coll,
                    const UChar     *source, int32_t sourceLength,
                    const UChar     *target, int32_t targetLength);

/**
 * Compare two strings for equality.
 * This function is equivalent to \Ref{ucol_strcoll} == UCOL_EQUAL
 * @param coll The UCollator containing the comparison rules.
 * @param source The source string.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param target The target string.
 * @param targetLength The length of target, or -1 if null-terminated.
 * @return TRUE if source is equal to target, FALSE otherwise
 * @see ucol_strcoll
 * @see ucol_greater
 * @see ucol_greaterOrEqual
 * @stable ICU 2.0
 */
U_CAPI UBool U_EXPORT2 
ucol_equal(const UCollator *coll,
           const UChar     *source, int32_t sourceLength,
           const UChar     *target, int32_t targetLength);

/**
 * Compare two UTF-8 encoded trings.
 * The strings will be compared using the options already specified.
 * @param coll The UCollator containing the comparison rules.
 * @param sIter The source string iterator.
 * @param tIter The target string iterator.
 * @return The result of comparing the strings; one of UCOL_EQUAL,
 * UCOL_GREATER, UCOL_LESS
 * @param status A pointer to an UErrorCode to receive any errors
 * @see ucol_strcoll
 * @draft ICU 2.6
 */
U_CAPI UCollationResult U_EXPORT2 
ucol_strcollIter(  const    UCollator    *coll,
                  UCharIterator *sIter,
                  UCharIterator *tIter,
                  UErrorCode *status);

/**
 * Get the collation strength used in a UCollator.
 * The strength influences how strings are compared.
 * @param coll The UCollator to query.
 * @return The collation strength; one of UCOL_PRIMARY, UCOL_SECONDARY,
 * UCOL_TERTIARY, UCOL_QUATERNARY, UCOL_IDENTICAL
 * @see ucol_setStrength
 * @stable ICU 2.0
 */
U_CAPI UCollationStrength U_EXPORT2 
ucol_getStrength(const UCollator *coll);

/**
 * Set the collation strength used in a UCollator.
 * The strength influences how strings are compared.
 * @param coll The UCollator to set.
 * @param strength The desired collation strength; one of UCOL_PRIMARY, 
 * UCOL_SECONDARY, UCOL_TERTIARY, UCOL_QUATERNARY, UCOL_IDENTICAL, UCOL_DEFAULT
 * @see ucol_getStrength
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 
ucol_setStrength(UCollator *coll,
                 UCollationStrength strength);

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
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2 
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
 * @stable ICU 2.0
 */
U_CAPI const char* U_EXPORT2 
ucol_getAvailable(int32_t index);

/**
 * Determine how many locales have collation rules available.
 * This function is most useful as determining the loop ending condition for
 * calls to \Ref{ucol_getAvailable}.
 * @return The number of locales for which collation rules are available.
 * @see ucol_getAvailable
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2 
ucol_countAvailable(void);

/**
 * Get the collation rules from a UCollator.
 * The rules will follow the rule syntax.
 * @param coll The UCollator to query.
 * @param length 
 * @return The collation rules.
 * @stable ICU 2.0
 */
U_CAPI const UChar* U_EXPORT2 
ucol_getRules(    const    UCollator    *coll, 
        int32_t            *length);

/**
 * Get a sort key for a string from a UCollator.
 * Sort keys may be compared using <TT>strcmp</TT>.
 * @param coll The UCollator containing the collation rules.
 * @param source The string to transform.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param result A pointer to a buffer to receive the attribute.
 * @param resultLength The maximum size of result.
 * @return The size needed to fully store the sort key..
 * @see ucol_keyHashCode
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2 
ucol_getSortKey(const    UCollator    *coll,
        const    UChar        *source,
        int32_t        sourceLength,
        uint8_t        *result,
        int32_t        resultLength);


/** Gets the next count bytes of a sort key. Caller needs
 *  to preserve state array between calls and to provide
 *  the same type of UCharIterator set with the same string.
 *  The destination buffer provided must be big enough to store
 *  the number of requested bytes. Generated sortkey is not 
 *  compatible with sortkeys generated using ucol_getSortKey
 *  API, since we don't do any compression. If uncompressed
 *  sortkeys are required, this API can be used.
 *  @param coll The UCollator containing the collation rules.
 *  @param iter UCharIterator containing the string we need 
 *              the sort key to be calculated for.
 *  @param state Opaque state of sortkey iteration.
 *  @param dest Buffer to hold the resulting sortkey part
 *  @param count number of sort key bytes required.
 *  @param status error code indicator.
 *  @return the actual number of bytes of a sortkey. It can be
 *          smaller than count if we have reached the end of 
 *          the sort key.
 *  @draft ICU 2.6
 */
U_CAPI int32_t U_EXPORT2 
ucol_nextSortKeyPart(const UCollator *coll,
                     UCharIterator *iter,
                     uint32_t state[2],
                     uint8_t *dest, int32_t count,
                     UErrorCode *status);

/** enum that is taken by ucol_getBound API 
 * See below for explanation                
 * do not change the values assigned to the 
 * members of this enum. Underlying code    
 * depends on them having these numbers     
 * @stable ICU 2.0
 */
typedef enum {
  /** lower bound */
  UCOL_BOUND_LOWER = 0,
  /** upper bound that will match strings of exact size */
  UCOL_BOUND_UPPER = 1,
  /** upper bound that will match all the strings that have the same initial substring as the given string */
  UCOL_BOUND_UPPER_LONG = 2,
  UCOL_BOUND_VALUE_COUNT
} UColBoundMode;

/**
 * Produce a bound for a given sortkey and a number of levels.
 * Return value is always the number of bytes needed, regardless of 
 * whether the result buffer was big enough or even valid.<br>
 * Resulting bounds can be used to produce a range of strings that are
 * between upper and lower bounds. For example, if bounds are produced
 * for a sortkey of string "smith", strings between upper and lower 
 * bounds with one level would include "Smith", "SMITH", "sMiTh".<br>
 * There are two upper bounds that can be produced. If UCOL_BOUND_UPPER
 * is produced, strings matched would be as above. However, if bound
 * produced using UCOL_BOUND_UPPER_LONG is used, the above example will
 * also match "Smithsonian" and similar.<br>
 * For more on usage, see example in cintltst/capitst.c in procedure
 * TestBounds.
 * Sort keys may be compared using <TT>strcmp</TT>.
 * @param source The source sortkey.
 * @param sourceLength The length of source, or -1 if null-terminated. 
 *                     (If an unmodified sortkey is passed, it is always null 
 *                      terminated).
 * @param boundType Type of bound required. It can be UCOL_BOUND_LOWER, which 
 *                  produces a lower inclusive bound, UCOL_BOUND_UPPER, that 
 *                  produces upper bound that matches strings of the same length 
 *                  or UCOL_BOUND_UPPER_LONG that matches strings that have the 
 *                  same starting substring as the source string.
 * @param noOfLevels  Number of levels required in the resulting bound (for most 
 *                    uses, the recommended value is 1). See users guide for 
 *                    explanation on number of levels a sortkey can have.
 * @param result A pointer to a buffer to receive the resulting sortkey.
 * @param resultLength The maximum size of result.
 * @param status Used for returning error code if something went wrong. If the 
 *               number of levels requested is higher than the number of levels
 *               in the source key, a warning (U_SORT_KEY_TOO_SHORT_WARNING) is 
 *               issued.
 * @return The size needed to fully store the bound. 
 * @see ucol_keyHashCode
 * @stable ICU 2.1
 */
U_CAPI int32_t U_EXPORT2 
ucol_getBound(const uint8_t       *source,
        int32_t             sourceLength,
        UColBoundMode       boundType,
        uint32_t            noOfLevels,
        uint8_t             *result,
        int32_t             resultLength,
        UErrorCode          *status);
        
/**
 * Gets the version information for a Collator. Version is currently
 * an opaque 32-bit number which depends, among other things, on major
 * versions of the collator tailoring and UCA.
 * @param coll The UCollator to query.
 * @param info the version # information, the result will be filled in
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2
ucol_getVersion(const UCollator* coll, UVersionInfo info);

/**
 * Gets the UCA version information for a Collator. Version is the
 * UCA version number (3.1.1, 4.0).
 * @param coll The UCollator to query.
 * @param info the version # information, the result will be filled in
 * @draft ICU 2.8
 */
U_CAPI void U_EXPORT2
ucol_getUCAVersion(const UCollator* coll, UVersionInfo info);

/** 
 * Merge two sort keys. The levels are merged with their corresponding counterparts
 * (primaries with primaries, secondaries with secondaries etc.). Between the values
 * from the same level a separator is inserted.
 * example (uncompressed): 
 * 191B1D 01 050505 01 910505 00 and 1F2123 01 050505 01 910505 00
 * will be merged as 
 * 191B1D 02 1F212301 050505 02 050505 01 910505 02 910505 00
 * This allows for concatenating of first and last names for sorting, among other things.
 * If the destination buffer is not big enough, the results are undefined.
 * If any of source lengths are zero or any of source pointers are NULL/undefined, 
 * result is of size zero.
 * @param src1 pointer to the first sortkey
 * @param src1Length length of the first sortkey
 * @param src2 pointer to the second sortkey
 * @param src2Length length of the second sortkey
 * @param dest buffer to hold the result
 * @param destCapacity size of the buffer for the result
 * @return size of the result. If the buffer is big enough size is always
 *         src1Length+src2Length-1
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2 
ucol_mergeSortkeys(const uint8_t *src1, int32_t src1Length,
                   const uint8_t *src2, int32_t src2Length,
                   uint8_t *dest, int32_t destCapacity);

/**
 * Universal attribute setter
 * @param coll collator which attributes are to be changed
 * @param attr attribute type 
 * @param value attribute value
 * @param status to indicate whether the operation went on smoothly or there were errors
 * @see UColAttribute
 * @see UColAttributeValue
 * @see ucol_getAttribute
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 
ucol_setAttribute(UCollator *coll, UColAttribute attr, UColAttributeValue value, UErrorCode *status);

/**
 * Universal attribute getter
 * @param coll collator which attributes are to be changed
 * @param attr attribute type
 * @return attribute value
 * @param status to indicate whether the operation went on smoothly or there were errors
 * @see UColAttribute
 * @see UColAttributeValue
 * @see ucol_setAttribute
 * @stable ICU 2.0
 */
U_CAPI UColAttributeValue  U_EXPORT2 
ucol_getAttribute(const UCollator *coll, UColAttribute attr, UErrorCode *status);

/** Variable top
 * is a two byte primary value which causes all the codepoints with primary values that
 * are less or equal than the variable top to be shifted when alternate handling is set
 * to UCOL_SHIFTED.
 * Sets the variable top to a collation element value of a string supplied. 
 * @param coll collator which variable top needs to be changed
 * @param varTop one or more (if contraction) UChars to which the variable top should be set
 * @param len length of variable top string. If -1 it is considered to be zero terminated.
 * @param status error code. If error code is set, the return value is undefined. 
 *               Errors set by this function are: <br>
 *    U_CE_NOT_FOUND_ERROR if more than one character was passed and there is no such 
 *    a contraction<br>
 *    U_PRIMARY_TOO_LONG_ERROR if the primary for the variable top has more than two bytes
 * @return a 32 bit value containing the value of the variable top in upper 16 bits. 
 *         Lower 16 bits are undefined
 * @see ucol_getVariableTop
 * @see ucol_restoreVariableTop
 * @stable ICU 2.0
 */
U_CAPI uint32_t U_EXPORT2 
ucol_setVariableTop(UCollator *coll, 
                    const UChar *varTop, int32_t len, 
                    UErrorCode *status);

/** 
 * Gets the variable top value of a Collator. 
 * Lower 16 bits are undefined and should be ignored.
 * @param coll collator which variable top needs to be retrieved
 * @param status error code (not changed by function). If error code is set, 
 *               the return value is undefined.
 * @return the variable top value of a Collator.
 * @see ucol_setVariableTop
 * @see ucol_restoreVariableTop
 * @stable ICU 2.0
 */
U_CAPI uint32_t U_EXPORT2 ucol_getVariableTop(const UCollator *coll, UErrorCode *status);

/** 
 * Sets the variable top to a collation element value supplied. Variable top is 
 * set to the upper 16 bits. 
 * Lower 16 bits are ignored.
 * @param coll collator which variable top needs to be changed
 * @param varTop CE value, as returned by ucol_setVariableTop or ucol)getVariableTop
 * @param status error code (not changed by function)
 * @see ucol_getVariableTop
 * @see ucol_setVariableTop
 * @stable ICU 2.0
 */
U_CAPI void U_EXPORT2 
ucol_restoreVariableTop(UCollator *coll, const uint32_t varTop, UErrorCode *status);

/**
 * Thread safe cloning operation. The result is a clone of a given collator.
 * @param coll collator to be cloned
 * @param stackBuffer user allocated space for the new clone. 
 * If NULL new memory will be allocated. 
 *  If buffer is not large enough, new memory will be allocated.
 *  Clients can use the U_COL_SAFECLONE_BUFFERSIZE. 
 *  This will probably be enough to avoid memory allocations.
 * @param pBufferSize pointer to size of allocated space. 
 *  If *pBufferSize == 0, a sufficient size for use in cloning will 
 *  be returned ('pre-flighting')
 *  If *pBufferSize is not enough for a stack-based safe clone, 
 *  new memory will be allocated.
 * @param status to indicate whether the operation went on smoothly or there were errors
 *    An informational status value, U_SAFECLONE_ALLOCATED_ERROR, is used if any
 * allocations were necessary.
 * @return pointer to the new clone
 * @see ucol_open
 * @see ucol_openRules
 * @see ucol_close
 * @stable ICU 2.0
 */
U_CAPI UCollator* U_EXPORT2 
ucol_safeClone(const UCollator *coll,
               void            *stackBuffer,
               int32_t         *pBufferSize,
               UErrorCode      *status);

/** default memory size for the new clone. It needs to be this large for os/400 large pointers 
 * @stable ICU 2.0
 */
#define U_COL_SAFECLONE_BUFFERSIZE 512

/**
 * Returns current rules. Delta defines whether full rules are returned or just the tailoring. 
 * Returns number of UChars needed to store rules. If buffer is NULL or bufferLen is not enough 
 * to store rules, will store up to available space.
 * @param coll collator to get the rules from
 * @param delta one of UCOL_TAILORING_ONLY, UCOL_FULL_RULES. 
 * @param buffer buffer to store the result in. If NULL, you'll get no rules.
 * @param bufferLen lenght of buffer to store rules in. If less then needed you'll get only the part that fits in.
 * @return current rules
 * @stable ICU 2.0
 */
U_CAPI int32_t U_EXPORT2 
ucol_getRulesEx(const UCollator *coll, UColRuleOption delta, UChar *buffer, int32_t bufferLen);

/**
 * gets the locale name of the collator. If the collator
 * is instantiated from the rules, then this function returns
 * NULL.
 * @param coll The UCollator for which the locale is needed
 * @param type You can choose between requested, valid and actual
 *             locale. For description see the definition of
 *             ULocDataLocaleType in uloc.h
 * @param status error code of the operation
 * @return real locale name from which the collation data comes. 
 *         If the collator was instantiated from rules, returns
 *         NULL.
 * @stable ICU 2.1
 */
U_CAPI const char * U_EXPORT2
ucol_getLocale(const UCollator *coll, ULocDataLocaleType type, UErrorCode *status);


/**
 * Get an Unicode set that contains all the characters and sequences tailored in 
 * this collator. The result must be disposed of by using uset_close.
 * @param coll        The UCollator for which we want to get tailored chars
 * @param status      error code of the operation
 * @return a pointer to newly created USet. Must be be disposed by using uset_close
 * @see ucol_openRules
 * @see uset_close
 * @draft ICU 2.4
 */
U_CAPI USet * U_EXPORT2
ucol_getTailoredSet(const UCollator *coll, UErrorCode *status);

#endif /* #if !UCONFIG_NO_COLLATION */

#endif
