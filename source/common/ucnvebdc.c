/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnvebcdic.c
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2000Aug29
*   created by: Ram Viswanadha
*
*   Change history:
*
*   
*/

#include "unicode/utypes.h"
#include "cmemory.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"
#include "unicode/ustring.h"
#include "cstring.h"


/* Protos */
U_CFUNC void T_UConverter_fromUnicode_EBCDIC_STATEFUL(UConverterFromUnicodeArgs * args,
                                                    UErrorCode * err);

U_CFUNC void T_UConverter_fromUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                                     UErrorCode * err);

U_CFUNC void T_UConverter_toUnicode_EBCDIC_STATEFUL(UConverterToUnicodeArgs * args,
                                                    UErrorCode * err);

U_CFUNC void T_UConverter_toUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC (UConverterToUnicodeArgs * args,
                                                                   UErrorCode * err);


U_CFUNC UChar32 T_UConverter_getNextUChar_EBCDIC_STATEFUL (UConverterToUnicodeArgs * args,
                                                           UErrorCode * err);

/* Forward declaration */

U_CFUNC void
_DBCSLoad(UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode);

U_CFUNC void
_DBCSUnload(UConverterSharedData *sharedData);





U_CFUNC void T_UConverter_toUnicode_EBCDIC_STATEFUL (UConverterToUnicodeArgs *args,
                                             UErrorCode * err)
{
  char *mySource = (char *) args->source;
  UChar *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - args->target;
  int32_t sourceLength = args->sourceLimit - args->source;
  CompactShortArray *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  int32_t myMode = args->converter->mode;

  myToUnicode = &(args->converter->sharedData->table->dbcs.toUnicode);
    while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (args->source[mySourceIndex++]);
          if (mySourceChar == UCNV_SI) myMode = UCNV_SI;
          else if (mySourceChar == UCNV_SO) myMode = UCNV_SO;
         else if ((myMode == UCNV_SO) &&
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
              else mySourceChar &= 0x00FF;

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar < 0xfffe)
                {
                  /*writes the UniChar to the output stream */
                  args->target[myTargetIndex++] = targetUniChar;
                }

              else
                {
                  const char* saveSource = args->source;
                  UChar* saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;
                  UConverterCallbackReason reason;

                  if (targetUniChar == 0xfffe)
                  {
                    reason = UCNV_UNASSIGNED;
                    *err = U_INVALID_CHAR_FOUND;
                  }
                  else
                  {
                    reason = UCNV_ILLEGAL;
                    *err = U_ILLEGAL_CHAR_FOUND;
                  }

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
                  args->converter->mode = myMode;
                  args->target += myTargetIndex;
                  args->source += mySourceIndex;
                  ToU_CALLBACK_MACRO(args->converter->toUContext,
                                     args,
                                     args->converter->invalidCharBuffer,
                                     args->converter->invalidCharLength,
                                     reason,
                                     err);

                  myMode = args->converter->mode;
                  args->source = saveSource;
                  args->target = saveTarget;
                  args->offsets = saveOffsets;
                  myMode = args->converter->mode;
                  if (U_FAILURE (*err))  break;
                  args->converter->invalidCharLength = 0;
                }
            }
        }
      else
        {
          *err = U_BUFFER_OVERFLOW_ERROR;
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
  args->converter->mode = myMode;

  return;
}

