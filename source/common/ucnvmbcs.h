/*
******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
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
#include "ucnv_cnv.h"

/**
 * ICU conversion (.cnv) data file structure, following the usual UDataInfo
 * header.
 *
 * Format version: 6.2
 *
 * struct UConverterStaticData -- struct containing the converter name, IBM CCSID,
 *                                min/max bytes per character, etc.
 *                                see ucnv_bld.h
 *
 * --------------------
 *
 * The static data is followed by conversionType-specific data structures.
 * At the moment, there are only variations of MBCS converters. They all have
 * the same toUnicode structures, while the fromUnicode structures for SBCS
 * differ from those for other MBCS-style converters.
 *
 * _MBCSHeader.version 4.2 adds an optional conversion extension data structure.
 * If it is present, then an ICU version reading header versions 4.0 or 4.1
 * will be able to use the base table and ignore the extension.
 *
 * The unicodeMask in the static data is part of the base table data structure.
 * Especially, the UCNV_HAS_SUPPLEMENTARY flag determines the length of the
 * fromUnicode stage 1 array.
 * The static data unicodeMask refers only to the base table's properties if
 * a base table is included.
 * In an extension-only file, the static data unicodeMask is 0.
 * The extension data indexes have a separate field with the unicodeMask flags.
 *
 * MBCS-style data structure following the static data.
 * Offsets are counted in bytes from the beginning of the MBCS header structure.
 * Details about usage in comments in ucnvmbcs.c.
 *
 * struct _MBCSHeader (see the definition in this header file below)
 * contains 32-bit fields as follows:
 * 8 values:
 *  0   uint8_t[4]  MBCS version in UVersionInfo format (currently 4.2.0.0)
 *  1   uint32_t    countStates
 *  2   uint32_t    countToUFallbacks
 *  3   uint32_t    offsetToUCodeUnits
 *  4   uint32_t    offsetFromUTable
 *  5   uint32_t    offsetFromUBytes
 *  6   uint32_t    flags, bits:
 *                      31.. 8 offsetExtension -- _MBCSHeader.version 4.2 (ICU 2.8) and higher
 *                                                0 for older versions and if
 *                                                there is not extension structure
 *                       7.. 0 outputType
 *  7   uint32_t    fromUBytesLength -- _MBCSHeader.version 4.1 (ICU 2.4) and higher
 *                  counts bytes in fromUBytes[]
 *
 * if(outputType==MBCS_OUTPUT_EXT_ONLY) {
 *     -- base table name for extension-only table
 *     char baseTableName[variable]; -- with NUL plus padding for 4-alignment
 *
 *     -- all _MBCSHeader fields except for version and flags are 0
 * } else {
 *     -- normal base table with optional extension
 *
 *     int32_t stateTable[countStates][256];
 *    
 *     struct _MBCSToUFallback { (fallbacks are sorted by offset)
 *         uint32_t offset;
 *         UChar32 codePoint;
 *     } toUFallbacks[countToUFallbacks];
 *    
 *     uint16_t unicodeCodeUnits[(offsetFromUTable-offsetToUCodeUnits)/2];
 *                  (padded to an even number of units)
 *    
 *     -- stage 1 tables
 *     if(staticData.unicodeMask&UCNV_HAS_SUPPLEMENTARY) {
 *         -- stage 1 table for all of Unicode
 *         uint16_t fromUTable[0x440]; (32-bit-aligned)
 *     } else {
 *         -- BMP-only tables have a smaller stage 1 table
 *         uint16_t fromUTable[0x40]; (32-bit-aligned)
 *     }
 *    
 *     -- stage 2 tables
 *        length determined by top of stage 1 and bottom of stage 3 tables
 *     if(outputType==MBCS_OUTPUT_1) {
 *         -- SBCS: pure indexes
 *         uint16_t stage 2 indexes[?];
 *     } else {
 *         -- DBCS, MBCS, EBCDIC_STATEFUL, ...: roundtrip flags and indexes
 *         uint32_t stage 2 flags and indexes[?];
 *     }
 *    
 *     -- stage 3 tables with byte results
 *     if(outputType==MBCS_OUTPUT_1) {
 *         -- SBCS: each 16-bit result contains flags and the result byte, see ucnvmbcs.c
 *         uint16_t fromUBytes[fromUBytesLength/2];
 *     } else {
 *         -- DBCS, MBCS, EBCDIC_STATEFUL, ... 2/3/4 bytes result, see ucnvmbcs.c
 *         uint8_t fromUBytes[fromUBytesLength]; or
 *         uint16_t fromUBytes[fromUBytesLength/2]; or
 *         uint32_t fromUBytes[fromUBytesLength/4];
 *     }
 * }
 *
 * -- extension table, details see ucnv_ext.h
 * int32_t indexes[>=32]; ...
 */

