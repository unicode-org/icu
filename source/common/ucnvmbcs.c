/*
******************************************************************************
*
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
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
*     stateful encodings (used for EBCDIC_STATEFUL)
*   - it is possible to convert Unicode code points other than U+0000
*     to a single zero byte (but not as a fallback except for SBCS)
*
*   Remaining limitations in fromUnicode:
*   - byte sequences must not have leading zero bytes
*   - except for SBCS codepages: no fallback mapping from Unicode to a zero byte
*   - limitation to up to 4 bytes per character
*
*   Change history: 
*
*    5/6/2001       Ram       Moved  MBCS_SINGLE_RESULT_FROM_U,MBCS_STAGE_2_FROM_U,
*                             MBCS_VALUE_2_FROM_STAGE_2, MBCS_VALUE_4_FROM_STAGE_2
*                             macros to ucnvmbcs.h file
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_LEGACY_CONVERSION

#include "unicode/ucnv.h"
#include "unicode/ucnv_cb.h"
#include "unicode/udata.h"
#include "unicode/uset.h"
#include "ucnv_bld.h"
#include "ucnvmbcs.h"
#include "ucnv_cnv.h"
#include "umutex.h"
#include "cmemory.h"
#include "cstring.h"

/* control optimizations according to the platform */
#define MBCS_UNROLL_SINGLE_TO_BMP 1
#define MBCS_UNROLL_SINGLE_FROM_BMP 0

/*
 * _MBCSHeader versions 4.1
 * (Note that the _MBCSHeader version is in addition to the converter formatVersion.)
 *
 * Change from version 4.0:
 * - Replace header.reserved with header.fromUBytesLength so that all
 *   fields in the data have length.
 *
 * Changes from version 3 (for performance improvements):
 * - new bit distribution for state table entries
 * - reordered action codes
 * - new data structure for single-byte fromUnicode
 *   + stage 2 only contains indexes
 *   + stage 3 stores 16 bits per character with classification bits 15..8
 * - no multiplier for stage 1 entries
 * - stage 2 for non-single-byte codepages contains the index and the flags in
 *   one 32-bit value
 * - 2-byte and 4-byte fromUnicode results are stored directly as 16/32-bit integers
 *
 * For more details about old versions of the MBCS data structure, see
 * the corresponding versions of this file.
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
 * Each entry is 32 bits wide, with two formats distinguished by
 * the sign bit (bit 31):
 *
 * One format for transitional entries (bit 31 not set) for non-final bytes, and
 * one format for final entries (bit 31 set).
 * Both formats contain the number of the next state in the same bit
 * positions.
 * State 0 is the initial state.
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
 * code point (16 bits for a BMP code point or 20 bits for a supplementary
 * code point (stored as code point minus 0x10000 so that 20 bits are enough).
 * For others, the code point in the Unicode table is stored with either
 * one or two code units: one for BMP code points, two for a pair of
 * surrogates.
 * All code points for a final state entry take up the same number of code
 * units, regardless of whether they all actually _use_ the same number
 * of code units. This is necessary for simple array access.
 *
 * An additional feature comes in with what in ICU is called "fallback"
 * mappings:
 *
 * In addition to round-trippable, precise, 1:1 mappings, there are often
 * mappings defined between similar, though not the same, characters.
 * Typically, such mappings occur only in fromUnicode mapping tables because
 * Unicode has a superset repertoire of most other codepages. However, it
 * is possible to provide such mappings in the toUnicode tables, too.
 * In this case, the fallback mappings are partly integrated into the
 * general state tables because the structure of the encoding includes their
 * byte sequences.
 * For final entries in an initial state, fallback mappings are stored in
 * the entry itself like with roundtrip mappings.
 * For other final entries, they are stored in the code units table if
 * the entry is for a pair of code units.
 * For single-unit results in the code units table, there is no space to
 * alternatively hold a fallback mapping; in this case, the code unit
 * is stored as U+fffe (unassigned), and the fallback mapping needs to
 * be looked up by the scalar offset value in a separate table.
 *
 * "Unassigned" state entries really mean "structurally unassigned",
 * i.e., such a byte sequence will never have a mapping result.
 *
 * The interpretation of the bits in each entry is as follows:
 *
 * Bit 31 not set, not a terminal entry ("transitional"):
 * 30..24 next state
 * 23..0  offset delta, to be added up
 *
 * Bit 31 set, terminal ("final") entry:
 * 30..24 next state (regardless of action code)
 * 23..20 action code:
 *        action codes 0 and 1 result in precise-mapping Unicode code points
 *        0  valid byte sequence
 *           19..16 not used, 0
 *           15..0  16-bit Unicode BMP code point
 *                  never U+fffe or U+ffff
 *        1  valid byte sequence
 *           19..0  20-bit Unicode supplementary code point
 *                  never U+fffe or U+ffff
 *
 *        action codes 2 and 3 result in fallback (unidirectional-mapping) Unicode code points
 *        2  valid byte sequence (fallback)
 *           19..16 not used, 0
 *           15..0  16-bit Unicode BMP code point as fallback result
 *        3  valid byte sequence (fallback)
 *           19..0  20-bit Unicode supplementary code point as fallback result
 *
 *        action codes 4 and 5 may result in roundtrip/fallback/unassigned/illegal results
 *        depending on the code units they result in
 *        4  valid byte sequence
 *           19..9  not used, 0
 *            8..0  final offset delta
 *                  pointing to one 16-bit code unit which may be
 *                  fffe  unassigned -- look for a fallback for this offset
 *                  ffff  illegal
 *        5  valid byte sequence
 *           19..9  not used, 0
 *            8..0  final offset delta
 *                  pointing to two 16-bit code units
 *                  (typically UTF-16 surrogates)
 *                  the result depends on the first code unit as follows:
 *                  0000..d7ff  roundtrip BMP code point (1st alone)
 *                  d800..dbff  roundtrip surrogate pair (1st, 2nd)
 *                  dc00..dfff  fallback surrogate pair (1st-400, 2nd)
 *                  e000        roundtrip BMP code point (2nd alone)
 *                  e001        fallback BMP code point (2nd alone)
 *                  fffe        unassigned
 *                  ffff        illegal
 *           (the final offset deltas are at most 255 * 2,
 *            times 2 because of storing code unit pairs)
 *
 *        6  unassigned byte sequence
 *           19..16 not used, 0
 *           15..0  16-bit Unicode BMP code point U+fffe (new with version 2)
 *                  this does not contain a final offset delta because the main
 *                  purpose of this action code is to save scalar offset values;
 *                  therefore, fallback values cannot be assigned to byte
 *                  sequences that result in this action code
 *        7  illegal byte sequence
 *           19..16 not used, 0
 *           15..0  16-bit Unicode BMP code point U+ffff (new with version 2)
 *        8  state change only
 *           19..0  not used, 0
 *           useful for state changes in simple stateful encodings,
 *           at Shift-In/Shift-Out codes
 *
 *
 *        9..15 reserved for future use
 *           current implementations will only perform a state change
 *           and ignore bits 19..0
 *
 * An encoding with contiguous ranges of unassigned byte sequences, like
 * Shift-JIS and especially EUC-TW, can be stored efficiently by having
 * at least two states for the trail bytes:
 * One trail byte state that results in code points, and one that only
 * has "unassigned" and "illegal" terminal states.
 *
 * Note: partly by accident, this data structure supports simple stateless
 * encodings without any additional logic.
 * Currently, only simple Shift-In/Shift-Out schemes are handled with
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
 * Beginning with version 4, single-byte codepages have a significantly different
 * trie compared to other codepages.
 * In all cases, the entry in stage 1 is directly the index of the block of
 * 64 entries in stage 2.
 *
 * Single-byte lookup:
 *
 * Stage 2 only contains 16-bit indexes directly to the 16-blocks in stage 3.
 * Stage 3 contains one 16-bit word per result:
 * Bits 15..8 indicate the kind of result:
 *    f  roundtrip result
 *    c  fallback result from private-use code point
 *    8  fallback result from other code points
 *    0  unassigned
 * Bits 7..0 contain the codepage byte. A zero byte is always possible.
 *
 * Multi-byte lookup:
 *
 * Stage 2 contains a 32-bit word for each 16-block in stage 3:
 * Bits 31..16 contain flags for which stage 3 entries contain roundtrip results
 *             test: MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c)
 *             If this test is false, then a non-zero result will be interpreted as
 *             a fallback mapping.
 * Bits 15..0  contain the index to stage 3, which must be multiplied by 16*(bytes per char)
 *
 * Stage 3 contains 2, 3, or 4 bytes per result.
 * 2 or 4 bytes are stored as uint16_t/uint32_t in platform endianness,
 * while 3 bytes are stored as bytes in big-endian order.
 * Leading zero bytes are ignored, and the number of bytes is counted.
 * A zero byte mapping result is possible as a roundtrip result.
 * For some output types, the actual result is processed from this;
 * see _MBCSFromUnicodeWithOffsets().
 *
 * Note that stage 1 always contains 0x440=1088 entries (0x440==0x110000>>10),
 * or (version 3 and up) for BMP-only codepages, it contains 64 entries.
 *
 * In version 3, stage 2 blocks may overlap by multiples of the multiplier
 * for compaction.
 * In version 4, stage 2 blocks (and for single-byte codepages, stage 3 blocks)
 * may overlap by any number of entries.
 *
 * MBCS version 2 added:
 * the converter checks for known output types, which allows
 * adding new ones without crashing an unaware converter
 */

