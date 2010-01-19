/*
*******************************************************************************
*
*   Copyright (C) 2009-2010, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  normalizer2impl.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2009nov22
*   created by: Markus W. Scherer
*/

#ifndef __NORMALIZER2IMPL_H__
#define __NORMALIZER2IMPL_H__

#include "unicode/utypes.h"

#if !UCONFIG_NO_NORMALIZATION

#include "unicode/normalizer2.h"
#include "unicode/udata.h"
#include "unicode/unistr.h"
#include "unicode/unorm.h"
#include "mutex.h"
#include "uset_imp.h"
#include "utrie2.h"

U_NAMESPACE_BEGIN

class Hangul {
public:
    /* Korean Hangul and Jamo constants */
    enum {
        JAMO_L_BASE=0x1100,     /* "lead" jamo */
        JAMO_V_BASE=0x1161,     /* "vowel" jamo */
        JAMO_T_BASE=0x11a7,     /* "trail" jamo */

        HANGUL_BASE=0xac00,

        JAMO_L_COUNT=19,
        JAMO_V_COUNT=21,
        JAMO_T_COUNT=28,

        HANGUL_COUNT=JAMO_L_COUNT*JAMO_V_COUNT*JAMO_T_COUNT,
        HANGUL_LIMIT=HANGUL_BASE+HANGUL_COUNT
    };

    static inline UBool isHangul(UChar32 c) {
        return HANGUL_BASE<=c && c<HANGUL_LIMIT;
    }
    static inline UBool
    isHangulWithoutJamoT(UChar c) {
        c-=HANGUL_BASE;
        return c<HANGUL_COUNT && c%JAMO_T_COUNT==0;
    }
    static inline UBool isJamoL(UChar32 c) {
        return (uint32_t)(c-JAMO_L_BASE)<JAMO_L_COUNT;
    }
    static inline UBool isJamoV(UChar32 c) {
        return (uint32_t)(c-JAMO_V_BASE)<JAMO_V_COUNT;
    }

    /**
     * Decomposes c, which must be a Hangul syllable, into buffer
     * and returns the length of the decomposition (2 or 3).
     */
    static inline int32_t decompose(UChar32 c, UChar buffer[3]) {
        c-=HANGUL_BASE;
        UChar32 c2=c%JAMO_T_COUNT;
        c/=JAMO_T_COUNT;
        buffer[0]=(UChar)(JAMO_L_BASE+c/JAMO_V_COUNT);
        buffer[1]=(UChar)(JAMO_V_BASE+c%JAMO_V_COUNT);
        if(c2==0) {
            return 2;
        } else {
            buffer[2]=(UChar)(JAMO_T_BASE+c2);
            return 3;
        }
    }
private:
    Hangul();  // no instantiation
};

class Normalizer2Impl;

class ReorderingBuffer : public UMemory {
public:
    ReorderingBuffer(const Normalizer2Impl &ni, UnicodeString &dest) :
        impl(ni), str(dest),
        start(NULL), reorderStart(NULL), limit(NULL),
        remainingCapacity(0), lastCC(0) {}
    ~ReorderingBuffer() {
        if(start!=NULL) {
            str.releaseBuffer((int32_t)(limit-start));
        }
    }
    UBool init(int32_t destCapacity, UErrorCode &errorCode);

    UBool isEmpty() const { return start==limit; }
    int32_t length() const { return (int32_t)(limit-start); }
    UChar *getStart() { return start; }
    UChar *getLimit() { return limit; }
    uint8_t getLastCC() const { return lastCC; }

