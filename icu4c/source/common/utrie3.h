// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3.h (modified from utrie2.h)
// created: 2017dec29 Markus W. Scherer

#ifndef __UTRIE3_H__
#define __UTRIE3_H__

#include "unicode/utypes.h"
#include "unicode/utf8.h"
#include "putilimp.h"
#include "udataswp.h"

U_CDECL_BEGIN

/**
 * \file
 *
 * This is a common implementation of a Unicode trie.
 * It is a kind of compressed, serializable table of 16- or 32-bit values associated with
 * Unicode code points (0..0x10ffff). (A map from code points to integers.)
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
 * Selectors for the width of a UTrie3 data value.
 */
enum UTrie3ValueBits {
    /** 16 bits per UTrie3 data value. */
    UTRIE3_16_VALUE_BITS,
    /** 32 bits per UTrie3 data value. */
    UTRIE3_32_VALUE_BITS,
    /** Number of selectors for the width of UTrie3 data values. */
    UTRIE3_COUNT_VALUE_BITS
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
 * @see utrie3_open
 * @see utrie3_serialize
 */
U_CAPI UTrie3 * U_EXPORT2
utrie3_openFromSerialized(UTrie3ValueBits valueBits,
                          const void *data, int32_t length, int32_t *pActualLength,
                          UErrorCode *pErrorCode);

/**
 * Open a frozen, empty "dummy" trie.
 * A dummy trie is an empty trie, used when a real data trie cannot
 * be loaded. Equivalent to calling utrie3_open() and utrie3_freeze(),
 * but without internally creating and compacting/serializing the
 * builder data structure.
 *
 * The trie always returns the initialValue,
 * or the errorValue for out-of-range code points and ill-formed UTF-8/16.
 *
 * You must utrie3_close() the trie once you are done using it.
 *
 * @param valueBits selects the data entry size
 * @param initialValue the initial value that is set for all code points
 * @param errorValue the value for out-of-range code points and ill-formed UTF-8/16
 * @param pErrorCode an in/out ICU UErrorCode
 * @return the dummy trie
 *
 * @see utrie3_openFromSerialized
 * @see utrie3_open
 */
U_CAPI UTrie3 * U_EXPORT2
utrie3_openDummy(UTrie3ValueBits valueBits,
                 uint32_t initialValue, uint32_t errorValue,
                 UErrorCode *pErrorCode);

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
utrie3_get32(const UTrie3 *trie, UChar32 c);

/* enumeration callback types */

/**
 * Callback from utrie3_enum(), extracts a uint32_t value from a
 * trie value. This value will be passed on to the UTrie3EnumRange function.
 *
 * @param context an opaque pointer, as passed into utrie3_enum()
 * @param value a value from the trie
 * @return the value that is to be passed on to the UTrie3EnumRange function
 */
typedef uint32_t U_CALLCONV
UTrie3EnumValue(const void *context, uint32_t value);

/**
 * Callback from utrie3_enum(), is called for each contiguous range
 * of code points with the same value as retrieved from the trie and
 * transformed by the UTrie3EnumValue function.
 *
 * The callback function can stop the enumeration by returning FALSE.
 *
 * @param context an opaque pointer, as passed into utrie3_enum()
 * @param start the first code point in a contiguous range with value
 * @param end the last code point in a contiguous range with value (inclusive)
 * @param value the value that is set for all code points in [start..end]
 * @return FALSE to stop the enumeration
 */
typedef UBool U_CALLCONV
UTrie3EnumRange(const void *context, UChar32 start, UChar32 end, uint32_t value);

/**
 * Enumerate efficiently all values in a trie.
 * Do not modify the trie during the enumeration.
 *
 * For each entry in the trie, the value to be delivered is passed through
 * the UTrie3EnumValue function.
 * The value is unchanged if that function pointer is NULL.
 *
 * For each contiguous range of code points with a given (transformed) value,
 * the UTrie3EnumRange function is called.
 *
 * @param trie a pointer to the trie
 * @param enumValue a pointer to a function that may transform the trie entry value,
 *                  or NULL if the values from the trie are to be used directly
 * @param enumRange a pointer to a function that is called for each contiguous range
 *                  of code points with the same (transformed) value
 * @param context an opaque pointer that is passed on to the callback functions
 */
U_CAPI void U_EXPORT2
utrie3_enum(const UTrie3 *trie,
            UTrie3EnumValue *enumValue, UTrie3EnumRange *enumRange, const void *context);

/* Building a trie ---------------------------------------------------------- */

/**
 * Open an empty, writable trie. At build time, 32-bit data values are used.
 * utrie3_freeze() takes a valueBits parameter
 * which determines the data value width in the serialized and frozen forms.
 * You must utrie3_close() the trie once you are done using it.
 *
 * @param initialValue the initial value that is set for all code points
 * @param errorValue the value for out-of-range code points and ill-formed UTF-8/16
 * @param pErrorCode an in/out ICU UErrorCode
 * @return a pointer to the allocated and initialized new trie
 */
U_CAPI UTrie3 * U_EXPORT2
utrie3_open(uint32_t initialValue, uint32_t errorValue, UErrorCode *pErrorCode);

/**
 * Clone a trie.
 * You must utrie3_close() the clone once you are done using it.
 *
 * @param other the trie to clone
 * @param pErrorCode an in/out ICU UErrorCode
 * @return a pointer to the new trie clone
 */
U_CAPI UTrie3 * U_EXPORT2
utrie3_clone(const UTrie3 *other, UErrorCode *pErrorCode);

/**
 * Clone a trie. The clone will be mutable/writable even if the other trie
 * is frozen. (See utrie3_freeze().)
 * You must utrie3_close() the clone once you are done using it.
 *
 * @param other the trie to clone
 * @param pErrorCode an in/out ICU UErrorCode
 * @return a pointer to the new trie clone
 */
U_CAPI UTrie3 * U_EXPORT2
utrie3_cloneAsThawed(const UTrie3 *other, UErrorCode *pErrorCode);

/**
 * Close a trie and release associated memory.
 *
 * @param trie the trie
 */
U_CAPI void U_EXPORT2
utrie3_close(UTrie3 *trie);

/**
 * Set a value for a code point.
 *
 * @param trie the unfrozen trie
 * @param c the code point
 * @param value the value
 * @param pErrorCode an in/out ICU UErrorCode; among other possible error codes:
 * - U_NO_WRITE_PERMISSION if the trie is frozen
 */
U_CAPI void U_EXPORT2
utrie3_set32(UTrie3 *trie, UChar32 c, uint32_t value, UErrorCode *pErrorCode);

/**
 * Set a value in a range of code points [start..end].
 * All code points c with start<=c<=end will get the value if
 * overwrite is TRUE or if the old value is the initial value.
 *
 * @param trie the unfrozen trie
 * @param start the first code point to get the value
 * @param end the last code point to get the value (inclusive)
 * @param value the value
 * @param overwrite flag for whether old non-initial values are to be overwritten
 * @param pErrorCode an in/out ICU UErrorCode; among other possible error codes:
 * - U_NO_WRITE_PERMISSION if the trie is frozen
 */
U_CAPI void U_EXPORT2
utrie3_setRange32(UTrie3 *trie,
                  UChar32 start, UChar32 end,
                  uint32_t value, UBool overwrite,
                  UErrorCode *pErrorCode);

/**
 * Freeze a trie. Make it immutable (read-only) and compact it,
 * ready for serialization and for use with fast macros.
 * Functions to set values will fail after serializing.
 *
 * A trie can be frozen only once. If this function is called again with different
 * valueBits then it will set a U_ILLEGAL_ARGUMENT_ERROR.
 *
 * @param trie the trie
 * @param valueBits selects the data entry size; if smaller than 32 bits, then
 *                  the values stored in the trie will be truncated
 * @param pErrorCode an in/out ICU UErrorCode; among other possible error codes:
 * - U_INDEX_OUTOFBOUNDS_ERROR if the compacted index or data arrays are too long
 *                             for serialization
 *                             (the trie will be immutable and usable,
 *                             but not frozen and not usable with the fast macros)
 *
 * @see utrie3_cloneAsThawed
 */
U_CAPI void U_EXPORT2
utrie3_freeze(UTrie3 *trie, UTrie3ValueBits valueBits, UErrorCode *pErrorCode);

/**
 * Test if the trie is frozen. (See utrie3_freeze().)
 *
 * @param trie the trie
 * @return TRUE if the trie is frozen, that is, immutable, ready for serialization
 *         and for use with fast macros
 */
U_CAPI UBool U_EXPORT2
utrie3_isFrozen(const UTrie3 *trie);

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

#if 0  // TODO
/**
 * Build a UTrie3 (version 3) from a UTrie (version 1).
 * Enumerates all values in the UTrie and builds a UTrie3 with the same values.
 * The resulting UTrie3 will be frozen.
 *
 * @param trie1 the runtime UTrie structure to be enumerated
 * @param errorValue the value for out-of-range code points and ill-formed UTF-8/16
 * @param pErrorCode an in/out ICU UErrorCode
 * @return The frozen UTrie3 with the same values as the UTrie.
 */
U_CAPI UTrie3 * U_EXPORT2
utrie3_fromUTrie(const UTrie *trie1, uint32_t errorValue, UErrorCode *pErrorCode);
#endif

/* Public UTrie3 API macros ------------------------------------------------- */

/*
 * These macros provide fast data lookup from a frozen trie.
 * They will crash when used on an unfrozen trie.
 */

/**
 * Return a 16-bit trie value from a code point, with range checking.
 * Returns trie->errorValue if c is not in the range 0..U+10ffff.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code point
 * @return (uint16_t) The code point's trie value.
 */
#define UTRIE3_GET16(trie, c) _UTRIE3_GET((trie), index, (trie)->indexLength, (c))

/**
 * Return a 32-bit trie value from a code point, with range checking.
 * Returns trie->errorValue if c is not in the range 0..U+10ffff.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code point
 * @return (uint32_t) The code point's trie value.
 */
#define UTRIE3_GET32(trie, c) _UTRIE3_GET((trie), data32, 0, (c))

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
#define UTRIE3_U16_NEXT16(trie, src, limit, c, result) _UTRIE3_U16_NEXT(trie, index, src, limit, c, result)

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
#define UTRIE3_U16_PREV16(trie, start, src, c, result) _UTRIE3_U16_PREV(trie, index, start, src, c, result)

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
#define UTRIE3_U8_NEXT16(trie, src, limit, result)\
    _UTRIE3_U8_NEXT(trie, data16, index, src, limit, result)

/**
 * UTF-8: Post-increment src and get a 32-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param src (const char *, in/out) the source text pointer
 * @param limit (const char *, in) the limit pointer for the text (must not be NULL)
 * @param result (uint16_t, out) uint32_t variable for the trie lookup result
 */
#define UTRIE3_U8_NEXT32(trie, src, limit, result) \
    _UTRIE3_U8_NEXT(trie, data32, data32, src, limit, result)

/**
 * UTF-8: Pre-decrement src and get a 16-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param start (const char *, in) the start pointer for the text
 * @param src (const char *, in/out) the source text pointer
 * @param result (uint16_t, out) uint16_t variable for the trie lookup result
 */
#define UTRIE3_U8_PREV16(trie, start, src, result) \
    _UTRIE3_U8_PREV(trie, data16, index, start, src, result)

/**
 * UTF-8: Pre-decrement src and get a 32-bit value from the trie.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param start (const char *, in) the start pointer for the text
 * @param src (const char *, in/out) the source text pointer
 * @param result (uint16_t, out) uint32_t variable for the trie lookup result
 */
#define UTRIE3_U8_PREV32(trie, start, src, result) \
    _UTRIE3_U8_PREV(trie, data32, data32, start, src, result)

/* Public UTrie3 API: optimized UTF-16 access ------------------------------- */

/*
 * The following function and macros are used for highly optimized UTF-16
 * text processing. The UTRIE3_U16_NEXTxy() macros do not depend on these.
 *
 * UTF-16 text processing can be optimized by detecting surrogate pairs and
 * assembling supplementary code points only when there is non-trivial data
 * available.
 *
 * At build-time, use utrie3_enumForLeadSurrogate() to see if there
 * is non-trivial (non-initialValue) data for any of the supplementary
 * code points associated with a lead surrogate.
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

/**
 * Enumerate the trie values for the 1024=0x400 code points
 * corresponding to a given lead surrogate.
 * For example, for the lead surrogate U+D87E it will enumerate the values
 * for [U+2F800..U+2FC00[.
 * Used by data builder code that sets special lead surrogate code unit values
 * for optimized UTF-16 string processing.
 *
 * Do not modify the trie during the enumeration.
 *
 * Except for the limited code point range, this functions just like utrie3_enum():
 * For each entry in the trie, the value to be delivered is passed through
 * the UTrie3EnumValue function.
 * The value is unchanged if that function pointer is NULL.
 *
 * For each contiguous range of code points with a given (transformed) value,
 * the UTrie3EnumRange function is called.
 *
 * @param trie a pointer to the trie
 * @param enumValue a pointer to a function that may transform the trie entry value,
 *                  or NULL if the values from the trie are to be used directly
 * @param enumRange a pointer to a function that is called for each contiguous range
 *                  of code points with the same (transformed) value
 * @param context an opaque pointer that is passed on to the callback functions
 */
U_CAPI void U_EXPORT2
utrie3_enumForLeadSurrogate(const UTrie3 *trie, UChar32 lead,
                            UTrie3EnumValue *enumValue, UTrie3EnumRange *enumRange,
                            const void *context);

/**
 * Returns a 16-bit trie value from a BMP code point or UTF-16 code unit (0..U+ffff).
 * Same as UTRIE3_GET16() if c is a BMP code point, but smaller and faster.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code unit, must be 0<=c<=U+ffff
 * @return (uint16_t) The code unit's trie value.
 */
#define UTRIE3_GET16_FROM_BMP(trie, c) _UTRIE3_GET_FROM_BMP((trie), index, c)

/**
 * Returns a 32-bit trie value from a BMP code point or UTF-16 code unit (0..U+ffff).
 * Same as UTRIE3_GET32() if c is a BMP code point, but smaller and faster.
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code unit, must be 0<=c<=U+ffff
 * @return (uint32_t) The code unit's trie value.
 */
#define UTRIE3_GET32_FROM_BMP(trie, c) _UTRIE3_GET_FROM_BMP((trie), data32, c)

/**
 * Return a 16-bit trie value from a supplementary code point (U+10000..U+10ffff).
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code point, must be U+10000<=c<=U+10ffff
 * @return (uint16_t) The code point's trie value.
 */
#define UTRIE3_GET16_FROM_SUPP(trie, c) _UTRIE3_GET_FROM_SUPP((trie), index, c)

/**
 * Return a 32-bit trie value from a supplementary code point (U+10000..U+10ffff).
 *
 * @param trie (const UTrie3 *, in) a frozen trie
 * @param c (UChar32, in) the input code point, must be U+10000<=c<=U+10ffff
 * @return (uint32_t) The code point's trie value.
 */
#define UTRIE3_GET32_FROM_SUPP(trie, c) _UTRIE3_GET_FROM_SUPP((trie), data32, c)

U_CDECL_END

/* C++ convenience wrappers ------------------------------------------------- */
// TODO: remove?

#ifdef __cplusplus

#include "unicode/utf.h"
#include "mutex.h"

U_NAMESPACE_BEGIN

// Use the Forward/Backward subclasses below.
class UTrie3StringIterator : public UMemory {
public:
    UTrie3StringIterator(const UTrie3 *t, const UChar *p) :
        trie(t), codePointStart(p), codePointLimit(p), codePoint(U_SENTINEL) {}

