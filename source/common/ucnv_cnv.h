/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
   *
   *   uconv_cnv.h:
   *   defines all the low level conversion functions
   *   T_UnicodeConverter_{to,from}Unicode_$ConversionType
 */

#ifndef UCNV_CNV_H
#define UCNV_CNV_H

#include "unicode/utypes.h"
#include "unicode/ucnv_bld.h"

U_CDECL_BEGIN

#define missingCharMarker 0xFFFF
#define missingUCharMarker 0xFFFD

#define FromU_CALLBACK_MACRO(_this, myTarget, myTargetIndex, targetLimit, mySource, mySourceIndex, sourceLimit, offsets, flush, err) \
              if (_this->fromUCharErrorBehaviour == (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_STOP) break;\
              else \
                { \
                  char *myTargetCopy = myTarget + myTargetIndex; \
                  const UChar *mySourceCopy = mySource + mySourceIndex; \
                  /*copies current values for the ErrorFunctor to update */ \
                  /*Calls the ErrorFunctor */ \
                  _this->fromUCharErrorBehaviour (_this, \
                                                  (char **) &myTargetCopy, \
                                                  targetLimit, \
                                                  (const UChar **) &mySourceCopy, \
                                                  sourceLimit, \
                                                  offsets, \
                                                  flush, \
                                                  err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                  mySourceIndex = (mySourceCopy - mySource) ; \
                  myTargetIndex = (char*)myTargetCopy - (char*)myTarget ; \
                }

#define ToU_CALLBACK_MACRO(_this, myTarget, myTargetIndex, targetLimit, mySource, mySourceIndex, sourceLimit, offsets, flush, err) \
              if (_this->fromCharErrorBehaviour == (UConverterToUCallback) UCNV_TO_U_CALLBACK_STOP) break; \
              else \
                { \
                  UChar *myTargetCopy = myTarget + myTargetIndex; \
                  const char *mySourceCopy = mySource + mySourceIndex; \
                  /*Calls the ErrorFunctor */ \
                  _this->fromCharErrorBehaviour (_this, \
                                                 &myTargetCopy, \
                                                 targetLimit, \
                                              (const char **) &mySourceCopy, \
                                                 sourceLimit, \
                                                 offsets, \
                                                 flush, \
                                                 err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                  mySourceIndex = ((char*)mySourceCopy - (char*)mySource); \
                  myTargetIndex = (myTargetCopy - myTarget); \
                }

#define FromU_CALLBACK_OFFSETS_LOGIC_MACRO(_this, myTarget, myTargetIndex, targetLimit, mySource, mySourceIndex, sourceLimit, offsets, flush, err) \
              if (_this->fromUCharErrorBehaviour == (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_STOP) break;\
              else \
                { \
                  char *myTargetCopy = myTarget + myTargetIndex; \
                  const UChar *mySourceCopy = mySource + mySourceIndex; \
                 int32_t My_i = myTargetIndex; \
                  /*copies current values for the ErrorFunctor to update */ \
                  /*Calls the ErrorFunctor */ \
                  _this->fromUCharErrorBehaviour (_this, \
                                                  (char **) &myTargetCopy, \
                                                  targetLimit, \
                                                  (const UChar **) &mySourceCopy, \
                                                  sourceLimit, \
                                                  offsets + myTargetIndex, \
                                                  flush, \
                                                  err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                  mySourceIndex = mySourceCopy - mySource ; \
                  myTargetIndex = (char*)myTargetCopy - (char*)myTarget ; \
                  for (;My_i < myTargetIndex;My_i++) offsets[My_i] += currentOffset  ;    \
                }

#define ToU_CALLBACK_OFFSETS_LOGIC_MACRO(_this, myTarget, myTargetIndex, targetLimit, mySource, mySourceIndex, sourceLimit, offsets, flush, err) \
              if (_this->fromCharErrorBehaviour == (UConverterToUCallback) UCNV_TO_U_CALLBACK_STOP) break; \
              else \
                { \
                  UChar *myTargetCopy = myTarget + myTargetIndex; \
                  const char *mySourceCopy = mySource + mySourceIndex; \
                                  int32_t My_i = myTargetIndex; \
                              _this->fromCharErrorBehaviour (_this, \
                                                 &myTargetCopy, \
                                                 targetLimit, \
                                              (const char **) &mySourceCopy, \
                                                 sourceLimit, \
                                                 offsets + myTargetIndex, \
                                                 flush, \
                                                 err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                  mySourceIndex = (char *)mySourceCopy - (char*)mySource; \
                  myTargetIndex = ((UChar*)myTargetCopy - (UChar*)myTarget);  \
                  for (;My_i < myTargetIndex;My_i++) {offsets[My_i] += currentOffset  ;   } \
                }

typedef void (*UConverterLoad) (UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode);
typedef void (*UConverterUnload) (UConverterSharedData *sharedData);

typedef void (*UConverterOpen) (UConverter *cnv, const char *name, const char *locale, UErrorCode *pErrorCode);
typedef void (*UConverterClose) (UConverter *cnv);

typedef void (*UConverterReset) (UConverter *cnv);

typedef void (*T_ToUnicodeFunction) (UConverter *,
				     UChar **,
				     const UChar *,
				     const char **,
				     const char *,
				     int32_t* offsets,
				     bool_t,
				     UErrorCode *);

typedef void (*T_FromUnicodeFunction) (UConverter *,
				       char **,
				       const char *,
				       const UChar **,
				       const UChar *,
				       int32_t* offsets,
				       bool_t,
				       UErrorCode *);

typedef UChar (*T_GetNextUCharFunction) (UConverter *,
					 const char **,
					 const char *,
					 UErrorCode *);

bool_t CONVERSION_U_SUCCESS (UErrorCode err);

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
};

extern UConverterSharedData
    _SBCSData, _DBCSData, _MBCSData, _Latin1Data,
    _UTF8Data, _UTF16BEData, _UTF16LEData, _EBCDICStatefulData,
    _ISO2022Data;

U_CDECL_END

#endif /* UCNV_CNV */
