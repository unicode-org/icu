/*
******************************************************************************
*
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
******************************************************************************
*
*   uconv_cnv.c:
*   Implements all the low level conversion functions
*   T_UnicodeConverter_{to,from}Unicode_$ConversionType
*
*   Change history:
*
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#include "unicode/utypes.h"
#include "unicode/ucnv_err.h"
#include "unicode/ucnv.h"
#include "unicode/uset.h"
#include "ucnv_cnv.h"
#include "cmemory.h"

/*Empties the internal unicode output buffer */
void  ucnv_flushInternalUnicodeBuffer (UConverter * _this,
                                  UChar * myTarget,
                                  int32_t * myTargetIndex,
                                  int32_t targetLength,
                                  int32_t** offsets,
                                  UErrorCode * err)
{
    int32_t myUCharErrorBufferLength = _this->UCharErrorBufferLength;
    
    if (myUCharErrorBufferLength <= targetLength)
    {
        /*we have enough space
        *So we just copy the whole Error Buffer in to the output stream
        */
        uprv_memcpy (myTarget,
            _this->UCharErrorBuffer,
            sizeof (UChar) * myUCharErrorBufferLength);
        if (offsets) 
        {
            int32_t i=0;
            for (i=0; i<myUCharErrorBufferLength;i++) (*offsets)[i] = -1; 
            *offsets += myUCharErrorBufferLength;
        }
        *myTargetIndex += myUCharErrorBufferLength;
        _this->UCharErrorBufferLength = 0;
    }
    else
    {
        /* We don't have enough space so we copy as much as we can
        * on the output stream and update the object
        * by updating the internal buffer*/
        uprv_memcpy (myTarget, _this->UCharErrorBuffer, sizeof (UChar) * targetLength);
        if (offsets) 
        {
            int32_t i=0;
            for (i=0; i< targetLength;i++) (*offsets)[i] = -1; 
            *offsets += targetLength;
        }
        uprv_memmove (_this->UCharErrorBuffer,
                    _this->UCharErrorBuffer + targetLength,
                    sizeof (UChar) * (myUCharErrorBufferLength - targetLength));
        _this->UCharErrorBufferLength -= (int8_t) targetLength;
        *myTargetIndex = targetLength;
        *err = U_BUFFER_OVERFLOW_ERROR;
    }
}

/*Empties the internal codepage output buffer */
void  ucnv_flushInternalCharBuffer (UConverter * _this,
                               char *myTarget,
                               int32_t * myTargetIndex,
                               int32_t targetLength,
                               int32_t** offsets,
                               UErrorCode * err)
{
    int32_t myCharErrorBufferLength = _this->charErrorBufferLength;
    
    /*we have enough space */
    if (myCharErrorBufferLength <= targetLength)
    {
        uprv_memcpy (myTarget, _this->charErrorBuffer, myCharErrorBufferLength);
        if (offsets) 
        {
            int32_t i=0;
            for (i=0; i<myCharErrorBufferLength;i++) (*offsets)[i] = -1; 
            *offsets += myCharErrorBufferLength;
        }
        
        *myTargetIndex += myCharErrorBufferLength;
        _this->charErrorBufferLength = 0;
    }
    else
    {
        /* We don't have enough space so we copy as much as we can
        * on the output stream and update the object
        */
        uprv_memcpy (myTarget, _this->charErrorBuffer, targetLength);
        if (offsets) 
        {
            int32_t i=0;
            for (i=0; i< targetLength;i++) (*offsets)[i] = -1; 
            *offsets += targetLength;
        }
        uprv_memmove (_this->charErrorBuffer,
            _this->charErrorBuffer + targetLength,
            (myCharErrorBufferLength - targetLength));
        _this->charErrorBufferLength -= (int8_t) targetLength;
        *myTargetIndex = targetLength;
        *err = U_BUFFER_OVERFLOW_ERROR;
    }
}

/**
 * This function is useful for implementations of getNextUChar().
 * After a call to a callback function or to toUnicode(), an output buffer
 * begins with a Unicode code point that needs to be returned as UChar32,
 * and all following code units must be prepended to the - potentially
 * prefilled - overflow buffer in the UConverter.
 * The buffer should be at least of capacity UTF_MAX_CHAR_LENGTH so that a
 * complete UChar32's UChars fit into it.
 *
 * @param cnv    The converter that will get remaining UChars copied to its overflow area.
 * @param buffer An array of UChars that was passed into a callback function
 *               or a toUnicode() function.
 * @param length The number of code units (UChars) that are actually in the buffer.
 *               This must be >0.
 * @return The code point from the first UChars in the buffer.
 */
