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
 */

#include "uhash.h"
#include "ucmp8.h"
#include "ucmp16.h"
#include "unicode/ucnv_bld.h"
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

bool_t 
  CONVERSION_U_SUCCESS (UErrorCode err)
{
  if ((err == U_INVALID_CHAR_FOUND) || (err == U_ILLEGAL_CHAR_FOUND))    return FALSE;
  else    return TRUE;
}

/*Takes a int32_t and fills in  a UChar* string with that number "radix"-based
 * and padded with "pad" zeroes
 */
static void   itou (UChar * buffer, int32_t i, int32_t radix, int32_t pad)
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
void   UCNV_FROM_U_CALLBACK_STOP (UConverter * _this,
				  char **target,
				  const char *targetLimit,
				  const UChar ** source,
				  const UChar * sourceLimit,
				  int32_t *offsets,
				  bool_t flush,
				  UErrorCode * err)
{
  return;
}


/*Function Pointer STOPS at the ILLEGAL_SEQUENCE */
void   UCNV_TO_U_CALLBACK_STOP (UConverter * _this,
			       UChar ** target,
			       const UChar * targetLimit,
			       const char **source,
			       const char *sourceLimit,
			       int32_t *offsets,
			       bool_t flush,
			       UErrorCode * err)
{
  return;
}

void   UCNV_FROM_U_CALLBACK_SKIP (UConverter * _this,
				  char **target,
				  const char *targetLimit,
				  const UChar ** source,
				  const UChar * sourceLimit,
				  int32_t *offsets,
				  bool_t flush,
				  UErrorCode * err)
{
  if (CONVERSION_U_SUCCESS (*err))    return;
  *err = U_ZERO_ERROR;
}

void   UCNV_FROM_U_CALLBACK_SUBSTITUTE (UConverter * _this,
					char **target,
					const char *targetLimit,
					const UChar ** source,
					const UChar * sourceLimit,
					int32_t *offsets,
					bool_t flush,
					UErrorCode * err)
{
  char togo[5];
  int32_t togoLen;



  if (CONVERSION_U_SUCCESS (*err)) return;
  
  /*In case we're dealing with a modal converter a la UCNV_EBCDIC_STATEFUL,
    we need to make sure that the emitting of the substitution charater in the right mode*/
  uprv_memcpy(togo, _this->subChar, togoLen = _this->subCharLen);
  if (ucnv_getType(_this) == UCNV_EBCDIC_STATEFUL)
    {
      if ((_this->fromUnicodeStatus)&&(togoLen != 2))
	{
	  togo[0] = UCNV_SI;
	  togo[1] = _this->subChar[0];
	  togo[2] = UCNV_SO;
	  togoLen = 3;
	}
      else if (!(_this->fromUnicodeStatus)&&(togoLen != 1))
	{
	  togo[0] = UCNV_SO;
	  togo[1] = _this->subChar[0];
	  togo[2] = _this->subChar[1];
	  togo[3] = UCNV_SI;
	  togoLen = 4;
	}
    }
  
  /*if we have enough space on the output buffer we just copy
    the subchar there and update the pointer */  
  if ((targetLimit - *target) >= togoLen)
    {
      uprv_memcpy (*target, togo, togoLen);
      *target += togoLen;
      *err = U_ZERO_ERROR;
      if (offsets)
	{
	  int i=0;
	  for (i=0;i<togoLen;i++) offsets[i]=0;
	  offsets += togoLen;
	}
    }
  else
    {
      /*if we don't have enough space on the output buffer
       *we copy as much as we can to it, update that pointer.
       *copy the rest in the internal buffer, and increase the
       *length marker
       */
      uprv_memcpy (*target, togo, (targetLimit - *target));
      if (offsets)
	{
	  int i=0;
	  for (i=0;i<(targetLimit - *target);i++) offsets[i]=0;
	  offsets += (targetLimit - *target);
	}
      uprv_memcpy (_this->charErrorBuffer + _this->charErrorBufferLength,
		  togo + (targetLimit - *target),
		  togoLen - (targetLimit - *target));
      _this->charErrorBufferLength += togoLen - (targetLimit - *target);
      *target += (targetLimit - *target);
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }

  return;

}

/*uses itou to get a unicode escape sequence of the offensive sequence,
 *uses a clean copy (resetted) of the converter, to convert that unicode
 *escape sequence to the target codepage (if conversion failure happens then
 *we revert to substituting with subchar)
 */
