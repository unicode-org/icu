/*  
**********************************************************************
*   Copyright (C) 2002-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv_u16.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002jul01
*   created by: Markus W. Scherer
*
*   UTF-16 converter implementation. Used to be in ucnv_utf.c.
*/

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "ucnv_cnv.h"
#include "cmemory.h"

/* UTF-16 Platform Endian --------------------------------------------------- */

static void
_UTF16PEToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                             UErrorCode *pErrorCode) {
    UConverter *cnv         = pArgs->converter;
    const uint8_t *source   = (const uint8_t *)pArgs->source;
    UChar *target           = pArgs->target;
    int32_t *offsets        = pArgs->offsets;
    int32_t targetCapacity  = pArgs->targetLimit - pArgs->target;
    int32_t length          = (const uint8_t *)pArgs->sourceLimit - source;
    int32_t count;
    int32_t sourceIndex     = 0;

    if(length <= 0 && cnv->toUnicodeStatus == 0) {
        /* no input, nothing to do */
        return;
    }

    if(targetCapacity <= 0) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        return;
    }

    /* complete a partial UChar from the last call */
    if(length != 0 && cnv->toUnicodeStatus != 0) {
        /*
         * copy the byte from the last call and the first one here into the target,
         * byte-wise to keep the platform endianness
         */
        uint8_t *p = (uint8_t *)target++;
        *p++ = (uint8_t)cnv->toUnicodeStatus;
        cnv->toUnicodeStatus = 0;
        *p = *source++;
        --length;
        --targetCapacity;
        if(offsets != NULL) {
            *offsets++ = -1;
        }
    }

    /* copy an even number of bytes for complete UChars */
    count = 2 * targetCapacity;
    if(count > length) {
        count = length & ~1;
    }
    if(count > 0) {
        uprv_memcpy(target, source, count);
        source += count;
        length -= count;
        count >>= 1;
        target += count;
        targetCapacity -= count;
        if(offsets != NULL) {
            while(count > 0) {
                *offsets++ = sourceIndex;
                sourceIndex += 2;
                --count;
            }
        }
    }

    /* check for a remaining source byte and store the status */
    if(length >= 2) {
        /* it must be targetCapacity==0 because otherwise the above would have copied more */
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
    } else if(length == 1) {
        if(pArgs->flush) {
            /* a UChar remains incomplete */
            *pErrorCode = U_TRUNCATED_CHAR_FOUND;
        } else {
            /* consume the last byte and store it, making sure that it will never set the status to 0 */
            cnv->toUnicodeStatus = *source++ | 0x100;
        }
    } else /* length==0 */ if(cnv->toUnicodeStatus!=0 && pArgs->flush) {
        /* a UChar remains incomplete */
        *pErrorCode = U_TRUNCATED_CHAR_FOUND;
    }

    /* write back the updated pointers */
    pArgs->source = (const char *)source;
    pArgs->target = target;
    pArgs->offsets = offsets;
}

