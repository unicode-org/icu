/*
*******************************************************************************
*                                                                             *
* COPYRIGHT:                                                                  *
*   (C) Copyright International Business Machines Corporation, 1998           *
*   Licensed Material - Program-Property of IBM - All Rights Reserved.        *
*   US Government Users Restricted Rights - Use, duplication, or disclosure   *
*   restricted by GSA ADP Schedule Contract with IBM Corp.                    *
*                                                                             *
*******************************************************************************
*
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

#include "utypes.h"

#ifndef UHASH_H
typedef struct _UHashtable UHashtable;
#endif

#ifndef UCMP16_H
typedef struct _CompactShortArray CompactShortArray;
#endif

#ifndef UCMP8_H
typedef struct _CompactByteArray CompactByteArray;
#endif


#define MAX_SUBCHAR_LEN 4
#define ERROR_BUFFER_LENGTH 20

#define IMPLEMENTED_CONVERSION_TYPES 9
/*Sentinel Value used to check the integrity of the binary data files */

#define FILE_CHECK_MARKER 0xBEDA

extern const char* COPYRIGHT_STRING;
extern const int32_t COPYRIGHT_STRING_LENGTH;

#define COPYRIGHT_STRING " * COPYRIGHT:                                                                   *\n *   (C) Copyright International Business Machines Corporation, 1999            *\n"

#define COPYRIGHT_STRING_LENGTH  200
/*maximum length of the converter names */
#define MAX_CONVERTER_NAME_LENGTH 60
#define MAX_FULL_FILE_NAME_LENGTH 600+MAX_CONVERTER_NAME_LENGTH

/*Pointer to the aforementionned file */
#define MAX_LINE_TEXT MAX_CONVERTER_NAME_LENGTH*400

#define  UCNV_SI 0x0F		/*Shift in for EBDCDIC_STATEFUL and iso2022 states */
#define  UCNV_SO 0x0E		/*Shift out for EBDCDIC_STATEFUL and iso2022 states */

typedef enum
{
  UNSUPPORTED_CONVERTER = -1,
  SBCS = 0,
  DBCS = 1,
  MBCS = 2,
  LATIN_1 = 3,
  UTF8 = 4,
  UTF16_BigEndian = 5,
  UTF16_LittleEndian = 6,
  EBCDIC_STATEFUL = 7,
  ISO_2022 = 8,
  JIS = 9,
  EUC = 10,
  GB = 11
}
UCNV_TYPE;

/*Number of converters types for which we have conversion routines */
#define NUMBER_OF_SUPPORTED_CONVERTER_TYPES 9

#ifdef UNKNOWN
#undef UNKNOWN
#endif
typedef enum {UNKNOWN = -1, IBM = 0}
UCNV_PLATFORM;


/*Table Node Definitions */
typedef struct
  {
    UChar toUnicode[256];
    CompactByteArray *fromUnicode;
  }
SBCS_TABLE;

typedef struct
  {
    CompactShortArray *toUnicode;
    CompactShortArray *fromUnicode;
  }
DBCS_TABLE;

typedef struct
  {
    bool_t starters[256];
    CompactShortArray *toUnicode;
    CompactShortArray *fromUnicode;
  }
MBCS_TABLE;

typedef union
  {
    SBCS_TABLE sbcs;
    DBCS_TABLE dbcs;
    MBCS_TABLE mbcs;
  }
ConverterTable;


/*Defines the struct of a UConverterSharedData the immutable, shared part of
 *UConverter
 */
typedef struct
  {
    uint32_t referenceCounter;	/*used to count number of clients */
    char name[MAX_CONVERTER_NAME_LENGTH];	/*internal name of the converter */
    UCNV_PLATFORM platform;	/*platform of the converter (only IBM now */
    int32_t codepage;		/*codepage # (now IBM-$codepage) */
    UCNV_TYPE conversionType;	/*conversion type */
    int8_t minBytesPerChar;	/*Minimum # bytes per char in this codepage */
    int8_t maxBytesPerChar;	/*Maximum # bytes per char in this codepage */
    struct
      {				/*initial values of some members of the mutable part of object */
	uint32_t toUnicodeStatus;
	int8_t subCharLen;
	unsigned char subChar[MAX_SUBCHAR_LEN];
      }
    defaultConverterValues;
    ConverterTable *table;	/*Pointer to conversion data */
  }
UConverterSharedData;


