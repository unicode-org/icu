/*
******************************************************************************
* Copyright (c) 1996-2001, International Business Machines
* Corporation and others. All Rights Reserved.
******************************************************************************
* File unorm.cpp
*
* Created by: Vladimir Weinstein 12052000
*
* Modification history :
*
* Date        Name        Description
* 02/01/01    synwee      Added normalization quickcheck enum and method.
* 02/12/01    synwee      Commented out quickcheck util api has been approved
*                         Added private method for doing FCD checks
* 02/23/01    synwee      Modified quickcheck and checkFCE to run through 
*                         string for codepoints < 0x300 for the normalization 
*                         mode NFC.
* 05/25/01+   Markus Scherer total rewrite, implement all normalization here
*                         instead of just wrappers around normlzr.cpp,
*                         load unorm.dat, support Unicode 3.1 with
*                         supplementary code points, etc.
*/

#include "unicode/utypes.h"
#include "unicode/ustring.h"
#include "unicode/chariter.h"
#include "unicode/udata.h"
#include "unicode/unorm.h"
#include "cmemory.h"
#include "ustr_imp.h"
#include "umutex.h"
#include "unormimp.h"

/*
 * This new implementation of the normalization code loads its data from
 * unorm.dat, which is generated with the gennorm tool.
 * The format of that file is described in unormimp.h .
 */

/* -------------------------------------------------------------------------- */

/* Korean Hangul and Jamo constants */
enum {
    JAMO_L_BASE=0x1100,     /* "lead" jamo */
    JAMO_V_BASE=0x1161,     /* "vowel" jamo */
    JAMO_T_BASE=0x11a7,     /* "trail" jamo */

    HANGUL_BASE=0xac00,

    JAMO_L_COUNT=19,
    JAMO_V_COUNT=21,
    JAMO_T_COUNT=28,

    HANGUL_COUNT=JAMO_L_COUNT*JAMO_V_COUNT*JAMO_T_COUNT
};

inline UBool
isHangulWithoutJamoT(UChar c) {
    c-=HANGUL_BASE;
    return c<HANGUL_COUNT && c%JAMO_T_COUNT==0;
}

/* norm32 helpers */

/* is this a norm32 with a regular index? */
inline UBool
isNorm32Regular(uint32_t norm32) {
    return norm32<_NORM_MIN_SPECIAL;
}

/* is this a norm32 with a special index for a lead surrogate? */
inline UBool
isNorm32LeadSurrogate(uint32_t norm32) {
    return _NORM_MIN_SPECIAL<=norm32 && norm32<_NORM_SURROGATES_TOP;
}

/* is this a norm32 with a special index for a Hangul syllable or a Jamo? */
inline UBool
isNorm32HangulOrJamo(uint32_t norm32) {
    return norm32>=_NORM_MIN_HANGUL;
}

/*
 * Given isNorm32HangulOrJamo(),
 * is this a Hangul syllable or a Jamo?
 */
inline UBool
isHangulJamoNorm32HangulOrJamoL(uint32_t norm32) {
    return norm32<_NORM_MIN_JAMO_V;
}

/*
 * Given norm32 for Jamo V or T,
 * is this a Jamo V?
 */
inline UBool
isJamoVTNorm32JamoV(uint32_t norm32) {
    return norm32<_NORM_JAMO_V_TOP;
}

/* load unorm.dat ----------------------------------------------------------- */

#define DATA_NAME "unorm"
#define DATA_TYPE "dat"

static UDataMemory *normData=NULL;
static UErrorCode dataErrorCode=U_ZERO_ERROR;
static int8_t haveNormData=0;

/*
 * pointers into the memory-mapped unorm.dat
 */
static const uint16_t *indexes=NULL,
                      *normTrieIndex=NULL, *extraData=NULL,
                      *combiningTable=NULL,
                      *fcdTrieIndex=NULL;

/*
 * note that there is no uint32_t *normTrieData:
 * the indexes in the trie are adjusted so that they point to the data based on
 * (uint32_t *)normTrieIndex - this saves one variable at runtime
 */
#define normTrieData ((uint32_t *)normTrieIndex)

/* similarly for the FCD trie index and data - but both are uint16_t * */

/* the Unicode version of the normalization data */
static UVersionInfo dataVersion={ 3, 1, 0, 0 };

U_CDECL_BEGIN

static UBool U_CALLCONV
isAcceptable(void * /* context */,
             const char * /* type */, const char * /* name */,
             const UDataInfo *pInfo) {
    if(
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->dataFormat[0]==0x4e &&   /* dataFormat="Norm" */
        pInfo->dataFormat[1]==0x6f &&
        pInfo->dataFormat[2]==0x72 &&
        pInfo->dataFormat[3]==0x6d &&
        pInfo->formatVersion[0]==1 &&
        pInfo->formatVersion[3]==_NORM_TRIE_SHIFT
    ) {
        uprv_memcpy(dataVersion, pInfo->dataVersion, 4);
        return TRUE;
    } else {
        return FALSE;
    }
}

U_CDECL_END

