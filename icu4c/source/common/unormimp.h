/*
*******************************************************************************
*
*   Copyright (C) 2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  unormimp.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2001may25
*   created by: Markus W. Scherer
*/

#ifndef __UNORMIMP_H__
#define __UNORMIMP_H__

#include "unicode/utypes.h"
#include "unicode/unorm.h"
#ifdef XP_CPLUSPLUS
#   include "unicode/chariter.h"
#endif
#include "ustr_imp.h"

/*
 * This new implementation of the normalization code loads its data from
 * unorm.dat, which is generated with the gennorm tool.
 * The format of that file is described at the end of this file.
 */

/* trie constants */
enum {
    /**
     * Normalization trie table shift value.
     * This value must be <=10:
     * above 10, a lead surrogate's block is smaller than a stage 2 block
     */
    _NORM_TRIE_SHIFT=5,

    _NORM_STAGE_2_BLOCK_COUNT=1<<_NORM_TRIE_SHIFT,
    _NORM_STAGE_2_MASK=_NORM_STAGE_2_BLOCK_COUNT-1,

    _NORM_STAGE_1_BMP_COUNT=(1<<(16-_NORM_TRIE_SHIFT)),

    _NORM_SURROGATE_BLOCK_BITS=10-_NORM_TRIE_SHIFT,
    _NORM_SURROGATE_BLOCK_COUNT=(1<<_NORM_SURROGATE_BLOCK_BITS)
};

/* this may be >0xffff and may not work as an enum */
#define _NORM_STAGE_1_MAX_COUNT (0x110000>>_NORM_TRIE_SHIFT)

/* norm32 value constants */
enum {
    /* quick check flags 0..3 set mean "no" for their forms */
    _NORM_QC_NFC=0x11,          /* no|maybe */
    _NORM_QC_NFKC=0x22,         /* no|maybe */
    _NORM_QC_NFD=4,             /* no */
    _NORM_QC_NFKD=8,            /* no */

    _NORM_QC_ANY_NO=0xf,

    /* quick check flags 4..5 mean "maybe" for their forms; test flags>=_NORM_QC_MAYBE */
    _NORM_QC_MAYBE=0x10,
    _NORM_QC_ANY_MAYBE=0x30,

    _NORM_QC_MASK=0x3f,

    _NORM_COMBINES_FWD=0x40,
    _NORM_COMBINES_BACK=0x80,
    _NORM_COMBINES_ANY=0xc0,

    _NORM_CC_SHIFT=8,           /* UnicodeData.txt combining class in bits 15..8 */
    _NORM_CC_MASK=0xff00,

    _NORM_EXTRA_SHIFT=16,               /* 16 bits for the index to UChars and other extra data */
    _NORM_EXTRA_INDEX_TOP=0xfc00,       /* start of surrogate specials after shift */

    _NORM_EXTRA_SURROGATE_MASK=0x3ff,
    _NORM_EXTRA_SURROGATE_TOP=0x3f0,    /* hangul etc. */

    _NORM_EXTRA_HANGUL=_NORM_EXTRA_SURROGATE_TOP,
    _NORM_EXTRA_JAMO_L,                 /* ### not used */
    _NORM_EXTRA_JAMO_V,
    _NORM_EXTRA_JAMO_T
};

/* norm32 value constants using >16 bits */
#define _NORM_MIN_SPECIAL       0xfc000000
#define _NORM_SURROGATES_TOP    0xfff00000
#define _NORM_MIN_HANGUL        0xfff00000
#define _NORM_MIN_JAMO_V        0xfff20000
#define _NORM_JAMO_V_TOP        0xfff30000


/* indexes[] value names */
enum {
    _NORM_INDEX_COUNT,
    _NORM_INDEX_TRIE_SHIFT,
    _NORM_INDEX_TRIE_INDEX_COUNT,
    _NORM_INDEX_TRIE_DATA_COUNT,
    _NORM_INDEX_UCHAR_COUNT,

    _NORM_INDEX_COMBINE_DATA_COUNT,
    _NORM_INDEX_COMBINE_FWD_COUNT,
    _NORM_INDEX_COMBINE_BOTH_COUNT,
    _NORM_INDEX_COMBINE_BACK_COUNT,

    _NORM_INDEX_MIN_NFC_NO_MAYBE,
    _NORM_INDEX_MIN_NFKC_NO_MAYBE,
    _NORM_INDEX_MIN_NFD_NO_MAYBE,
    _NORM_INDEX_MIN_NFKD_NO_MAYBE,

