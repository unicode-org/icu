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
 *
 *  05/04/2000    helena     Added fallback mapping into the picture...
 *  06/29/2000  helena      Major rewrite of the callback APIs.
 */
   
#include <stdio.h>
#include "unicode/putil.h"
#include "ucmp16.h"
#include "ucmp8.h"
#include "ucnv_io.h"
#include "unicode/ucnv_err.h"
#include "ucnv_bld.h"
#include "ucnv_imp.h"
#include "ucnv_cnv.h"
#include "cstring.h"
#include "cmemory.h"
#include "filestrm.h"
#include "toolutil.h"
#include "uoptions.h"
#include "unicode/udata.h"
#include "unewdata.h"
#include "ucmpwrit.h"
#include "makeconv.h"
#include "genmbcs.h"

#ifdef XP_MAC_CONSOLE
# include <console.h>
#endif

#define DEBUG 0

/*
 * from ucnvstat.c - static prototypes of data-based converters
 */
extern const UConverterStaticData * ucnv_converterStaticData[UCNV_NUMBER_OF_SUPPORTED_CONVERTER_TYPES];

/*
 * Global - verbosity
 */
UBool VERBOSE = FALSE;

/*Reads the header of the table file and fills in basic knowledge about the converter
 *in "converter"
 */
static void readHeaderFromFile(UConverterSharedData* myConverter, FileStream* convFile, const char* converterName, UErrorCode* err);

/*Reads the rest of the file, and fills up the shared objects if necessary
Returns the UConverterTable. */
static void loadTableFromFile(FileStream* convFile, UConverterSharedData* sharedData, UErrorCode* err);

/* creates a UConverterSharedData from a mapping file.
 * Fills in:  *staticData, *table.  Converter is NOT otherwise useful.
 */
static UConverterSharedData* createConverterFromTableFile(const char* realName, UErrorCode* err);

/*
 * Set up the UNewData and write the converter..
 */
void writeConverterData(UConverterSharedData *mySharedData, const char *cnvName, const char *cnvDir, UErrorCode *status);

/*
 * Utility functions
 */
static UConverterPlatform getPlatformFromName(char* name);
static int32_t getCodepageNumberFromName(char* name);

static const char NLTC_SEPARATORS[9] = { '\r', '\n', '\t', ' ', '<', '>' ,'"' , 'U', '\0' };
static const char FALLBACK_SEPARATOR = '|';
static const char PLAIN_SEPARATORS[9] = { '\r', '\n', '\t', ' ', '<', '>' ,'"' ,  '\0' };
static const char STATE_SEPARATORS[9] = { '\r', '\n', '\t', '<', '>' ,'"' , '\0' }; /* do not break on space */
static const char CODEPOINT_SEPARATORS[8] = {  '\r', '>', '\\', 'x', '\n', ' ', '\t', '\0' };
static const char UNICODE_CODEPOINT_SEPARATORS[6] = {  '<', '>', 'U', ' ', '\t', '\0' };

static const char *
skipWhitespace(const char *s) {
    while(*s==' ' || *s=='\t') {
        ++s;
    }
    return s;
}

static int32_t
parseCodepageBytes(const char *s, uint32_t *pBytes, const char **pEnd) {
    char *end;
    int32_t length=0;
    uint32_t bytes=0, value;

    while(s[0]=='\\' && s[1]=='x') {
        if(length==4) {
            return -1;
        }
        value=uprv_strtoul(s+2, &end, 16);
        s+=4;
        if(end!=s) {
            return -1;
        }
        bytes=(bytes<<8)|value;
        ++length;
    }
    if(length==0) {
        return -1;
    }
    if(pEnd!=NULL) {
        *pEnd=s;
    }
    *pBytes=bytes;
    return length;
}

/* Remove all characters followed by '#'
 */