/* prototypes --------------------------------------------------------------- */

static void
_MBCSSingleToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                                UErrorCode *pErrorCode);

static void
_MBCSSingleToBMPWithOffsets(UConverterToUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode);

static UChar32
_MBCSGetNextUChar(UConverterToUnicodeArgs *pArgs,
                  UErrorCode *pErrorCode);

static UChar32
_MBCSSingleGetNextUChar(UConverterToUnicodeArgs *pArgs,
                        UErrorCode *pErrorCode);

static void
_MBCSDoubleFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                                  UErrorCode *pErrorCode);

static void
_MBCSSingleFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                                  UErrorCode *pErrorCode);

static void
_MBCSSingleFromBMPWithOffsets(UConverterFromUnicodeArgs *pArgs,
                              UErrorCode *pErrorCode);

static void
fromUCallback(UConverter *cnv,
              const void *context, UConverterFromUnicodeArgs *pArgs,
              UChar32 codePoint,
              UConverterCallbackReason reason, UErrorCode *pErrorCode);

static void
toUCallback(UConverter *cnv,
            const void *context, UConverterToUnicodeArgs *pArgs,
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
    {0x10000, 0x10FFFF, LINEAR(0x90308130), LINEAR(0xE3329A35)},
    {0x9FA6, 0xD7FF, LINEAR(0x82358F33), LINEAR(0x8336C738)},
    {0x0452, 0x200F, LINEAR(0x8130D330), LINEAR(0x8136A531)},
    {0xE865, 0xF92B, LINEAR(0x8336D030), LINEAR(0x84308534)},
    {0x2643, 0x2E80, LINEAR(0x8137A839), LINEAR(0x8138FD38)},
    {0xFA2A, 0xFE2F, LINEAR(0x84309C38), LINEAR(0x84318537)},
    {0x3CE1, 0x4055, LINEAR(0x8231D438), LINEAR(0x8232AF32)},
    {0x361B, 0x3917, LINEAR(0x8230A633), LINEAR(0x8230F237)},
    {0x49B8, 0x4C76, LINEAR(0x8234A131), LINEAR(0x8234E733)},
    {0x4160, 0x4336, LINEAR(0x8232C937), LINEAR(0x8232F837)},
    {0x478E, 0x4946, LINEAR(0x8233E838), LINEAR(0x82349638)},
    {0x44D7, 0x464B, LINEAR(0x8233A339), LINEAR(0x8233C931)},
    {0xFFE6, 0xFFFF, LINEAR(0x8431A234), LINEAR(0x8431A439)}
};

/* bit flag for UConverter.options indicating GB 18030 special handling */
#define _MBCS_OPTION_GB18030 0x8000

/* Miscellaneous ------------------------------------------------------------ */

static uint32_t
_MBCSSizeofFromUBytes(UConverterMBCSTable *mbcsTable) {
    const uint16_t *table;

    uint32_t st3, maxStage3;
    uint16_t st1, maxStage1, st2;

    if(mbcsTable->fromUBytesLength>0) {
        /*
         * We _know_ the number of bytes in the fromUnicodeBytes array
         * starting with header.version 4.1.
         * Otherwise, below, we need to enumerate the fromUnicode
         * trie and find the highest entry.
         */
        return mbcsTable->fromUBytesLength;
    }

    /* Enumerate the from-Unicode trie table to find the highest stage 3 index. */
    table=mbcsTable->fromUnicodeTable;
    maxStage3=0;
    if(mbcsTable->unicodeMask&UCNV_HAS_SUPPLEMENTARY) {
        maxStage1=0x440;
    } else {
        maxStage1=0x40;
    }


    if(mbcsTable->outputType==MBCS_OUTPUT_1) {
        const uint16_t *stage2;

        for(st1=0; st1<maxStage1; ++st1) {
            st2=table[st1];
            if(st2>maxStage1) {
                stage2=table+st2;
                for(st2=0; st2<64; ++st2) {
                    st3=stage2[st2];
                    if(st3>maxStage3) {
                        maxStage3=st3;
                    }
                }
            }
        }

        /*
         * add 16 to get the limit not start index of the last stage 3 block,
         * times 2 for number of bytes
         */
        return (maxStage3+16)*2;
    } else {
        const uint32_t *stage2;

        for(st1=0; st1<maxStage1; ++st1) {
            st2=table[st1];
            if(st2>(maxStage1>>1)) {
                stage2=(const uint32_t *)table+st2;
                for(st2=0; st2<64; ++st2) {
                    st3=stage2[st2]&0xffff;
                    if(st3>maxStage3) {
                        maxStage3=st3;
                    }
                }
            }
        }

        /*
         * add 16 to get the limit not start index of the last stage 3 block,
         * times 2..4 for number of bytes
         */
        maxStage3=16*maxStage3+16;
        switch(mbcsTable->outputType) {
        case MBCS_OUTPUT_3:
        case MBCS_OUTPUT_4_EUC:
            maxStage3*=3;
            break;
        case MBCS_OUTPUT_4:
            maxStage3*=4;
            break;
        default:
            /* MBCS_OUTPUT_2... and MBCS_OUTPUT_3_EUC */
            maxStage3*=2;
            break;
        }
        return maxStage3;
    }
}

static void
_MBCSGetUnicodeSet(const UConverter *cnv,
                   USet *set,
                   UConverterUnicodeSet which,
                   UErrorCode *pErrorCode) {
    UConverterMBCSTable *mbcsTable;
    const uint16_t *table;

    uint32_t st3;
    uint16_t st1, maxStage1, st2;

    UChar32 c;

    if(cnv->options&_MBCS_OPTION_GB18030) {
        uset_addRange(set, 0, 0xd7ff);
        uset_addRange(set, 0xe000, 0x10ffff);
        return;
    }

    /* enumerate the from-Unicode trie table */
    mbcsTable=&cnv->sharedData->table->mbcs;
    table=mbcsTable->fromUnicodeTable;
    if(mbcsTable->unicodeMask&UCNV_HAS_SUPPLEMENTARY) {
        maxStage1=0x440;
    } else {
        maxStage1=0x40;
    }

    c=0; /* keep track of the current code point while enumerating */

    if(mbcsTable->outputType==MBCS_OUTPUT_1) {
        const uint16_t *stage2, *stage3, *results;

        results=(const uint16_t *)mbcsTable->fromUnicodeBytes;

        for(st1=0; st1<maxStage1; ++st1) {
            st2=table[st1];
            if(st2>maxStage1) {
                stage2=table+st2;
                for(st2=0; st2<64; ++st2) {
                    if((st3=stage2[st2])!=0) {
                        /* read the stage 3 block */
                        stage3=results+st3;

                        /*
                         * Add code points for which the roundtrip flag is set.
                         * Once we get a set for fallback mappings, we have to use
                         * a threshold variable with a value of 0x800.
                         * See _MBCSSingleFromBMPWithOffsets() and
                         * MBCS_SINGLE_RESULT_FROM_U() for details.
                         */
                        do {
                            if(*stage3++>=0xf00) {
                                uset_add(set, c);
                            }
                        } while((++c&0xf)!=0);
                    } else {
                        c+=16; /* empty stage 3 block */
                    }
                }
            } else {
                c+=1024; /* empty stage 2 block */
            }
        }
    } else {
        const uint32_t *stage2;

        for(st1=0; st1<maxStage1; ++st1) {
            st2=table[st1];
            if(st2>(maxStage1>>1)) {
                stage2=(const uint32_t *)table+st2;
                for(st2=0; st2<64; ++st2) {
                    if((st3=stage2[st2])!=0) {
                        /* get the roundtrip flags for the stage 3 block */
                        st3>>=16;

                        /*
                         * Add code points for which the roundtrip flag is set.
                         * Once we get a set for fallback mappings, we have to check
                         * non-roundtrip stage 3 results for whether they are 0.
                         * See _MBCSFromUnicodeWithOffsets() for details.
                         */
                        do {
                            if(st3&1) {
                                uset_add(set, c);
                            }
                            st3>>=1;
                        } while((++c&0xf)!=0);
                    } else {
                        c+=16; /* empty stage 3 block */
                    }
                }
            } else {
                c+=1024; /* empty stage 2 block */
            }
        }
    }
}

/* EBCDIC swap LF<->NL ------------------------------------------------------ */

/*
 * This code modifies a standard EBCDIC<->Unicode mapping table for
 * OS/390 (z/OS) Unix System Services (Open Edition).
 * The difference is in the mapping of Line Feed and New Line control codes:
 * Standard EBCDIC maps
 *
 *   <U000A> \x25 |0
 *   <U0085> \x15 |0
 *
 * but OS/390 USS EBCDIC swaps the control codes for LF and NL,
 * mapping
 *
 *   <U000A> \x15 |0
 *   <U0085> \x25 |0
 *
 * This code modifies a loaded standard EBCDIC<->Unicode mapping table
 * by copying it into allocated memory and swapping the LF and NL values.
 * It allows to support the same EBCDIC charset in both versions without
 * duplicating the entire installed table.
 */

