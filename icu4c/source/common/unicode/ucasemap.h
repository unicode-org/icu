// Copyright (C) 2016 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/*
*******************************************************************************
*
*   Copyright (C) 2005-2012, International Business Machines
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
#include "unicode/localpointer.h"

#if U_SHOW_CPLUSPLUS_API

#include "unicode/uobject.h"

#endif  // U_SHOW_CPLUSPLUS_API

/**
 * \file
 * \brief C API: Unicode case mapping functions using a UCaseMap service object.
 *
 * The service object takes care of memory allocations, data loading, and setup
 * for the attributes, as usual.
 *
 * Currently, the functionality provided here does not overlap with uchar.h
 * and ustring.h, except for ucasemap_toTitle().
 *
 * ucasemap_utf8XYZ() functions operate directly on UTF-8 strings.
 */

/**
 * UCaseMap is an opaque service object for newer ICU case mapping functions.
 * Older functions did not use a service object.
 * @stable ICU 3.4
 */
struct UCaseMap;
typedef struct UCaseMap UCaseMap; /**< C typedef for struct UCaseMap. @stable ICU 3.4 */

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
 * @see U_FOLD_CASE_DEFAULT
 * @see U_FOLD_CASE_EXCLUDE_SPECIAL_I
 * @see U_TITLECASE_NO_LOWERCASE
 * @see U_TITLECASE_NO_BREAK_ADJUSTMENT
 * @stable ICU 3.4
 */
U_STABLE UCaseMap * U_EXPORT2
ucasemap_open(const char *locale, uint32_t options, UErrorCode *pErrorCode);

/**
 * Close a UCaseMap service object.
 * @param csm Object to be closed.
 * @stable ICU 3.4
 */
U_STABLE void U_EXPORT2
ucasemap_close(UCaseMap *csm);

#if U_SHOW_CPLUSPLUS_API

U_NAMESPACE_BEGIN

/**
 * \class LocalUCaseMapPointer
 * "Smart pointer" class, closes a UCaseMap via ucasemap_close().
 * For most methods see the LocalPointerBase base class.
 *
 * @see LocalPointerBase
 * @see LocalPointer
 * @stable ICU 4.4
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalUCaseMapPointer, UCaseMap, ucasemap_close);

#ifndef U_HIDE_INTERNAL_API

/**
 * Records lengths of string edits but not replacement text.
 * Supports replacements, insertions, deletions in linear progression.
 * Does not support moving/reordering of text.
 *
 * @internal ICU 59 technology preview
 */
class Edits final : public UMemory {
public:
    /**
     * Constructs an empty object.
     * @internal ICU 59 technology preview
     */
    Edits() :
            array(stackArray), capacity(STACK_CAPACITY), length(0), delta(0),
            omit(FALSE), errorCode(U_ZERO_ERROR) {}
    ~Edits();

    /**
     * Resets the data but may not release memory.
     * @internal ICU 59 technology preview
     */
    void reset();

    /**
     * Controls whether the case mapping function is to write or omit
     * characters that do not change.
     * The complete result can be computed by applying just the changes
     * to the original string.
     * @see omitUnchanged
     * @see writeUnchanged
     * @internal ICU 59 technology preview
     */
    Edits &setWriteUnchanged(UBool write) {
        omit = !write;
        return *this;
    }
    /**
     * @return TRUE if the case mapping function is to omit characters that do not change.
     * @see setWriteUnchanged
     * @internal ICU 59 technology preview
     */
    UBool omitUnchanged() const { return omit; }
    /**
     * @return TRUE if the case mapping function is to write characters that do not change.
     * @see setWriteUnchanged
     * @internal ICU 59 technology preview
     */
    UBool writeUnchanged() const { return !omit; }

    /**
     * Adds a record for an unchanged segment of text.
     * @internal ICU 59 technology preview
     */
    void addUnchanged(int32_t unchangedLength);
    /**
     * Adds a record for a text replacement/insertion/deletion.
     * @internal ICU 59 technology preview
     */
    void addReplace(int32_t oldLength, int32_t newLength);
    /**
     * Sets the UErrorCode if an error occurred while recording edits.
     * Preserves older error codes in the outErrorCode.
     * @return TRUE if U_FAILURE(outErrorCode)
     * @internal ICU 59 technology preview
     */
    UBool setErrorCode(UErrorCode &outErrorCode);

