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
*/

#include "genuca.h"
#include "cnttable.h"
#include "uoptions.h"
#include "toolutil.h"
#include "cstring.h"

#include <stdlib.h>

#ifdef XP_MAC_CONSOLE
#include <console.h>
#endif

/*UHashtable *elements = NULL;*/
UCAElements le;

/*
 * Global - verbosity
 */
UBool VERBOSE = FALSE;

/*
void deleteElement(void *element) {
    UCAElements *el = (UCAElements *)element;
    int32_t i = 0;
    for(i = 0; i < el->noOfCEs; i++) {
        free(el->primary[i]);
        free(el->secondary[i]);
        free(el->tertiary[i]);
    }

    free(el);
}
*/

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


uint32_t getSingleCEValue(char *primary, char *secondary, char *tertiary, UBool caseBit, UErrorCode *status) {
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
    uint32_t primvalue = (*primary!='\0')?strtoul(primary, &primend, 16):0;
    uint32_t secvalue = (*secondary!='\0')?strtoul(secondary, &secend, 16):0;
    uint32_t tervalue = (*tertiary!='\0')?strtoul(tertiary, &terend, 16):0;
    if(primvalue <= 0xFF) {
      primvalue <<= 8;
    }

    value = ((primvalue<<UCOL_PRIMARYORDERSHIFT)&UCOL_PRIMARYORDERMASK)|
        ((secvalue<<UCOL_SECONDARYORDERSHIFT)&UCOL_SECONDARYORDERMASK)|
        (tervalue&UCOL_TERTIARYORDERMASK);

    // This CE is not special at all... a very uninteresting one...
    value &= 0xFFFFFF7F; 

    // Here's case handling!
    if(caseBit == TRUE) {
        value |= 0x40; // 0100 0000 set case bit
    } else {
        value &= 0xFFFFFFBF; // ... 1011 1111 (reset case bit)
    }
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


UCAElements *copyUCAElement(UCAElements *that) {
    UCAElements *r = (UCAElements *)malloc(sizeof(*that));
    memcpy(r, that, sizeof(*that));
    return r;
}

void releaseUCACopy(UCAElements *r) {
    free(r);
}

uint32_t inverseTable[0xFFFF][3];
uint32_t inversePos = 0;
/*UChar *stringContinue[0xFFFF];*/
UChar stringContinue[0xFFFF];
uint32_t stringContSize[0xFFFF]; 
uint32_t sContPos = 0;
uint32_t contSize = 0;

#define UCOL_INV_SIZEMASK 0xFFF00000
#define UCOL_INV_OFFSETMASK 0x000FFFFF
#define UCOL_INV_SHIFTVALUE 20

void addNewInverse(UCAElements *element, UErrorCode *status) {

  if(VERBOSE && isContinuation(element->CEs[1])) {
    fprintf(stdout, "+");
  }
  inversePos++;
  inverseTable[inversePos][0] = element->CEs[0];
  if(element->noOfCEs > 1 && isContinuation(element->CEs[1])) {
    inverseTable[inversePos][1] = element->CEs[1];
  }
  if(element->cSize < 2) {
    inverseTable[inversePos][2] = element->cPoints[0];
  } else { /* add a new store of cruft */
    inverseTable[inversePos][2] = ((element->cSize+1) << UCOL_INV_SHIFTVALUE) | sContPos;
    memcpy(stringContinue+sContPos, element->cPoints, element->cSize*sizeof(UChar));
    sContPos += element->cSize+1;
  }
}

void addToExistingInverse(UCAElements *element, uint32_t position, UErrorCode *status) {

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

uint32_t addToInverse(UCAElements *element, UErrorCode *status) {

  if(inverseTable[inversePos][0] > element->CEs[0]) {
    uint32_t position = inversePos;
    while(inverseTable[--position][0] > element->CEs[0])
    addToExistingInverse(element, position, status);
  } else if(inverseTable[inversePos][0] == element->CEs[0]) {
    if(element->noOfCEs > 1 && isContinuation(element->CEs[1]) 
      && inverseTable[inversePos][1] != element->CEs[1]) {
      /* also, we should do long primaries here */
      addNewInverse(element, status);
    } else {
      addToExistingInverse(element, inversePos, status);
    } 
  } else {
    addNewInverse(element, status);
  }
  return inversePos;
}

InverseTableHeader *assembleInverseTable(UErrorCode *status)
{
  InverseTableHeader *result = NULL;
  uint32_t headerByteSize = paddedsize(sizeof(InverseTableHeader));
  uint32_t inverseTableByteSize = (inversePos+2)*sizeof(uint32_t)*3;
  uint32_t contsByteSize = sContPos * sizeof(UChar);

  result = (InverseTableHeader *)malloc(headerByteSize + inverseTableByteSize + contsByteSize);
  if(result != NULL) {
    result->byteSize = headerByteSize + inverseTableByteSize + contsByteSize;

    inversePos++;
    inverseTable[inversePos][0] = 0xFFFFFFFF;
    inverseTable[inversePos][1] = 0xFFFFFFFF;
    inverseTable[inversePos][2] = 0x0000FFFF;
    inversePos++;

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


void writeOutInverseData(InverseTableHeader *data,
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



int32_t hex2num(char hex) {
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
        element->variableTop = TRUE;
        return element; // just a comment, skip whole line
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
      fprintf(stderr, " scanf(hex) failed on [%s]\n ");
    }
    element->cPoints[0] = theValue;

    element->codepoint = element->cPoints[0];
    if(spacePointer == 0) {
        detectedContraction = FALSE;
        element->cSize = 1;
    } else {
        i = 1;
        detectedContraction = TRUE;
        while(spacePointer != NULL) {
            sscanf(spacePointer+1, "%04X", (element->cPoints+i));
            i++;
            spacePointer = strchr(spacePointer+1, ' ');
        }

        element->cSize = i;

        //fprintf(stderr, "Number of codepoints in contraction: %i\n", i);
    }

    startCodePoint = endCodePoint+1;
    endCodePoint = strchr(startCodePoint, ';');

    while(*startCodePoint != 'L' && *startCodePoint != 'S') {
        startCodePoint++;
        if(startCodePoint == endCodePoint) {
            *status = U_INVALID_FORMAT_ERROR;
            return NULL;
        }
    }

    if(*startCodePoint == 'S') {
        element->caseBit = FALSE;
    } else {
        element->caseBit = TRUE;
    }

    startCodePoint = endCodePoint+1;

    commentStart = strchr(startCodePoint, '#');
    if(commentStart == NULL) {
        commentStart = strlen(startCodePoint) + startCodePoint;
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
#if 0
        if(element->sizePrim[i]==3 && 
              strtoul(secondary, 0, 16)== UCOL_UNMARKED && 
              strtoul(tertiary, 0, 16) < 0x40) {
              /* This is a test for a long primary - secondary has 6 bits and tertiary must be unmarked */
              /* fprintf(stderr, "Long primary in expansion for 0x%04X\n", element->codepoint);*/
              element->CEs[CEindex++] = (uint32_t)strtoul(primary, 0, 16) << 8 | 0xC0 | (strtoul(tertiary, 0, 16) & 0x3F);
              /* Long primary,                       |          24P        |1|1| 6T |            */
        } else {     
#endif /* we will try to go without long primaries */
          element->CEs[CEindex++] = getSingleCEValue(primary, secondary, tertiary, element->caseBit, status);

          uint32_t CEi = 1;
          while(2*CEi<element->sizePrim[i] || CEi<element->sizeSec[i] || CEi<element->sizeTer[i]) {
              uint32_t value = 0x80; /* Continuation marker */
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
#if 0
        }
#endif /* part for long primaries */

        uint32_t terValue = strtoul(tertiary+strlen(tertiary)-2, NULL, 16);
        if(terValue > 0x3F) {
            fprintf(stderr, "Tertiary value %02X too big for %04X\n", terValue, element->codepoint);
        }
        startCodePoint = endCodePoint+1;
        i++;
    }
    element->noOfCEs = CEindex;

    element->isThai = UCOL_ISTHAIPREVOWEL(element->codepoint);

    // we don't want any strange stuff after useful data!
    while(pointer < commentStart)  {
        if(*pointer != ' ') {
            *status=U_INVALID_FORMAT_ERROR;
            break;
        }
        *pointer++;
    }

    /*
    strcpy(element->comment, commentStart);
    uhash_put(elements, (void *)element->codepoint, element, status);
    */

    if(U_FAILURE(*status)) {
        fprintf(stderr, "problem putting stuff in hash table\n");
        *status = U_INTERNAL_PROGRAM_ERROR;
        free(element);
        return NULL;
    }

    return element;
}


void writeOutData(UCATableHeader *data,
                  const char *outputDir,
                  const char *copyright,
                  UErrorCode *status)
{
    if(U_FAILURE(*status)) {
        return;
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
    udata_writeBlock(pData, data, data->size);

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
    int32_t line = 0;
    int32_t sizesPrim[35], sizesSec[35], sizesTer[35];
    int32_t terValue[0xffff], secValue[0xffff];
    int32_t sizeBreakDown[35][35][35];
    UCAElements *element = NULL;
    UChar variableTopValue = 0;
    UBool foundVariableTop = FALSE;
    UCATableHeader *myD = (UCATableHeader *)uprv_malloc(sizeof(UCATableHeader));


    if(data == NULL) {
        fprintf(stderr, "Couldn't open file: %s\n", filename);
        return -1;
    }

    memset(secValue, 0, 0xffff*sizeof(int32_t));
    memset(terValue, 0, 0xffff*sizeof(int32_t));
    memset(sizesPrim, 0, 35*sizeof(int32_t));
    memset(sizesSec, 0, 35*sizeof(int32_t));
    memset(sizesTer, 0, 35*sizeof(int32_t));
    memset(sizeBreakDown, 0, 35*35*35*sizeof(int32_t));
    memset(inverseTable, 0, sizeof(int32_t)*3*0xFFFF);

    myD->variableTopValue = variableTopValue;
    myD->strength = UCOL_TERTIARY;
    myD->frenchCollation = UCOL_OFF;
    myD->alternateHandling = UCOL_SHIFTED; /* attribute for handling variable elements*/
    myD->caseFirst = UCOL_LOWER_FIRST;         /* who goes first, lower case or uppercase */
    myD->caseLevel = UCOL_OFF;         /* do we have an extra case level */
    myD->normalizationMode = UCOL_ON; /* attribute for normalization */
    /* populate the version info struct with version info*/
    myD->version[0] = UCA_BUILDER_VERSION;
    /*TODO:The fractional rules version should be taken from FractionalUCA.txt*/
    myD->version[1] = UCA_TAILORING_RULES_VERSION;

    tempUCATable *t = uprv_uca_initTempTable(myD, status);

    /*
    elements = uhash_open(uhash_hashLong, uhash_compareLong, &status);

    uhash_setValueDeleter(elements, deleteElement);
    */


    while(!feof(data)) {
        if(U_FAILURE(*status)) {
            fprintf(stderr, "Something returned an error %i while processing line: %i\nExiting...", status, line);
            exit(*status);
        }

        element = readAnElement(data, status);
        line++;
        if(element != NULL) {
            /* this does statistics on CE lengths, but is currently broken */
/*
          for( i = 0; i<element->noOfCEs; i++) {
            sizesPrim[element->sizePrim[i]]++;
            sizesSec[element->sizeSec[i]]++;
            sizesTer[element->sizeTer[i]]++;

            sizeBreakDown[element->sizePrim[i]][element->sizeSec[i]][element->sizeTer[i]]++;

            if(element->sizePrim[i] == 2 && element->sizeSec[i]==2) {
              terValue[strtoul(element->tertiary[i], 0, 16)]++;
              secValue[strtoul(element->secondary[i], 0, 16)]++;
            }
         }
*/


            // we have read the line, now do something sensible with the read data!
            if(element->variableTop == TRUE) {
                foundVariableTop = TRUE;
                continue;
            }

            if(variableTopValue == 0 && foundVariableTop == TRUE) {
                t->image->variableTopValue = element->cPoints[0];
                foundVariableTop = FALSE;
            }

            /* we're first adding to inverse, because addAnElement will reverse the order */
            /* of code points and stuff... we don't want that to happen */
            uint32_t invResult = addToInverse(element, status);
            uint32_t result = uprv_uca_addAnElement(t, element, status);
            //deleteElement(element);
        }
    }


    if (VERBOSE) {
        fprintf(stdout, "\nLines read: %i\n", line);
    }



/*
    for(i = 0; i<35; i++) {
        fprintf(stderr, "size %i: P:%i S:%i T:%i\n", i, sizesPrim[i], sizesSec[i], sizesTer[i]);
    }

    for(i = 0; i<35; i++) {
        UBool printedPrimary = FALSE;
        for(j = 0; j<35; j++) {
            for(k = 0; k<35; k++) {
                if(sizeBreakDown[i][j][k] != 0) {
                    if(!printedPrimary) {
                        fprintf(stderr, "Primary: %i\n", i);
                        printedPrimary = TRUE;
                    }
                    fprintf(stderr, "Sec: %i, Ter: %i = %i\n", j, k, sizeBreakDown[i][j][k]);
                }
            }
        }
    }

    for(i = 0; i<(uint32_t)0xffff; i++) {
      if(terValue[i] != 0) {
        fprintf(stderr, "Tertiaries with value %04X : %i\n", i, terValue[i]);
      }
      if(secValue[i] != 0) {
        fprintf(stderr, "Secondaries with value %04X : %i\n", i, secValue[i]);
      }
    }
*/
    /* test */
    UCATableHeader *myData = uprv_uca_assembleTable(t, status);  
    writeOutData(myData, outputDir, copyright, status);

    InverseTableHeader *inverse = assembleInverseTable(status);
    writeOutInverseData(inverse, outputDir, copyright, status);
/*
    uint32_t *itab = (uint32_t *)((uint8_t *)inverse + inverse->table);
    UChar *conts = (UChar *)((uint8_t *)inverse + inverse->conts);
    for(i = 0; i<inverse->tableSize; i++) {
      fprintf(stderr, "[%04X] 0x%08X 0x%08X 0x%08X\n", i, *(itab+3*i), *(itab+3*i+1), *(itab+3*i+2));
      if((*(itab+3*i+2) & UCOL_INV_SIZEMASK) != 0) {
        uint32_t contIndex = *(itab+3*i+2) & UCOL_INV_OFFSETMASK;
        uint32_t contSize = (*(itab+3*i+2) & UCOL_INV_SIZEMASK) >> UCOL_INV_SHIFTVALUE;
        fprintf(stderr, "\t");
        for(j = 0; j<contSize; j++) {
          if(*(conts+contIndex+j) < 0xFFFE) {
            fprintf(stderr, "%04X ", *(conts+contIndex+j));
          } else {
            fprintf(stderr, "\n\t");
          }
        }
        fprintf(stderr, "\n");
      }
    }
*/

    uprv_uca_closeTempTable(t);    
    uprv_free(myD);

    //printOutTable(myData, &status);
    //uhash_close(elements);
    ucmp32_close(myData->mapping);

    free(myData);
    free(inverse);
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
