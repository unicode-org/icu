// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3_impl.h (modified from utrie2_impl.h)
// created: 2017dec29 Markus W. Scherer

#ifndef __UTRIE3_IMPL_H__
#define __UTRIE3_IMPL_H__

#include "utrie3.h"

// UTrie3 signature values, in platform endianness and opposite endianness.
#define UTRIE3_SIG      0x54726933
#define UTRIE3_OE_SIG   0x33697254

/**
 * Trie data structure in serialized form:
 *
 * UTrie3Header header;
 * uint16_t index[header.index2Length];
 * uint16_t data[header.shiftedDataLength<<2];  -- or uint32_t data[...]
 * @internal
 */
typedef struct UTrie3Header {
    /** "Tri3" in big-endian US-ASCII (0x54726933) */
    uint32_t signature;

    /**
     * Options bit field:
     * Bits 15..12: Data length bits 19..16.
     * Bits 11..8: Data null block offset bits 19..16.
     * Bits 7..6: UTrie3Type
     * Bits 5..3: Reserved (0).
     * Bits 2..0: UTrie3ValueBits valueBits
     */
    uint16_t options;

    /** Total length of the BMP index and the supplementary index-1 and index-2 tables. */
    uint16_t indexLength;

    /** Data length bits 15..0. */
    uint16_t dataLength;

    /** Index-2 null block offset, 0x7fff or 0xffff if none. */
    uint16_t index2NullOffset;

    /** Data null block offset bits 15..0, 0xfffff if none. */
    uint16_t dataNullOffset;

    /**
     * First code point of the single-value range ending with U+10ffff,
     * rounded up and then shifted right by UTRIE3_SUPP_SHIFT_1.
     */
    uint16_t shiftedHighStart;
} UTrie3Header;

/**
 * Constants for use with UTrie3Header.options.
 * @internal
 */
enum {
    /** Mask to get the UTrie3ValueBits valueBits from options. */
    UTRIE3_OPTIONS_DATA_LENGTH_MASK = 0xf000,
    UTRIE3_OPTIONS_DATA_NULL_OFFSET_MASK = 0xf00,
    UTRIE3_OPTIONS_RESERVED_MASK = 0x38,
    UTRIE3_OPTIONS_VALUE_BITS_MASK = 7,
    /**
     * Value for index2NullOffset which indicates that there is no index-2 null block.
     * Bit 15 is unused for this value because this bit is used if the index-2 contains
     * 18-bit indexes.
     */
    UTRIE3_NO_INDEX2_NULL_OFFSET = 0x7fff,
    UTRIE3_NO_DATA_NULL_OFFSET = 0xfffff
};

// Internal constants.
// TODO: doc complete data structure
enum {
    /** The length of the BMP index table. 1024=0x400 */
    UTRIE3_BMP_INDEX_LENGTH = 0x10000 >> UTRIE3_BMP_SHIFT,

    UTRIE3_SMALL_LIMIT = 0x1000,
    UTRIE3_SMALL_INDEX_LENGTH = UTRIE3_SMALL_LIMIT >> UTRIE3_BMP_SHIFT,

    /** Shift size for getting the index-2 table offset. */
    UTRIE3_SUPP_SHIFT_2 = 4,
    // TODO: remove _SUPP, or replace BMP_ with FAST_ and SUPP_ with SMALL_

    /** Shift size for getting the index-1 table offset. */
    UTRIE3_SUPP_SHIFT_1 = 5 + UTRIE3_SUPP_SHIFT_2,

    /** Shift size for getting the index-0 table offset. TODO: shift index-x numbers; drop SUPP from shifts? */
    UTRIE3_SUPP_SHIFT_0 = 5 + UTRIE3_SUPP_SHIFT_1,

    /**
     * Difference between two shift sizes,
     * for getting an index-1 offset from an index-2 offset. 5=9-4
     */
    UTRIE3_SUPP_SHIFT_1_2 = UTRIE3_SUPP_SHIFT_1 - UTRIE3_SUPP_SHIFT_2,

    /**
     * Difference between two shift sizes,
     * for getting an index-0 offset from an index-1 offset. 5=14-9
     */
    UTRIE3_SUPP_SHIFT_0_1 = UTRIE3_SUPP_SHIFT_0 - UTRIE3_SUPP_SHIFT_1,

    /**
     * Number of index-0 entries for the BMP. (4)
     * This part of the index-0 table is omitted from the serialized form.
     */
    UTRIE3_OMITTED_BMP_INDEX_0_LENGTH = 0x10000 >> UTRIE3_SUPP_SHIFT_0,

    /** Number of entries in an index-1 block. 32=0x20 */
    UTRIE3_INDEX_1_BLOCK_LENGTH = 1 << UTRIE3_SUPP_SHIFT_0_1,

    /** Mask for getting the lower bits for the in-index-1-block offset. */
    UTRIE3_INDEX_1_MASK = UTRIE3_INDEX_1_BLOCK_LENGTH - 1,

    /** Number of code points per index-1 table entry. 512=0x200 */
    UTRIE3_CP_PER_INDEX_1_ENTRY = 1 << UTRIE3_SUPP_SHIFT_1,

    /** Number of entries in an index-2 block. 32=0x20 */
    UTRIE3_INDEX_2_BLOCK_LENGTH = 1 << UTRIE3_SUPP_SHIFT_1_2,

    /** Mask for getting the lower bits for the in-index-2-block offset. */
    UTRIE3_INDEX_2_MASK = UTRIE3_INDEX_2_BLOCK_LENGTH - 1,

    /** Number of entries in a supplementary data block. 16=0x10 */
    UTRIE3_SUPP_DATA_BLOCK_LENGTH = 1 << UTRIE3_SUPP_SHIFT_2,

    /** Mask for getting the lower bits for the in-supplementary-data-block offset. */
    UTRIE3_SUPP_DATA_MASK = UTRIE3_SUPP_DATA_BLOCK_LENGTH - 1
};

/**
 * For a "fast" trie:
 *
 * The supplementary index-0 table follows the BMP index table at offset 1024=0x400.
 * Variable length, for code points up to highStart, where the last single-value range starts.
 * Maximum length 1024=0x400=0x100000>>UTRIE3_SUPP_SHIFT_1.
 * (For 0x100000 supplementary code points U+10000..U+10ffff.)
 *
 * The supplementary index-2 table starts after this index-1 table.
 *
 * The supplementary index tables
 * are omitted completely if there is only BMP data (highStart<=0x10000).
 *
 * For a "small" trie:
 *
 * The "supplementary" index tables are always stored.
 *
 * For both trie types:
 *
 * The last index-1 block may be a partial block, storing indexes only for code points
 * below highStart.
 */

#endif
