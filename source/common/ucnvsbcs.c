/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnvsbcs.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000feb03
*   created by: Markus W. Scherer
*
*   Change history:
*
*   05/09/00    helena      Added implementation to handle fallback mappings.
*   06/20/2000  helena      OS/400 port changes; mostly typecast.
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#include "unicode/utypes.h"
#include "cmemory.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"

/* SBCS --------------------------------------------------------------------- */

static void
_SBCSLoad(UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode) {
    const uint8_t *oldraw = raw;
    sharedData->table->sbcs.toUnicode = (UChar *)raw;
    raw += sizeof(uint16_t)*256; oldraw = raw;
    ucmp8_initFromData(&sharedData->table->sbcs.fromUnicode, &raw, pErrorCode);
    if (sharedData->staticData->hasFromUnicodeFallback == TRUE)
    {
        if(((raw-oldraw)&3)!=0) {
            raw+=4-((raw-oldraw)&3);    /* pad to 4 */
        }
        ucmp8_initFromData(&sharedData->table->sbcs.fromUnicodeFallback, &raw, pErrorCode);    
    }
    if (sharedData->staticData->hasToUnicodeFallback == TRUE)
    {
        if(((raw-oldraw)&3)!=0) {
            raw+=4-((raw-oldraw)&3);    /* pad to 4 */
        }
        sharedData->table->sbcs.toUnicodeFallback = (UChar *)raw;
    }
}

static void
_SBCSUnload(UConverterSharedData *sharedData) {
    ucmp8_close (&sharedData->table->sbcs.fromUnicode);
    if (sharedData->staticData->hasFromUnicodeFallback == TRUE)
        ucmp8_close (&sharedData->table->sbcs.fromUnicodeFallback);
    uprv_free (sharedData->table);
}

