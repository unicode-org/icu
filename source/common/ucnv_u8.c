/*  
**********************************************************************
*   Copyright (C) 2002, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv_u8.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2002jul01
*   created by: Markus W. Scherer
*
*   UTF-8 converter implementation. Used to be in ucnv_utf.c.
*/

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "ucnv_cnv.h"
#include "cmemory.h"

/* Prototypes --------------------------------------------------------------- */

/* Keep these here to make finicky compilers happy */

U_CFUNC void T_UConverter_toUnicode_UTF8(UConverterToUnicodeArgs *args,
                                         UErrorCode *err);
U_CFUNC void T_UConverter_toUnicode_UTF8_OFFSETS_LOGIC(UConverterToUnicodeArgs *args,
                                                       UErrorCode *err);
U_CFUNC void T_UConverter_fromUnicode_UTF8(UConverterFromUnicodeArgs *args,
                                           UErrorCode *err);
U_CFUNC void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC(UConverterFromUnicodeArgs *args,
                                                        UErrorCode *err);
U_CFUNC UChar32 T_UConverter_getNextUChar_UTF8(UConverterToUnicodeArgs *args,
                                               UErrorCode *err);


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

/*
 * Starting with Unicode 3.0.1:
 * UTF-8 byte sequences of length N _must_ encode code points of or above utf8_minChar32[N];
 * byte sequences with more than 4 bytes are illegal in UTF-8,
 * which is tested with impossible values for them
 */
static const uint32_t
utf8_minChar32[7]={ 0, 0, 0x80, 0x800, 0x10000, 0xffffffff, 0xffffffff };

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
                    if (!UTF8_IS_TRAIL(ch2))
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

            /* Remove the accumulated high bits */
            ch -= offsetsFromUTF8[inBytes];

            /*
             * Legal UTF-8 byte sequences in Unicode 3.0.1 and up:
             * - use only trail bytes after a lead byte (checked above)
             * - use the right number of trail bytes for a given lead byte
             * - encode a code point <= U+10ffff
             * - use the fewest possible number of bytes for their code points
             * - use at most 4 bytes (for i>=4 it is 0x10ffff<utf8_minChar32[])
             * - single surrogate code points are legal but irregular (also cause a callback)
             */
            if (i == inBytes && ch <= MAXIMUM_UTF && ch >= utf8_minChar32[i] && !UTF_IS_SURROGATE(ch))
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
                UConverterCallbackReason reason =
                    i == inBytes && i == 3 && UTF_IS_SURROGATE(ch) ? UCNV_IRREGULAR : UCNV_ILLEGAL;
                args->source = (const char *) mySource;
                args->target = myTarget;
                args->converter->invalidCharLength = (int8_t)i;
                if (T_UConverter_toUnicode_InvalidChar_Callback(args, reason, err))
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
                    if (!UTF8_IS_TRAIL(ch2))
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

            /* Remove the accumulated high bits */
            ch -= offsetsFromUTF8[inBytes];

            /*
             * Legal UTF-8 byte sequences in Unicode 3.0.1 and up:
             * - use only trail bytes after a lead byte (checked above)
             * - use the right number of trail bytes for a given lead byte
             * - encode a code point <= U+10ffff
             * - use the fewest possible number of bytes for their code points
             * - use at most 4 bytes (for i>=4 it is 0x10ffff<utf8_minChar32[])
             * - single surrogate code points are legal but irregular (also cause a callback)
             */
            if (i == inBytes && ch <= MAXIMUM_UTF && ch >= utf8_minChar32[i] && !UTF_IS_SURROGATE(ch))
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
                        args->converter->UCharErrorBuffer[0] = (UChar) ch;
                        args->converter->UCharErrorBufferLength = 1;
                        *err = U_BUFFER_OVERFLOW_ERROR;
                    }
                }
                offsetNum += i;
            }
            else
            {
                UConverterCallbackReason reason =
                    i == inBytes && i == 3 && UTF_IS_SURROGATE(ch) ? UCNV_IRREGULAR : UCNV_ILLEGAL;
                UBool useOffset;

                args->source = (const char *) mySource;
                args->target = myTarget;
                args->offsets = myOffsets;
                args->converter->invalidCharLength = (int8_t)i;
                if (T_UConverter_toUnicode_InvalidChar_OffsetCallback(args,
                 offsetNum, reason, err))
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
    args->offsets = myOffsets;
}

U_CFUNC UChar32 T_UConverter_getNextUChar_UTF8(UConverterToUnicodeArgs *args,
                                               UErrorCode *err) {
    UChar buffer[2];
    char const *sourceInitial;
    UChar* myUCharPtr;
    UConverterCallbackReason reason;
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
            isLegalSequence = FALSE;
            ch = 0;
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
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                    break;
                }
            case 5:
                ch += (myByte = *(args->source++));
                ch <<= 6;
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                    break;
                }
            case 4:
                ch += (myByte = *(args->source++));
                ch <<= 6;
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                    break;
                }
            case 3:
                ch += (myByte = *(args->source++));
                ch <<= 6;
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                    break;
                }
            case 2:
                ch += (myByte = *(args->source++));
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                }
            };
        }
        ch -= offsetsFromUTF8[extraBytesToWrite];

        /*
         * Legal UTF-8 byte sequences in Unicode 3.0.1 and up:
         * - use only trail bytes after a lead byte (checked above)
         * - use the right number of trail bytes for a given lead byte
         * - encode a code point <= U+10ffff
         * - use the fewest possible number of bytes for their code points
         * - use at most 4 bytes (for i>=4 it is 0x10ffff<utf8_minChar32[])
         * - single surrogate code points are legal but irregular (also cause a callback)
         */
        if (isLegalSequence && (uint32_t)ch <= MAXIMUM_UTF && (uint32_t)ch >= utf8_minChar32[extraBytesToWrite] && !UTF_IS_SURROGATE(ch)) {
            return ch; /* return the code point */
        }

CALL_ERROR_FUNCTION:
        extraBytesToWrite = (uint16_t)(args->source - sourceInitial);
        args->converter->invalidCharLength = (uint8_t)extraBytesToWrite;
        uprv_memcpy(args->converter->invalidCharBuffer, sourceInitial, extraBytesToWrite);

        myUCharPtr = buffer;
        if (isLegalSequence && extraBytesToWrite == 3 && UTF_IS_SURROGATE(ch)) {
            reason = UCNV_IRREGULAR;
            *err = U_INVALID_CHAR_FOUND;
        } else {
            reason = UCNV_ILLEGAL;
            *err = U_ILLEGAL_CHAR_FOUND;
        }
        args->target = myUCharPtr;
        args->targetLimit = buffer + 2;
        args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                        args,
                                        sourceInitial,
                                        extraBytesToWrite,
                                        reason,
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

/* The 1208 CCSID refers to any version of Unicode of UTF-8 */
static const UConverterStaticData _UTF8StaticData={
    sizeof(UConverterStaticData),
    "UTF-8",
    1208, UCNV_IBM, UCNV_UTF8, 1, 4,
    { 0xef, 0xbf, 0xbd, 0 },3,FALSE,FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _UTF8Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF8StaticData, FALSE, &_UTF8Impl, 
    0
};