/*Defines a UConverter, the lightweight mutable part the user sees */
struct UConverter
  {
    int32_t toUnicodeStatus;	/*Used to internalize stream status information */
    int32_t fromUnicodeStatus;
    int8_t invalidCharLength;
    int8_t invalidUCharLength;
    int8_t pad;
    int32_t mode;
    int8_t subCharLen;		/*length of the codepage specific character sequence */
    unsigned char subChar[MAX_SUBCHAR_LEN];	/*codepage specific character sequence */
    UChar UCharErrorBuffer[ERROR_BUFFER_LENGTH];	/*used to store unicode data meant for 
							   *output stream  by the Error function pointers 
							 */
    unsigned char charErrorBuffer[ERROR_BUFFER_LENGTH];	/*used to store codepage data meant for
							   * output stream by the Error function pointers 
							 */
    int8_t UCharErrorBufferLength;	/*used to indicate the number of valid UChars
					   *in charErrorBuffer
					 */
    int8_t charErrorBufferLength;	/*used to indicate the number of valid bytes
					   *in charErrorBuffer
					 */

    UChar invalidUCharBuffer[3];
    char invalidCharBuffer[MAX_SUBCHAR_LEN];
    /*Error function pointer called when conversion issues
     *occur during a T_UConverter_fromUnicode call
     */
    void (*fromUCharErrorBehaviour) (struct UConverter *,
				     char **,
				     const char *,
				     const UChar **,
				     const UChar *,
				     int32_t* offsets,
				     bool_t,
				     UErrorCode *);
    /*Error function pointer called when conversion issues
     *occur during a T_UConverter_toUnicode call
     */
    void (*fromCharErrorBehaviour) (struct UConverter *,
				    UChar **,
				    const UChar *,
				    const char **,
				    const char *,
				    int32_t* offsets,
				    bool_t,
				    UErrorCode *);

    UConverterSharedData *sharedData;	/*Pointer to the shared immutable part of the
					 *converter object
					 */
    void *extraInfo;		/*currently only used to point to a struct containing UConverter used by iso 2022
				   Could be used by clients writing their own call back function to
				   pass context to them
				 */
  };

typedef struct UConverter UConverter;


typedef struct
  {
    UConverter *currentConverter;
    unsigned char escSeq2022[10];
    int8_t escSeq2022Length;
  }
UCNV_Data2022;

typedef void (*UCNV_ToUCallBack) (UConverter *,
				  UChar **,
				  const UChar *,
				  const char **,
				  const char *,
				  int32_t* offsets,
				  bool_t,
				  UErrorCode *);

typedef void (*UCNV_FromUCallBack) (UConverter *,
				    char **,
				    const char *,
				    const UChar **,
				    const UChar *,
				    int32_t* offsets,
				    bool_t,
				    UErrorCode *);


/*Hashtable used to store UConverterSharedData objects supporting
 *the Caching mechanism
 */
extern UHashtable *SHARED_DATA_HASHTABLE;

/*Hashtable generated (lazy eval) by algorithmicConverterNames for fast lookup
 *Behaviour is completely different for the Algorithmic conversions.
 *we use this table to as a litmus test.
 */
extern UHashtable *ALGORITHMIC_CONVERTERS_HASHTABLE;


/*Array used to generate ALGORITHMIC_CONVERTERS_HASHTABLE
 *should ALWAYS BE NULL STRING TERMINATED.
 */
static const char *algorithmicConverterNames[MAX_CONVERTER_NAME_LENGTH] = {
  "LATIN_1",
  "UTF8",
  "UTF16_BigEndian",
  "UTF16_LittleEndian",
  "ISO_2022",
  "JIS",
  "EUC",
  "GB",
  "ISO_2022",
  ""
};

/* figures out if we need to go to file to read in the data tables.
 */
UConverter *createConverter (const char *converterName, UErrorCode * err);

/* Stores the shared data in the SHARED_DATA_HASHTABLE
 */
void shareConverterData (UConverterSharedData * data);

/* gets the shared data from the SHARED_DATA_HASHTABLE (might return NULL if it isn't there)
 */
UConverterSharedData *getSharedConverterData (const char *name);

/* Deletes (frees) the Shared data it's passed. first it checks the referenceCounter to
 * see if anyone is using it, if not it frees all the memory stemming from sharedConverterData and
 * returns TRUE,
 * otherwise returns FALSE
 */
bool_t deleteSharedConverterData (UConverterSharedData * sharedConverterData);

/* returns true if "name" is in algorithmicConverterNames
 */
bool_t isDataBasedConverter (const char *name);

void copyPlatformString (char *platformString, UCNV_PLATFORM pltfrm);


#endif /* _UCNV_BLD */
