/*  
**********************************************************************
*   Copyright (C) 2002-2003, International Business Machines
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
*
*   Also, CESU-8 implementation, see UTR 26.
*   The CESU-8 converter uses all the same functions as the
*   UTF-8 converter, with a branch for converting supplementary code points.
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
                converter->toULength);
    converter->invalidCharLength = converter->toULength;

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
    UBool isCESU8 = (UBool)(args->converter->sharedData == &_CESU8Data);
    uint32_t ch, ch2 = 0;
    int32_t i, inBytes;
  
    /* Restore size of current sequence */
start:
    if (args->converter->toUnicodeStatus && myTarget < targetLimit)
    {
        inBytes = args->converter->mode;            /* restore # of bytes to consume */
        i = args->converter->toULength;             /* restore # of bytes consumed */

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
                    toUBytes[i] = (char) (ch2 = *mySource);
                    if (!UTF8_IS_TRAIL(ch2))
                    {
                        break; /* i < inBytes */
                    }
                    ch = (ch << 6) + ch2;
                    ++mySource;
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
                        args->converter->mode = inBytes;
                        args->converter->toULength = (int8_t) i;
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
             * - use at most 4 bytes (for i>=5 it is 0x10ffff<utf8_minChar32[])
             *
             * Starting with Unicode 3.2, surrogate code points must not be encoded in UTF-8.
             * There are no irregular sequences any more.
             * In CESU-8, only surrogates, not supplementary code points, are encoded directly.
             */
            if (i == inBytes && ch <= MAXIMUM_UTF && ch >= utf8_minChar32[i] &&
                (isCESU8 ? i <= 3 : !UTF_IS_SURROGATE(ch)))
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

                args->converter->toULength = (int8_t)i;
                if (T_UConverter_toUnicode_InvalidChar_Callback(args, UCNV_ILLEGAL, err))
                {
                    /* Stop if the error wasn't handled */
                    /* args and err should already be set properly */
                    return;
                }

                mySource = (unsigned char *) args->source;
                myTarget = args->target;

                /* goto the start to handle state left behind by the callback */
                goto start;
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
    UBool isCESU8 = (UBool)(args->converter->sharedData == &_CESU8Data);
    uint32_t ch, ch2 = 0;
    int32_t i, inBytes;

    /* Restore size of current sequence */
start:
    if (args->converter->toUnicodeStatus && myTarget < targetLimit)
    {
        inBytes = args->converter->mode;            /* restore # of bytes to consume */
        i = args->converter->toULength;             /* restore # of bytes consumed */

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
                    toUBytes[i] = (char) (ch2 = *mySource);
                    if (!UTF8_IS_TRAIL(ch2))
                    {
                        break; /* i < inBytes */
                    }
                    ch = (ch << 6) + ch2;
                    ++mySource;
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
                        args->converter->mode = inBytes;
                        args->converter->toULength = (int8_t)i;
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
             * - use at most 4 bytes (for i>=5 it is 0x10ffff<utf8_minChar32[])
             *
             * Starting with Unicode 3.2, surrogate code points must not be encoded in UTF-8.
             * There are no irregular sequences any more.
             * In CESU-8, only surrogates, not supplementary code points, are encoded directly.
             */
            if (i == inBytes && ch <= MAXIMUM_UTF && ch >= utf8_minChar32[i] &&
                (isCESU8 ? i <= 3 : !UTF_IS_SURROGATE(ch)))
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
                args->source = (const char *) mySource;
                args->target = myTarget;
                args->offsets = myOffsets;

                args->converter->toULength = (int8_t)i;
                if (T_UConverter_toUnicode_InvalidChar_OffsetCallback(args,
                    offsetNum, UCNV_ILLEGAL, err))
                {
                    /* Stop if the error wasn't handled */
                    /* args and err should already be set properly */
                    return;
                }

                offsetNum += i + ((unsigned char *) args->source - mySource);
                mySource = (unsigned char *) args->source;
                myTarget = args->target;
                myOffsets = args->offsets;

                /* goto the start to handle state left behind by the callback */
                goto start;
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
    UConverter *cnv = args->converter;
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    const UChar *sourceLimit = args->sourceLimit;
    const unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    UBool isCESU8 = (UBool)(args->converter->sharedData == &_CESU8Data);
    uint32_t ch, ch2;
    int16_t indexToWrite;
    char temp[4];

    if (cnv->fromUSurrogateLead && myTarget < targetLimit)
    {
        ch = cnv->fromUSurrogateLead;
        cnv->fromUSurrogateLead = 0;
        goto lowsurrogate;
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
                cnv->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                cnv->charErrorBufferLength = 1;
                *err = U_BUFFER_OVERFLOW_ERROR;
            }
        }
        else
        /* Check for surrogates */
        {
            if(UTF_IS_SURROGATE(ch) && !isCESU8) {
                if(UTF_IS_SURROGATE_FIRST(ch)) {
lowsurrogate:
                    if (mySource < sourceLimit) {
                        /* test the following code unit */
                        UChar trail=*mySource;
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySource;
                            ch=UTF16_GET_PAIR_VALUE(ch, trail);
                            ch2 = 0;
                            /* convert this supplementary code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            ch2 = ch;
                        }
                    } else {
                        /* no more input */
                        cnv->fromUSurrogateLead = (UChar)ch;
                        break;
                    }
                } else {
                    /* this is an unmatched trail code unit (2nd surrogate) */
                    /* callback(illegal) */
                    ch2 = ch;
                }

                if(ch2 != 0) {
                    /* call the callback function with all the preparations and post-processing */
                    *err = U_ILLEGAL_CHAR_FOUND;

                    /* update the arguments structure */
                    args->source=mySource;
                    args->target=(char *)myTarget;

                    /* write the code point as code units */
                    cnv->invalidUCharBuffer[0] = (UChar)ch2;
                    cnv->invalidUCharLength = 1;

                    /* call the callback function */
                    cnv->fromUCharErrorBehaviour(cnv->fromUContext, args, cnv->invalidUCharBuffer, 1, ch2, UCNV_ILLEGAL, err);

                    /* get the converter state from UConverter */
                    ch = cnv->fromUSurrogateLead;
                    cnv->fromUSurrogateLead = 0;

                    myTarget=(uint8_t *)args->target;
                    mySource=args->source;

                    /*
                     * If the callback overflowed the target, then we need to
                     * stop here with an overflow indication.
                     */
                    if(*err==U_BUFFER_OVERFLOW_ERROR) {
                        break;
                    } else if(U_FAILURE(*err)) {
                        /* break on error */
                        break;
                    } else if(cnv->charErrorBufferLength>0) {
                        /* target is full */
                        *err=U_BUFFER_OVERFLOW_ERROR;
                        break;
                        /*
                         * } else if(ch != 0) { ...
                         * ### TODO 2002jul01 markus: It looks like this code (from ucnvmbcs.c)
                         * does not handle the case where the callback leaves ch=fromUSurrogateLead!=0 .
                         * We would have to check myTarget<targetLimit and goto lowsurrogate?!
                         */
                    }

                    continue;
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
                    cnv->charErrorBuffer[cnv->charErrorBufferLength++] = temp[indexToWrite];
                    *err = U_BUFFER_OVERFLOW_ERROR;
                }
            }
        }
    }

    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }
    if(args->flush && mySource >= sourceLimit && cnv->fromUSurrogateLead != 0 && U_SUCCESS(*err)) {
        /* a Unicode code point remains incomplete (only a first surrogate) */
        *err = U_TRUNCATED_CHAR_FOUND;
        cnv->fromUSurrogateLead = 0;
    }

    args->target = (char *) myTarget;
    args->source = mySource;
}