static char *
  removeComments (char *line)
{
  char *pound = uprv_strchr (line, '#');
  char *fallback = uprv_strchr(line, '|');
  if (pound != NULL)
  {
      if (fallback != NULL)
      {
          uprv_memset(pound, ' ', fallback-pound);
      }
      else 
      {
          *pound = '\0';
      }
  }
  return line;
}

/*Returns uppercased string */
static char *
  strtoupper (char *name)
{
  char *oldPtr = name;

  do {
    *name = (char)uprv_toupper(*name);
  }
  while (*(name++));

  return oldPtr;
}

/* Returns true in c is a in set 'setOfChars', false otherwise
 */
static UBool 
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
static int32_t 
  nextTokenOffset (const char *line, const char *separators)
{
  int32_t i = 0;

  while (line[i] && isInSet(line[i], separators))
    i++;

  return i;
}

/* Returns pointer to the next token based on the set of separators
 */
static char *
  getToken (char *token, char *line, const char *separators)
{
  int32_t i = nextTokenOffset (line, separators);
  int8_t j = 0;

  while (line[i] && (!isInSet(line[i], separators)))
    token[j++] = line[i++];
  token[j] = '\0';

  return line + i;
}

UBool haveCopyright=TRUE;

static UDataInfo dataInfo={
    sizeof(UDataInfo),
    0,

    U_IS_BIG_ENDIAN,
    U_CHARSET_FAMILY,
    sizeof(UChar),
    0,

    {0x63, 0x6e, 0x76, 0x74},     /* dataFormat="cnvt" */
    {6, 2, 0, 0},                 /* formatVersion */
    {0, 0, 0, 0}                  /* dataVersion (calculated at runtime) */
};

void writeConverterData(UConverterSharedData *mySharedData, 
                        const char *cnvName, 
                        const char *cnvDir, 
                        UErrorCode *status)
{
    UNewDataMemory *mem = NULL;
    uint32_t sz2;
    uint32_t size = 0;

    if(U_FAILURE(*status))
      {
        return;
      }

    mem = udata_create(cnvDir, "cnv", cnvName, &dataInfo, haveCopyright ? U_COPYRIGHT_STRING : NULL, status);

    if(U_FAILURE(*status))
      {
        fprintf(stderr, "Couldn't create the udata %s.%s: %s\n",
                cnvName,
                "cnv",
                u_errorName(*status));
        return;
      }

    if(VERBOSE)
      {
        fprintf(stderr, "- Opened udata %s.%s\n", cnvName, "cnv");
      }
  
    /* all read only, clean, platform independent data.  Mmmm. :)  */
    udata_writeBlock(mem, mySharedData->staticData, sizeof(UConverterStaticData));
    size += sizeof(UConverterStaticData); /* Is 4-aligned  - by size */
    /* Now, write the table */
    size += ((NewConverter *)mySharedData->table)->write((NewConverter *)mySharedData->table, mySharedData->staticData, mem);

    sz2 = udata_finish(mem, status);
    if(size != sz2)
    {
        fprintf(stderr, "error: wrote %ld bytes to the .cnv file but counted %ld bytes\n", sz2, size);
        *status=U_INTERNAL_PROGRAM_ERROR;
    }
    if(VERBOSE)
    {
      fprintf(stderr, "- Wrote %d bytes to the udata.\n", sz2); 
    }
}

static UOption options[]={
    UOPTION_HELP_H,              /* 0  Numbers for those who*/ 
    UOPTION_HELP_QUESTION_MARK,  /* 1   can't count. */
    UOPTION_COPYRIGHT,           /* 2 */
    UOPTION_VERSION,             /* 3 */
    UOPTION_DESTDIR,             /* 4 */
    UOPTION_VERBOSE              /* 5 */
};

