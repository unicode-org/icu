/*
*******************************************************************************
*
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*
*   uconv_cnv.c:
*   Implements all the low level conversion functions
*   T_UnicodeConverter_{to,from}Unicode_$ConversionType
*
*/

#include "unicode/utypes.h"
#include "uhash.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_bld.h"
#include "unicode/ucnv_err.h"
#include "ucnv_cnv.h"
#include "unicode/ucnv.h"
#include "cmemory.h"

#ifdef Debug
#include <stdio.h>
#endif





void flushInternalUnicodeBuffer (UConverter * _this,
                                 UChar * myTarget,
                                 int32_t * myTargetIndex,
                                 int32_t targetLength,
                                 int32_t** offsets,
                                 UErrorCode * err);

void flushInternalCharBuffer (UConverter * _this,
                              char *myTarget,
                              int32_t * myTargetIndex,
                              int32_t targetLength,
                              int32_t** offsets,
                              UErrorCode * err);

#define FromU_CALLBACK_MACRO(_this, myTarget, myTargetIndex, targetLimit, mySource, mySourceIndex, sourceLimit, offsets, flush, err) \
              if (_this->fromUCharErrorBehaviour == (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_STOP) break;\
              else \
                { \
                  char *myTargetCopy = myTarget + myTargetIndex; \
                  const UChar *mySourceCopy = mySource + mySourceIndex; \
                  /*copies current values for the ErrorFunctor to update */ \
                  /*Calls the ErrorFunctor */ \
                  _this->fromUCharErrorBehaviour (_this, \
                                                  (char **) &myTargetCopy, \
                                                  targetLimit, \
                                                  (const UChar **) &mySourceCopy, \
                                                  sourceLimit, \
                                                  offsets, \
                                                  flush, \
                                                  err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                  mySourceIndex = (mySourceCopy - mySource) ; \
                  myTargetIndex = (char*)myTargetCopy - (char*)myTarget ; \
                }

#define ToU_CALLBACK_MACRO(_this, myTarget, myTargetIndex, targetLimit, mySource, mySourceIndex, sourceLimit, offsets, flush, err) \
              if (_this->fromCharErrorBehaviour == (UConverterToUCallback) UCNV_TO_U_CALLBACK_STOP) break; \
              else \
                { \
                  UChar *myTargetCopy = myTarget + myTargetIndex; \
                  const char *mySourceCopy = mySource + mySourceIndex; \
                  /*Calls the ErrorFunctor */ \
                  _this->fromCharErrorBehaviour (_this, \
                                                 &myTargetCopy, \
                                                 targetLimit, \
                                              (const char **) &mySourceCopy, \
                                                 sourceLimit, \
                                                 offsets, \
                                                 flush, \
                                                 err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                  mySourceIndex = ((char*)mySourceCopy - (char*)mySource); \
                  myTargetIndex = (myTargetCopy - myTarget); \
                }

#define FromU_CALLBACK_OFFSETS_LOGIC_MACRO(_this, myTarget, myTargetIndex, targetLimit, mySource, mySourceIndex, sourceLimit, offsets, flush, err) \
              if (_this->fromUCharErrorBehaviour == (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_STOP) break;\
              else \
                { \
                  char *myTargetCopy = myTarget + myTargetIndex; \
                  const UChar *mySourceCopy = mySource + mySourceIndex; \
                 int32_t My_i = myTargetIndex; \
                  /*copies current values for the ErrorFunctor to update */ \
                  /*Calls the ErrorFunctor */ \
                  _this->fromUCharErrorBehaviour (_this, \
                                                  (char **) &myTargetCopy, \
                                                  targetLimit, \
                                                  (const UChar **) &mySourceCopy, \
                                                  sourceLimit, \
                                                  offsets + myTargetIndex, \
                                                  flush, \
                                                  err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                  mySourceIndex = mySourceCopy - mySource ; \
                  myTargetIndex = (char*)myTargetCopy - (char*)myTarget ; \
                  for (;My_i < myTargetIndex;My_i++) offsets[My_i] += currentOffset  ;    \
                }



#define ToU_CALLBACK_OFFSETS_LOGIC_MACRO(_this, myTarget, myTargetIndex, targetLimit, mySource, mySourceIndex, sourceLimit, offsets, flush, err) \
              if (_this->fromCharErrorBehaviour == (UConverterToUCallback) UCNV_TO_U_CALLBACK_STOP) break; \
              else \
                { \
                  UChar *myTargetCopy = myTarget + myTargetIndex; \
                  const char *mySourceCopy = mySource + mySourceIndex; \
                                  int32_t My_i = myTargetIndex; \
                              _this->fromCharErrorBehaviour (_this, \
                                                 &myTargetCopy, \
                                                 targetLimit, \
                                              (const char **) &mySourceCopy, \
                                                 sourceLimit, \
                                                 offsets + myTargetIndex, \
                                                 flush, \
                                                 err); \
                  /*Update the local Indexes so that the conversion can restart at the right points */ \
                  mySourceIndex = (char *)mySourceCopy - (char*)mySource; \
                  myTargetIndex = ((UChar*)myTargetCopy - (UChar*)myTarget);  \
                  for (;My_i < myTargetIndex;My_i++) {offsets[My_i] += currentOffset  ;   } \
                }

                             

/* UTF-8 Conversion DATA
 *   for more information see Unicode Strandard 2.0 , Transformation Formats Appendix A-9
 */
const uint32_t kReplacementCharacter = 0x0000FFFD;
const uint32_t kMaximumUCS2 = 0x0000FFFF;
const uint32_t kMaximumUTF16 = 0x0010FFFF;
const uint32_t kMaximumUCS4 = 0x7FFFFFFF;
const int8_t halfShift = 10;
const uint32_t halfBase = 0x0010000;
const uint32_t halfMask = 0x3FF;
const uint32_t kSurrogateHighStart = 0xD800;
const uint32_t kSurrogateHighEnd = 0xDBFF;
const uint32_t kSurrogateLowStart = 0xDC00;
const uint32_t kSurrogateLowEnd = 0xDFFF;

const uint32_t offsetsFromUTF8[7] = {0,
  (uint32_t) 0x00000000, (uint32_t) 0x00003080, (uint32_t) 0x000E2080,
  (uint32_t) 0x03C82080, (uint32_t) 0xFA082080, (uint32_t) 0x82082080
};

#define ESC_2022 0x1B /*ESC*/
typedef enum 
{
  INVALID_2022 = -1, /*Doesn't correspond to a valid iso 2022 escape sequence*/
  VALID_NON_TERMINAL_2022 = 0, /*so far corresponds to a valid iso 2022 escape sequence*/
  VALID_TERMINAL_2022 = 1, /*corresponds to a valid iso 2022 escape sequence*/
  VALID_MAYBE_TERMINAL_2022 = 2 /*so far matches one iso 2022 escape sequence, but by adding more characters might match another escape sequence*/
} UCNV_TableStates_2022;

/*Below are the 3 arrays depicting a state transition table*/
int8_t normalize_esq_chars_2022[256] = {
         0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,1      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,4      ,7      ,0      ,0
        ,2      ,0      ,0      ,0      ,0      ,3      ,0      ,6      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,5      ,8      ,9      ,10     ,11     ,12
        ,13     ,14     ,15     ,16     ,17     ,18     ,19     ,20     ,0      ,0
        ,0      ,0      ,21     ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,22     ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0      ,0
        ,0      ,0      ,0      ,0      ,0      ,0};
#define MAX_STATES_2022 54
int32_t escSeqStateTable_Key_2022[MAX_STATES_2022] = {
         1      ,34     ,36     ,39     ,1093   ,1096   ,1097   ,1098   ,1099   ,1100
        ,1101   ,1102   ,1103   ,1104   ,1105   ,1106   ,1109   ,1154   ,1157   ,1160
        ,1161   ,1254   ,1257   ,35105  ,36933  ,36936  ,36937  ,36938  ,36939  ,36940
        ,36942  ,36943  ,36944  ,36945  ,36946  ,36947  ,36948  ,40133  ,40136  ,40138
        ,40139  ,40140  ,40141  ,1123363        ,35947624       ,35947625       ,35947626       ,35947627       ,35947629       ,35947630
        ,35947631       ,35947635       ,35947636       ,35947638};

const char* escSeqStateTable_Result_2022[MAX_STATES_2022] = {
         NULL   ,NULL   ,NULL   ,NULL    ,"latin1"    ,"latin1"    ,"latin1"    ,"ibm-865"    ,"ibm-865"    ,"ibm-865"
    ,"ibm-865"    ,"ibm-865"    ,"ibm-865"    ,"ibm-895"    ,"ibm-943"    ,"latin1"    ,"latin1"        ,NULL    ,"ibm-955"    ,"ibm-367"
    ,"ibm-952"  ,NULL    ,"UTF8"        ,NULL    ,"ibm-955"    ,"bm-367"    ,"ibm-952"    ,"ibm-949"    ,"ibm-953"    ,"ibm-1383"
    ,"ibm-952"    ,"ibm-964"    ,"ibm-964"    ,"ibm-964"    ,"ibm-964"    ,"ibm-964"    ,"ibm-964"    ,"UTF16_PlatformEndian"    ,"UTF16_PlatformEndian"    ,"UTF16_PlatformEndian"
    ,"UTF16_PlatformEndian"    ,"UTF16_PlatformEndian"    ,"UTF16_PlatformEndian"    ,NULL    ,"latin1"    ,"ibm-912"    ,"ibm-913"    ,"ibm-914"    ,"ibm-813"    ,"ibm-1089"
    ,"ibm-920"    ,"ibm-915"    ,"ibm-915"    ,"latin1"};

UCNV_TableStates_2022 escSeqStateTable_Value_2022[MAX_STATES_2022] = {
         VALID_NON_TERMINAL_2022        ,VALID_NON_TERMINAL_2022        ,VALID_NON_TERMINAL_2022        ,VALID_NON_TERMINAL_2022        ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_MAYBE_TERMINAL_2022      ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022
        ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_NON_TERMINAL_2022        ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022
        ,VALID_TERMINAL_2022    ,VALID_NON_TERMINAL_2022        ,VALID_TERMINAL_2022    ,VALID_NON_TERMINAL_2022        ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022
        ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022
        ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_NON_TERMINAL_2022        ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022
        ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022    ,VALID_TERMINAL_2022};

/*for 2022 looks ahead in the stream
 *to determine the longest possible convertible
 *data stream*/
static const char* getEndOfBuffer_2022(const char* source,
                                       const char* sourceLimit,
                                       bool_t flush); 
/*runs through a state machine to determine the escape sequence - codepage correspondance
 *changes the pointer pointed to be _this->extraInfo*/
static  void changeState_2022(UConverter* _this,
                             const char** source, 
                             const char* sourceLimit,
                             bool_t flush,
                             UErrorCode* err); 

UCNV_TableStates_2022 getKey_2022(char source,
                                  int32_t* key,
                                  int32_t* offset);

/* END OF UTF-8 Conversion DATA */

const int8_t bytesFromUTF8[256] = {
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
  2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
  3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 4, 4, 4, 4, 4, 4, 4, 4, 5, 5, 5, 5, 6, 6, 0, 0
};

const unsigned char firstByteMark[7] = {0x00, 0x00, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC};
#define missingCharMarker 0xFFFF
#define missingUCharMarker 0xFFFD



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
  UChar *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  
  myToUnicode = _this->sharedData->table->sbcs.toUnicode;

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
  CompactShortArray *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;

  myToUnicode = _this->sharedData->table->dbcs.toUnicode;

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
              else
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



void  T_UConverter_toUnicode_LATIN_1 (UConverter * _this,
                                      UChar ** target,
                                      const UChar * targetLimit,
                                      const char **source,
                                      const char *sourceLimit,
                                      int32_t *offsets,
                                      bool_t flush,
                                      UErrorCode * err)
{
  unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  int32_t readLen = 0;
  int32_t i = 0;

  /*Since there is no risk of encountering illegal Chars
   *we need to pad our latin1 chars to create Unicode codepoints
   *we need to go as far a min(targetLen, sourceLen)
   *in case we don't have enough buffer space
   *we set the error flag accordingly
   */
  if ((targetLimit - *target) < sourceLength)
    {
      readLen = targetLimit - *target;
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
    }
  else
    {
      readLen = sourceLimit - (char *) mySource;
    }

  for (i = 0; i < readLen; i++) myTarget[i] = (UChar) mySource[i];

  *target += i;
  *source += i;
  return;
}

void   T_UConverter_fromUnicode_LATIN_1 (UConverter * _this,
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

  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {

      if (myTargetIndex < targetLength)
        {
          if (mySource[mySourceIndex] < 0x0100)
            {
              /*writes the char to the output stream */
              myTarget[myTargetIndex++] = (char) mySource[mySourceIndex++];
            }
          else
            {
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar) mySource[mySourceIndex++];
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
  CompactByteArray *myFromUnicode;
  unsigned char targetChar = 0x00;

  myFromUnicode = _this->sharedData->table->sbcs.fromUnicode;

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
          else
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

void T_UConverter_toUnicode_EBCDIC_STATEFUL (UConverter * _this,
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
  int32_t myMode = _this->mode;


  myToUnicode = _this->sharedData->table->dbcs.toUnicode;

    while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (mySource[mySourceIndex++]);
          if (mySourceChar == UCNV_SI) myMode = UCNV_SI;
          else if (mySourceChar == UCNV_SO) myMode = UCNV_SO;
         else if ((myMode == UCNV_SO) &&
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
              else mySourceChar &= 0x00FF;

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar != missingUCharMarker)
                {
                  /*writes the UniChar to the output stream */
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
                  _this->mode = myMode;
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

                  if (U_FAILURE (*err))  break;
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
  _this->mode = myMode;

  return;
}


void T_UConverter_toUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC (UConverter * _this,
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
  int32_t myMode = _this->mode;
  int32_t* originalOffsets = offsets;


  myToUnicode = _this->sharedData->table->dbcs.toUnicode;

    while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) (mySource[mySourceIndex++]);
          if (mySourceChar == UCNV_SI) myMode = UCNV_SI;
          else if (mySourceChar == UCNV_SO) myMode = UCNV_SO;
          else if ((myMode == UCNV_SO) &&
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
              else mySourceChar &= 0x00FF;

              /*gets the corresponding Unicode codepoint */
              targetUniChar = (UChar) ucmp16_getu (myToUnicode, mySourceChar);

              /*writing the UniChar to the output stream */
              if (targetUniChar != missingUCharMarker)
                {
                  /*writes the UniChar to the output stream */
                  {
                        if(myMode == UCNV_SO)
                         offsets[myTargetIndex] = mySourceIndex-2; /* double byte */
                        else
                         offsets[myTargetIndex] = mySourceIndex-1; /* single byte */
                  }
                  myTarget[myTargetIndex++] = targetUniChar;
                }
              else
                {
                  int32_t currentOffset = offsets[myTargetIndex-1] + 2;/* Because mySourceIndex was already incremented */
                  
                  *err = U_INVALID_CHAR_FOUND;
                  if (mySourceChar > 0xFF)
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
                  _this->mode = myMode;
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
  _this->mode = myMode;

  return;
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


void T_UConverter_fromUnicode_EBCDIC_STATEFUL (UConverter * _this,
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
  bool_t isTargetUCharDBCS = (bool_t)_this->fromUnicodeStatus;
  bool_t oldIsTargetUCharDBCS = isTargetUCharDBCS;
  myFromUnicode = _this->sharedData->table->dbcs.fromUnicode;
  
  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);
          oldIsTargetUCharDBCS = isTargetUCharDBCS;
          isTargetUCharDBCS = (targetUniChar>0x00FF);
          
          if (targetUniChar != missingCharMarker)
            {
              if (oldIsTargetUCharDBCS != isTargetUCharDBCS)
                {
                  if (isTargetUCharDBCS) myTarget[myTargetIndex++] = UCNV_SO;
                  else myTarget[myTargetIndex++] = UCNV_SI;
                  
                  
                  if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength))
                    {
                      _this->charErrorBuffer[0] = (char) targetUniChar;
                      _this->charErrorBufferLength = 1;
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      break;
                    }
                  else if (myTargetIndex+1 >= targetLength)
                    {
                      _this->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      _this->charErrorBuffer[1] = (char) targetUniChar & 0x00FF;
                      _this->charErrorBufferLength = 2;
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      break;
                    }
                  
                }
              
              if (!isTargetUCharDBCS)
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
                      break;
                    }
                }
            }
          else
            {
              isTargetUCharDBCS = oldIsTargetUCharDBCS;
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar) mySourceChar;
              _this->invalidUCharLength = 1;

              _this->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;
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
  *source += mySourceIndex;
  
  _this->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;

  return;
}

void T_UConverter_fromUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC (UConverter * _this,
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
  bool_t isTargetUCharDBCS = (bool_t)_this->fromUnicodeStatus;
  bool_t oldIsTargetUCharDBCS = isTargetUCharDBCS;
  int32_t* originalOffsets = offsets;
  
  myFromUnicode = _this->sharedData->table->dbcs.fromUnicode;
  
  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);
          oldIsTargetUCharDBCS = isTargetUCharDBCS;
          isTargetUCharDBCS = (targetUniChar>0x00FF);
          
          if (targetUniChar != missingCharMarker)
            {
              if (oldIsTargetUCharDBCS != isTargetUCharDBCS)
                {
                  offsets[myTargetIndex] = mySourceIndex-1;
                  if (isTargetUCharDBCS) myTarget[myTargetIndex++] = UCNV_SO;
                  else myTarget[myTargetIndex++] = UCNV_SI;
                  
                  
                  if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength))
                    {
                      _this->charErrorBuffer[0] = (char) targetUniChar;
                      _this->charErrorBufferLength = 1;
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      break;
                    }
                  else if (myTargetIndex+1 >= targetLength)
                    {
                      _this->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      _this->charErrorBuffer[1] = (char) targetUniChar & 0x00FF;
                      _this->charErrorBufferLength = 2;
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      break;
                    }
                }
              
              if (!isTargetUCharDBCS)
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
                      break;
                    }
                }
            }
          else
            {
              int32_t currentOffset = offsets[myTargetIndex-1]+1;
              *err = U_INVALID_CHAR_FOUND;
              _this->invalidUCharBuffer[0] = (UChar) mySourceChar;
              _this->invalidUCharLength = 1;

              /* Breaks out of the loop since behaviour was set to stop */
              _this->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;
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
              
              if (U_FAILURE (*err))     break;
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
  
  _this->fromUnicodeStatus = (int32_t)isTargetUCharDBCS;

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
void T_UConverter_fromUnicode_ISO_2022(UConverter* _this,
                                       char** target,
                                       const char* targetLimit,
                                       const UChar** source,
                                       const UChar* sourceLimit,
                                       int32_t *offsets,
                                       bool_t flush,
                                       UErrorCode* err)
{
  char const* targetStart = *target;
  T_UConverter_fromUnicode_UTF8(_this,
                                target,
                                targetLimit,
                                source,
                                sourceLimit,
                                NULL,
                                flush,
                                err);
}


void T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC(UConverter* _this,
                                                     char** target,
                                                     const char* targetLimit,
                                                     const UChar** source,
                                                     const UChar* sourceLimit,
                                                     int32_t *offsets,
                                                     bool_t flush,
                                                     UErrorCode* err)
{

  char const* targetStart = *target;
  T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC(_this,
                                              target,
                                              targetLimit,
                                              source,
                                              sourceLimit,
                                              offsets,
                                              flush,
                                              err);
  {
    int32_t len = *target - targetStart;
    int32_t i;
    /* uprv_memmove(offsets+3, offsets, len);   MEMMOVE SEEMS BROKEN --srl */
    
    for(i=len-1;i>=0;i--)       offsets[i] = offsets[i];
    
  }
}

UCNV_TableStates_2022 getKey_2022(char c,
                                  int32_t* key,
                                  int32_t* offset)
{
  int32_t togo = *key;
  int32_t low = 0;
  int32_t hi = MAX_STATES_2022;
  int32_t oldmid;
  
  if (*key == 0)  togo = normalize_esq_chars_2022[c];
  else
    {
      togo <<= 5;
      togo += normalize_esq_chars_2022[c];
    }
  
  while (hi != low)  /*binary search*/
    {
      register int32_t mid = (hi+low) >> 1; /*Finds median*/
      
      if (mid == oldmid) break;
      if (escSeqStateTable_Key_2022[mid] > togo)  hi = mid;
      else if (escSeqStateTable_Key_2022[mid] < togo)  low = mid;
      else /*we found it*/
        {
          *key = togo;
          *offset = mid;
#ifdef Debug
        printf("found at @ %d\n", mid);
#endif /*Debug*/
          return escSeqStateTable_Value_2022[mid];
        }
      oldmid = mid;
      
    }

#ifdef Debug  
  printf("Could not find \"%d\" for %X\n", togo, c);
#endif /*Debug*/
  *key = 0;
  *offset = 0;
  

  return INVALID_2022;
}

void changeState_2022(UConverter* _this,
                      const char** source, 
                      const char* sourceLimit,
                      bool_t flush,
                      UErrorCode* err)
{
  UConverter* myUConverter;
  uint32_t key = _this->toUnicodeStatus;
  UCNV_TableStates_2022 value;
  UConverterDataISO2022* myData2022 = ((UConverterDataISO2022*)_this->extraInfo);
  const char* chosenConverterName = NULL;
  int32_t offset;
  
  /*Close the old Converter*/
  if (_this->mode == UCNV_SO) ucnv_close(myData2022->currentConverter);
  myData2022->currentConverter = NULL;
  _this->mode = UCNV_SI;
  
  /*In case we were in the process of consuming an escape sequence
    we need to reprocess it */
  
  do
    {
#ifdef Debug
      printf("Pre Stage: char = %x, key = %d, value =%d\n", **source, key, value);
      fflush(stdout);
#endif /*Debug*/
/* Needed explicit cast for key on MVS to make compiler happy - JJD */
      value = getKey_2022(**source,(int32_t *) &key, &offset);
#ifdef Debug
      printf("Post Stage: char = %x, key = %d, value =%d\n", **source, key, value);
      fflush(stdout);
#endif /*Debug*/
      switch (value)
        {
        case VALID_NON_TERMINAL_2022 : 
          {
#ifdef Debug
            puts("VALID_NON_TERMINAL_2022");
#endif /*Debug*/
          };break;
          
        case VALID_TERMINAL_2022:
          {
#ifdef Debug
            puts("VALID_TERMINAL_2022");
#endif /*Debug*/
            chosenConverterName = escSeqStateTable_Result_2022[offset];
            key = 0;
            goto DONE;
          };break;
          
        case INVALID_2022:
          {
#ifdef Debug        
            puts("INVALID_2022");
#endif /*Debug*/
            _this->toUnicodeStatus = 0;
            *err = U_ILLEGAL_CHAR_FOUND;
            return;
          }
          
        case VALID_MAYBE_TERMINAL_2022:
          {
            const char* mySource = (*source + 1);
            int32_t myKey = key;
            UCNV_TableStates_2022 myValue = value;
            int32_t myOffset;
#ifdef Debug        
            puts("VALID_MAYBE_TERMINAL_2022");
#endif /*Debug*/

            while ((mySource < sourceLimit) && 
                   ((myValue == VALID_MAYBE_TERMINAL_2022)||(myValue == VALID_NON_TERMINAL_2022)))
              {
#ifdef Debug
                printf("MAYBE value = %d myKey = %d %X\n", myValue, myKey, *mySource);
#endif /*Debug*/
                myValue = getKey_2022(*(mySource++), &myKey, &myOffset);
              }
#ifdef Debug        
            printf("myValue = %d\n", myValue);
#endif /*Debug*/
            switch (myValue)
              {
              case INVALID_2022:
                {
                  /*Backs off*/
#ifdef Debug        
                  puts("VALID_MAYBE_TERMINAL INVALID");
                  printf("offset = %d\n", offset);
#endif /*Debug*/
                  chosenConverterName = escSeqStateTable_Result_2022[offset];
                  value = VALID_TERMINAL_2022;
#ifdef Debug        
                  printf("%d\n", offset);
                  fflush(stdout);
#endif /*Debug*/
                  goto DONE;
                };break;
              
              case VALID_TERMINAL_2022:
                {
                  /*uses longer escape sequence*/
#ifdef Debug
                  puts("VALID_MAYBE_TERMINAL TERMINAL");
#endif /*Debug*/
                  *source = mySource-1; /*deals with the overshot in the while above*/
                  chosenConverterName = escSeqStateTable_Result_2022[myOffset];
                  key = 0;
                  value = VALID_TERMINAL_2022;
                  goto DONE;
                };break;
              
              case VALID_NON_TERMINAL_2022: 
#ifdef Debug
                puts("VALID_MAYBE_TERMINAL NON_TERMINAL");
#endif /*Debug*/
              case VALID_MAYBE_TERMINAL_2022:
                {
#ifdef Debug
                  puts("VALID_MAYBE_TERMINAL MAYBE_TERMINAL");
#endif /*Debug*/
                  if (flush) 
                    {
                      /*Backs off*/
                      chosenConverterName = escSeqStateTable_Result_2022[offset];
                      value = VALID_TERMINAL_2022;
                      key = 0;
                      goto DONE;
                    }
                  else
                    {
                      key = myKey;
                      value = VALID_NON_TERMINAL_2022;
                    }
                };break;
              };break;
          };break;
        }
    }  while ((*source)++ <= sourceLimit);
  
 DONE:
  _this->toUnicodeStatus = key;
  
  if ((value == VALID_NON_TERMINAL_2022) || (value == VALID_MAYBE_TERMINAL_2022)) 
    {
#ifdef Debug
      printf("Out: current **source = %X", **source);
#endif

      return;
    }
  if (value > 0) myData2022->currentConverter = myUConverter = ucnv_open(chosenConverterName, err);
  {
#ifdef Debug
    printf("Error = %d open \"%s\"\n", *err, chosenConverterName);
#endif /*Debug*/
    if (U_SUCCESS(*err)) 
      {
        /*Customize the converter with the attributes set on the 2022 converter*/
        myUConverter->fromUCharErrorBehaviour = _this->fromUCharErrorBehaviour;
        myUConverter->fromCharErrorBehaviour = _this->fromCharErrorBehaviour;
        uprv_memcpy(myUConverter->subChar, 
                   _this->subChar,
                   myUConverter->subCharLen = _this->subCharLen);
        
        _this->mode = UCNV_SO;
      }
  }
 
  
  return;
}

/*Checks the first 3 characters of the buffer against valid 2022 escape sequences
 *if the match we return a pointer to the initial start of the sequence otherwise
 *we return sourceLimit
 */
const char* getEndOfBuffer_2022(const char* source,
                                const char* sourceLimit,
                                bool_t flush)
{
  const char* mySource = source;
  
  if (source >= sourceLimit) return sourceLimit;
  
  do
    {
      if (*mySource == ESC_2022)
        {
          int8_t i;
          int32_t key = 0;
          int32_t offset;
          UCNV_TableStates_2022 value = VALID_NON_TERMINAL_2022;
          
          for (i=0; 
               (mySource+i < sourceLimit)&&(value == VALID_NON_TERMINAL_2022);
               i++) 
            {
              value =  getKey_2022(*(mySource+i), &key, &offset);
#ifdef Debug
              printf("Look ahead value = %d\n", value);
#endif /*Debug*/
            }
          if (value > 0) return mySource;
          if ((value == VALID_NON_TERMINAL_2022)&&(!flush) ) return sourceLimit;
        }
    }
  while (mySource++ < sourceLimit);
  
  return sourceLimit;
}
                  
  

void T_UConverter_toUnicode_ISO_2022(UConverter* _this,
                                     UChar** target,
                                     const UChar* targetLimit,
                                     const char** source,
                                     const char* sourceLimit,
                                     int32_t *offsets,
                                     bool_t flush,
                                     UErrorCode* err)
{
  int32_t base = 0;
  const char* mySourceLimit;
  char const* sourceStart;
  
  /*Arguments Check*/
  if (U_FAILURE(*err)) return;
  if ((_this == NULL) || (targetLimit < *target) || (sourceLimit < *source))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
  
  for (;;)
    {

       mySourceLimit =  getEndOfBuffer_2022(*source, sourceLimit, flush); 
      

      /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
      if (_this->mode == UCNV_SO) /*Already doing some conversion*/
        {
          const UChar* myTargetStart = *target;
#ifdef Debug
          printf("source %X\n mySourceLimit %X\n sourceLimit %X\n", *source, mySourceLimit, sourceLimit); 
#endif /*Debug*/
          
          ucnv_toUnicode(((UConverterDataISO2022*)(_this->extraInfo))->currentConverter,
                         target,
                         targetLimit,
                         source,
                         mySourceLimit,
                         NULL,
                         flush,
                         err);

          
#ifdef Debug      
          puts("---------------------------> CONVERTED");
          printf("source %X\n mySourceLimit %X\n sourceLimit %X\n", *source, mySourceLimit, sourceLimit); 
          printf("err =%d", *err);
#endif /*Debug*/
        }
      /*-Done with buffer with entire buffer
        -Error while converting
      */
        
      if (U_FAILURE(*err) || (*source == sourceLimit)) return;
#ifdef Debug            
      puts("Got Here!");
      fflush(stdout);
#endif /*Debug*/
      sourceStart = *source;
      changeState_2022(_this,
                       source, 
                       sourceLimit,
                       flush,
                       err);
      (*source)++;

    }
  
  return;
}

void T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC(UConverter* _this,
                                                   UChar** target,
                                                   const UChar* targetLimit,
                                                   const char** source,
                                                   const char* sourceLimit,
                                                   int32_t *offsets,
                                                   bool_t flush,
                                                   UErrorCode* err)
{
  int32_t myOffset=0;
  int32_t base = 0;
  const char* mySourceLimit;
  char const* sourceStart;
  
  /*Arguments Check*/
  if (U_FAILURE(*err)) return;
  if ((_this == NULL) || (targetLimit < *target) || (sourceLimit < *source))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
  
  for (;;)
    {

      mySourceLimit =  getEndOfBuffer_2022(*source, sourceLimit, flush); 
      /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/

      if (_this->mode == UCNV_SO) /*Already doing some conversion*/
        {
          const UChar* myTargetStart = *target;
#ifdef Debug
          printf("source %X\n mySourceLimit %X\n sourceLimit %X\n", *source, mySourceLimit, sourceLimit); 
#endif /*Debug*/
          
          ucnv_toUnicode(((UConverterDataISO2022*)(_this->extraInfo))->currentConverter,
                         target,
                         targetLimit,
                         source,
                         mySourceLimit,
                         offsets,
                         flush,
                         err);
          
            {
              int32_t lim =  *target - myTargetStart;
              int32_t i = 0;
              for (i=base; i < lim;i++)    offsets[i] += myOffset;
              base += lim;
            }
          
#ifdef Debug      
          puts("---------------------------> CONVERTED");
          printf("source %X\n mySourceLimit %X\n sourceLimit %X\n", *source, mySourceLimit, sourceLimit); 
          printf("err =%d", *err);
#endif /*Debug*/
        }

      /*-Done with buffer with entire buffer
        -Error while converting
      */
        
      if (U_FAILURE(*err) || (*source == sourceLimit)) return;
#ifdef Debug            
      puts("Got Here!");
      fflush(stdout);
#endif /*Debug*/
      sourceStart = *source;
      changeState_2022(_this,
                       source, 
                       sourceLimit,
                       flush,
                       err);
       (*source)++;
       myOffset += *source - sourceStart;

    }
  
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
  CompactShortArray *myFromUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;

  myFromUnicode = _this->sharedData->table->dbcs.fromUnicode;

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
          else
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

void T_UConverter_fromUnicode_UTF8 (UConverter * _this,
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
  int8_t targetCharByteNum = 0;
  UChar mySourceChar = 0x0000;
  uint32_t ch;
  int16_t i, bytesToWrite = 0;
  uint32_t ch2;
  char temp[4];

  if (_this->fromUnicodeStatus)
    {
      ch = _this->fromUnicodeStatus;
      _this->fromUnicodeStatus = 0;
      goto lowsurogate;
    }
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          bytesToWrite = 0;
          ch = mySource[mySourceIndex++];

          if (ch < 0x80)        /* Single byte */
            {
              myTarget[myTargetIndex++] = (char) ch;
            }
          else if (ch < 0x800)  /* Double byte */
            {
              myTarget[myTargetIndex++] = (char) ((ch >> 6) | 0xc0);
              if (myTargetIndex < targetLength)
                {
                  myTarget[myTargetIndex++] = (char) ((ch & 0x3f) | 0x80);
                }
              else
                {
                  _this->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                  _this->charErrorBufferLength = 1;
                  *err = U_INDEX_OUTOFBOUNDS_ERROR;
                }
            }
          else
            /* Check for surogates */
            {
              if ((ch >= kSurrogateHighStart) && (ch <= kSurrogateHighEnd))
                {
                lowsurogate:
                  if (mySourceIndex < sourceLength && !flush)
                    {
                      ch2 = mySource[mySourceIndex];
                      if ((ch2 >= kSurrogateLowStart) && (ch2 <= kSurrogateLowEnd))
                        {
                          ch = ((ch - kSurrogateHighStart) << halfShift) + (ch2 - kSurrogateLowStart) + halfBase;
                          ++mySourceIndex;
                        }
                    }
                }
              if (ch < 0x10000)
                {
                  bytesToWrite = 3;
                  temp[0] = (char) ((ch >> 12) | 0xe0);
                  temp[1] = (char) ((ch >> 6) & 0x3f | 0x80);
                  temp[2] = (char) (ch & 0x3f | 0x80);
                }
              else
                {
                  bytesToWrite = 4;
                  temp[0] = (char) ((ch >> 18) | 0xf0);
                  temp[1] = (char) ((ch >> 12) & 0x3f | 0xe0);
                  temp[2] = (char) ((ch >> 6) & 0x3f | 0x80);
                  temp[3] = (char) (ch & 0x3f | 0x80);
                }
              for (i = 0; i < bytesToWrite; i++)
                {
                  if (myTargetIndex < targetLength)
                    {
                      myTarget[myTargetIndex++] = temp[i];
                    }
                  else
                    {
                      _this->charErrorBuffer[_this->charErrorBufferLength++] = temp[i];
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                    }
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

void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverter * _this,
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
  int8_t targetCharByteNum = 0;
  UChar mySourceChar = 0x0000;
  uint32_t ch;
  int16_t i, bytesToWrite = 0;
  uint32_t ch2;
  char temp[4];

  if (_this->fromUnicodeStatus)
    {
      ch = _this->fromUnicodeStatus;
      _this->fromUnicodeStatus = 0;
      goto lowsurogate;
    }
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          bytesToWrite = 0;
          ch = mySource[mySourceIndex++];

          if (ch < 0x80)        /* Single byte */
            {
               offsets[myTargetIndex] = mySourceIndex-1;
              myTarget[myTargetIndex++] = (char) ch;
            }
          else if (ch < 0x800)  /* Double byte */
            {
               offsets[myTargetIndex] = mySourceIndex-1;
              myTarget[myTargetIndex++] = (char) ((ch >> 6) | 0xc0);
              if (myTargetIndex < targetLength)
                {
                   offsets[myTargetIndex] = mySourceIndex-1;
                  myTarget[myTargetIndex++] = (char) ((ch & 0x3f) | 0x80);
                }
              else
                {
                  _this->charErrorBuffer[0] = (char) ((ch & 0x3f) | 0x80);
                  _this->charErrorBufferLength = 1;
                  *err = U_INDEX_OUTOFBOUNDS_ERROR;
                }
            }
          else
            /* Check for surogates */
            {
              if ((ch >= kSurrogateHighStart) && (ch <= kSurrogateHighEnd))
                {
                lowsurogate:
                  if (mySourceIndex < sourceLength && !flush)
                    {
                      ch2 = mySource[mySourceIndex];
                      if ((ch2 >= kSurrogateLowStart) && (ch2 <= kSurrogateLowEnd))
                        {
                          ch = ((ch - kSurrogateHighStart) << halfShift) + (ch2 - kSurrogateLowStart) + halfBase;
                          ++mySourceIndex;
                        }
                    }
                }
              if (ch < 0x10000)
                {
                  bytesToWrite = 3;
                  temp[0] = (char) ((ch >> 12) | 0xe0);
                  temp[1] = (char) ((ch >> 6) & 0x3f | 0x80);
                  temp[2] = (char) (ch & 0x3f | 0x80);
                }
              else
                {
                  bytesToWrite = 4;
                  temp[0] = (char) ((ch >> 18) | 0xf0);
                  temp[1] = (char) ((ch >> 12) & 0x3f | 0xe0);
                  temp[2] = (char) ((ch >> 6) & 0x3f | 0x80);
                  temp[3] = (char) (ch & 0x3f | 0x80);
                }
              for (i = 0; i < bytesToWrite; i++)
                {
                  if (myTargetIndex < targetLength)
                    {
                       offsets[myTargetIndex] = mySourceIndex-1;
                      myTarget[myTargetIndex++] = temp[i];
                    }
                  else
                    {
                      _this->charErrorBuffer[_this->charErrorBufferLength++] = temp[i];
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                    }
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


void  T_UConverter_fromUnicode_UTF16_BE (UConverter * _this,
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
  UChar mySourceChar;

  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {

      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];
          myTarget[myTargetIndex++] = (char) (mySourceChar >> 8);
          if (myTargetIndex < targetLength)
            {
              myTarget[myTargetIndex++] = (char) mySourceChar;
            }
          else
            {
              _this->charErrorBuffer[0] = (char) mySourceChar;
              _this->charErrorBufferLength = 1;
              *err = U_INDEX_OUTOFBOUNDS_ERROR;
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

void   T_UConverter_fromUnicode_UTF16_LE (UConverter * _this,
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
  UChar mySourceChar;


  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {

      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) mySource[mySourceIndex++];
          myTarget[myTargetIndex++] = (char) mySourceChar;
          if (myTargetIndex < targetLength)
            {
              myTarget[myTargetIndex++] = (char) (mySourceChar >> 8);
            }
          else
            {
              _this->charErrorBuffer[0] = (char) (mySourceChar >> 8);
              _this->charErrorBufferLength = 1;
              *err = U_INDEX_OUTOFBOUNDS_ERROR;
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

void T_UConverter_toUnicode_UTF16_BE (UConverter * _this,
                                      UChar ** target,
                                      const UChar * targetLimit,
                                      const char **source,
                                      const char *sourceLimit,
                                      int32_t *offsets,
                                      bool_t flush,
                                      UErrorCode * err)
{
  const unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  UChar mySourceChar = 0x0000;
  UChar oldmySourceChar = 0x0000;


  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UChar */
          mySourceChar = (unsigned char) mySource[mySourceIndex++];
           oldmySourceChar = mySourceChar;
          if (_this->toUnicodeStatus == 0)
            {
              _this->toUnicodeStatus = (unsigned char) mySourceChar == 0x00 ? 0xFFFF : mySourceChar;
            }
          else
            {
              if (_this->toUnicodeStatus != 0xFFFF)
                mySourceChar = (UChar) ((_this->toUnicodeStatus << 8) | mySourceChar);
              _this->toUnicodeStatus = 0;



              myTarget[myTargetIndex++] = mySourceChar;

            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

  if (U_SUCCESS(*err) && flush
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

void  T_UConverter_toUnicode_UTF16_LE (UConverter * _this,
                                       UChar ** target,
                                       const UChar * targetLimit,
                                       const char **source,
                                       const char *sourceLimit,
                                       int32_t *offsets,
                                       bool_t flush,
                                       UErrorCode * err)
{
  const unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  CompactShortArray *myToUnicode = NULL;
  UChar targetUniChar = 0x0000;
  UChar mySourceChar = 0x0000;

  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          /*gets the corresponding UniChar */
          mySourceChar = (unsigned char) mySource[mySourceIndex++];

          if (_this->toUnicodeStatus == 0x00)
            {
              _this->toUnicodeStatus = (unsigned char) mySourceChar == 0x00 ? 0xFFFF : mySourceChar;
            }
          else
            {
              if (_this->toUnicodeStatus == 0xFFFF)
                mySourceChar = (UChar) (mySourceChar << 8);
              else
                {
                  mySourceChar <<= 8;
                  mySourceChar |= (UChar) (_this->toUnicodeStatus);
                }
              _this->toUnicodeStatus = 0x00;
              myTarget[myTargetIndex++] = mySourceChar;
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }


  if (U_SUCCESS(*err) && flush
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

void T_UConverter_toUnicode_UTF8 (UConverter * _this,
                                  UChar ** target,
                                  const UChar * targetLimit,
                                  const char **source,
                                  const char *sourceLimit,
                                  int32_t *offsets,
                                  bool_t flush,
                                  UErrorCode * err)
{
  const unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  uint32_t ch = 0 ,
           ch2 =0 ,
           i =0;            /* Index into the current # of bytes consumed in the current sequence */
  uint32_t inBytes = 0;  /* Total number of bytes in the current UTF8 sequence */
  
  if (_this->toUnicodeStatus)
    {
      i = _this->invalidCharLength;   /* restore # of bytes consumed */
      inBytes = _this->toUnicodeStatus; /* Restore size of current sequence */

      ch = _this->mode; /*Stores the previously calculated ch from a previous call*/
      _this->toUnicodeStatus = 0;
      _this->invalidCharLength = 0;
      goto morebytes;
    }


  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          ch = 0;
          ch = ((uint32_t)mySource[mySourceIndex++]) & 0x000000FF;
          if (ch < 0x80)        /* Simple case */
            {
              myTarget[myTargetIndex++] = (UChar) ch;
            }
          else
            {
              /* store the first char */

              inBytes = bytesFromUTF8[ch]; /* lookup current sequence length */
              _this->invalidCharBuffer[0] = (char)ch;
              i = 1;

            morebytes:
              for (; i < inBytes; i++)
                {
                  {
                    if (mySourceIndex >= sourceLength)
                      {
                        if (flush)
                          {
                            if (U_SUCCESS(*err)) 
                              {
                                *err = U_TRUNCATED_CHAR_FOUND;
                                _this->toUnicodeStatus = 0x00;
                              }
                          }
                        else
                          {
                            _this->toUnicodeStatus = inBytes;
                            _this->invalidCharLength = (int8_t)i;
                          }
                        goto donefornow;
                      }
                    _this->invalidCharBuffer[i] = (char) (ch2 = (((uint32_t)mySource[mySourceIndex++]) & 0x000000FF));
                    if ((ch2 & 0xC0) != 0x80)   /* Invalid trailing byte */
                      break;
                  }
                  ch <<= 6;
                  ch += ch2;
                }


              ch -= offsetsFromUTF8[inBytes];

              if (i == inBytes && ch <= kMaximumUTF16) 
                {
                  if (ch <= kMaximumUCS2) 
                    {
                      myTarget[myTargetIndex++] = (UChar) ch;
                    }
                  else
                    {
                      ch -= halfBase;
                      myTarget[myTargetIndex++] = (UChar) ((ch >> halfShift) + kSurrogateHighStart);
                      ch = (ch & halfMask) + kSurrogateLowStart;
                      if (myTargetIndex < targetLength)
                        {
                          myTarget[myTargetIndex++] = (char)ch;
                        }
                      else
                        {
                          _this->invalidUCharBuffer[0] = (UChar) ch;
                          _this->invalidUCharLength = 1;
                          *err = U_INDEX_OUTOFBOUNDS_ERROR;
                        }
                    }
                }
              else
                {
                  *err = U_ILLEGAL_CHAR_FOUND;
                  _this->invalidCharLength = (int8_t)i;
                  
#ifdef Debug
                  printf("inbytes %d\n, _this->invalidCharLength = %d,\n mySource[mySourceIndex]=%X\n", inBytes, _this->invalidCharLength, mySource[mySourceIndex]);
#endif
/* Needed explicit cast for mySource on MVS to make compiler happy - JJD */
                  ToU_CALLBACK_MACRO(_this,
                                     myTarget,
                                     myTargetIndex, 
                                     targetLimit,
                                     (const char *)mySource, 
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
        /* End of target buffer */
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

donefornow:
  *target += myTargetIndex;
  *source += mySourceIndex;
  _this->mode = ch; /*stores a partially calculated target*/
}

void T_UConverter_toUnicode_UTF8_OFFSETS_LOGIC (UConverter * _this,
                                                UChar ** target,
                                                const UChar * targetLimit,
                                                const char **source,
                                                const char *sourceLimit,
                                                int32_t *offsets,
                                                bool_t flush,
                                                UErrorCode * err)
{
  const unsigned char *mySource = (unsigned char *) *source;
  UChar *myTarget = *target;
  int32_t mySourceIndex = 0;
  int32_t myTargetIndex = 0;
  int32_t targetLength = targetLimit - myTarget;
  int32_t sourceLength = sourceLimit - (char *) mySource;
  uint32_t ch = 0, ch2 = 0, i = 0;
  uint32_t inBytes = 0;
  int32_t* originalOffsets = offsets;


  
  if (_this->toUnicodeStatus)
    {
      i = _this->invalidCharLength;
      inBytes = _this->toUnicodeStatus;
      _this->toUnicodeStatus = 0;
      ch = _this->mode;
      goto morebytes;
    }

  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          ch = mySource[mySourceIndex++];
          if (ch < 0x80)        /* Simple case */
            {
               offsets[myTargetIndex] = mySourceIndex-1;
              myTarget[myTargetIndex++] = (UChar) ch;
            }
          else
            {
              inBytes = bytesFromUTF8[ch];
              _this->invalidCharBuffer[0] = (char)ch;
              i = 1;

            morebytes:
              for (; i < inBytes; i++)
                {
                  {
                    if (mySourceIndex >= sourceLength)
                      {
                        if (flush)
                          {
                            if (U_SUCCESS(*err)) 
                              {
                                *err = U_TRUNCATED_CHAR_FOUND;
                                _this->toUnicodeStatus = 0x00;
                              }
                          }
                        else
                          {
                            _this->toUnicodeStatus = inBytes;
                            _this->invalidCharLength = (int8_t)i;
                          }
                        goto donefornow;
                      }
                    _this->invalidCharBuffer[i] = (char) (ch2 = mySource[mySourceIndex++]);
                    if ((ch2 & 0xC0) != 0x80)   /* Invalid trailing byte */
                      break;
                  }
                  ch <<= 6;
                  ch += ch2;
                }

              ch -= offsetsFromUTF8[inBytes];
              if (i == inBytes && ch <= kMaximumUTF16)
                {
                  if (ch <= kMaximumUCS2) {

                     offsets[myTargetIndex] = mySourceIndex-3;
                    myTarget[myTargetIndex++] = (UChar) ch;

                  }
                  else
                    {
                      ch -= halfBase;
                       offsets[myTargetIndex] = mySourceIndex-4;
                      myTarget[myTargetIndex++] = (UChar) ((ch >> halfShift) + kSurrogateHighStart);
                      ch = (ch & halfMask) + kSurrogateLowStart;
                      if (myTargetIndex < targetLength)
                        {
                           offsets[myTargetIndex] = mySourceIndex-4;
                          myTarget[myTargetIndex++] = (char)ch;
                        }
                      else
                        {
                          _this->invalidUCharBuffer[0] = (UChar) ch;
                          _this->invalidUCharLength = 1;
                          *err = U_INDEX_OUTOFBOUNDS_ERROR;
                        }
                    }
                }
              else
                {
                  int32_t currentOffset = offsets[myTargetIndex-1];

                  *err = U_ILLEGAL_CHAR_FOUND;
                  _this->invalidCharLength = (int8_t)i;
                  
/* Needed explicit cast for mySource on MVS to make compiler happy - JJD */
                  ToU_CALLBACK_OFFSETS_LOGIC_MACRO(_this,
                                                   myTarget,
                                                   myTargetIndex, 
                                                   targetLimit,
                                                   (const char *)mySource, 
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
        /* End of target buffer */
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }
    }

donefornow:
  *target += myTargetIndex;
  *source += mySourceIndex;
  _this->mode = ch;

}

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



UChar T_UConverter_getNextUChar_SBCS(UConverter* converter,
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

UChar T_UConverter_getNextUChar_LATIN_1(UConverter* converter,
                                                  const char** source,
                                                  const char* sourceLimit,
                                                  UErrorCode* err)
{
  
  /* Empties the internal buffers if need be
   * In this case since ErrorFunctors are never called 
   * (LATIN_1 is a subset of Unicode)
   */
  
  if ((*source)+1 > sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }
  
  return  (UChar)*((*source)++);
}

UChar T_UConverter_getNextUChar_ISO_2022(UConverter* converter,
                                     const char** source,
                                     const char* sourceLimit,
                                     UErrorCode* err)
{
  const char* mySourceLimit;
  /*Arguments Check*/
  if  (sourceLimit < *source)
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return 0xFFFD;
    }
  
  for (;;)
    {
      mySourceLimit =  getEndOfBuffer_2022(*source, sourceLimit, TRUE); 
      /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
      if (converter->mode == UCNV_SO) /*Already doing some conversion*/
        {
          
          return ucnv_getNextUChar(((UConverterDataISO2022*)(converter->extraInfo))->currentConverter,
                                   source,
                                   mySourceLimit,
                                   err);
          

        }
      /*-Done with buffer with entire buffer
        -Error while converting
      */
      

      changeState_2022(converter,
                       source, 
                       sourceLimit,
                       TRUE,
                       err);
      (*source)++;
    }
  
  return 0xFFFD;
}

UChar T_UConverter_getNextUChar_DBCS(UConverter* converter,
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
  myUChar = ucmp16_getu(converter->sharedData->table->dbcs.toUnicode,
                        ((UChar)((**source)) << 8) |((uint8_t)*((*source)+1)));
  
  /*update the input pointer*/
  *source += 2;
  if (myUChar != 0xFFFD) return myUChar;
  else
    {      
      UChar* myUCharPtr = &myUChar;
      const char* sourceFinal = *source;

      /*Calls the ErrorFunctor after rewinding the input buffer*/
      (*source) -= 2;
      
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
                            ((UChar)((**source)) << 8) |((uint8_t)*((*source)+1)));

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

UChar T_UConverter_getNextUChar_EBCDIC_STATEFUL(UConverter* converter,
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
  
  /*Checks to see if with have SI/SO shifters
   if we do we change the mode appropriately and we consume the byte*/
  if ((**source == UCNV_SI) || (**source == UCNV_SO)) 
    {
      converter->mode = **source;
      (*source)++;
      
      /*Rechecks boundary after consuming the shift sequence*/
      if ((*source)+1 > sourceLimit) 
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          return 0xFFFD;
        }
    }
  
  if (converter->mode == UCNV_SI)
    {
      /*Not lead byte: we update the source ptr and get the codepoint*/
      myUChar = ucmp16_getu(converter->sharedData->table->dbcs.toUnicode,
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

      myUChar = ucmp16_getu(converter->sharedData->table->dbcs.toUnicode,
                            ((UChar)((**source)) << 8) |((uint8_t)*((*source)+1)));

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

UChar T_UConverter_getNextUChar_UTF16_BE(UConverter* converter,
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

  myUChar = ((uint16_t)((**source)) << 8) |((uint8_t)*((*source)+1));
  *source += 2;
  return myUChar;
} 


UChar T_UConverter_getNextUChar_UTF16_LE(UConverter* converter,
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
  myUChar =  ((uint16_t)*((*source)+1) << 8) |((uint8_t)((**source)));
  /*updates the source*/
  *source += 2;  
  return myUChar;
} 

UChar T_UConverter_getNextUChar_UTF8(UConverter* converter,
                                               const char** source,
                                               const char* sourceLimit,
                                               UErrorCode* err)
{
  UChar myUChar;
  /*safe keeps a ptr to the beginning in case we need to step back*/
  char const *sourceInitial = *source;
  uint16_t extraBytesToWrite;
  uint8_t myByte;
  uint32_t ch;
  int8_t isLegalSequence = 1;

  /*Input boundary check*/
  if ((*source) >= sourceLimit) 
    {
      *err = U_INDEX_OUTOFBOUNDS_ERROR;
      return 0xFFFD;
    }
  
  myByte = (uint8_t)*((*source)++);
  if(myByte < 0x80) {
    return (UChar)myByte;
  }
  extraBytesToWrite = (uint16_t)bytesFromUTF8[myByte];
  if (extraBytesToWrite == 0 || extraBytesToWrite > 4) {
    goto CALL_ERROR_FUNCTION;
  }
  

  /*The byte sequence is longer than the buffer area passed*/

  if ((*source + extraBytesToWrite - 1) > sourceLimit)
    {
      *err = U_TRUNCATED_CHAR_FOUND;
      return 0xFFFD;
    }
  else
    {
      ch = myByte << 6;
      switch(extraBytesToWrite)
        {     
          /* note: code falls through cases! (sic)*/ 
        case 6: ch += (myByte = (uint8_t)*((*source)++)); ch <<= 6;
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
              break;
            }
        case 5: ch += (myByte = *((*source)++)); ch <<= 6;
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
              break;
            }
        case 4: ch += (myByte = *((*source)++)); ch <<= 6;
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
              break;
            }
        case 3: ch += (myByte = *((*source)++)); ch <<= 6;
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
              break;
            }
        case 2: ch += (myByte = *((*source)++));
          if ((myByte & 0xC0) != 0x80) 
            {
              isLegalSequence = 0;
            }
        };
    }
  ch -= offsetsFromUTF8[extraBytesToWrite];

  
  if (isLegalSequence == 0) goto CALL_ERROR_FUNCTION;
  
  /*we got a UCS-2 Character*/
  if (ch <= kMaximumUCS2)  return (UChar)ch;
  /*character out of bounds*/
  else if (ch >= kMaximumUTF16)      goto CALL_ERROR_FUNCTION;
  /*Surrogates found*/
  else 
    {
      ch -= halfBase;
      /*stores the 2nd surrogate inside the converter for the next call*/
      converter->UCharErrorBuffer[0] = (UChar)((ch >> halfShift) + kSurrogateHighStart);
      converter->UCharErrorBufferLength = 1;
      
      /*returns the 1st surrogate*/
      return  (UChar)((ch & halfMask) + kSurrogateLowStart);
    }
  
  
 CALL_ERROR_FUNCTION:
  {      
    /*rewinds source*/
    const char* sourceFinal = *source;
    UChar* myUCharPtr = &myUChar;
    
    *err = U_ILLEGAL_CHAR_FOUND;
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
