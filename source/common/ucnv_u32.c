/*  
**********************************************************************
*   Copyright (C) 2002-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv_u32.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002jul01
*   created by: Markus W. Scherer
*
*   UTF-32 converter implementation. Used to be in ucnv_utf.c.
*/

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "ucnv_cnv.h"
#include "cmemory.h"

#define MAXIMUM_UCS2            0x0000FFFF
#define MAXIMUM_UTF             0x0010FFFF
#define MAXIMUM_UCS4            0x7FFFFFFF
#define HALF_SHIFT              10
#define HALF_BASE               0x0010000
#define HALF_MASK               0x3FF
#define SURROGATE_HIGH_START    0xD800
#define SURROGATE_HIGH_END      0xDBFF
#define SURROGATE_LOW_START     0xDC00
#define SURROGATE_LOW_END       0xDFFF

/* -SURROGATE_LOW_START + HALF_BASE */
#define SURROGATE_LOW_BASE      9216

/**
 * Calls invalid char callback when an invalid character sequence is encountered.
 * It presumes that the converter has a callback to call.
 *
 * @returns true when callback fails
 */
static UBool
T_UConverter_toUnicode_InvalidChar_Callback(UConverterToUnicodeArgs * args,
                                            UConverterCallbackReason reason,
                                            UErrorCode *err)
{
    UConverter *converter = args->converter;

    if (U_SUCCESS(*err))
    {
        if (reason == UCNV_ILLEGAL) {
            *err = U_ILLEGAL_CHAR_FOUND;
        } else {
            *err = U_INVALID_CHAR_FOUND;
        }
    }

    /* copy the toUBytes to the invalidCharBuffer */
    uprv_memcpy(converter->invalidCharBuffer,
                converter->toUBytes,
                converter->invalidCharLength);

    /* Call the ErrorFunction */
    args->converter->fromCharErrorBehaviour(converter->toUContext,
                                            args,
                                            converter->invalidCharBuffer,
                                            converter->invalidCharLength,
                                            reason,
                                            err);

    return (UBool)U_FAILURE(*err);
}

static UBool
T_UConverter_toUnicode_InvalidChar_OffsetCallback(UConverterToUnicodeArgs * args,
                                                  int32_t currentOffset,
                                                  UConverterCallbackReason reason,
                                                  UErrorCode *err)
{
    int32_t *saveOffsets = args->offsets;
    UBool result;
    
    result = T_UConverter_toUnicode_InvalidChar_Callback(args, reason, err);

    while (saveOffsets < args->offsets)
    {
        *(saveOffsets++) = currentOffset;
    }
    return result;
}

/* UTF-32BE ----------------------------------------------------------------- */

static void
T_UConverter_toUnicode_UTF32_BE(UConverterToUnicodeArgs * args,
                                UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    const unsigned char *sourceLimit = (unsigned char *) args->sourceLimit;
    const UChar *targetLimit = args->targetLimit;
    unsigned char *toUBytes = args->converter->toUBytes;
    uint32_t ch, i;

    /* UTF-8 returns here for only non-offset, this needs to change.*/
    if (args->converter->toUnicodeStatus && myTarget < targetLimit)
    {
        i = args->converter->toULength;       /* restore # of bytes consumed */

        ch = args->converter->toUnicodeStatus - 1;/*Stores the previously calculated ch from a previous call*/
        args->converter->toUnicodeStatus = 0;
        goto morebytes;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        i = 0;
        ch = 0;
morebytes:
        while (i < sizeof(uint32_t))
        {
            if (mySource < sourceLimit)
            {
                ch = (ch << 8) | (uint8_t)(*mySource);
                toUBytes[i++] = (char) *(mySource++);
            }
            else
            {
                if (args->flush)
                {
                    if (U_SUCCESS(*err))
                    {
                        *err = U_TRUNCATED_CHAR_FOUND;
                        args->converter->toUnicodeStatus = MAXIMUM_UCS4;
                    }
                }
                else
                {   /* stores a partially calculated target*/
                    /* + 1 to make 0 a valid character */
                    args->converter->toUnicodeStatus = ch + 1;
                    args->converter->toULength = (int8_t) i;
                }
                goto donefornow;
            }
        }

        if (ch <= MAXIMUM_UTF)
        {
            /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
            if (ch <= MAXIMUM_UCS2) 
            {
                /* fits in 16 bits */
                *(myTarget++) = (UChar) ch;
            }
            else
            {
                /* write out the surrogates */
                ch -= HALF_BASE;
                *(myTarget++) = (UChar) ((ch >> HALF_SHIFT) + SURROGATE_HIGH_START);
                ch = (ch & HALF_MASK) + SURROGATE_LOW_START;
                if (myTarget < targetLimit)
                {
                    *(myTarget++) = (UChar)ch;
                }
                else
                {
                    /* Put in overflow buffer (not handled here) */
                    args->converter->UCharErrorBuffer[0] = (UChar) ch;
                    args->converter->UCharErrorBufferLength = 1;
                    *err = U_BUFFER_OVERFLOW_ERROR;
                    break;
                }
            }
        }
        else
        {
            args->source = (const char *) mySource;
            args->target = myTarget;
            args->converter->invalidCharLength = (int8_t)i;
            if (T_UConverter_toUnicode_InvalidChar_Callback(args, UCNV_ILLEGAL, err))
            {
                /* Stop if the error wasn't handled */
                break;
            }
            args->converter->invalidCharLength = 0;
            mySource = (unsigned char *) args->source;
            myTarget = args->target;
        }
    }

donefornow:
    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        /* End of target buffer */
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = myTarget;
    args->source = (const char *) mySource;
}