static void
_UTF16PEFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                               UErrorCode *pErrorCode) {
    UConverter *cnv         = pArgs->converter;
    const UChar *source     = pArgs->source;
    uint8_t *target         = (uint8_t *)pArgs->target;
    int32_t *offsets        = pArgs->offsets;
    int32_t targetCapacity  = pArgs->targetLimit - pArgs->target;
    int32_t length          = pArgs->sourceLimit - source;
    int32_t count;
    int32_t sourceIndex     = 0;

    if(length <= 0 && cnv->fromUnicodeStatus == 0) {
        /* no input, nothing to do */
        return;
    }

    if(targetCapacity <= 0) {
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        return;
    }

    /* complete a partial UChar from the last call */
    if(cnv->fromUnicodeStatus != 0) {
        *target++ = (uint8_t)cnv->fromUnicodeStatus;
        cnv->fromUnicodeStatus = 0;
        --targetCapacity;
        if(offsets != NULL) {
            *offsets++ = -1;
        }
    }

    /* copy an even number of bytes for complete UChars */
    count = 2 * length;
    if(count > targetCapacity) {
        count = targetCapacity & ~1;
    }
    if(count>0) {
        uprv_memcpy(target, source, count);
        target += count;
        targetCapacity -= count;
        count >>= 1;
        source += count;
        length -= count;
        if(offsets != NULL) {
            while(count > 0) {
                *offsets++ = sourceIndex;
                *offsets++ = sourceIndex++;
                --count;
            }
        }
    }

    if(length > 0) {
        /* it must be targetCapacity<=1 because otherwise the above would have copied more */
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        if(targetCapacity > 0) /* targetCapacity==1 */ {
            /* copy one byte and keep the other in the status */
            const uint8_t *p = (const uint8_t *)source++;
            *target++ = *p++;
            cnv->fromUnicodeStatus = *p | 0x100;
            if(offsets != NULL) {
                *offsets++ = sourceIndex;
            }
        }
    }

    /* write back the updated pointers */
    pArgs->source = source;
    pArgs->target = (char *)target;
    pArgs->offsets = offsets;
}

/* UTF-16 Opposite Endian --------------------------------------------------- */

/*
 * For opposite-endian UTF-16, we keep a byte pointer to the UChars
 * and copy two bytes at a time and reverse them.
 */

static void
_UTF16OEToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                             UErrorCode *pErrorCode) {
    UConverter *cnv         = pArgs->converter;
    const uint8_t *source   = (const uint8_t *)pArgs->source;
    UChar *target           = pArgs->target;
    uint8_t *target8        = (uint8_t *)target; /* byte pointer to the target */
    int32_t *offsets        = pArgs->offsets;
    int32_t targetCapacity  = pArgs->targetLimit - pArgs->target;
    int32_t length          = (const uint8_t *)pArgs->sourceLimit - source;
    int32_t count;
    int32_t sourceIndex     = 0;

    if(length <= 0 && cnv->toUnicodeStatus == 0) {
        /* no input, nothing to do */
        return;
    }

    if(targetCapacity <= 0) {
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        return;
    }

    /* complete a partial UChar from the last call */
    if(length != 0 && cnv->toUnicodeStatus != 0) {
        /*
         * copy the byte from the last call and the first one here into the target,
         * byte-wise, reversing the platform endianness
         */
        *target8++ = *source++;
        *target8++ = (uint8_t)cnv->toUnicodeStatus;
        cnv->toUnicodeStatus = 0;
        ++target;
        --length;
        --targetCapacity;
        if(offsets != NULL) {
            *offsets++ = -1;
        }
    }

    /* copy an even number of bytes for complete UChars */
    count = 2 * targetCapacity;
    if(count > length) {
        count = length & ~1;
    }
    if(count>0) {
        length -= count;
        count >>= 1;
        targetCapacity -= count;
        if(offsets == NULL) {
            while(count > 0) {
                target8[1] = *source++;
                target8[0] = *source++;
                target8 += 2;
                --count;
            }
        } else {
            while(count>0) {
                target8[1] = *source++;
                target8[0] = *source++;
                target8 += 2;
                *offsets++ = sourceIndex;
                sourceIndex += 2;
                --count;
            }
        }
        target=(UChar *)target8;
    }

    /* check for a remaining source byte and store the status */
    if(length >= 2) {
        /* it must be targetCapacity==0 because otherwise the above would have copied more */
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
    } else if(length == 1) {
        if(pArgs->flush) {
            /* a UChar remains incomplete */
            *pErrorCode = U_TRUNCATED_CHAR_FOUND;
        } else {
            /* consume the last byte and store it, making sure that it will never set the status to 0 */
            cnv->toUnicodeStatus = *source++ | 0x100;
        }
    } else /* length==0 */ if(cnv->toUnicodeStatus!=0 && pArgs->flush) {
        /* a UChar remains incomplete */
        *pErrorCode = U_TRUNCATED_CHAR_FOUND;
    }

    /* write back the updated pointers */
    pArgs->source = (const char *)source;
    pArgs->target = target;
    pArgs->offsets = offsets;
}