int main(int argc, char* argv[])
{
    UConverterSharedData* mySharedData = NULL; 
    UErrorCode err = U_ZERO_ERROR;
    char outFileName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
    const char* destdir, *arg;
    size_t destdirlen;
    char* dot = NULL, *outBasename;
    char cnvName[UCNV_MAX_FULL_FILE_NAME_LENGTH];
    UVersionInfo icuVersion;

#ifdef XP_MAC_CONSOLE
    argc = ccommand((char***)&argv);
#endif

    /* Set up the ICU version number */
    u_getVersion(icuVersion);
    uprv_memcpy(&dataInfo.dataVersion, &icuVersion, sizeof(UVersionInfo));

    /* preset then read command line options */
    options[4].value=u_getDataDirectory();
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    } else if(argc<2) {
        argc=-1;
    }
    if(argc<0 || options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] files...\n"
            "\tread .ucm codepage mapping files and write .cnv files\n"
            "\toptions:\n"
            "\t\t-h or -? or --help  this usage text\n"
            "\t\t-V or --version     show a version message\n"
            "\t\t-c or --copyright   include a copyright notice\n"
            "\t\t-d or --destdir     destination directory, followed by the path\n"
            "\t\t-v or --verbose     Turn on verbose output\n",
            argv[0]);
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if(options[3].doesOccur) {
      fprintf(stderr,"makeconv version %hu.%hu, ICU tool to read .ucm codepage mapping files and write .cnv files\n",
            dataInfo.formatVersion[0], dataInfo.formatVersion[1]);
      fprintf(stderr, "Copyright (C) 1998-2000, International Business Machines\n");
      fprintf(stderr,"Corporation and others.  All Rights Reserved.\n");
        exit(0);
    }

    /* get the options values */
    haveCopyright = options[2].doesOccur;
    destdir = options[4].value;
    VERBOSE = options[5].doesOccur;

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
    
#if DEBUG
    {
      int i;
      printf("makeconv: processing %d files...\n", argc - 1);
      for(i=1; i<argc; ++i) {
        printf("%s ", argv[i]);
      }
      printf("\n");
      fflush(stdout);
    }
#endif

  for (++argv; --argc; ++argv)
    {
      err = U_ZERO_ERROR;
      arg = getLongPathname(*argv);

      /*produces the right destination path for display*/
      if (destdirlen != 0)
        {
          const char *basename;

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
      dot = uprv_strrchr(outBasename, '.');
      if (dot) 
        {
          *dot = '\0';
        }

      /* the basename without extension is the converter name */
      uprv_strcpy(cnvName, outBasename);

      /*Adds the target extension*/
      uprv_strcat(outBasename, CONVERTER_FILE_EXTENSION);

#if DEBUG
        printf("makeconv: processing %s  ...\n", arg);
        fflush(stdout);
#endif
      mySharedData = createConverterFromTableFile(arg, &err);

      if (U_FAILURE(err) || (mySharedData == NULL))
        {
          /* if an error is found, print out an error msg and keep going */
          fprintf(stderr, "Error creating \"%s\" file for \"%s\" (error code %d - %s)\n", outFileName, arg, err,
                        u_errorName(err));
          err = U_ZERO_ERROR;
        }
      else
        {
          /* Make the static data name equal to the file name */
          if( /*VERBOSE &&  */ uprv_stricmp(cnvName,mySharedData->staticData->name))
          {
            fprintf(stderr, "Warning: %s%s claims to be '%s'\n", 
                    cnvName,
                    CONVERTER_FILE_EXTENSION,
                    mySharedData->staticData->name);
          }

          uprv_strcpy((char*)mySharedData->staticData->name, cnvName);

          writeConverterData(mySharedData, cnvName, destdir, &err);
          ((NewConverter *)mySharedData->table)->close((NewConverter *)mySharedData->table);
          uprv_free((UConverterStaticData *)mySharedData->staticData);
          uprv_free(mySharedData);

          if(U_FAILURE(err))
          {
                  /* if an error is found, print out an error msg and keep going*/
            fprintf(stderr, "Error writing \"%s\" file for \"%s\" (error code %d - %s)\n", outFileName, arg, err,
                    u_errorName(err));
          }
          else
          {
              puts(outFileName);
          }
        }
      fflush(stdout);
      fflush(stderr);
    }

  return err;
}

