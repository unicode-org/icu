// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3.h (modified from utrie2.h)
// created: 2017dec29 Markus W. Scherer

#ifndef __UTRIE3_H__
#define __UTRIE3_H__

#include "unicode/utypes.h"
#include "unicode/localpointer.h"
#include "unicode/utf8.h"
#include "putilimp.h"
#include "udataswp.h"

U_CDECL_BEGIN

/**
 * \file
 *
 * This is a common implementation of a Unicode trie.
 * It is a kind of compressed, serializable table of 16- or 32-bit values associated with
 * Unicode code points (0..U+10ffff). (A map from code points to integers.)
 *
 * This is the third common version of a Unicode trie (hence the name UTrie3).
 * For details see https://sites.google.com/site/icusite/design/struct/utrie
 */

/**
 * Trie structure.
 * Use only with public API macros and functions.
 */
struct UTrie3;
typedef struct UTrie3 UTrie3;

/* Public UTrie3 API functions: read-only access ---------------------------- */

/**
 * Selectors for the type of a UTrie3.
 * Different trade-offs for size vs. speed.
 */
enum UTrie3Type {
    /** Fast/simple/larger BMP data structure. Use functions and "fast" macros. */
    UTRIE3_TYPE_FAST,
    /** Small/slower BMP data structure. Use functions and "small" macros. */
    UTRIE3_TYPE_SMALL
};
typedef enum UTrie3Type UTrie3Type;

/**
 * Selectors for the width of a UTrie3 data value.
 */
enum UTrie3ValueBits {
    // TODO: 8
    /** 16 bits per UTrie3 data value. */
    UTRIE3_16_VALUE_BITS,  // TODO: move _16 to the end
    /** 32 bits per UTrie3 data value. */
    UTRIE3_32_VALUE_BITS
};
typedef enum UTrie3ValueBits UTrie3ValueBits;

/**
 * Open a frozen trie from its serialized from, stored in 32-bit-aligned memory.
 * Inverse of utrie3_serialize().
 * The memory must remain valid and unchanged as long as the trie is used.
 * You must utrie3_close() the trie once you are done using it.
 *
 * @param valueBits selects the data entry size; results in an
 *                  U_INVALID_FORMAT_ERROR if it does not match the serialized form
 * @param data a pointer to 32-bit-aligned memory containing the serialized form of a UTrie3
 * @param length the number of bytes available at data;
 *               can be more than necessary
 * @param pActualLength receives the actual number of bytes at data taken up by the trie data;
 *                      can be NULL
 * @param pErrorCode an in/out ICU UErrorCode
 * @return the unserialized trie
 *
 * @see utrie3bld_open
 * @see utrie3_serialize
 */
U_CAPI UTrie3 * U_EXPORT2
utrie3_openFromSerialized(UTrie3Type type, UTrie3ValueBits valueBits,
                          const void *data, int32_t length, int32_t *pActualLength,
                          UErrorCode *pErrorCode);

/**
 * Close a trie and release associated memory.
 *
 * @param trie the trie
 */
U_CAPI void U_EXPORT2
utrie3_close(UTrie3 *trie);

#if U_SHOW_CPLUSPLUS_API

U_NAMESPACE_BEGIN

/**
 * \class LocalUTrie3Pointer
 * "Smart pointer" class, closes a UTrie3 via utrie3_close().
 * For most methods see the LocalPointerBase base class.
 *
 * @see LocalPointerBase
 * @see LocalPointer
 * @draft ICU 62
 */
U_DEFINE_LOCAL_OPEN_POINTER(LocalUTrie3Pointer, UTrie3, utrie3_close);

U_NAMESPACE_END

#endif

/**
 * Get a value from a code point as stored in the trie.
 * Easier to use than UTRIE3_GET16() and UTRIE3_GET32() but slower.
 * Easier to use because, unlike the macros, this function works on all UTrie3
 * objects, frozen or not, holding 16-bit or 32-bit data values.
 *
 * @param trie the trie
 * @param c the code point
 * @return the value
 */
U_CAPI uint32_t U_EXPORT2
utrie3_get(const UTrie3 *trie, UChar32 c);

/**
 * Callback function type: Modifies a trie value.
 * Optionally called by utrie3_getRange() or utrie3bld_getRange().
 * The modified value will be returned by the getRange function.
 *
 * Can be used to ignore some of the value bits, return a value index by the trie value, etc.
 *
 * @param context an opaque pointer, as passed into the getRange function
 * @param value a value from the trie
 * @return the modified value
 */