static void
_UTF16OEFromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                               UErrorCode *pErrorCode) {
    UConverter *cnv         = pArgs->converter;
    const UChar *source     = pArgs->source;
    const uint8_t *source8  = (const uint8_t *)source; /* byte pointer to the source */
    uint8_t *target         = (uint8_t *)pArgs->target;
    int32_t *offsets        = pArgs->offsets;
    int32_t targetCapacity  = pArgs->targetLimit - pArgs->target;
    int32_t length          = pArgs->sourceLimit - source;
    int32_t count;
    int32_t sourceIndex = 0;

    if(length <= 0 && cnv->fromUnicodeStatus == 0) {
        /* no input, nothing to do */
        return;
    }

    if(targetCapacity <= 0) {
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        return;
    }

    /* complete a partial UChar from the last call */
    if(cnv->fromUnicodeStatus != 0) {
        *target++ = (uint8_t)cnv->fromUnicodeStatus;
        cnv->fromUnicodeStatus = 0;
        --targetCapacity;
        if(offsets != NULL) {
            *offsets++ = -1;
        }
    }

    /* copy an even number of bytes for complete UChars */
    count = 2 * length;
    if(count > targetCapacity) {
        count = targetCapacity & ~1;
    }
    if(count > 0) {
        targetCapacity -= count;
        count >>= 1;
        length -= count;
        if(offsets == NULL) {
            while(count > 0) {
                target[1] = *source8++;
                target[0] = *source8++;
                target += 2;
                --count;
            }
        } else {
            while(count>0) {
                target[1] = *source8++;
                target[0] = *source8++;
                target += 2;
                *offsets++ = sourceIndex;
                *offsets++ = sourceIndex++;
                --count;
            }
        }
        source=(const UChar *)source8;
    }

    if(length > 0) {
        /* it must be targetCapacity<=1 because otherwise the above would have copied more */
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        if(targetCapacity > 0) /* targetCapacity==1 */ {
            /* copy one byte and keep the other in the status */
            cnv->fromUnicodeStatus = *source8++ | 0x100;
            *target++ = *source8;
            ++source;
            if(offsets != NULL) {
                *offsets++ = sourceIndex;
            }
        }
    }

    /* write back the updated pointers */
    pArgs->source = source;
    pArgs->target = (char *)target;
    pArgs->offsets = offsets;
}

/* UTF-16BE ----------------------------------------------------------------- */

#if U_IS_BIG_ENDIAN
#   define _UTF16BEToUnicodeWithOffsets     _UTF16PEToUnicodeWithOffsets
#   define _UTF16LEToUnicodeWithOffsets     _UTF16OEToUnicodeWithOffsets
#   define _UTF16BEFromUnicodeWithOffsets   _UTF16PEFromUnicodeWithOffsets
#   define _UTF16LEFromUnicodeWithOffsets   _UTF16OEFromUnicodeWithOffsets
#else
#   define _UTF16BEToUnicodeWithOffsets     _UTF16OEToUnicodeWithOffsets
#   define _UTF16LEToUnicodeWithOffsets     _UTF16PEToUnicodeWithOffsets
#   define _UTF16BEFromUnicodeWithOffsets   _UTF16OEFromUnicodeWithOffsets
#   define _UTF16LEFromUnicodeWithOffsets   _UTF16PEFromUnicodeWithOffsets
#endif