static int8_t
loadNormData(UErrorCode &errorCode) {
    /* load Unicode normalization data from file */
    if(haveNormData==0) {
        UDataMemory *data;
        const uint16_t *p=NULL;

        if(&errorCode==NULL || U_FAILURE(errorCode)) {
            return 0;
        }

        /* open the data outside the mutex block */
        data=udata_openChoice(NULL, DATA_TYPE, DATA_NAME, isAcceptable, NULL, &errorCode);
        dataErrorCode=errorCode;
        if(U_FAILURE(errorCode)) {
            return haveNormData=-1;
        }

        p=(const uint16_t *)udata_getMemory(data);

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if(normData==NULL) {
            normData=data;
            data=NULL;
            indexes=p;
            p=NULL;
        }
        umtx_unlock(NULL);

        /* initialize some variables */
        normTrieIndex=indexes+indexes[_NORM_INDEX_COUNT];
        extraData=normTrieIndex+indexes[_NORM_INDEX_TRIE_INDEX_COUNT]+2*indexes[_NORM_INDEX_TRIE_DATA_COUNT];
        combiningTable=extraData+indexes[_NORM_INDEX_UCHAR_COUNT];
        fcdTrieIndex=combiningTable+indexes[_NORM_INDEX_COMBINE_DATA_COUNT];
        haveNormData=1;

        /* if a different thread set it first, then close the extra data */
        if(data!=NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return haveNormData;
}

inline UBool
_haveData(UErrorCode &errorCode) {
    if(haveNormData!=0) {
        errorCode=dataErrorCode;
        return (UBool)(haveNormData>0);
    } else {
        return (UBool)(loadNormData(errorCode)>0);
    }
}

U_CAPI UBool U_EXPORT2
unorm_haveData(UErrorCode *pErrorCode) {
    return _haveData(*pErrorCode);
}

U_CAPI const uint16_t * U_EXPORT2
unorm_getFCDTrie(UErrorCode *pErrorCode) {
    if(_haveData(*pErrorCode)) {
        return fcdTrieIndex;
    } else {
        return NULL;
    }
}

/* data access primitives --------------------------------------------------- */

inline uint32_t
_getNorm32(UChar c) {
    return
        normTrieData[
            normTrieIndex[
                c>>_NORM_TRIE_SHIFT
            ]+
            (c&_NORM_STAGE_2_MASK)
        ];
}

inline uint32_t
_getNorm32FromSurrogatePair(uint32_t norm32, UChar c2) {
    /* the surrogate index in norm32 is an offset over the BMP top of stage 1 */
    uint32_t c=
        ((norm32>>(_NORM_EXTRA_SHIFT-10))&0xffc00)|
        (c2&0x3ff);
    return
        normTrieData[
            normTrieIndex[
                _NORM_STAGE_1_BMP_COUNT+
                (c>>_NORM_TRIE_SHIFT)
            ]+
            (c&_NORM_STAGE_2_MASK)
        ];
}

inline uint16_t
_getFCD16(UChar c) {
    return
        fcdTrieIndex[
            fcdTrieIndex[
                c>>_NORM_TRIE_SHIFT
            ]+
            (c&_NORM_STAGE_2_MASK)
        ];
}

inline uint16_t
_getFCD16FromSurrogatePair(uint16_t fcd16, UChar c2) {
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

inline const uint16_t *
_getExtraData(uint32_t norm32) {
    return extraData+(norm32>>_NORM_EXTRA_SHIFT);
}

/* get the canonical or compatibility decomposition for one character */
inline const UChar *
_decompose(uint32_t norm32, uint32_t qcMask, int32_t &length,
           uint8_t &cc, uint8_t &trailCC) {
    const UChar *p=(const UChar *)_getExtraData(norm32);
    length=*p++;

    if((norm32&qcMask&_NORM_QC_NFKD)!=0 && length>=0x100) {
        /* use compatibility decomposition, skip canonical data */
        p+=((length>>7)&1)+(length&_NORM_DECOMP_LENGTH_MASK);
        length>>=8;
    }

    if(length&_NORM_DECOMP_FLAG_LENGTH_HAS_CC) {
        /* get the lead and trail cc's */
        UChar bothCCs=*p++;
        cc=(uint8_t)(bothCCs>>8);
        trailCC=(uint8_t)bothCCs;
    } else {
        /* lead and trail cc's are both 0 */
        cc=trailCC=0;
    }

    length&=_NORM_DECOMP_LENGTH_MASK;
    return p;
}

/* get the canonical decomposition for one character */
inline const UChar *
_decompose(uint32_t norm32, int32_t &length,
           uint8_t &cc, uint8_t &trailCC) {
    const UChar *p=(const UChar *)_getExtraData(norm32);
    length=*p++;

    if(length&_NORM_DECOMP_FLAG_LENGTH_HAS_CC) {
        /* get the lead and trail cc's */
        UChar bothCCs=*p++;
        cc=(uint8_t)(bothCCs>>8);
        trailCC=(uint8_t)bothCCs;
    } else {
        /* lead and trail cc's are both 0 */
        cc=trailCC=0;
    }

    length&=_NORM_DECOMP_LENGTH_MASK;
    return p;
}

/*
 * get the combining class of (c, c2)=*p++
 * before: p<limit  after: p<=limit
 * if only one code unit is used, then c2==0
 */
inline uint8_t
_getNextCC(const UChar *&p, const UChar *limit, UChar &c, UChar &c2) {
    uint32_t norm32;

    c=*p++;
    norm32=_getNorm32(c);
    if((norm32&_NORM_CC_MASK)==0) {
        c2=0;
        return 0;
    } else {
        if(!isNorm32LeadSurrogate(norm32)) {
            c2=0;
        } else {
            /* c is a lead surrogate, get the real norm32 */
            if(p!=limit && UTF_IS_SECOND_SURROGATE(c2=*p)) {
                ++p;
                norm32=_getNorm32FromSurrogatePair(norm32, c2);
            } else {
                c2=0;
                return 0;
            }
        }

        return (uint8_t)(norm32>>_NORM_CC_SHIFT);
    }
}

/*
 * get the combining class of (c, c2)=*--p
 * before: start<p  after: start<=p
 */
inline uint8_t
_getPrevCC(const UChar *start, const UChar *&p) {
    uint32_t norm32;
    UChar c, c2;

    c=*--p;

    /* check for a surrogate before getting norm32 to see if we need to predecrement further */
    if(!UTF_IS_SURROGATE(c)) {
        return (uint8_t)(_getNorm32(c)>>_NORM_CC_SHIFT);
    } else if(UTF_IS_SURROGATE_FIRST(c)) {
        /* unpaired first surrogate */
        return 0;
    } else if(p!=start && UTF_IS_FIRST_SURROGATE(c2=*(p-1))) {
        --p;
        norm32=_getNorm32(c2);
        if((norm32&_NORM_CC_MASK)==0) {
            /* all surrogate pairs with this lead surrogate have cc==0 */
            return 0;
        } else {
            /* norm32 must be a surrogate special */
            return (uint8_t)(_getNorm32FromSurrogatePair(norm32, c)>>_NORM_CC_SHIFT);
        }
    } else {
        /* unpaired second surrogate */
        return 0;
    }
}

/* reorder UTF-16 in-place -------------------------------------------------- */

/*
 * merge two UTF-16 string parts together
 * to canonically order (order by combining classes) their concatenation
 *
 * the two strings may already be adjacent, so that the merging is done in-place
 * if the two strings are not adjacent, then the buffer holding the first one
 * must be large enough
 * the second string may or may not be ordered in itself
 *
 * before: [start..current[ is already ordered, and
 *         [next..limit[    may be ordered in itself, but
 *                          is not in relation to [start..current[
 * after: [start..current+(limit-next)[ is ordered
 *
 * the algorithm is a simple bubble-sort that takes the characters from *next++
 * and inserts them in correct combining class order into the preceding part
 * of the string
 *
 * returns the trailing combining class
 * ### TODO see how often this is used - if very rare then just iterate over [next..limit[ and call optimized fn
 */
static uint8_t
_mergeOrdered(UChar *start, UChar *current,
              const UChar *next, const UChar *limit, UBool isOrdered=TRUE) {
    const UChar *pBack, *pPreBack;
    UChar *q, *r;
    UChar c, c2;
    uint8_t cc, prevCC, trailCC=0;
    UBool adjacent;

    adjacent= current==next;

    if(start!=current || !isOrdered) {
        while(next<limit) {
            cc=_getNextCC(next, limit, c, c2);
            if(cc==0) {
                /* does not bubble back */
                trailCC=0;
                if(adjacent) {
                    current=(UChar *)next;
                } else {
                    *current++=c;
                    if(c2!=0) {
                        *current++=c2;
                    }
                }
                if(isOrdered) {
                    break;
                } else {
                    start=current;
                }
            } else {
                /* search for the insertion point where cc>=prevCC */
                pPreBack=pBack=current;
                prevCC=_getPrevCC(start, pPreBack);
                if(cc>=prevCC) {
                    /* does not bubble back */
                    trailCC=cc;
                    if(adjacent) {
                        current=(UChar *)next;
                    } else {
                        *current++=c;
                        if(c2!=0) {
                            *current++=c2;
                        }
                    }
                    if(isOrdered) {
                        break;
                    }
                } else {
                    /* this will be the last code point, so keep its cc */
                    trailCC=prevCC;
                    pBack=pPreBack;
                    while(start<pPreBack) {
                        prevCC=_getPrevCC(start, pPreBack);
                        if(cc>=prevCC) {
                            break;
                        }
                        pBack=pPreBack;
                    }

                    /*
                     * this is where we are right now with all these pointers:
                     * [start..pPreBack[ 0..? code points that we can ignore
                     * [pPreBack..pBack[ 0..1 code points with prevCC<=cc
                     * [pBack..current[  0..n code points with >cc, move up to insert (c, c2)
                     * [current..next[      1 code point (c, c2) with cc
                     * [next..limit[     0..? code points yet to be bubbled in
                     *
                     * note that current and next may be unrelated (if not adjacent)!
                     */

                    /* move the code units in between up (q moves left of r) */
                    q=current;
                    r=current= c2==0 ? current+1 : current+2;
                    do {
                        *--r=*--q;
                    } while(pBack!=q);

                    /* insert (c, c2) */
                    *q=c;
                    if(c2!=0) {
                        *(q+1)=c2;
                    }

                    if(isOrdered) {
                        /* we know that the new part is ordered in itself, so we can move start up */
                        start=r; /* set it to after where (c, c2) were inserted */
                    }
                }
            }
        }
    }

    if(next==limit) {
        /* we know the cc of the last code point */
        return trailCC;
    } else {
        if(!adjacent) {
            /* copy the second string part */
            do {
                *current++=*next++;
            } while(next!=limit);
            limit=current;
        }
        return _getPrevCC(start, limit);
    }
}

/*
 * simpler, more efficient version of _mergeOrdered() -
 * inserts only one code point into the preceding string
 * assume that (c, c2) has not yet been inserted at [current..p[
 * ### TODO doc that p=current+1 or +2 according to c2=?=0
 */
static uint8_t
_insertOrdered(const UChar *start, UChar *current, UChar *p,
               UChar c, UChar c2, uint8_t cc) {
    const UChar *pBack, *pPreBack;
    UChar *r;
    uint8_t prevCC, trailCC=cc;

    if(start<current && cc!=0) {
        /* search for the insertion point where cc>=prevCC */
        pPreBack=pBack=current;
        prevCC=_getPrevCC(start, pPreBack);
        if(cc<prevCC) {
            /* this will be the last code point, so keep its cc */
            trailCC=prevCC;
            pBack=pPreBack;
            while(start<pPreBack) {
                prevCC=_getPrevCC(start, pPreBack);
                if(cc>=prevCC) {
                    break;
                }
                pBack=pPreBack;
            }

            /*
             * this is where we are right now with all these pointers:
             * [start..pPreBack[ 0..? code points that we can ignore
             * [pPreBack..pBack[ 0..1 code points with prevCC<=cc
             * [pBack..current[  0..n code points with >cc, move up to insert (c, c2)
             * [current..p[         1 code point (c, c2) with cc
             */

            /* move the code units in between up */
            r=p;
            do {
                *--r=*--current;
            } while(pBack!=current);
        }
    }

    /* insert (c, c2) */
    *current=c;
    if(c2!=0) {
        *(current+1)=c2;
    }

    /* we know the cc of the last code point */
    return trailCC;
}

/* quick check functions ---------------------------------------------------- */

static UBool
unorm_checkFCD(const UChar *src, int32_t srcLength) {
    const UChar *limit;
    UChar c, c2;
    uint16_t fcd16;
    int16_t prevCC, cc;

    /* initialize */
    prevCC=0;

    if(srcLength>=0) {
        /* string with length */
        limit=src+srcLength;
    } else /* srcLength==-1 */ {
        /* zero-terminated string */
        limit=NULL;
    }

    U_ALIGN_CODE(16);

    for(;;) {
        /* skip a run of code units below the minimum or with irrelevant data for the FCD check */
        if(limit==NULL) {
            for(;;) {
                c=*src++;
                if(c<_NORM_MIN_WITH_LEAD_CC) {
                    if(c==0) {
                        return TRUE;
                    }
                    /* ### TODO comment this is safe because c<=0x300... */
                    prevCC=-(int16_t)c;
                } else if((fcd16=_getFCD16(c))==0) {
                    prevCC=0;
                } else {
                    break;
                }
            }
        } else {
            for(;;) {
                if(src==limit) {
                    return TRUE;
                } else if((c=*src++)<_NORM_MIN_WITH_LEAD_CC) {
                    prevCC=-(int16_t)c;
                } else if((fcd16=_getFCD16(c))==0) {
                    prevCC=0;
                } else {
                    break;
                }
            }
        }

        /* check one above-minimum, relevant code unit */
        if(UTF_IS_FIRST_SURROGATE(c)) {
            /* c is a lead surrogate, get the real fcd16 */
            if((limit==NULL || src!=limit) && UTF_IS_SECOND_SURROGATE(c2=*src)) {
                ++src;
                fcd16=_getFCD16FromSurrogatePair(fcd16, c2);
            } else {
                fcd16=0;
            }
        }

        /*
         * prevCC has values from the following ranges:
         * 0..0xff - the previous trail combining class
         * <0      - the negative value of the previous code unit;
         *           that code unit was <_NORM_MIN_WITH_LEAD_CC and its _getFCD16()
         *           was deferred so that average text is checked faster
         */

        /* check the combining order */
        cc=(int16_t)(fcd16>>8);
        if(cc!=0) {
            if(prevCC<0) {
                /* the previous character was <_NORM_MIN_WITH_LEAD_CC, we need to get its trail cc */
                prevCC=(int16_t)_getFCD16((UChar)-prevCC)&0xff;
            }

            if(cc<prevCC) {
                return FALSE;
            }
        }
        prevCC=(int16_t)fcd16&0xff;
    }
}

U_CAPI UNormalizationCheckResult U_EXPORT2
unorm_quickCheck(const UChar *src,
                 int32_t srcLength, 
                 UNormalizationMode mode, 
                 UErrorCode *pErrorCode) {
    const UChar *limit;
    uint32_t norm32, ccOrQCMask, qcMask;
    UChar c, c2, minNoMaybe;
    uint8_t cc, prevCC;
    UNormalizationCheckResult result;

    /* check arguments */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return UNORM_MAYBE;
    }

    if(src==NULL || srcLength<-1) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return UNORM_MAYBE;
    }

    if(!_haveData(*pErrorCode)) {
        return UNORM_MAYBE;
    }

    /* check for a valid mode and set the quick check minimum and mask */
    switch(mode) {
    case UNORM_NFC:
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFC_NO_MAYBE];
        qcMask=_NORM_QC_NFC;
        break;
    case UNORM_NFKC:
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFKC_NO_MAYBE];
        qcMask=_NORM_QC_NFKC;
        break;
    case UNORM_NFD:
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE];
        qcMask=_NORM_QC_NFD;
        break;
    case UNORM_NFKD:
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE];
        qcMask=_NORM_QC_NFKD;
        break;
    case UNORM_FCD:
        return unorm_checkFCD(src, srcLength) ? UNORM_YES : UNORM_NO;
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return UNORM_MAYBE;
    }

    /* initialize */
    ccOrQCMask=_NORM_CC_MASK|qcMask;
    result=UNORM_YES;
    prevCC=0;

    if(srcLength>=0) {
        /* string with length */
        limit=src+srcLength;
    } else /* srcLength==-1 */ {
        /* zero-terminated string */
        limit=NULL;
    }

    U_ALIGN_CODE(16);

    for(;;) {
        /* skip a run of code units below the minimum or with irrelevant data for the quick check */
        if(limit==NULL) {
            for(;;) {
                c=*src++;
                if(c<minNoMaybe) {
                    if(c==0) {
                        return result;
                    }
                } else if(((norm32=_getNorm32(c))&ccOrQCMask)!=0) {
                    break;
                }
                prevCC=0;
            }
        } else {
            for(;;) {
                if(src==limit) {
                    return result;
                } else if((c=*src++)>=minNoMaybe && ((norm32=_getNorm32(c))&ccOrQCMask)!=0) {
                    break;
                }
                prevCC=0;
            }
        }

        /* check one above-minimum, relevant code unit */
        if(isNorm32LeadSurrogate(norm32)) {
            /* c is a lead surrogate, get the real norm32 */
            if((limit==NULL || src!=limit) && UTF_IS_SECOND_SURROGATE(c2=*src)) {
                ++src;
                norm32=_getNorm32FromSurrogatePair(norm32, c2);
            } else {
                norm32=0;
            }
        }

        /* check the combining order */
        cc=(uint8_t)(norm32>>_NORM_CC_SHIFT);
        if(cc!=0 && cc<prevCC) {
            return UNORM_NO;
        }
        prevCC=cc;

        /* check for "no" or "maybe" quick check flags */
        norm32&=qcMask;
        if(norm32&_NORM_QC_ANY_NO) {
            return UNORM_NO;
        } else if(norm32!=0) {
            result=UNORM_MAYBE;
        }
    }

    return result;
}