U_CFUNC void T_UConverter_toUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC (UConverterToUnicodeArgs * args,
                                                           UErrorCode * err)
{
  char *mySource = (char *) args->source;
  UChar *myTarget = args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - args->target;
  int32_t sourceLength = args->sourceLimit - args->source;
  CompactShortArray *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  int32_t myMode = args->converter->mode;

  myToUnicode = &args->converter->sharedData->table->dbcs.toUnicode;

    while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (args->source[mySourceIndex++]);
          if (mySourceChar == UCNV_SI) myMode = UCNV_SI;
          else if (mySourceChar == UCNV_SO) myMode = UCNV_SO;
          else if ((myMode == UCNV_SO) &&
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
              else mySourceChar &= 0x00FF;

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar < 0xfffe)
                {
                  /*writes the UniChar to the output stream */
                  {
                        if(myMode == UCNV_SO)
                         args->offsets[myTargetIndex] = mySourceIndex-2; /* double byte */
                        else
                         args->offsets[myTargetIndex] = mySourceIndex-1; /* single byte */
                  }
                  args->target[myTargetIndex++] = targetUniChar;
                }
              else
                {
                  int32_t currentOffset = args->offsets[myTargetIndex-1] + 2;/* Because mySourceIndex was already incremented */
                  int32_t My_i = myTargetIndex;
                  const char* saveSource = args->source;
                  UChar* saveTarget = args->target;
                  int32_t *saveOffsets = args->offsets;
                  UConverterCallbackReason reason;

                  if (targetUniChar == 0xfffe)
                  {
                    reason = UCNV_UNASSIGNED;
                    *err = U_INVALID_CHAR_FOUND;
                  }
                  else
                  {
                    reason = UCNV_ILLEGAL;
                    *err = U_ILLEGAL_CHAR_FOUND;
                  }

                  if (mySourceChar > 0xFF)
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
                  args->converter->mode = myMode;

                  args->target = args->target + myTargetIndex;
                  args->source = args->source + mySourceIndex;
                  args->offsets = args->offsets?args->offsets+myTargetIndex:0;
                  /* call back handles the offset array */
                  ToU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->toUContext,
                                     args,
                                     args->source,
                                     1, 
                                     reason,
                                     err);                  
                  
                  args->source = saveSource;
                  args->target = saveTarget;
                  myMode = args->converter->mode;
                  if (U_FAILURE (*err))   break;
                  args->converter->invalidCharLength = 0;
                  myMode = args->converter->mode;
                }
            }
        }
      else
        {
          *err = U_BUFFER_OVERFLOW_ERROR;
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
  args->converter->mode = myMode;

  return;
}

U_CFUNC void T_UConverter_fromUnicode_EBCDIC_STATEFUL (UConverterFromUnicodeArgs * args,
                                               UErrorCode * err)

{
  const UChar *mySource = args->source;
  unsigned char *myTarget = (unsigned char *) args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - args->target;
  int32_t sourceLength = args->sourceLimit - args->source;
  CompactShortArray *myFromUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
  UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;

  myFromUnicode = &args->converter->sharedData->table->dbcs.fromUnicode;
  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) args->source[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);
          oldIsTargetUCharDBCS = isTargetUCharDBCS;
          isTargetUCharDBCS = (UBool)(targetUniChar>0x00FF);
          
          if (targetUniChar != missingCharMarker)
            {
              if (oldIsTargetUCharDBCS != isTargetUCharDBCS)
                {
                  if (isTargetUCharDBCS) args->target[myTargetIndex++] = UCNV_SO;
                  else args->target[myTargetIndex++] = UCNV_SI;
                  
                  
                  if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength))
                    {
                      args->converter->charErrorBuffer[args->converter->charErrorBufferLength++] = (char) targetUniChar;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                  else if (myTargetIndex+1 >= targetLength)
                    {
                      args->converter->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      args->converter->charErrorBuffer[1] = (char)(targetUniChar & 0x00FF);
                      args->converter->charErrorBufferLength = 2;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                  
                }
              
              if (!isTargetUCharDBCS)
                {
                  args->target[myTargetIndex++] = (char) targetUniChar;
                }
              else
                {
                  args->target[myTargetIndex++] = (char) (targetUniChar >> 8);
                  if (myTargetIndex < targetLength)
                    {
                      args->target[myTargetIndex++] = (char) targetUniChar;
                    }
                  else
                    {
                      args->converter->charErrorBuffer[0] = (char) targetUniChar;
                      args->converter->charErrorBufferLength = 1;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                }
            }
          else
            {
              const UChar* saveSource = args->source;
              char* saveTarget = args->target;
              int32_t *saveOffsets = args->offsets;

              isTargetUCharDBCS = oldIsTargetUCharDBCS;
              *err = U_INVALID_CHAR_FOUND;
              args->converter->invalidUCharBuffer[0] = (UChar) mySourceChar;
              args->converter->invalidUCharLength = 1;

              args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;
              args->target += myTargetIndex;
              args->source += mySourceIndex;
              FromU_CALLBACK_MACRO(args->converter->fromUContext,
                                 args,
                                 args->converter->invalidUCharBuffer,
                                 1,
                                 (UChar32) mySourceChar,
                                 UCNV_UNASSIGNED,
                                 err);
              args->source = saveSource;
              args->target = saveTarget;
              args->offsets = saveOffsets;
              isTargetUCharDBCS  = (UBool) args->converter->fromUnicodeStatus;
              if (U_FAILURE (*err)) break;
              args->converter->invalidUCharLength = 0;
            }
        }
      else
        {
          *err = U_BUFFER_OVERFLOW_ERROR;
          break;
        }

    }


  args->target += myTargetIndex;
  args->source += mySourceIndex;
  
  args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;

  return;
}

