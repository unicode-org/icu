/*
 ********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-1999, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
 *
 *  uconv_bld.c:
 *
 *  Defines functions that are used in the creation/initialization/deletion
 *  of converters and related structures.
 *  uses uconv_io.h routines to access disk information
 *  is used by ucnv.h to implement public API create/delete/flushCache routines
 */


#include "ucnv_io.h"
#include "uhash.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "unicode/ucnv_bld.h"
#include "unicode/ucnv_err.h"
#include "ucnv_imp.h"
#include "unicode/udata.h"
#include "unicode/ucnv.h"
#include "umutex.h"
#include "cstring.h"
#include "cmemory.h"
#include "filestrm.h"

#include <stdio.h>

/*Array used to generate ALGORITHMIC_CONVERTERS_HASHTABLE
 *should ALWAYS BE EMPTY STRING TERMINATED.
 */
static const char *algorithmicConverterNames[] = {
  "LATIN_1",
  "UTF8",
  "UTF16_BigEndian",
  "UTF16_LittleEndian",
  "UTF16_PlatformEndian",
  "UTF16_OppositeEndian",
  "ISO_2022",
  "JIS",
  "EUC",
  "GB",
  ""
};

/*Takes an alias name gets an actual converter file name
 *goes to disk and opens it.
 *allocates the memory and returns a new UConverter object
 */
static UConverter *createConverterFromFile (const char *converterName, UErrorCode * err);
static UConverter *createConverterFromAlgorithmicType (const char *realName, UErrorCode * err);

/*Given a file returns a newly allocated CompactByteArray based on the a serialized one */
static CompactByteArray *createCompactByteArrayFromFile (FileStream * infile, UErrorCode * err);

/*Given a file returns a newly allocated CompactShortArray based on the a serialized one */
static CompactShortArray *createCompactShortArrayFromFile (FileStream * infile, UErrorCode * err);

/*Currently we have function to take us from a codepage name to
 *a platform type and a codepage number
 *assuming the following
 *codepage name = $PLATFORM-#CODEPAGE
 *e.g. ibm-949 = platform type = UCNV_IBM and codepage # = 949
 *the functions below implement that
 */
static UConverterPlatform getPlatformFromName (char *name);
static int32_t getCodepageNumberFromName (char *name);

static UConverterType getAlgorithmicTypeFromName (const char *realName);


/*these functions initialize the lightweight mutable part of the
 *object to correct values based on the sharedData defaults.
 */
static void initializeDataConverter (UConverter * myConverter);
static void initializeAlgorithmicConverter (UConverter * myConverter);
/**
 *hash function for UConverterSharedData
 */

static int32_t uhash_hashSharedData (void *sharedData);

/**
 * Un flatten shared data from a UDATA..
 */
U_CAPI  UConverterSharedData* U_EXPORT2 ucnv_data_unFlattenClone(const UConverterSharedData *data, UErrorCode *status);


/*initializes some global variables */
UHashtable *SHARED_DATA_HASHTABLE = NULL;
UHashtable *ALGORITHMIC_CONVERTERS_HASHTABLE = NULL;

/*Returns uppercased string */
char *
  strtoupper (char *name)
{
  int32_t i = 0;

  while (name[i] = uprv_toupper (name[i]))
    i++;

  return name;
}

/* Returns true in c is a in set 'setOfChars', false otherwise
 */
bool_t 
  isInSet (char c, const char *setOfChars)
{
  uint8_t i = 0;

  while (setOfChars[i] != '\0')
    {
      if (c == setOfChars[i++])
        return TRUE;
    }

  return FALSE;
}

/* Returns pointer to the next non-whitespace (or non-separator)
 */
int32_t 
  nextTokenOffset (const char *line, const char *separators)
{
  int32_t i = 0;

  while (line[i] && isInSet (line[i], separators))
    i++;

  return i;
}

/* Returns pointer to the next token based on the set of separators
 */
char *
  getToken (char *token, char *line, const char *separators)
{
  int32_t i = nextTokenOffset (line, separators);
  int8_t j = 0;

  while (line[i] && (!isInSet (line[i], separators)))
    token[j++] = line[i++];
  token[j] = '\0';

  return line + i;
}

