/*
 ********************************************************************************
 *                                                                              *
 * COPYRIGHT:                                                                   *
 *   (C) Copyright International Business Machines Corporation, 1998            *
 *   Licensed Material - Program-Property of IBM - All Rights Reserved.         *
 *   US Government Users Restricted Rights - Use, duplication, or disclosure    *
 *   restricted by GSA ADP Schedule Contract with IBM Corp.                     *
 *                                                                              *
 ********************************************************************************
 *
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
#include "ucnv_bld.h"
#include "ucnv_err.h"
#include "umutex.h"
#include "cstring.h"
#include "cmemory.h"
#include "filestrm.h"

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
 *e.g. ibm-949 = platform type = IBM and codepage # = 949
 *the functions below implement that
 */
static UCNV_PLATFORM getPlatformFromName (char *name);
static int32_t getCodepageNumberFromName (char *name);

static UCNV_TYPE getAlgorithmicTypeFromName (const char *realName);


/*these functions initialize the lightweight mutable part of the
 *object to correct values based on the sharedData defaults.
 */
static void initializeDataConverter (UConverter * myConverter);
static void initializeAlgorithmicConverter (UConverter * myConverter);
/**
 *hash function for UConverterSharedData
 */

static int32_t uhash_hashSharedData (void *sharedData);


/*initializes some global variables */
UHashtable *SHARED_DATA_HASHTABLE = NULL;
UHashtable *ALGORITHMIC_CONVERTERS_HASHTABLE = NULL;