    const UTrie3 *trie;
    const UChar *codePointStart, *codePointLimit;
    UChar32 codePoint;
};

class BackwardUTrie3StringIterator : public UTrie3StringIterator {
public:
    BackwardUTrie3StringIterator(const UTrie3 *t, const UChar *s, const UChar *p) :
        UTrie3StringIterator(t, p), start(s) {}

    uint16_t previous16();

    const UChar *start;
};

class ForwardUTrie3StringIterator : public UTrie3StringIterator {
public:
    // Iteration limit l can be NULL.
    // In that case, the caller must detect c==0 and stop.
    ForwardUTrie3StringIterator(const UTrie3 *t, const UChar *p, const UChar *l) :
        UTrie3StringIterator(t, p), limit(l) {}

    uint16_t next16();

    const UChar *limit;
};

U_NAMESPACE_END

#endif

/* Internal definitions ----------------------------------------------------- */

U_CDECL_BEGIN

/** Build-time trie structure. */
struct UNewTrie3;
typedef struct UNewTrie3 UNewTrie3;

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
    uint16_t index2NullOffset;  /* 0xffff if there is no dedicated index-2 null block */
    uint16_t dataNullOffset;
    uint32_t initialValue;
    /** Value returned for out-of-range code points and ill-formed UTF-8/16. */
    uint32_t errorValue;

