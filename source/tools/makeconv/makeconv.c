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
 *  makeconv.c:
 *  tool creating a binary (compressed) representation of the conversion mapping
 *  table (IBM NLTC ucmap format).
 */
   
#include <stdio.h>
#include "ucmp16.h"
#include "ucmp8.h"
#include "ucnv_io.h"
#include "ucnv_bld.h"
#include "ucnv_err.h"
#include "cstring.h"
#include "cmemory.h"
#include "filestrm.h"


/*Reads the header of the table file and fills in basic knowledge about the converter
 *in "converter"
 */
static void readHeaderFromFile(UConverter* myConverter, FileStream* convFile, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary*/
static void loadMBCSTableFromFile(FileStream* convFile, UConverter* converter, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary*/
static void loadEBCDIC_STATEFULTableFromFile(FileStream* convFile, UConverter* converter, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary*/
static void loadSBCSTableFromFile(FileStream* convFile, UConverter* converter, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary*/
static void loadDBCSTableFromFile(FileStream* convFile, UConverter* converter, UErrorCode* err);

/* creates a UConverterSharedData from a mapping file, fills in necessary links to it the 
 * appropriate function pointers
 * if the data tables are already in memory
 */
static UConverterSharedData* createConverterFromTableFile(const char* realName, UErrorCode* err);


/*writes a CompactShortArray to a file*/
static void writeCompactShortArrayToFile(FileStream* outfile, const CompactShortArray* myArray);

/*writes a CompactByteArray to a file*/
static void writeCompactByteArrayToFile(FileStream* outfile, const CompactByteArray* myArray);

/*writes a binary to a file*/
static void writeUConverterSharedDataToFile(const char* filename, 
						  UConverterSharedData* mySharedData, 
						  UErrorCode* err);

static UCNV_PLATFORM getPlatformFromName(char* name);
static int32_t getCodepageNumberFromName(char* name);


static const char NLTC_SEPARATORS[9] = { '\r', '\n', '\t', ' ', '<', '>' ,'"' , 'U', '\0' };
static const char PLAIN_SEPARATORS[9] = { '\r', '\n', '\t', ' ', '<', '>' ,'"' ,  '\0' };
static const char CODEPOINT_SEPARATORS[8] = {  '\r', '>', '\\', 'x', '\n', ' ', '\t', '\0' };
static const char UNICODE_CODEPOINT_SEPARATORS[6] = {  '<', '>', 'U', ' ', '\t', '\0' };



int main(int argc, char** argv)
{
  UConverterSharedData* mySharedData = NULL; 
  UErrorCode err = U_ZERO_ERROR;
  char outFileName[MAX_FULL_FILE_NAME_LENGTH];
  char* dot = NULL;

  
  if (argc == 1)
    {
      /*prints out a usage message*/
      printf("usage: %s file1 file2 file3 ...\n", argv[0]);
    }
  while (--argc)
    {
      /*removes the extension if any is found*/
      icu_strcpy(outFileName, argv[argc]);
      if (dot = icu_strchr(outFileName +   icu_strlen(outFileName) - 4, '.')) 
	{
	  *dot = '\0';
	}
      /*Adds the target extension*/
      icu_strcat(outFileName, CONVERTER_FILE_EXTENSION);
      
      mySharedData = createConverterFromTableFile(argv[argc], &err);
      if (FAILURE(err) || (mySharedData == NULL))
	{
	  /* in an error is found, print out a error msg and keep going*/
	  printf("Error creating \"%s\" file for \"%s\" (error code %d)\n", outFileName, argv[argc], err);
	  err = U_ZERO_ERROR;
	}
      else
	{
	  writeUConverterSharedDataToFile(outFileName, mySharedData, &err);
	  deleteSharedConverterData(mySharedData);
	  puts(outFileName);
	}
      
    }

  return err;
      
}


/*Streams out to a file a compact short array*/
void writeCompactShortArrayToFile(FileStream* outfile, const CompactShortArray* myArray)
{
  int32_t i = 0;
  const int16_t* myShortArray = NULL;
  const uint16_t* myIndexArray = NULL;
  int32_t myValuesCount = 0;
  int32_t myIndexCount = ucmp16_getkUnicodeCount() / ucmp16_getkBlockCount();
  int32_t myBlockShift = myArray->kBlockShift;
  
  /*streams out the length of the arrays to come*/
  myValuesCount = myArray->fCount;
  T_FileStream_write(outfile, &myValuesCount, sizeof(int32_t));
  T_FileStream_write(outfile, &myIndexCount, sizeof(int32_t));
  T_FileStream_write(outfile, &myBlockShift, sizeof(int32_t));

  /*Gets pointers to the internal arrays*/
  myShortArray = myArray->fArray;
  myIndexArray = myArray->fIndex;

  /*streams out the 2 arrays*/
  T_FileStream_write(outfile, myShortArray, myValuesCount*sizeof(int16_t));
  T_FileStream_write(outfile, myIndexArray, myIndexCount*sizeof(uint16_t));
  
  
  return ;
}

void writeCompactByteArrayToFile(FileStream* outfile, const CompactByteArray* myArray)
{
  int32_t i = 0;
  const int8_t* myByteArray = NULL;
  const uint16_t* myIndexArray = NULL;
  int32_t myValuesCount = 0;
  int32_t myIndexCount = ucmp8_getkUnicodeCount() / ucmp8_getkBlockCount();
  
  /*streams out the length of the arrays to come*/
  myValuesCount = ucmp8_getCount(myArray);
  T_FileStream_write(outfile, &myValuesCount, sizeof(int32_t));
  T_FileStream_write(outfile, &myIndexCount, sizeof(int32_t));

  /*Gets pointers to the internal arrays*/
  myByteArray =  myArray->fArray;
  myIndexArray =  myArray->fIndex;

  /*streams out the 2 arrays*/
  T_FileStream_write(outfile, myByteArray, myValuesCount*sizeof(int8_t));
  T_FileStream_write(outfile, myIndexArray, myIndexCount*sizeof(uint16_t));
  
  return ;
}

void writeUConverterSharedDataToFile(const char* filename,
				     UConverterSharedData* mySharedData,
				     UErrorCode* err)
{
  int32_t i = 0;
  const int8_t* myByteArray = NULL;
  const uint16_t* myIndexArray = NULL;
  int32_t myValuesCount = 0;
  int32_t myIndexCount = 0;
  int32_t myCheck = FILE_CHECK_MARKER;
  FileStream* outfile = NULL;
  ConverterTable* myTableAlias = NULL;
  
  if (FAILURE(*err)) return;
  
  outfile = T_FileStream_open(filename, "wb");
  if (outfile == NULL) 
    {
      *err = U_FILE_ACCESS_ERROR;
      return;
    }

  /*Writes a Sentinel value*/
  T_FileStream_write(outfile, &myCheck, sizeof(int32_t));
  T_FileStream_write(outfile, COPYRIGHT_STRING, COPYRIGHT_STRING_LENGTH);
  
  /*Writes NULL in places where there is a pointer in order
   *to enable bitwise equivalence of binary files
   */
  myTableAlias = mySharedData->table;
  mySharedData->table = NULL;
  T_FileStream_write(outfile, mySharedData, sizeof(UConverterSharedData));
  mySharedData->table = myTableAlias;
  
  switch (mySharedData->conversionType)
    {
    case SBCS :
      {
	T_FileStream_write(outfile, mySharedData->table->sbcs.toUnicode, 256*sizeof(UChar));
	writeCompactByteArrayToFile(outfile, mySharedData->table->sbcs.fromUnicode);
      }break;
    case DBCS : case EBCDIC_STATEFUL:
      {
	writeCompactShortArrayToFile(outfile, mySharedData->table->dbcs.toUnicode);
	writeCompactShortArrayToFile(outfile, mySharedData->table->dbcs.fromUnicode);
      }break;
    case MBCS : 
      {
	T_FileStream_write(outfile, mySharedData->table->mbcs.starters, 256*sizeof(bool_t));
	writeCompactShortArrayToFile(outfile, mySharedData->table->mbcs.toUnicode);
	writeCompactShortArrayToFile(outfile, mySharedData->table->mbcs.fromUnicode);
      }break;
      
    };

  if (T_FileStream_error(outfile)) 
    {
      *err = U_FILE_ACCESS_ERROR;
    }
  T_FileStream_close(outfile);
}


void copyPlatformString(char* platformString, UCNV_PLATFORM pltfrm)
{
  switch (pltfrm)
    {
    case IBM: {icu_strcpy(platformString, "ibm");break;}
    default: {icu_strcpy(platformString, "");break;}
    };
 
  return;
}

UCNV_PLATFORM getPlatformFromName(char* name)
{
  char myPlatform[10];
  char mySeparators[2] = { '-', '\0' };
  
  getToken(myPlatform, name, mySeparators);
  strtoupper(myPlatform);

  if (icu_strcmp(myPlatform, "IBM") == 0) return IBM;
  else return UNKNOWN;
}

int32_t getCodepageNumberFromName(char* name)
{
  char myNumber[10];
  char mySeparators[2] = { '-', '\0' };
  char* line = NULL;
 
  line = getToken(myNumber, name, mySeparators);
  getToken(myNumber, line, mySeparators);

  return T_CString_stringToInteger(myNumber, 10);
}

/*Reads the header of the table file and fills in basic knowledge about the converter in "converter"*/
void readHeaderFromFile(UConverter* myConverter,
			FileStream* convFile,
			UErrorCode* err)
{
  char storeLine[MAX_LINE_TEXT];
  char key[15];
  char value[30];
  char* line = storeLine;
  bool_t endOfHeader = FALSE;
  bool_t hasConvClass = FALSE;
  bool_t hasSubChar = FALSE;
  char codepointByte[3];

  if (FAILURE(*err)) return;
  while (!endOfHeader && T_FileStream_readLine(convFile, line, MAX_LINE_TEXT)) 
    {
      removeComments(line);
      
      /*skip blank lines*/
      if (*(line + nextTokenOffset(line, NLTC_SEPARATORS)) != '\0') 
	{	  
	  /*gets the key that will qualify adjacent information*/
	  /*gets the adjacent value*/
	  line = getToken(key, line, NLTC_SEPARATORS);
	  if (icu_strcmp(key, "uconv_class"))
	    line = getToken(value, line, NLTC_SEPARATORS);	      
	  else
	    line = getToken(value, line, PLAIN_SEPARATORS);	      

	  
	  /*
	     Figure out what key was found and fills in myConverter with the appropriate values
	     a switch statement for strings...
	     */
	  
	  /*Checks for end of header marker*/
	  if (icu_strcmp(key, "CHARMAP") == 0) endOfHeader = TRUE;

	  /*get name tag*/
	  else if (icu_strcmp(key, "code_set_name") == 0) 
	    {
	      icu_strcpy(myConverter->sharedData->name, value);
	      myConverter->sharedData->platform = getPlatformFromName(value);
	      myConverter->sharedData->codepage = getCodepageNumberFromName(value);
	      
	    }

	  /*get conversion type*/
	  else if (icu_strcmp(key, "uconv_class") == 0)
	    {

	      hasConvClass = TRUE;
	      if (icu_strcmp(value, "DBCS") == 0) 
		{
		  myConverter->sharedData->conversionType = DBCS;
		}
	      else if (icu_strcmp(value, "SBCS") == 0) 
		{
		  myConverter->sharedData->conversionType = SBCS;
		}
	      else if (icu_strcmp(value, "MBCS") == 0) 
		{
		  myConverter->sharedData->conversionType = MBCS;
		}
	      else if (icu_strcmp(value, "EBCDIC_STATEFUL") == 0) 
		{
		  myConverter->sharedData->conversionType = EBCDIC_STATEFUL;
		}
	      else 
		{
		  *err = U_INVALID_TABLE_FORMAT;
		  return;
		}

	    }
	  
	  /*get mb_cur_max amount*/
	  else if (icu_strcmp(key, "mb_cur_max") == 0) 
	    myConverter->sharedData->maxBytesPerChar = (int8_t)T_CString_stringToInteger(value, 10);
	  
	  /*get mb_cur_max amount*/
	  else if (icu_strcmp(key, "mb_cur_min") == 0)
	    myConverter->sharedData->minBytesPerChar = (int8_t)T_CString_stringToInteger(value, 10);
	 
	  
	  else if (icu_strcmp(key, "subchar") == 0) 
	    {
	      hasSubChar = TRUE;
	      myConverter->sharedData->defaultConverterValues.subCharLen = 0;
	      
	      /*readies value for tokenizing, we want to break each byte of the codepoint into single tokens*/
	      line = value;
	      while (*line)
		{
		  line = getToken(codepointByte, line, CODEPOINT_SEPARATORS);
		  myConverter->sharedData->defaultConverterValues.subChar[(myConverter->sharedData->defaultConverterValues.subCharLen++)] =
		    (unsigned char)T_CString_stringToInteger(codepointByte, 16);
		}
	      
	      /*Initializes data from the mutable area to that found in the immutable area*/
	      
	    }	  
	}
      /*make line point to the beginning of the storage buffer again*/
      line = storeLine;
    }

  if (!hasSubChar)   {myConverter->subCharLen = myConverter->sharedData->defaultConverterValues.subCharLen = 0;}
  else 
    {
      myConverter->subCharLen = myConverter->sharedData->defaultConverterValues.subCharLen;
      icu_memcpy(myConverter->subChar,
		 myConverter->sharedData->defaultConverterValues.subChar, 
		 myConverter->subCharLen);
    }
  
  
  if (!endOfHeader || !hasConvClass)     *err = U_INVALID_TABLE_FORMAT;
  return;
}
  
  

void loadSBCSTableFromFile(FileStream* convFile, UConverter* myConverter, UErrorCode* err)
{
  char storageLine[MAX_LINE_TEXT];
  char* line = NULL;
  ConverterTable* myConverterTable = NULL;
  UChar unicodeValue = 0xFFFF;
  int32_t sbcsCodepageValue = 0;
  char codepointBytes[5];
  unsigned char replacementChar = '\0';
  int32_t i = 0;
  CompactByteArray* myFromUnicode = NULL;

  
  if (FAILURE(*err)) return;
  replacementChar = myConverter->subChar[0];
  myConverterTable = (ConverterTable*)icu_malloc(sizeof(SBCS_TABLE));
  if (myConverterTable == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }

  /*create a compact array with replacement chars as default chars*/
  myFromUnicode = ucmp8_open(0);  
  if (myFromUnicode == NULL) 
    {
      icu_free(myConverterTable);
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    } 
  
  /*fills in the toUnicode array with the Unicode Replacement Char*/
  for (i=0;i<255;i++) myConverterTable->sbcs.toUnicode[i] = unicodeValue;

  
  while (T_FileStream_readLine(convFile, storageLine, MAX_LINE_TEXT))
    {
      /*removes comments*/
      removeComments(storageLine);

      /*set alias pointer back to the beginning of the buffer*/
      line = storageLine;
      
      /*skips empty lines*/
      if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
	{
	  line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
	  if (!icu_strcmp(codepointBytes, "END")) break;
	  unicodeValue = (UChar)T_CString_stringToInteger(codepointBytes, 16);
	  line = getToken(codepointBytes, line, CODEPOINT_SEPARATORS);
	  sbcsCodepageValue = T_CString_stringToInteger(codepointBytes, 16);
	  /*Store in the toUnicode array*/
	  myConverterTable->sbcs.toUnicode[sbcsCodepageValue] = unicodeValue;
	  /*Store in the fromUnicode compact array*/
	  ucmp8_set(myFromUnicode, unicodeValue, (int8_t)sbcsCodepageValue);
	}
    }
  ucmp8_compact(myFromUnicode, 1);
  myConverterTable->sbcs.fromUnicode = myFromUnicode;
  /*Initially sets the referenceCounter to 1*/
  myConverter->sharedData->referenceCounter = 1;
  myConverter->sharedData->table = myConverterTable;
  
  return;
}

void loadMBCSTableFromFile(FileStream* convFile, UConverter* myConverter, UErrorCode* err)
{
  char storageLine[MAX_LINE_TEXT];
  char* line = NULL;
  ConverterTable* myConverterTable = NULL;
  UChar unicodeValue = 0xFFFF;
  int32_t mbcsCodepageValue = '\0';
  char codepointBytes[6];
  int32_t replacementChar = 0x0000;
  uint16_t i = 0;
  CompactShortArray* myFromUnicode = NULL;
  CompactShortArray* myToUnicode = NULL;

  /*Evaluates the replacement codepoint*/
  replacementChar = 0xFFFF;

  myConverterTable = (ConverterTable*)icu_malloc(sizeof(MBCS_TABLE));
  if (myConverterTable == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }

  /*Initializes the mbcs.starters to FALSE*/

  for (i=0; i<=255; i++) 
    {
      myConverterTable->mbcs.starters[i] = FALSE;
    } 

  myFromUnicode = ucmp16_open((uint16_t)replacementChar);
  myToUnicode = ucmp16_open((int16_t)0xFFFD);  
  
  while (T_FileStream_readLine(convFile, storageLine, MAX_LINE_TEXT))
    {
      removeComments(storageLine);
      line = storageLine;
      if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
	{
	  line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
	  if (!icu_strcmp(codepointBytes, "END")) break;
	  unicodeValue = (UChar)T_CString_stringToInteger(codepointBytes, 16);
	  line = getToken(codepointBytes, line, CODEPOINT_SEPARATORS);
	  if (line[nextTokenOffset(line, CODEPOINT_SEPARATORS)] != '\0') 
	    {
	      /*When there is a second byte*/
	      myConverterTable->mbcs.starters[T_CString_stringToInteger(codepointBytes, 16)] = TRUE;
	      line = getToken(codepointBytes+2, line, CODEPOINT_SEPARATORS);
	    }

	  mbcsCodepageValue = T_CString_stringToInteger(codepointBytes, 16);
	  
	  ucmp16_set(myToUnicode, (int16_t)mbcsCodepageValue, unicodeValue);
	  ucmp16_set(myFromUnicode, unicodeValue, (int16_t)mbcsCodepageValue);
	}
    }

  ucmp16_compact(myFromUnicode);
  ucmp16_compact(myToUnicode);
  myConverterTable->mbcs.fromUnicode = myFromUnicode;
  myConverterTable->mbcs.toUnicode = myToUnicode;
  myConverter->sharedData->referenceCounter = 1;
  myConverter->sharedData->table = myConverterTable;

  /* if the default subCharLen is > 1 we need to insert it in the data structure
     so that we know how to transition */
  if (myConverter->subCharLen > 1)
    {
      myConverter->sharedData->table->mbcs.starters[myConverter->subChar[0]] = TRUE;
    }
  return;
}

void loadEBCDIC_STATEFULTableFromFile(FileStream* convFile, UConverter* myConverter, UErrorCode* err)
{
  char storageLine[MAX_LINE_TEXT];
  char* line = NULL;
  ConverterTable* myConverterTable = NULL;
  UChar unicodeValue = 0xFFFF;
  int32_t mbcsCodepageValue = '\0';
  char codepointBytes[6];
  int32_t replacementChar = 0x0000;
  uint8_t i = 0;
  CompactShortArray* myFromUnicode = NULL;
  CompactShortArray* myToUnicode = NULL;

  /*Evaluates the replacement codepoint*/
  replacementChar = 0xFFFF;

  myConverterTable = (ConverterTable*)icu_malloc(sizeof(MBCS_TABLE));
  if (myConverterTable == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
  
  
  myFromUnicode = ucmp16_open((uint16_t)replacementChar);
  myToUnicode = ucmp16_open((int16_t)0xFFFD);  
  
  while (T_FileStream_readLine(convFile, storageLine, MAX_LINE_TEXT))
    {
      removeComments(storageLine);
      line = storageLine;
      if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
	{
	  line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
	  if (!icu_strcmp(codepointBytes, "END")) break;
	  unicodeValue = (UChar)T_CString_stringToInteger(codepointBytes, 16);
	  line = getToken(codepointBytes, line, CODEPOINT_SEPARATORS);
	  if (line[nextTokenOffset(line, CODEPOINT_SEPARATORS)] != '\0') 
	    {
	      /*two-byter!*/
	      line = getToken(codepointBytes+2, line, CODEPOINT_SEPARATORS);
	    }
	  
	  mbcsCodepageValue = T_CString_stringToInteger(codepointBytes, 16);
	  
	  ucmp16_set(myToUnicode, (int16_t)mbcsCodepageValue, unicodeValue);
	  ucmp16_set(myFromUnicode, unicodeValue, (int16_t)mbcsCodepageValue);
	}
    }

  ucmp16_compact(myFromUnicode);
  ucmp16_compact(myToUnicode);
  myConverterTable->dbcs.fromUnicode = myFromUnicode;
  myConverterTable->dbcs.toUnicode = myToUnicode;
  myConverter->sharedData->referenceCounter = 1;
  myConverter->sharedData->table = myConverterTable;

  /* if the default subCharLen is > 1 we need to insert it in the data structure
     so that we know how to transition */
  if (myConverter->subCharLen > 1)
    {
      myConverter->sharedData->table->mbcs.starters[myConverter->subChar[0]] = TRUE;
    }
  return;
}


void loadDBCSTableFromFile(FileStream* convFile, UConverter* myConverter, UErrorCode* err)
{
  char storageLine[MAX_LINE_TEXT];
  char* line = NULL;
  ConverterTable* myConverterTable = NULL;
  UChar unicodeValue = 0xFFFD;
  int32_t dbcsCodepageValue = '\0';
  char codepointBytes[6];
  int32_t replacementChar = 0x0000;
  uint8_t i = 0;
  CompactShortArray* myFromUnicode = NULL;
  CompactShortArray* myToUnicode = NULL;
  
  /*Evaluates the replacement codepoint*/
  replacementChar = 0xFFFF;

  myConverterTable = (ConverterTable*)icu_malloc(sizeof(DBCS_TABLE));
  if (myConverterTable == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
  
  myFromUnicode = ucmp16_open((int16_t)replacementChar);
  myToUnicode = ucmp16_open((int16_t)0xFFFD);  
  
  while (T_FileStream_readLine(convFile, storageLine, MAX_LINE_TEXT))
    {
      removeComments(storageLine);
      line = storageLine;
      if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
	{
	  line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
	  if (!icu_strcmp(codepointBytes, "END")) break;
	  unicodeValue = (UChar)T_CString_stringToInteger(codepointBytes, 16);
	  
	  /*first byte*/
	  line = getToken(codepointBytes, line, CODEPOINT_SEPARATORS);
	  
	  /*second byte*/
	  line = getToken(codepointBytes+2, line, CODEPOINT_SEPARATORS);
	}
      
      dbcsCodepageValue = T_CString_stringToInteger(codepointBytes, 16);
      ucmp16_set(myToUnicode, (int16_t)dbcsCodepageValue, unicodeValue);
      ucmp16_set(myFromUnicode, unicodeValue, (int16_t)dbcsCodepageValue);
    }
  
  ucmp16_compact(myFromUnicode);
  ucmp16_compact(myToUnicode);
  myConverterTable->dbcs.fromUnicode = myFromUnicode;
  myConverterTable->dbcs.toUnicode = myToUnicode;
  myConverter->sharedData->referenceCounter = 1;
  myConverter->sharedData->table = myConverterTable;
  
  return;
}

/*deletes the "shared" type object*/
bool_t deleteSharedConverterData(UConverterSharedData* deadSharedData)
{
  if (deadSharedData->conversionType == SBCS)
    {
      ucmp8_close(deadSharedData->table->sbcs.fromUnicode);
      icu_free(deadSharedData->table);
      icu_free(deadSharedData);
    }
  else if (deadSharedData->conversionType == MBCS)
    {
      ucmp16_close(deadSharedData->table->mbcs.fromUnicode);
      ucmp16_close(deadSharedData->table->mbcs.toUnicode);
      icu_free(deadSharedData->table);
      icu_free(deadSharedData);
    }
  else if ((deadSharedData->conversionType == DBCS) || (deadSharedData->conversionType == EBCDIC_STATEFUL))
    {
      ucmp16_close(deadSharedData->table->dbcs.fromUnicode);
      ucmp16_close(deadSharedData->table->dbcs.toUnicode);
      icu_free(deadSharedData->table);
      icu_free(deadSharedData);
    }
  else
    {
      icu_free(deadSharedData);
    }
  return TRUE;
}



/*creates a UConverter, fills in necessary links to it the appropriate function pointers*/
UConverterSharedData* createConverterFromTableFile(const char* converterName, UErrorCode* err)
{
  FileStream* convFile = NULL;
  int32_t i = 0;
  UConverterSharedData* mySharedData = NULL;
  UConverter myConverter;


  if (FAILURE(*err)) return NULL;
  
  convFile = T_FileStream_open(converterName, "r");
  if (convFile == NULL) 
    {
      *err = U_FILE_ACCESS_ERROR;
      return NULL;
    }
  
  
  mySharedData = (UConverterSharedData*) icu_malloc(sizeof(UConverterSharedData));
  if (mySharedData == NULL)
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      T_FileStream_close(convFile);
    }
  
  myConverter.sharedData = mySharedData;
  readHeaderFromFile(&myConverter, convFile, err);

  if (FAILURE(*err)) return NULL;
  
  switch (mySharedData->conversionType)
    {
    case SBCS: 
      {
  	loadSBCSTableFromFile(convFile, &myConverter, err);
	break;
      }
    case MBCS: 
      {
	loadMBCSTableFromFile(convFile, &myConverter, err);
	break;
      }
    case EBCDIC_STATEFUL: 
      {
	loadEBCDIC_STATEFULTableFromFile(convFile, &myConverter, err);
	break;
      }
    case DBCS: 
      {
	loadDBCSTableFromFile(convFile, &myConverter, err);
	break;
      }

    default : break;
    };

  T_FileStream_close(convFile);
  return mySharedData;
}
