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

/* value constants */
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

    _NORM_COMBINES_FWD=0x40,
    _NORM_COMBINES_BACK=0x80,
    _NORM_COMBINES_ANY=0xc0,

#if 0
    _NORM_CC_TYPE_MASK=0xc0,
    _NORM_CC_TYPE_NONE=0,       /* no cc - lead and trail cc are 0 */
    _NORM_CC_TYPE_SAME=0x40,    /* lead and trail cc are same, non-zero, and in value */
    _NORM_CC_TYPE_TRAIL=0x80,   /* lead cc=0, trail cc in value */
    _NORM_CC_TYPE_TWO=0xc0,     /* 0 != lead cc < trail cc, lead cc in value, trail cc in extra data */

    _NORM_CC_HAS_LEAD=0x40,     /* side effect of the above flags: if and only if bit 6 is 0, then lead cc is 0 */
    _NORM_CC_HAS_LEAD_HAS_TRAIL=0x80,   /* if(has lead) then one can check for (has trail) instead of (&cc mask==same/two) */
#endif

    _NORM_CC_SHIFT=8,           /* UnicodeData.txt combining class in bits 15..8 */
    _NORM_CC_MASK=0xff00,

    _NORM_EXTRA_SHIFT=16,               /* 16 bits for the index to UChars and other extra data */
    _NORM_EXTRA_INDEX_TOP=0xfc00,       /* start of surrogate specials after shift */

    _NORM_EXTRA_SURROGATE_MASK=0x3ff,
    _NORM_EXTRA_SURROGATE_TOP=0x3f0,    /* hangul etc. */

    _NORM_EXTRA_HANGUL=_NORM_EXTRA_SURROGATE_TOP,
    _NORM_EXTRA_JAMO_1,                 /* ### not used */
    _NORM_EXTRA_JAMO_2,
    _NORM_EXTRA_JAMO_3
};

/* value constants using >16 bits */
#define _NORM_MIN_SPECIAL       0xfc000000
#define _NORM_SURROGATES_TOP    0xfff00000
#define _NORM_MIN_HANGUL        0xfff00000
#define _NORM_MIN_JAMO2         0xfff20000
#define _NORM_JAMO2_TOP         0xfff30000


/* index values */
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
 * Does not check for bad input and uses growBuffer.
 * @internal
 */
U_CFUNC int32_t
unorm_internalNormalize(UChar *dest, int32_t destCapacity,
                        const UChar *src, int32_t srcLength,
                        UNormalizationMode mode, UBool ignoreHangul,
                        UGrowBuffer *growBuffer, void *context,
                        UErrorCode *pErrorCode);

/**
 * internal API, used by normlzr.cpp
 * @internal
 */
U_CFUNC int32_t
unorm_decompose(UChar *dest, int32_t destCapacity,
                const UChar *src, int32_t srcLength,
                UBool compat, UBool ignoreHangul,
                UGrowBuffer *growBuffer, void *context,
                UErrorCode *pErrorCode);

/**
 * internal API, used by normlzr.cpp
 * @internal
 */
U_CFUNC int32_t
unorm_compose(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              UBool compat, UBool ignoreHangul,
              UGrowBuffer *growBuffer, void *context,
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

/**
 * Internal API for iterative normalizing - see Normalizer.
 * @internal
 */
U_CFUNC int32_t
unorm_nextDecompose(UChar *dest, int32_t destCapacity,
                    CharacterIterator &src,
                    UBool compat, UBool ignoreHangul,
                    UGrowBuffer *growBuffer, void *context,
                    UErrorCode *pErrorCode);

/**
 * Internal API for iterative normalizing - see Normalizer.
 * @internal
 */
U_CFUNC int32_t
unorm_prevDecompose(UChar *dest, int32_t destCapacity,
                    CharacterIterator &src,
                    UBool compat, UBool ignoreHangul,
                    UGrowBuffer *growBuffer, void *context,
                    UErrorCode *pErrorCode);

/**
 * Internal API for iterative normalizing - see Normalizer.
 * @internal
 */
U_CFUNC int32_t
unorm_nextFCD(UChar *dest, int32_t destCapacity,
              CharacterIterator &src,
              UGrowBuffer *growBuffer, void *context,
              UErrorCode *pErrorCode);

/**
 * Internal API for iterative normalizing - see Normalizer.
 * @internal
 */
U_CFUNC int32_t
unorm_prevFCD(UChar *dest, int32_t destCapacity,
              CharacterIterator &src,
              UGrowBuffer *growBuffer, void *context,
              UErrorCode *pErrorCode);

/**
 * Internal API for iterative normalizing - see Normalizer.
 * @internal
 */
U_CFUNC int32_t
unorm_nextCompose(UChar *dest, int32_t destCapacity,
                  CharacterIterator &src,
                  UBool compat, UBool ignoreHangul,
                  UGrowBuffer *growBuffer, void *context,
                  UErrorCode *pErrorCode);

/**
 * Internal API for iterative normalizing - see Normalizer.
 * @internal
 */
U_CFUNC int32_t
unorm_prevCompose(UChar *dest, int32_t destCapacity,
                  CharacterIterator &src,
                  UBool compat, UBool ignoreHangul,
                  UGrowBuffer *growBuffer, void *context,
                  UErrorCode *pErrorCode);

#endif

#endif
