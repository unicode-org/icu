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
*/

#include "cmemory.h"
#include "unicode/utypes.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"

/* UTF-8 -------------------------------------------------------------------- */

/* UTF-8 Conversion DATA
 *   for more information see Unicode Strandard 2.0 , Transformation Formats Appendix A-9
 */
/*static const uint32_t REPLACEMENT_CHARACTER = 0x0000FFFD;*/
static const uint32_t MAXIMUM_UCS2 = 0x0000FFFF;
static const uint32_t MAXIMUM_UTF16 = 0x0010FFFF;
static const uint32_t MAXIMUM_UCS4 = 0x7FFFFFFF;
static const int8_t HALF_SHIFT = 10;
static const uint32_t HALF_BASE = 0x0010000;
static const uint32_t HALF_MASK = 0x3FF;
static const uint32_t SURROGATE_HIGH_START = 0xD800;
static const uint32_t SURROGATE_HIGH_END = 0xDBFF;
static const uint32_t SURROGATE_LOW_START = 0xDC00;
static const uint32_t SURROGATE_LOW_END = 0xDFFF;

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

//static const unsigned char firstByteMark[7] = {0x00, 0x00, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC};

#define INVALID_UTF8_TAIL(utf8) (((utf8) & 0xC0) != 0x80)

/**
 * Calls invalid char callback when an invalid character sequence is encountered.
 * It presumes that the converter has a callback to call.
 *
 * @returns true when call back fails
 */
UBool T_UConverter_toUnicode_InvalidChar_Callback(UConverterToUnicodeArgs * args,
                                                  int32_t *sourceIndex,
                                                  int32_t *targetIndex,
                                                  UErrorCode *err)
{
    UConverter *converter = args->converter;
    UChar* saveTarget = args->target;
    const char* saveSource = args->source;

    *err = U_ILLEGAL_CHAR_FOUND;
    /*  */
    uprv_memcpy(converter->invalidCharBuffer,
                converter->toUBytes,
                converter->invalidCharLength);
    /* prepare for new character */

    args->source = (const char*) saveSource + *sourceIndex;
    args->target = saveTarget + *targetIndex;

    /* Call the ErrorFunction */
    args->converter->fromCharErrorBehaviour(converter->toUContext,
                                            args,
                                            converter->invalidCharBuffer,
                                            converter->invalidCharLength,
                                            UCNV_ILLEGAL,
                                            err);

    /* restore the state in case the callback changed it */
    converter->toUnicodeStatus = 0;
    converter->toULength = 0;
    converter->mode = 0;
    *targetIndex = args->target - saveTarget;
    *sourceIndex = args->source - (const char*)saveSource;
    args->source = saveSource;
    args->target = saveTarget;

    return U_FAILURE(*err);
}

