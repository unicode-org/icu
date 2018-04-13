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
 * This defines a Unicode code point trie.
 * It is a fast, reasonably compact, map from Unicode code points (0..U+10ffff) to integer values.
 *
 * For details see https://sites.google.com/site/icusite/design/struct/utrie
 */

/**
 * Trie structure.
 * Use only with public API macros and functions.
 */
struct UCPTrie;
typedef struct UCPTrie UCPTrie;

/* Public UCPTrie API functions: read-only access ---------------------------- */

/**
 * Selectors for the type of a UCPTrie.
 * Different trade-offs for size vs. speed.
 */
enum UCPTrieType {
    /** Fast/simple/larger BMP data structure. Use functions and "fast" macros. */
    UCPTRIE_TYPE_FAST,
    /** Small/slower BMP data structure. Use functions and "small" macros. */
    UCPTRIE_TYPE_SMALL
};
typedef enum UCPTrieType UCPTrieType;

/**
 * Selectors for the width of a UCPTrie data value.
 */
enum UCPTrieValueBits {
    // TODO: 8
    /** 16 bits per UCPTrie data value. */
    UCPTRIE_VALUE_BITS_16,
    /** 32 bits per UCPTrie data value. */
    UCPTRIE_VALUE_BITS_32
};
typedef enum UCPTrieValueBits UCPTrieValueBits;

/**
 * Open a frozen trie from its serialized from, stored in 32-bit-aligned memory.
 * Inverse of ucptrie_toBinary().
 * The memory must remain valid and unchanged as long as the trie is used.
 * You must ucptrie_close() the trie once you are done using it.
 *
 * @param valueBits selects the data entry size; results in an
 *                  U_INVALID_FORMAT_ERROR if it does not match the serialized form
 * @param data a pointer to 32-bit-aligned memory containing the serialized form of a UCPTrie
 * @param length the number of bytes available at data;
 *               can be more than necessary
 * @param pActualLength receives the actual number of bytes at data taken up by the trie data;
 *                      can be NULL
 * @param pErrorCode an in/out ICU UErrorCode
 * @return the unserialized trie
 *
 * @see ucptriebld_open
 * @see ucptrie_toBinary
 */
U_CAPI UCPTrie * U_EXPORT2
ucptrie_openFromBinary(UCPTrieType type, UCPTrieValueBits valueBits,
                       const void *data, int32_t length, int32_t *pActualLength,
                       UErrorCode *pErrorCode);

/**
 * Close a trie and release associated memory.
 *
 * @param trie the trie
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
 * @draft ICU 62
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalUCPTriePointer, UCPTrie, ucptrie_close);

U_NAMESPACE_END

#endif

/**
 * Get a value from a code point as stored in the trie.
 * Easier to use than UCPTRIE_FAST_GET16() and similar macros but slower.
 * Easier to use because, unlike the macros, this function works on all UCPTrie
 * objects, frozen or not, holding 16-bit or 32-bit data values.
 *
 * @param trie the trie
 * @param c the code point
 * @return the value
 */
U_CAPI uint32_t U_EXPORT2
ucptrie_get(const UCPTrie *trie, UChar32 c);

/**
 * Callback function type: Modifies a trie value.
 * Optionally called by ucptrie_getRange() or ucptriebld_getRange().
 * The modified value will be returned by the getRange function.
 *
 * Can be used to ignore some of the value bits,
 * return a value index extracted from the trie value, etc.
 *
 * @param context an opaque pointer, as passed into the getRange function
 * @param value a value from the trie
 * @return the modified value
 */
typedef uint32_t U_CALLCONV
UCPTrieHandleValue(const void *context, uint32_t value);

