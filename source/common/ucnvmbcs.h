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

/* MBCS converter data and state -------------------------------------------- */

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

enum {
    MBCS_OUTPUT_1,
    MBCS_OUTPUT_2,
    MBCS_OUTPUT_3,
    MBCS_OUTPUT_4,

    MBCS_OUTPUT_3_EUC=8,
    MBCS_OUTPUT_4_EUC
};

typedef struct {
    uint32_t offset;
    UChar32 codePoint;
} _MBCSToUFallback;

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
    uint8_t outputType;
} UConverterMBCSTable;

/*
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

struct UConverterSharedData;
typedef struct UConverterSharedData UConverterSharedData;

U_CFUNC UChar32
_MBCSSimpleGetNextUChar(UConverterSharedData *sharedData,
                        const char **pSource, const char *sourceLimit);

U_CFUNC UBool
_MBCSIsLeadByte(UConverterSharedData *sharedData, char byte);

#endif
