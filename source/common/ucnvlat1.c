/* 
**********************************************************************
*   Copyright (C) 2000, International Business Machines
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
#include "ucnv_bld.h"
#include "ucnv_cnv.h"

/* ISO 8859-1 --------------------------------------------------------------- */

/* This is a table-less and callback-less version of _MBCSSingleToBMPWithOffsets(). */
U_CFUNC void
_Latin1ToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode) {
    const uint8_t *source;
    UChar *target;
    int32_t targetCapacity, length;
    int32_t *offsets;

    /* set up the local pointers */
    source=(const uint8_t *)pArgs->source;
    target=pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;

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

    /* conversion loop */
    while(targetCapacity>0) {
        *target++=*source++;
        --targetCapacity;
    }

    /* write back the updated pointers */
    pArgs->source=(const char *)source;
    pArgs->target=target;

    /* set offsets */
    offsets=pArgs->offsets;
    if(offsets!=NULL) {
        int32_t sourceIndex=0;

        while(length>0) {
            *offsets++=sourceIndex++;
            --length;
        }
        pArgs->offsets=offsets;
    }
}

/* This is a table-less and callback-less version of _MBCSSingleGetNextUChar(). */
U_CFUNC UChar32
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
U_CFUNC void
_Latin1FromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                              UErrorCode *pErrorCode) {
    UConverter *cnv;
    const UChar *source, *sourceLimit, *lastSource;
    uint8_t *target;
    int32_t targetCapacity, length;
    int32_t *offsets;

    UChar32 c, max;

    int32_t sourceIndex;

    UConverterCallbackReason reason;
    int32_t i;

    /* set up the local pointers */
    cnv=pArgs->converter;
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetCapacity=pArgs->targetLimit-pArgs->target;
    offsets=pArgs->offsets;

    max=0xff; /* ### 0x7f for US-ASCII */

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
        if(c<=max) {
            /* convert the Unicode code point */
            *target++=(uint8_t)c;
            --targetCapacity;

            /* normal end of conversion: prepare for a new character */
            c=0;
        } else {
            if(!UTF_IS_SURROGATE(c)) {
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
            cnv->fromUCharErrorBehaviour(cnv->fromUContext, pArgs, cnv->invalidUCharBuffer, i, c, reason, pErrorCode);

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
    NULL
};

const UConverterStaticData _Latin1StaticData={
    sizeof(UConverterStaticData),
    "LATIN_1",
    819, UCNV_IBM, UCNV_LATIN_1, 1, 1,
    { 0x1a, 0, 0, 0 },1,FALSE, FALSE,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _Latin1Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_Latin1StaticData, FALSE, &_Latin1Impl, 
    0
};