typedef uint32_t U_CALLCONV
UTrie3HandleValue(const void *context, uint32_t value);

/**
 * Returns the last code point such that all those from start to there have the same value.
 * Can be used to efficiently iterate over all same-value ranges in a trie.
 * Do not modify the trie during the enumeration.  TODO
 *
 * For each entry in the trie, the value to be delivered is passed through
 * the UTrie3HandleValue function.
 * The value is unchanged if that function pointer is NULL.
 *
 * Example:
 * \code
 * UChar32 start = 0, end;
 * uint32_t value;
 * while ((end = utrie3_getRange(trie, start, NULL, NULL, &value)) >= 0) {
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
utrie3_getRange(const UTrie3 *trie, UChar32 start,
                UTrie3HandleValue *handleValue, const void *context, uint32_t *pValue);

/**
 * Returns the last code point such that all those from start to there have the same value,
 * with a fixed value for surrogates.
 * Same as utrie3_getRange() but treats either lead surrogates (U+D800..U+DBFF)
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
ucptrie_getRangeFixedSurr(const UTrie3 *trie, UChar32 start, UBool allSurr, uint32_t surrValue,
                          UTrie3HandleValue *handleValue, const void *context, uint32_t *pValue);

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
 * @see utrie3_openFromSerialized()
 */
U_CAPI int32_t U_EXPORT2
utrie3_serialize(const UTrie3 *trie,
                 void *data, int32_t capacity,
                 UErrorCode *pErrorCode);

/* Public UTrie3 API: miscellaneous functions ------------------------------- */

/**
 * Get the UTrie version from 32-bit-aligned memory containing the serialized form
 * of either a UTrie (version 1) or a UTrie3 (version 2).
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
utrie3_getVersion(const void *data, int32_t length, UBool anyEndianOk);

/**
 * Swap a serialized UTrie3.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
utrie3_swap(const UDataSwapper *ds,
            const void *inData, int32_t length, void *outData,
            UErrorCode *pErrorCode);

/**
 * Swap a serialized UTrie or UTrie3. TODO
 * @internal
 */
U_CAPI int32_t U_EXPORT2
utrie3_swapAnyVersion(const UDataSwapper *ds,
                      const void *inData, int32_t length, void *outData,
                      UErrorCode *pErrorCode);

/* Public UTrie3 API macros ------------------------------------------------- */

/**
 * Return a 16-bit trie value from a code point, with range checking.
 * Returns trie->errorValue if c is not in the range 0..U+10ffff.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code point
 * @return (uint16_t) The code point's trie value.
 */
#define UTRIE3_GET16(trie, c) (trie)->data16[_UTRIE3_INDEX_FROM_CP(trie, 0xffff, c)]

/**
 * Return a 32-bit trie value from a code point, with range checking.
 * Returns trie->errorValue if c is not in the range 0..U+10ffff.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code point
 * @return (uint32_t) The code point's trie value.
 */
#define UTRIE3_GET32(trie, c) (trie)->data32[_UTRIE3_INDEX_FROM_CP(trie, 0xffff, c)]

// TODO: docs
#define UTRIE3_SMALL_GET16(trie, c) (trie)->data16[_UTRIE3_INDEX_FROM_CP(trie, UTRIE3_SMALL_MAX, c)]
#define UTRIE3_SMALL_GET32(trie, c) (trie)->data32[_UTRIE3_INDEX_FROM_CP(trie, UTRIE3_SMALL_MAX, c)]

/**
 * UTF-16: Get the next code point (UChar32 c, out), post-increment src,
 * and get a 16-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param src (const UChar *, in/out) the source text pointer
 * @param limit (const UChar *, in) the limit pointer for the text, or NULL if NUL-terminated
 * @param c (UChar32, out) variable for the code point
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UTRIE3_U16_NEXT16(trie, src, limit, c, result) _UTRIE3_U16_NEXT(trie, data16, src, limit, c, result)

/**
 * UTF-16: Get the next code point (UChar32 c, out), post-increment src,
 * and get a 32-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param src (const UChar *, in/out) the source text pointer
 * @param limit (const UChar *, in) the limit pointer for the text, or NULL if NUL-terminated
 * @param c (UChar32, out) variable for the code point
 * @param result (uint32_t, out) uint32_t variable for the trie lookup result
 */
