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
*
*   Change history:
*
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

/* ISO-2022 ----------------------------------------------------------------- */

U_CFUNC void T_UConverter_fromUnicode_UTF8 (UConverterFromUnicodeArgs * args,
				    UErrorCode * err);

U_CFUNC void T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC (UConverterFromUnicodeArgs * converter,
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
                                       UBool flush); 
/*runs through a state machine to determine the escape sequence - codepage correspondance
 *changes the pointer pointed to be _this->extraInfo*/
static  void changeState_2022(UConverter* _this,
                             const char** source, 
                             const char* sourceLimit,
                             UBool flush,
                             UErrorCode* err); 

UCNV_TableStates_2022 getKey_2022(char source,
                                  int32_t* key,
                                  int32_t* offset);

static void
_ISO2022Open(UConverter *cnv, const char *name, const char *locale, UErrorCode *errorCode) {
    cnv->charErrorBufferLength = 3;
    cnv->charErrorBuffer[0] = 0x1b;
    cnv->charErrorBuffer[1] = 0x25;
    cnv->charErrorBuffer[2] = 0x42;
    cnv->extraInfo = uprv_malloc (sizeof (UConverterDataISO2022));
    if(cnv->extraInfo != NULL) {
        ((UConverterDataISO2022 *) cnv->extraInfo)->currentConverter = NULL;
        ((UConverterDataISO2022 *) cnv->extraInfo)->escSeq2022Length = 0;
    } else {
        *errorCode = U_MEMORY_ALLOCATION_ERROR;
    }
}

