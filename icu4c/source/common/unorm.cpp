/*
******************************************************************************
* Copyright (c) 1996-2004, International Business Machines
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

// moved up to make unorm_cmpEquivFold work without normalization
#include "unicode/ustring.h"
#include "unormimp.h"
#include "ustr_imp.h"

#if !UCONFIG_NO_NORMALIZATION

#include "unicode/udata.h"
#include "unicode/uchar.h"
#include "unicode/uiter.h"
#include "unicode/uniset.h"
#include "unicode/usetiter.h"
#include "unicode/unorm.h"
#include "cmemory.h"
#include "umutex.h"
#include "utrie.h"
#include "unicode/uset.h"
#include "udataswp.h"

/*
 * Status of tailored normalization
 *
 * This was done initially for investigation on Unicode public review issue 7
 * (http://www.unicode.org/review/). See Jitterbug 2481.
 * While the UTC at meeting #94 (2003mar) did not take up the issue, this is
 * a permanent feature in ICU 2.6 in support of IDNA which requires true
 * Unicode 3.2 normalization.
 * (NormalizationCorrections are rolled into IDNA mapping tables.)
 *
 * Tailored normalization as implemented here allows to "normalize less"
 * than full Unicode normalization would.
 * Based internally on a UnicodeSet of code points that are
 * "excluded from normalization", the normalization functions leave those
 * code points alone ("inert"). This means that tailored normalization
 * still transforms text into a canonically equivalent form.
 * It does not add decompositions to code points that do not have any or
 * change decomposition results.
 *
 * Any function that searches for a safe boundary has not been touched,
 * which means that these functions will be over-pessimistic when
 * exclusions are applied.
 * This should not matter because subsequent checks and normalizations
 * do apply the exclusions; only a little more of the text may be processed
 * than necessary under exclusions.
 *
 * Normalization exclusions have the following effect on excluded code points c:
 * - c is not decomposed
 * - c is not a composition target
 * - c does not combine forward or backward for composition
 *   except that this is not implemented for Jamo
 * - c is treated as having a combining class of 0
 */
#define LENGTHOF(array) (sizeof(array)/sizeof((array)[0]))

/*
 * This new implementation of the normalization code loads its data from
 * unorm.dat, which is generated with the gennorm tool.
 * The format of that file is described in unormimp.h .
 */

/* -------------------------------------------------------------------------- */

enum {
    _STACK_BUFFER_CAPACITY=100
};

/*
 * Constants for the bit fields in the options bit set parameter.
 * These need not be public.
 * A user only needs to know the currently assigned values.
 * The number and positions of reserved bits per field can remain private
 * and may change in future implementations.
 */
enum {
    _NORM_OPTIONS_NX_MASK=0x1f,
    _NORM_OPTIONS_UNICODE_MASK=0xe0,
    _NORM_OPTIONS_SETS_MASK=0xff,

    _NORM_OPTIONS_UNICODE_SHIFT=5
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
#define DATA_TYPE "icu"

static UDataMemory *normData=NULL;
static UErrorCode dataErrorCode=U_ZERO_ERROR;
static int8_t haveNormData=0;

static int32_t indexes[_NORM_INDEX_TOP]={ 0 };
static UTrie normTrie={ 0,0,0,0,0,0,0 }, fcdTrie={ 0,0,0,0,0,0,0 }, auxTrie={ 0,0,0,0,0,0,0 };

/*
 * pointers into the memory-mapped unorm.icu
 */
static const uint16_t *extraData=NULL,
                      *combiningTable=NULL,
                      *canonStartSets=NULL;

static uint8_t formatVersion[4]={ 0, 0, 0, 0 };
static UBool formatVersion_2_1=FALSE, formatVersion_2_2=FALSE;

/* the Unicode version of the normalization data */
static UVersionInfo dataVersion={ 0, 0, 0, 0 };

/* cache UnicodeSets for each combination of exclusion flags */
static UnicodeSet *nxCache[_NORM_OPTIONS_SETS_MASK+1]={ NULL };

U_CDECL_BEGIN

UBool
unorm_cleanup() {
    int32_t i;

    if(normData!=NULL) {
        udata_close(normData);
        normData=NULL;
    }
    dataErrorCode=U_ZERO_ERROR;
    haveNormData=0;

    for(i=0; i<(int32_t)LENGTHOF(nxCache); ++i) {
        delete nxCache[i];
    }
    uprv_memset(nxCache, 0, sizeof(nxCache));

    return TRUE;
}

/* normTrie: 32-bit trie result may contain a special extraData index with the folding offset */
static int32_t U_CALLCONV
getFoldingNormOffset(uint32_t norm32) {
    if(isNorm32LeadSurrogate(norm32)) {
        return
            UTRIE_BMP_INDEX_LENGTH+
                (((int32_t)norm32>>(_NORM_EXTRA_SHIFT-UTRIE_SURROGATE_BLOCK_BITS))&
                 (0x3ff<<UTRIE_SURROGATE_BLOCK_BITS));
    } else {
        return 0;
    }
}

/* fcdTrie: the folding offset is the lead FCD value itself */
static int32_t U_CALLCONV
getFoldingFCDOffset(uint32_t data) {
    return (int32_t)data;
}

/* auxTrie: the folding offset is in bits 9..0 of the 16-bit trie result */
static int32_t U_CALLCONV
getFoldingAuxOffset(uint32_t data) {
    return (int32_t)(data&_NORM_AUX_FNC_MASK)<<UTRIE_SURROGATE_BLOCK_BITS;
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
        pInfo->formatVersion[0]==2 &&
        pInfo->formatVersion[2]==UTRIE_SHIFT &&
        pInfo->formatVersion[3]==UTRIE_INDEX_SHIFT
    ) {
        uprv_memcpy(formatVersion, pInfo->formatVersion, 4);
        uprv_memcpy(dataVersion, pInfo->dataVersion, 4);
        return TRUE;
    } else {
        return FALSE;
    }
}

static UBool U_CALLCONV
_enumPropertyStartsRange(const void *context, UChar32 start, UChar32 /*limit*/, uint32_t /*value*/) {
    /* add the start code point to the USet */
    uset_add((USet *)context, start);
    return TRUE;
}

U_CDECL_END

static int8_t
loadNormData(UErrorCode &errorCode) {
    /* load Unicode normalization data from file */

    /*
     * This lazy intialization with double-checked locking (without mutex protection for
     * haveNormData==0) is transiently unsafe under certain circumstances.
     * Check the readme and use u_init() if necessary.
     *
     * While u_init() initializes the main normalization data via this functions,
     * it does not do so for exclusion sets (which are fully mutexed).
     * This is because
     * - there can be many exclusion sets
     * - they are rarely used
     * - they are not usually used in execution paths that are
     *   as performance-sensitive as others
     *   (e.g., IDNA takes more time than unorm_quickCheck() anyway)
     */
    if(haveNormData==0) {
        UTrie _normTrie={ 0,0,0,0,0,0,0 }, _fcdTrie={ 0,0,0,0,0,0,0 }, _auxTrie={ 0,0,0,0,0,0,0 };
        UDataMemory *data;
        const int32_t *p=NULL;
        const uint8_t *pb;

        if(&errorCode==NULL || U_FAILURE(errorCode)) {
            return 0;
        }

        /* open the data outside the mutex block */
        data=udata_openChoice(NULL, DATA_TYPE, DATA_NAME, isAcceptable, NULL, &errorCode);
        dataErrorCode=errorCode;
        if(U_FAILURE(errorCode)) {
            return haveNormData=-1;
        }

        p=(const int32_t *)udata_getMemory(data);
        pb=(const uint8_t *)(p+_NORM_INDEX_TOP);
        utrie_unserialize(&_normTrie, pb, p[_NORM_INDEX_TRIE_SIZE], &errorCode);
        _normTrie.getFoldingOffset=getFoldingNormOffset;

        pb+=p[_NORM_INDEX_TRIE_SIZE]+p[_NORM_INDEX_UCHAR_COUNT]*2+p[_NORM_INDEX_COMBINE_DATA_COUNT]*2;
        utrie_unserialize(&_fcdTrie, pb, p[_NORM_INDEX_FCD_TRIE_SIZE], &errorCode);
        _fcdTrie.getFoldingOffset=getFoldingFCDOffset;

        if(p[_NORM_INDEX_FCD_TRIE_SIZE]!=0) {
            pb+=p[_NORM_INDEX_FCD_TRIE_SIZE];
            utrie_unserialize(&_auxTrie, pb, p[_NORM_INDEX_AUX_TRIE_SIZE], &errorCode);
            _auxTrie.getFoldingOffset=getFoldingAuxOffset;
        }

        if(U_FAILURE(errorCode)) {
            dataErrorCode=errorCode;
            udata_close(data);
            return haveNormData=-1;
        }

        /* in the mutex block, set the data for this process */
        umtx_lock(NULL);
        if(normData==NULL) {
            normData=data;
            data=NULL;

            uprv_memcpy(&indexes, p, sizeof(indexes));
            uprv_memcpy(&normTrie, &_normTrie, sizeof(UTrie));
            uprv_memcpy(&fcdTrie, &_fcdTrie, sizeof(UTrie));
            uprv_memcpy(&auxTrie, &_auxTrie, sizeof(UTrie));
        } else {
            p=(const int32_t *)udata_getMemory(normData);
        }

        /* initialize some variables */
        extraData=(uint16_t *)((uint8_t *)(p+_NORM_INDEX_TOP)+indexes[_NORM_INDEX_TRIE_SIZE]);
        combiningTable=extraData+indexes[_NORM_INDEX_UCHAR_COUNT];
        formatVersion_2_1=formatVersion[0]>2 || (formatVersion[0]==2 && formatVersion[1]>=1);
        formatVersion_2_2=formatVersion[0]>2 || (formatVersion[0]==2 && formatVersion[1]>=2);
        if(formatVersion_2_1) {
            canonStartSets=combiningTable+
                indexes[_NORM_INDEX_COMBINE_DATA_COUNT]+
                (indexes[_NORM_INDEX_FCD_TRIE_SIZE]+indexes[_NORM_INDEX_AUX_TRIE_SIZE])/2;
        }
        haveNormData=1;
        umtx_unlock(NULL);

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
        return fcdTrie.index;
    } else {
        return NULL;
    }
}

/* data access primitives --------------------------------------------------- */

static inline uint32_t
_getNorm32(UChar c) {
    return UTRIE_GET32_FROM_LEAD(&normTrie, c);
}

static inline uint32_t
_getNorm32FromSurrogatePair(uint32_t norm32, UChar c2) {
    /*
     * the surrogate index in norm32 stores only the number of the surrogate index block
     * see gennorm/store.c/getFoldedNormValue()
     */
    norm32=
        UTRIE_BMP_INDEX_LENGTH+
            ((norm32>>(_NORM_EXTRA_SHIFT-UTRIE_SURROGATE_BLOCK_BITS))&
             (0x3ff<<UTRIE_SURROGATE_BLOCK_BITS));
    return UTRIE_GET32_FROM_OFFSET_TRAIL(&normTrie, norm32, c2);
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
    return UTRIE_GET16_FROM_LEAD(&fcdTrie, c);
}

static inline uint16_t
_getFCD16FromSurrogatePair(uint16_t fcd16, UChar c2) {
    /* the surrogate index in fcd16 is an absolute offset over the start of stage 1 */
    return UTRIE_GET16_FROM_OFFSET_TRAIL(&fcdTrie, fcd16, c2);
}

static inline const uint16_t *
_getExtraData(uint32_t norm32) {
    return extraData+(norm32>>_NORM_EXTRA_SHIFT);
}

/* normalization exclusion sets --------------------------------------------- */

/*
 * Normalization exclusion UnicodeSets are used for tailored normalization;
 * see the comment near the beginning of this file.
 *
 * By specifying one or several sets of code points,
 * those code points become inert for normalization.
 */

static const UnicodeSet *
internalGetNXHangul(UErrorCode &errorCode) {
    /* internal function, does not check for incoming U_FAILURE */

    UBool isCached;

    /* do this because double-checked locking is broken */
    umtx_lock(NULL);
    isCached=nxCache[UNORM_NX_HANGUL]!=NULL;
    umtx_unlock(NULL);

    if(!isCached) {
        UnicodeSet *set=new UnicodeSet(0xac00, 0xd7a3);
        if(set==NULL) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }

        umtx_lock(NULL);
        if(nxCache[UNORM_NX_HANGUL]==NULL) {
            nxCache[UNORM_NX_HANGUL]=set;
            set=NULL;
        }
        umtx_unlock(NULL);

        delete set;
    }

    return nxCache[UNORM_NX_HANGUL];
}

