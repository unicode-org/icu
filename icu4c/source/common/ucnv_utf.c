/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv_utf.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000feb03
*   created by: Markus W. Scherer
*
*   Change history:
*
*   06/29/2000  helena      Major rewrite of the callback APIs.
*   07/20/2000  george      Change the coding style to conform to the coding guidelines,
*                           and a few miscellaneous bug fixes.
*   11/15/2000  george      Added UTF-32
*/

#include "cmemory.h"
#include "unicode/utypes.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"

/* UTF-8 -------------------------------------------------------------------- */

/* UTF-8 Conversion DATA
 *   for more information see Unicode Strandard 2.0 , Transformation Formats Appendix A-9
 */
/*static const uint32_t REPLACEMENT_CHARACTER = 0x0000FFFD;*/
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

static const uint32_t offsetsFromUTF8[7] = {0,
  (uint32_t) 0x00000000, (uint32_t) 0x00003080, (uint32_t) 0x000E2080,
  (uint32_t) 0x03C82080, (uint32_t) 0xFA082080, (uint32_t) 0x82082080
};

/* END OF UTF-8 Conversion DATA */

static const int8_t bytesFromUTF8[256] = {
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
  3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
};

/* static const unsigned char firstByteMark[7] = {0x00, 0x00, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC};*/

#define INVALID_UTF8_TAIL(utf8) (((utf8) & 0xC0) != 0x80)

/**
 * Calls invalid char callback when an invalid character sequence is encountered.
 * It presumes that the converter has a callback to call.
 *
 * @returns true when callback fails
 */
static UBool
T_UConverter_toUnicode_InvalidChar_Callback(UConverterToUnicodeArgs * args,
                                                  UErrorCode *err)
{
    UConverter *converter = args->converter;

    if (U_SUCCESS(*err))
    {
        *err = U_ILLEGAL_CHAR_FOUND;
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
                                            UCNV_ILLEGAL,
                                            err);

    return (UBool)U_FAILURE(*err);
}

static UBool
T_UConverter_toUnicode_InvalidChar_OffsetCallback(UConverterToUnicodeArgs * args,
                                                        int32_t currentOffset,
                                                        UErrorCode *err)
{
    int32_t *saveOffsets = args->offsets;
    UBool result;
    
    result = T_UConverter_toUnicode_InvalidChar_Callback(args, err);

    while (saveOffsets < args->offsets)
    {
        *(saveOffsets++) = currentOffset;
    }
    return result;
}

