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

static void T_UConverter_toUnicode_MBCS (UConverter * _this,
                               UChar ** target,
                               const UChar * targetLimit,
                               const char **source,
                               const char *sourceLimit,
                               int32_t *offsets,
                               UBool flush,
                               UErrorCode * err)
{
  const char *mySource = *source, *srcTemp;
  UChar *myTarget = *target, *tgtTemp;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myToUnicode = NULL, *myToUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UBool *myStarters = NULL;
  UConverterToUnicodeArgs args;


  args.sourceStart = *source;
  myToUnicode = &_this->sharedData->table->mbcs.toUnicode;
  myToUnicodeFallback = &_this->sharedData->table->mbcs.toUnicodeFallback;
  myStarters = _this->sharedData->table->mbcs.starters;

  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (mySource[mySourceIndex++]);


          if (myStarters[(uint8_t) mySourceChar] &&
              (_this->toUnicodeStatus == 0x00))
            {
              _this->toUnicodeStatus = (unsigned char) mySourceChar;
            }
          else
            {
              /*In case there is a state, we update the source char
               *by concatenating the previous char with the current
               *one
               */

              if (_this->toUnicodeStatus != 0x00)
                {
                  mySourceChar |= (UChar) (_this->toUnicodeStatus << 8);

                  _this->toUnicodeStatus = 0x00;
                }

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar != missingUCharMarker)
                {
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
                  if (mySourceChar > 0xff)
                    {
                      _this->invalidCharLength = 2;
                      _this->invalidCharBuffer[0] = (char) (mySourceChar >> 8);
                      _this->invalidCharBuffer[1] = (char) mySourceChar;
                    }
                  else
                    {
                      _this->invalidCharLength = 1;
                      _this->invalidCharBuffer[0] = (char) mySourceChar;
                    }
                  

                  args.converter = _this;
                  srcTemp = mySource + mySourceIndex;
                  tgtTemp = myTarget + myTargetIndex;
                  args.pTarget = &tgtTemp;
                  args.targetLimit = targetLimit;
                  args.pSource = &srcTemp;
                  args.sourceLimit = sourceLimit;
                  args.flush = flush;
                  args.offsets = offsets+myTargetIndex;
                  args.size = sizeof(args);

                  /* to do hsys: add more smarts to the codeUnits and length later */
                  ToU_CALLBACK_MACRO(_this->toUContext,
                                     args,
                                     srcTemp,
                                     1, 
                                     UCNV_UNASSIGNED,
                                     err);

                  if (U_FAILURE (*err))    break;
                  _this->invalidCharLength = 0;
                  myTargetIndex = *(args.pTarget) - myTarget;
                  mySourceIndex = *(args.pSource) - mySource;
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
  if (_this->toUnicodeStatus
      && (mySourceIndex == sourceLength)
      && (flush == TRUE))
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

static void T_UConverter_toUnicode_MBCS_OFFSETS_LOGIC (UConverter * _this,
                                                UChar ** target,
                                                const UChar * targetLimit,
                                                const char **source,
                                                const char *sourceLimit,
                                                int32_t *offsets,
                                                UBool flush,
                                                UErrorCode * err)
{
  const char *mySource = *source, *srcTemp;
  UChar *myTarget = *target, *tgtTemp;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myToUnicode = NULL, *myToUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UChar oldMySourceChar = 0x0000;
  UBool *myStarters = NULL;
  UConverterToUnicodeArgs args;
  
  args.sourceStart = *source;
  myToUnicode = &_this->sharedData->table->mbcs.toUnicode;
  myToUnicodeFallback = &_this->sharedData->table->mbcs.toUnicodeFallback;
  myStarters = _this->sharedData->table->mbcs.starters;
 
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (mySource[mySourceIndex++]);


          if (myStarters[(uint8_t) mySourceChar] &&
              (_this->toUnicodeStatus == 0x00))
            {
              _this->toUnicodeStatus = (unsigned char) mySourceChar;
            }
          else
            {
              /*In case there is a state, we update the source char
               *by concatenating the previous char with the current
               *one
               */

              if (_this->toUnicodeStatus != 0x00)
                {
                  mySourceChar |= (UChar) (_this->toUnicodeStatus << 8);

                  _this->toUnicodeStatus = 0x00;
                }

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);
                  

              /*writing the UniChar to the output stream */
              if (targetUniChar != missingUCharMarker)
                {
                  /*writes the UniChar to the output stream */
                  {
                          

                         if (targetUniChar > 0x00FF)
                         offsets[myTargetIndex] = mySourceIndex -2; /* double byte character - make the offset point to the first char */
                        else
                         offsets[myTargetIndex] = mySourceIndex -1 ;  /* single byte char. Offset is OK */
                        

                  }
                myTarget[myTargetIndex++] = targetUniChar;
                oldMySourceChar  = mySourceChar;

                }
              else if ((_this->useFallback == TRUE) &&
                  (_this->sharedData->staticData->hasToUnicodeFallback == TRUE))
              {

                  targetUniChar = (UChar) ucmp16_getu (myToUnicodeFallback, mySourceChar);
                  /*writes the UniChar to the output stream */
                  {                          
                         if (targetUniChar > 0x00FF)
                         offsets[myTargetIndex] = mySourceIndex -2; /* double byte character - make the offset point to the first char */
                        else
                         offsets[myTargetIndex] = mySourceIndex -1 ;  /* single byte char. Offset is OK */                        

                  }
                myTarget[myTargetIndex++] = targetUniChar;
                oldMySourceChar  = mySourceChar;
              }
              if (targetUniChar == missingUCharMarker)  
              {
                  int32_t currentOffset = offsets[myTargetIndex-1] + ((oldMySourceChar>0x00FF)?2:1);
                          
                  *err = U_INVALID_CHAR_FOUND;
                  if (mySourceChar > 0xff)
                    {
                      _this->invalidCharLength = 2;
                      _this->invalidCharBuffer[0] = (char) (mySourceChar >> 8);
                      _this->invalidCharBuffer[1] = (char) mySourceChar;
                    }
                  else
                    {
                      _this->invalidCharLength = 1;
                      _this->invalidCharBuffer[0] = (char) mySourceChar;
                    }
                  args.converter = _this;
                  srcTemp = mySource + mySourceIndex;
                  tgtTemp = myTarget + myTargetIndex;
                  args.pTarget = &tgtTemp;
                  args.targetLimit = targetLimit;
                  args.pSource = &srcTemp;
                  args.sourceLimit = sourceLimit;
                  args.flush = flush;
                  args.offsets = offsets+myTargetIndex;
                  args.size = sizeof(args);

                  /* to do hsys: add more smarts to the codeUnits and length later and offsets */
                  ToU_CALLBACK_MACRO(_this->toUContext,
                                     args,
                                     srcTemp,
                                     1, 
                                     UCNV_UNASSIGNED,
                                     err);
          
                  if (U_FAILURE (*err))    break;
                  _this->invalidCharLength = 0;
                  myTargetIndex = *(args.pTarget) - myTarget;
                  mySourceIndex = *(args.pSource) - mySource;
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
  if (_this->toUnicodeStatus
      && (mySourceIndex == sourceLength)
      && (flush == TRUE))
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

static void   T_UConverter_fromUnicode_MBCS (UConverter * _this,
                                      char **target,
                                      const char *targetLimit,
                                      const UChar ** source,
                                      const UChar * sourceLimit,
                                      int32_t *offsets,
                                      UBool flush,
                                      UErrorCode * err)

{
  const UChar *mySource = *source, *srcTemp;
  char *myTarget = *target, *tgtTemp;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myFromUnicode = NULL, *myFromUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UConverterFromUnicodeArgs args;

  args.sourceStart = *source;
  myFromUnicode = &_this->sharedData->table->mbcs.fromUnicode;
  myFromUnicodeFallback = &_this->sharedData->table->mbcs.fromUnicodeFallback;

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
                      _this->charErrorBuffer[0] = (char) targetUniChar;
                      _this->charErrorBufferLength = 1;
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                    }
                }
          } 
          else if ((_this->useFallback == TRUE) &&
              (_this->sharedData->staticData->hasFromUnicodeFallback == TRUE))
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
                          _this->charErrorBuffer[0] = (char) targetUniChar;
                          _this->charErrorBufferLength = 1;
                          *err = U_INDEX_OUTOFBOUNDS_ERROR;
                        }
                    }
              } 
          }
          if (targetUniChar == missingCharMarker)
            {
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar) mySourceChar;
              _this->invalidUCharLength = 1;

              srcTemp = mySource + mySourceIndex;
              tgtTemp = myTarget + myTargetIndex;
              args.converter = _this;
              args.pTarget = &tgtTemp;
              args.targetLimit = targetLimit;
              args.pSource = &srcTemp;
              args.sourceLimit = sourceLimit;
              args.flush = flush;
              args.offsets = (offsets)?offsets+myTargetIndex:0;
              args.size = sizeof(args);
/* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
              /* HSYS: to do: more smarts */
              FromU_CALLBACK_MACRO(args.converter->fromUContext,
                                     args,
                                     srcTemp,
                                     1,
                                     (UChar32) (*srcTemp),
                                     UCNV_UNASSIGNED,
                                     err);
              if (U_FAILURE (*err)) break;
              myTargetIndex = *(args.pTarget) - myTarget;
              mySourceIndex = *(args.pSource) - mySource;
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

static void   T_UConverter_fromUnicode_MBCS_OFFSETS_LOGIC (UConverter * _this,
                                                    char **target,
                                                    const char *targetLimit,
                                                    const UChar ** source,
                                                    const UChar * sourceLimit,
                                                    int32_t *offsets,
                                                    UBool flush,
                                                    UErrorCode * err)

{
  const UChar *mySource = *source, *srcTemp;
  char *myTarget = *target, *tgtTemp;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myFromUnicode = NULL, *myFromUnicodeFallback = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UConverterFromUnicodeArgs args;

  args.sourceStart = *source;
  myFromUnicode = &_this->sharedData->table->mbcs.fromUnicode;
  myFromUnicodeFallback = &_this->sharedData->table->mbcs.fromUnicodeFallback;

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
                   offsets[myTargetIndex] = mySourceIndex-1;
                  myTarget[myTargetIndex++] = (char) targetUniChar;

                }
              else
                {
                   offsets[myTargetIndex] = mySourceIndex-1;
                  myTarget[myTargetIndex++] = (char) (targetUniChar >> 8);
                  if (myTargetIndex < targetLength)
                    {
                       offsets[myTargetIndex] = mySourceIndex-1;
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
          else if ((_this->useFallback == TRUE) &&
              (_this->sharedData->staticData->hasFromUnicodeFallback == TRUE))
          {
              targetUniChar = (UChar) ucmp16_getu (myFromUnicodeFallback, mySourceChar);
              if (targetUniChar != missingCharMarker)
                {
                  if (targetUniChar <= 0x00FF)
                    {
                       offsets[myTargetIndex] = mySourceIndex-1;
                      myTarget[myTargetIndex++] = (char) targetUniChar;

                    }
                  else
                    {
                       offsets[myTargetIndex] = mySourceIndex-1;
                      myTarget[myTargetIndex++] = (char) (targetUniChar >> 8);
                      if (myTargetIndex < targetLength)
                        {
                           offsets[myTargetIndex] = mySourceIndex-1;
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
          }

          if (targetUniChar == missingCharMarker)
            {
              int32_t currentOffset = mySourceIndex -1;
              
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar) mySourceChar;
              _this->invalidUCharLength = 1;


              srcTemp = mySource + mySourceIndex;
              tgtTemp = myTarget + myTargetIndex;
              args.converter = _this;
              args.pTarget = &tgtTemp;
              args.targetLimit = targetLimit;
              args.pSource = &srcTemp;
              args.sourceLimit = sourceLimit;
              args.flush = flush;
              args.offsets = offsets+myTargetIndex;
              args.size = sizeof(args);
/* Needed explicit cast for myTarget on MVS to make compiler happy - JJD */
              /* HSYS: to do: more smarts including offsets*/
              FromU_CALLBACK_MACRO(args.converter->fromUContext,
                                     args,
                                     srcTemp,
                                     1,
                                     (UChar32) (*srcTemp),
                                     UCNV_UNASSIGNED,
                                     err);
              
              if (U_FAILURE (*err)) break;
              myTargetIndex = *(args.pTarget) - myTarget;
              mySourceIndex = *(args.pSource) - mySource;
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

static UChar32 T_UConverter_getNextUChar_MBCS(UConverter* converter,
                                               const char** source,
                                               const char* sourceLimit,
                                               UErrorCode* err)
{
  UChar myUChar;
  char const *sourceInitial = *source;
  UConverterToUnicodeArgs args;
  /*safe keeps a ptr to the beginning in case we need to step back*/
  
  /*Input boundary check*/
  if ((*source)+1 > sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }
  args.sourceStart = *source;
  /*Checks to see if the byte is a lead*/
  if (converter->sharedData->table->mbcs.starters[(uint8_t)**source] == FALSE)
    {
      /*Not lead byte: we update the source ptr and get the codepoint*/
      myUChar = ucmp16_getu((&converter->sharedData->table->mbcs.toUnicode),
                            (UChar)(**source));
      if ((converter->useFallback == TRUE) &&
          (converter->sharedData->staticData->hasToUnicodeFallback == TRUE) && 
          (myUChar == 0xFFFD))
      {
          myUChar = ucmp16_getu((&converter->sharedData->table->mbcs.toUnicodeFallback),
                            (UChar)(**source));
      }
      (*source)++;
    }
  else
    {
      /*Lead byte: we Build the codepoint and get the corresponding character
       * and update the source ptr*/
      if ((*source + 2) > sourceLimit) 
        {
          *err = U_TRUNCATED_CHAR_FOUND;
          return 0xFFFD;
        }

      myUChar = ucmp16_getu((&converter->sharedData->table->mbcs.toUnicode),
                            (uint16_t)(((UChar)((**source)) << 8) |((uint8_t)*((*source)+1))));

      if ((converter->useFallback == TRUE) && 
          (converter->sharedData->staticData->hasToUnicodeFallback == TRUE) &&
          (myUChar == 0xFFFD))
      {
      myUChar = ucmp16_getu((&converter->sharedData->table->mbcs.toUnicodeFallback),
                            (uint16_t)(((UChar)((**source)) << 8) |((uint8_t)*((*source)+1))));
      }
      (*source) += 2;
    }
  
  if (myUChar != 0xFFFD) return myUChar;
  else
    {      
      /*rewinds source*/
      const char* sourceFinal = *source;
      UChar* myUCharPtr = &myUChar;
      
      *err = U_INVALID_CHAR_FOUND;
      *source = sourceInitial;
      
      /*It's is very likely that the ErrorFunctor will write to the
       *internal buffers */
      args.converter = converter;
      args.pTarget = &myUCharPtr;
      args.targetLimit = myUCharPtr + 1;
      args.pSource = &sourceFinal;
      args.sourceLimit = sourceLimit;
      args.flush = TRUE;
      args.offsets = NULL;  
      args.size = sizeof(args);
      converter->fromCharErrorBehaviour(converter->toUContext,
                                    &args,
                                    sourceFinal,
                                    1,
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
