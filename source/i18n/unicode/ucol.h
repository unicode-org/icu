/*
*******************************************************************************
* Copyright © {1996-1999}, International Business Machines Corporation and others. All Rights Reserved.
*******************************************************************************
*/

#ifndef UCOL_H
#define UCOL_H

#include "unicode/utypes.h"
/**
 * @name Collator C API
 *
 * The C API for Collator performs locale-sensitive
 * <code>String</code> comparison. You use this class to build
 * searching and sorting routines for natural language text.
 *
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
 * </pre>
 * </blockquote>
 * <p>
 * For comparing <code>String</code>s exactly once, the <code>u_strcoll</code>
 * method provides the best performance. When sorting a list of
 * <code>String</code>s however, it is generally necessary to compare each
 * <code>String</code> multiple times. In this case, <code>sortKey</code>s
 * provide better performance. The <code>ucol_getsortKey</code> method converts
 * a <code>String</code> to a series of bits that can be compared bitwise
 * against other <code>sortKey</code>s using <code>memcmp()</code> 
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

 /** A collator.
 *  For usage in C programs.
 */
typedef void* UCollator;

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
enum UCollationResult {
  /** string a == string b */
  UCOL_EQUAL    = 0,
  /** string a > string b */
  UCOL_GREATER    = 1,
  /** string a < string b */
  UCOL_LESS    = -1
};
typedef enum UCollationResult UCollationResult;
  /**
    * UCOL_NO_NORMALIZATION : Accented characters will not be decomposed for sorting.  
    * UCOL_DECOM_CAN          : Characters that are canonical variants according 
    * to Unicode 2.0 will be decomposed for sorting. 
    * UCOL_DECOMP_COMPAT    : Characters that are compatibility variants will be
    * decomposed for sorting. This is the default normalization mode used.
    * UCOL_DECOMP_CAN_COMP_COMPAT : Canonical decomposition followed by canonical composition 
    * UCOL_DECOMP_COMPAT_COMP_CAN : Compatibility decomposition followed by canonical composition
    *
    **/
/** Possible collation normalization modes */
enum UNormalizationMode {
  /** No decomposition/composition */
  UCOL_NO_NORMALIZATION,
  /** Canonical decomposition */
  UCOL_DECOMP_CAN,
  /** Compatibility decomposition */
  UCOL_DECOMP_COMPAT,
  /** Canonical decomposition followed by canonical composition */
  UCOL_DECOMP_CAN_COMP_COMPAT,
  /** Compatibility decomposition followed by canonical composition */
  UCOL_DECOMP_COMPAT_COMP_CAN,
  /** Default normalization */
  UCOL_DEFAULT_NORMALIZATION = UCOL_DECOMP_COMPAT
};
typedef enum UNormalizationMode UNormalizationMode;

/** Possible normalization options */
enum UNormalizationOption {
  /** Do not normalize Hangul */
  UCOL_IGNORE_HANGUL    = 1
};
typedef enum UNormalizationOption UNormalizationOption;
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
     **/
/** Possible collation strengths */
enum UCollationStrength {
  /** Primary collation strength */
  UCOL_PRIMARY = 0,
  /** Secondary collation strength */
  UCOL_SECONDARY = 1,
  /** Tertiary collation strength */
  UCOL_TERTIARY = 2,
  /** Identical collation strength */
  UCOL_IDENTICAL = 3,
  /** Default collation strength */
  UCOL_DEFAULT_STRENGTH = UCOL_TERTIARY
} ;
typedef enum UCollationStrength UCollationStrength;