void   UCNV_FROM_U_CALLBACK_ESCAPE (UConverter * _this,
						 char **target,
						 const char *targetLimit,
						 const UChar ** source,
						 const UChar * sourceLimit,
						 int32_t *offsets,
						 bool_t flush,
						 UErrorCode * err)
{

  UChar valueString[VALUE_STRING_LENGTH];
  int32_t valueStringLength = 0;
  const UChar *mySource = *source;
  UChar codepoint[CODEPOINT_STRING_LENGTH];
  int32_t i = 0;
  /*Makes a bitwise copy of the converter passwd in */
  UConverter myConverter = *_this;
  char myTarget[VALUE_STRING_LENGTH];
  char *myTargetAlias = myTarget;
  const UChar *myValueSource = NULL;
  UErrorCode err2 = U_ZERO_ERROR;
  uint32_t myFromUnicodeStatus = _this->fromUnicodeStatus;


  if (CONVERSION_U_SUCCESS (*err))   return;

  ucnv_reset (&myConverter);
  myConverter.fromUnicodeStatus = myFromUnicodeStatus;
  
  ucnv_setFromUCallBack (&myConverter,
			 (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_STOP,
			 &err2);
  if (U_FAILURE (err2))
    {
      *err = err2;
      return;
    }

  codepoint[0] = (UChar) UNICODE_PERCENT_SIGN_CODEPOINT;	/* adding % */
  codepoint[1] = (UChar) UNICODE_U_CODEPOINT;	/* adding U */

  while (i < _this->invalidUCharLength)
    {
      itou (codepoint + 2, _this->invalidUCharBuffer[i++], 16, 4);
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
      UCNV_FROM_U_CALLBACK_SUBSTITUTE (_this,
				       target,
				       targetLimit,
				       source,
				       sourceLimit,
				       offsets,
				       flush,
				       err);
      return;
    }


  
  valueStringLength = myTargetAlias - myTarget;
  
  /*if we have enough space on the output buffer we just copy
   * the subchar there and update the pointer
   */
  if ((targetLimit - *target) >= valueStringLength)
    {
      uprv_memcpy (*target, myTarget, valueStringLength);
      *target += valueStringLength;
      *err = U_ZERO_ERROR;

      if (offsets)
	{
	  int i=0;
	  for (i=0;i<valueStringLength;i++) offsets[i]=0;
	  offsets += valueStringLength;
	}
    }
  else
    {
      /*if we don't have enough space on the output buffer
       *we copy as much as we can to it, update that pointer.
       *copy the rest in the internal buffer, and increase the
       *length marker
       */

      if (offsets)
	{
	  int i=0;
	  for (i=0;i<(targetLimit - *target);i++) offsets[i]=0;
	  offsets += (targetLimit - *target);
	}
      uprv_memcpy (*target, myTarget, (targetLimit - *target));
      uprv_memcpy (_this->charErrorBuffer + _this->charErrorBufferLength,
		  myTarget + (targetLimit - *target),
		  valueStringLength - (targetLimit - *target));
      _this->charErrorBufferLength += valueStringLength - (targetLimit - *target);
      *target += (targetLimit - *target);
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }

  return;
}



void UCNV_TO_U_CALLBACK_SKIP (UConverter * _this,
			     UChar ** target,
			     const UChar * targetLimit,
			     const char **source,
			     const char *sourceLimit,
			     int32_t *offsets,
			     bool_t flush,
			     UErrorCode * err)
{
  if (CONVERSION_U_SUCCESS (*err))   return;
  *err = U_ZERO_ERROR;
}

void   UCNV_TO_U_CALLBACK_SUBSTITUTE (UConverter * _this,
				     UChar ** target,
				     const UChar * targetLimit,
				     const char **source,
				     const char *sourceLimit,
				     int32_t *offsets,
				     bool_t flush,
				     UErrorCode * err)
{
  
  if (CONVERSION_U_SUCCESS (*err))   return;
  
  if ((targetLimit - *target) >= 1)
    {
      **target = 0xFFFD;
      (*target)++;
      if (offsets)  *offsets = 0;
      *err = U_ZERO_ERROR;
    }
  else
    {
      _this->UCharErrorBuffer[_this->UCharErrorBufferLength] = 0xFFFD;
      _this->UCharErrorBufferLength++;
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
  
  return;
  
}

/*uses itou to get a unicode escape sequence of the offensive sequence,
 *and uses that as the substitution sequence
 */
void  UCNV_TO_U_CALLBACK_ESCAPE (UConverter * _this,
					     UChar ** target,
					     const UChar * targetLimit,
					     const char **source,
					     const char *sourceLimit,
					     int32_t *offsets,
					     bool_t flush,
					     UErrorCode * err)
{
  UChar uniValueString[VALUE_STRING_LENGTH];
  int32_t valueStringLength = 0;
  const unsigned char *mySource = (const unsigned char *) *source;
  UChar codepoint[CODEPOINT_STRING_LENGTH];
  int32_t j = 0, i = 0;
  const int32_t* offsets_end = offsets +( targetLimit - *target);
  
  if (CONVERSION_U_SUCCESS (*err))   return;
  
  codepoint[0] = (UChar) UNICODE_PERCENT_SIGN_CODEPOINT;	/* adding % */
  codepoint[1] = (UChar) UNICODE_X_CODEPOINT;	/* adding X */
  
  while (i < _this->invalidCharLength)
    {
      itou (codepoint + 2, _this->invalidCharBuffer[i++], 16, 2);
      uprv_memcpy (uniValueString + valueStringLength, codepoint, sizeof (UChar) * 4);
      valueStringLength += 4;
    }
  
  if ((targetLimit - *target) >= valueStringLength)
    {
      /*if we have enough space on the output buffer we just copy
       * the subchar there and update the pointer
       */
      uprv_memcpy (*target, uniValueString, (sizeof (UChar)) * (valueStringLength));
      if (offsets) 
	{
	  for (i = 0; i < valueStringLength; i++)  offsets[i] = 0;
	}
      *target += valueStringLength;
      
      *err = U_ZERO_ERROR;
    }
  else
    {
      /*if we don't have enough space on the output buffer
       *we copy as much as we can to it, update that pointer.
       *copy the rest in the internal buffer, and increase the
       *length marker
       */
      uprv_memcpy (*target, uniValueString, (sizeof (UChar)) * (targetLimit - *target));
      if (offsets) 
	{
	  for (i = 0; i < (targetLimit - *target); i++)  offsets[i] = 0;
	}	    
      
      
      uprv_memcpy (_this->UCharErrorBuffer,
		  uniValueString + (targetLimit - *target),
		  (sizeof (UChar)) * (valueStringLength - (targetLimit - *target)));
      _this->UCharErrorBufferLength += valueStringLength - (targetLimit - *target);
      *target += (targetLimit - *target);
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
  
  return;
}
