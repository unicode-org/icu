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
#include "ucnv_cnv.h"
#include "ucnv_imp.h"
#include "unicode/udata.h"
#include "unicode/ucnv.h"
#include "umutex.h"
#include "cstring.h"
#include "cmemory.h"
#include "filestrm.h"

#include <stdio.h>

static const UConverterSharedData *
converterData[UCNV_NUMBER_OF_SUPPORTED_CONVERTER_TYPES]={
    &_SBCSData, &_DBCSData, &_MBCSData, &_Latin1Data,
    &_UTF8Data, &_UTF16BEData, &_UTF16LEData, &_EBCDICStatefulData,
    &_ISO2022Data
};

static struct {
  const char *name;
  UConverterType type;
} cnvNameType[] = {
  { "LATIN_1", UCNV_LATIN_1 },
  { "UTF8", UCNV_UTF8 },
  { "UTF16_BigEndian", UCNV_UTF16_BigEndian },
  { "UTF16_LittleEndian", UCNV_UTF16_LittleEndian },
#if U_IS_BIG_ENDIAN
  { "UTF16_PlatformEndian", UCNV_UTF16_BigEndian },
  { "UTF16_OppositeEndian", UCNV_UTF16_LittleEndian },
#else
  { "UTF16_PlatformEndian", UCNV_UTF16_LittleEndian },
  { "UTF16_OppositeEndian", UCNV_UTF16_BigEndian},
#endif
  { "ISO_2022", UCNV_ISO_2022 }
};

/*Takes an alias name gets an actual converter file name
 *goes to disk and opens it.
 *allocates the memory and returns a new UConverter object
 */
static UConverter *createConverterFromFile (const char *converterName, UErrorCode * err);

static const UConverterSharedData *getAlgorithmicTypeFromName (const char *realName);


/*these functions initialize the lightweight mutable part of the
 *object to correct values based on the sharedData defaults.
 */
static void initializeDataConverter (UConverter * myConverter);
static void initializeAlgorithmicConverter (UConverter * myConverter);

/**
 *hash function for UConverterSharedData
 */
static int32_t uhash_hashSharedData (void *sharedData);

/*Defines the struct of a UConverterSharedData the immutable, shared part of
 *UConverter -
 * This is the definition from ICU 1.4, necessary to read converter data
 * version 1 because the structure is directly embedded in the data.
 * See udata.html for why this is bad (pointers, enums, padding...).
 */
typedef struct
  {
    uint32_t structSize;        /* Size of this structure */
    void *dataMemory;
    uint32_t referenceCounter;	/*used to count number of clients */
    char name[UCNV_MAX_CONVERTER_NAME_LENGTH];	/*internal name of the converter */
    UConverterPlatform platform;	/*platform of the converter (only IBM now) */
    int32_t codepage;		/*codepage # (now IBM-$codepage) */
    UConverterType conversionType;	/*conversion type */
    int8_t minBytesPerChar;	/*Minimum # bytes per char in this codepage */
    int8_t maxBytesPerChar;	/*Maximum # bytes per char in this codepage */
    struct
      {				/*initial values of some members of the mutable part of object */
	uint32_t toUnicodeStatus;
	int8_t subCharLen;
	unsigned char subChar[UCNV_MAX_SUBCHAR_LEN];
      }
    defaultConverterValues;
    UConverterTable *table;	/*Pointer to conversion data */
  }
UConverterSharedData_1_4;

/**
 * Un flatten shared data from a UDATA..
 */
U_CAPI  UConverterSharedData* U_EXPORT2 ucnv_data_unFlattenClone(UDataMemory *pData, UErrorCode *status);


/*initializes some global variables */
UHashtable *SHARED_DATA_HASHTABLE = NULL;