/* standard EBCDIC codes */
#define EBCDIC_LF 0x25
#define EBCDIC_NL 0x15

/* standard EBCDIC codes with roundtrip flag as stored in Unicode-to-single-byte tables */
#define EBCDIC_RT_LF 0xf25
#define EBCDIC_RT_NL 0xf15

/* Unicode code points */
#define U_LF 0x0a
#define U_NL 0x85

static UBool
_EBCDICSwapLFNL(UConverterSharedData *sharedData, UErrorCode *pErrorCode) {
    UConverterMBCSTable *mbcsTable;

    const uint16_t *table, *results;
    const uint8_t *bytes;

    int32_t (*newStateTable)[256];
    uint16_t *newResults;
    uint8_t *p;
    char *name;

    uint32_t stage2Entry;
    uint32_t size, sizeofFromUBytes;

    mbcsTable=&sharedData->table->mbcs;

    table=mbcsTable->fromUnicodeTable;
    bytes=mbcsTable->fromUnicodeBytes;
    results=(const uint16_t *)bytes;

    /*
     * Check that this is an EBCDIC table with SBCS portion -
     * SBCS or EBCDIC_STATEFUL with standard EBCDIC LF and NL mappings.
     *
     * If not, ignore the option. Options are always ignored if they do not apply.
     */
    if(!(
         (mbcsTable->outputType==MBCS_OUTPUT_1 || mbcsTable->outputType==MBCS_OUTPUT_2_SISO) &&
         mbcsTable->stateTable[0][EBCDIC_LF]==MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, U_LF) &&
         mbcsTable->stateTable[0][EBCDIC_NL]==MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, U_NL)
    )) {
        return FALSE;
    }

    if(mbcsTable->outputType==MBCS_OUTPUT_1) {
        if(!(
             EBCDIC_RT_LF==MBCS_SINGLE_RESULT_FROM_U(table, results, U_LF) &&
             EBCDIC_RT_NL==MBCS_SINGLE_RESULT_FROM_U(table, results, U_NL)
        )) {
            return FALSE;
        }
    } else /* MBCS_OUTPUT_2_SISO */ {
        stage2Entry=MBCS_STAGE_2_FROM_U(table, U_LF);
        if(!(
             MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, U_LF)!=0 &&
             EBCDIC_LF==MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, U_LF)
        )) {
            return FALSE;
        }

        stage2Entry=MBCS_STAGE_2_FROM_U(table, U_NL);
        if(!(
             MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, U_NL)!=0 &&
             EBCDIC_NL==MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, U_NL)
        )) {
            return FALSE;
        }
    }

    /*
     * The table has an appropriate format.
     * Allocate and build
     * - a modified to-Unicode state table
     * - a modified from-Unicode output array
     * - a converter name string with the swap option appended
     */
    sizeofFromUBytes=_MBCSSizeofFromUBytes(mbcsTable);
    size=
        mbcsTable->countStates*1024+
        sizeofFromUBytes+
        UCNV_MAX_CONVERTER_NAME_LENGTH+20;
    p=(uint8_t *)uprv_malloc(size);
    if(p==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return FALSE;
    }

    /* copy and modify the to-Unicode state table */
    newStateTable=(int32_t (*)[256])p;
    uprv_memcpy(newStateTable, mbcsTable->stateTable, mbcsTable->countStates*1024);

    newStateTable[0][EBCDIC_LF]=MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, U_NL);
    newStateTable[0][EBCDIC_NL]=MBCS_ENTRY_FINAL(0, MBCS_STATE_VALID_DIRECT_16, U_LF);

    /* copy and modify the from-Unicode result table */
    newResults=(uint16_t *)newStateTable[mbcsTable->countStates];
    uprv_memcpy(newResults, bytes, sizeofFromUBytes);

    /* conveniently, the table access macros work on the left side of expressions */
    if(mbcsTable->outputType==MBCS_OUTPUT_1) {
        MBCS_SINGLE_RESULT_FROM_U(table, newResults, U_LF)=EBCDIC_RT_NL;
        MBCS_SINGLE_RESULT_FROM_U(table, newResults, U_NL)=EBCDIC_RT_LF;
    } else /* MBCS_OUTPUT_2_SISO */ {
        stage2Entry=MBCS_STAGE_2_FROM_U(table, U_LF);
        MBCS_VALUE_2_FROM_STAGE_2(newResults, stage2Entry, U_LF)=EBCDIC_NL;

        stage2Entry=MBCS_STAGE_2_FROM_U(table, U_NL);
        MBCS_VALUE_2_FROM_STAGE_2(newResults, stage2Entry, U_NL)=EBCDIC_LF;
    }

    /* set the canonical converter name */
    name=(char *)newResults+sizeofFromUBytes;
    uprv_strcpy(name, sharedData->staticData->name);
    uprv_strcat(name, UCNV_SWAP_LFNL_OPTION_STRING);

    /* set the pointers */
    umtx_lock(NULL);
    if(mbcsTable->swapLFNLStateTable==NULL) {
        mbcsTable->swapLFNLStateTable=newStateTable;
        mbcsTable->swapLFNLFromUnicodeBytes=(uint8_t *)newResults;
        mbcsTable->swapLFNLName=name;

        newStateTable=NULL;
    }
    umtx_unlock(NULL);

    /* release the allocated memory if another thread beat us to it */
    if(newStateTable!=NULL) {
        uprv_free(newStateTable);
    }
    return TRUE;
}

/* MBCS setup functions ----------------------------------------------------- */

static void
_MBCSLoad(UConverterSharedData *sharedData,
          const uint8_t *raw,
          UErrorCode *pErrorCode) {
    UDataInfo info;
    UConverterMBCSTable *mbcsTable=&sharedData->table->mbcs;
    _MBCSHeader *header=(_MBCSHeader *)raw;

    if(header->version[0]!=4) {
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
    mbcsTable->fromUBytesLength=header->fromUBytesLength;
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
    if(info.formatVersion[0]>6 || (info.formatVersion[0]==6 && info.formatVersion[1]>=1)) {
        /* mask off possible future extensions to be safe */
        mbcsTable->unicodeMask=(uint8_t)(sharedData->staticData->unicodeMask&3);
    } else {
        /* for older versions, assume worst case: contains anything possible (prevent over-optimizations) */
        mbcsTable->unicodeMask=UCNV_HAS_SUPPLEMENTARY|UCNV_HAS_SURROGATES;
    }
}

static void
_MBCSUnload(UConverterSharedData *sharedData) {
    UConverterMBCSTable *mbcsTable=&sharedData->table->mbcs;

    if(mbcsTable->swapLFNLStateTable!=NULL) {
        uprv_free(mbcsTable->swapLFNLStateTable);
    }
}

static void
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

static void
_MBCSOpen(UConverter *cnv,
          const char *name,
          const char *locale,
          uint32_t options,
          UErrorCode *pErrorCode) {
    if((options&UCNV_OPTION_SWAP_LFNL)!=0) {
        /* do this because double-checked locking is broken */
        UBool isCached;

        umtx_lock(NULL);
        isCached=cnv->sharedData->table->mbcs.swapLFNLStateTable!=NULL;
        umtx_unlock(NULL);

        if(!isCached) {
            if(!_EBCDICSwapLFNL(cnv->sharedData, pErrorCode)) {
                /* the option does not apply, remove it */
                cnv->options&=~UCNV_OPTION_SWAP_LFNL;
            }
        }
    }


    if(uprv_strstr(name, "18030")!=NULL) {
        if(uprv_strstr(name, "gb18030")!=NULL || uprv_strstr(name, "GB18030")!=NULL) {
            /* set a flag for GB 18030 mode, which changes the callback behavior */
            cnv->options|=_MBCS_OPTION_GB18030;
        }
    }

    _MBCSReset(cnv, UCNV_RESET_BOTH);
}

static const char *
_MBCSGetName(const UConverter *cnv) {
    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0 && cnv->sharedData->table->mbcs.swapLFNLName!=NULL) {
        return cnv->sharedData->table->mbcs.swapLFNLName;
    } else {
        return cnv->sharedData->staticData->name;
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
    uint8_t action;
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

    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        stateTable=(const int32_t (*)[256])cnv->sharedData->table->mbcs.swapLFNLStateTable;
    } else {
        stateTable=cnv->sharedData->table->mbcs.stateTable;
    }
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
            ++nextSourceIndex;
            entry=stateTable[state][bytes[byteIndex++]=*source++];
            if(MBCS_ENTRY_IS_TRANSITION(entry)) {
                state=(uint8_t)MBCS_ENTRY_TRANSITION_STATE(entry);
                offset+=MBCS_ENTRY_TRANSITION_OFFSET(entry);
            } else {
                /* set the next state early so that we can reuse the entry variable */
                state=(uint8_t)MBCS_ENTRY_FINAL_STATE(entry); /* typically 0 */

                /*
                 * An if-else-if chain provides more reliable performance for
                 * the most common cases compared to a switch.
                 */
                action=(uint8_t)(MBCS_ENTRY_FINAL_ACTION(entry));
                if(action==MBCS_STATE_VALID_16) {
                    offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                    c=unicodeCodeUnits[offset];
                    if(c<0xfffe) {
                        /* output BMP code point */
                        *target++=c;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                        }
                    } else if(c==0xfffe) {
                        if(UCNV_TO_U_USE_FALLBACK(cnv) && (entry=(int32_t)_MBCSGetFallback(&cnv->sharedData->table->mbcs, offset))!=0xfffe) {
                            /* output fallback BMP code point */
                            *target++=(UChar)entry;
                            if(offsets!=NULL) {
                                *offsets++=sourceIndex;
                            }
                        } else {
                            /* callback(unassigned) */
                            goto unassigned;
                        }
                    } else {
                        /* callback(illegal) */
                        goto illegal;
                    }
                } else if(action==MBCS_STATE_VALID_DIRECT_16) {
                    /* output BMP code point */
                    *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                } else if(action==MBCS_STATE_VALID_16_PAIR) {
                    offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                    c=unicodeCodeUnits[offset++];
                    if(c<0xd800) {
                        /* output BMP code point below 0xd800 */
                        *target++=c;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                        }
                    } else if(UCNV_TO_U_USE_FALLBACK(cnv) ? c<=0xdfff : c<=0xdbff) {
                        /* output roundtrip or fallback surrogate pair */
                        *target++=(UChar)(c&0xdbff);
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
                            break;
                        }
                    } else if(UCNV_TO_U_USE_FALLBACK(cnv) ? (c&0xfffe)==0xe000 : c==0xe000) {
                        /* output roundtrip BMP code point above 0xd800 or fallback BMP code point */
                        *target++=unicodeCodeUnits[offset];
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                        }
                    } else if(c==0xffff) {
                        /* callback(illegal) */
                        goto illegal;
                    } else {
                        /* callback(unassigned) */
                        goto unassigned;
                    }
                } else if(action==MBCS_STATE_VALID_DIRECT_20) {
valid20:
                    entry=MBCS_ENTRY_FINAL_VALUE(entry);
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
                        break;
                    }
                } else if(action==MBCS_STATE_CHANGE_ONLY) {
                    /*
                     * This serves as a state change without any output.
                     * It is useful for reading simple stateful encodings,
                     * for example using just Shift-In/Shift-Out codes.
                     * The 21 unused bits may later be used for more sophisticated
                     * state transitions.
                     */
                } else if(action==MBCS_STATE_FALLBACK_DIRECT_16) {
                    if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                        /* callback(unassigned) */
                        goto unassigned;
                    }
                    /* output BMP code point */
                    *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                } else if(action==MBCS_STATE_FALLBACK_DIRECT_20) {
                    if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                        /* callback(unassigned) */
                        goto unassigned;
                    }
                    goto valid20;
                } else if(action==MBCS_STATE_UNASSIGNED) {
                    /* callback(unassigned) */
                    goto unassigned;
                } else if(action==MBCS_STATE_ILLEGAL) {
                    /* callback(illegal) */
                    goto illegal;
                } else {
                    /* reserved, must never occur */
                }

                /* normal end of action codes: prepare for a new character */
                offset=0;
                byteIndex=0;
                sourceIndex=nextSourceIndex;
                continue;