static void
T_UConverter_toUnicode_UTF32_BE_OFFSET_LOGIC(UConverterToUnicodeArgs * args,
                                             UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    int32_t *myOffsets = args->offsets;
    const unsigned char *sourceLimit = (unsigned char *) args->sourceLimit;
    const UChar *targetLimit = args->targetLimit;
    unsigned char *toUBytes = args->converter->toUBytes;
    uint32_t ch, i;
    int32_t offsetNum = 0;

    if (args->converter->toUnicodeStatus && myTarget < targetLimit)
    {
        i = args->converter->toULength;       /* restore # of bytes consumed */

        ch = args->converter->toUnicodeStatus - 1;/*Stores the previously calculated ch from a previous call*/
        args->converter->toUnicodeStatus = 0;
        goto morebytes;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        i = 0;
        ch = 0;
morebytes:
        while (i < sizeof(uint32_t))
        {
            if (mySource < sourceLimit)
            {
                ch = (ch << 8) | (uint8_t)(*mySource);
                toUBytes[i++] = (char) *(mySource++);
            }
            else
            {
                if (args->flush)
                {
                    if (U_SUCCESS(*err))
                    {
                        *err = U_TRUNCATED_CHAR_FOUND;
                        args->converter->toUnicodeStatus = MAXIMUM_UCS4;
                    }
                }
                else
                {   /* stores a partially calculated target*/
                    /* + 1 to make 0 a valid character */
                    args->converter->toUnicodeStatus = ch + 1;
                    args->converter->toULength = (int8_t) i;
                }
                goto donefornow;
            }
        }

        if (ch <= MAXIMUM_UTF)
        {
            /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
            if (ch <= MAXIMUM_UCS2) 
            {
                /* fits in 16 bits */
                *(myTarget++) = (UChar) ch;
                *(myOffsets++) = offsetNum;
            }
            else
            {
                /* write out the surrogates */
                ch -= HALF_BASE;
                *(myTarget++) = (UChar) ((ch >> HALF_SHIFT) + SURROGATE_HIGH_START);
                *myOffsets++ = offsetNum;
                ch = (ch & HALF_MASK) + SURROGATE_LOW_START;
                if (myTarget < targetLimit)
                {
                    *(myTarget++) = (UChar)ch;
                    *(myOffsets++) = offsetNum;
                }
                else
                {
                    /* Put in overflow buffer (not handled here) */
                    args->converter->UCharErrorBuffer[0] = (UChar) ch;
                    args->converter->UCharErrorBufferLength = 1;
                    *err = U_BUFFER_OVERFLOW_ERROR;
                    break;
                }
            }
        }
        else
        {
            args->source = (const char *) mySource;
            args->target = myTarget;
            args->converter->invalidCharLength = (int8_t)i;
            args->offsets = myOffsets;
            if (T_UConverter_toUnicode_InvalidChar_OffsetCallback(args, offsetNum, UCNV_ILLEGAL, err))
            {
                /* Stop if the error wasn't handled */
                break;
            }
            args->converter->invalidCharLength = 0;
            mySource = (unsigned char *) args->source;
            myTarget = args->target;
            myOffsets = args->offsets;
        }
        offsetNum += i;
    }

donefornow:
    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        /* End of target buffer */
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = myTarget;
    args->source = (const char *) mySource;
    args->offsets = myOffsets;
}