CompactShortArray*  createCompactShortArrayFromFile (FileStream * infile, UErrorCode * err)
{
  int32_t i = 0;
  int16_t *myShortArray = NULL;
  uint16_t *myIndexArray = NULL;
  int32_t myValuesCount = 0;
  int32_t myIndexCount = 0;
  int32_t myBlockShift = 0;

  if (FAILURE (*err))
    return NULL;

  /*reads in the lengths of the 2 serialized array */
  T_FileStream_read (infile, &myValuesCount, sizeof (int32_t));
  T_FileStream_read (infile, &myIndexCount, sizeof (int32_t));
  T_FileStream_read (infile, &myBlockShift, sizeof (int32_t));

  if (myValuesCount < 0)
    {
      *err = INVALID_TABLE_FILE;
      return NULL;
    }
  myShortArray = (int16_t *) icu_malloc (myValuesCount * sizeof (int16_t));
  if (myShortArray == NULL)
    {
      *err = MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  /*reads in the first array */
  T_FileStream_read (infile, myShortArray, myValuesCount * sizeof (int16_t));

  if (myIndexCount < 0)
    {
      icu_free (myShortArray);
      *err = INVALID_TABLE_FILE;
      return NULL;
    }

  myIndexArray = (uint16_t *) icu_malloc (myIndexCount * sizeof (uint16_t));
  if (myIndexArray == NULL)
    {
      icu_free (myShortArray);
      *err = MEMORY_ALLOCATION_ERROR;
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

  if (FAILURE (*err))
    return NULL;

  /*reads in the lengths of the 2 serialized array */
  T_FileStream_read (infile, &myValuesCount, sizeof (int32_t));
  T_FileStream_read (infile, &myIndexCount, sizeof (int32_t));

  if (myValuesCount < 0)
    {
      *err = INVALID_TABLE_FILE;
      return NULL;
    }
  myByteArray = (int8_t *) icu_malloc (myValuesCount * sizeof (int8_t));
  if (myByteArray == NULL)
    {
      *err = MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  /*reads in the first array */
  T_FileStream_read (infile, myByteArray, myValuesCount * sizeof (int8_t));

  if (myIndexCount < 0)
    {
      icu_free (myByteArray);
      *err = INVALID_TABLE_FILE;
      return NULL;
    }
  myIndexArray = (uint16_t *) icu_malloc (myIndexCount * sizeof (uint16_t));
  if (myIndexArray == NULL)
    {
      icu_free (myByteArray);
      *err = MEMORY_ALLOCATION_ERROR;
      return NULL;
    }
  /*reads in the second array */
  T_FileStream_read (infile, myIndexArray, myIndexCount * sizeof (uint16_t));

  /*create a compact array from the data just read
   *that adopts our newly created arrays
   */
  return ucmp8_openAdopt (myIndexArray, myByteArray, myValuesCount);
}

UConverter*  createConverterFromFile (const char *fileName, UErrorCode * err)
{
  int32_t i = 0;
  const int8_t *myByteArray = NULL;
  const uint16_t *myIndexArray = NULL;
  int32_t myValuesCount = 0;
  int32_t myIndexCount = 0;
  UConverter *myConverter = NULL;
  int32_t myCheck;
  FileStream *infile = NULL;
  int8_t errorLevel = 0;
  char throwAway[COPYRIGHT_STRING_LENGTH];
  if (FAILURE (*err))
    return NULL;

  infile = openConverterFile (fileName);
  if (infile == NULL)
    {
      *err = FILE_ACCESS_ERROR;
      return NULL;
    }

  /*Reads the FILE_CHECK_MARKER to assess the integrity of the file */
  T_FileStream_read (infile, &myCheck, sizeof (int32_t));
  if (myCheck != FILE_CHECK_MARKER)
    {
      T_FileStream_close (infile);
      *err = INVALID_TABLE_FILE;
      return NULL;
    }

  /*Skips the copyright*/
  T_FileStream_read(infile , throwAway, COPYRIGHT_STRING_LENGTH);
  
  myConverter = (UConverter *) icu_malloc (sizeof (UConverter));
  if (myConverter == NULL)
    {
      T_FileStream_close (infile);
      *err = MEMORY_ALLOCATION_ERROR;
      return NULL;
    }

  myConverter->sharedData =
    (UConverterSharedData *) icu_malloc (sizeof (UConverterSharedData));
  if (myConverter->sharedData == NULL)
    {
      T_FileStream_close (infile);
      icu_free (myConverter);
      *err = MEMORY_ALLOCATION_ERROR;
      return NULL;
    }

  /*Reads in the UConverterSharedData object straight from file */
  T_FileStream_read (infile, myConverter->sharedData, sizeof (UConverterSharedData));

  /*switches over the types of conversions
   *allocates appropriate amounts of memory for the table
   *and calls functions to read in the CompactArrays
   */
  switch (myConverter->sharedData->conversionType)
    {
    case SBCS:
      {
	myConverter->sharedData->table = (ConverterTable *) icu_malloc (sizeof (SBCS_TABLE));
	if (myConverter->sharedData->table == NULL)
	  {
	    icu_free (myConverter->sharedData);
	    icu_free (myConverter);
	    *err = MEMORY_ALLOCATION_ERROR;
	    break;
	  }
	T_FileStream_read (infile, myConverter->sharedData->table->sbcs.toUnicode, 256 * sizeof (UChar));
	myConverter->sharedData->table->sbcs.fromUnicode = createCompactByteArrayFromFile (infile, err);
      }
      break;

    case DBCS:
    case EBCDIC_STATEFUL:
      {
	myConverter->sharedData->table = (ConverterTable *) icu_malloc (sizeof (DBCS_TABLE));
	if (myConverter->sharedData->table == NULL)
	  {
	    icu_free (myConverter->sharedData);
	    icu_free (myConverter);
	    *err = MEMORY_ALLOCATION_ERROR;
	    break;
	  }
	myConverter->sharedData->table->dbcs.toUnicode = createCompactShortArrayFromFile (infile, err);
	myConverter->sharedData->table->dbcs.fromUnicode = createCompactShortArrayFromFile (infile, err);
      }
      break;

    case MBCS:
      {
	myConverter->sharedData->table = (ConverterTable *) icu_malloc (sizeof (MBCS_TABLE));
	if (myConverter->sharedData->table == NULL)
	  {
	    icu_free (myConverter->sharedData);
	    icu_free (myConverter);
	    *err = MEMORY_ALLOCATION_ERROR;
	    break;
	  }
	T_FileStream_read (infile, myConverter->sharedData->table->mbcs.starters, 256 * sizeof (bool_t));
	myConverter->sharedData->table->mbcs.toUnicode = createCompactShortArrayFromFile (infile, err);
	myConverter->sharedData->table->mbcs.fromUnicode = createCompactShortArrayFromFile (infile, err);
      }
      break;

    default:
      {
	/*If it isn't any of the above, the file is invalid */
	*err = INVALID_TABLE_FILE;
	icu_free (myConverter->sharedData);
	icu_free (myConverter);
      }
    };

  /*there could be a FAILURE on the createCompact{Short,Byte}ArrayFromFile
   *calls, if so we don't want to initialize
   */

  T_FileStream_close (infile);
  if (SUCCESS (*err))
    {
      initializeDataConverter (myConverter);
    }

  return myConverter;


}


void 
  copyPlatformString (char *platformString, UCNV_PLATFORM pltfrm)
{
  switch (pltfrm)
    {
    case IBM:
      {
	icu_strcpy (platformString, "ibm");
	break;
      }
    default:
      {
	icu_strcpy (platformString, "");
	break;
      }
    };

  return;
}

/*returns a converter type from a string
 */
UCNV_TYPE 
  getAlgorithmicTypeFromName (const char *realName)
{
  if (icu_strcmp (realName, "UTF8") == 0)
    return UTF8;
  else if (icu_strcmp (realName, "UTF16_BigEndian") == 0)
    return UTF16_BigEndian;
  else if (icu_strcmp (realName, "UTF16_LittleEndian") == 0)
    return UTF16_LittleEndian;
  else if (icu_strcmp (realName, "LATIN_1") == 0)
    return LATIN_1;
  else if (icu_strcmp (realName, "JIS") == 0)
    return JIS;
  else if (icu_strcmp (realName, "EUC") == 0)
    return EUC;
  else if (icu_strcmp (realName, "GB") == 0)
    return GB;
  else if (icu_strcmp (realName, "ISO_2022") == 0)
    return ISO_2022;
  else
    return UNSUPPORTED_CONVERTER;
}


UCNV_PLATFORM 
  getPlatformFromName (char *name)
{
  char myPlatform[10];
  char mySeparators[2] = {'-', '\0'};

  getToken (myPlatform, name, mySeparators);
  strtoupper (myPlatform);

  if (icu_strcmp (myPlatform, "IBM") == 0)
    return IBM;
  else
    return UNKNOWN;
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
  Mutex *sharedData = NULL;
  UErrorCode err = ZERO_ERROR;
  /*Lazy evaluates the Hashtable itself */

  if (SHARED_DATA_HASHTABLE == NULL)
    {
      UHashtable* myHT = uhash_openSize ((UHashFunction) uhash_hashSharedData, 
					 AVAILABLE_CONVERTERS,
					 &err);
      if (FAILURE (err)) return;
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

  switch (deadSharedData->conversionType)
    {

    case SBCS:
      {
	ucmp8_close (deadSharedData->table->sbcs.fromUnicode);
	icu_free (deadSharedData->table);
	icu_free (deadSharedData);
      };
      break;

    case MBCS:
      {
	ucmp16_close (deadSharedData->table->mbcs.fromUnicode);
	ucmp16_close (deadSharedData->table->mbcs.toUnicode);
	icu_free (deadSharedData->table);
	icu_free (deadSharedData);
      };
      break;

    case DBCS:
    case EBCDIC_STATEFUL:
      {
	ucmp16_close (deadSharedData->table->dbcs.fromUnicode);
	ucmp16_close (deadSharedData->table->dbcs.toUnicode);
	icu_free (deadSharedData->table);
	icu_free (deadSharedData);
      };
      break;

    default:
      icu_free (deadSharedData);
    };

  return TRUE;
}

int32_t uhash_hashIString(const void* name)
{
  char myName[MAX_CONVERTER_NAME_LENGTH];
  icu_strcpy(myName, (char*)name);
  strtoupper(myName);

  return uhash_hashString(myName);
}
bool_t   isDataBasedConverter (const char *name)
{
  Mutex *createHashTableMutex = NULL;
  int32_t i = 0;
  bool_t result = FALSE;
  UErrorCode err = ZERO_ERROR;

  /*Lazy evaluates the hashtable */
  if (ALGORITHMIC_CONVERTERS_HASHTABLE == NULL)
    {
      UHashtable* myHT;
      
      {
	  myHT = uhash_open (uhash_hashIString, &err);
	  
	  if (FAILURE (err)) return FALSE;
	  while (algorithmicConverterNames[i][0] != '\0')
	    {
	      /*Stores in the hashtable a pointer to the statically init'ed array containing
	       *the names
	       */
	      
	      uhash_put (myHT,
			 (void *) algorithmicConverterNames[i],
			 &err);
	      i++;			/*Some Compilers (Solaris WSpro and MSVC-Release Mode
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
  char realName[MAX_CONVERTER_NAME_LENGTH];
  UConverter *myUConverter = NULL;
  UConverterSharedData *mySharedConverterData = NULL;
  Mutex *updatingReferenceCounterMutex = NULL;

  if (FAILURE (*err))
    return NULL;

  if (resolveName (realName, converterName) == FALSE)
    {
      *err = INVALID_TABLE_FILE;
      return NULL;
    }


  if (isDataBasedConverter (realName))
    {
      mySharedConverterData = getSharedConverterData (realName);

      if (mySharedConverterData == NULL)
	{
	  /*Not cached, we need to stream it in from file */
	  myUConverter = createConverterFromFile (converterName, err);

	  if (FAILURE (*err) || (myUConverter == NULL))
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
	  myUConverter = (UConverter *) icu_malloc (sizeof (UConverter));
	  if (myUConverter == NULL)
	    {
	      *err = MEMORY_ALLOCATION_ERROR;
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
	  if (FAILURE (*err) || (myUConverter == NULL))
	    {
	      icu_free (myUConverter);
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
	  myUConverter = (UConverter *) icu_malloc (sizeof (UConverter));
	  if (myUConverter == NULL)
	    {
	      *err = MEMORY_ALLOCATION_ERROR;
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
  icu_memcpy (myUConverter->subChar,
	      myUConverter->sharedData->defaultConverterValues.subChar,
	      myUConverter->subCharLen);
  myUConverter->toUnicodeStatus = 0x00;
  myUConverter->fromUnicodeStatus = 0x00;
  myUConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0x00;

  myUConverter->fromCharErrorBehaviour = (UCNV_ToUCallBack) MissingCharAction_SUBSTITUTE;
  myUConverter->fromUCharErrorBehaviour = (UCNV_FromUCallBack) MissingUnicodeAction_SUBSTITUTE;
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
  myConverter->fromCharErrorBehaviour = (UCNV_ToUCallBack) MissingCharAction_SUBSTITUTE;
  myConverter->fromUCharErrorBehaviour = (UCNV_FromUCallBack) MissingUnicodeAction_SUBSTITUTE;
  myConverter->charErrorBufferLength = 0;
  myConverter->UCharErrorBufferLength = 0;

  myConverter->extraInfo = NULL;


  switch (myConverter->sharedData->conversionType)
    {
    case UTF8:
      {
	myConverter->sharedData->minBytesPerChar = 1;
	myConverter->sharedData->maxBytesPerChar = 4;
	myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
	myConverter->sharedData->defaultConverterValues.subCharLen = 3;
	myConverter->subCharLen = 3;
	myConverter->toUnicodeStatus = 0;
    myConverter->fromUnicodeStatus = 0; /* srl */ 
	myConverter->sharedData->platform = IBM;
	myConverter->sharedData->codepage = 1208;
	icu_strcpy(myConverter->sharedData->name, "UTF8");
	icu_memcpy (myConverter->subChar, UTF8_subChar, 3);
	icu_memcpy (myConverter->sharedData->defaultConverterValues.subChar, UTF8_subChar, 3);

	break;
      }
    case LATIN_1:
      {
	myConverter->sharedData->minBytesPerChar = 1;
	myConverter->sharedData->maxBytesPerChar = 1;
	myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
	myConverter->sharedData->defaultConverterValues.subCharLen = 1;
	myConverter->subCharLen = 1;
	myConverter->toUnicodeStatus = 0;
	myConverter->sharedData->platform = IBM;
	myConverter->sharedData->codepage = 819;
	icu_strcpy(myConverter->sharedData->name, "LATIN_1");
	*(myConverter->subChar) = LATIN1_subChar;
	*(myConverter->sharedData->defaultConverterValues.subChar) = LATIN1_subChar;
	break;
      }

    case UTF16_BigEndian:
      {
	myConverter->sharedData->minBytesPerChar = 2;
	myConverter->sharedData->maxBytesPerChar = 2;
	myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
	myConverter->sharedData->defaultConverterValues.subCharLen = 2;
	myConverter->subCharLen = 2;
	myConverter->toUnicodeStatus = 0;
	myConverter->fromUnicodeStatus = 0; 
	icu_strcpy(myConverter->sharedData->name, "UTF_16BE");
	myConverter->sharedData->platform = IBM;
	myConverter->sharedData->codepage = 1200;
	icu_memcpy (myConverter->subChar, UTF16BE_subChar, 2);
	icu_memcpy (myConverter->sharedData->defaultConverterValues.subChar, UTF16BE_subChar, 2);

	break;
      }
    case UTF16_LittleEndian:
      {
	myConverter->sharedData->minBytesPerChar = 2;
	myConverter->sharedData->maxBytesPerChar = 2;
	myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
	myConverter->sharedData->defaultConverterValues.subCharLen = 2;
	myConverter->subCharLen = 2;
	myConverter->toUnicodeStatus = 0;
	myConverter->fromUnicodeStatus = 0; 
	myConverter->sharedData->platform = IBM;
	myConverter->sharedData->codepage = 1200;
	icu_strcpy(myConverter->sharedData->name, "UTF_16LE");
	icu_memcpy (myConverter->subChar, UTF16LE_subChar, 2);
	icu_memcpy (myConverter->sharedData->defaultConverterValues.subChar, UTF16LE_subChar, 2);
	break;
      }
    case EUC:
      {
	myConverter->sharedData->minBytesPerChar = 1;
	myConverter->sharedData->maxBytesPerChar = 2;
	myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
	myConverter->sharedData->defaultConverterValues.subCharLen = 2;
	myConverter->subCharLen = 2;
	myConverter->toUnicodeStatus = 0;
	icu_memcpy (myConverter->subChar, EUC_subChar, 2);
	icu_memcpy (myConverter->sharedData->defaultConverterValues.subChar, EUC_subChar, 2);
	break;
      }
    case ISO_2022:
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
	icu_strcpy(myConverter->sharedData->name, "ISO_2022");
	*(myConverter->subChar) = LATIN1_subChar;
	*(myConverter->sharedData->defaultConverterValues.subChar) = LATIN1_subChar;
	myConverter->extraInfo = icu_malloc (sizeof (UCNV_Data2022));
	((UCNV_Data2022 *) myConverter->extraInfo)->currentConverter = NULL;
	((UCNV_Data2022 *) myConverter->extraInfo)->escSeq2022Length = 0;
	break;
      }
    case GB:
      {
	myConverter->sharedData->minBytesPerChar = 2;
	myConverter->sharedData->maxBytesPerChar = 2;
	myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
	myConverter->sharedData->defaultConverterValues.subCharLen = 2;
	myConverter->subCharLen = 2;
	myConverter->toUnicodeStatus = 0;
	icu_memcpy (myConverter->subChar, GB_subChar, 2);
	icu_memcpy (myConverter->sharedData->defaultConverterValues.subChar, GB_subChar, 2);
	break;
      }
    case JIS:
      {
	myConverter->sharedData->minBytesPerChar = 2;
	myConverter->sharedData->maxBytesPerChar = 2;
	myConverter->sharedData->defaultConverterValues.toUnicodeStatus = 0;
	myConverter->sharedData->defaultConverterValues.subCharLen = 2;
	myConverter->subCharLen = 2;
	myConverter->toUnicodeStatus = 0;
	icu_memcpy (myConverter->subChar, JIS_subChar, 2);
	icu_memcpy (myConverter->sharedData->defaultConverterValues.subChar, JIS_subChar, 2);
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
  UCNV_TYPE myType = getAlgorithmicTypeFromName (actualName);

  if (FAILURE (*err))
    return NULL;

  myConverter = (UConverter *) icu_malloc (sizeof (UConverter));
  if (myConverter == NULL)
    {
      *err = MEMORY_ALLOCATION_ERROR;
      return NULL;
    }

  myConverter->sharedData = NULL;
  mySharedData = (UConverterSharedData *) icu_malloc (sizeof (UConverterSharedData));
  if (mySharedData == NULL)
    {
      *err = MEMORY_ALLOCATION_ERROR;
      icu_free (myConverter);
      return NULL;
    }
  mySharedData->table = NULL;
  icu_strcpy (mySharedData->name, actualName);
  /*Initializes the referenceCounter to 1 */
  mySharedData->referenceCounter = 1;
  mySharedData->platform = UNKNOWN;
  mySharedData->codepage = 0;
  mySharedData->conversionType = myType;
  myConverter->sharedData = mySharedData;

  initializeAlgorithmicConverter (myConverter);
  return myConverter;
}
