/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucnvmbcs.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jul07
*   created by: Markus W. Scherer
*/

#ifndef __UCNVMBCS_H__
#define __UCNVMBCS_H__

#include "unicode/utypes.h"
#include "unicode/ucnv.h"

/* MBCS converter data and state -------------------------------------------- */

/**
 * MBCS action codes for conversions to Unicode.
 * These values are in bits 30..27 of the state table entries.
 */
enum {
    MBCS_STATE_ILLEGAL,
    MBCS_STATE_CHANGE_ONLY,
    MBCS_STATE_UNASSIGNED,

    MBCS_STATE_FALLBACK_DIRECT_16,
    MBCS_STATE_FALLBACK_DIRECT_20,

    MBCS_STATE_VALID_DIRECT_16,
    MBCS_STATE_VALID_DIRECT_20,

    MBCS_STATE_VALID_16,
    MBCS_STATE_VALID_16_PAIR
};

/**
 * MBCS output types for conversions from Unicode.
 * These per-converter types determine the storage method in stage 3 of the lookup table,
 * mostly how many bytes are stored per entry.
 */
enum {
    MBCS_OUTPUT_1,          /* 0 */
    MBCS_OUTPUT_2,          /* 1 */
    MBCS_OUTPUT_3,          /* 2 */
    MBCS_OUTPUT_4,          /* 3 */

    MBCS_OUTPUT_3_EUC=8,    /* 8 */
    MBCS_OUTPUT_4_EUC,      /* 9 */

    MBCS_OUTPUT_2_SISO=12,  /* c */
    MBCS_OUTPUT_2_HZ        /* d */
};

/**
 * Fallbacks to Unicode are stored outside the normal state table and code point structures
 * in a vector of items of this type. They are sorted by offset.
 */
typedef struct {
    uint32_t offset;
    UChar32 codePoint;
} _MBCSToUFallback;

/**
 * This is the MBCS part of the UConverterTable union (a runtime data structure).
 * It keeps all the per-converter data and points into the loaded mapping tables.
 */
typedef struct UConverterMBCSTable {
    /* toUnicode */
    uint8_t countStates;
    uint32_t countToUFallbacks;

    const int32_t (*stateTable)/*[countStates]*/[256];
    const uint16_t *unicodeCodeUnits/*[countUnicodeResults]*/;
    const _MBCSToUFallback *toUFallbacks;

    /* fromUnicode */
    const uint16_t *fromUnicodeTable;
    const uint8_t *fromUnicodeBytes;
    uint8_t outputType, unicodeMask;
} UConverterMBCSTable;

enum {
    MBCS_STAGE_2_MULTIPLIER=4
};

/**
 * MBCS data structure as part of a .cnv file:
 *
 * uint32_t [8]; -- 8 values:
 *  0   MBCS version in UVersionInfo format (1.0.0.0)
 *  1   countStates
 *  2   countToUFallbacks
 *  3   offsetToUCodeUnits (offsets are counted from the beginning of this header structure)
 *  4   offsetFromUTable
 *  5   offsetFromUBytes
 *  6   flags, bits:
 *          31.. 8 reserved
 *           7.. 0 outputType
 *  7   reserved
 *
 * stateTable[countStates][256];
 *
 * struct { (fallbacks are sorted by offset)
 *     uint32_t offset;
 *     UChar32 codePoint;
 * } toUFallbacks[countToUFallbacks];
 *
 * uint16_t unicodeCodeUnits[?]; (even number of units or padded)
 *
 * uint16_t fromUTable[0x440+?]; (32-bit-aligned)
 *
 * uint8_t fromUBytes[?];
 */
typedef struct {
    UVersionInfo version;
    uint32_t countStates,
             countToUFallbacks,
             offsetToUCodeUnits,
             offsetFromUTable,
             offsetFromUBytes,
             flags,
             reserved;
} _MBCSHeader;

/** Forward declaration to enable the following function declarations. */
struct UConverterSharedData;

/** Forward declaration to enable the following function declarations. */
typedef struct UConverterSharedData UConverterSharedData;

/**
 * This is a simple version of _MBCSGetNextUChar() that is used
 * by other converter implementations.
 * It does not use state from the converter, nor error codes.
 *
 * Return value:
 * U+fffe   unassigned
 * U+ffff   illegal
 * otherwise the Unicode code point
 */
U_CFUNC UChar32
_MBCSSimpleGetNextUChar(UConverterSharedData *sharedData,
                        const char **pSource, const char *sourceLimit,
                        UBool useFallback);

/** This version of _MBCSSimpleGetNextUChar() is optimized for single-byte, single-state codepages. */
U_CFUNC UChar32
_MBCSSingleSimpleGetNextUChar(UConverterSharedData *sharedData,
                              uint8_t b, UBool useFallback);

/**
 * This macro version of _MBCSSingleSimpleGetNextUChar() gets a code point from a byte.
 * It works for single-byte, single-state codepages that only map
 * to and from BMP code points, and it always
 * returns fallback values.
 */
#define _MBCS_SINGLE_SIMPLE_GET_NEXT_BMP(sharedData, b) \
    (UChar)(((sharedData)->table->mbcs.stateTable[0][(uint8_t)(b)])>>7)

/**
 * This is an internal function that allows other converter implementations
 * to check whether a byte is a lead byte.
 */
U_CFUNC UBool
_MBCSIsLeadByte(UConverterSharedData *sharedData, char byte);

/** This is a macro version of _MBCSIsLeadByte(). */
#define _MBCS_IS_LEAD_BYTE(sharedData, byte) \
    (UBool)((sharedData)->table->mbcs.stateTable[0][(uint8_t)(byte)]>=0)

/**
 * This is another simple conversion function for internal use by other
 * conversion implementations.
 * It does not use the converter state nor call callbacks.
 * It converts one single Unicode code point into codepage bytes, encoded
 * as one 32-bit value. The function returns the number of bytes in *pValue:
 * 1..4 the number of bytes in *pValue
 * 0    unassigned (*pValue undefined)
 * -1   illegal (currently not used, *pValue undefined)
 *
 * *pValue will contain the resulting bytes with the last byte in bits 7..0,
 * the second to last byte in bits 15..8, etc.
 * Currently, the function assumes but does not check that 0<=c<=0x10ffff.
 */
U_CFUNC int32_t
_MBCSFromUChar32(UConverterSharedData *sharedData,
                 UChar32 c, uint32_t *pValue,
                 UBool useFallback);

/**
 * This version of _MBCSFromUChar32() is optimized for single-byte codepages.
 * It returns the codepage byte for the code point, or -1 if it is unassigned.
 */
U_CFUNC int32_t
_MBCSSingleFromUChar32(UConverterSharedData *sharedData,
                       UChar32 c,
                       UBool useFallback);

/**
 * SBCS, DBCS, and EBCDIC_STATEFUL are replaced by MBCS, but
 * we cheat a little about the type, returning the old types if appropriate.
 */
U_CFUNC UConverterType
_MBCSGetType(const UConverter* converter);

#endif
