/*
*******************************************************************************
*
*   Copyright (C) 2000-2001, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  genuca.cpp
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created at the end of XX century
*   created by: Vladimir Weinstein
*
*   This program reads the Franctional UCA table and generates
*   internal format for UCA table as well as inverse UCA table.
*   It then writes binary files containing the data: ucadata.dat 
*   & invuca.dat
*   Change history:
*   02/23/2001  grhoten                 Made it into a tool
*   02/23/2001  weiv                    Moved element & table handling code to i18n
*   05/09/2001  weiv                    Case bits are now in the CEs, not in front
*/

#include "genuca.h"
#include "uoptions.h"
#include "toolutil.h"
#include "cstring.h"

#include <stdlib.h>

#ifdef XP_MAC_CONSOLE
#include <console.h>
#endif

UCAElements le;

/*
 * Global - verbosity
 */
UBool VERBOSE = FALSE;

int32_t readElement(char **from, char *to, char separator, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return 0;
    }
    char buffer[1024];
    int32_t i = 0;
    while(**from != separator) {
        if(**from != ' ') {
            *(buffer+i++) = **from;
        }
        (*from)++;
    }
    (*from)++;
    *(buffer + i) = 0;
    //*to = (char *)malloc(strlen(buffer)+1);
    strcpy(to, buffer);
    return i/2;
}


uint32_t getSingleCEValue(char *primary, char *secondary, char *tertiary, UErrorCode *status) {
    if(U_FAILURE(*status)) {
        return 0;
    }
    uint32_t value = 0;
    char primsave = '\0';
    char secsave = '\0';
    char tersave = '\0';
    char *primend = primary+4;
    if(strlen(primary) > 4) {
        primsave = *primend;
        *primend = '\0';
    }
    char *secend = secondary+2;
    if(strlen(secondary) > 2) {
        secsave = *secend;
        *secend = '\0';
    }
    char *terend = tertiary+2;
    if(strlen(tertiary) > 2) {
        tersave = *terend;
        *terend = '\0';
    }
    uint32_t primvalue = (uint32_t)((*primary!='\0')?strtoul(primary, &primend, 16):0);
    uint32_t secvalue = (uint32_t)((*secondary!='\0')?strtoul(secondary, &secend, 16):0);
    uint32_t tervalue = (uint32_t)((*tertiary!='\0')?strtoul(tertiary, &terend, 16):0);
    if(primvalue <= 0xFF) {
      primvalue <<= 8;
    }

    value = ((primvalue<<UCOL_PRIMARYORDERSHIFT)&UCOL_PRIMARYORDERMASK)|
        ((secvalue<<UCOL_SECONDARYORDERSHIFT)&UCOL_SECONDARYORDERMASK)|
        (tervalue&UCOL_TERTIARYORDERMASK);

    if(primsave!='\0') {
        *primend = primsave;
    }
    if(secsave!='\0') {
        *secend = secsave;
    }
    if(tersave!='\0') {
        *terend = tersave;
    }
    return value;
}

static uint32_t inverseTable[0xFFFF][3];
static uint32_t inversePos = 0;
static UChar stringContinue[0xFFFF];
static uint32_t sContPos = 0;

static void addNewInverse(UCAElements *element, UErrorCode *status) {
  if(U_FAILURE(*status)) {
    return;
  }
  if(VERBOSE && isContinuation(element->CEs[1])) {
    fprintf(stdout, "+");
  }
  inversePos++;
  inverseTable[inversePos][0] = element->CEs[0];
  if(element->noOfCEs > 1 && isContinuation(element->CEs[1])) {
    inverseTable[inversePos][1] = element->CEs[1];
  } else {
    inverseTable[inversePos][1] = 0;
  }
  if(element->cSize < 2) {
    inverseTable[inversePos][2] = element->cPoints[0];
  } else { /* add a new store of cruft */
    inverseTable[inversePos][2] = ((element->cSize+1) << UCOL_INV_SHIFTVALUE) | sContPos;
    memcpy(stringContinue+sContPos, element->cPoints, element->cSize*sizeof(UChar));
    sContPos += element->cSize+1;
  }
}

