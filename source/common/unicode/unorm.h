/*
*******************************************************************************
* Copyright (c) 1996-2001, International Business Machines Corporation
*               and others. All Rights Reserved.
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
 * <tt>unorm_normalize</tt> transforms Unicode text into an equivalent composed or
 * decomposed form, allowing for easier sorting and searching of text.
 * <tt>unorm_normalize</tt> supports the standard normalization forms described in
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
 * treated as the same "user-level" character "A with acute accent".  When you are searching or
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
 * <tt>unorm_normalize</tt> helps solve these problems by transforming text into the
 * canonical composed and decomposed forms as shown in the first example above.  
 * In addition, you can have it perform compatibility decompositions so that 
 * you can treat compatibility characters the same as their equivalents.
 * Finally, <tt>unorm_normalize</tt> rearranges accents into the proper canonical
 * order, so that you do not have to worry about accent rearrangement on your
 * own.
 * <p>
 * <tt>unorm_normalize</tt> adds one optional behavior, {@link #UCOL_IGNORE_HANGUL},
 * that differs from
 * the standard Unicode Normalization Forms.
 * This was used internally for collation and is deprecated.
 * It will be removed without replacement after 2002-mar-31.
 *
 * Form FCD, "Fast C or D", is also designed for collation.
 * It allows to work on strings that are not necessarily normalized
 * with an algorithm (like in collation) that works under "canonical closure", i.e., it treats precomposed
 * characters and their decomposed equivalents the same.
 *
 * It is not a normalization form because it does not provide for uniqueness of representation. Multiple strings
 * may be canonically equivalent (their NFDs are identical) and may all conform to FCD without being identical
 * themselves.
 *
 * The form is defined such that the "raw decomposition", the recursive canonical decomposition of each character,
 * results in a string that is canonically ordered. This means that precomposed characters are allowed for as long
 * as their decompositions do not need canonical reordering.
 *
 * Its advantage for a process like collation is that all NFD and most NFC texts - and many unnormalized texts -
 * already conform to FCD and do not need to be normalized (NFD) for such a process. The FCD quick check will
 * return UNORM_YES for most strings in practice.
 *
 * unorm_normalize(UNORM_FCD) may be implemented with UNORM_NFD.
 *
 * For more details on FCD see the collation design document:
 * http://oss.software.ibm.com/cvs/icu/~checkout~/icuhtml/design/collation/ICU_collation_design.htm
 */

typedef enum {
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
  /** "Fast C or D" form */
  UNORM_FCD = 6,

  UNORM_MODE_COUNT,

  /* *** The rest of this enum is entirely deprecated. *** */

  /**
   * No decomposition/composition
   * @deprecated To be removed after 2002-sep-30, use UNORM_NONE.
   */
  UCOL_NO_NORMALIZATION = 1,
  /**
   * Canonical decomposition
   * @deprecated To be removed after 2002-sep-30, use UNORM_NFD.
   */
  UCOL_DECOMP_CAN = 2,
  /**
   * Compatibility decomposition
   * @deprecated To be removed after 2002-sep-30, use UNORM_NFKD.
   */
  UCOL_DECOMP_COMPAT = 3,
  /**
   * Default normalization
   * @deprecated To be removed after 2002-sep-30, use UNORM_NFKD or UNORM_DEFAULT.
   */
  UCOL_DEFAULT_NORMALIZATION = UCOL_DECOMP_COMPAT, 
  /**
   * Canonical decomposition followed by canonical composition
   * @deprecated To be removed after 2002-sep-30, use UNORM_NFC.
   */
  UCOL_DECOMP_CAN_COMP_COMPAT = 4,
  /**
   * Compatibility decomposition followed by canonical composition
   * @deprecated To be removed after 2002-sep-30, use UNORM_NFKC.
   */
  UCOL_DECOMP_COMPAT_COMP_CAN =5,

  /**
   * Do not normalize Hangul.
   * @deprecated To be removed without replacement after 2002-mar-31.
   */
  UCOL_IGNORE_HANGUL    = 16,
  /**
   * Do not normalize Hangul.
   * @deprecated To be removed without replacement after 2002-mar-31.
   */
  UNORM_IGNORE_HANGUL    = 16
} UNormalizationMode;

/**
 * Normalize a string.
 * The string will be normalized according the the specified normalization mode
 * and options.
 * @param source The string to normalize.
 * @param sourceLength The length of source, or -1 if null-terminated.
 * @param mode The normalization mode; one of UNORM_NONE, 
 * UNORM_NFD, UNORM_NFC, UNORM_NFKC, UNORM_NFKD, UNORM_DEFAULT
 * @param options The normalization options, ORed together; possible values
 * are UNORM_IGNORE_HANGUL
 * @param result A pointer to a buffer to receive the attribute.
 * @param resultLength The maximum size of result.
 * @param status A pointer to an UErrorCode to receive any errors
 * @return The total buffer size needed; if greater than resultLength,
 * the output was truncated.
 * @stable
 */
U_CAPI int32_t
unorm_normalize(const UChar *source, int32_t sourceLength,
                UNormalizationMode mode, int32_t options,
                UChar *result, int32_t resultLength,
                UErrorCode *status);

/**
 * The function u_normalize() has been renamed to unorm_normalize()
 * for consistency. The old name is deprecated.
 * @deprecated To be removed after 2002-mar-31.
 */
#define u_normalize unorm_normalize

typedef enum UNormalizationCheckResult {
  /** 
  * Indicates that string is not in the normalized format
  */
  UNORM_NO,
  /** 
  * Indicates that string is in the normalized format
  */
  UNORM_YES,
  /** 
  * Indicates that string cannot be determined if it is in the normalized 
  * format without further thorough checks.
  */
  UNORM_MAYBE
} UNormalizationCheckResult;

/**
 * Performing quick check on a string, to quickly determine if the string is 
 * in a particular normalization format.
 * Three types of result can be returned UNORM_YES, UNORM_NO or
 * UNORM_MAYBE. Result UNORM_YES indicates that the argument
 * string is in the desired normalized format, UNORM_NO determines that
 * argument string is not in the desired normalized format. A 
 * UNORM_MAYBE result indicates that a more thorough check is required, 
 * the user may have to put the string in its normalized form and compare the 
 * results.
 * @param source       string for determining if it is in a normalized format
 * @param sourcelength length of source to test
 * @paran mode         normalization format from the enum UNormalizationMode
 * @param status A pointer to an UErrorCode to receive any errors
 * @return UNORM_YES, UNORM_NO or UNORM_MAYBE
 */
U_CAPI UNormalizationCheckResult U_EXPORT2
unorm_quickCheck(const UChar *source, int32_t sourcelength,
                 UNormalizationMode mode,
                 UErrorCode *status);

#endif