/*Returns uppercased string */
char *
  strtoupper (char *name)
{
  int32_t i = 0;

  while (name[i] = uprv_toupper (name[i]))
    i++;

  return name;
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
  UConverter *myConverter = NULL;
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

  myConverter->sharedData = ucnv_data_unFlattenClone(data, err);
  if(U_FAILURE(*err))
    {
      udata_close(data);
      uprv_free (myConverter);
      return NULL;
    }

  initializeDataConverter (myConverter);

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
const UConverterSharedData *
  getAlgorithmicTypeFromName (const char *realName)
{
  int i;
  for(i=0; i<sizeof(cnvNameType)/sizeof(cnvNameType[0]); ++i) {
    if(uprv_strcmp(realName, cnvNameType[i].name)==0) {
      return converterData[cnvNameType[i].type];
    }
  }
  return NULL;
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
  /* ### check to see if the element is not already there! */
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
    {
      return (UConverterSharedData*)uhash_get (SHARED_DATA_HASHTABLE, uhash_hashIString (name));
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

       This means that the compact arrays would have to be static fields in
       UConverterSharedData, not pointers to allocated structures.
       Markus
    */

    if (deadSharedData->impl->unload != NULL) {
        deadSharedData->impl->unload(deadSharedData);
    }

    if(deadSharedData->dataMemory != NULL)
    {
        UDataMemory *data = (UDataMemory*)deadSharedData->dataMemory;
        udata_close(data);
    }

    uprv_free (deadSharedData);
    
    return TRUE;
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

  mySharedConverterData = (UConverterSharedData *)getAlgorithmicTypeFromName (realName);
  if (mySharedConverterData == NULL)
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

          /* ### this is unsafe: the shared data could have been deleted since sharing or getting it - these operations should increase the counter! */
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
      myUConverter = (UConverter *) uprv_malloc (sizeof (UConverter));
      if (myUConverter == NULL)
        {
          *err = U_MEMORY_ALLOCATION_ERROR;
          return NULL;
        }

      /*initializes the converter */
      uprv_memset(myUConverter, 0, sizeof(UConverter));
      myUConverter->sharedData = mySharedConverterData;
      initializeAlgorithmicConverter (myUConverter);
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
 * based on their type
 */
void 
  initializeAlgorithmicConverter (UConverter * myConverter)
{
  myConverter->mode = UCNV_SI;
  myConverter->fromCharErrorBehaviour = (UConverterToUCallback) UCNV_TO_U_CALLBACK_SUBSTITUTE;
  myConverter->fromUCharErrorBehaviour = (UConverterFromUCallback) UCNV_FROM_U_CALLBACK_SUBSTITUTE;
  myConverter->charErrorBufferLength = 0;
  myConverter->UCharErrorBufferLength = 0;

  myConverter->extraInfo = NULL;

  myConverter->fromUnicodeStatus = 0;
  myConverter->toUnicodeStatus = myConverter->sharedData->defaultConverterValues.toUnicodeStatus;
  myConverter->subCharLen = myConverter->sharedData->defaultConverterValues.subCharLen;
  uprv_memcpy (myConverter->subChar, myConverter->sharedData->defaultConverterValues.subChar, UCNV_MAX_SUBCHAR_LEN);

  if(myConverter->sharedData->impl->open != NULL) {
    UErrorCode errorCode = U_ZERO_ERROR;

    /* ### pass in real parameters, and use the error code */
    myConverter->sharedData->impl->open(myConverter, NULL, NULL, &errorCode);
  }
}

UConverterSharedData* ucnv_data_unFlattenClone(UDataMemory *pData, UErrorCode *status)
{
    const uint8_t *raw = (const uint8_t *)udata_getMemory(pData);
    const UConverterSharedData_1_4 *source = (const UConverterSharedData_1_4 *) raw;
    UConverterSharedData *data;
    UConverterType type = source->conversionType;

    if(U_FAILURE(*status))
        return NULL;

    if( (uint16_t)type >= UCNV_NUMBER_OF_SUPPORTED_CONVERTER_TYPES ||
        converterData[type]->referenceCounter != 1 ||
        source->structSize != sizeof(UConverterSharedData_1_4))
    {
        *status = U_INVALID_TABLE_FORMAT;
        return NULL;
    }

    data = (UConverterSharedData *)uprv_malloc(sizeof(UConverterSharedData));
    if(data == NULL) {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    /* copy initial values from the static structure for this type */
    uprv_memcpy(data, converterData[type], sizeof(UConverterSharedData));

    /* ### it would be much more efficient if the table were a direct member, not a pointer */
    data->table = (UConverterTable *)uprv_malloc(sizeof(UConverterTable));
    if(data->table == NULL) {
        uprv_free(data);
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    /* fill in fields from the loaded data */
    data->dataMemory = (void*)pData; /* for future use */
    data->name = source->name; /* ### this could/should come from the caller - should be the same as the canonical name?!! */
    data->codepage = source->codepage;
    data->platform = source->platform;
    data->minBytesPerChar = source->minBytesPerChar;
    data->maxBytesPerChar = source->maxBytesPerChar;
    uprv_memcpy(&data->defaultConverterValues, &source->defaultConverterValues, sizeof(data->defaultConverterValues));

    if(data->impl->load != NULL) {
        data->impl->load(data, raw + source->structSize, status);
        if(U_FAILURE(*status)) {
            uprv_free(data);
            return NULL;
        }
    }
    return data;
}
