/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucm.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003jun20
*   created by: Markus W. Scherer
*
*   Definitions for the .ucm file parser and handler module ucm.c.
*/

#ifndef __UCM_H__
#define __UCM_H__

#include "unicode/utypes.h"
#include "ucnvmbcs.h"
#include "ucnv_ext.h"
#include <stdio.h>

U_CDECL_BEGIN

/*
 * Per-mapping data structure
 *
 * u if uLen==1: Unicode code point
 *   else index to uLen code points
 * b if bLen<=4: up to 4 bytes
 *   else index to bLen bytes
 * uLen number of code points
 * bLen number of words containing left-justified bytes
 * bIsMultipleChars indicates that the bytes contain more than one sequence
 *                  according to the state table
 * f flag for roundtrip (0), fallback (1), sub mapping (2), reverse fallback (3)
 *   same values as in the source file after |
 */
typedef struct UCMapping {
    UChar32 u;
    union {
        uint32_t index;
        uint8_t bytes[4];
    } b;
    int8_t uLen, bLen, f;
} UCMapping;

enum {
    UCM_FLAGS_INITIAL,  /* no mappings parsed yet */
    UCM_FLAGS_EXPLICIT, /* .ucm file has mappings with | fallback indicators */
    UCM_FLAGS_IMPLICIT, /* .ucm file has mappings without | fallback indicators, later wins */
    UCM_FLAGS_MIXED     /* both implicit and explicit */
};

typedef struct UCMTable {
    UCMapping *mappings;
    int32_t mappingsCapacity, mappingsLength;

    UChar32 *codePoints;
    int32_t codePointsCapacity, codePointsLength;

    uint8_t *bytes;
    int32_t bytesCapacity, bytesLength;

    /* index map for mapping by bytes first */
    int32_t *reverseMap;

    uint8_t unicodeMask;
    int8_t flagsType; /* UCM_FLAGS_INITIAL etc. */
} UCMTable;

enum {
    MBCS_STATE_FLAG_DIRECT=1,
    MBCS_STATE_FLAG_SURROGATES,

    MBCS_STATE_FLAG_READY=16
};

typedef struct UCMStates {
    int32_t stateTable[MBCS_MAX_STATE_COUNT][256];
    uint32_t stateFlags[MBCS_MAX_STATE_COUNT],
             stateOffsetSum[MBCS_MAX_STATE_COUNT];

    int32_t countStates, minCharLength, maxCharLength, countToUCodeUnits;
    int8_t conversionType, outputType;
} UCMStates;

typedef struct UCMFile {
    UCMTable *base, *ext;
    UCMStates states;

    char baseName[UCNV_MAX_CONVERTER_NAME_LENGTH];
} UCMFile;

/* simple accesses ---------------------------------------------------------- */

#define UCM_GET_CODE_POINTS(t, m) \
    (((m)->uLen==1) ? &(m)->u : (t)->codePoints+(m)->u)

#define UCM_GET_BYTES(t, m) \
    (((m)->bLen<=4) ? (m)->b.bytes : (t)->bytes+(m)->b.index)

/* APIs --------------------------------------------------------------------- */

U_CAPI UCMFile * U_EXPORT2
ucm_open(void);

U_CAPI void U_EXPORT2
ucm_close(UCMFile *ucm);

U_CAPI UBool U_EXPORT2
ucm_parseHeaderLine(UCMFile *ucm,
                    char *line, char **pKey, char **pValue);

/* @return -1 illegal bytes  0 suitable for base table  1 needs to go into extension table */
U_CAPI int32_t U_EXPORT2
ucm_mappingType(UCMStates *baseStates,
                UCMapping *m,
                UChar32 codePoints[UCNV_EXT_MAX_UCHARS],
                uint8_t bytes[UCNV_EXT_MAX_BYTES]);

/* add a mapping to the base or extension table as appropriate */
U_CAPI UBool U_EXPORT2
ucm_addMappingAuto(UCMFile *ucm, UBool forBase, UCMStates *baseStates,
                   UCMapping *m,
                   UChar32 codePoints[UCNV_EXT_MAX_UCHARS],
                   uint8_t bytes[UCNV_EXT_MAX_BYTES]);

