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

U_CFUNC void
ucnv_fromUWriteBytes(UConverter *cnv,
                     const char *bytes, int32_t length,
                     char **target, const char *targetLimit,
                     int32_t **offsets,
                     int32_t sourceIndex,
                     UErrorCode *pErrorCode) {
    char *t=*target;
    int32_t *o;

    /* write bytes */
    if(offsets==NULL || (o=*offsets)==NULL) {
        while(length>0 && t<targetLimit) {
            *t++=*bytes++;
            --length;
        }
    } else {
        /* output with offsets */
        while(length>0 && t<targetLimit) {
            *t++=*bytes++;
            *o++=sourceIndex;
            --length;
        }
        *offsets=o;
    }
    *target=t;

    /* write overflow */
    if(length>0) {
        if(cnv!=NULL) {
            t=(char *)cnv->charErrorBuffer;
            cnv->charErrorBufferLength=(int8_t)length;
            do {
                *t++=(uint8_t)*bytes++;
            } while(--length>0);
        }
        *pErrorCode=U_BUFFER_OVERFLOW_ERROR;
    }
}
