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
*/

#include "unicode/utypes.h"
#include "unicode/unorm.h"
#include "unicode/normlzr.h"
#include "unicode/ustring.h"
#include "unicode/udata.h"
#include "cpputils.h"
#include "ustr_imp.h"
#include "umutex.h"
#include "unormimp.h"

/* added by synwee ### TODO: remove once the new implementation is finished */
#include "unicode/uchar.h"
#include "unicode/utf16.h"

/* ### TODO: remove this once the new implementation is finished */
static UBool useNewImplementation=FALSE;

U_CAPI void U_EXPORT2
unorm_setNewImplementation(UBool useNew) {
    useNewImplementation=useNew;
}

U_CAPI UBool U_EXPORT2
unorm_usesNewImplementation() {
    return useNewImplementation;
}

/* new implementation ------------------------------------------------------- */

/* Korean Hangul and Jamo constants */
enum {
    JAMO_L_BASE=0x1100,     /* "lead" jamo */
    JAMO_V_BASE=0x1161,     /* "vowel" jamo */
    JAMO_T_BASE=0x11a7,     /* "trail" jamo */

    HANGUL_BASE=0xac00,

    JAMO_L_COUNT=19,
    JAMO_V_COUNT=21,
    JAMO_T_COUNT=28
};

/* load unorm.dat ----------------------------------------------------------- */

/* for a description of the file format, see icu/source/tools/gennorm/store.c */
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