    /* Start of the last range which ends at U+10ffff, and its value. */
    UChar32 highStart;
    uint16_t highStartLead16;  // U16_LEAD(highStart)
    uint16_t shiftedHighStart;  // highStart>>12
    uint32_t highValue;

    /* private: used by builder and unserialization functions */
    void *memory;           /* serialized bytes; NULL if not frozen yet */
    int32_t length;         /* number of serialized bytes at memory; 0 if not frozen yet */
    UBool isMemoryOwned;    /* TRUE if the trie owns the memory */
    UBool padding1;
    int16_t padding2;
    UNewTrie3 *newTrie;     /* builder object; NULL when frozen */
};

/**
 * Trie constants, defining shift widths, index array lengths, etc.
 *
 * These are needed for the runtime macros but users can treat these as
 * implementation details and skip to the actual public API further below.
 */
enum {
    /** Shift size for getting the index-1 table offset. */
    UTRIE3_SHIFT_1=6+5,

    /** Shift size for getting the index-2 table offset. */
    UTRIE3_SHIFT_2=5,

    /**
     * Difference between the two shift sizes,
     * for getting an index-1 offset from an index-2 offset. 6=11-5
     */
    UTRIE3_SHIFT_1_2=UTRIE3_SHIFT_1-UTRIE3_SHIFT_2,