illegal:
                reason=UCNV_ILLEGAL;
                *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                goto callback;
unassigned:
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
callback:
                /* call the callback function with all the preparations and post-processing */
                /* update the arguments structure */
                pArgs->source=(const char *)source;
                pArgs->target=target;
                pArgs->offsets=offsets;

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
static void
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
    uint8_t action;
    UConverterCallbackReason reason;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;
    target=pArgs->target;
    targetLimit=pArgs->targetLimit;
    offsets=pArgs->offsets;

    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        stateTable=(const int32_t (*)[256])cnv->sharedData->table->mbcs.swapLFNLStateTable;
    } else {
        stateTable=cnv->sharedData->table->mbcs.stateTable;
    }

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
            ++nextSourceIndex;
            entry=stateTable[0][*source++];
            /* MBCS_ENTRY_IS_FINAL(entry) */

            /* test the most common case first */
            if(MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry)) {
                /* output BMP code point */
                *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
                if(offsets!=NULL) {
                    *offsets++=sourceIndex;
                }

                /* normal end of action codes: prepare for a new character */
                sourceIndex=nextSourceIndex;
                continue;
            }

            /*
             * An if-else-if chain provides more reliable performance for
             * the most common cases compared to a switch.
             */
            action=(uint8_t)(MBCS_ENTRY_FINAL_ACTION(entry));
            if(action==MBCS_STATE_VALID_DIRECT_20) {
valid20:
                entry=MBCS_ENTRY_FINAL_VALUE(entry);
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
                    break;
                }
            } else if(action==MBCS_STATE_FALLBACK_DIRECT_16) {
                if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                    /* callback(unassigned) */
                    goto unassigned;
                }
                /* output BMP code point */
                *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
                if(offsets!=NULL) {
                    *offsets++=sourceIndex;
                }
            } else if(action==MBCS_STATE_FALLBACK_DIRECT_20) {
                if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                    /* callback(unassigned) */
                    goto unassigned;
                }
                goto valid20;
            } else if(action==MBCS_STATE_UNASSIGNED) {
                /* callback(unassigned) */
                goto unassigned;
            } else if(action==MBCS_STATE_ILLEGAL) {
                /* callback(illegal) */
                reason=UCNV_ILLEGAL;
                *pErrorCode=U_ILLEGAL_CHAR_FOUND;
                goto callback;
            } else {
                /* reserved, must never occur */
            }

            /* normal end of action codes: prepare for a new character */
            sourceIndex=nextSourceIndex;
            continue;

unassigned:
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=(const char *)source;
            pArgs->target=target;
            pArgs->offsets=offsets;

            /* call the callback function */
            toUCallback(cnv, cnv->toUContext, pArgs, (const char *)(source-1), 1, reason, pErrorCode);

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
static void
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
    uint8_t action;
    UConverterCallbackReason reason;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;
    target=pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        stateTable=(const int32_t (*)[256])cnv->sharedData->table->mbcs.swapLFNLStateTable;
    } else {
        stateTable=cnv->sharedData->table->mbcs.stateTable;
    }

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

#if MBCS_UNROLL_SINGLE_TO_BMP
    /* unrolling makes it faster on Pentium III/Windows 2000 */
    /* unroll the loop with the most common case */
unrolled:
    if(targetCapacity>=16) {
        int32_t count, loops, oredEntries;

        loops=count=targetCapacity>>4;
        do {
            oredEntries=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            oredEntries|=entry=stateTable[0][*source++];
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);

            /* were all 16 entries really valid? */
            if(!MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(oredEntries)) {
                /* no, return to the first of these 16 */
                source-=16;
                target-=16;
                break;
            }
        } while(--count>0);
        count=loops-count;
        targetCapacity-=16*count;

        if(offsets!=NULL) {
            lastSource+=16*count;
            while(count>0) {
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                --count;
            }
        }
    }
