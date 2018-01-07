// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

// utrie3_impl.h (modified from utrie2_impl.h)
// created: 2017dec29 Markus W. Scherer

#ifndef __UTRIE3_IMPL_H__
#define __UTRIE3_IMPL_H__

#include "utrie3.h"

/* Public UTrie3 API implementation ----------------------------------------- */

/*
 * These definitions are mostly needed by utrie3.c,
 * but also by utrie3_serialize() and utrie3_swap().
 */

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
     * options bit field:
     * 15.. 4   reserved (0)
     *  3.. 0   UTrie3ValueBits valueBits
     */
    uint16_t options;

    /** UTRIE3_INDEX_1_OFFSET..UTRIE3_MAX_INDEX_LENGTH */
    uint16_t indexLength;

    /** (UTRIE3_DATA_START_OFFSET..UTRIE3_MAX_DATA_LENGTH)>>UTRIE3_INDEX_SHIFT */
    uint16_t shiftedDataLength;

    /** Null index and data blocks, not shifted. */
    uint16_t index2NullOffset, dataNullOffset;

    /**
     * First code point of the single-value range ending with U+10ffff,
     * rounded up and then shifted right by UTRIE3_SHIFT_1.
     */
    uint16_t shiftedHighStart;
    /** Value for code points highStart..U+10FFFF. */
    uint32_t highValue;
    /** Value returned for out-of-range code points and ill-formed UTF-8/16. */
    uint32_t errorValue;
} UTrie3Header;

/**
 * Constants for use with UTrie3Header.options.
 * @internal
 */
enum {
    /** Mask to get the UTrie3ValueBits valueBits from options. */
    UTRIE3_OPTIONS_VALUE_BITS_MASK=0xf
};

/* Building a trie ---------------------------------------------------------- */

/*
 * These definitions are mostly needed by utrie3_builder.cpp, but also by
 * utrie3_get32() and utrie3_enum().
 */

enum {
    /**
     * At build time, leave a gap in the index-2 table,
     * at least as long as the maximum lengths of the 2-byte UTF-8 index-2 table
     * and the supplementary index-1 table.
     * Round up to UTRIE3_INDEX_2_BLOCK_LENGTH for proper compacting.
     */
    UNEWTRIE3_INDEX_GAP_OFFSET=UTRIE3_INDEX_2_BMP_LENGTH,
    UNEWTRIE3_INDEX_GAP_LENGTH=
        ((UTRIE3_UTF8_2B_INDEX_2_LENGTH+UTRIE3_MAX_INDEX_1_LENGTH)+UTRIE3_INDEX_2_MASK)&
        ~UTRIE3_INDEX_2_MASK,

    /**
     * Maximum length of the build-time index-2 array.
     * Maximum number of Unicode code points (0x110000) shifted right by UTRIE3_SHIFT_2,
     * plus the build-time index gap,
     * plus the null index-2 block.
     */
    UNEWTRIE3_MAX_INDEX_2_LENGTH=
        (0x110000>>UTRIE3_SHIFT_2)+
        UNEWTRIE3_INDEX_GAP_LENGTH+
        UTRIE3_INDEX_2_BLOCK_LENGTH,

    UNEWTRIE3_INDEX_1_LENGTH=0x110000>>UTRIE3_SHIFT_1
};

/**
 * Maximum length of the build-time data array.
 * One entry per 0x110000 code points, plus the null block.
 */
#define UNEWTRIE3_MAX_DATA_LENGTH (0x110000+0x40)

/*
 * Build-time trie structure.
 *
 * Just using a boolean flag for "repeat use" could lead to data array overflow
 * because we would not be able to detect when a data block becomes unused.
 * It also leads to orphan data blocks that are kept through serialization.
 *
 * Need to use reference counting for data blocks,
 * and allocDataBlock() needs to look for a free block before increasing dataLength.
 *
 * This scheme seems like overkill for index-2 blocks since the whole index array is
 * preallocated anyway (unlike the growable data array).
 * Just allocating multiple index-2 blocks as needed.
 */
struct UNewTrie3 {
    int32_t index1[UNEWTRIE3_INDEX_1_LENGTH];
    int32_t index2[UNEWTRIE3_MAX_INDEX_2_LENGTH];
    uint32_t *data;

    uint32_t initialValue, errorValue;
    int32_t index2Length, dataCapacity, dataLength;
    int32_t firstFreeBlock;
    int32_t index2NullOffset, dataNullOffset;
    UChar32 highStart;
    UBool isCompacted;

    /**
     * Multi-purpose per-data-block table.
     *
     * Before compacting:
     *
     * Per-data-block reference counters/free-block list.
     *  0: unused
     * >0: reference counter (number of index-2 entries pointing here)
     * <0: next free data block in free-block list
     *
     * While compacting:
     *
     * Map of adjusted indexes, used in compactData() and compactIndex2().
     * Maps from original indexes to new ones.
     */
    int32_t map[UNEWTRIE3_MAX_DATA_LENGTH>>UTRIE3_SHIFT_2];
};

#endif
