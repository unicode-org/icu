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

static inline UBool
isHangulWithoutJamoT(UChar c) {
    c-=HANGUL_BASE;
    return c<HANGUL_COUNT && c%JAMO_T_COUNT==0;
}

/* norm32 helpers */

/* is this a norm32 with a regular index? */
static inline UBool
isNorm32Regular(uint32_t norm32) {
    return norm32<_NORM_MIN_SPECIAL;
}

/* is this a norm32 with a special index for a lead surrogate? */
static inline UBool
isNorm32LeadSurrogate(uint32_t norm32) {
    return _NORM_MIN_SPECIAL<=norm32 && norm32<_NORM_SURROGATES_TOP;
}

/* is this a norm32 with a special index for a Hangul syllable or a Jamo? */
static inline UBool
isNorm32HangulOrJamo(uint32_t norm32) {
    return norm32>=_NORM_MIN_HANGUL;
}

/*
 * Given isNorm32HangulOrJamo(),
 * is this a Hangul syllable or a Jamo?
 */
static inline UBool
isHangulJamoNorm32HangulOrJamoL(uint32_t norm32) {
    return norm32<_NORM_MIN_JAMO_V;
}

/*
 * Given norm32 for Jamo V or T,
 * is this a Jamo V?
 */
static inline UBool
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

UBool
unorm_cleanup() {
    if(normData!=NULL) {
        udata_close(normData);
        normData=NULL;
    }
    dataErrorCode=U_ZERO_ERROR;
    haveNormData=0;

    return TRUE;
}

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

static inline UBool
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

static inline uint32_t
_getNorm32(UChar c) {
    return
        normTrieData[
            normTrieIndex[
                c>>_NORM_TRIE_SHIFT
            ]+
            (c&_NORM_STAGE_2_MASK)
        ];
}

static inline uint32_t
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

/*
 * get a norm32 from text with complete code points
 * (like from decompositions)
 */
static inline uint32_t
_getNorm32(const UChar *p, uint32_t mask) {
    uint32_t norm32=_getNorm32(*p);
    if((norm32&mask) && isNorm32LeadSurrogate(norm32)) {
        /* *p is a lead surrogate, get the real norm32 */
        norm32=_getNorm32FromSurrogatePair(norm32, *(p+1));
    }
    return norm32;
}

static inline uint16_t
_getFCD16(UChar c) {
    return
        fcdTrieIndex[
            fcdTrieIndex[
                c>>_NORM_TRIE_SHIFT
            ]+
            (c&_NORM_STAGE_2_MASK)
        ];
}

static inline uint16_t
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

static inline const uint16_t *
_getExtraData(uint32_t norm32) {
    return extraData+(norm32>>_NORM_EXTRA_SHIFT);
}

/* get the canonical or compatibility decomposition for one character */
static inline const UChar *
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
static inline const UChar *
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
static inline uint8_t
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
 * read backwards and get norm32
 * return 0 if the character is <minC
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
static inline uint32_t
_getPrevNorm32(const UChar *start, const UChar *&src,
               uint32_t minC, uint32_t mask,
               UChar &c, UChar &c2) {
    uint32_t norm32;

    c=*--src;
    c2=0;

    /* check for a surrogate before getting norm32 to see if we need to predecrement further */
    if(c<minC) {
        return 0;
    } else if(!UTF_IS_SURROGATE(c)) {
        return _getNorm32(c);
    } else if(UTF_IS_SURROGATE_FIRST(c)) {
        /* unpaired first surrogate */
        return 0;
    } else if(src!=start && UTF_IS_FIRST_SURROGATE(c2=*(src-1))) {
        --src;
        norm32=_getNorm32(c2);

        if((norm32&mask)==0) {
            /* all surrogate pairs with this lead surrogate have only irrelevant data */
            return 0;
        } else {
            /* norm32 must be a surrogate special */
            return _getNorm32FromSurrogatePair(norm32, c);
        }
    } else {
        /* unpaired second surrogate */
        c2=0;
        return 0;
    }
}

/*
 * get the combining class of (c, c2)=*--p
 * before: start<p  after: start<=p
 */
static inline uint8_t
_getPrevCC(const UChar *start, const UChar *&p) {
    UChar c, c2;

    return (uint8_t)(_getPrevNorm32(start, p, _NORM_MIN_WITH_LEAD_CC, _NORM_CC_MASK, c, c2)>>_NORM_CC_SHIFT);
}

/*
 * is this (or does its decomposition begin with) a "true starter"?
 * (cc==0 and NF*C_YES)
 */
