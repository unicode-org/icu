/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnvlat1.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000feb07
*   created by: Markus W. Scherer
*   Change history:
*
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#include "unicode/utypes.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"

/* ISO 8859-1 --------------------------------------------------------------- */

static void  T_UConverter_toUnicode_LATIN_1 (UConverter * _this,
                                      UChar ** target,
                                      const UChar * targetLimit,
                                      const char **source,
                                      const char *sourceLimit,
                                      int32_t *offsets,
                                      UBool flush,
                                      UErrorCode * err)
{
  unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  int32_t readLen = 0;
  int32_t i = 0;

  /*Since there is no risk of encountering illegal Chars
   *we need to pad our latin1 chars to create Unicode codepoints
   *we need to go as far a min(targetLen, sourceLen)
   *in case we don't have enough buffer space
   *we set the error flag accordingly
   */
  if ((targetLimit - *target) < sourceLength)
    {
      readLen = targetLimit - *target;
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
  else
    {
      readLen = sourceLimit - (char *) mySource;
    }

  for (i = 0; i < readLen; i++) myTarget[i] = (UChar) mySource[i];

  *target += i;
  *source += i;
  return;
}

static void   T_UConverter_fromUnicode_LATIN_1 (UConverter * _this,
                                         char **target,
                                         const char *targetLimit,
                                         const UChar ** source,
                                         const UChar * sourceLimit,
                                         int32_t *offsets,
                                         UBool flush,
                                         UErrorCode * err)
{
  const UChar *mySource = *source;
  unsigned char *myTarget = (unsigned char *) *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - (char *) myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  UConverterFromUnicodeArgs args;
  UConverterCallbackReason reason;

  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {

      if (myTargetIndex < targetLength)
        {
          if (mySource[mySourceIndex] < 0x0100)
            {
              /*writes the char to the output stream */
              myTarget[myTargetIndex++] = (char) mySource[mySourceIndex++];
            }
          else
            {
              *err = U_INVALID_CHAR_FOUND;
              reason = UCNV_UNASSIGNED;
              _this->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex];
              _this->invalidUCharLength = 1;
              if (UTF_IS_LEAD(mySource[mySourceIndex++]))
              {
                  if (mySourceIndex < sourceLength)
                  {
                      if (UTF_IS_TRAIL(mySource[mySourceIndex]))
                      {
                          _this->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
                          _this->invalidUCharLength++;
                          mySourceIndex++;
                      }
                      else 
                      {
                          reason = UCNV_ILLEGAL;
                      }                          
                  }
                  else if (flush == TRUE)
                  {
                      reason = UCNV_ILLEGAL;
                      *err = U_TRUNCATED_CHAR_FOUND;
                  } 
                  else 
                  {
                      _this->fromUSurrogateLead = _this->invalidUCharBuffer[0];
                      /* do not call the callback */
                  }
              }
              if (_this->fromUSurrogateLead == 0) 
              {
                  int32_t currentOffset = myTargetIndex;
    /* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
                  args.converter = _this;
                  args.target = (char*)myTarget + myTargetIndex;;
                  args.targetLimit = targetLimit;
                  args.source = mySource + mySourceIndex;
                  args.sourceLimit = sourceLimit;
                  args.flush = flush;
                  args.offsets = offsets;
                  args.size = sizeof(args);

                  FromU_CALLBACK_MACRO(args.converter->fromUContext,
                                     args,
                                     _this->invalidUCharBuffer,
                                     _this->invalidUCharLength,
                                     (UChar32) (_this->invalidUCharLength == 2 ? 
                                         UTF16_GET_PAIR_VALUE(_this->invalidUCharBuffer[0], 
                                                              _this->invalidUCharBuffer[2]) 
                                                : _this->invalidUCharBuffer[0]),
                                     reason,
                                     err);
                  if (U_FAILURE (*err)) break;
                  _this->invalidUCharLength = 0;
              }
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

  *target += myTargetIndex;
  *source += mySourceIndex;;

  return;
}

static UChar32 T_UConverter_getNextUChar_LATIN_1(UConverter* converter,
                                                  const char** source,
                                                  const char* sourceLimit,
                                                  UErrorCode* err)
{
  
  /* Empties the internal buffers if need be
   * In this case since ErrorFunctors are never called 
   * (LATIN_1 is a subset of Unicode)
   */
  
  if ((*source)+1 > sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }

  /* make sure that we zero-extend, not sign-extend, the byte */
  return  (UChar)(uint8_t)*((*source)++);
}

static const UConverterImpl _Latin1Impl={
    UCNV_LATIN_1,

    NULL,
    NULL,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_LATIN_1,
    NULL,
    T_UConverter_fromUnicode_LATIN_1,
    NULL,
    T_UConverter_getNextUChar_LATIN_1,

    NULL
};

const UConverterStaticData _Latin1StaticData={
  sizeof(UConverterStaticData),
  "LATIN_1",
    819, UCNV_IBM, UCNV_LATIN_1, 1, 1,
  1, { 0x1a, 0, 0, 0 }
};


const UConverterSharedData _Latin1Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_Latin1StaticData, FALSE, &_Latin1Impl, 
    0
};