U_CFUNC void T_UConverter_fromUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC (UConverterFromUnicodeArgs * args,
                                                             UErrorCode * err)

{
  const UChar *mySource = args->source;
  unsigned char *myTarget = (unsigned char *) args->target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = args->targetLimit - args->target;
  int32_t sourceLength = args->sourceLimit - args->source;
  CompactShortArray *myFromUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
  UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
  
  myFromUnicode = &args->converter->sharedData->table->dbcs.fromUnicode;
  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) args->source[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);
          oldIsTargetUCharDBCS = isTargetUCharDBCS;
          isTargetUCharDBCS =(UBool) (targetUniChar>0x00FF);
          
          if (targetUniChar != missingCharMarker)
            {
              if (oldIsTargetUCharDBCS != isTargetUCharDBCS)
                {
                  args->offsets[myTargetIndex] = mySourceIndex-1;
                  if (isTargetUCharDBCS) args->target[myTargetIndex++] = UCNV_SO;
                  else args->target[myTargetIndex++] = UCNV_SI;
                  
                  
                  if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength))
                    {
                      args->converter->charErrorBuffer[0] = (char) targetUniChar;
                      args->converter->charErrorBufferLength = 1;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                  else if (myTargetIndex+1 >= targetLength)
                    {
                      args->converter->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      args->converter->charErrorBuffer[1] = (char) (targetUniChar & 0x00FF);
                      args->converter->charErrorBufferLength = 2;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                }
              
              if (!isTargetUCharDBCS)
              {
                  args->offsets[myTargetIndex] = mySourceIndex-1;
                  args->target[myTargetIndex++] = (char) targetUniChar;
              }
              else
              {
                  args->offsets[myTargetIndex] = mySourceIndex-1;
                  args->target[myTargetIndex++] = (char) (targetUniChar >> 8);
                  if (myTargetIndex < targetLength)
                    {
                      args->offsets[myTargetIndex] = mySourceIndex-1;
                      args->target[myTargetIndex++] = (char) targetUniChar;
                    }
                  else
                    {
                      args->converter->charErrorBuffer[0] = (char) targetUniChar;
                      args->converter->charErrorBufferLength = 1;
                      *err = U_BUFFER_OVERFLOW_ERROR;
                      break;
                    }
                }
            }
          else
            {
              int32_t currentOffset = args->offsets[myTargetIndex-1]+1;
              char * saveTarget = args->target;
              const UChar* saveSource = args->source;
              int32_t *saveOffsets = args->offsets;
              *err = U_INVALID_CHAR_FOUND;
              args->converter->invalidUCharBuffer[0] = (UChar) mySourceChar;
              args->converter->invalidUCharLength = 1;

              /* Breaks out of the loop since behaviour was set to stop */
              args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;
              args->target += myTargetIndex;
              args->source += mySourceIndex;
              args->offsets = args->offsets?args->offsets+myTargetIndex:0;
              FromU_CALLBACK_OFFSETS_LOGIC_MACRO(args->converter->fromUContext,
                                     args,
                                     args->converter->invalidUCharBuffer,
                                     1,
                                     (UChar32)mySourceChar,
                                     UCNV_UNASSIGNED,
                                     err);
              isTargetUCharDBCS  = (UBool)(args->converter->fromUnicodeStatus);
              args->source = saveSource;
              args->target = saveTarget;
              args->offsets = saveOffsets;
              isTargetUCharDBCS  = (UBool)(args->converter->fromUnicodeStatus);
              if (U_FAILURE (*err))     break;
              args->converter->invalidUCharLength = 0;
            }
        }
      else
        {
          *err = U_BUFFER_OVERFLOW_ERROR;
          break;
        }

    }


  args->target += myTargetIndex;
  args->source += mySourceIndex;
  
  args->converter->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;

  return;
}