static inline UBool
_isTrueStarter(uint32_t norm32, uint32_t ccOrQCMask, uint32_t decompQCMask) {
    if((norm32&ccOrQCMask)==0) {
        return TRUE; /* this is a true starter (could be Hangul or Jamo L) */
    }

    /* inspect its decomposition - not a Hangul or a surrogate here */
    if((norm32&decompQCMask)!=0) {
        const UChar *p;
        int32_t length;
        uint8_t cc, trailCC;

        /* decomposes, get everything from the variable-length extra data */
        p=_decompose(norm32, decompQCMask, length, cc, trailCC);
        if(cc==0) {
            uint32_t qcMask=ccOrQCMask&_NORM_QC_MASK;

            /* does it begin with NFC_YES? */
            if((_getNorm32(p, qcMask)&qcMask)==0) {
                /* yes, the decomposition begins with a true starter */
                return TRUE;
            }
        }
    }
    return FALSE;
}

/* reorder UTF-16 in-place -------------------------------------------------- */

/*
 * simpler, single-character version of _mergeOrdered() -
 * bubble-insert one single code point into the preceding string
 * which is already canonically ordered
 * (c, c2) may or may not yet have been inserted at [current..p[
 *
 * it must be p=current+lengthof(c, c2) i.e. p=current+(c2==0 ? 1 : 2)
 *
 * before: [start..current[ is already ordered, and
 *         [current..p[     may or may not hold (c, c2) but
 *                          must be exactly the same length as (c, c2)
 * after: [start..p[ is ordered
 *
 * returns the trailing combining class
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
 * since this function is called much less often than the single-code point
 * _insertOrdered(), it just uses that for easier maintenance
 * (see file version from before 2001aug31 for a more optimized version)
 *
 * returns the trailing combining class
 */
static uint8_t
_mergeOrdered(UChar *start, UChar *current,
              const UChar *next, const UChar *limit, UBool isOrdered=TRUE) {
    UChar *r;
    UChar c, c2;
    uint8_t cc, trailCC=0;
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
                r=current+(c2==0 ? 1 : 2);
                trailCC=_insertOrdered(start, current, r, c, c2, cc);
                current=r;
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
                    /*
                     * delay _getFCD16(c) for any character <_NORM_MIN_WITH_LEAD_CC
                     * because chances are good that the next one will have
                     * a leading cc of 0;
                     * _getFCD16(-prevCC) is later called when necessary -
                     * -c fits into int16_t because it is <_NORM_MIN_WITH_LEAD_CC==0x300
                     */
                    prevCC=(int16_t)-c;
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
                    prevCC=(int16_t)-c;
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
            if(src!=limit && UTF_IS_SECOND_SURROGATE(c2=*src)) {
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
                prevCC=(int16_t)(_getFCD16((UChar)-prevCC)&0xff);
            }

            if(cc<prevCC) {
                return FALSE;
            }
        }
        prevCC=(int16_t)(fcd16&0xff);
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
            if(src!=limit && UTF_IS_SECOND_SURROGATE(c2=*src)) {
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

static int32_t
_decompose(UChar *&dest, int32_t &destCapacity,
           const UChar *src, int32_t srcLength,
           UBool compat, UBool ignoreHangul,
           UGrowBuffer *growBuffer, void *context,
           uint8_t &outTrailCC,
           UErrorCode * /*pErrorCode*/) {
    UChar buffer[3];
    const UChar *limit, *prevSrc, *p;
    uint32_t norm32, ccOrQCMask, qcMask;
    int32_t destIndex, reorderStartIndex, length;
    UChar c, c2, minNoMaybe;
    uint8_t cc, prevCC, trailCC;
    UBool canGrow;

    if(!compat) {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE];
        qcMask=_NORM_QC_NFD;
    } else {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE];
        qcMask=_NORM_QC_NFKD;
    }

    /* initialize */
    ccOrQCMask=_NORM_CC_MASK|qcMask;
    destIndex=reorderStartIndex=0;
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
            reorderStartIndex=destIndex;
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
         *
         * the following merge-sort of the current character into the preceding,
         * canonically ordered result text will use the optimized _insertOrdered()
         * if there is only one single code point to process;
         * this is indicated with p==NULL, and (c, c2) is the character to insert
         * ((c, 0) for a BMP character and (lead surrogate, trail surrogate)
         * for a supplementary character)
         * otherwise, p[length] is merged in with _mergeOrdered()
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
                if(src!=limit && UTF_IS_SECOND_SURROGATE(c2=*src)) {
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
                    trailCC=_insertOrdered(dest+reorderStartIndex, reorderSplit, dest+destIndex, c, c2, cc);
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
                    trailCC=_mergeOrdered(dest+reorderStartIndex, reorderSplit, p, p+length);
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
            reorderStartIndex=destIndex;
        }
    }

    outTrailCC=prevCC;
    return destIndex;
}

