/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnvmbcs.cpp
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

/* MBCS --------------------------------------------------------------------- */

static void
_MBCSLoad(UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode) {
    const uint8_t *oldraw;

    sharedData->table->mbcs.starters = (UBool*)raw;
    oldraw = raw += sizeof(UBool)*256;

    ucmp16_initFromData(&sharedData->table->mbcs.toUnicode, &raw, pErrorCode);
    if(((raw-oldraw)&3)!=0) {
        raw+=4-((raw-oldraw)&3);    /* pad to 4 */
    }
    oldraw = raw;
    ucmp16_initFromData(&sharedData->table->mbcs.fromUnicode, &raw, pErrorCode);
    if (sharedData->staticData->hasFromUnicodeFallback == TRUE)
    {
        if(((raw-oldraw)&3)!=0) {
            raw+=4-((raw-oldraw)&3);    /* pad to 4 */
        }
        oldraw = raw;
        ucmp16_initFromData(&sharedData->table->mbcs.fromUnicodeFallback, &raw, pErrorCode);
    }
    if (sharedData->staticData->hasToUnicodeFallback == TRUE)
    {
        if(((raw-oldraw)&3)!=0) {
            raw+=4-((raw-oldraw)&3);    /* pad to 4 */
        }
        ucmp16_initFromData(&sharedData->table->mbcs.toUnicodeFallback, &raw, pErrorCode);
    }
}

static void
_MBCSUnload(UConverterSharedData *sharedData) {
    ucmp16_close (&sharedData->table->mbcs.fromUnicode);
    ucmp16_close (&sharedData->table->mbcs.toUnicode);
    if (sharedData->staticData->hasFromUnicodeFallback == TRUE)
        ucmp16_close (&sharedData->table->mbcs.fromUnicodeFallback);
    if (sharedData->staticData->hasToUnicodeFallback == TRUE)
        ucmp16_close (&sharedData->table->mbcs.toUnicodeFallback);
	uprv_free (sharedData->table);
}