static const UnicodeSet *
internalGetNXCJKCompat(UErrorCode &errorCode) {
    /* internal function, does not check for incoming U_FAILURE */

    UBool isCached;

    /* do this because double-checked locking is broken */
    umtx_lock(NULL);
    isCached=nxCache[UNORM_NX_CJK_COMPAT]!=NULL;
    umtx_unlock(NULL);

    if(!isCached) {
        /* build a set from [CJK Ideographs]&[has canonical decomposition] */
        UnicodeSet *set, *hasDecomp;

        set=new UnicodeSet(UNICODE_STRING("[:Ideographic:]", 15), errorCode);
        if(set==NULL) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        if(U_FAILURE(errorCode)) {
            delete set;
            return NULL;
        }

        /* start with an empty set for [has canonical decomposition] */
        hasDecomp=new UnicodeSet();
        if(hasDecomp==NULL) {
            delete set;
            errorCode=U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }

        /* iterate over all ideographs and remember which canonically decompose */
        UnicodeSetIterator it(*set);
        UChar32 start, end;
        uint32_t norm32;

        while(it.nextRange() && !it.isString()) {
            start=it.getCodepoint();
            end=it.getCodepointEnd();
            while(start<=end) {
                UTRIE_GET32(&normTrie, start, norm32);
                if(norm32&_NORM_QC_NFD) {
                    hasDecomp->add(start);
                }
                ++start;
            }
        }

        /* hasDecomp now contains all ideographs that decompose canonically */

        umtx_lock(NULL);
        if(nxCache[UNORM_NX_CJK_COMPAT]==NULL) {
            nxCache[UNORM_NX_CJK_COMPAT]=hasDecomp;
            hasDecomp=NULL;
        }
        umtx_unlock(NULL);

        delete hasDecomp;
        delete set;
    }

    return nxCache[UNORM_NX_CJK_COMPAT];
}

static const UnicodeSet *
internalGetNXUnicode(uint32_t options, UErrorCode &errorCode) {
    /* internal function, does not check for incoming U_FAILURE */
    options&=_NORM_OPTIONS_UNICODE_MASK;
    if(options==0) {
        return NULL;
    }

    UBool isCached;

    /* do this because double-checked locking is broken */
    umtx_lock(NULL);
    isCached=nxCache[options]!=NULL;
    umtx_unlock(NULL);

    if(!isCached) {
        /* build a set with all code points that were not designated by the specified Unicode version */
        UnicodeSet *set;

        switch(options) {
        case UNORM_UNICODE_3_2:
            set=new UnicodeSet(UNICODE_STRING("[:^Age=3.2:]", 12), errorCode);
            break;
        default:
            errorCode=U_ILLEGAL_ARGUMENT_ERROR;
            return NULL;
        }

        if(set==NULL) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        if(U_FAILURE(errorCode)) {
            delete set;
            return NULL;
        }

        umtx_lock(NULL);
        if(nxCache[options]==NULL) {
            nxCache[options]=set;
            set=NULL;
        }
        umtx_unlock(NULL);

        delete set;
    }

    return nxCache[options];
}

/* Get a decomposition exclusion set. The data must be loaded. */
static const UnicodeSet *
internalGetNX(int32_t options, UErrorCode &errorCode) {
    options&=_NORM_OPTIONS_SETS_MASK;

    UBool isCached;

    /* do this because double-checked locking is broken */
    umtx_lock(NULL);
    isCached=nxCache[options]!=NULL;
    umtx_unlock(NULL);

    if(!isCached) {
        /* return basic sets */
        if(options==UNORM_NX_HANGUL) {
            return internalGetNXHangul(errorCode);
        }
        if(options==UNORM_NX_CJK_COMPAT) {
            return internalGetNXCJKCompat(errorCode);
        }
        if((options&_NORM_OPTIONS_UNICODE_MASK)!=0 && (options&_NORM_OPTIONS_NX_MASK)==0) {
            return internalGetNXUnicode(options, errorCode);
        }

        /* build a set from multiple subsets */
        UnicodeSet *set;
        const UnicodeSet *other;

        set=new UnicodeSet();
        if(set==NULL) {
            errorCode=U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }

        if((options&UNORM_NX_HANGUL)!=0 && NULL!=(other=internalGetNXHangul(errorCode))) {
            set->addAll(*other);
        }
        if((options&UNORM_NX_CJK_COMPAT)!=0 && NULL!=(other=internalGetNXCJKCompat(errorCode))) {
            set->addAll(*other);
        }
        if((options&_NORM_OPTIONS_UNICODE_MASK)!=0 && NULL!=(other=internalGetNXUnicode(options, errorCode))) {
            set->addAll(*other);
        }

        if(U_FAILURE(errorCode)) {
            delete set;
            return NULL;
        }

        umtx_lock(NULL);
        if(nxCache[options]==NULL) {
            nxCache[options]=set;
            set=NULL;
        }
        umtx_unlock(NULL);

        delete set;
    }

    return nxCache[options];
}

static inline const UnicodeSet *
getNX(int32_t options, UErrorCode &errorCode) {
    if(U_FAILURE(errorCode) || (options&=_NORM_OPTIONS_SETS_MASK)==0) {
        /* incoming failure, or no decomposition exclusions requested */
        return NULL;
    } else {
        return internalGetNX(options, errorCode);
    }
}

static inline UBool
nx_contains(const UnicodeSet *nx, UChar32 c) {
    return nx!=NULL && nx->contains(c);
}

static inline UBool
nx_contains(const UnicodeSet *nx, UChar c, UChar c2) {
    return nx!=NULL && nx->contains(c2==0 ? c : U16_GET_SUPPLEMENTARY(c, c2));
}

/* other normalization primitives ------------------------------------------- */

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

/**
 * Get the canonical decomposition for one code point.
 * @param c code point
 * @param buffer out-only buffer for algorithmic decompositions of Hangul
 * @param length out-only, takes the length of the decomposition, if any
 * @return pointer to decomposition, or 0 if none
 * @internal
 */