    _NORM_INDEX_FCD_TRIE_INDEX_COUNT,
    _NORM_INDEX_FCD_TRIE_DATA_COUNT,

    _NORM_INDEX_TOP=16
};

enum {
    /* FCD check: everything below this code point is known to have a 0 lead combining class */
    _NORM_MIN_WITH_LEAD_CC=0x300
};

enum {
    /**
     * Bit 7 of the length byte for a decomposition string in extra data is
     * a flag indicating whether the decomposition string is
     * preceded by a 16-bit word with the leading and trailing cc
     * of the decomposition (like for A-umlaut);
     * if not, then both cc's are zero (like for compatibility ideographs).
     */
    _NORM_DECOMP_FLAG_LENGTH_HAS_CC=0x80,
    /**
     * Bits 6..0 of the length byte contain the actual length.
     */
    _NORM_DECOMP_LENGTH_MASK=0x7f
};

/**
 * Is the normalizer data loaded?
 * This is used internally before other internal normalizer functions
 * are called.
 * It saves this check in each of many normalization calls that
 * are made for, e.g., collation.
 *
 * @param pErrorCode as usual
 * @return boolean value for whether the normalization data is loaded
 *
 * @internal
 */
U_CAPI UBool U_EXPORT2
unorm_haveData(UErrorCode *pErrorCode);

/**
 * Internal API for normalizing.
 * Does not check for bad input.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
unorm_internalNormalize(UChar *dest, int32_t destCapacity,
                        const UChar *src, int32_t srcLength,
                        UNormalizationMode mode, UBool ignoreHangul,
                        UErrorCode *pErrorCode);

/**
 * internal API, used by normlzr.cpp
 * @internal
 */
U_CAPI int32_t U_EXPORT2
unorm_decompose(UChar *dest, int32_t destCapacity,
                const UChar *src, int32_t srcLength,
                UBool compat, UBool ignoreHangul,
                UErrorCode *pErrorCode);

/**
 * internal API, used by normlzr.cpp
 * @internal
 */
U_CAPI int32_t U_EXPORT2
unorm_compose(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              UBool compat, UBool ignoreHangul,
              UErrorCode *pErrorCode);

/**
 * Internal API, used by collation code.
 * Get access to the internal FCD trie table to be able to perform
 * incremental, per-code unit, FCD checks in collation.
 * One pointer is sufficient because the trie index values are offset
 * by the index size, so that the same pointer is used to access the trie data.
 * @internal
 */
U_CAPI const uint16_t * U_EXPORT2
unorm_getFCDTrie(UErrorCode *pErrorCode);

#ifdef XP_CPLUSPLUS

U_NAMESPACE_BEGIN
/**
 * Internal API, used by collation code.
 * Get the FCD value for a code unit, with
 * bits 15..8   lead combining class
 * bits  7..0   trail combining class
 *
 * If c is a lead surrogate and the value is not 0,
 * then instead of combining classes the value
 * is used in unorm_getFCD16FromSurrogatePair() to get the real value
 * of the supplementary code point.
 *
 * @internal
 */
inline uint16_t
unorm_getFCD16(const uint16_t *fcdTrieIndex, UChar c) {
    return
        fcdTrieIndex[
            fcdTrieIndex[
                c>>_NORM_TRIE_SHIFT
            ]+
            (c&_NORM_STAGE_2_MASK)
        ];
}

/**
 * Internal API, used by collation code.
 * Get the FCD value for a supplementary code point, with
 * bits 15..8   lead combining class
 * bits  7..0   trail combining class
 *
 * @param fcd16  The FCD value for the lead surrogate, not 0.
 * @param c2     The trail surrogate code unit.
 *
 * @internal
 */
inline uint16_t
unorm_getFCD16FromSurrogatePair(const uint16_t *fcdTrieIndex, uint16_t fcd16, UChar c2) {
    /* the surrogate index in fcd16 is an absolute offset over the start of stage 1 */
    uint32_t c=
        ((uint32_t)fcd16<<10)|
        (c2&0x3ff);
    return
        fcdTrieIndex[
            fcdTrieIndex[
                c>>_NORM_TRIE_SHIFT
            ]+
            (c&_NORM_STAGE_2_MASK)
        ];
}

U_NAMESPACE_END

#endif

U_CDECL_BEGIN

struct UCharIterator;
typedef struct UCharIterator UCharIterator;

enum UCharIteratorOrigin {
    UITERATOR_START, UITERATOR_CURRENT, UITERATOR_END
};

typedef enum UCharIteratorOrigin UCharIteratorOrigin;

