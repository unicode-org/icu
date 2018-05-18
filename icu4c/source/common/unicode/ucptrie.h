// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// ucptrie.h (modified from utrie2.h)
// created: 2017dec29 Markus W. Scherer

#ifndef __UCPTRIE_H__
#define __UCPTRIE_H__

#include "unicode/utypes.h"
#include "unicode/localpointer.h"
#include "unicode/utf8.h"
#include "putilimp.h"
#include "udataswp.h"

U_CDECL_BEGIN

/**
 * \file
 *
 * This file defines an immutable Unicode code point trie.
 *
 * @see UCPTrie
 * @see UMutableCPTrie
 */

/**
 * Immutable Unicode code point trie structure.
 * Fast, reasonably compact, map from Unicode code points (U+0000..U+10FFFF) to integer values.
 * For details see http://site.icu-project.org/design/struct/utrie
 *
 * Do not access UCPTrie fields directly; use public functions and macros.
 * TODO: discuss why lots of macros vs. C++, what happens if wrong one used
 * - necessary for the type*width combinations unless we use a width parameter
 * - necessary for avoiding checks & dispatches.
 * - The macros will return bogus values, or may crash, if used on the wrong type or value width.
 * - use assert.h ??
 *
 * @see UMutableCPTrie
 * @draft ICU 63
 */
struct UCPTrie;
typedef struct UCPTrie UCPTrie;

/**
 * Selectors for the type of a UCPTrie.
 * Different trade-offs for size vs. speed.
 *
 * @see umutablecptrie_buildImmutable
 * @see ucptrie_openFromBinary
 * @see ucptrie_getType
 * @draft ICU 63
 */
enum UCPTrieType {
    /**
     * For ucptrie_openFromBinary() to accept any type.
     * ucptrie_getType() will return the actual type.
     * @draft ICU 63
     */
    UCPTRIE_TYPE_ANY = -1,
    /**
     * Fast/simple/larger BMP data structure. Use functions and "fast" macros.
     * @draft ICU 63
     */
    UCPTRIE_TYPE_FAST,
    /**
     * Small/slower BMP data structure. Use functions and "small" macros.
     * @draft ICU 63
     */
    UCPTRIE_TYPE_SMALL
};
typedef enum UCPTrieType UCPTrieType;

/**
 * Selectors for the number of bits in a UCPTrie data value.
 *
 * @see umutablecptrie_buildImmutable
 * @see ucptrie_openFromBinary
 * @see ucptrie_getValueWidth
 * @draft ICU 63
 */
enum UCPTrieValueWidth {
    /**
     * For ucptrie_openFromBinary() to accept any data value width.
     * ucptrie_getValueWidth() will return the actual data value width.
     * @draft ICU 63
     */
    UCPTRIE_VALUE_BITS_ANY = -1,
    /**
     * 16 bits per UCPTrie data value.
     * @draft ICU 63
     */
    UCPTRIE_VALUE_BITS_16,
    /**
     * 32 bits per UCPTrie data value.
     * @draft ICU 63
     */
    UCPTRIE_VALUE_BITS_32,
    /**
     * 8 bits per UCPTrie data value.
     * @draft ICU 63
     */
    UCPTRIE_VALUE_BITS_8
};
typedef enum UCPTrieValueWidth UCPTrieValueWidth;

/**
 * Opens a trie from its binary form, stored in 32-bit-aligned memory.
 * Inverse of ucptrie_toBinary().
 *
 * The memory must remain valid and unchanged as long as the trie is used.
 * You must ucptrie_close() the trie once you are done using it.
 *
 * @param type selects the trie type; results in an
 *             U_INVALID_FORMAT_ERROR if it does not match the binary data;
 *             use UCPTRIE_TYPE_ANY to accept any type
 * @param valueWidth selects the number of bits in a data value; results in an
 *                  U_INVALID_FORMAT_ERROR if it does not match the binary data;
 *                  use UCPTRIE_VALUE_BITS_ANY to accept any data value width
 * @param data a pointer to 32-bit-aligned memory containing the binary data of a UCPTrie
 * @param length the number of bytes available at data;
 *               can be more than necessary
 * @param pActualLength receives the actual number of bytes at data taken up by the trie data;
 *                      can be NULL
 * @param pErrorCode an in/out ICU UErrorCode
 * @return the trie
 *
 * @see umutablecptrie_open
 * @see umutablecptrie_buildImmutable
 * @see ucptrie_toBinary
 * @draft ICU 63
 */