#define UTRIE3_U16_NEXT32(trie, src, limit, c, result) _UTRIE3_U16_NEXT(trie, data32, src, limit, c, result)

/**
 * UTF-16: Get the previous code point (UChar32 c, out), pre-decrement src,
 * and get a 16-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param start (const UChar *, in) the start pointer for the text
 * @param src (const UChar *, in/out) the source text pointer
 * @param c (UChar32, out) variable for the code point
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UTRIE3_U16_PREV16(trie, start, src, c, result) _UTRIE3_U16_PREV(trie, data16, start, src, c, result)

/**
 * UTF-16: Get the previous code point (UChar32 c, out), pre-decrement src,
 * and get a 32-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param start (const UChar *, in) the start pointer for the text
 * @param src (const UChar *, in/out) the source text pointer
 * @param c (UChar32, out) variable for the code point
 * @param result (uint32_t, out) uint32_t variable for the trie lookup result
 */
#define UTRIE3_U16_PREV32(trie, start, src, c, result) _UTRIE3_U16_PREV(trie, data32, start, src, c, result)

/**
 * UTF-8: Post-increment src and get a 16-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param src (const char *, in/out) the source text pointer
 * @param limit (const char *, in) the limit pointer for the text (must not be NULL)
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UTRIE3_U8_NEXT16(trie, src, limit, result) _UTRIE3_U8_NEXT(trie, data16, src, limit, result)

/**
 * UTF-8: Post-increment src and get a 32-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param src (const char *, in/out) the source text pointer
 * @param limit (const char *, in) the limit pointer for the text (must not be NULL)
 * @param result (uint16_t, out) uint32_t variable for the trie lookup result
 */
#define UTRIE3_U8_NEXT32(trie, src, limit, result) _UTRIE3_U8_NEXT(trie, data32, src, limit, result)

/**
 * UTF-8: Pre-decrement src and get a 16-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param start (const char *, in) the start pointer for the text
 * @param src (const char *, in/out) the source text pointer
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UTRIE3_U8_PREV16(trie, start, src, result) _UTRIE3_U8_PREV(trie, data16, start, src, result)

/**
 * UTF-8: Pre-decrement src and get a 32-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param start (const char *, in) the start pointer for the text
 * @param src (const char *, in/out) the source text pointer
 * @param result (uint16_t, out) uint32_t variable for the trie lookup result
 */
#define UTRIE3_U8_PREV32(trie, start, src, result) _UTRIE3_U8_PREV(trie, data32, start, src, result)

/* Public UTrie3 API: optimized UTF-16 access ------------------------------- */

/*
 * TODO
 * The following function and macros are used for highly optimized UTF-16
 * text processing. The UTRIE3_U16_NEXTxy() macros do not depend on these.
 *
 * UTF-16 text processing can be optimized by detecting surrogate pairs and
 * assembling supplementary code points only when there is non-trivial data
 * available.
 *
 * At build-time, use utrie3bld_getRange() starting from U+10000 to see if there
 * is non-trivial data for any of the supplementary code points
 * associated with a lead surrogate.
 * If so, then set a special (application-specific) value for the
 * lead surrogate.
 *
 * At runtime, use UTRIE3_GET16_FROM_BMP() or
 * UTRIE3_GET32_FROM_BMP() per code unit. If there is non-trivial
 * data and the code unit is a lead surrogate, then check if a trail surrogate
 * follows. If so, assemble the supplementary code point with
 * U16_GET_SUPPLEMENTARY() and look up its value with UTRIE3_GET16_FROM_SUPP()
 * or UTRIE3_GET32_FROM_SUPP(); otherwise deal with the unpaired surrogate in some way.
 *
 * If there is only trivial data for lead and trail surrogates, then processing
 * can often skip them. For example, in normalization or case mapping
 * all characters that do not have any mappings are simply copied as is.
 */

// TODO: docs
#define UTRIE3_GET16_FROM_ASCII(trie, c) ((trie)->data16[c])
#define UTRIE3_GET32_FROM_ASCII(trie, c) ((trie)->data32[c])