U_CFUNC void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                  UErrorCode * err)
{
    UConverter *cnv = args->converter;
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t *myOffsets = args->offsets;
    const UChar *sourceLimit = args->sourceLimit;
    const unsigned char *targetLimit = (unsigned char *) args->targetLimit;
    UBool isCESU8 = (UBool)(args->converter->sharedData == &_CESU8Data);
    uint32_t ch, ch2;
    int32_t offsetNum, nextSourceIndex;
    int16_t indexToWrite;
    char temp[4];

    if (cnv->fromUSurrogateLead && myTarget < targetLimit)
    {
        ch = cnv->fromUSurrogateLead;
        cnv->fromUSurrogateLead = 0;
        offsetNum = -1;
        nextSourceIndex = 0;
        goto lowsurrogate;
    } else {
        offsetNum = 0;
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
                cnv->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                cnv->charErrorBufferLength = 1;
                *err = U_BUFFER_OVERFLOW_ERROR;
            }
        }
        else
        /* Check for surrogates */
        {
            nextSourceIndex = offsetNum + 1;

            if(UTF_IS_SURROGATE(ch) && !isCESU8) {
                if(UTF_IS_SURROGATE_FIRST(ch)) {
lowsurrogate:
                    if (mySource < sourceLimit) {
                        /* test the following code unit */
                        UChar trail=*mySource;
                        if(UTF_IS_SECOND_SURROGATE(trail)) {
                            ++mySource;
                            ++nextSourceIndex;
                            ch=UTF16_GET_PAIR_VALUE(ch, trail);
                            ch2 = 0;
                            /* convert this supplementary code point */
                            /* exit this condition tree */
                        } else {
                            /* this is an unmatched lead code unit (1st surrogate) */
                            /* callback(illegal) */
                            ch2 = ch;
                        }
                    } else {
                        /* no more input */
                        cnv->fromUSurrogateLead = (UChar)ch;
                        break;
                    }
                } else {
                    /* this is an unmatched trail code unit (2nd surrogate) */
                    /* callback(illegal) */
                    ch2 = ch;
                }

                if(ch2 != 0) {
                    /* call the callback function with all the preparations and post-processing */
                    *err = U_ILLEGAL_CHAR_FOUND;

                    /* update the arguments structure */
                    args->source=mySource;
                    args->target=(char *)myTarget;
                    args->offsets=myOffsets;

                    /* write the code point as code units */
                    cnv->invalidUCharBuffer[0] = (UChar)ch2;
                    cnv->invalidUCharLength = 1;

                    /* call the callback function */
                    cnv->fromUCharErrorBehaviour(cnv->fromUContext, args, cnv->invalidUCharBuffer, 1, ch2, UCNV_ILLEGAL, err);

                    /* get the converter state from UConverter */
                    ch = cnv->fromUSurrogateLead;
                    cnv->fromUSurrogateLead = 0;

                    /* update target and deal with offsets if necessary */
                    myOffsets=ucnv_updateCallbackOffsets(myOffsets, ((uint8_t *)args->target)-myTarget, offsetNum);
                    myTarget=(uint8_t *)args->target;

                    /* update the source pointer and index */
                    offsetNum=nextSourceIndex+(args->source-mySource);
                    mySource=args->source;

                    /*
                     * If the callback overflowed the target, then we need to
                     * stop here with an overflow indication.
                     */
                    if(*err==U_BUFFER_OVERFLOW_ERROR) {
                        break;
                    } else if(U_FAILURE(*err)) {
                        /* break on error */
                        break;
                    } else if(cnv->charErrorBufferLength>0) {
                        /* target is full */
                        *err=U_BUFFER_OVERFLOW_ERROR;
                        break;
                        /*
                         * } else if(ch != 0) { ...
                         * ### TODO 2002jul01 markus: It looks like this code (from ucnvmbcs.c)
                         * does not handle the case where the callback leaves ch=fromUSurrogateLead!=0 .
                         * We would have to check myTarget<targetLimit and goto lowsurrogate?!
                         */
                    }

                    continue;
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
                    cnv->charErrorBuffer[cnv->charErrorBufferLength++] = temp[indexToWrite];
                    *err = U_BUFFER_OVERFLOW_ERROR;
                }
            }
            offsetNum = nextSourceIndex;
        }
    }

    if (mySource < sourceLimit && myTarget >= targetLimit && U_SUCCESS(*err))
    {
        *err = U_BUFFER_OVERFLOW_ERROR;
    }
    if(args->flush && mySource >= sourceLimit && cnv->fromUSurrogateLead != 0 && U_SUCCESS(*err)) {
        /* a Unicode code point remains incomplete (only a first surrogate) */
        *err = U_TRUNCATED_CHAR_FOUND;
        cnv->fromUSurrogateLead = 0;
    }

    args->target = (char *) myTarget;
    args->source = mySource;
    args->offsets = myOffsets;
}