U_CAPI UCPTrie * U_EXPORT2
ucptrie_openFromBinary(UCPTrieType type, UCPTrieValueWidth valueWidth,
                       const void *data, int32_t length, int32_t *pActualLength,
                       UErrorCode *pErrorCode);

/**
 * Closes a trie and releases associated memory.
 *
 * @param trie the trie
 * @draft ICU 63
 */
U_CAPI void U_EXPORT2
ucptrie_close(UCPTrie *trie);

#if U_SHOW_CPLUSPLUS_API

U_NAMESPACE_BEGIN

/**
 * \class LocalUCPTriePointer
 * "Smart pointer" class, closes a UCPTrie via ucptrie_close().
 * For most methods see the LocalPointerBase base class.
 *
 * @see LocalPointerBase
 * @see LocalPointer
 * @draft ICU 63
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalUCPTriePointer, UCPTrie, ucptrie_close);

U_NAMESPACE_END

#endif

/**
 * Returns the trie type.
 *
 * @param trie the trie
 * @see ucptrie_openFromBinary
 * @see UCPTRIE_TYPE_ANY
 * @draft ICU 63
 */
U_CAPI UCPTrieType U_EXPORT2
ucptrie_getType(const UCPTrie *trie);

/**
 * Returns the the number of bits in a trie data value.
 *
 * @param trie the trie
 * @see ucptrie_openFromBinary
 * @see UCPTRIE_VALUE_BITS_ANY
 * @draft ICU 63
 */
U_CAPI UCPTrieValueWidth U_EXPORT2
ucptrie_getValueWidth(const UCPTrie *trie);

/**
 * Returns the value for a code point as stored in the trie.
 * Easier to use than UCPTRIE_FAST_GET16() and similar macros but slower.
 * Easier to use because, unlike the macros, this function works on all UCPTrie
 * objects, for all types and value widths.
 *
 * @param trie the trie
 * @param c the code point
 * @return the value
 * @draft ICU 63
 */
U_CAPI uint32_t U_EXPORT2
ucptrie_get(const UCPTrie *trie, UChar32 c);

/**
 * Callback function type: Modifies a trie value.
 * Optionally called by ucptrie_getRange() or umutablecptrie_getRange().
 * The modified value will be returned by the getRange function.
 *
 * Can be used to ignore some of the value bits,
 * make a filter for one of several values,
 * return a value index computed from the trie value, etc.
 *
 * @param context an opaque pointer, as passed into the getRange function
 * @param value a value from the trie
 * @return the modified value
 * @draft ICU 63
 */
typedef uint32_t U_CALLCONV
UCPTrieHandleValue(const void *context, uint32_t value);

/**
 * Returns the last code point such that all those from start to there have the same value.
 * Can be used to efficiently iterate over all same-value ranges in a trie.
 *
 * If the UCPTrieHandleValue function pointer is not NULL, then
 * the value to be delivered is passed through that function, and the return value is the end
 * of the range where all values are modified to the same actual value.
 * The value is unchanged if that function pointer is NULL.
 *
 * Example:
 * \code
 * UChar32 start = 0, end;
 * uint32_t value;
 * while ((end = ucptrie_getRange(trie, start, NULL, NULL, &value)) >= 0) {
 *     // Work with the range start..end and its value.
 *     start = end + 1;
 * }
 * \endcode
 *
 * @param trie the trie
 * @param start range start
 * @param handleValue a pointer to a function that may modify the trie data value,
 *     or NULL if the values from the trie are to be used unmodified
 * @param context an opaque pointer that is passed on to the handleValue function
 * @param pValue if not NULL, receives the value that every code point start..end has;
 *     may have been modified by handleValue(context, trie value)
 *     if that function pointer is not NULL
 * @return the range end code point, or -1 if start is not a valid code point
 * @draft ICU 63
 */
U_CAPI UChar32 U_EXPORT2
ucptrie_getRange(const UCPTrie *trie, UChar32 start,
                 UCPTrieHandleValue *handleValue, const void *context, uint32_t *pValue);