#endif

    /* conversion loop */
    while(targetCapacity>0) {
        entry=stateTable[0][*source++];
        /* MBCS_ENTRY_IS_FINAL(entry) */

        /* test the most common case first */
        if(MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry)) {
            /* output BMP code point */
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            --targetCapacity;
            continue;
        }

        /*
         * An if-else-if chain provides more reliable performance for
         * the most common cases compared to a switch.
         */
        action=(uint8_t)(MBCS_ENTRY_FINAL_ACTION(entry));
        if(action==MBCS_STATE_FALLBACK_DIRECT_16) {
            if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
            }
            /* output BMP code point */
            *target++=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            --targetCapacity;
            continue;
        } else if(action==MBCS_STATE_UNASSIGNED) {
            /* callback(unassigned) */
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
        } else if(action==MBCS_STATE_ILLEGAL) {
            /* callback(illegal) */
            reason=UCNV_ILLEGAL;
            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
        } else {
            /* reserved, must never occur */
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

        /* call the callback function */
        toUCallback(cnv, cnv->toUContext, pArgs, (const char *)(source-1), 1, reason, pErrorCode);

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

#if MBCS_UNROLL_SINGLE_TO_BMP
        /* unrolling makes it faster on Pentium III/Windows 2000 */
        goto unrolled;
#endif
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

static UChar32
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
    uint8_t action;
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

    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        stateTable=(const int32_t (*)[256])cnv->sharedData->table->mbcs.swapLFNLStateTable;
    } else {
        stateTable=cnv->sharedData->table->mbcs.stateTable;
    }
    unicodeCodeUnits=cnv->sharedData->table->mbcs.unicodeCodeUnits;

    /* get the converter state from UConverter */
    offset=cnv->toUnicodeStatus;
    state=(uint8_t)(cnv->mode);
    byteIndex=cnv->toULength;
    bytes=cnv->toUBytes;

    /* conversion loop */
    while(source<sourceLimit) {
        entry=stateTable[state][bytes[byteIndex++]=*source++];
        if(MBCS_ENTRY_IS_TRANSITION(entry)) {
            state=(uint8_t)MBCS_ENTRY_TRANSITION_STATE(entry);
            offset+=MBCS_ENTRY_TRANSITION_OFFSET(entry);
        } else {
            /* set the next state early so that we can reuse the entry variable */
            state=(uint8_t)MBCS_ENTRY_FINAL_STATE(entry); /* typically 0 */

            /*
             * An if-else-if chain provides more reliable performance for
             * the most common cases compared to a switch.
             */
            action=(uint8_t)(MBCS_ENTRY_FINAL_ACTION(entry));
            if(action==MBCS_STATE_VALID_16) {
                offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                c=unicodeCodeUnits[offset];
                if(c<0xfffe) {
                    /* output BMP code point */
                    goto finish;
                } else if(c==0xfffe) {
                    if(UCNV_TO_U_USE_FALLBACK(cnv) && (c=_MBCSGetFallback(&cnv->sharedData->table->mbcs, offset))!=0xfffe) {
                        goto finish;
                    }
                    /* callback(unassigned) */
                    goto unassigned;
                } else {
                    /* callback(illegal) */
                    goto illegal;
                }
            } else if(action==MBCS_STATE_VALID_DIRECT_16) {
                /* output BMP code point */
                c=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
                goto finish;
            } else if(action==MBCS_STATE_VALID_16_PAIR) {
                offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                c=unicodeCodeUnits[offset++];
                if(c<0xd800) {
                    /* output BMP code point below 0xd800 */
                    goto finish;
                } else if(UCNV_TO_U_USE_FALLBACK(cnv) ? c<=0xdfff : c<=0xdbff) {
                    /* output roundtrip or fallback supplementary code point */
                    c=((c&0x3ff)<<10)+unicodeCodeUnits[offset]+(0x10000-0xdc00);
                    goto finish;
                } else if(UCNV_TO_U_USE_FALLBACK(cnv) ? (c&0xfffe)==0xe000 : c==0xe000) {
                    /* output roundtrip BMP code point above 0xd800 or fallback BMP code point */
                    c=unicodeCodeUnits[offset];
                    goto finish;
                } else if(c==0xffff) {
                    /* callback(illegal) */
                    goto illegal;
                } else {
                    /* callback(unassigned) */
                    goto unassigned;
                }
            } else if(action==MBCS_STATE_VALID_DIRECT_20) {
                /* output supplementary code point */
                c=(UChar32)(MBCS_ENTRY_FINAL_VALUE(entry)+0x10000);
                goto finish;
            } else if(action==MBCS_STATE_CHANGE_ONLY) {
                /*
                 * This serves as a state change without any output.
                 * It is useful for reading simple stateful encodings,
                 * for example using just Shift-In/Shift-Out codes.
                 * The 21 unused bits may later be used for more sophisticated
                 * state transitions.
                 */
            } else if(action==MBCS_STATE_FALLBACK_DIRECT_16) {
                if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                    /* callback(unassigned) */
                    goto unassigned;
                }
                /* output BMP code point */
                c=(UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
                goto finish;
            } else if(action==MBCS_STATE_FALLBACK_DIRECT_20) {
                if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                    /* callback(unassigned) */
                    goto unassigned;
                }
                /* output supplementary code point */
                c=(UChar32)(MBCS_ENTRY_FINAL_VALUE(entry)+0x10000);
                goto finish;
            } else if(action==MBCS_STATE_UNASSIGNED) {
                /* callback(unassigned) */
                goto unassigned;
            } else if(action==MBCS_STATE_ILLEGAL) {
                /* callback(illegal) */
                goto illegal;
            } else {
                /* reserved, must never occur */
            }

            /* normal end of action codes: prepare for a new character */
            offset=0;
            byteIndex=0;
            continue;

illegal:
            reason=UCNV_ILLEGAL;
            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
            goto callback;
unassigned:
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=(const char *)source;
            pArgs->target=buffer;
            pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;

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
static UChar32
_MBCSSingleGetNextUChar(UConverterToUnicodeArgs *pArgs,
                        UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];

    UConverter *cnv;
    const int32_t (*stateTable)[256];
    const uint8_t *source, *sourceLimit;

    int32_t entry;
    uint8_t action;
    UConverterCallbackReason reason;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;
    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        stateTable=(const int32_t (*)[256])cnv->sharedData->table->mbcs.swapLFNLStateTable;
    } else {
        stateTable=cnv->sharedData->table->mbcs.stateTable;
    }

    /* conversion loop */
    while(source<sourceLimit) {
        entry=stateTable[0][*source++];
        /* MBCS_ENTRY_IS_FINAL(entry) */

        /* write back the updated pointer early so that we can return directly */
        pArgs->source=(const char *)source;

        if(MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry)) {
            /* output BMP code point */
            return (UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
        }

        /*
         * An if-else-if chain provides more reliable performance for
         * the most common cases compared to a switch.
         */
        action=(uint8_t)(MBCS_ENTRY_FINAL_ACTION(entry));
        if(action==MBCS_STATE_VALID_DIRECT_20) {
            /* output supplementary code point */
            return (UChar32)(MBCS_ENTRY_FINAL_VALUE(entry)+0x10000);
        } else if(action==MBCS_STATE_FALLBACK_DIRECT_16) {
            if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
            } else {
                /* output BMP code point */
                return (UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            }
        } else if(action==MBCS_STATE_FALLBACK_DIRECT_20) {
            if(!UCNV_TO_U_USE_FALLBACK(cnv)) {
                /* callback(unassigned) */
                reason=UCNV_UNASSIGNED;
                *pErrorCode=U_INVALID_CHAR_FOUND;
            } else {
                /* output supplementary code point */
                return (UChar32)(MBCS_ENTRY_FINAL_VALUE(entry)+0x10000);
            }
        } else if(action==MBCS_STATE_UNASSIGNED) {
            /* callback(unassigned) */
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
        } else if(action==MBCS_STATE_ILLEGAL) {
            /* callback(illegal) */
            reason=UCNV_ILLEGAL;
            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
        } else {
            /* reserved, must never occur */
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return 0xffff;
        }

        /* call the callback function with all the preparations and post-processing */
        /* update the arguments structure */
        pArgs->target=buffer;
        pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;

        /* call the callback function */
        toUCallback(cnv, cnv->toUContext, pArgs, (const char *)(source-1), 1, reason, pErrorCode);

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
 * It does not handle the EBCDIC swaplfnl option (set in UConverter).
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
    uint8_t state, action;

    int32_t entry;

    /* set up the local pointers */
    source=(const uint8_t *)*pSource;
    if(source>=(const uint8_t *)sourceLimit) {
        /* no input at all: "illegal" */
        return 0xffff;
    }

#if 0
/*
 * Code disabled 2002dec09 (ICU 2.4) because it is not currently used in ICU. markus
 * TODO In future releases, verify that this function is never called for SBCS
 * conversions, i.e., that sharedData->table->mbcs.countStates==1 is still true.
 * Removal improves code coverage.
 */
    /* use optimized function if possible */
    if(sharedData->table->mbcs.countStates==1) {
        return _MBCSSingleSimpleGetNextUChar(sharedData, (uint8_t)(*(*pSource)++), useFallback);
    }
#endif

    stateTable=sharedData->table->mbcs.stateTable;
    unicodeCodeUnits=sharedData->table->mbcs.unicodeCodeUnits;

    /* converter state */
    offset=0;
    state=0;

    /* conversion loop */
    do {
        entry=stateTable[state][*source++];
        if(MBCS_ENTRY_IS_TRANSITION(entry)) {
            state=(uint8_t)MBCS_ENTRY_TRANSITION_STATE(entry);
            offset+=MBCS_ENTRY_TRANSITION_OFFSET(entry);
        } else {
            *pSource=(const char *)source;

            /*
             * An if-else-if chain provides more reliable performance for
             * the most common cases compared to a switch.
             */
            action=(uint8_t)(MBCS_ENTRY_FINAL_ACTION(entry));
            if(action==MBCS_STATE_VALID_16) {
                offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                entry=unicodeCodeUnits[offset];
                if(entry!=0xfffe) {
                    return (UChar32)entry;
                } else if(UCNV_TO_U_USE_FALLBACK(cnv)) {
                    return _MBCSGetFallback(&sharedData->table->mbcs, offset);
                } else {
                    return 0xfffe;
                }
            } else if(action==MBCS_STATE_VALID_DIRECT_16) {
                /* output BMP code point */
                return (UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            } else if(action==MBCS_STATE_VALID_16_PAIR) {
                offset+=MBCS_ENTRY_FINAL_VALUE_16(entry);
                entry=unicodeCodeUnits[offset++];
                if(entry<0xd800) {
                    /* output BMP code point below 0xd800 */
                    return (UChar32)entry;
                } else if(UCNV_TO_U_USE_FALLBACK(cnv) ? entry<=0xdfff : entry<=0xdbff) {
                    /* output roundtrip or fallback supplementary code point */
                    return (UChar32)(((entry&0x3ff)<<10)+unicodeCodeUnits[offset]+(0x10000-0xdc00));
                } else if(UCNV_TO_U_USE_FALLBACK(cnv) ? (entry&0xfffe)==0xe000 : entry==0xe000) {
                    /* output roundtrip BMP code point above 0xd800 or fallback BMP code point */
                    return unicodeCodeUnits[offset];
                } else if(entry==0xffff) {
                    return 0xffff;
                } else {
                    return 0xfffe;
                }
            } else if(action==MBCS_STATE_VALID_DIRECT_20) {
                /* output supplementary code point */
                return 0x10000+MBCS_ENTRY_FINAL_VALUE(entry);
            } else if(action==MBCS_STATE_FALLBACK_DIRECT_16) {
                if(!TO_U_USE_FALLBACK(useFallback)) {
                    return 0xfffe;
                }
                /* output BMP code point */
                return (UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
            } else if(action==MBCS_STATE_FALLBACK_DIRECT_20) {
                if(!TO_U_USE_FALLBACK(useFallback)) {
                    return 0xfffe;
                }
                /* output supplementary code point */
                return 0x10000+MBCS_ENTRY_FINAL_VALUE(entry);
            } else if(action==MBCS_STATE_CHANGE_ONLY) {
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
            } else if(action==MBCS_STATE_UNASSIGNED) {
                return 0xfffe;
            } else if(action==MBCS_STATE_ILLEGAL) {
                return 0xffff;
            } else {
                /* reserved, must never occur */
            }

            /* state change only - prepare for a new character */
            state=(uint8_t)MBCS_ENTRY_FINAL_STATE(entry); /* typically 0 */
            offset=0;
        }
    } while(source<(const uint8_t *)sourceLimit);

    *pSource=(const char *)source;
    return 0xffff;
}

#if 0
/*
 * Code disabled 2002dec09 (ICU 2.4) because it is not currently used in ICU. markus
 * Removal improves code coverage.
 */
/**
 * This version of _MBCSSimpleGetNextUChar() is optimized for single-byte, single-state codepages.
 * It does not handle the EBCDIC swaplfnl option (set in UConverter).
 */
U_CFUNC UChar32
_MBCSSingleSimpleGetNextUChar(UConverterSharedData *sharedData,
                              uint8_t b, UBool useFallback) {
    int32_t entry;
    uint8_t action;

    entry=sharedData->table->mbcs.stateTable[0][b];
    /* MBCS_ENTRY_IS_FINAL(entry) */

    if(MBCS_ENTRY_FINAL_IS_VALID_DIRECT_16(entry)) {
        /* output BMP code point */
        return (UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
    }

    /*
     * An if-else-if chain provides more reliable performance for
     * the most common cases compared to a switch.
     */
    action=(uint8_t)(MBCS_ENTRY_FINAL_ACTION(entry));
    if(action==MBCS_STATE_VALID_DIRECT_20) {
        /* output supplementary code point */
        return 0x10000+MBCS_ENTRY_FINAL_VALUE(entry);
    } else if(action==MBCS_STATE_FALLBACK_DIRECT_16) {
        if(!TO_U_USE_FALLBACK(useFallback)) {
            return 0xfffe;
        }
        /* output BMP code point */
        return (UChar)MBCS_ENTRY_FINAL_VALUE_16(entry);
    } else if(action==MBCS_STATE_FALLBACK_DIRECT_20) {
        if(!TO_U_USE_FALLBACK(useFallback)) {
            return 0xfffe;
        }
        /* output supplementary code point */
        return 0x10000+MBCS_ENTRY_FINAL_VALUE(entry);
    } else if(action==MBCS_STATE_UNASSIGNED) {
        return 0xfffe;
    } else if(action==MBCS_STATE_ILLEGAL) {
        return 0xffff;
    } else {
        /* reserved, must never occur */
        return 0xffff;
    }
}
#endif

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

    int32_t prevSourceIndex, sourceIndex, nextSourceIndex;

    UConverterCallbackReason reason;
    uint32_t stage2Entry;
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
    } else if(outputType==MBCS_OUTPUT_2) {
        _MBCSDoubleFromUnicodeWithOffsets(pArgs, pErrorCode);
        return;
    }

    /* set up the local pointers */
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    table=cnv->sharedData->table->mbcs.fromUnicodeTable;
    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        bytes=cnv->sharedData->table->mbcs.swapLFNLFromUnicodeBytes;
    } else {
        bytes=cnv->sharedData->table->mbcs.fromUnicodeBytes;
    }

    /* get the converter state from UConverter */
    c=cnv->fromUSurrogateLead;
    prevLength=cnv->fromUnicodeStatus;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    prevSourceIndex=-1;
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
             * a matched surrogate pair for a "supplementary code point".
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
                                goto unassigned;
                            }
                            /* convert this supplementary code point */
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
             * The basic lookup is a triple-stage compact array (trie) lookup.
             * For details see the beginning of this file.
             *
             * Single-byte codepages are handled with a different data structure
             * by _MBCSSingle... functions.
             *
             * The result consists of a 32-bit value from stage 2 and
             * a pointer to as many bytes as are stored per character.
             * The pointer points to the character's bytes in stage 3.
             * Bits 15..0 of the stage 2 entry contain the stage 3 index
             * for that pointer, while bits 31..16 are flags for which of
             * the 16 characters in the block are roundtrip-assigned.
             *
             * For 2-byte and 4-byte codepages, the bytes are stored as uint16_t
             * respectively as uint32_t, in the platform encoding.
             * For 3-byte codepages, the bytes are always stored in big-endian order.
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
            stage2Entry=MBCS_STAGE_2_FROM_U(table, c);

            /* get the bytes and the length for the output */
            switch(outputType) {
            case MBCS_OUTPUT_2:
                value=MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
                if(value<=0xff) {
                    length=1;
                } else {
                    length=2;
                }
                break;
            case MBCS_OUTPUT_2_SISO:
                /* 1/2-byte stateful with Shift-In/Shift-Out */
                /*
                 * Save the old state in the converter object
                 * right here, then change the local prevLength state variable if necessary.
                 * Then, if this character turns out to be unassigned or a fallback that
                 * is not taken, the callback code must not save the new state in the converter
                 * because the new state is for a character that is not output.
                 * However, the callback must still restore the state from the converter
                 * in case the callback function changed it for its output.
                 */
                cnv->fromUnicodeStatus=prevLength; /* save the old state */
                value=MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
                if(value<=0xff) {
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
                p=MBCS_POINTER_3_FROM_STAGE_2(bytes, stage2Entry, c);
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
                value=MBCS_VALUE_4_FROM_STAGE_2(bytes, stage2Entry, c);
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
                value=MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
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
                p=MBCS_POINTER_3_FROM_STAGE_2(bytes, stage2Entry, c);
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
            if(!(MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c)!=0 ||
                 (UCNV_FROM_U_USE_FALLBACK(cnv, c) && (value!=0 || c==0)))
            ) {
                /*
                 * We allow a 0 byte output if the Unicode code point is
                 * U+0000 and also if the "assigned" bit is set for this entry.
                 * There is no way with this data structure for fallback output
                 * for other than U+0000 to be a zero byte.
                 */
                /* callback(unassigned) */
                goto unassigned;
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
                uint8_t *charErrorBuffer;

                /*
                 * We actually do this backwards here:
                 * In order to save an intermediate variable, we output
                 * first to the overflow buffer what does not fit into the
                 * regular target.
                 */
                /* we know that 1<=targetCapacity<length<=4 */
                length-=targetCapacity;
                charErrorBuffer=(uint8_t *)cnv->charErrorBuffer;
                switch(length) {
                    /* each branch falls through to the next one */
                case 3:
                    *charErrorBuffer++=(uint8_t)(value>>16);
                case 2:
                    *charErrorBuffer++=(uint8_t)(value>>8);
                case 1:
                    *charErrorBuffer=(uint8_t)value;
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
            if(offsets!=NULL) {
                prevSourceIndex=sourceIndex;
                sourceIndex=nextSourceIndex;
            }
            continue;

            /*
             * This is the same ugly trick as in ToUnicode(), for the
             * same reasons...
             */
unassigned:
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=source;
            pArgs->target=(char *)target;
            pArgs->offsets=offsets;

            /* set the converter state in UConverter to deal with the next character */
            cnv->fromUSurrogateLead=0;
            /*
             * Do not save the prevLength SISO state because prevLength is set for
             * the character that is now not output because it is unassigned or it is
             * a fallback that is not taken.
             * The above branch for MBCS_OUTPUT_2_SISO has saved the previous state already.
             * See comments there.
             */
            prevSourceIndex=sourceIndex;

            /* call the callback function */
            fromUCallback(cnv, cnv->fromUContext, pArgs, c, reason, pErrorCode);

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

    if(pArgs->flush && source>=sourceLimit && U_SUCCESS(*pErrorCode)) {
        /* end of input stream */
        if(c!=0) {
            /* a Unicode code point remains incomplete (only a first surrogate) */
            *pErrorCode=U_TRUNCATED_CHAR_FOUND;
            /* the following may change with Jitterbug 2449: would prepare for callback instead of resetting */
            c=0;
            prevLength=1;
        } else if(outputType==MBCS_OUTPUT_2_SISO && prevLength==2) {
            /* EBCDIC_STATEFUL ending with DBCS: emit an SI to return the output stream to SBCS */
            if(targetCapacity>0) {
                *target++=(uint8_t)UCNV_SI;
                if(offsets!=NULL) {
                    /* set the last source character's index (sourceIndex points at sourceLimit now) */
                    *offsets++=prevSourceIndex;
                }
            } else {
                /* target is full */
                cnv->charErrorBuffer[0]=(char)UCNV_SI;
                cnv->charErrorBufferLength=1;
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            }
            prevLength=1; /* we switched into SBCS */
        }

        /* reset the state for the next conversion */
        if(U_SUCCESS(*pErrorCode)) {
            c=0;
            prevLength=1;
        }
    }

    /* set the converter state back into UConverter */
    cnv->fromUSurrogateLead=(UChar)c;
    cnv->fromUnicodeStatus=prevLength;

    /* write back the updated pointers */
    pArgs->source=source;
    pArgs->target=(char *)target;
    pArgs->offsets=offsets;
}

/* This version of _MBCSFromUnicodeWithOffsets() is optimized for double-byte codepages. */
static void
_MBCSDoubleFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
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
    uint32_t stage2Entry;
    uint32_t value;
    int32_t length, prevLength;
    uint8_t unicodeMask;

    /* use optimized function if possible */
    cnv=pArgs->converter;
    unicodeMask=cnv->sharedData->table->mbcs.unicodeMask;

    /* set up the local pointers */
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    table=cnv->sharedData->table->mbcs.fromUnicodeTable;
    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        bytes=cnv->sharedData->table->mbcs.swapLFNLFromUnicodeBytes;
    } else {
        bytes=cnv->sharedData->table->mbcs.fromUnicodeBytes;
    }

    /* get the converter state from UConverter */
    c=cnv->fromUSurrogateLead;
    prevLength=cnv->fromUnicodeStatus;

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
             * a matched surrogate pair for a "supplementary code point".
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
                                goto unassigned;
                            }
                            /* convert this supplementary code point */
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
            stage2Entry=MBCS_STAGE_2_FROM_U(table, c);

            /* get the bytes and the length for the output */
            /* MBCS_OUTPUT_2 */
            value=MBCS_VALUE_2_FROM_STAGE_2(bytes, stage2Entry, c);
            if(value<=0xff) {
                length=1;
            } else {
                length=2;
            }

            /* is this code point assigned, or do we use fallbacks? */
            if(!(MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c) ||
                 (UCNV_FROM_U_USE_FALLBACK(cnv, c) && (value!=0 || c==0)))
            ) {
                /*
                 * We allow a 0 byte output if the Unicode code point is
                 * U+0000 and also if the "assigned" bit is set for this entry.
                 * There is no way with this data structure for fallback output
                 * for other than U+0000 to be a zero byte.
                 */
                /* callback(unassigned) */
                goto unassigned;
            }

            /* write the output character bytes from value and length */
            /* from the first if in the loop we know that targetCapacity>0 */
            if(length==1) {
                /* this is easy because we know that there is enough space */
                *target++=(uint8_t)value;
                if(offsets!=NULL) {
                    *offsets++=sourceIndex;
                }
                --targetCapacity;
            } else /* length==2 */ {
                *target++=(uint8_t)(value>>8);
                if(2<=targetCapacity) {
                    *target++=(uint8_t)value;
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                        *offsets++=sourceIndex;
                    }
                    targetCapacity-=2;
                } else {
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                    }
                    cnv->charErrorBuffer[0]=(char)value;
                    cnv->charErrorBufferLength=1;

                    /* target overflow */
                    targetCapacity=0;
                    *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                    c=0;
                    break;
                }
            }

            /* normal end of conversion: prepare for a new character */
            c=0;
            sourceIndex=nextSourceIndex;
            continue;

            /*
             * This is the same ugly trick as in ToUnicode(), for the
             * same reasons...
             */
