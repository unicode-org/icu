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

static void  T_UConverter_toUnicode_LATIN_1 (UConverterToUnicodeArgs * args,
                                      UErrorCode * err)
{
  unsigned char *mySource = (unsigned char *)  args->source;
  UChar *myTarget = args->target;
  int32_t sourceLength = args->sourceLimit - (char *) mySource;
  int32_t readLen = 0;
  int32_t i = 0;

  /*Since there is no risk of encountering illegal Chars
   *we need to pad our latin1 chars to create Unicode codepoints
   *we need to go as far a min(targetLen, sourceLen)
   *in case we don't have enough buffer space
   *we set the error flag accordingly
   */
  if ((args->targetLimit - args->target) < sourceLength)
    {
      readLen = args->targetLimit - args->target;
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
  else
    {
      readLen = args->sourceLimit - (char *) mySource;
    }

  for (i = 0; i < readLen; i++) myTarget[i] = (UChar) mySource[i];

  args->target += i;
  args->source += i;
  return;
}

static void   T_UConverter_fromUnicode_LATIN_1 (UConverterFromUnicodeArgs * args,
                                         UErrorCode * err)
{
  const UChar *mySource = args->source;
  unsigned char *myTarget = (unsigned char *) args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - (char *) myTarget;
  int32_t sourceLength = args->sourceLimit - mySource;
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
              args->converter->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex];
              args->converter->invalidUCharLength = 1;
              if (UTF_IS_LEAD(mySource[mySourceIndex++]))
              {
                  if (mySourceIndex < sourceLength)
                  {
                      if (UTF_IS_TRAIL(mySource[mySourceIndex]))
                      {
                          args->converter->invalidUCharBuffer[1] = (UChar)mySource[mySourceIndex];
                          args->converter->invalidUCharLength++;
                          mySourceIndex++;
                      }
                      else 
                      {
                          reason = UCNV_ILLEGAL;
                      }                          
                  }
                  else if (args->flush == TRUE)
                  {
                      reason = UCNV_ILLEGAL;
                      *err = U_TRUNCATED_CHAR_FOUND;
                  } 
                  else 
                  {
                      args->converter->fromUSurrogateLead = args->converter->invalidUCharBuffer[0];
                      /* do not call the callback */
                  }
              }
              if (args->converter->fromUSurrogateLead == 0) 
              {
                  int32_t currentOffset = myTargetIndex;
                  const UChar *saveSource = args->source;
                  char *saveTarget = args->target;
                  int32_t *saveOffset = args->offsets;
                  
    /* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
                  
                  args->target = (char*)myTarget + myTargetIndex;;
                  args->source = mySource + mySourceIndex;                  

                  FromU_CALLBACK_MACRO(args->converter->fromUContext,
                                     args,
                                     args->converter->invalidUCharBuffer,
                                     args->converter->invalidUCharLength,
                                     (UChar32) (args->converter->invalidUCharLength == 2 ? 
                                         UTF16_GET_PAIR_VALUE(args->converter->invalidUCharBuffer[0], 
                                                              args->converter->invalidUCharBuffer[2]) 
                                                : args->converter->invalidUCharBuffer[0]),
                                     reason,
                                     err);
                  args->source = saveSource;
                  args->target = saveTarget;
                  args->offsets = saveOffset;
                  if (U_FAILURE (*err)) 
                  {
                      break;
                  }
                  args->converter->invalidUCharLength = 0;
              }
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

  args->target += myTargetIndex;
  args->source += mySourceIndex;;

  return;
}

static UChar32 T_UConverter_getNextUChar_LATIN_1(UConverterToUnicodeArgs* args,
                                                UErrorCode* err)
{
  
  /* Empties the internal buffers if need be
   * In this case since ErrorFunctors are never called 
   * (LATIN_1 is a subset of Unicode)
   */
  
  if (args->source+1 > args->sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }

  /* make sure that we zero-extend, not sign-extend, the byte */
  return  (UChar)(uint8_t)*(args->source++);
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