/* make NFD & NFKD ---------------------------------------------------------- */

U_CFUNC int32_t
unorm_decompose(UChar *dest, int32_t destCapacity,
                const UChar *src, int32_t srcLength,
                UBool compat, UBool ignoreHangul,
                UGrowBuffer *growBuffer, void *context,
                UErrorCode *pErrorCode) {
    UChar buffer[3];
    const UChar *limit, *prevSrc, *p;
    UChar *reorderStart;
    uint32_t norm32, ccOrQCMask, qcMask;
    int32_t destIndex, length;
    UChar c, c2, minNoMaybe;
    uint8_t cc, prevCC, trailCC;
    UBool canGrow;

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    if(!compat) {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE];
        qcMask=_NORM_QC_NFD;
    } else {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE];
        qcMask=_NORM_QC_NFKD;
    }

    /* initialize */
    reorderStart=dest;
    ccOrQCMask=_NORM_CC_MASK|qcMask;
    destIndex=0;
    prevCC=0;

    /* avoid compiler warnings */
    norm32=0;
    c=0;

    /* do not attempt to grow if there is no growBuffer function or if it has failed before */
    canGrow=(UBool)(growBuffer!=NULL);

    if(srcLength>=0) {
        /* string with length */
        limit=src+srcLength;
    } else /* srcLength==-1 */ {
        /* zero-terminated string */
        limit=NULL;
    }

    U_ALIGN_CODE(16);

    for(;;) {
        /* count code units below the minimum or with irrelevant data for the quick check */
        prevSrc=src;
        if(limit==NULL) {
            while((c=*src)<minNoMaybe ? c!=0 : ((norm32=_getNorm32(c))&ccOrQCMask)==0) {
                prevCC=0;
                ++src;
            }
        } else {
            while(src!=limit && ((c=*src)<minNoMaybe || ((norm32=_getNorm32(c))&ccOrQCMask)==0)) {
                prevCC=0;
                ++src;
            }
        }

        /* copy these code units all at once */
        if(src!=prevSrc) {
            length=(int32_t)(src-prevSrc);
            if( (destIndex+length)<=destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity,
                                                limit==NULL ?
                                                    2*destCapacity+length+20 :
                                                    destCapacity+length+2*(limit-src)+20,
                                                destIndex))!=FALSE)
            ) {
                uprv_memcpy(dest+destIndex, prevSrc, length*U_SIZEOF_UCHAR);
            }
            destIndex+=length;
            reorderStart=dest+destIndex; /* not valid if dest==NULL */
        }

        /* end of source reached? */
        if(limit==NULL ? c==0 : src==limit) {
            break;
        }

        /* c already contains *src and norm32 is set for it, increment src */
        ++src;

        /* check one above-minimum, relevant code unit */
        /*
         * generally, set p and length to the decomposition string
         * in simple cases, p==NULL and (c, c2) will hold the length code units to append
         * in all cases, set cc to the lead and trailCC to the trail combining class
         * ### TODO say that c, c2 is either (BMP, 0) or (lead surr, trail surr) - for optimized single-char bubble sort
         */
        if(isNorm32HangulOrJamo(norm32)) {
            if(ignoreHangul) {
                c2=0;
                p=NULL;
                length=1;
            } else {
                /* Hangul syllable: decompose algorithmically */
                p=buffer;
                cc=trailCC=0;

                c-=HANGUL_BASE;

                c2=(UChar)(c%JAMO_T_COUNT);
                c/=JAMO_T_COUNT;
                if(c2>0) {
                    buffer[2]=(UChar)(JAMO_T_BASE+c2);
                    length=3;
                } else {
                    length=2;
                }

                buffer[1]=(UChar)(JAMO_V_BASE+c%JAMO_V_COUNT);
                buffer[0]=(UChar)(JAMO_L_BASE+c/JAMO_V_COUNT);
            }
        } else {
            if(isNorm32Regular(norm32)) {
                c2=0;
                length=1;
            } else {
                /* c is a lead surrogate, get the real norm32 */
                if((limit==NULL || src!=limit) && UTF_IS_SECOND_SURROGATE(c2=*src)) {
                    ++src;
                    length=2;
                    norm32=_getNorm32FromSurrogatePair(norm32, c2);
                } else {
                    c2=0;
                    length=1;
                    norm32=0;
                }
            }

            /* get the decomposition and the lead and trail cc's */
            if((norm32&qcMask)==0) {
                /* c does not decompose */
                cc=trailCC=(uint8_t)(norm32>>_NORM_CC_SHIFT);
                p=NULL;
            } else {
                /* c decomposes, get everything from the variable-length extra data */
                p=_decompose(norm32, qcMask, length, cc, trailCC);
                if(length==1) {
                    /* fastpath a single code unit from decomposition */
                    c=*p;
                    c2=0;
                    p=NULL;
                }
            }
        }

        /* append the decomposition to the destination buffer, assume length>0 */
        if( (destIndex+length)<=destCapacity ||
            /* attempt to grow the buffer */
            (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity,
                                            limit==NULL ?
                                                2*destCapacity+length+20 :
                                                destCapacity+length+2*(limit-src)+20,
                                            destIndex))!=FALSE)
        ) {
            UChar *reorderSplit=dest+destIndex;
            if(p==NULL) {
                /* fastpath: single code point */
                if(cc!=0 && cc<prevCC) {
                    /* (c, c2) is out of order with respect to the preceding text */
                    destIndex+=length;
                    trailCC=_insertOrdered(reorderStart, reorderSplit, dest+destIndex, c, c2, cc);
                } else {
                    /* just append (c, c2) */
                    dest[destIndex++]=c;
                    if(c2!=0) {
                        dest[destIndex++]=c2;
                    }
                }
            } else {
                /* general: multiple code points (ordered by themselves) from decomposition */
                if(cc!=0 && cc<prevCC) {
                    /* the decomposition is out of order with respect to the preceding text */
                    destIndex+=length;
                    trailCC=_mergeOrdered(reorderStart, reorderSplit, p, p+length);
                } else {
                    /* just append the decomposition */
                    do {
                        dest[destIndex++]=*p++;
                    } while(--length>0);
                }
            }
        } else {
            /* buffer overflow */
            /* keep incrementing the destIndex for preflighting */
            destIndex+=length;
        }

        prevCC=trailCC;
        if(prevCC==0) {
            reorderStart=dest+destIndex;
        }
    }

#if 1
    /* ### TODO: this passes the tests but seems weird */
    /* we may NUL-terminate if it fits as a convenience */
    if(destIndex<destCapacity) {
        dest[destIndex]=0;
    } else if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
