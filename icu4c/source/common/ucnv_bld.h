/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
*
*  ucnv_bld.h:
*  Contains all internal and external data structure definitions
* Created & Maitained by Bertrand A. Damiba
*
*
*
* ATTENTION:
* ---------
* Although the data structures in this file are open and stack allocatable
* we reserve the right to hide them in further releases.
*
*   Change history:
*
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#ifndef UCNV_BLD_H
#define UCNV_BLD_H

#include "unicode/utypes.h"
#include "unicode/ucnv.h"
#include "unicode/ucnv_err.h"
#include "ucnvmbcs.h"

/* size of the overflow buffers in UConverter, enough for escaping callbacks */
#define UCNV_ERROR_BUFFER_LENGTH 32

#define UCNV_MAX_SUBCHAR_LEN 4
#define UCNV_MAX_AMBIGUOUSCCSIDS 5

/* ### move the following typedef and array into implementation files! */
typedef struct
{
    int32_t ccsid;
    UChar mismapped;
    UChar replacement;
} UAmbiguousConverter;

static const UAmbiguousConverter UCNV_AMBIGUOUSCONVERTERS[UCNV_MAX_AMBIGUOUSCCSIDS] =
{
    { 943, 0x00A5, 0x005C },
    { 949, 0x20A9, 0x005C },
    { 1361, 0x20A9, 0x005C },
    { 942, 0x00A5, 0x005C },
    { 1363, 0x20A9, 0x005C }
};


U_CDECL_BEGIN /* We must declare the following as 'extern "C"' so that if ucnv
                 itself is compiled under C++, the linkage of the funcptrs will
                 work.
              */

union UConverterTable;
typedef union UConverterTable UConverterTable;

struct UConverterImpl;
typedef struct UConverterImpl UConverterImpl;

typedef struct UConverterStaticData {
    uint32_t structSize;            /* Size of this structure */
    
    char name [UCNV_MAX_CONVERTER_NAME_LENGTH];               /* internal name of the converter- invariant chars */

    int32_t codepage;               /* codepage # (now IBM-$codepage) */

    int8_t platform;                /* platform of the converter (only IBM now) */
    int8_t conversionType;          /* conversion type */

    int8_t minBytesPerChar;         /* Minimum # bytes per char in this codepage */
    int8_t maxBytesPerChar;         /* Maximum # bytes per char in this codepage */

    int8_t subCharLen;
  
    uint8_t subChar[UCNV_MAX_SUBCHAR_LEN]; 
    uint8_t hasToUnicodeFallback;              /* UBool needs to be changed to UBool to be consistent across platform */
    uint8_t hasFromUnicodeFallback;
    uint8_t reserved[19];  /* to round out the structure */

} UConverterStaticData;

/*
 * Defines the UConverterSharedData struct,
 * the immutable, shared part of UConverter.
 */
struct UConverterSharedData {
    uint32_t structSize;            /* Size of this structure */
    uint32_t referenceCounter;      /* used to count number of clients, 0xffffffff for static SharedData */

    const void *dataMemory;         /* from udata_openChoice() */
    UConverterTable *table;         /* Pointer to conversion data */

    const UConverterStaticData *staticData; /* pointer to the static (non changing) data. */
    UBool                staticDataOwned; /* T if we own the staticData */
    const UConverterImpl *impl;     /* vtable-style struct of mostly function pointers */

    /*initial values of some members of the mutable part of object */
    uint32_t toUnicodeStatus;
};


/* Defines a UConverter, the lightweight mutable part the user sees */

struct UConverter {
    uint32_t toUnicodeStatus;           /* Used to internalize stream status information */
    uint8_t toUBytes[7];                /* more "toU status"; keeps the bytes of the current character */
    int8_t toULength;                   /* number of bytes in toUBytes */
    uint32_t fromUnicodeStatus;
    UChar    fromUSurrogateLead;        /* similar to toUBytes; keeps the lead surrogate of the current character */
    int32_t mode;
    UBool  useFallback;

    int8_t subCharLen;                  /* length of the codepage specific character sequence */
    int8_t invalidCharLength;
    int8_t invalidUCharLength;
    int8_t charErrorBufferLength;       /* number of valid bytes in charErrorBuffer */
    int8_t UCharErrorBufferLength;      /* number of valid UChars in charErrorBuffer */

    uint8_t subChar[UCNV_MAX_SUBCHAR_LEN];              /* codepage specific character sequence */
    char invalidCharBuffer[UCNV_MAX_SUBCHAR_LEN];       /* bytes from last error/callback situation */
    uint8_t charErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];  /* codepage output from Error functions */

    UChar invalidUCharBuffer[3];                        /* UChars from last error/callback situation */
    UChar UCharErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];   /* unicode output from Error functions */

    /*
     * Error function pointer called when conversion issues
     * occur during a ucnv_fromUnicode call
     */
    void (*fromUCharErrorBehaviour) (void *context,
                                     UConverterFromUnicodeArgs *args,
                                     const UChar *codeUnits,
                                     int32_t length,
                                     UChar32 codePoint,
                                     UConverterCallbackReason reason,
                                     UErrorCode *);
    /*
     * Error function pointer called when conversion issues
     * occur during a T_UConverter_toUnicode call
     */
    void (*fromCharErrorBehaviour) (void *context,
                                    UConverterToUnicodeArgs *args,
                                    const char *codeUnits,
                                    int32_t length,
                                    UConverterCallbackReason reason,
                                    UErrorCode *);

    void *fromUContext;
    void *toUContext;
    UConverterSharedData *sharedData;   /* Pointer to the shared immutable part of the converter object */

    /*
     * currently only used to point to a struct containing UConverter used by iso 2022;
     * could be used by clients writing their own call back function to pass context to them
     */
    void *extraInfo;
};

U_CDECL_END /* end of UConverter */

typedef struct
  {
    UConverter *OptGrpConverter[0x20];    /* Converter per Opt. grp. */
    uint8_t    OptGroup;                  /* default Opt. grp. for this LMBCS session */
    uint8_t    localeConverterIndex;      /* reasonable locale match for index */

  }
UConverterDataLMBCS;

#define CONVERTER_FILE_EXTENSION ".cnv"

#endif /* _UCNV_BLD */