    UBool append(UChar32 c, uint8_t cc, UErrorCode &errorCode) {
        return (c<=0xffff) ?
            appendBMP((UChar)c, cc, errorCode) :
            appendSupplementary(c, cc, errorCode);
    }
    // s must be in NFD, otherwise change the implementation.
    UBool append(const UChar *s, int32_t length,
                 uint8_t leadCC, uint8_t trailCC,
                 UErrorCode &errorCode);
    UBool appendBMP(UChar c, uint8_t cc, UErrorCode &errorCode) {
        if(remainingCapacity==0 && !resize(1, errorCode)) {
            return FALSE;
        }
        if(lastCC<=cc || cc==0) {
            *limit++=c;
            lastCC=cc;
            if(cc<=1) {
                reorderStart=limit;
            }
        } else {
            insert(c, cc);
        }
        --remainingCapacity;
        return TRUE;
    }
    UBool appendZeroCC(UChar32 c, UErrorCode &errorCode);
    UBool appendZeroCC(const UChar *s, const UChar *sLimit, UErrorCode &errorCode);
    void removeZeroCCSuffix(int32_t length);
    void setReorderingLimitAndLastCC(UChar *newLimit, uint8_t newLastCC) {
        remainingCapacity+=(int32_t)(limit-newLimit);
        reorderStart=limit=newLimit;
        lastCC=newLastCC;
    }
private:
    /*
     * TODO: Revisit whether it makes sense to track reorderStart.
     * It is set to after the last known character with cc<=1,
     * which stops previousCC() before it reads that character and looks up its cc.
     * previousCC() is normally only called from insert().
     * In other words, reorderStart speeds up the insertion of a combining mark
     * into a multi-combining mark sequence where it does not belong at the end.
     * This might not be worth the trouble.
     * On the other hand, it's not a huge amount of trouble.
     *
     * We probably need it for UNORM_SIMPLE_APPEND.
     */

    UBool appendSupplementary(UChar32 c, uint8_t cc, UErrorCode &errorCode);
    void insert(UChar32 c, uint8_t cc);
    static void writeCodePoint(UChar *p, UChar32 c) {
        if(c<=0xffff) {
            *p=(UChar)c;
        } else {
            p[0]=U16_LEAD(c);
            p[1]=U16_TRAIL(c);
        }
    }
    UBool resize(int32_t appendLength, UErrorCode &errorCode);

    const Normalizer2Impl &impl;
    UnicodeString &str;
    UChar *start, *reorderStart, *limit;
    int32_t remainingCapacity;
    uint8_t lastCC;

    // private backward iterator
    void setIterator() { codePointStart=limit; }
    void skipPrevious();  // Requires start<codePointStart.
    uint8_t previousCC();  // Returns 0 if there is no previous character.

    UChar *codePointStart, *codePointLimit;
};

class U_COMMON_API Normalizer2Impl : public UMemory {
public:
    Normalizer2Impl() : memory(NULL), normTrie(NULL) {
        fcdTrieSingleton.fInstance=NULL;
    }
    ~Normalizer2Impl();

    void load(const char *packageName, const char *name, UErrorCode &errorCode);

    void addPropertyStarts(const USetAdder *sa, UErrorCode &errorCode) const;

    // low-level properties ------------------------------------------------ ***

    const UTrie2 *getNormTrie() const { return normTrie; }
    const UTrie2 *getFCDTrie(UErrorCode &errorCode) const ;

    uint16_t getNorm16(UChar32 c) const { return UTRIE2_GET16(normTrie, c); }

    UNormalizationCheckResult getCompQuickCheck(uint16_t norm16) const {
        if(norm16<minNoNo || MIN_YES_YES_WITH_CC<=norm16) {
            return UNORM_YES;
        } else if(minMaybeYes<=norm16) {
            return UNORM_MAYBE;
        } else {
            return UNORM_NO;
        }
    }
    UBool isCompNo(uint16_t norm16) const { return minNoNo<=norm16 && norm16<minMaybeYes; }
    UBool isDecompYes(uint16_t norm16) const { return norm16<minYesNo || minMaybeYes<=norm16; }

    uint8_t getCC(uint16_t norm16) const {
        if(norm16>=MIN_NORMAL_MAYBE_YES) {
            return (uint8_t)norm16;
        }
        if(norm16<minNoNo || limitNoNo<=norm16) {
            return 0;
        }
        return getCCFromNoNo(norm16);
    }
    static uint8_t getCCFromYesOrMaybe(uint16_t norm16) {
        return norm16>=MIN_NORMAL_MAYBE_YES ? (uint8_t)norm16 : 0;
    }