U_CFUNC UChar32 T_UConverter_getNextUChar_UTF8(UConverterToUnicodeArgs *args,
                                               UErrorCode *err) {
    UChar buffer[2];
    const char *sourceInitial;
    const uint8_t *source;
    UChar* myUCharPtr;
    uint16_t extraBytesToWrite;
    uint8_t myByte;
    UChar32 ch;
    int8_t isLegalSequence;
    UBool isCESU8 = (UBool)(args->converter->sharedData == &_CESU8Data);

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
        source = (const uint8_t *)args->source;
        if (((const char *)source + extraBytesToWrite - 1) > args->sourceLimit)
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
                ch += (myByte = *source++);
                ch <<= 6;
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                    break;
                }
            case 5:
                ch += (myByte = *source++);
                ch <<= 6;
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                    break;
                }
            case 4:
                ch += (myByte = *source++);
                ch <<= 6;
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                    break;
                }
            case 3:
                ch += (myByte = *source++);
                ch <<= 6;
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                    break;
                }
            case 2:
                ch += (myByte = *source++);
                if (!UTF8_IS_TRAIL(myByte))
                {
                    isLegalSequence = 0;
                }
            };
        }
        ch -= offsetsFromUTF8[extraBytesToWrite];
        args->source = (const char *)source;

        /*
         * Legal UTF-8 byte sequences in Unicode 3.0.1 and up:
         * - use only trail bytes after a lead byte (checked above)
         * - use the right number of trail bytes for a given lead byte
         * - encode a code point <= U+10ffff
         * - use the fewest possible number of bytes for their code points
         * - use at most 4 bytes (for i>=5 it is 0x10ffff<utf8_minChar32[])
         *
         * Starting with Unicode 3.2, surrogate code points must not be encoded in UTF-8.
         * There are no irregular sequences any more.
         * In CESU-8, only surrogates, not supplementary code points, are encoded directly.
         */
        if (isLegalSequence && (uint32_t)ch <= MAXIMUM_UTF && (uint32_t)ch >= utf8_minChar32[extraBytesToWrite]) {
            if(isCESU8) {
                if(extraBytesToWrite <= 3) {
                    if( UTF_IS_FIRST_SURROGATE(ch) &&
                        (const char *)(source + 3) <= args->sourceLimit &&
                        source[0] == 0xed && (source[1] & 0xf0) == 0xb0 && (source[2] & 0xc0) == 0x80
                    ) {
                        /* ch is a lead surrogate followed by a trail surrogate */
                        ch = (ch << 10) +
                             ((source[1] & 0xf) << 6) + (source[2] & 0x3f) -
                             ((0xd800 << 10) - 0x10000);
                        args->source = (const char *)(source + 3);
                    }
                    return ch; /* return the code point */
                }
                /* illegal CESU-8 */
            } else {
                if(!UTF_IS_SURROGATE(ch)) {
                    return ch; /* return the code point */
                }
                /* illegal UTF-8 */
            }
        }

CALL_ERROR_FUNCTION:
        extraBytesToWrite = (uint16_t)(args->source - sourceInitial);
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

/* UTF-8 converter data ----------------------------------------------------- */

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
    NULL,
    NULL,
    NULL,
    ucnv_getNonSurrogateUnicodeSet
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

/* CESU-8 converter data ---------------------------------------------------- */

static const UConverterStaticData _CESU8StaticData={
    sizeof(UConverterStaticData),
    "CESU-8",
    0, UCNV_UNKNOWN, UCNV_CESU8, 1, 3,
    { 0xef, 0xbf, 0xbd, 0 },3,FALSE,FALSE,
    0,
    0,
    { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0 } /* reserved */
};


const UConverterSharedData _CESU8Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_CESU8StaticData, FALSE, &_UTF8Impl,
    0
};