/**
 * Returns the last code point such that all those from start to there have the same value.
 * Can be used to efficiently iterate over all same-value ranges in a trie.
 * Do not modify the trie during the enumeration.  TODO
 *
 * For each entry in the trie, the value to be delivered is passed through
 * the UCPTrieHandleValue function.
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
 * @param trie a pointer to a trie
 * @param start range start
 * @param handleValue a pointer to a function that may modify the trie entry value,
 *     or NULL if the values from the trie are to be used directly
 * @param context an opaque pointer that is passed on to the handleValue function
 * @param pValue if not NULL, receives the value that every code point start..end has;
 *     optionally modified by handleValue(context, trie value)
 * @return the range end code point, or -1 if start is not a valid code point
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
 * This is useful for tries that map surrogate code *units* to
 * special values optimized for UTF-16 string processing
 * or for special error behavior for unpaired surrogates,
 * but those values are not to be associated with the lead surrogate code *points*.
 *
 * @param trie a pointer to a trie
 * @param start range start
 * @param allSurr if TRUE, then the surrValue is used for all surrogates;
 *                if FALSE, it is used only for lead surrogates
 * @param surrValue value for surrogates
 * @param handleValue a pointer to a function that may modify the trie entry value,
 *     or NULL if the values from the trie are to be used directly
 * @param context an opaque pointer that is passed on to the handleValue function
 * @param pValue if not NULL, receives the value that every code point start..end has;
 *     optionally modified by handleValue(context, trie value)
 * @return the range end code point, or -1 if start is not a valid code point
 */
U_CAPI UChar32 U_EXPORT2
ucptrie_getRangeFixedSurr(const UCPTrie *trie, UChar32 start, UBool allSurr, uint32_t surrValue,
                          UCPTrieHandleValue *handleValue, const void *context, uint32_t *pValue);

/**
 * Serialize a frozen trie into 32-bit aligned memory.
 * If the trie is not frozen, then the function returns with a U_ILLEGAL_ARGUMENT_ERROR.
 * A trie can be serialized multiple times.
 *
 * @param trie the frozen trie
 * @param data a pointer to 32-bit-aligned memory to be filled with the trie data,
 *             can be NULL if capacity==0
 * @param capacity the number of bytes available at data,
 *                 or 0 for preflighting
 * @param pErrorCode an in/out ICU UErrorCode; among other possible error codes:
 * - U_BUFFER_OVERFLOW_ERROR if the data storage block is too small for serialization
 * - U_ILLEGAL_ARGUMENT_ERROR if the trie is not frozen or the data and capacity
 *                            parameters are bad
 * @return the number of bytes written or needed for the trie
 *
 * @see ucptrie_openFromBinary()
 */
U_CAPI int32_t U_EXPORT2
ucptrie_toBinary(const UCPTrie *trie, void *data, int32_t capacity, UErrorCode *pErrorCode);

/* Public UCPTrie API: miscellaneous functions ------------------------------- */

/**
 * Get the UTrie version from 32-bit-aligned memory containing the serialized form
 * of either a UTrie (version 1) or a UCPTrie (version 2).
 *
 * @param data a pointer to 32-bit-aligned memory containing the serialized form
 *             of a UTrie, version 1 or 2
 * @param length the number of bytes available at data;
 *               can be more than necessary (see return value)
 * @param anyEndianOk If FALSE, only platform-endian serialized forms are recognized.
 *                    If TRUE, opposite-endian serialized forms are recognized as well.
 * @return the UTrie version of the serialized form, or 0 if it is not
 *         recognized as a serialized UTrie
 */
U_CAPI int32_t U_EXPORT2
ucptrie_getVersion(const void *data, int32_t length, UBool anyEndianOk);

/**
 * Swap a serialized UCPTrie.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
ucptrie_swap(const UDataSwapper *ds,
             const void *inData, int32_t length, void *outData,
             UErrorCode *pErrorCode);

/**
 * Swap a serialized UTrie or UCPTrie. TODO
 * @internal
 */
U_CAPI int32_t U_EXPORT2
ucptrie_swapAnyVersion(const UDataSwapper *ds,
                       const void *inData, int32_t length, void *outData,
                       UErrorCode *pErrorCode);

/* Public UCPTrie API macros ------------------------------------------------- */

/**
 * Return a 16-bit trie value from a code point, with range checking.
 * Returns trie->errorValue if c is not in the range 0..U+10ffff.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param c (UChar32, in) the input code point
 * @return (uint16_t) The code point's trie value.
 */
#define UCPTRIE_FAST_GET16(trie, c) (trie)->data16[_UCPTRIE_INDEX_FROM_CP(trie, 0xffff, c)]