U_CAPI int32_t U_EXPORT2
unorm_decompose(UChar **pDest, int32_t *pDestCapacity,
                const UChar *src, int32_t srcLength,
                UBool compat, UBool ignoreHangul,
                UGrowBuffer *growBuffer, void *context,
                UErrorCode *pErrorCode) {
    int32_t destIndex;
    uint8_t trailCC;

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    destIndex=_decompose(*pDest, *pDestCapacity,
                         src, srcLength,
                         compat, ignoreHangul,
                         growBuffer, context,
                         trailCC,
                         pErrorCode);

    return u_terminateUChars(*pDest, *pDestCapacity, destIndex, pErrorCode);
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
        if(src==limit) {
            break;
        }
        c=*src;

        /* stop if lead cc==0 for this character */
        if(c<_NORM_MIN_WITH_LEAD_CC || (fcd16=_getFCD16(c))==0) {
            break; /* catches terminating NUL, too */
        }

        if(!UTF_IS_FIRST_SURROGATE(c)) {
            if(fcd16<=0xff) {
                break;
            }
            ++src;
        } else if((src+1)!=limit && (c2=*(src+1), UTF_IS_SECOND_SURROGATE(c2))) {
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
              UChar *&dest, int32_t &destIndex, int32_t &destCapacity,
              UBool canGrow, UGrowBuffer *growBuffer, void *context) {
    const UChar *p;
    uint32_t norm32;
    int32_t reorderStartIndex, length;
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
    reorderStartIndex=destIndex;
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
                    trailCC=_insertOrdered(dest+reorderStartIndex, reorderSplit, dest+destIndex, c, c2, cc);
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
                    trailCC=_mergeOrdered(dest+reorderStartIndex, reorderSplit, p, p+length);
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
            reorderStartIndex=destIndex;
        }
    }

    return prevCC;
}

static int32_t
unorm_makeFCD(UChar *&dest, int32_t &destCapacity,
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
                    prevCC=(int16_t)-c;
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
                    prevCC=(int16_t)-c;
                } else if((fcd16=_getFCD16(c))==0) {
                    prevCC=0;
                } else {
                    break;
                }
                ++src;
            }
        }

        /*
         * prevCC has values from the following ranges:
         * 0..0xff - the previous trail combining class
         * <0      - the negative value of the previous code unit;
         *           that code unit was <_NORM_MIN_WITH_LEAD_CC and its _getFCD16()
         *           was deferred so that average text is checked faster
         */

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

            /* prevCC<0 is only possible from the above loop, i.e., only if prevSrc<src */
            if(prevCC<0) {
                /* the previous character was <_NORM_MIN_WITH_LEAD_CC, we need to get its trail cc */
                prevCC=(int16_t)(_getFCD16((UChar)-prevCC)&0xff);

                /*
                 * set a pointer to this below-U+0300 character;
                 * if prevCC==0 then it will moved to after this character below
                 */
                decompStart=prevSrc-1;
            }
        }
        /*
         * now:
         * prevSrc==src - used later to adjust destIndex before decomposition
         * prevCC>=0
         */

        /* end of source reached? */
        if(limit==NULL ? c==0 : src==limit) {
            break;
        }

        /* set a pointer to after the last source position where prevCC==0 */
        if(prevCC==0) {
            decompStart=prevSrc;
        }

        /* c already contains *src and fcd16 is set for it, increment src */
        ++src;

        /* check one above-minimum, relevant code unit */
        if(UTF_IS_FIRST_SURROGATE(c)) {
            /* c is a lead surrogate, get the real fcd16 */
            if(src!=limit && UTF_IS_SECOND_SURROGATE(c2=*src)) {
                ++src;
                fcd16=_getFCD16FromSurrogatePair(fcd16, c2);
            } else {
                c2=0;
                fcd16=0;
            }
        } else {
            c2=0;
        }

        /* we are looking at the character (c, c2) at [prevSrc..src[ */

        /* check the combining order, get the lead cc */
        cc=(int16_t)(fcd16>>8);
        if(cc==0 || cc>=prevCC) {
            /* the order is ok */
            if(cc==0) {
                decompStart=prevSrc;
            }
            prevCC=(int16_t)(fcd16&0xff);

            /* just append (c, c2) */
            length= c2==0 ? 1 : 2;
            if( (destIndex+length)<=destCapacity ||
                /* attempt to grow the buffer */
                (canGrow && (canGrow=growBuffer(context, &dest, &destCapacity,
                                                limit==NULL ?
                                                    2*destCapacity+length+20 :
                                                    destCapacity+length+2*(limit-src)+20,
                                                destIndex))!=FALSE)
            ) {
                dest[destIndex++]=c;
                if(c2!=0) {
                    dest[destIndex++]=c2;
                }
            } else {
                destIndex+=length;
            }
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

    return u_terminateUChars(dest, destCapacity, destIndex, pErrorCode);
}

