/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  ucnvmbcs.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000jul03
*   created by: Markus W. Scherer
*
*   The current code in this file replaces the previous implementation
*   of conversion code from multi-byte codepages to Unicode and back.
*   This implementation supports the following:
*   - legacy variable-length codepages with up to 4 bytes per character
*   - all Unicode code points (up to 0x10ffff)
*   - efficient distinction of unassigned vs. illegal byte sequences
*   - it is possible in fromUnicode() to directly deal with simple
*     stateful encodings
*   - it is possible to convert Unicode code points other than U+0000
*     to a single zero byte (but not as a fallback)
*
*   Remaining limitations in fromUnicode:
*   - byte sequences must not have leading zero bytes
*   - no fallback mapping from Unicode to a zero byte
*   - limitation to up to 4 bytes per character
*/

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_cb.h"
#include "unicode/udata.h"
#include "ucnv_bld.h"
#include "ucnvmbcs.h"
#include "ucnv_cnv.h"
#include "cstring.h"

/*
 * _MBCSHeader versions 1 to 3
 * (Note that the _MBCSHeader version is in addition to the converter formatVersion.)
 *
 * Converting stateless codepage data ---------------------------------------***
 * (or codepage data with simple states) to Unicode.
 *
 * Data structure and algorithm for converting from complex legacy codepages
 * to Unicode. (Designed before 2000-may-22.)
 *
 * The basic idea is that the structure of legacy codepages can be described
 * with state tables.
 * When reading a byte stream, each input byte causes a state transition.
 * Some transitions result in the output of a code point, some result in
 * "unassigned" or "illegal" output.
 * This is used here for character conversion.
 *
 * The data structure begins with a state table consisting of a row
 * per state, with 256 entries (columns) per row for each possible input
 * byte value.
 * Each entry is 32 bits wide, with the lower 7 bits containing the next state.
 * State 0 is the initial state.
 *
 * Bit 31 of each entry indicates whether the state is
 * terminal (bit 31 set) or not.
 *
 * Most of the time, the offset values of subsequent states are added
 * up to a scalar value. This value will eventually be the index of
 * the Unicode code point in a table that follows the state table.
 * The effect is that the code points for final state table rows
 * are contiguous. The code points of final state rows follow each other
 * in the order of the references to those final states by previous
 * states, etc.
 *
 * For some terminal states, the offset is itself the output Unicode
 * code point (16 bits for a BMP code point or 20 bits for a code point
 * that is written as a surrogate pair).
 * For others, the code point in the Unicode table is stored with either
 * one or two code units: one for BMP code points, two for a pair of
 * surrogates.
 * All code points for a final table take up the same number of code
 * units, regardless of whether they all actually _use_ the same number
 * of code units. This is necessary for simple array access.
 *
 * An additional feature comes in with what in ICU is called "fallback"
 * mappings:
 * In addition to round-trippable, precise, 1:1 mappings, there are often
 * mappings defined between similar, though not the same, characters.
 * Typically, such mappings occur only in fromUnicode mapping tables because
 * Unicode has a superset repertoire of most other codepages. However, it
 * is possible to provide such mappings in the toUnicode tables, too.
 * In this case, the fallback mappings are partly integrated into the
 * general state tables because the structure of the encoding includes their
 * byte sequences. They are optional mappings when the main mapping is
 * "unassigned", and are looked up by the scalar offset of the main mapping
 * in a separate table. Only when the main mapping does not have such a
 * scalar offset, i.e., in the case of action codes 5 of 6 below (valid-direct),
 * would there need to be some different mechanism. Therefore, there are
 * separate action codes 3 and 4 (fallback-direct) especially for that.
 * The "unassigned" action code 2 cannot be used for fallback lookups because
 * it also does not result in a scalar offset. This means that fallback mappings
 * require to fit into either fallback-direct action codes or valid-single or
 * valid-pair codes that result in scalar offsets.
 * "Unassigned" really means "structurally unassigned".
 *
 * The interpretation of the bits in each entry is as follows:
 *
 * Bit 31 not set, not a terminal entry:
 * 30..7  offset delta, to be added up
 *  6..0  next state
 *
 * Bit 31 set, terminal entry:
 * 30..27 action code:
 *        0  illegal byte sequence
 *           26..23 not used, 0
 *           22..7  16-bit Unicode BMP code point U+ffff (new with version 2)
 *        1  state change only
 *           26..7  not used, 0
 *           useful for state changes in simple stateful encodings,
 *           at Shift-In/Shift-Out codes
 *        2  unassigned byte sequence
 *           26..23 not used, 0
 *           22..7  16-bit Unicode BMP code point U+ffff (new with version 2)
 *                  this does not contain a final offset delta because the main
 *                  purpose of this action code is to save scalar offset values;
 *                  therefore, fallback values cannot be assigned to byte
 *                  sequences that result in this action code - use codes 5 or 6
 *
 *        action codes 3 and 4 result in fallback (unidirectional-mapping) Unicode code points
 *        3  valid byte sequence (fallback)
 *           26..23 not used, 0
 *           22..7  16-bit Unicode BMP code point as fallback result
 *        4  valid byte sequence (fallback)
 *           26..7  20-bit Unicode surrogate code point as fallback result
 *
 *        action codes 5, 6, 7, and 8 result in precise-mapping Unicode code points
 *        5  valid byte sequence
 *           26..23 not used, 0
 *           22..7  16-bit Unicode BMP code point
 *                  never U+fffe or U+ffff (use action codes 0, 2, 3 or 4 for that)
 *        6  valid byte sequence
 *           26..7  20-bit Unicode surrogate code point
 *                  never U+fffe or U+ffff (use action codes 0, 2, 3 or 4 for that)
 *
 *        action codes 7 and 8 may result in U+fffe (unassigned), in which case the
 *        final offset is to be looked up in a special fallback table
 *        7  valid byte sequence
 *           26..16 not used, 0
 *           15..7  final offset delta
 *                  pointing to one 16-bit code unit
 *                  which may be U+fffe (unassigned) or U+ffff (illegal)
 *        8  valid byte sequence
 *           26..16 not used, 0
 *           15..7  final offset delta
 *                  pointing to two 16-bit code units
 *                  (UTF-16 surrogates)
 *                  the first code unit either is a lead surrogate and indicates
 *                  an assigned surrogate pair, or it is a single unit
 *                  which may be U+fffe (unassigned) or U+ffff (illegal)
 *           (the final offset deltas are at most 255 * 2,
 *            times 2 because of storing code unit pairs)
 *
 *        9..15 reserved for future use
 *           current implementations will only perform a state change
 *           and ignore bits 26..7
 *  6..0  next state (regardless of action code)
 *
 * An encoding with contiguous ranges of unassigned byte sequences, like
 * Shift-JIS and especially EUC-TW, can be stored efficiently by having
 * at least two states for the trail bytes:
 * One trail byte state that results in code points, and one that only
 * has "unassigned" and "illegal" terminal states.
 *
 * Note: partly by accident, this data structure supports simple stateless
 * encodings without any additional logic.
 * Especially simple Shift-In/Shift-Out schemes could be handled with
 * appropriate state tables (especially EBCDIC_STATEFUL!).
 *
 * MBCS version 2 added:
 * unassigned and illegal action codes have U+fffe and U+ffff
 * instead of unused bits; this is useful for _MBCS_SINGLE_SIMPLE_GET_NEXT_BMP()
 *
 * Converting from Unicode to codepage bytes --------------------------------***
 *
 * The conversion data structure for fromUnicode is designed for the known
 * structure of Unicode. It maps from 21-bit code points (0..0x10ffff) to
 * a sequence of 1..4 bytes, in addition to a flag that indicates if there is
 * a roundtrip mapping.
 *
 * The lookup is done with a 3-stage trie, using 11/6/4 bits for stage 1/2/3
 * like in the character properties table.
 * The beginning of the trie is at offsetFromUTable, the beginning of stage 3
 * with the resulting bytes is at offsetFromUBytes.
 *
 * The trie lookup works as follows:
 * Stage 1/2: set i to the first of 2 entries in the second stage
 *     (_MBCSHeader version 3)
 *     uint32_t i=MBCS_STAGE_2_MULTIPLIER*(uint32_t)table[c>>10]+2*((c>>4)&0x3f);
 *     // i is an index into stage 2 based on table=offsetFromUTable=start of stage 1
 *
 *     (_MBCSHeader version 1 & 2)
 *     uint32_t i=0x440+2*((uint32_t)table[c>>10]+((c>>4)&0x3f));
 *     // i is an index into stage 2 based on table=offsetFromUTable=start of stage 1
 *
 * Note that stage 1 always contains 0x440=1088 entries (0x440==0x110000>>10),
 * or (version 3) for BMP-only codepages, it contains 64 entries.
 *
 * In version 3, stage 2 blocks may overlap by multiples of the multiplier
 * for compaction.
 *
 * Is this an assigned code point?
 * The following test is done to determine if the code point has a roundtrip
 * assignment. If not, then a fallback mapping might be stored.
 * It will be retrieved if fallbacks are used.
 *     is-roundtrip-assigned=(table[i]&(1<<(c&0xf)))!=0
 *
 * If the value is to be retrieved, then it is looked up in the bytes table
 * depending on the number of bytes that are stored per character:
 *     p=bytes array;
 *     // [i+1]: advance beyond assignment flags in stage 2 to the bytes index
 *     p+=(16*(uint32_t)table[i+1]+(c&0xf))*bytes-stored-per-character;
 *
 * That is, the bytes array index is 16 times the index from stage 2,
 * plus the last 4 bits from the code point, and indexing as many bytes
 * per entry as are stored per character.
 * Leading zeros are then removed, and the number of bytes is counted.
 * A zero byte mapping result is possible as a roundtrip result.
 * For some output types, the actual result is processed from this;
 * see _MBCSFromUnicodeWithOffsets().
 *
 * MBCS version 2 added:
 * the converter checks for known output types, which allows
 * adding new ones without crashing an unaware converter
 *
 * ---
 * Reasons for the multiplier MBCS_STAGE_2_MULTIPLIER=4
 * in the calculation of the stage 2 index above:
 * ->It must be a power of two.
 *
 * The stage 1 entry needs to be able to address blocks of 64 stage 2 entries each,
 * with 2 16-bit words per entry: stage 2 blocks have 128 16-bit entries.
 * ->The multiplier must be at most 128 to address each block.
 *
 * Beginning with version 3, stage 1 entries are based on the beginning of the
 * stage table, which means the beginning of stage 1, not the beginning of stage 2.
 * This means that the first stage 2 block begins at the 1088th (1088=17*64)
 * or 64th (BMP-only codepages) 16-bit table entry.
 * ->Therefore, the multiplier must be at most 64 to address the first block.
 *
 * Stages 1 & 2 contain 16-bit entries for indexing. Stage 3 can hold up to
 * 1M byte result sequences. Stage 2 entries index 16-entry blocks in stage 3,
 * which means that all stage 2 blocks index up to 64k stage 3 blocks.
 * This means that there may be as many as 128k entries in stage 2, in
 * up to 1k stage 2 blocks.
 * ->The 16-bit indexes in stage 1 must be multiplied by at least 2 to reach
 * all of stage 2.
 *
 * Since with version 3 the stage 2 indexes are offset by the length of stage 1,
 * ->the multiplier must be at least the next greater power of 2, which is 4.
 *
 * This results in a possible range for the mutliplier from 4 to 64.
 * ->A small multiplier allows stage 2 blocks to overlap for compaction.
 * 4 is chosen.
 */

/* prototypes --------------------------------------------------------------- */

U_CFUNC void
_MBCSSingleToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                                UErrorCode *pErrorCode);

U_CFUNC void
_MBCSSingleToBMPWithOffsets(UConverterToUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode);

U_CFUNC UChar32
_MBCSSingleGetNextUChar(UConverterToUnicodeArgs *pArgs,
                  UErrorCode *pErrorCode);

U_CFUNC UChar32
_MBCSSingleSimpleGetNextUChar(UConverterSharedData *sharedData,
                              uint8_t b, UBool useFallback);

U_CFUNC void
_MBCSSingleFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                                  UErrorCode *pErrorCode);

U_CFUNC void
_MBCSSingleFromBMPWithOffsets(UConverterFromUnicodeArgs *pArgs,
                              UErrorCode *pErrorCode);

static void
fromUCallback(UConverter *cnv,
              void *context, UConverterFromUnicodeArgs *pArgs,
              const UChar *codeUnits, int32_t length, UChar32 codePoint,
              UConverterCallbackReason reason, UErrorCode *pErrorCode);