/*===============================================
=================================================
    ---> MOVE SOMEWHERE ELSE !!! <---
=================================================
===============================================*/
/**
 * @name Unicode normalization API
 *
 * <tt>u_normalize</tt> transforms Unicode text into an equivalent composed or
 * decomposed form, allowing for easier sorting and searching of text.
 * <tt>u_normalize</tt> supports the standard normalization forms described in
 * <a href="http://www.unicode.org/unicode/reports/tr15/" target="unicode">
 * Unicode Technical Report #15</a>.
 * <p>
 * Characters with accents or other adornments can be encoded in
 * several different ways in Unicode.  For example, take the character "Á"
 * (A-acute).   In Unicode, this can be encoded as a single character (the
 * "composed" form):
 * <pre>
 *      00C1    LATIN CAPITAL LETTER A WITH ACUTE</pre>
 * or as two separate characters (the "decomposed" form):
 * <pre>
 *      0041    LATIN CAPITAL LETTER A
 *      0301    COMBINING ACUTE ACCENT</pre>
 * <p>
 * To a user of your program, however, both of these sequences should be
 * treated as the same "user-level" character "Á".  When you are searching or
 * comparing text, you must ensure that these two sequences are treated 
 * equivalently.  In addition, you must handle characters with more than one
 * accent.  Sometimes the order of a character's combining accents is
 * significant, while in other cases accent sequences in different orders are
 * really equivalent.
 * <p>
 * Similarly, the string "ffi" can be encoded as three separate letters:
 * <pre>
 *      0066    LATIN SMALL LETTER F
 *      0066    LATIN SMALL LETTER F
 *      0069    LATIN SMALL LETTER I</pre>
 * or as the single character
 * <pre>
 *      FB03    LATIN SMALL LIGATURE FFI</pre>
 * <p>
 * The ffi ligature is not a distinct semantic character, and strictly speaking
 * it shouldn't be in Unicode at all, but it was included for compatibility
 * with existing character sets that already provided it.  The Unicode standard
 * identifies such characters by giving them "compatibility" decompositions
 * into the corresponding semantic characters.  When sorting and searching, you
 * will often want to use these mappings.
 * <p>
 * <tt>u_normalize</tt> helps solve these problems by transforming text into the
 * canonical composed and decomposed forms as shown in the first example above.  
 * In addition, you can have it perform compatibility decompositions so that 
 * you can treat compatibility characters the same as their equivalents.
 * Finally, <tt>u_normalize</tt> rearranges accents into the proper canonical
 * order, so that you do not have to worry about accent rearrangement on your
 * own.
 * <p>
 * <tt>u_normalize</tt> adds one optional behavior, {@link #UCOL_IGNORE_HANGUL},
 * that differs from
 * the standard Unicode Normalization Forms. 
 **/
 
 
/**
 * Normalize a string.
 * The string will be normalized according the the specified normalization mode
 * and options.
 * @param source The string to normalize.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param mode The normalization mode; one of UCOL_NO_NORMALIZATION, 
 * UCOL_CAN_DECOMP, UCOL_COMPAT_DECOMP, UCOL_CAN_DECOMP_COMPAT_COMP, 
 * UCOL_COMPAT_DECOMP_CAN_COMP, UCOL_DEFAULT_NORMALIZATION
 * @param options The normalization options, ORed together; possible values
 * are UCOL_IGNORE_HANGUL
 * @param result A pointer to a buffer to receive the attribute.
 * @param resultLength The maximum size of result.
 * @param status A pointer to an UErrorCode to receive any errors
 * @return The total buffer size needed; if greater than resultLength,
 * the output was truncated.
 * @stable
 */
U_CAPI int32_t
u_normalize(const UChar*           source,
        int32_t                 sourceLength, 
        UNormalizationMode      mode, 
        int32_t            options,
        UChar*                  result,
        int32_t                 resultLength,
        UErrorCode*             status);    

/**
 * Open a UCollator for comparing strings.
 * The UCollator may be used in calls to \Ref{ucol_strcoll}.
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
 * Open a UCollator for comparing strings.
 * The UCollator may be used in calls to \Ref{ucol_strcoll}.
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
 * Once closed, a UCollator should not be used.
 * @param coll The UCollator to close.
 * @stable
 */
U_CAPI void
ucol_close(UCollator *coll);

/**
 * Compare two strings.
 * The strings will be compared using the normalization mode and options
 * specified in \Ref{ucol_open} or \Ref{ucol_openRules}
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
 * @param coll The UCollator containing the comparison rules.
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
U_CAPI bool_t
ucol_greater(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength);

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
 * @stable
 */