static void insertInverse(UCAElements *element, uint32_t position, UErrorCode *status) {
  uint8_t space[4096];

  if(U_FAILURE(*status)) {
    return;
  }

  if(VERBOSE && isContinuation(element->CEs[1])) {
    fprintf(stdout, "+");
  }
  if(position <= inversePos) {
    /*move stuff around */
    uprv_memcpy(space, inverseTable[position], (inversePos - position+1)*sizeof(inverseTable[0]));
    uprv_memcpy(inverseTable[position+1], space, (inversePos - position+1)*sizeof(inverseTable[0]));
  }
  inverseTable[position][0] = element->CEs[0];
  if(element->noOfCEs > 1 && isContinuation(element->CEs[1])) {
    inverseTable[position][1] = element->CEs[1];
  } else {
    inverseTable[position][1] = 0;
  }
  if(element->cSize < 2) {
    inverseTable[position][2] = element->cPoints[0];
  } else { /* add a new store of cruft */
    inverseTable[position][2] = ((element->cSize+1) << UCOL_INV_SHIFTVALUE) | sContPos;
    memcpy(stringContinue+sContPos, element->cPoints, element->cSize*sizeof(UChar));
    sContPos += element->cSize+1;
  }
  inversePos++;
}

static void addToExistingInverse(UCAElements *element, uint32_t position, UErrorCode *status) {

  if(U_FAILURE(*status)) {
    return;
  }

      if((inverseTable[position][2] & UCOL_INV_SIZEMASK) == 0) { /* single element, have to make new extension place and put both guys there */
        stringContinue[sContPos] = (UChar)inverseTable[position][2];
        inverseTable[position][2] = ((element->cSize+3) << UCOL_INV_SHIFTVALUE) | sContPos;
        sContPos++;
        stringContinue[sContPos++] = 0xFFFF;
        memcpy(stringContinue+sContPos, element->cPoints, element->cSize*sizeof(UChar));
        sContPos += element->cSize;
        stringContinue[sContPos++] = 0xFFFE;
      } else { /* adding to the already existing continuing table */
        uint32_t contIndex = inverseTable[position][2] & UCOL_INV_OFFSETMASK;
        uint32_t contSize = (inverseTable[position][2] & UCOL_INV_SIZEMASK) >> UCOL_INV_SHIFTVALUE;

        if(contIndex+contSize < sContPos) {
          /*fprintf(stderr, ".", sContPos, contIndex+contSize);*/
          memcpy(stringContinue+contIndex+contSize+element->cSize+1, stringContinue+contIndex+contSize, (element->cSize+1)*sizeof(UChar));
        }

        stringContinue[contIndex+contSize-1] = 0xFFFF;
        memcpy(stringContinue+contIndex+contSize, element->cPoints, element->cSize*sizeof(UChar));
        sContPos += element->cSize+1;
        stringContinue[contIndex+contSize+element->cSize] = 0xFFFE;

        inverseTable[position][2] = ((contSize+element->cSize+1) << UCOL_INV_SHIFTVALUE) | contIndex;
      }
}

static uint32_t addToInverse(UCAElements *element, UErrorCode *status) {
  uint32_t comp = 0;
  uint32_t position = inversePos;
  uint32_t saveElement = element->CEs[0];
  element->CEs[0] &= 0xFFFFFF3F;
  if(element->noOfCEs == 1) {
    element->CEs[1] = 0;
  }
  if(inversePos == 0) {
    inverseTable[0][0] = inverseTable[0][1] = inverseTable[0][2] = 0;
    addNewInverse(element, status);
  } else if(inverseTable[inversePos][0] > element->CEs[0]) {
    while(inverseTable[--position][0] > element->CEs[0]) {}
    if(inverseTable[position][0] == element->CEs[0]) {
      if(isContinuation(element->CEs[1])) {
        comp = element->CEs[1];
      } else {
        comp = 0;
      }
      if(inverseTable[position][1] > comp) {
        while(inverseTable[--position][1] > comp) {}
      }
      if(inverseTable[position][1] == comp) {
        addToExistingInverse(element, position, status);
      } else {
        insertInverse(element, position+1, status);
      }
    } else {
      insertInverse(element, position+1, status);
    }
  } else if(inverseTable[inversePos][0] == element->CEs[0]) {
    if(element->noOfCEs > 1 && isContinuation(element->CEs[1])) {
      comp = element->CEs[1];
      if(inverseTable[position][1] > comp) {
        while(inverseTable[--position][1] > comp) {}
      }
      if(inverseTable[position][1] == comp) {
        addToExistingInverse(element, position, status);
      } else {
        insertInverse(element, position+1, status);
      }
    } else {
      addToExistingInverse(element, inversePos, status);
    } 
  } else {
    addNewInverse(element, status);
  }
  element->CEs[0] = saveElement;
  return inversePos;
}