static void
toUCallback(UConverter *cnv,
            void *context, UConverterToUnicodeArgs *pArgs,
            const char *codeUnits, int32_t length,
            UConverterCallbackReason reason, UErrorCode *pErrorCode);

/* GB 18030 data ------------------------------------------------------------ */

/* helper macros for linear values for GB 18030 four-byte sequences */
#define LINEAR_18030(a, b, c, d) ((((a)*10+(b))*126L+(c))*10L+(d))

#define LINEAR_18030_BASE LINEAR_18030(0x81, 0x30, 0x81, 0x30)

#define LINEAR(x) LINEAR_18030(x>>24, (x>>16)&0xff, (x>>8)&0xff, x&0xff)

/*
 * Some ranges of GB 18030 where both the Unicode code points and the
 * GB four-byte sequences are contiguous and are handled algorithmically by
 * the special callback functions below.
 * The values are start & end of Unicode & GB codes.
 *
 * Note that single surrogates are not mapped by GB 18030
 * as of the re-released mapping tables from 2000-nov-30.
 */
static const uint32_t
gb18030Ranges[13][4]={
    0x10000, 0x10FFFF, LINEAR(0x90308130), LINEAR(0xE3329A35),
    0x9FA6, 0xD7FF, LINEAR(0x82358F33), LINEAR(0x8336C738),
    0x0452, 0x200F, LINEAR(0x8130D330), LINEAR(0x8136A531),
    0xE865, 0xF92B, LINEAR(0x8336D030), LINEAR(0x84308534),
    0x2643, 0x2E80, LINEAR(0x8137A839), LINEAR(0x8138FD38),
    0xFA2A, 0xFE2F, LINEAR(0x84309C38), LINEAR(0x84318537),
    0x3CE1, 0x4055, LINEAR(0x8231D438), LINEAR(0x8232AF32),
    0x361B, 0x3917, LINEAR(0x8230A633), LINEAR(0x8230F237),
    0x49B8, 0x4C76, LINEAR(0x8234A131), LINEAR(0x8234E733),
    0x4160, 0x4336, LINEAR(0x8232C937), LINEAR(0x8232F837),
    0x478E, 0x4946, LINEAR(0x8233E838), LINEAR(0x82349638),
    0x44D7, 0x464B, LINEAR(0x8233A339), LINEAR(0x8233C931),
    0xFFE6, 0xFFFF, LINEAR(0x8431A234), LINEAR(0x8431A439)
};

/* MBCS setup functions ----------------------------------------------------- */

U_CFUNC void
_MBCSLoad(UConverterSharedData *sharedData,
          const uint8_t *raw,
          UErrorCode *pErrorCode) {
    UDataInfo info;
    UConverterMBCSTable *mbcsTable=&sharedData->table->mbcs;
    _MBCSHeader *header=(_MBCSHeader *)raw;

    if(header->version[0]!=3) {
        *pErrorCode=U_INVALID_TABLE_FORMAT;
        return;
    }

    mbcsTable->countStates=(uint8_t)header->countStates;
    mbcsTable->countToUFallbacks=header->countToUFallbacks;
    mbcsTable->stateTable=(const int32_t (*)[256])(raw+sizeof(_MBCSHeader));
    mbcsTable->toUFallbacks=(const _MBCSToUFallback *)(mbcsTable->stateTable+header->countStates);
    mbcsTable->unicodeCodeUnits=(const uint16_t *)(raw+header->offsetToUCodeUnits);

    mbcsTable->fromUnicodeTable=(const uint16_t *)(raw+header->offsetFromUTable);
    mbcsTable->fromUnicodeBytes=(const uint8_t *)(raw+header->offsetFromUBytes);
    mbcsTable->outputType=(uint8_t)header->flags;

    /* make sure that the output type is known */
    switch(mbcsTable->outputType) {
    case MBCS_OUTPUT_1:
    case MBCS_OUTPUT_2:
    case MBCS_OUTPUT_3:
    case MBCS_OUTPUT_4:
    case MBCS_OUTPUT_3_EUC:
    case MBCS_OUTPUT_4_EUC:
    case MBCS_OUTPUT_2_SISO:
        /* OK */
        break;
    default:
        *pErrorCode=U_INVALID_TABLE_FORMAT;
        return;
    }

    /*
     * converter versions 6.1 and up contain a unicodeMask that is
     * used here to select the most efficient function implementations
     */
    info.size=sizeof(UDataInfo);
    udata_getInfo((UDataMemory *)sharedData->dataMemory, &info);
    if(info.formatVersion[0]>6 || info.formatVersion[0]==6 && info.formatVersion[1]>=1) {
        /* mask off possible future extensions to be safe */
        mbcsTable->unicodeMask=sharedData->staticData->unicodeMask&3;
    } else {
        /* for older versions, assume worst case: contains anything possible (prevent over-optimizations) */
        mbcsTable->unicodeMask=UCNV_HAS_SUPPLEMENTARY|UCNV_HAS_SURROGATES;
    }
}

U_CFUNC void
_MBCSReset(UConverter *cnv, UConverterResetChoice choice) {
    if(choice<=UCNV_RESET_TO_UNICODE) {
        /* toUnicode */
        cnv->toUnicodeStatus=0;     /* offset */
        cnv->mode=0;                /* state */
        cnv->toULength=0;           /* byteIndex */
    }
    if(choice!=UCNV_RESET_TO_UNICODE) {
        /* fromUnicode */
        cnv->fromUSurrogateLead=0;
        cnv->fromUnicodeStatus=1;   /* prevLength */
    }
}

U_CFUNC void
_MBCSOpen(UConverter *cnv,
          const char *name,
          const char *locale,
          uint32_t options,
          UErrorCode *pErrorCode) {
    _MBCSReset(cnv, UCNV_RESET_BOTH);
    if(uprv_strstr(name, "gb18030")!=NULL || uprv_strstr(name, "GB18030")!=NULL) {
        /* set a flag for GB 18030 mode, which changes the callback behavior */
        cnv->extraInfo=(void *)gb18030Ranges;
    }
}

/* MBCS-to-Unicode conversion functions ------------------------------------- */

static UChar32
_MBCSGetFallback(UConverterMBCSTable *mbcsTable, uint32_t offset) {
    const _MBCSToUFallback *toUFallbacks;
    uint32_t i, start, limit;

    limit=mbcsTable->countToUFallbacks;
    if(limit>0) {
        /* do a binary search for the fallback mapping */
        toUFallbacks=mbcsTable->toUFallbacks;
        start=0;
        while(start<limit-1) {
            i=(start+limit)/2;
            if(offset<toUFallbacks[i].offset) {
                limit=i;
            } else {
                start=i;
            }
        }

        /* did we really find it? */
        if(offset==toUFallbacks[start].offset) {
            return toUFallbacks[start].codePoint;
        }
    }

    return 0xfffe;
}

U_CFUNC void
_MBCSToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                          UErrorCode *pErrorCode) {
    UConverter *cnv;
    const uint8_t *source, *sourceLimit;
    UChar *target;
    const UChar *targetLimit;
    int32_t *offsets;

    const int32_t (*stateTable)[256];
    const uint16_t *unicodeCodeUnits;

    uint32_t offset;
    uint8_t state;
    int8_t byteIndex;
    uint8_t *bytes;

    int32_t sourceIndex, nextSourceIndex;

    int32_t entry;
    UChar c;
    uint8_t b;
    UConverterCallbackReason reason;

    /* use optimized function if possible */
    cnv=pArgs->converter;
    if(cnv->sharedData->table->mbcs.countStates==1) {
        if(!(cnv->sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
            _MBCSSingleToBMPWithOffsets(pArgs, pErrorCode);
        } else {
            _MBCSSingleToUnicodeWithOffsets(pArgs, pErrorCode);
        }
        return;
    }

    /* set up the local pointers */
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;
    target=pArgs->target;
    targetLimit=pArgs->targetLimit;
    offsets=pArgs->offsets;

    stateTable=cnv->sharedData->table->mbcs.stateTable;
    unicodeCodeUnits=cnv->sharedData->table->mbcs.unicodeCodeUnits;

    /* get the converter state from UConverter */
    offset=cnv->toUnicodeStatus;
    state=(uint8_t)(cnv->mode);
    byteIndex=cnv->toULength;
    bytes=cnv->toUBytes;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex=byteIndex==0 ? 0 : -1;
    nextSourceIndex=0;

    /* conversion loop */
    while(source<sourceLimit) {
        /*
         * This following test is to see if available input would overflow the output.
         * It does not catch output of more than one code unit that
         * overflows as a result of a surrogate pair or callback output
         * from the last source byte.
         * Therefore, those situations also test for overflows and will
         * then break the loop, too.
         */
        if(target<targetLimit) {
            bytes[byteIndex++]=b=*source++;
            ++nextSourceIndex;
            entry=stateTable[state][b];
            if(entry>=0) {
                /*
                 * bit 31 is not set, bits:
                 * 30..7  offset delta
                 *  6..0  next state
                 */
                state=(uint8_t)(entry&0x7f);
                offset+=entry>>7;
            } else {
                /*
                 * bit 31 is set, bits:
                 * 30..27 action code
                 *        (do not mask out bit 31 for speed, include it in action values)
                 * 26..7  depend on the action code
                 *  6..0  next state
                 */

                /* set the next state early so that we can reuse the entry variable */
                state=(uint8_t)(entry&0x7f); /* typically 0 */

                /* switch per action code */
                switch((uint32_t)entry>>27U) {
                case 16|MBCS_STATE_ILLEGAL:
                    /* bits 26..7 are not used, 0 */
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                    goto callback;
                case 16|MBCS_STATE_CHANGE_ONLY:
                    /* bits 26..7 are not used, 0 */
                    /*
                     * This serves as a state change without any output.
                     * It is useful for reading simple stateful encodings,
                     * for example using just Shift-In/Shift-Out codes.
                     * The 21 unused bits may later be used for more sophisticated
                     * state transitions.
                     */
                    break;
                case 16|MBCS_STATE_UNASSIGNED:
                    /* bits 26..7 are not used, 0 */
                    /* callback(unassigned) */
                    reason=UCNV_UNASSIGNED;
                    *pErrorCode=U_INVALID_CHAR_FOUND;
                    goto callback;
                case 16|MBCS_STATE_FALLBACK_DIRECT_16:
                    /* bits 26..23 are not used, 0 */
                    /* bits 22..7 contain the Unicode BMP code point */
                    if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                        /* callback(unassigned) */
                        reason=UCNV_UNASSIGNED;
                        *pErrorCode=U_INVALID_CHAR_FOUND;
                        goto callback;
                    }
                    /* fall through to the MBCS_STATE_VALID_DIRECT_16 branch */
                case 16|MBCS_STATE_VALID_DIRECT_16:
                    /* bits 26..23 are not used, 0 */
                    /* bits 22..7 contain the Unicode BMP code point */
                    /* output BMP code point */
                    *target++=(UChar)(entry>>7);
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                    break;
                case 16|MBCS_STATE_FALLBACK_DIRECT_20:
                    /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
                    if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                        /* callback(unassigned) */
                        reason=UCNV_UNASSIGNED;
                        *pErrorCode=U_INVALID_CHAR_FOUND;
                        goto callback;
                    }
                    /* fall through to the MBCS_STATE_VALID_DIRECT_20 branch */
                case 16|MBCS_STATE_VALID_DIRECT_20:
                    /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
                    entry=(entry>>7)&0xfffff;
                    /* output surrogate pair */
                    *target++=(UChar)(0xd800|(UChar)(entry>>10));
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                    c=(UChar)(0xdc00|(UChar)(entry&0x3ff));
                    if(target<targetLimit) {
                        *target++=c;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                        }
                    } else {
                        /* target overflow */
                        cnv->UCharErrorBuffer[0]=c;
                        cnv->UCharErrorBufferLength=1;
                        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;

                        offset=0;
                        byteIndex=0;
                        goto endloop;
                    }
                    break;
                case 16|MBCS_STATE_VALID_16:
                    /* bits 26..16 are not used, 0 */
                    /* bits 15..7 contain the final offset delta to one 16-bit code unit */
                    offset+=(uint16_t)entry>>7;
                    c=unicodeCodeUnits[offset];
                    if(c<0xfffe) {
                        /* output BMP code point */
                        *target++=c;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                        }
                    } else if(c==0xfffe) {
                        if(UCNV_TO_U_USE_FALLBACK(cnv) && (entry=(int32_t)_MBCSGetFallback(&cnv->sharedData->table->mbcs, offset))!=0xfffe) {
                            goto output32;
                        }
                        /* callback(unassigned) */
                        reason=UCNV_UNASSIGNED;
                        *pErrorCode=U_INVALID_CHAR_FOUND;
                        goto callback;
                    } else {
                        /* callback(illegal) */
                        reason=UCNV_ILLEGAL;
                        *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                        goto callback;
                    }
                    break;
                case 16|MBCS_STATE_VALID_16_PAIR:
                    /* bits 26..16 are not used, 0 */
                    /* bits 15..7 contain the final offset delta to two 16-bit code units */
                    offset+=(uint16_t)entry>>7;
                    c=unicodeCodeUnits[offset++];
                    if(UTF_IS_FIRST_SURROGATE(c)) {
                        *target++=c;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                        }
                        if(target<targetLimit) {
                            *target++=unicodeCodeUnits[offset];
                            if(offsets!=NULL) {
                                *offsets++=sourceIndex;
                            }
                        } else {
                            /* target overflow */
                            cnv->UCharErrorBuffer[0]=unicodeCodeUnits[offset];
                            cnv->UCharErrorBufferLength=1;
                            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;

                            offset=0;
                            byteIndex=0;
                            goto endloop;
                        }
                    } else if(c<0xfffe) {
                        /* output BMP code point */
                        *target++=c;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                        }
                    } else if(c==0xfffe) {
                        /*
                         * For the fallback, we need to restore the offset that
                         * we had before the unicodeCodeUnits[offset++] above that incremented it!
                         */
                        if(UCNV_TO_U_USE_FALLBACK(cnv) && (entry=(int32_t)_MBCSGetFallback(&cnv->sharedData->table->mbcs, offset-1))!=0xfffe) {
                            goto output32;
                        }
                        /* callback(unassigned) */
                        reason=UCNV_UNASSIGNED;
                        *pErrorCode=U_INVALID_CHAR_FOUND;
                        goto callback;
                    } else {
                        /* callback(illegal) */
                        reason=UCNV_ILLEGAL;
                        *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                        goto callback;
                    }
                    break;
                default:
                    /* reserved, must never occur */
                    /* bits 26..7 are not used, 0 */
                    break;
                }

                /* normal end of action codes: prepare for a new character */
                offset=0;
                byteIndex=0;
                sourceIndex=nextSourceIndex;
                continue;

                /*
                 * Markus Scherer 2000-jul-05
                 *
                 * The following is extremely ugly, and I apologize for it:
                 * Several places in the above switch statement need to call
                 * a callback function or output a 32-bit code point,
                 * each of which is an involved process with
                 * a couple dozen of statements.
                 *
                 * I could do this in a function call, but I fear that then
                 * the compiler does not keep the frequently used variables in
                 * registers because the function call would need them on the stack
                 * for input and output.
                 *
                 * I could do this with a macro, but that is harder to debug and
                 * bloats the compiled code.
                 *
                 * I could just copy and paste the code, but that would also bloat
                 * the program size, make the pieces harder to maintain, and make
                 * the switch statement extremely long and clumsy.
                 *
                 * Therefore, those places goto here and do it all in one place,
                 * while the normal processing has a continue above and skips this
                 * part.
                 * This actually _saves_ goto statements, too:
                 * Since it is not possible in C to break a loop from within a switch
                 * statement, the callback code in the switch statement would have to
                 * goto behind the loop. Here, it can break if necessary.
                 */