/**
 * Return a 32-bit trie value from a code point, with range checking.
 * Returns trie->errorValue if c is not in the range 0..U+10ffff.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param c (UChar32, in) the input code point
 * @return (uint32_t) The code point's trie value.
 */
#define UCPTRIE_FAST_GET32(trie, c) (trie)->data32[_UCPTRIE_INDEX_FROM_CP(trie, 0xffff, c)]

// TODO: docs
#define UCPTRIE_SMALL_GET16(trie, c) (trie)->data16[_UCPTRIE_INDEX_FROM_CP(trie, UCPTRIE_SMALL_MAX, c)]
#define UCPTRIE_SMALL_GET32(trie, c) (trie)->data32[_UCPTRIE_INDEX_FROM_CP(trie, UCPTRIE_SMALL_MAX, c)]

/**
 * UTF-16: Get the next code point (UChar32 c, out), post-increment src,
 * and get a 16-bit value from the trie.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param src (const UChar *, in/out) the source text pointer
 * @param limit (const UChar *, in) the limit pointer for the text, or NULL if NUL-terminated
 * @param c (UChar32, out) variable for the code point
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UCPTRIE_FAST_U16_NEXT16(trie, src, limit, c, result) \
    _UCPTRIE_FAST_U16_NEXT(trie, data16, src, limit, c, result)

/**
 * UTF-16: Get the next code point (UChar32 c, out), post-increment src,
 * and get a 32-bit value from the trie.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param src (const UChar *, in/out) the source text pointer
 * @param limit (const UChar *, in) the limit pointer for the text, or NULL if NUL-terminated
 * @param c (UChar32, out) variable for the code point
 * @param result (uint32_t, out) uint32_t variable for the trie lookup result
 */
#define UCPTRIE_FAST_U16_NEXT32(trie, src, limit, c, result) \
    _UCPTRIE_FAST_U16_NEXT(trie, data32, src, limit, c, result)

/**
 * UTF-16: Get the previous code point (UChar32 c, out), pre-decrement src,
 * and get a 16-bit value from the trie.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param start (const UChar *, in) the start pointer for the text
 * @param src (const UChar *, in/out) the source text pointer
 * @param c (UChar32, out) variable for the code point
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UCPTRIE_FAST_U16_PREV16(trie, start, src, c, result) \
    _UCPTRIE_FAST_U16_PREV(trie, data16, start, src, c, result)

/**
 * UTF-16: Get the previous code point (UChar32 c, out), pre-decrement src,
 * and get a 32-bit value from the trie.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param start (const UChar *, in) the start pointer for the text
 * @param src (const UChar *, in/out) the source text pointer
 * @param c (UChar32, out) variable for the code point
 * @param result (uint32_t, out) uint32_t variable for the trie lookup result
 */
#define UCPTRIE_FAST_U16_PREV32(trie, start, src, c, result) \
    _UCPTRIE_FAST_U16_PREV(trie, data32, start, src, c, result)

/**
 * UTF-8: Post-increment src and get a 16-bit value from the trie.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param src (const char *, in/out) the source text pointer
 * @param limit (const char *, in) the limit pointer for the text (must not be NULL)
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UCPTRIE_FAST_U8_NEXT16(trie, src, limit, result) \
    _UCPTRIE_FAST_U8_NEXT(trie, data16, src, limit, result)

/**
 * UTF-8: Post-increment src and get a 32-bit value from the trie.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param src (const char *, in/out) the source text pointer
 * @param limit (const char *, in) the limit pointer for the text (must not be NULL)
 * @param result (uint16_t, out) uint32_t variable for the trie lookup result
 */
#define UCPTRIE_FAST_U8_NEXT32(trie, src, limit, result) \
    _UCPTRIE_FAST_U8_NEXT(trie, data32, src, limit, result)

/**
 * UTF-8: Pre-decrement src and get a 16-bit value from the trie.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param start (const char *, in) the start pointer for the text
 * @param src (const char *, in/out) the source text pointer
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UCPTRIE_FAST_U8_PREV16(trie, start, src, result) \
    _UCPTRIE_FAST_U8_PREV(trie, data16, start, src, result)

/**
 * UTF-8: Pre-decrement src and get a 32-bit value from the trie.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param start (const char *, in) the start pointer for the text
 * @param src (const char *, in/out) the source text pointer
 * @param result (uint16_t, out) uint32_t variable for the trie lookup result
 */