static UBool U_CALLCONV
isAcceptable(void *context,
             const char *type, const char *name,
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
        if(norm32<_NORM_MIN_SPECIAL || _NORM_SURROGATES_TOP<=norm32) {
            c2=0;
        } else {
            /* c is a lead surrogate, get the real norm32 */
            if(p!=limit && (c2=*p, UTF_IS_SECOND_SURROGATE(c2))) {
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
    } else if(p!=start && (c2=*(p-1), UTF_IS_FIRST_SURROGATE(c2))) {
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
 * merge two parts of a UTF-16 string in-place
 * to canonically order (order by combining classes) their concatenation
 *
 * before: [start..p[ is already ordered, and
 *         [p..limit[ is ordered in itself, but
 *                    not in relation to [start..p[
 * after: [start..limit[ is ordered
 *
 * the algorithm is a simple bubble-sort that takes the characters from *p++
 * and inserts them in correct combining class order into the preceding part
 * of the string
 *
 * returns the trailing combining class
 */
static uint8_t
_mergeOrdered(const UChar *start, UChar *p, const UChar *limit) {
    const UChar *pBack, *pPreBack;
    UChar *pSplit, *q;
    UChar c, c2;
    uint8_t cc, prevCC, trailCC=0;

    if(start==p) {
        /* nothing to do */
        if(start!=limit) {
            return _getPrevCC(start, limit);
        } else {
            return 0;
        }
    }

    while(p<limit) {
        pSplit=p;
        cc=_getNextCC(p, limit, c, c2);
        if(cc==0) {
            /* does not bubble back */
            trailCC=0;
            break;
        } else {
            /* search for the insertion point where cc>=prevCC */
            pPreBack=pBack=pSplit;
            prevCC=_getPrevCC(start, pPreBack);
            if(cc>=prevCC) {
                /* does not bubble back */
                trailCC=cc;
                break;
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
                 * [pBack..pSplit[   0..n code points with >cc, move up to insert (c, c2)
                 * [pSplit..p[          1 code point (c, c2) with cc
                 * [p..limit[        0..? code points yet to be bubbled in
                 */

                /* move the code units in between up */
                q=p;
                do {
                    *--q=*--pSplit;
                } while(pBack!=pSplit);

                /* insert (c, c2) */
                *pSplit=c;
                if(c2!=0) {
                    *(pSplit+1)=c2;
                }

                /* we know that the new part is ordered in itself, so we can move start up */
                start=q; /* set it to after where (c, c2) were inserted */
            }
        }
    }

    if(p==limit) {
        /* we know the cc of the last code point */
        return trailCC;
    } else {
        return _getPrevCC(start, limit);
    }
}

/*
 * simpler, more efficient version of _mergeOrdered() -
 * inserts only one code point into the preceding string
 * assume that (c, c2) has not yet inserted at [pSplit..p[
 */
static uint8_t
_insertOrdered(const UChar *start, UChar *pSplit, UChar *p,
               UChar c, UChar c2, uint8_t cc) {
    const UChar *pBack, *pPreBack;
    UChar *q;
    uint8_t prevCC, trailCC=cc;

    if(start<pSplit && cc!=0) {
        /* search for the insertion point where cc>=prevCC */
        pPreBack=pBack=pSplit;
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
             * [pBack..pSplit[   0..n code points with >cc, move up to insert (c, c2)
             * [pSplit..p[          1 code point (c, c2) with cc
             */

            /* move the code units in between up */
            q=p;
            do {
                *--q=*--pSplit;
            } while(pBack!=pSplit);
        }
    }

    /* insert (c, c2) */
    *pSplit=c;
    if(c2!=0) {
        *(pSplit+1)=c2;
    }

    /* we know the cc of the last code point */
    return trailCC;
}

/* quick check functions ---------------------------------------------------- */

static UBool
unorm_checkFCD(const UChar *src,
               int32_t srcLength, 
               UErrorCode *pErrorCode) {
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
            if((limit==NULL || src!=limit) && (c2=*src, UTF_IS_SECOND_SURROGATE(c2))) {
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

static UNormalizationCheckResult
_unorm_quickCheck(const UChar *src,
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
        return unorm_checkFCD(src, srcLength, pErrorCode) ? UNORM_YES : UNORM_NO;
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
        if(_NORM_MIN_SPECIAL<=norm32 && norm32<_NORM_SURROGATES_TOP) {
            /* c is a lead surrogate, get the real norm32 */
            if((limit==NULL || src!=limit) && (c2=*src, UTF_IS_SECOND_SURROGATE(c2))) {
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
                GrowBuffer *growBuffer, void *context,
                UErrorCode *pErrorCode) {
    UChar buffer[3];
    const UChar *limit, *prevSrc, *p, *reorderStart;
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
                                                destIndex)))
            ) {
                do {
                    dest[destIndex++]=*prevSrc++;
                } while(src!=prevSrc);
                reorderStart=dest+destIndex;
            } else {
                /* buffer overflow */
                /* keep incrementing the destIndex for preflighting */
                destIndex+=length;
            }
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
         */
        if(norm32>=_NORM_MIN_HANGUL) {
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
            if(norm32<_NORM_MIN_SPECIAL) {
                c2=0;
                length=1;
            } else {
                /* c is a lead surrogate, get the real norm32 */
                if((limit==NULL || src!=limit) && (c2=*src, UTF_IS_SECOND_SURROGATE(c2))) {
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
                p=(const UChar *)_getExtraData(norm32);
                length=*p++;

                if((norm32&qcMask&_NORM_QC_NFKD)!=0 && length>=0x100) {
                    /* use compatibility decomposition, skip canonical data */
                    p+=((length>>7)&1)+(length&0x7f);
                    length>>=8;
                }

                if(length&0x80) {
                    /* get the lead and trail cc's */
                    UChar bothCCs=*p++;
                    cc=(uint8_t)(bothCCs>>8);
                    trailCC=(uint8_t)bothCCs;
                } else {
                    /* lead and trail cc's are both 0 */
                    cc=trailCC=0;
                }

                length&=0x7f;
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
                                            destIndex)))
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
                /* append the decomposition */
                do {
                    dest[destIndex++]=*p++;
                } while(--length>0);

                if(cc!=0 && cc<prevCC) {
                    /* the decomposition is out of order with respect to the preceding text */
                    trailCC=_mergeOrdered(reorderStart, reorderSplit, dest+destIndex);
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
_decompFCD(const UChar *src, const UChar *decompLimit, const UChar *limit,
           UChar *dest, int32_t &destIndex, int32_t &destCapacity,
           UBool canGrow, GrowBuffer *growBuffer, void *context,
           UErrorCode *pErrorCode) {
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
        if(norm32<_NORM_MIN_SPECIAL) {
            c2=0;
            length=1;
        } else {
            /*
             * reminder: this function is called with [src..decompLimit[
             * not containing any Hangul/Jamo characters,
             * therefore the only specials are lead surrogates
             */
            /* c is a lead surrogate, get the real norm32 */
            if(src!=decompLimit && (c2=*src, UTF_IS_SECOND_SURROGATE(c2))) {
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
            p=(const UChar *)_getExtraData(norm32);
            length=*p++;

            if(length&0x80) {
                /* get the lead and trail cc's */
                UChar bothCCs=*p++;
                cc=(uint8_t)(bothCCs>>8);
                trailCC=(uint8_t)bothCCs;
            } else {
                /* lead and trail cc's are both 0 */
                cc=trailCC=0;
            }

            length&=0x7f;
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
                                            destIndex)))
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
                /* append the decomposition */
                do {
                    dest[destIndex++]=*p++;
                } while(--length>0);

                if(cc!=0 && cc<prevCC) {
                    /* the decomposition is out of order with respect to the preceding text */
                    trailCC=_mergeOrdered(reorderStart, reorderSplit, dest+destIndex);
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
              GrowBuffer *growBuffer, void *context,
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
                                                destIndex)))
            ) {
                do {
                    dest[destIndex++]=*prevSrc++;
                } while(src!=prevSrc);
            } else {
                /* buffer overflow */
                /* keep incrementing the destIndex for preflighting */
                destIndex+=length;
                prevSrc=src;
            }
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
            if((limit==NULL || src!=limit) && (c2=*src, UTF_IS_SECOND_SURROGATE(c2))) {
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
            prevCC=_decompFCD(decompStart, src, limit,
                              dest, destIndex, destCapacity,
                              canGrow, growBuffer, context,
                              pErrorCode);
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

U_CFUNC int32_t
unorm_compose(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              UBool compat, UBool ignoreHangul,
              GrowBuffer *growBuffer, void *context,
              UErrorCode *pErrorCode) {
    /* ### TODO: for now, this is just basically the same as the old unorm_normalize() */
  if(U_FAILURE(*pErrorCode)) return -1;

  /* synwee : removed hard coded conversion */
  Normalizer::EMode normMode = compat ? Normalizer::COMPOSE_COMPAT : Normalizer::COMPOSE;
  if (U_FAILURE(*pErrorCode)) {
    return -1;
  }

  int32_t len = (srcLength == -1 ? u_strlen(src) : srcLength);
  const UnicodeString source(srcLength == -1, src, len);
  UnicodeString dst(dest, 0, destCapacity);
  /* synwee : note quickcheck is added in C ++ normalize method */
  Normalizer::normalize(source, normMode, ignoreHangul ? Normalizer::IGNORE_HANGUL : 0, dst, *pErrorCode);
  return uprv_fillOutputString(dst, dest, destCapacity, pErrorCode);
}




















/* old implementation ------------------------------------------------------- */

/* added by synwee for trie manipulation*/
#define STAGE_1_SHIFT_            10
#define STAGE_2_SHIFT_            4
#define STAGE_2_MASK_AFTER_SHIFT_ 0x3F
#define STAGE_3_MASK_             0xF
#define LAST_BYTE_MASK_           0xFF
#define SECOND_LAST_BYTE_SHIFT_   8

/* added by synwee for fast route in quickcheck and fcd */
#define NFC_ZERO_CC_BLOCK_LIMIT_  0x300

/*
 * for a description of the file format, 
 * see icu/source/tools/genqchk/genqchk.c
 */
#define QCHK_DATA_NAME "qchk"
#define FCHK_DATA_NAME "fchk"
#define DATA_TYPE "dat"

static UDataMemory *quickcheckData = NULL;
static UDataMemory *fcdcheckData   = NULL;

/**
* Authentication values
*/
static const uint8_t QCHK_DATA_FORMAT_[]    = {0x71, 0x63, 0x68, 0x6b};
static const uint8_t FCHK_DATA_FORMAT_[]    = {0x66, 0x63, 0x68, 0x6b};
static const uint8_t QCHK_FORMAT_VERSION_[] = {1, 0, 0, 0};
static const uint8_t FCHK_FORMAT_VERSION_[] = {1, 0, 0, 0};

/** 
* index values loaded from qchk.dat.
* static uint16_t indexes[8]; 
*/
enum {
    QCHK_INDEX_STAGE_2_BITS,
    QCHK_INDEX_STAGE_3_BITS,
    QCHK_INDEX_MIN_VALUES_SIZE,
    QCHK_INDEX_STAGE_1_INDEX,
    QCHK_INDEX_STAGE_2_INDEX,
    QCHK_INDEX_STAGE_3_INDEX
};

/** 
* index values loaded from qchk.dat.
* static uint16_t indexes[8]; 
*/
enum {
    FCHK_INDEX_STAGE_2_BITS,
    FCHK_INDEX_STAGE_3_BITS,
    FCHK_INDEX_STAGE_1_INDEX,
    FCHK_INDEX_STAGE_2_INDEX,
    FCHK_INDEX_STAGE_3_INDEX
};

/**
* Array of mask for determining normalization quick check values.
* Indexes follows the values in UNormalizationMode
*/
static const uint8_t QCHK_MASK_[] = {0, 0, 0x11, 0x22, 0x44, 0x88};
/** 
* Array of minimum codepoints that has UNORM_MAYBE or UNORM_NO quick check
* values. Indexes follows the values in UNormalizationMode.
* Generated values! Edit at your own risk.
*/
static const UChar32 *QCHK_MIN_VALUES_;

/**
* Flag to indicate if data has been loaded 
*/
static UBool isQuickCheckLoaded = FALSE;
static UBool isFCDCheckLoaded   = FALSE;

/**
* Minimum value to determine if quickcheck value contains a MAYBE
*/
static const uint8_t MIN_UNORM_MAYBE_ = 0x10;

/**
* Array of normalization form corresponding to the index code point.
* Hence codepoint 0xABCD will have normalization form QUICK_CHECK_DATA[0xABCD].
* UQUICK_CHECK_DATA[0xABCD] is a byte containing 2 sets of 4 bits information
* representing UNORM_MAYBE and UNORM_YES.<br>
* bits 1 2 3 4                        5678<br>
*      NFKC NFC NFKD NFD MAYBES       NFKC NFC NFKD NFD YES<br>
* ie if UQUICK_CHECK_DATA[0xABCD] = 10000001, this means that 0xABCD is in 
* NFD form and maybe in NFKC form
*/
static const uint16_t *QCHK_STAGE_1_;
static const uint16_t *QCHK_STAGE_2_;
static const uint8_t  *QCHK_STAGE_3_;

/**
* Trie data for FCD.
* Each index corresponds to each code point. 
* Trie value is the combining class of the first and the last character of the
* NFD of the codepoint.
* size uint16_t for the first 2 stages instead of uint32_t to reduce size.
*/
static const uint16_t *FCHK_STAGE_1_;
static const uint16_t *FCHK_STAGE_2_;
static const uint16_t *FCHK_STAGE_3_;

U_CAPI int32_t
unorm_normalize(const UChar*            src,
        int32_t                 srcLength, 
        UNormalizationMode      mode, 
        int32_t                 option,
        UChar*                  dest,
        int32_t                 destCapacity,
        UErrorCode*             pErrorCode)
{
    if(useNewImplementation) {
        UBool ignoreHangul;

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

        ignoreHangul= (option&UNORM_IGNORE_HANGUL)!=0;

        switch(mode) {
        case UNORM_NFD:
            return unorm_decompose(dest, destCapacity,
                                   src, srcLength,
                                   FALSE, ignoreHangul,
                                   NULL, NULL,
                                   pErrorCode);
        case UNORM_NFKD:
            return unorm_decompose(dest, destCapacity,
                                   src, srcLength,
                                   TRUE, ignoreHangul,
                                   NULL, NULL,
                                   pErrorCode);
        case UNORM_NFC:
            return unorm_compose(dest, destCapacity,
                                 src, srcLength,
                                 FALSE, ignoreHangul,
                                 NULL, NULL,
                                 pErrorCode);
        case UNORM_NFKC:
            return unorm_compose(dest, destCapacity,
                                 src, srcLength,
                                 TRUE, ignoreHangul,
                                 NULL, NULL,
                                 pErrorCode);
        case UNORM_FCD:
            return unorm_makeFCD(dest, destCapacity,
                                 src, srcLength,
                                 NULL, NULL,
                                 pErrorCode);
        default:
            *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return 0;
        }
    }

  if(U_FAILURE(*pErrorCode)) return -1;

  /* synwee : removed hard coded conversion */
  Normalizer::EMode normMode = Normalizer::getNormalizerEMode(mode, *pErrorCode);
  if (U_FAILURE(*pErrorCode))
    return -1;

  int32_t len = (srcLength == -1 ? u_strlen(src) : srcLength);
  const UnicodeString source(srcLength == -1, src, len);
  UnicodeString dst(dest, 0, destCapacity);
  /* synwee : note quickcheck is added in C ++ normalize method */
  if ((option & UNORM_IGNORE_HANGUL) != 0)
    option = Normalizer::IGNORE_HANGUL;
  Normalizer::normalize(source, normMode, option, dst, *pErrorCode);
  return uprv_fillOutputString(dst, dest, destCapacity, pErrorCode);
}

static UBool U_CALLCONV
isQuickCheckAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    if (pInfo->size >= 20 &&
        pInfo->isBigEndian == U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily == U_CHARSET_FAMILY &&
        (uprv_memcmp(pInfo->dataFormat, QCHK_DATA_FORMAT_, 
                     sizeof(QCHK_DATA_FORMAT_)) == 0) && 
        /*
        pInfo->dataFormat[0] == 0x71 && 
        pInfo->dataFormat[1] == 0x63 &&
        pInfo->dataFormat[2] == 0x68 &&
        pInfo->dataFormat[3] == 0x6b &&
        pInfo->formatVersion[0] == 1
        */
        (uprv_memcmp(pInfo->formatVersion, QCHK_FORMAT_VERSION_, 
                     sizeof(QCHK_FORMAT_VERSION_)) == 0)) {
        return TRUE;
    } else {
        context = NULL;
        type    = NULL;
        name    = NULL;
        return FALSE;
    }
}

static UBool
loadQuickCheckData(UErrorCode *error) {
    /* load quickcheck data from file if necessary */
    if (!isQuickCheckLoaded && U_SUCCESS(*error)) {
        UDataMemory *data;

        /* open the data outside the mutex block */
        data = udata_openChoice(NULL, DATA_TYPE, QCHK_DATA_NAME, 
                                isQuickCheckAcceptable, NULL, error);
        if (U_FAILURE(*error)) {
            return isQuickCheckLoaded = FALSE;
        }

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if (quickcheckData == NULL) {
            const uint16_t *temp = (const uint16_t *)udata_getMemory(data);
            const uint16_t *indexes = temp;
    
            quickcheckData = data;

            temp += 8;
            QCHK_MIN_VALUES_ = (const UChar32 *)temp;
            QCHK_STAGE_1_    = temp + indexes[QCHK_INDEX_STAGE_1_INDEX];
            QCHK_STAGE_2_    = temp + indexes[QCHK_INDEX_STAGE_2_INDEX];
            QCHK_STAGE_3_    = (const uint8_t *)(temp + 
                                           indexes[QCHK_INDEX_STAGE_3_INDEX]);
            data = NULL;
        }
        umtx_unlock(NULL);

        isQuickCheckLoaded = TRUE;

        /* if a different thread set it first, then close the extra data */
        if (data != NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return isQuickCheckLoaded;
}

/**
 * Performing quick check on a string, to quickly determine if the string is 
 * in a particular normalization format.
 * Three types of result can be returned UNORM_YES, UNORM_NO or
 * UNORM_MAYBE. Result UNORM_YES indicates that the argument
 * string is in the desired normalized format, UNORM_NO determines that
 * argument string is not in the desired normalized format. A 
 * UNORM_MAYBE result indicates that a more thorough check is required, 
 * the user may have to put the string in its normalized form and compare the 
 * results.
 * @param source       string for determining if it is in a normalized format
 * @param sourcelength length of source to test
 * @param mode         normalization format from the enum UNormalizationMode
 * @param status A pointer to an UErrorCode to receive any errors
 * @return UNORM_YES, UNORM_NO or UNORM_MAYBE
 */
U_CAPI UNormalizationCheckResult
unorm_quickCheck(const UChar             *source,
                       int32_t            sourcelength, 
                       UNormalizationMode mode, 
                       UErrorCode*        status)
{
  uint8_t                    oldcombiningclass = 0;
  uint8_t                    combiningclass;
  uint8_t                    quickcheckvalue;
  uint8_t                    mask              = QCHK_MASK_[mode];
  UChar32                    min;
  UChar32                    codepoint;
  UNormalizationCheckResult  result            = UNORM_YES;
  const UChar                *psource;
  const UChar                *pend             = 0;

  if(useNewImplementation) {
    return _unorm_quickCheck(source, sourcelength, mode, status);
  }

  if (!loadQuickCheckData(status) || U_FAILURE(*status)) {
      return UNORM_MAYBE;
  }

  min = QCHK_MIN_VALUES_[mode];
  
  /* checking argument*/
  if (mode >= UNORM_MODE_COUNT || mode < UNORM_NONE) {
    *status = U_ILLEGAL_ARGUMENT_ERROR;
    return UNORM_MAYBE;
  }

  if (sourcelength >= 0) {
    psource = source;
    pend    = source + sourcelength;
    for (;;) {
      if (psource >= pend) {
        return UNORM_YES;
      }
      /* fast route : since codepoints < min has combining class 0 and YES
         looking at the minimum values, surrogates are not a problem */
      if (*psource >= min) {
        break;
      }
      psource ++;
    }
  }
  else {
    psource = source;
    for (;;) {
      if (*psource == 0) {
        return UNORM_YES;
      }
      /* fast route : since codepoints < min has combining class 0 and YES 
         looking at the minimum values, surrogates are not a problem */
      if (*psource >= min) {
        break;
      }
      psource ++;
    }
  }

  if (sourcelength >= 0) {
    for (;;) {
      int count = 0;

      if (psource >= pend) {
        break;
      }
      UTF_NEXT_CHAR(psource, count, pend - psource, codepoint);      
      combiningclass = u_getCombiningClass(codepoint);
      /* not in canonical order */

      if (oldcombiningclass > combiningclass && combiningclass != 0) {
        return UNORM_NO;
      }

      oldcombiningclass = combiningclass;

      /* trie access */
      quickcheckvalue = (uint8_t)(QCHK_STAGE_3_[
          QCHK_STAGE_2_[QCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
          ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
          (codepoint & STAGE_3_MASK_)] & mask);
      /* value is a byte containing 2 sets of 4 bits information.
         bits 1 2 3 4                        5678<br>
         NFKC NFC NFKD NFD MAYBES       NFKC NFC NFKD NFD YES<br>
         ie if quick[0xABCD] = 10000001, this means that 0xABCD is in NFD form 
         and maybe in NFKC form. */
      if (quickcheckvalue == 0) {
        return UNORM_NO;
      }
      if (quickcheckvalue >= MIN_UNORM_MAYBE_) {
        result = UNORM_MAYBE;
      }
      psource += count;
    }
  }
  else {
    for (;;) {
      int count = 0;
      UTF_NEXT_CHAR(psource, count, pend - psource, codepoint);      
      if (codepoint == 0) {
        break;
      }
      
      combiningclass = u_getCombiningClass(codepoint);
      /* not in canonical order */

      if (oldcombiningclass > combiningclass && combiningclass != 0) {
        return UNORM_NO;
      }

      oldcombiningclass = combiningclass;

      /* trie access */
      quickcheckvalue = (uint8_t)(QCHK_STAGE_3_[
          QCHK_STAGE_2_[QCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
          ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
          (codepoint & STAGE_3_MASK_)] & mask);
      /* value is a byte containing 2 sets of 4 bits information.
         bits 1 2 3 4                        5678<br>
         NFKC NFC NFKD NFD MAYBES       NFKC NFC NFKD NFD YES<br>
         ie if quick[0xABCD] = 10000001, this means that 0xABCD is in NFD form 
         and maybe in NFKC form. */
      if (quickcheckvalue == 0) {
        return UNORM_NO;
      }
      if (quickcheckvalue >= MIN_UNORM_MAYBE_) {
        result = UNORM_MAYBE;
      }
      psource += count;
    }
  }
  
  return result;
}

/* private methods ---------------------------------------------------------- */

static UBool U_CALLCONV
isFCDCheckAcceptable(void *context,
             const char *type, const char *name,
             const UDataInfo *pInfo) {
    if(
        pInfo->size >= 20 &&
        pInfo->isBigEndian == U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily == U_CHARSET_FAMILY &&
        (uprv_memcmp(pInfo->dataFormat, FCHK_DATA_FORMAT_, 
                     sizeof(FCHK_DATA_FORMAT_)) == 0) && 
        /*
        pInfo->dataFormat[0] == 0x71 && 
        pInfo->dataFormat[1] == 0x63 &&
        pInfo->dataFormat[2] == 0x68 &&
        pInfo->dataFormat[3] == 0x6b &&
        pInfo->formatVersion[0] == 1
        */
        (uprv_memcmp(pInfo->formatVersion, FCHK_FORMAT_VERSION_, 
                     sizeof(FCHK_FORMAT_VERSION_)) == 0)) {
        return TRUE;
    } else {
        context = NULL;
        type    = NULL;
        name    = NULL;
        return FALSE;
    }
}

static UBool 
loadFCDCheckData(UErrorCode *error) {
    /* load fcdcheck data from file if necessary */
    if (!isFCDCheckLoaded && U_SUCCESS(*error)) {
        UDataMemory *data;

        /* open the data outside the mutex block */
        data = udata_openChoice(NULL, DATA_TYPE, FCHK_DATA_NAME, 
                                isFCDCheckAcceptable, NULL, error);
        if (U_FAILURE(*error)) {
            return isFCDCheckLoaded = FALSE;
        }

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if (fcdcheckData == NULL) {
            const uint16_t *temp = (const uint16_t *)udata_getMemory(data);
            const uint16_t *indexes = temp;
    
            fcdcheckData = data;

            temp += 8;
            FCHK_STAGE_1_    = temp + indexes[FCHK_INDEX_STAGE_1_INDEX];
            FCHK_STAGE_2_    = temp + indexes[FCHK_INDEX_STAGE_2_INDEX];
            FCHK_STAGE_3_    = (const uint16_t *)(temp + 
                                           indexes[FCHK_INDEX_STAGE_3_INDEX]);
            data = NULL;
        }
        umtx_unlock(NULL);

        isFCDCheckLoaded = TRUE;

        /* if a different thread set it first, then close the extra data */
        if (data != NULL) {
            udata_close(data); /* NULL if it was set correctly */
        }
    }

    return isFCDCheckLoaded;
}

/**
* Gets the stage 1 data for checkFCD.
* @param error status 
* @return checkFCD data stage 1, null if data can not be loaded
*/
U_CAPI const uint16_t * getFCHK_STAGE_1_(UErrorCode *error)
{
    if (loadFCDCheckData(error)) {
        return FCHK_STAGE_1_;
    }
    return NULL;
}

/**
* Gets the stage 2 data for checkFCD.
* @param error status 
* @return checkFCD data stage 2, null if data can not be loaded
*/
U_CAPI const uint16_t * getFCHK_STAGE_2_(UErrorCode *error)
{
    if (loadFCDCheckData(error)) {
        return FCHK_STAGE_2_;
    }
    return NULL;
}

/**
* Gets the stage 3 data for checkFCD.
* @param error status 
* @return checkFCD data stage 3, null if data can not be loaded
*/
U_CAPI const uint16_t * getFCHK_STAGE_3_(UErrorCode *error)
{
    if (loadFCDCheckData(error)) {
        return FCHK_STAGE_3_;
    }
    return NULL;
}

/**
* Private method which performs a quick FCD check on a string, to quickly 
* determine if a string is in a required FCD format.
* FCD is the set of strings such that for each character in the string, 
* decomposition without any canonical reordering will produce a NFD.
* @param source       string for determining if it is in a normalized format
* @param sourcelength length of source to test
* @paran mode         normalization format from the enum UNormalizationMode
* @param status       A pointer to an UErrorCode to receive any errors
* @return TRUE if source is in FCD format, FALSE otherwise
*/
U_CAPI UBool 
checkFCD(const UChar* source, int32_t sourcelength, UErrorCode* status)
{
    if(useNewImplementation) {
        return UNORM_YES==unorm_quickCheck(source, sourcelength, UNORM_FCD, status);
    }

        UChar32  codepoint;
  const UChar   *psource;
  const UChar   *pend = 0;
        uint8_t  oldfcdtrail = 0;
        uint16_t fcd = 0;
  
  if (!loadFCDCheckData(status) || U_FAILURE(*status)) {
    return FALSE;
        }

  if (sourcelength >= 0) {
    psource = source;
    pend    = source + sourcelength;
    for (;;) {
      if (psource >= pend) {
        return TRUE;
      }
      /* fast route : since codepoints < NFC_ZER_CC_BLOCK_LIMIT_ has 
         combining class 0.
         looking at the minimum values, surrogates are not a problem */
      if (*psource >= NFC_ZERO_CC_BLOCK_LIMIT_) {
        break;
      }
      psource ++;
    }
  }
  else {
    psource = source;
    for (;;) {
      if (*psource == 0) {
        return TRUE;
      }
      /* fast route : since codepoints < min has combining class 0 and YES 
         looking at the minimum values, surrogates are not a problem */
      if (*psource >= NFC_ZERO_CC_BLOCK_LIMIT_) {
        break;
      }
      psource ++;
    }
  }

  /* not end of string and yet failed simple compare 
     safe to shift back one char because the previous char has to be < 0x300 or the
     start of a string */
  if (psource == source) {
    oldfcdtrail = 0;
  }
  else {
    codepoint = *(psource - 1);
    oldfcdtrail = (uint8_t)(FCHK_STAGE_3_[
                  FCHK_STAGE_2_[FCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
                  ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] 
                  + (codepoint & STAGE_3_MASK_)] & LAST_BYTE_MASK_);
  }

  if (sourcelength >= 0) {
    for (;;) {
      int count = 0;
      uint8_t lead;

      if (psource >= pend) {
        return TRUE;
      }
      
      UTF_NEXT_CHAR(psource, count, pend - psource, codepoint);

      /* trie access */
      fcd = FCHK_STAGE_3_[
            FCHK_STAGE_2_[FCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
              ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
            (codepoint & STAGE_3_MASK_)];
      lead = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);
    
      if (lead != 0 && oldfcdtrail > lead) {
        return FALSE;
      }
      oldfcdtrail = (uint8_t)(fcd & LAST_BYTE_MASK_);
    
      psource += count;
    }
  }
  else {
    for (;;) {
      int count = 0;
      uint8_t lead;

      UTF_NEXT_CHAR(psource, count, pend - psource, codepoint);
      if (codepoint == 0) {
        return TRUE;
      }
      /* trie access */
      fcd = FCHK_STAGE_3_[
            FCHK_STAGE_2_[FCHK_STAGE_1_[codepoint >> STAGE_1_SHIFT_] + 
              ((codepoint >> STAGE_2_SHIFT_) & STAGE_2_MASK_AFTER_SHIFT_)] +
            (codepoint & STAGE_3_MASK_)];
    
      lead = (uint8_t)(fcd >> SECOND_LAST_BYTE_SHIFT_);
    
      if (lead != 0 && oldfcdtrail > lead) {
        return FALSE;
      }
      oldfcdtrail = (uint8_t)(fcd & LAST_BYTE_MASK_);
      psource += count;
    }
  }
  return TRUE;
}