#else
    /* ### TODO: this looks slightly to much more reasonable but fails some tests, esp. /tscoll/cmsccoll/TestIncrementalNormalize */
    if(limit==NULL) {
        /* assume that we must NUL-terminate */
        if(destIndex<destCapacity) {
            /* ### TODO: this one would make sense -- dest[destIndex++]=0; -- but the following is more compatible */
            dest[destIndex]=0;
        } else {
            /* ### TODO: same as above -- ++destIndex; */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    } else {
        /* we may NUL-terminate if it fits as a convenience */
        if(destIndex<destCapacity) {
            dest[destIndex]=0;
        } else if(destIndex>destCapacity) {
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    }
#endif

    return destIndex;
}

/* make FCD ----------------------------------------------------------------- */

static const UChar *
_findSafeFCD(const UChar *src, const UChar *limit, uint16_t fcd16) {
    UChar c, c2;

    /*
     * find the first position in [src..limit[ after some cc==0 according to FCD data
     *
     * at the beginning of the loop, we have fcd16 from before src
     *
     * stop at positions:
     * - after trail cc==0
     * - at the end of the source
     * - before lead cc==0
     */
    for(;;) {
        /* stop if trail cc==0 for the previous character */
        if((fcd16&0xff)==0) {
            break;
        }

        /* get c=*src - stop at end of string */
        if(limit==NULL) {
            c=*src;
            if(c==0) {
                break;
            }
        } else {
            if(src==limit) {
                break;
            }
            c=*src;
        }

        /* stop if lead cc==0 for this character */
        if(c<_NORM_MIN_WITH_LEAD_CC || (fcd16=_getFCD16(c))==0) {
            break;
        }

        if(!UTF_IS_FIRST_SURROGATE(c)) {
            if(fcd16<=0xff) {
                break;
            }
            ++src;
        } else if((limit==NULL || (src+1)!=limit) && (c2=*(src+1), UTF_IS_SECOND_SURROGATE(c2))) {
            /* c is a lead surrogate, get the real fcd16 */
            fcd16=_getFCD16FromSurrogatePair(fcd16, c2);
            if(fcd16<=0xff) {
                break;
            }
            src+=2;
        } else {
            /* c is an unpaired first surrogate, lead cc==0 */
            break;
        }
    }

    return src;
}

static uint8_t
_decomposeFCD(const UChar *src, const UChar *decompLimit, const UChar *limit,
              UChar *dest, int32_t &destIndex, int32_t &destCapacity,
              UBool canGrow, UGrowBuffer *growBuffer, void *context) {
    UChar *reorderStart;
    const UChar *p;
    uint32_t norm32;
    int32_t length;
    UChar c, c2;
    uint8_t cc, prevCC, trailCC;

    /*
     * canonically decompose [src..decompLimit[
     *
     * all characters in this range have some non-zero cc,
     * directly or in decomposition,
     * so that we do not need to check in the following for quick-check limits etc.
     *
     * there _are_ _no_ Hangul syllables or Jamos in here because they are FCD-safe (cc==0)!
     *
     * we also do not need to check for c==0 because we have an established decompLimit
     */
    reorderStart=dest+destIndex;
    prevCC=0;

    while(src<decompLimit) {
        c=*src++;
        norm32=_getNorm32(c);
        if(isNorm32Regular(norm32)) {
            c2=0;
            length=1;
        } else {
            /*
             * reminder: this function is called with [src..decompLimit[
             * not containing any Hangul/Jamo characters,
             * therefore the only specials are lead surrogates
             */
            /* c is a lead surrogate, get the real norm32 */
            if(src!=decompLimit && UTF_IS_SECOND_SURROGATE(c2=*src)) {
                ++src;
                length=2;
                norm32=_getNorm32FromSurrogatePair(norm32, c2);
            } else {
                c2=0;
                length=1;
                norm32=0;
            }
        }

        /* get the decomposition and the lead and trail cc's */
        if((norm32&_NORM_QC_NFD)==0) {
            /* c does not decompose */
            cc=trailCC=(uint8_t)(norm32>>_NORM_CC_SHIFT);
            p=NULL;
        } else {
            /* c decomposes, get everything from the variable-length extra data */
            p=_decompose(norm32, length, cc, trailCC);
            if(length==1) {
                /* fastpath a single code unit from decomposition */
                c=*p;
                c2=0;
                p=NULL;
            }
        }

        /* append the decomposition to the destination buffer, assume length>0 */
        if( (destIndex+length)<=destCapacity ||
            /* attempt to grow the buffer */
            (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity,
                                            limit==NULL ?
                                                2*destCapacity+length+20 :
                                                destCapacity+length+2*(limit-src)+20,
                                            destIndex))!=FALSE)
        ) {
            UChar *reorderSplit=dest+destIndex;
            if(p==NULL) {
                /* fastpath: single code point */
                if(cc!=0 && cc<prevCC) {
                    /* (c, c2) is out of order with respect to the preceding text */
                    destIndex+=length;
                    trailCC=_insertOrdered(reorderStart, reorderSplit, dest+destIndex, c, c2, cc);
                } else {
                    /* just append (c, c2) */
                    dest[destIndex++]=c;
                    if(c2!=0) {
                        dest[destIndex++]=c2;
                    }
                }
            } else {
                /* general: multiple code points (ordered by themselves) from decomposition */
                if(cc!=0 && cc<prevCC) {
                    /* the decomposition is out of order with respect to the preceding text */
                    destIndex+=length;
                    trailCC=_mergeOrdered(reorderStart, reorderSplit, p, p+length);
                } else {
                    /* just append the decomposition */
                    do {
                        dest[destIndex++]=*p++;
                    } while(--length>0);
                }
            }
        } else {
            /* buffer overflow */
            /* keep incrementing the destIndex for preflighting */
            destIndex+=length;
        }

        prevCC=trailCC;
        if(prevCC==0) {
            reorderStart=dest+destIndex;
        }
    }

    return prevCC;
}

/*
 * ### TODO:
 * try to use the previous two functions in incremental FCD in collation
 */

static int32_t
unorm_makeFCD(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              UGrowBuffer *growBuffer, void *context,
              UErrorCode *pErrorCode) {
    const UChar *limit, *prevSrc, *decompStart;
    int32_t destIndex, length;
    UChar c, c2;
    uint16_t fcd16;
    int16_t prevCC, cc;
    UBool canGrow;

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    /* initialize */
    decompStart=src;
    destIndex=0;
    prevCC=0;

    /* avoid compiler warnings */
    c=0;
    fcd16=0;

    /* do not attempt to grow if there is no growBuffer function or if it has failed before */
    canGrow=(UBool)(growBuffer!=NULL);

    if(srcLength>=0) {
        /* string with length */
        limit=src+srcLength;
    } else /* srcLength==-1 */ {
        /* zero-terminated string */
        limit=NULL;
    }

    U_ALIGN_CODE(16);

    for(;;) {
        /* skip a run of code units below the minimum or with irrelevant data for the FCD check */
        prevSrc=src;
        if(limit==NULL) {
            for(;;) {
                c=*src;
                if(c<_NORM_MIN_WITH_LEAD_CC) {
                    if(c==0) {
                        break;
                    }
                    prevCC=-(int16_t)c;
                } else if((fcd16=_getFCD16(c))==0) {
                    prevCC=0;
                } else {
                    break;
                }
                ++src;
            }
        } else {
            for(;;) {
                if(src==limit) {
                    break;
                } else if((c=*src)<_NORM_MIN_WITH_LEAD_CC) {
                    prevCC=-(int16_t)c;
                } else if((fcd16=_getFCD16(c))==0) {
                    prevCC=0;
                } else {
                    break;
                }
                ++src;
            }
        }

        /* copy these code units all at once */
        if(src!=prevSrc) {
            length=(int32_t)(src-prevSrc);
            if( (destIndex+length)<=destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity,
                                                limit==NULL ?
                                                    2*destCapacity+length+20 :
                                                    destCapacity+length+2*(limit-src)+20,
                                                destIndex))!=FALSE)
            ) {
                uprv_memcpy(dest+destIndex, prevSrc, length*U_SIZEOF_UCHAR);
            }
            destIndex+=length;
            prevSrc=src;
        }
        /* now prevSrc==src - used later to adjust destIndex before decomposition */

        /* end of source reached? */
        if(limit==NULL ? c==0 : src==limit) {
            break;
        }

        /*
         * prevCC has values from the following ranges:
         * 0..0xff - the previous trail combining class
         * <0      - the negative value of the previous code unit;
         *           that code unit was <_NORM_MIN_WITH_LEAD_CC and its _getFCD16()
         *           was deferred so that average text is checked faster
         */

        /* set a pointer to after the last source position where prevCC==0 */
        if(prevCC<0) {
            /* the previous character was <_NORM_MIN_WITH_LEAD_CC, we need to get its trail cc */
            prevCC=(int16_t)_getFCD16((UChar)-prevCC)&0xff;
            decompStart= prevCC==0 ? src : src-1;
        } else if(prevCC==0) {
            decompStart=src;
        /* else do not change decompStart */
        }

        /* c already contains *src and fcd16 is set for it, increment src */
        ++src;

        /* check one above-minimum, relevant code unit */
        if(UTF_IS_FIRST_SURROGATE(c)) {
            /* c is a lead surrogate, get the real fcd16 */
            if((limit==NULL || src!=limit) && UTF_IS_SECOND_SURROGATE(c2=*src)) {
                ++src;
                fcd16=_getFCD16FromSurrogatePair(fcd16, c2);
            } else {
                fcd16=0;
            }
        }

        /* we are looking at the character at [prevSrc..src[ */

        /* check the combining order */
        cc=(int16_t)(fcd16>>8);
        if(cc==0 || cc>=prevCC) {
            /* the order is ok */
            prevCC=(int16_t)fcd16&0xff;
        } else {
            /*
             * back out the part of the source that we copied already but
             * is now going to be decomposed;
             * prevSrc is set to after what was copied
             */
            destIndex-=(int32_t)(prevSrc-decompStart);

            /*
             * find the part of the source that needs to be decomposed;
             * to be safe and simple, decompose to before the next character with lead cc==0
             */
            src=_findSafeFCD(src, limit, fcd16);

            /*
             * the source text does not fulfill the conditions for FCD;
             * decompose and reorder a limited piece of the text
             */
            prevCC=_decomposeFCD(decompStart, src, limit,
                                 dest, destIndex, destCapacity,
                                 canGrow, growBuffer, context);
            decompStart=src;
        }
    }

#if 1
    /* ### TODO: this passes the tests but seems weird */
    /* we may NUL-terminate if it fits as a convenience */
    if(destIndex<destCapacity) {
        dest[destIndex]=0;
    } else if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
