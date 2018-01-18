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
     * Options bit field:
     * Bits 31..12: Data null block offset, not shifted (0xfffff if none).
     * Bits 11..4: Reserved (0).
     * Bits 3..0: UTrie3ValueBits valueBits
     */
    uint32_t options;

    /** UTRIE3_INDEX_1_OFFSET..UTRIE3_MAX_INDEX_LENGTH */
    uint16_t indexLength;

    /** (UTRIE3_DATA_START_OFFSET..UTRIE3_MAX_DATA_LENGTH)>>UTRIE3_INDEX_SHIFT */
    uint16_t shiftedDataLength;

    /** Index-2 null block offset, not shifted. */
    uint16_t index2NullOffset;

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
    UTRIE3_OPTIONS_VALUE_BITS_MASK=0xf,  // TODO: 7? 3??
    UTRIE3_NO_INDEX2_NULL_OFFSET = 0xffff,  // TODO: doc anything > max index length
    UTRIE3_NO_DATA_NULL_OFFSET = 0xfffff  // TODO: doc always granularity if real
};

/* Building a trie ---------------------------------------------------------- */

/*
 * These definitions are mostly needed by utrie3_builder.cpp, but also by
 * utrie3_get32() and utrie3_enum().
 */

enum {
    /**
     * At build time, leave a gap in the index-2 table,
     * at least as long as the maximum length of the supplementary index-1 table.
     * Round up to UTRIE3_INDEX_2_BLOCK_LENGTH for proper compacting.
     */
    UNEWTRIE3_INDEX_GAP_OFFSET=UTRIE3_INDEX_2_BMP_LENGTH,
    UNEWTRIE3_INDEX_GAP_LENGTH=(UTRIE3_MAX_INDEX_1_LENGTH+UTRIE3_INDEX_2_MASK)&~UTRIE3_INDEX_2_MASK,

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
struct UNewTrie3 {  // TODO: move to .cpp?
    uint32_t *data;
    int32_t dataCapacity, dataLength;
    int32_t dataNullIndex;

    uint8_t flags[0x110000>>UTRIE3_SHIFT_2];
    uint32_t index[0x110000>>UTRIE3_SHIFT_2];
};

U_CFUNC uint32_t  // TODO: back to utrie3.cpp? or utrie3_get32() to builder.cpp?
utrie3_get32FromBuilder(const UTrie3 *trie, UChar32 c);

#endif