U_CFUNC UChar32 T_UConverter_getNextUChar_EBCDIC_STATEFUL(UConverterToUnicodeArgs* args,
                                                UErrorCode* err)
{
  UChar myUChar;
  char const *sourceInitial = args->source;
  /*safe keeps a ptr to the beginning in case we need to step back*/
  
  /*Input boundary check*/
  if (args->source >= args->sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xffff;
    }
  
  /*Checks to see if with have SI/SO shifters
   if we do we change the mode appropriately and we consume the byte*/
  while ((*(args->source) == UCNV_SI) || (*(args->source) == UCNV_SO)) 
    {
      args->converter->mode = *(args->source);
      args->source++;
      sourceInitial = args->source;
      
      /*Rechecks boundary after consuming the shift sequence*/
      if (args->source >= args->sourceLimit) 
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          return 0xffff;
        }
    }
  
  if (args->converter->mode == UCNV_SI)
    {
      myUChar = ucmp16_getu( (&(args->converter->sharedData->table->dbcs.toUnicode)),
                             ((UChar)(uint8_t)(*(args->source))));
      args->source++;
    }
  else
    {
      /*Lead byte: we Build the codepoint and get the corresponding character
       * and update the source ptr*/
      if ((args->source + 2) > args->sourceLimit) 
        {
          *err = U_TRUNCATED_CHAR_FOUND;
          return 0xffff;
        }

      myUChar = ucmp16_getu( (&(args->converter->sharedData->table->dbcs.toUnicode)),
                             (((UChar)(uint8_t)((*(args->source))) << 8) |((uint8_t)*(args->source+1))) );

      args->source += 2;
    }
  
  if (myUChar < 0xfffe) return myUChar;
  else
    {      
      /* HSYS: Check logic here */
      UChar* myUCharPtr = &myUChar;
      UConverterCallbackReason reason;

      if (myUChar == 0xfffe)
      {
        reason = UCNV_UNASSIGNED;
        *err = U_INVALID_CHAR_FOUND;
      }
      else
      {
        reason = UCNV_ILLEGAL;
        *err = U_ILLEGAL_CHAR_FOUND;
      }

      /*It's is very likely that the ErrorFunctor will write to the
       *internal buffers */
      args->target = myUCharPtr;
      args->targetLimit = myUCharPtr + 1;

      args->converter->fromCharErrorBehaviour(args->converter->toUContext,
                                    args,
                                    sourceInitial,
                                    args->source - sourceInitial,
                                    reason,
                                    err);
      
      /*makes the internal caching transparent to the user*/
      if (*err == U_BUFFER_OVERFLOW_ERROR) *err = U_ZERO_ERROR;
      
      return myUChar;
    }
} 

static const UConverterImpl _EBCDICStatefulImpl={
    UCNV_EBCDIC_STATEFUL,

    _DBCSLoad,
    _DBCSUnload,

    NULL,
    NULL,
    NULL,

    T_UConverter_toUnicode_EBCDIC_STATEFUL,
    T_UConverter_toUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_EBCDIC_STATEFUL,
    T_UConverter_fromUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_EBCDIC_STATEFUL,

    NULL
};

/* Static data is in tools/makeconv/ucnvstat.c for data-based
 * converters. Be sure to update it as well.
 */

const UConverterSharedData _EBCDICStatefulData={
    sizeof(UConverterSharedData), 1,
    NULL, NULL, NULL, FALSE, &_EBCDICStatefulImpl,
    0
};