/**
 * Returns the last code point such that all those from start to there have the same value,
 * with a fixed value for surrogates.
 * Same as ucptrie_getRange() but treats either lead surrogates (U+D800..U+DBFF)
 * or all surrogates (U+D800..U+DFFF)
 * as having the surrValue (without transforming it via handleValue()).
 * See U_IS_LEAD(c) and U_IS_SURROGATE(c).
 *
 * Most users should use ucptrie_getRange() instead.
 *
 * This function is useful for tries that map surrogate code *units* to
 * special values optimized for UTF-16 string processing
 * or for special error behavior for unpaired surrogates,
 * but those values are not to be associated with the lead surrogate code *points*.
 *
 * @param trie the trie
 * @param start range start
 * @param allSurr if TRUE, then the surrValue is used for all surrogates;
 *                if FALSE, it is used only for lead surrogates
 * @param surrValue value for surrogates
 * @param handleValue a pointer to a function that may modify the trie data value,
 *     or NULL if the values from the trie are to be used unmodified
 * @param context an opaque pointer that is passed on to the handleValue function
 * @param pValue if not NULL, receives the value that every code point start..end has;
 *     may have been modified by handleValue(context, trie value)
 *     if that function pointer is not NULL
 * @return the range end code point, or -1 if start is not a valid code point
 * @draft ICU 63
 */
U_CAPI UChar32 U_EXPORT2
ucptrie_getRangeFixedSurr(const UCPTrie *trie, UChar32 start, UBool allSurr, uint32_t surrValue,
                          UCPTrieHandleValue *handleValue, const void *context, uint32_t *pValue);

/**
 * Writes a memory-mappable form of the trie into 32-bit aligned memory.
 * Inverse of ucptrie_openFromBinary().
 *
 * @param trie the trie
 * @param data a pointer to 32-bit-aligned memory to be filled with the trie data;
 *             can be NULL if capacity==0
 * @param capacity the number of bytes available at data, or 0 for pure preflighting
 * @param pErrorCode an in/out ICU UErrorCode;
 *                   U_BUFFER_OVERFLOW_ERROR if the capacity is too small
 * @return the number of bytes written or (if buffer overflow) needed for the trie
 *
 * @see ucptrie_openFromBinary()
 * @draft ICU 63
 */
U_CAPI int32_t U_EXPORT2
ucptrie_toBinary(const UCPTrie *trie, void *data, int32_t capacity, UErrorCode *pErrorCode);

/**
 * Returns a 16-bit trie value for a code point, with range checking.
 * Returns the trie error value if c is not in the range 0..U+10FFFF.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param c (UChar32, in) the input code point
 * @return (uint16_t) The code point's trie value.
 * @draft ICU 63
 */
#define UCPTRIE_FAST_GET16(trie, c) (trie)->data.ptr16[_UCPTRIE_CP_INDEX(trie, 0xffff, c)]

/**
 * Returns a 32-bit trie value for a code point, with range checking.
 * Returns the trie error value if c is not in the range U+0000..U+10FFFF.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param c (UChar32, in) the input code point
 * @return (uint32_t) The code point's trie value.
 * @draft ICU 63
 */
#define UCPTRIE_FAST_GET32(trie, c) (trie)->data.ptr32[_UCPTRIE_CP_INDEX(trie, 0xffff, c)]
#define UCPTRIE_FAST_GET8(trie, c) (trie)->data.ptr8[_UCPTRIE_CP_INDEX(trie, 0xffff, c)]

/**
 * Returns a 16-bit trie value for a code point, with range checking.
 * Returns the trie error value if c is not in the range U+0000..U+10FFFF.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_SMALL
 * @param c (UChar32, in) the input code point
 * @return (uint16_t) The code point's trie value.
 * @draft ICU 63
 */
#define UCPTRIE_SMALL_GET16(trie, c) (trie)->data.ptr16[_UCPTRIE_CP_INDEX(trie, UCPTRIE_SMALL_MAX, c)]
#define UCPTRIE_SMALL_GET32(trie, c) (trie)->data.ptr32[_UCPTRIE_CP_INDEX(trie, UCPTRIE_SMALL_MAX, c)]
#define UCPTRIE_SMALL_GET8(trie, c) (trie)->data.ptr8[_UCPTRIE_CP_INDEX(trie, UCPTRIE_SMALL_MAX, c)]