static const UChar *
_decompose(UChar32 c, UChar buffer[4], int32_t &length) {
    uint32_t norm32;

    UTRIE_GET32(&normTrie, c, norm32);
    if(norm32&_NORM_QC_NFD) {
        if(isNorm32HangulOrJamo(norm32)) {
            /* Hangul syllable: decompose algorithmically */
            UChar c2;

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
            return buffer;
        } else {
            /* normal decomposition */
            uint8_t cc, trailCC;
            return _decompose(norm32, length, cc, trailCC);
        }
    } else {
        return 0;
    }
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
 * is this a safe boundary character for NF*D?
 * (lead cc==0)
 */
static inline UBool
_isNFDSafe(uint32_t norm32, uint32_t ccOrQCMask, uint32_t decompQCMask) {
    if((norm32&ccOrQCMask)==0) {
        return TRUE; /* cc==0 and no decomposition: this is NF*D safe */
    }

    /* inspect its decomposition - maybe a Hangul but not a surrogate here */
    if(isNorm32Regular(norm32) && (norm32&decompQCMask)!=0) {
        int32_t length;
        uint8_t cc, trailCC;

        /* decomposes, get everything from the variable-length extra data */
        _decompose(norm32, decompQCMask, length, cc, trailCC);
        return cc==0;
    } else {
        /* no decomposition (or Hangul), test the cc directly */
        return (norm32&_NORM_CC_MASK)==0;
    }
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

/* uchar.h */
U_CAPI uint8_t U_EXPORT2
u_getCombiningClass(UChar32 c) {
    UErrorCode errorCode=U_ZERO_ERROR;
    if(_haveData(errorCode)) {
        uint32_t norm32;

        UTRIE_GET32(&normTrie, c, norm32);
        return (uint8_t)(norm32>>_NORM_CC_SHIFT);
    } else {
        return 0;
    }
}

U_CAPI UBool U_EXPORT2
unorm_internalIsFullCompositionExclusion(UChar32 c) {
    UErrorCode errorCode=U_ZERO_ERROR;
    if(_haveData(errorCode) && formatVersion_2_1) {
        uint16_t aux;

        UTRIE_GET16(&auxTrie, c, aux);
        return (UBool)((aux&_NORM_AUX_COMP_EX_MASK)!=0);
    } else {
        return FALSE;
    }
}

U_CAPI UBool U_EXPORT2
unorm_isCanonSafeStart(UChar32 c) {
    UErrorCode errorCode=U_ZERO_ERROR;
    if(_haveData(errorCode) && formatVersion_2_1) {
        uint16_t aux;

        UTRIE_GET16(&auxTrie, c, aux);
        return (UBool)((aux&_NORM_AUX_UNSAFE_MASK)==0);
    } else {
        return FALSE;
    }
}

U_CAPI void U_EXPORT2
unorm_getUnicodeVersion(UVersionInfo *versionInfo, UErrorCode *pErrorCode){
    if(unorm_haveData(pErrorCode)){
        uprv_memcpy(*versionInfo, dataVersion, 4);
    }
}


U_CAPI UBool U_EXPORT2
unorm_getCanonStartSet(UChar32 c, USerializedSet *fillSet) {
    UErrorCode errorCode=U_ZERO_ERROR;
    if( fillSet!=NULL && (uint32_t)c<=0x10ffff &&
        _haveData(errorCode) && canonStartSets!=NULL
    ) {
        const uint16_t *table;
        int32_t i, start, limit;

        /*
         * binary search for c
         *
         * There are two search tables,
         * one for BMP code points and one for supplementary ones.
         * See unormimp.h for details.
         */
        if(c<=0xffff) {
            table=canonStartSets+canonStartSets[_NORM_SET_INDEX_CANON_SETS_LENGTH];
            start=0;
            limit=canonStartSets[_NORM_SET_INDEX_CANON_BMP_TABLE_LENGTH];

            /* each entry is a pair { c, result } */
            while(start<limit-2) {
                i=(uint16_t)(((start+limit)/4)*2); /* (start+limit)/2 and address pairs */
                if(c<table[i]) {
                    limit=i;
                } else {
                    start=i;
                }
            }

            /* found? */
            if(c==table[start]) {
                i=table[start+1];
                if((i&_NORM_CANON_SET_BMP_MASK)==_NORM_CANON_SET_BMP_IS_INDEX) {
                    /* result 01xxxxxx xxxxxx contains index x to a USerializedSet */
                    i&=(_NORM_MAX_CANON_SETS-1);
                    return uset_getSerializedSet(fillSet,
                                            canonStartSets+i,
                                            canonStartSets[_NORM_SET_INDEX_CANON_SETS_LENGTH]-i);
                } else {
                    /* other result values are BMP code points for single-code point sets */
                    uset_setSerializedToOne(fillSet, (UChar32)i);
                    return TRUE;
                }
            }
        } else {
            uint16_t high, low, h;

            table=canonStartSets+canonStartSets[_NORM_SET_INDEX_CANON_SETS_LENGTH]+
                                 canonStartSets[_NORM_SET_INDEX_CANON_BMP_TABLE_LENGTH];
            start=0;
            limit=canonStartSets[_NORM_SET_INDEX_CANON_SUPP_TABLE_LENGTH];

            high=(uint16_t)(c>>16);
            low=(uint16_t)c;

            /* each entry is a triplet { high(c), low(c), result } */
            while(start<limit-3) {
                i=(uint16_t)(((start+limit)/6)*3); /* (start+limit)/2 and address triplets */
                h=table[i]&0x1f; /* high word */
                if(high<h || (high==h && low<table[i+1])) {
                    limit=i;
                } else {
                    start=i;
                }
            }

            /* found? */
            h=table[start];
            if(high==(h&0x1f) && low==table[start+1]) {
                i=table[start+2];
                if((h&0x8000)==0) {
                    /* the result is an index to a USerializedSet */
                    return uset_getSerializedSet(fillSet,
                                            canonStartSets+i,
                                            canonStartSets[_NORM_SET_INDEX_CANON_SETS_LENGTH]-i);
                } else {
                    /*
                     * single-code point set {x} in
                     * triplet { 100xxxxx 000hhhhh  llllllll llllllll  xxxxxxxx xxxxxxxx }
                     */
                    i|=((int32_t)h&0x1f00)<<8; /* add high bits from high(c) */
                    uset_setSerializedToOne(fillSet, (UChar32)i);
                    return TRUE;
                }
            }
        }
    }

    return FALSE; /* not found */
}

U_CAPI int32_t U_EXPORT2
u_getFC_NFKC_Closure(UChar32 c, UChar *dest, int32_t destCapacity, UErrorCode *pErrorCode) {
    uint16_t aux;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(destCapacity<0 || (dest==NULL && destCapacity>0)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }
    if(!_haveData(*pErrorCode) || !formatVersion_2_1) {
        return 0;
    }

    UTRIE_GET16(&auxTrie, c, aux);
    aux&=_NORM_AUX_FNC_MASK;
    if(aux!=0) {
        const UChar *s;
        int32_t length;

        s=(const UChar *)(extraData+aux);
        if(*s<0xff00) {
            /* s points to the single-unit string */
            length=1;
        } else {
            length=*s&0xff;
            ++s;
        }
        if(0<length && length<=destCapacity) {
            uprv_memcpy(dest, s, length*U_SIZEOF_UCHAR);
        }
        return u_terminateUChars(dest, destCapacity, length, pErrorCode);
    } else {
        return u_terminateUChars(dest, destCapacity, 0, pErrorCode);
    }
}

/* Is c an NF<mode>-skippable code point? See unormimp.h. */
U_CAPI UBool U_EXPORT2
unorm_isNFSkippable(UChar32 c, UNormalizationMode mode) {
    UErrorCode errorCode;
    uint32_t norm32, mask;
    uint16_t aux, fcd;

    errorCode=U_ZERO_ERROR;
    if(!_haveData(errorCode)) {
        return FALSE;
    }

    /* handle trivial cases; set the comparison mask for the normal ones */
    switch(mode) {
    case UNORM_NONE:
        return TRUE;
    case UNORM_NFD:
        mask=_NORM_CC_MASK|_NORM_QC_NFD;
        break;
    case UNORM_NFKD:
        mask=_NORM_CC_MASK|_NORM_QC_NFKD;
        break;
    case UNORM_NFC:
    /* case UNORM_FCC: */
        mask=_NORM_CC_MASK|_NORM_COMBINES_ANY|(_NORM_QC_NFC&_NORM_QC_ANY_NO);
        break;
    case UNORM_NFKC:
        mask=_NORM_CC_MASK|_NORM_COMBINES_ANY|(_NORM_QC_NFKC&_NORM_QC_ANY_NO);
        break;
    case UNORM_FCD:
        /* FCD: skippable if lead cc==0 and trail cc<=1 */
        UTRIE_GET16(&fcdTrie, c, fcd);
        return fcd<=1;
    default:
        return FALSE;
    }

    /* check conditions (a)..(e), see unormimp.h */
    UTRIE_GET32(&normTrie, c, norm32);
    if((norm32&mask)!=0) {
        return FALSE; /* fails (a)..(e), not skippable */
    }

    if(mode<UNORM_NFC) {
        return TRUE; /* NF*D, passed (a)..(c), is skippable */
    }

    /* NF*C/FCC, passed (a)..(e) */
    if((norm32&_NORM_QC_NFD)==0) {
        return TRUE; /* no canonical decomposition, is skippable */
    }

    /* check Hangul syllables algorithmically */
    if(isNorm32HangulOrJamo(norm32)) {
        /* Jamo passed (a)..(e) above, must be Hangul */
        return !isHangulWithoutJamoT((UChar)c); /* LVT are skippable, LV are not */
    }

    /* if(mode<=UNORM_NFKC) { -- enable when implementing FCC */
    /* NF*C, test (f) flag */
    if(!formatVersion_2_2) {
        return FALSE; /* no (f) data, say not skippable to be safe */
    }

    UTRIE_GET16(&auxTrie, c, aux);
    return (aux&_NORM_AUX_NFC_SKIP_F_MASK)==0; /* TRUE=skippable if the (f) flag is not set */

    /* } else { FCC, test fcd<=1 instead of the above } */
}

U_CAPI void U_EXPORT2
unorm_addPropertyStarts(USet *set, UErrorCode *pErrorCode) {
    UChar c;

    if(!_haveData(*pErrorCode)) {
        return;
    }

    /* add the start code point of each same-value range of each trie */
    utrie_enum(&normTrie, NULL, _enumPropertyStartsRange, set);
    utrie_enum(&fcdTrie, NULL, _enumPropertyStartsRange, set);
    if(formatVersion_2_1) {
        utrie_enum(&auxTrie, NULL, _enumPropertyStartsRange, set);
    }

    /* add Hangul LV syllables and LV+1 because of skippables */
    for(c=HANGUL_BASE; c<HANGUL_BASE+HANGUL_COUNT; c+=JAMO_T_COUNT) {
        uset_add(set, c);
        uset_add(set, c+1);
    }
    uset_add(set, HANGUL_BASE+HANGUL_COUNT); /* add Hangul+1 to continue with other properties */
}

U_CAPI UNormalizationCheckResult U_EXPORT2
unorm_getQuickCheck(UChar32 c, UNormalizationMode mode) {
    static const uint32_t qcMask[UNORM_MODE_COUNT]={
        0, 0, _NORM_QC_NFD, _NORM_QC_NFKD, _NORM_QC_NFC, _NORM_QC_NFKC
    };

    UErrorCode errorCode;
    uint32_t norm32;

    errorCode=U_ZERO_ERROR;
    if(!_haveData(errorCode)) {
        return UNORM_YES;
    }

    UTRIE_GET32(&normTrie, c, norm32);
    norm32&=qcMask[mode];

    if(norm32==0) {
        return UNORM_YES;
    } else if(norm32&_NORM_QC_ANY_NO) {
        return UNORM_NO;
    } else /* _NORM_QC_ANY_MAYBE */ {
        return UNORM_MAYBE;
    }
}

U_CAPI uint16_t U_EXPORT2
unorm_getFCD16FromCodePoint(UChar32 c) {
    UErrorCode errorCode;
    uint16_t fcd;

    errorCode=U_ZERO_ERROR;
    if(!_haveData(errorCode)) {
        return 0;
    }

    UTRIE_GET16(&fcdTrie, c, fcd);
    return fcd;
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

/* find the last true starter in [start..src[ and return the pointer to it */
static const UChar *
_findPreviousStarter(const UChar *start, const UChar *src,
                     uint32_t ccOrQCMask, uint32_t decompQCMask, UChar minNoMaybe) {
    uint32_t norm32;
    UChar c, c2;

    while(start<src) {
        norm32=_getPrevNorm32(start, src, minNoMaybe, ccOrQCMask|decompQCMask, c, c2);
        if(_isTrueStarter(norm32, ccOrQCMask, decompQCMask)) {
            break;
        }
    }
    return src;
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

        if(isNorm32LeadSurrogate(norm32)) {
            /* c is a lead surrogate, get the real norm32 */
            if((src+1)==limit || !UTF_IS_SECOND_SURROGATE(c2=*(src+1))) {
                break; /* unmatched first surrogate: counts as a true starter */
            }
            norm32=_getNorm32FromSurrogatePair(norm32, c2);

            if((norm32&ccOrQCMask)==0) {
                break; /* true starter */
            }
        } else {
            c2=0;
        }

        /* (c, c2) is not a true starter but its decomposition may be */
        if(norm32&decompQCMask) {
            /* (c, c2) decomposes, get everything from the variable-length extra data */
            p=_decompose(norm32, decompQCMask, length, cc, trailCC);

            /* get the first character's norm32 to check if it is a true starter */
            if(cc==0 && (_getNorm32(p, qcMask)&qcMask)==0) {
                break; /* true starter */
            }
        }

        src+= c2==0 ? 1 : 2; /* not a true starter, continue */
    }

    return src;
}

/* make NFD & NFKD ---------------------------------------------------------- */

U_CAPI int32_t U_EXPORT2
unorm_getDecomposition(UChar32 c, UBool compat,
                       UChar *dest, int32_t destCapacity) {
    UErrorCode errorCode=U_ZERO_ERROR;
    if( (uint32_t)c<=0x10ffff &&
        _haveData(errorCode) &&
        ((dest!=NULL && destCapacity>0) || destCapacity==0)
    ) {
        uint32_t norm32, qcMask;
        UChar32 minNoMaybe;
        int32_t length;

        /* initialize */
        if(!compat) {
            minNoMaybe=(UChar32)indexes[_NORM_INDEX_MIN_NFD_NO_MAYBE];
            qcMask=_NORM_QC_NFD;
        } else {
            minNoMaybe=(UChar32)indexes[_NORM_INDEX_MIN_NFKD_NO_MAYBE];
            qcMask=_NORM_QC_NFKD;
        }

        if(c<minNoMaybe) {
            /* trivial case */
            if(destCapacity>0) {
                dest[0]=(UChar)c;
            }
            return -1;
        }

        /* data lookup */
        UTRIE_GET32(&normTrie, c, norm32);
        if((norm32&qcMask)==0) {
            /* simple case: no decomposition */
            if(c<=0xffff) {
                if(destCapacity>0) {
                    dest[0]=(UChar)c;
                }
                return -1;
            } else {
                if(destCapacity>=2) {
                    dest[0]=UTF16_LEAD(c);
                    dest[1]=UTF16_TRAIL(c);
                }
                return -2;
            }
        } else if(isNorm32HangulOrJamo(norm32)) {
            /* Hangul syllable: decompose algorithmically */
            UChar c2;

            c-=HANGUL_BASE;

            c2=(UChar)(c%JAMO_T_COUNT);
            c/=JAMO_T_COUNT;
            if(c2>0) {
                if(destCapacity>=3) {
                    dest[2]=(UChar)(JAMO_T_BASE+c2);
                }
                length=3;
            } else {
                length=2;
            }

            if(destCapacity>=2) {
                dest[1]=(UChar)(JAMO_V_BASE+c%JAMO_V_COUNT);
                dest[0]=(UChar)(JAMO_L_BASE+c/JAMO_V_COUNT);
            }
            return length;
        } else {
            /* c decomposes, get everything from the variable-length extra data */
            const UChar *p, *limit;
            uint8_t cc, trailCC;

            p=_decompose(norm32, qcMask, length, cc, trailCC);
            if(length<=destCapacity) {
                limit=p+length;
                do {
                    *dest++=*p++;
                } while(p<limit);
            }
            return length;
        }
    } else {
        return 0;
    }
}

static int32_t
_decompose(UChar *dest, int32_t destCapacity,
           const UChar *src, int32_t srcLength,
           UBool compat, const UnicodeSet *nx,
           uint8_t &outTrailCC) {
    UChar buffer[3];
    const UChar *limit, *prevSrc, *p;
    uint32_t norm32, ccOrQCMask, qcMask;
    int32_t destIndex, reorderStartIndex, length;
    UChar c, c2, minNoMaybe;
    uint8_t cc, prevCC, trailCC;

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
            if((destIndex+length)<=destCapacity) {
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
            if(nx_contains(nx, c)) {
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
            if(nx_contains(nx, c, c2)) {
                /* excluded: norm32==0 */
                cc=trailCC=0;
                p=NULL;
            } else if((norm32&qcMask)==0) {
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
        if((destIndex+length)<=destCapacity) {
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
unorm_decompose(UChar *dest, int32_t destCapacity,
                const UChar *src, int32_t srcLength,
                UBool compat, int32_t options,
                UErrorCode *pErrorCode) {
    const UnicodeSet *nx;
    int32_t destIndex;
    uint8_t trailCC;

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    nx=getNX(options, *pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    destIndex=_decompose(dest, destCapacity,
                         src, srcLength,
                         compat, nx,
                         trailCC);

    return u_terminateUChars(dest, destCapacity, destIndex, pErrorCode);
}

/* make NFC & NFKC ---------------------------------------------------------- */

/* get the composition properties of the next character */
static inline uint32_t
_getNextCombining(UChar *&p, const UChar *limit,
                  UChar &c, UChar &c2,
                  uint16_t &combiningIndex, uint8_t &cc,
                  const UnicodeSet *nx) {
    uint32_t norm32, combineFlags;

    /* get properties */
    c=*p++;
    norm32=_getNorm32(c);

    /* preset output values for most characters */
    c2=0;
    combiningIndex=0;
    cc=0;

    if((norm32&(_NORM_CC_MASK|_NORM_COMBINES_ANY))==0) {
        return 0;
    } else {
        if(isNorm32Regular(norm32)) {
            /* set cc etc. below */
        } else if(isNorm32HangulOrJamo(norm32)) {
            /* a compatibility decomposition contained Jamos */
            combiningIndex=(uint16_t)(0xfff0|(norm32>>_NORM_EXTRA_SHIFT));
            return norm32&_NORM_COMBINES_ANY;
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

        if(nx_contains(nx, c, c2)) {
            return 0; /* excluded: norm32==0 */
        }

        cc=(uint8_t)(norm32>>_NORM_CC_SHIFT);

        combineFlags=norm32&_NORM_COMBINES_ANY;
        if(combineFlags!=0) {
            combiningIndex=*(_getExtraData(norm32)-1);
        }
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

static inline UBool
_composeHangul(UChar prev, UChar c, uint32_t norm32, const UChar *&src, const UChar *limit,
               UBool compat, UChar *dest, const UnicodeSet *nx) {
    if(isJamoVTNorm32JamoV(norm32)) {
        /* c is a Jamo V, compose with previous Jamo L and following Jamo T */
        prev=(UChar)(prev-JAMO_L_BASE);
        if(prev<JAMO_L_COUNT) {
            c=(UChar)(HANGUL_BASE+(prev*JAMO_V_COUNT+(c-JAMO_V_BASE))*JAMO_T_COUNT);

            /* check if the next character is a Jamo T (normal or compatibility) */
            if(src!=limit) {
                UChar next, t;

                next=*src;
                if((t=(UChar)(next-JAMO_T_BASE))<JAMO_T_COUNT) {
                    /* normal Jamo T */
                    ++src;
                    c+=t;
                } else if(compat) {
                    /* if NFKC, then check for compatibility Jamo T (BMP only) */
                    norm32=_getNorm32(next);
                    if(isNorm32Regular(norm32) && (norm32&_NORM_QC_NFKD)) {
                        const UChar *p;
                        int32_t length;
                        uint8_t cc, trailCC;

                        p=_decompose(norm32, _NORM_QC_NFKD, length, cc, trailCC);
                        if(length==1 && (t=(UChar)(*p-JAMO_T_BASE))<JAMO_T_COUNT) {
                            /* compatibility Jamo T */
                            ++src;
                            c+=t;
                        }
                    }
                }
            }
            if(nx_contains(nx, c)) {
                if(!isHangulWithoutJamoT(c)) {
                    --src; /* undo ++src from reading the Jamo T */
                }
                return FALSE;
            }
            if(dest!=0) {
                *dest=c;
            }
            return TRUE;
        }
    } else if(isHangulWithoutJamoT(prev)) {
        /* c is a Jamo T, compose with previous Hangul LV that does not contain a Jamo T */
        c=(UChar)(prev+(c-JAMO_T_BASE));
        if(nx_contains(nx, c)) {
            return FALSE;
        }
        if(dest!=0) {
            *dest=c;
        }
        return TRUE;
    }
    return FALSE;
}

/*
 * recompose the characters in [p..limit[
 * (which is in NFD - decomposed and canonically ordered),
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
_recompose(UChar *p, UChar *&limit, const UnicodeSet *nx) {
    UChar *starter, *pRemove, *q, *r;
    uint32_t combineFlags;
    UChar c, c2;
    uint16_t combineFwdIndex, combineBackIndex;
    uint16_t result, value, value2;
    uint8_t cc, prevCC;
    UBool starterIsSupplementary;

    starter=NULL;                   /* no starter */
    combineFwdIndex=0;              /* will not be used until starter!=NULL - avoid compiler warnings */
    combineBackIndex=0;             /* will always be set if combineFlags!=0 - avoid compiler warnings */
    value=value2=0;                 /* always set by _combine() before used - avoid compiler warnings */
    starterIsSupplementary=FALSE;   /* will not be used until starter!=NULL - avoid compiler warnings */
    prevCC=0;

    for(;;) {
        combineFlags=_getNextCombining(p, limit, c, c2, combineBackIndex, cc, nx);
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
                        if(!nx_contains(nx, c)) {
                            *starter=c;
                        } else {
                            /* excluded */
                            if(!isHangulWithoutJamoT(c)) {
                                --p; /* undo the ++p from reading the Jamo T */
                            }
                            /* c is modified but not used any more -- c=*(p-1); -- re-read the Jamo V/T */
                            pRemove=NULL;
                        }
                    }
#if 0
                /*
                 * The following is disabled with #if 0 because it can not occur:
                 * Since the input is in NFD, there are no Hangul LV syllables that
                 * a Jamo T could combine with.
                 * All Jamo Ts are combined above when handling Jamo Vs.
                 */
                } else {
                    /* Jamo T, compose with previous Hangul that does not have a Jamo T */
                    if(isHangulWithoutJamoT(c2)) {
                        pRemove=p-1;
                        *starter=(UChar)(c2+(c-JAMO_T_BASE));
                    }
#endif
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
                /* the starter and the combining mark (c, c2) do combine and */
                0!=(result=_combine(combiningTable+combineFwdIndex, combineBackIndex, value, value2)) &&
                /* the composition result is not excluded */
                !nx_contains(nx, value, value2)
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
            /* found a new starter; combineFlags==0 if (c, c2) is excluded */
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

/* decompose and recompose [prevStarter..src[ */
static const UChar *
_composePart(UChar *stackBuffer, UChar *&buffer, int32_t &bufferCapacity, int32_t &length,
             const UChar *prevStarter, const UChar *src,
             uint32_t qcMask, uint8_t &prevCC,
             const UnicodeSet *nx,
             UErrorCode *pErrorCode) {
    UChar *recomposeLimit;
    uint8_t trailCC;
    UBool compat;

    compat=(UBool)((qcMask&_NORM_QC_NFKC)!=0);

    /* decompose [prevStarter..src[ */
    length=_decompose(buffer, bufferCapacity,
                      prevStarter, src-prevStarter,
                      compat, nx,
                      trailCC);
    if(length>bufferCapacity) {
        if(!u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity, 2*length, 0)) {
            *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
            return NULL;
        }
        length=_decompose(buffer, bufferCapacity,
                          prevStarter, src-prevStarter,
                          compat, nx,
                          trailCC);
    }

    /* recompose the decomposition */
    recomposeLimit=buffer+length;
    if(length>=2) {
        prevCC=_recompose(buffer, recomposeLimit, nx);
    }

    /* return with a pointer to the recomposition and its length */
    length=recomposeLimit-buffer;
    return buffer;
}

static int32_t
_compose(UChar *dest, int32_t destCapacity,
         const UChar *src, int32_t srcLength,
         UBool compat, const UnicodeSet *nx,
         UErrorCode *pErrorCode) {
    UChar stackBuffer[_STACK_BUFFER_CAPACITY];
    UChar *buffer;
    int32_t bufferCapacity;

    const UChar *limit, *prevSrc, *prevStarter;
    uint32_t norm32, ccOrQCMask, qcMask;
    int32_t destIndex, reorderStartIndex, length;
    UChar c, c2, minNoMaybe;
    uint8_t cc, prevCC;

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
     * When _composePart() backs out from prevSrc back to prevStarter,
     * then it also backs out destIndex by the same amount.
     * Therefore, at all times, the (prevSrc-prevStarter) source units
     * must correspond 1:1 to destination units counted with destIndex,
     * except for reordering.
     * This is true for the qc "yes" characters copied in the fast loop,
     * and for pure reordering.
     * prevStarter must be set forward to src when this is not true:
     * In _composePart() and after composing a Hangul syllable.
     *
     * This mechanism relies on the assumption that the decomposition of a true starter
     * also begins with a true starter. gennorm/store.c checks for this.
     */
    prevStarter=src;

    ccOrQCMask=_NORM_CC_MASK|qcMask;
    destIndex=reorderStartIndex=0;
    prevCC=0;

    /* avoid compiler warnings */
    norm32=0;
    c=0;

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
            if((destIndex+length)<=destCapacity) {
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
         * c is not a Hangul syllable or Jamo L because
         * they are not marked with no/maybe for NFC & NFKC (and their cc==0)
         */
        if(isNorm32HangulOrJamo(norm32)) {
            /*
             * c is a Jamo V/T:
             * try to compose with the previous character, Jamo V also with a following Jamo T,
             * and set values here right now in case we just continue with the main loop
             */
            prevCC=cc=0;
            reorderStartIndex=destIndex;

            if(
                destIndex>0 &&
                _composeHangul(
                    *(prevSrc-1), c, norm32, src, limit, compat,
                    destIndex<=destCapacity ? dest+(destIndex-1) : 0,
                    nx)
            ) {
                prevStarter=src;
                continue;
            }

            /* the Jamo V/T did not compose into a Hangul syllable, just append to dest */
            c2=0;
            length=1;
            prevStarter=prevSrc;
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
            if(nx_contains(nx, c, c2)) {
                /* excluded: norm32==0 */
                cc=0;
            } else if((norm32&qcMask)==0) {
                cc=(uint8_t)(norm32>>_NORM_CC_SHIFT);
            } else {
                const UChar *p;
                uint32_t decompQCMask;

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
                decompQCMask=(qcMask<<2)&0xf; /* decomposition quick check mask */

                /*
                 * find the last true starter in [prevStarter..src[
                 * it is either the decomposition of the current character (at prevSrc),
                 * or prevStarter
                 */
                if(_isTrueStarter(norm32, ccOrQCMask, decompQCMask)) {
                    prevStarter=prevSrc;
                } else {
                    /* adjust destIndex: back out what had been copied with qc "yes" */
                    destIndex-=(int32_t)(prevSrc-prevStarter);
                }

                /* find the next true starter in [src..limit[ - modifies src to point to the next starter */
                src=_findNextStarter(src, limit, qcMask, decompQCMask, minNoMaybe);

                /* compose [prevStarter..src[ */
                p=_composePart(stackBuffer, buffer, bufferCapacity,
                               length,          /* output */
                               prevStarter, src,
                               qcMask,
                               prevCC,          /* output */
                               nx,
                               pErrorCode);

                if(p==NULL) {
                    destIndex=0;   /* an error occurred (out of memory) */
                    break;
                }

                /* append the recomposed buffer contents to the destination buffer */
                if((destIndex+length)<=destCapacity) {
                    while(length>0) {
                        dest[destIndex++]=*p++;
                        --length;
                    }
                } else {
                    /* buffer overflow */
                    /* keep incrementing the destIndex for preflighting */
                    destIndex+=length;
                }

                /* set the next starter */
                prevStarter=src;

                continue;
            }
        }

        /* append the single code point (c, c2) to the destination buffer */
        if((destIndex+length)<=destCapacity) {
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
unorm_compose(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              UBool compat, int32_t options,
              UErrorCode *pErrorCode) {
    const UnicodeSet *nx;
    int32_t destIndex;

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    nx=getNX(options, *pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    destIndex=_compose(dest, destCapacity,
                       src, srcLength,
                       compat, nx,
                       pErrorCode);

    return u_terminateUChars(dest, destCapacity, destIndex, pErrorCode);
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
_decomposeFCD(const UChar *src, const UChar *decompLimit,
              UChar *dest, int32_t &destIndex, int32_t destCapacity,
              const UnicodeSet *nx) {
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
        if(nx_contains(nx, c, c2)) {
            /* excluded: norm32==0 */
            cc=trailCC=0;
            p=NULL;
        } else if((norm32&_NORM_QC_NFD)==0) {
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
        if((destIndex+length)<=destCapacity) {
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
unorm_makeFCD(UChar *dest, int32_t destCapacity,
              const UChar *src, int32_t srcLength,
              const UnicodeSet *nx,
              UErrorCode *pErrorCode) {
    const UChar *limit, *prevSrc, *decompStart;
    int32_t destIndex, length;
    UChar c, c2;
    uint16_t fcd16;
    int16_t prevCC, cc;

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
            if((destIndex+length)<=destCapacity) {
                uprv_memcpy(dest+destIndex, prevSrc, length*U_SIZEOF_UCHAR);
            }
            destIndex+=length;
            prevSrc=src;

            /* prevCC<0 is only possible from the above loop, i.e., only if prevSrc<src */
            if(prevCC<0) {
                /* the previous character was <_NORM_MIN_WITH_LEAD_CC, we need to get its trail cc */
                if(!nx_contains(nx, (UChar32)-prevCC)) {
                    prevCC=(int16_t)(_getFCD16((UChar)-prevCC)&0xff);
                } else {
                    prevCC=0; /* excluded: fcd16==0 */
                }

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
        if(nx_contains(nx, c, c2)) {
            fcd16=0; /* excluded: fcd16==0 */
        }

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
            if((destIndex+length)<=destCapacity) {
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
            prevCC=_decomposeFCD(decompStart, src,
                                 dest, destIndex, destCapacity,
                                 nx);
            decompStart=src;
        }
    }

    return u_terminateUChars(dest, destCapacity, destIndex, pErrorCode);
}

/* quick check functions ---------------------------------------------------- */

static UBool
unorm_checkFCD(const UChar *src, int32_t srcLength, const UnicodeSet *nx) {
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
                c2=0;
                fcd16=0;
            }
        } else {
            c2=0;
        }

        if(nx_contains(nx, c, c2)) {
            prevCC=0; /* excluded: fcd16==0 */
            continue;
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
                if(!nx_contains(nx, (UChar32)-prevCC)) {
                    prevCC=(int16_t)(_getFCD16((UChar)-prevCC)&0xff);
                } else {
                    prevCC=0; /* excluded: fcd16==0 */
                }
            }

            if(cc<prevCC) {
                return FALSE;
            }
        }
        prevCC=(int16_t)(fcd16&0xff);
    }
}

static UNormalizationCheckResult
_quickCheck(const UChar *src,
            int32_t srcLength,
            UNormalizationMode mode,
            UBool allowMaybe,
            const UnicodeSet *nx,
            UErrorCode *pErrorCode) {
    UChar stackBuffer[_STACK_BUFFER_CAPACITY];
    UChar *buffer;
    int32_t bufferCapacity;

    const UChar *start, *limit;
    uint32_t norm32, qcNorm32, ccOrQCMask, qcMask;
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
        return unorm_checkFCD(src, srcLength, nx) ? UNORM_YES : UNORM_NO;
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return UNORM_MAYBE;
    }

    /* initialize */
    buffer=stackBuffer;
    bufferCapacity=_STACK_BUFFER_CAPACITY;

    ccOrQCMask=_NORM_CC_MASK|qcMask;
    result=UNORM_YES;
    prevCC=0;

    start=src;
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
                        goto endloop; /* break out of outer loop */
                    }
                } else if(((norm32=_getNorm32(c))&ccOrQCMask)!=0) {
                    break;
                }
                prevCC=0;
            }
        } else {
            for(;;) {
                if(src==limit) {
                    goto endloop; /* break out of outer loop */
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
                c2=0;
                norm32=0;
            }
        } else {
            c2=0;
        }

        if(nx_contains(nx, c, c2)) {
            /* excluded: norm32==0 */
            norm32=0;
        }

        /* check the combining order */
        cc=(uint8_t)(norm32>>_NORM_CC_SHIFT);
        if(cc!=0 && cc<prevCC) {
            result=UNORM_NO;
            break;
        }
        prevCC=cc;

        /* check for "no" or "maybe" quick check flags */
        qcNorm32=norm32&qcMask;
        if(qcNorm32&_NORM_QC_ANY_NO) {
            result=UNORM_NO;
            break;
        } else if(qcNorm32!=0) {
            /* "maybe" can only occur for NFC and NFKC */
            if(allowMaybe) {
                result=UNORM_MAYBE;
            } else {
                /* normalize a section around here to see if it is really normalized or not */
                const UChar *prevStarter;
                uint32_t decompQCMask;
                int32_t length;

                decompQCMask=(qcMask<<2)&0xf; /* decomposition quick check mask */

                /* find the previous starter */
                prevStarter=src-1; /* set prevStarter to the beginning of the current character */
                if(UTF_IS_TRAIL(*prevStarter)) {
                    --prevStarter; /* safe because unpaired surrogates do not result in "maybe" */
                }
                prevStarter=_findPreviousStarter(start, prevStarter, ccOrQCMask, decompQCMask, minNoMaybe);

                /* find the next true starter in [src..limit[ - modifies src to point to the next starter */
                src=_findNextStarter(src, limit, qcMask, decompQCMask, minNoMaybe);

                /* decompose and recompose [prevStarter..src[ */
                _composePart(stackBuffer, buffer, bufferCapacity,
                             length,
                             prevStarter,
                             src,
                             qcMask,
                             prevCC, nx, pErrorCode);
                if(U_FAILURE(*pErrorCode)) {
                    result=UNORM_MAYBE; /* error (out of memory) */
                    break;
                }

                /* compare the normalized version with the original */
                if(0!=uprv_strCompare(prevStarter, (int32_t)(src-prevStarter), buffer, length, FALSE, FALSE)) {
                    result=UNORM_NO; /* normalization differs */
                    break;
                }

                /* continue after the next starter */
            }
        }
    }
endloop:

    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return result;
}

U_CAPI UNormalizationCheckResult U_EXPORT2
unorm_quickCheck(const UChar *src,
                 int32_t srcLength, 
                 UNormalizationMode mode,
                 UErrorCode *pErrorCode) {
    return _quickCheck(src, srcLength, mode, TRUE, NULL, pErrorCode);
}

U_CAPI UNormalizationCheckResult U_EXPORT2
unorm_quickCheckWithOptions(const UChar *src, int32_t srcLength, 
                            UNormalizationMode mode, int32_t options,
                            UErrorCode *pErrorCode) {
    return _quickCheck(src, srcLength, mode, TRUE, getNX(options, *pErrorCode), pErrorCode);
}

U_CAPI UBool U_EXPORT2
unorm_isNormalized(const UChar *src, int32_t srcLength,
                   UNormalizationMode mode,
                   UErrorCode *pErrorCode) {
    return (UBool)(UNORM_YES==_quickCheck(src, srcLength, mode, FALSE, NULL, pErrorCode));
}

U_CAPI UBool U_EXPORT2
unorm_isNormalizedWithOptions(const UChar *src, int32_t srcLength,
                              UNormalizationMode mode, int32_t options,
                              UErrorCode *pErrorCode) {
    return (UBool)(UNORM_YES==_quickCheck(src, srcLength, mode, FALSE, getNX(options, *pErrorCode), pErrorCode));
}

/* normalize() API ---------------------------------------------------------- */

/**
 * Internal API for normalizing.
 * Does not check for bad input.
 * Requires _haveData() to be true.
 * @internal
 */
static int32_t
unorm_internalNormalize(UChar *dest, int32_t destCapacity,
                        const UChar *src, int32_t srcLength,
                        UNormalizationMode mode, const UnicodeSet *nx,
                        UErrorCode *pErrorCode) {
    int32_t destLength;
    uint8_t trailCC;

    switch(mode) {
    case UNORM_NFD:
        destLength=_decompose(dest, destCapacity,
                              src, srcLength,
                              FALSE, nx, trailCC);
        break;
    case UNORM_NFKD:
        destLength=_decompose(dest, destCapacity,
                              src, srcLength,
                              TRUE, nx, trailCC);
        break;
    case UNORM_NFC:
        destLength=_compose(dest, destCapacity,
                            src, srcLength,
                            FALSE, nx, pErrorCode);
        break;
    case UNORM_NFKC:
        destLength=_compose(dest, destCapacity,
                            src, srcLength,
                            TRUE, nx, pErrorCode);
        break;
    case UNORM_FCD:
        return unorm_makeFCD(dest, destCapacity,
                             src, srcLength,
                             nx,
                             pErrorCode);
    case UNORM_NONE:
        /* just copy the string */
        if(srcLength==-1) {
            srcLength=u_strlen(src);
        }
        if(srcLength>0 && srcLength<=destCapacity) {
            uprv_memcpy(dest, src, srcLength*U_SIZEOF_UCHAR);
        }
        destLength=srcLength;
        break;
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    return u_terminateUChars(dest, destCapacity, destLength, pErrorCode);
}

/**
 * Internal API for normalizing.
 * Does not check for bad input.
 * @internal
 */
U_CAPI int32_t U_EXPORT2
unorm_internalNormalize(UChar *dest, int32_t destCapacity,
                        const UChar *src, int32_t srcLength,
                        UNormalizationMode mode, int32_t options,
                        UErrorCode *pErrorCode) {
    const UnicodeSet *nx;

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    nx=getNX(options, *pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    return unorm_internalNormalize(dest, destCapacity,
                                   src, srcLength,
                                   mode, nx,
                                   pErrorCode);
}

/** Public API for normalizing. */
U_CAPI int32_t U_EXPORT2
unorm_normalize(const UChar *src, int32_t srcLength,
                UNormalizationMode mode, int32_t options,
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

    return unorm_internalNormalize(dest, destCapacity,
                                   src, srcLength,
                                   mode, options,
                                   pErrorCode);
}


/* iteration functions ------------------------------------------------------ */

/*
 * These iteration functions are the core implementations of the
 * Normalizer class iteration API.
 * They read from a UCharIterator into their own buffer
 * and normalize into the Normalizer iteration buffer.
 * Normalizer itself then iterates over its buffer until that needs to be
 * filled again.
 */

/*
 * ### TODO:
 * Now that UCharIterator.next/previous return (int32_t)-1 not (UChar)0xffff
 * if iteration bounds are reached,
 * try to not call hasNext/hasPrevious and instead check for >=0.
 */

/* backward iteration ------------------------------------------------------- */

/*
 * read backwards and get norm32
 * return 0 if the character is <minC
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
static inline uint32_t
_getPrevNorm32(UCharIterator &src, uint32_t minC, uint32_t mask, UChar &c, UChar &c2) {
    uint32_t norm32;

    /* need src.hasPrevious() */
    c=(UChar)src.previous(&src);
    c2=0;

    /* check for a surrogate before getting norm32 to see if we need to predecrement further */
    if(c<minC) {
        return 0;
    } else if(!UTF_IS_SURROGATE(c)) {
        return _getNorm32(c);
    } else if(UTF_IS_SURROGATE_FIRST(c) || !src.hasPrevious(&src)) {
        /* unpaired surrogate */
        return 0;
    } else if(UTF_IS_FIRST_SURROGATE(c2=(UChar)src.previous(&src))) {
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
        src.move(&src, 1, UITER_CURRENT);
        c2=0;
        return 0;
    }
}

/*
 * read backwards and check if the character is a previous-iteration boundary
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
typedef UBool
IsPrevBoundaryFn(UCharIterator &src, uint32_t minC, uint32_t mask, UChar &c, UChar &c2);

/*
 * for NF*D:
 * read backwards and check if the lead combining class is 0
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
static UBool
_isPrevNFDSafe(UCharIterator &src, uint32_t minC, uint32_t ccOrQCMask, UChar &c, UChar &c2) {
    return _isNFDSafe(_getPrevNorm32(src, minC, ccOrQCMask, c, c2), ccOrQCMask, ccOrQCMask&_NORM_QC_MASK);
}

/*
 * read backwards and check if the character is (or its decomposition begins with)
 * a "true starter" (cc==0 and NF*C_YES)
 * if c2!=0 then (c2, c) is a surrogate pair (reversed - c2 is first surrogate but read second!)
 */
static UBool
_isPrevTrueStarter(UCharIterator &src, uint32_t minC, uint32_t ccOrQCMask, UChar &c, UChar &c2) {
    uint32_t norm32, decompQCMask;
    
    decompQCMask=(ccOrQCMask<<2)&0xf; /* decomposition quick check mask */
    norm32=_getPrevNorm32(src, minC, ccOrQCMask|decompQCMask, c, c2);
    return _isTrueStarter(norm32, ccOrQCMask, decompQCMask);
}

static int32_t
_findPreviousIterationBoundary(UCharIterator &src,
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

    while(src.hasPrevious(&src)) {
        isBoundary=isPrevBoundary(src, minC, mask, c, c2);

        /* always write this character to the front of the buffer */
        /* make sure there is enough space in the buffer */
        if(startIndex < (c2==0 ? 1 : 2)) {
            int32_t bufferLength=bufferCapacity;

            if(!u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity, 2*bufferCapacity, bufferLength)) {
                *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                src.move(&src, 0, UITER_START);
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

U_CAPI int32_t U_EXPORT2
unorm_previous(UCharIterator *src,
               UChar *dest, int32_t destCapacity,
               UNormalizationMode mode, int32_t options,
               UBool doNormalize, UBool *pNeededToNormalize,
               UErrorCode *pErrorCode) {
    UChar stackBuffer[100];
    UChar *buffer=NULL;
    IsPrevBoundaryFn *isPreviousBoundary=NULL;
    uint32_t mask=0;
    int32_t startIndex=0, bufferLength=0, bufferCapacity=0, destLength=0;
    int32_t c=0, c2=0;
    UChar minC=0;

    /* check argument values */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( destCapacity<0 || (dest==NULL && destCapacity>0) ||
        src==NULL
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    if(pNeededToNormalize!=NULL) {
        *pNeededToNormalize=FALSE;
    }

    switch(mode) {
    case UNORM_NFD:
    case UNORM_FCD:
        isPreviousBoundary=_isPrevNFDSafe;
        minC=_NORM_MIN_WITH_LEAD_CC;
        mask=_NORM_CC_MASK|_NORM_QC_NFD;
        break;
    case UNORM_NFKD:
        isPreviousBoundary=_isPrevNFDSafe;
        minC=_NORM_MIN_WITH_LEAD_CC;
        mask=_NORM_CC_MASK|_NORM_QC_NFKD;
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
        destLength=0;
        if((c=src->previous(src))>=0) {
            destLength=1;
            if(UTF_IS_TRAIL(c) && (c2=src->previous(src))>=0) {
                if(UTF_IS_LEAD(c2)) {
                    if(destCapacity>=2) {
                        dest[1]=(UChar)c; /* trail surrogate */
                        destLength=2;
                    }
                    c=c2; /* lead surrogate to be written below */
                } else {
                    src->move(src, 1, UITER_CURRENT);
                }
            }

            if(destCapacity>0) {
                dest[0]=(UChar)c;
            }
        }
        return u_terminateUChars(dest, destCapacity, destLength, pErrorCode);
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    buffer=stackBuffer;
    bufferCapacity=(int32_t)(sizeof(stackBuffer)/U_SIZEOF_UCHAR);
    bufferLength=_findPreviousIterationBoundary(*src,
                                                isPreviousBoundary, minC, mask,
                                                buffer, bufferCapacity,
                                                startIndex,
                                                pErrorCode);
    if(bufferLength>0) {
        if(doNormalize) {
            destLength=unorm_internalNormalize(dest, destCapacity,
                                               buffer+startIndex, bufferLength,
                                               mode, options,
                                               pErrorCode);
            if(pNeededToNormalize!=0 && U_SUCCESS(*pErrorCode)) {
                *pNeededToNormalize=
                    (UBool)(destLength!=bufferLength ||
                            0!=uprv_memcmp(dest, buffer+startIndex, destLength*U_SIZEOF_UCHAR));
            }
        } else {
            /* just copy the source characters */
            if(destCapacity>0) {
                uprv_memcpy(dest, buffer+startIndex, uprv_min(bufferLength, destCapacity)*U_SIZEOF_UCHAR);
            }
            destLength=u_terminateUChars(dest, destCapacity, bufferLength, pErrorCode);
        }
    } else {
        destLength=u_terminateUChars(dest, destCapacity, 0, pErrorCode);
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
_getNextNorm32(UCharIterator &src, uint32_t minC, uint32_t mask, UChar &c, UChar &c2) {
    uint32_t norm32;

    /* need src.hasNext() to be true */
    c=(UChar)src.next(&src);
    c2=0;

    if(c<minC) {
        return 0;
    }

    norm32=_getNorm32(c);
    if(UTF_IS_FIRST_SURROGATE(c)) {
        if(src.hasNext(&src) && UTF_IS_SECOND_SURROGATE(c2=(UChar)src.current(&src))) {
            src.move(&src, 1, UITER_CURRENT); /* skip the c2 surrogate */
            if((norm32&mask)==0) {
                /* irrelevant data */
                return 0;
            } else {
                /* norm32 must be a surrogate special */
                return _getNorm32FromSurrogatePair(norm32, c2);
            }
        } else {
            /* unmatched surrogate */
            c2=0;
            return 0;
        }
    }
    return norm32;
}

/*
 * read forward and check if the character is a next-iteration boundary
 * if c2!=0 then (c, c2) is a surrogate pair
 */
typedef UBool
IsNextBoundaryFn(UCharIterator &src, uint32_t minC, uint32_t mask, UChar &c, UChar &c2);

/*
 * for NF*D:
 * read forward and check if the lead combining class is 0
 * if c2!=0 then (c, c2) is a surrogate pair
 */
static UBool
_isNextNFDSafe(UCharIterator &src, uint32_t minC, uint32_t ccOrQCMask, UChar &c, UChar &c2) {
    return _isNFDSafe(_getNextNorm32(src, minC, ccOrQCMask, c, c2), ccOrQCMask, ccOrQCMask&_NORM_QC_MASK);
}

/*
 * for NF*C:
 * read forward and check if the character is (or its decomposition begins with)
 * a "true starter" (cc==0 and NF*C_YES)
 * if c2!=0 then (c, c2) is a surrogate pair
 */
static UBool
_isNextTrueStarter(UCharIterator &src, uint32_t minC, uint32_t ccOrQCMask, UChar &c, UChar &c2) {
    uint32_t norm32, decompQCMask;
    
    decompQCMask=(ccOrQCMask<<2)&0xf; /* decomposition quick check mask */
    norm32=_getNextNorm32(src, minC, ccOrQCMask|decompQCMask, c, c2);
    return _isTrueStarter(norm32, ccOrQCMask, decompQCMask);
}

static int32_t
_findNextIterationBoundary(UCharIterator &src,
                           IsNextBoundaryFn *isNextBoundary, uint32_t minC, uint32_t mask,
                           UChar *&buffer, int32_t &bufferCapacity,
                           UErrorCode *pErrorCode) {
    UChar *stackBuffer;
    int32_t bufferIndex;
    UChar c, c2;

    if(!src.hasNext(&src)) {
        return 0;
    }

    /* initialize */
    stackBuffer=buffer;

    /* get one character and ignore its properties */
    buffer[0]=c=(UChar)src.next(&src);
    bufferIndex=1;
    if(UTF_IS_FIRST_SURROGATE(c) && src.hasNext(&src)) {
        if(UTF_IS_SECOND_SURROGATE(c2=(UChar)src.next(&src))) {
            buffer[bufferIndex++]=c2;
        } else {
            src.move(&src, -1, UITER_CURRENT); /* back out the non-trail-surrogate */
        }
    }

    /* get all following characters until we see a boundary */
    /* checking hasNext() instead of c!=DONE on the off-chance that U+ffff is part of the string */
    while(src.hasNext(&src)) {
        if(isNextBoundary(src, minC, mask, c, c2)) {
            /* back out the latest movement to stop at the boundary */
            src.move(&src, c2==0 ? -1 : -2, UITER_CURRENT);
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
                src.move(&src, 0, UITER_LIMIT);
                return 0;
            }
        }
    }

    /* return the length of the buffer contents */
    return bufferIndex;
}

U_CAPI int32_t U_EXPORT2
unorm_next(UCharIterator *src,
           UChar *dest, int32_t destCapacity,
           UNormalizationMode mode, int32_t options,
           UBool doNormalize, UBool *pNeededToNormalize,
           UErrorCode *pErrorCode) {
    UChar stackBuffer[100];
    UChar *buffer;
    IsNextBoundaryFn *isNextBoundary;
    uint32_t mask;
    int32_t bufferLength, bufferCapacity, destLength;
    int32_t c, c2;
    UChar minC;

    /* check argument values */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( destCapacity<0 || (dest==NULL && destCapacity>0) ||
        src==NULL
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    if(!_haveData(*pErrorCode)) {
        return 0;
    }

    if(pNeededToNormalize!=NULL) {
        *pNeededToNormalize=FALSE;
    }

    switch(mode) {
    case UNORM_NFD:
    case UNORM_FCD:
        isNextBoundary=_isNextNFDSafe;
        minC=_NORM_MIN_WITH_LEAD_CC;
        mask=_NORM_CC_MASK|_NORM_QC_NFD;
        break;
    case UNORM_NFKD:
        isNextBoundary=_isNextNFDSafe;
        minC=_NORM_MIN_WITH_LEAD_CC;
        mask=_NORM_CC_MASK|_NORM_QC_NFKD;
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
        destLength=0;
        if((c=src->next(src))>=0) {
            destLength=1;
            if(UTF_IS_LEAD(c) && (c2=src->next(src))>=0) {
                if(UTF_IS_TRAIL(c2)) {
                    if(destCapacity>=2) {
                        dest[1]=(UChar)c2; /* trail surrogate */
                        destLength=2;
                    }
                    /* lead surrogate to be written below */
                } else {
                    src->move(src, -1, UITER_CURRENT);
                }
            }

            if(destCapacity>0) {
                dest[0]=(UChar)c;
            }
        }
        return u_terminateUChars(dest, destCapacity, destLength, pErrorCode);
    default:
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    buffer=stackBuffer;
    bufferCapacity=(int32_t)(sizeof(stackBuffer)/U_SIZEOF_UCHAR);
    bufferLength=_findNextIterationBoundary(*src,
                                            isNextBoundary, minC, mask,
                                            buffer, bufferCapacity,
                                            pErrorCode);
    if(bufferLength>0) {
        if(doNormalize) {
            destLength=unorm_internalNormalize(dest, destCapacity,
                                               buffer, bufferLength,
                                               mode, options,
                                               pErrorCode);
            if(pNeededToNormalize!=0 && U_SUCCESS(*pErrorCode)) {
                *pNeededToNormalize=
                    (UBool)(destLength!=bufferLength ||
                            0!=uprv_memcmp(dest, buffer, destLength*U_SIZEOF_UCHAR));
            }
        } else {
            /* just copy the source characters */
            if(destCapacity>0) {
                uprv_memcpy(dest, buffer, uprv_min(bufferLength, destCapacity)*U_SIZEOF_UCHAR);
            }
            destLength=u_terminateUChars(dest, destCapacity, bufferLength, pErrorCode);
        }
    } else {
        destLength=u_terminateUChars(dest, destCapacity, 0, pErrorCode);
    }

    /* cleanup */
    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return destLength;
}

/*
 * ### TODO: check if NF*D and FCD iteration finds optimal boundaries
 * and if not, how hard it would be to improve it.
 * For example, see _findSafeFCD().
 */

/* Concatenation of normalized strings -------------------------------------- */

U_CAPI int32_t U_EXPORT2
unorm_concatenate(const UChar *left, int32_t leftLength,
                  const UChar *right, int32_t rightLength,
                  UChar *dest, int32_t destCapacity,
                  UNormalizationMode mode, int32_t options,
                  UErrorCode *pErrorCode) {
    UChar stackBuffer[100];
    UChar *buffer;
    int32_t bufferLength, bufferCapacity;

    UCharIterator iter;
    int32_t leftBoundary, rightBoundary, destLength;

    /* check argument values */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    if( destCapacity<0 || (dest==NULL && destCapacity>0) ||
        left==NULL || leftLength<-1 ||
        right==NULL || rightLength<-1
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* check for overlapping right and destination */
    if( dest!=NULL &&
        ((right>=dest && right<(dest+destCapacity)) ||
         (rightLength>0 && dest>=right && dest<(right+rightLength)))
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* allow left==dest */

    /* set up intermediate buffer */
    buffer=stackBuffer;
    bufferCapacity=(int32_t)(sizeof(stackBuffer)/U_SIZEOF_UCHAR);

    /*
     * Input: left[0..leftLength[ + right[0..rightLength[
     *
     * Find normalization-safe boundaries leftBoundary and rightBoundary
     * and copy the end parts together:
     * buffer=left[leftBoundary..leftLength[ + right[0..rightBoundary[
     *
     * dest=left[0..leftBoundary[ +
     *      normalize(buffer) +
     *      right[rightBoundary..rightLength[
     */

    /*
     * find a normalization boundary at the end of the left string
     * and copy the end part into the buffer
     */
    uiter_setString(&iter, left, leftLength);
    iter.index=leftLength=iter.length; /* end of left string */

    bufferLength=unorm_previous(&iter, buffer, bufferCapacity,
                                mode, options,
                                FALSE, NULL,
                                pErrorCode);
    leftBoundary=iter.index;
    if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
        *pErrorCode=U_ZERO_ERROR;
        if(!u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity, 2*bufferLength, 0)) {
            *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
            /* dont need to cleanup here since 
             * u_growBufferFromStatic frees buffer if(buffer!=stackBuffer)
             */
            return 0;
        }

        /* just copy from the left string: we know the boundary already */
        uprv_memcpy(buffer, left+leftBoundary, bufferLength*U_SIZEOF_UCHAR);
    }

    /*
     * find a normalization boundary at the beginning of the right string
     * and concatenate the beginning part to the buffer
     */
    uiter_setString(&iter, right, rightLength);
    rightLength=iter.length; /* in case it was -1 */

    rightBoundary=unorm_next(&iter, buffer+bufferLength, bufferCapacity-bufferLength,
                             mode, options,
                             FALSE, NULL,
                             pErrorCode);
    if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
        *pErrorCode=U_ZERO_ERROR;
        if(!u_growBufferFromStatic(stackBuffer, &buffer, &bufferCapacity, bufferLength+rightBoundary, 0)) {
            *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
            /* dont need to cleanup here since 
             * u_growBufferFromStatic frees buffer if(buffer!=stackBuffer)
             */
            return 0;
        }

        /* just copy from the right string: we know the boundary already */
        uprv_memcpy(buffer+bufferLength, right, rightBoundary*U_SIZEOF_UCHAR);
    }

    bufferLength+=rightBoundary;

    /* copy left[0..leftBoundary[ to dest */
    if(left!=dest && leftBoundary>0 && destCapacity>0) {
        uprv_memcpy(dest, left, uprv_min(leftBoundary, destCapacity)*U_SIZEOF_UCHAR);
    }
    destLength=leftBoundary;

    /* concatenate the normalization of the buffer to dest */
    if(destCapacity>destLength) {
        destLength+=unorm_internalNormalize(dest+destLength, destCapacity-destLength,
                                            buffer, bufferLength,
                                            mode, options,
                                            pErrorCode);
    } else {
        destLength+=unorm_internalNormalize(NULL, 0,
                                            buffer, bufferLength,
                                            mode, options,
                                            pErrorCode);
    }
    /*
     * only errorCode that is expected is a U_BUFFER_OVERFLOW_ERROR
     * so we dont check for the error code here..just let it pass through
     */
    /* concatenate right[rightBoundary..rightLength[ to dest */
    right+=rightBoundary;
    rightLength-=rightBoundary;
    if(rightLength>0 && destCapacity>destLength) {
        uprv_memcpy(dest+destLength, right, uprv_min(rightLength, destCapacity-destLength)*U_SIZEOF_UCHAR);
    }
    destLength+=rightLength;

    /* cleanup */
    if(buffer!=stackBuffer) {
        uprv_free(buffer);
    }

    return u_terminateUChars(dest, destCapacity, destLength, pErrorCode);
}

/* compare canonically equivalent ------------------------------------------- */

#else

/*
 * Normalization is not built into the ICU library, but case-insensitive
 * comparisons are possible using unorm_cmpEquivFold().
 * The following simply disables the decomposition part.
 */

static inline UBool
_haveData(UErrorCode &errorCode) {
    if(U_SUCCESS(errorCode)) {
        errorCode=U_INTERNAL_PROGRAM_ERROR;
    }
    return FALSE;
}

static inline const UChar *
_decompose(UChar32 /*c*/, UChar /*buffer*/[4], int32_t &/*length*/) {
    return NULL;
}

#endif /* #if !UCONFIG_NO_NORMALIZATION */

/*
 * Compare two strings for canonical equivalence.
 * Further options include case-insensitive comparison and
 * code point order (as opposed to code unit order).
 *
 * In this function, canonical equivalence is optional as well.
 * If canonical equivalence is tested, then both strings must fulfill
 * the FCD check.
 *
 * Semantically, this is equivalent to
 *   strcmp[CodePointOrder](NFD(foldCase(s1)), NFD(foldCase(s2)))
 * where code point order, NFD and foldCase are all optional.
 *
 * String comparisons almost always yield results before processing both strings
 * completely.
 * They are generally more efficient working incrementally instead of
 * performing the sub-processing (strlen, normalization, case-folding)
 * on the entire strings first.
 *
 * It is also unnecessary to not normalize identical characters.
 *
 * This function works in principle as follows:
 *
 * loop {
 *   get one code unit c1 from s1 (-1 if end of source)
 *   get one code unit c2 from s2 (-1 if end of source)
 *
 *   if(either string finished) {
 *     return result;
 *   }
 *   if(c1==c2) {
 *     continue;
 *   }
 *
 *   // c1!=c2
 *   try to decompose/case-fold c1/c2, and continue if one does;
 *
 *   // still c1!=c2 and neither decomposes/case-folds, return result
 *   return c1-c2;
 * }
 *
 * When a character decomposes, then the pointer for that source changes to
 * the decomposition, pushing the previous pointer onto a stack.
 * When the end of the decomposition is reached, then the code unit reader
 * pops the previous source from the stack.
 * (Same for case-folding.)
 *
 * This is complicated further by operating on variable-width UTF-16.
 * The top part of the loop works on code units, while lookups for decomposition
 * and case-folding need code points.
 * Code points are assembled after the equality/end-of-source part.
 * The source pointer is only advanced beyond all code units when the code point
 * actually decomposes/case-folds.
 *
 * If we were on a trail surrogate unit when assembling a code point,
 * and the code point decomposes/case-folds, then the decomposition/folding
 * result must be compared with the part of the other string that corresponds to
 * this string's lead surrogate.
 * Since we only assemble a code point when hitting a trail unit when the
 * preceding lead units were identical, we back up the other string by one unit
 * in such a case.
 *
 * The optional code point order comparison at the end works with
 * the same fix-up as the other code point order comparison functions.
 * See ustring.c and the comment near the end of this function.
 *
 * Assumption: A decomposition or case-folding result string never contains
 * a single surrogate. This is a safe assumption in the Unicode Standard.
 * Therefore, we do not need to check for surrogate pairs across
 * decomposition/case-folding boundaries.
 *
 * Further assumptions (see verifications tstnorm.cpp):
 * The API function checks for FCD first, while the core function
 * first case-folds and then decomposes. This requires that case-folding does not
 * un-FCD any strings.
 *
 * The API function may also NFD the input and turn off decomposition.
 * This requires that case-folding does not un-NFD strings either.
 *
 * TODO If any of the above two assumptions is violated,
 * then this entire code must be re-thought.
 * If this happens, then a simple solution is to case-fold both strings up front
 * and to turn off UNORM_INPUT_IS_FCD.
 * We already do this when not both strings are in FCD because makeFCD
 * would be a partial NFD before the case folding, which does not work.
 * Note that all of this is only a problem when case-folding _and_
 * canonical equivalence come together.
 *
 * This function could be moved to a different source file, at increased cost
 * for calling the decomposition access function.
 */

// stack element for previous-level source/decomposition pointers
struct CmpEquivLevel {
    const UChar *start, *s, *limit;
};
typedef struct CmpEquivLevel CmpEquivLevel;

// internal function
U_CAPI int32_t U_EXPORT2
unorm_cmpEquivFold(const UChar *s1, int32_t length1,
                   const UChar *s2, int32_t length2,
                   uint32_t options,
                   UErrorCode *pErrorCode) {
    // current-level start/limit - s1/s2 as current
    const UChar *start1, *start2, *limit1, *limit2;

    // decomposition variables
    const UChar *p;
    int32_t length;

    // stacks of previous-level start/current/limit
    CmpEquivLevel stack1[2], stack2[2];

    // decomposition buffers for Hangul
    UChar decomp1[4], decomp2[4];

    // case folding buffers, only use current-level start/limit
    UChar fold1[32], fold2[32];

    // track which is the current level per string
    int32_t level1, level2;

    // current code units, and code points for lookups
    int32_t c1, c2, cp1, cp2;

    // no argument error checking because this itself is not an API

    // assume that at least one of the options _COMPARE_EQUIV and U_COMPARE_IGNORE_CASE is set
    // otherwise this function must behave exactly as uprv_strCompare()
    // not checking for that here makes testing this function easier

    // normalization/properties data loaded?
    if( ((options&_COMPARE_EQUIV)!=0 && !_haveData(*pErrorCode)) ||
        ((options&U_COMPARE_IGNORE_CASE)!=0 && !uprv_haveProperties(pErrorCode)) ||
        U_FAILURE(*pErrorCode)
    ) {
        return 0;
    }

    // initialize
    start1=s1;
    if(length1==-1) {
        limit1=NULL;
    } else {
        limit1=s1+length1;
    }

    start2=s2;
    if(length2==-1) {
        limit2=NULL;
    } else {
        limit2=s2+length2;
    }

    level1=level2=0;
    c1=c2=-1;

    // comparison loop
    for(;;) {
        // here a code unit value of -1 means "get another code unit"
        // below it will mean "this source is finished"

        if(c1<0) {
            // get next code unit from string 1, post-increment
            for(;;) {
                if(s1==limit1 || ((c1=*s1)==0 && (limit1==NULL || (options&_STRNCMP_STYLE)))) {
                    if(level1==0) {
                        c1=-1;
                        break;
                    }
                } else {
                    ++s1;
                    break;
                }

                // reached end of level buffer, pop one level
                do {
                    --level1;
                    start1=stack1[level1].start;
                } while(start1==NULL);
                s1=stack1[level1].s;
                limit1=stack1[level1].limit;
            }
        }

        if(c2<0) {
            // get next code unit from string 2, post-increment
            for(;;) {
                if(s2==limit2 || ((c2=*s2)==0 && (limit2==NULL || (options&_STRNCMP_STYLE)))) {
                    if(level2==0) {
                        c2=-1;
                        break;
                    }
                } else {
                    ++s2;
                    break;
                }

                // reached end of level buffer, pop one level
                do {
                    --level2;
                    start2=stack2[level2].start;
                } while(start2==NULL);
                s2=stack2[level2].s;
                limit2=stack2[level2].limit;
            }
        }

        // compare c1 and c2
        // either variable c1, c2 is -1 only if the corresponding string is finished
        if(c1==c2) {
            if(c1<0) {
                return 0;   // c1==c2==-1 indicating end of strings
            }
            c1=c2=-1;       // make us fetch new code units
            continue;
        } else if(c1<0) {
            return -1;      // string 1 ends before string 2
        } else if(c2<0) {
            return 1;       // string 2 ends before string 1
        }
        // c1!=c2 && c1>=0 && c2>=0

        // get complete code points for c1, c2 for lookups if either is a surrogate
        cp1=c1;
        if(UTF_IS_SURROGATE(c1)) {
            UChar c;

            if(UTF_IS_SURROGATE_FIRST(c1)) {
                if(s1!=limit1 && UTF_IS_TRAIL(c=*s1)) {
                    // advance ++s1; only below if cp1 decomposes/case-folds
                    cp1=UTF16_GET_PAIR_VALUE(c1, c);
                }
            } else /* isTrail(c1) */ {
                if(start1<=(s1-2) && UTF_IS_LEAD(c=*(s1-2))) {
                    cp1=UTF16_GET_PAIR_VALUE(c, c1);
                }
            }
        }

        cp2=c2;
        if(UTF_IS_SURROGATE(c2)) {
            UChar c;

            if(UTF_IS_SURROGATE_FIRST(c2)) {
                if(s2!=limit2 && UTF_IS_TRAIL(c=*s2)) {
                    // advance ++s2; only below if cp2 decomposes/case-folds
                    cp2=UTF16_GET_PAIR_VALUE(c2, c);
                }
            } else /* isTrail(c2) */ {
                if(start2<=(s2-2) && UTF_IS_LEAD(c=*(s2-2))) {
                    cp2=UTF16_GET_PAIR_VALUE(c, c2);
                }
            }
        }

        // go down one level for each string
        // continue with the main loop as soon as there is a real change

        if( level1==0 && (options&U_COMPARE_IGNORE_CASE) &&
            (length=u_internalFoldCase((UChar32)cp1, fold1, 32, options))>=0
        ) {
            // cp1 case-folds to fold1[length]
            if(UTF_IS_SURROGATE(c1)) {
                if(UTF_IS_SURROGATE_FIRST(c1)) {
                    // advance beyond source surrogate pair if it case-folds
                    ++s1;
                } else /* isTrail(c1) */ {
                    // we got a supplementary code point when hitting its trail surrogate,
                    // therefore the lead surrogate must have been the same as in the other string;
                    // compare this decomposition with the lead surrogate in the other string
                    // remember that this simulates bulk text replacement:
                    // the decomposition would replace the entire code point
                    --s2;
                    c2=*(s2-1);
                }
            }

            // push current level pointers
            stack1[0].start=start1;
            stack1[0].s=s1;
            stack1[0].limit=limit1;
            ++level1;

            // set next level pointers to case folding
            start1=s1=fold1;
            limit1=fold1+length;

            // get ready to read from decomposition, continue with loop
            c1=-1;
            continue;
        }

        if( level2==0 && (options&U_COMPARE_IGNORE_CASE) &&
            (length=u_internalFoldCase((UChar32)cp2, fold2, 32, options))>=0
        ) {
            // cp2 case-folds to fold2[length]
            if(UTF_IS_SURROGATE(c2)) {
                if(UTF_IS_SURROGATE_FIRST(c2)) {
                    // advance beyond source surrogate pair if it case-folds
                    ++s2;
                } else /* isTrail(c2) */ {
                    // we got a supplementary code point when hitting its trail surrogate,
                    // therefore the lead surrogate must have been the same as in the other string;
                    // compare this decomposition with the lead surrogate in the other string
                    // remember that this simulates bulk text replacement:
                    // the decomposition would replace the entire code point
                    --s1;
                    c1=*(s1-1);
                }
            }

            // push current level pointers
            stack2[0].start=start2;
            stack2[0].s=s2;
            stack2[0].limit=limit2;
            ++level2;

            // set next level pointers to case folding
            start2=s2=fold2;
            limit2=fold2+length;

            // get ready to read from decomposition, continue with loop
            c2=-1;
            continue;
        }

        if( level1<2 && (options&_COMPARE_EQUIV) &&
            0!=(p=_decompose((UChar32)cp1, decomp1, length))
        ) {
            // cp1 decomposes into p[length]
            if(UTF_IS_SURROGATE(c1)) {
                if(UTF_IS_SURROGATE_FIRST(c1)) {
                    // advance beyond source surrogate pair if it decomposes
                    ++s1;
                } else /* isTrail(c1) */ {
                    // we got a supplementary code point when hitting its trail surrogate,
                    // therefore the lead surrogate must have been the same as in the other string;
                    // compare this decomposition with the lead surrogate in the other string
                    // remember that this simulates bulk text replacement:
                    // the decomposition would replace the entire code point
                    --s2;
                    c2=*(s2-1);
                }
            }

            // push current level pointers
            stack1[level1].start=start1;
            stack1[level1].s=s1;
            stack1[level1].limit=limit1;
            ++level1;

            // set empty intermediate level if skipped
            if(level1<2) {
                stack1[level1++].start=NULL;
            }

            // set next level pointers to decomposition
            start1=s1=p;
            limit1=p+length;

            // get ready to read from decomposition, continue with loop
            c1=-1;
            continue;
        }

        if( level2<2 && (options&_COMPARE_EQUIV) &&
            0!=(p=_decompose((UChar32)cp2, decomp2, length))
        ) {
            // cp2 decomposes into p[length]
            if(UTF_IS_SURROGATE(c2)) {
                if(UTF_IS_SURROGATE_FIRST(c2)) {
                    // advance beyond source surrogate pair if it decomposes
                    ++s2;
                } else /* isTrail(c2) */ {
                    // we got a supplementary code point when hitting its trail surrogate,
                    // therefore the lead surrogate must have been the same as in the other string;
                    // compare this decomposition with the lead surrogate in the other string
                    // remember that this simulates bulk text replacement:
                    // the decomposition would replace the entire code point
                    --s1;
                    c1=*(s1-1);
                }
            }

            // push current level pointers
            stack2[level2].start=start2;
            stack2[level2].s=s2;
            stack2[level2].limit=limit2;
            ++level2;

            // set empty intermediate level if skipped
            if(level2<2) {
                stack2[level2++].start=NULL;
            }

            // set next level pointers to decomposition
            start2=s2=p;
            limit2=p+length;

            // get ready to read from decomposition, continue with loop
            c2=-1;
            continue;
        }

        // no decomposition/case folding, max level for both sides:
        // return difference result

        // code point order comparison must not just return cp1-cp2
        // because when single surrogates are present then the surrogate pairs
        // that formed cp1 and cp2 may be from different string indexes

        // example: { d800 d800 dc01 } vs. { d800 dc00 }, compare at second code units
        // c1=d800 cp1=10001 c2=dc00 cp2=10000
        // cp1-cp2>0 but c1-c2<0 and in fact in UTF-32 it is { d800 10001 } < { 10000 }

        // therefore, use same fix-up as in ustring.c/uprv_strCompare()
        // except: uprv_strCompare() fetches c=*s while this functions fetches c=*s++
        // so we have slightly different pointer/start/limit comparisons here

        if(c1>=0xd800 && c2>=0xd800 && (options&U_COMPARE_CODE_POINT_ORDER)) {
            /* subtract 0x2800 from BMP code points to make them smaller than supplementary ones */
            if(
                (c1<=0xdbff && s1!=limit1 && UTF_IS_TRAIL(*s1)) ||
                (UTF_IS_TRAIL(c1) && start1!=(s1-1) && UTF_IS_LEAD(*(s1-2)))
            ) {
                /* part of a surrogate pair, leave >=d800 */
            } else {
                /* BMP code point - may be surrogate code point - make <d800 */
                c1-=0x2800;
            }

            if(
                (c2<=0xdbff && s2!=limit2 && UTF_IS_TRAIL(*s2)) ||
                (UTF_IS_TRAIL(c2) && start2!=(s2-1) && UTF_IS_LEAD(*(s2-2)))
            ) {
                /* part of a surrogate pair, leave >=d800 */
            } else {
                /* BMP code point - may be surrogate code point - make <d800 */
                c2-=0x2800;
            }
        }

        return c1-c2;
    }
}

#if !UCONFIG_NO_NORMALIZATION

U_CAPI int32_t U_EXPORT2
unorm_compare(const UChar *s1, int32_t length1,
              const UChar *s2, int32_t length2,
              uint32_t options,
              UErrorCode *pErrorCode) {
    UChar fcd1[300], fcd2[300];
    UChar *d1, *d2;
    const UnicodeSet *nx;
    UNormalizationMode mode;
    int32_t result;

    /* argument checking */
    if(pErrorCode==0 || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(s1==0 || length1<-1 || s2==0 || length2<-1) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    if(!_haveData(*pErrorCode)) {
        return 0;
    }
    if(!uprv_haveProperties(pErrorCode)) {
        return 0;
    }

    nx=getNX((int32_t)(options>>UNORM_COMPARE_NORM_OPTIONS_SHIFT), *pErrorCode);
    if(U_FAILURE(*pErrorCode)) {
        return 0;
    }

    d1=d2=0;
    options|=_COMPARE_EQUIV;
    result=0;

    /*
     * UAX #21 Case Mappings, as fixed for Unicode version 4
     * (see Jitterbug 2021), defines a canonical caseless match as
     *
     * A string X is a canonical caseless match
     * for a string Y if and only if
     * NFD(toCasefold(NFD(X))) = NFD(toCasefold(NFD(Y)))
     *
     * For better performance, we check for FCD (or let the caller tell us that
     * both strings are in FCD) for the inner normalization.
     * BasicNormalizerTest::FindFoldFCDExceptions() makes sure that
     * case-folding preserves the FCD-ness of a string.
     * The outer normalization is then only performed by unorm_cmpEquivFold()
     * when there is a difference.
     *
     * Exception: When using the Turkic case-folding option, we do perform
     * full NFD first. This is because in the Turkic case precomposed characters
     * with 0049 capital I or 0069 small i fold differently whether they
     * are first decomposed or not, so an FCD check - a check only for
     * canonical order - is not sufficient.
     */
    if(options&U_FOLD_CASE_EXCLUDE_SPECIAL_I) {
        mode=UNORM_NFD;
        options&=~UNORM_INPUT_IS_FCD;
    } else {
        mode=UNORM_FCD;
    }

    if(!(options&UNORM_INPUT_IS_FCD)) {
        int32_t _len1, _len2;
        UBool isFCD1, isFCD2;

        // check if s1 and/or s2 fulfill the FCD conditions
        isFCD1= UNORM_YES==_quickCheck(s1, length1, mode, TRUE, nx, pErrorCode);
        isFCD2= UNORM_YES==_quickCheck(s2, length2, mode, TRUE, nx, pErrorCode);
        if(U_FAILURE(*pErrorCode)) {
            return 0;
        }

        /*
         * ICU 2.4 had a further optimization:
         * If both strings were not in FCD, then they were both NFD'ed,
         * and the _COMPARE_EQUIV option was turned off.
         * It is not entirely clear that this is valid with the current
         * definition of the canonical caseless match.
         * Therefore, ICU 2.6 removes that optimization.
         */

        if(!isFCD1) {
            _len1=unorm_internalNormalize(fcd1, LENGTHOF(fcd1),
                                          s1, length1,
                                          mode, nx,
                                          pErrorCode);
            if(*pErrorCode!=U_BUFFER_OVERFLOW_ERROR) {
                s1=fcd1;
            } else {
                d1=(UChar *)uprv_malloc(_len1*U_SIZEOF_UCHAR);
                if(d1==0) {
                    *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                    goto cleanup;
                }

                *pErrorCode=U_ZERO_ERROR;
                _len1=unorm_internalNormalize(d1, _len1,
                                              s1, length1,
                                              mode, nx,
                                              pErrorCode);
                if(U_FAILURE(*pErrorCode)) {
                    goto cleanup;
                }

                s1=d1;
            }
            length1=_len1;
        }

        if(!isFCD2) {
            _len2=unorm_internalNormalize(fcd2, LENGTHOF(fcd2),
                                          s2, length2,
                                          mode, nx,
                                          pErrorCode);
            if(*pErrorCode!=U_BUFFER_OVERFLOW_ERROR) {
                s2=fcd2;
            } else {
                d2=(UChar *)uprv_malloc(_len2*U_SIZEOF_UCHAR);
                if(d2==0) {
                    *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                    goto cleanup;
                }

                *pErrorCode=U_ZERO_ERROR;
                _len2=unorm_internalNormalize(d2, _len2,
                                              s2, length2,
                                              mode, nx,
                                              pErrorCode);
                if(U_FAILURE(*pErrorCode)) {
                    goto cleanup;
                }

                s2=d2;
            }
            length2=_len2;
        }
    }

    if(U_SUCCESS(*pErrorCode)) {
        result=unorm_cmpEquivFold(s1, length1, s2, length2, options, pErrorCode);
    }

cleanup:
    if(d1!=0) {
        uprv_free(d1);
    }
    if(d2!=0) {
        uprv_free(d2);
    }

    return result;
}

/* data swapping ------------------------------------------------------------ */

U_CAPI int32_t U_EXPORT2
unorm_swap(const UDataSwapper *ds,
           const void *inData, int32_t length, void *outData,
           UErrorCode *pErrorCode) {
    const UDataInfo *pInfo;
    int32_t headerSize;

    const uint8_t *inBytes;
    uint8_t *outBytes;

    const int32_t *inIndexes;
    int32_t indexes[32];

    int32_t i, offset, count, size;

    /* udata_swapDataHeader checks the arguments */
    headerSize=udata_swapDataHeader(ds, inData, length, outData, pErrorCode);
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }

    /* check data format and format version */
    pInfo=(const UDataInfo *)((const char *)inData+4);
    if(!(
        pInfo->dataFormat[0]==0x4e &&   /* dataFormat="Norm" */
        pInfo->dataFormat[1]==0x6f &&
        pInfo->dataFormat[2]==0x72 &&
        pInfo->dataFormat[3]==0x6d &&
        pInfo->formatVersion[0]==2
    )) {
        udata_printError(ds, "unorm_swap(): data format %02x.%02x.%02x.%02x (format version %02x) is not recognized as unorm.icu\n",
                         pInfo->dataFormat[0], pInfo->dataFormat[1],
                         pInfo->dataFormat[2], pInfo->dataFormat[3],
                         pInfo->formatVersion[0]);
        *pErrorCode=U_UNSUPPORTED_ERROR;
        return 0;
    }

    inBytes=(const uint8_t *)inData+headerSize;
    outBytes=(uint8_t *)outData+headerSize;

    inIndexes=(const int32_t *)inBytes;

    if(length>=0) {
        length-=headerSize;
        if(length<32*4) {
            udata_printError(ds, "unorm_swap(): too few bytes (%d after header) for unorm.icu\n",
                             length);
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return 0;
        }
    }

    /* read the first 32 indexes (ICU 2.8/format version 2.2: _NORM_INDEX_TOP==32, might grow) */
    for(i=0; i<32; ++i) {
        indexes[i]=udata_readInt32(ds, inIndexes[i]);
    }

    /* calculate the total length of the data */
    size=
        32*4+ /* size of indexes[] */
        indexes[_NORM_INDEX_TRIE_SIZE]+
        indexes[_NORM_INDEX_UCHAR_COUNT]*2+
        indexes[_NORM_INDEX_COMBINE_DATA_COUNT]*2+
        indexes[_NORM_INDEX_FCD_TRIE_SIZE]+
        indexes[_NORM_INDEX_AUX_TRIE_SIZE]+
        indexes[_NORM_INDEX_CANON_SET_COUNT]*2;

    if(length>=0) {
        if(length<size) {
            udata_printError(ds, "unorm_swap(): too few bytes (%d after header) for all of unorm.icu\n",
                             length);
            *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
            return 0;
        }

        /* copy the data for inaccessible bytes */
        if(inBytes!=outBytes) {
            uprv_memcpy(outBytes, inBytes, size);
        }

        offset=0;

        /* swap the indexes[] */
        count=32*4;
        ds->swapArray32(ds, inBytes, count, outBytes, pErrorCode);
        offset+=count;

        /* swap the main UTrie */
        count=indexes[_NORM_INDEX_TRIE_SIZE];
        utrie_swap(ds, inBytes+offset, count, outBytes+offset, pErrorCode);
        offset+=count;

        /* swap the uint16_t extraData[] and the uint16_t combiningTable[] */
        count=(indexes[_NORM_INDEX_UCHAR_COUNT]+indexes[_NORM_INDEX_COMBINE_DATA_COUNT])*2;
        ds->swapArray16(ds, inBytes+offset, count, outBytes+offset, pErrorCode);
        offset+=count;

        /* swap the FCD UTrie */
        count=indexes[_NORM_INDEX_FCD_TRIE_SIZE];
        if(count!=0) {
            utrie_swap(ds, inBytes+offset, count, outBytes+offset, pErrorCode);
            offset+=count;
        }

        /* swap the aux UTrie */
        count=indexes[_NORM_INDEX_AUX_TRIE_SIZE];
        if(count!=0) {
            utrie_swap(ds, inBytes+offset, count, outBytes+offset, pErrorCode);
            offset+=count;
        }

        /* swap the uint16_t combiningTable[] */
        count=indexes[_NORM_INDEX_CANON_SET_COUNT]*2;
        ds->swapArray16(ds, inBytes+offset, count, outBytes+offset, pErrorCode);
        offset+=count;
    }

    return headerSize+size;
}

#endif /* #if !UCONFIG_NO_NORMALIZATION */