static void
T_UConverter_fromUnicode_UTF32_BE(UConverterFromUnicodeArgs * args,
                                  UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    const UChar *sourceLimit = args->sourceLimit;
    const unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    UChar32 ch, ch2;
    unsigned int indexToWrite;
    unsigned char temp[sizeof(uint32_t)];

    temp[0] = 0;

    if (args->converter->fromUnicodeStatus)
    {
        ch = args->converter->fromUnicodeStatus;
        args->converter->fromUnicodeStatus = 0;
        goto lowsurogate;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        ch = *(mySource++);

        if (SURROGATE_HIGH_START <= ch && ch < SURROGATE_LOW_START)
        {
lowsurogate:
            if (mySource < sourceLimit)
            {
                ch2 = *mySource;
                if (SURROGATE_LOW_START <= ch2 && ch2 <= SURROGATE_LOW_END)
                {
                    ch = ((ch - SURROGATE_HIGH_START) << HALF_SHIFT) + ch2 + SURROGATE_LOW_BASE;
                    mySource++;
                }
            }
            else if (!args->flush)
            {
                /* ran out of source */
                args->converter->fromUnicodeStatus = ch;
                break;
            }
        }

        /* We cannot get any larger than 10FFFF because we are coming from UTF-16 */
        temp[1] = (uint8_t) (ch >> 16 & 0x1F);
        temp[2] = (uint8_t) (ch >> 8);  /* unsigned cast implicitly does (ch & FF) */
        temp[3] = (uint8_t) (ch);       /* unsigned cast implicitly does (ch & FF) */

        for (indexToWrite = 0; indexToWrite <= sizeof(uint32_t) - 1; indexToWrite++)
        {
            if (myTarget < targetLimit)
            {
                *(myTarget++) = temp[indexToWrite];
            }
            else
            {
                args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = temp[indexToWrite];
                *err = U_BUFFER_OVERFLOW_ERROR;
            }
        }
    }

    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = (char *) myTarget;
    args->source = mySource;
}

static void
T_UConverter_fromUnicode_UTF32_BE_OFFSET_LOGIC(UConverterFromUnicodeArgs * args,
                                               UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t *myOffsets = args->offsets;
    const UChar *sourceLimit = args->sourceLimit;
    const unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    UChar32 ch, ch2;
    int32_t offsetNum = 0;
    unsigned int indexToWrite;
    unsigned char temp[sizeof(uint32_t)];

    temp[0] = 0;

    if (args->converter->fromUnicodeStatus)
    {
        ch = args->converter->fromUnicodeStatus;
        args->converter->fromUnicodeStatus = 0;
        goto lowsurogate;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        ch = *(mySource++);

        if (SURROGATE_HIGH_START <= ch && ch < SURROGATE_LOW_START)
        {
lowsurogate:
            if (mySource < sourceLimit)
            {
                ch2 = *mySource;
                if (SURROGATE_LOW_START <= ch2 && ch2 <= SURROGATE_LOW_END)
                {
                    ch = ((ch - SURROGATE_HIGH_START) << HALF_SHIFT) + ch2 + SURROGATE_LOW_BASE;
                    mySource++;
                }
            }
            else if (!args->flush)
            {
                /* ran out of source */
                args->converter->fromUnicodeStatus = ch;
                break;
            }
        }

        /* We cannot get any larger than 10FFFF because we are coming from UTF-16 */
        temp[1] = (uint8_t) (ch >> 16 & 0x1F);
        temp[2] = (uint8_t) (ch >> 8);  /* unsigned cast implicitly does (ch & FF) */
        temp[3] = (uint8_t) (ch);       /* unsigned cast implicitly does (ch & FF) */

        for (indexToWrite = 0; indexToWrite <= sizeof(uint32_t) - 1; indexToWrite++)
        {
            if (myTarget < targetLimit)
            {
                *(myTarget++) = temp[indexToWrite];
                *(myOffsets++) = offsetNum;
            }
            else
            {
                args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = temp[indexToWrite];
                *err = U_BUFFER_OVERFLOW_ERROR;
            }
        }
        offsetNum++;
    }

    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = (char *) myTarget;
    args->source = mySource;
    args->offsets = myOffsets;
}