U_CFUNC void T_UConverter_toUnicode_UTF8 (UConverterToUnicodeArgs * args,
                                  UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    UConverter *converter = args->converter;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - myTarget;
    int32_t sourceLength = args->sourceLimit - (char *) mySource;
    uint32_t ch, ch2,
             i;        /* Index into the current # of bytes consumed in the current sequence */
    uint32_t inBytes;  /* Total number of bytes in the current UTF8 sequence */
  
    inBytes = converter->toUnicodeStatus; /* Restore size of current sequence */
    if (inBytes && myTargetIndex < targetLength)
    {
        converter->toUnicodeStatus = 0;
        i = converter->toULength;   /* restore # of bytes consumed */
        converter->toULength = 0;

        ch = converter->mode; /*Stores the previously calculated ch from a previous call*/
        converter->mode = 0;
        goto morebytes;
    }


    while (mySourceIndex < sourceLength)
    {
        if (myTargetIndex < targetLength)
        {
            ch = mySource[mySourceIndex++];
            if (ch < 0x80)        /* Simple case */
            {
                myTarget[myTargetIndex++] = (UChar) ch;
            }
            else
            {
                /* store the first char */

                converter->toUBytes[0] = (char)ch;
                inBytes = bytesFromUTF8[ch]; /* lookup current sequence length */
                i = 1;

morebytes:
                while (i < inBytes)
                {
                    if (mySourceIndex < sourceLength)
                    {
                        converter->toUBytes[i] = (char) (ch2 = mySource[mySourceIndex++]);
                        if (INVALID_UTF8_TAIL(ch2))
                            break;
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
                        {
                            converter->toUnicodeStatus = inBytes;
                            converter->toULength = (int8_t)i;
                            converter->mode = ch; /* stores a partially calculated target*/
                        }
                        goto donefornow;
                    }
                }

                ch -= offsetsFromUTF8[inBytes];

                if (i == inBytes && ch <= MAXIMUM_UTF16)
                {   /* Normal valid byte when the loop has not prematurely terminated (i < inBytes) */
                    if (ch <= MAXIMUM_UCS2) 
                    {   /* fits in 16 bits */
                        myTarget[myTargetIndex++] = (UChar) ch;
                    }
                    else
                    {
                        ch -= HALF_BASE;
                        myTarget[myTargetIndex++] = (UChar) ((ch >> HALF_SHIFT) + SURROGATE_HIGH_START);
                        ch = (ch & HALF_MASK) + SURROGATE_LOW_START;
                        if (myTargetIndex < targetLength)
                        {
                            myTarget[myTargetIndex++] = (UChar)ch;
                        }
                        else
                        {
                            /* not using invalidUChar anymore */
                            converter->UCharErrorBuffer[0] = (UChar) ch;
                            converter->UCharErrorBufferLength = 1;
                            break;
                        }
                    }
                }
                else
                {
                    if (args->converter->fromCharErrorBehaviour
                        != (UConverterToUCallback) UCNV_TO_U_CALLBACK_STOP)
                    {
                        converter->invalidCharLength = (int8_t)i;
                        if (T_UConverter_toUnicode_InvalidChar_Callback(args,
                            &mySourceIndex, &myTargetIndex, err))
                        {
                            break;
                        }
                    }
                    else
                    {
                        break;
                    }
                }
            }
        }
        else
        /* End of target buffer */
        {
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
            break;
        }
    }

donefornow:
    args->target += myTargetIndex;
    args->source += mySourceIndex;
}

U_CFUNC void T_UConverter_toUnicode_UTF8_OFFSETS_LOGIC (UConverterToUnicodeArgs * args,
                                                UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    int32_t *myOffsets = args->offsets;
    UConverter *converter = args->converter;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - myTarget;
    int32_t sourceLength = args->sourceLimit - (char *) mySource;
    uint32_t ch, ch2, i;
    uint32_t inBytes;

    inBytes = converter->toUnicodeStatus; /* Restore size of current sequence */
    if (converter->toUnicodeStatus && myTargetIndex < targetLength)
    {
        converter->toUnicodeStatus = 0;
        i = converter->toULength;   /* restore # of bytes consumed */
        converter->toULength = 0;

        ch = converter->mode; /*Stores the previously calculated ch from a previous call*/
        converter->mode = 0;

        goto morebytes;
    }

    while (mySourceIndex < sourceLength)
    {
        if (myTargetIndex < targetLength)
        {
            ch = mySource[mySourceIndex++];
            if (ch < 0x80)        /* Simple case */
            {
                myOffsets[myTargetIndex] = mySourceIndex - 1;
                myTarget[myTargetIndex++] = (UChar) ch;
            }
            else
            {
                converter->invalidCharBuffer[0] = (char)ch;
                inBytes = bytesFromUTF8[ch];
                i = 1;

morebytes:
                while (i < inBytes)
                {
                    if (mySourceIndex < sourceLength)
                    {
                        converter->invalidCharBuffer[i] = (char) (ch2 = mySource[mySourceIndex++]);
                        if (INVALID_UTF8_TAIL(ch2))
                            break;
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
                                converter->toUnicodeStatus = 0x00;
                            }
                        }
                        else
                        {
                            converter->toUnicodeStatus = inBytes;
                            converter->toULength = (int8_t)i;
                        }
                        goto donefornow;
                    }
                }

                ch -= offsetsFromUTF8[inBytes];

                if (i == inBytes && ch <= MAXIMUM_UTF16)
                {
                    myOffsets[myTargetIndex] = mySourceIndex - inBytes;
                    if (ch <= MAXIMUM_UCS2)
                    {
                        myTarget[myTargetIndex++] = (UChar) ch;
                    }
                    else
                    {
                        ch -= HALF_BASE;
                        myTarget[myTargetIndex++] = (UChar) ((ch >> HALF_SHIFT) + SURROGATE_HIGH_START);
                        ch = (ch & HALF_MASK) + SURROGATE_LOW_START;
                        if (myTargetIndex < targetLength)
                        {
                            myOffsets[myTargetIndex] = mySourceIndex - inBytes;
                            myTarget[myTargetIndex++] = (UChar)ch;
                        }
                        else
                        {
                            converter->invalidUCharBuffer[0] = (UChar) ch;
                            converter->invalidUCharLength = 1;
                            *err = U_INDEX_OUTOFBOUNDS_ERROR;
                        }
                    }
                }
                else
                {
                    int32_t currentOffset = myOffsets[myTargetIndex - 1];
                    int32_t My_i = myTargetIndex;
                    UChar* saveTarget = args->target;
                    const char* saveSource = args->source;
                    int32_t* saveOffsets = myOffsets;

                    *err = U_ILLEGAL_CHAR_FOUND;
                    converter->invalidCharLength = (int8_t)i;
                    uprv_memcpy(converter->invalidCharBuffer, converter->toUBytes, i);

                    args->target = myTarget + myTargetIndex;
                    args->source = (const char*)mySource + mySourceIndex;
                    myOffsets = myOffsets ? (myOffsets + myTargetIndex) : 0;

                    /* To do HSYS: more smarts here, including offsets */
                    ToU_CALLBACK_OFFSETS_LOGIC_MACRO(converter->toUContext,
                                     args,
                                     converter->invalidCharBuffer,
                                     i,
                                     UCNV_UNASSIGNED,
                                     err);

                    converter->toUnicodeStatus = 0;
                    converter->toULength = 0;
                    converter->mode = 0;
                    args->source = saveSource;
                    args->target = saveTarget;
 
                    if (U_FAILURE (*err))
                        break;
                    converter->invalidCharLength = 0;
                }
            }
        }
        else
        /* End of target buffer */
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