/**
 * C API for code unit iteration.
 * This can be used as a C wrapper around
 * CharacterIterator, Replaceable, or implemented using simple strings, etc.
 *
 * @internal for normalization
 */
struct UCharIterator {
    /**
     * (protected) Pointer to string or wrapped object or similar.
     * Not used by caller.
     */
    const void *context;

    /**
     * (protected) Length of string or similar.
     * Not used by caller.
     */
    int32_t length;

    /**
     * (protected) Start index or similar.
     * Not used by caller.
     */
    int32_t start;

    /**
     * (protected) Current index or similar.
     * Not used by caller.
     */
    int32_t index;

    /**
     * (protected) Limit index or similar.
     * Not used by caller.
     */
    int32_t limit;

    /**
     * (public) Moves the current position relative to the start or end of the
     * iteration range, or relative to the current position itself.
     * The movement is expressed in numbers of code units forward
     * or backward by specifying a positive or negative delta.
     *
     * @param delta can be positive, zero, or negative
     * @param origin move relative to the start, end, or current index
     * @return the new index
     */
    int32_t U_CALLCONV
    (*move)(UCharIterator *iter, int32_t delta, UCharIteratorOrigin origin);

    /**
     * (public) Check if current() and next() can still
     * return another code unit.
     */
    UBool U_CALLCONV
    (*hasNext)(UCharIterator *iter);

    /**
     * (public) Check if previous() can still return another code unit.
     */
    UBool U_CALLCONV
    (*hasPrevious)(UCharIterator *iter);

    /**
     * (public) Return the code unit at the current position,
     * or 0xffff if there is none (index is at the end).
     */
    UChar U_CALLCONV
    (*current)(UCharIterator *iter);

    /**
     * (public) Return the code unit at the current index and increment
     * the index (post-increment, like s[i++]),
     * or return 0xffff if there is none (index is at the end).
     */
    UChar U_CALLCONV
    (*next)(UCharIterator *iter);

    /**
     * (public) Decrement the index and return the code unit from there
     * (pre-decrement, like s[--i]),
     * or return 0xffff if there is none (index is at the start).
     */
    UChar U_CALLCONV
    (*previous)(UCharIterator *iter);
};