U_CAPI bool_t
ucol_greaterOrEqual(    const    UCollator    *coll,
            const    UChar        *source,
            int32_t            sourceLength,
            const    UChar        *target,
            int32_t            targetLength);

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
 * @stable
 */
U_CAPI bool_t
ucol_equal(    const    UCollator    *coll,
        const    UChar        *source,
        int32_t            sourceLength,
        const    UChar        *target,
        int32_t            targetLength);

/**
 * Get the collation strength used in a UCollator.
 * The strength influences how strings are compared.
 * @param coll The UCollator to query.
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
 * .       UCollator *myCollation = ucol_open("en_US", status);
 * .       if (U_FAILURE(&status)) return;
 * .       ucol_setStrength(myCollation, UCOL_PRIMARY);
 * .       u_uastrcpy(source, "abc");
 * .       u_uastrcpy(target, "ABC");
 * .       // result will be "abc" == "ABC"
 * .       // tertiary differences will be ignored
 * .       result = ucol_strcoll(myCollation, source, u_strlen(source), target, u_strlen(target));
 * </pre>
 * @param coll The UCollator to set.
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
 * @param coll The UCollator to query.
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
 * @param coll The UCollator to set.
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
 * @param coll The UCollator to query.
 * @param length 
 * @return The collation rules.
 * @stable
 */
U_CAPI const UChar*
ucol_getRules(    const    UCollator    *coll, 
        int32_t            *length);

/**
 * Get a sort key for a string from a UCollator.
 * Sort keys may be compared using <TT>memcmp</TT>.
 * @param coll The UCollator containing the collation rules.
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
        int32_t            sourceLength,
        uint8_t            *result,
        int32_t            resultLength);

/**
 * Generate a hash code for a collation key.
 * A hash code is a 32-bit value suitable for use as a key in a hashtable.
 * @param key The collation key.
 * @param keyLength The length of key.
 * @return A hash code for key.
 * @see ucol_getSortKey
 * @deprecated ?  why is hashCode useful for C users?
 */
U_CAPI int32_t
ucol_keyHashCode(    const    uint8_t*    key, 
            int32_t        length);


/** The UCollationElements struct.
 *  For usage in C programs.
 */
struct UCollationElements;
typedef struct UCollationElements UCollationElements;
/**
 * The UCollationElements  is used as an iterator to walk through
 * each character of an international string. Use the iterator to return the
 * ordering priority of the positioned character. The ordering priority of
 * a character, which we refer to as a key, defines how a character is
 * collated in the given collation object.
 * For example, consider the following in Spanish:
 * <pre>
 * .       "ca" -> the first key is key('c') and second key is key('a').
 * .       "cha" -> the first key is key('ch') and second key is key('a').
 * </pre>
 * And in German,
 * <pre>
 * .       "æb"-> the first key is key('a'), the second key is key('e'), and
 * .       the third key is key('b').
 * </pre>
 * The key of a character, is an const UCOL_PRIMARYMASK, UCOL_SECONDARY_MASK,
 * UCOL_TERTIARYMASK.    
 * <p>Example of the iterator usage: (without error checking)
 * <pre>
 * .  void CollationElementIterator_Example()
 * .  {
 * .      UChar *s;
 * .      t_int32 order, primaryOrder;
 * .      UCollationElements *c;
 * .      UCollator *coll;
 * .      UErrorCode success = U_ZERO_ERROR;
 * .      s=(UChar*)malloc(sizeof(UChar) * (strlen("This is a test")+1) );
 * .      u_uastrcpy(s, "This is a test");
 * .      coll = ucol_open(NULL, &success);
 * .      c = ucol_openElements(coll, str, u_strlen(str), &status);
 * .      order = ucol_next(c, &success);
 * .      primaryOrder = order & UCOL_PRIMARYMASK;
 * .      free(s);
 * .      ucol_close(coll);
 * .      ucol_closeElements(c);
 * .  }
 * </pre>
 * <p>
 * ucol_next() returns the collation order of the next
 * character based on the comparison level of the collator.  A collation order 
 * consists of primary order, secondary order and tertiary order.  The data 
 * type of the collation order is <strong>t_int32</strong>.  The first 16 bits of 
 * a collation order is its primary order; the next 8 bits is the secondary 
 * order and the last 8 bits is the tertiary order.
 *
 * @see                Collator
 */

