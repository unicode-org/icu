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
*/

#include "unicode/utypes.h"
#include "cmemory.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"

/* SBCS --------------------------------------------------------------------- */

static void
_SBCSLoad(UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode) {
    const uint8_t *oldraw = raw;
    sharedData->table->sbcs.toUnicode = (uint16_t*)raw;
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
        sharedData->table->sbcs.toUnicodeFallback = (uint16_t*)raw;
    }
}

static void
_SBCSUnload(UConverterSharedData *sharedData) {
    ucmp8_close (&sharedData->table->sbcs.fromUnicode);
    if (sharedData->staticData->hasFromUnicodeFallback == TRUE)
        ucmp8_close (&sharedData->table->sbcs.fromUnicodeFallback);
    uprv_free (sharedData->table);
}

void T_UConverter_toUnicode_SBCS (UConverter * _this,
                                  UChar ** target,
                                  const UChar * targetLimit,
                                  const char **source,
                                  const char *sourceLimit,
                                  int32_t *offsets,
                                  bool_t flush,
                                  UErrorCode * err)
{
  char *mySource = (char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  UChar *myToUnicode = NULL, *myToUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  
  myToUnicode = _this->sharedData->table->sbcs.toUnicode;
  myToUnicodeFallback = _this->sharedData->table->sbcs.toUnicodeFallback;
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
              if ((_this->useFallback == TRUE) &&
                  (_this->sharedData->staticData->hasToUnicodeFallback == TRUE))
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
                  *err = U_INVALID_CHAR_FOUND;
                  _this->invalidCharBuffer[0] = (char) mySource[mySourceIndex - 1];
                  _this->invalidCharLength = 1;

                  ToU_CALLBACK_MACRO(_this,
                                     myTarget,
                                     myTargetIndex, 
                                     targetLimit,
                                     mySource, 
                                     mySourceIndex,
                                     sourceLimit,
                                     offsets,
                                     flush,
                                     err);
          
                  if (U_FAILURE (*err)) break;
                  _this->invalidCharLength = 0;
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
  *source += mySourceIndex;

  return;
}

void T_UConverter_fromUnicode_SBCS (UConverter * _this,
                                 char **target,
                                 const char *targetLimit,
                                 const UChar ** source,
                                 const UChar * sourceLimit,
                                 int32_t *offsets,
                                 bool_t flush,
                                 UErrorCode * err)
{
  const UChar *mySource = *source;
  unsigned char *myTarget = (unsigned char *) *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - (char *) myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactByteArray *myFromUnicode = NULL, *myFromUnicodeFallback = NULL;
  unsigned char targetChar = 0x00;

  myFromUnicode = &_this->sharedData->table->sbcs.fromUnicode;
  myFromUnicodeFallback = &_this->sharedData->table->sbcs.fromUnicodeFallback;

  /*writing the char to the output stream */
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
          else if ((_this->useFallback == TRUE) &&
                  (_this->sharedData->staticData->hasFromUnicodeFallback == TRUE))
          {
              /* Look up in the fallback table first */
              targetChar = ucmp8_getu (myFromUnicodeFallback, mySource[mySourceIndex-1]);
              if (targetChar != 0 || !mySource[mySourceIndex - 1])
                {
                  /*writes the char to the output stream */
                  myTarget[myTargetIndex++] = targetChar;
                }
          }
          if (targetChar == 0 && !mySource[mySourceIndex-1])
          {
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar)mySource[mySourceIndex - 1];
              _this->invalidUCharLength = 1;

/* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
              FromU_CALLBACK_MACRO(_this,
                                   (char *)myTarget, 
                                   myTargetIndex,
                                   targetLimit, 
                                   mySource,
                                   mySourceIndex, 
                                   sourceLimit,
                                   offsets, 
                                   flush, 
                                   err);
              if (U_FAILURE (*err))
                {
                  break;
                }
              _this->invalidUCharLength = 0;
          }               
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }

    }

  *target += myTargetIndex;
  *source += mySourceIndex;


  return;
}

UChar32 T_UConverter_getNextUChar_SBCS(UConverter* converter,
                                               const char** source,
                                               const char* sourceLimit,
                                               UErrorCode* err)
{
  UChar myUChar;

  
  if ((*source)+1 > sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }
  
  
  /*Gets the corresponding codepoint*/
  myUChar = converter->sharedData->table->sbcs.toUnicode[(unsigned char)*((*source)++)];
  
  if (myUChar != 0xFFFD) return myUChar;
  else
    {      
      UChar* myUCharPtr = &myUChar;
      const char* sourceFinal = *source;

      /* Do the fallback stuff */
      if ((converter->useFallback == TRUE)&&
          (converter->sharedData->staticData->hasToUnicodeFallback == TRUE))
      {
          myUChar = converter->sharedData->table->sbcs.toUnicodeFallback[ (unsigned char)*((*source)-1)];
          if (myUChar != 0xFFFD) return myUChar;
      }

      *err = U_INVALID_CHAR_FOUND;
      
      /*Calls the ErrorFunctor after rewinding the input buffer*/
      (*source)--;
      /*It's is very likely that the ErrorFunctor will write to the
       *internal buffers */
      converter->fromCharErrorBehaviour(converter,
                                        &myUCharPtr,
                                        myUCharPtr + 1,
                                        &sourceFinal,
                                        sourceLimit,
                                        NULL,
                                        TRUE,
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

void   T_UConverter_toUnicode_DBCS (UConverter * _this,
                                    UChar ** target,
                                    const UChar * targetLimit,
                                    const char **source,
                                    const char *sourceLimit,
                                    int32_t *offsets,
                                    bool_t flush,
                                    UErrorCode * err)
{
  const char *mySource = ( char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  CompactShortArray *myToUnicode = NULL, *myToUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;

  myToUnicode = &_this->sharedData->table->dbcs.toUnicode;
  myToUnicodeFallback = &_this->sharedData->table->dbcs.toUnicodeFallback;

  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) mySource[mySourceIndex++];

          /*We have no internal state, we should */
          if (_this->toUnicodeStatus == 0x00)
            {
              _this->toUnicodeStatus = (unsigned char) mySourceChar;
            }
          else
            {
              if (_this->toUnicodeStatus != 0x00)
                {
                  mySourceChar = (UChar) ((_this->toUnicodeStatus << 8) | (mySourceChar & 0x00FF));
                  _this->toUnicodeStatus = 0x00;
                }

              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar != missingUCharMarker)
                {
                  /*writes the UniChar to the output stream */
                  myTarget[myTargetIndex++] = targetUniChar;
                }
              else if ((_this->useFallback == TRUE) &&
                  (_this->sharedData->staticData->hasToUnicodeFallback == TRUE))
              {
                  targetUniChar = (UChar) ucmp16_getu(myToUnicodeFallback, mySourceChar);
                  if (targetUniChar != missingUCharMarker)
                  {
                      myTarget[myTargetIndex++] = targetUniChar;
                  }
              }
              if (targetUniChar == missingUCharMarker)
                {

                  *err = U_INVALID_CHAR_FOUND;
                  _this->invalidCharBuffer[0] = (char) (mySourceChar >> 8);
                  _this->invalidCharBuffer[1] = (char) mySourceChar;
                  _this->invalidCharLength = 2;
                  
                  ToU_CALLBACK_MACRO(_this,
                                     myTarget,
                                     myTargetIndex, 
                                     targetLimit,
                                     mySource, 
                                     mySourceIndex,
                                     sourceLimit,
                                     offsets,
                                     flush,
                                     err);

                  if (U_FAILURE (*err))   break;
                  _this->invalidCharLength = 0;
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
  if ((flush == TRUE)
      && (mySourceIndex == sourceLength)
      && (_this->toUnicodeStatus != 0x00))
    {
       
      if (U_SUCCESS(*err)) 
        {
          *err = U_TRUNCATED_CHAR_FOUND;
          _this->toUnicodeStatus = 0x00;
        }
    }

  *target += myTargetIndex;
  *source += mySourceIndex;

  return;
}

void   T_UConverter_fromUnicode_DBCS (UConverter * _this,
                                      char **target,
                                      const char *targetLimit,
                                      const UChar ** source,
                                      const UChar * sourceLimit,
                                      int32_t *offsets,
                                      bool_t flush,
                                      UErrorCode * err)
{
  const UChar *mySource = *source;
  unsigned char *myTarget = (unsigned char *) *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - (char *) myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myFromUnicode = NULL, *myFromUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;

  myFromUnicode = &_this->sharedData->table->dbcs.fromUnicode;
  myFromUnicodeFallback = &_this->sharedData->table->dbcs.fromUnicodeFallback;

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
                  _this->charErrorBuffer[0] = (char) targetUniChar;
                  _this->charErrorBufferLength = 1;
                  *err = U_INDEX_OUTOFBOUNDS_ERROR;
                }
            }
          else if ((_this->useFallback == TRUE) &&
                  (_this->sharedData->staticData->hasFromUnicodeFallback == TRUE))
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
                        _this->charErrorBuffer[0] = (char) targetUniChar;
                        _this->charErrorBufferLength = 1;
                        *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      }
                }
          }
          if (targetUniChar == missingCharMarker)  
          {
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar) mySourceChar;
              _this->invalidUCharLength = 1;


/* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
              FromU_CALLBACK_MACRO(_this,
                                   (char *)myTarget, 
                                   myTargetIndex,
                                   targetLimit, 
                                   mySource,
                                   mySourceIndex, 
                                   sourceLimit,
                                   offsets, 
                                   flush, 
                                   err);

              if (U_FAILURE (*err)) break;
              _this->invalidUCharLength = 0;
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

UChar32 T_UConverter_getNextUChar_DBCS(UConverter* converter,
                                               const char** source,
                                               const char* sourceLimit,
                                               UErrorCode* err)
{
  UChar myUChar;
  
  /*Checks boundaries and set appropriate error codes*/
  if ((*source)+2 > sourceLimit) 
    {
      if ((*source) >= sourceLimit)
        {
          /*Either caller has reached the end of the byte stream*/
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
        }
      else if (((*source)+1) == sourceLimit)
        {
          /* a character was cut in half*/
          *err = U_TRUNCATED_CHAR_FOUND;
        }
      
      return 0xFFFD;
    }
  
  /*Gets the corresponding codepoint*/
  myUChar = ucmp16_getu((&converter->sharedData->table->dbcs.toUnicode),
                        (uint16_t)(((UChar)((**source)) << 8) |((uint8_t)*((*source)+1))));
  
  /*update the input pointer*/
  *source += 2;
  if (myUChar != 0xFFFD) return myUChar;
  else
    {      
      UChar* myUCharPtr = &myUChar;
      const char* sourceFinal = *source;

      /* rewinding the input buffer*/
      (*source) -= 2;
      /* Do the fallback stuff */
      if ((converter->useFallback == TRUE) &&
          (converter->sharedData->staticData->hasToUnicodeFallback == TRUE))
      {
          myUChar = ucmp16_getu((&converter->sharedData->table->dbcs.toUnicodeFallback),
                            (uint16_t)(((UChar)((**source)) << 8) |((uint8_t)*((*source)-1))));
          if (myUChar != 0xFFFD) 
          {
              *source += 2;
              return myUChar;
          }
      }
      
      *err = U_INVALID_CHAR_FOUND;
    
      /*It's is very likely that the ErrorFunctor will write to the
       *internal buffers */
      converter->fromCharErrorBehaviour(converter,
                                        &myUCharPtr,
                                        myUCharPtr + 1,
                                        &sourceFinal,
                                        sourceLimit,
                                        NULL,
                                        TRUE,
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
