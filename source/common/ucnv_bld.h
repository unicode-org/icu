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
#include "ucmp8.h"
#include "ucmp16.h"

#define UCNV_MAX_SUBCHAR_LEN 4
#define UCNV_ERROR_BUFFER_LENGTH 20

#define UCNV_IMPLEMENTED_CONVERSION_TYPES 9
/*Sentinel Value used to check the integrity of the binary data files */

#define UCNV_FILE_CHECK_MARKER 0xBEDA

#define UCNV_COPYRIGHT_STRING \
    " * COPYRIGHT:                                                                   *\n" \
    " *   (C) Copyright International Business Machines Corporation, 1999            *\n"

#define UCNV_COPYRIGHT_STRING_LENGTH  200
/*maximum length of the converter names */
#define UCNV_MAX_CONVERTER_NAME_LENGTH 60
#define UCNV_MAX_FULL_FILE_NAME_LENGTH (600+UCNV_MAX_CONVERTER_NAME_LENGTH)

/*Pointer to the aforementioned file */
#define UCNV_MAX_LINE_TEXT (UCNV_MAX_CONVERTER_NAME_LENGTH*400)

#define  UCNV_SI 0x0F		/*Shift in for EBDCDIC_STATEFUL and iso2022 states */
#define  UCNV_SO 0x0E		/*Shift out for EBDCDIC_STATEFUL and iso2022 states */

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
    /** Number of converter types for which we have conversion routines. */
    UCNV_NUMBER_OF_SUPPORTED_CONVERTER_TYPES = 9,
    UCNV_JIS = 9,
    UCNV_EUC = 10,
    UCNV_GB = 11
} UConverterType;

typedef enum {
    UCNV_UNKNOWN = -1,
    UCNV_IBM = 0
} UConverterPlatform;


/*Table Node Definitions */
typedef struct
  {
    UChar toUnicode[256];
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
    bool_t starters[256];
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


/*Defines the struct of a UConverterSharedData the immutable, shared part of
 *UConverter
 */
typedef struct
  {
    uint32_t referenceCounter;	/*used to count number of clients */
    char name[UCNV_MAX_CONVERTER_NAME_LENGTH];	/*internal name of the converter */
    UConverterPlatform platform;	/*platform of the converter (only IBM now) */
    int32_t codepage;		/*codepage # (now IBM-$codepage) */
    UConverterType conversionType;	/*conversion type */
    int8_t minBytesPerChar;	/*Minimum # bytes per char in this codepage */
    int8_t maxBytesPerChar;	/*Maximum # bytes per char in this codepage */
    struct
      {				/*initial values of some members of the mutable part of object */
	uint32_t toUnicodeStatus;
	int8_t subCharLen;
	unsigned char subChar[UCNV_MAX_SUBCHAR_LEN];
      }
    defaultConverterValues;
    UConverterTable *table;	/*Pointer to conversion data */
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
    unsigned char subChar[UCNV_MAX_SUBCHAR_LEN];	/*codepage specific character sequence */
    UChar UCharErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];	/*used to store unicode data meant for 
							   *output stream  by the Error function pointers 
							 */
    unsigned char charErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];	/*used to store codepage data meant for
							   * output stream by the Error function pointers 
							 */
    int8_t UCharErrorBufferLength;	/*used to indicate the number of valid UChars
					   *in charErrorBuffer
					 */
    int8_t charErrorBufferLength;	/*used to indicate the number of valid bytes
					   *in charErrorBuffer
					 */

    UChar invalidUCharBuffer[3];
    char invalidCharBuffer[UCNV_MAX_SUBCHAR_LEN];
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
UConverterDataISO2022;


#endif /* _UCNV_BLD */