U_CFUNC void T_UConverter_toUnicode_UTF8 (UConverterToUnicodeArgs * args,
                                  UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    const unsigned char *sourceLimit = (unsigned char *) args->sourceLimit;
    const UChar *targetLimit = args->targetLimit;
    unsigned char *toUBytes = args->converter->toUBytes;
    UBool invalidTailChar = FALSE;
    uint32_t ch, ch2 = 0, i;
    uint32_t inBytes;  /* Total number of bytes in the current UTF8 sequence */
  
    /* Restore size of current sequence */
    if (args->converter->toUnicodeStatus && myTarget < targetLimit)
    {
        inBytes = args->converter->toULength;       /* restore # of bytes to consume */
        i = args->converter->invalidCharLength;     /* restore # of bytes consumed */

        ch = args->converter->toUnicodeStatus;/*Stores the previously calculated ch from a previous call*/
        args->converter->toUnicodeStatus = 0;
        goto morebytes;
    }


    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        ch = *(mySource++);
        if (ch < 0x80)        /* Simple case */
        {
            *(myTarget++) = (UChar) ch;
        }
        else
        {
            /* store the first char */
            toUBytes[0] = (char)ch;
            inBytes = bytesFromUTF8[ch]; /* lookup current sequence length */
            i = 1;

morebytes:
            while (i < inBytes)
            {
                if (mySource < sourceLimit)
                {
                    toUBytes[i] = (char) (ch2 = *(mySource++));
                    if (INVALID_UTF8_TAIL(ch2))
                    {
                        *err = U_TRUNCATED_CHAR_FOUND;
                        invalidTailChar = TRUE;
                        break;
                    }
                    ch = (ch << 6) + ch2;
                    i++;
                }
                else
                {
                    if (args->flush)
                    {
                        if (U_SUCCESS(*err))
                        {
                            *err = U_TRUNCATED_CHAR_FOUND;
                        }
                    }
                    else
                    {    /* stores a partially calculated target*/
                        args->converter->toUnicodeStatus = ch;
                        args->converter->toULength = (int8_t) inBytes;
                        args->converter->invalidCharLength = (int8_t) i;
                    }
                    goto donefornow;
                }
            }

            /* Remove the acummulated high bits */
            ch -= offsetsFromUTF8[inBytes];

            if (i == inBytes && ch <= MAXIMUM_UTF)
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
                if (T_UConverter_toUnicode_InvalidChar_Callback(args, err))
                {
                    /* Stop if the error wasn't handled */
                    break;
                }
                args->converter->invalidCharLength = 0;
                mySource = (unsigned char *) args->source;
                myTarget = args->target;
                if (invalidTailChar)
                {
                    /* Treat the tail as ASCII*/
                    if (myTarget < targetLimit)
                    {
                        *(myTarget++) = (UChar) ch2;
                        invalidTailChar = FALSE;
                    }
                    else
                    {
                        /* Put in overflow buffer (not handled here) */
                        args->converter->UCharErrorBuffer[0] = (UChar) ch2;
                        args->converter->UCharErrorBufferLength = 1;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                }
            }
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

U_CFUNC void T_UConverter_toUnicode_UTF8_OFFSETS_LOGIC (UConverterToUnicodeArgs * args,
                                                UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    int32_t *myOffsets = args->offsets;
    int32_t offsetNum = 0;
    const unsigned char *sourceLimit = (unsigned char *) args->sourceLimit;
    const UChar *targetLimit = args->targetLimit;
    unsigned char *toUBytes = args->converter->toUBytes;
    UBool invalidTailChar = FALSE;
    uint32_t ch, ch2 = 0, i;
    uint32_t inBytes;

    /* Restore size of current sequence */
    if (args->converter->toUnicodeStatus && myTarget < targetLimit)
    {
        inBytes = args->converter->toULength;       /* restore # of bytes to consume */
        i = args->converter->invalidCharLength;     /* restore # of bytes consumed */

        ch = args->converter->toUnicodeStatus;/*Stores the previously calculated ch from a previous call*/
        args->converter->toUnicodeStatus = 0;
        goto morebytes;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        ch = *(mySource++);
        if (ch < 0x80)        /* Simple case */
        {
            *(myTarget++) = (UChar) ch;
            *(myOffsets++) = offsetNum++;
        }
        else
        {
            toUBytes[0] = (char)ch;
            inBytes = bytesFromUTF8[ch];
            i = 1;

morebytes:
            while (i < inBytes)
            {
                if (mySource < sourceLimit)
                {
                    toUBytes[i] = (char) (ch2 = *(mySource++));
                    if (INVALID_UTF8_TAIL(ch2))
                    {
                        *err = U_TRUNCATED_CHAR_FOUND;
                        invalidTailChar = TRUE;
                        break;
                    }
                    ch = (ch << 6) + ch2;
                    i++;
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
                    {
                        args->converter->toUnicodeStatus = ch;
                        args->converter->toULength = (int8_t)inBytes;
                        args->converter->invalidCharLength = (int8_t)i;
                    }
                    goto donefornow;
                }
            }

            /* Remove the acummulated high bits */
            ch -= offsetsFromUTF8[inBytes];

            if (i == inBytes && ch <= MAXIMUM_UTF)
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
                    *(myOffsets++) = offsetNum;
                    ch -= HALF_BASE;
                    *(myTarget++) = (UChar) ((ch >> HALF_SHIFT) + SURROGATE_HIGH_START);
                    ch = (ch & HALF_MASK) + SURROGATE_LOW_START;
                    if (myTarget < targetLimit)
                    {
                        *(myTarget++) = (UChar)ch;
                        *(myOffsets++) = offsetNum;
                    }
                    else
                    {
                        args->converter->UCharErrorBuffer[0] = (UChar) ch;
                        args->converter->UCharErrorBufferLength = 1;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                    }
                }
                offsetNum += i;
            }
            else
            {
                UBool useOffset;

                args->source = (const char *) mySource;
                args->target = myTarget;
                args->offsets = myOffsets;
                args->converter->invalidCharLength = (int8_t)i;
                if (T_UConverter_toUnicode_InvalidChar_OffsetCallback(args,
                 offsetNum, err))
                {
                    /* Stop if the error wasn't handled */
                    break;
                }

                args->converter->invalidCharLength = 0;
                mySource = (unsigned char *) args->source;
                myTarget = args->target;

                useOffset = (UBool)(myOffsets != args->offsets);
                myOffsets = args->offsets;
                offsetNum += i;

                if (invalidTailChar)
                {
                    /* Treat the tail as ASCII*/
                    if (myTarget < targetLimit)
                    {
                        *(myTarget++) = (UChar) ch2;
                        *myOffsets = offsetNum++;
                        if (useOffset)
                        {
                            /* Increment when the target was consumed */
                            myOffsets++;
                        }
                        invalidTailChar = FALSE;
                    }
                    else
                    {
                        /* Put in overflow buffer (not handled here) */
                        args->converter->UCharErrorBuffer[0] = (UChar) ch2;
                        args->converter->UCharErrorBufferLength = 1;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                        break;
                    }
                }
            }
        }
    }

donefornow:
    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {   /* End of target buffer */
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = myTarget;
    args->source = (const char *) mySource;
    args->offsets = myOffsets;
}

U_CFUNC void T_UConverter_fromUnicode_UTF8 (UConverterFromUnicodeArgs * args,
                                    UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    const UChar *sourceLimit = args->sourceLimit;
    const unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    uint32_t ch, ch2;
    int16_t indexToWrite;
    char temp[4];

    if (args->converter->fromUnicodeStatus && myTarget < targetLimit)
    {
        ch = args->converter->fromUnicodeStatus;
        args->converter->fromUnicodeStatus = 0;
        goto lowsurogate;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        ch = *(mySource++);

        if (ch < 0x80)        /* Single byte */
        {
            *(myTarget++) = (char) ch;
        }
        else if (ch < 0x800)  /* Double byte */
        {
            *(myTarget++) = (char) ((ch >> 6) | 0xc0);
            if (myTarget < targetLimit)
            {
                *(myTarget++) = (char) ((ch & 0x3f) | 0x80);
            }
            else
            {
                args->converter->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                args->converter->charErrorBufferLength = 1;
                *err = U_BUFFER_OVERFLOW_ERROR;
            }
        }
        else
        /* Check for surogates */
        {
            if ((ch >= SURROGATE_HIGH_START) && (ch <= SURROGATE_HIGH_END))
            {
lowsurogate:
                if (mySource < sourceLimit)
                {
                    ch2 = *mySource;
                    if ((ch2 >= SURROGATE_LOW_START) && (ch2 <= SURROGATE_LOW_END))
                    {
                        /* If there were two surrogates, combine them otherwise treat them normally */
                        ch = ((ch - SURROGATE_HIGH_START) << HALF_SHIFT) + ch2 + SURROGATE_LOW_BASE;
                        mySource++;
                    }
                }
                else if (!args->flush)
                {
                    args->converter->fromUnicodeStatus = ch;
                    break;
                }
            }

            if (ch < 0x10000)
            {
                indexToWrite = 2;
                temp[2] = (char) ((ch >> 12) | 0xe0);
            }
            else
            {
                indexToWrite = 3;
                temp[3] = (char) ((ch >> 18) | 0xf0);
                temp[2] = (char) (((ch >> 12) & 0x3f) | 0x80);
            }
            temp[1] = (char) (((ch >> 6) & 0x3f) | 0x80);
            temp[0] = (char) ((ch & 0x3f) | 0x80);

            for (; indexToWrite >= 0; indexToWrite--)
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
    }

    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = (char *) myTarget;
    args->source = mySource;
}

U_CFUNC void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                  UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t *myOffsets = args->offsets;
    const UChar *sourceLimit = args->sourceLimit;
    const unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    uint32_t ch, ch2;
    int32_t offsetNum = 0;
    int16_t indexToWrite;
    char temp[4];

    if (args->converter->fromUnicodeStatus && myTarget < targetLimit)
    {
        ch = args->converter->fromUnicodeStatus;
        args->converter->fromUnicodeStatus = 0;
        goto lowsurogate;
    }

    while (mySource < sourceLimit && myTarget < targetLimit)
    {
        ch = *(mySource++);

        if (ch < 0x80)        /* Single byte */
        {
            *(myOffsets++) = offsetNum++;
            *(myTarget++) = (char) ch;
        }
        else if (ch < 0x800)  /* Double byte */
        {
            *(myOffsets++) = offsetNum;
            *(myTarget++) = (char) ((ch >> 6) | 0xc0);
            if (myTarget < targetLimit)
            {
                *(myOffsets++) = offsetNum++;
                *(myTarget++) = (char) ((ch & 0x3f) | 0x80);
            }
            else
            {
                args->converter->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                args->converter->charErrorBufferLength = 1;
                *err = U_BUFFER_OVERFLOW_ERROR;
            }
        }
        else
        /* Check for surogates */
        {
            if ((ch >= SURROGATE_HIGH_START) && (ch <= SURROGATE_HIGH_END))
            {
lowsurogate:
                if (mySource < sourceLimit)
                {
                    ch2 = *mySource;
                    if ((ch2 >= SURROGATE_LOW_START) && (ch2 <= SURROGATE_LOW_END))
                    {
                        /* If there were two surrogates, combine them otherwise treat them normally */
                        ch = ((ch - SURROGATE_HIGH_START) << HALF_SHIFT) + ch2 + SURROGATE_LOW_BASE;
                        mySource++;
                    }
                }
                else if (!args->flush)
                {
                    args->converter->fromUnicodeStatus = ch;
                    break;
                }
            }

            if (ch < 0x10000)
            {
                indexToWrite = 2;
                temp[2] = (char) ((ch >> 12) | 0xe0);
            }
            else
            {
                indexToWrite = 3;
                temp[3] = (char) ((ch >> 18) | 0xf0);
                temp[2] = (char) (((ch >> 12) & 0x3f) | 0x80);
            }
            temp[1] = (char) (((ch >> 6) & 0x3f) | 0x80);
            temp[0] = (char) ((ch & 0x3f) | 0x80);

            for (; indexToWrite >= 0; indexToWrite--)
            {
                if (myTarget < targetLimit)
                {
                    *(myOffsets++) = offsetNum;
                    *(myTarget++) = temp[indexToWrite];
                }
                else
                {
                    args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = temp[indexToWrite];
                    *err = U_BUFFER_OVERFLOW_ERROR;
                }
            }
            offsetNum += (ch >= 0x10000) + 1;
        }
    }

    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }

    args->target = (char *) myTarget;
    args->source = mySource;
}