#else
    /* ### TODO: this looks slightly to much more reasonable but fails some tests, esp. /tscoll/cmsccoll/TestIncrementalNormalize */
    if(limit==NULL) {
        /* assume that we must NUL-terminate */
        if(destIndex<destCapacity) {
            /* ### TODO: this one would make sense -- dest[destIndex++]=0; -- but the following is more compatible */
            dest[destIndex]=0;
        } else {
            /* ### TODO: same as above -- ++destIndex; */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    } else {
        /* we may NUL-terminate if it fits as a convenience */
        if(destIndex<destCapacity) {
            dest[destIndex]=0;
        } else if(destIndex>destCapacity) {
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    }
#endif

    return destIndex;
}

/* make NFC & NFKC ---------------------------------------------------------- */

enum {
    _STACK_BUFFER_CAPACITY=100
};

/* get the composition properties of the next character */
inline uint32_t
_getNextCombining(UChar *&p, const UChar *limit,
                  UChar &c, UChar &c2,
                  uint16_t &combiningIndex, uint8_t &cc) {
    uint32_t norm32, combineFlags;

    c=*p++;
    norm32=_getNorm32(c);
    if((norm32&(_NORM_CC_MASK|_NORM_COMBINES_ANY))==0) {
        c2=0;
        combiningIndex=0;
        cc=0;
        return 0;
    } else {
        if(isNorm32Regular(norm32)) {
            c2=0;
        } else if(isNorm32HangulOrJamo(norm32)) {
            /* a compatibility decomposition contained Jamos */
            c2=0;
            combiningIndex=(uint16_t)(0xfff0|(norm32>>_NORM_EXTRA_SHIFT));
            cc=0;
            return norm32&_NORM_COMBINES_ANY;
        } else {
            /* c is a lead surrogate, get the real norm32 */
            if(p!=limit && UTF_IS_SECOND_SURROGATE(c2=*p)) {
                ++p;
                norm32=_getNorm32FromSurrogatePair(norm32, c2);
            } else {
                c2=0;
                combiningIndex=0;
                cc=0;
                return 0;
            }
        }

        combineFlags=norm32&_NORM_COMBINES_ANY;
        if(combineFlags!=0) {
            combiningIndex=*(_getExtraData(norm32)-1);
        }

        cc=(uint8_t)(norm32>>_NORM_CC_SHIFT);
        return combineFlags;
    }
}

/*
 * given a composition-result starter (c, c2) - which means its cc==0,
 * it combines forward, it has extra data, its norm32!=0,
 * it is not a Hangul or Jamo,
 * get just its combineFwdIndex
 *
 * norm32(c) is special if and only if c2!=0
 */
inline uint16_t
_getCombiningIndexFromStarter(UChar c, UChar c2) {
    uint32_t norm32;

    norm32=_getNorm32(c);
    if(c2!=0) {
        norm32=_getNorm32FromSurrogatePair(norm32, c2);
    }
    return *(_getExtraData(norm32)-1);
}

/*
 * Find the recomposition result for
 * a forward-combining character
 * (specified with a pointer to its part of the combiningTable[])
 * and a backward-combining character
 * (specified with its combineBackIndex).
 *
 * If these two characters combine, then set (value, value2)
 * with the code unit(s) of the composition character.
 *
 * Return value:
 * 0    do not combine
 * 1    combine
 * >1   combine, and the composition is a forward-combining starter
 *
 * See unormimp.h for a description of the composition table format.
 */
inline uint16_t
_combine(const uint16_t *table, uint16_t combineBackIndex,
         uint16_t &value, uint16_t &value2) {
    uint16_t key;

    /* search in the starter's composition table */
    for(;;) {
        key=*table++;
        if(key>=combineBackIndex) {
            break;
        }
        table+= *table&0x8000 ? 2 : 1;
    }

    /* mask off bit 15, the last-entry-in-the-list flag */
    if((key&0x7fff)==combineBackIndex) {
        /* found! combine! */
        value=*table;

        /* is the composition a starter that combines forward? */
        key=(value&0x2000)+1;

        /* get the composition result code point from the variable-length result value */
        if(value&0x8000) {
            if(value&0x4000) {
                /* surrogate pair composition result */
                value=(value&0x3ff)|0xd800;
                value2=*(table+1);
            } else {
                /* BMP composition result U+2000..U+ffff */
                value=*(table+1);
                value2=0;
            }
        } else {
            /* BMP composition result U+0000..U+1fff */
            value&=0x1fff;
            value2=0;
        }

        return key;
    } else {
        /* not found */
        return 0;
    }
}

/*
 * recompose the characters in [p..limit[ (which is canonically ordered),
 * adjust limit, and return the trailing cc
 *
 * since for NFKC we may get Jamos in decompositions, we need to
 * recompose those too
 *
 * note that recomposition never lengthens the text:
 * any character consists of either one or two code units;
 * a composition may contain at most one more code unit than the original starter,
 * while the combining mark that is removed has at least one code unit
 */
static uint8_t
_recompose(UChar *p, UChar *&limit) {
    UChar *starter, *pRemove, *q, *r;
    uint32_t combineFlags;
    UChar c, c2;
    uint16_t combineFwdIndex, combineBackIndex;
    uint16_t result, value, value2;
    uint8_t cc, prevCC;
    UBool starterIsSupplementary;

    starter=NULL;                   /* no starter */
    combineFwdIndex=0;              /* will not be used until starter!=NULL - avoid compiler warnings */
    starterIsSupplementary=FALSE;   /* will not be used until starter!=NULL - avoid compiler warnings */
    prevCC=0;

    for(;;) {
        combineFlags=_getNextCombining(p, limit, c, c2, combineBackIndex, cc);
        if((combineFlags&_NORM_COMBINES_BACK) && starter!=NULL) {
            if(combineBackIndex&0x8000) {
                /* c is a Jamo V/T, see if we can compose it with the previous character */
                pRemove=NULL; /* NULL while no Hangul composition */
                c2=*starter;
                if(combineBackIndex==0xfff2) {
                    /* Jamo V, compose with previous Jamo L and following Jamo T */
                    c2=(UChar)(c2-JAMO_L_BASE);
                    if(c2<JAMO_L_COUNT) {
                        pRemove=p-1;
                        c=(UChar)(HANGUL_BASE+(c2*JAMO_V_COUNT+(c-JAMO_V_BASE))*JAMO_T_COUNT);
                        if(p!=limit && (c2=(UChar)(*p-JAMO_T_BASE))<JAMO_T_COUNT) {
                            ++p;
                            c+=c2;
                        }
                        *starter=c;
                    }
                } else {
                    /* Jamo T, compose with previous Hangul that does not have a Jamo T */
                    if(isHangulWithoutJamoT(c2)) {
                        pRemove=p-1;
                        *starter=(UChar)(c2+(c-JAMO_T_BASE));
                    }
                }

                if(pRemove!=NULL) {
                    /* remove the Jamo(s) */
                    q=pRemove;
                    r=p;
                    while(r<limit) {
                        *q++=*r++;
                    }
                    p=pRemove;
                    limit=q;
                }

                c2=0; /* c2 held *starter temporarily */

                /*
                 * now: cc==0 and the combining index does not include "forward" ->
                 * the rest of the loop body will reset starter to NULL;
                 * technically, a composed Hangul syllable is a starter, but it
                 * does not combine forward now that we have consumed all eligible Jamos;
                 * for Jamo V/T, combineFlags does not contain _NORM_COMBINES_FWD
                 */

            } else if(
                /* the starter is not a Jamo V/T and */
                !(combineFwdIndex&0x8000) &&
                /* the combining mark is not blocked and */
                (prevCC<cc || prevCC==0) &&
                /* the starter and the combining mark (c, c2) do combine */
                0!=(result=_combine(combiningTable+combineFwdIndex, combineBackIndex, value, value2))
            ) {
                /* replace the starter with the composition, remove the combining mark */
                pRemove= c2==0 ? p-1 : p-2; /* pointer to the combining mark */

                /* replace the starter with the composition */
                *starter=(UChar)value;
                if(starterIsSupplementary) {
                    if(value2!=0) {
                        /* both are supplementary */
                        *(starter+1)=(UChar)value2;
                    } else {
                        /* the composition is shorter than the starter, move the intermediate characters forward one */
                        starterIsSupplementary=FALSE;
                        q=starter+1;
                        r=q+1;
                        while(r<pRemove) {
                            *q++=*r++;
                        }
                        --pRemove;
                    }
                } else if(value2!=0) {
                    /* the composition is longer than the starter, move the intermediate characters back one */
                    starterIsSupplementary=TRUE;
                    ++starter; /* temporarily increment for the loop boundary */
                    q=pRemove;
                    r=++pRemove;
                    while(starter<q) {
                        *--r=*--q;
                    }
                    *starter=(UChar)value2;
                    --starter; /* undo the temporary increment */
                /* } else { both are on the BMP, nothing more to do */
                }

                /* remove the combining mark by moving the following text over it */
                if(pRemove<p) {
                    q=pRemove;
                    r=p;
                    while(r<limit) {
                        *q++=*r++;
                    }
                    p=pRemove;
                    limit=q;
                }

                /* keep prevCC because we removed the combining mark */

                /* done? */
                if(p==limit) {
                    return prevCC;
                }

                /* is the composition a starter that combines forward? */
                if(result>1) {
                    combineFwdIndex=_getCombiningIndexFromStarter((UChar)value, (UChar)value2);
                } else {
                    starter=NULL;
                }

                /* we combined and set prevCC, continue with looking for compositions */
                continue;
            }
        }

        /* no combination this time */
        prevCC=cc;
        if(p==limit) {
            return prevCC;
        }

        /* if (c, c2) did not combine, then check if it is a starter */
        if(cc==0) {
            /* found a new starter */
            if(combineFlags&_NORM_COMBINES_FWD) {
                /* it may combine with something, prepare for it */
                if(c2==0) {
                    starterIsSupplementary=FALSE;
                    starter=p-1;
                } else {
                    starterIsSupplementary=TRUE;
                    starter=p-2;
                }
                combineFwdIndex=combineBackIndex;
            } else {
                /* it will not combine with anything */
                starter=NULL;
            }
        }
    }
}

/*
 * get a norm32 from text with complete code points
 * (like from decompositions)
 */
inline uint32_t
_getNorm32(const UChar *p, uint32_t mask) {
    uint32_t norm32=_getNorm32(*p);
    if((norm32&mask) && isNorm32LeadSurrogate(norm32)) {
        /* *p is a lead surrogate, get the real norm32 */
        norm32=_getNorm32FromSurrogatePair(norm32, *(p+1));
    }
    return norm32;
}

/*
 * read and decompose the following character
 * return NULL if it is (or its decomposition starts with) a starter (cc==0)
 * that has NF*C "yes"
 * otherwise, return its decomposition (and set length, cc, and trailCC)
 */
static const UChar *
_decomposeBeforeNextStarter(const UChar *&src, const UChar *limit,
                            uint32_t qcMask, uint32_t decompQCMask, UChar minNoMaybe,
                            uint8_t &cc, uint8_t &trailCC,
                            int32_t &length) {
    const UChar *p;
    uint32_t norm32;
    UChar c, c2;

    /* end of string? get c */
    if(limit==NULL) {
        c=*src;
        if(c==0) {
            return NULL;
        }
    } else {
        if(src==limit) {
            return NULL;
        }
        c=*src;
    }

    /* anything to be done? */
    if(c<minNoMaybe) {
        return NULL;
    }
    norm32=_getNorm32(c);
    if((norm32&(_NORM_CC_MASK|qcMask|decompQCMask))==0) {
        return NULL;
    }

    if(isNorm32HangulOrJamo(norm32)) {
        if(isHangulJamoNorm32HangulOrJamoL(norm32)) {
            /* Hangul decomposes but is all starters, Jamo L are starters */
            return NULL;
        }

        /* Jamo V/T are not starters but cc==0 */
        cc=trailCC=0;
        length=1;
        return src++;
    }

    if(isNorm32Regular(norm32)) {
        c2=0;
        length=1;
    } else {
        /* c is a lead surrogate, get the real norm32 */
        if((limit==NULL || (src+1)!=limit) && UTF_IS_SECOND_SURROGATE(c2=*(src+1))) {
            length=2;
            norm32=_getNorm32FromSurrogatePair(norm32, c2);
        } else {
            return NULL;
        }
    }

    /* get the decomposition and the lead and trail cc's */
    if((norm32&decompQCMask)==0) {
        /* c does not decompose */
        cc=trailCC=(uint8_t)(norm32>>_NORM_CC_SHIFT);
        p=src;
    } else {
        /* c decomposes, get everything from the variable-length extra data */
        p=_decompose(norm32, decompQCMask, length, cc, trailCC);
        if(cc==0) {
            /* get the first character's norm32 to check if it is a starter with qc "no" or "maybe" */
            norm32=_getNorm32(p, qcMask);
        }
    }

    if(cc==0 && !(norm32&qcMask)) {
        return NULL;
    } else {
        src+= c2==0 ? 1 : 2;
        return p;
    }
}

/*
 * decompose the previous code point (needs start<src)
 * set starterIndex>=0 to the last starter in the decomposition
 * that has NF*C "yes"
 * starterIndex==-1 if there is no starter
 */
static const UChar *
_decomposeBackFindStarter(const UChar *start, const UChar *&src,
                          uint32_t qcMask, uint32_t decompQCMask, UChar minNoMaybe,
                          int32_t &starterIndex,
                          int32_t &length) {
    const UChar *p;
    uint32_t norm32;
    UChar c, c2;
    uint8_t cc, trailCC;

    c=*--src;
    length=1;
    starterIndex=0; /* many characters are themselves starters */

    /* check for a surrogate before getting norm32 to see if we need to predecrement further */
    if(c<minNoMaybe) {
        return src;
    } else if(!UTF_IS_SURROGATE(c)) {
        norm32=_getNorm32(c);
        if(isNorm32HangulOrJamo(norm32)) {
            /* Hangul decomposes but is all starters, Jamo L are starters */
#if 0
            /*
             * this is commented out because
             * we should never get Jamo V/T here because
             * we go back through quick check "yes" text
             * and Jamo V/T have NFC_MAYBE
             */
            if(!isHangulJamoNorm32HangulOrJamoL(norm32)) {
                /* Jamo V/T are not starters */
                starterIndex=-1;
            }
#endif
            return src;
        }
    } else if(UTF_IS_SURROGATE_FIRST(c)) {
        /* unpaired first surrogate */
        return src;
    } else if(src!=start && UTF_IS_FIRST_SURROGATE(c2=*(src-1))) {
        --src;
        length=2;
        norm32=_getNorm32(c2);

        if((norm32&(_NORM_CC_MASK|decompQCMask))==0) {
            /* all surrogate pairs with this lead surrogate have cc==0 and no decomposition */
            return src;
        } else {
            /* norm32 must be a surrogate special */
            norm32=_getNorm32FromSurrogatePair(norm32, c);
        }
    } else {
        /* unpaired second surrogate */
        return src;
    }

    /* get the decomposition and the lead and trail cc's */
    if((norm32&decompQCMask)==0) {
        /* c does not decompose */
        if((norm32&(_NORM_CC_MASK|qcMask))!=0) {
            starterIndex=-1;
        }
        p=src;
    } else {
        /* c decomposes, get everything from the variable-length extra data */
        p=_decompose(norm32, decompQCMask, length, cc, trailCC);

        /* find the starterIndex (the decomposition is canonically ordered!) */
        /* assume that the decomposition contains complete code points */
        if(UTF_IS_SECOND_SURROGATE(p[length-1])) {
            starterIndex=length-2;
        } else {
            starterIndex=length-1;
        }
        if(trailCC!=0 || (_getNorm32(p+starterIndex, qcMask)&qcMask)) {
            /* search backwards */
            for(;;) {
                if(starterIndex==0) {
                    starterIndex=-1;
                    break;
                }
                c=p[--starterIndex];
                if(UTF_IS_SECOND_SURROGATE(c)) {
                    c2=p[--starterIndex];
                    norm32=_getNorm32(c2);
                    if((norm32&(_NORM_CC_MASK|qcMask))==0) {
                        /* all surrogate pairs with this lead surrogate have cc==0 */
                        break;
                    } else {
                        /* norm32 must be a surrogate special */
                        norm32=_getNorm32FromSurrogatePair(norm32, c);
                    }
                } else {
                    norm32=_getNorm32(c);
                }
                if((norm32&(_NORM_CC_MASK|qcMask))==0) {
                    break;
                }
            }
        }
    }

    return p;
}

/*
 * recompose around the current character:
 * this function is called when unorm_decompose() finds a quick check "no" or "maybe"
 * after some text (with quick check "yes") has been copied already
 *
 * decompose this character as well as parts of the source surrounding it,
 * find the previous and the next starter,
 * and then recompose between these two starters
 */
static const UChar *
_composePart(UChar *stackBuffer, UChar *&buffer, int32_t &bufferCapacity, int32_t &length,
             const UChar *&prevStarter, const UChar *prevSrc, const UChar *src, const UChar *limit,
             uint32_t norm32,
             uint32_t qcMask, uint8_t &prevCC,
             int32_t &destIndex,
             UErrorCode *pErrorCode) {
    const UChar *p, *starter;
    UChar *reorderSplit, *recomposeLimit;
    uint32_t decompQCMask;
    int32_t startIndex, limitIndex, firstStarterIndex, starterIndex;
    UChar minNoMaybe;
    uint8_t cc, trailCC;

    decompQCMask=(qcMask<<2)&0xf; /* decomposition quick check mask */

    if(!(decompQCMask&_NORM_QC_NFKD)) {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE];
    } else {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE];
    }

    /* get the decomposition and the lead and trail cc's */
    if((norm32&decompQCMask)==0) {
        /* c does not decompose */
        cc=trailCC=(uint8_t)(norm32>>_NORM_CC_SHIFT);
        p=prevSrc;
    } else {
        /* c decomposes, get everything from the variable-length extra data */
        p=_decompose(norm32, decompQCMask, length, cc, trailCC);
        if(cc==0) {
            /* get the first character's norm32 to check if it is a starter with qc "no" or "maybe" */
            norm32=_getNorm32(p, qcMask);
        }
    }

    /* copy the decomposition into the buffer, assume that it fits */
    startIndex=limitIndex=bufferCapacity/2;
    do {
        buffer[limitIndex++]=*p++;
    } while(--length>0);

    /* find the last starter in [prevStarter..src[ including this new decomposition */
    if((cc==0 && !(norm32&qcMask)) || prevStarter==prevSrc) {
        prevCC=trailCC;
        starter=prevSrc;
        firstStarterIndex=startIndex;
    } else {
        /*
         * ### TODO
         * - verify that prevStarter is indeed at the _last_ starter before prevSrc
         * - if that is so, then perform a normal decomposition on [prevStarter..src[
         *   instead of this special, incremental one
         */

        /* decompose backwards and look for a starter */
        firstStarterIndex=0;
        starter=prevSrc;
        for(;;) {
            p=_decomposeBackFindStarter(prevStarter, starter,
                                        qcMask, decompQCMask, minNoMaybe,
                                        starterIndex, length);

            /* make sure there is enough space in the buffer */
            if(startIndex<length) {
                int32_t bufferLength;

                if(!u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity, 2*bufferCapacity, limitIndex)) {
                    *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                    return NULL;
                }

                /* move the current buffer contents up */
                bufferLength=limitIndex-startIndex;
                limitIndex=bufferCapacity-_STACK_BUFFER_CAPACITY/2;
                uprv_memmove(buffer+(limitIndex-bufferLength), buffer+startIndex, bufferLength*U_SIZEOF_UCHAR);
                startIndex=limitIndex-bufferLength;
            }

            /* prepend the decomposition */
            p+=length;
            do {
                buffer[--startIndex]=*--p;
            } while(--length>0);

            /* stop if we found a starter */
            if(starterIndex>=0) {
                firstStarterIndex=startIndex+starterIndex;
                break;
            }

            /* stop if we are at the beginning of the text */
            if(prevStarter>=starter) {
                firstStarterIndex=startIndex;
                break;
            }
        }

        /* reorder the backwards decomposition, set prevCC */
        reorderSplit=buffer+firstStarterIndex;
        prevCC=_mergeOrdered(reorderSplit, reorderSplit, reorderSplit, buffer+limitIndex, FALSE);

        /* adjust destIndex: back out what had been copied with qc "yes" */
        destIndex-=(int32_t)(prevSrc-starter);
    }

    /* find the next starter in [src..limit[ */
    for(;;) {
        p=_decomposeBeforeNextStarter(src, limit, qcMask, decompQCMask, minNoMaybe, cc, trailCC, length);
        if(p==NULL) {
            break; /* reached a starter */
        }

        /* make sure there is enough space in the buffer */
        if((limitIndex+length)>bufferCapacity) {
            if(startIndex>=length) {
                /* it fits if we move the buffer contents up */
                uprv_memmove(buffer, buffer+startIndex, (limitIndex-startIndex)*U_SIZEOF_UCHAR);
                firstStarterIndex-=startIndex;
                limitIndex-=startIndex;
                startIndex=0;
            } else if(!u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity, 2*bufferCapacity, limitIndex)) {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                return NULL;
            }
        }

        if(cc!=0 && cc<prevCC) {
            /* the decomposition is out of order with respect to the preceding text */
            reorderSplit=buffer+limitIndex;
            limitIndex+=length;
            if(length==1) {
                prevCC=_insertOrdered(buffer+firstStarterIndex, reorderSplit, buffer+limitIndex, *p, 0, cc);
            } else {
                prevCC=_mergeOrdered(buffer+firstStarterIndex, reorderSplit, p, p+length);
            }
        } else {
            /* just append the decomposition */
            do {
                buffer[limitIndex++]=*p++;
            } while(--length>0);
            prevCC=trailCC;
        }
    }

    /* recompose between the two starters */
    recomposeLimit=buffer+limitIndex;
    if((limitIndex-firstStarterIndex)>=2) {
        prevCC=_recompose(buffer+firstStarterIndex, recomposeLimit);
    }

    /* set output parameters and return with a pointer to the recomposition */
    prevStarter=src;
    p=buffer+startIndex;
    length=recomposeLimit-p;
    return p;
}

