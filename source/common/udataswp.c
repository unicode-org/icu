/*
*******************************************************************************
*
*   Copyright (C) 2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  udataswp.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003jun05
*   created by: Markus W. Scherer
*
*   Definitions for ICU data transformations for different platforms,
*   changing between big- and little-endian data and/or between
*   charset families (ASCII<->EBCDIC).
*/

#include <stdarg.h>
#include "unicode/utypes.h"
#include "unicode/udata.h" /* UDataInfo */
#include "ucmndata.h" /* DataHeader */
#include "cmemory.h"
#include "udataswp.h"

/* swapping primitives ------------------------------------------------------ */

/* generic noop swapper for when nothing needs to be done */
static int32_t U_CALLCONV
uprv_swapNone(const UDataSwapper *ds,
              const void *inData, int32_t length, void *outData,
              UErrorCode *pErrorCode) {
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || length<0 || outData==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    if(length>0 && inData!=outData) {
        uprv_memcpy(outData, inData, length);
    }

    return length;
}

static int32_t U_CALLCONV
uprv_swapArray16(const UDataSwapper *ds,
                 const void *inData, int32_t length, void *outData,
                 UErrorCode *pErrorCode) {
    const uint16_t *p;
    uint16_t *q;
    int32_t count;
    uint16_t x;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || length<0 || (length&1)!=0 || outData==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* setup and swapping */
    p=(const uint16_t *)inData;
    q=(uint16_t *)outData;
    count=length/2;
    while(count>0) {
        x=*p++;
        *q++=(uint16_t)((x<<8)|(x>>8));
        --count;
    }

    return length;
}

static int32_t U_CALLCONV
uprv_swapArray32(const UDataSwapper *ds,
                 const void *inData, int32_t length, void *outData,
                 UErrorCode *pErrorCode) {
    const uint32_t *p;
    uint32_t *q;
    int32_t count;
    uint32_t x;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || length<0 || (length&3)!=0 || outData==NULL) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* setup and swapping */
    p=(const uint32_t *)inData;
    q=(uint32_t *)outData;
    count=length/4;
    while(count>0) {
        x=*p++;
        *q++=(uint32_t)((x<<24)|((x<<8)&0xff0000)|((x>>8)&0xff00)|(x>>24));
        --count;
    }

    return length;
}

static uint16_t U_CALLCONV
uprv_readSwapUInt16(uint16_t x) {
    return (uint16_t)((x<<8)|(x>>8));
}

static uint16_t U_CALLCONV
uprv_readDirectUInt16(uint16_t x) {
    return x;
}

static uint32_t U_CALLCONV
uprv_readSwapUInt32(uint32_t x) {
    return (uint32_t)((x<<24)|((x<<8)&0xff0000)|((x>>8)&0xff00)|(x>>24));
}

static uint32_t U_CALLCONV
uprv_readDirectUInt32(uint32_t x) {
    return x;
}

U_CAPI int16_t U_EXPORT2
udata_readInt16(const UDataSwapper *ds, int16_t x) {
    return (int16_t)ds->readUInt16((uint16_t)x);
}

U_CAPI int32_t U_EXPORT2
udata_readInt32(const UDataSwapper *ds, int32_t x) {
    return (int32_t)ds->readUInt32((uint32_t)x);
}

U_CAPI void U_EXPORT2
udata_printError(const UDataSwapper *ds,
                 const char *fmt,
                 ...) {
    va_list args;

    if(ds->printError!=NULL) {
        va_start(args, fmt);
        ds->printError(ds->printErrorContext, fmt, args);
        va_end(args);
    }
}

/* swap a data header ------------------------------------------------------- */

U_CAPI int32_t U_EXPORT2
udata_swapDataHeader(const UDataSwapper *ds,
                     const void *inData, int32_t length, void *outData,
                     UErrorCode *pErrorCode) {
    const DataHeader *pHeader;
    uint16_t headerSize, infoSize;

    /* argument checking */
    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return 0;
    }
    if(ds==NULL || inData==NULL || length<-1 || (length>0 && outData==NULL)) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return 0;
    }

    /* check minimum length and magic bytes */
    pHeader=(const DataHeader *)inData;
    if( (length>=0 && length<sizeof(DataHeader)) ||
        pHeader->dataHeader.magic1!=0xda ||
        pHeader->dataHeader.magic2!=0x27 ||
        pHeader->info.sizeofUChar!=2
    ) {
        *pErrorCode=U_UNSUPPORTED_ERROR;
        return 0;
    }

    headerSize=ds->readUInt16(pHeader->dataHeader.headerSize);
    infoSize=ds->readUInt16(pHeader->info.size);

    if( headerSize<sizeof(DataHeader) ||
        infoSize<sizeof(UDataInfo) ||
        headerSize<(sizeof(pHeader->dataHeader)+infoSize) ||
        (length>=0 && length<headerSize)
    ) {
        *pErrorCode=U_INDEX_OUTOFBOUNDS_ERROR;
        return 0;
    }

    if(length>0) {
        DataHeader *outHeader;
        const char *s;
        int32_t maxLength;

        /* Most of the fields are just bytes and need no swapping. */
        if(inData!=outData) {
            uprv_memcpy(outData, inData, headerSize);
        }
        outHeader=(DataHeader *)outData;

        /* swap headerSize */
        ds->swapArray16(ds, &pHeader->dataHeader.headerSize, 2, &outHeader->dataHeader.headerSize, pErrorCode);

        /* swap UDataInfo size and reservedWord */
        ds->swapArray16(ds, &pHeader->info.size, 4, &outHeader->info.size, pErrorCode);

        /* swap copyright statement after the UDataInfo */
        infoSize+=sizeof(pHeader->dataHeader);
        s=(const char *)inData+infoSize;
        maxLength=headerSize-infoSize;
        /* get the length of the string */
        for(length=0; length<maxLength && s[length]!=0; ++length) {}
        /* swap the string contents */
        ds->swapInvChars(ds, s, length, (char *)outData+infoSize, pErrorCode);
    }

    return headerSize;
}