unassigned:
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=source;
            pArgs->target=(char *)target;
            pArgs->offsets=offsets;

            /* set the converter state in UConverter to deal with the next character */
            cnv->fromUSurrogateLead=0;
            cnv->fromUnicodeStatus=prevLength;

            /* call the callback function */
            fromUCallback(cnv, cnv->fromUContext, pArgs, c, reason, pErrorCode);

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

/* This version of _MBCSFromUnicodeWithOffsets() is optimized for single-byte codepages. */
static void
_MBCSSingleFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                                  UErrorCode *pErrorCode) {
    UConverter *cnv;
    const UChar *source, *sourceLimit;
    uint8_t *target;
    int32_t targetCapacity;
    int32_t *offsets;

    const uint16_t *table;
    const uint16_t *results;

    UChar32 c;

    int32_t sourceIndex, nextSourceIndex;

    UConverterCallbackReason reason;
    uint16_t value, minValue;
    UBool hasSupplementary;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    table=cnv->sharedData->table->mbcs.fromUnicodeTable;
    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        results=(uint16_t *)cnv->sharedData->table->mbcs.swapLFNLFromUnicodeBytes;
    } else {
        results=(uint16_t *)cnv->sharedData->table->mbcs.fromUnicodeBytes;
    }

    if(cnv->useFallback) {
        /* use all roundtrip and fallback results */
        minValue=0x800;
    } else {
        /* use only roundtrips and fallbacks from private-use characters */
        minValue=0xc00;
    }
    hasSupplementary=(UBool)(cnv->sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY);

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
             * a matched surrogate pair for a "supplementary code point".
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
                                goto unassigned;
                            }
                            /* convert this supplementary code point */
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
            value=MBCS_SINGLE_RESULT_FROM_U(table, results, c);

            /* is this code point assigned, or do we use fallbacks? */
            if(value>=minValue) {
                /* assigned, write the output character bytes from value and length */
                /* length==1 */
                /* this is easy because we know that there is enough space */
                *target++=(uint8_t)value;
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
            }