void T_UConverter_toUnicode_SBCS (UConverterToUnicodeArgs * args,
                                  UErrorCode * err)
{
  char *mySource = (char *) args->source;
  UChar *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - myTarget;
  int32_t sourceLength = args->sourceLimit - (char *) mySource;
  UChar *myToUnicode = NULL, *myToUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  
  myToUnicode = args->converter->sharedData->table->sbcs.toUnicode;
  myToUnicodeFallback = args->converter->sharedData->table->sbcs.toUnicodeFallback;
  while (mySourceIndex < sourceLength)
    {

      /*writing the UniChar to the output stream */
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          targetUniChar = myToUnicode[(unsigned char) mySource[mySourceIndex++]];

          if (targetUniChar != missingUCharMarker)
            {
              /* writes the UniChar to the output stream */
              myTarget[myTargetIndex++] = targetUniChar;
            }
          else
            {
              if ((args->converter->useFallback == TRUE) &&
                  (args->converter->sharedData->staticData->hasToUnicodeFallback == TRUE))
              {
                  /* Look up in the fallback table first */
                  targetUniChar = myToUnicodeFallback[(unsigned char) mySource[mySourceIndex-1]];
                  if (targetUniChar != missingUCharMarker)
                  {
                      myTarget[myTargetIndex++] = targetUniChar;
                  }
              }
              if (targetUniChar == missingUCharMarker)
              {
                  const char *saveSource = args->source;
                  UChar *saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;

                  *err = U_INVALID_CHAR_FOUND;
                  args->converter->invalidCharBuffer[0] = (char) mySource[mySourceIndex - 1];
                  args->converter->invalidCharLength = 1;

                  args->target = myTarget + myTargetIndex;
                  args->source = mySource + mySourceIndex;

                  /* to do hsys: add more smarts to the codeUnits and length later */
                  ToU_CALLBACK_MACRO(args->converter->toUContext,
                                     args,
                                     args->converter->invalidCharBuffer,
                                     args->converter->invalidCharLength, 
                                     UCNV_UNASSIGNED,
                                     err);
                  /* Hsys: calculate the source and target advancement */
                  args->source = saveSource;
                  args->target = saveTarget;
                  args->offsets = saveOffsets;
                  if (U_FAILURE (*err)) break;
                  args->converter->invalidCharLength = 0;
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
  args->source += mySourceIndex;

  return;
}

void T_UConverter_fromUnicode_SBCS (UConverterFromUnicodeArgs * args,
                                 UErrorCode * err)
{
  const UChar *mySource = args->source;
  unsigned char *myTarget = (unsigned char *) args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - (char *) myTarget;
  int32_t sourceLength = args->sourceLimit - mySource;
  CompactByteArray *myFromUnicode = NULL, *myFromUnicodeFallback = NULL;
  unsigned char targetChar = 0x00;
  UConverterCallbackReason reason;

  myFromUnicode = &args->converter->sharedData->table->sbcs.fromUnicode;
  myFromUnicodeFallback = &args->converter->sharedData->table->sbcs.fromUnicodeFallback;
  /*writing the char to the output stream */
  /* HSYS : to do : finish the combining of the surrogate characters later */
  /*
  if (args->converter->fromUSurrogateLead != 0 && UTF_IS_TRAIL(mySource[mySourceIndex]))
  {
  }
  */
  while (mySourceIndex < sourceLength)
    {
      targetChar = ucmp8_getu (myFromUnicode, mySource[mySourceIndex]);

      if (myTargetIndex < targetLength)
        {
          mySourceIndex++;
          if (targetChar != 0 || !mySource[mySourceIndex - 1])
            {
              /*writes the char to the output stream */
              myTarget[myTargetIndex++] = targetChar;
            }
          else if ((args->converter->useFallback == TRUE) &&
                  (args->converter->sharedData->staticData->hasFromUnicodeFallback == TRUE))
          {
              /* Look up in the fallback table first */
              targetChar = ucmp8_getu (myFromUnicodeFallback, mySource[mySourceIndex-1]);
              if (targetChar != 0 || !mySource[mySourceIndex - 1])
                {
                  /*writes the char to the output stream */
                  myTarget[myTargetIndex++] = targetChar;
                }
          }
          if (targetChar == 0 && mySource[mySourceIndex-1] != 0)
          {
              *err = U_INVALID_CHAR_FOUND;
              reason = UCNV_UNASSIGNED;
              
              args->converter->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex - 1];
              args->converter->invalidUCharLength = 1;
              if (UTF_IS_LEAD(mySource[mySourceIndex-1]))
              {
                  /*if (mySource < args->sourceLimit)*/
                  if(mySourceIndex < sourceLength)
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
                  const UChar *saveSource = args->source;
                  char *saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;
                  args->target = (char *)myTarget+myTargetIndex;
                  args->source = mySource+mySourceIndex;
                  /* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
                  /* Check if we have encountered a surrogate pair.  If first UChar is lead byte
                   and second UChar is trail byte, it's a surrogate char.  If UChar is lead byte 
                   but second UChar is not trail byte, it's illegal sequence.  If neither, it's
                   plain unassigned code point.*/
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
                  args->offsets = saveOffsets;
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
  args->source += mySourceIndex;


  return;
}

UChar32 T_UConverter_getNextUChar_SBCS(UConverterToUnicodeArgs* args,
                                               UErrorCode* err)
{
  UChar myUChar;
  
  if (U_FAILURE(*err)) return 0xFFFD;

  if (args->source+1 > args->sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }
  
  /*Gets the corresponding codepoint*/
  myUChar = args->converter->sharedData->table->sbcs.toUnicode[(unsigned char)*(args->source++)];
  
  if (myUChar != 0xFFFD) return myUChar;
  else
    {      
      UChar* myUCharPtr = &myUChar;
      const char* sourceFinal = args->source;

      /* Do the fallback stuff */
      if ((args->converter->useFallback == TRUE)&&
          (args->converter->sharedData->staticData->hasToUnicodeFallback == TRUE))
      {
          myUChar = args->converter->sharedData->table->sbcs.toUnicodeFallback[ (unsigned char)*(args->source-1)];
          if (myUChar != 0xFFFD) return myUChar;
      }

      *err = U_INVALID_CHAR_FOUND;
      
      /*Calls the ErrorFunctor after rewinding the input buffer*/
      args->source--;
      /*It's is very likely that the ErrorFunctor will write to the
       *internal buffers */
      args->target = myUCharPtr;
      args->targetLimit = myUCharPtr + 1;
      args->source = sourceFinal;
      args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                    args,
                                    sourceFinal,
                                    1,
                                    UCNV_UNASSIGNED,
                                    err);

      /*makes the internal caching transparent to the user*/
      if (*err == U_INDEX_OUTOFBOUNDS_ERROR) *err = U_ZERO_ERROR;
      
      return myUChar;
    }
}

static const UConverterImpl _SBCSImpl={
    UCNV_SBCS,

    _SBCSLoad,
    _SBCSUnload,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_SBCS,
    NULL,
    T_UConverter_fromUnicode_SBCS,
    NULL,
    T_UConverter_getNextUChar_SBCS,

    NULL
};


/* Static data is in tools/makeconv/ucnvstat.c for data-based
 * converters. Be sure to update it as well.
 */

const UConverterSharedData _SBCSData={
    sizeof(UConverterSharedData), 1,
    NULL, NULL, NULL, FALSE, &_SBCSImpl, 
    0
};

/* DBCS --------------------------------------------------------------------- */

U_CFUNC void
_DBCSLoad(UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode) {
    const uint8_t *oldraw = raw;
    ucmp16_initFromData(&sharedData->table->dbcs.toUnicode,&raw, pErrorCode);
    if(((raw-oldraw)&3)!=0) {
        raw+=4-((raw-oldraw)&3);    /* pad to 4 */
    }
    oldraw = raw;
    ucmp16_initFromData(&sharedData->table->dbcs.fromUnicode, &raw, pErrorCode);
    if (sharedData->staticData->hasFromUnicodeFallback == TRUE)
    {
        if(((raw-oldraw)&3)!=0) {
            raw+=4-((raw-oldraw)&3);    /* pad to 4 */
        }
        ucmp16_initFromData(&sharedData->table->dbcs.fromUnicodeFallback, &raw, pErrorCode);
        oldraw = raw;
    }
    if (sharedData->staticData->hasToUnicodeFallback == TRUE)
    {
        if(((raw-oldraw)&3)!=0) {
            raw+=4-((raw-oldraw)&3);    /* pad to 4 */
        }
        ucmp16_initFromData(&sharedData->table->dbcs.toUnicodeFallback, &raw, pErrorCode);
    }    
}

U_CFUNC void
_DBCSUnload(UConverterSharedData *sharedData) {
    ucmp16_close (&sharedData->table->dbcs.fromUnicode);
    ucmp16_close (&sharedData->table->dbcs.toUnicode);
    if (sharedData->staticData->hasFromUnicodeFallback == TRUE)
        ucmp16_close (&sharedData->table->dbcs.fromUnicodeFallback);
    if (sharedData->staticData->hasToUnicodeFallback == TRUE)
        ucmp16_close (&sharedData->table->dbcs.toUnicodeFallback);
    uprv_free (sharedData->table);
}

void   T_UConverter_toUnicode_DBCS (UConverterToUnicodeArgs * args,
                                    UErrorCode * err)
{
  const char *mySource = ( char *) args->source;
  UChar *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - myTarget;
  int32_t sourceLength = args->sourceLimit - (char *) mySource;
  CompactShortArray *myToUnicode = NULL, *myToUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;

  myToUnicode = &args->converter->sharedData->table->dbcs.toUnicode;
  myToUnicodeFallback = &args->converter->sharedData->table->dbcs.toUnicodeFallback;

  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) mySource[mySourceIndex++];

          /*We have no internal state, we should */
          if (args->converter->toUnicodeStatus == 0x00)
            {
              args->converter->toUnicodeStatus = (unsigned char) mySourceChar;
            }
          else
            {
              if (args->converter->toUnicodeStatus != 0x00)
                {
                  mySourceChar = (UChar) ((args->converter->toUnicodeStatus << 8) | (mySourceChar & 0x00FF));
                  args->converter->toUnicodeStatus = 0x00;
                }

              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar != missingUCharMarker)
                {
                  /*writes the UniChar to the output stream */
                  myTarget[myTargetIndex++] = targetUniChar;
                }
              else if ((args->converter->useFallback == TRUE) &&
                  (args->converter->sharedData->staticData->hasToUnicodeFallback == TRUE))
              {
                  targetUniChar = (UChar) ucmp16_getu(myToUnicodeFallback, mySourceChar);
                  if (targetUniChar != missingUCharMarker)
                  {
                      myTarget[myTargetIndex++] = targetUniChar;
                  }
              }
              if (targetUniChar == missingUCharMarker)
                {
                  const char *saveSource = args->source;
                  UChar *saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;

                  *err = U_INVALID_CHAR_FOUND;
                  args->converter->invalidCharBuffer[0] = (char) (mySourceChar >> 8);
                  args->converter->invalidCharBuffer[1] = (char) mySourceChar;
                  args->converter->invalidCharLength = 2;
                  
                  args->target = myTarget + myTargetIndex;
                  args->source = mySource + mySourceIndex;

                  /* to do hsys: add more smarts to the codeUnits and length later */
                  ToU_CALLBACK_MACRO(args->converter->toUContext,
                                     args,
                                     args->converter->invalidCharBuffer,
                                     args->converter->invalidCharLength, 
                                     UCNV_UNASSIGNED,
                                     err);
                  /* Hsys: calculate the source and target advancement */
                  args->source = saveSource;
                  args->target = saveTarget;
                  args->offsets = saveOffsets;
                  if (U_FAILURE (*err)) break;
                  args->converter->invalidCharLength = 0;
                }
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

  /*If at the end of conversion we are still carrying state information
   *flush is TRUE, we can deduce that the input stream is truncated
   */
  if ((args->flush == TRUE)
      && (mySourceIndex == sourceLength)
      && (args->converter->toUnicodeStatus != 0x00))
    {
       
      if (U_SUCCESS(*err)) 
        {
          *err = U_TRUNCATED_CHAR_FOUND;
          args->converter->toUnicodeStatus = 0x00;
        }
    }

  args->target += myTargetIndex;
  args->source += mySourceIndex;

  return;
}

void   T_UConverter_fromUnicode_DBCS (UConverterFromUnicodeArgs * args,
                                      UErrorCode * err)
{
  const UChar *mySource = args->source;
  unsigned char *myTarget = (unsigned char *) args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - (char *) myTarget;
  int32_t sourceLength = args->sourceLimit - mySource;
  CompactShortArray *myFromUnicode = NULL, *myFromUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UConverterCallbackReason reason;

  myFromUnicode = &args->converter->sharedData->table->dbcs.fromUnicode;
  myFromUnicodeFallback = &args->converter->sharedData->table->dbcs.fromUnicodeFallback;

  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {

      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];

          /*Gets the corresponding codepoint */
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);
          if (targetUniChar != missingCharMarker)
            {
              /*writes the char to the output stream */
              myTarget[myTargetIndex++] = (char) (targetUniChar >> 8);
              if (myTargetIndex < targetLength)
                {
                  myTarget[myTargetIndex++] = (char) targetUniChar;
                }
              else
                {
                  args->converter->charErrorBuffer[0] = (char) targetUniChar;
                  args->converter->charErrorBufferLength = 1;
                  *err = U_INDEX_OUTOFBOUNDS_ERROR;
                }
            }
          else if ((args->converter->useFallback == TRUE) &&
                  (args->converter->sharedData->staticData->hasFromUnicodeFallback == TRUE))
          {

              targetUniChar = (UChar) ucmp16_getu (myFromUnicodeFallback, mySourceChar);
              if (targetUniChar != missingCharMarker)
                {
                    /*writes the char to the output stream */
                    myTarget[myTargetIndex++] = (char) (targetUniChar >> 8);
                    if (myTargetIndex < targetLength)
                      {
                        myTarget[myTargetIndex++] = (char) targetUniChar;
                      }
                    else
                      {
                        args->converter->charErrorBuffer[0] = (char) targetUniChar;
                        args->converter->charErrorBufferLength = 1;
                        *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      }
                }
          }
          if (targetUniChar == missingCharMarker)  
          {
              *err = U_INVALID_CHAR_FOUND;
              reason = UCNV_UNASSIGNED;
              
              args->converter->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex - 1];
              args->converter->invalidUCharLength = 1;
              if (UTF_IS_LEAD(mySource[mySourceIndex-1]))
              {
                  /*if (mySource < args->sourceLimit) */
                  if(mySourceIndex < sourceLength)
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
                  /* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
                  /* Check if we have encountered a surrogate pair.  If first UChar is lead byte
                   and second UChar is trail byte, it's a surrogate char.  If UChar is lead byte 
                   but second UChar is not trail byte, it's illegal sequence.  If neither, it's
                   plain unassigned code point.*/
                  const UChar *saveSource = args->source;
                  char *saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;
                  args->target = (char*)myTarget + myTargetIndex;
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
                  args->offsets = saveOffsets;
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

UChar32 T_UConverter_getNextUChar_DBCS(UConverterToUnicodeArgs* args,
                                               UErrorCode* err)
{
  UChar myUChar;
  
  if (U_FAILURE(*err)) return 0xFFFD;
  /*Checks boundaries and set appropriate error codes*/
  if (args->source+2 > args->sourceLimit) 
    {
      if (args->source >= args->sourceLimit)
        {
          /*Either caller has reached the end of the byte stream*/
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
        }
      else if ((args->source+1) == args->sourceLimit)
        {
          /* a character was cut in half*/
          *err = U_TRUNCATED_CHAR_FOUND;
        }
      
      return 0xFFFD;
    }

  /*Gets the corresponding codepoint*/
  myUChar = ucmp16_getu((&args->converter->sharedData->table->dbcs.toUnicode),
                        (uint16_t)(((UChar)((*(args->source))) << 8) |((uint8_t)*(args->source+1))));
  
  /*update the input pointer*/
  args->source += 2;
  if (myUChar != 0xFFFD) return myUChar;
  else
    {      
      UChar* myUCharPtr = &myUChar;
      const char* sourceFinal = args->source;

      /* rewinding the input buffer*/
      args->source -= 2;
      /* Do the fallback stuff */
      if ((args->converter->useFallback == TRUE) &&
          (args->converter->sharedData->staticData->hasToUnicodeFallback == TRUE))
      {
          myUChar = ucmp16_getu((&args->converter->sharedData->table->dbcs.toUnicodeFallback),
                            (uint16_t)(((UChar)((*(args->source))) << 8) |((uint8_t)*(args->source-1))));
          if (myUChar != 0xFFFD) 
          {
              args->source += 2;
              return myUChar;
          }
      }
      
      *err = U_INVALID_CHAR_FOUND;
    
      args->target = myUCharPtr;
      args->targetLimit = myUCharPtr + 1;
      args->source = sourceFinal;
      /*It's is very likely that the ErrorFunctor will write to the
       *internal buffers */
      args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                    args,
                                    sourceFinal,
                                    2,
                                    UCNV_UNASSIGNED,
                                    err);
      /*makes the internal caching transparent to the user*/
      if (*err == U_INDEX_OUTOFBOUNDS_ERROR) *err = U_ZERO_ERROR;

      return myUChar;
    }
} 

static const UConverterImpl _DBCSImpl={
    UCNV_DBCS,

    _DBCSLoad,
    _DBCSUnload,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_DBCS,
    NULL,
    T_UConverter_fromUnicode_DBCS,
    NULL,
    T_UConverter_getNextUChar_DBCS,

    NULL
};


/* Static data is in tools/makeconv/ucnvstat.c for data-based
 * converters. Be sure to update it as well.
 */

const UConverterSharedData _DBCSData={
    sizeof(UConverterSharedData), 1,
    NULL, NULL, NULL, FALSE, &_DBCSImpl, 
    0, /* tounicodestatus */
};