U_CFUNC UChar32 T_UConverter_getNextUChar_UTF8(UConverterToUnicodeArgs *args,
                                               UErrorCode *err) {
    UChar buffer[2];
    char const *sourceInitial;
    UChar* myUCharPtr;
    uint16_t extraBytesToWrite;
    uint8_t myByte;
    UChar32 ch;
    int8_t isLegalSequence;

    while (args->source < args->sourceLimit)
    {
        sourceInitial = args->source;
        myByte = (uint8_t)*(args->source++);
        if (myByte < 0x80)
        {
            return (UChar32)myByte;
        }

        extraBytesToWrite = (uint16_t)bytesFromUTF8[myByte];
        if (extraBytesToWrite == 0) {
            goto CALL_ERROR_FUNCTION;
        }

        /*The byte sequence is longer than the buffer area passed*/
        if ((args->source + extraBytesToWrite - 1) > args->sourceLimit)
        {
            *err = U_TRUNCATED_CHAR_FOUND;
            return 0xffff;
        }
        else
        {
            isLegalSequence = 1;
            ch = myByte << 6;
            switch(extraBytesToWrite)
            {     
              /* note: code falls through cases! (sic)*/ 
            case 6:
                ch += (myByte = (uint8_t)*(args->source++));
                ch <<= 6;
                if ((myByte & 0xC0) != 0x80) 
                {
                    isLegalSequence = 0;
                    break;
                }
            case 5:
                ch += (myByte = *(args->source++));
                ch <<= 6;
                if ((myByte & 0xC0) != 0x80) 
                {
                    isLegalSequence = 0;
                    break;
                }
            case 4:
                ch += (myByte = *(args->source++));
                ch <<= 6;
                if ((myByte & 0xC0) != 0x80) 
                {
                    isLegalSequence = 0;
                    break;
                }
            case 3:
                ch += (myByte = *(args->source++));
                ch <<= 6;
                if ((myByte & 0xC0) != 0x80) 
                {
                    isLegalSequence = 0;
                    break;
                }
            case 2:
                ch += (myByte = *(args->source++));
                if ((myByte & 0xC0) != 0x80) 
                {
                    isLegalSequence = 0;
                }
            };
        }
        ch -= offsetsFromUTF8[extraBytesToWrite];

        if (isLegalSequence && extraBytesToWrite <= 4 && ch <= 0x10ffff) {
            return ch; /* return the code point */
        }

CALL_ERROR_FUNCTION:
        extraBytesToWrite = args->source - sourceInitial;
        args->converter->invalidCharLength = (uint8_t)extraBytesToWrite;
        uprv_memcpy(args->converter->invalidCharBuffer, sourceInitial, extraBytesToWrite);

        myUCharPtr = buffer;
        *err = U_ILLEGAL_CHAR_FOUND;
        args->target = myUCharPtr;
        args->targetLimit = buffer + 2;
        args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                        args,
                                        sourceInitial,
                                        extraBytesToWrite,
                                        UCNV_ILLEGAL,
                                        err);

        if(U_SUCCESS(*err)) {
            extraBytesToWrite = (uint16_t)(args->target - buffer);
            if(extraBytesToWrite > 0) {
                return ucnv_getUChar32KeepOverflow(args->converter, buffer, extraBytesToWrite);
            }
            /* else (callback did not write anything) continue */
        } else if(*err == U_BUFFER_OVERFLOW_ERROR) {
            *err = U_ZERO_ERROR;
            return ucnv_getUChar32KeepOverflow(args->converter, buffer, 2);
        } else {
            /* break on error */
            /* ### what if a callback set an error but _also_ generated output?! */
            return 0xffff;
        }
    }

    /* no input or only skipping callback calls */
    *err = U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
} 

static const UConverterImpl _UTF8Impl={
    UCNV_UTF8,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_UTF8,
    T_UConverter_toUnicode_UTF8_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_UTF8,
    T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_UTF8,

    NULL,
    NULL
};