U_CFUNC int32_t
unorm_compose(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              UBool compat, UBool /* ### TODO: need to do this? -- ignoreHangul -- ### */,
              UGrowBuffer *growBuffer, void *context,
              UErrorCode *pErrorCode) {
    UChar stackBuffer[_STACK_BUFFER_CAPACITY];
    UChar *buffer;
    int32_t bufferCapacity;

    const UChar *limit, *prevSrc, *reorderStart, *prevStarter;
    uint32_t norm32, ccOrQCMask, qcMask;
    int32_t destIndex, length;
    UChar c, c2, minNoMaybe;
    uint8_t cc, prevCC;
    UBool canGrow;

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    if(!compat) {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFC_NO_MAYBE];
        qcMask=_NORM_QC_NFC;
    } else {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFKC_NO_MAYBE];
        qcMask=_NORM_QC_NFKC;
    }

    /* initialize */
    buffer=stackBuffer;
    bufferCapacity=_STACK_BUFFER_CAPACITY;

    /*
     * prevStarter points to the last character before the current one
     * that is a "true" starter with cc==0 and quick check "yes".
     */
    prevStarter=src;

    reorderStart=dest;
    ccOrQCMask=_NORM_CC_MASK|qcMask;
    destIndex=0;
    prevCC=0;

    /* avoid compiler warnings */
    norm32=0;
    c=0;

    /* do not attempt to grow if there is no growBuffer function or if it has failed before */
    canGrow=(UBool)(growBuffer!=NULL);

    if(srcLength>=0) {
        /* string with length */
        limit=src+srcLength;
    } else /* srcLength==-1 */ {
        /* zero-terminated string */
        limit=NULL;
    }

    U_ALIGN_CODE(16);

    for(;;) {
        /* count code units below the minimum or with irrelevant data for the quick check */
        prevSrc=src;
        if(limit==NULL) {
            while((c=*src)<minNoMaybe ? c!=0 : ((norm32=_getNorm32(c))&ccOrQCMask)==0) {
                prevCC=0;
                ++src;
            }
        } else {
            while(src!=limit && ((c=*src)<minNoMaybe || ((norm32=_getNorm32(c))&ccOrQCMask)==0)) {
                prevCC=0;
                ++src;
            }
        }

        /* copy these code units all at once */
        if(src!=prevSrc) {
            length=(int32_t)(src-prevSrc);
            if( (destIndex+length)<=destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity,
                                                limit==NULL ?
                                                    2*destCapacity+length+20 :
                                                    destCapacity+length+2*(limit-src)+20,
                                                destIndex))!=FALSE)
            ) {
                uprv_memcpy(dest+destIndex, prevSrc, length*U_SIZEOF_UCHAR);
            }
            destIndex+=length;
            reorderStart=dest+destIndex; /* not valid if dest==NULL */

            /* set prevStarter to the last character in the quick check loop */
            prevStarter=src-1;
            if(UTF_IS_SECOND_SURROGATE(*prevStarter) && prevSrc<prevStarter && UTF_IS_FIRST_SURROGATE(*(prevStarter-1))) {
                --prevStarter;
            }

            prevSrc=src;
        }

        /* end of source reached? */
        if(limit==NULL ? c==0 : src==limit) {
            break;
        }

        /* c already contains *src and norm32 is set for it, increment src */
        ++src;

        /*
         * source buffer pointers:
         *
         *  all done      quick check   current char  not yet
         *                "yes" but     (c, c2)       processed
         *                may combine
         *                forward
         * [-------------[-------------[-------------[-------------[
         * |             |             |             |             |
         * start         prevStarter   prevSrc       src           limit
         *
         *
         * destination buffer pointers and indexes:
         *
         *  all done      might take    not filled yet
         *                characters for
         *                reordering
         * [-------------[-------------[-------------[
         * |             |             |             |
         * dest          reorderStart  destIndex     destCapacity
         */

        /* check one above-minimum, relevant code unit */
        /*
         * norm32 is for c=*(src-1), and the quick check flag is "no" or "maybe", and/or cc!=0
         * check for Jamo V/T, then for surrogates and regular characters
         * c is not a Hangul syllable because they are not marked with no/maybe for NFC & NFKC (and their cc==0)
         */
        if(isNorm32HangulOrJamo(norm32)) {
            /*
             * Jamo V/T:
             * try to compose with the previous character, Jamo V also with a following Jamo T,
             * and set values here right now in case we just continue with the main loop
             */
            length=1;
            prevCC=cc=0;
            prevStarter=prevSrc;
            reorderStart=dest+destIndex;

            if(/* ### TODO: do we need to do this? !ignoreHangul && ### */ destIndex>0) {
                /* c is a Jamo V/T, see if we can compose it with the previous character */
                c2=*(prevSrc-1);
                if(isJamoVTNorm32JamoV(norm32)) {
                    /* Jamo V, compose with previous Jamo L and following Jamo T */
                    c2=(UChar)(c2-JAMO_L_BASE);
                    if(c2<JAMO_L_COUNT) {
                        c=(UChar)(HANGUL_BASE+(c2*JAMO_V_COUNT+(c-JAMO_V_BASE))*JAMO_T_COUNT);
                        if((limit==NULL || src!=limit) && (c2=(UChar)(*src-JAMO_T_BASE))<JAMO_T_COUNT) {
                            ++src;
                            c+=c2;
                        }
                        if(destIndex<=destCapacity) {
                            dest[destIndex-1]=c;
                        }
                        continue;
                    }
                } else {
                    /* Jamo T, compose with previous Hangul that does not have a Jamo T */
                    if(isHangulWithoutJamoT(c2)) {
                        if(destIndex<=destCapacity) {
                            dest[destIndex-1]=(UChar)(c2+(c-JAMO_T_BASE));
                        }
                        continue;
                    }
                }
            }
            c2=0;
        } else {
            if(isNorm32Regular(norm32)) {
                c2=0;
                length=1;
            } else {
                /* c is a lead surrogate, get the real norm32 */
                if((limit==NULL || src!=limit) && UTF_IS_SECOND_SURROGATE(c2=*src)) {
                    ++src;
                    length=2;
                    norm32=_getNorm32FromSurrogatePair(norm32, c2);
                } else {
                    /* c is an unpaired lead surrogate, nothing to do */
                    c2=0;
                    length=1;
                    norm32=0;
                }
            }

            /* we are looking at the character (c, c2) at [prevSrc..src[ */
            if((norm32&qcMask)==0) {
                cc=(uint8_t)(norm32>>_NORM_CC_SHIFT);
            } else {
                const UChar *p;

                /* ### TODO use sidebuffer because intermediate result might not fit but end result might - also rework some of dest buffer */
                p=_composePart(stackBuffer, buffer, bufferCapacity, length,
                               prevStarter,     /* in/out, will be set to the following true starter */
                               prevSrc, src, limit,
                               norm32,
                               qcMask,
                               prevCC,          /* output */
                               destIndex,       /* will be adjusted */
                               pErrorCode);

                if(p==NULL) {
                    return 0;   /* an error occurred (out of memory) */
                }

                /* append the recomposed buffer contents to the destination buffer */
                if( (destIndex+length)<=destCapacity ||
                    /* attempt to grow the buffer */
                    (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity,
                                                    limit==NULL ?
                                                        2*destCapacity+length+20 :
                                                        destCapacity+length+2*(limit-src)+20,
                                                    destIndex))!=FALSE)
                ) {
                    while(length>0) {
                        dest[destIndex++]=*p++;
                        --length;
                    }
                } else {
                    /* buffer overflow */
                    /* keep incrementing the destIndex for preflighting */
                    destIndex+=length;
                }

                src=prevStarter;
                continue;
            }
        }

        /* append the single code point (c, c2) to the destination buffer */
        if( (destIndex+length)<=destCapacity ||
            /* attempt to grow the buffer */
            (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity,
                                            limit==NULL ?
                                                2*destCapacity+length+20 :
                                                destCapacity+length+2*(limit-src)+20,
                                            destIndex))!=FALSE)
        ) {
            if(cc!=0 && cc<prevCC) {
                /* (c, c2) is out of order with respect to the preceding text */
                UChar *reorderSplit=dest+destIndex;
                destIndex+=length;
                prevCC=_insertOrdered(reorderStart, reorderSplit, dest+destIndex, c, c2, cc);
            } else {
                /* just append (c, c2) */
                dest[destIndex++]=c;
                if(c2!=0) {
                    dest[destIndex++]=c2;
                }
                prevCC=cc;
            }
        } else {
            /* buffer overflow */
            /* keep incrementing the destIndex for preflighting */
            destIndex+=length;
            prevCC=cc;
        }
    }