UConverterPlatform getPlatformFromName(char* name)
{
  char myPlatform[10];
  char mySeparators[2] = { '-', '\0' };
  
  getToken(myPlatform, name, mySeparators);
  strtoupper(myPlatform);

  if (uprv_strcmp(myPlatform, "IBM") == 0)
    return UCNV_IBM;
  else
    return UCNV_UNKNOWN;
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
void readHeaderFromFile(UConverterSharedData* mySharedData,
                        FileStream* convFile,
                        const char* converterName,
                        UErrorCode *pErrorCode)
{
    char line[200];
    char *s, *end, *key, *value;
    UConverterStaticData *staticData;
    char c;

    if(U_FAILURE(*pErrorCode)) {
        return;
    }

    staticData=(UConverterStaticData *)mySharedData->staticData;
    staticData->conversionType=UCNV_UNSUPPORTED_CONVERTER;
    uprv_strcpy((char*)staticData->name, converterName);
    staticData->platform=UCNV_IBM;
    staticData->subCharLen=0;

    while(T_FileStream_readLine(convFile, line, sizeof(line))) {
        /* remove comments and trailing CR and LF and remove whitespace from the end */
        for(end=line; (c=*end)!=0; ++end) {
            if(c=='#' || c=='\r' || c=='\n') {
                break;
            }
        }
        while(end>line && (*(end-1)==' ' || *(end-1)=='\t')) {
            --end;
        }
        *end=0;

        /* skip leading white space and ignore empty lines */
        s=(char *)skipWhitespace(line);
        if(*s==0) {
            continue;
        }

        /* stop at the beginning of the mapping section */
        if(uprv_memcmp(s, "CHARMAP", 7)==0) {
            break;
        }

        /* get the key name, bracketed in <> */
        if(*s!='<') {
            fprintf(stderr, "error: no header field <key> in line \"%s\"\n", line);
            *pErrorCode=U_INVALID_TABLE_FORMAT;
            return;
        }
        key=++s;
        while(*s!='>') {
            if(*s==0) {
                fprintf(stderr, "error: incomplete header field <key> in line \"%s\"\n", line);
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
            ++s;
        }
        *s=0;

        /* get the value string, possibly quoted */
        s=(char *)skipWhitespace(s+1);
        if(*s!='"') {
            value=s;
        } else {
            /* remove the quotes */
            value=s+1;
            if(end>value && *(end-1)=='"') {
                *--end=0;
            }
        }

        /* collect the information from the header field, ignore unknown keys */
        if(uprv_strcmp(key, "code_set_name")==0) {
            if(*value!=0) {
                uprv_strcpy((char*)staticData->name, value);
                staticData->platform=(int8_t)getPlatformFromName(value);
                staticData->codepage=getCodepageNumberFromName(value);
            }
        } else if(uprv_strcmp(key, "uconv_class")==0) {
            const UConverterStaticData *prototype;

            if(uprv_strcmp(value, "DBCS")==0) {
                staticData->conversionType=UCNV_DBCS;
            } else if(uprv_strcmp(value, "SBCS")==0) {
                staticData->conversionType = UCNV_SBCS;
            } else if(uprv_strcmp(value, "MBCS")==0) {
                staticData->conversionType = UCNV_MBCS;
            } else if(uprv_strcmp(value, "EBCDIC_STATEFUL")==0) {
                staticData->conversionType = UCNV_EBCDIC_STATEFUL;
            } else {
                fprintf(stderr, "error: unknown <uconv_class> %s\n", value);
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }

            /* Now that we know the type, copy any 'default' values from the table. */
            prototype=ucnv_converterStaticData[staticData->conversionType];
            if(prototype!=NULL) {
                if(staticData->name[0]==0) {
                    uprv_strcpy((char*)staticData->name, prototype->name);
                }

                if(staticData->codepage==0) {
                    staticData->codepage = prototype->codepage;
                }

                if(staticData->platform==0) {
                    staticData->platform = prototype->platform;
                }

                if(staticData->minBytesPerChar==0) {
                    staticData->minBytesPerChar = prototype->minBytesPerChar;
                }

                if(staticData->maxBytesPerChar==0) {
                    staticData->maxBytesPerChar = prototype->maxBytesPerChar;
                }

                if(staticData->subCharLen==0) {
                    staticData->subCharLen=prototype->subCharLen;
                    if(prototype->subCharLen>0) {
                        uprv_memcpy(staticData->subChar, prototype->subChar, prototype->subCharLen);
                    }
                }
            }
        } else if(uprv_strcmp(key, "mb_cur_max")==0) {
            if('1'<=*value && *value<='4' && value[1]==0) {
                staticData->maxBytesPerChar=(int8_t)(*value-'0');
            } else {
                fprintf(stderr, "error: illegal <mb_cur_max> %s\n", value);
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
        } else if(uprv_strcmp(key, "mb_cur_min")==0) {
            if('1'<=*value && *value<='4' && value[1]==0) {
                staticData->minBytesPerChar=(int8_t)(*value-'0');
            } else {
                fprintf(stderr, "error: illegal <mb_cur_min> %s\n", value);
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
        } else if(uprv_strcmp(key, "subchar")==0) {
            uint32_t bytes;
            int32_t length;

            length=parseCodepageBytes(value, &bytes, (const char **)&end);
            if(length>0 && *end==0) {
                staticData->subCharLen=(int8_t)length;
                do {
                    staticData->subChar[--length]=(uint8_t)bytes;
                    bytes>>=8;
                } while(length>0);
            } else {
                fprintf(stderr, "error: illegal <subchar> %s\n", value);
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
        } else if(uprv_strcmp(key, "subchar1")==0) {
            uint32_t bytes;

            if(1==parseCodepageBytes(value, &bytes, (const char **)&end) && *end==0) {
                staticData->subChar1=(uint8_t)bytes;
            } else {
                fprintf(stderr, "error: illegal <subchar1> %s\n", value);
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
        } else if(uprv_strcmp(key, "icu:state")==0) {
            /* if an SBCS/DBCS/EBCDIC_STATEFUL converter has icu:state, then turn it into MBCS */
            switch(staticData->conversionType) {
            case UCNV_SBCS:
            case UCNV_DBCS:
            case UCNV_EBCDIC_STATEFUL:
                staticData->conversionType = UCNV_MBCS;
                break;
            case UCNV_MBCS:
                break;
            default:
                fprintf(stderr, "error: <icu:state> entry for non-MBCS table or before the <uconv_class> line\n");
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }

            if(staticData->maxBytesPerChar==0) {
                fprintf(stderr, "error: <icu:state> before the <mb_cur_max> line\n");
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
            if(mySharedData->table==NULL) {
                mySharedData->table=(UConverterTable *)MBCSOpen(staticData->maxBytesPerChar);
                if(mySharedData->table==NULL) {
                    *pErrorCode=U_MEMORY_ALLOCATION_ERROR;
                    return;
                }
            }
            if(!MBCSAddState((NewConverter *)mySharedData->table, value)) {
                *pErrorCode=U_INVALID_TABLE_FORMAT;
                return;
            }
        }
    }

    if(staticData->conversionType==UCNV_UNSUPPORTED_CONVERTER) {
        *pErrorCode=U_INVALID_TABLE_FORMAT;
    } else if(staticData->conversionType==UCNV_MBCS && mySharedData->table==NULL) {
        fprintf(stderr, "error: missing state table information (<icu:state>) for MBCS\n");
        *pErrorCode=U_INVALID_TABLE_FORMAT;
    }
}
  
void loadTableFromFile(FileStream* convFile, UConverterSharedData* sharedData, UErrorCode* err)
{
    char storageLine[200];
    char* line = NULL;
    UConverterStaticData *staticData=(UConverterStaticData *)sharedData->staticData;
    NewConverter *cnvData = (NewConverter *)sharedData->table;
    UChar32 unicodeValue, codepageValue;
    uint8_t mbcsBytes[8];
    int32_t mbcsLength;
    char codepointBytes[20];
    UBool isOK = TRUE;
    uint8_t precisionMask = 0, unicodeMask = 0;
    char endOfLine;

    if(cnvData->startMappings!=NULL)
    {
        if(!cnvData->startMappings(cnvData)) {
            *err = U_INVALID_TABLE_FORMAT;
            return;
        }
    }

    if(cnvData->isValid!=NULL)
    {
        const uint8_t *p = staticData->subChar;
        codepageValue = 0;
        switch(staticData->subCharLen) {
        case 4:     codepageValue = (codepageValue << 8) | *p++;
        case 3:     codepageValue = (codepageValue << 8) | *p++;
        case 2:     codepageValue = (codepageValue << 8) | *p++;
        case 1:     codepageValue = (codepageValue << 8) | *p;
        default:    break; /* must never occur */
        }
        if(!cnvData->isValid(cnvData, staticData->subChar, staticData->subCharLen, codepageValue)) {
            fprintf(stderr, "       the substitution character byte sequence is illegal in this codepage structure!\n");
            *err = U_INVALID_TABLE_FORMAT;
            isOK = FALSE;
        }
    }

    staticData->hasFromUnicodeFallback = staticData->hasToUnicodeFallback = FALSE;

    while (T_FileStream_readLine(convFile, storageLine, sizeof(storageLine)))
    {
        removeComments(storageLine);
        line = storageLine;
        if (line[nextTokenOffset(line, NLTC_SEPARATORS)] != '\0')
        {
            /* get the Unicode code point */
            line = getToken(codepointBytes, line, UNICODE_CODEPOINT_SEPARATORS);
            if (uprv_strcmp(codepointBytes, "END") == 0)
            {
                break;
            }
            unicodeValue = (UChar32)T_CString_stringToInteger(codepointBytes, 16);

            /* get the codepage bytes */
            codepageValue = 0;
            mbcsLength = 0;
            do
            {
                line = getToken(codepointBytes, line, CODEPOINT_SEPARATORS);
                mbcsBytes[mbcsLength] = (uint8_t)T_CString_stringToInteger(codepointBytes, 16);
                codepageValue = codepageValue << 8 | mbcsBytes[mbcsLength++];

                /* End of line could be \0 or | (if fallback) */
                endOfLine= line[nextTokenOffset(line, CODEPOINT_SEPARATORS)];
            } while((endOfLine != '\0') && (endOfLine != FALLBACK_SEPARATOR));

            if(unicodeValue>=0x10000) {
                unicodeMask|=UCNV_HAS_SUPPLEMENTARY;    /* there are supplementary code points */
            } else if(UTF_IS_SURROGATE(unicodeValue)) {
                unicodeMask|=UCNV_HAS_SURROGATES;       /* there are single surrogates */
            }

            if((uint32_t)unicodeValue > 0x10ffff)
            {
                fprintf(stderr, "error: Unicode code point > U+10ffff in '%s'\n", storageLine);
                isOK = FALSE;
            }
            else if(endOfLine == FALLBACK_SEPARATOR)
            {
                /* we know that there is a fallback separator */
                precisionMask |= 1;
                line = uprv_strchr(line, FALLBACK_SEPARATOR) + 1;
                switch(*line)
                {
                case '0':
                    /* set roundtrip mappings */
                    isOK &= cnvData->addToUnicode(cnvData, mbcsBytes, mbcsLength, unicodeValue, codepageValue, 0) &&
                            cnvData->addFromUnicode(cnvData, mbcsBytes, mbcsLength, unicodeValue, codepageValue, 0);
                    break;
                case '1':
                    /* set only a fallback mapping from Unicode to codepage */
                    staticData->hasFromUnicodeFallback = TRUE;
                    isOK &= cnvData->addFromUnicode(cnvData, mbcsBytes, mbcsLength, unicodeValue, codepageValue, 1);
                    break;
                case '2':
                    /* skip subchar mappings */
                    break;
                case '3':
                    /* set only a fallback mapping from codepage to Unicode */
                    staticData->hasToUnicodeFallback = TRUE;
                    isOK &= cnvData->addToUnicode(cnvData, mbcsBytes, mbcsLength, unicodeValue, codepageValue, 1);
                    break;
                default:
                    fprintf(stderr, "error: illegal fallback indicator '%s' in '%s'\n", line - 1, storageLine);
                    *err = U_INVALID_TABLE_FORMAT;
                    break;
                }
            }
            else
            {
                precisionMask |= 2;
                /* set the mappings */
                isOK &= cnvData->addToUnicode(cnvData, mbcsBytes, mbcsLength, unicodeValue, codepageValue, -1) &&
                        cnvData->addFromUnicode(cnvData, mbcsBytes, mbcsLength, unicodeValue, codepageValue, -1);
            }
        }
    }

    if(unicodeMask == 3)
    {
        fprintf(stderr, "warning: contains mappings to both supplementary code points and single surrogates\n");
    }
    staticData->unicodeMask = unicodeMask;

    if(cnvData->finishMappings!=NULL)
    {
        cnvData->finishMappings(cnvData, staticData);
    }

    if(!isOK)
    {
        *err = U_INVALID_TABLE_FORMAT;
    }
    else if(precisionMask == 3)
    {
        fprintf(stderr, "error: some entries have the mapping precision (with '|'), some do not\n");
        *err = U_INVALID_TABLE_FORMAT;
    }
}

/*creates a UConverterStaticData, fills in necessary links to it the appropriate function pointers*/
UConverterSharedData* createConverterFromTableFile(const char* converterName, UErrorCode* err)
{
  FileStream* convFile = NULL;
  UConverterSharedData* mySharedData = NULL;
  UConverterStaticData* myStaticData = NULL;

  if (U_FAILURE(*err)) return NULL;
  
  convFile = T_FileStream_open(converterName, "r");
  if (convFile == NULL) 
    {
      *err = U_FILE_ACCESS_ERROR;
      return NULL;
    }
  
  
  mySharedData = (UConverterSharedData*) uprv_malloc(sizeof(UConverterSharedData));
  if (mySharedData == NULL)
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      T_FileStream_close(convFile);
      return NULL;
    }
  
  uprv_memset(mySharedData, 0, sizeof(UConverterSharedData));
  
  mySharedData->structSize = sizeof(UConverterSharedData);

  myStaticData =  (UConverterStaticData*) uprv_malloc(sizeof(UConverterStaticData));
  if (myStaticData == NULL)
    {
      *err = U_MEMORY_ALLOCATION_ERROR;
      T_FileStream_close(convFile);
      return NULL;
    }  
  uprv_memset(myStaticData, 0, sizeof(UConverterStaticData));
  mySharedData->staticData = myStaticData;
  myStaticData->structSize = sizeof(UConverterStaticData);
  mySharedData->staticDataOwned = TRUE;

  uprv_strcpy(myStaticData->name, converterName);

  mySharedData->dataMemory = NULL; /* for init */

  readHeaderFromFile(mySharedData, convFile, converterName, err);

  if (U_FAILURE(*err)) return NULL;
  
  switch (myStaticData->conversionType)
    {
    case UCNV_SBCS: 
      {
        /* SBCS: use MBCS data structure with a default state table */
        if(mySharedData->staticData->maxBytesPerChar!=1) {
            fprintf(stderr, "error: SBCS codepage with max bytes/char!=1\n");
            *err = U_INVALID_TABLE_FORMAT;
            break;
        }
        myStaticData->conversionType = UCNV_MBCS;
        if(mySharedData->table == NULL) {
            mySharedData->table = (UConverterTable *)MBCSOpen(1);
            if(mySharedData->table != NULL) {
                if(!MBCSAddState((NewConverter *)mySharedData->table, "0-ff")) {
                    *err = U_INVALID_TABLE_FORMAT;
                    ((NewConverter *)mySharedData->table)->close((NewConverter *)mySharedData->table);
                    mySharedData->table=NULL;
                }
            } else {
                *err = U_MEMORY_ALLOCATION_ERROR;
            }
        }
        break;
      }
    case UCNV_MBCS: 
      {
        /* MBCSOpen() was called by readHeaderFromFile() */
        break;
      }
    case UCNV_EBCDIC_STATEFUL: 
      {
        /* EBCDIC_STATEFUL: use MBCS data structure with a default state table */
        if(mySharedData->staticData->maxBytesPerChar!=2) {
            fprintf(stderr, "error: DBCS codepage with max bytes/char!=2\n");
            *err = U_INVALID_TABLE_FORMAT;
            break;
        }
        myStaticData->conversionType = UCNV_MBCS;
        if(mySharedData->table == NULL) {
            mySharedData->table = (UConverterTable *)MBCSOpen(2);
            if(mySharedData->table != NULL) {
                if( !MBCSAddState((NewConverter *)mySharedData->table, "0-ff, e:1.s, f:0.s") ||
                    !MBCSAddState((NewConverter *)mySharedData->table, "initial, 0-3f:4, e:1.s, f:0.s, 40:3, 41-fe:2, ff:4") ||
                    !MBCSAddState((NewConverter *)mySharedData->table, "0-40:1.i, 41-fe:1., ff:1.i") ||
                    !MBCSAddState((NewConverter *)mySharedData->table, "0-ff:1.i, 40:1.") ||
                    !MBCSAddState((NewConverter *)mySharedData->table, "0-ff:1.i")
                ) {
                    *err = U_INVALID_TABLE_FORMAT;
                    ((NewConverter *)mySharedData->table)->close((NewConverter *)mySharedData->table);
                    mySharedData->table=NULL;
                }
            } else {
                *err = U_MEMORY_ALLOCATION_ERROR;
            }
        }
        break;
      }
    case UCNV_DBCS: 
      {
        /* DBCS: use MBCS data structure with a default state table */
        if(mySharedData->staticData->maxBytesPerChar!=2) {
            fprintf(stderr, "error: DBCS codepage with max bytes/char!=2\n");
            *err = U_INVALID_TABLE_FORMAT;
            break;
        }
        myStaticData->conversionType = UCNV_MBCS;
        if(mySharedData->table == NULL) {
            mySharedData->table = (UConverterTable *)MBCSOpen(2);
            if(mySharedData->table != NULL) {
                if( !MBCSAddState((NewConverter *)mySharedData->table, "0-3f:3, 40:2, 41-fe:1, ff:3") ||
                    !MBCSAddState((NewConverter *)mySharedData->table, "41-fe") ||
                    !MBCSAddState((NewConverter *)mySharedData->table, "40") ||
                    !MBCSAddState((NewConverter *)mySharedData->table, "")
                ) {
                    *err = U_INVALID_TABLE_FORMAT;
                    ((NewConverter *)mySharedData->table)->close((NewConverter *)mySharedData->table);
                    mySharedData->table=NULL;
                }
            } else {
                *err = U_MEMORY_ALLOCATION_ERROR;
            }
        }
        break;
      }

    default :
      fprintf(stderr, "error: <uconv_class> omitted\n");
      *err = U_INVALID_TABLE_FORMAT;
      mySharedData->table = NULL;
      break;
    };

    if(U_SUCCESS(*err) && mySharedData->table != NULL)
    {
        loadTableFromFile(convFile, mySharedData, err);
    }

  T_FileStream_close(convFile);
  
  return mySharedData;
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