/* Todo: verify that UTF-8 == (ccsid (ibm-codepage) 1208) for unicode version 2.0 and 3.0 */
const UConverterStaticData _UTF8StaticData={
    sizeof(UConverterStaticData),
    "UTF8",
    1208, UCNV_IBM, UCNV_UTF8, 1, 4,
    { 0xef, 0xbf, 0xbd, 0 },3,FALSE,FALSE,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _UTF8Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF8StaticData, FALSE, &_UTF8Impl, 
    0
};

/* UTF-16 Platform Endian --------------------------------------------------- */

U_CFUNC void
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

    if(length <= 0) {
        /* no input, nothing to do */
        return;
    }

    if(targetCapacity <= 0) {
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        return;
    }

    /* complete a partial UChar from the last call */
    if(cnv->toUnicodeStatus != 0) {
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
    /* } else length==0 { nothing to do */
    }

    /* write back the updated pointers */
    pArgs->source = (const char *)source;
    pArgs->target = target;
    pArgs->offsets = offsets;
}

U_CFUNC void
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

U_CFUNC void
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

    if(length <= 0) {
        /* no input, nothing to do */
        return;
    }

    if(targetCapacity <= 0) {
        *pErrorCode = U_BUFFER_OVERFLOW_ERROR;
        return;
    }

    /* complete a partial UChar from the last call */
    if(cnv->toUnicodeStatus != 0) {
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
    /* } else length==0 { nothing to do */
    }

    /* write back the updated pointers */
    pArgs->source = (const char *)source;
    pArgs->target = target;
    pArgs->offsets = offsets;
}

U_CFUNC void
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

U_CFUNC UChar32 T_UConverter_getNextUChar_UTF16_BE(UConverterToUnicodeArgs* args,
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

#if U_IS_BIG_ENDIAN
    _UTF16PEToUnicodeWithOffsets,
    _UTF16PEToUnicodeWithOffsets,
    _UTF16PEFromUnicodeWithOffsets,
    _UTF16PEFromUnicodeWithOffsets,
#else
    _UTF16OEToUnicodeWithOffsets,
    _UTF16OEToUnicodeWithOffsets,
    _UTF16OEFromUnicodeWithOffsets,
    _UTF16OEFromUnicodeWithOffsets,
#endif
    T_UConverter_getNextUChar_UTF16_BE,

    NULL,
    NULL
};

/* Todo: verify that UTF-16BE == (ccsid (ibm-codepage) 1200) for unicode version 2.0 and 3.0 */
const UConverterStaticData _UTF16BEStaticData={
    sizeof(UConverterStaticData),
    "UTF16_BigEndian",
    1200, UCNV_IBM, UCNV_UTF16_BigEndian, 2, 2,
    { 0xff, 0xfd, 0, 0 },2,FALSE,FALSE,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _UTF16BEData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16BEStaticData, FALSE, &_UTF16BEImpl, 
    0
};

/* UTF-16LE ----------------------------------------------------------------- */

U_CFUNC UChar32 T_UConverter_getNextUChar_UTF16_LE(UConverterToUnicodeArgs* args,
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

#if !U_IS_BIG_ENDIAN
    _UTF16PEToUnicodeWithOffsets,
    _UTF16PEToUnicodeWithOffsets,
    _UTF16PEFromUnicodeWithOffsets,
    _UTF16PEFromUnicodeWithOffsets,
#else
    _UTF16OEToUnicodeWithOffsets,
    _UTF16OEToUnicodeWithOffsets,
    _UTF16OEFromUnicodeWithOffsets,
    _UTF16OEFromUnicodeWithOffsets,
#endif
    T_UConverter_getNextUChar_UTF16_LE,

    NULL,
    NULL
};


/* Todo: verify that UTF-16LE == (ccsid (ibm-codepage) 1200) for unicode version 2.0 and 3.0 */
const UConverterStaticData _UTF16LEStaticData={
    sizeof(UConverterStaticData),
    "UTF16_LittleEndian",
    1200, UCNV_IBM, UCNV_UTF16_LittleEndian, 2, 2,
    { 0xfd, 0xff, 0, 0 },2,FALSE,FALSE,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _UTF16LEData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16LEStaticData, FALSE, &_UTF16LEImpl, 
    0
};

/* UTF-32BE ----------------------------------------------------------------- */