static void T_UConverter_toUnicode_MBCS (UConverterToUnicodeArgs * args,
                               UErrorCode * err)
{
  const char *mySource = args->source;
  UChar *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - myTarget;
  int32_t sourceLength = args->sourceLimit - mySource;
  CompactShortArray *myToUnicode = NULL, *myToUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UBool *myStarters = NULL;

  myToUnicode = &args->converter->sharedData->table->mbcs.toUnicode;
  myToUnicodeFallback = &args->converter->sharedData->table->mbcs.toUnicodeFallback;
  myStarters = args->converter->sharedData->table->mbcs.starters;

  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (mySource[mySourceIndex++]);


          if (myStarters[(uint8_t) mySourceChar] &&
              (args->converter->toUnicodeStatus == 0x00))
            {
              args->converter->toUnicodeStatus = (unsigned char) mySourceChar;
            }
          else
            {
              /*In case there is a state, we update the source char
               *by concatenating the previous char with the current
               *one
               */

              if (args->converter->toUnicodeStatus != 0x00)
                {
                  mySourceChar |= (UChar) (args->converter->toUnicodeStatus << 8);

                  args->converter->toUnicodeStatus = 0x00;
                }

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar != missingUCharMarker)
                {
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
                  if (mySourceChar > 0xff)
                    {
                      args->converter->invalidCharLength = 2;
                      args->converter->invalidCharBuffer[0] = (char) (mySourceChar >> 8);
                      args->converter->invalidCharBuffer[1] = (char) mySourceChar;
                    }
                  else
                    {
                      args->converter->invalidCharLength = 1;
                      args->converter->invalidCharBuffer[0] = (char) mySourceChar;
                    }
                  

                  args->target = myTarget + myTargetIndex;
                  args->source = mySource + mySourceIndex;
                  /* to do hsys: add more smarts to the codeUnits and length later */
                  ToU_CALLBACK_MACRO(args->converter->toUContext,
                                     args,
                                     args->converter->invalidCharBuffer,
                                     args->converter->invalidCharLength, 
                                     UCNV_UNASSIGNED,
                                     err);

                  args->source = saveSource;
                  args->target = saveTarget;
                  args->offsets = saveOffsets;
                  if (U_FAILURE (*err))    break;
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
  if (args->converter->toUnicodeStatus
      && (mySourceIndex == sourceLength)
      && (args->flush == TRUE))
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

static void T_UConverter_toUnicode_MBCS_OFFSETS_LOGIC (UConverterToUnicodeArgs * args,
                                                UErrorCode * err)
{
  const char *mySource = args->source;
  UChar *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - myTarget;
  int32_t sourceLength = args->sourceLimit - mySource;
  CompactShortArray *myToUnicode = NULL, *myToUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UChar oldMySourceChar = 0x0000;
  UBool *myStarters = NULL;
  
  myToUnicode = &args->converter->sharedData->table->mbcs.toUnicode;
  myToUnicodeFallback = &args->converter->sharedData->table->mbcs.toUnicodeFallback;
  myStarters = args->converter->sharedData->table->mbcs.starters;
 
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (mySource[mySourceIndex++]);


          if (myStarters[(uint8_t) mySourceChar] &&
              (args->converter->toUnicodeStatus == 0x00))
            {
              args->converter->toUnicodeStatus = (unsigned char) mySourceChar;
            }
          else
            {
              /*In case there is a state, we update the source char
               *by concatenating the previous char with the current
               *one
               */

              if (args->converter->toUnicodeStatus != 0x00)
                {
                  mySourceChar |= (UChar) (args->converter->toUnicodeStatus << 8);

                  args->converter->toUnicodeStatus = 0x00;
                }

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);
                  

              /*writing the UniChar to the output stream */
              if (targetUniChar != missingUCharMarker)
                {
                  /*writes the UniChar to the output stream */
                  {
                          

                      if (targetUniChar > 0x00FF)
                         args->offsets[myTargetIndex] = mySourceIndex -2; /* double byte character - make the offset point to the first char */
                      else
                         args->offsets[myTargetIndex] = mySourceIndex -1 ;  /* single byte char. Offset is OK */
                        

                  }
                myTarget[myTargetIndex++] = targetUniChar;
                oldMySourceChar  = mySourceChar;

                }
              else if ((args->converter->useFallback == TRUE) &&
                  (args->converter->sharedData->staticData->hasToUnicodeFallback == TRUE))
              {

                  targetUniChar = (UChar) ucmp16_getu (myToUnicodeFallback, mySourceChar);
                  /*writes the UniChar to the output stream */
                  {                          
                      if (targetUniChar > 0x00FF)
                         args->offsets[myTargetIndex] = mySourceIndex -2; /* double byte character - make the offset point to the first char */
                      else
                         args->offsets[myTargetIndex] = mySourceIndex -1 ;  /* single byte char. Offset is OK */                        

                  }
                myTarget[myTargetIndex++] = targetUniChar;
                oldMySourceChar  = mySourceChar;
              }
              if (targetUniChar == missingUCharMarker)  
              {
                  int32_t currentOffset = args->offsets[myTargetIndex-1] + ((oldMySourceChar>0x00FF)?2:1);
                  int32_t My_i = myTargetIndex; 
                  const char *saveSource = args->source;
                  UChar *saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;
                          
                  *err = U_INVALID_CHAR_FOUND;
                  if (mySourceChar > 0xff)
                    {
                      args->converter->invalidCharLength = 2;
                      args->converter->invalidCharBuffer[0] = (char) (mySourceChar >> 8);
                      args->converter->invalidCharBuffer[1] = (char) mySourceChar;
                    }
                  else
                    {
                      args->converter->invalidCharLength = 1;
                      args->converter->invalidCharBuffer[0] = (char) mySourceChar;
                    }
                  args->target = myTarget + myTargetIndex;
                  args->source = mySource + mySourceIndex;
                  args->offsets = args->offsets?args->offsets+myTargetIndex:0;

                  ToU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->toUContext,
                                     args,
                                     args->converter->invalidCharBuffer,
                                     args->converter->invalidCharLength, 
                                     UCNV_UNASSIGNED,
                                     err);
          
                  args->source = saveSource;
                  args->target = saveTarget;
                  args->offsets = saveOffsets;
                  if (U_FAILURE (*err))    break;
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
  if (args->converter->toUnicodeStatus
      && (mySourceIndex == sourceLength)
      && (args->flush == TRUE))
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

static void   T_UConverter_fromUnicode_MBCS (UConverterFromUnicodeArgs * args,
                                      UErrorCode * err)

{
  const UChar *mySource = args->source;
  char *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - myTarget;
  int32_t sourceLength = args->sourceLimit - mySource;
  CompactShortArray *myFromUnicode = NULL, *myFromUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UConverterCallbackReason reason;

  myFromUnicode = &args->converter->sharedData->table->mbcs.fromUnicode;
  myFromUnicodeFallback = &args->converter->sharedData->table->mbcs.fromUnicodeFallback;

  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);

          if (targetUniChar != missingCharMarker)
            {
              if (targetUniChar <= 0x00FF)
                {
                  myTarget[myTargetIndex++] = (char) targetUniChar;
                }
              else
                {
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
          else if ((args->converter->useFallback == TRUE) &&
              (args->converter->sharedData->staticData->hasFromUnicodeFallback == TRUE))
          {
              targetUniChar = (UChar) ucmp16_getu (myFromUnicodeFallback, mySourceChar);

              if (targetUniChar != missingCharMarker)
                {
                  if (targetUniChar <= 0x00FF)
                    {
                      myTarget[myTargetIndex++] = (char) targetUniChar;
                    }
                  else
                    {
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
          }
          if (targetUniChar == missingCharMarker)
            {
              const UChar *saveSource = args->source;
              char *saveTarget = args->target;
              int32_t *saveOffsets = args->offsets;
 
              *err = U_INVALID_CHAR_FOUND;
              args->converter->invalidUCharBuffer[0] = (UChar) mySourceChar;
              args->converter->invalidUCharLength = 1;
             if (UTF_IS_LEAD(mySource[mySourceIndex-1]))
              {
                  /* if (mySource < args->sourceLimit) */
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
                  args->target = myTarget + myTargetIndex;
                  args->source = mySource + mySourceIndex;
    /* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
                  /* HSYS: to do: more smarts */
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
                  if (U_FAILURE (*err)) break;
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

static void   T_UConverter_fromUnicode_MBCS_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                    UErrorCode * err)

{
  const UChar *mySource = args->source;
  char *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - myTarget;
  int32_t sourceLength = args->sourceLimit - mySource;
  CompactShortArray *myFromUnicode = NULL, *myFromUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UConverterCallbackReason reason;

  myFromUnicode = &args->converter->sharedData->table->mbcs.fromUnicode;
  myFromUnicodeFallback = &args->converter->sharedData->table->mbcs.fromUnicodeFallback;

  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);

          if (targetUniChar != missingCharMarker)
            {
              if (targetUniChar <= 0x00FF)
                {
                   args->offsets[myTargetIndex] = mySourceIndex-1;
                  myTarget[myTargetIndex++] = (char) targetUniChar;

                }
              else
                {
                   args->offsets[myTargetIndex] = mySourceIndex-1;
                  myTarget[myTargetIndex++] = (char) (targetUniChar >> 8);
                  if (myTargetIndex < targetLength)
                    {
                       args->offsets[myTargetIndex] = mySourceIndex-1;
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
          else if ((args->converter->useFallback == TRUE) &&
              (args->converter->sharedData->staticData->hasFromUnicodeFallback == TRUE))
          {
              targetUniChar = (UChar) ucmp16_getu (myFromUnicodeFallback, mySourceChar);
              if (targetUniChar != missingCharMarker)
                {
                  if (targetUniChar <= 0x00FF)
                    {
                       args->offsets[myTargetIndex] = mySourceIndex-1;
                      myTarget[myTargetIndex++] = (char) targetUniChar;

                    }
                  else
                    {
                       args->offsets[myTargetIndex] = mySourceIndex-1;
                      myTarget[myTargetIndex++] = (char) (targetUniChar >> 8);
                      if (myTargetIndex < targetLength)
                        {
                           args->offsets[myTargetIndex] = mySourceIndex-1;
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
          }

          if (targetUniChar == missingCharMarker)
            {
              int32_t currentOffset = mySourceIndex -1;
              int32_t My_i = myTargetIndex; 
              const UChar *saveSource = args->source;
              char *saveTarget = args->target;
              int32_t *saveOffsets = args->offsets;

              
              *err = U_INVALID_CHAR_FOUND;
              reason = UCNV_UNASSIGNED;
              args->converter->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex - 1];
              args->converter->invalidUCharLength = 1;
              if (UTF_IS_LEAD(mySource[mySourceIndex-1]))
              {
                  /* if (mySource < args->sourceLimit) */
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
                  args->target = myTarget + myTargetIndex;
                  args->source = mySource + mySourceIndex;
                  args->offsets = args->offsets?args->offsets+myTargetIndex:0; 
    /* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
                  /* HSYS: to do: more smarts including offsets*/
                  FromU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->fromUContext,
                                         args,
                                         args->converter->invalidUCharBuffer,
                                         args->converter->invalidUCharLength,
                                         (UChar32) (args->converter->invalidUCharLength == 2 ? 
                                             UTF16_GET_PAIR_VALUE(args->converter->invalidUCharBuffer[0], 
                                                                  args->converter->invalidUCharBuffer[2]) 
                                                    : args->converter->invalidUCharBuffer[0]),
                                         reason,
                                         err);
                  args->offsets = saveOffsets;
                  args->source = saveSource;
                  args->target = saveTarget;
                  if (U_FAILURE (*err)) break;
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

static UChar32 T_UConverter_getNextUChar_MBCS(UConverterToUnicodeArgs* args,
                                               UErrorCode* err)
{
  UChar myUChar;
  char const *sourceInitial = args->source;
  /*safe keeps a ptr to the beginning in case we need to step back*/
  
  /*Input boundary check*/
  if (args->source+1 > args->sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }
  /*Checks to see if the byte is a lead*/
  if (args->converter->sharedData->table->mbcs.starters[(uint8_t)*(args->source)] == FALSE)
    {
      /*Not lead byte: we update the source ptr and get the codepoint*/
      myUChar = ucmp16_getu((&args->converter->sharedData->table->mbcs.toUnicode),
                            (UChar)*(args->source));
      if ((args->converter->useFallback == TRUE) &&
          (args->converter->sharedData->staticData->hasToUnicodeFallback == TRUE) && 
          (myUChar == 0xFFFD))
      {
          myUChar = ucmp16_getu((&args->converter->sharedData->table->mbcs.toUnicodeFallback),
                            (UChar)*(args->source));
      }
      args->source++;
    }
  else
    {
      /*Lead byte: we Build the codepoint and get the corresponding character
       * and update the source ptr*/
      if (args->source + 2 > args->sourceLimit) 
        {
          *err = U_TRUNCATED_CHAR_FOUND;
          return 0xFFFD;
        }

      myUChar = ucmp16_getu((&args->converter->sharedData->table->mbcs.toUnicode),
                            (uint16_t)(((UChar)(*(args->source)) << 8) |((uint8_t)*(args->source+1))));

      if ((args->converter->useFallback == TRUE) && 
          (args->converter->sharedData->staticData->hasToUnicodeFallback == TRUE) &&
          (myUChar == 0xFFFD))
      {
      myUChar = ucmp16_getu((&args->converter->sharedData->table->mbcs.toUnicodeFallback),
                            (uint16_t)(((UChar)(*(args->source)) << 8) |((uint8_t)*(args->source+1))));
      }
      args->source += 2;
    }
  
  if (myUChar != 0xFFFD) return myUChar;
  else
    {      
      /*rewinds source*/
      const char* sourceFinal = args->source;
      UChar* myUCharPtr = &myUChar;
      
      *err = U_INVALID_CHAR_FOUND;
      args->source = sourceInitial;

      /*It's is very likely that the ErrorFunctor will write to the
       *internal buffers */
      args->target = myUCharPtr;
      args->targetLimit = myUCharPtr + 1;
      args->source = sourceFinal;
      args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                    args,
                                    sourceFinal,
                                    args->sourceLimit-sourceFinal,
                                    UCNV_UNASSIGNED,
                                    err);

      /*makes the internal caching transparent to the user*/
      if (*err == U_INDEX_OUTOFBOUNDS_ERROR) *err = U_ZERO_ERROR;
      
      return myUChar;
    }
} 

static void
_MBCSGetStarters(const UConverter* converter, UBool starters[256], UErrorCode *pErrorCode) {
    /* fills in the starters boolean array */
    uprv_memcpy(starters, converter->sharedData->table->mbcs.starters, 256*sizeof(UBool));
}

static const UConverterImpl _MBCSImpl={
    UCNV_MBCS,

    _MBCSLoad,
    _MBCSUnload,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_MBCS,
    T_UConverter_toUnicode_MBCS_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_MBCS,
    T_UConverter_fromUnicode_MBCS_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_MBCS,

    _MBCSGetStarters
};


/* Static data is in tools/makeconv/ucnvstat.c for data-based
 * converters. Be sure to update it as well.
 */

const UConverterSharedData _MBCSData={
    sizeof(UConverterSharedData), 1,
    NULL, NULL, NULL, FALSE, &_MBCSImpl, 
    0
};