/**
 * UTF-16: Reads the next code point (UChar32 c, out), post-increments src,
 * and gets a 16-bit value from the trie.
 * Sets the trie error value if c is an unpaired surrogate.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param src (const UChar *, in/out) the source text pointer
 * @param limit (const UChar *, in) the limit pointer for the text, or NULL if NUL-terminated
 * @param c (UChar32, out) variable for the code point
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 * @draft ICU 63
 */
#define UCPTRIE_FAST_U16_NEXT16(trie, src, limit, c, result) \
    _UCPTRIE_FAST_U16_NEXT(trie, data.ptr16, src, limit, c, result)

/**
 * UTF-16: Reads the next code point (UChar32 c, out), post-increments src,
 * and gets a 32-bit value from the trie.
 * Sets the trie error value if c is an unpaired surrogate.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param src (const UChar *, in/out) the source text pointer
 * @param limit (const UChar *, in) the limit pointer for the text, or NULL if NUL-terminated
 * @param c (UChar32, out) variable for the code point
 * @param result (uint32_t, out) uint32_t variable for the trie lookup result
 * @draft ICU 63
 */
#define UCPTRIE_FAST_U16_NEXT32(trie, src, limit, c, result) \
    _UCPTRIE_FAST_U16_NEXT(trie, data.ptr32, src, limit, c, result)
#define UCPTRIE_FAST_U16_NEXT8(trie, src, limit, c, result) \
    _UCPTRIE_FAST_U16_NEXT(trie, data.ptr8, src, limit, c, result)

/**
 * UTF-16: Reads the previous code point (UChar32 c, out), pre-decrements src,
 * and gets a 16-bit value from the trie.
 * Sets the trie error value if c is an unpaired surrogate.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param start (const UChar *, in) the start pointer for the text
 * @param src (const UChar *, in/out) the source text pointer
 * @param c (UChar32, out) variable for the code point
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 * @draft ICU 63
 */
#define UCPTRIE_FAST_U16_PREV16(trie, start, src, c, result) \
    _UCPTRIE_FAST_U16_PREV(trie, data.ptr16, start, src, c, result)

/**
 * UTF-16: Reads the previous code point (UChar32 c, out), pre-decrements src,
 * and gets a 32-bit value from the trie.
 * Sets the trie error value if c is an unpaired surrogate.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param start (const UChar *, in) the start pointer for the text
 * @param src (const UChar *, in/out) the source text pointer
 * @param c (UChar32, out) variable for the code point
 * @param result (uint32_t, out) uint32_t variable for the trie lookup result
 * @draft ICU 63
 */
#define UCPTRIE_FAST_U16_PREV32(trie, start, src, c, result) \
    _UCPTRIE_FAST_U16_PREV(trie, data.ptr32, start, src, c, result)
#define UCPTRIE_FAST_U16_PREV8(trie, start, src, c, result) \
    _UCPTRIE_FAST_U16_PREV(trie, data.ptr8, start, src, c, result)

/**
 * UTF-8: Post-increments src and gets a 16-bit value from the trie.
 * Sets the trie error value for an ill-formed byte sequence.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param src (const char *, in/out) the source text pointer
 * @param limit (const char *, in) the limit pointer for the text (must not be NULL)
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 * @draft ICU 63
 */
#define UCPTRIE_FAST_U8_NEXT16(trie, src, limit, result) \
    _UCPTRIE_FAST_U8_NEXT(trie, data.ptr16, src, limit, result)

/**
 * UTF-8: Post-increments src and gets a 32-bit value from the trie.
 * Sets the trie error value for an ill-formed byte sequence.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param src (const char *, in/out) the source text pointer
 * @param limit (const char *, in) the limit pointer for the text (must not be NULL)
 * @param result (uint16_t, out) uint32_t variable for the trie lookup result
 * @draft ICU 63
 */
#define UCPTRIE_FAST_U8_NEXT32(trie, src, limit, result) \
    _UCPTRIE_FAST_U8_NEXT(trie, data.ptr32, src, limit, result)
#define UCPTRIE_FAST_U8_NEXT8(trie, src, limit, result) \
    _UCPTRIE_FAST_U8_NEXT(trie, data.ptr8, src, limit, result)

/**
 * UTF-8: Pre-decrements src and gets a 16-bit value from the trie.
 * Sets the trie error value for an ill-formed byte sequence.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param start (const char *, in) the start pointer for the text
 * @param src (const char *, in/out) the source text pointer
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 * @draft ICU 63
 */