void T_UConverter_toUnicode_UTF32_BE(UConverterToUnicodeArgs * args,
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
            if (T_UConverter_toUnicode_InvalidChar_Callback(args, err))
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

void T_UConverter_toUnicode_UTF32_BE_OFFSET_LOGIC(UConverterToUnicodeArgs * args,
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
            if (T_UConverter_toUnicode_InvalidChar_OffsetCallback(args, offsetNum, err))
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
}

void T_UConverter_fromUnicode_UTF32_BE(UConverterFromUnicodeArgs * args,
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

void T_UConverter_fromUnicode_UTF32_BE_OFFSET_LOGIC(UConverterFromUnicodeArgs * args,
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
}

UChar32 T_UConverter_getNextUChar_UTF32_BE(UConverterToUnicodeArgs* args,
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
        if (myUChar <= MAXIMUM_UTF) {
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
    NULL
};

const UConverterStaticData _UTF32BEStaticData = {
    sizeof(UConverterStaticData),
    "UTF32_BigEndian",
    0,  /* TODO: Change this number to the UTF-32 CCSID which currently does not exist */
    UCNV_IBM, UCNV_UTF32_BigEndian, 4, 4,
    { 0, 0, 0xff, 0xfd }, 4, FALSE, FALSE,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};

const UConverterSharedData _UTF32BEData = {
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF32BEStaticData, FALSE, &_UTF32BEImpl, 
    0
};

/* UTF-32LE ---------------------------------------------------------- */

void T_UConverter_toUnicode_UTF32_LE(UConverterToUnicodeArgs * args,
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
            if (T_UConverter_toUnicode_InvalidChar_Callback(args, err))
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

void T_UConverter_toUnicode_UTF32_LE_OFFSET_LOGIC(UConverterToUnicodeArgs * args,
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
            if (T_UConverter_toUnicode_InvalidChar_OffsetCallback(args, offsetNum, err))
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
}

void  T_UConverter_fromUnicode_UTF32_LE(UConverterFromUnicodeArgs * args,
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

void  T_UConverter_fromUnicode_UTF32_LE_OFFSET_LOGIC(UConverterFromUnicodeArgs * args,
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
}

UChar32 T_UConverter_getNextUChar_UTF32_LE(UConverterToUnicodeArgs* args,
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
        if (myUChar <= MAXIMUM_UTF) {
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
    NULL
};

const UConverterStaticData _UTF32LEStaticData = {
    sizeof(UConverterStaticData),
    "UTF32_LittleEndian",
    0,    /* TODO: Change this number to the UTF-32 CCSID which currently does not exist */
    UCNV_IBM, UCNV_UTF32_BigEndian, 4, 4,
    { 0xfd, 0xff, 0, 0 }, 4, FALSE, FALSE,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _UTF32LEData = {
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF32LEStaticData, FALSE, &_UTF32LEImpl, 
    0
};

/* UTF-7 -------------------------------------------------------------------- */

/* ### TODO: in the and user guide, document version option (=1 for escaping set O characters) */
/*
 * UTF-7 is a stateful encoding of Unicode, somewhat like UTF7.
 * It is defined in RFC 2152  http://www.imc.org/rfc2152 .
 * It was intended for use in Internet email systems, using in its bytewise
 * encoding only a subset of 7-bit US-ASCII.
 * UTF-7 is deprecated in favor of UTF-8/16/32 and UTF7, but still
 * occasionally used.
 *
 * For converting Unicode to UTF-7, the RFC allows to encode some US-ASCII
 * characters directly or in base64. Especially, the characters in set O
 * as defined in the RFC (see below) may be encoded directly but are not
 * allowed in, e.g., email headers.
 * By default, the ICU UTF-7 converter encodes set O directly.
 * By choosing the option "version=1", set O will be escaped instead.
 * For example:
 *     utf7Converter=ucnv_open("UTF-7,version=1");
 */

/*
 * Tests for US-ASCII characters belonging to character classes
 * defined in UTF-7.
 *
 * Set D (directly encoded characters) consists of the following
 * characters: the upper and lower case letters A through Z
 * and a through z, the 10 digits 0-9, and the following nine special
 * characters (note that "+" and "=" are omitted):
 *     '(),-./:?
 *
 * Set O (optional direct characters) consists of the following
 * characters (note that "\" and "~" are omitted):
 *     !"#$%&*;<=>@[]^_`{|}
 *
 * According to the rules in RFC 2152, the byte values for the following
 * US-ASCII characters are not used in UTF-7 and are therefore illegal:
 * - all C0 control codes except for CR LF TAB
 * - BACKSLASH
 * - TILDE
 * - DEL
 * - all codes beyond US-ASCII, i.e. all >127
 */
#define inSetD(c) \
    ((uint8_t)((c)-97)<26 || (uint8_t)((c)-65)<26 || /* letters */ \
     (uint8_t)((c)-48)<10 ||    /* digits */ \
     (uint8_t)((c)-39)<3 ||     /* '() */ \
     (uint8_t)((c)-44)<4 ||     /* ,-./ */ \
     (c)==58 || (c)==63         /* :? */ \
    )

#define inSetO(c) \
    ((uint8_t)((c)-33)<6 ||         /* !"#$%& */ \
     (uint8_t)((c)-59)<4 ||         /* ;<=> */ \
     (uint8_t)((c)-93)<4 ||         /* ]^_` */ \
     (uint8_t)((c)-123)<3 ||        /* {|} */ \
     (c)==42 || (c)==64 || (c)==91  /* *@[ */ \
    )

#define isCRLFTAB(c) ((c)==13 || (c)==10 || (c)==9)
#define isCRLFSPTAB(c) ((c)==32 || (c)==13 || (c)==10 || (c)==9)

#define PLUS  43
#define MINUS 45
#define BACKSLASH 92
#define TILDE 126

/* legal byte values: all US-ASCII graphic characters from space to before tilde, and CR LF TAB */
#define isLegalUTF7(c) (((uint8_t)((c)-32)<94 && (c)!=BACKSLASH) || isCRLFTAB(c))

/* encode directly sets D and O and CR LF SP TAB */
static const UBool encodeDirectlyMaximum[128]={
 /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f */
    0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,

    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1,

    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0
};

/* encode directly set D and CR LF SP TAB but not set O */
static const UBool encodeDirectlyRestricted[128]={
 /* 0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f */
    0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,

    1, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1,

    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0,

    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0
};

static const uint8_t
toBase64[64]={
    /* A-Z */
    65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77,
    78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
    /* a-z */
    97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
    110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122,
    /* 0-9 */
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57,
    /* +/ */
    43, 47
};

static const int8_t
fromBase64[128]={
    /* C0 controls, -1 for legal ones (CR LF TAB), -3 for illegal ones */
    -3, -3, -3, -3, -3, -3, -3, -3, -3, -1, -1, -3, -3, -1, -3, -3,
    -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3, -3,

    /* general punctuation with + and / and a special value (-2) for - */
    -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -2, -1, 63,
    /* digits */
    52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,

    /* A-Z */
    -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
    15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -3, -1, -1, -1,

    /* a-z */
    -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
    41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -3, -3
};

/*
 * converter status values:
 *
 * toUnicodeStatus:
 *     24 inDirectMode (boolean)
 * 23..16 base64Counter (-1..7)
 * 15..0  bits (up to 14 bits incoming base64)
 *
 * fromUnicodeStatus:
 * 31..28 version (0: set O direct  1: set O escaped)
 *     24 inDirectMode (boolean)
 * 23..16 base64Counter (0..2)
 *  7..0  bits (6 bits outgoing base64)
 *
 */

U_CFUNC void
_UTF7Reset(UConverter *cnv, UConverterResetChoice choice) {
    if(choice<=UCNV_RESET_TO_UNICODE) {
        /* reset toUnicode */
        cnv->toUnicodeStatus=0x1000000; /* inDirectMode=TRUE */
        cnv->toULength=0;
    }
    if(choice!=UCNV_RESET_TO_UNICODE) {
        /* reset fromUnicode */
        cnv->fromUnicodeStatus=(cnv->fromUnicodeStatus&0xf0000000)|0x1000000; /* keep version, inDirectMode=TRUE */
    }
}

U_CFUNC void
_UTF7Open(UConverter *cnv,
          const char *name,
          const char *locale,
          uint32_t options,
          UErrorCode *pErrorCode) {
    if((options&0xf)<=1) {
        cnv->fromUnicodeStatus=(options&0xf)<<28;
        _UTF7Reset(cnv, UCNV_RESET_BOTH);
    } else {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
    }
}

U_CFUNC void
_UTF7ToUnicodeWithOffsets(UConverterToUnicodeArgs *pArgs,
                          UErrorCode *pErrorCode) {
    UConverter *cnv;
    const uint8_t *source, *sourceLimit;
    UChar *target;
    const UChar *targetLimit;
    int32_t *offsets;

    uint8_t *bytes;
    uint8_t byteIndex;

    int32_t length, targetCapacity;

    /* UTF-7 state */
    uint16_t bits;
    int8_t base64Counter;
    UBool inDirectMode;

    int8_t base64Value;

    int32_t sourceIndex, nextSourceIndex;

    uint8_t b;

    /* set up the local pointers */
    cnv=pArgs->converter;

    source=(const uint8_t *)pArgs->source;
    sourceLimit=(const uint8_t *)pArgs->sourceLimit;
    target=pArgs->target;
    targetLimit=pArgs->targetLimit;
    offsets=pArgs->offsets;

    /* get the state machine state */
    {
        uint32_t status=cnv->toUnicodeStatus;
        inDirectMode=(UBool)((status>>24)&1);
        base64Counter=(int8_t)(status>>16);
        bits=(uint16_t)status;
    }
    bytes=cnv->toUBytes;
    byteIndex=cnv->toULength;

    /* sourceIndex=-1 if the current character began in the previous buffer */
    sourceIndex=byteIndex==0 ? 0 : -1;
    nextSourceIndex=0;

loop:
    if(inDirectMode) {
directMode:
        /*
         * In Direct Mode, most US-ASCII characters are encoded directly, i.e.,
         * with their US-ASCII byte values.
         * Backslash and Tilde and most control characters are not allowed in UTF-7.
         * A plus sign starts Unicode (or "escape") Mode.
         *
         * In Direct Mode, only the sourceIndex is used.
         */
        byteIndex=0;
        length=sourceLimit-source;
        targetCapacity=targetLimit-target;
        if(length>targetCapacity) {
            length=targetCapacity;
        }
        while(length>0) {
            b=*source++;
            if(!isLegalUTF7(b)) {
                /* illegal */
                bytes[0]=b;
                byteIndex=1;
                nextSourceIndex=sourceIndex+1;
                goto callback;
            } else if(b!=PLUS) {
                /* write directly encoded character */
                *target++=b;
                if(offsets!=NULL) {
                    *offsets++=sourceIndex++;
                }
            } else /* PLUS */ {
                /* switch to Unicode mode */
                nextSourceIndex=++sourceIndex;
                inDirectMode=FALSE;
                byteIndex=0;
                bits=0;
                base64Counter=-1;
                goto unicodeMode;
            }
            --length;
        }
        if(source<sourceLimit && target>=targetLimit) {
            /* target is full */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    } else {
unicodeMode:
        /*
         * In Unicode (or "escape") Mode, UTF-16BE is base64-encoded.
         * The base64 sequence ends with any character that is not in the base64 alphabet.
         * A terminating minus sign is consumed.
         *
         * In Unicode Mode, the sourceIndex has the index to the start of the current
         * base64 bytes, while nextSourceIndex is precisely parallel to source,
         * keeping the index to the following byte.
         * Note that in 2 out of 3 cases, UChars overlap within a base64 byte.
         */
        while(source<sourceLimit) {
            if(target<targetLimit) {
                bytes[byteIndex++]=b=*source++;
                ++nextSourceIndex;
                if(b>=126) {
                    /* illegal - test other illegal US-ASCII values by base64Value==-3 */
                    inDirectMode=TRUE;
                    goto callback;
                } else if((base64Value=fromBase64[b])>=0) {
                    /* collect base64 bytes into UChars */
                    switch(base64Counter) {
                    case -1: /* -1 is immediately after the + */
                    case 0:
                        bits=base64Value;
                        base64Counter=1;
                        break;
                    case 1:
                    case 3:
                    case 4:
                    case 6:
                        bits=(bits<<6)|base64Value;
                        ++base64Counter;
                        break;
                    case 2:
                        *target++=(bits<<4)|(base64Value>>2);
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                            sourceIndex=nextSourceIndex-1;
                        }
                        bytes[0]=b; /* keep this byte in case an error occurs */
                        byteIndex=1;
                        bits=base64Value&3;
                        base64Counter=3;
                        break;
                    case 5:
                        *target++=(bits<<2)|(base64Value>>4);
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                            sourceIndex=nextSourceIndex-1;
                        }
                        bytes[0]=b; /* keep this byte in case an error occurs */
                        byteIndex=1;
                        bits=base64Value&15;
                        base64Counter=6;
                        break;
                    case 7:
                        *target++=(bits<<6)|base64Value;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex;
                            sourceIndex=nextSourceIndex;
                        }
                        byteIndex=0;
                        bits=0;
                        base64Counter=0;
                        break;
                    default:
                        /* will never occur */
                        break;
                    }
                } else if(base64Value==-2) {
                    /* minus sign terminates the base64 sequence */
                    inDirectMode=TRUE;
                    if(base64Counter==-1) {
                        /* +- i.e. a minus immediately following a plus */
                        *target++=PLUS;
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex-1;
                        }
                    } else {
                        /* absorb the minus and leave the Unicode Mode */
                        if(bits!=0) {
                            /* bits are illegally left over, a UChar is incomplete */
                            goto callback;
                        }
                    }
                    sourceIndex=nextSourceIndex;
                    goto directMode;
                } else if(base64Value==-1) /* for any legal character except base64 and minus sign */ {
                    /* leave the Unicode Mode */
                    inDirectMode=TRUE;
                    if(base64Counter==-1) {
                        /* illegal: + immediately followed by something other than base64 or minus sign */
                        /* include the plus sign in the reported sequence */
                        --sourceIndex;
                        bytes[0]=PLUS;
                        bytes[1]=b;
                        byteIndex=2;
                        goto callback;
                    } else if(bits==0) {
                        /* un-read the character in case it is a plus sign */
                        --source;
                        sourceIndex=nextSourceIndex-1;
                        goto directMode;
                    } else {
                        /* bits are illegally left over, a UChar is incomplete */
                        goto callback;
                    }
                } else /* base64Value==-3 for illegal characters */ {
                    /* illegal */
                    inDirectMode=TRUE;
                    goto callback;
                }
            } else {
                /* target is full */
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                break;
            }
        }
    }
endloop:

    if(pArgs->flush && source>=sourceLimit) {
        /* reset the state for the next conversion */
        if(!inDirectMode && bits!=0 && U_SUCCESS(*pErrorCode)) {
            /* a character byte sequence remains incomplete */
            *pErrorCode=U_TRUNCATED_CHAR_FOUND;
        }
        cnv->toUnicodeStatus=0x1000000; /* inDirectMode=TRUE */
        cnv->toULength=0;
    } else {
        /* set the converter state back into UConverter */
        cnv->toUnicodeStatus=((uint32_t)inDirectMode<<24)|((uint32_t)((uint8_t)base64Counter)<<16)|(uint32_t)bits;
        cnv->toULength=byteIndex;
    }

finish:
    /* write back the updated pointers */
    pArgs->source=(const char *)source;
    pArgs->target=target;
    pArgs->offsets=offsets;
    return;

callback:
    /* call the callback function with all the preparations and post-processing */
    /* update the arguments structure */
    pArgs->source=(const char *)source;
    pArgs->target=target;
    pArgs->offsets=offsets;

    /* copy the current bytes to invalidCharBuffer */
    for(b=0; b<(uint8_t)byteIndex; ++b) {
        cnv->invalidCharBuffer[b]=(char)bytes[b];
    }
    cnv->invalidCharLength=byteIndex;

    /* set the converter state in UConverter to deal with the next character */
    cnv->toUnicodeStatus=(uint32_t)inDirectMode<<24;
    cnv->toULength=0;

    /* call the callback function */
    *pErrorCode=U_ILLEGAL_CHAR_FOUND;
    cnv->fromCharErrorBehaviour(cnv->toUContext, pArgs, cnv->invalidCharBuffer, cnv->invalidCharLength, UCNV_ILLEGAL, pErrorCode);

    /* get the converter state from UConverter */
    {
        uint32_t status=cnv->toUnicodeStatus;
        inDirectMode=(UBool)((status>>24)&1);
        base64Counter=(int8_t)(status>>16);
        bits=(uint16_t)status;
    }
    byteIndex=cnv->toULength;

    /* update target and deal with offsets if necessary */
    offsets=ucnv_updateCallbackOffsets(offsets, pArgs->target-target, sourceIndex);
    target=pArgs->target;

    /* update the source pointer and index */
    sourceIndex=nextSourceIndex+((const uint8_t *)pArgs->source-source);
    source=(const uint8_t *)pArgs->source;

    /*
     * If the callback overflowed the target, then we need to
     * stop here with an overflow indication.
     */
    if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
        goto endloop;
    } else if(cnv->UCharErrorBufferLength>0) {
        /* target is full */
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        goto endloop;
    } else if(U_FAILURE(*pErrorCode)) {
        /* break on error */
        cnv->toUnicodeStatus=0x1000000; /* inDirectMode=TRUE */
        cnv->toULength=0;
        goto finish;
    } else {
        goto loop;
    }
}

U_CFUNC UChar32
_UTF7GetNextUChar(UConverterToUnicodeArgs *pArgs,
                  UErrorCode *pErrorCode) {
    return ucnv_getNextUCharFromToUImpl(pArgs, _UTF7ToUnicodeWithOffsets, TRUE, pErrorCode);
}

U_CFUNC void
_UTF7FromUnicodeWithOffsets(UConverterFromUnicodeArgs *pArgs,
                            UErrorCode *pErrorCode) {
    UConverter *cnv;
    const UChar *source, *sourceLimit;
    uint8_t *target, *targetLimit;
    int32_t *offsets;

    int32_t length, targetCapacity, sourceIndex;
    UChar c;

    /* UTF-7 state */
    const UBool *encodeDirectly;
    uint8_t bits;
    int8_t base64Counter;
    UBool inDirectMode;

    /* set up the local pointers */
    cnv=pArgs->converter;

    /* set up the local pointers */
    source=pArgs->source;
    sourceLimit=pArgs->sourceLimit;
    target=(uint8_t *)pArgs->target;
    targetLimit=(uint8_t *)pArgs->targetLimit;
    offsets=pArgs->offsets;

    /* get the state machine state */
    {
        uint32_t status=cnv->fromUnicodeStatus;
        encodeDirectly= status<0x10000000 ? encodeDirectlyMaximum : encodeDirectlyRestricted;
        inDirectMode=(UBool)((status>>24)&1);
        base64Counter=(int8_t)(status>>16);
        bits=(uint8_t)status;
    }

    /* UTF-7 always encodes UTF-16 code units, therefore we need only a simple sourceIndex */
    sourceIndex=0;

    if(inDirectMode) {
directMode:
        length=sourceLimit-source;
        targetCapacity=targetLimit-target;
        if(length>targetCapacity) {
            length=targetCapacity;
        }
        while(length>0) {
            c=*source++;
            /* currently always encode CR LF SP TAB directly */
            if(c<=127 && encodeDirectly[c]) {
                /* encode directly */
                *target++=(uint8_t)c;
                if(offsets!=NULL) {
                    *offsets++=sourceIndex++;
                }
            } else if(c==PLUS) {
                /* output +- for + */
                *target++=PLUS;
                if(target<targetLimit) {
                    *target++=MINUS;
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex;
                        *offsets++=sourceIndex++;
                    }
                    /* realign length and targetCapacity */
                    goto directMode;
                } else {
                    if(offsets!=NULL) {
                        *offsets++=sourceIndex++;
                    }
                    cnv->charErrorBuffer[0]=MINUS;
                    cnv->charErrorBufferLength=1;
                    *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                    break;
                }
            } else {
                /* un-read this character and switch to Unicode Mode */
                --source;
                *target++=PLUS;
                if(offsets!=NULL) {
                    *offsets++=sourceIndex;
                }
                inDirectMode=FALSE;
                base64Counter=0;
                goto unicodeMode;
            }
            --length;
        }
        if(source<sourceLimit && target>=targetLimit) {
            /* target is full */
            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
        }
    } else {
unicodeMode:
        while(source<sourceLimit) {
            if(target<targetLimit) {
                c=*source++;
                if(c<=127 && encodeDirectly[c]) {
                    /* encode directly */
                    inDirectMode=TRUE;

                    /* trick: back out this character to make this easier */
                    --source;

                    /* terminate the base64 sequence */
                    if(base64Counter!=0) {
                        /* write remaining bits for the previous character */
                        *target++=toBase64[bits];
                        if(offsets!=NULL) {
                            *offsets++=sourceIndex-1;
                        }
                    }
                    if(fromBase64[c]!=-1) {
                        /* need to terminate with a minus */
                        if(target<targetLimit) {
                            *target++=MINUS;
                            if(offsets!=NULL) {
                                *offsets++=sourceIndex;
                            }
                        } else {
                            cnv->charErrorBuffer[0]=MINUS;
                            cnv->charErrorBufferLength=1;
                            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                            break;
                        }
                    }
                    goto directMode;
                } else {
                    /*
                     * base64 this character:
                     * Output 2 or 3 base64 bytes for the remaining bits of the previous character
                     * and the bits of this character, each implicitly in UTF-16BE.
                     *
                     * Here, bits is an 8-bit variable because only 6 bits need to be kept from one
                     * character to the next. The actual 2 or 4 bits are shifted to the left edge
                     * of the 6-bits field 5..0 to make the termination of the base64 sequence easier.
                     */
                    switch(base64Counter) {
                    case 0:
                        *target++=toBase64[c>>10];
                        if(target<targetLimit) {
                            *target++=toBase64[(c>>4)&0x3f];
                            if(offsets!=NULL) {
                                *offsets++=sourceIndex;
                                *offsets++=sourceIndex++;
                            }
                        } else {
                            if(offsets!=NULL) {
                                *offsets++=sourceIndex++;
                            }
                            cnv->charErrorBuffer[0]=toBase64[(c>>4)&0x3f];
                            cnv->charErrorBufferLength=1;
                            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                        }
                        bits=(uint8_t)((c&15)<<2);
                        base64Counter=1;
                        break;
                    case 1:
                        *target++=toBase64[bits|(c>>14)];
                        if(target<targetLimit) {
                            *target++=toBase64[(c>>8)&0x3f];
                            if(target<targetLimit) {
                                *target++=toBase64[(c>>2)&0x3f];
                                if(offsets!=NULL) {
                                    *offsets++=sourceIndex;
                                    *offsets++=sourceIndex;
                                    *offsets++=sourceIndex++;
                                }
                            } else {
                                if(offsets!=NULL) {
                                    *offsets++=sourceIndex;
                                    *offsets++=sourceIndex++;
                                }
                                cnv->charErrorBuffer[0]=toBase64[(c>>2)&0x3f];
                                cnv->charErrorBufferLength=1;
                                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                            }
                        } else {
                            if(offsets!=NULL) {
                                *offsets++=sourceIndex++;
                            }
                            cnv->charErrorBuffer[0]=toBase64[(c>>8)&0x3f];
                            cnv->charErrorBuffer[1]=toBase64[(c>>2)&0x3f];
                            cnv->charErrorBufferLength=2;
                            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                        }
                        bits=(uint8_t)((c&3)<<4);
                        base64Counter=2;
                        break;
                    case 2:
                        *target++=toBase64[bits|(c>>12)];
                        if(target<targetLimit) {
                            *target++=toBase64[(c>>6)&0x3f];
                            if(target<targetLimit) {
                                *target++=toBase64[c&0x3f];
                                if(offsets!=NULL) {
                                    *offsets++=sourceIndex;
                                    *offsets++=sourceIndex;
                                    *offsets++=sourceIndex++;
                                }
                            } else {
                                if(offsets!=NULL) {
                                    *offsets++=sourceIndex;
                                    *offsets++=sourceIndex++;
                                }
                                cnv->charErrorBuffer[0]=toBase64[c&0x3f];
                                cnv->charErrorBufferLength=1;
                                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                            }
                        } else {
                            if(offsets!=NULL) {
                                *offsets++=sourceIndex++;
                            }
                            cnv->charErrorBuffer[0]=toBase64[(c>>6)&0x3f];
                            cnv->charErrorBuffer[1]=toBase64[c&0x3f];
                            cnv->charErrorBufferLength=2;
                            *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                        }
                        bits=0;
                        base64Counter=0;
                        break;
                    default:
                        /* will never occur */
                        break;
                    }
                }
            } else {
                /* target is full */
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
                break;
            }
        }
    }

    if(pArgs->flush && source>=sourceLimit) {
        /* flush remaining bits to the target */
        if(!inDirectMode && base64Counter!=0) {
            if(target<targetLimit) {
                *target++=toBase64[bits];
                if(offsets!=NULL) {
                    *offsets++=sourceIndex-1;
                }
            } else {
                cnv->charErrorBuffer[0]=toBase64[bits];
                cnv->charErrorBufferLength=1;
                *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
            }
        }
        /* reset the state for the next conversion */
        cnv->fromUnicodeStatus=(cnv->fromUnicodeStatus&0xf0000000)|0x1000000; /* keep version, inDirectMode=TRUE */
    } else {
        /* set the converter state back into UConverter */
        cnv->fromUnicodeStatus=
            (cnv->fromUnicodeStatus&0xf0000000)|    /* keep version*/
            ((uint32_t)inDirectMode<<24)|((uint32_t)base64Counter<<16)|(uint32_t)bits;
    }

    /* write back the updated pointers */
    pArgs->source=source;
    pArgs->target=(char *)target;
    pArgs->offsets=offsets;
    return;
}

U_CFUNC const char *
_UTF7GetName(const UConverter *cnv) {
    switch(cnv->fromUnicodeStatus>>28) {
    case 1:
        return "UTF-7,version=1";
    default:
        return "UTF-7";
    }
}

static const UConverterImpl _UTF7Impl={
    UCNV_UTF7,

    NULL,
    NULL,

    _UTF7Open,
    NULL,
    _UTF7Reset,

    _UTF7ToUnicodeWithOffsets,
    _UTF7ToUnicodeWithOffsets,
    _UTF7FromUnicodeWithOffsets,
    _UTF7FromUnicodeWithOffsets,
    _UTF7GetNextUChar,

    NULL,
    _UTF7GetName,
    NULL /* we don't need writeSub() because we never call a callback at fromUnicode() */
};

static const UConverterStaticData _UTF7StaticData={
    sizeof(UConverterStaticData),
    "UTF-7",
    0, /* CCSID for UTF-7 */
    UCNV_IBM, UCNV_UTF7,
    1, 4,
    { 0x3f, 0, 0, 0 }, 1, /* the subchar is not used */
    FALSE, FALSE,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};

const UConverterSharedData _UTF7Data={
    sizeof(UConverterSharedData), ~((uint32_t)0),
    NULL, NULL, &_UTF7StaticData, FALSE, &_UTF7Impl,
    0
};