static InverseTableHeader *assembleInverseTable(UErrorCode *status)
{
  InverseTableHeader *result = NULL;
  uint32_t headerByteSize = paddedsize(sizeof(InverseTableHeader));
  uint32_t inverseTableByteSize = (inversePos+2)*sizeof(uint32_t)*3;
  uint32_t contsByteSize = sContPos * sizeof(UChar);
  uint32_t i = 0;

  result = (InverseTableHeader *)malloc(headerByteSize + inverseTableByteSize + contsByteSize);
  if(result != NULL) {
    result->byteSize = headerByteSize + inverseTableByteSize + contsByteSize;

    inversePos++;
    inverseTable[inversePos][0] = 0xFFFFFFFF;
    inverseTable[inversePos][1] = 0xFFFFFFFF;
    inverseTable[inversePos][2] = 0x0000FFFF;
    inversePos++;

    for(i = 2; i<inversePos; i++) {
      if(inverseTable[i-1][0] > inverseTable[i][0]) {
        fprintf(stderr, "Error at %i: %08X & %08X\n", i, inverseTable[i-1][0], inverseTable[i][0]);
      } else if(inverseTable[i-1][0] == inverseTable[i][0] && !(inverseTable[i-1][1] < inverseTable[i][1])) {
        fprintf(stderr, "Continuation error at %i: %08X %08X & %08X %08X\n", i, inverseTable[i-1][0], inverseTable[i-1][1], inverseTable[i][0], inverseTable[i][1]);
      }
    }

    result->tableSize = inversePos;
    result->contsSize = sContPos;

    result->table = headerByteSize;
    result->conts = headerByteSize + inverseTableByteSize;

    memcpy((uint8_t *)result + result->table, inverseTable, inverseTableByteSize);
    memcpy((uint8_t *)result + result->conts, stringContinue, contsByteSize);

  } else {
    *status = U_MEMORY_ALLOCATION_ERROR;
    return NULL;
  }

  return result; 
}


static void writeOutInverseData(InverseTableHeader *data,
                  const char *outputDir,
                  const char *copyright,
                  UErrorCode *status)
{
    UNewDataMemory *pData;
    
    long dataLength;

    pData=udata_create(outputDir, INVC_DATA_TYPE, INVC_DATA_NAME, &invDataInfo,
                       copyright, status);

    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error: unable to create data memory, error %d\n", *status);
        return;
    }

    /* write the data to the file */
    fprintf(stdout, "Writing out inverse UCA table: %s%s.%s\n", outputDir,
                                                                INVC_DATA_NAME,
                                                                INVC_DATA_TYPE);
    udata_writeBlock(pData, data, data->byteSize);

    /* finish up */
    dataLength=udata_finish(pData, status);
    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error: error %d writing the output file\n", *status);
        return;
    }
}



static int32_t hex2num(char hex) {
    if(hex>='0' && hex <='9') {
        return hex-'0';
    } else if(hex>='a' && hex<='f') {
        return hex-'a'+10;
    } else if(hex>='A' && hex<='F') {
        return hex-'A'+10;
    } else {
        return 0;
    }
}