/* MBCS converter data and state -------------------------------------------- */

enum {
    MBCS_MAX_STATE_COUNT=128
};

/**
 * MBCS action codes for conversions to Unicode.
 * These values are in bits 23..20 of the state table entries.
 */
enum {
    MBCS_STATE_VALID_DIRECT_16,
    MBCS_STATE_VALID_DIRECT_20,

    MBCS_STATE_FALLBACK_DIRECT_16,
    MBCS_STATE_FALLBACK_DIRECT_20,

    MBCS_STATE_VALID_16,
    MBCS_STATE_VALID_16_PAIR,

    MBCS_STATE_UNASSIGNED,
    MBCS_STATE_ILLEGAL,

    MBCS_STATE_CHANGE_ONLY
};

/* Macros for state table entries */
#define MBCS_ENTRY_TRANSITION(state, offset) (int32_t)(((int32_t)(state)<<24L)|(offset))
#define MBCS_ENTRY_TRANSITION_SET_OFFSET(entry, offset) (int32_t)(((entry)&0xff000000)|(offset))
#define MBCS_ENTRY_TRANSITION_ADD_OFFSET(entry, offset) (int32_t)((entry)+(offset))

#define MBCS_ENTRY_FINAL(state, action, value) (int32_t)(0x80000000|((int32_t)(state)<<24L)|((action)<<20L)|(value))
#define MBCS_ENTRY_SET_FINAL(entry) (int32_t)((entry)|0x80000000)
#define MBCS_ENTRY_FINAL_SET_ACTION(entry, action) (int32_t)(((entry)&0xff0fffff)|((int32_t)(action)<<20L))
#define MBCS_ENTRY_FINAL_SET_VALUE(entry, value) (int32_t)(((entry)&0xfff00000)|(value))
#define MBCS_ENTRY_FINAL_SET_ACTION_VALUE(entry, action, value) (int32_t)(((entry)&0xff000000)|((int32_t)(action)<<20L)|(value))

#define MBCS_ENTRY_SET_STATE(entry, state) (int32_t)(((entry)&0x80ffffff)|((int32_t)(state)<<24L))

#define MBCS_ENTRY_STATE(entry) (((entry)>>24)&0x7f)

#define MBCS_ENTRY_IS_TRANSITION(entry) ((entry)>=0)
#define MBCS_ENTRY_IS_FINAL(entry) ((entry)<0)

#define MBCS_ENTRY_TRANSITION_STATE(entry) ((entry)>>24)
#define MBCS_ENTRY_TRANSITION_OFFSET(entry) ((entry)&0xffffff)

#define MBCS_ENTRY_FINAL_STATE(entry) (((entry)>>24)&0x7f)
#define MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry) ((entry)<(int32_t)0x80100000)
#define MBCS_ENTRY_FINAL_ACTION(entry) (((entry)>>20)&0xf)
#define MBCS_ENTRY_FINAL_VALUE(entry) ((entry)&0xfffff)
#define MBCS_ENTRY_FINAL_VALUE_16(entry) (uint16_t)(entry)

/* single-byte fromUnicode: get the 16-bit result word */
#define MBCS_SINGLE_RESULT_FROM_U(table, results, c) (results)[ (table)[ (table)[(c)>>10] +(((c)>>4)&0x3f) ] +((c)&0xf) ]

/* multi-byte fromUnicode: get the 32-bit stage 2 entry */
#define MBCS_STAGE_2_FROM_U(table, c) ((const uint32_t *)(table))[ (table)[(c)>>10] +(((c)>>4)&0x3f) ]
#define MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c) ( ((stage2Entry) & ((uint32_t)1<< (16+((c)&0xf)) )) !=0)

#define MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c) ((uint16_t *)(bytes))[16*(uint32_t)(uint16_t)(stage2Entry)+((c)&0xf)]
#define MBCS_VALUE_4_FROM_STAGE_2(bytes, stage2Entry, c) ((uint32_t *)(bytes))[16*(uint32_t)(uint16_t)(stage2Entry)+((c)&0xf)]

