// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// ucptrie_impl.h (modified from utrie2_impl.h)
// created: 2017dec29 Markus W. Scherer

#ifndef __UCPTRIE_IMPL_H__
#define __UCPTRIE_IMPL_H__

#include "unicode/ucptrie.h"

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
typedef struct UCPTrieHeader {
    /** "Tri3" in big-endian US-ASCII (0x54726933) */
    uint32_t signature;

    /**
     * Options bit field:
     * Bits 15..12: Data length bits 19..16.
     * Bits 11..8: Data null block offset bits 19..16.
     * Bits 7..6: UCPTrieType
     * Bits 5..3: Reserved (0).
     * Bits 2..0: UCPTrieValueBits
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
} UCPTrieHeader;

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

#endif
