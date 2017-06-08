// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// stringoptions.h
// created: 2017jun08 Markus W. Scherer

#ifndef __STRINGOPTIONS_H__
#define __STRINGOPTIONS_H__

#include "unicode/utypes.h"

/**
 * \file
 * \brief C API: Bit set option bit constants for various string and character processing functions.
 */

/**
 * Option value for case folding: Use default mappings defined in CaseFolding.txt.
 *
 * @stable ICU 2.0
 */
#define U_FOLD_CASE_DEFAULT 0

/**
 * Option value for case folding:
 *
 * Use the modified set of mappings provided in CaseFolding.txt to handle dotted I
 * and dotless i appropriately for Turkic languages (tr, az).
 *
 * Before Unicode 3.2, CaseFolding.txt contains mappings marked with 'I' that
 * are to be included for default mappings and
 * excluded for the Turkic-specific mappings.
 *
 * Unicode 3.2 CaseFolding.txt instead contains mappings marked with 'T' that
 * are to be excluded for default mappings and
 * included for the Turkic-specific mappings.
 *
 * @stable ICU 2.0
 */
#define U_FOLD_CASE_EXCLUDE_SPECIAL_I 1

/**
 * Do not lowercase non-initial parts of words when titlecasing.
 * Option bit for titlecasing APIs that take an options bit set.
 *
 * By default, titlecasing will titlecase the first cased character
 * of a word and lowercase all other characters.
 * With this option, the other characters will not be modified.
 *
 * @see ucasemap_setOptions
 * @see ucasemap_toTitle
 * @see ucasemap_utf8ToTitle
 * @see UnicodeString::toTitle
 * @stable ICU 3.8
 */
#define U_TITLECASE_NO_LOWERCASE 0x100

/**
 * Do not adjust the titlecasing indexes from BreakIterator::next() indexes;
 * titlecase exactly the characters at breaks from the iterator.
 * Option bit for titlecasing APIs that take an options bit set.
 *
 * By default, titlecasing will take each break iterator index,
 * adjust it by looking for the next cased character, and titlecase that one.
 * Other characters are lowercased.
 *
 * This follows Unicode 4 & 5 section 3.13 Default Case Operations:
 *
 * R3  toTitlecase(X): Find the word boundaries based on Unicode Standard Annex
 * #29, "Text Boundaries." Between each pair of word boundaries, find the first
 * cased character F. If F exists, map F to default_title(F); then map each
 * subsequent character C to default_lower(C).
 *
 * @see ucasemap_setOptions
 * @see ucasemap_toTitle
 * @see ucasemap_utf8ToTitle
 * @see UnicodeString::toTitle
 * @see U_TITLECASE_NO_LOWERCASE
 * @stable ICU 3.8
 */
#define U_TITLECASE_NO_BREAK_ADJUSTMENT 0x200

#ifndef U_HIDE_DRAFT_API

/**
 * Omit unchanged text when recording how source substrings
 * relate to changed and unchanged result substrings.
 * Used for example in some case-mapping and normalization functions.
 *
 * @see CaseMap
 * @see Edits
 * @see Normalizer2
 * @draft ICU 60
 */
#define U_OMIT_UNCHANGED_TEXT 0x4000

#endif  // U_HIDE_DRAFT_API

/**
 * Option bit for u_strCaseCompare, u_strcasecmp, unorm_compare, etc:
 * Compare strings in code point order instead of code unit order.
 * @stable ICU 2.2
 */
#define U_COMPARE_CODE_POINT_ORDER  0x8000

/**
 * Option bit for unorm_compare:
 * Perform case-insensitive comparison.
 * @stable ICU 2.2
 */
#define U_COMPARE_IGNORE_CASE       0x10000

/**
 * Option bit for unorm_compare:
 * Both input strings are assumed to fulfill FCD conditions.
 * @stable ICU 2.2
 */
#define UNORM_INPUT_IS_FCD          0x20000

// Related definitions elsewhere.
// Options that are not meaningful in the same functions
// can share the same bits.
//
// Public:
// unicode/unorm.h #define UNORM_COMPARE_NORM_OPTIONS_SHIFT 20
//
// Internal: (may change or be removed)
// ucase.h #define _STRCASECMP_OPTIONS_MASK 0xffff
// ucase.h #define _FOLD_CASE_OPTIONS_MASK 0xff
// ustr_imp.h #define _STRNCMP_STYLE 0x1000
// unormcmp.cpp #define _COMPARE_EQUIV 0x80000

#endif  // __STRINGOPTIONS_H__