    /**
     * Number of index-1 entries for the BMP. 32=0x20
     * This part of the index-1 table is omitted from the serialized form.
     */
    UTRIE3_OMITTED_BMP_INDEX_1_LENGTH=0x10000>>UTRIE3_SHIFT_1,

    /** Number of code points per index-1 table entry. 2048=0x800 */
    UTRIE3_CP_PER_INDEX_1_ENTRY=1<<UTRIE3_SHIFT_1,

    /** Number of entries in an index-2 block. 64=0x40 */
    UTRIE3_INDEX_2_BLOCK_LENGTH=1<<UTRIE3_SHIFT_1_2,

    /** Mask for getting the lower bits for the in-index-2-block offset. */
    UTRIE3_INDEX_2_MASK=UTRIE3_INDEX_2_BLOCK_LENGTH-1,

    /** Number of entries in a data block. 32=0x20 */
    UTRIE3_DATA_BLOCK_LENGTH=1<<UTRIE3_SHIFT_2,

    /** Mask for getting the lower bits for the in-data-block offset. */
    UTRIE3_DATA_MASK=UTRIE3_DATA_BLOCK_LENGTH-1,

    /**
     * Shift size for shifting left the index array values.
     * Increases possible data size with 16-bit index values at the cost
     * of compactability.
     * This requires data blocks to be aligned by UTRIE3_DATA_GRANULARITY.
     */
    UTRIE3_INDEX_SHIFT=2,

