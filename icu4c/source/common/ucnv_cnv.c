/*
*******************************************************************************
*
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
*   uconv_cnv.c:
*   Implements all the low level conversion functions
*   T_UnicodeConverter_{to,from}Unicode_$ConversionType
*
*   Change history:
*
*   06/29/2000  helena      Major rewrite of the callback APIs.
*/

#include "unicode/utypes.h"
#include "unicode/ucnv_err.h"
#include "ucnv_cnv.h"
#include "unicode/ucnv.h"
#include "cmemory.h"

/*Empties the internal unicode output buffer */
void  flushInternalUnicodeBuffer (UConverter * _this,
                                  UChar * myTarget,
                                  int32_t * myTargetIndex,
                                  int32_t targetLength,
                                  int32_t** offsets,
                                  UErrorCode * err)
{
  int32_t myUCharErrorBufferLength = _this->UCharErrorBufferLength;

  if (myUCharErrorBufferLength <= targetLength)
    {
      /*we have enough space
       *So we just copy the whole Error Buffer in to the output stream*/
      uprv_memcpy (myTarget,
                  _this->UCharErrorBuffer,
                  sizeof (UChar) * myUCharErrorBufferLength);
      if (offsets) 
        {
          int32_t i=0;
          for (i=0; i<myUCharErrorBufferLength;i++) (*offsets)[i] = -1; 
          *offsets += myUCharErrorBufferLength;
        }
      *myTargetIndex += myUCharErrorBufferLength;
      _this->UCharErrorBufferLength = 0;
    }
  else
    {
      /* We don't have enough space so we copy as much as we can
       * on the output stream and update the object
       * by updating the internal buffer*/
      uprv_memcpy (myTarget, _this->UCharErrorBuffer, sizeof (UChar) * targetLength);
      if (offsets) 
        {
          int32_t i=0;
          for (i=0; i< targetLength;i++) (*offsets)[i] = -1; 
          *offsets += targetLength;
        }
      uprv_memmove (_this->UCharErrorBuffer,
                   _this->UCharErrorBuffer + targetLength,
                   sizeof (UChar) * (myUCharErrorBufferLength - targetLength));
      _this->UCharErrorBufferLength -= (int8_t) targetLength;
      *myTargetIndex = targetLength;
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }

  return;
}

/*Empties the internal codepage output buffer */
void  flushInternalCharBuffer (UConverter * _this,
                               char *myTarget,
                               int32_t * myTargetIndex,
                               int32_t targetLength,
                               int32_t** offsets,
                               UErrorCode * err)
{
  int32_t myCharErrorBufferLength = _this->charErrorBufferLength;

  /*we have enough space */
  if (myCharErrorBufferLength <= targetLength)
    {
      uprv_memcpy (myTarget, _this->charErrorBuffer, myCharErrorBufferLength);
      if (offsets) 
        {
          int32_t i=0;
          for (i=0; i<myCharErrorBufferLength;i++) (*offsets)[i] = -1; 
          *offsets += myCharErrorBufferLength;
        }

      *myTargetIndex += myCharErrorBufferLength;
      _this->charErrorBufferLength = 0;
    }
  else
    /* We don't have enough space so we copy as much as we can
     * on the output stream and update the object*/
    {
      uprv_memcpy (myTarget, _this->charErrorBuffer, targetLength);
      if (offsets) 
        {
          int32_t i=0;
          for (i=0; i< targetLength;i++) (*offsets)[i] = -1; 
          *offsets += targetLength;
        }
      uprv_memmove (_this->charErrorBuffer,
                   _this->charErrorBuffer + targetLength,
                   (myCharErrorBufferLength - targetLength));
      _this->charErrorBufferLength -= (int8_t) targetLength;
      *myTargetIndex = targetLength;
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }

  return;
}