unassigned:
            reason=UCNV_UNASSIGNED;
            *pErrorCode=U_INVALID_CHAR_FOUND;
callback:
            /* call the callback function with all the preparations and post-processing */
            /* update the arguments structure */
            pArgs->source=source;
            pArgs->target=(char *)target;
            pArgs->offsets=offsets;

            /* set the converter state in UConverter to deal with the next character */
            cnv->fromUSurrogateLead=0;

            /* call the callback function */
            fromUCallback(cnv, cnv->fromUContext, pArgs, c, reason, pErrorCode);

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
static void
_MBCSSingleFromBMPWithOffsets(UConverterFromUnicodeArgs *pArgs,
                              UErrorCode *pErrorCode) {
    UConverter *cnv;
    const UChar *source, *sourceLimit, *lastSource;
    uint8_t *target;
    int32_t targetCapacity, length;
    int32_t *offsets;

    const uint16_t *table;
    const uint16_t *results;

    UChar32 c;

    int32_t sourceIndex;

    UConverterCallbackReason reason;
    uint16_t value, minValue;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    table=cnv->sharedData->table->mbcs.fromUnicodeTable;
    if((cnv->options&UCNV_OPTION_SWAP_LFNL)!=0) {
        results=(uint16_t *)cnv->sharedData->table->mbcs.swapLFNLFromUnicodeBytes;
    } else {
        results=(uint16_t *)cnv->sharedData->table->mbcs.fromUnicodeBytes;
    }

    if(cnv->useFallback) {
        /* use all roundtrip and fallback results */
        minValue=0x800;
    } else {
        /* use only roundtrips and fallbacks from private-use characters */
        minValue=0xc00;
    }

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

#if MBCS_UNROLL_SINGLE_FROM_BMP
    /* unrolling makes it slower on Pentium III/Windows 2000?! */
    /* unroll the loop with the most common case */
unrolled:
    if(targetCapacity>=4) {
        int32_t count, loops;
        uint16_t andedValues;

        loops=count=targetCapacity>>2;
        do {
            c=*source++;
            andedValues=value=MBCS_SINGLE_RESULT_FROM_U(table, results, c);
            *target++=(uint8_t)value;
            c=*source++;
            andedValues&=value=MBCS_SINGLE_RESULT_FROM_U(table, results, c);
            *target++=(uint8_t)value;
            c=*source++;
            andedValues&=value=MBCS_SINGLE_RESULT_FROM_U(table, results, c);
            *target++=(uint8_t)value;
            c=*source++;
            andedValues&=value=MBCS_SINGLE_RESULT_FROM_U(table, results, c);
            *target++=(uint8_t)value;

            /* were all 4 entries really valid? */
            if(andedValues<minValue) {
                /* no, return to the first of these 4 */
                source-=4;
                target-=4;
                break;
            }
        } while(--count>0);
        count=loops-count;
        targetCapacity-=4*count;

        if(offsets!=NULL) {
            lastSource+=4*count;
            while(count>0) {
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                *offsets++=sourceIndex++;
                --count;
            }
        }

        c=0;
    }
#endif

    while(targetCapacity>0) {
        /*
         * Get a correct Unicode code point:
         * a single UChar for a BMP code point or
         * a matched surrogate pair for a "supplementary code point".
         */
        c=*source++;
        /*
         * Do not immediately check for single surrogates:
         * Assume that they are unassigned and check for them in that case.
         * This speeds up the conversion of assigned characters.
         */
        /* convert the Unicode code point in c into codepage bytes */
        value=MBCS_SINGLE_RESULT_FROM_U(table, results, c);

        /* is this code point assigned, or do we use fallbacks? */
        if(value>=minValue) {
            /* assigned, write the output character bytes from value and length */
            /* length==1 */
            /* this is easy because we know that there is enough space */
            *target++=(uint8_t)value;
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

        /* call the callback function */
        fromUCallback(cnv, cnv->fromUContext, pArgs, c, reason, pErrorCode);

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

#if MBCS_UNROLL_SINGLE_FROM_BMP
        /* unrolling makes it slower on Pentium III/Windows 2000?! */
        goto unrolled;
#endif
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
 * It does not handle the EBCDIC swaplfnl option (set in UConverter).
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
                 UBool useFallback) {
    const uint16_t *table=sharedData->table->mbcs.fromUnicodeTable;
    const uint8_t *p;
    uint32_t stage2Entry;
    uint32_t value;
    int32_t length;

    /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
    if(c>=0x10000 && !(sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
        return 0;
    }

    /* convert the Unicode code point in c into codepage bytes (same as in _MBCSFromUnicodeWithOffsets) */
    if(sharedData->table->mbcs.outputType==MBCS_OUTPUT_1) {
        value=MBCS_SINGLE_RESULT_FROM_U(table, (uint16_t *)sharedData->table->mbcs.fromUnicodeBytes, c);
        /* is this code point assigned, or do we use fallbacks? */
        if(useFallback ? value>=0x800 : value>=0xc00) {
            *pValue=value&0xff;
            return 1;
        } else {
            return 0;
        }
    }

    stage2Entry=MBCS_STAGE_2_FROM_U(table, c);

    /* get the bytes and the length for the output */
    switch(sharedData->table->mbcs.outputType) {
    case MBCS_OUTPUT_2:
        value=MBCS_VALUE_2_FROM_STAGE_2(sharedData->table->mbcs.fromUnicodeBytes, stage2Entry, c);
        if(value<=0xff) {
            length=1;
        } else {
            length=2;
        }
        break;
    case MBCS_OUTPUT_3:
        p=MBCS_POINTER_3_FROM_STAGE_2(sharedData->table->mbcs.fromUnicodeBytes, stage2Entry, c);
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
        value=MBCS_VALUE_4_FROM_STAGE_2(sharedData->table->mbcs.fromUnicodeBytes, stage2Entry, c);
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
        value=MBCS_VALUE_2_FROM_STAGE_2(sharedData->table->mbcs.fromUnicodeBytes, stage2Entry, c);
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
        p=MBCS_POINTER_3_FROM_STAGE_2(sharedData->table->mbcs.fromUnicodeBytes, stage2Entry, c);
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
    if( MBCS_FROM_U_IS_ROUNDTRIP(stage2Entry, c) ||
        (FROM_U_USE_FALLBACK(useFallback, c) && (value!=0 || c==0))
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


#if 0
/** 
 * ################################################################
 * # 
 * # This function has been moved to ucnv2022.c for inlining.
 * # This implementation is here only for documentation purposes
 * #
 * ################################################################
 */

/**
 * This version of _MBCSFromUChar32() is optimized for single-byte codepages.
 * It does not handle the EBCDIC swaplfnl option (set in UConverter).
 *
 * It returns the codepage byte for the code point, or -1 if it is unassigned.
 */
U_CFUNC int32_t
_MBCSSingleFromUChar32(UConverterSharedData *sharedData,
                       UChar32 c,
                       UBool useFallback) {
    const uint16_t *table;
    int32_t value;

    /* BMP-only codepages are stored without stage 1 entries for supplementary code points */
    if(c>=0x10000 && !(sharedData->table->mbcs.unicodeMask&UCNV_HAS_SUPPLEMENTARY)) {
        return -1;
    }

    /* convert the Unicode code point in c into codepage bytes (same as in _MBCSFromUnicodeWithOffsets) */
    table=sharedData->table->mbcs.fromUnicodeTable;

    /* get the byte for the output */
    value=MBCS_SINGLE_RESULT_FROM_U(table, (uint16_t *)sharedData->table->mbcs.fromUnicodeBytes, c);
    /* is this code point assigned, or do we use fallbacks? */
    if(useFallback ? value>=0x800 : value>=0xc00) {
        return value&0xff;
    } else {
        return -1;
    }
}
#endif

/* miscellaneous ------------------------------------------------------------ */

static void
_MBCSGetStarters(const UConverter* cnv,
                 UBool starters[256],
                 UErrorCode *pErrorCode) {
    const int32_t *state0=cnv->sharedData->table->mbcs.stateTable[0];
    int i;

    for(i=0; i<256; ++i) {
        /* all bytes that cause a state transition from state 0 are lead bytes */
        starters[i]= (UBool)MBCS_ENTRY_IS_TRANSITION(state0[i]);
    }
}

/*
 * This is an internal function that allows other converter implementations
 * to check whether a byte is a lead byte.
 */
U_CFUNC UBool
_MBCSIsLeadByte(UConverterSharedData *sharedData, char byte) {
    return (UBool)MBCS_ENTRY_IS_TRANSITION(sharedData->table->mbcs.stateTable[0][(uint8_t)byte]);
}

static void
_MBCSWriteSub(UConverterFromUnicodeArgs *pArgs,
              int32_t offsetIndex,
              UErrorCode *pErrorCode) {
    UConverter *cnv=pArgs->converter;
    char *p, *subchar;
    char buffer[4];
    int32_t length;

    /* first, select between subChar and subChar1 */
    if(cnv->subChar1!=0 && cnv->invalidUCharBuffer[0]<=0xff) {
        /* select subChar1 if it is set (not 0) and the unmappable Unicode code point is up to U+00ff (IBM MBCS behavior) */
        subchar=(char *)&cnv->subChar1;
        length=1;
    } else {
        /* select subChar in all other cases */
        subchar=(char *)cnv->subChar;
        length=cnv->subCharLen;
    }

    switch(cnv->sharedData->table->mbcs.outputType) {
    case MBCS_OUTPUT_2_SISO:
        p=buffer;

        /* fromUnicodeStatus contains prevLength */
        switch(length) {
        case 1:
            if(cnv->fromUnicodeStatus==2) {
                /* DBCS mode and SBCS sub char: change to SBCS */
                cnv->fromUnicodeStatus=1;
                *p++=UCNV_SI;
            }
            *p++=subchar[0];
            break;
        case 2:
            if(cnv->fromUnicodeStatus==1) {
                /* SBCS mode and DBCS sub char: change to DBCS */
                cnv->fromUnicodeStatus=2;
                *p++=UCNV_SO;
            }
            *p++=subchar[0];
            *p++=subchar[1];
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
                               subchar, length,
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
    _MBCSUnload,

    _MBCSOpen,
    NULL,
    _MBCSReset,

    _MBCSToUnicodeWithOffsets,
    _MBCSToUnicodeWithOffsets,
    _MBCSFromUnicodeWithOffsets,
    _MBCSFromUnicodeWithOffsets,
    _MBCSGetNextUChar,

    _MBCSGetStarters,
    _MBCSGetName,
    _MBCSWriteSub,
    NULL,
    _MBCSGetUnicodeSet
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
              const void *context, UConverterFromUnicodeArgs *pArgs,
              UChar32 codePoint,
              UConverterCallbackReason reason, UErrorCode *pErrorCode) {
    int32_t i;

    if((cnv->options&_MBCS_OPTION_GB18030)!=0 && reason==UCNV_UNASSIGNED) {
        const uint32_t *range;

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
                bytes[3]=(char)(0x30+linear%10); linear/=10;
                bytes[2]=(char)(0x81+linear%126); linear/=126;
                bytes[1]=(char)(0x30+linear%10); linear/=10;
                bytes[0]=(char)(0x81+linear);

                /* output this sequence */
                ucnv_cbFromUWriteBytes(pArgs, bytes, 4, 0, pErrorCode);
                return;
            }
        }
    }

    /* write the code point as code units */
    i=0;
    UTF_APPEND_CHAR_UNSAFE(cnv->invalidUCharBuffer, i, codePoint);
    cnv->invalidUCharLength=(int8_t)i;

    /* call the normal callback function */
    cnv->fromUCharErrorBehaviour(context, pArgs, cnv->invalidUCharBuffer, i, codePoint, reason, pErrorCode);
}

static void
toUCallback(UConverter *cnv,
            const void *context, UConverterToUnicodeArgs *pArgs,
            const char *codeUnits, int32_t length,
            UConverterCallbackReason reason, UErrorCode *pErrorCode) {
    int32_t i;

    if((cnv->options&_MBCS_OPTION_GB18030)!=0 && reason==UCNV_UNASSIGNED && length==4) {
        const uint32_t *range;
        uint32_t linear;

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

    /* copy the current bytes to invalidCharBuffer */
    for(i=0; i<length; ++i) {
        cnv->invalidCharBuffer[i]=codeUnits[i];
    }
    cnv->invalidCharLength=(int8_t)length;

    /* call the normal callback function */
    cnv->fromCharErrorBehaviour(context, pArgs, codeUnits, length, reason, pErrorCode);
}

#endif /* #if !UCONFIG_NO_LEGACY_CONVERSION */