    /** The alignment size of a data block. Also the granularity for compaction. */
    UTRIE3_DATA_GRANULARITY=1<<UTRIE3_INDEX_SHIFT,

    /* Fixed layout of the first part of the index array. ------------------- */

    /** The BMP part of the index-2 table is fixed and linear and starts at offset 0. */
    UTRIE3_INDEX_2_OFFSET=0,

    /** The length of the BMP part of the index-2 table. 2048=0x800 */
    UTRIE3_INDEX_2_BMP_LENGTH=0x10000>>UTRIE3_SHIFT_2,

    /**
     * The 2-byte UTF-8 version of the index-2 table follows at offset 2048=0x800.
     * Length 32=0x20 for lead bytes C0..DF, regardless of UTRIE3_SHIFT_2.
     */
    UTRIE3_UTF8_2B_INDEX_2_OFFSET=UTRIE3_INDEX_2_BMP_LENGTH,
    UTRIE3_UTF8_2B_INDEX_2_LENGTH=0x800>>6,  /* U+0800 is the first code point after 2-byte UTF-8 */

    /**
     * The index-1 table, only used for supplementary code points, at offset 2080=0x820.
     * Variable length, for code points up to highStart, where the last single-value range starts.
     * Maximum length 512=0x200=0x100000>>UTRIE3_SHIFT_1.
     * (For 0x100000 supplementary code points U+10000..U+10ffff.)
     *
     * The part of the index-2 table for supplementary code points starts
     * after this index-1 table.
     *
     * Both the index-1 table and the following part of the index-2 table
     * are omitted completely if there is only BMP data.
     */
    UTRIE3_INDEX_1_OFFSET=UTRIE3_UTF8_2B_INDEX_2_OFFSET+UTRIE3_UTF8_2B_INDEX_2_LENGTH,
    UTRIE3_MAX_INDEX_1_LENGTH=0x100000>>UTRIE3_SHIFT_1,

