// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// ucptrie_impl.h (modified from utrie2_impl.h)
// created: 2017dec29 Markus W. Scherer

#ifndef __UCPTRIE_IMPL_H__
#define __UCPTRIE_IMPL_H__

#include "unicode/ucptrie.h"
#include "unicode/umutablecptrie.h"  // TODO

// UCPTrie signature values, in platform endianness and opposite endianness.
#define UCPTRIE_SIG     0x54726933
#define UCPTRIE_OE_SIG  0x33697254

/**
 * Trie data structure in serialized form:
 *
 * UCPTrieHeader header;
 * uint16_t index[header.index2Length];
 * uint16_t data[header.shiftedDataLength<<2];  -- or uint32_t data[...]
 * @internal
 */
struct UCPTrieHeader {
    /** "Tri3" in big-endian US-ASCII (0x54726933) */
    uint32_t signature;

    /**
     * Options bit field:
     * Bits 15..12: Data length bits 19..16.
     * Bits 11..8: Data null block offset bits 19..16.
     * Bits 7..6: UCPTrieType
     * Bits 5..3: Reserved (0).
     * Bits 2..0: UCPTrieValueWidth
     */
    uint16_t options;

    /** Total length of the index tables. */
    uint16_t indexLength;

    /** Data length bits 15..0. */
    uint16_t dataLength;

    /** Index-3 null block offset, 0x7fff or 0xffff if none. */
    uint16_t index3NullOffset;

    /** Data null block offset bits 15..0, 0xfffff if none. */
    uint16_t dataNullOffset;

    /**
     * First code point of the single-value range ending with U+10ffff,
     * rounded up and then shifted right by UCPTRIE_SHIFT_2.
     */
    uint16_t shiftedHighStart;
};

/**
 * Constants for use with UCPTrieHeader.options.
 * @internal
 */
enum {
    UCPTRIE_OPTIONS_DATA_LENGTH_MASK = 0xf000,
    UCPTRIE_OPTIONS_DATA_NULL_OFFSET_MASK = 0xf00,
    UCPTRIE_OPTIONS_RESERVED_MASK = 0x38,
    UCPTRIE_OPTIONS_VALUE_BITS_MASK = 7,
    /**
     * Value for index3NullOffset which indicates that there is no index-3 null block.
     * Bit 15 is unused for this value because this bit is used if the index-3 contains
     * 18-bit indexes.
     */
    UCPTRIE_NO_INDEX2_NULL_OFFSET = 0x7fff,
    UCPTRIE_NO_DATA_NULL_OFFSET = 0xfffff
};

// Internal constants.
// TODO: doc complete data structure
enum {
    /** The length of the BMP index table. 1024=0x400 */
    UCPTRIE_BMP_INDEX_LENGTH = 0x10000 >> UCPTRIE_FAST_SHIFT,

    UCPTRIE_SMALL_LIMIT = 0x1000,
    UCPTRIE_SMALL_INDEX_LENGTH = UCPTRIE_SMALL_LIMIT >> UCPTRIE_FAST_SHIFT,

    /** Shift size for getting the index-3 table offset. */
    UCPTRIE_SHIFT_3 = 4,

    /** Shift size for getting the index-2 table offset. */
    UCPTRIE_SHIFT_2 = 5 + UCPTRIE_SHIFT_3,

    /** Shift size for getting the index-1 table offset. */
    UCPTRIE_SHIFT_1 = 5 + UCPTRIE_SHIFT_2,

    /**
     * Difference between two shift sizes,
     * for getting an index-2 offset from an index-3 offset. 5=9-4
     */
    UCPTRIE_SHIFT_2_3 = UCPTRIE_SHIFT_2 - UCPTRIE_SHIFT_3,

    /**
     * Difference between two shift sizes,
     * for getting an index-1 offset from an index-2 offset. 5=14-9
     */
    UCPTRIE_SHIFT_1_2 = UCPTRIE_SHIFT_1 - UCPTRIE_SHIFT_2,