    uint16_t getFCD16(UChar32 c) const { return UTRIE2_GET16(fcdTrie(), c); }
    uint16_t getFCD16FromSingleLead(UChar c) const {
        return UTRIE2_GET16_FROM_U16_SINGLE_LEAD(fcdTrie(), c);
    }
    uint16_t getFCD16FromSupplementary(UChar32 c) const {
        return UTRIE2_GET16_FROM_SUPP(fcdTrie(), c);
    }
    uint16_t getFCD16FromSurrogatePair(UChar c, UChar c2) const {
        return getFCD16FromSupplementary(U16_GET_SUPPLEMENTARY(c, c2));
    }

    void setFCD16FromNorm16(UChar32 start, UChar32 end, uint16_t norm16,
                            UTrie2 *newFCDTrie, UErrorCode &errorCode) const;

    /**
     * Get the decomposition for one code point.
     * @param c code point
     * @param buffer out-only buffer for algorithmic decompositions
     * @param length out-only, takes the length of the decomposition, if any
     * @return pointer to the decomposition, or NULL if none
     */
    const UChar *getDecomposition(UChar32 c, UChar buffer[4], int32_t &length) const;

    enum {
        MIN_CCC_LCCC_CP=0x300
    };

    enum {
        MIN_YES_YES_WITH_CC=0xff01,
        JAMO_VT=0xff00,
        MIN_NORMAL_MAYBE_YES=0xfe00,
        JAMO_L=1,
        MAX_DELTA=0x40
    };

    enum {
        // Byte offsets from the start of the data, after the generic header.
        IX_NORM_TRIE_OFFSET,
        IX_EXTRA_DATA_OFFSET,
        IX_RESERVED2_OFFSET,
        IX_RESERVED3_OFFSET,
        IX_RESERVED4_OFFSET,
        IX_RESERVED5_OFFSET,
        IX_RESERVED6_OFFSET,
        IX_TOTAL_SIZE,

        // Code point thresholds for quick check codes.
        IX_MIN_DECOMP_NO_CP,
        IX_MIN_COMP_NO_MAYBE_CP,

        // Norm16 value thresholds for quick check combinations and types of extra data.
        IX_MIN_YES_NO,
        IX_MIN_NO_NO,
        IX_LIMIT_NO_NO,
        IX_MIN_MAYBE_YES,

        IX_RESERVED14,
        IX_RESERVED15,
        IX_COUNT
    };

    enum {
        MAPPING_HAS_CCC_LCCC_WORD=0x80,
        MAPPING_PLUS_COMPOSITION_LIST=0x40,
        MAPPING_NO_COMP_BOUNDARY_AFTER=0x20,
        MAPPING_LENGTH_MASK=0x1f
    };

    enum {
        COMP_1_LAST_TUPLE=0x8000,
        COMP_1_TRIPLE=1,
        COMP_1_TRAIL_LIMIT=0x3400,
        COMP_1_TRAIL_MASK=0x7ffe,
        COMP_1_TRAIL_SHIFT=9,  // 10-1 for the "triple" bit
        COMP_2_TRAIL_SHIFT=6,
        COMP_2_TRAIL_MASK=0xffc0
    };

    // higher-level functionality ------------------------------------------ ***

    const UChar *decompose(const UChar *src, const UChar *limit,
                           ReorderingBuffer *buffer, UErrorCode &errorCode) const;
    void decomposeAndAppend(const UChar *src, const UChar *limit,
                            UBool doDecompose,
                            ReorderingBuffer &buffer,
                            UErrorCode &errorCode) const;
    UBool compose(const UChar *src, const UChar *limit,
                  UBool onlyContiguous,
                  UBool doCompose,
                  ReorderingBuffer &buffer,
                  UErrorCode &errorCode) const;
    const UChar *composeQuickCheck(const UChar *src, const UChar *limit,
                                   UBool onlyContiguous,
                                   UNormalizationCheckResult *pQCResult) const;
    void composeAndAppend(const UChar *src, const UChar *limit,
                          UBool doCompose,
                          UBool onlyContiguous,
                          ReorderingBuffer &buffer,
                          UErrorCode &errorCode) const;
    const UChar *makeFCD(const UChar *src, const UChar *limit,
                         ReorderingBuffer *buffer, UErrorCode &errorCode) const;
    void makeFCDAndAppend(const UChar *src, const UChar *limit,
                          UBool doMakeFCD,
                          ReorderingBuffer &buffer,
                          UErrorCode &errorCode) const;