/**
 * Returns a 16-bit trie value from a BMP code point or UTF-16 code unit (0..U+ffff).
 * Same as UTRIE3_GET16() if c is a BMP code point, but smaller and faster.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code unit, must be 0<=c<=U+ffff
 * @return (uint16_t) The code unit's trie value.
 */
#define UTRIE3_GET16_FROM_BMP(trie, c) ((trie)->data16[_UTRIE3_INDEX_FROM_BMP(trie, c)])
// TODO: UTRIE3_FAST_...

/**
 * Returns a 32-bit trie value from a BMP code point or UTF-16 code unit (0..U+ffff).
 * Same as UTRIE3_GET32() if c is a BMP code point, but smaller and faster.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code unit, must be 0<=c<=U+ffff
 * @return (uint32_t) The code unit's trie value.
 */
#define UTRIE3_GET32_FROM_BMP(trie, c) ((trie)->data32[_UTRIE3_INDEX_FROM_BMP(trie, c)])

/**
 * Return a 16-bit trie value from a supplementary code point (U+10000..U+10ffff).
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code point, must be U+10000<=c<=U+10ffff
 * @return (uint16_t) The code point's trie value.
 */
#define UTRIE3_GET16_FROM_SUPP(trie, c) ((trie)->data16[_UTRIE3_INDEX_FROM_SUPP(trie, c)])

/**
 * Return a 32-bit trie value from a supplementary code point (U+10000..U+10ffff).
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code point, must be U+10000<=c<=U+10ffff
 * @return (uint32_t) The code point's trie value.
 */
#define UTRIE3_GET32_FROM_SUPP(trie, c) ((trie)->data32[_UTRIE3_INDEX_FROM_SUPP(trie, c)])

/* Internal definitions ----------------------------------------------------- */

/*
 * Trie structure definition.
 *
 * Either the data table is 16 bits wide and accessed via the index
 * pointer, with each index item increased by indexLength;
 * in this case, data32==NULL, and data16 is used for direct ASCII access.
 *
 * Or the data table is 32 bits wide and accessed via the data32 pointer.
 */
struct UTrie3 {
    /* protected: used by macros and functions for reading values */
    const uint16_t *index;
    const uint16_t *data16;     /* for fast UTF-8 ASCII access, if 16b data */
    const uint32_t *data32;     /* NULL if 16b data is used via index */

    int32_t indexLength, dataLength;
    /** Start of the last range which ends at U+10ffff, and its value. */
    UChar32 highStart;
    uint16_t shifted12HighStart;  // highStart>>12

    UTrie3Type type;

    /**
     * Index-2 null block offset.
     * Set to an impossibly high value (e.g., 0xffff) if there is no dedicated index-2 null block.
     */
    uint16_t index2NullOffset;
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
    UTRIE3_BMP_SHIFT = 6,

    /** Number of entries in a BMP data block. 64=0x40 */
    UTRIE3_BMP_DATA_BLOCK_LENGTH = 1 << UTRIE3_BMP_SHIFT,

    /** Mask for getting the lower bits for the in-BMP-data-block offset. */
    UTRIE3_BMP_DATA_MASK = UTRIE3_BMP_DATA_BLOCK_LENGTH - 1,

    UTRIE3_SMALL_MAX = 0xfff,

    /** TODO docs */
    /** TODO Value returned for out-of-range code points and ill-formed UTF-8/16. */
    UTRIE3_ERROR_VALUE_NEG_DATA_OFFSET = 1,
    /** TODO Value for code points highStart..U+10FFFF. */
    UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET = 2
};

/* Internal functions and macros -------------------------------------------- */

/** TODO */
U_INTERNAL int32_t U_EXPORT2
utrie3_internalSmallIndex(const UTrie3 *trie, UChar32 c);

/** TODO */
U_INTERNAL int32_t U_EXPORT2
utrie3_internalSmallIndexFromU8(const UTrie3 *trie, int32_t lt1, uint8_t t2, uint8_t t3);

/**
 * Internal function for part of the UTRIE3_U8_PREVxx() macro implementations.
 * Do not call directly.
 * @internal
 */
U_INTERNAL int32_t U_EXPORT2
utrie3_internalU8PrevIndex(const UTrie3 *trie, UChar32 c,
                           const uint8_t *start, const uint8_t *src);

/** Internal trie getter from a BMP code point. Returns the data index. */
#define _UTRIE3_INDEX_FROM_BMP(trie, c) \
    (((int32_t)(trie)->index[(c) >> UTRIE3_BMP_SHIFT]) + ((c) & UTRIE3_BMP_DATA_MASK))