int32_t uhash_hashIString(const void* name)
{
  char myName[UCNV_MAX_CONVERTER_NAME_LENGTH];
  uprv_strcpy(myName, (char*)name);
  strtoupper(myName);

  return uhash_hashString(myName);
}

CompactShortArray*  createCompactShortArrayFromFile (FileStream * infile, UErrorCode * err)
{
  int32_t i = 0;
  int16_t *myShortArray = NULL;
  uint16_t *myIndexArray = NULL;
  int32_t myValuesCount = 0;
  int32_t myIndexCount = 0;
  int32_t myBlockShift = 0;

  if (U_FAILURE (*err))
    return NULL;

  /*reads in the lengths of the 2 serialized array */
  T_FileStream_read (infile, &myValuesCount, sizeof (int32_t));
  T_FileStream_read (infile, &myIndexCount, sizeof (int32_t));
  T_FileStream_read (infile, &myBlockShift, sizeof (int32_t));

  if (myValuesCount < 0)
    {
      *err = U_INVALID_TABLE_FILE;
      return NULL;
    }
  myShortArray = (int16_t *) uprv_malloc (myValuesCount * sizeof (int16_t));
  if (myShortArray == NULL)
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  /*reads in the first array */
  T_FileStream_read (infile, myShortArray, myValuesCount * sizeof (int16_t));

  if (myIndexCount < 0)
    {
      uprv_free (myShortArray);
      *err = U_INVALID_TABLE_FILE;
      return NULL;
    }

  myIndexArray = (uint16_t *) uprv_malloc (myIndexCount * sizeof (uint16_t));
  if (myIndexArray == NULL)
    {
      uprv_free (myShortArray);
      *err = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }

  /*reads in the second array */
  T_FileStream_read (infile, myIndexArray, myIndexCount * sizeof (uint16_t));

  /*create a compact array from the data just read
   *that adopts our newly created arrays
   */
  return ucmp16_openAdoptWithBlockShift (myIndexArray, myShortArray, myValuesCount, 0, myBlockShift);
}