UCAElements *readAnElement(FILE *data, UErrorCode *status) {
    char buffer[2048], primary[100], secondary[100], tertiary[100];
    UBool detectedContraction;
    int32_t i = 0;
    unsigned int theValue;
    char *pointer = NULL;
    char *commentStart = NULL;
    char *startCodePoint = NULL;
    char *endCodePoint = NULL;
    char *spacePointer = NULL;
    char *result = fgets(buffer, 2048, data);
    if(U_FAILURE(*status)) {
        return 0;
    }
    *primary = *secondary = *tertiary = '\0';
    if(result == NULL) {
        if(feof(data)) {
            return NULL;
        } else {
            fprintf(stderr, "empty line but no EOF!\n");
            *status = U_INVALID_FORMAT_ERROR;
            return NULL;
        }
    }
    if(buffer[0] == '#' || buffer[0] == '\n') {
        return NULL; // just a comment, skip whole line
    }

    UCAElements *element = &le; //(UCAElements *)malloc(sizeof(UCAElements));

    if(buffer[0] == '[') {
      const char *vt = "[variable top = ";
      uint32_t vtLen = uprv_strlen(vt);
      if(uprv_strncmp(buffer, vt, vtLen) == 0) {
        element->variableTop = TRUE;
        if(sscanf(buffer+vtLen, "%04X", &theValue) != 1) /* read first code point */
        {
          fprintf(stderr, " scanf(hex) failed!\n ");
        }
        element->cPoints[0] = (UChar)theValue;
        return element; // just a comment, skip whole line
      } else {
        *status = U_INVALID_FORMAT_ERROR;
        return NULL;
      }
    }
    element->variableTop = FALSE;

    startCodePoint = buffer;
    endCodePoint = strchr(startCodePoint, ';');

    if(endCodePoint == 0) {
        fprintf(stderr, "error - line with no code point!\n");
        *status = U_INVALID_FORMAT_ERROR; /* No code point - could be an error, but probably only an empty line */
        return NULL;
    } else {
        *(endCodePoint) = 0;
    }

    if(element != NULL) {
        memset(element, 0, sizeof(*element));
    } else {
        *status = U_MEMORY_ALLOCATION_ERROR;
        return NULL;
    }

    element->cPoints = element->uchars;

    spacePointer = strchr(buffer, ' ');
    if(sscanf(buffer, "%04X", &theValue) != 1) /* read first code point */
    {
      fprintf(stderr, " scanf(hex) failed!\n ");
    }
    element->cPoints[0] = (UChar)theValue;

    if(spacePointer == 0) {
        detectedContraction = FALSE;
        element->cSize = 1;
    } else {
        i = 1;
        detectedContraction = TRUE;
        while(spacePointer != NULL) {
            sscanf(spacePointer+1, "%04X", &theValue);
            element->cPoints[i++] = (UChar)theValue;
            spacePointer = strchr(spacePointer+1, ' ');
        }

        element->cSize = i;

        //fprintf(stderr, "Number of codepoints in contraction: %i\n", i);
    }

    startCodePoint = endCodePoint+1;

    commentStart = strchr(startCodePoint, '#');
    if(commentStart == NULL) {
        commentStart = strlen(startCodePoint) + startCodePoint - 1;
    }

    i = 0;
    uint32_t CEindex = 0;
    element->noOfCEs = 0;
    for(;;) {
        endCodePoint = strchr(startCodePoint, ']');
        if(endCodePoint == NULL || endCodePoint >= commentStart) {
            break;
        }
        pointer = strchr(startCodePoint, '[');
        pointer++;

        element->sizePrim[i]=readElement(&pointer, primary, ',', status);
        element->sizeSec[i]=readElement(&pointer, secondary, ',', status);
        element->sizeTer[i]=readElement(&pointer, tertiary, ']', status);


        /* I want to get the CEs entered right here, including continuation */
        element->CEs[CEindex++] = getSingleCEValue(primary, secondary, tertiary, status);

        uint32_t CEi = 1;
        while(2*CEi<element->sizePrim[i] || CEi<element->sizeSec[i] || CEi<element->sizeTer[i]) {
          uint32_t value = UCOL_CONTINUATION_MARKER; /* Continuation marker */
            if(2*CEi<element->sizePrim[i]) {
                value |= ((hex2num(*(primary+4*CEi))&0xF)<<28);
                value |= ((hex2num(*(primary+4*CEi+1))&0xF)<<24);
            }

            if(2*CEi+1<element->sizePrim[i]) {
                value |= ((hex2num(*(primary+4*CEi+2))&0xF)<<20);
                value |= ((hex2num(*(primary+4*CEi+3))&0xF)<<16);
            }

            if(CEi<element->sizeSec[i]) {
                value |= ((hex2num(*(secondary+2*CEi))&0xF)<<12);
                value |= ((hex2num(*(secondary+2*CEi+1))&0xF)<<8);
            }

            if(CEi<element->sizeTer[i]) {
                value |= ((hex2num(*(tertiary+2*CEi))&0x3)<<4);
                value |= (hex2num(*(tertiary+2*CEi+1))&0xF);
            }

            CEi++;

            element->CEs[CEindex++] = value;
        }

      startCodePoint = endCodePoint+1;
      i++;
    }
    element->noOfCEs = CEindex;

    element->isThai = UCOL_ISTHAIPREVOWEL(element->cPoints[0]);

    // we don't want any strange stuff after useful data!
    while(pointer < commentStart)  {
        if(*pointer != ' ') {
            *status=U_INVALID_FORMAT_ERROR;
            break;
        }
        pointer++;
    }

    if(U_FAILURE(*status)) {
        fprintf(stderr, "problem putting stuff in hash table\n");
        *status = U_INTERNAL_PROGRAM_ERROR;
        return NULL;
    }

    return element;
}