output32:
                /* output a 32-bit (21-bit) Unicode code point stored in entry */
                if(entry<=0xffff) {
                    /* output BMP code point */
                    *target++=(UChar)entry;
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                } else {
                    /* output surrogate pair */
                    *target++=(UChar)(0xd7c0+(entry>>10));
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                    c=(UChar)(0xdc00|(UChar)(entry&0x3ff));
                    if(target<targetLimit) {
                        *target++=c;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                        }
                    } else {
                        /* target overflow */
                        cnv->UCharErrorBuffer[0]=c;
                        cnv->UCharErrorBufferLength=1;
                        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;

                        offset=0;
                        byteIndex=0;
                        break;
                    }
                }

                /* same as normal end of action codes: prepare for a new character */
                offset=0;
                byteIndex=0;
                sourceIndex=nextSourceIndex;
                continue;

callback:
                /* call the callback function with all the preparations and post-processing */
                /* update the arguments structure */
                pArgs->source=(const char *)source;
                pArgs->target=target;
                pArgs->offsets=offsets;

                /* copy the current bytes to invalidCharBuffer */
                for(b=0; b<(uint8_t)byteIndex; ++b) {
                    cnv->invalidCharBuffer[b]=(char)bytes[b];
                }
                cnv->invalidCharLength=byteIndex;

                /* set the converter state in UConverter to deal with the next character */
                cnv->toUnicodeStatus=0;
                cnv->mode=state;
                cnv->toULength=0;

                /* call the callback function */
                toUCallback(cnv, cnv->toUContext, pArgs, (const char *)bytes, byteIndex, reason, pErrorCode);

                /* get the converter state from UConverter */
                offset=cnv->toUnicodeStatus;
                state=(uint8_t)cnv->mode;
                byteIndex=cnv->toULength;

                /* update target and deal with offsets if necessary */
                offsets=ucnv_updateCallbackOffsets(offsets, pArgs->target-target, sourceIndex);
                target=pArgs->target;

                /* update the source pointer and index */
                sourceIndex=nextSourceIndex+((const uint8_t *)pArgs->source-source);
                source=(const uint8_t *)pArgs->source;

                /*
                 * If the callback overflowed the target, then we need to
                 * stop here with an overflow indication.
                 */
                if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                    break;
                } else if(U_FAILURE(*pErrorCode)) {
                    /* break on error */
                    offset=0;
                    state=0;
                    byteIndex=0;
                    break;
                } else if(cnv->UCharErrorBufferLength>0) {
                    /* target is full */
                    *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                    break;
                }

                /*
                 * We do not need to repeat the statements from the normal
                 * end of the action codes because we already updated all the
                 * necessary variables.
                 */
            }
        } else {
            /* target is full */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
endloop:

    if(pArgs->flush && source>=sourceLimit) {
        /* reset the state for the next conversion */
        if(byteIndex>0 && U_SUCCESS(*pErrorCode)) {
            /* a character byte sequence remains incomplete */
            *pErrorCode=U_TRUNCATED_CHAR_FOUND;
        }
        cnv->toUnicodeStatus=0;
        cnv->mode=0;
        cnv->toULength=0;
    } else {
        /* set the converter state back into UConverter */
        cnv->toUnicodeStatus=offset;
        cnv->mode=state;
        cnv->toULength=byteIndex;
    }

    /* write back the updated pointers */
    pArgs->source=(const char *)source;
    pArgs->target=target;
    pArgs->offsets=offsets;
}

/* This version of _MBCSToUnicodeWithOffsets() is optimized for single-byte, single-state codepages. */
U_CFUNC void
_MBCSSingleToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                                UErrorCode *pErrorCode) {
    UConverter *cnv;
    const uint8_t *source, *sourceLimit;
    UChar *target;
    const UChar *targetLimit;
    int32_t *offsets;

    const int32_t (*stateTable)[256];

    int32_t sourceIndex, nextSourceIndex;

    int32_t entry;
    UChar c;
    uint8_t b;
    UConverterCallbackReason reason;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;
    target=pArgs->target;
    targetLimit=pArgs->targetLimit;
    offsets=pArgs->offsets;

    stateTable=cnv->sharedData->table->mbcs.stateTable;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex=0;
    nextSourceIndex=0;

    /* conversion loop */
    while(source<sourceLimit) {
        /*
         * This following test is to see if available input would overflow the output.
         * It does not catch output of more than one code unit that
         * overflows as a result of a surrogate pair or callback output
         * from the last source byte.
         * Therefore, those situations also test for overflows and will
         * then break the loop, too.
         */
        if(target<targetLimit) {
            b=*source++;
            ++nextSourceIndex;
            entry=stateTable[0][b];
            /* entry<0 */
            /*
             * bit 31 is set, bits:
             * 30..27 action code
             *        (do not mask out bit 31 for speed, include it in action values)
             * 26..7  depend on the action code
             *  6..0  next state
             */

            /* switch per action code */
            switch((uint32_t)entry>>27U) {
            case 16|MBCS_STATE_ILLEGAL:
                /* bits 26..7 are not used, 0 */
                /* callback(illegal) */
                reason=UCNV_ILLEGAL;
                *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                goto callback;
            case 16|MBCS_STATE_UNASSIGNED:
                /* bits 26..7 are not used, 0 */
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
                goto callback;
            case 16|MBCS_STATE_FALLBACK_DIRECT_16:
                /* bits 26..23 are not used, 0 */
                /* bits 22..7 contain the Unicode BMP code point */
                if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                    /* callback(unassigned) */
                    reason=UCNV_UNASSIGNED;
                    *pErrorCode=U_INVALID_CHAR_FOUND;
                    goto callback;
                }
                /* fall through to the MBCS_STATE_VALID_DIRECT_16 branch */
            case 16|MBCS_STATE_VALID_DIRECT_16:
                /* bits 26..23 are not used, 0 */
                /* bits 22..7 contain the Unicode BMP code point */
                /* output BMP code point */
                *target++=(UChar)(entry>>7);
                if(offsets!=NULL) {
                    *offsets++=sourceIndex;
                }
                break;
            case 16|MBCS_STATE_FALLBACK_DIRECT_20:
                /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
                if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                    /* callback(unassigned) */
                    reason=UCNV_UNASSIGNED;
                    *pErrorCode=U_INVALID_CHAR_FOUND;
                    goto callback;
                }
                /* fall through to the MBCS_STATE_VALID_DIRECT_20 branch */
            case 16|MBCS_STATE_VALID_DIRECT_20:
                /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
                entry=(entry>>7)&0xfffff;
                /* output surrogate pair */
                *target++=(UChar)(0xd800|(UChar)(entry>>10));
                if(offsets!=NULL) {
                    *offsets++=sourceIndex;
                }
                c=(UChar)(0xdc00|(UChar)(entry&0x3ff));
                if(target<targetLimit) {
                    *target++=c;
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                } else {
                    /* target overflow */
                    cnv->UCharErrorBuffer[0]=c;
                    cnv->UCharErrorBufferLength=1;
                    *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                    goto endloop;
                }
                break;
            default:
                /* reserved, must never occur */
                /* bits 26..7 are not used, 0 */
                break;
            }

            /* normal end of action codes: prepare for a new character */
            sourceIndex=nextSourceIndex;
            continue;

callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=(const char *)source;
            pArgs->target=target;
            pArgs->offsets=offsets;

            /* copy the current bytes to invalidCharBuffer */
            cnv->invalidCharBuffer[0]=b;
            cnv->invalidCharLength=1;

            /* call the callback function */
            toUCallback(cnv, cnv->toUContext, pArgs, cnv->invalidCharBuffer, 1, reason, pErrorCode);

            /* update target and deal with offsets if necessary */
            offsets=ucnv_updateCallbackOffsets(offsets, pArgs->target-target, sourceIndex);
            target=pArgs->target;

            /* update the source pointer and index */
            sourceIndex=nextSourceIndex+((const uint8_t *)pArgs->source-source);
            source=(const uint8_t *)pArgs->source;

            /*
             * If the callback overflowed the target, then we need to
             * stop here with an overflow indication.
             */
            if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                break;
            } else if(U_FAILURE(*pErrorCode)) {
                /* break on error */
                break;
            } else if(cnv->UCharErrorBufferLength>0) {
                /* target is full */
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                break;
            }

            /*
             * We do not need to repeat the statements from the normal
             * end of the action codes because we already updated all the
             * necessary variables.
             */
        } else {
            /* target is full */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }
endloop:

    /* write back the updated pointers */
    pArgs->source=(const char *)source;
    pArgs->target=target;
    pArgs->offsets=offsets;
}

/*
 * This version of _MBCSSingleToUnicodeWithOffsets() is optimized for single-byte, single-state codepages
 * that only map to and from the BMP.
 * In addition to single-byte optimizations, the offset calculations
 * become much easier.
 */