U_CAPI UBool U_EXPORT2
ucm_addMappingFromLine(UCMFile *ucm, const char *line, UBool forBase, UCMStates *baseStates);


U_CAPI UCMTable * U_EXPORT2
ucm_openTable(void);

U_CAPI void U_EXPORT2
ucm_closeTable(UCMTable *table);

U_CAPI void U_EXPORT2
ucm_sortTable(UCMTable *t);

/**
 * Check the validity of mappings against a base table's states;
 * necessary for extension-only tables that were read before their base tables.
 */
U_CAPI UBool U_EXPORT2
ucm_checkValidity(UCMTable *ext, UCMStates *baseStates);

/**
 * Check a base table against an extension table.
 * Set moveToExt=TRUE for where base and extension tables are parsed
 * from a single file,
 * and moveToExt=FALSE for where the extension table is in a separate file.
 *
 * For both tables in the same file, the extension table is automatically
 * built.
 * For separate files, the extension file can use a complete mapping table,
 * so that common mappings need not be stripped out manually.
 *
 *
 * Sort both tables, and then for each mapping direction:
 *
 * If the base table contains a mapping for which the input sequence is
 * the same as the extension input, then
 * - if the output is the same: remove the extension mapping
 * - else: error
 *
 * If the base table contains a mapping for which the input sequence is
 * a prefix of the extension input, then
 * - if moveToExt: move the base mapping to the extension table
 * - else: error
 *
 * @return FALSE in case of an irreparable error
 */
U_CAPI UBool U_EXPORT2
ucm_checkBaseExt(UCMStates *baseStates, UCMTable *base, UCMTable *ext, UBool moveToExt);

U_CAPI void U_EXPORT2
ucm_printTable(UCMTable *table, FILE *f, UBool byUnicode);

U_CAPI void U_EXPORT2
ucm_printMapping(UCMTable *table, UCMapping *m, FILE *f);


U_CAPI void U_EXPORT2
ucm_addState(UCMStates *states, const char *s);

U_CAPI void U_EXPORT2
ucm_processStates(UCMStates *states);

U_CAPI int32_t U_EXPORT2
ucm_countChars(UCMStates *states,
               const uint8_t *bytes, int32_t length);


U_CAPI int8_t U_EXPORT2
ucm_parseBytes(uint8_t bytes[UCNV_EXT_MAX_BYTES], const char *line, const char **ps);

U_CAPI UBool U_EXPORT2
ucm_parseMappingLine(UCMapping *m,
                     UChar32 codePoints[UCNV_EXT_MAX_UCHARS],
                     uint8_t bytes[UCNV_EXT_MAX_BYTES],
                     const char *line);

U_CAPI void U_EXPORT2
ucm_addMapping(UCMTable *table,
               UCMapping *m,
               UChar32 codePoints[UCNV_EXT_MAX_UCHARS],
               uint8_t bytes[UCNV_EXT_MAX_BYTES]);

/* very makeconv-specific functions ----------------------------------------- */

/* finalize and optimize states after the toUnicode mappings are processed */
U_CAPI void U_EXPORT2
ucm_optimizeStates(UCMStates *states,
                   uint16_t **pUnicodeCodeUnits,
                   _MBCSToUFallback *toUFallbacks, int32_t countToUFallbacks,
                   UBool verbose);

/* moved here because it is used inside ucmstate.c */
U_CAPI int32_t U_EXPORT2
ucm_findFallback(_MBCSToUFallback *toUFallbacks, int32_t countToUFallbacks,
                 uint32_t offset);

/* very rptp2ucm-specific functions ----------------------------------------- */

/*
 * Input: Separate tables with mappings from/to Unicode,
 * subchar and subchar1 (0 if none).
 * All mappings must have flag 0.
 *
 * Output: fromUTable will contain the union of mappings with the correct
 * precision flags, and be sorted.
 */
U_CAPI void U_EXPORT2
ucm_mergeTables(UCMTable *fromUTable, UCMTable *toUTable,
                const uint8_t *subchar, int32_t subcharLength,
                uint8_t subchar1);

U_CAPI UBool U_EXPORT2
ucm_separateMappings(UCMFile *ucm, UBool isSISO);

U_CDECL_END

#endif