static UChar32 T_UConverter_getNextUChar_UTF16_BE(UConverterToUnicodeArgs* args,
                                                   UErrorCode* err)
{
    UChar32 myUChar;
    uint16_t first;
    /*Checks boundaries and set appropriate error codes*/
    if (args->source+2 > args->sourceLimit) 
    {
        if (args->source >= args->sourceLimit)
        {
            /*Either caller has reached the end of the byte stream*/
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
        }
        else
        {
            /* a character was cut in half*/
            *err = U_TRUNCATED_CHAR_FOUND;
        }
        return 0xffff;
    }

    /*Gets the corresponding codepoint*/
    first = (uint16_t)(((uint16_t)(*(args->source)) << 8) |((uint8_t)*((args->source)+1)));
    myUChar = first;
    args->source += 2;

    if(UTF_IS_FIRST_SURROGATE(first)) {
        uint16_t second;

        if (args->source+2 > args->sourceLimit) {
            *err = U_TRUNCATED_CHAR_FOUND;
            return 0xffff;
        }

        /* get the second surrogate and assemble the code point */
        second = (uint16_t)(((uint16_t)(*(args->source)) << 8) |((uint8_t)*(args->source+1)));

        /* ignore unmatched surrogates and just deliver the first one in such a case */
        if(UTF_IS_SECOND_SURROGATE(second)) {
            /* matched pair, get pair value */
            myUChar = UTF16_GET_PAIR_VALUE(first, second);
            args->source += 2;
        }
    }

    return myUChar;
} 

static const UConverterImpl _UTF16BEImpl={
    UCNV_UTF16_BigEndian,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    _UTF16BEToUnicodeWithOffsets,
    _UTF16BEToUnicodeWithOffsets,
    _UTF16BEFromUnicodeWithOffsets,
    _UTF16BEFromUnicodeWithOffsets,
    T_UConverter_getNextUChar_UTF16_BE,

    NULL,
    NULL,
    NULL,
    NULL,
    ucnv_getCompleteUnicodeSet
};

/* The 1200 CCSID refers to any version of Unicode with any endianess of UTF-16 */
static const UConverterStaticData _UTF16BEStaticData={
    sizeof(UConverterStaticData),
    "UTF-16BE",
    1200, UCNV_IBM, UCNV_UTF16_BigEndian, 2, 2,
    { 0xff, 0xfd, 0, 0 },2,FALSE,FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _UTF16BEData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16BEStaticData, FALSE, &_UTF16BEImpl, 
    0
};

/* UTF-16LE ----------------------------------------------------------------- */

static UChar32 T_UConverter_getNextUChar_UTF16_LE(UConverterToUnicodeArgs* args,
                                                   UErrorCode* err)
{
    UChar32 myUChar;
    uint16_t first;
    /*Checks boundaries and set appropriate error codes*/
    if (args->source+2 > args->sourceLimit) 
    {
        if (args->source >= args->sourceLimit)
        {
            /*Either caller has reached the end of the byte stream*/
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
        }
        else
        {
            /* a character was cut in half*/
            *err = U_TRUNCATED_CHAR_FOUND;
        }

        return 0xffff;
    }

    /*Gets the corresponding codepoint*/
    first = (uint16_t)(((uint16_t)*((args->source)+1) << 8) | ((uint8_t)(*(args->source))));
    myUChar=first;
    /*updates the source*/
    args->source += 2;  

    if (UTF_IS_FIRST_SURROGATE(first))
    {
        uint16_t second;

        if (args->source+2 > args->sourceLimit)
        {
           *err = U_TRUNCATED_CHAR_FOUND;
            return 0xffff;
        }

        /* get the second surrogate and assemble the code point */
        second = (uint16_t)(((uint16_t)*(args->source+1) << 8) |((uint8_t)(*(args->source))));

        /* ignore unmatched surrogates and just deliver the first one in such a case */
        if(UTF_IS_SECOND_SURROGATE(second))
        {
            /* matched pair, get pair value */
            myUChar = UTF16_GET_PAIR_VALUE(first, second);
            args->source += 2;
        }
    }

    return myUChar;
} 

static const UConverterImpl _UTF16LEImpl={
    UCNV_UTF16_LittleEndian,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    _UTF16LEToUnicodeWithOffsets,
    _UTF16LEToUnicodeWithOffsets,
    _UTF16LEFromUnicodeWithOffsets,
    _UTF16LEFromUnicodeWithOffsets,
    T_UConverter_getNextUChar_UTF16_LE,

    NULL,
    NULL,
    NULL,
    NULL,
    ucnv_getCompleteUnicodeSet
};


