/*
 ********************************************************************************
 *
 *   Copyright (C) 1998-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 *
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
#include "unicode/ucnv_bld.h"
#include "unicode/ucnv_err.h"
#include "ucnv_imp.h"
#include "cstring.h"
#include "cmemory.h"
#include "filestrm.h"
#include "toolutil.h"

#include "unicode/udata.h"
#include "unewdata.h"
#include "ucmpwrit.h"

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
    uint32_t referenceCounter;  /*used to count number of clients */
    char name[UCNV_MAX_CONVERTER_NAME_LENGTH];  /*internal name of the converter */
    UConverterPlatform platform;        /*platform of the converter (only IBM now) */
    int32_t codepage;           /*codepage # (now IBM-$codepage) */
    UConverterType conversionType;      /*conversion type */
    int8_t minBytesPerChar;     /*Minimum # bytes per char in this codepage */
    int8_t maxBytesPerChar;     /*Maximum # bytes per char in this codepage */
    struct
      {                         /*initial values of some members of the mutable part of object */
        uint32_t toUnicodeStatus;
        int8_t subCharLen;
        unsigned char subChar[UCNV_MAX_SUBCHAR_LEN];
      }
    defaultConverterValues;
    UConverterTable *table;     /*Pointer to conversion data */
  }
UConverterSharedData_1_4;

struct UConverter_1_4
  {
    int32_t toUnicodeStatus;    /*Used to internalize stream status information */
    int32_t fromUnicodeStatus;
    int8_t invalidCharLength;
    int8_t invalidUCharLength;
    int8_t pad;
    int32_t mode;
    int8_t subCharLen;          /*length of the codepage specific character sequence */
    unsigned char subChar[UCNV_MAX_SUBCHAR_LEN];        /*codepage specific character sequence */
    UChar UCharErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];   /*used to store unicode data meant for 
                                                           *output stream  by the Error function pointers 
                                                         */
    unsigned char charErrorBuffer[UCNV_ERROR_BUFFER_LENGTH];    /*used to store codepage data meant for
                                                           * output stream by the Error function pointers 
                                                         */
    int8_t UCharErrorBufferLength;      /*used to indicate the number of valid UChars
                                           *in charErrorBuffer
                                         */
    int8_t charErrorBufferLength;       /*used to indicate the number of valid bytes
                                           *in charErrorBuffer
                                         */

    UChar invalidUCharBuffer[3];
    char invalidCharBuffer[UCNV_MAX_SUBCHAR_LEN];
    /*Error function pointer called when conversion issues
     *occur during a T_UConverter_fromUnicode call
     */
    void (*fromUCharErrorBehaviour) (struct UConverter_1_4 *,
                                     char **,
                                     const char *,
                                     const UChar **,
                                     const UChar *,
                                     int32_t* offsets,
                                     bool_t,
                                     UErrorCode *);
    /*Error function pointer called when conversion issues
     *occur during a T_UConverter_toUnicode call
     */
    void (*fromCharErrorBehaviour) (struct UConverter_1_4 *,
                                    UChar **,
                                    const UChar *,
                                    const char **,
                                    const char *,
                                    int32_t* offsets,
                                    bool_t,
                                    UErrorCode *);

    UConverterSharedData_1_4 *sharedData;       /*Pointer to the shared immutable part of the
                                         *converter object
                                         */
    void *extraInfo;            /*currently only used to point to a struct containing UConverter_1_4 used by iso 2022
                                   Could be used by clients writing their own call back function to
                                   pass context to them
                                 */
  };

typedef struct UConverter_1_4 UConverter_1_4;

/*Reads the header of the table file and fills in basic knowledge about the converter
 *in "converter"
 */
