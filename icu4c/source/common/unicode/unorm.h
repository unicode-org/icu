/*
*******************************************************************************
* Copyright © {1996-2001}, International Business Machines Corporation and others. All Rights Reserved.
*******************************************************************************
* File unorm.h
*
* Created by: Vladimir Weinstein 12052000
*
* Modification history :
*
* Date        Name        Description
* 02/01/01    synwee      Added normalization quickcheck enum and method.
*/
#ifndef UNORM_H
#define UNORM_H

#include "unicode/utypes.h"

/**
 * \file
 * \brief C API: Unicode Normalization 
 *
 * <h2>  Unicode normalization API </h2>
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
 * \code
 *      00C1    LATIN CAPITAL LETTER A WITH ACUTE
 * \endcode
 * </pre>
 * or as two separate characters (the "decomposed" form):
 * <pre>
 * \code
 *      0041    LATIN CAPITAL LETTER A
 *      0301    COMBINING ACUTE ACCENT</pre>
 * \endcode
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
 * \code
 *      0066    LATIN SMALL LETTER F
 *      0066    LATIN SMALL LETTER F
 *      0069    LATIN SMALL LETTER I
 * \endcode
 * </pre>
 * or as the single character
 * <pre>
 * \code
 *      FB03    LATIN SMALL LIGATURE FFI</pre>
 * \endcode
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
    * UCOL_NO_NORMALIZATION : Accented characters will not be decomposed for sorting.  
    * UCOL_DECOM_CAN          : Characters that are canonical variants according 
    * to Unicode 2.0 will be decomposed for sorting. 
    * UCOL_DECOMP_COMPAT    : Characters that are compatibility variants will be
    * decomposed for sorting. This is the default normalization mode used.
    * UCOL_DECOMP_CAN_COMP_COMPAT : Canonical decomposition followed by canonical composition 
    * UCOL_DECOMP_COMPAT_COMP_CAN : Compatibility decomposition followed by canonical composition
    *
    **/

typedef enum {
  /** No decomposition/composition */
  UCOL_NO_NORMALIZATION = 1,
  /** Canonical decomposition */
  UCOL_DECOMP_CAN = 2,
  /** Compatibility decomposition */
  UCOL_DECOMP_COMPAT = 3,
  /** Default normalization */
  UCOL_DEFAULT_NORMALIZATION = UCOL_DECOMP_COMPAT, 
  /** Canonical decomposition followed by canonical composition */
  UCOL_DECOMP_CAN_COMP_COMPAT = 4,
  /** Compatibility decomposition followed by canonical composition */
  UCOL_DECOMP_COMPAT_COMP_CAN =5,
  /** No decomposition/composition */
  UNORM_NONE = 1, 
  /** Canonical decomposition */
  UNORM_NFD = 2,
  /** Compatibility decomposition */
  UNORM_NFKD = 3,
  /** Canonical decomposition followed by canonical composition */
  UNORM_NFC = 4,
  /** Default normalization */
  UNORM_DEFAULT = UNORM_NFC, 
  /** Compatibility decomposition followed by canonical composition */
  UNORM_NFKC =5,

  UNORM_MODE_COUNT,

  /** Do not normalize Hangul */
  UCOL_IGNORE_HANGUL    = 16,
  UNORM_IGNORE_HANGUL    = 16
} UNormalizationMode;

/** Possible normalization options */
typedef UNormalizationMode UNormalizationOption;

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

typedef enum {
  /** 
  * Indicates that string is not in the normalized format
  */
  UQUICK_CHECK_NO, 
  /** 
  * Indicates that string is in the normalized format
  */
  UQUICK_CHECK_YES,
  /** 
  * Indicates that string cannot be determined if it is in the normalized 
  * format without further thorough checks.
  */
  UQUICK_CHECK_MAYBE
} UQUICK_CHECK_VALUES;

/**
 * Performing quick check on a string, to quickly determine if the string is 
 * in a particular normalization format.
 * Three types of result can be returned UQUICK_CHECK_YES, UQUICK_CHECK_NO or
 * UQUICK_CHECK_MAYBE. Result UQUICK_CHECK_YES indicates that the argument
 * string is in the desired normalized format, UQUICK_CHECK_NO determines that
 * argument string is not in the desired normalized format. A 
 * UQUICK_CHECK_MAYBE result indicates that a more thorough check is required, 
 * the user may have to put the string in its normalized form and compare the 
 * results.
 * @param source       string for determining if it is in a normalized format
 * @param sourcelength length of source to test
 * @paran mode         normalization format either UCOL_DECOMP_CAN, 
 *                     UCOL_DECOMP_COMPAT, UCOL_DECOMP_CAN_COMP_COMPAT or
 *                     UCOL_DECOMP_COMPAT_COMP_CAN
 * @param status A pointer to an UErrorCode to receive any errors
 * @return UQUICK_CHECK_YES, UQUICK_CHECK_NO or UQUICK_CHECK_MAYBE
 */
U_CAPI UQUICK_CHECK_VALUES
u_quickCheck(const UChar*       source,
             int32_t            sourcelength, 
             UNormalizationMode mode, 
             UErrorCode*        status);

/**
 * Performing quick check on a string, to quickly determine if the string is 
 * in a particular normalization format.
 * Three types of result can be returned UQUICK_CHECK_YES, UQUICK_CHECK_NO or
 * UQUICK_CHECK_MAYBE. Result UQUICK_CHECK_YES indicates that the argument
 * string is in the desired normalized format, UQUICK_CHECK_NO determines that
 * argument string is not in the desired normalized format. A 
 * UQUICK_CHECK_MAYBE result indicates that a more thorough check is required, 
 * the user may have to put the string in its normalized form and compare the 
 * results.
 * @param source       string for determining if it is in a normalized format
 * @param sourcelength length of source to test
 * @paran mode         normalization format either UCOL_DECOMP_CAN, 
 *                     UCOL_DECOMP_COMPAT, UCOL_DECOMP_CAN_COMP_COMPAT or
 *                     UCOL_DECOMP_COMPAT_COMP_CAN
 * @param options The normalization options, ORed together; possible values
 *        are UCOL_IGNORE_HANGUL
 * @param status A pointer to an UErrorCode to receive any errors
 * @return UQUICK_CHECK_YES, UQUICK_CHECK_NO or UQUICK_CHECK_MAYBE
 */
U_CAPI UQUICK_CHECK_VALUES
u_quickCheckWithOption(const UChar*       source,
                       int32_t            sourcelength, 
                       UNormalizationMode mode, 
                       int32_t            options,
                       UErrorCode*        status);

#endif
