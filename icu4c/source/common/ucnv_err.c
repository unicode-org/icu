/*
 *******************************************************************************
 *
 *   Copyright (C) 1998-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
 *******************************************************************************
 *
 *  ucnv_err.c
 *  Implements error behaviour functions called by T_UConverter_{from,to}Unicode
 *
 *
*   Change history:
*
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#include "ucmp8.h"
#include "ucmp16.h"
#include "unicode/ucnv_err.h"
#include "ucnv_cnv.h"
#include "cmemory.h"
#include "unicode/ucnv.h"

#define VALUE_STRING_LENGTH 32
/*Magic # 32 = 4(number of char in value string) * 8(max number of bytes per char for any converter) */
#define CODEPOINT_STRING_LENGTH 7
#define UNICODE_PERCENT_SIGN_CODEPOINT 0x0025
#define UNICODE_U_CODEPOINT 0x0055
#define UNICODE_X_CODEPOINT 0x0058


#define ToOffset(a) a<=9?(0x0030+a):(0x0030+a+7)

UBool 
  CONVERSION_U_SUCCESS (UErrorCode err)
{
  if ((err == U_INVALID_CHAR_FOUND) || (err == U_ILLEGAL_CHAR_FOUND))    return FALSE;
  else    return TRUE;
}

/*Takes a int32_t and fills in  a UChar* string with that number "radix"-based
 * and padded with "pad" zeroes
 */
static void   itou (UChar * buffer, uint32_t i, uint32_t radix, int32_t pad)
{
  int32_t length = 0;
  int32_t num = 0;
  int8_t digit;
  int32_t j;
  UChar temp;

  while (i >= radix)
    {
      num = i / radix;
      digit = (int8_t) (i - num * radix);
      buffer[length++] = (UChar) (ToOffset (digit));
      i = num;
    }

  buffer[length] = (UChar) (ToOffset (i));

  while (length < pad)   buffer[++length] = (UChar) 0x0030;	/*zero padding */
  buffer[length--] = (UChar) 0x0000;
  
  /*Reverses the string */
  for (j = 0; j < (pad / 2); j++)
    {
      temp = buffer[length - j];
      buffer[length - j] = buffer[j];
      buffer[j] = temp;
    }

  return;
}

/*Function Pointer STOPS at the ILLEGAL_SEQUENCE */
void   UCNV_FROM_U_CALLBACK_STOP (
                  void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
				  UErrorCode * err)
{
  /* the caller must have set the error code accordingly */
  return;
}


/*Function Pointer STOPS at the ILLEGAL_SEQUENCE */
void   UCNV_TO_U_CALLBACK_STOP (
                   void *context,
                   UConverterToUnicodeArgs *toUArgs,
                   const char* codePoints,
                   int32_t length,
                   UConverterCallbackReason reason,
			       UErrorCode * err)
{
  /* the caller must have set the error code accordingly */
  return;
}

void   UCNV_FROM_U_CALLBACK_SKIP (                  
                  void *context,
                  UConverterFromUnicodeArgs *fromUArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
				  UErrorCode * err)
{
  if (reason <= UCNV_IRREGULAR)
  {
    *err = U_ZERO_ERROR;
  }
}