/* The 1200 CCSID refers to any version of Unicode with any endianess of UTF-16 */
static const UConverterStaticData _UTF16LEStaticData={
    sizeof(UConverterStaticData),
    "UTF-16LE",
    1202, UCNV_IBM, UCNV_UTF16_LittleEndian, 2, 2,
    { 0xfd, 0xff, 0, 0 },2,FALSE,FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _UTF16LEData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16LEStaticData, FALSE, &_UTF16LEImpl, 
    0
};

/* UTF-16 (Detect BOM) ------------------------------------------------------ */

/*
 * Detect a BOM at the beginning of the stream and select UTF-16BE or UTF-16LE
 * accordingly.
 * This is a simpler version of the UTF-32 converter below, with
 * fewer states for shorter BOMs.
 *
 * State values:
 * 0    initial state
 * 1    saw FE
 * 2..4 -
 * 5    saw FF
 * 6..7 -
 * 8    UTF-16BE mode
 * 9    UTF-16LE mode
 *
 * During detection: state&3==number of matching bytes so far.
 *
 * On output, emit U+FEFF as the first code point.
 */

static void
_UTF16Reset(UConverter *cnv, UConverterResetChoice choice) {
    if(choice<=UCNV_RESET_TO_UNICODE) {
        /* reset toUnicode: state=0 */
        cnv->mode=0;
    }
    if(choice!=UCNV_RESET_TO_UNICODE) {
        /* reset fromUnicode: prepare to output the UTF-16PE BOM */
        cnv->charErrorBufferLength=2;
#if U_IS_BIG_ENDIAN
        cnv->charErrorBuffer[0]=0xfe;
        cnv->charErrorBuffer[1]=0xff;
#else
        cnv->charErrorBuffer[0]=0xff;
        cnv->charErrorBuffer[1]=0xfe;
#endif
    }
}

static void
_UTF16Open(UConverter *cnv,
           const char *name,
           const char *locale,
           uint32_t options,
           UErrorCode *pErrorCode) {
    _UTF16Reset(cnv, UCNV_RESET_BOTH);
}

static const char utf16BOM[8]={ (char)0xfe, (char)0xff, 0, 0,    (char)0xff, (char)0xfe, 0, 0 };

