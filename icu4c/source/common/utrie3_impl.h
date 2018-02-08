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
     * Bits 31..30: Reserved (0).
     * Bits 29..12: Data null block offset, not shifted (0x3ffff if none).
     * Bits 11..3: Reserved (0).
     * Bits 2..0: UTrie3ValueBits valueBits
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
    UTRIE3_OPTIONS_RESERVED_MASK = 0xc0000ff8,
    UTRIE3_OPTIONS_VALUE_BITS_MASK = 7,
    UTRIE3_NO_INDEX2_NULL_OFFSET = 0xffff,  // TODO: doc anything > max index length
    UTRIE3_NO_DATA_NULL_OFFSET = 0xfffff  // TODO: doc always granularity if real
};

#endif