/* API functions ------------------------------------------------------------ */

U_CAPI UDataSwapper * U_EXPORT2
udata_openSwapper(UBool inIsBigEndian, uint8_t inCharset,
                  UBool outIsBigEndian, uint8_t outCharset,
                  UErrorCode *pErrorCode) {
    UDataSwapper *swapper;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    if(inCharset>U_EBCDIC_FAMILY || outCharset>U_EBCDIC_FAMILY) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    /* allocate the swapper */
    swapper=uprv_malloc(sizeof(UDataSwapper));
    if(swapper==NULL) {
        *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }
    uprv_memset(swapper, 0, sizeof(UDataSwapper));

    /* set values and functions pointers according to in/out parameters */
    swapper->inIsBigEndian=inIsBigEndian;
    swapper->inCharset=inCharset;
    swapper->outIsBigEndian=outIsBigEndian;
    swapper->outCharset=outCharset;

    swapper->readUInt16= inIsBigEndian==U_IS_BIG_ENDIAN ? uprv_readDirectUInt16 : uprv_readSwapUInt16;
    swapper->readUInt32= inIsBigEndian==U_IS_BIG_ENDIAN ? uprv_readDirectUInt32 : uprv_readSwapUInt32;

    swapper->compareInvChars= inCharset==U_ASCII_FAMILY ? uprv_compareInvAscii : uprv_compareInvEbcdic;

    swapper->swapArray16= inIsBigEndian==outIsBigEndian ? uprv_swapNone : uprv_swapArray16;
    swapper->swapArray32= inIsBigEndian==outIsBigEndian ? uprv_swapNone : uprv_swapArray32;

    swapper->swapInvChars=
        inCharset==outCharset ? uprv_swapNone :
        inCharset==U_ASCII_FAMILY ? uprv_ebcdicFromAscii :
        uprv_asciiFromEbcdic;

    return swapper;
}

U_CAPI UDataSwapper * U_EXPORT2
udata_openSwapperForInputData(const void *data, int32_t length,
                              UBool outIsBigEndian, uint8_t outCharset,
                              UErrorCode *pErrorCode) {
    const DataHeader *pHeader;
    uint16_t headerSize, infoSize;
    UBool inIsBigEndian;
    int8_t inCharset;

    if(pErrorCode==NULL || U_FAILURE(*pErrorCode)) {
        return NULL;
    }
    if( data==NULL ||
        (length>=0 && length<sizeof(DataHeader)) ||
        outCharset>U_EBCDIC_FAMILY
    ) {
        *pErrorCode=U_ILLEGAL_ARGUMENT_ERROR;
        return NULL;
    }

    pHeader=(const DataHeader *)data;
    if( (length>=0 && length<sizeof(DataHeader)) ||
        pHeader->dataHeader.magic1!=0xda ||
        pHeader->dataHeader.magic2!=0x27 ||
        pHeader->info.sizeofUChar!=2
    ) {
        *pErrorCode=U_UNSUPPORTED_ERROR;
        return 0;
    }

    inIsBigEndian=(UBool)pHeader->info.isBigEndian;
    inCharset=pHeader->info.charsetFamily;

    if(inIsBigEndian==U_IS_BIG_ENDIAN) {
        headerSize=pHeader->dataHeader.headerSize;
        infoSize=pHeader->info.size;
    } else {
        headerSize=uprv_readSwapUInt16(pHeader->dataHeader.headerSize);
        infoSize=uprv_readSwapUInt16(pHeader->info.size);
    }

    if( headerSize<sizeof(DataHeader) ||
        infoSize<sizeof(UDataInfo) ||
        headerSize<(sizeof(pHeader->dataHeader)+infoSize) ||
        (length>=0 && length<headerSize)
    ) {
        *pErrorCode=U_UNSUPPORTED_ERROR;
        return 0;
    }

    return udata_openSwapper(inIsBigEndian, inCharset, outIsBigEndian, outCharset, pErrorCode);
}

U_CAPI void U_EXPORT2
udata_closeSwapper(UDataSwapper *ds) {
    uprv_free(ds);
}
