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
*/

#ifndef UCNV_BLD_H
#define UCNV_BLD_H

#include "unicode/utypes.h"

#define UCNV_MAX_SUBCHAR_LEN 4
#define UCNV_ERROR_BUFFER_LENGTH 20
#define UCNV_MAX_AMBIGUOUSCCSIDS 5

#ifndef UCMP16_H
typedef struct _CompactShortArray CompactShortArray;
#endif

#ifndef UCMP8_H
typedef struct _CompactByteArray CompactByteArray;
#endif

#define UCNV_IMPLEMENTED_CONVERSION_TYPES 9
/*Sentinel Value used to check the integrity of the binary data files */

#define UCNV_FILE_CHECK_MARKER 0xBEDA

/*maximum length of the converter names */
#define UCNV_MAX_CONVERTER_NAME_LENGTH 60
#define UCNV_MAX_FULL_FILE_NAME_LENGTH (600+UCNV_MAX_CONVERTER_NAME_LENGTH)

/*Pointer to the aforementioned file */
#define UCNV_MAX_LINE_TEXT (UCNV_MAX_CONVERTER_NAME_LENGTH*400)

#define  UCNV_SI 0x0F           /*Shift in for EBDCDIC_STATEFUL and iso2022 states */
#define  UCNV_SO 0x0E           /*Shift out for EBDCDIC_STATEFUL and iso2022 states */

typedef enum {
    UCNV_UNSUPPORTED_CONVERTER = -1,
    UCNV_SBCS = 0,
    UCNV_DBCS = 1,
    UCNV_MBCS = 2,
    UCNV_LATIN_1 = 3,
    UCNV_UTF8 = 4,
    UCNV_UTF16_BigEndian = 5,
    UCNV_UTF16_LittleEndian = 6,
    UCNV_EBCDIC_STATEFUL = 7,
    UCNV_ISO_2022 = 8,
    
    UCNV_LMBCS_1 = 9,
    UCNV_LMBCS_2, 
    UCNV_LMBCS_3,		
    UCNV_LMBCS_4,
    UCNV_LMBCS_5,
    UCNV_LMBCS_6,
    UCNV_LMBCS_8,
    UCNV_LMBCS_11,
    UCNV_LMBCS_16,
    UCNV_LMBCS_17,
    UCNV_LMBCS_18,
    UCNV_LMBCS_19,
    UCNV_LMBCS_LAST = UCNV_LMBCS_19,

    /* Number of converter types for which we have conversion routines. */
    UCNV_NUMBER_OF_SUPPORTED_CONVERTER_TYPES = UCNV_LMBCS_LAST+1
   
} UConverterType;

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

typedef enum {
    UCNV_UNKNOWN = -1,
    UCNV_IBM = 0
} UConverterPlatform;


/*Table Node Definitions */
typedef struct
  {
    UChar *toUnicode;  /* [256]; */
    CompactByteArray *fromUnicode;
  }
UConverterSBCSTable;

typedef struct
  {
    CompactShortArray *toUnicode;
    CompactShortArray *fromUnicode;
  }
UConverterDBCSTable;

typedef struct
  {
    bool_t *starters; /* [256]; */
    CompactShortArray *toUnicode;
    CompactShortArray *fromUnicode;
  }
UConverterMBCSTable;

typedef union
  {
    UConverterSBCSTable sbcs;
    UConverterDBCSTable dbcs;
    UConverterMBCSTable mbcs;
  }
UConverterTable;


U_CDECL_BEGIN /* We must declare the following as 'extern "C"' so that if ucnv
                 itself is compiled under C++, the linkage of the funcptrs will
                 work.
              */

struct UConverterImpl;
typedef struct UConverterImpl UConverterImpl;

/* ###
 * Markus Scherer on 2000feb04:
 * I have change UConverter and UConverterSharedData; there may be more changes,
 * or we may decide to roll back the structure definitions to what they were
 * before, with the additional UConverterImpl field and the new semantics for
 * referenceCounter.
 *
 * Reasons for changes: Attempt at performance improvements, especially
 * a) decrease amount of internal, implicit padding by reordering the fields
 * b) save space by storing the internal name of the converter only with a
 *    pointer instead of an array
 *
 * In addition to that, I added the UConverterImpl field for better
 * modularizing the code and making it more maintainable. It may actually
 * become slightly faster by doing this.
 *
 * I changed the UConverter.to|fromUnicodeStatus to be unsigned because
 * the defaultValues.toUnicodeStatus is unsigned, and it seemed to be a safer choice.
 *
 * Ultimately, I would prefer not to expose these definitions any more at all,
 * but this is suspect to discussions, proposals and design reviews.
 *
 * I would personally like to see more information hiding (with helper APIs),
 * useful state fields in UConverter that are reserved for the callbacks,
 * and directly included structures instead of pointers to allocated
 * memory, like for UConverterTable and its variant fields.
 *
 * Also, with the more C++-like converter implementation,
 * the conversionType does not need to be in UConverterSharedData any more:
 * it is in UConverterImpl and hardly used.
 */