static UChar32
T_UConverter_getNextUChar_UTF32_BE(UConverterToUnicodeArgs* args,
                                   UErrorCode* err)
{
    UChar myUCharBuf[2];
    UChar *myUCharPtr;
    const unsigned char *mySource;
    UChar32 myUChar;
    int32_t length;

    while (args->source < args->sourceLimit)
    {
        if (args->source + 4 > args->sourceLimit) 
        {
            /* got a partial character */
            *err = U_TRUNCATED_CHAR_FOUND;
            return 0xffff;
        }

        /* Don't even try to do a direct cast because the value may be on an odd address. */
        mySource = (unsigned char *) args->source;
        myUChar = (mySource[0] << 24)
                | (mySource[1] << 16)
                | (mySource[2] << 8)
                | (mySource[3]);

        args->source = (const char *)(mySource + 4);
        if (myUChar <= MAXIMUM_UTF && myUChar >= 0) {
            return myUChar;
        }

        uprv_memcpy(args->converter->invalidCharBuffer, mySource, 4);
        args->converter->invalidCharLength = 4;

        myUCharPtr = myUCharBuf;
        *err = U_ILLEGAL_CHAR_FOUND;
        args->target = myUCharPtr;
        args->targetLimit = myUCharBuf + 2;
        args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                        args,
                                        (const char *)mySource,
                                        4,
                                        UCNV_ILLEGAL,
                                        err);

        if(U_SUCCESS(*err)) {
            length = (uint16_t)(args->target - myUCharBuf);
            if(length > 0) {
                return ucnv_getUChar32KeepOverflow(args->converter, myUCharBuf, length);
            }
            /* else (callback did not write anything) continue */
        } else if(*err == U_BUFFER_OVERFLOW_ERROR) {
            *err = U_ZERO_ERROR;
            return ucnv_getUChar32KeepOverflow(args->converter, myUCharBuf, 2);
        } else {
            /* break on error */
            /* ### what if a callback set an error but _also_ generated output?! */
            return 0xffff;
        }
    }

    /* no input or only skipping callbacks */
    *err = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}

static const UConverterImpl _UTF32BEImpl = {
    UCNV_UTF32_BigEndian,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_UTF32_BE,
    T_UConverter_toUnicode_UTF32_BE_OFFSET_LOGIC,
    T_UConverter_fromUnicode_UTF32_BE,
    T_UConverter_fromUnicode_UTF32_BE_OFFSET_LOGIC,
    T_UConverter_getNextUChar_UTF32_BE,

    NULL,
    NULL,
    NULL,
    NULL,
    ucnv_getCompleteUnicodeSet
};

/* The 1232 CCSID refers to any version of Unicode with any endianess of UTF-32 */
static const UConverterStaticData _UTF32BEStaticData = {
    sizeof(UConverterStaticData),
    "UTF-32BE",
    1232,
    UCNV_IBM, UCNV_UTF32_BigEndian, 4, 4,
    { 0, 0, 0xff, 0xfd }, 4, FALSE, FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};

const UConverterSharedData _UTF32BEData = {
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF32BEStaticData, FALSE, &_UTF32BEImpl, 
    0
};

/* UTF-32LE ---------------------------------------------------------- */

static void
T_UConverter_toUnicode_UTF32_LE(UConverterToUnicodeArgs * args,
                                UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    const unsigned char *sourceLimit = (unsigned char *) args->sourceLimit;
    const UChar *targetLimit = args->targetLimit;
    unsigned char *toUBytes = args->converter->toUBytes;
    uint32_t ch, i;

    /* UTF-8 returns here for only non-offset, this needs to change.*/
    if (args->converter->toUnicodeStatus && myTarget < targetLimit)
    {
        i = args->converter->toULength;       /* restore # of bytes consumed */

        /* Stores the previously calculated ch from a previous call*/
        ch = args->converter->toUnicodeStatus - 1;
        args->converter->toUnicodeStatus = 0;
        goto morebytes;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        i = 0;
        ch = 0;
morebytes:
        while (i < sizeof(uint32_t))
        {
            if (mySource < sourceLimit)
            {
                ch |= ((uint8_t)(*mySource)) << (i * 8);
                toUBytes[i++] = (char) *(mySource++);
            }
            else
            {
                if (args->flush)
                {
                    if (U_SUCCESS(*err))
                    {
                        *err = U_TRUNCATED_CHAR_FOUND;
                        args->converter->toUnicodeStatus = 0;
                    }
                }
                else
                {   /* stores a partially calculated target*/
                    /* + 1 to make 0 a valid character */
                    args->converter->toUnicodeStatus = ch + 1;
                    args->converter->toULength = (int8_t) i;
                }
                goto donefornow;
            }
        }

        if (ch <= MAXIMUM_UTF)
        {
            /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
            if (ch <= MAXIMUM_UCS2) 
            {
                /* fits in 16 bits */
                *(myTarget++) = (UChar) ch;
            }
            else
            {
                /* write out the surrogates */
                ch -= HALF_BASE;
                *(myTarget++) = (UChar) ((ch >> HALF_SHIFT) + SURROGATE_HIGH_START);
                ch = (ch & HALF_MASK) + SURROGATE_LOW_START;
                if (myTarget < targetLimit)
                {
                    *(myTarget++) = (UChar)ch;
                }
                else
                {
                    /* Put in overflow buffer (not handled here) */
                    args->converter->UCharErrorBuffer[0] = (UChar) ch;
                    args->converter->UCharErrorBufferLength = 1;
                    *err = U_BUFFER_OVERFLOW_ERROR;
                    break;
                }
            }
        }
        else
        {
            args->source = (const char *) mySource;
            args->target = myTarget;
            args->converter->invalidCharLength = (int8_t)i;
            if (T_UConverter_toUnicode_InvalidChar_Callback(args, UCNV_ILLEGAL, err))
            {
                /* Stop if the error wasn't handled */
                break;
            }
            args->converter->invalidCharLength = 0;
            mySource = (unsigned char *) args->source;
            myTarget = args->target;
        }
    }

donefornow:
    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        /* End of target buffer */
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = myTarget;
    args->source = (const char *) mySource;
}

