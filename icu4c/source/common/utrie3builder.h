// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3builder.h (split out of utrie3.h)
// created: 2018jan24 Markus W. Scherer

#ifndef __UTRIE3BUILDER_H__
#define __UTRIE3BUILDER_H__

#include "unicode/utypes.h"
#include "unicode/utf8.h"
#include "putilimp.h"
#include "udataswp.h"
#include "utrie3.h"

U_CDECL_BEGIN

/**
 * \file
 *
 * TODO
 *
 * The following function and macros are used for highly optimized UTF-16
 * text processing. The UTRIE3_U16_NEXTxy() macros do not depend on these.
 *
 * UTF-16 text processing can be optimized by detecting surrogate pairs and
 * assembling supplementary code points only when there is non-trivial data
 * available.
 *
 * At build-time, use utrie3bld_getRange() starting from U+10000 to see if there
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
utrie3bld_open(uint32_t initialValue, uint32_t errorValue, UErrorCode *pErrorCode);

/**
 * Clone a trie.
 * You must utrie3_close() the clone once you are done using it.
 *
 * @param other the trie to clone
 * @param pErrorCode an in/out ICU UErrorCode
 * @return a pointer to the new trie clone
 */
U_CAPI UTrie3 * U_EXPORT2
utrie3bld_clone(const UTrie3 *other, UErrorCode *pErrorCode);

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
utrie3bld_cloneAsThawed(const UTrie3 *other, UErrorCode *pErrorCode);

/**
 * Close a trie and release associated memory.
 *
 * @param trie the trie
 */
U_CAPI void U_EXPORT2
utrie3bld_close(UTrie3 *trie);

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
utrie3bld_set(UTrie3 *trie, UChar32 c, uint32_t value, UErrorCode *pErrorCode);

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
utrie3bld_setRange(UTrie3 *trie,
                   UChar32 start, UChar32 end,
                   uint32_t value, UBool overwrite,
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
utrie3bld_get(const UTrie3 *trie, UChar32 c);

/**
 * Returns the last code point such that all those from start to there have the same value.
 * Can be used to efficiently iterate over all same-value ranges in a trie.
 *
 * For each entry in the trie, the value to be delivered is passed through
 * the UTrie3HandleValue function.
 * The value is unchanged if that function pointer is NULL.
 *
 * See the same-signature utrie3_getRange() for a code sample.
 *
 * @param trie a pointer to a trie builder
 * @param start range start
 * @param handleValue a pointer to a function that may modify the trie entry value,
 *     or NULL if the values from the trie are to be used directly
 * @param context an opaque pointer that is passed on to the handleValue function
 * @param pValue if not NULL, receives the value that every code point start..end has;
 *     optionally modified by handleValue(context, trie value)
 * @return the range end code point, or -1 if start is not a valid code point
 */
U_CAPI int32_t U_EXPORT2
utrie3bld_getRange(const UTrie3 *trie, UChar32 start,
                   UTrie3HandleValue *enumValue, const void *context, uint32_t *pValue);

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
utrie3bld_freeze(UTrie3 *trie, UTrie3ValueBits valueBits, UErrorCode *pErrorCode);

/**
 * Test if the trie is frozen. (See utrie3_freeze().)
 *
 * @param trie the trie
 * @return TRUE if the trie is frozen, that is, immutable, ready for serialization
 *         and for use with fast macros
 */
U_CAPI UBool U_EXPORT2
utrie3bld_isFrozen(const UTrie3 *trie);

/* Public UTrie3 API: miscellaneous functions ------------------------------- */

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

U_CDECL_END

/* Internal definitions ----------------------------------------------------- */

U_CDECL_BEGIN

#if 0
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
    const char *name;
};
#endif

U_CDECL_END

#endif