U_CFUNC void
_MBCSSingleToBMPWithOffsets(UConverterToUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode) {
    UConverter *cnv;
    const uint8_t *source, *sourceLimit, *lastSource;
    UChar *target;
    int32_t targetCapacity, length;
    int32_t *offsets;

    const int32_t (*stateTable)[256];

    int32_t sourceIndex;

    int32_t entry;
    uint8_t b;
    UConverterCallbackReason reason;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;
    target=pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    stateTable=cnv->sharedData->table->mbcs.stateTable;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex=0;
    lastSource=source;

    /*
     * since the conversion here is 1:1 UChar:uint8_t, we need only one counter
     * for the minimum of the sourceLength and targetCapacity
     */
    length=sourceLimit-source;
    if(length<targetCapacity) {
        targetCapacity=length;
    }

    /* conversion loop */
    while(targetCapacity>0) {
        b=*source++;
        entry=stateTable[0][b];
        /* entry<0 */
        /*
         * bit 31 is set, bits:
         * 30..27 action code
         *        (do not mask out bit 31 for speed, include it in action values)
         * 26..7  depend on the action code
         *  6..0  next state
         */

        /* switch per action code */
        switch((uint32_t)entry>>27U) {
        case 16|MBCS_STATE_ILLEGAL:
            /* bits 26..7 are not used, 0 */
            /* callback(illegal) */
            reason=UCNV_ILLEGAL;
            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
            break;
        case 16|MBCS_STATE_UNASSIGNED:
            /* bits 26..7 are not used, 0 */
            /* callback(unassigned) */
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
            break;
        case 16|MBCS_STATE_FALLBACK_DIRECT_16:
            /* bits 26..23 are not used, 0 */
            /* bits 22..7 contain the Unicode BMP code point */
            if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
                break;
            }
            /* fall through to the MBCS_STATE_VALID_DIRECT_16 branch */
        case 16|MBCS_STATE_VALID_DIRECT_16:
            /* bits 26..23 are not used, 0 */
            /* bits 22..7 contain the Unicode BMP code point */
            /* output BMP code point */
            *target++=(UChar)(entry>>7);
            --targetCapacity;
            continue;
        default:
            /* reserved, must never occur */
            /* bits 26..7 are not used, 0 */
            continue;
        }

        /* call the callback function with all the preparations and post-processing */
        /* set offsets since the start or the last callback */
        if(offsets!=NULL) {
            int32_t count=(int32_t)(source-lastSource);

            /* predecrement: do not set the offset for the callback-causing character */
            while(--count>0) {
                *offsets++=sourceIndex++;
            }
            /* offset and sourceIndex are now set for the current character */
        }

        /* update the arguments structure */
        pArgs->source=(const char *)source;
        pArgs->target=target;
        pArgs->offsets=offsets;

        /* copy the current bytes to invalidCharBuffer */
        cnv->invalidCharBuffer[0]=b;
        cnv->invalidCharLength=1;

        /* call the callback function */
        toUCallback(cnv, cnv->toUContext, pArgs, cnv->invalidCharBuffer, 1, reason, pErrorCode);

        /* update target and deal with offsets if necessary */
        offsets=ucnv_updateCallbackOffsets(offsets, pArgs->target-target, sourceIndex);
        target=pArgs->target;

        /* update the source pointer and index */
        sourceIndex+=1+((const uint8_t *)pArgs->source-source);
        source=lastSource=(const uint8_t *)pArgs->source;
        targetCapacity=pArgs->targetLimit-target;
        length=sourceLimit-source;
        if(length<targetCapacity) {
            targetCapacity=length;
        }

        /*
         * If the callback overflowed the target, then we need to
         * stop here with an overflow indication.
         */
        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
            break;
        } else if(U_FAILURE(*pErrorCode)) {
            /* break on error */
            break;
        } else if(cnv->UCharErrorBufferLength>0) {
            /* target is full */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }

    if(U_SUCCESS(*pErrorCode) && source<sourceLimit && target>=pArgs->targetLimit) {
        /* target is full */
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }

    /* set offsets since the start or the last callback */
    if(offsets!=NULL) {
        size_t count=source-lastSource;
        while(count>0) {
            *offsets++=sourceIndex++;
            --count;
        }
    }

    /* write back the updated pointers */
    pArgs->source=(const char *)source;
    pArgs->target=target;
    pArgs->offsets=offsets;
}

U_CFUNC UChar32
_MBCSGetNextUChar(UConverterToUnicodeArgs *pArgs,
                  UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];

    UConverter *cnv;
    const uint8_t *source, *sourceLimit;

    const int32_t (*stateTable)[256];
    const uint16_t *unicodeCodeUnits;

    uint32_t offset;
    uint8_t state;
    int8_t byteIndex;
    uint8_t *bytes;

    int32_t entry;
    UChar32 c;
    uint8_t b;
    UConverterCallbackReason reason;

    /* use optimized function if possible */
    cnv=pArgs->converter;
    if(cnv->sharedData->table->mbcs.unicodeMask&UCNV_HAS_SURROGATES) {
        /*
         * Calling the inefficient, generic getNextUChar() lets us deal correctly
         * with the rare case of a codepage that maps single surrogates
         * without adding the complexity to this already complicated function here.
         */
        return ucnv_getNextUCharFromToUImpl(pArgs, _MBCSToUnicodeWithOffsets, TRUE, pErrorCode);
    } else if(cnv->sharedData->table->mbcs.countStates==1) {
        return _MBCSSingleGetNextUChar(pArgs, pErrorCode);
    }

    /* set up the local pointers */
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;

    stateTable=cnv->sharedData->table->mbcs.stateTable;
    unicodeCodeUnits=cnv->sharedData->table->mbcs.unicodeCodeUnits;

    /* get the converter state from UConverter */
    offset=cnv->toUnicodeStatus;
    state=(uint8_t)(cnv->mode);
    byteIndex=cnv->toULength;
    bytes=cnv->toUBytes;

    /* conversion loop */
    while(source<sourceLimit) {
        bytes[byteIndex++]=b=*source++;
        entry=stateTable[state][b];
        if(entry>=0) {
            /*
             * bit 31 is not set, bits:
             * 30..7  offset delta
             *  6..0  next state
             */
            state=(uint8_t)(entry&0x7f);
            offset+=entry>>7;
        } else {
            /*
             * bit 31 is set, bits:
             * 30..27 action code
             *        (do not mask out bit 31 for speed, include it in action values)
             * 26..7  depend on the action code
             *  6..0  next state
             */

            /* set the next state early so that we can reuse the entry variable */
            state=(uint8_t)(entry&0x7f); /* typically 0 */

            /* switch per action code */
            switch((uint32_t)entry>>27U) {
            case 16|MBCS_STATE_ILLEGAL:
                /* bits 26..7 are not used, 0 */
                /* callback(illegal) */
                reason=UCNV_ILLEGAL;
                *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                goto callback;
            case 16|MBCS_STATE_CHANGE_ONLY:
                /* bits 26..7 are not used, 0 */
                /*
                 * This serves as a state change without any output.
                 * It is useful for reading simple stateful encodings,
                 * for example using just Shift-In/Shift-Out codes.
                 * The 21 unused bits may later be used for more sophisticated
                 * state transitions.
                 */
                break;
            case 16|MBCS_STATE_UNASSIGNED:
                /* bits 26..7 are not used, 0 */
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
                goto callback;
            case 16|MBCS_STATE_FALLBACK_DIRECT_16:
                /* bits 26..23 are not used, 0 */
                /* bits 22..7 contain the Unicode BMP code point */
                if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                    /* callback(unassigned) */
                    reason=UCNV_UNASSIGNED;
                    *pErrorCode=U_INVALID_CHAR_FOUND;
                    goto callback;
                }
                /* fall through to the MBCS_STATE_VALID_DIRECT_16 branch */
            case 16|MBCS_STATE_VALID_DIRECT_16:
                /* bits 26..23 are not used, 0 */
                /* bits 22..7 contain the Unicode BMP code point */
                /* output BMP code point */
                c=(UChar)(entry>>7);
                goto finish;
            case 16|MBCS_STATE_FALLBACK_DIRECT_20:
                /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
                if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                    /* callback(unassigned) */
                    reason=UCNV_UNASSIGNED;
                    *pErrorCode=U_INVALID_CHAR_FOUND;
                    goto callback;
                }
                /* fall through to the MBCS_STATE_VALID_DIRECT_20 branch */
            case 16|MBCS_STATE_VALID_DIRECT_20:
                /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
                /* output supplementary code point */
                c=(UChar32)(((entry>>7)&0xfffff)+0x10000);
                goto finish;
            case 16|MBCS_STATE_VALID_16:
                /* bits 26..16 are not used, 0 */
                /* bits 15..7 contain the final offset delta to one 16-bit code unit */
                offset+=(uint16_t)entry>>7;
                c=unicodeCodeUnits[offset];
                if(c<0xfffe) {
                    /* output BMP code point */
                    goto finish;
                } else if(c==0xfffe) {
                    if(UCNV_TO_U_USE_FALLBACK(cnv) && (c=_MBCSGetFallback(&cnv->sharedData->table->mbcs, offset))!=0xfffe) {
                        goto finish;
                    }
                    /* callback(unassigned) */
                    reason=UCNV_UNASSIGNED;
                    *pErrorCode=U_INVALID_CHAR_FOUND;
                    goto callback;
                } else {
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                    goto callback;
                }
                break;
            case 16|MBCS_STATE_VALID_16_PAIR:
                /* bits 26..16 are not used, 0 */
                /* bits 15..7 contain the final offset delta to two 16-bit code units */
                offset+=(uint16_t)entry>>7;
                c=unicodeCodeUnits[offset++];
                if(UTF_IS_FIRST_SURROGATE(c)) {
                    c=UTF16_GET_PAIR_VALUE(c, unicodeCodeUnits[offset]);
                    goto finish;
                } else if(c<0xfffe) {
                    /* output BMP code point */
                    goto finish;
                } else if(c==0xfffe) {
                    /*
                     * For the fallback, we need to restore the offset that
                     * we had before the unicodeCodeUnits[offset++] above that incremented it!
                     */
                    if(UCNV_TO_U_USE_FALLBACK(cnv) && (c=_MBCSGetFallback(&cnv->sharedData->table->mbcs, offset-1))!=0xfffe) {
                        goto finish;
                    }
                    /* callback(unassigned) */
                    reason=UCNV_UNASSIGNED;
                    *pErrorCode=U_INVALID_CHAR_FOUND;
                    goto callback;
                } else {
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                    goto callback;
                }
                break;
            default:
                /* reserved, must never occur */
                /* bits 26..7 are not used, 0 */
                break;
            }

            /* normal end of action codes: prepare for a new character */
            offset=0;
            byteIndex=0;
            continue;

callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=(const char *)source;
            pArgs->target=buffer;
            pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;

            /* copy the current bytes to invalidCharBuffer */
            for(b=0; b<(uint8_t)byteIndex; ++b) {
                cnv->invalidCharBuffer[b]=(char)bytes[b];
            }
            cnv->invalidCharLength=byteIndex;

            /* set the converter state in UConverter to deal with the next character */
            cnv->toUnicodeStatus=0;
            cnv->mode=state;
            cnv->toULength=0;

            /* call the callback function */
            toUCallback(cnv, cnv->toUContext, pArgs, (const char *)bytes, byteIndex, reason, pErrorCode);

            /* get the converter state from UConverter */
            offset=cnv->toUnicodeStatus;
            state=(uint8_t)cnv->mode;
            byteIndex=cnv->toULength;

            /* update the source pointer */
            source=(const uint8_t *)pArgs->source;

            /*
             * return the first character if the callback wrote some
             * we do not need to goto finish because the converter state is already set
             */
            if(U_SUCCESS(*pErrorCode)) {
                entry=pArgs->target-buffer;
                if(entry>0) {
                    return ucnv_getUChar32KeepOverflow(cnv, buffer, entry);
                }
                /* else (callback did not write anything) continue */
            } else if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                *pErrorCode=U_ZERO_ERROR;
                return ucnv_getUChar32KeepOverflow(cnv, buffer, UTF_MAX_CHAR_LENGTH);
            } else {
                /* break on error */
                /* ### what if a callback set an error but _also_ generated output?! */
                state=0;
                c=0xffff;
                goto finish;
            }

            /*
             * We do not need to repeat the statements from the normal
             * end of the action codes because we already updated all the
             * necessary variables.
             */
        }
    }

    if(byteIndex>0) {
        /* incomplete character byte sequence */
        *pErrorCode=U_TRUNCATED_CHAR_FOUND;
        state=0;
    } else {
        /* no output because of empty input or only state changes and skipping callbacks */
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    }
    c=0xffff;

finish:
    /* set the converter state back into UConverter, ready for a new character */
    cnv->toUnicodeStatus=0;
    cnv->mode=state;
    cnv->toULength=0;

    /* write back the updated pointer */
    pArgs->source=(const char *)source;
    return c;
}

/*
 * This version of _MBCSGetNextUChar() is optimized for single-byte, single-state codepages.
 * We still need a conversion loop in case a skipping callback is called.
 */