donefornow:
    args->target += myTargetIndex;
    args->source += mySourceIndex;
}

U_CFUNC void T_UConverter_fromUnicode_UTF8 (UConverterFromUnicodeArgs * args,
                                    UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - (char *) myTarget;
    int32_t sourceLength = args->sourceLimit - mySource;
    uint32_t ch;
    int16_t i, bytesToWrite;
    uint32_t ch2;
    char temp[4];

    if (args->converter->fromUnicodeStatus && myTargetIndex < targetLength)
    {
        ch = args->converter->fromUnicodeStatus;
        args->converter->fromUnicodeStatus = 0;
        goto lowsurogate;
    }

    while (mySourceIndex < sourceLength)
    {
        if (myTargetIndex < targetLength)
        {
            bytesToWrite = 0;
            ch = mySource[mySourceIndex++];

            if (ch < 0x80)        /* Single byte */
            {
                myTarget[myTargetIndex++] = (char) ch;
            }
            else if (ch < 0x800)  /* Double byte */
            {
                myTarget[myTargetIndex++] = (char) ((ch >> 6) | 0xc0);
                if (myTargetIndex < targetLength)
                {
                    myTarget[myTargetIndex++] = (char) ((ch & 0x3f) | 0x80);
                }
                else
                {
                    args->converter->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                    args->converter->charErrorBufferLength = 1;
                    *err = U_INDEX_OUTOFBOUNDS_ERROR;
                }
            }
            else
            /* Check for surogates */
            {
                if ((ch >= SURROGATE_HIGH_START) && (ch <= SURROGATE_HIGH_END))
                {
lowsurogate:
                    if (mySourceIndex < sourceLength)
                    {
                        ch2 = mySource[mySourceIndex];
                        if ((ch2 >= SURROGATE_LOW_START) && (ch2 <= SURROGATE_LOW_END))
                        {
                            ch = ((ch - SURROGATE_HIGH_START) << HALF_SHIFT) + (ch2 - SURROGATE_LOW_START) + HALF_BASE;
                            mySourceIndex++;
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
                    bytesToWrite = 3;
                    temp[0] = (char) ((ch >> 12) | 0xe0);
                    temp[1] = (char) ((ch >> 6) & 0x3f | 0x80);
                    temp[2] = (char) (ch & 0x3f | 0x80);
                }
                else
                {
                    bytesToWrite = 4;
                    temp[0] = (char) ((ch >> 18) | 0xf0);
                    temp[1] = (char) ((ch >> 12) & 0x3f | 0x80);
                    temp[2] = (char) ((ch >> 6) & 0x3f | 0x80);
                    temp[3] = (char) (ch & 0x3f | 0x80);
                }
                for (i = 0; i < bytesToWrite; i++)
                {
                    if (myTargetIndex < targetLength)
                    {
                        myTarget[myTargetIndex++] = temp[i];
                    }
                    else
                    {
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = temp[i];
                        *err = U_INDEX_OUTOFBOUNDS_ERROR;
                    }
                }
            }
        }
        else
        {
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
            break;
        }
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
}

U_CFUNC void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                  UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t *myOffsets = args->offsets;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - (char *) myTarget;
    int32_t sourceLength = args->sourceLimit - mySource;
    uint32_t ch;
    int16_t i, bytesToWrite = 0;
    uint32_t ch2;
    char temp[4];

    if (args->converter->fromUnicodeStatus && myTargetIndex < targetLength)
    {
        ch = args->converter->fromUnicodeStatus;
        args->converter->fromUnicodeStatus = 0;
        goto lowsurogate;
    }

    while (mySourceIndex < sourceLength)
    {
        if (myTargetIndex < targetLength)
        {
            bytesToWrite = 0;
            ch = mySource[mySourceIndex++];

            if (ch < 0x80)        /* Single byte */
            {
                myOffsets[myTargetIndex] = mySourceIndex-1;
                myTarget[myTargetIndex++] = (char) ch;
            }
            else if (ch < 0x800)  /* Double byte */
            {
                myOffsets[myTargetIndex] = mySourceIndex-1;
                myTarget[myTargetIndex++] = (char) ((ch >> 6) | 0xc0);
                if (myTargetIndex < targetLength)
                {
                    myOffsets[myTargetIndex] = mySourceIndex-1;
                    myTarget[myTargetIndex++] = (char) ((ch & 0x3f) | 0x80);
                }
                else
                {
                    args->converter->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                    args->converter->charErrorBufferLength = 1;
                    *err = U_INDEX_OUTOFBOUNDS_ERROR;
                }
            }
            else
            /* Check for surogates */
            {
                if ((ch >= SURROGATE_HIGH_START) && (ch <= SURROGATE_HIGH_END))
                {
lowsurogate:
                    if (mySourceIndex < sourceLength)
                    {
                        ch2 = mySource[mySourceIndex];
                        if ((ch2 >= SURROGATE_LOW_START) && (ch2 <= SURROGATE_LOW_END))
                        {
                            ch = ((ch - SURROGATE_HIGH_START) << HALF_SHIFT) + (ch2 - SURROGATE_LOW_START) + HALF_BASE;
                            mySourceIndex++;
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
                    bytesToWrite = 3;
                    temp[0] = (char) ((ch >> 12) | 0xe0);
                    temp[1] = (char) ((ch >> 6) & 0x3f | 0x80);
                    temp[2] = (char) (ch & 0x3f | 0x80);
                }
                else
                {
                    bytesToWrite = 4;
                    temp[0] = (char) ((ch >> 18) | 0xf0);
                    temp[1] = (char) ((ch >> 12) & 0x3f | 0x80);
                    temp[2] = (char) ((ch >> 6) & 0x3f | 0x80);
                    temp[3] = (char) (ch & 0x3f | 0x80);
                }
                for (i = 0; i < bytesToWrite; i++)
                {
                    if (myTargetIndex < targetLength)
                    {
                        myOffsets[myTargetIndex] = mySourceIndex - bytesToWrite + 2;
                        myTarget[myTargetIndex++] = temp[i];
                    }
                    else
                    {
                        args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = temp[i];
                        *err = U_INDEX_OUTOFBOUNDS_ERROR;
                    }
                }
            }
        }
        else
        {
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
            break;
        }
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
}

U_CFUNC UChar32 T_UConverter_getNextUChar_UTF8(UConverterToUnicodeArgs *args,
                                               UErrorCode* err)
{
    /*safe keeps a ptr to the beginning in case we need to step back*/
    char const *sourceInitial = args->source;
    uint16_t extraBytesToWrite;
    uint8_t myByte;
    UChar32 ch;
    int8_t isLegalSequence = 1;

    /*Input boundary check*/
    if (args->source >= args->sourceLimit) 
    {
        *err = U_INDEX_OUTOFBOUNDS_ERROR;
        return 0xffff;
    }

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

    if (isLegalSequence)
        return ch; /* return the code point */

CALL_ERROR_FUNCTION:
    {
    UChar myUChar = (UChar)0xffff; /* ### TODO: this is a hack until we prepare the callbacks for code points */
    UChar* myUCharPtr = &myUChar;
    
    *err = U_ILLEGAL_CHAR_FOUND;
    
    /*It is very likely that the ErrorFunctor will write to the
     *internal buffers */
    args->target = myUCharPtr;
    args->targetLimit = myUCharPtr + 1;
    args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                    args,
                                    sourceInitial,
                                    args->source-sourceInitial,
                                    UCNV_ILLEGAL,
                                    err);

    /*makes the internal caching transparent to the user*/
    if (*err == U_INDEX_OUTOFBOUNDS_ERROR)
        *err = U_ZERO_ERROR;

    return (UChar32)myUChar;
    }
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

    NULL
};


const UConverterStaticData _UTF8StaticData={
  sizeof(UConverterStaticData),
"UTF8",
    1208, UCNV_IBM, UCNV_UTF8, 1, 4,
    3, { 0xef, 0xbf, 0xbd, 0 }
};


const UConverterSharedData _UTF8Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF8StaticData, FALSE, &_UTF8Impl, 
    0
};

/* UTF-16BE ----------------------------------------------------------------- */

U_CFUNC void T_UConverter_toUnicode_UTF16_BE (UConverterToUnicodeArgs * args,
                                      UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - myTarget;
    int32_t sourceLength = args->sourceLimit - (char *) mySource;
    UChar mySourceChar = 0x0000;
    UChar oldmySourceChar = 0x0000;

    while (mySourceIndex < sourceLength)
    {
        if (myTargetIndex < targetLength)
        {
            /*gets the corresponding UChar */
            mySourceChar = (unsigned char) mySource[mySourceIndex++];
            oldmySourceChar = mySourceChar;
            if (args->converter->toUnicodeStatus == 0)
            {
                args->converter->toUnicodeStatus = 
                    (unsigned char) mySourceChar == 0 ? 0xFFFF : mySourceChar;
            }
            else
            {
                if (args->converter->toUnicodeStatus != 0xFFFF)
                    mySourceChar = (UChar) ((args->converter->toUnicodeStatus << 8) | mySourceChar);
                args->converter->toUnicodeStatus = 0;

                myTarget[myTargetIndex++] = mySourceChar;
            }
        }
        else
        {
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
            break;
        }
    }

    if (U_SUCCESS(*err) && args->flush
      && (mySourceIndex == sourceLength)
      && (args->converter->toUnicodeStatus != 0x00))
    {
        if (U_SUCCESS(*err)) 
        {
            *err = U_TRUNCATED_CHAR_FOUND;
            args->converter->toUnicodeStatus = 0x00;
        }
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
}

U_CFUNC void  T_UConverter_fromUnicode_UTF16_BE (UConverterFromUnicodeArgs * args,
                                         UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - (char *) myTarget;
    int32_t sourceLength = args->sourceLimit - mySource;
    UChar mySourceChar;

    /*writing the char to the output stream */
    while (mySourceIndex < sourceLength)
    {
        if (myTargetIndex < targetLength)
        {
            mySourceChar = (UChar) mySource[mySourceIndex++];
            myTarget[myTargetIndex++] = (char) (mySourceChar >> 8);
            if (myTargetIndex < targetLength)
            {
                myTarget[myTargetIndex++] = (char) mySourceChar;
            }
            else
            {
                args->converter->charErrorBuffer[0] = (char) mySourceChar;
                args->converter->charErrorBufferLength = 1;
                *err = U_INDEX_OUTOFBOUNDS_ERROR;
            }
        }
        else
        {
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
            break;
        }
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
}

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
    first = ((uint16_t)(*(args->source)) << 8) |((uint8_t)*((args->source)+1));
    myUChar = first;
    args->source += 2;

    if(UTF_IS_FIRST_SURROGATE(first)) {
        uint16_t second;

        if (args->source+2 > args->sourceLimit) {
            *err = U_TRUNCATED_CHAR_FOUND;
            return 0xffff;
        }

        /* get the second surrogate and assemble the code point */
        second = ((uint16_t)(*(args->source)) << 8) |((uint8_t)*(args->source+1));

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

    T_UConverter_toUnicode_UTF16_BE,
    NULL,
    T_UConverter_fromUnicode_UTF16_BE,
    NULL,
    T_UConverter_getNextUChar_UTF16_BE,

    NULL
};

const UConverterStaticData _UTF16BEStaticData={
  sizeof(UConverterStaticData),
"UTF16_BigEndian",
    1200, UCNV_IBM, UCNV_UTF16_BigEndian, 2, 2,
    2, { 0xff, 0xfd, 0, 0 }
};


const UConverterSharedData _UTF16BEData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16BEStaticData, FALSE, &_UTF16BEImpl, 
    0
};

/* UTF-16LE ----------------------------------------------------------------- */

U_CFUNC void  T_UConverter_toUnicode_UTF16_LE (UConverterToUnicodeArgs * args,
                                       UErrorCode * err)
{
    const unsigned char *mySource = (unsigned char *) args->source;
    UChar *myTarget = args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - myTarget;
    int32_t sourceLength = args->sourceLimit - (char *) mySource;
    UChar mySourceChar = 0x0000;

    while (mySourceIndex < sourceLength)
    {
        if (myTargetIndex < targetLength)
        {
            /*gets the corresponding UniChar */
            mySourceChar = (unsigned char) mySource[mySourceIndex++];

            if (args->converter->toUnicodeStatus == 0x00)
            {
                args->converter->toUnicodeStatus = (unsigned char) mySourceChar == 0x00 ? 0xFFFF : mySourceChar;
            }
            else
            {
                if (args->converter->toUnicodeStatus == 0xFFFF) {
                    mySourceChar = (UChar) (mySourceChar << 8);
                }
                else
                {
                    mySourceChar <<= 8;
                    mySourceChar |= (UChar) (args->converter->toUnicodeStatus);
                }
                args->converter->toUnicodeStatus = 0x00;
                myTarget[myTargetIndex++] = mySourceChar;
            }
        }
        else
        {
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
            break;
        }
    }


    if (U_SUCCESS(*err) && args->flush
      && (mySourceIndex == sourceLength)
      && (args->converter->toUnicodeStatus != 0x00))
    {
        if (U_SUCCESS(*err)) 
        {
          *err = U_TRUNCATED_CHAR_FOUND; 
          args->converter->toUnicodeStatus = 0x00;
        }
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
}

U_CFUNC void   T_UConverter_fromUnicode_UTF16_LE (UConverterFromUnicodeArgs * args,
                                          UErrorCode * err)
{
    const UChar *mySource = args->source;
    unsigned char *myTarget = (unsigned char *) args->target;
    int32_t mySourceIndex = 0;
    int32_t myTargetIndex = 0;
    int32_t targetLength = args->targetLimit - (char *) myTarget;
    int32_t sourceLength = args->sourceLimit - mySource;
    UChar mySourceChar;

    /*writing the char to the output stream */
    while (mySourceIndex < sourceLength)
    {
        if (myTargetIndex < targetLength)
        {
            mySourceChar = (UChar) mySource[mySourceIndex++];
            myTarget[myTargetIndex++] = (char) mySourceChar;
            if (myTargetIndex < targetLength)
            {
                myTarget[myTargetIndex++] = (char) (mySourceChar >> 8);
            }
            else
            {
                args->converter->charErrorBuffer[0] = (char) (mySourceChar >> 8);
                args->converter->charErrorBufferLength = 1;
                *err = U_INDEX_OUTOFBOUNDS_ERROR;
            }
        }
        else
        {
            *err = U_INDEX_OUTOFBOUNDS_ERROR;
            break;
        }
    }

    args->target += myTargetIndex;
    args->source += mySourceIndex;
}

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
    first =  ((uint16_t)*((args->source)+1) << 8) | ((uint8_t)(*(args->source)));
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
        second = ((uint16_t)*(args->source+1) << 8) |((uint8_t)(*(args->source)));

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

    T_UConverter_toUnicode_UTF16_LE,
    NULL,
    T_UConverter_fromUnicode_UTF16_LE,
    NULL,
    T_UConverter_getNextUChar_UTF16_LE,

    NULL
};


const UConverterStaticData _UTF16LEStaticData={
    sizeof(UConverterStaticData),
    "UTF16_LittleEndian",
    1200, UCNV_IBM, UCNV_UTF16_LittleEndian, 2, 2,
    2, { 0xfd, 0xff, 0, 0 }
};


const UConverterSharedData _UTF16LEData={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_UTF16LEStaticData, FALSE, &_UTF16LEImpl, 
    0
};