static void readHeaderFromFile(UConverter_1_4* myConverter, FileStream* convFile, const char* converterName, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary*/
static void loadMBCSTableFromFile(FileStream* convFile, UConverter_1_4* converter, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary*/
static void loadEBCDIC_STATEFULTableFromFile(FileStream* convFile, UConverter_1_4* converter, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary*/
static void loadSBCSTableFromFile(FileStream* convFile, UConverter_1_4* converter, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary*/
static void loadDBCSTableFromFile(FileStream* convFile, UConverter_1_4* converter, UErrorCode* err);

/* creates a UConverterSharedData_1_4 from a mapping file, fills in necessary links to it the 
 * appropriate function pointers
 * if the data tables are already in memory
 */
static UConverterSharedData_1_4* createConverterFromTableFile(const char* realName, UErrorCode* err);


/*writes a CompactShortArray to a file*/
static void writeCompactShortArrayToFile(FileStream* outfile, const CompactShortArray* myArray);

/*writes a CompactByteArray to a file*/
static void writeCompactByteArrayToFile(FileStream* outfile, const CompactByteArray* myArray);

/*writes a binary to a file*/
static void writeUConverterSharedDataToFile(const char* filename, 
                                                  UConverterSharedData_1_4* mySharedData, 
                                                  UErrorCode* err);


static void WriteConverterSharedData(UNewDataMemory *pData, const UConverterSharedData_1_4* data);

bool_t makeconv_deleteSharedConverterData(UConverterSharedData_1_4* deadSharedData);

static UConverterPlatform getPlatformFromName(char* name);
static int32_t getCodepageNumberFromName(char* name);


static const char NLTC_SEPARATORS[9] = { '\r', '\n', '\t', ' ', '<', '>' ,'"' , 'U', '\0' };
static const char PLAIN_SEPARATORS[9] = { '\r', '\n', '\t', ' ', '<', '>' ,'"' ,  '\0' };
static const char CODEPOINT_SEPARATORS[8] = {  '\r', '>', '\\', 'x', '\n', ' ', '\t', '\0' };
static const char UNICODE_CODEPOINT_SEPARATORS[6] = {  '<', '>', 'U', ' ', '\t', '\0' };

/* Remove all characters followed by '#'
 */
char *
  removeComments (char *line)
{
  char *pound = uprv_strchr (line, '#');

  if (pound != NULL)
    *pound = '\0';
  return line;
}

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

static const UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    0x63, 0x6e, 0x76, 0x74,     /* dataFormat="cnvt" */
    2, 0, 0, 0,                 /* formatVersion */
    1, 3, 1, 0                  /* dataVersion */
};


void writeConverterData(UConverterSharedData_1_4 *mySharedData, const char *cnvName, const char *cnvDir, UErrorCode *status)
{
  UNewDataMemory *mem;
  uint32_t sz2;

  mem = udata_create(cnvDir, "cnv", cnvName, &dataInfo, U_COPYRIGHT_STRING, status);
  
  WriteConverterSharedData(mem, mySharedData);

  sz2 = udata_finish(mem, status);
  
/*  printf("Done. Wrote %d bytes.\n", sz2); */
}

static void copyright() {
    printf("Copyright (C) 1998-1999, International Business Machines\n");
    printf("Corporation and others.  All Rights Reserved.\n");
}

static void usage(const char *pname, int exitcode) {
    FILE *where = exitcode ? stderr : stdout;
    fprintf(where, "%csage: %s [ -h, --help ] [ --version ] [ --copyright ] [ -d destdir ] file ...\n", exitcode ? 'u' : 'U', pname);
    if (!exitcode) {
        fputc('\n', where);
        fprintf(where, "Options: -h, --help\tdisplay this help and exit\n");
        fprintf(where, "         --version\tdisplay the tool's version and exit\n");
        fprintf(where, "         --copyright\tdisplay a copyright statement and exit\n");
        fprintf(where, "         -d destdir\tsets the output directory (defaults to ICU data dir)\n");
    }
    exit(exitcode);
}

int main(int argc, char** argv)
{
  UConverterSharedData_1_4* mySharedData = NULL; 
  UErrorCode err = U_ZERO_ERROR;
  char outFileName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
  char *pname = *argv;
  const char* destdir = 0;
  size_t destdirlen;
  char* dot = NULL, *arg, *outBasename;
  char cnvName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
 
  /* Lame getopt() thingy. */

  for (++argv; *argv && **argv == '-' && (*argv)[1]; ++argv) {
      switch ((*argv)[1]) {
          case 'd':
              if ((*argv)[2]) {
                  destdir = *argv + 2;
              } else if (*++argv) {
                  destdir = *argv;
              } else {
                  usage(pname, 1);
              }
              break;

          case 'h':
              if (!(*argv)[2]) {
                  usage(pname, 0);
              } else {
                  usage(pname, 1);
              }
              break;

          case '-':
              if (!strcmp(*argv, "--help")) {
                  usage(pname, 0);
              } else if (!strcmp(*argv, "--copyright")) {
                  copyright();
                  exit(0);
              } else if (!strcmp(*argv, "--version")) {
                  printf("makeconv version 1.0, by IBM and others\n");
                  exit(0);
              } else {
                  usage(pname, 1);
              }
              break;

          default:
              usage(pname, 1);
      }
  }

  if (!*argv) {
      usage(pname, 1);
  }

  if (!destdir) {
      destdir = u_getDataDirectory();
  }

  if (destdir != NULL && *destdir != 0) {
        uprv_strcpy(outFileName, destdir);
        destdirlen = uprv_strlen(destdir);
        outBasename = outFileName + destdirlen;
        if (*(outBasename - 1) != U_FILE_SEP_CHAR) {
            *outBasename++ = U_FILE_SEP_CHAR;
            ++destdirlen;
        }
    } else {
        destdirlen = 0;
        outBasename = outFileName;
    }

  for (; *argv; ++argv)
    {
      err = U_ZERO_ERROR;
      arg = getLongPathname(*argv);

      /*produces the right destination path for display*/
      if (destdirlen != 0)
        {
          char *basename;

          /* find the last file sepator */
          basename = uprv_strrchr(arg, U_FILE_SEP_CHAR);
          if (basename == NULL) {
              basename = arg;
          } else {
              ++basename;
          }

          uprv_strcpy(outBasename, basename);
        }
      else
        {
          uprv_strcpy(outFileName, arg);
        }

      /*removes the extension if any is found*/
      if (dot = uprv_strrchr(outBasename, '.')) 
        {
          *dot = '\0';
        }

      /* the basename without extension is the converter name */
      uprv_strcpy(cnvName, outBasename);

      /*Adds the target extension*/
      uprv_strcat(outBasename, CONVERTER_FILE_EXTENSION);

      mySharedData = createConverterFromTableFile(arg, &err);

      if (U_FAILURE(err) || (mySharedData == NULL))
        {
          /* if an error is found, print out an error msg and keep going */
          printf("Error creating \"%s\" file for \"%s\" (error code %d - %s)\n", outFileName, arg, err,
                        u_errorName(err));
          err = U_ZERO_ERROR;
        }
      else
        {
          writeConverterData(mySharedData, cnvName, destdir, &err);
          makeconv_deleteSharedConverterData(mySharedData);

          if(U_FAILURE(err))
          {
                  /* in an error is found, print out a error msg and keep going*/
                  printf("Error writing \"%s\" file for \"%s\" (error code %d - %s)\n", outFileName, arg, err,
                                u_errorName(err));
          }
          else
          {
                  puts(outFileName);
          }
        }
      
    }

  return err;
      
}


void copyPlatformString(char* platformString, UConverterPlatform pltfrm)
{
  switch (pltfrm)
    {
    case UCNV_IBM: {uprv_strcpy(platformString, "ibm");break;}
    default: {uprv_strcpy(platformString, "");break;}
    };
 
  return;
}

UConverterPlatform getPlatformFromName(char* name)
{
  char myPlatform[10];
  char mySeparators[2] = { '-', '\0' };
  
  getToken(myPlatform, name, mySeparators);
  strtoupper(myPlatform);

  if (uprv_strcmp(myPlatform, "IBM") == 0) return UCNV_IBM;
  else return UCNV_UNKNOWN;
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
void readHeaderFromFile(UConverter_1_4* myConverter,
                        FileStream* convFile,
                        const char* converterName,
                        UErrorCode* err)
{
  char storeLine[UCNV_MAX_LINE_TEXT];
  char key[15];
  char value[30];
  char* line = storeLine;
  bool_t endOfHeader = FALSE;
  bool_t hasConvClass = FALSE;
  bool_t hasSubChar = FALSE;
  char codepointByte[3];

  if (U_FAILURE(*err)) return;
  while (!endOfHeader && T_FileStream_readLine(convFile, line, UCNV_MAX_LINE_TEXT)) 
    {
      removeComments(line);
      
      /*skip blank lines*/
      if (*(line + nextTokenOffset(line, NLTC_SEPARATORS)) != '\0') 
        {         
          /*gets the key that will qualify adjacent information*/
          /*gets the adjacent value*/
          line = getToken(key, line, NLTC_SEPARATORS);
          if (uprv_strcmp(key, "uconv_class"))
            line = getToken(value, line, NLTC_SEPARATORS);            
          else
            line = getToken(value, line, PLAIN_SEPARATORS);           

          
          /*
             Figure out what key was found and fills in myConverter with the appropriate values
             a switch statement for strings...
             */
          
          /*Checks for end of header marker*/
          if (uprv_strcmp(key, "CHARMAP") == 0) endOfHeader = TRUE;

          /*get name tag*/
          else if (uprv_strcmp(key, "code_set_name") == 0) 
            {
              if (uprv_strlen(value) != 0) 
              {
                  uprv_strcpy(myConverter->sharedData->name, value);
                  myConverter->sharedData->platform = getPlatformFromName(value);
                  myConverter->sharedData->codepage = getCodepageNumberFromName(value);
              } else {
                  uprv_strcpy(myConverter->sharedData->name, converterName);
                  myConverter->sharedData->platform = UCNV_IBM;
              }
            	                    
            }

          /*get conversion type*/
          else if (uprv_strcmp(key, "uconv_class") == 0)
            {

              hasConvClass = TRUE;
              if (uprv_strcmp(value, "DBCS") == 0) 
                {
                  myConverter->sharedData->conversionType = UCNV_DBCS;
                }
              else if (uprv_strcmp(value, "SBCS") == 0) 
                {
                  myConverter->sharedData->conversionType = UCNV_SBCS;
                }
              else if (uprv_strcmp(value, "MBCS") == 0) 
                {
                  myConverter->sharedData->conversionType = UCNV_MBCS;
                }
              else if (uprv_strcmp(value, "EBCDIC_STATEFUL") == 0) 
                {
                  myConverter->sharedData->conversionType = UCNV_EBCDIC_STATEFUL;
                }
              else 
                {
                  *err = U_INVALID_TABLE_FORMAT;
                  return;
                }

            }
          
          /*get mb_cur_max amount*/
          else if (uprv_strcmp(key, "mb_cur_max") == 0) 
            myConverter->sharedData->maxBytesPerChar = (int8_t)T_CString_stringToInteger(value, 10);
          
          /*get mb_cur_max amount*/
          else if (uprv_strcmp(key, "mb_cur_min") == 0)
            myConverter->sharedData->minBytesPerChar = (int8_t)T_CString_stringToInteger(value, 10);
         
          
          else if (uprv_strcmp(key, "subchar") == 0) 
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
      uprv_memcpy(myConverter->subChar,
                 myConverter->sharedData->defaultConverterValues.subChar, 
                 myConverter->subCharLen);
    }
  
  
  if (!endOfHeader || !hasConvClass)     *err = U_INVALID_TABLE_FORMAT;
  return;
}
  
  

void loadSBCSTableFromFile(FileStream* convFile, UConverter_1_4* myConverter, UErrorCode* err)
{
  char storageLine[UCNV_MAX_LINE_TEXT];
  char* line = NULL;
  UConverterTable* myUConverterTable = NULL;
  UChar unicodeValue = 0xFFFF;
  int32_t sbcsCodepageValue = 0;
  char codepointBytes[5];
  unsigned char replacementChar = '\0';
  int32_t i = 0;
  CompactByteArray* myFromUnicode = NULL;

  
  if (U_FAILURE(*err)) return;
  replacementChar = myConverter->subChar[0];
  myUConverterTable = (UConverterTable*)uprv_malloc(sizeof(UConverterSBCSTable));
  if (myUConverterTable == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }

  /*create a compact array with replacement chars as default chars*/
  myFromUnicode = ucmp8_open(0);  
  if (myFromUnicode == NULL) 
    {
      uprv_free(myUConverterTable);
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    } 
  
  myUConverterTable->sbcs.toUnicode = (UChar*)malloc(sizeof(UChar)*256);
  /*fills in the toUnicode array with the Unicode Replacement Char*/
  for (i=0;i<255;i++) myUConverterTable->sbcs.toUnicode[i] = unicodeValue;

  
  while (T_FileStream_readLine(convFile, storageLine, UCNV_MAX_LINE_TEXT))
    {
      /*removes comments*/
      removeComments(storageLine);

      /*set alias pointer back to the beginning of the buffer*/
      line = storageLine;
      
      /*skips empty lines*/
      if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
        {
          line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
          if (!uprv_strcmp(codepointBytes, "END")) break;
          unicodeValue = (UChar)T_CString_stringToInteger(codepointBytes, 16);
          line = getToken(codepointBytes, line, CODEPOINT_SEPARATORS);
          sbcsCodepageValue = T_CString_stringToInteger(codepointBytes, 16);
          /*Store in the toUnicode array*/
          myUConverterTable->sbcs.toUnicode[sbcsCodepageValue] = unicodeValue;
          /*Store in the fromUnicode compact array*/
          ucmp8_set(myFromUnicode, unicodeValue, (int8_t)sbcsCodepageValue);
        }
    }
  ucmp8_compact(myFromUnicode, 1);
  myUConverterTable->sbcs.fromUnicode = myFromUnicode;
  /*Initially sets the referenceCounter to 1*/
  myConverter->sharedData->referenceCounter = 1;
  myConverter->sharedData->table = myUConverterTable;
  
  return;
}

void loadMBCSTableFromFile(FileStream* convFile, UConverter_1_4* myConverter, UErrorCode* err)
{
  char storageLine[UCNV_MAX_LINE_TEXT];
  char* line = NULL;
  UConverterTable* myUConverterTable = NULL;
  UChar unicodeValue = 0xFFFF;
  int32_t mbcsCodepageValue = '\0';
  char codepointBytes[6];
  int32_t replacementChar = 0x0000;
  uint16_t i = 0;
  CompactShortArray* myFromUnicode = NULL;
  CompactShortArray* myToUnicode = NULL;

  /*Evaluates the replacement codepoint*/
  replacementChar = 0xFFFF;

  myUConverterTable = (UConverterTable*)uprv_malloc(sizeof(UConverterMBCSTable));
  if (myUConverterTable == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
  
  myUConverterTable->mbcs.starters = (bool_t*)(uprv_malloc(sizeof(bool_t)*256));
  if (myUConverterTable->mbcs.starters == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
  

  /*Initializes the mbcs.starters to FALSE*/

  for (i=0; i<=255; i++) 
    {
      myUConverterTable->mbcs.starters[i] = FALSE;
    } 

  myFromUnicode = ucmp16_open((uint16_t)replacementChar);
  myToUnicode = ucmp16_open((int16_t)0xFFFD);  
  
  while (T_FileStream_readLine(convFile, storageLine, UCNV_MAX_LINE_TEXT))
    {
      removeComments(storageLine);
      line = storageLine;
      if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
        {
          line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
          if (!uprv_strcmp(codepointBytes, "END")) break;
          unicodeValue = (UChar)T_CString_stringToInteger(codepointBytes, 16);
          line = getToken(codepointBytes, line, CODEPOINT_SEPARATORS);
          if (line[nextTokenOffset(line, CODEPOINT_SEPARATORS)] != '\0') 
            {
              /*When there is a second byte*/
              myUConverterTable->mbcs.starters[T_CString_stringToInteger(codepointBytes, 16)] = TRUE;
              line = getToken(codepointBytes+2, line, CODEPOINT_SEPARATORS);
            }

          mbcsCodepageValue = T_CString_stringToInteger(codepointBytes, 16);
          
          ucmp16_set(myToUnicode, (int16_t)mbcsCodepageValue, unicodeValue);
          ucmp16_set(myFromUnicode, unicodeValue, (int16_t)mbcsCodepageValue);
        }
    }

  ucmp16_compact(myFromUnicode);
  ucmp16_compact(myToUnicode);
  myUConverterTable->mbcs.fromUnicode = myFromUnicode;
  myUConverterTable->mbcs.toUnicode = myToUnicode;
  myConverter->sharedData->referenceCounter = 1;
  myConverter->sharedData->table = myUConverterTable;

  /* if the default subCharLen is > 1 we need to insert it in the data structure
     so that we know how to transition */
  if (myConverter->subCharLen > 1)
    {
      myConverter->sharedData->table->mbcs.starters[(uint8_t)(myConverter->subChar[0])] = TRUE;
    }
  return;
}

void loadEBCDIC_STATEFULTableFromFile(FileStream* convFile, UConverter_1_4* myConverter, UErrorCode* err)
{
  char storageLine[UCNV_MAX_LINE_TEXT];
  char* line = NULL;
  UConverterTable* myUConverterTable = NULL;
  UChar unicodeValue = 0xFFFF;
  int32_t mbcsCodepageValue = '\0';
  char codepointBytes[6];
  int32_t replacementChar = 0x0000;
  uint8_t i = 0;
  CompactShortArray* myFromUnicode = NULL;
  CompactShortArray* myToUnicode = NULL;

  /*Evaluates the replacement codepoint*/
  replacementChar = 0xFFFF;

  myUConverterTable = (UConverterTable*)uprv_malloc(sizeof(UConverterMBCSTable));
  if (myUConverterTable == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
  
  
  myFromUnicode = ucmp16_open((uint16_t)replacementChar);
  myToUnicode = ucmp16_open((int16_t)0xFFFD);  

  while (T_FileStream_readLine(convFile, storageLine, UCNV_MAX_LINE_TEXT))
    {
      removeComments(storageLine);
      line = storageLine;
      if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
        {
          line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
          if (!uprv_strcmp(codepointBytes, "END")) break;
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
  myUConverterTable->dbcs.fromUnicode = myFromUnicode;
  myUConverterTable->dbcs.toUnicode = myToUnicode;
  myConverter->sharedData->referenceCounter = 1;
  myConverter->sharedData->table = myUConverterTable;

  return;
}


void loadDBCSTableFromFile(FileStream* convFile, UConverter_1_4* myConverter, UErrorCode* err)
{
  char storageLine[UCNV_MAX_LINE_TEXT];
  char* line = NULL;
  UConverterTable* myUConverterTable = NULL;
  UChar unicodeValue = 0xFFFD;
  int32_t dbcsCodepageValue = '\0';
  char codepointBytes[6];
  int32_t replacementChar = 0x0000;
  uint8_t i = 0;
  CompactShortArray* myFromUnicode = NULL;
  CompactShortArray* myToUnicode = NULL;
  
  /*Evaluates the replacement codepoint*/
  replacementChar = 0xFFFF;

  myUConverterTable = (UConverterTable*)uprv_malloc(sizeof(UConverterDBCSTable));
  if (myUConverterTable == NULL) 
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      return;
    }
  
  myFromUnicode = ucmp16_open((int16_t)replacementChar);
  myToUnicode = ucmp16_open((int16_t)0xFFFD);  
  
  while (T_FileStream_readLine(convFile, storageLine, UCNV_MAX_LINE_TEXT))
    {
      removeComments(storageLine);
      line = storageLine;
      if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
        {
          line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
          if (!uprv_strcmp(codepointBytes, "END")) break;
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
  myUConverterTable->dbcs.fromUnicode = myFromUnicode;
  myUConverterTable->dbcs.toUnicode = myToUnicode;


  myConverter->sharedData->referenceCounter = 1;
  myConverter->sharedData->table = myUConverterTable;
  
  return;
}

/*deletes the "shared" type object*/
bool_t makeconv_deleteSharedConverterData(UConverterSharedData_1_4* deadSharedData)
{
  if (deadSharedData->conversionType == UCNV_SBCS)
    {
      ucmp8_close(deadSharedData->table->sbcs.fromUnicode);
      uprv_free(deadSharedData->table);
      uprv_free(deadSharedData);
    }
  else if (deadSharedData->conversionType == UCNV_MBCS)
    {
      ucmp16_close(deadSharedData->table->mbcs.fromUnicode);
      ucmp16_close(deadSharedData->table->mbcs.toUnicode);
      uprv_free(deadSharedData->table);
      uprv_free(deadSharedData);
    }
  else if ((deadSharedData->conversionType == UCNV_DBCS) || (deadSharedData->conversionType == UCNV_EBCDIC_STATEFUL))
    {
      ucmp16_close(deadSharedData->table->dbcs.fromUnicode);
      ucmp16_close(deadSharedData->table->dbcs.toUnicode);
      uprv_free(deadSharedData->table);
      uprv_free(deadSharedData);
    }
  else
    {
      uprv_free(deadSharedData);
    }
  return TRUE;
}



/*creates a UConverter_1_4, fills in necessary links to it the appropriate function pointers*/
UConverterSharedData_1_4* createConverterFromTableFile(const char* converterName, UErrorCode* err)
{
  FileStream* convFile = NULL;
  int32_t i = 0;
  UConverterSharedData_1_4* mySharedData = NULL;
  UConverter_1_4 myConverter;


  if (U_FAILURE(*err)) return NULL;
  
  convFile = T_FileStream_open(converterName, "r");
  if (convFile == NULL) 
    {
      *err = U_FILE_ACCESS_ERROR;
      return NULL;
    }
  
  
  mySharedData = (UConverterSharedData_1_4*) uprv_malloc(sizeof(UConverterSharedData_1_4));
  if (mySharedData == NULL)
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      T_FileStream_close(convFile);
    }
  
  mySharedData->structSize = sizeof(UConverterSharedData_1_4);
  mySharedData->dataMemory = NULL; /* for init */

  myConverter.sharedData = mySharedData;
  readHeaderFromFile(&myConverter, convFile, converterName, err);

  if (U_FAILURE(*err)) return NULL;
  
  switch (mySharedData->conversionType)
    {
    case UCNV_SBCS: 
      {
        loadSBCSTableFromFile(convFile, &myConverter, err);
        break;
      }
    case UCNV_MBCS: 
      {
        loadMBCSTableFromFile(convFile, &myConverter, err);
        break;
      }
    case UCNV_EBCDIC_STATEFUL: 
      {
        loadEBCDIC_STATEFULTableFromFile(convFile, &myConverter, err);
        break;
      }
    case UCNV_DBCS: 
      {
        loadDBCSTableFromFile(convFile, &myConverter, err);
        break;
      }

    default : break;
    };

  T_FileStream_close(convFile);

  
  return mySharedData;
}



static void WriteConverterSharedData(UNewDataMemory *pData, const UConverterSharedData_1_4* data)
{
    uint32_t size = 0;
    
    udata_writeBlock(pData, data, sizeof(UConverterSharedData_1_4));

    size += sizeof(UConverterSharedData_1_4); /* Is 4-aligned- it ends with a pointer */

    switch (data->conversionType)
    {
    case UCNV_SBCS:
    {
        udata_writeBlock(pData, (void*)data->table->sbcs.toUnicode, sizeof(UChar)*256);
        size += udata_write_ucmp8(pData, data->table->sbcs.fromUnicode);
        size += sizeof(UChar)*256;
        /* don't care aboutalignment */
      }
    break;
    
    case UCNV_DBCS:
    case UCNV_EBCDIC_STATEFUL:
      {
        size += udata_write_ucmp16(pData,data->table->dbcs.toUnicode);
        if(size%4)
        {
            udata_writePadding(pData, 4-(size%4) );
            size+= 4-(size%4);
        }
        size += udata_write_ucmp16(pData,data->table->dbcs.fromUnicode);
      }
      break;

    case UCNV_MBCS:
      {
        udata_writeBlock(pData, data->table->mbcs.starters, 256*sizeof(bool_t));
        size += 256*sizeof(bool_t);
        size += udata_write_ucmp16(pData,data->table->mbcs.toUnicode);
        if(size%4)
        {
            udata_writePadding(pData, 4-(size%4) );
            size+= 4-(size%4);
        }
        size += udata_write_ucmp16(pData,data->table->mbcs.fromUnicode);
      }
      break;

    default:
      {
        /*If it isn't any of the above, the file is invalid */
        fprintf(stderr, "Error: bad converter type, can't write!!\n");
        exit(1);
        return; /* 0; */
      }
    }
  
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