    /**
     * Number of index-1 entries for the BMP. (4)
     * This part of the index-1 table is omitted from the serialized form.
     */
    UCPTRIE_OMITTED_BMP_INDEX_1_LENGTH = 0x10000 >> UCPTRIE_SHIFT_1,

    /** Number of entries in an index-2 block. 32=0x20 */
    UCPTRIE_INDEX_2_BLOCK_LENGTH = 1 << UCPTRIE_SHIFT_1_2,

    /** Mask for getting the lower bits for the in-index-2-block offset. */
    UCPTRIE_INDEX_2_MASK = UCPTRIE_INDEX_2_BLOCK_LENGTH - 1,

    /** Number of code points per index-2 table entry. 512=0x200 */
    UCPTRIE_CP_PER_INDEX_2_ENTRY = 1 << UCPTRIE_SHIFT_2,

    /** Number of entries in an index-3 block. 32=0x20 */
    UCPTRIE_INDEX_3_BLOCK_LENGTH = 1 << UCPTRIE_SHIFT_2_3,

    /** Mask for getting the lower bits for the in-index-3-block offset. */
    UCPTRIE_INDEX_3_MASK = UCPTRIE_INDEX_3_BLOCK_LENGTH - 1,

    /** Number of entries in a small data block. 16=0x10 */
    UCPTRIE_SMALL_DATA_BLOCK_LENGTH = 1 << UCPTRIE_SHIFT_3,

    /** Mask for getting the lower bits for the in-small-data-block offset. */
    UCPTRIE_SMALL_DATA_MASK = UCPTRIE_SMALL_DATA_BLOCK_LENGTH - 1
};

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

#ifdef UCPTRIE_DEBUG
U_CFUNC void
ucptrie_printLengths(const UCPTrie *trie, const char *which);

U_CFUNC void umutablecptrie_setName(UMutableCPTrie *builder, const char *name);
#endif

/*
 * For a "fast" trie:
 *
 * The supplementary index-1 table follows the BMP index table at offset 1024=0x400.
 * Variable length, for code points up to highStart, where the last single-value range starts.
 * Maximum length 64=0x40=0x100000>>UCPTRIE_SHIFT_1.
 * (For 0x100000 supplementary code points U+10000..U+10ffff.)
 *
 * After this index-1 table follow the index-3 and index-2 tables.
 *
 * The supplementary index tables are omitted completely
 * if there is only BMP data (highStart<=0x10000).
 *
 * For a "small" trie:
 *
 * The "supplementary" index tables are always stored.
 * The index-1 table starts from U+0000, its maximum length is 68=0x44=0x110000>>UCPTRIE_SHIFT_1.
 *
 * For both trie types:
 *
 * The last index-2 block may be a partial block, storing indexes only for code points
 * below highStart.
 */

/*
 * TODO User Guide?
 *
 * Public UCPTrie API: optimized UTF-16 access
 *
 * The following function and macros are used for highly optimized UTF-16
 * text processing. The UCPTRIE_FAST_U16_NEXTxy() macros do not depend on these.
 *
 * UTF-16 text processing can be optimized by detecting surrogate pairs and
 * assembling supplementary code points only when there is non-trivial data
 * available.
 *
 * At build-time, use umutablecptrie_getRange() starting from U+10000 to see if there
 * is non-trivial data for any of the supplementary code points
 * associated with a lead surrogate.
 * If so, then set a special (application-specific) value for the
 * lead surrogate.
 *
 * At runtime, use UCPTRIE_FAST_BMP_GET16() or
 * UCPTRIE_FAST_BMP_GET32() per code unit. If there is non-trivial
 * data and the code unit is a lead surrogate, then check if a trail surrogate
 * follows. If so, assemble the supplementary code point with
 * U16_GET_SUPPLEMENTARY() and look up its value with UCPTRIE_FAST_SUPP_GET16()
 * or UCPTRIE_FAST_SUPP_GET32(); otherwise deal with the unpaired surrogate in some way.
 *
 * If there is only trivial data for lead and trail surrogates, then processing
 * can often skip them. For example, in normalization or case mapping
 * all characters that do not have any mappings are simply copied as is.
 */

#endif