#if 1
    /* ### TODO: this passes the tests but seems weird */
    /* we may NUL-terminate if it fits as a convenience */
    if(destIndex<destCapacity) {
        dest[destIndex]=0;
    } else if(destIndex>destCapacity) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
#else
    /* ### TODO: this looks slightly to much more reasonable but fails some tests, esp. /tscoll/cmsccoll/TestIncrementalNormalize */
    if(limit==NULL) {
        /* assume that we must NUL-terminate */
        if(destIndex<destCapacity) {
            /* ### TODO: this one would make sense -- dest[destIndex++]=0; -- but the following is more compatible */
            dest[destIndex]=0;
        } else {
            /* ### TODO: same as above -- ++destIndex; */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    } else {
        /* we may NUL-terminate if it fits as a convenience */
        if(destIndex<destCapacity) {
            dest[destIndex]=0;
        } else if(destIndex>destCapacity) {
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    }
#endif

    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return destIndex;
}

/*
 ### TODO
 task items:
 - 2.0 Java sample code from unicode.org compare vs. JNI around C implementation - do monkey test
 - 2.1 port that sample code to C/C++ and run as part of regular test suite
 */

/* normalize() API ---------------------------------------------------------- */

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
                        UErrorCode *pErrorCode) {
    switch(mode) {
    case UNORM_NFD:
        return unorm_decompose(dest, destCapacity,
                               src, srcLength,
                               FALSE, ignoreHangul,
                               growBuffer, context,
                               pErrorCode);
    case UNORM_NFKD:
        return unorm_decompose(dest, destCapacity,
                               src, srcLength,
                               TRUE, ignoreHangul,
                               growBuffer, context,
                               pErrorCode);
    case UNORM_NFC:
        return unorm_compose(dest, destCapacity,
                             src, srcLength,
                             FALSE, ignoreHangul,
                             growBuffer, context,
                             pErrorCode);
    case UNORM_NFKC:
        return unorm_compose(dest, destCapacity,
                             src, srcLength,
                             TRUE, ignoreHangul,
                             growBuffer, context,
                             pErrorCode);
    case UNORM_FCD:
        return unorm_makeFCD(dest, destCapacity,
                             src, srcLength,
                             growBuffer, context,
                             pErrorCode);
    case UNORM_NONE:
        /* just copy the string */
        if(srcLength==-1) {
            srcLength=u_strlen(src);
        }
        if( srcLength<=destCapacity ||
            /* attempt to grow the buffer */
            (growBuffer!=NULL && growBuffer(context, &dest, &destCapacity, srcLength+1, 0))
        ) {
            uprv_memcpy(dest, src, srcLength*U_SIZEOF_UCHAR);
            /* ### TODO: revise NUL-termination parallel to rest of API */
            /* we may NUL-terminate if it fits as a convenience */
            if(srcLength<destCapacity) {
                dest[srcLength]=0;
            } else if(srcLength>destCapacity) {
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            }
        }
        /* ### TODO: revise NUL-termination parallel to rest of API */
        return srcLength;
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
}

/** Public API for normalizing. */
U_CAPI int32_t
unorm_normalize(const UChar *src, int32_t srcLength,
                UNormalizationMode mode, int32_t option,
                UChar *dest, int32_t destCapacity,
                UErrorCode *pErrorCode) {
    /* check argument values */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( destCapacity<0 || (dest==NULL && destCapacity>0) ||
        src==NULL || srcLength<-1
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* check for overlapping src and destination */
    /* ### TODO: real API may provide a temp buffer */
    if( (src>=dest && src<(dest+destCapacity)) ||
        (srcLength>0 && dest>=src && dest<(src+srcLength))
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    return unorm_internalNormalize(dest, destCapacity,
                                   src, srcLength,
                                   mode, (UBool)((option&UNORM_IGNORE_HANGUL)!=0),
                                   NULL, NULL,
                                   pErrorCode);
}

/* iteration functions ------------------------------------------------------ */

/*
 * These iteration functions are the core implementations of the
 * Normalizer class iteration API.
 * They read from a CharacterIterator into their own buffer
 * and normalize into the Normalizer iteration buffer.
 * Normalizer itself then iterates over its buffer until that needs to be
 * filled again.
 */

/*
 * Read backwards and check if the combining class is 0.
 * If c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!).
 */
inline UBool
_isPrevCCZero(CharacterIterator &src, UChar &c, UChar &c2) {
    uint32_t norm32;

    /* need src.hasPrevious() */
    c=src.previous();
    c2=0;

    /* check for a surrogate before getting norm32 to see if we need to predecrement further */
    if(!UTF_IS_SURROGATE(c)) {
        return (_getNorm32(c)&_NORM_CC_MASK)==0;
    } else if(UTF_IS_SURROGATE_FIRST(c) || !src.hasPrevious()) {
        /* unpaired surrogate */
        return TRUE;
    } else if(UTF_IS_FIRST_SURROGATE(c2=src.previous())) {
        norm32=_getNorm32(c2);
        if((norm32&_NORM_CC_MASK)==0) {
            /* all surrogate pairs with this lead surrogate have cc==0 */
            return TRUE;
        } else {
            /* norm32 must be a surrogate special */
            return (_getNorm32FromSurrogatePair(norm32, c)&_NORM_CC_MASK)==0;
        }
    } else {
        /* unpaired second surrogate, undo the c2=src.previous() movement */
        src.move(1, CharacterIterator::kCurrent);
        return TRUE;
    }
}

/*
 * Helper functions:
 * Read from the CharacterIterator until a normalization boundary is reached.
 */

static int32_t
_findNextDecomposeBoundary(CharacterIterator &src,
                           UChar *&buffer, int32_t bufferCapacity,
                           UErrorCode *pErrorCode) {
    UChar *stackBuffer;
    uint32_t norm32;
    int32_t bufferIndex;
    UChar c, c2;

    if(!src.hasNext()) {
        return 0;
    }

    /* initialize */
    stackBuffer=buffer;

    /* get one character, ignore its cc, and keep c filled with a one-UChar look-ahead */
    buffer[0]=c=src.current();
    bufferIndex=1;
    c2=src.next();
    if(UTF_IS_FIRST_SURROGATE(c) && UTF_IS_SECOND_SURROGATE(c2)) {
        buffer[bufferIndex++]=c2;
        c=src.next();
    } else {
        c=c2;
    }

    /* get all following characters with non-zero combining class */
    /* checking hasNext() instead of c!=DONE on the off-chance that U+ffff is part of the string */
    while(src.hasNext()) {
        /* get the cc of the next character, src.getIndex() is at c */
        norm32=_getNorm32(c);
        if((norm32&_NORM_CC_MASK)==0) {
            break;
        }

        if(!isNorm32LeadSurrogate(norm32)) {
            /* BMP character with cc!=0, write c into the buffer */
            if( bufferIndex<bufferCapacity ||
                /* attempt to grow the buffer */
                u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity,
                                       2*bufferCapacity,
                                       bufferIndex)
            ) {
                buffer[bufferIndex++]=c;
            } else {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                return 0;
            }
        } else {
            /* c is a lead surrogate, get the real norm32 */
            if(UTF_IS_SECOND_SURROGATE(c2=src.next())) {
                norm32=_getNorm32FromSurrogatePair(norm32, c2);
            } else {
                norm32=0;
            }
            if((norm32&_NORM_CC_MASK)==0) {
                /* cc(supplementary)==0, back out the c2=src.next() movement to stop at the first surrogate */
                src.move(-1, CharacterIterator::kCurrent);
                break;
            }

            /* supplementary character with cc!=0, write (c, c2) into the buffer */
            if( (bufferIndex+2)<=bufferCapacity ||
                /* attempt to grow the buffer */
                u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity,
                                       2*bufferCapacity,
                                       bufferIndex)
            ) {
                buffer[bufferIndex++]=c;
                buffer[bufferIndex++]=c2;
            } else {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                return 0;
            }
        }

        /* get the next character (pre-increment) */
        c=src.next();
    }

    /* return the length of the buffer contents */
    return bufferIndex;
}

static int32_t
_findPreviousDecomposeBoundary(CharacterIterator &src,
                               UChar *&buffer, int32_t bufferCapacity,
                               int32_t &startIndex,
                               UErrorCode *pErrorCode) {
    UChar *stackBuffer;
    UChar c, c2;
    UBool isZero;

    /* initialize */
    stackBuffer=buffer;
    startIndex=bufferCapacity; /* fill the buffer from the end backwards */

    while(src.hasPrevious()) {
        isZero=_isPrevCCZero(src, c, c2);

        /* always write this character to the front of the buffer */
        /* make sure there is enough space in the buffer */
        if(startIndex < (c2==0 ? 1 : 2)) {
            int32_t bufferLength=bufferCapacity;

            if(!u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity, 2*bufferCapacity, bufferLength)) {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                return 0;
            }

            /* move the current buffer contents up */
            uprv_memmove(buffer+(bufferCapacity-bufferLength), buffer, bufferLength*U_SIZEOF_UCHAR);
            startIndex+=bufferCapacity-bufferLength;
        }

        buffer[--startIndex]=c;
        if(c2!=0) {
            buffer[--startIndex]=c2;
        }

        /* stop if this just-copied character has cc==0 */
        if(isZero) {
            break;
        }
    }

    /* return the length of the buffer contents */
    return bufferCapacity-startIndex;
}