void writeOutData(UCATableHeader *data,
                  UChar contractions[][3],
                  uint32_t noOfcontractions,
                  const char *outputDir,
                  const char *copyright,
                  UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return;
    }

    uint32_t size = data->size;

    if(noOfcontractions != 0) {
      contractions[noOfcontractions][0] = 0;
      contractions[noOfcontractions][1] = 0;
      contractions[noOfcontractions][2] = 0;
      noOfcontractions++;


      data->contractionUCACombos = size;
      data->size += paddedsize((noOfcontractions*3*sizeof(UChar)));
    }

    UNewDataMemory *pData;
    
    long dataLength;

    pData=udata_create(outputDir, UCA_DATA_TYPE, UCA_DATA_NAME, &dataInfo,
                       copyright, status);

    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error: unable to create data memory, error %d\n", *status);
        return;
    }

    /* write the data to the file */
    fprintf(stdout, "Writing out UCA table: %s%s.%s\n", outputDir,
                                                        UCA_DATA_NAME,
                                                        UCA_DATA_TYPE);
    udata_writeBlock(pData, data, size);

    if(noOfcontractions != 0) {
      udata_writeBlock(pData, contractions, noOfcontractions*3*sizeof(UChar));
      udata_writePadding(pData, paddedsize((noOfcontractions*3*sizeof(UChar))) - noOfcontractions*3*sizeof(uint16_t));
    }

    /* finish up */
    dataLength=udata_finish(pData, status);
    if(U_FAILURE(*status)) {
        fprintf(stderr, "Error: error %d writing the output file\n", *status);
        return;
    }
}

static int32_t
write_uca_table(const char *filename,
                const char *outputDir,
                const char *copyright,
                UErrorCode *status)
{
    FILE *data = fopen(filename, "r");
    uint32_t line = 0;
    UCAElements *element = NULL;
    UChar variableTopValue = 0;
    UCATableHeader *myD = (UCATableHeader *)uprv_malloc(sizeof(UCATableHeader));
    UColOptionSet *opts = (UColOptionSet *)uprv_malloc(sizeof(UColOptionSet));
    UChar contractionCEs[256][3];
    uint32_t noOfContractions = 0;


    if(data == NULL) {
        fprintf(stderr, "Couldn't open file: %s\n", filename);
        return -1;
    }

    memset(inverseTable, 0xDA, sizeof(int32_t)*3*0xFFFF);

    opts->variableTopValue = variableTopValue;
    opts->strength = UCOL_TERTIARY;
    opts->frenchCollation = UCOL_OFF;
    opts->alternateHandling = UCOL_NON_IGNORABLE; /* attribute for handling variable elements*/
    opts->caseFirst = UCOL_OFF;         /* who goes first, lower case or uppercase */
    opts->caseLevel = UCOL_OFF;         /* do we have an extra case level */
    opts->normalizationMode = UCOL_OFF; /* attribute for normalization */
    /* populate the version info struct with version info*/
    myD->version[0] = UCOL_BUILDER_VERSION;
    /*TODO:The fractional rules version should be taken from FractionalUCA.txt*/
    myD->version[1] = UCOL_FRACTIONAL_UCA_VERSION;
    myD->jamoSpecial = FALSE;

    tempUCATable *t = uprv_uca_initTempTable(myD, opts, NULL, status);


    while(!feof(data)) {
        if(U_FAILURE(*status)) {
            fprintf(stderr, "Something returned an error %i (%s) while processing line: %i\nExiting...",
                *status, u_errorName(*status), line);
            exit(*status);
        }

        element = readAnElement(data, status);
        line++;
        if(element != NULL) {
            // we have read the line, now do something sensible with the read data!
            if(element->variableTop == TRUE && variableTopValue == 0) {
                t->options->variableTopValue = element->cPoints[0];
            }

            // if element is a contraction, we want to add it to contractions
            if(element->cSize > 1) { // this is a contraction
              contractionCEs[noOfContractions][0] = element->cPoints[0];
              contractionCEs[noOfContractions][1] = element->cPoints[1];
              if(element->cSize > 2) { // the third one
                contractionCEs[noOfContractions][2] = element->cPoints[2];
              } else {
                contractionCEs[noOfContractions][2] = 0;
              }
              noOfContractions++;
            }

            /* we're first adding to inverse, because addAnElement will reverse the order */
            /* of code points and stuff... we don't want that to happen */
            addToInverse(element, status);
            uprv_uca_addAnElement(t, element, status);
        }
    }


    if (VERBOSE) {
        fprintf(stdout, "\nLines read: %i\n", line);
    }

    /* test */
    UCATableHeader *myData = uprv_uca_assembleTable(t, status);  
    writeOutData(myData, contractionCEs, noOfContractions, outputDir, copyright, status);

    InverseTableHeader *inverse = assembleInverseTable(status);
    writeOutInverseData(inverse, outputDir, copyright, status);

    uprv_uca_closeTempTable(t);    
    uprv_free(myD);
    uprv_free(opts);


    uprv_free(myData);
    uprv_free(inverse);
    fclose(data);

	return 0;
}

