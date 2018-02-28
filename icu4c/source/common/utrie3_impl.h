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
     * Bits 7..3: Reserved (0).
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
    UTRIE3_OPTIONS_RESERVED_MASK = 0xf8,
    UTRIE3_OPTIONS_VALUE_BITS_MASK = 7,
    UTRIE3_NO_INDEX2_NULL_OFFSET = 0x7fff,  // TODO: doc max value, bit 15 indicates something
    UTRIE3_NO_DATA_NULL_OFFSET = 0xfffff
};

#endif