#define UCPTRIE_FAST_U8_PREV32(trie, start, src, result) \
    _UCPTRIE_FAST_U8_PREV(trie, data32, start, src, result)

/* Public UCPTrie API: optimized UTF-16 access ------------------------------ */

/*
 * TODO
 * The following function and macros are used for highly optimized UTF-16
 * text processing. The UCPTRIE_FAST_U16_NEXTxy() macros do not depend on these.
 *
 * UTF-16 text processing can be optimized by detecting surrogate pairs and
 * assembling supplementary code points only when there is non-trivial data
 * available.
 *
 * At build-time, use ucptriebld_getRange() starting from U+10000 to see if there
 * is non-trivial data for any of the supplementary code points
 * associated with a lead surrogate.
 * If so, then set a special (application-specific) value for the
 * lead surrogate.
 *
 * At runtime, use UCPTRIE_FAST_GET16_FROM_BMP() or
 * UCPTRIE_FAST_GET32_FROM_BMP() per code unit. If there is non-trivial
 * data and the code unit is a lead surrogate, then check if a trail surrogate
 * follows. If so, assemble the supplementary code point with
 * U16_GET_SUPPLEMENTARY() and look up its value with UCPTRIE_FAST_GET16_FROM_SUPP()
 * or UCPTRIE_FAST_GET32_FROM_SUPP(); otherwise deal with the unpaired surrogate in some way.
 *
 * If there is only trivial data for lead and trail surrogates, then processing
 * can often skip them. For example, in normalization or case mapping
 * all characters that do not have any mappings are simply copied as is.
 */

// TODO: docs
#define UCPTRIE_GET16_FROM_ASCII(trie, c) ((trie)->data16[c])
#define UCPTRIE_GET32_FROM_ASCII(trie, c) ((trie)->data32[c])

/**
 * Returns a 16-bit trie value from a BMP code point or UTF-16 code unit (0..U+ffff).
 * Same as UCPTRIE_FAST_GET16() if c is a BMP code point, but smaller and faster.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param c (UChar32, in) the input code unit, must be 0<=c<=U+ffff
 * @return (uint16_t) The code unit's trie value.
 */
#define UCPTRIE_FAST_GET16_FROM_BMP(trie, c) ((trie)->data16[_UCPTRIE_FAST_INDEX(trie, c)])

/**
 * Returns a 32-bit trie value from a BMP code point or UTF-16 code unit (0..U+ffff).
 * Same as UCPTRIE_FAST_GET32() if c is a BMP code point, but smaller and faster.
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param c (UChar32, in) the input code unit, must be 0<=c<=U+ffff
 * @return (uint32_t) The code unit's trie value.
 */
#define UCPTRIE_FAST_GET32_FROM_BMP(trie, c) ((trie)->data32[_UCPTRIE_FAST_INDEX(trie, c)])

/**
 * Return a 16-bit trie value from a supplementary code point (U+10000..U+10ffff).
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param c (UChar32, in) the input code point, must be U+10000<=c<=U+10ffff
 * @return (uint16_t) The code point's trie value.
 */
#define UCPTRIE_FAST_GET16_FROM_SUPP(trie, c) ((trie)->data16[_UCPTRIE_SMALL_INDEX(trie, c)])

/**
 * Return a 32-bit trie value from a supplementary code point (U+10000..U+10ffff).
 *
 * @param trie (const UCPTrie *, in) a frozen trie
 * @param c (UChar32, in) the input code point, must be U+10000<=c<=U+10ffff
 * @return (uint32_t) The code point's trie value.
 */
#define UCPTRIE_FAST_GET32_FROM_SUPP(trie, c) ((trie)->data32[_UCPTRIE_SMALL_INDEX(trie, c)])

/* Internal definitions ----------------------------------------------------- */

/*
 * Trie structure definition.
 */
