/* 
**********************************************************************
*   Copyright (C) 2000-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnvlat1.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000feb07
*   created by: Markus W. Scherer
*/

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "unicode/uset.h"
#include "ucnv_bld.h"
#include "ucnv_cnv.h"

/* control optimizations according to the platform */
#define LATIN1_UNROLL_TO_UNICODE 1
#define LATIN1_UNROLL_FROM_UNICODE 1
#define ASCII_UNROLL_TO_UNICODE 1

/* ISO 8859-1 --------------------------------------------------------------- */

/* This is a table-less and callback-less version of _MBCSSingleToBMPWithOffsets(). */
static void
_Latin1ToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode) {
    const uint8_t *source;
    UChar *target;
    int32_t targetCapacity, length;
    int32_t *offsets;

    int32_t sourceIndex;

    /* set up the local pointers */
    source=(const uint8_t *)pArgs->source;
    target=pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    sourceIndex=0;

    /*
     * since the conversion here is 1:1 UChar:uint8_t, we need only one counter
     * for the minimum of the sourceLength and targetCapacity
     */
    length=(const uint8_t *)pArgs->sourceLimit-source;
    if(length<=targetCapacity) {
        targetCapacity=length;
    } else {
        /* target will be full */
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        length=targetCapacity;
    }

#if LATIN1_UNROLL_TO_UNICODE
    if(targetCapacity>=16) {
        int32_t count, loops;

        loops=count=targetCapacity>>4;
        length=targetCapacity&=0xf;
        do {
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
            *target++=*source++;
        } while(--count>0);

        if(offsets!=NULL) {
            do {
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
            } while(--loops>0);
        }
    }
#endif

    /* conversion loop */
    while(targetCapacity>0) {
        *target++=*source++;
        --targetCapacity;
    }

    /* write back the updated pointers */
    pArgs->source=(const char *)source;
    pArgs->target=target;

    /* set offsets */
    if(offsets!=NULL) {
        while(length>0) {
            *offsets++=sourceIndex++;
            --length;
        }
        pArgs->offsets=offsets;
    }
}

/* This is a table-less and callback-less version of _MBCSSingleGetNextUChar(). */
static UChar32
_Latin1GetNextUChar(UConverterToUnicodeArgs *pArgs,
                    UErrorCode *pErrorCode) {
    const uint8_t *source=(const uint8_t *)pArgs->source;
    if(source<(const uint8_t *)pArgs->sourceLimit) {
        pArgs->source=(const char *)(source+1);
        return *source;
    }

    /* no output because of empty input */
    *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}