static void
_ISO2022Close(UConverter *converter) {
    if (converter->extraInfo != NULL) {
        ucnv_close (((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter);
        uprv_free (converter->extraInfo);
    }

}

static void
_ISO2022Reset(UConverter *converter) {
  if (converter->mode == UCNV_SO)
    {
      converter->charErrorBufferLength = 3;
      converter->charErrorBuffer[0] = 0x1b;
      converter->charErrorBuffer[1] = 0x25;
      converter->charErrorBuffer[2] = 0x42;
      ucnv_close (((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter);
      ((UConverterDataISO2022 *) (converter->extraInfo))->currentConverter = NULL;
      ((UConverterDataISO2022 *) (converter->extraInfo))->escSeq2022Length = 0;
      converter->mode = UCNV_SI;
    }
}

U_CFUNC void T_UConverter_fromUnicode_ISO_2022(UConverterFromUnicodeArgs *args,
                                       UErrorCode* err)
{
  T_UConverter_fromUnicode_UTF8(args, err);
}


U_CFUNC void T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC(UConverterFromUnicodeArgs* args,
                                                                    UErrorCode* err)
{

  char const* targetStart = args->target;
  T_UConverter_fromUnicode_UTF8_OFFSETS_LOGIC(args, err);
  {
    int32_t len = args->target - targetStart;
    int32_t i;
    /* uprv_memmove(offsets+3, offsets, len);   MEMMOVE SEEMS BROKEN --srl */
    
    for(i=len-1;i>=0;i--)       args->offsets[i] = args->offsets[i];
    
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
                      UBool flush,
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
      printf("Post Stage: char = %x, key = %d, value =%d\n", *source, key, value);
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
        myUConverter->fromUContext = _this->fromUContext;
        myUConverter->fromCharErrorBehaviour = _this->fromCharErrorBehaviour;
        myUConverter->toUContext = _this->toUContext;

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
                                UBool flush)
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
                  
  

U_CFUNC void T_UConverter_toUnicode_ISO_2022(UConverterToUnicodeArgs *args,
                                     UErrorCode* err)
{
  int32_t base = 0;
  const char *mySourceLimit;
  char const* sourceStart;
  UConverter *saveThis;
  
  /*Arguments Check*/
  if (U_FAILURE(*err)) return;
  if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
  
  for (;;)
    {
      mySourceLimit =  getEndOfBuffer_2022(args->source, args->sourceLimit, args->flush); 
      

      /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
      if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/
        {
          const UChar* myTargetStart = args->target;
#ifdef Debug
          printf("source %X\n sourceLimit %X\n mySourceLimit %X\n", args->source, args->sourceLimit, mySourceLimit); 
#endif /*Debug*/
          saveThis = args->converter;
          args->offsets = NULL;
          args->converter = ((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter;
          ucnv_toUnicode(args->converter,
              &args->target,
              args->targetLimit,
              &args->source,
              mySourceLimit,
              args->offsets,
              args->flush,
              err);
#ifdef Debug      
          puts("---------------------------> CONVERTED");
          printf("source %X\n sourceLimit %X\n mySourceLimit %X\n", args->source, args->sourceLimit, mySourceLimit); 
          printf("err =%d", *err);
#endif /*Debug*/
          args->converter = saveThis;
        }
      /*-Done with buffer with entire buffer
        -Error while converting
      */
        
      if (U_FAILURE(*err) || (args->source == args->sourceLimit)) return;
#ifdef Debug            
      puts("Got Here!");
      fflush(stdout);
#endif /*Debug*/
      sourceStart = args->source;
      changeState_2022(args->converter,
                       &(args->source), 
                       args->sourceLimit,
                       args->flush,
                       err);
     /* args->source = sourceStart; */
      (args->source)++;

    }
  
  return;
}

U_CFUNC void T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC(UConverterToUnicodeArgs* args,
                                                          UErrorCode* err)
{
  int32_t myOffset=0;
  int32_t base = 0;
  const char* mySourceLimit;
  char const* sourceStart;
  UConverter* _this = NULL;

  /*Arguments Check*/
  if (U_FAILURE(*err)) return;
  if ((args->converter == NULL) || (args->targetLimit < args->target) || (args->sourceLimit < args->source))
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return;
    }
  
  for (;;)
    {
      mySourceLimit =  getEndOfBuffer_2022(args->source, args->sourceLimit, args->flush); 
      /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/

      if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/
        {
          const UChar* myTargetStart = args->target;
#ifdef Debug
          printf("source %X\n mySourceLimit %X\n sourceLimit %X\n", args->source, mySourceLimit, args->sourceLimit); 
#endif /*Debug*/
          _this = args->converter;
          args->converter = ((UConverterDataISO2022*)(_this->extraInfo))->currentConverter;
          ucnv_toUnicode(args->converter, 
              &(args->target),
              args->targetLimit,
              &(args->source),
              mySourceLimit,
              args->offsets,
              args->flush,
              err);

          args->converter = _this;
            {
              int32_t lim =  args->target - myTargetStart;
              int32_t i = 0;
              for (i=base; i < lim;i++)    args->offsets[i] += myOffset;
              base += lim;
            }
          
#ifdef Debug      
          puts("---------------------------> CONVERTED");
          printf("source %X\n sourceLimit %X\n mySourceLimit %X\n", args->source, args->sourceLimit, mySourceLimit); 
          printf("err =%d", *err);
#endif /*Debug*/
        }

      /*-Done with buffer with entire buffer
        -Error while converting
      */
        
      if (U_FAILURE(*err) || (args->source == args->sourceLimit)) return;
#ifdef Debug            
      puts("Got Here!");
      fflush(stdout);
#endif /*Debug*/
      sourceStart = args->source;
      changeState_2022(args->converter,
                       &(args->source), 
                       args->sourceLimit,
                       args->flush,
                       err);
       (args->source)++;
       myOffset += args->source - sourceStart;

    }
  
  return;
}

U_CFUNC UChar32 T_UConverter_getNextUChar_ISO_2022(UConverterToUnicodeArgs* args,
                                     UErrorCode* err)
{
  const char* mySourceLimit;
  /*Arguments Check*/
  if  (args->sourceLimit < args->source)
    {
      *err = U_ILLEGAL_ARGUMENT_ERROR;
      return 0xffff;
    }
  
  for (;;)
    {
      mySourceLimit =  getEndOfBuffer_2022(args->source, args->sourceLimit, TRUE); 
      /*Find the end of the buffer e.g : Next Escape Seq | end of Buffer*/
      if (args->converter->mode == UCNV_SO) /*Already doing some conversion*/
        {
          
          return ucnv_getNextUChar(((UConverterDataISO2022*)(args->converter->extraInfo))->currentConverter,
                                   &(args->source),
                                   mySourceLimit,
                                   err);
          

        }
      /*-Done with buffer with entire buffer
        -Error while converting
      */
      

      changeState_2022(args->converter,
                       &(args->source), 
                       args->sourceLimit,
                       TRUE,
                       err);
      args->source++;
    }
  
  return 0xffff;
}

static const UConverterImpl _ISO2022Impl={
    UCNV_ISO_2022,

    NULL,
    NULL,

    _ISO2022Open,
    _ISO2022Close,
    _ISO2022Reset,

    T_UConverter_toUnicode_ISO_2022,
    T_UConverter_toUnicode_ISO_2022_OFFSETS_LOGIC,
    T_UConverter_fromUnicode_ISO_2022,
    T_UConverter_fromUnicode_ISO_2022_OFFSETS_LOGIC,
    T_UConverter_getNextUChar_ISO_2022,

    NULL
};

const UConverterStaticData _ISO2022StaticData={
  sizeof(UConverterStaticData),
  "ISO_2022",
    2022, UCNV_IBM, UCNV_ISO_2022, 1, 4,
    1, { 0x1a, 0, 0, 0 }, FALSE, FALSE,
  { 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0} /* reserved */
};


const UConverterSharedData _ISO2022Data={
    sizeof(UConverterSharedData), ~((uint32_t) 0),
    NULL, NULL, &_ISO2022StaticData, FALSE, &_ISO2022Impl, 
    0
};

/* EBCDICStateful ----------------------------------------------------------- */

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

                  args->source = saveSource;
                  args->target = saveTarget;
                  args->offsets = saveOffsets;
                  if (U_FAILURE (*err))  break;
                  args->converter->invalidCharLength = 0;
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
  int32_t* originalOffsets = args->offsets;

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
                  if (U_FAILURE (*err))   break;
                  args->converter->invalidCharLength = 0;
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
  int8_t targetUniCharByteNum = 0;
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
          isTargetUCharDBCS = (targetUniChar>0x00FF);
          
          if (targetUniChar != missingCharMarker)
            {
              if (oldIsTargetUCharDBCS != isTargetUCharDBCS)
                {
                  if (isTargetUCharDBCS) args->target[myTargetIndex++] = UCNV_SO;
                  else args->target[myTargetIndex++] = UCNV_SI;
                  
                  
                  if ((!isTargetUCharDBCS)&&(myTargetIndex+1 >= targetLength))
                    {
                      args->converter->charErrorBuffer[0] = (char) targetUniChar;
                      args->converter->charErrorBufferLength = 1;
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      break;
                    }
                  else if (myTargetIndex+1 >= targetLength)
                    {
                      args->converter->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      args->converter->charErrorBuffer[1] = (char) targetUniChar & 0x00FF;
                      args->converter->charErrorBufferLength = 2;
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
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
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
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
              if (U_FAILURE (*err)) break;
              args->converter->invalidUCharLength = 0;
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
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
  int8_t targetUniCharByteNum = 0;
  UChar mySourceChar = 0x0000;
  UBool isTargetUCharDBCS = (UBool)args->converter->fromUnicodeStatus;
  UBool oldIsTargetUCharDBCS = isTargetUCharDBCS;
  int32_t* originalOffsets = args->offsets;
  
  myFromUnicode = &args->converter->sharedData->table->dbcs.fromUnicode;
  /*writing the char to the output stream */
  while (mySourceIndex < sourceLength)
    {
      if (myTargetIndex < targetLength)
        {
          mySourceChar = (UChar) args->source[mySourceIndex++];
          targetUniChar = (UChar) ucmp16_getu (myFromUnicode, mySourceChar);
          oldIsTargetUCharDBCS = isTargetUCharDBCS;
          isTargetUCharDBCS = (targetUniChar>0x00FF);
          
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
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      break;
                    }
                  else if (myTargetIndex+1 >= targetLength)
                    {
                      args->converter->charErrorBuffer[0] = (char) (targetUniChar >> 8);
                      args->converter->charErrorBuffer[1] = (char) targetUniChar & 0x00FF;
                      args->converter->charErrorBufferLength = 2;
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
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
                      *err = U_INDEX_OUTOFBOUNDS_ERROR;
                      break;
                    }
                }
            }
          else
            {
              int32_t currentOffset = args->offsets[myTargetIndex-1]+1;
              int32_t My_i = myTargetIndex;
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
              args->source = saveSource;
              args->target = saveTarget;
              args->offsets = saveOffsets;
              if (U_FAILURE (*err))     break;
              args->converter->invalidUCharLength = 0;
            }
        }
      else
        {
          *err = U_INDEX_OUTOFBOUNDS_ERROR;
          break;
        }

    }


  args->target += myTargetIndex;
  args->source += mySourceIndex;;
  
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
      /*Not lead byte: we update the source ptr and get the codepoint*/
      myUChar = ucmp16_getu( (&(args->converter->sharedData->table->dbcs.toUnicode)),
                            (UChar)(*(args->source)));
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
                            ((UChar)((*(args->source))) << 8) |((uint8_t)*(args->source+1)));

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
      if (*err == U_INDEX_OUTOFBOUNDS_ERROR) *err = U_ZERO_ERROR;
      
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
