/*
**********************************************************************
*   Copyright (C) 1999-2003, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
*
*  ucnv_bld.h:
*  Contains internal data structure definitions
* Created by Bertrand A. Damiba
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


/* size of the overflow buffers in UConverter, enough for escaping callbacks */
#define UCNV_ERROR_BUFFER_LENGTH 32

#define UCNV_MAX_SUBCHAR_LEN 4

/* converter options bits */
#define UCNV_OPTION_VERSION     0xf
#define UCNV_OPTION_SWAP_LFNL   0x10


U_CDECL_BEGIN /* We must declare the following as 'extern "C"' so that if ucnv
                 itself is compiled under C++, the linkage of the funcptrs will
                 work.
              */

union UConverterTable;
typedef union UConverterTable UConverterTable;

struct UConverterImpl;
typedef struct UConverterImpl UConverterImpl;

/** values for the unicodeMask */
#define UCNV_HAS_SUPPLEMENTARY 1
#define UCNV_HAS_SURROGATES    2

typedef struct UConverterStaticData {   /* +offset: size */
    uint32_t structSize;                /* +0: 4 Size of this structure */
    
    char name 
      [UCNV_MAX_CONVERTER_NAME_LENGTH]; /* +4: 60  internal name of the converter- invariant chars */

    int32_t codepage;               /* +64: 4 codepage # (now IBM-$codepage) */

    int8_t platform;                /* +68: 1 platform of the converter (only IBM now) */
    int8_t conversionType;          /* +69: 1 conversion type */

    int8_t minBytesPerChar;         /* +70: 1 Minimum # bytes per char in this codepage */
    int8_t maxBytesPerChar;         /* +71: 1 Maximum # bytes per char in this codepage */

    uint8_t subChar[UCNV_MAX_SUBCHAR_LEN]; /* +72: 4  [note:  4 and 8 byte boundary] */
    int8_t subCharLen;              /* +76: 1 */
    
    uint8_t hasToUnicodeFallback;   /* +77: 1 UBool needs to be changed to UBool to be consistent across platform */
    uint8_t hasFromUnicodeFallback; /* +78: 1 */
    uint8_t unicodeMask;            /* +79: 1  bit 0: has supplementary  bit 1: has single surrogates */
    uint8_t subChar1;               /* +80: 1  single-byte substitution character for IBM MBCS (0 if none) */
    uint8_t reserved[19];           /* +81: 19 to round out the structure */
                                    /* total size: 100 */
} UConverterStaticData;

/*
 * Defines the UConverterSharedData struct,
 * the immutable, shared part of UConverter.
 */
struct UConverterSharedData {
    uint32_t structSize;            /* Size of this structure */
    uint32_t referenceCounter;      /* used to count number of clients, 0xffffffff for static SharedData */

    const void *dataMemory;         /* from udata_openChoice() - for cleanup */
    UConverterTable *table;         /* Pointer to conversion data */

    const UConverterStaticData *staticData; /* pointer to the static (non changing) data. */

    UBool                sharedDataCached;   /* TRUE:  shared data is in cache, don't destroy on ucnv_close() if 0 ref.  FALSE: shared data isn't in the cache, do attempt to clean it up if the ref is 0 */
  /*UBool               staticDataOwned;   TRUE if static data owned by shared data & should be freed with it, NEVER true for udata() loaded statics. This ignored variable was removed to make space for sharedDataCached.   */

    const UConverterImpl *impl;     /* vtable-style struct of mostly function pointers */

    /*initial values of some members of the mutable part of object */
    uint32_t toUnicodeStatus;
};

typedef struct UConverterSharedData UConverterSharedData;

/* Defines a UConverter, the lightweight mutable part the user sees */

struct UConverter {
    /*
     * Error function pointer called when conversion issues
     * occur during a ucnv_fromUnicode call
     */
    void (U_EXPORT2 *fromUCharErrorBehaviour) (const void *context,
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
    void (U_EXPORT2 *fromCharErrorBehaviour) (const void *context,
                                    UConverterToUnicodeArgs *args,
                                    const char *codeUnits,
                                    int32_t length,
                                    UConverterCallbackReason reason,
                                    UErrorCode *);

    /*
     * Pointer to additional data that depends on the converter type.
     * Used by ISO 2022, SCSU, GB 18030 converters, possibly more.
     */
    void *extraInfo;

    const void *fromUContext;
    const void *toUContext;

    UConverterSharedData *sharedData;   /* Pointer to the shared immutable part of the converter object */

    uint32_t options; /* options flags from UConverterOpen, may contain additional bits */

    UBool sharedDataIsCached;  /* TRUE:  shared data is in cache, don't destroy on ucnv_close() if 0 ref.  FALSE: shared data isn't in the cache, do attempt to clean it up if the ref is 0 */
    UBool isCopyLocal;  /* TRUE if UConverter is not owned and not released in ucnv_close() (stack-allocated, safeClone(), etc.) */
    UBool isExtraLocal; /* TRUE if extraInfo is not owned and not released in ucnv_close() (stack-allocated, safeClone(), etc.) */

    UBool  useFallback;
    int8_t toULength;                   /* number of bytes in toUBytes */
    uint8_t toUBytes[7];                /* more "toU status"; keeps the bytes of the current character */
    uint32_t toUnicodeStatus;           /* Used to internalize stream status information */
    int32_t mode;
    uint32_t fromUnicodeStatus;
    UChar    fromUSurrogateLead;        /* similar to toUBytes; keeps the lead surrogate of the current character */

    int8_t subCharLen;                  /* length of the codepage specific character sequence */
    int8_t invalidCharLength;
    int8_t charErrorBufferLength;       /* number of valid bytes in charErrorBuffer */

    int8_t invalidUCharLength;
    int8_t UCharErrorBufferLength;      /* number of valid UChars in charErrorBuffer */

    uint8_t subChar1;                                   /* single-byte substitution character if different from subChar */
    uint8_t subChar[UCNV_MAX_SUBCHAR_LEN];              /* codepage specific character sequence */
    char invalidCharBuffer[UCNV_MAX_SUBCHAR_LEN];       /* bytes from last error/callback situation */
    uint8_t charErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];  /* codepage output from Error functions */

    UChar invalidUCharBuffer[3];                        /* UChars from last error/callback situation */
    UChar UCharErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];   /* unicode output from Error functions */

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
