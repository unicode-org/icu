/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
*   uconv_cnv.h:
*   defines all the low level conversion functions
*   T_UnicodeConverter_{to,from}Unicode_$ConversionType
*
* Modification History:
*
*   Date        Name        Description
*   05/09/00    helena      Added implementation to handle fallback mappings.
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#ifndef UCNV_CNV_H
#define UCNV_CNV_H

#include "unicode/utypes.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "ucnvmbcs.h"
#include "ucmp8.h"
#include "ucmp16.h"

/*Table Node Definitions */
typedef struct
  {
    UChar *toUnicode;  /* [256]; */
    CompactByteArray fromUnicode;
    UChar *toUnicodeFallback;
    CompactByteArray fromUnicodeFallback;
  }
UConverterSBCSTable;

typedef struct
  {
    CompactShortArray toUnicode;
    CompactShortArray fromUnicode;
    CompactShortArray toUnicodeFallback;
    CompactShortArray fromUnicodeFallback;
  }
UConverterDBCSTable;

union UConverterTable
  {
    UConverterSBCSTable sbcs;
    UConverterDBCSTable dbcs;
    UConverterMBCSTable mbcs;
  };


U_CDECL_BEGIN

/* this is used in fromUnicode DBCS tables as an "unassigned" marker */
#define missingCharMarker 0xFFFF

/*
 * #define missingUCharMarker 0xfffe
 *
 * there are actually two values used in toUnicode tables:
 * U+fffe "unassigned"
 * U+ffff "illegal"
 */

#define FromU_CALLBACK_MACRO(context, args, codeUnits, length, codePoint, reason, err) \
              if (args->converter->fromUCharErrorBehaviour == (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_STOP) break;\
              else \
                { \
                  /*copies current values for the ErrorFunctor to update */ \
                  /*Calls the ErrorFunctor */ \
                  args->converter->fromUCharErrorBehaviour ( context, \
                                                  args, \
                                                  codeUnits, \
                                                  length, \
                                                  codePoint, \
                                                  reason, \
                                                  err); \
                 myTargetIndex = args->target - (char*)myTarget; \
                 mySourceIndex = args->source - mySource; \
                }
/*
*/
#define ToU_CALLBACK_MACRO(context, args, codePoints, length, reason, err) \
              if (args->converter->fromCharErrorBehaviour == (UConverterToUCallback) UCNV_TO_U_CALLBACK_STOP) break; \
              else \
                { \
                  /*Calls the ErrorFunctor */ \
                  args->converter->fromCharErrorBehaviour ( \
                                                 context, \
                                                 args, \
                                                 codePoints, \
                                                 length, \
                                                 reason, \
                                                 err); \
                 myTargetIndex = args->target - myTarget; \
                 mySourceIndex = args->source - (const char*)mySource; \
                }
/*
*/
#define FromU_CALLBACK_OFFSETS_LOGIC_MACRO(context, args, codeUnits, length, codePoint, reason, err) \
              if (args->converter->fromUCharErrorBehaviour == (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_STOP) break;\
              else \
                { \
                 int32_t My_i = myTargetIndex; \
                  /*copies current values for the ErrorFunctor to update */ \
                  /*Calls the ErrorFunctor */ \
                  args->converter->fromUCharErrorBehaviour ( \
                                                 context, \
                                                 args, \
                                                 codeUnits, \
                                                 length, \
                                                 codePoint, \
                                                 reason, \
                                                 err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                 myTargetIndex = args->target - (char*)myTarget; \
                 mySourceIndex = args->source - mySource; \
                 args->offsets = saveOffsets; \
                  for (;My_i < myTargetIndex;My_i++) args->offsets[My_i] += currentOffset  ;    \
                }
/*
*/
#define ToU_CALLBACK_OFFSETS_LOGIC_MACRO(context, args, codePoints, length, reason, err) \
              if (args->converter->fromCharErrorBehaviour == (UConverterToUCallback) UCNV_TO_U_CALLBACK_STOP) break; \
              else \
                { \
                      args->converter->fromCharErrorBehaviour ( \
                                                 context, \
                                                 args, \
                                                 codePoints, \
                                                 length, \
                                                 reason, \
                                                 err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                 myTargetIndex = args->target - myTarget; \
                 mySourceIndex = args->source - (const char*)mySource; \
                 args->offsets = saveOffsets; \
                  for (;My_i < myTargetIndex;My_i++) {args->offsets[My_i] += currentOffset  ;   } \
                }


typedef void (*UConverterLoad) (UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode);
typedef void (*UConverterUnload) (UConverterSharedData *sharedData);

typedef void (*UConverterOpen) (UConverter *cnv, const char *name, const char *locale, UErrorCode *pErrorCode);
typedef void (*UConverterClose) (UConverter *cnv);

typedef void (*UConverterReset) (UConverter *cnv);

typedef void (*T_ToUnicodeFunction) (UConverterToUnicodeArgs *, UErrorCode *);

typedef void (*T_FromUnicodeFunction) (UConverterFromUnicodeArgs *, UErrorCode *);

typedef UChar32 (*T_GetNextUCharFunction) (UConverterToUnicodeArgs *, UErrorCode *);

typedef void (*UConverterGetStarters)(const UConverter* converter,
				     UBool starters[256],
				     UErrorCode *pErrorCode);

UBool CONVERSION_U_SUCCESS (UErrorCode err);

void flushInternalUnicodeBuffer (UConverter * _this,
				 UChar * myTarget,
				 int32_t * myTargetIndex,
				 int32_t targetLength,
				 int32_t** offsets,
				 UErrorCode * err);

void flushInternalCharBuffer (UConverter * _this,
			      char *myTarget,
			      int32_t * myTargetIndex,
			      int32_t targetLength,
			      int32_t** offsets,
			      UErrorCode * err);

/**
 * UConverterImpl contains all the data and functions for a converter type.
 * Its function pointers work much like a C++ vtable.
 * Many converter types need to define only a subset of the functions;
 * when a function pointer is NULL, then a default action will be performed.
 *
 * Every converter type must implement toUnicode, fromUnicode, and getNextUChar,
 * otherwise the converter may crash.
 * Every converter type that has variable-length codepage sequences should
 * also implement toUnicodeWithOffsets and fromUnicodeWithOffsets for
 * correct offset handling.
 * All other functions may or may not be implemented - it depends only on
 * whether the converter type needs them.
 *
 * When open() fails, then close() will be called, if present.
 */
struct UConverterImpl {
    UConverterType type;

    UConverterLoad load;
    UConverterUnload unload;

    UConverterOpen open;
    UConverterClose close;
    UConverterReset reset;

    T_ToUnicodeFunction toUnicode;
    T_ToUnicodeFunction toUnicodeWithOffsets;
    T_FromUnicodeFunction fromUnicode;
    T_FromUnicodeFunction fromUnicodeWithOffsets;
    T_GetNextUCharFunction getNextUChar;

    UConverterGetStarters getStarters;
};

extern const UConverterSharedData
    _SBCSData, _DBCSData, _MBCSData, _Latin1Data,
    _UTF8Data, _UTF16BEData, _UTF16LEData, _EBCDICStatefulData,
    _ISO2022Data,
    _LMBCSData1,_LMBCSData2, _LMBCSData3, _LMBCSData4, _LMBCSData5, _LMBCSData6,
    _LMBCSData8,_LMBCSData11,_LMBCSData16,_LMBCSData17,_LMBCSData18,_LMBCSData19;

U_CDECL_END

#endif /* UCNV_CNV */