static void
T_UConverter_toUnicode_UTF32_LE_OFFSET_LOGIC(UConverterToUnicodeArgs * args,
                                             UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    int32_t *myOffsets = args->offsets;
    const unsigned char *sourceLimit = (unsigned char *) args->sourceLimit;
    const UChar *targetLimit = args->targetLimit;
    unsigned char *toUBytes = args->converter->toUBytes;
    uint32_t ch, i;
    int32_t offsetNum = 0;

    /* UTF-8 returns here for only non-offset, this needs to change.*/
    if (args->converter->toUnicodeStatus && myTarget < targetLimit)
    {
        i = args->converter->toULength;       /* restore # of bytes consumed */

        /* Stores the previously calculated ch from a previous call*/
        ch = args->converter->toUnicodeStatus - 1;
        args->converter->toUnicodeStatus = 0;
        goto morebytes;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        i = 0;
        ch = 0;
morebytes:
        while (i < sizeof(uint32_t))
        {
            if (mySource < sourceLimit)
            {
                ch |= ((uint8_t)(*mySource)) << (i * 8);
                toUBytes[i++] = (char) *(mySource++);
            }
            else
            {
                if (args->flush)
                {
                    if (U_SUCCESS(*err))
                    {
                        *err = U_TRUNCATED_CHAR_FOUND;
                        args->converter->toUnicodeStatus = 0;
                    }
                }
                else
                {   /* stores a partially calculated target*/
                    /* + 1 to make 0 a valid character */
                    args->converter->toUnicodeStatus = ch + 1;
                    args->converter->toULength = (int8_t) i;
                }
                goto donefornow;
            }
        }

        if (ch <= MAXIMUM_UTF)
        {
            /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
            if (ch <= MAXIMUM_UCS2) 
            {
                /* fits in 16 bits */
                *(myTarget++) = (UChar) ch;
                *(myOffsets++) = offsetNum;
            }
            else
            {
                /* write out the surrogates */
                ch -= HALF_BASE;
                *(myTarget++) = (UChar) ((ch >> HALF_SHIFT) + SURROGATE_HIGH_START);
                *(myOffsets++) = offsetNum;
                ch = (ch & HALF_MASK) + SURROGATE_LOW_START;
                if (myTarget < targetLimit)
                {
                    *(myTarget++) = (UChar)ch;
                    *(myOffsets++) = offsetNum;
                }
                else
                {
                    /* Put in overflow buffer (not handled here) */
                    args->converter->UCharErrorBuffer[0] = (UChar) ch;
                    args->converter->UCharErrorBufferLength = 1;
                    *err = U_BUFFER_OVERFLOW_ERROR;
                    break;
                }
            }
        }
        else
        {
            args->source = (const char *) mySource;
            args->target = myTarget;
            args->converter->invalidCharLength = (int8_t)i;
            args->offsets = myOffsets;
            if (T_UConverter_toUnicode_InvalidChar_OffsetCallback(args, offsetNum, UCNV_ILLEGAL, err))
            {
                /* Stop if the error wasn't handled */
                break;
            }
            args->converter->invalidCharLength = 0;
            mySource = (unsigned char *) args->source;
            myTarget = args->target;
            myOffsets = args->offsets;
        }
        offsetNum += i;
    }

donefornow:
    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        /* End of target buffer */
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = myTarget;
    args->source = (const char *) mySource;
    args->offsets = myOffsets;
}