/**
 * Internal API for iterative normalizing - see Normalizer.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
unorm_nextNormalize(UChar *dest, int32_t destCapacity,
                    UCharIterator *src,
                    UNormalizationMode mode, UBool ignoreHangul,
                    UErrorCode *pErrorCode);

/**
 * Internal API for iterative normalizing - see Normalizer.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
unorm_previousNormalize(UChar *dest, int32_t destCapacity,
                        UCharIterator *src,
                        UNormalizationMode mode, UBool ignoreHangul,
                        UErrorCode *pErrorCode);

U_CDECL_END

/**
 * Description of the format of unorm.dat.
 *
 * For more details of how to use the data structures see the code
 * in unorm.cpp (runtime normalization code) and
 * in gennorm.c and gennorm/store.c (build-time data generation).
 *
 *
 * - Overall partition
 *
 * unorm.dat customarily begins with a UDataInfo structure, see udata.h and .c.
 * After that there are the following arrays:
 *
 * uint16_t indexes[_NORM_INDEX_TOP];           -- _NORM_INDEX_TOP=indexes[0]=indexes[_NORM_INDEX_COUNT]
 *
 * uint16_t stage1[stage1Top];                  -- stage1Top=indexes[_NORM_INDEX_TRIE_INDEX_COUNT]
 * uint32_t norm32Table[norm32TableTop];        -- norm32TableTop=indexes[_NORM_INDEX_TRIE_DATA_COUNT]
 *
 * uint16_t extraData[extraDataTop];            -- extraDataTop=indexes[_NORM_INDEX_UCHAR_COUNT]
 * uint16_t combiningTable[combiningTableTop];  -- combiningTableTop=indexes[_NORM_INDEX_COMBINE_DATA_COUNT]
 *
 * uint16_t fcdStage1[fcdStage1Top];            -- fcdStage1Top=indexes[_NORM_INDEX_FCD_TRIE_INDEX_COUNT]
 * uint16_t fcdTable[fcdTableTop];              -- fcdTableTop=indexes[_NORM_INDEX_FCD_TRIE_DATA_COUNT]
 *
 *
 * The indexes array contains lengths of the following arrays (and its own length)
 * as well as the following values:
 *  indexes[_NORM_INDEX_COUNT]=_NORM_INDEX_TOP
 *      -- length of indexes[]
 *  indexes[_NORM_INDEX_TRIE_SHIFT]=_NORM_TRIE_SHIFT
 *      -- for trie indexes: shift UChars by this much
 *  indexes[_NORM_INDEX_COMBINE_FWD_COUNT]=combineFwdTop
 *      -- one more than the highest combining index computed for forward-only-combining characters
 *  indexes[_NORM_INDEX_COMBINE_BOTH_COUNT]=combineBothTop-combineFwdTop
 *      -- number of combining indexes computed for both-ways-combining characters
 *  indexes[_NORM_INDEX_COMBINE_BACK_COUNT]=combineBackTop-combineBothTop
 *      -- number of combining indexes computed for backward-only-combining characters
 *
 *
 * - Tries
 *
 * The main structures are two trie tables ("compact arrays"),
 * each with one index array and one data array.
 * Generally, tries use the upper bits of an input value to access the index array,
 * which results in an index to the data array where a block of values is stored.
 * The lower bits of the same input value are then used to index inside that data
 * block to get to the specific data element for the input value.
 *
 * In order to use each trie with a single base pointer, the index values in
 * the index array are offset by the length of the index array.
 * With this, a base pointer to the trie index array is also directly used
 * with the index value to access the trie data array.
 * For example, if trieIndex[n] refers to offset m in trieData[] then
 * the actual value is q=trieIndex[n]=lengthof(trieIndex)+m
 * and you access trieIndex[q] instead of trieData[m].
 *
 *
 * - Folded tries
 *
 * The tries here are extended to work for lookups on UTF-16 strings with
 * supplementary characters encoded with surrogate pairs.
 * They are called "folded tries".
 *
 * Each 16-bit code unit (UChar, not code point UChar32) is looked up this way.
 * If there is relevant data for any given code unit, then the data or the code unit
 * must be checked for whether it is a leading surrogate.
 * If so, then the data contains an offset that is used together with the following
 * trailing surrogate code unit value for a second trie access.
 * This uses a portion of the index array beyond what is accessible with 16-bit units,
 * i.e., it uses the part of the trie index array starting at its index
 * 0x10000>>_NORM_TRIE_SHIFT.
 *
 * Such folded tries are useful when processing UTF-16 strings, especially if
 * many code points do not have relevant data, so that the check for
 * surrogates and the second trie lookup are rarely performed.
 * It avoids the overhead of a double-index trie that is necessary if the input
 * is always with 21-bit code points.
 *
 *
 * - Tries in unorm.dat
 *
 * The first trie consists of the stage1 and the norm32Table arrays.
 * It provides data for the NF* quick checks and normalization.
 * The second trie consists of the fcdStage1 and the fcdTable arrays
 * and provides data just for FCD checks.
 *
 *
 * - norm32 data words from the first trie
 *
 * The norm32Table contains one 32-bit word "norm32" per code point.
 * It contains the following bit fields:
 * 31..16   extra data index, _NORM_EXTRA_SHIFT is used to shift this field down
 *          if this index is <_NORM_EXTRA_INDEX_TOP then it is an index into
 *              extraData[] where variable-length normalization data for this
 *              code point is found
 *          if this index is <_NORM_EXTRA_INDEX_TOP+_NORM_EXTRA_SURROGATE_TOP
 *              then this is a norm32 for a leading surrogate, and the index
 *              value is used together with the following trailing surrogate
 *              code unit in the second trie access
 *          if this index is >=_NORM_EXTRA_INDEX_TOP+_NORM_EXTRA_SURROGATE_TOP
 *              then this is a norm32 for a "special" character,
 *              i.e., the character is a Hangul syllable or a Jamo
 *              see _NORM_EXTRA_HANGUL etc.
 *          generally, instead of extracting this index from the norm32 and
 *              comparing it with the above constants,
 *              the normalization code compares the entire norm32 value
 *              with _NORM_MIN_SPECIAL, _NORM_SURROGATES_TOP, _NORM_MIN_HANGUL etc.
 *
 * 15..8    combining class (cc) according to UnicodeData.txt
 *
 *  7..6    _NORM_COMBINES_ANY flags, used in composition to see if a character
 *              combines with any following or preceding character(s)
 *              at all
 *     7    _NORM_COMBINES_BACK
 *     6    _NORM_COMBINES_FWD
 *
 *  5..0    quick check flags, set for "no" or "maybe", with separate flags for
 *              each normalization form
 *              the higher bits are "maybe" flags; for NF*D there are no such flags
 *              the lower bits are "no" flags for all forms, in the same order
 *              as the "maybe" flags,
 *              which is (MSB to LSB): NFKD NFD NFKC NFC
 *  5..4    _NORM_QC_ANY_MAYBE
 *  3..0    _NORM_QC_ANY_NO
 *              see further related constants
 *
 *
 * - Extra data per code point
 *
 * "Extra data" is referenced by the index in norm32.
 * It is variable-length data. It is only present, and only those parts
 * of it are, as needed for a given character.
 * The norm32 extra data index is added to the beginning of extraData[]
 * to get to a vector of 16-bit words with data at the following offsets:
 *
 * [-1]     Combining index for composition.
 *              Stored only if norm32&_NORM_COMBINES_ANY .
 * [0]      Lengths of the canonical and compatibility decomposition strings.
 *              Stored only if there are decompositions, i.e.,
 *              if norm32&(_NORM_QC_NFD|_NORM_QC_NFKD)
 *          High byte: length of NFKD, or 0 if none
 *          Low byte: length of NFD, or 0 if none
 *          Each length byte also has another flag:
 *              Bit 7 of a length byte is set if there are non-zero
 *              combining classes (cc's) associated with the respective
 *              decomposition. If this flag is set, then the decomposition
 *              is preceded by a 16-bit word that contains the
 *              leading and trailing cc's.
 *              Bits 6..0 of a length byte are the length of the
 *              decomposition string, not counting the cc word.
 * [1..n]   NFD
 * [n+1..]  NFKD
 *
 * Each of the two decompositions consists of up to two parts:
 * - The 16-bit words with the leading and trailing cc's.
 *   This is only stored if bit 7 of the corresponding length byte
 *   is set. In this case, at least one of the cc's is not zero.
 *   High byte: leading cc==cc of the first code point in the decomposition string
 *   Low byte: trailing cc==cc of the last code point in the decomposition string
 * - The decomposition string in UTF-16, with length code units.
 *
 *
 * - Combining indexes and combiningTable[]
 *
 * Combining indexes are stored at the [-1] offset of the extra data
 * if the character combines forward or backward with any other characters.
 * They are used for (re)composition in NF*C.
 * Values of combining indexes are arranged according to whether a character
 * combines forward, backward, or both ways:
 *    forward-only < both ways < backward-only
 *
 * The index values for forward-only and both-ways combining characters
 * are indexes into the combiningTable[].
 * The index values for backward-only combining characters are simply
 * incremented from the preceding index values to be unique.
 *
 * In the combiningTable[], a variable-length list
 * of variable-length (back-index, code point) pair entries is stored
 * for each forward-combining character.
 *
 * These back-indexes are the combining indexes of both-ways or backward-only
 * combining characters that the forward-combining character combines with.
 *
 * Each list is sorted in ascending order of back-indexes.
 * Each list is terminated with the last back-index having bit 15 set.
 *
 * Each pair (back-index, code point) takes up either 2 or 3
 * 16-bit words.
 * The first word of a list entry is the back-index, with its bit 15 set if
 * this is the last pair in the list.
 *
 * The second word contains flags in bits 15..13 that determine
 * if there is a third word and how the combined character is encoded:
 * 15   set if there is a third word in this list entry
 * 14   set if the result is a supplementary character
 * 13   set if the result itself combines forward
 *
 * According to these bits 15..14 of the second word,
 * the result character is encoded as follows:
 * 00 or 01 The result is <=0x1fff and stored in bits 12..0 of
 *          the second word.
 * 10       The result is 0x2000..0xffff and stored in the third word.
 *          Bits 12..0 of the second word are not used.
 * 11       The result is a supplementary character.
 *          Bits 9..0 of the leading surrogate are in bits 9..0 of
 *          the second word.
 *          Add 0xd800 to these bits to get the complete surrogate.
 *          Bits 12..10 of the second word are not used.
 *          The trailing surrogate is stored in the third word.
 *
 *
 * - FCD trie
 *
 * The FCD trie is very simple.
 * It is a folded trie with 16-bit data words.
 * In each word, the high byte contains the leading cc of the character,
 * and the low byte contains the trailing cc of the character.
 * These cc's are the cc's of the first and last code points in the
 * canonical decomposition of the character.
 *
 * Since all 16 bits are used for cc's, lead surrogates must be tested
 * by checking the code unit instead of the trie data.
 * This is done only if the 16-bit data word is not zero.
 * If the code unit is a leading surrogate and the data word is not zero,
 * then instead of cc's it contains the offset for the second trie lookup.
 */

#endif