    /**
     * How much longer is the new text compared with the old text?
     * @return new length minus old length
     * @internal ICU 59 technology preview
     */
    int32_t lengthDelta() const { return delta; }
    /**
     * @return TRUE if there are any change edits
     * @internal ICU 59 technology preview
     */
    UBool hasChanges() const;

    /**
     * Access to the list of edits.
     * @see getCoarseIterator
     * @see getFineIterator
     * @internal ICU 59 technology preview
     */
    struct Iterator final : public UMemory {
        /**
         * Advances to the next edit.
         * @return TRUE if there is another edit
         * @internal ICU 59 technology preview
         */
        UBool next(UErrorCode &errorCode);

        /**
         * TRUE if this edit replaces oldLength units with newLength different ones.
         * FALSE if oldLength units remain unchanged.
         * @internal ICU 59 technology preview
         */
        UBool changed;
        /**
         * Number of units in the original string which are replaced or remain unchanged.
         * @internal ICU 59 technology preview
         */
        int32_t oldLength;
        /**
         * Number of units in the modified string, if changed is TRUE.
         * Same as oldLength if changed is FALSE.
         * @internal ICU 59 technology preview
         */
        int32_t newLength;

    private:
        friend class Edits;

        Iterator(const uint16_t *a, int32_t len, UBool crs) :
                array(a), index(0), length(len), width(0), remaining(0), coarse(crs) {}

        int32_t readLength(int32_t head);

        const uint16_t *array;
        int32_t index, length;
        int32_t width, remaining;
        UBool coarse;
    };

    /**
     * Returns an Iterator for coarse-grained changes for simple string updates.
     * @return an Iterator that merges adjacent changes.
     * @internal ICU 59 technology preview
     */
    Iterator getCoarseIterator() const {
        return Iterator(array, length, TRUE);
    }

    /**
     * Returns an Iterator for fine-grained changes for modifying text with metadata.
     * @return an Iterator that separates adjacent changes.
     * @internal ICU 59 technology preview
     */
    Iterator getFineIterator() const {
        return Iterator(array, length, FALSE);
    }

private:
    Edits(const Edits &) = delete;
    Edits &operator=(const Edits &) = delete;

    void setLastUnit(int32_t last) { array[length - 1] = (uint16_t)last; }
    int32_t lastUnit() const { return length > 0 ? array[length - 1] : 0xffff; }

    void append(int32_t r);
    void append(const uint16_t *buffer, int32_t bLength);
    UBool growArray();

    static const int32_t STACK_CAPACITY = 100;
    uint16_t *array;
    int32_t capacity;
    int32_t length;
    int32_t delta;
    UBool omit;
    UErrorCode errorCode;
    uint16_t stackArray[STACK_CAPACITY];
};

/**
 * Omit unchanged text when case-mapping with Edits.
 *
 * TODO: revisit which bit to use; currently:
 * - 31..20: old normalization options (only deprecated Unicode 3.2)
 *           shifted up for unorm_compare()
 * - 19..16: more options specific to unorm_compare() (currently bits 19, 17, 16)
 * - 15..12: more string compare options (currently bits 15 & 12)
 * - 11.. 8: titlecase mapping options (currently bits 9..8)
 * -  7.. 0: case folding options, but only bit 0 currently used
 *
 * could overlay any normalization and string *comparison* option bits
 * with case *mapping* option bits
 * *unless* we start using UCaseMap for string comparison functions
 *
 * future: German sharp s may need locale variant or option bit
 *
 * @internal ICU 59 technology preview
 */
// TODO: does not work well as an option because we would need to set/reset it on UCaseMaps
// that are often const, replaced for now by Edits.setWriteUnchanged(UBool)
// #define UCASEMAP_OMIT_UNCHANGED 0x4000

#endif  // U_HIDE_INTERNAL_API

U_NAMESPACE_END

#endif

/**
 * Get the locale ID that is used for language-dependent case mappings.
 * @param csm UCaseMap service object.
 * @return locale ID
 * @stable ICU 3.4
 */
U_STABLE const char * U_EXPORT2
ucasemap_getLocale(const UCaseMap *csm);

/**
 * Get the options bit set that is used for case folding and string comparisons.
 * @param csm UCaseMap service object.
 * @return options bit set
 * @stable ICU 3.4
 */
U_STABLE uint32_t U_EXPORT2
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
 * @stable ICU 3.4
 */