U_CFUNC UChar32
_MBCSSingleGetNextUChar(UConverterToUnicodeArgs *pArgs,
                        UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];

    UConverter *cnv;
    const uint8_t *source, *sourceLimit;

    int32_t entry;
    uint8_t b;
    UConverterCallbackReason reason;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;

    /* conversion loop */
    while(source<sourceLimit) {
        b=*source++;
        entry=cnv->sharedData->table->mbcs.stateTable[0][b];
        /* entry<0 */
        /*
         * bit 31 is set, bits:
         * 30..27 action code
         *        (do not mask out bit 31 for speed, include it in action values)
         * 26..7  depend on the action code
         *  6..0  next state
         */

        /* write back the updated pointer early so that we can return directly */
        pArgs->source=(const char *)source;

        /* switch per action code */
        switch((uint32_t)entry>>27U) {
        case 16|MBCS_STATE_ILLEGAL:
            /* bits 26..7 are not used, 0 */
            /* callback(illegal) */
            reason=UCNV_ILLEGAL;
            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
            break;
        case 16|MBCS_STATE_UNASSIGNED:
            /* bits 26..7 are not used, 0 */
            /* callback(unassigned) */
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
            break;
        case 16|MBCS_STATE_FALLBACK_DIRECT_16:
            /* bits 26..23 are not used, 0 */
            /* bits 22..7 contain the Unicode BMP code point */
            if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
                break;
            }
            /* fall through to the MBCS_STATE_VALID_DIRECT_16 branch */
        case 16|MBCS_STATE_VALID_DIRECT_16:
            /* bits 26..23 are not used, 0 */
            /* bits 22..7 contain the Unicode BMP code point */
            /* output BMP code point */
            return (UChar)(entry>>7);
        case 16|MBCS_STATE_FALLBACK_DIRECT_20:
            /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
            if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
                break;
            }
            /* fall through to the MBCS_STATE_VALID_DIRECT_20 branch */
        case 16|MBCS_STATE_VALID_DIRECT_20:
            /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
            /* output supplementary code point */
            return (UChar32)(((entry>>7)&0xfffff)+0x10000);
        default:
            /* reserved, must never occur */
            /* bits 26..7 are not used, 0 */
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return 0xffff;
        }

        /* call the callback function with all the preparations and post-processing */
        /* update the arguments structure */
        pArgs->target=buffer;
        pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;

        /* copy the current byte to invalidCharBuffer */
        cnv->invalidCharBuffer[0]=(char)b;
        cnv->invalidCharLength=1;

        /* call the callback function */
        toUCallback(cnv, cnv->toUContext, pArgs, cnv->invalidCharBuffer, 1, reason, pErrorCode);

        /* update the source pointer */
        source=(const uint8_t *)pArgs->source;

        /*
         * return the first character if the callback wrote some
         * we do not need to goto finish because the converter state is already set
         */
        if(U_SUCCESS(*pErrorCode)) {
            entry=pArgs->target-buffer;
            if(entry>0) {
                return ucnv_getUChar32KeepOverflow(cnv, buffer, entry);
            }
            /* else (callback did not write anything) continue */
        } else if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
            *pErrorCode=U_ZERO_ERROR;
            return ucnv_getUChar32KeepOverflow(cnv, buffer, UTF_MAX_CHAR_LENGTH);
        } else {
            /* break on error */
            /* ### what if a callback set an error but _also_ generated output?! */
            return 0xffff;
        }
    }

    /* no output because of empty input or only state changes and skipping callbacks */
    *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}

/*
 * This is a simple version of getNextUChar() that is used
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
                        UBool useFallback) {
    const uint8_t *source;

    const int32_t (*stateTable)[256];
    const uint16_t *unicodeCodeUnits;

    uint32_t offset;
    uint8_t state;

    int32_t entry;

    /* set up the local pointers */
    source=(const uint8_t *)*pSource;
    if(source>=(const uint8_t *)sourceLimit) {
        /* no input at all: "illegal" */
        return 0xffff;
    }

    /* use optimized function if possible */
    if(sharedData->table->mbcs.countStates==1) {
        return _MBCSSingleSimpleGetNextUChar(sharedData, (uint8_t)(*(*pSource)++), useFallback);
    }

    stateTable=sharedData->table->mbcs.stateTable;
    unicodeCodeUnits=sharedData->table->mbcs.unicodeCodeUnits;

    /* converter state */
    offset=0;
    state=0;

    /* conversion loop */
    do {
        entry=stateTable[state][*source++];
        if(entry>=0) {
            /*
             * bit 31 is not set, bits:
             * 30..7  offset delta
             *  6..0  next state
             */
            state=(uint8_t)(entry&0x7f);
            offset+=entry>>7;
        } else {
            /*
             * bit 31 is set, bits:
             * 30..27 action code
             *        (do not mask out bit 31 for speed, include it in action values)
             * 26..7  depend on the action code
             *  6..0  next state
             */

            *pSource=(const char *)source;

            /* switch per action code */
            switch((uint32_t)entry>>27U) {
            case 16|MBCS_STATE_ILLEGAL:
                /* bits 26..7 are not used, 0 */
                return 0xffff;
            case 16|MBCS_STATE_CHANGE_ONLY:
                /* bits 26..7 are not used, 0 */
                /*
                 * This serves as a state change without any output.
                 * It is useful for reading simple stateful encodings,
                 * for example using just Shift-In/Shift-Out codes.
                 * The 21 unused bits may later be used for more sophisticated
                 * state transitions.
                 */
                if(source==(const uint8_t *)sourceLimit) {
                    /* if there are only state changes, then return "unassigned" */
                    return 0xfffe;
                }
                break;
            case 16|MBCS_STATE_UNASSIGNED:
                /* bits 26..7 are not used, 0 */
                return 0xfffe;
            case 16|MBCS_STATE_FALLBACK_DIRECT_16:
                /* bits 26..23 are not used, 0 */
                /* bits 22..7 contain the Unicode BMP code point */
                if(!TO_U_USE_FALLBACK(useFallback)) {
                    return 0xfffe;
                }
                /* fall through to the MBCS_STATE_VALID_DIRECT_16 branch */
            case 16|MBCS_STATE_VALID_DIRECT_16:
                /* bits 26..23 are not used, 0 */
                /* bits 22..7 contain the Unicode BMP code point */
                /* output BMP code point */
                return (UChar)(entry>>7);
            case 16|MBCS_STATE_FALLBACK_DIRECT_20:
                /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
                if(!TO_U_USE_FALLBACK(useFallback)) {
                    return 0xfffe;
                }
                /* fall through to the MBCS_STATE_VALID_DIRECT_20 branch */
            case 16|MBCS_STATE_VALID_DIRECT_20:
                /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
                return 0x10000+((entry>>7)&0xfffff);
            case 16|MBCS_STATE_VALID_16:
                /* bits 26..16 are not used, 0 */
                /* bits 15..7 contain the final offset delta to one 16-bit code unit */
                offset+=(uint16_t)entry>>7;
                entry=unicodeCodeUnits[offset];
                if(entry!=0xfffe) {
                    return (UChar32)entry;
                } else {
                    return _MBCSGetFallback(&sharedData->table->mbcs, offset);
                }
            case 16|MBCS_STATE_VALID_16_PAIR:
                /* bits 26..16 are not used, 0 */
                /* bits 15..7 contain the final offset delta to two 16-bit code units */
                offset+=(uint16_t)entry>>7;
                entry=unicodeCodeUnits[offset++];
                if(UTF_IS_FIRST_SURROGATE(entry)) {
                    return UTF16_GET_PAIR_VALUE(entry, unicodeCodeUnits[offset]);
                } else if(entry!=0xfffe) {
                    /* output BMP code point */
                    return (UChar32)entry;
                } else {
                    /*
                     * For the fallback, we need to restore the offset that
                     * we had before the unicodeCodeUnits[offset++] above that incremented it!
                     */
                    return _MBCSGetFallback(&sharedData->table->mbcs, offset-1);
                }
            default:
                /* reserved, must never occur */
                /* bits 26..7 are not used, 0 */
                break;
            }

            /* state change only - prepare for a new character */
            state=(uint8_t)(entry&0x7f); /* typically 0 */
            offset=0;
        }
    } while(source<(const uint8_t *)sourceLimit);

    *pSource=(const char *)source;
    return 0xffff;
}

/* This version of _MBCSSimpleGetNextUChar() is optimized for single-byte, single-state codepages. */
U_CFUNC UChar32
_MBCSSingleSimpleGetNextUChar(UConverterSharedData *sharedData,
                              uint8_t b, UBool useFallback) {
    int32_t entry;

    entry=sharedData->table->mbcs.stateTable[0][b];
    /* entry<0 */
    /*
     * bit 31 is set, bits:
     * 30..27 action code
     *        (do not mask out bit 31 for speed, include it in action values)
     * 26..7  depend on the action code
     *  6..0  next state
     */

    /* switch per action code */
    switch((uint32_t)entry>>27U) {
    case 16|MBCS_STATE_ILLEGAL:
        /* bits 26..7 are not used, 0 */
        return 0xffff;
    case 16|MBCS_STATE_UNASSIGNED:
        /* bits 26..7 are not used, 0 */
        return 0xfffe;
    case 16|MBCS_STATE_FALLBACK_DIRECT_16:
        /* bits 26..23 are not used, 0 */
        /* bits 22..7 contain the Unicode BMP code point */
        if(!TO_U_USE_FALLBACK(useFallback)) {
            return 0xfffe;
        }
        /* fall through to the MBCS_STATE_VALID_DIRECT_16 branch */
    case 16|MBCS_STATE_VALID_DIRECT_16:
        /* bits 26..23 are not used, 0 */
        /* bits 22..7 contain the Unicode BMP code point */
        /* output BMP code point */
        return (UChar)(entry>>7);
    case 16|MBCS_STATE_FALLBACK_DIRECT_20:
        /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
        if(!TO_U_USE_FALLBACK(useFallback)) {
            return 0xfffe;
        }
        /* fall through to the MBCS_STATE_VALID_DIRECT_20 branch */
    case 16|MBCS_STATE_VALID_DIRECT_20:
        /* bits 26..7 contain the Unicode surrogate code point minus 0x10000 */
        return 0x10000+((entry>>7)&0xfffff);
    default:
        /* reserved, must never occur */
        /* bits 26..7 are not used, 0 */
        return 0xffff;
    }
}

/* MBCS-from-Unicode conversion functions ----------------------------------- */