/* make NFC & NFKC ---------------------------------------------------------- */

enum {
    _STACK_BUFFER_CAPACITY=100
};

/* get the composition properties of the next character */
static inline uint32_t
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
static inline uint16_t
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
static inline uint16_t
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
        key=(uint16_t)((value&0x2000)+1);

        /* get the composition result code point from the variable-length result value */
        if(value&0x8000) {
            if(value&0x4000) {
                /* surrogate pair composition result */
                value=(uint16_t)((value&0x3ff)|0xd800);
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

/* find the first true starter in [src..limit[ and return the pointer to it */
static const UChar *
_findNextStarter(const UChar *src, const UChar *limit,
                 uint32_t qcMask, uint32_t decompQCMask, UChar minNoMaybe) {
    const UChar *p;
    uint32_t norm32, ccOrQCMask;
    int32_t length;
    UChar c, c2;
    uint8_t cc, trailCC;

    ccOrQCMask=_NORM_CC_MASK|qcMask;

    for(;;) {
        if(src==limit) {
            break; /* end of string */
        }
        c=*src;
        if(c<minNoMaybe) {
            break; /* catches NUL terminater, too */
        }

        norm32=_getNorm32(c);
        if((norm32&ccOrQCMask)==0) {
            break; /* true starter */
        }

        if((norm32&decompQCMask)==0) {
            ++src; /* does not decompose, continue */
            continue;
        }

        /* no Hangul/Jamo here because they are all true starters or don't decompose */
        if(isNorm32Regular(norm32)) {
            c2=0;
        } else {
            /* c is a lead surrogate, get the real norm32 */
            if((src+1)==limit || UTF_IS_SECOND_SURROGATE(c2=*(src+1))) {
                break; /* unmatched first surrogate */
            }
            norm32=_getNorm32FromSurrogatePair(norm32, c2);

            if((norm32&ccOrQCMask)==0) {
                break; /* true starter */
            } else if((norm32&decompQCMask)==0) {
                src+=2; /* does not decompose, continue */
                continue;
            }
        }

        /* (c, c2) decomposes, get everything from the variable-length extra data */
        p=_decompose(norm32, decompQCMask, length, cc, trailCC);

        /* get the first character's norm32 to check if it is a true starter */
        if(cc==0 && (_getNorm32(p, qcMask)&qcMask)==0) {
            break; /* true starter */
        }

        src+= c2==0 ? 1 : 2; /* not a true starter, continue */
    }

    return src;
}

/*
 * recompose around the current character:
 * this function is called when unorm_decompose() finds a quick check "no" or "maybe"
 * after some text (with quick check "yes") has been copied already
 *
 * decompose this character as well as parts of the source surrounding it,
 * bounded by the previous and the next true starter,
 * and then recompose this decomposition
 */
static const UChar *
_composePart(UChar *stackBuffer, UChar *&buffer, int32_t &bufferCapacity, int32_t &length,
             const UChar *&prevStarter, const UChar *prevSrc, const UChar *src, const UChar *limit,
             uint32_t norm32,
             uint32_t qcMask, uint8_t &prevCC,
             int32_t &destIndex,
             UErrorCode *pErrorCode) {
    UChar *recomposeLimit;
    uint32_t decompQCMask;
    UChar minNoMaybe;
    uint8_t trailCC;

    decompQCMask=(qcMask<<2)&0xf; /* decomposition quick check mask */

    if(!(decompQCMask&_NORM_QC_NFKD)) {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE];
    } else {
        minNoMaybe=(UChar)indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE];
    }

    /*
     * find the last true starter in [prevStarter..src[
     * it is either the decomposition of the current character (at prevSrc),
     * or prevStarter
     */
    if(_isTrueStarter(norm32, _NORM_CC_MASK|qcMask, decompQCMask)) {
        prevStarter=prevSrc;
    } else {
        /* adjust destIndex: back out what had been copied with qc "yes" */
        destIndex-=(int32_t)(prevSrc-prevStarter);
    }

    /* find the next true starter in [src..limit[ */
    src=_findNextStarter(src, limit, qcMask, decompQCMask, minNoMaybe);

    /* decompose [prevStarter..src[ */
    length=_decompose(buffer, bufferCapacity,
                      prevStarter, src-prevStarter,
                      (decompQCMask&_NORM_QC_NFKD)!=0, FALSE,
                      (UGrowBuffer*)u_growBufferFromStatic, stackBuffer,
                      trailCC,
                      pErrorCode);

    /* set the next starter */
    prevStarter=src;

    /* recompose the decomposition */
    recomposeLimit=buffer+length;
    if(length>=2) {
        prevCC=_recompose(buffer, recomposeLimit);
    }

    /* return with a pointer to the recomposition and its length */
    length=recomposeLimit-buffer;
    return buffer;
}

