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
#include "unicode/ucnv_cb.h"
#include "ucnv_cnv.h"
#include "cmemory.h"
#include "unicode/ucnv.h"

#define VALUE_STRING_LENGTH 32
/*Magic # 32 = 4(number of char in value string) * 8(max number of bytes per char for any converter) */
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
  if (reason > UCNV_IRREGULAR)
  {
    return;
  }
  
  *err = U_ZERO_ERROR;
  
  ucnv_cbFromUWriteSub(fromArgs, 0, err);
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
  int32_t i = 0;

  const UChar *myValueSource = NULL;
  UErrorCode err2 = U_ZERO_ERROR;
  UConverterFromUCallback original = NULL;
  void *originalContext;

  UConverterFromUCallback ignoredCallback = NULL;
  void *ignoredContext;

  if (reason > UCNV_IRREGULAR)
  {
    return;
  }

  ucnv_setFromUCallBack (fromArgs->converter,
             (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_SUBSTITUTE,
             NULL,  /* To Do for HSYS: context is null? */
             &original,
             &originalContext,
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
  while (i < length)
  {
    valueString[valueStringLength++] = (UChar) UNICODE_PERCENT_SIGN_CODEPOINT;	/* adding % */
    valueString[valueStringLength++] = (UChar) UNICODE_U_CODEPOINT;	/* adding U */
    itou (valueString + valueStringLength, codeUnits[i++], 16, 4);
    valueStringLength += 4;
  }

  myValueSource = valueString;

  /* reset the error */
  *err = U_ZERO_ERROR;

  ucnv_cbFromUWriteUChars(fromArgs, &myValueSource, myValueSource+valueStringLength, 0, err);

  ucnv_setFromUCallBack (fromArgs->converter,
                         original,
                         originalContext,
                         &ignoredCallback,
                         &ignoredContext,
                         &err2);
  if (U_FAILURE (err2))
    {
      *err = err2;
      return;
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
    
    *err = U_ZERO_ERROR;
    ucnv_cbToUWriteSub(toArgs,0,err);

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


  /* reset the error */
  *err = U_ZERO_ERROR;
  
  ucnv_cbToUWriteUChars(toArgs, uniValueString, valueStringLength, 0, err);

  return;
}