U_CFUNC void
_MBCSFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode) {
    UConverter *cnv;
    const UChar *source, *sourceLimit;
    uint8_t *target;
    int32_t targetCapacity;
    int32_t *offsets;

    const uint16_t *table;
    const uint8_t *p, *bytes;
    uint8_t outputType;

    UChar32 c;

    int32_t sourceIndex, nextSourceIndex;

    UConverterCallbackReason reason;
    uint32_t i;
    uint32_t value;
    int32_t length, prevLength;
    uint8_t unicodeMask;

    /* use optimized function if possible */
    cnv=pArgs->converter;
    outputType=cnv->sharedData->table->mbcs.outputType;
    unicodeMask=cnv->sharedData->table->mbcs.unicodeMask;
    if(outputType==MBCS_OUTPUT_1 && !(unicodeMask&UCNV_HAS_SURROGATES)) {
        if(!(unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
            _MBCSSingleFromBMPWithOffsets(pArgs, pErrorCode);
        } else {
            _MBCSSingleFromUnicodeWithOffsets(pArgs, pErrorCode);
        }
        return;
    }

    /* set up the local pointers */
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    table=cnv->sharedData->table->mbcs.fromUnicodeTable;
    bytes=cnv->sharedData->table->mbcs.fromUnicodeBytes;

    /* get the converter state from UConverter */
    c=cnv->fromUSurrogateLead;
    prevLength=cnv->fromUnicodeStatus;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex= c==0 ? 0 : -1;
    nextSourceIndex=0;

    /* conversion loop */
    /*
     * This is another piece of ugly code:
     * A goto into the loop if the converter state contains a first surrogate
     * from the previous function call.
     * It saves me to check in each loop iteration a check of if(c==0)
     * and duplicating the trail-surrogate-handling code in the else
     * branch of that check.
     * I could not find any other way to get around this other than
     * using a function call for the conversion and callback, which would
     * be even more inefficient.
     *
     * Markus Scherer 2000-jul-19
     */
    if(c!=0 && targetCapacity>0) {
        goto getTrail;
    }

    while(source<sourceLimit) {
        /*
         * This following test is to see if available input would overflow the output.
         * It does not catch output of more than one byte that
         * overflows as a result of a multi-byte character or callback output
         * from the last source character.
         * Therefore, those situations also test for overflows and will
         * then break the loop, too.
         */
        if(targetCapacity>0) {
            /*
             * Get a correct Unicode code point:
             * a single UChar for a BMP code point or
             * a matched surrogate pair for a "surrogate code point".
             */
            c=*source++;
            ++nextSourceIndex;
            /*
             * This also tests if the codepage maps single surrogates.
             * If it does, then surrogates are not paired but mapped separately.
             * Note that in this case unmatched surrogates are not detected.
             */
            if(UTF_IS_SURROGATE(c) && !(unicodeMask&UCNV_HAS_SURROGATES)) {
                if(UTF_IS_SURROGATE_FIRST(c)) {
getTrail:
                    if(source<sourceLimit) {
                        /* test the following code unit */
                        UChar trail=*source;
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++source;
                            ++nextSourceIndex;
                            c=UTF16_GET_PAIR_VALUE(c, trail);
                            if(!(unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
                                /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
                                /* callback(unassigned) */
                                reason=UCNV_UNASSIGNED;
                                *pErrorCode=U_INVALID_CHAR_FOUND;
                                goto callback;
                            }
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                            goto callback;
                        }
                    } else {
                        /* no more input */
                        break;
                    }
                } else {
                    /* this is an unmatched trail code unit (2nd surrogate) */
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                    goto callback;
                }
            }

            /* convert the Unicode code point in c into codepage bytes */

            /*
             * The basic lookup is a triple-stage compact array lookup:
             *
             * Bits 21..10 (0x440 different values because Unicode code points
             * reach up to 0x10ffff) are used as an index into table[],
             * then bits 9..4 are added to that and together multiplied by 2
             * to be used as an index into a second table that starts at table+0x440.
             * With version 3 of the data structure, that offset of 0x440 is taken into
             * account in the stage 1 entry.
             * Also with version 3, if a codepage maps only from BMP code points,
             * then there are only 64=0x40 entries in stage 1.
             * For more details about the indexing and the multiplier, see the
             * comments at the beginning of this file.
             *
             * In that second table, there will be two 16-bit values
             * (and therefore we multiplied by two in the previous step):
             * One 16-bit value stores a bit for each of the 16 Unicode code points
             * that are grouped here to indicate if it is assigned or not.
             * If it is not assigned, there may still be a codepage character
             * stored in the third stage: a fallback value. It is used only when
             * fallbacks are turned on for the converter. If the code point is
             * unassigned and fallbacks not used or there is no fallback character
             * (all bytes 0), then the callback function is called.
             *
             * The second value in the second table (stage) is an index into
             * the third table. It is multiplied by 16*(bytes stored per character)
             * to get to the first of 16 characters. At last, bits 3..0 of
             * the Unicode code point are multiplied by (bytes stored per character)
             * and added to that index for the address of the output codepage
             * character.
             *
             * For EUC encodings that use only either 0x8e or 0x8f as the first
             * byte of their longest byte sequences, the first two bytes in
             * this third stage indicate with their 7th bits whether these bytes
             * are to be written directly or actually need to be preceeded by
             * one of the two Single-Shift codes. With this, the third stage
             * stores one byte fewer per character than the actual maximum length of
             * EUC byte sequences.
             *
             * Other than that, leading zero bytes are removed and the other
             * bytes output. A single zero byte may be output if the "assigned"
             * bit in stage 2 was on or also if the Unicode code point is U+0000.
             * The data structure does not support zero byte output as a fallback
             * for other code points, and also does not allow output of leading zeros.
             */
            i=MBCS_STAGE_2_MULTIPLIER*(uint32_t)table[c>>10]+((c>>3)&0x7e);
            p=bytes;

            /* get the bytes and the length for the output */
            switch(outputType) {
            case MBCS_OUTPUT_1:
                p+=(16*(uint32_t)table[i+1]+(c&0xf));
                value=*p;
                length=1;
                break;
            case MBCS_OUTPUT_2:
                p+=(16*(uint32_t)table[i+1]+(c&0xf))*2;
#               if U_IS_BIG_ENDIAN
                    value=*(uint16_t *)p;
#               else
                    value=((uint32_t)*p<<8)|p[1];
#               endif
                if(value<=0xff) {
                    length=1;
                } else {
                    length=2;
                }
                break;
            case MBCS_OUTPUT_2_SISO:
                /* 1/2-byte stateful with Shift-In/Shift-Out */
                p+=(16*(uint32_t)table[i+1]+(c&0xf))*2;
#               if U_IS_BIG_ENDIAN
                    value=*(uint16_t *)p;
#               else
                    value=((uint32_t)*p<<8)|p[1];
#               endif
                if(value==0 && c!=0) {
                    /* unassigned (do not allow 0 result for assigned here), do not change state */
                    length=0;
                } else if(value<=0xff) {
                    if(prevLength==1) {
                        length=1;
                    } else {
                        /* change from double-byte mode to single-byte */
                        value|=(uint32_t)UCNV_SI<<8;
                        length=2;
                        prevLength=1;
                    }
                } else {
                    if(prevLength==2) {
                        length=2;
                    } else {
                        /* change from single-byte mode to double-byte */
                        value|=(uint32_t)UCNV_SO<<16;
                        length=3;
                        prevLength=2;
                    }
                }
                break;
            case MBCS_OUTPUT_3:
                p+=(16*(uint32_t)table[i+1]+(c&0xf))*3;
                value=((uint32_t)*p<<16)|((uint32_t)p[1]<<8)|p[2];
                if(value<=0xff) {
                    length=1;
                } else if(value<=0xffff) {
                    length=2;
                } else {
                    length=3;
                }
                break;
            case MBCS_OUTPUT_4:
                p+=(16*(uint32_t)table[i+1]+(c&0xf))*4;
#               if U_IS_BIG_ENDIAN
                    value=*(uint32_t *)p;
#               else
                    value=((uint32_t)*p<<24)|((uint32_t)p[1]<<16)|((uint32_t)p[2]<<8)|p[3];
#               endif
                if(value<=0xff) {
                    length=1;
                } else if(value<=0xffff) {
                    length=2;
                } else if(value<=0xffffff) {
                    length=3;
                } else {
                    length=4;
                }
                break;
            case MBCS_OUTPUT_3_EUC:
                p+=(16*(uint32_t)table[i+1]+(c&0xf))*2;
#               if U_IS_BIG_ENDIAN
                    value=*(uint16_t *)p;
#               else
                    value=((uint32_t)*p<<8)|p[1];
#               endif
                /* EUC 16-bit fixed-length representation */
                if(value<=0xff) {
                    length=1;
                } else if((value&0x8000)==0) {
                    value|=0x8e8000;
                    length=3;
                } else if((value&0x80)==0) {
                    value|=0x8f0080;
                    length=3;
                } else {
                    length=2;
                }
                break;
            case MBCS_OUTPUT_4_EUC:
                p+=(16*(uint32_t)table[i+1]+(c&0xf))*3;
                value=((uint32_t)*p<<16)|((uint32_t)p[1]<<8)|p[2];
                /* EUC 16-bit fixed-length representation applied to the first two bytes */
                if(value<=0xff) {
                    length=1;
                } else if(value<=0xffff) {
                    length=2;
                } else if((value&0x800000)==0) {
                    value|=0x8e800000;
                    length=4;
                } else if((value&0x8000)==0) {
                    value|=0x8f008000;
                    length=4;
                } else {
                    length=3;
                }
                break;
            default:
                /* must not occur */
                /*
                 * To avoid compiler warnings that value & length may be
                 * used without having been initialized, we set them here.
                 * In reality, this is unreachable code.
                 * Not having a default branch also causes warnings with
                 * some compilers.
                 */
                value=0;
                length=0;
                break;
            }

            /* is this code point assigned, or do we use fallbacks? */
            if(!((table[i]&(1<<(c&0xf)))!=0 ||
                 UCNV_FROM_U_USE_FALLBACK(cnv, c) && (value!=0 || c==0))
            ) {
                /*
                 * We allow a 0 byte output if the Unicode code point is
                 * U+0000 and also if the "assigned" bit is set for this entry.
                 * There is no way with this data structure for fallback output
                 * for other than U+0000 to be a zero byte.
                 */
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
                goto callback;
            }

            /* write the output character bytes from value and length */
            /* from the first if in the loop we know that targetCapacity>0 */
            if(length<=targetCapacity) {
                if(offsets==NULL) {
                    switch(length) {
                        /* each branch falls through to the next one */
                    case 4:
                        *target++=(uint8_t)(value>>24);
                    case 3:
                        *target++=(uint8_t)(value>>16);
                    case 2:
                        *target++=(uint8_t)(value>>8);
                    case 1:
                        *target++=(uint8_t)value;
                    default:
                        /* will never occur */
                        break;
                    }
                } else {
                    switch(length) {
                        /* each branch falls through to the next one */
                    case 4:
                        *target++=(uint8_t)(value>>24);
                        *offsets++=sourceIndex;
                    case 3:
                        *target++=(uint8_t)(value>>16);
                        *offsets++=sourceIndex;
                    case 2:
                        *target++=(uint8_t)(value>>8);
                        *offsets++=sourceIndex;
                    case 1:
                        *target++=(uint8_t)value;
                        *offsets++=sourceIndex;
                    default:
                        /* will never occur */
                        break;
                    }
                }
                targetCapacity-=length;
            } else {
                uint8_t *p;

                /*
                 * We actually do this backwards here:
                 * In order to save an intermediate variable, we output
                 * first to the overflow buffer what does not fit into the
                 * regular target.
                 */
                /* we know that 1<=targetCapacity<length<=4 */
                length-=targetCapacity;
                p=(uint8_t *)cnv->charErrorBuffer;
                switch(length) {
                    /* each branch falls through to the next one */
                case 3:
                    *p++=(uint8_t)(value>>16);
                case 2:
                    *p++=(uint8_t)(value>>8);
                case 1:
                    *p=(uint8_t)value;
                default:
                    /* will never occur */
                    break;
                }
                cnv->charErrorBufferLength=(int8_t)length;

                /* now output what fits into the regular target */
                value>>=8*length; /* length was reduced by targetCapacity */
                switch(targetCapacity) {
                    /* each branch falls through to the next one */
                case 3:
                    *target++=(uint8_t)(value>>16);
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                case 2:
                    *target++=(uint8_t)(value>>8);
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                case 1:
                    *target++=(uint8_t)value;
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                default:
                    /* will never occur */
                    break;
                }

                /* target overflow */
                targetCapacity=0;
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                c=0;
                break;
            }

            /* normal end of conversion: prepare for a new character */
            c=0;
            sourceIndex=nextSourceIndex;
            continue;

            /*
             * This is the same ugly trick as in ToUnicode(), for the
             * same reasons...
             */
callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=source;
            pArgs->target=(char *)target;
            pArgs->offsets=offsets;

            /* set the converter state in UConverter to deal with the next character */
            cnv->fromUSurrogateLead=0;
            cnv->fromUnicodeStatus=prevLength;

            /* write the code point as code units */
            i=0;
            UTF_APPEND_CHAR_UNSAFE(cnv->invalidUCharBuffer, i, c);
            cnv->invalidUCharLength=(int8_t)i;

            /* call the callback function */
            fromUCallback(cnv, cnv->fromUContext, pArgs, cnv->invalidUCharBuffer, i, c, reason, pErrorCode);

            /* get the converter state from UConverter */
            c=cnv->fromUSurrogateLead;
            prevLength=cnv->fromUnicodeStatus;

            /* update target and deal with offsets if necessary */
            offsets=ucnv_updateCallbackOffsets(offsets, ((uint8_t *)pArgs->target)-target, sourceIndex);
            target=(uint8_t *)pArgs->target;

            /* update the source pointer and index */
            sourceIndex=nextSourceIndex+(pArgs->source-source);
            source=pArgs->source;
            targetCapacity=(uint8_t *)pArgs->targetLimit-target;

            /*
             * If the callback overflowed the target, then we need to
             * stop here with an overflow indication.
             */
            if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                break;
            } else if(U_FAILURE(*pErrorCode)) {
                /* break on error */
                c=0;
                break;
            } else if(cnv->charErrorBufferLength>0) {
                /* target is full */
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                break;
            }

            /*
             * We do not need to repeat the statements from the normal
             * end of the conversion because we already updated all the
             * necessary variables.
             */
        } else {
            /* target is full */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }

    if(pArgs->flush && source>=sourceLimit) {
        /* reset the state for the next conversion */
        if(c!=0 && U_SUCCESS(*pErrorCode)) {
            /* a Unicode code point remains incomplete (only a first surrogate) */
            *pErrorCode=U_TRUNCATED_CHAR_FOUND;
        }
        cnv->fromUSurrogateLead=0;
        cnv->fromUnicodeStatus=1;
    } else {
        /* set the converter state back into UConverter */
        cnv->fromUSurrogateLead=(UChar)c;
        cnv->fromUnicodeStatus=prevLength;
    }

    /* write back the updated pointers */
    pArgs->source=source;
    pArgs->target=(char *)target;
    pArgs->offsets=offsets;
}

/* This version of _MBCSFromUnicode() is optimized for single-byte codepages. */
U_CFUNC void
_MBCSSingleFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                                  UErrorCode *pErrorCode) {
    UConverter *cnv;
    const UChar *source, *sourceLimit;
    uint8_t *target;
    int32_t targetCapacity;
    int32_t *offsets;

    const uint16_t *table;
    const uint8_t *bytes;

    UChar32 c;

    int32_t sourceIndex, nextSourceIndex;

    UConverterCallbackReason reason;
    uint32_t i;
    uint8_t value;
    UBool hasSupplementary;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    table=cnv->sharedData->table->mbcs.fromUnicodeTable;
    bytes=cnv->sharedData->table->mbcs.fromUnicodeBytes;

    hasSupplementary=cnv->sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY;

    /* get the converter state from UConverter */
    c=cnv->fromUSurrogateLead;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex= c==0 ? 0 : -1;
    nextSourceIndex=0;

    /* conversion loop */
    if(c!=0 && targetCapacity>0) {
        goto getTrail;
    }

    while(source<sourceLimit) {
        /*
         * This following test is to see if available input would overflow the output.
         * It does not catch output of more than one byte that
         * overflows as a result of a multi-byte character or callback output
         * from the last source character.
         * Therefore, those situations also test for overflows and will
         * then break the loop, too.
         */
        if(targetCapacity>0) {
            /*
             * Get a correct Unicode code point:
             * a single UChar for a BMP code point or
             * a matched surrogate pair for a "surrogate code point".
             */
            c=*source++;
            ++nextSourceIndex;
            if(UTF_IS_SURROGATE(c)) {
                if(UTF_IS_SURROGATE_FIRST(c)) {
getTrail:
                    if(source<sourceLimit) {
                        /* test the following code unit */
                        UChar trail=*source;
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++source;
                            ++nextSourceIndex;
                            c=UTF16_GET_PAIR_VALUE(c, trail);
                            if(!hasSupplementary) {
                                /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
                                /* callback(unassigned) */
                                reason=UCNV_UNASSIGNED;
                                *pErrorCode=U_INVALID_CHAR_FOUND;
                                goto callback;
                            }
                            /* convert this surrogate code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            reason=UCNV_ILLEGAL;
                            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                            goto callback;
                        }
                    } else {
                        /* no more input */
                        break;
                    }
                } else {
                    /* this is an unmatched trail code unit (2nd surrogate) */
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                    goto callback;
                }
            }

            /* convert the Unicode code point in c into codepage bytes */
            i=MBCS_STAGE_2_MULTIPLIER*(uint32_t)table[c>>10]+((c>>3)&0x7e);

            /* MBCS_OUTPUT_1 */
            value=bytes[16*(uint32_t)table[i+1]+(c&0xf)];

            /* is this code point assigned, or do we use fallbacks? */
            if( (table[i]&(1<<(c&0xf)))!=0 ||
                UCNV_FROM_U_USE_FALLBACK(cnv, c) && (value!=0 || c==0)
            ) {
                /* assigned, write the output character bytes from value and length */
                /* length==1 */
                /* this is easy because we know that there is enough space */
                *target++=value;
                if(offsets!=NULL) {
                    *offsets++=sourceIndex;
                }
                --targetCapacity;

                /* normal end of conversion: prepare for a new character */
                c=0;
                sourceIndex=nextSourceIndex;
                continue;
            } else { /* unassigned */
                /*
                 * We allow a 0 byte output if the Unicode code point is
                 * U+0000 and also if the "assigned" bit is set for this entry.
                 * There is no way with this data structure for fallback output
                 * for other than U+0000 to be a zero byte.
                 */
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
            }

callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=source;
            pArgs->target=(char *)target;
            pArgs->offsets=offsets;

            /* set the converter state in UConverter to deal with the next character */
            cnv->fromUSurrogateLead=0;

            /* write the code point as code units */
            i=0;
            UTF_APPEND_CHAR_UNSAFE(cnv->invalidUCharBuffer, i, c);
            cnv->invalidUCharLength=(int8_t)i;

            /* call the callback function */
            fromUCallback(cnv, cnv->fromUContext, pArgs, cnv->invalidUCharBuffer, i, c, reason, pErrorCode);

            /* get the converter state from UConverter */
            c=cnv->fromUSurrogateLead;

            /* update target and deal with offsets if necessary */
            offsets=ucnv_updateCallbackOffsets(offsets, ((uint8_t *)pArgs->target)-target, sourceIndex);
            target=(uint8_t *)pArgs->target;

            /* update the source pointer and index */
            sourceIndex=nextSourceIndex+(pArgs->source-source);
            source=pArgs->source;
            targetCapacity=(uint8_t *)pArgs->targetLimit-target;

            /*
             * If the callback overflowed the target, then we need to
             * stop here with an overflow indication.
             */
            if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
                break;
            } else if(U_FAILURE(*pErrorCode)) {
                /* break on error */
                c=0;
                break;
            } else if(cnv->charErrorBufferLength>0) {
                /* target is full */
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                break;
            }

            /*
             * We do not need to repeat the statements from the normal
             * end of the conversion because we already updated all the
             * necessary variables.
             */
        } else {
            /* target is full */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }

    if(pArgs->flush && source>=sourceLimit) {
        /* reset the state for the next conversion */
        if(c!=0 && U_SUCCESS(*pErrorCode)) {
            /* a Unicode code point remains incomplete (only a first surrogate) */
            *pErrorCode=U_TRUNCATED_CHAR_FOUND;
        }
        cnv->fromUSurrogateLead=0;
    } else {
        /* set the converter state back into UConverter */
        cnv->fromUSurrogateLead=(UChar)c;
    }

    /* write back the updated pointers */
    pArgs->source=source;
    pArgs->target=(char *)target;
    pArgs->offsets=offsets;
}