void   UCNV_FROM_U_CALLBACK_SUBSTITUTE (
                  void *context,
                  UConverterFromUnicodeArgs *fromArgs,
                  const UChar* codeUnits,
                  int32_t length,
                  UChar32 codePoint,
                  UConverterCallbackReason reason,
                  UErrorCode * err)
{
    char togo[5];
    int32_t togoLen;

    if (reason > UCNV_IRREGULAR)
    {
        return;
    }

    /* ### TODO:
     * This should use the new ucnv_cbWrite...() functions instead of doing
     * "tricks" as before we had a good callback API!
     */

    /*In case we're dealing with a modal converter a la UCNV_EBCDIC_STATEFUL,
    we need to make sure that the emitting of the substitution charater in the right mode*/
    uprv_memcpy(togo, fromArgs->converter->subChar, togoLen = fromArgs->converter->subCharLen);
    if (ucnv_getType(fromArgs->converter) == UCNV_EBCDIC_STATEFUL)
    {
        if ((fromArgs->converter->fromUnicodeStatus)&&(togoLen != 2))
        {
            togo[0] = UCNV_SI;
            togo[1] = fromArgs->converter->subChar[0];
            togo[2] = UCNV_SO;
            togoLen = 3;
        }
        else if (!(fromArgs->converter->fromUnicodeStatus)&&(togoLen != 1))
        {
            togo[0] = UCNV_SO;
            togo[1] = fromArgs->converter->subChar[0];
            togo[2] = fromArgs->converter->subChar[1];
            togo[3] = UCNV_SI;
            togoLen = 4;
        }
    }

    /*if we have enough space on the output buffer we just copy
    the subchar there and update the pointer */  
    if ((fromArgs->targetLimit - fromArgs->target) >= togoLen)
    {
        uprv_memcpy (fromArgs->target, togo, togoLen);
        fromArgs->target += togoLen;
        *err = U_ZERO_ERROR;
        if (fromArgs->offsets)
        {
            int i=0;
            for (i=0;i<togoLen;i++) fromArgs->offsets[i]=0;
            fromArgs->offsets += togoLen;
        }
    }
    else
    {
        /*if we don't have enough space on the output buffer
        *we copy as much as we can to it, update that pointer.
        *copy the rest in the internal buffer, and increase the
        *length marker
        */
        uprv_memcpy (fromArgs->target, togo, (fromArgs->targetLimit - fromArgs->target));
        if (fromArgs->offsets)
        {
            int i=0;
            for (i=0;i<(fromArgs->targetLimit - fromArgs->target);i++) fromArgs->offsets[i]=0;
            fromArgs->offsets += (fromArgs->targetLimit - fromArgs->target);
        }
        uprv_memcpy (fromArgs->converter->charErrorBuffer + fromArgs->converter->charErrorBufferLength,
        togo + (fromArgs->targetLimit - fromArgs->target),
        togoLen - (fromArgs->targetLimit - fromArgs->target));
        fromArgs->converter->charErrorBufferLength += togoLen - (fromArgs->targetLimit - fromArgs->target);
        fromArgs->target += (fromArgs->targetLimit - fromArgs->target);
        *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
    return;
}

/*uses itou to get a unicode escape sequence of the offensive sequence,
 *uses a clean copy (resetted) of the converter, to convert that unicode
 *escape sequence to the target codepage (if conversion failure happens then
 *we revert to substituting with subchar)
 */
void   UCNV_FROM_U_CALLBACK_ESCAPE (
                         void *context,
                         UConverterFromUnicodeArgs *fromArgs,
                         const UChar *codeUnits,
                         int32_t length,
                         UChar32 codePoint,
                         UConverterCallbackReason reason,
						 UErrorCode * err)
{

  UChar valueString[VALUE_STRING_LENGTH];
  int32_t valueStringLength = 0;
  UChar codepoint[CODEPOINT_STRING_LENGTH];
  int32_t i = 0;
  /*Makes a bitwise copy of the converter passwd in */
  UConverter myConverter = *(fromArgs->converter);
  char myTarget[VALUE_STRING_LENGTH];
  char *myTargetAlias = myTarget;
  const UChar *myValueSource = NULL;
  UErrorCode err2 = U_ZERO_ERROR;
  uint32_t myFromUnicodeStatus = fromArgs->converter->fromUnicodeStatus;
  UConverterFromUCallback original = NULL;

  if (reason > UCNV_IRREGULAR)
  {
    return;
  }

  /* ### TODO:
   * This should use the new ucnv_cbWrite...() functions instead of doing
   * "tricks" as before we had a good callback API!
   */

  ucnv_reset (&myConverter);
  myConverter.fromUnicodeStatus = myFromUnicodeStatus;
  
  ucnv_setFromUCallBack (&myConverter,
			 (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_STOP,
             NULL,  /* To Do for HSYS: context is null? */
             &original,
             NULL,
			 &err2);
  if (U_FAILURE (err2))
    {
      *err = err2;
      return;
    }

  /*
   * ### TODO:
   * This should actually really work with the codePoint, not with the codeUnits;
   * how do we represent a code point > 0xffff? It should be one single escape, not
   * two for a surrogate pair!
   */
  codepoint[0] = (UChar) UNICODE_PERCENT_SIGN_CODEPOINT;	/* adding % */
  codepoint[1] = (UChar) UNICODE_U_CODEPOINT;	/* adding U */

  while (i < length)
    {
      itou (codepoint + 2, codeUnits[i++], 16, 4);
      uprv_memcpy (valueString + valueStringLength, codepoint, sizeof (UChar) * 6);
      valueStringLength += CODEPOINT_STRING_LENGTH - 1;
    }

  myValueSource = valueString;

  /*converts unicode escape sequence */
  ucnv_fromUnicode (&myConverter,
		    &myTargetAlias,
		    myTargetAlias + VALUE_STRING_LENGTH,
		    &myValueSource,
		    myValueSource + CODEPOINT_STRING_LENGTH - 1,
		    NULL,
		    TRUE,
		    &err2);
  
  if (U_FAILURE (err2))
    {
      UCNV_FROM_U_CALLBACK_SUBSTITUTE (
                       NULL, /* TO do for HSYS: context */
                       fromArgs,
                       codeUnits,
                       length,
                       codePoint,
                       reason,
				       err);
      return;
    }

  valueStringLength = myTargetAlias - myTarget;
  
  /*if we have enough space on the output buffer we just copy
   * the subchar there and update the pointer
   */
  if ((fromArgs->targetLimit - fromArgs->target) >= valueStringLength)
    {
      uprv_memcpy (fromArgs->target, myTarget, valueStringLength);
      fromArgs->target += valueStringLength;
      *err = U_ZERO_ERROR;

      if (fromArgs->offsets)
	{
	  int j=0;
	  for (j=0;j<valueStringLength;j++) fromArgs->offsets[j]=0;
	  fromArgs->offsets += valueStringLength;
	}
    }
  else
    {
      /*if we don't have enough space on the output buffer
       *we copy as much as we can to it, update that pointer.
       *copy the rest in the internal buffer, and increase the
       *length marker
       */

      if (fromArgs->offsets)
	{
	  int j=0;
	  for (j=0;j<(fromArgs->targetLimit - fromArgs->target);j++) fromArgs->offsets[j]=0;
	  fromArgs->offsets += (fromArgs->targetLimit - fromArgs->target);
	}
      uprv_memcpy (fromArgs->target, myTarget, (fromArgs->targetLimit - fromArgs->target));
      uprv_memcpy (fromArgs->converter->charErrorBuffer + fromArgs->converter->charErrorBufferLength,
		  myTarget + (fromArgs->targetLimit - fromArgs->target),
		  valueStringLength - (fromArgs->targetLimit - fromArgs->target));
      fromArgs->converter->charErrorBufferLength += valueStringLength - (fromArgs->targetLimit - fromArgs->target);
      fromArgs->target += (fromArgs->targetLimit - fromArgs->target);
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }

  return;
}



void UCNV_TO_U_CALLBACK_SKIP (
                 void *context,
                 UConverterToUnicodeArgs *toArgs,
                 const char* codeUnits,
                 int32_t length,
                 UConverterCallbackReason reason,
			     UErrorCode * err)
{
  if (reason <= UCNV_IRREGULAR)
  {
    *err = U_ZERO_ERROR;
  }
}

void   UCNV_TO_U_CALLBACK_SUBSTITUTE (
                 void *context,
                 UConverterToUnicodeArgs *toArgs,
                 const char* codeUnits,
                 int32_t length,
                 UConverterCallbackReason reason,
			     UErrorCode * err)
{
  if (reason > UCNV_IRREGULAR)
  {
    return;
  }

  /* ### TODO:
   * This should use the new ucnv_cbWrite...() functions instead of doing
   * "tricks" as before we had a good callback API!
   */

  if ((toArgs->targetLimit - toArgs->target) >= 1)
    {
      *toArgs->target = 0xFFFD;
      (toArgs->target)++;
      if (toArgs->offsets)  *(toArgs->offsets) = 0;
      *err = U_ZERO_ERROR;
    }
  else
    {
      toArgs->converter->UCharErrorBuffer[toArgs->converter->UCharErrorBufferLength] = 0xFFFD;
      toArgs->converter->UCharErrorBufferLength++;
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
  
  return;
  
}

/*uses itou to get a unicode escape sequence of the offensive sequence,
 *and uses that as the substitution sequence
 */
void  UCNV_TO_U_CALLBACK_ESCAPE (
                 void *context,
                 UConverterToUnicodeArgs *toArgs,
                 const char* codeUnits,
                 int32_t length,
                 UConverterCallbackReason reason,
			     UErrorCode * err)
{
  UChar uniValueString[VALUE_STRING_LENGTH];
  int32_t valueStringLength = 0;
  int32_t i = 0;
  
  if (reason > UCNV_IRREGULAR)
  {
    return;
  }

  /* ### TODO:
   * This should use the new ucnv_cbWrite...() functions instead of doing
   * "tricks" as before we had a good callback API!
   * (Actually, this function is not all that bad.)
   */

  while (i < length)
    {
      uniValueString[valueStringLength++] = (UChar) UNICODE_PERCENT_SIGN_CODEPOINT;	/* adding % */
      uniValueString[valueStringLength++] = (UChar) UNICODE_X_CODEPOINT;	/* adding X */
      itou (uniValueString + valueStringLength, (uint8_t) codeUnits[i++], 16, 2);
      valueStringLength += 2;
    }
  
  if ((toArgs->targetLimit - toArgs->target) >= valueStringLength)
    {
      /*if we have enough space on the output buffer we just copy
       * the subchar there and update the pointer
       */
      uprv_memcpy (toArgs->target, uniValueString, (sizeof (UChar)) * (valueStringLength));
      if (toArgs->offsets) 
	{
	  for (i = 0; i < valueStringLength; i++)  toArgs->offsets[i] = 0;
	}
      toArgs->target += valueStringLength;
      
      *err = U_ZERO_ERROR;
    }
  else
    {
      /*if we don't have enough space on the output buffer
       *we copy as much as we can to it, update that pointer.
       *copy the rest in the internal buffer, and increase the
       *length marker
       */
      uprv_memcpy (toArgs->target, uniValueString, (sizeof (UChar)) * (toArgs->targetLimit - toArgs->target));
      if (toArgs->offsets) 
	{
	  for (i = 0; i < (toArgs->targetLimit - toArgs->target); i++)  toArgs->offsets[i] = 0;
	}	    
      
      
      uprv_memcpy (toArgs->converter->UCharErrorBuffer,
		  uniValueString + (toArgs->targetLimit - toArgs->target),
		  (sizeof (UChar)) * (valueStringLength - (toArgs->targetLimit - toArgs->target)));
      toArgs->converter->UCharErrorBufferLength += valueStringLength - (toArgs->targetLimit - toArgs->target);
      toArgs->target += (toArgs->targetLimit - toArgs->target);
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
  
  return;
}