static int32_t
_compose(UChar *&dest, int32_t &destCapacity,
         const UChar *src, int32_t srcLength,
         UBool compat, UBool /* ### TODO: need to do this? -- ignoreHangul -- ### */,
         UGrowBuffer *growBuffer, void *context,
         UErrorCode *pErrorCode) {
    UChar stackBuffer[_STACK_BUFFER_CAPACITY];
    UChar *buffer;
    int32_t bufferCapacity;

    const UChar *limit, *prevSrc, *prevStarter;
    uint32_t norm32, ccOrQCMask, qcMask;
    int32_t destIndex, reorderStartIndex, length;
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
     *
     * prevStarter will be used instead of looking for a true starter
     * while incrementally decomposing [prevStarter..prevSrc[
     * in _composePart(). Having a good prevStarter allows to just decompose
     * the entire [prevStarter..prevSrc[.
     *
     * This relies on the assumption that the decomposition of a true starter
     * also begins with a true starter. gennorm/store.c checks for this.
     */
    prevStarter=src;

    ccOrQCMask=_NORM_CC_MASK|qcMask;
    destIndex=reorderStartIndex=0;
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
            reorderStartIndex=destIndex;

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
         * dest      reorderStartIndex destIndex     destCapacity
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
            reorderStartIndex=destIndex;

            if(/* ### TODO: do we need to do this? !ignoreHangul && ### */ destIndex>0) {
                /* c is a Jamo V/T, see if we can compose it with the previous character */
                c2=*(prevSrc-1);
                if(isJamoVTNorm32JamoV(norm32)) {
                    /* Jamo V, compose with previous Jamo L and following Jamo T */
                    c2=(UChar)(c2-JAMO_L_BASE);
                    if(c2<JAMO_L_COUNT) {
                        c=(UChar)(HANGUL_BASE+(c2*JAMO_V_COUNT+(c-JAMO_V_BASE))*JAMO_T_COUNT);
                        if(src!=limit && (c2=(UChar)(*src-JAMO_T_BASE))<JAMO_T_COUNT) {
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
                if(src!=limit && UTF_IS_SECOND_SURROGATE(c2=*src)) {
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

                /*
                 * find appropriate boundaries around this character,
                 * decompose the source text from between the boundaries,
                 * and recompose it
                 *
                 * this puts the intermediate text into the side buffer because
                 * it might be longer than the recomposition end result,
                 * or the destination buffer may be too short or missing
                 *
                 * note that destIndex may be adjusted backwards to account
                 * for source text that passed the quick check but needed to
                 * take part in the recomposition
                 */
                p=_composePart(stackBuffer, buffer, bufferCapacity, length,
                               prevStarter,     /* in/out, will be set to the following true starter */
                               prevSrc, src, limit,
                               norm32,
                               qcMask,
                               prevCC,          /* output */
                               destIndex,       /* will be adjusted */
                               pErrorCode);

                if(p==NULL) {
                    destIndex=0;   /* an error occurred (out of memory) */
                    break;
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
                prevCC=_insertOrdered(dest+reorderStartIndex, reorderSplit, dest+destIndex, c, c2, cc);
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

    /* cleanup */
    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return destIndex;
}

U_CAPI int32_t U_EXPORT2
unorm_compose(UChar **pDest, int32_t *pDestCapacity,
              const UChar *src, int32_t srcLength,
              UBool compat, UBool ignoreHangul,
              UGrowBuffer *growBuffer, void *context,
              UErrorCode *pErrorCode) {
    int32_t destIndex;

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    destIndex=_compose(*pDest, *pDestCapacity,
                       src, srcLength,
                       compat, ignoreHangul,
                       growBuffer, context,
                       pErrorCode);

    return u_terminateUChars(*pDest, *pDestCapacity, destIndex, pErrorCode);
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
U_CAPI int32_t U_EXPORT2
unorm_internalNormalize(UChar **pDest, int32_t *pDestCapacity,
                        const UChar *src, int32_t srcLength,
                        UNormalizationMode mode, UBool ignoreHangul,
                        UGrowBuffer *growBuffer, void *context,
                        UErrorCode *pErrorCode) {
    switch(mode) {
    case UNORM_NFD:
        return unorm_decompose(pDest, pDestCapacity,
                               src, srcLength,
                               FALSE, ignoreHangul,
                               growBuffer, context,
                               pErrorCode);
    case UNORM_NFKD:
        return unorm_decompose(pDest, pDestCapacity,
                               src, srcLength,
                               TRUE, ignoreHangul,
                               growBuffer, context,
                               pErrorCode);
    case UNORM_NFC:
        return unorm_compose(pDest, pDestCapacity,
                             src, srcLength,
                             FALSE, ignoreHangul,
                             growBuffer, context,
                             pErrorCode);
    case UNORM_NFKC:
        return unorm_compose(pDest, pDestCapacity,
                             src, srcLength,
                             TRUE, ignoreHangul,
                             growBuffer, context,
                             pErrorCode);
    case UNORM_FCD:
        return unorm_makeFCD(*pDest, *pDestCapacity,
                             src, srcLength,
                             growBuffer, context,
                             pErrorCode);
    case UNORM_NONE:
        /* just copy the string */
        if(srcLength==-1) {
            srcLength=u_strlen(src);
        }
        if( srcLength<=*pDestCapacity ||
            /* attempt to grow the buffer */
            (growBuffer!=NULL && growBuffer(context, pDest, pDestCapacity, srcLength+1, 0))
        ) {
            uprv_memcpy(*pDest, src, srcLength*U_SIZEOF_UCHAR);
        }
        return u_terminateUChars(*pDest, *pDestCapacity, srcLength, pErrorCode);
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
    if( dest!=NULL &&
        ((src>=dest && src<(dest+destCapacity)) ||
         (srcLength>0 && dest>=src && dest<(src+srcLength)))
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    return unorm_internalNormalize(&dest, &destCapacity,
                                   src, srcLength,
                                   mode, (UBool)((option&UNORM_IGNORE_HANGUL)!=0),
                                   NULL, NULL,
                                   pErrorCode);
}

U_NAMESPACE_BEGIN

/* iteration functions ------------------------------------------------------ */

/*
 * These iteration functions are the core implementations of the
 * Normalizer class iteration API.
 * They read from a CharacterIterator into their own buffer
 * and normalize into the Normalizer iteration buffer.
 * Normalizer itself then iterates over its buffer until that needs to be
 * filled again.
 */

/* backward iteration ------------------------------------------------------- */

/*
 * read backwards and get norm32
 * return 0 if the character is <minC
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
static inline uint32_t
_getPrevNorm32(CharacterIterator &src, uint32_t minC, uint32_t mask, UChar &c, UChar &c2) {
    uint32_t norm32;

    /* need src.hasPrevious() */
    c=src.previous();
    c2=0;

    /* check for a surrogate before getting norm32 to see if we need to predecrement further */
    if(c<minC) {
        return 0;
    } else if(!UTF_IS_SURROGATE(c)) {
        return _getNorm32(c);
    } else if(UTF_IS_SURROGATE_FIRST(c) || !src.hasPrevious()) {
        /* unpaired surrogate */
        return 0;
    } else if(UTF_IS_FIRST_SURROGATE(c2=src.previous())) {
        norm32=_getNorm32(c2);
        if((norm32&mask)==0) {
            /* all surrogate pairs with this lead surrogate have irrelevant data */
            return 0;
        } else {
            /* norm32 must be a surrogate special */
            return _getNorm32FromSurrogatePair(norm32, c);
        }
    } else {
        /* unpaired second surrogate, undo the c2=src.previous() movement */
        src.move(1, CharacterIterator::kCurrent);
        return 0;
    }
}

/*
 * read backwards and check if the character is a previous-iteration boundary
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
typedef UBool
IsPrevBoundaryFn(CharacterIterator &src, uint32_t minC, uint32_t mask, UChar &c, UChar &c2);

/*
 * read backwards and check if the combining class is 0
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
static UBool
_isPrevCCZero(CharacterIterator &src, uint32_t minC, uint32_t ccMask, UChar &c, UChar &c2) {
    return (_getPrevNorm32(src, minC, ccMask, c, c2)&ccMask)==0;
}

/*
 * read backwards and check if the character is (or its decomposition begins with)
 * a "true starter" (cc==0 and NF*C_YES)
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
static UBool
_isPrevTrueStarter(CharacterIterator &src, uint32_t minC, uint32_t ccOrQCMask, UChar &c, UChar &c2) {
    uint32_t norm32, decompQCMask;
    
    decompQCMask=(ccOrQCMask<<2)&0xf; /* decomposition quick check mask */
    norm32=_getPrevNorm32(src, minC, ccOrQCMask|decompQCMask, c, c2);
    return _isTrueStarter(norm32, ccOrQCMask, decompQCMask);
}

static int32_t
_findPreviousIterationBoundary(CharacterIterator &src,
                               IsPrevBoundaryFn *isPrevBoundary, uint32_t minC, uint32_t mask,
                               UChar *&buffer, int32_t &bufferCapacity,
                               int32_t &startIndex,
                               UErrorCode *pErrorCode) {
    UChar *stackBuffer;
    UChar c, c2;
    UBool isBoundary;

    /* initialize */
    stackBuffer=buffer;
    startIndex=bufferCapacity; /* fill the buffer from the end backwards */

    while(src.hasPrevious()) {
        isBoundary=isPrevBoundary(src, minC, mask, c, c2);

        /* always write this character to the front of the buffer */
        /* make sure there is enough space in the buffer */
        if(startIndex < (c2==0 ? 1 : 2)) {
            int32_t bufferLength=bufferCapacity;

            if(!u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity, 2*bufferCapacity, bufferLength)) {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                src.setToStart();
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

        /* stop if this just-copied character is a boundary */
        if(isBoundary) {
            break;
        }
    }

    /* return the length of the buffer contents */
    return bufferCapacity-startIndex;
}

U_CFUNC int32_t
unorm_previousNormalize(UChar *&dest, int32_t &destCapacity,
                        CharacterIterator &src,
                        UNormalizationMode mode, UBool ignoreHangul,
                        UGrowBuffer *growBuffer, void *context,
                        UErrorCode *pErrorCode) {
    UChar stackBuffer[40];
    UChar *buffer;
    IsPrevBoundaryFn *isPreviousBoundary;
    uint32_t mask;
    int32_t startIndex, bufferLength, bufferCapacity, destLength;
    UChar minC;

    switch(mode) {
    case UNORM_NFD:
    case UNORM_NFKD:
    case UNORM_FCD:
        isPreviousBoundary=_isPrevCCZero;
        minC=_NORM_MIN_WITH_LEAD_CC;
        mask=_NORM_CC_MASK;
        break;
    case UNORM_NFC:
        isPreviousBoundary=_isPrevTrueStarter;
        minC=(UChar)indexes[_NORM_INDEX_MIN_NFC_NO_MAYBE];
        mask=_NORM_CC_MASK|_NORM_QC_NFC;
        break;
    case UNORM_NFKC:
        isPreviousBoundary=_isPrevTrueStarter;
        minC=(UChar)indexes[_NORM_INDEX_MIN_NFKC_NO_MAYBE];
        mask=_NORM_CC_MASK|_NORM_QC_NFKC;
        break;
    case UNORM_NONE:
        if(src.hasPrevious()) {
            UChar32 c=src.previous32();

            destLength=0;
            UTF_APPEND_CHAR_UNSAFE(dest, destLength, c);
            return destLength;
        } else {
            return 0;
        }
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    buffer=stackBuffer;
    bufferCapacity=(int32_t)(sizeof(stackBuffer)/U_SIZEOF_UCHAR);
    bufferLength=_findPreviousIterationBoundary(src,
                                                isPreviousBoundary, minC, mask,
                                                buffer, bufferCapacity,
                                                startIndex,
                                                pErrorCode);
    if(bufferLength>0) {
        destLength=unorm_internalNormalize(&dest, &destCapacity,
                                           buffer+startIndex, bufferLength,
                                           mode, ignoreHangul,
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

/* forward iteration -------------------------------------------------------- */

/*
 * read forward and get norm32
 * return 0 if the character is <minC
 * if c2!=0 then (c2, c) is a surrogate pair
 * always reads complete characters
 */
static inline uint32_t
_getNextNorm32(CharacterIterator &src, uint32_t minC, uint32_t mask, UChar &c, UChar &c2) {
    uint32_t norm32;

    /* need src.hasNext() */
    c=src.nextPostInc();
    c2=0;

    if(c<minC) {
        return 0;
    }

    norm32=_getNorm32(c);
    if(UTF_IS_FIRST_SURROGATE(c) && src.hasNext() && UTF_IS_SECOND_SURROGATE(c2=src.current())) {
        src.move(1, CharacterIterator::kCurrent); /* skip the c2 surrogate */
        if((norm32&mask)==0) {
            /* irrelevant data */
            return 0;
        } else {
            /* norm32 must be a surrogate special */
            return _getNorm32FromSurrogatePair(norm32, c2);
        }
    }
    return norm32;
}

/*
 * read forward and check if the character is a next-iteration boundary
 * if c2!=0 then (c, c2) is a surrogate pair
 */
typedef UBool
IsNextBoundaryFn(CharacterIterator &src, uint32_t minC, uint32_t mask, UChar &c, UChar &c2);

/*
 * read forward and check if the combining class is 0
 * if c2!=0 then (c, c2) is a surrogate pair
 */
static UBool
_isNextCCZero(CharacterIterator &src, uint32_t minC, uint32_t ccMask, UChar &c, UChar &c2) {
    return (_getNextNorm32(src, minC, ccMask, c, c2)&ccMask)==0;
}

/*
 * read forward and check if the character is (or its decomposition begins with)
 * a "true starter" (cc==0 and NF*C_YES)
 * if c2!=0 then (c, c2) is a surrogate pair
 */
static UBool
_isNextTrueStarter(CharacterIterator &src, uint32_t minC, uint32_t ccOrQCMask, UChar &c, UChar &c2) {
    uint32_t norm32, decompQCMask;
    
    decompQCMask=(ccOrQCMask<<2)&0xf; /* decomposition quick check mask */
    norm32=_getNextNorm32(src, minC, ccOrQCMask|decompQCMask, c, c2);
    return _isTrueStarter(norm32, ccOrQCMask, decompQCMask);
}

static int32_t
_findNextIterationBoundary(CharacterIterator &src,
                           IsNextBoundaryFn *isNextBoundary, uint32_t minC, uint32_t mask,
                           UChar *&buffer, int32_t &bufferCapacity,
                           UErrorCode *pErrorCode) {
    UChar *stackBuffer;
    int32_t bufferIndex;
    UChar c, c2;

    if(!src.hasNext()) {
        return 0;
    }

    /* initialize */
    stackBuffer=buffer;

    /* get one character and ignore its properties */
    buffer[0]=c=src.current();
    bufferIndex=1;
    c2=src.next();
    if(UTF_IS_FIRST_SURROGATE(c) && UTF_IS_SECOND_SURROGATE(c2)) {
        buffer[bufferIndex++]=c2;
        src.move(1, CharacterIterator::kCurrent); /* skip the c2 surrogate */
    }

    /* get all following characters until we see a boundary */
    /* checking hasNext() instead of c!=DONE on the off-chance that U+ffff is part of the string */
    while(src.hasNext()) {
        if(isNextBoundary(src, minC, mask, c, c2)) {
            /* back out the latest movement to stop at the boundary */
            src.move(c2==0 ? -1 : -2, CharacterIterator::kCurrent);
            break;
        } else {
            if(bufferIndex+(c2==0 ? 1 : 2)<=bufferCapacity ||
                /* attempt to grow the buffer */
                u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity,
                                       2*bufferCapacity,
                                       bufferIndex)
            ) {
                buffer[bufferIndex++]=c;
                if(c2!=0) {
                    buffer[bufferIndex++]=c2;
                }
            } else {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                src.setToEnd();
                return 0;
            }
        }
    }

    /* return the length of the buffer contents */
    return bufferIndex;
}

U_CFUNC int32_t
unorm_nextNormalize(UChar *&dest, int32_t &destCapacity,
                    CharacterIterator &src,
                    UNormalizationMode mode, UBool ignoreHangul,
                    UGrowBuffer *growBuffer, void *context,
                    UErrorCode *pErrorCode) {
    UChar stackBuffer[40];
    UChar *buffer;
    IsNextBoundaryFn *isNextBoundary;
    uint32_t mask;
    int32_t bufferLength, bufferCapacity, destLength;
    UChar minC;

    switch(mode) {
    case UNORM_NFD:
    case UNORM_NFKD:
    case UNORM_FCD:
        isNextBoundary=_isNextCCZero;
        minC=_NORM_MIN_WITH_LEAD_CC;
        mask=_NORM_CC_MASK;
        break;
    case UNORM_NFC:
        isNextBoundary=_isNextTrueStarter;
        minC=(UChar)indexes[_NORM_INDEX_MIN_NFC_NO_MAYBE];
        mask=_NORM_CC_MASK|_NORM_QC_NFC;
        break;
    case UNORM_NFKC:
        isNextBoundary=_isNextTrueStarter;
        minC=(UChar)indexes[_NORM_INDEX_MIN_NFKC_NO_MAYBE];
        mask=_NORM_CC_MASK|_NORM_QC_NFKC;
        break;
    case UNORM_NONE:
        if(src.hasNext()) {
            UChar32 c=src.next32PostInc();

            destLength=0;
            UTF_APPEND_CHAR_UNSAFE(dest, destLength, c);
            return destLength;
        } else {
            return 0;
        }
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    buffer=stackBuffer;
    bufferCapacity=(int32_t)(sizeof(stackBuffer)/U_SIZEOF_UCHAR);
    bufferLength=_findNextIterationBoundary(src,
                                            isNextBoundary, minC, mask,
                                            buffer, bufferCapacity,
                                            pErrorCode);
    if(bufferLength>0) {
        destLength=unorm_internalNormalize(&dest, &destCapacity,
                                           buffer, bufferLength,
                                           mode, ignoreHangul,
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

U_NAMESPACE_END
/*
 * ### TODO: check if NF*D and FCD iteration finds optimal boundaries
 * and if not, how hard it would be to improve it.
 * For example, see _findSafeFCD().
 */