U_STABLE void U_EXPORT2
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
 * @stable ICU 3.4
 */
U_STABLE void U_EXPORT2
ucasemap_setOptions(UCaseMap *csm, uint32_t options, UErrorCode *pErrorCode);

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

#if !UCONFIG_NO_BREAK_ITERATION

/**
 * Get the break iterator that is used for titlecasing.
 * Do not modify the returned break iterator.
 * @param csm UCaseMap service object.
 * @return titlecasing break iterator
 * @stable ICU 3.8
 */
U_STABLE const UBreakIterator * U_EXPORT2
ucasemap_getBreakIterator(const UCaseMap *csm);

/**
 * Set the break iterator that is used for titlecasing.
 * The UCaseMap service object releases a previously set break iterator
 * and "adopts" this new one, taking ownership of it.
 * It will be released in a subsequent call to ucasemap_setBreakIterator()
 * or ucasemap_close().
 *
 * Break iterator operations are not thread-safe. Therefore, titlecasing
 * functions use non-const UCaseMap objects. It is not possible to titlecase
 * strings concurrently using the same UCaseMap.
 *
 * @param csm UCaseMap service object.
 * @param iterToAdopt Break iterator to be adopted for titlecasing.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                   which must not indicate a failure before the function call.
 *
 * @see ucasemap_toTitle
 * @see ucasemap_utf8ToTitle
 * @stable ICU 3.8
 */
U_STABLE void U_EXPORT2
ucasemap_setBreakIterator(UCaseMap *csm, UBreakIterator *iterToAdopt, UErrorCode *pErrorCode);

/**
 * Titlecase a UTF-16 string. This function is almost a duplicate of u_strToTitle(),
 * except that it takes ucasemap_setOptions() into account and has performance
 * advantages from being able to use a UCaseMap object for multiple case mapping
 * operations, saving setup time.
 *
 * Casing is locale-dependent and context-sensitive.
 * Titlecasing uses a break iterator to find the first characters of words
 * that are to be titlecased. It titlecases those characters and lowercases
 * all others. (This can be modified with ucasemap_setOptions().)
 *
 * Note: This function takes a non-const UCaseMap pointer because it will
 * open a default break iterator if no break iterator was set yet,
 * and effectively call ucasemap_setBreakIterator();
 * also because the break iterator is stateful and will be modified during
 * the iteration.
 *
 * The titlecase break iterator can be provided to customize for arbitrary
 * styles, using rules and dictionaries beyond the standard iterators.
 * The standard titlecase iterator for the root locale implements the
 * algorithm of Unicode TR 21.
 *
 * This function uses only the setUText(), first(), next() and close() methods of the
 * provided break iterator.
 *
 * The result may be longer or shorter than the original.
 * The source string and the destination buffer must not overlap.
 *
 * @param csm       UCaseMap service object. This pointer is non-const!
 *                  See the note above for details.
 * @param dest      A buffer for the result string. The result will be NUL-terminated if
 *                  the buffer is large enough.
 *                  The contents is undefined in case of failure.
 * @param destCapacity The size of the buffer (number of bytes). If it is 0, then
 *                  dest may be NULL and the function will only return the length of the result
 *                  without writing any of the result string.
 * @param src       The original string.
 * @param srcLength The length of the original string. If -1, then src must be NUL-terminated.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                  which must not indicate a failure before the function call.
 * @return The length of the result string, if successful - or in case of a buffer overflow,
 *         in which case it will be greater than destCapacity.
 *
 * @see u_strToTitle
 * @stable ICU 3.8
 */
U_STABLE int32_t U_EXPORT2
ucasemap_toTitle(UCaseMap *csm,
                 UChar *dest, int32_t destCapacity,
                 const UChar *src, int32_t srcLength,
                 UErrorCode *pErrorCode);

#endif

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
 * @param src       The original string.
 * @param srcLength The length of the original string. If -1, then src must be NUL-terminated.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                  which must not indicate a failure before the function call.
 * @return The length of the result string, if successful - or in case of a buffer overflow,
 *         in which case it will be greater than destCapacity.
 *
 * @see u_strToLower
 * @stable ICU 3.4
 */
U_STABLE int32_t U_EXPORT2
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
 * @param src       The original string.
 * @param srcLength The length of the original string. If -1, then src must be NUL-terminated.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                  which must not indicate a failure before the function call.
 * @return The length of the result string, if successful - or in case of a buffer overflow,
 *         in which case it will be greater than destCapacity.
 *
 * @see u_strToUpper
 * @stable ICU 3.4
 */