/*
 * Iteration functions:
 * Collect a minimum amount of text from the character iterator,
 * normalize into the destination buffer,
 * and return the length of the output.
 */

U_CFUNC int32_t
unorm_nextDecompose(UChar *dest, int32_t destCapacity,
                    CharacterIterator &src,
                    UBool compat, UBool ignoreHangul,
                    UGrowBuffer *growBuffer, void *context,
                    UErrorCode *pErrorCode) {
    UChar stackBuffer[40];
    UChar *buffer;
    int32_t bufferLength, destLength;

    buffer=stackBuffer;
    bufferLength=_findNextDecomposeBoundary(src,
                                            buffer, sizeof(stackBuffer)/U_SIZEOF_UCHAR,
                                            pErrorCode);
    if(bufferLength>0) {
        destLength=unorm_decompose(dest, destCapacity,
                                   buffer, bufferLength,
                                   compat, ignoreHangul,
                                   growBuffer, context, pErrorCode);
    } else {
        destLength=0;
    }

    /* cleanup */
    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return destLength;
}

U_CFUNC int32_t
unorm_prevDecompose(UChar *dest, int32_t destCapacity,
                    CharacterIterator &src,
                    UBool compat, UBool ignoreHangul,
                    UGrowBuffer *growBuffer, void *context,
                    UErrorCode *pErrorCode) {
    UChar stackBuffer[40];
    UChar *buffer;
    int32_t startIndex, bufferLength, destLength;

    buffer=stackBuffer;
    bufferLength=_findPreviousDecomposeBoundary(src,
                                                buffer, sizeof(stackBuffer)/U_SIZEOF_UCHAR,
                                                startIndex,
                                                pErrorCode);
    if(bufferLength>0) {
        destLength=unorm_decompose(dest, destCapacity,
                                   buffer+startIndex, bufferLength,
                                   compat, ignoreHangul,
                                   growBuffer, context, pErrorCode);
    } else {
        destLength=0;
    }

    /* cleanup */
    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return destLength;
}

U_CFUNC int32_t
unorm_nextFCD(UChar *dest, int32_t destCapacity,
              CharacterIterator &src,
              UGrowBuffer *growBuffer, void *context,
              UErrorCode *pErrorCode) {
    UChar stackBuffer[40];
    UChar *buffer;
    int32_t bufferLength, destLength;

    buffer=stackBuffer;
    bufferLength=_findNextDecomposeBoundary(src,
                                            buffer, sizeof(stackBuffer)/U_SIZEOF_UCHAR,
                                            pErrorCode);
    if(bufferLength>0) {
        destLength=unorm_makeFCD(dest, destCapacity,
                                 buffer, bufferLength,
                                 growBuffer, context, pErrorCode);
    } else {
        destLength=0;
    }

    /* cleanup */
    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return destLength;
}

U_CFUNC int32_t
unorm_prevFCD(UChar *dest, int32_t destCapacity,
              CharacterIterator &src,
              UGrowBuffer *growBuffer, void *context,
              UErrorCode *pErrorCode) {
    UChar stackBuffer[40];
    UChar *buffer;
    int32_t startIndex, bufferLength, destLength;

    buffer=stackBuffer;
    bufferLength=_findPreviousDecomposeBoundary(src,
                                                buffer, sizeof(stackBuffer)/U_SIZEOF_UCHAR,
                                                startIndex,
                                                pErrorCode);
    if(bufferLength>0) {
        destLength=unorm_makeFCD(dest, destCapacity,
                                 buffer+startIndex, bufferLength,
                                 growBuffer, context, pErrorCode);
    } else {
        destLength=0;
    }

    /* cleanup */
    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return destLength;
}

U_CFUNC int32_t
unorm_nextCompose(UChar *dest, int32_t destCapacity,
                  CharacterIterator &src,
                  UBool compat, UBool ignoreHangul,
                  UGrowBuffer *growBuffer, void *context,
                  UErrorCode *pErrorCode) {
    *pErrorCode=U_UNSUPPORTED_ERROR;
    return 0;
}

U_CFUNC int32_t
unorm_prevCompose(UChar *dest, int32_t destCapacity,
                  CharacterIterator &src,
                  UBool compat, UBool ignoreHangul,
                  UGrowBuffer *growBuffer, void *context,
                  UErrorCode *pErrorCode) {
    *pErrorCode=U_UNSUPPORTED_ERROR;
    return 0;
}
