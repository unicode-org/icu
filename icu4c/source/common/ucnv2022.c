/*  
**********************************************************************
*   Copyright (C) 2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucnv2022.cpp
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

/* ISO-2022 ----------------------------------------------------------------- */

void T_UConverter_fromUnicode_UTF8 (UConverter * converter,
				    char **target,
				    const char *targetLimit,
				    const UChar ** source,
				    const UChar * sourceLimit,
				    int32_t* offsets,
				    bool_t flush,
				    UErrorCode * err);

void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverter * converter,
				    char **target,
				    const char *targetLimit,
				    const UChar ** source,
				    const UChar * sourceLimit,
				    int32_t* offsets,
				    bool_t flush,
				    UErrorCode * err);

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

static UConverterImpl _ISO2022Impl={
    UCNV_ISO_2022,

    T_UConverter_toUnicode_ISO_2022,
    T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_ISO_2022,
    T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_ISO_2022
};

extern UConverterSharedData _ISO2022Data={
    sizeof(UConverterSharedData), ~0,
    NULL, NULL, &_ISO2022Impl, "ISO_2022",
    2022, UCNV_IBM, UCNV_ISO_2022, 1, 4,
    { 0, 1, 0x1a, 0, 0, 0 }
};

/* EBCDICStateful ----------------------------------------------------------- */

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

static UConverterImpl _EBCDICStatefulImpl={
    UCNV_EBCDIC_STATEFUL,

    T_UConverter_toUnicode_EBCDIC_STATEFUL,
    T_UConverter_toUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_EBCDIC_STATEFUL,
    T_UConverter_fromUnicode_EBCDIC_STATEFUL_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_EBCDIC_STATEFUL
};

extern UConverterSharedData _EBCDICStatefulData={
    sizeof(UConverterSharedData), 1,
    NULL, NULL, &_EBCDICStatefulImpl, "EBCDICStateful",
    0, UCNV_IBM, UCNV_EBCDIC_STATEFUL, 1, 1,
    { 0, 1, 0, 0, 0, 0 }
};