#define UCPTRIE_FAST_U8_PREV16(trie, start, src, result) \
    _UCPTRIE_FAST_U8_PREV(trie, data.ptr16, start, src, result)

/**
 * UTF-8: Pre-decrements src and gets a 32-bit value from the trie.
 * Sets the trie error value for an ill-formed byte sequence.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param start (const char *, in) the start pointer for the text
 * @param src (const char *, in/out) the source text pointer
 * @param result (uint16_t, out) uint32_t variable for the trie lookup result
 * @draft ICU 63
 */
#define UCPTRIE_FAST_U8_PREV32(trie, start, src, result) \
    _UCPTRIE_FAST_U8_PREV(trie, data.ptr32, start, src, result)
#define UCPTRIE_FAST_U8_PREV8(trie, start, src, result) \
    _UCPTRIE_FAST_U8_PREV(trie, data.ptr8, start, src, result)

/**
 * Returns a 16-bit trie value for an ASCII code point, without range checking.
 *
 * @param trie (const UCPTrie *, in) the trie (of either fast or small type)
 * @param c (UChar32, in) the input code point; must be U+0000..U+007F
 * @return (uint16_t) The code point's trie value.
 * @draft ICU 63
 */
#define UCPTRIE_ASCII_GET16(trie, c) ((trie)->data.ptr16[c])
#define UCPTRIE_ASCII_GET32(trie, c) ((trie)->data.ptr32[c])
#define UCPTRIE_ASCII_GET8(trie, c) ((trie)->data.ptr8[c])

/**
 * Returns a 16-bit trie value for a BMP code point or UTF-16 code unit (U+0000..U+FFFF),
 * without range checking.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param c (UChar32, in) the input code unit, must be U+0000<=c<=U+FFFF
 * @return (uint16_t) The code unit's trie value.
 * @draft ICU 63
 */
#define UCPTRIE_FAST_BMP_GET16(trie, c) ((trie)->data.ptr16[_UCPTRIE_FAST_INDEX(trie, c)])

/**
 * Returns a 32-bit trie value for a BMP code point or UTF-16 code unit (U+0000..U+FFFF),
 * without range checking.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param c (UChar32, in) the input code unit, must be U+0000<=c<=U+FFFF
 * @return (uint32_t) The code unit's trie value.
 * @draft ICU 63
 */
#define UCPTRIE_FAST_BMP_GET32(trie, c) ((trie)->data.ptr32[_UCPTRIE_FAST_INDEX(trie, c)])
#define UCPTRIE_FAST_BMP_GET8(trie, c) ((trie)->data.ptr8[_UCPTRIE_FAST_INDEX(trie, c)])

/**
 * Returns a 16-bit trie value for a supplementary code point (U+10000..U+10FFFF),
 * without range checking.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param c (UChar32, in) the input code point, must be U+10000<=c<=U+10FFFF
 * @return (uint16_t) The code point's trie value.
 * @draft ICU 63
 */
#define UCPTRIE_FAST_SUPP_GET16(trie, c) ((trie)->data.ptr16[_UCPTRIE_SMALL_INDEX(trie, c)])

/**
 * Returns a 32-bit trie value for a supplementary code point (U+10000..U+10FFFF),
 * without range checking.
 *
 * @param trie (const UCPTrie *, in) the trie; must have type UCPTRIE_TYPE_FAST
 * @param c (UChar32, in) the input code point, must be U+10000<=c<=U+10FFFF
 * @return (uint32_t) The code point's trie value.
 * @draft ICU 63
 */
#define UCPTRIE_FAST_SUPP_GET32(trie, c) ((trie)->data.ptr32[_UCPTRIE_SMALL_INDEX(trie, c)])
#define UCPTRIE_FAST_SUPP_GET8(trie, c) ((trie)->data.ptr8[_UCPTRIE_SMALL_INDEX(trie, c)])

/* Internal definitions ----------------------------------------------------- */

/** @internal */
typedef union UCPTrieData {
    /** @internal */
    const void *ptr0;
    /** @internal */
    const uint16_t *ptr16;
    /** @internal */
    const uint32_t *ptr32;
    /** @internal */
    const uint8_t *ptr8;
} UCPTrieData;

/**
 * Internal trie structure definition.
 * Visible only for use by API macros.
 * @internal
 */
struct UCPTrie {
    /** @internal */
    const uint16_t *index;
    /** @internal */
    UCPTrieData data;