U_CFUNC UChar32
ucnv_getUChar32KeepOverflow(UConverter *cnv, const UChar *buffer, int32_t length) {
    UChar32 c;
    int32_t i;

    if(length<=0) {
        return 0xffff;
    }

    /* get the first code point in the buffer */
    i=0;
    UTF_NEXT_CHAR(buffer, i, length, c);
    if(i<length) {
        /* there are UChars left in the buffer that need to go into the overflow buffer */
        UChar *overflow=cnv->UCharErrorBuffer;
        int32_t j=cnv->UCharErrorBufferLength;

        if(j>0) {
            /* move the overflow buffer contents to make room for the extra UChars */
            int32_t k;

            cnv->UCharErrorBufferLength=(int8_t)(k=(length-i)+j);
            do {
                overflow[--k]=overflow[--j];
            } while(j>0);
        } else {
            cnv->UCharErrorBufferLength=(int8_t)(length-i);
        }

        /* copy the remaining UChars to the beginning of the overflow buffer */
        do {
            overflow[j++]=buffer[i++];
        } while(i<length);
    }
    return c;
}

/* update target offsets after a callback call */
U_CFUNC int32_t *
ucnv_updateCallbackOffsets(int32_t *offsets, int32_t length, int32_t sourceIndex) {
    if(offsets!=NULL) {
        if(sourceIndex>=0) {
            /* add the sourceIndex to the relative offsets that the callback wrote */
            while(length>0) {
                *offsets+=sourceIndex;
                ++offsets;
                --length;
            }
        } else {
            /* sourceIndex==-1, set -1 offsets */
            while(length>0) {
                *offsets=-1;
                ++offsets;
                --length;
            }
        }
        return offsets;
    } else {
        return NULL;
    }
}

/*
 * This is a simple implementation of ucnv_getNextUChar() that uses the
 * converter's toUnicode() function. See ucnv_cnv.h for details.
 */
U_CFUNC UChar32
ucnv_getNextUCharFromToUImpl(UConverterToUnicodeArgs *pArgs,
                             T_ToUnicodeFunction toU,
                             UBool collectPairs,
                             UErrorCode *pErrorCode) {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    const char *realLimit=pArgs->sourceLimit;

    pArgs->target=buffer;
    pArgs->targetLimit=buffer+UTF_MAX_CHAR_LENGTH;

    while(pArgs->source<realLimit) {
        /* feed in one byte at a time to make sure to get only one character out */
        pArgs->sourceLimit=pArgs->source+1;
        pArgs->flush= (UBool)(pArgs->sourceLimit==realLimit);

        /* convert this byte and check the result */
        toU(pArgs, pErrorCode);
        if(U_SUCCESS(*pErrorCode)) {
            int32_t length=(int32_t)(pArgs->target-buffer);

            /* this test is UTF-16 specific */
            if(/* some output and
                  (source consumed or don't collect surrogate pairs or not a surrogate or a surrogate pair) */
               length>0 &&
               (pArgs->flush || !collectPairs || !UTF_IS_FIRST_SURROGATE(buffer[0]) || length==2)
            ) {
                return ucnv_getUChar32KeepOverflow(pArgs->converter, buffer, length);
            }
            /* else continue with the loop */
        } else if(*pErrorCode==U_BUFFER_OVERFLOW_ERROR) {
            *pErrorCode=U_ZERO_ERROR;
            return ucnv_getUChar32KeepOverflow(pArgs->converter, buffer, UTF_MAX_CHAR_LENGTH);
        } else {
            /* U_FAILURE() */
            return 0xffff;
        }
    }

    /* no output because of empty input or only state changes and skipping callbacks */
    *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
    return 0xffff;
}

U_CFUNC void
ucnv_getCompleteUnicodeSet(const UConverter *cnv,
                   USet *set,
                   UConverterUnicodeSet which,
                   UErrorCode *pErrorCode) {
    uset_addRange(set, 0, 0x10ffff);
}

U_CFUNC void
ucnv_getNonSurrogateUnicodeSet(const UConverter *cnv,
                               USet *set,
                               UConverterUnicodeSet which,
                               UErrorCode *pErrorCode) {
    uset_addRange(set, 0, 0xd7ff);
    uset_addRange(set, 0xe000, 0x10ffff);
}