    /*
     * Fixed layout of the first part of the data array. -----------------------
     * Starts with 4 blocks (128=0x80 entries) for ASCII.
     */

    /** The start of non-linear-ASCII data blocks, at offset 128=0x80. */
    UTRIE3_DATA_START_OFFSET=0x80
};

/* Internal functions and macros -------------------------------------------- */

/**
 * Internal function for part of the UTRIE3_U8_PREVxx() macro implementations.
 * Do not call directly.
 * @internal
 */
U_INTERNAL int32_t U_EXPORT2
utrie3_internalU8PrevIndex(const UTrie3 *trie, UChar32 c,
                           const uint8_t *start, const uint8_t *src);


/** Internal trie getter from a BMP code point. Returns the data index. */
#define _UTRIE3_INDEX_FROM_BMP(trieIndex, c) \
    (((int32_t)((trieIndex)[(c)>>UTRIE3_SHIFT_2])<<UTRIE3_INDEX_SHIFT)+ \
        ((c)&UTRIE3_DATA_MASK))

/** Internal trie getter from a supplementary code point below highStart. Returns the data index. */
#define _UTRIE3_INDEX_FROM_SUPP(trieIndex, c) \
    (((int32_t)((trieIndex)[ \
        (trieIndex)[(UTRIE3_INDEX_1_OFFSET-UTRIE3_OMITTED_BMP_INDEX_1_LENGTH)+ \
                      ((c)>>UTRIE3_SHIFT_1)]+ \
        (((c)>>UTRIE3_SHIFT_2)&UTRIE3_INDEX_2_MASK)]) \
    <<UTRIE3_INDEX_SHIFT)+ \
    ((c)&UTRIE3_DATA_MASK))

/** Internal trie getter from a UTF-16 single/lead code unit. Returns the data. */
#define _UTRIE3_GET_FROM_BMP(trie, data, c) \
    (trie)->data[_UTRIE3_INDEX_FROM_BMP((trie)->index, c)]

/** Internal trie getter from a supplementary code point. Returns the data. */
#define _UTRIE3_GET_FROM_SUPP(trie, data, c) \
    ((c)>=(trie)->highStart ? (trie)->highValue : (trie)->data[_UTRIE3_INDEX_FROM_SUPP((trie)->index, c)])

/**
 * Internal trie getter from a code point, with checking that c is in 0..10FFFF.
 * Returns the data.
 */
#define _UTRIE3_GET(trie, data, asciiOffset, c) \
    ((uint32_t)(c)<=0xffff ? \
        _UTRIE3_GET_FROM_BMP(trie, data, c) : \
        (uint32_t)(c)>0x10ffff ? \
            (trie)->errorValue : \
            _UTRIE3_GET_FROM_SUPP(trie, data, c))

/** Internal next-post-increment: get the next code point (c) and its data. */
#define _UTRIE3_U16_NEXT(trie, data, src, limit, c, result) { \
    (c)=*(src)++; \
    if(!U16_IS_SURROGATE(c)) { \
        (result)=_UTRIE3_GET_FROM_BMP(trie, data, c); \
    } else { \
        uint16_t __c2; \
        if(U16_IS_SURROGATE_LEAD(c) && (src)!=(limit) && U16_IS_TRAIL(__c2=*(src))) { \
            ++(src); \
            (c)=U16_GET_SUPPLEMENTARY((c), __c2); \
            (result)=_UTRIE3_GET_FROM_SUPP((trie), data, (c)); \
        } else { \
            (result)=(trie)->errorValue; \
        } \
    } \
}