U_STABLE int32_t U_EXPORT2
ucasemap_utf8ToUpper(const UCaseMap *csm,
                     char *dest, int32_t destCapacity,
                     const char *src, int32_t srcLength,
                     UErrorCode *pErrorCode);

#if !UCONFIG_NO_BREAK_ITERATION

/**
 * Titlecase a UTF-8 string.
 * Casing is locale-dependent and context-sensitive.
 * Titlecasing uses a break iterator to find the first characters of words
 * that are to be titlecased. It titlecases those characters and lowercases
 * all others. (This can be modified with ucasemap_setOptions().)
 *
 * Note: This function takes a non-const UCaseMap pointer because it will
 * open a default break iterator if no break iterator was set yet,
 * and effectively call ucasemap_setBreakIterator();
 * also because the break iterator is stateful and will be modified during
 * the iteration.
 *
 * The titlecase break iterator can be provided to customize for arbitrary
 * styles, using rules and dictionaries beyond the standard iterators.
 * The standard titlecase iterator for the root locale implements the
 * algorithm of Unicode TR 21.
 *
 * This function uses only the setUText(), first(), next() and close() methods of the
 * provided break iterator.
 *
 * The result may be longer or shorter than the original.
 * The source string and the destination buffer must not overlap.
 *
 * @param csm       UCaseMap service object. This pointer is non-const!
 *                  See the note above for details.
 * @param dest      A buffer for the result string. The result will be NUL-terminated if
 *                  the buffer is large enough.
 *                  The contents is undefined in case of failure.
 * @param destCapacity The size of the buffer (number of bytes). If it is 0, then
 *                  dest may be NULL and the function will only return the length of the result
 *                  without writing any of the result string.
 * @param src       The original string.
 * @param srcLength The length of the original string. If -1, then src must be NUL-terminated.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                  which must not indicate a failure before the function call.
 * @return The length of the result string, if successful - or in case of a buffer overflow,
 *         in which case it will be greater than destCapacity.
 *
 * @see u_strToTitle
 * @see U_TITLECASE_NO_LOWERCASE
 * @see U_TITLECASE_NO_BREAK_ADJUSTMENT
 * @stable ICU 3.8
 */
U_STABLE int32_t U_EXPORT2
ucasemap_utf8ToTitle(UCaseMap *csm,
                    char *dest, int32_t destCapacity,
                    const char *src, int32_t srcLength,
                    UErrorCode *pErrorCode);

#endif

/**
 * Case-folds the characters in a UTF-8 string.
 *
 * Case-folding is locale-independent and not context-sensitive,
 * but there is an option for whether to include or exclude mappings for dotted I
 * and dotless i that are marked with 'T' in CaseFolding.txt.
 *
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
 * @param src       The original string.
 * @param srcLength The length of the original string. If -1, then src must be NUL-terminated.
 * @param pErrorCode Must be a valid pointer to an error code value,
 *                  which must not indicate a failure before the function call.
 * @return The length of the result string, if successful - or in case of a buffer overflow,
 *         in which case it will be greater than destCapacity.
 *
 * @see u_strFoldCase
 * @see ucasemap_setOptions
 * @see U_FOLD_CASE_DEFAULT
 * @see U_FOLD_CASE_EXCLUDE_SPECIAL_I
 * @stable ICU 3.8
 */
U_STABLE int32_t U_EXPORT2
ucasemap_utf8FoldCase(const UCaseMap *csm,
                      char *dest, int32_t destCapacity,
                      const char *src, int32_t srcLength,
                      UErrorCode *pErrorCode);

#if U_SHOW_CPLUSPLUS_API

// Not #ifndef U_HIDE_INTERNAL_API because UnicodeString needs the UStringCaseMapper.
/**
 * Internal string case mapping function type.
 * All error checking must be done.
 * The UCaseMap must be fully initialized, with locale and/or iter set as needed.
 * src and dest must not overlap.
 * @internal
 */
typedef int32_t U_CALLCONV
UStringCaseMapper(const UCaseMap *csm,
                  UChar *dest, int32_t destCapacity,
                  const UChar *src, int32_t srcLength,
                  icu::Edits *edits,
                  UErrorCode *pErrorCode);

#endif  // U_SHOW_CPLUSPLUS_API
#endif