static void
T_UConverter_fromUnicode_UTF32_LE(UConverterFromUnicodeArgs * args,
                                  UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    const UChar *sourceLimit = args->sourceLimit;
    const unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    UChar32 ch, ch2;
    unsigned int indexToWrite;
    unsigned char temp[sizeof(uint32_t)];

    temp[3] = 0;

    if (args->converter->fromUnicodeStatus)
    {
        ch = args->converter->fromUnicodeStatus;
        args->converter->fromUnicodeStatus = 0;
        goto lowsurogate;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        ch = *(mySource++);

        if (SURROGATE_HIGH_START <= ch && ch < SURROGATE_LOW_START)
        {
lowsurogate:
            if (mySource < sourceLimit)
            {
                ch2 = *mySource;
                if (SURROGATE_LOW_START <= ch2 && ch2 <= SURROGATE_LOW_END)
                {
                    ch = ((ch - SURROGATE_HIGH_START) << HALF_SHIFT) + ch2 + SURROGATE_LOW_BASE;
                    mySource++;
                }
            }
            else if (!args->flush)
            {
                /* ran out of source */
                args->converter->fromUnicodeStatus = ch;
                break;
            }
        }

        /* We cannot get any larger than 10FFFF because we are coming from UTF-16 */
        temp[2] = (uint8_t) (ch >> 16 & 0x1F);
        temp[1] = (uint8_t) (ch >> 8);  /* unsigned cast implicitly does (ch & FF) */
        temp[0] = (uint8_t) (ch);       /* unsigned cast implicitly does (ch & FF) */

        for (indexToWrite = 0; indexToWrite <= sizeof(uint32_t) - 1; indexToWrite++)
        {
            if (myTarget < targetLimit)
            {
                *(myTarget++) = temp[indexToWrite];
            }
            else
            {
                args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = temp[indexToWrite];
                *err = U_BUFFER_OVERFLOW_ERROR;
            }
        }
    }

    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = (char *) myTarget;
    args->source = mySource;
}

static void
T_UConverter_fromUnicode_UTF32_LE_OFFSET_LOGIC(UConverterFromUnicodeArgs * args,
                                               UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t *myOffsets = args->offsets;
    const UChar *sourceLimit = args->sourceLimit;
    const unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    UChar32 ch, ch2;
    unsigned int indexToWrite;
    unsigned char temp[sizeof(uint32_t)];
    int32_t offsetNum = 0;

    temp[3] = 0;

    if (args->converter->fromUnicodeStatus)
    {
        ch = args->converter->fromUnicodeStatus;
        args->converter->fromUnicodeStatus = 0;
        goto lowsurogate;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        ch = *(mySource++);

        if (SURROGATE_HIGH_START <= ch && ch < SURROGATE_LOW_START)
        {
lowsurogate:
            if (mySource < sourceLimit)
            {
                ch2 = *mySource;
                if (SURROGATE_LOW_START <= ch2 && ch2 <= SURROGATE_LOW_END)
                {
                    ch = ((ch - SURROGATE_HIGH_START) << HALF_SHIFT) + ch2 + SURROGATE_LOW_BASE;
                    mySource++;
                }
            }
            else if (!args->flush)
            {
                /* ran out of source */
                args->converter->fromUnicodeStatus = ch;
                break;
            }
        }

        /* We cannot get any larger than 10FFFF because we are coming from UTF-16 */
        temp[2] = (uint8_t) (ch >> 16 & 0x1F);
        temp[1] = (uint8_t) (ch >> 8);  /* unsigned cast implicitly does (ch & FF) */
        temp[0] = (uint8_t) (ch);       /* unsigned cast implicitly does (ch & FF) */

        for (indexToWrite = 0; indexToWrite <= sizeof(uint32_t) - 1; indexToWrite++)
        {
            if (myTarget < targetLimit)
            {
                *(myTarget++) = temp[indexToWrite];
                *(myOffsets++) = offsetNum;
            }
            else
            {
                args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = temp[indexToWrite];
                *err = U_BUFFER_OVERFLOW_ERROR;
            }
        }
        offsetNum++;
    }

    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = (char *) myTarget;
    args->source = mySource;
    args->offsets = myOffsets;
}