    UBool hasDecompBoundary(UChar32 c, UBool before) const;
    UBool isDecompInert(UChar32 c) const { return isDecompYesAndZeroCC(getNorm16(c)); }

    UBool hasCompBoundaryBefore(UChar32 c) const {
        return c<minCompNoMaybeCP || hasCompBoundaryBefore(c, getNorm16(c));
    }
    UBool hasCompBoundaryAfter(UChar32 c, UBool onlyContiguous, UBool testInert) const;

    UBool hasFCDBoundaryBefore(UChar32 c) const { return c<MIN_CCC_LCCC_CP || getFCD16(c)<=0xff; }
    UBool hasFCDBoundaryAfter(UChar32 c) const {
        uint16_t fcd16=getFCD16(c);
        return fcd16<=1 || (fcd16&0xff)==0;
    }
    UBool isFCDInert(UChar32 c) const { return getFCD16(c)<=1; }
private:
    static UBool U_CALLCONV
    isAcceptable(void *context, const char *type, const char *name, const UDataInfo *pInfo);

    UBool isMaybe(uint16_t norm16) const { return minMaybeYes<=norm16 && norm16<=JAMO_VT; }
    UBool isMaybeOrNonZeroCC(uint16_t norm16) const { return norm16>=minMaybeYes; }
    static UBool isInert(uint16_t norm16) { return norm16==0; }
    // static UBool isJamoL(uint16_t norm16) const { return norm16==1; }
    static UBool isJamoVT(uint16_t norm16) { return norm16==JAMO_VT; }
    UBool isHangul(uint16_t norm16) const { return norm16==minYesNo; }
    UBool isCompYesAndZeroCC(uint16_t norm16) const { return norm16<minNoNo; }
    // UBool isCompYes(uint16_t norm16) const {
    //     return norm16>=MIN_YES_YES_WITH_CC || norm16<minNoNo;
    // }
    // UBool isCompYesOrMaybe(uint16_t norm16) const {
    //     return norm16<minNoNo || minMaybeYes<=norm16;
    // }
    UBool hasZeroCCFromDecompYes(uint16_t norm16) {
        return norm16<=MIN_NORMAL_MAYBE_YES || norm16==JAMO_VT;
    }
    UBool isDecompYesAndZeroCC(uint16_t norm16) const {
        return norm16<minYesNo ||
               norm16==JAMO_VT ||
               (minMaybeYes<=norm16 && norm16<=MIN_NORMAL_MAYBE_YES);
    }
    /**
     * A little faster and simpler than isDecompYesAndZeroCC() but does not include
     * the MaybeYes which combine-forward and have ccc=0.
     * (Standard Unicode 5.2 normalization does not have such characters.)
     */
    UBool isMostDecompYesAndZeroCC(uint16_t norm16) const {
        return norm16<minYesNo || norm16==MIN_NORMAL_MAYBE_YES || norm16==JAMO_VT;
    }
    UBool isDecompNoAlgorithmic(uint16_t norm16) const { return norm16>=limitNoNo; }

    // For use with isCompYes().
    // Perhaps the compiler can combine the two tests for MIN_YES_YES_WITH_CC.
    // static uint8_t getCCFromYes(uint16_t norm16) {
    //     return norm16>=MIN_YES_YES_WITH_CC ? (uint8_t)norm16 : 0;
    // }
    uint8_t getCCFromNoNo(uint16_t norm16) const {
        const uint16_t *mapping=getMapping(norm16);
        if(*mapping&MAPPING_HAS_CCC_LCCC_WORD) {
            return (uint8_t)mapping[1];
        } else {
            return 0;
        }
    }
    // requires that the [cpStart..cpLimit[ character passes isCompYesAndZeroCC()
    uint8_t getTrailCCFromCompYesAndZeroCC(const UChar *cpStart, const UChar *cpLimit) const;

    // Requires algorithmic-NoNo.
    UChar32 mapAlgorithmic(UChar32 c, uint16_t norm16) const {
        return c+norm16-(minMaybeYes-MAX_DELTA-1);
    }