/** Internal pre-decrement-previous: get the previous code point (c) and its data */
#define _UTRIE3_U16_PREV(trie, data, start, src, c, result) { \
    (c)=*--(src); \
    if(!U16_IS_SURROGATE(c)) { \
        (result)=_UTRIE3_GET_FROM_BMP(trie, data, c); \
    } else { \
        uint16_t __c2; \
        if(U16_IS_SURROGATE_TRAIL(c) && (src)!=(start) && U16_IS_LEAD(__c2=*((src)-1))) { \
            --(src); \
            (c)=U16_GET_SUPPLEMENTARY(__c2, (c)); \
            (result)=_UTRIE3_GET_FROM_SUPP((trie), data, (c)); \
        } else { \
            (result)=(trie)->errorValue; \
        } \
    } \
}

/** Internal UTF-8 next-post-increment: get the next code point's data. */
#define _UTRIE3_U8_NEXT(trie, ascii, data, src, limit, result) { \
    uint16_t __lead=(uint8_t)*(src)++; \
    if(U8_IS_SINGLE(__lead)) { \
        (result)=(trie)->ascii[__lead]; \
    } else { \
        uint8_t __t1, __t2, __t3; \
        if((src)!=(limit) && \
            (__lead>=0xe0 ? \
                __lead<0xf0 ?  /* U+0800..U+FFFF except surrogates */ \
                    U8_LEAD3_T1_BITS[__lead&=0xf]&(1<<((__t1=*(src))>>5)) && ++(src)!=(limit) && \
                    (__t2=*(src)-0x80)<=0x3f && \
                    ((result)=(trie)->data[ \
                        ((int32_t)((trie)->index[(__lead<<(12-UTRIE3_SHIFT_2))+ \
                                                 ((__t1&0x3f)<<(6-UTRIE3_SHIFT_2))+ \
                                                 (__t2>>UTRIE3_SHIFT_2)]) \
                        <<UTRIE3_INDEX_SHIFT)+ \
                        (__t2&UTRIE3_DATA_MASK)], 1) \
                :  /* U+10000..U+10FFFF */ \
                    (__lead-=0xf0)<=4 && \
                    U8_LEAD4_T1_BITS[(__t1=*(src))>>4]&(1<<__lead) && \
                    (__lead=(__lead<<6)|(__t1&0x3f), ++(src)!=(limit)) && \
                    (__t2=*(src)-0x80)<=0x3f && ++(src)!=(limit) && (__t3=*(src)-0x80)<=0x3f && \
                    (result= __lead>=(trie)->shiftedHighStart ? (trie)->highValue : \
                        (trie)->data[ \
                            ((int32_t)((trie)->index[ \
                                (trie)->index[(UTRIE3_INDEX_1_OFFSET-UTRIE3_OMITTED_BMP_INDEX_1_LENGTH)+ \
                                            (__lead<<(12-UTRIE3_SHIFT_1))+(__t2>>(UTRIE3_SHIFT_1-6))]+ \
                                ((__t2&0x1f)<<(6-UTRIE3_SHIFT_2))+(__t3>>UTRIE3_SHIFT_2)]) \
                            <<UTRIE3_INDEX_SHIFT)+ \
                            (__t3&UTRIE3_DATA_MASK)], 1) \
            :  /* U+0080..U+07FF */ \
                __lead>=0xc2 && (__t1=*(src)-0x80)<=0x3f && \
                ((result)=(trie)->data[ \
                    (trie)->index[(UTRIE3_UTF8_2B_INDEX_2_OFFSET-0xc0)+__lead]+__t1], 1))) { \
            ++(src); \
        } else { \
            (result)=(trie)->errorValue;  /* ill-formed*/ \
        } \
    } \
}

/** Internal UTF-8 pre-decrement-previous: get the previous code point's data. */
#define _UTRIE3_U8_PREV(trie, ascii, data, start, src, result) { \
    uint8_t __b=(uint8_t)*--(src); \
    if(U8_IS_SINGLE(__b)) { \
        (result)=(trie)->ascii[__b]; \
    } else { \
        int32_t __index=utrie3_internalU8PrevIndex((trie), __b, (const uint8_t *)(start), \
                                                                (const uint8_t *)(src)); \
        (src)-=__index&7; \
        (result)= __index>=0 ? (trie)->data[__index>>3] : \
                  __index>=-8 ? (trie)->errorValue : (trie)->highValue; \
    } \
}

U_CDECL_END

#endif