static void
_UTF16ToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                           UErrorCode *pErrorCode) {
    UConverter *cnv=pArgs->converter;
    const char *source=pArgs->source;
    const char *sourceLimit=pArgs->sourceLimit;
    int32_t *offsets=pArgs->offsets;

    int32_t state, offsetDelta;
    char b;

    state=cnv->mode;

    /*
     * If we detect a BOM in this buffer, then we must add the BOM size to the
     * offsets because the actual converter function will not see and count the BOM.
     * offsetDelta will have the number of the BOM bytes that are in the current buffer.
     */
    offsetDelta=0;

    while(source<sourceLimit && U_SUCCESS(*pErrorCode)) {
        switch(state) {
        case 0:
            b=*source;
            if(b==(char)0xfe) {
                state=1; /* could be FE FF */
            } else if(b==(char)0xff) {
                state=5; /* could be FF FE */
            } else {
                state=8; /* default to UTF-16BE */
                continue;
            }
            ++source;
            break;
        case 1:
        case 5:
            if(*source==utf16BOM[state]) {
                ++source;
                if(state==1) {
                    state=8; /* detect UTF-16BE */
                    offsetDelta=source-pArgs->source;
                } else if(state==5) {
                    state=9; /* detect UTF-16LE */
                    offsetDelta=source-pArgs->source;
                }
            } else {
                /* switch to UTF-16BE and pass the previous bytes */
                if(source!=pArgs->source) {
                    /* just reset the source */
                    source=pArgs->source;
                } else {
                    UBool oldFlush=pArgs->flush;

                    /* the first byte is from a previous buffer, replay it first */
                    pArgs->source=utf16BOM+(state&4); /* select the correct BOM */
                    pArgs->sourceLimit=pArgs->source+1; /* replay previous byte */
                    pArgs->flush=FALSE; /* this sourceLimit is not the real source stream limit */

                    _UTF16BEToUnicodeWithOffsets(pArgs, pErrorCode);

                    /* restore real pointers; pArgs->source will be set in case 8/9 */
                    pArgs->sourceLimit=sourceLimit;
                    pArgs->flush=oldFlush;
                }
                state=8;
                continue;
            }
            break;
        case 8:
            /* call UTF-16BE */
            pArgs->source=source;
            _UTF16BEToUnicodeWithOffsets(pArgs, pErrorCode);
            source=pArgs->source;
            break;
        case 9:
            /* call UTF-16LE */
            pArgs->source=source;
            _UTF16LEToUnicodeWithOffsets(pArgs, pErrorCode);
            source=pArgs->source;
            break;
        default:
            break; /* does not occur */
        }
    }

    /* add BOM size to offsets - see comment at offsetDelta declaration */
    if(offsets!=NULL && offsetDelta!=0) {
        int32_t *offsetsLimit=pArgs->offsets;
        while(offsets<offsetsLimit) {
            *offsets++ += offsetDelta;
        }
    }

    pArgs->source=source;

    if(source==sourceLimit && pArgs->flush) {
        /* handle truncated input */
        switch(state) {
        case 0:
            break; /* no input at all, nothing to do */
        case 8:
            _UTF16BEToUnicodeWithOffsets(pArgs, pErrorCode);
            break;
        case 9:
            _UTF16LEToUnicodeWithOffsets(pArgs, pErrorCode);
            break;
        default:
            /* handle 0<state<8: call UTF-16BE with too-short input */
            pArgs->source=utf16BOM+(state&4); /* select the correct BOM */
            pArgs->sourceLimit=pArgs->source+(state&3); /* replay bytes */

            /* no offsets: not enough for output */
            _UTF16BEToUnicodeWithOffsets(pArgs, pErrorCode);
            pArgs->source=source;
            pArgs->sourceLimit=sourceLimit;
            break;
        }
        cnv->mode=0; /* reset */
    } else {
        cnv->mode=state;
    }
}

static UChar32
_UTF16GetNextUChar(UConverterToUnicodeArgs *pArgs,
                   UErrorCode *pErrorCode) {
    switch(pArgs->converter->mode) {
    case 8:
        return T_UConverter_getNextUChar_UTF16_BE(pArgs, pErrorCode);
    case 9:
        return T_UConverter_getNextUChar_UTF16_LE(pArgs, pErrorCode);
    default:
        return ucnv_getNextUCharFromToUImpl(pArgs, _UTF16ToUnicodeWithOffsets, TRUE, pErrorCode);
    }
}

static const UConverterImpl _UTF16Impl = {
    UCNV_UTF16,

    NULL,
    NULL,

    _UTF16Open,
    NULL,
    _UTF16Reset,

    _UTF16ToUnicodeWithOffsets,
    _UTF16ToUnicodeWithOffsets,
    _UTF16PEFromUnicodeWithOffsets,
    _UTF16PEFromUnicodeWithOffsets,
    _UTF16GetNextUChar,

    NULL, /* ### TODO implement getStarters for all Unicode encodings?! */
    NULL,
    NULL,
    NULL,
    ucnv_getCompleteUnicodeSet
};

static const UConverterStaticData _UTF16StaticData = {
    sizeof(UConverterStaticData),
    "UTF-16",
    0, /* ### TODO review correctness of all Unicode CCSIDs */
    UCNV_IBM, UCNV_UTF16, 2, 2,
#if U_IS_BIG_ENDIAN
    { 0xff, 0xfd, 0, 0 }, 2,
#else
    { 0xfd, 0xff, 0, 0 }, 2,
#endif
    FALSE, FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};

const UConverterSharedData _UTF16Data = {
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16StaticData, FALSE, &_UTF16Impl, 
    0
};