    // Requires minYesNo<norm16<limitNoNo.
    const uint16_t *getMapping(uint16_t norm16) const { return extraData+norm16; }
    const uint16_t *getCompositionsListForDecompYesAndZeroCC(uint16_t norm16) const {
        if(norm16==0 || MIN_NORMAL_MAYBE_YES<=norm16) {
            return NULL;
        } else if(norm16<minMaybeYes) {
            return extraData+norm16;  // for yesYes; if Jamo L: harmless empty list
        } else {
            return maybeYesCompositions+norm16-minMaybeYes;
        }
    }
    const uint16_t *getCompositionsListForComposite(uint16_t norm16) const {
        const uint16_t *list=extraData+norm16;  // composite has both mapping & compositions list
        return list+  // mapping pointer
            1+  // +1 to skip the first unit with the mapping lenth
            (*list&MAPPING_LENGTH_MASK)+  // + mapping length
            ((*list>>7)&1);  // +1 if MAPPING_HAS_CCC_LCCC_WORD
    }

    const UChar *copyLowPrefixFromNulTerminated(const UChar *src,
                                                UChar32 minNeedDataCP,
                                                ReorderingBuffer *buffer,
                                                UErrorCode &errorCode) const;
    UBool decomposeShort(const UChar *src, const UChar *limit,
                         ReorderingBuffer &buffer, UErrorCode &errorCode) const;
    UBool decompose(UChar32 c, uint16_t norm16,
                    ReorderingBuffer &buffer, UErrorCode &errorCode) const;

    static int32_t combine(const uint16_t *list, UChar32 trail);
    void recompose(ReorderingBuffer &buffer, int32_t recomposeStartIndex,
                   UBool onlyContiguous) const;

    UBool hasCompBoundaryBefore(UChar32 c, uint16_t norm16) const;
    const UChar *findPreviousCompBoundary(const UChar *start, const UChar *p) const;
    const UChar *findNextCompBoundary(const UChar *p, const UChar *limit) const;

    const UTrie2 *fcdTrie() const { return (const UTrie2 *)fcdTrieSingleton.fInstance; }

    const UChar *findPreviousFCDBoundary(const UChar *start, const UChar *p) const;
    const UChar *findNextFCDBoundary(const UChar *p, const UChar *limit) const;

    UDataMemory *memory;
    UVersionInfo dataVersion;

    // Code point thresholds for quick check codes.
    UChar32 minDecompNoCP;
    UChar32 minCompNoMaybeCP;

    // Norm16 value thresholds for quick check combinations and types of extra data.
    uint16_t minYesNo;
    uint16_t minNoNo;
    uint16_t limitNoNo;
    uint16_t minMaybeYes;

    UTrie2 *normTrie;
    const uint16_t *maybeYesCompositions;
    const uint16_t *extraData;  // mappings and/or compositions for yesYes, yesNo & noNo characters

    SimpleSingleton fcdTrieSingleton;
};

/**
 * ICU-internal shortcut for quick access to standard Unicode normalization.
 */
class U_COMMON_API Normalizer2Factory {
public:
    static const Normalizer2 *getNFCInstance(UErrorCode &errorCode);
    static const Normalizer2 *getNFDInstance(UErrorCode &errorCode);
    static const Normalizer2 *getFCDInstance(UErrorCode &errorCode);
    static const Normalizer2 *getFCCInstance(UErrorCode &errorCode);
    static const Normalizer2 *getNFKCInstance(UErrorCode &errorCode);
    static const Normalizer2 *getNFKDInstance(UErrorCode &errorCode);
    static const Normalizer2 *getNFKC_CFInstance(UErrorCode &errorCode);
    static const Normalizer2 *getNoopInstance(UErrorCode &errorCode);

    static const Normalizer2 *getInstance(UNormalizationMode mode, UErrorCode &errorCode);

    static const Normalizer2Impl *getNFCImpl(UErrorCode &errorCode);
    static const Normalizer2Impl *getNFKCImpl(UErrorCode &errorCode);
    static const Normalizer2Impl *getNFKC_CFImpl(UErrorCode &errorCode);