static UChar32
T_UConverter_getNextUChar_UTF32_LE(UConverterToUnicodeArgs* args,
                                   UErrorCode* err)
{
    UChar myUCharBuf[2];
    UChar *myUCharPtr;
    const unsigned char *mySource;
    UChar32 myUChar;
    int32_t length;

    while (args->source < args->sourceLimit)
    {
        if (args->source + 4 > args->sourceLimit) 
        {
            /* got a partial character */
            *err = U_TRUNCATED_CHAR_FOUND;
            return 0xffff;
        }

        /* Don't even try to do a direct cast because the value may be on an odd address. */
        mySource = (unsigned char *) args->source;
        myUChar = (mySource[0])
                | (mySource[1] << 8)
                | (mySource[2] << 16)
                | (mySource[3] << 24);

        args->source = (const char *)(mySource + 4);
        if (myUChar <= MAXIMUM_UTF && myUChar >= 0) {
            return myUChar;
        }

        uprv_memcpy(args->converter->invalidCharBuffer, mySource, 4);
        args->converter->invalidCharLength = 4;

        myUCharPtr = myUCharBuf;
        *err = U_ILLEGAL_CHAR_FOUND;
        args->target = myUCharPtr;
        args->targetLimit = myUCharBuf + 2;
        args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                        args,
                                        (const char *)mySource,
                                        4,
                                        UCNV_ILLEGAL,
                                        err);

        if(U_SUCCESS(*err)) {
            length = (uint16_t)(args->target - myUCharBuf);
            if(length > 0) {
                return ucnv_getUChar32KeepOverflow(args->converter, myUCharBuf, length);
            }
            /* else (callback did not write anything) continue */
        } else if(*err == U_BUFFER_OVERFLOW_ERROR) {
            *err = U_ZERO_ERROR;
            return ucnv_getUChar32KeepOverflow(args->converter, myUCharBuf, 2);
        } else {
            /* break on error */
            /* ### what if a callback set an error but _also_ generated output?! */
            return 0xffff;
        }
    }

    /* no input or only skipping callbacks */
    *err = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}

static const UConverterImpl _UTF32LEImpl = {
    UCNV_UTF32_LittleEndian,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_UTF32_LE,
    T_UConverter_toUnicode_UTF32_LE_OFFSET_LOGIC,
    T_UConverter_fromUnicode_UTF32_LE,
    T_UConverter_fromUnicode_UTF32_LE_OFFSET_LOGIC,
    T_UConverter_getNextUChar_UTF32_LE,

    NULL,
    NULL,
    NULL,
    NULL,
    ucnv_getCompleteUnicodeSet
};