#define MBCS_POINTER_3_FROM_STAGE_2(bytes, stage2Entry, c) ((bytes)+(16*(uint32_t)(uint16_t)(stage2Entry)+((c)&0xf))*3)


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
    MBCS_OUTPUT_2_HZ,       /* d */

    MBCS_OUTPUT_EXT_ONLY,   /* e */

    MBCS_OUTPUT_COUNT,

    MBCS_OUTPUT_DBCS_ONLY=0xdb  /* runtime-only type for DBCS-only handling of SISO tables */
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
    uint8_t countStates, dbcsOnlyState, stateTableOwned;
    uint32_t countToUFallbacks;

    const int32_t (*stateTable)/*[countStates]*/[256];
    int32_t (*swapLFNLStateTable)/*[countStates]*/[256]; /* for swaplfnl */
    const uint16_t *unicodeCodeUnits/*[countUnicodeResults]*/;
    const _MBCSToUFallback *toUFallbacks;

    /* fromUnicode */
    const uint16_t *fromUnicodeTable;
    const uint8_t *fromUnicodeBytes;
    uint8_t *swapLFNLFromUnicodeBytes; /* for swaplfnl */
    uint32_t fromUBytesLength;
    uint8_t outputType, unicodeMask;

    /* converter name for swaplfnl */
    char *swapLFNLName;

    /* extension data */
    struct UConverterSharedData *baseSharedData;
    const int32_t *extIndexes;
} UConverterMBCSTable;

/**
 * MBCS data header. See data format description above.
 */
typedef struct {
    UVersionInfo version;
    uint32_t countStates,
             countToUFallbacks,
             offsetToUCodeUnits,
             offsetFromUTable,
             offsetFromUBytes,
             flags,
             fromUBytesLength;
} _MBCSHeader;

/*
 * This is a simple version of _MBCSGetNextUChar() that is used
 * by other converter implementations.
 * It only returns an "assigned" result if it consumes the entire input.
 * It does not use state from the converter, nor error codes.
 * It does not handle the EBCDIC swaplfnl option (set in UConverter).
 * It handles conversion extensions but not GB 18030.
 *
 * Return value:
 * U+fffe   unassigned
 * U+ffff   illegal
 * otherwise the Unicode code point
 */
U_CFUNC UChar32
_MBCSSimpleGetNextUChar(UConverterSharedData *sharedData,
                        const char *source, int32_t length,
                        UBool useFallback);

/**
 * This version of _MBCSSimpleGetNextUChar() is optimized for single-byte, single-state codepages.
 * It does not handle the EBCDIC swaplfnl option (set in UConverter).
 * It does not handle conversion extensions (_extToU()).
 */
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
    (UChar)MBCS_ENTRY_FINAL_VALUE_16((sharedData)->mbcs.stateTable[0][(uint8_t)(b)])

/**
 * This is an internal function that allows other converter implementations
 * to check whether a byte is a lead byte.
 */
U_CFUNC UBool
_MBCSIsLeadByte(UConverterSharedData *sharedData, char byte);

/** This is a macro version of _MBCSIsLeadByte(). */
#define _MBCS_IS_LEAD_BYTE(sharedData, byte) \
    (UBool)MBCS_ENTRY_IS_TRANSITION((sharedData)->mbcs.stateTable[0][(uint8_t)(byte)])

/*
 * This is another simple conversion function for internal use by other
 * conversion implementations.
 * It does not use the converter state nor call callbacks.
 * It does not handle the EBCDIC swaplfnl option (set in UConverter).
 * It handles conversion extensions but not GB 18030.
 *
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
 * It does not handle the EBCDIC swaplfnl option (set in UConverter).
 *
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

U_CFUNC void 
_MBCSFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode);
U_CFUNC void 
_MBCSToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                          UErrorCode *pErrorCode);

/*
 * Internal function returning a UnicodeSet for toUnicode() conversion.
 * Currently only used for ISO-2022-CN, and only handles roundtrip mappings.
 * In the future, if we add support for reverse-fallback sets, this function
 * needs to be updated, and called for each initial state.
 * Does not currently handle extensions.
 * Does not empty the set first.
 */
U_CFUNC void
_MBCSGetUnicodeSetForBytes(const UConverterSharedData *sharedData,
                           USet *set,
                           UConverterUnicodeSet which,
                           uint8_t state, int32_t lowByte, int32_t highByte,
                           UErrorCode *pErrorCode);

/*
 * Internal function returning a UnicodeSet for toUnicode() conversion.
 * Currently only used for ISO-2022-CN, and only handles roundtrip mappings.
 * In the future, if we add support for fallback sets, this function
 * needs to be updated.
 * Handles extensions.
 * Does not empty the set first.
 */
U_CFUNC void
_MBCSGetUnicodeSetForUnicode(const UConverterSharedData *sharedData,
                             USet *set,
                             UConverterUnicodeSet which,
                             UErrorCode *pErrorCode);

#endif