    static const UTrie2 *getFCDTrie(UErrorCode &errorCode);
private:
    Normalizer2Factory();  // No instantiation.
};

U_NAMESPACE_END

U_CAPI int32_t U_EXPORT2
unorm2_swap(const UDataSwapper *ds,
            const void *inData, int32_t length, void *outData,
            UErrorCode *pErrorCode);

/**
 * Get the NF*_QC property for a code point, for u_getIntPropertyValue().
 * @internal
 */
U_CFUNC UNormalizationCheckResult U_EXPORT2
unorm_getQuickCheck(UChar32 c, UNormalizationMode mode);

/**
 * Internal API, used by collation code.
 * Get access to the internal FCD trie table to be able to perform
 * incremental, per-code unit, FCD checks in collation.
 * One pointer is sufficient because the trie index values are offset
 * by the index size, so that the same pointer is used to access the trie data.
 * Code points at fcdHighStart and above have a zero FCD value.
 * @internal
 */
U_CAPI const uint16_t * U_EXPORT2
unorm_getFCDTrieIndex(UChar32 &fcdHighStart, UErrorCode *pErrorCode);

/**
 * Internal API, used by collation code.
 * Get the FCD value for a code unit, with
 * bits 15..8   lead combining class
 * bits  7..0   trail combining class
 *
 * If c is a lead surrogate and the value is not 0,
 * then some of c's associated supplementary code points have a non-zero FCD value.
 *
 * @internal
 */
static inline uint16_t
unorm_getFCD16(const uint16_t *fcdTrieIndex, UChar c) {
    return fcdTrieIndex[_UTRIE2_INDEX_FROM_U16_SINGLE_LEAD(fcdTrieIndex, c)];
}

/**
 * Internal API, used by collation code.
 * Get the FCD value of the next code point (post-increment), with
 * bits 15..8   lead combining class
 * bits  7..0   trail combining class
 *
 * @internal
 */
static inline uint16_t
unorm_nextFCD16(const uint16_t *fcdTrieIndex, UChar32 fcdHighStart,
                const UChar *&s, const UChar *limit) {
    UChar32 c=*s++;
    uint16_t fcd=fcdTrieIndex[_UTRIE2_INDEX_FROM_U16_SINGLE_LEAD(fcdTrieIndex, c)];
    if(fcd!=0 && U16_IS_LEAD(c)) {
        UChar c2;
        if(s!=limit && U16_IS_TRAIL(c2=*s)) {
            ++s;
            c=U16_GET_SUPPLEMENTARY(c, c2);
            if(c<fcdHighStart) {
                fcd=fcdTrieIndex[_UTRIE2_INDEX_FROM_SUPP(fcdTrieIndex, c)];
            } else {
                fcd=0;
            }
        } else /* unpaired lead surrogate */ {
            fcd=0;
        }
    }
    return fcd;
}

/**
 * Internal API, used by collation code.
 * Get the FCD value of the previous code point (pre-decrement), with
 * bits 15..8   lead combining class
 * bits  7..0   trail combining class
 *
 * @internal
 */
static inline uint16_t
unorm_prevFCD16(const uint16_t *fcdTrieIndex, UChar32 fcdHighStart,
                const UChar *start, const UChar *&s) {
    UChar32 c=*--s;
    uint16_t fcd;
    if(!U16_IS_SURROGATE(c)) {
        fcd=fcdTrieIndex[_UTRIE2_INDEX_FROM_U16_SINGLE_LEAD(fcdTrieIndex, c)];
    } else {
        UChar c2;
        if(U16_IS_SURROGATE_TRAIL(c) && s!=start && U16_IS_LEAD(c2=*(s-1))) {
            --s;
            c=U16_GET_SUPPLEMENTARY(c2, c);
            if(c<fcdHighStart) {
                fcd=fcdTrieIndex[_UTRIE2_INDEX_FROM_SUPP(fcdTrieIndex, c)];
            } else {
                fcd=0;
            }
        } else /* unpaired surrogate */ {
            fcd=0;
        }
    }
    return fcd;
}

#endif  /* !UCONFIG_NO_NORMALIZATION */
#endif  /* __NORMALIZER2IMPL_H__ */