/*
 * This version of _MBCSFromUnicode() is optimized for single-byte codepages
 * that map only to and from the BMP.
 * In addition to single-byte/state optimizations, the offset calculations
 * become much easier.
 */
U_CFUNC void
_MBCSSingleFromBMPWithOffsets(UConverterFromUnicodeArgs *pArgs,
                              UErrorCode *pErrorCode) {
    UConverter *cnv;
    const UChar *source, *sourceLimit, *lastSource;
    uint8_t *target;
    int32_t targetCapacity, length;
    int32_t *offsets;

    const uint16_t *table;
    const uint8_t *bytes;

    UChar32 c;

    int32_t sourceIndex;

    UConverterCallbackReason reason;
    uint32_t i;
    uint8_t value;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    table=cnv->sharedData->table->mbcs.fromUnicodeTable;
    bytes=cnv->sharedData->table->mbcs.fromUnicodeBytes;

    /* get the converter state from UConverter */
    c=cnv->fromUSurrogateLead;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex= c==0 ? 0 : -1;
    lastSource=source;

    /*
     * since the conversion here is 1:1 UChar:uint8_t, we need only one counter
     * for the minimum of the sourceLength and targetCapacity
     */
    length=sourceLimit-source;
    if(length<targetCapacity) {
        targetCapacity=length;
    }

    /* conversion loop */
    if(c!=0 && targetCapacity>0) {
        goto getTrail;
    }

    while(targetCapacity>0) {
        /*
         * Get a correct Unicode code point:
         * a single UChar for a BMP code point or
         * a matched surrogate pair for a "surrogate code point".
         */
        c=*source++;
        /*
         * Do not immediately check for single surrogates:
         * Assume that they are unassigned and check for them in that case.
         * This speeds up the conversion of assigned characters.
         */
        /* convert the Unicode code point in c into codepage bytes */
        i=MBCS_STAGE_2_MULTIPLIER*(uint32_t)table[c>>10]+((c>>3)&0x7e);

        /* MBCS_OUTPUT_1 */
        value=bytes[16*(uint32_t)table[i+1]+(c&0xf)];

        /* is this code point assigned, or do we use fallbacks? */
        if( (table[i]&(1<<(c&0xf)))!=0 ||
            UCNV_FROM_U_USE_FALLBACK(cnv, c) && (value!=0 || c==0)
        ) {
            /* assigned, write the output character bytes from value and length */
            /* length==1 */
            /* this is easy because we know that there is enough space */
            *target++=value;
            --targetCapacity;

            /* normal end of conversion: prepare for a new character */
            c=0;
            continue;
        } else if(!UTF_IS_SURROGATE(c)) {
            /* normal, unassigned BMP character */
            /*
             * We allow a 0 byte output if the Unicode code point is
             * U+0000 and also if the "assigned" bit is set for this entry.
             * There is no way with this data structure for fallback output
             * for other than U+0000 to be a zero byte.
             */
            /* callback(unassigned) */
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
        } else if(UTF_IS_SURROGATE_FIRST(c)) {
getTrail:
            if(source<sourceLimit) {
                /* test the following code unit */
                UChar trail=*source;
                if(UTF_IS_SECOND_SURROGATE(trail)) {
                    ++source;
                    c=UTF16_GET_PAIR_VALUE(c, trail);
                    /* this codepage does not map supplementary code points */
                    /* callback(unassigned) */
                    reason=UCNV_UNASSIGNED;
                    *pErrorCode=U_INVALID_CHAR_FOUND;
                } else {
                    /* this is an unmatched lead code unit (1st surrogate) */
                    /* callback(illegal) */
                    reason=UCNV_ILLEGAL;
                    *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                }
            } else {
                /* no more input */
                break;
            }
        } else {
            /* this is an unmatched trail code unit (2nd surrogate) */
            /* callback(illegal) */
            reason=UCNV_ILLEGAL;
            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
        }

        /* call the callback function with all the preparations and post-processing */
        /* get the number of code units for c to correctly advance sourceIndex after the callback call */
        length=UTF_CHAR_LENGTH(c);

        /* set offsets since the start or the last callback */
        if(offsets!=NULL) {
            int32_t count=(int32_t)(source-lastSource);

            /* do not set the offset for the callback-causing character */
            count-=length;

            while(count>0) {
                *offsets++=sourceIndex++;
                --count;
            }
            /* offset and sourceIndex are now set for the current character */
        }

        /* update the arguments structure */
        pArgs->source=source;
        pArgs->target=(char *)target;
        pArgs->offsets=offsets;

        /* set the converter state in UConverter to deal with the next character */
        cnv->fromUSurrogateLead=0;

        /* write the code point as code units */
        i=0;
        UTF_APPEND_CHAR_UNSAFE(cnv->invalidUCharBuffer, i, c);
        cnv->invalidUCharLength=(int8_t)i;
        /* i==length */

        /* call the callback function */
        fromUCallback(cnv, cnv->fromUContext, pArgs, cnv->invalidUCharBuffer, i, c, reason, pErrorCode);

        /* get the converter state from UConverter */
        c=cnv->fromUSurrogateLead;

        /* update target and deal with offsets if necessary */
        offsets=ucnv_updateCallbackOffsets(offsets, ((uint8_t *)pArgs->target)-target, sourceIndex);
        target=(uint8_t *)pArgs->target;

        /* update the source pointer and index */
        sourceIndex+=length+(pArgs->source-source);
        source=lastSource=pArgs->source;
        targetCapacity=(uint8_t *)pArgs->targetLimit-target;
        length=sourceLimit-source;
        if(length<targetCapacity) {
            targetCapacity=length;
        }

        /*
         * If the callback overflowed the target, then we need to
         * stop here with an overflow indication.
         */
        if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
            break;
        } else if(U_FAILURE(*pErrorCode)) {
            /* break on error */
            c=0;
            break;
        } else if(cnv->charErrorBufferLength>0) {
            /* target is full */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            break;
        }
    }

    if(U_SUCCESS(*pErrorCode) && source<sourceLimit && target>=(uint8_t *)pArgs->targetLimit) {
        /* target is full */
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }

    /* set offsets since the start or the last callback */
    if(offsets!=NULL) {
        size_t count=source-lastSource;
        while(count>0) {
            *offsets++=sourceIndex++;
            --count;
        }
    }

    if(pArgs->flush && source>=sourceLimit) {
        /* reset the state for the next conversion */
        if(c!=0 && U_SUCCESS(*pErrorCode)) {
            /* a Unicode code point remains incomplete (only a first surrogate) */
            *pErrorCode=U_TRUNCATED_CHAR_FOUND;
        }
        cnv->fromUSurrogateLead=0;
    } else {
        /* set the converter state back into UConverter */
        cnv->fromUSurrogateLead=(UChar)c;
    }

    /* write back the updated pointers */
    pArgs->source=source;
    pArgs->target=(char *)target;
    pArgs->offsets=offsets;
}