/* The 1232 CCSID refers to any version of Unicode with any endianess of UTF-32 */
static const UConverterStaticData _UTF32LEStaticData = {
    sizeof(UConverterStaticData),
    "UTF-32LE",
    1234,
    UCNV_IBM, UCNV_UTF32_LittleEndian, 4, 4,
    { 0xfd, 0xff, 0, 0 }, 4, FALSE, FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _UTF32LEData = {
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF32LEStaticData, FALSE, &_UTF32LEImpl, 
    0
};

/* UTF-32 (Detect BOM) ------------------------------------------------------ */

/*
 * Detect a BOM at the beginning of the stream and select UTF-32BE or UTF-32LE
 * accordingly.
 *
 * State values:
 * 0    initial state
 * 1    saw 00
 * 2    saw 00 00
 * 3    saw 00 00 FE
 * 4    -
 * 5    saw FF
 * 6    saw FF FE
 * 7    saw FF FE 00
 * 8    UTF-32BE mode
 * 9    UTF-32LE mode
 *
 * During detection: state&3==number of matching bytes so far.
 *
 * On output, emit U+FEFF as the first code point.
 */

static void
_UTF32Reset(UConverter *cnv, UConverterResetChoice choice) {
    if(choice<=UCNV_RESET_TO_UNICODE) {
        /* reset toUnicode: state=0 */
        cnv->mode=0;
    }
    if(choice!=UCNV_RESET_TO_UNICODE) {
        /* reset fromUnicode: prepare to output the UTF-32PE BOM */
        cnv->charErrorBufferLength=4;
#if U_IS_BIG_ENDIAN
        cnv->charErrorBuffer[0]=0;
        cnv->charErrorBuffer[1]=0;
        cnv->charErrorBuffer[2]=0xfe;
        cnv->charErrorBuffer[3]=0xff;
#else
        cnv->charErrorBuffer[0]=0xff;
        cnv->charErrorBuffer[1]=0xfe;
        cnv->charErrorBuffer[2]=0;
        cnv->charErrorBuffer[3]=0;
#endif
    }
}

static void
_UTF32Open(UConverter *cnv,
           const char *name,
           const char *locale,
           uint32_t options,
           UErrorCode *pErrorCode) {
    _UTF32Reset(cnv, UCNV_RESET_BOTH);
}

static const char utf32BOM[8]={ 0, 0, (char)0xfe, (char)0xff,    (char)0xff, (char)0xfe, 0, 0 };

static void
_UTF32ToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
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
            if(b==0) {
                state=1; /* could be 00 00 FE FF */
            } else if(b==(char)0xff) {
                state=5; /* could be FF FE 00 00 */
            } else {
                state=8; /* default to UTF-32BE */
                continue;
            }
            ++source;
            break;
        case 1:
        case 2:
        case 3:
        case 5:
        case 6:
        case 7:
            if(*source==utf32BOM[state]) {
                ++state;
                ++source;
                if(state==4) {
                    state=8; /* detect UTF-32BE */
                    offsetDelta=source-pArgs->source;
                } else if(state==8) {
                    state=9; /* detect UTF-32LE */
                    offsetDelta=source-pArgs->source;
                }
            } else {
                /* switch to UTF-32BE and pass the previous bytes */
                int32_t count=source-pArgs->source; /* number of bytes from this buffer */

                /* reset the source */
                source=pArgs->source;

                if(count==(state&3)) {
                    /* simple: all in the same buffer, just reset source */
                } else {
                    UBool oldFlush=pArgs->flush;

                    /* some of the bytes are from a previous buffer, replay those first */
                    pArgs->source=utf32BOM+(state&4); /* select the correct BOM */
                    pArgs->sourceLimit=pArgs->source+((state&3)-count); /* replay previous bytes */
                    pArgs->flush=FALSE; /* this sourceLimit is not the real source stream limit */

                    /* no offsets: bytes from previous buffer, and not enough for output */
                    T_UConverter_toUnicode_UTF32_BE(pArgs, pErrorCode);

                    /* restore real pointers; pArgs->source will be set in case 8/9 */
                    pArgs->sourceLimit=sourceLimit;
                    pArgs->flush=oldFlush;
                }
                state=8;
                continue;
            }
            break;
        case 8:
            /* call UTF-32BE */
            pArgs->source=source;
            if(offsets==NULL) {
                T_UConverter_toUnicode_UTF32_BE(pArgs, pErrorCode);
            } else {
                T_UConverter_toUnicode_UTF32_BE_OFFSET_LOGIC(pArgs, pErrorCode);
            }
            source=pArgs->source;
            break;
        case 9:
            /* call UTF-32LE */
            pArgs->source=source;
            if(offsets==NULL) {
                T_UConverter_toUnicode_UTF32_LE(pArgs, pErrorCode);
            } else {
                T_UConverter_toUnicode_UTF32_LE_OFFSET_LOGIC(pArgs, pErrorCode);
            }
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
            T_UConverter_toUnicode_UTF32_BE(pArgs, pErrorCode);
            break;
        case 9:
            T_UConverter_toUnicode_UTF32_LE(pArgs, pErrorCode);
            break;
        default:
            /* handle 0<state<8: call UTF-32BE with too-short input */
            pArgs->source=utf32BOM+(state&4); /* select the correct BOM */
            pArgs->sourceLimit=pArgs->source+(state&3); /* replay bytes */

            /* no offsets: not enough for output */
            T_UConverter_toUnicode_UTF32_BE(pArgs, pErrorCode);
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
_UTF32GetNextUChar(UConverterToUnicodeArgs *pArgs,
                   UErrorCode *pErrorCode) {
    switch(pArgs->converter->mode) {
    case 8:
        return T_UConverter_getNextUChar_UTF32_BE(pArgs, pErrorCode);
    case 9:
        return T_UConverter_getNextUChar_UTF32_LE(pArgs, pErrorCode);
    default:
        return ucnv_getNextUCharFromToUImpl(pArgs, _UTF32ToUnicodeWithOffsets, FALSE, pErrorCode);
    }
}

static const UConverterImpl _UTF32Impl = {
    UCNV_UTF32,

    NULL,
    NULL,

    _UTF32Open,
    NULL,
    _UTF32Reset,

    _UTF32ToUnicodeWithOffsets,
    _UTF32ToUnicodeWithOffsets,
#if U_IS_BIG_ENDIAN
    T_UConverter_fromUnicode_UTF32_BE,
    T_UConverter_fromUnicode_UTF32_BE_OFFSET_LOGIC,
#else
    T_UConverter_fromUnicode_UTF32_LE,
    T_UConverter_fromUnicode_UTF32_LE_OFFSET_LOGIC,
#endif
    _UTF32GetNextUChar,

    NULL, /* ### TODO implement getStarters for all Unicode encodings?! */
    NULL,
    NULL,
    NULL,
    ucnv_getCompleteUnicodeSet
};

static const UConverterStaticData _UTF32StaticData = {
    sizeof(UConverterStaticData),
    "UTF-32",
    0, /* ### TODO review correctness of all Unicode CCSIDs */
    UCNV_IBM, UCNV_UTF32, 4, 4,
#if U_IS_BIG_ENDIAN
    { 0, 0, 0xff, 0xfd }, 4,
#else
    { 0xfd, 0xff, 0, 0 }, 4,
#endif
    FALSE, FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};

const UConverterSharedData _UTF32Data = {
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF32StaticData, FALSE, &_UTF32Impl, 
    0
};