static UOption options[]={
    UOPTION_HELP_H,              /* 0  Numbers for those who*/ 
    UOPTION_HELP_QUESTION_MARK,  /* 1   can't count. */
    UOPTION_COPYRIGHT,           /* 2 */
    UOPTION_VERSION,             /* 3 */
    UOPTION_DESTDIR,             /* 4 */
    UOPTION_SOURCEDIR,           /* 5 */
    UOPTION_VERBOSE              /* 6 */
    /* weiv can't count :))))) */
};

int main(int argc, char* argv[]) {
    UErrorCode status = U_ZERO_ERROR;
    const char* destdir = NULL;
    const char* srcDir = NULL;
    char filename[300];
    char *basename = NULL;
    const char *copyright = NULL;

#ifdef XP_MAC_CONSOLE
    argc = ccommand((char***)&argv);
#endif

    /* preset then read command line options */
    options[4].value=u_getDataDirectory();
    options[5].value="";
    argc=u_parseArgs(argc, argv, sizeof(options)/sizeof(options[0]), options);

    /* error handling, printing usage message */
    if(argc<0) {
        fprintf(stderr,
            "error in command line argument \"%s\"\n",
            argv[-argc]);
    } else if(argc<2) {
        argc=-1;
    }
    if(options[0].doesOccur || options[1].doesOccur) {
        fprintf(stderr,
            "usage: %s [-options] file\n"
            "\tRead in UCA collation text data and write out the binary collation data\n"
            "\toptions:\n"
            "\t\t-h or -? or --help  this usage text\n"
            "\t\t-V or --version     show a version message\n"
            "\t\t-c or --copyright   include a copyright notice\n"
            "\t\t-d or --destdir     destination directory, followed by the path\n"
            "\t\t-s or --sourcedir   source directory, followed by the path\n"
            "\t\t-v or --verbose     Turn on verbose output\n",
            argv[0]);
        return argc<0 ? U_ILLEGAL_ARGUMENT_ERROR : U_ZERO_ERROR;
    }

    if(options[3].doesOccur) {
      fprintf(stdout, "genuca version %hu.%hu, ICU tool to read UCA text data and create UCA data tables for collation.\n",
            dataInfo.formatVersion[0], dataInfo.formatVersion[1]);
      fprintf(stdout, "Copyright (C) 2000-2001, International Business Machines\n");
      fprintf(stdout, "Corporation and others.  All Rights Reserved.\n");
        exit(0);
    }

    /* get the options values */
    destdir = options[4].value;
    srcDir = options[5].value;
    VERBOSE = options[6].doesOccur;

    if (options[2].doesOccur) {
        copyright = U_COPYRIGHT_STRING;
    }

    if(argc < 0) {

      /* prepare the filename beginning with the source dir */
      uprv_strcpy(filename, srcDir);
      basename=filename+uprv_strlen(filename);

      if(basename>filename && *(basename-1)!=U_FILE_SEP_CHAR) {
          *basename++=U_FILE_SEP_CHAR;
      }
    
      uprv_strcpy(basename, "FractionalUCA.txt");
    } else {
      argv++;
      uprv_strcpy(filename, getLongPathname(*argv));
    }

    return write_uca_table(filename, destdir, copyright, &status);
}

/*
 * Hey, Emacs, please set the following:
 *
 * Local Variables:
 * indent-tabs-mode: nil
 * End:
 *
 */