/*
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
                 UBool useFallback) {
    const uint16_t *table=sharedData->table->mbcs.fromUnicodeTable;
    const uint8_t *p;
    uint32_t i;
    uint32_t value;
    int32_t length;

    /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
    if(c>=0x10000 && !(sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
        return 0;
    }

    /* convert the Unicode code point in c into codepage bytes (same as in _MBCSFromUnicodeWithOffsets) */
    i=MBCS_STAGE_2_MULTIPLIER*(uint32_t)table[c>>10]+((c>>3)&0x7e);
    p=sharedData->table->mbcs.fromUnicodeBytes;

    /* get the bytes and the length for the output */
    switch(sharedData->table->mbcs.outputType) {
    case MBCS_OUTPUT_1:
        p+=(16*(uint32_t)table[i+1]+(c&0xf));
        value=*p;
        length=1;
        break;
    case MBCS_OUTPUT_2:
        p+=(16*(uint32_t)table[i+1]+(c&0xf))*2;
#       if U_IS_BIG_ENDIAN
            value=*(uint16_t *)p;
#       else
            value=((uint32_t)*p<<8)|p[1];
#       endif
        if(value<=0xff) {
            length=1;
        } else {
            length=2;
        }
        break;
    case MBCS_OUTPUT_3:
        p+=(16*(uint32_t)table[i+1]+(c&0xf))*3;
        value=((uint32_t)*p<<16)|((uint32_t)p[1]<<8)|p[2];
        if(value<=0xff) {
            length=1;
        } else if(value<=0xffff) {
            length=2;
        } else {
            length=3;
        }
        break;
    case MBCS_OUTPUT_4:
        p+=(16*(uint32_t)table[i+1]+(c&0xf))*4;
#       if U_IS_BIG_ENDIAN
            value=*(uint32_t *)p;
#       else
            value=((uint32_t)*p<<24)|((uint32_t)p[1]<<16)|((uint32_t)p[2]<<8)|p[3];
#       endif
        if(value<=0xff) {
            length=1;
        } else if(value<=0xffff) {
            length=2;
        } else if(value<=0xffffff) {
            length=3;
        } else {
            length=4;
        }
        break;
    case MBCS_OUTPUT_3_EUC:
        p+=(16*(uint32_t)table[i+1]+(c&0xf))*2;
#       if U_IS_BIG_ENDIAN
            value=*(uint16_t *)p;
#       else
            value=((uint32_t)*p<<8)|p[1];
#       endif
        /* EUC 16-bit fixed-length representation */
        if(value<=0xff) {
            length=1;
        } else if((value&0x8000)==0) {
            value|=0x8e8000;
            length=3;
        } else if((value&0x80)==0) {
            value|=0x8f0080;
            length=3;
        } else {
            length=2;
        }
        break;
    case MBCS_OUTPUT_4_EUC:
        p+=(16*(uint32_t)table[i+1]+(c&0xf))*3;
        value=((uint32_t)*p<<16)|((uint32_t)p[1]<<8)|p[2];
        /* EUC 16-bit fixed-length representation applied to the first two bytes */
        if(value<=0xff) {
            length=1;
        } else if(value<=0xffff) {
            length=2;
        } else if((value&0x800000)==0) {
            value|=0x8e800000;
            length=4;
        } else if((value&0x8000)==0) {
            value|=0x8f008000;
            length=4;
        } else {
            length=3;
        }
        break;
    default:
        /* must not occur */
        return -1;
    }

    /* is this code point assigned, or do we use fallbacks? */
    if( (table[i]&(1<<(c&0xf)))!=0 ||
        FROM_U_USE_FALLBACK(useFallback, c) && (value!=0 || c==0)
    ) {
        /*
         * We allow a 0 byte output if the Unicode code point is
         * U+0000 and also if the "assigned" bit is set for this entry.
         * There is no way with this data structure for fallback output
         * for other than U+0000 to be a zero byte.
         */
        /* assigned */
        *pValue=value;
        return length;
    } else {
        return 0;
    }
}

U_CFUNC int32_t
_MBCSSingleFromUChar32(UConverterSharedData *sharedData,
                       UChar32 c,
                       UBool useFallback) {
    const uint16_t *table;
    uint32_t i;
    int32_t value;

    /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
    if(c>=0x10000 && !(sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
        return -1;
    }

    /* convert the Unicode code point in c into codepage bytes (same as in _MBCSFromUnicodeWithOffsets) */
    table=sharedData->table->mbcs.fromUnicodeTable;
    i=MBCS_STAGE_2_MULTIPLIER*(uint32_t)table[c>>10]+((c>>3)&0x7e);

    /* get the byte for the output */
    /* MBCS_OUTPUT_1 */
    value=sharedData->table->mbcs.fromUnicodeBytes[16*(uint32_t)table[i+1]+(c&0xf)];

    /* is this code point assigned, or do we use fallbacks? */
    if( (table[i]&(1<<(c&0xf)))!=0 ||
        FROM_U_USE_FALLBACK(useFallback, c) && (value!=0 || c==0)
    ) {
        /*
         * We allow a 0 byte output if the Unicode code point is
         * U+0000 and also if the "assigned" bit is set for this entry.
         * There is no way with this data structure for fallback output
         * for other than U+0000 to be a zero byte.
         */
        /* assigned */
        return value;
    } else {
        return -1;
    }
}

/* miscellaneous ------------------------------------------------------------ */

static void
_MBCSGetStarters(const UConverter* cnv,
                 UBool starters[256],
                 UErrorCode *pErrorCode) {
    const int32_t *state0=cnv->sharedData->table->mbcs.stateTable[0];
    int i;

    for(i=0; i<256; ++i) {
        /* all bytes that cause a state transition from state 0 are lead bytes */
        starters[i]= (UBool)(state0[i]>=0);
    }
}

/*
 * This is an internal function that allows other converter implementations
 * to check whether a byte is a lead byte.
 */
U_CFUNC UBool
_MBCSIsLeadByte(UConverterSharedData *sharedData, char byte) {
    return (UBool)(sharedData->table->mbcs.stateTable[0][(uint8_t)byte]>=0);
}

U_CFUNC void
_MBCSWriteSub(UConverterFromUnicodeArgs *pArgs,
              int32_t offsetIndex,
              UErrorCode *pErrorCode) {
    UConverter *cnv=pArgs->converter;
    char *p;
    char buffer[4];

    switch(cnv->sharedData->table->mbcs.outputType) {
    case MBCS_OUTPUT_2_SISO:
        p=buffer;

        /* fromUnicodeStatus contains prevLength */
        switch(cnv->subCharLen) {
        case 1:
            if(cnv->fromUnicodeStatus==2) {
                /* DBCS mode and SBCS sub char: change to SBCS */
                cnv->fromUnicodeStatus=1;
                *p++=UCNV_SI;
            }
            *p++=cnv->subChar[0];
            break;
        case 2:
            if(cnv->fromUnicodeStatus==1) {
                /* SBCS mode and DBCS sub char: change to DBCS */
                cnv->fromUnicodeStatus=2;
                *p++=UCNV_SO;
            }
            *p++=cnv->subChar[0];
            *p++=cnv->subChar[1];
            break;
        default:
            *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        ucnv_cbFromUWriteBytes(pArgs,
                               buffer, (int32_t)(p-buffer),
                               offsetIndex, pErrorCode);
        break;
    default:
        ucnv_cbFromUWriteBytes(pArgs,
                               (const char *)cnv->subChar, cnv->subCharLen,
                               offsetIndex, pErrorCode);
        break;
    }
}

U_CFUNC UConverterType
_MBCSGetType(const UConverter* converter) {
    /* SBCS, DBCS, and EBCDIC_STATEFUL are replaced by MBCS, but here we cheat a little */
    if(converter->sharedData->table->mbcs.countStates==1) {
        return (UConverterType)UCNV_SBCS;
    } else if((converter->sharedData->table->mbcs.outputType&0xff)==MBCS_OUTPUT_2_SISO) {
        return (UConverterType)UCNV_EBCDIC_STATEFUL;
    } else if(converter->sharedData->staticData->minBytesPerChar==2 && converter->sharedData->staticData->maxBytesPerChar==2) {
        return (UConverterType)UCNV_DBCS;
    }
    return (UConverterType)UCNV_MBCS;
}

static const UConverterImpl _MBCSImpl={
    UCNV_MBCS,

    _MBCSLoad,
    NULL,

    _MBCSOpen,
    NULL,
    _MBCSReset,

    _MBCSToUnicodeWithOffsets,
    _MBCSToUnicodeWithOffsets,
    _MBCSFromUnicodeWithOffsets,
    _MBCSFromUnicodeWithOffsets,
    _MBCSGetNextUChar,

    _MBCSGetStarters,
    NULL,
    _MBCSWriteSub
};


/* Static data is in tools/makeconv/ucnvstat.c for data-based
 * converters. Be sure to update it as well.
 */

const UConverterSharedData _MBCSData={
    sizeof(UConverterSharedData), 1,
    NULL, NULL, NULL, FALSE, &_MBCSImpl, 
    0
};

/* GB 18030 special handling ------------------------------------------------ */

/* definition of LINEAR macros and gb18030Ranges see near the beginning of the file */

/* the callback functions handle GB 18030 specially */
static void
fromUCallback(UConverter *cnv,
              void *context, UConverterFromUnicodeArgs *pArgs,
              const UChar *codeUnits, int32_t length, UChar32 codePoint,
              UConverterCallbackReason reason, UErrorCode *pErrorCode) {
    if(cnv->extraInfo==gb18030Ranges && reason==UCNV_UNASSIGNED) {
        const uint32_t *range;
        int i;

        range=gb18030Ranges[0];
        for(i=0; i<sizeof(gb18030Ranges)/sizeof(gb18030Ranges[0]); range+=4, ++i) {
            if(range[0]<=(uint32_t)codePoint && (uint32_t)codePoint<=range[1]) {
                uint32_t linear;
                char bytes[4];

                /* found the Unicode code point, output the four-byte sequence for it */
                *pErrorCode=U_ZERO_ERROR;

                /* get the linear value of the first GB 18030 code in this range */
                linear=range[2]-LINEAR_18030_BASE;

                /* add the offset from the beginning of the range */
                linear+=((uint32_t)codePoint-range[0]);

                /* turn this into a four-byte sequence */
                bytes[3]=(const char)(0x30+linear%10); linear/=10;
                bytes[2]=(const char)(0x81+linear%126); linear/=126;
                bytes[1]=(const char)(0x30+linear%10); linear/=10;
                bytes[0]=(const char)(0x81+linear);

                /* output this sequence */
                ucnv_cbFromUWriteBytes(pArgs, bytes, 4, 0, pErrorCode);
                return;
            }
        }
    }

    /* call the normal callback function */
    cnv->fromUCharErrorBehaviour(context, pArgs, codeUnits, length, codePoint, reason, pErrorCode);
}

static void
toUCallback(UConverter *cnv,
            void *context, UConverterToUnicodeArgs *pArgs,
            const char *codeUnits, int32_t length,
            UConverterCallbackReason reason, UErrorCode *pErrorCode) {
    if(cnv->extraInfo==gb18030Ranges && reason==UCNV_UNASSIGNED && length==4) {
        const uint32_t *range;
        uint32_t linear;
        int i;

        linear=LINEAR_18030((uint8_t)codeUnits[0], (uint8_t)codeUnits[1], (uint8_t)codeUnits[2], (uint8_t)codeUnits[3]);
        range=gb18030Ranges[0];
        for(i=0; i<sizeof(gb18030Ranges)/sizeof(gb18030Ranges[0]); range+=4, ++i) {
            if(range[2]<=linear && linear<=range[3]) {
                UChar u[UTF_MAX_CHAR_LENGTH];

                /* found the sequence, output the Unicode code point for it */
                *pErrorCode=U_ZERO_ERROR;

                /* add the linear difference between the input and start sequences to the start code point */
                linear=range[0]+(linear-range[2]);

                /* write the result as UChars and output */
                i=0;
                UTF_APPEND_CHAR_UNSAFE(u, i, linear);
                ucnv_cbToUWriteUChars(pArgs, u, i, 0, pErrorCode);
                return;
            }
        }
    }

    /* call the normal callback function */
    cnv->fromCharErrorBehaviour(context, pArgs, codeUnits, length, reason, pErrorCode);
}