/** Internal trie getter from a supplementary code point. Returns the data index. */
#define _UTRIE3_INDEX_FROM_SUPP(trie, c) \
    ((c) >= (trie)->highStart ? \
        (trie)->dataLength - UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET : \
        utrie3_internalSmallIndex(trie, c))

/**
 * Internal trie getter from a code point, with checking that c is in 0..10FFFF.
 * Returns the data index.
 */
#define _UTRIE3_INDEX_FROM_CP(trie, fastMax, c) \
    ((uint32_t)(c) <= (uint32_t)(fastMax) ? \
        _UTRIE3_INDEX_FROM_BMP(trie, c) : \
        (uint32_t)(c) <= 0x10ffff ? \
            _UTRIE3_INDEX_FROM_SUPP(trie, c) : \
            (trie)->dataLength - UTRIE3_ERROR_VALUE_NEG_DATA_OFFSET)

/** Internal next-post-increment: get the next code point (c) and its data. */
#define _UTRIE3_U16_NEXT(trie, data, src, limit, c, result) { \
    (c) = *(src)++; \
    int32_t __index; \
    if (!U16_IS_SURROGATE(c)) { \
        __index = _UTRIE3_INDEX_FROM_BMP(trie, c); \
    } else { \
        uint16_t __c2; \
        if (U16_IS_SURROGATE_LEAD(c) && (src) != (limit) && U16_IS_TRAIL(__c2 = *(src))) { \
            ++(src); \
            (c) = U16_GET_SUPPLEMENTARY((c), __c2); \
            __index = _UTRIE3_INDEX_FROM_SUPP(trie, c); \
        } else { \
            __index = (trie)->dataLength - UTRIE3_ERROR_VALUE_NEG_DATA_OFFSET; \
        } \
    } \
    (result) = (trie)->data[__index]; \
}

/** Internal pre-decrement-previous: get the previous code point (c) and its data */
#define _UTRIE3_U16_PREV(trie, data, start, src, c, result) { \
    (c) = *--(src); \
    int32_t __index; \
    if (!U16_IS_SURROGATE(c)) { \
        __index = _UTRIE3_INDEX_FROM_BMP(trie, c); \
    } else { \
        uint16_t __c2; \
        if (U16_IS_SURROGATE_TRAIL(c) && (src) != (start) && U16_IS_LEAD(__c2 = *((src) - 1))) { \
            --(src); \
            (c) = U16_GET_SUPPLEMENTARY(__c2, (c)); \
            __index = _UTRIE3_INDEX_FROM_SUPP(trie, c); \
        } else { \
            __index = (trie)->dataLength - UTRIE3_ERROR_VALUE_NEG_DATA_OFFSET; \
        } \
    } \
    (result) = (trie)->data[__index]; \
}

/** Internal UTF-8 next-post-increment: get the next code point's data. */
#define _UTRIE3_U8_NEXT(trie, data, src, limit, result) { \
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
                        (trie)->dataLength - UTRIE3_HIGH_VALUE_NEG_DATA_OFFSET : \
                        utrie3_internalSmallIndexFromU8((trie), __lead, __t2, __t3), 1) \
            :  /* U+0080..U+07FF */ \
                __lead >= 0xc2 && (__t1 = *(src) - 0x80) <= 0x3f && \
                (__lead = (trie)->index[__lead & 0x1f] + __t1, 1))) { \
            ++(src); \
        } else { \
            __lead = (trie)->dataLength - UTRIE3_ERROR_VALUE_NEG_DATA_OFFSET;  /* ill-formed*/ \
        } \
    } \
    (result) = (trie)->data[__lead]; \
}

/** Internal UTF-8 pre-decrement-previous: get the previous code point's data. */
#define _UTRIE3_U8_PREV(trie, data, start, src, result) { \
    int32_t __index = (uint8_t)*--(src); \
    if (!U8_IS_SINGLE(__index)) { \
        __index = utrie3_internalU8PrevIndex((trie), __index, (const uint8_t *)(start), \
                                                              (const uint8_t *)(src)); \
        (src) -= __index & 7; \
        __index >>= 3; \
    } \
    (result) = (trie)->data[__index]; \
}

U_CDECL_END

#endif
