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
*/

#include "unicode/utypes.h"
#include "cmemory.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_bld.h"
#include "unicode/ucnv.h"
#include "ucnv_cnv.h"

/* MBCS --------------------------------------------------------------------- */

static void
_MBCSLoad(UConverterSharedData *sharedData, const uint8_t *raw, UErrorCode *pErrorCode) {
    const uint8_t *oldraw;

    sharedData->table->mbcs.starters = (bool_t*)raw;
    oldraw = raw += sizeof(bool_t)*256;

    sharedData->table->mbcs.toUnicode   = ucmp16_cloneFromData(&raw, pErrorCode);
    if(((raw-oldraw)&3)!=0) {
        raw+=4-((raw-oldraw)&3);    /* pad to 4 */
    }
    sharedData->table->mbcs.fromUnicode = ucmp16_cloneFromData(&raw, pErrorCode);
}

static void
_MBCSUnload(UConverterSharedData *sharedData) {
    ucmp16_close (sharedData->table->mbcs.fromUnicode);
    ucmp16_close (sharedData->table->mbcs.toUnicode);
	uprv_free (sharedData->table);
}

void T_UConverter_toUnicode_MBCS (UConverter * _this,
                               UChar ** target,
                               const UChar * targetLimit,
                               const char **source,
                               const char *sourceLimit,
                               int32_t *offsets,
                               bool_t flush,
                               UErrorCode * err)
{
  const char *mySource = *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  bool_t *myStarters = NULL;




  myToUnicode = _this->sharedData->table->mbcs.toUnicode;
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
              else
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

                  if (U_FAILURE (*err))    break;
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

void T_UConverter_toUnicode_MBCS_OFFSETS_LOGIC (UConverter * _this,
                                                UChar ** target,
                                                const UChar * targetLimit,
                                                const char **source,
                                                const char *sourceLimit,
                                                int32_t *offsets,
                                                bool_t flush,
                                                UErrorCode * err)
{
  const char *mySource = *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;
  UChar oldMySourceChar;
  bool_t *myStarters = NULL;
  int32_t* originalOffsets = offsets;



  myToUnicode = _this->sharedData->table->mbcs.toUnicode;
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
              else
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

                  ToU_CALLBACK_OFFSETS_LOGIC_MACRO(_this,
                                                   myTarget,
                                                   myTargetIndex, 
                                                   targetLimit,
                                                   mySource, 
                                                   mySourceIndex,
                                                   sourceLimit,
                                                   offsets,
                                                   flush,
                                                   err);
          
                  if (U_FAILURE (*err))    break;
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

void   T_UConverter_fromUnicode_MBCS (UConverter * _this,
                                      char **target,
                                      const char *targetLimit,
                                      const UChar ** source,
                                      const UChar * sourceLimit,
                                      int32_t *offsets,
                                      bool_t flush,
                                      UErrorCode * err)

{
  const UChar *mySource = *source;
  char *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myFromUnicode = NULL;
  UChar targetUniChar = 0x0000;
  int8_t targetUniCharByteNum = 0;
  UChar mySourceChar = 0x0000;

  myFromUnicode = _this->sharedData->table->mbcs.fromUnicode;

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
          else
            {
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar) mySourceChar;
              _this->invalidUCharLength = 1;

              FromU_CALLBACK_MACRO(_this,
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

void   T_UConverter_fromUnicode_MBCS_OFFSETS_LOGIC (UConverter * _this,
                                                    char **target,
                                                    const char *targetLimit,
                                                    const UChar ** source,
                                                    const UChar * sourceLimit,
                                                    int32_t *offsets,
                                                    bool_t flush,
                                                    UErrorCode * err)

{
  const UChar *mySource = *source;
  char *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - mySource;
  CompactShortArray *myFromUnicode = NULL;
  UChar targetUniChar = 0x0000;
  int8_t targetUniCharByteNum = 0;
  UChar mySourceChar = 0x0000;
  int32_t* originalOffsets = offsets;

  myFromUnicode = _this->sharedData->table->mbcs.fromUnicode;

  

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
          else
            {
              int32_t currentOffset = mySourceIndex -1;
              int32_t* offsetsAnchor = offsets;
              
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar) mySourceChar;
              _this->invalidUCharLength = 1;

              FromU_CALLBACK_OFFSETS_LOGIC_MACRO(_this,
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

UChar T_UConverter_getNextUChar_MBCS(UConverter* converter,
                                               const char** source,
                                               const char* sourceLimit,
                                               UErrorCode* err)
{
  UChar myUChar;
  char const *sourceInitial = *source;
  /*safe keeps a ptr to the beginning in case we need to step back*/
  
  /*Input boundary check*/
  if ((*source)+1 > sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }
  
  /*Checks to see if the byte is a lead*/
  if (converter->sharedData->table->mbcs.starters[(uint8_t)**source] == FALSE)
    {
      /*Not lead byte: we update the source ptr and get the codepoint*/
      myUChar = ucmp16_getu(converter->sharedData->table->mbcs.toUnicode,
                            (UChar)(**source));
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

      myUChar = ucmp16_getu(converter->sharedData->table->mbcs.toUnicode,
                            (uint16_t)(((UChar)((**source)) << 8) |((uint8_t)*((*source)+1))));

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

void
_MBCSGetStarters(const UConverter* converter, bool_t starters[256], UErrorCode *pErrorCode) {
    /* fills in the starters boolean array */
    uprv_memcpy(starters, converter->sharedData->table->mbcs.starters, 256*sizeof(bool_t));
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

extern const UConverterSharedData _MBCSData={
    sizeof(UConverterSharedData), 1,
    NULL, NULL, &_MBCSImpl, "MBCS",
    0, UCNV_IBM, UCNV_MBCS, 1, 1,
    { 0, 1, 0, 0, 0, 0 }
};