/*
 * Defines the UConverterSharedData struct,
 * the immutable, shared part of UConverter.
 */
typedef struct {
    uint32_t structSize;            /* Size of this structure */
    uint32_t referenceCounter;      /* used to count number of clients, 0xffffffff for static SharedData */

    const void *dataMemory;         /* from udata_openChoice() */
    UConverterTable *table;         /* Pointer to conversion data */
    const UConverterImpl *impl;     /* vtable-style struct of mostly function pointers */
    const char *name;               /* internal name of the converter */

    int32_t codepage;               /* codepage # (now IBM-$codepage) */

    int8_t platform;                /* platform of the converter (only IBM now) */
    int8_t conversionType;          /* conversion type */

    int8_t minBytesPerChar;         /* Minimum # bytes per char in this codepage */
    int8_t maxBytesPerChar;         /* Maximum # bytes per char in this codepage */

    /*initial values of some members of the mutable part of object */
    struct {
        uint32_t toUnicodeStatus;
        int8_t subCharLen;
        uint8_t subChar[UCNV_MAX_SUBCHAR_LEN];
    } defaultConverterValues;
} UConverterSharedData;


/* Defines a UConverter, the lightweight mutable part the user sees */

struct UConverter {
    uint32_t toUnicodeStatus;           /* Used to internalize stream status information */
    uint32_t fromUnicodeStatus;
    int32_t mode;

    int8_t subCharLen;                  /* length of the codepage specific character sequence */
    int8_t invalidCharLength;
    int8_t invalidUCharLength;
    int8_t charErrorBufferLength;       /* number of valid bytes in charErrorBuffer */
    int8_t UCharErrorBufferLength;      /* number of valid UChars in charErrorBuffer */

    uint8_t subChar[UCNV_MAX_SUBCHAR_LEN];              /* codepage specific character sequence */
    char invalidCharBuffer[UCNV_MAX_SUBCHAR_LEN];
    uint8_t charErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];  /* codepage output from Error functions */

    UChar invalidUCharBuffer[3];
    UChar UCharErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];   /* unicode output from Error functions */

    /*
     * Error function pointer called when conversion issues
     * occur during a T_UConverter_fromUnicode call
     */
    void (*fromUCharErrorBehaviour) (struct UConverter *,
                                     char **,
                                     const char *,
                                     const UChar **,
                                     const UChar *,
                                     int32_t* offsets,
                                     bool_t,
                                     UErrorCode *);
    /*
     * Error function pointer called when conversion issues
     * occur during a T_UConverter_toUnicode call
     */
    void (*fromCharErrorBehaviour) (struct UConverter *,
                                    UChar **,
                                    const UChar *,
                                    const char **,
                                    const char *,
                                    int32_t* offsets,
                                    bool_t,
                                    UErrorCode *);

    UConverterSharedData *sharedData;   /* Pointer to the shared immutable part of the converter object */

    /*
     * currently only used to point to a struct containing UConverter used by iso 2022;
     * could be used by clients writing their own call back function to pass context to them
     */
    void *extraInfo;
};

U_CDECL_END /* end of UConverter */

typedef struct UConverter UConverter;


typedef struct
  {
    UConverter *currentConverter;
    uint8_t escSeq2022[10];
    int8_t escSeq2022Length;
  }
UConverterDataISO2022;


typedef struct
  {
    UConverter *OptGrpConverter[0x20];    /* Converter per Opt. grp. */
    uint8_t    OptGroup;                  /* default Opt. grp. for this LMBCS session */
    uint8_t    localeConverterIndex;      /* reasonable locale match for index */

  }
UConverterDataLMBCS;


#define CONVERTER_FILE_EXTENSION ".cnv"

#endif /* _UCNV_BLD */
