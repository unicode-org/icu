/*
*******************************************************************************
*
*   Copyright (C) 2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucasemap.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2005may06
*   created by: Markus W. Scherer
*
*   Case mapping service object and functions using it.
*/

#ifndef __UCASEMAP_H__
#define __UCASEMAP_H__

#include "unicode/utypes.h"
#include "unicode/ustring.h"

/**
 * \file
 * \brief C API: Unicode case mapping functions using a UCaseMap service object.
 *
 * The service object takes care of memory allocations, data loading, and setup
 * for the attributes, as usual.
 *
 * Currently, the functionality provided here does not overlap with uchar.h
 * and ustring.h.
 *
 * ucasemap_utf8ToLower() and ucasemap_utf8ToUpper() operate directly on
 * UTF-8 strings.
 */

/**
 * UCaseMap is an opaque service object for newer ICU case mapping functions.
 * Older functions did not use a service object.
 * @draft ICU 3.4
 */
struct UCaseMap;
typedef struct UCaseMap UCaseMap; /**< C typedef for struct UCaseMap. @draft ICU 3.4 */

/**
 * Open a UCaseMap service object for a locale and a set of options.
 * The locale ID and options are preprocessed so that functions using the
 * service object need not process them in each call.
 *
 * @param locale ICU locale ID, used for language-dependent
 *               upper-/lower-/title-casing according to the Unicode standard.
 *               Usual semantics: ""=root, NULL=default locale, etc.
 * @param options Options bit set, used for case folding and string comparisons.
 *                Same flags as for u_foldCase(), u_strFoldCase(),
 *                u_strCaseCompare(), etc.
 *                Use 0 or U_FOLD_CASE_DEFAULT for default behavior.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                   which must not indicate a failure before the function call.
 * @return Pointer to a UCaseMap service object, if successful.
 *
 * @draft ICU 3.4
 */
U_DRAFT UCaseMap * U_EXPORT2
ucasemap_open(const char *locale, uint32_t options, UErrorCode *pErrorCode);

/**
 * Close a UCaseMap service object.
 * @param csm Object to be closed.
 * @draft ICU 3.4
 */
U_DRAFT void U_EXPORT2
ucasemap_close(UCaseMap *csm);

/**
 * Get the locale ID that is used for language-dependent case mappings.
 * @param csm UCaseMap service object.
 * @return locale ID
 * @draft ICU 3.4
 */
U_DRAFT const char * U_EXPORT2
ucasemap_getLocale(const UCaseMap *csm);

/**
 * Get the options bit set that is used for case folding and string comparisons.
 * @param csm UCaseMap service object.
 * @return options bit set
 * @draft ICU 3.4
 */
U_DRAFT uint32_t U_EXPORT2
ucasemap_getOptions(const UCaseMap *csm);

/**
 * Set the locale ID that is used for language-dependent case mappings.
 *
 * @param csm UCaseMap service object.
 * @param locale Locale ID, see ucasemap_open().
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                   which must not indicate a failure before the function call.
 *
 * @see ucasemap_open
 * @draft ICU 3.4
 */
U_DRAFT void U_EXPORT2
ucasemap_setLocale(UCaseMap *csm, const char *locale, UErrorCode *pErrorCode);

/**
 * Set the options bit set that is used for case folding and string comparisons.
 *
 * @param csm UCaseMap service object.
 * @param options Options bit set, see ucasemap_open().
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                   which must not indicate a failure before the function call.
 *
 * @see ucasemap_open
 * @draft ICU 3.4
 */
U_DRAFT void U_EXPORT2
ucasemap_setOptions(UCaseMap *csm, uint32_t options, UErrorCode *pErrorCode);

/**
 * Lowercase the characters in a UTF-8 string.
 * Casing is locale-dependent and context-sensitive.
 * The result may be longer or shorter than the original.
 * The source string and the destination buffer must not overlap.
 *
 * @param csm       UCaseMap service object.
 * @param dest      A buffer for the result string. The result will be NUL-terminated if
 *                  the buffer is large enough.
 *                  The contents is undefined in case of failure.
 * @param destCapacity The size of the buffer (number of bytes). If it is 0, then
 *                  dest may be NULL and the function will only return the length of the result
 *                  without writing any of the result string.
 * @param src       The original string
 * @param srcLength The length of the original string. If -1, then src must be NUL-terminated.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                  which must not indicate a failure before the function call.
 * @return The length of the result string, if successful - or in case of a buffer overflow,
 *         in which case it will be greater than destCapacity.
 *
 * @see u_strToLower
 * @draft ICU 3.4
 */
U_DRAFT int32_t U_EXPORT2
ucasemap_utf8ToLower(const UCaseMap *csm,
                     char *dest, int32_t destCapacity,
                     const char *src, int32_t srcLength,
                     UErrorCode *pErrorCode);

/**
 * Uppercase the characters in a UTF-8 string.
 * Casing is locale-dependent and context-sensitive.
 * The result may be longer or shorter than the original.
 * The source string and the destination buffer must not overlap.
 *
 * @param csm       UCaseMap service object.
 * @param dest      A buffer for the result string. The result will be NUL-terminated if
 *                  the buffer is large enough.
 *                  The contents is undefined in case of failure.
 * @param destCapacity The size of the buffer (number of bytes). If it is 0, then
 *                  dest may be NULL and the function will only return the length of the result
 *                  without writing any of the result string.
 * @param src       The original string
 * @param srcLength The length of the original string. If -1, then src must be NUL-terminated.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                  which must not indicate a failure before the function call.
 * @return The length of the result string, if successful - or in case of a buffer overflow,
 *         in which case it will be greater than destCapacity.
 *
 * @see u_strToUpper
 * @draft ICU 3.4
 */
U_DRAFT int32_t U_EXPORT2
ucasemap_utf8ToUpper(const UCaseMap *csm,
                     char *dest, int32_t destCapacity,
                     const char *src, int32_t srcLength,
                     UErrorCode *pErrorCode);

#endif