/* This is a table-less version of _MBCSSingleFromBMPWithOffsets(). */
static void
_Latin1FromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                              UErrorCode *pErrorCode) {
    UConverter *cnv;
    const UChar *source, *sourceLimit;
    uint8_t *target, *oldTarget;
    int32_t targetCapacity, length;
    int32_t *offsets;

    UChar32 cp;
    UChar c, max;

    int32_t sourceIndex;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=oldTarget=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    if(cnv->sharedData==&_Latin1Data) {
        max=0xff; /* Latin-1 */
    } else {
        max=0x7f; /* US-ASCII */
    }

    /* get the converter state from UConverter */
    cp=cnv->fromUChar32;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex= cp==0 ? 0 : -1;

    /*
     * since the conversion here is 1:1 UChar:uint8_t, we need only one counter
     * for the minimum of the sourceLength and targetCapacity
     */
    length=sourceLimit-source;
    if(length<targetCapacity) {
        targetCapacity=length;
    }

    /* conversion loop */
    if(cp!=0 && targetCapacity>0) {
        goto getTrail;
    }

#if LATIN1_UNROLL_FROM_UNICODE
    /* unroll the loop with the most common case */
    if(targetCapacity>=16) {
        int32_t count, loops;
        UChar u, oredChars;

        loops=count=targetCapacity>>4;
        do {
            oredChars=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;
            oredChars|=u=*source++;
            *target++=(uint8_t)u;

            /* were all 16 entries really valid? */
            if(oredChars>max) {
                /* no, return to the first of these 16 */
                source-=16;
                target-=16;
                break;
            }
        } while(--count>0);
        count=loops-count;
        targetCapacity-=16*count;

        if(offsets!=NULL) {
            oldTarget+=16*count;
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
    c=0;
    while(targetCapacity>0 && (c=*source++)<=max) {
        /* convert the Unicode code point */
        *target++=(uint8_t)c;
        --targetCapacity;
    }

    if(c>max) {
        cp=c;
        if(!U_IS_SURROGATE(cp)) {
            /* callback(unassigned) */
        } else if(U_IS_SURROGATE_LEAD(cp)) {
getTrail:
            if(source<sourceLimit) {
                /* test the following code unit */
                UChar trail=*source;
                if(U16_IS_TRAIL(trail)) {
                    ++source;
                    cp=U16_GET_SUPPLEMENTARY(cp, trail);
                    /* this codepage does not map supplementary code points */
                    /* callback(unassigned) */
                } else {
                    /* this is an unmatched lead code unit (1st surrogate) */
                    /* callback(illegal) */
                }
            } else {
                /* no more input */
                cnv->fromUChar32=cp;
                goto noMoreInput;
            }
        } else {
            /* this is an unmatched trail code unit (2nd surrogate) */
            /* callback(illegal) */
        }

        *pErrorCode= U_IS_SURROGATE(cp) ? U_ILLEGAL_CHAR_FOUND : U_INVALID_CHAR_FOUND;
        cnv->fromUChar32=cp;
    }
noMoreInput:

    /* set offsets since the start */
    if(offsets!=NULL) {
        size_t count=target-oldTarget;
        while(count>0) {
            *offsets++=sourceIndex++;
            --count;
        }
    }

    if(U_SUCCESS(*pErrorCode) && source<sourceLimit && target>=(uint8_t *)pArgs->targetLimit) {
        /* target is full */
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }

    /* write back the updated pointers */
    pArgs->source=source;
    pArgs->target=(char *)target;
    pArgs->offsets=offsets;
}

static void
_Latin1GetUnicodeSet(const UConverter *cnv,
                     USet *set,
                     UConverterUnicodeSet which,
                     UErrorCode *pErrorCode) {
    uset_addRange(set, 0, 0xff);
}

static const UConverterImpl _Latin1Impl={
    UCNV_LATIN_1,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    _Latin1ToUnicodeWithOffsets,
    _Latin1ToUnicodeWithOffsets,
    _Latin1FromUnicodeWithOffsets,
    _Latin1FromUnicodeWithOffsets,
    _Latin1GetNextUChar,

    NULL,
    NULL,
    NULL,
    NULL,
    _Latin1GetUnicodeSet
};

static const UConverterStaticData _Latin1StaticData={
    sizeof(UConverterStaticData),
    "ISO-8859-1",
    819, UCNV_IBM, UCNV_LATIN_1, 1, 1,
    { 0x1a, 0, 0, 0 }, 1, FALSE, FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};

const UConverterSharedData _Latin1Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_Latin1StaticData, FALSE, &_Latin1Impl, 
    0
};

/* US-ASCII ----------------------------------------------------------------- */

/* This is a table-less version of _MBCSSingleToBMPWithOffsets(). */
static void
_ASCIIToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                           UErrorCode *pErrorCode) {
    const uint8_t *source, *sourceLimit;
    UChar *target, *oldTarget;
    int32_t targetCapacity, length;
    int32_t *offsets;

    int32_t sourceIndex;

    uint8_t c;

    /* set up the local pointers */
    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;
    target=oldTarget=pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex=0;

    /*
     * since the conversion here is 1:1 UChar:uint8_t, we need only one counter
     * for the minimum of the sourceLength and targetCapacity
     */
    length=sourceLimit-source;
    if(length<targetCapacity) {
        targetCapacity=length;
    }

#if ASCII_UNROLL_TO_UNICODE
    /* unroll the loop with the most common case */
    if(targetCapacity>=16) {
        int32_t count, loops;
        UChar oredChars;

        loops=count=targetCapacity>>4;
        do {
            oredChars=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;
            oredChars|=*target++=*source++;

            /* were all 16 entries really valid? */
            if(oredChars>0x7f) {
                /* no, return to the first of these 16 */
                source-=16;
                target-=16;
                break;
            }
        } while(--count>0);
        count=loops-count;
        targetCapacity-=16*count;

        if(offsets!=NULL) {
            oldTarget+=16*count;
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
    c=0;
    while(targetCapacity>0 && (c=*source++)<=0x7f) {
        *target++=c;
        --targetCapacity;
    }

    if(c>0x7f) {
        /* callback(illegal); copy the current bytes to toUBytes[] */
        UConverter *cnv=pArgs->converter;
        cnv->toUBytes[0]=c;
        cnv->toULength=1;
        *pErrorCode=U_ILLEGAL_CHAR_FOUND;
    } else if(source<sourceLimit && target>=pArgs->targetLimit) {
        /* target is full */
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }

    /* set offsets since the start */
    if(offsets!=NULL) {
        size_t count=target-oldTarget;
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

/* This is a table-less version of _MBCSSingleGetNextUChar(). */
static UChar32
_ASCIIGetNextUChar(UConverterToUnicodeArgs *pArgs,
                   UErrorCode *pErrorCode) {
    const uint8_t *source;
    uint8_t b;

    source=(const uint8_t *)pArgs->source;
    if(source<(const uint8_t *)pArgs->sourceLimit) {
        b=*source++;
        pArgs->source=(const char *)source;
        if(b<=0x7f) {
            return b;
        } else {
            UConverter *cnv=pArgs->converter;
            cnv->toUBytes[0]=b;
            cnv->toULength=1;
            *pErrorCode=U_ILLEGAL_CHAR_FOUND;
            return 0xffff;
        }
    }

    /* no output because of empty input */
    *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}

static void
_ASCIIGetUnicodeSet(const UConverter *cnv,
                    USet *set,
                    UConverterUnicodeSet which,
                    UErrorCode *pErrorCode) {
    uset_addRange(set, 0, 0x7f);
}

static const UConverterImpl _ASCIIImpl={
    UCNV_US_ASCII,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    _ASCIIToUnicodeWithOffsets,
    _ASCIIToUnicodeWithOffsets,
    _Latin1FromUnicodeWithOffsets,
    _Latin1FromUnicodeWithOffsets,
    _ASCIIGetNextUChar,

    NULL,
    NULL,
    NULL,
    NULL,
    _ASCIIGetUnicodeSet
};

static const UConverterStaticData _ASCIIStaticData={
    sizeof(UConverterStaticData),
    "US-ASCII",
    367, UCNV_IBM, UCNV_US_ASCII, 1, 1,
    { 0x1a, 0, 0, 0 }, 1, FALSE, FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};

const UConverterSharedData _ASCIIData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_ASCIIStaticData, FALSE, &_ASCIIImpl, 
    0
};