struct UCPTrie {
    /* protected: used by macros and functions for reading values */
    const uint16_t *index;
    const uint16_t *data16;
    const uint32_t *data32;

    int32_t indexLength, dataLength;
    /** Start of the last range which ends at U+10ffff. */
    UChar32 highStart;
    uint16_t shifted12HighStart;  // highStart>>12

    UCPTrieType type;

    /**
     * Index-3 null block offset.
     * Set to an impossibly high value (e.g., 0xffff) if there is no dedicated index-3 null block.
     */
    uint16_t index3NullOffset;
    /**
     * Data null block offset, not shifted.
     * Set to an impossibly high value (e.g., 0xfffff) if there is no dedicated data null block.
     */
    int32_t dataNullOffset;
    uint32_t nullValue;

    const char *name;  // TODO
};

/**
 * Implementation constants.
 * These are needed for the runtime macros, but users should not use these directly.
 */
enum {
    UCPTRIE_FAST_SHIFT = 6,

    /** Number of entries in a data block for code points below the fast limit. 64=0x40 */
    UCPTRIE_FAST_DATA_BLOCK_LENGTH = 1 << UCPTRIE_FAST_SHIFT,

    /** Mask for getting the lower bits for the in-fast-data-block offset. */
    UCPTRIE_FAST_DATA_MASK = UCPTRIE_FAST_DATA_BLOCK_LENGTH - 1,

    UCPTRIE_SMALL_MAX = 0xfff,

    /** TODO docs */
    /** TODO Value returned for out-of-range code points and ill-formed UTF-8/16. */
    UCPTRIE_ERROR_VALUE_NEG_DATA_OFFSET = 1,
    /** TODO Value for code points highStart..U+10FFFF. */
    UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET = 2
};

/* Internal functions and macros -------------------------------------------- */

/** TODO */
U_INTERNAL int32_t U_EXPORT2
ucptrie_internalSmallIndex(const UCPTrie *trie, UChar32 c);

/** TODO */
U_INTERNAL int32_t U_EXPORT2
ucptrie_internalSmallIndexFromU8(const UCPTrie *trie, int32_t lt1, uint8_t t2, uint8_t t3);

/**
 * Internal function for part of the UCPTRIE_FAST_U8_PREVxx() macro implementations.
 * Do not call directly.
 * @internal
 */
U_INTERNAL int32_t U_EXPORT2
ucptrie_internalU8PrevIndex(const UCPTrie *trie, UChar32 c,
                            const uint8_t *start, const uint8_t *src);

/** Internal trie getter from a code point below the fast limit. Returns the data index. */
#define _UCPTRIE_FAST_INDEX(trie, c) \
    (((int32_t)(trie)->index[(c) >> UCPTRIE_FAST_SHIFT]) + ((c) & UCPTRIE_FAST_DATA_MASK))

/** Internal trie getter from a code point at or above the fast limit. Returns the data index. */
#define _UCPTRIE_SMALL_INDEX(trie, c) \
    ((c) >= (trie)->highStart ? \
        (trie)->dataLength - UCPTRIE_HIGH_VALUE_NEG_DATA_OFFSET : \
        ucptrie_internalSmallIndex(trie, c))

/**
 * Internal trie getter from a code point, with checking that c is in 0..10FFFF.
 * Returns the data index.
 */
#define _UCPTRIE_INDEX_FROM_CP(trie, fastMax, c) \
    ((uint32_t)(c) <= (uint32_t)(fastMax) ? \
        _UCPTRIE_FAST_INDEX(trie, c) : \
        (uint32_t)(c) <= 0x10ffff ? \
            _UCPTRIE_SMALL_INDEX(trie, c) : \
            (trie)->dataLength - UCPTRIE_ERROR_VALUE_NEG_DATA_OFFSET)

/** Internal next-post-increment: get the next code point (c) and its data. */
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

/** Internal pre-decrement-previous: get the previous code point (c) and its data */
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

/** Internal UTF-8 next-post-increment: get the next code point's data. */
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
                        ucptrie_internalSmallIndexFromU8((trie), __lead, __t2, __t3), 1) \
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

/** Internal UTF-8 pre-decrement-previous: get the previous code point's data. */
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