    /** @internal */
    int32_t indexLength, dataLength;
    /** Start of the last range which ends at U+10FFFF. @internal */
    UChar32 highStart;
    /** highStart>>12 @internal */
    uint16_t shifted12HighStart;

    /** @internal */
    int8_t type;  // UCPTrieType
    /** @internal */
    int8_t valueWidth;  // UCPTrieValueWidth

    /** padding/reserved @internal */
    uint32_t reserved32;
    /** padding/reserved @internal */
    uint16_t reserved16;

    /**
     * Internal index-3 null block offset.
     * Set to an impossibly high value (e.g., 0xffff) if there is no dedicated index-3 null block.
     * @internal
     */
    uint16_t index3NullOffset;
    /**
     * Internal data null block offset, not shifted.
     * Set to an impossibly high value (e.g., 0xfffff) if there is no dedicated data null block.
     * @internal
     */
    int32_t dataNullOffset;
    /** @internal */
    uint32_t nullValue;

    const char *name;  // TODO
};

/**
 * Internal implementation constants.
 * These are needed for the API macros, but users should not use these directly.
 * @internal
 */
enum {
    /** @internal */
    UCPTRIE_FAST_SHIFT = 6,

    /** Number of entries in a data block for code points below the fast limit. 64=0x40 @internal */
    UCPTRIE_FAST_DATA_BLOCK_LENGTH = 1 << UCPTRIE_FAST_SHIFT,

    /** Mask for getting the lower bits for the in-fast-data-block offset. @internal */
    UCPTRIE_FAST_DATA_MASK = UCPTRIE_FAST_DATA_BLOCK_LENGTH - 1,

    /** @internal */
    UCPTRIE_SMALL_MAX = 0xfff,

    /**
     * Offset from dataLength (to be subtracted) for fetching the
     * value returned for out-of-range code points and ill-formed UTF-8/16.
     * @internal
     */
    UCPTRIE_ERROR_VALUE_NEG_DATA_OFFSET = 1,
    /**
     * Offset from dataLength (to be subtracted) for fetching the
     * value returned for code points highStart..U+10FFFF.
     * @internal
     */
    UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET = 2
};

/* Internal functions and macros -------------------------------------------- */

/** @internal */
U_INTERNAL int32_t U_EXPORT2
ucptrie_internalSmallIndex(const UCPTrie *trie, UChar32 c);

/** @internal */
U_INTERNAL int32_t U_EXPORT2
ucptrie_internalSmallU8Index(const UCPTrie *trie, int32_t lt1, uint8_t t2, uint8_t t3);

/**
 * Internal function for part of the UCPTRIE_FAST_U8_PREVxx() macro implementations.
 * Do not call directly.
 * @internal
 */
U_INTERNAL int32_t U_EXPORT2
ucptrie_internalU8PrevIndex(const UCPTrie *trie, UChar32 c,
                            const uint8_t *start, const uint8_t *src);

/** Internal trie getter for a code point below the fast limit. Returns the data index. @internal */
#define _UCPTRIE_FAST_INDEX(trie, c) \
    (((int32_t)(trie)->index[(c) >> UCPTRIE_FAST_SHIFT]) + ((c) & UCPTRIE_FAST_DATA_MASK))

/** Internal trie getter for a code point at or above the fast limit. Returns the data index. @internal */
#define _UCPTRIE_SMALL_INDEX(trie, c) \
    ((c) >= (trie)->highStart ? \
        (trie)->dataLength - UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET : \
        ucptrie_internalSmallIndex(trie, c))

/**
 * Internal trie getter for a code point, with checking that c is in U+0000..10FFFF.
 * Returns the data index.
 * @internal
 */
#define _UCPTRIE_CP_INDEX(trie, fastMax, c) \
    ((uint32_t)(c) <= (uint32_t)(fastMax) ? \
        _UCPTRIE_FAST_INDEX(trie, c) : \
        (uint32_t)(c) <= 0x10ffff ? \
            _UCPTRIE_SMALL_INDEX(trie, c) : \
            (trie)->dataLength - UCPTRIE_ERROR_VALUE_NEG_DATA_OFFSET)