CompactByteArray*  createCompactByteArrayFromFile (FileStream * infile,
                                                   UErrorCode * err)
{
  int32_t i = 0;
  int8_t *myByteArray = NULL;
  uint16_t *myIndexArray = NULL;
  int32_t myValuesCount = 0;
  int32_t myIndexCount = 0;

  if (U_FAILURE (*err))
    return NULL;

  /*reads in the lengths of the 2 serialized array */
  T_FileStream_read (infile, &myValuesCount, sizeof (int32_t));
  T_FileStream_read (infile, &myIndexCount, sizeof (int32_t));

  if (myValuesCount < 0)
    {
      *err = U_INVALID_TABLE_FILE;
      return NULL;
    }
  myByteArray = (int8_t *) uprv_malloc (myValuesCount * sizeof (int8_t));
  if (myByteArray == NULL)
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  /*reads in the first array */
  T_FileStream_read (infile, myByteArray, myValuesCount * sizeof (int8_t));

  if (myIndexCount < 0)
    {
      uprv_free (myByteArray);
      *err = U_INVALID_TABLE_FILE;
      return NULL;
    }
  myIndexArray = (uint16_t *) uprv_malloc (myIndexCount * sizeof (uint16_t));
  if (myIndexArray == NULL)
    {
      uprv_free (myByteArray);
      *err = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  /*reads in the second array */
  T_FileStream_read (infile, myIndexArray, myIndexCount * sizeof (uint16_t));

  /*create a compact array from the data just read
   *that adopts our newly created arrays
   */
  return ucmp8_openAdopt (myIndexArray, myByteArray, myValuesCount);
}


static bool_t
isCnvAcceptable(void *context,
             const char *type, const char *name,
             UDataInfo *pInfo) {
    return 
        pInfo->size>=20 &&
        pInfo->isBigEndian==U_IS_BIG_ENDIAN &&
        pInfo->charsetFamily==U_CHARSET_FAMILY &&
        pInfo->sizeofUChar==U_SIZEOF_UCHAR &&
        pInfo->dataFormat[0]==0x63 &&   /* dataFormat="cnvt" */
        pInfo->dataFormat[1]==0x6e &&
        pInfo->dataFormat[2]==0x76 &&
        pInfo->dataFormat[3]==0x74 &&
        pInfo->formatVersion[0]==2;
}

#define DATA_TYPE "cnv"

UConverter*  createConverterFromFile (const char *fileName, UErrorCode * err)
{
  int32_t i = 0;
  const int8_t *myByteArray = NULL;
  const uint16_t *myIndexArray = NULL;
  int32_t myValuesCount = 0;
  int32_t myIndexCount = 0;
  UConverter *myConverter = NULL;
  int8_t errorLevel = 0;

  UDataMemory *data;

  if (err == NULL || U_FAILURE (*err)) {
    return NULL;
  }
  

  data = udata_openChoice(NULL, DATA_TYPE, fileName, isCnvAcceptable, NULL, err);
  if(U_FAILURE(*err))
    {
      return NULL;
    }

  myConverter = (UConverter *) uprv_malloc (sizeof (UConverter));
  if (myConverter == NULL)
    {
      udata_close(data);
      *err = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }

  myConverter->sharedData =
    (UConverterSharedData *) udata_getMemory(data);

  if (myConverter->sharedData == NULL)
    {
      udata_close(data);
      uprv_free (myConverter);
      *err = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }

  /* clone it. OK to drop the original sharedData */
  myConverter->sharedData = ucnv_data_unFlattenClone(myConverter->sharedData, err);

  myConverter->sharedData->dataMemory = (void*)data; /* for future use */


  if(U_FAILURE(*err))
    {
      udata_close(data);
      uprv_free (myConverter);
      *err = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }

  if (U_SUCCESS (*err))
    {
      initializeDataConverter (myConverter);
    }

  return myConverter;
}


void 
  copyPlatformString (char *platformString, UConverterPlatform pltfrm)
{
  switch (pltfrm)
    {
    case UCNV_IBM:
      {
        uprv_strcpy (platformString, "ibm");
        break;
      }
    default:
      {
        uprv_strcpy (platformString, "");
        break;
      }
    };

  return;
}

/*returns a converter type from a string
 */
UConverterType 
  getAlgorithmicTypeFromName (const char *realName)
{
  if (uprv_strcmp (realName, "UTF8") == 0)
    return UCNV_UTF8;
  else if (uprv_strcmp (realName, "UTF16_BigEndian") == 0)
    return UCNV_UTF16_BigEndian;
  else if (uprv_strcmp (realName, "UTF16_LittleEndian") == 0)
    return UCNV_UTF16_LittleEndian;
  else if (uprv_strcmp (realName, "LATIN_1") == 0)
    return UCNV_LATIN_1;
  else if (uprv_strcmp (realName, "JIS") == 0)
    return UCNV_JIS;
  else if (uprv_strcmp (realName, "EUC") == 0)
    return UCNV_EUC;
  else if (uprv_strcmp (realName, "GB") == 0)
    return UCNV_GB;
  else if (uprv_strcmp (realName, "ISO_2022") == 0)
    return UCNV_ISO_2022;
  else if (uprv_strcmp (realName, "UTF16_PlatformEndian") == 0)
#  if U_IS_BIG_ENDIAN
      return UCNV_UTF16_BigEndian;
#  else
      return UCNV_UTF16_LittleEndian;
#  endif
  else if (uprv_strcmp (realName, "UTF16_OppositeEndian") == 0)
#  if U_IS_BIG_ENDIAN
      return UCNV_UTF16_LittleEndian;
#  else
      return UCNV_UTF16_BigEndian;
#  endif
  else
    return UCNV_UNSUPPORTED_CONVERTER;
}


UConverterPlatform 
  getPlatformFromName (char *name)
{
  char myPlatform[10];
  char mySeparators[2] = {'-', '\0'};

  getToken (myPlatform, name, mySeparators);
  strtoupper (myPlatform);

  if (uprv_strcmp (myPlatform, "IBM") == 0)
    return UCNV_IBM;
  else
    return UCNV_UNKNOWN;
}

int32_t 
  getCodepageNumberFromName (char *name)
{
  char myNumber[10];
  char mySeparators[2] = {'-', '\0'};
  char *line = NULL;

  line = getToken (myNumber, name, mySeparators);
  getToken (myNumber, line, mySeparators);

  return T_CString_stringToInteger (myNumber, 10);
}

int32_t uhash_hashSharedData (void *sharedData)
{
  return uhash_hashIString(((UConverterSharedData *) sharedData)->name);
}

/*Puts the shared data in the static hashtable SHARED_DATA_HASHTABLE */
void   shareConverterData (UConverterSharedData * data)
{
  UErrorCode err = U_ZERO_ERROR;
  /*Lazy evaluates the Hashtable itself */

  if (SHARED_DATA_HASHTABLE == NULL)
    {
      UHashtable* myHT = uhash_openSize ((UHashFunction) uhash_hashSharedData, 
                                         ucnv_io_countAvailableAliases(&err),
                                         &err);
      if (U_FAILURE (err)) return;
      umtx_lock (NULL);
      if (SHARED_DATA_HASHTABLE == NULL) SHARED_DATA_HASHTABLE = myHT;
      else uhash_close(myHT);
      umtx_unlock (NULL);
      
    }
  umtx_lock (NULL);
  uhash_put(SHARED_DATA_HASHTABLE,
            data,
            &err);
  umtx_unlock (NULL);

  return;
}

UConverterSharedData *getSharedConverterData (const char *name)
{
  /*special case when no Table has yet been created we return NULL */
  if (SHARED_DATA_HASHTABLE == NULL)    return NULL;
  else
    /*    return (UConverterSharedData *) uhash_get (SHARED_DATA_HASHTABLE, uhash_hashString (name));*/
    {
      UConverterSharedData *i = (UConverterSharedData*)uhash_get (SHARED_DATA_HASHTABLE, uhash_hashIString (name));
      return i;
    }
}

/*frees the string of memory blocks associates with a sharedConverter
 *if and only if the referenceCounter == 0
 */
bool_t   deleteSharedConverterData (UConverterSharedData * deadSharedData)
{
    if (deadSharedData->referenceCounter > 0)
        return FALSE;
    
    /* Note: if we have a dataMemory, then that means that all ucmp's came
       from udata, and their tables will go away at the end
       of this function. So, we need to simply dealloc the UCMP8's themselves.
       We're guaranteed that they do not allocate any further memory.
       
       When we have an API to simply 'init' a ucmp8, then no action at all will
       need to happen.   --srl 
    */
    
    switch (deadSharedData->conversionType)
    {
    case UCNV_SBCS:
    {
        ucmp8_close (deadSharedData->table->sbcs.fromUnicode);
        uprv_free (deadSharedData->table);
    };
    break;
    
    case UCNV_MBCS:
    {
        ucmp16_close (deadSharedData->table->mbcs.fromUnicode);
        ucmp16_close (deadSharedData->table->mbcs.toUnicode);
	uprv_free (deadSharedData->table);
    };
    break;

    case UCNV_DBCS:
    case UCNV_EBCDIC_STATEFUL:
    {
        ucmp16_close (deadSharedData->table->dbcs.fromUnicode);
        ucmp16_close (deadSharedData->table->dbcs.toUnicode);
	uprv_free (deadSharedData->table);
    };
    break;

    default: ; /* semicolon makes MSVC happy */
    };

    if(deadSharedData->dataMemory != NULL)
    {
        UDataMemory *data = (UDataMemory*)deadSharedData->dataMemory;
        udata_close(data);
    }

    uprv_free (deadSharedData);
    
    return TRUE;
}

bool_t   isDataBasedConverter (const char *name)
{
  int32_t i = 0;
  bool_t result = FALSE;
  UErrorCode err = U_ZERO_ERROR;

  /*Lazy evaluates the hashtable */
  if (ALGORITHMIC_CONVERTERS_HASHTABLE == NULL)
    {
      UHashtable* myHT;
      
      {
          myHT = uhash_open (uhash_hashIString, &err);
          
          if (U_FAILURE (err)) return FALSE;
          while (algorithmicConverterNames[i][0] != '\0')
            {
              /*Stores in the hashtable a pointer to the statically init'ed array containing
               *the names
               */
              
              uhash_put (myHT,
                         (void *) algorithmicConverterNames[i],
                         &err);
              i++;                      /*Some Compilers (Solaris WSpro and MSVC-Release Mode
                                         *don't differentiate between i++ and ++i
                                         *so we have to increment in a line by itself
                                         */
            }
      }
      
      umtx_lock (NULL);
      if (ALGORITHMIC_CONVERTERS_HASHTABLE == NULL) ALGORITHMIC_CONVERTERS_HASHTABLE = myHT;
      else uhash_close(myHT);      
      umtx_unlock (NULL);
      
      
    }
    
  
  if (uhash_get (ALGORITHMIC_CONVERTERS_HASHTABLE,
                 uhash_hashIString (name)) == NULL)
    {
      result = TRUE;
    }
  

  return result;
}
/*Logic determines if the converter is Algorithmic AND/OR cached
 *depending on that:
 * -we either go to get data from disk and cache it (Data=TRUE, Cached=False)
 * -Get it from a Hashtable (Data=X, Cached=TRUE)
 * -Call dataConverter initializer (Data=TRUE, Cached=TRUE)
 * -Call AlgorithmicConverter initializer (Data=FALSE, Cached=TRUE)
 */
UConverter *
  createConverter (const char *converterName, UErrorCode * err)
{
  const char *realName;
  UConverter *myUConverter = NULL;
  UConverterSharedData *mySharedConverterData = NULL;
  UErrorCode internalErrorCode = U_ZERO_ERROR;
  bool_t isDefaultConverter;

  if (U_FAILURE (*err))
    return NULL;

  /* In case "name" is NULL we want to open the default converter. */
  if (converterName == NULL) {
    converterName = ucnv_io_getDefaultConverterName();
    if (converterName == NULL) {
      *err = U_MISSING_RESOURCE_ERROR;
      return NULL;
    } else {
      isDefaultConverter = TRUE;
    }
  } else {
    isDefaultConverter = FALSE;
  }

  if (*converterName == 0) {
    /* Use the PlatformInvariant algorithmic converter. */
    realName = "PlatformInvariant";
  } else if(isDefaultConverter) {
    /* the default converter name is already canonical */
    realName = converterName;
  } else {
    /* get the canonical converter name */
    realName = ucnv_io_getConverterName(converterName, &internalErrorCode);
    if (U_FAILURE(internalErrorCode) || realName == NULL) {
      /*
       * set the input name in case the converter was added
       * without updating the alias table, or when there is no alias table
       */
      realName = converterName;
    }
  }

  if (isDataBasedConverter (realName))
    {
      mySharedConverterData = getSharedConverterData (realName);

      if (mySharedConverterData == NULL)
        {
          /*Not cached, we need to stream it in from file */
          myUConverter = createConverterFromFile (realName, err);

          if (U_FAILURE (*err) || (myUConverter == NULL))
            {
              return myUConverter;
            }
          else
            {
              /*shared it with other library clients */


              shareConverterData (myUConverter->sharedData);
              return myUConverter;
            }
        }
      else
        {
          /*Is already cached, point to an existing one */
          myUConverter = (UConverter *) uprv_malloc (sizeof (UConverter));
          if (myUConverter == NULL)
            {
              *err = U_MEMORY_ALLOCATION_ERROR;
              return NULL;
            }

          /*update the reference counter: one more client */
          umtx_lock (NULL);
          mySharedConverterData->referenceCounter++;
          umtx_unlock (NULL);

          myUConverter->sharedData = mySharedConverterData;
          initializeDataConverter (myUConverter);

          return myUConverter;
        }
    }
  else
    {
      /*with have an algorithmic converter */
      mySharedConverterData = getSharedConverterData (realName);

      /*Non cached */
      if (mySharedConverterData == NULL)
        {
          myUConverter = createConverterFromAlgorithmicType (realName, err);
          if (U_FAILURE (*err) || (myUConverter == NULL))
            {
              uprv_free (myUConverter);
              return NULL;
            }
          else
            {
              /* put the shared object in shared table */
              shareConverterData (myUConverter->sharedData);
              return myUConverter;
            }
        }
      else
        {
          myUConverter = (UConverter *) uprv_malloc (sizeof (UConverter));
          if (myUConverter == NULL)
            {
              *err = U_MEMORY_ALLOCATION_ERROR;
              return NULL;
            }

          /*Increase the reference counter */
          umtx_lock (NULL);
          mySharedConverterData->referenceCounter++;
          umtx_unlock (NULL);

          /*initializes the converter */
          myUConverter->sharedData = mySharedConverterData;
          initializeAlgorithmicConverter (myUConverter);
          return myUConverter;
        }

      return myUConverter;
    }

  return NULL;
}

/*Initializes the mutable lightweight portion of the object
 *By copying data from UConverter->sharedData->defaultConverter
 */
void   initializeDataConverter (UConverter * myUConverter)
{

  myUConverter->mode = UCNV_SI;
  myUConverter->UCharErrorBufferLength = 0;
  myUConverter->charErrorBufferLength = 0;
  myUConverter->subCharLen = myUConverter->sharedData->defaultConverterValues.subCharLen;
  uprv_memcpy (myUConverter->subChar,
              myUConverter->sharedData->defaultConverterValues.subChar,
              myUConverter->subCharLen);
  myUConverter->toUnicodeStatus = 0x00;
  myUConverter->fromUnicodeStatus = 0x00;
  myUConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0x00;

  myUConverter->fromCharErrorBehaviour = (UConverterToUCallback) UCNV_TO_U_CALLBACK_SUBSTITUTE;
  myUConverter->fromUCharErrorBehaviour = (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_SUBSTITUTE;
  myUConverter->extraInfo = NULL;

  return;
}

/* This function initializes algorithmic converters
 * based on there type
 */
void 
  initializeAlgorithmicConverter (UConverter * myConverter)
{
  char UTF8_subChar[] = {(char) 0xFF, (char) 0xFF, (char) 0xFF};
  char UTF16BE_subChar[] = {(char) 0xFF, (char) 0xFD};
  char UTF16LE_subChar[] = {(char) 0xFD, (char) 0xFF};
  char EUC_subChar[] = {(char) 0xAF, (char) 0xFE};
  char GB_subChar[] = {(char) 0xFF, (char) 0xFF};
  char JIS_subChar[] = {(char) 0xFF, (char) 0xFF};
  char LATIN1_subChar = 0x1A;



  myConverter->mode = UCNV_SI;
  myConverter->fromCharErrorBehaviour = (UConverterToUCallback) UCNV_TO_U_CALLBACK_SUBSTITUTE;
  myConverter->fromUCharErrorBehaviour = (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_SUBSTITUTE;
  myConverter->charErrorBufferLength = 0;
  myConverter->UCharErrorBufferLength = 0;

  myConverter->extraInfo = NULL;


  switch (myConverter->sharedData->conversionType)
    {
    case UCNV_UTF8:
      {
        myConverter->sharedData->minBytesPerChar = 1;
        myConverter->sharedData->maxBytesPerChar = 4;
        myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
        myConverter->sharedData->defaultConverterValues.subCharLen = 3;
        myConverter->subCharLen = 3;
        myConverter->toUnicodeStatus = 0;
    myConverter->fromUnicodeStatus = 0; /* srl */ 
        myConverter->sharedData->platform = UCNV_IBM;
        myConverter->sharedData->codepage = 1208;
        uprv_strcpy(myConverter->sharedData->name, "UTF8");
        uprv_memcpy (myConverter->subChar, UTF8_subChar, 3);
        uprv_memcpy (myConverter->sharedData->defaultConverterValues.subChar, UTF8_subChar, 3);

        break;
      }
    case UCNV_LATIN_1:
      {
        myConverter->sharedData->minBytesPerChar = 1;
        myConverter->sharedData->maxBytesPerChar = 1;
        myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
        myConverter->sharedData->defaultConverterValues.subCharLen = 1;
        myConverter->subCharLen = 1;
        myConverter->toUnicodeStatus = 0;
        myConverter->sharedData->platform = UCNV_IBM;
        myConverter->sharedData->codepage = 819;
        uprv_strcpy(myConverter->sharedData->name, "LATIN_1");
        *(myConverter->subChar) = LATIN1_subChar;
        *(myConverter->sharedData->defaultConverterValues.subChar) = LATIN1_subChar;
        break;
      }

    case UCNV_UTF16_BigEndian:
      {
        myConverter->sharedData->minBytesPerChar = 2;
        myConverter->sharedData->maxBytesPerChar = 2;
        myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
        myConverter->sharedData->defaultConverterValues.subCharLen = 2;
        myConverter->subCharLen = 2;
        myConverter->toUnicodeStatus = 0;
        myConverter->fromUnicodeStatus = 0; 
        uprv_strcpy(myConverter->sharedData->name, "UTF_16BE");
        myConverter->sharedData->platform = UCNV_IBM;
        myConverter->sharedData->codepage = 1200;
        uprv_memcpy (myConverter->subChar, UTF16BE_subChar, 2);
        uprv_memcpy (myConverter->sharedData->defaultConverterValues.subChar, UTF16BE_subChar, 2);

        break;
      }

    case UCNV_UTF16_LittleEndian:
      {
        myConverter->sharedData->minBytesPerChar = 2;
        myConverter->sharedData->maxBytesPerChar = 2;
        myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
        myConverter->sharedData->defaultConverterValues.subCharLen = 2;
        myConverter->subCharLen = 2;
        myConverter->toUnicodeStatus = 0;
        myConverter->fromUnicodeStatus = 0; 
        myConverter->sharedData->platform = UCNV_IBM;
        myConverter->sharedData->codepage = 1200;
        uprv_strcpy(myConverter->sharedData->name, "UTF_16LE");
        uprv_memcpy (myConverter->subChar, UTF16LE_subChar, 2);
        uprv_memcpy (myConverter->sharedData->defaultConverterValues.subChar, UTF16LE_subChar, 2);
        break;
      }
    case UCNV_EUC:
      {
        myConverter->sharedData->minBytesPerChar = 1;
        myConverter->sharedData->maxBytesPerChar = 2;
        myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
        myConverter->sharedData->defaultConverterValues.subCharLen = 2;
        myConverter->subCharLen = 2;
        myConverter->toUnicodeStatus = 0;
        uprv_memcpy (myConverter->subChar, EUC_subChar, 2);
        uprv_memcpy (myConverter->sharedData->defaultConverterValues.subChar, EUC_subChar, 2);
        break;
      }
    case UCNV_ISO_2022:
      {
        myConverter->charErrorBuffer[0] = 0x1b;
        myConverter->charErrorBuffer[1] = 0x25;
        myConverter->charErrorBuffer[2] = 0x42;
        myConverter->charErrorBufferLength = 3;
        myConverter->sharedData->minBytesPerChar = 1;
        myConverter->sharedData->maxBytesPerChar = 3;
        myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
        myConverter->sharedData->defaultConverterValues.subCharLen = 1;
        myConverter->subCharLen = 1;
        myConverter->toUnicodeStatus = 0;
    myConverter->fromUnicodeStatus = 0; /* srl */ 
        myConverter->sharedData->codepage = 2022;
        uprv_strcpy(myConverter->sharedData->name, "ISO_2022");
        *(myConverter->subChar) = LATIN1_subChar;
        *(myConverter->sharedData->defaultConverterValues.subChar) = LATIN1_subChar;
        myConverter->extraInfo = uprv_malloc (sizeof (UConverterDataISO2022));
        ((UConverterDataISO2022 *) myConverter->extraInfo)->currentConverter = NULL;
        ((UConverterDataISO2022 *) myConverter->extraInfo)->escSeq2022Length = 0;
        break;
      }
    case UCNV_GB:
      {
        myConverter->sharedData->minBytesPerChar = 2;
        myConverter->sharedData->maxBytesPerChar = 2;
        myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
        myConverter->sharedData->defaultConverterValues.subCharLen = 2;
        myConverter->subCharLen = 2;
        myConverter->toUnicodeStatus = 0;
        uprv_memcpy (myConverter->subChar, GB_subChar, 2);
        uprv_memcpy (myConverter->sharedData->defaultConverterValues.subChar, GB_subChar, 2);
        break;
      }
    case UCNV_JIS:
      {
        myConverter->sharedData->minBytesPerChar = 2;
        myConverter->sharedData->maxBytesPerChar = 2;
        myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
        myConverter->sharedData->defaultConverterValues.subCharLen = 2;
        myConverter->subCharLen = 2;
        myConverter->toUnicodeStatus = 0;
        uprv_memcpy (myConverter->subChar, JIS_subChar, 2);
        uprv_memcpy (myConverter->sharedData->defaultConverterValues.subChar, JIS_subChar, 2);
        break;
      }
    default:
      break;
    };

  myConverter->toUnicodeStatus = myConverter->sharedData->defaultConverterValues.toUnicodeStatus;
}


/*This function creates an algorithmic converter
 *Note That even algorithmic converters are shared
 * (The UConverterSharedData->table == NULL since
 * there are no tables)
 *for uniformity of design and control flow
 */
UConverter *
  createConverterFromAlgorithmicType (const char *actualName, UErrorCode * err)
{
  int32_t i = 0;
  UConverter *myConverter = NULL;
  UConverterSharedData *mySharedData = NULL;
  UConverterType myType = getAlgorithmicTypeFromName (actualName);

  if (U_FAILURE (*err))
    return NULL;

  myConverter = (UConverter *) uprv_malloc (sizeof (UConverter));
  if (myConverter == NULL)
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return NULL;
    }

  myConverter->sharedData = NULL;
  mySharedData = (UConverterSharedData *) uprv_malloc (sizeof (UConverterSharedData));
  if (mySharedData == NULL)
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      uprv_free (myConverter);
      return NULL;
    }
  mySharedData->structSize = sizeof(UConverterSharedData);
  mySharedData->table = NULL;
  mySharedData->dataMemory = NULL;
  uprv_strcpy (mySharedData->name, actualName);
  /*Initializes the referenceCounter to 1 */
  mySharedData->referenceCounter = 1;
  mySharedData->platform = UCNV_UNKNOWN;
  mySharedData->codepage = 0;
  mySharedData->conversionType = myType;
  myConverter->sharedData = mySharedData;

  initializeAlgorithmicConverter (myConverter);
  return myConverter;
}


UConverterSharedData* ucnv_data_unFlattenClone(const UConverterSharedData *source, UErrorCode *status)
{
    const uint8_t *raw, *oldraw;
    UConverterSharedData *data = NULL;
    
    if(U_FAILURE(*status))
        return NULL;

    if(source->structSize != sizeof(UConverterSharedData))
    {
        *status = U_INVALID_TABLE_FORMAT;
        return NULL;
    }

  data = (UConverterSharedData*) malloc(sizeof(UConverterSharedData));
  raw = (uint8_t*)source;
  uprv_memcpy(data,source,sizeof(UConverterSharedData));
  
  raw += data->structSize;

  /*  data->table = (UConverterTable*)raw; */
  
  switch (data->conversionType)
    {
    case UCNV_SBCS:
      data->table = malloc(sizeof(UConverterSBCSTable));
      data->table->sbcs.toUnicode = (UChar*)raw;
      raw += sizeof(UChar)*256;

      data->table->sbcs.fromUnicode = ucmp8_cloneFromData(&raw, status);

      break;

    case UCNV_EBCDIC_STATEFUL:
    case UCNV_DBCS:
      data->table = uprv_malloc(sizeof(UConverterDBCSTable));

      oldraw = raw;

      data->table->dbcs.toUnicode=ucmp16_cloneFromData(&raw, status);

      while((raw-oldraw)%4) /* pad to 4 */
          raw++;

      data->table->dbcs.fromUnicode =ucmp16_cloneFromData(&raw, status);

      break;

    case UCNV_MBCS:
      data->table = uprv_malloc(sizeof(UConverterMBCSTable));

      data->table->mbcs.starters = (bool_t*)raw;
      raw += sizeof(bool_t)*256;
      
      oldraw = raw;

      data->table->mbcs.toUnicode   = ucmp16_cloneFromData(&raw, status);

      while((raw-oldraw)%4) /* pad to 4 */
          raw++;

      data->table->mbcs.fromUnicode = ucmp16_cloneFromData(&raw, status);

      break;

    default:
      *status = U_INVALID_TABLE_FORMAT;
      return NULL;
    }
  
  return data;
}