/**
 * Open the collation elements for a string.
 *
 * @param coll The collator containing the desired collation rules.
 * @param text The text to iterate over.
 * @param textLength The number of characters in text, or -1 if null-terminated
 * @param status A pointer to an UErrorCode to receive any errors.
 * @stable
 */
U_CAPI UCollationElements*
ucol_openElements(    const    UCollator       *coll,
            const    UChar           *text,
            int32_t                  textLength,
            UErrorCode         *status);

/* Bit mask for primary collation strength. */
#define UCOL_PRIMARYMASK    0xFFFF0000

/* Bit mask for secondary collation strength. */
#define UCOL_SECONDARYMASK  0x0000FF00

/* Bit mask for tertiary collation strength. */
#define UCOL_TERTIARYMASK   0x000000FF

/** This indicates the last element in a UCollationElements has been consumed. 
 *
 */
#define UCOL_NULLORDER        0xFFFFFFFF

/**
 * Close a UCollationElements.
 * Once closed, a UCollationElements may no longer be used.
 * @param elems The UCollationElements to close.
 * @stable
 */
U_CAPI void
ucol_closeElements(UCollationElements *elems);

/**
 * Reset the collation elements to their initial state.
 * This will move the 'cursor' to the beginning of the text.
 * @param elems The UCollationElements to reset.
 * @see ucol_next
 * @see ucol_previous
 * @stable
 */
U_CAPI void
ucol_reset(UCollationElements *elems);

/**
 * Get the ordering priority of the next collation element in the text.
 * A single character may contain more than one collation element.
 * @param elems The UCollationElements containing the text.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The next collation elements ordering, or \Ref{UCOL_NULLORDER} if the
 * end of the text is reached.
 * @stable
 */
U_CAPI int32_t
ucol_next(    UCollationElements    *elems,
        UErrorCode        *status);

/**
 * Get the ordering priority of the previous collation element in the text.
 * A single character may contain more than one collation element.
 * @param elems The UCollationElements containing the text.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @return The previous collation elements ordering, or \Ref{UCOL_NULLORDER}
 * if the end of the text is reached.
 * @stable
 */
U_CAPI int32_t
ucol_previous(    UCollationElements    *elems,
        UErrorCode        *status);

/**
 * Get the maximum length of any expansion sequences that end with the 
 * specified comparison order.
 * This is useful for .... ?
 * @param elems The UCollationElements containing the text.
 * @param order A collation order returned by previous or next.
 * @return The maximum length of any expansion sequences ending with the 
 * specified order.
 * @stable
 */
U_CAPI int32_t
ucol_getMaxExpansion(    const    UCollationElements    *elems,
            int32_t                order);

/**
 * Set the text containing the collation elements.
 * This 
 * @param elems The UCollationElements to set.
 * @param text The source text containing the collation elements.
 * @param textLength The length of text, or -1 if null-terminated.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @see ucol_getText
 * @stable
 */
U_CAPI void
ucol_setText(    UCollationElements    *elems,
        const    UChar        *text,
        int32_t            textLength,
        UErrorCode        *status);

/**
 * Get the offset of the current source character.
 * This is an offset into the text of the character containing the current
 * collation elements.
 * @param elems The UCollationElements to query.
 * @return The offset of the current source character.
 * @see ucol_setOffset
 * @stable
 */
U_CAPI UTextOffset
ucol_getOffset(const UCollationElements *elems);

/**
 * Set the offset of the current source character.
 * This is an offset into the text of the character to be processed.
 * @param elems The UCollationElements to set.
 * @param offset The desired character offset.
 * @param status A pointer to an UErrorCode to receive any errors.
 * @see ucol_getOffset
 * @stable
 */
U_CAPI void
ucol_setOffset(    UCollationElements    *elems,
        UTextOffset        offset,
        UErrorCode        *status);

#endif