/** Internal next-post-increment: get the next code point (c) and its data. @internal */
#define _UCPTRIE_FAST_U16_NEXT(trie, data, src, limit, c, result) { \
    (c) = *(src)++; \
    int32_t __index; \
    if (!U16_IS_SURROGATE(c)) { \
        __index = _UCPTRIE_FAST_INDEX(trie, c); \
    } else { \
        uint16_t __c2; \
        if (U16_IS_SURROGATE_LEAD(c) && (src) != (limit) && U16_IS_TRAIL(__c2 = *(src))) { \
            ++(src); \
            (c) = U16_GET_SUPPLEMENTARY((c), __c2); \
            __index = _UCPTRIE_SMALL_INDEX(trie, c); \
        } else { \
            __index = (trie)->dataLength - UCPTRIE_ERROR_VALUE_NEG_DATA_OFFSET; \
        } \
    } \
    (result) = (trie)->data[__index]; \
}

/** Internal pre-decrement-previous: Gets the previous code point (c) and its data. @internal */
#define _UCPTRIE_FAST_U16_PREV(trie, data, start, src, c, result) { \
    (c) = *--(src); \
    int32_t __index; \
    if (!U16_IS_SURROGATE(c)) { \
        __index = _UCPTRIE_FAST_INDEX(trie, c); \
    } else { \
        uint16_t __c2; \
        if (U16_IS_SURROGATE_TRAIL(c) && (src) != (start) && U16_IS_LEAD(__c2 = *((src) - 1))) { \
            --(src); \
            (c) = U16_GET_SUPPLEMENTARY(__c2, (c)); \
            __index = _UCPTRIE_SMALL_INDEX(trie, c); \
        } else { \
            __index = (trie)->dataLength - UCPTRIE_ERROR_VALUE_NEG_DATA_OFFSET; \
        } \
    } \
    (result) = (trie)->data[__index]; \
}

/** Internal UTF-8 next-post-increment: Gets the next code point's data. @internal */
#define _UCPTRIE_FAST_U8_NEXT(trie, data, src, limit, result) { \
    int32_t __lead = (uint8_t)*(src)++; \
    if (!U8_IS_SINGLE(__lead)) { \
        uint8_t __t1, __t2, __t3; \
        if ((src) != (limit) && \
            (__lead >= 0xe0 ? \
                __lead < 0xf0 ?  /* U+0800..U+FFFF except surrogates */ \
                    U8_LEAD3_T1_BITS[__lead &= 0xf] & (1 << ((__t1 = *(src)) >> 5)) && \
                    ++(src) != (limit) && (__t2 = *(src) - 0x80) <= 0x3f && \
                    (__lead = ((int32_t)(trie)->index[(__lead << 6) + (__t1 & 0x3f)]) + __t2, 1) \
                :  /* U+10000..U+10FFFF */ \
                    (__lead -= 0xf0) <= 4 && \
                    U8_LEAD4_T1_BITS[(__t1 = *(src)) >> 4] & (1 << __lead) && \
                    (__lead = (__lead << 6) | (__t1 & 0x3f), ++(src) != (limit)) && \
                    (__t2 = *(src) - 0x80) <= 0x3f && \
                    ++(src) != (limit) && (__t3 = *(src) - 0x80) <= 0x3f && \
                    (__lead = __lead >= (trie)->shifted12HighStart ? \
                        (trie)->dataLength - UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET : \
                        ucptrie_internalSmallU8Index((trie), __lead, __t2, __t3), 1) \
            :  /* U+0080..U+07FF */ \
                __lead >= 0xc2 && (__t1 = *(src) - 0x80) <= 0x3f && \
                (__lead = (trie)->index[__lead & 0x1f] + __t1, 1))) { \
            ++(src); \
        } else { \
            __lead = (trie)->dataLength - UCPTRIE_ERROR_VALUE_NEG_DATA_OFFSET;  /* ill-formed*/ \
        } \
    } \
    (result) = (trie)->data[__lead]; \
}

/** Internal UTF-8 pre-decrement-previous: get the previous code point's data. @internal */
#define _UCPTRIE_FAST_U8_PREV(trie, data, start, src, result) { \
    int32_t __index = (uint8_t)*--(src); \
    if (!U8_IS_SINGLE(__index)) { \
        __index = ucptrie_internalU8PrevIndex((trie), __index, (const uint8_t *)(start), \
                                                              (const uint8_t *)(src)); \
        (src) -= __index & 7; \
        __index >>= 3; \
    } \
    (result) = (trie)->data[__index]; \
}

U_CDECL_END

#endif
